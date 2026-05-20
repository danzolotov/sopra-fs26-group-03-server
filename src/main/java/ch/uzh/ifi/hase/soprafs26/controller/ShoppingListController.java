package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.IngredientService;
import ch.uzh.ifi.hase.soprafs26.service.PantryService;
import ch.uzh.ifi.hase.soprafs26.service.ShoppingListAutoDetectService;
import ch.uzh.ifi.hase.soprafs26.service.ShoppingListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
	private final PantryService pantryService;
	private final ch.uzh.ifi.hase.soprafs26.service.UserService userService;
	private final SimpMessagingTemplate messagingTemplate;

	@Autowired
	public ShoppingListController(ShoppingListService shoppingListService, GroupService groupService,
			ShoppingListAutoDetectService shoppingListAutoDetectService, IngredientService ingredientService,
			ch.uzh.ifi.hase.soprafs26.service.UserService userService, SimpMessagingTemplate messagingTemplate,
			PantryService pantryService) {
		this.shoppingListService = shoppingListService;
		this.groupService = groupService;
		this.shoppingListAutoDetectService = shoppingListAutoDetectService;
		this.ingredientService = ingredientService;
		this.userService = userService;
		this.messagingTemplate = messagingTemplate;
		this.pantryService = pantryService;
	}

	private void broadcastUpdate(Long groupId) {
		ShoppingList list = shoppingListService.getShoppingListByGroupId(groupId);
		ShoppingListGetDTO dto = DTOMapper.INSTANCE.convertEntityToShoppingListGetDTO(list);
		messagingTemplate.convertAndSend("/topic/shopping-list/" + groupId, dto);
	}

	private void broadcastPantryUpdate(Long groupId) {
		Pantry pantry = pantryService.getPantryByGroupId(groupId);
		PantryGetDTO dto = DTOMapper.INSTANCE.convertEntityToPantryGetDTO(pantry);
		messagingTemplate.convertAndSend("/topic/pantry/" + groupId, dto);
	}

	@PostMapping(value = "/shoppings-list/auto-detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.OK)
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
				// Resolve current user and forward to ingredient service so created ingredients are user-scoped
				User currentUser = userService.getUserByUsername(auth.getName());
				Ingredient ingredient = ingredientService.resolveOrCreateDetectedIngredient(detectedItem.ingredientName(), currentUser);
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
				dto.setQuantity(dto.getQuantity() + detectedItem.quantity());
			}

			return aggregated.values().stream().toList();
		}
		catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded image", e);
		}
	}

	@GetMapping("/groups/me/shopping-list")
	@ResponseStatus(HttpStatus.OK)
	public ShoppingListGetDTO getShoppingList(Authentication auth) {
		Group group = groupService.getGroupOfUser(auth.getName());
		ShoppingList list = shoppingListService.getShoppingListByGroupId(group.getId());
		return DTOMapper.INSTANCE.convertEntityToShoppingListGetDTO(list);
	}

	@PostMapping("/groups/me/shopping-list/items")
	@ResponseStatus(HttpStatus.CREATED)
	public ShoppingListItemGetDTO addItem(Authentication auth, @RequestBody ShoppingListItemPostDTO dto) {
		Group group = groupService.getGroupOfUser(auth.getName());
		ShoppingList list = shoppingListService.getShoppingListByGroupId(group.getId());
		ShoppingListItem item = shoppingListService.addItemToList(list.getId(), dto.getIngredientId(), dto.getIngredientName(),
				dto.getIngredientDescription(), dto.getStandardUnit(), dto.getCategory(), dto.getQuantity());
		broadcastUpdate(group.getId());
		return DTOMapper.INSTANCE.convertEntityToShoppingListItemGetDTO(item);
	}

	@GetMapping("/groups/me/shopping-list/items/{itemId}")
	@ResponseStatus(HttpStatus.OK)
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
		shoppingListService.updateItem(itemId, dto.getIngredientId(), dto.getQuantity(), dto.getUnit());
		broadcastUpdate(group.getId());
	}

	@PatchMapping("/groups/me/shopping-list/items/{itemId}")
	@ResponseStatus(HttpStatus.OK)
	public ShoppingListItemGetDTO patchItemBoughtStatus(Authentication auth, @PathVariable Long itemId,
			@RequestBody ItemPatchDTO dto) {
		Group group = groupService.getGroupOfUser(auth.getName());
		shoppingListService.getItemByIdAndVerifyGroup(itemId, group.getId());
		ShoppingListItem item = shoppingListService.patchItemBoughtStatus(itemId, dto.getIsBought());
		broadcastUpdate(group.getId());
		if (Boolean.TRUE.equals(dto.getIsBought())) {
			broadcastPantryUpdate(group.getId());
		}
		return DTOMapper.INSTANCE.convertEntityToShoppingListItemGetDTO(item);
	}

	@DeleteMapping("/groups/me/shopping-list/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteItem(Authentication auth, @PathVariable Long itemId) {
		Group group = groupService.getGroupOfUser(auth.getName());
		shoppingListService.getItemByIdAndVerifyGroup(itemId, group.getId());
		shoppingListService.deleteItem(itemId);
		broadcastUpdate(group.getId());
	}
}
