package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(@Qualifier("userRepository") UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User createUser(User newUser) {
		checkIfUserExists(newUser);
		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.OFFLINE);
		newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	public User loginUser(String username, String password) {
		User user = userRepository.findByUsername(username);
		if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}

		user.setToken(UUID.randomUUID().toString());
		user.setStatus(UserStatus.ONLINE);
		user = userRepository.save(user);
		userRepository.flush();
		return user;
	}

	public User getUserByToken(String token) {
		return userRepository.findByToken(token);
	}

	public void logoutUser(String token) {
		User user = userRepository.findByToken(token);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
		}

		user.setStatus(UserStatus.OFFLINE);
		user.setToken(UUID.randomUUID().toString());
		userRepository.save(user);
		userRepository.flush();
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and email defined in the User entity. The method will do nothing
	 * if the input is unique and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
		User userByEmail = userRepository.findByEmail(userToBeCreated.getEmail());

		if (userByUsername != null && userByEmail != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					String.format("Both username '%s' and email '%s' are already in use.",
							userToBeCreated.getUsername(), userToBeCreated.getEmail()));
		} else if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					String.format("Username '%s' is already in use.", userToBeCreated.getUsername()));
		} else if (userByEmail != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					String.format("Email '%s' is already in use.", userToBeCreated.getEmail()));
		}
	}
}
