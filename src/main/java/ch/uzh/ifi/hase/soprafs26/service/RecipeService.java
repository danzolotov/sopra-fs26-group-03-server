package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.Recipe;
import ch.uzh.ifi.hase.soprafs26.repository.IngredientRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RecipeRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RecipePutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, IngredientRepository ingredientRepository) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public Recipe updateRecipe(Long recipeId, RecipePutDTO recipePutDTO) {
        Recipe existingRecipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        if (recipePutDTO.getName() != null && !recipePutDTO.getName().trim().isEmpty()) {
            existingRecipe.setName(recipePutDTO.getName().trim());
        }
        
        if (recipePutDTO.getDescription() != null) {
            existingRecipe.setDescription(recipePutDTO.getDescription().trim());
        }

        if (recipePutDTO.getIngredients() != null) {
            // Remove existing ingredients completely
            existingRecipe.getIngredients().clear();
            ingredientRepository.flush();

            List<Ingredient> newIngredients = new ArrayList<>();
            for (IngredientPutDTO ingDto : recipePutDTO.getIngredients()) {
                Ingredient newIng = DTOMapper.INSTANCE.convertIngredientPutDTOtoEntity(ingDto);
                newIng.setRecipe(existingRecipe);
                newIngredients.add(newIng);
            }
            existingRecipe.getIngredients().addAll(newIngredients);
        }

        return recipeRepository.saveAndFlush(existingRecipe);
    }
}
