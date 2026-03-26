package ch.uzh.ifi.hase.soprafs26.security.authorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuthorizationPolicyServiceTest {

    private UserRelationshipService userRelationshipService;
    private AuthorizationPolicyService authorizationPolicyService;

    @BeforeEach
    public void setup() {
        userRelationshipService = Mockito.mock(UserRelationshipService.class);
        authorizationPolicyService = new AuthorizationPolicyService(userRelationshipService);
    }

    @Test
    public void everyone_allowsWithoutAuthentication() {
        assertDoesNotThrow(() -> authorizationPolicyService.assertCanAccessUser(
                null, "target-user", AccessScope.EVERYONE));
    }

    @Test
    public void ownUser_allowsMatchingIds() {
        assertDoesNotThrow(() -> authorizationPolicyService.assertCanAccessUser(
                "user-1", "user-1", AccessScope.OWN_USER));
    }

    @Test
    public void ownUser_deniesMismatchedIds() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationPolicyService.assertCanAccessUser("user-1", "user-2", AccessScope.OWN_USER));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    public void householdMembers_allowsWhenRelationshipExists() {
        Mockito.when(userRelationshipService.areHouseholdMembers("user-1", "user-2")).thenReturn(true);

        assertDoesNotThrow(() -> authorizationPolicyService.assertCanAccessUser(
                "user-1", "user-2", AccessScope.HOUSEHOLD_MEMBERS));
    }

    @Test
    public void householdMembers_deniesWhenRelationshipMissing() {
        Mockito.when(userRelationshipService.areHouseholdMembers("user-1", "user-2")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationPolicyService.assertCanAccessUser("user-1", "user-2", AccessScope.HOUSEHOLD_MEMBERS));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    public void ownUser_deniesWithoutAuthentication() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationPolicyService.assertCanAccessUser(null, "user-1", AccessScope.OWN_USER));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
