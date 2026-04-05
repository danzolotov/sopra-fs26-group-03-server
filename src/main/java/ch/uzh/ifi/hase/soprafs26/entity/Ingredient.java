package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import java.io.Serializable;

@Entity
@Table(name = "ingredients")
public class Ingredient implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false, unique = true)
	private String ingredientName;

	@Column(nullable = true)
	private String ingredientDescription;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Unit unit;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getIngredientName() { return ingredientName; }
	public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
	public String getIngredientDescription() { return ingredientDescription; }
	public void setIngredientDescription(String ingredientDescription) { this.ingredientDescription = ingredientDescription; }
	public Unit getUnit() { return unit; }
	public void setUnit(Unit unit) { this.unit = unit; }
}
