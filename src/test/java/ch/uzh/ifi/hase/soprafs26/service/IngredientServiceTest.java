package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory;
import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IngredientServiceTest {

	@Mock
	private IngredientRepository ingredientRepository;

	@InjectMocks
	private IngredientService ingredientService;

	private Ingredient testIngredient;
	private User testUser;

	@BeforeEach
    void setup() {
		MockitoAnnotations.openMocks(this);

		testIngredient = new Ingredient();
		testIngredient.setIngredientName("Milk");
		testIngredient.setUnit(Unit.LITER);

		testUser = new User();
		testUser.setUserID("user-1");
		testIngredient.setUser(testUser);
	}

	@Test
     void createIngredient_validInput_success() {
		when(ingredientRepository.findByIngredientNameIgnoreCaseAndUser("Milk", testUser)).thenReturn(Optional.empty());
		when(ingredientRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Ingredient created = ingredientService.createIngredient(testIngredient);

		assertNotNull(created);
		assertEquals("Milk", created.getIngredientName());
		verify(ingredientRepository, times(1)).saveAndFlush(any());
	}

	@Test
	void createIngredient_trimsNameBeforeDuplicateCheckAndSave() {
		testIngredient.setIngredientName("  Milk  ");
		when(ingredientRepository.findByIngredientNameIgnoreCaseAndUser("Milk", testUser)).thenReturn(Optional.empty());
		when(ingredientRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Ingredient created = ingredientService.createIngredient(testIngredient);

		assertEquals("Milk", created.getIngredientName());
		verify(ingredientRepository).findByIngredientNameIgnoreCaseAndUser("Milk", testUser);
	}

	@Test
	void createIngredient_sameNameSameUnit_overwritesCategory() {
		Ingredient existing = ingredient("Milk", 7L, testUser, Unit.LITER, IngredientCategory.FRUIT);
		when(ingredientRepository.findByIngredientNameIgnoreCaseAndUser("Milk", testUser))
				.thenReturn(Optional.of(existing));
		when(ingredientRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		testIngredient.setCategory(IngredientCategory.DAIRY);
		Ingredient created = ingredientService.createIngredient(testIngredient);

		assertEquals(IngredientCategory.DAIRY, created.getCategory());
		verify(ingredientRepository).saveAndFlush(existing);
	}

	@Test
	void createIngredient_sameNameDifferentUnit_createsSeparateIngredient() {
		when(ingredientRepository.findByIngredientNameIgnoreCaseAndUnitAndUser("Milk", Unit.MILLILITER, testUser))
				.thenReturn(Optional.empty());
		when(ingredientRepository.findFirstByIngredientNameIgnoreCaseAndUnitAndUserIsNull("Milk", Unit.MILLILITER))
				.thenReturn(Optional.empty());
		when(ingredientRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Ingredient differentUnit = ingredient("Milk", null, testUser, Unit.MILLILITER, IngredientCategory.DAIRY);
		Ingredient created = ingredientService.createIngredient(differentUnit);

		assertEquals(Unit.MILLILITER, created.getUnit());
		verify(ingredientRepository).saveAndFlush(differentUnit);
	}

	@Test
	void createIngredient_globalMatchWithSameUnit_createsNewUserIngredient() {
		Ingredient global = ingredient("Milk", 99L, null, Unit.LITER, IngredientCategory.DAIRY);
		when(ingredientRepository.findByIngredientNameIgnoreCaseAndUnitAndUser("Milk", Unit.LITER, testUser)).thenReturn(Optional.empty());
		when(ingredientRepository.findFirstByIngredientNameIgnoreCaseAndUnitAndUserIsNull("Milk", Unit.LITER))
				.thenReturn(Optional.of(global));
		when(ingredientRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Ingredient created = ingredientService.createIngredient(testIngredient);

		assertNotNull(created);
		assertEquals(testUser, created.getUser());
		verify(ingredientRepository).saveAndFlush(any(Ingredient.class));
	}

	@Test
    void createIngredient_missingName_throwsBadRequest() {
		testIngredient.setIngredientName(null);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> ingredientService.createIngredient(testIngredient));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
    void createIngredient_missingUnit_throwsBadRequest() {
		testIngredient.setUnit(null);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> ingredientService.createIngredient(testIngredient));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
    void createIngredient_missingUser_throwsUnauthorized() {
		testIngredient.setUser(null);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> ingredientService.createIngredient(testIngredient));
		assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
	}

	@Test
	void getIngredients_nullUser_throwsUnauthorized() {
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> ingredientService.getIngredients(null));

		assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
	}

	@Test
	void getIngredients_userIngredientOverridesGlobalIngredientWithSameName() {
		Ingredient userMilk = ingredient("Milk", 1L, testUser, Unit.LITER, IngredientCategory.DAIRY);
		Ingredient globalMilk = ingredient("milk", 2L, null, Unit.MILLILITER, IngredientCategory.DAIRY);
		Ingredient globalMilkSameUnit = ingredient("Milk", 3L, null, Unit.LITER, IngredientCategory.DAIRY);
		Ingredient globalEgg = ingredient("Egg", 4L, null, Unit.PIECE, IngredientCategory.EGGS);
		when(ingredientRepository.findAllByUser(testUser)).thenReturn(List.of(userMilk));
		when(ingredientRepository.findAllByUserIsNull()).thenReturn(List.of(globalMilk, globalMilkSameUnit, globalEgg));

		List<Ingredient> result = ingredientService.getIngredients(testUser);

		// Both global Milk variants should be excluded because user has Milk (case-insensitive match)
		assertEquals(List.of(userMilk, globalEgg), result);
	}

	@Test
	void autocompleteIngredients_matchesSeedAliasAndStripsQuantity() {
		List<IngredientService.IngredientAutocompleteResult> results =
				ingredientService.autocompleteIngredients(List.of("capsicum", "- 2 kg tomatoes"));

		assertEquals("Bell Pepper", results.get(0).ingredientName());
		assertTrue(results.get(0).matched());
		assertEquals("Tomato", results.get(1).ingredientName());
		assertTrue(results.get(1).matched());
	}

	@Test
	void autocompleteIngredients_unrelatedInputReturnsUnmatchedResult() {
		List<IngredientService.IngredientAutocompleteResult> results =
				ingredientService.autocompleteIngredients(List.of("dish soap"));

		assertEquals("dish soap", results.get(0).input());
		assertFalse(results.get(0).matched());
		assertNull(results.get(0).ingredientName());
	}

	@Test
	void resolveOrCreateDetectedIngredient_existingGlobalMatchById_returnsGlobalIngredient() {
		Ingredient globalMilk = ingredient("Milk", 42L, null, Unit.MILLILITER, IngredientCategory.DAIRY);
		when(ingredientRepository.findAll()).thenReturn(List.of(globalMilk));
		when(ingredientRepository.findById(42L)).thenReturn(Optional.of(globalMilk));

		Ingredient resolved = ingredientService.resolveOrCreateDetectedIngredient("milk", testUser);

		assertEquals(globalMilk, resolved);
		verify(ingredientRepository, never()).saveAndFlush(any(Ingredient.class));
	}

	@Test
	void resolveOrCreateDetectedIngredient_unknownInputCreatesCleanUserIngredient() {
		when(ingredientRepository.findByIngredientNameIgnoreCaseAndUser("Dragon Fruit", testUser)).thenReturn(Optional.empty());
		when(ingredientRepository.findByIngredientNameIgnoreCase("Dragon Fruit")).thenReturn(List.of());
		when(ingredientRepository.saveAndFlush(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Ingredient resolved = ingredientService.resolveOrCreateDetectedIngredient("[ ] 2 packs dragon fruit!", testUser);

		assertEquals("Dragon Fruit", resolved.getIngredientName());
		assertEquals(Unit.PIECE, resolved.getUnit());
		assertEquals(testUser, resolved.getUser());
		verify(ingredientRepository).saveAndFlush(resolved);
	}

	@Test
	void resolveOrCreateDetectedIngredients_deduplicatesResolvedIngredientsByName() {
		Ingredient globalMilk = ingredient("Milk", 42L, null, Unit.MILLILITER, IngredientCategory.DAIRY);
		when(ingredientRepository.findAll()).thenReturn(List.of(globalMilk));
		when(ingredientRepository.findById(42L)).thenReturn(Optional.of(globalMilk));

		List<Ingredient> resolved = ingredientService.resolveOrCreateDetectedIngredients(List.of("milk", "Milk"), testUser);

		assertEquals(List.of(globalMilk), resolved);
	}

	private Ingredient ingredient(String name, Long id, User user, Unit unit, IngredientCategory category) {
		Ingredient ingredient = new Ingredient();
		ingredient.setId(id);
		ingredient.setIngredientName(name);
		ingredient.setUnit(unit);
		ingredient.setUser(user);
		ingredient.setCategory(category);
		return ingredient;
	}
}
