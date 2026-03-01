/*
 * Copyright 2025 Tegmentum AI
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
package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of supported WASI versions.
 *
 * <p>This enum defines the available WASI versions that can be used when configuring WASI
 * components. Each version provides different capabilities and feature sets.
 *
 * <p>Key differences between versions:
 *
 * <ul>
 *   <li>Preview 1: Traditional POSIX-like interface with synchronous operations
 *   <li>Preview 2: Component model-based interface with async operations and improved composition
 * </ul>
 *
 * @since 1.0.0
 */
public enum WasiVersion {
  /**
   * WASI Preview 1 (0.1.0).
   *
   * <p>Traditional WASI interface providing POSIX-like system calls:
   *
   * <ul>
   *   <li>Synchronous file operations
   *   <li>Environment variable access
   *   <li>Command line argument access
   *   <li>Basic networking support
   *   <li>Clock and random operations
   * </ul>
   */
  PREVIEW_1("0.1.0", "wasi_unstable"),

  /**
   * WASI Preview 2 (0.2.0).
   *
   * <p>Modern WASI interface based on the WebAssembly Component Model:
   *
   * <ul>
   *   <li>Asynchronous I/O operations
   *   <li>Component-based composition
   *   <li>WIT (WebAssembly Interface Types) interfaces
   *   <li>Enhanced networking with HTTP support
   *   <li>Resource management and lifecycle
   *   <li>Stream-based operations
   * </ul>
   */
  PREVIEW_2("0.2.0", "wasi");

  private final String version;
  private final String importNamespace;

  WasiVersion(final String version, final String importNamespace) {
    this.version = version;
    this.importNamespace = importNamespace;
  }

  /**
   * Gets the version string for this WASI version.
   *
   * @return the version string (e.g., "0.1.0", "0.2.0")
   */
  public String getVersion() {
    return version;
  }

  /**
   * Gets the import namespace for this WASI version.
   *
   * @return the import namespace used in WebAssembly modules
   */
  public String getImportNamespace() {
    return importNamespace;
  }

  /**
   * Determines if this version supports async operations.
   *
   * @return true if async operations are supported, false otherwise
   */
  public boolean supportsAsyncOperations() {
    return this == PREVIEW_2;
  }

  /**
   * Determines if this version supports the component model.
   *
   * @return true if component model is supported, false otherwise
   */
  public boolean supportsComponentModel() {
    return this == PREVIEW_2;
  }

  /**
   * Determines if this version supports WIT interfaces.
   *
   * @return true if WIT interfaces are supported, false otherwise
   */
  public boolean supportsWitInterfaces() {
    return this == PREVIEW_2;
  }

  /**
   * Determines if this version supports stream operations.
   *
   * @return true if stream operations are supported, false otherwise
   */
  public boolean supportsStreamOperations() {
    return this == PREVIEW_2;
  }

  /**
   * Determines if this version supports HTTP operations.
   *
   * @return true if HTTP operations are supported, false otherwise
   */
  public boolean supportsHttpOperations() {
    return this == PREVIEW_2;
  }

  /**
   * Gets the default WASI version.
   *
   * <p>Returns Preview 1 for maximum compatibility with existing WebAssembly modules.
   *
   * @return the default WASI version
   */
  public static WasiVersion getDefault() {
    return PREVIEW_1;
  }

  /**
   * Gets the latest available WASI version.
   *
   * @return the latest WASI version
   */
  public static WasiVersion getLatest() {
    return PREVIEW_2;
  }

  /**
   * Parses a version string to the corresponding WasiVersion.
   *
   * @param versionString the version string to parse
   * @return the corresponding WasiVersion
   * @throws IllegalArgumentException if the version string is not recognized
   */
  public static WasiVersion fromVersionString(final String versionString) {
    if (versionString == null || versionString.trim().isEmpty()) {
      throw new IllegalArgumentException("Version string cannot be null or empty");
    }

    final String normalized = versionString.trim();
    for (final WasiVersion version : values()) {
      if (version.getVersion().equals(normalized)) {
        return version;
      }
    }

    throw new IllegalArgumentException("Unknown WASI version: " + versionString);
  }

  /**
   * Determines if this version is compatible with the specified version.
   *
   * <p>Currently, WASI versions are not backward compatible due to significant interface changes
   * between Preview 1 and Preview 2.
   *
   * @param other the version to check compatibility with
   * @return true if compatible, false otherwise
   */
  public boolean isCompatibleWith(final WasiVersion other) {
    // WASI Preview 1 and Preview 2 are not compatible
    return this == other;
  }

  @Override
  public String toString() {
    return String.format("WASI %s (%s)", version, name());
  }
}
