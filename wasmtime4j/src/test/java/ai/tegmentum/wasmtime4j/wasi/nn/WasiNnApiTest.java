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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive API tests for the WASI Neural Network (NN) package.
 *
 * <p>Tests the structure, contracts, and behavior of all classes in the wasi.nn package using
 * reflection-based testing to verify API contracts without requiring runtime initialization.
 *
 * @since 1.0.0
 */
@DisplayName("WASI NN API Tests")
class WasiNnApiTest {

  private static final Logger LOGGER = Logger.getLogger(WasiNnApiTest.class.getName());

  // ==================== NnTensorType Tests ====================

  @Nested
  @DisplayName("NnTensorType Enum Tests")
  class NnTensorTypeTests {

    @Test
    @DisplayName("Should have exactly 7 tensor types")
    void shouldHaveExactly7TensorTypes() {
      NnTensorType[] values = NnTensorType.values();
      assertEquals(7, values.length, "NnTensorType should have exactly 7 values");
      LOGGER.info("NnTensorType has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected tensor types")
    void shouldContainAllExpectedTensorTypes() {
      Set<String> expected =
          new HashSet<>(Arrays.asList("FP16", "FP32", "FP64", "BF16", "U8", "I32", "I64"));
      for (String name : expected) {
        assertDoesNotThrow(() -> NnTensorType.valueOf(name), "NnTensorType should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct byte sizes")
    void shouldHaveCorrectByteSizes() {
      assertEquals(2, NnTensorType.FP16.getByteSize());
      assertEquals(4, NnTensorType.FP32.getByteSize());
      assertEquals(8, NnTensorType.FP64.getByteSize());
      assertEquals(2, NnTensorType.BF16.getByteSize());
      assertEquals(1, NnTensorType.U8.getByteSize());
      assertEquals(4, NnTensorType.I32.getByteSize());
      assertEquals(8, NnTensorType.I64.getByteSize());
    }

    @Test
    @DisplayName("Should have correct WASI names")
    void shouldHaveCorrectWasiNames() {
      assertEquals("fp16", NnTensorType.FP16.getWasiName());
      assertEquals("fp32", NnTensorType.FP32.getWasiName());
      assertEquals("fp64", NnTensorType.FP64.getWasiName());
      assertEquals("bf16", NnTensorType.BF16.getWasiName());
      assertEquals("u8", NnTensorType.U8.getWasiName());
      assertEquals("i32", NnTensorType.I32.getWasiName());
      assertEquals("i64", NnTensorType.I64.getWasiName());
    }

    @Test
    @DisplayName("Should calculate correct byte size for dimensions")
    void shouldCalculateCorrectByteSizeForDimensions() {
      int[] dims = {2, 3, 4}; // 24 elements
      assertEquals(24 * 4, NnTensorType.FP32.calculateByteSize(dims));
      assertEquals(24 * 1, NnTensorType.U8.calculateByteSize(dims));
      assertEquals(24 * 8, NnTensorType.I64.calculateByteSize(dims));
    }

    @Test
    @DisplayName("Should throw for negative dimensions")
    void shouldThrowForNegativeDimensions() {
      int[] dims = {2, -1, 4};
      assertThrows(IllegalArgumentException.class, () -> NnTensorType.FP32.calculateByteSize(dims));
    }

    @Test
    @DisplayName("Should parse from WASI name")
    void shouldParseFromWasiName() {
      assertEquals(NnTensorType.FP32, NnTensorType.fromWasiName("fp32"));
      assertEquals(NnTensorType.U8, NnTensorType.fromWasiName("u8"));
      assertEquals(NnTensorType.I64, NnTensorType.fromWasiName("i64"));
    }

    @Test
    @DisplayName("Should throw for unknown WASI name")
    void shouldThrowForUnknownWasiName() {
      assertThrows(IllegalArgumentException.class, () -> NnTensorType.fromWasiName("unknown"));
    }

    @Test
    @DisplayName("Should convert to and from native code")
    void shouldConvertToAndFromNativeCode() {
      for (NnTensorType type : NnTensorType.values()) {
        int code = type.getNativeCode();
        assertEquals(type, NnTensorType.fromNativeCode(code));
      }
    }

    @Test
    @DisplayName("Should throw for invalid native code")
    void shouldThrowForInvalidNativeCode() {
      assertThrows(IllegalArgumentException.class, () -> NnTensorType.fromNativeCode(-1));
      assertThrows(IllegalArgumentException.class, () -> NnTensorType.fromNativeCode(100));
    }
  }

  // ==================== NnGraphEncoding Tests ====================

  @Nested
  @DisplayName("NnGraphEncoding Enum Tests")
  class NnGraphEncodingTests {

    @Test
    @DisplayName("Should have exactly 7 graph encodings")
    void shouldHaveExactly7GraphEncodings() {
      NnGraphEncoding[] values = NnGraphEncoding.values();
      assertEquals(7, values.length, "NnGraphEncoding should have exactly 7 values");
      LOGGER.info("NnGraphEncoding has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected encodings")
    void shouldContainAllExpectedEncodings() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList(
                  "OPENVINO",
                  "ONNX",
                  "TENSORFLOW",
                  "PYTORCH",
                  "TENSORFLOWLITE",
                  "GGML",
                  "AUTODETECT"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> NnGraphEncoding.valueOf(name), "NnGraphEncoding should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct WASI names")
    void shouldHaveCorrectWasiNames() {
      assertEquals("openvino", NnGraphEncoding.OPENVINO.getWasiName());
      assertEquals("onnx", NnGraphEncoding.ONNX.getWasiName());
      assertEquals("tensorflow", NnGraphEncoding.TENSORFLOW.getWasiName());
      assertEquals("pytorch", NnGraphEncoding.PYTORCH.getWasiName());
      assertEquals("tensorflowlite", NnGraphEncoding.TENSORFLOWLITE.getWasiName());
      assertEquals("ggml", NnGraphEncoding.GGML.getWasiName());
      assertEquals("autodetect", NnGraphEncoding.AUTODETECT.getWasiName());
    }

    @Test
    @DisplayName("Should have correct display names")
    void shouldHaveCorrectDisplayNames() {
      assertEquals("OpenVINO IR", NnGraphEncoding.OPENVINO.getDisplayName());
      assertEquals("ONNX", NnGraphEncoding.ONNX.getDisplayName());
      assertEquals("TensorFlow", NnGraphEncoding.TENSORFLOW.getDisplayName());
      assertEquals("PyTorch", NnGraphEncoding.PYTORCH.getDisplayName());
      assertEquals("TensorFlow Lite", NnGraphEncoding.TENSORFLOWLITE.getDisplayName());
      assertEquals("GGML", NnGraphEncoding.GGML.getDisplayName());
      assertEquals("Auto-detect", NnGraphEncoding.AUTODETECT.getDisplayName());
    }

    @Test
    @DisplayName("Should parse from WASI name")
    void shouldParseFromWasiName() {
      assertEquals(NnGraphEncoding.ONNX, NnGraphEncoding.fromWasiName("onnx"));
      assertEquals(NnGraphEncoding.PYTORCH, NnGraphEncoding.fromWasiName("pytorch"));
    }

    @Test
    @DisplayName("Should convert to and from native code")
    void shouldConvertToAndFromNativeCode() {
      for (NnGraphEncoding encoding : NnGraphEncoding.values()) {
        int code = encoding.getNativeCode();
        assertEquals(encoding, NnGraphEncoding.fromNativeCode(code));
      }
    }
  }

  // ==================== NnExecutionTarget Tests ====================

  @Nested
  @DisplayName("NnExecutionTarget Enum Tests")
  class NnExecutionTargetTests {

    @Test
    @DisplayName("Should have exactly 3 execution targets")
    void shouldHaveExactly3ExecutionTargets() {
      NnExecutionTarget[] values = NnExecutionTarget.values();
      assertEquals(3, values.length, "NnExecutionTarget should have exactly 3 values");
      LOGGER.info("NnExecutionTarget has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected targets")
    void shouldContainAllExpectedTargets() {
      Set<String> expected = new HashSet<>(Arrays.asList("CPU", "GPU", "TPU"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> NnExecutionTarget.valueOf(name), "NnExecutionTarget should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct WASI names")
    void shouldHaveCorrectWasiNames() {
      assertEquals("cpu", NnExecutionTarget.CPU.getWasiName());
      assertEquals("gpu", NnExecutionTarget.GPU.getWasiName());
      assertEquals("tpu", NnExecutionTarget.TPU.getWasiName());
    }

    @Test
    @DisplayName("Should have correct display names")
    void shouldHaveCorrectDisplayNames() {
      assertEquals("CPU", NnExecutionTarget.CPU.getDisplayName());
      assertEquals("GPU", NnExecutionTarget.GPU.getDisplayName());
      assertEquals("TPU", NnExecutionTarget.TPU.getDisplayName());
    }

    @Test
    @DisplayName("Should parse from WASI name")
    void shouldParseFromWasiName() {
      assertEquals(NnExecutionTarget.CPU, NnExecutionTarget.fromWasiName("cpu"));
      assertEquals(NnExecutionTarget.GPU, NnExecutionTarget.fromWasiName("gpu"));
      assertEquals(NnExecutionTarget.TPU, NnExecutionTarget.fromWasiName("tpu"));
    }

    @Test
    @DisplayName("Should convert to and from native code")
    void shouldConvertToAndFromNativeCode() {
      for (NnExecutionTarget target : NnExecutionTarget.values()) {
        int code = target.getNativeCode();
        assertEquals(target, NnExecutionTarget.fromNativeCode(code));
      }
    }
  }

  // ==================== NnErrorCode Tests ====================

  @Nested
  @DisplayName("NnErrorCode Enum Tests")
  class NnErrorCodeTests {

    @Test
    @DisplayName("Should have exactly 9 error codes")
    void shouldHaveExactly9ErrorCodes() {
      NnErrorCode[] values = NnErrorCode.values();
      assertEquals(9, values.length, "NnErrorCode should have exactly 9 values");
      LOGGER.info("NnErrorCode has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected error codes")
    void shouldContainAllExpectedErrorCodes() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList(
                  "INVALID_ARGUMENT",
                  "INVALID_ENCODING",
                  "TIMEOUT",
                  "RUNTIME_ERROR",
                  "UNSUPPORTED_OPERATION",
                  "TOO_LARGE",
                  "NOT_FOUND",
                  "SECURITY",
                  "UNKNOWN"));
      for (String name : expected) {
        assertDoesNotThrow(() -> NnErrorCode.valueOf(name), "NnErrorCode should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct WASI names")
    void shouldHaveCorrectWasiNames() {
      assertEquals("invalid-argument", NnErrorCode.INVALID_ARGUMENT.getWasiName());
      assertEquals("invalid-encoding", NnErrorCode.INVALID_ENCODING.getWasiName());
      assertEquals("timeout", NnErrorCode.TIMEOUT.getWasiName());
      assertEquals("runtime-error", NnErrorCode.RUNTIME_ERROR.getWasiName());
      assertEquals("unsupported-operation", NnErrorCode.UNSUPPORTED_OPERATION.getWasiName());
      assertEquals("too-large", NnErrorCode.TOO_LARGE.getWasiName());
      assertEquals("not-found", NnErrorCode.NOT_FOUND.getWasiName());
      assertEquals("security", NnErrorCode.SECURITY.getWasiName());
      assertEquals("unknown", NnErrorCode.UNKNOWN.getWasiName());
    }

    @Test
    @DisplayName("Should have descriptions")
    void shouldHaveDescriptions() {
      for (NnErrorCode code : NnErrorCode.values()) {
        assertNotNull(code.getDescription());
        assertFalse(code.getDescription().isEmpty());
      }
    }

    @Test
    @DisplayName("Should parse from WASI name")
    void shouldParseFromWasiName() {
      assertEquals(NnErrorCode.TIMEOUT, NnErrorCode.fromWasiName("timeout"));
      assertEquals(NnErrorCode.SECURITY, NnErrorCode.fromWasiName("security"));
    }

    @Test
    @DisplayName("Should return UNKNOWN for invalid native code")
    void shouldReturnUnknownForInvalidNativeCode() {
      assertEquals(NnErrorCode.UNKNOWN, NnErrorCode.fromNativeCode(-1));
      assertEquals(NnErrorCode.UNKNOWN, NnErrorCode.fromNativeCode(100));
    }
  }

  // ==================== NnException Tests ====================

  @Nested
  @DisplayName("NnException Class Tests")
  class NnExceptionTests {

    @Test
    @DisplayName("Should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(NnException.class),
          "NnException should extend WasmException");
    }

    @Test
    @DisplayName("Should have four constructors")
    void shouldHaveFourConstructors() {
      Constructor<?>[] constructors = NnException.class.getDeclaredConstructors();
      assertEquals(4, constructors.length, "NnException should have 4 constructors");
    }

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      NnException ex = new NnException("test message");
      assertEquals("test message", ex.getMessage());
      assertEquals(NnErrorCode.UNKNOWN, ex.getErrorCode());
      assertNull(ex.getCause());
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      RuntimeException cause = new RuntimeException("cause");
      NnException ex = new NnException("test message", cause);
      assertEquals("test message", ex.getMessage());
      assertEquals(NnErrorCode.UNKNOWN, ex.getErrorCode());
      assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("Should create exception with error code and message")
    void shouldCreateExceptionWithErrorCodeAndMessage() {
      NnException ex = new NnException(NnErrorCode.TIMEOUT, "operation timed out");
      assertTrue(ex.getMessage().contains("operation timed out"));
      assertTrue(ex.getMessage().contains("timeout"));
      assertEquals(NnErrorCode.TIMEOUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("Should create exception with error code, message, and cause")
    void shouldCreateExceptionWithErrorCodeMessageAndCause() {
      RuntimeException cause = new RuntimeException("cause");
      NnException ex = new NnException(NnErrorCode.RUNTIME_ERROR, "inference failed", cause);
      assertTrue(ex.getMessage().contains("inference failed"));
      assertEquals(NnErrorCode.RUNTIME_ERROR, ex.getErrorCode());
      assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("Should have error checking convenience methods")
    void shouldHaveErrorCheckingConvenienceMethods() {
      NnException invalidArg = new NnException(NnErrorCode.INVALID_ARGUMENT, "bad arg");
      assertTrue(invalidArg.isInvalidArgument());
      assertFalse(invalidArg.isTimeout());

      NnException timeout = new NnException(NnErrorCode.TIMEOUT, "timed out");
      assertTrue(timeout.isTimeout());
      assertFalse(timeout.isInvalidArgument());

      NnException notFound = new NnException(NnErrorCode.NOT_FOUND, "not found");
      assertTrue(notFound.isNotFound());

      NnException security = new NnException(NnErrorCode.SECURITY, "security");
      assertTrue(security.isSecurity());
    }

    @Test
    @DisplayName("Should have static factory methods")
    void shouldHaveStaticFactoryMethods() {
      assertTrue(NnException.invalidArgument("test").isInvalidArgument());
      assertTrue(NnException.invalidEncoding("test").isInvalidEncoding());
      assertTrue(NnException.timeout("test").isTimeout());
      assertTrue(NnException.runtimeError("test").isRuntimeError());
      assertTrue(NnException.unsupportedOperation("test").isUnsupportedOperation());
      assertTrue(NnException.tooLarge("test").isTooLarge());
      assertTrue(NnException.notFound("test").isNotFound());
      assertTrue(NnException.security("test").isSecurity());
    }

    @Test
    @DisplayName("Should create from native error code")
    void shouldCreateFromNativeErrorCode() {
      NnException ex = NnException.fromNativeError(2, "timeout error");
      assertEquals(NnErrorCode.TIMEOUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("Should have serialVersionUID")
    void shouldHaveSerialVersionUid() {
      assertDoesNotThrow(
          () -> {
            java.lang.reflect.Field field = NnException.class.getDeclaredField("serialVersionUID");
            assertTrue(Modifier.isPrivate(field.getModifiers()));
            assertTrue(Modifier.isStatic(field.getModifiers()));
            assertTrue(Modifier.isFinal(field.getModifiers()));
          });
    }
  }

  // ==================== NnTensor Tests ====================

  @Nested
  @DisplayName("NnTensor Class Tests")
  class NnTensorTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(NnTensor.class.getModifiers()), "NnTensor should be final");
    }

    @Test
    @DisplayName("Should create tensor from float array")
    void shouldCreateTensorFromFloatArray() {
      int[] dims = {2, 3};
      float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
      NnTensor tensor = NnTensor.fromFloatArray(dims, data);

      assertArrayEquals(dims, tensor.getDimensions());
      assertEquals(NnTensorType.FP32, tensor.getType());
      assertEquals(6, tensor.getElementCount());
      assertEquals(24, tensor.getByteSize()); // 6 * 4 bytes
      assertArrayEquals(data, tensor.toFloatArray());
      assertFalse(tensor.isNamed());
      assertNull(tensor.getName());
    }

    @Test
    @DisplayName("Should create named tensor from float array")
    void shouldCreateNamedTensorFromFloatArray() {
      int[] dims = {2, 2};
      float[] data = {1.0f, 2.0f, 3.0f, 4.0f};
      NnTensor tensor = NnTensor.fromFloatArray("input", dims, data);

      assertEquals("input", tensor.getName());
      assertTrue(tensor.isNamed());
      assertArrayEquals(dims, tensor.getDimensions());
      assertEquals(NnTensorType.FP32, tensor.getType());
    }

    @Test
    @DisplayName("Should create tensor from byte array")
    void shouldCreateTensorFromByteArray() {
      int[] dims = {4};
      byte[] data = {1, 2, 3, 4};
      NnTensor tensor = NnTensor.fromByteArray(dims, data);

      assertEquals(NnTensorType.U8, tensor.getType());
      assertEquals(4, tensor.getElementCount());
      assertEquals(4, tensor.getByteSize());
      assertArrayEquals(data, tensor.toByteArray());
    }

    @Test
    @DisplayName("Should create tensor from int array")
    void shouldCreateTensorFromIntArray() {
      int[] dims = {3};
      int[] data = {100, 200, 300};
      NnTensor tensor = NnTensor.fromIntArray(dims, data);

      assertEquals(NnTensorType.I32, tensor.getType());
      assertEquals(3, tensor.getElementCount());
      assertEquals(12, tensor.getByteSize()); // 3 * 4 bytes
      assertArrayEquals(data, tensor.toIntArray());
    }

    @Test
    @DisplayName("Should create tensor from long array")
    void shouldCreateTensorFromLongArray() {
      int[] dims = {2};
      long[] data = {1000000000000L, 2000000000000L};
      NnTensor tensor = NnTensor.fromLongArray(dims, data);

      assertEquals(NnTensorType.I64, tensor.getType());
      assertEquals(2, tensor.getElementCount());
      assertEquals(16, tensor.getByteSize()); // 2 * 8 bytes
      assertArrayEquals(data, tensor.toLongArray());
    }

    @Test
    @DisplayName("Should create tensor from raw bytes")
    void shouldCreateTensorFromRawBytes() {
      int[] dims = {2};
      byte[] data = new byte[8]; // 2 * 4 bytes for FP32
      NnTensor tensor = NnTensor.fromBytes(dims, NnTensorType.FP32, data);

      assertEquals(NnTensorType.FP32, tensor.getType());
      assertEquals(2, tensor.getElementCount());
    }

    @Test
    @DisplayName("Should throw for mismatched data size")
    void shouldThrowForMismatchedDataSize() {
      int[] dims = {2, 3}; // 6 elements expected
      float[] wrongData = {1.0f, 2.0f}; // Only 2 elements
      assertThrows(IllegalArgumentException.class, () -> NnTensor.fromFloatArray(dims, wrongData));
    }

    @Test
    @DisplayName("Should throw for empty dimensions")
    void shouldThrowForEmptyDimensions() {
      int[] emptyDims = {};
      float[] data = {1.0f};
      assertThrows(IllegalArgumentException.class, () -> NnTensor.fromFloatArray(emptyDims, data));
    }

    @Test
    @DisplayName("Should throw for negative dimensions")
    void shouldThrowForNegativeDimensions() {
      int[] negDims = {2, -1};
      float[] data = {1.0f, 2.0f};
      assertThrows(IllegalArgumentException.class, () -> NnTensor.fromFloatArray(negDims, data));
    }

    @Test
    @DisplayName("Should throw when converting to wrong type array")
    void shouldThrowWhenConvertingToWrongTypeArray() {
      int[] dims = {2};
      float[] data = {1.0f, 2.0f};
      NnTensor tensor = NnTensor.fromFloatArray(dims, data);

      assertThrows(IllegalStateException.class, tensor::toByteArray);
      assertThrows(IllegalStateException.class, tensor::toIntArray);
      assertThrows(IllegalStateException.class, tensor::toLongArray);
    }

    @Test
    @DisplayName("Should get rank correctly")
    void shouldGetRankCorrectly() {
      int[] dims1d = {10};
      int[] dims2d = {2, 5};
      int[] dims3d = {2, 3, 4};

      assertEquals(1, NnTensor.fromFloatArray(dims1d, new float[10]).getRank());
      assertEquals(2, NnTensor.fromFloatArray(dims2d, new float[10]).getRank());
      assertEquals(3, NnTensor.fromFloatArray(dims3d, new float[24]).getRank());
    }

    @Test
    @DisplayName("Should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
      int[] dims = {2, 2};
      float[] data = {1.0f, 2.0f, 3.0f, 4.0f};
      NnTensor tensor1 = NnTensor.fromFloatArray("input", dims, data);
      NnTensor tensor2 = NnTensor.fromFloatArray("input", dims, data);
      NnTensor tensor3 = NnTensor.fromFloatArray("other", dims, data);

      assertEquals(tensor1, tensor2);
      assertEquals(tensor1.hashCode(), tensor2.hashCode());
      assertFalse(tensor1.equals(tensor3));
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
      int[] dims = {2, 3};
      float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
      NnTensor tensor = NnTensor.fromFloatArray("test", dims, data);

      String str = tensor.toString();
      assertTrue(str.contains("NnTensor"));
      assertTrue(str.contains("test"));
      assertTrue(str.contains("FP32"));
    }
  }

  // ==================== NnContext Interface Tests ====================

  @Nested
  @DisplayName("NnContext Interface Tests")
  class NnContextInterfaceTests {

    @Test
    @DisplayName("Should be an interface")
    void shouldBeAnInterface() {
      assertTrue(NnContext.class.isInterface(), "NnContext should be an interface");
    }

    @Test
    @DisplayName("Should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(NnContext.class),
          "NnContext should extend AutoCloseable");
    }

    @Test
    @DisplayName("Should have loadGraph method with byte array")
    void shouldHaveLoadGraphMethodWithByteArray() {
      assertDoesNotThrow(
          () -> {
            Method method =
                NnContext.class.getDeclaredMethod(
                    "loadGraph", byte[].class, NnGraphEncoding.class, NnExecutionTarget.class);
            assertEquals(NnGraph.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have loadGraph method with list of byte arrays")
    void shouldHaveLoadGraphMethodWithListOfByteArrays() {
      assertDoesNotThrow(
          () -> {
            Method method =
                NnContext.class.getDeclaredMethod(
                    "loadGraph", List.class, NnGraphEncoding.class, NnExecutionTarget.class);
            assertEquals(NnGraph.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have loadGraphFromFile method")
    void shouldHaveLoadGraphFromFileMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                NnContext.class.getDeclaredMethod(
                    "loadGraphFromFile",
                    Path.class,
                    NnGraphEncoding.class,
                    NnExecutionTarget.class);
            assertEquals(NnGraph.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have loadGraphByName method")
    void shouldHaveLoadGraphByNameMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnContext.class.getDeclaredMethod("loadGraphByName", String.class);
            assertEquals(NnGraph.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getSupportedEncodings method")
    void shouldHaveGetSupportedEncodingsMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnContext.class.getDeclaredMethod("getSupportedEncodings");
            assertEquals(Set.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getSupportedTargets method")
    void shouldHaveGetSupportedTargetsMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnContext.class.getDeclaredMethod("getSupportedTargets");
            assertEquals(Set.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isEncodingSupported method")
    void shouldHaveIsEncodingSupportedMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                NnContext.class.getDeclaredMethod("isEncodingSupported", NnGraphEncoding.class);
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isTargetSupported method")
    void shouldHaveIsTargetSupportedMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                NnContext.class.getDeclaredMethod("isTargetSupported", NnExecutionTarget.class);
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isAvailable method")
    void shouldHaveIsAvailableMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnContext.class.getDeclaredMethod("isAvailable");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getImplementationInfo method")
    void shouldHaveGetImplementationInfoMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnContext.class.getDeclaredMethod("getImplementationInfo");
            assertEquals(NnContext.NnImplementationInfo.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isValid method")
    void shouldHaveIsValidMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnContext.class.getDeclaredMethod("isValid");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have inner NnImplementationInfo class")
    void shouldHaveInnerNnImplementationInfoClass() {
      Class<?>[] innerClasses = NnContext.class.getDeclaredClasses();
      boolean hasImplementationInfo = false;
      for (Class<?> inner : innerClasses) {
        if (inner.getSimpleName().equals("NnImplementationInfo")) {
          hasImplementationInfo = true;
          assertTrue(Modifier.isFinal(inner.getModifiers()));
          break;
        }
      }
      assertTrue(hasImplementationInfo, "NnContext should have NnImplementationInfo inner class");
    }
  }

  // ==================== NnImplementationInfo Tests ====================

  @Nested
  @DisplayName("NnImplementationInfo Class Tests")
  class NnImplementationInfoTests {

    @Test
    @DisplayName("Should create implementation info")
    void shouldCreateImplementationInfo() {
      List<String> backends = Arrays.asList("openvino", "onnx");
      NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", backends, "openvino");

      assertEquals("1.0.0", info.getVersion());
      assertEquals(backends, info.getBackends());
      assertEquals("openvino", info.getDefaultBackend());
    }

    @Test
    @DisplayName("Should check backend availability")
    void shouldCheckBackendAvailability() {
      List<String> backends = Arrays.asList("openvino", "onnx");
      NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", backends, "openvino");

      assertTrue(info.hasBackend("openvino"));
      assertTrue(info.hasBackend("onnx"));
      assertFalse(info.hasBackend("tensorflow"));
    }

    @Test
    @DisplayName("Should handle null backends")
    void shouldHandleNullBackends() {
      NnContext.NnImplementationInfo info = new NnContext.NnImplementationInfo("1.0.0", null, null);

      assertNull(info.getBackends());
      assertNull(info.getDefaultBackend());
      assertFalse(info.hasBackend("any"));
    }
  }

  // ==================== NnGraph Interface Tests ====================

  @Nested
  @DisplayName("NnGraph Interface Tests")
  class NnGraphInterfaceTests {

    @Test
    @DisplayName("Should be an interface")
    void shouldBeAnInterface() {
      assertTrue(NnGraph.class.isInterface(), "NnGraph should be an interface");
    }

    @Test
    @DisplayName("Should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(NnGraph.class),
          "NnGraph should extend AutoCloseable");
    }

    @Test
    @DisplayName("Should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraph.class.getDeclaredMethod("getNativeHandle");
            assertEquals(long.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getEncoding method")
    void shouldHaveGetEncodingMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraph.class.getDeclaredMethod("getEncoding");
            assertEquals(NnGraphEncoding.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getExecutionTarget method")
    void shouldHaveGetExecutionTargetMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraph.class.getDeclaredMethod("getExecutionTarget");
            assertEquals(NnExecutionTarget.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have createExecutionContext method")
    void shouldHaveCreateExecutionContextMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraph.class.getDeclaredMethod("createExecutionContext");
            assertEquals(NnGraphExecutionContext.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isValid method")
    void shouldHaveIsValidMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraph.class.getDeclaredMethod("isValid");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getModelName method")
    void shouldHaveGetModelNameMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraph.class.getDeclaredMethod("getModelName");
            assertEquals(String.class, method.getReturnType());
          });
    }
  }

  // ==================== NnGraphExecutionContext Interface Tests ====================

  @Nested
  @DisplayName("NnGraphExecutionContext Interface Tests")
  class NnGraphExecutionContextInterfaceTests {

    @Test
    @DisplayName("Should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          NnGraphExecutionContext.class.isInterface(),
          "NnGraphExecutionContext should be an interface");
    }

    @Test
    @DisplayName("Should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(NnGraphExecutionContext.class),
          "NnGraphExecutionContext should extend AutoCloseable");
    }

    @Test
    @DisplayName("Should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraphExecutionContext.class.getDeclaredMethod("getNativeHandle");
            assertEquals(long.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getGraph method")
    void shouldHaveGetGraphMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraphExecutionContext.class.getDeclaredMethod("getGraph");
            assertEquals(NnGraph.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have compute method")
    void shouldHaveComputeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraphExecutionContext.class.getDeclaredMethod("compute", List.class);
            assertEquals(List.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have computeByIndex method")
    void shouldHaveComputeByIndexMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                NnGraphExecutionContext.class.getDeclaredMethod("computeByIndex", NnTensor[].class);
            assertEquals(List.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have setInput methods")
    void shouldHaveSetInputMethods() {
      assertDoesNotThrow(
          () -> {
            Method byIndex =
                NnGraphExecutionContext.class.getDeclaredMethod(
                    "setInput", int.class, NnTensor.class);
            Method byName =
                NnGraphExecutionContext.class.getDeclaredMethod(
                    "setInput", String.class, NnTensor.class);
            assertEquals(void.class, byIndex.getReturnType());
            assertEquals(void.class, byName.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have computeNoInputs method")
    void shouldHaveComputeNoInputsMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraphExecutionContext.class.getDeclaredMethod("computeNoInputs");
            assertEquals(List.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getOutput methods")
    void shouldHaveGetOutputMethods() {
      assertDoesNotThrow(
          () -> {
            Method byIndex =
                NnGraphExecutionContext.class.getDeclaredMethod("getOutput", int.class);
            Method byName =
                NnGraphExecutionContext.class.getDeclaredMethod("getOutput", String.class);
            assertEquals(NnTensor.class, byIndex.getReturnType());
            assertEquals(NnTensor.class, byName.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getInputCount method")
    void shouldHaveGetInputCountMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraphExecutionContext.class.getDeclaredMethod("getInputCount");
            assertEquals(int.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getOutputCount method")
    void shouldHaveGetOutputCountMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraphExecutionContext.class.getDeclaredMethod("getOutputCount");
            assertEquals(int.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getInputMetadata method")
    void shouldHaveGetInputMetadataMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraphExecutionContext.class.getDeclaredMethod("getInputMetadata");
            assertEquals(Map.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getOutputMetadata method")
    void shouldHaveGetOutputMetadataMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraphExecutionContext.class.getDeclaredMethod("getOutputMetadata");
            assertEquals(Map.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isValid method")
    void shouldHaveIsValidMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnGraphExecutionContext.class.getDeclaredMethod("isValid");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have inner NnTensorMetadata class")
    void shouldHaveInnerNnTensorMetadataClass() {
      Class<?>[] innerClasses = NnGraphExecutionContext.class.getDeclaredClasses();
      boolean hasMetadata = false;
      for (Class<?> inner : innerClasses) {
        if (inner.getSimpleName().equals("NnTensorMetadata")) {
          hasMetadata = true;
          assertTrue(Modifier.isFinal(inner.getModifiers()));
          break;
        }
      }
      assertTrue(hasMetadata, "NnGraphExecutionContext should have NnTensorMetadata inner class");
    }
  }

  // ==================== NnTensorMetadata Tests ====================

  @Nested
  @DisplayName("NnTensorMetadata Class Tests")
  class NnTensorMetadataTests {

    @Test
    @DisplayName("Should create tensor metadata")
    void shouldCreateTensorMetadata() {
      int[] dims = {1, 3, 224, 224};
      NnGraphExecutionContext.NnTensorMetadata metadata =
          new NnGraphExecutionContext.NnTensorMetadata("input", dims, NnTensorType.FP32);

      assertEquals("input", metadata.getName());
      assertArrayEquals(dims, metadata.getDimensions());
      assertEquals(NnTensorType.FP32, metadata.getType());
      assertTrue(metadata.hasFixedDimensions());
      assertTrue(metadata.hasFixedType());
    }

    @Test
    @DisplayName("Should handle dynamic dimensions")
    void shouldHandleDynamicDimensions() {
      NnGraphExecutionContext.NnTensorMetadata metadata =
          new NnGraphExecutionContext.NnTensorMetadata("input", null, NnTensorType.FP32);

      assertNull(metadata.getDimensions());
      assertFalse(metadata.hasFixedDimensions());
      assertTrue(metadata.hasFixedType());
    }

    @Test
    @DisplayName("Should handle dynamic type")
    void shouldHandleDynamicType() {
      int[] dims = {1, 3, 224, 224};
      NnGraphExecutionContext.NnTensorMetadata metadata =
          new NnGraphExecutionContext.NnTensorMetadata("input", dims, null);

      assertArrayEquals(dims, metadata.getDimensions());
      assertNull(metadata.getType());
      assertTrue(metadata.hasFixedDimensions());
      assertFalse(metadata.hasFixedType());
    }
  }

  // ==================== NnContextFactory Interface Tests ====================

  @Nested
  @DisplayName("NnContextFactory Interface Tests")
  class NnContextFactoryInterfaceTests {

    @Test
    @DisplayName("Should be an interface")
    void shouldBeAnInterface() {
      assertTrue(NnContextFactory.class.isInterface(), "NnContextFactory should be an interface");
    }

    @Test
    @DisplayName("Should have createNnContext method")
    void shouldHaveCreateNnContextMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnContextFactory.class.getDeclaredMethod("createNnContext");
            assertEquals(NnContext.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isNnAvailable method")
    void shouldHaveIsNnAvailableMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnContextFactory.class.getDeclaredMethod("isNnAvailable");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getDefaultExecutionTarget method with default implementation")
    void shouldHaveGetDefaultExecutionTargetMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = NnContextFactory.class.getDeclaredMethod("getDefaultExecutionTarget");
            assertEquals(NnExecutionTarget.class, method.getReturnType());
            assertTrue(method.isDefault(), "getDefaultExecutionTarget should be a default method");
          });
    }
  }

  // ==================== Package Consistency Tests ====================

  @Nested
  @DisplayName("Package Consistency Tests")
  class PackageConsistencyTests {

    @Test
    @DisplayName("All NN classes should be in the same package")
    void allNnClassesShouldBeInSamePackage() {
      String expectedPackage = "ai.tegmentum.wasmtime4j.wasi.nn";
      assertEquals(expectedPackage, NnContext.class.getPackage().getName());
      assertEquals(expectedPackage, NnGraph.class.getPackage().getName());
      assertEquals(expectedPackage, NnGraphExecutionContext.class.getPackage().getName());
      assertEquals(expectedPackage, NnContextFactory.class.getPackage().getName());
      assertEquals(expectedPackage, NnTensor.class.getPackage().getName());
      assertEquals(expectedPackage, NnException.class.getPackage().getName());
      assertEquals(expectedPackage, NnTensorType.class.getPackage().getName());
      assertEquals(expectedPackage, NnGraphEncoding.class.getPackage().getName());
      assertEquals(expectedPackage, NnExecutionTarget.class.getPackage().getName());
      assertEquals(expectedPackage, NnErrorCode.class.getPackage().getName());
    }

    @Test
    @DisplayName("All enums should have consistent getWasiName pattern")
    void allEnumsShouldHaveConsistentNamingPattern() {
      assertDoesNotThrow(() -> NnTensorType.class.getDeclaredMethod("getWasiName"));
      assertDoesNotThrow(() -> NnGraphEncoding.class.getDeclaredMethod("getWasiName"));
      assertDoesNotThrow(() -> NnExecutionTarget.class.getDeclaredMethod("getWasiName"));
      assertDoesNotThrow(() -> NnErrorCode.class.getDeclaredMethod("getWasiName"));
    }

    @Test
    @DisplayName("All enums with codes should have fromNativeCode method")
    void allEnumsWithCodesShouldHaveFromNativeCodeMethod() {
      assertDoesNotThrow(() -> NnTensorType.class.getDeclaredMethod("fromNativeCode", int.class));
      assertDoesNotThrow(
          () -> NnGraphEncoding.class.getDeclaredMethod("fromNativeCode", int.class));
      assertDoesNotThrow(
          () -> NnExecutionTarget.class.getDeclaredMethod("fromNativeCode", int.class));
      assertDoesNotThrow(() -> NnErrorCode.class.getDeclaredMethod("fromNativeCode", int.class));
    }
  }

  // ==================== Exception Throwing Tests ====================

  @Nested
  @DisplayName("Exception Throwing Tests")
  class ExceptionThrowingTests {

    @Test
    @DisplayName("NnContext methods should declare NnException")
    void nnContextMethodsShouldDeclareNnException() {
      assertDoesNotThrow(
          () -> {
            Method loadGraph =
                NnContext.class.getDeclaredMethod(
                    "loadGraph", byte[].class, NnGraphEncoding.class, NnExecutionTarget.class);
            assertTrue(
                Arrays.asList(loadGraph.getExceptionTypes()).contains(NnException.class),
                "loadGraph should declare NnException");
          });
    }

    @Test
    @DisplayName("NnGraph createExecutionContext should declare NnException")
    void nnGraphCreateExecutionContextShouldDeclareNnException() {
      assertDoesNotThrow(
          () -> {
            Method createExec = NnGraph.class.getDeclaredMethod("createExecutionContext");
            assertTrue(
                Arrays.asList(createExec.getExceptionTypes()).contains(NnException.class),
                "createExecutionContext should declare NnException");
          });
    }

    @Test
    @DisplayName("NnGraphExecutionContext compute should declare NnException")
    void nnGraphExecutionContextComputeShouldDeclareNnException() {
      assertDoesNotThrow(
          () -> {
            Method compute = NnGraphExecutionContext.class.getDeclaredMethod("compute", List.class);
            assertTrue(
                Arrays.asList(compute.getExceptionTypes()).contains(NnException.class),
                "compute should declare NnException");
          });
    }
  }
}
