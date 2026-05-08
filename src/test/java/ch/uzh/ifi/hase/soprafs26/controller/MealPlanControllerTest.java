package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.MealPlan;
import ch.uzh.ifi.hase.soprafs26.entity.Recipe;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MealPlanPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.MealPlanService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealPlanController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MealPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MealPlanService mealPlanService;

    @MockitoBean
    private UserService userService;

    @Test
    public void getMealPlans_success() throws Exception {
        MealPlan plan = new MealPlan();
        plan.setId(1L);
        plan.setUserID("user-1");
        
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setName("Pasta");
        plan.setRecipe(recipe);

        given(mealPlanService.getMealPlans(eq("user-1"), any(), any()))
                .willReturn(Collections.singletonList(plan));

        mockMvc.perform(get("/meal-plans")
                .principal(new UsernamePasswordAuthenticationToken("user-1", null))
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].recipe.name").value("Pasta"));
    }

    @Test
    public void createMealPlan_success() throws Exception {
        MealPlanPostDTO dto = new MealPlanPostDTO();
        dto.setRecipeId(1L);
        dto.setDate(LocalDate.now());

        MealPlan saved = new MealPlan();
        saved.setId(1L);
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        saved.setRecipe(recipe);

        given(mealPlanService.createMealPlan(any())).willReturn(saved);

        mockMvc.perform(post("/meal-plans")
                .principal(new UsernamePasswordAuthenticationToken("user-1", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeId\":1,\"date\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void deleteMealPlan_success() throws Exception {
        mockMvc.perform(delete("/meal-plans/1")
                .principal(new UsernamePasswordAuthenticationToken("user-1", null)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void getMissingIngredients_success() throws Exception {
        Ingredient ing = new Ingredient();
        ing.setIngredientName("Pasta");
        Map<Ingredient, Integer> missing = new HashMap<>();
        missing.put(ing, 200);

        given(mealPlanService.getMissingIngredients(eq("user-1"), any(), any()))
                .willReturn(missing);

        mockMvc.perform(get("/meal-plans/missing-ingredients")
                .principal(new UsernamePasswordAuthenticationToken("user-1", null))
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ingredient.ingredientName").value("Pasta"))
                .andExpect(jsonPath("$[0].missingQuantity").value(200));
    }

    @Test
    public void syncToShoppingList_success() throws Exception {
        mockMvc.perform(post("/meal-plans/sync-shopping-list")
                .principal(new UsernamePasswordAuthenticationToken("user-1", null))
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-07"))
                .andExpect(status().isNoContent());
    }
}
