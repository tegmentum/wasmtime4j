package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for WasiFactory.
 *
 * @since 1.0.0
 */
final class WasiFactoryTest {

  @Test
  void testGetJavaVersion() {
    final int version = WasiFactory.getJavaVersion();
    assertTrue(version >= 8, "Java version should be at least 8");
  }

  @Test
  void testGetSelectedRuntimeType() {
    final WasiRuntimeType runtimeType = WasiFactory.getSelectedRuntimeType();
    assertNotNull(runtimeType, "Selected runtime type should not be null");
    assertTrue(
        runtimeType == WasiRuntimeType.JNI || runtimeType == WasiRuntimeType.PANAMA,
        "Runtime type should be either JNI or Panama");
  }

  @Test
  void testIsRuntimeAvailableWithNull() {
    assertFalse(WasiFactory.isRuntimeAvailable(null), "Null runtime should not be available");
  }

  @Test
  void testIsRuntimeAvailableWithValidTypes() {
    // At least one runtime type should be available (even if class not found, method should not throw)
    assertDoesNotThrow(() -> WasiFactory.isRuntimeAvailable(WasiRuntimeType.JNI));
    assertDoesNotThrow(() -> WasiFactory.isRuntimeAvailable(WasiRuntimeType.PANAMA));
  }

  @Test
  void testCreateContextWithNullRuntimeType() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> WasiFactory.createContext(null),
            "Creating context with null runtime type should throw IllegalArgumentException");
    
    assertEquals("WASI runtime type cannot be null", exception.getMessage());
  }

  @Test
  void testCreateContextWithJniType() {
    // This should attempt to create JNI context (will fail since implementation doesn't exist yet)
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> WasiFactory.createContext(WasiRuntimeType.JNI),
            "Creating JNI context should throw WasmException when implementation not available");
    
    assertTrue(exception.getMessage().contains("Failed to create JNI WASI context"));
  }

  @Test
  void testCreateContextWithPanamaType() {
    // This should attempt to create Panama context (will fail since implementation doesn't exist yet)
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> WasiFactory.createContext(WasiRuntimeType.PANAMA),
            "Creating Panama context should throw WasmException when implementation not available");
    
    assertTrue(exception.getMessage().contains("Failed to create Panama WASI context"));
  }

  @Test
  void testCreateContextAutoDetect() {
    // Auto-detect should fail gracefully when no implementation is available
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> WasiFactory.createContext(),
            "Creating context with auto-detect should throw WasmException when no implementation available");
    
    assertTrue(
        exception.getMessage().contains("Failed to create JNI WASI context")
            || exception.getMessage().contains("Failed to create Panama WASI context"));
  }

  @Test
  void testSystemPropertyConstants() {
    assertEquals("wasmtime4j.wasi.runtime", WasiFactory.WASI_RUNTIME_PROPERTY);
    assertEquals("jni", WasiFactory.WASI_RUNTIME_JNI);
    assertEquals("panama", WasiFactory.WASI_RUNTIME_PANAMA);
  }
}