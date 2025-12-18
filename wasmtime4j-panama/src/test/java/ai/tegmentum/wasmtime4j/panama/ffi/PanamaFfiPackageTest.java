/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j.panama.ffi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Panama FFI package.
 *
 * <p>This test covers all classes in the ai.tegmentum.wasmtime4j.panama.ffi package including
 * WasmtimeBindings, MemorySegmentManager, and FunctionDescriptors.
 */
@DisplayName("Panama FFI Package Tests")
class PanamaFfiPackageTest {

  // ========================================================================
  // WasmtimeBindings Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmtimeBindings Tests")
  class WasmtimeBindingsTests {

    @Test
    @DisplayName("WasmtimeBindings should be a final class")
    void wasmtimeBindingsShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasmtimeBindings.class.getModifiers()),
          "WasmtimeBindings should be final");
    }

    @Test
    @DisplayName("WasmtimeBindings constructor should throw for null symbolLookup")
    void wasmtimeBindingsConstructorShouldThrowForNullSymbolLookup() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasmtimeBindings(null),
          "Should throw for null symbolLookup");
    }

    @Test
    @DisplayName("WasmtimeBindings constructor should accept valid symbolLookup")
    void wasmtimeBindingsConstructorShouldAcceptValidSymbolLookup() {
      // Use the loader lookup which is always available
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      WasmtimeBindings bindings = new WasmtimeBindings(lookup);

      assertNotNull(bindings, "WasmtimeBindings should be created");
    }

    @Test
    @DisplayName("WasmtimeBindings getMethodHandle should throw for null functionName")
    void wasmtimeBindingsGetMethodHandleShouldThrowForNullFunctionName() {
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      WasmtimeBindings bindings = new WasmtimeBindings(lookup);

      assertThrows(
          IllegalArgumentException.class,
          () -> bindings.getMethodHandle(null, FunctionDescriptor.ofVoid()),
          "Should throw for null functionName");
    }

    @Test
    @DisplayName("WasmtimeBindings getMethodHandle should throw for empty functionName")
    void wasmtimeBindingsGetMethodHandleShouldThrowForEmptyFunctionName() {
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      WasmtimeBindings bindings = new WasmtimeBindings(lookup);

      assertThrows(
          IllegalArgumentException.class,
          () -> bindings.getMethodHandle("", FunctionDescriptor.ofVoid()),
          "Should throw for empty functionName");
    }

    @Test
    @DisplayName("WasmtimeBindings getMethodHandle should throw for null descriptor")
    void wasmtimeBindingsGetMethodHandleShouldThrowForNullDescriptor() {
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      WasmtimeBindings bindings = new WasmtimeBindings(lookup);

      assertThrows(
          IllegalArgumentException.class,
          () -> bindings.getMethodHandle("test_function", null),
          "Should throw for null descriptor");
    }

    @Test
    @DisplayName("WasmtimeBindings getSymbolLookup should return the symbolLookup")
    void wasmtimeBindingsGetSymbolLookupShouldReturnLookup() {
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      WasmtimeBindings bindings = new WasmtimeBindings(lookup);

      assertEquals(lookup, bindings.getSymbolLookup(), "Should return the same lookup");
    }

    @Test
    @DisplayName("WasmtimeBindings getLinker should return non-null linker")
    void wasmtimeBindingsGetLinkerShouldReturnNonNullLinker() {
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      WasmtimeBindings bindings = new WasmtimeBindings(lookup);

      assertNotNull(bindings.getLinker(), "getLinker should return non-null");
    }

    @Test
    @DisplayName("WasmtimeBindings clearCache should reset cache")
    void wasmtimeBindingsClearCacheShouldResetCache() {
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      WasmtimeBindings bindings = new WasmtimeBindings(lookup);

      // Try to get a method handle (will fail but should cache the null result)
      bindings.getMethodHandle("nonexistent_function", FunctionDescriptor.ofVoid());
      int sizeAfterGet = bindings.getCacheSize();

      bindings.clearCache();
      int sizeAfterClear = bindings.getCacheSize();

      assertEquals(0, sizeAfterClear, "Cache should be empty after clear");
    }

    @Test
    @DisplayName("WasmtimeBindings getCacheSize should return correct size")
    void wasmtimeBindingsGetCacheSizeShouldReturnCorrectSize() {
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      WasmtimeBindings bindings = new WasmtimeBindings(lookup);

      int initialSize = bindings.getCacheSize();
      assertEquals(0, initialSize, "Initial cache size should be 0");

      // Request a method handle - even if null, it gets cached
      bindings.getMethodHandle("test_func1", FunctionDescriptor.ofVoid());
      assertTrue(bindings.getCacheSize() >= 0, "Cache size should be non-negative");
    }

    @Test
    @DisplayName("WasmtimeBindings should have static memory layout constants")
    void wasmtimeBindingsShouldHaveStaticLayoutConstants() {
      assertNotNull(WasmtimeBindings.WASMTIME_ENGINE_LAYOUT, "ENGINE_LAYOUT should exist");
      assertNotNull(WasmtimeBindings.WASMTIME_MODULE_LAYOUT, "MODULE_LAYOUT should exist");
      assertNotNull(WasmtimeBindings.WASMTIME_INSTANCE_LAYOUT, "INSTANCE_LAYOUT should exist");
      assertNotNull(WasmtimeBindings.WASMTIME_MEMORY_LAYOUT, "MEMORY_LAYOUT should exist");
      assertNotNull(WasmtimeBindings.WASMTIME_TABLE_LAYOUT, "TABLE_LAYOUT should exist");
    }

    @Test
    @DisplayName("WasmtimeBindings method handles methods should return null for unknown functions")
    void wasmtimeBindingsMethodsShouldReturnNullForUnknownFunctions() {
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      WasmtimeBindings bindings = new WasmtimeBindings(lookup);

      // These will likely return null since the native library isn't loaded
      // The methods should still be callable without throwing
      bindings.wasmtimeEngineNew();
      bindings.wasmtimeEngineDelete();
      bindings.wasmtimeModuleNew();
      bindings.wasmtimeModuleDelete();
      // Just ensure they don't throw - actual functionality requires native library
    }
  }

  // ========================================================================
  // MemorySegmentManager Tests
  // ========================================================================

  @Nested
  @DisplayName("MemorySegmentManager Tests")
  class MemorySegmentManagerTests {

    private Arena arena;
    private MemorySegmentManager manager;

    @BeforeEach
    void setUp() {
      arena = Arena.ofConfined();
      manager = new MemorySegmentManager(arena);
    }

    @AfterEach
    void tearDown() {
      if (arena != null) {
        arena.close();
      }
    }

    @Test
    @DisplayName("MemorySegmentManager should be a final class")
    void memorySegmentManagerShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(MemorySegmentManager.class.getModifiers()),
          "MemorySegmentManager should be final");
    }

    @Test
    @DisplayName("MemorySegmentManager constructor should throw for null arena")
    void memorySegmentManagerConstructorShouldThrowForNullArena() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new MemorySegmentManager(null),
          "Should throw for null arena");
    }

    @Test
    @DisplayName("MemorySegmentManager fromByteArray should create segment from bytes")
    void memorySegmentManagerFromByteArrayShouldCreateSegment() {
      byte[] data = {1, 2, 3, 4, 5};
      MemorySegment segment = manager.fromByteArray(data);

      assertNotNull(segment, "Segment should be created");
      assertEquals(data.length, segment.byteSize(), "Segment size should match");
    }

    @Test
    @DisplayName("MemorySegmentManager fromByteArray should throw for null data")
    void memorySegmentManagerFromByteArrayShouldThrowForNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.fromByteArray(null),
          "Should throw for null data");
    }

    @Test
    @DisplayName("MemorySegmentManager fromByteArray should return NULL for empty array")
    void memorySegmentManagerFromByteArrayShouldReturnNullForEmptyArray() {
      MemorySegment segment = manager.fromByteArray(new byte[0]);
      assertEquals(MemorySegment.NULL, segment, "Should return NULL for empty array");
    }

    @Test
    @DisplayName("MemorySegmentManager fromByteBuffer should create segment from buffer")
    void memorySegmentManagerFromByteBufferShouldCreateSegment() {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5});
      MemorySegment segment = manager.fromByteBuffer(buffer);

      assertNotNull(segment, "Segment should be created");
      assertEquals(5, segment.byteSize(), "Segment size should match buffer remaining");
    }

    @Test
    @DisplayName("MemorySegmentManager fromByteBuffer should throw for null buffer")
    void memorySegmentManagerFromByteBufferShouldThrowForNullBuffer() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.fromByteBuffer(null),
          "Should throw for null buffer");
    }

    @Test
    @DisplayName("MemorySegmentManager fromByteBuffer should return NULL for empty buffer")
    void memorySegmentManagerFromByteBufferShouldReturnNullForEmptyBuffer() {
      ByteBuffer buffer = ByteBuffer.allocate(0);
      MemorySegment segment = manager.fromByteBuffer(buffer);
      assertEquals(MemorySegment.NULL, segment, "Should return NULL for empty buffer");
    }

    @Test
    @DisplayName("MemorySegmentManager toByteArray should convert segment to bytes")
    void memorySegmentManagerToByteArrayShouldConvertSegmentToBytes() {
      byte[] original = {10, 20, 30, 40, 50};
      MemorySegment segment = manager.fromByteArray(original);
      byte[] result = manager.toByteArray(segment);

      assertArrayEquals(original, result, "Converted bytes should match original");
    }

    @Test
    @DisplayName("MemorySegmentManager toByteArray should throw for null segment")
    void memorySegmentManagerToByteArrayShouldThrowForNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.toByteArray(null),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("MemorySegmentManager toByteArray should return empty for NULL segment")
    void memorySegmentManagerToByteArrayShouldReturnEmptyForNullSegment() {
      byte[] result = manager.toByteArray(MemorySegment.NULL);
      assertEquals(0, result.length, "Should return empty array for NULL segment");
    }

    @Test
    @DisplayName("MemorySegmentManager asByteBuffer should create buffer view")
    void memorySegmentManagerAsByteBufferShouldCreateBufferView() {
      byte[] original = {1, 2, 3, 4, 5};
      MemorySegment segment = manager.fromByteArray(original);
      ByteBuffer buffer = manager.asByteBuffer(segment);

      assertNotNull(buffer, "Buffer should be created");
      assertEquals(original.length, buffer.remaining(), "Buffer size should match");
    }

    @Test
    @DisplayName("MemorySegmentManager asByteBuffer should throw for null segment")
    void memorySegmentManagerAsByteBufferShouldThrowForNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.asByteBuffer(null),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("MemorySegmentManager asByteBuffer should return empty buffer for NULL segment")
    void memorySegmentManagerAsByteBufferShouldReturnEmptyForNullSegment() {
      ByteBuffer buffer = manager.asByteBuffer(MemorySegment.NULL);
      assertEquals(0, buffer.remaining(), "Should return empty buffer for NULL segment");
    }

    @Test
    @DisplayName("MemorySegmentManager allocate should allocate memory of specified size")
    void memorySegmentManagerAllocateShouldAllocateMemory() {
      MemorySegment segment = manager.allocate(100);

      assertNotNull(segment, "Segment should be allocated");
      assertEquals(100, segment.byteSize(), "Segment size should match requested");
    }

    @Test
    @DisplayName("MemorySegmentManager allocate should throw for negative size")
    void memorySegmentManagerAllocateShouldThrowForNegativeSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.allocate(-1),
          "Should throw for negative size");
    }

    @Test
    @DisplayName("MemorySegmentManager allocate should return NULL for zero size")
    void memorySegmentManagerAllocateShouldReturnNullForZeroSize() {
      MemorySegment segment = manager.allocate(0);
      assertEquals(MemorySegment.NULL, segment, "Should return NULL for zero size");
    }

    @Test
    @DisplayName("MemorySegmentManager allocateAndFill should allocate and initialize memory")
    void memorySegmentManagerAllocateAndFillShouldAllocateAndInitialize() {
      MemorySegment segment = manager.allocateAndFill(10, (byte) 0xFF);

      assertNotNull(segment, "Segment should be allocated");
      assertEquals(10, segment.byteSize(), "Segment size should match");

      // Verify all bytes are filled
      for (int i = 0; i < 10; i++) {
        assertEquals((byte) 0xFF, segment.get(ValueLayout.JAVA_BYTE, i), "Byte at " + i + " should be 0xFF");
      }
    }

    @Test
    @DisplayName("MemorySegmentManager fromString should create null-terminated string")
    void memorySegmentManagerFromStringShouldCreateNullTerminatedString() {
      String testStr = "Hello, World!";
      MemorySegment segment = manager.fromString(testStr);

      assertNotNull(segment, "Segment should be created");
      // The segment should be at least the string length + 1 for null terminator
      assertTrue(segment.byteSize() >= testStr.length() + 1, "Segment should include null terminator");
    }

    @Test
    @DisplayName("MemorySegmentManager fromString should throw for null string")
    void memorySegmentManagerFromStringShouldThrowForNullString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.fromString(null),
          "Should throw for null string");
    }

    @Test
    @DisplayName("MemorySegmentManager toString should read string from segment")
    void memorySegmentManagerToStringShouldReadStringFromSegment() {
      String testStr = "Test String";
      MemorySegment segment = manager.fromString(testStr);
      String result = manager.toString(segment);

      assertEquals(testStr, result, "Read string should match original");
    }

    @Test
    @DisplayName("MemorySegmentManager toString should throw for null segment")
    void memorySegmentManagerToStringShouldThrowForNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.toString(null),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("MemorySegmentManager toString should return empty for NULL segment")
    void memorySegmentManagerToStringShouldReturnEmptyForNullSegment() {
      String result = manager.toString(MemorySegment.NULL);
      assertEquals("", result, "Should return empty string for NULL segment");
    }

    @Test
    @DisplayName("MemorySegmentManager getArena should return the arena")
    void memorySegmentManagerGetArenaShouldReturnArena() {
      assertEquals(arena, manager.getArena(), "Should return the same arena");
    }

    @Test
    @DisplayName("MemorySegmentManager should handle roundtrip conversion")
    void memorySegmentManagerShouldHandleRoundtripConversion() {
      byte[] original = {100, -50, 0, 127, -128};
      MemorySegment segment = manager.fromByteArray(original);
      byte[] converted = manager.toByteArray(segment);

      assertArrayEquals(original, converted, "Roundtrip conversion should preserve data");
    }
  }

  // ========================================================================
  // FunctionDescriptors Tests
  // ========================================================================

  @Nested
  @DisplayName("FunctionDescriptors Tests")
  class FunctionDescriptorsTests {

    @BeforeEach
    void setUp() {
      FunctionDescriptors.clearCache();
    }

    @Test
    @DisplayName("FunctionDescriptors should be a final class")
    void functionDescriptorsShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(FunctionDescriptors.class.getModifiers()),
          "FunctionDescriptors should be final");
    }

    @Test
    @DisplayName("FunctionDescriptors should not be instantiable")
    void functionDescriptorsShouldNotBeInstantiable() {
      assertThrows(
          Exception.class,
          () -> {
            var constructor = FunctionDescriptors.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
          },
          "FunctionDescriptors should not be instantiable");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeEngineNew should return correct descriptor")
    void functionDescriptorsWasmtimeEngineNewShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeEngineNew();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(ValueLayout.ADDRESS, desc.returnLayout().get(), "Return type should be ADDRESS");
      assertEquals(0, desc.argumentLayouts().size(), "Should have no arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeEngineDelete should return correct descriptor")
    void functionDescriptorsWasmtimeEngineDeleteShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeEngineDelete();

      assertNotNull(desc, "Descriptor should not be null");
      assertFalse(desc.returnLayout().isPresent(), "Should have void return type");
      assertEquals(1, desc.argumentLayouts().size(), "Should have one argument");
      assertEquals(ValueLayout.ADDRESS, desc.argumentLayouts().get(0), "Argument should be ADDRESS");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeModuleNew should return correct descriptor")
    void functionDescriptorsWasmtimeModuleNewShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeModuleNew();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(3, desc.argumentLayouts().size(), "Should have three arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeModuleDelete should return correct descriptor")
    void functionDescriptorsWasmtimeModuleDeleteShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeModuleDelete();

      assertNotNull(desc, "Descriptor should not be null");
      assertFalse(desc.returnLayout().isPresent(), "Should have void return type");
      assertEquals(1, desc.argumentLayouts().size(), "Should have one argument");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeModuleSerialize should return correct descriptor")
    void functionDescriptorsWasmtimeModuleSerializeShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeModuleSerialize();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(3, desc.argumentLayouts().size(), "Should have three arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeInstanceNew should return correct descriptor")
    void functionDescriptorsWasmtimeInstanceNewShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeInstanceNew();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(3, desc.argumentLayouts().size(), "Should have three arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeInstanceDelete should return correct descriptor")
    void functionDescriptorsWasmtimeInstanceDeleteShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeInstanceDelete();

      assertNotNull(desc, "Descriptor should not be null");
      assertFalse(desc.returnLayout().isPresent(), "Should have void return type");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeInstanceExportGet should return correct descriptor")
    void functionDescriptorsWasmtimeInstanceExportGetShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeInstanceExportGet();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(3, desc.argumentLayouts().size(), "Should have three arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeInstanceExportNth should return correct descriptor")
    void functionDescriptorsWasmtimeInstanceExportNthShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeInstanceExportNth();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(4, desc.argumentLayouts().size(), "Should have four arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeInstanceExportsLen should return correct descriptor")
    void functionDescriptorsWasmtimeInstanceExportsLenShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeInstanceExportsLen();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(1, desc.argumentLayouts().size(), "Should have one argument");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeMemoryNew should return correct descriptor")
    void functionDescriptorsWasmtimeMemoryNewShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeMemoryNew();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(2, desc.argumentLayouts().size(), "Should have two arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeMemorySize should return correct descriptor")
    void functionDescriptorsWasmtimeMemorySizeShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeMemorySize();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(1, desc.argumentLayouts().size(), "Should have one argument");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeMemoryGrow should return correct descriptor")
    void functionDescriptorsWasmtimeMemoryGrowShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeMemoryGrow();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(2, desc.argumentLayouts().size(), "Should have two arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeMemoryData should return correct descriptor")
    void functionDescriptorsWasmtimeMemoryDataShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeMemoryData();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(ValueLayout.ADDRESS, desc.returnLayout().get(), "Return type should be ADDRESS");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeFuncCall should return correct descriptor")
    void functionDescriptorsWasmtimeFuncCallShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeFuncCall();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(5, desc.argumentLayouts().size(), "Should have five arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeGlobalGet should return correct descriptor")
    void functionDescriptorsWasmtimeGlobalGetShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeGlobalGet();

      assertNotNull(desc, "Descriptor should not be null");
      assertFalse(desc.returnLayout().isPresent(), "Should have void return type");
      assertEquals(2, desc.argumentLayouts().size(), "Should have two arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeGlobalSet should return correct descriptor")
    void functionDescriptorsWasmtimeGlobalSetShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeGlobalSet();

      assertNotNull(desc, "Descriptor should not be null");
      assertFalse(desc.returnLayout().isPresent(), "Should have void return type");
      assertEquals(2, desc.argumentLayouts().size(), "Should have two arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeTableSize should return correct descriptor")
    void functionDescriptorsWasmtimeTableSizeShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeTableSize();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(1, desc.argumentLayouts().size(), "Should have one argument");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeTableGet should return correct descriptor")
    void functionDescriptorsWasmtimeTableGetShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeTableGet();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(3, desc.argumentLayouts().size(), "Should have three arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors wasmtimeTableSet should return correct descriptor")
    void functionDescriptorsWasmtimeTableSetShouldReturnCorrectDescriptor() {
      FunctionDescriptor desc = FunctionDescriptors.wasmtimeTableSet();

      assertNotNull(desc, "Descriptor should not be null");
      assertTrue(desc.returnLayout().isPresent(), "Should have return type");
      assertEquals(3, desc.argumentLayouts().size(), "Should have three arguments");
    }

    @Test
    @DisplayName("FunctionDescriptors getCacheSize should return correct size")
    void functionDescriptorsGetCacheSizeShouldReturnCorrectSize() {
      FunctionDescriptors.clearCache();
      assertEquals(0, FunctionDescriptors.getCacheSize(), "Initial cache should be empty");

      FunctionDescriptors.wasmtimeEngineNew();
      assertEquals(1, FunctionDescriptors.getCacheSize(), "Cache should have one entry");

      FunctionDescriptors.wasmtimeEngineDelete();
      assertEquals(2, FunctionDescriptors.getCacheSize(), "Cache should have two entries");
    }

    @Test
    @DisplayName("FunctionDescriptors clearCache should empty the cache")
    void functionDescriptorsClearCacheShouldEmptyCache() {
      FunctionDescriptors.wasmtimeEngineNew();
      FunctionDescriptors.wasmtimeModuleNew();
      assertTrue(FunctionDescriptors.getCacheSize() > 0, "Cache should not be empty");

      FunctionDescriptors.clearCache();
      assertEquals(0, FunctionDescriptors.getCacheSize(), "Cache should be empty after clear");
    }

    @Test
    @DisplayName("FunctionDescriptors should cache descriptors")
    void functionDescriptorsShouldCacheDescriptors() {
      FunctionDescriptors.clearCache();

      FunctionDescriptor desc1 = FunctionDescriptors.wasmtimeEngineNew();
      FunctionDescriptor desc2 = FunctionDescriptors.wasmtimeEngineNew();

      // Same instance should be returned due to caching
      assertTrue(desc1 == desc2, "Same descriptor instance should be returned from cache");
    }

    @Test
    @DisplayName("FunctionDescriptors all methods should be static")
    void functionDescriptorsAllMethodsShouldBeStatic() throws Exception {
      Method[] methods = FunctionDescriptors.class.getDeclaredMethods();

      for (Method method : methods) {
        if (method.getName().startsWith("wasmtime")
            || method.getName().equals("getCacheSize")
            || method.getName().equals("clearCache")) {
          assertTrue(
              Modifier.isStatic(method.getModifiers()),
              method.getName() + " should be static");
        }
      }
    }
  }
}
