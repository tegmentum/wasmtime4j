package ai.tegmentum.wasmtime4j.jni;

/**
 * JNI library loader utility class.
 *
 * @since 1.0.0
 */
public final class JniLibraryLoader {

  private static volatile boolean loaded = false;
  private static final Object LOAD_LOCK = new Object();

  private JniLibraryLoader() {
    // Utility class
  }

  /**
   * Loads the native library if not already loaded.
   *
   * @throws RuntimeException if loading fails
   */
  public static void ensureLoaded() {
    if (!loaded) {
      synchronized (LOAD_LOCK) {
        if (!loaded) {
          try {
            System.loadLibrary("wasmtime4j");
            loaded = true;
          } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load wasmtime4j native library", e);
          }
        }
      }
    }
  }

  /**
   * Checks if the native library is loaded.
   *
   * @return true if loaded
   */
  public static boolean isLoaded() {
    return loaded;
  }
}
