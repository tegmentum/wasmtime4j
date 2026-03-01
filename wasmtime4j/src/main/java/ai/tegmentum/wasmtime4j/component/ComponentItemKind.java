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
 * Enumeration of component item kinds matching Wasmtime's {@code ComponentItem} variants.
 *
 * <p>Each variant represents a different type of item that can appear in a component's imports or
 * exports.
 *
 * @since 1.1.0
 */
public enum ComponentItemKind {
  /** A component model function (high-level, with component model types). */
  COMPONENT_FUNC,

  /** A core WebAssembly function (low-level, with core value types). */
  CORE_FUNC,

  /** A WebAssembly core module. */
  MODULE,

  /** A nested component. */
  COMPONENT,

  /** A component instance (a collection of named exports). */
  COMPONENT_INSTANCE,

  /** A type definition. */
  TYPE,

  /** A resource type. */
  RESOURCE
}
