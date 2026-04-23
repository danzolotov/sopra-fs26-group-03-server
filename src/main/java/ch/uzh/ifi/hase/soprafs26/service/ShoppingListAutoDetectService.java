package ch.uzh.ifi.hase.soprafs26.service;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
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
			Pattern.compile("^(?:[-*\\u2022]|\\d+[.)]|\\[\\s*[xX ]\\s*\\])\\s+");
	private static final Pattern STARTS_WITH_QUANTITY =
			Pattern.compile("^\\d+(?:[.,]\\d+)?\\s*(?:x|kg|g|mg|l|ml|pcs?|pack|packs|bottle|bottles)?\\b.*",
					Pattern.CASE_INSENSITIVE);
	private static final Pattern LEADING_QUANTITY = Pattern.compile("^\\s*(\\d+)(?:[.,]\\d+)?\\s*(?:x)?\\b\\s*");

	public List<String> detectShoppingListItems(byte[] imageBytes) {
		return detectShoppingListItemsWithQuantities(imageBytes).stream()
				.map(DetectedShoppingItem::getIngredientName)
				.toList();
	}

	public List<DetectedShoppingItem> detectShoppingListItemsWithQuantities(byte[] imageBytes) {
		if (imageBytes == null || imageBytes.length == 0) {
			return List.of();
		}

		String ocrText = detectText(imageBytes);
		if (ocrText.isBlank()) {
			return List.of();
		}

		return extractShoppingListItemsWithQuantities(ocrText);
	}

	private String detectText(byte[] imageBytes) {
		try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
			Image image = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();
			Feature feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
			AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
					.addFeatures(feature)
					.setImage(image)
					.build();

			BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
			List<AnnotateImageResponse> responses = response.getResponsesList();
			if (responses.isEmpty()) {
				return "";
			}

			AnnotateImageResponse firstResponse = responses.get(0);
			if (firstResponse.hasError()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
						"Vision API failed: " + firstResponse.getError().getMessage());
			}

			List<EntityAnnotation> texts = firstResponse.getTextAnnotationsList();
			return texts.isEmpty() ? "" : texts.get(0).getDescription();
		}
		catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to access Vision API", e);
		}
	}

	private List<String> extractShoppingListItems(String ocrText) {
		return extractShoppingListItemsWithQuantities(ocrText).stream()
				.map(DetectedShoppingItem::getIngredientName)
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

		List<DetectedShoppingItem> items = new ArrayList<>();
		for (String line : cleanedLines) {
			if (isListItemCandidate(line)) {
				String normalizedItemLine = stripListMarker(line);
				int quantity = extractQuantity(normalizedItemLine);
				String ingredientName = stripQuantityPrefix(normalizedItemLine);
				if (!ingredientName.isBlank()) {
					items.add(new DetectedShoppingItem(ingredientName, quantity));
				}
			}
		}
		return items;
	}

	private int extractQuantity(String line) {
		Matcher matcher = LEADING_QUANTITY.matcher(line);
		if (!matcher.find()) {
			return 1;
		}
		try {
			return Math.max(1, Integer.parseInt(matcher.group(1)));
		}
		catch (NumberFormatException ignored) {
			return 1;
		}
	}

	private String stripQuantityPrefix(String line) {
		String cleaned = LEADING_QUANTITY.matcher(line).replaceFirst("").trim();
		return cleaned.replaceAll("\\s+", " ");
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

		if (LEADING_LIST_MARKER.matcher(line).find() || STARTS_WITH_QUANTITY.matcher(line).find()) {
			return true;
		}

		String normalized = line.toLowerCase(Locale.ROOT);
		if (normalized.matches("^[a-zA-Z][a-zA-Z\\s-]{1,35}$")) {
			return !normalized.contains("total") && !normalized.contains("receipt");
		}
		return false;
	}

	private String normalizeLine(String line) {
		return line.replaceAll("\\s+", " ").trim();
	}

	private String stripListMarker(String line) {
		String cleaned = LEADING_LIST_MARKER.matcher(line).replaceFirst("").trim();
		return cleaned.replaceAll("\\s+", " ");
	}

	public static class DetectedShoppingItem {
		private final String ingredientName;
		private final int quantity;

		public DetectedShoppingItem(String ingredientName, int quantity) {
			this.ingredientName = ingredientName;
			this.quantity = quantity;
		}

		public String getIngredientName() {
			return ingredientName;
		}

		public int getQuantity() {
			return quantity;
		}
	}
}

