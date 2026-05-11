package ch.uzh.ifi.hase.soprafs26.exceptions;

import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class GlobalExceptionAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    public void handleResponseStatusException_returnsCorrectStatusAndMessage() throws Exception {
        mockMvc.perform(get("/test/response-status-exception"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Test reason"));
    }

    @Test
    public void handleGeneralException_returnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test/general-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred: General error"));
    }

    @Test
    public void handleIllegalArgument_returnsConflict() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isConflict());
    }
}
