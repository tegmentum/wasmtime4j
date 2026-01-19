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

package ai.tegmentum.wasmtime4j.jni.wasi.filesystem;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiInputStream;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.filesystem.DescriptorFlags;
import ai.tegmentum.wasmtime4j.wasi.filesystem.DescriptorType;
import ai.tegmentum.wasmtime4j.wasi.filesystem.OpenFlags;
import ai.tegmentum.wasmtime4j.wasi.filesystem.PathFlags;
import ai.tegmentum.wasmtime4j.wasi.filesystem.WasiDescriptor;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiDescriptor interface.
 *
 * <p>This class provides access to WASI Preview 2 filesystem operations through JNI calls to the
 * native Wasmtime library. Descriptors represent filesystem objects like files and directories.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class JniWasiDescriptor extends JniResource implements WasiDescriptor {

  private static final Logger LOGGER = Logger.getLogger(JniWasiDescriptor.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiDescriptor: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI descriptor with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param descriptorHandle the native descriptor handle
   * @throws IllegalArgumentException if either handle is 0
   */
  public JniWasiDescriptor(final long contextHandle, final long descriptorHandle) {
    super(descriptorHandle);
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI descriptor with handle: " + descriptorHandle);
  }

  @Override
  public WasiInputStream readViaStream(final long offset) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }
    ensureNotClosed();

    final long streamHandle = nativeReadViaStream(contextHandle, nativeHandle, offset);
    if (streamHandle == 0) {
      throw new WasmException("Failed to create read stream");
    }
    return new JniWasiInputStream(contextHandle, streamHandle);
  }

  @Override
  public WasiOutputStream writeViaStream(final long offset) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }
    ensureNotClosed();

    final long streamHandle = nativeWriteViaStream(contextHandle, nativeHandle, offset);
    if (streamHandle == 0) {
      throw new WasmException("Failed to create write stream");
    }
    return new JniWasiOutputStream(contextHandle, streamHandle);
  }

  @Override
  public WasiOutputStream appendViaStream() throws WasmException {
    ensureNotClosed();

    final long streamHandle = nativeAppendViaStream(contextHandle, nativeHandle);
    if (streamHandle == 0) {
      throw new WasmException("Failed to create append stream");
    }
    return new JniWasiOutputStream(contextHandle, streamHandle);
  }

  @Override
  public DescriptorType getDescriptorType() throws WasmException {
    ensureNotClosed();

    final int typeValue = nativeGetType(contextHandle, nativeHandle);
    if (typeValue < 0) {
      throw new WasmException("Failed to get descriptor type");
    }
    return decodeDescriptorType(typeValue);
  }

  @Override
  public Set<DescriptorFlags> getFlags() throws WasmException {
    ensureNotClosed();

    final int flagsValue = nativeGetFlags(contextHandle, nativeHandle);
    if (flagsValue < 0) {
      throw new WasmException("Failed to get descriptor flags");
    }
    return decodeDescriptorFlags(flagsValue);
  }

  @Override
  public void setSize(final long size) throws WasmException {
    if (size < 0) {
      throw new IllegalArgumentException("Size cannot be negative");
    }
    ensureNotClosed();

    final int result = nativeSetSize(contextHandle, nativeHandle, size);
    if (result != 0) {
      throw new WasmException("Failed to set descriptor size");
    }
  }

  @Override
  public void syncData() throws WasmException {
    ensureNotClosed();

    final int result = nativeSyncData(contextHandle, nativeHandle);
    if (result != 0) {
      throw new WasmException("Failed to sync descriptor data");
    }
  }

  @Override
  public void sync() throws WasmException {
    ensureNotClosed();

    final int result = nativeSync(contextHandle, nativeHandle);
    if (result != 0) {
      throw new WasmException("Failed to sync descriptor");
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
    JniValidation.requireNonNull(pathFlags, "pathFlags");
    JniValidation.requireNonNull(openFlags, "openFlags");
    JniValidation.requireNonNull(descriptorFlags, "descriptorFlags");
    ensureNotClosed();

    final int pathFlagsValue = encodePathFlags(pathFlags);
    final int openFlagsValue = encodeOpenFlags(openFlags);
    final int descriptorFlagsValue = encodeDescriptorFlags(descriptorFlags);

    final long newDescriptorHandle =
        nativeOpenAt(
            contextHandle,
            nativeHandle,
            path,
            pathFlagsValue,
            openFlagsValue,
            descriptorFlagsValue);
    if (newDescriptorHandle == 0) {
      throw new WasmException("Failed to open file at path: " + path);
    }
    return new JniWasiDescriptor(contextHandle, newDescriptorHandle);
  }

  @Override
  public void createDirectoryAt(final String path) throws WasmException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    ensureNotClosed();

    final int result = nativeCreateDirectoryAt(contextHandle, nativeHandle, path);
    if (result != 0) {
      throw new WasmException("Failed to create directory at path: " + path);
    }
  }

  @Override
  public List<String> readDirectory() throws WasmException {
    ensureNotClosed();

    final String[] entries = nativeReadDirectory(contextHandle, nativeHandle);
    if (entries == null) {
      throw new WasmException("Failed to read directory");
    }

    return java.util.Arrays.asList(entries);
  }

  @Override
  public String readLinkAt(final String path) throws WasmException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    ensureNotClosed();

    final String target = nativeReadLinkAt(contextHandle, nativeHandle, path);
    if (target == null) {
      throw new WasmException("Failed to read symbolic link at path: " + path);
    }
    return target;
  }

  @Override
  public void unlinkFileAt(final String path) throws WasmException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    ensureNotClosed();

    final int result = nativeUnlinkFileAt(contextHandle, nativeHandle, path);
    if (result != 0) {
      throw new WasmException("Failed to unlink file at path: " + path);
    }
  }

  @Override
  public void removeDirectoryAt(final String path) throws WasmException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    ensureNotClosed();

    final int result = nativeRemoveDirectoryAt(contextHandle, nativeHandle, path);
    if (result != 0) {
      throw new WasmException("Failed to remove directory at path: " + path);
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
    JniValidation.requireNonNull(newDescriptor, "newDescriptor");
    ensureNotClosed();

    if (!(newDescriptor instanceof JniWasiDescriptor)) {
      throw new IllegalArgumentException("New descriptor must be a JNI descriptor");
    }
    final JniWasiDescriptor jniNewDescriptor = (JniWasiDescriptor) newDescriptor;

    final int result =
        nativeRenameAt(
            contextHandle, nativeHandle, oldPath, jniNewDescriptor.nativeHandle, newPath);
    if (result != 0) {
      throw new WasmException("Failed to rename from " + oldPath + " to " + newPath);
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
    ensureNotClosed();

    final int result = nativeSymlinkAt(contextHandle, nativeHandle, oldPath, newPath);
    if (result != 0) {
      throw new WasmException("Failed to create symbolic link from " + oldPath + " to " + newPath);
    }
  }

  @Override
  public void linkAt(
      final Set<PathFlags> oldPathFlags,
      final String oldPath,
      final WasiDescriptor newDescriptor,
      final String newPath)
      throws WasmException {
    if (oldPath == null || oldPath.isEmpty()) {
      throw new IllegalArgumentException("Old path cannot be null or empty");
    }
    if (newPath == null || newPath.isEmpty()) {
      throw new IllegalArgumentException("New path cannot be null or empty");
    }
    JniValidation.requireNonNull(oldPathFlags, "oldPathFlags");
    JniValidation.requireNonNull(newDescriptor, "newDescriptor");
    ensureNotClosed();

    if (!(newDescriptor instanceof JniWasiDescriptor)) {
      throw new IllegalArgumentException("New descriptor must be a JNI descriptor");
    }
    final JniWasiDescriptor jniNewDescriptor = (JniWasiDescriptor) newDescriptor;

    final int oldPathFlagsValue = encodePathFlags(oldPathFlags);
    final int result =
        nativeLinkAt(
            contextHandle,
            nativeHandle,
            oldPathFlagsValue,
            oldPath,
            jniNewDescriptor.nativeHandle,
            newPath);
    if (result != 0) {
      throw new WasmException("Failed to create hard link from " + oldPath + " to " + newPath);
    }
  }

  @Override
  public boolean isSameObject(final WasiDescriptor other) throws WasmException {
    JniValidation.requireNonNull(other, "other");
    ensureNotClosed();

    if (!(other instanceof JniWasiDescriptor)) {
      throw new IllegalArgumentException("Other descriptor must be a JNI descriptor");
    }
    final JniWasiDescriptor jniOther = (JniWasiDescriptor) other;

    return nativeIsSameObject(contextHandle, nativeHandle, jniOther.nativeHandle);
  }

  @Override
  public long getId() {
    return nativeHandle;
  }

  @Override
  public String getType() {
    return "wasi:filesystem/descriptor";
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiInstance getOwner() {
    return null; // Instance ownership tracking not yet implemented for WASI descriptors
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
    return null; // Stats not yet implemented for WASI descriptors
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceState getState() {
    return isClosed()
        ? ai.tegmentum.wasmtime4j.wasi.WasiResourceState.CLOSED
        : ai.tegmentum.wasmtime4j.wasi.WasiResourceState.ACTIVE;
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata getMetadata() throws WasmException {
    return null; // Metadata not yet implemented for WASI descriptors
  }

  @Override
  public java.util.Optional<java.time.Instant> getLastAccessedAt() {
    return java.util.Optional.empty(); // Access tracking not yet implemented for WASI descriptors
  }

  @Override
  public java.time.Instant getCreatedAt() {
    return java.time.Instant
        .now(); // Creation time tracking not yet implemented for WASI descriptors
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle createHandle() throws WasmException {
    ensureNotClosed();
    return new ai.tegmentum.wasmtime4j.jni.wasi.JniWasiResourceHandle(this);
  }

  @Override
  public void transferOwnership(final ai.tegmentum.wasmtime4j.wasi.WasiInstance targetInstance)
      throws WasmException {
    ai.tegmentum.wasmtime4j.jni.util.JniValidation.requireNonNull(targetInstance, "targetInstance");
    ensureNotClosed();

    final int result = nativeTransferOwnership(contextHandle, nativeHandle, targetInstance.getId());
    if (result != 0) {
      throw new WasmException("Failed to transfer descriptor ownership");
    }
    LOGGER.fine(
        "Transferred ownership of descriptor "
            + nativeHandle
            + " to instance "
            + targetInstance.getId());
  }

  @Override
  protected void doClose() {
    final int result = nativeClose(contextHandle, nativeHandle);
    if (result != 0) {
      LOGGER.warning("Failed to close descriptor handle: " + nativeHandle);
    }
  }

  @Override
  protected String getResourceType() {
    return "WasiDescriptor";
  }

  // Helper methods for encoding flags

  /** Encodes PathFlags set into an integer bitmask. */
  private static int encodePathFlags(final Set<PathFlags> flags) {
    int value = 0;
    for (final PathFlags flag : flags) {
      value |= (1 << flag.ordinal());
    }
    return value;
  }

  /** Encodes OpenFlags set into an integer bitmask. */
  private static int encodeOpenFlags(final Set<OpenFlags> flags) {
    int value = 0;
    for (final OpenFlags flag : flags) {
      value |= (1 << flag.ordinal());
    }
    return value;
  }

  /** Encodes DescriptorFlags set into an integer bitmask. */
  private static int encodeDescriptorFlags(final Set<DescriptorFlags> flags) {
    int value = 0;
    for (final DescriptorFlags flag : flags) {
      value |= (1 << flag.ordinal());
    }
    return value;
  }

  /** Decodes DescriptorType from an integer value. */
  private static DescriptorType decodeDescriptorType(final int value) {
    final DescriptorType[] types = DescriptorType.values();
    if (value >= 0 && value < types.length) {
      return types[value];
    }
    return DescriptorType.UNKNOWN;
  }

  /** Decodes DescriptorFlags from an integer bitmask. */
  private static Set<DescriptorFlags> decodeDescriptorFlags(final int value) {
    final Set<DescriptorFlags> flags = EnumSet.noneOf(DescriptorFlags.class);
    final DescriptorFlags[] allFlags = DescriptorFlags.values();
    for (int i = 0; i < allFlags.length; i++) {
      if ((value & (1 << i)) != 0) {
        flags.add(allFlags[i]);
      }
    }
    return flags;
  }

  // Native method declarations

  private static native long nativeReadViaStream(
      long contextHandle, long descriptorHandle, long offset);

  private static native long nativeWriteViaStream(
      long contextHandle, long descriptorHandle, long offset);

  private static native long nativeAppendViaStream(long contextHandle, long descriptorHandle);

  private static native int nativeGetType(long contextHandle, long descriptorHandle);

  private static native int nativeGetFlags(long contextHandle, long descriptorHandle);

  private static native int nativeSetSize(long contextHandle, long descriptorHandle, long size);

  private static native int nativeSyncData(long contextHandle, long descriptorHandle);

  private static native int nativeSync(long contextHandle, long descriptorHandle);

  private static native long nativeOpenAt(
      long contextHandle,
      long descriptorHandle,
      String path,
      int pathFlags,
      int openFlags,
      int descriptorFlags);

  private static native int nativeCreateDirectoryAt(
      long contextHandle, long descriptorHandle, String path);

  private static native String[] nativeReadDirectory(long contextHandle, long descriptorHandle);

  private static native String nativeReadLinkAt(
      long contextHandle, long descriptorHandle, String path);

  private static native int nativeUnlinkFileAt(
      long contextHandle, long descriptorHandle, String path);

  private static native int nativeRemoveDirectoryAt(
      long contextHandle, long descriptorHandle, String path);

  private static native int nativeRenameAt(
      long contextHandle,
      long descriptorHandle,
      String oldPath,
      long newDescriptorHandle,
      String newPath);

  private static native int nativeSymlinkAt(
      long contextHandle, long descriptorHandle, String oldPath, String newPath);

  private static native int nativeLinkAt(
      long contextHandle,
      long descriptorHandle,
      int oldPathFlags,
      String oldPath,
      long newDescriptorHandle,
      String newPath);

  private static native boolean nativeIsSameObject(
      long contextHandle, long descriptorHandle, long otherDescriptorHandle);

  private static native int nativeClose(long contextHandle, long descriptorHandle);

  private static native int nativeTransferOwnership(
      long contextHandle, long descriptorHandle, long targetInstanceId);
}
