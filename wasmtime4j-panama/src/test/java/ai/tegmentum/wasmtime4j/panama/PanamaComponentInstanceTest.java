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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaComponentInstance}.
 *
 * <p>These tests verify class structure, method signatures, and field definitions using reflection.
 * This approach allows testing without requiring native library loading.
 */
@DisplayName("PanamaComponentInstance Tests")
class PanamaComponentInstanceTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaComponentInstance should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      int modifiers = PanamaComponentInstance.class.getModifiers();
      assertTrue(Modifier.isFinal(modifiers), "PanamaComponentInstance should be final");
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "PanamaComponentInstance should be package-private");
    }

    @Test
    @DisplayName("PanamaComponentInstance should implement ComponentInstance")
    void shouldImplementComponentInstance() {
      assertTrue(
          ComponentInstance.class.isAssignableFrom(PanamaComponentInstance.class),
          "PanamaComponentInstance should implement ComponentInstance");
    }

    @Test
    @DisplayName("Class should be in correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaComponentInstance.class.getPackage().getName(),
          "Should be in panama package");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have NATIVE_BINDINGS field")
    void shouldHaveNativeBindingsField() throws NoSuchFieldException {
      Field field = PanamaComponentInstance.class.getDeclaredField("NATIVE_BINDINGS");
      assertNotNull(field, "NATIVE_BINDINGS field should exist");
      assertEquals(
          NativeComponentBindings.class, field.getType(), "Should be NativeComponentBindings type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have enhancedEngineHandle field")
    void shouldHaveEnhancedEngineHandleField() throws NoSuchFieldException {
      Field field = PanamaComponentInstance.class.getDeclaredField("enhancedEngineHandle");
      assertNotNull(field, "enhancedEngineHandle field should exist");
      assertEquals(MemorySegment.class, field.getType(), "Should be MemorySegment type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have instanceId field")
    void shouldHaveInstanceIdField() throws NoSuchFieldException {
      Field field = PanamaComponentInstance.class.getDeclaredField("instanceId");
      assertNotNull(field, "instanceId field should exist");
      assertEquals(long.class, field.getType(), "Should be long type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have component field")
    void shouldHaveComponentField() throws NoSuchFieldException {
      Field field = PanamaComponentInstance.class.getDeclaredField("component");
      assertNotNull(field, "component field should exist");
      assertEquals(
          PanamaComponentImpl.class, field.getType(), "Should be PanamaComponentImpl type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have store field")
    void shouldHaveStoreField() throws NoSuchFieldException {
      Field field = PanamaComponentInstance.class.getDeclaredField("store");
      assertNotNull(field, "store field should exist");
      assertEquals(PanamaStore.class, field.getType(), "Should be PanamaStore type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have resourceHandle field")
    void shouldHaveResourceHandleField() throws NoSuchFieldException {
      Field field = PanamaComponentInstance.class.getDeclaredField("resourceHandle");
      assertNotNull(field, "resourceHandle field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have constructor with 4 parameters for enhanced engine")
    void shouldHaveConstructorWith4Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          PanamaComponentInstance.class.getDeclaredConstructor(
              MemorySegment.class, long.class, PanamaComponentImpl.class, PanamaStore.class);
      assertNotNull(constructor, "4-param constructor should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("Should have constructor with 3 parameters for linker-based instantiation")
    void shouldHaveConstructorWith3Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          PanamaComponentInstance.class.getDeclaredConstructor(
              MemorySegment.class, PanamaComponentImpl.class, PanamaStore.class);
      assertNotNull(constructor, "3-param constructor should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("Should have exactly 2 constructors")
    void shouldHaveExactlyTwoConstructors() {
      Constructor<?>[] constructors = PanamaComponentInstance.class.getDeclaredConstructors();
      assertEquals(2, constructors.length, "Should have exactly 2 constructors");
    }
  }

  @Nested
  @DisplayName("ComponentInstance Interface Method Tests")
  class ComponentInstanceInterfaceMethodTests {

    @Test
    @DisplayName("getId should exist and return String")
    void getIdShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getComponent should exist and return Component")
    void getComponentShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("getComponent");
      assertNotNull(method, "getComponent method should exist");
      assertEquals(Component.class, method.getReturnType(), "Should return Component");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("invoke should exist with String and varargs Object")
    void invokeShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentInstance.class.getMethod("invoke", String.class, Object[].class);
      assertNotNull(method, "invoke method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("hasFunction should exist and return boolean")
    void hasFunctionShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("hasFunction", String.class);
      assertNotNull(method, "hasFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getFunc should exist and return Optional<ComponentFunction>")
    void getFuncShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("getFunc", String.class);
      assertNotNull(method, "getFunc method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getExportedFunctions should exist and return Set<String>")
    void getExportedFunctionsShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("getExportedFunctions");
      assertNotNull(method, "getExportedFunctions method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getExportedInterfaces should exist and return Map")
    void getExportedInterfacesShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("getExportedInterfaces");
      assertNotNull(method, "getExportedInterfaces method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("bindInterface should exist")
    void bindInterfaceShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentInstance.class.getMethod("bindInterface", String.class, Object.class);
      assertNotNull(method, "bindInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getConfig should exist and return ComponentInstanceConfig")
    void getConfigShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ComponentInstanceConfig.class,
          method.getReturnType(),
          "Should return ComponentInstanceConfig");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("isValid should exist and return boolean")
    void isValidShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("pause should exist and return void")
    void pauseShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("pause");
      assertNotNull(method, "pause method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("resume should exist and return void")
    void resumeShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("resume");
      assertNotNull(method, "resume method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("stop should exist and return void")
    void stopShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("close should exist and return void")
    void closeShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }
  }

  @Nested
  @DisplayName("Package-Private Method Tests")
  class PackagePrivateMethodTests {

    @Test
    @DisplayName("getInstanceId should exist as package-private method")
    void getInstanceIdShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getDeclaredMethod("getInstanceId");
      assertNotNull(method, "getInstanceId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      int modifiers = method.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Should be package-private");
    }

    @Test
    @DisplayName("getEnhancedEngineHandle should exist as package-private method")
    void getEnhancedEngineHandleShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getDeclaredMethod("getEnhancedEngineHandle");
      assertNotNull(method, "getEnhancedEngineHandle method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
      int modifiers = method.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Should be package-private");
    }

    @Test
    @DisplayName("getStore should exist as package-private method")
    void getStoreShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getDeclaredMethod("getStore");
      assertNotNull(method, "getStore method should exist");
      assertEquals(PanamaStore.class, method.getReturnType(), "Should return PanamaStore");
      int modifiers = method.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Should be package-private");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("ensureNotClosed should exist as private method")
    void ensureNotClosedShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
    }

    @Test
    @DisplayName("convertToWitValue should exist as private static method")
    void convertToWitValueShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentInstance.class.getDeclaredMethod("convertToWitValue", Object.class);
      assertNotNull(method, "convertToWitValue method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("Should implement all ComponentInstance interface methods")
    void shouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : ComponentInstance.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : PanamaComponentInstance.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement interface method: " + interfaceMethod.getName());
        }
      }
    }

    private boolean arrayEquals(final Class<?>[] a, final Class<?>[] b) {
      if (a.length != b.length) {
        return false;
      }
      for (int i = 0; i < a.length; i++) {
        if (!a[i].equals(b[i])) {
          return false;
        }
      }
      return true;
    }
  }

  @Nested
  @DisplayName("AutoCloseable Tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaComponentInstance.class),
          "Should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Field Count Tests")
  class FieldCountTests {

    @Test
    @DisplayName("Should have expected number of fields")
    void shouldHaveExpectedFieldCount() {
      Field[] fields = PanamaComponentInstance.class.getDeclaredFields();
      // Expected: LOGGER, NATIVE_BINDINGS, enhancedEngineHandle, instanceId, component, store,
      // closed
      assertEquals(7, fields.length, "Should have exactly 7 fields");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("invoke method should declare WasmException")
    void invokeMethodShouldDeclareWasmException() throws NoSuchMethodException {
      Method method =
          PanamaComponentInstance.class.getMethod("invoke", String.class, Object[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getName().contains("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "invoke should declare WasmException");
    }

    @Test
    @DisplayName("pause method should declare WasmException")
    void pauseMethodShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("pause");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getName().contains("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "pause should declare WasmException");
    }

    @Test
    @DisplayName("resume method should declare WasmException")
    void resumeMethodShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("resume");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getName().contains("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "resume should declare WasmException");
    }

    @Test
    @DisplayName("stop method should declare WasmException")
    void stopMethodShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("stop");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getName().contains("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "stop should declare WasmException");
    }

    @Test
    @DisplayName("getFunc method should declare WasmException")
    void getFuncMethodShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("getFunc", String.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getName().contains("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "getFunc should declare WasmException");
    }

    @Test
    @DisplayName("getExportedInterfaces method should declare WasmException")
    void getExportedInterfacesMethodShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = PanamaComponentInstance.class.getMethod("getExportedInterfaces");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getName().contains("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "getExportedInterfaces should declare WasmException");
    }

    @Test
    @DisplayName("bindInterface method should declare WasmException")
    void bindInterfaceMethodShouldDeclareWasmException() throws NoSuchMethodException {
      Method method =
          PanamaComponentInstance.class.getMethod("bindInterface", String.class, Object.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getName().contains("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "bindInterface should declare WasmException");
    }
  }
}
