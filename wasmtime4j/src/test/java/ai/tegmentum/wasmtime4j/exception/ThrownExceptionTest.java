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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.TagType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ThrownException} class.
 *
 * <p>This test class verifies the construction and behavior of the ThrownException value object,
 * which represents WebAssembly exception data.
 */
@DisplayName("ThrownException Tests")
class ThrownExceptionTest {

  /** Simple stub implementation of Tag interface for testing. */
  private static final class StubTag implements Tag {
    private final String id;

    StubTag(final String id) {
      this.id = id;
    }

    @Override
    public TagType getType(final Store store) {
      return null;
    }

    @Override
    public boolean equals(final Tag other, final Store store) {
      if (other == null) {
        return false;
      }
      if (other instanceof StubTag) {
        return id.equals(((StubTag) other).id);
      }
      return false;
    }

    @Override
    public long getNativeHandle() {
      return 0;
    }

    @Override
    public String toString() {
      return "StubTag[" + id + "]";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof StubTag)) {
        return false;
      }
      return id.equals(((StubTag) obj).id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }
  }

  /** Simple stub implementation of ExnRef interface for testing. */
  private static final class StubExnRef implements ExnRef {
    private final String id;

    StubExnRef(final String id) {
      this.id = id;
    }

    @Override
    public Tag getTag(final Store store) {
      return null;
    }

    @Override
    public long getNativeHandle() {
      return 0;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public String toString() {
      return "StubExnRef[" + id + "]";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof StubExnRef)) {
        return false;
      }
      return id.equals(((StubExnRef) obj).id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }
  }

  /** Creates a simple stub WasmValue for testing. */
  private static WasmValue createStubWasmValue(final int value) {
    return WasmValue.i32(value);
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ThrownException should be final")
    void shouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ThrownException.class.getModifiers()),
          "ThrownException should be final");
    }

    @Test
    @DisplayName("ThrownException should NOT extend Throwable")
    void shouldNotExtendThrowable() {
      assertFalse(
          Throwable.class.isAssignableFrom(ThrownException.class),
          "ThrownException should NOT extend Throwable - it's a value object");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with tag, payload, and exnRef should set all fields")
    void constructorWithAllFieldsShouldSetAll() {
      final Tag tag = new StubTag("tag1");
      final WasmValue value = createStubWasmValue(42);
      final List<WasmValue> payload = Arrays.asList(value);
      final ExnRef exnRef = new StubExnRef("exn1");

      final ThrownException exception = new ThrownException(tag, payload, exnRef);

      assertSame(tag, exception.getTag(), "Tag should match");
      assertEquals(1, exception.getPayload().size(), "Payload size should be 1");
      assertTrue(exception.getExnRef().isPresent(), "ExnRef should be present");
      assertSame(exnRef, exception.getExnRef().get(), "ExnRef should match");
    }

    @Test
    @DisplayName("Constructor with tag and payload should set fields")
    void constructorWithTagAndPayloadShouldSetFields() {
      final Tag tag = new StubTag("tag1");
      final WasmValue value = createStubWasmValue(42);
      final List<WasmValue> payload = Arrays.asList(value);

      final ThrownException exception = new ThrownException(tag, payload);

      assertSame(tag, exception.getTag(), "Tag should match");
      assertEquals(1, exception.getPayload().size(), "Payload size should be 1");
      assertFalse(exception.getExnRef().isPresent(), "ExnRef should not be present");
    }

    @Test
    @DisplayName("Constructor should throw NPE for null tag")
    void constructorShouldThrowNpeForNullTag() {
      final List<WasmValue> payload = Collections.emptyList();

      assertThrows(
          NullPointerException.class,
          () -> new ThrownException(null, payload),
          "Constructor should throw NPE for null tag");
    }

    @Test
    @DisplayName("Constructor should handle null payload")
    void constructorShouldHandleNullPayload() {
      final Tag tag = new StubTag("tag1");

      final ThrownException exception = new ThrownException(tag, null);

      assertNotNull(exception.getPayload(), "Payload should not be null");
      assertTrue(exception.getPayload().isEmpty(), "Payload should be empty");
    }

    @Test
    @DisplayName("Constructor should handle null exnRef")
    void constructorShouldHandleNullExnRef() {
      final Tag tag = new StubTag("tag1");

      final ThrownException exception = new ThrownException(tag, Collections.emptyList(), null);

      assertFalse(exception.getExnRef().isPresent(), "ExnRef should not be present");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getTag should return the tag")
    void getTagShouldReturnTag() {
      final Tag tag = new StubTag("tag1");
      final ThrownException exception = new ThrownException(tag, Collections.emptyList());

      assertSame(tag, exception.getTag(), "getTag should return the tag");
    }

    @Test
    @DisplayName("getPayload should return unmodifiable list")
    void getPayloadShouldReturnUnmodifiableList() {
      final Tag tag = new StubTag("tag1");
      final WasmValue value = createStubWasmValue(42);
      final List<WasmValue> payload = Arrays.asList(value);

      final ThrownException exception = new ThrownException(tag, payload);
      final List<WasmValue> retrievedPayload = exception.getPayload();

      assertThrows(
          UnsupportedOperationException.class,
          () -> retrievedPayload.add(createStubWasmValue(99)),
          "Payload should be unmodifiable");
    }

    @Test
    @DisplayName("hasPayload should return true when payload not empty")
    void hasPayloadShouldReturnTrueWhenPayloadNotEmpty() {
      final Tag tag = new StubTag("tag1");
      final WasmValue value = createStubWasmValue(42);

      final ThrownException exception = new ThrownException(tag, Arrays.asList(value));

      assertTrue(exception.hasPayload(), "hasPayload should return true");
    }

    @Test
    @DisplayName("hasPayload should return false when payload empty")
    void hasPayloadShouldReturnFalseWhenPayloadEmpty() {
      final Tag tag = new StubTag("tag1");

      final ThrownException exception = new ThrownException(tag, Collections.emptyList());

      assertFalse(exception.hasPayload(), "hasPayload should return false");
    }

    @Test
    @DisplayName("getPayloadValue should return value at index")
    void getPayloadValueShouldReturnValueAtIndex() {
      final Tag tag = new StubTag("tag1");
      final WasmValue value1 = createStubWasmValue(1);
      final WasmValue value2 = createStubWasmValue(2);

      final ThrownException exception = new ThrownException(tag, Arrays.asList(value1, value2));

      assertEquals(value1, exception.getPayloadValue(0), "First value should match");
      assertEquals(value2, exception.getPayloadValue(1), "Second value should match");
    }

    @Test
    @DisplayName("getPayloadValue should throw for invalid index")
    void getPayloadValueShouldThrowForInvalidIndex() {
      final Tag tag = new StubTag("tag1");

      final ThrownException exception = new ThrownException(tag, Collections.emptyList());

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> exception.getPayloadValue(0),
          "Should throw for invalid index");
    }

    @Test
    @DisplayName("getPayloadSize should return correct size")
    void getPayloadSizeShouldReturnCorrectSize() {
      final Tag tag = new StubTag("tag1");
      final WasmValue value1 = createStubWasmValue(1);
      final WasmValue value2 = createStubWasmValue(2);
      final WasmValue value3 = createStubWasmValue(3);

      final ThrownException exception =
          new ThrownException(tag, Arrays.asList(value1, value2, value3));

      assertEquals(3, exception.getPayloadSize(), "Payload size should be 3");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include tag")
    void toStringShouldIncludeTag() {
      final Tag tag = new StubTag("testTag");
      final ThrownException exception = new ThrownException(tag, Collections.emptyList());

      final String result = exception.toString();

      assertTrue(result.contains("ThrownException"), "toString should include class name");
      assertTrue(result.contains("tag="), "toString should include tag");
    }

    @Test
    @DisplayName("toString should include payload info")
    void toStringShouldIncludePayloadInfo() {
      final Tag tag = new StubTag("testTag");
      final ThrownException exception = new ThrownException(tag, Collections.emptyList());

      final String result = exception.toString();

      assertTrue(result.contains("payload="), "toString should include payload");
    }

    @Test
    @DisplayName("toString should include exnRef status")
    void toStringShouldIncludeExnRefStatus() {
      final Tag tag = new StubTag("testTag");
      final ExnRef exnRef = new StubExnRef("exn1");
      final ThrownException withRef = new ThrownException(tag, Collections.emptyList(), exnRef);
      final ThrownException withoutRef = new ThrownException(tag, Collections.emptyList());

      assertTrue(
          withRef.toString().contains("hasExnRef=true"), "toString should show hasExnRef=true");
      assertTrue(
          withoutRef.toString().contains("hasExnRef=false"),
          "toString should show hasExnRef=false");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for same object")
    void equalsShouldReturnTrueForSameObject() {
      final Tag tag = new StubTag("tag1");
      final ThrownException exception = new ThrownException(tag, Collections.emptyList());

      assertEquals(exception, exception, "Same object should be equal");
    }

    @Test
    @DisplayName("equals should return true for equivalent objects")
    void equalsShouldReturnTrueForEquivalentObjects() {
      final Tag tag = new StubTag("tag1");
      final List<WasmValue> payload = Collections.emptyList();

      final ThrownException exception1 = new ThrownException(tag, payload);
      final ThrownException exception2 = new ThrownException(tag, payload);

      assertEquals(exception1, exception2, "Equivalent objects should be equal");
    }

    @Test
    @DisplayName("equals should return false for different tags")
    void equalsShouldReturnFalseForDifferentTags() {
      final Tag tag1 = new StubTag("tag1");
      final Tag tag2 = new StubTag("tag2");

      final ThrownException exception1 = new ThrownException(tag1, Collections.emptyList());
      final ThrownException exception2 = new ThrownException(tag2, Collections.emptyList());

      assertNotEquals(exception1, exception2, "Different tags should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final Tag tag = new StubTag("tag1");
      final ThrownException exception = new ThrownException(tag, Collections.emptyList());

      assertNotEquals(null, exception, "Should not equal null");
    }

    @Test
    @DisplayName("hashCode should be consistent for equivalent objects")
    void hashCodeShouldBeConsistentForEquivalentObjects() {
      final Tag tag = new StubTag("tag1");
      final List<WasmValue> payload = Collections.emptyList();

      final ThrownException exception1 = new ThrownException(tag, payload);
      final ThrownException exception2 = new ThrownException(tag, payload);

      assertEquals(
          exception1.hashCode(),
          exception2.hashCode(),
          "Equivalent objects should have same hashCode");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create ThrownException")
    void builderShouldCreateThrownException() {
      final Tag tag = new StubTag("tag1");

      final ThrownException exception = ThrownException.builder(tag).build();

      assertSame(tag, exception.getTag(), "Tag should match");
      assertTrue(exception.getPayload().isEmpty(), "Default payload should be empty");
      assertFalse(exception.getExnRef().isPresent(), "Default exnRef should be absent");
    }

    @Test
    @DisplayName("builder should set payload")
    void builderShouldSetPayload() {
      final Tag tag = new StubTag("tag1");
      final WasmValue value = createStubWasmValue(42);

      final ThrownException exception =
          ThrownException.builder(tag).payload(Arrays.asList(value)).build();

      assertEquals(1, exception.getPayloadSize(), "Payload size should be 1");
    }

    @Test
    @DisplayName("builder should set exnRef")
    void builderShouldSetExnRef() {
      final Tag tag = new StubTag("tag1");
      final ExnRef exnRef = new StubExnRef("exn1");

      final ThrownException exception = ThrownException.builder(tag).exnRef(exnRef).build();

      assertTrue(exception.getExnRef().isPresent(), "ExnRef should be present");
      assertSame(exnRef, exception.getExnRef().get(), "ExnRef should match");
    }

    @Test
    @DisplayName("builder should throw NPE for null tag")
    void builderShouldThrowNpeForNullTag() {
      assertThrows(
          NullPointerException.class,
          () -> ThrownException.builder(null),
          "Builder should throw NPE for null tag");
    }
  }
}
