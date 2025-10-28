package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;

/**
 * JNI library loader utility class.
 *
 * <p>This class delegates to {@link NativeLibraryLoader} which handles extracting the native
 * library from JAR resources.
 *
 * @since 1.0.0
 */
public final class JniLibraryLoader {

  private JniLibraryLoader() {
    // Utility class
  }

  /**
   * Loads the native library if not already loaded.
   *
   * @throws RuntimeException if loading fails
   */
  public static void ensureLoaded() {
    NativeLibraryLoader.loadLibrary();
  }

  /**
   * Checks if the native library is loaded.
   *
   * @return true if loaded
   */
  public static boolean isLoaded() {
    return NativeLibraryLoader.isLibraryLoaded();
  }
}
