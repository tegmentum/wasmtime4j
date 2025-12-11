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

/**
 * Base interface for all WebAssembly GC reference types.
 *
 * <p>This interface provides common functionality for all GC reference types in the WebAssembly GC
 * proposal, including anyref, eqref, structref, arrayref, and i31ref.
 *
 * @since 1.0.0
 */
public interface GcRef {

  /**
   * Checks if this is a null reference.
   *
   * @return true if this is a null reference
   */
  boolean isNull();

  /**
   * Gets the GC reference type of this reference.
   *
   * @return the reference type
   */
  GcReferenceType getReferenceType();

  /**
   * Gets the unique identifier for this reference.
   *
   * @return the reference ID
   */
  long getId();
}
