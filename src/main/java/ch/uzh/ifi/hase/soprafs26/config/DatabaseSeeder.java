package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.IngredientService;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * DatabaseSeeder initializes the database with default data on startup.
 * It seeds standard test user if they don't already exist.
 */
@Component
@Profile("!prod")
public class DatabaseSeeder implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    private final UserRepository userRepository;
    private final IngredientService ingredientService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DatabaseSeeder(UserRepository userRepository,
                          IngredientService ingredientService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.ingredientService = ingredientService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Starting database seeding process...");
        ingredientService.seedIngredients();
        seedTestUser();
        log.info("Database seeding process completed.");
    }

    private void seedTestUser() {
        String testUsername = "testuser";
        if (!userRepository.existsByUsername(testUsername)) {
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
            log.info("Test user '{}' already exists. Skipping user seeding.", testUsername);
        }
    }
}
