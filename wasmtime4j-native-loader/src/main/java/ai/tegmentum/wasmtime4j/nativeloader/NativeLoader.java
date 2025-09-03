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

/**
 * Main entry point for native library loading with a fluent builder API.
 *
 * <p>This class provides both static convenience methods for simple use cases and a builder pattern
 * for advanced configuration. It serves as a high-level facade over the underlying {@link
 * NativeLibraryUtils} while providing additional features like security levels and resource path
 * conventions.
 *
 * <p><strong>Simple Usage:</strong>
 *
 * <pre>{@code
 * // Load with default configuration
 * LibraryLoadInfo info = NativeLoader.loadLibrary("wasmtime4j");
 *
 * // Load with custom library name
 * LibraryLoadInfo info = NativeLoader.loadLibrary("mylib");
 * }</pre>
 *
 * <p><strong>Advanced Usage with Builder:</strong>
 *
 * <pre>{@code
 * LibraryLoadInfo info = NativeLoader.builder()
 *     .libraryName("mylib")
 *     .tempFilePrefix("mylib-native-")
 *     .securityLevel(SecurityLevel.STRICT)
 *     .resourcePathConvention(ResourcePathConvention.MAVEN_NATIVE)
 *     .load();
 * }</pre>
 *
 * <p>All operations are thread-safe and configurations are immutable.
 *
 * @since 1.0.0
 */
public final class NativeLoader {

  /** Private constructor to prevent instantiation of utility class. */
  private NativeLoader() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Loads a native library using default configuration.
   *
   * <p>This is equivalent to calling {@code builder().libraryName(libraryName).load()}.
   *
   * @param libraryName the base name of the library to load
   * @return information about the loading attempt
   * @throws IllegalArgumentException if libraryName is null
   */
  public static NativeLibraryUtils.LibraryLoadInfo loadLibrary(final String libraryName) {
    return builder().libraryName(libraryName).load();
  }

  /**
   * Creates a new builder for configuring native library loading.
   *
   * <p>The builder starts with default values suitable for most use cases. All configuration is
   * validated when {@link NativeLoaderBuilder#load()} is called.
   *
   * @return a new builder instance
   */
  public static NativeLoaderBuilder builder() {
    return new NativeLoaderBuilder();
  }
}
