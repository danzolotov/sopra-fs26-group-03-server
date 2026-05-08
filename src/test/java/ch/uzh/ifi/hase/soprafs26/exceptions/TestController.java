package ch.uzh.ifi.hase.soprafs26.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TestController {
    @GetMapping("/test/response-status-exception")
    public void throwResponseStatusException() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test reason");
    }

    @GetMapping("/test/general-exception")
    public void throwGeneralException() throws Exception {
        throw new Exception("General error");
    }

    @GetMapping("/test/illegal-argument")
    public void throwIllegalArgument() {
        throw new IllegalArgumentException("Illegal argument");
    }
}
