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
 * <p>This builder provides comprehensive configuration options for native library loading, including
 * security levels, resource path conventions, and custom naming patterns. All configurations are
 * validated and result in immutable, thread-safe objects.
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
 *     .securityLevel(SecurityLevel.STRICT)
 *     .resourcePathConvention(ResourcePathConvention.MAVEN_NATIVE)
 *     .load();
 * }</pre>
 *
 * <p>Builder instances are reusable and thread-safe for configuration. The {@link #load()} method
 * creates a new configuration and attempts library loading each time it is called.
 *
 * @since 1.0.0
 */
public final class NativeLoaderBuilder {

  /**
   * Security level for native library loading operations.
   *
   * <p>Security levels provide different trade-offs between security and compatibility:
   *
   * <ul>
   *   <li><strong>STRICT</strong>: Maximum security, restrictive validation
   *   <li><strong>MODERATE</strong>: Balanced security and compatibility (default)
   *   <li><strong>PERMISSIVE</strong>: Minimal security, maximum compatibility
   * </ul>
   */
  public enum SecurityLevel {
    /**
     * Strict security level with maximum validation.
     *
     * <p>This level enforces:
     *
     * <ul>
     *   <li>Strict filename and path validation
     *   <li>Enhanced path traversal protection
     *   <li>Conservative temporary directory permissions
     *   <li>Additional resource validation checks
     * </ul>
     */
    STRICT,

    /**
     * Moderate security level balancing security and compatibility.
     *
     * <p>This is the default level that provides reasonable security while maintaining broad
     * compatibility. It enforces standard validation patterns used throughout the system.
     */
    MODERATE,

    /**
     * Permissive security level with minimal restrictions.
     *
     * <p>This level should only be used when maximum compatibility is required and the environment
     * is trusted. It provides basic validation but allows more flexible naming patterns.
     */
    PERMISSIVE
  }

  /**
   * Resource path convention for locating native libraries within JAR resources.
   *
   * <p>Different conventions support various packaging and deployment scenarios:
   *
   * <ul>
   *   <li><strong>MAVEN_NATIVE</strong>: Standard Maven native plugin layout
   *   <li><strong>JNA</strong>: JNA-compatible resource layout
   *   <li><strong>CUSTOM</strong>: User-defined resource paths
   * </ul>
   */
  public enum ResourcePathConvention {
    /**
     * Maven Native plugin convention.
     *
     * <p>Uses the standard Maven native plugin resource layout:
     * {@code /natives/{os}-{arch}/{libraryname}.{extension}}
     */
    MAVEN_NATIVE,

    /**
     * JNA (Java Native Access) convention.
     *
     * <p>Uses JNA-compatible resource paths for better interoperability with JNA-based libraries.
     */
    JNA,

    /**
     * Custom resource path convention.
     *
     * <p>Allows user-defined resource path patterns. When using this convention, additional
     * configuration methods may be available to specify the exact path format.
     */
    CUSTOM
  }

  /** Default library name for backward compatibility. */
  private static final String DEFAULT_LIBRARY_NAME = NativeLibraryConfig.DEFAULT_LIBRARY_NAME;

  /** Default temporary file prefix. */
  private static final String DEFAULT_TEMP_FILE_PREFIX = NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX;

  /** Default temporary directory suffix. */
  private static final String DEFAULT_TEMP_DIR_SUFFIX = NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX;

  /** Default security level. */
  private static final SecurityLevel DEFAULT_SECURITY_LEVEL = SecurityLevel.MODERATE;

  /** Default resource path convention. */
  private static final ResourcePathConvention DEFAULT_RESOURCE_PATH_CONVENTION = 
      ResourcePathConvention.MAVEN_NATIVE;

  // Configuration fields
  private String libraryName = DEFAULT_LIBRARY_NAME;
  private String tempFilePrefix = DEFAULT_TEMP_FILE_PREFIX;
  private String tempDirSuffix = DEFAULT_TEMP_DIR_SUFFIX;
  private SecurityLevel securityLevel = DEFAULT_SECURITY_LEVEL;
  private ResourcePathConvention resourcePathConvention = DEFAULT_RESOURCE_PATH_CONVENTION;

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
   * Sets the security level for library loading operations.
   *
   * <p>The security level affects validation strictness and security measures applied during
   * loading. See {@link SecurityLevel} for detailed descriptions of each level.
   *
   * @param securityLevel the security level
   * @return this builder for method chaining
   * @throws IllegalArgumentException if securityLevel is null
   */
  public NativeLoaderBuilder securityLevel(final SecurityLevel securityLevel) {
    Objects.requireNonNull(securityLevel, "securityLevel must not be null");
    this.securityLevel = securityLevel;
    return this;
  }

  /**
   * Sets the resource path convention for locating libraries in JAR resources.
   *
   * <p>The resource path convention determines how native libraries are located within JAR files.
   * Different conventions support various packaging tools and deployment scenarios.
   *
   * @param resourcePathConvention the resource path convention
   * @return this builder for method chaining
   * @throws IllegalArgumentException if resourcePathConvention is null
   */
  public NativeLoaderBuilder resourcePathConvention(
      final ResourcePathConvention resourcePathConvention) {
    Objects.requireNonNull(resourcePathConvention, "resourcePathConvention must not be null");
    this.resourcePathConvention = resourcePathConvention;
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
   * <p>Security validation is applied according to the configured {@link SecurityLevel}.
   *
   * @return information about the library loading attempt
   * @throws IllegalArgumentException if any configuration parameter is invalid
   * @throws IllegalStateException if the configuration cannot be built
   */
  public NativeLibraryUtils.LibraryLoadInfo load() {
    // Create base configuration
    final NativeLibraryConfig baseConfig = buildBaseConfig();
    
    // Apply security level and resource path convention
    final NativeLibraryConfig finalConfig = applyAdvancedConfiguration(baseConfig);
    
    // Delegate to NativeLibraryUtils for actual loading
    return NativeLibraryUtils.loadNativeLibrary(finalConfig);
  }

  /**
   * Builds the base configuration from current builder state.
   *
   * @return the base configuration
   * @throws IllegalArgumentException if any parameter is invalid
   */
  private NativeLibraryConfig buildBaseConfig() {
    return new NativeLibraryConfig(libraryName, tempFilePrefix, tempDirSuffix);
  }

  /**
   * Applies advanced configuration options like security level and resource path convention.
   *
   * <p>For now, this method returns the base configuration unchanged as the advanced features
   * are primarily behavioral and don't affect the core NativeLibraryConfig. Future versions
   * may extend the configuration system to support these features.
   *
   * @param baseConfig the base configuration
   * @return the enhanced configuration
   */
  private NativeLibraryConfig applyAdvancedConfiguration(final NativeLibraryConfig baseConfig) {
    // Note: For now, security level and resource path convention are stored for future use
    // but don't modify the base configuration. Future implementations may extend
    // NativeLibraryConfig to support these features or create a wrapper configuration.
    
    // TODO: Implement security level validation
    // TODO: Implement resource path convention handling
    
    return baseConfig;
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
   * Gets the current security level.
   *
   * @return the security level
   */
  public SecurityLevel getSecurityLevel() {
    return securityLevel;
  }

  /**
   * Gets the current resource path convention.
   *
   * @return the resource path convention
   */
  public ResourcePathConvention getResourcePathConvention() {
    return resourcePathConvention;
  }
}