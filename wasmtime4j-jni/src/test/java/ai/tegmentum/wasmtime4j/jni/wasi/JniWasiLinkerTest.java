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
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiLinker;
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
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiLinker.class.getModifiers()), "JniWasiLinker should be final");
    }

    @Test
    @DisplayName("should implement WasiLinker")
    void shouldImplementWasiLinker() {
      assertTrue(
          WasiLinker.class.isAssignableFrom(JniWasiLinker.class),
          "JniWasiLinker should implement WasiLinker");
    }

    @Test
    @DisplayName("should have constructor with long, JniEngine, and WasiConfig parameters")
    void shouldHaveConstructorWithParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          JniWasiLinker.class.getConstructor(long.class, JniEngine.class, WasiConfig.class);
      assertNotNull(constructor, "Should have constructor with long, JniEngine, WasiConfig");
    }
  }

  @Nested
  @DisplayName("WASI Methods Tests")
  class WasiMethodsTests {

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() {
      boolean hasInstantiate = false;
      for (final Method method : JniWasiLinker.class.getDeclaredMethods()) {
        if (method.getName().equals("instantiate")) {
          hasInstantiate = true;
          break;
        }
      }
      assertTrue(hasInstantiate, "JniWasiLinker should have instantiate method");
    }

    @Test
    @DisplayName("should have allowDirectoryAccess method")
    void shouldHaveAllowDirectoryAccessMethod() {
      boolean hasMethod = false;
      for (final Method method : JniWasiLinker.class.getDeclaredMethods()) {
        if (method.getName().equals("allowDirectoryAccess")) {
          hasMethod = true;
          break;
        }
      }
      assertTrue(hasMethod, "JniWasiLinker should have allowDirectoryAccess method");
    }

    @Test
    @DisplayName("should have setEnvironmentVariable method")
    void shouldHaveSetEnvironmentVariableMethod() throws NoSuchMethodException {
      assertNotNull(
          JniWasiLinker.class.getMethod("setEnvironmentVariable", String.class, String.class),
          "Should have setEnvironmentVariable method");
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
  @DisplayName("Accessor Tests")
  class AccessorTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiLinker.class.getMethod("isValid"), "Should have isValid method");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiLinker.class.getMethod("getEngine"), "Should have getEngine method");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiLinker.class.getMethod("getConfig"), "Should have getConfig method");
    }

    @Test
    @DisplayName("should have getLinker method")
    void shouldHaveGetLinkerMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiLinker.class.getMethod("getLinker"), "Should have getLinker method");
    }
  }
}
