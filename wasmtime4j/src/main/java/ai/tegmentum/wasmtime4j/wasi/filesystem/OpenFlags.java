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
 * Flags controlling file creation and opening behavior.
 *
 * <p>These flags correspond to the open-flags from the WASI Preview 2 filesystem specification.
 * They control how files are created and opened.
 *
 * @since 1.0.0
 */
public enum OpenFlags {
  /**
   * Create the file if it does not exist.
   *
   * <p>Similar to O_CREAT in POSIX. If the file already exists and EXCLUSIVE is not set, the
   * existing file is opened.
   */
  CREATE,

  /**
   * Fail if the path does not refer to a directory.
   *
   * <p>Similar to O_DIRECTORY in POSIX. This ensures that the opened descriptor refers to a
   * directory, failing if it refers to any other type of filesystem object.
   */
  DIRECTORY,

  /**
   * Fail if the file already exists.
   *
   * <p>Similar to O_EXCL in POSIX. This flag only has meaning when combined with CREATE. It causes
   * the open operation to fail if the file already exists, ensuring that the file is newly created
   * rather than opened.
   */
  EXCLUSIVE,

  /**
   * Truncate the file to size zero after opening.
   *
   * <p>Similar to O_TRUNC in POSIX. If the file exists and is a regular file, and the descriptor is
   * opened with write access, the file's length is set to zero. This flag has no effect on
   * directories or other non-regular files.
   */
  TRUNCATE;

  /**
   * Creates a set of open flags from individual flag values.
   *
   * @param flags the flags to include in the set
   * @return an immutable set containing the specified flags
   */
  public static Set<OpenFlags> of(final OpenFlags... flags) {
    if (flags.length == 0) {
      return EnumSet.noneOf(OpenFlags.class);
    }
    return EnumSet.of(flags[0], flags);
  }

  /**
   * Creates an empty set of open flags.
   *
   * @return an empty immutable set
   */
  public static Set<OpenFlags> none() {
    return EnumSet.noneOf(OpenFlags.class);
  }

  /**
   * Creates a set containing all open flags.
   *
   * @return an immutable set containing all flags
   */
  public static Set<OpenFlags> all() {
    return EnumSet.allOf(OpenFlags.class);
  }
}
