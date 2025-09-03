/**
 * Native library loading, extraction, and platform detection utilities for Wasmtime4j.
 *
 * <p>This package provides centralized functionality for managing native libraries across different
 * platforms and architectures. It handles platform detection, native library extraction from JAR
 * files, and loading of native libraries for both JNI and Panama implementations.
 *
 * <p>Key components include:
 *
 * <ul>
 *   <li>Platform detection and classification
 *   <li>Native library extraction and caching
 *   <li>Cross-platform file system operations
 *   <li>Error handling and logging
 * </ul>
 *
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.nativeloader;
