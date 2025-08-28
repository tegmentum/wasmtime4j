package ai.tegmentum.wasmtime4j.jni.nativelib;

import ai.tegmentum.wasmtime4j.NativeLibraryUtils;
import ai.tegmentum.wasmtime4j.PlatformDetector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI-specific wrapper for native library loading.
 *
 * <p>This class provides a simplified interface for JNI operations while using the shared {@link
 * NativeLibraryUtils} for the actual loading logic. It maintains thread-safety and prevents
 * multiple loading attempts.
 *
 * <p>The loader automatically detects the current platform and loads the appropriate native library
 * for JNI operations.
 */
public final class NativeLibraryLoader {

  private static final Logger LOGGER = Logger.getLogger(NativeLibraryLoader.class.getName());

  /** Flag to track if the native library has been loaded. */
  private static final AtomicBoolean LIBRARY_LOADED = new AtomicBoolean(false);

  /** Cached information about the library loading. */
  private static volatile NativeLibraryUtils.LibraryLoadInfo loadInfo;

  /** Private constructor to prevent instantiation of utility class. */
  private NativeLibraryLoader() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Loads the native library required for JNI operations.
   *
   * <p>This method is thread-safe and will only load the library once, even if called multiple
   * times. It automatically detects the current platform and loads the appropriate native library.
   *
   * @throws UnsatisfiedLinkError if the native library cannot be loaded
   * @throws RuntimeException if there's an error during library extraction or loading
   */
  public static void loadLibrary() {
    if (LIBRARY_LOADED.get()) {
      return; // Already loaded
    }

    synchronized (NativeLibraryLoader.class) {
      if (LIBRARY_LOADED.get()) {
        return; // Double-check after acquiring lock
      }

      try {
        loadInfo = NativeLibraryUtils.loadNativeLibrary();
        if (loadInfo.isSuccessful()) {
          LIBRARY_LOADED.set(true);
          LOGGER.info("Successfully loaded native library for JNI: " + loadInfo);
        } else {
          LOGGER.log(
              Level.SEVERE,
              "Failed to load native library for JNI: " + loadInfo,
              loadInfo.getError());
          throw new RuntimeException(
              "Failed to load native library for JNI operations", loadInfo.getError());
        }
      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "Unexpected error during native library loading", e);
        throw new RuntimeException("Failed to load native library for JNI operations", e);
      }
    }
  }

  /**
   * Checks if the native library has been loaded.
   *
   * @return true if the library is loaded, false otherwise
   */
  public static boolean isLibraryLoaded() {
    return LIBRARY_LOADED.get();
  }

  /**
   * Gets the expected native library resource path for the current platform.
   *
   * @return the resource path to the native library
   * @throws RuntimeException if the current platform is not supported
   */
  public static String getLibraryResourcePath() {
    try {
      final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
      return platformInfo.getLibraryResourcePath("wasmtime4j");
    } catch (final RuntimeException e) {
      LOGGER.log(Level.SEVERE, "Failed to detect platform for resource path", e);
      throw e;
    }
  }

  /**
   * Gets information about the library loading attempt.
   *
   * @return the load info, or null if not yet attempted
   */
  public static NativeLibraryUtils.LibraryLoadInfo getLoadInfo() {
    return loadInfo;
  }

  /**
   * Gets information about the current platform and library loading status.
   *
   * @return a string describing the platform and library status
   */
  public static String getPlatformInfo() {
    final StringBuilder sb = new StringBuilder();
    sb.append("JNI Native Library Status:\n");
    sb.append("  Loaded: ").append(isLibraryLoaded()).append("\n");

    if (loadInfo != null) {
      sb.append("  Load info: ").append(loadInfo).append("\n");
    }

    sb.append(NativeLibraryUtils.getDiagnosticInfo());
    return sb.toString();
  }
}
