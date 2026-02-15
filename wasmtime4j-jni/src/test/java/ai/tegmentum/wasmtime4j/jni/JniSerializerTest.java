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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniSerializer}.
 *
 * <p>Tests focus on Java wrapper logic, parameter validation, and defensive programming. Tests
 * verify constructor behavior, resource management, and basic API functionality without requiring
 * actual native library loading.
 *
 * <p>Note: Integration tests with actual serialization are in wasmtime4j-tests.
 */
@DisplayName("JniSerializer Tests")
class JniSerializerTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long ZERO_HANDLE = 0L;

  private JniEngine testEngine;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create serializer with valid handle")
    void shouldCreateSerializerWithValidHandle() {
      final JniSerializer serializer = new JniSerializer(VALID_HANDLE);

      assertEquals(VALID_HANDLE, serializer.getNativeHandle(), "Handle should match");
    }

    @Test
    @DisplayName("should throw on zero handle")
    void shouldThrowOnZeroHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniSerializer(ZERO_HANDLE),
          "Should throw on zero handle");
    }
  }

  @Nested
  @DisplayName("GetNativeHandle Tests")
  class GetNativeHandleTests {

    @Test
    @DisplayName("should return correct native handle")
    void shouldReturnCorrectNativeHandle() {
      final JniSerializer serializer = new JniSerializer(VALID_HANDLE);

      assertEquals(VALID_HANDLE, serializer.getNativeHandle(), "Should return correct handle");
    }

    // Note: Test for closed serializer getNativeHandle is covered by integration tests
    // because calling close() on fake handles triggers native destructor which crashes the JVM
  }

  // Note: Close tests are covered by integration tests in wasmtime4j-tests module
  // because calling close() on objects with fake handles triggers native destructor
  // which crashes the JVM when trying to free memory at an invalid address

  @Nested
  @DisplayName("Serialize Tests")
  class SerializeTests {

    @Test
    @DisplayName("should throw on null engine")
    void shouldThrowOnNullEngine() {
      final JniSerializer serializer = new JniSerializer(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> serializer.serialize(null, new byte[] {0x00, 0x61, 0x73, 0x6D}),
          "Should throw on null engine");
    }

    @Test
    @DisplayName("should throw on null module bytes")
    void shouldThrowOnNullModuleBytes() {
      final JniSerializer serializer = new JniSerializer(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> serializer.serialize(testEngine, null),
          "Should throw on null module bytes");
    }

    // Note: Test for closed serializer serialize is covered by integration tests
    // because calling close() on fake handles triggers native destructor which crashes the JVM

    @Test
    @DisplayName("should throw on non-JniEngine")
    void shouldThrowOnNonJniEngine() {
      final JniSerializer serializer = new JniSerializer(VALID_HANDLE);

      // Create a mock non-JniEngine
      final ai.tegmentum.wasmtime4j.Engine nonJniEngine = createNonJniEngine();

      assertThrows(
          IllegalArgumentException.class,
          () -> serializer.serialize(nonJniEngine, new byte[] {0x00, 0x61, 0x73, 0x6D}),
          "Should throw on non-JniEngine");
    }

    private ai.tegmentum.wasmtime4j.Engine createNonJniEngine() {
      return new ai.tegmentum.wasmtime4j.Engine() {
        @Override
        public ai.tegmentum.wasmtime4j.Store createStore() {
          return null;
        }

        @Override
        public ai.tegmentum.wasmtime4j.Store createStore(final Object data) {
          return null;
        }

        @Override
        public ai.tegmentum.wasmtime4j.WasmRuntime getRuntime() {
          return null;
        }

        @Override
        public boolean isEpochInterruptionEnabled() {
          return false;
        }

        @Override
        public boolean isCoredumpOnTrapEnabled() {
          return false;
        }

        @Override
        public boolean isFuelEnabled() {
          return false;
        }

        @Override
        public long getStackSizeLimit() {
          return 0;
        }

        @Override
        public int getMemoryLimitPages() {
          return 0;
        }

        @Override
        public boolean supportsFeature(final ai.tegmentum.wasmtime4j.WasmFeature feature) {
          return false;
        }

        @Override
        public boolean isValid() {
          return true;
        }

        @Override
        public void incrementEpoch() {}

        @Override
        public ai.tegmentum.wasmtime4j.Module compileModule(final byte[] wasmBytes) {
          return null;
        }

        @Override
        public ai.tegmentum.wasmtime4j.Module compileWat(final String wat) {
          return null;
        }

        @Override
        public byte[] precompileModule(final byte[] wasmBytes) {
          return new byte[0];
        }

        @Override
        public ai.tegmentum.wasmtime4j.Module compileFromStream(final java.io.InputStream stream) {
          return null;
        }

        @Override
        public ai.tegmentum.wasmtime4j.config.EngineConfig getConfig() {
          return null;
        }

        @Override
        public void close() {}

        @Override
        public boolean isPulley() {
          return false;
        }

        @Override
        public byte[] precompileCompatibilityHash() {
          return new byte[0];
        }

        @Override
        public ai.tegmentum.wasmtime4j.Precompiled detectPrecompiled(final byte[] bytes) {
          return null;
        }

        @Override
        public boolean same(final ai.tegmentum.wasmtime4j.Engine other) {
          return false;
        }

        @Override
        public boolean isAsync() {
          return false;
        }
      };
    }
  }

  @Nested
  @DisplayName("Deserialize Tests")
  class DeserializeTests {

    @Test
    @DisplayName("should throw on null engine")
    void shouldThrowOnNullEngine() {
      final JniSerializer serializer = new JniSerializer(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> serializer.deserialize(null, new byte[] {0x00}),
          "Should throw on null engine");
    }

    @Test
    @DisplayName("should throw on null serialized bytes")
    void shouldThrowOnNullSerializedBytes() {
      final JniSerializer serializer = new JniSerializer(VALID_HANDLE);

      assertThrows(
          IllegalArgumentException.class,
          () -> serializer.deserialize(testEngine, null),
          "Should throw on null serialized bytes");
    }

    // Note: Test for closed serializer deserialize is covered by integration tests
    // because calling close() on fake handles triggers native destructor which crashes the JVM
  }

  // Note: ClearCache, GetCacheEntryCount, GetCacheTotalSize, GetCacheHitRate closed tests
  // are covered by integration tests because calling close() on fake handles crashes the JVM

  @Nested
  @DisplayName("Multiple Instance Tests")
  class MultipleInstanceTests {

    @Test
    @DisplayName("should create independent serializers")
    void shouldCreateIndependentSerializers() {
      final long handle1 = 0xAAAAAAAAL;
      final long handle2 = 0xBBBBBBBBL;

      final JniSerializer serializer1 = new JniSerializer(handle1);
      final JniSerializer serializer2 = new JniSerializer(handle2);

      assertNotEquals(
          serializer1.getNativeHandle(),
          serializer2.getNativeHandle(),
          "Serializers should have different handles");

      // Note: We don't call close() on fake handles as it triggers native destructor
      // which crashes the JVM. Close behavior is tested in integration tests.
    }
  }
}
