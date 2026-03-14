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
package ai.tegmentum.wasmtime4j.wit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Information about WebAssembly Interface Types (WIT) support in the runtime.
 *
 * <p>This class provides details about the WIT capabilities and features supported by the current
 * WebAssembly runtime implementation.
 *
 * @since 1.0.0
 */
public final class WitSupportInfo {

  private final boolean supported;
  private final String version;
  private final Set<String> supportedFeatures;
  private final List<String> supportedTypes;
  private final int maxInterfaceDepth;

  /**
   * Creates new WIT support information.
   *
   * @param supported whether WIT is supported
   * @param version the WIT version supported
   * @param supportedFeatures the set of supported WIT features
   * @param supportedTypes the list of supported WIT types
   * @param maxInterfaceDepth the maximum interface nesting depth
   */
  public WitSupportInfo(
      final boolean supported,
      final String version,
      final Set<String> supportedFeatures,
      final List<String> supportedTypes,
      final int maxInterfaceDepth) {
    this.supported = supported;
    this.version = version;
    this.supportedFeatures = Collections.unmodifiableSet(new HashSet<>(supportedFeatures));
    this.supportedTypes = Collections.unmodifiableList(new ArrayList<>(supportedTypes));
    this.maxInterfaceDepth = maxInterfaceDepth;
  }

  /**
   * Checks if WIT is supported by the runtime.
   *
   * @return true if WIT is supported, false otherwise
   */
  public boolean isSupported() {
    return supported;
  }

  /**
   * Gets the supported WIT version.
   *
   * @return the WIT version string
   */
  public String getVersion() {
    return version;
  }

  /**
   * Gets the set of supported WIT features.
   *
   * @return the supported features
   */
  public Set<String> getSupportedFeatures() {
    return supportedFeatures;
  }

  /**
   * Gets the list of supported WIT types.
   *
   * @return the supported types
   */
  public List<String> getSupportedTypes() {
    return supportedTypes;
  }

  /**
   * Gets the maximum interface nesting depth.
   *
   * @return the maximum interface depth
   */
  public int getMaxInterfaceDepth() {
    return maxInterfaceDepth;
  }

  /**
   * Checks if a specific feature is supported.
   *
   * @param feature the feature name to check
   * @return true if the feature is supported, false otherwise
   */
  public boolean isFeatureSupported(final String feature) {
    return supportedFeatures.contains(feature);
  }

  /**
   * Checks if a specific type is supported.
   *
   * @param type the type name to check
   * @return true if the type is supported, false otherwise
   */
  public boolean isTypeSupported(final String type) {
    return supportedTypes.contains(type);
  }

  /**
   * Creates a WitSupportInfo indicating no WIT support.
   *
   * @return WIT support info for unsupported runtime
   */
  public static WitSupportInfo unsupported() {
    return new WitSupportInfo(false, "none", Collections.emptySet(), Collections.emptyList(), 0);
  }

  /**
   * Creates a WitSupportInfo for basic WIT support.
   *
   * @return WIT support info for basic support
   */
  public static WitSupportInfo basic() {
    return new WitSupportInfo(
        true,
        "1.0",
        Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("interfaces", "functions", "types"))),
        Collections.unmodifiableList(
            Arrays.asList(
                "u8", "u16", "u32", "u64", "s8", "s16", "s32", "s64", "f32", "f64", "string")),
        10);
  }

  @Override
  public String toString() {
    return "WitSupportInfo{"
        + "supported="
        + supported
        + ", version='"
        + version
        + '\''
        + ", features="
        + supportedFeatures.size()
        + ", types="
        + supportedTypes.size()
        + ", maxDepth="
        + maxInterfaceDepth
        + '}';
  }
}
