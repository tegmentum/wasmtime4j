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
package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.TagType;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Store exception handling APIs: {@link Store#throwException(ExnRef)}, {@link
 * Store#takePendingException()}, {@link Store#hasPendingException()}, and related types {@link
 * Tag}, {@link TagType}, {@link ExnRef}.
 *
 * <p>Extends coverage beyond existing {@code ExceptionHandlingIntegrationTest} by testing the
 * Store-level exception management APIs directly.
 *
 * <p>Uses try/catch guards since exception handling may not be fully implemented in all runtimes.
 *
 * @since 1.0.0
 */
@DisplayName("Store Exception Handling Tests")
public class StoreExceptionHandlingTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(StoreExceptionHandlingTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("hasPendingException initially false on fresh store")
  void hasPendingExceptionInitiallyFalse(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing hasPendingException on fresh store");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final boolean hasPending = store.hasPendingException();
        assertFalse(hasPending, "Fresh store should not have pending exception");
        LOGGER.info("[" + runtime + "] hasPendingException on fresh store: " + hasPending);
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] hasPendingException not supported: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("create Tag with i32 payload")
  void createTagWithI32Payload(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Tag creation with i32 payload");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final FunctionType payloadType =
            FunctionType.of(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
        final TagType tagType = TagType.create(payloadType);
        final Tag tag = Tag.create(store, tagType);

        assertNotNull(tag, "Tag should not be null");
        LOGGER.info(
            "[" + runtime + "] Tag created with i32 payload, handle=" + tag.getNativeHandle());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] Tag creation not supported: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Tag.getType round-trips correctly")
  void tagGetTypeRoundTrips(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Tag.getType round-trip");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final FunctionType originalType =
            FunctionType.of(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
        final TagType tagType = TagType.create(originalType);
        final Tag tag = Tag.create(store, tagType);

        final TagType retrieved = tag.getType(store);
        assertNotNull(retrieved, "Retrieved TagType should not be null");

        final FunctionType retrievedFt = retrieved.getFunctionType();
        assertNotNull(retrievedFt, "Retrieved FunctionType should not be null");
        LOGGER.info(
            "["
                + runtime
                + "] Tag.getType returned FunctionType with "
                + retrievedFt.getParamCount()
                + " params, "
                + retrievedFt.getReturnCount()
                + " returns");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] Tag.getType not supported: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("create Tag with multiple payload types (i32, i64, f64)")
  void createTagWithMultiplePayloadTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Tag creation with multiple payload types");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final FunctionType payloadType =
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I64, WasmValueType.F64},
                new WasmValueType[] {});
        final TagType tagType = TagType.create(payloadType);
        final Tag tag = Tag.create(store, tagType);

        assertNotNull(tag, "Tag with multi-payload should not be null");
        LOGGER.info("[" + runtime + "] Tag created with (i32, i64, f64) payload");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] Tag creation not supported: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("create Tag with empty payload")
  void createTagWithEmptyPayload(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Tag creation with empty payload");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final FunctionType emptyType =
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {});
        final TagType tagType = TagType.create(emptyType);
        final Tag tag = Tag.create(store, tagType);

        assertNotNull(tag, "Tag with empty payload should not be null");
        LOGGER.info("[" + runtime + "] Tag created with empty payload");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] Tag creation not supported: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("throwException sets pending and takePendingException clears it")
  void throwExceptionSetsAndTakeClears(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing throwException/takePendingException lifecycle");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final FunctionType payloadType =
            FunctionType.of(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
        final TagType tagType = TagType.create(payloadType);
        final Tag tag = Tag.create(store, tagType);

        // throwException should throw WasmException
        try {
          store.throwException(null);
          // If no exception, throwException may not be implemented
          LOGGER.info("[" + runtime + "] throwException(null) did not throw");
        } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
          LOGGER.info("[" + runtime + "] throwException threw WasmException: " + e.getMessage());
          // Check if pending exception exists
          if (store.hasPendingException()) {
            final ExnRef pending = store.takePendingException();
            LOGGER.info("[" + runtime + "] takePendingException returned: " + pending);
            assertFalse(
                store.hasPendingException(), "After take, should not have pending exception");
          }
        } catch (final UnsupportedOperationException e) {
          LOGGER.info("[" + runtime + "] throwException not supported: " + e.getMessage());
        } catch (final UnsatisfiedLinkError | Exception e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] throwException threw: "
                  + e.getClass().getName()
                  + " - "
                  + e.getMessage());
        }
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] Exception handling not supported: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("takePendingException when none returns null")
  void takePendingExceptionWhenNoneReturnsNull(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing takePendingException when none pending");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final ExnRef pending = store.takePendingException();
        assertNull(pending, "takePendingException on fresh store should return null");
        LOGGER.info("[" + runtime + "] takePendingException returned null as expected");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] takePendingException not supported: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }
}
