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
import ai.tegmentum.wasmtime4j.Store;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
public final class MemoryTypeFromNativeTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryTypeFromNativeTest.class.getName());

  /**
   * Creates a simple WebAssembly module with a memory export with minimum only.
   *
   * <pre>
   * (module
   *   (memory (export "memory") 1))
   * </pre>
   */
  private static byte[] createMinOnlyMemoryModule() {
    // WebAssembly binary format for module with 1-page minimum memory
    return new byte[] {
      // Magic number and version
      0x00,
      0x61,
      0x73,
      0x6D, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version 1
      // Memory section (section 5)
      0x05, // section id
      0x03, // section size (3 bytes)
      0x01, // number of memories
      0x00, // limits flags: no max
      0x01, // min = 1 page
      // Export section (section 7)
      0x07, // section id
      0x0A, // section size (10 bytes)
      0x01, // number of exports
      0x06, // name length
      'm',
      'e',
      'm',
      'o',
      'r',
      'y', // name: "memory"
      0x02, // export kind: memory
      0x00 // memory index: 0
    };
  }

  /**
   * Creates a WebAssembly module with a memory export with min and max.
   *
   * <pre>
   * (module
   *   (memory (export "memory") 1 10))
   * </pre>
   */
  private static byte[] createMinMaxMemoryModule() {
    return new byte[] {
      // Magic number and version
      0x00,
      0x61,
      0x73,
      0x6D, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version 1
      // Memory section (section 5)
      0x05, // section id
      0x04, // section size (4 bytes)
      0x01, // number of memories
      0x01, // limits flags: has max
      0x01, // min = 1 page
      0x0A, // max = 10 pages
      // Export section (section 7)
      0x07, // section id
      0x0A, // section size (10 bytes)
      0x01, // number of exports
      0x06, // name length
      'm',
      'e',
      'm',
      'o',
      'r',
      'y', // name: "memory"
      0x02, // export kind: memory
      0x00 // memory index: 0
    };
  }

  /**
   * Creates a WebAssembly module with a larger memory configuration.
   *
   * <pre>
   * (module
   *   (memory (export "memory") 16 256))
   * </pre>
   */
  private static byte[] createLargerMemoryModule() {
    return new byte[] {
      // Magic number and version
      0x00,
      0x61,
      0x73,
      0x6D, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version 1
      // Memory section (section 5)
      0x05, // section id
      0x05, // section size (5 bytes)
      0x01, // number of memories
      0x01, // limits flags: has max
      0x10, // min = 16 pages (LEB128)
      (byte) 0x80,
      0x02, // max = 256 pages (LEB128: 0x100)
      // Export section (section 7)
      0x07, // section id
      0x0A, // section size (10 bytes)
      0x01, // number of exports
      0x06, // name length
      'm',
      'e',
      'm',
      'o',
      'r',
      'y', // name: "memory"
      0x02, // export kind: memory
      0x00 // memory index: 0
    };
  }

  /**
   * Creates a WebAssembly module with zero-minimum memory.
   *
   * <pre>
   * (module
   *   (memory (export "memory") 0 100))
   * </pre>
   */
  private static byte[] createZeroMinMemoryModule() {
    return new byte[] {
      // Magic number and version
      0x00,
      0x61,
      0x73,
      0x6D, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version 1
      // Memory section (section 5)
      0x05, // section id
      0x04, // section size (4 bytes)
      0x01, // number of memories
      0x01, // limits flags: has max
      0x00, // min = 0 pages
      0x64, // max = 100 pages
      // Export section (section 7)
      0x07, // section id
      0x0A, // section size (10 bytes)
      0x01, // number of exports
      0x06, // name length
      'm',
      'e',
      'm',
      'o',
      'r',
      'y', // name: "memory"
      0x02, // export kind: memory
      0x00 // memory index: 0
    };
  }

  @Nested
  @DisplayName("Module.getMemoryType() Tests")
  class ModuleGetMemoryTypeTests {

    @Test
    @DisplayName("should get memory type with minimum only from module")
    void shouldGetMemoryTypeWithMinOnlyFromModule() throws Exception {
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

    @Test
    @DisplayName("should get memory type with min and max from module")
    void shouldGetMemoryTypeWithMinMaxFromModule() throws Exception {
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

    @Test
    @DisplayName("should get larger memory type from module")
    void shouldGetLargerMemoryTypeFromModule() throws Exception {
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

    @Test
    @DisplayName("should get zero-minimum memory type from module")
    void shouldGetZeroMinMemoryTypeFromModule() throws Exception {
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

    @Test
    @DisplayName("should return empty for non-existent memory")
    void shouldReturnEmptyForNonExistentMemory() throws Exception {
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
  @DisplayName("Instance.getMemoryType() Tests")
  class InstanceGetMemoryTypeTests {

    @Test
    @DisplayName("should get memory type from instance")
    void shouldGetMemoryTypeFromInstance() throws Exception {
      LOGGER.info("Testing Instance.getMemoryType()");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createMinMaxMemoryModule());
        final Instance instance = store.createInstance(module);

        final Optional<MemoryType> memoryTypeOpt = instance.getMemoryType("memory");

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

    @Test
    @DisplayName("should get all memory types from module")
    void shouldGetAllMemoryTypesFromModule() throws Exception {
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

    @Test
    @DisplayName("module and instance memory types should match")
    void moduleAndInstanceMemoryTypesShouldMatch() throws Exception {
      LOGGER.info("Testing consistency between module and instance memory types");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createMinMaxMemoryModule());
        final Instance instance = store.createInstance(module);

        final Optional<MemoryType> moduleMemoryType = module.getMemoryType("memory");
        final Optional<MemoryType> instanceMemoryType = instance.getMemoryType("memory");

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

    @Test
    @DisplayName("should handle memory with equal min and max")
    void shouldHandleMemoryWithEqualMinMax() throws Exception {
      LOGGER.info("Testing memory with equal min and max");

      // Create module with min = max = 5
      final byte[] wasmBytes =
          new byte[] {
            0x00,
            0x61,
            0x73,
            0x6D, // magic
            0x01,
            0x00,
            0x00,
            0x00, // version 1
            0x05, // memory section
            0x04, // section size
            0x01, // 1 memory
            0x01, // has max
            0x05, // min = 5
            0x05, // max = 5
            0x07, // export section
            0x0A, // section size
            0x01, // 1 export
            0x06,
            'm',
            'e',
            'm',
            'o',
            'r',
            'y', // name
            0x02, // memory export
            0x00 // index 0
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
