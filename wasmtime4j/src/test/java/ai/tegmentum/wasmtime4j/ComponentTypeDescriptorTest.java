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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentTypeDescriptor} interface.
 *
 * <p>ComponentTypeDescriptor provides full type information for Component Model types.
 */
@DisplayName("ComponentTypeDescriptor Tests")
class ComponentTypeDescriptorTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentTypeDescriptor.class.getModifiers()),
          "ComponentTypeDescriptor should be public");
      assertTrue(
          ComponentTypeDescriptor.class.isInterface(),
          "ComponentTypeDescriptor should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ComponentType.class, method.getReturnType(), "Should return ComponentType");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getElementType method")
    void shouldHaveGetElementTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getElementType");
      assertNotNull(method, "getElementType method should exist");
      assertEquals(
          ComponentTypeDescriptor.class,
          method.getReturnType(),
          "Should return ComponentTypeDescriptor");
    }

    @Test
    @DisplayName("should have getRecordFields method")
    void shouldHaveGetRecordFieldsMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getRecordFields");
      assertNotNull(method, "getRecordFields method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getTupleElements method")
    void shouldHaveGetTupleElementsMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getTupleElements");
      assertNotNull(method, "getTupleElements method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getVariantCases method")
    void shouldHaveGetVariantCasesMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getVariantCases");
      assertNotNull(method, "getVariantCases method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getEnumCases method")
    void shouldHaveGetEnumCasesMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getEnumCases");
      assertNotNull(method, "getEnumCases method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getOptionType method")
    void shouldHaveGetOptionTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getOptionType");
      assertNotNull(method, "getOptionType method should exist");
      assertEquals(
          ComponentTypeDescriptor.class,
          method.getReturnType(),
          "Should return ComponentTypeDescriptor");
    }

    @Test
    @DisplayName("should have getResultOkType method")
    void shouldHaveGetResultOkTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getResultOkType");
      assertNotNull(method, "getResultOkType method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getResultErrType method")
    void shouldHaveGetResultErrTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getResultErrType");
      assertNotNull(method, "getResultErrType method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getFlagNames method")
    void shouldHaveGetFlagNamesMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getFlagNames");
      assertNotNull(method, "getFlagNames method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getResourceTypeName method")
    void shouldHaveGetResourceTypeNameMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("getResourceTypeName");
      assertNotNull(method, "getResourceTypeName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests - Primitives")
  class StaticPrimitiveFactoryMethodTests {

    @Test
    @DisplayName("should have bool static factory method")
    void shouldHaveBoolStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("bool");
      assertNotNull(method, "bool method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "bool should be static");
      assertEquals(
          ComponentTypeDescriptor.class,
          method.getReturnType(),
          "Should return ComponentTypeDescriptor");
    }

    @Test
    @DisplayName("should have s32 static factory method")
    void shouldHaveS32StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("s32");
      assertNotNull(method, "s32 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "s32 should be static");
    }

    @Test
    @DisplayName("should have string static factory method")
    void shouldHaveStringStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentTypeDescriptor.class.getMethod("string");
      assertNotNull(method, "string method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "string should be static");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests - Compound")
  class StaticCompoundFactoryMethodTests {

    @Test
    @DisplayName("should have named static factory method")
    void shouldHaveNamedStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentTypeDescriptor.class.getMethod(
              "named", String.class, ComponentTypeDescriptor.class);
      assertNotNull(method, "named method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "named should be static");
    }

    @Test
    @DisplayName("should have list static factory method")
    void shouldHaveListStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentTypeDescriptor.class.getMethod("list", ComponentTypeDescriptor.class);
      assertNotNull(method, "list method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "list should be static");
    }

    @Test
    @DisplayName("should have option static factory method")
    void shouldHaveOptionStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentTypeDescriptor.class.getMethod("option", ComponentTypeDescriptor.class);
      assertNotNull(method, "option method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "option should be static");
    }

    @Test
    @DisplayName("should have result static factory method")
    void shouldHaveResultStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentTypeDescriptor.class.getMethod(
              "result", ComponentTypeDescriptor.class, ComponentTypeDescriptor.class);
      assertNotNull(method, "result method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "result should be static");
    }
  }

  @Nested
  @DisplayName("Primitive Type Descriptor Behavior Tests")
  class PrimitiveTypeDescriptorBehaviorTests {

    @Test
    @DisplayName("bool descriptor should have correct type")
    void boolDescriptorShouldHaveCorrectType() {
      final ComponentTypeDescriptor desc = ComponentTypeDescriptor.bool();
      assertNotNull(desc, "Should create descriptor");
      assertEquals(ComponentType.BOOL, desc.getType(), "Type should be BOOL");
      assertTrue(desc.getName().isEmpty(), "Should not have a name");
    }

    @Test
    @DisplayName("s32 descriptor should have correct type")
    void s32DescriptorShouldHaveCorrectType() {
      final ComponentTypeDescriptor desc = ComponentTypeDescriptor.s32();
      assertNotNull(desc, "Should create descriptor");
      assertEquals(ComponentType.S32, desc.getType(), "Type should be S32");
    }

    @Test
    @DisplayName("primitive descriptor should throw for non-applicable methods")
    void primitiveDescriptorShouldThrowForNonApplicableMethods() {
      final ComponentTypeDescriptor desc = ComponentTypeDescriptor.s32();

      assertThrows(
          IllegalStateException.class, desc::getElementType, "Should throw for getElementType");
      assertThrows(
          IllegalStateException.class, desc::getRecordFields, "Should throw for getRecordFields");
      assertThrows(
          IllegalStateException.class, desc::getTupleElements, "Should throw for getTupleElements");
      assertThrows(
          IllegalStateException.class, desc::getVariantCases, "Should throw for getVariantCases");
      assertThrows(
          IllegalStateException.class, desc::getEnumCases, "Should throw for getEnumCases");
      assertThrows(
          IllegalStateException.class, desc::getOptionType, "Should throw for getOptionType");
      assertThrows(
          IllegalStateException.class, desc::getResultOkType, "Should throw for getResultOkType");
      assertThrows(
          IllegalStateException.class, desc::getFlagNames, "Should throw for getFlagNames");
      assertThrows(
          IllegalStateException.class,
          desc::getResourceTypeName,
          "Should throw for getResourceTypeName");
    }
  }

  @Nested
  @DisplayName("List Type Descriptor Behavior Tests")
  class ListTypeDescriptorBehaviorTests {

    @Test
    @DisplayName("list descriptor should have correct type")
    void listDescriptorShouldHaveCorrectType() {
      final ComponentTypeDescriptor elementType = ComponentTypeDescriptor.s32();
      final ComponentTypeDescriptor desc = ComponentTypeDescriptor.list(elementType);

      assertNotNull(desc, "Should create descriptor");
      assertEquals(ComponentType.LIST, desc.getType(), "Type should be LIST");
      assertEquals(elementType, desc.getElementType(), "Element type should match");
    }

    @Test
    @DisplayName("list descriptor toString should include element type")
    void listDescriptorToStringShouldIncludeElementType() {
      final ComponentTypeDescriptor desc =
          ComponentTypeDescriptor.list(ComponentTypeDescriptor.s32());
      assertTrue(desc.toString().contains("list"), "toString should contain 'list'");
    }
  }

  @Nested
  @DisplayName("Option Type Descriptor Behavior Tests")
  class OptionTypeDescriptorBehaviorTests {

    @Test
    @DisplayName("option descriptor should have correct type")
    void optionDescriptorShouldHaveCorrectType() {
      final ComponentTypeDescriptor innerType = ComponentTypeDescriptor.string();
      final ComponentTypeDescriptor desc = ComponentTypeDescriptor.option(innerType);

      assertNotNull(desc, "Should create descriptor");
      assertEquals(ComponentType.OPTION, desc.getType(), "Type should be OPTION");
      assertEquals(innerType, desc.getOptionType(), "Option type should match");
    }
  }

  @Nested
  @DisplayName("Result Type Descriptor Behavior Tests")
  class ResultTypeDescriptorBehaviorTests {

    @Test
    @DisplayName("result descriptor should have correct type")
    void resultDescriptorShouldHaveCorrectType() {
      final ComponentTypeDescriptor okType = ComponentTypeDescriptor.s32();
      final ComponentTypeDescriptor errType = ComponentTypeDescriptor.string();
      final ComponentTypeDescriptor desc = ComponentTypeDescriptor.result(okType, errType);

      assertNotNull(desc, "Should create descriptor");
      assertEquals(ComponentType.RESULT, desc.getType(), "Type should be RESULT");
      assertTrue(desc.getResultOkType().isPresent(), "Ok type should be present");
      assertTrue(desc.getResultErrType().isPresent(), "Err type should be present");
      assertEquals(okType, desc.getResultOkType().get(), "Ok type should match");
      assertEquals(errType, desc.getResultErrType().get(), "Err type should match");
    }

    @Test
    @DisplayName("result descriptor with null types should return empty optional")
    void resultDescriptorWithNullTypesShouldReturnEmptyOptional() {
      final ComponentTypeDescriptor desc = ComponentTypeDescriptor.result(null, null);

      assertNotNull(desc, "Should create descriptor");
      assertTrue(desc.getResultOkType().isEmpty(), "Ok type should be empty");
      assertTrue(desc.getResultErrType().isEmpty(), "Err type should be empty");
    }
  }

  @Nested
  @DisplayName("Named Type Descriptor Behavior Tests")
  class NamedTypeDescriptorBehaviorTests {

    @Test
    @DisplayName("named descriptor should have name")
    void namedDescriptorShouldHaveName() {
      final ComponentTypeDescriptor inner = ComponentTypeDescriptor.s32();
      final ComponentTypeDescriptor desc = ComponentTypeDescriptor.named("count", inner);

      assertNotNull(desc, "Should create descriptor");
      assertEquals(ComponentType.S32, desc.getType(), "Type should match inner type");
      assertTrue(desc.getName().isPresent(), "Should have a name");
      assertEquals("count", desc.getName().get(), "Name should match");
    }

    @Test
    @DisplayName("named descriptor should delegate to inner for type-specific methods")
    void namedDescriptorShouldDelegateToInnerForTypeSpecificMethods() {
      final ComponentTypeDescriptor elementType = ComponentTypeDescriptor.s32();
      final ComponentTypeDescriptor inner = ComponentTypeDescriptor.list(elementType);
      final ComponentTypeDescriptor desc = ComponentTypeDescriptor.named("items", inner);

      assertEquals(ComponentType.LIST, desc.getType(), "Type should be LIST");
      assertEquals(
          elementType, desc.getElementType(), "Element type should match inner's element type");
    }
  }

  @Nested
  @DisplayName("Nested Class Tests")
  class NestedClassTests {

    @Test
    @DisplayName("should have PrimitiveImpl nested class")
    void shouldHavePrimitiveImplNestedClass() {
      final var nestedClasses = ComponentTypeDescriptor.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("PrimitiveImpl")) {
          found = true;
          assertTrue(
              ComponentTypeDescriptor.class.isAssignableFrom(nestedClass),
              "PrimitiveImpl should implement ComponentTypeDescriptor");
          break;
        }
      }
      assertTrue(found, "Should have PrimitiveImpl nested class");
    }

    @Test
    @DisplayName("should have ListImpl nested class")
    void shouldHaveListImplNestedClass() {
      final var nestedClasses = ComponentTypeDescriptor.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ListImpl")) {
          found = true;
          assertTrue(
              ComponentTypeDescriptor.class.isAssignableFrom(nestedClass),
              "ListImpl should implement ComponentTypeDescriptor");
          break;
        }
      }
      assertTrue(found, "Should have ListImpl nested class");
    }

    @Test
    @DisplayName("should have OptionImpl nested class")
    void shouldHaveOptionImplNestedClass() {
      final var nestedClasses = ComponentTypeDescriptor.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("OptionImpl")) {
          found = true;
          assertTrue(
              ComponentTypeDescriptor.class.isAssignableFrom(nestedClass),
              "OptionImpl should implement ComponentTypeDescriptor");
          break;
        }
      }
      assertTrue(found, "Should have OptionImpl nested class");
    }

    @Test
    @DisplayName("should have ResultImpl nested class")
    void shouldHaveResultImplNestedClass() {
      final var nestedClasses = ComponentTypeDescriptor.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ResultImpl")) {
          found = true;
          assertTrue(
              ComponentTypeDescriptor.class.isAssignableFrom(nestedClass),
              "ResultImpl should implement ComponentTypeDescriptor");
          break;
        }
      }
      assertTrue(found, "Should have ResultImpl nested class");
    }

    @Test
    @DisplayName("should have NamedImpl nested class")
    void shouldHaveNamedImplNestedClass() {
      final var nestedClasses = ComponentTypeDescriptor.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("NamedImpl")) {
          found = true;
          assertTrue(
              ComponentTypeDescriptor.class.isAssignableFrom(nestedClass),
              "NamedImpl should implement ComponentTypeDescriptor");
          break;
        }
      }
      assertTrue(found, "Should have NamedImpl nested class");
    }
  }
}
