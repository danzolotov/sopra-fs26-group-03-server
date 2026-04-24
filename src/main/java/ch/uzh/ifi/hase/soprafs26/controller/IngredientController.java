package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientAutocompleteMatchDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientAutocompleteRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.IngredientService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class IngredientController {

	private final IngredientService ingredientService;
	private final UserService userService;

	private static final String AUTH_COOKIE_NAME = "AUTH_TOKEN";

	public IngredientController(IngredientService ingredientService, UserService userService) {
		this.ingredientService = ingredientService;
		this.userService = userService;
	}

	@GetMapping("/ingredients")
	@ResponseStatus(HttpStatus.OK)
	public List<IngredientGetDTO> getIngredients(HttpServletRequest request) {
		User user = resolveUserFromRequest(request);
		return ingredientService.getIngredients(user).stream()
				.map(DTOMapper.INSTANCE::convertEntityToIngredientGetDTO)
				.toList();
	}

    @PostMapping("/ingredients")
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientGetDTO createIngredient(@RequestBody IngredientPostDTO dto, HttpServletRequest request) {
        User user = resolveUserFromRequest(request);
        Ingredient ingredient = DTOMapper.INSTANCE.convertIngredientPostDTOtoEntity(dto);
        ingredient.setUser(user);
        Ingredient createdIngredient = ingredientService.createIngredient(ingredient);
        return DTOMapper.INSTANCE.convertEntityToIngredientGetDTO(createdIngredient);
    }

@PostMapping("/ingredients/autocomplete")
@ResponseStatus(HttpStatus.OK)
public List<IngredientAutocompleteMatchDTO> autocompleteIngredients(@RequestBody IngredientAutocompleteRequestDTO dto) {
List<String> foundIngredients = dto == null ? List.of() : dto.getFoundIngredients();
return ingredientService.autocompleteIngredients(foundIngredients).stream()
.map(this::toAutocompleteMatchDTO)
.toList();
}

    private User resolveUserFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    User user = userService.getUserByToken(token);
                    if (user == null) {
                        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid authentication token");
                    }
                    return user;
                }
            }
        }
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing authentication token");
    }

private IngredientAutocompleteMatchDTO toAutocompleteMatchDTO(IngredientService.IngredientAutocompleteResult result) {
IngredientAutocompleteMatchDTO dto = new IngredientAutocompleteMatchDTO();
dto.setInput(result.input());
dto.setIngredientName(result.ingredientName());
dto.setIngredientId(result.ingredientId());
dto.setSimilarity(result.similarity());
dto.setMatched(result.matched());
return dto;
}
}
