package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Recipe;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RecipeGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
public class RecipeController {

    private final RecipeRepository recipeRepository;

    @Autowired
    public RecipeController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @GetMapping("/recipes")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<RecipeGetDTO> getAllRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        List<RecipeGetDTO> dtos = new ArrayList<>();
        for (Recipe r : recipes) {
            dtos.add(DTOMapper.INSTANCE.convertEntityToRecipeGetDTO(r));
        }
        return dtos;
    }

    @GetMapping("/recipes/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public RecipeGetDTO getRecipe(@PathVariable Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        return DTOMapper.INSTANCE.convertEntityToRecipeGetDTO(recipe);
    }
}
