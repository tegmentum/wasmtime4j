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

package ai.tegmentum.wasmtime4j.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ModuleCache interface.
 *
 * <p>ModuleCache provides persistent caching for compiled WebAssembly modules.
 */
@DisplayName("ModuleCache Interface Tests")
class ModuleCacheTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ModuleCache.class.isInterface(), "ModuleCache should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ModuleCache.class.getModifiers()), "ModuleCache should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(ModuleCache.class),
          "ModuleCache should extend Closeable");
    }

    @Test
    @DisplayName("should have exactly 1 super interface")
    void shouldHaveExactly1SuperInterface() {
      assertEquals(
          1, ModuleCache.class.getInterfaces().length, "Should extend exactly 1 interface");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getOrCompile method")
    void shouldHaveGetOrCompileMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("getOrCompile", byte[].class);
      assertNotNull(method, "getOrCompile method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should have precompile method")
    void shouldHavePrecompileMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("precompile", byte[].class);
      assertNotNull(method, "precompile method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(String.class, method.getReturnType(), "Should return String (cache key)");
    }

    @Test
    @DisplayName("should have contains method")
    void shouldHaveContainsMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("contains", byte[].class);
      assertNotNull(method, "contains method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have remove method")
    void shouldHaveRemoveMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("remove", byte[].class);
      assertNotNull(method, "remove method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have performMaintenance method")
    void shouldHavePerformMaintenanceMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("performMaintenance");
      assertNotNull(method, "performMaintenance method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          ModuleCacheStatistics.class,
          method.getReturnType(),
          "Should return ModuleCacheStatistics");
    }

    @Test
    @DisplayName("should have getEntryCount method")
    void shouldHaveGetEntryCountMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("getEntryCount");
      assertNotNull(method, "getEntryCount method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStorageBytesUsed method")
    void shouldHaveGetStorageBytesUsedMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("getStorageBytesUsed");
      assertNotNull(method, "getStorageBytesUsed method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getHitCount method")
    void shouldHaveGetHitCountMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("getHitCount");
      assertNotNull(method, "getHitCount method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMissCount method")
    void shouldHaveGetMissCountMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("getMissCount");
      assertNotNull(method, "getMissCount method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          ModuleCacheConfig.class, method.getReturnType(), "Should return ModuleCacheConfig");
    }

    @Test
    @DisplayName("should have close method from Closeable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected abstract methods")
    void shouldHaveAllExpectedAbstractMethods() {
      Set<String> expectedAbstractMethods =
          Set.of(
              "getOrCompile",
              "precompile",
              "contains",
              "remove",
              "clear",
              "performMaintenance",
              "getStatistics",
              "getEntryCount",
              "getStorageBytesUsed",
              "getHitCount",
              "getMissCount",
              "getEngine",
              "getConfig",
              "close");

      Set<String> actualAbstractMethods =
          Arrays.stream(ModuleCache.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedAbstractMethods) {
        assertTrue(actualAbstractMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 14 abstract methods")
    void shouldHaveExactly14AbstractMethods() {
      long abstractCount =
          Arrays.stream(ModuleCache.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(14, abstractCount, "Should have exactly 14 abstract methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultCount =
          Arrays.stream(ModuleCache.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultCount, "Should have no default methods");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticCount =
          Arrays.stream(ModuleCache.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticCount, "Should have no static methods");
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
          ModuleCache.class.getDeclaredClasses().length,
          "ModuleCache should have no nested classes");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("getOrCompile should declare WasmException")
    void getOrCompileShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("getOrCompile", byte[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "getOrCompile should declare WasmException");
    }

    @Test
    @DisplayName("precompile should declare WasmException")
    void precompileShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("precompile", byte[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "precompile should declare WasmException");
    }

    @Test
    @DisplayName("clear should declare WasmException")
    void clearShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("clear");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "clear should declare WasmException");
    }

    @Test
    @DisplayName("performMaintenance should declare WasmException")
    void performMaintenanceShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = ModuleCache.class.getMethod("performMaintenance");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "performMaintenance should declare WasmException");
    }
  }
}
