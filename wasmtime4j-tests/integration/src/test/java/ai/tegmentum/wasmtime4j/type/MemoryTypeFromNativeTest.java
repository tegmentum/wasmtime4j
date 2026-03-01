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

package ai.tegmentum.wasmtime4j.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for MemoryType.fromNative() functionality.
 *
 * <p>These tests verify that MemoryType instances are correctly parsed from native handles when
 * retrieved from compiled WebAssembly modules. This exercises the fromNative() code path in both
 * JNI and Panama implementations.
 *
 * @since 1.0.0
 */
@DisplayName("MemoryType fromNative Integration Tests")
public class MemoryTypeFromNativeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryTypeFromNativeTest.class.getName());

  private static byte[] createMinOnlyMemoryModule() {
    return new byte[] {
      0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00, 0x05, 0x03, 0x01, 0x00, 0x01, 0x07, 0x0A,
      0x01, 0x06, 'm', 'e', 'm', 'o', 'r', 'y', 0x02, 0x00
    };
  }

  private static byte[] createMinMaxMemoryModule() {
    return new byte[] {
      0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00, 0x05, 0x04, 0x01, 0x01, 0x01, 0x0A, 0x07,
      0x0A, 0x01, 0x06, 'm', 'e', 'm', 'o', 'r', 'y', 0x02, 0x00
    };
  }

  private static byte[] createLargerMemoryModule() {
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6D,
      0x01,
      0x00,
      0x00,
      0x00,
      0x05,
      0x05,
      0x01,
      0x01,
      0x10,
      (byte) 0x80,
      0x02,
      0x07,
      0x0A,
      0x01,
      0x06,
      'm',
      'e',
      'm',
      'o',
      'r',
      'y',
      0x02,
      0x00
    };
  }

  private static byte[] createZeroMinMemoryModule() {
    return new byte[] {
      0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00, 0x05, 0x04, 0x01, 0x01, 0x00, 0x64, 0x07,
      0x0A, 0x01, 0x06, 'm', 'e', 'm', 'o', 'r', 'y', 0x02, 0x00
    };
  }

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Module.getMemoryType() Tests")
  class ModuleGetMemoryTypeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get memory type with minimum only from module")
    void shouldGetMemoryTypeWithMinOnlyFromModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing Module.getMemoryType() for minimum-only memory");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMinOnlyMemoryModule())) {

        final Optional<MemoryType> memoryTypeOpt = module.getMemoryType("memory");

        assertTrue(memoryTypeOpt.isPresent(), "MemoryType should be present");
        final MemoryType memoryType = memoryTypeOpt.get();

        assertNotNull(memoryType, "MemoryType should not be null");
        assertEquals(1, memoryType.getMinimum(), "Minimum should be 1 page");
        assertFalse(memoryType.getMaximum().isPresent(), "Maximum should not be present");
        assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "Kind should be MEMORY");
        assertFalse(memoryType.is64Bit(), "Memory should not be 64-bit");
        assertFalse(memoryType.isShared(), "Memory should not be shared");

        LOGGER.info("Minimum-only memory type parsed correctly from native");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get memory type with min and max from module")
    void shouldGetMemoryTypeWithMinMaxFromModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing Module.getMemoryType() for min/max memory");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMinMaxMemoryModule())) {

        final Optional<MemoryType> memoryTypeOpt = module.getMemoryType("memory");

        assertTrue(memoryTypeOpt.isPresent(), "MemoryType should be present");
        final MemoryType memoryType = memoryTypeOpt.get();

        assertEquals(1, memoryType.getMinimum(), "Minimum should be 1 page");
        assertTrue(memoryType.getMaximum().isPresent(), "Maximum should be present");
        assertEquals(10L, memoryType.getMaximum().get(), "Maximum should be 10 pages");
        assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "Kind should be MEMORY");

        LOGGER.info("Min/max memory type parsed correctly from native");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get larger memory type from module")
    void shouldGetLargerMemoryTypeFromModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing Module.getMemoryType() for larger memory");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createLargerMemoryModule())) {

        final Optional<MemoryType> memoryTypeOpt = module.getMemoryType("memory");

        assertTrue(memoryTypeOpt.isPresent(), "MemoryType should be present");
        final MemoryType memoryType = memoryTypeOpt.get();

        assertEquals(16, memoryType.getMinimum(), "Minimum should be 16 pages");
        assertTrue(memoryType.getMaximum().isPresent(), "Maximum should be present");
        assertEquals(256L, memoryType.getMaximum().get(), "Maximum should be 256 pages");

        LOGGER.info("Larger memory type parsed correctly from native");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get zero-minimum memory type from module")
    void shouldGetZeroMinMemoryTypeFromModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing Module.getMemoryType() for zero-minimum memory");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createZeroMinMemoryModule())) {

        final Optional<MemoryType> memoryTypeOpt = module.getMemoryType("memory");

        assertTrue(memoryTypeOpt.isPresent(), "MemoryType should be present");
        final MemoryType memoryType = memoryTypeOpt.get();

        assertEquals(0, memoryType.getMinimum(), "Minimum should be 0 pages");
        assertTrue(memoryType.getMaximum().isPresent(), "Maximum should be present");
        assertEquals(100L, memoryType.getMaximum().get(), "Maximum should be 100 pages");

        LOGGER.info("Zero-minimum memory type parsed correctly from native");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return empty for non-existent memory")
    void shouldReturnEmptyForNonExistentMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing Module.getMemoryType() for non-existent memory");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMinOnlyMemoryModule())) {

        final Optional<MemoryType> memoryTypeOpt = module.getMemoryType("nonexistent");

        assertFalse(memoryTypeOpt.isPresent(), "MemoryType should not be present for non-existent");

        LOGGER.info("Non-existent memory correctly returns empty");
      }
    }
  }

  @Nested
  @DisplayName("Instance Memory Type via getMemory() Tests")
  class InstanceGetMemoryTypeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get memory type from instance via getMemory()")
    void shouldGetMemoryTypeFromInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing Instance memory type via getMemory()");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createMinMaxMemoryModule());
        final Instance instance = store.createInstance(module);

        final Optional<MemoryType> memoryTypeOpt =
            instance.getMemory("memory").map(m -> m.getMemoryType());

        assertTrue(memoryTypeOpt.isPresent(), "MemoryType should be present");
        final MemoryType memoryType = memoryTypeOpt.get();

        assertEquals(1, memoryType.getMinimum(), "Minimum should be 1 page");
        assertTrue(memoryType.getMaximum().isPresent(), "Maximum should be present");
        assertEquals(10L, memoryType.getMaximum().get(), "Maximum should be 10 pages");

        LOGGER.info("Instance memory type retrieved correctly from native");
      }
    }
  }

  @Nested
  @DisplayName("Module.getMemoryTypes() List Tests")
  class ModuleGetMemoryTypesListTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get all memory types from module")
    void shouldGetAllMemoryTypesFromModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing Module.getMemoryTypes() list");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMinMaxMemoryModule())) {

        final List<MemoryType> memoryTypes = module.getMemoryTypes();

        assertNotNull(memoryTypes, "Memory types list should not be null");
        assertEquals(1, memoryTypes.size(), "Should have 1 memory type");

        final MemoryType memoryType = memoryTypes.get(0);
        assertNotNull(memoryType, "MemoryType in list should not be null");
        assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "Kind should be MEMORY");
        assertEquals(1, memoryType.getMinimum(), "Minimum should be 1 page");
        assertEquals(10L, memoryType.getMaximum().get(), "Maximum should be 10 pages");

        LOGGER.info("Memory types list parsed correctly from native");
      }
    }
  }

  @Nested
  @DisplayName("MemoryType Consistency Tests")
  class MemoryTypeConsistencyTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("module and instance memory types should match")
    void moduleAndInstanceMemoryTypesShouldMatch(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing consistency between module and instance memory types");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createMinMaxMemoryModule());
        final Instance instance = store.createInstance(module);

        final Optional<MemoryType> moduleMemoryType = module.getMemoryType("memory");
        final Optional<MemoryType> instanceMemoryType =
            instance.getMemory("memory").map(m -> m.getMemoryType());

        assertTrue(moduleMemoryType.isPresent(), "Module memory type should be present");
        assertTrue(instanceMemoryType.isPresent(), "Instance memory type should be present");

        assertEquals(
            moduleMemoryType.get().getMinimum(),
            instanceMemoryType.get().getMinimum(),
            "Minimum should match");
        assertEquals(
            moduleMemoryType.get().getMaximum(),
            instanceMemoryType.get().getMaximum(),
            "Maximum should match");
        assertEquals(
            moduleMemoryType.get().is64Bit(),
            instanceMemoryType.get().is64Bit(),
            "64-bit flag should match");
        assertEquals(
            moduleMemoryType.get().isShared(),
            instanceMemoryType.get().isShared(),
            "Shared flag should match");
        assertEquals(
            moduleMemoryType.get().getKind(),
            instanceMemoryType.get().getKind(),
            "Kinds should match");

        LOGGER.info("Module and instance memory types are consistent");
      }
    }
  }

  @Nested
  @DisplayName("Memory Limits Edge Cases Tests")
  class MemoryLimitsEdgeCasesTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle memory with equal min and max")
    void shouldHandleMemoryWithEqualMinMax(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing memory with equal min and max");

      // Create module with min = max = 5
      final byte[] wasmBytes =
          new byte[] {
            0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00, 0x05, 0x04, 0x01, 0x01, 0x05, 0x05,
            0x07, 0x0A, 0x01, 0x06, 'm', 'e', 'm', 'o', 'r', 'y', 0x02, 0x00
          };

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(wasmBytes)) {

        final Optional<MemoryType> memoryTypeOpt = module.getMemoryType("memory");
        assertTrue(memoryTypeOpt.isPresent(), "MemoryType should be present");

        final MemoryType memoryType = memoryTypeOpt.get();
        assertEquals(5, memoryType.getMinimum(), "Minimum should be 5");
        assertTrue(memoryType.getMaximum().isPresent(), "Maximum should be present");
        assertEquals(5L, memoryType.getMaximum().get(), "Maximum should be 5");

        LOGGER.info("Equal min/max memory handled correctly");
      }
    }
  }
}
