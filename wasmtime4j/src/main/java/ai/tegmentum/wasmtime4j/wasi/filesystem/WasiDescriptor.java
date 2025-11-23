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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.util.List;
import java.util.Set;

/**
 * WASI Preview 2 filesystem descriptor interface.
 *
 * <p>A descriptor is a reference to a filesystem object, which may be a file, directory, named
 * pipe, special file, or other object on which filesystem calls may be made.
 *
 * <p>This interface corresponds to the wasi:filesystem/types.descriptor resource from the WASI
 * Preview 2 specification.
 *
 * <p>Descriptors use randomized identifiers to prevent applications from depending on specific
 * index values. All paths in WASI are relative paths interpreted relative to a descriptor referring
 * to a base directory.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Open a file
 * try (WasiDescriptor dir = getPreopenedDirectory("/")) {
 *   WasiDescriptor file = dir.openAt(
 *     "data.txt",
 *     PathFlags.none(),
 *     OpenFlags.of(OpenFlags.CREATE),
 *     DescriptorFlags.of(DescriptorFlags.READ, DescriptorFlags.WRITE)
 *   );
 *
 *   // Read via stream
 *   try (WasiInputStream stream = file.readViaStream(0)) {
 *     byte[] data = stream.blockingRead(1024);
 *   }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiDescriptor extends WasiResource {

  /**
   * Creates an input stream for reading from this descriptor.
   *
   * <p>Multiple read streams may be active on the same descriptor simultaneously. Position is
   * maintained per stream.
   *
   * @param offset byte offset to start reading from
   * @return an input stream for reading
   * @throws WasmException if stream creation fails
   * @throws IllegalArgumentException if offset is negative
   * @throws IllegalStateException if descriptor is not valid or not readable
   */
  WasiInputStream readViaStream(final long offset) throws WasmException;

  /**
   * Creates an output stream for writing to this descriptor.
   *
   * <p>Multiple write streams may be active on the same descriptor simultaneously. Position is
   * maintained per stream.
   *
   * @param offset byte offset to start writing at
   * @return an output stream for writing
   * @throws WasmException if stream creation fails
   * @throws IllegalArgumentException if offset is negative
   * @throws IllegalStateException if descriptor is not valid or not writable
   */
  WasiOutputStream writeViaStream(final long offset) throws WasmException;

  /**
   * Creates an output stream for appending to this descriptor.
   *
   * <p>Data is always written at the end of the file, regardless of concurrent operations.
   *
   * @return an output stream for appending
   * @throws WasmException if stream creation fails
   * @throws IllegalStateException if descriptor is not valid or not writable
   */
  WasiOutputStream appendViaStream() throws WasmException;

  /**
   * Gets the filesystem object type of this descriptor.
   *
   * @return the descriptor type
   * @throws WasmException if type cannot be determined
   * @throws IllegalStateException if descriptor is not valid
   */
  DescriptorType getDescriptorType() throws WasmException;

  /**
   * Gets the flags associated with this descriptor.
   *
   * @return the descriptor flags
   * @throws WasmException if flags cannot be retrieved
   * @throws IllegalStateException if descriptor is not valid
   */
  Set<DescriptorFlags> getFlags() throws WasmException;

  /**
   * Sets the size of the file referred to by this descriptor.
   *
   * <p>If the new size is larger than the current size, the file is extended with zeros. If
   * smaller, it is truncated.
   *
   * @param size the new file size in bytes
   * @throws WasmException if size cannot be set
   * @throws IllegalArgumentException if size is negative or descriptor is not a regular file
   * @throws IllegalStateException if descriptor is not valid or not writable
   */
  void setSize(final long size) throws WasmException;

  /**
   * Synchronizes file data to the underlying storage device.
   *
   * <p>This ensures that data written to the file is committed to permanent storage, but does not
   * necessarily synchronize metadata.
   *
   * @throws WasmException if synchronization fails
   * @throws IllegalStateException if descriptor is not valid
   */
  void syncData() throws WasmException;

  /**
   * Synchronizes both file data and metadata to the underlying storage device.
   *
   * <p>This ensures that both data and all metadata (such as timestamps and file size) are
   * committed to permanent storage.
   *
   * @throws WasmException if synchronization fails
   * @throws IllegalStateException if descriptor is not valid
   */
  void sync() throws WasmException;

  /**
   * Opens a filesystem object at the specified path relative to this descriptor.
   *
   * @param path the path relative to this descriptor
   * @param pathFlags flags controlling path resolution
   * @param openFlags flags controlling file creation
   * @param descriptorFlags flags for the new descriptor
   * @return a new descriptor for the opened object
   * @throws WasmException if open operation fails
   * @throws IllegalArgumentException if path is null or empty
   * @throws IllegalStateException if this descriptor is not valid or not a directory
   */
  WasiDescriptor openAt(
      final String path,
      final Set<PathFlags> pathFlags,
      final Set<OpenFlags> openFlags,
      final Set<DescriptorFlags> descriptorFlags)
      throws WasmException;

  /**
   * Creates a directory at the specified path relative to this descriptor.
   *
   * @param path the path for the new directory
   * @throws WasmException if directory creation fails
   * @throws IllegalArgumentException if path is null or empty
   * @throws IllegalStateException if this descriptor is not valid or not a directory
   */
  void createDirectoryAt(final String path) throws WasmException;

  /**
   * Reads the entries in this directory.
   *
   * <p>Returns a list of directory entries. The order is not specified and may vary between calls.
   *
   * @return list of directory entry names
   * @throws WasmException if directory cannot be read
   * @throws IllegalStateException if descriptor is not valid or not a directory
   */
  List<String> readDirectory() throws WasmException;

  /**
   * Reads the target of a symbolic link at the specified path.
   *
   * @param path the path to the symbolic link
   * @return the target path of the symbolic link
   * @throws WasmException if link cannot be read
   * @throws IllegalArgumentException if path is null or empty, or does not refer to a symlink
   * @throws IllegalStateException if this descriptor is not valid or not a directory
   */
  String readLinkAt(final String path) throws WasmException;

  /**
   * Removes a file at the specified path.
   *
   * @param path the path to the file to remove
   * @throws WasmException if file cannot be removed
   * @throws IllegalArgumentException if path is null, empty, or refers to a directory
   * @throws IllegalStateException if this descriptor is not valid or not a directory
   */
  void unlinkFileAt(final String path) throws WasmException;

  /**
   * Removes an empty directory at the specified path.
   *
   * @param path the path to the directory to remove
   * @throws WasmException if directory cannot be removed or is not empty
   * @throws IllegalArgumentException if path is null, empty, or does not refer to a directory
   * @throws IllegalStateException if this descriptor is not valid or not a directory
   */
  void removeDirectoryAt(final String path) throws WasmException;

  /**
   * Renames a filesystem object.
   *
   * @param oldPath the current path relative to this descriptor
   * @param newDescriptor the descriptor for the new parent directory
   * @param newPath the new path relative to newDescriptor
   * @throws WasmException if rename fails
   * @throws IllegalArgumentException if paths are null/empty or newDescriptor is null
   * @throws IllegalStateException if either descriptor is not valid or not a directory
   */
  void renameAt(final String oldPath, final WasiDescriptor newDescriptor, final String newPath)
      throws WasmException;

  /**
   * Creates a symbolic link.
   *
   * @param oldPath the target path for the symbolic link
   * @param newPath the path where the symbolic link will be created
   * @throws WasmException if symbolic link creation fails
   * @throws IllegalArgumentException if paths are null or empty
   * @throws IllegalStateException if this descriptor is not valid or not a directory
   */
  void symlinkAt(final String oldPath, final String newPath) throws WasmException;

  /**
   * Creates a hard link.
   *
   * @param oldPathFlags flags for resolving oldPath
   * @param oldPath the existing filesystem object path
   * @param newDescriptor the descriptor for the new link's parent directory
   * @param newPath the path for the new hard link
   * @throws WasmException if hard link creation fails
   * @throws IllegalArgumentException if paths are null/empty or newDescriptor is null
   * @throws IllegalStateException if either descriptor is not valid or not a directory
   */
  void linkAt(
      final Set<PathFlags> oldPathFlags,
      final String oldPath,
      final WasiDescriptor newDescriptor,
      final String newPath)
      throws WasmException;

  /**
   * Checks if this descriptor and another refer to the same filesystem object.
   *
   * @param other the descriptor to compare with
   * @return true if both descriptors refer to the same object
   * @throws WasmException if comparison fails
   * @throws IllegalArgumentException if other is null
   * @throws IllegalStateException if either descriptor is not valid
   */
  boolean isSameObject(final WasiDescriptor other) throws WasmException;
}
