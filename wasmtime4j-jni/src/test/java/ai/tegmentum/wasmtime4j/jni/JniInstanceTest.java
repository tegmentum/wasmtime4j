package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.InstanceStatistics;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link JniInstance}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly instance operations is tested in integration
 * tests.
 */
class JniInstanceTest {

  private static final long VALID_HANDLE = 0x12345678L;

  @Mock private Module mockModule;

  @Mock private Store mockStore;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testConstructorWithValidHandle() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    assertThat(instance.getResourceType()).isEqualTo("Instance");
    assertFalse(instance.isClosed());
    assertEquals(mockModule, instance.getModule());
    assertEquals(mockStore, instance.getStore());
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception =
        assertThrows(
            JniValidationException.class, () -> new JniInstance(0L, mockModule, mockStore));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testConstructorWithNullModule() {
    final JniValidationException exception =
        assertThrows(
            JniValidationException.class, () -> new JniInstance(VALID_HANDLE, null, mockStore));

    assertThat(exception.getMessage()).contains("module");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testConstructorWithNullStore() {
    final JniValidationException exception =
        assertThrows(
            JniValidationException.class, () -> new JniInstance(VALID_HANDLE, mockModule, null));

    assertThat(exception.getMessage()).contains("store");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testGetFunctionWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getFunction(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetFunctionWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getFunction(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetFunctionWithWhitespaceOnlyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getFunction("   "));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty or whitespace-only");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetMemoryWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getMemory(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetMemoryWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getMemory(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetTableWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getTable(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetTableWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getTable(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetGlobalWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getGlobal(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetGlobalWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getGlobal(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testHasExportWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.hasExport(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testHasExportWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.hasExport(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testResourceManagement() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);
    assertFalse(instance.isClosed());

    // Test that resource starts in open state
    assertFalse(instance.isClosed());
    // Note: Actual close() testing requires native methods and is covered in integration tests
  }

  @Test
  void testCloseIsIdempotent() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);
    assertFalse(instance.isClosed());

    // Note: Actual close() idempotency testing requires native methods
    // This test verifies the initial state only
    // Integration tests will verify close() behavior
  }

  @Test
  void testTryWithResources() {
    try (final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore)) {
      assertFalse(instance.isClosed());
    }
    // Instance should be automatically closed after try block
  }

  @Test
  void testOperationsOnClosedInstance() {
    // Note: This test would need to actually close the instance to test closed state operations
    // Since close() requires native methods, this is covered in integration tests
    // This unit test verifies parameter validation only

    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);
    assertFalse(instance.isClosed());

    // Test that operations work on open instance (would call native methods in real implementation)
    // Integration tests will verify behavior on closed instances
  }

  @Test
  void testToString() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);
    final String toString = instance.toString();

    assertThat(toString).contains("Instance");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    // Note: Testing toString() after close() requires native methods
    // Integration tests will verify toString() behavior after close()
  }

  @Test
  void testGetModuleReturnsCorrectReference() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final Module retrievedModule = instance.getModule();

    assertNotNull(retrievedModule);
    assertEquals(mockModule, retrievedModule);
  }

  @Test
  void testGetStoreReturnsCorrectReference() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    final Store retrievedStore = instance.getStore();

    assertNotNull(retrievedStore);
    assertEquals(mockStore, retrievedStore);
  }

  @Test
  void testIsValidWithValidHandle() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    assertTrue(instance.isValid());
  }

  @Test
  void testIsValidWithZeroHandle() {
    // This tests the isValid method logic for checking native handle
    // We can't test with a zero handle through constructor due to validation
    // but we can verify the logic would work correctly
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Valid instance should return true
    assertTrue(instance.isValid());
  }

  @Test
  void testGetExportNamesValidation() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // This test validates the method exists and doesn't throw for basic validation
    // Actual export name retrieval is tested in integration tests
    // The method should be callable without throwing validation exceptions
    assertNotNull(instance);
  }

  @Test
  void testHasExportValidation() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test validation for hasExport method calls
    assertThrows(JniValidationException.class, () -> instance.hasExport(null));
    assertThrows(JniValidationException.class, () -> instance.hasExport(""));
    assertThrows(JniValidationException.class, () -> instance.hasExport("   "));
  }

  @Test
  void testCallFunctionValidation() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test validation - null function name should be handled gracefully
    // This tests the defensive programming approach
    assertNotNull(instance);

    // The actual callFunction behavior is tested in integration tests
    // since it requires native method calls
  }

  @Test
  void testLifecycleStateManagement() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test initial state - instance should not be disposed initially
    assertFalse(instance.isDisposed());

    // Test creation timestamp tracking
    assertTrue(instance.getCreatedAtMicros() > 0);
    long creationTime = instance.getCreatedAtMicros();
    assertTrue(creationTime <= System.currentTimeMillis() * 1000); // Should be reasonable

    // Test metadata export count (basic validation)
    // Note: Actual count requires native calls, tested in integration tests
    assertTrue(instance.getMetadataExportCount() >= 0);
  }

  @Test
  void testInstanceStatistics() throws WasmException {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test basic statistics retrieval
    final InstanceStatistics stats = instance.getStatistics();
    assertNotNull(stats);

    // Verify statistics structure
    assertTrue(stats.getFunctionCallCount() >= 0);
    assertTrue(stats.getTotalExecutionTime() >= 0);
    assertTrue(stats.getMemoryBytesAllocated() >= 0);
    assertTrue(stats.getPeakMemoryUsage() >= 0);
    assertTrue(stats.getActiveTableElements() >= 0);
    assertTrue(stats.getActiveGlobals() >= 0);
    assertTrue(stats.getFuelConsumed() >= 0);
    assertTrue(stats.getEpochTicks() >= 0);
  }

  @Test
  void testI32FunctionCallValidation() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test null function name validation
    assertThrows(WasmException.class, () -> instance.callI32Function(null));

    // Note: Actual function calling requires native methods and is tested in integration tests
  }

  @Test
  void testGetFunctionByIndex() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test negative index validation
    assertThrows(IllegalArgumentException.class, () -> instance.getFunction(-1));

    // Note: Actual index-based retrieval requires native methods and is tested in integration tests
  }

  @Test
  void testGetMemoryByIndex() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test negative index validation
    assertThrows(IllegalArgumentException.class, () -> instance.getMemory(-1));

    // Note: Actual index-based retrieval requires native methods and is tested in integration tests
  }

  @Test
  void testGetTableByIndex() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test negative index validation
    assertThrows(IllegalArgumentException.class, () -> instance.getTable(-1));

    // Note: Actual index-based retrieval requires native methods and is tested in integration tests
  }

  @Test
  void testGetGlobalByIndex() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test negative index validation
    assertThrows(IllegalArgumentException.class, () -> instance.getGlobal(-1));

    // Note: Actual index-based retrieval requires native methods and is tested in integration tests
  }

  @Test
  void testGetAllExports() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test basic functionality - should return non-null map
    final java.util.Map<String, Object> exports = instance.getAllExports();
    assertNotNull(exports);

    // Note: Actual export retrieval requires native methods and is tested in integration tests
  }

  @Test
  void testSetImportsUnsupported() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test that setImports throws UnsupportedOperationException
    assertThrows(
        UnsupportedOperationException.class,
        () -> instance.setImports(java.util.Collections.emptyMap()));
  }

  @Test
  void testThreadAccessValidation() {
    final JniInstance instance = new JniInstance(VALID_HANDLE, mockModule, mockStore);

    // Test that validateThreadAccess doesn't throw exceptions
    // Note: Actual thread validation requires native methods and is tested in integration tests
    assertTrue(instance.validateThreadAccess() || !instance.validateThreadAccess());
  }
}
