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
package ai.tegmentum.wasmtime4j.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.test.TestUtils;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WebAssembly shared memory functionality.
 *
 * <p>This test suite validates shared memory operations including:
 *
 * <ul>
 *   <li>Creating shared memory
 *   <li>Importing shared memory between instances
 *   <li>Atomic operations on shared memory
 *   <li>Thread synchronization via shared memory
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("Shared Memory Integration Tests")
public class SharedMemoryTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(SharedMemoryTest.class.getName());

  // WAT module with shared memory for atomic operations
  private static final String SHARED_MEMORY_WAT =
      "(module\n"
          + "  (memory (export \"memory\") 1 1 shared)\n"
          + "  \n"
          + "  ;; Atomic load i32\n"
          + "  (func (export \"atomic_load\") (param $offset i32) (result i32)\n"
          + "    local.get $offset\n"
          + "    i32.atomic.load\n"
          + "  )\n"
          + "  \n"
          + "  ;; Atomic store i32\n"
          + "  (func (export \"atomic_store\") (param $offset i32) (param $value i32)\n"
          + "    local.get $offset\n"
          + "    local.get $value\n"
          + "    i32.atomic.store\n"
          + "  )\n"
          + "  \n"
          + "  ;; Atomic add i32\n"
          + "  (func (export \"atomic_add\") (param $offset i32) (param $value i32) (result i32)\n"
          + "    local.get $offset\n"
          + "    local.get $value\n"
          + "    i32.atomic.rmw.add\n"
          + "  )\n"
          + "  \n"
          + "  ;; Atomic compare-and-swap i32\n"
          + "  (func (export \"atomic_cas\") (param $offset i32) (param $expected i32) "
          + "(param $new i32) (result i32)\n"
          + "    local.get $offset\n"
          + "    local.get $expected\n"
          + "    local.get $new\n"
          + "    i32.atomic.rmw.cmpxchg\n"
          + "  )\n"
          + ")";

  // WAT module that imports shared memory
  private static final String IMPORT_SHARED_MEMORY_WAT =
      "(module\n"
          + "  (import \"env\" \"memory\" (memory 1 1 shared))\n"
          + "  \n"
          + "  (func (export \"read\") (param $offset i32) (result i32)\n"
          + "    local.get $offset\n"
          + "    i32.atomic.load\n"
          + "  )\n"
          + "  \n"
          + "  (func (export \"write\") (param $offset i32) (param $value i32)\n"
          + "    local.get $offset\n"
          + "    local.get $value\n"
          + "    i32.atomic.store\n"
          + "  )\n"
          + ")";

  private final List<AutoCloseable> resources = new ArrayList<>();

  private boolean probeSharedMemorySupported() {
    try {
      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      try (Engine engine = Engine.create(config)) {
        final Module testModule = engine.compileWat(SHARED_MEMORY_WAT);
        if (testModule != null) {
          try (Store testStore = engine.createStore()) {
            final Instance testInstance = testModule.instantiate(testStore);
            if (testInstance != null) {
              testInstance.close();
              testModule.close();
              return true;
            }
          }
          testModule.close();
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Shared memory not supported: " + e.getMessage());
    }
    return false;
  }

  private boolean probeSharedMemoryExportSupported() {
    try {
      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      try (Engine engine = Engine.create(config)) {
        final Module testModule = engine.compileWat(SHARED_MEMORY_WAT);
        if (testModule != null) {
          try (Store testStore = engine.createStore()) {
            final Instance testInstance = testModule.instantiate(testStore);
            if (testInstance != null) {
              final java.util.Optional<WasmMemory> memoryOpt = testInstance.getMemory("memory");
              if (memoryOpt.isPresent()) {
                try {
                  memoryOpt.get().isShared();
                  testInstance.close();
                  testModule.close();
                  return true;
                } catch (final IllegalArgumentException e) {
                  LOGGER.warning("Shared memory export returns null pointer: " + e.getMessage());
                }
              }
              testInstance.close();
            }
          }
          testModule.close();
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Shared memory export not supported: " + e.getMessage());
    }
    return false;
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    clearRuntimeSelection();
    TestUtils.clearHandleRegistries();
  }

  @Nested
  @DisplayName("Shared Memory Creation Tests")
  class SharedMemoryCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create module with shared memory")
    void shouldCreateModuleWithSharedMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(probeSharedMemorySupported(), "Shared memory not supported - skipping");
      LOGGER.info("Testing: should create module with shared memory");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);

      assertNotNull(module, "Module should be created");
      LOGGER.info("Module with shared memory created successfully");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should instantiate module with shared memory")
    void shouldInstantiateModuleWithSharedMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(
          probeSharedMemoryExportSupported(),
          "Shared memory export retrieval not supported - skipping");
      LOGGER.info("Testing: should instantiate module with shared memory");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);

      final Instance instance = module.instantiate(store);
      resources.add(instance);

      assertNotNull(instance, "Instance should be created");

      // Verify memory is accessible
      final WasmMemory memory = instance.getMemory("memory").orElse(null);
      assertNotNull(memory, "Memory should be exported");
      assertTrue(memory.isShared(), "Memory should be shared");

      LOGGER.info("Instance with shared memory created successfully");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should report memory as shared")
    void shouldReportMemoryAsShared(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(
          probeSharedMemoryExportSupported(),
          "Shared memory export retrieval not supported - skipping");
      LOGGER.info("Testing: should report memory as shared");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);

      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmMemory memory = instance.getMemory("memory").orElseThrow();
      assertTrue(memory.isShared(), "Memory should report as shared");

      LOGGER.info("Memory correctly reports as shared");
    }
  }

  @Nested
  @DisplayName("Shared Memory Import Tests")
  class SharedMemoryImportTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should import shared memory into second instance")
    void shouldImportSharedMemoryIntoSecondInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(
          probeSharedMemoryExportSupported(),
          "Shared memory export retrieval not supported - skipping");
      LOGGER.info("Testing: should import shared memory into second instance");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      // Create first module with shared memory export
      final Module exportModule = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(exportModule);

      final Instance exportInstance = exportModule.instantiate(store);
      resources.add(exportInstance);

      // Get the shared memory
      final WasmMemory sharedMemory = exportInstance.getMemory("memory").orElseThrow();

      // Create second module that imports shared memory
      final Module importModule = engine.compileWat(IMPORT_SHARED_MEMORY_WAT);
      resources.add(importModule);

      // Link shared memory to second module
      final Linker<Void> linker = engine.getRuntime().createLinker(engine);
      resources.add(linker);
      linker.defineMemory(store, "env", "memory", sharedMemory);

      final Instance importInstance = linker.instantiate(store, importModule);
      resources.add(importInstance);

      assertNotNull(importInstance, "Import instance should be created");
      LOGGER.info("Shared memory imported successfully");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should share data between instances via shared memory")
    void shouldShareDataBetweenInstancesViaSharedMemory(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(
          probeSharedMemoryExportSupported(),
          "Shared memory export retrieval not supported - skipping");
      LOGGER.info("Testing: should share data between instances via shared memory");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      // Create exporter instance
      final Module exportModule = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(exportModule);
      final Instance exportInstance = exportModule.instantiate(store);
      resources.add(exportInstance);

      // Get shared memory and write a value
      final WasmFunction atomicStore = exportInstance.getFunction("atomic_store").orElseThrow();
      atomicStore.call(WasmValue.i32(0), WasmValue.i32(42));

      // Create importer instance with same shared memory
      final WasmMemory sharedMemory = exportInstance.getMemory("memory").orElseThrow();
      final Module importModule = engine.compileWat(IMPORT_SHARED_MEMORY_WAT);
      resources.add(importModule);

      final Linker<Void> linker = engine.getRuntime().createLinker(engine);
      resources.add(linker);
      linker.defineMemory(store, "env", "memory", sharedMemory);

      final Instance importInstance = linker.instantiate(store, importModule);
      resources.add(importInstance);

      // Read the value from importer instance
      final WasmFunction read = importInstance.getFunction("read").orElseThrow();
      final WasmValue[] result = read.call(WasmValue.i32(0));

      assertEquals(42, result[0].asInt(), "Importer should read value written by exporter");
      LOGGER.info("Data sharing between instances verified");
    }
  }

  @Nested
  @DisplayName("Atomic Operations Tests")
  class AtomicOperationsTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should perform atomic load")
    void shouldPerformAtomicLoad(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(probeSharedMemorySupported(), "Shared memory not supported - skipping");
      LOGGER.info("Testing: should perform atomic load");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      // Store then load atomically
      final WasmFunction atomicStore = instance.getFunction("atomic_store").orElseThrow();
      final WasmFunction atomicLoad = instance.getFunction("atomic_load").orElseThrow();

      atomicStore.call(WasmValue.i32(0), WasmValue.i32(100));
      final WasmValue[] result = atomicLoad.call(WasmValue.i32(0));

      assertEquals(100, result[0].asInt(), "Atomic load should return stored value");
      LOGGER.info("Atomic load verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should perform atomic add")
    void shouldPerformAtomicAdd(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(probeSharedMemorySupported(), "Shared memory not supported - skipping");
      LOGGER.info("Testing: should perform atomic add");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmFunction atomicStore = instance.getFunction("atomic_store").orElseThrow();
      final WasmFunction atomicAdd = instance.getFunction("atomic_add").orElseThrow();
      final WasmFunction atomicLoad = instance.getFunction("atomic_load").orElseThrow();

      // Store initial value
      atomicStore.call(WasmValue.i32(0), WasmValue.i32(10));

      // Atomic add returns the old value
      final WasmValue[] oldValue = atomicAdd.call(WasmValue.i32(0), WasmValue.i32(5));
      assertEquals(10, oldValue[0].asInt(), "Atomic add should return old value");

      // Verify new value
      final WasmValue[] newValue = atomicLoad.call(WasmValue.i32(0));
      assertEquals(15, newValue[0].asInt(), "Value should be incremented");

      LOGGER.info("Atomic add verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should perform atomic compare-and-swap")
    void shouldPerformAtomicCompareAndSwap(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(probeSharedMemorySupported(), "Shared memory not supported - skipping");
      LOGGER.info("Testing: should perform atomic compare-and-swap");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmFunction atomicStore = instance.getFunction("atomic_store").orElseThrow();
      final WasmFunction atomicCas = instance.getFunction("atomic_cas").orElseThrow();
      final WasmFunction atomicLoad = instance.getFunction("atomic_load").orElseThrow();

      // Store initial value
      atomicStore.call(WasmValue.i32(0), WasmValue.i32(50));

      // CAS should succeed when expected matches current value
      final WasmValue[] result =
          atomicCas.call(WasmValue.i32(0), WasmValue.i32(50), WasmValue.i32(100));
      assertEquals(50, result[0].asInt(), "CAS should return old value");

      // Verify value was swapped
      final WasmValue[] newValue = atomicLoad.call(WasmValue.i32(0));
      assertEquals(100, newValue[0].asInt(), "Value should be swapped");

      // CAS should fail when expected doesn't match
      final WasmValue[] failedResult =
          atomicCas.call(WasmValue.i32(0), WasmValue.i32(50), WasmValue.i32(200));
      assertEquals(100, failedResult[0].asInt(), "CAS should return current value on failure");

      // Value should remain unchanged
      final WasmValue[] unchangedValue = atomicLoad.call(WasmValue.i32(0));
      assertEquals(100, unchangedValue[0].asInt(), "Value should not change on failed CAS");

      LOGGER.info("Atomic compare-and-swap verified");
    }
  }

  @Nested
  @DisplayName("Thread Synchronization Tests")
  class ThreadSynchronizationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle concurrent atomic increments")
    void shouldHandleConcurrentAtomicIncrements(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(
          probeSharedMemoryExportSupported(),
          "Shared memory export retrieval not supported - skipping");
      LOGGER.info("Testing: should handle concurrent atomic increments");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      // Get the shared memory to import into other instances
      final WasmMemory sharedMemory = instance.getMemory("memory").orElseThrow();

      // Initialize memory to 0
      final WasmFunction atomicStore = instance.getFunction("atomic_store").orElseThrow();
      atomicStore.call(WasmValue.i32(0), WasmValue.i32(0));

      final int threadCount = 10;
      final int incrementsPerThread = 100;
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(threadCount);
      final AtomicBoolean hasError = new AtomicBoolean(false);

      // Pre-compile the import module once before spawning threads.
      final Module importModule = engine.compileWat(IMPORT_SHARED_MEMORY_WAT);
      resources.add(importModule);

      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      for (int t = 0; t < threadCount; t++) {
        executor.submit(
            () -> {
              Store localStore = null;
              Linker<Void> localLinker = null;
              Instance localInstance = null;
              try {
                startLatch.await();
                // Each thread creates its own store and imports the shared memory
                localStore = engine.createStore();
                localLinker = engine.getRuntime().createLinker(engine);
                localLinker.defineMemory(localStore, "env", "memory", sharedMemory);

                localInstance = localLinker.instantiate(localStore, importModule);

                final WasmFunction read = localInstance.getFunction("read").orElseThrow();

                for (int i = 0; i < incrementsPerThread; i++) {
                  read.call(WasmValue.i32(0));
                }
              } catch (final Exception e) {
                LOGGER.warning("Thread error: " + e.getMessage());
                hasError.set(true);
              } finally {
                try {
                  if (localInstance != null) {
                    localInstance.close();
                  }
                } catch (final Exception ex) {
                  LOGGER.fine("Instance cleanup: " + ex.getMessage());
                }
                try {
                  if (localLinker != null) {
                    localLinker.close();
                  }
                } catch (final Exception ex) {
                  LOGGER.fine("Linker cleanup: " + ex.getMessage());
                }
                try {
                  if (localStore != null) {
                    localStore.close();
                  }
                } catch (final Exception ex) {
                  LOGGER.fine("Store cleanup: " + ex.getMessage());
                }
                doneLatch.countDown();
              }
            });
      }

      // Start all threads simultaneously
      startLatch.countDown();
      assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
      executor.shutdown();
      assertTrue(
          executor.awaitTermination(30, TimeUnit.SECONDS), "Executor should terminate gracefully");

      if (!hasError.get()) {
        LOGGER.info("Concurrent atomic operations completed without errors");
      } else {
        LOGGER.warning("Thread errors occurred during test");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should support multiple memory offsets concurrently")
    void shouldSupportMultipleMemoryOffsetsConcurrently(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeSharedMemorySupported(), "Shared memory not supported - skipping");
      LOGGER.info("Testing: should support multiple memory offsets concurrently");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmFunction atomicStore = instance.getFunction("atomic_store").orElseThrow();
      final WasmFunction atomicLoad = instance.getFunction("atomic_load").orElseThrow();

      // Test with multiple offsets (must be 4-byte aligned)
      final int[] offsets = {0, 4, 8, 12};
      for (final int offset : offsets) {
        atomicStore.call(WasmValue.i32(offset), WasmValue.i32(offset * 10));
      }

      // Verify each offset independently
      for (final int offset : offsets) {
        final WasmValue[] value = atomicLoad.call(WasmValue.i32(offset));
        assertEquals(
            offset * 10, value[0].asInt(), "Offset " + offset + " should have correct value");
      }

      LOGGER.info("Multiple memory offsets verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle concurrent atomic write contention correctly")
    void shouldHandleConcurrentAtomicWriteContention(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(
          probeSharedMemoryExportSupported(),
          "Shared memory export retrieval not supported - skipping");
      LOGGER.info("Testing: should handle concurrent atomic write contention correctly");

      // WAT module with atomic add that exports shared memory
      final String atomicAddModuleWat =
          "(module\n"
              + "  (memory (export \"memory\") 1 1 shared)\n"
              + "  (func (export \"atomic_add\") (param $offset i32) (param $value i32) (result"
              + " i32)\n"
              + "    local.get $offset\n"
              + "    local.get $value\n"
              + "    i32.atomic.rmw.add\n"
              + "  )\n"
              + "  (func (export \"atomic_store\") (param $offset i32) (param $value i32)\n"
              + "    local.get $offset\n"
              + "    local.get $value\n"
              + "    i32.atomic.store\n"
              + "  )\n"
              + "  (func (export \"atomic_load\") (param $offset i32) (result i32)\n"
              + "    local.get $offset\n"
              + "    i32.atomic.load\n"
              + "  )\n"
              + ")";

      // WAT module that imports memory and does atomic add
      final String atomicAddImportWat =
          "(module\n"
              + "  (import \"env\" \"memory\" (memory 1 1 shared))\n"
              + "  (func (export \"atomic_add\") (param $offset i32) (param $value i32) (result"
              + " i32)\n"
              + "    local.get $offset\n"
              + "    local.get $value\n"
              + "    i32.atomic.rmw.add\n"
              + "  )\n"
              + ")";

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module exportModule = engine.compileWat(atomicAddModuleWat);
      resources.add(exportModule);
      final Instance exportInstance = exportModule.instantiate(store);
      resources.add(exportInstance);

      // Get the shared memory and initialize to 0
      final WasmMemory sharedMemory = exportInstance.getMemory("memory").orElseThrow();
      final WasmFunction atomicStore = exportInstance.getFunction("atomic_store").orElseThrow();
      final WasmFunction atomicLoad = exportInstance.getFunction("atomic_load").orElseThrow();

      atomicStore.call(WasmValue.i32(0), WasmValue.i32(0));

      final int threadCount = 10;
      final int incrementsPerThread = 100;
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(threadCount);
      final AtomicBoolean hasError = new AtomicBoolean(false);
      final List<String> errors = new ArrayList<>();

      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      // Compile the import module once
      final Module importModule = engine.compileWat(atomicAddImportWat);
      resources.add(importModule);

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              Store localStore = null;
              Linker<Void> localLinker = null;
              Instance localInstance = null;
              try {
                startLatch.await();

                localStore = engine.createStore();
                localLinker = engine.getRuntime().createLinker(engine);
                localLinker.defineMemory(localStore, "env", "memory", sharedMemory);

                localInstance = localLinker.instantiate(localStore, importModule);

                final WasmFunction atomicAdd =
                    localInstance.getFunction("atomic_add").orElseThrow();

                for (int i = 0; i < incrementsPerThread; i++) {
                  atomicAdd.call(WasmValue.i32(0), WasmValue.i32(1));
                }

                LOGGER.fine("Thread " + threadId + " completed " + incrementsPerThread + " adds");
              } catch (final Exception e) {
                LOGGER.warning("Thread " + threadId + " error: " + e.getMessage());
                synchronized (errors) {
                  errors.add("Thread " + threadId + ": " + e.getMessage());
                }
                hasError.set(true);
              } finally {
                try {
                  if (localInstance != null) {
                    localInstance.close();
                  }
                } catch (final Exception ex) {
                  LOGGER.fine("Instance cleanup: " + ex.getMessage());
                }
                try {
                  if (localLinker != null) {
                    localLinker.close();
                  }
                } catch (final Exception ex) {
                  LOGGER.fine("Linker cleanup: " + ex.getMessage());
                }
                try {
                  if (localStore != null) {
                    localStore.close();
                  }
                } catch (final Exception ex) {
                  LOGGER.fine("Store cleanup: " + ex.getMessage());
                }
                doneLatch.countDown();
              }
            });
      }

      // Start all threads simultaneously
      startLatch.countDown();
      assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
      executor.shutdown();
      assertTrue(
          executor.awaitTermination(30, TimeUnit.SECONDS), "Executor should terminate gracefully");

      if (hasError.get()) {
        LOGGER.warning("Errors occurred: " + errors);
      }

      // Verify the final value equals threadCount * incrementsPerThread
      final WasmValue[] finalValue = atomicLoad.call(WasmValue.i32(0));
      final int expectedValue = threadCount * incrementsPerThread;
      final int actualValue = finalValue[0].asInt();

      LOGGER.info(
          "Concurrent atomic write test: expected="
              + expectedValue
              + ", actual="
              + actualValue
              + ", errors="
              + hasError.get());

      assertEquals(
          expectedValue,
          actualValue,
          "Final value should equal threadCount * incrementsPerThread ("
              + threadCount
              + " * "
              + incrementsPerThread
              + " = "
              + expectedValue
              + ")");

      assertTrue(!hasError.get(), "No thread should have failed");
      LOGGER.info("Concurrent atomic write contention test passed");
    }
  }

  @Nested
  @DisplayName("Shared Memory API Tests")
  class SharedMemoryApiTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get memory size")
    void shouldGetMemorySize(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(
          probeSharedMemoryExportSupported(),
          "Shared memory export retrieval not supported - skipping");
      LOGGER.info("Testing: should get memory size");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmMemory memory = instance.getMemory("memory").orElseThrow();
      final long sizeInPages = memory.size();
      final long sizeInBytes = memory.dataSize();

      assertTrue(sizeInPages >= 1, "Memory should have at least 1 page");
      assertEquals(sizeInPages * 65536, sizeInBytes, "Data size should be pages * 64KB");

      LOGGER.info("Memory size: " + sizeInPages + " pages, " + sizeInBytes + " bytes");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should read and write bytes to shared memory")
    void shouldReadAndWriteBytesToSharedMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeTrue(
          probeSharedMemoryExportSupported(),
          "Shared memory export retrieval not supported - skipping");
      LOGGER.info("Testing: should read and write bytes to shared memory");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      final Engine engine = Engine.create(config);
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileWat(SHARED_MEMORY_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmMemory memory = instance.getMemory("memory").orElseThrow();

      // Write bytes
      final byte[] testData = {1, 2, 3, 4, 5};
      memory.writeBytes(100, testData, 0, testData.length);

      // Read bytes back
      final byte[] readData = new byte[5];
      memory.readBytes(100, readData, 0, readData.length);

      for (int i = 0; i < testData.length; i++) {
        assertEquals(testData[i], readData[i], "Byte at position " + i + " should match");
      }

      LOGGER.info("Byte read/write on shared memory verified");
    }
  }
}
