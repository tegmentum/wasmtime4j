/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI Neural Network package classes.
 *
 * <p>This test class validates the NN enums, NnTensor, and NnException.
 */
@DisplayName("WASI NN Integration Tests")
public class WasiNnTest {

  private static final Logger LOGGER = Logger.getLogger(WasiNnTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WASI NN Integration Tests");
  }

  @Nested
  @DisplayName("NnTensorType Tests")
  class NnTensorTypeTests {

    @Test
    @DisplayName("Should have all expected tensor types")
    void shouldHaveAllExpectedTensorTypes() {
      LOGGER.info("Testing NnTensorType enum values");

      NnTensorType[] types = NnTensorType.values();
      assertEquals(7, types.length, "Should have 7 tensor types");

      assertNotNull(NnTensorType.FP16, "FP16 should exist");
      assertNotNull(NnTensorType.FP32, "FP32 should exist");
      assertNotNull(NnTensorType.FP64, "FP64 should exist");
      assertNotNull(NnTensorType.BF16, "BF16 should exist");
      assertNotNull(NnTensorType.U8, "U8 should exist");
      assertNotNull(NnTensorType.I32, "I32 should exist");
      assertNotNull(NnTensorType.I64, "I64 should exist");

      LOGGER.info("NnTensorType enum verified");
    }

    @Test
    @DisplayName("Should have correct byte sizes")
    void shouldHaveCorrectByteSizes() {
      LOGGER.info("Testing NnTensorType byte sizes");

      assertEquals(2, NnTensorType.FP16.getByteSize(), "FP16 should be 2 bytes");
      assertEquals(4, NnTensorType.FP32.getByteSize(), "FP32 should be 4 bytes");
      assertEquals(8, NnTensorType.FP64.getByteSize(), "FP64 should be 8 bytes");
      assertEquals(2, NnTensorType.BF16.getByteSize(), "BF16 should be 2 bytes");
      assertEquals(1, NnTensorType.U8.getByteSize(), "U8 should be 1 byte");
      assertEquals(4, NnTensorType.I32.getByteSize(), "I32 should be 4 bytes");
      assertEquals(8, NnTensorType.I64.getByteSize(), "I64 should be 8 bytes");

      LOGGER.info("Byte sizes verified");
    }

    @Test
    @DisplayName("Should have correct WASI names")
    void shouldHaveCorrectWasiNames() {
      LOGGER.info("Testing NnTensorType WASI names");

      assertEquals("fp16", NnTensorType.FP16.getWasiName());
      assertEquals("fp32", NnTensorType.FP32.getWasiName());
      assertEquals("fp64", NnTensorType.FP64.getWasiName());
      assertEquals("bf16", NnTensorType.BF16.getWasiName());
      assertEquals("u8", NnTensorType.U8.getWasiName());
      assertEquals("i32", NnTensorType.I32.getWasiName());
      assertEquals("i64", NnTensorType.I64.getWasiName());

      LOGGER.info("WASI names verified");
    }

    @Test
    @DisplayName("Should parse from WASI name")
    void shouldParseFromWasiName() {
      LOGGER.info("Testing NnTensorType fromWasiName");

      assertEquals(NnTensorType.FP32, NnTensorType.fromWasiName("fp32"));
      assertEquals(NnTensorType.I64, NnTensorType.fromWasiName("i64"));

      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.fromWasiName("unknown"),
          "Should reject unknown WASI name");

      LOGGER.info("fromWasiName verified");
    }

    @Test
    @DisplayName("Should convert to/from native code")
    void shouldConvertToFromNativeCode() {
      LOGGER.info("Testing NnTensorType native code conversion");

      for (NnTensorType type : NnTensorType.values()) {
        int code = type.getNativeCode();
        assertEquals(type, NnTensorType.fromNativeCode(code), "Round trip should match");
      }

      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.fromNativeCode(-1),
          "Should reject negative code");

      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.fromNativeCode(100),
          "Should reject out of range code");

      LOGGER.info("Native code conversion verified");
    }

    @Test
    @DisplayName("Should calculate byte size for dimensions")
    void shouldCalculateByteSizeForDimensions() {
      LOGGER.info("Testing NnTensorType calculateByteSize");

      // 1x3x224x224 FP32 image
      int[] imageDims = {1, 3, 224, 224};
      long expected = 4L * 1 * 3 * 224 * 224; // 602112 bytes
      assertEquals(expected, NnTensorType.FP32.calculateByteSize(imageDims));

      // Empty dimensions
      assertEquals(0, NnTensorType.FP32.calculateByteSize(null));
      assertEquals(0, NnTensorType.FP32.calculateByteSize(new int[0]));

      // Negative dimension should throw
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.FP32.calculateByteSize(new int[] {1, -1, 10}),
          "Should reject negative dimensions");

      LOGGER.info("calculateByteSize verified");
    }
  }

  @Nested
  @DisplayName("NnGraphEncoding Tests")
  class NnGraphEncodingTests {

    @Test
    @DisplayName("Should have all expected encodings")
    void shouldHaveAllExpectedEncodings() {
      LOGGER.info("Testing NnGraphEncoding enum values");

      NnGraphEncoding[] encodings = NnGraphEncoding.values();
      assertEquals(7, encodings.length, "Should have 7 graph encodings");

      assertNotNull(NnGraphEncoding.OPENVINO, "OPENVINO should exist");
      assertNotNull(NnGraphEncoding.ONNX, "ONNX should exist");
      assertNotNull(NnGraphEncoding.TENSORFLOW, "TENSORFLOW should exist");
      assertNotNull(NnGraphEncoding.PYTORCH, "PYTORCH should exist");
      assertNotNull(NnGraphEncoding.TENSORFLOWLITE, "TENSORFLOWLITE should exist");
      assertNotNull(NnGraphEncoding.GGML, "GGML should exist");
      assertNotNull(NnGraphEncoding.AUTODETECT, "AUTODETECT should exist");

      LOGGER.info("NnGraphEncoding enum verified");
    }

    @Test
    @DisplayName("Should have WASI and display names")
    void shouldHaveWasiAndDisplayNames() {
      LOGGER.info("Testing NnGraphEncoding names");

      assertEquals("onnx", NnGraphEncoding.ONNX.getWasiName());
      assertEquals("ONNX", NnGraphEncoding.ONNX.getDisplayName());
      assertEquals("ONNX", NnGraphEncoding.ONNX.toString());

      assertEquals("pytorch", NnGraphEncoding.PYTORCH.getWasiName());
      assertEquals("PyTorch", NnGraphEncoding.PYTORCH.getDisplayName());

      LOGGER.info("Names verified");
    }

    @Test
    @DisplayName("Should parse from WASI name")
    void shouldParseFromWasiName() {
      LOGGER.info("Testing NnGraphEncoding fromWasiName");

      assertEquals(NnGraphEncoding.ONNX, NnGraphEncoding.fromWasiName("onnx"));
      assertEquals(NnGraphEncoding.AUTODETECT, NnGraphEncoding.fromWasiName("autodetect"));

      assertThrows(
          IllegalArgumentException.class,
          () -> NnGraphEncoding.fromWasiName("invalid"),
          "Should reject unknown WASI name");

      LOGGER.info("fromWasiName verified");
    }

    @Test
    @DisplayName("Should convert to/from native code")
    void shouldConvertToFromNativeCode() {
      LOGGER.info("Testing NnGraphEncoding native code conversion");

      for (NnGraphEncoding encoding : NnGraphEncoding.values()) {
        int code = encoding.getNativeCode();
        assertEquals(encoding, NnGraphEncoding.fromNativeCode(code));
      }

      assertThrows(IllegalArgumentException.class, () -> NnGraphEncoding.fromNativeCode(-1));

      LOGGER.info("Native code conversion verified");
    }
  }

  @Nested
  @DisplayName("NnExecutionTarget Tests")
  class NnExecutionTargetTests {

    @Test
    @DisplayName("Should have all expected targets")
    void shouldHaveAllExpectedTargets() {
      LOGGER.info("Testing NnExecutionTarget enum values");

      NnExecutionTarget[] targets = NnExecutionTarget.values();
      assertEquals(3, targets.length, "Should have 3 execution targets");

      assertNotNull(NnExecutionTarget.CPU, "CPU should exist");
      assertNotNull(NnExecutionTarget.GPU, "GPU should exist");
      assertNotNull(NnExecutionTarget.TPU, "TPU should exist");

      LOGGER.info("NnExecutionTarget enum verified");
    }

    @Test
    @DisplayName("Should have WASI and display names")
    void shouldHaveWasiAndDisplayNames() {
      LOGGER.info("Testing NnExecutionTarget names");

      assertEquals("cpu", NnExecutionTarget.CPU.getWasiName());
      assertEquals("CPU", NnExecutionTarget.CPU.getDisplayName());
      assertEquals("CPU", NnExecutionTarget.CPU.toString());

      assertEquals("gpu", NnExecutionTarget.GPU.getWasiName());
      assertEquals("GPU", NnExecutionTarget.GPU.getDisplayName());

      LOGGER.info("Names verified");
    }

    @Test
    @DisplayName("Should parse from WASI name")
    void shouldParseFromWasiName() {
      LOGGER.info("Testing NnExecutionTarget fromWasiName");

      assertEquals(NnExecutionTarget.CPU, NnExecutionTarget.fromWasiName("cpu"));
      assertEquals(NnExecutionTarget.TPU, NnExecutionTarget.fromWasiName("tpu"));

      assertThrows(
          IllegalArgumentException.class,
          () -> NnExecutionTarget.fromWasiName("fpga"),
          "Should reject unknown target");

      LOGGER.info("fromWasiName verified");
    }
  }

  @Nested
  @DisplayName("NnErrorCode Tests")
  class NnErrorCodeTests {

    @Test
    @DisplayName("Should have all expected error codes")
    void shouldHaveAllExpectedErrorCodes() {
      LOGGER.info("Testing NnErrorCode enum values");

      NnErrorCode[] codes = NnErrorCode.values();
      assertEquals(9, codes.length, "Should have 9 error codes");

      assertNotNull(NnErrorCode.INVALID_ARGUMENT, "INVALID_ARGUMENT should exist");
      assertNotNull(NnErrorCode.INVALID_ENCODING, "INVALID_ENCODING should exist");
      assertNotNull(NnErrorCode.TIMEOUT, "TIMEOUT should exist");
      assertNotNull(NnErrorCode.RUNTIME_ERROR, "RUNTIME_ERROR should exist");
      assertNotNull(NnErrorCode.UNSUPPORTED_OPERATION, "UNSUPPORTED_OPERATION should exist");
      assertNotNull(NnErrorCode.TOO_LARGE, "TOO_LARGE should exist");
      assertNotNull(NnErrorCode.NOT_FOUND, "NOT_FOUND should exist");
      assertNotNull(NnErrorCode.SECURITY, "SECURITY should exist");
      assertNotNull(NnErrorCode.UNKNOWN, "UNKNOWN should exist");

      LOGGER.info("NnErrorCode enum verified");
    }

    @Test
    @DisplayName("Should have descriptions")
    void shouldHaveDescriptions() {
      LOGGER.info("Testing NnErrorCode descriptions");

      assertNotNull(NnErrorCode.TIMEOUT.getDescription());
      assertTrue(NnErrorCode.TIMEOUT.getDescription().length() > 0);

      String str = NnErrorCode.TIMEOUT.toString();
      assertTrue(str.contains("timeout"));

      LOGGER.info("Descriptions verified");
    }

    @Test
    @DisplayName("Should return UNKNOWN for invalid native codes")
    void shouldReturnUnknownForInvalidNativeCodes() {
      LOGGER.info("Testing NnErrorCode fromNativeCode with invalid codes");

      assertEquals(NnErrorCode.UNKNOWN, NnErrorCode.fromNativeCode(-1));
      assertEquals(NnErrorCode.UNKNOWN, NnErrorCode.fromNativeCode(100));

      LOGGER.info("Invalid native code handling verified");
    }
  }

  @Nested
  @DisplayName("NnTensor Tests")
  class NnTensorTests {

    @Test
    @DisplayName("Should create tensor from float array")
    void shouldCreateTensorFromFloatArray() {
      LOGGER.info("Testing NnTensor fromFloatArray");

      int[] dims = {2, 3};
      float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};

      NnTensor tensor = NnTensor.fromFloatArray(dims, data);

      assertNotNull(tensor);
      assertArrayEquals(dims, tensor.getDimensions());
      assertEquals(NnTensorType.FP32, tensor.getType());
      assertEquals(2, tensor.getRank());
      assertEquals(6, tensor.getElementCount());
      assertEquals(24, tensor.getByteSize()); // 6 * 4 bytes
      assertFalse(tensor.isNamed());
      assertNull(tensor.getName());

      float[] retrieved = tensor.toFloatArray();
      assertArrayEquals(data, retrieved, 0.001f);

      LOGGER.info("NnTensor fromFloatArray verified");
    }

    @Test
    @DisplayName("Should create named tensor")
    void shouldCreateNamedTensor() {
      LOGGER.info("Testing named NnTensor");

      int[] dims = {1, 10};
      float[] data = new float[10];
      for (int i = 0; i < 10; i++) {
        data[i] = i * 0.1f;
      }

      NnTensor tensor = NnTensor.fromFloatArray("input_tensor", dims, data);

      assertTrue(tensor.isNamed());
      assertEquals("input_tensor", tensor.getName());

      LOGGER.info("Named tensor verified");
    }

    @Test
    @DisplayName("Should create tensor from byte array (U8)")
    void shouldCreateTensorFromByteArray() {
      LOGGER.info("Testing NnTensor fromByteArray");

      int[] dims = {2, 2};
      byte[] data = {1, 2, 3, 4};

      NnTensor tensor = NnTensor.fromByteArray(dims, data);

      assertEquals(NnTensorType.U8, tensor.getType());
      assertEquals(4, tensor.getElementCount());
      assertEquals(4, tensor.getByteSize());

      byte[] retrieved = tensor.toByteArray();
      assertArrayEquals(data, retrieved);

      LOGGER.info("NnTensor fromByteArray verified");
    }

    @Test
    @DisplayName("Should create tensor from int array (I32)")
    void shouldCreateTensorFromIntArray() {
      LOGGER.info("Testing NnTensor fromIntArray");

      int[] dims = {3};
      int[] data = {100, 200, 300};

      NnTensor tensor = NnTensor.fromIntArray(dims, data);

      assertEquals(NnTensorType.I32, tensor.getType());
      assertEquals(3, tensor.getElementCount());
      assertEquals(12, tensor.getByteSize());

      int[] retrieved = tensor.toIntArray();
      assertArrayEquals(data, retrieved);

      LOGGER.info("NnTensor fromIntArray verified");
    }

    @Test
    @DisplayName("Should create tensor from long array (I64)")
    void shouldCreateTensorFromLongArray() {
      LOGGER.info("Testing NnTensor fromLongArray");

      int[] dims = {2};
      long[] data = {Long.MAX_VALUE, Long.MIN_VALUE};

      NnTensor tensor = NnTensor.fromLongArray(dims, data);

      assertEquals(NnTensorType.I64, tensor.getType());
      assertEquals(2, tensor.getElementCount());
      assertEquals(16, tensor.getByteSize());

      long[] retrieved = tensor.toLongArray();
      assertArrayEquals(data, retrieved);

      LOGGER.info("NnTensor fromLongArray verified");
    }

    @Test
    @DisplayName("Should create tensor from raw bytes")
    void shouldCreateTensorFromRawBytes() {
      LOGGER.info("Testing NnTensor fromBytes");

      int[] dims = {2};
      byte[] data = new byte[8]; // 2 FP32 values
      NnTensor tensor = NnTensor.fromBytes(dims, NnTensorType.FP32, data);

      assertEquals(NnTensorType.FP32, tensor.getType());
      assertArrayEquals(data, tensor.getData());

      LOGGER.info("NnTensor fromBytes verified");
    }

    @Test
    @DisplayName("Should reject invalid tensor creation")
    void shouldRejectInvalidTensorCreation() {
      LOGGER.info("Testing NnTensor validation");

      // Null dimensions
      assertThrows(NullPointerException.class, () -> NnTensor.fromFloatArray(null, new float[1]));

      // Null data
      assertThrows(NullPointerException.class, () -> NnTensor.fromFloatArray(new int[] {1}, null));

      // Empty dimensions
      assertThrows(
          IllegalArgumentException.class, () -> NnTensor.fromFloatArray(new int[0], new float[0]));

      // Negative dimension
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensor.fromFloatArray(new int[] {-1}, new float[1]));

      // Data size mismatch
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensor.fromFloatArray(new int[] {2, 2}, new float[3]));

      LOGGER.info("Tensor validation verified");
    }

    @Test
    @DisplayName("Should reject wrong type conversion")
    void shouldRejectWrongTypeConversion() {
      LOGGER.info("Testing NnTensor type conversion validation");

      NnTensor floatTensor = NnTensor.fromFloatArray(new int[] {2}, new float[] {1.0f, 2.0f});

      assertThrows(IllegalStateException.class, floatTensor::toByteArray);
      assertThrows(IllegalStateException.class, floatTensor::toIntArray);
      assertThrows(IllegalStateException.class, floatTensor::toLongArray);

      LOGGER.info("Type conversion validation verified");
    }

    @Test
    @DisplayName("Should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
      LOGGER.info("Testing NnTensor equals and hashCode");

      int[] dims = {2, 2};
      float[] data = {1.0f, 2.0f, 3.0f, 4.0f};

      NnTensor t1 = NnTensor.fromFloatArray(dims, data);
      NnTensor t2 = NnTensor.fromFloatArray(dims, data);
      NnTensor t3 = NnTensor.fromFloatArray(dims, new float[] {1.0f, 2.0f, 3.0f, 5.0f});

      assertEquals(t1, t2);
      assertEquals(t1.hashCode(), t2.hashCode());
      assertNotEquals(t1, t3);

      LOGGER.info("equals and hashCode verified");
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
      LOGGER.info("Testing NnTensor toString");

      NnTensor tensor = NnTensor.fromFloatArray("test", new int[] {1, 3}, new float[3]);

      String str = tensor.toString();
      assertTrue(str.contains("NnTensor"));
      assertTrue(str.contains("test"));
      assertTrue(str.contains("FP32"));

      LOGGER.info("toString verified: " + str);
    }

    @Test
    @DisplayName("Should defensively copy dimensions")
    void shouldDefensivelyCopyDimensions() {
      LOGGER.info("Testing NnTensor defensive copying");

      int[] dims = {2, 3};
      float[] data = new float[6];

      NnTensor tensor = NnTensor.fromFloatArray(dims, data);

      // Modify original
      dims[0] = 100;

      // Tensor should be unchanged
      assertEquals(2, tensor.getDimensions()[0]);

      // Modify returned dimensions
      int[] returned = tensor.getDimensions();
      returned[0] = 200;

      // Tensor should still be unchanged
      assertEquals(2, tensor.getDimensions()[0]);

      LOGGER.info("Defensive copying verified");
    }
  }

  @Nested
  @DisplayName("NnException Tests")
  class NnExceptionTests {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
      LOGGER.info("Testing NnException creation");

      NnException ex = new NnException("Test error");

      assertEquals("Test error", ex.getMessage());
      assertEquals(NnErrorCode.UNKNOWN, ex.getErrorCode());

      LOGGER.info("NnException creation verified");
    }

    @Test
    @DisplayName("Should create exception with error code")
    void shouldCreateExceptionWithErrorCode() {
      LOGGER.info("Testing NnException with error code");

      NnException ex = new NnException(NnErrorCode.TIMEOUT, "Operation timed out");

      assertTrue(ex.getMessage().contains("timeout"));
      assertEquals(NnErrorCode.TIMEOUT, ex.getErrorCode());
      assertTrue(ex.isTimeout());
      assertFalse(ex.isInvalidArgument());

      LOGGER.info("NnException with error code verified");
    }

    @Test
    @DisplayName("Should create exception with cause")
    void shouldCreateExceptionWithCause() {
      LOGGER.info("Testing NnException with cause");

      RuntimeException cause = new RuntimeException("Root cause");
      NnException ex = new NnException("Wrapper", cause);

      assertEquals(cause, ex.getCause());

      LOGGER.info("NnException with cause verified");
    }

    @Test
    @DisplayName("Should create exceptions via factory methods")
    void shouldCreateExceptionsViaFactoryMethods() {
      LOGGER.info("Testing NnException factory methods");

      assertTrue(NnException.invalidArgument("bad arg").isInvalidArgument());
      assertTrue(NnException.invalidEncoding("bad encoding").isInvalidEncoding());
      assertTrue(NnException.timeout("timed out").isTimeout());
      assertTrue(NnException.runtimeError("runtime").isRuntimeError());
      assertTrue(NnException.unsupportedOperation("not supported").isUnsupportedOperation());
      assertTrue(NnException.tooLarge("too big").isTooLarge());
      assertTrue(NnException.notFound("missing").isNotFound());
      assertTrue(NnException.security("blocked").isSecurity());

      LOGGER.info("Factory methods verified");
    }

    @Test
    @DisplayName("Should create from native error")
    void shouldCreateFromNativeError() {
      LOGGER.info("Testing NnException fromNativeError");

      NnException ex = NnException.fromNativeError(0, "Invalid argument");

      assertEquals(NnErrorCode.INVALID_ARGUMENT, ex.getErrorCode());

      LOGGER.info("fromNativeError verified");
    }
  }
}
