package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.Unit;

public class ShoppingListItemGetDTO {
	private Long id;
	private Integer quantity;
	private Boolean isBought;
	private Long ingredientId;
	private String ingredientName;
	private Unit unit;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Integer getQuantity() { return quantity; }
	public void setQuantity(Integer quantity) { this.quantity = quantity; }
	public Boolean getIsBought() { return isBought; }
	public void setIsBought(Boolean isBought) { this.isBought = isBought; }
	public Long getIngredientId() { return ingredientId; }
	public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
	public String getIngredientName() { return ingredientName; }
	public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
	public Unit getUnit() { return unit; }
	public void setUnit(Unit unit) { this.unit = unit; }
}
