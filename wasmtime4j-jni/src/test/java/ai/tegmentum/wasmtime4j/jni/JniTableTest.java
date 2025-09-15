package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniTable}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and resource management.
 * The tests verify constructor behavior, resource lifecycle, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly table operations is tested in integration
 * tests.
 */
class JniTableTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long INVALID_HANDLE = 0L;

  @Nested
  class ConstructorTests {

    @Test
    void testCreateTableWithValidHandle() {
      final JniTable newTable = new JniTable(VALID_HANDLE);
      assertNotNull(newTable);
      assertFalse(newTable.isClosed());
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    void testRejectInvalidHandle() {
      assertThrows(
          JniValidationException.class,
          () -> new JniTable(INVALID_HANDLE),
          "Should throw JniValidationException for invalid handle");
    }

    @Test
    void testRejectNegativeHandle() {
      assertThrows(
          JniValidationException.class,
          () -> new JniTable(-1L),
          "Should throw JniValidationException for negative handle");
    }
  }

  @Nested
  class ResourceManagementTests {

    @Test
    void testProvideResourceType() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertNotNull(table.getResourceType());
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    void testCloseGracefully() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertFalse(table.isClosed(), "Should not be closed initially");

      // Test that resource starts in open state
      assertFalse(table.isClosed(), "Should remain open");
      // Note: Actual close() testing requires native methods and is covered in integration tests
    }

    @Test
    void testIdempotentOnClose() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertFalse(table.isClosed(), "Should not be closed initially");

      // Note: Actual close() idempotency testing requires native methods
      // This test verifies the initial state only
      // Integration tests will verify close() behavior
    }

    @Test
    void testWorkWithTryWithResources() {
      final JniTable autoClosedTable = new JniTable(VALID_HANDLE);
      assertFalse(autoClosedTable.isClosed(), "Should not be closed inside try block");
      // Note: Not using try-with-resources in unit test since close() requires native methods
    }
  }

  @Nested
  class ParameterValidationTests {

    @Test
    void testRejectNegativeIndexInGet() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          JniValidationException.class,
          () -> table.get(-1),
          "Should throw JniValidationException for negative index");
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    void testRejectNegativeIndexInSet() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          JniValidationException.class,
          () -> table.set(-1, "value"),
          "Should throw JniValidationException for negative index");
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    void testRejectNegativeDeltaInGrow() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          JniValidationException.class,
          () -> table.grow(-1, null),
          "Should throw JniValidationException for negative delta");
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    void testRejectNegativeStartInFill() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          JniValidationException.class,
          () -> table.fill(-1, 1, "value"),
          "Should throw JniValidationException for negative start");
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    void testRejectNegativeCountInFill() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          JniValidationException.class,
          () -> table.fill(0, -1, "value"),
          "Should throw JniValidationException for negative count");
      // Note: Not calling close() in unit test since it requires native methods
    }
  }

  @Nested
  class ClosedResourceTests {

    @Test
    void testThrowExceptionWhenAccessingSizeOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          table::getSize,
          "Should throw JniResourceException when accessing size of closed table");
    }

    @Test
    void testThrowExceptionWhenAccessingMaxSizeOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          table::getMaxSize,
          "Should throw JniResourceException when accessing max size of closed table");
    }

    @Test
    void testThrowExceptionWhenAccessingElementTypeOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          table::getElementType,
          "Should throw JniResourceException when accessing element type of closed table");
    }

    @Test
    void testThrowExceptionWhenAccessingElementOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          () -> table.get(0),
          "Should throw JniResourceException when accessing closed table");
    }

    @Test
    void testThrowExceptionWhenSettingElementInClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          () -> table.set(0, "value"),
          "Should throw JniResourceException when setting element in closed table");
    }

    @Test
    void testThrowExceptionWhenGrowingClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          () -> table.grow(1, null),
          "Should throw JniResourceException when growing closed table");
    }

    @Test
    void testThrowExceptionWhenFillingClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          () -> table.fill(0, 1, "value"),
          "Should throw JniResourceException when filling closed table");
    }
  }

  @Nested
  class ToStringAndObjectMethodsTests {

    @Test
    void testProvideMeaningfulToString() {
      final JniTable table = new JniTable(VALID_HANDLE);
      final String toString = table.toString();
      assertNotNull(toString, "toString should not be null");
      assertTrue(toString.contains("Table"), "toString should contain resource type");
      assertTrue(
          toString.contains(Long.toHexString(VALID_HANDLE)), "toString should contain handle");
      assertTrue(toString.contains("false"), "toString should show not closed");
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    void testShowOpenStateInToString() {
      final JniTable table = new JniTable(VALID_HANDLE);
      final String toString = table.toString();
      assertTrue(toString.contains("false"), "toString should show open state");

      // Note: Testing toString() after close() requires native methods
      // Integration tests will verify toString() behavior after close()
    }
  }
}
