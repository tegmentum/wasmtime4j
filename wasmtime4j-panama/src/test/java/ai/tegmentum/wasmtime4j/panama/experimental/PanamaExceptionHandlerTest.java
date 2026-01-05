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

package ai.tegmentum.wasmtime4j.panama.experimental;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.experimental.DefaultExceptionHandlingConfig;
import ai.tegmentum.wasmtime4j.experimental.ExceptionHandler.ExceptionHandlingConfig;
import ai.tegmentum.wasmtime4j.experimental.ExceptionHandler.ExceptionTag;
import ai.tegmentum.wasmtime4j.experimental.ExceptionHandler.HandlingResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link PanamaExceptionHandler} class. */
@DisplayName("PanamaExceptionHandler Tests")
public class PanamaExceptionHandlerTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaExceptionHandlerTest.class.getName());

  @Test
  @DisplayName("Create exception handler with default configuration")
  public void testCreateDefaultHandler() {
    LOGGER.info("Testing creation of default exception handler");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      assertNotNull(handler, "Handler should not be null");
      assertEquals("PanamaExceptionHandler", handler.getHandlerName(), "Handler name should match");
      assertTrue(handler.isEnabled(), "Handler should be enabled by default");
      assertFalse(handler.isClosed(), "Handler should not be closed");
      assertNotNull(handler.getConfig(), "Config should not be null");

      LOGGER.info("Default handler creation test passed");
    }
  }

  @Test
  @DisplayName("Create exception handler with custom configuration")
  public void testCreateCustomHandler() {
    LOGGER.info("Testing creation of custom exception handler");

    final ExceptionHandlingConfig config =
        DefaultExceptionHandlingConfig.builder()
            .nestedTryCatch(false)
            .exceptionUnwinding(true)
            .maxUnwindDepth(500)
            .typeValidation(true)
            .stackTraces(false)
            .build();

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create(config, "CustomHandler")) {
      assertNotNull(handler, "Handler should not be null");
      assertEquals("CustomHandler", handler.getHandlerName(), "Handler name should match");
      assertTrue(handler.isEnabled(), "Handler should be enabled");
      assertNotNull(handler.getConfig(), "Config should not be null");
      assertFalse(
          handler.getConfig().isNestedTryCatchEnabled(), "Nested try/catch should be disabled");
      assertEquals(500, handler.getConfig().getMaxUnwindDepth(), "Max unwind depth should match");

      LOGGER.info("Custom handler creation test passed");
    }
  }

  @Test
  @DisplayName("Handle null exception returns NOT_HANDLED")
  public void testHandleNullException() {
    LOGGER.info("Testing handle null exception");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      final HandlingResult result = handler.handle(null);
      assertEquals(HandlingResult.NOT_HANDLED, result, "Null exception should return NOT_HANDLED");

      LOGGER.info("Handle null exception test passed");
    }
  }

  @Test
  @DisplayName("Handle exception when disabled returns NOT_HANDLED")
  public void testHandleDisabledException() {
    LOGGER.info("Testing handle when handler is disabled");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      handler.setEnabled(false);
      assertFalse(handler.isEnabled(), "Handler should be disabled");

      final HandlingResult result = handler.handle(new RuntimeException("test"));
      assertEquals(
          HandlingResult.NOT_HANDLED, result, "Disabled handler should return NOT_HANDLED");

      LOGGER.info("Handle disabled test passed");
    }
  }

  @Test
  @DisplayName("Handle valid exception returns HANDLED")
  public void testHandleValidException() {
    LOGGER.info("Testing handle valid exception");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      final HandlingResult result = handler.handle(new RuntimeException("test"));
      assertEquals(HandlingResult.HANDLED, result, "Valid exception should return HANDLED");

      LOGGER.info("Handle valid exception test passed");
    }
  }

  @Test
  @DisplayName("Enable and disable handler")
  public void testEnableDisable() {
    LOGGER.info("Testing enable/disable handler");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      assertTrue(handler.isEnabled(), "Handler should be enabled by default");

      handler.setEnabled(false);
      assertFalse(handler.isEnabled(), "Handler should be disabled");

      handler.setEnabled(true);
      assertTrue(handler.isEnabled(), "Handler should be re-enabled");

      LOGGER.info("Enable/disable test passed");
    }
  }

  @Test
  @DisplayName("Create exception tag")
  public void testCreateExceptionTag() {
    LOGGER.info("Testing exception tag creation");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      final List<WasmValueType> paramTypes = Arrays.asList(WasmValueType.I32, WasmValueType.I64);
      final ExceptionTag tag = handler.createExceptionTag("test_tag", paramTypes);

      assertNotNull(tag, "Tag should not be null");
      assertEquals("test_tag", tag.getTagName(), "Tag name should match");
      assertEquals(paramTypes, tag.getParameterTypes(), "Parameter types should match");
      assertTrue(tag.getTagHandle() != 0, "Tag handle should be non-zero");

      LOGGER.info("Exception tag creation test passed: " + tag);
    }
  }

  @Test
  @DisplayName("Create exception tag with empty parameter types")
  public void testCreateExceptionTagEmptyParams() {
    LOGGER.info("Testing exception tag creation with empty params");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      final ExceptionTag tag = handler.createExceptionTag("empty_tag", Collections.emptyList());

      assertNotNull(tag, "Tag should not be null");
      assertEquals("empty_tag", tag.getTagName(), "Tag name should match");
      assertTrue(tag.getParameterTypes().isEmpty(), "Parameter types should be empty");

      LOGGER.info("Exception tag empty params test passed");
    }
  }

  @Test
  @DisplayName("Reject duplicate exception tag")
  public void testRejectDuplicateTag() {
    LOGGER.info("Testing rejection of duplicate tag");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      handler.createExceptionTag("duplicate_tag", Collections.emptyList());

      assertThrows(
          IllegalArgumentException.class,
          () -> handler.createExceptionTag("duplicate_tag", Collections.emptyList()),
          "Should reject duplicate tag name");

      LOGGER.info("Reject duplicate tag test passed");
    }
  }

  @Test
  @DisplayName("Reject null tag name")
  public void testRejectNullTagName() {
    LOGGER.info("Testing rejection of null tag name");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      assertThrows(
          NullPointerException.class,
          () -> handler.createExceptionTag(null, Collections.emptyList()),
          "Should reject null tag name");

      LOGGER.info("Reject null tag name test passed");
    }
  }

  @Test
  @DisplayName("Reject blank tag name")
  public void testRejectBlankTagName() {
    LOGGER.info("Testing rejection of blank tag name");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> handler.createExceptionTag("  ", Collections.emptyList()),
          "Should reject blank tag name");

      LOGGER.info("Reject blank tag name test passed");
    }
  }

  @Test
  @DisplayName("Get exception tag by name")
  public void testGetExceptionTag() {
    LOGGER.info("Testing get exception tag by name");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      final List<WasmValueType> paramTypes = Arrays.asList(WasmValueType.F32);
      handler.createExceptionTag("find_me", paramTypes);

      final Optional<ExceptionTag> found = handler.getExceptionTag("find_me");
      assertTrue(found.isPresent(), "Tag should be found");
      assertEquals("find_me", found.get().getTagName(), "Tag name should match");

      final Optional<ExceptionTag> notFound = handler.getExceptionTag("not_exists");
      assertFalse(notFound.isPresent(), "Non-existent tag should return empty");

      LOGGER.info("Get exception tag test passed");
    }
  }

  @Test
  @DisplayName("List exception tags")
  public void testListExceptionTags() {
    LOGGER.info("Testing list exception tags");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      assertTrue(handler.listExceptionTags().isEmpty(), "Initial tag list should be empty");

      handler.createExceptionTag("tag1", Collections.emptyList());
      handler.createExceptionTag("tag2", Collections.emptyList());
      handler.createExceptionTag("tag3", Collections.emptyList());

      final List<ExceptionTag> tags = handler.listExceptionTags();
      assertEquals(3, tags.size(), "Should have 3 tags");

      LOGGER.info("List exception tags test passed");
    }
  }

  @Test
  @DisplayName("Perform unwinding respects max depth")
  public void testPerformUnwindingMaxDepth() {
    LOGGER.info("Testing perform unwinding with max depth");

    final ExceptionHandlingConfig config =
        DefaultExceptionHandlingConfig.builder().maxUnwindDepth(5).build();

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create(config)) {
      assertTrue(handler.performUnwinding(0), "Should continue at depth 0");
      assertTrue(handler.performUnwinding(4), "Should continue at depth 4");
      assertFalse(handler.performUnwinding(5), "Should stop at max depth");
      assertFalse(handler.performUnwinding(100), "Should stop beyond max depth");

      LOGGER.info("Perform unwinding test passed");
    }
  }

  @Test
  @DisplayName("Perform unwinding returns false when disabled")
  public void testPerformUnwindingDisabled() {
    LOGGER.info("Testing perform unwinding when disabled");

    final ExceptionHandlingConfig config =
        DefaultExceptionHandlingConfig.builder().exceptionUnwinding(false).build();

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create(config)) {
      assertFalse(handler.performUnwinding(0), "Should return false when unwinding disabled");

      LOGGER.info("Perform unwinding disabled test passed");
    }
  }

  @Test
  @DisplayName("Reject negative unwinding depth")
  public void testRejectNegativeUnwindingDepth() {
    LOGGER.info("Testing rejection of negative unwinding depth");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> handler.performUnwinding(-1),
          "Should reject negative depth");

      LOGGER.info("Reject negative depth test passed");
    }
  }

  @Test
  @DisplayName("Capture stack trace")
  public void testCaptureStackTrace() {
    LOGGER.info("Testing capture stack trace");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      final ExceptionTag tag = handler.createExceptionTag("trace_tag", Collections.emptyList());
      final String trace = handler.captureStackTrace(tag.getTagHandle());

      assertNotNull(trace, "Stack trace should not be null");
      assertTrue(trace.contains("wasm function"), "Stack trace should contain wasm function info");

      LOGGER.info("Capture stack trace test passed: " + trace);
    }
  }

  @Test
  @DisplayName("Capture stack trace returns null when disabled")
  public void testCaptureStackTraceDisabled() {
    LOGGER.info("Testing capture stack trace when disabled");

    final ExceptionHandlingConfig config =
        DefaultExceptionHandlingConfig.builder().stackTraces(false).build();

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create(config)) {
      final ExceptionTag tag = handler.createExceptionTag("no_trace", Collections.emptyList());
      final String trace = handler.captureStackTrace(tag.getTagHandle());

      assertNull(trace, "Stack trace should be null when disabled");

      LOGGER.info("Capture stack trace disabled test passed");
    }
  }

  @Test
  @DisplayName("Capture stack trace returns null for zero handle")
  public void testCaptureStackTraceZeroHandle() {
    LOGGER.info("Testing capture stack trace with zero handle");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      final String trace = handler.captureStackTrace(0L);
      assertNull(trace, "Stack trace should be null for zero handle");

      LOGGER.info("Capture stack trace zero handle test passed");
    }
  }

  @Test
  @DisplayName("Close handler releases resources")
  public void testCloseHandler() {
    LOGGER.info("Testing close handler");

    final PanamaExceptionHandler handler = PanamaExceptionHandler.create();
    assertFalse(handler.isClosed(), "Handler should not be closed initially");

    handler.close();
    assertTrue(handler.isClosed(), "Handler should be closed after close()");
    assertFalse(handler.isEnabled(), "Closed handler should not be enabled");

    // Multiple closes should be idempotent
    handler.close();
    assertTrue(handler.isClosed(), "Handler should still be closed");

    LOGGER.info("Close handler test passed");
  }

  @Test
  @DisplayName("Operations on closed handler throw IllegalStateException")
  public void testClosedHandlerOperations() {
    LOGGER.info("Testing operations on closed handler");

    final PanamaExceptionHandler handler = PanamaExceptionHandler.create();
    handler.close();

    assertThrows(
        IllegalStateException.class,
        () -> handler.createExceptionTag("test", Collections.emptyList()),
        "createExceptionTag should throw on closed handler");

    assertThrows(
        IllegalStateException.class,
        () -> handler.captureStackTrace(1L),
        "captureStackTrace should throw on closed handler");

    assertThrows(
        IllegalStateException.class,
        () -> handler.performUnwinding(0),
        "performUnwinding should throw on closed handler");

    assertThrows(
        IllegalStateException.class,
        handler::getNativeHandle,
        "getNativeHandle should throw on closed handler");

    LOGGER.info("Closed handler operations test passed");
  }

  @Test
  @DisplayName("Get tag by handle")
  public void testGetTagByHandle() {
    LOGGER.info("Testing get tag by handle");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      final ExceptionTag created =
          handler.createExceptionTag("handle_test", Collections.emptyList());
      final ExceptionTag found = handler.getTagByHandle(created.getTagHandle());

      assertNotNull(found, "Should find tag by handle");
      assertEquals(created.getTagHandle(), found.getTagHandle(), "Handles should match");
      assertEquals(created.getTagName(), found.getTagName(), "Names should match");

      assertNull(handler.getTagByHandle(999999L), "Non-existent handle should return null");

      LOGGER.info("Get tag by handle test passed");
    }
  }

  @Test
  @DisplayName("Reject null configuration")
  public void testRejectNullConfig() {
    LOGGER.info("Testing rejection of null configuration");

    assertThrows(
        NullPointerException.class,
        () -> PanamaExceptionHandler.create(null),
        "Should reject null config");

    LOGGER.info("Reject null config test passed");
  }

  @Test
  @DisplayName("Reject null handler name")
  public void testRejectNullHandlerName() {
    LOGGER.info("Testing rejection of null handler name");

    assertThrows(
        NullPointerException.class,
        () -> PanamaExceptionHandler.create(DefaultExceptionHandlingConfig.getDefault(), null),
        "Should reject null handler name");

    LOGGER.info("Reject null handler name test passed");
  }

  @Test
  @DisplayName("Get native handle returns valid handle")
  public void testGetNativeHandle() {
    LOGGER.info("Testing get native handle");

    try (PanamaExceptionHandler handler = PanamaExceptionHandler.create()) {
      assertNotNull(handler.getNativeHandle(), "Native handle should not be null");
      assertTrue(
          handler.getNativeHandle().address() != 0L, "Native handle address should be non-zero");

      LOGGER.info("Get native handle test passed");
    }
  }
}
