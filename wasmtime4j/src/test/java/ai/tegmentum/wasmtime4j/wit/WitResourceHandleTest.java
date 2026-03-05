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
package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for WitOwn and WitBorrow resource handle classes. */
@DisplayName("WIT Resource Handle Tests")
final class WitResourceHandleTest {

  @Nested
  @DisplayName("WitOwn Tests")
  class WitOwnTests {

    @Test
    @DisplayName("Create owned handle with resource type and index")
    void testCreateOwnedHandle() {
      final WitOwn own = WitOwn.of("file-handle", 42);

      assertNotNull(own, "WitOwn should not be null");
      assertEquals("file-handle", own.getResourceType(), "Resource type should match");
      assertEquals(42, own.getIndex(), "Index should match");
      assertTrue(own.getHandle().isOwned(), "Handle should be owned");
    }

    @Test
    @DisplayName("Create owned handle with zero index")
    void testCreateOwnedHandleWithZeroIndex() {
      final WitOwn own = WitOwn.of("database-connection", 0);

      assertEquals("database-connection", own.getResourceType());
      assertEquals(0, own.getIndex());
    }

    @Test
    @DisplayName("Create owned handle with max index")
    void testCreateOwnedHandleWithMaxIndex() {
      final WitOwn own = WitOwn.of("socket", Integer.MAX_VALUE);

      assertEquals(Integer.MAX_VALUE, own.getIndex());
    }

    @Test
    @DisplayName("Reject negative index for owned handle")
    void testRejectNegativeIndex() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOwn.of("invalid-handle", -1),
          "Should reject negative index");
    }

    @Test
    @DisplayName("Create owned handle with host object")
    void testCreateOwnedHandleWithHostObject() {
      final String hostObject = "my-host-object";
      final WitOwn own = WitOwn.ofWithHost("custom-resource", 10, hostObject);

      assertNotNull(own);
      assertEquals("custom-resource", own.getResourceType());
      assertEquals(10, own.getIndex());
      assertEquals(hostObject, own.getHostObject(String.class));
    }

    @Test
    @DisplayName("Create owned handle from existing ComponentResourceHandle")
    void testFromHandle() {
      final ComponentResourceHandle handle = ComponentResourceHandle.own("file", 5);
      final WitOwn own = WitOwn.fromHandle(handle);

      assertNotNull(own);
      assertEquals("file", own.getResourceType());
      assertEquals(5, own.getIndex());
      assertEquals(handle, own.getHandle());
    }

    @Test
    @DisplayName("Reject null handle in fromHandle")
    void testFromHandleRejectsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOwn.fromHandle(null),
          "Should reject null handle");
    }

    @Test
    @DisplayName("Reject borrowed handle in fromHandle")
    void testFromHandleRejectsBorrowed() {
      final ComponentResourceHandle borrowed = ComponentResourceHandle.borrow("file", 5);
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOwn.fromHandle(borrowed),
          "Should reject borrowed handle");
    }

    @Test
    @DisplayName("toJava returns ComponentResourceHandle")
    void testToJava() {
      final WitOwn own = WitOwn.of("resource", 7);
      final Object result = own.toJava();

      assertTrue(result instanceof ComponentResourceHandle);
      assertEquals(own.getHandle(), result);
    }

    @Test
    @DisplayName("toString provides readable representation")
    void testToString() {
      final WitOwn own = WitOwn.of("my-resource", 123);
      final String str = own.toString();

      assertTrue(str.contains("WitOwn"), "Should contain class name");
      assertTrue(str.contains("my-resource"), "Should contain resource type");
      assertTrue(str.contains("123"), "Should contain index");
    }

    @Test
    @DisplayName("equals and hashCode work correctly")
    void testEqualsAndHashCode() {
      final WitOwn own1 = WitOwn.of("resource", 5);
      final WitOwn own2 = WitOwn.of("resource", 5);
      final WitOwn own3 = WitOwn.of("resource", 6);
      final WitOwn own4 = WitOwn.of("other", 5);

      assertEquals(own1, own2, "Equal handles should be equal");
      assertEquals(own1.hashCode(), own2.hashCode(), "Equal handles should have same hash");
      assertNotEquals(own1, own3, "Different indices should not be equal");
      assertNotEquals(own1, own4, "Different types should not be equal");
      assertNotEquals(own1, null, "Should not equal null");
      assertNotEquals(own1, "not a WitOwn", "Should not equal other types");
    }
  }

  @Nested
  @DisplayName("WitBorrow Tests")
  class WitBorrowTests {

    @Test
    @DisplayName("Create borrowed handle with resource type and index")
    void testCreateBorrowedHandle() {
      final WitBorrow borrow = WitBorrow.of("file-handle", 42);

      assertNotNull(borrow, "WitBorrow should not be null");
      assertEquals("file-handle", borrow.getResourceType(), "Resource type should match");
      assertEquals(42, borrow.getIndex(), "Index should match");
      assertFalse(borrow.getHandle().isOwned(), "Handle should be borrowed");
    }

    @Test
    @DisplayName("Create borrowed handle with zero index")
    void testCreateBorrowedHandleWithZeroIndex() {
      final WitBorrow borrow = WitBorrow.of("database-connection", 0);

      assertEquals("database-connection", borrow.getResourceType());
      assertEquals(0, borrow.getIndex());
    }

    @Test
    @DisplayName("Create borrowed handle with max index")
    void testCreateBorrowedHandleWithMaxIndex() {
      final WitBorrow borrow = WitBorrow.of("socket", Integer.MAX_VALUE);

      assertEquals(Integer.MAX_VALUE, borrow.getIndex());
    }

    @Test
    @DisplayName("Reject negative index for borrowed handle")
    void testRejectNegativeIndex() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.of("invalid-handle", -1),
          "Should reject negative index");
    }

    @Test
    @DisplayName("Create borrowed handle with host object")
    void testCreateBorrowedHandleWithHostObject() {
      final Integer hostObject = 999;
      final WitBorrow borrow = WitBorrow.ofWithHost("custom-resource", 10, hostObject);

      assertNotNull(borrow);
      assertEquals("custom-resource", borrow.getResourceType());
      assertEquals(10, borrow.getIndex());
      assertEquals(hostObject, borrow.getHostObject(Integer.class));
    }

    @Test
    @DisplayName("Create borrowed handle from existing ComponentResourceHandle")
    void testFromHandle() {
      final ComponentResourceHandle handle = ComponentResourceHandle.borrow("file", 5);
      final WitBorrow borrow = WitBorrow.fromHandle(handle);

      assertNotNull(borrow);
      assertEquals("file", borrow.getResourceType());
      assertEquals(5, borrow.getIndex());
      assertEquals(handle, borrow.getHandle());
    }

    @Test
    @DisplayName("Reject null handle in fromHandle")
    void testFromHandleRejectsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.fromHandle(null),
          "Should reject null handle");
    }

    @Test
    @DisplayName("Reject owned handle in fromHandle")
    void testFromHandleRejectsOwned() {
      final ComponentResourceHandle owned = ComponentResourceHandle.own("file", 5);
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.fromHandle(owned),
          "Should reject owned handle");
    }

    @Test
    @DisplayName("toJava returns ComponentResourceHandle")
    void testToJava() {
      final WitBorrow borrow = WitBorrow.of("resource", 7);
      final Object result = borrow.toJava();

      assertTrue(result instanceof ComponentResourceHandle);
      assertEquals(borrow.getHandle(), result);
    }

    @Test
    @DisplayName("toString provides readable representation")
    void testToString() {
      final WitBorrow borrow = WitBorrow.of("my-resource", 456);
      final String str = borrow.toString();

      assertTrue(str.contains("WitBorrow"), "Should contain class name");
      assertTrue(str.contains("my-resource"), "Should contain resource type");
      assertTrue(str.contains("456"), "Should contain index");
    }

    @Test
    @DisplayName("equals and hashCode work correctly")
    void testEqualsAndHashCode() {
      final WitBorrow borrow1 = WitBorrow.of("resource", 5);
      final WitBorrow borrow2 = WitBorrow.of("resource", 5);
      final WitBorrow borrow3 = WitBorrow.of("resource", 6);
      final WitBorrow borrow4 = WitBorrow.of("other", 5);

      assertEquals(borrow1, borrow2, "Equal handles should be equal");
      assertEquals(borrow1.hashCode(), borrow2.hashCode(), "Equal handles should have same hash");
      assertNotEquals(borrow1, borrow3, "Different indices should not be equal");
      assertNotEquals(borrow1, borrow4, "Different types should not be equal");
      assertNotEquals(borrow1, null, "Should not equal null");
      assertNotEquals(borrow1, "not a WitBorrow", "Should not equal other types");
    }
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("Serialize WitOwn to binary format with i64 handle")
    void testSerializeOwn() throws ValidationException {
      final WitOwn own = WitOwn.of("file-handle", 42);
      final byte[] result = WitValueSerializer.serialize(own);

      assertNotNull(result, "Serialized result should not be null");

      // Format: [type_name_length: u32 LE][type_name: UTF-8][handle: i64 LE]
      final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
      final int typeNameLength = buffer.getInt();
      assertEquals("file-handle".length(), typeNameLength, "Type name length should match");

      final byte[] typeNameBytes = new byte[typeNameLength];
      buffer.get(typeNameBytes);
      assertEquals("file-handle", new String(typeNameBytes, StandardCharsets.UTF_8));

      assertEquals(42L, buffer.getLong(), "Handle should be serialized as i64");
    }

    @Test
    @DisplayName("Serialize WitBorrow to binary format with i64 handle")
    void testSerializeBorrow() throws ValidationException {
      final WitBorrow borrow = WitBorrow.of("socket", 100);
      final byte[] result = WitValueSerializer.serialize(borrow);

      assertNotNull(result, "Serialized result should not be null");

      // Format: [type_name_length: u32 LE][type_name: UTF-8][handle: i64 LE]
      final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
      final int typeNameLength = buffer.getInt();
      assertEquals("socket".length(), typeNameLength, "Type name length should match");

      final byte[] typeNameBytes = new byte[typeNameLength];
      buffer.get(typeNameBytes);
      assertEquals("socket", new String(typeNameBytes, StandardCharsets.UTF_8));

      assertEquals(100L, buffer.getLong(), "Handle should be serialized as i64");
    }

    @Test
    @DisplayName("Get type discriminator for WitOwn")
    void testOwnTypeDiscriminator() throws ValidationException {
      final WitOwn own = WitOwn.of("resource", 1);
      assertEquals(22, WitValueSerializer.getTypeDiscriminator(own));
    }

    @Test
    @DisplayName("Get type discriminator for WitBorrow")
    void testBorrowTypeDiscriminator() throws ValidationException {
      final WitBorrow borrow = WitBorrow.of("resource", 1);
      assertEquals(23, WitValueSerializer.getTypeDiscriminator(borrow));
    }
  }

  @Nested
  @DisplayName("Deserialization Tests")
  class DeserializationTests {

    @Test
    @DisplayName("Deserialize WitOwn from binary format with i64 handle")
    void testDeserializeOwn() throws ValidationException {
      // Create serialized data: [type_name_length: u32 LE][type_name: UTF-8][handle: i64 LE]
      final String resourceType = "file-handle";
      final byte[] typeBytes = resourceType.getBytes(StandardCharsets.UTF_8);
      final ByteBuffer buffer =
          ByteBuffer.allocate(4 + typeBytes.length + 8).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(typeBytes.length);
      buffer.put(typeBytes);
      buffer.putLong(42L);

      final WitValue result = WitValueDeserializer.deserialize(22, buffer.array());

      assertTrue(result instanceof WitOwn, "Should deserialize to WitOwn");
      final WitOwn own = (WitOwn) result;
      assertEquals("file-handle", own.getResourceType(), "Resource type should match");
      assertEquals(42L, own.getHandle().getNativeHandle(), "Native handle should match");
    }

    @Test
    @DisplayName("Deserialize WitBorrow from binary format with i64 handle")
    void testDeserializeBorrow() throws ValidationException {
      // Create serialized data: [type_name_length: u32 LE][type_name: UTF-8][handle: i64 LE]
      final String resourceType = "socket";
      final byte[] typeBytes = resourceType.getBytes(StandardCharsets.UTF_8);
      final ByteBuffer buffer =
          ByteBuffer.allocate(4 + typeBytes.length + 8).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(typeBytes.length);
      buffer.put(typeBytes);
      buffer.putLong(100L);

      final WitValue result = WitValueDeserializer.deserialize(23, buffer.array());

      assertTrue(result instanceof WitBorrow, "Should deserialize to WitBorrow");
      final WitBorrow borrow = (WitBorrow) result;
      assertEquals("socket", borrow.getResourceType(), "Resource type should match");
      assertEquals(100L, borrow.getHandle().getNativeHandle(), "Native handle should match");
    }

    @Test
    @DisplayName("Reject truncated own data")
    void testRejectTruncatedOwnData() {
      final byte[] truncatedData = new byte[] {0, 0, 0, 5};
      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(22, truncatedData),
          "Should reject truncated data");
    }

    @Test
    @DisplayName("Reject truncated borrow data")
    void testRejectTruncatedBorrowData() {
      final byte[] truncatedData = new byte[] {0, 0, 0, 5};
      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(23, truncatedData),
          "Should reject truncated data");
    }
  }

  @Nested
  @DisplayName("Round-trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("Round-trip WitOwn through serialization")
    void testOwnRoundTrip() throws ValidationException {
      final WitOwn original = WitOwn.of("database-connection", 999);

      final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
      final byte[] serialized = WitValueSerializer.serialize(original);
      final WitValue deserialized = WitValueDeserializer.deserialize(discriminator, serialized);

      assertTrue(deserialized instanceof WitOwn);
      final WitOwn result = (WitOwn) deserialized;
      assertEquals(original.getResourceType(), result.getResourceType());
      assertEquals(original.getIndex(), result.getIndex());
    }

    @Test
    @DisplayName("Round-trip WitBorrow through serialization")
    void testBorrowRoundTrip() throws ValidationException {
      final WitBorrow original = WitBorrow.of("network-stream", 12345);

      final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
      final byte[] serialized = WitValueSerializer.serialize(original);
      final WitValue deserialized = WitValueDeserializer.deserialize(discriminator, serialized);

      assertTrue(deserialized instanceof WitBorrow);
      final WitBorrow result = (WitBorrow) deserialized;
      assertEquals(original.getResourceType(), result.getResourceType());
      assertEquals(original.getIndex(), result.getIndex());
    }

    @Test
    @DisplayName("Round-trip with Unicode resource type name")
    void testUnicodeResourceTypeRoundTrip() throws ValidationException {
      final WitOwn original = WitOwn.of("文件句柄", 42);

      final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
      final byte[] serialized = WitValueSerializer.serialize(original);
      final WitValue deserialized = WitValueDeserializer.deserialize(discriminator, serialized);

      assertTrue(deserialized instanceof WitOwn);
      final WitOwn result = (WitOwn) deserialized;
      assertEquals("文件句柄", result.getResourceType());
      assertEquals(42L, result.getHandle().getNativeHandle(), "Native handle should match");
    }

    @Test
    @DisplayName("Round-trip WitOwn with handle value exceeding Integer.MAX_VALUE")
    void testOwnRoundTripWithLargeHandle() throws ValidationException {
      final long largeHandle = (long) Integer.MAX_VALUE + 100L;
      final WitOwn original = WitOwn.of("large-resource", largeHandle);

      assertEquals(
          largeHandle,
          original.getHandle().getNativeHandle(),
          "Native handle should preserve large value");

      final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
      final byte[] serialized = WitValueSerializer.serialize(original);
      final WitValue deserialized = WitValueDeserializer.deserialize(discriminator, serialized);

      assertTrue(deserialized instanceof WitOwn, "Should deserialize to WitOwn");
      final WitOwn result = (WitOwn) deserialized;
      assertEquals("large-resource", result.getResourceType(), "Resource type should match");
      assertEquals(
          largeHandle,
          result.getHandle().getNativeHandle(),
          "Large handle should survive round-trip");
    }

    @Test
    @DisplayName("Round-trip WitBorrow with handle value exceeding Integer.MAX_VALUE")
    void testBorrowRoundTripWithLargeHandle() throws ValidationException {
      final long largeHandle = (long) Integer.MAX_VALUE + 500L;
      final WitBorrow original = WitBorrow.of("large-borrow", largeHandle);

      assertEquals(
          largeHandle,
          original.getHandle().getNativeHandle(),
          "Native handle should preserve large value");

      final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
      final byte[] serialized = WitValueSerializer.serialize(original);
      final WitValue deserialized = WitValueDeserializer.deserialize(discriminator, serialized);

      assertTrue(deserialized instanceof WitBorrow, "Should deserialize to WitBorrow");
      final WitBorrow result = (WitBorrow) deserialized;
      assertEquals("large-borrow", result.getResourceType(), "Resource type should match");
      assertEquals(
          largeHandle,
          result.getHandle().getNativeHandle(),
          "Large handle should survive round-trip");
    }
  }

  @Nested
  @DisplayName("Long Handle Factory Tests")
  class LongHandleFactoryTests {

    @Test
    @DisplayName("WitOwn.of(String, long) creates handle with nativeHandle preserved")
    void testOwnOfLongHandle() {
      final long handle = 5_000_000_000L;
      final WitOwn own = WitOwn.of("native-resource", handle);

      assertNotNull(own, "WitOwn should not be null");
      assertEquals("native-resource", own.getResourceType(), "Resource type should match");
      assertEquals(
          handle,
          own.getHandle().getNativeHandle(),
          "Native handle should preserve the long value");
      assertTrue(own.getHandle().isOwned(), "Handle should be owned");
    }

    @Test
    @DisplayName("WitBorrow.of(String, long) creates handle with nativeHandle preserved")
    void testBorrowOfLongHandle() {
      final long handle = 5_000_000_000L;
      final WitBorrow borrow = WitBorrow.of("native-resource", handle);

      assertNotNull(borrow, "WitBorrow should not be null");
      assertEquals("native-resource", borrow.getResourceType(), "Resource type should match");
      assertEquals(
          handle,
          borrow.getHandle().getNativeHandle(),
          "Native handle should preserve the long value");
      assertFalse(borrow.getHandle().isOwned(), "Handle should be borrowed");
    }

    @Test
    @DisplayName("WitOwn.of(String, long) with value fitting in int should still use nativeHandle")
    void testOwnOfLongHandleSmallValue() {
      final WitOwn own = WitOwn.of("small-resource", 42L);

      assertEquals(42L, own.getHandle().getNativeHandle(), "Native handle should be 42");
    }

    @Test
    @DisplayName(
        "WitBorrow.of(String, long) with value fitting in int should still use nativeHandle")
    void testBorrowOfLongHandleSmallValue() {
      final WitBorrow borrow = WitBorrow.of("small-resource", 42L);

      assertEquals(42L, borrow.getHandle().getNativeHandle(), "Native handle should be 42");
    }
  }

  @Nested
  @DisplayName("ComponentResourceHandle NativeHandle Tests")
  class NativeHandleTests {

    @Test
    @DisplayName("ownWithNativeHandle preserves nativeHandle value")
    void testOwnWithNativeHandle() {
      final long nativeHandle = 9_876_543_210L;
      final ComponentResourceHandle handle =
          ComponentResourceHandle.ownWithNativeHandle("resource", 0, nativeHandle);

      assertEquals(
          nativeHandle,
          handle.getNativeHandle(),
          "getNativeHandle should return the nativeHandle value");
      assertTrue(handle.isOwned(), "Handle should be owned");
      assertEquals("resource", handle.getResourceType(), "Resource type should match");
    }

    @Test
    @DisplayName("borrowWithNativeHandle preserves nativeHandle value")
    void testBorrowWithNativeHandle() {
      final long nativeHandle = 9_876_543_210L;
      final ComponentResourceHandle handle =
          ComponentResourceHandle.borrowWithNativeHandle("resource", 0, nativeHandle);

      assertEquals(
          nativeHandle,
          handle.getNativeHandle(),
          "getNativeHandle should return the nativeHandle value");
      assertFalse(handle.isOwned(), "Handle should be borrowed");
      assertEquals("resource", handle.getResourceType(), "Resource type should match");
    }

    @Test
    @DisplayName("own(type, index) returns -1 for nativeHandle")
    void testOwnDefaultNativeHandle() {
      final ComponentResourceHandle handle = ComponentResourceHandle.own("resource", 5);

      assertEquals(
          -1L, handle.getNativeHandle(), "Default own handle should return -1 for nativeHandle");
    }

    @Test
    @DisplayName("borrow(type, index) returns -1 for nativeHandle")
    void testBorrowDefaultNativeHandle() {
      final ComponentResourceHandle handle = ComponentResourceHandle.borrow("resource", 5);

      assertEquals(
          -1L, handle.getNativeHandle(), "Default borrow handle should return -1 for nativeHandle");
    }
  }
}
