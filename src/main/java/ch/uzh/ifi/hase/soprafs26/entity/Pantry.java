package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pantries")
public class Pantry implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private Long groupId;

	@OneToMany(mappedBy = "pantry", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PantryItem> items = new ArrayList<>();

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getGroupId() { return groupId; }
	public void setGroupId(Long groupId) { this.groupId = groupId; }
	public List<PantryItem> getItems() { return items; }
	public void setItems(List<PantryItem> items) { this.items = items; }
}
