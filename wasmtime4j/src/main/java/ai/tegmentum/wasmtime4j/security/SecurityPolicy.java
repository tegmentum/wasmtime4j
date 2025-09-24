package ai.tegmentum.wasmtime4j.security;

import java.time.Duration;
import java.util.Set;

/**
 * Security policy configuration.
 *
 * @since 1.0.0
 */
public interface SecurityPolicy {

  /**
   * Checks if signatures are required.
   *
   * @return true if required
   */
  boolean requireSignatures();

  /**
   * Checks if certificate chains are enforced.
   *
   * @return true if enforced
   */
  boolean enforceCertificateChains();

  /**
   * Gets allowed signature algorithms.
   *
   * @return allowed algorithms
   */
  Set<SignatureAlgorithm> getAllowedSignatureAlgorithms();

  /**
   * Gets maximum signature age.
   *
   * @return max age
   */
  Duration getMaxSignatureAge();

  /**
   * Checks if self-signed certificates are allowed.
   *
   * @return true if allowed
   */
  boolean allowSelfSigned();
}
