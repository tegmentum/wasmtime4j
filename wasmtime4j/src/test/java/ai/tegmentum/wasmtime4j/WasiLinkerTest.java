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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiLinker utility class.
 *
 * <p>WasiLinker provides static utility methods for adding WASI imports to linkers.
 */
@DisplayName("WasiLinker Utility Class Tests")
class WasiLinkerTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(WasiLinker.class.isInterface(), "WasiLinker should be a class");
      assertFalse(WasiLinker.class.isEnum(), "WasiLinker should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasiLinker.class.getModifiers()), "WasiLinker should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(Modifier.isFinal(WasiLinker.class.getModifiers()), "WasiLinker should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = WasiLinker.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("should have exactly 1 constructor")
    void shouldHaveExactly1Constructor() {
      assertEquals(
          1,
          WasiLinker.class.getDeclaredConstructors().length,
          "Should have exactly 1 constructor");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any class except Object")
    void shouldNotExtendAnyClass() {
      assertEquals(
          Object.class, WasiLinker.class.getSuperclass(), "WasiLinker should only extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          WasiLinker.class.getInterfaces().length,
          "WasiLinker should not implement any interfaces");
    }
  }

  // ========================================================================
  // Static Method Tests - addToLinker
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests - addToLinker")
  class AddToLinkerMethodTests {

    @Test
    @DisplayName("should have addToLinker(Linker, WasiContext) method")
    void shouldHaveAddToLinkerWithContextMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("addToLinker", Linker.class, WasiContext.class);
      assertNotNull(method, "addToLinker(Linker, WasiContext) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have addToLinker(Linker) method")
    void shouldHaveAddToLinkerDefaultMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("addToLinker", Linker.class);
      assertNotNull(method, "addToLinker(Linker) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("addToLinker(Linker, WasiContext) should declare WasmException")
    void addToLinkerWithContextShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("addToLinker", Linker.class, WasiContext.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(ex -> ex.getSimpleName().equals("WasmException"));
      assertTrue(
          declaresWasmException, "addToLinker(Linker, WasiContext) should declare WasmException");
    }

    @Test
    @DisplayName("addToLinker(Linker) should declare WasmException")
    void addToLinkerDefaultShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("addToLinker", Linker.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(ex -> ex.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "addToLinker(Linker) should declare WasmException");
    }
  }

  // ========================================================================
  // Static Method Tests - createLinker
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests - createLinker")
  class CreateLinkerMethodTests {

    @Test
    @DisplayName("should have createLinker(Engine, WasiContext) method")
    void shouldHaveCreateLinkerWithContextMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createLinker", Engine.class, WasiContext.class);
      assertNotNull(method, "createLinker(Engine, WasiContext) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("should have createLinker(Engine) method")
    void shouldHaveCreateLinkerDefaultMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createLinker", Engine.class);
      assertNotNull(method, "createLinker(Engine) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("createLinker(Engine, WasiContext) should declare WasmException")
    void createLinkerWithContextShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createLinker", Engine.class, WasiContext.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(ex -> ex.getSimpleName().equals("WasmException"));
      assertTrue(
          declaresWasmException, "createLinker(Engine, WasiContext) should declare WasmException");
    }

    @Test
    @DisplayName("createLinker(Engine) should declare WasmException")
    void createLinkerDefaultShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createLinker", Engine.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(ex -> ex.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "createLinker(Engine) should declare WasmException");
    }
  }

  // ========================================================================
  // Static Method Tests - addPreview2ToLinker
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests - addPreview2ToLinker")
  class AddPreview2ToLinkerMethodTests {

    @Test
    @DisplayName("should have addPreview2ToLinker(Linker, WasiContext) method")
    void shouldHaveAddPreview2ToLinkerWithContextMethod() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod("addPreview2ToLinker", Linker.class, WasiContext.class);
      assertNotNull(method, "addPreview2ToLinker(Linker, WasiContext) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have addPreview2ToLinker(Linker) method")
    void shouldHaveAddPreview2ToLinkerDefaultMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("addPreview2ToLinker", Linker.class);
      assertNotNull(method, "addPreview2ToLinker(Linker) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("addPreview2ToLinker(Linker, WasiContext) should declare WasmException")
    void addPreview2ToLinkerWithContextShouldDeclareWasmException() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod("addPreview2ToLinker", Linker.class, WasiContext.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(ex -> ex.getSimpleName().equals("WasmException"));
      assertTrue(
          declaresWasmException,
          "addPreview2ToLinker(Linker, WasiContext) should declare WasmException");
    }
  }

  // ========================================================================
  // Static Method Tests - createPreview2Linker
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests - createPreview2Linker")
  class CreatePreview2LinkerMethodTests {

    @Test
    @DisplayName("should have createPreview2Linker(Engine, WasiContext) method")
    void shouldHaveCreatePreview2LinkerWithContextMethod() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod("createPreview2Linker", Engine.class, WasiContext.class);
      assertNotNull(method, "createPreview2Linker(Engine, WasiContext) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("should have createPreview2Linker(Engine) method")
    void shouldHaveCreatePreview2LinkerDefaultMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createPreview2Linker", Engine.class);
      assertNotNull(method, "createPreview2Linker(Engine) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("createPreview2Linker methods should declare WasmException")
    void createPreview2LinkerShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createPreview2Linker", Engine.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(ex -> ex.getSimpleName().equals("WasmException"));
      assertTrue(
          declaresWasmException, "createPreview2Linker(Engine) should declare WasmException");
    }
  }

  // ========================================================================
  // Static Method Tests - addComponentModelToLinker
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests - addComponentModelToLinker")
  class AddComponentModelToLinkerMethodTests {

    @Test
    @DisplayName("should have addComponentModelToLinker(Linker) method")
    void shouldHaveAddComponentModelToLinkerMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("addComponentModelToLinker", Linker.class);
      assertNotNull(method, "addComponentModelToLinker(Linker) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("addComponentModelToLinker should declare WasmException")
    void addComponentModelToLinkerShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("addComponentModelToLinker", Linker.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(ex -> ex.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "addComponentModelToLinker should declare WasmException");
    }
  }

  // ========================================================================
  // Static Method Tests - createFullLinker
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests - createFullLinker")
  class CreateFullLinkerMethodTests {

    @Test
    @DisplayName("should have createFullLinker(Engine, WasiContext) method")
    void shouldHaveCreateFullLinkerWithContextMethod() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod("createFullLinker", Engine.class, WasiContext.class);
      assertNotNull(method, "createFullLinker(Engine, WasiContext) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("should have createFullLinker(Engine) method")
    void shouldHaveCreateFullLinkerDefaultMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createFullLinker", Engine.class);
      assertNotNull(method, "createFullLinker(Engine) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("createFullLinker methods should declare WasmException")
    void createFullLinkerShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createFullLinker", Engine.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(ex -> ex.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "createFullLinker(Engine) should declare WasmException");
    }
  }

  // ========================================================================
  // Static Method Tests - hasWasiImports
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests - hasWasiImports")
  class HasWasiImportsMethodTests {

    @Test
    @DisplayName("should have hasWasiImports method")
    void shouldHaveHasWasiImportsMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("hasWasiImports", Linker.class);
      assertNotNull(method, "hasWasiImports method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hasWasiPreview2Imports method")
    void shouldHaveHasWasiPreview2ImportsMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("hasWasiPreview2Imports", Linker.class);
      assertNotNull(method, "hasWasiPreview2Imports method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hasComponentModelImports method")
    void shouldHaveHasComponentModelImportsMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("hasComponentModelImports", Linker.class);
      assertNotNull(method, "hasComponentModelImports method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected static methods")
    void shouldHaveAllExpectedStaticMethods() {
      Set<String> expectedMethods =
          Set.of(
              "addToLinker",
              "createLinker",
              "addPreview2ToLinker",
              "createPreview2Linker",
              "addComponentModelToLinker",
              "createFullLinker",
              "hasWasiImports",
              "hasWasiPreview2Imports",
              "hasComponentModelImports");

      Set<String> actualMethods =
          Arrays.stream(WasiLinker.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("all public methods should be static")
    void allPublicMethodsShouldBeStatic() {
      long nonStaticPublicMethods =
          Arrays.stream(WasiLinker.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .filter(m -> !Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, nonStaticPublicMethods, "All public methods should be static");
    }

    @Test
    @DisplayName("should have at least 14 static methods (counting overloads)")
    void shouldHaveAtLeast14StaticMethods() {
      long staticCount =
          Arrays.stream(WasiLinker.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertTrue(staticCount >= 14, "Should have at least 14 static methods (with overloads)");
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
          WasiLinker.class.getDeclaredClasses().length,
          "WasiLinker should have no nested classes");
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
          WasiLinker.class.getDeclaredFields().length,
          "WasiLinker should have no declared fields");
    }
  }
}
