package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs26.entity.Ingredient;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.constant.Unit;

import java.util.Optional;
import java.util.List;

@Repository("ingredientRepository")
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
	List<Ingredient> findByIngredientNameIgnoreCase(String ingredientName);
	Optional<Ingredient> findByIngredientNameIgnoreCaseAndUser(String ingredientName, User user);
	Optional<Ingredient> findByIngredientNameIgnoreCaseAndUnitAndUser(String ingredientName, Unit unit, User user);
	Optional<Ingredient> findFirstByIngredientNameIgnoreCaseAndUnitAndUserIsNull(String ingredientName, Unit unit);
	List<Ingredient> findAllByUser(User user);
	List<Ingredient> findAllByUserIsNull();
	long countByUserIsNull();
}
