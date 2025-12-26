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

import ai.tegmentum.wasmtime4j.panama.util.PanamaResource;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaResource} class.
 *
 * <p>PanamaResource provides base class for managing native resources in Panama FFI implementations
 * with automatic cleanup via phantom references.
 */
@DisplayName("PanamaResource Tests")
class PanamaResourceTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and abstract")
    void shouldBePublicAndAbstract() {
      assertTrue(
          Modifier.isPublic(PanamaResource.class.getModifiers()), "PanamaResource should be public");
      assertTrue(
          Modifier.isAbstract(PanamaResource.class.getModifiers()),
          "PanamaResource should be abstract");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaResource.class),
          "PanamaResource should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have protected constructor with MemorySegment")
    void shouldHaveProtectedConstructorWithMemorySegment() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaResource.class.getDeclaredConstructor(MemorySegment.class);
      assertNotNull(constructor, "Constructor with MemorySegment should exist");
      assertTrue(
          Modifier.isProtected(constructor.getModifiers()), "Constructor should be protected");
    }
  }

  @Nested
  @DisplayName("Public Method Tests")
  class PublicMethodTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = PanamaResource.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertTrue(Modifier.isFinal(method.getModifiers()), "getNativeHandle should be final");
      assertEquals(
          MemorySegment.class, method.getReturnType(), "getNativeHandle should return MemorySegment");
    }

    @Test
    @DisplayName("should have getNativeLibrary static method")
    void shouldHaveGetNativeLibraryMethod() throws NoSuchMethodException {
      final Method method = PanamaResource.class.getMethod("getNativeLibrary");
      assertNotNull(method, "getNativeLibrary method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getNativeLibrary should be static");
      assertEquals(
          SymbolLookup.class, method.getReturnType(), "getNativeLibrary should return SymbolLookup");
    }

    @Test
    @DisplayName("should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      final Method method = PanamaResource.class.getMethod("isClosed");
      assertNotNull(method, "isClosed method should exist");
      assertTrue(Modifier.isFinal(method.getModifiers()), "isClosed should be final");
      assertEquals(boolean.class, method.getReturnType(), "isClosed should return boolean");
    }

    @Test
    @DisplayName("should have markClosedForTesting method")
    void shouldHaveMarkClosedForTestingMethod() throws NoSuchMethodException {
      final Method method = PanamaResource.class.getMethod("markClosedForTesting");
      assertNotNull(method, "markClosedForTesting method should exist");
      assertEquals(void.class, method.getReturnType(), "markClosedForTesting should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaResource.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertTrue(Modifier.isFinal(method.getModifiers()), "close should be final");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Protected Method Tests")
  class ProtectedMethodTests {

    @Test
    @DisplayName("should have ensureNotClosed protected method")
    void shouldHaveEnsureNotClosedMethod() throws NoSuchMethodException {
      final Method method = PanamaResource.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertTrue(
          Modifier.isProtected(method.getModifiers()), "ensureNotClosed should be protected");
      assertTrue(Modifier.isFinal(method.getModifiers()), "ensureNotClosed should be final");
      assertEquals(void.class, method.getReturnType(), "ensureNotClosed should return void");
    }

    @Test
    @DisplayName("should have doClose abstract method")
    void shouldHaveDoCloseAbstractMethod() throws NoSuchMethodException {
      final Method method = PanamaResource.class.getDeclaredMethod("doClose");
      assertNotNull(method, "doClose method should exist");
      assertTrue(Modifier.isProtected(method.getModifiers()), "doClose should be protected");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "doClose should be abstract");
      assertEquals(void.class, method.getReturnType(), "doClose should return void");
    }

    @Test
    @DisplayName("should have getResourceType abstract method")
    void shouldHaveGetResourceTypeAbstractMethod() throws NoSuchMethodException {
      final Method method = PanamaResource.class.getDeclaredMethod("getResourceType");
      assertNotNull(method, "getResourceType method should exist");
      assertTrue(Modifier.isProtected(method.getModifiers()), "getResourceType should be protected");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "getResourceType should be abstract");
      assertEquals(String.class, method.getReturnType(), "getResourceType should return String");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have nativeHandle protected field")
    void shouldHaveNativeHandleField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaResource.class.getDeclaredField("nativeHandle");
      assertNotNull(field, "nativeHandle field should exist");
      assertTrue(Modifier.isProtected(field.getModifiers()), "nativeHandle should be protected");
      assertTrue(Modifier.isFinal(field.getModifiers()), "nativeHandle should be final");
      assertEquals(
          MemorySegment.class, field.getType(), "nativeHandle should be of type MemorySegment");
    }
  }
}
