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
package ai.tegmentum.wasmtime4j.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensor;
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensorType;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WASI-NN (WebAssembly System Interface for Neural Networks).
 *
 * <p>This test suite validates the WASI-NN implementation including:
 *
 * <ul>
 *   <li>NnContext creation and availability
 *   <li>NnTensor creation and manipulation
 *   <li>Encoding and target support queries
 *   <li>Error handling and validation
 *   <li>Resource lifecycle management
 * </ul>
 *
 * <p>Note: WASI-NN is an experimental feature (Tier 3) that requires specific ML backends (like
 * ONNX Runtime, OpenVINO, or WinML) to be available. Tests that require actual inference are
 * conditionally enabled based on backend availability.
 */
public class WasiNnTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiNnTest.class.getName());

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  // ==================== Tensor Tests (pure Java, no native calls) ====================

  @Test
  void testTensorCreationFromFloatArray() {
    LOGGER.info("Testing NnTensor creation from float array");

    int[] dimensions = {1, 3, 4};
    float[] data = new float[12];
    for (int i = 0; i < data.length; i++) {
      data[i] = (float) i * 0.1f;
    }

    NnTensor tensor = NnTensor.fromFloatArray(dimensions, data);

    assertNotNull(tensor, "Tensor should be created");
    assertEquals(NnTensorType.FP32, tensor.getType(), "Tensor type should be FP32");
    assertArrayEquals(dimensions, tensor.getDimensions(), "Dimensions should match");
    assertEquals(3, tensor.getRank(), "Rank should be 3");
    assertEquals(12, tensor.getElementCount(), "Element count should be 12");
    assertEquals(48, tensor.getByteSize(), "Byte size should be 48 (12 * 4 bytes)");
    assertFalse(tensor.isNamed(), "Tensor should not be named");

    float[] retrieved = tensor.toFloatArray();
    assertArrayEquals(data, retrieved, 0.0001f, "Retrieved float data should match");

    LOGGER.info("Successfully created and validated FP32 tensor");
  }

  @Test
  void testNamedTensorCreation() {
    LOGGER.info("Testing named NnTensor creation");

    String tensorName = "input_tensor";
    int[] dimensions = {2, 2};
    float[] data = {1.0f, 2.0f, 3.0f, 4.0f};

    NnTensor tensor = NnTensor.fromFloatArray(tensorName, dimensions, data);

    assertNotNull(tensor, "Named tensor should be created");
    assertTrue(tensor.isNamed(), "Tensor should be named");
    assertEquals(tensorName, tensor.getName(), "Tensor name should match");

    LOGGER.info("Successfully created named tensor: " + tensor);
  }

  @Test
  void testTensorCreationFromByteArray() {
    LOGGER.info("Testing NnTensor creation from byte array");

    int[] dimensions = {4, 4};
    byte[] data = new byte[16];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) i;
    }

    NnTensor tensor = NnTensor.fromByteArray(dimensions, data);

    assertNotNull(tensor, "Tensor should be created");
    assertEquals(NnTensorType.U8, tensor.getType(), "Tensor type should be U8");
    assertEquals(16, tensor.getElementCount(), "Element count should be 16");
    assertEquals(16, tensor.getByteSize(), "Byte size should be 16");

    byte[] retrieved = tensor.toByteArray();
    assertArrayEquals(data, retrieved, "Retrieved byte data should match");

    LOGGER.info("Successfully created and validated U8 tensor");
  }

  @Test
  void testTensorCreationFromIntArray() {
    LOGGER.info("Testing NnTensor creation from int array");

    int[] dimensions = {2, 3};
    int[] data = {1, 2, 3, 4, 5, 6};

    NnTensor tensor = NnTensor.fromIntArray(dimensions, data);

    assertNotNull(tensor, "Tensor should be created");
    assertEquals(NnTensorType.I32, tensor.getType(), "Tensor type should be I32");
    assertEquals(6, tensor.getElementCount(), "Element count should be 6");
    assertEquals(24, tensor.getByteSize(), "Byte size should be 24 (6 * 4 bytes)");

    int[] retrieved = tensor.toIntArray();
    assertArrayEquals(data, retrieved, "Retrieved int data should match");

    LOGGER.info("Successfully created and validated I32 tensor");
  }

  @Test
  void testTensorCreationFromLongArray() {
    LOGGER.info("Testing NnTensor creation from long array");

    int[] dimensions = {3};
    long[] data = {100L, 200L, 300L};

    NnTensor tensor = NnTensor.fromLongArray(dimensions, data);

    assertNotNull(tensor, "Tensor should be created");
    assertEquals(NnTensorType.I64, tensor.getType(), "Tensor type should be I64");
    assertEquals(3, tensor.getElementCount(), "Element count should be 3");
    assertEquals(24, tensor.getByteSize(), "Byte size should be 24 (3 * 8 bytes)");

    long[] retrieved = tensor.toLongArray();
    assertArrayEquals(data, retrieved, "Retrieved long data should match");

    LOGGER.info("Successfully created and validated I64 tensor");
  }

  @Test
  void testTensorCreationFromBytes() {
    LOGGER.info("Testing NnTensor creation from raw bytes");

    int[] dimensions = {2, 2};
    byte[] rawData = new byte[16]; // 4 floats * 4 bytes
    NnTensorType type = NnTensorType.FP32;

    NnTensor tensor = NnTensor.fromBytes(dimensions, type, rawData);

    assertNotNull(tensor, "Tensor should be created");
    assertEquals(type, tensor.getType(), "Tensor type should match");
    assertArrayEquals(dimensions, tensor.getDimensions(), "Dimensions should match");

    LOGGER.info("Successfully created tensor from raw bytes");
  }

  @Test
  void testTensorInvalidDimensions() {
    LOGGER.info("Testing tensor validation for invalid dimensions");

    // Test empty dimensions
    assertThrows(
        IllegalArgumentException.class,
        () -> NnTensor.fromFloatArray(new int[0], new float[1]),
        "Empty dimensions should throw");

    // Test negative dimensions
    assertThrows(
        IllegalArgumentException.class,
        () -> NnTensor.fromFloatArray(new int[] {-1, 2}, new float[2]),
        "Negative dimensions should throw");

    LOGGER.info("Successfully validated dimension error handling");
  }

  @Test
  void testTensorDataSizeMismatch() {
    LOGGER.info("Testing tensor validation for data size mismatch");

    int[] dimensions = {2, 3}; // expects 6 elements

    assertThrows(
        IllegalArgumentException.class,
        () -> NnTensor.fromFloatArray(dimensions, new float[5]),
        "Data size mismatch should throw (too few)");

    assertThrows(
        IllegalArgumentException.class,
        () -> NnTensor.fromFloatArray(dimensions, new float[7]),
        "Data size mismatch should throw (too many)");

    LOGGER.info("Successfully validated data size error handling");
  }

  @Test
  void testTensorTypeConversionErrors() {
    LOGGER.info("Testing tensor type conversion errors");

    NnTensor floatTensor = NnTensor.fromFloatArray(new int[] {2}, new float[] {1.0f, 2.0f});

    assertThrows(
        IllegalStateException.class,
        floatTensor::toByteArray,
        "Converting FP32 to byte array should throw");

    assertThrows(
        IllegalStateException.class,
        floatTensor::toIntArray,
        "Converting FP32 to int array should throw");

    assertThrows(
        IllegalStateException.class,
        floatTensor::toLongArray,
        "Converting FP32 to long array should throw");

    LOGGER.info("Successfully validated type conversion error handling");
  }

  @Test
  void testTensorEquality() {
    LOGGER.info("Testing tensor equality");

    int[] dimensions = {2, 2};
    float[] data = {1.0f, 2.0f, 3.0f, 4.0f};

    NnTensor tensor1 = NnTensor.fromFloatArray(dimensions, data);
    NnTensor tensor2 = NnTensor.fromFloatArray(dimensions, data);
    NnTensor tensor3 = NnTensor.fromFloatArray(dimensions, new float[] {1.0f, 2.0f, 3.0f, 5.0f});

    assertEquals(tensor1, tensor2, "Identical tensors should be equal");
    assertNotEquals(tensor1, tensor3, "Different tensors should not be equal");
    assertEquals(tensor1.hashCode(), tensor2.hashCode(), "Equal tensors should have same hash");

    LOGGER.info("Successfully validated tensor equality");
  }

  // ==================== TensorType Tests (pure Java, no native calls) ====================

  @Test
  void testTensorTypeProperties() {
    LOGGER.info("Testing NnTensorType properties");

    assertEquals(2, NnTensorType.FP16.getByteSize(), "FP16 should be 2 bytes");
    assertEquals(4, NnTensorType.FP32.getByteSize(), "FP32 should be 4 bytes");
    assertEquals(8, NnTensorType.FP64.getByteSize(), "FP64 should be 8 bytes");
    assertEquals(2, NnTensorType.BF16.getByteSize(), "BF16 should be 2 bytes");
    assertEquals(1, NnTensorType.U8.getByteSize(), "U8 should be 1 byte");
    assertEquals(4, NnTensorType.I32.getByteSize(), "I32 should be 4 bytes");
    assertEquals(8, NnTensorType.I64.getByteSize(), "I64 should be 8 bytes");

    assertEquals("fp32", NnTensorType.FP32.getWasiName(), "FP32 WASI name should be 'fp32'");
    assertEquals("u8", NnTensorType.U8.getWasiName(), "U8 WASI name should be 'u8'");

    LOGGER.info("Successfully validated tensor type properties");
  }

  @Test
  void testTensorTypeByteSizeCalculation() {
    LOGGER.info("Testing NnTensorType byte size calculation");

    int[] dimensions = {2, 3, 4};
    long expectedElements = 2L * 3 * 4;

    assertEquals(
        expectedElements * 4, NnTensorType.FP32.calculateByteSize(dimensions), "FP32 byte size");
    assertEquals(
        expectedElements * 8, NnTensorType.FP64.calculateByteSize(dimensions), "FP64 byte size");
    assertEquals(
        expectedElements * 1, NnTensorType.U8.calculateByteSize(dimensions), "U8 byte size");

    LOGGER.info("Successfully validated byte size calculation");
  }

  @Test
  void testTensorTypeFromWasiName() {
    LOGGER.info("Testing NnTensorType parsing from WASI name");

    assertEquals(NnTensorType.FP32, NnTensorType.fromWasiName("fp32"));
    assertEquals(NnTensorType.FP16, NnTensorType.fromWasiName("fp16"));
    assertEquals(NnTensorType.U8, NnTensorType.fromWasiName("u8"));
    assertEquals(NnTensorType.I64, NnTensorType.fromWasiName("i64"));

    assertThrows(
        IllegalArgumentException.class,
        () -> NnTensorType.fromWasiName("unknown"),
        "Unknown WASI name should throw");

    LOGGER.info("Successfully validated WASI name parsing");
  }

  @Test
  void testTensorTypeFromNativeCode() {
    LOGGER.info("Testing NnTensorType parsing from native code");

    for (NnTensorType type : NnTensorType.values()) {
      assertEquals(type, NnTensorType.fromNativeCode(type.getNativeCode()));
    }

    assertThrows(
        IllegalArgumentException.class,
        () -> NnTensorType.fromNativeCode(-1),
        "Negative code should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> NnTensorType.fromNativeCode(100),
        "Invalid code should throw");

    LOGGER.info("Successfully validated native code parsing");
  }

  // ==================== NnContext Tests (native calls, dual-runtime) ====================

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNnAvailabilityCheck(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WASI-NN availability check");

    boolean nnAvailable = isNnAvailableForRuntime(runtime);
    LOGGER.info("[" + runtime + "] WASI-NN availability: " + nnAvailable);

    // This test verifies the availability check works without throwing
    // The actual availability depends on the Wasmtime build and ML backend configuration
    assertNotNull(Boolean.valueOf(nnAvailable), "Availability check should return a boolean");
  }

  @EnabledIf("isNnAvailable")
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNnContextCreation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing NnContext creation");

    try (WasmRuntime rt = WasmRuntimeFactory.create(runtime);
        NnContext context = rt.createNnContext()) {
      assertNotNull(context, "NnContext should be created");
      assertTrue(context.isAvailable(), "Context should be available");
      assertTrue(context.isValid(), "Context should be valid");

      LOGGER.info("[" + runtime + "] Successfully created NnContext");
    }
  }

  @EnabledIf("isNnAvailable")
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNnContextSupportedEncodings(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing NnContext supported encodings query");

    try (WasmRuntime rt = WasmRuntimeFactory.create(runtime);
        NnContext context = rt.createNnContext()) {
      Set<NnGraphEncoding> supportedEncodings = context.getSupportedEncodings();
      assertNotNull(supportedEncodings, "Supported encodings should not be null");

      LOGGER.info("[" + runtime + "] Supported encodings: " + supportedEncodings);

      for (NnGraphEncoding encoding : supportedEncodings) {
        assertTrue(
            context.isEncodingSupported(encoding),
            "isEncodingSupported should return true for " + encoding);
      }
    }
  }

  @EnabledIf("isNnAvailable")
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNnContextSupportedTargets(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing NnContext supported targets query");

    try (WasmRuntime rt = WasmRuntimeFactory.create(runtime);
        NnContext context = rt.createNnContext()) {
      Set<NnExecutionTarget> supportedTargets = context.getSupportedTargets();
      assertNotNull(supportedTargets, "Supported targets should not be null");

      LOGGER.info("[" + runtime + "] Supported targets: " + supportedTargets);

      for (NnExecutionTarget target : supportedTargets) {
        assertTrue(
            context.isTargetSupported(target),
            "isTargetSupported should return true for " + target);
      }
    }
  }

  @EnabledIf("isNnAvailable")
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNnContextImplementationInfo(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing NnContext implementation info");

    try (WasmRuntime rt = WasmRuntimeFactory.create(runtime);
        NnContext context = rt.createNnContext()) {
      NnContext.NnImplementationInfo info = context.getImplementationInfo();
      assertNotNull(info, "Implementation info should not be null");

      LOGGER.info("[" + runtime + "] Implementation version: " + info.getVersion());
      LOGGER.info("[" + runtime + "] Available backends: " + info.getBackends());
      LOGGER.info("[" + runtime + "] Default backend: " + info.getDefaultBackend());
    }
  }

  @EnabledIf("isNnAvailable")
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNnContextLifecycle(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing NnContext lifecycle");

    try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
      NnContext context = rt.createNnContext();
      assertNotNull(context, "Context should be created");
      assertTrue(context.isValid(), "Context should be valid after creation");

      context.close();
      assertFalse(context.isValid(), "Context should be invalid after close");

      // Double close should not throw
      assertDoesNotThrow(context::close, "Double close should not throw");

      LOGGER.info("[" + runtime + "] Successfully validated context lifecycle");
    }
  }

  @EnabledIf("isNnAvailable")
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNnContextMultipleCreation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multiple NnContext creation");

    try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
      for (int i = 0; i < 5; i++) {
        try (NnContext context = rt.createNnContext()) {
          assertNotNull(context, "Context " + i + " should be created");
          assertTrue(context.isValid(), "Context " + i + " should be valid");
          LOGGER.fine("[" + runtime + "] Created context iteration: " + i);
        }
      }
    }

    LOGGER.info("[" + runtime + "] Successfully created and closed multiple contexts");
  }

  @EnabledIf("isNnAvailable")
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNnGraphLoadingWithInvalidData(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing NnGraph loading with invalid data");

    try (WasmRuntime rt = WasmRuntimeFactory.create(runtime);
        NnContext context = rt.createNnContext()) {
      // Empty model data should fail
      assertThrows(
          Exception.class,
          () -> context.loadGraph(new byte[0], NnGraphEncoding.ONNX, NnExecutionTarget.CPU),
          "Empty model data should throw");

      // Invalid model data should fail
      byte[] invalidData = {0x00, 0x01, 0x02, 0x03};
      assertThrows(
          Exception.class,
          () -> context.loadGraph(invalidData, NnGraphEncoding.ONNX, NnExecutionTarget.CPU),
          "Invalid model data should throw");

      LOGGER.info("[" + runtime + "] Successfully validated graph loading error handling");
    }
  }

  @EnabledIf("isNnAvailable")
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNnGraphLoadingNullParameters(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing NnGraph loading with null parameters");

    try (WasmRuntime rt = WasmRuntimeFactory.create(runtime);
        NnContext context = rt.createNnContext()) {
      assertThrows(
          NullPointerException.class,
          () -> context.loadGraph((byte[]) null, NnGraphEncoding.ONNX, NnExecutionTarget.CPU),
          "Null model data should throw NullPointerException");

      assertThrows(
          NullPointerException.class,
          () -> context.loadGraph(new byte[] {1, 2, 3}, null, NnExecutionTarget.CPU),
          "Null encoding should throw NullPointerException");

      assertThrows(
          NullPointerException.class,
          () -> context.loadGraph(new byte[] {1, 2, 3}, NnGraphEncoding.ONNX, null),
          "Null target should throw NullPointerException");

      LOGGER.info("[" + runtime + "] Successfully validated null parameter handling");
    }
  }

  // ==================== Encoding and Target Enum Tests (pure Java) ====================

  @Test
  void testNnGraphEncodingValues() {
    LOGGER.info("Testing NnGraphEncoding enum values");

    NnGraphEncoding[] encodings = NnGraphEncoding.values();
    assertTrue(encodings.length > 0, "Should have at least one encoding");

    // Verify ONNX is present as it's the most common format
    boolean hasOnnx = false;
    for (NnGraphEncoding encoding : encodings) {
      assertNotNull(encoding.name(), "Encoding name should not be null");
      if (encoding == NnGraphEncoding.ONNX) {
        hasOnnx = true;
      }
      LOGGER.fine("Encoding: " + encoding.name());
    }
    assertTrue(hasOnnx, "ONNX encoding should be present");

    LOGGER.info("Found " + encodings.length + " graph encodings");
  }

  @Test
  void testNnExecutionTargetValues() {
    LOGGER.info("Testing NnExecutionTarget enum values");

    NnExecutionTarget[] targets = NnExecutionTarget.values();
    assertTrue(targets.length > 0, "Should have at least one target");

    // Verify CPU is present as it's always available
    boolean hasCpu = false;
    for (NnExecutionTarget target : targets) {
      assertNotNull(target.name(), "Target name should not be null");
      if (target == NnExecutionTarget.CPU) {
        hasCpu = true;
      }
      LOGGER.fine("Target: " + target.name());
    }
    assertTrue(hasCpu, "CPU target should be present");

    LOGGER.info("Found " + targets.length + " execution targets");
  }

  // ==================== Helper Methods ====================

  /**
   * Checks if WASI-NN is available for a specific runtime type.
   *
   * @param runtime the runtime type to check
   * @return true if WASI-NN is available
   */
  private static boolean isNnAvailableForRuntime(final RuntimeType runtime) {
    try {
      try (WasmRuntime testRuntime = WasmRuntimeFactory.create(runtime)) {
        return testRuntime.isNnAvailable();
      }
    } catch (final Exception e) {
      return false;
    }
  }

  /** Checks if WASI-NN is available in any runtime. Used by @EnabledIf annotations. */
  static boolean isNnAvailable() {
    try {
      // Check if JNI NN classes are available (native implementation exists)
      Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.nn.JniNnContext");
      Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.nn.JniNnContextFactory");

      // Also try to actually call the native method to verify it's implemented
      WasmRuntime testRuntime = WasmRuntimeFactory.create();
      boolean available = testRuntime.isNnAvailable();
      testRuntime.close();
      return available;
    } catch (final ClassNotFoundException e) {
      return false;
    } catch (final ExceptionInInitializerError e) {
      // Native library load failed
      return false;
    } catch (final UnsatisfiedLinkError e) {
      // Native method not implemented
      return false;
    } catch (final Exception e) {
      // Any other error (including WasmException)
      return false;
    }
  }
}
