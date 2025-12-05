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

/**
 * WASI cryptography API interfaces for wasi:crypto.
 *
 * <p>This package provides comprehensive cryptographic operations including:
 *
 * <ul>
 *   <li>Symmetric encryption (AES, ChaCha20)
 *   <li>Asymmetric encryption (RSA, ECDH)
 *   <li>Digital signatures (ECDSA, Ed25519, RSA)
 *   <li>Hash functions (SHA-2, SHA-3, BLAKE)
 *   <li>Message authentication codes (HMAC)
 *   <li>Key derivation functions (HKDF, PBKDF2)
 *   <li>Post-quantum cryptography support
 *   <li>Hardware acceleration integration
 * </ul>
 *
 * <p>WASI-crypto specification: wasi:crypto@0.2.0
 *
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.wasi.crypto;
