package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.entity.MealPlan;
import java.time.LocalDate;

public class MealPlanGetDTO {
    private Long id;
    private LocalDate date;
    private MealPlan.MealType mealType;
    private String userID;
    private Long groupId;
    private RecipeGetDTO recipe;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public MealPlan.MealType getMealType() { return mealType; }
    public void setMealType(MealPlan.MealType mealType) { this.mealType = mealType; }
    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public RecipeGetDTO getRecipe() { return recipe; }
    public void setRecipe(RecipeGetDTO recipe) { this.recipe = recipe; }
}
