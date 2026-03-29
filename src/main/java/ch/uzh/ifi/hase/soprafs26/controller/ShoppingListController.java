package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.ShoppingList;
import ch.uzh.ifi.hase.soprafs26.entity.ShoppingListItem;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.ShoppingListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ShoppingListController {

	private final ShoppingListService shoppingListService;

	@Autowired
	public ShoppingListController(ShoppingListService shoppingListService) {
		this.shoppingListService = shoppingListService;
	}

	@GetMapping("/shoppingLists")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ShoppingListGetDTO> getAllShoppingLists() {
		List<ShoppingList> shoppingLists = shoppingListService.getShoppingLists();
		List<ShoppingListGetDTO> shoppingListGetDTOs = new ArrayList<>();

		for (ShoppingList shoppingList : shoppingLists) {
			shoppingListGetDTOs.add(DTOMapper.INSTANCE.convertEntityToShoppingListGetDTO(shoppingList));
		}
		return shoppingListGetDTOs;
	}

	@PostMapping("/shoppingLists")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public ShoppingListGetDTO createShoppingList(@RequestBody ShoppingListPostDTO shoppingListPostDTO) {
		ShoppingList userInput = DTOMapper.INSTANCE.convertShoppingListPostDTOtoEntity(shoppingListPostDTO);
		ShoppingList createdShoppingList = shoppingListService.createShoppingList(userInput);
		return DTOMapper.INSTANCE.convertEntityToShoppingListGetDTO(createdShoppingList);
	}

	@GetMapping("/shoppingLists/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ShoppingListGetDTO getShoppingListById(@PathVariable Long id) {
		ShoppingList shoppingList = shoppingListService.getShoppingListById(id);
		return DTOMapper.INSTANCE.convertEntityToShoppingListGetDTO(shoppingList);
	}

	@PutMapping("/shoppingLists/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ResponseBody
	public void updateShoppingList(@PathVariable Long id, @RequestBody ShoppingListPostDTO shoppingListPostDTO) {
		ShoppingList listUpdate = DTOMapper.INSTANCE.convertShoppingListPostDTOtoEntity(shoppingListPostDTO);
		shoppingListService.updateShoppingList(id, listUpdate);
	}

	@DeleteMapping("/shoppingLists/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ResponseBody
	public void deleteShoppingList(@PathVariable Long id) {
		shoppingListService.deleteShoppingList(id);
	}

	@PostMapping("/shoppingLists/{listId}/items")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public ShoppingListItemGetDTO addItemToShoppingList(@PathVariable Long listId,
			@RequestBody ShoppingListItemPostDTO shoppingListItemPostDTO) {
		ShoppingListItem newItem = DTOMapper.INSTANCE.convertShoppingListItemPostDTOtoEntity(shoppingListItemPostDTO);
		ShoppingListItem createdItem = shoppingListService.addItemToShoppingList(listId, newItem,
				shoppingListItemPostDTO.getIngredientId());
		return DTOMapper.INSTANCE.convertEntityToShoppingListItemGetDTO(createdItem);
	}

	@GetMapping("/shoppingLists/items/{itemId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ShoppingListItemGetDTO getItemById(@PathVariable Long itemId) {
		ShoppingListItem item = shoppingListService.getItemById(itemId);
		return DTOMapper.INSTANCE.convertEntityToShoppingListItemGetDTO(item);
	}

	@PutMapping("/shoppingLists/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ResponseBody
	public void updateItem(@PathVariable Long itemId, @RequestBody ItemPutDTO itemPutDTO) {
		ShoppingListItem itemUpdate = new ShoppingListItem();
		itemUpdate.setQuantity(itemPutDTO.getQuantity());

		shoppingListService.updateItem(itemId, itemUpdate, itemPutDTO.getIngredientId());
	}

	@PatchMapping("/shoppingLists/items/{itemId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ShoppingListItemGetDTO patchItemBoughtStatus(@PathVariable Long itemId,
			@RequestBody ItemPatchDTO itemPatchDTO) {
		ShoppingListItem updatedItem = shoppingListService.patchItemBoughtStatus(itemId, itemPatchDTO.getIsBought());
		return DTOMapper.INSTANCE.convertEntityToShoppingListItemGetDTO(updatedItem);
	}

	@DeleteMapping("/shoppingLists/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ResponseBody
	public void deleteItem(@PathVariable Long itemId) {
		shoppingListService.deleteItem(itemId);
	}
}
