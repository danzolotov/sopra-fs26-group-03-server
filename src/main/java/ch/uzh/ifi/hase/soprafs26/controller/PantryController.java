package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.PantryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class PantryController {

	private final PantryService pantryService;
	private final GroupService groupService;

	@Autowired
	public PantryController(PantryService pantryService, GroupService groupService) {
		this.pantryService = pantryService;
		this.groupService = groupService;
	}

	@GetMapping("/groups/me/pantry")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PantryGetDTO getPantry(Authentication auth) {
		Group group = groupService.getGroupOfUser(auth.getName());
		Pantry pantry = pantryService.getPantryByGroupId(group.getId());
		return DTOMapper.INSTANCE.convertEntityToPantryGetDTO(pantry);
	}

	@PostMapping("/groups/me/pantry/items")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public PantryItemGetDTO addItem(Authentication auth, @RequestBody PantryItemPostDTO dto) {
		Group group = groupService.getGroupOfUser(auth.getName());
		Pantry pantry = pantryService.getPantryByGroupId(group.getId());
		PantryItem item = pantryService.addItemToPantry(
				pantry.getId(), dto.getIngredientId(), dto.getQuantity());
		return DTOMapper.INSTANCE.convertEntityToPantryItemGetDTO(item);
	}

	@GetMapping("/groups/me/pantry/items/{itemId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PantryItemGetDTO getItem(Authentication auth, @PathVariable Long itemId) {
		Group group = groupService.getGroupOfUser(auth.getName());
		PantryItem item = pantryService.getItemByIdAndVerifyGroup(itemId, group.getId());
		return DTOMapper.INSTANCE.convertEntityToPantryItemGetDTO(item);
	}

	@PutMapping("/groups/me/pantry/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateItem(Authentication auth, @PathVariable Long itemId,
			@RequestBody PantryItemPutDTO dto) {
		Group group = groupService.getGroupOfUser(auth.getName());
		pantryService.getItemByIdAndVerifyGroup(itemId, group.getId());
		pantryService.updateItem(itemId, dto.getIngredientId(), dto.getQuantity());
	}

	@DeleteMapping("/groups/me/pantry/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteItem(Authentication auth, @PathVariable Long itemId) {
		Group group = groupService.getGroupOfUser(auth.getName());
		pantryService.getItemByIdAndVerifyGroup(itemId, group.getId());
		pantryService.deleteItem(itemId);
	}
}
