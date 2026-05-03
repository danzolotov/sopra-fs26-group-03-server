package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class RecipePutDTO {
    private String name;
    private String description;
    private List<IngredientPutDTO> ingredients;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<IngredientPutDTO> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<IngredientPutDTO> ingredients) {
        this.ingredients = ingredients;
    }
}
