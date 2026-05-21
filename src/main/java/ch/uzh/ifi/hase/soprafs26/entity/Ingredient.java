package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory;
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

	@Column(nullable = false)
	private String ingredientName;

	@Column(nullable = true)
	private String ingredientDescription;

    @Enumerated(EnumType.STRING)
    private IngredientCategory category;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Unit unit;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

	@Column(nullable = true)
	private Integer quantity;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getIngredientName() { return ingredientName; }
	public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
	public String getIngredientDescription() { return ingredientDescription; }
	public void setIngredientDescription(String ingredientDescription) { this.ingredientDescription = ingredientDescription; }
	public Unit getUnit() { return unit; }
	public void setUnit(Unit unit) { this.unit = unit; }
	public Integer getQuantity() { return quantity; }
	public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public IngredientCategory getCategory() {
        return category;
    }

    public void setCategory(IngredientCategory category) {
        this.category = category;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
