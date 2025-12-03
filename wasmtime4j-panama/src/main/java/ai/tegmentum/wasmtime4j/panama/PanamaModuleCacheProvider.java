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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheConfig;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheFactory;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Panama FFI provider for ModuleCache.
 *
 * <p>This provider is registered via META-INF/services for ServiceLoader discovery.
 *
 * @since 1.0.0
 */
public final class PanamaModuleCacheProvider implements ModuleCacheFactory.ModuleCacheProvider {

  @Override
  public ModuleCache create(final Engine engine, final ModuleCacheConfig config)
      throws WasmException {
    if (!(engine instanceof PanamaEngine)) {
      return null; // Let another provider handle this
    }
    return new PanamaModuleCache((PanamaEngine) engine, config);
  }
}
