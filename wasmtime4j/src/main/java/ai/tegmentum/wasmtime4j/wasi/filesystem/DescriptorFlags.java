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

package ai.tegmentum.wasmtime4j.wasi.filesystem;

import java.util.EnumSet;
import java.util.Set;

/**
 * Descriptor flags controlling access permissions.
 *
 * <p>These flags correspond to the descriptor-flags from the WASI Preview 2 filesystem
 * specification. They control read/write access and synchronization behavior.
 *
 * @since 1.0.0
 */
public enum DescriptorFlags {
  /** Data can be read from the descriptor. */
  READ,

  /** Data can be written to the descriptor. */
  WRITE,

  /**
   * File integrity synchronized I/O.
   *
   * <p>Requests that write operations wait until data and all metadata are written to the
   * underlying storage device.
   */
  FILE_INTEGRITY_SYNC,

  /**
   * Data integrity synchronized I/O.
   *
   * <p>Requests that write operations wait until data is written to the underlying storage device,
   * but not necessarily all metadata.
   */
  DATA_INTEGRITY_SYNC,

  /**
   * Read operations should have integrity matching the write synchronization level.
   *
   * <p>If this flag is set along with FILE_INTEGRITY_SYNC or DATA_INTEGRITY_SYNC, read operations
   * return data that has been synchronized to the level specified by those flags.
   */
  REQUESTED_WRITE_SYNC,

  /**
   * Directory contents may be modified.
   *
   * <p>When set on a directory descriptor, allows creation, renaming, and deletion of files within
   * the directory.
   */
  MUTATE_DIRECTORY;

  /**
   * Creates a set of descriptor flags from individual flag values.
   *
   * @param flags the flags to include in the set
   * @return an immutable set containing the specified flags
   */
  public static Set<DescriptorFlags> of(final DescriptorFlags... flags) {
    return EnumSet.of(flags[0], flags);
  }

  /**
   * Creates an empty set of descriptor flags.
   *
   * @return an empty immutable set
   */
  public static Set<DescriptorFlags> none() {
    return EnumSet.noneOf(DescriptorFlags.class);
  }

  /**
   * Creates a set containing all descriptor flags.
   *
   * @return an immutable set containing all flags
   */
  public static Set<DescriptorFlags> all() {
    return EnumSet.allOf(DescriptorFlags.class);
  }
}
