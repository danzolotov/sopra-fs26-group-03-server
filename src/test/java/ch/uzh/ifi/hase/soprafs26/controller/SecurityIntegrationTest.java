package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
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
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    @Test
    public void register_withoutAuthorization_isAllowed() throws Exception {
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
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    public void login_withValidCredentials_returnsUserIdAndToken() throws Exception {
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
    public void users_withoutAuthorization_isUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = get("/users").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void users_withBearerHeader_isUnauthorized() throws Exception {
        User persistedUser = new User();
        persistedUser.setEmail("authorized-user@example.com");
        persistedUser.setUsername("authorized-user");
        persistedUser.setPasswordHash("hashed-password");
        persistedUser.setToken("valid-token");
        persistedUser.setStatus(UserStatus.ONLINE);
        userRepository.saveAndFlush(persistedUser);

        MockHttpServletRequestBuilder request = get("/users")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void users_withValidAuthCookie_isAllowed() throws Exception {
        User persistedUser = new User();
        persistedUser.setEmail("authorized-user@example.com");
        persistedUser.setUsername("authorized-user");
        persistedUser.setPasswordHash("hashed-password");
        persistedUser.setToken("valid-token");
        persistedUser.setStatus(UserStatus.ONLINE);
        userRepository.saveAndFlush(persistedUser);

        MockHttpServletRequestBuilder request = get("/users")
                .cookie(new MockCookie("AUTH_TOKEN", "valid-token"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void userById_withOwnAuthCookie_isAllowed() throws Exception {
        User persistedUser = new User();
        persistedUser.setEmail("self-user@example.com");
        persistedUser.setUsername("self-user");
        persistedUser.setPasswordHash("hashed-password");
        persistedUser.setToken("self-token");
        persistedUser.setStatus(UserStatus.ONLINE);
        persistedUser = userRepository.saveAndFlush(persistedUser);

        MockHttpServletRequestBuilder request = get("/users/{userID}", persistedUser.getUserID())
                .cookie(new MockCookie("AUTH_TOKEN", "self-token"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userID").value(persistedUser.getUserID()));
    }

    @Test
    public void userById_withOtherUsersAuthCookie_isForbidden() throws Exception {
        User requestedUser = new User();
        requestedUser.setEmail("requested-user@example.com");
        requestedUser.setUsername("requested-user");
        requestedUser.setPasswordHash("hashed-password");
        requestedUser.setToken("requested-token");
        requestedUser.setStatus(UserStatus.ONLINE);
        requestedUser = userRepository.saveAndFlush(requestedUser);

        User requesterUser = new User();
        requesterUser.setEmail("requester-user@example.com");
        requesterUser.setUsername("requester-user");
        requesterUser.setPasswordHash("hashed-password");
        requesterUser.setToken("requester-token");
        requesterUser.setStatus(UserStatus.ONLINE);
        userRepository.saveAndFlush(requesterUser);

        MockHttpServletRequestBuilder request = get("/users/{userID}", requestedUser.getUserID())
                .cookie(new MockCookie("AUTH_TOKEN", "requester-token"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    public void logout_withValidAuthCookie_clearsCookieAndInvalidatesToken() throws Exception {
        User persistedUser = new User();
        persistedUser.setEmail("logout-user@example.com");
        persistedUser.setUsername("logout-user");
        persistedUser.setPasswordHash("hashed-password");
        persistedUser.setToken("logout-token");
        persistedUser.setStatus(UserStatus.ONLINE);
        userRepository.saveAndFlush(persistedUser);

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
