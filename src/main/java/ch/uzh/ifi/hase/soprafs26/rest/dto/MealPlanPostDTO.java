package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.entity.MealPlan;
import java.time.LocalDate;

public class MealPlanPostDTO {
    private LocalDate date;
    private MealPlan.MealType mealType;
    private Long recipeId;
    private Long groupId;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public MealPlan.MealType getMealType() { return mealType; }
    public void setMealType(MealPlan.MealType mealType) { this.mealType = mealType; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
}
