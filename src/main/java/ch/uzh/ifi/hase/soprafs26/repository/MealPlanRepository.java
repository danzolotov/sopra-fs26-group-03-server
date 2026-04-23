package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs26.entity.MealPlan;
import java.time.LocalDate;
import java.util.List;

@Repository("mealPlanRepository")
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    List<MealPlan> findByUserIDAndDateBetween(String userID, LocalDate startDate, LocalDate endDate);
    List<MealPlan> findByGroupIdAndDateBetween(Long groupId, LocalDate startDate, LocalDate endDate);
    List<MealPlan> findByGroupIdAndUserIDAndDateBetween(Long groupId, String userID, LocalDate startDate, LocalDate endDate);
}
