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
 * Builder for configuring native library loading with a fluent API.
 *
 * <p>This builder provides comprehensive configuration options for native library loading,
 * including resource path conventions and custom naming patterns. All configurations are validated
 * and result in immutable, thread-safe objects.
 *
 * <p><strong>Basic Usage:</strong>
 *
 * <pre>{@code
 * LibraryLoadInfo info = new NativeLoaderBuilder()
 *     .libraryName("mylib")
 *     .load();
 * }</pre>
 *
 * <p><strong>Advanced Configuration:</strong>
 *
 * <pre>{@code
 * LibraryLoadInfo info = new NativeLoaderBuilder()
 *     .libraryName("mylib")
 *     .tempFilePrefix("mylib-native-")
 *     .tempDirSuffix("-mylib")
 *     .pathConvention(PathConvention.MAVEN_NATIVE)
 *     .load();
 * }</pre>
 *
 * <p>Builder instances are reusable and thread-safe for configuration. The {@link #load()} method
 * creates a new configuration and attempts library loading each time it is called.
 *
 * @since 1.0.0
 */
public final class NativeLoaderBuilder {

  // Configuration fields
  private String libraryName = NativeLibraryConfig.DEFAULT_LIBRARY_NAME;
  private String tempFilePrefix = NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX;
  private String tempDirSuffix = NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX;
  private PathConvention pathConvention = PathConvention.WASMTIME4J;
  private PathConvention.CustomPathConvention customPathConvention;
  private PathConvention[] conventionPriority;

  /** Package-private constructor - use {@link NativeLoader#builder()} to create instances. */
  NativeLoaderBuilder() {
    // Package-private constructor
  }

  /**
   * Sets the library name to load.
   *
   * <p>The library name should be the base name without platform-specific prefixes or extensions.
   * For example, use "wasmtime4j" rather than "libwasmtime4j.so" or "wasmtime4j.dll".
   *
   * @param libraryName the library name
   * @return this builder for method chaining
   * @throws IllegalArgumentException if libraryName is null
   */
  public NativeLoaderBuilder libraryName(final String libraryName) {
    Objects.requireNonNull(libraryName, "libraryName must not be null");
    this.libraryName = libraryName;
    return this;
  }

  /**
   * Sets the prefix for temporary files created during library extraction.
   *
   * <p>This prefix is used when creating temporary files and directories. It should end with a
   * separator character like "-" or "_" for readability.
   *
   * @param tempFilePrefix the temporary file prefix
   * @return this builder for method chaining
   * @throws IllegalArgumentException if tempFilePrefix is null
   */
  public NativeLoaderBuilder tempFilePrefix(final String tempFilePrefix) {
    Objects.requireNonNull(tempFilePrefix, "tempFilePrefix must not be null");
    this.tempFilePrefix = tempFilePrefix;
    return this;
  }

  /**
   * Sets the suffix for temporary directories created during library extraction.
   *
   * <p>This suffix is appended to temporary directory names. It should begin with a separator
   * character like "-" or "_" for readability.
   *
   * @param tempDirSuffix the temporary directory suffix
   * @return this builder for method chaining
   * @throws IllegalArgumentException if tempDirSuffix is null
   */
  public NativeLoaderBuilder tempDirSuffix(final String tempDirSuffix) {
    Objects.requireNonNull(tempDirSuffix, "tempDirSuffix must not be null");
    this.tempDirSuffix = tempDirSuffix;
    return this;
  }

  /**
   * Sets the path convention for locating libraries in JAR resources.
   *
   * <p>The path convention determines how native libraries are located within JAR files. Different
   * conventions support various packaging tools and deployment scenarios.
   *
   * @param pathConvention the path convention to use
   * @return this builder for method chaining
   * @throws IllegalArgumentException if pathConvention is null
   */
  public NativeLoaderBuilder pathConvention(final PathConvention pathConvention) {
    Objects.requireNonNull(pathConvention, "pathConvention must not be null");
    this.pathConvention = pathConvention;
    return this;
  }

  /**
   * Sets a custom path pattern when using CUSTOM path convention.
   *
   * <p>The pattern supports placeholders that will be substituted with platform-specific values:
   *
   * <ul>
   *   <li>{@code {platform}} - Full platform identifier (e.g., "linux-x86_64")
   *   <li>{@code {os}} - Operating system name (e.g., "linux")
   *   <li>{@code {arch}} - Architecture name (e.g., "x86_64")
   *   <li>{@code {lib}} - Platform-specific library prefix
   *   <li>{@code {name}} - Library name
   *   <li>{@code {ext}} - Platform-specific library extension
   * </ul>
   *
   * @param customPattern the custom path pattern with placeholders
   * @return this builder for method chaining
   * @throws IllegalArgumentException if customPattern is null or invalid
   */
  public NativeLoaderBuilder customPathPattern(final String customPattern) {
    Objects.requireNonNull(customPattern, "customPattern must not be null");
    // Create and validate the custom convention
    this.customPathConvention = PathConvention.custom(customPattern);
    return this;
  }

  /**
   * Sets the convention priority order for fallback resolution.
   *
   * <p>When multiple conventions are specified, they will be tried in the given order until a
   * library resource is found. If no priority is set, only the primary convention is used.
   *
   * @param conventions the conventions to try in order
   * @return this builder for method chaining
   * @throws IllegalArgumentException if conventions is null or empty
   */
  public NativeLoaderBuilder conventionPriority(final PathConvention... conventions) {
    Objects.requireNonNull(conventions, "conventions must not be null");
    if (conventions.length == 0) {
      throw new IllegalArgumentException("conventions must not be empty");
    }
    this.conventionPriority = conventions.clone();
    return this;
  }

  /**
   * Builds the configuration and attempts to load the native library.
   *
   * <p>This method validates all configuration parameters, builds an immutable configuration
   * object, and attempts to load the specified native library using the configured parameters.
   *
   * <p>The loading process follows these strategies in order:
   *
   * <ol>
   *   <li>Attempt to load from system library path
   *   <li>Extract from JAR resources and load from temporary location
   * </ol>
   *
   * @return information about the library loading attempt
   * @throws IllegalArgumentException if any configuration parameter is invalid
   * @throws IllegalStateException if the configuration cannot be built
   */
  public NativeLibraryUtils.LibraryLoadInfo load() {
    final NativeLibraryConfig config = new NativeLibraryConfig(libraryName, tempFilePrefix, tempDirSuffix);
    return performLoad(config);
  }

  /**
   * Performs the actual library loading using the configured conventions.
   *
   * @param config the base configuration
   * @return the library loading information
   */
  private NativeLibraryUtils.LibraryLoadInfo performLoad(final NativeLibraryConfig config) {
    // Determine which conventions to use
    final PathConvention[] conventions = getConventionsToTry();

    if (customPathConvention != null) {
      // Use custom convention with fallback to standard conventions
      return NativeLibraryUtils.loadNativeLibraryWithCustomConvention(
          libraryName, config, customPathConvention, conventions);
    } else if (conventions.length > 1 || conventions[0] != PathConvention.WASMTIME4J) {
      // Use standard convention-based loading
      return NativeLibraryUtils.loadNativeLibraryWithConventions(libraryName, config, conventions);
    } else {
      // Use legacy loading method for backward compatibility
      return NativeLibraryUtils.loadNativeLibrary(libraryName, config);
    }
  }

  /**
   * Gets the conventions to try in priority order.
   *
   * @return the conventions array
   */
  private PathConvention[] getConventionsToTry() {
    if (conventionPriority != null && conventionPriority.length > 0) {
      return conventionPriority.clone();
    } else {
      return new PathConvention[] {pathConvention};
    }
  }

  /**
   * Gets the current library name.
   *
   * @return the library name
   */
  public String getLibraryName() {
    return libraryName;
  }

  /**
   * Gets the current temporary file prefix.
   *
   * @return the temporary file prefix
   */
  public String getTempFilePrefix() {
    return tempFilePrefix;
  }

  /**
   * Gets the current temporary directory suffix.
   *
   * @return the temporary directory suffix
   */
  public String getTempDirSuffix() {
    return tempDirSuffix;
  }

  /**
   * Gets the current path convention.
   *
   * @return the path convention
   */
  public PathConvention getPathConvention() {
    return pathConvention;
  }

  /**
   * Gets the current custom path convention.
   *
   * @return the custom path convention, or null if not set
   */
  public PathConvention.CustomPathConvention getCustomPathConvention() {
    return customPathConvention;
  }

  /**
   * Gets the current convention priority array.
   *
   * @return the convention priority array, or null if not set
   */
  public PathConvention[] getConventionPriority() {
    return conventionPriority != null ? conventionPriority.clone() : null;
  }
}
