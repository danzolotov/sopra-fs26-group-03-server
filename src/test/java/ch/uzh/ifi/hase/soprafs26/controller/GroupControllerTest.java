package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupJoinPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
public class GroupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GroupService groupService;

	@MockitoBean
	private UserService userService;

	private User testUser;
	private Group testGroup;

	@BeforeEach
	public void setup() {
		testUser = new User();
		testUser.setUserID("user-1");
		testUser.setUsername("testuser");

		testGroup = new Group();
		testGroup.setId(1L);
		testGroup.setName("Test Group");
		testGroup.setInviteCode("ABC1EFG2");
	}

	@Test
	public void createGroup_success() throws Exception {
		given(userService.getUserById("user-1")).willReturn(testUser);
		given(groupService.createGroup(any(User.class), eq("Test Group"))).willReturn(testGroup);

		mockMvc.perform(post("/groups")
						.principal(new UsernamePasswordAuthenticationToken("user-1", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Test Group\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.name", is("Test Group")))
				.andExpect(jsonPath("$.inviteCode", is("ABC1EFG2")));
	}

	@Test
	public void joinGroup_success() throws Exception {
		given(userService.getUserById("user-1")).willReturn(testUser);
		given(groupService.joinGroup(any(User.class), eq("ABC1EFG2"))).willReturn(testGroup);

		mockMvc.perform(post("/groups/join")
						.principal(new UsernamePasswordAuthenticationToken("user-1", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"inviteCode\":\"ABC1EFG2\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.inviteCode", is("ABC1EFG2")));
	}

	@Test
	public void getMyGroup_success() throws Exception {
		given(groupService.getGroupOfUser("user-1")).willReturn(testGroup);

		mockMvc.perform(get("/groups/me")
						.principal(new UsernamePasswordAuthenticationToken("user-1", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.name", is("Test Group")));
	}
}
