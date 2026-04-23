package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs26.entity.Recipe;

@Repository("recipeRepository")
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Recipe findByName(String name);
}
