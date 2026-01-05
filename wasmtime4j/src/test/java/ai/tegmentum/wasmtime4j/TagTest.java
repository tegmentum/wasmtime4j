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
 * Comprehensive test suite for the Tag interface.
 *
 * <p>Tag represents a WebAssembly exception tag used in the exception handling proposal. This test
 * verifies the interface structure and method signatures.
 */
@DisplayName("Tag Interface Tests")
class TagTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Tag.class.isInterface(), "Tag should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Tag.class.getModifiers()), "Tag should be public");
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
      final Method method = Tag.class.getMethod("create", Store.class, TagType.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "create should be public");
    }

    @Test
    @DisplayName("create method should return Tag")
    void createMethodShouldReturnTag() throws NoSuchMethodException {
      final Method method = Tag.class.getMethod("create", Store.class, TagType.class);
      assertEquals(Tag.class, method.getReturnType(), "create should return Tag");
    }

    @Test
    @DisplayName("create method should have 2 parameters")
    void createMethodShouldHave2Parameters() throws NoSuchMethodException {
      final Method method = Tag.class.getMethod("create", Store.class, TagType.class);
      assertEquals(2, method.getParameterCount(), "create should have 2 parameters");
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(Store.class, paramTypes[0], "First parameter should be Store");
      assertEquals(TagType.class, paramTypes[1], "Second parameter should be TagType");
    }
  }

  // ========================================================================
  // Instance Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Methods Tests")
  class InstanceMethodsTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = Tag.class.getMethod("getType", Store.class);
      assertNotNull(method, "getType method should exist");
      assertEquals(TagType.class, method.getReturnType(), "getType should return TagType");
    }

    @Test
    @DisplayName("should have equals method with Tag and Store parameters")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = Tag.class.getMethod("equals", Tag.class, Store.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "equals should return boolean");
    }

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = Tag.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "getNativeHandle should return long");
    }

    @Test
    @DisplayName("getType should have 1 parameter")
    void getTypeShouldHave1Parameter() throws NoSuchMethodException {
      final Method method = Tag.class.getMethod("getType", Store.class);
      assertEquals(1, method.getParameterCount(), "getType should have 1 parameter");
      assertEquals(Store.class, method.getParameterTypes()[0], "Parameter should be Store");
    }

    @Test
    @DisplayName("equals should have 2 parameters")
    void equalsShouldHave2Parameters() throws NoSuchMethodException {
      final Method method = Tag.class.getMethod("equals", Tag.class, Store.class);
      assertEquals(2, method.getParameterCount(), "equals should have 2 parameters");
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(Tag.class, paramTypes[0], "First parameter should be Tag");
      assertEquals(Store.class, paramTypes[1], "Second parameter should be Store");
    }

    @Test
    @DisplayName("getNativeHandle should have no parameters")
    void getNativeHandleShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = Tag.class.getMethod("getNativeHandle");
      assertEquals(0, method.getParameterCount(), "getNativeHandle should have 0 parameters");
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
      Set<String> expectedMethods = Set.of("create", "getType", "equals", "getNativeHandle");

      Set<String> actualMethods =
          Arrays.stream(Tag.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Tag should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 4 declared methods")
    void shouldHaveAtLeast4DeclaredMethods() {
      assertTrue(Tag.class.getDeclaredMethods().length >= 4, "Tag should have at least 4 methods");
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
      assertEquals(0, Tag.class.getInterfaces().length, "Tag should not extend any interface");
    }
  }
}
