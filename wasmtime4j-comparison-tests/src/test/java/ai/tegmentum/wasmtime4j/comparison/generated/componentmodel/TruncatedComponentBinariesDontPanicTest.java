package ai.tegmentum.wasmtime4j.comparison.generated.componentmodel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.factory.WasmEngineFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: component_model::truncated_component_binaries_dont_panic
 *
 * <p>Original source: aot.rs:170 Category: component_model
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class TruncatedComponentBinariesDontPanicTest {

  @Test
  @DisplayName("component_model::truncated_component_binaries_dont_panic")
  public void testTruncatedComponentBinariesDontPanic() {
    // This test verifies that truncated component binaries are handled gracefully
    // without causing a panic (e.g., JVM crash or uncaught native exception)

    // Create an engine to test component validation
    final Engine engine =
        assertDoesNotThrow(
            WasmEngineFactory::create, "Engine creation should not throw");

    try {
      // Test various truncated component bytes - they should throw exceptions, not panic

      // Empty bytes
      assertThrows(
          Exception.class,
          () -> engine.compileModule(new byte[] {}),
          "Empty bytes should throw an exception, not panic");

      // Just the magic number (truncated)
      assertThrows(
          Exception.class,
          () -> engine.compileModule(new byte[] {0x00, 0x61, 0x73, 0x6d}),
          "Truncated magic number should throw an exception, not panic");

      // Magic number + incomplete version (truncated)
      assertThrows(
          Exception.class,
          () -> engine.compileModule(new byte[] {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00}),
          "Truncated version should throw an exception, not panic");

      // Component magic (0d 00 01 00 instead of 01 00 00 00 for modules)
      // with truncated content
      assertThrows(
          Exception.class,
          () -> engine.compileModule(new byte[] {0x00, 0x61, 0x73, 0x6d, 0x0d, 0x00, 0x01, 0x00}),
          "Truncated component should throw an exception, not panic");

      // If we get here without any native crash, the test passes
      // The key validation is that truncated binaries throw exceptions rather than
      // causing undefined behavior or JVM crashes
    } finally {
      engine.close();
    }
  }
}
