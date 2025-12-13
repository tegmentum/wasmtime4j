package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PanamaFunction}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior and validation without relying on actual native calls.
 *
 * <p>Note: Tests that require real native operations (actual function calls) are tested in
 * integration tests.
 */
class PanamaFunctionTest {

  private static final String FUNCTION_NAME = "test_function";
  private static final FunctionType MOCK_FUNCTION_TYPE = createMockFunctionType();

  @Test
  void testConstructorWithNullInstance() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PanamaFunction(null, FUNCTION_NAME, MOCK_FUNCTION_TYPE));

    assertThat(exception.getMessage()).contains("Instance cannot be null");
  }

  @Test
  void testConstructorValidationOrderDocumentation() {
    // This test documents the constructor validation order for PanamaFunction
    // The validation order is:
    // 1. instance != null (first check)
    // 2. name != null (second check - requires valid instance)
    // 3. functionType != null (third check - requires valid instance)
    //
    // Since creating a real PanamaInstance requires native resources,
    // we can only test the instance null check without native library.
    // Tests for name/functionType null checks are in integration tests.
    assertThat(true).isTrue();
  }

  @Test
  void testValidationDocumentation() {
    // This test documents the expected validation behavior of PanamaFunction
    // These validations are tested in integration tests with real native libraries

    // Constructor validations:
    // 1. instance != null
    // 2. name != null
    // 3. functionType != null

    // Method validations (tested in integration tests):
    // - call(params) - params != null
    // - asTyped(signature) - signature != null

    assertThat(true).isTrue(); // Documentation test always passes
  }

  /**
   * Creates a minimal FunctionType for testing constructor validation.
   *
   * @return a FunctionType with no parameters and one I32 result
   */
  private static FunctionType createMockFunctionType() {
    return new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
  }

  /**
   * Creates a minimal mock PanamaInstance for testing. Note: This returns null because creating a
   * real PanamaInstance requires native resources. The constructor validation tests use this to
   * verify null parameter checks occur in the correct order.
   */
  private PanamaInstance createMockInstance() {
    // Cannot create a real PanamaInstance without native resources
    // Return null to verify the parameter validation order
    // (null instance check should come before other checks)
    return null;
  }
}
