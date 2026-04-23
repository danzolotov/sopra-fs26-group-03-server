package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class MissingIngredientGetDTO {
    private IngredientGetDTO ingredient;
    private Integer missingQuantity;

    public IngredientGetDTO getIngredient() { return ingredient; }
    public void setIngredient(IngredientGetDTO ingredient) { this.ingredient = ingredient; }
    public Integer getMissingQuantity() { return missingQuantity; }
    public void setMissingQuantity(Integer missingQuantity) { this.missingQuantity = missingQuantity; }
}
