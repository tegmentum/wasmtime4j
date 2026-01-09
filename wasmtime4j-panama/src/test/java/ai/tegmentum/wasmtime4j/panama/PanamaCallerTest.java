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

import ai.tegmentum.wasmtime4j.Caller;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaCaller} class.
 *
 * <p>This test class verifies the class structure, method signatures, and interface implementation
 * for the PanamaCaller class that provides access to WebAssembly instance context.
 */
@DisplayName("PanamaCaller Tests")
class PanamaCallerTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaCaller Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaCaller should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaCaller.class.getModifiers()),
          "PanamaCaller should be a final class");
    }

    @Test
    @DisplayName("PanamaCaller should have package-private visibility")
    void shouldHavePackagePrivateVisibility() {
      int modifiers = PanamaCaller.class.getModifiers();
      assertFalse(
          Modifier.isPublic(modifiers),
          "PanamaCaller should not be public (should be package-private)");
      assertFalse(Modifier.isProtected(modifiers), "PanamaCaller should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "PanamaCaller should not be private");
    }

    @Test
    @DisplayName("PanamaCaller should implement Caller interface")
    void shouldImplementCallerInterface() {
      assertTrue(
          Caller.class.isAssignableFrom(PanamaCaller.class),
          "PanamaCaller should implement Caller interface");
    }

    @Test
    @DisplayName("PanamaCaller should be a generic class with type parameter T")
    void shouldBeGenericClass() {
      TypeVariable<?>[] typeParameters = PanamaCaller.class.getTypeParameters();
      assertEquals(1, typeParameters.length, "PanamaCaller should have exactly 1 type parameter");
      assertEquals("T", typeParameters[0].getName(), "Type parameter should be named 'T'");
    }

    @Test
    @DisplayName("PanamaCaller should implement Caller<T>")
    void shouldImplementCallerWithTypeParameter() {
      Type[] genericInterfaces = PanamaCaller.class.getGenericInterfaces();
      boolean implementsCallerT = false;

      for (Type genericInterface : genericInterfaces) {
        if (genericInterface instanceof ParameterizedType) {
          ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
          if (parameterizedType.getRawType().equals(Caller.class)) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length == 1 && typeArguments[0] instanceof TypeVariable) {
              TypeVariable<?> typeVar = (TypeVariable<?>) typeArguments[0];
              if (typeVar.getName().equals("T")) {
                implementsCallerT = true;
              }
            }
          }
        }
      }

      assertTrue(implementsCallerT, "PanamaCaller should implement Caller<T>");
    }
  }

  // ========================================================================
  // Method Implementation Tests
  // ========================================================================

  @Nested
  @DisplayName("Caller Interface Method Tests")
  class CallerInterfaceMethodTests {

    @Test
    @DisplayName("should have data method returning T")
    void shouldHaveDataMethod() throws NoSuchMethodException {
      Method method = PanamaCaller.class.getMethod("data");
      assertNotNull(method, "data method should exist");
      assertEquals(0, method.getParameterCount(), "data should have no parameters");
      // Return type should be Object due to type erasure
      assertTrue(
          Object.class.isAssignableFrom(method.getReturnType()),
          "Return type should be Object (erased from T)");
    }

    @Test
    @DisplayName("should have getExport method returning Optional<Extern>")
    void shouldHaveGetExportMethod() throws NoSuchMethodException {
      Method method = PanamaCaller.class.getMethod("getExport", String.class);
      assertNotNull(method, "getExport method should exist");
      assertEquals(1, method.getParameterCount(), "getExport should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getFunction method returning Optional<WasmFunc>")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      Method method = PanamaCaller.class.getMethod("getFunction", String.class);
      assertNotNull(method, "getFunction method should exist");
      assertEquals(1, method.getParameterCount(), "getFunction should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getMemory method returning Optional<WasmMemory>")
    void shouldHaveGetMemoryMethod() throws NoSuchMethodException {
      Method method = PanamaCaller.class.getMethod("getMemory", String.class);
      assertNotNull(method, "getMemory method should exist");
      assertEquals(1, method.getParameterCount(), "getMemory should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getTable method returning Optional<WasmTable>")
    void shouldHaveGetTableMethod() throws NoSuchMethodException {
      Method method = PanamaCaller.class.getMethod("getTable", String.class);
      assertNotNull(method, "getTable method should exist");
      assertEquals(1, method.getParameterCount(), "getTable should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getGlobal method returning Optional<WasmGlobal>")
    void shouldHaveGetGlobalMethod() throws NoSuchMethodException {
      Method method = PanamaCaller.class.getMethod("getGlobal", String.class);
      assertNotNull(method, "getGlobal method should exist");
      assertEquals(1, method.getParameterCount(), "getGlobal should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }
  }

  // ========================================================================
  // Fuel Management Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Fuel Management Method Tests")
  class FuelManagementMethodTests {

    @Test
    @DisplayName("should have fuelConsumed method returning Optional<Long>")
    void shouldHaveFuelConsumedMethod() throws NoSuchMethodException {
      Method method = PanamaCaller.class.getMethod("fuelConsumed");
      assertNotNull(method, "fuelConsumed method should exist");
      assertEquals(0, method.getParameterCount(), "fuelConsumed should have no parameters");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have addFuel method")
    void shouldHaveAddFuelMethod() throws NoSuchMethodException {
      Method method = PanamaCaller.class.getMethod("addFuel", long.class);
      assertNotNull(method, "addFuel method should exist");
      assertEquals(1, method.getParameterCount(), "addFuel should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");
      // Note: Implementation returns void instead of remaining fuel
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // Field Declaration Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Declaration Tests")
  class FieldDeclarationTests {

    @Test
    @DisplayName("should have nativeHandle field")
    void shouldHaveNativeHandleField() {
      boolean hasNativeHandle =
          Arrays.stream(PanamaCaller.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getName().contains("Handle")
                          || f.getName().contains("handle")
                          || f.getType().equals(MemorySegment.class));
      assertTrue(hasNativeHandle, "Should have a native handle field");
    }

    @Test
    @DisplayName("should have store field")
    void shouldHaveStoreField() {
      boolean hasStore =
          Arrays.stream(PanamaCaller.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getName().contains("store")
                          || f.getName().contains("Store")
                          || f.getType().equals(PanamaStore.class));
      assertTrue(hasStore, "Should have a store field");
    }

    @Test
    @DisplayName("should have store field for data delegation")
    void shouldHaveStoreFieldForDataDelegation() {
      // PanamaCaller delegates data() to the store, so it needs a store field
      // Data is stored in the Store, not directly in the Caller
      boolean hasStore =
          Arrays.stream(PanamaCaller.class.getDeclaredFields())
              .anyMatch(
                  f ->
                      f.getName().contains("store")
                          || f.getName().contains("Store")
                          || f.getType().equals(PanamaStore.class));
      assertTrue(hasStore, "Should have a store field for data delegation");
    }

    @Test
    @DisplayName("all fields should be private")
    void allFieldsShouldBePrivate() {
      for (Field field : PanamaCaller.class.getDeclaredFields()) {
        if (!field.isSynthetic()) {
          assertTrue(
              Modifier.isPrivate(field.getModifiers()),
              "Field " + field.getName() + " should be private");
        }
      }
    }

    @Test
    @DisplayName("all fields should be final")
    void allFieldsShouldBeFinal() {
      for (Field field : PanamaCaller.class.getDeclaredFields()) {
        if (!field.isSynthetic()) {
          assertTrue(
              Modifier.isFinal(field.getModifiers()),
              "Field " + field.getName() + " should be final");
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
    @DisplayName("should have package-private constructor")
    void shouldHavePackagePrivateConstructor() {
      Constructor<?>[] constructors = PanamaCaller.class.getDeclaredConstructors();
      assertTrue(constructors.length >= 1, "Should have at least one constructor");

      for (Constructor<?> constructor : constructors) {
        int modifiers = constructor.getModifiers();
        assertFalse(
            Modifier.isPublic(modifiers),
            "Constructor should not be public (package-private for internal use)");
      }
    }

    @Test
    @DisplayName("constructor should accept necessary parameters")
    void constructorShouldAcceptNecessaryParameters() {
      Constructor<?>[] constructors = PanamaCaller.class.getDeclaredConstructors();
      assertTrue(constructors.length >= 1, "Should have at least one constructor");

      // Find the main constructor (with the most parameters)
      Constructor<?> mainConstructor = constructors[0];
      for (Constructor<?> constructor : constructors) {
        if (constructor.getParameterCount() > mainConstructor.getParameterCount()) {
          mainConstructor = constructor;
        }
      }

      // Should have parameters for native handle, store, and data
      assertTrue(
          mainConstructor.getParameterCount() >= 2,
          "Constructor should have at least 2 parameters (handle and store/data)");
    }
  }

  // ========================================================================
  // Method Signature Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("public methods may throw WasmException for WebAssembly operations")
    void publicMethodsMayThrowWasmException() {
      // Note: WebAssembly operations typically throw WasmException as a checked exception
      // This is intentional design to ensure callers handle WebAssembly failures
      for (Method method : PanamaCaller.class.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) {
          for (Class<?> exceptionType : method.getExceptionTypes()) {
            if (!RuntimeException.class.isAssignableFrom(exceptionType)
                && !Error.class.isAssignableFrom(exceptionType)) {
              // Allow WasmException
              assertTrue(
                  exceptionType.getSimpleName().contains("WasmException"),
                  "Method "
                      + method.getName()
                      + " throws unexpected checked exception: "
                      + exceptionType.getSimpleName());
            }
          }
        }
      }
    }

    @Test
    @DisplayName("getter methods should return appropriate types")
    void getterMethodsShouldReturnAppropriateTypes() throws NoSuchMethodException {
      // Verify return types for accessor methods
      Method dataMethod = PanamaCaller.class.getMethod("data");
      assertNotNull(dataMethod.getReturnType(), "data() should have a return type");

      Method getExportMethod = PanamaCaller.class.getMethod("getExport", String.class);
      assertEquals(
          Optional.class, getExportMethod.getReturnType(), "getExport should return Optional");

      Method getFunctionMethod = PanamaCaller.class.getMethod("getFunction", String.class);
      assertEquals(
          Optional.class, getFunctionMethod.getReturnType(), "getFunction should return Optional");

      Method getMemoryMethod = PanamaCaller.class.getMethod("getMemory", String.class);
      assertEquals(
          Optional.class, getMemoryMethod.getReturnType(), "getMemory should return Optional");

      Method getTableMethod = PanamaCaller.class.getMethod("getTable", String.class);
      assertEquals(
          Optional.class, getTableMethod.getReturnType(), "getTable should return Optional");

      Method getGlobalMethod = PanamaCaller.class.getMethod("getGlobal", String.class);
      assertEquals(
          Optional.class, getGlobalMethod.getReturnType(), "getGlobal should return Optional");
    }

    @Test
    @DisplayName("fuelConsumed should return Optional<Long>")
    void fuelConsumedShouldReturnOptionalLong() throws NoSuchMethodException {
      Method method = PanamaCaller.class.getMethod("fuelConsumed");
      assertEquals(Optional.class, method.getReturnType(), "fuelConsumed should return Optional");

      // Check generic type
      Type genericReturnType = method.getGenericReturnType();
      if (genericReturnType instanceof ParameterizedType) {
        ParameterizedType paramType = (ParameterizedType) genericReturnType;
        Type[] typeArgs = paramType.getActualTypeArguments();
        assertEquals(1, typeArgs.length, "Optional should have 1 type argument");
        assertEquals(Long.class, typeArgs[0], "Optional should contain Long");
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
    @DisplayName("PanamaCaller should implement all methods from Caller interface")
    void shouldImplementAllCallerInterfaceMethods() {
      Set<String> callerMethods =
          Arrays.stream(Caller.class.getDeclaredMethods())
              .filter(m -> !Modifier.isStatic(m.getModifiers()))
              .filter(m -> !m.isDefault())
              .map(Method::getName)
              .collect(Collectors.toSet());

      Set<String> panamaCallerMethods =
          Arrays.stream(PanamaCaller.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String methodName : callerMethods) {
        assertTrue(
            panamaCallerMethods.contains(methodName),
            "PanamaCaller should implement method: " + methodName);
      }
    }

    @Test
    @DisplayName("PanamaCaller should be in the same package as other Panama implementations")
    void shouldBeInCorrectPackage() {
      String expectedPackage = "ai.tegmentum.wasmtime4j.panama";
      assertEquals(
          expectedPackage,
          PanamaCaller.class.getPackage().getName(),
          "PanamaCaller should be in package " + expectedPackage);
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 8 declared methods")
    void shouldHaveAtLeastEightDeclaredMethods() {
      // data, getExport, getFunction, getMemory, getTable, getGlobal, fuelConsumed, addFuel
      Method[] methods = PanamaCaller.class.getDeclaredMethods();
      assertTrue(
          methods.length >= 8,
          "PanamaCaller should have at least 8 declared methods, found: " + methods.length);
    }

    @Test
    @DisplayName("should have expected public methods from Caller interface")
    void shouldHaveExpectedPublicMethods() {
      Set<String> expectedMethods =
          Set.of(
              "data",
              "getExport",
              "getFunction",
              "getMemory",
              "getTable",
              "getGlobal",
              "fuelConsumed",
              "addFuel");

      Set<String> actualMethods =
          Arrays.stream(PanamaCaller.class.getMethods())
              .filter(m -> m.getDeclaringClass().equals(PanamaCaller.class))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expectedMethod : expectedMethods) {
        assertTrue(
            actualMethods.contains(expectedMethod)
                || Arrays.stream(PanamaCaller.class.getMethods())
                    .anyMatch(m -> m.getName().equals(expectedMethod)),
            "Should have method: " + expectedMethod);
      }
    }
  }

  // ========================================================================
  // Accessibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessibility Tests")
  class AccessibilityTests {

    @Test
    @DisplayName("Caller interface methods should be public in PanamaCaller")
    void callerInterfaceMethodsShouldBePublic() throws NoSuchMethodException {
      String[] interfaceMethods = {
        "data",
        "getExport",
        "getFunction",
        "getMemory",
        "getTable",
        "getGlobal",
        "fuelConsumed",
        "addFuel"
      };

      for (String methodName : interfaceMethods) {
        Method[] methods =
            Arrays.stream(PanamaCaller.class.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .toArray(Method[]::new);
        assertTrue(methods.length > 0, "Method " + methodName + " should exist");
        for (Method method : methods) {
          assertTrue(
              Modifier.isPublic(method.getModifiers()),
              "Method " + methodName + " should be public");
        }
      }
    }
  }

  // ========================================================================
  // Native Integration Tests
  // ========================================================================

  @Nested
  @DisplayName("Native Integration Tests")
  class NativeIntegrationTests {

    @Test
    @DisplayName("should use MemorySegment for native handles")
    void shouldUseMemorySegmentForNativeHandles() {
      boolean usesMemorySegment =
          Arrays.stream(PanamaCaller.class.getDeclaredFields())
              .anyMatch(f -> f.getType().equals(MemorySegment.class));
      assertTrue(usesMemorySegment, "PanamaCaller should use MemorySegment for native handles");
    }

    @Test
    @DisplayName("constructor should accept native handle parameter")
    void constructorShouldAcceptNativeHandle() {
      // Note: Implementation uses long handle instead of MemorySegment
      Constructor<?>[] constructors = PanamaCaller.class.getDeclaredConstructors();
      boolean acceptsNativeHandle =
          Arrays.stream(constructors)
              .anyMatch(
                  c ->
                      Arrays.asList(c.getParameterTypes()).contains(MemorySegment.class)
                          || Arrays.asList(c.getParameterTypes()).contains(long.class));
      assertTrue(acceptsNativeHandle, "Constructor should accept native handle parameter");
    }
  }
}
