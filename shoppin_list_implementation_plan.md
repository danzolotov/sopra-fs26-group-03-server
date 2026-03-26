# Shopping List Backend Implementation

Implement the Shopping List feature backend for PlateMate, covering CRUD operations for shopping lists and their items, per the REST specification, UML diagrams, and user stories from the M1 report.

## User Review Required

> [!IMPORTANT]
> **Scope decision**: This plan covers the Shopping List entities and endpoints only. The `HouseholdGroup` entity (which owns shopping lists) is being developed by your colleague on the auth backend. For now, `ShoppingList` will reference a `groupId` (Long) as a simple foreign key — the full JPA relationship to `HouseholdGroup` can be wired once that entity is merged. Similarly, we won't implement auth token validation yet.

> [!WARNING]
> **Ingredient entity**: The UML shows `Ingredient` as a shared entity used by ShoppingListItem, PantryItem, and RecipeItem. We'll create a basic `Ingredient` entity now. If another team member is also implementing Ingredient (e.g., for Pantry), we should coordinate.

## Proposed Changes

### Enums & Constants

#### [NEW] [Unit.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/constant/Unit.java)
Enum for measurement units: `GRAM`, `KILOGRAM`, `MILLILITER`, `CENTILITER`, `LITER`, `PIECE`, `TABLESPOON`, `TEASPOON`, `CUP`

---

### Entities

#### [NEW] [Ingredient.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/entity/Ingredient.java)
Fields: [id](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/test/java/ch/uzh/ifi/hase/soprafs26/service/UserServiceTest.java#42-57) (Long, auto-generated), `ingredientName` (String), `ingredientDescription` (String), `unit` (Unit enum)

#### [NEW] [ShoppingList.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/entity/ShoppingList.java)
Fields: [id](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/test/java/ch/uzh/ifi/hase/soprafs26/service/UserServiceTest.java#42-57) (Long, auto-generated), `groupId` (Long — FK to HouseholdGroup), `totalEstimatedCost` (Double), `items` (OneToMany → ShoppingListItem, cascade ALL, orphanRemoval)

#### [NEW] [ShoppingListItem.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/entity/ShoppingListItem.java)
Fields: [id](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/test/java/ch/uzh/ifi/hase/soprafs26/service/UserServiceTest.java#42-57) (Long, auto-generated), `quantity` (Integer), `isBought` (Boolean, default false), `estimatedPrice` (Double), `ingredient` (ManyToOne → Ingredient), `shoppingList` (ManyToOne → ShoppingList, JsonIgnore)

---

### Repositories

#### [NEW] [IngredientRepository.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/repository/IngredientRepository.java)
`JpaRepository<Ingredient, Long>` with `findByIngredientName(String name)`

#### [NEW] [ShoppingListRepository.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/repository/ShoppingListRepository.java)
`JpaRepository<ShoppingList, Long>` with `findAllByGroupId(Long groupId)`

#### [NEW] [ShoppingListItemRepository.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/repository/ShoppingListItemRepository.java)
`JpaRepository<ShoppingListItem, Long>`

---

### DTOs

#### [NEW] [ShoppingListPostDTO.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/rest/dto/ShoppingListPostDTO.java)
Fields: `groupId` (Long)

#### [NEW] [ShoppingListGetDTO.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/rest/dto/ShoppingListGetDTO.java)
Fields: [id](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/test/java/ch/uzh/ifi/hase/soprafs26/service/UserServiceTest.java#42-57), `groupId`, `totalEstimatedCost`, `items` (List of ShoppingListItemGetDTO)

#### [NEW] [ShoppingListItemPostDTO.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/rest/dto/ShoppingListItemPostDTO.java)
Fields: `ingredientId` (Long), `quantity` (Integer), `estimatedPrice` (Double)

#### [NEW] [ShoppingListItemGetDTO.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/rest/dto/ShoppingListItemGetDTO.java)
Fields: [id](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/test/java/ch/uzh/ifi/hase/soprafs26/service/UserServiceTest.java#42-57), `quantity`, `isBought`, `estimatedPrice`, `ingredientId`, `ingredientName`, `unit`

#### [NEW] [ItemPutDTO.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/rest/dto/ItemPutDTO.java)
Fields: `quantity` (Integer), `estimatedPrice` (Double), `ingredientId` (Long)

#### [NEW] [ItemPatchDTO.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/rest/dto/ItemPatchDTO.java)
Fields: `isBought` (Boolean)

---

### DTO Mapper

#### [MODIFY] [DTOMapper.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/rest/mapper/DTOMapper.java)
Add mapping methods:
- `ShoppingList convertShoppingListPostDTOtoEntity(ShoppingListPostDTO)`
- `ShoppingListGetDTO convertEntityToShoppingListGetDTO(ShoppingList)` — maps nested items
- `ShoppingListItem convertShoppingListItemPostDTOtoEntity(ShoppingListItemPostDTO)`
- `ShoppingListItemGetDTO convertEntityToShoppingListItemGetDTO(ShoppingListItem)` — maps `ingredient.ingredientName` → `ingredientName`, `ingredient.id` → `ingredientId`, `ingredient.unit` → `unit`

---

### Service

#### [NEW] [ShoppingListService.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/service/ShoppingListService.java)
Methods:
| Method | Description |
|---|---|
| `getShoppingLists()` | Returns all shopping lists |
| `createShoppingList(ShoppingList)` | Creates and persists a new shopping list |
| `getShoppingListById(Long)` | Finds by ID, throws 404 if not found |
| `updateShoppingList(Long, ShoppingList)` | Updates groupId; recalculates total cost |
| `deleteShoppingList(Long)` | Deletes by ID, throws 404 if not found |
| `addItemToShoppingList(Long, ShoppingListItem, Long ingredientId)` | Adds item, links ingredient, recalculates total |
| `getItemById(Long)` | Finds item by ID, throws 404 |
| `updateItem(Long, ShoppingListItem, Long ingredientId)` | Updates item fields, recalculates total |
| `patchItemBoughtStatus(Long, boolean)` | Toggles isBought flag |
| `deleteItem(Long)` | Removes item, recalculates total on parent list |
| `recalculateTotalCost(ShoppingList)` | Sums `quantity * estimatedPrice` across items |

---

### Controllers

#### [NEW] [ShoppingListController.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/controller/ShoppingListController.java)

| Method | Endpoint | Status |
|---|---|---|
| GET | `/shopping-lists` | 200 |
| POST | `/shopping-lists` | 201 |
| GET | `/shopping-lists/{shoppingListId}` | 200 |
| PUT | `/shopping-lists/{shoppingListId}` | 204 |
| DELETE | `/shopping-lists/{shoppingListId}` | 204 |
| POST | `/shopping-lists/{shoppingListId}/items` | 201 |

#### [NEW] [ItemController.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/main/java/ch/uzh/ifi/hase/soprafs26/controller/ItemController.java)

| Method | Endpoint | Status |
|---|---|---|
| GET | `/items/{itemId}` | 200 |
| PUT | `/items/{itemId}` | 204 |
| PATCH | `/items/{itemId}` | 200 |
| DELETE | `/items/{itemId}` | 204 |

---

## Verification Plan

### Automated Tests

All tests run via: `./gradlew test`

#### Unit Tests

**[NEW] `ShoppingListServiceTest.java`** — Mockito-based tests mirroring the existing [UserServiceTest](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/test/java/ch/uzh/ifi/hase/soprafs26/service/UserServiceTest.java#17-87) pattern:
- `createShoppingList_validInput_success` — verifies save is called, totalEstimatedCost starts at 0
- `getShoppingListById_notFound_throwsException` — verifies 404 ResponseStatusException
- `addItemToShoppingList_validInput_success` — verifies item added, total recalculated
- `deleteItem_recalculatesTotalCost` — verifies total updates after item removal
- `patchItemBoughtStatus_success` — verifies isBought is toggled

#### Controller Tests (WebMvc)

**[NEW] `ShoppingListControllerTest.java`** — MockMvc tests mirroring [UserControllerTest](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/src/test/java/ch/uzh/ifi/hase/soprafs26/controller/UserControllerTest.java#40-121):
- `getAllShoppingLists_returnsJsonArray` — GET `/shopping-lists`
- `createShoppingList_validInput_returnsCreated` — POST `/shopping-lists`
- `getShoppingListById_returnsJson` — GET `/shopping-lists/1`
- `deleteShoppingList_returnsNoContent` — DELETE `/shopping-lists/1`
- `addItem_validInput_returnsCreated` — POST `/shopping-lists/1/items`

**[NEW] `ItemControllerTest.java`** — MockMvc tests:
- `getItem_returnsJson` — GET `/items/1`
- `updateItem_returnsNoContent` — PUT `/items/1`
- `patchItemBought_returnsJson` — PATCH `/items/1`
- `deleteItem_returnsNoContent` — DELETE `/items/1`

### Build Verification

After all files are created, run: `./gradlew build` to verify compilation and all tests pass.
