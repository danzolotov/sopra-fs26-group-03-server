package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.security.authorization.AccessScope;
import ch.uzh.ifi.hase.soprafs26.security.authorization.AuthorizationPolicyService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private AuthorizationPolicyService authorizationPolicyService;

	@Test
	public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
		User user = new User();
		user.setEmail("firstname@lastname.com");
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);
		user.setPasswordHash("secret");

		List<User> allUsers = Collections.singletonList(user);
		given(userService.getUsers()).willReturn(allUsers);

		MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

		mockMvc.perform(getRequest).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].email", is(user.getEmail())))
				.andExpect(jsonPath("$[0].username", is(user.getUsername())))
				.andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
	}

	@Test
	public void patchUserById_validInput_updatesUser() throws Exception {
		User existingUser = new User();
		existingUser.setUserID("user-1");
		existingUser.setEmail("old@example.com");
		existingUser.setUsername("firstname@lastname");
		existingUser.setStatus(UserStatus.OFFLINE);

		User updatedUser = new User();
		updatedUser.setUserID("user-1");
		updatedUser.setEmail("new@example.com");
		updatedUser.setUsername("new-username");
		updatedUser.setBio("new bio");
		updatedUser.setStatus(UserStatus.ONLINE);

		given(userService.getUserById("user-1")).willReturn(existingUser);
		doNothing().when(authorizationPolicyService)
				.assertCanAccessUser(any(), any(), org.mockito.Mockito.eq(AccessScope.OWN_USER));
		given(userService.updateUserById(org.mockito.Mockito.eq("user-1"), argThat(user ->
				"new@example.com".equals(user.getEmail())
						&& "new-username".equals(user.getUsername())
						&& "new-password".equals(user.getPasswordHash())
						&& "new bio".equals(user.getBio())
						&& UserStatus.ONLINE == user.getStatus()))).willReturn(updatedUser);

		MockHttpServletRequestBuilder postRequest = patch("/users/{userID}", "user-1")
				.principal(new UsernamePasswordAuthenticationToken("user-1", null))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"new@example.com\",\"username\":\"new-username\",\"password\":\"new-password\",\"bio\":\"new bio\",\"status\":\"ONLINE\"}");

		mockMvc.perform(postRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userID", is("user-1")))
				.andExpect(jsonPath("$.email", is("new@example.com")))
				.andExpect(jsonPath("$.username", is("new-username")))
				.andExpect(jsonPath("$.bio", is("new bio")))
				.andExpect(jsonPath("$.status", is("ONLINE")));

		verify(userService).updateUserById(org.mockito.Mockito.eq("user-1"), argThat(user ->
				"new@example.com".equals(user.getEmail())
						&& "new-username".equals(user.getUsername())
						&& "new-password".equals(user.getPasswordHash())
						&& "new bio".equals(user.getBio())
						&& UserStatus.ONLINE == user.getStatus()));
	}

	@Test
	public void patchUserById_emptyObject_returnsBadRequest() throws Exception {
		User existingUser = new User();
		existingUser.setUserID("user-1");
		existingUser.setEmail("old@example.com");
		existingUser.setUsername("old-username");
		existingUser.setStatus(UserStatus.OFFLINE);

		given(userService.getUserById("user-1")).willReturn(existingUser);

		MockHttpServletRequestBuilder request = patch("/users/{userID}", "user-1")
				.principal(new UsernamePasswordAuthenticationToken("user-1", null))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}");

		mockMvc.perform(request).andExpect(status().isBadRequest());

		verify(authorizationPolicyService, never())
				.assertCanAccessUser(any(), any(), org.mockito.Mockito.eq(AccessScope.OWN_USER));
		verify(userService, never()).updateUserById(any(), any());
	}
}
