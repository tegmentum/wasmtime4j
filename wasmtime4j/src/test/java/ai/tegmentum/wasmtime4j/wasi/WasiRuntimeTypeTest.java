package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for WasiRuntimeType.
 *
 * @since 1.0.0
 */
final class WasiRuntimeTypeTest {

  @Test
  void testEnumValues() {
    final WasiRuntimeType[] values = WasiRuntimeType.values();
    assertEquals(2, values.length, "Should have exactly 2 runtime types");

    assertEquals(WasiRuntimeType.JNI, values[0]);
    assertEquals(WasiRuntimeType.PANAMA, values[1]);
  }

  @Test
  void testEnumValueOf() {
    assertEquals(WasiRuntimeType.JNI, WasiRuntimeType.valueOf("JNI"));
    assertEquals(WasiRuntimeType.PANAMA, WasiRuntimeType.valueOf("PANAMA"));
  }

  @Test
  void testEnumToString() {
    assertNotNull(WasiRuntimeType.JNI.toString());
    assertNotNull(WasiRuntimeType.PANAMA.toString());
    assertEquals("JNI", WasiRuntimeType.JNI.toString());
    assertEquals("PANAMA", WasiRuntimeType.PANAMA.toString());
  }
}
