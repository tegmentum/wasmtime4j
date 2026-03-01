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
package ai.tegmentum.wasmtime4j.component;

/**
 * An opaque pre-computed export index for efficient repeated function lookups.
 *
 * <p>A {@code ComponentExportIndex} is obtained from {@link Component#exportIndex(String)} and can
 * be passed to {@link ComponentInstance#getFunc(ComponentExportIndex)} for O(1) function lookup
 * instead of the O(n) string-based lookup.
 *
 * <p>This is useful in hot loops where the same function is called many times:
 *
 * <pre>{@code
 * // Pre-compute the index once
 * ComponentExportIndex addIndex = component.exportIndex(null, "add")
 *     .orElseThrow(() -> new WasmException("export 'add' not found"));
 *
 * // Use the index for fast repeated lookups
 * for (int i = 0; i < 1000; i++) {
 *     ComponentFunction func = instance.getFunc(addIndex)
 *         .orElseThrow();
 *     func.call(i, i + 1);
 * }
 * }</pre>
 *
 * <p>Instances of this class wrap a native pointer and must be {@link #close() closed} when no
 * longer needed to free native memory. Using try-with-resources is recommended.
 *
 * @since 1.0.0
 */
public interface ComponentExportIndex extends AutoCloseable {

  /**
   * Gets the native handle for this export index.
   *
   * <p>This is intended for internal use by JNI and Panama implementations.
   *
   * @return the native handle value
   */
  long getNativeHandle();

  /**
   * Checks if this export index is still valid (not closed).
   *
   * @return true if the index is valid, false if it has been closed
   */
  boolean isValid();

  @Override
  void close();
}
