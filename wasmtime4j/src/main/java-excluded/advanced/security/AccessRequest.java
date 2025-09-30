package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an access request for authorization.
 *
 * @since 1.0.0
 */
public final class AccessRequest {

  private final UserIdentity user;
  private final String resourceType;
  private final Optional<String> resourceId;
  private final String action;
  private final Map<String, String> context;
  private final Instant timestamp;

  /**
   * Creates a new access request.
   *
   * @param user the user making the request
   * @param resourceType the resource type being accessed
   * @param resourceId the specific resource identifier (optional)
   * @param action the action being performed
   * @param context the request context
   */
  public AccessRequest(
      final UserIdentity user,
      final String resourceType,
      final Optional<String> resourceId,
      final String action,
      final Map<String, String> context) {
    this.user = user;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.action = action;
    this.context = Map.copyOf(context);
    this.timestamp = Instant.now();
  }

  public UserIdentity getUser() {
    return user;
  }

  public String getResourceType() {
    return resourceType;
  }

  public Optional<String> getResourceId() {
    return resourceId;
  }

  public String getAction() {
    return action;
  }

  public Map<String, String> getContext() {
    return Map.copyOf(context);
  }

  public Instant getTimestamp() {
    return timestamp;
  }
}
