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

package ai.tegmentum.wasmtime4j.jni.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Tests for the {@link WasmGlobalToGlobalAdapter} class.
 *
 * <p>This test class verifies the adapter that bridges WasmGlobal to Global interface.
 */
@DisplayName("WasmGlobalToGlobalAdapter Tests")
class WasmGlobalToGlobalAdapterTest {

  /**
   * Creates a mock WasmGlobal for testing.
   */
  private WasmGlobal createMockGlobal(
      final WasmValueType type,
      final boolean mutable,
      final WasmValue initialValue) {
    return new TestWasmGlobal(type, mutable, initialValue);
  }

  /**
   * Test implementation of WasmGlobal.
   */
  private static class TestWasmGlobal implements WasmGlobal {
    private WasmValue value;
    private final WasmValueType type;
    private final boolean mutable;

    TestWasmGlobal(final WasmValueType type, final boolean mutable, final WasmValue initialValue) {
      this.type = type;
      this.mutable = mutable;
      this.value = initialValue;
    }

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

    @Override
    public ai.tegmentum.wasmtime4j.GlobalType getGlobalType() {
      final WasmValueType valueType = type != null ? type : WasmValueType.I32;
      final boolean isMut = mutable;
      return new ai.tegmentum.wasmtime4j.GlobalType() {
        @Override
        public WasmValueType getValueType() {
          return valueType;
        }

        @Override
        public boolean isMutable() {
          return isMut;
        }
      };
    }
  }

  /**
   * Creates a mock WasmGlobal that throws on get().
   */
  private WasmGlobal createFailingGlobal() {
    return new FailingWasmGlobal();
  }

  /**
   * Test implementation of WasmGlobal that throws on access.
   */
  private static class FailingWasmGlobal implements WasmGlobal {
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

    @Override
    public ai.tegmentum.wasmtime4j.GlobalType getGlobalType() {
      return new ai.tegmentum.wasmtime4j.GlobalType() {
        @Override
        public WasmValueType getValueType() {
          return WasmValueType.I32;
        }

        @Override
        public boolean isMutable() {
          return true;
        }
      };
    }
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

      assertNotNull(adapter, "Adapter should not be null");
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

      final Object value = adapter.getValue();

      assertEquals(42, value, "Value should be 42");
    }

    @Test
    @DisplayName("getValue should return long value")
    void getValueShouldReturnLongValue() throws WasmException {
      final WasmGlobal delegate =
          createMockGlobal(WasmValueType.I64, false, WasmValue.i64(Long.MAX_VALUE));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      final Object value = adapter.getValue();

      assertEquals(Long.MAX_VALUE, value, "Value should be Long.MAX_VALUE");
    }

    @Test
    @DisplayName("getValue should return float value")
    void getValueShouldReturnFloatValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F32, false, WasmValue.f32(3.14f));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      final Object value = adapter.getValue();

      assertEquals(3.14f, value, "Value should be 3.14f");
    }

    @Test
    @DisplayName("getValue should return double value")
    void getValueShouldReturnDoubleValue() throws WasmException {
      final WasmGlobal delegate =
          createMockGlobal(WasmValueType.F64, false, WasmValue.f64(2.718281828));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      final Object value = adapter.getValue();

      assertEquals(2.718281828, value, "Value should be 2.718281828");
    }

    @Test
    @DisplayName("getValue should return null for null WasmValue")
    void getValueShouldReturnNullForNullWasmValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.EXTERNREF, false, null);
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      final Object value = adapter.getValue();

      assertNull(value, "Value should be null");
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
    @DisplayName("setValue should set long value")
    void setValueShouldSetLongValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I64, true, WasmValue.i64(0L));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setValue(999999999999L);

      assertEquals(999999999999L, adapter.getValue(), "Value should be updated");
    }

    @Test
    @DisplayName("setValue should set float value")
    void setValueShouldSetFloatValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F32, true, WasmValue.f32(0f));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setValue(1.5f);

      assertEquals(1.5f, adapter.getValue(), "Value should be 1.5f after set");
    }

    @Test
    @DisplayName("setValue should set double value")
    void setValueShouldSetDoubleValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F64, true, WasmValue.f64(0.0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setValue(Math.PI);

      assertEquals(Math.PI, adapter.getValue(), "Value should be PI after set");
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

    @Test
    @DisplayName("setValue should handle null as externref")
    void setValueShouldHandleNullAsExternref() throws WasmException {
      final WasmGlobal delegate =
          createMockGlobal(WasmValueType.EXTERNREF, true, WasmValue.externref("initial"));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setValue(null);

      assertNull(adapter.getValue(), "Value should be null after set");
    }
  }

  @Nested
  @DisplayName("getValueType Tests")
  class GetValueTypeTests {

    @Test
    @DisplayName("getValueType should return I32 for I32 type")
    void getValueTypeShouldReturnI32ForI32Type() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Global.GlobalValueType.I32, adapter.getValueType(), "Type should be I32");
    }

    @Test
    @DisplayName("getValueType should return I64 for I64 type")
    void getValueTypeShouldReturnI64ForI64Type() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I64, false, WasmValue.i64(0L));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Global.GlobalValueType.I64, adapter.getValueType(), "Type should be I64");
    }

    @Test
    @DisplayName("getValueType should return F32 for F32 type")
    void getValueTypeShouldReturnF32ForF32Type() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F32, false, WasmValue.f32(0f));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Global.GlobalValueType.F32, adapter.getValueType(), "Type should be F32");
    }

    @Test
    @DisplayName("getValueType should return F64 for F64 type")
    void getValueTypeShouldReturnF64ForF64Type() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F64, false, WasmValue.f64(0.0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Global.GlobalValueType.F64, adapter.getValueType(), "Type should be F64");
    }

    @Test
    @DisplayName("getValueType should return V128 for V128 type")
    void getValueTypeShouldReturnV128ForV128Type() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.V128, false, null);
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Global.GlobalValueType.V128, adapter.getValueType(), "Type should be V128");
    }

    @Test
    @DisplayName("getValueType should return FUNCREF for FUNCREF type")
    void getValueTypeShouldReturnFuncrefForFuncrefType() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.FUNCREF, false, null);
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Global.GlobalValueType.FUNCREF, adapter.getValueType(),
          "Type should be FUNCREF");
    }

    @Test
    @DisplayName("getValueType should return EXTERNREF for EXTERNREF type")
    void getValueTypeShouldReturnExternrefForExternrefType() {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.EXTERNREF, false, null);
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Global.GlobalValueType.EXTERNREF, adapter.getValueType(),
          "Type should be EXTERNREF");
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
  @DisplayName("getIntValue Tests")
  class GetIntValueTests {

    @Test
    @DisplayName("getIntValue should return integer value")
    void getIntValueShouldReturnIntegerValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(123));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(123, adapter.getIntValue(), "Int value should be 123");
    }

    @Test
    @DisplayName("getIntValue should convert long to int")
    void getIntValueShouldConvertLongToInt() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I64, false, WasmValue.i64(456L));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(456, adapter.getIntValue(), "Int value should be 456");
    }

    @Test
    @DisplayName("getIntValue should throw for non-numeric value")
    void getIntValueShouldThrowForNonNumericValue() {
      final WasmGlobal delegate =
          createMockGlobal(WasmValueType.EXTERNREF, false, WasmValue.externref("not a number"));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertThrows(WasmException.class, () -> adapter.getIntValue(),
          "Should throw for non-numeric value");
    }
  }

  @Nested
  @DisplayName("getLongValue Tests")
  class GetLongValueTests {

    @Test
    @DisplayName("getLongValue should return long value")
    void getLongValueShouldReturnLongValue() throws WasmException {
      final WasmGlobal delegate =
          createMockGlobal(WasmValueType.I64, false, WasmValue.i64(Long.MAX_VALUE));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Long.MAX_VALUE, adapter.getLongValue(), "Long value should match");
    }

    @Test
    @DisplayName("getLongValue should convert int to long")
    void getLongValueShouldConvertIntToLong() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, false, WasmValue.i32(789));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(789L, adapter.getLongValue(), "Long value should be 789L");
    }
  }

  @Nested
  @DisplayName("getFloatValue Tests")
  class GetFloatValueTests {

    @Test
    @DisplayName("getFloatValue should return float value")
    void getFloatValueShouldReturnFloatValue() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F32, false, WasmValue.f32(1.5f));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(1.5f, adapter.getFloatValue(), "Float value should be 1.5f");
    }

    @Test
    @DisplayName("getFloatValue should convert double to float")
    void getFloatValueShouldConvertDoubleToFloat() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F64, false, WasmValue.f64(2.5));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(2.5f, adapter.getFloatValue(), "Float value should be 2.5f");
    }
  }

  @Nested
  @DisplayName("getDoubleValue Tests")
  class GetDoubleValueTests {

    @Test
    @DisplayName("getDoubleValue should return double value")
    void getDoubleValueShouldReturnDoubleValue() throws WasmException {
      final WasmGlobal delegate =
          createMockGlobal(WasmValueType.F64, false, WasmValue.f64(Math.E));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(Math.E, adapter.getDoubleValue(), "Double value should be Math.E");
    }

    @Test
    @DisplayName("getDoubleValue should convert float to double")
    void getDoubleValueShouldConvertFloatToDouble() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F32, false, WasmValue.f32(1.0f));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      assertEquals(1.0, adapter.getDoubleValue(), 0.001, "Double value should be 1.0");
    }
  }

  @Nested
  @DisplayName("setIntValue Tests")
  class SetIntValueTests {

    @Test
    @DisplayName("setIntValue should set value correctly")
    void setIntValueShouldSetValueCorrectly() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I32, true, WasmValue.i32(0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setIntValue(555);

      assertEquals(555, adapter.getIntValue(), "Int value should be 555");
    }
  }

  @Nested
  @DisplayName("setLongValue Tests")
  class SetLongValueTests {

    @Test
    @DisplayName("setLongValue should set value correctly")
    void setLongValueShouldSetValueCorrectly() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.I64, true, WasmValue.i64(0L));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setLongValue(Long.MIN_VALUE);

      assertEquals(Long.MIN_VALUE, adapter.getLongValue(), "Long value should match");
    }
  }

  @Nested
  @DisplayName("setFloatValue Tests")
  class SetFloatValueTests {

    @Test
    @DisplayName("setFloatValue should set value correctly")
    void setFloatValueShouldSetValueCorrectly() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(WasmValueType.F32, true, WasmValue.f32(0f));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setFloatValue(Float.MAX_VALUE);

      assertEquals(Float.MAX_VALUE, adapter.getFloatValue(), "Float value should match");
    }
  }

  @Nested
  @DisplayName("setDoubleValue Tests")
  class SetDoubleValueTests {

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
  @DisplayName("Type Conversion Tests")
  class TypeConversionTests {

    @Test
    @DisplayName("convertToWasmValue should infer Integer as I32")
    void convertToWasmValueShouldInferIntegerAsI32() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(null, true, WasmValue.i32(0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setValue(42);

      assertEquals(42, adapter.getIntValue(), "Should convert Integer to I32");
    }

    @Test
    @DisplayName("convertToWasmValue should infer Long as I64")
    void convertToWasmValueShouldInferLongAsI64() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(null, true, WasmValue.i64(0L));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setValue(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, adapter.getLongValue(), "Should convert Long to I64");
    }

    @Test
    @DisplayName("convertToWasmValue should infer Float as F32")
    void convertToWasmValueShouldInferFloatAsF32() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(null, true, WasmValue.f32(0f));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setValue(1.23f);

      assertEquals(1.23f, adapter.getFloatValue(), "Should convert Float to F32");
    }

    @Test
    @DisplayName("convertToWasmValue should infer Double as F64")
    void convertToWasmValueShouldInferDoubleAsF64() throws WasmException {
      final WasmGlobal delegate = createMockGlobal(null, true, WasmValue.f64(0.0));
      final WasmGlobalToGlobalAdapter adapter = new WasmGlobalToGlobalAdapter(delegate);

      adapter.setValue(Math.PI);

      assertEquals(Math.PI, adapter.getDoubleValue(), "Should convert Double to F64");
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
