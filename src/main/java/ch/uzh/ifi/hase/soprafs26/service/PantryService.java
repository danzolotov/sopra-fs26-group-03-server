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
		return addItemToPantry(pantryId, ingredientId, null, null, null, null, quantity);
	}

	public PantryItem addItemToPantry(Long pantryId, Long ingredientId, String ingredientName,
			String ingredientDescription, Unit standardUnit, IngredientCategory category, Integer quantity) {
		if (quantity == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be provided");
		}
		Pantry pantry = pantryRepository.findById(pantryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pantry not found"));
		Ingredient ingredient = resolveIngredient(ingredientId, ingredientName, ingredientDescription, standardUnit, category);

		// Determine the unit to use for the item: prefer standardUnit, fallback to ingredient.unit
		Unit itemUnit = standardUnit != null ? standardUnit : ingredient.getUnit();
		if (itemUnit == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit must be provided or ingredient must have a default unit");
		}

		for (PantryItem existing : pantry.getItems()) {
			if (existing.getIngredient().getId().equals(ingredient.getId()) && existing.getUnit() == itemUnit) {
				existing.setQuantity(existing.getQuantity() + quantity);
				pantryItemRepository.save(existing);
				pantryItemRepository.flush();
				log.debug("Merged quantity for ingredient {} in pantry {}", ingredient.getId(), pantryId);
				return existing;
			}
		}

		PantryItem newItem = new PantryItem();
		newItem.setPantry(pantry);
		newItem.setIngredient(ingredient);
		newItem.setUnit(itemUnit);
		newItem.setQuantity(quantity);
		pantry.getItems().add(newItem);
		newItem = pantryItemRepository.save(newItem);
		pantryItemRepository.flush();
		log.debug("Added ingredient {} to pantry {}", ingredient.getId(), pantryId);
		return newItem;
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
		updateItem(itemId, ingredientId, quantity, null);
	}

	public void updateItem(Long itemId, Long ingredientId, Integer quantity, Unit unit) {
		PantryItem item = getItemById(itemId);
		Ingredient ingredient = ingredientRepository.findById(ingredientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));
		item.setIngredient(ingredient);
		item.setQuantity(quantity);
		if (unit != null) {
			item.setUnit(unit);
		}
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
