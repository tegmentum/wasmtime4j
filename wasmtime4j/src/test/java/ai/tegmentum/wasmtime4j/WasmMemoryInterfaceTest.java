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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmMemory interface.
 *
 * <p>WasmMemory represents WebAssembly linear memory with support for 32-bit and 64-bit addressing,
 * atomic operations, and bulk memory operations. This test verifies the interface structure and
 * method signatures.
 */
@DisplayName("WasmMemory Interface Tests")
class WasmMemoryInterfaceTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmMemory.class.isInterface(), "WasmMemory should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasmMemory.class.getModifiers()), "WasmMemory should be public");
    }
  }

  // ========================================================================
  // Core Memory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Core Memory Methods Tests")
  class CoreMemoryMethodsTests {

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getSize should return int");
    }

    @Test
    @DisplayName("should have size default method")
    void shouldHaveSizeDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertTrue(method.isDefault(), "size should be a default method");
      assertEquals(int.class, method.getReturnType(), "size should return int");
    }

    @Test
    @DisplayName("should have grow method")
    void shouldHaveGrowMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("grow", int.class);
      assertNotNull(method, "grow method should exist");
      assertEquals(int.class, method.getReturnType(), "grow should return int");
    }

    @Test
    @DisplayName("should have getMaxSize method")
    void shouldHaveGetMaxSizeMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("getMaxSize");
      assertNotNull(method, "getMaxSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getMaxSize should return int");
    }

    @Test
    @DisplayName("should have getMemoryType method")
    void shouldHaveGetMemoryTypeMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("getMemoryType");
      assertNotNull(method, "getMemoryType method should exist");
      assertEquals(
          MemoryType.class, method.getReturnType(), "getMemoryType should return MemoryType");
    }

    @Test
    @DisplayName("should have getBuffer method")
    void shouldHaveGetBufferMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("getBuffer");
      assertNotNull(method, "getBuffer method should exist");
      assertEquals(ByteBuffer.class, method.getReturnType(), "getBuffer should return ByteBuffer");
    }
  }

  // ========================================================================
  // Read/Write Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Read/Write Methods Tests")
  class ReadWriteMethodsTests {

    @Test
    @DisplayName("should have readByte method")
    void shouldHaveReadByteMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("readByte", int.class);
      assertNotNull(method, "readByte method should exist");
      assertEquals(byte.class, method.getReturnType(), "readByte should return byte");
    }

    @Test
    @DisplayName("should have writeByte method")
    void shouldHaveWriteByteMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("writeByte", int.class, byte.class);
      assertNotNull(method, "writeByte method should exist");
      assertEquals(void.class, method.getReturnType(), "writeByte should return void");
    }

    @Test
    @DisplayName("should have readBytes method")
    void shouldHaveReadBytesMethod() throws NoSuchMethodException {
      final Method method =
          WasmMemory.class.getMethod("readBytes", int.class, byte[].class, int.class, int.class);
      assertNotNull(method, "readBytes method should exist");
      assertEquals(void.class, method.getReturnType(), "readBytes should return void");
    }

    @Test
    @DisplayName("should have writeBytes method")
    void shouldHaveWriteBytesMethod() throws NoSuchMethodException {
      final Method method =
          WasmMemory.class.getMethod("writeBytes", int.class, byte[].class, int.class, int.class);
      assertNotNull(method, "writeBytes method should exist");
      assertEquals(void.class, method.getReturnType(), "writeBytes should return void");
    }
  }

  // ========================================================================
  // Bulk Memory Operations Tests
  // ========================================================================

  @Nested
  @DisplayName("Bulk Memory Operations Tests")
  class BulkMemoryOperationsTests {

    @Test
    @DisplayName("should have copy method")
    void shouldHaveCopyMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("copy", int.class, int.class, int.class);
      assertNotNull(method, "copy method should exist");
      assertEquals(void.class, method.getReturnType(), "copy should return void");
    }

    @Test
    @DisplayName("should have fill method")
    void shouldHaveFillMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("fill", int.class, byte.class, int.class);
      assertNotNull(method, "fill method should exist");
      assertEquals(void.class, method.getReturnType(), "fill should return void");
    }

    @Test
    @DisplayName("should have init method")
    void shouldHaveInitMethod() throws NoSuchMethodException {
      final Method method =
          WasmMemory.class.getMethod("init", int.class, int.class, int.class, int.class);
      assertNotNull(method, "init method should exist");
      assertEquals(void.class, method.getReturnType(), "init should return void");
    }

    @Test
    @DisplayName("should have dropDataSegment method")
    void shouldHaveDropDataSegmentMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("dropDataSegment", int.class);
      assertNotNull(method, "dropDataSegment method should exist");
      assertEquals(void.class, method.getReturnType(), "dropDataSegment should return void");
    }
  }

  // ========================================================================
  // Shared Memory Atomic Operations Tests
  // ========================================================================

  @Nested
  @DisplayName("Shared Memory Atomic Operations Tests")
  class SharedMemoryAtomicOperationsTests {

    @Test
    @DisplayName("should have isShared method")
    void shouldHaveIsSharedMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("isShared");
      assertNotNull(method, "isShared method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isShared should return boolean");
    }

    @Test
    @DisplayName("should have atomicCompareAndSwapInt method")
    void shouldHaveAtomicCompareAndSwapIntMethod() throws NoSuchMethodException {
      final Method method =
          WasmMemory.class.getMethod("atomicCompareAndSwapInt", int.class, int.class, int.class);
      assertNotNull(method, "atomicCompareAndSwapInt method should exist");
      assertEquals(int.class, method.getReturnType(), "atomicCompareAndSwapInt should return int");
    }

    @Test
    @DisplayName("should have atomicCompareAndSwapLong method")
    void shouldHaveAtomicCompareAndSwapLongMethod() throws NoSuchMethodException {
      final Method method =
          WasmMemory.class.getMethod("atomicCompareAndSwapLong", int.class, long.class, long.class);
      assertNotNull(method, "atomicCompareAndSwapLong method should exist");
      assertEquals(
          long.class, method.getReturnType(), "atomicCompareAndSwapLong should return long");
    }

    @Test
    @DisplayName("should have atomicLoadInt method")
    void shouldHaveAtomicLoadIntMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("atomicLoadInt", int.class);
      assertNotNull(method, "atomicLoadInt method should exist");
      assertEquals(int.class, method.getReturnType(), "atomicLoadInt should return int");
    }

    @Test
    @DisplayName("should have atomicLoadLong method")
    void shouldHaveAtomicLoadLongMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("atomicLoadLong", int.class);
      assertNotNull(method, "atomicLoadLong method should exist");
      assertEquals(long.class, method.getReturnType(), "atomicLoadLong should return long");
    }

    @Test
    @DisplayName("should have atomicStoreInt method")
    void shouldHaveAtomicStoreIntMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("atomicStoreInt", int.class, int.class);
      assertNotNull(method, "atomicStoreInt method should exist");
      assertEquals(void.class, method.getReturnType(), "atomicStoreInt should return void");
    }

    @Test
    @DisplayName("should have atomicStoreLong method")
    void shouldHaveAtomicStoreLongMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("atomicStoreLong", int.class, long.class);
      assertNotNull(method, "atomicStoreLong method should exist");
      assertEquals(void.class, method.getReturnType(), "atomicStoreLong should return void");
    }

    @Test
    @DisplayName("should have atomicAddInt method")
    void shouldHaveAtomicAddIntMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("atomicAddInt", int.class, int.class);
      assertNotNull(method, "atomicAddInt method should exist");
      assertEquals(int.class, method.getReturnType(), "atomicAddInt should return int");
    }

    @Test
    @DisplayName("should have atomicFence method")
    void shouldHaveAtomicFenceMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("atomicFence");
      assertNotNull(method, "atomicFence method should exist");
      assertEquals(void.class, method.getReturnType(), "atomicFence should return void");
    }

    @Test
    @DisplayName("should have atomicNotify method")
    void shouldHaveAtomicNotifyMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("atomicNotify", int.class, int.class);
      assertNotNull(method, "atomicNotify method should exist");
      assertEquals(int.class, method.getReturnType(), "atomicNotify should return int");
    }

    @Test
    @DisplayName("should have atomicWait32 method")
    void shouldHaveAtomicWait32Method() throws NoSuchMethodException {
      final Method method =
          WasmMemory.class.getMethod("atomicWait32", int.class, int.class, long.class);
      assertNotNull(method, "atomicWait32 method should exist");
      assertEquals(int.class, method.getReturnType(), "atomicWait32 should return int");
    }

    @Test
    @DisplayName("should have atomicWait64 method")
    void shouldHaveAtomicWait64Method() throws NoSuchMethodException {
      final Method method =
          WasmMemory.class.getMethod("atomicWait64", int.class, long.class, long.class);
      assertNotNull(method, "atomicWait64 method should exist");
      assertEquals(int.class, method.getReturnType(), "atomicWait64 should return int");
    }
  }

  // ========================================================================
  // 64-bit Addressing Default Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("64-bit Addressing Default Methods Tests")
  class Memory64DefaultMethodsTests {

    @Test
    @DisplayName("should have supports64BitAddressing default method")
    void shouldHaveSupports64BitAddressingDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("supports64BitAddressing");
      assertNotNull(method, "supports64BitAddressing method should exist");
      assertTrue(method.isDefault(), "supports64BitAddressing should be a default method");
      assertEquals(
          boolean.class, method.getReturnType(), "supports64BitAddressing should return boolean");
    }

    @Test
    @DisplayName("should have getSize64 default method")
    void shouldHaveGetSize64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("getSize64");
      assertNotNull(method, "getSize64 method should exist");
      assertTrue(method.isDefault(), "getSize64 should be a default method");
      assertEquals(long.class, method.getReturnType(), "getSize64 should return long");
    }

    @Test
    @DisplayName("should have grow64 default method")
    void shouldHaveGrow64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("grow64", long.class);
      assertNotNull(method, "grow64 method should exist");
      assertTrue(method.isDefault(), "grow64 should be a default method");
      assertEquals(long.class, method.getReturnType(), "grow64 should return long");
    }

    @Test
    @DisplayName("should have readByte64 default method")
    void shouldHaveReadByte64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("readByte64", long.class);
      assertNotNull(method, "readByte64 method should exist");
      assertTrue(method.isDefault(), "readByte64 should be a default method");
      assertEquals(byte.class, method.getReturnType(), "readByte64 should return byte");
    }

    @Test
    @DisplayName("should have writeByte64 default method")
    void shouldHaveWriteByte64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("writeByte64", long.class, byte.class);
      assertNotNull(method, "writeByte64 method should exist");
      assertTrue(method.isDefault(), "writeByte64 should be a default method");
      assertEquals(void.class, method.getReturnType(), "writeByte64 should return void");
    }

    @Test
    @DisplayName("should have getSizeInBytes64 default method")
    void shouldHaveGetSizeInBytes64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("getSizeInBytes64");
      assertNotNull(method, "getSizeInBytes64 method should exist");
      assertTrue(method.isDefault(), "getSizeInBytes64 should be a default method");
      assertEquals(long.class, method.getReturnType(), "getSizeInBytes64 should return long");
    }
  }

  // ========================================================================
  // Typed Read/Write Default Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Typed Read/Write Default Methods Tests")
  class TypedReadWriteDefaultMethodsTests {

    @Test
    @DisplayName("should have readInt32 default method")
    void shouldHaveReadInt32DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("readInt32", long.class);
      assertNotNull(method, "readInt32 method should exist");
      assertTrue(method.isDefault(), "readInt32 should be a default method");
      assertEquals(int.class, method.getReturnType(), "readInt32 should return int");
    }

    @Test
    @DisplayName("should have readInt64 default method")
    void shouldHaveReadInt64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("readInt64", long.class);
      assertNotNull(method, "readInt64 method should exist");
      assertTrue(method.isDefault(), "readInt64 should be a default method");
      assertEquals(long.class, method.getReturnType(), "readInt64 should return long");
    }

    @Test
    @DisplayName("should have readFloat32 default method")
    void shouldHaveReadFloat32DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("readFloat32", long.class);
      assertNotNull(method, "readFloat32 method should exist");
      assertTrue(method.isDefault(), "readFloat32 should be a default method");
      assertEquals(float.class, method.getReturnType(), "readFloat32 should return float");
    }

    @Test
    @DisplayName("should have readFloat64 default method")
    void shouldHaveReadFloat64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("readFloat64", long.class);
      assertNotNull(method, "readFloat64 method should exist");
      assertTrue(method.isDefault(), "readFloat64 should be a default method");
      assertEquals(double.class, method.getReturnType(), "readFloat64 should return double");
    }

    @Test
    @DisplayName("should have writeInt32 default method")
    void shouldHaveWriteInt32DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("writeInt32", long.class, int.class);
      assertNotNull(method, "writeInt32 method should exist");
      assertTrue(method.isDefault(), "writeInt32 should be a default method");
      assertEquals(void.class, method.getReturnType(), "writeInt32 should return void");
    }

    @Test
    @DisplayName("should have writeFloat64 default method")
    void shouldHaveWriteFloat64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("writeFloat64", long.class, double.class);
      assertNotNull(method, "writeFloat64 method should exist");
      assertTrue(method.isDefault(), "writeFloat64 should be a default method");
      assertEquals(void.class, method.getReturnType(), "writeFloat64 should return void");
    }
  }

  // ========================================================================
  // Direct Memory Access Default Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Direct Memory Access Default Methods Tests")
  class DirectMemoryAccessDefaultMethodsTests {

    @Test
    @DisplayName("should have dataPtr default method")
    void shouldHaveDataPtrDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("dataPtr");
      assertNotNull(method, "dataPtr method should exist");
      assertTrue(method.isDefault(), "dataPtr should be a default method");
      assertEquals(long.class, method.getReturnType(), "dataPtr should return long");
    }

    @Test
    @DisplayName("should have dataSize default method")
    void shouldHaveDataSizeDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("dataSize");
      assertNotNull(method, "dataSize method should exist");
      assertTrue(method.isDefault(), "dataSize should be a default method");
      assertEquals(long.class, method.getReturnType(), "dataSize should return long");
    }

    @Test
    @DisplayName("should have pageSize default method")
    void shouldHavePageSizeDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("pageSize");
      assertNotNull(method, "pageSize method should exist");
      assertTrue(method.isDefault(), "pageSize should be a default method");
      assertEquals(int.class, method.getReturnType(), "pageSize should return int");
    }

    @Test
    @DisplayName("should have pageSizeLog2 default method")
    void shouldHavePageSizeLog2DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("pageSizeLog2");
      assertNotNull(method, "pageSizeLog2 method should exist");
      assertTrue(method.isDefault(), "pageSizeLog2 should be a default method");
      assertEquals(int.class, method.getReturnType(), "pageSizeLog2 should return int");
    }

    @Test
    @DisplayName("should have growAsync default method")
    void shouldHaveGrowAsyncDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("growAsync", int.class);
      assertNotNull(method, "growAsync method should exist");
      assertTrue(method.isDefault(), "growAsync should be a default method");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "growAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("should have getMinSize default method")
    void shouldHaveGetMinSizeDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmMemory.class.getMethod("getMinSize");
      assertNotNull(method, "getMinSize method should exist");
      assertTrue(method.isDefault(), "getMinSize should be a default method");
      assertEquals(int.class, method.getReturnType(), "getMinSize should return int");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 40 declared methods")
    void shouldHaveAtLeast40DeclaredMethods() {
      assertTrue(
          WasmMemory.class.getDeclaredMethods().length >= 40,
          "WasmMemory should have at least 40 methods (found "
              + WasmMemory.class.getDeclaredMethods().length
              + ")");
    }

    @Test
    @DisplayName("should have at least 20 default methods")
    void shouldHaveAtLeast20DefaultMethods() {
      long defaultMethodCount =
          Arrays.stream(WasmMemory.class.getDeclaredMethods()).filter(Method::isDefault).count();
      assertTrue(
          defaultMethodCount >= 20,
          "WasmMemory should have at least 20 default methods (found " + defaultMethodCount + ")");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0, WasmMemory.class.getInterfaces().length, "WasmMemory should not extend any interface");
    }
  }
}
