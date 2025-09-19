package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive tests for OptimizationLevel enum.
 *
 * <p>Tests all optimization levels, value mappings, and edge cases to ensure the optimization
 * level API works correctly with native value conversions.
 */
@DisplayName("OptimizationLevel Tests")
final class OptimizationLevelTest {

  @Nested
  @DisplayName("Value Mapping Tests")
  class ValueMappingTests {

    @Test
    @DisplayName("NONE maps to value 0")
    void testNoneValue() {
      assertEquals(0, OptimizationLevel.NONE.getValue(), "NONE should map to value 0");
    }

    @Test
    @DisplayName("SPEED maps to value 1")
    void testSpeedValue() {
      assertEquals(1, OptimizationLevel.SPEED.getValue(), "SPEED should map to value 1");
    }

    @Test
    @DisplayName("SIZE maps to value 2")
    void testSizeValue() {
      assertEquals(2, OptimizationLevel.SIZE.getValue(), "SIZE should map to value 2");
    }

    @Test
    @DisplayName("SPEED_AND_SIZE maps to value 2")
    void testSpeedAndSizeValue() {
      assertEquals(
          2,
          OptimizationLevel.SPEED_AND_SIZE.getValue(),
          "SPEED_AND_SIZE should map to value 2");
    }

    @ParameterizedTest
    @EnumSource(OptimizationLevel.class)
    @DisplayName("All optimization levels have valid values")
    void testAllLevelsHaveValidValues(final OptimizationLevel level) {
      final int value = level.getValue();
      assertTrue(
          value >= 0 && value <= 2, "All optimization levels should have values 0-2");
    }

    private void assertTrue(final boolean condition, final String message) {
      if (!condition) {
        throw new AssertionError(message);
      }
    }
  }

  @Nested
  @DisplayName("fromValue() Method Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue(0) returns NONE")
    void testFromValueZero() {
      assertEquals(
          OptimizationLevel.NONE,
          OptimizationLevel.fromValue(0),
          "fromValue(0) should return NONE");
    }

    @Test
    @DisplayName("fromValue(1) returns SPEED")
    void testFromValueOne() {
      assertEquals(
          OptimizationLevel.SPEED,
          OptimizationLevel.fromValue(1),
          "fromValue(1) should return SPEED");
    }

    @Test
    @DisplayName("fromValue(2) returns SIZE")
    void testFromValueTwo() {
      assertEquals(
          OptimizationLevel.SIZE,
          OptimizationLevel.fromValue(2),
          "fromValue(2) should return SIZE (note: SPEED_AND_SIZE also maps to 2)");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, 3, 4, 10, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("fromValue() throws IllegalArgumentException for invalid values")
    void testFromValueInvalid(final int invalidValue) {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> OptimizationLevel.fromValue(invalidValue),
              "fromValue() should throw IllegalArgumentException for invalid value: "
                  + invalidValue);

      assertEquals(
          "Invalid optimization level: " + invalidValue,
          exception.getMessage(),
          "Exception message should include the invalid value");
    }

    @Test
    @DisplayName("Round-trip conversion preserves values for known mappings")
    void testRoundTripConversion() {
      // Test NONE
      final OptimizationLevel noneFromValue = OptimizationLevel.fromValue(OptimizationLevel.NONE.getValue());
      assertEquals(OptimizationLevel.NONE, noneFromValue, "NONE round-trip should be preserved");

      // Test SPEED
      final OptimizationLevel speedFromValue = OptimizationLevel.fromValue(OptimizationLevel.SPEED.getValue());
      assertEquals(OptimizationLevel.SPEED, speedFromValue, "SPEED round-trip should be preserved");

      // Test SIZE (note: this will return SIZE, not SPEED_AND_SIZE)
      final OptimizationLevel sizeFromValue = OptimizationLevel.fromValue(OptimizationLevel.SIZE.getValue());
      assertEquals(OptimizationLevel.SIZE, sizeFromValue, "SIZE round-trip should return SIZE");

      // Test SPEED_AND_SIZE (this will return SIZE because both map to value 2)
      final OptimizationLevel speedAndSizeFromValue = OptimizationLevel.fromValue(OptimizationLevel.SPEED_AND_SIZE.getValue());
      assertEquals(OptimizationLevel.SIZE, speedAndSizeFromValue, "SPEED_AND_SIZE round-trip should return SIZE");
    }
  }

  @Nested
  @DisplayName("Edge Cases and Special Behavior")
  class EdgeCasesTests {

    @Test
    @DisplayName("SIZE and SPEED_AND_SIZE both map to value 2")
    void testSizeAndSpeedAndSizeMapping() {
      final int sizeValue = OptimizationLevel.SIZE.getValue();
      final int speedAndSizeValue = OptimizationLevel.SPEED_AND_SIZE.getValue();

      assertEquals(
          sizeValue,
          speedAndSizeValue,
          "SIZE and SPEED_AND_SIZE should both map to the same value");
      assertEquals(2, sizeValue, "Both should map to value 2");
    }

    @Test
    @DisplayName("fromValue() with SIZE/SPEED_AND_SIZE shared value returns SIZE")
    void testFromValueSharedMapping() {
      final OptimizationLevel result = OptimizationLevel.fromValue(2);
      assertEquals(
          OptimizationLevel.SIZE,
          result,
          "fromValue(2) should return SIZE (the first match in the switch statement)");
    }

    @Test
    @DisplayName("All enum values are distinct except for their native mappings")
    void testEnumDistinctness() {
      final OptimizationLevel[] levels = OptimizationLevel.values();
      assertEquals(4, levels.length, "Should have exactly 4 optimization levels");

      // Verify all enum constants are present
      boolean hasNone = false;
      boolean hasSpeed = false;
      boolean hasSize = false;
      boolean hasSpeedAndSize = false;

      for (final OptimizationLevel level : levels) {
        switch (level) {
          case NONE:
            hasNone = true;
            break;
          case SPEED:
            hasSpeed = true;
            break;
          case SIZE:
            hasSize = true;
            break;
          case SPEED_AND_SIZE:
            hasSpeedAndSize = true;
            break;
          default:
            // Should not happen with known enum values
            break;
        }
      }

      assertTrue(hasNone, "Should have NONE");
      assertTrue(hasSpeed, "Should have SPEED");
      assertTrue(hasSize, "Should have SIZE");
      assertTrue(hasSpeedAndSize, "Should have SPEED_AND_SIZE");
    }

    private void assertTrue(final boolean condition, final String message) {
      if (!condition) {
        throw new AssertionError(message);
      }
    }
  }

  @Nested
  @DisplayName("Enum Contract Tests")
  class EnumContractTests {

    @ParameterizedTest
    @EnumSource(OptimizationLevel.class)
    @DisplayName("toString() returns enum name")
    void testToString(final OptimizationLevel level) {
      final String toString = level.toString();
      final String name = level.name();
      assertEquals(name, toString, "toString() should return the enum name");
    }

    @ParameterizedTest
    @EnumSource(OptimizationLevel.class)
    @DisplayName("valueOf() works for all enum constants")
    void testValueOf(final OptimizationLevel level) {
      final String name = level.name();
      final OptimizationLevel parsed = OptimizationLevel.valueOf(name);
      assertEquals(level, parsed, "valueOf() should return the same enum constant");
    }

    @Test
    @DisplayName("values() returns all optimization levels")
    void testValues() {
      final OptimizationLevel[] values = OptimizationLevel.values();
      assertEquals(4, values.length, "Should return exactly 4 optimization levels");

      // Verify specific order (implementation dependent but useful for consistency)
      assertEquals(OptimizationLevel.NONE, values[0], "NONE should be first");
      assertEquals(OptimizationLevel.SPEED, values[1], "SPEED should be second");
      assertEquals(OptimizationLevel.SIZE, values[2], "SIZE should be third");
      assertEquals(OptimizationLevel.SPEED_AND_SIZE, values[3], "SPEED_AND_SIZE should be fourth");
    }
  }
}