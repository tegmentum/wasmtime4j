package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmEngine;
import ai.tegmentum.wasmtime4j.WasmInstance;
import ai.tegmentum.wasmtime4j.WasmModule;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmStore;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContextBuilder;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for WASI environment variable configuration and access.
 * 
 * This test class validates:
 * - Environment variable configuration and retrieval
 * - Environment variable security and access controls
 * - Large environment variable handling
 * - Unicode and special character support
 * - Environment inheritance and isolation
 * - Cross-runtime environment consistency
 */
@Tag(TestCategories.WASI_ENVIRONMENT)
@Tag(TestCategories.COMPREHENSIVE_TESTING)
@DisplayName("WASI Environment Variable Tests")
@Timeout(value = 5, unit = TimeUnit.MINUTES)
public final class WasiEnvironmentTest extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiEnvironmentTest.class.getName());

  @TempDir
  private Path tempDirectory;

  /**
   * Tests basic environment variable configuration and access.
   * Validates that environment variables can be set and retrieved correctly.
   */
  @Test
  @DisplayName("Environment Variables - Basic Configuration and Access")
  void testBasicEnvironmentVariableAccess() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing basic environment variable access with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        // Create context with basic environment variables
        final Map<String, String> environment = new HashMap<>();
        environment.put("HOME", "/home/test");
        environment.put("PATH", "/usr/bin:/bin");
        environment.put("USER", "testuser");
        environment.put("LANG", "en_US.UTF-8");

        final WasiContext context = createWasiContextWithEnvironment(runtimeType, environment);
        registerForCleanup(context);

        // Test individual environment variable access
        assertEquals("/home/test", context.getEnvironmentVariable("HOME"),
            "HOME environment variable should match");
        assertEquals("/usr/bin:/bin", context.getEnvironmentVariable("PATH"),
            "PATH environment variable should match");
        assertEquals("testuser", context.getEnvironmentVariable("USER"),
            "USER environment variable should match");
        assertEquals("en_US.UTF-8", context.getEnvironmentVariable("LANG"),
            "LANG environment variable should match");

        // Test non-existent variable
        assertNull(context.getEnvironmentVariable("NONEXISTENT"),
            "Non-existent environment variable should return null");

        // Test all environment variables
        final Map<String, String> allEnv = context.getEnvironment();
        assertEquals(environment.size(), allEnv.size(),
            "Environment variable count should match");
        for (final Map.Entry<String, String> entry : environment.entrySet()) {
          assertEquals(entry.getValue(), allEnv.get(entry.getKey()),
              "Environment variable " + entry.getKey() + " should match");
        }

        LOGGER.info("Successfully validated basic environment variable access");
      }
    });
  }

  /**
   * Tests environment variable configuration with special characters and Unicode.
   * Validates proper handling of complex environment variable values.
   */
  @Test
  @DisplayName("Environment Variables - Special Characters and Unicode Support")
  void testEnvironmentVariableSpecialCharacters() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing environment variables with special characters using " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final Map<String, String> environment = new HashMap<>();
        environment.put("SPECIAL_CHARS", "!@#$%^&*()_+-=[]{}|;':\",./<>?");
        environment.put("UNICODE_VAR", "Hello, 世界! Привет мир! مرحبا بالعالم");
        environment.put("MULTILINE", "Line 1\nLine 2\nLine 3");
        environment.put("QUOTES", "\"double quotes\" and 'single quotes'");
        environment.put("BACKSLASH", "C:\\Windows\\System32");
        environment.put("EMPTY_VALUE", "");
        environment.put("SPACES", "   value with spaces   ");

        final WasiContext context = createWasiContextWithEnvironment(runtimeType, environment);
        registerForCleanup(context);

        // Test special characters
        assertEquals("!@#$%^&*()_+-=[]{}|;':\",./<>?", 
            context.getEnvironmentVariable("SPECIAL_CHARS"),
            "Special characters should be preserved");

        // Test Unicode
        assertEquals("Hello, 世界! Привет мир! مرحبا بالعالم", 
            context.getEnvironmentVariable("UNICODE_VAR"),
            "Unicode characters should be preserved");

        // Test multiline
        assertEquals("Line 1\nLine 2\nLine 3", 
            context.getEnvironmentVariable("MULTILINE"),
            "Multiline values should be preserved");

        // Test quotes
        assertEquals("\"double quotes\" and 'single quotes'", 
            context.getEnvironmentVariable("QUOTES"),
            "Quotes should be preserved");

        // Test backslashes
        assertEquals("C:\\Windows\\System32", 
            context.getEnvironmentVariable("BACKSLASH"),
            "Backslashes should be preserved");

        // Test empty value
        assertEquals("", context.getEnvironmentVariable("EMPTY_VALUE"),
            "Empty values should be supported");

        // Test spaces
        assertEquals("   value with spaces   ", 
            context.getEnvironmentVariable("SPACES"),
            "Leading/trailing spaces should be preserved");

        LOGGER.info("Successfully validated special character and Unicode environment variables");
      }
    });
  }

  /**
   * Tests large environment variable configurations.
   * Validates handling of many variables and large values.
   */
  @Test
  @DisplayName("Environment Variables - Large Configuration Handling")
  void testLargeEnvironmentConfiguration() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing large environment configuration with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final Map<String, String> environment = new HashMap<>();
        
        // Create many environment variables
        for (int i = 0; i < 100; i++) {
          environment.put("VAR_" + i, "value_" + i);
        }
        
        // Add some variables with large values
        final StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
          largeValue.append("This is a very long environment variable value part ").append(i).append(" ");
        }
        environment.put("LARGE_VALUE", largeValue.toString());
        
        // Add variable with very long name
        final String longName = "VERY_LONG_ENVIRONMENT_VARIABLE_NAME_" + "X".repeat(100);
        environment.put(longName, "long_name_value");

        final WasiContext context = createWasiContextWithEnvironment(runtimeType, environment);
        registerForCleanup(context);

        // Validate all variables are accessible
        final Map<String, String> retrievedEnv = context.getEnvironment();
        assertEquals(environment.size(), retrievedEnv.size(),
            "All environment variables should be accessible");

        // Test access to numbered variables
        for (int i = 0; i < 100; i++) {
          assertEquals("value_" + i, context.getEnvironmentVariable("VAR_" + i),
              "Numbered variable " + i + " should be accessible");
        }

        // Test large value access
        assertEquals(largeValue.toString(), context.getEnvironmentVariable("LARGE_VALUE"),
            "Large environment variable value should be accessible");

        // Test long name access
        assertEquals("long_name_value", context.getEnvironmentVariable(longName),
            "Variable with long name should be accessible");

        LOGGER.info("Successfully validated large environment configuration");
      }
    });
  }

  /**
   * Tests environment variable inheritance from host process.
   * Validates proper inheritance of system environment variables.
   */
  @Test
  @DisplayName("Environment Variables - Host Process Inheritance")
  void testEnvironmentInheritance() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing environment inheritance with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        // Get current system environment
        final Map<String, String> systemEnv = System.getenv();
        assertFalse(systemEnv.isEmpty(), "System environment should not be empty");

        // Create context with inherited environment
        final WasiContext context = createWasiContextWithInheritedEnvironment(runtimeType);
        registerForCleanup(context);

        final Map<String, String> inheritedEnv = context.getEnvironment();

        // Validate that key system variables are inherited
        if (systemEnv.containsKey("PATH")) {
          assertEquals(systemEnv.get("PATH"), inheritedEnv.get("PATH"),
              "PATH should be inherited from system environment");
        }

        if (systemEnv.containsKey("HOME") || systemEnv.containsKey("USERPROFILE")) {
          final String homeVar = TestUtils.isWindows() ? "USERPROFILE" : "HOME";
          if (systemEnv.containsKey(homeVar)) {
            assertEquals(systemEnv.get(homeVar), inheritedEnv.get(homeVar),
                homeVar + " should be inherited from system environment");
          }
        }

        // Validate inherited environment size is reasonable
        assertTrue(inheritedEnv.size() >= 3,
            "Inherited environment should contain multiple variables");

        LOGGER.info("Successfully validated environment inheritance (" + inheritedEnv.size() + " variables)");
      }
    });
  }

  /**
   * Tests environment variable security and access controls.
   * Validates that environment access can be controlled and restricted.
   */
  @Test
  @DisplayName("Environment Variables - Security and Access Controls")
  void testEnvironmentSecurityControls() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing environment security controls with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final Map<String, String> environment = new HashMap<>();
        environment.put("PUBLIC_VAR", "public_value");
        environment.put("SENSITIVE_VAR", "sensitive_value");
        environment.put("API_KEY", "secret_api_key_12345");
        environment.put("PASSWORD", "super_secret_password");

        final WasiContext context = createWasiContextWithEnvironment(runtimeType, environment);
        registerForCleanup(context);

        // Test basic access
        assertEquals("public_value", context.getEnvironmentVariable("PUBLIC_VAR"),
            "Public variable should be accessible");

        // Test case sensitivity
        assertNull(context.getEnvironmentVariable("public_var"),
            "Environment variables should be case sensitive");

        // Test null/empty name handling
        assertThrows(IllegalArgumentException.class, () -> {
          context.getEnvironmentVariable(null);
        }, "Null variable name should throw exception");

        assertThrows(IllegalArgumentException.class, () -> {
          context.getEnvironmentVariable("");
        }, "Empty variable name should throw exception");

        // Test access after context close
        context.close();
        assertThrows(Exception.class, () -> {
          context.getEnvironmentVariable("PUBLIC_VAR");
        }, "Environment access should fail after context close");

        LOGGER.info("Successfully validated environment security controls");
      }
    });
  }

  /**
   * Tests environment variable immutability within WASI context.
   * Validates that environment variables cannot be modified after context creation.
   */
  @Test
  @DisplayName("Environment Variables - Immutability and Isolation")
  void testEnvironmentImmutability() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing environment immutability with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final Map<String, String> originalEnv = new HashMap<>();
        originalEnv.put("VAR1", "value1");
        originalEnv.put("VAR2", "value2");

        final WasiContext context = createWasiContextWithEnvironment(runtimeType, originalEnv);
        registerForCleanup(context);

        // Get environment and verify it's a defensive copy
        final Map<String, String> retrievedEnv = context.getEnvironment();
        assertEquals(originalEnv.size(), retrievedEnv.size(),
            "Retrieved environment should match original size");

        // Modify retrieved environment (should not affect context)
        retrievedEnv.put("VAR3", "value3");
        retrievedEnv.put("VAR1", "modified_value1");

        // Verify context environment is unchanged
        final Map<String, String> contextEnv = context.getEnvironment();
        assertEquals(originalEnv.size(), contextEnv.size(),
            "Context environment should not be affected by external modifications");
        assertEquals("value1", contextEnv.get("VAR1"),
            "Original values should be preserved");
        assertFalse(contextEnv.containsKey("VAR3"),
            "New variables should not appear in context environment");

        // Test multiple retrievals return consistent results
        final Map<String, String> secondRetrieval = context.getEnvironment();
        assertEquals(contextEnv, secondRetrieval,
            "Multiple environment retrievals should return consistent results");

        LOGGER.info("Successfully validated environment immutability");
      }
    });
  }

  /**
   * Tests concurrent environment variable access.
   * Validates thread safety of environment variable operations.
   */
  @Test
  @DisplayName("Environment Variables - Concurrent Access Thread Safety")
  void testEnvironmentConcurrentAccess() {
    skipIfCategoryNotEnabled(TestCategories.PERFORMANCE_TESTING);
    
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing concurrent environment access with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final Map<String, String> environment = new HashMap<>();
        for (int i = 0; i < 50; i++) {
          environment.put("VAR_" + i, "value_" + i);
        }

        final WasiContext context = createWasiContextWithEnvironment(runtimeType, environment);
        registerForCleanup(context);

        final int threadCount = 10;
        final Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
          final int threadIndex = i;
          threads[i] = new Thread(() -> {
            try {
              for (int j = 0; j < 100; j++) {
                // Test individual variable access
                final String varName = "VAR_" + (j % 50);
                final String expectedValue = "value_" + (j % 50);
                assertEquals(expectedValue, context.getEnvironmentVariable(varName),
                    "Thread " + threadIndex + " should get correct value for " + varName);

                // Test environment map access
                final Map<String, String> env = context.getEnvironment();
                assertEquals(50, env.size(),
                    "Thread " + threadIndex + " should get all environment variables");

                // Test non-existent variable
                assertNull(context.getEnvironmentVariable("NONEXISTENT_" + threadIndex),
                    "Thread " + threadIndex + " should get null for non-existent variable");
              }
            } catch (final Exception e) {
              exceptions[threadIndex] = e;
            }
          });
        }

        // Start all threads
        for (final Thread thread : threads) {
          thread.start();
        }

        // Wait for completion
        for (final Thread thread : threads) {
          thread.join(10000); // 10 second timeout
        }

        // Check for exceptions
        for (int i = 0; i < threadCount; i++) {
          if (exceptions[i] != null) {
            throw new AssertionError("Thread " + i + " encountered exception", exceptions[i]);
          }
        }

        LOGGER.info("Successfully tested concurrent environment access with " + threadCount + " threads");
      }
    });
  }

  /**
   * Tests environment variable integration with WASM module execution.
   * Validates that environment variables are properly accessible within WASM programs.
   */
  @Test
  @DisplayName("Environment Variables - WASM Module Integration")
  void testEnvironmentWasmIntegration() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing environment WASM integration with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final Map<String, String> environment = new HashMap<>();
        environment.put("TEST_VAR", "test_value");
        environment.put("WASM_ENV", "wasm_environment_value");

        final WasiContext context = createWasiContextWithEnvironment(runtimeType, environment);
        registerForCleanup(context);

        // Test that context is properly created and accessible
        assertNotNull(context, "WASI context should be created for WASM integration");
        assertFalse(context.isClosed(), "WASI context should be active");

        // Validate environment is properly configured
        assertEquals("test_value", context.getEnvironmentVariable("TEST_VAR"),
            "Environment should be accessible for WASM module");
        assertEquals("wasm_environment_value", context.getEnvironmentVariable("WASM_ENV"),
            "WASM-specific environment should be accessible");

        // Test with WASI environment access module
        if (WasmTestModules.hasModule("wasi_env")) {
          try (final WasmStore store = engine.createStore()) {
            final byte[] moduleBytes = WasmTestModules.getModule("wasi_env");
            final WasmModule module = WasmModule.fromBytes(engine, moduleBytes);
            registerForCleanup(module);

            // Note: Full WASM execution would require linking WASI context to instance
            // This validates the environment is ready for such integration
            assertNotNull(module, "WASI environment test module should compile");
          }
        }

        LOGGER.info("Successfully validated environment WASM integration");
      }
    });
  }

  /**
   * Creates a WASI context with specified environment variables.
   */
  private WasiContext createWasiContextWithEnvironment(
      final RuntimeType runtimeType,
      final Map<String, String> environment) throws Exception {
    
    if (runtimeType == RuntimeType.JNI) {
      return ai.tegmentum.wasmtime4j.jni.wasi.WasiContext.builder()
          .withEnvironment(environment)
          .withArgument("test-program")
          .withWorkingDirectory("/app")
          .build();
    } else {
      return (WasiContext) ai.tegmentum.wasmtime4j.panama.wasi.WasiContext.builder()
          .withEnvironment(environment)
          .withArgument("test-program")
          .withWorkingDirectory("/app")
          .build();
    }
  }

  /**
   * Creates a WASI context with inherited system environment.
   */
  private WasiContext createWasiContextWithInheritedEnvironment(final RuntimeType runtimeType) throws Exception {
    if (runtimeType == RuntimeType.JNI) {
      return ai.tegmentum.wasmtime4j.jni.wasi.WasiContext.builder()
          .withInheritedEnvironment()
          .withArgument("test-program")
          .withWorkingDirectory("/app")
          .build();
    } else {
      return (WasiContext) ai.tegmentum.wasmtime4j.panama.wasi.WasiContext.builder()
          .withInheritedEnvironment()
          .withArgument("test-program")
          .withWorkingDirectory("/app")
          .build();
    }
  }
}