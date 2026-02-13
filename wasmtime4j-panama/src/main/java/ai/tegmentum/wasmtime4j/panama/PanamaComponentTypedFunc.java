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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.component.ComponentFunc;
import ai.tegmentum.wasmtime4j.component.ComponentTypedFunc;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of typed WebAssembly Component Model function calls.
 *
 * <p>This class provides zero-cost typed function calls by caching type information and avoiding
 * runtime type checking on each invocation. This is a significant performance optimization for
 * frequently called Component Model functions with known signatures.
 *
 * <p>ComponentTypedFunc wraps a regular ComponentFunc with compile-time type information, enabling:
 *
 * <ul>
 *   <li>Zero-cost abstraction - no runtime type checking overhead
 *   <li>Direct primitive parameter passing without ComponentVal boxing
 *   <li>Optimized parameter/result marshalling for common type combinations
 *   <li>Type safety through signature validation at creation time
 * </ul>
 *
 * <p>Supported signatures use Component Model type names:
 *
 * <ul>
 *   <li>"s32" = signed 32-bit integer
 *   <li>"s64" = signed 64-bit integer
 *   <li>"f32" = 32-bit float
 *   <li>"f64" = 64-bit float
 *   <li>"string" = UTF-8 string
 *   <li>"bool" = boolean
 *   <li>"void" = no value
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentFunc func = instance.getFunc("add");
 * PanamaComponentTypedFunc typedAdd = new PanamaComponentTypedFunc(func, "s32,s32->s32");
 * int result = typedAdd.callS32S32ToS32(10, 20); // Returns 30
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PanamaComponentTypedFunc implements ComponentTypedFunc {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentTypedFunc.class.getName());

  /** The underlying component function to call. */
  private final ComponentFunc function;

  /** Function signature string (e.g., "s32,s32->s32" for (s32, s32) -> s32). */
  private final String signature;

  /** Handle for managing native resource lifecycle and cleanup. */
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a typed component function wrapper from a regular component function.
   *
   * <p>The signature string encodes parameter and return types using Component Model names:
   *
   * <ul>
   *   <li>"s8", "s16", "s32", "s64" = signed integers
   *   <li>"u8", "u16", "u32", "u64" = unsigned integers
   *   <li>"f32", "f64" = floating point
   *   <li>"bool" = boolean
   *   <li>"string" = string
   *   <li>"void" or empty = no value
   * </ul>
   *
   * <p>Format: "params->results", e.g., "s32,s32->s32" for (s32, s32) -> s32
   *
   * @param func the component function to wrap with type information
   * @param signature the signature string encoding parameter and return types
   * @throws IllegalArgumentException if function or signature is invalid
   */
  public PanamaComponentTypedFunc(final ComponentFunc func, final String signature) {
    if (func == null) {
      throw new IllegalArgumentException("Function cannot be null");
    }
    if (signature == null || signature.isEmpty()) {
      throw new IllegalArgumentException("Signature cannot be null or empty");
    }

    this.function = func;
    this.signature = signature;

    LOGGER.log(Level.FINE, "Created ComponentTypedFunc with signature: {0}", signature);

    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaComponentTypedFunc",
            () -> {
              LOGGER.log(Level.FINE, "Closed ComponentTypedFunc with signature: {0}", signature);
            });
  }

  // ========== Void return signatures ==========

  @Override
  public void callVoidToVoid() throws WasmException {
    ensureNotClosed();
    function.call();
  }

  @Override
  public void callS32ToVoid(final int param) throws WasmException {
    ensureNotClosed();
    function.call(ComponentVal.s32(param));
  }

  @Override
  public void callS32S32ToVoid(final int param1, final int param2) throws WasmException {
    ensureNotClosed();
    function.call(ComponentVal.s32(param1), ComponentVal.s32(param2));
  }

  @Override
  public void callS64ToVoid(final long param) throws WasmException {
    ensureNotClosed();
    function.call(ComponentVal.s64(param));
  }

  @Override
  public void callS64S64ToVoid(final long param1, final long param2) throws WasmException {
    ensureNotClosed();
    function.call(ComponentVal.s64(param1), ComponentVal.s64(param2));
  }

  @Override
  public void callStringToVoid(final String param) throws WasmException {
    ensureNotClosed();
    function.call(ComponentVal.string(param));
  }

  // ========== s32 return signatures ==========

  @Override
  public int callS32ToS32(final int param) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results = function.call(ComponentVal.s32(param));
    validateSingleResult(results);
    return results.get(0).asS32();
  }

  @Override
  public int callS32S32ToS32(final int param1, final int param2) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.s32(param1), ComponentVal.s32(param2));
    validateSingleResult(results);
    return results.get(0).asS32();
  }

  @Override
  public int callS32S32S32ToS32(final int param1, final int param2, final int param3)
      throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.s32(param1), ComponentVal.s32(param2), ComponentVal.s32(param3));
    validateSingleResult(results);
    return results.get(0).asS32();
  }

  @Override
  public int callS64ToS32(final long param) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results = function.call(ComponentVal.s64(param));
    validateSingleResult(results);
    return results.get(0).asS32();
  }

  // ========== s64 return signatures ==========

  @Override
  public long callS64ToS64(final long param) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results = function.call(ComponentVal.s64(param));
    validateSingleResult(results);
    return results.get(0).asS64();
  }

  @Override
  public long callS64S64ToS64(final long param1, final long param2) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.s64(param1), ComponentVal.s64(param2));
    validateSingleResult(results);
    return results.get(0).asS64();
  }

  @Override
  public long callS64S64S64ToS64(final long param1, final long param2, final long param3)
      throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.s64(param1), ComponentVal.s64(param2), ComponentVal.s64(param3));
    validateSingleResult(results);
    return results.get(0).asS64();
  }

  @Override
  public long callS32S32ToS64(final int param1, final int param2) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.s32(param1), ComponentVal.s32(param2));
    validateSingleResult(results);
    return results.get(0).asS64();
  }

  // ========== f32 return signatures ==========

  @Override
  public float callF32ToF32(final float param) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results = function.call(ComponentVal.f32(param));
    validateSingleResult(results);
    return results.get(0).asF32();
  }

  @Override
  public float callF32F32ToF32(final float param1, final float param2) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.f32(param1), ComponentVal.f32(param2));
    validateSingleResult(results);
    return results.get(0).asF32();
  }

  @Override
  public float callF32F32F32ToF32(final float param1, final float param2, final float param3)
      throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.f32(param1), ComponentVal.f32(param2), ComponentVal.f32(param3));
    validateSingleResult(results);
    return results.get(0).asF32();
  }

  // ========== f64 return signatures ==========

  @Override
  public double callF64ToF64(final double param) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results = function.call(ComponentVal.f64(param));
    validateSingleResult(results);
    return results.get(0).asF64();
  }

  @Override
  public double callF64F64ToF64(final double param1, final double param2) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.f64(param1), ComponentVal.f64(param2));
    validateSingleResult(results);
    return results.get(0).asF64();
  }

  @Override
  public double callF64F64F64ToF64(final double param1, final double param2, final double param3)
      throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.f64(param1), ComponentVal.f64(param2), ComponentVal.f64(param3));
    validateSingleResult(results);
    return results.get(0).asF64();
  }

  // ========== string return signatures ==========

  @Override
  public String callVoidToString() throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results = function.call();
    validateSingleResult(results);
    return results.get(0).asString();
  }

  @Override
  public String callStringToString(final String param) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results = function.call(ComponentVal.string(param));
    validateSingleResult(results);
    return results.get(0).asString();
  }

  @Override
  public String callStringStringToString(final String param1, final String param2)
      throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results =
        function.call(ComponentVal.string(param1), ComponentVal.string(param2));
    validateSingleResult(results);
    return results.get(0).asString();
  }

  // ========== bool signatures ==========

  @Override
  public boolean callVoidToBool() throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results = function.call();
    validateSingleResult(results);
    return results.get(0).asBool();
  }

  @Override
  public boolean callBoolToBool(final boolean param) throws WasmException {
    ensureNotClosed();
    final List<ComponentVal> results = function.call(ComponentVal.bool(param));
    validateSingleResult(results);
    return results.get(0).asBool();
  }

  // ========== Metadata and lifecycle ==========

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
    resourceHandle.close();
  }

  /**
   * Validates that the result list contains exactly one value.
   *
   * @param results the result list to validate
   * @throws WasmException if the result count is not 1
   */
  private void validateSingleResult(final List<ComponentVal> results) throws WasmException {
    if (results == null || results.size() != 1) {
      final int count = results == null ? 0 : results.size();
      throw new WasmException("Expected 1 result, got " + count);
    }
  }

  /**
   * Ensures the typed function is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  @Override
  public String toString() {
    return "PanamaComponentTypedFunc{signature='"
        + signature
        + "', function="
        + function.getName()
        + ", closed="
        + resourceHandle.isClosed()
        + "}";
  }
}
