package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory;

public class IngredientPostDTO {
	private String ingredientName;
	private String ingredientDescription;
	private Unit standardUnit;
	private IngredientCategory category;

	public String getIngredientName() {
		return ingredientName;
	}

	public void setIngredientName(String ingredientName) {
		this.ingredientName = ingredientName;
	}

	public String getIngredientDescription() {
		return ingredientDescription;
	}

	public void setIngredientDescription(String ingredientDescription) {
		this.ingredientDescription = ingredientDescription;
	}

	public Unit getStandardUnit() {
		return standardUnit;
	}

	public void setStandardUnit(Unit standardUnit) {
		this.standardUnit = standardUnit;
	}

	public IngredientCategory getCategory() {
		return category;
	}

	public void setCategory(IngredientCategory category) {
		this.category = category;
	}
}

