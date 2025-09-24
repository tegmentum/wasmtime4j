package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a user identity for security operations.
 *
 * @since 1.0.0
 */
public final class UserIdentity {

  private final String userId;
  private final String displayName;
  private final Optional<String> email;
  private final Set<String> groups;
  private final Map<String, String> attributes;
  private final Instant authenticatedAt;
  private final String authProvider;

  /**
   * Creates a new user identity.
   *
   * @param userId unique user identifier
   * @param displayName user display name
   * @param email user email address (optional)
   * @param groups user groups
   * @param attributes user attributes
   * @param authenticatedAt authentication timestamp
   * @param authProvider authentication provider
   */
  public UserIdentity(
      final String userId,
      final String displayName,
      final Optional<String> email,
      final Set<String> groups,
      final Map<String, String> attributes,
      final Instant authenticatedAt,
      final String authProvider) {
    this.userId = userId;
    this.displayName = displayName;
    this.email = email;
    this.groups = Set.copyOf(groups);
    this.attributes = Map.copyOf(attributes);
    this.authenticatedAt = authenticatedAt;
    this.authProvider = authProvider;
  }

  public String getUserId() {
    return userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Optional<String> getEmail() {
    return email;
  }

  public Set<String> getGroups() {
    return Set.copyOf(groups);
  }

  public Map<String, String> getAttributes() {
    return Map.copyOf(attributes);
  }

  public Instant getAuthenticatedAt() {
    return authenticatedAt;
  }

  public String getAuthProvider() {
    return authProvider;
  }
}
