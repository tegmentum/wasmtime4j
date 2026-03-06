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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NnContext.NnImplementationInfo} and its {@code parseFromJson()} factory method.
 *
 * <p>Verifies JSON parsing from the native backend info format, constructor behavior, accessor
 * methods, and defensive copy semantics.
 */
@DisplayName("NnImplementationInfo Tests")
class NnImplementationInfoTest {

  @Nested
  @DisplayName("parseFromJson Tests")
  class ParseFromJsonTests {

    @Test
    @DisplayName("should parse complete JSON with version, backends, and default")
    void shouldParseCompleteJson() {
      final String json =
          "{\"version\":\"42.0.1\",\"backends\":[\"openvino\",\"onnx\"],\"default\":\"openvino\"}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      assertNotNull(info, "Info should not be null");
      assertEquals("42.0.1", info.getVersion(), "Version should be '42.0.1'");
      assertNotNull(info.getBackends(), "Backends should not be null");
      assertEquals(2, info.getBackends().size(), "Should have 2 backends");
      assertEquals("openvino", info.getBackends().get(0), "First backend should be 'openvino'");
      assertEquals("onnx", info.getBackends().get(1), "Second backend should be 'onnx'");
      assertEquals("openvino", info.getDefaultBackend(), "Default should be 'openvino'");
    }

    @Test
    @DisplayName("should handle null JSON input")
    void shouldHandleNullJson() {
      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(null);

      assertNotNull(info, "Info should not be null even with null input");
      assertEquals("unknown", info.getVersion(), "Version should default to 'unknown'");
      assertNotNull(info.getBackends(), "Backends should not be null");
      assertTrue(info.getBackends().isEmpty(), "Backends should be empty");
      assertNull(info.getDefaultBackend(), "Default backend should be null");
    }

    @Test
    @DisplayName("should handle empty string JSON input")
    void shouldHandleEmptyStringJson() {
      final NnContext.NnImplementationInfo info = NnContext.NnImplementationInfo.parseFromJson("");

      assertNotNull(info, "Info should not be null with empty string");
      assertEquals("unknown", info.getVersion(), "Version should default to 'unknown'");
      assertTrue(info.getBackends().isEmpty(), "Backends should be empty");
      assertNull(info.getDefaultBackend(), "Default backend should be null");
    }

    @Test
    @DisplayName("should parse JSON with version only")
    void shouldParseJsonWithVersionOnly() {
      final String json = "{\"version\":\"1.0.0\"}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      assertEquals("1.0.0", info.getVersion(), "Version should be '1.0.0'");
      assertTrue(info.getBackends().isEmpty(), "Backends should be empty when not in JSON");
      assertNull(info.getDefaultBackend(), "Default should be null when not in JSON");
    }

    @Test
    @DisplayName("should parse JSON with empty backends array")
    void shouldParseJsonWithEmptyBackends() {
      final String json = "{\"version\":\"42.0.1\",\"backends\":[],\"default\":\"\"}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      assertEquals("42.0.1", info.getVersion(), "Version should be '42.0.1'");
      assertTrue(info.getBackends().isEmpty(), "Backends should be empty");
      assertNull(info.getDefaultBackend(), "Default should be null when empty string in JSON");
    }

    @Test
    @DisplayName("should parse JSON with single backend")
    void shouldParseJsonWithSingleBackend() {
      final String json = "{\"version\":\"42.0.1\",\"backends\":[\"onnx\"],\"default\":\"onnx\"}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      assertEquals(1, info.getBackends().size(), "Should have 1 backend");
      assertEquals("onnx", info.getBackends().get(0), "Backend should be 'onnx'");
      assertEquals("onnx", info.getDefaultBackend(), "Default should be 'onnx'");
    }

    @Test
    @DisplayName("should parse JSON with many backends")
    void shouldParseJsonWithManyBackends() {
      final String json =
          "{\"version\":\"42.0.1\","
              + "\"backends\":[\"openvino\",\"onnx\",\"winml\",\"autodetect\"],"
              + "\"default\":\"openvino\"}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      assertEquals(4, info.getBackends().size(), "Should have 4 backends");
      assertEquals("openvino", info.getBackends().get(0));
      assertEquals("onnx", info.getBackends().get(1));
      assertEquals("winml", info.getBackends().get(2));
      assertEquals("autodetect", info.getBackends().get(3));
    }

    @Test
    @DisplayName("should handle JSON with no matching keys")
    void shouldHandleJsonWithNoMatchingKeys() {
      final String json = "{\"foo\":\"bar\",\"baz\":42}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      assertEquals("unknown", info.getVersion(), "Version should default to 'unknown'");
      assertTrue(info.getBackends().isEmpty(), "Backends should be empty");
      assertNull(info.getDefaultBackend(), "Default should be null");
    }

    @Test
    @DisplayName("should handle malformed JSON gracefully")
    void shouldHandleMalformedJsonGracefully() {
      // Unclosed string — extractJsonString will find version start but no closing quote
      final String json = "{\"version\":\"42.0.1";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      // The parser is best-effort; it may or may not extract the version depending on
      // whether the closing quote is found
      assertNotNull(info, "Info should not be null even with malformed JSON");
    }

    @Test
    @DisplayName("should skip non-quoted items in backends array")
    void shouldSkipNonQuotedBackendsItems() {
      final String json = "{\"version\":\"1.0\",\"backends\":[123,\"onnx\",true]}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      // Only quoted strings should be extracted
      assertEquals(1, info.getBackends().size(), "Should extract only quoted backend strings");
      assertEquals("onnx", info.getBackends().get(0), "Should extract 'onnx'");
    }

    @Test
    @DisplayName("should handle backends array with whitespace")
    void shouldHandleBackendsWithWhitespace() {
      final String json = "{\"version\":\"1.0\",\"backends\":[ \"a\" , \"b\" , \"c\" ]}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      assertEquals(3, info.getBackends().size(), "Should have 3 backends despite whitespace");
      assertEquals("a", info.getBackends().get(0));
      assertEquals("b", info.getBackends().get(1));
      assertEquals("c", info.getBackends().get(2));
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should construct with all parameters")
    void shouldConstructWithAllParams() {
      final List<String> backends = Arrays.asList("onnx", "openvino");

      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", backends, "onnx");

      assertEquals("1.0.0", info.getVersion(), "Version should match");
      assertEquals(2, info.getBackends().size(), "Should have 2 backends");
      assertEquals("onnx", info.getDefaultBackend(), "Default should match");
    }

    @Test
    @DisplayName("should construct with null backends")
    void shouldConstructWithNullBackends() {
      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", null, null);

      assertEquals("1.0.0", info.getVersion(), "Version should match");
      assertNull(info.getBackends(), "Backends should be null when constructed with null");
      assertNull(info.getDefaultBackend(), "Default should be null");
    }

    @Test
    @DisplayName("should construct with empty backends")
    void shouldConstructWithEmptyBackends() {
      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("2.0.0", new ArrayList<>(), null);

      assertEquals("2.0.0", info.getVersion(), "Version should match");
      assertNotNull(info.getBackends(), "Backends should not be null");
      assertTrue(info.getBackends().isEmpty(), "Backends should be empty");
    }

    @Test
    @DisplayName("should make defensive copy of backends list")
    void shouldMakeDefensiveCopyOfBackendsList() {
      final List<String> original = new ArrayList<>(Arrays.asList("onnx", "openvino"));

      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", original, "onnx");

      // Modify original list
      original.add("winml");

      // Info's backends should not be affected
      assertEquals(
          2,
          info.getBackends().size(),
          "Backends should not be affected by modification of original list");
    }
  }

  @Nested
  @DisplayName("Accessor Tests")
  class AccessorTests {

    @Test
    @DisplayName("getBackends should return defensive copy")
    void getBackendsShouldReturnDefensiveCopy() {
      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", Arrays.asList("onnx", "openvino"), "onnx");

      final List<String> backends1 = info.getBackends();
      final List<String> backends2 = info.getBackends();

      assertNotSame(backends1, backends2, "getBackends should return new list each call");
      assertEquals(backends1, backends2, "Both lists should have same content");
    }

    @Test
    @DisplayName("getVersion should return the version string")
    void getVersionShouldReturnVersion() {
      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("42.0.1", new ArrayList<>(), null);

      assertEquals("42.0.1", info.getVersion(), "Version should match");
    }

    @Test
    @DisplayName("getDefaultBackend should return null when not set")
    void getDefaultBackendShouldReturnNull() {
      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", new ArrayList<>(), null);

      assertNull(info.getDefaultBackend(), "Default backend should be null");
    }
  }

  @Nested
  @DisplayName("hasBackend Tests")
  class HasBackendTests {

    @Test
    @DisplayName("should return true for existing backend")
    void shouldReturnTrueForExistingBackend() {
      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", Arrays.asList("onnx", "openvino"), "onnx");

      assertTrue(info.hasBackend("onnx"), "Should find 'onnx'");
      assertTrue(info.hasBackend("openvino"), "Should find 'openvino'");
    }

    @Test
    @DisplayName("should return false for non-existing backend")
    void shouldReturnFalseForNonExistingBackend() {
      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", Arrays.asList("onnx"), "onnx");

      assertFalse(info.hasBackend("winml"), "Should not find 'winml'");
      assertFalse(info.hasBackend(""), "Should not find empty string");
    }

    @Test
    @DisplayName("should return false when backends is null")
    void shouldReturnFalseWhenBackendsNull() {
      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", null, null);

      assertFalse(info.hasBackend("onnx"), "Should return false when backends is null");
    }

    @Test
    @DisplayName("should return false when backends is empty")
    void shouldReturnFalseWhenBackendsEmpty() {
      final NnContext.NnImplementationInfo info =
          new NnContext.NnImplementationInfo("1.0.0", new ArrayList<>(), null);

      assertFalse(info.hasBackend("onnx"), "Should return false when backends is empty");
    }
  }

  @Nested
  @DisplayName("Integration with parseFromJson")
  class IntegrationTests {

    @Test
    @DisplayName("should parse realistic wasmtime backend info JSON")
    void shouldParseRealisticJson() {
      // This is the format generated by the Rust native code
      final String json =
          "{\"version\":\"42.0.1\",\"backends\":[\"openvino\"],\"default\":\"openvino\"}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      assertEquals("42.0.1", info.getVersion(), "Version should match wasmtime version");
      assertTrue(info.hasBackend("openvino"), "Should have openvino backend");
      assertFalse(info.hasBackend("onnx"), "Should not have onnx backend");
      assertEquals("openvino", info.getDefaultBackend(), "Default should be openvino");
    }

    @Test
    @DisplayName("should parse JSON with no backends available")
    void shouldParseJsonWithNoBackends() {
      // When no ML backends are installed
      final String json = "{\"version\":\"42.0.1\",\"backends\":[],\"default\":\"\"}";

      final NnContext.NnImplementationInfo info =
          NnContext.NnImplementationInfo.parseFromJson(json);

      assertEquals("42.0.1", info.getVersion());
      assertTrue(info.getBackends().isEmpty(), "Backends should be empty");
      assertNull(info.getDefaultBackend(), "Default should be null when empty string is provided");
      assertFalse(info.hasBackend("onnx"), "hasBackend should return false");
    }
  }
}
