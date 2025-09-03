/**
 * Wasmtime4j Native Library Loader Module.
 *
 * <p>This module provides native library loading, extraction, and platform detection utilities for
 * the Wasmtime4j project. It serves as a centralized solution for managing native libraries across
 * different platforms and architectures.
 */
module ai.tegmentum.wasmtime4j.nativeloader {
  requires java.logging;

  exports ai.tegmentum.wasmtime4j.nativeloader;
}
