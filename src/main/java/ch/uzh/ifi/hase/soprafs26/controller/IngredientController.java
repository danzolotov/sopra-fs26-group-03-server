package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientAutocompleteMatchDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientAutocompleteRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.IngredientPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.IngredientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class IngredientController {

private final IngredientService ingredientService;

public IngredientController(IngredientService ingredientService) {
this.ingredientService = ingredientService;
}

@GetMapping("/ingredients")
@ResponseStatus(HttpStatus.OK)
@ResponseBody
public List<IngredientGetDTO> getIngredients() {
return ingredientService.getIngredients().stream()
.map(DTOMapper.INSTANCE::convertEntityToIngredientGetDTO)
.toList();
}

@PostMapping("/ingredients")
@ResponseStatus(HttpStatus.CREATED)
@ResponseBody
public IngredientGetDTO createIngredient(@RequestBody IngredientPostDTO dto) {
Ingredient ingredient = DTOMapper.INSTANCE.convertIngredientPostDTOtoEntity(dto);
Ingredient createdIngredient = ingredientService.createIngredient(ingredient);
return DTOMapper.INSTANCE.convertEntityToIngredientGetDTO(createdIngredient);
}

@PostMapping("/ingredients/autocomplete")
@ResponseStatus(HttpStatus.OK)
@ResponseBody
public List<IngredientAutocompleteMatchDTO> autocompleteIngredients(@RequestBody IngredientAutocompleteRequestDTO dto) {
List<String> foundIngredients = dto == null ? List.of() : dto.getFoundIngredients();
return ingredientService.autocompleteIngredients(foundIngredients).stream()
.map(this::toAutocompleteMatchDTO)
.toList();
}

private IngredientAutocompleteMatchDTO toAutocompleteMatchDTO(IngredientService.IngredientAutocompleteResult result) {
IngredientAutocompleteMatchDTO dto = new IngredientAutocompleteMatchDTO();
dto.setInput(result.getInput());
dto.setIngredientName(result.getIngredientName());
dto.setIngredientId(result.getIngredientId());
dto.setSimilarity(result.getSimilarity());
dto.setMatched(result.isMatched());
return dto;
}
}
