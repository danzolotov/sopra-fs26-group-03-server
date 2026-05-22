package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientAutocompleteRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.IngredientService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngredientController.class)
@AutoConfigureMockMvc(addFilters = false)
public class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientService ingredientService;

    @MockitoBean
    private UserService userService;

    private User testUser;
    private Ingredient testIngredient;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setUserID("user-123");
        testUser.setUsername("testuser");
        testUser.setToken("valid-token");

        testIngredient = new Ingredient();
        testIngredient.setId(1L);
        testIngredient.setIngredientName("Tomato");
        testIngredient.setCategory(IngredientCategory.VEGETABLE);
        testIngredient.setUnit(Unit.PIECE);
        testIngredient.setQuantity(5);
        testIngredient.setUser(testUser);
    }

    @Test
    public void getIngredients_authorizedHeader_success() throws Exception {
        given(userService.getUserByToken("valid-token")).willReturn(testUser);
        given(ingredientService.getIngredients(testUser)).willReturn(Collections.singletonList(testIngredient));

        mockMvc.perform(get("/ingredients")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ingredientName").value("Tomato"))
                .andExpect(jsonPath("$[0].category").value("VEGETABLE"))
                .andExpect(jsonPath("$[0].standardUnit").value("PIECE"));
    }

    @Test
    public void getIngredients_authorizedCookie_success() throws Exception {
        given(userService.getUserByToken("valid-token")).willReturn(testUser);
        given(ingredientService.getIngredients(testUser)).willReturn(Collections.singletonList(testIngredient));

        mockMvc.perform(get("/ingredients")
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ingredientName").value("Tomato"));
    }

    @Test
    public void getIngredients_missingToken_throwsUnauthorized() throws Exception {
        mockMvc.perform(get("/ingredients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getIngredients_invalidToken_throwsUnauthorized() throws Exception {
        given(userService.getUserByToken("invalid-token")).willReturn(null);

        mockMvc.perform(get("/ingredients")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createIngredient_success() throws Exception {
        IngredientPostDTO dto = new IngredientPostDTO();
        dto.setIngredientName("Tomato");
        dto.setCategory(IngredientCategory.VEGETABLE);
        dto.setStandardUnit(Unit.PIECE);

        given(userService.getUserByToken("valid-token")).willReturn(testUser);
        given(ingredientService.createIngredient(any(Ingredient.class))).willReturn(testIngredient);

        mockMvc.perform(post("/ingredients")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ingredientName").value("Tomato"));
    }

    @Test
    public void autocompleteIngredients_success() throws Exception {
        IngredientAutocompleteRequestDTO dto = new IngredientAutocompleteRequestDTO();
        dto.setFoundIngredients(List.of("tom"));

        IngredientService.IngredientAutocompleteResult result = new IngredientService.IngredientAutocompleteResult(
                "tom", "Tomato", 1L, 0.9, true
        );

        given(ingredientService.autocompleteIngredients(List.of("tom"))).willReturn(List.of(result));

        mockMvc.perform(post("/ingredients/autocomplete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].input").value("tom"))
                .andExpect(jsonPath("$[0].ingredientName").value("Tomato"))
                .andExpect(jsonPath("$[0].ingredientId").value(1))
                .andExpect(jsonPath("$[0].similarity").value(0.9))
                .andExpect(jsonPath("$[0].matched").value(true));
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e));
        }
    }
}

