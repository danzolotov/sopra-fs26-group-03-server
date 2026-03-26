package ch.uzh.ifi.hase.soprafs26.security.authorization;

import org.springframework.stereotype.Service;

// ! AI - Default implementation of UserRelationshipService

@Service
public class DefaultUserRelationshipService implements UserRelationshipService {

    @Override
    public boolean areHouseholdMembers(String firstUserID, String secondUserID) {
        return false;
    }
}

