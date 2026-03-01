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
 * Compilation strategy enumeration for WebAssembly code generation.
 *
 * <p>Maps to Wasmtime's {@code Strategy} enum which determines which compiler backend is used.
 *
 * @since 1.0.0
 */
public enum CompilationStrategy {
  /** Auto-detect best compilation strategy. */
  AUTO,
  /** Use the Cranelift compiler backend (optimizing). */
  CRANELIFT,
  /** Use the Winch compiler backend (baseline). */
  WINCH
}
