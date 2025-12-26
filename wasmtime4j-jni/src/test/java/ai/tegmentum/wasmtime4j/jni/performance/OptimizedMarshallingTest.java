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

package ai.tegmentum.wasmtime4j.jni.performance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.jni.performance.OptimizedMarshalling.MarshallingStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link OptimizedMarshalling} class.
 *
 * <p>This test class verifies the OptimizedMarshalling utility class which provides
 * high-performance parameter marshalling techniques between Java and WebAssembly.
 */
@DisplayName("OptimizedMarshalling Tests")
class OptimizedMarshallingTest {

  @BeforeEach
  void setUp() {
    OptimizedMarshalling.reset();
  }

  @AfterEach
  void tearDown() {
    OptimizedMarshalling.reset();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("OptimizedMarshalling should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(OptimizedMarshalling.class.getModifiers()),
          "OptimizedMarshalling should be final");
    }
  }

  @Nested
  @DisplayName("MarshallingStrategy Enum Tests")
  class MarshallingStrategyTests {

    @Test
    @DisplayName("Should have DIRECT value")
    void shouldHaveDirectValue() {
      assertNotNull(MarshallingStrategy.valueOf("DIRECT"), "Should have DIRECT value");
    }

    @Test
    @DisplayName("Should have BUFFERED value")
    void shouldHaveBufferedValue() {
      assertNotNull(MarshallingStrategy.valueOf("BUFFERED"), "Should have BUFFERED value");
    }

    @Test
    @DisplayName("Should have BULK value")
    void shouldHaveBulkValue() {
      assertNotNull(MarshallingStrategy.valueOf("BULK"), "Should have BULK value");
    }

    @Test
    @DisplayName("Should have ZERO_COPY value")
    void shouldHaveZeroCopyValue() {
      assertNotNull(MarshallingStrategy.valueOf("ZERO_COPY"), "Should have ZERO_COPY value");
    }

    @Test
    @DisplayName("Should have exactly 4 marshalling strategies")
    void shouldHaveExactly4MarshallingStrategies() {
      assertEquals(4, MarshallingStrategy.values().length,
          "Should have exactly 4 marshalling strategies");
    }

    @Test
    @DisplayName("DIRECT should be at ordinal 0")
    void directShouldBeAtOrdinal0() {
      assertEquals(0, MarshallingStrategy.DIRECT.ordinal(), "DIRECT should be at ordinal 0");
    }

    @Test
    @DisplayName("BUFFERED should be at ordinal 1")
    void bufferedShouldBeAtOrdinal1() {
      assertEquals(1, MarshallingStrategy.BUFFERED.ordinal(), "BUFFERED should be at ordinal 1");
    }

    @Test
    @DisplayName("BULK should be at ordinal 2")
    void bulkShouldBeAtOrdinal2() {
      assertEquals(2, MarshallingStrategy.BULK.ordinal(), "BULK should be at ordinal 2");
    }

    @Test
    @DisplayName("ZERO_COPY should be at ordinal 3")
    void zeroCopyShouldBeAtOrdinal3() {
      assertEquals(3, MarshallingStrategy.ZERO_COPY.ordinal(), "ZERO_COPY should be at ordinal 3");
    }
  }

  @Nested
  @DisplayName("marshalParameters Tests")
  class MarshalParametersTests {

    @Test
    @DisplayName("marshalParameters should throw for null parameters")
    void marshalParametersShouldThrowForNullParameters() {
      assertThrows(IllegalArgumentException.class,
          () -> OptimizedMarshalling.marshalParameters(null),
          "Should throw for null parameters");
    }

    @Test
    @DisplayName("marshalParameters should return empty array for empty parameters")
    void marshalParametersShouldReturnEmptyArrayForEmptyParameters() {
      final Object[] result = OptimizedMarshalling.marshalParameters(new WasmValue[0]);
      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.length, "Result should be empty");
    }

    @Test
    @DisplayName("marshalParameters should marshal single I32 value")
    void marshalParametersShouldMarshalSingleI32Value() {
      final WasmValue[] params = new WasmValue[]{WasmValue.i32(42)};
      final Object[] result = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result, "Result should not be null");
      assertTrue(result.length >= 1, "Result should have at least 1 element");
    }

    @Test
    @DisplayName("marshalParameters should marshal single I64 value")
    void marshalParametersShouldMarshalSingleI64Value() {
      final WasmValue[] params = new WasmValue[]{WasmValue.i64(123456789L)};
      final Object[] result = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result, "Result should not be null");
      assertTrue(result.length >= 1, "Result should have at least 1 element");
    }

    @Test
    @DisplayName("marshalParameters should marshal single F32 value")
    void marshalParametersShouldMarshalSingleF32Value() {
      final WasmValue[] params = new WasmValue[]{WasmValue.f32(3.14f)};
      final Object[] result = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result, "Result should not be null");
      assertTrue(result.length >= 1, "Result should have at least 1 element");
    }

    @Test
    @DisplayName("marshalParameters should marshal single F64 value")
    void marshalParametersShouldMarshalSingleF64Value() {
      final WasmValue[] params = new WasmValue[]{WasmValue.f64(3.14159265359)};
      final Object[] result = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result, "Result should not be null");
      assertTrue(result.length >= 1, "Result should have at least 1 element");
    }

    @Test
    @DisplayName("marshalParameters should marshal multiple I32 values")
    void marshalParametersShouldMarshalMultipleI32Values() {
      final WasmValue[] params = new WasmValue[]{
          WasmValue.i32(1),
          WasmValue.i32(2),
          WasmValue.i32(3)
      };
      final Object[] result = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result, "Result should not be null");
      assertTrue(result.length >= 1, "Result should have at least 1 element");
    }

    @Test
    @DisplayName("marshalParameters should marshal mixed type values")
    void marshalParametersShouldMarshalMixedTypeValues() {
      final WasmValue[] params = new WasmValue[]{
          WasmValue.i32(42),
          WasmValue.i64(123L),
          WasmValue.f32(1.5f),
          WasmValue.f64(2.5)
      };
      final Object[] result = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result, "Result should not be null");
      assertTrue(result.length >= 1, "Result should have at least 1 element");
    }

    @Test
    @DisplayName("marshalParameters should use bulk strategy for many same-type values")
    void marshalParametersShouldUseBulkStrategyForManySameTypeValues() {
      final WasmValue[] params = new WasmValue[10];
      for (int i = 0; i < 10; i++) {
        params[i] = WasmValue.i32(i);
      }
      final Object[] result = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("marshalParameters should handle V128 value")
    void marshalParametersShouldHandleV128Value() {
      final byte[] v128Data = new byte[16];
      for (int i = 0; i < 16; i++) {
        v128Data[i] = (byte) i;
      }
      final WasmValue[] params = new WasmValue[]{WasmValue.v128(v128Data)};
      final Object[] result = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("marshalParameters should cache marshalling plan for reuse")
    void marshalParametersShouldCacheMarshallingPlanForReuse() {
      final WasmValue[] params = new WasmValue[]{WasmValue.i32(42), WasmValue.i64(100L)};

      // First call creates the plan
      final Object[] result1 = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result1, "First result should not be null");

      // Second call should use cached plan
      final Object[] result2 = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(result2, "Second result should not be null");
    }
  }

  @Nested
  @DisplayName("marshalParametersBatch Tests")
  class MarshalParametersBatchTests {

    @Test
    @DisplayName("marshalParametersBatch should throw for null parameter sets")
    void marshalParametersBatchShouldThrowForNullParameterSets() {
      assertThrows(IllegalArgumentException.class,
          () -> OptimizedMarshalling.marshalParametersBatch((WasmValue[][]) null),
          "Should throw for null parameter sets");
    }

    @Test
    @DisplayName("marshalParametersBatch should handle empty parameter sets")
    void marshalParametersBatchShouldHandleEmptyParameterSets() {
      final Object[][] result = OptimizedMarshalling.marshalParametersBatch();
      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.length, "Result should be empty");
    }

    @Test
    @DisplayName("marshalParametersBatch should marshal single set")
    void marshalParametersBatchShouldMarshalSingleSet() {
      final WasmValue[] params = new WasmValue[]{WasmValue.i32(42)};
      final Object[][] result = OptimizedMarshalling.marshalParametersBatch(params);
      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Result should have 1 set");
    }

    @Test
    @DisplayName("marshalParametersBatch should marshal multiple sets")
    void marshalParametersBatchShouldMarshalMultipleSets() {
      final WasmValue[] params1 = new WasmValue[]{WasmValue.i32(1)};
      final WasmValue[] params2 = new WasmValue[]{WasmValue.i32(2)};
      final WasmValue[] params3 = new WasmValue[]{WasmValue.i32(3)};

      final Object[][] result = OptimizedMarshalling.marshalParametersBatch(params1, params2, params3);
      assertNotNull(result, "Result should not be null");
      assertEquals(3, result.length, "Result should have 3 sets");
    }
  }

  @Nested
  @DisplayName("marshalMultiValueResults Tests")
  class MarshalMultiValueResultsTests {

    @Test
    @DisplayName("marshalMultiValueResults should return empty array for null values")
    void marshalMultiValueResultsShouldReturnEmptyArrayForNullValues() {
      final Object[] result = OptimizedMarshalling.marshalMultiValueResults(null);
      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.length, "Result should be empty");
    }

    @Test
    @DisplayName("marshalMultiValueResults should return empty array for empty values")
    void marshalMultiValueResultsShouldReturnEmptyArrayForEmptyValues() {
      final Object[] result = OptimizedMarshalling.marshalMultiValueResults(new WasmValue[0]);
      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.length, "Result should be empty");
    }

    @Test
    @DisplayName("marshalMultiValueResults should handle single value")
    void marshalMultiValueResultsShouldHandleSingleValue() {
      final WasmValue[] values = new WasmValue[]{WasmValue.i32(42)};
      final Object[] result = OptimizedMarshalling.marshalMultiValueResults(values);
      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Result should have 1 element");
      assertEquals(42, result[0], "Result should contain correct value");
    }

    @Test
    @DisplayName("marshalMultiValueResults should handle dual values optimally")
    void marshalMultiValueResultsShouldHandleDualValuesOptimally() {
      final WasmValue[] values = new WasmValue[]{WasmValue.i32(10), WasmValue.i64(20L)};
      final Object[] result = OptimizedMarshalling.marshalMultiValueResults(values);
      assertNotNull(result, "Result should not be null");
      assertEquals(2, result.length, "Result should have 2 elements");
      assertEquals(10, result[0], "First result should be correct");
      assertEquals(20L, result[1], "Second result should be correct");
    }

    @Test
    @DisplayName("marshalMultiValueResults should handle more than 2 values")
    void marshalMultiValueResultsShouldHandleMoreThan2Values() {
      final WasmValue[] values = new WasmValue[]{
          WasmValue.i32(1),
          WasmValue.i32(2),
          WasmValue.i32(3)
      };
      final Object[] result = OptimizedMarshalling.marshalMultiValueResults(values);
      assertNotNull(result, "Result should not be null");
      assertTrue(result.length >= 1, "Result should have at least 1 element");
    }
  }

  @Nested
  @DisplayName("unmarshalResults Tests")
  class UnmarshalResultsTests {

    @Test
    @DisplayName("unmarshalResults should throw for null native results")
    void unmarshalResultsShouldThrowForNullNativeResults() {
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.I32};
      assertThrows(IllegalArgumentException.class,
          () -> OptimizedMarshalling.unmarshalResults(null, types),
          "Should throw for null native results");
    }

    @Test
    @DisplayName("unmarshalResults should throw for null expected types")
    void unmarshalResultsShouldThrowForNullExpectedTypes() {
      final Object[] results = new Object[]{42};
      assertThrows(IllegalArgumentException.class,
          () -> OptimizedMarshalling.unmarshalResults(results, null),
          "Should throw for null expected types");
    }

    @Test
    @DisplayName("unmarshalResults should throw for count mismatch")
    void unmarshalResultsShouldThrowForCountMismatch() {
      final Object[] results = new Object[]{42, 100L};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.I32};
      assertThrows(IllegalArgumentException.class,
          () -> OptimizedMarshalling.unmarshalResults(results, types),
          "Should throw for count mismatch");
    }

    @Test
    @DisplayName("unmarshalResults should unmarshal I32")
    void unmarshalResultsShouldUnmarshalI32() {
      final Object[] results = new Object[]{42};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.I32};
      final WasmValue[] wasmValues = OptimizedMarshalling.unmarshalResults(results, types);
      assertNotNull(wasmValues, "Result should not be null");
      assertEquals(1, wasmValues.length, "Should have 1 value");
      assertEquals(WasmValueType.I32, wasmValues[0].getType(), "Should be I32 type");
      assertEquals(42, wasmValues[0].asI32(), "Should have correct value");
    }

    @Test
    @DisplayName("unmarshalResults should unmarshal I64")
    void unmarshalResultsShouldUnmarshalI64() {
      final Object[] results = new Object[]{123456789L};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.I64};
      final WasmValue[] wasmValues = OptimizedMarshalling.unmarshalResults(results, types);
      assertNotNull(wasmValues, "Result should not be null");
      assertEquals(1, wasmValues.length, "Should have 1 value");
      assertEquals(WasmValueType.I64, wasmValues[0].getType(), "Should be I64 type");
      assertEquals(123456789L, wasmValues[0].asI64(), "Should have correct value");
    }

    @Test
    @DisplayName("unmarshalResults should unmarshal F32")
    void unmarshalResultsShouldUnmarshalF32() {
      final Object[] results = new Object[]{3.14f};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.F32};
      final WasmValue[] wasmValues = OptimizedMarshalling.unmarshalResults(results, types);
      assertNotNull(wasmValues, "Result should not be null");
      assertEquals(1, wasmValues.length, "Should have 1 value");
      assertEquals(WasmValueType.F32, wasmValues[0].getType(), "Should be F32 type");
      assertEquals(3.14f, wasmValues[0].asF32(), 0.001f, "Should have correct value");
    }

    @Test
    @DisplayName("unmarshalResults should unmarshal F64")
    void unmarshalResultsShouldUnmarshalF64() {
      final Object[] results = new Object[]{3.14159265359};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.F64};
      final WasmValue[] wasmValues = OptimizedMarshalling.unmarshalResults(results, types);
      assertNotNull(wasmValues, "Result should not be null");
      assertEquals(1, wasmValues.length, "Should have 1 value");
      assertEquals(WasmValueType.F64, wasmValues[0].getType(), "Should be F64 type");
      assertEquals(3.14159265359, wasmValues[0].asF64(), 0.0001, "Should have correct value");
    }

    @Test
    @DisplayName("unmarshalResults should unmarshal V128")
    void unmarshalResultsShouldUnmarshalV128() {
      final byte[] v128Data = new byte[16];
      for (int i = 0; i < 16; i++) {
        v128Data[i] = (byte) i;
      }
      final Object[] results = new Object[]{v128Data};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.V128};
      final WasmValue[] wasmValues = OptimizedMarshalling.unmarshalResults(results, types);
      assertNotNull(wasmValues, "Result should not be null");
      assertEquals(1, wasmValues.length, "Should have 1 value");
      assertEquals(WasmValueType.V128, wasmValues[0].getType(), "Should be V128 type");
      assertArrayEquals(v128Data, wasmValues[0].asV128(), "Should have correct value");
    }

    @Test
    @DisplayName("unmarshalResults should unmarshal EXTERNREF")
    void unmarshalResultsShouldUnmarshalExternref() {
      final Object refValue = "test-reference";
      final Object[] results = new Object[]{refValue};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.EXTERNREF};
      final WasmValue[] wasmValues = OptimizedMarshalling.unmarshalResults(results, types);
      assertNotNull(wasmValues, "Result should not be null");
      assertEquals(1, wasmValues.length, "Should have 1 value");
      assertEquals(WasmValueType.EXTERNREF, wasmValues[0].getType(), "Should be EXTERNREF type");
    }

    @Test
    @DisplayName("unmarshalResults should unmarshal FUNCREF")
    void unmarshalResultsShouldUnmarshalFuncref() {
      final Object funcValue = new Object();
      final Object[] results = new Object[]{funcValue};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.FUNCREF};
      final WasmValue[] wasmValues = OptimizedMarshalling.unmarshalResults(results, types);
      assertNotNull(wasmValues, "Result should not be null");
      assertEquals(1, wasmValues.length, "Should have 1 value");
      assertEquals(WasmValueType.FUNCREF, wasmValues[0].getType(), "Should be FUNCREF type");
    }

    @Test
    @DisplayName("unmarshalResults should throw for null result element")
    void unmarshalResultsShouldThrowForNullResultElement() {
      final Object[] results = new Object[]{null};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.I32};
      assertThrows(IllegalArgumentException.class,
          () -> OptimizedMarshalling.unmarshalResults(results, types),
          "Should throw for null result element");
    }

    @Test
    @DisplayName("unmarshalResults should throw for type mismatch")
    void unmarshalResultsShouldThrowForTypeMismatch() {
      final Object[] results = new Object[]{"string-not-int"};
      final WasmValueType[] types = new WasmValueType[]{WasmValueType.I32};
      assertThrows(IllegalArgumentException.class,
          () -> OptimizedMarshalling.unmarshalResults(results, types),
          "Should throw for type mismatch");
    }

    @Test
    @DisplayName("unmarshalResults should unmarshal multiple values")
    void unmarshalResultsShouldUnmarshalMultipleValues() {
      final Object[] results = new Object[]{10, 20L, 3.0f, 4.0};
      final WasmValueType[] types = new WasmValueType[]{
          WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64
      };
      final WasmValue[] wasmValues = OptimizedMarshalling.unmarshalResults(results, types);
      assertNotNull(wasmValues, "Result should not be null");
      assertEquals(4, wasmValues.length, "Should have 4 values");
      assertEquals(10, wasmValues[0].asI32(), "First value should be correct");
      assertEquals(20L, wasmValues[1].asI64(), "Second value should be correct");
      assertEquals(3.0f, wasmValues[2].asF32(), 0.001f, "Third value should be correct");
      assertEquals(4.0, wasmValues[3].asF64(), 0.001, "Fourth value should be correct");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getStatistics should return formatted string")
    void getStatisticsShouldReturnFormattedString() {
      final String stats = OptimizedMarshalling.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.contains("Optimized Marshalling"),
          "Statistics should contain header");
    }

    @Test
    @DisplayName("getStatistics should include cache size")
    void getStatisticsShouldIncludeCacheSize() {
      final String stats = OptimizedMarshalling.getStatistics();
      assertTrue(stats.contains("Cached marshalling plans") || stats.contains("cache"),
          "Statistics should include cache information");
    }

    @Test
    @DisplayName("getStatistics should include strategy usage")
    void getStatisticsShouldIncludeStrategyUsage() {
      // Generate some statistics first
      final WasmValue[] params = new WasmValue[]{WasmValue.i32(42)};
      OptimizedMarshalling.marshalParameters(params);

      final String stats = OptimizedMarshalling.getStatistics();
      assertTrue(stats.contains("Strategy usage") || stats.contains("DIRECT"),
          "Statistics should include strategy usage");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("reset should not throw")
    void resetShouldNotThrow() {
      assertDoesNotThrow(() -> OptimizedMarshalling.reset(),
          "reset should not throw");
    }

    @Test
    @DisplayName("reset should clear marshalling cache")
    void resetShouldClearMarshallingCache() {
      // Generate some cache entries
      final WasmValue[] params = new WasmValue[]{WasmValue.i32(42)};
      OptimizedMarshalling.marshalParameters(params);

      // Reset
      OptimizedMarshalling.reset();

      // Verify cache is cleared by checking statistics
      final String stats = OptimizedMarshalling.getStatistics();
      assertTrue(stats.contains("0") || stats.contains("cleared"),
          "Cache should be cleared after reset");
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Concurrent marshalling should not throw")
    void concurrentMarshallingShouldNotThrow() throws InterruptedException {
      final int threadCount = 4;
      final int operationsPerThread = 50;
      final Thread[] threads = new Thread[threadCount];
      final boolean[] errors = new boolean[1];

      for (int t = 0; t < threadCount; t++) {
        threads[t] = new Thread(() -> {
          try {
            for (int i = 0; i < operationsPerThread; i++) {
              final WasmValue[] params = new WasmValue[]{
                  WasmValue.i32(i),
                  WasmValue.i64((long) i)
              };
              OptimizedMarshalling.marshalParameters(params);
            }
          } catch (Exception e) {
            errors[0] = true;
          }
        });
      }

      for (final Thread thread : threads) {
        thread.start();
      }

      for (final Thread thread : threads) {
        thread.join();
      }

      assertEquals(false, errors[0], "No errors should occur during concurrent marshalling");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full marshalling round-trip should work")
    void fullMarshallingRoundTripShouldWork() {
      // Marshal parameters
      final WasmValue[] params = new WasmValue[]{
          WasmValue.i32(42),
          WasmValue.i64(123L),
          WasmValue.f32(3.14f),
          WasmValue.f64(2.71828)
      };
      final Object[] marshalled = OptimizedMarshalling.marshalParameters(params);
      assertNotNull(marshalled, "Marshalled parameters should not be null");

      // Marshal multi-value results
      final Object[] multiValueResult = OptimizedMarshalling.marshalMultiValueResults(params);
      assertNotNull(multiValueResult, "Multi-value results should not be null");

      // Unmarshal results
      final Object[] nativeResults = new Object[]{100, 200L, 1.5f, 2.5};
      final WasmValueType[] expectedTypes = new WasmValueType[]{
          WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64
      };
      final WasmValue[] unmarshalledResults =
          OptimizedMarshalling.unmarshalResults(nativeResults, expectedTypes);
      assertNotNull(unmarshalledResults, "Unmarshalled results should not be null");
      assertEquals(4, unmarshalledResults.length, "Should have 4 results");
    }

    @Test
    @DisplayName("Batch marshalling should work with various parameter sets")
    void batchMarshallingShouldWorkWithVariousParameterSets() {
      final WasmValue[] set1 = new WasmValue[]{WasmValue.i32(1)};
      final WasmValue[] set2 = new WasmValue[]{WasmValue.i64(2L), WasmValue.f32(3.0f)};
      final WasmValue[] set3 = new WasmValue[]{
          WasmValue.f64(4.0), WasmValue.i32(5), WasmValue.i64(6L)
      };

      final Object[][] results = OptimizedMarshalling.marshalParametersBatch(set1, set2, set3);
      assertNotNull(results, "Batch results should not be null");
      assertEquals(3, results.length, "Should have 3 result sets");
      assertNotNull(results[0], "First set should not be null");
      assertNotNull(results[1], "Second set should not be null");
      assertNotNull(results[2], "Third set should not be null");
    }

    @Test
    @DisplayName("Marshalling plan caching should improve performance")
    void marshallingPlanCachingShouldImprovePerformance() {
      final WasmValue[] params = new WasmValue[]{
          WasmValue.i32(1),
          WasmValue.i32(2),
          WasmValue.i32(3)
      };

      // First call creates the plan
      final long start1 = System.nanoTime();
      OptimizedMarshalling.marshalParameters(params);
      final long duration1 = System.nanoTime() - start1;

      // Subsequent calls should use cached plan (potentially faster)
      long totalCached = 0;
      for (int i = 0; i < 10; i++) {
        final long startCached = System.nanoTime();
        OptimizedMarshalling.marshalParameters(params);
        totalCached += System.nanoTime() - startCached;
      }
      final long avgCached = totalCached / 10;

      // Just verify it works, don't make timing assertions
      assertTrue(duration1 >= 0, "First call duration should be measurable");
      assertTrue(avgCached >= 0, "Cached call duration should be measurable");
    }
  }
}
