package ai.tegmentum.wasmtime4j.security;

/**
 * Module signature interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ModuleSignature {

  /**
   * Gets the signature algorithm.
   *
   * @return the signature algorithm
   */
  String getAlgorithm();

  /**
   * Gets the signature data.
   *
   * @return the signature as byte array
   */
  byte[] getSignature();

  /**
   * Gets the public key used for verification.
   *
   * @return the public key
   */
  byte[] getPublicKey();

  /**
   * Verifies the signature against the given data.
   *
   * @param data the data to verify
   * @return true if signature is valid
   */
  boolean verify(byte[] data);
}
