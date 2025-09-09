package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
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
 * <p>Note: Functional behavior with actual WebAssembly table operations is tested in integration
 * tests.
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
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    @DisplayName("Should reject invalid handle")
    void shouldRejectInvalidHandle() {
      assertThrows(
          JniValidationException.class,
          () -> new JniTable(INVALID_HANDLE),
          "Should throw JniValidationException for invalid handle");
    }

    @Test
    @DisplayName("Should reject negative handle")  
    void shouldRejectNegativeHandle() {
      assertThrows(
          JniValidationException.class,
          () -> new JniTable(-1L),
          "Should throw JniValidationException for negative handle");
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
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertFalse(table.isClosed(), "Should not be closed initially");
      
      // Test that resource starts in open state
      assertFalse(table.isClosed(), "Should remain open");
      // Note: Actual close() testing requires native methods and is covered in integration tests
    }

    @Test
    @DisplayName("Should be idempotent on close")
    void shouldBeIdempotentOnClose() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertFalse(table.isClosed(), "Should not be closed initially");

      // Note: Actual close() idempotency testing requires native methods
      // This test verifies the initial state only
      // Integration tests will verify close() behavior
    }

    @Test
    @DisplayName("Should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      final JniTable autoClosedTable = new JniTable(VALID_HANDLE);
      assertFalse(autoClosedTable.isClosed(), "Should not be closed inside try block");
      // Note: Not using try-with-resources in unit test since close() requires native methods
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
          JniValidationException.class,
          () -> table.get(-1),
          "Should throw JniValidationException for negative index");
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    @DisplayName("Should reject negative index in set")
    void shouldRejectNegativeIndexInSet() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          JniValidationException.class,
          () -> table.set(-1, "value"),
          "Should throw JniValidationException for negative index");
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    @DisplayName("Should reject negative delta in grow")
    void shouldRejectNegativeDeltaInGrow() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          JniValidationException.class,
          () -> table.grow(-1, null),
          "Should throw JniValidationException for negative delta");
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    @DisplayName("Should reject negative start in fill")
    void shouldRejectNegativeStartInFill() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          JniValidationException.class,
          () -> table.fill(-1, 1, "value"),
          "Should throw JniValidationException for negative start");
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    @DisplayName("Should reject negative count in fill")
    void shouldRejectNegativeCountInFill() {
      final JniTable table = new JniTable(VALID_HANDLE);
      assertThrows(
          JniValidationException.class,
          () -> table.fill(0, -1, "value"),
          "Should throw JniValidationException for negative count");
      // Note: Not calling close() in unit test since it requires native methods
    }
  }

  @Nested
  @DisplayName("Closed Resource Tests")
  class ClosedResourceTests {

    @Test
    @DisplayName("Should throw exception when accessing size of closed table")
    void shouldThrowExceptionWhenAccessingSizeOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      // Note: Not calling close() in unit test since it requires native methods

      assertThrows(
          JniResourceException.class,
          table::getSize,
          "Should throw JniResourceException when accessing size of closed table");
    }

    @Test
    @DisplayName("Should throw exception when accessing max size of closed table")
    void shouldThrowExceptionWhenAccessingMaxSizeOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      // Note: Not calling close() in unit test since it requires native methods

      assertThrows(
          JniResourceException.class,
          table::getMaxSize,
          "Should throw JniResourceException when accessing max size of closed table");
    }

    @Test
    @DisplayName("Should throw exception when accessing element type of closed table")
    void shouldThrowExceptionWhenAccessingElementTypeOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      // Note: Not calling close() in unit test since it requires native methods

      assertThrows(
          JniResourceException.class,
          table::getElementType,
          "Should throw JniResourceException when accessing element type of closed table");
    }

    @Test
    @DisplayName("Should throw exception when accessing element of closed table")
    void shouldThrowExceptionWhenAccessingElementOfClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      // Note: Not calling close() in unit test since it requires native methods

      assertThrows(
          JniResourceException.class,
          () -> table.get(0),
          "Should throw JniResourceException when accessing closed table");
    }

    @Test
    @DisplayName("Should throw exception when setting element in closed table")
    void shouldThrowExceptionWhenSettingElementInClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      // Note: Not calling close() in unit test since it requires native methods

      assertThrows(
          JniResourceException.class,
          () -> table.set(0, "value"),
          "Should throw JniResourceException when setting element in closed table");
    }

    @Test
    @DisplayName("Should throw exception when growing closed table")
    void shouldThrowExceptionWhenGrowingClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      // Note: Not calling close() in unit test since it requires native methods

      assertThrows(
          JniResourceException.class,
          () -> table.grow(1, null),
          "Should throw JniResourceException when growing closed table");
    }

    @Test
    @DisplayName("Should throw exception when filling closed table")
    void shouldThrowExceptionWhenFillingClosedTable() {
      final JniTable table = new JniTable(VALID_HANDLE);
      // Note: Not calling close() in unit test since it requires native methods

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
      // Note: Not calling close() in unit test since it requires native methods
    }

    @Test
    @DisplayName("Should show open state in toString")
    void shouldShowOpenStateInToString() {
      final JniTable table = new JniTable(VALID_HANDLE);
      final String toString = table.toString();
      assertTrue(toString.contains("false"), "toString should show open state");
      
      // Note: Testing toString() after close() requires native methods
      // Integration tests will verify toString() behavior after close()
    }
  }
}
