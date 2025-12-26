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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniLibraryLoader} class.
 *
 * <p>JniLibraryLoader provides utility methods for loading native JNI libraries.
 */
@DisplayName("JniLibraryLoader Tests")
class JniLibraryLoaderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(JniLibraryLoader.class.getModifiers()),
          "JniLibraryLoader should be public");
      assertTrue(
          Modifier.isFinal(JniLibraryLoader.class.getModifiers()),
          "JniLibraryLoader should be final");
    }

    @Test
    @DisplayName("should have private constructor (utility class)")
    void shouldHavePrivateConstructor() {
      final Constructor<?>[] constructors = JniLibraryLoader.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly one constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have ensureLoaded static method")
    void shouldHaveEnsureLoadedStaticMethod() throws NoSuchMethodException {
      final Method method = JniLibraryLoader.class.getMethod("ensureLoaded");
      assertNotNull(method, "ensureLoaded method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ensureLoaded should be static");
      assertEquals(void.class, method.getReturnType(), "ensureLoaded should return void");
    }

    @Test
    @DisplayName("should have isLoaded static method")
    void shouldHaveIsLoadedStaticMethod() throws NoSuchMethodException {
      final Method method = JniLibraryLoader.class.getMethod("isLoaded");
      assertNotNull(method, "isLoaded method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isLoaded should be static");
      assertEquals(boolean.class, method.getReturnType(), "isLoaded should return boolean");
    }
  }
}
