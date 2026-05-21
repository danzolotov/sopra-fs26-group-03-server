package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ShoppingListService {

	private final Logger log = LoggerFactory.getLogger(ShoppingListService.class);

	private final ShoppingListRepository shoppingListRepository;
	private final ShoppingListItemRepository shoppingListItemRepository;
	private final IngredientRepository ingredientRepository;
	private final PantryRepository pantryRepository;
	private final PantryService pantryService;

	@Autowired
	public ShoppingListService(@Qualifier("shoppingListRepository") ShoppingListRepository shoppingListRepository,
			@Qualifier("shoppingListItemRepository") ShoppingListItemRepository shoppingListItemRepository,
			@Qualifier("ingredientRepository") IngredientRepository ingredientRepository,
			@Qualifier("pantryRepository") PantryRepository pantryRepository,
			PantryService pantryService) {
		this.shoppingListRepository = shoppingListRepository;
		this.shoppingListItemRepository = shoppingListItemRepository;
		this.ingredientRepository = ingredientRepository;
		this.pantryRepository = pantryRepository;
		this.pantryService = pantryService;
	}

	public ShoppingList getShoppingListByGroupId(Long groupId) {
		List<ShoppingList> lists = shoppingListRepository.findAllByGroupId(groupId);
		if (lists.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No shopping list found for this group");
		}
		return lists.get(0);
	}

	public ShoppingListItem addItemToList(Long listId, Long ingredientId, Integer quantity) {
		return addItemToList(listId, ingredientId, null, null, null, null, quantity);
	}

	public ShoppingListItem addItemToList(Long listId, Long ingredientId, String ingredientName,
			String ingredientDescription, Unit standardUnit, IngredientCategory category, Integer quantity) {
		if (quantity == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be provided");
		}
		ShoppingList list = shoppingListRepository.findById(listId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shopping list not found"));
		Ingredient ingredient = resolveIngredient(ingredientId, ingredientName, ingredientDescription, standardUnit, category);

		// Determine the unit to use for the item: prefer standardUnit, fallback to ingredient.unit
		Unit itemUnit = standardUnit != null ? standardUnit : ingredient.getUnit();
		if (itemUnit == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit must be provided or ingredient must have a default unit");
		}

		for (ShoppingListItem existing : list.getItems()) {
			if (existing.getIngredient().getId().equals(ingredient.getId()) && existing.getUnit() == itemUnit && Boolean.FALSE.equals(existing.getIsBought())) {
				existing.setQuantity(existing.getQuantity() + quantity);
				shoppingListItemRepository.save(existing);
				shoppingListItemRepository.flush();
				log.debug("Merged quantity for ingredient {} in shopping list {}", ingredient.getId(), listId);
				return existing;
			}
		}

		ShoppingListItem item = new ShoppingListItem();
		item.setShoppingList(list);
		item.setIngredient(ingredient);
		item.setUnit(itemUnit);
		item.setQuantity(quantity);
		item.setIsBought(false);
		list.getItems().add(item);

		item = shoppingListItemRepository.save(item);
		shoppingListItemRepository.flush();
		log.debug("Added ingredient {} to shopping list {}", ingredient.getId(), listId);
		return item;
	}

	private Ingredient resolveIngredient(Long ingredientId, String ingredientName, String ingredientDescription,
			Unit standardUnit, IngredientCategory category) {
		if (ingredientId != null) {
			Ingredient ingredient = ingredientRepository.findById(ingredientId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));
			return fillMissingIngredientDetails(ingredient, ingredientDescription, standardUnit, category);
		}

		if (ingredientName == null || ingredientName.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient name must be provided");
		}

		String normalizedName = ingredientName.trim();
		Ingredient existing = ingredientRepository.findByIngredientNameIgnoreCase(normalizedName).stream()
				.findFirst()
				.orElse(null);
		if (existing != null) {
			return fillMissingIngredientDetails(existing, ingredientDescription, standardUnit, category);
		}
		if (standardUnit == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient standardUnit must be provided");
		}

		Ingredient ingredient = new Ingredient();
		ingredient.setIngredientName(normalizedName);
		ingredient.setIngredientDescription(ingredientDescription);
		ingredient.setUnit(standardUnit);
		ingredient.setCategory(category != null ? category : IngredientCategory.OTHER);
		return ingredientRepository.saveAndFlush(ingredient);
	}

	private Ingredient fillMissingIngredientDetails(Ingredient ingredient, String ingredientDescription,
			Unit standardUnit, IngredientCategory category) {
		boolean changed = false;
		if ((ingredient.getIngredientDescription() == null || ingredient.getIngredientDescription().isBlank())
				&& ingredientDescription != null) {
			ingredient.setIngredientDescription(ingredientDescription);
			changed = true;
		}
		if (ingredient.getUnit() == null && standardUnit != null) {
			ingredient.setUnit(standardUnit);
			changed = true;
		}
		if (category != null && ingredient.getCategory() != category) {
			ingredient.setCategory(category);
			changed = true;
		}
		return changed ? ingredientRepository.saveAndFlush(ingredient) : ingredient;
	}

	public ShoppingListItem getItemById(Long itemId) {
		return shoppingListItemRepository.findById(itemId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shopping list item not found"));
	}

	public ShoppingListItem getItemByIdAndVerifyGroup(Long itemId, Long groupId) {
		ShoppingListItem item = getItemById(itemId);
		if (!item.getShoppingList().getGroupId().equals(groupId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"This item does not belong to your group's shopping list");
		}
		return item;
	}

	public void updateItem(Long itemId, Long ingredientId, Integer quantity) {
		updateItem(itemId, ingredientId, quantity, null);
	}

	public void updateItem(Long itemId, Long ingredientId, Integer quantity, Unit unit) {
		ShoppingListItem item = getItemById(itemId);
		Ingredient ingredient = ingredientRepository.findById(ingredientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));
		item.setIngredient(ingredient);
		item.setQuantity(quantity);
		if (unit != null) {
			item.setUnit(unit);
		}
		shoppingListItemRepository.save(item);
		shoppingListItemRepository.flush();
	}

	public void deleteItem(Long itemId) {
		ShoppingListItem item = getItemById(itemId);
		ShoppingList list = item.getShoppingList();
		list.getItems().remove(item);
		shoppingListItemRepository.delete(item);
		shoppingListItemRepository.flush();
	}

	public ShoppingListItem patchItemBoughtStatus(Long itemId, Boolean isBought) {
		ShoppingListItem item = getItemById(itemId);
		item.setIsBought(isBought);
		item = shoppingListItemRepository.save(item);
		shoppingListItemRepository.flush();

		if (Boolean.TRUE.equals(isBought)) {
			Long groupId = item.getShoppingList().getGroupId();
			Pantry pantry = pantryRepository.findAllByGroupId(groupId).stream()
					.findFirst().orElse(null);
			if (pantry != null) {
				pantryService.addItemToPantry(
						pantry.getId(),
						item.getIngredient().getId(),
						item.getIngredient().getIngredientName(),
						item.getIngredient().getIngredientDescription(),
						item.getUnit(),
						item.getIngredient().getCategory(),
						item.getQuantity());

				ShoppingList shoppingList = item.getShoppingList();
				shoppingList.getItems().remove(item);
				shoppingListItemRepository.delete(item);
				shoppingListItemRepository.flush();
			}
		}

		return item;
	}
}
