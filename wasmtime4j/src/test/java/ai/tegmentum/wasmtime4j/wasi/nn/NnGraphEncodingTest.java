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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NnGraphEncoding} enum.
 *
 * <p>NnGraphEncoding represents ML model encodings (formats) supported by WASI-NN per the WASI-NN
 * specification.
 */
@DisplayName("NnGraphEncoding Tests")
class NnGraphEncodingTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnGraphEncoding.class.isEnum(), "NnGraphEncoding should be an enum");
    }

    @Test
    @DisplayName("should have exactly 7 values")
    void shouldHaveExactlySevenValues() {
      final NnGraphEncoding[] values = NnGraphEncoding.values();
      assertEquals(7, values.length, "Should have exactly 7 graph encodings");
    }

    @Test
    @DisplayName("should have OPENVINO value")
    void shouldHaveOpenvinoValue() {
      assertNotNull(NnGraphEncoding.valueOf("OPENVINO"), "Should have OPENVINO");
    }

    @Test
    @DisplayName("should have ONNX value")
    void shouldHaveOnnxValue() {
      assertNotNull(NnGraphEncoding.valueOf("ONNX"), "Should have ONNX");
    }

    @Test
    @DisplayName("should have TENSORFLOW value")
    void shouldHaveTensorflowValue() {
      assertNotNull(NnGraphEncoding.valueOf("TENSORFLOW"), "Should have TENSORFLOW");
    }

    @Test
    @DisplayName("should have PYTORCH value")
    void shouldHavePytorchValue() {
      assertNotNull(NnGraphEncoding.valueOf("PYTORCH"), "Should have PYTORCH");
    }

    @Test
    @DisplayName("should have TENSORFLOWLITE value")
    void shouldHaveTensorflowliteValue() {
      assertNotNull(NnGraphEncoding.valueOf("TENSORFLOWLITE"), "Should have TENSORFLOWLITE");
    }

    @Test
    @DisplayName("should have GGML value")
    void shouldHaveGgmlValue() {
      assertNotNull(NnGraphEncoding.valueOf("GGML"), "Should have GGML");
    }

    @Test
    @DisplayName("should have AUTODETECT value")
    void shouldHaveAutodetectValue() {
      assertNotNull(NnGraphEncoding.valueOf("AUTODETECT"), "Should have AUTODETECT");
    }
  }

  @Nested
  @DisplayName("getWasiName Method Tests")
  class GetWasiNameTests {

    @Test
    @DisplayName("should return openvino for OPENVINO")
    void shouldReturnOpenvinoForOpenvino() {
      assertEquals("openvino", NnGraphEncoding.OPENVINO.getWasiName(), "OPENVINO WASI name");
    }

    @Test
    @DisplayName("should return onnx for ONNX")
    void shouldReturnOnnxForOnnx() {
      assertEquals("onnx", NnGraphEncoding.ONNX.getWasiName(), "ONNX WASI name");
    }

    @Test
    @DisplayName("should return tensorflow for TENSORFLOW")
    void shouldReturnTensorflowForTensorflow() {
      assertEquals("tensorflow", NnGraphEncoding.TENSORFLOW.getWasiName(), "TENSORFLOW WASI name");
    }

    @Test
    @DisplayName("should return pytorch for PYTORCH")
    void shouldReturnPytorchForPytorch() {
      assertEquals("pytorch", NnGraphEncoding.PYTORCH.getWasiName(), "PYTORCH WASI name");
    }

    @Test
    @DisplayName("should return tensorflowlite for TENSORFLOWLITE")
    void shouldReturnTensorflowliteForTensorflowlite() {
      assertEquals(
          "tensorflowlite",
          NnGraphEncoding.TENSORFLOWLITE.getWasiName(),
          "TENSORFLOWLITE WASI name");
    }

    @Test
    @DisplayName("should return ggml for GGML")
    void shouldReturnGgmlForGgml() {
      assertEquals("ggml", NnGraphEncoding.GGML.getWasiName(), "GGML WASI name");
    }

    @Test
    @DisplayName("should return autodetect for AUTODETECT")
    void shouldReturnAutodetectForAutodetect() {
      assertEquals("autodetect", NnGraphEncoding.AUTODETECT.getWasiName(), "AUTODETECT WASI name");
    }
  }

  @Nested
  @DisplayName("getDisplayName Method Tests")
  class GetDisplayNameTests {

    @Test
    @DisplayName("should return OpenVINO IR for OPENVINO")
    void shouldReturnOpenvinoIrForOpenvino() {
      assertEquals(
          "OpenVINO IR", NnGraphEncoding.OPENVINO.getDisplayName(), "OPENVINO display name");
    }

    @Test
    @DisplayName("should return ONNX for ONNX")
    void shouldReturnOnnxForOnnxDisplayName() {
      assertEquals("ONNX", NnGraphEncoding.ONNX.getDisplayName(), "ONNX display name");
    }

    @Test
    @DisplayName("should return TensorFlow for TENSORFLOW")
    void shouldReturnTensorFlowForTensorflow() {
      assertEquals(
          "TensorFlow", NnGraphEncoding.TENSORFLOW.getDisplayName(), "TENSORFLOW display name");
    }

    @Test
    @DisplayName("should return PyTorch for PYTORCH")
    void shouldReturnPyTorchForPytorch() {
      assertEquals("PyTorch", NnGraphEncoding.PYTORCH.getDisplayName(), "PYTORCH display name");
    }

    @Test
    @DisplayName("should return TensorFlow Lite for TENSORFLOWLITE")
    void shouldReturnTensorFlowLiteForTensorflowlite() {
      assertEquals(
          "TensorFlow Lite",
          NnGraphEncoding.TENSORFLOWLITE.getDisplayName(),
          "TENSORFLOWLITE display name");
    }

    @Test
    @DisplayName("should return GGML for GGML")
    void shouldReturnGgmlForGgmlDisplayName() {
      assertEquals("GGML", NnGraphEncoding.GGML.getDisplayName(), "GGML display name");
    }

    @Test
    @DisplayName("should return Auto-detect for AUTODETECT")
    void shouldReturnAutoDetectForAutodetect() {
      assertEquals(
          "Auto-detect", NnGraphEncoding.AUTODETECT.getDisplayName(), "AUTODETECT display name");
    }
  }

  @Nested
  @DisplayName("fromWasiName Method Tests")
  class FromWasiNameTests {

    @Test
    @DisplayName("should parse openvino")
    void shouldParseOpenvino() {
      assertEquals(
          NnGraphEncoding.OPENVINO,
          NnGraphEncoding.fromWasiName("openvino"),
          "Should parse openvino");
    }

    @Test
    @DisplayName("should parse onnx")
    void shouldParseOnnx() {
      assertEquals(NnGraphEncoding.ONNX, NnGraphEncoding.fromWasiName("onnx"), "Should parse onnx");
    }

    @Test
    @DisplayName("should parse tensorflow")
    void shouldParseTensorflow() {
      assertEquals(
          NnGraphEncoding.TENSORFLOW,
          NnGraphEncoding.fromWasiName("tensorflow"),
          "Should parse tensorflow");
    }

    @Test
    @DisplayName("should parse pytorch")
    void shouldParsePytorch() {
      assertEquals(
          NnGraphEncoding.PYTORCH, NnGraphEncoding.fromWasiName("pytorch"), "Should parse pytorch");
    }

    @Test
    @DisplayName("should parse tensorflowlite")
    void shouldParseTensorflowlite() {
      assertEquals(
          NnGraphEncoding.TENSORFLOWLITE,
          NnGraphEncoding.fromWasiName("tensorflowlite"),
          "Should parse tensorflowlite");
    }

    @Test
    @DisplayName("should parse ggml")
    void shouldParseGgml() {
      assertEquals(NnGraphEncoding.GGML, NnGraphEncoding.fromWasiName("ggml"), "Should parse ggml");
    }

    @Test
    @DisplayName("should parse autodetect")
    void shouldParseAutodetect() {
      assertEquals(
          NnGraphEncoding.AUTODETECT,
          NnGraphEncoding.fromWasiName("autodetect"),
          "Should parse autodetect");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown name")
    void shouldThrowIllegalArgumentExceptionForUnknownName() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> NnGraphEncoding.fromWasiName("unknown"));

      assertTrue(ex.getMessage().contains("Unknown"), "Exception should mention Unknown");
    }

    @Test
    @DisplayName("should be case sensitive")
    void shouldBeCaseSensitive() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnGraphEncoding.fromWasiName("ONNX"),
          "Should be case sensitive");
    }
  }

  @Nested
  @DisplayName("getNativeCode Method Tests")
  class GetNativeCodeTests {

    @Test
    @DisplayName("should return ordinal as native code")
    void shouldReturnOrdinalAsNativeCode() {
      for (final NnGraphEncoding encoding : NnGraphEncoding.values()) {
        assertEquals(
            encoding.ordinal(),
            encoding.getNativeCode(),
            encoding.name() + " native code should be ordinal");
      }
    }

    @Test
    @DisplayName("should have unique native codes")
    void shouldHaveUniqueNativeCodes() {
      final Set<Integer> codes = new HashSet<>();
      for (final NnGraphEncoding encoding : NnGraphEncoding.values()) {
        assertTrue(
            codes.add(encoding.getNativeCode()), "Native code should be unique: " + encoding);
      }
    }
  }

  @Nested
  @DisplayName("fromNativeCode Method Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("should parse all valid codes")
    void shouldParseAllValidCodes() {
      for (final NnGraphEncoding expected : NnGraphEncoding.values()) {
        final NnGraphEncoding actual = NnGraphEncoding.fromNativeCode(expected.ordinal());
        assertEquals(expected, actual, "Should parse code " + expected.ordinal());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative code")
    void shouldThrowIllegalArgumentExceptionForNegativeCode() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> NnGraphEncoding.fromNativeCode(-1));

      assertTrue(ex.getMessage().contains("Invalid"), "Exception should mention Invalid");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for code out of range")
    void shouldThrowIllegalArgumentExceptionForCodeOutOfRange() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> NnGraphEncoding.fromNativeCode(100));

      assertTrue(ex.getMessage().contains("Invalid"), "Exception should mention Invalid");
    }

    @Test
    @DisplayName("should round trip from encoding to code and back")
    void shouldRoundTripFromEncodingToCodeAndBack() {
      for (final NnGraphEncoding original : NnGraphEncoding.values()) {
        final int code = original.getNativeCode();
        final NnGraphEncoding roundTripped = NnGraphEncoding.fromNativeCode(code);
        assertEquals(original, roundTripped, "Should round trip: " + original);
      }
    }
  }

  @Nested
  @DisplayName("toString Method Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return display name")
    void shouldReturnDisplayName() {
      for (final NnGraphEncoding encoding : NnGraphEncoding.values()) {
        assertEquals(
            encoding.getDisplayName(),
            encoding.toString(),
            encoding.name() + " toString should return display name");
      }
    }

    @Test
    @DisplayName("should return human-readable string")
    void shouldReturnHumanReadableString() {
      assertEquals("OpenVINO IR", NnGraphEncoding.OPENVINO.toString(), "OPENVINO toString");
      assertEquals("ONNX", NnGraphEncoding.ONNX.toString(), "ONNX toString");
      assertEquals("TensorFlow", NnGraphEncoding.TENSORFLOW.toString(), "TENSORFLOW toString");
    }
  }

  @Nested
  @DisplayName("WASI-NN Specification Compliance Tests")
  class WasiNnSpecificationComplianceTests {

    @Test
    @DisplayName("should cover all WASI-NN graph encodings")
    void shouldCoverAllWasiNnGraphEncodings() {
      // Per WASI-NN specification: graph_encoding enum
      final String[] expectedEncodings = {
        "openvino", "onnx", "tensorflow", "pytorch", "tensorflowlite", "ggml", "autodetect"
      };

      for (final String expectedName : expectedEncodings) {
        assertNotNull(
            NnGraphEncoding.fromWasiName(expectedName), "Should have encoding: " + expectedName);
      }

      assertEquals(
          expectedEncodings.length,
          NnGraphEncoding.values().length,
          "Should have exact count of encodings");
    }

    @Test
    @DisplayName("should represent common ML model formats")
    void shouldRepresentCommonMlModelFormats() {
      // Verify key ML frameworks are represented
      assertNotNull(NnGraphEncoding.ONNX, "ONNX is standard interchange format");
      assertNotNull(NnGraphEncoding.TENSORFLOW, "TensorFlow is major framework");
      assertNotNull(NnGraphEncoding.PYTORCH, "PyTorch is major framework");
      assertNotNull(NnGraphEncoding.TENSORFLOWLITE, "TFLite for mobile/embedded");
      assertNotNull(NnGraphEncoding.OPENVINO, "OpenVINO for Intel optimization");
      assertNotNull(NnGraphEncoding.GGML, "GGML for LLM inference");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support model format selection")
    void shouldSupportModelFormatSelection() {
      final String modelPath = "model.onnx";
      final NnGraphEncoding encoding;

      if (modelPath.endsWith(".onnx")) {
        encoding = NnGraphEncoding.ONNX;
      } else if (modelPath.endsWith(".pb")) {
        encoding = NnGraphEncoding.TENSORFLOW;
      } else {
        encoding = NnGraphEncoding.AUTODETECT;
      }

      assertEquals(NnGraphEncoding.ONNX, encoding, "Should select ONNX for .onnx file");
    }

    @Test
    @DisplayName("should support autodetect fallback")
    void shouldSupportAutodetectFallback() {
      final NnGraphEncoding encoding = NnGraphEncoding.AUTODETECT;

      assertEquals("autodetect", encoding.getWasiName(), "AUTODETECT WASI name");
      assertEquals("Auto-detect", encoding.getDisplayName(), "AUTODETECT display name");
    }

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final NnGraphEncoding encoding = NnGraphEncoding.ONNX;

      final String fileExtension;
      switch (encoding) {
        case ONNX:
          fileExtension = ".onnx";
          break;
        case TENSORFLOW:
          fileExtension = ".pb";
          break;
        case PYTORCH:
          fileExtension = ".pt";
          break;
        case TENSORFLOWLITE:
          fileExtension = ".tflite";
          break;
        case GGML:
          fileExtension = ".ggml";
          break;
        default:
          fileExtension = "";
      }

      assertEquals(".onnx", fileExtension, "ONNX extension should be .onnx");
    }
  }
}
