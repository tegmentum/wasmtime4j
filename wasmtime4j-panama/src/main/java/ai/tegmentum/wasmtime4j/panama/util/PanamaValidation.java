package ai.tegmentum.wasmtime4j.panama.util;

import ai.tegmentum.wasmtime4j.util.Validation;
import java.lang.foreign.MemorySegment;

/**
 * Panama-specific validation methods that extend the base {@link Validation} utility.
 *
 * <p>This class contains only validation methods that require Panama Foreign Function API types
 * (e.g., {@link MemorySegment}). For all other validation needs, use {@link Validation} directly.
 *
 * @since 1.0.0
 */
public final class PanamaValidation {

  /** Private constructor to prevent instantiation of utility class. */
  private PanamaValidation() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Validates that a native memory segment handle is valid (not null and not {@link
   * MemorySegment#NULL}).
   *
   * @param handle the memory segment handle to validate
   * @param handleName the handle name for error messages
   * @throws IllegalArgumentException if the handle is null or equals {@link MemorySegment#NULL}
   */
  public static void requireValidHandle(
      final MemorySegment handle, final String handleName) {
    Validation.requireNonNull(handle, handleName);
    if (handle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException(
          "Native handle '" + handleName + "' is invalid (null pointer)");
    }
  }
}
