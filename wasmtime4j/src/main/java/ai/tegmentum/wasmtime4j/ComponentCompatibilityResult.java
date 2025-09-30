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

/**
 * Result of component compatibility checking operation.
 *
 * <p>This class provides detailed information about the compatibility between two components,
 * including compatibility status and descriptive details about any issues found.
 *
 * @since 1.0.0
 */
public final class ComponentCompatibilityResult {

  private final boolean compatible;
  private final String details;

  /**
   * Creates a new component compatibility result.
   *
   * @param compatible whether the components are compatible
   * @param details detailed information about the compatibility check
   */
  public ComponentCompatibilityResult(final boolean compatible, final String details) {
    this.compatible = compatible;
    this.details = details != null ? details : "";
  }

  /**
   * Creates a compatible result with details.
   *
   * @param details compatibility details
   * @return a compatible result
   */
  public static ComponentCompatibilityResult compatible(final String details) {
    return new ComponentCompatibilityResult(true, details);
  }

  /**
   * Creates an incompatible result with details.
   *
   * @param details incompatibility details
   * @return an incompatible result
   */
  public static ComponentCompatibilityResult incompatible(final String details) {
    return new ComponentCompatibilityResult(false, details);
  }

  /**
   * Checks if the components are compatible.
   *
   * @return true if compatible, false otherwise
   */
  public boolean isCompatible() {
    return compatible;
  }

  /**
   * Gets the compatibility details.
   *
   * @return the compatibility details
   */
  public String getDetails() {
    return details;
  }

  @Override
  public String toString() {
    return "ComponentCompatibilityResult{compatible=" + compatible + ", details='" + details + "'}";
  }
}
