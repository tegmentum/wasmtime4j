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
package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.WasmValue;

/**
 * Base interface for WebAssembly GC objects.
 *
 * <p>Represents any object managed by the WebAssembly garbage collector, including structs, arrays,
 * and I31 values. All GC objects have a unique identifier and type information.
 *
 * @since 1.0.0
 */
public interface GcObject {
  /**
   * Gets the unique object identifier.
   *
   * @return the object ID
   */
  long getObjectId();

  /**
   * Gets the GC reference type of this object.
   *
   * @return the reference type
   */
  GcReferenceType getReferenceType();

  /**
   * Checks if this object is null.
   *
   * @return true if this is a null reference
   */
  boolean isNull();

  /**
   * Checks if this object is of the specified type.
   *
   * @param type the type to check against
   * @return true if this object is of the specified type
   */
  boolean isOfType(GcReferenceType type);

  /**
   * Attempts to cast this object to the specified type.
   *
   * @param type the target type
   * @return this object if the cast is valid
   * @throws ClassCastException if the cast is invalid
   */
  GcObject castTo(GcReferenceType type);

  /**
   * Checks if this object equals another object by identity.
   *
   * @param other the other object
   * @return true if objects are identical
   */
  boolean refEquals(GcObject other);

  /**
   * Gets the size of this object in bytes.
   *
   * @return the object size in bytes
   */
  int getSizeBytes();

  /**
   * Converts this GC object to a WasmValue for WebAssembly operations.
   *
   * @return the corresponding WasmValue
   */
  WasmValue toWasmValue();
}
