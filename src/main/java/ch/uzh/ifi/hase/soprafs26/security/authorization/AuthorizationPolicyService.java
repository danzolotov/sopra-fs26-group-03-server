package ch.uzh.ifi.hase.soprafs26.security.authorization;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorizationPolicyService {

    private final UserRelationshipService userRelationshipService;

    public AuthorizationPolicyService(UserRelationshipService userRelationshipService) {
        this.userRelationshipService = userRelationshipService;
    }

    public void assertCanAccessUser(String authenticatedUserID, String targetUserID, AccessScope accessScope) {
        if (accessScope == AccessScope.EVERYONE) {
            return;
        }

        if (authenticatedUserID == null || authenticatedUserID.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (authenticatedUserID.equals(targetUserID)) {
            return;
        }

        if (accessScope == AccessScope.HOUSEHOLD_MEMBERS
                && userRelationshipService.areHouseholdMembers(authenticatedUserID, targetUserID)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this resource");
    }
}
