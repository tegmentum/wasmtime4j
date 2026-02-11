package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.logging.Logger;

/**
 * Factory for creating WASI (WebAssembly System Interface) context instances.
 *
 * <p>This factory automatically selects the appropriate WASI runtime implementation based on the
 * Java version and available native bindings. It can also be configured to use a specific runtime
 * type.
 *
 * <p>Runtime selection priority:
 *
 * <ol>
 *   <li>Manual override via system property: {@code wasmtime4j.wasi.runtime}
 *   <li>Panama FFI for Java 23+ (if available)
 *   <li>JNI fallback for all Java versions
 * </ol>
 *
 * @since 1.0.0
 */
public final class WasiFactory {

  private static final Logger logger = Logger.getLogger(WasiFactory.class.getName());

  /** System property to override WASI runtime selection. */
  public static final String WASI_RUNTIME_PROPERTY = "wasmtime4j.wasi.runtime";

  /** Runtime type value for JNI implementation. */
  public static final String WASI_RUNTIME_JNI = "jni";

  /** Runtime type value for Panama implementation. */
  public static final String WASI_RUNTIME_PANAMA = "panama";

  private WasiFactory() {
    // Utility class - no instantiation
  }

  /**
   * Sanitizes a string for safe logging by removing CRLF injection characters.
   *
   * @param input the string to sanitize for logging
   * @return the sanitized string safe for logging
   */
  private static String sanitizeForLog(final String input) {
    if (input == null) {
      return "null";
    }
    // Remove all control and format characters to prevent log injection
    return input.replaceAll("[\\p{Cntrl}\\p{Cf}]", "_");
  }

  /**
   * Creates a new WASI context with automatic implementation selection.
   *
   * <p>This method automatically selects the best available WASI runtime implementation based on
   * the Java version and system configuration. It prefers Panama FFI on Java 23+ but falls back to
   * JNI if Panama is not available.
   *
   * @return a new WasiComponentContext instance
   * @throws WasmException if no suitable WASI runtime implementation is available
   */
  public static WasiComponentContext createContext() throws WasmException {
    final WasiRuntimeType runtimeType = selectRuntimeType();
    return createContext(runtimeType);
  }

  /**
   * Creates a new WASI context with the specified implementation type.
   *
   * @param runtimeType the type of WASI runtime implementation to create
   * @return a new WasiComponentContext instance of the specified type
   * @throws WasmException if the specified runtime type is not available
   * @throws IllegalArgumentException if runtimeType is null
   */
  public static WasiComponentContext createContext(final WasiRuntimeType runtimeType)
      throws WasmException {
    if (runtimeType == null) {
      throw new IllegalArgumentException("WASI runtime type cannot be null");
    }

    logger.info("Creating WASI context with type: " + sanitizeForLog(runtimeType.toString()));

    switch (runtimeType) {
      case JNI:
        return createJniContext();
      case PANAMA:
        return createPanamaContext();
      default:
        throw new WasmException("Unknown WASI runtime type: " + runtimeType);
    }
  }

  /**
   * Gets the WASI runtime type that would be automatically selected.
   *
   * @return the runtime type that would be selected by {@link #createContext()}
   */
  public static WasiRuntimeType getSelectedRuntimeType() {
    return selectRuntimeType();
  }

  /**
   * Checks if the specified WASI runtime type is available.
   *
   * @param runtimeType the runtime type to check
   * @return true if the runtime type is available
   */
  public static boolean isRuntimeAvailable(final WasiRuntimeType runtimeType) {
    if (runtimeType == null) {
      return false;
    }

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
          "Error checking WASI runtime availability for "
              + sanitizeForLog(runtimeType.toString())
              + ": "
              + sanitizeForLog(e.getMessage()));
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
      final int dot = version.indexOf('.');
      if (dot != -1) {
        return Integer.parseInt(version.substring(0, dot));
      } else {
        return Integer.parseInt(version);
      }
    }
  }

  private static WasiRuntimeType selectRuntimeType() {
    // Check for manual override
    final String override = System.getProperty(WASI_RUNTIME_PROPERTY);
    if (override != null) {
      if (WASI_RUNTIME_JNI.equalsIgnoreCase(override)) {
        logger.info("WASI runtime manually set to JNI via system property");
        return WasiRuntimeType.JNI;
      } else if (WASI_RUNTIME_PANAMA.equalsIgnoreCase(override)) {
        logger.info("WASI runtime manually set to Panama via system property");
        return WasiRuntimeType.PANAMA;
      } else {
        logger.warning(
            "Unknown WASI runtime type in system property: "
                + sanitizeForLog(override)
                + ", using automatic selection");
      }
    }

    // Automatic selection based on Java version
    final int javaVersion = getJavaVersion();

    if (javaVersion >= 23 && isPanamaRuntimeAvailable()) {
      logger.info(
          "Auto-selected Panama WASI runtime for Java "
              + sanitizeForLog(String.valueOf(javaVersion)));
      return WasiRuntimeType.PANAMA;
    } else {
      logger.info(
          "Auto-selected JNI WASI runtime for Java " + sanitizeForLog(String.valueOf(javaVersion)));
      return WasiRuntimeType.JNI;
    }
  }

  private static WasiComponentContext createJniContext() throws WasmException {
    try {
      // This will be implemented by loading the JNI WASI implementation class
      final Class<?> jniWasiComponentContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.JniWasiComponentContext");
      return (WasiComponentContext)
          jniWasiComponentContextClass.getDeclaredConstructor().newInstance();
    } catch (final Exception e) {
      throw new WasmException("Failed to create JNI WASI context: " + e.getMessage(), e);
    }
  }

  private static WasiComponentContext createPanamaContext() throws WasmException {
    try {
      // This will be implemented by loading the Panama WASI implementation class
      final Class<?> panamaWasiComponentContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaWasiComponentContext");
      return (WasiComponentContext)
          panamaWasiComponentContextClass.getDeclaredConstructor().newInstance();
    } catch (final Exception e) {
      throw new WasmException("Failed to create Panama WASI context: " + e.getMessage(), e);
    }
  }

  private static boolean isJniRuntimeAvailable() {
    try {
      Class.forName("ai.tegmentum.wasmtime4j.jni.JniWasiComponentContext");
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }

  private static boolean isPanamaRuntimeAvailable() {
    try {
      Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaWasiComponentContext");
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }
}
