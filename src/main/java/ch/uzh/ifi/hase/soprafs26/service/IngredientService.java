package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class IngredientService {

	private final IngredientRepository ingredientRepository;

	public IngredientService(@Qualifier("ingredientRepository") IngredientRepository ingredientRepository) {
		this.ingredientRepository = ingredientRepository;
	}

	public List<Ingredient> getIngredients() {
		return ingredientRepository.findAll();
	}

	public Ingredient createIngredient(Ingredient ingredient) {
		if (ingredient == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient payload must be provided");
		}
		if (ingredient.getIngredientName() == null || ingredient.getIngredientName().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient name must be provided");
		}
		if (ingredient.getUnit() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient unit must be provided");
		}

		String normalizedName = ingredient.getIngredientName().trim();
		ingredient.setIngredientName(normalizedName);

		ingredientRepository.findByIngredientNameIgnoreCase(normalizedName).ifPresent(existing -> {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					String.format("Ingredient '%s' already exists", normalizedName));
		});

		return ingredientRepository.saveAndFlush(ingredient);
	}
}

