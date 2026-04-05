package ch.uzh.ifi.hase.soprafs26.service;

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
		ShoppingList list = shoppingListRepository.findById(listId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shopping list not found"));
		Ingredient ingredient = ingredientRepository.findById(ingredientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));

		for (ShoppingListItem existing : list.getItems()) {
			if (existing.getIngredient().getId().equals(ingredientId) && Boolean.FALSE.equals(existing.getIsBought())) {
				existing.setQuantity(existing.getQuantity() + quantity);
				shoppingListItemRepository.save(existing);
				shoppingListItemRepository.flush();
				log.debug("Merged quantity for ingredient {} in shopping list {}", ingredientId, listId);
				return existing;
			}
		}

		ShoppingListItem item = new ShoppingListItem();
		item.setShoppingList(list);
		item.setIngredient(ingredient);
		item.setQuantity(quantity);
		item.setIsBought(false);
		list.getItems().add(item);

		item = shoppingListItemRepository.save(item);
		shoppingListItemRepository.flush();
		log.debug("Added ingredient {} to shopping list {}", ingredientId, listId);
		return item;
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
		ShoppingListItem item = getItemById(itemId);
		Ingredient ingredient = ingredientRepository.findById(ingredientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));
		item.setIngredient(ingredient);
		item.setQuantity(quantity);
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
