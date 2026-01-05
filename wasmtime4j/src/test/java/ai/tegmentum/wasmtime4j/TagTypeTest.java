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
 * Comprehensive test suite for the TagType interface.
 *
 * <p>TagType represents the type of a WebAssembly exception tag, wrapping a FunctionType to
 * describe the signature of exception payloads. This test verifies the interface structure and
 * method signatures.
 */
@DisplayName("TagType Interface Tests")
class TagTypeTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(TagType.class.isInterface(), "TagType should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(TagType.class.getModifiers()), "TagType should be public");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have static create method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      final Method method = TagType.class.getMethod("create", FunctionType.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "create should be public");
    }

    @Test
    @DisplayName("create method should return TagType")
    void createMethodShouldReturnTagType() throws NoSuchMethodException {
      final Method method = TagType.class.getMethod("create", FunctionType.class);
      assertEquals(TagType.class, method.getReturnType(), "create should return TagType");
    }

    @Test
    @DisplayName("create method should have 1 parameter")
    void createMethodShouldHave1Parameter() throws NoSuchMethodException {
      final Method method = TagType.class.getMethod("create", FunctionType.class);
      assertEquals(1, method.getParameterCount(), "create should have 1 parameter");
      assertEquals(
          FunctionType.class, method.getParameterTypes()[0], "Parameter should be FunctionType");
    }
  }

  // ========================================================================
  // Instance Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Methods Tests")
  class InstanceMethodsTests {

    @Test
    @DisplayName("should have getFunctionType method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      final Method method = TagType.class.getMethod("getFunctionType");
      assertNotNull(method, "getFunctionType method should exist");
      assertEquals(
          FunctionType.class, method.getReturnType(), "getFunctionType should return FunctionType");
    }

    @Test
    @DisplayName("getFunctionType should have no parameters")
    void getFunctionTypeShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = TagType.class.getMethod("getFunctionType");
      assertEquals(0, method.getParameterCount(), "getFunctionType should have 0 parameters");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods = Set.of("create", "getFunctionType");

      Set<String> actualMethods =
          Arrays.stream(TagType.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "TagType should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 2 declared methods")
    void shouldHaveAtLeast2DeclaredMethods() {
      assertTrue(
          TagType.class.getDeclaredMethods().length >= 2, "TagType should have at least 2 methods");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0, TagType.class.getInterfaces().length, "TagType should not extend any interface");
    }
  }
}
