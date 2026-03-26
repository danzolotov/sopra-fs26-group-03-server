package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "email", target = "email")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "passwordHash")
	User convertRegisterPostDTOtoEntity(RegisterPostDTO registerPostDTO);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "passwordHash")
	User convertLoginPostDTOtoEntity(LoginPostDTO loginPostDTO);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "email", target = "email")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "passwordHash")
	@Mapping(source = "bio", target = "bio")
	@Mapping(source = "profilePicture", target = "profilePicture")
	@Mapping(source = "status", target = "status")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "userID", target = "userID")
	@Mapping(source = "email", target = "email")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "bio", target = "bio")
	@Mapping(source = "profilePicture", target = "profilePicture")
	@Mapping(source = "status", target = "status")
	UserGetDTO convertEntityToUserGetDTO(User user);

    @Mapping(source = "userID", target = "userID")
    @Mapping(source = "token", target = "token")
    LoginGetDTO convertEntityToLoginGetDTO(User user);
}
