package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LoginGetDTO {

    private String userID;
    private String token;

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
