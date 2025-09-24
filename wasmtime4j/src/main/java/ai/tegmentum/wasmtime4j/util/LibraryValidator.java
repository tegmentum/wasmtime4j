package ai.tegmentum.wasmtime4j.util;

import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for validating native library loading and runtime availability. This tool is used in
 * Docker containers and deployment environments to verify that Wasmtime4j can successfully load and
 * initialize.
 */
public final class LibraryValidator {

  private static final Logger LOGGER = Logger.getLogger(LibraryValidator.class.getName());

  /** Private constructor to prevent instantiation. */
  private LibraryValidator() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Main entry point for library validation. Exits with code 0 on success, 1 on failure.
   *
   * @param args command line arguments (not used)
   */
  public static void main(final String[] args) {
    System.out.println("Wasmtime4j Library Validator");
    System.out.println("============================");

    final boolean valid = validateLibraries();
    if (valid) {
      System.out.println("\n✓ Library validation PASSED");
      System.out.println("Wasmtime4j is ready for use");
      System.exit(0);
    } else {
      System.err.println("\n✗ Library validation FAILED");
      System.err.println("Wasmtime4j is not properly configured");
      System.exit(1);
    }
  }

  /**
   * Validates all aspects of library loading and runtime availability.
   *
   * @return true if all validations pass, false otherwise
   */
  public static boolean validateLibraries() {
    try {
      // Print system information
      printSystemInformation();

      // Test runtime availability
      if (!validateRuntimeAvailability()) {
        return false;
      }

      // Test runtime creation
      if (!validateRuntimeCreation()) {
        return false;
      }

      // Test runtime functionality
      if (!validateRuntimeFunctionality()) {
        return false;
      }

      return true;

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Library validation failed with exception", e);
      return false;
    }
  }

  /** Prints system information relevant to WebAssembly runtime. */
  private static void printSystemInformation() {
    System.out.println("System Information:");
    System.out.println("  Java Version: " + System.getProperty("java.version"));
    System.out.println("  Java Vendor: " + System.getProperty("java.vendor"));
    System.out.println("  Java Home: " + System.getProperty("java.home"));
    System.out.println("  OS Name: " + System.getProperty("os.name"));
    System.out.println("  OS Version: " + System.getProperty("os.version"));
    System.out.println("  OS Arch: " + System.getProperty("os.arch"));
    System.out.println("  User Dir: " + System.getProperty("user.dir"));

    // Print native library path information
    final String libraryPath = System.getProperty("java.library.path");
    if (libraryPath != null) {
      System.out.println("  Library Path: " + libraryPath);
    }

    final String wasmtimePath = System.getProperty("wasmtime4j.native.path");
    if (wasmtimePath != null) {
      System.out.println("  Wasmtime4j Native Path: " + wasmtimePath);
    }

    System.out.println();
  }

  /**
   * Validates that runtime implementations are available.
   *
   * @return true if at least one runtime is available, false otherwise
   */
  private static boolean validateRuntimeAvailability() {
    System.out.println("Runtime Availability:");

    boolean anyAvailable = false;

    // Check JNI runtime availability
    try {
      final boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      System.out.println("  JNI Runtime: " + (jniAvailable ? "✓ Available" : "✗ Not Available"));
      if (jniAvailable) {
        anyAvailable = true;
      }
    } catch (final Exception e) {
      System.out.println("  JNI Runtime: ✗ Error checking availability");
      LOGGER.log(Level.WARNING, "Error checking JNI runtime availability", e);
    }

    // Check Panama runtime availability
    try {
      final boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
      System.out.println(
          "  Panama Runtime: " + (panamaAvailable ? "✓ Available" : "✗ Not Available"));
      if (panamaAvailable) {
        anyAvailable = true;
      }
    } catch (final Exception e) {
      System.out.println("  Panama Runtime: ✗ Error checking availability");
      LOGGER.log(Level.WARNING, "Error checking Panama runtime availability", e);
    }

    System.out.println();

    if (!anyAvailable) {
      System.err.println("ERROR: No runtime implementations are available");
      return false;
    }

    return true;
  }

  /**
   * Validates that a runtime can be successfully created.
   *
   * @return true if runtime can be created, false otherwise
   */
  private static boolean validateRuntimeCreation() {
    System.out.println("Runtime Creation:");

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      if (runtime == null) {
        System.err.println("  ✗ Runtime creation returned null");
        return false;
      }

      System.out.println("  ✓ Runtime created successfully");

      // Get runtime information
      final RuntimeInfo info = runtime.getRuntimeInfo();
      if (info != null) {
        System.out.println("  Selected Runtime: " + info.getRuntimeType());
        System.out.println("  Runtime Version: " + info.getRuntimeVersion());

        if (info.getWasmtimeVersion() != null) {
          System.out.println("  Wasmtime Version: " + info.getWasmtimeVersion());
        }

        System.out.println("  Java Version: " + info.getJavaVersion());
        System.out.println("  Platform: " + info.getPlatformInfo());
      } else {
        System.out.println("  ⚠ Runtime information not available");
      }

      System.out.println();
      return true;

    } catch (final Exception e) {
      System.err.println("  ✗ Runtime creation failed");
      LOGGER.log(Level.SEVERE, "Runtime creation failed", e);
      return false;
    }
  }

  /**
   * Validates basic runtime functionality.
   *
   * @return true if runtime functionality works, false otherwise
   */
  private static boolean validateRuntimeFunctionality() {
    System.out.println("Runtime Functionality:");

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      // Test engine creation
      try {
        runtime.createEngine().close();
        System.out.println("  ✓ Engine creation works");
      } catch (final Exception e) {
        System.err.println("  ✗ Engine creation failed");
        LOGGER.log(Level.SEVERE, "Engine creation failed", e);
        return false;
      }

      // Note: More comprehensive functionality testing is done by HealthCheck
      System.out.println("  ✓ Basic functionality validated");
      System.out.println();
      return true;

    } catch (final Exception e) {
      System.err.println("  ✗ Runtime functionality validation failed");
      LOGGER.log(Level.SEVERE, "Runtime functionality validation failed", e);
      return false;
    }
  }

  /**
   * Validates that native libraries can be loaded. This is a lightweight check for basic library
   * loading.
   *
   * @return true if libraries can be loaded, false otherwise
   */
  public static boolean canLoadLibraries() {
    try {
      // Try to check if any runtime is available
      return WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI)
          || WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error checking library loading capability", e);
      return false;
    }
  }

  /**
   * Gets a summary of available runtimes for diagnostic purposes.
   *
   * @return summary string of runtime availability
   */
  public static String getRuntimeSummary() {
    final StringBuilder summary = new StringBuilder();
    summary.append("Runtime Summary: ");

    try {
      final boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      final boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

      if (jniAvailable && panamaAvailable) {
        summary.append("Both JNI and Panama runtimes available");
      } else if (jniAvailable) {
        summary.append("JNI runtime available, Panama not available");
      } else if (panamaAvailable) {
        summary.append("Panama runtime available, JNI not available");
      } else {
        summary.append("No runtimes available");
      }

    } catch (final Exception e) {
      summary.append("Error checking runtime availability: ").append(e.getMessage());
    }

    return summary.toString();
  }
}
