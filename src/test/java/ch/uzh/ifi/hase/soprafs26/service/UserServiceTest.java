package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		testUser = new User();
		testUser.setUserID("1");
		testUser.setEmail("test@example.com");
		testUser.setUsername("testUsername");
		testUser.setPasswordHash("secret");

		Mockito.when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
		Mockito.when(userRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	public void createUser_validInputs_success() {
		User createdUser = userService.createUser(testUser);

		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
		assertEquals(testUser.getEmail(), createdUser.getEmail());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertEquals("hashed-secret", createdUser.getPasswordHash());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
	}

	@Test
	public void createUser_duplicateEmail_throwsException() {
		userService.createUser(testUser);
		Mockito.when(userRepository.findByEmail(Mockito.any())).thenReturn(testUser);
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void createUser_duplicateInputs_throwsException() {
		userService.createUser(testUser);
		Mockito.when(userRepository.findByEmail(Mockito.any())).thenReturn(testUser);
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void loginUser_validCredentials_success() {
		User persistedUser = new User();
		persistedUser.setEmail("test@example.com");
		persistedUser.setUsername("testUsername");
		persistedUser.setPasswordHash("hashed-secret");
		persistedUser.setStatus(UserStatus.OFFLINE);
		persistedUser.setToken("old-token");

		Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(persistedUser);
		Mockito.when(passwordEncoder.matches("secret", "hashed-secret")).thenReturn(true);
		Mockito.when(userRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

		User loggedInUser = userService.loginUser("testUsername", "secret");

		assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
		assertNotNull(loggedInUser.getToken());
		assertNotEquals("old-token", loggedInUser.getToken());
	}

	@Test
	public void loginUser_invalidPassword_throwsUnauthorized() {
		User persistedUser = new User();
		persistedUser.setEmail("test@example.com");
		persistedUser.setUsername("testUsername");
		persistedUser.setPasswordHash("hashed-secret");

		Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(persistedUser);
		Mockito.when(passwordEncoder.matches("wrong", "hashed-secret")).thenReturn(false);

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> userService.loginUser("testUsername", "wrong"));
		assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
	}

	@Test
	public void updateUserById_validInput_updatesAllProvidedFields() {
		User persistedUser = new User();
		persistedUser.setUserID("1");
		persistedUser.setEmail("old@example.com");
		persistedUser.setUsername("old-username");
		persistedUser.setPasswordHash("old-hash");
		persistedUser.setBio("old bio");
		persistedUser.setStatus(UserStatus.OFFLINE);

		User userUpdates = new User();
		userUpdates.setEmail("new@example.com");
		userUpdates.setUsername("new-username");
		userUpdates.setPasswordHash("new-password");
		userUpdates.setBio("new bio");
		userUpdates.setStatus(UserStatus.ONLINE);

		Mockito.when(userRepository.findByUserID("1")).thenReturn(persistedUser);
		Mockito.when(userRepository.findByEmail("new@example.com")).thenReturn(null);
		Mockito.when(userRepository.findByUsername("new-username")).thenReturn(null);
		Mockito.when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
		Mockito.when(userRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

		User updatedUser = userService.updateUserById("1", userUpdates);

		assertEquals("new@example.com", updatedUser.getEmail());
		assertEquals("new-username", updatedUser.getUsername());
		assertEquals("new-hash", updatedUser.getPasswordHash());
		assertEquals("new bio", updatedUser.getBio());
		assertEquals(UserStatus.ONLINE, updatedUser.getStatus());
	}

	@Test
	public void updateUserById_duplicateEmail_throwsConflict() {
		User persistedUser = new User();
		persistedUser.setUserID("1");
		persistedUser.setEmail("old@example.com");

		User userUpdates = new User();
		userUpdates.setEmail("new@example.com");

		User emailOwner = new User();
		emailOwner.setUserID("2");
		emailOwner.setEmail("new@example.com");

		Mockito.when(userRepository.findByUserID("1")).thenReturn(persistedUser);
		Mockito.when(userRepository.findByEmail("new@example.com")).thenReturn(emailOwner);

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> userService.updateUserById("1", userUpdates));
		assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
	}
}
