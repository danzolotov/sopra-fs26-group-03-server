package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GroupRole;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;

@Service
@Transactional
public class GroupService {

	private final Logger log = LoggerFactory.getLogger(GroupService.class);

	private static final int MAX_MEMBERS = 100;
	private static final int INVITE_CODE_LENGTH = 8;
	private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final GroupRepository groupRepository;
	private final GroupMembershipRepository membershipRepository;
	private final ShoppingListRepository shoppingListRepository;
	private final PantryRepository pantryRepository;

	@Autowired
	public GroupService(GroupRepository groupRepository,
			GroupMembershipRepository membershipRepository,
			ShoppingListRepository shoppingListRepository,
			PantryRepository pantryRepository) {
		this.groupRepository = groupRepository;
		this.membershipRepository = membershipRepository;
		this.shoppingListRepository = shoppingListRepository;
		this.pantryRepository = pantryRepository;
	}

	public Group createGroup(User creator, String groupName) {
		ensureUserNotInGroup(creator.getUserID());

		Group group = new Group();
		group.setName(groupName);
		group.setInviteCode(generateUniqueInviteCode());
		group = groupRepository.save(group);
		groupRepository.flush();

		GroupMembership membership = new GroupMembership();
		membership.setUser(creator);
		membership.setGroup(group);
		membership.setRole(GroupRole.ADMIN);
		membershipRepository.save(membership);
		membershipRepository.flush();

		ShoppingList shoppingList = new ShoppingList();
		shoppingList.setGroupId(group.getId());
		shoppingListRepository.save(shoppingList);
		shoppingListRepository.flush();

		Pantry pantry = new Pantry();
		pantry.setGroupId(group.getId());
		pantryRepository.save(pantry);
		pantryRepository.flush();

		log.debug("Created group '{}' (id={}) with admin userID={}", groupName, group.getId(), creator.getUserID());
		return group;
	}

	public Group joinGroup(User joiner, String inviteCode) {
		ensureUserNotInGroup(joiner.getUserID());

		Group group = groupRepository.findByInviteCode(inviteCode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No group found with that invite code"));

		long memberCount = membershipRepository.countByGroupId(group.getId());
		if (memberCount >= MAX_MEMBERS) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Group has reached the maximum of " + MAX_MEMBERS + " members");
		}

		GroupMembership membership = new GroupMembership();
		membership.setUser(joiner);
		membership.setGroup(group);
		membership.setRole(GroupRole.MEMBER);
		membershipRepository.save(membership);
		membershipRepository.flush();

		log.debug("User {} joined group {} via invite code", joiner.getUserID(), group.getId());
		return group;
	}

	public Group getGroupOfUser(String userID) {
		GroupMembership membership = membershipRepository.findByUserUserID(userID)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"You are not a member of any group"));
		return membership.getGroup();
	}

	public Group updateGroupName(User admin, String newName) {
		GroupMembership membership = getAdminMembership(admin.getUserID());
		Group group = membership.getGroup();
		group.setName(newName);
		groupRepository.save(group);
		groupRepository.flush();
		return group;
	}

	public Group regenerateInviteCode(User admin) {
		GroupMembership membership = getAdminMembership(admin.getUserID());
		Group group = membership.getGroup();
		group.setInviteCode(generateUniqueInviteCode());
		groupRepository.save(group);
		groupRepository.flush();
		return group;
	}

	public void updateMemberRole(User admin, String targetUserID, GroupRole newRole) {
		GroupMembership adminMembership = getAdminMembership(admin.getUserID());
		Long groupId = adminMembership.getGroup().getId();

		GroupMembership targetMembership = membershipRepository.findByUserUserIDAndGroupId(targetUserID, groupId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Target user is not a member of your group"));

		if (targetMembership.getRole() == GroupRole.ADMIN && newRole == GroupRole.MEMBER) {
			if (countAdmins(groupId) <= 1) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Cannot demote the only admin. Promote another member first.");
			}
		}

		targetMembership.setRole(newRole);
		membershipRepository.save(targetMembership);
		membershipRepository.flush();
	}

	public void removeMember(User admin, String targetUserID) {
		GroupMembership adminMembership = getAdminMembership(admin.getUserID());
		Long groupId = adminMembership.getGroup().getId();

		GroupMembership targetMembership = membershipRepository.findByUserUserIDAndGroupId(targetUserID, groupId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Target user is not a member of your group"));

		if (targetMembership.getRole() == GroupRole.ADMIN) {
			if (countAdmins(groupId) <= 1) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Cannot remove the only admin. Delete the group instead, or promote another member first.");
			}
		}

		membershipRepository.delete(targetMembership);
		membershipRepository.flush();
	}

	public void leaveGroup(User user) {
		GroupMembership membership = membershipRepository.findByUserUserID(user.getUserID())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"You are not a member of any group"));

		if (membership.getRole() == GroupRole.ADMIN) {
			long adminCount = countAdmins(membership.getGroup().getId());
			if (adminCount <= 1) {
				long totalMembers = membershipRepository.countByGroupId(membership.getGroup().getId());
				if (totalMembers > 1) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"You are the only admin. Promote another member to admin before leaving, or delete the group.");
				}
				deleteGroupInternal(membership.getGroup());
				return;
			}
		}

		membershipRepository.delete(membership);
		membershipRepository.flush();
	}

	public void deleteGroup(User admin) {
		GroupMembership membership = getAdminMembership(admin.getUserID());
		deleteGroupInternal(membership.getGroup());
	}

	// ─── helpers ───────────────────────────────────────────────

	private void deleteGroupInternal(Group group) {
		shoppingListRepository.deleteAll(shoppingListRepository.findAllByGroupId(group.getId()));
		pantryRepository.deleteAll(pantryRepository.findAllByGroupId(group.getId()));
		groupRepository.delete(group);
		groupRepository.flush();
		log.debug("Deleted group {}", group.getId());
	}


	private void ensureUserNotInGroup(String userID) {
		if (membershipRepository.findByUserUserID(userID).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"You are already a member of a group. Leave your current group first.");
		}
	}

	private GroupMembership getAdminMembership(String userID) {
		GroupMembership membership = membershipRepository.findByUserUserID(userID)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"You are not a member of any group"));
		if (membership.getRole() != GroupRole.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Only group admins can perform this action");
		}
		return membership;
	}

	private long countAdmins(Long groupId) {
		return membershipRepository.findByGroupId(groupId).stream()
				.filter(m -> m.getRole() == GroupRole.ADMIN)
				.count();
	}

	private String generateUniqueInviteCode() {
		String code;
		do {
			code = generateRandomCode();
		} while (groupRepository.findByInviteCode(code).isPresent());
		return code;
	}

	private String generateRandomCode() {
		StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
		for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
			sb.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
		}
		return sb.toString();
	}
}
