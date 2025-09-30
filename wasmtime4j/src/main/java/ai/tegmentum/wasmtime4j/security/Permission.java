package ai.tegmentum.wasmtime4j.security;

import java.util.Map;
import java.util.Optional;

/**
 * Represents a security permission for access control.
 *
 * @since 1.0.0
 */
public final class Permission {

  private final String resourceType;
  private final Optional<String> resourceId;
  private final String action;
  private final Map<String, String> conditions;

  /**
   * Creates a new permission.
   *
   * @param resourceType the resource type
   * @param resourceId the specific resource identifier (optional)
   * @param action the action
   * @param conditions additional conditions
   */
  public Permission(
      final String resourceType,
      final Optional<String> resourceId,
      final String action,
      final Map<String, String> conditions) {
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.action = action;
    this.conditions = Map.copyOf(conditions);
  }

  /**
   * Creates a simple permission.
   *
   * @param resourceType the resource type
   * @param action the action
   * @return a new permission
   */
  public static Permission of(final String resourceType, final String action) {
    return new Permission(resourceType, Optional.empty(), action, Map.of());
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

  public Map<String, String> getConditions() {
    return Map.copyOf(conditions);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Permission)) {
      return false;
    }
    final Permission that = (Permission) obj;
    return resourceType.equals(that.resourceType)
        && resourceId.equals(that.resourceId)
        && action.equals(that.action)
        && conditions.equals(that.conditions);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(resourceType, resourceId, action, conditions);
  }

  @Override
  public String toString() {
    return String.format(
        "Permission{type=%s, id=%s, action=%s}", resourceType, resourceId.orElse("*"), action);
  }
}
