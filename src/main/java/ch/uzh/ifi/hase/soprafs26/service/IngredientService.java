package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.IngredientSeedData;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Transactional
public class IngredientService {

	private static final Pattern LEADING_LIST_MARKER =
			Pattern.compile("^(?:[-*\\u2022]|\\d+[.)]|\\[\\s*[xX ]\\s*])\\s+");
	private static final Pattern STARTS_WITH_QUANTITY = Pattern.compile("^\\d+(?:[.,]\\d+)?\\s*");
	private static final Pattern STARTS_WITH_UNIT =
			Pattern.compile("^(?:x|kg|g|mg|l|ml|pcs?|pack|packs|bottle|bottles)\\b\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9 ]");
	private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
	private static final double AUTO_COMPLETE_THRESHOLD = 0.78;

	private final IngredientRepository ingredientRepository;

	public IngredientService(@Qualifier("ingredientRepository") IngredientRepository ingredientRepository) {
		this.ingredientRepository = ingredientRepository;
	}

	public List<Ingredient> getIngredients() {
		return ingredientRepository.findAll();
	}

	public Ingredient createIngredient(Ingredient ingredient) {
		if (ingredient == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient payload must be provided");
		}
		if (ingredient.getIngredientName() == null || ingredient.getIngredientName().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient name must be provided");
		}
		if (ingredient.getUnit() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient unit must be provided");
		}

		String normalizedName = ingredient.getIngredientName().trim();
		ingredient.setIngredientName(normalizedName);

		ingredientRepository.findByIngredientNameIgnoreCase(normalizedName).ifPresent(existing -> {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					String.format("Ingredient '%s' already exists", normalizedName));
		});

		return ingredientRepository.saveAndFlush(ingredient);
	}

	public List<IngredientAutocompleteResult> autocompleteIngredients(List<String> foundIngredients) {
		if (foundIngredients == null || foundIngredients.isEmpty()) {
			return List.of();
		}

		Map<String, CandidateIngredient> candidates = buildCandidates();
		List<IngredientAutocompleteResult> results = new ArrayList<>();

		for (String foundIngredient : foundIngredients) {
			String input = foundIngredient == null ? "" : foundIngredient.trim();
			String normalizedInput = normalizeIngredientText(input);
			CandidateMatch bestMatch = normalizedInput.isBlank()
					? null
					: findBestMatch(normalizedInput, candidates.values());
			boolean matched = bestMatch != null && bestMatch.similarity >= AUTO_COMPLETE_THRESHOLD;

			results.add(new IngredientAutocompleteResult(
					input,
					matched ? bestMatch.candidate.canonicalName : null,
					matched ? bestMatch.candidate.ingredientId : null,
					bestMatch == null ? 0.0 : bestMatch.similarity,
					matched));
		}

		return results;
	}

	public List<Ingredient> resolveOrCreateDetectedIngredients(List<String> foundIngredients) {
		if (foundIngredients == null || foundIngredients.isEmpty()) {
			return List.of();
		}

		List<IngredientAutocompleteResult> matches = autocompleteIngredients(foundIngredients);
		Map<String, Ingredient> uniqueIngredientsByName = new LinkedHashMap<>();

		for (IngredientAutocompleteResult match : matches) {
			Ingredient ingredient = resolveOrCreateIngredient(match);
			if (ingredient != null) {
				uniqueIngredientsByName.putIfAbsent(ingredient.getIngredientName().toLowerCase(Locale.ROOT), ingredient);
			}
		}

		return new ArrayList<>(uniqueIngredientsByName.values());
	}

	public Ingredient resolveOrCreateDetectedIngredient(String foundIngredient) {
		if (foundIngredient == null || foundIngredient.isBlank()) {
			return null;
		}

		List<IngredientAutocompleteResult> matches = autocompleteIngredients(List.of(foundIngredient));
		if (matches.isEmpty()) {
			return null;
		}
		return resolveOrCreateIngredient(matches.get(0));
	}

	private Ingredient resolveOrCreateIngredient(IngredientAutocompleteResult match) {
		Ingredient byId = resolveById(match.getIngredientId());
		if (byId != null) {
			return byId;
		}

		String candidateName = deriveIngredientName(match);
		if (candidateName.isBlank()) {
			return null;
		}

		Ingredient existingByName = ingredientRepository.findByIngredientNameIgnoreCase(candidateName).orElse(null);
		if (existingByName != null) {
			return existingByName;
		}

		Ingredient ingredient = new Ingredient();
		ingredient.setIngredientName(candidateName);
		ingredient.setIngredientDescription("");
		ingredient.setUnit(Unit.PIECE);
		return ingredientRepository.saveAndFlush(ingredient);
	}

	private Ingredient resolveById(Long ingredientId) {
		if (ingredientId == null) {
			return null;
		}
		return ingredientRepository.findById(ingredientId).orElse(null);
	}

	private String deriveIngredientName(IngredientAutocompleteResult match) {
		if (match.isMatched() && match.getIngredientName() != null && !match.getIngredientName().isBlank()) {
			return match.getIngredientName().trim();
		}

		String rawInput = match.getInput() == null ? "" : match.getInput().trim();
		if (rawInput.isBlank()) {
			return "";
		}

		String cleaned = LEADING_LIST_MARKER.matcher(rawInput).replaceFirst("");
		cleaned = STARTS_WITH_QUANTITY.matcher(cleaned).replaceFirst("");
		cleaned = STARTS_WITH_UNIT.matcher(cleaned).replaceFirst("");
		cleaned = NON_ALNUM.matcher(cleaned.toLowerCase(Locale.ROOT)).replaceAll(" ");
		cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ").trim();
		return toTitleCase(cleaned);
	}

	private String toTitleCase(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		for (String token : Arrays.asList(value.split(" "))) {
			if (token.isBlank()) {
				continue;
			}
			if (!result.isEmpty()) {
				result.append(' ');
			}
			result.append(Character.toUpperCase(token.charAt(0)));
			if (token.length() > 1) {
				result.append(token.substring(1));
			}
		}
		return result.toString();
	}

	private Map<String, CandidateIngredient> buildCandidates() {
		Map<String, CandidateIngredient> candidates = new LinkedHashMap<>();
		Map<String, Long> databaseIngredientIdsByName = new LinkedHashMap<>();

		for (Ingredient ingredient : ingredientRepository.findAll()) {
			String canonicalName = ingredient.getIngredientName().trim();
			String normalizedLabel = normalizeIngredientText(canonicalName);
			if (!normalizedLabel.isBlank()) {
				candidates.putIfAbsent(normalizedLabel, new CandidateIngredient(canonicalName, ingredient.getId(), normalizedLabel));
			}
			databaseIngredientIdsByName.put(canonicalName.toLowerCase(Locale.ROOT), ingredient.getId());
		}

		for (IngredientSeedData.IngredientData seed : IngredientSeedData.INGREDIENTS) {
			String canonicalName = seed.getName();
			Long ingredientId = databaseIngredientIdsByName.get(canonicalName.toLowerCase(Locale.ROOT));
			addCandidateLabel(candidates, canonicalName, canonicalName, ingredientId);
			for (String alias : seed.getAliases()) {
				addCandidateLabel(candidates, canonicalName, alias, ingredientId);
			}
		}

		return candidates;
	}

	private void addCandidateLabel(Map<String, CandidateIngredient> candidates, String canonicalName, String label, Long ingredientId) {
		String normalizedLabel = normalizeIngredientText(label);
		if (normalizedLabel.isBlank()) {
			return;
		}
		candidates.putIfAbsent(normalizedLabel, new CandidateIngredient(canonicalName, ingredientId, normalizedLabel));
	}

	private CandidateMatch findBestMatch(String normalizedInput, Iterable<CandidateIngredient> candidates) {
		CandidateMatch bestMatch = null;
		for (CandidateIngredient candidate : candidates) {
			double similarity = computeSimilarity(normalizedInput, candidate.normalizedLabel);
			if (bestMatch == null || similarity > bestMatch.similarity) {
				bestMatch = new CandidateMatch(candidate, similarity);
			}
		}
		return bestMatch;
	}

	private double computeSimilarity(String left, String right) {
		if (left.equals(right)) {
			return 1.0;
		}

		if (left.length() >= 4 && right.length() >= 4 && (left.contains(right) || right.contains(left))) {
			return 0.9;
		}

		int distance = levenshteinDistance(left, right);
		int longestLength = Math.max(left.length(), right.length());
		if (longestLength == 0) {
			return 0.0;
		}
		return 1.0 - ((double) distance / longestLength);
	}

	private int levenshteinDistance(String left, String right) {
		int[] previous = new int[right.length() + 1];
		int[] current = new int[right.length() + 1];

		for (int j = 0; j <= right.length(); j++) {
			previous[j] = j;
		}

		for (int i = 1; i <= left.length(); i++) {
			current[0] = i;
			for (int j = 1; j <= right.length(); j++) {
				int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(
						Math.min(current[j - 1] + 1, previous[j] + 1),
						previous[j - 1] + cost);
			}
			int[] temp = previous;
			previous = current;
			current = temp;
		}

		return previous[right.length()];
	}

	private String normalizeIngredientText(String rawText) {
		String value = rawText == null ? "" : rawText.toLowerCase(Locale.ROOT).trim();
		value = LEADING_LIST_MARKER.matcher(value).replaceFirst("");
		value = STARTS_WITH_QUANTITY.matcher(value).replaceFirst("");
		value = STARTS_WITH_UNIT.matcher(value).replaceFirst("");
		value = NON_ALNUM.matcher(value).replaceAll(" ");
		value = MULTI_SPACE.matcher(value).replaceAll(" ").trim();

		if (value.isBlank()) {
			return value;
		}

		StringBuilder normalized = new StringBuilder();
		for (String token : value.split(" ")) {
			String singular = singularize(token);
			if (!normalized.isEmpty()) {
				normalized.append(' ');
			}
			normalized.append(singular);
		}
		return normalized.toString();
	}

	private String singularize(String token) {
		if (token.endsWith("ies") && token.length() > 4) {
			return token.substring(0, token.length() - 3) + "y";
		}
		if (token.endsWith("oes") && token.length() > 4) {
			return token.substring(0, token.length() - 2);
		}
		if (token.endsWith("es") && token.length() > 3) {
			return token.substring(0, token.length() - 1);
		}
		if (token.endsWith("s") && token.length() > 3) {
			return token.substring(0, token.length() - 1);
		}
		return token;
	}

	private static class CandidateIngredient {
		private final String canonicalName;
		private final Long ingredientId;
		private final String normalizedLabel;

		private CandidateIngredient(String canonicalName, Long ingredientId, String normalizedLabel) {
			this.canonicalName = canonicalName;
			this.ingredientId = ingredientId;
			this.normalizedLabel = normalizedLabel;
		}
	}

	private static class CandidateMatch {
		private final CandidateIngredient candidate;
		private final double similarity;

		private CandidateMatch(CandidateIngredient candidate, double similarity) {
			this.candidate = candidate;
			this.similarity = similarity;
		}
	}

	public static class IngredientAutocompleteResult {
		private final String input;
		private final String ingredientName;
		private final Long ingredientId;
		private final double similarity;
		private final boolean matched;

		public IngredientAutocompleteResult(String input, String ingredientName, Long ingredientId, double similarity,
				boolean matched) {
			this.input = input;
			this.ingredientName = ingredientName;
			this.ingredientId = ingredientId;
			this.similarity = similarity;
			this.matched = matched;
		}

		public String getInput() {
			return input;
		}

		public String getIngredientName() {
			return ingredientName;
		}

		public Long getIngredientId() {
			return ingredientId;
		}

		public double getSimilarity() {
			return similarity;
		}

		public boolean isMatched() {
			return matched;
		}
	}
}
