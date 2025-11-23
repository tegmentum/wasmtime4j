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
 * Flags controlling path resolution behavior.
 *
 * <p>These flags correspond to the path-flags from the WASI Preview 2 filesystem specification.
 * They control how paths are resolved, particularly regarding symbolic links.
 *
 * @since 1.0.0
 */
public enum PathFlags {
  /**
   * Follow symbolic links during path resolution.
   *
   * <p>When this flag is set, symbolic links encountered during path resolution are expanded and
   * followed to their targets. Without this flag, operations act on the symbolic link itself rather
   * than its target.
   */
  SYMLINK_FOLLOW;

  /**
   * Creates a set of path flags from individual flag values.
   *
   * @param flags the flags to include in the set
   * @return an immutable set containing the specified flags
   */
  public static Set<PathFlags> of(final PathFlags... flags) {
    if (flags.length == 0) {
      return EnumSet.noneOf(PathFlags.class);
    }
    return EnumSet.of(flags[0], flags);
  }

  /**
   * Creates an empty set of path flags.
   *
   * @return an empty immutable set
   */
  public static Set<PathFlags> none() {
    return EnumSet.noneOf(PathFlags.class);
  }

  /**
   * Creates a set containing all path flags.
   *
   * @return an immutable set containing all flags
   */
  public static Set<PathFlags> all() {
    return EnumSet.allOf(PathFlags.class);
  }
}
