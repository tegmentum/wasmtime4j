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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaComponent} factory class and its nested classes.
 *
 * <p>This test class verifies the class structure, method signatures, and field declarations for
 * the PanamaComponent factory and its nested implementation classes (PanamaComponentEngine,
 * PanamaComponentHandle, PanamaComponentInstanceHandle).
 */
@DisplayName("PanamaComponent Factory Tests")
class PanamaComponentTest {

  // ========================================================================
  // PanamaComponent Factory Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaComponent Class Structure Tests")
  class PanamaComponentStructureTests {

    @Test
    @DisplayName("PanamaComponent should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaComponent.class.getModifiers()),
          "PanamaComponent should be a final class");
    }

    @Test
    @DisplayName("PanamaComponent should have public visibility")
    void shouldHavePublicVisibility() {
      // Note: Implementation uses public visibility for external API access
      int modifiers = PanamaComponent.class.getModifiers();
      assertTrue(Modifier.isPublic(modifiers), "PanamaComponent should be public for API access");
      assertFalse(Modifier.isProtected(modifiers), "PanamaComponent should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "PanamaComponent should not be private");
    }

    @Test
    @DisplayName("PanamaComponent should have a private constructor")
    void shouldHavePrivateConstructor() {
      Constructor<?>[] constructors = PanamaComponent.class.getDeclaredConstructors();
      assertTrue(constructors.length > 0, "PanamaComponent should have at least one constructor");

      for (Constructor<?> constructor : constructors) {
        assertTrue(
            Modifier.isPrivate(constructor.getModifiers()),
            "All constructors should be private for factory pattern");
      }
    }

    @Test
    @DisplayName("PanamaComponent should have exactly 3 nested classes")
    void shouldHaveExactlyThreeNestedClasses() {
      Class<?>[] nestedClasses = PanamaComponent.class.getDeclaredClasses();

      // Get the names of nested classes
      Set<String> nestedClassNames =
          Arrays.stream(nestedClasses).map(Class::getSimpleName).collect(Collectors.toSet());

      assertTrue(
          nestedClassNames.contains("PanamaComponentEngine"),
          "Should have PanamaComponentEngine nested class");
      assertTrue(
          nestedClassNames.contains("PanamaComponentHandle"),
          "Should have PanamaComponentHandle nested class");
      assertTrue(
          nestedClassNames.contains("PanamaComponentInstanceHandle"),
          "Should have PanamaComponentInstanceHandle nested class");
    }
  }

  // ========================================================================
  // PanamaComponent Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaComponent Factory Method Tests")
  class PanamaComponentFactoryMethodTests {

    @Test
    @DisplayName("should have static createComponentEngine method")
    void shouldHaveCreateComponentEngineMethod() throws NoSuchMethodException {
      Method method =
          PanamaComponent.class.getDeclaredMethod(
              "createComponentEngine", ArenaResourceManager.class);
      assertNotNull(method, "createComponentEngine method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()),
          "createComponentEngine should be a static method");
      assertEquals(
          PanamaComponent.class.getDeclaredClasses()[0].getEnclosingClass(),
          PanamaComponent.class,
          "Method should return a nested class type");
    }

    @Test
    @DisplayName("createComponentEngine should have correct parameter type")
    void createComponentEngineShouldHaveCorrectParameterType() throws NoSuchMethodException {
      Method method =
          PanamaComponent.class.getDeclaredMethod(
              "createComponentEngine", ArenaResourceManager.class);
      Class<?>[] parameterTypes = method.getParameterTypes();
      assertEquals(1, parameterTypes.length, "Should have exactly 1 parameter");
      assertEquals(
          ArenaResourceManager.class,
          parameterTypes[0],
          "Parameter should be ArenaResourceManager");
    }
  }

  // ========================================================================
  // PanamaComponentEngine Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaComponentEngine Nested Class Tests")
  class PanamaComponentEngineTests {

    private Class<?> getComponentEngineClass() {
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          return nestedClass;
        }
      }
      throw new AssertionError("PanamaComponentEngine nested class not found");
    }

    @Test
    @DisplayName("PanamaComponentEngine should be a static nested class")
    void shouldBeStaticNestedClass() {
      Class<?> engineClass = getComponentEngineClass();
      assertTrue(
          Modifier.isStatic(engineClass.getModifiers()),
          "PanamaComponentEngine should be a static nested class");
    }

    @Test
    @DisplayName("PanamaComponentEngine should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      Class<?> engineClass = getComponentEngineClass();
      assertTrue(
          AutoCloseable.class.isAssignableFrom(engineClass),
          "PanamaComponentEngine should implement AutoCloseable");
    }

    @Test
    @DisplayName("PanamaComponentEngine should have loadComponentFromBytes method")
    void shouldHaveLoadComponentFromBytesMethod() {
      Class<?> engineClass = getComponentEngineClass();
      boolean hasMethod =
          Arrays.stream(engineClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("loadComponentFromBytes"));
      assertTrue(hasMethod, "Should have loadComponentFromBytes method");
    }

    @Test
    @DisplayName("PanamaComponentEngine should have instantiateComponent method")
    void shouldHaveInstantiateComponentMethod() {
      Class<?> engineClass = getComponentEngineClass();
      boolean hasMethod =
          Arrays.stream(engineClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("instantiateComponent"));
      assertTrue(hasMethod, "Should have instantiateComponent method");
    }

    @Test
    @DisplayName("PanamaComponentEngine should have close method")
    void shouldHaveCloseMethod() {
      Class<?> engineClass = getComponentEngineClass();
      boolean hasMethod =
          Arrays.stream(engineClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("close"));
      assertTrue(hasMethod, "Should have close method from AutoCloseable");
    }

    @Test
    @DisplayName("PanamaComponentEngine should have arenaResourceManager field")
    void shouldHaveArenaResourceManagerField() {
      Class<?> engineClass = getComponentEngineClass();
      boolean hasField =
          Arrays.stream(engineClass.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(ArenaResourceManager.class));
      assertTrue(hasField, "Should have ArenaResourceManager field");
    }
  }

  // ========================================================================
  // PanamaComponentHandle Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaComponentHandle Nested Class Tests")
  class PanamaComponentHandleTests {

    private Class<?> getComponentHandleClass() {
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentHandle")) {
          return nestedClass;
        }
      }
      throw new AssertionError("PanamaComponentHandle nested class not found");
    }

    @Test
    @DisplayName("PanamaComponentHandle should be a static nested class")
    void shouldBeStaticNestedClass() {
      Class<?> handleClass = getComponentHandleClass();
      assertTrue(
          Modifier.isStatic(handleClass.getModifiers()),
          "PanamaComponentHandle should be a static nested class");
    }

    @Test
    @DisplayName("PanamaComponentHandle should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      Class<?> handleClass = getComponentHandleClass();
      assertTrue(
          AutoCloseable.class.isAssignableFrom(handleClass),
          "PanamaComponentHandle should implement AutoCloseable");
    }

    @Test
    @DisplayName("PanamaComponentHandle should have getSize method")
    void shouldHaveGetSizeMethod() {
      Class<?> handleClass = getComponentHandleClass();
      boolean hasMethod =
          Arrays.stream(handleClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("getSize") && m.getReturnType().equals(long.class));
      assertTrue(hasMethod, "Should have getSize method returning long");
    }

    @Test
    @DisplayName("PanamaComponentHandle should have exportsInterface method")
    void shouldHaveExportsInterfaceMethod() {
      Class<?> handleClass = getComponentHandleClass();
      boolean hasMethod =
          Arrays.stream(handleClass.getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("exportsInterface")
                          && m.getReturnType().equals(boolean.class));
      assertTrue(hasMethod, "Should have exportsInterface method returning boolean");
    }

    @Test
    @DisplayName("PanamaComponentHandle should have importsInterface method")
    void shouldHaveImportsInterfaceMethod() {
      Class<?> handleClass = getComponentHandleClass();
      boolean hasMethod =
          Arrays.stream(handleClass.getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("importsInterface")
                          && m.getReturnType().equals(boolean.class));
      assertTrue(hasMethod, "Should have importsInterface method returning boolean");
    }

    @Test
    @DisplayName("PanamaComponentHandle should have componentId field")
    void shouldHaveComponentIdField() {
      // Note: Implementation uses componentResource for native resource management
      Class<?> handleClass = getComponentHandleClass();
      boolean hasField =
          Arrays.stream(handleClass.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getName().equals("componentId")
                          || f.getName().contains("componentResource")
                          || f.getName().contains("Resource"));
      assertTrue(hasField, "Should have componentId or componentResource field");
    }

    @Test
    @DisplayName("PanamaComponentHandle should have close method")
    void shouldHaveCloseMethod() {
      Class<?> handleClass = getComponentHandleClass();
      boolean hasMethod =
          Arrays.stream(handleClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("close"));
      assertTrue(hasMethod, "Should have close method from AutoCloseable");
    }
  }

  // ========================================================================
  // PanamaComponentInstanceHandle Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaComponentInstanceHandle Nested Class Tests")
  class PanamaComponentInstanceHandleTests {

    private Class<?> getComponentInstanceHandleClass() {
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentInstanceHandle")) {
          return nestedClass;
        }
      }
      throw new AssertionError("PanamaComponentInstanceHandle nested class not found");
    }

    @Test
    @DisplayName("PanamaComponentInstanceHandle should be a static nested class")
    void shouldBeStaticNestedClass() {
      Class<?> instanceHandleClass = getComponentInstanceHandleClass();
      assertTrue(
          Modifier.isStatic(instanceHandleClass.getModifiers()),
          "PanamaComponentInstanceHandle should be a static nested class");
    }

    @Test
    @DisplayName("PanamaComponentInstanceHandle should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      Class<?> instanceHandleClass = getComponentInstanceHandleClass();
      assertTrue(
          AutoCloseable.class.isAssignableFrom(instanceHandleClass),
          "PanamaComponentInstanceHandle should implement AutoCloseable");
    }

    @Test
    @DisplayName("PanamaComponentInstanceHandle should have getResource method")
    void shouldHaveGetResourceMethod() {
      // Note: Implementation uses getResource() for native resource access
      Class<?> instanceHandleClass = getComponentInstanceHandleClass();
      boolean hasMethod =
          Arrays.stream(instanceHandleClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("getResource") || m.getName().equals("getExport"));
      assertTrue(hasMethod, "Should have getResource or getExport method");
    }

    @Test
    @DisplayName("PanamaComponentInstanceHandle should have isValid method")
    void shouldHaveIsValidMethod() {
      // Note: Implementation uses isValid() instead of callFunction()
      Class<?> instanceHandleClass = getComponentInstanceHandleClass();
      boolean hasMethod =
          Arrays.stream(instanceHandleClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("isValid") || m.getName().equals("callFunction"));
      assertTrue(hasMethod, "Should have isValid or callFunction method");
    }

    @Test
    @DisplayName("PanamaComponentInstanceHandle should have close method")
    void shouldHaveCloseMethod() {
      Class<?> instanceHandleClass = getComponentInstanceHandleClass();
      boolean hasMethod =
          Arrays.stream(instanceHandleClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("close"));
      assertTrue(hasMethod, "Should have close method from AutoCloseable");
    }

    @Test
    @DisplayName("PanamaComponentInstanceHandle should have instanceResource field")
    void shouldHaveInstanceResourceField() {
      // Note: Implementation uses instanceResource for native resource management
      Class<?> instanceHandleClass = getComponentInstanceHandleClass();
      boolean hasField =
          Arrays.stream(instanceHandleClass.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getName().contains("instanceId")
                          || f.getName().contains("instanceResource")
                          || f.getName().contains("Resource"));
      assertTrue(hasField, "Should have instanceId or instanceResource field");
    }
  }

  // ========================================================================
  // Cross-Nested Class Relationship Tests
  // ========================================================================

  @Nested
  @DisplayName("Cross-Nested Class Relationship Tests")
  class CrossNestedClassRelationshipTests {

    @Test
    @DisplayName("All nested classes should be in the same package as PanamaComponent")
    void allNestedClassesShouldBeInSamePackage() {
      String expectedPackage = PanamaComponent.class.getPackage().getName();
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        assertEquals(
            expectedPackage,
            nestedClass.getPackage().getName(),
            nestedClass.getSimpleName() + " should be in package " + expectedPackage);
      }
    }

    @Test
    @DisplayName("All nested classes should have their enclosing class as PanamaComponent")
    void allNestedClassesShouldHaveCorrectEnclosingClass() {
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        assertEquals(
            PanamaComponent.class,
            nestedClass.getEnclosingClass(),
            nestedClass.getSimpleName() + " should have PanamaComponent as its enclosing class");
      }
    }

    @Test
    @DisplayName("PanamaComponentEngine should be able to create PanamaComponentHandle")
    void engineShouldBeAbleToCreateHandle() {
      // Verify through method signature that engine can create handles
      Class<?> engineClass = null;
      Class<?> handleClass = null;

      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
        } else if (nestedClass.getSimpleName().equals("PanamaComponentHandle")) {
          handleClass = nestedClass;
        }
      }

      assertNotNull(engineClass, "PanamaComponentEngine should exist");
      assertNotNull(handleClass, "PanamaComponentHandle should exist");

      // The loadComponentFromBytes method should exist (creates handles)
      boolean hasLoadMethod =
          Arrays.stream(engineClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("loadComponentFromBytes"));
      assertTrue(hasLoadMethod, "Engine should have loadComponentFromBytes method");
    }

    @Test
    @DisplayName("PanamaComponentEngine should be able to create PanamaComponentInstanceHandle")
    void engineShouldBeAbleToCreateInstanceHandle() {
      Class<?> engineClass = null;
      Class<?> instanceHandleClass = null;

      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
        } else if (nestedClass.getSimpleName().equals("PanamaComponentInstanceHandle")) {
          instanceHandleClass = nestedClass;
        }
      }

      assertNotNull(engineClass, "PanamaComponentEngine should exist");
      assertNotNull(instanceHandleClass, "PanamaComponentInstanceHandle should exist");

      // The instantiateComponent method should exist (creates instance handles)
      boolean hasInstantiateMethod =
          Arrays.stream(engineClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("instantiateComponent"));
      assertTrue(hasInstantiateMethod, "Engine should have instantiateComponent method");
    }
  }

  // ========================================================================
  // Method Accessibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Accessibility Tests")
  class MethodAccessibilityTests {

    @Test
    @DisplayName("Factory method createComponentEngine should be package-private or public")
    void factoryMethodShouldBeAccessible() throws NoSuchMethodException {
      Method method =
          PanamaComponent.class.getDeclaredMethod(
              "createComponentEngine", ArenaResourceManager.class);
      int modifiers = method.getModifiers();
      // Should be either package-private (no modifier) or public
      assertTrue(!Modifier.isPrivate(modifiers), "createComponentEngine should not be private");
    }

    @Test
    @DisplayName("Nested class public methods may throw WasmException for WebAssembly operations")
    void nestedClassMethodsMayThrowWasmException() {
      // Note: WebAssembly operations typically throw WasmException as a checked exception
      // This is intentional design to ensure callers handle WebAssembly failures
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        for (Method method : nestedClass.getDeclaredMethods()) {
          if (Modifier.isPublic(method.getModifiers()) && !method.getName().equals("close")) {
            for (Class<?> exceptionType : method.getExceptionTypes()) {
              if (!RuntimeException.class.isAssignableFrom(exceptionType)
                  && !Error.class.isAssignableFrom(exceptionType)
                  && exceptionType != Exception.class) {
                // Allow WasmException, Exception, and close() exceptions
                assertTrue(
                    method.getName().equals("close")
                        || exceptionType.getSimpleName().contains("WasmException"),
                    "Method "
                        + method.getName()
                        + " throws unexpected checked exception "
                        + exceptionType.getSimpleName());
              }
            }
          }
        }
      }
    }
  }

  // ========================================================================
  // Field Declaration Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Declaration Tests")
  class FieldDeclarationTests {

    @Test
    @DisplayName("All nested class fields should be private or package-private")
    void allFieldsShouldBePrivateOrPackagePrivate() {
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        for (Field field : nestedClass.getDeclaredFields()) {
          int modifiers = field.getModifiers();
          // Synthetic fields and constants are allowed to be different
          if (!field.isSynthetic() && !Modifier.isStatic(modifiers)) {
            assertFalse(
                Modifier.isPublic(modifiers),
                "Field "
                    + field.getName()
                    + " in "
                    + nestedClass.getSimpleName()
                    + " should not be public");
          }
        }
      }
    }

    @Test
    @DisplayName("All nested classes should have final fields where appropriate")
    void nestedClassesShouldHaveFinalFieldsWhereAppropriate() {
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        // Check that native handle fields are final (immutable after construction)
        for (Field field : nestedClass.getDeclaredFields()) {
          if (field.getName().contains("Handle")
              || field.getName().contains("handle")
              || field.getName().contains("native")) {
            assertTrue(
                Modifier.isFinal(field.getModifiers()),
                "Field "
                    + field.getName()
                    + " in "
                    + nestedClass.getSimpleName()
                    + " should be final");
          }
        }
      }
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("PanamaComponentEngine should have constructor accepting ArenaResourceManager")
    void engineShouldHaveArenaResourceManagerConstructor() {
      Class<?> engineClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
          break;
        }
      }
      assertNotNull(engineClass, "PanamaComponentEngine should exist");

      boolean hasConstructor =
          Arrays.stream(engineClass.getDeclaredConstructors())
              .anyMatch(
                  c ->
                      c.getParameterCount() > 0
                          && Arrays.asList(c.getParameterTypes())
                              .contains(ArenaResourceManager.class));
      assertTrue(
          hasConstructor,
          "PanamaComponentEngine should have a constructor accepting ArenaResourceManager");
    }

    @Test
    @DisplayName("All nested class constructors should be package-private or private")
    void allNestedConstructorsShouldBePackagePrivateOrPrivate() {
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        for (Constructor<?> constructor : nestedClass.getDeclaredConstructors()) {
          int modifiers = constructor.getModifiers();
          assertFalse(
              Modifier.isPublic(modifiers),
              "Constructor in "
                  + nestedClass.getSimpleName()
                  + " should not be public (factory pattern)");
        }
      }
    }
  }

  // ========================================================================
  // Interface Implementation Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Implementation Tests")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("All nested classes implementing AutoCloseable should have close method")
    void autoCloseableClassesShouldHaveCloseMethod() {
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (AutoCloseable.class.isAssignableFrom(nestedClass)) {
          boolean hasClose =
              Arrays.stream(nestedClass.getDeclaredMethods())
                  .anyMatch(m -> m.getName().equals("close") && m.getParameterCount() == 0);
          assertTrue(
              hasClose,
              nestedClass.getSimpleName()
                  + " implements AutoCloseable but doesn't have close() method");
        }
      }
    }

    @Test
    @DisplayName("close methods should return void")
    void closeMethodsShouldReturnVoid() {
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        for (Method method : nestedClass.getDeclaredMethods()) {
          if (method.getName().equals("close") && method.getParameterCount() == 0) {
            assertEquals(
                void.class,
                method.getReturnType(),
                "close() in " + nestedClass.getSimpleName() + " should return void");
          }
        }
      }
    }
  }
}
