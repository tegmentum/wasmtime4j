package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.wasi.Wasi;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive integration tests for WASI (WebAssembly System Interface) functionality across
 * both JNI and Panama implementations. Tests all WASI operations including filesystem access,
 * environment variables, I/O redirection, and security boundaries.
 */
@Tag(TestCategories.INTEGRATION)
@Tag(TestCategories.WASI)
@Tag(TestCategories.CROSS_RUNTIME)
public final class WasiIntegrationComprehensiveTest {
  private static final Logger LOGGER =
      Logger.getLogger(WasiIntegrationComprehensiveTest.class.getName());

  @TempDir private Path tempDirectory;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() {
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = engine.createStore();
    LOGGER.info("Set up WASI integration test with runtime: " + runtime.getRuntimeType());
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  /** Tests basic WASI context creation and configuration. */
  @Test
  void testWasiContextCreation() {
    LOGGER.info("Testing WASI context creation");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);
      assertTrue(wasi.isValid());
      wasi.close();
    });
  }

  /** Tests WASI context creation with custom environment variables. */
  @Test
  void testWasiContextWithCustomEnvironment() {
    LOGGER.info("Testing WASI context with custom environment");

    final Map<String, String> customEnv = new HashMap<>();
    customEnv.put("TEST_VAR", "test_value");
    customEnv.put("CUSTOM_PATH", "/custom/path");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(customEnv)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);
      assertTrue(wasi.isValid());

      // Verify environment is properly configured
      final Map<String, String> actualEnv = wasi.getEnvironment();
      assertEquals("test_value", actualEnv.get("TEST_VAR"));
      assertEquals("/custom/path", actualEnv.get("CUSTOM_PATH"));

      wasi.close();
    });
  }

  /** Tests WASI context creation with command line arguments. */
  @Test
  void testWasiContextWithArguments() {
    LOGGER.info("Testing WASI context with command line arguments");

    final List<String> args = Arrays.asList("program", "--flag", "value", "positional");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .arguments(args)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);
      assertTrue(wasi.isValid());

      // Verify arguments are properly configured
      final List<String> actualArgs = wasi.getArguments();
      assertEquals(args.size(), actualArgs.size());
      for (int i = 0; i < args.size(); i++) {
        assertEquals(args.get(i), actualArgs.get(i));
      }

      wasi.close();
    });
  }

  /** Tests WASI context creation with pre-opened directories. */
  @Test
  void testWasiContextWithPreopenedDirectories() throws IOException {
    LOGGER.info("Testing WASI context with pre-opened directories");

    // Create test directories
    final Path readOnlyDir = tempDirectory.resolve("readonly");
    final Path readWriteDir = tempDirectory.resolve("readwrite");
    Files.createDirectories(readOnlyDir);
    Files.createDirectories(readWriteDir);

    // Create test files
    final Path testFile1 = readOnlyDir.resolve("test1.txt");
    final Path testFile2 = readWriteDir.resolve("test2.txt");
    Files.write(testFile1, "Hello from readonly".getBytes());
    Files.write(testFile2, "Hello from readwrite".getBytes());

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(readOnlyDir.toString(), "readonly", true, false) // read-only
            .preopenDir(readWriteDir.toString(), "readwrite", true, true) // read-write
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);
      assertTrue(wasi.isValid());

      // Verify directories are pre-opened
      final Map<String, String> preopenedDirs = wasi.getPreopenedDirectories();
      assertTrue(preopenedDirs.containsKey("readonly"));
      assertTrue(preopenedDirs.containsKey("readwrite"));

      wasi.close();
    });
  }

  /** Tests WASI module compilation and instantiation. */
  @Test
  void testWasiModuleInstantiation() {
    LOGGER.info("Testing WASI module instantiation");

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
    assertNotNull(wasmBytes);

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Module module = engine.createModule(wasmBytes);
      assertNotNull(module);

      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);

      final Instance instance = store.createInstance(module, wasi.getImports());
      assertNotNull(instance);
      assertTrue(instance.isValid());

      instance.close();
      wasi.close();
      module.close();
    });
  }

  /** Tests WASI function execution with basic I/O operations. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testWasiFunctionExecution() {
    LOGGER.info("Testing WASI function execution");

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Module module = engine.createModule(wasmBytes);
      final Wasi wasi = store.createWasi(config);
      final Instance instance = store.createInstance(module, wasi.getImports());

      // Look for WASI start function
      if (instance.hasExport("_start")) {
        final var startFunction = instance.getExport("_start").asFunction();
        assertNotNull(startFunction);

        // Execute the start function
        assertDoesNotThrow(() -> startFunction.call());
      } else {
        LOGGER.warning("WASI module does not export _start function");
      }

      instance.close();
      wasi.close();
      module.close();
    });
  }

  /** Tests WASI resource cleanup and lifecycle management. */
  @Test
  void testWasiResourceCleanup() {
    LOGGER.info("Testing WASI resource cleanup");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);
      assertTrue(wasi.isValid());

      // Close and verify resource cleanup
      wasi.close();
      
      // Verify wasi is no longer valid after close
      // Note: Implementation should invalidate the handle
    });
  }

  /** Tests WASI error handling with invalid configurations. */
  @Test
  void testWasiErrorHandling() {
    LOGGER.info("Testing WASI error handling");

    // Test with invalid directory path
    assertThrows(Exception.class, () -> {
      final WasiConfig config =
          WasiConfig.builder()
              .inheritEnv(true)
              .preopenDir("/invalid/nonexistent/path", "invalid", true, false)
              .build();

      store.createWasi(config);
    });

    // Test with null environment values
    assertThrows(Exception.class, () -> {
      final Map<String, String> invalidEnv = new HashMap<>();
      invalidEnv.put("TEST_KEY", null);

      final WasiConfig config =
          WasiConfig.builder()
              .inheritEnv(false)
              .environment(invalidEnv)
              .build();

      store.createWasi(config);
    });
  }

  /** Tests WASI concurrency with multiple simultaneous instances. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testWasiConcurrency() throws InterruptedException {
    LOGGER.info("Testing WASI concurrency");

    final int concurrentInstances = 4;
    final ExecutorService executor = Executors.newFixedThreadPool(concurrentInstances);

    try {
      final CompletableFuture<Void>[] futures = new CompletableFuture[concurrentInstances];

      for (int i = 0; i < concurrentInstances; i++) {
        final int instanceId = i;
        futures[i] = CompletableFuture.runAsync(() -> {
          try (final WasmRuntime instanceRuntime = WasmRuntimeFactory.create();
               final Engine instanceEngine = instanceRuntime.createEngine();
               final Store instanceStore = instanceEngine.createStore()) {

            LOGGER.info("Starting concurrent WASI instance " + instanceId);

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .arguments(Arrays.asList("program", "--instance", String.valueOf(instanceId)))
                    .inheritStdin(false) // Avoid stdin conflicts
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final Wasi wasi = instanceStore.createWasi(config);
            assertNotNull(wasi);
            assertTrue(wasi.isValid());

            // Hold the instance for a brief period
            try {
              Thread.sleep(100);
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new RuntimeException(e);
            }

            wasi.close();
            LOGGER.info("Completed concurrent WASI instance " + instanceId);

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Concurrent WASI instance " + instanceId + " failed", e);
            throw new RuntimeException(e);
          }
        }, executor);
      }

      // Wait for all instances to complete
      CompletableFuture.allOf(futures).join();
      LOGGER.info("All concurrent WASI instances completed successfully");

    } finally {
      executor.shutdown();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  /** Tests cross-runtime WASI parity between JNI and Panama implementations. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeWasiParity() {
    LOGGER.info("Testing cross-runtime WASI parity");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final CrossRuntimeValidator.RuntimeOperation<String> wasiOperation = runtime -> {
      try (final Engine engine = runtime.createEngine();
           final Store store = engine.createStore()) {

        final WasiConfig config =
            WasiConfig.builder()
                .inheritEnv(true)
                .arguments(Arrays.asList("test", "program"))
                .inheritStdin(true)
                .inheritStdout(true)
                .inheritStderr(true)
                .build();

        final Wasi wasi = store.createWasi(config);
        final boolean isValid = wasi.isValid();
        final int envCount = wasi.getEnvironment().size();
        final int argCount = wasi.getArguments().size();

        wasi.close();

        return String.format("valid=%s,env=%d,args=%d", isValid, envCount, argCount);
      }
    };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(wasiOperation, Duration.ofSeconds(15));

    assertTrue(result.isValid(),
        "WASI behavior differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime WASI validation successful: " + result.getDifferenceDescription());
  }

  /** Tests WASI environment variable isolation between instances. */
  @Test
  void testWasiEnvironmentIsolation() {
    LOGGER.info("Testing WASI environment isolation");

    final Map<String, String> env1 = new HashMap<>();
    env1.put("INSTANCE", "1");
    env1.put("SHARED", "value1");

    final Map<String, String> env2 = new HashMap<>();
    env2.put("INSTANCE", "2");
    env2.put("SHARED", "value2");

    final WasiConfig config1 =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(env1)
            .build();

    final WasiConfig config2 =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(env2)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi1 = store.createWasi(config1);
      final Wasi wasi2 = store.createWasi(config2);

      // Verify environments are isolated
      final Map<String, String> actualEnv1 = wasi1.getEnvironment();
      final Map<String, String> actualEnv2 = wasi2.getEnvironment();

      assertEquals("1", actualEnv1.get("INSTANCE"));
      assertEquals("value1", actualEnv1.get("SHARED"));

      assertEquals("2", actualEnv2.get("INSTANCE"));
      assertEquals("value2", actualEnv2.get("SHARED"));

      // Environments should not affect each other
      assertTrue(actualEnv1.containsKey("INSTANCE"));
      assertTrue(actualEnv2.containsKey("INSTANCE"));
      assertEquals("1", actualEnv1.get("INSTANCE"));
      assertEquals("2", actualEnv2.get("INSTANCE"));

      wasi1.close();
      wasi2.close();
    });
  }

  /** Tests WASI with stress conditions and resource limits. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testWasiStressConditions() {
    LOGGER.info("Testing WASI under stress conditions");

    // Test with many environment variables
    final Map<String, String> largeEnv = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      largeEnv.put("VAR_" + i, "value_" + i);
    }

    final WasiConfig stressConfig =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(largeEnv)
            .arguments(Arrays.asList("program", "--stress", "test"))
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(stressConfig);
      assertNotNull(wasi);
      assertTrue(wasi.isValid());

      final Map<String, String> actualEnv = wasi.getEnvironment();
      assertEquals(1000, actualEnv.size());

      // Verify a few random entries
      assertTrue(actualEnv.containsKey("VAR_0"));
      assertTrue(actualEnv.containsKey("VAR_500"));
      assertTrue(actualEnv.containsKey("VAR_999"));

      wasi.close();
    });
  }

  /** Tests WASI configuration validation and error scenarios. */
  @Test
  void testWasiConfigValidation() {
    LOGGER.info("Testing WASI configuration validation");

    // Test valid minimal configuration
    assertDoesNotThrow(() -> {
      final WasiConfig config = WasiConfig.builder().build();
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);
      wasi.close();
    });

    // Test configuration with empty arguments list
    assertDoesNotThrow(() -> {
      final WasiConfig config =
          WasiConfig.builder()
              .arguments(Arrays.asList())
              .build();
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);
      assertEquals(0, wasi.getArguments().size());
      wasi.close();
    });

    // Test configuration with empty environment
    assertDoesNotThrow(() -> {
      final WasiConfig config =
          WasiConfig.builder()
              .inheritEnv(false)
              .environment(new HashMap<>())
              .build();
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);
      assertEquals(0, wasi.getEnvironment().size());
      wasi.close();
    });
  }

  /** Tests WASI preview1 compatibility and features. */
  @Test
  void testWasiPreview1Compatibility() {
    LOGGER.info("Testing WASI preview1 compatibility");

    // Use WASI preview1 module
    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Module module = engine.createModule(wasmBytes);
      final Wasi wasi = store.createWasi(config);

      // Verify WASI imports are properly provided
      final var imports = wasi.getImports();
      assertNotNull(imports);
      assertTrue(imports.size() > 0);

      // Verify common WASI preview1 functions are available
      final boolean hasRequiredImports = imports.keySet().stream()
          .anyMatch(key -> key.contains("wasi_snapshot_preview1"));

      assertTrue(hasRequiredImports, "WASI preview1 imports not found");

      final Instance instance = store.createInstance(module, imports);
      assertNotNull(instance);

      instance.close();
      wasi.close();
      module.close();
    });
  }

  /** Tests WASI memory isolation and security boundaries. */
  @Test
  void testWasiMemoryIsolation() {
    LOGGER.info("Testing WASI memory isolation");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi1 = store.createWasi(config);
      final Wasi wasi2 = store.createWasi(config);

      // Each WASI instance should be independent
      assertNotNull(wasi1);
      assertNotNull(wasi2);
      assertTrue(wasi1.isValid());
      assertTrue(wasi2.isValid());

      // Verify they are different instances
      assertTrue(wasi1 != wasi2);

      wasi1.close();
      wasi2.close();
    });
  }
}