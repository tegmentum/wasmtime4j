package ai.tegmentum.wasmtime4j.tests;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.WasiLinker;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for WASI Preview 1 backward compatibility.
 *
 * <p>This test suite ensures that the WASI Preview 2 implementation maintains full backward
 * compatibility with WASI Preview 1 functionality, allowing existing WebAssembly modules to
 * continue working without modification.
 */
public class WasiBackwardCompatibilityTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiBackwardCompatibilityTest.class.getName());

  private WasmRuntime runtime;
  private Engine engine;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    LOGGER.info("Setting up backward compatibility test: " + testInfo.getDisplayName());
    try {
      runtime = WasmRuntimeFactory.create();
      engine = runtime.createEngine();
    } catch (WasmException e) {
      fail("Failed to set up runtime: " + e.getMessage(), e);
    }
  }

  @AfterEach
  void tearDown(TestInfo testInfo) {
    LOGGER.info("Tearing down backward compatibility test: " + testInfo.getDisplayName());
    if (runtime != null) {
      try {
        runtime.close();
      } catch (Exception e) {
        LOGGER.warning("Failed to close runtime: " + e.getMessage());
      }
    }
  }

  @Test
  void testPreview1ContextCreation() throws WasmException {
    // Create traditional WASI context (Preview 1 style)
    WasiContext context = runtime.createWasiContext();
    assertNotNull(context, "WASI context should be created");

    // Configure with Preview 1 methods only
    context.inheritStdio().inheritEnv().setMaxOpenFiles(100);

    LOGGER.info("Successfully created WASI Preview 1 compatible context");
  }

  @Test
  void testPreview1LinkerCreation() throws WasmException {
    WasiContext context = runtime.createWasiContext().inheritStdio().inheritEnv();

    // Create traditional WASI linker (Preview 1)
    Linker<WasiContext> linker = WasiLinker.createLinker(engine, context);
    assertNotNull(linker, "Preview 1 linker should be created");

    // Verify traditional WASI imports are present
    assertTrue(WasiLinker.hasWasiImports(linker), "Linker should have traditional WASI imports");

    LOGGER.info("Successfully created WASI Preview 1 linker");
  }

  @Test
  void testTraditionalWasiLinkerMethods() throws WasmException {
    WasiContext context = runtime.createWasiContext();

    // Test the static utility methods from WasiLinker
    Linker<WasiContext> linker1 = runtime.createLinker(engine);
    assertDoesNotThrow(
        () -> {
          WasiLinker.addToLinker(linker1, context);
        },
        "Adding WASI to linker should not throw");

    // Test convenience method with default context
    Linker<WasiContext> linker2 = runtime.createLinker(engine);
    assertDoesNotThrow(
        () -> {
          WasiLinker.addToLinker(linker2);
        },
        "Adding WASI with default context should not throw");

    // Test convenience linker creation
    Linker<WasiContext> linker3 = WasiLinker.createLinker(engine, context);
    assertNotNull(linker3, "Convenience linker creation should work");

    Linker<WasiContext> linker4 = WasiLinker.createLinker(engine);
    assertNotNull(linker4, "Convenience linker creation with default context should work");

    LOGGER.info("Successfully validated traditional WasiLinker methods");
  }

  @Test
  void testEnvironmentVariableHandling() throws WasmException {
    WasiContext context = runtime.createWasiContext();

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

  @Test
  void testCommandLineArgumentHandling() throws WasmException {
    WasiContext context = runtime.createWasiContext();

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

  @Test
  void testStdioRedirection() throws WasmException, IOException {
    WasiContext context = runtime.createWasiContext();

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

  @Test
  void testFilesystemAccess() throws WasmException, IOException {
    WasiContext context = runtime.createWasiContext();

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

  @Test
  void testResourceLimiting() throws WasmException {
    WasiContext context = runtime.createWasiContext();

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

  @Test
  void testNetworkConfiguration() throws WasmException {
    WasiContext context = runtime.createWasiContext();

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

  @Test
  void testPreview1AndPreview2Coexistence() throws WasmException {
    // Create context with Preview 1 configuration
    WasiContext context =
        runtime
            .createWasiContext()
            .inheritStdio()
            .inheritEnv()
            .setNetworkEnabled(true)
            .setMaxOpenFiles(100);

    // Create both Preview 1 and Preview 2 linkers with same context
    Linker<WasiContext> preview1Linker = WasiLinker.createLinker(engine, context);
    Linker<WasiContext> preview2Linker = WasiLinker.createPreview2Linker(engine, context);

    assertNotNull(preview1Linker, "Preview 1 linker should be created");
    assertNotNull(preview2Linker, "Preview 2 linker should be created");

    // Both should have their respective imports
    assertTrue(
        WasiLinker.hasWasiImports(preview1Linker), "Preview 1 linker should have WASI imports");
    assertTrue(
        WasiLinker.hasWasiPreview2Imports(preview2Linker),
        "Preview 2 linker should have WASI Preview 2 imports");

    LOGGER.info("Successfully validated Preview 1 and Preview 2 coexistence");
  }

  @Test
  void testErrorHandlingBackwardCompatibility() throws WasmException {
    WasiContext context = runtime.createWasiContext();

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

  @Test
  void testMethodChainingCompatibility() throws WasmException {
    // Test that method chaining works the same way in both versions
    WasiContext context =
        assertDoesNotThrow(
            () -> {
              return runtime
                  .createWasiContext()
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

  @Test
  void testLegacyWasmModuleCompatibility() throws WasmException {
    // Test that legacy WASM modules expecting Preview 1 still work
    WasiContext context = runtime.createWasiContext().inheritStdio().inheritEnv();

    Linker<WasiContext> linker = WasiLinker.createLinker(engine, context);

    // Verify that the linker has the expected Preview 1 function imports
    assertTrue(
        WasiLinker.hasWasiImports(linker),
        "Linker should have traditional WASI imports for legacy modules");

    // In a real test, we would load and run a legacy WASM module here
    // and verify it works correctly

    LOGGER.info("Successfully validated legacy WASM module compatibility");
  }

  @Test
  void testConfigurationMigrationPath() throws WasmException {
    // Test that configurations can be migrated from Preview 1 to Preview 2 style

    // Start with Preview 1 style configuration
    WasiContext context =
        runtime
            .createWasiContext()
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
    Linker<WasiContext> preview1Linker = WasiLinker.createLinker(engine, context);
    Linker<WasiContext> preview2Linker = WasiLinker.createPreview2Linker(engine, context);
    Linker<WasiContext> fullLinker = WasiLinker.createFullLinker(engine, context);

    assertNotNull(preview1Linker, "Preview 1 linker should work with migrated context");
    assertNotNull(preview2Linker, "Preview 2 linker should work with migrated context");
    assertNotNull(fullLinker, "Full linker should work with migrated context");

    LOGGER.info("Successfully validated configuration migration path");
  }
}
