package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GroupRole;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GroupServiceTest {

	@Mock
	private GroupRepository groupRepository;
	@Mock
	private GroupMembershipRepository membershipRepository;
	@Mock
	private ShoppingListRepository shoppingListRepository;

	@InjectMocks
	private GroupService groupService;

	private User testUser;
	private User testUser2;
	private Group testGroup;
	private GroupMembership adminMembership;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		testUser = new User();
		testUser.setUserID("user-1");
		testUser.setUsername("admin");
		testUser.setToken("admin-token");

		testUser2 = new User();
		testUser2.setUserID("user-2");
		testUser2.setUsername("member");
		testUser2.setToken("member-token");

		testGroup = new Group();
		testGroup.setId(1L);
		testGroup.setName("Test Group");
		testGroup.setInviteCode("ABC12345");
		testGroup.setMemberships(new ArrayList<>());

		adminMembership = new GroupMembership();
		adminMembership.setId(1L);
		adminMembership.setUser(testUser);
		adminMembership.setGroup(testGroup);
		adminMembership.setRole(GroupRole.ADMIN);
	}

	@Test
	public void createGroup_validInput_success() {
		when(membershipRepository.findByUserUserID(testUser.getUserID())).thenReturn(Optional.empty());
		when(groupRepository.save(any(Group.class))).thenAnswer(i -> { Group g = i.getArgument(0); g.setId(1L); return g; });
		when(membershipRepository.save(any(GroupMembership.class))).thenAnswer(i -> i.getArgument(0));
		when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(i -> i.getArgument(0));
		when(groupRepository.findByInviteCode(any())).thenReturn(Optional.empty());

		Group created = groupService.createGroup(testUser, "My Group");
		assertNotNull(created);
		assertEquals("My Group", created.getName());
		verify(shoppingListRepository).save(any(ShoppingList.class));
	}

	@Test
	public void createGroup_alreadyInGroup_throwsConflict() {
		when(membershipRepository.findByUserUserID(testUser.getUserID())).thenReturn(Optional.of(adminMembership));
		assertThrows(ResponseStatusException.class, () -> groupService.createGroup(testUser, "X"));
	}

	@Test
	public void joinGroup_validCode_success() {
		when(membershipRepository.findByUserUserID(testUser2.getUserID())).thenReturn(Optional.empty());
		when(groupRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(testGroup));
		when(membershipRepository.countByGroupId(1L)).thenReturn(1L);
		when(membershipRepository.save(any())).thenAnswer(i -> i.getArgument(0));

		Group joined = groupService.joinGroup(testUser2, "ABC12345");
		assertEquals(1L, joined.getId());
	}

	@Test
	public void joinGroup_invalidCode_throwsNotFound() {
		when(membershipRepository.findByUserUserID(testUser2.getUserID())).thenReturn(Optional.empty());
		when(groupRepository.findByInviteCode("BAD")).thenReturn(Optional.empty());
		assertThrows(ResponseStatusException.class, () -> groupService.joinGroup(testUser2, "BAD"));
	}

	@Test
	public void joinGroup_groupFull_throwsConflict() {
		when(membershipRepository.findByUserUserID(testUser2.getUserID())).thenReturn(Optional.empty());
		when(groupRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(testGroup));
		when(membershipRepository.countByGroupId(1L)).thenReturn(100L);
		assertThrows(ResponseStatusException.class, () -> groupService.joinGroup(testUser2, "ABC12345"));
	}

	@Test
	public void getGroupOfUser_isMember_returnsGroup() {
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		assertEquals(1L, groupService.getGroupOfUser("user-1").getId());
	}

	@Test
	public void getGroupOfUser_notInGroup_throws() {
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.empty());
		assertThrows(ResponseStatusException.class, () -> groupService.getGroupOfUser("user-1"));
	}

	@Test
	public void updateGroupName_asAdmin_success() {
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		when(groupRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		assertEquals("New", groupService.updateGroupName(testUser, "New").getName());
	}

	@Test
	public void updateGroupName_asMember_throwsForbidden() {
		GroupMembership m = new GroupMembership();
		m.setUser(testUser2); m.setGroup(testGroup); m.setRole(GroupRole.MEMBER);
		when(membershipRepository.findByUserUserID("user-2")).thenReturn(Optional.of(m));
		assertThrows(ResponseStatusException.class, () -> groupService.updateGroupName(testUser2, "New"));
	}

	@Test
	public void updateMemberRole_promoteToAdmin_success() {
		GroupMembership m = new GroupMembership();
		m.setUser(testUser2); m.setGroup(testGroup); m.setRole(GroupRole.MEMBER);
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		when(membershipRepository.findByUserUserIDAndGroupId("user-2", 1L)).thenReturn(Optional.of(m));
		when(membershipRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		groupService.updateMemberRole(testUser, "user-2", GroupRole.ADMIN);
		assertEquals(GroupRole.ADMIN, m.getRole());
	}

	@Test
	public void updateMemberRole_demoteOnlyAdmin_throws() {
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		when(membershipRepository.findByUserUserIDAndGroupId("user-1", 1L)).thenReturn(Optional.of(adminMembership));
		when(membershipRepository.findByGroupId(1L)).thenReturn(Collections.singletonList(adminMembership));
		assertThrows(ResponseStatusException.class, () -> groupService.updateMemberRole(testUser, "user-1", GroupRole.MEMBER));
	}

	@Test
	public void removeMember_success() {
		GroupMembership m = new GroupMembership();
		m.setUser(testUser2); m.setGroup(testGroup); m.setRole(GroupRole.MEMBER);
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		when(membershipRepository.findByUserUserIDAndGroupId("user-2", 1L)).thenReturn(Optional.of(m));
		groupService.removeMember(testUser, "user-2");
		verify(membershipRepository).delete(m);
	}

	@Test
	public void removeMember_soleAdmin_throws() {
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		when(membershipRepository.findByUserUserIDAndGroupId("user-1", 1L)).thenReturn(Optional.of(adminMembership));
		when(membershipRepository.findByGroupId(1L)).thenReturn(Collections.singletonList(adminMembership));
		assertThrows(ResponseStatusException.class, () -> groupService.removeMember(testUser, "user-1"));
	}

	@Test
	public void leaveGroup_asMember_success() {
		GroupMembership m = new GroupMembership();
		m.setUser(testUser2); m.setGroup(testGroup); m.setRole(GroupRole.MEMBER);
		when(membershipRepository.findByUserUserID("user-2")).thenReturn(Optional.of(m));
		groupService.leaveGroup(testUser2);
		verify(membershipRepository).delete(m);
	}

	@Test
	public void leaveGroup_soleAdminSoleMember_deletesGroup() {
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		when(membershipRepository.findByGroupId(1L)).thenReturn(Collections.singletonList(adminMembership));
		when(membershipRepository.countByGroupId(1L)).thenReturn(1L);
		when(shoppingListRepository.findAllByGroupId(1L)).thenReturn(Collections.emptyList());
		groupService.leaveGroup(testUser);
		verify(groupRepository).delete(testGroup);
	}

	@Test
	public void leaveGroup_soleAdminWithMembers_throws() {
		GroupMembership m = new GroupMembership();
		m.setUser(testUser2); m.setGroup(testGroup); m.setRole(GroupRole.MEMBER);
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		when(membershipRepository.findByGroupId(1L)).thenReturn(List.of(adminMembership, m));
		when(membershipRepository.countByGroupId(1L)).thenReturn(2L);
		assertThrows(ResponseStatusException.class, () -> groupService.leaveGroup(testUser));
	}

	@Test
	public void deleteGroup_asAdmin_success() {
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		when(shoppingListRepository.findAllByGroupId(1L)).thenReturn(Collections.emptyList());
		groupService.deleteGroup(testUser);
		verify(groupRepository).delete(testGroup);
	}

	@Test
	public void deleteGroup_asMember_throws() {
		GroupMembership m = new GroupMembership();
		m.setUser(testUser2); m.setGroup(testGroup); m.setRole(GroupRole.MEMBER);
		when(membershipRepository.findByUserUserID("user-2")).thenReturn(Optional.of(m));
		assertThrows(ResponseStatusException.class, () -> groupService.deleteGroup(testUser2));
	}

	@Test
	public void regenerateInviteCode_asAdmin_success() {
		when(membershipRepository.findByUserUserID("user-1")).thenReturn(Optional.of(adminMembership));
		when(groupRepository.findByInviteCode(any())).thenReturn(Optional.empty());
		when(groupRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		Group result = groupService.regenerateInviteCode(testUser);
		assertNotNull(result.getInviteCode());
	}
}
