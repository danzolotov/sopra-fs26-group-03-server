package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DTOMapperTest {
	@Test
	public void testCreateUser_fromUserPostDTO_toUser_success() {
		RegisterPostDTO registerPostDTO = new RegisterPostDTO();
		registerPostDTO.setEmail("name@example.com");
		registerPostDTO.setUsername("username");
		registerPostDTO.setPassword("secret");

		User user = DTOMapper.INSTANCE.convertRegisterPostDTOtoEntity(registerPostDTO);

		assertEquals(registerPostDTO.getEmail(), user.getEmail());
		assertEquals(registerPostDTO.getUsername(), user.getUsername());
		assertEquals(registerPostDTO.getPassword(), user.getPasswordHash());
	}

	@Test
	public void testCreateLoginUser_fromLoginPostDTO_toUser_success() {
		LoginPostDTO loginPostDTO = new LoginPostDTO();
		loginPostDTO.setUsername("username");
		loginPostDTO.setPassword("secret");

		User user = DTOMapper.INSTANCE.convertLoginPostDTOtoEntity(loginPostDTO);

		assertEquals(loginPostDTO.getUsername(), user.getUsername());
		assertEquals(loginPostDTO.getPassword(), user.getPasswordHash());
	}

	@Test
	public void testGetUser_fromUser_toUserGetDTO_success() {
		User user = new User();
		user.setEmail("firstname@lastname.com");
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("1");
		user.setPasswordHash("secret");

		UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

		assertEquals(user.getUserID(), userGetDTO.getUserID());
		assertEquals(user.getEmail(), userGetDTO.getEmail());
		assertEquals(user.getUsername(), userGetDTO.getUsername());
		assertEquals(user.getStatus(), userGetDTO.getStatus());
	}

	@Test
	public void testGetLoginResponse_fromUser_toLoginGetDTO_success() {
		User user = new User();
		user.setUserID("user-1");
		user.setToken("login-token");

		LoginGetDTO loginGetDTO = DTOMapper.INSTANCE.convertEntityToLoginGetDTO(user);

		assertEquals(user.getUserID(), loginGetDTO.getUserID());
		assertEquals(user.getToken(), loginGetDTO.getToken());
	}
}
