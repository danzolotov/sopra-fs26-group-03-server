package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.IngredientRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RecipeRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @BeforeEach
    void setup() {
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createAndPersistUser(String email, String username, String passwordHash, String token) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setToken(token);
        user.setStatus(UserStatus.ONLINE);
        return userRepository.saveAndFlush(user);
    }

    private User createOnlineUser(String email, String username, String token) {
        String encodedPassword = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("hashed-password");
        return createAndPersistUser(email, username, encodedPassword, token);
    }

    @Test
    void register_withoutAuthorization_isAllowed() throws Exception {
        RegisterPostDTO registerPostDTO = new RegisterPostDTO();
        registerPostDTO.setEmail("public-register-user@example.com");
        registerPostDTO.setUsername("public-register-user");
        registerPostDTO.setPassword("secret");

        MockHttpServletRequestBuilder request = post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(registerPostDTO));

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("AUTH_TOKEN=")))
                .andExpect(jsonPath("$.userID").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_withValidCredentials_returnsUserIdAndToken() throws Exception {
        User persistedUser = new User();
        persistedUser.setEmail("login-user@example.com");
        persistedUser.setUsername("login-user");
        persistedUser.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret"));
        persistedUser.setToken("initial-token");
        persistedUser.setStatus(UserStatus.OFFLINE);
        persistedUser = userRepository.saveAndFlush(persistedUser);

        String loginPayload = "{\"username\":\"login-user\",\"password\":\"secret\"}";

        MockHttpServletRequestBuilder request = post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userID").value(persistedUser.getUserID()))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void users_withoutAuthorization_isUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = get("/users").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void users_withBearerHeader_isAllowed() throws Exception {
        createOnlineUser("authorized-user@example.com", "authorized-user", "valid-token");

        MockHttpServletRequestBuilder request = get("/users")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void users_withValidAuthCookie_isAllowed() throws Exception {
        createOnlineUser("authorized-user@example.com", "authorized-user", "valid-token");

        MockHttpServletRequestBuilder request = get("/users")
                .cookie(new MockCookie("AUTH_TOKEN", "valid-token"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void userById_withOwnAuthCookie_isAllowed() throws Exception {
        User persistedUser = createOnlineUser("self-user@example.com", "self-user", "self-token");

        MockHttpServletRequestBuilder request = get("/users/{userID}", persistedUser.getUserID())
                .cookie(new MockCookie("AUTH_TOKEN", "self-token"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userID").value(persistedUser.getUserID()));
    }

    @Test
    void userById_withOtherUsersAuthCookie_isForbidden() throws Exception {
        User requestedUser = createOnlineUser("requested-user@example.com", "requested-user", "requested-token");
        createOnlineUser("requester-user@example.com", "requester-user", "requester-token");

        MockHttpServletRequestBuilder request = get("/users/{userID}", requestedUser.getUserID())
                .cookie(new MockCookie("AUTH_TOKEN", "requester-token"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void logout_withValidAuthCookie_clearsCookieAndInvalidatesToken() throws Exception {
        createOnlineUser("logout-user@example.com", "logout-user", "logout-token");

        MockHttpServletRequestBuilder logoutRequest = post("/auth/logout")
                .cookie(new MockCookie("AUTH_TOKEN", "logout-token"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(logoutRequest)
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("AUTH_TOKEN=")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        MockHttpServletRequestBuilder usersRequest = get("/users")
                .cookie(new MockCookie("AUTH_TOKEN", "logout-token"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(usersRequest).andExpect(status().isUnauthorized());
    }
}
