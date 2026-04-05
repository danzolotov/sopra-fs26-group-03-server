package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.Instant;
import java.util.List;

public class GroupGetDTO {
	private Long id;
	private String name;
	private String inviteCode;
	private Instant createdAt;
	private List<GroupMemberGetDTO> members;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInviteCode() {
		return inviteCode;
	}

	public void setInviteCode(String inviteCode) {
		this.inviteCode = inviteCode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public List<GroupMemberGetDTO> getMembers() {
		return members;
	}

	public void setMembers(List<GroupMemberGetDTO> members) {
		this.members = members;
	}
}
