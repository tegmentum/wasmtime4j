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

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaInstanceGlobal} class.
 *
 * <p>PanamaInstanceGlobal is a WebAssembly Global bound to an instance export.
 */
@DisplayName("PanamaInstanceGlobal Tests")
class PanamaInstanceGlobalTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      assertTrue(
          !Modifier.isPublic(PanamaInstanceGlobal.class.getModifiers()),
          "PanamaInstanceGlobal should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaInstanceGlobal.class.getModifiers()),
          "PanamaInstanceGlobal should be final");
    }

    @Test
    @DisplayName("should implement WasmGlobal interface")
    void shouldImplementWasmGlobalInterface() {
      assertTrue(
          WasmGlobal.class.isAssignableFrom(PanamaInstanceGlobal.class),
          "PanamaInstanceGlobal should implement WasmGlobal");
    }

    @Test
    @DisplayName("should implement AutoCloseable interface")
    void shouldImplementAutoCloseableInterface() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaInstanceGlobal.class),
          "PanamaInstanceGlobal should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("WasmGlobal Method Tests")
  class WasmGlobalMethodTests {

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("get");
      assertNotNull(method, "get method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "Should return WasmValue");
    }

    @Test
    @DisplayName("should have set method")
    void shouldHaveSetMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("set", WasmValue.class);
      assertNotNull(method, "set method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(WasmValueType.class, method.getReturnType(), "Should return WasmValueType");
    }

    @Test
    @DisplayName("should have isMutable method")
    void shouldHaveIsMutableMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getGlobalType method")
    void shouldHaveGetGlobalTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("getGlobalType");
      assertNotNull(method, "getGlobalType method should exist");
      assertEquals(GlobalType.class, method.getReturnType(), "Should return GlobalType");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with 5 parameters")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaInstanceGlobal.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 5
            && constructor.getParameterTypes()[0] == PanamaInstance.class
            && constructor.getParameterTypes()[1] == PanamaStore.class
            && constructor.getParameterTypes()[2] == String.class
            && constructor.getParameterTypes()[3] == WasmValueType.class
            && constructor.getParameterTypes()[4] == boolean.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(
          hasExpectedConstructor,
          "Should have constructor with PanamaInstance, PanamaStore, String, WasmValueType,"
              + " boolean");
    }
  }
}
