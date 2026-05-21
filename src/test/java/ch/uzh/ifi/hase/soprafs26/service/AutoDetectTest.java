package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import com.google.cloud.vision.v1.BoundingPoly;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Vertex;
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
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Flour", 1, Unit.KILOGRAM), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Sugar", 2), items.get(1));
	}

	@Test
	void extractShoppingListItemsWithQuantities_units_returnsDetectedItemsWithUnits() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				2 kg Flour
				500 ml Milk
				3 pcs Apples
				""");

		assertEquals(3, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Flour", 2, Unit.KILOGRAM), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Milk", 500, Unit.MILLILITER), items.get(1));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Apples", 3, Unit.PIECE), items.get(2));
	}

	@Test
	void extractShoppingListItemsWithQuantities_trailingQuantitiesAndUnits_returnsDetectedItems() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				Flour 2 kg
				Milk 1 l
				""");

		assertEquals(2, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Flour", 2, Unit.KILOGRAM), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Milk", 1, Unit.LITER), items.get(1));
	}

	@Test
	void extractShoppingListItemsWithQuantities_multiplierQuantities_returnsDetectedItems() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				- Milk x2
				x 3 Apples
				""");

		assertEquals(2, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Milk", 2), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Apples", 3), items.get(1));
	}

	@Test
	void extractShoppingListItemsWithQuantities_ocrCircleBullets_returnsQuantitiesAndUnits() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				o 200 ml Milk
				o 2 Banana
				o 5 Eggs
				o 400g Carrots
				""");

		assertEquals(4, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Milk", 200, Unit.MILLILITER), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Banana", 2), items.get(1));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Eggs", 5), items.get(2));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Carrots", 400, Unit.GRAM), items.get(3));
	}

	@Test
	void extractShoppingListItemsWithQuantities_splitAmountsAndNames_returnsQuantitiesAndUnits() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				200ml
				Milk
				2
				Banana
				5
				Eggs
				400g
				Carrots
				""");

		assertEquals(4, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Milk", 200, Unit.MILLILITER), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Banana", 2), items.get(1));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Eggs", 5), items.get(2));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Carrots", 400, Unit.GRAM), items.get(3));
	}

	@Test
	void extractShoppingListItemsWithQuantities_columnSeparatedAmountsAndNames_returnsDetectedItems() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				750ml
				3
				8
				250g
				Milk
				Banana
				Eggs
				Carrots
				""");

		assertEquals(4, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Milk", 750, Unit.MILLILITER), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Banana", 3), items.get(1));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Eggs", 8), items.get(2));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Carrots", 250, Unit.GRAM), items.get(3));
	}

	@Test
	@SuppressWarnings("unchecked")
	void buildRowsFromAnnotations_twoVisualColumns_returnsRowsByYCoordinate() throws Exception {
		Method buildRowsMethod = ShoppingListAutoDetectService.class
				.getDeclaredMethod("buildRowsFromAnnotations", List.class);
		buildRowsMethod.setAccessible(true);

		List<EntityAnnotation> annotations = List.of(
				word("750", 10, 10, 45, 25),
				word("ml", 50, 10, 70, 25),
				word("3", 10, 35, 20, 50),
				word("8", 10, 60, 20, 75),
				word("250g", 10, 85, 55, 100),
				word("Milk", 110, 10, 145, 25),
				word("Banana", 110, 35, 170, 50),
				word("Eggs", 110, 60, 150, 75),
				word("Carrots", 110, 85, 175, 100)
		);

		List<String> rows = (List<String>) buildRowsMethod.invoke(autoDetectService, annotations);

		assertEquals(List.of(
				"750 ml Milk",
				"3 Banana",
				"8 Eggs",
				"250g Carrots"
		), rows);
	}

	@Test
	void extractShoppingListItemsWithQuantities_ocrZerosInQuantities_returnsCorrectQuantities() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				2OOml Milk
				4OOg Carrots
				""");

		assertEquals(2, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Milk", 200, Unit.MILLILITER), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Carrots", 400, Unit.GRAM), items.get(1));
	}

	@Test
	void extractShoppingListItemsWithQuantities_splitOcrZerosInQuantities_returnsCorrectQuantities() throws Exception {
		List<ShoppingListAutoDetectService.DetectedShoppingItem> items = extractItems("""
				2OOml
				Milk
				4OOg
				Carrots
				""");

		assertEquals(2, items.size());
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Milk", 200, Unit.MILLILITER), items.get(0));
		assertEquals(new ShoppingListAutoDetectService.DetectedShoppingItem("Carrots", 400, Unit.GRAM), items.get(1));
	}

	@SuppressWarnings("unchecked")
	private List<ShoppingListAutoDetectService.DetectedShoppingItem> extractItems(String ocrText) throws Exception {
		return (List<ShoppingListAutoDetectService.DetectedShoppingItem>) extractMethod.invoke(autoDetectService, ocrText);
	}

	private EntityAnnotation word(String text, int minX, int minY, int maxX, int maxY) {
		BoundingPoly boundingPoly = BoundingPoly.newBuilder()
				.addVertices(Vertex.newBuilder().setX(minX).setY(minY).build())
				.addVertices(Vertex.newBuilder().setX(maxX).setY(minY).build())
				.addVertices(Vertex.newBuilder().setX(maxX).setY(maxY).build())
				.addVertices(Vertex.newBuilder().setX(minX).setY(maxY).build())
				.build();
		return EntityAnnotation.newBuilder()
				.setDescription(text)
				.setBoundingPoly(boundingPoly)
				.build();
	}
}
