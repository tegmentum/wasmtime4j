package ai.tegmentum.wasmtime4j.security;

import java.util.Set;

/**
 * Modifier for creating derived security policies with additional restrictions.
 *
 * @since 1.0.0
 */
public final class SecurityPolicyModifier {

  private final Set<Capability> capabilitiesToRevoke;
  private final ResourceLimits additionalLimits;
  private final EnforcementLevel enforcementLevel;

  public SecurityPolicyModifier(
      final Set<Capability> capabilitiesToRevoke,
      final ResourceLimits additionalLimits,
      final EnforcementLevel enforcementLevel) {
    this.capabilitiesToRevoke = Set.copyOf(capabilitiesToRevoke);
    this.additionalLimits = additionalLimits;
    this.enforcementLevel = enforcementLevel;
  }

  public static SecurityPolicyModifier restrictive() {
    return new SecurityPolicyModifier(
        Set.of(Capability.HOST_NATIVE_ACCESS, Capability.FILESYSTEM_DELETE),
        ResourceLimits.minimal(),
        EnforcementLevel.STRICT);
  }

  public Set<Capability> getCapabilitiesToRevoke() {
    return capabilitiesToRevoke;
  }

  public ResourceLimits getAdditionalLimits() {
    return additionalLimits;
  }

  public EnforcementLevel getEnforcementLevel() {
    return enforcementLevel;
  }
}
