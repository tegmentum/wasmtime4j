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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmFunction} default methods.
 *
 * <p>These tests use anonymous implementations to exercise default method behavior without
 * requiring a native runtime.
 */
@DisplayName("WasmFunction Default Method Tests")
class WasmFunctionTest {

  private WasmFunction createAddFunction() {
    return new WasmFunction() {
      @Override
      public WasmValue[] call(final WasmValue... params) throws WasmException {
        int a = params[0].asInt();
        int b = params[1].asInt();
        return new WasmValue[] {WasmValue.i32(a + b)};
      }

      @Override
      public FunctionType getFunctionType() {
        return new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});
      }

      @Override
      public String getName() {
        return "add";
      }

      @Override
      public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
        return CompletableFuture.supplyAsync(
            () -> {
              try {
                return call(params);
              } catch (WasmException e) {
                throw new java.util.concurrent.CompletionException(e);
              }
            });
      }

      @Override
      public long toRawFuncRef() {
        return 0;
      }
    };
  }

  private WasmFunction createVoidFunction() {
    return new WasmFunction() {
      @Override
      public WasmValue[] call(final WasmValue... params) throws WasmException {
        return new WasmValue[0];
      }

      @Override
      public FunctionType getFunctionType() {
        return new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      }

      @Override
      public String getName() {
        return "noop";
      }

      @Override
      public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
        return CompletableFuture.completedFuture(new WasmValue[0]);
      }

      @Override
      public long toRawFuncRef() {
        return 0;
      }
    };
  }

  @Nested
  @DisplayName("matchesType() Default Method")
  class MatchesTypeTests {

    @Test
    @DisplayName("should return true for matching signature")
    void shouldReturnTrueForMatchingSignature() {
      final WasmFunction func = createAddFunction();
      assertTrue(
          func.matchesType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32}));
    }

    @Test
    @DisplayName("should return false for mismatched param count")
    void shouldReturnFalseForMismatchedParamCount() {
      final WasmFunction func = createAddFunction();
      assertFalse(
          func.matchesType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32}));
    }

    @Test
    @DisplayName("should return false for mismatched param types")
    void shouldReturnFalseForMismatchedParamTypes() {
      final WasmFunction func = createAddFunction();
      assertFalse(
          func.matchesType(
              new WasmValueType[] {WasmValueType.I64, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32}));
    }

    @Test
    @DisplayName("should return false for mismatched result count")
    void shouldReturnFalseForMismatchedResultCount() {
      final WasmFunction func = createAddFunction();
      assertFalse(
          func.matchesType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32}, new WasmValueType[] {}));
    }

    @Test
    @DisplayName("should return false for mismatched result types")
    void shouldReturnFalseForMismatchedResultTypes() {
      final WasmFunction func = createAddFunction();
      assertFalse(
          func.matchesType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I64}));
    }

    @Test
    @DisplayName("should return true for void function with empty arrays")
    void shouldReturnTrueForVoidFunction() {
      final WasmFunction func = createVoidFunction();
      assertTrue(func.matchesType(new WasmValueType[] {}, new WasmValueType[] {}));
    }
  }

  @Nested
  @DisplayName("matchesFuncType() Default Method")
  class MatchesFuncTypeTests {

    @Test
    @DisplayName("should return false for null funcType")
    void shouldReturnFalseForNullFuncType() throws WasmException {
      final WasmFunction func = createAddFunction();
      assertFalse(func.matchesFuncType(null, null));
    }

    @Test
    @DisplayName("should return true for matching FunctionType")
    void shouldReturnTrueForMatchingFunctionType() throws WasmException {
      final WasmFunction func = createAddFunction();
      final FunctionType matchingType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});
      assertTrue(func.matchesFuncType(null, matchingType));
    }
  }

  @Nested
  @DisplayName("callSingleAsync() Default Method")
  class CallSingleAsyncTests {

    @Test
    @DisplayName("should return single result")
    void shouldReturnSingleResult() throws ExecutionException, InterruptedException {
      final WasmFunction func = createAddFunction();
      final WasmValue result = func.callSingleAsync(WasmValue.i32(3), WasmValue.i32(7)).get();
      assertEquals(10, result.asInt());
    }

    @Test
    @DisplayName("should fail for void function")
    void shouldFailForVoidFunction() {
      final WasmFunction func = createVoidFunction();
      final CompletableFuture<WasmValue> future = func.callSingleAsync();
      assertThrows(ExecutionException.class, future::get);
    }

    @Test
    @DisplayName("should fail for multi-value function")
    void shouldFailForMultiValueFunction() {
      final WasmFunction func =
          new WasmFunction() {
            @Override
            public WasmValue[] call(final WasmValue... params) {
              return new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)};
            }

            @Override
            public FunctionType getFunctionType() {
              return new FunctionType(
                  new WasmValueType[0], new WasmValueType[] {WasmValueType.I32, WasmValueType.I32});
            }

            @Override
            public String getName() {
              return "multi";
            }

            @Override
            public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
              return CompletableFuture.completedFuture(call(params));
            }

            @Override
            public long toRawFuncRef() {
              return 0;
            }
          };
      final CompletableFuture<WasmValue> future = func.callSingleAsync();
      assertThrows(ExecutionException.class, future::get);
    }
  }

  @Nested
  @DisplayName("Typed Fast-Path Default Methods")
  class TypedFastPathTests {

    @Test
    @DisplayName("callVoid() should succeed for void function")
    void callVoidShouldSucceedForVoidFunction() throws WasmException {
      createVoidFunction().callVoid();
    }

    @Test
    @DisplayName("callVoid() should throw for non-void function")
    void callVoidShouldThrowForNonVoidFunction() {
      final WasmFunction func =
          new WasmFunction() {
            @Override
            public WasmValue[] call(final WasmValue... params) {
              return new WasmValue[] {WasmValue.i32(1)};
            }

            @Override
            public FunctionType getFunctionType() {
              return new FunctionType(
                  new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
            }

            @Override
            public String getName() {
              return "ret1";
            }

            @Override
            public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
              return CompletableFuture.completedFuture(call(params));
            }

            @Override
            public long toRawFuncRef() {
              return 0;
            }
          };
      assertThrows(WasmException.class, func::callVoid);
    }

    @Test
    @DisplayName("callI32ToI32() should work correctly")
    void callI32ToI32ShouldWorkCorrectly() throws WasmException {
      final WasmFunction func =
          new WasmFunction() {
            @Override
            public WasmValue[] call(final WasmValue... params) {
              return new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)};
            }

            @Override
            public FunctionType getFunctionType() {
              return new FunctionType(
                  new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
            }

            @Override
            public String getName() {
              return "double";
            }

            @Override
            public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
              return CompletableFuture.completedFuture(call(params));
            }

            @Override
            public long toRawFuncRef() {
              return 0;
            }
          };
      assertEquals(42, func.callI32ToI32(21));
    }

    @Test
    @DisplayName("callI32I32ToI32() should work correctly")
    void callI32I32ToI32ShouldWorkCorrectly() throws WasmException {
      final WasmFunction func = createAddFunction();
      assertEquals(30, func.callI32I32ToI32(10, 20));
    }

    @Test
    @DisplayName("callI64ToI64() should work correctly")
    void callI64ToI64ShouldWorkCorrectly() throws WasmException {
      final WasmFunction func =
          new WasmFunction() {
            @Override
            public WasmValue[] call(final WasmValue... params) {
              return new WasmValue[] {WasmValue.i64(params[0].asLong() + 1)};
            }

            @Override
            public FunctionType getFunctionType() {
              return new FunctionType(
                  new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {WasmValueType.I64});
            }

            @Override
            public String getName() {
              return "inc64";
            }

            @Override
            public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
              return CompletableFuture.completedFuture(call(params));
            }

            @Override
            public long toRawFuncRef() {
              return 0;
            }
          };
      assertEquals(101L, func.callI64ToI64(100L));
    }

    @Test
    @DisplayName("callF64ToF64() should work correctly")
    void callF64ToF64ShouldWorkCorrectly() throws WasmException {
      final WasmFunction func =
          new WasmFunction() {
            @Override
            public WasmValue[] call(final WasmValue... params) {
              return new WasmValue[] {WasmValue.f64(params[0].asDouble() * 2.0)};
            }

            @Override
            public FunctionType getFunctionType() {
              return new FunctionType(
                  new WasmValueType[] {WasmValueType.F64}, new WasmValueType[] {WasmValueType.F64});
            }

            @Override
            public String getName() {
              return "double64";
            }

            @Override
            public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
              return CompletableFuture.completedFuture(call(params));
            }

            @Override
            public long toRawFuncRef() {
              return 0;
            }
          };
      assertEquals(6.28, func.callF64ToF64(3.14), 0.001);
    }

    @Test
    @DisplayName("callToI32() should work for no-param i32-returning function")
    void callToI32ShouldWork() throws WasmException {
      final WasmFunction func =
          new WasmFunction() {
            @Override
            public WasmValue[] call(final WasmValue... params) {
              return new WasmValue[] {WasmValue.i32(42)};
            }

            @Override
            public FunctionType getFunctionType() {
              return new FunctionType(
                  new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
            }

            @Override
            public String getName() {
              return "answer";
            }

            @Override
            public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
              return CompletableFuture.completedFuture(call(params));
            }

            @Override
            public long toRawFuncRef() {
              return 0;
            }
          };
      assertEquals(42, func.callToI32());
    }
  }

  @Nested
  @DisplayName("getNativeHandle() Default Method")
  class NativeHandleTests {

    @Test
    @DisplayName("should return 0 by default")
    void shouldReturnZeroByDefault() {
      assertEquals(0L, createAddFunction().getNativeHandle());
    }
  }

  @Nested
  @DisplayName("fromRawFuncRef() Static Method")
  class FromRawFuncRefTests {

    @Test
    @DisplayName("should throw for null store")
    void shouldThrowForNullStore() {
      assertThrows(IllegalArgumentException.class, () -> WasmFunction.fromRawFuncRef(null, 0L));
    }
  }
}
