package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoppingListAutoDetectServiceTest {

	private ShoppingListAutoDetectService autoDetectService;
	private Method extractMethod;

	@BeforeEach
	void setup() throws NoSuchMethodException {
		autoDetectService = new ShoppingListAutoDetectService();
		extractMethod = ShoppingListAutoDetectService.class
				.getDeclaredMethod("extractShoppingListItemsWithQuantities", String.class);
		extractMethod.setAccessible(true);
	}

	@Test
	void detectShoppingListItemsWithQuantities_nullOrEmptyImage_returnsEmptyList() {
		assertTrue(autoDetectService.detectShoppingListItemsWithQuantities(null).isEmpty());
		assertTrue(autoDetectService.detectShoppingListItemsWithQuantities(new byte[0]).isEmpty());
	}

	@Test
	void extractShoppingListItemsWithQuantities_listMarkersAndQuantities_returnsDetectedItems() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				- Milk
				2 Eggs
				[x] 3 apples
				""");

		assertEquals(3, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Milk", 1), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Eggs", 2), items.get(1));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("apples", 3), items.get(2));
	}

	@Test
	void extractShoppingListItemsWithQuantities_receiptLikeText_returnsEmptyList() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				Receipt
				Total 12.30
				Thank you for shopping with us
				""");

		assertTrue(items.isEmpty());
	}

	@Test
	void extractShoppingListItemsWithQuantities_singleCandidate_returnsEmptyList() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("Milk");

		assertTrue(items.isEmpty());
	}

	@Test
	void extractShoppingListItemsWithQuantities_decimalQuantityKeepsWholeNumber() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				1.5 kg Flour
				2 Sugar
				""");

		assertEquals(2, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("kg Flour", 1), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Sugar", 2), items.get(1));
	}

	@SuppressWarnings("unchecked")
	private List<ShoppingListAutoDetectService.DetectedShoppingItem> extractItems(String ocrText) throws Exception {
		return (List<ShoppingListAutoDetectService.DetectedShoppingItem>) extractMethod.invoke(autoDetectService, ocrText);
	}
}
