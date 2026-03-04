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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Native function bindings for execution-related operations.
 *
 * <p>Provides type-safe wrappers for pooling allocator native functions.
 */
public final class NativeExecutionBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeExecutionBindings.class.getName());

  /** Initialization-on-demand holder for thread-safe lazy singleton. */
  private static final class Holder {
    static final NativeExecutionBindings INSTANCE = new NativeExecutionBindings();
  }

  private NativeExecutionBindings() {
    super();
    initializeBindings();
    markInitialized();
    LOGGER.fine("Initialized NativeExecutionBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeExecutionBindings getInstance() {
    return Holder.INSTANCE;
  }

  private void initializeBindings() {
    // Pooling Allocator Functions
    addFunctionBinding(
        "wasmtime4j_pooling_allocator_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return allocator*
            ValueLayout.JAVA_LONG, // instance_pool_size (usize)
            ValueLayout.JAVA_LONG, // max_memory_per_instance
            ValueLayout.JAVA_INT, // stack_size
            ValueLayout.JAVA_LONG, // max_stacks (usize)
            ValueLayout.JAVA_INT, // max_tables_per_instance
            ValueLayout.JAVA_LONG, // max_tables (usize)
            ValueLayout.JAVA_BOOLEAN, // memory_decommit_enabled
            ValueLayout.JAVA_BOOLEAN, // pool_warming_enabled
            ValueLayout.JAVA_FLOAT)); // pool_warming_percentage

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_allocate_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.ADDRESS)); // instance_id_out

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_reuse_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.JAVA_LONG)); // instance_id

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_release_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.JAVA_LONG)); // instance_id

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_get_statistics",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.ADDRESS)); // stats_out

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_reset_statistics",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS)); // allocator*

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_warm_pools",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS)); // allocator*

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_perform_maintenance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS)); // allocator*

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // allocator*
  }

  // ===== Pooling Allocator Functions =====

  /**
   * Creates a new pooling allocator with custom configuration.
   *
   * @param instancePoolSize the number of instances in the pool
   * @param maxMemoryPerInstance maximum memory per instance in bytes
   * @param stackSize stack size for WebAssembly execution
   * @param maxStacks maximum number of stacks
   * @param maxTablesPerInstance maximum tables per instance
   * @param maxTables maximum total tables
   * @param memoryDecommitEnabled whether memory decommit is enabled
   * @param poolWarmingEnabled whether pool warming is enabled
   * @param poolWarmingPercentage pool warming percentage (0.0 to 1.0)
   * @return pointer to the allocator, or null on failure
   */
  public MemorySegment poolingAllocatorCreateWithConfig(
      final int instancePoolSize,
      final long maxMemoryPerInstance,
      final int stackSize,
      final int maxStacks,
      final int maxTablesPerInstance,
      final int maxTables,
      final boolean memoryDecommitEnabled,
      final boolean poolWarmingEnabled,
      final float poolWarmingPercentage) {
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_create_with_config",
        MemorySegment.class,
        (long) instancePoolSize,
        maxMemoryPerInstance,
        stackSize,
        (long) maxStacks,
        maxTablesPerInstance,
        (long) maxTables,
        memoryDecommitEnabled,
        poolWarmingEnabled,
        poolWarmingPercentage);
  }

  /**
   * Creates a pooling allocator from JSON configuration bytes.
   *
   * <p>All fields in the JSON map directly to wasmtime's PoolingAllocationConfig API. This is the
   * preferred creation path as it wires all configuration options.
   *
   * @param jsonBytes UTF-8 JSON configuration bytes
   * @return pointer to the allocator, or null on failure
   */
  public MemorySegment poolingAllocatorCreateFromJson(final byte[] jsonBytes) {
    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment jsonSegment = localArena.allocateFrom(ValueLayout.JAVA_BYTE, jsonBytes);
      return callNativeFunction(
          "wasmtime4j_pooling_allocator_create_from_json",
          MemorySegment.class,
          jsonSegment,
          (long) jsonBytes.length);
    }
  }

  /**
   * Allocates an instance from the pool.
   *
   * @param allocatorPtr pointer to the allocator
   * @param instanceIdOut pointer to store the instance ID
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorAllocateInstance(
      final MemorySegment allocatorPtr, final MemorySegment instanceIdOut) {
    validatePointer(allocatorPtr, "allocatorPtr");
    validatePointer(instanceIdOut, "instanceIdOut");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_allocate_instance",
        Boolean.class,
        allocatorPtr,
        instanceIdOut);
  }

  /**
   * Reuses an existing instance from the pool.
   *
   * @param allocatorPtr pointer to the allocator
   * @param instanceId the instance ID to reuse
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorReuseInstance(
      final MemorySegment allocatorPtr, final long instanceId) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_reuse_instance", Boolean.class, allocatorPtr, instanceId);
  }

  /**
   * Releases an instance back to the pool.
   *
   * @param allocatorPtr pointer to the allocator
   * @param instanceId the instance ID to release
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorReleaseInstance(
      final MemorySegment allocatorPtr, final long instanceId) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_release_instance", Boolean.class, allocatorPtr, instanceId);
  }

  /**
   * Gets pool statistics.
   *
   * @param allocatorPtr pointer to the allocator
   * @param statsOut pointer to store the statistics
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorGetStatistics(
      final MemorySegment allocatorPtr, final MemorySegment statsOut) {
    validatePointer(allocatorPtr, "allocatorPtr");
    validatePointer(statsOut, "statsOut");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_get_statistics", Boolean.class, allocatorPtr, statsOut);
  }

  /**
   * Resets pool statistics.
   *
   * @param allocatorPtr pointer to the allocator
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorResetStatistics(final MemorySegment allocatorPtr) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_reset_statistics", Boolean.class, allocatorPtr);
  }

  /**
   * Warms up the pools by pre-allocating resources.
   *
   * @param allocatorPtr pointer to the allocator
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorWarmPools(final MemorySegment allocatorPtr) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_warm_pools", Boolean.class, allocatorPtr);
  }

  /**
   * Performs maintenance operations on the pools.
   *
   * @param allocatorPtr pointer to the allocator
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorPerformMaintenance(final MemorySegment allocatorPtr) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_perform_maintenance", Boolean.class, allocatorPtr);
  }

  /**
   * Destroys a pooling allocator.
   *
   * @param allocatorPtr pointer to the allocator
   */
  public void poolingAllocatorDestroy(final MemorySegment allocatorPtr) {
    if (allocatorPtr != null && !allocatorPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_pooling_allocator_destroy", Void.class, allocatorPtr);
    }
  }
}
