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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ComponentTypeDescriptor} factory methods and inner classes.
 *
 * @since 1.0.0
 */
@DisplayName("ComponentTypeDescriptor Tests")
class ComponentTypeDescriptorTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentTypeDescriptorTest.class.getName());

  @Nested
  @DisplayName("Future type descriptor")
  class FutureTypeTests {

    @Test
    @DisplayName("future with payload type has correct type and accessor")
    void futureWithPayloadType() {
      final ComponentTypeDescriptor payload = ComponentTypeDescriptor.u32();
      final ComponentTypeDescriptor future = ComponentTypeDescriptor.future(payload);

      assertEquals(ComponentType.FUTURE, future.getType(), "Type should be FUTURE");
      assertTrue(
          future.getFuturePayloadType().isPresent(), "Future payload type should be present");
      assertEquals(
          ComponentType.U32,
          future.getFuturePayloadType().get().getType(),
          "Payload type should be U32");
      LOGGER.info("future<u32> toString: " + future);
    }

    @Test
    @DisplayName("future with null payload (void future) has empty payload type")
    void futureWithNullPayload() {
      final ComponentTypeDescriptor future = ComponentTypeDescriptor.future(null);

      assertEquals(ComponentType.FUTURE, future.getType(), "Type should be FUTURE");
      assertFalse(
          future.getFuturePayloadType().isPresent(), "Void future should have empty payload type");
      LOGGER.info("future (void) toString: " + future);
    }

    @Test
    @DisplayName("future throws on non-future accessors")
    void futureThrowsOnNonFutureAccessors() {
      final ComponentTypeDescriptor future = ComponentTypeDescriptor.future(null);

      assertThrows(
          IllegalStateException.class, future::getElementType, "Should throw for getElementType");
      assertThrows(
          IllegalStateException.class,
          future::getStreamElementType,
          "Should throw for getStreamElementType");
      assertThrows(
          IllegalStateException.class,
          future::getResourceTypeName,
          "Should throw for getResourceTypeName");
      assertThrows(
          IllegalStateException.class, future::getOptionType, "Should throw for getOptionType");
    }

    @Test
    @DisplayName("future getName returns empty")
    void futureGetNameReturnsEmpty() {
      final ComponentTypeDescriptor future = ComponentTypeDescriptor.future(null);

      assertEquals(Optional.empty(), future.getName(), "Future getName should return empty");
    }
  }

  @Nested
  @DisplayName("Stream type descriptor")
  class StreamTypeTests {

    @Test
    @DisplayName("stream with element type has correct type and accessor")
    void streamWithElementType() {
      final ComponentTypeDescriptor elementType = ComponentTypeDescriptor.string();
      final ComponentTypeDescriptor stream = ComponentTypeDescriptor.stream(elementType);

      assertEquals(ComponentType.STREAM, stream.getType(), "Type should be STREAM");
      assertTrue(
          stream.getStreamElementType().isPresent(), "Stream element type should be present");
      assertEquals(
          ComponentType.STRING,
          stream.getStreamElementType().get().getType(),
          "Element type should be STRING");
      LOGGER.info("stream<string> toString: " + stream);
    }

    @Test
    @DisplayName("stream with null element type (void stream) has empty element type")
    void streamWithNullElementType() {
      final ComponentTypeDescriptor stream = ComponentTypeDescriptor.stream(null);

      assertEquals(ComponentType.STREAM, stream.getType(), "Type should be STREAM");
      assertFalse(
          stream.getStreamElementType().isPresent(), "Void stream should have empty element type");
      LOGGER.info("stream (void) toString: " + stream);
    }

    @Test
    @DisplayName("stream throws on non-stream accessors")
    void streamThrowsOnNonStreamAccessors() {
      final ComponentTypeDescriptor stream = ComponentTypeDescriptor.stream(null);

      assertThrows(
          IllegalStateException.class, stream::getElementType, "Should throw for getElementType");
      assertThrows(
          IllegalStateException.class,
          stream::getFuturePayloadType,
          "Should throw for getFuturePayloadType");
      assertThrows(
          IllegalStateException.class,
          stream::getResourceTypeName,
          "Should throw for getResourceTypeName");
    }
  }

  @Nested
  @DisplayName("Resource handle type descriptors (own/borrow)")
  class ResourceHandleTypeTests {

    @Test
    @DisplayName("own resource has correct type, name, id, and ownership")
    void ownResourceHasCorrectFields() {
      final ComponentTypeDescriptor own = ComponentTypeDescriptor.own("my-resource", 42L);

      assertEquals(ComponentType.OWN, own.getType(), "Type should be OWN");
      assertEquals("my-resource", own.getResourceTypeName(), "Resource type name should match");
      assertEquals(42L, own.getResourceTypeId(), "Resource type ID should match");
      assertTrue(own.isResourceOwned(), "Own resource should return true for isResourceOwned");
      LOGGER.info("own<my-resource> toString: " + own);
    }

    @Test
    @DisplayName("borrow resource has correct type, name, id, and ownership")
    void borrowResourceHasCorrectFields() {
      final ComponentTypeDescriptor borrow = ComponentTypeDescriptor.borrow("my-resource", 99L);

      assertEquals(ComponentType.BORROW, borrow.getType(), "Type should be BORROW");
      assertEquals("my-resource", borrow.getResourceTypeName(), "Resource type name should match");
      assertEquals(99L, borrow.getResourceTypeId(), "Resource type ID should match");
      assertFalse(
          borrow.isResourceOwned(), "Borrow resource should return false for isResourceOwned");
      LOGGER.info("borrow<my-resource> toString: " + borrow);
    }

    @Test
    @DisplayName("resource handle throws on non-resource accessors")
    void resourceHandleThrowsOnNonResourceAccessors() {
      final ComponentTypeDescriptor own = ComponentTypeDescriptor.own("res", 1L);

      assertThrows(
          IllegalStateException.class, own::getElementType, "Should throw for getElementType");
      assertThrows(
          IllegalStateException.class,
          own::getFuturePayloadType,
          "Should throw for getFuturePayloadType");
      assertThrows(
          IllegalStateException.class,
          own::getStreamElementType,
          "Should throw for getStreamElementType");
      assertThrows(
          IllegalStateException.class, own::getOptionType, "Should throw for getOptionType");
    }

    @Test
    @DisplayName("resource handle getName returns empty")
    void resourceHandleGetNameReturnsEmpty() {
      final ComponentTypeDescriptor own = ComponentTypeDescriptor.own("res", 1L);

      assertEquals(Optional.empty(), own.getName(), "Resource handle getName should return empty");
    }
  }

  @Nested
  @DisplayName("Error context type descriptor")
  class ErrorContextTypeTests {

    @Test
    @DisplayName("errorContext has correct type")
    void errorContextHasCorrectType() {
      final ComponentTypeDescriptor errCtx = ComponentTypeDescriptor.errorContext();

      assertEquals(ComponentType.ERROR_CONTEXT, errCtx.getType(), "Type should be ERROR_CONTEXT");
      LOGGER.info("error_context toString: " + errCtx);
    }

    @Test
    @DisplayName("errorContext throws on type-specific accessors")
    void errorContextThrowsOnTypeSpecificAccessors() {
      final ComponentTypeDescriptor errCtx = ComponentTypeDescriptor.errorContext();

      assertThrows(
          IllegalStateException.class,
          errCtx::getFuturePayloadType,
          "Should throw for getFuturePayloadType");
      assertThrows(
          IllegalStateException.class,
          errCtx::getStreamElementType,
          "Should throw for getStreamElementType");
      assertThrows(
          IllegalStateException.class,
          errCtx::getResourceTypeName,
          "Should throw for getResourceTypeName");
    }
  }

  @Nested
  @DisplayName("Named wrapper with new types")
  class NamedWrapperTests {

    @Test
    @DisplayName("named future delegates correctly")
    void namedFutureDelegatesCorrectly() {
      final ComponentTypeDescriptor future =
          ComponentTypeDescriptor.future(ComponentTypeDescriptor.u32());
      final ComponentTypeDescriptor named = ComponentTypeDescriptor.named("my_future", future);

      assertEquals(ComponentType.FUTURE, named.getType(), "Named future type should be FUTURE");
      assertTrue(
          named.getFuturePayloadType().isPresent(),
          "Named future should delegate getFuturePayloadType");
      assertEquals(
          "my_future", named.getName().orElse(null), "Named future should have correct name");
      LOGGER.info("Named future toString: " + named);
    }

    @Test
    @DisplayName("named own resource delegates correctly")
    void namedOwnResourceDelegatesCorrectly() {
      final ComponentTypeDescriptor own = ComponentTypeDescriptor.own("my-res", 7L);
      final ComponentTypeDescriptor named = ComponentTypeDescriptor.named("handle", own);

      assertEquals(ComponentType.OWN, named.getType(), "Named own type should be OWN");
      assertEquals(
          "my-res", named.getResourceTypeName(), "Named own should delegate getResourceTypeName");
      assertEquals(7L, named.getResourceTypeId(), "Named own should delegate getResourceTypeId");
      assertTrue(named.isResourceOwned(), "Named own should delegate isResourceOwned");
    }
  }

  @Nested
  @DisplayName("Existing types still throw on new accessors")
  class ExistingTypesTests {

    @Test
    @DisplayName("primitive types throw on future/stream/resource accessors")
    void primitiveThrowsOnNewAccessors() {
      final ComponentTypeDescriptor bool = ComponentTypeDescriptor.bool();

      assertThrows(
          IllegalStateException.class,
          bool::getFuturePayloadType,
          "Primitive should throw for getFuturePayloadType");
      assertThrows(
          IllegalStateException.class,
          bool::getStreamElementType,
          "Primitive should throw for getStreamElementType");
      assertThrows(
          IllegalStateException.class,
          bool::getResourceTypeId,
          "Primitive should throw for getResourceTypeId");
      assertThrows(
          IllegalStateException.class,
          bool::isResourceOwned,
          "Primitive should throw for isResourceOwned");
    }

    @Test
    @DisplayName("list type throws on future/stream/resource accessors")
    void listThrowsOnNewAccessors() {
      final ComponentTypeDescriptor list =
          ComponentTypeDescriptor.list(ComponentTypeDescriptor.u8());

      assertThrows(
          IllegalStateException.class,
          list::getFuturePayloadType,
          "List should throw for getFuturePayloadType");
      assertThrows(
          IllegalStateException.class,
          list::getStreamElementType,
          "List should throw for getStreamElementType");
      assertThrows(
          IllegalStateException.class,
          list::getResourceTypeId,
          "List should throw for getResourceTypeId");
    }

    @Test
    @DisplayName("option type throws on future/stream/resource accessors")
    void optionThrowsOnNewAccessors() {
      final ComponentTypeDescriptor option =
          ComponentTypeDescriptor.option(ComponentTypeDescriptor.string());

      assertThrows(
          IllegalStateException.class,
          option::getFuturePayloadType,
          "Option should throw for getFuturePayloadType");
      assertThrows(
          IllegalStateException.class,
          option::getStreamElementType,
          "Option should throw for getStreamElementType");
    }

    @Test
    @DisplayName("result type throws on future/stream/resource accessors")
    void resultThrowsOnNewAccessors() {
      final ComponentTypeDescriptor result =
          ComponentTypeDescriptor.result(
              ComponentTypeDescriptor.u32(), ComponentTypeDescriptor.string());

      assertThrows(
          IllegalStateException.class,
          result::getFuturePayloadType,
          "Result should throw for getFuturePayloadType");
      assertThrows(
          IllegalStateException.class,
          result::getStreamElementType,
          "Result should throw for getStreamElementType");
    }
  }

  @Nested
  @DisplayName("toString verification")
  class ToStringTests {

    @Test
    @DisplayName("all new types produce readable toString")
    void allNewTypesProduceReadableToString() {
      final String futureStr =
          ComponentTypeDescriptor.future(ComponentTypeDescriptor.u32()).toString();
      assertNotNull(futureStr, "future toString should not be null");
      assertTrue(futureStr.contains("future"), "future toString should contain 'future'");
      LOGGER.info("future toString: " + futureStr);

      final String streamStr =
          ComponentTypeDescriptor.stream(ComponentTypeDescriptor.string()).toString();
      assertNotNull(streamStr, "stream toString should not be null");
      assertTrue(streamStr.contains("stream"), "stream toString should contain 'stream'");
      LOGGER.info("stream toString: " + streamStr);

      final String ownStr = ComponentTypeDescriptor.own("widget", 10L).toString();
      assertNotNull(ownStr, "own toString should not be null");
      assertTrue(ownStr.contains("own"), "own toString should contain 'own'");
      assertTrue(ownStr.contains("widget"), "own toString should contain resource name");
      LOGGER.info("own toString: " + ownStr);

      final String borrowStr = ComponentTypeDescriptor.borrow("widget", 10L).toString();
      assertNotNull(borrowStr, "borrow toString should not be null");
      assertTrue(borrowStr.contains("borrow"), "borrow toString should contain 'borrow'");
      LOGGER.info("borrow toString: " + borrowStr);

      final String voidFutureStr = ComponentTypeDescriptor.future(null).toString();
      assertEquals("future", voidFutureStr, "Void future should be just 'future'");
      LOGGER.info("void future toString: " + voidFutureStr);

      final String voidStreamStr = ComponentTypeDescriptor.stream(null).toString();
      assertEquals("stream", voidStreamStr, "Void stream should be just 'stream'");
      LOGGER.info("void stream toString: " + voidStreamStr);
    }
  }
}
