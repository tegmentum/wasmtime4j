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
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

/**
 * Integration tests for Panama global zero-copy access functionality.
 *
 * <p>These tests verify that zero-copy global operations work correctly and provide
 * performance benefits when direct memory access is available.
 */
@EnabledOnJre(JRE.JAVA_23)
@DisplayName("Panama Global Zero-Copy Integration Tests")
class PanamaGlobalZeroCopyIT {

  private PanamaEngine engine;
  private PanamaStore store;

  @BeforeAll
  static void checkPanamaAvailability() {
    assumeTrue(
        PanamaTestUtils.isPanamaAvailable(),
        "Panama FFI not available - skipping Panama-specific tests");
  }

  @BeforeEach
  void setUp() throws Exception {
    engine = new PanamaEngine();
    store = new PanamaStore(engine);
  }

  @Test
  @DisplayName("Zero-copy get operation works for numeric globals")
  void testZeroCopyGet() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(123);
    
    try (PanamaModule module = new PanamaModule(engine, wasmBytes);
         PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Test zero-copy get
      assertDoesNotThrow(() -> {
        Object rawValue = global.getZeroCopy();
        
        if (rawValue != null) {
          // If zero-copy is supported, verify the value
          assertThat(rawValue).isInstanceOf(Integer.class);
          assertThat((Integer) rawValue).isEqualTo(123);
        } else {
          // If zero-copy is not supported, it falls back gracefully
          // Verify fallback still works
          WasmValue wasmValue = global.get();
          assertThat(wasmValue.asI32()).isEqualTo(123);
        }
      }, "Zero-copy get should work or fallback gracefully");
    }
  }

  @Test
  @DisplayName("Zero-copy set operation works for mutable numeric globals")
  void testZeroCopySet() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(456);
    
    try (PanamaModule module = new PanamaModule(engine, wasmBytes);
         PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");
      assertThat(global.isMutable()).isTrue();

      // Test zero-copy set
      assertDoesNotThrow(() -> {
        global.setZeroCopy(789);
        
        // Verify the value was set
        WasmValue currentValue = global.get();
        assertThat(currentValue.asI32()).isEqualTo(789);
      }, "Zero-copy set should work or fallback gracefully");
    }
  }

  @Test
  @DisplayName("Zero-copy operations fail gracefully for immutable globals")
  void testZeroCopySetOnImmutableGlobal() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithImmutableI32Global(999);
    
    try (PanamaModule module = new PanamaModule(engine, wasmBytes);
         PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");
      assertThat(global.isMutable()).isFalse();

      // Zero-copy set should fail on immutable global
      assertThrows(UnsupportedOperationException.class, () -> {
        global.setZeroCopy(111);
      }, "Zero-copy set should fail on immutable global");
    }
  }

  @Test
  @DisplayName("Direct global access provides zero-copy operations")
  void testDirectGlobalAccess() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(555);
    
    try (PanamaModule module = new PanamaModule(engine, wasmBytes);
         PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Test direct access
      try (PanamaGlobal.DirectGlobalAccess directAccess = global.getDirectAccess()) {
        if (directAccess != null) {
          // Direct access is supported
          assertThat(directAccess.getWasmType()).isEqualTo(MemoryLayouts.WASM_I32);
          assertThat(directAccess.supports(Integer.class)).isTrue();
          assertThat(directAccess.supports(Long.class)).isFalse();

          // Test direct read
          Object readValue = directAccess.readRawValue();
          assertThat(readValue).isInstanceOf(Integer.class);
          assertThat((Integer) readValue).isEqualTo(555);

          // Test direct write
          directAccess.writeRawValue(777);
          
          // Verify write worked
          Object newValue = directAccess.readRawValue();
          assertThat((Integer) newValue).isEqualTo(777);

          // Verify regular access also sees the change
          WasmValue wasmValue = global.get();
          assertThat(wasmValue.asI32()).isEqualTo(777);
        } else {
          // Direct access not supported - test fallback
          Object rawValue = global.getZeroCopy();
          if (rawValue != null) {
            assertThat((Integer) rawValue).isEqualTo(555);
          }
        }
      }
    }
  }

  @Test
  @DisplayName("Direct access handles type validation correctly")
  void testDirectAccessTypeValidation() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(888);
    
    try (PanamaModule module = new PanamaModule(engine, wasmBytes);
         PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      try (PanamaGlobal.DirectGlobalAccess directAccess = global.getDirectAccess()) {
        if (directAccess != null) {
          // Test type validation on write
          assertThrows(IllegalArgumentException.class, () -> {
            directAccess.writeRawValue("invalid_type");
          }, "Direct access should validate types");

          assertThrows(IllegalArgumentException.class, () -> {
            directAccess.writeRawValue(123L); // Long instead of Integer for i32
          }, "Direct access should validate specific numeric types");
        }
      }
    }
  }

  @Test
  @DisplayName("Zero-copy operations work with different numeric types")
  void testZeroCopyWithDifferentTypes() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMultipleGlobals();
    
    try (PanamaModule module = new PanamaModule(engine, wasmBytes);
         PanamaInstance instance = store.instantiateModule(module)) {

      // Test i64 global
      PanamaGlobal i64Global = PanamaTestUtils.getGlobalFromInstance(instance, "i64_global");
      if (i64Global != null && i64Global.isMutable()) {
        assertDoesNotThrow(() -> {
          i64Global.setZeroCopy(123456789L);
          Object rawValue = i64Global.getZeroCopy();
          if (rawValue != null) {
            assertThat(rawValue).isInstanceOf(Long.class);
            assertThat((Long) rawValue).isEqualTo(123456789L);
          }
        }, "Zero-copy should work with i64");
      }

      // Test f32 global
      PanamaGlobal f32Global = PanamaTestUtils.getGlobalFromInstance(instance, "f32_global");
      if (f32Global != null && f32Global.isMutable()) {
        assertDoesNotThrow(() -> {
          f32Global.setZeroCopy(3.14f);
          Object rawValue = f32Global.getZeroCopy();
          if (rawValue != null) {
            assertThat(rawValue).isInstanceOf(Float.class);
            assertThat((Float) rawValue).isCloseTo(3.14f, org.assertj.core.data.Offset.offset(0.001f));
          }
        }, "Zero-copy should work with f32");
      }

      // Test f64 global
      PanamaGlobal f64Global = PanamaTestUtils.getGlobalFromInstance(instance, "f64_global");
      if (f64Global != null && f64Global.isMutable()) {
        assertDoesNotThrow(() -> {
          f64Global.setZeroCopy(2.71828);
          Object rawValue = f64Global.getZeroCopy();
          if (rawValue != null) {
            assertThat(rawValue).isInstanceOf(Double.class);
            assertThat((Double) rawValue).isCloseTo(2.71828, org.assertj.core.data.Offset.offset(0.00001));
          }
        }, "Zero-copy should work with f64");
      }
    }
  }

  @Test
  @DisplayName("Zero-copy operations handle closed globals gracefully")
  void testZeroCopyWithClosedGlobals() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(333);
    
    try (PanamaModule module = new PanamaModule(engine, wasmBytes);
         PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Close the global
      global.close();

      // Zero-copy operations should fail gracefully
      assertThrows(IllegalStateException.class, () -> {
        global.getZeroCopy();
      }, "Zero-copy get should fail on closed global");

      assertThrows(IllegalStateException.class, () -> {
        global.setZeroCopy(444);
      }, "Zero-copy set should fail on closed global");
    }
  }

  @Test
  @DisplayName("Direct access cleanup works correctly")
  void testDirectAccessCleanup() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(666);
    
    try (PanamaModule module = new PanamaModule(engine, wasmBytes);
         PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      PanamaGlobal.DirectGlobalAccess directAccess = global.getDirectAccess();
      
      if (directAccess != null) {
        // Use the direct access
        assertThat(directAccess.isClosed()).isFalse();
        Object value = directAccess.readRawValue();
        assertThat(value).isInstanceOf(Integer.class);

        // Close should be idempotent
        directAccess.close();
        assertThat(directAccess.isClosed()).isTrue();
        
        directAccess.close(); // Second close should not fail
        assertThat(directAccess.isClosed()).isTrue();

        // Operations should fail after close
        assertThrows(IllegalStateException.class, () -> {
          directAccess.readRawValue();
        }, "Read should fail after close");

        assertThrows(IllegalStateException.class, () -> {
          directAccess.writeRawValue(111);
        }, "Write should fail after close");
      }
    }
  }
}