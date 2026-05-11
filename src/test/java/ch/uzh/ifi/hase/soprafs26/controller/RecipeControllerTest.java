package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.service.RecipeService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import ch.uzh.ifi.hase.soprafs26.entity.Recipe;
import ch.uzh.ifi.hase.soprafs26.repository.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeRepository recipeRepository;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private UserService userService;

    @Test
    public void getAllRecipes_success() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setName("Test Recipe");

        given(recipeRepository.findAll()).willReturn(Collections.singletonList(recipe));

        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Recipe"));
    }

    @Test
    public void getRecipe_success() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setName("Test Recipe");

        given(recipeRepository.findById(1L)).willReturn(Optional.of(recipe));

        mockMvc.perform(get("/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Recipe"));
    }

    @Test
    public void getRecipe_notFound_throwsNotFound() throws Exception {
        given(recipeRepository.findById(1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/recipes/1"))
                .andExpect(status().isNotFound());
    }
}
