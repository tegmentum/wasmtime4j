package ai.tegmentum.wasmtime4j.comparison.generated.misctestsuite;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java test for Wasmtime test: misc_testsuite::mutable_externref_globals
 *
 * <p>Original source: mutable_externref_globals.wast:1 Category: misc_testsuite
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class MutableExternrefGlobalsTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("misc_testsuite::mutable_externref_globals")
  public void testMutableExternrefGlobals(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    // WAT module with mutable externref global
    final String wat =
        """
        (module
          (global $mr (mut externref) (ref.null extern))
          (func (export "get-mr") (result externref) (global.get $mr))
          (func (export "set-mr") (param externref) (global.set $mr (local.get 0)))
        )
    """;

    try (final ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner runner =
        new ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner()) {

      // Compile and instantiate the module
      runner.compileAndInstantiate(wat);

      // Test 1: get-mr should return null externref initially
      final ai.tegmentum.wasmtime4j.WasmValue[] result1 = runner.invoke("get-mr");
      org.junit.jupiter.api.Assertions.assertEquals(
          1, result1.length, "get-mr should return 1 value");
      org.junit.jupiter.api.Assertions.assertEquals(
          ai.tegmentum.wasmtime4j.WasmValueType.EXTERNREF,
          result1[0].getType(),
          "get-mr should return externref");
      org.junit.jupiter.api.Assertions.assertNull(
          result1[0].asExternref(), "get-mr should return null externref initially");

      // Test 2: set-mr with externref value (10)
      // Note: ref.extern in WAT maps to externref with a Long value
      final ai.tegmentum.wasmtime4j.WasmValue[] result2 =
          runner.invoke("set-mr", ai.tegmentum.wasmtime4j.WasmValue.externref(10L));
      org.junit.jupiter.api.Assertions.assertEquals(
          0, result2.length, "set-mr should return no values");

      // Test 3: get-mr should now return the set value (10)
      final ai.tegmentum.wasmtime4j.WasmValue[] result3 = runner.invoke("get-mr");
      org.junit.jupiter.api.Assertions.assertEquals(
          1, result3.length, "get-mr should return 1 value");
      org.junit.jupiter.api.Assertions.assertEquals(
          ai.tegmentum.wasmtime4j.WasmValueType.EXTERNREF,
          result3[0].getType(),
          "get-mr should return externref");
      org.junit.jupiter.api.Assertions.assertEquals(
          10L, result3[0].asExternref(), "get-mr should return externref with value 10");
    }
  }
}
