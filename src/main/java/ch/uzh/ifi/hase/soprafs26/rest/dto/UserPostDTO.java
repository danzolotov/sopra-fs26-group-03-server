package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class UserPostDTO extends BaseUserDTO {

	private String password;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
