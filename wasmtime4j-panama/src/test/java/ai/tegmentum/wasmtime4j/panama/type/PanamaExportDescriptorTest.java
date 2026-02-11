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

package ai.tegmentum.wasmtime4j.panama.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaExportDescriptor} class.
 *
 * <p>This test class verifies the Panama implementation of ExportDescriptor interface.
 */
@DisplayName("PanamaExportDescriptor Tests")
class PanamaExportDescriptorTest {

  private Arena arena;
  private MemorySegment validHandle;

  @BeforeEach
  void setUp() {
    arena = Arena.ofConfined();
    // Create a valid non-null handle for testing
    validHandle = arena.allocate(8);
  }

  @AfterEach
  void tearDown() {
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  /**
   * Creates a test WasmType for export descriptor testing.
   *
   * @return a PanamaFuncType for testing
   */
  private WasmType createTestType() {
    return new PanamaFuncType(
        Arrays.asList(WasmValueType.I32, WasmValueType.I64),
        Collections.singletonList(WasmValueType.F64),
        arena,
        validHandle);
  }

  /**
   * Creates a PanamaMemoryType for testing.
   *
   * @return a PanamaMemoryType for testing
   */
  private WasmType createMemoryType() {
    return new PanamaMemoryType(1L, 10L, false, false, arena, validHandle);
  }

  /**
   * Creates a PanamaTableType for testing.
   *
   * @return a PanamaTableType for testing
   */
  private WasmType createTableType() {
    return new PanamaTableType(WasmValueType.FUNCREF, 1L, 100L, arena, validHandle);
  }

  /**
   * Creates a PanamaGlobalType for testing.
   *
   * @return a PanamaGlobalType for testing
   */
  private WasmType createGlobalType() {
    return new PanamaGlobalType(WasmValueType.I32, false, arena, validHandle);
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaExportDescriptor should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaExportDescriptor.class.getModifiers()),
          "PanamaExportDescriptor should be final");
    }

    @Test
    @DisplayName("PanamaExportDescriptor should implement ExportDescriptor")
    void shouldImplementExportDescriptor() {
      assertTrue(
          ExportDescriptor.class.isAssignableFrom(PanamaExportDescriptor.class),
          "PanamaExportDescriptor should implement ExportDescriptor");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid parameters")
    void constructorShouldAcceptValidParameters() {
      final WasmType type = createTestType();

      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("main", type);

      assertNotNull(descriptor, "Descriptor should not be null");
      assertEquals("main", descriptor.getName(), "Name should match");
      assertEquals(type, descriptor.getType(), "Type should match");
    }

    @Test
    @DisplayName("Constructor should throw on null name")
    void constructorShouldThrowOnNullName() {
      final WasmType type = createTestType();

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaExportDescriptor(null, type),
          "Should throw on null name");
    }

    @Test
    @DisplayName("Constructor should throw on null type")
    void constructorShouldThrowOnNullType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaExportDescriptor("main", null),
          "Should throw on null type");
    }

    @Test
    @DisplayName("Constructor should accept empty name")
    void constructorShouldAcceptEmptyName() {
      final WasmType type = createTestType();

      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("", type);

      assertEquals("", descriptor.getName(), "Empty name should be accepted");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName should return correct name")
    void getNameShouldReturnCorrectName() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("exported_func", type);

      assertEquals("exported_func", descriptor.getName(), "Name should match");
    }

    @Test
    @DisplayName("getType should return correct type")
    void getTypeShouldReturnCorrectType() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("main", type);

      assertEquals(type, descriptor.getType(), "Type should match");
    }

    @Test
    @DisplayName("getType should return same reference")
    void getTypeShouldReturnSameReference() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("main", type);

      assertTrue(type == descriptor.getType(), "Should return same type reference");
    }
  }

  @Nested
  @DisplayName("equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("main", type);

      assertEquals(descriptor, descriptor, "Same instance should be equal");
    }

    @Test
    @DisplayName("equals should return true for equivalent descriptors")
    void equalsShouldReturnTrueForEquivalentDescriptors() {
      final WasmType type1 = createTestType();
      final WasmType type2 = createTestType();
      final PanamaExportDescriptor descriptor1 = new PanamaExportDescriptor("main", type1);
      final PanamaExportDescriptor descriptor2 = new PanamaExportDescriptor("main", type2);

      assertEquals(descriptor1, descriptor2, "Equivalent descriptors should be equal");
    }

    @Test
    @DisplayName("equals should return false for different names")
    void equalsShouldReturnFalseForDifferentNames() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor1 = new PanamaExportDescriptor("main", type);
      final PanamaExportDescriptor descriptor2 = new PanamaExportDescriptor("run", type);

      assertNotEquals(descriptor1, descriptor2, "Different names should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different types")
    void equalsShouldReturnFalseForDifferentTypes() {
      final WasmType funcType = createTestType();
      final WasmType memoryType = createMemoryType();
      final PanamaExportDescriptor descriptor1 = new PanamaExportDescriptor("export", funcType);
      final PanamaExportDescriptor descriptor2 = new PanamaExportDescriptor("export", memoryType);

      assertNotEquals(descriptor1, descriptor2, "Different types should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("main", type);

      assertFalse(descriptor.equals(null), "Should not equal null");
    }

    @Test
    @DisplayName("equals should return false for different class")
    void equalsShouldReturnFalseForDifferentClass() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("main", type);

      assertFalse(descriptor.equals("not a descriptor"), "Should not equal different class");
    }
  }

  @Nested
  @DisplayName("hashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("main", type);

      final int hash1 = descriptor.hashCode();
      final int hash2 = descriptor.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent");
    }

    @Test
    @DisplayName("hashCode should be equal for equal descriptors")
    void hashCodeShouldBeEqualForEqualDescriptors() {
      final WasmType type1 = createTestType();
      final WasmType type2 = createTestType();
      final PanamaExportDescriptor descriptor1 = new PanamaExportDescriptor("main", type1);
      final PanamaExportDescriptor descriptor2 = new PanamaExportDescriptor("main", type2);

      assertEquals(
          descriptor1.hashCode(),
          descriptor2.hashCode(),
          "Equal descriptors should have equal hashCode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain name")
    void toStringShouldContainName() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor =
          new PanamaExportDescriptor("my_exported_function", type);

      final String str = descriptor.toString();

      assertTrue(str.contains("my_exported_function"), "toString should contain name");
    }

    @Test
    @DisplayName("toString should contain type info")
    void toStringShouldContainTypeInfo() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("main", type);

      final String str = descriptor.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("ExportDescriptor"), "toString should contain class name");
    }

    @Test
    @DisplayName("toString should not be null")
    void toStringShouldNotBeNull() {
      final WasmType type = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("main", type);

      assertNotNull(descriptor.toString(), "toString should not return null");
    }
  }

  @Nested
  @DisplayName("fromNative Tests")
  class FromNativeTests {

    @Test
    @DisplayName("fromNative should throw UnsupportedOperationException")
    void fromNativeShouldThrowUnsupportedOperationException() {
      final MemorySegment segment = arena.allocate(64);

      assertThrows(
          UnsupportedOperationException.class,
          () -> PanamaExportDescriptor.fromNative(segment, arena),
          "fromNative should throw UnsupportedOperationException");
    }

    @Test
    @DisplayName("fromNative should throw on null segment")
    void fromNativeShouldThrowOnNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaExportDescriptor.fromNative(null, arena),
          "fromNative should throw on null segment");
    }

    @Test
    @DisplayName("fromNative should throw on null arena")
    void fromNativeShouldThrowOnNullArena() {
      final MemorySegment segment = arena.allocate(64);

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaExportDescriptor.fromNative(segment, null),
          "fromNative should throw on null arena");
    }
  }

  @Nested
  @DisplayName("Type Kind Tests")
  class TypeKindTests {

    @Test
    @DisplayName("Should work with function type export")
    void shouldWorkWithFunctionTypeExport() {
      final WasmType funcType = createTestType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("add", funcType);

      assertEquals(WasmTypeKind.FUNCTION, descriptor.getType().getKind());
    }

    @Test
    @DisplayName("Should work with memory type export")
    void shouldWorkWithMemoryTypeExport() {
      final WasmType memoryType = createMemoryType();
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("memory", memoryType);

      assertEquals(WasmTypeKind.MEMORY, descriptor.getType().getKind());
    }

    @Test
    @DisplayName("Should work with table type export")
    void shouldWorkWithTableTypeExport() {
      final WasmType tableType = createTableType();
      final PanamaExportDescriptor descriptor =
          new PanamaExportDescriptor("__indirect_function_table", tableType);

      assertEquals(WasmTypeKind.TABLE, descriptor.getType().getKind());
    }

    @Test
    @DisplayName("Should work with global type export")
    void shouldWorkWithGlobalTypeExport() {
      final WasmType globalType = createGlobalType();
      final PanamaExportDescriptor descriptor =
          new PanamaExportDescriptor("__stack_pointer", globalType);

      assertEquals(WasmTypeKind.GLOBAL, descriptor.getType().getKind());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should handle unicode in names")
    void shouldHandleUnicodeInNames() {
      final WasmType type = createTestType();

      // Using Unicode characters: 世界 (Chinese for "world")
      final PanamaExportDescriptor descriptor = new PanamaExportDescriptor("世界", type);

      assertEquals("世界", descriptor.getName());
    }

    @Test
    @DisplayName("Should handle special characters in names")
    void shouldHandleSpecialCharactersInNames() {
      final WasmType type = createTestType();

      final PanamaExportDescriptor descriptor =
          new PanamaExportDescriptor("func_name$test.inner", type);

      assertEquals("func_name$test.inner", descriptor.getName());
    }

    @Test
    @DisplayName("Multiple descriptors should be independent")
    void multipleDescriptorsShouldBeIndependent() {
      final WasmType funcType = createTestType();
      final WasmType memoryType = createMemoryType();

      final PanamaExportDescriptor descriptor1 = new PanamaExportDescriptor("func1", funcType);
      final PanamaExportDescriptor descriptor2 = new PanamaExportDescriptor("memory", memoryType);

      assertNotEquals(descriptor1.getName(), descriptor2.getName());
      assertNotEquals(descriptor1.getType().getKind(), descriptor2.getType().getKind());
    }

    @Test
    @DisplayName("Should work with typical WASM exports")
    void shouldWorkWithTypicalWasmExports() {
      final WasmType funcType = createTestType();
      final WasmType memoryType = createMemoryType();
      final WasmType tableType = createTableType();
      final WasmType globalType = createGlobalType();

      // Typical exports
      final PanamaExportDescriptor mainExport = new PanamaExportDescriptor("_start", funcType);
      final PanamaExportDescriptor memoryExport = new PanamaExportDescriptor("memory", memoryType);
      final PanamaExportDescriptor tableExport =
          new PanamaExportDescriptor("__indirect_function_table", tableType);
      final PanamaExportDescriptor globalExport =
          new PanamaExportDescriptor("__heap_base", globalType);

      assertEquals("_start", mainExport.getName());
      assertEquals("memory", memoryExport.getName());
      assertEquals("__indirect_function_table", tableExport.getName());
      assertEquals("__heap_base", globalExport.getName());
    }

    @Test
    @DisplayName("Descriptors should be usable in collections")
    void descriptorsShouldBeUsableInCollections() {
      final WasmType type1 = createTestType();
      final WasmType type2 = createTestType();

      final PanamaExportDescriptor descriptor1 = new PanamaExportDescriptor("func1", type1);
      final PanamaExportDescriptor descriptor2 = new PanamaExportDescriptor("func1", type2);
      final PanamaExportDescriptor descriptor3 = new PanamaExportDescriptor("func2", type1);

      final java.util.Set<PanamaExportDescriptor> set = new java.util.HashSet<>();
      set.add(descriptor1);
      set.add(descriptor2); // Same as descriptor1
      set.add(descriptor3);

      assertEquals(2, set.size(), "Set should have 2 unique descriptors");
    }
  }
}
