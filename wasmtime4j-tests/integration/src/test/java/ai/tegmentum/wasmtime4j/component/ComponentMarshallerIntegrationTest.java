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

package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitType;
import ai.tegmentum.wasmtime4j.exception.WitValueException;
import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.wit.PanamaWitValueMarshaller;
import java.lang.foreign.Arena;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Component Model marshalling with native library.
 *
 * <p>These tests verify parameter validation and discriminator validation for the Panama WIT value
 * marshaller. The tests focus on the marshalling infrastructure rather than actual value
 * marshalling which requires complete native implementations.
 *
 * @since 1.0.0
 */
@DisplayName("Component Marshaller Integration Tests")
class ComponentMarshallerIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentMarshallerIntegrationTest.class.getName());

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for component marshaller tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @Nested
  @DisplayName("Parameter Validation Tests")
  class ParameterValidationTests {

    @Test
    @DisplayName("should reject null value for marshal")
    void shouldRejectNullValueForMarshal() {
      LOGGER.info("Testing marshal rejection of null value");

      try (Arena arena = Arena.ofConfined()) {
        assertThrows(
            WitValueException.class,
            () -> PanamaWitValueMarshaller.marshal(null, arena),
            "Should reject null value");
      }

      LOGGER.info("Marshal correctly rejected null value");
    }

    @Test
    @DisplayName("should reject null data for unmarshal")
    void shouldRejectNullDataForUnmarshal() {
      LOGGER.info("Testing unmarshal rejection of null data");

      try (Arena arena = Arena.ofConfined()) {
        assertThrows(
            WitValueException.class,
            () -> PanamaWitValueMarshaller.unmarshal(1, null, arena),
            "Should reject null data");
      }

      LOGGER.info("Unmarshal correctly rejected null data");
    }

    @Test
    @DisplayName("should reject null arena for unmarshal")
    void shouldRejectNullArenaForUnmarshal() {
      LOGGER.info("Testing unmarshal rejection of null arena");

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaWitValueMarshaller.unmarshal(1, new byte[] {1, 2, 3}, null),
          "Should reject null arena");

      LOGGER.info("Unmarshal correctly rejected null arena");
    }
  }

  @Nested
  @DisplayName("Discriminator Validation Tests")
  class DiscriminatorValidationTests {

    @Test
    @DisplayName("should validate valid discriminator range")
    void shouldValidateValidDiscriminatorRange() {
      LOGGER.info("Testing valid discriminator validation");

      // Valid discriminators are 1-8 based on native implementation
      assertTrue(
          PanamaWitValueMarshaller.validateDiscriminator(1), "Discriminator 1 should be valid");
      assertTrue(
          PanamaWitValueMarshaller.validateDiscriminator(2), "Discriminator 2 should be valid");
      assertTrue(
          PanamaWitValueMarshaller.validateDiscriminator(3), "Discriminator 3 should be valid");

      LOGGER.info("Valid discriminators accepted");
    }

    @Test
    @DisplayName("should reject invalid discriminator")
    void shouldRejectInvalidDiscriminator() {
      LOGGER.info("Testing invalid discriminator rejection");

      assertFalse(
          PanamaWitValueMarshaller.validateDiscriminator(0), "Discriminator 0 should be invalid");
      assertFalse(
          PanamaWitValueMarshaller.validateDiscriminator(-1), "Discriminator -1 should be invalid");
      assertFalse(
          PanamaWitValueMarshaller.validateDiscriminator(999),
          "Discriminator 999 should be invalid");

      LOGGER.info("Invalid discriminators correctly rejected");
    }

    @Test
    @DisplayName("should throw on unmarshal with invalid discriminator")
    void shouldThrowOnUnmarshalWithInvalidDiscriminator() {
      LOGGER.info("Testing unmarshal with invalid discriminator");

      try (Arena arena = Arena.ofConfined()) {
        assertThrows(
            WitValueException.class,
            () -> PanamaWitValueMarshaller.unmarshal(999, new byte[] {0}, arena),
            "Should throw for invalid discriminator");
      }

      LOGGER.info("Unmarshal correctly threw for invalid discriminator");
    }
  }

  @Nested
  @DisplayName("WitType Creation Tests")
  class WitTypeCreationTests {

    @Test
    @DisplayName("should create all primitive WitTypes without exception")
    void shouldCreateAllPrimitiveWitTypes() {
      LOGGER.info("Testing WitType creation for all primitives");

      assertDoesNotThrow(WitType::createBool, "Should create bool type");
      assertDoesNotThrow(WitType::createS8, "Should create s8 type");
      assertDoesNotThrow(WitType::createS16, "Should create s16 type");
      assertDoesNotThrow(WitType::createS32, "Should create s32 type");
      assertDoesNotThrow(WitType::createS64, "Should create s64 type");
      assertDoesNotThrow(WitType::createU8, "Should create u8 type");
      assertDoesNotThrow(WitType::createU16, "Should create u16 type");
      assertDoesNotThrow(WitType::createU32, "Should create u32 type");
      assertDoesNotThrow(WitType::createU64, "Should create u64 type");
      assertDoesNotThrow(WitType::createFloat32, "Should create float32 type");
      assertDoesNotThrow(WitType::createFloat64, "Should create float64 type");
      assertDoesNotThrow(WitType::createChar, "Should create char type");
      assertDoesNotThrow(WitType::createString, "Should create string type");

      LOGGER.info("All primitive WitTypes created successfully");
    }

    @Test
    @DisplayName("should return correct primitive type property")
    void shouldReturnCorrectPrimitiveTypeProperty() {
      LOGGER.info("Testing WitType primitive property");

      assertTrue(WitType.createBool().isPrimitive(), "Bool should be primitive");
      assertTrue(WitType.createS32().isPrimitive(), "S32 should be primitive");
      assertTrue(WitType.createString().isPrimitive(), "String should be primitive");

      LOGGER.info("All WitTypes correctly report primitive property");
    }

    @Test
    @DisplayName("should create list type from element type")
    void shouldCreateListTypeFromElementType() {
      LOGGER.info("Testing list type creation");

      final WitType s32Type = WitType.createS32();
      final WitType listType = WitType.list(s32Type);

      assertNotNull(listType, "List type should not be null");
      assertTrue(listType.isComposite(), "List should be composite type");

      LOGGER.info("List type created successfully");
    }

    @Test
    @DisplayName("should create option type from inner type")
    void shouldCreateOptionTypeFromInnerType() {
      LOGGER.info("Testing option type creation");

      final WitType s32Type = WitType.createS32();
      final WitType optionType = WitType.option(s32Type);

      assertNotNull(optionType, "Option type should not be null");
      assertTrue(optionType.isComposite(), "Option should be composite type");

      LOGGER.info("Option type created successfully");
    }

    @Test
    @DisplayName("should create tuple type from element types")
    void shouldCreateTupleTypeFromElementTypes() {
      LOGGER.info("Testing tuple type creation");

      final WitType s32Type = WitType.createS32();
      final WitType stringType = WitType.createString();
      final WitType tupleType = WitType.tuple(s32Type, stringType);

      assertNotNull(tupleType, "Tuple type should not be null");
      assertTrue(tupleType.isComposite(), "Tuple should be composite type");

      LOGGER.info("Tuple type created successfully");
    }
  }

  @Nested
  @DisplayName("WitType Properties Tests")
  class WitTypePropertiesTests {

    @Test
    @DisplayName("should return correct type names")
    void shouldReturnCorrectTypeNames() {
      LOGGER.info("Testing WitType names");

      assertEquals("bool", WitType.createBool().getName(), "Bool name should be 'bool'");
      assertEquals("s32", WitType.createS32().getName(), "S32 name should be 's32'");
      assertEquals("u64", WitType.createU64().getName(), "U64 name should be 'u64'");
      assertEquals(
          "float32", WitType.createFloat32().getName(), "Float32 name should be 'float32'");
      assertEquals("string", WitType.createString().getName(), "String name should be 'string'");

      LOGGER.info("All type names are correct");
    }

    @Test
    @DisplayName("should correctly identify composite types")
    void shouldCorrectlyIdentifyCompositeTypes() {
      LOGGER.info("Testing composite type identification");

      final WitType listType = WitType.list(WitType.createS32());
      final WitType optionType = WitType.option(WitType.createString());

      assertTrue(listType.isComposite(), "List should be composite");
      assertTrue(optionType.isComposite(), "Option should be composite");
      assertFalse(WitType.createS32().isComposite(), "S32 should not be composite");
      assertFalse(WitType.createString().isComposite(), "String should not be composite");

      LOGGER.info("Composite types correctly identified");
    }

    @Test
    @DisplayName("should correctly identify resource types")
    void shouldCorrectlyIdentifyResourceTypes() {
      LOGGER.info("Testing resource type identification");

      final WitType resourceType = WitType.resource("TestResource", "resource-id");

      assertTrue(resourceType.isResource(), "Resource should be resource type");
      assertFalse(WitType.createS32().isResource(), "S32 should not be resource");
      assertFalse(WitType.list(WitType.createS32()).isResource(), "List should not be resource");

      LOGGER.info("Resource types correctly identified");
    }
  }
}
