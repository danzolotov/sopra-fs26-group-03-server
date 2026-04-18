package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.Pantry;
import ch.uzh.ifi.hase.soprafs26.entity.PantryItem;
import ch.uzh.ifi.hase.soprafs26.repository.IngredientRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PantryItemRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PantryRepository;
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

public class PantryServiceTest {

	@Mock
	private PantryRepository pantryRepository;

	@Mock
	private PantryItemRepository pantryItemRepository;

	@Mock
	private IngredientRepository ingredientRepository;

	@InjectMocks
	private PantryService pantryService;

	private Pantry testPantry;
	private Ingredient testIngredient;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		testPantry = new Pantry();
		testPantry.setId(1L);
		testPantry.setGroupId(10L);

		testIngredient = new Ingredient();
		testIngredient.setId(100L);
		testIngredient.setIngredientName("Apple");
	}

	@Test
	public void getPantryByGroupId_validGroupId_success() {
		when(pantryRepository.findAllByGroupId(10L)).thenReturn(java.util.Collections.singletonList(testPantry));

		Pantry found = pantryService.getPantryByGroupId(10L);

		assertEquals(testPantry, found);
	}

	@Test
	public void getPantryByGroupId_invalidGroupId_throwsNotFound() {
		when(pantryRepository.findAllByGroupId(any())).thenReturn(java.util.Collections.emptyList());

		assertThrows(ResponseStatusException.class, () -> pantryService.getPantryByGroupId(99L));
	}

	@Test
	public void addItemToPantry_newIngredient_success() {
		when(pantryRepository.findById(1L)).thenReturn(Optional.of(testPantry));
		when(ingredientRepository.findById(100L)).thenReturn(Optional.of(testIngredient));
		when(pantryItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		PantryItem result = pantryService.addItemToPantry(1L, 100L, 5);

		assertNotNull(result);
		assertEquals(testIngredient, result.getIngredient());
		assertEquals(5, result.getQuantity());
		verify(pantryItemRepository, times(1)).save(any());
	}

	@Test
	public void addItemToPantry_existingIngredient_mergesQuantity() {
		PantryItem existingItem = new PantryItem();
		existingItem.setIngredient(testIngredient);
		existingItem.setQuantity(3);
		testPantry.getItems().add(existingItem);

		when(pantryRepository.findById(1L)).thenReturn(Optional.of(testPantry));
		when(ingredientRepository.findById(100L)).thenReturn(Optional.of(testIngredient));
		when(pantryItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		PantryItem result = pantryService.addItemToPantry(1L, 100L, 5);

		assertEquals(8, result.getQuantity());
		assertEquals(existingItem, result);
		verify(pantryItemRepository, times(1)).save(existingItem);
	}

	@Test
	public void getItemByIdAndVerifyGroup_validAccess_success() {
		PantryItem item = new PantryItem();
		item.setPantry(testPantry);
		when(pantryItemRepository.findById(500L)).thenReturn(Optional.of(item));

		PantryItem found = pantryService.getItemByIdAndVerifyGroup(500L, 10L);

		assertEquals(item, found);
	}

	@Test
	public void getItemByIdAndVerifyGroup_forbiddenAccess_throwsForbidden() {
		PantryItem item = new PantryItem();
		item.setPantry(testPantry); // group 10
		when(pantryItemRepository.findById(500L)).thenReturn(Optional.of(item));

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> pantryService.getItemByIdAndVerifyGroup(500L, 20L));
		assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
	}

	@Test
	public void deleteItem_validId_success() {
		PantryItem item = new PantryItem();
		item.setPantry(testPantry);
		testPantry.getItems().add(item);
		when(pantryItemRepository.findById(500L)).thenReturn(Optional.of(item));

		pantryService.deleteItem(500L);

		assertTrue(testPantry.getItems().isEmpty());
		verify(pantryItemRepository, times(1)).delete(item);
	}
}
