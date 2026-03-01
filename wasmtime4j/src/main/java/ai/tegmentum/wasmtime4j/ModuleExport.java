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
 * An opaque handle representing a pre-resolved export from a WebAssembly module.
 *
 * <p>ModuleExport provides O(1) export lookup by caching the internal index that Wasmtime uses to
 * locate exports. Instead of performing a string-based hash lookup on every access, callers can
 * resolve the export name once via {@link Module#getModuleExport(String)} and then use this handle
 * for repeated fast lookups via {@link Instance#getExport(Store, ModuleExport)}.
 *
 * <p>A ModuleExport is tied to the {@link Module} that created it. Using a ModuleExport with an
 * Instance created from a different Module produces undefined behavior.
 *
 * <p>ModuleExport instances are lightweight and safe to cache for the lifetime of their parent
 * Module.
 *
 * @since 1.0.0
 */
public interface ModuleExport {

  /**
   * Returns the name of the export this handle refers to.
   *
   * @return the export name
   */
  String name();

  /**
   * Returns the native handle for this module export.
   *
   * <p>This is an internal method used by runtime implementations and should not be called by
   * application code.
   *
   * @return the native handle value
   */
  long nativeHandle();
}
