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

 class MealPlanServiceTest {

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
    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @InjectMocks
    private MealPlanService mealPlanService;

    private User testUser;
    private Group testGroup;
    private Recipe testRecipe;
    private MealPlan testPlan;

    @BeforeEach
    void setup() {
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
        RecipeIngredient ri = new RecipeIngredient();
        ri.setIngredient(ing1);
        ri.setQuantity(200);
        ri.setUnit(Unit.GRAM);
        ri.setRecipe(testRecipe);
        testRecipe.setIngredients(Collections.singletonList(ri));

        testPlan = new MealPlan();
        testPlan.setId(1L);
        testPlan.setUserID("user-1");
        testPlan.setRecipe(testRecipe);
        testPlan.setDate(LocalDate.now());
    }

    @Test
     void getMealPlans_success() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(1);
        
        when(mealPlanRepository.findByUserIDAndDateBetween("user-1", start, end))
                .thenReturn(new ArrayList<>(Collections.singletonList(testPlan)));
        GroupMembership membership = new GroupMembership();
        membership.setGroup(testGroup);
        when(groupMembershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(membership));
        
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
     void createMealPlan_success() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(mealPlanRepository.save(any())).thenReturn(testPlan);

        MealPlan result = mealPlanService.createMealPlan(testPlan);

        assertNotNull(result);
        assertEquals(testRecipe, result.getRecipe());
        verify(mealPlanRepository, times(1)).save(any());
    }

    @Test
     void createMealPlan_noRecipe_throwsBadRequest() {
        testPlan.setRecipe(null);
        assertThrows(ResponseStatusException.class, () -> mealPlanService.createMealPlan(testPlan));
    }

    @Test
     void deleteMealPlan_owner_success() {
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));

        mealPlanService.deleteMealPlan(1L, "user-1");

        verify(mealPlanRepository, times(1)).delete(testPlan);
    }

    @Test
     void deleteMealPlan_groupMember_success() {
        testPlan.setUserID("user-2");
        testPlan.setGroupId(1L);
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        GroupMembership membership = new GroupMembership();
        membership.setGroup(testGroup);
        when(groupMembershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(membership));

        mealPlanService.deleteMealPlan(1L, "user-1");

        verify(mealPlanRepository, times(1)).delete(testPlan);
    }

    @Test
     void deleteMealPlan_unauthorized_throwsForbidden() {
        testPlan.setUserID("user-2");
        testPlan.setGroupId(2L);
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        GroupMembership membership = new GroupMembership();
        membership.setGroup(testGroup);
        when(groupMembershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(membership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> mealPlanService.deleteMealPlan(1L, "user-1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
     void getMissingIngredients_calculationSuccess() {
         LocalDate now = LocalDate.now();
         when(mealPlanRepository.findByUserIDAndDateBetween(any(), any(), any()))
                 .thenReturn(new ArrayList<>(Collections.singletonList(testPlan)));
         GroupMembership membership = new GroupMembership();
         membership.setGroup(testGroup);
         when(groupMembershipRepository.findByUserUserID(any())).thenReturn(Optional.of(membership));

         Pantry pantry = new Pantry();
         PantryItem item = new PantryItem();
         Ingredient pantryIng = new Ingredient();
         pantryIng.setIngredientName("Pasta");
         pantryIng.setUnit(Unit.GRAM);
         item.setIngredient(pantryIng);
         item.setUnit(Unit.GRAM);
         item.setQuantity(50); // 150 missing (200 needed)
         pantry.setItems(Collections.singletonList(item));

         when(pantryService.getPantryByGroupId(1L)).thenReturn(pantry);

         Map<Ingredient, Integer> missing = mealPlanService.getMissingIngredients("user-1", now, now);

         assertEquals(1, missing.size());
         assertEquals(150, missing.values().iterator().next());
     }

    @Test
     void syncToShoppingList_success() {
        LocalDate now = LocalDate.now();
        when(groupService.getGroupOfUser("user-1")).thenReturn(testGroup);
        GroupMembership membership = new GroupMembership();
        membership.setGroup(testGroup);
        when(groupMembershipRepository.findByUserUserID(any())).thenReturn(Optional.of(membership));
        
        ShoppingList list = new ShoppingList();
        list.setId(1L);
        when(shoppingListService.getShoppingListByGroupId(1L)).thenReturn(list);
        
        // Mock missing ingredients
        when(mealPlanRepository.findByUserIDAndDateBetween(any(), any(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList(testPlan)));
        when(pantryService.getPantryByGroupId(any())).thenReturn(new Pantry()); // empty pantry

        // Mock base ingredient finding/creation
        Ingredient baseIng = new Ingredient();
        baseIng.setId(10L);
        baseIng.setIngredientName("Pasta");
        when(ingredientRepository.findByIngredientNameIgnoreCase("Pasta"))
                  .thenReturn(Collections.singletonList(baseIng));
          when(ingredientRepository.findByIngredientNameIgnoreCaseAndUnitAndUser("Pasta", Unit.GRAM, null))
                  .thenReturn(Optional.of(baseIng));
        when(ingredientRepository.save(any())).thenReturn(baseIng);

        mealPlanService.syncToShoppingList("user-1", now, now);

        verify(shoppingListService, times(1)).addItemToList(eq(1L), eq(10L), eq(200));
    }
}
