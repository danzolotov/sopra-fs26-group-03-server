package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.AuthUtil;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class GroupController {

	private final GroupService groupService;
	private final UserRepository userRepository;

	@Autowired
	public GroupController(GroupService groupService, UserRepository userRepository) {
		this.groupService = groupService;
		this.userRepository = userRepository;
	}

	/**
	 * POST /groups — Create a new group. Caller becomes ADMIN.
	 */
	@PostMapping("/groups")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public GroupGetDTO createGroup(@RequestHeader("Authorization") String authHeader,
			@RequestBody GroupPostDTO groupPostDTO) {
		User caller = AuthUtil.authenticateUser(authHeader, userRepository);
		Group group = groupService.createGroup(caller, groupPostDTO.getName());
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	/**
	 * POST /groups/join — Join an existing group via invite code.
	 */
	@PostMapping("/groups/join")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public GroupGetDTO joinGroup(@RequestHeader("Authorization") String authHeader,
			@RequestBody GroupJoinPostDTO joinDTO) {
		User caller = AuthUtil.authenticateUser(authHeader, userRepository);
		Group group = groupService.joinGroup(caller, joinDTO.getInviteCode());
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	/**
	 * GET /groups/my — Get the caller's group details.
	 */
	@GetMapping("/groups/my")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public GroupGetDTO getMyGroup(@RequestHeader("Authorization") String authHeader) {
		User caller = AuthUtil.authenticateUser(authHeader, userRepository);
		Group group = groupService.getGroupOfUser(caller.getId());
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	/**
	 * PUT /groups/my — Update group name. ADMIN only.
	 */
	@PutMapping("/groups/my")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public GroupGetDTO updateGroup(@RequestHeader("Authorization") String authHeader,
			@RequestBody GroupPostDTO groupPostDTO) {
		User caller = AuthUtil.authenticateUser(authHeader, userRepository);
		Group group = groupService.updateGroupName(caller, groupPostDTO.getName());
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	/**
	 * DELETE /groups/my — Delete the group. ADMIN only.
	 */
	@DeleteMapping("/groups/my")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteGroup(@RequestHeader("Authorization") String authHeader) {
		User caller = AuthUtil.authenticateUser(authHeader, userRepository);
		groupService.deleteGroup(caller);
	}

	/**
	 * POST /groups/my/invite-code — Regenerate the invite code. ADMIN only.
	 */
	@PostMapping("/groups/my/invite-code")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public GroupGetDTO regenerateInviteCode(@RequestHeader("Authorization") String authHeader) {
		User caller = AuthUtil.authenticateUser(authHeader, userRepository);
		Group group = groupService.regenerateInviteCode(caller);
		return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
	}

	/**
	 * PUT /groups/my/members/{userId} — Change a member's role. ADMIN only.
	 */
	@PutMapping("/groups/my/members/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateMemberRole(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long userId,
			@RequestBody GroupRolePutDTO rolePutDTO) {
		User caller = AuthUtil.authenticateUser(authHeader, userRepository);
		groupService.updateMemberRole(caller, userId, rolePutDTO.getRole());
	}

	/**
	 * DELETE /groups/my/members/{userId} — Remove a member. ADMIN only.
	 */
	@DeleteMapping("/groups/my/members/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMember(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long userId) {
		User caller = AuthUtil.authenticateUser(authHeader, userRepository);
		groupService.removeMember(caller, userId);
	}

	/**
	 * DELETE /groups/my/members/me — Leave the group.
	 */
	@DeleteMapping("/groups/my/members/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leaveGroup(@RequestHeader("Authorization") String authHeader) {
		User caller = AuthUtil.authenticateUser(authHeader, userRepository);
		groupService.leaveGroup(caller);
	}
}
