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

package ai.tegmentum.wasmtime4j.panama.adapter;

import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Adapter class that wraps a WasmFunction instance to implement the Function interface.
 *
 * <p>This adapter bridges the gap between the WasmFunction interface and the Function interface
 * used by the Caller interface. It handles conversion between WasmValue and Object types.
 *
 * @param <T> the type of user data associated with the store (not used but required by interface)
 * @since 1.0.0
 */
public final class WasmFunctionToFunctionAdapter<T> implements Function<T> {

  /** Default timeout for async operations in milliseconds (30 seconds). */
  private static final long DEFAULT_TIMEOUT_MS = 30_000L;

  /** Shared executor service for async function calls. */
  private static final ExecutorService ASYNC_EXECUTOR =
      Executors.newCachedThreadPool(
          r -> {
            Thread t = new Thread(r, "wasmtime4j-async-function");
            t.setDaemon(true);
            return t;
          });

  private final WasmFunction delegate;
  private final String name;

  /**
   * Creates a new adapter wrapping the given WasmFunction instance.
   *
   * @param delegate the WasmFunction instance to wrap
   * @throws IllegalArgumentException if delegate is null
   */
  public WasmFunctionToFunctionAdapter(final WasmFunction delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("WasmFunction delegate cannot be null");
    }
    this.delegate = delegate;
    this.name = delegate.getName();
  }

  @Override
  public Object[] call(final Object... args) throws WasmException {
    try {
      // Convert Object[] to WasmValue[]
      final WasmValue[] wasmArgs = convertToWasmValues(args);

      // Call the delegate
      final WasmValue[] results = delegate.call(wasmArgs);

      // Convert WasmValue[] back to Object[]
      return convertFromWasmValues(results);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Function call failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Object callSingle(final Object... args) throws WasmException {
    final Object[] results = call(args);
    if (results == null || results.length == 0) {
      return null;
    }
    if (results.length > 1) {
      throw new WasmException("Function returns multiple values, use call() instead");
    }
    return results[0];
  }

  @Override
  public FunctionSignature getSignature() {
    final FunctionType funcType = delegate.getFunctionType();
    if (funcType == null) {
      return null;
    }

    return new FunctionSignature() {
      @Override
      public List<ValueType> getParameterTypes() {
        return convertWasmTypesArray(funcType.getParamTypes());
      }

      @Override
      public List<ValueType> getReturnTypes() {
        return convertWasmTypesArray(funcType.getReturnTypes());
      }

      @Override
      public boolean matches(final FunctionSignature other) {
        if (other == null) {
          return false;
        }
        return getParameterTypes().equals(other.getParameterTypes())
            && getReturnTypes().equals(other.getReturnTypes());
      }
    };
  }

  @Override
  public List<ValueType> getParameterTypes() {
    final FunctionType funcType = delegate.getFunctionType();
    if (funcType == null) {
      return Collections.emptyList();
    }
    return convertWasmTypesArray(funcType.getParamTypes());
  }

  @Override
  public List<ValueType> getReturnTypes() {
    final FunctionType funcType = delegate.getFunctionType();
    if (funcType == null) {
      return Collections.emptyList();
    }
    return convertWasmTypesArray(funcType.getReturnTypes());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isValid() {
    try {
      delegate.getFunctionType();
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  @Override
  public int getParameterCount() {
    final FunctionType funcType = delegate.getFunctionType();
    if (funcType == null) {
      return 0;
    }
    return funcType.getParamCount();
  }

  @Override
  public int getReturnCount() {
    final FunctionType funcType = delegate.getFunctionType();
    if (funcType == null) {
      return 0;
    }
    return funcType.getReturnCount();
  }

  @Override
  public CompletableFuture<Object[]> callAsync(final Object... args) {
    return callAsync(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS, args);
  }

  @Override
  public CompletableFuture<Object[]> callAsync(
      final long timeout, final TimeUnit unit, final Object... args) {
    final CompletableFuture<Object[]> future = new CompletableFuture<>();

    ASYNC_EXECUTOR.submit(
        () -> {
          try {
            final Object[] result = call(args);
            future.complete(result);
          } catch (final Exception e) {
            future.completeExceptionally(e);
          }
        });

    // Apply timeout if specified
    if (timeout > 0 && unit != null) {
      return future.orTimeout(timeout, unit);
    }

    return future;
  }

  @Override
  public CompletableFuture<Object> callSingleAsync(final Object... args) {
    return callAsync(args)
        .thenApply(
            results -> {
              if (results == null || results.length == 0) {
                return null;
              }
              if (results.length > 1) {
                throw new RuntimeException(
                    new WasmException("Function returns multiple values, use callAsync() instead"));
              }
              return results[0];
            });
  }

  @Override
  public CompletableFuture<Object> callSingleAsync(
      final long timeout, final TimeUnit unit, final Object... args) {
    return callAsync(timeout, unit, args)
        .thenApply(
            results -> {
              if (results == null || results.length == 0) {
                return null;
              }
              if (results.length > 1) {
                throw new RuntimeException(
                    new WasmException("Function returns multiple values, use callAsync() instead"));
              }
              return results[0];
            });
  }

  /**
   * Converts Object arguments to WasmValue arguments.
   *
   * @param args the Object arguments
   * @return the WasmValue arguments
   */
  private WasmValue[] convertToWasmValues(final Object[] args) {
    if (args == null || args.length == 0) {
      return new WasmValue[0];
    }

    final FunctionType funcType = delegate.getFunctionType();
    final WasmValueType[] paramTypes = funcType != null ? funcType.getParamTypes() : null;

    final WasmValue[] wasmArgs = new WasmValue[args.length];
    for (int i = 0; i < args.length; i++) {
      final Object arg = args[i];
      if (arg instanceof WasmValue) {
        wasmArgs[i] = (WasmValue) arg;
      } else {
        // Infer type from parameter types if available, otherwise from value
        final WasmValueType expectedType =
            (paramTypes != null && i < paramTypes.length) ? paramTypes[i] : null;
        wasmArgs[i] = convertToWasmValue(arg, expectedType);
      }
    }
    return wasmArgs;
  }

  /**
   * Converts a single Object to WasmValue.
   *
   * @param value the Object value
   * @param expectedType the expected WasmValueType, or null to infer
   * @return the WasmValue
   */
  private WasmValue convertToWasmValue(final Object value, final WasmValueType expectedType) {
    if (value == null) {
      return WasmValue.externref(null);
    }

    if (expectedType != null) {
      switch (expectedType) {
        case I32:
          if (value instanceof Number) {
            return WasmValue.i32(((Number) value).intValue());
          }
          break;
        case I64:
          if (value instanceof Number) {
            return WasmValue.i64(((Number) value).longValue());
          }
          break;
        case F32:
          if (value instanceof Number) {
            return WasmValue.f32(((Number) value).floatValue());
          }
          break;
        case F64:
          if (value instanceof Number) {
            return WasmValue.f64(((Number) value).doubleValue());
          }
          break;
        case FUNCREF:
          return WasmValue.funcref(value);
        case EXTERNREF:
          return WasmValue.externref(value);
        default:
          break;
      }
    }

    // Infer from value type
    if (value instanceof Integer) {
      return WasmValue.i32((Integer) value);
    } else if (value instanceof Long) {
      return WasmValue.i64((Long) value);
    } else if (value instanceof Float) {
      return WasmValue.f32((Float) value);
    } else if (value instanceof Double) {
      return WasmValue.f64((Double) value);
    } else {
      return WasmValue.externref(value);
    }
  }

  /**
   * Converts WasmValue results to Object results.
   *
   * @param results the WasmValue results
   * @return the Object results
   */
  private Object[] convertFromWasmValues(final WasmValue[] results) {
    if (results == null || results.length == 0) {
      return new Object[0];
    }

    final Object[] objects = new Object[results.length];
    for (int i = 0; i < results.length; i++) {
      objects[i] = results[i] != null ? results[i].getValue() : null;
    }
    return objects;
  }

  /**
   * Converts WasmValueType array to ValueType list.
   *
   * @param wasmTypes the WasmValueType array
   * @return the ValueType list
   */
  private List<ValueType> convertWasmTypesArray(final WasmValueType[] wasmTypes) {
    if (wasmTypes == null || wasmTypes.length == 0) {
      return Collections.emptyList();
    }

    final List<ValueType> types = new ArrayList<>(wasmTypes.length);
    for (final WasmValueType wasmType : wasmTypes) {
      types.add(convertWasmType(wasmType));
    }
    return types;
  }

  /**
   * Converts a single WasmValueType to ValueType.
   *
   * @param wasmType the WasmValueType
   * @return the ValueType
   */
  private ValueType convertWasmType(final WasmValueType wasmType) {
    if (wasmType == null) {
      return ValueType.EXTERNREF;
    }

    switch (wasmType) {
      case I32:
        return ValueType.I32;
      case I64:
        return ValueType.I64;
      case F32:
        return ValueType.F32;
      case F64:
        return ValueType.F64;
      case V128:
        return ValueType.V128;
      case FUNCREF:
        return ValueType.FUNCREF;
      case EXTERNREF:
      default:
        return ValueType.EXTERNREF;
    }
  }

  /**
   * Gets the underlying WasmFunction delegate.
   *
   * @return the delegate WasmFunction instance
   */
  public WasmFunction getDelegate() {
    return delegate;
  }
}
