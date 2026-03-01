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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests for ExternRef type-safe wrapper functionality. */
final class ExternRefTest {

  // Simple test class for typed externref testing
  static class TestData {
    private final String name;
    private final int value;

    TestData(final String name, final int value) {
      this.name = name;
      this.value = value;
    }

    String getName() {
      return name;
    }

    int getValue() {
      return value;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final TestData other = (TestData) obj;
      return value == other.value && java.util.Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(name, value);
    }
  }

  // ==================== Factory Method Tests ====================

  @Test
  void testOfWithNonNullValue() {
    final String value = "test string";
    final ExternRef<String> ref = ExternRef.of(value);

    assertNotNull(ref);
    assertEquals(value, ref.get());
    assertEquals(String.class, ref.getDeclaredType());
    assertTrue(ref.isPresent());
    assertFalse(ref.isNull());
  }

  @Test
  void testOfWithTypedObject() {
    final TestData data = new TestData("test", 42);
    final ExternRef<TestData> ref = ExternRef.of(data);

    assertNotNull(ref);
    assertEquals(data, ref.get());
    assertEquals(TestData.class, ref.getDeclaredType());
    assertEquals("test", ref.get().getName());
    assertEquals(42, ref.get().getValue());
  }

  @Test
  void testOfWithNullThrowsNullPointerException() {
    assertThrows(NullPointerException.class, () -> ExternRef.of(null));
  }

  @Test
  void testOfWithExplicitType() {
    final String value = "test";
    final ExternRef<String> ref = ExternRef.of(value, String.class);

    assertNotNull(ref);
    assertEquals(value, ref.get());
    assertEquals(String.class, ref.getDeclaredType());
  }

  @Test
  void testOfWithExplicitTypeNullValueThrows() {
    assertThrows(NullPointerException.class, () -> ExternRef.of(null, String.class));
  }

  @Test
  void testOfWithExplicitTypeNullTypeThrows() {
    assertThrows(NullPointerException.class, () -> ExternRef.of("test", null));
  }

  @Test
  void testOfNullableWithValue() {
    final String value = "test";
    final ExternRef<String> ref = ExternRef.ofNullable(value, String.class);

    assertNotNull(ref);
    assertEquals(value, ref.get());
    assertTrue(ref.isPresent());
    assertFalse(ref.isNull());
  }

  @Test
  void testOfNullableWithNull() {
    final ExternRef<String> ref = ExternRef.ofNullable(null, String.class);

    assertNotNull(ref);
    assertNull(ref.get());
    assertFalse(ref.isPresent());
    assertTrue(ref.isNull());
    assertEquals(String.class, ref.getDeclaredType());
  }

  @Test
  void testOfNullableWithNullTypeThrows() {
    assertThrows(NullPointerException.class, () -> ExternRef.ofNullable("test", null));
  }

  @Test
  void testNullRef() {
    final ExternRef<String> ref = ExternRef.nullRef(String.class);

    assertNotNull(ref);
    assertNull(ref.get());
    assertTrue(ref.isNull());
    assertFalse(ref.isPresent());
    assertEquals(String.class, ref.getDeclaredType());
  }

  @Test
  void testNullRefWithNullTypeThrows() {
    assertThrows(NullPointerException.class, () -> ExternRef.nullRef(null));
  }

  @Test
  void testFromRawWithObject() {
    final String value = "test";
    final ExternRef<Object> ref = ExternRef.fromRaw(value);

    assertNotNull(ref);
    assertEquals(value, ref.get());
    assertEquals(Object.class, ref.getDeclaredType());
  }

  @Test
  void testFromRawWithNull() {
    final ExternRef<Object> ref = ExternRef.fromRaw(null);

    assertNotNull(ref);
    assertNull(ref.get());
    assertTrue(ref.isNull());
    assertEquals(Object.class, ref.getDeclaredType());
  }

  @Test
  void testFromRawWithExternRefPassthrough() {
    final ExternRef<String> original = ExternRef.of("test");
    final ExternRef<Object> ref = ExternRef.fromRaw(original);

    // When passed an ExternRef, fromRaw should return it cast to Object
    assertNotNull(ref);
    // The original ExternRef is returned as-is (cast)
    assertEquals(original.get(), ref.get());
  }

  @Test
  void testFromWasmValue() {
    final String value = "test externref";
    final WasmValue wasmValue = WasmValue.externref(value);
    final ExternRef<Object> ref = ExternRef.fromWasmValue(wasmValue);

    assertNotNull(ref);
    assertEquals(value, ref.get());
  }

  @Test
  void testFromWasmValueWithNullExternref() {
    final WasmValue wasmValue = WasmValue.externref((Object) null);
    final ExternRef<Object> ref = ExternRef.fromWasmValue(wasmValue);

    assertNotNull(ref);
    assertNull(ref.get());
    assertTrue(ref.isNull());
  }

  @Test
  void testFromWasmValueWithNonExternrefThrows() {
    final WasmValue intValue = WasmValue.i32(42);
    assertThrows(IllegalArgumentException.class, () -> ExternRef.fromWasmValue(intValue));
  }

  @Test
  void testFromWasmValueWithNullThrows() {
    assertThrows(NullPointerException.class, () -> ExternRef.fromWasmValue(null));
  }

  // ==================== Accessor Method Tests ====================

  @Test
  void testGet() {
    final TestData data = new TestData("accessor", 100);
    final ExternRef<TestData> ref = ExternRef.of(data);

    final TestData retrieved = ref.get();
    assertEquals(data, retrieved);
    assertEquals("accessor", retrieved.getName());
    assertEquals(100, retrieved.getValue());
  }

  @Test
  void testGetReturnsNullForNullRef() {
    final ExternRef<String> ref = ExternRef.nullRef(String.class);
    assertNull(ref.get());
  }

  @Test
  void testGetOrThrow() {
    final String value = "test";
    final ExternRef<String> ref = ExternRef.of(value);

    assertEquals(value, ref.getOrThrow());
  }

  @Test
  void testGetOrThrowWithNullThrows() {
    final ExternRef<String> ref = ExternRef.nullRef(String.class);
    assertThrows(NullPointerException.class, ref::getOrThrow);
  }

  @Test
  void testGetOrDefault() {
    final String value = "actual";
    final ExternRef<String> ref = ExternRef.of(value);

    assertEquals(value, ref.getOrDefault("default"));
  }

  @Test
  void testGetOrDefaultWithNullReturnsDefault() {
    final ExternRef<String> ref = ExternRef.nullRef(String.class);
    assertEquals("default", ref.getOrDefault("default"));
  }

  @Test
  void testToOptional() {
    final String value = "test";
    final ExternRef<String> ref = ExternRef.of(value);

    final Optional<String> optional = ref.toOptional();
    assertTrue(optional.isPresent());
    assertEquals(value, optional.get());
  }

  @Test
  void testToOptionalWithNull() {
    final ExternRef<String> ref = ExternRef.nullRef(String.class);

    final Optional<String> optional = ref.toOptional();
    assertFalse(optional.isPresent());
  }

  @Test
  void testGetAs() {
    final TestData data = new TestData("typed", 50);
    final ExternRef<Object> ref = ExternRef.fromRaw(data);

    final TestData retrieved = ref.getAs(TestData.class);
    assertNotNull(retrieved);
    assertEquals("typed", retrieved.getName());
    assertEquals(50, retrieved.getValue());
  }

  @Test
  void testGetAsWithNullReturnsNull() {
    final ExternRef<Object> ref = ExternRef.fromRaw(null);

    final String retrieved = ref.getAs(String.class);
    assertNull(retrieved);
  }

  @Test
  void testGetAsWithIncompatibleTypeThrows() {
    final String value = "test";
    final ExternRef<Object> ref = ExternRef.fromRaw(value);

    assertThrows(ClassCastException.class, () -> ref.getAs(Integer.class));
  }

  @Test
  void testGetAsWithNullTargetTypeThrows() {
    final ExternRef<String> ref = ExternRef.of("test");
    assertThrows(NullPointerException.class, () -> ref.getAs(null));
  }

  @Test
  void testGetAsOrNull() {
    final TestData data = new TestData("test", 1);
    final ExternRef<Object> ref = ExternRef.fromRaw(data);

    final TestData retrieved = ref.getAsOrNull(TestData.class);
    assertNotNull(retrieved);
    assertEquals(data, retrieved);
  }

  @Test
  void testGetAsOrNullWithIncompatibleType() {
    final String value = "test";
    final ExternRef<Object> ref = ExternRef.fromRaw(value);

    final Integer retrieved = ref.getAsOrNull(Integer.class);
    assertNull(retrieved);
  }

  @Test
  void testGetAsOrNullWithNullValue() {
    final ExternRef<Object> ref = ExternRef.fromRaw(null);

    final String retrieved = ref.getAsOrNull(String.class);
    assertNull(retrieved);
  }

  @Test
  void testGetAsOrNullWithNullTargetTypeThrows() {
    final ExternRef<String> ref = ExternRef.of("test");
    assertThrows(NullPointerException.class, () -> ref.getAsOrNull(null));
  }

  // ==================== State Method Tests ====================

  @Test
  void testIsNull() {
    final ExternRef<String> nullRef = ExternRef.nullRef(String.class);
    final ExternRef<String> nonNullRef = ExternRef.of("test");

    assertTrue(nullRef.isNull());
    assertFalse(nonNullRef.isNull());
  }

  @Test
  void testIsPresent() {
    final ExternRef<String> nullRef = ExternRef.nullRef(String.class);
    final ExternRef<String> nonNullRef = ExternRef.of("test");

    assertFalse(nullRef.isPresent());
    assertTrue(nonNullRef.isPresent());
  }

  @Test
  void testGetDeclaredType() {
    final ExternRef<String> stringRef = ExternRef.of("test");
    final ExternRef<Integer> intRef = ExternRef.of(42);
    final ExternRef<TestData> dataRef = ExternRef.of(new TestData("test", 1));

    assertEquals(String.class, stringRef.getDeclaredType());
    assertEquals(Integer.class, intRef.getDeclaredType());
    assertEquals(TestData.class, dataRef.getDeclaredType());
  }

  @Test
  void testIsInstanceOf() {
    final ExternRef<Object> ref = ExternRef.fromRaw("test");

    assertTrue(ref.isInstanceOf(String.class));
    assertTrue(ref.isInstanceOf(CharSequence.class));
    assertTrue(ref.isInstanceOf(Object.class));
    assertFalse(ref.isInstanceOf(Integer.class));
    assertFalse(ref.isInstanceOf(TestData.class));
  }

  @Test
  void testIsInstanceOfWithNull() {
    final ExternRef<Object> ref = ExternRef.fromRaw(null);

    assertFalse(ref.isInstanceOf(String.class));
    assertFalse(ref.isInstanceOf(Object.class));
  }

  @Test
  void testIsInstanceOfWithNullType() {
    final ExternRef<String> ref = ExternRef.of("test");
    assertFalse(ref.isInstanceOf(null));
  }

  @Test
  void testGetId() {
    final ExternRef<String> ref1 = ExternRef.of("first");
    final ExternRef<String> ref2 = ExternRef.of("second");

    assertTrue(ref1.getId() > 0);
    assertTrue(ref2.getId() > 0);
    assertNotEquals(ref1.getId(), ref2.getId());
  }

  @Test
  void testIdsAreSequential() {
    final ExternRef<String> ref1 = ExternRef.of("a");
    final ExternRef<String> ref2 = ExternRef.of("b");
    final ExternRef<String> ref3 = ExternRef.of("c");

    assertTrue(ref2.getId() > ref1.getId());
    assertTrue(ref3.getId() > ref2.getId());
  }

  // ==================== Conversion Method Tests ====================

  @Test
  void testToWasmValue() {
    final TestData data = new TestData("conversion", 99);
    final ExternRef<TestData> ref = ExternRef.of(data);

    final WasmValue wasmValue = ref.toWasmValue();

    assertNotNull(wasmValue);
    assertTrue(wasmValue.isExternref());
    assertEquals(WasmValueType.EXTERNREF, wasmValue.getType());
    // The WasmValue contains the ExternRef itself
    assertTrue(wasmValue.asExternref() instanceof ExternRef);
  }

  @Test
  void testToUnwrappedWasmValue() {
    final String value = "unwrapped";
    final ExternRef<String> ref = ExternRef.of(value);

    final WasmValue wasmValue = ref.toUnwrappedWasmValue();

    assertNotNull(wasmValue);
    assertTrue(wasmValue.isExternref());
    // The WasmValue contains the raw value, not the ExternRef
    assertEquals(value, wasmValue.asExternref());
  }

  @Test
  void testToUnwrappedWasmValueWithNull() {
    final ExternRef<String> ref = ExternRef.nullRef(String.class);

    final WasmValue wasmValue = ref.toUnwrappedWasmValue();

    assertNotNull(wasmValue);
    assertTrue(wasmValue.isExternref());
    assertNull(wasmValue.asExternref());
  }

  // ==================== WasmValue Integration Tests ====================

  @Test
  void testWasmValueExternrefWithExternRef() {
    final ExternRef<String> ref = ExternRef.of("test");
    final WasmValue wasmValue = WasmValue.externref(ref);

    assertNotNull(wasmValue);
    assertTrue(wasmValue.isExternref());
  }

  @Test
  void testWasmValueAsExternRef() {
    final String value = "roundtrip";
    final WasmValue wasmValue = WasmValue.externref(value);

    final ExternRef<Object> ref = wasmValue.asExternRef();
    assertNotNull(ref);
    assertEquals(value, ref.get());
  }

  @Test
  void testWasmValueAsExternRefTyped() {
    final TestData data = new TestData("typed roundtrip", 123);
    final WasmValue wasmValue = WasmValue.externref(data);

    final ExternRef<TestData> ref = wasmValue.asExternRef(TestData.class);
    assertNotNull(ref);
    assertEquals(data, ref.get());
    assertEquals("typed roundtrip", ref.get().getName());
  }

  @Test
  void testWasmValueAsExternRefTypedWithWrongType() {
    final String value = "string value";
    final WasmValue wasmValue = WasmValue.externref(value);

    assertThrows(ClassCastException.class, () -> wasmValue.asExternRef(Integer.class));
  }

  // ==================== Equals and HashCode Tests ====================

  @Test
  void testEqualsWithSameValue() {
    final ExternRef<String> ref1 = ExternRef.of("test");
    final ExternRef<String> ref2 = ExternRef.of("test");

    assertEquals(ref1, ref2);
    assertEquals(ref2, ref1);
  }

  @Test
  void testEqualsWithDifferentValues() {
    final ExternRef<String> ref1 = ExternRef.of("test1");
    final ExternRef<String> ref2 = ExternRef.of("test2");

    assertNotEquals(ref1, ref2);
  }

  @Test
  void testEqualsWithNull() {
    final ExternRef<String> ref = ExternRef.of("test");

    assertNotEquals(ref, null);
  }

  @Test
  void testEqualsWithDifferentType() {
    final ExternRef<String> ref = ExternRef.of("test");

    assertNotEquals(ref, "test");
    assertNotEquals(ref, 42);
  }

  @Test
  void testEqualsWithSameInstance() {
    final ExternRef<String> ref = ExternRef.of("test");

    assertEquals(ref, ref);
  }

  @Test
  void testEqualsWithBothNull() {
    final ExternRef<String> ref1 = ExternRef.nullRef(String.class);
    final ExternRef<String> ref2 = ExternRef.nullRef(String.class);

    assertEquals(ref1, ref2);
  }

  @Test
  void testEqualsWithOneNull() {
    final ExternRef<String> ref1 = ExternRef.of("test");
    final ExternRef<String> ref2 = ExternRef.nullRef(String.class);

    assertNotEquals(ref1, ref2);
    assertNotEquals(ref2, ref1);
  }

  @Test
  void testHashCodeConsistency() {
    final ExternRef<String> ref1 = ExternRef.of("test");
    final ExternRef<String> ref2 = ExternRef.of("test");

    assertEquals(ref1.hashCode(), ref2.hashCode());
  }

  @Test
  void testHashCodeWithNull() {
    final ExternRef<String> ref1 = ExternRef.nullRef(String.class);
    final ExternRef<Integer> ref2 = ExternRef.nullRef(Integer.class);

    // Both null refs should have same hash (based on null value)
    assertEquals(ref1.hashCode(), ref2.hashCode());
  }

  // ==================== toString Tests ====================

  @Test
  void testToString() {
    final ExternRef<String> ref = ExternRef.of("test");
    final String str = ref.toString();

    assertNotNull(str);
    assertTrue(str.contains("ExternRef"));
    assertTrue(str.contains("test"));
    assertTrue(str.contains("String"));
    assertTrue(str.contains("id="));
  }

  @Test
  void testToStringWithNull() {
    final ExternRef<String> ref = ExternRef.nullRef(String.class);
    final String str = ref.toString();

    assertNotNull(str);
    assertTrue(str.contains("ExternRef"));
    assertTrue(str.contains("null"));
    assertTrue(str.contains("String"));
    assertTrue(str.contains("id="));
  }

  @Test
  void testToStringWithCustomType() {
    final TestData data = new TestData("test", 1);
    final ExternRef<TestData> ref = ExternRef.of(data);
    final String str = ref.toString();

    assertNotNull(str);
    assertTrue(str.contains("ExternRef"));
    assertTrue(str.contains("TestData"));
  }

  // ==================== Edge Case Tests ====================

  @Test
  void testWithPrimitiveWrappers() {
    final ExternRef<Integer> intRef = ExternRef.of(42);
    final ExternRef<Double> doubleRef = ExternRef.of(3.14);
    final ExternRef<Boolean> boolRef = ExternRef.of(true);

    assertEquals(Integer.valueOf(42), intRef.get());
    assertEquals(Double.valueOf(3.14), doubleRef.get());
    assertEquals(Boolean.TRUE, boolRef.get());
  }

  @Test
  void testWithArray() {
    final int[] array = {1, 2, 3};
    final ExternRef<int[]> ref = ExternRef.of(array);

    assertNotNull(ref.get());
    assertEquals(3, ref.get().length);
    assertEquals(1, ref.get()[0]);
    assertEquals(2, ref.get()[1]);
    assertEquals(3, ref.get()[2]);
  }

  @Test
  void testRoundTripThroughWasmValue() {
    final TestData original = new TestData("roundtrip", 999);
    final ExternRef<TestData> originalRef = ExternRef.of(original);

    // Convert to WasmValue and back
    final WasmValue wasmValue = originalRef.toWasmValue();
    final ExternRef<Object> recovered = ExternRef.fromWasmValue(wasmValue);

    // fromRaw passes through ExternRef directly, so recovered IS originalRef
    // (just cast to ExternRef<Object>), and get() returns the TestData directly
    assertEquals(original, recovered.get());
    assertEquals(original, recovered.getAs(TestData.class));
  }

  @Test
  void testUnwrappedRoundTripThroughWasmValue() {
    final TestData original = new TestData("unwrapped roundtrip", 888);
    final ExternRef<TestData> originalRef = ExternRef.of(original);

    // Convert to unwrapped WasmValue and back
    final WasmValue wasmValue = originalRef.toUnwrappedWasmValue();
    final ExternRef<Object> recovered = ExternRef.fromWasmValue(wasmValue);

    // The raw value should be the TestData directly
    assertEquals(original, recovered.get());
    assertEquals(original, recovered.getAs(TestData.class));
  }
}
