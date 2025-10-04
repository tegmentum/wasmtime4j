/*
 * Copyright 2024 Tegmentum AI
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

import java.util.List;
import java.util.Objects;

/**
 * Conditions for loading WebAssembly components.
 *
 * <p>This class defines requirements and constraints that must be met for a component to be loaded
 * successfully.
 *
 * @since 1.0.0
 */
public final class ComponentLoadConditions {

  private final List<String> requiredImports;
  private final List<String> requiredFeatures;
  private final long maxMemoryBytes;
  private final boolean allowUnsafeFeatures;

  /**
   * Creates new component load conditions with default values.
   *
   * <p>Default values:
   *
   * <ul>
   *   <li>requiredImports: empty list
   *   <li>requiredFeatures: empty list
   *   <li>maxMemoryBytes: 1GB
   *   <li>allowUnsafeFeatures: false
   * </ul>
   */
  public ComponentLoadConditions() {
    this(List.of(), List.of(), 1024L * 1024 * 1024, false);
  }

  /**
   * Creates new component load conditions.
   *
   * @param requiredImports list of imports that must be satisfied
   * @param requiredFeatures list of WebAssembly features that must be supported
   * @param maxMemoryBytes maximum memory in bytes
   * @param allowUnsafeFeatures whether to allow unsafe features
   */
  public ComponentLoadConditions(
      final List<String> requiredImports,
      final List<String> requiredFeatures,
      final long maxMemoryBytes,
      final boolean allowUnsafeFeatures) {
    this.requiredImports =
        List.copyOf(Objects.requireNonNull(requiredImports, "requiredImports cannot be null"));
    this.requiredFeatures =
        List.copyOf(Objects.requireNonNull(requiredFeatures, "requiredFeatures cannot be null"));
    this.maxMemoryBytes = maxMemoryBytes;
    this.allowUnsafeFeatures = allowUnsafeFeatures;
  }

  /**
   * Gets the list of required imports.
   *
   * @return immutable list of required imports
   */
  public List<String> getRequiredImports() {
    return requiredImports;
  }

  /**
   * Gets the list of required features.
   *
   * @return immutable list of required features
   */
  public List<String> getRequiredFeatures() {
    return requiredFeatures;
  }

  /**
   * Gets the maximum memory in bytes.
   *
   * @return maximum memory bytes
   */
  public long getMaxMemoryBytes() {
    return maxMemoryBytes;
  }

  /**
   * Checks if unsafe features are allowed.
   *
   * @return true if unsafe features are allowed
   */
  public boolean isAllowUnsafeFeatures() {
    return allowUnsafeFeatures;
  }
}
