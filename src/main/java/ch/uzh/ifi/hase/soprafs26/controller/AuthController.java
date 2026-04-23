package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "AUTH_TOKEN";
    private static final long AUTH_COOKIE_MAX_AGE_SECONDS = 7L * 24L * 60L * 60L;

    private final UserService userService;
    private final boolean authCookieSecure;

    AuthController(UserService userService, @Value("${app.auth.cookie.secure:false}") boolean authCookieSecure) {
        this.userService = userService;
        this.authCookieSecure = authCookieSecure;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public UserGetDTO registerUser(@RequestBody RegisterPostDTO registerPostDTO, HttpServletResponse response) {
        validateRegisterInput(registerPostDTO);
        User userInput = DTOMapper.INSTANCE.convertRegisterPostDTOtoEntity(registerPostDTO);
        User createdUser = userService.createUser(userInput);
        addAuthCookie(response, createdUser.getToken());
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LoginGetDTO loginUser(@RequestBody LoginPostDTO loginPostDTO, HttpServletResponse response) {
        validateLoginInput(loginPostDTO);
        User loginInput = DTOMapper.INSTANCE.convertLoginPostDTOtoEntity(loginPostDTO);
        User loggedInUser = userService.loginUser(loginInput.getUsername(), loginInput.getPasswordHash());
        addAuthCookie(response, loggedInUser.getToken());
        return DTOMapper.INSTANCE.convertEntityToLoginGetDTO(loggedInUser);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public void logoutUser(HttpServletRequest request, HttpServletResponse response) {
        String token = resolveToken(request);
        userService.logoutUser(token);
        removeAuthCookie(response);
    }

    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication token");
    }

    private void validateRegisterInput(RegisterPostDTO registerPostDTO) {
        if (registerPostDTO == null
                || isBlank(registerPostDTO.getEmail())
                || isBlank(registerPostDTO.getUsername())
                || isBlank(registerPostDTO.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email, username and password must be provided");
        }
    }

    private void validateLoginInput(LoginPostDTO loginPostDTO) {
        if (loginPostDTO == null
                || isBlank(loginPostDTO.getUsername())
                || isBlank(loginPostDTO.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password must be provided");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void addAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie authCookie = ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(authCookieSecure)
                .sameSite(authCookieSecure ? "None" : "Lax")
                .path("/")
                .maxAge(AUTH_COOKIE_MAX_AGE_SECONDS)
                .build();
        response.addHeader("Set-Cookie", authCookie.toString());
    }

    private void removeAuthCookie(HttpServletResponse response) {
        ResponseCookie authCookie = ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(authCookieSecure)
                .sameSite(authCookieSecure ? "None" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", authCookie.toString());
    }
}
