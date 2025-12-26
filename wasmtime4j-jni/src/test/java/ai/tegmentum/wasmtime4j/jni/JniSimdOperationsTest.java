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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.simd.SimdOperations;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniSimdOperations} class.
 *
 * <p>JniSimdOperations provides JNI implementation of WebAssembly SIMD operations.
 */
@DisplayName("JniSimdOperations Tests")
class JniSimdOperationsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(Modifier.isPublic(JniSimdOperations.class.getModifiers()), "JniSimdOperations should be public");
      assertTrue(Modifier.isFinal(JniSimdOperations.class.getModifiers()), "JniSimdOperations should be final");
    }

    @Test
    @DisplayName("should implement SimdOperations interface")
    void shouldImplementSimdOperationsInterface() {
      assertTrue(
          SimdOperations.class.isAssignableFrom(JniSimdOperations.class),
          "JniSimdOperations should implement SimdOperations");
    }

    @Test
    @DisplayName("should have constructor with runtime handle")
    void shouldHaveConstructorWithRuntimeHandle() throws NoSuchMethodException {
      final Constructor<?> constructor = JniSimdOperations.class.getConstructor(long.class);
      assertNotNull(constructor, "Constructor with runtime handle should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Arithmetic Operations Tests")
  class ArithmeticOperationsTests {

    @Test
    @DisplayName("should have add method")
    void shouldHaveAddMethod() throws NoSuchMethodException {
      final Method method =
          JniSimdOperations.class.getMethod(
              "add",
              ai.tegmentum.wasmtime4j.simd.SimdVector.class,
              ai.tegmentum.wasmtime4j.simd.SimdVector.class);
      assertNotNull(method, "add method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdVector.class, method.getReturnType(), "add should return SimdVector");
    }

    @Test
    @DisplayName("should have subtract method")
    void shouldHaveSubtractMethod() throws NoSuchMethodException {
      final Method method =
          JniSimdOperations.class.getMethod(
              "subtract",
              ai.tegmentum.wasmtime4j.simd.SimdVector.class,
              ai.tegmentum.wasmtime4j.simd.SimdVector.class);
      assertNotNull(method, "subtract method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdVector.class, method.getReturnType(), "subtract should return SimdVector");
    }

    @Test
    @DisplayName("should have multiply method")
    void shouldHaveMultiplyMethod() throws NoSuchMethodException {
      final Method method =
          JniSimdOperations.class.getMethod(
              "multiply",
              ai.tegmentum.wasmtime4j.simd.SimdVector.class,
              ai.tegmentum.wasmtime4j.simd.SimdVector.class);
      assertNotNull(method, "multiply method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdVector.class, method.getReturnType(), "multiply should return SimdVector");
    }

    @Test
    @DisplayName("should have divide method")
    void shouldHaveDivideMethod() throws NoSuchMethodException {
      final Method method =
          JniSimdOperations.class.getMethod(
              "divide",
              ai.tegmentum.wasmtime4j.simd.SimdVector.class,
              ai.tegmentum.wasmtime4j.simd.SimdVector.class);
      assertNotNull(method, "divide method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdVector.class, method.getReturnType(), "divide should return SimdVector");
    }
  }

  @Nested
  @DisplayName("Bitwise Operations Tests")
  class BitwiseOperationsTests {

    @Test
    @DisplayName("should have and method")
    void shouldHaveAndMethod() throws NoSuchMethodException {
      final Method method =
          JniSimdOperations.class.getMethod(
              "and",
              ai.tegmentum.wasmtime4j.simd.SimdVector.class,
              ai.tegmentum.wasmtime4j.simd.SimdVector.class);
      assertNotNull(method, "and method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdVector.class, method.getReturnType(), "and should return SimdVector");
    }

    @Test
    @DisplayName("should have or method")
    void shouldHaveOrMethod() throws NoSuchMethodException {
      final Method method =
          JniSimdOperations.class.getMethod(
              "or",
              ai.tegmentum.wasmtime4j.simd.SimdVector.class,
              ai.tegmentum.wasmtime4j.simd.SimdVector.class);
      assertNotNull(method, "or method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdVector.class, method.getReturnType(), "or should return SimdVector");
    }

    @Test
    @DisplayName("should have xor method")
    void shouldHaveXorMethod() throws NoSuchMethodException {
      final Method method =
          JniSimdOperations.class.getMethod(
              "xor",
              ai.tegmentum.wasmtime4j.simd.SimdVector.class,
              ai.tegmentum.wasmtime4j.simd.SimdVector.class);
      assertNotNull(method, "xor method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdVector.class, method.getReturnType(), "xor should return SimdVector");
    }

    @Test
    @DisplayName("should have not method")
    void shouldHaveNotMethod() throws NoSuchMethodException {
      final Method method = JniSimdOperations.class.getMethod("not", ai.tegmentum.wasmtime4j.simd.SimdVector.class);
      assertNotNull(method, "not method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdVector.class, method.getReturnType(), "not should return SimdVector");
    }
  }

  @Nested
  @DisplayName("Memory Operations Tests")
  class MemoryOperationsTests {

    @Test
    @DisplayName("should have load method")
    void shouldHaveLoadMethod() throws NoSuchMethodException {
      final Method method =
          JniSimdOperations.class.getMethod(
              "load",
              ai.tegmentum.wasmtime4j.WasmMemory.class,
              int.class,
              ai.tegmentum.wasmtime4j.simd.SimdLane.class);
      assertNotNull(method, "load method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdVector.class, method.getReturnType(), "load should return SimdVector");
    }

    @Test
    @DisplayName("should have store method")
    void shouldHaveStoreMethod() throws NoSuchMethodException {
      final Method method =
          JniSimdOperations.class.getMethod(
              "store",
              ai.tegmentum.wasmtime4j.WasmMemory.class,
              int.class,
              ai.tegmentum.wasmtime4j.simd.SimdVector.class);
      assertNotNull(method, "store method should exist");
      assertEquals(void.class, method.getReturnType(), "store should return void");
    }
  }

  @Nested
  @DisplayName("Capability Tests")
  class CapabilityTests {

    @Test
    @DisplayName("should have isSimdSupported method")
    void shouldHaveIsSimdSupportedMethod() throws NoSuchMethodException {
      final Method method = JniSimdOperations.class.getMethod("isSimdSupported");
      assertNotNull(method, "isSimdSupported method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isSimdSupported should return boolean");
    }

    @Test
    @DisplayName("should have getSimdCapabilities method")
    void shouldHaveGetSimdCapabilitiesMethod() throws NoSuchMethodException {
      final Method method = JniSimdOperations.class.getMethod("getSimdCapabilities");
      assertNotNull(method, "getSimdCapabilities method should exist");
      assertEquals(String.class, method.getReturnType(), "getSimdCapabilities should return String");
    }
  }
}
