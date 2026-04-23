package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ShoppingListServiceTest {

	@Mock
	private ShoppingListRepository shoppingListRepository;

	@Mock
	private ShoppingListItemRepository shoppingListItemRepository;

	@Mock
	private IngredientRepository ingredientRepository;

	@Mock
	private PantryRepository pantryRepository;

	@Mock
	private PantryService pantryService;

	@InjectMocks
	private ShoppingListService shoppingListService;

	private ShoppingList testList;
	private Ingredient testIngredient;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		testList = new ShoppingList();
		testList.setId(1L);
		testList.setGroupId(10L);

		testIngredient = new Ingredient();
		testIngredient.setId(100L);
		testIngredient.setIngredientName("Milk");
	}

	@Test
	public void addItemToList_newIngredient_success() {
		when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(testList));
		when(ingredientRepository.findById(100L)).thenReturn(Optional.of(testIngredient));
		when(shoppingListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ShoppingListItem result = shoppingListService.addItemToList(1L, 100L, 2);

		assertNotNull(result);
		assertEquals(testIngredient, result.getIngredient());
		assertEquals(2, result.getQuantity());
		assertFalse(result.getIsBought());
	}

	@Test
	public void addItemToList_existingUnbought_mergesQuantity() {
		ShoppingListItem existing = new ShoppingListItem();
		existing.setIngredient(testIngredient);
		existing.setQuantity(1);
		existing.setIsBought(false);
		testList.getItems().add(existing);

		when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(testList));
		when(ingredientRepository.findById(100L)).thenReturn(Optional.of(testIngredient));
		when(shoppingListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ShoppingListItem result = shoppingListService.addItemToList(1L, 100L, 2);

		assertEquals(3, result.getQuantity());
		assertEquals(existing, result);
	}

	@Test
	public void patchItemBoughtStatus_setToTrue_movesToPantry() {
		ShoppingListItem item = new ShoppingListItem();
		item.setId(500L);
		item.setShoppingList(testList);
		item.setIngredient(testIngredient);
		item.setQuantity(2);
		item.setIsBought(false);
		testList.getItems().add(item);

		Pantry testPantry = new Pantry();
		testPantry.setId(20L);

		when(shoppingListItemRepository.findById(500L)).thenReturn(Optional.of(item));
		when(shoppingListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(pantryRepository.findAllByGroupId(10L)).thenReturn(java.util.Collections.singletonList(testPantry));

		ShoppingListItem result = shoppingListService.patchItemBoughtStatus(500L, true);

		assertTrue(result.getIsBought());
		// Verify moved to pantry logic
		verify(pantryService).addItemToPantry(20L, 100L, 2);
		// Verify removed from list
		assertFalse(testList.getItems().contains(item));
		verify(shoppingListItemRepository).delete(item);
	}

	@Test
	public void getItemByIdAndVerifyGroup_forbiddenAccess() {
		ShoppingListItem item = new ShoppingListItem();
		item.setShoppingList(testList); // group 10
		when(shoppingListItemRepository.findById(500L)).thenReturn(Optional.of(item));

		assertThrows(ResponseStatusException.class, 
				() -> shoppingListService.getItemByIdAndVerifyGroup(500L, 20L));
	}
}
