package ch.uzh.ifi.hase.soprafs26.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "pantry_items")
public class PantryItem implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private Integer quantity;

	@ManyToOne
	@JoinColumn(name = "ingredient_id", nullable = false)
	private Ingredient ingredient;

	@ManyToOne
	@JoinColumn(name = "pantry_id", nullable = false)
	@JsonIgnore
	private Pantry pantry;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Integer getQuantity() { return quantity; }
	public void setQuantity(Integer quantity) { this.quantity = quantity; }
	public Ingredient getIngredient() { return ingredient; }
	public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
	public Pantry getPantry() { return pantry; }
	public void setPantry(Pantry pantry) { this.pantry = pantry; }
}
