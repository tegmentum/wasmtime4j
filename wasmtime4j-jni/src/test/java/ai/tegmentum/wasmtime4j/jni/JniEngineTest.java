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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniEngine}.
 *
 * <p>Tests focus on Java wrapper logic, parameter validation, and defensive programming. Tests
 * verify constructor behavior, resource management, and basic API functionality without requiring
 * actual native library loading.
 *
 * <p>Note: Integration tests with actual WebAssembly compilation are in wasmtime4j-tests.
 */
@DisplayName("JniEngine Tests")
class JniEngineTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long ZERO_HANDLE = 0L;

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create engine with valid handle")
    void shouldCreateEngineWithValidHandle() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertNotNull(engine, "Engine should not be null");
      assertEquals(VALID_HANDLE, engine.getNativeHandle(), "Native handle should match");
    }

    @Test
    @DisplayName("should create engine with zero handle")
    void shouldCreateEngineWithZeroHandle() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      assertNotNull(engine, "Engine should not be null");
      assertEquals(ZERO_HANDLE, engine.getNativeHandle(), "Native handle should be zero");
    }
  }

  @Nested
  @DisplayName("GetNativeHandle Tests")
  class GetNativeHandleTests {

    @Test
    @DisplayName("should return correct native handle")
    void shouldReturnCorrectNativeHandle() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertEquals(VALID_HANDLE, engine.getNativeHandle(), "Should return correct handle");
    }

    @Test
    @DisplayName("should return different handles for different engines")
    void shouldReturnDifferentHandles() {
      final long handle1 = 0xAABBCCDDL;
      final long handle2 = 0x11223344L;

      final JniEngine engine1 = new JniEngine(handle1);
      final JniEngine engine2 = new JniEngine(handle2);

      assertEquals(handle1, engine1.getNativeHandle(), "Engine1 handle should match");
      assertEquals(handle2, engine2.getNativeHandle(), "Engine2 handle should match");
    }
  }

  @Nested
  @DisplayName("IsValid Tests")
  class IsValidTests {

    @Test
    @DisplayName("should return true for valid unclosed engine")
    void shouldReturnTrueForValidUnclosedEngine() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertTrue(engine.isValid(), "Engine with valid handle should be valid");
    }

    @Test
    @DisplayName("should return false for zero handle engine")
    void shouldReturnFalseForZeroHandleEngine() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      assertFalse(engine.isValid(), "Engine with zero handle should not be valid");
    }

    // Note: Test for closed engine state is covered by integration tests
    // because calling close() on a fake handle crashes the JVM
  }

  // Note: Close tests are covered by integration tests in wasmtime4j-tests module
  // because calling close() on objects with fake handles triggers native destructor
  // which crashes the JVM when trying to free memory at an invalid address

  @Nested
  @DisplayName("CreateStore Tests")
  class CreateStoreTests {

    // Note: Test for closed engine createStore is covered by integration tests
    // because calling close() on fake handles crashes the JVM

    @Test
    @DisplayName("should throw on invalid handle")
    void shouldThrowOnInvalidHandle() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      assertThrows(
          IllegalStateException.class, engine::createStore, "Should throw on zero handle engine");
    }

    @Test
    @DisplayName("should throw UnsupportedOperationException for createStore with data")
    void shouldThrowUnsupportedForCreateStoreWithData() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          UnsupportedOperationException.class,
          () -> engine.createStore("test data"),
          "Should throw UnsupportedOperationException for createStore with data");
    }
  }

  @Nested
  @DisplayName("CompileModule Tests")
  class CompileModuleTests {

    @Test
    @DisplayName("should throw on null bytes")
    void shouldThrowOnNullBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileModule(null),
          "Should throw on null bytes");
    }

    @Test
    @DisplayName("should throw on empty bytes")
    void shouldThrowOnEmptyBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileModule(new byte[0]),
          "Should throw on empty bytes");
    }

    // Note: Test for closed engine compileModule is covered by integration tests
    // because calling close() on fake handles crashes the JVM

    @Test
    @DisplayName("should throw on invalid handle")
    void shouldThrowOnInvalidHandle() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      assertThrows(
          IllegalStateException.class,
          () -> engine.compileModule(new byte[] {0x00, 0x61, 0x73, 0x6D}),
          "Should throw on zero handle engine");
    }
  }

  @Nested
  @DisplayName("CompileWat Tests")
  class CompileWatTests {

    @Test
    @DisplayName("should throw on null wat")
    void shouldThrowOnNullWat() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileWat(null),
          "Should throw on null WAT");
    }

    @Test
    @DisplayName("should throw on empty wat")
    void shouldThrowOnEmptyWat() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class, () -> engine.compileWat(""), "Should throw on empty WAT");
    }

    // Note: Test for closed engine compileWat is covered by integration tests
    // because calling close() on fake handles crashes the JVM

    @Test
    @DisplayName("should throw on invalid handle")
    void shouldThrowOnInvalidHandle() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      assertThrows(
          IllegalStateException.class,
          () -> engine.compileWat("(module)"),
          "Should throw on zero handle engine");
    }
  }

  @Nested
  @DisplayName("PrecompileModule Tests")
  class PrecompileModuleTests {

    @Test
    @DisplayName("should throw on null bytes")
    void shouldThrowOnNullBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.precompileModule(null),
          "Should throw on null bytes");
    }

    @Test
    @DisplayName("should throw on empty bytes")
    void shouldThrowOnEmptyBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.precompileModule(new byte[0]),
          "Should throw on empty bytes");
    }

    // Note: Test for closed engine precompileModule is covered by integration tests
    // because calling close() on fake handles crashes the JVM
  }

  @Nested
  @DisplayName("CompileFromStream Tests")
  class CompileFromStreamTests {

    @Test
    @DisplayName("should throw on null stream")
    void shouldThrowOnNullStream() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileFromStream(null),
          "Should throw on null stream");
    }

    // Note: Test for closed engine compileFromStream is covered by integration tests
    // because calling close() on fake handles crashes the JVM
  }

  @Nested
  @DisplayName("Feature Detection Tests")
  class FeatureDetectionTests {

    // Note: Tests for closed engine feature detection are covered by integration tests
    // because calling close() on fake handles crashes the JVM

    @Test
    @DisplayName("should return false for feature check on invalid handle")
    void shouldReturnFalseForInvalidHandle() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      assertFalse(
          engine.isEpochInterruptionEnabled(),
          "Epoch interruption should be false for invalid handle");
      assertFalse(
          engine.isCoredumpOnTrapEnabled(), "Coredump on trap should be false for invalid handle");
      assertFalse(engine.isFuelEnabled(), "Fuel should be false for invalid handle");
    }

    @Test
    @DisplayName("should return false for supportsFeature with null")
    void shouldReturnFalseForSupportsFeatureWithNull() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertFalse(engine.supportsFeature(null), "Should return false for null feature");
    }
  }

  @Nested
  @DisplayName("Precompile Hash Tests")
  class PrecompileHashTests {

    // Note: Tests for closed engine precompile hash are covered by integration tests
    // because calling close() on fake handles crashes the JVM

    @Test
    @DisplayName("should return empty array for invalid handle")
    void shouldReturnEmptyArrayForInvalidHandle() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      final byte[] hash = engine.precompileCompatibilityHash();

      assertNotNull(hash, "Hash should not be null");
      assertEquals(0, hash.length, "Hash should be empty for invalid handle");
    }

    @Test
    @DisplayName("should return false for isPulley on invalid handle")
    void shouldReturnFalseForIsPulleyOnInvalidHandle() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      assertFalse(engine.isPulley(), "isPulley should return false for invalid handle");
    }
  }

  @Nested
  @DisplayName("DetectPrecompiled Tests")
  class DetectPrecompiledTests {

    @Test
    @DisplayName("should throw on null bytes")
    void shouldThrowOnNullBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.detectPrecompiled(null),
          "Should throw on null bytes");
    }

    @Test
    @DisplayName("should return null for empty bytes")
    void shouldReturnNullForEmptyBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertNull(engine.detectPrecompiled(new byte[0]), "Should return null for empty bytes");
    }

    // Note: Test for closed engine detectPrecompiled is covered by integration tests
    // because calling close() on fake handles crashes the JVM
  }

  @Nested
  @DisplayName("Same Engine Tests")
  class SameEngineTests {

    @Test
    @DisplayName("should throw on null other engine")
    void shouldThrowOnNullOtherEngine() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class, () -> engine.same(null), "Should throw on null engine");
    }

    // Note: Test for closed engine same() is covered by integration tests
    // because calling close() on fake handles crashes the JVM

    @Test
    @DisplayName("should return false when comparing with zero handle")
    void shouldReturnFalseWhenComparingWithZeroHandle() {
      final JniEngine engine1 = new JniEngine(VALID_HANDLE);
      final JniEngine engine2 = new JniEngine(ZERO_HANDLE);

      assertFalse(engine1.same(engine2), "Should return false when other has zero handle");
    }
  }

  @Nested
  @DisplayName("Async Tests")
  class AsyncTests {

    // Note: Test for closed engine isAsync is covered by integration tests
    // because calling close() on fake handles crashes the JVM

    @Test
    @DisplayName("should return false for async on invalid handle")
    void shouldReturnFalseForAsyncOnInvalidHandle() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      assertFalse(engine.isAsync(), "isAsync should return false for zero handle");
    }
  }

  @Nested
  @DisplayName("Config and Stats Tests")
  class ConfigAndStatsTests {

    @Test
    @DisplayName("should return null for getConfig - placeholder until native implementation")
    void shouldReturnNullForGetConfig() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      // getConfig returns null as a placeholder until native implementation is complete.
      // This is the documented behavior for the current version.
      assertNull(engine.getConfig(), "getConfig should return null (placeholder behavior)");
    }

    @Test
    @DisplayName("should return 1 for getReferenceCount - placeholder until native implementation")
    void shouldReturnOneForGetReferenceCount() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      // getReferenceCount returns 1 as a placeholder until native implementation is complete.
      // This is the documented behavior for the current version.
      assertEquals(
          1, engine.getReferenceCount(), "getReferenceCount should return 1 (placeholder)");
    }

    @Test
    @DisplayName("should return MAX_VALUE for getMaxInstances - placeholder until native impl")
    void shouldReturnMaxValueForGetMaxInstances() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      // getMaxInstances returns MAX_VALUE as a placeholder until native implementation is complete.
      // This is the documented behavior for the current version (no instance limits).
      assertEquals(
          Integer.MAX_VALUE,
          engine.getMaxInstances(),
          "getMaxInstances should return MAX_VALUE (placeholder)");
    }
  }

  @Nested
  @DisplayName("IncrementEpoch Tests")
  class IncrementEpochTests {

    // Note: Tests that call incrementEpoch() or close() with fake handles are covered
    // by integration tests in wasmtime4j-tests because calling these methods on
    // objects with fake handles triggers native code that crashes the JVM.

    @Test
    @DisplayName("should throw on invalid handle")
    void shouldThrowOnInvalidHandle() {
      final JniEngine engine = new JniEngine(ZERO_HANDLE);

      // Native code throws IllegalArgumentException for null/zero pointer
      assertThrows(
          IllegalArgumentException.class,
          engine::incrementEpoch,
          "Should throw on zero handle engine");
    }

    @Test
    @DisplayName("incrementEpoch method should exist and be public")
    void incrementEpochMethodShouldExistAndBePublic() throws NoSuchMethodException {
      // Verify the incrementEpoch method exists with correct signature
      java.lang.reflect.Method method = JniEngine.class.getMethod("incrementEpoch");
      assertNotNull(method, "incrementEpoch method should exist");
      assertEquals(void.class, method.getReturnType(), "incrementEpoch should return void");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(method.getModifiers()),
          "incrementEpoch should be public");
    }
  }
}
