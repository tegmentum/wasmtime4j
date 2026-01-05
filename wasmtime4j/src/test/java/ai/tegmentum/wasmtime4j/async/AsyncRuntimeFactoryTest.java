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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AsyncRuntimeFactory} class.
 *
 * <p>AsyncRuntimeFactory provides factory methods for creating AsyncRuntime instances using
 * ServiceLoader discovery.
 */
@DisplayName("AsyncRuntimeFactory Tests")
class AsyncRuntimeFactoryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(AsyncRuntimeFactory.class.getModifiers()),
          "AsyncRuntimeFactory should be public");
      assertTrue(
          Modifier.isFinal(AsyncRuntimeFactory.class.getModifiers()),
          "AsyncRuntimeFactory should be final");
      assertFalse(
          AsyncRuntimeFactory.class.isInterface(),
          "AsyncRuntimeFactory should not be an interface");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?>[] constructors = AsyncRuntimeFactory.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly one constructor");

      final Constructor<?> constructor = constructors[0];
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("AsyncRuntimeProvider Interface Tests")
  class AsyncRuntimeProviderInterfaceTests {

    @Test
    @DisplayName("should have AsyncRuntimeProvider nested interface")
    void shouldHaveAsyncRuntimeProviderNestedInterface() {
      final var nestedClasses = AsyncRuntimeFactory.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("AsyncRuntimeProvider")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "AsyncRuntimeProvider should be an interface");
          assertTrue(
              Modifier.isPublic(nestedClass.getModifiers()),
              "AsyncRuntimeProvider should be public");
          break;
        }
      }
      assertTrue(found, "Should have AsyncRuntimeProvider nested interface");
    }

    @Test
    @DisplayName("AsyncRuntimeProvider should have create method")
    void asyncRuntimeProviderShouldHaveCreateMethod() throws NoSuchMethodException {
      final Class<?> providerClass = AsyncRuntimeFactory.AsyncRuntimeProvider.class;
      final Method method = providerClass.getMethod("create");

      assertNotNull(method, "create method should exist");
      assertEquals(AsyncRuntime.class, method.getReturnType(), "Should return AsyncRuntime");

      // Check it throws WasmException
      final Class<?>[] exceptions = method.getExceptionTypes();
      boolean throwsWasmException = false;
      for (Class<?> ex : exceptions) {
        if (ex.equals(WasmException.class)) {
          throwsWasmException = true;
          break;
        }
      }
      assertTrue(throwsWasmException, "create should throw WasmException");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create static method")
    void shouldHaveCreateStaticMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntimeFactory.class.getMethod("create");
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(AsyncRuntime.class, method.getReturnType(), "Should return AsyncRuntime");

      // Check it throws WasmException
      final Class<?>[] exceptions = method.getExceptionTypes();
      boolean throwsWasmException = false;
      for (Class<?> ex : exceptions) {
        if (ex.equals(WasmException.class)) {
          throwsWasmException = true;
          break;
        }
      }
      assertTrue(throwsWasmException, "create should throw WasmException");
    }

    @Test
    @DisplayName("should have getSharedInstance static method")
    void shouldHaveGetSharedInstanceStaticMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntimeFactory.class.getMethod("getSharedInstance");
      assertNotNull(method, "getSharedInstance method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getSharedInstance should be static");
      assertEquals(AsyncRuntime.class, method.getReturnType(), "Should return AsyncRuntime");

      // Check it throws WasmException
      final Class<?>[] exceptions = method.getExceptionTypes();
      boolean throwsWasmException = false;
      for (Class<?> ex : exceptions) {
        if (ex.equals(WasmException.class)) {
          throwsWasmException = true;
          break;
        }
      }
      assertTrue(throwsWasmException, "getSharedInstance should throw WasmException");
    }
  }

  @Nested
  @DisplayName("SharedRuntimeHolder Tests")
  class SharedRuntimeHolderTests {

    @Test
    @DisplayName("should have SharedRuntimeHolder nested class")
    void shouldHaveSharedRuntimeHolderNestedClass() {
      final var nestedClasses = AsyncRuntimeFactory.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("SharedRuntimeHolder")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "SharedRuntimeHolder should be a class");
          assertTrue(
              Modifier.isPrivate(nestedClass.getModifiers()),
              "SharedRuntimeHolder should be private");
          assertTrue(
              Modifier.isFinal(nestedClass.getModifiers()), "SharedRuntimeHolder should be final");
          assertTrue(
              Modifier.isStatic(nestedClass.getModifiers()),
              "SharedRuntimeHolder should be static");
          break;
        }
      }
      assertTrue(found, "Should have SharedRuntimeHolder nested class");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly 2 public static methods")
    void shouldHaveExactlyTwoPublicStaticMethods() {
      final Method[] methods = AsyncRuntimeFactory.class.getDeclaredMethods();
      int publicStaticCount = 0;
      for (Method method : methods) {
        if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
          publicStaticCount++;
        }
      }
      assertEquals(2, publicStaticCount, "Should have exactly 2 public static methods");
    }
  }
}
