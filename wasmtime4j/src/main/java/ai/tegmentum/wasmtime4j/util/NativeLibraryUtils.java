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

package ai.tegmentum.wasmtime4j.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Backward-compatible wrapper for native library loading utilities.
 *
 * <p>This class provides the same API as the original NativeLibraryUtils but delegates to the
 * refactored implementation in the wasmtime4j-native-loader module. This maintains backward
 * compatibility for existing code while enabling the new configurable functionality.
 *
 * @deprecated This class is maintained for backward compatibility. New code should use {@link
 *     ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils} directly for access to configuration
 *     options.
 */
@Deprecated
public final class NativeLibraryUtils {

  /** Private constructor to prevent instantiation of utility class. */
  private NativeLibraryUtils() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /** Information about a native library loading attempt. */
  public static final class LibraryLoadInfo {
    private final ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.LibraryLoadInfo delegate;

    private LibraryLoadInfo(
        final ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.LibraryLoadInfo delegate) {
      this.delegate = delegate;
    }

    /** The method used to load the library. */
    public enum LoadingMethod {
      SYSTEM_LIBRARY_PATH,
      EXTRACTED_FROM_JAR
    }

    /**
     * Gets the library name.
     *
     * @return the library name
     */
    public String getLibraryName() {
      return delegate.getLibraryName();
    }

    /**
     * Gets the platform information.
     *
     * @return the platform info
     */
    public PlatformDetector.PlatformInfo getPlatformInfo() {
      return new PlatformDetector.PlatformInfo(delegate.getPlatformInfo());
    }

    /**
     * Gets the resource path that was used.
     *
     * @return the resource path
     */
    public String getResourcePath() {
      return delegate.getResourcePath();
    }

    /**
     * Checks if the library was found in resources.
     *
     * @return true if found in resources
     */
    public boolean isFoundInResources() {
      return delegate.isFoundInResources();
    }

    /**
     * Gets the path where the library was extracted (if applicable).
     *
     * @return the extracted path, or null if not extracted
     */
    public Path getExtractedPath() {
      return delegate.getExtractedPath();
    }

    /**
     * Gets the loading method that was successful.
     *
     * @return the loading method, or null if loading failed
     */
    public LoadingMethod getLoadingMethod() {
      final ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.LibraryLoadInfo.LoadingMethod
          delegateMethod = delegate.getLoadingMethod();
      if (delegateMethod == null) {
        return null;
      }
      switch (delegateMethod) {
        case SYSTEM_LIBRARY_PATH:
          return LoadingMethod.SYSTEM_LIBRARY_PATH;
        case EXTRACTED_FROM_JAR:
          return LoadingMethod.EXTRACTED_FROM_JAR;
        default:
          throw new IllegalStateException("Unknown loading method: " + delegateMethod);
      }
    }

    /**
     * Gets the error message from the exception that occurred during loading.
     *
     * @return the error message, or null if no error
     */
    public String getErrorMessage() {
      return delegate.getErrorMessage();
    }

    /**
     * Gets the type of error that occurred during loading.
     *
     * @return the error type name, or null if no error
     */
    public String getErrorType() {
      return delegate.getErrorType();
    }

    /**
     * Checks if the loading was successful.
     *
     * @return true if successful
     */
    public boolean isSuccessful() {
      return delegate.isSuccessful();
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
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
    return new LibraryLoadInfo(
        ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.loadNativeLibrary());
  }

  /**
   * Attempts to load a native library using multiple strategies.
   *
   * @param libraryName the base name of the library to load
   * @return information about the loading attempt
   */
  public static LibraryLoadInfo loadNativeLibrary(final String libraryName) {
    return new LibraryLoadInfo(
        ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.loadNativeLibrary(libraryName));
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
    // Validate null parameters to maintain backward compatibility
    if (libraryName == null) {
      throw new NullPointerException("libraryName cannot be null");
    }
    if (platformInfo == null) {
      throw new NullPointerException("platformInfo cannot be null");
    }
    if (resourcePath == null) {
      throw new NullPointerException("resourcePath cannot be null");
    }

    // Convert the wrapper PlatformInfo to native PlatformInfo
    final ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.PlatformInfo nativePlatformInfo =
        ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.detect();
    return ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.extractLibraryFromJar(
        libraryName, nativePlatformInfo, resourcePath);
  }

  /**
   * Checks if a resource exists in the JAR.
   *
   * @param resourcePath the resource path
   * @return true if the resource exists
   */
  public static boolean checkResourceExists(final String resourcePath) {
    return ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.checkResourceExists(
        resourcePath);
  }

  /**
   * Gets diagnostic information about native library loading.
   *
   * @return diagnostic information string
   */
  public static String getDiagnosticInfo() {
    return ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.getDiagnosticInfo();
  }
}
