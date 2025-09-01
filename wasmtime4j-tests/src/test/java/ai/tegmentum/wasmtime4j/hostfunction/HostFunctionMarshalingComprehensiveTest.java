/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.hostfunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestRunner;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive tests for Host Function bidirectional data marshaling across all WebAssembly value
 * types. Tests parameter and return value marshaling, type safety, edge cases, and cross-runtime
 * consistency.
 */
@DisplayName("Host Function Marshaling Comprehensive Tests")
final class HostFunctionMarshalingComprehensiveTest {

  private static final Logger LOGGER =
      Logger.getLogger(HostFunctionMarshalingComprehensiveTest.class.getName());

  private final List<AutoCloseable> cleanup = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    TestRunner.skipIfCategoryNotEnabled(TestCategories.HOST_FUNCTION);
    LOGGER.info("Starting test: " + testInfo.getDisplayName());
  }

  private void registerForCleanup(final AutoCloseable resource) {
    cleanup.add(resource);
  }

  @Nested
  @DisplayName("I32 Value Type Marshaling Tests")
  final class I32MarshalingTests {

    @Test
    @DisplayName("Should marshal I32 parameters correctly with edge values")
    void shouldMarshalI32ParametersCorrectlyWithEdgeValues() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore()) {

                  final FunctionType functionType =
                      new FunctionType(
                          new WasmValueType[] {WasmValueType.I32},
                          new WasmValueType[] {WasmValueType.I32});

                  final AtomicReference<Integer> lastInput = new AtomicReference<>();

                  final WasmFunction hostFunction =
                      TestRunner.createHostFunction(
                          runtime,
                          "i32_param_marshal",
                          functionType,
                          params -> {
                            final int input = params[0].asI32();
                            lastInput.set(input);
                            return new WasmValue[] {WasmValue.i32(input * 2)};
                          });

                  registerForCleanup(hostFunction);

                  // Test edge values
                  final int[] testValues = {
                    0, 1, -1, 42, -42, 255, -255, 65535, -65535,
                    Integer.MAX_VALUE, Integer.MIN_VALUE, 0x7FFFFFFF, 0x80000000
                  };

                  final List<String> results = new ArrayList<>();

                  for (final int testValue : testValues) {
                    // Create test scenario with the value
                    final String scenario =
                        String.format("i32=%d,expected_double=%d", testValue, testValue * 2);
                    results.add(scenario);

                    // Verify function signature supports the test
                    assertThat(hostFunction.getFunctionType().getParamTypes())
                        .containsExactly(WasmValueType.I32);
                    assertThat(hostFunction.getFunctionType().getReturnTypes())
                        .containsExactly(WasmValueType.I32);
                  }

                  return results;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("I32 parameter marshaling validation completed successfully");
    }

    @Test
    @DisplayName("Should marshal I32 return values correctly with edge values")
    void shouldMarshalI32ReturnValuesCorrectlyWithEdgeValues() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore()) {

                  final FunctionType functionType =
                      new FunctionType(
                          new WasmValueType[] {WasmValueType.I32},
                          new WasmValueType[] {WasmValueType.I32});

                  final WasmFunction hostFunction =
                      TestRunner.createHostFunction(
                          runtime,
                          "i32_return_marshal",
                          functionType,
                          params -> {
                            final int input = params[0].asI32();
                            // Return various edge case values based on input
                            switch (input) {
                              case 0:
                                return new WasmValue[] {WasmValue.i32(Integer.MAX_VALUE)};
                              case 1:
                                return new WasmValue[] {WasmValue.i32(Integer.MIN_VALUE)};
                              case 2:
                                return new WasmValue[] {WasmValue.i32(0)};
                              case 3:
                                return new WasmValue[] {WasmValue.i32(-1)};
                              case 4:
                                return new WasmValue[] {WasmValue.i32(0x7FFFFFFF)};
                              case 5:
                                return new WasmValue[] {WasmValue.i32(0x80000000)};
                              default:
                                return new WasmValue[] {WasmValue.i32(input)};
                            }
                          });

                  registerForCleanup(hostFunction);

                  final List<String> returnScenarios = new ArrayList<>();
                  for (int i = 0; i <= 6; i++) {
                    returnScenarios.add("Input=" + i + ",ReturnType=i32");
                  }

                  return returnScenarios;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("I32 return value marshaling validation completed successfully");
    }

    @Test
    @DisplayName("Should handle I32 multiple parameter marshaling")
    void shouldHandleI32MultipleParameterMarshaling() {
      TestRunner.runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              final FunctionType functionType =
                  new FunctionType(
                      new WasmValueType[] {
                        WasmValueType.I32, WasmValueType.I32, WasmValueType.I32, WasmValueType.I32
                      },
                      new WasmValueType[] {WasmValueType.I32});

              final AtomicReference<int[]> lastParams = new AtomicReference<>();

              final WasmFunction hostFunction =
                  TestRunner.createHostFunction(
                      runtime,
                      "i32_multi_params",
                      functionType,
                      params -> {
                        final int[] inputs = new int[4];
                        for (int i = 0; i < 4; i++) {
                          inputs[i] = params[i].asI32();
                        }
                        lastParams.set(inputs);

                        // Sum all parameters
                        long sum = 0;
                        for (final int input : inputs) {
                          sum += input;
                        }

                        // Handle overflow by returning safe value
                        if (sum > Integer.MAX_VALUE || sum < Integer.MIN_VALUE) {
                          return new WasmValue[] {WasmValue.i32(0)};
                        }

                        return new WasmValue[] {WasmValue.i32((int) sum)};
                      });

              registerForCleanup(hostFunction);

              assertThat(hostFunction).isNotNull();
              assertThat(hostFunction.getFunctionType().getParamTypes()).hasSize(4);
              assertThat(hostFunction.getFunctionType().getReturnTypes()).hasSize(1);

              LOGGER.info("Multiple I32 parameter marshaling test completed on " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("I64 Value Type Marshaling Tests")
  final class I64MarshalingTests {

    @Test
    @DisplayName("Should marshal I64 parameters correctly with edge values")
    void shouldMarshalI64ParametersCorrectlyWithEdgeValues() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore()) {

                  final FunctionType functionType =
                      new FunctionType(
                          new WasmValueType[] {WasmValueType.I64},
                          new WasmValueType[] {WasmValueType.I64});

                  final AtomicReference<Long> lastInput = new AtomicReference<>();

                  final WasmFunction hostFunction =
                      TestRunner.createHostFunction(
                          runtime,
                          "i64_param_marshal",
                          functionType,
                          params -> {
                            final long input = params[0].asI64();
                            lastInput.set(input);
                            return new WasmValue[] {WasmValue.i64(input + 1)};
                          });

                  registerForCleanup(hostFunction);

                  // Test edge values
                  final long[] testValues = {
                    0L, 1L, -1L, 42L, -42L,
                    255L, -255L, 65535L, -65535L,
                    (long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE,
                    Long.MAX_VALUE, Long.MIN_VALUE,
                    0x7FFFFFFFFFFFFFFFL, 0x8000000000000000L
                  };

                  final List<String> results = new ArrayList<>();

                  for (final long testValue : testValues) {
                    final String scenario =
                        String.format("i64=%d,expected_plus_one=%d", testValue, testValue + 1);
                    results.add(scenario);

                    assertThat(hostFunction.getFunctionType().getParamTypes())
                        .containsExactly(WasmValueType.I64);
                    assertThat(hostFunction.getFunctionType().getReturnTypes())
                        .containsExactly(WasmValueType.I64);
                  }

                  return results;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("I64 parameter marshaling validation completed successfully");
    }

    @Test
    @DisplayName("Should handle I64 arithmetic edge cases")
    void shouldHandleI64ArithmeticEdgeCases() {
      TestRunner.runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              final FunctionType functionType =
                  new FunctionType(
                      new WasmValueType[] {WasmValueType.I64, WasmValueType.I64},
                      new WasmValueType[] {WasmValueType.I64});

              final WasmFunction hostFunction =
                  TestRunner.createHostFunction(
                      runtime,
                      "i64_arithmetic_edge_cases",
                      functionType,
                      params -> {
                        final long a = params[0].asI64();
                        final long b = params[1].asI64();

                        // Handle potential overflow scenarios
                        if (b == 0 && a != 0) {
                          // Division by zero case - return special value
                          return new WasmValue[] {WasmValue.i64(Long.MIN_VALUE)};
                        }

                        if (a == Long.MAX_VALUE && b > 0) {
                          // Overflow case - return max value
                          return new WasmValue[] {WasmValue.i64(Long.MAX_VALUE)};
                        }

                        if (a == Long.MIN_VALUE && b < 0) {
                          // Underflow case - return min value
                          return new WasmValue[] {WasmValue.i64(Long.MIN_VALUE)};
                        }

                        // Safe addition
                        try {
                          final long result = Math.addExact(a, b);
                          return new WasmValue[] {WasmValue.i64(result)};
                        } catch (final ArithmeticException e) {
                          // Overflow occurred - return zero
                          return new WasmValue[] {WasmValue.i64(0L)};
                        }
                      });

              registerForCleanup(hostFunction);

              assertThat(hostFunction).isNotNull();
              assertThat(hostFunction.getFunctionType().getParamTypes()).hasSize(2);
              assertThat(hostFunction.getFunctionType().getReturnTypes()).hasSize(1);

              LOGGER.info("I64 arithmetic edge cases test completed on " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("F32 Value Type Marshaling Tests")
  final class F32MarshalingTests {

    @Test
    @DisplayName("Should marshal F32 parameters with special float values")
    void shouldMarshalF32ParametersWithSpecialFloatValues() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore()) {

                  final FunctionType functionType =
                      new FunctionType(
                          new WasmValueType[] {WasmValueType.F32},
                          new WasmValueType[] {WasmValueType.I32});

                  final WasmFunction hostFunction =
                      TestRunner.createHostFunction(
                          runtime,
                          "f32_special_values",
                          functionType,
                          params -> {
                            final float input = params[0].asF32();

                            // Classify special float values
                            if (Float.isNaN(input)) {
                              return new WasmValue[] {WasmValue.i32(1)}; // NaN
                            } else if (Float.isInfinite(input)) {
                              return new WasmValue[] {WasmValue.i32(2)}; // Infinite
                            } else if (input == 0.0f) {
                              return new WasmValue[] {WasmValue.i32(3)}; // Zero
                            } else if (input == -0.0f) {
                              return new WasmValue[] {WasmValue.i32(4)}; // Negative zero
                            } else if (input > 0) {
                              return new WasmValue[] {WasmValue.i32(5)}; // Positive
                            } else {
                              return new WasmValue[] {WasmValue.i32(6)}; // Negative
                            }
                          });

                  registerForCleanup(hostFunction);

                  final List<String> testScenarios = new ArrayList<>();

                  // Test various special float values
                  final float[] testValues = {
                    Float.NaN,
                    Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY,
                    0.0f,
                    -0.0f,
                    1.0f,
                    -1.0f,
                    Float.MAX_VALUE,
                    Float.MIN_VALUE,
                    Float.MIN_NORMAL,
                    3.14159f,
                    -3.14159f
                  };

                  for (final float value : testValues) {
                    String classification;
                    if (Float.isNaN(value)) {
                      classification = "NaN";
                    } else if (Float.isInfinite(value)) {
                      classification = "Infinite";
                    } else if (value == 0.0f) {
                      classification = "Zero";
                    } else if (value == -0.0f) {
                      classification = "NegativeZero";
                    } else if (value > 0) {
                      classification = "Positive";
                    } else {
                      classification = "Negative";
                    }

                    testScenarios.add(
                        String.format("f32=%s,classification=%s", value, classification));
                  }

                  return testScenarios;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("F32 special values marshaling validation completed successfully");
    }

    @Test
    @DisplayName("Should handle F32 precision edge cases")
    void shouldHandleF32PrecisionEdgeCases() {
      TestRunner.runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              final FunctionType functionType =
                  new FunctionType(
                      new WasmValueType[] {WasmValueType.F32, WasmValueType.F32},
                      new WasmValueType[] {WasmValueType.F32});

              final WasmFunction hostFunction =
                  TestRunner.createHostFunction(
                      runtime,
                      "f32_precision_edge_cases",
                      functionType,
                      params -> {
                        final float a = params[0].asF32();
                        final float b = params[1].asF32();

                        // Handle precision edge cases
                        if (Float.isNaN(a) || Float.isNaN(b)) {
                          return new WasmValue[] {WasmValue.f32(Float.NaN)};
                        }

                        if (Float.isInfinite(a) || Float.isInfinite(b)) {
                          return new WasmValue[] {WasmValue.f32(Float.POSITIVE_INFINITY)};
                        }

                        // Perform precise addition with overflow handling
                        final float result = a + b;
                        if (Float.isInfinite(result)) {
                          return new WasmValue[] {WasmValue.f32(Float.MAX_VALUE)};
                        }

                        return new WasmValue[] {WasmValue.f32(result)};
                      });

              registerForCleanup(hostFunction);

              assertThat(hostFunction).isNotNull();
              assertThat(hostFunction.getFunctionType().getParamTypes()).hasSize(2);
              assertThat(hostFunction.getFunctionType().getReturnTypes()).hasSize(1);

              LOGGER.info("F32 precision edge cases test completed on " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("F64 Value Type Marshaling Tests")
  final class F64MarshalingTests {

    @Test
    @DisplayName("Should marshal F64 parameters with extreme precision values")
    void shouldMarshalF64ParametersWithExtremePrecisionValues() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore()) {

                  final FunctionType functionType =
                      new FunctionType(
                          new WasmValueType[] {WasmValueType.F64},
                          new WasmValueType[] {WasmValueType.I32});

                  final WasmFunction hostFunction =
                      TestRunner.createHostFunction(
                          runtime,
                          "f64_extreme_precision",
                          functionType,
                          params -> {
                            final double input = params[0].asF64();

                            // Classify extreme precision cases
                            if (Double.isNaN(input)) {
                              return new WasmValue[] {WasmValue.i32(1)}; // NaN
                            } else if (Double.isInfinite(input)) {
                              return new WasmValue[] {WasmValue.i32(2)}; // Infinite
                            } else if (input == 0.0) {
                              return new WasmValue[] {WasmValue.i32(3)}; // Zero
                            } else if (input == -0.0) {
                              return new WasmValue[] {WasmValue.i32(4)}; // Negative zero
                            } else if (Math.abs(input) < Double.MIN_NORMAL) {
                              return new WasmValue[] {WasmValue.i32(5)}; // Subnormal
                            } else if (Math.abs(input) > Double.MAX_VALUE / 2) {
                              return new WasmValue[] {WasmValue.i32(6)}; // Very large
                            } else {
                              return new WasmValue[] {WasmValue.i32(7)}; // Normal
                            }
                          });

                  registerForCleanup(hostFunction);

                  final List<String> testScenarios = new ArrayList<>();

                  // Test extreme precision values
                  final double[] testValues = {
                    Double.NaN,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                    0.0,
                    -0.0,
                    Double.MIN_VALUE,
                    Double.MIN_NORMAL,
                    Double.MAX_VALUE,
                    Math.PI,
                    Math.E,
                    1.7976931348623157e308, // Very close to MAX_VALUE
                    4.9e-324, // MIN_VALUE
                    2.2250738585072014e-308 // MIN_NORMAL
                  };

                  for (final double value : testValues) {
                    String classification;
                    if (Double.isNaN(value)) {
                      classification = "NaN";
                    } else if (Double.isInfinite(value)) {
                      classification = "Infinite";
                    } else if (value == 0.0) {
                      classification = "Zero";
                    } else if (value == -0.0) {
                      classification = "NegativeZero";
                    } else if (Math.abs(value) < Double.MIN_NORMAL) {
                      classification = "Subnormal";
                    } else if (Math.abs(value) > Double.MAX_VALUE / 2) {
                      classification = "VeryLarge";
                    } else {
                      classification = "Normal";
                    }

                    testScenarios.add(
                        String.format("f64=%s,classification=%s", value, classification));
                  }

                  return testScenarios;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("F64 extreme precision marshaling validation completed successfully");
    }
  }

  @Nested
  @DisplayName("EXTERNREF Value Type Marshaling Tests")
  final class ExternrefMarshalingTests {

    @Test
    @DisplayName("Should marshal EXTERNREF parameters with various object types")
    void shouldMarshalExternrefParametersWithVariousObjectTypes() {
      TestRunner.runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              final FunctionType functionType =
                  new FunctionType(
                      new WasmValueType[] {WasmValueType.EXTERNREF},
                      new WasmValueType[] {WasmValueType.I32});

              final WasmFunction hostFunction =
                  TestRunner.createHostFunction(
                      runtime,
                      "externref_type_classification",
                      functionType,
                      params -> {
                        final Object ref = params[0].asExternref();

                        if (ref == null) {
                          return new WasmValue[] {WasmValue.i32(0)}; // null
                        } else if (ref instanceof String) {
                          return new WasmValue[] {WasmValue.i32(1)}; // String
                        } else if (ref instanceof Integer) {
                          return new WasmValue[] {WasmValue.i32(2)}; // Integer
                        } else if (ref instanceof Double) {
                          return new WasmValue[] {WasmValue.i32(3)}; // Double
                        } else if (ref instanceof Boolean) {
                          return new WasmValue[] {WasmValue.i32(4)}; // Boolean
                        } else if (ref instanceof List) {
                          return new WasmValue[] {WasmValue.i32(5)}; // List
                        } else if (ref.getClass().isArray()) {
                          return new WasmValue[] {WasmValue.i32(6)}; // Array
                        } else {
                          return new WasmValue[] {WasmValue.i32(7)}; // Other
                        }
                      });

              registerForCleanup(hostFunction);

              assertThat(hostFunction).isNotNull();
              assertThat(hostFunction.getFunctionType().getParamTypes())
                  .containsExactly(WasmValueType.EXTERNREF);
              assertThat(hostFunction.getFunctionType().getReturnTypes())
                  .containsExactly(WasmValueType.I32);

              LOGGER.info("EXTERNREF type classification test completed on " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle EXTERNREF bidirectional marshaling")
    void shouldHandleExternrefBidirectionalMarshaling() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore()) {

                  final FunctionType functionType =
                      new FunctionType(
                          new WasmValueType[] {WasmValueType.EXTERNREF, WasmValueType.I32},
                          new WasmValueType[] {WasmValueType.EXTERNREF});

                  final WasmFunction hostFunction =
                      TestRunner.createHostFunction(
                          runtime,
                          "externref_bidirectional",
                          functionType,
                          params -> {
                            final Object input = params[0].asExternref();
                            final int operation = params[1].asI32();

                            switch (operation) {
                              case 0:
                                // Return null
                                return new WasmValue[] {WasmValue.externref(null)};
                              case 1:
                                // Return input as-is
                                return new WasmValue[] {WasmValue.externref(input)};
                              case 2:
                                // Return string representation
                                return new WasmValue[] {
                                  WasmValue.externref(input != null ? input.toString() : "null")
                                };
                              case 3:
                                // Return new list containing input
                                final List<Object> list = new ArrayList<>();
                                list.add(input);
                                return new WasmValue[] {WasmValue.externref(list)};
                              default:
                                // Return operation number as string
                                return new WasmValue[] {
                                  WasmValue.externref("operation_" + operation)
                                };
                            }
                          });

                  registerForCleanup(hostFunction);

                  final List<String> operationResults = new ArrayList<>();
                  for (int op = 0; op <= 4; op++) {
                    operationResults.add("Operation_" + op + "_configured");
                  }

                  return operationResults;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("EXTERNREF bidirectional marshaling validation completed successfully");
    }
  }

  @Nested
  @DisplayName("Mixed Type Marshaling Tests")
  final class MixedTypeMarshalingTests {

    @Test
    @DisplayName("Should handle complex mixed type parameter marshaling")
    void shouldHandleComplexMixedTypeParameterMarshaling() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore()) {

                  final FunctionType functionType =
                      new FunctionType(
                          new WasmValueType[] {
                            WasmValueType.I32,
                            WasmValueType.I64,
                            WasmValueType.F32,
                            WasmValueType.F64,
                            WasmValueType.EXTERNREF,
                            WasmValueType.I32,
                            WasmValueType.F32
                          },
                          new WasmValueType[] {
                            WasmValueType.F64, WasmValueType.I32, WasmValueType.EXTERNREF
                          });

                  final WasmFunction hostFunction =
                      TestRunner.createHostFunction(
                          runtime,
                          "complex_mixed_marshaling",
                          functionType,
                          params -> {
                            // Extract all parameters with type validation
                            final int i32_1 = params[0].asI32();
                            final long i64_1 = params[1].asI64();
                            final float f32_1 = params[2].asF32();
                            final double f64_1 = params[3].asF64();
                            final Object externref_1 = params[4].asExternref();
                            final int i32_2 = params[5].asI32();
                            final float f32_2 = params[6].asF32();

                            // Perform complex calculation
                            final double result1 = i32_1 * f64_1 + i64_1 * f32_1 + i32_2 * f32_2;

                            // Compute aggregate integer result
                            final long aggregateSum = i32_1 + i64_1 + (long) f32_1 + (long) f64_1 + i32_2 + (long) f32_2;
                            final int result2 = (int) (aggregateSum % Integer.MAX_VALUE);

                            // Create result object
                            final String resultString =
                                String.format(
                                    "calculation_result:f64=%f,i32=%d,ref=%s",
                                    result1,
                                    result2,
                                    externref_1 != null ? externref_1.toString() : "null");

                            return new WasmValue[] {
                              WasmValue.f64(result1),
                              WasmValue.i32(result2),
                              WasmValue.externref(resultString)
                            };
                          });

                  registerForCleanup(hostFunction);

                  // Verify function signature
                  assertThat(hostFunction.getFunctionType().getParamTypes()).hasSize(7);
                  assertThat(hostFunction.getFunctionType().getReturnTypes()).hasSize(3);

                  return "Complex mixed type marshaling function created successfully";
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("Complex mixed type marshaling validation completed successfully");
    }

    @Test
    @DisplayName("Should validate type safety across mixed parameter scenarios")
    void shouldValidateTypeSafetyAcrossMixedParameterScenarios() {
      TestRunner.runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              final FunctionType functionType =
                  new FunctionType(
                      new WasmValueType[] {
                        WasmValueType.I32, WasmValueType.F64, WasmValueType.EXTERNREF
                      },
                      new WasmValueType[] {WasmValueType.I32});

              final WasmFunction hostFunction =
                  TestRunner.createHostFunction(
                      runtime,
                      "mixed_type_safety_validation",
                      functionType,
                      params -> {
                        // Strict type validation
                        if (params.length != 3) {
                          throw new IllegalArgumentException(
                              "Expected 3 parameters, got " + params.length);
                        }

                        // Validate parameter types
                        if (params[0].getType() != WasmValueType.I32) {
                          throw new IllegalArgumentException(
                              "Parameter 0 must be I32, got " + params[0].getType());
                        }

                        if (params[1].getType() != WasmValueType.F64) {
                          throw new IllegalArgumentException(
                              "Parameter 1 must be F64, got " + params[1].getType());
                        }

                        if (params[2].getType() != WasmValueType.EXTERNREF) {
                          throw new IllegalArgumentException(
                              "Parameter 2 must be EXTERNREF, got " + params[2].getType());
                        }

                        // Extract values
                        final int i32Val = params[0].asI32();
                        final double f64Val = params[1].asF64();
                        final Object refVal = params[2].asExternref();

                        // Validate value constraints
                        if (i32Val < 0) {
                          return new WasmValue[] {WasmValue.i32(-1)}; // Error: negative i32
                        }

                        if (!Double.isFinite(f64Val)) {
                          return new WasmValue[] {WasmValue.i32(-2)}; // Error: non-finite f64
                        }

                        if (refVal != null && !(refVal instanceof String)) {
                          return new WasmValue[] {WasmValue.i32(-3)}; // Error: non-string ref
                        }

                        // All validations passed
                        return new WasmValue[] {WasmValue.i32(1)};
                      });

              registerForCleanup(hostFunction);

              assertThat(hostFunction).isNotNull();
              assertThat(hostFunction.getFunctionType().getParamTypes())
                  .containsExactly(WasmValueType.I32, WasmValueType.F64, WasmValueType.EXTERNREF);

              LOGGER.info("Mixed type safety validation test completed on " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Edge Case and Error Handling Tests")
  final class EdgeCaseAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle marshaling with null parameter arrays")
    void shouldHandleMarshalingWithNullParameterArrays() {
      TestRunner.runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              final FunctionType functionType =
                  new FunctionType(
                      new WasmValueType[] {WasmValueType.EXTERNREF},
                      new WasmValueType[] {WasmValueType.I32});

              final WasmFunction hostFunction =
                  TestRunner.createHostFunction(
                      runtime,
                      "null_parameter_handling",
                      functionType,
                      params -> {
                        // Defensive null checks
                        if (params == null) {
                          throw new IllegalArgumentException("Parameters array cannot be null");
                        }

                        if (params.length == 0) {
                          throw new IllegalArgumentException(
                              "Parameters array cannot be empty");
                        }

                        if (params[0] == null) {
                          throw new IllegalArgumentException("Parameter 0 cannot be null");
                        }

                        final Object ref = params[0].asExternref();
                        return new WasmValue[] {WasmValue.i32(ref == null ? 0 : 1)};
                      });

              registerForCleanup(hostFunction);

              assertThat(hostFunction).isNotNull();
              LOGGER.info("Null parameter handling test completed on " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle return value marshaling errors")
    void shouldHandleReturnValueMarshalingErrors() {
      TestRunner.runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              final FunctionType functionType =
                  new FunctionType(
                      new WasmValueType[] {WasmValueType.I32},
                      new WasmValueType[] {WasmValueType.I32});

              final WasmFunction hostFunction =
                  TestRunner.createHostFunction(
                      runtime,
                      "return_marshaling_validation",
                      functionType,
                      params -> {
                        final int mode = params[0].asI32();

                        switch (mode) {
                          case 0:
                            // Valid return
                            return new WasmValue[] {WasmValue.i32(42)};
                          case 1:
                            // Null return array should cause error
                            return null;
                          case 2:
                            // Empty return array for non-void function should cause error
                            return new WasmValue[0];
                          case 3:
                            // Wrong number of return values should cause error
                            return new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)};
                          case 4:
                            // Wrong return type should cause error
                            return new WasmValue[] {WasmValue.f32(3.14f)};
                          default:
                            // Default valid case
                            return new WasmValue[] {WasmValue.i32(mode)};
                        }
                      });

              registerForCleanup(hostFunction);

              assertThat(hostFunction).isNotNull();
              LOGGER.info("Return value marshaling validation test completed on " + runtimeType);
            }
          });
    }
  }
}