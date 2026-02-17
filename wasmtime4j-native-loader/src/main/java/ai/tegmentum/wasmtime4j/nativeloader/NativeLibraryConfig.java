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

package ai.tegmentum.wasmtime4j.nativeloader;

import java.util.Objects;

/**
 * Configuration for native library loading parameters.
 *
 * <p>This class provides configurable parameters for native library loading, allowing different
 * projects to customize the library name and temporary file naming conventions while maintaining
 * backward compatibility with existing APIs.
 *
 * <p>All configuration parameters are validated for security to prevent path traversal attacks and
 * other malicious inputs.
 */
public final class NativeLibraryConfig {

  /** Default library name for backward compatibility. */
  public static final String DEFAULT_LIBRARY_NAME = "wasmtime4j";

  /** Default temporary file prefix for backward compatibility. */
  public static final String DEFAULT_TEMP_FILE_PREFIX = "wasmtime4j-native-";

  /** Default temporary directory suffix for backward compatibility. */
  public static final String DEFAULT_TEMP_DIR_SUFFIX = "-wasmtime4j";

  /** The base name of the native library. */
  private final String libraryName;

  /** Prefix for temporary files. */
  private final String tempFilePrefix;

  /** Suffix for temporary directories. */
  private final String tempDirSuffix;

  /**
   * Creates a configuration with default values.
   *
   * @return a configuration with default values
   */
  public static NativeLibraryConfig defaultConfig() {
    return new NativeLibraryConfig(
        DEFAULT_LIBRARY_NAME, DEFAULT_TEMP_FILE_PREFIX, DEFAULT_TEMP_DIR_SUFFIX);
  }

  /**
   * Creates a configuration with specified parameters.
   *
   * @param libraryName the base name of the native library
   * @param tempFilePrefix prefix for temporary files
   * @param tempDirSuffix suffix for temporary directories
   */
  public NativeLibraryConfig(
      final String libraryName, final String tempFilePrefix, final String tempDirSuffix) {
    this.libraryName = validateLibraryName(libraryName);
    this.tempFilePrefix = validateTempFilePrefix(tempFilePrefix);
    this.tempDirSuffix = validateTempDirSuffix(tempDirSuffix);
  }

  /**
   * Gets the library name.
   *
   * @return the library name
   */
  public String getLibraryName() {
    return libraryName;
  }

  /**
   * Gets the temporary file prefix.
   *
   * @return the temporary file prefix
   */
  public String getTempFilePrefix() {
    return tempFilePrefix;
  }

  /**
   * Gets the temporary directory suffix.
   *
   * @return the temporary directory suffix
   */
  public String getTempDirSuffix() {
    return tempDirSuffix;
  }

  /**
   * Validates the library name parameter.
   *
   * @param libraryName the library name to validate
   * @return the validated library name
   * @throws IllegalArgumentException if the library name is invalid
   */
  private static String validateLibraryName(final String libraryName) {
    Objects.requireNonNull(libraryName, "libraryName must not be null");

    final String trimmed = libraryName.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("libraryName must not be empty");
    }

    // Validate that library name contains only safe characters
    if (!trimmed.matches("^[a-zA-Z0-9_-]+$")) {
      throw new IllegalArgumentException(
          "libraryName must contain only alphanumeric characters, underscores, and dashes: "
              + trimmed);
    }

    return trimmed;
  }

  /**
   * Validates the temporary file prefix parameter.
   *
   * @param tempFilePrefix the temporary file prefix to validate
   * @return the validated temporary file prefix
   * @throws IllegalArgumentException if the temporary file prefix is invalid
   */
  private static String validateTempFilePrefix(final String tempFilePrefix) {
    Objects.requireNonNull(tempFilePrefix, "tempFilePrefix must not be null");

    final String trimmed = tempFilePrefix.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("tempFilePrefix must not be empty");
    }

    // Validate that prefix contains only safe characters and doesn't contain path separators
    if (!trimmed.matches("^[a-zA-Z0-9_-]+$")) {
      throw new IllegalArgumentException(
          "tempFilePrefix must contain only alphanumeric characters, underscores, and dashes: "
              + trimmed);
    }

    return trimmed;
  }

  /**
   * Validates the temporary directory suffix parameter.
   *
   * @param tempDirSuffix the temporary directory suffix to validate
   * @return the validated temporary directory suffix
   * @throws IllegalArgumentException if the temporary directory suffix is invalid
   */
  private static String validateTempDirSuffix(final String tempDirSuffix) {
    Objects.requireNonNull(tempDirSuffix, "tempDirSuffix must not be null");

    final String trimmed = tempDirSuffix.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("tempDirSuffix must not be empty");
    }

    // Validate that suffix contains only safe characters and doesn't contain path separators
    if (!trimmed.matches("^-?[a-zA-Z0-9_-]+$")) {
      throw new IllegalArgumentException(
          "tempDirSuffix must contain only alphanumeric characters, underscores, and dashes: "
              + trimmed);
    }

    return trimmed;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final NativeLibraryConfig that = (NativeLibraryConfig) obj;
    return Objects.equals(libraryName, that.libraryName)
        && Objects.equals(tempFilePrefix, that.tempFilePrefix)
        && Objects.equals(tempDirSuffix, that.tempDirSuffix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(libraryName, tempFilePrefix, tempDirSuffix);
  }

  @Override
  public String toString() {
    return "NativeLibraryConfig{"
        + "libraryName='"
        + libraryName
        + '\''
        + ", tempFilePrefix='"
        + tempFilePrefix
        + '\''
        + ", tempDirSuffix='"
        + tempDirSuffix
        + '\''
        + '}';
  }
}
