package ch.uzh.ifi.hase.soprafs26.security.authorization;

public interface UserRelationshipService {
    boolean areHouseholdMembers(String firstUserID, String secondUserID);
}

