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

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
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
 * <p>JniEngine extends JniResource, which rejects zero handles in the constructor and provides
 * thread-safe lifecycle management via AtomicBoolean and phantom reference cleanup.
 *
 * <p>Note: Integration tests with actual WebAssembly compilation are in wasmtime4j-tests.
 */
@DisplayName("JniEngine Tests")
class JniEngineTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long VALID_HANDLE_2 = 0xAABBCCDDL;
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
    @DisplayName("should reject zero handle")
    void shouldRejectZeroHandle() {
      assertThrows(
          RuntimeException.class,
          () -> new JniEngine(ZERO_HANDLE),
          "Constructor should reject zero handle");
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
      final JniEngine engine1 = new JniEngine(VALID_HANDLE);
      final JniEngine engine2 = new JniEngine(VALID_HANDLE_2);

      assertEquals(VALID_HANDLE, engine1.getNativeHandle(), "Engine1 handle should match");
      assertEquals(VALID_HANDLE_2, engine2.getNativeHandle(), "Engine2 handle should match");
    }

    @Test
    @DisplayName("should throw when engine is closed")
    void shouldThrowWhenClosed() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          engine::getNativeHandle,
          "Should throw when engine is closed");
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
    @DisplayName("should return false for closed engine")
    void shouldReturnFalseForClosedEngine() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertFalse(engine.isValid(), "Closed engine should not be valid");
    }
  }

  @Nested
  @DisplayName("CreateStore Tests")
  class CreateStoreTests {

    @Test
    @DisplayName("should throw when engine is closed")
    void shouldThrowWhenClosed() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertThrows(
          JniResourceException.class, engine::createStore, "Should throw on closed engine");
    }

    @Test
    @DisplayName("should throw when createStore with data on closed engine")
    void shouldThrowWhenClosedForCreateStoreWithData() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          () -> engine.createStore("test data"),
          "Should throw on closed engine");
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

    @Test
    @DisplayName("should throw when engine is closed")
    void shouldThrowWhenClosed() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          () -> engine.compileModule(new byte[] {0x00, 0x61, 0x73, 0x6D}),
          "Should throw on closed engine");
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

    @Test
    @DisplayName("should throw when engine is closed")
    void shouldThrowWhenClosed() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertThrows(
          JniResourceException.class,
          () -> engine.compileWat("(module)"),
          "Should throw on closed engine");
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
  }

  @Nested
  @DisplayName("Feature Detection Tests")
  class FeatureDetectionTests {

    @Test
    @DisplayName("should return false for feature check on closed engine")
    void shouldReturnFalseForClosedEngine() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertFalse(
          engine.isEpochInterruptionEnabled(),
          "Epoch interruption should be false for closed engine");
      assertFalse(
          engine.isCoredumpOnTrapEnabled(), "Coredump on trap should be false for closed engine");
      assertFalse(engine.isFuelEnabled(), "Fuel should be false for closed engine");
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

    @Test
    @DisplayName("should return empty array for closed engine")
    void shouldReturnEmptyArrayForClosedEngine() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      final byte[] hash = engine.precompileCompatibilityHash();

      assertNotNull(hash, "Hash should not be null");
      assertEquals(0, hash.length, "Hash should be empty for closed engine");
    }

    @Test
    @DisplayName("should return false for isPulley on closed engine")
    void shouldReturnFalseForIsPulleyOnClosedEngine() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertFalse(engine.isPulley(), "isPulley should return false for closed engine");
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

    @Test
    @DisplayName("should return false when closed")
    void shouldReturnFalseWhenClosed() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      final JniEngine other = new JniEngine(VALID_HANDLE_2);
      engine.markClosedForTesting();

      assertFalse(engine.same(other), "Should return false when engine is closed");
    }
  }

  @Nested
  @DisplayName("Async Tests")
  class AsyncTests {

    @Test
    @DisplayName("should return false for async on closed engine")
    void shouldReturnFalseForAsyncOnClosedEngine() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertFalse(engine.isAsync(), "isAsync should return false for closed engine");
    }
  }

  @Nested
  @DisplayName("Config and Stats Tests")
  class ConfigAndStatsTests {

    @Test
    @DisplayName("should return default EngineConfig when created without config")
    void shouldReturnDefaultConfigWhenCreatedWithoutConfig() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      final ai.tegmentum.wasmtime4j.config.EngineConfig config = engine.getConfig();
      assertNotNull(config, "getConfig should return a non-null EngineConfig");
    }
  }

  @Nested
  @DisplayName("IncrementEpoch Tests")
  class IncrementEpochTests {

    @Test
    @DisplayName("should throw when engine is closed")
    void shouldThrowWhenClosed() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      engine.markClosedForTesting();

      assertThrows(
          JniResourceException.class, engine::incrementEpoch, "Should throw on closed engine");
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

  @Nested
  @DisplayName("JniResource Integration Tests")
  class JniResourceIntegrationTests {

    @Test
    @DisplayName("should report correct resource type")
    void shouldReportCorrectResourceType() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertTrue(
          engine.toString().contains("JniEngine"),
          "toString should contain resource type JniEngine");
    }

    @Test
    @DisplayName("should report closed status correctly")
    void shouldReportClosedStatusCorrectly() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);

      assertFalse(engine.isClosed(), "Should not be closed initially");
      engine.markClosedForTesting();
      assertTrue(engine.isClosed(), "Should be closed after marking");
    }
  }
}
