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
 * Optimization levels for WebAssembly compilation.
 *
 * <p>These levels control the trade-off between compilation time and runtime performance. Higher
 * optimization levels may take longer to compile but produce faster code.
 *
 * @since 1.0.0
 */
public enum OptimizationLevel {
  /** No optimization - fastest compilation, slowest execution. */
  NONE,

  /** Optimize for runtime speed - slower compilation, fastest execution. */
  SPEED,

  /** Optimize for code size - moderate compilation time, smaller code size. */
  SIZE,

  /** Optimize for both speed and size - balanced approach. */
  SPEED_AND_SIZE
}
