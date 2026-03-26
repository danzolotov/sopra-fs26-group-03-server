package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.ShoppingList;
import ch.uzh.ifi.hase.soprafs26.entity.ShoppingListItem;
import ch.uzh.ifi.hase.soprafs26.repository.IngredientRepository;
import ch.uzh.ifi.hase.soprafs26.repository.ShoppingListItemRepository;
import ch.uzh.ifi.hase.soprafs26.repository.ShoppingListRepository;
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

	@Autowired
	public ShoppingListService(@Qualifier("shoppingListRepository") ShoppingListRepository shoppingListRepository,
			@Qualifier("shoppingListItemRepository") ShoppingListItemRepository shoppingListItemRepository,
			@Qualifier("ingredientRepository") IngredientRepository ingredientRepository) {
		this.shoppingListRepository = shoppingListRepository;
		this.shoppingListItemRepository = shoppingListItemRepository;
		this.ingredientRepository = ingredientRepository;
	}

	public List<ShoppingList> getShoppingLists() {
		return this.shoppingListRepository.findAll();
	}

	public ShoppingList createShoppingList(ShoppingList newList) {
		newList.setTotalEstimatedCost(0.0);
		newList = shoppingListRepository.save(newList);
		shoppingListRepository.flush();
		log.debug("Created ShoppingList: {}", newList.getId());
		return newList;
	}

	public ShoppingList getShoppingListById(Long id) {
		return shoppingListRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shopping list not found"));
	}

	public void updateShoppingList(Long id, ShoppingList listUpdate) {
		ShoppingList list = getShoppingListById(id);
		list.setGroupId(listUpdate.getGroupId());
		shoppingListRepository.save(list);
		shoppingListRepository.flush();
	}

	public void deleteShoppingList(Long id) {
		ShoppingList list = getShoppingListById(id);
		shoppingListRepository.delete(list);
		shoppingListRepository.flush();
	}

	public ShoppingListItem addItemToShoppingList(Long listId, ShoppingListItem newItem, Long ingredientId) {
		ShoppingList shoppingList = getShoppingListById(listId);
		Ingredient ingredient = ingredientRepository.findById(ingredientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));

		newItem.setShoppingList(shoppingList);
		newItem.setIngredient(ingredient);
		shoppingList.getItems().add(newItem);

		recalculateTotalCost(shoppingList);

		newItem = shoppingListItemRepository.save(newItem);
		shoppingListItemRepository.flush();
		return newItem;
	}

	public ShoppingListItem getItemById(Long id) {
		return shoppingListItemRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
	}

	public void updateItem(Long itemId, ShoppingListItem itemUpdate, Long ingredientId) {
		ShoppingListItem item = getItemById(itemId);
		Ingredient ingredient = ingredientRepository.findById(ingredientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));

		item.setQuantity(itemUpdate.getQuantity());
		item.setEstimatedPrice(itemUpdate.getEstimatedPrice());
		item.setIngredient(ingredient);

		recalculateTotalCost(item.getShoppingList());

		shoppingListItemRepository.save(item);
		shoppingListItemRepository.flush();
	}

	public ShoppingListItem patchItemBoughtStatus(Long itemId, Boolean isBought) {
		ShoppingListItem item = getItemById(itemId);
		item.setIsBought(isBought);
		item = shoppingListItemRepository.save(item);
		shoppingListItemRepository.flush();
		return item;
	}

	public void deleteItem(Long itemId) {
		ShoppingListItem item = getItemById(itemId);
		ShoppingList shoppingList = item.getShoppingList();
		shoppingList.getItems().remove(item);

		recalculateTotalCost(shoppingList);

		shoppingListItemRepository.delete(item);
		shoppingListItemRepository.flush();
	}

	private void recalculateTotalCost(ShoppingList list) {
		double total = list.getItems().stream()
				.mapToDouble(item -> item.getQuantity() * item.getEstimatedPrice())
				.sum();
		list.setTotalEstimatedCost(total);
		shoppingListRepository.save(list);
	}
}
