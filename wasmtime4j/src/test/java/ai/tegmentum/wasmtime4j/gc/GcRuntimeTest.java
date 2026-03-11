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
package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcRuntime} default methods.
 *
 * <p>Verifies that the async and fixed-array default methods correctly delegate to their
 * synchronous counterparts. Uses a recording implementation to track which methods are called.
 */
@DisplayName("GcRuntime Default Method Tests")
class GcRuntimeTest {

  private RecordingGcRuntime runtime;
  private StructType testStructType;
  private ArrayType testArrayType;
  private List<GcValue> testValues;

  @BeforeEach
  void setUp() {
    runtime = new RecordingGcRuntime();
    testStructType = StructType.builder("TestStruct").addField("x", FieldType.i32(), true).build();
    testArrayType = ArrayType.builder("TestArray").elementType(FieldType.i32()).build();
    testValues = Arrays.asList(GcValue.i32(1), GcValue.i32(2));
  }

  @Nested
  @DisplayName("createStructAsync Default Delegation")
  class CreateStructAsyncTests {

    @Test
    @DisplayName("should delegate to createStruct with field values")
    void shouldDelegateToCreateStructWithFieldValues() throws GcException {
      final List<GcValue> fieldValues = Collections.singletonList(GcValue.i32(42));
      final StructInstance result = runtime.createStructAsync(testStructType, fieldValues);

      assertSame(runtime.lastStructInstance, result, "Should return the same instance");
      assertEquals(
          "createStruct(StructType,List)",
          runtime.lastCalledMethod,
          "Should delegate to createStruct");
      assertSame(testStructType, runtime.lastStructType, "Should pass through the struct type");
      assertSame(fieldValues, runtime.lastFieldValues, "Should pass through the field values");
    }

    @Test
    @DisplayName("should delegate with empty field values")
    void shouldDelegateWithEmptyFieldValues() throws GcException {
      final List<GcValue> emptyValues = Collections.emptyList();
      runtime.createStructAsync(testStructType, emptyValues);

      assertEquals(
          "createStruct(StructType,List)",
          runtime.lastCalledMethod,
          "Should delegate to createStruct");
      assertSame(emptyValues, runtime.lastFieldValues, "Should pass through empty list");
    }

    @Test
    @DisplayName("should propagate GcException from delegate")
    void shouldPropagateGcExceptionFromDelegate() {
      runtime.throwOnCreateStruct = true;
      try {
        runtime.createStructAsync(testStructType, Collections.singletonList(GcValue.i32(1)));
        org.junit.jupiter.api.Assertions.fail("Should have thrown GcException");
      } catch (final GcException e) {
        assertEquals("createStruct failed", e.getMessage(), "Should propagate exception message");
      }
    }
  }

  @Nested
  @DisplayName("createArrayAsync Default Delegation")
  class CreateArrayAsyncTests {

    @Test
    @DisplayName("should delegate to createArray with elements")
    void shouldDelegateToCreateArrayWithElements() throws GcException {
      final ArrayInstance result = runtime.createArrayAsync(testArrayType, testValues);

      assertSame(runtime.lastArrayInstance, result, "Should return the same instance");
      assertEquals(
          "createArray(ArrayType,List)",
          runtime.lastCalledMethod,
          "Should delegate to createArray");
      assertSame(testArrayType, runtime.lastArrayType, "Should pass through the array type");
      assertSame(testValues, runtime.lastElements, "Should pass through the elements");
    }

    @Test
    @DisplayName("should delegate with empty elements")
    void shouldDelegateWithEmptyElements() throws GcException {
      final List<GcValue> emptyValues = Collections.emptyList();
      runtime.createArrayAsync(testArrayType, emptyValues);

      assertEquals(
          "createArray(ArrayType,List)",
          runtime.lastCalledMethod,
          "Should delegate to createArray");
      assertSame(emptyValues, runtime.lastElements, "Should pass through empty list");
    }

    @Test
    @DisplayName("should propagate GcException from delegate")
    void shouldPropagateGcExceptionFromDelegate() {
      runtime.throwOnCreateArray = true;
      try {
        runtime.createArrayAsync(testArrayType, testValues);
        org.junit.jupiter.api.Assertions.fail("Should have thrown GcException");
      } catch (final GcException e) {
        assertEquals("createArray failed", e.getMessage(), "Should propagate exception message");
      }
    }
  }

  @Nested
  @DisplayName("createArrayFixed Default Delegation")
  class CreateArrayFixedTests {

    @Test
    @DisplayName("should delegate to createArray with elements")
    void shouldDelegateToCreateArrayWithElements() throws GcException {
      final ArrayInstance result = runtime.createArrayFixed(testArrayType, testValues);

      assertSame(runtime.lastArrayInstance, result, "Should return the same instance");
      assertEquals(
          "createArray(ArrayType,List)",
          runtime.lastCalledMethod,
          "Should delegate to createArray");
      assertSame(testArrayType, runtime.lastArrayType, "Should pass through the array type");
      assertSame(testValues, runtime.lastElements, "Should pass through the elements");
    }

    @Test
    @DisplayName("should propagate GcException from delegate")
    void shouldPropagateGcExceptionFromDelegate() {
      runtime.throwOnCreateArray = true;
      try {
        runtime.createArrayFixed(testArrayType, testValues);
        org.junit.jupiter.api.Assertions.fail("Should have thrown GcException");
      } catch (final GcException e) {
        assertEquals("createArray failed", e.getMessage(), "Should propagate exception message");
      }
    }
  }

  @Nested
  @DisplayName("createArrayFixedAsync Default Delegation")
  class CreateArrayFixedAsyncTests {

    @Test
    @DisplayName("should delegate through createArrayFixed to createArray")
    void shouldDelegateThroughCreateArrayFixedToCreateArray() throws GcException {
      final ArrayInstance result = runtime.createArrayFixedAsync(testArrayType, testValues);

      assertSame(runtime.lastArrayInstance, result, "Should return the same instance");
      // createArrayFixedAsync -> createArrayFixed -> createArray
      assertEquals(
          "createArray(ArrayType,List)",
          runtime.lastCalledMethod,
          "Should ultimately delegate to createArray");
      assertSame(testArrayType, runtime.lastArrayType, "Should pass through the array type");
      assertSame(testValues, runtime.lastElements, "Should pass through the elements");
    }

    @Test
    @DisplayName("should propagate GcException through delegation chain")
    void shouldPropagateGcExceptionThroughDelegationChain() {
      runtime.throwOnCreateArray = true;
      try {
        runtime.createArrayFixedAsync(testArrayType, testValues);
        org.junit.jupiter.api.Assertions.fail("Should have thrown GcException");
      } catch (final GcException e) {
        assertEquals("createArray failed", e.getMessage(), "Should propagate exception message");
      }
    }

    @Test
    @DisplayName("should use overridden createArrayFixed when available")
    void shouldUseOverriddenCreateArrayFixedWhenAvailable() throws GcException {
      final StubArrayInstance customInstance = new StubArrayInstance(99L);
      final GcRuntime overriddenRuntime =
          new RecordingGcRuntime() {
            @Override
            public ArrayInstance createArrayFixed(
                final ArrayType arrayType, final List<GcValue> elements) throws GcException {
              lastCalledMethod = "createArrayFixed(ArrayType,List)";
              return customInstance;
            }
          };

      final ArrayInstance result =
          overriddenRuntime.createArrayFixedAsync(testArrayType, testValues);
      assertSame(customInstance, result, "Should use overridden createArrayFixed result");
    }
  }

  @Nested
  @DisplayName("Delegation Chain Verification")
  class DelegationChainTests {

    @Test
    @DisplayName("createArrayFixedAsync delegates to createArrayFixed not createArray directly")
    void createArrayFixedAsyncDelegatesToCreateArrayFixed() throws GcException {
      final List<String> callOrder = new ArrayList<>();
      final StubArrayInstance instance = new StubArrayInstance(1L);
      final GcRuntime chainRuntime =
          new RecordingGcRuntime() {
            @Override
            public ArrayInstance createArrayFixed(
                final ArrayType arrayType, final List<GcValue> elements) throws GcException {
              callOrder.add("createArrayFixed");
              return instance;
            }
          };

      chainRuntime.createArrayFixedAsync(testArrayType, testValues);
      assertEquals(1, callOrder.size(), "Should have called one method");
      assertEquals(
          "createArrayFixed",
          callOrder.get(0),
          "createArrayFixedAsync should call createArrayFixed");
    }

    @Test
    @DisplayName("createStructAsync delegates to createStruct not another method")
    void createStructAsyncDelegatesToCreateStruct() throws GcException {
      final List<String> callOrder = new ArrayList<>();
      final StubStructInstance instance = new StubStructInstance(1L);
      final GcRuntime chainRuntime =
          new RecordingGcRuntime() {
            @Override
            public StructInstance createStruct(
                final StructType structType, final List<GcValue> fieldValues) throws GcException {
              callOrder.add("createStruct");
              return instance;
            }
          };

      chainRuntime.createStructAsync(testStructType, Collections.singletonList(GcValue.i32(1)));
      assertEquals(1, callOrder.size(), "Should have called one method");
      assertEquals("createStruct", callOrder.get(0), "createStructAsync should call createStruct");
    }
  }

  // ========== Stub / Recording Implementation ==========

  /**
   * A recording implementation of GcRuntime that tracks which methods are called and with what
   * arguments, without requiring a native runtime.
   */
  static class RecordingGcRuntime implements GcRuntime {
    String lastCalledMethod;
    StructType lastStructType;
    ArrayType lastArrayType;
    List<GcValue> lastFieldValues;
    List<GcValue> lastElements;
    StructInstance lastStructInstance;
    ArrayInstance lastArrayInstance;
    boolean throwOnCreateStruct;
    boolean throwOnCreateArray;

    @Override
    public StructInstance createStruct(final StructType structType, final List<GcValue> fieldValues)
        throws GcException {
      if (throwOnCreateStruct) {
        throw new GcException("createStruct failed");
      }
      lastCalledMethod = "createStruct(StructType,List)";
      lastStructType = structType;
      lastFieldValues = fieldValues;
      lastStructInstance = new StubStructInstance(1L);
      return lastStructInstance;
    }

    @Override
    public StructInstance createStruct(final StructType structType) throws GcException {
      lastCalledMethod = "createStruct(StructType)";
      lastStructType = structType;
      lastStructInstance = new StubStructInstance(2L);
      return lastStructInstance;
    }

    @Override
    public ArrayInstance createArray(final ArrayType arrayType, final List<GcValue> elements)
        throws GcException {
      if (throwOnCreateArray) {
        throw new GcException("createArray failed");
      }
      lastCalledMethod = "createArray(ArrayType,List)";
      lastArrayType = arrayType;
      lastElements = elements;
      lastArrayInstance = new StubArrayInstance(1L);
      return lastArrayInstance;
    }

    @Override
    public ArrayInstance createArray(final ArrayType arrayType, final int length)
        throws GcException {
      lastCalledMethod = "createArray(ArrayType,int)";
      lastArrayType = arrayType;
      lastArrayInstance = new StubArrayInstance(3L);
      return lastArrayInstance;
    }

    @Override
    public I31Instance createI31(final int value) throws GcException {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public I31Instance createI31Unsigned(final int value) throws GcException {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public I31Instance createI31Wrapping(final int value) throws GcException {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public I31Instance createI31WrappingUnsigned(final int value) throws GcException {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public GcValue getStructField(final StructInstance struct, final int fieldIndex) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public void setStructField(
        final StructInstance struct, final int fieldIndex, final GcValue value) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public GcValue getArrayElement(final ArrayInstance array, final int elementIndex) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public void setArrayElement(
        final ArrayInstance array, final int elementIndex, final GcValue value) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public int getArrayLength(final ArrayInstance array) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public GcObject refCast(final GcObject object, final GcReferenceType targetType) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public StructInstance refCastStruct(final GcObject object, final StructType targetStructType) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public ArrayInstance refCastArray(final GcObject object, final ArrayType targetArrayType) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean refTest(final GcObject object, final GcReferenceType targetType) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean refTestStruct(final GcObject object, final StructType targetStructType) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean refTestArray(final GcObject object, final ArrayType targetArrayType) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean refEquals(final GcObject obj1, final GcObject obj2) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean isNull(final GcObject object) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public GcReferenceType getRuntimeType(final GcObject object) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public Optional<GcObject> refCastNullable(
        final GcObject object, final GcReferenceType targetType) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public void copyArrayElements(
        final ArrayInstance sourceArray,
        final int sourceIndex,
        final ArrayInstance destArray,
        final int destIndex,
        final int length) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public void fillArrayElements(
        final ArrayInstance array, final int startIndex, final int length, final GcValue value) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public int registerStructType(final StructType structType) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public int registerArrayType(final ArrayType arrayType) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public long anyRefToRaw(final long objectId) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public long anyRefFromRaw(final long raw) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean anyRefMatchesTy(final long objectId, final int heapTypeOrdinal) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public long externRefConvertAny(final long objectId) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public long anyRefConvertExtern(final long externRefData) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public int eqRefTy(final long objectId) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean eqRefMatchesTy(final long objectId, final int heapTypeOrdinal) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean structRefMatchesTy(final long objectId, final int heapTypeOrdinal) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean arrayRefMatchesTy(final long objectId, final int heapTypeOrdinal) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public boolean releaseObject(final long objectId) {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public GcStats collectGarbage() {
      throw new UnsupportedOperationException("Not tested");
    }

    @Override
    public GcStats getGcStats() {
      throw new UnsupportedOperationException("Not tested");
    }
  }

  /** Minimal stub for StructInstance. */
  static class StubStructInstance implements StructInstance {
    private final long objectId;

    StubStructInstance(final long objectId) {
      this.objectId = objectId;
    }

    @Override
    public StructType getType() {
      return null;
    }

    @Override
    public int getFieldCount() {
      return 0;
    }

    @Override
    public GcValue getField(final int index) {
      return null;
    }

    @Override
    public void setField(final int index, final GcValue value) {}

    @Override
    public long getObjectId() {
      return objectId;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return GcReferenceType.STRUCT_REF;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public boolean isOfType(final GcReferenceType type) {
      return type == GcReferenceType.STRUCT_REF;
    }

    @Override
    public GcObject castTo(final GcReferenceType type) {
      return this;
    }

    @Override
    public boolean refEquals(final GcObject other) {
      return this == other;
    }

    @Override
    public int getSizeBytes() {
      return 0;
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return null;
    }
  }

  /** Minimal stub for ArrayInstance. */
  static class StubArrayInstance implements ArrayInstance {
    private final long objectId;

    StubArrayInstance(final long objectId) {
      this.objectId = objectId;
    }

    @Override
    public ArrayType getType() {
      return null;
    }

    @Override
    public int getLength() {
      return 0;
    }

    @Override
    public GcValue getElement(final int index) {
      return null;
    }

    @Override
    public void setElement(final int index, final GcValue value) {}

    @Override
    public long getObjectId() {
      return objectId;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return GcReferenceType.ARRAY_REF;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public boolean isOfType(final GcReferenceType type) {
      return type == GcReferenceType.ARRAY_REF;
    }

    @Override
    public GcObject castTo(final GcReferenceType type) {
      return this;
    }

    @Override
    public boolean refEquals(final GcObject other) {
      return this == other;
    }

    @Override
    public int getSizeBytes() {
      return 0;
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return null;
    }
  }
}
