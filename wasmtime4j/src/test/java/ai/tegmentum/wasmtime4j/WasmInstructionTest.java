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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmInstruction interface.
 *
 * <p>WasmInstruction represents a WebAssembly instruction with metadata including opcode, operands,
 * and execution characteristics.
 */
@DisplayName("WasmInstruction Interface Tests")
class WasmInstructionTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmInstruction.class.isInterface(), "WasmInstruction should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmInstruction.class.getModifiers()),
          "WasmInstruction should be public");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getOpcode method")
    void shouldHaveGetOpcodeMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.class.getMethod("getOpcode");
      assertNotNull(method, "getOpcode method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMnemonic method")
    void shouldHaveGetMnemonicMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.class.getMethod("getMnemonic");
      assertNotNull(method, "getMnemonic method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getOperands method")
    void shouldHaveGetOperandsMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.class.getMethod("getOperands");
      assertNotNull(method, "getOperands method should exist");
      assertEquals(Object[].class, method.getReturnType(), "Should return Object[]");
    }

    @Test
    @DisplayName("should have getCategory method")
    void shouldHaveGetCategoryMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.class.getMethod("getCategory");
      assertNotNull(method, "getCategory method should exist");
      assertEquals(
          WasmInstruction.InstructionCategory.class,
          method.getReturnType(),
          "Should return InstructionCategory");
    }

    @Test
    @DisplayName("should have canTrap method")
    void shouldHaveCanTrapMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.class.getMethod("canTrap");
      assertNotNull(method, "canTrap method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getStackEffect method")
    void shouldHaveGetStackEffectMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.class.getMethod("getStackEffect");
      assertNotNull(method, "getStackEffect method should exist");
      assertEquals(
          WasmInstruction.StackEffect.class, method.getReturnType(), "Should return StackEffect");
    }

    @Test
    @DisplayName("should have static create factory method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method method =
          WasmInstruction.class.getMethod(
              "create",
              int.class,
              String.class,
              WasmInstruction.InstructionCategory.class,
              Object[].class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(WasmInstruction.class, method.getReturnType(), "Should return WasmInstruction");
    }

    @Test
    @DisplayName("should have static createStackEffect method")
    void shouldHaveStaticCreateStackEffectMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.class.getMethod("createStackEffect", int.class, String.class);
      assertNotNull(method, "createStackEffect method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasmInstruction.StackEffect.class, method.getReturnType(), "Should return StackEffect");
    }
  }

  // ========================================================================
  // InstructionCategory Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("InstructionCategory Enum Tests")
  class InstructionCategoryEnumTests {

    @Test
    @DisplayName("should be a nested enum")
    void shouldBeNestedEnum() {
      assertTrue(
          WasmInstruction.InstructionCategory.class.isEnum(),
          "InstructionCategory should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmInstruction.InstructionCategory.class.getModifiers()),
          "InstructionCategory should be public");
    }

    @Test
    @DisplayName("should have NUMERIC category")
    void shouldHaveNumericCategory() {
      assertNotNull(WasmInstruction.InstructionCategory.valueOf("NUMERIC"), "NUMERIC should exist");
    }

    @Test
    @DisplayName("should have CONTROL category")
    void shouldHaveControlCategory() {
      assertNotNull(WasmInstruction.InstructionCategory.valueOf("CONTROL"), "CONTROL should exist");
    }

    @Test
    @DisplayName("should have MEMORY category")
    void shouldHaveMemoryCategory() {
      assertNotNull(WasmInstruction.InstructionCategory.valueOf("MEMORY"), "MEMORY should exist");
    }

    @Test
    @DisplayName("should have VARIABLE category")
    void shouldHaveVariableCategory() {
      assertNotNull(
          WasmInstruction.InstructionCategory.valueOf("VARIABLE"), "VARIABLE should exist");
    }

    @Test
    @DisplayName("should have REFERENCE category")
    void shouldHaveReferenceCategory() {
      assertNotNull(
          WasmInstruction.InstructionCategory.valueOf("REFERENCE"), "REFERENCE should exist");
    }

    @Test
    @DisplayName("should have TABLE category")
    void shouldHaveTableCategory() {
      assertNotNull(WasmInstruction.InstructionCategory.valueOf("TABLE"), "TABLE should exist");
    }

    @Test
    @DisplayName("should have SIMD category")
    void shouldHaveSimdCategory() {
      assertNotNull(WasmInstruction.InstructionCategory.valueOf("SIMD"), "SIMD should exist");
    }

    @Test
    @DisplayName("should have ATOMIC category")
    void shouldHaveAtomicCategory() {
      assertNotNull(WasmInstruction.InstructionCategory.valueOf("ATOMIC"), "ATOMIC should exist");
    }

    @Test
    @DisplayName("should have BULK_MEMORY category")
    void shouldHaveBulkMemoryCategory() {
      assertNotNull(
          WasmInstruction.InstructionCategory.valueOf("BULK_MEMORY"), "BULK_MEMORY should exist");
    }

    @Test
    @DisplayName("should have exactly 9 categories")
    void shouldHaveExactly9Categories() {
      assertEquals(
          9,
          WasmInstruction.InstructionCategory.values().length,
          "Should have exactly 9 categories");
    }
  }

  // ========================================================================
  // StackEffect Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("StackEffect Interface Tests")
  class StackEffectInterfaceTests {

    @Test
    @DisplayName("should be a nested interface")
    void shouldBeNestedInterface() {
      assertTrue(
          WasmInstruction.StackEffect.class.isInterface(), "StackEffect should be an interface");
    }

    @Test
    @DisplayName("should have getPops method")
    void shouldHaveGetPopsMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.StackEffect.class.getMethod("getPops");
      assertNotNull(method, "getPops method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPushes method")
    void shouldHaveGetPushesMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.StackEffect.class.getMethod("getPushes");
      assertNotNull(method, "getPushes method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getNetEffect default method")
    void shouldHaveGetNetEffectMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.StackEffect.class.getMethod("getNetEffect");
      assertNotNull(method, "getNetEffect method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertTrue(method.isDefault(), "Should be a default method");
    }

    @Test
    @DisplayName("should have getPopTypes method")
    void shouldHaveGetPopTypesMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.StackEffect.class.getMethod("getPopTypes");
      assertNotNull(method, "getPopTypes method should exist");
      assertEquals(WasmValueType[].class, method.getReturnType(), "Should return WasmValueType[]");
    }

    @Test
    @DisplayName("should have getPushTypes method")
    void shouldHaveGetPushTypesMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.StackEffect.class.getMethod("getPushTypes");
      assertNotNull(method, "getPushTypes method should exist");
      assertEquals(WasmValueType[].class, method.getReturnType(), "Should return WasmValueType[]");
    }

    @Test
    @DisplayName("should have isPolymorphic method")
    void shouldHaveIsPolymorphicMethod() throws NoSuchMethodException {
      Method method = WasmInstruction.StackEffect.class.getMethod("isPolymorphic");
      assertNotNull(method, "isPolymorphic method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  // ========================================================================
  // WasmInstructionHelper Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmInstructionHelper Tests")
  class WasmInstructionHelperTests {

    @Test
    @DisplayName("should be a nested final class")
    void shouldBeNestedFinalClass() {
      assertTrue(
          Modifier.isFinal(WasmInstruction.WasmInstructionHelper.class.getModifiers()),
          "WasmInstructionHelper should be final");
    }

    @Test
    @DisplayName("should have inferTypeFromMnemonic static method")
    void shouldHaveInferTypeFromMnemonicMethod() throws NoSuchMethodException {
      Method method =
          WasmInstruction.WasmInstructionHelper.class.getDeclaredMethod(
              "inferTypeFromMnemonic", String.class);
      assertNotNull(method, "inferTypeFromMnemonic method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have expected nested classes")
    void shouldHaveExpectedNestedClasses() {
      Set<String> nestedClassNames =
          Arrays.stream(WasmInstruction.class.getDeclaredClasses())
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      assertTrue(
          nestedClassNames.contains("InstructionCategory"), "Should have InstructionCategory");
      assertTrue(nestedClassNames.contains("StackEffect"), "Should have StackEffect");
      assertTrue(
          nestedClassNames.contains("WasmInstructionHelper"), "Should have WasmInstructionHelper");
    }

    @Test
    @DisplayName("should have exactly 3 nested types")
    void shouldHaveExactly3NestedTypes() {
      assertEquals(
          3,
          WasmInstruction.class.getDeclaredClasses().length,
          "Should have 3 nested types (InstructionCategory, StackEffect, WasmInstructionHelper)");
    }
  }
}
