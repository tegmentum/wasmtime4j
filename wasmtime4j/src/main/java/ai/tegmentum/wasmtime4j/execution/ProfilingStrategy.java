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
package ai.tegmentum.wasmtime4j.execution;

/**
 * Profiling strategies for WebAssembly execution.
 *
 * <p>These strategies control how profiling information is collected during WebAssembly execution,
 * which can be useful for performance analysis and optimization.
 *
 * @since 1.0.0
 */
public enum ProfilingStrategy {
  /** No profiling - minimal overhead. */
  NONE,

  /** JIT function profiling - tracks JIT compilation events. */
  JIT_DUMP,

  /** Performance event profiling - tracks runtime performance events. */
  PERF_MAP,

  /** VTune profiling - integration with Intel VTune profiler. */
  VTUNE,

  /** Pulley profiling - profiling for the Pulley interpreter backend. */
  PULLEY
}
