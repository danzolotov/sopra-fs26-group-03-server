package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.constant.IngredientCategory;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.Recipe;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.RecipeRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.service.IngredientService;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

/**
 * DatabaseSeeder initializes the database with default data on startup.
 * It seeds standard test user and default recipes if they don't already exist.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    private final UserRepository userRepository;
    private final IngredientService ingredientService;
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DatabaseSeeder(UserRepository userRepository,
            IngredientService ingredientService,
            RecipeRepository recipeRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.ingredientService = ingredientService;
        this.recipeRepository = recipeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Starting database seeding process...");
        ingredientService.seedIngredients();
        seedTestUser();
        seedRecipes();
        log.info("Database seeding process completed.");
    }

    private void seedRecipes() {
        boolean needsSeeding = recipeRepository.count() == 0;
        if (!needsSeeding) {
            for (Recipe recipe : recipeRepository.findAll()) {
                if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
                    needsSeeding = true;
                    break;
                }
            }
        }

        if (needsSeeding) {
            log.info("Recipes table needs seeding/reseeding. Re-seeding recipe ingredients...");

            // 1. Pasta Carbonara
            Recipe carbonara = getOrCreateRecipe("Pasta Carbonara",
                    "Classic Italian pasta dish with eggs, cheese, and pancetta.");
            createAndAddIngredient(carbonara, "Spaghetti", Unit.GRAM, 200, IngredientCategory.GRAIN);
            createAndAddIngredient(carbonara, "Eggs", Unit.PIECE, 2, IngredientCategory.EGGS);
            createAndAddIngredient(carbonara, "Pecorino Romano", Unit.GRAM, 50, IngredientCategory.DAIRY);
            createAndAddIngredient(carbonara, "Pancetta", Unit.GRAM, 100, IngredientCategory.MEAT);
            recipeRepository.save(carbonara);

            // 2. Vegetable Stir Fry
            Recipe stirFry = getOrCreateRecipe("Vegetable Stir Fry",
                    "Quick and healthy stir-fry with seasonal vegetables.");
            createAndAddIngredient(stirFry, "Basmati Rice", Unit.GRAM, 150, IngredientCategory.GRAIN);
            createAndAddIngredient(stirFry, "Broccoli", Unit.GRAM, 200, IngredientCategory.VEGETABLE);
            createAndAddIngredient(stirFry, "Carrots", Unit.PIECE, 2, IngredientCategory.VEGETABLE);
            createAndAddIngredient(stirFry, "Soy Sauce", Unit.MILLILITER, 30, IngredientCategory.CONDIMENT);
            recipeRepository.save(stirFry);

            // 3. Chicken Caesar Salad
            Recipe caesar = getOrCreateRecipe("Chicken Caesar Salad",
                    "Crispy romaine lettuce, grilled chicken, and Caesar dressing.");
            createAndAddIngredient(caesar, "Chicken Breast", Unit.GRAM, 200, IngredientCategory.MEAT);
            createAndAddIngredient(caesar, "Romaine Lettuce", Unit.PIECE, 1, IngredientCategory.VEGETABLE);
            createAndAddIngredient(caesar, "Croutons", Unit.GRAM, 50, IngredientCategory.BAKERY);
            createAndAddIngredient(caesar, "Caesar Dressing", Unit.MILLILITER, 50, IngredientCategory.CONDIMENT);
            recipeRepository.save(caesar);

            // 4. Beef Tacos
            Recipe tacos = getOrCreateRecipe("Beef Tacos",
                    "Mexican-style tacos with seasoned ground beef and fresh toppings.");
            createAndAddIngredient(tacos, "Ground Beef", Unit.GRAM, 250, IngredientCategory.MEAT);
            createAndAddIngredient(tacos, "Taco Shells", Unit.PIECE, 3, IngredientCategory.BAKERY);
            createAndAddIngredient(tacos, "Shredded Cheese", Unit.GRAM, 50, IngredientCategory.DAIRY);
            createAndAddIngredient(tacos, "Lettuce", Unit.GRAM, 30, IngredientCategory.VEGETABLE);
            recipeRepository.save(tacos);

            // 5. Mushroom Risotto
            Recipe risotto = getOrCreateRecipe("Mushroom Risotto",
                    "Creamy Italian rice dish with mushrooms and parmesan.");
            createAndAddIngredient(risotto, "Arborio Rice", Unit.GRAM, 150, IngredientCategory.GRAIN);
            createAndAddIngredient(risotto, "Mushrooms", Unit.GRAM, 150, IngredientCategory.VEGETABLE);
            createAndAddIngredient(risotto, "Vegetable Broth", Unit.MILLILITER, 500, IngredientCategory.OTHER);
            createAndAddIngredient(risotto, "Parmesan", Unit.GRAM, 40, IngredientCategory.DAIRY);
            recipeRepository.save(risotto);

            // 6. Greek Salad
            Recipe greek = getOrCreateRecipe("Greek Salad",
                    "Refreshingly crisp salad with cucumbers, tomatoes, and feta cheese.");
            createAndAddIngredient(greek, "Cucumber", Unit.PIECE, 1, IngredientCategory.VEGETABLE);
            createAndAddIngredient(greek, "Tomatoes", Unit.PIECE, 2, IngredientCategory.VEGETABLE);
            createAndAddIngredient(greek, "Feta Cheese", Unit.GRAM, 100, IngredientCategory.DAIRY);
            createAndAddIngredient(greek, "Olives", Unit.GRAM, 50, IngredientCategory.OTHER);
            recipeRepository.save(greek);

            // 7. Tomato Soup
            Recipe soup = getOrCreateRecipe("Tomato Soup", "Comforting homemade tomato soup with basil.");
            createAndAddIngredient(soup, "Canned Tomatoes", Unit.GRAM, 400, IngredientCategory.VEGETABLE);
            createAndAddIngredient(soup, "Onion", Unit.PIECE, 1, IngredientCategory.VEGETABLE);
            createAndAddIngredient(soup, "Garlic", Unit.PIECE, 2, IngredientCategory.VEGETABLE);
            createAndAddIngredient(soup, "Cream", Unit.MILLILITER, 100, IngredientCategory.DAIRY);
            recipeRepository.save(soup);

            // 8. Pancakes
            Recipe pancakes = getOrCreateRecipe("Pancakes", "Fluffy breakfast pancakes served with maple syrup.");
            createAndAddIngredient(pancakes, "Flour", Unit.GRAM, 200, IngredientCategory.BAKING);
            createAndAddIngredient(pancakes, "Milk", Unit.MILLILITER, 250, IngredientCategory.DAIRY);
            createAndAddIngredient(pancakes, "Eggs", Unit.PIECE, 1, IngredientCategory.EGGS);
            createAndAddIngredient(pancakes, "Maple Syrup", Unit.MILLILITER, 50, IngredientCategory.CONDIMENT);
            recipeRepository.save(pancakes);

            // 9. Guacamole
            Recipe guacamole = getOrCreateRecipe("Guacamole", "Fresh avocado dip with lime and cilantro.");
            createAndAddIngredient(guacamole, "Avocados", Unit.PIECE, 2, IngredientCategory.VEGETABLE);
            createAndAddIngredient(guacamole, "Limes", Unit.PIECE, 1, IngredientCategory.FRUIT);
            createAndAddIngredient(guacamole, "Onion", Unit.PIECE, 1, IngredientCategory.VEGETABLE);
            createAndAddIngredient(guacamole, "Tortilla Chips", Unit.GRAM, 100, IngredientCategory.BAKERY);
            recipeRepository.save(guacamole);

            // 10. Club Sandwich
            Recipe club = getOrCreateRecipe("Club Sandwich", "Triple-decker sandwich with turkey, bacon, and lettuce.");
            createAndAddIngredient(club, "Bread Slices", Unit.PIECE, 3, IngredientCategory.BAKERY);
            createAndAddIngredient(club, "Turkey Breast", Unit.GRAM, 50, IngredientCategory.MEAT);
            createAndAddIngredient(club, "Bacon", Unit.GRAM, 30, IngredientCategory.MEAT);
            createAndAddIngredient(club, "Mayonnaise", Unit.MILLILITER, 20, IngredientCategory.CONDIMENT);
            recipeRepository.save(club);

            log.info("Recipe seeding/reseeding complete. Total recipes: {}", recipeRepository.count());
        } else {
            log.info("Recipes already exist and have ingredients. Skipping recipe seeding.");
        }
    }

    private Recipe getOrCreateRecipe(String name, String desc) {
        Recipe r = recipeRepository.findByName(name);
        if (r == null) {
            r = new Recipe();
            r.setName(name);
            r.setDescription(desc);
            r.setIngredients(new ArrayList<>());
        } else {
            r.setDescription(desc);
            r.getIngredients().clear();
        }
        return r;
    }

    private void createAndAddIngredient(Recipe recipe, String name, Unit unit, Integer quantity,
            IngredientCategory category) {
        Ingredient ing = new Ingredient();
        ing.setIngredientName(name);
        ing.setUnit(unit);
        ing.setQuantity(quantity);
        ing.setCategory(category);
        ing.setIngredientDescription("");
        ing.setUser(null); // Linked to this recipe, not owned by any user
        recipe.getIngredients().add(ing);
    }
}
