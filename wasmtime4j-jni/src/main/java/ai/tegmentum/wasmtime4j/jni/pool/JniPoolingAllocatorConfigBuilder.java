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
package ai.tegmentum.wasmtime4j.jni.pool;

import ai.tegmentum.wasmtime4j.pool.AbstractPoolingAllocatorConfigBuilder;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;

/**
 * JNI implementation of pooling allocator configuration builder.
 *
 * @since 1.0.0
 */
public final class JniPoolingAllocatorConfigBuilder extends AbstractPoolingAllocatorConfigBuilder {

  @Override
  public PoolingAllocatorConfig build() {
    final JniPoolingAllocatorConfig config =
        new JniPoolingAllocatorConfig(
            instancePoolSize,
            maxMemoryPerInstance,
            stackSize,
            maxStacks,
            maxTablesPerInstance,
            maxTablesPerComponent,
            maxTables,
            memoryDecommitEnabled,
            poolWarmingEnabled,
            poolWarmingPercentage,
            totalCoreInstances,
            totalComponentInstances,
            maxCoreInstancesPerComponent,
            totalGcHeaps,
            maxMemorySize,
            maxUnusedWarmSlots,
            decommitBatchSize,
            linearMemoryKeepResident,
            tableKeepResident,
            asyncStackKeepResident,
            totalMemories,
            maxCoreInstanceSize,
            maxComponentInstanceSize,
            maxMemoriesPerModule,
            maxMemoriesPerComponent,
            tableElements,
            memoryProtectionKeysEnabled,
            maxMemoryProtectionKeys,
            pagemapScanEnabled);
    config.validate();
    return config;
  }
}
