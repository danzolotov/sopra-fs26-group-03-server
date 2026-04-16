package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;

@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	// ─── User mappings (from main) ──────────────────

	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "email", target = "email")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "passwordHash")
	User convertRegisterPostDTOtoEntity(RegisterPostDTO registerPostDTO);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "passwordHash")
	User convertLoginPostDTOtoEntity(LoginPostDTO loginPostDTO);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "email", target = "email")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "passwordHash")
	@Mapping(source = "bio", target = "bio")
	@Mapping(source = "profilePicture", target = "profilePicture")
	@Mapping(source = "status", target = "status")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "userID", target = "userID")
	@Mapping(source = "email", target = "email")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "bio", target = "bio")
	@Mapping(source = "profilePicture", target = "profilePicture")
	@Mapping(source = "status", target = "status")
	UserGetDTO convertEntityToUserGetDTO(User user);

	@Mapping(source = "userID", target = "userID")
	@Mapping(source = "token", target = "token")
	LoginGetDTO convertEntityToLoginGetDTO(User user);

	// ─── Group mappings ─────────────────────────────

	@Mapping(source = "memberships", target = "members")
	GroupGetDTO convertEntityToGroupGetDTO(Group group);

	@Mapping(source = "user.userID", target = "userID")
	@Mapping(source = "user.username", target = "username")
	@Mapping(source = "role", target = "role")
	@Mapping(source = "joinedAt", target = "joinedAt")
	GroupMemberGetDTO convertEntityToGroupMemberGetDTO(GroupMembership membership);

	// ─── Shopping list mappings ─────────────────────

	@Mapping(source = "id", target = "id")
	@Mapping(source = "groupId", target = "groupId")
	@Mapping(source = "items", target = "items")
	ShoppingListGetDTO convertEntityToShoppingListGetDTO(ShoppingList shoppingList);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "quantity", target = "quantity")
	@Mapping(source = "isBought", target = "isBought")
	@Mapping(source = "ingredient.id", target = "ingredientId")
	@Mapping(source = "ingredient.ingredientName", target = "ingredientName")
	@Mapping(source = "ingredient.unit", target = "unit")
	ShoppingListItemGetDTO convertEntityToShoppingListItemGetDTO(ShoppingListItem item);

	// ─── Pantry mappings ────────────────────────────

	@Mapping(source = "id", target = "id")
	@Mapping(source = "groupId", target = "groupId")
	@Mapping(source = "items", target = "items")
	PantryGetDTO convertEntityToPantryGetDTO(Pantry pantry);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "quantity", target = "quantity")
	@Mapping(source = "ingredient.id", target = "ingredientId")
	@Mapping(source = "ingredient.ingredientName", target = "ingredientName")
	@Mapping(source = "ingredient.unit", target = "unit")
	PantryItemGetDTO convertEntityToPantryItemGetDTO(PantryItem pantryItem);

	// ─── Ingredient mappings ────────────────────────

	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "ingredientName", target = "ingredientName")
	@Mapping(source = "ingredientDescription", target = "ingredientDescription")
	@Mapping(source = "unit", target = "unit")
	Ingredient convertIngredientPostDTOtoEntity(IngredientPostDTO ingredientPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "ingredientName", target = "ingredientName")
	@Mapping(source = "ingredientDescription", target = "ingredientDescription")
	@Mapping(source = "unit", target = "unit")
	IngredientGetDTO convertEntityToIngredientGetDTO(Ingredient ingredient);
}
