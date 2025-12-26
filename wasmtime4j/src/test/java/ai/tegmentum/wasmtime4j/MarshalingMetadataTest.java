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

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link MarshalingMetadata} class.
 *
 * <p>This test class verifies the construction and behavior of the MarshalingMetadata, including
 * builder pattern, optional fields, and validation.
 */
@DisplayName("MarshalingMetadata Tests")
class MarshalingMetadataTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("MarshalingMetadata should be final")
    void shouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(MarshalingMetadata.class.getModifiers()),
          "MarshalingMetadata should be final");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should return non-null builder")
    void builderShouldReturnNonNullBuilder() {
      assertNotNull(MarshalingMetadata.builder(), "builder() should return non-null");
    }

    @Test
    @DisplayName("builder should build empty metadata")
    void builderShouldBuildEmptyMetadata() {
      final MarshalingMetadata metadata = MarshalingMetadata.builder().build();

      assertNotNull(metadata, "Metadata should not be null");
      assertFalse(metadata.getArrayDimensions().isPresent(), "Array dimensions should be absent");
      assertFalse(metadata.getComponentType().isPresent(), "Component type should be absent");
      assertFalse(metadata.getCollectionSize().isPresent(), "Collection size should be absent");
    }

    @Test
    @DisplayName("builder should set array dimensions")
    void builderShouldSetArrayDimensions() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withArrayDimensions(2).build();

      assertTrue(metadata.getArrayDimensions().isPresent(), "Array dimensions should be present");
      assertEquals(2, metadata.getArrayDimensions().get(), "Array dimensions should be 2");
    }

    @Test
    @DisplayName("builder should set component type")
    void builderShouldSetComponentType() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withComponentType(String.class).build();

      assertTrue(metadata.getComponentType().isPresent(), "Component type should be present");
      assertEquals(
          String.class, metadata.getComponentType().get(), "Component type should be String.class");
    }

    @Test
    @DisplayName("builder should set collection size")
    void builderShouldSetCollectionSize() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withCollectionSize(100).build();

      assertTrue(metadata.getCollectionSize().isPresent(), "Collection size should be present");
      assertEquals(100, metadata.getCollectionSize().get(), "Collection size should be 100");
    }

    @Test
    @DisplayName("builder should set key type")
    void builderShouldSetKeyType() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withKeyType(Integer.class).build();

      assertTrue(metadata.getKeyType().isPresent(), "Key type should be present");
      assertEquals(Integer.class, metadata.getKeyType().get(), "Key type should be Integer.class");
    }

    @Test
    @DisplayName("builder should set value type")
    void builderShouldSetValueType() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withValueType(String.class).build();

      assertTrue(metadata.getValueType().isPresent(), "Value type should be present");
      assertEquals(
          String.class, metadata.getValueType().get(), "Value type should be String.class");
    }

    @Test
    @DisplayName("builder should set object class name")
    void builderShouldSetObjectClassName() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withObjectClassName("com.example.MyClass").build();

      assertTrue(metadata.getObjectClassName().isPresent(), "Object class name should be present");
      assertEquals(
          "com.example.MyClass",
          metadata.getObjectClassName().get(),
          "Object class name should match");
    }

    @Test
    @DisplayName("builder should set data size")
    void builderShouldSetDataSize() {
      final MarshalingMetadata metadata = MarshalingMetadata.builder().withDataSize(1024).build();

      assertTrue(metadata.getDataSize().isPresent(), "Data size should be present");
      assertEquals(1024, metadata.getDataSize().get(), "Data size should be 1024");
    }

    @Test
    @DisplayName("builder should set string encoding")
    void builderShouldSetStringEncoding() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withStringEncoding("UTF-8").build();

      assertTrue(metadata.getStringEncoding().isPresent(), "String encoding should be present");
      assertEquals("UTF-8", metadata.getStringEncoding().get(), "String encoding should be UTF-8");
    }

    @Test
    @DisplayName("builder should set memory passing flag")
    void builderShouldSetMemoryPassingFlag() {
      final MarshalingMetadata metadataTrue =
          MarshalingMetadata.builder().withMemoryPassing(true).build();
      final MarshalingMetadata metadataFalse =
          MarshalingMetadata.builder().withMemoryPassing(false).build();

      assertTrue(
          metadataTrue.shouldUseMemoryPassing().isPresent(),
          "Memory passing flag should be present");
      assertTrue(metadataTrue.shouldUseMemoryPassing().get(), "Memory passing should be true");
      assertFalse(metadataFalse.shouldUseMemoryPassing().get(), "Memory passing should be false");
    }

    @Test
    @DisplayName("builder should set memory alignment")
    void builderShouldSetMemoryAlignment() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withMemoryAlignment(16).build();

      assertTrue(metadata.getMemoryAlignment().isPresent(), "Memory alignment should be present");
      assertEquals(16, metadata.getMemoryAlignment().get(), "Memory alignment should be 16");
    }

    @Test
    @DisplayName("builder should throw for non-power-of-2 alignment")
    void builderShouldThrowForNonPowerOf2Alignment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingMetadata.builder().withMemoryAlignment(3),
          "Should throw for non-power-of-2 alignment");

      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingMetadata.builder().withMemoryAlignment(0),
          "Should throw for zero alignment");

      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingMetadata.builder().withMemoryAlignment(-1),
          "Should throw for negative alignment");
    }

    @Test
    @DisplayName("builder should set all fields together")
    void builderShouldSetAllFieldsTogether() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder()
              .withArrayDimensions(2)
              .withComponentType(Integer.class)
              .withCollectionSize(50)
              .withKeyType(String.class)
              .withValueType(Object.class)
              .withObjectClassName("MyClass")
              .withDataSize(512)
              .withStringEncoding("UTF-16")
              .withMemoryPassing(true)
              .withMemoryAlignment(8)
              .build();

      assertEquals(2, metadata.getArrayDimensions().get(), "Array dimensions should be 2");
      assertEquals(
          Integer.class, metadata.getComponentType().get(), "Component type should be Integer");
      assertEquals(50, metadata.getCollectionSize().get(), "Collection size should be 50");
      assertEquals(String.class, metadata.getKeyType().get(), "Key type should be String");
      assertEquals(Object.class, metadata.getValueType().get(), "Value type should be Object");
      assertEquals(
          "MyClass", metadata.getObjectClassName().get(), "Object class name should be MyClass");
      assertEquals(512, metadata.getDataSize().get(), "Data size should be 512");
      assertEquals(
          "UTF-16", metadata.getStringEncoding().get(), "String encoding should be UTF-16");
      assertTrue(metadata.shouldUseMemoryPassing().get(), "Memory passing should be true");
      assertEquals(8, metadata.getMemoryAlignment().get(), "Memory alignment should be 8");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getArrayDimensions should return Optional")
    void getArrayDimensionsShouldReturnOptional() {
      final MarshalingMetadata withDimensions =
          MarshalingMetadata.builder().withArrayDimensions(3).build();
      final MarshalingMetadata withoutDimensions = MarshalingMetadata.builder().build();

      assertEquals(
          Optional.of(3), withDimensions.getArrayDimensions(), "Should return Optional with value");
      assertEquals(
          Optional.empty(), withoutDimensions.getArrayDimensions(), "Should return empty Optional");
    }

    @Test
    @DisplayName("getComponentType should return Optional")
    void getComponentTypeShouldReturnOptional() {
      final MarshalingMetadata with =
          MarshalingMetadata.builder().withComponentType(Long.class).build();
      final MarshalingMetadata without = MarshalingMetadata.builder().build();

      assertEquals(
          Optional.of(Long.class), with.getComponentType(), "Should return Optional with value");
      assertEquals(Optional.empty(), without.getComponentType(), "Should return empty Optional");
    }

    @Test
    @DisplayName("getCollectionSize should return Optional")
    void getCollectionSizeShouldReturnOptional() {
      final MarshalingMetadata with = MarshalingMetadata.builder().withCollectionSize(25).build();
      final MarshalingMetadata without = MarshalingMetadata.builder().build();

      assertEquals(Optional.of(25), with.getCollectionSize(), "Should return Optional with value");
      assertEquals(Optional.empty(), without.getCollectionSize(), "Should return empty Optional");
    }

    @Test
    @DisplayName("getKeyType should return Optional")
    void getKeyTypeShouldReturnOptional() {
      final MarshalingMetadata with =
          MarshalingMetadata.builder().withKeyType(Double.class).build();
      final MarshalingMetadata without = MarshalingMetadata.builder().build();

      assertEquals(
          Optional.of(Double.class), with.getKeyType(), "Should return Optional with value");
      assertEquals(Optional.empty(), without.getKeyType(), "Should return empty Optional");
    }

    @Test
    @DisplayName("getValueType should return Optional")
    void getValueTypeShouldReturnOptional() {
      final MarshalingMetadata with =
          MarshalingMetadata.builder().withValueType(Float.class).build();
      final MarshalingMetadata without = MarshalingMetadata.builder().build();

      assertEquals(
          Optional.of(Float.class), with.getValueType(), "Should return Optional with value");
      assertEquals(Optional.empty(), without.getValueType(), "Should return empty Optional");
    }

    @Test
    @DisplayName("getObjectClassName should return Optional")
    void getObjectClassNameShouldReturnOptional() {
      final MarshalingMetadata with =
          MarshalingMetadata.builder().withObjectClassName("TestClass").build();
      final MarshalingMetadata without = MarshalingMetadata.builder().build();

      assertEquals(
          Optional.of("TestClass"), with.getObjectClassName(), "Should return Optional with value");
      assertEquals(Optional.empty(), without.getObjectClassName(), "Should return empty Optional");
    }

    @Test
    @DisplayName("getDataSize should return Optional")
    void getDataSizeShouldReturnOptional() {
      final MarshalingMetadata with = MarshalingMetadata.builder().withDataSize(2048).build();
      final MarshalingMetadata without = MarshalingMetadata.builder().build();

      assertEquals(Optional.of(2048), with.getDataSize(), "Should return Optional with value");
      assertEquals(Optional.empty(), without.getDataSize(), "Should return empty Optional");
    }

    @Test
    @DisplayName("getStringEncoding should return Optional")
    void getStringEncodingShouldReturnOptional() {
      final MarshalingMetadata with =
          MarshalingMetadata.builder().withStringEncoding("ASCII").build();
      final MarshalingMetadata without = MarshalingMetadata.builder().build();

      assertEquals(
          Optional.of("ASCII"), with.getStringEncoding(), "Should return Optional with value");
      assertEquals(Optional.empty(), without.getStringEncoding(), "Should return empty Optional");
    }

    @Test
    @DisplayName("shouldUseMemoryPassing should return Optional")
    void shouldUseMemoryPassingShouldReturnOptional() {
      final MarshalingMetadata with = MarshalingMetadata.builder().withMemoryPassing(true).build();
      final MarshalingMetadata without = MarshalingMetadata.builder().build();

      assertEquals(
          Optional.of(true), with.shouldUseMemoryPassing(), "Should return Optional with value");
      assertEquals(
          Optional.empty(), without.shouldUseMemoryPassing(), "Should return empty Optional");
    }

    @Test
    @DisplayName("getMemoryAlignment should return Optional")
    void getMemoryAlignmentShouldReturnOptional() {
      final MarshalingMetadata with = MarshalingMetadata.builder().withMemoryAlignment(32).build();
      final MarshalingMetadata without = MarshalingMetadata.builder().build();

      assertEquals(Optional.of(32), with.getMemoryAlignment(), "Should return Optional with value");
      assertEquals(Optional.empty(), without.getMemoryAlignment(), "Should return empty Optional");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for same object")
    void equalsShouldReturnTrueForSameObject() {
      final MarshalingMetadata metadata = MarshalingMetadata.builder().build();

      assertEquals(metadata, metadata, "Same object should be equal");
    }

    @Test
    @DisplayName("equals should return true for equivalent objects")
    void equalsShouldReturnTrueForEquivalentObjects() {
      final MarshalingMetadata metadata1 =
          MarshalingMetadata.builder()
              .withArrayDimensions(2)
              .withComponentType(String.class)
              .build();
      final MarshalingMetadata metadata2 =
          MarshalingMetadata.builder()
              .withArrayDimensions(2)
              .withComponentType(String.class)
              .build();

      assertEquals(metadata1, metadata2, "Equivalent objects should be equal");
    }

    @Test
    @DisplayName("equals should return false for different values")
    void equalsShouldReturnFalseForDifferentValues() {
      final MarshalingMetadata metadata1 =
          MarshalingMetadata.builder().withArrayDimensions(2).build();
      final MarshalingMetadata metadata2 =
          MarshalingMetadata.builder().withArrayDimensions(3).build();

      assertNotEquals(metadata1, metadata2, "Different values should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final MarshalingMetadata metadata = MarshalingMetadata.builder().build();

      assertNotEquals(null, metadata, "Should not equal null");
    }

    @Test
    @DisplayName("hashCode should be consistent for equivalent objects")
    void hashCodeShouldBeConsistentForEquivalentObjects() {
      final MarshalingMetadata metadata1 =
          MarshalingMetadata.builder()
              .withArrayDimensions(2)
              .withComponentType(String.class)
              .build();
      final MarshalingMetadata metadata2 =
          MarshalingMetadata.builder()
              .withArrayDimensions(2)
              .withComponentType(String.class)
              .build();

      assertEquals(
          metadata1.hashCode(),
          metadata2.hashCode(),
          "Equivalent objects should have same hashCode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include MarshalingMetadata identifier")
    void toStringShouldIncludeMarshalingMetadataIdentifier() {
      final MarshalingMetadata metadata = MarshalingMetadata.builder().build();

      assertTrue(
          metadata.toString().contains("MarshalingMetadata"),
          "toString should contain 'MarshalingMetadata'");
    }

    @Test
    @DisplayName("toString should include set values")
    void toStringShouldIncludeSetValues() {
      final MarshalingMetadata metadata =
          MarshalingMetadata.builder().withArrayDimensions(2).withStringEncoding("UTF-8").build();

      final String str = metadata.toString();
      assertTrue(str.contains("2"), "toString should contain array dimensions");
      assertTrue(str.contains("UTF-8"), "toString should contain string encoding");
    }

    @Test
    @DisplayName("toString should return non-null value")
    void toStringShouldReturnNonNullValue() {
      final MarshalingMetadata metadata = MarshalingMetadata.builder().build();

      assertNotNull(metadata.toString(), "toString should not return null");
    }
  }
}
