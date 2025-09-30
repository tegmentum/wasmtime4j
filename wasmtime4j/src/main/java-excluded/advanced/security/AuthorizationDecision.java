package ai.tegmentum.wasmtime4j.security;

import java.util.List;

/**
 * Result of an authorization decision.
 *
 * @since 1.0.0
 */
public abstract class AuthorizationDecision {

  /** Decision result types. */
  public enum Result {
    ALLOW,
    DENY,
    INDETERMINATE
  }

  private final Result result;
  private final String reason;

  protected AuthorizationDecision(final Result result, final String reason) {
    this.result = result;
    this.reason = reason;
  }

  /**
   * Creates an allow decision.
   *
   * @param reason the reason for allowing
   * @param permissions the applicable permissions
   * @return an allow decision
   */
  public static AuthorizationDecision allow(
      final String reason, final List<Permission> permissions) {
    return new Allow(reason, permissions);
  }

  /**
   * Creates a deny decision.
   *
   * @param reason the reason for denying
   * @param missingPermissions the missing permissions
   * @return a deny decision
   */
  public static AuthorizationDecision deny(
      final String reason, final List<Permission> missingPermissions) {
    return new Deny(reason, missingPermissions);
  }

  /**
   * Creates an indeterminate decision.
   *
   * @param reason the reason for indeterminate result
   * @return an indeterminate decision
   */
  public static AuthorizationDecision indeterminate(final String reason) {
    return new Indeterminate(reason);
  }

  public Result getResult() {
    return result;
  }

  public String getReason() {
    return reason;
  }

  public boolean isAllowed() {
    return result == Result.ALLOW;
  }

  public boolean isDenied() {
    return result == Result.DENY;
  }

  public boolean isIndeterminate() {
    return result == Result.INDETERMINATE;
  }

  private static final class Allow extends AuthorizationDecision {
    private final List<Permission> permissions;

    Allow(final String reason, final List<Permission> permissions) {
      super(Result.ALLOW, reason);
      this.permissions = List.copyOf(permissions);
    }

    public List<Permission> getPermissions() {
      return permissions;
    }
  }

  private static final class Deny extends AuthorizationDecision {
    private final List<Permission> missingPermissions;

    Deny(final String reason, final List<Permission> missingPermissions) {
      super(Result.DENY, reason);
      this.missingPermissions = List.copyOf(missingPermissions);
    }

    public List<Permission> getMissingPermissions() {
      return missingPermissions;
    }
  }

  private static final class Indeterminate extends AuthorizationDecision {
    Indeterminate(final String reason) {
      super(Result.INDETERMINATE, reason);
    }
  }
}
