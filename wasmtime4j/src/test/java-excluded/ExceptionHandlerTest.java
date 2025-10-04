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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for WebAssembly exception handling experimental feature. */
final class ExceptionHandlerTest {

  @BeforeEach
  void setUp() {
    // Enable exception handling feature for tests
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);
  }

  @AfterEach
  void tearDown() {
    // Clean up experimental features
    ExperimentalFeatures.reset();
  }

  @Test
  void testExceptionHandlerCreation() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder()
            .enableNestedTryCatch(true)
            .enableExceptionUnwinding(true)
            .maxUnwindDepth(500)
            .validateExceptionTypes(true)
            .build();

    assertDoesNotThrow(() -> new ExceptionHandler(config));
  }

  @Test
  void testExceptionHandlerWithNullConfig() {
    assertThrows(IllegalArgumentException.class, () -> new ExceptionHandler(null));
  }

  @Test
  void testExceptionHandlerWithFeatureDisabled() {
    ExperimentalFeatures.disableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);

    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();

    assertThrows(UnsupportedOperationException.class, () -> new ExceptionHandler(config));
  }

  @Test
  void testExceptionTagCreation() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();
    final ExceptionHandler handler = new ExceptionHandler(config);

    final List<WasmValueType> parameterTypes = Arrays.asList(WasmValueType.I32, WasmValueType.F64);

    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("test_exception", parameterTypes);

    assertNotNull(tag);
    assertEquals("test_exception", tag.getName());
    assertEquals(parameterTypes, tag.getParameterTypes());
    assertTrue(tag.getNativeHandle() > 0);
  }

  @Test
  void testExceptionTagWithNullName() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();
    final ExceptionHandler handler = new ExceptionHandler(config);

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.createExceptionTag(null, Collections.singletonList(WasmValueType.I32)));
  }

  @Test
  void testExceptionTagWithEmptyName() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();
    final ExceptionHandler handler = new ExceptionHandler(config);

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.createExceptionTag("", Collections.singletonList(WasmValueType.I32)));
  }

  @Test
  void testExceptionTagWithNullParameterTypes() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();
    final ExceptionHandler handler = new ExceptionHandler(config);

    assertThrows(IllegalArgumentException.class, () -> handler.createExceptionTag("test", null));
  }

  @Test
  void testExceptionTagEquality() {
    final List<WasmValueType> parameterTypes = Collections.singletonList(WasmValueType.I32);
    final ExceptionHandler.ExceptionTag tag1 =
        new ExceptionHandler.ExceptionTag("test", parameterTypes, 1L);
    final ExceptionHandler.ExceptionTag tag2 =
        new ExceptionHandler.ExceptionTag("test", parameterTypes, 1L);
    final ExceptionHandler.ExceptionTag tag3 =
        new ExceptionHandler.ExceptionTag("other", parameterTypes, 1L);

    assertEquals(tag1, tag2);
    assertNotEquals(tag1, tag3);
    assertEquals(tag1.hashCode(), tag2.hashCode());
  }

  @Test
  void testWasmExceptionCreation() {
    final List<WasmValueType> parameterTypes = Collections.singletonList(WasmValueType.I32);
    final ExceptionHandler.ExceptionTag tag =
        new ExceptionHandler.ExceptionTag("test", parameterTypes, 1L);
    final List<WasmValue> payload = Collections.singletonList(WasmValue.i32(42));

    final ExceptionHandler.WasmException exception =
        new ExceptionHandler.WasmException(tag, payload, "Test exception");

    assertEquals(tag, exception.getTag());
    assertEquals(payload, exception.getPayload());
    assertEquals("Test exception", exception.getMessage());
  }

  @Test
  void testWasmExceptionWithNullTag() {
    final List<WasmValue> payload = Collections.singletonList(WasmValue.i32(42));

    assertThrows(
        IllegalArgumentException.class,
        () -> new ExceptionHandler.WasmException(null, payload, "Test"));
  }

  @Test
  void testWasmExceptionWithNullPayload() {
    final List<WasmValueType> parameterTypes = Collections.singletonList(WasmValueType.I32);
    final ExceptionHandler.ExceptionTag tag =
        new ExceptionHandler.ExceptionTag("test", parameterTypes, 1L);

    assertThrows(
        IllegalArgumentException.class,
        () -> new ExceptionHandler.WasmException(tag, null, "Test"));
  }

  @Test
  void testThrowException() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();
    final ExceptionHandler handler = new ExceptionHandler(config);

    final List<WasmValueType> parameterTypes = Collections.singletonList(WasmValueType.I32);
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("test_exception", parameterTypes);
    final List<WasmValue> payload = Collections.singletonList(WasmValue.i32(42));

    final ExceptionHandler.WasmException exception =
        assertThrows(
            ExceptionHandler.WasmException.class, () -> handler.throwException(tag, payload));

    assertEquals(tag, exception.getTag());
    assertEquals(payload, exception.getPayload());
    assertTrue(exception.getMessage().contains("test_exception"));
  }

  @Test
  void testThrowExceptionWithNullTag() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();
    final ExceptionHandler handler = new ExceptionHandler(config);

    final List<WasmValue> payload = Collections.singletonList(WasmValue.i32(42));

    assertThrows(IllegalArgumentException.class, () -> handler.throwException(null, payload));
  }

  @Test
  void testThrowExceptionWithNullPayload() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();
    final ExceptionHandler handler = new ExceptionHandler(config);

    final List<WasmValueType> parameterTypes = Collections.singletonList(WasmValueType.I32);
    final ExceptionHandler.ExceptionTag tag = handler.createExceptionTag("test", parameterTypes);

    assertThrows(IllegalArgumentException.class, () -> handler.throwException(tag, null));
  }

  @Test
  void testExceptionHandlingConfigBuilder() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder()
            .enableNestedTryCatch(false)
            .enableExceptionUnwinding(false)
            .maxUnwindDepth(100)
            .validateExceptionTypes(false)
            .build();

    assertFalse(config.isNestedTryCatchEnabled());
    assertFalse(config.isExceptionUnwindingEnabled());
    assertEquals(100, config.getMaxUnwindDepth());
    assertFalse(config.isExceptionTypeValidationEnabled());
  }

  @Test
  void testExceptionHandlingConfigBuilderWithInvalidDepth() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ExceptionHandler.ExceptionHandlingConfig.builder().maxUnwindDepth(0));

    assertThrows(
        IllegalArgumentException.class,
        () -> ExceptionHandler.ExceptionHandlingConfig.builder().maxUnwindDepth(-1));
  }

  @Test
  void testExceptionHandlerResourceManagement() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();
    final ExceptionHandler handler = new ExceptionHandler(config);

    assertTrue(handler.getNativeHandle() > 0);

    // Test that close can be called multiple times safely
    assertDoesNotThrow(handler::close);
    assertDoesNotThrow(handler::close);
  }

  @Test
  void testExceptionHandlerGetConfig() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder()
            .enableNestedTryCatch(false)
            .maxUnwindDepth(250)
            .build();

    final ExceptionHandler handler = new ExceptionHandler(config);

    assertEquals(config, handler.getConfig());
    assertFalse(handler.getConfig().isNestedTryCatchEnabled());
    assertEquals(250, handler.getConfig().getMaxUnwindDepth());
  }

  @Test
  void testExceptionTagToString() {
    final List<WasmValueType> parameterTypes = Arrays.asList(WasmValueType.I32, WasmValueType.F64);
    final ExceptionHandler.ExceptionTag tag =
        new ExceptionHandler.ExceptionTag("test_tag", parameterTypes, 123L);

    final String result = tag.toString();

    assertTrue(result.contains("test_tag"));
    assertTrue(result.contains("123"));
    assertTrue(result.contains("I32"));
    assertTrue(result.contains("F64"));
  }

  @Test
  void testPayloadValidationDisabled() {
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().validateExceptionTypes(false).build();

    final ExceptionHandler handler = new ExceptionHandler(config);

    final List<WasmValueType> parameterTypes = Collections.singletonList(WasmValueType.I32);
    final ExceptionHandler.ExceptionTag tag = handler.createExceptionTag("test", parameterTypes);

    // With validation disabled, this should not throw even with wrong types
    final List<WasmValue> invalidPayload = Collections.singletonList(WasmValue.f32(3.14f));

    final ExceptionHandler.WasmException exception =
        assertThrows(
            ExceptionHandler.WasmException.class,
            () -> handler.throwException(tag, invalidPayload));

    assertEquals(tag, exception.getTag());
    assertEquals(invalidPayload, exception.getPayload());
  }
}
