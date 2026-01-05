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

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.TagType;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaTag} class.
 *
 * <p>PanamaTag wraps a native Wasmtime tag for exception handling.
 */
@DisplayName("PanamaTag Tests")
class PanamaTagTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(Modifier.isPublic(PanamaTag.class.getModifiers()), "PanamaTag should be public");
      assertTrue(Modifier.isFinal(PanamaTag.class.getModifiers()), "PanamaTag should be final");
    }

    @Test
    @DisplayName("should implement Tag interface")
    void shouldImplementTagInterface() {
      assertTrue(Tag.class.isAssignableFrom(PanamaTag.class), "PanamaTag should implement Tag");
    }
  }

  @Nested
  @DisplayName("Tag Method Tests")
  class TagMethodTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaTag.class.getMethod("getType", Store.class);
      assertNotNull(method, "getType method should exist");
      assertEquals(TagType.class, method.getReturnType(), "Should return TagType");
    }

    @Test
    @DisplayName("should have equals method with Tag and Store")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = PanamaTag.class.getMethod("equals", Tag.class, Store.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = PanamaTag.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Internal Method Tests")
  class InternalMethodTests {

    @Test
    @DisplayName("should have getNativeSegment method")
    void shouldHaveGetNativeSegmentMethod() throws NoSuchMethodException {
      final Method method = PanamaTag.class.getDeclaredMethod("getNativeSegment");
      assertNotNull(method, "getNativeSegment method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaTag.class.getMethod("close");
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
      for (var constructor : PanamaTag.class.getDeclaredConstructors()) {
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
