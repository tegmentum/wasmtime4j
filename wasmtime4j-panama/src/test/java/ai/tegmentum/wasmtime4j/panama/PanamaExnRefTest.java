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

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.memory.Tag;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaExnRef} class.
 *
 * <p>PanamaExnRef wraps a native Wasmtime exception reference for exception handling.
 */
@DisplayName("PanamaExnRef Tests")
class PanamaExnRefTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaExnRef.class.getModifiers()), "PanamaExnRef should be public");
      assertTrue(
          Modifier.isFinal(PanamaExnRef.class.getModifiers()), "PanamaExnRef should be final");
    }

    @Test
    @DisplayName("should implement ExnRef interface")
    void shouldImplementExnRefInterface() {
      assertTrue(
          ExnRef.class.isAssignableFrom(PanamaExnRef.class),
          "PanamaExnRef should implement ExnRef");
    }
  }

  @Nested
  @DisplayName("ExnRef Method Tests")
  class ExnRefMethodTests {

    @Test
    @DisplayName("should have getTag method")
    void shouldHaveGetTagMethod() throws NoSuchMethodException {
      final Method method = PanamaExnRef.class.getMethod("getTag", Store.class);
      assertNotNull(method, "getTag method should exist");
      assertEquals(Tag.class, method.getReturnType(), "Should return Tag");
    }

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = PanamaExnRef.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaExnRef.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Internal Method Tests")
  class InternalMethodTests {

    @Test
    @DisplayName("should have getNativeSegment method")
    void shouldHaveGetNativeSegmentMethod() throws NoSuchMethodException {
      final Method method = PanamaExnRef.class.getDeclaredMethod("getNativeSegment");
      assertNotNull(method, "getNativeSegment method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaExnRef.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with 2 MemorySegment parameters")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaExnRef.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 2
            && constructor.getParameterTypes()[0] == MemorySegment.class
            && constructor.getParameterTypes()[1] == MemorySegment.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(
          hasExpectedConstructor, "Should have constructor with two MemorySegment parameters");
    }
  }
}
