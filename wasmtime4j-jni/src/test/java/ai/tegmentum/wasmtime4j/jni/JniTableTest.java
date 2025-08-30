package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniTable}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and resource management.
 * The tests verify constructor behavior, resource lifecycle, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly table operations is tested in integration tests.
 */
@DisplayName("JniTable Tests")
class JniTableTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long INVALID_HANDLE = 0L;

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create table with valid handle")
    void shouldCreateTableWithValidHandle() {
      final JniTable newTable = new JniTable(VALID_HANDLE);
      assertNotNull(newTable);
      assertFalse(newTable.isClosed());
      newTable.close();
    }

    @Test
    @DisplayName("Should reject invalid handle")
    void shouldRejectInvalidHandle() {
      assertThrows(
          JniResourceException.class,
          () -> new JniTable(INVALID_HANDLE),
          "Should throw JniResourceException for invalid handle");
    }

    @Test
    @DisplayName("Should reject negative handle")
    void shouldRejectNegativeHandle() {
      assertThrows(
          JniResourceException.class,
          () -> new JniTable(-1L),
          "Should throw JniResourceException for negative handle");
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should provide resource type")
    void shouldProvideResourceType() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertNotNull(table.getResourceType());
      table.close();
    }

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertFalse(table.isClosed(), "Should not be closed initially");
      table.close();
      assertTrue(table.isClosed(), "Should be closed after calling close()");
    }

    @Test
    @DisplayName("Should be idempotent on close")
    void shouldBeIdempotentOnClose() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.close();
      assertTrue(table.isClosed(), "Should be closed after first call");

      // Second close should not throw
      table.close();
      assertTrue(table.isClosed(), "Should remain closed after second call");
    }

    @Test
    @DisplayName("Should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      try (final JniTable autoClosedTable = new JniTable(VALID_HANDLE)) {
        assertFalse(autoClosedTable.isClosed(), "Should not be closed inside try block");
      }
      // Table should be automatically closed after try block
    }
  }

  @Nested
  @DisplayName("Parameter Validation Tests")
  class ParameterValidationTests {

    @Test
    @DisplayName("Should reject negative index in get")
    void shouldRejectNegativeIndexInGet() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.get(-1),
          "Should throw IllegalArgumentException for negative index");
      table.close();
    }

    @Test
    @DisplayName("Should reject negative index in set")
    void shouldRejectNegativeIndexInSet() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.set(-1, "value"),
          "Should throw IllegalArgumentException for negative index");
      table.close();
    }

    @Test
    @DisplayName("Should reject negative delta in grow")
    void shouldRejectNegativeDeltaInGrow() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.grow(-1, null),
          "Should throw IllegalArgumentException for negative delta");
      table.close();
    }

    @Test
    @DisplayName("Should reject negative start in fill")
    void shouldRejectNegativeStartInFill() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(-1, 1, "value"),
          "Should throw IllegalArgumentException for negative start");
      table.close();
    }

    @Test
    @DisplayName("Should reject negative count in fill")
    void shouldRejectNegativeCountInFill() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(0, -1, "value"),
          "Should throw IllegalArgumentException for negative count");
      table.close();
    }
  }

  @Nested
  @DisplayName("Closed Resource Tests")
  class ClosedResourceTests {

    @Test
    @DisplayName("Should throw exception when accessing size of closed table")
    void shouldThrowExceptionWhenAccessingSizeOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.close();

      assertThrows(
          JniResourceException.class,
          table::getSize,
          "Should throw JniResourceException when accessing size of closed table");
    }

    @Test
    @DisplayName("Should throw exception when accessing max size of closed table")
    void shouldThrowExceptionWhenAccessingMaxSizeOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.close();

      assertThrows(
          JniResourceException.class,
          table::getMaxSize,
          "Should throw JniResourceException when accessing max size of closed table");
    }

    @Test
    @DisplayName("Should throw exception when accessing element type of closed table")
    void shouldThrowExceptionWhenAccessingElementTypeOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.close();

      assertThrows(
          JniResourceException.class,
          table::getElementType,
          "Should throw JniResourceException when accessing element type of closed table");
    }

    @Test
    @DisplayName("Should throw exception when accessing element of closed table")
    void shouldThrowExceptionWhenAccessingElementOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.get(0),
          "Should throw JniResourceException when accessing closed table");
    }

    @Test
    @DisplayName("Should throw exception when setting element in closed table")
    void shouldThrowExceptionWhenSettingElementInClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.set(0, "value"),
          "Should throw JniResourceException when setting element in closed table");
    }

    @Test
    @DisplayName("Should throw exception when growing closed table")
    void shouldThrowExceptionWhenGrowingClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.grow(1, null),
          "Should throw JniResourceException when growing closed table");
    }

    @Test
    @DisplayName("Should throw exception when filling closed table")
    void shouldThrowExceptionWhenFillingClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.fill(0, 1, "value"),
          "Should throw JniResourceException when filling closed table");
    }
  }

  @Nested
  @DisplayName("toString and Object Methods Tests")
  class ToStringAndObjectMethodsTests {

    @Test
    @DisplayName("Should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
      final JniTable table = new JniTable(VALID_HANDLE);
      final String toString = table.toString();
      assertNotNull(toString, "toString should not be null");
      assertTrue(toString.contains("Table"), "toString should contain resource type");
      assertTrue(
          toString.contains(Long.toHexString(VALID_HANDLE)), "toString should contain handle");
      assertTrue(toString.contains("false"), "toString should show not closed");
      table.close();
    }

    @Test
    @DisplayName("Should show closed state in toString")
    void shouldShowClosedStateInToString() {
      final JniTable table = new JniTable(VALID_HANDLE);
      table.close();
      final String toString = table.toString();
      assertTrue(toString.contains("true"), "toString should show closed state");
    }
  }
}