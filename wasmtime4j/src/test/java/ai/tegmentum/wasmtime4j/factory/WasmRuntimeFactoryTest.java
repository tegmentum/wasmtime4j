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

package ai.tegmentum.wasmtime4j.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmRuntimeFactory} class.
 *
 * <p>WasmRuntimeFactory provides factory methods for creating WebAssembly runtime instances.
 */
@DisplayName("WasmRuntimeFactory Class Tests")
class WasmRuntimeFactoryTest {

  @BeforeEach
  void setUp() {
    // Clear cache before each test
    WasmRuntimeFactory.clearCache();
  }

  @AfterEach
  void tearDown() {
    // Clear any system properties that were set
    System.clearProperty(WasmRuntimeFactory.RUNTIME_PROPERTY);
    WasmRuntimeFactory.clearCache();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasmRuntimeFactory.class.getModifiers()),
          "WasmRuntimeFactory should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<WasmRuntimeFactory> constructor =
          WasmRuntimeFactory.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("should have RUNTIME_PROPERTY constant")
    void shouldHaveRuntimePropertyConstant() throws NoSuchFieldException {
      assertEquals(
          "wasmtime4j.runtime",
          WasmRuntimeFactory.RUNTIME_PROPERTY,
          "RUNTIME_PROPERTY should be 'wasmtime4j.runtime'");
    }

    @Test
    @DisplayName("should have RUNTIME_JNI constant")
    void shouldHaveRuntimeJniConstant() throws NoSuchFieldException {
      assertEquals("jni", WasmRuntimeFactory.RUNTIME_JNI, "RUNTIME_JNI should be 'jni'");
    }

    @Test
    @DisplayName("should have RUNTIME_PANAMA constant")
    void shouldHaveRuntimePanamaConstant() throws NoSuchFieldException {
      assertEquals(
          "panama", WasmRuntimeFactory.RUNTIME_PANAMA, "RUNTIME_PANAMA should be 'panama'");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have static create method without parameters")
    void shouldHaveStaticCreateMethodWithoutParameters() throws NoSuchMethodException {
      final Method method = WasmRuntimeFactory.class.getMethod("create");
      assertNotNull(method, "create() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
    }

    @Test
    @DisplayName("should have static create method with RuntimeType parameter")
    void shouldHaveStaticCreateMethodWithRuntimeTypeParameter() throws NoSuchMethodException {
      final Method method = WasmRuntimeFactory.class.getMethod("create", RuntimeType.class);
      assertNotNull(method, "create(RuntimeType) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
    }

    @Test
    @DisplayName("should have static getSelectedRuntimeType method")
    void shouldHaveStaticGetSelectedRuntimeTypeMethod() throws NoSuchMethodException {
      final Method method = WasmRuntimeFactory.class.getMethod("getSelectedRuntimeType");
      assertNotNull(method, "getSelectedRuntimeType method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "getSelectedRuntimeType should be static");
      assertEquals(RuntimeType.class, method.getReturnType(), "Should return RuntimeType");
    }

    @Test
    @DisplayName("should have static isRuntimeAvailable method")
    void shouldHaveStaticIsRuntimeAvailableMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntimeFactory.class.getMethod("isRuntimeAvailable", RuntimeType.class);
      assertNotNull(method, "isRuntimeAvailable method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isRuntimeAvailable should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have static getJavaVersion method")
    void shouldHaveStaticGetJavaVersionMethod() throws NoSuchMethodException {
      final Method method = WasmRuntimeFactory.class.getMethod("getJavaVersion");
      assertNotNull(method, "getJavaVersion method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getJavaVersion should be static");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have static clearCache method")
    void shouldHaveStaticClearCacheMethod() throws NoSuchMethodException {
      final Method method = WasmRuntimeFactory.class.getMethod("clearCache");
      assertNotNull(method, "clearCache method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "clearCache should be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("getJavaVersion Tests")
  class GetJavaVersionTests {

    @Test
    @DisplayName("should return positive version number")
    void shouldReturnPositiveVersionNumber() {
      final int version = WasmRuntimeFactory.getJavaVersion();
      assertTrue(version > 0, "Java version should be positive");
    }

    @Test
    @DisplayName("should return at least Java 8")
    void shouldReturnAtLeastJava8() {
      final int version = WasmRuntimeFactory.getJavaVersion();
      assertTrue(version >= 8, "Java version should be at least 8");
    }
  }

  @Nested
  @DisplayName("getSelectedRuntimeType Tests")
  class GetSelectedRuntimeTypeTests {

    @Test
    @DisplayName("should return non-null RuntimeType")
    void shouldReturnNonNullRuntimeType() {
      final RuntimeType runtimeType = WasmRuntimeFactory.getSelectedRuntimeType();
      assertNotNull(runtimeType, "Selected runtime type should not be null");
    }

    @Test
    @DisplayName("should return JNI or PANAMA")
    void shouldReturnJniOrPanama() {
      final RuntimeType runtimeType = WasmRuntimeFactory.getSelectedRuntimeType();
      assertTrue(
          runtimeType == RuntimeType.JNI || runtimeType == RuntimeType.PANAMA,
          "Should return either JNI or PANAMA");
    }
  }

  @Nested
  @DisplayName("isRuntimeAvailable Tests")
  class IsRuntimeAvailableTests {

    @Test
    @DisplayName("should return boolean for JNI")
    void shouldReturnBooleanForJni() {
      final boolean available = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      // The result could be true or false depending on environment
      assertNotNull(available);
    }

    @Test
    @DisplayName("should return boolean for PANAMA")
    void shouldReturnBooleanForPanama() {
      final boolean available = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
      // The result could be true or false depending on environment
      assertNotNull(available);
    }
  }

  @Nested
  @DisplayName("System Property Override Tests")
  class SystemPropertyOverrideTests {

    @Test
    @DisplayName("setting JNI property should select JNI runtime")
    void settingJniPropertyShouldSelectJniRuntime() {
      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "jni");
      WasmRuntimeFactory.clearCache();

      final RuntimeType selected = WasmRuntimeFactory.getSelectedRuntimeType();
      assertEquals(
          RuntimeType.JNI, selected, "Should select JNI runtime when property is set to 'jni'");
    }

    @Test
    @DisplayName("setting PANAMA property should select PANAMA runtime")
    void settingPanamaPropertyShouldSelectPanamaRuntime() {
      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "panama");
      WasmRuntimeFactory.clearCache();

      final RuntimeType selected = WasmRuntimeFactory.getSelectedRuntimeType();
      assertEquals(
          RuntimeType.PANAMA,
          selected,
          "Should select PANAMA runtime when property is set to 'panama'");
    }

    @Test
    @DisplayName("property should be case insensitive")
    void propertyShouldBeCaseInsensitive() {
      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "JNI");
      WasmRuntimeFactory.clearCache();

      final RuntimeType selected = WasmRuntimeFactory.getSelectedRuntimeType();
      assertEquals(RuntimeType.JNI, selected, "Should handle uppercase property value");
    }
  }

  @Nested
  @DisplayName("create Method Tests")
  class CreateMethodTests {

    @Test
    @DisplayName("create with null RuntimeType should throw IllegalArgumentException")
    void createWithNullRuntimeTypeShouldThrowIllegalArgumentException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmRuntimeFactory.create(null),
          "Should throw IllegalArgumentException for null RuntimeType");
    }
  }

  @Nested
  @DisplayName("clearCache Tests")
  class ClearCacheTests {

    @Test
    @DisplayName("clearCache should not throw")
    void clearCacheShouldNotThrow() {
      // Should complete without exception
      WasmRuntimeFactory.clearCache();
    }

    @Test
    @DisplayName("clearCache should allow re-evaluation of runtime type")
    void clearCacheShouldAllowReEvaluationOfRuntimeType() {
      // Get initial runtime type
      final RuntimeType initial = WasmRuntimeFactory.getSelectedRuntimeType();

      // Clear cache
      WasmRuntimeFactory.clearCache();

      // Get runtime type again (should re-evaluate)
      final RuntimeType afterClear = WasmRuntimeFactory.getSelectedRuntimeType();

      // Both should be the same since environment hasn't changed
      assertEquals(
          initial, afterClear, "Runtime type should be same after clear in same environment");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("create without parameters should declare WasmException")
    void createWithoutParametersShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasmRuntimeFactory.class.getMethod("create");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "create() method should declare WasmException");
    }

    @Test
    @DisplayName("create with RuntimeType should declare WasmException")
    void createWithRuntimeTypeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasmRuntimeFactory.class.getMethod("create", RuntimeType.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "create(RuntimeType) method should declare WasmException");
    }
  }
}
