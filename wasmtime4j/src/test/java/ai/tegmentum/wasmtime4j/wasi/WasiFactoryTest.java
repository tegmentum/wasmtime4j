package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.testing.RequiresWasmRuntime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for WasiFactory.
 *
 * @since 1.0.0
 */
@DisplayName("WasiFactory Tests")
@RequiresWasmRuntime
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
    // At least one runtime type should be available (even if class not found, method should not
    // throw)
    assertDoesNotThrow(() -> WasiFactory.isRuntimeAvailable(WasiRuntimeType.JNI));
    assertDoesNotThrow(() -> WasiFactory.isRuntimeAvailable(WasiRuntimeType.PANAMA));
  }

  @Test
  void testSystemPropertyConstants() {
    assertEquals("wasmtime4j.wasi.runtime", WasiFactory.WASI_RUNTIME_PROPERTY);
    assertEquals("jni", WasiFactory.WASI_RUNTIME_JNI);
    assertEquals("panama", WasiFactory.WASI_RUNTIME_PANAMA);
  }
}
