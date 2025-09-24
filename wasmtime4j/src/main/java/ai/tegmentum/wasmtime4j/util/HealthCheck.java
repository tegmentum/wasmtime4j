package ai.tegmentum.wasmtime4j.util;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Health check utility for validating Wasmtime4j runtime functionality. This class provides basic
 * health verification for production deployments.
 */
public final class HealthCheck {

  private static final Logger LOGGER = Logger.getLogger(HealthCheck.class.getName());

  /** Simple WebAssembly module for health checks (adds two numbers). */
  private static final byte[] HEALTH_CHECK_WASM = {
    0x00,
    0x61,
    0x73,
    0x6d, // WASM magic number
    0x01,
    0x00,
    0x00,
    0x00, // WASM version
    0x01,
    0x07, // Type section
    0x01, // 1 type
    0x60,
    0x02,
    0x7f,
    0x7f,
    0x01,
    0x7f, // (i32, i32) -> i32
    0x03,
    0x02, // Function section
    0x01,
    0x00, // 1 function, type 0
    0x07,
    0x07, // Export section
    0x01,
    0x03,
    0x61,
    0x64,
    0x64,
    0x00,
    0x00, // export "add" as function 0
    0x0a,
    0x09, // Code section
    0x01,
    0x07,
    0x00, // 1 function, 7 bytes, 0 locals
    0x20,
    0x00, // local.get 0
    0x20,
    0x01, // local.get 1
    0x6a, // i32.add
    0x0b // end
  };

  /** Private constructor to prevent instantiation. */
  private HealthCheck() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Main entry point for health check execution. Exits with code 0 on success, 1 on failure.
   *
   * @param args command line arguments (not used)
   */
  public static void main(final String[] args) {
    final boolean healthy = performHealthCheck();
    if (healthy) {
      System.out.println("Health check PASSED: Wasmtime4j is operational");
      System.exit(0);
    } else {
      System.err.println("Health check FAILED: Wasmtime4j is not operational");
      System.exit(1);
    }
  }

  /**
   * Performs a comprehensive health check of the Wasmtime4j runtime.
   *
   * @return true if all health checks pass, false otherwise
   */
  public static boolean performHealthCheck() {
    try {
      LOGGER.info("Starting Wasmtime4j health check");

      // Test 1: Runtime creation
      if (!testRuntimeCreation()) {
        return false;
      }

      // Test 2: Module compilation and execution
      if (!testModuleExecution()) {
        return false;
      }

      // Test 3: Runtime information
      if (!testRuntimeInformation()) {
        return false;
      }

      LOGGER.info("All health checks passed successfully");
      return true;

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Health check failed with exception", e);
      return false;
    }
  }

  /**
   * Tests basic runtime creation and disposal.
   *
   * @return true if runtime can be created and closed, false otherwise
   */
  private static boolean testRuntimeCreation() {
    try {
      LOGGER.fine("Testing runtime creation");
      try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
        if (runtime == null) {
          LOGGER.severe("Runtime creation returned null");
          return false;
        }
        LOGGER.fine("Runtime creation test passed");
        return true;
      }
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Runtime creation test failed", e);
      return false;
    }
  }

  /**
   * Tests WebAssembly module compilation and execution.
   *
   * @return true if module can be compiled and executed, false otherwise
   */
  private static boolean testModuleExecution() {
    try {
      LOGGER.fine("Testing module compilation and execution");
      try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
        try (Engine engine = runtime.createEngine()) {
          try (Module module = engine.compileModule(HEALTH_CHECK_WASM)) {
            try (Store store = engine.createStore()) {
              try (Instance instance = module.instantiate(store)) {

                // Get the exported "add" function
                final Optional<WasmFunction> addFunc = instance.getFunction("add");
                if (!addFunc.isPresent()) {
                  LOGGER.severe("Health check module 'add' function not found");
                  return false;
                }

                // Call the function: add(2, 3) should return 5
                final WasmValue[] result = addFunc.get().call(WasmValue.i32(2), WasmValue.i32(3));

                if (result.length != 1) {
                  LOGGER.severe("Expected 1 result, got " + result.length);
                  return false;
                }

                final int resultValue = result[0].asInt();
                if (resultValue != 5) {
                  LOGGER.severe("Expected result 5, got " + resultValue);
                  return false;
                }

                LOGGER.fine("Module execution test passed");
                return true;
              }
            }
          }
        }
      }
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Module execution test failed", e);
      return false;
    }
  }

  /**
   * Tests runtime information retrieval.
   *
   * @return true if runtime information can be retrieved, false otherwise
   */
  private static boolean testRuntimeInformation() {
    try {
      LOGGER.fine("Testing runtime information retrieval");
      try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
        final RuntimeInfo info = runtime.getRuntimeInfo();
        if (info == null) {
          LOGGER.severe("Runtime information is null");
          return false;
        }

        // Validate basic information is available
        if (info.getRuntimeType() == null) {
          LOGGER.severe("Runtime type is null");
          return false;
        }

        if (info.getRuntimeVersion() == null || info.getRuntimeVersion().isEmpty()) {
          LOGGER.severe("Runtime version is null or empty");
          return false;
        }

        LOGGER.fine("Runtime information test passed");
        LOGGER.info("Runtime type: " + info.getRuntimeType());
        LOGGER.info("Runtime version: " + info.getRuntimeVersion());

        return true;
      }
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Runtime information test failed", e);
      return false;
    }
  }

  /**
   * Performs a quick health check suitable for readiness probes. This is a lighter version that
   * just verifies runtime availability.
   *
   * @return true if runtime is ready, false otherwise
   */
  public static boolean isReady() {
    try {
      try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
        return runtime != null;
      }
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Readiness check failed", e);
      return false;
    }
  }

  /**
   * Performs a liveness check suitable for liveness probes. This just checks if the application is
   * responsive.
   *
   * @return always true unless the application is completely unresponsive
   */
  public static boolean isLive() {
    // For now, just return true if we can execute this method
    // In a real application, this might check thread pools, etc.
    return true;
  }
}
