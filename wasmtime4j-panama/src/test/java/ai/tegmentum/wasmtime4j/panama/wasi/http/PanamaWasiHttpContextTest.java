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

package ai.tegmentum.wasmtime4j.panama.wasi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpContext;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpStats;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiHttpContext} class.
 *
 * <p>PanamaWasiHttpContext provides Panama implementation of WASI HTTP context.
 */
@DisplayName("PanamaWasiHttpContext Tests")
class PanamaWasiHttpContextTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiHttpContext.class.getModifiers()),
          "PanamaWasiHttpContext should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiHttpContext.class.getModifiers()),
          "PanamaWasiHttpContext should be final");
    }

    @Test
    @DisplayName("should implement WasiHttpContext interface")
    void shouldImplementWasiHttpContextInterface() {
      assertTrue(
          WasiHttpContext.class.isAssignableFrom(PanamaWasiHttpContext.class),
          "PanamaWasiHttpContext should implement WasiHttpContext");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with WasiHttpConfig")
    void shouldHaveConfigConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaWasiHttpContext.class.getConstructor(WasiHttpConfig.class);
      assertNotNull(constructor, "Constructor with WasiHttpConfig should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Linker Integration Method Tests")
  class LinkerIntegrationMethodTests {

    @Test
    @DisplayName("should have addToLinker method")
    void shouldHaveAddToLinkerMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpContext.class.getMethod("addToLinker", Linker.class, Store.class);
      assertNotNull(method, "addToLinker method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Config Method Tests")
  class ConfigMethodTests {

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpContext.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(WasiHttpConfig.class, method.getReturnType(), "Should return WasiHttpConfig");
    }
  }

  @Nested
  @DisplayName("Stats Method Tests")
  class StatsMethodTests {

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpContext.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(WasiHttpStats.class, method.getReturnType(), "Should return WasiHttpStats");
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpContext.class.getMethod("resetStats");
      assertNotNull(method, "resetStats method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isHostAllowed method")
    void shouldHaveIsHostAllowedMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpContext.class.getMethod("isHostAllowed", String.class);
      assertNotNull(method, "isHostAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpContext.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
