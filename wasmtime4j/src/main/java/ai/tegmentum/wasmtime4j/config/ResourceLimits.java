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
 * Resource limits configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ResourceLimits {

  /**
   * Gets the maximum memory limit in bytes.
   *
   * @return the memory limit in bytes
   */
  long getMemoryLimitBytes();

  /**
   * Gets the maximum execution time in milliseconds.
   *
   * @return the time limit in milliseconds
   */
  long getExecutionTimeLimitMs();

  /**
   * Gets the maximum stack depth.
   *
   * @return the maximum stack depth
   */
  int getMaxStackDepth();

  /**
   * Checks if resource limits are enabled.
   *
   * @return true if resource limits are enabled
   */
  boolean isEnabled();
}
