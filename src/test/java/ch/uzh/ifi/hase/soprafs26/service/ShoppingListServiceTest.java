package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
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
		testIngredient.setUnit(Unit.PIECE);
		testIngredient.setCategory(IngredientCategory.DAIRY);
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
		existing.setUnit(Unit.PIECE);
		testList.getItems().add(existing);

		when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(testList));
		when(ingredientRepository.findById(100L)).thenReturn(Optional.of(testIngredient));
		when(shoppingListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ShoppingListItem result = shoppingListService.addItemToList(1L, 100L, 2);

		assertEquals(3, result.getQuantity());
		assertEquals(existing, result);
	}

	@Test
	public void addItemToList_inlineIngredient_createsIngredient() {
		when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(testList));
		when(ingredientRepository.findByIngredientNameIgnoreCaseAndUnitAndUser("Yogurt", Unit.GRAM, null)).thenReturn(Optional.empty());
		when(ingredientRepository.saveAndFlush(any(Ingredient.class))).thenAnswer(invocation -> {
			Ingredient ingredient = invocation.getArgument(0);
			ingredient.setId(200L);
			return ingredient;
		});
		when(shoppingListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ShoppingListItem result = shoppingListService.addItemToList(1L, null, "Yogurt", "Plain yogurt",
				Unit.GRAM, IngredientCategory.DAIRY, 2);

		assertEquals("Yogurt", result.getIngredient().getIngredientName());
		assertEquals(Unit.GRAM, result.getIngredient().getUnit());
		assertEquals(IngredientCategory.DAIRY, result.getIngredient().getCategory());
		assertFalse(result.getIsBought());
	}

	@Test
	public void addItemToList_inlineIngredient_existingName_reusesIngredient() {
		when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(testList));
		when(ingredientRepository.findByIngredientNameIgnoreCase("Milk"))
				.thenReturn(java.util.List.of(testIngredient));
		when(shoppingListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ShoppingListItem result = shoppingListService.addItemToList(1L, null, "Milk", null,
				Unit.PIECE, IngredientCategory.DAIRY, 2);

		assertEquals(testIngredient, result.getIngredient());
		verify(ingredientRepository, never()).saveAndFlush(any(Ingredient.class));
	}

	@Test
	public void addItemToList_existingId_fillsMissingCategory() {
		Ingredient ingredientWithoutCategory = new Ingredient();
		ingredientWithoutCategory.setId(300L);
		ingredientWithoutCategory.setIngredientName("Cheese");
		ingredientWithoutCategory.setUnit(Unit.PIECE);

		when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(testList));
		when(ingredientRepository.findById(300L)).thenReturn(Optional.of(ingredientWithoutCategory));
		when(ingredientRepository.saveAndFlush(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(shoppingListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ShoppingListItem result = shoppingListService.addItemToList(1L, 300L, null, null,
				null, IngredientCategory.DAIRY, 1);

		assertEquals(IngredientCategory.DAIRY, result.getIngredient().getCategory());
		verify(ingredientRepository).saveAndFlush(ingredientWithoutCategory);
	}

	@Test
	public void patchItemBoughtStatus_setToTrue_movesToPantry() {
		ShoppingListItem item = new ShoppingListItem();
		item.setId(500L);
		item.setShoppingList(testList);
		item.setIngredient(testIngredient);
		item.setQuantity(2);
		item.setIsBought(false);
		item.setUnit(Unit.PIECE);
		testList.getItems().add(item);

		Pantry testPantry = new Pantry();
		testPantry.setId(20L);

		when(shoppingListItemRepository.findById(500L)).thenReturn(Optional.of(item));
		when(shoppingListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(pantryRepository.findAllByGroupId(10L)).thenReturn(java.util.Collections.singletonList(testPantry));

		ShoppingListItem result = shoppingListService.patchItemBoughtStatus(500L, true);

		assertTrue(result.getIsBought());
		// Verify moved to pantry logic with full parameters
		verify(pantryService).addItemToPantry(20L, 100L, "Milk", null, Unit.PIECE, IngredientCategory.DAIRY, 2);
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

	@Test
	public void getShoppingListByGroupId_success() {
		when(shoppingListRepository.findAllByGroupId(10L)).thenReturn(java.util.List.of(testList));
		ShoppingList result = shoppingListService.getShoppingListByGroupId(10L);
		assertNotNull(result);
		assertEquals(1L, result.getId());
	}

	@Test
	public void getShoppingListByGroupId_notFound() {
		when(shoppingListRepository.findAllByGroupId(10L)).thenReturn(java.util.Collections.emptyList());
		assertThrows(ResponseStatusException.class, () -> shoppingListService.getShoppingListByGroupId(10L));
	}

	@Test
	public void addItemToList_nullQuantity_throwsBadRequest() {
		assertThrows(ResponseStatusException.class, () -> shoppingListService.addItemToList(1L, 100L, null));
	}

	@Test
	public void addItemToList_listNotFound_throwsNotFound() {
		when(shoppingListRepository.findById(1L)).thenReturn(Optional.empty());
		assertThrows(ResponseStatusException.class, () -> shoppingListService.addItemToList(1L, 100L, 2));
	}

	@Test
	public void addItemToList_ingredientNotFound_throwsNotFound() {
		when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(testList));
		when(ingredientRepository.findById(100L)).thenReturn(Optional.empty());
		assertThrows(ResponseStatusException.class, () -> shoppingListService.addItemToList(1L, 100L, 2));
	}

	@Test
	public void addItemToList_emptyName_throwsBadRequest() {
		when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(testList));
		assertThrows(ResponseStatusException.class, () -> shoppingListService.addItemToList(1L, null, "", "Desc", Unit.PIECE, IngredientCategory.DAIRY, 2));
	}

	@Test
	public void addItemToList_nullUnitNewIngredient_throwsBadRequest() {
		when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(testList));
		when(ingredientRepository.findByIngredientNameIgnoreCase("Yogurt")).thenReturn(java.util.Collections.emptyList());
		assertThrows(ResponseStatusException.class, () -> shoppingListService.addItemToList(1L, null, "Yogurt", "Desc", null, IngredientCategory.DAIRY, 2));
	}

	@Test
	public void updateItem_success() {
		ShoppingListItem item = new ShoppingListItem();
		item.setId(500L);
		item.setIngredient(testIngredient);
		item.setQuantity(2);
		item.setUnit(Unit.PIECE);

		when(shoppingListItemRepository.findById(500L)).thenReturn(Optional.of(item));
		when(ingredientRepository.findById(100L)).thenReturn(Optional.of(testIngredient));

		shoppingListService.updateItem(500L, 100L, 5, Unit.GRAM);

		assertEquals(5, item.getQuantity());
		assertEquals(Unit.GRAM, item.getUnit());
		verify(shoppingListItemRepository).save(item);
	}

	@Test
	public void deleteItem_success() {
		ShoppingListItem item = new ShoppingListItem();
		item.setId(500L);
		item.setShoppingList(testList);
		testList.getItems().add(item);

		when(shoppingListItemRepository.findById(500L)).thenReturn(Optional.of(item));

		shoppingListService.deleteItem(500L);

		assertFalse(testList.getItems().contains(item));
		verify(shoppingListItemRepository).delete(item);
	}

	@Test
	public void patchItemBoughtStatus_setToFalse_success() {
		ShoppingListItem item = new ShoppingListItem();
		item.setId(500L);
		item.setShoppingList(testList);
		item.setIsBought(true);

		when(shoppingListItemRepository.findById(500L)).thenReturn(Optional.of(item));
		when(shoppingListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ShoppingListItem result = shoppingListService.patchItemBoughtStatus(500L, false);

		assertFalse(result.getIsBought());
		verifyNoInteractions(pantryService);
	}
}
