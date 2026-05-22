package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.Recipe;
import ch.uzh.ifi.hase.soprafs26.repository.IngredientRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RecipeRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RecipePutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe testRecipe;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testRecipe = new Recipe();
        testRecipe.setId(1L);
        testRecipe.setName("Original Recipe");
        testRecipe.setDescription("Original Description");
        testRecipe.setIngredients(new ArrayList<>());
    }

    @Test
    public void updateRecipe_success() {
        RecipePutDTO recipePutDTO = new RecipePutDTO();
        recipePutDTO.setName("Updated Recipe");
        recipePutDTO.setDescription("Updated Description");

        IngredientPutDTO ingPutDto = new IngredientPutDTO();
        ingPutDto.setIngredientName("Tomato");
        ingPutDto.setQuantity(2);
        ingPutDto.setUnit(Unit.PIECE);
        ingPutDto.setCategory(IngredientCategory.VEGETABLE);
        recipePutDTO.setIngredients(Collections.singletonList(ingPutDto));

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Recipe updated = recipeService.updateRecipe(1L, recipePutDTO);

        assertNotNull(updated);
        assertEquals("Updated Recipe", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
        assertEquals(1, updated.getIngredients().size());
        assertEquals("Tomato", updated.getIngredients().get(0).getIngredientName());
        assertEquals(2, updated.getIngredients().get(0).getQuantity());
        assertEquals(Unit.PIECE, updated.getIngredients().get(0).getUnit());
        assertEquals(IngredientCategory.VEGETABLE, updated.getIngredients().get(0).getCategory());

        verify(ingredientRepository, times(1)).flush();
        verify(recipeRepository, times(1)).saveAndFlush(any(Recipe.class));
    }

    @Test
    public void updateRecipe_notFound_throwsException() {
        RecipePutDTO recipePutDTO = new RecipePutDTO();
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            recipeService.updateRecipe(1L, recipePutDTO);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Recipe not found", exception.getReason());
    }

    @Test
    public void updateRecipe_nullOrEmptyFields_noUpdate() {
        RecipePutDTO recipePutDTO = new RecipePutDTO();
        recipePutDTO.setName("   ");
        recipePutDTO.setDescription(null);
        recipePutDTO.setIngredients(null);

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Recipe updated = recipeService.updateRecipe(1L, recipePutDTO);

        assertNotNull(updated);
        assertEquals("Original Recipe", updated.getName());
        assertEquals("Original Description", updated.getDescription());
        assertTrue(updated.getIngredients().isEmpty());
    }
}
