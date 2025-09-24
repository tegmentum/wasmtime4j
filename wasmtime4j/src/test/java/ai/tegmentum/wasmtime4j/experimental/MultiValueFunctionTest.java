/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for multi-value function experimental feature. */
final class MultiValueFunctionTest {

  @BeforeEach
  void setUp() {
    // Enable multi-value feature for tests
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.MULTI_VALUE);
  }

  @AfterEach
  void tearDown() {
    // Clean up experimental features
    ExperimentalFeatures.reset();
  }

  @Test
  void testMultiValueSignatureCreation() {
    final List<WasmValueType> parameterTypes = Arrays.asList(WasmValueType.I32, WasmValueType.F64);
    final List<WasmValueType> returnTypes =
        Arrays.asList(WasmValueType.I32, WasmValueType.F64, WasmValueType.I32);

    final MultiValueFunction.MultiValueSignature signature =
        new MultiValueFunction.MultiValueSignature("test_function", parameterTypes, returnTypes);

    assertEquals("test_function", signature.getName());
    assertEquals(parameterTypes, signature.getParameterTypes());
    assertEquals(returnTypes, signature.getReturnTypes());
    assertTrue(signature.isMultiValue());
    assertEquals(3, signature.getReturnCount());
  }

  @Test
  void testSingleValueSignature() {
    final List<WasmValueType> parameterTypes = Collections.singletonList(WasmValueType.I32);
    final List<WasmValueType> returnTypes = Collections.singletonList(WasmValueType.F64);

    final MultiValueFunction.MultiValueSignature signature =
        new MultiValueFunction.MultiValueSignature("single_value", parameterTypes, returnTypes);

    assertFalse(signature.isMultiValue());
    assertEquals(1, signature.getReturnCount());
  }

  @Test
  void testMultiValueSignatureWithNullName() {
    final List<WasmValueType> types = Collections.singletonList(WasmValueType.I32);

    assertThrows(
        IllegalArgumentException.class,
        () -> new MultiValueFunction.MultiValueSignature(null, types, types));
  }

  @Test
  void testMultiValueSignatureWithEmptyName() {
    final List<WasmValueType> types = Collections.singletonList(WasmValueType.I32);

    assertThrows(
        IllegalArgumentException.class,
        () -> new MultiValueFunction.MultiValueSignature("", types, types));
  }

  @Test
  void testMultiValueSignatureWithNullTypes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MultiValueFunction.MultiValueSignature("test", null, Collections.emptyList()));

    assertThrows(
        IllegalArgumentException.class,
        () -> new MultiValueFunction.MultiValueSignature("test", Collections.emptyList(), null));
  }

  @Test
  void testMultiValueSignatureEquality() {
    final List<WasmValueType> params = Collections.singletonList(WasmValueType.I32);
    final List<WasmValueType> returns = Collections.singletonList(WasmValueType.F64);

    final MultiValueFunction.MultiValueSignature sig1 =
        new MultiValueFunction.MultiValueSignature("test", params, returns);
    final MultiValueFunction.MultiValueSignature sig2 =
        new MultiValueFunction.MultiValueSignature("test", params, returns);
    final MultiValueFunction.MultiValueSignature sig3 =
        new MultiValueFunction.MultiValueSignature("other", params, returns);

    assertEquals(sig1, sig2);
    assertNotEquals(sig1, sig3);
    assertEquals(sig1.hashCode(), sig2.hashCode());
  }

  @Test
  void testMultiValueResultCreation() {
    final List<WasmValue> values =
        Arrays.asList(WasmValue.i32(42), WasmValue.f64(3.14), WasmValue.i32(100));

    final MultiValueFunction.MultiValueResult result =
        new MultiValueFunction.MultiValueResult(values);

    assertEquals(values, result.getValues());
    assertEquals(3, result.getValueCount());
    assertTrue(result.hasMultipleValues());

    assertEquals(WasmValue.i32(42), result.getValue(0));
    assertEquals(WasmValue.f64(3.14), result.getValue(1));
    assertEquals(WasmValue.i32(100), result.getValue(2));

    assertEquals(WasmValue.i32(42), result.getFirstValue());
  }

  @Test
  void testMultiValueResultWithNullValues() {
    assertThrows(
        IllegalArgumentException.class, () -> new MultiValueFunction.MultiValueResult(null));
  }

  @Test
  void testMultiValueResultWithEmptyValues() {
    final MultiValueFunction.MultiValueResult result =
        new MultiValueFunction.MultiValueResult(Collections.emptyList());

    assertEquals(0, result.getValueCount());
    assertFalse(result.hasMultipleValues());
    assertThrows(IndexOutOfBoundsException.class, () -> result.getValue(0));
    assertThrows(RuntimeException.class, result::getFirstValue);
  }

  @Test
  void testMultiValueResultEquality() {
    final List<WasmValue> values = Collections.singletonList(WasmValue.i32(42));

    final MultiValueFunction.MultiValueResult result1 =
        new MultiValueFunction.MultiValueResult(values);
    final MultiValueFunction.MultiValueResult result2 =
        new MultiValueFunction.MultiValueResult(values);
    final MultiValueFunction.MultiValueResult result3 =
        new MultiValueFunction.MultiValueResult(Collections.singletonList(WasmValue.i32(100)));

    assertEquals(result1, result2);
    assertNotEquals(result1, result3);
    assertEquals(result1.hashCode(), result2.hashCode());
  }

  @Test
  void testMultiValueFunctionCreation() {
    final MultiValueFunction.MultiValueConfig config =
        MultiValueFunction.MultiValueConfig.builder()
            .validateReturnTypes(true)
            .enableParameterValidation(true)
            .maxReturnValues(8)
            .allowEmptyReturns(false)
            .build();

    final MultiValueFunction handler = new MultiValueFunction(config);

    assertNotNull(handler);
    assertEquals(config, handler.getConfig());
    assertTrue(handler.getNativeHandle() > 0);
  }

  @Test
  void testMultiValueFunctionWithNullConfig() {
    assertThrows(IllegalArgumentException.class, () -> new MultiValueFunction(null));
  }

  @Test
  void testMultiValueFunctionWithFeatureDisabled() {
    ExperimentalFeatures.disableFeature(ExperimentalFeatures.Feature.MULTI_VALUE);

    final MultiValueFunction.MultiValueConfig config =
        MultiValueFunction.MultiValueConfig.builder().build();

    assertThrows(UnsupportedOperationException.class, () -> new MultiValueFunction(config));
  }

  @Test
  void testMultiValueConfigBuilder() {
    final MultiValueFunction.MultiValueConfig config =
        MultiValueFunction.MultiValueConfig.builder()
            .validateReturnTypes(false)
            .enableParameterValidation(false)
            .maxReturnValues(32)
            .allowEmptyReturns(true)
            .build();

    assertFalse(config.isReturnTypeValidationEnabled());
    assertFalse(config.isParameterValidationEnabled());
    assertEquals(32, config.getMaxReturnValues());
    assertTrue(config.isEmptyReturnsAllowed());
  }

  @Test
  void testMultiValueConfigBuilderWithInvalidMaxReturnValues() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MultiValueFunction.MultiValueConfig.builder().maxReturnValues(0));

    assertThrows(
        IllegalArgumentException.class,
        () -> MultiValueFunction.MultiValueConfig.builder().maxReturnValues(-1));
  }

  @Test
  void testHostFunctionInterface() {
    final MultiValueFunction.MultiValueHostFunction hostFunc =
        parameters -> {
          if (parameters.size() != 2) {
            throw new RuntimeException("Expected 2 parameters");
          }

          final WasmValue param1 = parameters.get(0);
          final WasmValue param2 = parameters.get(1);

          if (param1.getType() != WasmValueType.I32 || param2.getType() != WasmValueType.I32) {
            throw new RuntimeException("Expected I32 parameters");
          }

          final int sum = param1.asI32() + param2.asI32();
          final int product = param1.asI32() * param2.asI32();

          return new MultiValueFunction.MultiValueResult(
              Arrays.asList(WasmValue.i32(sum), WasmValue.i32(product)));
        };

    final List<WasmValue> params = Arrays.asList(WasmValue.i32(5), WasmValue.i32(3));
    final MultiValueFunction.MultiValueResult result = hostFunc.invoke(params);

    assertEquals(2, result.getValueCount());
    assertEquals(WasmValue.i32(8), result.getValue(0)); // 5 + 3
    assertEquals(WasmValue.i32(15), result.getValue(1)); // 5 * 3
  }

  @Test
  void testCreateHostFunction() {
    final MultiValueFunction.MultiValueConfig config =
        MultiValueFunction.MultiValueConfig.builder().build();
    final MultiValueFunction handler = new MultiValueFunction(config);

    final List<WasmValueType> paramTypes = Arrays.asList(WasmValueType.I32, WasmValueType.I32);
    final List<WasmValueType> returnTypes = Arrays.asList(WasmValueType.I32, WasmValueType.I32);

    final MultiValueFunction.MultiValueSignature signature =
        new MultiValueFunction.MultiValueSignature("add_and_multiply", paramTypes, returnTypes);

    final MultiValueFunction.MultiValueHostFunction implementation =
        parameters -> {
          final int a = parameters.get(0).asI32();
          final int b = parameters.get(1).asI32();
          return new MultiValueFunction.MultiValueResult(
              Arrays.asList(WasmValue.i32(a + b), WasmValue.i32(a * b)));
        };

    final long functionHandle = handler.createHostFunction(signature, implementation);
    assertTrue(functionHandle > 0);
  }

  @Test
  void testCreateHostFunctionWithNullSignature() {
    final MultiValueFunction.MultiValueConfig config =
        MultiValueFunction.MultiValueConfig.builder().build();
    final MultiValueFunction handler = new MultiValueFunction(config);

    final MultiValueFunction.MultiValueHostFunction implementation =
        params -> new MultiValueFunction.MultiValueResult(Collections.emptyList());

    assertThrows(
        IllegalArgumentException.class, () -> handler.createHostFunction(null, implementation));
  }

  @Test
  void testCreateHostFunctionWithNullImplementation() {
    final MultiValueFunction.MultiValueConfig config =
        MultiValueFunction.MultiValueConfig.builder().build();
    final MultiValueFunction handler = new MultiValueFunction(config);

    final MultiValueFunction.MultiValueSignature signature =
        new MultiValueFunction.MultiValueSignature(
            "test", Collections.emptyList(), Collections.emptyList());

    assertThrows(IllegalArgumentException.class, () -> handler.createHostFunction(signature, null));
  }

  @Test
  void testCallFunctionWithValidation() {
    final MultiValueFunction.MultiValueConfig config =
        MultiValueFunction.MultiValueConfig.builder()
            .enableParameterValidation(true)
            .validateReturnTypes(true)
            .build();
    final MultiValueFunction handler = new MultiValueFunction(config);

    final List<WasmValueType> paramTypes = Arrays.asList(WasmValueType.I32, WasmValueType.F64);
    final List<WasmValueType> returnTypes = Collections.singletonList(WasmValueType.I32);

    final MultiValueFunction.MultiValueSignature signature =
        new MultiValueFunction.MultiValueSignature("test_func", paramTypes, returnTypes);

    // Note: This test would require an actual WebAssembly function handle
    // For now, we test parameter validation by checking for null values

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.callFunction(0L, null, Collections.emptyList()));

    assertThrows(IllegalArgumentException.class, () -> handler.callFunction(0L, signature, null));
  }

  @Test
  void testMultiValueFunctionResourceManagement() {
    final MultiValueFunction.MultiValueConfig config =
        MultiValueFunction.MultiValueConfig.builder().build();
    final MultiValueFunction handler = new MultiValueFunction(config);

    assertTrue(handler.getNativeHandle() > 0);

    // Test that close can be called multiple times safely
    assertDoesNotThrow(handler::close);
    assertDoesNotThrow(handler::close);
  }

  @Test
  void testMultiValueSignatureToString() {
    final List<WasmValueType> paramTypes = Collections.singletonList(WasmValueType.I32);
    final List<WasmValueType> returnTypes = Arrays.asList(WasmValueType.I32, WasmValueType.F64);

    final MultiValueFunction.MultiValueSignature signature =
        new MultiValueFunction.MultiValueSignature("test_func", paramTypes, returnTypes);

    final String result = signature.toString();

    assertTrue(result.contains("test_func"));
    assertTrue(result.contains("I32"));
    assertTrue(result.contains("F64"));
  }

  @Test
  void testMultiValueResultToString() {
    final List<WasmValue> values = Arrays.asList(WasmValue.i32(42), WasmValue.f64(3.14));
    final MultiValueFunction.MultiValueResult result =
        new MultiValueFunction.MultiValueResult(values);

    final String resultString = result.toString();

    assertTrue(resultString.contains("MultiValueResult"));
    assertTrue(resultString.contains("values="));
  }
}
