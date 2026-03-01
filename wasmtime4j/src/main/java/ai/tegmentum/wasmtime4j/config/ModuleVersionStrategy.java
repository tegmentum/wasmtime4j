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
package ai.tegmentum.wasmtime4j.config;

/**
 * Strategy for module version validation during deserialization.
 *
 * <p>When deserializing precompiled modules, this controls how version compatibility is checked
 * between the module and the engine that compiled it.
 *
 * @since 1.0.0
 */
public enum ModuleVersionStrategy {
  /** Use the Wasmtime version for module compatibility checks (default). */
  WASMTIME_VERSION,
  /** Disable version checks entirely. */
  NONE,
  /** Use a custom version string for compatibility checks. */
  CUSTOM
}
