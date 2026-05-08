package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MealPlanServiceTest {

    @Mock
    private MealPlanRepository mealPlanRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private PantryService pantryService;
    @Mock
    private ShoppingListService shoppingListService;
    @Mock
    private GroupService groupService;
    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private MealPlanService mealPlanService;

    private User testUser;
    private Group testGroup;
    private Recipe testRecipe;
    private MealPlan testPlan;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setUserID("user-1");

        testGroup = new Group();
        testGroup.setId(1L);

        testRecipe = new Recipe();
        testRecipe.setId(1L);
        testRecipe.setName("Test Pasta");

        Ingredient ing1 = new Ingredient();
        ing1.setIngredientName("Pasta");
        ing1.setQuantity(200);
        ing1.setUnit(Unit.GRAM);
        testRecipe.setIngredients(Collections.singletonList(ing1));

        testPlan = new MealPlan();
        testPlan.setId(1L);
        testPlan.setUserID("user-1");
        testPlan.setRecipe(testRecipe);
        testPlan.setDate(LocalDate.now());
    }

    @Test
    public void getMealPlans_success() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(1);
        
        when(mealPlanRepository.findByUserIDAndDateBetween("user-1", start, end))
                .thenReturn(new ArrayList<>(Collections.singletonList(testPlan)));
        when(groupService.getGroupOfUser("user-1")).thenReturn(testGroup);
        
        MealPlan groupPlan = new MealPlan();
        groupPlan.setId(2L);
        groupPlan.setGroupId(1L);
        when(mealPlanRepository.findByGroupIdAndDateBetween(1L, start, end))
                .thenReturn(Collections.singletonList(groupPlan));

        List<MealPlan> result = mealPlanService.getMealPlans("user-1", start, end);

        assertEquals(2, result.size());
        assertTrue(result.contains(testPlan));
        assertTrue(result.contains(groupPlan));
    }

    @Test
    public void createMealPlan_success() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(mealPlanRepository.save(any())).thenReturn(testPlan);

        MealPlan result = mealPlanService.createMealPlan(testPlan);

        assertNotNull(result);
        assertEquals(testRecipe, result.getRecipe());
        verify(mealPlanRepository, times(1)).save(any());
    }

    @Test
    public void createMealPlan_noRecipe_throwsBadRequest() {
        testPlan.setRecipe(null);
        assertThrows(ResponseStatusException.class, () -> mealPlanService.createMealPlan(testPlan));
    }

    @Test
    public void deleteMealPlan_owner_success() {
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));

        mealPlanService.deleteMealPlan(1L, "user-1");

        verify(mealPlanRepository, times(1)).delete(testPlan);
    }

    @Test
    public void deleteMealPlan_groupMember_success() {
        testPlan.setUserID("user-2");
        testPlan.setGroupId(1L);
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(groupService.getGroupOfUser("user-1")).thenReturn(testGroup);

        mealPlanService.deleteMealPlan(1L, "user-1");

        verify(mealPlanRepository, times(1)).delete(testPlan);
    }

    @Test
    public void deleteMealPlan_unauthorized_throwsForbidden() {
        testPlan.setUserID("user-2");
        testPlan.setGroupId(2L);
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(groupService.getGroupOfUser("user-1")).thenReturn(testGroup);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> mealPlanService.deleteMealPlan(1L, "user-1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void getMissingIngredients_calculationSuccess() {
        LocalDate now = LocalDate.now();
        when(mealPlanRepository.findByUserIDAndDateBetween(any(), any(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList(testPlan)));
        when(groupService.getGroupOfUser(any())).thenReturn(testGroup);
        
        Pantry pantry = new Pantry();
        PantryItem item = new PantryItem();
        Ingredient pantryIng = new Ingredient();
        pantryIng.setIngredientName("Pasta");
        item.setIngredient(pantryIng);
        item.setQuantity(50); // 150 missing (200 needed)
        pantry.setItems(Collections.singletonList(item));
        
        when(pantryService.getPantryByGroupId(1L)).thenReturn(pantry);

        Map<Ingredient, Integer> missing = mealPlanService.getMissingIngredients("user-1", now, now);

        assertEquals(1, missing.size());
        assertEquals(150, missing.values().iterator().next());
    }

    @Test
    public void syncToShoppingList_success() {
        LocalDate now = LocalDate.now();
        when(groupService.getGroupOfUser("user-1")).thenReturn(testGroup);
        
        ShoppingList list = new ShoppingList();
        list.setId(1L);
        when(shoppingListService.getShoppingListByGroupId(1L)).thenReturn(list);
        
        // Mock missing ingredients
        Ingredient reqIng = testRecipe.getIngredients().get(0);
        when(mealPlanRepository.findByUserIDAndDateBetween(any(), any(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList(testPlan)));
        when(pantryService.getPantryByGroupId(any())).thenReturn(new Pantry()); // empty pantry

        // Mock base ingredient finding/creation
        Ingredient baseIng = new Ingredient();
        baseIng.setId(10L);
        baseIng.setIngredientName("Pasta");
        when(ingredientRepository.findByIngredientNameIgnoreCase("Pasta"))
                .thenReturn(Collections.singletonList(baseIng));

        mealPlanService.syncToShoppingList("user-1", now, now);

        verify(shoppingListService, times(1)).addItemToList(eq(1L), eq(10L), eq(200));
    }
}
