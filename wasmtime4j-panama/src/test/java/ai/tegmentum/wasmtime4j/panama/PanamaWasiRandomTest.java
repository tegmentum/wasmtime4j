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

import ai.tegmentum.wasmtime4j.WasiRandom;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiRandom} class.
 *
 * <p>PanamaWasiRandom provides cryptographically secure random number generation using Panama FFI.
 */
@DisplayName("PanamaWasiRandom Tests")
class PanamaWasiRandomTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiRandom.class.getModifiers()),
          "PanamaWasiRandom should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiRandom.class.getModifiers()),
          "PanamaWasiRandom should be final");
    }

    @Test
    @DisplayName("should implement WasiRandom interface")
    void shouldImplementWasiRandomInterface() {
      assertTrue(
          WasiRandom.class.isAssignableFrom(PanamaWasiRandom.class),
          "PanamaWasiRandom should implement WasiRandom");
    }
  }

  @Nested
  @DisplayName("Random Method Tests")
  class RandomMethodTests {

    @Test
    @DisplayName("should have getRandomBytes method")
    void shouldHaveGetRandomBytesMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiRandom.class.getMethod("getRandomBytes", int.class);
      assertNotNull(method, "getRandomBytes method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("should have getRandomU64 method")
    void shouldHaveGetRandomU64Method() throws NoSuchMethodException {
      final Method method = PanamaWasiRandom.class.getMethod("getRandomU64");
      assertNotNull(method, "getRandomU64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public default constructor")
    void shouldHavePublicDefaultConstructor() throws NoSuchMethodException {
      var constructor = PanamaWasiRandom.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
