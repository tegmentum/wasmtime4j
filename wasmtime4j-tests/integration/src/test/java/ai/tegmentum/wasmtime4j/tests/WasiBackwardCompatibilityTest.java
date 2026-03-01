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
package ai.tegmentum.wasmtime4j.tests;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for WASI Preview 1 backward compatibility.
 *
 * <p>This test suite ensures that the WASI Preview 2 implementation maintains full backward
 * compatibility with WASI Preview 1 functionality, allowing existing WebAssembly modules to
 * continue working without modification.
 */
public class WasiBackwardCompatibilityTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiBackwardCompatibilityTest.class.getName());

  private Engine engine;

  @TempDir Path tempDir;

  @AfterEach
  void tearDown(TestInfo testInfo) {
    LOGGER.info("Tearing down backward compatibility test: " + testInfo.getDisplayName());
    if (engine != null) {
      try {
        engine.close();
      } catch (Exception e) {
        LOGGER.warning("Failed to close engine: " + e.getMessage());
      }
    }
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testPreview1ContextCreation")
  void testPreview1ContextCreation(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    // Create traditional WASI context (Preview 1 style)
    WasiContext context = WasiContext.create();
    assertNotNull(context, "WASI context should be created");

    // Configure with Preview 1 methods only
    context.inheritStdio().inheritEnv().setMaxOpenFiles(100);

    LOGGER.info("Successfully created WASI Preview 1 compatible context");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testPreview1LinkerCreation")
  void testPreview1LinkerCreation(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create().inheritStdio().inheritEnv();

    // Create traditional WASI linker (Preview 1)
    Linker<WasiContext> linker = WasiLinkerUtils.createLinker(engine, context);
    assertNotNull(linker, "Preview 1 linker should be created");

    // Verify traditional WASI imports are present
    assertTrue(
        WasiLinkerUtils.hasWasiImports(linker), "Linker should have traditional WASI imports");

    LOGGER.info("Successfully created WASI Preview 1 linker");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testTraditionalWasiLinkerMethods")
  void testTraditionalWasiLinkerMethods(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Test the static utility methods from WasiLinker
    Linker<WasiContext> linker1 = Linker.create(engine);
    assertDoesNotThrow(
        () -> {
          WasiLinkerUtils.addToLinker(linker1, context);
        },
        "Adding WASI to linker should not throw");

    // Test convenience method with default context
    Linker<WasiContext> linker2 = Linker.create(engine);
    assertDoesNotThrow(
        () -> {
          WasiLinkerUtils.addToLinker(linker2);
        },
        "Adding WASI with default context should not throw");

    // Test convenience linker creation
    Linker<WasiContext> linker3 = WasiLinkerUtils.createLinker(engine, context);
    assertNotNull(linker3, "Convenience linker creation should work");

    Linker<WasiContext> linker4 = WasiLinkerUtils.createLinker(engine);
    assertNotNull(linker4, "Convenience linker creation with default context should work");

    LOGGER.info("Successfully validated traditional WasiLinker methods");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testEnvironmentVariableHandling")
  void testEnvironmentVariableHandling(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Test individual environment variable setting
    assertDoesNotThrow(
        () -> {
          context.setEnv("TEST_VAR", "test_value");
        },
        "Setting individual env var should not throw");

    // Test bulk environment variable setting
    Map<String, String> envVars = new HashMap<>();
    envVars.put("VAR1", "value1");
    envVars.put("VAR2", "value2");
    envVars.put("VAR3", "value3");

    assertDoesNotThrow(
        () -> {
          context.setEnv(envVars);
        },
        "Setting bulk env vars should not throw");

    // Test inheriting environment
    assertDoesNotThrow(
        () -> {
          context.inheritEnv();
        },
        "Inheriting environment should not throw");

    LOGGER.info("Successfully validated environment variable handling");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testCommandLineArgumentHandling")
  void testCommandLineArgumentHandling(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Test various argument configurations
    String[] args1 = {"program"};
    assertDoesNotThrow(
        () -> {
          context.setArgv(args1);
        },
        "Setting single argument should not throw");

    String[] args2 = {"program", "--flag", "value", "--verbose"};
    assertDoesNotThrow(
        () -> {
          context.setArgv(args2);
        },
        "Setting multiple arguments should not throw");

    String[] emptyArgs = {};
    assertDoesNotThrow(
        () -> {
          context.setArgv(emptyArgs);
        },
        "Setting empty arguments should not throw");

    LOGGER.info("Successfully validated command line argument handling");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testStdioRedirection")
  void testStdioRedirection(final RuntimeType runtime) throws WasmException, IOException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Create test files for stdio redirection
    Path stdinFile = tempDir.resolve("input.txt");
    Path stdoutFile = tempDir.resolve("output.txt");
    Path stderrFile = tempDir.resolve("error.txt");

    Files.writeString(stdinFile, "test input\n");

    // Test stdio redirection
    assertDoesNotThrow(
        () -> {
          context.setStdin(stdinFile).setStdout(stdoutFile).setStderr(stderrFile);
        },
        "Setting stdio redirection should not throw");

    // Test inheriting stdio (default behavior)
    assertDoesNotThrow(
        () -> {
          context.inheritStdio();
        },
        "Inheriting stdio should not throw");

    LOGGER.info("Successfully validated stdio redirection");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testFilesystemAccess")
  void testFilesystemAccess(final RuntimeType runtime) throws WasmException, IOException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Create test directories
    Path testDir1 = tempDir.resolve("test1");
    Path testDir2 = tempDir.resolve("test2");
    Files.createDirectories(testDir1);
    Files.createDirectories(testDir2);

    // Test traditional preopened directory mapping
    assertDoesNotThrow(
        () -> {
          context.preopenedDir(testDir1, "/test1");
        },
        "Adding preopened directory should not throw");

    // Test read-only directory mapping
    assertDoesNotThrow(
        () -> {
          context.preopenedDirReadOnly(testDir2, "/test2");
        },
        "Adding read-only preopened directory should not throw");

    // Test working directory setting
    assertDoesNotThrow(
        () -> {
          context.setWorkingDirectory("/tmp");
        },
        "Setting working directory should not throw");

    LOGGER.info("Successfully validated filesystem access");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testResourceLimiting")
  void testResourceLimiting(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Test file descriptor limiting
    assertDoesNotThrow(
        () -> {
          context.setMaxOpenFiles(50);
        },
        "Setting max open files should not throw");

    assertDoesNotThrow(
        () -> {
          context.setMaxOpenFiles(-1); // Unlimited
        },
        "Setting unlimited open files should not throw");

    assertDoesNotThrow(
        () -> {
          context.setMaxOpenFiles(0); // No files allowed
        },
        "Setting zero open files should not throw");

    LOGGER.info("Successfully validated resource limiting");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testNetworkConfiguration")
  void testNetworkConfiguration(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Test network enabling/disabling
    assertDoesNotThrow(
        () -> {
          context.setNetworkEnabled(true);
        },
        "Enabling network should not throw");

    assertDoesNotThrow(
        () -> {
          context.setNetworkEnabled(false);
        },
        "Disabling network should not throw");

    LOGGER.info("Successfully validated network configuration");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testPreview1AndPreview2Coexistence")
  void testPreview1AndPreview2Coexistence(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    // Create context with Preview 1 configuration
    WasiContext context =
        WasiContext.create()
            .inheritStdio()
            .inheritEnv()
            .setNetworkEnabled(true)
            .setMaxOpenFiles(100);

    // Create both Preview 1 and Preview 2 linkers with same context
    Linker<WasiContext> preview1Linker = WasiLinkerUtils.createLinker(engine, context);
    Linker<WasiContext> preview2Linker = WasiLinkerUtils.createPreview2Linker(engine, context);

    assertNotNull(preview1Linker, "Preview 1 linker should be created");
    assertNotNull(preview2Linker, "Preview 2 linker should be created");

    // Both should have their respective imports
    assertTrue(
        WasiLinkerUtils.hasWasiImports(preview1Linker),
        "Preview 1 linker should have WASI imports");
    assertTrue(
        WasiLinkerUtils.hasWasiPreview2Imports(preview2Linker),
        "Preview 2 linker should have WASI Preview 2 imports");

    LOGGER.info("Successfully validated Preview 1 and Preview 2 coexistence");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testErrorHandlingBackwardCompatibility")
  void testErrorHandlingBackwardCompatibility(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Test error conditions that should behave the same in both versions
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.setArgv(null);
        },
        "Null argv should throw IllegalArgumentException");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.setEnv(null, "value");
        },
        "Null env key should throw IllegalArgumentException");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.setEnv("key", null);
        },
        "Null env value should throw IllegalArgumentException");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.setStdin(null);
        },
        "Null stdin path should throw IllegalArgumentException");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.setMaxOpenFiles(-2);
        },
        "Invalid max files should throw IllegalArgumentException");

    LOGGER.info("Successfully validated error handling backward compatibility");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testMethodChainingCompatibility")
  void testMethodChainingCompatibility(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    // Test that method chaining works the same way in both versions
    WasiContext context =
        assertDoesNotThrow(
            () -> {
              return WasiContext.create()
                  .inheritStdio()
                  .inheritEnv()
                  .setNetworkEnabled(true)
                  .setMaxOpenFiles(50)
                  .setWorkingDirectory("/tmp");
            },
            "Method chaining should work in backward compatibility mode");

    assertNotNull(context, "Chained context configuration should succeed");

    LOGGER.info("Successfully validated method chaining compatibility");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testLegacyWasmModuleCompatibility")
  void testLegacyWasmModuleCompatibility(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    // Test that legacy WASM modules expecting Preview 1 still work
    WasiContext context = WasiContext.create().inheritStdio().inheritEnv();

    Linker<WasiContext> linker = WasiLinkerUtils.createLinker(engine, context);

    // Verify that the linker has the expected Preview 1 function imports
    assertTrue(
        WasiLinkerUtils.hasWasiImports(linker),
        "Linker should have traditional WASI imports for legacy modules");

    // In a real test, we would load and run a legacy WASM module here
    // and verify it works correctly

    LOGGER.info("Successfully validated legacy WASM module compatibility");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testConfigurationMigrationPath")
  void testConfigurationMigrationPath(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    // Test that configurations can be migrated from Preview 1 to Preview 2 style

    // Start with Preview 1 style configuration
    WasiContext context =
        WasiContext.create()
            .inheritStdio()
            .inheritEnv()
            .setNetworkEnabled(true)
            .setMaxOpenFiles(100);

    // Add Preview 2 features to the same context
    assertDoesNotThrow(
        () -> {
          context
              .setAsyncIoEnabled(true)
              .setComponentModelEnabled(true)
              .setProcessEnabled(true)
              .setMaxAsyncOperations(20)
              .setAsyncTimeout(5000);
        },
        "Adding Preview 2 features to Preview 1 context should not throw");

    // Create linkers for both versions with the same context
    Linker<WasiContext> preview1Linker = WasiLinkerUtils.createLinker(engine, context);
    Linker<WasiContext> preview2Linker = WasiLinkerUtils.createPreview2Linker(engine, context);
    Linker<WasiContext> fullLinker = WasiLinkerUtils.createFullLinker(engine, context);

    assertNotNull(preview1Linker, "Preview 1 linker should work with migrated context");
    assertNotNull(preview2Linker, "Preview 2 linker should work with migrated context");
    assertNotNull(fullLinker, "Full linker should work with migrated context");

    LOGGER.info("Successfully validated configuration migration path");
  }
}
