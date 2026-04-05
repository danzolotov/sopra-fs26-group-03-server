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

@Service
@Transactional
public class PantryService {

	private final Logger log = LoggerFactory.getLogger(PantryService.class);

	private final PantryRepository pantryRepository;
	private final PantryItemRepository pantryItemRepository;
	private final IngredientRepository ingredientRepository;

	@Autowired
	public PantryService(@Qualifier("pantryRepository") PantryRepository pantryRepository,
			@Qualifier("pantryItemRepository") PantryItemRepository pantryItemRepository,
			@Qualifier("ingredientRepository") IngredientRepository ingredientRepository) {
		this.pantryRepository = pantryRepository;
		this.pantryItemRepository = pantryItemRepository;
		this.ingredientRepository = ingredientRepository;
	}

	public Pantry getPantryByGroupId(Long groupId) {
		return pantryRepository.findAllByGroupId(groupId).stream()
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No pantry found for this group"));
	}

	public PantryItem addItemToPantry(Long pantryId, Long ingredientId, Integer quantity) {
		Pantry pantry = pantryRepository.findById(pantryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pantry not found"));
		Ingredient ingredient = ingredientRepository.findById(ingredientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));

		for (PantryItem existing : pantry.getItems()) {
			if (existing.getIngredient().getId().equals(ingredientId)) {
				existing.setQuantity(existing.getQuantity() + quantity);
				pantryItemRepository.save(existing);
				pantryItemRepository.flush();
				log.debug("Merged quantity for ingredient {} in pantry {}", ingredientId, pantryId);
				return existing;
			}
		}

		PantryItem newItem = new PantryItem();
		newItem.setPantry(pantry);
		newItem.setIngredient(ingredient);
		newItem.setQuantity(quantity);
		pantry.getItems().add(newItem);
		newItem = pantryItemRepository.save(newItem);
		pantryItemRepository.flush();
		log.debug("Added ingredient {} to pantry {}", ingredientId, pantryId);
		return newItem;
	}

	public PantryItem getItemById(Long itemId) {
		return pantryItemRepository.findById(itemId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pantry item not found"));
	}

	public PantryItem getItemByIdAndVerifyGroup(Long itemId, Long groupId) {
		PantryItem item = getItemById(itemId);
		if (!item.getPantry().getGroupId().equals(groupId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"This item does not belong to your group's pantry");
		}
		return item;
	}

	public void updateItem(Long itemId, Long ingredientId, Integer quantity) {
		PantryItem item = getItemById(itemId);
		Ingredient ingredient = ingredientRepository.findById(ingredientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));
		item.setIngredient(ingredient);
		item.setQuantity(quantity);
		pantryItemRepository.save(item);
		pantryItemRepository.flush();
	}

	public void deleteItem(Long itemId) {
		PantryItem item = getItemById(itemId);
		item.getPantry().getItems().remove(item);
		pantryItemRepository.delete(item);
		pantryItemRepository.flush();
	}
}
