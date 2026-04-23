package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class MealPlanService {

    private final Logger log = LoggerFactory.getLogger(MealPlanService.class);

    private final MealPlanRepository mealPlanRepository;
    private final RecipeRepository recipeRepository;
    private final PantryService pantryService;
    private final ShoppingListService shoppingListService;
    private final GroupService groupService;
    private final IngredientRepository ingredientRepository;

    @Autowired
    public MealPlanService(MealPlanRepository mealPlanRepository,
                           RecipeRepository recipeRepository,
                           PantryService pantryService,
                           ShoppingListService shoppingListService,
                           GroupService groupService,
                           IngredientRepository ingredientRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.recipeRepository = recipeRepository;
        this.pantryService = pantryService;
        this.shoppingListService = shoppingListService;
        this.groupService = groupService;
        this.ingredientRepository = ingredientRepository;
    }

    public List<MealPlan> getMealPlans(String userID, LocalDate start, LocalDate end) {
        List<MealPlan> plans = mealPlanRepository.findByUserIDAndDateBetween(userID, start, end);
        
        try {
            Group group = groupService.getGroupOfUser(userID);
            List<MealPlan> groupPlans = mealPlanRepository.findByGroupIdAndDateBetween(group.getId(), start, end);
            for (MealPlan gp : groupPlans) {
                if (plans.stream().noneMatch(p -> p.getId().equals(gp.getId()))) {
                    plans.add(gp);
                }
            }
        } catch (ResponseStatusException e) {
        }
        
        return plans;
    }

    public MealPlan createMealPlan(MealPlan plan) {
        if (plan.getRecipe() == null || plan.getRecipe().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe is required");
        }
        Recipe recipe = recipeRepository.findById(plan.getRecipe().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        plan.setRecipe(recipe);
        return mealPlanRepository.save(plan);
    }

    public void deleteMealPlan(Long planId, String userID) {
        MealPlan plan = mealPlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal plan not found"));
        
        if (!plan.getUserID().equals(userID)) {
             try {
                 Group group = groupService.getGroupOfUser(userID);
                 if (plan.getGroupId() == null || !plan.getGroupId().equals(group.getId())) {
                     throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this meal plan");
                 }
             } catch (ResponseStatusException e) {
                 throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this meal plan");
             }
        }
        
        mealPlanRepository.delete(plan);
    }

    public Map<Ingredient, Integer> getMissingIngredients(String userID, LocalDate start, LocalDate end) {
        List<MealPlan> plans = getMealPlans(userID, start, end);
        
        // Group by Name since Ingredient IDs are now unique per recipe usage
        Map<String, Integer> requiredByName = new HashMap<>();
        Map<String, Ingredient> prototypeMap = new HashMap<>();

        for (MealPlan plan : plans) {
            for (Ingredient ing : plan.getRecipe().getIngredients()) {
                String nameKey = ing.getIngredientName().toLowerCase();
                requiredByName.put(nameKey, requiredByName.getOrDefault(nameKey, 0) + ing.getQuantity());
                prototypeMap.putIfAbsent(nameKey, ing);
            }
        }

        Map<String, Integer> stockByName = new HashMap<>();
        try {
            Group group = groupService.getGroupOfUser(userID);
            Pantry pantry = pantryService.getPantryByGroupId(group.getId());
            for (PantryItem item : pantry.getItems()) {
                String nameKey = item.getIngredient().getIngredientName().toLowerCase();
                stockByName.put(nameKey, stockByName.getOrDefault(nameKey, 0) + item.getQuantity());
            }
        } catch (ResponseStatusException e) {
        }

        Map<Ingredient, Integer> missing = new HashMap<>();
        for (Map.Entry<String, Integer> entry : requiredByName.entrySet()) {
            int needed = entry.getValue();
            int available = stockByName.getOrDefault(entry.getKey(), 0);
            if (needed > available) {
                missing.put(prototypeMap.get(entry.getKey()), needed - available);
            }
        }

        return missing;
    }

    public void syncToShoppingList(String userID, LocalDate start, LocalDate end) {
        Group group = groupService.getGroupOfUser(userID);
        ShoppingList list = shoppingListService.getShoppingListByGroupId(group.getId());
        
        Map<Ingredient, Integer> missing = getMissingIngredients(userID, start, end);
        for (Map.Entry<Ingredient, Integer> entry : missing.entrySet()) {
            Ingredient reqIng = entry.getKey();
            // Find or create a base ingredient (recipeId=null) for the shopping list
            Ingredient baseIng = ingredientRepository.findByIngredientNameIgnoreCase(reqIng.getIngredientName())
                .stream()
                .filter(i -> i.getRecipe() == null)
                .findFirst()
                .orElseGet(() -> {
                    Ingredient ni = new Ingredient();
                    ni.setIngredientName(reqIng.getIngredientName());
                    ni.setUnit(reqIng.getUnit());
                    return ingredientRepository.save(ni);
                });
            
            shoppingListService.addItemToList(list.getId(), baseIng.getId(), entry.getValue());
        }
    }
}
