package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class PantryGetDTO {
	private Long id;
	private Long groupId;
	private List<PantryItemGetDTO> items;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getGroupId() { return groupId; }
	public void setGroupId(Long groupId) { this.groupId = groupId; }
	public List<PantryItemGetDTO> getItems() { return items; }
	public void setItems(List<PantryItemGetDTO> items) { this.items = items; }
}
