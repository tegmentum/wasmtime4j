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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link NnGraphEncoding} enum.
 *
 * <p>Verifies WASI-NN graph encoding values, name mappings, native codes, and string
 * representations.
 */
@DisplayName("NnGraphEncoding Tests")
class NnGraphEncodingTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("NnGraphEncoding should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnGraphEncoding.class.isEnum(), "NnGraphEncoding should be an enum");
    }

    @Test
    @DisplayName("NnGraphEncoding should have exactly 7 values")
    void shouldHaveExactlySevenValues() {
      assertEquals(
          7, NnGraphEncoding.values().length, "Should have exactly 7 graph encoding values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have OPENVINO value")
    void shouldHaveOpenvinoValue() {
      assertNotNull(NnGraphEncoding.OPENVINO, "OPENVINO should exist");
      assertEquals("OPENVINO", NnGraphEncoding.OPENVINO.name(), "Name should be OPENVINO");
    }

    @Test
    @DisplayName("should have ONNX value")
    void shouldHaveOnnxValue() {
      assertNotNull(NnGraphEncoding.ONNX, "ONNX should exist");
      assertEquals("ONNX", NnGraphEncoding.ONNX.name(), "Name should be ONNX");
    }

    @Test
    @DisplayName("should have TENSORFLOW value")
    void shouldHaveTensorflowValue() {
      assertNotNull(NnGraphEncoding.TENSORFLOW, "TENSORFLOW should exist");
      assertEquals("TENSORFLOW", NnGraphEncoding.TENSORFLOW.name(), "Name should be TENSORFLOW");
    }

    @Test
    @DisplayName("should have PYTORCH value")
    void shouldHavePytorchValue() {
      assertNotNull(NnGraphEncoding.PYTORCH, "PYTORCH should exist");
      assertEquals("PYTORCH", NnGraphEncoding.PYTORCH.name(), "Name should be PYTORCH");
    }

    @Test
    @DisplayName("should have TENSORFLOWLITE value")
    void shouldHaveTensorflowliteValue() {
      assertNotNull(NnGraphEncoding.TENSORFLOWLITE, "TENSORFLOWLITE should exist");
      assertEquals(
          "TENSORFLOWLITE", NnGraphEncoding.TENSORFLOWLITE.name(), "Name should be TENSORFLOWLITE");
    }

    @Test
    @DisplayName("should have GGML value")
    void shouldHaveGgmlValue() {
      assertNotNull(NnGraphEncoding.GGML, "GGML should exist");
      assertEquals("GGML", NnGraphEncoding.GGML.name(), "Name should be GGML");
    }

    @Test
    @DisplayName("should have AUTODETECT value")
    void shouldHaveAutodetectValue() {
      assertNotNull(NnGraphEncoding.AUTODETECT, "AUTODETECT should exist");
      assertEquals("AUTODETECT", NnGraphEncoding.AUTODETECT.name(), "Name should be AUTODETECT");
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          NnGraphEncoding.OPENVINO, NnGraphEncoding.valueOf("OPENVINO"), "Should return OPENVINO");
      assertEquals(NnGraphEncoding.ONNX, NnGraphEncoding.valueOf("ONNX"), "Should return ONNX");
      assertEquals(
          NnGraphEncoding.TENSORFLOW,
          NnGraphEncoding.valueOf("TENSORFLOW"),
          "Should return TENSORFLOW");
      assertEquals(
          NnGraphEncoding.PYTORCH, NnGraphEncoding.valueOf("PYTORCH"), "Should return PYTORCH");
      assertEquals(
          NnGraphEncoding.TENSORFLOWLITE,
          NnGraphEncoding.valueOf("TENSORFLOWLITE"),
          "Should return TENSORFLOWLITE");
      assertEquals(NnGraphEncoding.GGML, NnGraphEncoding.valueOf("GGML"), "Should return GGML");
      assertEquals(
          NnGraphEncoding.AUTODETECT,
          NnGraphEncoding.valueOf("AUTODETECT"),
          "Should return AUTODETECT");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnGraphEncoding.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final NnGraphEncoding[] values = NnGraphEncoding.values();
      final Set<NnGraphEncoding> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(NnGraphEncoding.OPENVINO), "Should contain OPENVINO");
      assertTrue(valueSet.contains(NnGraphEncoding.ONNX), "Should contain ONNX");
      assertTrue(valueSet.contains(NnGraphEncoding.TENSORFLOW), "Should contain TENSORFLOW");
      assertTrue(valueSet.contains(NnGraphEncoding.PYTORCH), "Should contain PYTORCH");
      assertTrue(
          valueSet.contains(NnGraphEncoding.TENSORFLOWLITE), "Should contain TENSORFLOWLITE");
      assertTrue(valueSet.contains(NnGraphEncoding.GGML), "Should contain GGML");
      assertTrue(valueSet.contains(NnGraphEncoding.AUTODETECT), "Should contain AUTODETECT");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final NnGraphEncoding[] first = NnGraphEncoding.values();
      final NnGraphEncoding[] second = NnGraphEncoding.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("GetWasiName Tests")
  class GetWasiNameTests {

    @Test
    @DisplayName("OPENVINO should have wasi name 'openvino'")
    void openvinoShouldHaveCorrectWasiName() {
      assertEquals(
          "openvino",
          NnGraphEncoding.OPENVINO.getWasiName(),
          "OPENVINO wasi name should be 'openvino'");
    }

    @Test
    @DisplayName("ONNX should have wasi name 'onnx'")
    void onnxShouldHaveCorrectWasiName() {
      assertEquals("onnx", NnGraphEncoding.ONNX.getWasiName(), "ONNX wasi name should be 'onnx'");
    }

    @Test
    @DisplayName("TENSORFLOW should have wasi name 'tensorflow'")
    void tensorflowShouldHaveCorrectWasiName() {
      assertEquals(
          "tensorflow",
          NnGraphEncoding.TENSORFLOW.getWasiName(),
          "TENSORFLOW wasi name should be 'tensorflow'");
    }

    @Test
    @DisplayName("PYTORCH should have wasi name 'pytorch'")
    void pytorchShouldHaveCorrectWasiName() {
      assertEquals(
          "pytorch",
          NnGraphEncoding.PYTORCH.getWasiName(),
          "PYTORCH wasi name should be 'pytorch'");
    }

    @Test
    @DisplayName("TENSORFLOWLITE should have wasi name 'tensorflowlite'")
    void tensorflowliteShouldHaveCorrectWasiName() {
      assertEquals(
          "tensorflowlite",
          NnGraphEncoding.TENSORFLOWLITE.getWasiName(),
          "TENSORFLOWLITE wasi name should be 'tensorflowlite'");
    }

    @Test
    @DisplayName("GGML should have wasi name 'ggml'")
    void ggmlShouldHaveCorrectWasiName() {
      assertEquals("ggml", NnGraphEncoding.GGML.getWasiName(), "GGML wasi name should be 'ggml'");
    }

    @Test
    @DisplayName("AUTODETECT should have wasi name 'autodetect'")
    void autodetectShouldHaveCorrectWasiName() {
      assertEquals(
          "autodetect",
          NnGraphEncoding.AUTODETECT.getWasiName(),
          "AUTODETECT wasi name should be 'autodetect'");
    }
  }

  @Nested
  @DisplayName("GetDisplayName Tests")
  class GetDisplayNameTests {

    @Test
    @DisplayName("OPENVINO should have display name 'OpenVINO IR'")
    void openvinoShouldHaveCorrectDisplayName() {
      assertEquals(
          "OpenVINO IR",
          NnGraphEncoding.OPENVINO.getDisplayName(),
          "OPENVINO display name should be 'OpenVINO IR'");
    }

    @Test
    @DisplayName("ONNX should have display name 'ONNX'")
    void onnxShouldHaveCorrectDisplayName() {
      assertEquals(
          "ONNX", NnGraphEncoding.ONNX.getDisplayName(), "ONNX display name should be 'ONNX'");
    }

    @Test
    @DisplayName("TENSORFLOW should have display name 'TensorFlow'")
    void tensorflowShouldHaveCorrectDisplayName() {
      assertEquals(
          "TensorFlow",
          NnGraphEncoding.TENSORFLOW.getDisplayName(),
          "TENSORFLOW display name should be 'TensorFlow'");
    }

    @Test
    @DisplayName("PYTORCH should have display name 'PyTorch'")
    void pytorchShouldHaveCorrectDisplayName() {
      assertEquals(
          "PyTorch",
          NnGraphEncoding.PYTORCH.getDisplayName(),
          "PYTORCH display name should be 'PyTorch'");
    }

    @Test
    @DisplayName("TENSORFLOWLITE should have display name 'TensorFlow Lite'")
    void tensorflowliteShouldHaveCorrectDisplayName() {
      assertEquals(
          "TensorFlow Lite",
          NnGraphEncoding.TENSORFLOWLITE.getDisplayName(),
          "TENSORFLOWLITE display name should be 'TensorFlow Lite'");
    }

    @Test
    @DisplayName("GGML should have display name 'GGML'")
    void ggmlShouldHaveCorrectDisplayName() {
      assertEquals(
          "GGML", NnGraphEncoding.GGML.getDisplayName(), "GGML display name should be 'GGML'");
    }

    @Test
    @DisplayName("AUTODETECT should have display name 'Auto-detect'")
    void autodetectShouldHaveCorrectDisplayName() {
      assertEquals(
          "Auto-detect",
          NnGraphEncoding.AUTODETECT.getDisplayName(),
          "AUTODETECT display name should be 'Auto-detect'");
    }
  }

  @Nested
  @DisplayName("FromWasiName Tests")
  class FromWasiNameTests {

    @Test
    @DisplayName("should resolve valid wasi names to correct constants")
    void shouldResolveValidWasiNames() {
      assertEquals(
          NnGraphEncoding.OPENVINO,
          NnGraphEncoding.fromWasiName("openvino"),
          "Should resolve 'openvino'");
      assertEquals(
          NnGraphEncoding.ONNX, NnGraphEncoding.fromWasiName("onnx"), "Should resolve 'onnx'");
      assertEquals(
          NnGraphEncoding.TENSORFLOW,
          NnGraphEncoding.fromWasiName("tensorflow"),
          "Should resolve 'tensorflow'");
      assertEquals(
          NnGraphEncoding.PYTORCH,
          NnGraphEncoding.fromWasiName("pytorch"),
          "Should resolve 'pytorch'");
      assertEquals(
          NnGraphEncoding.TENSORFLOWLITE,
          NnGraphEncoding.fromWasiName("tensorflowlite"),
          "Should resolve 'tensorflowlite'");
      assertEquals(
          NnGraphEncoding.GGML, NnGraphEncoding.fromWasiName("ggml"), "Should resolve 'ggml'");
      assertEquals(
          NnGraphEncoding.AUTODETECT,
          NnGraphEncoding.fromWasiName("autodetect"),
          "Should resolve 'autodetect'");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid wasi name")
    void shouldThrowForInvalidWasiName() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> NnGraphEncoding.fromWasiName("caffe"),
              "Should throw for invalid wasi name");
      assertTrue(
          exception.getMessage().contains("caffe"),
          "Exception message should mention the invalid name: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("GetNativeCode Tests")
  class GetNativeCodeTests {

    @Test
    @DisplayName("getNativeCode should return ordinal for each constant")
    void getNativeCodeShouldReturnOrdinal() {
      for (final NnGraphEncoding encoding : NnGraphEncoding.values()) {
        assertEquals(
            encoding.ordinal(),
            encoding.getNativeCode(),
            "getNativeCode() should return ordinal() for " + encoding);
      }
    }
  }

  @Nested
  @DisplayName("FromNativeCode Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("should resolve valid native codes to correct constants")
    void shouldResolveValidNativeCodes() {
      for (final NnGraphEncoding encoding : NnGraphEncoding.values()) {
        assertSame(
            encoding,
            NnGraphEncoding.fromNativeCode(encoding.getNativeCode()),
            "Should resolve native code " + encoding.getNativeCode() + " to " + encoding);
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid native code")
    void shouldThrowForInvalidNativeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnGraphEncoding.fromNativeCode(-1),
          "Should throw for negative native code");
      assertThrows(
          IllegalArgumentException.class,
          () -> NnGraphEncoding.fromNativeCode(7),
          "Should throw for out-of-range native code 7");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return displayName")
    void toStringShouldReturnDisplayName() {
      for (final NnGraphEncoding encoding : NnGraphEncoding.values()) {
        assertEquals(
            encoding.getDisplayName(),
            encoding.toString(),
            "toString() for " + encoding.name() + " should return displayName");
      }
    }
  }
}
