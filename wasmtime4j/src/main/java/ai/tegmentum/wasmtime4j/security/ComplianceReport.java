package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;

/**
 * Compliance report for regulatory frameworks.
 *
 * @since 1.0.0
 */
public interface ComplianceReport {

  /**
   * Gets the compliance framework.
   *
   * @return the framework
   */
  ComplianceFramework getFramework();

  /**
   * Gets when the report was generated.
   *
   * @return generation timestamp
   */
  Instant getGeneratedAt();

  /**
   * Gets the report period start.
   *
   * @return period start
   */
  Instant getPeriodStart();

  /**
   * Gets the report period end.
   *
   * @return period end
   */
  Instant getPeriodEnd();
}
