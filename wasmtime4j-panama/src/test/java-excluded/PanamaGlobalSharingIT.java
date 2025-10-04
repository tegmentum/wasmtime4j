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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

/**
 * Integration tests for Panama global cross-module sharing functionality.
 *
 * <p>These tests verify that globals can be shared between different WebAssembly modules within the
 * same store context, maintaining type safety and proper resource management.
 */
@EnabledOnJre(JRE.JAVA_23)
@DisplayName("Panama Global Sharing Integration Tests")
class PanamaGlobalSharingIT {

  private ArenaResourceManager resourceManager;
  private PanamaEngine engine;
  private PanamaStore store;
  private GlobalRegistry registry;

  @BeforeAll
  static void checkPanamaAvailability() {
    assumeTrue(
        PanamaTestUtils.isPanamaAvailable(),
        "Panama FFI not available - skipping Panama-specific tests");
  }

  @BeforeEach
  void setUp() throws Exception {
    resourceManager = new ArenaResourceManager();
    engine = new PanamaEngine(resourceManager);
    store = (PanamaStore) engine.createStore();
    registry = new GlobalRegistry(store);
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (resourceManager != null) {
      resourceManager.close();
    }
  }

  @Test
  @DisplayName("Global registry registration and lookup works")
  void testGlobalRegistryBasicOperations() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(42);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Test registration
      assertDoesNotThrow(
          () -> {
            registry.registerGlobal("shared_counter", global);
            assertThat(registry.getRegisteredCount()).isEqualTo(1);
            assertThat(registry.isRegistered("shared_counter")).isTrue();
            assertThat(registry.isRegistered("non_existent")).isFalse();
          },
          "Global registration should work");

      // Test lookup
      Optional<PanamaGlobal> lookedUp = registry.lookupGlobal("shared_counter");
      assertThat(lookedUp).isPresent();
      assertThat(lookedUp.get()).isSameAs(global);

      // Test non-existent lookup
      Optional<PanamaGlobal> notFound = registry.lookupGlobal("non_existent");
      assertThat(notFound).isEmpty();
    }
  }

  @Test
  @DisplayName("Global registry prevents duplicate registration")
  void testDuplicateRegistrationPrevention() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(100);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // First registration should succeed
      registry.registerGlobal("test_name", global);

      // Second registration with same name should fail
      assertThrows(
          WasmException.class,
          () -> {
            registry.registerGlobal("test_name", global);
          },
          "Duplicate registration should be prevented");
    }
  }

  @Test
  @DisplayName("Global registry unregistration works correctly")
  void testGlobalUnregistration() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(200);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Register and verify
      registry.registerGlobal("temp_global", global);
      assertThat(registry.isRegistered("temp_global")).isTrue();

      // Unregister and verify
      boolean removed = registry.unregisterGlobal("temp_global");
      assertThat(removed).isTrue();
      assertThat(registry.isRegistered("temp_global")).isFalse();

      // Second unregister should return false
      boolean removedAgain = registry.unregisterGlobal("temp_global");
      assertThat(removedAgain).isFalse();
    }
  }

  @Test
  @DisplayName("Global registry clears all registrations")
  void testGlobalRegistryClearing() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(300);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Register multiple globals
      registry.registerGlobal("global1", global);
      registry.registerGlobal("global2", global);
      registry.registerGlobal("global3", global);

      assertThat(registry.getRegisteredCount()).isEqualTo(3);

      // Clear and verify
      registry.clear();
      assertThat(registry.getRegisteredCount()).isEqualTo(0);
      assertThat(registry.isRegistered("global1")).isFalse();
      assertThat(registry.isRegistered("global2")).isFalse();
      assertThat(registry.isRegistered("global3")).isFalse();
    }
  }

  @Test
  @DisplayName("Shared global reference provides access to original global")
  void testSharedGlobalReference() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(500);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Create shared reference
      SharedGlobalReference sharedRef = global.createSharedReference();
      assertNotNull(sharedRef, "Shared reference should be created");

      // Test shared reference properties
      assertThat(sharedRef.getType()).isEqualTo(WasmValueType.I32);
      assertThat(sharedRef.isMutable()).isTrue();
      assertThat(sharedRef.getTypeName()).isEqualTo("i32");
      assertThat(sharedRef.getWasmType()).isEqualTo(MemoryLayouts.WASM_I32);
      assertThat(sharedRef.isValid()).isTrue();

      // Test that shared reference reflects original value
      WasmValue originalValue = global.get();
      WasmValue sharedValue = sharedRef.get();
      assertThat(sharedValue.asI32()).isEqualTo(originalValue.asI32());

      // Test that changes through shared reference affect original
      sharedRef.set(WasmValue.i32(777));
      WasmValue newOriginalValue = global.get();
      assertThat(newOriginalValue.asI32()).isEqualTo(777);

      sharedRef.close();
    }
  }

  @Test
  @DisplayName("Shared global reference handles original global lifecycle")
  void testSharedReferenceLifecycle() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(600);

    SharedGlobalReference sharedRef;

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      sharedRef = global.createSharedReference();
      assertThat(sharedRef.isValid()).isTrue();

      // Global is still alive, shared reference should work
      WasmValue value = sharedRef.get();
      assertThat(value.asI32()).isEqualTo(600);

      // Close the original global
      global.close();
    }

    // After original global is closed, shared reference should handle it gracefully
    assertThat(sharedRef.isValid()).isFalse();

    assertThrows(
        IllegalStateException.class,
        () -> {
          sharedRef.get();
        },
        "Shared reference should fail when original global is closed");

    sharedRef.close();
  }

  @Test
  @DisplayName("Global compatibility checking works correctly")
  void testGlobalCompatibilityChecking() throws Exception {
    byte[] wasmBytes1 = PanamaTestUtils.createModuleWithMutableI32Global(100);
    byte[] wasmBytes2 = PanamaTestUtils.createModuleWithImmutableI32Global(200);

    try (PanamaModule module1 = (PanamaModule) engine.compileModule(wasmBytes1);
        PanamaInstance instance1 = store.instantiateModule(module1);
        PanamaModule module2 = (PanamaModule) engine.compileModule(wasmBytes2);
        PanamaInstance instance2 = store.instantiateModule(module2)) {

      PanamaGlobal mutableGlobal = PanamaTestUtils.getGlobalFromInstance(instance1, "test_global");
      PanamaGlobal immutableGlobal =
          PanamaTestUtils.getGlobalFromInstance(instance2, "test_global");

      assertNotNull(mutableGlobal, "Mutable global should exist");
      assertNotNull(immutableGlobal, "Immutable global should exist");

      // Same type, different mutability - not compatible
      assertThat(mutableGlobal.isCompatibleForSharing(immutableGlobal)).isFalse();
      assertThat(immutableGlobal.isCompatibleForSharing(mutableGlobal)).isFalse();

      // Same global with itself - compatible
      assertThat(mutableGlobal.isCompatibleForSharing(mutableGlobal)).isTrue();
      assertThat(immutableGlobal.isCompatibleForSharing(immutableGlobal)).isTrue();

      // Null or closed global - not compatible
      assertThat(mutableGlobal.isCompatibleForSharing(null)).isFalse();

      immutableGlobal.close();
      assertThat(mutableGlobal.isCompatibleForSharing(immutableGlobal)).isFalse();
    }
  }

  @Test
  @DisplayName("Shared global reference supports zero-copy operations")
  void testSharedReferenceZeroCopy() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(888);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      try (SharedGlobalReference sharedRef = global.createSharedReference()) {
        // Test zero-copy operations through shared reference
        assertDoesNotThrow(
            () -> {
              Object rawValue = sharedRef.getZeroCopy();
              if (rawValue != null) {
                assertThat(rawValue).isInstanceOf(Integer.class);
                assertThat((Integer) rawValue).isEqualTo(888);

                // Test zero-copy set
                sharedRef.setZeroCopy(999);

                Object newRawValue = sharedRef.getZeroCopy();
                if (newRawValue != null) {
                  assertThat((Integer) newRawValue).isEqualTo(999);
                }

                // Verify original global also sees the change
                WasmValue originalValue = global.get();
                assertThat(originalValue.asI32()).isEqualTo(999);
              }
            },
            "Zero-copy operations should work through shared reference");

        // Test direct access support
        boolean supportsDirectAccess = sharedRef.supportsDirectAccess();
        if (supportsDirectAccess) {
          try (PanamaGlobal.DirectGlobalAccess directAccess = sharedRef.getDirectAccess()) {
            if (directAccess != null) {
              Object directValue = directAccess.readRawValue();
              assertThat(directValue).isInstanceOf(Integer.class);
            }
          }
        }
      }
    }
  }

  @Test
  @DisplayName("Global registration and sharing across multiple instances works")
  void testCrossInstanceGlobalSharing() throws Exception {
    // This test simulates sharing globals between multiple module instances
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(1000);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance1 = store.instantiateModule(module);
        PanamaInstance instance2 = store.instantiateModule(module)) {

      PanamaGlobal global1 = PanamaTestUtils.getGlobalFromInstance(instance1, "test_global");
      PanamaGlobal global2 = PanamaTestUtils.getGlobalFromInstance(instance2, "test_global");

      assertNotNull(global1, "Global1 should exist");
      assertNotNull(global2, "Global2 should exist");

      // Register first global
      registry.registerGlobal("shared_state", global1);

      // Lookup from registry should return the same global
      Optional<PanamaGlobal> lookedUp = registry.lookupGlobal("shared_state");
      assertThat(lookedUp).isPresent();
      assertThat(lookedUp.get()).isSameAs(global1);

      // Modify through registry lookup
      lookedUp.get().set(WasmValue.i32(1234));

      // Original global should reflect the change
      WasmValue originalValue = global1.get();
      assertThat(originalValue.asI32()).isEqualTo(1234);

      // Second instance global is independent
      WasmValue instance2Value = global2.get();
      assertThat(instance2Value.asI32()).isEqualTo(1000); // Original value
    }
  }
}
