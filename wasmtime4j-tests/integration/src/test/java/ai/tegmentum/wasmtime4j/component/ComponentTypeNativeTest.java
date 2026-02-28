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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for ComponentType enum with native library loaded.
 *
 * <p>These tests verify that ComponentType enum works correctly in the context of native library
 * operations. While the enum itself is pure Java, these tests ensure its type classification
 * methods behave correctly when the native runtime is active.
 *
 * @since 1.0.0
 */
@DisplayName("ComponentType Native Integration Tests")
class ComponentTypeNativeIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentTypeNativeIntegrationTest.class.getName());

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for ComponentType integration tests");
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
  @DisplayName("Primitive Type Classification Tests")
  class PrimitiveTypeClassificationTests {

    @Test
    @DisplayName("should classify boolean as primitive")
    void shouldClassifyBooleanAsPrimitive() {
      LOGGER.info("Testing BOOL primitive classification");

      assertTrue(ComponentType.BOOL.isPrimitive(), "BOOL should be primitive");

      LOGGER.info("BOOL correctly classified as primitive");
    }

    @Test
    @DisplayName("should classify signed integers as primitive")
    void shouldClassifySignedIntegersAsPrimitive() {
      LOGGER.info("Testing signed integer primitive classification");

      assertTrue(ComponentType.S8.isPrimitive(), "S8 should be primitive");
      assertTrue(ComponentType.S16.isPrimitive(), "S16 should be primitive");
      assertTrue(ComponentType.S32.isPrimitive(), "S32 should be primitive");
      assertTrue(ComponentType.S64.isPrimitive(), "S64 should be primitive");

      LOGGER.info("All signed integers correctly classified as primitive");
    }

    @Test
    @DisplayName("should classify unsigned integers as primitive")
    void shouldClassifyUnsignedIntegersAsPrimitive() {
      LOGGER.info("Testing unsigned integer primitive classification");

      assertTrue(ComponentType.U8.isPrimitive(), "U8 should be primitive");
      assertTrue(ComponentType.U16.isPrimitive(), "U16 should be primitive");
      assertTrue(ComponentType.U32.isPrimitive(), "U32 should be primitive");
      assertTrue(ComponentType.U64.isPrimitive(), "U64 should be primitive");

      LOGGER.info("All unsigned integers correctly classified as primitive");
    }

    @Test
    @DisplayName("should classify floating point as primitive")
    void shouldClassifyFloatingPointAsPrimitive() {
      LOGGER.info("Testing floating point primitive classification");

      assertTrue(ComponentType.F32.isPrimitive(), "F32 should be primitive");
      assertTrue(ComponentType.F64.isPrimitive(), "F64 should be primitive");

      LOGGER.info("All floating point types correctly classified as primitive");
    }

    @Test
    @DisplayName("should classify CHAR and STRING as primitive")
    void shouldClassifyCharAndStringAsPrimitive() {
      LOGGER.info("Testing CHAR and STRING primitive classification");

      assertTrue(ComponentType.CHAR.isPrimitive(), "CHAR should be primitive");
      assertTrue(ComponentType.STRING.isPrimitive(), "STRING should be primitive");

      LOGGER.info("CHAR and STRING correctly classified as primitive");
    }

    @Test
    @DisplayName("should not classify compound types as primitive")
    void shouldNotClassifyCompoundTypesAsPrimitive() {
      LOGGER.info("Testing compound types are not primitive");

      assertFalse(ComponentType.LIST.isPrimitive(), "LIST should not be primitive");
      assertFalse(ComponentType.RECORD.isPrimitive(), "RECORD should not be primitive");
      assertFalse(ComponentType.TUPLE.isPrimitive(), "TUPLE should not be primitive");
      assertFalse(ComponentType.VARIANT.isPrimitive(), "VARIANT should not be primitive");
      assertFalse(ComponentType.ENUM.isPrimitive(), "ENUM should not be primitive");
      assertFalse(ComponentType.OPTION.isPrimitive(), "OPTION should not be primitive");
      assertFalse(ComponentType.RESULT.isPrimitive(), "RESULT should not be primitive");
      assertFalse(ComponentType.FLAGS.isPrimitive(), "FLAGS should not be primitive");

      LOGGER.info("All compound types correctly classified as non-primitive");
    }
  }

  @Nested
  @DisplayName("Integer Type Classification Tests")
  class IntegerTypeClassificationTests {

    @Test
    @DisplayName("should classify signed integers correctly")
    void shouldClassifySignedIntegersCorrectly() {
      LOGGER.info("Testing isInteger for signed types");

      assertTrue(ComponentType.S8.isInteger(), "S8 should be integer");
      assertTrue(ComponentType.S16.isInteger(), "S16 should be integer");
      assertTrue(ComponentType.S32.isInteger(), "S32 should be integer");
      assertTrue(ComponentType.S64.isInteger(), "S64 should be integer");

      LOGGER.info("All signed integers correctly classified as integer");
    }

    @Test
    @DisplayName("should classify unsigned integers correctly")
    void shouldClassifyUnsignedIntegersCorrectly() {
      LOGGER.info("Testing isInteger for unsigned types");

      assertTrue(ComponentType.U8.isInteger(), "U8 should be integer");
      assertTrue(ComponentType.U16.isInteger(), "U16 should be integer");
      assertTrue(ComponentType.U32.isInteger(), "U32 should be integer");
      assertTrue(ComponentType.U64.isInteger(), "U64 should be integer");

      LOGGER.info("All unsigned integers correctly classified as integer");
    }

    @Test
    @DisplayName("should not classify non-integers as integer")
    void shouldNotClassifyNonIntegersAsInteger() {
      LOGGER.info("Testing non-integer types");

      assertFalse(ComponentType.BOOL.isInteger(), "BOOL should not be integer");
      assertFalse(ComponentType.F32.isInteger(), "F32 should not be integer");
      assertFalse(ComponentType.F64.isInteger(), "F64 should not be integer");
      assertFalse(ComponentType.CHAR.isInteger(), "CHAR should not be integer");
      assertFalse(ComponentType.STRING.isInteger(), "STRING should not be integer");
      assertFalse(ComponentType.LIST.isInteger(), "LIST should not be integer");

      LOGGER.info("All non-integer types correctly classified");
    }
  }

  @Nested
  @DisplayName("Floating Point Type Classification Tests")
  class FloatingPointTypeClassificationTests {

    @Test
    @DisplayName("should classify F32 and F64 as floating point")
    void shouldClassifyF32AndF64AsFloatingPoint() {
      LOGGER.info("Testing isFloatingPoint for F32 and F64");

      assertTrue(ComponentType.F32.isFloat(), "F32 should be floating point");
      assertTrue(ComponentType.F64.isFloat(), "F64 should be floating point");

      LOGGER.info("F32 and F64 correctly classified as floating point");
    }

    @Test
    @DisplayName("should not classify non-floating point types")
    void shouldNotClassifyNonFloatingPointTypes() {
      LOGGER.info("Testing non-floating point types");

      assertFalse(ComponentType.BOOL.isFloat(), "BOOL should not be floating point");
      assertFalse(ComponentType.S32.isFloat(), "S32 should not be floating point");
      assertFalse(ComponentType.U64.isFloat(), "U64 should not be floating point");
      assertFalse(ComponentType.STRING.isFloat(), "STRING should not be floating point");

      LOGGER.info("All non-floating point types correctly classified");
    }
  }

  @Nested
  @DisplayName("Resource Type Classification Tests")
  class ResourceTypeClassificationTests {

    @Test
    @DisplayName("should classify OWN and BORROW as resource types")
    void shouldClassifyOwnAndBorrowAsResourceTypes() {
      LOGGER.info("Testing isResource for OWN and BORROW");

      assertTrue(ComponentType.OWN.isResource(), "OWN should be resource");
      assertTrue(ComponentType.BORROW.isResource(), "BORROW should be resource");

      LOGGER.info("OWN and BORROW correctly classified as resource types");
    }

    @Test
    @DisplayName("should not classify non-resource types as resource")
    void shouldNotClassifyNonResourceTypesAsResource() {
      LOGGER.info("Testing non-resource types");

      assertFalse(ComponentType.BOOL.isResource(), "BOOL should not be resource");
      assertFalse(ComponentType.S32.isResource(), "S32 should not be resource");
      assertFalse(ComponentType.LIST.isResource(), "LIST should not be resource");
      assertFalse(ComponentType.RECORD.isResource(), "RECORD should not be resource");

      LOGGER.info("All non-resource types correctly classified");
    }
  }

  @Nested
  @DisplayName("Enum Completeness Tests")
  class EnumCompletenessTests {

    @Test
    @DisplayName("should have all 26 component types")
    void shouldHaveAllComponentTypes() {
      LOGGER.info("Testing enum completeness");

      final ComponentType[] types = ComponentType.values();
      assertEquals(26, types.length, "Should have 26 component types");

      LOGGER.info("Enum has " + types.length + " values as expected");
    }

    @Test
    @DisplayName("should be able to retrieve all types by name")
    void shouldRetrieveAllTypesByName() {
      LOGGER.info("Testing valueOf for all types");

      assertNotNull(ComponentType.valueOf("BOOL"), "Should retrieve BOOL");
      assertNotNull(ComponentType.valueOf("S8"), "Should retrieve S8");
      assertNotNull(ComponentType.valueOf("S16"), "Should retrieve S16");
      assertNotNull(ComponentType.valueOf("S32"), "Should retrieve S32");
      assertNotNull(ComponentType.valueOf("S64"), "Should retrieve S64");
      assertNotNull(ComponentType.valueOf("U8"), "Should retrieve U8");
      assertNotNull(ComponentType.valueOf("U16"), "Should retrieve U16");
      assertNotNull(ComponentType.valueOf("U32"), "Should retrieve U32");
      assertNotNull(ComponentType.valueOf("U64"), "Should retrieve U64");
      assertNotNull(ComponentType.valueOf("F32"), "Should retrieve F32");
      assertNotNull(ComponentType.valueOf("F64"), "Should retrieve F64");
      assertNotNull(ComponentType.valueOf("CHAR"), "Should retrieve CHAR");
      assertNotNull(ComponentType.valueOf("STRING"), "Should retrieve STRING");
      assertNotNull(ComponentType.valueOf("LIST"), "Should retrieve LIST");
      assertNotNull(ComponentType.valueOf("RECORD"), "Should retrieve RECORD");
      assertNotNull(ComponentType.valueOf("TUPLE"), "Should retrieve TUPLE");
      assertNotNull(ComponentType.valueOf("VARIANT"), "Should retrieve VARIANT");
      assertNotNull(ComponentType.valueOf("ENUM"), "Should retrieve ENUM");
      assertNotNull(ComponentType.valueOf("OPTION"), "Should retrieve OPTION");
      assertNotNull(ComponentType.valueOf("RESULT"), "Should retrieve RESULT");
      assertNotNull(ComponentType.valueOf("FLAGS"), "Should retrieve FLAGS");
      assertNotNull(ComponentType.valueOf("OWN"), "Should retrieve OWN");
      assertNotNull(ComponentType.valueOf("BORROW"), "Should retrieve BORROW");

      LOGGER.info("All types successfully retrieved by name");
    }
  }
}
