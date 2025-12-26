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

package ai.tegmentum.wasmtime4j.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the WASI NN (Neural Network) package interfaces and enums.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI NN API
 * using reflection-based testing.
 */
@DisplayName("WASI NN Package Tests")
class WasiNnPackageTest {

  // ========================================================================
  // NnTensorType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("NnTensorType Enum Tests")
  class NnTensorTypeTests {

    @Test
    @DisplayName("NnTensorType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnTensorType.class.isEnum(), "NnTensorType should be an enum");
    }

    @Test
    @DisplayName("NnTensorType should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(NnTensorType.class.getModifiers()), "NnTensorType should be public");
    }

    @Test
    @DisplayName("NnTensorType should have exactly 7 values")
    void shouldHaveExactValueCount() {
      NnTensorType[] values = NnTensorType.values();
      assertEquals(7, values.length, "NnTensorType should have exactly 7 values");
    }

    @Test
    @DisplayName("NnTensorType should have FP16 value with 2 bytes")
    void shouldHaveFp16Value() {
      NnTensorType fp16 = NnTensorType.valueOf("FP16");
      assertNotNull(fp16, "FP16 value should exist");
      assertEquals(2, fp16.getByteSize(), "FP16 should be 2 bytes");
      assertEquals("fp16", fp16.getWasiName(), "FP16 WASI name should be fp16");
    }

    @Test
    @DisplayName("NnTensorType should have FP32 value with 4 bytes")
    void shouldHaveFp32Value() {
      NnTensorType fp32 = NnTensorType.valueOf("FP32");
      assertNotNull(fp32, "FP32 value should exist");
      assertEquals(4, fp32.getByteSize(), "FP32 should be 4 bytes");
      assertEquals("fp32", fp32.getWasiName(), "FP32 WASI name should be fp32");
    }

    @Test
    @DisplayName("NnTensorType should have FP64 value with 8 bytes")
    void shouldHaveFp64Value() {
      NnTensorType fp64 = NnTensorType.valueOf("FP64");
      assertNotNull(fp64, "FP64 value should exist");
      assertEquals(8, fp64.getByteSize(), "FP64 should be 8 bytes");
      assertEquals("fp64", fp64.getWasiName(), "FP64 WASI name should be fp64");
    }

    @Test
    @DisplayName("NnTensorType should have BF16 value with 2 bytes")
    void shouldHaveBf16Value() {
      NnTensorType bf16 = NnTensorType.valueOf("BF16");
      assertNotNull(bf16, "BF16 value should exist");
      assertEquals(2, bf16.getByteSize(), "BF16 should be 2 bytes");
      assertEquals("bf16", bf16.getWasiName(), "BF16 WASI name should be bf16");
    }

    @Test
    @DisplayName("NnTensorType should have U8 value with 1 byte")
    void shouldHaveU8Value() {
      NnTensorType u8 = NnTensorType.valueOf("U8");
      assertNotNull(u8, "U8 value should exist");
      assertEquals(1, u8.getByteSize(), "U8 should be 1 byte");
      assertEquals("u8", u8.getWasiName(), "U8 WASI name should be u8");
    }

    @Test
    @DisplayName("NnTensorType should have I32 value with 4 bytes")
    void shouldHaveI32Value() {
      NnTensorType i32 = NnTensorType.valueOf("I32");
      assertNotNull(i32, "I32 value should exist");
      assertEquals(4, i32.getByteSize(), "I32 should be 4 bytes");
      assertEquals("i32", i32.getWasiName(), "I32 WASI name should be i32");
    }

    @Test
    @DisplayName("NnTensorType should have I64 value with 8 bytes")
    void shouldHaveI64Value() {
      NnTensorType i64 = NnTensorType.valueOf("I64");
      assertNotNull(i64, "I64 value should exist");
      assertEquals(8, i64.getByteSize(), "I64 should be 8 bytes");
      assertEquals("i64", i64.getWasiName(), "I64 WASI name should be i64");
    }

    @Test
    @DisplayName("NnTensorType should have getByteSize method")
    void shouldHaveGetByteSizeMethod() throws NoSuchMethodException {
      Method method = NnTensorType.class.getMethod("getByteSize");
      assertNotNull(method, "getByteSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("NnTensorType should have getWasiName method")
    void shouldHaveGetWasiNameMethod() throws NoSuchMethodException {
      Method method = NnTensorType.class.getMethod("getWasiName");
      assertNotNull(method, "getWasiName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("NnTensorType should have calculateByteSize method")
    void shouldHaveCalculateByteSizeMethod() throws NoSuchMethodException {
      Method method = NnTensorType.class.getMethod("calculateByteSize", int[].class);
      assertNotNull(method, "calculateByteSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("calculateByteSize should compute correct size for dimensions")
    void calculateByteSizeShouldComputeCorrectSize() {
      // 2x3x4 tensor of FP32 (4 bytes each) = 2*3*4*4 = 96 bytes
      long size = NnTensorType.FP32.calculateByteSize(new int[] {2, 3, 4});
      assertEquals(96, size, "calculateByteSize should return 96 for [2,3,4] FP32 tensor");
    }

    @Test
    @DisplayName("calculateByteSize should return 0 for empty dimensions")
    void calculateByteSizeShouldReturnZeroForEmptyDimensions() {
      long size = NnTensorType.FP32.calculateByteSize(new int[] {});
      assertEquals(0, size, "calculateByteSize should return 0 for empty dimensions");
    }

    @Test
    @DisplayName("calculateByteSize should throw for negative dimensions")
    void calculateByteSizeShouldThrowForNegativeDimensions() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.FP32.calculateByteSize(new int[] {2, -1, 3}),
          "Should throw for negative dimensions");
    }

    @Test
    @DisplayName("NnTensorType should have fromWasiName static method")
    void shouldHaveFromWasiNameMethod() throws NoSuchMethodException {
      Method method = NnTensorType.class.getMethod("fromWasiName", String.class);
      assertNotNull(method, "fromWasiName method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromWasiName should be static");
      assertEquals(
          NnTensorType.class, method.getReturnType(), "Return type should be NnTensorType");
    }

    @Test
    @DisplayName("fromWasiName should return correct type")
    void fromWasiNameShouldReturnCorrectType() {
      assertEquals(NnTensorType.FP32, NnTensorType.fromWasiName("fp32"));
      assertEquals(NnTensorType.U8, NnTensorType.fromWasiName("u8"));
      assertEquals(NnTensorType.BF16, NnTensorType.fromWasiName("bf16"));
    }

    @Test
    @DisplayName("fromWasiName should throw for unknown type")
    void fromWasiNameShouldThrowForUnknownType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.fromWasiName("unknown"),
          "Should throw for unknown WASI name");
    }

    @Test
    @DisplayName("NnTensorType should have getNativeCode method")
    void shouldHaveGetNativeCodeMethod() throws NoSuchMethodException {
      Method method = NnTensorType.class.getMethod("getNativeCode");
      assertNotNull(method, "getNativeCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("NnTensorType should have fromNativeCode static method")
    void shouldHaveFromNativeCodeMethod() throws NoSuchMethodException {
      Method method = NnTensorType.class.getMethod("fromNativeCode", int.class);
      assertNotNull(method, "fromNativeCode method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromNativeCode should be static");
    }

    @Test
    @DisplayName("getNativeCode and fromNativeCode should be inverses")
    void nativeCodeMethodsShouldBeInverses() {
      for (NnTensorType type : NnTensorType.values()) {
        int code = type.getNativeCode();
        NnTensorType recovered = NnTensorType.fromNativeCode(code);
        assertEquals(type, recovered, "fromNativeCode should recover original type");
      }
    }
  }

  // ========================================================================
  // NnGraphEncoding Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("NnGraphEncoding Enum Tests")
  class NnGraphEncodingTests {

    @Test
    @DisplayName("NnGraphEncoding should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnGraphEncoding.class.isEnum(), "NnGraphEncoding should be an enum");
    }

    @Test
    @DisplayName("NnGraphEncoding should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(NnGraphEncoding.class.getModifiers()),
          "NnGraphEncoding should be public");
    }

    @Test
    @DisplayName("NnGraphEncoding should have ONNX value")
    void shouldHaveOnnxValue() {
      NnGraphEncoding[] values = NnGraphEncoding.values();
      boolean hasOnnx = false;
      for (NnGraphEncoding encoding : values) {
        if (encoding.name().equals("ONNX")) {
          hasOnnx = true;
          break;
        }
      }
      assertTrue(hasOnnx, "Should have ONNX encoding");
    }
  }

  // ========================================================================
  // NnExecutionTarget Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("NnExecutionTarget Enum Tests")
  class NnExecutionTargetTests {

    @Test
    @DisplayName("NnExecutionTarget should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnExecutionTarget.class.isEnum(), "NnExecutionTarget should be an enum");
    }

    @Test
    @DisplayName("NnExecutionTarget should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(NnExecutionTarget.class.getModifiers()),
          "NnExecutionTarget should be public");
    }

    @Test
    @DisplayName("NnExecutionTarget should have CPU value")
    void shouldHaveCpuValue() {
      NnExecutionTarget[] values = NnExecutionTarget.values();
      boolean hasCpu = false;
      for (NnExecutionTarget target : values) {
        if (target.name().equals("CPU")) {
          hasCpu = true;
          break;
        }
      }
      assertTrue(hasCpu, "Should have CPU execution target");
    }
  }

  // ========================================================================
  // NnErrorCode Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("NnErrorCode Enum Tests")
  class NnErrorCodeTests {

    @Test
    @DisplayName("NnErrorCode should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnErrorCode.class.isEnum(), "NnErrorCode should be an enum");
    }

    @Test
    @DisplayName("NnErrorCode should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(NnErrorCode.class.getModifiers()), "NnErrorCode should be public");
    }

    @Test
    @DisplayName("NnErrorCode should have error values")
    void shouldHaveErrorValues() {
      NnErrorCode[] values = NnErrorCode.values();
      assertTrue(values.length >= 1, "NnErrorCode should have at least 1 value");
    }
  }

  // ========================================================================
  // NnException Class Tests
  // ========================================================================

  @Nested
  @DisplayName("NnException Class Tests")
  class NnExceptionTests {

    @Test
    @DisplayName("NnException should be a class")
    void shouldBeAClass() {
      assertTrue(
          !NnException.class.isInterface() && !NnException.class.isEnum(),
          "NnException should be a class");
    }

    @Test
    @DisplayName("NnException should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(NnException.class.getModifiers()), "NnException should be public");
    }

    @Test
    @DisplayName("NnException should extend Exception")
    void shouldExtendException() {
      assertTrue(
          Exception.class.isAssignableFrom(NnException.class),
          "NnException should extend Exception");
    }
  }

  // ========================================================================
  // NnContext Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("NnContext Interface Tests")
  class NnContextTests {

    @Test
    @DisplayName("NnContext should be an interface")
    void shouldBeAnInterface() {
      assertTrue(NnContext.class.isInterface(), "NnContext should be an interface");
    }

    @Test
    @DisplayName("NnContext should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(NnContext.class.getModifiers()), "NnContext should be public");
    }

    @Test
    @DisplayName("All NnContext methods should be public")
    void allMethodsShouldBePublic() {
      Method[] methods = NnContext.class.getDeclaredMethods();
      for (Method method : methods) {
        if (!Modifier.isStatic(method.getModifiers())) {
          assertTrue(
              Modifier.isPublic(method.getModifiers()),
              "Method " + method.getName() + " should be public");
        }
      }
    }
  }

  // ========================================================================
  // NnGraph Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("NnGraph Interface Tests")
  class NnGraphTests {

    @Test
    @DisplayName("NnGraph should be an interface")
    void shouldBeAnInterface() {
      assertTrue(NnGraph.class.isInterface(), "NnGraph should be an interface");
    }

    @Test
    @DisplayName("NnGraph should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(NnGraph.class.getModifiers()), "NnGraph should be public");
    }
  }

  // ========================================================================
  // NnGraphExecutionContext Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("NnGraphExecutionContext Interface Tests")
  class NnGraphExecutionContextTests {

    @Test
    @DisplayName("NnGraphExecutionContext should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          NnGraphExecutionContext.class.isInterface(),
          "NnGraphExecutionContext should be an interface");
    }

    @Test
    @DisplayName("NnGraphExecutionContext should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(NnGraphExecutionContext.class.getModifiers()),
          "NnGraphExecutionContext should be public");
    }
  }

  // ========================================================================
  // NnTensor Class Tests
  // ========================================================================

  @Nested
  @DisplayName("NnTensor Class Tests")
  class NnTensorTests {

    @Test
    @DisplayName("NnTensor should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(NnTensor.class.getModifiers()), "NnTensor should be a final class");
      assertFalse(NnTensor.class.isInterface(), "NnTensor should not be an interface");
    }

    @Test
    @DisplayName("NnTensor should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(NnTensor.class.getModifiers()), "NnTensor should be public");
    }

    @Test
    @DisplayName("NnTensor should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = NnTensor.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(
          NnTensorType.class, method.getReturnType(), "Return type should be NnTensorType");
    }

    @Test
    @DisplayName("NnTensor should have factory methods")
    void shouldHaveFactoryMethods() throws NoSuchMethodException {
      Method fromFloatArray =
          NnTensor.class.getMethod("fromFloatArray", int[].class, float[].class);
      assertNotNull(fromFloatArray, "fromFloatArray method should exist");
      assertTrue(
          Modifier.isStatic(fromFloatArray.getModifiers()), "fromFloatArray should be static");
      assertEquals(
          NnTensor.class, fromFloatArray.getReturnType(), "Return type should be NnTensor");
    }
  }

  // ========================================================================
  // NnContextFactory Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("NnContextFactory Interface Tests")
  class NnContextFactoryTests {

    @Test
    @DisplayName("NnContextFactory should be an interface")
    void shouldBeAnInterface() {
      assertTrue(NnContextFactory.class.isInterface(), "NnContextFactory should be an interface");
    }

    @Test
    @DisplayName("NnContextFactory should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(NnContextFactory.class.getModifiers()),
          "NnContextFactory should be public");
    }

    @Test
    @DisplayName("NnContextFactory should have createNnContext method")
    void shouldHaveCreateNnContextMethod() throws NoSuchMethodException {
      Method method = NnContextFactory.class.getMethod("createNnContext");
      assertNotNull(method, "createNnContext method should exist");
      assertEquals(NnContext.class, method.getReturnType(), "Return type should be NnContext");
    }

    @Test
    @DisplayName("NnContextFactory should have isNnAvailable method")
    void shouldHaveIsNnAvailableMethod() throws NoSuchMethodException {
      Method method = NnContextFactory.class.getMethod("isNnAvailable");
      assertNotNull(method, "isNnAvailable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // Package Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All WASI NN classes should be loadable")
    void allClassesShouldBeLoadable() {
      assertNotNull(NnTensorType.class, "NnTensorType should be loadable");
      assertNotNull(NnGraphEncoding.class, "NnGraphEncoding should be loadable");
      assertNotNull(NnExecutionTarget.class, "NnExecutionTarget should be loadable");
      assertNotNull(NnErrorCode.class, "NnErrorCode should be loadable");
      assertNotNull(NnException.class, "NnException should be loadable");
      assertNotNull(NnContext.class, "NnContext should be loadable");
      assertNotNull(NnGraph.class, "NnGraph should be loadable");
      assertNotNull(NnGraphExecutionContext.class, "NnGraphExecutionContext should be loadable");
      assertNotNull(NnTensor.class, "NnTensor should be loadable");
      assertNotNull(NnContextFactory.class, "NnContextFactory should be loadable");
    }

    @Test
    @DisplayName("Package should have 10 public types (excluding package-info)")
    void shouldHaveExpectedTypeCount() {
      // Count all expected public types
      int typeCount = 0;
      if (Modifier.isPublic(NnTensorType.class.getModifiers())) {
        typeCount++;
      }
      if (Modifier.isPublic(NnGraphEncoding.class.getModifiers())) {
        typeCount++;
      }
      if (Modifier.isPublic(NnExecutionTarget.class.getModifiers())) {
        typeCount++;
      }
      if (Modifier.isPublic(NnErrorCode.class.getModifiers())) {
        typeCount++;
      }
      if (Modifier.isPublic(NnException.class.getModifiers())) {
        typeCount++;
      }
      if (Modifier.isPublic(NnContext.class.getModifiers())) {
        typeCount++;
      }
      if (Modifier.isPublic(NnGraph.class.getModifiers())) {
        typeCount++;
      }
      if (Modifier.isPublic(NnGraphExecutionContext.class.getModifiers())) {
        typeCount++;
      }
      if (Modifier.isPublic(NnTensor.class.getModifiers())) {
        typeCount++;
      }
      if (Modifier.isPublic(NnContextFactory.class.getModifiers())) {
        typeCount++;
      }

      assertEquals(10, typeCount, "Package should have 10 public types");
    }
  }

  // ========================================================================
  // Type Safety Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Safety Tests")
  class TypeSafetyTests {

    @Test
    @DisplayName("NnTensorType byte sizes should match expected values")
    void tensorTypeByteSizesShouldMatch() {
      int[] expectedSizes = {2, 4, 8, 2, 1, 4, 8}; // FP16, FP32, FP64, BF16, U8, I32, I64
      NnTensorType[] types = NnTensorType.values();

      for (int i = 0; i < types.length; i++) {
        assertEquals(
            expectedSizes[i],
            types[i].getByteSize(),
            types[i].name() + " should have correct byte size");
      }
    }

    @Test
    @DisplayName("NnTensorType ordinals should match native codes")
    void ordinalsShouldsMatchNativeCodes() {
      for (NnTensorType type : NnTensorType.values()) {
        assertEquals(
            type.ordinal(),
            type.getNativeCode(),
            type.name() + " ordinal should match native code");
      }
    }
  }
}
