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

package ai.tegmentum.wasmtime4j.panama.wasi.sockets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiNetwork} class.
 *
 * <p>PanamaWasiNetwork provides Panama FFI implementation of WASI network resource.
 */
@DisplayName("PanamaWasiNetwork Tests")
class PanamaWasiNetworkTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiNetwork.class.getModifiers()),
          "PanamaWasiNetwork should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiNetwork.class.getModifiers()),
          "PanamaWasiNetwork should be final");
    }

    @Test
    @DisplayName("should implement WasiNetwork interface")
    void shouldImplementWasiNetworkInterface() {
      assertTrue(
          WasiNetwork.class.isAssignableFrom(PanamaWasiNetwork.class),
          "PanamaWasiNetwork should implement WasiNetwork");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create static method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiNetwork.class.getMethod("create", MemorySegment.class);
      assertNotNull(method, "create method should exist");
      assertEquals(
          PanamaWasiNetwork.class, method.getReturnType(), "Should return PanamaWasiNetwork");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Handle Access Method Tests")
  class HandleAccessMethodTests {

    @Test
    @DisplayName("should have getNetworkHandle method")
    void shouldHaveGetNetworkHandleMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiNetwork.class.getMethod("getNetworkHandle");
      assertNotNull(method, "getNetworkHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiNetwork.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
