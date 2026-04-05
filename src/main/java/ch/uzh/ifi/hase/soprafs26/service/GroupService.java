package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GroupRole;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMembership;
import ch.uzh.ifi.hase.soprafs26.entity.Pantry;
import ch.uzh.ifi.hase.soprafs26.entity.ShoppingList;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GroupMembershipRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PantryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.ShoppingListRepository;
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

	/**
	 * Create a new group. The caller becomes ADMIN.
	 * Auto-creates one ShoppingList for the group.
	 */
	public Group createGroup(User creator, String groupName) {
		// user must not already be in a group
		ensureUserNotInGroup(creator.getId());

		Group group = new Group();
		group.setName(groupName);
		group.setInviteCode(generateUniqueInviteCode());
		group = groupRepository.save(group);
		groupRepository.flush();

		// create admin membership
		GroupMembership membership = new GroupMembership();
		membership.setUser(creator);
		membership.setGroup(group);
		membership.setRole(GroupRole.ADMIN);
		membershipRepository.save(membership);
		membershipRepository.flush();

		// auto-create shopping list
		ShoppingList shoppingList = new ShoppingList();
		shoppingList.setGroupId(group.getId());
		shoppingListRepository.save(shoppingList);
		shoppingListRepository.flush();

		// auto-create pantry
		Pantry pantry = new Pantry();
		pantry.setGroupId(group.getId());
		pantryRepository.save(pantry);
		pantryRepository.flush();

		log.debug("Created group '{}' (id={}) with admin userId={}", groupName, group.getId(), creator.getId());
		return group;
	}

	/**
	 * Join an existing group via invite code. The caller becomes MEMBER.
	 */
	public Group joinGroup(User joiner, String inviteCode) {
		ensureUserNotInGroup(joiner.getId());

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

		log.debug("User {} joined group {} via invite code", joiner.getId(), group.getId());
		return group;
	}

	/**
	 * Get the group the user belongs to, including all memberships.
	 */
	public Group getGroupOfUser(Long userId) {
		GroupMembership membership = membershipRepository.findByUserId(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"You are not a member of any group"));
		return membership.getGroup();
	}

	/**
	 * Update the group name. Only ADMIN.
	 */
	public Group updateGroupName(User admin, String newName) {
		GroupMembership membership = getAdminMembership(admin.getId());
		Group group = membership.getGroup();
		group.setName(newName);
		groupRepository.save(group);
		groupRepository.flush();
		return group;
	}

	/**
	 * Regenerate the invite code. Only ADMIN.
	 */
	public Group regenerateInviteCode(User admin) {
		GroupMembership membership = getAdminMembership(admin.getId());
		Group group = membership.getGroup();
		group.setInviteCode(generateUniqueInviteCode());
		groupRepository.save(group);
		groupRepository.flush();
		log.debug("Regenerated invite code for group {}", group.getId());
		return group;
	}

	/**
	 * Change a member's role. Only ADMIN.
	 * Cannot demote the only ADMIN.
	 */
	public void updateMemberRole(User admin, Long targetUserId, GroupRole newRole) {
		GroupMembership adminMembership = getAdminMembership(admin.getId());
		Long groupId = adminMembership.getGroup().getId();

		GroupMembership targetMembership = membershipRepository.findByUserIdAndGroupId(targetUserId, groupId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Target user is not a member of your group"));

		// if demoting an admin, ensure there's at least one other admin
		if (targetMembership.getRole() == GroupRole.ADMIN && newRole == GroupRole.MEMBER) {
			long adminCount = countAdmins(groupId);
			if (adminCount <= 1) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Cannot demote the only admin. Promote another member first.");
			}
		}

		targetMembership.setRole(newRole);
		membershipRepository.save(targetMembership);
		membershipRepository.flush();
	}

	/**
	 * Remove a member from the group. Only ADMIN.
	 * Cannot remove the sole ADMIN.
	 */
	public void removeMember(User admin, Long targetUserId) {
		GroupMembership adminMembership = getAdminMembership(admin.getId());
		Long groupId = adminMembership.getGroup().getId();

		GroupMembership targetMembership = membershipRepository.findByUserIdAndGroupId(targetUserId, groupId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Target user is not a member of your group"));

		// cannot remove the sole admin
		if (targetMembership.getRole() == GroupRole.ADMIN) {
			long adminCount = countAdmins(groupId);
			if (adminCount <= 1) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Cannot remove the only admin. Delete the group instead, or promote another member first.");
			}
		}

		membershipRepository.delete(targetMembership);
		membershipRepository.flush();
		log.debug("Removed user {} from group {}", targetUserId, groupId);
	}

	/**
	 * Leave the group. If the caller is the sole ADMIN, they must delete the group instead.
	 */
	public void leaveGroup(User user) {
		GroupMembership membership = membershipRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"You are not a member of any group"));

		if (membership.getRole() == GroupRole.ADMIN) {
			long adminCount = countAdmins(membership.getGroup().getId());
			if (adminCount <= 1) {
				// check if there are other members
				long totalMembers = membershipRepository.countByGroupId(membership.getGroup().getId());
				if (totalMembers > 1) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"You are the only admin. Promote another member to admin before leaving, or delete the group.");
				}
				// sole member and sole admin → just delete the group
				deleteGroupInternal(membership.getGroup());
				return;
			}
		}

		membershipRepository.delete(membership);
		membershipRepository.flush();
		log.debug("User {} left group {}", user.getId(), membership.getGroup().getId());
	}

	/**
	 * Delete the group and all associated data. Only ADMIN.
	 */
	public void deleteGroup(User admin) {
		GroupMembership membership = getAdminMembership(admin.getId());
		deleteGroupInternal(membership.getGroup());
	}

	// ─── helpers ───────────────────────────────────────────────

	private void deleteGroupInternal(Group group) {
		// delete associated shopping lists
		List<ShoppingList> lists = shoppingListRepository.findAllByGroupId(group.getId());
		shoppingListRepository.deleteAll(lists);

		// delete associated pantries
		List<Pantry> pantries = pantryRepository.findAllByGroupId(group.getId());
		pantryRepository.deleteAll(pantries);

		// delete group (cascades to memberships)
		groupRepository.delete(group);
		groupRepository.flush();
		log.debug("Deleted group {}", group.getId());
	}

	private void ensureUserNotInGroup(Long userId) {
		if (membershipRepository.findByUserId(userId).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"You are already a member of a group. Leave your current group first.");
		}
	}

	private GroupMembership getAdminMembership(Long userId) {
		GroupMembership membership = membershipRepository.findByUserId(userId)
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
