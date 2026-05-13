package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.MealPlan;
import ch.uzh.ifi.hase.soprafs26.entity.Recipe;
import ch.uzh.ifi.hase.soprafs26.entity.ShoppingList;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.MealPlanService;
import ch.uzh.ifi.hase.soprafs26.service.ShoppingListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final GroupService groupService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ShoppingListService shoppingListService;

    @Autowired
    public MealPlanController(MealPlanService mealPlanService, GroupService groupService,
                              SimpMessagingTemplate messagingTemplate, ShoppingListService shoppingListService) {
        this.mealPlanService = mealPlanService;
        this.groupService = groupService;
        this.messagingTemplate = messagingTemplate;
        this.shoppingListService = shoppingListService;
    }

    private void broadcastUpdate(String username) {
        try {
            Group group = groupService.getGroupOfUser(username);
            messagingTemplate.convertAndSend("/topic/meal-plans/" + group.getId(), "REFRESH");
        } catch (Exception e) {
            // User might not be in a group, ignore
        }
    }

    private void broadcastShoppingListUpdate(Long groupId) {
        ShoppingList list = shoppingListService.getShoppingListByGroupId(groupId);
        ShoppingListGetDTO dto = DTOMapper.INSTANCE.convertEntityToShoppingListGetDTO(list);
        messagingTemplate.convertAndSend("/topic/shopping-list/" + groupId, dto);
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
        broadcastUpdate(auth.getName());
        return DTOMapper.INSTANCE.convertEntityToMealPlanGetDTO(saved);
    }

    @DeleteMapping("/meal-plans/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMealPlan(Authentication auth, @PathVariable Long id) {
        mealPlanService.deleteMealPlan(id, auth.getName());
        broadcastUpdate(auth.getName());
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
        broadcastUpdate(auth.getName());
        Group group = groupService.getGroupOfUser(auth.getName());
        broadcastShoppingListUpdate(group.getId());
    }
}
