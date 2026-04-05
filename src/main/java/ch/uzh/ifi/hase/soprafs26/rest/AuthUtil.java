package ch.uzh.ifi.hase.soprafs26.rest;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Utility class for authenticating users from the Authorization header token.
 */
public class AuthUtil {

	private AuthUtil() {
		// utility class — no instantiation
	}

	/**
	 * Extracts the Bearer token from the Authorization header and resolves the
	 * corresponding User.
	 *
	 * @param authHeader the raw value of the Authorization header (e.g. "Bearer abc-123")
	 * @param userRepository repository to look up the user by token
	 * @return the authenticated User
	 * @throws ResponseStatusException 401 if header is missing/malformed or token unknown
	 */
	public static User authenticateUser(String authHeader, UserRepository userRepository) {
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Missing or malformed Authorization header. Expected: Bearer <token>");
		}

		String token = authHeader.substring("Bearer ".length()).trim();
		if (token.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is empty");
		}

		User user = userRepository.findByToken(token);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
		}

		return user;
	}
}
