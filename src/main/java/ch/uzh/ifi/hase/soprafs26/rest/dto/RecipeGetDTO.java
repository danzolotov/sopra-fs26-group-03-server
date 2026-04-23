package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class RecipeGetDTO {
    private Long id;
    private String name;
    private String description;
    private List<IngredientGetDTO> ingredients;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<IngredientGetDTO> getIngredients() { return ingredients; }
    public void setIngredients(List<IngredientGetDTO> ingredients) { this.ingredients = ingredients; }
}
