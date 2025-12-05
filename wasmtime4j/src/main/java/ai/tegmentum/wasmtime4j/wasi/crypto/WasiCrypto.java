/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.wasi.crypto;

/**
 * WASI cryptography context for comprehensive cryptographic operations.
 *
 * <p>Provides access to symmetric and asymmetric encryption, digital signatures, hash functions,
 * message authentication codes, and key management.
 *
 * <p>WASI-crypto specification: wasi:crypto@0.2.0
 *
 * @since 1.0.0
 */
public interface WasiCrypto extends AutoCloseable {

  // Key Generation

  /**
   * Generates a new symmetric key for the specified algorithm.
   *
   * @param algorithm the symmetric algorithm
   * @return the generated key
   * @throws CryptoException if key generation fails
   */
  CryptoKey generateSymmetricKey(SymmetricAlgorithm algorithm) throws CryptoException;

  /**
   * Generates a new key pair for the specified asymmetric algorithm.
   *
   * @param algorithm the asymmetric algorithm
   * @return the generated key pair
   * @throws CryptoException if key generation fails
   */
  CryptoKey generateKeyPair(AsymmetricAlgorithm algorithm) throws CryptoException;

  /**
   * Generates a new key pair for the specified signature algorithm.
   *
   * @param algorithm the signature algorithm
   * @return the generated key pair
   * @throws CryptoException if key generation fails
   */
  CryptoKey generateSignatureKeyPair(SignatureAlgorithm algorithm) throws CryptoException;

  /**
   * Imports a symmetric key from raw bytes.
   *
   * @param algorithm the symmetric algorithm
   * @param keyBytes the raw key bytes
   * @return the imported key
   * @throws CryptoException if import fails
   */
  CryptoKey importSymmetricKey(SymmetricAlgorithm algorithm, byte[] keyBytes)
      throws CryptoException;

  /**
   * Imports a public key from encoded bytes.
   *
   * @param algorithm the asymmetric algorithm
   * @param keyBytes the encoded key bytes
   * @return the imported public key
   * @throws CryptoException if import fails
   */
  CryptoKey importPublicKey(AsymmetricAlgorithm algorithm, byte[] keyBytes) throws CryptoException;

  /**
   * Imports a private key from encoded bytes.
   *
   * @param algorithm the asymmetric algorithm
   * @param keyBytes the encoded key bytes
   * @return the imported private key
   * @throws CryptoException if import fails
   */
  CryptoKey importPrivateKey(AsymmetricAlgorithm algorithm, byte[] keyBytes) throws CryptoException;

  // Symmetric Encryption

  /**
   * Encrypts data using symmetric encryption.
   *
   * @param data the plaintext data
   * @param algorithm the symmetric algorithm
   * @param key the encryption key
   * @param options encryption options
   * @return the ciphertext
   * @throws CryptoException if encryption fails
   */
  byte[] symmetricEncrypt(
      byte[] data, SymmetricAlgorithm algorithm, CryptoKey key, EncryptionOptions options)
      throws CryptoException;

  /**
   * Decrypts data using symmetric decryption.
   *
   * @param ciphertext the encrypted data
   * @param algorithm the symmetric algorithm
   * @param key the decryption key
   * @param options encryption options
   * @return the plaintext
   * @throws CryptoException if decryption fails
   */
  byte[] symmetricDecrypt(
      byte[] ciphertext, SymmetricAlgorithm algorithm, CryptoKey key, EncryptionOptions options)
      throws CryptoException;

  // Asymmetric Encryption

  /**
   * Encrypts data using asymmetric encryption.
   *
   * @param data the plaintext data
   * @param algorithm the asymmetric algorithm
   * @param publicKey the public key
   * @param padding the padding scheme
   * @return the ciphertext
   * @throws CryptoException if encryption fails
   */
  byte[] asymmetricEncrypt(
      byte[] data, AsymmetricAlgorithm algorithm, CryptoKey publicKey, PaddingScheme padding)
      throws CryptoException;

  /**
   * Decrypts data using asymmetric decryption.
   *
   * @param ciphertext the encrypted data
   * @param algorithm the asymmetric algorithm
   * @param privateKey the private key
   * @param padding the padding scheme
   * @return the plaintext
   * @throws CryptoException if decryption fails
   */
  byte[] asymmetricDecrypt(
      byte[] ciphertext, AsymmetricAlgorithm algorithm, CryptoKey privateKey, PaddingScheme padding)
      throws CryptoException;

  // Digital Signatures

  /**
   * Signs a message using a digital signature algorithm.
   *
   * @param message the message to sign
   * @param algorithm the signature algorithm
   * @param privateKey the signing key
   * @param options signature options
   * @return the signature bytes
   * @throws CryptoException if signing fails
   */
  byte[] sign(
      byte[] message, SignatureAlgorithm algorithm, CryptoKey privateKey, SignatureOptions options)
      throws CryptoException;

  /**
   * Verifies a digital signature.
   *
   * @param message the original message
   * @param signature the signature to verify
   * @param algorithm the signature algorithm
   * @param publicKey the verification key
   * @param options signature options
   * @return true if the signature is valid
   * @throws CryptoException if verification fails
   */
  boolean verify(
      byte[] message,
      byte[] signature,
      SignatureAlgorithm algorithm,
      CryptoKey publicKey,
      SignatureOptions options)
      throws CryptoException;

  // Hash Functions

  /**
   * Computes a cryptographic hash.
   *
   * @param data the data to hash
   * @param algorithm the hash algorithm
   * @return the hash digest
   * @throws CryptoException if hashing fails
   */
  byte[] hash(byte[] data, HashAlgorithm algorithm) throws CryptoException;

  // Message Authentication Codes

  /**
   * Computes a message authentication code.
   *
   * @param data the data to authenticate
   * @param algorithm the MAC algorithm
   * @param key the MAC key
   * @return the MAC value
   * @throws CryptoException if MAC computation fails
   */
  byte[] mac(byte[] data, MacAlgorithm algorithm, CryptoKey key) throws CryptoException;

  /**
   * Verifies a message authentication code.
   *
   * @param data the original data
   * @param mac the MAC to verify
   * @param algorithm the MAC algorithm
   * @param key the MAC key
   * @return true if the MAC is valid
   * @throws CryptoException if verification fails
   */
  boolean verifyMac(byte[] data, byte[] mac, MacAlgorithm algorithm, CryptoKey key)
      throws CryptoException;

  // Key Derivation

  /**
   * Derives a key using a key derivation function.
   *
   * @param inputKeyMaterial the input key material
   * @param algorithm the KDF algorithm
   * @param salt optional salt
   * @param info optional context info
   * @param outputLength the desired output length in bytes
   * @return the derived key bytes
   * @throws CryptoException if derivation fails
   */
  byte[] deriveKey(
      byte[] inputKeyMaterial,
      KeyDerivationAlgorithm algorithm,
      byte[] salt,
      byte[] info,
      int outputLength)
      throws CryptoException;

  // Key Agreement

  /**
   * Performs key agreement to derive a shared secret.
   *
   * @param algorithm the key agreement algorithm
   * @param privateKey the local private key
   * @param publicKey the remote public key
   * @return the shared secret
   * @throws CryptoException if key agreement fails
   */
  byte[] keyAgreement(AsymmetricAlgorithm algorithm, CryptoKey privateKey, CryptoKey publicKey)
      throws CryptoException;

  @Override
  void close();
}
