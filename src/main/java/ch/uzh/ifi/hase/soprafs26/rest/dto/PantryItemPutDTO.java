package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class PantryItemPutDTO {
	private Long ingredientId;
	private Integer quantity;

	public Long getIngredientId() { return ingredientId; }
	public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
	public Integer getQuantity() { return quantity; }
	public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
