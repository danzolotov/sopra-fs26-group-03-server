package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.GroupRole;
import ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MealPlanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMembershipRepository groupMembershipRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private PantryRepository pantryRepository;

    @Autowired
    private PantryItemRepository pantryItemRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingListItemRepository shoppingListItemRepository;

    private User testUser;
    private Group testGroup;
    private Recipe testRecipe;

    @BeforeEach
    void setup() {
        mealPlanRepository.deleteAll();
        pantryItemRepository.deleteAll();
        pantryRepository.deleteAll();
        shoppingListItemRepository.deleteAll();
        shoppingListRepository.deleteAll();
        groupMembershipRepository.deleteAll();
        groupRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create a test user
        testUser = new User();
        testUser.setEmail("test@platemate.ch");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hashed-password");
        testUser.setToken("valid-token");
        testUser.setStatus(UserStatus.ONLINE);
        testUser = userRepository.saveAndFlush(testUser);

        // 2. Create a test group & membership
        testGroup = new Group();
        testGroup.setName("Test Group");
        testGroup.setInviteCode("INVITE123");
        testGroup = groupRepository.saveAndFlush(testGroup);

        GroupMembership membership = new GroupMembership();
        membership.setUser(testUser);
        membership.setGroup(testGroup);
        membership.setRole(GroupRole.MEMBER);
        groupMembershipRepository.saveAndFlush(membership);

        // Initialize Pantry & ShoppingList for the group
        Pantry pantry = new Pantry();
        pantry.setGroupId(testGroup.getId());
        pantryRepository.saveAndFlush(pantry);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setGroupId(testGroup.getId());
        shoppingListRepository.saveAndFlush(shoppingList);

        // 3. Create a test recipe with ingredients
        testRecipe = new Recipe();
        testRecipe.setName("Pasta Carbonara");
        testRecipe.setDescription("Classic Carbonara");
        testRecipe.setIngredients(new ArrayList<>());

        Ingredient pasta = new Ingredient();
        pasta.setIngredientName("Spaghetti");
        pasta.setUnit(Unit.GRAM);
        pasta.setQuantity(200);
        pasta.setCategory(IngredientCategory.GRAIN);

        testRecipe.getIngredients().add(pasta);
        testRecipe = recipeRepository.saveAndFlush(testRecipe);
    }

    @Test
    void createMealPlan_groupMember_automaticallySetsGroupId() throws Exception {
        String payload = String.format("{\"recipeId\":%d,\"date\":\"%s\",\"mealType\":\"DINNER\"}",
                testRecipe.getId(), LocalDate.now());

        MockHttpServletRequestBuilder request = post("/meal-plans")
                .cookie(new MockCookie("AUTH_TOKEN", "valid-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload);

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.groupId").value(testGroup.getId()))
                .andExpect(jsonPath("$.recipe.id").value(testRecipe.getId()));
    }

    @Test
    void getMealPlans_returnsGroupSharedAndPersonalPlans() throws Exception {
        // Save a meal plan belonging to the group
        MealPlan groupPlan = new MealPlan();
        groupPlan.setRecipe(testRecipe);
        groupPlan.setDate(LocalDate.now());
        groupPlan.setMealType(MealPlan.MealType.DINNER);
        groupPlan.setUserID(testUser.getUserID());
        groupPlan.setGroupId(testGroup.getId());
        mealPlanRepository.saveAndFlush(groupPlan);

        MockHttpServletRequestBuilder request = get("/meal-plans")
                .cookie(new MockCookie("AUTH_TOKEN", "valid-token"))
                .param("startDate", LocalDate.now().minusDays(1).toString())
                .param("endDate", LocalDate.now().plusDays(1).toString())
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].groupId").value(testGroup.getId()));
    }

    @Test
    void getMissingIngredients_comparesMealPlanToGroupPantry() throws Exception {
        // Create a meal plan for today
        MealPlan plan = new MealPlan();
        plan.setRecipe(testRecipe); // Needs 200g Spaghetti
        plan.setDate(LocalDate.now());
        plan.setMealType(MealPlan.MealType.DINNER);
        plan.setUserID(testUser.getUserID());
        plan.setGroupId(testGroup.getId());
        mealPlanRepository.saveAndFlush(plan);

        // Put 50g Spaghetti in the group pantry (so 150g is missing)
        Ingredient baseSpaghetti = new Ingredient();
        baseSpaghetti.setIngredientName("Spaghetti");
        baseSpaghetti.setUnit(Unit.GRAM);
        baseSpaghetti.setCategory(IngredientCategory.GRAIN);
        baseSpaghetti = ingredientRepository.saveAndFlush(baseSpaghetti);

        Pantry pantry = pantryRepository.findAllByGroupId(testGroup.getId()).stream().findFirst().orElseThrow();
        PantryItem item = new PantryItem();
        item.setPantry(pantry);
        item.setIngredient(baseSpaghetti);
        item.setQuantity(50);
        item.setUnit(Unit.GRAM);
        pantryItemRepository.saveAndFlush(item);

        MockHttpServletRequestBuilder request = get("/meal-plans/missing-ingredients")
                .cookie(new MockCookie("AUTH_TOKEN", "valid-token"))
                .param("startDate", LocalDate.now().minusDays(1).toString())
                .param("endDate", LocalDate.now().plusDays(1).toString())
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ingredient.ingredientName").value("Spaghetti"))
                .andExpect(jsonPath("$[0].missingQuantity").value(150));
    }

    @Test
    void syncToShoppingList_populatesMissingIngredientsIntoGroupList() throws Exception {
        // Create a meal plan for today
        MealPlan plan = new MealPlan();
        plan.setRecipe(testRecipe); // Needs 200g Spaghetti
        plan.setDate(LocalDate.now());
        plan.setMealType(MealPlan.MealType.DINNER);
        plan.setUserID(testUser.getUserID());
        plan.setGroupId(testGroup.getId());
        mealPlanRepository.saveAndFlush(plan);

        // Call sync
        MockHttpServletRequestBuilder request = post("/meal-plans/sync-shopping-list")
                .cookie(new MockCookie("AUTH_TOKEN", "valid-token"))
                .param("startDate", LocalDate.now().minusDays(1).toString())
                .param("endDate", LocalDate.now().plusDays(1).toString())
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        // Verify that the shopping list now has 200g Spaghetti
        mockMvc.perform(get("/groups/me/shopping-list")
                .cookie(new MockCookie("AUTH_TOKEN", "valid-token"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].ingredientName").value("Spaghetti"))
                .andExpect(jsonPath("$.items[0].quantity").value(200));
    }
}
