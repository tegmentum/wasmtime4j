package ai.tegmentum.wasmtime4j.test;

import java.util.logging.Logger;

/**
 * Shared test utilities for integration tests.
 *
 * @since 1.0.0
 */
public final class TestUtils {

  private static final Logger LOGGER = Logger.getLogger(TestUtils.class.getName());

  /** Private constructor to prevent instantiation. */
  private TestUtils() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Clears global handle registries in native code.
   *
   * <p>Uses reflection to call the appropriate clearHandleRegistries method based on the runtime
   * implementation (JNI or Panama). This prevents test isolation issues when running the full test
   * suite.
   */
  public static void clearHandleRegistries() {
    // Try JNI first
    try {
      final Class<?> jniEngineClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniEngine");
      final java.lang.reflect.Method method = jniEngineClass.getMethod("clearHandleRegistries");
      final int result = (int) method.invoke(null);
      if (result == 0) {
        LOGGER.info("Cleared JNI handle registries");
        return;
      }
    } catch (final Exception e) {
      LOGGER.fine("JNI clearHandleRegistries not available: " + e.getMessage());
    }

    // Try Panama
    try {
      final Class<?> panamaBindingsClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.NativeMemoryBindings");
      final java.lang.reflect.Method getInstanceMethod =
          panamaBindingsClass.getMethod("getInstance");
      final Object instance = getInstanceMethod.invoke(null);
      final java.lang.reflect.Method method =
          panamaBindingsClass.getMethod("memoryClearHandleRegistries");
      final int result = (int) method.invoke(instance);
      if (result == 0) {
        LOGGER.info("Cleared Panama handle registries");
        return;
      }
    } catch (final Exception e) {
      LOGGER.fine("Panama clearHandleRegistries not available: " + e.getMessage());
    }

    LOGGER.warning("Could not clear handle registries - no implementation available");
  }
}
