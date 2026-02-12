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

package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Fuzz tests for EngineConfig and Engine feature combinations.
 *
 * <p>This fuzzer tests the robustness of engine configuration by:
 *
 * <ul>
 *   <li>Random boolean toggle combinations for all wasm features
 *   <li>Extreme numeric values for memory and pool configuration
 *   <li>Feature query consistency after engine creation
 *   <li>Multiple engines with different configs compiling the same module
 * </ul>
 *
 * @since 1.0.0
 */
public class EngineConfigFuzzer {

  private static final String SIMPLE_MODULE_WAT =
      """
      (module
        (func $add (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add)
        (export "add" (func $add))
      )
      """;

  /**
   * Builds an EngineConfig with random boolean toggles for various features.
   *
   * <p>Creates an engine from the config, verifies isValid(), queries each feature with
   * supportsFeature(), and closes. No crash should occur regardless of the combination.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzRandomConfigCombinations(final FuzzedDataProvider data) {
    try {
      final EngineConfig config = new EngineConfig();

      // Apply random boolean toggles
      config.consumeFuel(data.consumeBoolean());
      config.setEpochInterruption(data.consumeBoolean());
      config.parallelCompilation(data.consumeBoolean());

      // Wasm feature toggles via individual setters
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.THREADS);
      }
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.MEMORY64);
      }
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.SIMD);
      }
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.BULK_MEMORY);
      }
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.MULTI_VALUE);
      }
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.REFERENCE_TYPES);
      }
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.RELAXED_SIMD);
      }
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.WIDE_ARITHMETIC);
      }
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.TAIL_CALL);
      }
      if (data.consumeBoolean()) {
        config.addWasmFeature(WasmFeature.MULTI_MEMORY);
      }

      config.debugInfo(data.consumeBoolean());
      config.craneliftDebugVerifier(data.consumeBoolean());
      config.coredumpOnTrap(data.consumeBoolean());

      try (Engine engine = Engine.create(config)) {
        engine.isValid();

        // Query features
        for (final WasmFeature feature : WasmFeature.values()) {
          try {
            engine.supportsFeature(feature);
          } catch (UnsupportedOperationException e) {
            // Expected: some features may not be queryable
          }
        }
      }
    } catch (WasmException e) {
      // Expected: incompatible config combinations
    } catch (IllegalArgumentException e) {
      // Expected: invalid config values
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets memory reservation, memory guard size, and instance pool size with fuzzed extreme values.
   *
   * <p>Values include 0, negatives, and very large numbers. Engine creation should either succeed
   * or throw cleanly.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzExtremeNumericConfig(final FuzzedDataProvider data) {
    final long memoryReservation = data.consumeLong();
    final long memoryGuardSize = data.consumeLong();
    final int instancePoolSize = data.consumeInt();

    try {
      final EngineConfig config = new EngineConfig();

      try {
        config.memoryReservation(memoryReservation);
      } catch (IllegalArgumentException e) {
        // Expected: negative or too large
      }

      try {
        config.memoryGuardSize(memoryGuardSize);
      } catch (IllegalArgumentException e) {
        // Expected: negative or too large
      }

      try {
        config.setInstancePoolSize(instancePoolSize);
      } catch (IllegalArgumentException e) {
        // Expected: negative or zero
      }

      try {
        config.setMaxWasmStack(data.consumeLong());
      } catch (IllegalArgumentException e) {
        // Expected
      }

      try {
        config.setAsyncStackSize(data.consumeLong());
      } catch (IllegalArgumentException e) {
        // Expected
      }

      try {
        config.memoryReservationForGrowth(data.consumeLong());
      } catch (IllegalArgumentException e) {
        // Expected
      }

      try (Engine engine = Engine.create(config)) {
        engine.isValid();
      }
    } catch (WasmException e) {
      // Expected: invalid config
    } catch (IllegalArgumentException e) {
      // Expected
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates an engine with random config, iterates all WasmFeature enum values calling
   * supportsFeature().
   *
   * <p>Verifies no crash and consistent responses across repeated queries.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzEngineFeatureQuery(final FuzzedDataProvider data) {
    try {
      final EngineConfig config = new EngineConfig();
      config.consumeFuel(data.consumeBoolean());
      config.setEpochInterruption(data.consumeBoolean());
      config.parallelCompilation(data.consumeBoolean());
      config.debugInfo(data.consumeBoolean());

      try (Engine engine = Engine.create(config)) {
        final WasmFeature[] features = WasmFeature.values();

        // Query all features twice and verify consistency
        for (final WasmFeature feature : features) {
          try {
            final boolean first = engine.supportsFeature(feature);
            final boolean second = engine.supportsFeature(feature);
            if (first != second) {
              throw new AssertionError(
                  "Inconsistent feature query for " + feature + ": " + first + " vs " + second);
            }
          } catch (UnsupportedOperationException e) {
            // Expected: some features may not be queryable
          }
        }

        // Also verify other engine properties
        engine.isFuelEnabled();
        engine.isEpochInterruptionEnabled();
        engine.getMemoryLimitPages();
        engine.getStackSizeLimit();
      }
    } catch (WasmException e) {
      // Expected
    } catch (IllegalArgumentException e) {
      // Expected
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates multiple engines with different random configs and compiles the same WAT module on
   * each.
   *
   * <p>Verifies all succeed or throw consistently. Tests config isolation between engines.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzMultipleEngineConfigs(final FuzzedDataProvider data) {
    final int engineCount = data.consumeInt(2, 5);

    for (int i = 0; i < engineCount; i++) {
      try {
        final EngineConfig config = new EngineConfig();
        config.consumeFuel(data.consumeBoolean());
        config.parallelCompilation(data.consumeBoolean());
        config.debugInfo(data.consumeBoolean());
        config.craneliftDebugVerifier(data.consumeBoolean());

        try (Engine engine = Engine.create(config);
            Module module = engine.compileWat(SIMPLE_MODULE_WAT)) {
          // Verify module is valid
          module.getExports();
          module.isValid();
        }
      } catch (WasmException e) {
        // Expected: some configs may not compile
      } catch (IllegalArgumentException e) {
        // Expected
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
