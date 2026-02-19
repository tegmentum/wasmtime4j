package ai.tegmentum.wasmtime4j.factory;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;
import java.util.logging.Logger;

/**
 * Factory for creating WebAssembly runtime instances.
 *
 * <p>This factory automatically selects the appropriate runtime implementation based on the Java
 * version and available native bindings. It can also be configured to use a specific runtime type.
 *
 * <p>Runtime selection priority:
 *
 * <ol>
 *   <li>Manual override via system property: {@code wasmtime4j.runtime}
 *   <li>Panama FFI for Java 23+ (if available)
 *   <li>JNI fallback for all Java versions
 * </ol>
 *
 * @since 1.0.0
 */
public final class WasmRuntimeFactory {

  private static final Logger logger = Logger.getLogger(WasmRuntimeFactory.class.getName());

  /** System property to override runtime selection. */
  public static final String RUNTIME_PROPERTY = "wasmtime4j.runtime";

  /** Runtime type value for JNI implementation. */
  public static final String RUNTIME_JNI = "jni";

  /** Runtime type value for Panama implementation. */
  public static final String RUNTIME_PANAMA = "panama";

  // Performance optimization: cache runtime availability checks
  private static volatile Boolean jniAvailable;
  private static volatile Boolean panamaAvailable;
  private static volatile RuntimeType selectedRuntimeType;

  private WasmRuntimeFactory() {
    // Utility class - no instantiation
  }

  /**
   * Creates a new WebAssembly runtime with automatic implementation selection.
   *
   * <p>This method automatically selects the best available runtime implementation based on the
   * Java version and system configuration. It prefers Panama FFI on Java 23+ but falls back to JNI
   * if Panama is not available.
   *
   * @return a new WasmRuntime instance
   * @throws WasmException if no suitable runtime implementation is available
   */
  public static WasmRuntime create() throws WasmException {
    final RuntimeType runtimeType = selectRuntimeType();
    return create(runtimeType);
  }

  /**
   * Creates a new WebAssembly runtime with the specified implementation type.
   *
   * @param runtimeType the type of runtime implementation to create
   * @return a new WasmRuntime instance of the specified type
   * @throws WasmException if the specified runtime type is not available
   * @throws IllegalArgumentException if runtimeType is null
   */
  public static WasmRuntime create(final RuntimeType runtimeType) throws WasmException {
    if (runtimeType == null) {
      throw new IllegalArgumentException("Runtime type cannot be null");
    }

    logger.info(
        "Creating WebAssembly runtime with type: " + PlatformDetector.sanitizeForLog(runtimeType.toString()));

    // Check if the requested runtime is available before attempting to create it
    if (!isRuntimeAvailable(runtimeType)) {
      // Try to provide a fallback
      if (runtimeType == RuntimeType.JNI && isPanamaRuntimeAvailable()) {
        logger.warning("JNI runtime not available, falling back to Panama runtime");
        return createPanamaRuntime();
      } else if (runtimeType == RuntimeType.PANAMA && isJniRuntimeAvailable()) {
        logger.warning("Panama runtime not available, falling back to JNI runtime");
        return createJniRuntime();
      } else {
        throw new WasmException(
            "Requested runtime type "
                + runtimeType
                + " is not available and no suitable fallback found");
      }
    }

    switch (runtimeType) {
      case JNI:
        return createJniRuntime();
      case PANAMA:
        return createPanamaRuntime();
      default:
        throw new WasmException("Unknown runtime type: " + runtimeType);
    }
  }

  /**
   * Gets the runtime type that would be automatically selected.
   *
   * @return the runtime type that would be selected by {@link #create()}
   */
  public static RuntimeType getSelectedRuntimeType() {
    return selectRuntimeType();
  }

  /**
   * Checks if the specified runtime type is available.
   *
   * @param runtimeType the runtime type to check
   * @return true if the runtime type is available
   */
  public static boolean isRuntimeAvailable(final RuntimeType runtimeType) {
    try {
      switch (runtimeType) {
        case JNI:
          return isJniRuntimeAvailable();
        case PANAMA:
          return isPanamaRuntimeAvailable();
        default:
          return false;
      }
    } catch (final Exception e) {
      logger.warning(
          "Error checking runtime availability for "
              + PlatformDetector.sanitizeForLog(runtimeType.toString())
              + ": "
              + PlatformDetector.sanitizeForLog(e.getMessage()));
      return false;
    }
  }

  /**
   * Gets the Java version as a major version number.
   *
   * @return the Java major version (e.g., 8, 11, 17, 23)
   */
  public static int getJavaVersion() {
    final String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      return Integer.parseInt(version.substring(2, 3));
    } else {
      // Strip any suffix after a dot or hyphen (e.g., "26-ea", "23.0.1")
      String majorStr = version;
      final int dot = majorStr.indexOf('.');
      if (dot != -1) {
        majorStr = majorStr.substring(0, dot);
      }
      final int hyphen = majorStr.indexOf('-');
      if (hyphen != -1) {
        majorStr = majorStr.substring(0, hyphen);
      }
      return Integer.parseInt(majorStr);
    }
  }

  /**
   * Clears cached runtime availability and selection information. This method is primarily intended
   * for testing purposes.
   */
  public static void clearCache() {
    synchronized (WasmRuntimeFactory.class) {
      jniAvailable = null;
      panamaAvailable = null;
      selectedRuntimeType = null;
    }
  }

  private static RuntimeType selectRuntimeType() {
    // Use cached result if no system property override
    final String override = System.getProperty(RUNTIME_PROPERTY);
    if (override == null && selectedRuntimeType != null) {
      return selectedRuntimeType;
    }

    synchronized (WasmRuntimeFactory.class) {
      // Double-check pattern for cache
      if (override == null && selectedRuntimeType != null) {
        return selectedRuntimeType;
      }

      // Check for manual override
      if (override != null) {
        if (RUNTIME_JNI.equalsIgnoreCase(override)) {
          logger.info("Runtime manually set to JNI via system property");
          return RuntimeType.JNI;
        } else if (RUNTIME_PANAMA.equalsIgnoreCase(override)) {
          logger.info("Runtime manually set to Panama via system property");
          return RuntimeType.PANAMA;
        } else {
          logger.warning(
              "Unknown runtime type in system property: "
                  + PlatformDetector.sanitizeForLog(override)
                  + ", using automatic selection");
        }
      }

      // Automatic selection based on Java version
      final int javaVersion = getJavaVersion();
      final RuntimeType selected;

      if (javaVersion >= 23 && isPanamaRuntimeAvailable()) {
        logger.info(
            "Auto-selected Panama runtime for Java " + PlatformDetector.sanitizeForLog(String.valueOf(javaVersion)));
        selected = RuntimeType.PANAMA;
      } else {
        logger.info(
            "Auto-selected JNI runtime for Java " + PlatformDetector.sanitizeForLog(String.valueOf(javaVersion)));
        selected = RuntimeType.JNI;
      }

      // Cache the result only if no override property was set
      if (override == null) {
        selectedRuntimeType = selected;
      }

      return selected;
    }
  }

  private static WasmRuntime createJniRuntime() throws WasmException {
    try {
      // This will be implemented by loading the JNI implementation class
      final Class<?> jniRuntimeClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniWasmRuntime");
      return (WasmRuntime) jniRuntimeClass.getDeclaredConstructor().newInstance();
    } catch (final Exception e) {
      throw new WasmException("Failed to create JNI runtime: " + e.getMessage(), e);
    }
  }

  private static WasmRuntime createPanamaRuntime() throws WasmException {
    try {
      // This will be implemented by loading the Panama implementation class
      final Class<?> panamaRuntimeClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaWasmRuntime");
      return (WasmRuntime) panamaRuntimeClass.getDeclaredConstructor().newInstance();
    } catch (final Exception e) {
      throw new WasmException("Failed to create Panama runtime: " + e.getMessage(), e);
    }
  }

  private static boolean isJniRuntimeAvailable() {
    // Use cached result if available
    if (jniAvailable != null) {
      return jniAvailable;
    }

    synchronized (WasmRuntimeFactory.class) {
      // Double-check pattern
      if (jniAvailable != null) {
        return jniAvailable;
      }

      try {
        Class.forName("ai.tegmentum.wasmtime4j.jni.JniWasmRuntime");
        jniAvailable = Boolean.TRUE;
        return true;
      } catch (final ClassNotFoundException e) {
        logger.fine("JNI runtime class not found: " + PlatformDetector.sanitizeForLog(e.getMessage()));
        jniAvailable = Boolean.FALSE;
        return false;
      } catch (final ExceptionInInitializerError e) {
        logger.warning("JNI runtime initialization failed: " + PlatformDetector.sanitizeForLog(e.getMessage()));
        if (e.getCause() != null) {
          logger.warning("  Caused by: " + PlatformDetector.sanitizeForLog(e.getCause().toString()));
        }
        jniAvailable = Boolean.FALSE;
        return false;
      } catch (final Exception e) {
        logger.warning(
            "Unexpected error checking JNI runtime availability: "
                + PlatformDetector.sanitizeForLog(e.getMessage()));
        jniAvailable = Boolean.FALSE;
        return false;
      }
    }
  }

  private static boolean isPanamaRuntimeAvailable() {
    // Use cached result if available
    if (panamaAvailable != null) {
      return panamaAvailable;
    }

    synchronized (WasmRuntimeFactory.class) {
      // Double-check pattern
      if (panamaAvailable != null) {
        return panamaAvailable;
      }

      try {
        Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaWasmRuntime");
        panamaAvailable = Boolean.TRUE;
        return true;
      } catch (final ClassNotFoundException e) {
        logger.fine("Panama runtime class not found: " + PlatformDetector.sanitizeForLog(e.getMessage()));
        panamaAvailable = Boolean.FALSE;
        return false;
      } catch (final ExceptionInInitializerError e) {
        logger.warning("Panama runtime initialization failed: " + PlatformDetector.sanitizeForLog(e.getMessage()));
        if (e.getCause() != null) {
          logger.warning("  Caused by: " + PlatformDetector.sanitizeForLog(e.getCause().toString()));
        }
        panamaAvailable = Boolean.FALSE;
        return false;
      } catch (final Exception e) {
        logger.warning(
            "Unexpected error checking Panama runtime availability: "
                + PlatformDetector.sanitizeForLog(e.getMessage()));
        panamaAvailable = Boolean.FALSE;
        return false;
      }
    }
  }
}
