package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.Pantry;
import ch.uzh.ifi.hase.soprafs26.entity.PantryItem;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemPutDTO;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.PantryService;
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

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PantryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PantryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PantryService pantryService;

	@MockitoBean
	private GroupService groupService;

	@MockitoBean
	private UserService userService;

	private Group testGroup;
	private Pantry testPantry;
	private PantryItem testItem;
	private Ingredient testIngredient;

	@BeforeEach
	public void setup() {
		testGroup = new Group();
		testGroup.setId(1L);

		testPantry = new Pantry();
		testPantry.setId(10L);
		testPantry.setGroupId(1L);

		testIngredient = new Ingredient();
		testIngredient.setId(100L);
		testIngredient.setIngredientName("Apple");

		testItem = new PantryItem();
		testItem.setId(500L);
		testItem.setIngredient(testIngredient);
		testItem.setQuantity(5);
		testItem.setPantry(testPantry);
	}

	@Test
	public void getPantry_success() throws Exception {
		given(groupService.getGroupOfUser("user-1")).willReturn(testGroup);
		given(pantryService.getPantryByGroupId(1L)).willReturn(testPantry);

		mockMvc.perform(get("/groups/me/pantry")
						.principal(new UsernamePasswordAuthenticationToken("user-1", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(10)))
				.andExpect(jsonPath("$.groupId", is(1)));
	}

	@Test
	public void addItem_success() throws Exception {
		PantryItemPostDTO dto = new PantryItemPostDTO();
		dto.setIngredientId(100L);
		dto.setQuantity(5);

		given(groupService.getGroupOfUser("user-1")).willReturn(testGroup);
		given(pantryService.getPantryByGroupId(1L)).willReturn(testPantry);
		given(pantryService.addItemToPantry(10L, 100L, 5)).willReturn(testItem);

		mockMvc.perform(post("/groups/me/pantry/items")
						.principal(new UsernamePasswordAuthenticationToken("user-1", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"ingredientId\":100,\"quantity\":5}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(500)))
				.andExpect(jsonPath("$.quantity", is(5)))
				.andExpect(jsonPath("$.ingredientName", is("Apple")));
	}

	@Test
	public void updateItem_success() throws Exception {
		PantryItemPutDTO dto = new PantryItemPutDTO();
		dto.setIngredientId(100L);
		dto.setQuantity(10);

		given(groupService.getGroupOfUser("user-1")).willReturn(testGroup);
		doNothing().when(pantryService).updateItem(eq(500L), eq(100L), eq(10));

		mockMvc.perform(put("/groups/me/pantry/items/{itemId}", 500L)
						.principal(new UsernamePasswordAuthenticationToken("user-1", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"ingredientId\":100,\"quantity\":10}"))
				.andExpect(status().isNoContent());

		verify(pantryService).updateItem(eq(500L), eq(100L), eq(10));
	}

	@Test
	public void deleteItem_success() throws Exception {
		given(groupService.getGroupOfUser("user-1")).willReturn(testGroup);
		doNothing().when(pantryService).deleteItem(500L);

		mockMvc.perform(delete("/groups/me/pantry/items/{itemId}", 500L)
						.principal(new UsernamePasswordAuthenticationToken("user-1", null)))
				.andExpect(status().isNoContent());

		verify(pantryService).deleteItem(500L);
	}
}
