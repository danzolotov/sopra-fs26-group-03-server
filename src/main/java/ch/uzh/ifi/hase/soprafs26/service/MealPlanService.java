package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
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
    private final MealPlanRepository mealPlanRepository;
    private final RecipeRepository recipeRepository;
    private final PantryService pantryService;
    private final ShoppingListService shoppingListService;
    private final GroupService groupService;
    private final IngredientRepository ingredientRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    @Autowired
    public MealPlanService(MealPlanRepository mealPlanRepository,
                           RecipeRepository recipeRepository,
                           PantryService pantryService,
                           ShoppingListService shoppingListService,
                           GroupService groupService,
                           IngredientRepository ingredientRepository,
                           GroupMembershipRepository groupMembershipRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.recipeRepository = recipeRepository;
        this.pantryService = pantryService;
        this.shoppingListService = shoppingListService;
        this.groupService = groupService;
        this.ingredientRepository = ingredientRepository;
        this.groupMembershipRepository = groupMembershipRepository;
    }

    public List<MealPlan> getMealPlans(String userID, LocalDate start, LocalDate end) {
        List<MealPlan> plans = new ArrayList<>(mealPlanRepository.findByUserIDAndDateBetween(userID, start, end));
        
        groupMembershipRepository.findByUserUserID(userID).ifPresent(membership -> {
            Group group = membership.getGroup();
            List<MealPlan> groupPlans = mealPlanRepository.findByGroupIdAndDateBetween(group.getId(), start, end);
            for (MealPlan gp : groupPlans) {
                if (plans.stream().noneMatch(p -> p.getId().equals(gp.getId()))) {
                    plans.add(gp);
                }
            }
        });
        
        return plans;
    }

    public MealPlan createMealPlan(MealPlan plan) {
        if (plan.getRecipe() == null || plan.getRecipe().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe is required");
        }
        if (plan.getGroupId() == null) {
            groupMembershipRepository.findByUserUserID(plan.getUserID()).ifPresent(membership -> {
                plan.setGroupId(membership.getGroup().getId());
            });
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
             groupMembershipRepository.findByUserUserID(userID).ifPresentOrElse(membership -> {
                 Group group = membership.getGroup();
                 if (plan.getGroupId() == null || !plan.getGroupId().equals(group.getId())) {
                     throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this meal plan");
                 }
             }, () -> {
                 throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this meal plan");
             });
        }
        
        mealPlanRepository.delete(plan);
    }

    public Map<Ingredient, Integer> getMissingIngredients(String userID, LocalDate start, LocalDate end) {
        List<MealPlan> plans = getMealPlans(userID, start, end);
        
          // Group by name + unit so ingredients with the same name but different units stay separate
          Map<String, Integer> requiredByKey = new HashMap<>();
          Map<String, Ingredient> prototypeMap = new HashMap<>();

         for (MealPlan plan : plans) {
             for (ch.uzh.ifi.hase.soprafs26.entity.Ingredient ing : plan.getRecipe().getIngredients()) {
                  String nameKey = buildIngredientKey(ing.getIngredientName(), ing.getUnit());
                 int qty = ing.getQuantity() == null ? 0 : ing.getQuantity();
                  requiredByKey.put(nameKey, requiredByKey.getOrDefault(nameKey, 0) + qty);
                 prototypeMap.putIfAbsent(nameKey, ing);
             }
         }

          Map<String, Integer> stockByKey = new HashMap<>();
        groupMembershipRepository.findByUserUserID(userID).ifPresent(membership -> {
            Group group = membership.getGroup();
            Pantry pantry = pantryService.getPantryByGroupId(group.getId());
            for (PantryItem item : pantry.getItems()) {
                  String nameKey = buildIngredientKey(item.getIngredient().getIngredientName(), item.getUnit());
                   stockByKey.put(nameKey, stockByKey.getOrDefault(nameKey, 0) + item.getQuantity());
            }
            try {
                ShoppingList shoppingList = shoppingListService.getShoppingListByGroupId(group.getId());
                if (shoppingList != null && shoppingList.getItems() != null) {
                    for (ShoppingListItem item : shoppingList.getItems()) {
                        if (item.getIngredient() != null && !Boolean.TRUE.equals(item.getIsBought())) {
                            String nameKey = buildIngredientKey(item.getIngredient().getIngredientName(), item.getUnit());
                            stockByKey.put(nameKey, stockByKey.getOrDefault(nameKey, 0) + item.getQuantity());
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore if no shopping list is found
            }
        });

        Map<Ingredient, Integer> missing = new HashMap<>();
          for (Map.Entry<String, Integer> entry : requiredByKey.entrySet()) {
            int needed = entry.getValue();
              int available = stockByKey.getOrDefault(entry.getKey(), 0);
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
              Ingredient baseIng = ingredientRepository.findByIngredientNameIgnoreCase(reqIng.getIngredientName()).stream()
                      .filter(i -> i.getUser() == null)
                      .findFirst()
                      .orElseGet(() -> {
                          Ingredient ni = new Ingredient();
                          ni.setIngredientName(reqIng.getIngredientName());
                          ni.setUnit(reqIng.getUnit());
                          if (reqIng.getCategory() != null) {
                              ni.setCategory(reqIng.getCategory());
                          } else {
                              ni.setCategory(ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory.OTHER);
                          }
                          return ingredientRepository.save(ni);
                      });

            if (baseIng.getCategory() == null) {
                if (reqIng.getCategory() != null) {
                    baseIng.setCategory(reqIng.getCategory());
                } else {
                    baseIng.setCategory(ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory.OTHER);
                }
                baseIng = ingredientRepository.save(baseIng);
            }
            
            shoppingListService.addItemToList(list.getId(), baseIng.getId(), entry.getValue());
        }
    }

  private String buildIngredientKey(String name, ch.uzh.ifi.hase.soprafs26.constant.Unit unit) {
    return (name == null ? "" : name.toLowerCase()) + "|" + (unit == null ? "" : unit.name());
  }
}
