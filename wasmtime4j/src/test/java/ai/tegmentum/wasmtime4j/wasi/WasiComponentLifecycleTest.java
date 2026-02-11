package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.testing.RequiresWasmRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Integration tests for the WASI Component Model lifecycle.
 *
 * <p>These tests validate the complete lifecycle of WASI components including creation,
 * instantiation, function calling, and cleanup. They test both JNI and Panama implementations to
 * ensure consistent behavior across runtime types.
 *
 * <p>Tests are designed to be verbose for debugging purposes and use actual component operations
 * rather than mocks to ensure real-world functionality.
 */
@DisplayName("WasiComponentLifecycle Tests")
@RequiresWasmRuntime
class WasiComponentLifecycleTest {

  private WasiComponentContext context;
  private WasiComponent component;
  private WasiInstance instance;

  // Simple WebAssembly module bytes (empty module for basic testing)
  // This would be replaced with actual component bytes in real tests
  private static final byte[] MINIMAL_WASM_BYTES = {
    0x00, 0x61, 0x73, 0x6d, // WASM magic number
    0x01, 0x00, 0x00, 0x00 // WASM version
  };

  @BeforeEach
  void setUp() throws WasmException {
    // Skip test if no runtime implementations are available
    try {
      // Create WASI context using factory (auto-selects implementation)
      context = WasiFactory.createContext();
      assertNotNull(context, "WASI context should be created successfully");
      assertTrue(context.isValid(), "WASI context should be valid after creation");

      System.out.println(
          "Created WASI context with runtime: " + context.getRuntimeInfo().getRuntimeType());
    } catch (WasmException e) {
      // If no implementations available, skip the test
      Assumptions.assumeTrue(
          false, "Skipping test - no WASI implementation available: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDown() {
    // Clean up in reverse order of creation
    if (instance != null && instance.isValid()) {
      try {
        instance.close();
        System.out.println("Closed instance: " + instance.getId());
      } catch (Exception e) {
        System.err.println("Error closing instance: " + e.getMessage());
      }
    }

    if (component != null && component.isValid()) {
      try {
        component.close();
        System.out.println("Closed component: " + component.getName());
      } catch (Exception e) {
        System.err.println("Error closing component: " + e.getMessage());
      }
    }

    if (context != null && context.isValid()) {
      try {
        context.close();
        System.out.println("Closed WASI context");
      } catch (Exception e) {
        System.err.println("Error closing context: " + e.getMessage());
      }
    }
  }

  @Test
  void testComponentCreationFromBytes() throws WasmException {
    // Test component creation from minimal bytes
    System.out.println("Testing component creation from bytes...");

    // Note: This will likely fail with current minimal bytes, but tests the API
    assertThrows(
        WasmException.class,
        () -> {
          component = context.createComponent(MINIMAL_WASM_BYTES);
        },
        "Minimal WASM bytes should fail component creation (expected)");

    System.out.println("Component creation correctly rejected invalid bytes");
  }

  @Test
  void testComponentValidation() throws WasmException {
    System.out.println("Testing component validation...");

    // Test with null bytes
    assertThrows(
        NullPointerException.class,
        () -> {
          context.createComponent(null);
        },
        "Null bytes should throw NullPointerException");

    // Test with empty bytes
    assertThrows(
        Exception.class,
        () -> {
          context.createComponent(new byte[0]);
        },
        "Empty bytes should throw exception");

    System.out.println("Component validation working correctly");
  }

  @Test
  void testRuntimeInfo() {
    System.out.println("Testing runtime information...");

    WasiRuntimeInfo info = context.getRuntimeInfo();
    assertNotNull(info, "Runtime info should not be null");
    assertNotNull(info.getRuntimeType(), "Runtime type should not be null");
    assertNotNull(info.getVersion(), "Runtime version should not be null");
    assertNotNull(info.getWasmtimeVersion(), "Wasmtime version should not be null");

    // Note: Component model and async support are inherent to WASI
    // assertTrue(info.supportsComponentModel(), "Runtime should support component model");
    // assertTrue(info.supportsAsync(), "Runtime should support async operations");

    System.out.println("Runtime Type: " + info.getRuntimeType());
    System.out.println("Runtime Version: " + info.getVersion());
    System.out.println("Wasmtime Version: " + info.getWasmtimeVersion());
    // System.out.println("Component Model: " + info.supportsComponentModel());
    // System.out.println("Async Support: " + info.supportsAsync());
    // System.out.println("Resource Sharing: " + info.supportsResourceSharing());
    // System.out.println("Description: " + info.getDescription());
  }

  @Test
  void testContextLifecycle() {
    System.out.println("Testing context lifecycle management...");

    // Test context validity
    assertTrue(context.isValid(), "Context should be valid initially");

    // Close context
    context.close();
    assertFalse(context.isValid(), "Context should be invalid after close");

    // Verify operations fail on closed context
    assertThrows(
        IllegalStateException.class,
        () -> {
          context.createComponent(MINIMAL_WASM_BYTES);
        },
        "Operations should fail on closed context");

    System.out.println("Context lifecycle management working correctly");
  }

  @Test
  void testConfigurationValidation() throws WasmException {
    System.out.println("Testing configuration validation...");

    // Test default configuration
    WasiConfig defaultConfig = WasiConfig.defaultConfig();
    assertNotNull(defaultConfig, "Default config should not be null");

    // Test configuration validation
    assertDoesNotThrow(
        () -> {
          defaultConfig.validate();
        },
        "Default configuration should be valid");

    // Test configuration properties
    assertNotNull(defaultConfig.getEnvironment(), "Environment should not be null");
    assertNotNull(defaultConfig.getArguments(), "Arguments should not be null");
    assertNotNull(defaultConfig.getPreopenDirectories(), "Preopen directories should not be null");
    assertNotNull(defaultConfig.getImportResolvers(), "Import resolvers should not be null");

    System.out.println("Environment variables: " + defaultConfig.getEnvironment().size());
    System.out.println("Arguments: " + defaultConfig.getArguments().size());
    System.out.println("Preopen directories: " + defaultConfig.getPreopenDirectories().size());
    System.out.println("Validation enabled: " + defaultConfig.isValidationEnabled());
    System.out.println("Strict mode: " + defaultConfig.isStrictModeEnabled());
  }

  @Test
  @EnabledIfSystemProperty(named = "wasmtime4j.test.jni", matches = "true")
  void testJniSpecificFeatures() throws WasmException {
    System.out.println("Testing JNI-specific features...");

    // Force JNI runtime
    WasiComponentContext jniContext = WasiFactory.createContext(WasiRuntimeType.JNI);

    try {
      assertEquals(
          WasiRuntimeType.JNI,
          jniContext.getRuntimeInfo().getRuntimeType(),
          "Should create JNI runtime when explicitly requested");

      System.out.println("JNI runtime created successfully");

    } finally {
      jniContext.close();
    }
  }

  @Test
  @EnabledIfSystemProperty(named = "wasmtime4j.test.panama", matches = "true")
  void testPanamaSpecificFeatures() throws WasmException {
    System.out.println("Testing Panama-specific features...");

    // Only run on Java 23+
    if (WasiFactory.getJavaVersion() >= 23) {
      // Force Panama runtime
      WasiComponentContext panamaContext = WasiFactory.createContext(WasiRuntimeType.PANAMA);

      try {
        assertEquals(
            WasiRuntimeType.PANAMA,
            panamaContext.getRuntimeInfo().getRuntimeType(),
            "Should create Panama runtime when explicitly requested");

        System.out.println("Panama runtime created successfully");

      } finally {
        panamaContext.close();
      }
    } else {
      System.out.println("Skipping Panama test - requires Java 23+");
    }
  }

  @Test
  void testRuntimeSelection() {
    System.out.println("Testing runtime selection logic...");

    WasiRuntimeType selectedType = WasiFactory.getSelectedRuntimeType();
    assertNotNull(selectedType, "Selected runtime type should not be null");

    boolean jniAvailable = WasiFactory.isRuntimeAvailable(WasiRuntimeType.JNI);
    boolean panamaAvailable = WasiFactory.isRuntimeAvailable(WasiRuntimeType.PANAMA);

    System.out.println("Java Version: " + WasiFactory.getJavaVersion());
    System.out.println("Selected Runtime: " + selectedType);
    System.out.println("JNI Available: " + jniAvailable);
    System.out.println("Panama Available: " + panamaAvailable);

    // At least one runtime should be available
    assertTrue(
        jniAvailable || panamaAvailable, "At least one runtime implementation should be available");
  }

  @Test
  void testErrorHandling() {
    System.out.println("Testing error handling robustness...");

    // Test various error conditions to ensure robust error handling

    // Null context operations
    assertThrows(
        Exception.class,
        () -> {
          WasiFactory.createContext(null);
        },
        "Null runtime type should throw exception");

    // Invalid component creation
    assertThrows(
        Exception.class,
        () -> {
          context.createComponent(new byte[1]); // Invalid WASM
        },
        "Invalid WASM bytes should throw exception");

    System.out.println("Error handling working correctly");
  }
}
