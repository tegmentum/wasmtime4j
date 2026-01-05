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

import ai.tegmentum.wasmtime4j.WasmThreadLocalStorage;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasmThreadLocalStorage} class.
 *
 * <p>PanamaWasmThreadLocalStorage provides thread-local storage using Panama FFI.
 */
@DisplayName("PanamaWasmThreadLocalStorage Tests")
class PanamaWasmThreadLocalStorageTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasmThreadLocalStorage.class.getModifiers()),
          "PanamaWasmThreadLocalStorage should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasmThreadLocalStorage.class.getModifiers()),
          "PanamaWasmThreadLocalStorage should be final");
    }

    @Test
    @DisplayName("should implement WasmThreadLocalStorage interface")
    void shouldImplementWasmThreadLocalStorageInterface() {
      assertTrue(
          WasmThreadLocalStorage.class.isAssignableFrom(PanamaWasmThreadLocalStorage.class),
          "PanamaWasmThreadLocalStorage should implement WasmThreadLocalStorage");
    }
  }

  @Nested
  @DisplayName("Integer Storage Method Tests")
  class IntegerStorageMethodTests {

    @Test
    @DisplayName("should have putInt method")
    void shouldHavePutIntMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmThreadLocalStorage.class.getMethod("putInt", String.class, int.class);
      assertNotNull(method, "putInt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getInt method")
    void shouldHaveGetIntMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("getInt", String.class);
      assertNotNull(method, "getInt method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Long Storage Method Tests")
  class LongStorageMethodTests {

    @Test
    @DisplayName("should have putLong method")
    void shouldHavePutLongMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmThreadLocalStorage.class.getMethod("putLong", String.class, long.class);
      assertNotNull(method, "putLong method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getLong method")
    void shouldHaveGetLongMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("getLong", String.class);
      assertNotNull(method, "getLong method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Float Storage Method Tests")
  class FloatStorageMethodTests {

    @Test
    @DisplayName("should have putFloat method")
    void shouldHavePutFloatMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmThreadLocalStorage.class.getMethod("putFloat", String.class, float.class);
      assertNotNull(method, "putFloat method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getFloat method")
    void shouldHaveGetFloatMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("getFloat", String.class);
      assertNotNull(method, "getFloat method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }
  }

  @Nested
  @DisplayName("Double Storage Method Tests")
  class DoubleStorageMethodTests {

    @Test
    @DisplayName("should have putDouble method")
    void shouldHavePutDoubleMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmThreadLocalStorage.class.getMethod("putDouble", String.class, double.class);
      assertNotNull(method, "putDouble method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getDouble method")
    void shouldHaveGetDoubleMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("getDouble", String.class);
      assertNotNull(method, "getDouble method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("Bytes Storage Method Tests")
  class BytesStorageMethodTests {

    @Test
    @DisplayName("should have putBytes method")
    void shouldHavePutBytesMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmThreadLocalStorage.class.getMethod("putBytes", String.class, byte[].class);
      assertNotNull(method, "putBytes method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getBytes method")
    void shouldHaveGetBytesMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("getBytes", String.class);
      assertNotNull(method, "getBytes method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }
  }

  @Nested
  @DisplayName("String Storage Method Tests")
  class StringStorageMethodTests {

    @Test
    @DisplayName("should have putString method")
    void shouldHavePutStringMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmThreadLocalStorage.class.getMethod("putString", String.class, String.class);
      assertNotNull(method, "putString method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getString method")
    void shouldHaveGetStringMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("getString", String.class);
      assertNotNull(method, "getString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Key Management Method Tests")
  class KeyManagementMethodTests {

    @Test
    @DisplayName("should have remove method")
    void shouldHaveRemoveMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("remove", String.class);
      assertNotNull(method, "remove method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have contains method")
    void shouldHaveContainsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("contains", String.class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMemoryUsage method")
    void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("getMemoryUsage");
      assertNotNull(method, "getMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThreadLocalStorage.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 2 parameters")
    void shouldHavePublicConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaWasmThreadLocalStorage.class.getConstructors()) {
        if (constructor.getParameterCount() == 2
            && constructor.getParameterTypes()[0] == MemorySegment.class
            && constructor.getParameterTypes()[1] == Arena.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(hasExpectedConstructor, "Should have constructor with MemorySegment and Arena");
    }
  }
}
