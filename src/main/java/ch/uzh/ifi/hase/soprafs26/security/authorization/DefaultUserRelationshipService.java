package ch.uzh.ifi.hase.soprafs26.security.authorization;

import org.springframework.stereotype.Service;

// Default fallback implementation for user-relationship checks.
// Household-based access is not wired yet, so this currently denies that relation.

@Service
public class DefaultUserRelationshipService implements UserRelationshipService {

    @Override
    public boolean areHouseholdMembers(String firstUserID, String secondUserID) {
        return false;
    }
}
