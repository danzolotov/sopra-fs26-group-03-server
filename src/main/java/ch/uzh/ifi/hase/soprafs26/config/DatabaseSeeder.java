package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.Recipe;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.RecipeRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * DatabaseSeeder initializes the database with default data on startup.
 * It seeds default recipes and a standard test user if they don't already exist.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DatabaseSeeder(RecipeRepository recipeRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Starting database seeding process...");
        seedTestUser();
        seedRecipes();
        log.info("Database seeding process completed.");
    }

    private void seedTestUser() {
        String testUsername = "testuser";
        User existingUser = userRepository.findByUsername(testUsername);
        if (existingUser == null) {
            log.info("Seeding default test user: {}...", testUsername);
            User testUser = new User();
            testUser.setUsername(testUsername);
            testUser.setEmail("test@platemate.ch");
            testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            testUser.setToken(UUID.randomUUID().toString());
            testUser.setStatus(UserStatus.OFFLINE);
            userRepository.save(testUser);
            log.info("Test user '{}' seeded successfully with password 'Password123!'.", testUsername);
        } else {
            log.info("Test user '{}' already exists. Forcing password update to 'Password123!'.", testUsername);
            existingUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            userRepository.save(existingUser);
        }
    }

    private void seedRecipes() {
        log.info("Checking if recipes need to be seeded...");
        long count = recipeRepository.count();
        if (count == 0) {
            log.info("Seeding 10 default recipes...");
            
            // 1. Pasta Carbonara
            Recipe carbonara = createRecipe("Pasta Carbonara", "Classic Italian pasta dish with eggs, cheese, and pancetta.");
            createAndAddIngredient(carbonara, "Spaghetti", Unit.GRAM, 200);
            createAndAddIngredient(carbonara, "Eggs", Unit.PIECE, 2);
            createAndAddIngredient(carbonara, "Pecorino Romano", Unit.GRAM, 50);
            createAndAddIngredient(carbonara, "Pancetta", Unit.GRAM, 100);
            recipeRepository.save(carbonara);

            // 2. Vegetable Stir Fry
            Recipe stirFry = createRecipe("Vegetable Stir Fry", "Quick and healthy stir-fry with seasonal vegetables.");
            createAndAddIngredient(stirFry, "Basmati Rice", Unit.GRAM, 150);
            createAndAddIngredient(stirFry, "Broccoli", Unit.GRAM, 200);
            createAndAddIngredient(stirFry, "Carrots", Unit.PIECE, 2);
            createAndAddIngredient(stirFry, "Soy Sauce", Unit.MILLILITER, 30);
            recipeRepository.save(stirFry);

            // 3. Chicken Caesar Salad
            Recipe caesar = createRecipe("Chicken Caesar Salad", "Crispy romaine lettuce, grilled chicken, and Caesar dressing.");
            createAndAddIngredient(caesar, "Chicken Breast", Unit.GRAM, 200);
            createAndAddIngredient(caesar, "Romaine Lettuce", Unit.PIECE, 1);
            createAndAddIngredient(caesar, "Croutons", Unit.GRAM, 50);
            createAndAddIngredient(caesar, "Caesar Dressing", Unit.MILLILITER, 50);
            recipeRepository.save(caesar);

            // 4. Beef Tacos
            Recipe tacos = createRecipe("Beef Tacos", "Mexican-style tacos with seasoned ground beef and fresh toppings.");
            createAndAddIngredient(tacos, "Ground Beef", Unit.GRAM, 250);
            createAndAddIngredient(tacos, "Taco Shells", Unit.PIECE, 3);
            createAndAddIngredient(tacos, "Shredded Cheese", Unit.GRAM, 50);
            createAndAddIngredient(tacos, "Lettuce", Unit.GRAM, 30);
            recipeRepository.save(tacos);

            // 5. Mushroom Risotto
            Recipe risotto = createRecipe("Mushroom Risotto", "Creamy Italian rice dish with mushrooms and parmesan.");
            createAndAddIngredient(risotto, "Arborio Rice", Unit.GRAM, 150);
            createAndAddIngredient(risotto, "Mushrooms", Unit.GRAM, 150);
            createAndAddIngredient(risotto, "Vegetable Broth", Unit.MILLILITER, 500);
            createAndAddIngredient(risotto, "Parmesan", Unit.GRAM, 40);
            recipeRepository.save(risotto);

            // 6. Greek Salad
            Recipe greek = createRecipe("Greek Salad", "Refreshingly crisp salad with cucumbers, tomatoes, and feta cheese.");
            createAndAddIngredient(greek, "Cucumber", Unit.PIECE, 1);
            createAndAddIngredient(greek, "Tomatoes", Unit.PIECE, 2);
            createAndAddIngredient(greek, "Feta Cheese", Unit.GRAM, 100);
            createAndAddIngredient(greek, "Olives", Unit.GRAM, 50);
            recipeRepository.save(greek);

            // 7. Tomato Soup
            Recipe soup = createRecipe("Tomato Soup", "Comforting homemade tomato soup with basil.");
            createAndAddIngredient(soup, "Canned Tomatoes", Unit.GRAM, 400);
            createAndAddIngredient(soup, "Onion", Unit.PIECE, 1);
            createAndAddIngredient(soup, "Garlic", Unit.PIECE, 2);
            createAndAddIngredient(soup, "Cream", Unit.MILLILITER, 100);
            recipeRepository.save(soup);

            // 8. Pancakes
            Recipe pancakes = createRecipe("Pancakes", "Fluffy breakfast pancakes served with maple syrup.");
            createAndAddIngredient(pancakes, "Flour", Unit.GRAM, 200);
            createAndAddIngredient(pancakes, "Milk", Unit.MILLILITER, 250);
            createAndAddIngredient(pancakes, "Eggs", Unit.PIECE, 1);
            createAndAddIngredient(pancakes, "Maple Syrup", Unit.MILLILITER, 50);
            recipeRepository.save(pancakes);

            // 9. Guacamole
            Recipe guacamole = createRecipe("Guacamole", "Fresh avocado dip with lime and cilantro.");
            createAndAddIngredient(guacamole, "Avocados", Unit.PIECE, 2);
            createAndAddIngredient(guacamole, "Limes", Unit.PIECE, 1);
            createAndAddIngredient(guacamole, "Onion", Unit.PIECE, 1);
            createAndAddIngredient(guacamole, "Tortilla Chips", Unit.GRAM, 100);
            recipeRepository.save(guacamole);

            // 10. Club Sandwich
            Recipe club = createRecipe("Club Sandwich", "Triple-decker sandwich with turkey, bacon, and lettuce.");
            createAndAddIngredient(club, "Bread Slices", Unit.PIECE, 3);
            createAndAddIngredient(club, "Turkey Breast", Unit.GRAM, 50);
            createAndAddIngredient(club, "Bacon", Unit.GRAM, 30);
            createAndAddIngredient(club, "Mayonnaise", Unit.MILLILITER, 20);
            recipeRepository.save(club);

            log.info("Recipe seeding complete. Total recipes: " + recipeRepository.count());
        } else {
            log.info("Recipes already exist (count: {}). Skipping recipe seeding.", count);
        }
    }

    private Recipe createRecipe(String name, String desc) {
        Recipe r = new Recipe();
        r.setName(name);
        r.setDescription(desc);
        return r;
    }

    private void createAndAddIngredient(Recipe recipe, String name, Unit unit, Integer quantity) {
        Ingredient ing = new Ingredient();
        ing.setIngredientName(name);
        ing.setUnit(unit);
        ing.setQuantity(quantity);
        ing.setRecipe(recipe);
        recipe.getIngredients().add(ing);
    }
}
