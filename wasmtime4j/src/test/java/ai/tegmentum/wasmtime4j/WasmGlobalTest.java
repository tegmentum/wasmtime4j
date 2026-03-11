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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmGlobal} default methods.
 *
 * <p>These tests use anonymous implementations to exercise default method behavior without
 * requiring a native runtime.
 */
@DisplayName("WasmGlobal Default Method Tests")
class WasmGlobalTest {

  private WasmGlobal createTestGlobal(final WasmValue initialValue) {
    return new WasmGlobal() {
      private WasmValue currentValue = initialValue;

      @Override
      public WasmValue get() {
        return currentValue;
      }

      @Override
      public void set(final WasmValue value) {
        currentValue = value;
      }

      @Override
      public WasmValueType getType() {
        return initialValue.getType();
      }

      @Override
      public boolean isMutable() {
        return true;
      }

      @Override
      public GlobalType getGlobalType() {
        return new GlobalType() {
          @Override
          public WasmValueType getValueType() {
            return initialValue.getType();
          }

          @Override
          public boolean isMutable() {
            return true;
          }

          @Override
          public WasmTypeKind getKind() {
            return WasmTypeKind.GLOBAL;
          }
        };
      }
    };
  }

  @Nested
  @DisplayName("getValue() Default Method")
  class GetValueTests {

    @Test
    @DisplayName("getValue() should delegate to get()")
    void getValueShouldDelegateToGet() {
      final WasmGlobal global = createTestGlobal(WasmValue.i32(42));
      assertEquals(42, global.getValue().asInt());
      assertEquals(global.get(), global.getValue());
    }

    @Test
    @DisplayName("getValue() should reflect mutations via set()")
    void getValueShouldReflectMutations() {
      final WasmGlobal global = createTestGlobal(WasmValue.i32(10));
      assertEquals(10, global.getValue().asInt());
      global.set(WasmValue.i32(99));
      assertEquals(99, global.getValue().asInt());
    }

    @Test
    @DisplayName("getValue() should work with i64 type")
    void getValueShouldWorkWithI64() {
      final WasmGlobal global = createTestGlobal(WasmValue.i64(Long.MAX_VALUE));
      assertEquals(Long.MAX_VALUE, global.getValue().asLong());
    }

    @Test
    @DisplayName("getValue() should work with f32 type")
    void getValueShouldWorkWithF32() {
      final WasmGlobal global = createTestGlobal(WasmValue.f32(3.14f));
      assertEquals(3.14f, global.getValue().asFloat(), 0.001f);
    }

    @Test
    @DisplayName("getValue() should work with f64 type")
    void getValueShouldWorkWithF64() {
      final WasmGlobal global = createTestGlobal(WasmValue.f64(2.718));
      assertEquals(2.718, global.getValue().asDouble(), 0.001);
    }
  }
}
