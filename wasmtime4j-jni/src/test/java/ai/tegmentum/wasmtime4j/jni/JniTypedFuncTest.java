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

import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniTypedFunc}.
 *
 * <p>Tests focus on constructor parameter validation. These tests verify the createHandle
 * validation without requiring actual native library loading.
 *
 * <p>Note: Integration tests with actual typed function calls are in wasmtime4j-tests.
 */
@DisplayName("JniTypedFunc Tests")
class JniTypedFuncTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniStore testStore;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
  }

  @AfterEach
  void tearDown() {
    testStore.markClosedForTesting();
    testEngine.markClosedForTesting();
  }

  @Nested
  @DisplayName("CreateHandle Validation Tests")
  class CreateHandleValidationTests {

    @Test
    @DisplayName("should throw on null store")
    void shouldThrowOnNullStore() {
      final JniFunction func = createMockJniFunction();

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(null, func, "ii->i"),
          "Should throw on null store");
    }

    @Test
    @DisplayName("should throw on null function")
    void shouldThrowOnNullFunction() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(testStore, null, "ii->i"),
          "Should throw on null function");
    }

    @Test
    @DisplayName("should throw on null signature")
    void shouldThrowOnNullSignature() {
      final JniFunction func = createMockJniFunction();

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(testStore, func, null),
          "Should throw on null signature");
    }

    @Test
    @DisplayName("should throw on empty signature")
    void shouldThrowOnEmptySignature() {
      final JniFunction func = createMockJniFunction();

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(testStore, func, ""),
          "Should throw on empty signature");
    }

    @Test
    @DisplayName("should throw on non-JniFunction")
    void shouldThrowOnNonJniFunction() {
      final WasmFunction nonJniFunc =
          new WasmFunction() {
            @Override
            public FunctionType getFunctionType() {
              return null;
            }

            @Override
            public String getName() {
              return "mock";
            }

            @Override
            public WasmValue[] call(final WasmValue... params) {
              return new WasmValue[0];
            }

            @Override
            public java.util.concurrent.CompletableFuture<WasmValue[]> callAsync(
                final WasmValue... params) {
              return java.util.concurrent.CompletableFuture.completedFuture(new WasmValue[0]);
            }

            @Override
            public long toRawFuncRef() {
              return 0L;
            }
          };

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(testStore, nonJniFunc, "ii->i"),
          "Should throw on non-JniFunction");
    }

    private JniFunction createMockJniFunction() {
      return new JniFunction(VALID_HANDLE, "mockFunc", VALID_HANDLE, testStore);
    }
  }
}
