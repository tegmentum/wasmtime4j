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

package ai.tegmentum.wasmtime4j.panama.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.WasmMemory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmMemoryToMemoryAdapter} class.
 *
 * <p>WasmMemoryToMemoryAdapter bridges the gap between the WasmMemory interface and the Memory
 * interface used by the Caller interface, handling type conversions and method signature
 * differences.
 */
@DisplayName("WasmMemoryToMemoryAdapter Tests")
class WasmMemoryToMemoryAdapterTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasmMemoryToMemoryAdapter.class.getModifiers()),
          "WasmMemoryToMemoryAdapter should be public");
      assertTrue(
          Modifier.isFinal(WasmMemoryToMemoryAdapter.class.getModifiers()),
          "WasmMemoryToMemoryAdapter should be final");
    }

    @Test
    @DisplayName("should implement Memory interface")
    void shouldImplementMemoryInterface() {
      assertTrue(
          Memory.class.isAssignableFrom(WasmMemoryToMemoryAdapter.class),
          "WasmMemoryToMemoryAdapter should implement Memory");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with WasmMemory parameter")
    void shouldHaveConstructorWithWasmMemory() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasmMemoryToMemoryAdapter.class.getConstructor(WasmMemory.class);
      assertNotNull(constructor, "Constructor with WasmMemory should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Size Method Tests")
  class SizeMethodTests {

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasmMemoryToMemoryAdapter.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getSizeInBytes method")
    void shouldHaveGetSizeInBytesMethod() throws NoSuchMethodException {
      final Method method = WasmMemoryToMemoryAdapter.class.getMethod("getSizeInBytes");
      assertNotNull(method, "getSizeInBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxSize method")
    void shouldHaveGetMaxSizeMethod() throws NoSuchMethodException {
      final Method method = WasmMemoryToMemoryAdapter.class.getMethod("getMaxSize");
      assertNotNull(method, "getMaxSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Grow Method Tests")
  class GrowMethodTests {

    @Test
    @DisplayName("should have grow method")
    void shouldHaveGrowMethod() throws NoSuchMethodException {
      final Method method = WasmMemoryToMemoryAdapter.class.getMethod("grow", long.class);
      assertNotNull(method, "grow method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Read Method Tests")
  class ReadMethodTests {

    @Test
    @DisplayName("should have read method with ByteBuffer")
    void shouldHaveReadMethodWithByteBuffer() throws NoSuchMethodException {
      final Method method =
          WasmMemoryToMemoryAdapter.class.getMethod("read", long.class, ByteBuffer.class);
      assertNotNull(method, "read method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have readByte method")
    void shouldHaveReadByteMethod() throws NoSuchMethodException {
      final Method method = WasmMemoryToMemoryAdapter.class.getMethod("readByte", long.class);
      assertNotNull(method, "readByte method should exist");
      assertEquals(byte.class, method.getReturnType(), "Should return byte");
    }

    @Test
    @DisplayName("should have readInt32 method")
    void shouldHaveReadInt32Method() throws NoSuchMethodException {
      final Method method = WasmMemoryToMemoryAdapter.class.getMethod("readInt32", long.class);
      assertNotNull(method, "readInt32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have readInt64 method")
    void shouldHaveReadInt64Method() throws NoSuchMethodException {
      final Method method = WasmMemoryToMemoryAdapter.class.getMethod("readInt64", long.class);
      assertNotNull(method, "readInt64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Write Method Tests")
  class WriteMethodTests {

    @Test
    @DisplayName("should have write method with ByteBuffer")
    void shouldHaveWriteMethodWithByteBuffer() throws NoSuchMethodException {
      final Method method =
          WasmMemoryToMemoryAdapter.class.getMethod("write", long.class, ByteBuffer.class);
      assertNotNull(method, "write method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have writeByte method")
    void shouldHaveWriteByteMethod() throws NoSuchMethodException {
      final Method method =
          WasmMemoryToMemoryAdapter.class.getMethod("writeByte", long.class, byte.class);
      assertNotNull(method, "writeByte method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have writeInt32 method")
    void shouldHaveWriteInt32Method() throws NoSuchMethodException {
      final Method method =
          WasmMemoryToMemoryAdapter.class.getMethod("writeInt32", long.class, int.class);
      assertNotNull(method, "writeInt32 method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have writeInt64 method")
    void shouldHaveWriteInt64Method() throws NoSuchMethodException {
      final Method method =
          WasmMemoryToMemoryAdapter.class.getMethod("writeInt64", long.class, long.class);
      assertNotNull(method, "writeInt64 method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("State Method Tests")
  class StateMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasmMemoryToMemoryAdapter.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getDelegate method")
    void shouldHaveGetDelegateMethod() throws NoSuchMethodException {
      final Method method = WasmMemoryToMemoryAdapter.class.getMethod("getDelegate");
      assertNotNull(method, "getDelegate method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "Should return WasmMemory");
    }
  }
}
