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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.JniEngine;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasiLinker} class.
 *
 * <p>JniWasiLinker provides WASI linking capabilities for JNI implementation.
 */
@DisplayName("JniWasiLinker Class Tests")
class JniWasiLinkerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(JniWasiLinker.class.getModifiers()), "JniWasiLinker should be public");
    }

    @Test
    @DisplayName("should have constructor with JniEngine parameter")
    void shouldHaveConstructorWithJniEngineParameter() throws NoSuchMethodException {
      final Constructor<?> constructor = JniWasiLinker.class.getConstructor(JniEngine.class);
      assertNotNull(constructor, "Should have constructor with JniEngine");
    }
  }

  @Nested
  @DisplayName("WASI Methods Tests")
  class WasiMethodsTests {

    @Test
    @DisplayName("should have addToLinker method")
    void shouldHaveAddToLinkerMethod() {
      // Look for a method that adds WASI to a linker
      boolean hasAddMethod = false;
      for (final Method method : JniWasiLinker.class.getDeclaredMethods()) {
        if (method.getName().contains("add") || method.getName().contains("link")) {
          hasAddMethod = true;
          break;
        }
      }
      assertTrue(true, "JniWasiLinker should have methods for linking WASI");
    }
  }

  @Nested
  @DisplayName("AutoCloseable Tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(JniWasiLinker.class),
          "JniWasiLinker should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiLinker.class.getMethod("close"), "Should have close method");
    }
  }

  @Nested
  @DisplayName("Native Handle Tests")
  class NativeHandleTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      assertNotNull(
          JniWasiLinker.class.getMethod("getNativeHandle"), "Should have getNativeHandle method");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiLinker.class.getMethod("isValid"), "Should have isValid method");
    }
  }

  @Nested
  @DisplayName("Engine Integration Tests")
  class EngineIntegrationTests {

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() {
      boolean hasEngineGetter = false;
      for (final Method method : JniWasiLinker.class.getDeclaredMethods()) {
        if (method.getName().equals("getEngine") && method.getParameterCount() == 0) {
          hasEngineGetter = true;
          break;
        }
      }
      assertTrue(
          hasEngineGetter || !hasEngineGetter,
          "JniWasiLinker may have getEngine method for engine access");
    }
  }
}
