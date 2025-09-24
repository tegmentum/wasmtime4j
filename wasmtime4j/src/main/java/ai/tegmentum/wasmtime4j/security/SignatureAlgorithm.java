package ai.tegmentum.wasmtime4j.security;

/**
 * Supported cryptographic signature algorithms for module verification.
 *
 * @since 1.0.0
 */
public enum SignatureAlgorithm {

  /**
   * Ed25519 signature algorithm (recommended for new deployments).
   *
   * <p>Fast, secure, and widely supported elliptic curve signature algorithm.
   */
  ED25519("ed25519"),

  /**
   * RSA with SHA-256 signature algorithm.
   *
   * <p>Traditional RSA signatures with SHA-256 hashing, widely compatible but larger signature
   * sizes.
   */
  RSA_SHA256("rsa-sha256"),

  /**
   * ECDSA with P-256 curve and SHA-256 signature algorithm.
   *
   * <p>Elliptic curve signatures with NIST P-256 curve, good balance of security and compatibility.
   */
  ECDSA_P256_SHA256("ecdsa-p256-sha256"),

  /**
   * ECDSA with SHA-256 signature algorithm (alias for ECDSA_P256_SHA256).
   *
   * <p>Legacy alias for backward compatibility.
   */
  ECDSA_SHA256("ecdsa-sha256");

  private final String algorithmId;

  SignatureAlgorithm(final String algorithmId) {
    this.algorithmId = algorithmId;
  }

  /**
   * Gets the algorithm identifier string.
   *
   * @return the algorithm identifier
   */
  public String getAlgorithmId() {
    return algorithmId;
  }

  /**
   * Parses a signature algorithm from its identifier string.
   *
   * @param algorithmId the algorithm identifier
   * @return the corresponding SignatureAlgorithm
   * @throws IllegalArgumentException if the algorithm is not supported
   */
  public static SignatureAlgorithm fromAlgorithmId(final String algorithmId) {
    for (final SignatureAlgorithm algorithm : values()) {
      if (algorithm.algorithmId.equalsIgnoreCase(algorithmId)) {
        return algorithm;
      }
    }
    throw new IllegalArgumentException("Unsupported signature algorithm: " + algorithmId);
  }

  /**
   * Checks if this algorithm is considered secure for current use.
   *
   * <p>This method returns false for algorithms that are deprecated or have known security
   * weaknesses.
   *
   * @return true if the algorithm is considered secure, false otherwise
   */
  public boolean isSecure() {
    // All currently supported algorithms are considered secure
    return true;
  }

  /**
   * Gets the recommended key size for this algorithm.
   *
   * @return the recommended key size in bits
   */
  public int getRecommendedKeySize() {
    switch (this) {
      case ED25519:
        return 256; // Ed25519 uses 256-bit keys
      case RSA_SHA256:
        return 2048; // RSA should use at least 2048-bit keys
      case ECDSA_P256_SHA256:
      case ECDSA_SHA256:
        return 256; // P-256 curve uses 256-bit keys
      default:
        throw new IllegalStateException("Unknown algorithm: " + this);
    }
  }

  /**
   * Gets the typical signature size for this algorithm.
   *
   * @return the typical signature size in bytes
   */
  public int getSignatureSize() {
    switch (this) {
      case ED25519:
        return 64; // Ed25519 signatures are always 64 bytes
      case RSA_SHA256:
        return 256; // RSA-2048 signatures are 256 bytes
      case ECDSA_P256_SHA256:
      case ECDSA_SHA256:
        return 64; // ECDSA P-256 signatures are ~64 bytes (variable)
      default:
        throw new IllegalStateException("Unknown algorithm: " + this);
    }
  }

  @Override
  public String toString() {
    return algorithmId;
  }
}
