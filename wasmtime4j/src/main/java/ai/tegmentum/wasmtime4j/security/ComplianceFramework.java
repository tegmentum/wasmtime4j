package ai.tegmentum.wasmtime4j.security;

/**
 * Compliance frameworks supported by the security system.
 *
 * @since 1.0.0
 */
public enum ComplianceFramework {
  SOX("Sarbanes-Oxley Act"),
  GDPR("General Data Protection Regulation"),
  HIPAA("Health Insurance Portability and Accountability Act"),
  PCIDSS("Payment Card Industry Data Security Standard"),
  ISO27001("ISO 27001"),
  NIST("NIST Cybersecurity Framework");

  private final String displayName;

  ComplianceFramework(final String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
