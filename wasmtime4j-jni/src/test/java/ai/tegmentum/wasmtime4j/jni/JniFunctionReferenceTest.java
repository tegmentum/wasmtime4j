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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JniFunctionReference defensive programming and validation logic.
 *
 * <p>Note: JniFunctionReference constructors call native code immediately, so most tests require
 * native library loading and are in integration tests. These unit tests verify parameter validation
 * and static methods only.
 */
class JniFunctionReferenceTest {
  private static final long VALID_HANDLE = 1L;
  private JniEngine testEngine;
  private JniStore testStore;
  private FunctionType testFunctionType;
  private HostFunction testHostFunction;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
    testFunctionType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
    testHostFunction = params -> null;
  }

  // Constructor tests - host function variant

  @Test
  void testConstructorFromHostFunctionWithNullHostFunction() {
    final WasmException exception =
        assertThrows(
            WasmException.class, () -> new JniFunctionReference(null, testFunctionType, testStore));

    assertThat(exception.getMessage()).contains("Failed to create native function reference");
  }

  @Test
  void testConstructorFromHostFunctionWithNullFunctionType() {
    final WasmException exception =
        assertThrows(
            WasmException.class, () -> new JniFunctionReference(testHostFunction, null, testStore));

    assertThat(exception.getMessage()).contains("Failed to create native function reference");
  }

  @Test
  void testConstructorFromHostFunctionWithNullStore() {
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> new JniFunctionReference(testHostFunction, testFunctionType, null));

    assertThat(exception.getMessage()).contains("Failed to create native function reference");
  }

  // Constructor tests - WASM function variant

  @Test
  void testConstructorFromWasmFunctionWithNullFunction() {
    final WasmException exception =
        assertThrows(WasmException.class, () -> new JniFunctionReference(null, testStore));

    assertThat(exception.getMessage()).contains("Failed to create native function reference");
  }

  @Test
  void testConstructorFromWasmFunctionWithNullStore() {
    final WasmFunction wasmFunc = createMockWasmFunction("test");

    final WasmException exception =
        assertThrows(WasmException.class, () -> new JniFunctionReference(wasmFunc, null));

    assertThat(exception.getMessage()).contains("Failed to create native function reference");
  }

  @Test
  void testConstructorFromWasmFunctionWithNonJniFunction() {
    // Create a WasmFunction that is NOT a JniFunction
    final WasmFunction nonJniFunc = createMockWasmFunction("test");

    final WasmException exception =
        assertThrows(WasmException.class, () -> new JniFunctionReference(nonJniFunc, testStore));

    assertThat(exception.getMessage()).contains("Failed to create native function reference");
  }

  // Registry tests - these are static and don't require instances

  @Test
  void testRegistryStatsNotNull() {
    final long[] stats = JniFunctionReference.getRegistryStats();

    assertThat(stats).isNotNull();
    assertThat(stats).hasSize(2);
    assertThat(stats[0]).isGreaterThanOrEqualTo(0); // registry size
    assertThat(stats[1]).isGreaterThan(0); // next ID
  }

  @Test
  void testGetFromRegistryWithInvalidId() {
    final JniFunctionReference result = JniFunctionReference.getFromRegistry(-1L);

    assertThat(result).isNull();
  }

  @Test
  void testGetFromRegistryWithNonExistentId() {
    final JniFunctionReference result = JniFunctionReference.getFromRegistry(999999999L);

    assertThat(result).isNull();
  }

  // Helper method to create mock WasmFunction

  private WasmFunction createMockWasmFunction(final String name) {
    return new WasmFunction() {
      @Override
      public ai.tegmentum.wasmtime4j.WasmValue[] call(
          final ai.tegmentum.wasmtime4j.WasmValue... params) throws WasmException {
        return new ai.tegmentum.wasmtime4j.WasmValue[0];
      }

      @Override
      public FunctionType getFunctionType() {
        return testFunctionType;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public java.util.concurrent.CompletableFuture<ai.tegmentum.wasmtime4j.WasmValue[]> callAsync(
          final ai.tegmentum.wasmtime4j.WasmValue... params) {
        return java.util.concurrent.CompletableFuture.supplyAsync(
            () -> {
              try {
                return call(params);
              } catch (final WasmException e) {
                throw new RuntimeException(e);
              }
            });
      }

      @Override
      public long toRawFuncRef() {
        return 0L;
      }
    };
  }
}
