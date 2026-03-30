package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	public void createUser_validInput_userCreated() throws Exception {
		User user = new User();
		user.setUserID("1");
		user.setEmail("test@example.com");
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);
		user.setPasswordHash("secret");

		RegisterPostDTO registerPostDTO = new RegisterPostDTO();
		registerPostDTO.setEmail("test@example.com");
		registerPostDTO.setUsername("testUsername");
		registerPostDTO.setPassword("secret");

		given(userService.createUser(Mockito.any())).willReturn(user);

		MockHttpServletRequestBuilder postRequest = post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(registerPostDTO));

		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(header().string("Set-Cookie", not(containsString("Secure"))))
				.andExpect(jsonPath("$.userID", is(user.getUserID())))
				.andExpect(jsonPath("$.email", is(user.getEmail())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())));
	}

	@Test
	public void loginUser_validInput_userLoggedIn() throws Exception {
		User user = new User();
		user.setUserID("user-1");
		user.setUsername("testUsername");
		user.setToken("new-token");
		user.setStatus(UserStatus.ONLINE);
		user.setPasswordHash("hashed-secret");

		LoginPostDTO loginPostDTO = new LoginPostDTO();
		loginPostDTO.setUsername("testUsername");
		loginPostDTO.setPassword("secret");

		given(userService.loginUser("testUsername", "secret")).willReturn(user);

		MockHttpServletRequestBuilder postRequest = post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(loginPostDTO));

		mockMvc.perform(postRequest)
				.andExpect(status().isOk())
				.andExpect(header().string("Set-Cookie", not(containsString("Secure"))))
				.andExpect(jsonPath("$.userID", is(user.getUserID())))
				.andExpect(jsonPath("$.token", is(user.getToken())));
	}

	@Test
	public void createUser_blankUsername_returnsBadRequest() throws Exception {
		RegisterPostDTO registerPostDTO = new RegisterPostDTO();
		registerPostDTO.setEmail("test@example.com");
		registerPostDTO.setUsername("   ");
		registerPostDTO.setPassword("secret");

		MockHttpServletRequestBuilder postRequest = post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(registerPostDTO));

		mockMvc.perform(postRequest).andExpect(status().isBadRequest());
		verifyNoInteractions(userService);
	}

	@Test
	public void loginUser_blankPassword_returnsBadRequest() throws Exception {
		LoginPostDTO loginPostDTO = new LoginPostDTO();
		loginPostDTO.setUsername("testUsername");
		loginPostDTO.setPassword(" ");

		MockHttpServletRequestBuilder postRequest = post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(loginPostDTO));

		mockMvc.perform(postRequest).andExpect(status().isBadRequest());
		verifyNoInteractions(userService);
	}

	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e));
		}
	}
}