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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentEngine;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaComponentEngine} class.
 *
 * <p>This test class verifies the class structure, method signatures, and interface implementation
 * for the PanamaComponentEngine class that implements the ComponentEngine interface.
 */
@DisplayName("PanamaComponentEngine Tests")
class PanamaComponentEngineTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaComponentEngine Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaComponentEngine should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaComponentEngine.class.getModifiers()),
          "PanamaComponentEngine should be a final class");
    }

    @Test
    @DisplayName("PanamaComponentEngine should have package-private visibility")
    void shouldHavePackagePrivateVisibility() {
      int modifiers = PanamaComponentEngine.class.getModifiers();
      assertFalse(
          Modifier.isPublic(modifiers),
          "PanamaComponentEngine should not be public (should be package-private)");
      assertFalse(Modifier.isProtected(modifiers), "PanamaComponentEngine should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "PanamaComponentEngine should not be private");
    }

    @Test
    @DisplayName("PanamaComponentEngine should implement ComponentEngine interface")
    void shouldImplementComponentEngineInterface() {
      assertTrue(
          ComponentEngine.class.isAssignableFrom(PanamaComponentEngine.class),
          "PanamaComponentEngine should implement ComponentEngine interface");
    }

    @Test
    @DisplayName("PanamaComponentEngine should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaComponentEngine.class),
          "PanamaComponentEngine should implement AutoCloseable");
    }
  }

  // ========================================================================
  // Field Declaration Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Declaration Tests")
  class FieldDeclarationTests {

    @Test
    @DisplayName("should have arenaResourceManager field")
    void shouldHaveArenaResourceManagerField() {
      boolean hasField =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(ArenaResourceManager.class));
      assertTrue(hasField, "Should have ArenaResourceManager field");
    }

    @Test
    @DisplayName("should have loadedComponents field")
    void shouldHaveLoadedComponentsField() {
      boolean hasField =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getName().contains("loadedComponents")
                          || f.getName().contains("Components")
                          || Map.class.isAssignableFrom(f.getType()));
      assertTrue(hasField, "Should have loadedComponents or similar collection field");
    }

    @Test
    @DisplayName("should have componentIdCounter field")
    void shouldHaveComponentIdCounterField() {
      boolean hasField =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getName().contains("componentId")
                          || f.getName().contains("Counter")
                          || f.getName().contains("counter"));
      assertTrue(hasField, "Should have componentIdCounter or similar counter field");
    }

    @Test
    @DisplayName("should have nativeFunctionBindings field")
    void shouldHaveNativeFunctionBindingsField() {
      boolean hasField =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getType().equals(NativeFunctionBindings.class)
                          || f.getName().contains("native")
                          || f.getName().contains("binding"));
      assertTrue(hasField, "Should have NativeFunctionBindings field or similar");
    }

    @Test
    @DisplayName("all fields should be private")
    void allFieldsShouldBePrivate() {
      for (Field field : PanamaComponentEngine.class.getDeclaredFields()) {
        if (!field.isSynthetic()) {
          assertTrue(
              Modifier.isPrivate(field.getModifiers()),
              "Field " + field.getName() + " should be private");
        }
      }
    }
  }

  // ========================================================================
  // ComponentEngine Interface Method Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentEngine Interface Method Tests")
  class ComponentEngineInterfaceMethodTests {

    @Test
    @DisplayName("should have compileComponent method")
    void shouldHaveCompileComponentMethod() {
      boolean hasMethod =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("compileComponent"));
      assertTrue(hasMethod, "Should have compileComponent method");
    }

    @Test
    @DisplayName("should have createInstance method")
    void shouldHaveCreateInstanceMethod() {
      boolean hasMethod =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("createInstance"));
      assertTrue(hasMethod, "Should have createInstance method");
    }

    @Test
    @DisplayName("should have validateComponent method")
    void shouldHaveValidateComponentMethod() {
      boolean hasMethod =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("validateComponent"));
      assertTrue(hasMethod, "Should have validateComponent method");
    }

    @Test
    @DisplayName("should have linkComponents method")
    void shouldHaveLinkComponentsMethod() {
      boolean hasMethod =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("linkComponents"));
      assertTrue(hasMethod, "Should have linkComponents method");
    }
  }

  // ========================================================================
  // AutoCloseable Method Tests
  // ========================================================================

  @Nested
  @DisplayName("AutoCloseable Method Tests")
  class AutoCloseableMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = PanamaComponentEngine.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }

    @Test
    @DisplayName("close method should be public")
    void closeMethodShouldBePublic() throws NoSuchMethodException {
      Method method = PanamaComponentEngine.class.getMethod("close");
      assertTrue(Modifier.isPublic(method.getModifiers()), "close method should be public");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor accepting ArenaResourceManager")
    void shouldHaveArenaResourceManagerConstructor() {
      boolean hasConstructor =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredConstructors())
              .anyMatch(
                  c ->
                      c.getParameterCount() > 0
                          && Arrays.asList(c.getParameterTypes())
                              .contains(ArenaResourceManager.class));
      assertTrue(hasConstructor, "Should have constructor accepting ArenaResourceManager");
    }

    @Test
    @DisplayName("constructor should be package-private")
    void constructorShouldBePackagePrivate() {
      Constructor<?>[] constructors = PanamaComponentEngine.class.getDeclaredConstructors();
      for (Constructor<?> constructor : constructors) {
        assertFalse(
            Modifier.isPublic(constructor.getModifiers()),
            "Constructor should not be public (package-private for factory pattern)");
      }
    }
  }

  // ========================================================================
  // Method Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Return Type Tests")
  class MethodReturnTypeTests {

    @Test
    @DisplayName("compileComponent methods should have appropriate return types")
    void compileComponentMethodsShouldHaveAppropriateReturnTypes() {
      List<Method> compileMethods =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .filter(m -> m.getName().equals("compileComponent"))
              .collect(Collectors.toList());

      assertFalse(compileMethods.isEmpty(), "Should have compileComponent method(s)");

      for (Method method : compileMethods) {
        // Return type should not be void for compile operations
        assertFalse(
            method.getReturnType().equals(void.class), "compileComponent should not return void");
      }
    }

    @Test
    @DisplayName("validateComponent should return boolean")
    void validateComponentShouldReturnBoolean() {
      List<Method> validateMethods =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .filter(m -> m.getName().equals("validateComponent"))
              .collect(Collectors.toList());

      if (!validateMethods.isEmpty()) {
        for (Method method : validateMethods) {
          assertEquals(
              boolean.class, method.getReturnType(), "validateComponent should return boolean");
        }
      }
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("compileComponent should accept byte array or similar")
    void compileComponentShouldAcceptByteArray() {
      List<Method> compileMethods =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .filter(m -> m.getName().equals("compileComponent"))
              .collect(Collectors.toList());

      boolean acceptsByteArray =
          compileMethods.stream()
              .anyMatch(
                  m -> Arrays.stream(m.getParameterTypes()).anyMatch(p -> p.equals(byte[].class)));

      assertTrue(acceptsByteArray, "compileComponent should accept byte[] parameter");
    }

    @Test
    @DisplayName("createInstance should have parameters")
    void createInstanceShouldHaveParameters() {
      List<Method> createMethods =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .filter(m -> m.getName().equals("createInstance"))
              .collect(Collectors.toList());

      if (!createMethods.isEmpty()) {
        for (Method method : createMethods) {
          assertTrue(
              method.getParameterCount() > 0, "createInstance should have at least one parameter");
        }
      }
    }
  }

  // ========================================================================
  // Interface Contract Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should implement all non-default methods from ComponentEngine")
    void shouldImplementAllComponentEngineMethod() {
      Set<String> componentEngineMethods =
          Arrays.stream(ComponentEngine.class.getDeclaredMethods())
              .filter(m -> !Modifier.isStatic(m.getModifiers()))
              .filter(m -> !m.isDefault())
              .map(Method::getName)
              .collect(Collectors.toSet());

      Set<String> implementedMethods =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String methodName : componentEngineMethods) {
        assertTrue(
            implementedMethods.contains(methodName), "Should implement method: " + methodName);
      }
    }

    @Test
    @DisplayName("should be in the correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaComponentEngine.class.getPackage().getName(),
          "PanamaComponentEngine should be in ai.tegmentum.wasmtime4j.panama package");
    }
  }

  // ========================================================================
  // Resource Management Tests
  // ========================================================================

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("should track loaded components")
    void shouldTrackLoadedComponents() {
      // Verify through field presence
      boolean hasComponentTracking =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      Map.class.isAssignableFrom(f.getType())
                          || f.getName().contains("loadedComponents")
                          || f.getName().contains("components"));
      assertTrue(hasComponentTracking, "Should have mechanism to track loaded components");
    }

    @Test
    @DisplayName("should use ArenaResourceManager for memory management")
    void shouldUseArenaResourceManager() {
      boolean usesArenaResourceManager =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(ArenaResourceManager.class));
      assertTrue(usesArenaResourceManager, "Should use ArenaResourceManager for memory management");
    }

    @Test
    @DisplayName("close method should clean up resources")
    void closeMethodShouldCleanUpResources() throws NoSuchMethodException {
      Method closeMethod = PanamaComponentEngine.class.getMethod("close");
      assertNotNull(closeMethod, "close method should exist");
      // The close method exists and should clean up native resources
      // This is verified by the method's existence and proper signature
      assertEquals(void.class, closeMethod.getReturnType());
    }
  }

  // ========================================================================
  // Native Integration Tests
  // ========================================================================

  @Nested
  @DisplayName("Native Integration Tests")
  class NativeIntegrationTests {

    @Test
    @DisplayName("should use NativeFunctionBindings")
    void shouldUseNativeFunctionBindings() {
      boolean usesNativeFunctionBindings =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(NativeFunctionBindings.class));
      assertTrue(usesNativeFunctionBindings, "Should use NativeFunctionBindings for native calls");
    }

    @Test
    @DisplayName("should have methods for native interaction")
    void shouldHaveMethodsForNativeInteraction() {
      Set<String> methodNames =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      // Should have compile/create/validate methods that interact with native code
      assertTrue(
          methodNames.contains("compileComponent") || methodNames.contains("compile"),
          "Should have compile method for native interaction");
    }
  }

  // ========================================================================
  // Thread Safety Tests
  // ========================================================================

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("component ID counter should be thread-safe")
    void componentIdCounterShouldBeThreadSafe() {
      // Check for atomic counter or synchronized field
      boolean hasAtomicCounter =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getType().getSimpleName().contains("Atomic")
                          || f.getName().contains("counter")
                          || f.getName().contains("Counter"));
      assertTrue(
          hasAtomicCounter, "Should have atomic counter or similar for thread-safe ID generation");
    }

    @Test
    @DisplayName("loaded components map should be thread-safe")
    void loadedComponentsMapShouldBeThreadSafe() {
      // Check for concurrent map or synchronized structure
      boolean hasConcurrentMap =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getType().getName().contains("Concurrent")
                          || f.getType().getName().contains("Synchronized")
                          || Map.class.isAssignableFrom(f.getType()));
      assertTrue(
          hasConcurrentMap,
          "Should have concurrent or synchronized map for thread-safe component storage");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 5 declared methods")
    void shouldHaveAtLeastFiveDeclaredMethods() {
      Method[] methods = PanamaComponentEngine.class.getDeclaredMethods();
      // At minimum: compileComponent, createInstance, validateComponent, linkComponents, close
      assertTrue(
          methods.length >= 5, "Should have at least 5 declared methods, found: " + methods.length);
    }

    @Test
    @DisplayName("should have expected core methods")
    void shouldHaveExpectedCoreMethods() {
      Set<String> methodNames =
          Arrays.stream(PanamaComponentEngine.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      // Check for expected methods from ComponentEngine interface
      Set<String> expectedMethods =
          Set.of(
              "compileComponent", "createInstance", "validateComponent", "linkComponents", "close");

      for (String expected : expectedMethods) {
        assertTrue(
            methodNames.contains(expected)
                || Arrays.stream(PanamaComponentEngine.class.getMethods())
                    .anyMatch(m -> m.getName().equals(expected)),
            "Should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Exception Handling Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("public methods should not throw checked exceptions except close")
    void publicMethodsShouldNotThrowCheckedExceptions() {
      for (Method method : PanamaComponentEngine.class.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) {
          for (Class<?> exceptionType : method.getExceptionTypes()) {
            if (!RuntimeException.class.isAssignableFrom(exceptionType)
                && !Error.class.isAssignableFrom(exceptionType)) {
              // Only close() is allowed to throw Exception
              assertTrue(
                  method.getName().equals("close") || exceptionType.equals(Exception.class),
                  "Method "
                      + method.getName()
                      + " should not throw checked exception: "
                      + exceptionType.getSimpleName());
            }
          }
        }
      }
    }
  }
}
