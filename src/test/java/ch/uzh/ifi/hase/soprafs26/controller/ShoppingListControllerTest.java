package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.ShoppingListService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShoppingListController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ShoppingListControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShoppingListService shoppingListService;

	@MockitoBean
	private GroupService groupService;

	@MockitoBean
	private ch.uzh.ifi.hase.soprafs26.service.ShoppingListAutoDetectService shoppingListAutoDetectService;

	@MockitoBean
	private ch.uzh.ifi.hase.soprafs26.service.IngredientService ingredientService;

	@MockitoBean
	private ch.uzh.ifi.hase.soprafs26.service.UserService userService;

	private Group testGroup;
	private ShoppingList testList;
	private ShoppingListItem testItem;
	private Ingredient testIngredient;

	@BeforeEach
	public void setup() {
		testGroup = new Group();
		testGroup.setId(1L);

		testList = new ShoppingList();
		testList.setId(10L);
		testList.setGroupId(1L);

		testIngredient = new Ingredient();
		testIngredient.setId(100L);
		testIngredient.setIngredientName("Milk");

		testItem = new ShoppingListItem();
		testItem.setId(500L);
		testItem.setIngredient(testIngredient);
		testItem.setQuantity(2);
		testItem.setIsBought(false);
		testItem.setShoppingList(testList);
	}

	@Test
	public void getShoppingList_success() throws Exception {
		given(groupService.getGroupOfUser("user-1")).willReturn(testGroup);
		given(shoppingListService.getShoppingListByGroupId(1L)).willReturn(testList);

		mockMvc.perform(get("/groups/me/shopping-list")
						.principal(new UsernamePasswordAuthenticationToken("user-1", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(10)));
	}

	@Test
	public void addItem_success() throws Exception {
		ShoppingListItemPostDTO dto = new ShoppingListItemPostDTO();
		dto.setIngredientId(100L);
		dto.setQuantity(2);

		given(groupService.getGroupOfUser("user-1")).willReturn(testGroup);
		given(shoppingListService.getShoppingListByGroupId(1L)).willReturn(testList);
		given(shoppingListService.addItemToList(10L, 100L, 2)).willReturn(testItem);

		mockMvc.perform(post("/groups/me/shopping-list/items")
						.principal(new UsernamePasswordAuthenticationToken("user-1", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"ingredientId\":100,\"quantity\":2}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(500)))
				.andExpect(jsonPath("$.ingredientName", is("Milk")));
	}

	@Test
	public void patchItemBoughtStatus_success() throws Exception {
		ItemPatchDTO dto = new ItemPatchDTO();
		dto.setIsBought(true);

		testItem.setIsBought(true);

		given(groupService.getGroupOfUser("user-1")).willReturn(testGroup);
		given(shoppingListService.patchItemBoughtStatus(500L, true)).willReturn(testItem);

		mockMvc.perform(patch("/groups/me/shopping-list/items/{itemId}", 500L)
						.principal(new UsernamePasswordAuthenticationToken("user-1", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"isBought\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isBought", is(true)));

		verify(shoppingListService).patchItemBoughtStatus(500L, true);
	}
}
