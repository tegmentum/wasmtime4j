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
package ai.tegmentum.wasmtime4j.wasi;

/**
 * Access mode for a WASI preopened directory.
 *
 * <p>Mirrors the upstream {@code wasmtime_wasi::FsPerms} enum introduced in wasmtime 48. The
 * previous {@code DirPerms}/{@code FilePerms} bit-flag pair was collapsed into a two-value access
 * mode; any granular combination now maps to {@link #READ_ONLY} or {@link #READ_WRITE}.
 *
 * @since 2.0.0
 */
public enum FsPerms {

  /** Grant read-only access to the preopened directory and every file below it. */
  READ_ONLY(0),

  /** Grant read/write access to the preopened directory and every file below it. */
  READ_WRITE(1);

  private final int value;

  FsPerms(final int value) {
    this.value = value;
  }

  /**
   * Gets the integer value used for FFI communication with the native side.
   *
   * @return the integer value
   */
  public int getValue() {
    return value;
  }

  /**
   * Converts an integer value to the corresponding enum constant.
   *
   * @param value the integer value
   * @return the corresponding enum constant
   * @throws IllegalArgumentException if the value does not correspond to any constant
   */
  public static FsPerms fromValue(final int value) {
    for (final FsPerms perms : values()) {
      if (perms.value == value) {
        return perms;
      }
    }
    throw new IllegalArgumentException("Unknown FsPerms value: " + value);
  }
}
