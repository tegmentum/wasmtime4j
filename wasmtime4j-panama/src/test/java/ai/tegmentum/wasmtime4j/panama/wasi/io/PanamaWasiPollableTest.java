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

package ai.tegmentum.wasmtime4j.panama.wasi.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiPollable} class.
 *
 * <p>PanamaWasiPollable is a Panama FFI implementation of the WasiPollable interface for event
 * notification and blocking operations on streams.
 */
@DisplayName("PanamaWasiPollable Tests")
class PanamaWasiPollableTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiPollable.class.getModifiers()),
          "PanamaWasiPollable should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiPollable.class.getModifiers()),
          "PanamaWasiPollable should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiPollable.class),
          "PanamaWasiPollable should implement AutoCloseable");
    }

    @Test
    @DisplayName("should implement WasiPollable interface")
    void shouldImplementWasiPollableInterface() {
      assertTrue(
          WasiPollable.class.isAssignableFrom(PanamaWasiPollable.class),
          "PanamaWasiPollable should implement WasiPollable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with context and pollable handles")
    void shouldHaveConstructorWithHandles() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaWasiPollable.class.getConstructor(MemorySegment.class, MemorySegment.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Blocking Method Tests")
  class BlockingMethodTests {

    @Test
    @DisplayName("should have block method")
    void shouldHaveBlockMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiPollable.class.getMethod("block");
      assertNotNull(method, "block method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have ready method")
    void shouldHaveReadyMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiPollable.class.getMethod("ready");
      assertNotNull(method, "ready method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Identification Method Tests")
  class IdentificationMethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiPollable.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiPollable.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Ownership Method Tests")
  class OwnershipMethodTests {

    @Test
    @DisplayName("should have isOwned method")
    void shouldHaveIsOwnedMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiPollable.class.getMethod("isOwned");
      assertNotNull(method, "isOwned method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("State Method Tests")
  class StateMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiPollable.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Operations Method Tests")
  class OperationsMethodTests {

    @Test
    @DisplayName("should have getAvailableOperations method")
    void shouldHaveGetAvailableOperationsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiPollable.class.getMethod("getAvailableOperations");
      assertNotNull(method, "getAvailableOperations method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have invoke method")
    void shouldHaveInvokeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiPollable.class.getMethod("invoke", String.class, Object[].class);
      assertNotNull(method, "invoke method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }
  }

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("should have getLastAccessedAt method")
    void shouldHaveGetLastAccessedAtMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiPollable.class.getMethod("getLastAccessedAt");
      assertNotNull(method, "getLastAccessedAt method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getCreatedAt method")
    void shouldHaveGetCreatedAtMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiPollable.class.getMethod("getCreatedAt");
      assertNotNull(method, "getCreatedAt method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }
  }
}
