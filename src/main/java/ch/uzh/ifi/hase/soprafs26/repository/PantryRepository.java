package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs26.entity.Pantry;
import java.util.List;

@Repository("pantryRepository")
public interface PantryRepository extends JpaRepository<Pantry, Long> {
	List<Pantry> findAllByGroupId(Long groupId);
}
