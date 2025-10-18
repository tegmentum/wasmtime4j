package ai.tegmentum.wasmtime4j.jni.nativelib;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Central registry for all native method bindings used by the JNI implementation.
 *
 * <p>This class manages the initialization of native methods and provides a single point of control
 * for loading and validating JNI bindings. It ensures that the native library is loaded before any
 * native methods are called.
 *
 * <p>The class follows defensive programming practices to prevent JVM crashes and provides
 * comprehensive validation of native method availability.
 */
public final class NativeMethodBindings {

  private static final Logger LOGGER = Logger.getLogger(NativeMethodBindings.class.getName());

  /** Flag to track if native methods have been initialized. */
  private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

  /** Private constructor to prevent instantiation of utility class. */
  private NativeMethodBindings() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Initializes all native method bindings.
   *
   * <p>This method must be called before any native methods are used. It loads the native library
   * and validates that all required methods are available. This method is thread-safe and will only
   * perform initialization once.
   *
   * @throws RuntimeException if initialization fails or required methods are missing
   */
  public static void initialize() {
    if (INITIALIZED.get()) {
      return; // Already initialized
    }

    synchronized (NativeMethodBindings.class) {
      if (INITIALIZED.get()) {
        return; // Double-check after acquiring lock
      }

      try {
        // Load the native library first
        NativeLibraryLoader.loadLibrary();
        System.err.println("[JAVA DEBUG] Library load info: " + NativeLibraryLoader.getLoadInfo());
        System.err.flush();
        LOGGER.info("Native library loaded successfully");

        // Validate that critical native methods are available
        validateNativeMethods();
        LOGGER.info("Native method validation completed");

        // Perform any additional native initialization
        performNativeInitialization();
        LOGGER.info("Native initialization completed");

        INITIALIZED.set(true);
        LOGGER.info("Native method bindings initialized successfully");

      } catch (final Exception e) {
        LOGGER.severe("Failed to initialize native method bindings: " + e.getMessage());
        throw new RuntimeException("Native method binding initialization failed", e);
      }
    }
  }

  /**
   * Checks if native method bindings have been initialized.
   *
   * @return true if initialized, false otherwise
   */
  public static boolean isInitialized() {
    return INITIALIZED.get();
  }

  /**
   * Ensures that native method bindings are initialized before proceeding.
   *
   * <p>This is a convenience method that should be called at the beginning of any operation that
   * requires native methods. It will initialize the bindings if they haven't been initialized yet.
   *
   * @throws RuntimeException if initialization fails
   */
  public static void ensureInitialized() {
    if (!INITIALIZED.get()) {
      initialize();
    }
  }

  /**
   * Gets the version of the native Wasmtime library.
   *
   * <p>This method can be used to verify that the correct version of the native library is loaded
   * and accessible.
   *
   * @return the Wasmtime version string
   * @throws RuntimeException if the version cannot be retrieved
   */
  public static String getNativeLibraryVersion() {
    ensureInitialized();
    try {
      return nativeGetWasmtimeVersion();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to get native library version", e);
    }
  }

  /**
   * Gets information about the loaded native library.
   *
   * @return a string with library information
   */
  public static String getLibraryInfo() {
    final StringBuilder info = new StringBuilder();
    info.append("Native Method Bindings Status:\n");
    info.append("  Initialized: ").append(isInitialized()).append("\n");
    info.append("  Platform: ").append(NativeLibraryLoader.getPlatformInfo()).append("\n");

    if (isInitialized()) {
      try {
        info.append("  Wasmtime Version: ").append(getNativeLibraryVersion()).append("\n");
      } catch (final Exception e) {
        info.append("  Wasmtime Version: Error retrieving (").append(e.getMessage()).append(")\n");
      }
    }

    return info.toString();
  }

  /**
   * Validates that all critical native methods are available.
   *
   * <p>This method performs basic validation by calling native methods that should always be
   * available. If any critical method is missing, it throws an exception.
   *
   * @throws RuntimeException if validation fails
   */
  private static void validateNativeMethods() {
    try {
      // DEBUG: Confirm we're calling the native method
      System.err.println("[JAVA DEBUG] About to call nativeGetWasmtimeVersion()");
      System.err.flush();

      // Test basic version retrieval to ensure native library is functional
      final String version = nativeGetWasmtimeVersion();

      System.err.println(
          "[JAVA DEBUG] Returned from nativeGetWasmtimeVersion(), result: " + version);
      System.err.flush();

      if (version == null || version.trim().isEmpty()) {
        throw new RuntimeException("Native library version is null or empty");
      }
      LOGGER.fine("Native library version: " + version);

      // Test basic runtime operations to ensure core functionality is available
      final long testHandle = nativeCreateRuntime();
      if (testHandle != 0) {
        nativeDestroyRuntime(testHandle);
        LOGGER.fine("Native runtime creation/destruction test passed");
      } else {
        LOGGER.warning(
            "Native runtime creation returned null handle (may be normal during initialization)");
      }

    } catch (final UnsatisfiedLinkError e) {
      throw new RuntimeException("Required native methods are not available", e);
    } catch (final Exception e) {
      throw new RuntimeException("Native method validation failed", e);
    }
  }

  /**
   * Performs any additional native initialization required by the library.
   *
   * <p>This method can be used to call native initialization functions that need to be executed
   * once after the library is loaded.
   */
  private static void performNativeInitialization() {
    try {
      // Call native initialization if required
      nativeInitialize();
      LOGGER.fine("Native library initialization completed");
    } catch (final UnsatisfiedLinkError e) {
      // Native initialization method may not exist in all versions
      LOGGER.fine("Native initialization method not available (this may be normal)");
    } catch (final Exception e) {
      LOGGER.warning("Native initialization failed: " + e.getMessage());
      throw new RuntimeException("Native initialization failed", e);
    }
  }

  // Native method declarations used for validation and initialization

  /**
   * Gets the version of the native Wasmtime library.
   *
   * @return the version string
   */
  private static native String nativeGetWasmtimeVersion();

  /**
   * Creates a test runtime for validation purposes.
   *
   * @return native runtime handle or 0 on failure
   */
  private static native long nativeCreateRuntime();

  /**
   * Destroys a test runtime used for validation.
   *
   * @param runtimeHandle the native runtime handle
   */
  private static native void nativeDestroyRuntime(long runtimeHandle);

  /** Performs native library initialization (optional method). */
  private static native void nativeInitialize();
}
