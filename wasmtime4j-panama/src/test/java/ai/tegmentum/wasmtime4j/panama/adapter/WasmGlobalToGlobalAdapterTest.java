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

package ai.tegmentum.wasmtime4j.panama.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Global;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Panama {@link WasmGlobalToGlobalAdapter} class.
 *
 * <p>This test class verifies the adapter that bridges WasmGlobal to Global interface.
 */
@DisplayName("Panama WasmGlobalToGlobalAdapter Tests")
class WasmGlobalToGlobalAdapterTest {

  /**
   * Creates a mock WasmGlobal for testing.
   */
  private WasmGlobal createMockGlobal(
      final WasmValueType type,
      final boolean mutable,
      final WasmValue initialValue) {
    return new WasmGlobal() {
      private WasmValue value = initialValue;

      @Override
      public WasmValue get() {
        return value;
      }

      @Override
      public void set(final WasmValue newValue) {
        if (!mutable) {
          throw new RuntimeException("Cannot set immutable global");
        }
        this.value = newValue;
      }

      @Override
      public WasmValueType getType() {
        return type;
      }

      @Override
      public boolean isMutable() {
        return mutable;
      }
    };
  }

  /**
   * Creates a mock WasmGlobal that throws on get().
   */
  private WasmGlobal createFailingGlobal() {
    return new WasmGlobal() {
      @Override
      public WasmValue get() {
        throw new RuntimeException("Global access failed");
      }

      @Override
      public void set(final WasmValue newValue) {
        throw new RuntimeException("Global access failed");
      }

      @Override
      public WasmValueType getType() {
        return WasmValueType.I32;
      }

      @Override
      public boolean isMutable() {
        return true;
      }
    };
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasmGlobalToGlobalAdapter should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasmGlobalToGlobalAdapter.class.getModifiers()),
          "WasmGlobalToGlobalAdapter should be final");
    }

    @Test
    @DisplayName("WasmGlobalToGlobalAdapter should implement Global")
    void shouldImplementGlobal() {
      assertTrue(
          Global.class.isAssignableFrom(WasmGlobalToGlobalAdapter.class),
          "WasmGlobalToGlobalAdapter should implement Global");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid delegate")
    void constructorShouldAcceptValidDelegate() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(0));

      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertSame(delegate, adapter.getDelegate(), "Delegate should match");
    }

    @Test
    @DisplayName("Constructor should throw on null delegate")
    void constructorShouldThrowOnNullDelegate() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasmGlobalToGlobalAdapter(null),
          "Should throw on null delegate");
    }
  }

  @Nested
  @DisplayName("getValue Tests")
  class GetValueTests {

    @Test
    @DisplayName("getValue should return integer value")
    void getValueShouldReturnIntegerValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(42));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(42, adapter.getValue(), "Value should be 42");
    }

    @Test
    @DisplayName("getValue should return long value")
    void getValueShouldReturnLongValue() throws WasmException {
      final WasmGlobal delegate =
          createMockGlobal(WasmValueType.I64, false, WasmValue.i64(Long.MAX_VALUE));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Long.MAX_VALUE, adapter.getValue(), "Value should be Long.MAX_VALUE");
    }

    @Test
    @DisplayName("getValue should return null for null WasmValue")
    void getValueShouldReturnNullForNullWasmValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.EXTERNREF, false, null);
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertNull(adapter.getValue(), "Value should be null");
    }

    @Test
    @DisplayName("getValue should wrap exception in WasmException")
    void getValueShouldWrapExceptionInWasmException() {
      final WasmGlobal delegate = createFailingGlobal();
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      final WasmException ex = assertThrows(WasmException.class, () -> adapter.getValue());
      assertTrue(ex.getMessage().contains("Failed to get global value"),
          "Exception message should indicate get failure");
    }
  }

  @Nested
  @DisplayName("setValue Tests")
  class SetValueTests {

    @Test
    @DisplayName("setValue should set integer value")
    void setValueShouldSetIntegerValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, true, WasmValue.i32(0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setValue(100);

      assertEquals(100, adapter.getValue(), "Value should be 100 after set");
    }

    @Test
    @DisplayName("setValue should throw for immutable global")
    void setValueShouldThrowForImmutableGlobal() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(42));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      final WasmException ex = assertThrows(WasmException.class, () -> adapter.setValue(100));
      assertTrue(ex.getMessage().contains("immutable"),
          "Exception message should mention immutable");
    }
  }

  @Nested
  @DisplayName("getValueType Tests")
  class GetValueTypeTests {

    @Test
    @DisplayName("getValueType should return correct types")
    void getValueTypeShouldReturnCorrectTypes() {
      assertEquals(Global.GlobalValueType.I32,
          new WasmGlobalToGlobalAdapter(
              createMockGlobal(WasmValueType.I32, false, WasmValue.i32(0))).getValueType());
      assertEquals(Global.GlobalValueType.I64,
          new WasmGlobalToGlobalAdapter(
              createMockGlobal(WasmValueType.I64, false, WasmValue.i64(0L))).getValueType());
      assertEquals(Global.GlobalValueType.F32,
          new WasmGlobalToGlobalAdapter(
              createMockGlobal(WasmValueType.F32, false, WasmValue.f32(0f))).getValueType());
      assertEquals(Global.GlobalValueType.F64,
          new WasmGlobalToGlobalAdapter(
              createMockGlobal(WasmValueType.F64, false, WasmValue.f64(0.0))).getValueType());
    }

    @Test
    @DisplayName("getValueType should return I32 as default for null type")
    void getValueTypeShouldReturnI32AsDefaultForNullType() {
      final WasmGlobal delegate = createMockGlobal(null, false, WasmValue.i32(0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Global.GlobalValueType.I32, adapter.getValueType(),
          "Type should default to I32");
    }
  }

  @Nested
  @DisplayName("isMutable Tests")
  class IsMutableTests {

    @Test
    @DisplayName("isMutable should return true for mutable global")
    void isMutableShouldReturnTrueForMutableGlobal() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, true, WasmValue.i32(0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertTrue(adapter.isMutable(), "Should be mutable");
    }

    @Test
    @DisplayName("isMutable should return false for immutable global")
    void isMutableShouldReturnFalseForImmutableGlobal() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertFalse(adapter.isMutable(), "Should be immutable");
    }
  }

  @Nested
  @DisplayName("isValid Tests")
  class IsValidTests {

    @Test
    @DisplayName("isValid should return true for valid global")
    void isValidShouldReturnTrueForValidGlobal() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(42));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertTrue(adapter.isValid(), "Should be valid");
    }

    @Test
    @DisplayName("isValid should return false when delegate throws")
    void isValidShouldReturnFalseWhenDelegateThrows() {
      final WasmGlobal delegate = createFailingGlobal();
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertFalse(adapter.isValid(), "Should be invalid when delegate throws");
    }
  }

  @Nested
  @DisplayName("Typed Value Accessors Tests")
  class TypedValueAccessorsTests {

    @Test
    @DisplayName("getIntValue should return integer value")
    void getIntValueShouldReturnIntegerValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(123));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(123, adapter.getIntValue(), "Int value should be 123");
    }

    @Test
    @DisplayName("getLongValue should return long value")
    void getLongValueShouldReturnLongValue() throws WasmException {
      final WasmGlobal delegate =
          createMockGlobal(WasmValueType.I64, false, WasmValue.i64(Long.MAX_VALUE));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Long.MAX_VALUE, adapter.getLongValue(), "Long value should match");
    }

    @Test
    @DisplayName("getFloatValue should return float value")
    void getFloatValueShouldReturnFloatValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F32, false, WasmValue.f32(1.5f));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(1.5f, adapter.getFloatValue(), "Float value should be 1.5f");
    }

    @Test
    @DisplayName("getDoubleValue should return double value")
    void getDoubleValueShouldReturnDoubleValue() throws WasmException {
      final WasmGlobal delegate =
          createMockGlobal(WasmValueType.F64, false, WasmValue.f64(Math.E));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Math.E, adapter.getDoubleValue(), "Double value should be Math.E");
    }
  }

  @Nested
  @DisplayName("Typed Value Setters Tests")
  class TypedValueSettersTests {

    @Test
    @DisplayName("setIntValue should set value correctly")
    void setIntValueShouldSetValueCorrectly() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, true, WasmValue.i32(0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setIntValue(555);

      assertEquals(555, adapter.getIntValue(), "Int value should be 555");
    }

    @Test
    @DisplayName("setLongValue should set value correctly")
    void setLongValueShouldSetValueCorrectly() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I64, true, WasmValue.i64(0L));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setLongValue(Long.MIN_VALUE);

      assertEquals(Long.MIN_VALUE, adapter.getLongValue(), "Long value should match");
    }

    @Test
    @DisplayName("setFloatValue should set value correctly")
    void setFloatValueShouldSetValueCorrectly() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F32, true, WasmValue.f32(0f));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setFloatValue(Float.MAX_VALUE);

      assertEquals(Float.MAX_VALUE, adapter.getFloatValue(), "Float value should match");
    }

    @Test
    @DisplayName("setDoubleValue should set value correctly")
    void setDoubleValueShouldSetValueCorrectly() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F64, true, WasmValue.f64(0.0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setDoubleValue(Double.MIN_VALUE);

      assertEquals(Double.MIN_VALUE, adapter.getDoubleValue(), "Double value should match");
    }
  }

  @Nested
  @DisplayName("getDelegate Tests")
  class GetDelegateTests {

    @Test
    @DisplayName("getDelegate should return original delegate")
    void getDelegateShouldReturnOriginalDelegate() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertSame(delegate, adapter.getDelegate(), "Should return same delegate");
    }
  }
}
