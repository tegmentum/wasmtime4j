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

package ai.tegmentum.wasmtime4j.panama.ffi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link FunctionDescriptors} class.
 *
 * <p>This test class verifies the function descriptor repository for Wasmtime FFI calls.
 */
@DisplayName("FunctionDescriptors Tests")
class FunctionDescriptorsTest {

  @BeforeEach
  void setUp() {
    // Clear cache before each test
    FunctionDescriptors.clearCache();
  }

  @AfterEach
  void tearDown() {
    // Clear cache after each test
    FunctionDescriptors.clearCache();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("FunctionDescriptors should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(FunctionDescriptors.class.getModifiers()),
          "FunctionDescriptors should be final");
    }

    @Test
    @DisplayName("FunctionDescriptors should have private constructor")
    void shouldHavePrivateConstructor() throws Exception {
      final Constructor<FunctionDescriptors> constructor =
          FunctionDescriptors.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should throw UnsupportedOperationException")
    void privateConstructorShouldThrowUnsupportedOperationException() throws Exception {
      final Constructor<FunctionDescriptors> constructor =
          FunctionDescriptors.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      final InvocationTargetException exception =
          assertThrows(
              InvocationTargetException.class,
              constructor::newInstance,
              "Constructor should throw wrapped exception");

      assertTrue(
          exception.getCause() instanceof UnsupportedOperationException,
          "Cause should be UnsupportedOperationException");
    }
  }

  @Nested
  @DisplayName("Engine Function Descriptor Tests")
  class EngineFunctionDescriptorTests {

    @Test
    @DisplayName("wasmtimeEngineNew should return correct descriptor")
    void wasmtimeEngineNewShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeEngineNew();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(ValueLayout.ADDRESS, descriptor.returnLayout().get(), "Should return ADDRESS");
      assertEquals(0, descriptor.argumentLayouts().size(), "Should have no arguments");
    }

    @Test
    @DisplayName("wasmtimeEngineDelete should return correct descriptor")
    void wasmtimeEngineDeleteShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeEngineDelete();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertFalse(descriptor.returnLayout().isPresent(), "Should be void return");
      assertEquals(1, descriptor.argumentLayouts().size(), "Should have 1 argument");
      assertEquals(
          ValueLayout.ADDRESS, descriptor.argumentLayouts().get(0), "First arg should be ADDRESS");
    }
  }

  @Nested
  @DisplayName("Module Function Descriptor Tests")
  class ModuleFunctionDescriptorTests {

    @Test
    @DisplayName("wasmtimeModuleNew should return correct descriptor")
    void wasmtimeModuleNewShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeModuleNew();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(3, descriptor.argumentLayouts().size(), "Should have 3 arguments");
      assertEquals(
          ValueLayout.ADDRESS, descriptor.argumentLayouts().get(0), "First arg should be ADDRESS");
      assertEquals(
          ValueLayout.ADDRESS, descriptor.argumentLayouts().get(1), "Second arg should be ADDRESS");
      assertEquals(
          ValueLayout.JAVA_LONG,
          descriptor.argumentLayouts().get(2),
          "Third arg should be JAVA_LONG");
    }

    @Test
    @DisplayName("wasmtimeModuleDelete should return correct descriptor")
    void wasmtimeModuleDeleteShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeModuleDelete();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertFalse(descriptor.returnLayout().isPresent(), "Should be void return");
      assertEquals(1, descriptor.argumentLayouts().size(), "Should have 1 argument");
    }

    @Test
    @DisplayName("wasmtimeModuleSerialize should return correct descriptor")
    void wasmtimeModuleSerializeShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeModuleSerialize();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(
          ValueLayout.JAVA_LONG, descriptor.returnLayout().get(), "Should return JAVA_LONG");
      assertEquals(3, descriptor.argumentLayouts().size(), "Should have 3 arguments");
    }
  }

  @Nested
  @DisplayName("Instance Function Descriptor Tests")
  class InstanceFunctionDescriptorTests {

    @Test
    @DisplayName("wasmtimeInstanceNew should return correct descriptor")
    void wasmtimeInstanceNewShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeInstanceNew();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(3, descriptor.argumentLayouts().size(), "Should have 3 arguments");
    }

    @Test
    @DisplayName("wasmtimeInstanceDelete should return correct descriptor")
    void wasmtimeInstanceDeleteShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeInstanceDelete();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertFalse(descriptor.returnLayout().isPresent(), "Should be void return");
      assertEquals(1, descriptor.argumentLayouts().size(), "Should have 1 argument");
    }

    @Test
    @DisplayName("wasmtimeInstanceExportGet should return correct descriptor")
    void wasmtimeInstanceExportGetShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeInstanceExportGet();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(
          ValueLayout.JAVA_BOOLEAN, descriptor.returnLayout().get(), "Should return JAVA_BOOLEAN");
      assertEquals(3, descriptor.argumentLayouts().size(), "Should have 3 arguments");
    }

    @Test
    @DisplayName("wasmtimeInstanceExportNth should return correct descriptor")
    void wasmtimeInstanceExportNthShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeInstanceExportNth();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(4, descriptor.argumentLayouts().size(), "Should have 4 arguments");
    }

    @Test
    @DisplayName("wasmtimeInstanceExportsLen should return correct descriptor")
    void wasmtimeInstanceExportsLenShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeInstanceExportsLen();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(
          ValueLayout.JAVA_LONG, descriptor.returnLayout().get(), "Should return JAVA_LONG");
      assertEquals(1, descriptor.argumentLayouts().size(), "Should have 1 argument");
    }
  }

  @Nested
  @DisplayName("Memory Function Descriptor Tests")
  class MemoryFunctionDescriptorTests {

    @Test
    @DisplayName("wasmtimeMemoryNew should return correct descriptor")
    void wasmtimeMemoryNewShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeMemoryNew();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(ValueLayout.ADDRESS, descriptor.returnLayout().get(), "Should return ADDRESS");
      assertEquals(2, descriptor.argumentLayouts().size(), "Should have 2 arguments");
      assertEquals(
          ValueLayout.JAVA_INT,
          descriptor.argumentLayouts().get(0),
          "First arg should be JAVA_INT");
      assertEquals(
          ValueLayout.JAVA_INT,
          descriptor.argumentLayouts().get(1),
          "Second arg should be JAVA_INT");
    }

    @Test
    @DisplayName("wasmtimeMemorySize should return correct descriptor")
    void wasmtimeMemorySizeShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeMemorySize();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(ValueLayout.JAVA_INT, descriptor.returnLayout().get(), "Should return JAVA_INT");
      assertEquals(1, descriptor.argumentLayouts().size(), "Should have 1 argument");
    }

    @Test
    @DisplayName("wasmtimeMemoryGrow should return correct descriptor")
    void wasmtimeMemoryGrowShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeMemoryGrow();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(
          ValueLayout.JAVA_BOOLEAN, descriptor.returnLayout().get(), "Should return JAVA_BOOLEAN");
      assertEquals(2, descriptor.argumentLayouts().size(), "Should have 2 arguments");
    }

    @Test
    @DisplayName("wasmtimeMemoryData should return correct descriptor")
    void wasmtimeMemoryDataShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeMemoryData();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(ValueLayout.ADDRESS, descriptor.returnLayout().get(), "Should return ADDRESS");
      assertEquals(1, descriptor.argumentLayouts().size(), "Should have 1 argument");
    }
  }

  @Nested
  @DisplayName("Function Function Descriptor Tests")
  class FunctionFunctionDescriptorTests {

    @Test
    @DisplayName("wasmtimeFuncCall should return correct descriptor")
    void wasmtimeFuncCallShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeFuncCall();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(ValueLayout.ADDRESS, descriptor.returnLayout().get(), "Should return ADDRESS");
      assertEquals(5, descriptor.argumentLayouts().size(), "Should have 5 arguments");
    }
  }

  @Nested
  @DisplayName("Global Function Descriptor Tests")
  class GlobalFunctionDescriptorTests {

    @Test
    @DisplayName("wasmtimeGlobalGet should return correct descriptor")
    void wasmtimeGlobalGetShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeGlobalGet();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertFalse(descriptor.returnLayout().isPresent(), "Should be void return");
      assertEquals(2, descriptor.argumentLayouts().size(), "Should have 2 arguments");
    }

    @Test
    @DisplayName("wasmtimeGlobalSet should return correct descriptor")
    void wasmtimeGlobalSetShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeGlobalSet();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertFalse(descriptor.returnLayout().isPresent(), "Should be void return");
      assertEquals(2, descriptor.argumentLayouts().size(), "Should have 2 arguments");
    }
  }

  @Nested
  @DisplayName("Table Function Descriptor Tests")
  class TableFunctionDescriptorTests {

    @Test
    @DisplayName("wasmtimeTableSize should return correct descriptor")
    void wasmtimeTableSizeShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeTableSize();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(ValueLayout.JAVA_INT, descriptor.returnLayout().get(), "Should return JAVA_INT");
      assertEquals(1, descriptor.argumentLayouts().size(), "Should have 1 argument");
    }

    @Test
    @DisplayName("wasmtimeTableGet should return correct descriptor")
    void wasmtimeTableGetShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeTableGet();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(
          ValueLayout.JAVA_BOOLEAN, descriptor.returnLayout().get(), "Should return JAVA_BOOLEAN");
      assertEquals(3, descriptor.argumentLayouts().size(), "Should have 3 arguments");
    }

    @Test
    @DisplayName("wasmtimeTableSet should return correct descriptor")
    void wasmtimeTableSetShouldReturnCorrectDescriptor() {
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeTableSet();

      assertNotNull(descriptor, "Descriptor should not be null");
      assertTrue(descriptor.returnLayout().isPresent(), "Should have return layout");
      assertEquals(
          ValueLayout.JAVA_BOOLEAN, descriptor.returnLayout().get(), "Should return JAVA_BOOLEAN");
      assertEquals(3, descriptor.argumentLayouts().size(), "Should have 3 arguments");
    }
  }

  @Nested
  @DisplayName("Cache Management Tests")
  class CacheManagementTests {

    @Test
    @DisplayName("getCacheSize should return zero initially")
    void getCacheSizeShouldReturnZeroInitially() {
      assertEquals(0, FunctionDescriptors.getCacheSize(), "Cache should be empty initially");
    }

    @Test
    @DisplayName("getCacheSize should increase after getting descriptors")
    void getCacheSizeShouldIncreaseAfterGettingDescriptors() {
      FunctionDescriptors.wasmtimeEngineNew();
      assertEquals(1, FunctionDescriptors.getCacheSize(), "Cache should have 1 entry");

      FunctionDescriptors.wasmtimeModuleNew();
      assertEquals(2, FunctionDescriptors.getCacheSize(), "Cache should have 2 entries");
    }

    @Test
    @DisplayName("clearCache should reset cache size to zero")
    void clearCacheShouldResetCacheSizeToZero() {
      FunctionDescriptors.wasmtimeEngineNew();
      FunctionDescriptors.wasmtimeModuleNew();
      assertTrue(FunctionDescriptors.getCacheSize() > 0, "Cache should have entries");

      FunctionDescriptors.clearCache();

      assertEquals(0, FunctionDescriptors.getCacheSize(), "Cache should be empty after clear");
    }

    @Test
    @DisplayName("clearCache should be idempotent")
    void clearCacheShouldBeIdempotent() {
      FunctionDescriptors.clearCache();
      assertDoesNotThrow(FunctionDescriptors::clearCache, "Multiple clears should not throw");
      assertEquals(0, FunctionDescriptors.getCacheSize(), "Cache should remain empty");
    }

    @Test
    @DisplayName("Repeated calls should return cached descriptor")
    void repeatedCallsShouldReturnCachedDescriptor() {
      final FunctionDescriptor first = FunctionDescriptors.wasmtimeEngineNew();
      final FunctionDescriptor second = FunctionDescriptors.wasmtimeEngineNew();

      assertSame(first, second, "Should return same cached instance");
      assertEquals(1, FunctionDescriptors.getCacheSize(), "Cache should not grow");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("All engine descriptors should be accessible")
    void allEngineDescriptorsShouldBeAccessible() {
      assertDoesNotThrow(FunctionDescriptors::wasmtimeEngineNew, "wasmtimeEngineNew should work");
      assertDoesNotThrow(
          FunctionDescriptors::wasmtimeEngineDelete, "wasmtimeEngineDelete should work");
    }

    @Test
    @DisplayName("All module descriptors should be accessible")
    void allModuleDescriptorsShouldBeAccessible() {
      assertDoesNotThrow(FunctionDescriptors::wasmtimeModuleNew, "wasmtimeModuleNew should work");
      assertDoesNotThrow(
          FunctionDescriptors::wasmtimeModuleDelete, "wasmtimeModuleDelete should work");
      assertDoesNotThrow(
          FunctionDescriptors::wasmtimeModuleSerialize, "wasmtimeModuleSerialize should work");
    }

    @Test
    @DisplayName("All instance descriptors should be accessible")
    void allInstanceDescriptorsShouldBeAccessible() {
      assertDoesNotThrow(
          FunctionDescriptors::wasmtimeInstanceNew, "wasmtimeInstanceNew should work");
      assertDoesNotThrow(
          FunctionDescriptors::wasmtimeInstanceDelete, "wasmtimeInstanceDelete should work");
      assertDoesNotThrow(
          FunctionDescriptors::wasmtimeInstanceExportGet, "wasmtimeInstanceExportGet should work");
      assertDoesNotThrow(
          FunctionDescriptors::wasmtimeInstanceExportNth, "wasmtimeInstanceExportNth should work");
      assertDoesNotThrow(
          FunctionDescriptors::wasmtimeInstanceExportsLen,
          "wasmtimeInstanceExportsLen should work");
    }

    @Test
    @DisplayName("All memory descriptors should be accessible")
    void allMemoryDescriptorsShouldBeAccessible() {
      assertDoesNotThrow(FunctionDescriptors::wasmtimeMemoryNew, "wasmtimeMemoryNew should work");
      assertDoesNotThrow(FunctionDescriptors::wasmtimeMemorySize, "wasmtimeMemorySize should work");
      assertDoesNotThrow(FunctionDescriptors::wasmtimeMemoryGrow, "wasmtimeMemoryGrow should work");
      assertDoesNotThrow(FunctionDescriptors::wasmtimeMemoryData, "wasmtimeMemoryData should work");
    }

    @Test
    @DisplayName("All function descriptors should be accessible")
    void allFunctionDescriptorsShouldBeAccessible() {
      assertDoesNotThrow(FunctionDescriptors::wasmtimeFuncCall, "wasmtimeFuncCall should work");
    }

    @Test
    @DisplayName("All global descriptors should be accessible")
    void allGlobalDescriptorsShouldBeAccessible() {
      assertDoesNotThrow(FunctionDescriptors::wasmtimeGlobalGet, "wasmtimeGlobalGet should work");
      assertDoesNotThrow(FunctionDescriptors::wasmtimeGlobalSet, "wasmtimeGlobalSet should work");
    }

    @Test
    @DisplayName("All table descriptors should be accessible")
    void allTableDescriptorsShouldBeAccessible() {
      assertDoesNotThrow(FunctionDescriptors::wasmtimeTableSize, "wasmtimeTableSize should work");
      assertDoesNotThrow(FunctionDescriptors::wasmtimeTableGet, "wasmtimeTableGet should work");
      assertDoesNotThrow(FunctionDescriptors::wasmtimeTableSet, "wasmtimeTableSet should work");
    }

    @Test
    @DisplayName("Full lifecycle should work correctly")
    void fullLifecycleShouldWorkCorrectly() {
      // Get various descriptors
      FunctionDescriptors.wasmtimeEngineNew();
      FunctionDescriptors.wasmtimeModuleNew();
      FunctionDescriptors.wasmtimeInstanceNew();
      FunctionDescriptors.wasmtimeMemoryNew();
      FunctionDescriptors.wasmtimeFuncCall();

      // Check cache
      assertTrue(FunctionDescriptors.getCacheSize() >= 5, "Should have at least 5 cached entries");

      // Clear cache
      FunctionDescriptors.clearCache();
      assertEquals(0, FunctionDescriptors.getCacheSize(), "Cache should be cleared");

      // Can still get descriptors after clear
      final FunctionDescriptor descriptor = FunctionDescriptors.wasmtimeEngineNew();
      assertNotNull(descriptor, "Descriptor should still be available after clear");
      assertEquals(1, FunctionDescriptors.getCacheSize(), "Cache should have new entry");
    }
  }
}
