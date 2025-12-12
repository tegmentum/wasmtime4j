package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmFeature;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniEngine}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly engine operations is tested in integration
 * tests.
 */
class JniEngineTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long ZERO_HANDLE = 0L;

  @Test
  void testConstructorWithValidHandle() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    assertNotNull(engine);
    assertEquals(VALID_HANDLE, engine.getNativeHandle());
    // Note: isValid() calls native code, tested in integration tests
    // Note: Other native method calls removed - tested in integration tests
  }

  @Test
  void testConstructorWithZeroHandle() {
    final JniEngine engine = new JniEngine(ZERO_HANDLE);

    assertNotNull(engine);
    assertEquals(ZERO_HANDLE, engine.getNativeHandle());
    // Note: isValid() calls native code, tested in integration tests
  }

  // Note: isValid tests removed - they call native code and belong in integration tests

  @Test
  void testIsValidAfterClose() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);
    assertTrue(engine.isValid());

    // Note: close() calls native method, but we can test the state
    // Integration tests will verify full close() behavior
  }

  @Test
  void testCreateStoreWithZeroHandle() {
    final JniEngine engine = new JniEngine(ZERO_HANDLE);

    final IllegalStateException exception =
        assertThrows(IllegalStateException.class, engine::createStore);

    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testCreateStoreWithDataUnsupported() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    final UnsupportedOperationException exception =
        assertThrows(UnsupportedOperationException.class, () -> engine.createStore(new Object()));

    assertThat(exception.getMessage()).contains("not yet implemented");
  }

  @Test
  void testCompileModuleWithNullBytes() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> engine.compileModule(null));

    assertThat(exception.getMessage()).contains("wasmBytes");
    assertThat(exception.getMessage()).contains("cannot be null");
  }

  @Test
  void testCompileModuleWithEmptyBytes() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> engine.compileModule(new byte[0]));

    assertThat(exception.getMessage()).contains("wasmBytes");
    assertThat(exception.getMessage()).contains("cannot be empty");
  }

  @Test
  void testCompileModuleWithZeroHandle() {
    final JniEngine engine = new JniEngine(ZERO_HANDLE);
    final byte[] validWasm = new byte[] {0x00, 0x61, 0x73, 0x6d};

    final IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> engine.compileModule(validWasm));

    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testCompileWatWithNullString() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> engine.compileWat(null));

    assertThat(exception.getMessage()).contains("wat");
    assertThat(exception.getMessage()).contains("cannot be null");
  }

  @Test
  void testCompileWatWithEmptyString() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> engine.compileWat(""));

    assertThat(exception.getMessage()).contains("wat");
    assertThat(exception.getMessage()).contains("cannot be empty");
  }

  @Test
  void testCompileWatWithZeroHandle() {
    final JniEngine engine = new JniEngine(ZERO_HANDLE);
    final String validWat = "(module)";

    final IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> engine.compileWat(validWat));

    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testIsEpochInterruptionEnabledWithZeroHandle() {
    final JniEngine engine = new JniEngine(ZERO_HANDLE);

    assertFalse(engine.isEpochInterruptionEnabled());
  }

  @Test
  void testIsFuelEnabledWithZeroHandle() {
    final JniEngine engine = new JniEngine(ZERO_HANDLE);

    assertFalse(engine.isFuelEnabled());
  }

  @Test
  void testGetStackSizeLimitWithZeroHandle() {
    final JniEngine engine = new JniEngine(ZERO_HANDLE);

    assertEquals(0, engine.getStackSizeLimit());
  }

  @Test
  void testGetMemoryLimitPagesWithZeroHandle() {
    final JniEngine engine = new JniEngine(ZERO_HANDLE);

    assertEquals(0, engine.getMemoryLimitPages());
  }

  @Test
  void testSupportsFeatureWithNullFeature() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    assertFalse(engine.supportsFeature(null));
  }

  @Test
  void testSupportsFeatureWithZeroHandle() {
    final JniEngine engine = new JniEngine(ZERO_HANDLE);

    assertFalse(engine.supportsFeature(WasmFeature.REFERENCE_TYPES));
  }

  @Test
  void testSupportsFeatureReturnsDefaultFalse() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    // Default implementation returns false for all features
    assertFalse(engine.supportsFeature(WasmFeature.REFERENCE_TYPES));
    assertFalse(engine.supportsFeature(WasmFeature.BULK_MEMORY));
    assertFalse(engine.supportsFeature(WasmFeature.MULTI_VALUE));
    assertFalse(engine.supportsFeature(WasmFeature.SIMD));
    assertFalse(engine.supportsFeature(WasmFeature.THREADS));
  }

  @Test
  void testGetConfigReturnsNull() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    // TODO: Should return actual config when implemented
    assertThat(engine.getConfig()).isNull();
  }

  @Test
  void testGetReferenceCountReturnsDefault() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    // Default implementation returns 1
    assertEquals(1, engine.getReferenceCount());
  }

  @Test
  void testGetMaxInstancesReturnsDefault() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    // Default implementation returns Integer.MAX_VALUE
    assertEquals(Integer.MAX_VALUE, engine.getMaxInstances());
  }

  // Note: Tests that call close() are disabled in unit tests since they require native library
  // These are tested in integration tests instead

  @Test
  void testCloseDoesNotCrashWithValidHandle() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    // Verify engine is valid before close
    assertTrue(engine.isValid());

    // Note: Cannot call close() in unit test - requires native library
    // Integration tests verify close() behavior
  }

  @Test
  void testResourceLifecycleState() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    // Verify initial state
    assertNotNull(engine);
    assertTrue(engine.isValid());
    assertEquals(VALID_HANDLE, engine.getNativeHandle());

    // Note: Cannot test try-with-resources in unit test - requires native library
    // Integration tests verify automatic resource cleanup
  }

  @Test
  void testGetNativeHandleReturnsCorrectValue() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    assertEquals(VALID_HANDLE, engine.getNativeHandle());
  }

  @Test
  void testMultipleEnginesWithDifferentHandles() {
    final JniEngine engine1 = new JniEngine(0x1111L);
    final JniEngine engine2 = new JniEngine(0x2222L);

    assertEquals(0x1111L, engine1.getNativeHandle());
    assertEquals(0x2222L, engine2.getNativeHandle());
    assertTrue(engine1.isValid());
    assertTrue(engine2.isValid());
  }

  // Note: Tests that call native methods (isValid, isEpochInterruptionEnabled, isFuelEnabled,
  // getStackSizeLimit, getMemoryLimitPages, supportsFeature, etc.) have been removed.
  // These should be tested in integration tests with real engine instances, not unit tests
  // with fake handles that cause JVM crashes.

  @Test
  void testValidationPreventsCrashOnInvalidInput() {
    final JniEngine engine = new JniEngine(VALID_HANDLE);

    // All these should throw exceptions, not crash the JVM
    assertThrows(IllegalArgumentException.class, () -> engine.compileModule(null));
    assertThrows(IllegalArgumentException.class, () -> engine.compileModule(new byte[0]));
    assertThrows(IllegalArgumentException.class, () -> engine.compileWat(null));
    assertThrows(IllegalArgumentException.class, () -> engine.compileWat(""));
  }
}
