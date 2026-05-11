package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.User;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, String> {
	User findByUserID(String userID);
	User findByToken(String token);
	User findByUsername(String username);
	User findByEmail(String email);
	User findByUsernameOrEmail(String username, String email);

	boolean existsByUsername(String username);
	boolean existsByEmail(String email);
}
