package ai.tegmentum.wasmtime4j.wasi;

import java.util.EnumSet;
import java.util.Set;

/**
 * Enumeration of WASI resource permission types.
 *
 * <p>Defines the different permission levels that can be granted to components for accessing WASI
 * resources. Permissions can be combined to create fine-grained access control policies.
 *
 * @since 1.0.0
 */
public enum WasiResourcePermissions {
  /** No permissions - resource cannot be accessed. */
  NONE("none", 0),

  /** Read permission - resource can be read from. */
  READ("read", 1),

  /** Write permission - resource can be written to. */
  WRITE("write", 2),

  /** Execute permission - resource can be executed or invoked. */
  EXECUTE("execute", 4),

  /** Create permission - new instances of the resource can be created. */
  CREATE("create", 8),

  /** Delete permission - resource instances can be deleted. */
  DELETE("delete", 16),

  /** Admin permission - full administrative access to the resource. */
  ADMIN("admin", 32);

  /** Read-only permission set. */
  public static final Set<WasiResourcePermissions> READ_ONLY = EnumSet.of(READ);

  /** Write-only permission set. */
  public static final Set<WasiResourcePermissions> WRITE_ONLY = EnumSet.of(WRITE);

  /** Read-write permission set. */
  public static final Set<WasiResourcePermissions> READ_WRITE = EnumSet.of(READ, WRITE);

  /** Full permission set (all permissions except ADMIN). */
  public static final Set<WasiResourcePermissions> FULL =
      EnumSet.of(READ, WRITE, EXECUTE, CREATE, DELETE);

  /** All permissions including administrative access. */
  public static final Set<WasiResourcePermissions> ALL =
      EnumSet.allOf(WasiResourcePermissions.class);

  private final String name;
  private final int mask;

  WasiResourcePermissions(final String name, final int mask) {
    this.name = name;
    this.mask = mask;
  }

  /**
   * Gets the permission name.
   *
   * @return the permission name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the permission bit mask.
   *
   * @return the permission bit mask
   */
  public int getMask() {
    return mask;
  }

  /**
   * Checks if this permission includes another permission.
   *
   * @param other the permission to check
   * @return true if this permission includes the other permission, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  public boolean includes(final WasiResourcePermissions other) {
    if (other == null) {
      throw new IllegalArgumentException("Permission cannot be null");
    }
    return (this.mask & other.mask) == other.mask;
  }

  /**
   * Combines this permission with another permission.
   *
   * @param other the permission to combine with
   * @return a set containing both permissions
   * @throws IllegalArgumentException if other is null
   */
  public Set<WasiResourcePermissions> with(final WasiResourcePermissions other) {
    if (other == null) {
      throw new IllegalArgumentException("Permission cannot be null");
    }
    return EnumSet.of(this, other);
  }

  /**
   * Checks if a permission set includes all specified permissions.
   *
   * @param permissions the permission set to check
   * @param required the required permissions
   * @return true if all required permissions are present, false otherwise
   * @throws IllegalArgumentException if any parameter is null
   */
  public static boolean hasAll(
      final Set<WasiResourcePermissions> permissions, final WasiResourcePermissions... required) {
    if (permissions == null) {
      throw new IllegalArgumentException("Permissions set cannot be null");
    }
    if (required == null) {
      throw new IllegalArgumentException("Required permissions cannot be null");
    }

    for (final WasiResourcePermissions perm : required) {
      if (!permissions.contains(perm)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if a permission set includes any of the specified permissions.
   *
   * @param permissions the permission set to check
   * @param candidates the candidate permissions
   * @return true if any candidate permission is present, false otherwise
   * @throws IllegalArgumentException if any parameter is null
   */
  public static boolean hasAny(
      final Set<WasiResourcePermissions> permissions, final WasiResourcePermissions... candidates) {
    if (permissions == null) {
      throw new IllegalArgumentException("Permissions set cannot be null");
    }
    if (candidates == null) {
      throw new IllegalArgumentException("Candidate permissions cannot be null");
    }

    for (final WasiResourcePermissions perm : candidates) {
      if (permissions.contains(perm)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a permission set from a bit mask.
   *
   * @param mask the permission bit mask
   * @return set of permissions corresponding to the mask
   */
  public static Set<WasiResourcePermissions> fromMask(final int mask) {
    final Set<WasiResourcePermissions> permissions = EnumSet.noneOf(WasiResourcePermissions.class);

    for (final WasiResourcePermissions perm : values()) {
      if (perm != NONE && (mask & perm.mask) == perm.mask) {
        permissions.add(perm);
      }
    }

    return permissions;
  }

  /**
   * Converts a permission set to a bit mask.
   *
   * @param permissions the permission set to convert
   * @return the corresponding bit mask
   * @throws IllegalArgumentException if permissions is null
   */
  public static int toMask(final Set<WasiResourcePermissions> permissions) {
    if (permissions == null) {
      throw new IllegalArgumentException("Permissions set cannot be null");
    }

    int mask = 0;
    for (final WasiResourcePermissions perm : permissions) {
      mask |= perm.mask;
    }
    return mask;
  }

  /**
   * Finds a permission by name.
   *
   * @param name the permission name to find
   * @return the permission, or null if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  public static WasiResourcePermissions fromName(final String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Permission name cannot be null or empty");
    }

    for (final WasiResourcePermissions perm : values()) {
      if (perm.name.equalsIgnoreCase(name.trim())) {
        return perm;
      }
    }

    return null;
  }

  /**
   * Checks if a permission name is valid.
   *
   * @param name the permission name to check
   * @return true if the name corresponds to a known permission, false otherwise
   */
  public static boolean isValidName(final String name) {
    return fromName(name) != null;
  }
}
