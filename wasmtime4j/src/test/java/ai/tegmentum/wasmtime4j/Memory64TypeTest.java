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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Memory64Type interface.
 *
 * <p>Memory64Type extends MemoryType to provide 64-bit addressing capabilities.
 */
@DisplayName("Memory64Type Interface Tests")
class Memory64TypeTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Memory64Type.class.isInterface(), "Memory64Type should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Memory64Type.class.getModifiers()), "Memory64Type should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend MemoryType")
    void shouldExtendMemoryType() {
      assertTrue(
          MemoryType.class.isAssignableFrom(Memory64Type.class),
          "Memory64Type should extend MemoryType");
    }

    @Test
    @DisplayName("should have exactly 1 super interface")
    void shouldHaveExactly1SuperInterface() {
      assertEquals(
          1, Memory64Type.class.getInterfaces().length, "Should extend exactly 1 interface");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getMinimum64 abstract method")
    void shouldHaveGetMinimum64Method() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("getMinimum64");
      assertNotNull(method, "getMinimum64 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaximum64 abstract method")
    void shouldHaveGetMaximum64Method() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("getMaximum64");
      assertNotNull(method, "getMaximum64 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have getMinimum default method overriding MemoryType")
    void shouldHaveGetMinimumDefaultMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("getMinimum");
      assertNotNull(method, "getMinimum method should exist");
      assertTrue(method.isDefault(), "getMinimum should be a default method");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaximum default method overriding MemoryType")
    void shouldHaveGetMaximumDefaultMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("getMaximum");
      assertNotNull(method, "getMaximum method should exist");
      assertTrue(method.isDefault(), "getMaximum should be a default method");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have is64Bit default method")
    void shouldHaveIs64BitDefaultMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("is64Bit");
      assertNotNull(method, "is64Bit method should exist");
      assertTrue(method.isDefault(), "is64Bit should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getPageSizeBytes default method")
    void shouldHaveGetPageSizeBytesDefaultMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("getPageSizeBytes");
      assertNotNull(method, "getPageSizeBytes method should exist");
      assertTrue(method.isDefault(), "getPageSizeBytes should be a default method");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaximumSizeBytes default method")
    void shouldHaveGetMaximumSizeBytesDefaultMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("getMaximumSizeBytes");
      assertNotNull(method, "getMaximumSizeBytes method should exist");
      assertTrue(method.isDefault(), "getMaximumSizeBytes should be a default method");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getMinimumSizeBytes default method")
    void shouldHaveGetMinimumSizeBytesDefaultMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("getMinimumSizeBytes");
      assertNotNull(method, "getMinimumSizeBytes method should exist");
      assertTrue(method.isDefault(), "getMinimumSizeBytes should be a default method");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have canAccommodatePages default method")
    void shouldHaveCanAccommodatePagesDefaultMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("canAccommodatePages", long.class);
      assertNotNull(method, "canAccommodatePages method should exist");
      assertTrue(method.isDefault(), "canAccommodatePages should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have canAccommodateSize default method")
    void shouldHaveCanAccommodateSizeDefaultMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("canAccommodateSize", long.class);
      assertNotNull(method, "canAccommodateSize method should exist");
      assertTrue(method.isDefault(), "canAccommodateSize should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create(long, Long, boolean) static method")
    void shouldHaveCreate3ParamMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("create", long.class, Long.class, boolean.class);
      assertNotNull(method, "create(long, Long, boolean) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Memory64Type.class, method.getReturnType(), "Should return Memory64Type");
    }

    @Test
    @DisplayName("should have create(long, Long) static method")
    void shouldHaveCreate2ParamMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("create", long.class, Long.class);
      assertNotNull(method, "create(long, Long) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Memory64Type.class, method.getReturnType(), "Should return Memory64Type");
    }

    @Test
    @DisplayName("should have createUnlimited(long, boolean) static method")
    void shouldHaveCreateUnlimited2ParamMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("createUnlimited", long.class, boolean.class);
      assertNotNull(method, "createUnlimited(long, boolean) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Memory64Type.class, method.getReturnType(), "Should return Memory64Type");
    }

    @Test
    @DisplayName("should have createUnlimited(long) static method")
    void shouldHaveCreateUnlimited1ParamMethod() throws NoSuchMethodException {
      Method method = Memory64Type.class.getMethod("createUnlimited", long.class);
      assertNotNull(method, "createUnlimited(long) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Memory64Type.class, method.getReturnType(), "Should return Memory64Type");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected abstract methods")
    void shouldHaveAllExpectedAbstractMethods() {
      Set<String> expectedAbstractMethods = Set.of("getMinimum64", "getMaximum64");

      Set<String> actualAbstractMethods =
          Arrays.stream(Memory64Type.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedAbstractMethods) {
        assertTrue(actualAbstractMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 2 abstract methods declared")
    void shouldHaveExactly2AbstractMethods() {
      long abstractCount =
          Arrays.stream(Memory64Type.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(2, abstractCount, "Should have exactly 2 abstract methods");
    }

    @Test
    @DisplayName("should have expected default methods")
    void shouldHaveExpectedDefaultMethods() {
      Set<String> expectedDefaultMethods =
          Set.of(
              "getMinimum",
              "getMaximum",
              "is64Bit",
              "getPageSizeBytes",
              "getMaximumSizeBytes",
              "getMinimumSizeBytes",
              "canAccommodatePages",
              "canAccommodateSize");

      Set<String> actualDefaultMethods =
          Arrays.stream(Memory64Type.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedDefaultMethods) {
        assertTrue(
            actualDefaultMethods.contains(expected), "Should have default method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 4 static methods")
    void shouldHaveExactly4StaticMethods() {
      long staticCount =
          Arrays.stream(Memory64Type.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(4, staticCount, "Should have exactly 4 static factory methods");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          Memory64Type.class.getDeclaredClasses().length,
          "Memory64Type should have no nested classes");
    }
  }
}
