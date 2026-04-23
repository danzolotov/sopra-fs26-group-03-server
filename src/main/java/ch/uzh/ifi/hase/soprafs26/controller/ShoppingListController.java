package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.ShoppingListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ShoppingListController {

	private final ShoppingListService shoppingListService;
	private final GroupService groupService;

	@Autowired
	public ShoppingListController(ShoppingListService shoppingListService, GroupService groupService) {
		this.shoppingListService = shoppingListService;
		this.groupService = groupService;
	}

	@GetMapping("/groups/me/shopping-list")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ShoppingListGetDTO getShoppingList(Authentication auth) {
		Group group = groupService.getGroupOfUser(auth.getName());
		ShoppingList list = shoppingListService.getShoppingListByGroupId(group.getId());
		return DTOMapper.INSTANCE.convertEntityToShoppingListGetDTO(list);
	}

	@PostMapping("/groups/me/shopping-list/items")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public ShoppingListItemGetDTO addItem(Authentication auth, @RequestBody ShoppingListItemPostDTO dto) {
		Group group = groupService.getGroupOfUser(auth.getName());
		ShoppingList list = shoppingListService.getShoppingListByGroupId(group.getId());
		ShoppingListItem item = shoppingListService.addItemToList(
				list.getId(), dto.getIngredientId(), dto.getQuantity());
		return DTOMapper.INSTANCE.convertEntityToShoppingListItemGetDTO(item);
	}

	@GetMapping("/groups/me/shopping-list/items/{itemId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ShoppingListItemGetDTO getItem(Authentication auth, @PathVariable Long itemId) {
		Group group = groupService.getGroupOfUser(auth.getName());
		ShoppingListItem item = shoppingListService.getItemByIdAndVerifyGroup(itemId, group.getId());
		return DTOMapper.INSTANCE.convertEntityToShoppingListItemGetDTO(item);
	}

	@PutMapping("/groups/me/shopping-list/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateItem(Authentication auth, @PathVariable Long itemId,
			@RequestBody ItemPutDTO dto) {
		Group group = groupService.getGroupOfUser(auth.getName());
		shoppingListService.getItemByIdAndVerifyGroup(itemId, group.getId());
		shoppingListService.updateItem(itemId, dto.getIngredientId(), dto.getQuantity());
	}

	@PatchMapping("/groups/me/shopping-list/items/{itemId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ShoppingListItemGetDTO patchItemBoughtStatus(Authentication auth, @PathVariable Long itemId,
			@RequestBody ItemPatchDTO dto) {
		Group group = groupService.getGroupOfUser(auth.getName());
		shoppingListService.getItemByIdAndVerifyGroup(itemId, group.getId());
		ShoppingListItem item = shoppingListService.patchItemBoughtStatus(itemId, dto.getIsBought());
		return DTOMapper.INSTANCE.convertEntityToShoppingListItemGetDTO(item);
	}

	@DeleteMapping("/groups/me/shopping-list/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteItem(Authentication auth, @PathVariable Long itemId) {
		Group group = groupService.getGroupOfUser(auth.getName());
		shoppingListService.getItemByIdAndVerifyGroup(itemId, group.getId());
		shoppingListService.deleteItem(itemId);
	}
}
