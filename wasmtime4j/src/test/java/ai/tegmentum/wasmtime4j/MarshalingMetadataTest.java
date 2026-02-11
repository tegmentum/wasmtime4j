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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.MarshalingMetadata;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MarshalingMetadata} class.
 *
 * <p>MarshalingMetadata is a metadata container for complex marshaling operations with all-Optional
 * fields and a builder pattern. Fields include array dimensions, component/key/value types,
 * collection size, encoding, memory passing, and alignment.
 */
@DisplayName("MarshalingMetadata Tests")
class MarshalingMetadataTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(MarshalingMetadata.class.getModifiers()),
          "MarshalingMetadata should be public");
      assertTrue(
          Modifier.isFinal(MarshalingMetadata.class.getModifiers()),
          "MarshalingMetadata should be final");
    }

    @Test
    @DisplayName("should have static builder() method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final var method = MarshalingMetadata.class.getMethod("builder");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder() should be static");
    }
  }

  @Nested
  @DisplayName("Builder Default Values Tests")
  class BuilderDefaultValuesTests {

    @Test
    @DisplayName("should build with all fields empty by default")
    void shouldBuildWithAllFieldsEmpty() {
      final MarshalingMetadata metadata = MarshalingMetadata.builder().build();

      assertNotNull(metadata, "Metadata should not be null");
      assertFalse(metadata.getArrayDimensions().isPresent(), "arrayDimensions should be empty");
      assertFalse(metadata.getComponentType().isPresent(), "componentType should be empty");
      assertFalse(metadata.getCollectionSize().isPresent(), "collectionSize should be empty");
      assertFalse(metadata.getKeyType().isPresent(), "keyType should be empty");
      assertFalse(metadata.getValueType().isPresent(), "valueType should be empty");
      assertFalse(metadata.getObjectClassName().isPresent(), "objectClassName should be empty");
      assertFalse(metadata.getDataSize().isPresent(), "dataSize should be empty");
      assertFalse(metadata.getStringEncoding().isPresent(), "stringEncoding should be empty");
      assertFalse(metadata.shouldUseMemoryPassing().isPresent(), "memoryPassing should be empty");
      assertFalse(metadata.getMemoryAlignment().isPresent(), "memoryAlignment should be empty");
    }
  }

  @Nested
  @DisplayName("Builder Field Setting Tests")
  class BuilderFieldSettingTests {

    @Test
    @DisplayName("should set arrayDimensions")
    void shouldSetArrayDimensions() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withArrayDimensions(3).build();

      assertTrue(metadata.getArrayDimensions().isPresent(), "arrayDimensions should be present");
      assertEquals(3, metadata.getArrayDimensions().get(), "arrayDimensions should be 3");
    }

    @Test
    @DisplayName("should set componentType")
    void shouldSetComponentType() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withComponentType(Integer.class).build();

      assertTrue(metadata.getComponentType().isPresent(), "componentType should be present");
      assertEquals(
          Integer.class,
          metadata.getComponentType().get(),
          "componentType should be Integer.class");
    }

    @Test
    @DisplayName("should set collectionSize")
    void shouldSetCollectionSize() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withCollectionSize(100).build();

      assertTrue(metadata.getCollectionSize().isPresent(), "collectionSize should be present");
      assertEquals(100, metadata.getCollectionSize().get(), "collectionSize should be 100");
    }

    @Test
    @DisplayName("should set keyType and valueType for maps")
    void shouldSetKeyAndValueTypes() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder()
              .withKeyType(String.class)
              .withValueType(Integer.class)
              .build();

      assertTrue(metadata.getKeyType().isPresent(), "keyType should be present");
      assertEquals(String.class, metadata.getKeyType().get(), "keyType should be String.class");
      assertTrue(metadata.getValueType().isPresent(), "valueType should be present");
      assertEquals(
          Integer.class, metadata.getValueType().get(), "valueType should be Integer.class");
    }

    @Test
    @DisplayName("should set objectClassName")
    void shouldSetObjectClassName() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withObjectClassName("com.example.MyClass").build();

      assertTrue(metadata.getObjectClassName().isPresent(), "objectClassName should be present");
      assertEquals(
          "com.example.MyClass",
          metadata.getObjectClassName().get(),
          "objectClassName should match");
    }

    @Test
    @DisplayName("should set dataSize")
    void shouldSetDataSize() {
      final MarshalingMetadata metadata = MarshalingMetadata.builder().withDataSize(4096).build();

      assertTrue(metadata.getDataSize().isPresent(), "dataSize should be present");
      assertEquals(4096, metadata.getDataSize().get(), "dataSize should be 4096");
    }

    @Test
    @DisplayName("should set stringEncoding")
    void shouldSetStringEncoding() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withStringEncoding("UTF-8").build();

      assertTrue(metadata.getStringEncoding().isPresent(), "stringEncoding should be present");
      assertEquals("UTF-8", metadata.getStringEncoding().get(), "stringEncoding should be 'UTF-8'");
    }

    @Test
    @DisplayName("should set memoryPassing flag")
    void shouldSetMemoryPassing() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withMemoryPassing(true).build();

      assertTrue(metadata.shouldUseMemoryPassing().isPresent(), "memoryPassing should be present");
      assertTrue(metadata.shouldUseMemoryPassing().get(), "memoryPassing should be true");
    }

    @Test
    @DisplayName("should set memoryAlignment")
    void shouldSetMemoryAlignment() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withMemoryAlignment(16).build();

      assertTrue(metadata.getMemoryAlignment().isPresent(), "memoryAlignment should be present");
      assertEquals(16, metadata.getMemoryAlignment().get(), "memoryAlignment should be 16");
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("should reject non-power-of-2 memoryAlignment")
    void shouldRejectNonPowerOf2Alignment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingMetadata.builder().withMemoryAlignment(3),
          "Non-power-of-2 alignment should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should reject zero memoryAlignment")
    void shouldRejectZeroAlignment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingMetadata.builder().withMemoryAlignment(0),
          "Zero alignment should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should reject negative memoryAlignment")
    void shouldRejectNegativeAlignment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingMetadata.builder().withMemoryAlignment(-4),
          "Negative alignment should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should accept power-of-2 values for alignment")
    void shouldAcceptPowerOf2Alignment() {
      final int[] validAlignments = {1, 2, 4, 8, 16, 32, 64, 128, 256};
      for (final int alignment : validAlignments) {
        final MarshalingMetadata metadata =
            MarshalingMetadata.builder().withMemoryAlignment(alignment).build();
        assertEquals(
            alignment,
            metadata.getMemoryAlignment().get(),
            "Alignment " + alignment + " should be accepted");
      }
    }
  }

  @Nested
  @DisplayName("Complete Builder Chaining Tests")
  class CompleteBuilderChainingTests {

    @Test
    @DisplayName("should chain all builder methods together")
    void shouldChainAllMethods() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder()
              .withArrayDimensions(2)
              .withComponentType(Byte.class)
              .withCollectionSize(50)
              .withKeyType(String.class)
              .withValueType(Long.class)
              .withObjectClassName("test.Class")
              .withDataSize(2048)
              .withStringEncoding("UTF-16")
              .withMemoryPassing(false)
              .withMemoryAlignment(8)
              .build();

      assertEquals(2, metadata.getArrayDimensions().get(), "arrayDimensions should be 2");
      assertEquals(Byte.class, metadata.getComponentType().get(), "componentType should be Byte");
      assertEquals(50, metadata.getCollectionSize().get(), "collectionSize should be 50");
      assertEquals(String.class, metadata.getKeyType().get(), "keyType should be String");
      assertEquals(Long.class, metadata.getValueType().get(), "valueType should be Long");
      assertEquals("test.Class", metadata.getObjectClassName().get(), "objectClassName matches");
      assertEquals(2048, metadata.getDataSize().get(), "dataSize should be 2048");
      assertEquals("UTF-16", metadata.getStringEncoding().get(), "stringEncoding should be UTF-16");
      assertFalse(metadata.shouldUseMemoryPassing().get(), "memoryPassing should be false");
      assertEquals(8, metadata.getMemoryAlignment().get(), "memoryAlignment should be 8");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal metadata should be equal")
    void equalMetadataShouldBeEqual() {
      final MarshalingMetadata meta1 =
          MarshalingMetadata.builder()
              .withArrayDimensions(2)
              .withComponentType(Integer.class)
              .build();
      final MarshalingMetadata meta2 =
          MarshalingMetadata.builder()
              .withArrayDimensions(2)
              .withComponentType(Integer.class)
              .build();

      assertEquals(meta1, meta2, "Identical metadata should be equal");
      assertEquals(meta1.hashCode(), meta2.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("different metadata should not be equal")
    void differentMetadataShouldNotBeEqual() {
      final MarshalingMetadata meta1 = MarshalingMetadata.builder().withArrayDimensions(2).build();
      final MarshalingMetadata meta2 = MarshalingMetadata.builder().withArrayDimensions(3).build();

      assertNotEquals(meta1, meta2, "Different metadata should not be equal");
    }

    @Test
    @DisplayName("empty metadata instances should be equal")
    void emptyMetadataShouldBeEqual() {
      final MarshalingMetadata meta1 = MarshalingMetadata.builder().build();
      final MarshalingMetadata meta2 = MarshalingMetadata.builder().build();

      assertEquals(meta1, meta2, "Empty metadata should be equal");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain field information")
    void toStringShouldContainFieldInfo() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withArrayDimensions(2).withStringEncoding("UTF-8").build();

      final String result = metadata.toString();
      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("MarshalingMetadata"), "toString should contain class name");
      assertTrue(result.contains("2"), "toString should contain arrayDimensions");
      assertTrue(result.contains("UTF-8"), "toString should contain stringEncoding");
    }
  }
}
