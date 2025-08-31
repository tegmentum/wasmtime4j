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

package ai.tegmentum.wasmtime4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared utilities for native library loading across JNI and Panama implementations.
 *
 * <p>This class provides common functionality for:
 *
 * <ul>
 *   <li>Platform detection and resource path generation
 *   <li>Native library extraction from JAR resources
 *   <li>Temporary file management and cleanup
 *   <li>Error handling and diagnostics
 * </ul>
 *
 * <p>The utilities ensure consistent behavior between JNI and Panama implementations while
 * providing defensive programming practices to prevent JVM crashes.
 */
public final class NativeLibraryUtils {

  private static final Logger LOGGER = Logger.getLogger(NativeLibraryUtils.class.getName());

  /** The base name of the native library. */
  private static final String LIBRARY_NAME = "wasmtime4j";

  /** Prefix for temporary files. */
  private static final String TEMP_FILE_PREFIX = "wasmtime4j-native-";

  /** Suffix for temporary directories. */
  private static final String TEMP_DIR_SUFFIX = "-wasmtime4j";

  /** Cache for extracted library paths to avoid multiple extractions. */
  private static final ConcurrentHashMap<String, Path> extractedLibrariesCache =
      new ConcurrentHashMap<>();

  /** Reference to the cleanup thread for proper shutdown handling. */
  private static final AtomicReference<Thread> cleanupThreadRef = new AtomicReference<>();

  /** Information about a native library loading attempt. */
  public static final class LibraryLoadInfo {
    private final String libraryName;
    private final PlatformDetector.PlatformInfo platformInfo;
    private final String resourcePath;
    private final boolean foundInResources;
    private final Path extractedPath;
    private final LoadingMethod loadingMethod;
    private final Exception error;

    /** The method used to load the library. */
    public enum LoadingMethod {
      SYSTEM_LIBRARY_PATH,
      EXTRACTED_FROM_JAR
    }

    LibraryLoadInfo(
        final String libraryName,
        final PlatformDetector.PlatformInfo platformInfo,
        final String resourcePath,
        final boolean foundInResources,
        final Path extractedPath,
        final LoadingMethod loadingMethod,
        final Exception error) {
      this.libraryName = libraryName;
      this.platformInfo = platformInfo;
      this.resourcePath = resourcePath;
      this.foundInResources = foundInResources;
      this.extractedPath = extractedPath;
      this.loadingMethod = loadingMethod;
      this.error = error;
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
     * Gets the platform information.
     *
     * @return the platform info
     */
    public PlatformDetector.PlatformInfo getPlatformInfo() {
      return platformInfo;
    }

    /**
     * Gets the resource path that was used.
     *
     * @return the resource path
     */
    public String getResourcePath() {
      return resourcePath;
    }

    /**
     * Checks if the library was found in resources.
     *
     * @return true if found in resources
     */
    public boolean isFoundInResources() {
      return foundInResources;
    }

    /**
     * Gets the path where the library was extracted (if applicable).
     *
     * @return the extracted path, or null if not extracted
     */
    public Path getExtractedPath() {
      return extractedPath;
    }

    /**
     * Gets the loading method that was successful.
     *
     * @return the loading method, or null if loading failed
     */
    public LoadingMethod getLoadingMethod() {
      return loadingMethod;
    }

    /**
     * Gets the error that occurred during loading (if any).
     *
     * @return the error, or null if no error
     * @deprecated Use {@link #getErrorMessage()} or {@link #getErrorType()} for safer access
     */
    @Deprecated
    public Exception getError() {
      return error;
    }

    /**
     * Gets the error message from the exception that occurred during loading.
     *
     * @return the error message, or null if no error
     */
    public String getErrorMessage() {
      return error != null ? error.getMessage() : null;
    }

    /**
     * Gets the type of error that occurred during loading.
     *
     * @return the error type name, or null if no error
     */
    public String getErrorType() {
      return error != null ? error.getClass().getSimpleName() : null;
    }

    /**
     * Checks if the loading was successful.
     *
     * @return true if successful
     */
    public boolean isSuccessful() {
      return error == null && loadingMethod != null;
    }

    @Override
    public String toString() {
      if (isSuccessful()) {
        return String.format(
            "LibraryLoadInfo{platform=%s, method=%s, path=%s}",
            platformInfo.getPlatformId(),
            loadingMethod,
            extractedPath != null ? extractedPath : "system");
      } else {
        return String.format(
            "LibraryLoadInfo{platform=%s, error=%s}",
            platformInfo.getPlatformId(),
            getErrorMessage() != null ? getErrorMessage() : "unknown");
      }
    }
  }

  /** Private constructor to prevent instantiation of utility class. */
  private NativeLibraryUtils() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Attempts to load the wasmtime4j native library using multiple strategies.
   *
   * <p>This method tries the following loading strategies in order:
   *
   * <ol>
   *   <li>Load from system library path using {@code System.loadLibrary()}
   *   <li>Extract from JAR resources and load from temporary location
   * </ol>
   *
   * @return information about the loading attempt
   */
  public static LibraryLoadInfo loadNativeLibrary() {
    return loadNativeLibrary(LIBRARY_NAME);
  }

  /**
   * Attempts to load a native library using multiple strategies.
   *
   * @param libraryName the base name of the library to load
   * @return information about the loading attempt
   */
  public static LibraryLoadInfo loadNativeLibrary(final String libraryName) {
    Objects.requireNonNull(libraryName, "libraryName must not be null");

    final PlatformDetector.PlatformInfo platformInfo;
    try {
      platformInfo = PlatformDetector.detect();
    } catch (final RuntimeException e) {
      LOGGER.log(Level.SEVERE, "Failed to detect platform", e);
      return new LibraryLoadInfo(libraryName, null, null, false, null, null, e);
    }

    final String resourcePath = platformInfo.getLibraryResourcePath(libraryName);

    // Strategy 1: Try loading from system library path
    try {
      System.loadLibrary(libraryName);
      LOGGER.info(
          "Successfully loaded native library from system library path: "
              + sanitizeForLog(libraryName));
      return new LibraryLoadInfo(
          libraryName,
          platformInfo,
          resourcePath,
          false,
          null,
          LibraryLoadInfo.LoadingMethod.SYSTEM_LIBRARY_PATH,
          null);
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.fine("Failed to load from system library path: " + sanitizeForLog(e.getMessage()));
    }

    // Strategy 2: Extract from JAR resources and load
    try {
      final Path extractedPath = extractLibraryFromJar(libraryName, platformInfo, resourcePath);
      System.load(extractedPath.toAbsolutePath().toString());
      LOGGER.info(
          "Successfully loaded native library from JAR: "
              + sanitizeForLog(resourcePath)
              + " -> "
              + sanitizeForLog(extractedPath.toString()));
      return new LibraryLoadInfo(
          libraryName,
          platformInfo,
          resourcePath,
          true,
          extractedPath,
          LibraryLoadInfo.LoadingMethod.EXTRACTED_FROM_JAR,
          null);
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to load native library from JAR", e);
      return new LibraryLoadInfo(
          libraryName,
          platformInfo,
          resourcePath,
          checkResourceExists(resourcePath),
          null,
          null,
          e);
    }
  }

  /**
   * Extracts a native library from JAR resources to a temporary location.
   *
   * @param libraryName the library name
   * @param platformInfo the platform information
   * @param resourcePath the resource path
   * @return the path to the extracted library
   * @throws IOException if extraction fails
   */
  public static Path extractLibraryFromJar(
      final String libraryName,
      final PlatformDetector.PlatformInfo platformInfo,
      final String resourcePath)
      throws IOException {
    Objects.requireNonNull(libraryName, "libraryName must not be null");
    Objects.requireNonNull(platformInfo, "platformInfo must not be null");
    Objects.requireNonNull(resourcePath, "resourcePath must not be null");

    // Check cache first to avoid duplicate extractions
    final String cacheKey = platformInfo.getPlatformId() + ":" + libraryName;
    final Path cachedPath = extractedLibrariesCache.get(cacheKey);
    if (cachedPath != null && Files.exists(cachedPath)) {
      LOGGER.fine("Using cached extracted library: " + sanitizeForLog(cachedPath.toString()));
      return cachedPath;
    }

    // Extract the library
    try (final InputStream inputStream =
        NativeLibraryUtils.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IOException("Native library not found in JAR resources: " + resourcePath);
      }

      // Sanitize platform ID to prevent path traversal attacks
      final String sanitizedPlatformId = sanitizePlatformId(platformInfo.getPlatformId());

      // Create temporary directory with unique name
      final Path tempDir =
          Files.createTempDirectory(TEMP_FILE_PREFIX + sanitizedPlatformId + TEMP_DIR_SUFFIX);
      final String libraryFileName = platformInfo.getLibraryFileName(libraryName);
      final Path extractedLibrary = tempDir.resolve(libraryFileName);

      // Copy library to temporary location with atomic operation
      Files.copy(inputStream, extractedLibrary, StandardCopyOption.REPLACE_EXISTING);

      // Set appropriate permissions
      setLibraryPermissions(extractedLibrary, platformInfo);

      // Verify the extracted file
      if (!Files.exists(extractedLibrary) || Files.size(extractedLibrary) == 0) {
        throw new IOException("Extracted library file is invalid: " + extractedLibrary);
      }

      // Register for cleanup
      registerForCleanup(tempDir);
      registerForCleanup(extractedLibrary);

      // Cache the path
      extractedLibrariesCache.put(cacheKey, extractedLibrary);

      LOGGER.fine(
          "Extracted native library: "
              + sanitizeForLog(resourcePath)
              + " -> "
              + sanitizeForLog(extractedLibrary.toString()));
      return extractedLibrary;
    }
  }

  /**
   * Checks if a resource exists in the JAR.
   *
   * @param resourcePath the resource path
   * @return true if the resource exists
   */
  public static boolean checkResourceExists(final String resourcePath) {
    if (resourcePath == null) {
      return false;
    }
    try (final InputStream stream = NativeLibraryUtils.class.getResourceAsStream(resourcePath)) {
      return stream != null;
    } catch (final IOException e) {
      return false;
    }
  }

  /**
   * Gets diagnostic information about native library loading.
   *
   * @return diagnostic information string
   */
  public static String getDiagnosticInfo() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Native Library Diagnostics:\n");
    sb.append("  Platform: ").append(PlatformDetector.getPlatformDescription()).append("\n");
    sb.append("  Library path: ").append(System.getProperty("java.library.path")).append("\n");

    try {
      final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
      final String resourcePath = info.getLibraryResourcePath(LIBRARY_NAME);
      sb.append("  Expected resource: ").append(resourcePath).append("\n");
      sb.append("  Resource exists: ").append(checkResourceExists(resourcePath)).append("\n");
    } catch (final RuntimeException e) {
      sb.append("  Platform detection error: ").append(e.getMessage()).append("\n");
    }

    sb.append("  Cached extractions: ").append(extractedLibrariesCache.size()).append("\n");

    return sb.toString();
  }

  /**
   * Sanitizes a string for safe logging by removing CRLF injection characters.
   *
   * <p>This method removes carriage return and line feed characters that could be used for log
   * injection attacks.
   *
   * @param input the string to sanitize for logging
   * @return the sanitized string safe for logging
   */
  private static String sanitizeForLog(final String input) {
    if (input == null) {
      return "null";
    }
    return input.replaceAll("[\r\n]", "_");
  }

  /**
   * Sanitizes a platform ID string to prevent path traversal attacks.
   *
   * <p>This method removes any characters that could be used for directory traversal and validates
   * that the result contains only safe characters.
   *
   * @param platformId the platform ID to sanitize
   * @return the sanitized platform ID
   * @throws IllegalArgumentException if the platform ID cannot be safely sanitized
   */
  private static String sanitizePlatformId(final String platformId) {
    if (platformId == null || platformId.trim().isEmpty()) {
      throw new IllegalArgumentException("Platform ID cannot be null or empty");
    }

    // Remove any path traversal sequences and unsafe characters
    final String sanitized =
        platformId
            .replaceAll("\\.\\.", "") // Remove .. sequences
            .replaceAll("[\\\\/:]", "-") // Replace path separators and colons with dashes
            .replaceAll("[^a-zA-Z0-9\\-_]", ""); // Keep only alphanumeric, dashes, and underscores

    if (sanitized.isEmpty()) {
      throw new IllegalArgumentException("Platform ID contains no valid characters: " + platformId);
    }

    return sanitized;
  }

  /**
   * Sets appropriate permissions on the extracted library file.
   *
   * @param libraryPath the path to the library
   * @param platformInfo the platform information
   */
  private static void setLibraryPermissions(
      final Path libraryPath, final PlatformDetector.PlatformInfo platformInfo) {
    try {
      // On Unix-like systems, make the library executable
      if (platformInfo.getOperatingSystem() != PlatformDetector.OperatingSystem.WINDOWS) {
        libraryPath.toFile().setExecutable(true, false);
        libraryPath.toFile().setReadable(true, false);
      }
    } catch (final Exception e) {
      LOGGER.log(
          Level.WARNING,
          "Failed to set library permissions: " + sanitizeForLog(libraryPath.toString()),
          e);
    }
  }

  /**
   * Registers a path for cleanup on JVM shutdown.
   *
   * @param path the path to clean up
   */
  private static void registerForCleanup(final Path path) {
    // Use deleteOnExit as the primary cleanup mechanism
    path.toFile().deleteOnExit();

    // Also register a shutdown hook for more thorough cleanup
    if (cleanupThreadRef.get() == null) {
      final Thread cleanupThread =
          new Thread(
              () -> {
                try {
                  // Clean up extracted libraries cache
                  for (final Path extractedPath : extractedLibrariesCache.values()) {
                    try {
                      if (Files.exists(extractedPath)) {
                        Files.deleteIfExists(extractedPath);
                        // Also try to delete parent directory if empty
                        final Path parent = extractedPath.getParent();
                        if (parent != null && Files.exists(parent)) {
                          try {
                            Files.deleteIfExists(parent);
                          } catch (final Exception e) {
                            // Expected - directory might not be empty or have permission issues
                            LOGGER.log(
                                Level.FINE,
                                "Could not delete parent directory (expected if not empty): "
                                    + sanitizeForLog(parent.toString()),
                                e);
                          }
                        }
                      }
                    } catch (final Exception e) {
                      LOGGER.log(
                          Level.FINE,
                          "Error during cleanup of extracted library: "
                              + sanitizeForLog(extractedPath.toString()),
                          e);
                    }
                  }
                  extractedLibrariesCache.clear();
                  LOGGER.fine("Completed native library cleanup");
                } catch (final Exception e) {
                  LOGGER.log(Level.WARNING, "Error during native library cleanup", e);
                }
              },
              "wasmtime4j-cleanup");

      cleanupThread.setDaemon(true);

      if (cleanupThreadRef.compareAndSet(null, cleanupThread)) {
        Runtime.getRuntime().addShutdownHook(cleanupThread);
        LOGGER.fine("Registered native library cleanup shutdown hook");
      }
    }
  }
}
