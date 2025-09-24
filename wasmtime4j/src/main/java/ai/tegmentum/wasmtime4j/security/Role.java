package ai.tegmentum.wasmtime4j.security;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a security role for RBAC.
 *
 * @since 1.0.0
 */
public final class Role {

  private final String roleId;
  private final String name;
  private final Optional<String> description;
  private final Set<Permission> permissions;
  private final Set<String> parentRoles;
  private final Map<String, String> attributes;

  /**
   * Creates a new role.
   *
   * @param roleId unique role identifier
   * @param name role name
   * @param description role description (optional)
   * @param permissions role permissions
   * @param parentRoles parent roles
   * @param attributes role attributes
   */
  public Role(
      final String roleId,
      final String name,
      final Optional<String> description,
      final Set<Permission> permissions,
      final Set<String> parentRoles,
      final Map<String, String> attributes) {
    this.roleId = roleId;
    this.name = name;
    this.description = description;
    this.permissions = Set.copyOf(permissions);
    this.parentRoles = Set.copyOf(parentRoles);
    this.attributes = Map.copyOf(attributes);
  }

  public String getRoleId() {
    return roleId;
  }

  public String getName() {
    return name;
  }

  public Optional<String> getDescription() {
    return description;
  }

  public Set<Permission> getPermissions() {
    return Set.copyOf(permissions);
  }

  public Set<String> getParentRoles() {
    return Set.copyOf(parentRoles);
  }

  public Map<String, String> getAttributes() {
    return Map.copyOf(attributes);
  }
}
