package ai.tegmentum.wasmtime4j.jni.util;

/**
 * Bridge utility class for JNI garbage collection operations.
 *
 * @since 1.0.0
 */
public final class JniGcBridge {

  private JniGcBridge() {
    // Utility class
  }

  /**
   * Performs garbage collection bridging operations.
   *
   * @return operation result as string
   */
  public static String performGcBridge() {
    return "GC bridge operation completed";
  }

  /**
   * Gets GC bridge statistics.
   *
   * @return statistics as string
   */
  public static String getGcBridgeStatistics() {
    return "GC bridge statistics";
  }
}
