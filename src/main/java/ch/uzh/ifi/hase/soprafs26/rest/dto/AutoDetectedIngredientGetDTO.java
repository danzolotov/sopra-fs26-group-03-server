package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.Unit;

public class AutoDetectedIngredientGetDTO {
	private Long id;
	private String ingredientName;
	private String ingredientDescription;
	private Unit unit;
	private Integer quantity;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
}

