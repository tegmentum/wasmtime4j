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

/**
 * Represents a WebAssembly continuation reference.
 *
 * <p>ContRef is a rooted reference to a continuation from the WebAssembly stack switching proposal.
 * Continuations represent suspended computations that can be resumed.
 *
 * <p>Continuation references are part of the WebAssembly stack switching proposal (typed
 * continuations) and represent a first-class handle to a suspended computation. Unlike GC reference
 * types (anyref, structref, arrayref), continuation references form their own separate type
 * hierarchy and are not subtypes of anyref.
 *
 * <p><b>Note:</b> Stack switching is an experimental feature in Wasmtime. This interface provides
 * the type system representation but actual suspension/resumption operations require the stack
 * switching proposal to be enabled and stabilized.
 *
 * @since 1.0.0
 */
public interface ContRef {

  /**
   * Gets the native handle for this continuation reference.
   *
   * <p>This method is intended for internal use by the runtime implementations.
   *
   * @return the native handle
   */
  long getNativeHandle();

  /**
   * Checks if this continuation reference is still valid.
   *
   * <p>Continuation references can become invalid if their owning store is closed or if they are
   * explicitly invalidated.
   *
   * @return true if this reference is valid and can be used
   */
  boolean isValid();
}
