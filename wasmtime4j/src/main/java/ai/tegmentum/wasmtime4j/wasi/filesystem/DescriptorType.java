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

/**
 * Type of filesystem object referenced by a descriptor.
 *
 * <p>This enum corresponds to the descriptor-type from the WASI Preview 2 filesystem specification.
 * It identifies the type of inode a descriptor references.
 *
 * @since 1.0.0
 */
public enum DescriptorType {
  /** The type of the filesystem object is unknown or cannot be determined. */
  UNKNOWN,

  /** The descriptor refers to a block device inode. */
  BLOCK_DEVICE,

  /** The descriptor refers to a character device inode. */
  CHARACTER_DEVICE,

  /** The descriptor refers to a directory inode. */
  DIRECTORY,

  /** The descriptor refers to a named pipe (FIFO) inode. */
  FIFO,

  /** The descriptor refers to a symbolic link inode. */
  SYMBOLIC_LINK,

  /** The descriptor refers to a regular file inode. */
  REGULAR_FILE,

  /** The descriptor refers to a socket inode. */
  SOCKET
}
