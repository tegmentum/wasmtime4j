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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceState;
import ai.tegmentum.wasmtime4j.component.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JniComponentInstanceImpl}.
 *
 * <p>These tests use reflection to verify the class structure, method signatures, and expected
 * behavior without requiring native library loading.
 */
@DisplayName("JniComponentInstanceImpl Tests")
class JniComponentInstanceImplTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniComponentInstanceImpl should be public final class")
    void shouldBePublicFinalClass() {
      int modifiers = JniComponentInstanceImpl.class.getModifiers();
      assertTrue(Modifier.isPublic(modifiers), "Should be public");
      assertTrue(Modifier.isFinal(modifiers), "Should be final");
    }

    @Test
    @DisplayName("JniComponentInstanceImpl should implement ComponentInstance")
    void shouldImplementComponentInstance() {
      assertTrue(
          ComponentInstance.class.isAssignableFrom(JniComponentInstanceImpl.class),
          "Should implement ComponentInstance");
    }

    @Test
    @DisplayName("JniComponentInstanceImpl should not extend any non-Object class")
    void shouldNotExtendNonObjectClass() {
      Class<?> superclass = JniComponentInstanceImpl.class.getSuperclass();
      assertEquals(Object.class, superclass, "Should directly extend Object");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = JniComponentInstanceImpl.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertEquals(Logger.class, field.getType(), "Should be Logger type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have nativeInstance field")
    void shouldHaveNativeInstanceField() throws NoSuchFieldException {
      Field field = JniComponentInstanceImpl.class.getDeclaredField("nativeInstance");
      assertNotNull(field, "nativeInstance field should exist");
      assertEquals(
          JniComponent.JniComponentInstanceHandle.class,
          field.getType(),
          "Should be JniComponentInstanceHandle type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have component field")
    void shouldHaveComponentField() throws NoSuchFieldException {
      Field field = JniComponentInstanceImpl.class.getDeclaredField("component");
      assertNotNull(field, "component field should exist");
      assertEquals(JniComponentImpl.class, field.getType(), "Should be JniComponentImpl type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have config field")
    void shouldHaveConfigField() throws NoSuchFieldException {
      Field field = JniComponentInstanceImpl.class.getDeclaredField("config");
      assertNotNull(field, "config field should exist");
      assertEquals(
          ComponentInstanceConfig.class, field.getType(), "Should be ComponentInstanceConfig type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have instanceId field")
    void shouldHaveInstanceIdField() throws NoSuchFieldException {
      Field field = JniComponentInstanceImpl.class.getDeclaredField("instanceId");
      assertNotNull(field, "instanceId field should exist");
      assertEquals(String.class, field.getType(), "Should be String type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have public constructor with three parameters")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      Constructor<JniComponentInstanceImpl> constructor =
          JniComponentInstanceImpl.class.getConstructor(
              JniComponent.JniComponentInstanceHandle.class,
              JniComponentImpl.class,
              ComponentInstanceConfig.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("Constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<JniComponentInstanceImpl> constructor =
          JniComponentInstanceImpl.class.getConstructor(
              JniComponent.JniComponentInstanceHandle.class,
              JniComponentImpl.class,
              ComponentInstanceConfig.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(3, paramTypes.length, "Should have 3 parameters");
      assertEquals(
          JniComponent.JniComponentInstanceHandle.class,
          paramTypes[0],
          "First param should be JniComponentInstanceHandle");
      assertEquals(
          JniComponentImpl.class, paramTypes[1], "Second param should be JniComponentImpl");
      assertEquals(
          ComponentInstanceConfig.class,
          paramTypes[2],
          "Third param should be ComponentInstanceConfig");
    }
  }

  @Nested
  @DisplayName("ComponentInstance Interface Method Tests")
  class ComponentInstanceInterfaceMethodTests {

    @Test
    @DisplayName("getId method should exist and return String")
    void getIdMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getComponent method should exist and return Component")
    void getComponentMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getComponent");
      assertNotNull(method, "getComponent method should exist");
      assertEquals(Component.class, method.getReturnType(), "Should return Component");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getState method should exist and return ComponentInstanceState")
    void getStateMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(
          ComponentInstanceState.class,
          method.getReturnType(),
          "Should return ComponentInstanceState");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getResourceUsage method should exist and return ComponentResourceUsage")
    void getResourceUsageMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getResourceUsage");
      assertNotNull(method, "getResourceUsage method should exist");
      assertEquals(
          ComponentResourceUsage.class,
          method.getReturnType(),
          "Should return ComponentResourceUsage");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("isValid method should exist and return boolean")
    void isValidMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("close method should exist and return void")
    void closeMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("stop method should exist and return void")
    void stopMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");

      // Check throws WasmException
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean throwsWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (WasmException.class.isAssignableFrom(exType)) {
          throwsWasmException = true;
          break;
        }
      }
      assertTrue(throwsWasmException, "Should throw WasmException");
    }

    @Test
    @DisplayName("pause method should exist and return void")
    void pauseMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("pause");
      assertNotNull(method, "pause method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("resume method should exist and return void")
    void resumeMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("resume");
      assertNotNull(method, "resume method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("bindInterface method should exist with correct signature")
    void bindInterfaceMethodShouldExist() throws NoSuchMethodException {
      Method method =
          JniComponentInstanceImpl.class.getMethod("bindInterface", String.class, Object.class);
      assertNotNull(method, "bindInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getExportedInterfaces method should exist and return Map")
    void getExportedInterfacesMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getExportedInterfaces");
      assertNotNull(method, "getExportedInterfaces method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getExportedFunctions method should exist and return Set")
    void getExportedFunctionsMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getExportedFunctions");
      assertNotNull(method, "getExportedFunctions method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("hasFunction method should exist and return boolean")
    void hasFunctionMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("hasFunction", String.class);
      assertNotNull(method, "hasFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getFunc method should exist and return Optional")
    void getFuncMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getFunc", String.class);
      assertNotNull(method, "getFunc method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("invoke method should exist with correct signature")
    void invokeMethodShouldExist() throws NoSuchMethodException {
      Method method =
          JniComponentInstanceImpl.class.getMethod("invoke", String.class, Object[].class);
      assertNotNull(method, "invoke method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }
  }

  @Nested
  @DisplayName("Additional Public Method Tests")
  class AdditionalPublicMethodTests {

    @Test
    @DisplayName("getNativeInstance method should exist and return JniComponentInstanceHandle")
    void getNativeInstanceMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getNativeInstance");
      assertNotNull(method, "getNativeInstance method should exist");
      assertEquals(
          JniComponent.JniComponentInstanceHandle.class,
          method.getReturnType(),
          "Should return JniComponentInstanceHandle");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getConfig method should exist and return ComponentInstanceConfig")
    void getConfigMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ComponentInstanceConfig.class,
          method.getReturnType(),
          "Should return ComponentInstanceConfig");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
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
          for (Method implMethod : JniComponentInstanceImpl.class.getMethods()) {
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

    private boolean arrayEquals(Class<?>[] a, Class<?>[] b) {
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
  @DisplayName("AutoCloseable Support Tests")
  class AutoCloseableSupportTests {

    @Test
    @DisplayName("JniComponentInstanceImpl should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(JniComponentInstanceImpl.class),
          "Should implement AutoCloseable via ComponentInstance");
    }

    @Test
    @DisplayName("close method from AutoCloseable should exist")
    void closeMethodFromAutoCloseableShouldExist() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Package Location Tests")
  class PackageLocationTests {

    @Test
    @DisplayName("Class should be in correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni",
          JniComponentInstanceImpl.class.getPackage().getName(),
          "Should be in jni package");
    }
  }

  @Nested
  @DisplayName("Method Return Type Verification Tests")
  class MethodReturnTypeVerificationTests {

    @Test
    @DisplayName("getExportedInterfaces should return Map with correct generic types")
    void getExportedInterfacesShouldReturnCorrectType() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getExportedInterfaces");
      // Check it's a Map (generic type erasure prevents checking exact generics at runtime)
      assertTrue(Map.class.isAssignableFrom(method.getReturnType()), "Should return a Map");
    }

    @Test
    @DisplayName("getExportedFunctions should return Set")
    void getExportedFunctionsShouldReturnCorrectType() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getExportedFunctions");
      assertTrue(Set.class.isAssignableFrom(method.getReturnType()), "Should return a Set");
    }

    @Test
    @DisplayName("getFunc should return Optional")
    void getFuncShouldReturnCorrectType() throws NoSuchMethodException {
      Method method = JniComponentInstanceImpl.class.getMethod("getFunc", String.class);
      assertTrue(
          Optional.class.isAssignableFrom(method.getReturnType()), "Should return an Optional");
    }
  }

  @Nested
  @DisplayName("Field Count Verification Tests")
  class FieldCountVerificationTests {

    @Test
    @DisplayName("Should have at least 5 non-synthetic fields")
    void shouldHaveAtLeastFiveNonSyntheticFields() {
      // Count only non-synthetic fields (excludes $jacocoData and similar)
      long nonSyntheticCount =
          java.util.Arrays.stream(JniComponentInstanceImpl.class.getDeclaredFields())
              .filter(f -> !f.isSynthetic())
              .count();
      assertTrue(
          nonSyntheticCount >= 5,
          "Should have at least 5 non-synthetic fields (LOGGER, nativeInstance, component, config, "
              + "instanceId), found: "
              + nonSyntheticCount);
    }

    @Test
    @DisplayName("All fields except LOGGER should be instance fields")
    void allInstanceFieldsShouldBeNonStatic() throws NoSuchFieldException {
      String[] instanceFields = {"nativeInstance", "component", "config", "instanceId"};
      for (String fieldName : instanceFields) {
        Field field = JniComponentInstanceImpl.class.getDeclaredField(fieldName);
        assertFalse(
            Modifier.isStatic(field.getModifiers()), fieldName + " should be instance field");
      }
    }
  }
}
