package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.security.authorization.AccessScope;
import ch.uzh.ifi.hase.soprafs26.security.authorization.AuthorizationPolicyService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {

	private final UserService userService;
	private final AuthorizationPolicyService authorizationPolicyService;

	UserController(UserService userService, AuthorizationPolicyService authorizationPolicyService) {
		this.userService = userService;
		this.authorizationPolicyService = authorizationPolicyService;
	}

	@GetMapping("/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGetDTO> getAllUsers() {
		List<User> users = userService.getUsers();
		List<UserGetDTO> userGetDTOs = new ArrayList<>();

		for (User user : users) {
			userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
		}
		return userGetDTOs;
	}

	@GetMapping("/users/me")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO getMe(Authentication authentication) {
		if (authentication == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
		}
		User user = userService.getUserById(authentication.getName());
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
	}


    @GetMapping("/users/{userID}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO getUserById(@PathVariable("userID") String userID, Authentication authentication) {
        User requestedUser = userService.getUserById(userID);
        if (requestedUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        String authenticatedUserID = authentication != null ? authentication.getName() : null;
        authorizationPolicyService.assertCanAccessUser(authenticatedUserID, requestedUser.getUserID(), AccessScope.OWN_USER);

        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(requestedUser);
    }

    @PatchMapping("/users/{userID}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO patchUserById(@PathVariable("userID") String userID,
                                    @RequestBody(required = false) UserPostDTO userPostDTO,
                                    Authentication authentication) {
        User requestedUser = userService.getUserById(userID);
        if (requestedUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (!hasAnyUpdateField(userPostDTO)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided for update");
        }

        String authenticatedUserID = authentication != null ? authentication.getName() : null;
        authorizationPolicyService.assertCanAccessUser(authenticatedUserID, requestedUser.getUserID(), AccessScope.OWN_USER);

        User userUpdates = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
        User updatedUser = userService.updateUserById(requestedUser.getUserID(), userUpdates);
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
    }

    private boolean hasAnyUpdateField(UserPostDTO userPostDTO) {
        return userPostDTO != null
                && (userPostDTO.getEmail() != null
                || userPostDTO.getUsername() != null
                || userPostDTO.getPassword() != null
                || userPostDTO.getBio() != null
                || userPostDTO.getProfilePicture() != null
                || userPostDTO.getStatus() != null);
    }
}
