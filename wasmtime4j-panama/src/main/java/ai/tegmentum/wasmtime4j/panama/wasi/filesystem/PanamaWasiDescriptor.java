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

package ai.tegmentum.wasmtime4j.panama.wasi.filesystem;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaResource;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiInputStream;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.filesystem.DescriptorFlags;
import ai.tegmentum.wasmtime4j.wasi.filesystem.DescriptorType;
import ai.tegmentum.wasmtime4j.wasi.filesystem.OpenFlags;
import ai.tegmentum.wasmtime4j.wasi.filesystem.PathFlags;
import ai.tegmentum.wasmtime4j.wasi.filesystem.WasiDescriptor;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiDescriptor interface.
 *
 * <p>This class provides access to WASI Preview 2 filesystem operations through Panama Foreign
 * Function API calls to the native Wasmtime library. Descriptors represent filesystem objects
 * like files and directories.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class PanamaWasiDescriptor extends PanamaResource implements WasiDescriptor {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiDescriptor.class.getName());

  // Panama FFI function handles for stream operations
  private static final MethodHandle READ_VIA_STREAM_HANDLE;
  private static final MethodHandle WRITE_VIA_STREAM_HANDLE;
  private static final MethodHandle APPEND_VIA_STREAM_HANDLE;

  // Panama FFI function handles for metadata operations
  private static final MethodHandle GET_TYPE_HANDLE;
  private static final MethodHandle GET_FLAGS_HANDLE;
  private static final MethodHandle SET_SIZE_HANDLE;
  private static final MethodHandle SYNC_DATA_HANDLE;
  private static final MethodHandle SYNC_HANDLE;

  // Panama FFI function handles for directory operations
  private static final MethodHandle OPEN_AT_HANDLE;
  private static final MethodHandle CREATE_DIRECTORY_AT_HANDLE;
  private static final MethodHandle READ_DIRECTORY_HANDLE;
  private static final MethodHandle READ_LINK_AT_HANDLE;

  // Panama FFI function handles for file operations
  private static final MethodHandle UNLINK_FILE_AT_HANDLE;
  private static final MethodHandle REMOVE_DIRECTORY_AT_HANDLE;

  // Panama FFI function handles for path operations
  private static final MethodHandle RENAME_AT_HANDLE;
  private static final MethodHandle SYMLINK_AT_HANDLE;
  private static final MethodHandle LINK_AT_HANDLE;

  // Panama FFI function handles for utility operations
  private static final MethodHandle IS_SAME_OBJECT_HANDLE;
  private static final MethodHandle CLOSE_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = PanamaResource.getNativeLibrary();
      final Linker linker = Linker.nativeLinker();

      // Stream operations
      READ_VIA_STREAM_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_read_via_stream").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      WRITE_VIA_STREAM_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_write_via_stream").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      APPEND_VIA_STREAM_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_descriptor_append_via_stream")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // Metadata operations
      GET_TYPE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_get_type").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      GET_FLAGS_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_get_flags").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      SET_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_set_size").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG));

      SYNC_DATA_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_sync_data").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      SYNC_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_sync").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // Directory operations
      OPEN_AT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_open_at").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.JAVA_INT,
                  ValueLayout.JAVA_INT,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS));

      CREATE_DIRECTORY_AT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_create_directory_at").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT));

      READ_DIRECTORY_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_read_directory").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      READ_LINK_AT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_read_link_at").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // File operations
      UNLINK_FILE_AT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_unlink_file_at").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT));

      REMOVE_DIRECTORY_AT_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_descriptor_remove_directory_at")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT));

      // Path operations
      RENAME_AT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_rename_at").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT));

      SYMLINK_AT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_symlink_at").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT));

      LINK_AT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_link_at").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT));

      // Utility operations
      IS_SAME_OBJECT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_is_same_object").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      CLOSE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_descriptor_close").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiDescriptor: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI descriptor with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param descriptorHandle the native descriptor handle
   * @throws IllegalArgumentException if either handle is null
   */
  public PanamaWasiDescriptor(
      final MemorySegment contextHandle, final MemorySegment descriptorHandle) {
    super(descriptorHandle);
    PanamaValidation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI descriptor with handle: " + descriptorHandle);
  }

  @Override
  public WasiInputStream readViaStream(final long offset) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative: " + offset);
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outStreamHandle = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int) READ_VIA_STREAM_HANDLE.invoke(contextHandle, nativeHandle, offset, outStreamHandle);

      if (result != 0) {
        throw new WasmException("Failed to create read stream for descriptor");
      }

      final MemorySegment streamHandle = outStreamHandle.get(ValueLayout.ADDRESS, 0);
      if (streamHandle == null || streamHandle.address() == 0) {
        throw new WasmException("Failed to create read stream (null handle returned)");
      }

      return new PanamaWasiInputStream(contextHandle, streamHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating read stream: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiOutputStream writeViaStream(final long offset) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative: " + offset);
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outStreamHandle = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              WRITE_VIA_STREAM_HANDLE.invoke(contextHandle, nativeHandle, offset, outStreamHandle);

      if (result != 0) {
        throw new WasmException("Failed to create write stream for descriptor");
      }

      final MemorySegment streamHandle = outStreamHandle.get(ValueLayout.ADDRESS, 0);
      if (streamHandle == null || streamHandle.address() == 0) {
        throw new WasmException("Failed to create write stream (null handle returned)");
      }

      return new PanamaWasiOutputStream(contextHandle, streamHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating write stream: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiOutputStream appendViaStream() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outStreamHandle = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int) APPEND_VIA_STREAM_HANDLE.invoke(contextHandle, nativeHandle, outStreamHandle);

      if (result != 0) {
        throw new WasmException("Failed to create append stream for descriptor");
      }

      final MemorySegment streamHandle = outStreamHandle.get(ValueLayout.ADDRESS, 0);
      if (streamHandle == null || streamHandle.address() == 0) {
        throw new WasmException("Failed to create append stream (null handle returned)");
      }

      return new PanamaWasiOutputStream(contextHandle, streamHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating append stream: " + e.getMessage(), e);
    }
  }

  @Override
  public DescriptorType getDescriptorType() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outType = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) GET_TYPE_HANDLE.invoke(contextHandle, nativeHandle, outType);

      if (result != 0) {
        throw new WasmException("Failed to get descriptor type");
      }

      final int typeValue = outType.get(ValueLayout.JAVA_INT, 0);
      return DescriptorType.values()[typeValue];

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error getting descriptor type: " + e.getMessage(), e);
    }
  }

  @Override
  public Set<DescriptorFlags> getFlags() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outFlags = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) GET_FLAGS_HANDLE.invoke(contextHandle, nativeHandle, outFlags);

      if (result != 0) {
        throw new WasmException("Failed to get descriptor flags");
      }

      final int flagsValue = outFlags.get(ValueLayout.JAVA_INT, 0);
      return decodeDescriptorFlags(flagsValue);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error getting descriptor flags: " + e.getMessage(), e);
    }
  }

  @Override
  public void setSize(final long size) throws WasmException {
    if (size < 0) {
      throw new IllegalArgumentException("Size cannot be negative: " + size);
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try {
      final int result = (int) SET_SIZE_HANDLE.invoke(contextHandle, nativeHandle, size);

      if (result != 0) {
        throw new WasmException("Failed to set file size");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error setting file size: " + e.getMessage(), e);
    }
  }

  @Override
  public void syncData() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try {
      final int result = (int) SYNC_DATA_HANDLE.invoke(contextHandle, nativeHandle);

      if (result != 0) {
        throw new WasmException("Failed to sync data");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error syncing data: " + e.getMessage(), e);
    }
  }

  @Override
  public void sync() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try {
      final int result = (int) SYNC_HANDLE.invoke(contextHandle, nativeHandle);

      if (result != 0) {
        throw new WasmException("Failed to sync");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error syncing: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiDescriptor openAt(
      final String path,
      final Set<PathFlags> pathFlags,
      final Set<OpenFlags> openFlags,
      final Set<DescriptorFlags> descriptorFlags)
      throws WasmException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    PanamaValidation.requireNonNull(pathFlags, "pathFlags");
    PanamaValidation.requireNonNull(openFlags, "openFlags");
    PanamaValidation.requireNonNull(descriptorFlags, "descriptorFlags");

    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] pathBytes = path.getBytes(StandardCharsets.UTF_8);
      final MemorySegment pathSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);
      final MemorySegment outDescriptorHandle = arena.allocate(ValueLayout.ADDRESS);

      final int pathFlagsValue = encodePathFlags(pathFlags);
      final int openFlagsValue = encodeOpenFlags(openFlags);
      final int descriptorFlagsValue = encodeDescriptorFlags(descriptorFlags);

      final int result =
          (int)
              OPEN_AT_HANDLE.invoke(
                  contextHandle,
                  nativeHandle,
                  pathSegment,
                  pathBytes.length,
                  pathFlagsValue,
                  openFlagsValue,
                  descriptorFlagsValue,
                  outDescriptorHandle);

      if (result != 0) {
        throw new WasmException("Failed to open file at path: " + path);
      }

      final MemorySegment descriptorHandle = outDescriptorHandle.get(ValueLayout.ADDRESS, 0);
      if (descriptorHandle == null || descriptorHandle.address() == 0) {
        throw new WasmException("Failed to open file (null handle returned)");
      }

      return new PanamaWasiDescriptor(contextHandle, descriptorHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error opening file at path: " + e.getMessage(), e);
    }
  }

  @Override
  public void createDirectoryAt(final String path) throws WasmException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] pathBytes = path.getBytes(StandardCharsets.UTF_8);
      final MemorySegment pathSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);

      final int result =
          (int)
              CREATE_DIRECTORY_AT_HANDLE.invoke(
                  contextHandle, nativeHandle, pathSegment, pathBytes.length);

      if (result != 0) {
        throw new WasmException("Failed to create directory at path: " + path);
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating directory: " + e.getMessage(), e);
    }
  }

  @Override
  public List<String> readDirectory() throws WasmException {
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outEntries = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outEntriesLen = arena.allocate(ValueLayout.JAVA_INT);

      final int result =
          (int)
              READ_DIRECTORY_HANDLE.invoke(
                  contextHandle, nativeHandle, outEntries, outEntriesLen);

      if (result != 0) {
        throw new WasmException("Failed to read directory");
      }

      final MemorySegment entriesPtr = outEntries.get(ValueLayout.ADDRESS, 0);
      final int entriesLen = outEntriesLen.get(ValueLayout.JAVA_INT, 0);

      if (entriesPtr == null || entriesPtr.address() == 0 || entriesLen == 0) {
        return new ArrayList<>();
      }

      // Parse null-terminated entry names from the buffer
      final List<String> entries = new ArrayList<>();
      int offset = 0;
      while (offset < entriesLen) {
        final String entry = entriesPtr.getUtf8String(offset);
        if (entry.isEmpty()) {
          break;
        }
        entries.add(entry);
        offset += entry.getBytes(StandardCharsets.UTF_8).length + 1; // +1 for null terminator
      }

      return entries;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error reading directory: " + e.getMessage(), e);
    }
  }

  @Override
  public String readLinkAt(final String path) throws WasmException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] pathBytes = path.getBytes(StandardCharsets.UTF_8);
      final MemorySegment pathSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);
      final MemorySegment outTarget = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outTargetLen = arena.allocate(ValueLayout.JAVA_INT);

      final int result =
          (int)
              READ_LINK_AT_HANDLE.invoke(
                  contextHandle,
                  nativeHandle,
                  pathSegment,
                  pathBytes.length,
                  outTarget,
                  outTargetLen);

      if (result != 0) {
        throw new WasmException("Failed to read symbolic link at path: " + path);
      }

      final MemorySegment targetPtr = outTarget.get(ValueLayout.ADDRESS, 0);
      final int targetLen = outTargetLen.get(ValueLayout.JAVA_INT, 0);

      if (targetPtr == null || targetPtr.address() == 0 || targetLen == 0) {
        throw new WasmException("Failed to read symbolic link (null target returned)");
      }

      return targetPtr.getUtf8String(0);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error reading symbolic link: " + e.getMessage(), e);
    }
  }

  @Override
  public void unlinkFileAt(final String path) throws WasmException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] pathBytes = path.getBytes(StandardCharsets.UTF_8);
      final MemorySegment pathSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);

      final int result =
          (int)
              UNLINK_FILE_AT_HANDLE.invoke(
                  contextHandle, nativeHandle, pathSegment, pathBytes.length);

      if (result != 0) {
        throw new WasmException("Failed to unlink file at path: " + path);
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error unlinking file: " + e.getMessage(), e);
    }
  }

  @Override
  public void removeDirectoryAt(final String path) throws WasmException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] pathBytes = path.getBytes(StandardCharsets.UTF_8);
      final MemorySegment pathSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);

      final int result =
          (int)
              REMOVE_DIRECTORY_AT_HANDLE.invoke(
                  contextHandle, nativeHandle, pathSegment, pathBytes.length);

      if (result != 0) {
        throw new WasmException("Failed to remove directory at path: " + path);
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error removing directory: " + e.getMessage(), e);
    }
  }

  @Override
  public void renameAt(
      final String oldPath, final WasiDescriptor newDescriptor, final String newPath)
      throws WasmException {
    if (oldPath == null || oldPath.isEmpty()) {
      throw new IllegalArgumentException("Old path cannot be null or empty");
    }
    if (newPath == null || newPath.isEmpty()) {
      throw new IllegalArgumentException("New path cannot be null or empty");
    }
    if (newDescriptor == null) {
      throw new IllegalArgumentException("New descriptor cannot be null");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    if (!(newDescriptor instanceof PanamaWasiDescriptor)) {
      throw new IllegalArgumentException(
          "New descriptor must be a PanamaWasiDescriptor, got: "
              + newDescriptor.getClass().getName());
    }

    final MemorySegment newDescriptorHandle;
    try {
      newDescriptorHandle = ((PanamaWasiDescriptor) newDescriptor).getNativeHandle();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("New descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] oldPathBytes = oldPath.getBytes(StandardCharsets.UTF_8);
      final MemorySegment oldPathSegment =
          arena.allocateArray(ValueLayout.JAVA_BYTE, oldPathBytes);

      final byte[] newPathBytes = newPath.getBytes(StandardCharsets.UTF_8);
      final MemorySegment newPathSegment =
          arena.allocateArray(ValueLayout.JAVA_BYTE, newPathBytes);

      final int result =
          (int)
              RENAME_AT_HANDLE.invoke(
                  contextHandle,
                  nativeHandle,
                  oldPathSegment,
                  oldPathBytes.length,
                  newDescriptorHandle,
                  newPathSegment,
                  newPathBytes.length);

      if (result != 0) {
        throw new WasmException("Failed to rename from " + oldPath + " to " + newPath);
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error renaming: " + e.getMessage(), e);
    }
  }

  @Override
  public void symlinkAt(final String oldPath, final String newPath) throws WasmException {
    if (oldPath == null || oldPath.isEmpty()) {
      throw new IllegalArgumentException("Old path cannot be null or empty");
    }
    if (newPath == null || newPath.isEmpty()) {
      throw new IllegalArgumentException("New path cannot be null or empty");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] oldPathBytes = oldPath.getBytes(StandardCharsets.UTF_8);
      final MemorySegment oldPathSegment =
          arena.allocateArray(ValueLayout.JAVA_BYTE, oldPathBytes);

      final byte[] newPathBytes = newPath.getBytes(StandardCharsets.UTF_8);
      final MemorySegment newPathSegment =
          arena.allocateArray(ValueLayout.JAVA_BYTE, newPathBytes);

      final int result =
          (int)
              SYMLINK_AT_HANDLE.invoke(
                  contextHandle,
                  nativeHandle,
                  oldPathSegment,
                  oldPathBytes.length,
                  newPathSegment,
                  newPathBytes.length);

      if (result != 0) {
        throw new WasmException("Failed to create symbolic link from " + oldPath + " to " + newPath);
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating symbolic link: " + e.getMessage(), e);
    }
  }

  @Override
  public void linkAt(
      final Set<PathFlags> oldPathFlags,
      final String oldPath,
      final WasiDescriptor newDescriptor,
      final String newPath)
      throws WasmException {
    PanamaValidation.requireNonNull(oldPathFlags, "oldPathFlags");
    if (oldPath == null || oldPath.isEmpty()) {
      throw new IllegalArgumentException("Old path cannot be null or empty");
    }
    if (newPath == null || newPath.isEmpty()) {
      throw new IllegalArgumentException("New path cannot be null or empty");
    }
    if (newDescriptor == null) {
      throw new IllegalArgumentException("New descriptor cannot be null");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    if (!(newDescriptor instanceof PanamaWasiDescriptor)) {
      throw new IllegalArgumentException(
          "New descriptor must be a PanamaWasiDescriptor, got: "
              + newDescriptor.getClass().getName());
    }

    final MemorySegment newDescriptorHandle;
    try {
      newDescriptorHandle = ((PanamaWasiDescriptor) newDescriptor).getNativeHandle();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("New descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] oldPathBytes = oldPath.getBytes(StandardCharsets.UTF_8);
      final MemorySegment oldPathSegment =
          arena.allocateArray(ValueLayout.JAVA_BYTE, oldPathBytes);

      final byte[] newPathBytes = newPath.getBytes(StandardCharsets.UTF_8);
      final MemorySegment newPathSegment =
          arena.allocateArray(ValueLayout.JAVA_BYTE, newPathBytes);

      final int oldPathFlagsValue = encodePathFlags(oldPathFlags);

      final int result =
          (int)
              LINK_AT_HANDLE.invoke(
                  contextHandle,
                  nativeHandle,
                  oldPathFlagsValue,
                  oldPathSegment,
                  oldPathBytes.length,
                  newDescriptorHandle,
                  newPathSegment,
                  newPathBytes.length);

      if (result != 0) {
        throw new WasmException("Failed to create hard link from " + oldPath + " to " + newPath);
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating hard link: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isSameObject(final WasiDescriptor other) throws WasmException {
    if (other == null) {
      throw new IllegalArgumentException("Other descriptor cannot be null");
    }
    try {
      ensureNotClosed();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Descriptor is closed: " + e.getMessage(), e);
    }

    if (!(other instanceof PanamaWasiDescriptor)) {
      throw new IllegalArgumentException(
          "Other descriptor must be a PanamaWasiDescriptor, got: " + other.getClass().getName());
    }

    final MemorySegment otherHandle;
    try {
      otherHandle = ((PanamaWasiDescriptor) other).getNativeHandle();
    } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException e) {
      throw new WasmException("Other descriptor is closed: " + e.getMessage(), e);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outSame = arena.allocate(ValueLayout.JAVA_INT);

      final int result =
          (int) IS_SAME_OBJECT_HANDLE.invoke(contextHandle, nativeHandle, otherHandle, outSame);

      if (result != 0) {
        throw new WasmException("Failed to compare descriptors");
      }

      final int same = outSame.get(ValueLayout.JAVA_INT, 0);
      return same != 0;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error comparing descriptors: " + e.getMessage(), e);
    }
  }

  @Override
  public long getId() {
    return nativeHandle.address();
  }

  @Override
  public String getType() {
    return "wasi:filesystem/descriptor";
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiInstance getOwner() {
    return null; // Instance ownership tracking not yet implemented for WASI filesystem descriptors
  }

  @Override
  public boolean isOwned() {
    return true; // WASI descriptors are owned by default
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  public java.util.List<String> getAvailableOperations() {
    return java.util.Arrays.asList(
        "read-via-stream",
        "write-via-stream",
        "append-via-stream",
        "get-type",
        "get-flags",
        "set-size",
        "sync-data",
        "sync",
        "open-at",
        "create-directory-at",
        "read-directory",
        "read-link-at",
        "unlink-file-at",
        "remove-directory-at",
        "rename-at",
        "symlink-at",
        "link-at",
        "is-same-object");
  }

  @Override
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    throw new UnsupportedOperationException(
        "Generic invoke not supported for WASI descriptors - use dedicated methods");
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceStats getStats() {
    return null; // Stats not yet implemented for WASI filesystem descriptors
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceState getState() {
    return isClosed()
        ? ai.tegmentum.wasmtime4j.wasi.WasiResourceState.CLOSED
        : ai.tegmentum.wasmtime4j.wasi.WasiResourceState.ACTIVE;
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata getMetadata() throws WasmException {
    return null; // Metadata not yet implemented for WASI filesystem descriptors
  }

  @Override
  public java.util.Optional<java.time.Instant> getLastAccessedAt() {
    return java.util
        .Optional
        .empty(); // Access tracking not yet implemented for WASI filesystem descriptors
  }

  @Override
  public java.time.Instant getCreatedAt() {
    return java.time.Instant
        .now(); // Creation time tracking not yet implemented for WASI filesystem descriptors
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle createHandle() throws WasmException {
    throw new UnsupportedOperationException(
        "Resource handle creation not yet implemented for WASI descriptors");
  }

  @Override
  public void transferOwnership(final ai.tegmentum.wasmtime4j.wasi.WasiInstance targetInstance)
      throws WasmException {
    throw new UnsupportedOperationException(
        "Ownership transfer not yet implemented for WASI descriptors");
  }

  @Override
  protected void doClose() throws Exception {
    try {
      final int result = (int) CLOSE_HANDLE.invoke(contextHandle, nativeHandle);
      if (result != 0) {
        LOGGER.warning("Failed to close WASI descriptor (error code: " + result + ")");
      }
    } catch (final Throwable e) {
      throw new Exception("Error closing WASI descriptor", e);
    }
  }

  @Override
  protected String getResourceType() {
    return "WasiDescriptor";
  }

  // Helper methods for encoding/decoding flag sets

  private static int encodePathFlags(final Set<PathFlags> flags) {
    int value = 0;
    for (final PathFlags flag : flags) {
      value |= (1 << flag.ordinal());
    }
    return value;
  }

  private static int encodeOpenFlags(final Set<OpenFlags> flags) {
    int value = 0;
    for (final OpenFlags flag : flags) {
      value |= (1 << flag.ordinal());
    }
    return value;
  }

  private static int encodeDescriptorFlags(final Set<DescriptorFlags> flags) {
    int value = 0;
    for (final DescriptorFlags flag : flags) {
      value |= (1 << flag.ordinal());
    }
    return value;
  }

  private static Set<DescriptorFlags> decodeDescriptorFlags(final int value) {
    final Set<DescriptorFlags> flags = EnumSet.noneOf(DescriptorFlags.class);
    for (final DescriptorFlags flag : DescriptorFlags.values()) {
      if ((value & (1 << flag.ordinal())) != 0) {
        flags.add(flag);
      }
    }
    return flags;
  }
}
