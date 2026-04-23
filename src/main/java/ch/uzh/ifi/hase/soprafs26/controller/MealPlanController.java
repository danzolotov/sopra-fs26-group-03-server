package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.MealPlan;
import ch.uzh.ifi.hase.soprafs26.entity.Recipe;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.MealPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class MealPlanController {

    private final MealPlanService mealPlanService;

    @Autowired
    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    @GetMapping("/meal-plans")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<MealPlanGetDTO> getMealPlans(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<MealPlan> plans = mealPlanService.getMealPlans(auth.getName(), startDate, endDate);
        List<MealPlanGetDTO> dtos = new ArrayList<>();
        for (MealPlan mp : plans) {
            dtos.add(DTOMapper.INSTANCE.convertEntityToMealPlanGetDTO(mp));
        }
        return dtos;
    }

    @PostMapping("/meal-plans")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public MealPlanGetDTO createMealPlan(Authentication auth, @RequestBody MealPlanPostDTO dto) {
        MealPlan plan = DTOMapper.INSTANCE.convertMealPlanPostDTOtoEntity(dto);
        plan.setUserID(auth.getName());
        
        Recipe recipe = new Recipe();
        recipe.setId(dto.getRecipeId());
        plan.setRecipe(recipe);
        
        MealPlan saved = mealPlanService.createMealPlan(plan);
        return DTOMapper.INSTANCE.convertEntityToMealPlanGetDTO(saved);
    }

    @DeleteMapping("/meal-plans/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMealPlan(Authentication auth, @PathVariable Long id) {
        mealPlanService.deleteMealPlan(id, auth.getName());
    }

    @GetMapping("/meal-plans/missing-ingredients")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<MissingIngredientGetDTO> getMissingIngredients(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<Ingredient, Integer> missing = mealPlanService.getMissingIngredients(auth.getName(), startDate, endDate);
        List<MissingIngredientGetDTO> dtos = new ArrayList<>();
        missing.forEach((ingredient, quantity) -> {
            MissingIngredientGetDTO dto = new MissingIngredientGetDTO();
            dto.setIngredient(DTOMapper.INSTANCE.convertEntityToIngredientGetDTO(ingredient));
            dto.setMissingQuantity(quantity);
            dtos.add(dto);
        });
        return dtos;
    }

    @PostMapping("/meal-plans/sync-shopping-list")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncToShoppingList(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        mealPlanService.syncToShoppingList(auth.getName(), startDate, endDate);
    }
}
