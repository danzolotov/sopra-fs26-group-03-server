package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs26.entity.ShoppingListItem;

@Repository("shoppingListItemRepository")
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {
}
