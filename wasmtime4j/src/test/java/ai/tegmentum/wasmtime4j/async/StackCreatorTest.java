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

package ai.tegmentum.wasmtime4j.async;

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
 * Comprehensive test suite for the StackCreator interface.
 *
 * <p>StackCreator provides custom async stack allocation for WebAssembly async execution.
 */
@DisplayName("StackCreator Interface Tests")
class StackCreatorTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(StackCreator.class.isInterface(), "StackCreator should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(StackCreator.class.getModifiers()), "StackCreator should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          StackCreator.class.getInterfaces().length,
          "StackCreator should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have createStack method")
    void shouldHaveCreateStackMethod() throws NoSuchMethodException {
      Method method = StackCreator.class.getMethod("createStack", long.class);
      assertNotNull(method, "createStack method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          StackCreator.AsyncStack.class, method.getReturnType(), "Should return AsyncStack");
    }

    @Test
    @DisplayName("createStack should declare WasmException")
    void createStackShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = StackCreator.class.getMethod("createStack", long.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(ex -> ex.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "createStack should declare WasmException");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have getDefaultStackSize default method")
    void shouldHaveGetDefaultStackSizeMethod() throws NoSuchMethodException {
      Method method = StackCreator.class.getMethod("getDefaultStackSize");
      assertNotNull(method, "getDefaultStackSize method should exist");
      assertTrue(method.isDefault(), "Should be default method");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxStackSize default method")
    void shouldHaveGetMaxStackSizeMethod() throws NoSuchMethodException {
      Method method = StackCreator.class.getMethod("getMaxStackSize");
      assertNotNull(method, "getMaxStackSize method should exist");
      assertTrue(method.isDefault(), "Should be default method");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMinStackSize default method")
    void shouldHaveGetMinStackSizeMethod() throws NoSuchMethodException {
      Method method = StackCreator.class.getMethod("getMinStackSize");
      assertNotNull(method, "getMinStackSize method should exist");
      assertTrue(method.isDefault(), "Should be default method");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have close default method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = StackCreator.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertTrue(method.isDefault(), "Should be default method");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static defaultCreator method")
    void shouldHaveStaticDefaultCreatorMethod() throws NoSuchMethodException {
      Method method = StackCreator.class.getMethod("defaultCreator");
      assertNotNull(method, "defaultCreator method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(StackCreator.class, method.getReturnType(), "Should return StackCreator");
    }
  }

  // ========================================================================
  // Nested AsyncStack Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested AsyncStack Interface Tests")
  class AsyncStackInterfaceTests {

    @Test
    @DisplayName("should have AsyncStack nested interface")
    void shouldHaveAsyncStackNestedInterface() {
      Class<?>[] nestedClasses = StackCreator.class.getDeclaredClasses();
      boolean hasAsyncStack =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("AsyncStack") && c.isInterface());
      assertTrue(hasAsyncStack, "Should have AsyncStack nested interface");
    }

    @Test
    @DisplayName("AsyncStack should be public")
    void asyncStackShouldBePublic() {
      assertTrue(
          Modifier.isPublic(StackCreator.AsyncStack.class.getModifiers()),
          "AsyncStack should be public");
    }

    @Test
    @DisplayName("AsyncStack should have getBaseAddress method")
    void asyncStackShouldHaveGetBaseAddressMethod() throws NoSuchMethodException {
      Method method = StackCreator.AsyncStack.class.getMethod("getBaseAddress");
      assertNotNull(method, "getBaseAddress method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("AsyncStack should have getTopAddress method")
    void asyncStackShouldHaveGetTopAddressMethod() throws NoSuchMethodException {
      Method method = StackCreator.AsyncStack.class.getMethod("getTopAddress");
      assertNotNull(method, "getTopAddress method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("AsyncStack should have getSize method")
    void asyncStackShouldHaveGetSizeMethod() throws NoSuchMethodException {
      Method method = StackCreator.AsyncStack.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("AsyncStack should have getGuardSize default method")
    void asyncStackShouldHaveGetGuardSizeMethod() throws NoSuchMethodException {
      Method method = StackCreator.AsyncStack.class.getMethod("getGuardSize");
      assertNotNull(method, "getGuardSize method should exist");
      assertTrue(method.isDefault(), "Should be default method");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("AsyncStack should have isValid method")
    void asyncStackShouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = StackCreator.AsyncStack.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("AsyncStack should have release method")
    void asyncStackShouldHaveReleaseMethod() throws NoSuchMethodException {
      Method method = StackCreator.AsyncStack.class.getMethod("release");
      assertNotNull(method, "release method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("AsyncStack should have getId default method")
    void asyncStackShouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = StackCreator.AsyncStack.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertTrue(method.isDefault(), "Should be default method");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  // ========================================================================
  // Nested DefaultStackCreator Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested DefaultStackCreator Class Tests")
  class DefaultStackCreatorClassTests {

    @Test
    @DisplayName("should have DefaultStackCreator nested class")
    void shouldHaveDefaultStackCreatorNestedClass() {
      Class<?>[] nestedClasses = StackCreator.class.getDeclaredClasses();
      boolean hasDefaultStackCreator =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("DefaultStackCreator"));
      assertTrue(hasDefaultStackCreator, "Should have DefaultStackCreator nested class");
    }

    @Test
    @DisplayName("DefaultStackCreator should be final")
    void defaultStackCreatorShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(StackCreator.DefaultStackCreator.class.getModifiers()),
          "DefaultStackCreator should be final");
    }

    @Test
    @DisplayName("DefaultStackCreator should implement StackCreator")
    void defaultStackCreatorShouldImplementStackCreator() {
      assertTrue(
          StackCreator.class.isAssignableFrom(StackCreator.DefaultStackCreator.class),
          "DefaultStackCreator should implement StackCreator");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods in StackCreator")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "createStack",
              "getDefaultStackSize",
              "getMaxStackSize",
              "getMinStackSize",
              "close",
              "defaultCreator");

      Set<String> actualMethods =
          Arrays.stream(StackCreator.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 1 abstract method")
    void shouldHaveExactly1AbstractMethod() {
      long abstractCount =
          Arrays.stream(StackCreator.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(1, abstractCount, "Should have exactly 1 abstract method (createStack)");
    }

    @Test
    @DisplayName("should have exactly 4 default methods")
    void shouldHaveExactly4DefaultMethods() {
      long defaultCount =
          Arrays.stream(StackCreator.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(
          4,
          defaultCount,
          "Should have exactly 4 default methods"
              + " (getDefaultStackSize, getMaxStackSize, getMinStackSize, close)");
    }

    @Test
    @DisplayName("should have exactly 1 static method")
    void shouldHaveExactly1StaticMethod() {
      long staticCount =
          Arrays.stream(StackCreator.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(1, staticCount, "Should have exactly 1 static method (defaultCreator)");
    }
  }

  // ========================================================================
  // AsyncStack Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("AsyncStack Method Count Tests")
  class AsyncStackMethodCountTests {

    @Test
    @DisplayName("should have all expected methods in AsyncStack")
    void shouldHaveAllExpectedAsyncStackMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getBaseAddress",
              "getTopAddress",
              "getSize",
              "getGuardSize",
              "isValid",
              "release",
              "getId");

      Set<String> actualMethods =
          Arrays.stream(StackCreator.AsyncStack.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 5 abstract methods in AsyncStack")
    void shouldHaveExactly5AbstractMethodsInAsyncStack() {
      long abstractCount =
          Arrays.stream(StackCreator.AsyncStack.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(5, abstractCount, "Should have exactly 5 abstract methods in AsyncStack");
    }

    @Test
    @DisplayName("should have exactly 2 default methods in AsyncStack")
    void shouldHaveExactly2DefaultMethodsInAsyncStack() {
      long defaultCount =
          Arrays.stream(StackCreator.AsyncStack.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(2, defaultCount, "Should have exactly 2 default methods in AsyncStack");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have expected nested types")
    void shouldHaveExpectedNestedTypes() {
      Set<String> nestedClassNames =
          Arrays.stream(StackCreator.class.getDeclaredClasses())
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      assertTrue(nestedClassNames.contains("AsyncStack"), "Should have AsyncStack");
      assertTrue(
          nestedClassNames.contains("DefaultStackCreator"), "Should have DefaultStackCreator");
    }

    @Test
    @DisplayName("should have exactly 2 nested types")
    void shouldHaveExactly2NestedTypes() {
      assertEquals(
          2,
          StackCreator.class.getDeclaredClasses().length,
          "Should have 2 nested types (AsyncStack, DefaultStackCreator)");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          StackCreator.class.getDeclaredFields().length,
          "StackCreator should have no declared fields");
    }
  }
}
