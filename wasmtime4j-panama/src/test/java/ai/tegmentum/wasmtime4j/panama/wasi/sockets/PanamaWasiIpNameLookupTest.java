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

import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.ResolveAddressStream;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiIpNameLookup;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiIpNameLookup} class.
 *
 * <p>PanamaWasiIpNameLookup provides Panama FFI implementation of DNS name resolution.
 */
@DisplayName("PanamaWasiIpNameLookup Tests")
class PanamaWasiIpNameLookupTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiIpNameLookup.class.getModifiers()),
          "PanamaWasiIpNameLookup should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiIpNameLookup.class.getModifiers()),
          "PanamaWasiIpNameLookup should be final");
    }

    @Test
    @DisplayName("should implement WasiIpNameLookup interface")
    void shouldImplementWasiIpNameLookupInterface() {
      assertTrue(
          WasiIpNameLookup.class.isAssignableFrom(PanamaWasiIpNameLookup.class),
          "PanamaWasiIpNameLookup should implement WasiIpNameLookup");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with MemorySegment")
    void shouldHaveContextConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaWasiIpNameLookup.class.getConstructor(MemorySegment.class);
      assertNotNull(constructor, "Constructor with MemorySegment should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Resolve Addresses Method Tests")
  class ResolveAddressesMethodTests {

    @Test
    @DisplayName("should have resolveAddresses method with network and name")
    void shouldHaveResolveAddressesMethodWithNameOnly() throws NoSuchMethodException {
      final Method method =
          PanamaWasiIpNameLookup.class.getMethod(
              "resolveAddresses", WasiNetwork.class, String.class);
      assertNotNull(method, "resolveAddresses(network, name) method should exist");
      assertEquals(
          ResolveAddressStream.class, method.getReturnType(), "Should return ResolveAddressStream");
    }

    @Test
    @DisplayName("should have resolveAddresses method with network, name and address family")
    void shouldHaveResolveAddressesMethodWithFamily() throws NoSuchMethodException {
      final Method method =
          PanamaWasiIpNameLookup.class.getMethod(
              "resolveAddresses", WasiNetwork.class, String.class, IpAddressFamily.class);
      assertNotNull(method, "resolveAddresses(network, name, family) method should exist");
      assertEquals(
          ResolveAddressStream.class, method.getReturnType(), "Should return ResolveAddressStream");
    }
  }
}
