package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class GroupController {

	private final GroupService groupService;
	private final UserService userService;

	@Autowired
	public GroupController(GroupService groupService, UserService userService) {
		this.groupService = groupService;
		this.userService = userService;
	}

	private User resolveUser(Authentication auth) {
		return userService.getUserById(auth.getName());
	}

	@PostMapping("/groups")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public GroupGetDTO createGroup(Authentication auth, @RequestBody GroupPostDTO dto) {
		User caller = resolveUser(auth);
		Group group = groupService.createGroup(caller, dto.getName());
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	@PostMapping("/groups/join")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public GroupGetDTO joinGroup(Authentication auth, @RequestBody GroupJoinPostDTO dto) {
		User caller = resolveUser(auth);
		Group group = groupService.joinGroup(caller, dto.getInviteCode());
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	@GetMapping("/groups/me")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public GroupGetDTO getMyGroup(Authentication auth) {
		Group group = groupService.getGroupOfUser(auth.getName());
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	@PutMapping("/groups/me")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public GroupGetDTO updateGroup(Authentication auth, @RequestBody GroupPostDTO dto) {
		User caller = resolveUser(auth);
		Group group = groupService.updateGroupName(caller, dto.getName());
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	@DeleteMapping("/groups/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteGroup(Authentication auth) {
		User caller = resolveUser(auth);
		groupService.deleteGroup(caller);
	}

	@PostMapping("/groups/me/invite-code")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public GroupGetDTO regenerateInviteCode(Authentication auth) {
		User caller = resolveUser(auth);
		Group group = groupService.regenerateInviteCode(caller);
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	@PutMapping("/groups/me/members/{userID}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateMemberRole(Authentication auth, @PathVariable String userID,
			@RequestBody GroupRolePutDTO dto) {
		User caller = resolveUser(auth);
		groupService.updateMemberRole(caller, userID, dto.getRole());
	}

	@DeleteMapping("/groups/me/members/{userID}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMember(Authentication auth, @PathVariable String userID) {
		User caller = resolveUser(auth);
		groupService.removeMember(caller, userID);
	}

	@DeleteMapping("/groups/me/members/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leaveGroup(Authentication auth) {
		User caller = resolveUser(auth);
		groupService.leaveGroup(caller);
	}
}
