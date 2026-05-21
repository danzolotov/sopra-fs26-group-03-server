package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.Vertex;
import com.google.protobuf.ByteString;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ShoppingListAutoDetectService {

	private static final Pattern LEADING_LIST_MARKER =
			Pattern.compile("^(?:[-*\\u2022\\u2023\\u25E6\\u2043\\u2219\\u00B7]|[oO0\\u00BA\\u00B0]|\\d+[.)]|\\[\\s*[xX ]\\s*])\\s+");
	private static final String OCR_QUANTITY = "(?=[\\dOo]*\\d)[\\dOo]+";
	private static final String OCR_AMOUNT = OCR_QUANTITY + "(?:[.,]" + OCR_QUANTITY + ")?";
	private static final Pattern STARTS_WITH_QUANTITY =
			Pattern.compile("^" + OCR_AMOUNT + "\\s*(?:x|kg|g|mg|l|ml|cl|pcs?|pieces?|pack|packs|tbsp|tablespoons?|tsp|teaspoons?|cups?)?\\b.*",
					Pattern.CASE_INSENSITIVE);
	private static final String UNIT_ALIASES =
			"kg|kilograms?|g|grams?|mg|milligrams?|l|liters?|litres?|ml|milliliters?|millilitres?|cl|centiliters?|centilitres?|pcs?|pieces?|pack|packs|bottles?|tbsp|tablespoons?|tsp|teaspoons?|cups?";
	private static final Pattern LEADING_AMOUNT = Pattern.compile(
			"^\\s*(" + OCR_AMOUNT + ")\\s*(?:(x)\\b|(" + UNIT_ALIASES + ")\\b)?\\s*(.*)$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern TRAILING_AMOUNT = Pattern.compile(
			"^(.*?)\\s+(" + OCR_AMOUNT + ")\\s*(?:(x)\\b|(" + UNIT_ALIASES + ")\\b)?\\s*$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern LEADING_MULTIPLIER = Pattern.compile(
			"^\\s*x\\s*(\\d+)\\s+(.+)$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern TRAILING_MULTIPLIER = Pattern.compile(
			"^(.+?)\\s+x\\s*(\\d+)\\s*$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern INGREDIENT_NAME_ONLY = Pattern.compile("^[a-zA-Z][a-zA-Z\\s-]{1,35}$");

	public List<DetectedShoppingItem> detectShoppingListItemsWithQuantities(byte[] imageBytes) {
		if (imageBytes == null || imageBytes.length == 0) {
			return List.of();
		}

		OcrResult ocrResult = detectText(imageBytes);
		if (ocrResult.isBlank()) {
			return List.of();
		}

		if (!ocrResult.rows().isEmpty()) {
			List<DetectedShoppingItem> rowItems = extractShoppingListItemsWithQuantities(String.join("\n", ocrResult.rows()));
			if (!rowItems.isEmpty()) {
				return rowItems;
			}
		}

		return extractShoppingListItemsWithQuantities(ocrResult.description());
	}

	private OcrResult detectText(byte[] imageBytes) {
		try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
			Image image = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();

			OcrResult result = tryFeature(vision, image, Feature.Type.TEXT_DETECTION);
			if (result != null && !result.isBlank()) {
				return result;
			}

			result = tryFeature(vision, image, Feature.Type.DOCUMENT_TEXT_DETECTION);
			return result == null ? OcrResult.blank() : result;
		}
		catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to access Vision API", e);
		}
	}

	private OcrResult tryFeature(ImageAnnotatorClient vision, Image image, Feature.Type featureType) {
		Feature feature = Feature.newBuilder().setType(featureType).build();
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
				.addFeatures(feature)
				.setImage(image)
				.build();

		BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
		List<AnnotateImageResponse> responses = response.getResponsesList();
		if (responses.isEmpty()) {
			return OcrResult.blank();
		}

		AnnotateImageResponse firstResponse = responses.get(0);
		if (firstResponse.hasError()) {
			return OcrResult.blank();
		}

		List<EntityAnnotation> texts = firstResponse.getTextAnnotationsList();
		if (texts != null && !texts.isEmpty()) {
			String description = texts.get(0).getDescription() == null ? "" : texts.get(0).getDescription();
			return new OcrResult(description, buildRowsFromAnnotations(texts.subList(1, texts.size())));
		}

		return OcrResult.blank();
	}

	private List<String> buildRowsFromAnnotations(List<EntityAnnotation> annotations) {
		List<OcrToken> tokens = new ArrayList<>();
		for (EntityAnnotation annotation : annotations) {
			String text = normalizeLine(annotation.getDescription());
			if (text.isBlank() || !annotation.hasBoundingPoly()) {
				continue;
			}

			Bounds bounds = Bounds.from(annotation.getBoundingPoly().getVerticesList());
			if (bounds == null) {
				continue;
			}
			tokens.add(new OcrToken(text, bounds.minX(), bounds.centerY(), Math.max(1, bounds.height())));
		}

		if (tokens.isEmpty()) {
			return List.of();
		}

		tokens.sort((left, right) -> {
			int byY = Double.compare(left.centerY(), right.centerY());
			return byY != 0 ? byY : Integer.compare(left.minX(), right.minX());
		});

		List<OcrRow> rows = new ArrayList<>();
		for (OcrToken token : tokens) {
			OcrRow matchingRow = null;
			for (OcrRow row : rows) {
				double tolerance = Math.max(8.0, Math.max(row.averageHeight(), token.height()) * 0.65);
				if (Math.abs(row.centerY() - token.centerY()) <= tolerance) {
					matchingRow = row;
					break;
				}
			}

			if (matchingRow == null) {
				rows.add(new OcrRow(token));
			}
			else {
				matchingRow.add(token);
			}
		}

		rows.sort((left, right) -> Double.compare(left.centerY(), right.centerY()));
		return rows.stream()
				.map(OcrRow::text)
				.filter(line -> !line.isBlank())
				.toList();
	}

	private List<DetectedShoppingItem> extractShoppingListItemsWithQuantities(String ocrText) {
		String[] rawLines = ocrText.split("\\R");
		List<String> cleanedLines = new ArrayList<>();

		for (String rawLine : rawLines) {
			String normalized = normalizeLine(rawLine);
			if (!normalized.isBlank()) {
				cleanedLines.add(normalized);
			}
		}

		if (!looksLikeShoppingList(cleanedLines)) {
			return List.of();
		}

		List<DetectedShoppingItem> columnItems = parseColumnSeparatedList(cleanedLines);
		if (!columnItems.isEmpty()) {
			return columnItems;
		}

		List<DetectedShoppingItem> items = new ArrayList<>();
		for (int i = 0; i < cleanedLines.size(); i++) {
			String line = cleanedLines.get(i);
			if (isListItemCandidate(line)) {
				String normalizedItemLine = stripListMarker(line);
				ParsedAmount amountOnly = parseAmountOnly(normalizedItemLine);
				if (amountOnly != null && i + 1 < cleanedLines.size()) {
					String nextLine = stripListMarker(cleanedLines.get(i + 1));
					if (isIngredientNameOnly(nextLine)) {
						String ingredientName = normalizeIngredientName(nextLine);
						items.add(new DetectedShoppingItem(
								ingredientName,
								adjustLikelyDroppedZerosQuantity(ingredientName, amountOnly.quantity(), amountOnly.unit()),
								amountOnly.unit()));
						i++;
						continue;
					}
				}

				ParsedShoppingLine parsedLine = parseShoppingLine(normalizedItemLine);
				if (!parsedLine.ingredientName().isBlank()) {
					items.add(new DetectedShoppingItem(
							parsedLine.ingredientName(),
							parsedLine.quantity(),
							parsedLine.unit()));
				}
			}
		}
		return items;
	}

	private List<DetectedShoppingItem> parseColumnSeparatedList(List<String> cleanedLines) {
		List<String> normalizedLines = cleanedLines.stream()
				.map(this::stripListMarker)
				.filter(line -> !line.isBlank())
				.toList();

		if (normalizedLines.size() < 4 || normalizedLines.size() % 2 != 0) {
			return List.of();
		}

		int midpoint = normalizedLines.size() / 2;
		List<ParsedAmount> amounts = new ArrayList<>();
		for (int i = 0; i < midpoint; i++) {
			ParsedAmount amount = parseAmountOnly(normalizedLines.get(i));
			if (amount == null) {
				return List.of();
			}
			amounts.add(amount);
		}

		List<DetectedShoppingItem> items = new ArrayList<>();
		for (int i = midpoint; i < normalizedLines.size(); i++) {
			String ingredientName = normalizeIngredientName(normalizedLines.get(i));
			if (!isIngredientNameOnly(ingredientName)) {
				return List.of();
			}
			ParsedAmount amount = amounts.get(i - midpoint);
			items.add(new DetectedShoppingItem(
					ingredientName,
					adjustLikelyDroppedZerosQuantity(ingredientName, amount.quantity(), amount.unit()),
					amount.unit()));
		}
		return items;
	}

	private ParsedAmount parseAmountOnly(String line) {
		Matcher leadingMatcher = LEADING_AMOUNT.matcher(line);
		if (leadingMatcher.matches() && leadingMatcher.group(4).isBlank()) {
			return new ParsedAmount(parseQuantity(leadingMatcher.group(1), leadingMatcher.group(3)), parseUnit(leadingMatcher.group(3)));
		}

		return null;
	}

	private ParsedShoppingLine parseShoppingLine(String line) {
		Matcher leadingMultiplierMatcher = LEADING_MULTIPLIER.matcher(line);
		if (leadingMultiplierMatcher.matches() && !leadingMultiplierMatcher.group(2).isBlank()) {
			return new ParsedShoppingLine(
					normalizeIngredientName(leadingMultiplierMatcher.group(2)),
					parseQuantity(leadingMultiplierMatcher.group(1), null),
					null);
		}

		Matcher leadingMatcher = LEADING_AMOUNT.matcher(line);
		if (leadingMatcher.matches() && !leadingMatcher.group(4).isBlank()) {
			String ingredientName = normalizeIngredientName(leadingMatcher.group(4));
			Unit unit = parseUnit(leadingMatcher.group(3));
			return new ParsedShoppingLine(
					ingredientName,
					adjustLikelyDroppedZerosQuantity(ingredientName, parseQuantity(leadingMatcher.group(1), leadingMatcher.group(3)), unit),
					unit);
		}

		Matcher trailingMultiplierMatcher = TRAILING_MULTIPLIER.matcher(line);
		if (trailingMultiplierMatcher.matches() && !trailingMultiplierMatcher.group(1).isBlank()) {
			return new ParsedShoppingLine(
					normalizeIngredientName(trailingMultiplierMatcher.group(1)),
					parseQuantity(trailingMultiplierMatcher.group(2), null),
					null);
		}

		Matcher trailingMatcher = TRAILING_AMOUNT.matcher(line);
		if (trailingMatcher.matches() && !trailingMatcher.group(1).isBlank()) {
			String ingredientName = normalizeIngredientName(trailingMatcher.group(1));
			Unit unit = parseUnit(trailingMatcher.group(4));
			return new ParsedShoppingLine(
					ingredientName,
					adjustLikelyDroppedZerosQuantity(ingredientName, parseQuantity(trailingMatcher.group(2), trailingMatcher.group(4)), unit),
					unit);
		}

		return new ParsedShoppingLine(normalizeIngredientName(line), 1, null);
	}

	private int parseQuantity(String rawQuantity, String rawUnit) {
		try {
			String normalizedQuantity = rawQuantity.replace('O', '0').replace('o', '0');
			Matcher decimalMatcher = Pattern.compile("^(\\d+)[.,](\\d+)$").matcher(normalizedQuantity);
			if (decimalMatcher.matches()) {
				String decimalPart = decimalMatcher.group(2);
				if (decimalPart.matches("0+") && isLikelySplitHundredsUnit(rawUnit)) {
					return Math.max(1, Integer.parseInt(decimalMatcher.group(1) + decimalPart));
				}
				return Math.max(1, Integer.parseInt(decimalMatcher.group(1)));
			}
			return Math.max(1, Integer.parseInt(normalizedQuantity));
		}
		catch (NumberFormatException ignored) {
			return 1;
		}
	}

	private boolean isLikelySplitHundredsUnit(String rawUnit) {
		if (rawUnit == null || rawUnit.isBlank()) {
			return false;
		}

		String normalizedUnit = rawUnit.toLowerCase(Locale.ROOT).replace(".", "");
		return normalizedUnit.equals("ml")
				|| normalizedUnit.equals("milliliter")
				|| normalizedUnit.equals("milliliters")
				|| normalizedUnit.equals("millilitre")
				|| normalizedUnit.equals("millilitres")
				|| normalizedUnit.equals("g")
				|| normalizedUnit.equals("gram")
				|| normalizedUnit.equals("grams");
	}

	private int adjustLikelyDroppedZerosQuantity(String ingredientName, int quantity, Unit unit) {
		if (unit == Unit.MILLILITER
				&& quantity > 0
				&& quantity < 10
				&& ingredientName != null
				&& ingredientName.toLowerCase(Locale.ROOT).contains("milk")) {
			return quantity * 100;
		}

		return quantity;
	}

	private Unit parseUnit(String rawUnit) {
		if (rawUnit == null || rawUnit.isBlank()) {
			return null;
		}

		return switch (rawUnit.toLowerCase(Locale.ROOT).replace(".", "")) {
			case "kg", "kilogram", "kilograms" -> Unit.KILOGRAM;
			case "g", "gram", "grams" -> Unit.GRAM;
			case "l", "liter", "liters", "litre", "litres" -> Unit.LITER;
			case "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> Unit.MILLILITER;
			case "cl", "centiliter", "centiliters", "centilitre", "centilitres" -> Unit.CENTILITER;
			case "pc", "pcs", "piece", "pieces", "pack", "packs", "bottle", "bottles" -> Unit.PIECE;
			case "tbsp", "tablespoon", "tablespoons" -> Unit.TABLESPOON;
			case "tsp", "teaspoon", "teaspoons" -> Unit.TEASPOON;
			case "cup", "cups" -> Unit.CUP;
			default -> null;
		};
	}

	private String normalizeIngredientName(String line) {
		return line.trim().replaceAll("\\s+", " ");
	}

	private boolean looksLikeShoppingList(List<String> lines) {
		if (lines.size() < 2) {
			return false;
		}

		int likelyItems = 0;
		for (String line : lines) {
			if (isListItemCandidate(line)) {
				likelyItems++;
			}
		}
		return likelyItems >= 2;
	}

	private boolean isListItemCandidate(String line) {
		if (line.length() > 45) {
			return false;
		}

		String normalized = line.toLowerCase(Locale.ROOT);
		if (normalized.contains("total") || normalized.contains("receipt") || normalized.contains("thank you")) {
			return false;
		}

		if (LEADING_LIST_MARKER.matcher(line).find()
				|| STARTS_WITH_QUANTITY.matcher(line).find()
				|| LEADING_MULTIPLIER.matcher(line).matches()
				|| TRAILING_MULTIPLIER.matcher(line).matches()
				|| TRAILING_AMOUNT.matcher(line).matches()) {
			return true;
		}

		return INGREDIENT_NAME_ONLY.matcher(normalized).matches();
	}

	private String normalizeLine(String line) {
		if (line == null) {
			return "";
		}
		return line.replaceAll("\\s+", " ").trim();
	}

	private String stripListMarker(String line) {
		String cleaned = LEADING_LIST_MARKER.matcher(line).replaceFirst("").trim();
		return cleaned.replaceAll("\\s+", " ");
	}

	private boolean isIngredientNameOnly(String line) {
		if (line == null || line.isBlank()) {
			return false;
		}

		String normalized = normalizeIngredientName(line);
		String lowerCase = normalized.toLowerCase(Locale.ROOT);
		return !lowerCase.contains("total")
				&& !lowerCase.contains("receipt")
				&& !lowerCase.contains("thank you")
				&& INGREDIENT_NAME_ONLY.matcher(normalized).matches();
	}

	private record OcrResult(String description, List<String> rows) {

		private static OcrResult blank() {
			return new OcrResult("", List.of());
		}

		private boolean isBlank() {
			return description.isBlank() && rows.isEmpty();
		}
	}

	private record Bounds(int minX, int minY, int maxX, int maxY) {

		private static Bounds from(List<Vertex> vertices) {
			if (vertices == null || vertices.isEmpty()) {
				return null;
			}

			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxY = Integer.MIN_VALUE;

			for (Vertex vertex : vertices) {
				minX = Math.min(minX, vertex.getX());
				minY = Math.min(minY, vertex.getY());
				maxX = Math.max(maxX, vertex.getX());
				maxY = Math.max(maxY, vertex.getY());
			}

			if (minX == Integer.MAX_VALUE || minY == Integer.MAX_VALUE) {
				return null;
			}
			return new Bounds(minX, minY, maxX, maxY);
		}

		private double centerY() {
			return (minY + maxY) / 2.0;
		}

		private int height() {
			return maxY - minY;
		}
	}

	private record OcrToken(String text, int minX, double centerY, int height) {
	}

	private static class OcrRow {
		private final List<OcrToken> tokens = new ArrayList<>();
		private double centerY;
		private double averageHeight;

		private OcrRow(OcrToken token) {
			add(token);
		}

		private void add(OcrToken token) {
			tokens.add(token);
			centerY = tokens.stream()
					.mapToDouble(OcrToken::centerY)
					.average()
					.orElse(token.centerY());
			averageHeight = tokens.stream()
					.mapToInt(OcrToken::height)
					.average()
					.orElse(token.height());
		}

		private double centerY() {
			return centerY;
		}

		private double averageHeight() {
			return averageHeight;
		}

		private String text() {
			tokens.sort((left, right) -> Integer.compare(left.minX(), right.minX()));
			return tokens.stream()
					.map(OcrToken::text)
					.reduce((left, right) -> left + " " + right)
					.orElse("");
		}
	}

	private record ParsedAmount(int quantity, Unit unit) {
	}

	private record ParsedShoppingLine(String ingredientName, int quantity, Unit unit) {
	}

	public record DetectedShoppingItem(String ingredientName, int quantity, Unit unit) {

		public DetectedShoppingItem(String ingredientName, int quantity) {
			this(ingredientName, quantity, null);
		}
	}
}
