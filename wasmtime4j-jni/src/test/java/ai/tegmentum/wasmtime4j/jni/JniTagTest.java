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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniTag} class.
 *
 * <p>JniTag provides JNI implementation of WebAssembly exception tags.
 */
@DisplayName("JniTag Tests")
class JniTagTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(Modifier.isPublic(JniTag.class.getModifiers()), "JniTag should be public");
      assertTrue(Modifier.isFinal(JniTag.class.getModifiers()), "JniTag should be final");
    }

    @Test
    @DisplayName("should implement Tag interface")
    void shouldImplementTagInterface() {
      assertTrue(Tag.class.isAssignableFrom(JniTag.class), "JniTag should implement Tag");
    }

    @Test
    @DisplayName("should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniTag.class), "JniTag should extend JniResource");
    }
  }

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = JniTag.class.getMethod("getType", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "getType method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.type.TagType.class,
          method.getReturnType(),
          "getType should return TagType");
    }

    @Test
    @DisplayName("should have equals method with Tag and Store parameters")
    void shouldHaveEqualsMethodWithTagAndStore() throws NoSuchMethodException {
      final Method method =
          JniTag.class.getMethod("equals", Tag.class, ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "equals should return boolean");
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should have inherited close method")
    void shouldHaveInheritedCloseMethod() throws NoSuchMethodException {
      final Method method = JniTag.class.getMethod("close");
      assertNotNull(method, "close method should exist (inherited from JniResource)");
    }

    @Test
    @DisplayName("should have inherited isClosed method")
    void shouldHaveInheritedIsClosedMethod() throws NoSuchMethodException {
      final Method method = JniTag.class.getMethod("isClosed");
      assertNotNull(method, "isClosed method should exist (inherited from JniResource)");
    }
  }
}
