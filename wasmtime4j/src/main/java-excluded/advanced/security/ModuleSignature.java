package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cryptographic signature for a WebAssembly module.
 *
 * <p>Contains the signature data, public key, algorithm information, and optional certificate chain
 * for verifying module authenticity and integrity.
 *
 * @since 1.0.0
 */
public final class ModuleSignature {

  private final SignatureAlgorithm algorithm;
  private final byte[] signature;
  private final byte[] publicKey;
  private final Optional<List<String>> certificateChain;
  private final Instant timestamp;
  private final Map<String, String> metadata;

  /**
   * Creates a new module signature.
   *
   * @param algorithm the signature algorithm used
   * @param signature the signature bytes
   * @param publicKey the public key bytes
   * @param certificateChain optional certificate chain
   * @param timestamp the signature timestamp
   * @param metadata additional signature metadata
   */
  public ModuleSignature(
      final SignatureAlgorithm algorithm,
      final byte[] signature,
      final byte[] publicKey,
      final Optional<List<String>> certificateChain,
      final Instant timestamp,
      final Map<String, String> metadata) {
    this.algorithm = algorithm;
    this.signature = signature.clone();
    this.publicKey = publicKey.clone();
    this.certificateChain = certificateChain;
    this.timestamp = timestamp;
    this.metadata = Map.copyOf(metadata);
  }

  /**
   * Gets the signature algorithm.
   *
   * @return the signature algorithm
   */
  public SignatureAlgorithm getAlgorithm() {
    return algorithm;
  }

  /**
   * Gets the signature bytes.
   *
   * @return a copy of the signature bytes
   */
  public byte[] getSignature() {
    return signature.clone();
  }

  /**
   * Gets the signature bytes (alias for getSignature).
   *
   * @return a copy of the signature bytes
   */
  public byte[] getSignatureBytes() {
    return getSignature();
  }

  /**
   * Gets the public key bytes.
   *
   * @return a copy of the public key bytes
   */
  public byte[] getPublicKey() {
    return publicKey.clone();
  }

  /**
   * Gets the certificate chain if present.
   *
   * @return the certificate chain, or empty if not present
   */
  public Optional<List<String>> getCertificateChain() {
    return certificateChain.map(List::copyOf);
  }

  /**
   * Gets the signature timestamp.
   *
   * @return the signature timestamp
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the signature metadata.
   *
   * @return a copy of the signature metadata
   */
  public Map<String, String> getMetadata() {
    return Map.copyOf(metadata);
  }

  /**
   * Gets a specific metadata value.
   *
   * @param key the metadata key
   * @return the metadata value, or empty if not present
   */
  public Optional<String> getMetadata(final String key) {
    return Optional.ofNullable(metadata.get(key));
  }

  /**
   * Checks if this signature has a certificate chain.
   *
   * @return true if a certificate chain is present, false otherwise
   */
  public boolean hasCertificateChain() {
    return certificateChain.isPresent() && !certificateChain.get().isEmpty();
  }

  /**
   * Gets the age of this signature.
   *
   * @return the duration since the signature was created
   */
  public java.time.Duration getAge() {
    return java.time.Duration.between(timestamp, Instant.now());
  }

  @Override
  public String toString() {
    return String.format(
        "ModuleSignature{algorithm=%s, timestamp=%s, hasCertChain=%s, metadata=%s}",
        algorithm, timestamp, hasCertificateChain(), metadata.keySet());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ModuleSignature that = (ModuleSignature) obj;
    return algorithm == that.algorithm
        && java.util.Arrays.equals(signature, that.signature)
        && java.util.Arrays.equals(publicKey, that.publicKey)
        && certificateChain.equals(that.certificateChain)
        && timestamp.equals(that.timestamp)
        && metadata.equals(that.metadata);
  }

  @Override
  public int hashCode() {
    int result = algorithm.hashCode();
    result = 31 * result + java.util.Arrays.hashCode(signature);
    result = 31 * result + java.util.Arrays.hashCode(publicKey);
    result = 31 * result + certificateChain.hashCode();
    result = 31 * result + timestamp.hashCode();
    result = 31 * result + metadata.hashCode();
    return result;
  }
}
