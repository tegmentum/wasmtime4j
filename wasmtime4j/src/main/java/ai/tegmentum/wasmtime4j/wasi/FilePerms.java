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

package ai.tegmentum.wasmtime4j.wasi;

/**
 * File permissions for WASI preopened directories.
 *
 * <p>Corresponds to wasmtime_wasi::FilePerms bitflags.
 *
 * @since 1.0.0
 */
public final class FilePerms {

  /** Permission to read file contents. */
  public static final int READ = 0x1;

  /** Permission to write file contents. */
  public static final int WRITE = 0x2;

  /** All permissions enabled. */
  public static final int ALL = READ | WRITE;

  /** No permissions. */
  public static final int NONE = 0;

  private final int bits;

  /**
   * Creates file permissions from raw bits.
   *
   * @param bits the permission bits
   */
  public FilePerms(final int bits) {
    this.bits = bits & ALL;
  }

  /**
   * Creates read-only file permissions.
   *
   * @return read-only permissions
   */
  public static FilePerms readOnly() {
    return new FilePerms(READ);
  }

  /**
   * Creates full file permissions.
   *
   * @return full permissions
   */
  public static FilePerms all() {
    return new FilePerms(ALL);
  }

  /**
   * Creates no permissions.
   *
   * @return no permissions
   */
  public static FilePerms none() {
    return new FilePerms(NONE);
  }

  /**
   * Gets the raw permission bits.
   *
   * @return the bits
   */
  public int getBits() {
    return bits;
  }

  /**
   * Checks if read permission is set.
   *
   * @return true if read is allowed
   */
  public boolean canRead() {
    return (bits & READ) != 0;
  }

  /**
   * Checks if write permission is set.
   *
   * @return true if write is allowed
   */
  public boolean canWrite() {
    return (bits & WRITE) != 0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FilePerms)) {
      return false;
    }
    return bits == ((FilePerms) obj).bits;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(bits);
  }

  @Override
  public String toString() {
    return "FilePerms{" + "read=" + canRead() + ", write=" + canWrite() + '}';
  }

  private FilePerms() {
    this.bits = 0;
  }
}
