package ch.uzh.ifi.hase.soprafs26.rest.dto;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
public class PantryItemPutDTO {
	private Long ingredientId;
	private Integer quantity;
	private Unit unit;

	public Long getIngredientId() { return ingredientId; }
	public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
	public Integer getQuantity() { return quantity; }
	public void setQuantity(Integer quantity) { this.quantity = quantity; }
	public Unit getUnit() { return unit; }
	public void setUnit(Unit unit) { this.unit = unit; }
}
