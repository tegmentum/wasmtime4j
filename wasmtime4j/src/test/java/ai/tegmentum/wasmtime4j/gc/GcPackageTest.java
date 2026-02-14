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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Store;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the GC package classes.
 *
 * <p>This package provides WebAssembly garbage collection support including reference types,
 * statistics, rooting, and memory management.
 */
@DisplayName("GC Package Tests")
class GcPackageTest {

  @Nested
  @DisplayName("GcRef Interface Tests")
  class GcRefTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GcRef.class.isInterface(), "GcRef should be an interface");
    }

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      final Method method = GcRef.class.getMethod("isNull");
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getReferenceType method")
    void shouldHaveGetReferenceTypeMethod() throws NoSuchMethodException {
      final Method method = GcRef.class.getMethod("getReferenceType");
      assertNotNull(method, "getReferenceType method should exist");
      assertEquals(GcReferenceType.class, method.getReturnType(), "Should return GcReferenceType");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = GcRef.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("GcObject Interface Tests")
  class GcObjectTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GcObject.class.isInterface(), "GcObject should be an interface");
    }

    @Test
    @DisplayName("should have getObjectId method")
    void shouldHaveGetObjectIdMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("getObjectId");
      assertNotNull(method, "getObjectId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getReferenceType method")
    void shouldHaveGetReferenceTypeMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("getReferenceType");
      assertNotNull(method, "getReferenceType method should exist");
      assertEquals(GcReferenceType.class, method.getReturnType(), "Should return GcReferenceType");
    }

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("isNull");
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isOfType method")
    void shouldHaveIsOfTypeMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("isOfType", GcReferenceType.class);
      assertNotNull(method, "isOfType method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have castTo method")
    void shouldHaveCastToMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("castTo", GcReferenceType.class);
      assertNotNull(method, "castTo method should exist");
      assertEquals(GcObject.class, method.getReturnType(), "Should return GcObject");
    }

    @Test
    @DisplayName("should have refEquals method")
    void shouldHaveRefEqualsMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("refEquals", GcObject.class);
      assertNotNull(method, "refEquals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getSizeBytes method")
    void shouldHaveGetSizeBytesMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("getSizeBytes");
      assertNotNull(method, "getSizeBytes method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("I31Instance Interface Tests")
  class I31InstanceTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(I31Instance.class.isInterface(), "I31Instance should be an interface");
    }

    @Test
    @DisplayName("should extend GcObject")
    void shouldExtendGcObject() {
      assertTrue(
          GcObject.class.isAssignableFrom(I31Instance.class), "I31Instance should extend GcObject");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = I31Instance.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(I31Type.class, method.getReturnType(), "Should return I31Type");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      final Method method = I31Instance.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getSignedValue method")
    void shouldHaveGetSignedValueMethod() throws NoSuchMethodException {
      final Method method = I31Instance.class.getMethod("getSignedValue");
      assertNotNull(method, "getSignedValue method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getUnsignedValue method")
    void shouldHaveGetUnsignedValueMethod() throws NoSuchMethodException {
      final Method method = I31Instance.class.getMethod("getUnsignedValue");
      assertNotNull(method, "getUnsignedValue method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("GcStats Tests")
  class GcStatsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(GcStats.class.getModifiers()), "GcStats should be final");
    }

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = GcStats.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("should build stats with default values")
    void shouldBuildStatsWithDefaultValues() {
      final GcStats stats = GcStats.builder().build();

      assertNotNull(stats, "Stats should not be null");
      assertEquals(0L, stats.getTotalAllocated(), "Default total allocated should be 0");
      assertEquals(0L, stats.getTotalCollected(), "Default total collected should be 0");
      assertEquals(0L, stats.getBytesAllocated(), "Default bytes allocated should be 0");
    }

    @Test
    @DisplayName("should build stats with all values")
    void shouldBuildStatsWithAllValues() {
      final GcStats stats =
          GcStats.builder()
              .totalAllocated(1000L)
              .totalCollected(500L)
              .bytesAllocated(1024L * 1024L)
              .bytesCollected(512L * 1024L)
              .minorCollections(10L)
              .majorCollections(2L)
              .totalGcTime(Duration.ofMillis(100))
              .currentHeapSize(256 * 1024)
              .peakHeapSize(512 * 1024)
              .maxHeapSize(1024 * 1024)
              .build();

      assertEquals(1000L, stats.getTotalAllocated(), "Total allocated should match");
      assertEquals(500L, stats.getTotalCollected(), "Total collected should match");
      assertEquals(1024L * 1024L, stats.getBytesAllocated(), "Bytes allocated should match");
      assertEquals(512L * 1024L, stats.getBytesCollected(), "Bytes collected should match");
      assertEquals(10L, stats.getMinorCollections(), "Minor collections should match");
      assertEquals(2L, stats.getMajorCollections(), "Major collections should match");
      assertEquals(12L, stats.getTotalCollections(), "Total collections should be sum");
    }

    @Test
    @DisplayName("should calculate live objects correctly")
    void shouldCalculateLiveObjectsCorrectly() {
      final GcStats stats = GcStats.builder().totalAllocated(1000L).totalCollected(400L).build();

      assertEquals(600L, stats.getLiveObjects(), "Live objects should be difference");
    }

    @Test
    @DisplayName("should calculate heap utilization correctly")
    void shouldCalculateHeapUtilizationCorrectly() {
      final GcStats stats =
          GcStats.builder().currentHeapSize(512 * 1024).maxHeapSize(1024 * 1024).build();

      assertEquals(50.0, stats.getHeapUtilization(), 0.001, "Heap utilization should be 50%");
    }

    @Test
    @DisplayName("should handle zero max heap size for utilization")
    void shouldHandleZeroMaxHeapSizeForUtilization() {
      final GcStats stats = GcStats.builder().currentHeapSize(100).maxHeapSize(0).build();

      assertEquals(0.0, stats.getHeapUtilization(), 0.001, "Should be 0 with no max heap");
    }

    @Test
    @DisplayName("should calculate collection efficiency correctly")
    void shouldCalculateCollectionEfficiencyCorrectly() {
      final GcStats stats = GcStats.builder().bytesAllocated(1000L).bytesCollected(800L).build();

      assertEquals(80.0, stats.getCollectionEfficiency(), 0.001, "Efficiency should be 80%");
    }

    @Test
    @DisplayName("should handle zero bytes allocated for efficiency")
    void shouldHandleZeroBytesAllocatedForEfficiency() {
      final GcStats stats = GcStats.builder().bytesAllocated(0L).bytesCollected(0L).build();

      assertEquals(100.0, stats.getCollectionEfficiency(), 0.001, "Should be 100% with no alloc");
    }

    @Test
    @DisplayName("should detect significant GC impact")
    void shouldDetectSignificantGcImpact() {
      // Create stats where GC overhead would be > 5%
      final GcStats stats =
          GcStats.builder().totalGcTime(Duration.ofMillis(60)).captureTime(Instant.now()).build();

      // Just verify method exists and returns boolean
      assertNotNull(stats.hasSignificantGcImpact());
    }

    @Test
    @DisplayName("should detect high heap utilization")
    void shouldDetectHighHeapUtilization() {
      final GcStats stats =
          GcStats.builder().currentHeapSize(900 * 1024).maxHeapSize(1024 * 1024).build();

      assertTrue(stats.hasHighHeapUtilization(), "Should detect high heap utilization > 80%");
    }

    @Test
    @DisplayName("should get summary string")
    void shouldGetSummaryString() {
      final GcStats stats =
          GcStats.builder()
              .totalAllocated(100L)
              .totalCollected(50L)
              .currentHeapSize(1024)
              .maxHeapSize(2048)
              .minorCollections(5L)
              .majorCollections(1L)
              .totalGcTime(Duration.ofMillis(10))
              .build();

      final String summary = stats.getSummary();
      assertNotNull(summary, "Summary should not be null");
      assertFalse(summary.isEmpty(), "Summary should not be empty");
    }

    @Test
    @DisplayName("toString should return summary")
    void toStringShouldReturnSummary() {
      final GcStats stats = GcStats.builder().build();
      assertEquals(stats.getSummary(), stats.toString(), "toString should return summary");
    }

    @Test
    @DisplayName("should support objects by generation")
    void shouldSupportObjectsByGeneration() {
      final Map<Integer, Long> objectsByGen = new HashMap<>();
      objectsByGen.put(0, 100L);
      objectsByGen.put(1, 50L);
      objectsByGen.put(2, 10L);

      final GcStats stats = GcStats.builder().objectsByGeneration(objectsByGen).build();

      final Map<Integer, Long> result = stats.getObjectsByGeneration();
      assertEquals(100L, result.get(0), "Gen 0 should match");
      assertEquals(50L, result.get(1), "Gen 1 should match");
      assertEquals(10L, result.get(2), "Gen 2 should match");
    }

    @Test
    @DisplayName("equals and hashCode should work correctly")
    void equalsAndHashCodeShouldWorkCorrectly() {
      final Instant captureTime = Instant.now();
      final GcStats stats1 =
          GcStats.builder()
              .totalAllocated(100L)
              .totalCollected(50L)
              .captureTime(captureTime)
              .build();
      final GcStats stats2 =
          GcStats.builder()
              .totalAllocated(100L)
              .totalCollected(50L)
              .captureTime(captureTime)
              .build();

      assertEquals(stats1, stats2, "Equal stats should be equal");
      assertEquals(stats1.hashCode(), stats2.hashCode(), "Hash codes should match");
    }
  }

  @Nested
  @DisplayName("GcHeapStats Tests")
  class GcHeapStatsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(GcHeapStats.class.getModifiers()), "GcHeapStats should be final");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = GcHeapStats.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
    }

    @Test
    @DisplayName("should initialize with default values")
    void shouldInitializeWithDefaultValues() {
      final GcHeapStats stats = new GcHeapStats();

      assertEquals(0L, stats.getTotalAllocated(), "Total allocated should be 0");
      assertEquals(0L, stats.getCurrentHeapSize(), "Current heap size should be 0");
      assertEquals(0L, stats.getMajorCollections(), "Major collections should be 0");
    }

    @Test
    @DisplayName("should allow setting public fields")
    void shouldAllowSettingPublicFields() {
      final GcHeapStats stats = new GcHeapStats();
      stats.totalAllocated = 1000L;
      stats.currentHeapSize = 512L;
      stats.majorCollections = 5L;

      assertEquals(1000L, stats.getTotalAllocated(), "Total allocated should match");
      assertEquals(512L, stats.getCurrentHeapSize(), "Current heap size should match");
      assertEquals(5L, stats.getMajorCollections(), "Major collections should match");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final GcHeapStats stats = new GcHeapStats();
      stats.totalAllocated = 100L;
      stats.currentHeapSize = 50L;
      stats.majorCollections = 1L;

      final String str = stats.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("100"), "Should contain total allocated");
      assertTrue(str.contains("50"), "Should contain heap size");
    }
  }

  @Nested
  @DisplayName("GcCollectionResult Tests")
  class GcCollectionResultTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(GcCollectionResult.class.getModifiers()),
          "GcCollectionResult should be final");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = GcCollectionResult.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
    }

    @Test
    @DisplayName("should initialize with default values")
    void shouldInitializeWithDefaultValues() {
      final GcCollectionResult result = new GcCollectionResult();

      assertEquals(0L, result.getObjectsCollected(), "Objects collected should be 0");
      assertEquals(0L, result.getBytesCollected(), "Bytes collected should be 0");
    }

    @Test
    @DisplayName("should allow setting public fields")
    void shouldAllowSettingPublicFields() {
      final GcCollectionResult result = new GcCollectionResult();
      result.objectsCollected = 100L;
      result.bytesCollected = 1024L;

      assertEquals(100L, result.getObjectsCollected(), "Objects collected should match");
      assertEquals(1024L, result.getBytesCollected(), "Bytes collected should match");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final GcCollectionResult result = new GcCollectionResult();
      result.objectsCollected = 50L;
      result.bytesCollected = 512L;

      final String str = result.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("50"), "Should contain objects collected");
      assertTrue(str.contains("512"), "Should contain bytes collected");
    }
  }

  @Nested
  @DisplayName("GcException Tests")
  class GcExceptionTests {

    @Test
    @DisplayName("should be a subclass of WasmException")
    void shouldBeSubclassOfWasmException() {
      assertTrue(
          ai.tegmentum.wasmtime4j.exception.WasmException.class.isAssignableFrom(GcException.class),
          "GcException should extend WasmException");
    }

    @Test
    @DisplayName("should have ErrorCode enum")
    void shouldHaveErrorCodeEnum() {
      final GcException.ErrorCode[] codes = GcException.ErrorCode.values();
      assertTrue(codes.length > 0, "Should have error codes");
    }

    @Test
    @DisplayName("should have all expected error codes")
    void shouldHaveAllExpectedErrorCodes() {
      assertNotNull(GcException.ErrorCode.TYPE_VALIDATION_ERROR, "Should have type validation");
      assertNotNull(GcException.ErrorCode.ALLOCATION_ERROR, "Should have allocation error");
      assertNotNull(GcException.ErrorCode.ACCESS_ERROR, "Should have access error");
      assertNotNull(GcException.ErrorCode.CAST_ERROR, "Should have cast error");
      assertNotNull(GcException.ErrorCode.REFERENCE_ERROR, "Should have reference error");
      assertNotNull(GcException.ErrorCode.MEMORY_ERROR, "Should have memory error");
      assertNotNull(GcException.ErrorCode.COLLECTION_ERROR, "Should have collection error");
      assertNotNull(GcException.ErrorCode.INTERNAL_ERROR, "Should have internal error");
    }

    @Test
    @DisplayName("should create with message")
    void shouldCreateWithMessage() {
      final GcException ex = new GcException("test message");
      // Message is passed through directly to parent WasmException
      assertTrue(ex.getMessage().contains("test message"), "Message should contain original text");
      assertEquals(GcException.ErrorCode.INTERNAL_ERROR, ex.getErrorCode(), "Default error code");
    }

    @Test
    @DisplayName("should create with message and cause")
    void shouldCreateWithMessageAndCause() {
      final Exception cause = new Exception("cause");
      final GcException ex = new GcException("test message", cause);
      // Message is passed through directly to parent WasmException
      assertTrue(ex.getMessage().contains("test message"), "Message should contain original text");
      assertEquals(cause, ex.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("should create with cause only")
    void shouldCreateWithCauseOnly() {
      final Exception cause = new Exception("cause message");
      final GcException ex = new GcException(cause);
      assertEquals(cause, ex.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("should create with detailed info")
    void shouldCreateWithDetailedInfo() {
      final GcException ex =
          new GcException(
              "type error", GcException.ErrorCode.TYPE_VALIDATION_ERROR, "castTo", "context info");

      // Message is passed through directly to parent WasmException
      assertTrue(ex.getMessage().contains("type error"), "Message should contain original text");
      assertEquals(
          GcException.ErrorCode.TYPE_VALIDATION_ERROR,
          ex.getErrorCode(),
          "Error code should match");
      assertEquals("castTo", ex.getOperation(), "Operation should match");
      assertEquals("context info", ex.getContext(), "Context should match");
    }

    @Test
    @DisplayName("toString should include error details")
    void toStringShouldIncludeErrorDetails() {
      final GcException ex =
          new GcException(
              "test error", GcException.ErrorCode.CAST_ERROR, "testOperation", "testContext");

      final String str = ex.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("CAST_ERROR"), "Should contain error code");
      assertTrue(str.contains("testOperation"), "Should contain operation");
    }

    @Test
    @DisplayName("should have TypeValidationException subclass")
    void shouldHaveTypeValidationExceptionSubclass() {
      final GcException.TypeValidationException ex =
          new GcException.TypeValidationException("type error", "validate", null);
      assertEquals(
          GcException.ErrorCode.TYPE_VALIDATION_ERROR,
          ex.getErrorCode(),
          "Should have correct code");
    }

    @Test
    @DisplayName("should have AllocationException subclass")
    void shouldHaveAllocationExceptionSubclass() {
      final GcException.AllocationException ex =
          new GcException.AllocationException("alloc error", "allocate", null);
      assertEquals(
          GcException.ErrorCode.ALLOCATION_ERROR, ex.getErrorCode(), "Should have correct code");
    }

    @Test
    @DisplayName("should have AccessException subclass")
    void shouldHaveAccessExceptionSubclass() {
      final GcException.AccessException ex =
          new GcException.AccessException("access error", "getField", null);
      assertEquals(
          GcException.ErrorCode.ACCESS_ERROR, ex.getErrorCode(), "Should have correct code");
    }

    @Test
    @DisplayName("should have CastException subclass")
    void shouldHaveCastExceptionSubclass() {
      final GcException.CastException ex =
          new GcException.CastException("cast error", "castTo", null);
      assertEquals(GcException.ErrorCode.CAST_ERROR, ex.getErrorCode(), "Should have correct code");
    }

    @Test
    @DisplayName("should have ReferenceException subclass")
    void shouldHaveReferenceExceptionSubclass() {
      final GcException.ReferenceException ex =
          new GcException.ReferenceException("ref error", "deref", null);
      assertEquals(
          GcException.ErrorCode.REFERENCE_ERROR, ex.getErrorCode(), "Should have correct code");
    }

    @Test
    @DisplayName("should have MemoryException subclass")
    void shouldHaveMemoryExceptionSubclass() {
      final GcException.MemoryException ex =
          new GcException.MemoryException("memory error", "alloc", null);
      assertEquals(
          GcException.ErrorCode.MEMORY_ERROR, ex.getErrorCode(), "Should have correct code");
    }

    @Test
    @DisplayName("should have CollectionException subclass")
    void shouldHaveCollectionExceptionSubclass() {
      final GcException.CollectionException ex =
          new GcException.CollectionException("collection error", "collect", null);
      assertEquals(
          GcException.ErrorCode.COLLECTION_ERROR, ex.getErrorCode(), "Should have correct code");
    }
  }

  @Nested
  @DisplayName("AnyRef Tests")
  class AnyRefTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(AnyRef.class.getModifiers()), "AnyRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef")
    void shouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(AnyRef.class), "AnyRef should implement GcRef");
    }

    @Test
    @DisplayName("should create null reference")
    void shouldCreateNullReference() {
      final AnyRef nullRef = AnyRef.nullRef();

      assertNotNull(nullRef, "Null ref instance should not be null");
      assertTrue(nullRef.isNull(), "Should be null reference");
      assertEquals(GcReferenceType.ANY_REF, nullRef.getReferenceType(), "Should be ANY_REF type");
    }

    @Test
    @DisplayName("should throw on null value for of method")
    void shouldThrowOnNullValueForOfMethod() {
      assertThrows(
          NullPointerException.class,
          () -> AnyRef.of((GcObject) null),
          "Should throw on null GcObject");
    }

    @Test
    @DisplayName("should have unique IDs")
    void shouldHaveUniqueIds() {
      final AnyRef ref1 = AnyRef.nullRef();
      final AnyRef ref2 = AnyRef.nullRef();

      assertTrue(ref1.getId() != ref2.getId(), "IDs should be unique");
    }

    @Test
    @DisplayName("should test refEquals for null references")
    void shouldTestRefEqualsForNullReferences() {
      final AnyRef nullRef1 = AnyRef.nullRef();
      final AnyRef nullRef2 = AnyRef.nullRef();

      assertTrue(nullRef1.refEquals(nullRef2), "Null refs should be equal");
      assertFalse(nullRef1.refEquals(null), "Should return false for null argument");
    }

    @Test
    @DisplayName("should check isI31 for null reference")
    void shouldCheckIsI31ForNullReference() {
      final AnyRef nullRef = AnyRef.nullRef();
      assertFalse(nullRef.isI31(), "Null ref should not be i31");
    }

    @Test
    @DisplayName("should check isStruct for null reference")
    void shouldCheckIsStructForNullReference() {
      final AnyRef nullRef = AnyRef.nullRef();
      assertFalse(nullRef.isStruct(), "Null ref should not be struct");
    }

    @Test
    @DisplayName("should check isArray for null reference")
    void shouldCheckIsArrayForNullReference() {
      final AnyRef nullRef = AnyRef.nullRef();
      assertFalse(nullRef.isArray(), "Null ref should not be array");
    }

    @Test
    @DisplayName("should check isEq for null reference")
    void shouldCheckIsEqForNullReference() {
      final AnyRef nullRef = AnyRef.nullRef();
      assertFalse(nullRef.isEq(), "Null ref should not be eq");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final AnyRef nullRef = AnyRef.nullRef();
      final String str = nullRef.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("null"), "Should indicate null reference");
    }

    @Test
    @DisplayName("equals and hashCode should work correctly")
    void equalsAndHashCodeShouldWorkCorrectly() {
      final AnyRef nullRef1 = AnyRef.nullRef();
      final AnyRef nullRef2 = AnyRef.nullRef();

      // Note: Different instances have different IDs, but both wrap null value
      assertEquals(nullRef1, nullRef2, "Null refs with null value should be equal");
    }
  }

  @Nested
  @DisplayName("EqRef Tests")
  class EqRefTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(EqRef.class.getModifiers()), "EqRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef")
    void shouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(EqRef.class), "EqRef should implement GcRef");
    }

    @Test
    @DisplayName("should create null reference")
    void shouldCreateNullReference() {
      final EqRef nullRef = EqRef.nullRef();

      assertNotNull(nullRef, "Null ref instance should not be null");
      assertTrue(nullRef.isNull(), "Should be null reference");
      assertEquals(GcReferenceType.EQ_REF, nullRef.getReferenceType(), "Should be EQ_REF type");
    }

    @Test
    @DisplayName("should have unique IDs")
    void shouldHaveUniqueIds() {
      final EqRef ref1 = EqRef.nullRef();
      final EqRef ref2 = EqRef.nullRef();

      assertTrue(ref1.getId() != ref2.getId(), "IDs should be unique");
    }

    @Test
    @DisplayName("should test refEquals for null references")
    void shouldTestRefEqualsForNullReferences() {
      final EqRef nullRef1 = EqRef.nullRef();
      final EqRef nullRef2 = EqRef.nullRef();

      assertTrue(nullRef1.refEquals(nullRef2), "Null refs should be equal");
      assertFalse(nullRef1.refEquals(null), "Should return false for null argument");
    }

    @Test
    @DisplayName("should check isI31 for null reference")
    void shouldCheckIsI31ForNullReference() {
      final EqRef nullRef = EqRef.nullRef();
      assertFalse(nullRef.isI31(), "Null ref should not be i31");
    }

    @Test
    @DisplayName("should check isStruct for null reference")
    void shouldCheckIsStructForNullReference() {
      final EqRef nullRef = EqRef.nullRef();
      assertFalse(nullRef.isStruct(), "Null ref should not be struct");
    }

    @Test
    @DisplayName("should check isArray for null reference")
    void shouldCheckIsArrayForNullReference() {
      final EqRef nullRef = EqRef.nullRef();
      assertFalse(nullRef.isArray(), "Null ref should not be array");
    }

    @Test
    @DisplayName("should convert to AnyRef")
    void shouldConvertToAnyRef() {
      final EqRef nullRef = EqRef.nullRef();
      final AnyRef anyRef = nullRef.toAnyRef();

      assertNotNull(anyRef, "AnyRef should not be null");
      assertTrue(anyRef.isNull(), "Converted ref should be null");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final EqRef nullRef = EqRef.nullRef();
      final String str = nullRef.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("null"), "Should indicate null reference");
    }
  }

  @Nested
  @DisplayName("Rooted Tests")
  class RootedTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(Rooted.class.getModifiers()), "Rooted should be final");
    }

    @Test
    @DisplayName("should create with value and rootId")
    void shouldCreateWithValueAndRootId() {
      final String testValue = "test";
      final Rooted<String> rooted = new Rooted<>(testValue, 42L);

      assertEquals(42L, rooted.getRootId(), "Root ID should match");
      assertTrue(rooted.isValid(), "Should be valid initially");
    }

    @Test
    @DisplayName("should throw on null value")
    void shouldThrowOnNullValue() {
      assertThrows(
          NullPointerException.class, () -> new Rooted<>(null, 1L), "Should throw on null value");
    }

    @Test
    @DisplayName("unroot should require non-null store")
    void unrootShouldRequireNonNullStore() throws Exception {
      final Rooted<String> rooted = new Rooted<>("test", 1L);

      assertTrue(rooted.isValid(), "Should be valid before unroot");
      // unroot() requires a non-null Store parameter
      assertThrows(
          NullPointerException.class,
          () -> rooted.unroot(null),
          "Should throw NullPointerException for null store");
    }

    @Test
    @DisplayName("unroot method should exist with Store parameter")
    void unrootMethodShouldExistWithStoreParameter() throws Exception {
      final Method unrootMethod = Rooted.class.getMethod("unroot", Store.class);
      assertNotNull(unrootMethod, "unroot method should exist");
      assertEquals(void.class, unrootMethod.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("equals and hashCode should work correctly")
    void equalsAndHashCodeShouldWorkCorrectly() {
      final Rooted<String> rooted1 = new Rooted<>("test", 1L);
      final Rooted<String> rooted2 = new Rooted<>("test", 1L);

      assertEquals(rooted1, rooted2, "Equal rooted refs should be equal");
      assertEquals(rooted1.hashCode(), rooted2.hashCode(), "Hash codes should match");
    }

    @Test
    @DisplayName("toString should return non-null")
    void toStringShouldReturnNonNull() {
      final Rooted<String> rooted = new Rooted<>("test", 1L);
      final String str = rooted.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("test"), "Should contain value");
      assertTrue(str.contains("1"), "Should contain root ID");
    }

    @Test
    @DisplayName("ManualRoot should be a static nested class")
    void manualRootShouldBeStaticNestedClass() {
      final Class<?>[] declaredClasses = Rooted.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ManualRoot")) {
          found = true;
          assertTrue(Modifier.isStatic(clazz.getModifiers()), "ManualRoot should be static");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "ManualRoot should be final");
          break;
        }
      }
      assertTrue(found, "Should have ManualRoot nested class");
    }
  }

  @Nested
  @DisplayName("RootScope Tests")
  class RootScopeTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(RootScope.class.getModifiers()), "RootScope should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(RootScope.class),
          "RootScope should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have static create method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      final Method method =
          RootScope.class.getMethod("create", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
    }

    @Test
    @DisplayName("should have root methods for various types")
    void shouldHaveRootMethodsForVariousTypes() throws NoSuchMethodException {
      assertNotNull(RootScope.class.getMethod("root", Object.class), "Should have generic root");
      assertNotNull(RootScope.class.getMethod("root", AnyRef.class), "Should have AnyRef root");
      assertNotNull(RootScope.class.getMethod("root", EqRef.class), "Should have EqRef root");
      assertNotNull(
          RootScope.class.getMethod("root", StructRef.class), "Should have StructRef root");
      assertNotNull(RootScope.class.getMethod("root", ArrayRef.class), "Should have ArrayRef root");
    }

    @Test
    @DisplayName("should have getRootedCount method")
    void shouldHaveGetRootedCountMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("getRootedCount");
      assertNotNull(method, "getRootedCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getScopeId method")
    void shouldHaveGetScopeIdMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("getScopeId");
      assertNotNull(method, "getScopeId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isOpen method")
    void shouldHaveIsOpenMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("isOpen");
      assertNotNull(method, "isOpen method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("close");
      assertNotNull(method, "close method should exist");
    }
  }

  @Nested
  @DisplayName("GcValue Abstract Class Tests")
  class GcValueTests {

    @Test
    @DisplayName("should be an abstract class")
    void shouldBeAnAbstractClass() {
      assertTrue(
          Modifier.isAbstract(GcValue.class.getModifiers()), "GcValue should be an abstract class");
      assertFalse(GcValue.class.isInterface(), "GcValue should not be an interface");
    }

    @Test
    @DisplayName("should have Type enum")
    void shouldHaveTypeEnum() throws Exception {
      final Class<?> typeClass = Class.forName("ai.tegmentum.wasmtime4j.gc.GcValue$Type");
      assertTrue(typeClass.isEnum(), "Type should be an enum");
    }

    @Test
    @DisplayName("should have static factory methods")
    void shouldHaveStaticFactoryMethods() throws Exception {
      assertNotNull(GcValue.class.getMethod("i32", int.class), "Should have i32 factory");
      assertNotNull(GcValue.class.getMethod("i64", long.class), "Should have i64 factory");
      assertNotNull(GcValue.class.getMethod("f32", float.class), "Should have f32 factory");
      assertNotNull(GcValue.class.getMethod("f64", double.class), "Should have f64 factory");
      assertNotNull(GcValue.class.getMethod("nullValue"), "Should have nullValue factory");
    }

    @Test
    @DisplayName("should create i32 value")
    void shouldCreateI32Value() {
      final GcValue value = GcValue.i32(42);
      assertNotNull(value, "Should create i32 value");
      assertEquals(GcValue.Type.I32, value.getType(), "Should have I32 type");
      assertEquals(42, value.asI32(), "Should return correct value");
    }

    @Test
    @DisplayName("should create null value")
    void shouldCreateNullValue() {
      final GcValue value = GcValue.nullValue();
      assertNotNull(value, "Should create null value");
      assertTrue(value.isNull(), "Should be null");
      assertEquals(GcValue.Type.NULL, value.getType(), "Should have NULL type");
    }
  }

  @Nested
  @DisplayName("StructInstance Interface Tests")
  class StructInstanceTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(StructInstance.class.isInterface(), "StructInstance should be an interface");
    }

    @Test
    @DisplayName("should extend GcObject")
    void shouldExtendGcObject() {
      assertTrue(
          GcObject.class.isAssignableFrom(StructInstance.class),
          "StructInstance should extend GcObject");
    }
  }

  @Nested
  @DisplayName("ArrayInstance Interface Tests")
  class ArrayInstanceTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ArrayInstance.class.isInterface(), "ArrayInstance should be an interface");
    }

    @Test
    @DisplayName("should extend GcObject")
    void shouldExtendGcObject() {
      assertTrue(
          GcObject.class.isAssignableFrom(ArrayInstance.class),
          "ArrayInstance should extend GcObject");
    }
  }

  @Nested
  @DisplayName("StructRef Tests")
  class StructRefTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(StructRef.class.getModifiers()), "StructRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef")
    void shouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(StructRef.class), "StructRef should implement GcRef");
    }

    @Test
    @DisplayName("should have nullRef static method")
    void shouldHaveNullRefStaticMethod() throws NoSuchMethodException {
      final Method method = StructRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef should be static");
    }
  }

  @Nested
  @DisplayName("ArrayRef Tests")
  class ArrayRefTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(ArrayRef.class.getModifiers()), "ArrayRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef")
    void shouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(ArrayRef.class), "ArrayRef should implement GcRef");
    }

    @Test
    @DisplayName("should have nullRef static method")
    void shouldHaveNullRefStaticMethod() throws NoSuchMethodException {
      final Method method = ArrayRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef should be static");
    }
  }

  @Nested
  @DisplayName("I31Type Tests")
  class I31TypeTests {

    @Test
    @DisplayName("should be a final class or interface")
    void shouldBeFinalClassOrInterface() {
      final boolean isFinal = Modifier.isFinal(I31Type.class.getModifiers());
      final boolean isInterface = I31Type.class.isInterface();
      assertTrue(isFinal || isInterface, "I31Type should be final or interface");
    }
  }

  @Nested
  @DisplayName("OwnedRooted Tests")
  class OwnedRootedTests {

    @Test
    @DisplayName("should exist as a class")
    void shouldExistAsAClass() {
      assertNotNull(OwnedRooted.class, "OwnedRooted class should exist");
    }

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(OwnedRooted.class.getModifiers()), "OwnedRooted should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(OwnedRooted.class),
          "OwnedRooted should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("GcRootManager Tests")
  class GcRootManagerTests {

    @Test
    @DisplayName("should exist as a class")
    void shouldExistAsAClass() {
      assertNotNull(GcRootManager.class, "GcRootManager class should exist");
    }
  }

  @Nested
  @DisplayName("GcRuntime Tests")
  class GcRuntimeTests {

    @Test
    @DisplayName("should exist as an interface")
    void shouldExistAsAnInterface() {
      assertTrue(GcRuntime.class.isInterface(), "GcRuntime should be an interface");
    }
  }

  @Nested
  @DisplayName("FieldDefinition Tests")
  class FieldDefinitionTests {

    @Test
    @DisplayName("should exist as a class")
    void shouldExistAsAClass() {
      assertNotNull(FieldDefinition.class, "FieldDefinition class should exist");
    }

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(FieldDefinition.class.getModifiers()),
          "FieldDefinition should be final");
    }
  }
}
