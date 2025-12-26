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

package ai.tegmentum.wasmtime4j.jni.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniExportDescriptor} class.
 *
 * <p>This test class verifies the JNI implementation of ExportDescriptor interface
 * for WebAssembly export descriptors.
 */
@DisplayName("JniExportDescriptor Tests")
class JniExportDescriptorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniExportDescriptor should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(JniExportDescriptor.class.getModifiers()),
          "JniExportDescriptor should be final");
    }

    @Test
    @DisplayName("JniExportDescriptor should implement ExportDescriptor")
    void shouldImplementExportDescriptor() {
      assertTrue(ExportDescriptor.class.isAssignableFrom(JniExportDescriptor.class),
          "JniExportDescriptor should implement ExportDescriptor");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create with valid arguments")
    void constructorShouldCreateWithValidArguments() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("main", funcType);
      assertNotNull(descriptor, "Descriptor should not be null");
      assertEquals("main", descriptor.getName(), "Name should be 'main'");
      assertEquals(funcType, descriptor.getType(), "Type should match");
    }

    @Test
    @DisplayName("Constructor should throw for null name")
    void constructorShouldThrowForNullName() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      assertThrows(IllegalArgumentException.class, () ->
              new JniExportDescriptor(null, funcType),
          "Should throw for null name");
    }

    @Test
    @DisplayName("Constructor should throw for null type")
    void constructorShouldThrowForNullType() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniExportDescriptor("test", null),
          "Should throw for null type");
    }

    @Test
    @DisplayName("Constructor should accept empty name")
    void constructorShouldAcceptEmptyName() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("", funcType);
      assertEquals("", descriptor.getName(), "Name should be empty");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("Should work with FuncType")
    void shouldWorkWithFuncType() {
      final JniFuncType funcType = new JniFuncType(
          Arrays.asList(WasmValueType.I32, WasmValueType.I32),
          Arrays.asList(WasmValueType.I32)
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("add", funcType);
      assertEquals(WasmTypeKind.FUNCTION, descriptor.getType().getKind(),
          "Type kind should be FUNCTION");
    }

    @Test
    @DisplayName("Should work with GlobalType")
    void shouldWorkWithGlobalType() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      final JniExportDescriptor descriptor = new JniExportDescriptor("counter", globalType);
      assertEquals(WasmTypeKind.GLOBAL, descriptor.getType().getKind(),
          "Type kind should be GLOBAL");
    }

    @Test
    @DisplayName("Should work with MemoryType")
    void shouldWorkWithMemoryType() {
      final JniMemoryType memoryType = new JniMemoryType(1, 256L, false, false);
      final JniExportDescriptor descriptor = new JniExportDescriptor("memory", memoryType);
      assertEquals(WasmTypeKind.MEMORY, descriptor.getType().getKind(),
          "Type kind should be MEMORY");
    }

    @Test
    @DisplayName("Should work with TableType")
    void shouldWorkWithTableType() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, 100L);
      final JniExportDescriptor descriptor = new JniExportDescriptor("table", tableType);
      assertEquals(WasmTypeKind.TABLE, descriptor.getType().getKind(),
          "Type kind should be TABLE");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("Same instance should be equal")
    void sameInstanceShouldBeEqual() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("test", funcType);
      assertEquals(descriptor, descriptor, "Same instance should be equal");
    }

    @Test
    @DisplayName("Equal values should be equal")
    void equalValuesShouldBeEqual() {
      final JniFuncType funcType1 = new JniFuncType(
          Arrays.asList(WasmValueType.I32),
          Arrays.asList(WasmValueType.I32)
      );
      final JniFuncType funcType2 = new JniFuncType(
          Arrays.asList(WasmValueType.I32),
          Arrays.asList(WasmValueType.I32)
      );
      final JniExportDescriptor descriptor1 = new JniExportDescriptor("test", funcType1);
      final JniExportDescriptor descriptor2 = new JniExportDescriptor("test", funcType2);
      assertEquals(descriptor1, descriptor2, "Equal values should be equal");
    }

    @Test
    @DisplayName("Different name should not be equal")
    void differentNameShouldNotBeEqual() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor1 = new JniExportDescriptor("test1", funcType);
      final JniExportDescriptor descriptor2 = new JniExportDescriptor("test2", funcType);
      assertNotEquals(descriptor1, descriptor2, "Different name should not be equal");
    }

    @Test
    @DisplayName("Different type should not be equal")
    void differentTypeShouldNotBeEqual() {
      final JniFuncType funcType1 = new JniFuncType(
          Arrays.asList(WasmValueType.I32),
          Collections.emptyList()
      );
      final JniFuncType funcType2 = new JniFuncType(
          Arrays.asList(WasmValueType.I64),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor1 = new JniExportDescriptor("test", funcType1);
      final JniExportDescriptor descriptor2 = new JniExportDescriptor("test", funcType2);
      assertNotEquals(descriptor1, descriptor2, "Different type should not be equal");
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("test", funcType);
      assertFalse(descriptor.equals(null), "Should not be equal to null");
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("test", funcType);
      assertFalse(descriptor.equals("string"), "Should not be equal to different type");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("Equal objects should have equal hashCodes")
    void equalObjectsShouldHaveEqualHashCodes() {
      final JniFuncType funcType1 = new JniFuncType(
          Arrays.asList(WasmValueType.I32),
          Arrays.asList(WasmValueType.I64)
      );
      final JniFuncType funcType2 = new JniFuncType(
          Arrays.asList(WasmValueType.I32),
          Arrays.asList(WasmValueType.I64)
      );
      final JniExportDescriptor descriptor1 = new JniExportDescriptor("test", funcType1);
      final JniExportDescriptor descriptor2 = new JniExportDescriptor("test", funcType2);
      assertEquals(descriptor1.hashCode(), descriptor2.hashCode(),
          "Equal objects should have equal hashCodes");
    }

    @Test
    @DisplayName("HashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("test", funcType);
      final int hash1 = descriptor.hashCode();
      final int hash2 = descriptor.hashCode();
      assertEquals(hash1, hash2, "HashCode should be consistent");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include name")
    void toStringShouldIncludeName() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("myExport", funcType);
      final String str = descriptor.toString();
      assertTrue(str.contains("myExport"), "toString should include name");
    }

    @Test
    @DisplayName("toString should include type info")
    void toStringShouldIncludeTypeInfo() {
      final JniFuncType funcType = new JniFuncType(
          Arrays.asList(WasmValueType.I32),
          Arrays.asList(WasmValueType.I64)
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("test", funcType);
      final String str = descriptor.toString();
      assertTrue(str.contains("type") || str.contains("Func"),
          "toString should include type info");
    }

    @Test
    @DisplayName("toString should include ExportDescriptor")
    void toStringShouldIncludeExportDescriptor() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Collections.emptyList()
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("test", funcType);
      final String str = descriptor.toString();
      assertTrue(str.contains("ExportDescriptor"), "toString should include ExportDescriptor");
    }
  }

  @Nested
  @DisplayName("fromNative Tests")
  class FromNativeTests {

    @Test
    @DisplayName("fromNative should throw for zero handle")
    void fromNativeShouldThrowForZeroHandle() {
      assertThrows(IllegalArgumentException.class, () ->
              JniExportDescriptor.fromNative(0),
          "Should throw for zero handle");
    }

    @Test
    @DisplayName("fromNative should throw for negative handle")
    void fromNativeShouldThrowForNegativeHandle() {
      assertThrows(IllegalArgumentException.class, () ->
              JniExportDescriptor.fromNative(-1),
          "Should throw for negative handle");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Typical memory export should work")
    void typicalMemoryExportShouldWork() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);
      final JniExportDescriptor descriptor = new JniExportDescriptor("memory", memoryType);
      assertEquals("memory", descriptor.getName(), "Name should be 'memory'");
      assertEquals(WasmTypeKind.MEMORY, descriptor.getType().getKind(),
          "Type should be memory");
    }

    @Test
    @DisplayName("Typical function export should work")
    void typicalFunctionExportShouldWork() {
      final JniFuncType funcType = new JniFuncType(
          Arrays.asList(WasmValueType.I32, WasmValueType.I32),
          Arrays.asList(WasmValueType.I32)
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("add", funcType);
      assertEquals("add", descriptor.getName(), "Name should be 'add'");
      assertEquals(WasmTypeKind.FUNCTION, descriptor.getType().getKind(),
          "Type should be function");
    }

    @Test
    @DisplayName("Main function export should work")
    void mainFunctionExportShouldWork() {
      final JniFuncType funcType = new JniFuncType(
          Collections.emptyList(),
          Arrays.asList(WasmValueType.I32)
      );
      final JniExportDescriptor descriptor = new JniExportDescriptor("_start", funcType);
      assertEquals("_start", descriptor.getName(), "Name should be '_start'");
    }

    @Test
    @DisplayName("Table export should work")
    void tableExportShouldWork() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      final JniExportDescriptor descriptor = new JniExportDescriptor(
          "__indirect_function_table", tableType
      );
      assertEquals(WasmTypeKind.TABLE, descriptor.getType().getKind(),
          "Type should be table");
    }

    @Test
    @DisplayName("Global export should work")
    void globalExportShouldWork() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, true);
      final JniExportDescriptor descriptor = new JniExportDescriptor(
          "__stack_pointer", globalType
      );
      assertEquals(WasmTypeKind.GLOBAL, descriptor.getType().getKind(),
          "Type should be global");
    }

    @Test
    @DisplayName("Multiple exports with same type but different names should work")
    void multipleExportsWithSameTypeButDifferentNamesShouldWork() {
      final JniFuncType funcType = new JniFuncType(
          Arrays.asList(WasmValueType.I32),
          Arrays.asList(WasmValueType.I32)
      );
      final JniExportDescriptor descriptor1 = new JniExportDescriptor("increment", funcType);
      final JniExportDescriptor descriptor2 = new JniExportDescriptor("decrement", funcType);

      assertNotEquals(descriptor1, descriptor2, "Different names should not be equal");
      assertEquals(descriptor1.getType(), descriptor2.getType(), "Types should be equal");
    }
  }
}
