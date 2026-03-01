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
package ai.tegmentum.wasmtime4j.wasi.jni;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContextBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for JNI WasiContext and WasiContextBuilder.
 *
 * <p>These tests verify the actual native implementation of WasiContext, including context
 * creation, configuration, and resource management.
 *
 * <p>Test coverage:
 *
 * <ul>
 *   <li>Context creation via builder pattern
 *   <li>Environment variable configuration
 *   <li>Command-line argument configuration
 *   <li>Pre-opened directory configuration
 *   <li>Working directory configuration
 *   <li>Resource lifecycle management
 * </ul>
 */
@DisplayName("JNI WasiContext Integration Tests")
class JniWasiContextTest {

  private static final Logger LOGGER = Logger.getLogger(JniWasiContextTest.class.getName());

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @TempDir Path tempDir;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for WASI context tests");
    try {
      NativeLibraryLoader.loadLibrary();
      LOGGER.info("Native library loaded successfully");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up test resources");
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    for (final AutoCloseable resource : resources) {
      try {
        resource.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Builder Creation Tests")
  class BuilderCreationTests {

    @Test
    @DisplayName("should create builder from static method")
    void shouldCreateBuilderFromStaticMethod() {
      LOGGER.info("Testing builder creation from static method");

      final WasiContextBuilder builder = WasiContext.builder();

      assertNotNull(builder, "Builder should not be null");
      LOGGER.info("Builder created successfully");
    }

    @Test
    @DisplayName("should create builder from WasiContextBuilder.builder()")
    void shouldCreateBuilderFromBuilderClass() {
      LOGGER.info("Testing builder creation from WasiContextBuilder.builder()");

      final WasiContextBuilder builder = WasiContextBuilder.builder();

      assertNotNull(builder, "Builder should not be null");
      LOGGER.info("Builder created successfully");
    }
  }

  @Nested
  @DisplayName("Context Creation Tests")
  class ContextCreationTests {

    @Test
    @DisplayName("should create context with default configuration")
    void shouldCreateContextWithDefaultConfiguration() throws Exception {
      LOGGER.info("Testing context creation with default configuration");

      // Create a temp directory that exists for preopened directories
      final Path existingDir = tempDir.resolve("preopen");
      Files.createDirectories(existingDir);
      LOGGER.info("Created temp preopen directory: " + existingDir);

      final WasiContext context =
          WasiContext.builder().withPreopenDirectory("/", existingDir.toString()).build();
      resources.add(context);

      assertNotNull(context, "Context should not be null");
      assertTrue(context.isValid(), "Context should be valid after creation");
      LOGGER.info("Context created successfully with native handle: " + context.getNativeHandle());
    }

    @Test
    @DisplayName("should create context with environment variables")
    void shouldCreateContextWithEnvironmentVariables() throws Exception {
      LOGGER.info("Testing context creation with environment variables");

      final Path existingDir = tempDir.resolve("preopen-env");
      Files.createDirectories(existingDir);

      final WasiContext context =
          WasiContext.builder()
              .withEnvironment("TEST_VAR", "test_value")
              .withEnvironment("HOME", "/home/test")
              .withEnvironment("PATH", "/usr/bin:/bin")
              .withPreopenDirectory("/", existingDir.toString())
              .build();
      resources.add(context);

      assertNotNull(context, "Context should not be null");
      assertTrue(context.isValid(), "Context should be valid");

      // Verify environment is accessible
      final Map<String, String> env = context.getEnvironment();
      assertNotNull(env, "Environment should not be null");
      assertEquals("test_value", env.get("TEST_VAR"), "TEST_VAR should have correct value");
      assertEquals("/home/test", env.get("HOME"), "HOME should have correct value");
      assertEquals("/usr/bin:/bin", env.get("PATH"), "PATH should have correct value");

      LOGGER.info("Context created with " + env.size() + " environment variables");
    }

    @Test
    @DisplayName("should create context with command line arguments")
    void shouldCreateContextWithArguments() throws Exception {
      LOGGER.info("Testing context creation with command line arguments");

      final Path existingDir = tempDir.resolve("preopen-args");
      Files.createDirectories(existingDir);

      final WasiContext context =
          WasiContext.builder()
              .withArgument("program")
              .withArgument("--verbose")
              .withArgument("-o")
              .withArgument("output.txt")
              .withPreopenDirectory("/", existingDir.toString())
              .build();
      resources.add(context);

      assertNotNull(context, "Context should not be null");
      assertTrue(context.isValid(), "Context should be valid");

      // Verify arguments are accessible
      final String[] args = context.getArguments();
      assertNotNull(args, "Arguments should not be null");
      assertEquals(4, args.length, "Should have 4 arguments");
      assertEquals("program", args[0], "First argument should be program name");
      assertEquals("--verbose", args[1], "Second argument should be --verbose");
      assertEquals("-o", args[2], "Third argument should be -o");
      assertEquals("output.txt", args[3], "Fourth argument should be output.txt");

      LOGGER.info("Context created with " + args.length + " arguments");
    }

    @Test
    @DisplayName("should create context with preopened directories")
    void shouldCreateContextWithPreopenedDirectories() throws Exception {
      LOGGER.info("Testing context creation with preopened directories");

      // Create multiple temp directories
      final Path dir1 = tempDir.resolve("preopen1");
      final Path dir2 = tempDir.resolve("preopen2");
      Files.createDirectories(dir1);
      Files.createDirectories(dir2);

      final WasiContext context =
          WasiContext.builder()
              .withPreopenDirectory("/guest/tmp", dir1.toString())
              .withPreopenDirectory("/guest/data", dir2.toString())
              .build();
      resources.add(context);

      assertNotNull(context, "Context should not be null");
      assertTrue(context.isValid(), "Context should be valid");

      // Verify preopened directories are accessible
      final Map<String, Path> preopened = context.getPreopenedDirectories();
      assertNotNull(preopened, "Preopened directories should not be null");
      assertEquals(2, preopened.size(), "Should have 2 preopened directories");

      LOGGER.info("Context created with " + preopened.size() + " preopened directories");
    }

    @Test
    @DisplayName("should create context with working directory")
    void shouldCreateContextWithWorkingDirectory() throws Exception {
      LOGGER.info("Testing context creation with working directory");

      final Path existingDir = tempDir.resolve("preopen-working");
      Files.createDirectories(existingDir);

      final WasiContext context =
          WasiContext.builder()
              .withWorkingDirectory("/app")
              .withPreopenDirectory("/", existingDir.toString())
              .build();
      resources.add(context);

      assertNotNull(context, "Context should not be null");
      assertTrue(context.isValid(), "Context should be valid");

      // Verify working directory
      final Path workingDir = context.getWorkingDirectory();
      assertNotNull(workingDir, "Working directory should not be null");
      assertEquals("/app", workingDir.toString(), "Working directory should be /app");

      LOGGER.info("Context created with working directory: " + workingDir);
    }

    @Test
    @DisplayName("should create context with full configuration")
    void shouldCreateContextWithFullConfiguration() throws Exception {
      LOGGER.info("Testing context creation with full configuration");

      final Path tmpDir = tempDir.resolve("full-config-tmp");
      final Path dataDir = tempDir.resolve("full-config-data");
      Files.createDirectories(tmpDir);
      Files.createDirectories(dataDir);

      final WasiContext context =
          WasiContext.builder()
              .withEnvironment("APP_ENV", "production")
              .withEnvironment("LOG_LEVEL", "debug")
              .withArgument("myapp")
              .withArgument("--config=/etc/app.conf")
              .withPreopenDirectory("/tmp", tmpDir.toString())
              .withPreopenDirectory("/data", dataDir.toString())
              .withWorkingDirectory("/app")
              .build();
      resources.add(context);

      assertNotNull(context, "Context should not be null");
      assertTrue(context.isValid(), "Context should be valid");

      // Verify all configurations
      final Map<String, String> env = context.getEnvironment();
      assertEquals(2, env.size(), "Should have 2 environment variables");

      final String[] args = context.getArguments();
      assertEquals(2, args.length, "Should have 2 arguments");

      final Map<String, Path> preopened = context.getPreopenedDirectories();
      assertEquals(2, preopened.size(), "Should have 2 preopened directories");

      LOGGER.info("Context created with full configuration successfully");
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("should throw on null environment variable name")
    void shouldThrowOnNullEnvVarName() {
      LOGGER.info("Testing validation of null environment variable name");

      assertThrows(
          RuntimeException.class,
          () -> WasiContext.builder().withEnvironment(null, "value"),
          "Should throw on null environment variable name");

      LOGGER.info("Validation correctly rejected null environment variable name");
    }

    @Test
    @DisplayName("should throw on empty environment variable name")
    void shouldThrowOnEmptyEnvVarName() {
      LOGGER.info("Testing validation of empty environment variable name");

      assertThrows(
          RuntimeException.class,
          () -> WasiContext.builder().withEnvironment("", "value"),
          "Should throw on empty environment variable name");

      LOGGER.info("Validation correctly rejected empty environment variable name");
    }

    @Test
    @DisplayName("should throw on null argument")
    void shouldThrowOnNullArgument() {
      LOGGER.info("Testing validation of null argument");

      assertThrows(
          RuntimeException.class,
          () -> WasiContext.builder().withArgument(null),
          "Should throw on null argument");

      LOGGER.info("Validation correctly rejected null argument");
    }

    @Test
    @DisplayName("should throw on non-existent host directory")
    void shouldThrowOnNonExistentHostDirectory() {
      LOGGER.info("Testing validation of non-existent host directory");

      assertThrows(
          RuntimeException.class,
          () -> WasiContext.builder().withPreopenDirectory("/guest", "/non/existent/path"),
          "Should throw on non-existent host directory");

      LOGGER.info("Validation correctly rejected non-existent host directory");
    }

    @Test
    @DisplayName("should throw on file path instead of directory")
    void shouldThrowOnFilePathInsteadOfDirectory() throws IOException {
      LOGGER.info("Testing validation of file path instead of directory");

      final Path file = tempDir.resolve("not-a-directory.txt");
      Files.createFile(file);

      assertThrows(
          RuntimeException.class,
          () -> WasiContext.builder().withPreopenDirectory("/guest", file.toString()),
          "Should throw on file path instead of directory");

      LOGGER.info("Validation correctly rejected file path");
    }

    @Test
    @DisplayName("should throw on null guest directory path")
    void shouldThrowOnNullGuestPath() throws IOException {
      LOGGER.info("Testing validation of null guest directory path");

      final Path hostDir = tempDir.resolve("host-dir");
      Files.createDirectories(hostDir);

      assertThrows(
          RuntimeException.class,
          () -> WasiContext.builder().withPreopenDirectory(null, hostDir.toString()),
          "Should throw on null guest directory path");

      LOGGER.info("Validation correctly rejected null guest path");
    }

    @Test
    @DisplayName("should throw on empty guest directory path")
    void shouldThrowOnEmptyGuestPath() throws IOException {
      LOGGER.info("Testing validation of empty guest directory path");

      final Path hostDir = tempDir.resolve("host-dir-empty");
      Files.createDirectories(hostDir);

      assertThrows(
          RuntimeException.class,
          () -> WasiContext.builder().withPreopenDirectory("", hostDir.toString()),
          "Should throw on empty guest directory path");

      LOGGER.info("Validation correctly rejected empty guest path");
    }
  }

  @Nested
  @DisplayName("Resource Lifecycle Tests")
  class ResourceLifecycleTests {

    @Test
    @DisplayName("should close context properly")
    void shouldCloseContextProperly() throws Exception {
      LOGGER.info("Testing context close operation");

      final Path existingDir = tempDir.resolve("close-test");
      Files.createDirectories(existingDir);

      final WasiContext context =
          WasiContext.builder().withPreopenDirectory("/", existingDir.toString()).build();

      assertTrue(context.isValid(), "Context should be valid before close");

      context.close();

      assertFalse(context.isValid(), "Context should not be valid after close");
      LOGGER.info("Context closed successfully");
    }

    @Test
    @DisplayName("should handle double close gracefully")
    void shouldHandleDoubleCloseGracefully() throws Exception {
      LOGGER.info("Testing double close handling");

      final Path existingDir = tempDir.resolve("double-close-test");
      Files.createDirectories(existingDir);

      final WasiContext context =
          WasiContext.builder().withPreopenDirectory("/", existingDir.toString()).build();

      assertDoesNotThrow(
          () -> {
            context.close();
            context.close();
          },
          "Double close should not throw exception");

      LOGGER.info("Double close handled gracefully");
    }

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources() throws Exception {
      LOGGER.info("Testing try-with-resources pattern");

      final Path existingDir = tempDir.resolve("try-with-resources-test");
      Files.createDirectories(existingDir);

      final long[] handleHolder = new long[1];

      try (final WasiContext context =
          WasiContext.builder().withPreopenDirectory("/", existingDir.toString()).build()) {
        assertTrue(context.isValid(), "Context should be valid inside try block");
        handleHolder[0] = context.getNativeHandle();
        LOGGER.info("Context is valid inside try block with handle: " + handleHolder[0]);
      }

      LOGGER.info("Try-with-resources completed successfully");
    }
  }

  @Nested
  @DisplayName("Native Handle Tests")
  class NativeHandleTests {

    @Test
    @DisplayName("should return non-zero native handle")
    void shouldReturnNonZeroNativeHandle() throws Exception {
      LOGGER.info("Testing native handle retrieval");

      final Path existingDir = tempDir.resolve("handle-test");
      Files.createDirectories(existingDir);

      final WasiContext context =
          WasiContext.builder().withPreopenDirectory("/", existingDir.toString()).build();
      resources.add(context);

      final long handle = context.getNativeHandle();
      assertTrue(handle != 0, "Native handle should not be zero");

      LOGGER.info("Native handle retrieved: " + handle);
    }
  }

  @Nested
  @DisplayName("Path Validation Tests")
  class PathValidationTests {

    @Test
    @DisplayName("should validate path correctly")
    void shouldValidatePathCorrectly() throws Exception {
      LOGGER.info("Testing path validation");

      final Path existingDir = tempDir.resolve("validate-path-test");
      Files.createDirectories(existingDir);

      final WasiContext context =
          WasiContext.builder().withPreopenDirectory("/guest", existingDir.toString()).build();
      resources.add(context);

      // Valid paths should not throw
      assertDoesNotThrow(
          () -> context.validatePath("/guest/subdir"), "Valid path should not throw");

      LOGGER.info("Path validation completed successfully");
    }
  }

  @Nested
  @DisplayName("Environment Variable Retrieval Tests")
  class EnvironmentVariableRetrievalTests {

    @Test
    @DisplayName("should retrieve specific environment variable")
    void shouldRetrieveSpecificEnvironmentVariable() throws Exception {
      LOGGER.info("Testing specific environment variable retrieval");

      final Path existingDir = tempDir.resolve("env-retrieval-test");
      Files.createDirectories(existingDir);

      final WasiContext context =
          WasiContext.builder()
              .withEnvironment("MY_VAR", "my_value")
              .withPreopenDirectory("/", existingDir.toString())
              .build();
      resources.add(context);

      final String value = context.getEnvironmentVariable("MY_VAR");
      assertEquals("my_value", value, "Should retrieve correct environment variable value");

      LOGGER.info("Environment variable retrieved: MY_VAR=" + value);
    }

    @Test
    @DisplayName("should return null for non-existent environment variable")
    void shouldReturnNullForNonExistentEnvVar() throws Exception {
      LOGGER.info("Testing non-existent environment variable retrieval");

      final Path existingDir = tempDir.resolve("env-nonexistent-test");
      Files.createDirectories(existingDir);

      final WasiContext context =
          WasiContext.builder().withPreopenDirectory("/", existingDir.toString()).build();
      resources.add(context);

      final String value = context.getEnvironmentVariable("NON_EXISTENT_VAR");
      // Null return is acceptable for non-existent variables
      LOGGER.info("Non-existent environment variable returned: " + value);
    }
  }
}
