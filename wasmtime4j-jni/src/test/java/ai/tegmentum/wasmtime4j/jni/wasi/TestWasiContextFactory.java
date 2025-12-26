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

package ai.tegmentum.wasmtime4j.jni.wasi;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Factory for creating test-friendly WasiContext instances.
 *
 * <p>This factory uses reflection to create WasiContext instances with test native handles,
 * avoiding the need for mocking while still allowing unit tests to run without native libraries.
 *
 * <p>Note: Test contexts created by this factory should not be used for actual native operations.
 * They are only suitable for testing Java-side logic that requires a WasiContext instance.
 */
public final class TestWasiContextFactory {

  /** Default test native handle value. */
  public static final long TEST_NATIVE_HANDLE = 1L;

  private TestWasiContextFactory() {
    // Utility class
  }

  /**
   * Creates a test WasiContext with a default test native handle.
   *
   * @return a test WasiContext instance
   */
  public static WasiContext createTestContext() {
    return createTestContext(TEST_NATIVE_HANDLE);
  }

  /**
   * Creates a test WasiContext with the specified native handle.
   *
   * @param nativeHandle the native handle value to use
   * @return a test WasiContext instance
   */
  public static WasiContext createTestContext(final long nativeHandle) {
    try {
      // Create a minimal builder for test context
      final WasiContextBuilder builder = createTestBuilder();

      // Use reflection to access the package-private constructor
      final Constructor<WasiContext> constructor =
          WasiContext.class.getDeclaredConstructor(long.class, WasiContextBuilder.class);
      constructor.setAccessible(true);

      return constructor.newInstance(nativeHandle, builder);
    } catch (final ReflectiveOperationException e) {
      throw new RuntimeException("Failed to create test WasiContext", e);
    }
  }

  /**
   * Creates a test WasiContext with a custom native handle and preopened directories.
   *
   * @param nativeHandle the native handle value to use
   * @param preopenedDirectories pre-opened directories for the context
   * @return a test WasiContext instance with the specified configuration
   */
  public static WasiContext createTestContext(
      final long nativeHandle,
      final Map<String, Path> preopenedDirectories) {
    try {
      final WasiContextBuilder builder = createTestBuilder(
          Paths.get(System.getProperty("java.io.tmpdir")),
          null,
          preopenedDirectories);

      final Constructor<WasiContext> constructor =
          WasiContext.class.getDeclaredConstructor(long.class, WasiContextBuilder.class);
      constructor.setAccessible(true);

      return constructor.newInstance(nativeHandle, builder);
    } catch (final ReflectiveOperationException e) {
      throw new RuntimeException("Failed to create test WasiContext", e);
    }
  }

  /**
   * Creates a test WasiContext with a custom working directory.
   *
   * <p>This is useful for tests that need to resolve paths relative to a specific directory,
   * such as a temp directory.
   *
   * @param workingDirectory the working directory for path resolution
   * @return a test WasiContext instance
   */
  public static WasiContext createTestContextWithWorkingDir(final Path workingDirectory) {
    return createTestContextWithWorkingDir(workingDirectory, null, null);
  }

  /**
   * Creates a test WasiContext with a custom working directory and environment.
   *
   * @param workingDirectory the working directory for path resolution
   * @param environment optional environment variables
   * @param arguments optional command-line arguments
   * @return a test WasiContext instance
   */
  public static WasiContext createTestContextWithWorkingDir(
      final Path workingDirectory,
      final Map<String, String> environment,
      final String[] arguments) {
    try {
      final WasiContextBuilder builder = new WasiContextBuilder()
          .withWorkingDirectory(workingDirectory.toString())
          .withPreopenDirectory(".", workingDirectory.toString());

      if (environment != null) {
        environment.forEach(builder::withEnvironment);
      }

      if (arguments != null) {
        builder.withArguments(arguments);
      }

      final Constructor<WasiContext> constructor =
          WasiContext.class.getDeclaredConstructor(long.class, WasiContextBuilder.class);
      constructor.setAccessible(true);

      return constructor.newInstance(TEST_NATIVE_HANDLE, builder);
    } catch (final ReflectiveOperationException e) {
      throw new RuntimeException("Failed to create test WasiContext", e);
    }
  }

  /**
   * Creates a test WasiContextBuilder with minimal configuration.
   *
   * @return a configured WasiContextBuilder for testing
   */
  public static WasiContextBuilder createTestBuilder() {
    return new WasiContextBuilder()
        .withWorkingDirectory(System.getProperty("java.io.tmpdir"));
  }

  /**
   * Creates a test WasiContextBuilder with custom configuration.
   *
   * @param workingDirectory the working directory for the context
   * @param environment environment variables
   * @param preopenedDirectories pre-opened directories
   * @return a configured WasiContextBuilder for testing
   */
  public static WasiContextBuilder createTestBuilder(
      final Path workingDirectory,
      final Map<String, String> environment,
      final Map<String, Path> preopenedDirectories) {
    final WasiContextBuilder builder = new WasiContextBuilder()
        .withWorkingDirectory(workingDirectory.toString());

    if (environment != null) {
      environment.forEach(builder::withEnvironment);
    }

    if (preopenedDirectories != null) {
      preopenedDirectories.forEach((name, path) -> builder.withPreopenDirectory(name, path.toString()));
    }

    return builder;
  }
}
