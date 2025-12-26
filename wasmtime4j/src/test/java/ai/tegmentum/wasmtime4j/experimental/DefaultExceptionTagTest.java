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

package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultExceptionTag} class.
 *
 * <p>DefaultExceptionTag provides the default implementation of ExceptionHandler.ExceptionTag.
 */
@DisplayName("DefaultExceptionTag Tests")
class DefaultExceptionTagTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(DefaultExceptionTag.class.getModifiers()),
          "DefaultExceptionTag should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(DefaultExceptionTag.class.getModifiers()),
          "DefaultExceptionTag should be public");
    }

    @Test
    @DisplayName("should implement ExceptionTag interface")
    void shouldImplementExceptionTagInterface() {
      assertTrue(
          ExceptionHandler.ExceptionTag.class.isAssignableFrom(DefaultExceptionTag.class),
          "DefaultExceptionTag should implement ExceptionTag");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create tag with all parameters")
    void shouldCreateTagWithAllParameters() {
      final List<WasmValueType> paramTypes = Arrays.asList(WasmValueType.I32, WasmValueType.I64);
      final DefaultExceptionTag tag = new DefaultExceptionTag(123L, "test-tag", paramTypes, true);

      assertEquals(123L, tag.getTagHandle(), "Tag handle should match");
      assertEquals("test-tag", tag.getTagName(), "Tag name should match");
      assertEquals(paramTypes, tag.getParameterTypes(), "Parameter types should match");
      assertTrue(tag.isGcAware(), "GC aware should be true");
    }

    @Test
    @DisplayName("should create tag with three parameters (no GC)")
    void shouldCreateTagWithThreeParameters() {
      final List<WasmValueType> paramTypes = Collections.singletonList(WasmValueType.F32);
      final DefaultExceptionTag tag = new DefaultExceptionTag(456L, "another-tag", paramTypes);

      assertEquals(456L, tag.getTagHandle(), "Tag handle should match");
      assertEquals("another-tag", tag.getTagName(), "Tag name should match");
      assertEquals(paramTypes, tag.getParameterTypes(), "Parameter types should match");
      assertFalse(tag.isGcAware(), "GC aware should be false by default");
    }

    @Test
    @DisplayName("should throw on null tag name")
    void shouldThrowOnNullTagName() {
      assertThrows(
          NullPointerException.class,
          () -> new DefaultExceptionTag(1L, null, Collections.emptyList()),
          "Should throw on null tag name");
    }

    @Test
    @DisplayName("should throw on null parameter types")
    void shouldThrowOnNullParameterTypes() {
      assertThrows(
          NullPointerException.class,
          () -> new DefaultExceptionTag(1L, "test", null),
          "Should throw on null parameter types");
    }

    @Test
    @DisplayName("should throw on empty tag name")
    void shouldThrowOnEmptyTagName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DefaultExceptionTag(1L, "", Collections.emptyList()),
          "Should throw on empty tag name");
    }

    @Test
    @DisplayName("should throw on whitespace-only tag name")
    void shouldThrowOnWhitespaceOnlyTagName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DefaultExceptionTag(1L, "   ", Collections.emptyList()),
          "Should throw on whitespace-only tag name");
    }

    @Test
    @DisplayName("should trim tag name")
    void shouldTrimTagName() {
      final DefaultExceptionTag tag =
          new DefaultExceptionTag(1L, "  trimmed  ", Collections.emptyList());
      assertEquals("trimmed", tag.getTagName(), "Tag name should be trimmed");
    }
  }

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("getTagType should return empty brackets for no parameters")
    void getTagTypeShouldReturnEmptyBracketsForNoParameters() {
      final DefaultExceptionTag tag = new DefaultExceptionTag(1L, "empty", Collections.emptyList());
      assertEquals("[]", tag.getTagType(), "Tag type should be empty brackets");
    }

    @Test
    @DisplayName("getTagType should return formatted parameter types")
    void getTagTypeShouldReturnFormattedParameterTypes() {
      final List<WasmValueType> paramTypes = Arrays.asList(WasmValueType.I32, WasmValueType.I64);
      final DefaultExceptionTag tag = new DefaultExceptionTag(1L, "typed", paramTypes);
      assertEquals("[I32, I64]", tag.getTagType(), "Tag type should be formatted");
    }

    @Test
    @DisplayName("getParameterTypes should return unmodifiable list")
    void getParameterTypesShouldReturnUnmodifiableList() {
      final List<WasmValueType> paramTypes = Arrays.asList(WasmValueType.I32, WasmValueType.I64);
      final DefaultExceptionTag tag = new DefaultExceptionTag(1L, "test", paramTypes);

      assertThrows(
          UnsupportedOperationException.class,
          () -> tag.getParameterTypes().add(WasmValueType.F32),
          "Parameter types list should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final DefaultExceptionTag tag = new DefaultExceptionTag(1L, "test", Collections.emptyList());
      assertEquals(tag, tag, "Tag should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to tag with same handle")
    void shouldBeEqualToTagWithSameHandle() {
      final DefaultExceptionTag tag1 =
          new DefaultExceptionTag(1L, "test1", Collections.emptyList());
      final DefaultExceptionTag tag2 =
          new DefaultExceptionTag(1L, "test2", Arrays.asList(WasmValueType.I32), true);
      assertEquals(tag1, tag2, "Tags with same handle should be equal");
    }

    @Test
    @DisplayName("should not be equal to tag with different handle")
    void shouldNotBeEqualToTagWithDifferentHandle() {
      final DefaultExceptionTag tag1 = new DefaultExceptionTag(1L, "test", Collections.emptyList());
      final DefaultExceptionTag tag2 = new DefaultExceptionTag(2L, "test", Collections.emptyList());
      assertNotEquals(tag1, tag2, "Tags with different handles should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final DefaultExceptionTag tag = new DefaultExceptionTag(1L, "test", Collections.emptyList());
      assertNotEquals(null, tag, "Tag should not be equal to null");
    }

    @Test
    @DisplayName("should have consistent hash code based on handle")
    void shouldHaveConsistentHashCodeBasedOnHandle() {
      final DefaultExceptionTag tag1 =
          new DefaultExceptionTag(1L, "test1", Collections.emptyList());
      final DefaultExceptionTag tag2 =
          new DefaultExceptionTag(1L, "test2", Arrays.asList(WasmValueType.I32));
      assertEquals(
          tag1.hashCode(), tag2.hashCode(), "Tags with same handle should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain handle")
    void toStringShouldContainHandle() {
      final DefaultExceptionTag tag =
          new DefaultExceptionTag(123L, "test", Collections.emptyList());
      assertTrue(tag.toString().contains("123"), "toString should contain handle");
    }

    @Test
    @DisplayName("toString should contain name")
    void toStringShouldContainName() {
      final DefaultExceptionTag tag =
          new DefaultExceptionTag(1L, "my-tag", Collections.emptyList());
      assertTrue(tag.toString().contains("my-tag"), "toString should contain name");
    }

    @Test
    @DisplayName("toString should contain gcAware")
    void toStringShouldContainGcAware() {
      final DefaultExceptionTag tag =
          new DefaultExceptionTag(1L, "test", Collections.emptyList(), true);
      assertTrue(tag.toString().contains("gcAware=true"), "toString should contain gcAware");
    }

    @Test
    @DisplayName("toString should not be null")
    void toStringShouldNotBeNull() {
      final DefaultExceptionTag tag = new DefaultExceptionTag(1L, "test", Collections.emptyList());
      assertNotNull(tag.toString(), "toString should not be null");
    }
  }
}
