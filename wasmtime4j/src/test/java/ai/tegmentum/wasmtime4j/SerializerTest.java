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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Comprehensive test suite for the Serializer interface.
 *
 * <p>Serializer provides WebAssembly module serialization and deserialization capabilities.
 */
@DisplayName("Serializer Interface Tests")
class SerializerTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Serializer.class.isInterface(), "Serializer should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Serializer.class.getModifiers()), "Serializer should be public");
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
          Closeable.class.isAssignableFrom(Serializer.class), "Serializer should extend Closeable");
    }

    @Test
    @DisplayName("should have exactly 1 super interface")
    void shouldHaveExactly1SuperInterface() {
      assertEquals(1, Serializer.class.getInterfaces().length, "Should extend exactly 1 interface");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have serialize method")
    void shouldHaveSerializeMethod() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("serialize", Engine.class, byte[].class);
      assertNotNull(method, "serialize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("should have deserialize method")
    void shouldHaveDeserializeMethod() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("deserialize", Engine.class, byte[].class);
      assertNotNull(method, "deserialize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should have clearCache method")
    void shouldHaveClearCacheMethod() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("clearCache");
      assertNotNull(method, "clearCache method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getCacheEntryCount method")
    void shouldHaveGetCacheEntryCountMethod() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("getCacheEntryCount");
      assertNotNull(method, "getCacheEntryCount method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getCacheTotalSize method")
    void shouldHaveGetCacheTotalSizeMethod() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("getCacheTotalSize");
      assertNotNull(method, "getCacheTotalSize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCacheHitRate method")
    void shouldHaveGetCacheHitRateMethod() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("getCacheHitRate");
      assertNotNull(method, "getCacheHitRate method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have close method from Closeable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static create() method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("create");
      assertNotNull(method, "create() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Serializer.class, method.getReturnType(), "Should return Serializer");
    }

    @Test
    @DisplayName("should have static create(long, boolean, int) method")
    void shouldHaveStaticCreateWithConfigMethod() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("create", long.class, boolean.class, int.class);
      assertNotNull(method, "create(long, boolean, int) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Serializer.class, method.getReturnType(), "Should return Serializer");
      assertEquals(3, method.getParameterCount(), "Should have 3 parameters");
    }

    @Test
    @DisplayName("create(long, boolean, int) should have correct parameter types")
    void createWithConfigShouldHaveCorrectParams() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("create", long.class, boolean.class, int.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(long.class, paramTypes[0], "First param should be long (maxCacheSize)");
      assertEquals(
          boolean.class, paramTypes[1], "Second param should be boolean (enableCompression)");
      assertEquals(int.class, paramTypes[2], "Third param should be int (compressionLevel)");
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
              "serialize",
              "deserialize",
              "clearCache",
              "getCacheEntryCount",
              "getCacheTotalSize",
              "getCacheHitRate",
              "close");

      Set<String> actualAbstractMethods =
          Arrays.stream(Serializer.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedAbstractMethods) {
        assertTrue(actualAbstractMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 7 abstract methods")
    void shouldHaveExactly7AbstractMethods() {
      long abstractCount =
          Arrays.stream(Serializer.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(7, abstractCount, "Should have exactly 7 abstract methods");
    }

    @Test
    @DisplayName("should have exactly 2 static methods")
    void shouldHaveExactly2StaticMethods() {
      long staticCount =
          Arrays.stream(Serializer.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(2, staticCount, "Should have exactly 2 static factory methods");
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
          Serializer.class.getDeclaredClasses().length,
          "Serializer should have no nested classes");
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
          Arrays.stream(Serializer.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultCount, "Should have no default methods");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("serialize should declare WasmException")
    void serializeShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("serialize", Engine.class, byte[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "serialize should declare WasmException");
    }

    @Test
    @DisplayName("deserialize should declare WasmException")
    void deserializeShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("deserialize", Engine.class, byte[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "deserialize should declare WasmException");
    }

    @Test
    @DisplayName("clearCache should declare WasmException")
    void clearCacheShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("clearCache");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "clearCache should declare WasmException");
    }

    @Test
    @DisplayName("create() should declare WasmException")
    void createShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("create");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "create() should declare WasmException");
    }

    @Test
    @DisplayName("create(long, boolean, int) should declare WasmException")
    void createWithConfigShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Serializer.class.getMethod("create", long.class, boolean.class, int.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "create(long, boolean, int) should declare WasmException");
    }
  }
}
