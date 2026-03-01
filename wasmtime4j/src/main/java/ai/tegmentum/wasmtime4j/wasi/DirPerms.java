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
 * Directory permissions for WASI preopened directories.
 *
 * <p>Corresponds to wasmtime_wasi::DirPerms bitflags.
 *
 * @since 1.0.0
 */
public final class DirPerms {

  /** Permission to read directory contents (list entries). */
  public static final int READ = 0x1;

  /** Permission to mutate directory contents (create, rename, delete). */
  public static final int MUTATE = 0x2;

  /** All permissions enabled. */
  public static final int ALL = READ | MUTATE;

  /** No permissions. */
  public static final int NONE = 0;

  private final int bits;

  /**
   * Creates directory permissions from raw bits.
   *
   * @param bits the permission bits
   */
  public DirPerms(final int bits) {
    this.bits = bits & ALL;
  }

  /**
   * Creates read-only directory permissions.
   *
   * @return read-only permissions
   */
  public static DirPerms readOnly() {
    return new DirPerms(READ);
  }

  /**
   * Creates full directory permissions.
   *
   * @return full permissions
   */
  public static DirPerms all() {
    return new DirPerms(ALL);
  }

  /**
   * Creates no permissions.
   *
   * @return no permissions
   */
  public static DirPerms none() {
    return new DirPerms(NONE);
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
   * Checks if mutate permission is set.
   *
   * @return true if mutate is allowed
   */
  public boolean canMutate() {
    return (bits & MUTATE) != 0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DirPerms)) {
      return false;
    }
    return bits == ((DirPerms) obj).bits;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(bits);
  }

  @Override
  public String toString() {
    return "DirPerms{" + "read=" + canRead() + ", mutate=" + canMutate() + '}';
  }

  private DirPerms() {
    this.bits = 0;
  }
}
