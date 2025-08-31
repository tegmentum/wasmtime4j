package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for WasiRuntimeInfo.
 *
 * @since 1.0.0
 */
final class WasiRuntimeInfoTest {

  @Test
  void testConstructorAndGetters() {
    final WasiRuntimeInfo info = new WasiRuntimeInfo(WasiRuntimeType.JNI, "1.0.0", "36.0.2");
    
    assertEquals(WasiRuntimeType.JNI, info.getRuntimeType());
    assertEquals("1.0.0", info.getVersion());
    assertEquals("36.0.2", info.getWasmtimeVersion());
  }

  @Test
  void testToString() {
    final WasiRuntimeInfo info = new WasiRuntimeInfo(WasiRuntimeType.PANAMA, "1.0.0", "36.0.2");
    final String result = info.toString();
    
    assertTrue(result.contains("PANAMA"));
    assertTrue(result.contains("1.0.0"));
    assertTrue(result.contains("36.0.2"));
    assertEquals("WasiRuntimeInfo{type=PANAMA, version=1.0.0, wasmtime=36.0.2}", result);
  }

  @Test
  void testEquals() {
    final WasiRuntimeInfo info1 = new WasiRuntimeInfo(WasiRuntimeType.JNI, "1.0.0", "36.0.2");
    final WasiRuntimeInfo info2 = new WasiRuntimeInfo(WasiRuntimeType.JNI, "1.0.0", "36.0.2");
    final WasiRuntimeInfo info3 = new WasiRuntimeInfo(WasiRuntimeType.PANAMA, "1.0.0", "36.0.2");
    final WasiRuntimeInfo info4 = new WasiRuntimeInfo(WasiRuntimeType.JNI, "2.0.0", "36.0.2");
    final WasiRuntimeInfo info5 = new WasiRuntimeInfo(WasiRuntimeType.JNI, "1.0.0", "37.0.0");

    assertTrue(info1.equals(info1)); // Same instance
    assertTrue(info1.equals(info2)); // Same values
    assertFalse(info1.equals(info3)); // Different runtime type
    assertFalse(info1.equals(info4)); // Different version
    assertFalse(info1.equals(info5)); // Different wasmtime version
    assertFalse(info1.equals(null)); // Null comparison
    assertFalse(info1.equals("string")); // Different type
  }

  @Test
  void testHashCode() {
    final WasiRuntimeInfo info1 = new WasiRuntimeInfo(WasiRuntimeType.JNI, "1.0.0", "36.0.2");
    final WasiRuntimeInfo info2 = new WasiRuntimeInfo(WasiRuntimeType.JNI, "1.0.0", "36.0.2");
    final WasiRuntimeInfo info3 = new WasiRuntimeInfo(WasiRuntimeType.PANAMA, "1.0.0", "36.0.2");

    assertEquals(info1.hashCode(), info2.hashCode()); // Same values should have same hash
    assertNotEquals(info1.hashCode(), info3.hashCode()); // Different values should have different hash (usually)
  }

  @Test
  void testEqualsAndHashCodeWithNullValues() {
    final WasiRuntimeInfo info1 = new WasiRuntimeInfo(WasiRuntimeType.JNI, null, null);
    final WasiRuntimeInfo info2 = new WasiRuntimeInfo(WasiRuntimeType.JNI, null, null);
    final WasiRuntimeInfo info3 = new WasiRuntimeInfo(WasiRuntimeType.JNI, "1.0.0", null);

    assertTrue(info1.equals(info2)); // Both have nulls
    assertFalse(info1.equals(info3)); // One has null, other doesn't
    assertEquals(info1.hashCode(), info2.hashCode()); // Same null values should have same hash
  }
}