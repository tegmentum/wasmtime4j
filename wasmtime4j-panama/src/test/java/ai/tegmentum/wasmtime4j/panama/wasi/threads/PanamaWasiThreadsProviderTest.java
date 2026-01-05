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
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContextBuilder;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiThreadsProvider} class.
 *
 * <p>PanamaWasiThreadsProvider provides Panama FFI implementation of WASI-Threads provider.
 */
@DisplayName("PanamaWasiThreadsProvider Tests")
class PanamaWasiThreadsProviderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiThreadsProvider.class.getModifiers()),
          "PanamaWasiThreadsProvider should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiThreadsProvider.class.getModifiers()),
          "PanamaWasiThreadsProvider should be final");
    }

    @Test
    @DisplayName("should implement WasiThreadsProvider interface")
    void shouldImplementWasiThreadsProviderInterface() {
      assertTrue(
          WasiThreadsProvider.class.isAssignableFrom(PanamaWasiThreadsProvider.class),
          "PanamaWasiThreadsProvider should implement WasiThreadsProvider");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor for ServiceLoader")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = PanamaWasiThreadsProvider.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist for ServiceLoader");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Availability Method Tests")
  class AvailabilityMethodTests {

    @Test
    @DisplayName("should have isAvailable method")
    void shouldHaveIsAvailableMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsProvider.class.getMethod("isAvailable");
      assertNotNull(method, "isAvailable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Builder Factory Method Tests")
  class BuilderFactoryMethodTests {

    @Test
    @DisplayName("should have createBuilder method")
    void shouldHaveCreateBuilderMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsProvider.class.getMethod("createBuilder");
      assertNotNull(method, "createBuilder method should exist");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder");
    }
  }

  @Nested
  @DisplayName("Linker Integration Method Tests")
  class LinkerIntegrationMethodTests {

    @Test
    @DisplayName("should have addToLinker method")
    void shouldHaveAddToLinkerMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiThreadsProvider.class.getMethod(
              "addToLinker", Linker.class, Store.class, Module.class);
      assertNotNull(method, "addToLinker method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
