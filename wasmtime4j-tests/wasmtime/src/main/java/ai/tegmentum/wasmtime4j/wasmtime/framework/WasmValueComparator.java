package ai.tegmentum.wasmtime4j.wasmtime.framework;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Objects;

/**
 * Shared utility for comparing {@link WasmValue} instances in WAST test runners.
 *
 * <p>Handles NaN equality correctly: {@code NaN == NaN} is true for both F32 and F64, which matches
 * the WebAssembly test suite semantics where {@code assert_return} with NaN expects NaN.
 *
 * @since 1.0.0
 */
public final class WasmValueComparator {

  private WasmValueComparator() {
    // Utility class
  }

  /**
   * Compares two WasmValue instances for equality, including NaN-equals-NaN semantics.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @return true if the values are equal
   */
  public static boolean valuesEqual(final WasmValue expected, final WasmValue actual) {
    if (expected.getType() != actual.getType()) {
      return false;
    }

    switch (expected.getType()) {
      case I32:
        return expected.asInt() == actual.asInt();
      case I64:
        return expected.asLong() == actual.asLong();
      case F32:
        return Float.compare(expected.asFloat(), actual.asFloat()) == 0
            || (Float.isNaN(expected.asFloat()) && Float.isNaN(actual.asFloat()));
      case F64:
        return Double.compare(expected.asDouble(), actual.asDouble()) == 0
            || (Double.isNaN(expected.asDouble()) && Double.isNaN(actual.asDouble()));
      case EXTERNREF:
      case FUNCREF:
      case V128:
        return Objects.equals(expected, actual);
      default:
        return false;
    }
  }

  /**
   * Compares two WasmValue arrays for equality using NaN-equals-NaN semantics.
   *
   * @param expected the expected values
   * @param actual the actual values
   * @return true if both arrays have the same length and all elements are equal
   */
  public static boolean arraysEqual(final WasmValue[] expected, final WasmValue[] actual) {
    if (expected.length != actual.length) {
      return false;
    }
    for (int i = 0; i < expected.length; i++) {
      if (!valuesEqual(expected[i], actual[i])) {
        return false;
      }
    }
    return true;
  }
}
