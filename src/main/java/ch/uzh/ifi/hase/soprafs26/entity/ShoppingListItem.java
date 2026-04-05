package ch.uzh.ifi.hase.soprafs26.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "shopping_list_items")
public class ShoppingListItem implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false)
	private Boolean isBought = false;

	@ManyToOne
	@JoinColumn(name = "ingredient_id", nullable = false)
	private Ingredient ingredient;

	@ManyToOne
	@JoinColumn(name = "shopping_list_id", nullable = false)
	@JsonIgnore
	private ShoppingList shoppingList;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Integer getQuantity() { return quantity; }
	public void setQuantity(Integer quantity) { this.quantity = quantity; }
	public Boolean getIsBought() { return isBought; }
	public void setIsBought(Boolean isBought) { this.isBought = isBought; }
	public Ingredient getIngredient() { return ingredient; }
	public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
	public ShoppingList getShoppingList() { return shoppingList; }
	public void setShoppingList(ShoppingList shoppingList) { this.shoppingList = shoppingList; }
}
