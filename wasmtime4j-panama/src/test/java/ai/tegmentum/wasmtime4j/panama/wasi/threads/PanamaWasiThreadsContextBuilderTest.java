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

package ai.tegmentum.wasmtime4j.panama.wasi.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContextBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiThreadsContextBuilder} class.
 *
 * <p>PanamaWasiThreadsContextBuilder provides builder for WASI-Threads context.
 */
@DisplayName("PanamaWasiThreadsContextBuilder Tests")
class PanamaWasiThreadsContextBuilderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiThreadsContextBuilder.class.getModifiers()),
          "PanamaWasiThreadsContextBuilder should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiThreadsContextBuilder.class.getModifiers()),
          "PanamaWasiThreadsContextBuilder should be final");
    }

    @Test
    @DisplayName("should implement WasiThreadsContextBuilder interface")
    void shouldImplementWasiThreadsContextBuilderInterface() {
      assertTrue(
          WasiThreadsContextBuilder.class.isAssignableFrom(PanamaWasiThreadsContextBuilder.class),
          "PanamaWasiThreadsContextBuilder should implement WasiThreadsContextBuilder");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = PanamaWasiThreadsContextBuilder.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Module Configuration Method Tests")
  class ModuleConfigurationMethodTests {

    @Test
    @DisplayName("should have withModule method")
    void shouldHaveWithModuleMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiThreadsContextBuilder.class.getMethod("withModule", Module.class);
      assertNotNull(method, "withModule method should exist");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder");
    }
  }

  @Nested
  @DisplayName("Linker Configuration Method Tests")
  class LinkerConfigurationMethodTests {

    @Test
    @DisplayName("should have withLinker method")
    void shouldHaveWithLinkerMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiThreadsContextBuilder.class.getMethod("withLinker", Linker.class);
      assertNotNull(method, "withLinker method should exist");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder");
    }
  }

  @Nested
  @DisplayName("Store Configuration Method Tests")
  class StoreConfigurationMethodTests {

    @Test
    @DisplayName("should have withStore method")
    void shouldHaveWithStoreMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiThreadsContextBuilder.class.getMethod("withStore", Store.class);
      assertNotNull(method, "withStore method should exist");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder");
    }
  }

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsContextBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          WasiThreadsContext.class, method.getReturnType(), "Should return WasiThreadsContext");
    }
  }
}
