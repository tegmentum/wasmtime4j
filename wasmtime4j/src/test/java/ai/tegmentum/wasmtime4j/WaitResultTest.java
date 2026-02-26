package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WaitResult} enum.
 *
 * <p>Verifies enum structure, native code mapping, fromNativeCode factory method, and switch
 * exhaustiveness.
 */
@DisplayName("WaitResult Tests")
class WaitResultTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(WaitResult.class.isEnum(), "WaitResult should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactValueCount() {
      assertEquals(3, WaitResult.values().length, "WaitResult should have exactly 3 values");
    }
  }

  @Nested
  @DisplayName("Native Code Mapping Tests")
  class NativeCodeMappingTests {

    @Test
    @DisplayName("OK should have native code 0")
    void okShouldHaveNativeCodeZero() {
      assertEquals(0, WaitResult.OK.getNativeCode(), "OK native code should be 0");
    }

    @Test
    @DisplayName("MISMATCH should have native code 1")
    void mismatchShouldHaveNativeCodeOne() {
      assertEquals(1, WaitResult.MISMATCH.getNativeCode(), "MISMATCH native code should be 1");
    }

    @Test
    @DisplayName("TIMED_OUT should have native code 2")
    void timedOutShouldHaveNativeCodeTwo() {
      assertEquals(2, WaitResult.TIMED_OUT.getNativeCode(), "TIMED_OUT native code should be 2");
    }

    @Test
    @DisplayName("all native codes should be unique")
    void allNativeCodesShouldBeUnique() {
      final WaitResult[] values = WaitResult.values();
      for (int i = 0; i < values.length; i++) {
        for (int j = i + 1; j < values.length; j++) {
          assertTrue(
              values[i].getNativeCode() != values[j].getNativeCode(),
              values[i] + " and " + values[j] + " should have different native codes");
        }
      }
    }
  }

  @Nested
  @DisplayName("fromNativeCode Factory Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("fromNativeCode(0) should return OK")
    void fromNativeCodeZeroShouldReturnOk() {
      assertEquals(WaitResult.OK, WaitResult.fromNativeCode(0), "Code 0 should map to OK");
    }

    @Test
    @DisplayName("fromNativeCode(1) should return MISMATCH")
    void fromNativeCodeOneShouldReturnMismatch() {
      assertEquals(
          WaitResult.MISMATCH, WaitResult.fromNativeCode(1), "Code 1 should map to MISMATCH");
    }

    @Test
    @DisplayName("fromNativeCode(2) should return TIMED_OUT")
    void fromNativeCodeTwoShouldReturnTimedOut() {
      assertEquals(
          WaitResult.TIMED_OUT, WaitResult.fromNativeCode(2), "Code 2 should map to TIMED_OUT");
    }

    @Test
    @DisplayName("fromNativeCode should throw for negative code")
    void fromNativeCodeShouldThrowForNegativeCode() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WaitResult.fromNativeCode(-1),
              "Negative code should throw IllegalArgumentException");
      assertTrue(
          ex.getMessage().contains("-1"), "Error message should contain the invalid code: " + ex);
    }

    @Test
    @DisplayName("fromNativeCode should throw for code 3")
    void fromNativeCodeShouldThrowForCodeThree() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WaitResult.fromNativeCode(3),
          "Code 3 should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromNativeCode should throw for large code")
    void fromNativeCodeShouldThrowForLargeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WaitResult.fromNativeCode(999),
          "Code 999 should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromNativeCode roundtrips through getNativeCode")
    void fromNativeCodeRoundtrip() {
      for (final WaitResult result : WaitResult.values()) {
        assertEquals(
            result,
            WaitResult.fromNativeCode(result.getNativeCode()),
            "fromNativeCode(getNativeCode()) should return same enum: " + result);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final WaitResult value : WaitResult.values()) {
        assertEquals(
            value, WaitResult.valueOf(value.name()), "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WaitResult.valueOf("INVALID"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain OK")
    void shouldContainOk() {
      assertNotNull(WaitResult.OK, "OK constant should exist");
    }

    @Test
    @DisplayName("should contain MISMATCH")
    void shouldContainMismatch() {
      assertNotNull(WaitResult.MISMATCH, "MISMATCH constant should exist");
    }

    @Test
    @DisplayName("should contain TIMED_OUT")
    void shouldContainTimedOut() {
      assertNotNull(WaitResult.TIMED_OUT, "TIMED_OUT constant should exist");
    }
  }

  @Nested
  @DisplayName("Switch Exhaustiveness Tests")
  class SwitchExhaustivenessTests {

    @Test
    @DisplayName("all enum values should be usable in switch statement")
    void allEnumValuesShouldBeUsableInSwitch() {
      for (final WaitResult result : WaitResult.values()) {
        final String label;
        switch (result) {
          case OK:
            label = "ok";
            break;
          case MISMATCH:
            label = "mismatch";
            break;
          case TIMED_OUT:
            label = "timed_out";
            break;
          default:
            label = "unknown";
            break;
        }
        assertTrue(!label.equals("unknown"), "Should match a known case: " + result);
      }
    }
  }
}
