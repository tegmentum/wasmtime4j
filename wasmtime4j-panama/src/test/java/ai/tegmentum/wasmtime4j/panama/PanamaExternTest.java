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

import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.type.ExternType;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Panama Extern implementations.
 *
 * <p>This test class verifies the class structure, method signatures, and interface implementation
 * for PanamaExternFunc, PanamaExternGlobal, PanamaExternMemory, and PanamaExternTable.
 */
@DisplayName("Panama Extern Implementation Tests")
class PanamaExternTest {

  // ========================================================================
  // PanamaExternFunc Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaExternFunc Tests")
  class PanamaExternFuncTests {

    @Test
    @DisplayName("PanamaExternFunc should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaExternFunc.class.getModifiers()),
          "PanamaExternFunc should be a final class");
    }

    @Test
    @DisplayName("PanamaExternFunc should have package-private visibility")
    void shouldHavePackagePrivateVisibility() {
      int modifiers = PanamaExternFunc.class.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "PanamaExternFunc should not be public");
    }

    @Test
    @DisplayName("PanamaExternFunc should implement Extern interface")
    void shouldImplementExternInterface() {
      assertTrue(
          Extern.class.isAssignableFrom(PanamaExternFunc.class),
          "PanamaExternFunc should implement Extern interface");
    }

    @Test
    @DisplayName("PanamaExternFunc should have getType method returning FUNC")
    void shouldHaveGetTypeMethodReturningFunc() throws NoSuchMethodException {
      Method method = PanamaExternFunc.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Return type should be ExternType");
    }

    @Test
    @DisplayName("PanamaExternFunc should have asFunction method")
    void shouldHaveAsFunctionMethod() throws NoSuchMethodException {
      Method method = PanamaExternFunc.class.getMethod("asFunction");
      assertNotNull(method, "asFunction method should exist");
      assertEquals(
          WasmFunction.class, method.getReturnType(), "Return type should be WasmFunction");
    }

    @Test
    @DisplayName("PanamaExternFunc should have nativeHandle field")
    void shouldHaveNativeHandleField() {
      boolean hasNativeHandle =
          Arrays.stream(PanamaExternFunc.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getName().contains("Handle")
                          || f.getName().contains("handle")
                          || f.getType().equals(MemorySegment.class));
      assertTrue(hasNativeHandle, "Should have a native handle field");
    }

    @Test
    @DisplayName("PanamaExternFunc should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() {
      boolean hasMethod =
          Arrays.stream(PanamaExternFunc.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("getNativeHandle"));
      assertTrue(hasMethod, "Should have getNativeHandle method");
    }

    @Test
    @DisplayName("PanamaExternFunc constructor should be package-private")
    void constructorShouldBePackagePrivate() {
      Constructor<?>[] constructors = PanamaExternFunc.class.getDeclaredConstructors();
      for (Constructor<?> constructor : constructors) {
        assertFalse(
            Modifier.isPublic(constructor.getModifiers()), "Constructor should not be public");
      }
    }
  }

  // ========================================================================
  // PanamaExternGlobal Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaExternGlobal Tests")
  class PanamaExternGlobalTests {

    @Test
    @DisplayName("PanamaExternGlobal should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaExternGlobal.class.getModifiers()),
          "PanamaExternGlobal should be a final class");
    }

    @Test
    @DisplayName("PanamaExternGlobal should have package-private visibility")
    void shouldHavePackagePrivateVisibility() {
      int modifiers = PanamaExternGlobal.class.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "PanamaExternGlobal should not be public");
    }

    @Test
    @DisplayName("PanamaExternGlobal should implement Extern interface")
    void shouldImplementExternInterface() {
      assertTrue(
          Extern.class.isAssignableFrom(PanamaExternGlobal.class),
          "PanamaExternGlobal should implement Extern interface");
    }

    @Test
    @DisplayName("PanamaExternGlobal should have getType method returning GLOBAL")
    void shouldHaveGetTypeMethodReturningGlobal() throws NoSuchMethodException {
      Method method = PanamaExternGlobal.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Return type should be ExternType");
    }

    @Test
    @DisplayName("PanamaExternGlobal should have asGlobal method")
    void shouldHaveAsGlobalMethod() throws NoSuchMethodException {
      Method method = PanamaExternGlobal.class.getMethod("asGlobal");
      assertNotNull(method, "asGlobal method should exist");
      assertEquals(WasmGlobal.class, method.getReturnType(), "Return type should be WasmGlobal");
    }

    @Test
    @DisplayName("PanamaExternGlobal should have nativeHandle field")
    void shouldHaveNativeHandleField() {
      boolean hasNativeHandle =
          Arrays.stream(PanamaExternGlobal.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(MemorySegment.class));
      assertTrue(hasNativeHandle, "Should have a MemorySegment field for native handle");
    }

    @Test
    @DisplayName("PanamaExternGlobal should have store field")
    void shouldHaveStoreField() {
      boolean hasStore =
          Arrays.stream(PanamaExternGlobal.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(PanamaStore.class));
      assertTrue(hasStore, "Should have a PanamaStore field");
    }

    @Test
    @DisplayName("PanamaExternGlobal should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() {
      boolean hasMethod =
          Arrays.stream(PanamaExternGlobal.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("getNativeHandle"));
      assertTrue(hasMethod, "Should have getNativeHandle method");
    }

    @Test
    @DisplayName("PanamaExternGlobal constructor should accept MemorySegment and PanamaStore")
    void constructorShouldAcceptCorrectParameters() {
      Constructor<?>[] constructors = PanamaExternGlobal.class.getDeclaredConstructors();
      boolean hasCorrectConstructor =
          Arrays.stream(constructors)
              .anyMatch(
                  c -> {
                    Class<?>[] paramTypes = c.getParameterTypes();
                    return paramTypes.length == 2
                        && Arrays.asList(paramTypes).contains(MemorySegment.class)
                        && Arrays.asList(paramTypes).contains(PanamaStore.class);
                  });
      assertTrue(
          hasCorrectConstructor, "Should have constructor accepting MemorySegment and PanamaStore");
    }
  }

  // ========================================================================
  // PanamaExternMemory Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaExternMemory Tests")
  class PanamaExternMemoryTests {

    @Test
    @DisplayName("PanamaExternMemory should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaExternMemory.class.getModifiers()),
          "PanamaExternMemory should be a final class");
    }

    @Test
    @DisplayName("PanamaExternMemory should have package-private visibility")
    void shouldHavePackagePrivateVisibility() {
      int modifiers = PanamaExternMemory.class.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "PanamaExternMemory should not be public");
    }

    @Test
    @DisplayName("PanamaExternMemory should implement Extern interface")
    void shouldImplementExternInterface() {
      assertTrue(
          Extern.class.isAssignableFrom(PanamaExternMemory.class),
          "PanamaExternMemory should implement Extern interface");
    }

    @Test
    @DisplayName("PanamaExternMemory should have getType method returning MEMORY")
    void shouldHaveGetTypeMethodReturningMemory() throws NoSuchMethodException {
      Method method = PanamaExternMemory.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Return type should be ExternType");
    }

    @Test
    @DisplayName("PanamaExternMemory should have asMemory method")
    void shouldHaveAsMemoryMethod() throws NoSuchMethodException {
      Method method = PanamaExternMemory.class.getMethod("asMemory");
      assertNotNull(method, "asMemory method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "Return type should be WasmMemory");
    }

    @Test
    @DisplayName("PanamaExternMemory should have nativeHandle field")
    void shouldHaveNativeHandleField() {
      boolean hasNativeHandle =
          Arrays.stream(PanamaExternMemory.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(MemorySegment.class));
      assertTrue(hasNativeHandle, "Should have a MemorySegment field for native handle");
    }

    @Test
    @DisplayName("PanamaExternMemory should have store field")
    void shouldHaveStoreField() {
      boolean hasStore =
          Arrays.stream(PanamaExternMemory.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(PanamaStore.class));
      assertTrue(hasStore, "Should have a PanamaStore field");
    }

    @Test
    @DisplayName("PanamaExternMemory should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() {
      boolean hasMethod =
          Arrays.stream(PanamaExternMemory.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("getNativeHandle"));
      assertTrue(hasMethod, "Should have getNativeHandle method");
    }

    @Test
    @DisplayName("PanamaExternMemory constructor should accept MemorySegment and PanamaStore")
    void constructorShouldAcceptCorrectParameters() {
      Constructor<?>[] constructors = PanamaExternMemory.class.getDeclaredConstructors();
      boolean hasCorrectConstructor =
          Arrays.stream(constructors)
              .anyMatch(
                  c -> {
                    Class<?>[] paramTypes = c.getParameterTypes();
                    return paramTypes.length == 2
                        && Arrays.asList(paramTypes).contains(MemorySegment.class)
                        && Arrays.asList(paramTypes).contains(PanamaStore.class);
                  });
      assertTrue(
          hasCorrectConstructor, "Should have constructor accepting MemorySegment and PanamaStore");
    }
  }

  // ========================================================================
  // PanamaExternTable Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaExternTable Tests")
  class PanamaExternTableTests {

    @Test
    @DisplayName("PanamaExternTable should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaExternTable.class.getModifiers()),
          "PanamaExternTable should be a final class");
    }

    @Test
    @DisplayName("PanamaExternTable should have package-private visibility")
    void shouldHavePackagePrivateVisibility() {
      int modifiers = PanamaExternTable.class.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "PanamaExternTable should not be public");
    }

    @Test
    @DisplayName("PanamaExternTable should implement Extern interface")
    void shouldImplementExternInterface() {
      assertTrue(
          Extern.class.isAssignableFrom(PanamaExternTable.class),
          "PanamaExternTable should implement Extern interface");
    }

    @Test
    @DisplayName("PanamaExternTable should have getType method returning TABLE")
    void shouldHaveGetTypeMethodReturningTable() throws NoSuchMethodException {
      Method method = PanamaExternTable.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Return type should be ExternType");
    }

    @Test
    @DisplayName("PanamaExternTable should have asTable method")
    void shouldHaveAsTableMethod() throws NoSuchMethodException {
      Method method = PanamaExternTable.class.getMethod("asTable");
      assertNotNull(method, "asTable method should exist");
      assertEquals(WasmTable.class, method.getReturnType(), "Return type should be WasmTable");
    }

    @Test
    @DisplayName("PanamaExternTable should have nativeHandle field")
    void shouldHaveNativeHandleField() {
      boolean hasNativeHandle =
          Arrays.stream(PanamaExternTable.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(MemorySegment.class));
      assertTrue(hasNativeHandle, "Should have a MemorySegment field for native handle");
    }

    @Test
    @DisplayName("PanamaExternTable should have store field")
    void shouldHaveStoreField() {
      boolean hasStore =
          Arrays.stream(PanamaExternTable.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(PanamaStore.class));
      assertTrue(hasStore, "Should have a PanamaStore field");
    }

    @Test
    @DisplayName("PanamaExternTable should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() {
      boolean hasMethod =
          Arrays.stream(PanamaExternTable.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("getNativeHandle"));
      assertTrue(hasMethod, "Should have getNativeHandle method");
    }

    @Test
    @DisplayName("PanamaExternTable constructor should accept MemorySegment and PanamaStore")
    void constructorShouldAcceptCorrectParameters() {
      Constructor<?>[] constructors = PanamaExternTable.class.getDeclaredConstructors();
      boolean hasCorrectConstructor =
          Arrays.stream(constructors)
              .anyMatch(
                  c -> {
                    Class<?>[] paramTypes = c.getParameterTypes();
                    return paramTypes.length == 2
                        && Arrays.asList(paramTypes).contains(MemorySegment.class)
                        && Arrays.asList(paramTypes).contains(PanamaStore.class);
                  });
      assertTrue(
          hasCorrectConstructor, "Should have constructor accepting MemorySegment and PanamaStore");
    }
  }

  // ========================================================================
  // Common Extern Pattern Tests
  // ========================================================================

  @Nested
  @DisplayName("Common Extern Pattern Tests")
  class CommonExternPatternTests {

    @Test
    @DisplayName("All Extern implementations should be in same package")
    void allExternImplementationsShouldBeInSamePackage() {
      String expectedPackage = "ai.tegmentum.wasmtime4j.panama";
      assertEquals(expectedPackage, PanamaExternFunc.class.getPackage().getName());
      assertEquals(expectedPackage, PanamaExternGlobal.class.getPackage().getName());
      assertEquals(expectedPackage, PanamaExternMemory.class.getPackage().getName());
      assertEquals(expectedPackage, PanamaExternTable.class.getPackage().getName());
    }

    @Test
    @DisplayName("All Extern implementations should implement Extern interface")
    void allExternImplementationsShouldImplementExtern() {
      assertTrue(Extern.class.isAssignableFrom(PanamaExternFunc.class));
      assertTrue(Extern.class.isAssignableFrom(PanamaExternGlobal.class));
      assertTrue(Extern.class.isAssignableFrom(PanamaExternMemory.class));
      assertTrue(Extern.class.isAssignableFrom(PanamaExternTable.class));
    }

    @Test
    @DisplayName("All Extern implementations should be final classes")
    void allExternImplementationsShouldBeFinal() {
      assertTrue(Modifier.isFinal(PanamaExternFunc.class.getModifiers()));
      assertTrue(Modifier.isFinal(PanamaExternGlobal.class.getModifiers()));
      assertTrue(Modifier.isFinal(PanamaExternMemory.class.getModifiers()));
      assertTrue(Modifier.isFinal(PanamaExternTable.class.getModifiers()));
    }

    @Test
    @DisplayName("All Extern implementations should have package-private visibility")
    void allExternImplementationsShouldBePackagePrivate() {
      assertFalse(Modifier.isPublic(PanamaExternFunc.class.getModifiers()));
      assertFalse(Modifier.isPublic(PanamaExternGlobal.class.getModifiers()));
      assertFalse(Modifier.isPublic(PanamaExternMemory.class.getModifiers()));
      assertFalse(Modifier.isPublic(PanamaExternTable.class.getModifiers()));
    }

    @Test
    @DisplayName("All Extern implementations should have getType method")
    void allExternImplementationsShouldHaveGetTypeMethod() throws NoSuchMethodException {
      assertNotNull(PanamaExternFunc.class.getMethod("getType"));
      assertNotNull(PanamaExternGlobal.class.getMethod("getType"));
      assertNotNull(PanamaExternMemory.class.getMethod("getType"));
      assertNotNull(PanamaExternTable.class.getMethod("getType"));
    }

    @Test
    @DisplayName("All Extern implementations should have getNativeHandle method")
    void allExternImplementationsShouldHaveGetNativeHandleMethod() {
      Class<?>[] externClasses = {
        PanamaExternFunc.class,
        PanamaExternGlobal.class,
        PanamaExternMemory.class,
        PanamaExternTable.class
      };

      for (Class<?> externClass : externClasses) {
        boolean hasMethod =
            Arrays.stream(externClass.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("getNativeHandle"));
        assertTrue(hasMethod, externClass.getSimpleName() + " should have getNativeHandle method");
      }
    }
  }

  // ========================================================================
  // Field Declaration Consistency Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Declaration Consistency Tests")
  class FieldDeclarationConsistencyTests {

    @Test
    @DisplayName("All fields in Extern implementations should be private")
    void allFieldsShouldBePrivate() {
      Class<?>[] externClasses = {
        PanamaExternFunc.class,
        PanamaExternGlobal.class,
        PanamaExternMemory.class,
        PanamaExternTable.class
      };

      for (Class<?> externClass : externClasses) {
        for (Field field : externClass.getDeclaredFields()) {
          if (!field.isSynthetic()) {
            assertTrue(
                Modifier.isPrivate(field.getModifiers()),
                "Field "
                    + field.getName()
                    + " in "
                    + externClass.getSimpleName()
                    + " should be private");
          }
        }
      }
    }

    @Test
    @DisplayName("All fields in Extern implementations should be final")
    void allFieldsShouldBeFinal() {
      Class<?>[] externClasses = {
        PanamaExternFunc.class,
        PanamaExternGlobal.class,
        PanamaExternMemory.class,
        PanamaExternTable.class
      };

      for (Class<?> externClass : externClasses) {
        for (Field field : externClass.getDeclaredFields()) {
          if (!field.isSynthetic()) {
            assertTrue(
                Modifier.isFinal(field.getModifiers()),
                "Field "
                    + field.getName()
                    + " in "
                    + externClass.getSimpleName()
                    + " should be final");
          }
        }
      }
    }
  }

  // ========================================================================
  // Constructor Pattern Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Pattern Tests")
  class ConstructorPatternTests {

    @Test
    @DisplayName("All Extern implementations should have package-private constructors")
    void allConstructorsShouldBePackagePrivate() {
      Class<?>[] externClasses = {
        PanamaExternFunc.class,
        PanamaExternGlobal.class,
        PanamaExternMemory.class,
        PanamaExternTable.class
      };

      for (Class<?> externClass : externClasses) {
        for (Constructor<?> constructor : externClass.getDeclaredConstructors()) {
          assertFalse(
              Modifier.isPublic(constructor.getModifiers()),
              "Constructor in " + externClass.getSimpleName() + " should not be public");
        }
      }
    }

    @Test
    @DisplayName("All Extern implementations should have at least one constructor")
    void allShouldHaveAtLeastOneConstructor() {
      Class<?>[] externClasses = {
        PanamaExternFunc.class,
        PanamaExternGlobal.class,
        PanamaExternMemory.class,
        PanamaExternTable.class
      };

      for (Class<?> externClass : externClasses) {
        Constructor<?>[] constructors = externClass.getDeclaredConstructors();
        assertTrue(
            constructors.length >= 1,
            externClass.getSimpleName() + " should have at least one constructor");
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
    @DisplayName("getType methods should all return ExternType")
    void getTypeMethodsShouldReturnExternType() throws NoSuchMethodException {
      assertEquals(ExternType.class, PanamaExternFunc.class.getMethod("getType").getReturnType());
      assertEquals(ExternType.class, PanamaExternGlobal.class.getMethod("getType").getReturnType());
      assertEquals(ExternType.class, PanamaExternMemory.class.getMethod("getType").getReturnType());
      assertEquals(ExternType.class, PanamaExternTable.class.getMethod("getType").getReturnType());
    }

    @Test
    @DisplayName("asFunction should return WasmFunction")
    void asFunctionShouldReturnWasmFunction() throws NoSuchMethodException {
      assertEquals(
          WasmFunction.class, PanamaExternFunc.class.getMethod("asFunction").getReturnType());
    }

    @Test
    @DisplayName("asGlobal should return WasmGlobal")
    void asGlobalShouldReturnWasmGlobal() throws NoSuchMethodException {
      assertEquals(
          WasmGlobal.class, PanamaExternGlobal.class.getMethod("asGlobal").getReturnType());
    }

    @Test
    @DisplayName("asMemory should return WasmMemory")
    void asMemoryShouldReturnWasmMemory() throws NoSuchMethodException {
      assertEquals(
          WasmMemory.class, PanamaExternMemory.class.getMethod("asMemory").getReturnType());
    }

    @Test
    @DisplayName("asTable should return WasmTable")
    void asTableShouldReturnWasmTable() throws NoSuchMethodException {
      assertEquals(WasmTable.class, PanamaExternTable.class.getMethod("asTable").getReturnType());
    }
  }
}
