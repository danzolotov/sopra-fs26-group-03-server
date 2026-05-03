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

@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(@Qualifier("userRepository") UserRepository userRepository,
			PasswordEncoder passwordEncoder) {
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
		newUser.setPasswordHash(passwordEncoder.encode(newUser.getPasswordHash()));
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	public User loginUser(String username, String password) {
		User user = userRepository.findByUsername(username);
		if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
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

    public User getUserById(String userID) {
        return userRepository.findByUserID(userID);
    }

	public User updateUserById(String userID, User userUpdates) {
		User user = userRepository.findByUserID(userID);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}

		if (userUpdates.getEmail() != null) {
			String newEmail = userUpdates.getEmail();
			if (newEmail.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be blank");
			}
			if (!newEmail.equals(user.getEmail())) {
				User existingUser = userRepository.findByEmail(newEmail);
				if (existingUser != null && !existingUser.getUserID().equals(userID)) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							String.format("Email '%s' is already in use.", newEmail));
				}
				user.setEmail(newEmail);
			}
		}

		if (userUpdates.getUsername() != null) {
			String newUsername = userUpdates.getUsername();
			if (newUsername.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must not be blank");
			}
			if (!newUsername.equals(user.getUsername())) {
				User existingUser = userRepository.findByUsername(newUsername);
				if (existingUser != null && !existingUser.getUserID().equals(userID)) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							String.format("Username '%s' is already in use.", newUsername));
				}
				user.setUsername(newUsername);
			}
		}

		if (userUpdates.getPasswordHash() != null) {
			String newPassword = userUpdates.getPasswordHash();
			if (newPassword.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not be blank");
			}
			user.setPasswordHash(passwordEncoder.encode(newPassword));
		}

		if (userUpdates.getBio() != null) {
			user.setBio(userUpdates.getBio());
		}
		if (userUpdates.getProfilePicture() != null) {
			user.setProfilePicture(userUpdates.getProfilePicture());
		}
		if (userUpdates.getStatus() != null) {
			user.setStatus(userUpdates.getStatus());
		}

		user = userRepository.save(user);
		userRepository.flush();
		return user;
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