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

import ai.tegmentum.wasmtime4j.component.ComponentFunc;
import ai.tegmentum.wasmtime4j.component.ComponentTypedFunc;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * JNI implementation of ComponentTypedFunc for type-safe WebAssembly Component Model function
 * calls.
 *
 * <p>This class provides zero-cost typed function calls by caching type information and providing
 * direct primitive type access for common signatures.
 *
 * @since 1.0.0
 */
public final class JniComponentTypedFunc implements ComponentTypedFunc {

  private static final Logger LOGGER = Logger.getLogger(JniComponentTypedFunc.class.getName());

  private final JniComponentFunc function;
  private final String signature;
  private volatile boolean closed = false;
  private final ReentrantReadWriteLock closeLock = new ReentrantReadWriteLock();

  /**
   * Creates a new typed component function wrapper.
   *
   * @param function the underlying component function
   * @param signature the type signature (e.g., "s32,s32->s32")
   */
  public JniComponentTypedFunc(final JniComponentFunc function, final String signature) {
    Validation.requireNonNull(function, "function");
    Validation.requireNonEmpty(signature, "signature");
    this.function = function;
    this.signature = signature;
    LOGGER.fine("Created JniComponentTypedFunc with signature: " + signature);
  }

  @Override
  public void callVoidToVoid() throws WasmException {
    beginOperation();
    try {
      function.call();
    } finally {
      endOperation();
    }
  }

  @Override
  public void callS32ToVoid(final int param) throws WasmException {
    beginOperation();
    try {
      function.call(ComponentVal.s32(param));
    } finally {
      endOperation();
    }
  }

  @Override
  public void callS32S32ToVoid(final int param1, final int param2) throws WasmException {
    beginOperation();
    try {
      function.call(ComponentVal.s32(param1), ComponentVal.s32(param2));
    } finally {
      endOperation();
    }
  }

  @Override
  public void callS64ToVoid(final long param) throws WasmException {
    beginOperation();
    try {
      function.call(ComponentVal.s64(param));
    } finally {
      endOperation();
    }
  }

  @Override
  public void callS64S64ToVoid(final long param1, final long param2) throws WasmException {
    beginOperation();
    try {
      function.call(ComponentVal.s64(param1), ComponentVal.s64(param2));
    } finally {
      endOperation();
    }
  }

  @Override
  public void callStringToVoid(final String param) throws WasmException {
    beginOperation();
    try {
      function.call(ComponentVal.string(param));
    } finally {
      endOperation();
    }
  }

  @Override
  public int callS32ToS32(final int param) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result = function.call(ComponentVal.s32(param));
      return extractS32Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public int callS32S32ToS32(final int param1, final int param2) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(ComponentVal.s32(param1), ComponentVal.s32(param2));
      return extractS32Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public int callS32S32S32ToS32(final int param1, final int param2, final int param3)
      throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(
              ComponentVal.s32(param1), ComponentVal.s32(param2), ComponentVal.s32(param3));
      return extractS32Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public int callS64ToS32(final long param) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result = function.call(ComponentVal.s64(param));
      return extractS32Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public long callS64ToS64(final long param) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result = function.call(ComponentVal.s64(param));
      return extractS64Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public long callS64S64ToS64(final long param1, final long param2) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(ComponentVal.s64(param1), ComponentVal.s64(param2));
      return extractS64Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public long callS64S64S64ToS64(final long param1, final long param2, final long param3)
      throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(
              ComponentVal.s64(param1), ComponentVal.s64(param2), ComponentVal.s64(param3));
      return extractS64Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public long callS32S32ToS64(final int param1, final int param2) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(ComponentVal.s32(param1), ComponentVal.s32(param2));
      return extractS64Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public float callF32ToF32(final float param) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result = function.call(ComponentVal.f32(param));
      return extractF32Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public float callF32F32ToF32(final float param1, final float param2) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(ComponentVal.f32(param1), ComponentVal.f32(param2));
      return extractF32Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public float callF32F32F32ToF32(final float param1, final float param2, final float param3)
      throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(
              ComponentVal.f32(param1), ComponentVal.f32(param2), ComponentVal.f32(param3));
      return extractF32Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public double callF64ToF64(final double param) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result = function.call(ComponentVal.f64(param));
      return extractF64Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public double callF64F64ToF64(final double param1, final double param2) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(ComponentVal.f64(param1), ComponentVal.f64(param2));
      return extractF64Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public double callF64F64F64ToF64(final double param1, final double param2, final double param3)
      throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(
              ComponentVal.f64(param1), ComponentVal.f64(param2), ComponentVal.f64(param3));
      return extractF64Result(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public String callVoidToString() throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result = function.call();
      return extractStringResult(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public String callStringToString(final String param) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result = function.call(ComponentVal.string(param));
      return extractStringResult(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public String callStringStringToString(final String param1, final String param2)
      throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result =
          function.call(ComponentVal.string(param1), ComponentVal.string(param2));
      return extractStringResult(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean callVoidToBool() throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result = function.call();
      return extractBoolResult(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean callBoolToBool(final boolean param) throws WasmException {
    beginOperation();
    try {
      final List<ComponentVal> result = function.call(ComponentVal.bool(param));
      return extractBoolResult(result);
    } finally {
      endOperation();
    }
  }

  @Override
  public String getSignature() {
    return signature;
  }

  @Override
  public ComponentFunc getFunction() {
    return function;
  }

  @Override
  public void close() {
    closeLock.writeLock().lock();
    try {
      if (!closed) {
        closed = true;
        LOGGER.fine("Closed JniComponentTypedFunc with signature: " + signature);
      }
    } finally {
      closeLock.writeLock().unlock();
    }
  }

  private void beginOperation() {
    closeLock.readLock().lock();
    if (closed) {
      closeLock.readLock().unlock();
      throw new IllegalStateException("ComponentTypedFunc has been closed");
    }
  }

  private void endOperation() {
    closeLock.readLock().unlock();
  }

  /**
   * Extracts an s32 result from the function call result.
   *
   * @param result the function call result
   * @return the s32 value
   * @throws WasmException if result is empty or wrong type
   */
  private int extractS32Result(final List<ComponentVal> result) throws WasmException {
    if (result.isEmpty()) {
      throw new WasmException("Expected s32 result but got void");
    }
    final ComponentVal val = result.get(0);
    if (!val.isS32()) {
      throw new WasmException("Expected s32 result but got " + val.getType());
    }
    return val.asS32();
  }

  /**
   * Extracts an s64 result from the function call result.
   *
   * @param result the function call result
   * @return the s64 value
   * @throws WasmException if result is empty or wrong type
   */
  private long extractS64Result(final List<ComponentVal> result) throws WasmException {
    if (result.isEmpty()) {
      throw new WasmException("Expected s64 result but got void");
    }
    final ComponentVal val = result.get(0);
    if (!val.isS64()) {
      throw new WasmException("Expected s64 result but got " + val.getType());
    }
    return val.asS64();
  }

  /**
   * Extracts an f32 result from the function call result.
   *
   * @param result the function call result
   * @return the f32 value
   * @throws WasmException if result is empty or wrong type
   */
  private float extractF32Result(final List<ComponentVal> result) throws WasmException {
    if (result.isEmpty()) {
      throw new WasmException("Expected f32 result but got void");
    }
    final ComponentVal val = result.get(0);
    if (!val.isF32()) {
      throw new WasmException("Expected f32 result but got " + val.getType());
    }
    return val.asF32();
  }

  /**
   * Extracts an f64 result from the function call result.
   *
   * @param result the function call result
   * @return the f64 value
   * @throws WasmException if result is empty or wrong type
   */
  private double extractF64Result(final List<ComponentVal> result) throws WasmException {
    if (result.isEmpty()) {
      throw new WasmException("Expected f64 result but got void");
    }
    final ComponentVal val = result.get(0);
    if (!val.isF64()) {
      throw new WasmException("Expected f64 result but got " + val.getType());
    }
    return val.asF64();
  }

  /**
   * Extracts a string result from the function call result.
   *
   * @param result the function call result
   * @return the string value
   * @throws WasmException if result is empty or wrong type
   */
  private String extractStringResult(final List<ComponentVal> result) throws WasmException {
    if (result.isEmpty()) {
      throw new WasmException("Expected string result but got void");
    }
    final ComponentVal val = result.get(0);
    if (!val.isString()) {
      throw new WasmException("Expected string result but got " + val.getType());
    }
    return val.asString();
  }

  /**
   * Extracts a bool result from the function call result.
   *
   * @param result the function call result
   * @return the boolean value
   * @throws WasmException if result is empty or wrong type
   */
  private boolean extractBoolResult(final List<ComponentVal> result) throws WasmException {
    if (result.isEmpty()) {
      throw new WasmException("Expected bool result but got void");
    }
    final ComponentVal val = result.get(0);
    if (!val.isBool()) {
      throw new WasmException("Expected bool result but got " + val.getType());
    }
    return val.asBool();
  }
}
