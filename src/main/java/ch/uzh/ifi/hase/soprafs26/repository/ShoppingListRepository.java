package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs26.entity.ShoppingList;
import java.util.List;

@Repository("shoppingListRepository")
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
	List<ShoppingList> findAllByGroupId(Long groupId);
}
