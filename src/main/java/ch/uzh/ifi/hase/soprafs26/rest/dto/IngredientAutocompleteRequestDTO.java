package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class IngredientAutocompleteRequestDTO {
	private List<String> foundIngredients;

	public List<String> getFoundIngredients() {
		return foundIngredients;
	}

	public void setFoundIngredients(List<String> foundIngredients) {
		this.foundIngredients = foundIngredients;
	}
}

