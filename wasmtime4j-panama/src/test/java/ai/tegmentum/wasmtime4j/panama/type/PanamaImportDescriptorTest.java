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

import ai.tegmentum.wasmtime4j.ImportDescriptor;
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
 * Tests for the {@link PanamaImportDescriptor} class.
 *
 * <p>This test class verifies the Panama implementation of ImportDescriptor interface.
 */
@DisplayName("PanamaImportDescriptor Tests")
class PanamaImportDescriptorTest {

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
   * Creates a test WasmType for import descriptor testing.
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

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaImportDescriptor should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaImportDescriptor.class.getModifiers()),
          "PanamaImportDescriptor should be final");
    }

    @Test
    @DisplayName("PanamaImportDescriptor should implement ImportDescriptor")
    void shouldImplementImportDescriptor() {
      assertTrue(
          ImportDescriptor.class.isAssignableFrom(PanamaImportDescriptor.class),
          "PanamaImportDescriptor should implement ImportDescriptor");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid parameters")
    void constructorShouldAcceptValidParameters() {
      final WasmType type = createTestType();

      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "memory", type);

      assertNotNull(descriptor, "Descriptor should not be null");
      assertEquals("env", descriptor.getModuleName(), "Module name should match");
      assertEquals("memory", descriptor.getName(), "Name should match");
      assertEquals(type, descriptor.getType(), "Type should match");
    }

    @Test
    @DisplayName("Constructor should throw on null module name")
    void constructorShouldThrowOnNullModuleName() {
      final WasmType type = createTestType();

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaImportDescriptor(null, "memory", type),
          "Should throw on null module name");
    }

    @Test
    @DisplayName("Constructor should throw on null name")
    void constructorShouldThrowOnNullName() {
      final WasmType type = createTestType();

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaImportDescriptor("env", null, type),
          "Should throw on null name");
    }

    @Test
    @DisplayName("Constructor should throw on null type")
    void constructorShouldThrowOnNullType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaImportDescriptor("env", "memory", null),
          "Should throw on null type");
    }

    @Test
    @DisplayName("Constructor should accept empty module name")
    void constructorShouldAcceptEmptyModuleName() {
      final WasmType type = createTestType();

      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("", "name", type);

      assertEquals("", descriptor.getModuleName(), "Empty module name should be accepted");
    }

    @Test
    @DisplayName("Constructor should accept empty name")
    void constructorShouldAcceptEmptyName() {
      final WasmType type = createTestType();

      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "", type);

      assertEquals("", descriptor.getName(), "Empty name should be accepted");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getModuleName should return correct module name")
    void getModuleNameShouldReturnCorrectModuleName() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor =
          new PanamaImportDescriptor("wasi", "fd_write", type);

      assertEquals("wasi", descriptor.getModuleName(), "Module name should match");
    }

    @Test
    @DisplayName("getName should return correct name")
    void getNameShouldReturnCorrectName() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "print", type);

      assertEquals("print", descriptor.getName(), "Name should match");
    }

    @Test
    @DisplayName("getType should return correct type")
    void getTypeShouldReturnCorrectType() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "memory", type);

      assertEquals(type, descriptor.getType(), "Type should match");
    }

    @Test
    @DisplayName("getType should return same reference")
    void getTypeShouldReturnSameReference() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "memory", type);

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
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "memory", type);

      assertEquals(descriptor, descriptor, "Same instance should be equal");
    }

    @Test
    @DisplayName("equals should return true for equivalent descriptors")
    void equalsShouldReturnTrueForEquivalentDescriptors() {
      final WasmType type1 = createTestType();
      final WasmType type2 = createTestType();
      final PanamaImportDescriptor descriptor1 = new PanamaImportDescriptor("env", "memory", type1);
      final PanamaImportDescriptor descriptor2 = new PanamaImportDescriptor("env", "memory", type2);

      assertEquals(descriptor1, descriptor2, "Equivalent descriptors should be equal");
    }

    @Test
    @DisplayName("equals should return false for different module names")
    void equalsShouldReturnFalseForDifferentModuleNames() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor1 = new PanamaImportDescriptor("env", "memory", type);
      final PanamaImportDescriptor descriptor2 = new PanamaImportDescriptor("wasi", "memory", type);

      assertNotEquals(descriptor1, descriptor2, "Different module names should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different names")
    void equalsShouldReturnFalseForDifferentNames() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor1 = new PanamaImportDescriptor("env", "memory", type);
      final PanamaImportDescriptor descriptor2 = new PanamaImportDescriptor("env", "table", type);

      assertNotEquals(descriptor1, descriptor2, "Different names should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different types")
    void equalsShouldReturnFalseForDifferentTypes() {
      final WasmType funcType = createTestType();
      final WasmType memoryType = createMemoryType();
      final PanamaImportDescriptor descriptor1 =
          new PanamaImportDescriptor("env", "memory", funcType);
      final PanamaImportDescriptor descriptor2 =
          new PanamaImportDescriptor("env", "memory", memoryType);

      assertNotEquals(descriptor1, descriptor2, "Different types should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "memory", type);

      assertFalse(descriptor.equals(null), "Should not equal null");
    }

    @Test
    @DisplayName("equals should return false for different class")
    void equalsShouldReturnFalseForDifferentClass() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "memory", type);

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
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "memory", type);

      final int hash1 = descriptor.hashCode();
      final int hash2 = descriptor.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent");
    }

    @Test
    @DisplayName("hashCode should be equal for equal descriptors")
    void hashCodeShouldBeEqualForEqualDescriptors() {
      final WasmType type1 = createTestType();
      final WasmType type2 = createTestType();
      final PanamaImportDescriptor descriptor1 = new PanamaImportDescriptor("env", "memory", type1);
      final PanamaImportDescriptor descriptor2 = new PanamaImportDescriptor("env", "memory", type2);

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
    @DisplayName("toString should contain module name")
    void toStringShouldContainModuleName() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor =
          new PanamaImportDescriptor("wasmModule", "funcName", type);

      final String str = descriptor.toString();

      assertTrue(str.contains("wasmModule"), "toString should contain module name");
    }

    @Test
    @DisplayName("toString should contain name")
    void toStringShouldContainName() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor =
          new PanamaImportDescriptor("env", "myFunction", type);

      final String str = descriptor.toString();

      assertTrue(str.contains("myFunction"), "toString should contain name");
    }

    @Test
    @DisplayName("toString should contain type info")
    void toStringShouldContainTypeInfo() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "memory", type);

      final String str = descriptor.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("ImportDescriptor"), "toString should contain class name");
    }

    @Test
    @DisplayName("toString should not be null")
    void toStringShouldNotBeNull() {
      final WasmType type = createTestType();
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("env", "memory", type);

      assertNotNull(descriptor.toString(), "toString should not return null");
    }
  }

  @Nested
  @DisplayName("fromNative Tests")
  class FromNativeTests {

    @Test
    @DisplayName("fromNative should throw on zero-filled segment with null module name pointer")
    void fromNativeShouldThrowOnNullModuleNamePointer() {
      final MemorySegment segment = arena.allocate(64);

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaImportDescriptor.fromNative(segment, arena),
          "fromNative should throw IllegalArgumentException for null module name pointer");
    }

    @Test
    @DisplayName("fromNative should throw on null segment")
    void fromNativeShouldThrowOnNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaImportDescriptor.fromNative(null, arena),
          "fromNative should throw on null segment");
    }

    @Test
    @DisplayName("fromNative should throw on null arena")
    void fromNativeShouldThrowOnNullArena() {
      final MemorySegment segment = arena.allocate(64);

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaImportDescriptor.fromNative(segment, null),
          "fromNative should throw on null arena");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should work with WASI imports")
    void shouldWorkWithWasiImports() {
      final WasmType funcType = createTestType();

      final PanamaImportDescriptor descriptor =
          new PanamaImportDescriptor("wasi_snapshot_preview1", "fd_write", funcType);

      assertEquals("wasi_snapshot_preview1", descriptor.getModuleName());
      assertEquals("fd_write", descriptor.getName());
      assertEquals(WasmTypeKind.FUNCTION, descriptor.getType().getKind());
    }

    @Test
    @DisplayName("Should work with memory imports")
    void shouldWorkWithMemoryImports() {
      final WasmType memoryType = createMemoryType();

      final PanamaImportDescriptor descriptor =
          new PanamaImportDescriptor("env", "memory", memoryType);

      assertEquals("env", descriptor.getModuleName());
      assertEquals("memory", descriptor.getName());
      assertEquals(WasmTypeKind.MEMORY, descriptor.getType().getKind());
    }

    @Test
    @DisplayName("Should handle unicode in names")
    void shouldHandleUnicodeInNames() {
      final WasmType type = createTestType();

      // Using Unicode characters: Hello (ASCII) and 世界 (Chinese for "world")
      final PanamaImportDescriptor descriptor = new PanamaImportDescriptor("Hello", "世界", type);

      assertEquals("Hello", descriptor.getModuleName());
      assertEquals("世界", descriptor.getName());
    }

    @Test
    @DisplayName("Should handle special characters in names")
    void shouldHandleSpecialCharactersInNames() {
      final WasmType type = createTestType();

      final PanamaImportDescriptor descriptor =
          new PanamaImportDescriptor("module_name-1", "func.name$test", type);

      assertEquals("module_name-1", descriptor.getModuleName());
      assertEquals("func.name$test", descriptor.getName());
    }

    @Test
    @DisplayName("Multiple descriptors should be independent")
    void multipleDescriptorsShouldBeIndependent() {
      final WasmType type1 = createTestType();
      final WasmType type2 = createMemoryType();

      final PanamaImportDescriptor descriptor1 = new PanamaImportDescriptor("mod1", "func1", type1);
      final PanamaImportDescriptor descriptor2 = new PanamaImportDescriptor("mod2", "func2", type2);

      assertNotEquals(descriptor1.getModuleName(), descriptor2.getModuleName());
      assertNotEquals(descriptor1.getName(), descriptor2.getName());
      assertNotEquals(descriptor1.getType().getKind(), descriptor2.getType().getKind());
    }
  }
}
