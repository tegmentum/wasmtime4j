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

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CoreDumpMemory} interface.
 *
 * <p>CoreDumpMemory represents memory contents captured in a WebAssembly core dump.
 */
@DisplayName("CoreDumpMemory Interface Tests")
class CoreDumpMemoryInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CoreDumpMemory.class.isInterface(), "CoreDumpMemory should be an interface");
    }

    @Test
    @DisplayName("should have getInstanceIndex method")
    void shouldHaveGetInstanceIndexMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("getInstanceIndex");
      assertNotNull(method, "getInstanceIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMemoryIndex method")
    void shouldHaveGetMemoryIndexMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("getMemoryIndex");
      assertNotNull(method, "getMemoryIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getSizeInPages method")
    void shouldHaveGetSizeInPagesMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("getSizeInPages");
      assertNotNull(method, "getSizeInPages method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getSizeInBytes method")
    void shouldHaveGetSizeInBytesMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("getSizeInBytes");
      assertNotNull(method, "getSizeInBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isMemory64 method")
    void shouldHaveIsMemory64Method() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("isMemory64");
      assertNotNull(method, "isMemory64 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMinPages method")
    void shouldHaveGetMinPagesMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("getMinPages");
      assertNotNull(method, "getMinPages method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxPages method")
    void shouldHaveGetMaxPagesMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("getMaxPages");
      assertNotNull(method, "getMaxPages method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getSegments method")
    void shouldHaveGetSegmentsMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("getSegments");
      assertNotNull(method, "getSegments method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have read method")
    void shouldHaveReadMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.class.getMethod("read", long.class, int.class);
      assertNotNull(method, "read method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }
  }

  @Nested
  @DisplayName("MemorySegment Nested Interface Tests")
  class MemorySegmentNestedInterfaceTests {

    @Test
    @DisplayName("MemorySegment should be a nested interface")
    void memorySegmentShouldBeNestedInterface() {
      assertTrue(
          CoreDumpMemory.MemorySegment.class.isInterface(), "MemorySegment should be an interface");
    }

    @Test
    @DisplayName("MemorySegment should have getOffset method")
    void shouldHaveGetOffsetMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.MemorySegment.class.getMethod("getOffset");
      assertNotNull(method, "getOffset method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemorySegment should have getData method")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.MemorySegment.class.getMethod("getData");
      assertNotNull(method, "getData method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("MemorySegment should have getSize default method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = CoreDumpMemory.MemorySegment.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertTrue(method.isDefault(), "getSize should be a default method");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly ten declared methods")
    void shouldHaveExactlyTenDeclaredMethods() {
      final Method[] methods = CoreDumpMemory.class.getDeclaredMethods();
      assertEquals(10, methods.length, "CoreDumpMemory should have exactly 10 declared methods");
    }
  }
}
