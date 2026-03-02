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
package ai.tegmentum.wasmtime4j.jni.type;

import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JNI implementation of MemoryType interface.
 *
 * <p>This class provides type introspection capabilities for WebAssembly memory types using JNI
 * bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class JniMemoryType implements MemoryType {

  private static final Logger LOGGER = Logger.getLogger(JniMemoryType.class.getName());

  private final long minimum;
  private final Optional<Long> maximum;
  private final boolean is64Bit;
  private final boolean isShared;
  private final int pageSizeLog2;

  /**
   * Creates a new JniMemoryType instance with default page size.
   *
   * @param minimum the minimum number of memory pages
   * @param maximum the maximum number of memory pages (null if unlimited)
   * @param is64Bit true if this is 64-bit addressable memory
   * @param isShared true if this is shared memory
   */
  public JniMemoryType(
      final long minimum, final Long maximum, final boolean is64Bit, final boolean isShared) {
    this(minimum, maximum, is64Bit, isShared, 16);
  }

  /**
   * Creates a new JniMemoryType instance.
   *
   * @param minimum the minimum number of memory pages
   * @param maximum the maximum number of memory pages (null if unlimited)
   * @param is64Bit true if this is 64-bit addressable memory
   * @param isShared true if this is shared memory
   * @param pageSizeLog2 the log2 of the page size (e.g. 16 for 65536 bytes)
   */
  public JniMemoryType(
      final long minimum,
      final Long maximum,
      final boolean is64Bit,
      final boolean isShared,
      final int pageSizeLog2) {
    if (minimum < 0) {
      throw new IllegalArgumentException("Minimum page count cannot be negative: " + minimum);
    }
    if (maximum != null && maximum < minimum) {
      throw new IllegalArgumentException(
          "Maximum page count cannot be less than minimum: " + maximum + " < " + minimum);
    }

    this.minimum = minimum;
    this.maximum = Optional.ofNullable(maximum);
    this.is64Bit = is64Bit;
    this.isShared = isShared;
    this.pageSizeLog2 = pageSizeLog2;

    LOGGER.fine(
        String.format(
            "Created JniMemoryType: min=%d, max=%s, 64bit=%b, shared=%b, pageSizeLog2=%d",
            minimum, maximum, is64Bit, isShared, pageSizeLog2));
  }

  /**
   * Creates a JniMemoryType from native memory type information.
   *
   * @param nativeHandle the native handle to the memory type
   * @return the JniMemoryType instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static JniMemoryType fromNative(final long nativeHandle) {
    Validation.requireValidHandle(nativeHandle, "nativeHandle");

    final long[] typeInfo = nativeGetMemoryTypeInfo(nativeHandle);
    if (typeInfo.length < 5) {
      throw new IllegalStateException("Invalid memory type info from native");
    }

    final long minimum = typeInfo[0];
    final Long maximum = typeInfo[1] == -1 ? null : typeInfo[1];
    final boolean is64Bit = typeInfo[2] != 0;
    final boolean isShared = typeInfo[3] != 0;
    final int pageSizeLog2 = (int) typeInfo[4];

    return new JniMemoryType(minimum, maximum, is64Bit, isShared, pageSizeLog2);
  }

  @Override
  public long getMinimum() {
    return minimum;
  }

  @Override
  public Optional<Long> getMaximum() {
    return maximum;
  }

  @Override
  public boolean is64Bit() {
    return is64Bit;
  }

  @Override
  public boolean isShared() {
    return isShared;
  }

  @Override
  public long getPageSize() {
    return 1L << pageSizeLog2;
  }

  @Override
  public int getPageSizeLog2() {
    return pageSizeLog2;
  }

  @Override
  public WasmTypeKind getKind() {
    return WasmTypeKind.MEMORY;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MemoryType)) {
      return false;
    }

    final MemoryType other = (MemoryType) obj;
    return minimum == other.getMinimum()
        && maximum.equals(other.getMaximum())
        && is64Bit == other.is64Bit()
        && isShared == other.isShared()
        && getPageSize() == other.getPageSize();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(minimum, maximum, is64Bit, isShared, pageSizeLog2);
  }

  @Override
  public String toString() {
    return String.format(
        "MemoryType{min=%d, max=%s, 64bit=%b, shared=%b, pageSize=%d}",
        minimum,
        maximum.map(String::valueOf).orElse("unlimited"),
        is64Bit,
        isShared,
        getPageSize());
  }

  /**
   * Native method to get memory type information.
   *
   * @param nativeHandle the native handle to the memory type
   * @return array containing [minimum, maximum(-1 if unlimited), is64Bit(0/1), isShared(0/1),
   *     pageSizeLog2]
   */
  private static native long[] nativeGetMemoryTypeInfo(long nativeHandle);
}
