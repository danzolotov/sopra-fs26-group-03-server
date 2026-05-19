package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.service.IngredientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class IngredientInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IngredientInitializer.class);

    private final IngredientService ingredientService;

    public IngredientInitializer(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        ingredientService.seedIngredients();
    }
}
