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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "AUTH_TOKEN";
    private static final long AUTH_COOKIE_MAX_AGE_SECONDS = 7L * 24L * 60L * 60L;

    private final UserService userService;

    AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public UserGetDTO registerUser(@RequestBody RegisterPostDTO registerPostDTO, HttpServletResponse response) {
        User userInput = DTOMapper.INSTANCE.convertRegisterPostDTOtoEntity(registerPostDTO);
        User createdUser = userService.createUser(userInput);
        addAuthCookie(response, createdUser.getToken());
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LoginGetDTO loginUser(@RequestBody LoginPostDTO loginPostDTO, HttpServletResponse response) {
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

    private void addAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie authCookie = ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(AUTH_COOKIE_MAX_AGE_SECONDS)
                .build();
        response.addHeader("Set-Cookie", authCookie.toString());
    }

    private void removeAuthCookie(HttpServletResponse response) {
        ResponseCookie authCookie = ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", authCookie.toString());
    }
}
