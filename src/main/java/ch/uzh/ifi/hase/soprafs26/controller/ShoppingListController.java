package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.IngredientService;
import ch.uzh.ifi.hase.soprafs26.service.ShoppingListAutoDetectService;
import ch.uzh.ifi.hase.soprafs26.service.ShoppingListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
public class ShoppingListController {

	private final ShoppingListService shoppingListService;
	private final GroupService groupService;
	private final ShoppingListAutoDetectService shoppingListAutoDetectService;
	private final IngredientService ingredientService;

	@Autowired
	public ShoppingListController(ShoppingListService shoppingListService, GroupService groupService,
			ShoppingListAutoDetectService shoppingListAutoDetectService, IngredientService ingredientService) {
		this.shoppingListService = shoppingListService;
		this.groupService = groupService;
		this.shoppingListAutoDetectService = shoppingListAutoDetectService;
		this.ingredientService = ingredientService;
	}

	@PostMapping(value = "/shoppings-list/auto-detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<AutoDetectedIngredientGetDTO> autoDetectIngredients(Authentication auth, @RequestParam("file") MultipartFile file) {
		groupService.getGroupOfUser(auth.getName());
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A non-empty image file is required");
		}

		try {
			List<ShoppingListAutoDetectService.DetectedShoppingItem> detectedItems =
					shoppingListAutoDetectService.detectShoppingListItemsWithQuantities(file.getBytes());

			Map<String, AutoDetectedIngredientGetDTO> aggregated = new LinkedHashMap<>();
			for (ShoppingListAutoDetectService.DetectedShoppingItem detectedItem : detectedItems) {
				Ingredient ingredient = ingredientService.resolveOrCreateDetectedIngredient(detectedItem.getIngredientName());
				if (ingredient == null) {
					continue;
				}

				String aggregationKey = ingredient.getId() != null
						? "id:" + ingredient.getId()
						: "name:" + ingredient.getIngredientName().toLowerCase(Locale.ROOT);
				AutoDetectedIngredientGetDTO dto = aggregated.get(aggregationKey);
				if (dto == null) {
					dto = new AutoDetectedIngredientGetDTO();
					dto.setId(ingredient.getId());
					dto.setIngredientName(ingredient.getIngredientName());
					dto.setIngredientDescription(ingredient.getIngredientDescription());
					dto.setUnit(ingredient.getUnit());
					dto.setQuantity(0);
					aggregated.put(aggregationKey, dto);
				}
				dto.setQuantity(dto.getQuantity() + detectedItem.getQuantity());
			}

			return aggregated.values().stream().toList();
		}
		catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded image", e);
		}
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
