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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a WIT flags value (bitset type).
 *
 * <p>Flags are a set of named boolean flags that can be independently set or unset. They are
 * similar to bitfields in C or EnumSet in Java. Each flag has a name, and a flags value contains
 * the set of flags that are currently enabled.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create flags type definition
 * WitType permissionsType = WitType.flags("permissions",
 *     Arrays.asList("read", "write", "execute"));
 *
 * // Create flags values
 * WitFlags readWrite = WitFlags.of(permissionsType, "read", "write");
 * WitFlags readOnly = WitFlags.of(permissionsType, "read");
 * WitFlags allPerms = WitFlags.of(permissionsType, "read", "write", "execute");
 *
 * // Check flags
 * boolean canWrite = readWrite.isSet("write");  // true
 * boolean canExecute = readWrite.isSet("execute");  // false
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WitFlags extends WitValue {

  private final Set<String> setFlags;

  /**
   * Creates a new WIT flags value.
   *
   * @param flagsType the flags type definition
   * @param setFlags the set of enabled flags
   */
  private WitFlags(final WitType flagsType, final Set<String> setFlags) {
    super(flagsType);
    if (setFlags == null) {
      throw new IllegalArgumentException("Set flags cannot be null");
    }
    // Defensive copy
    this.setFlags = new HashSet<>(setFlags);
    validate();
  }

  /**
   * Creates WIT flags with the specified flags set.
   *
   * @param flagsType the flags type (must be a flags type)
   * @param flagNames the names of flags to set (must all be valid flags)
   * @return a WIT flags value
   * @throws IllegalArgumentException if any flag name is not valid for this type
   */
  public static WitFlags of(final WitType flagsType, final String... flagNames) {
    final Set<String> flags = new HashSet<>(Arrays.asList(flagNames));
    return new WitFlags(flagsType, flags);
  }

  /**
   * Creates WIT flags with the specified flags set.
   *
   * @param flagsType the flags type (must be a flags type)
   * @param flagNames the names of flags to set (must all be valid flags)
   * @return a WIT flags value
   * @throws IllegalArgumentException if any flag name is not valid for this type
   */
  public static WitFlags of(final WitType flagsType, final Set<String> flagNames) {
    return new WitFlags(flagsType, flagNames);
  }

  /**
   * Creates empty WIT flags with no flags set.
   *
   * @param flagsType the flags type (must be a flags type)
   * @return a WIT flags value with no flags set
   */
  public static WitFlags empty(final WitType flagsType) {
    return new WitFlags(flagsType, Collections.emptySet());
  }

  /**
   * Creates a builder for constructing WIT flags.
   *
   * @param flagsType the flags type
   * @return a new flags builder
   */
  public static Builder builder(final WitType flagsType) {
    return new Builder(flagsType);
  }

  /**
   * Checks if a specific flag is set.
   *
   * @param flagName the flag name to check
   * @return true if the flag is set
   */
  public boolean isSet(final String flagName) {
    return setFlags.contains(flagName);
  }

  /**
   * Gets the set of enabled flag names.
   *
   * @return an unmodifiable set of flag names that are set
   */
  public Set<String> getSetFlags() {
    return Collections.unmodifiableSet(setFlags);
  }

  /**
   * Gets the number of flags that are set.
   *
   * @return the count of set flags
   */
  public int size() {
    return setFlags.size();
  }

  /**
   * Checks if no flags are set.
   *
   * @return true if no flags are set
   */
  public boolean isEmpty() {
    return setFlags.isEmpty();
  }

  @Override
  public Set<String> toJava() {
    return new HashSet<>(setFlags);
  }

  @Override
  protected void validate() {
    // Extract valid flag names from flags type
    final List<String> validFlags = extractFlagNames(getType());

    // Verify all set flags are valid
    for (final String flagName : setFlags) {
      if (flagName == null || flagName.isEmpty()) {
        throw new IllegalArgumentException("Flag name cannot be null or empty");
      }
      if (!validFlags.contains(flagName)) {
        throw new IllegalArgumentException(
            String.format(
                "Flag '%s' not found in flags type. Valid flags: %s", flagName, validFlags));
      }
    }
  }

  /**
   * Extracts the flag names from a flags type.
   *
   * @param flagsType the flags type
   * @return a list of valid flag names
   */
  private static List<String> extractFlagNames(final WitType flagsType) {
    // Get flag names from flags type kind
    // This is a simplified extraction - in a full implementation,
    // WitType would provide a getFlagNames() method
    if (flagsType.getKind() == null || flagsType.getKind().getCategory() != WitTypeCategory.FLAGS) {
      throw new IllegalArgumentException("Type must be a flags type");
    }

    // Get flag names from the type kind
    return flagsType.getKind().getFlags();
  }

  @Override
  public String toString() {
    return "WitFlags" + setFlags.stream().sorted().collect(Collectors.toList());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitFlags)) {
      return false;
    }
    final WitFlags other = (WitFlags) obj;
    return Objects.equals(getType(), other.getType()) && setFlags.equals(other.setFlags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), setFlags);
  }

  /** Builder for constructing WIT flags with a fluent API. */
  public static final class Builder {
    private final WitType flagsType;
    private final Set<String> flags = new HashSet<>();

    private Builder(final WitType flagsType) {
      if (flagsType == null) {
        throw new IllegalArgumentException("Flags type cannot be null");
      }
      this.flagsType = flagsType;
    }

    /**
     * Sets a flag.
     *
     * @param flagName the flag name to set (must be a valid flag)
     * @return this builder
     * @throws IllegalArgumentException if flag name is invalid
     */
    public Builder set(final String flagName) {
      if (flagName == null || flagName.isEmpty()) {
        throw new IllegalArgumentException("Flag name cannot be null or empty");
      }
      flags.add(flagName);
      return this;
    }

    /**
     * Sets multiple flags.
     *
     * @param flagNames the flag names to set
     * @return this builder
     */
    public Builder setAll(final String... flagNames) {
      for (final String flagName : flagNames) {
        set(flagName);
      }
      return this;
    }

    /**
     * Builds the WIT flags value.
     *
     * @return the constructed WIT flags
     */
    public WitFlags build() {
      return new WitFlags(flagsType, flags);
    }
  }
}
