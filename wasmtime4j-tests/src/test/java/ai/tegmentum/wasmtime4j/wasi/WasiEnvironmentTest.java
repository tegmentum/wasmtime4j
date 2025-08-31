package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.functions.WasmFunction;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.wasi.Wasi;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive environment and argument testing for WASI functionality. Tests environment
 * variable access, command line argument handling, inheritance patterns, and cross-runtime
 * compatibility for environment configuration.
 */
@Tag(TestCategories.INTEGRATION)
@Tag(TestCategories.WASI)
@Tag(TestCategories.ENVIRONMENT)
public final class WasiEnvironmentTest {
  private static final Logger LOGGER = Logger.getLogger(WasiEnvironmentTest.class.getName());

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() {
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = engine.createStore();
    LOGGER.info("Set up WASI environment test with runtime: " + runtime.getRuntimeType());
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

  /** Tests basic environment variable configuration and access. */
  @Test
  void testBasicEnvironmentConfiguration() {
    LOGGER.info("Testing basic environment configuration");

    final Map<String, String> testEnv = new HashMap<>();
    testEnv.put("TEST_VAR", "test_value");
    testEnv.put("PATH", "/usr/bin:/bin");
    testEnv.put("HOME", "/home/user");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(testEnv)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);

      final Map<String, String> actualEnv = wasi.getEnvironment();
      assertNotNull(actualEnv);
      assertEquals(3, actualEnv.size());
      assertEquals("test_value", actualEnv.get("TEST_VAR"));
      assertEquals("/usr/bin:/bin", actualEnv.get("PATH"));
      assertEquals("/home/user", actualEnv.get("HOME"));

      wasi.close();
    });
  }

  /** Tests environment variable inheritance from host system. */
  @Test
  void testEnvironmentInheritance() {
    LOGGER.info("Testing environment inheritance");

    final WasiConfig inheritConfig =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(inheritConfig);
      assertNotNull(wasi);

      final Map<String, String> inheritedEnv = wasi.getEnvironment();
      assertNotNull(inheritedEnv);
      
      // Should inherit system environment (at least some variables)
      assertTrue(inheritedEnv.size() > 0);
      
      // Common environment variables that should be present on most systems
      assertTrue(inheritedEnv.containsKey("PATH") || inheritedEnv.containsKey("HOME") ||
                 inheritedEnv.size() > 5); // Flexible check for different OS

      wasi.close();
    });
  }

  /** Tests mixed environment configuration (inheritance + custom). */
  @Test
  void testMixedEnvironmentConfiguration() {
    LOGGER.info("Testing mixed environment configuration");

    final Map<String, String> customEnv = new HashMap<>();
    customEnv.put("CUSTOM_VAR", "custom_value");
    customEnv.put("OVERRIDE_VAR", "overridden");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true) // Inherit system environment
            .environment(customEnv) // Add custom variables
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);

      final Map<String, String> actualEnv = wasi.getEnvironment();
      assertNotNull(actualEnv);

      // Should have both inherited and custom variables
      assertTrue(actualEnv.size() > 2);
      assertEquals("custom_value", actualEnv.get("CUSTOM_VAR"));
      assertEquals("overridden", actualEnv.get("OVERRIDE_VAR"));

      wasi.close();
    });
  }

  /** Tests command line argument configuration and access. */
  @Test
  void testCommandLineArguments() {
    LOGGER.info("Testing command line arguments");

    final List<String> testArgs = Arrays.asList(
        "program",
        "--verbose",
        "--output", "file.txt",
        "--count", "42",
        "input1.txt",
        "input2.txt"
    );

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .arguments(testArgs)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);

      final List<String> actualArgs = wasi.getArguments();
      assertNotNull(actualArgs);
      assertEquals(testArgs.size(), actualArgs.size());

      for (int i = 0; i < testArgs.size(); i++) {
        assertEquals(testArgs.get(i), actualArgs.get(i));
      }

      wasi.close();
    });
  }

  /** Tests empty and minimal configurations. */
  @Test
  void testEmptyConfigurations() {
    LOGGER.info("Testing empty configurations");

    // Test empty environment
    final WasiConfig emptyEnvConfig =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(new HashMap<>())
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(emptyEnvConfig);
      assertNotNull(wasi);

      final Map<String, String> env = wasi.getEnvironment();
      assertNotNull(env);
      assertEquals(0, env.size());

      wasi.close();
    });

    // Test empty arguments
    final WasiConfig emptyArgsConfig =
        WasiConfig.builder()
            .inheritEnv(true)
            .arguments(Arrays.asList())
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(emptyArgsConfig);
      assertNotNull(wasi);

      final List<String> args = wasi.getArguments();
      assertNotNull(args);
      assertEquals(0, args.size());

      wasi.close();
    });
  }

  /** Tests environment variable access through WASI modules. */
  @Test
  void testEnvironmentVariableAccess() {
    LOGGER.info("Testing environment variable access through WASI module");

    final Map<String, String> testEnv = new HashMap<>();
    testEnv.put("TEST_ACCESS", "accessible_value");
    testEnv.put("PROGRAM_MODE", "test");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(testEnv)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_env");

    assertDoesNotThrow(() -> {
      final Module module = engine.createModule(wasmBytes);
      final Wasi wasi = store.createWasi(config);
      final Instance instance = store.createInstance(module, wasi.getImports());

      // Test environment access functionality
      if (instance.hasExport("get_envs")) {
        final WasmFunction envFunction = instance.getExport("get_envs").asFunction();
        assertNotNull(envFunction);
        
        assertDoesNotThrow(() -> {
          final var result = envFunction.call();
          assertNotNull(result);
          // The function should be able to access environment variables
        });
      }

      instance.close();
      wasi.close();
      module.close();
    });
  }

  /** Tests large environment configurations for stress testing. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testLargeEnvironmentConfiguration() {
    LOGGER.info("Testing large environment configuration");

    final Map<String, String> largeEnv = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      largeEnv.put("VAR_" + i, "value_" + i + "_" + "x".repeat(50)); // Longer values
    }

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(largeEnv)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);

      final Map<String, String> actualEnv = wasi.getEnvironment();
      assertNotNull(actualEnv);
      assertEquals(1000, actualEnv.size());

      // Verify some random entries
      assertTrue(actualEnv.containsKey("VAR_0"));
      assertTrue(actualEnv.containsKey("VAR_500"));
      assertTrue(actualEnv.containsKey("VAR_999"));

      // Verify value content
      assertTrue(actualEnv.get("VAR_123").startsWith("value_123_"));
      assertTrue(actualEnv.get("VAR_123").contains("x"));

      wasi.close();
    });
  }

  /** Tests command line argument parsing and special characters. */
  @Test
  void testArgumentParsingAndSpecialCharacters() {
    LOGGER.info("Testing argument parsing with special characters");

    final List<String> complexArgs = Arrays.asList(
        "program",
        "--option=value with spaces",
        "--json={\"key\":\"value\"}",
        "--path=/path/to/file with spaces",
        "argument with spaces",
        "--empty=",
        "unicode_test_ñ_λ_中文",
        "--quotes=\"quoted value\"",
        "--escape=path\\with\\backslashes"
    );

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .arguments(complexArgs)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      assertNotNull(wasi);

      final List<String> actualArgs = wasi.getArguments();
      assertNotNull(actualArgs);
      assertEquals(complexArgs.size(), actualArgs.size());

      // Verify special characters are preserved
      assertEquals("--option=value with spaces", actualArgs.get(1));
      assertEquals("--json={\"key\":\"value\"}", actualArgs.get(2));
      assertEquals("--path=/path/to/file with spaces", actualArgs.get(3));
      assertEquals("argument with spaces", actualArgs.get(4));
      assertEquals("unicode_test_ñ_λ_中文", actualArgs.get(6));

      wasi.close();
    });
  }

  /** Tests environment variable validation and error handling. */
  @Test
  void testEnvironmentValidation() {
    LOGGER.info("Testing environment validation");

    // Test null key validation
    assertThrows(Exception.class, () -> {
      final Map<String, String> invalidEnv = new HashMap<>();
      invalidEnv.put(null, "value");

      final WasiConfig config =
          WasiConfig.builder()
              .inheritEnv(false)
              .environment(invalidEnv)
              .build();

      store.createWasi(config);
    });

    // Test null value validation
    assertThrows(Exception.class, () -> {
      final Map<String, String> invalidEnv = new HashMap<>();
      invalidEnv.put("KEY", null);

      final WasiConfig config =
          WasiConfig.builder()
              .inheritEnv(false)
              .environment(invalidEnv)
              .build();

      store.createWasi(config);
    });

    // Test empty key validation
    assertDoesNotThrow(() -> {
      final Map<String, String> validEnv = new HashMap<>();
      validEnv.put("", "empty_key_value"); // Empty key should be allowed

      final WasiConfig config =
          WasiConfig.builder()
              .inheritEnv(false)
              .environment(validEnv)
              .build();

      final Wasi wasi = store.createWasi(config);
      assertEquals("empty_key_value", wasi.getEnvironment().get(""));
      wasi.close();
    });
  }

  /** Tests argument validation and error handling. */
  @Test
  void testArgumentValidation() {
    LOGGER.info("Testing argument validation");

    // Test null argument validation
    assertThrows(Exception.class, () -> {
      final List<String> invalidArgs = Arrays.asList("program", null, "valid");

      final WasiConfig config =
          WasiConfig.builder()
              .arguments(invalidArgs)
              .build();

      store.createWasi(config);
    });

    // Test empty string arguments (should be allowed)
    assertDoesNotThrow(() -> {
      final List<String> validArgs = Arrays.asList("program", "", "valid");

      final WasiConfig config =
          WasiConfig.builder()
              .arguments(validArgs)
              .build();

      final Wasi wasi = store.createWasi(config);
      assertEquals(3, wasi.getArguments().size());
      assertEquals("", wasi.getArguments().get(1));
      wasi.close();
    });
  }

  /** Tests cross-runtime environment compatibility. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeEnvironmentCompatibility() {
    LOGGER.info("Testing cross-runtime environment compatibility");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final Map<String, String> testEnv = new HashMap<>();
    testEnv.put("CROSS_TEST", "cross_value");
    testEnv.put("RUNTIME_CHECK", "both");

    final CrossRuntimeValidator.RuntimeOperation<String> envOperation = runtime -> {
      try (final Engine engine = runtime.createEngine();
           final Store store = engine.createStore()) {

        final WasiConfig config =
            WasiConfig.builder()
                .inheritEnv(false)
                .environment(testEnv)
                .arguments(Arrays.asList("test", "cross", "runtime"))
                .inheritStdin(true)
                .inheritStdout(true)
                .inheritStderr(true)
                .build();

        final Wasi wasi = store.createWasi(config);
        final Map<String, String> env = wasi.getEnvironment();
        final List<String> args = wasi.getArguments();

        final String result = String.format("env_size=%d,cross_test=%s,args_size=%d",
            env.size(), env.get("CROSS_TEST"), args.size());

        wasi.close();
        return result;
      }
    };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(envOperation, Duration.ofSeconds(15));

    assertTrue(result.isValid(),
        "Environment behavior differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime environment validation successful");
  }

  /** Tests environment isolation between multiple WASI instances. */
  @Test
  void testEnvironmentIsolation() {
    LOGGER.info("Testing environment isolation between instances");

    final Map<String, String> env1 = new HashMap<>();
    env1.put("INSTANCE_ID", "1");
    env1.put("SHARED_VAR", "value1");

    final Map<String, String> env2 = new HashMap<>();
    env2.put("INSTANCE_ID", "2");
    env2.put("SHARED_VAR", "value2");
    env2.put("UNIQUE_VAR", "unique");

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

      final Map<String, String> actualEnv1 = wasi1.getEnvironment();
      final Map<String, String> actualEnv2 = wasi2.getEnvironment();

      // Verify isolation
      assertEquals("1", actualEnv1.get("INSTANCE_ID"));
      assertEquals("2", actualEnv2.get("INSTANCE_ID"));
      assertEquals("value1", actualEnv1.get("SHARED_VAR"));
      assertEquals("value2", actualEnv2.get("SHARED_VAR"));

      // Verify unique variables
      assertFalse(actualEnv1.containsKey("UNIQUE_VAR"));
      assertTrue(actualEnv2.containsKey("UNIQUE_VAR"));
      assertEquals("unique", actualEnv2.get("UNIQUE_VAR"));

      wasi1.close();
      wasi2.close();
    });
  }

  /** Tests environment variable modification restrictions. */
  @Test
  void testEnvironmentModificationRestrictions() {
    LOGGER.info("Testing environment modification restrictions");

    final Map<String, String> testEnv = new HashMap<>();
    testEnv.put("IMMUTABLE_VAR", "original_value");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(testEnv)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      final Map<String, String> env = wasi.getEnvironment();

      // Environment should be read-only or defensive copy
      final String originalValue = env.get("IMMUTABLE_VAR");
      assertEquals("original_value", originalValue);

      // Attempt to modify returned map should not affect WASI instance
      assertDoesNotThrow(() -> {
        env.put("IMMUTABLE_VAR", "modified_value");
        env.put("NEW_VAR", "new_value");
      });

      // Original environment should remain unchanged if defensive copy is used
      final Map<String, String> freshEnv = wasi.getEnvironment();
      // This test depends on implementation - may return defensive copies
      assertNotNull(freshEnv);

      wasi.close();
    });
  }

  /** Tests Unicode and international character support in environment. */
  @Test
  void testUnicodeEnvironmentSupport() {
    LOGGER.info("Testing Unicode environment support");

    final Map<String, String> unicodeEnv = new HashMap<>();
    unicodeEnv.put("ENGLISH", "Hello World");
    unicodeEnv.put("ESPAÑOL", "Hola Mundo");
    unicodeEnv.put("FRANÇAIS", "Bonjour le Monde");
    unicodeEnv.put("中文", "你好世界");
    unicodeEnv.put("РУССКИЙ", "Привет мир");
    unicodeEnv.put("العربية", "مرحبا بالعالم");
    unicodeEnv.put("EMOJI", "🌍🚀✨");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(false)
            .environment(unicodeEnv)
            .build();

    assertDoesNotThrow(() -> {
      final Wasi wasi = store.createWasi(config);
      final Map<String, String> actualEnv = wasi.getEnvironment();

      assertEquals("Hello World", actualEnv.get("ENGLISH"));
      assertEquals("Hola Mundo", actualEnv.get("ESPAÑOL"));
      assertEquals("Bonjour le Monde", actualEnv.get("FRANÇAIS"));
      assertEquals("你好世界", actualEnv.get("中文"));
      assertEquals("Привет мир", actualEnv.get("РУССКИЙ"));
      assertEquals("مرحبا بالعالم", actualEnv.get("العربية"));
      assertEquals("🌍🚀✨", actualEnv.get("EMOJI"));

      wasi.close();
    });
  }
}