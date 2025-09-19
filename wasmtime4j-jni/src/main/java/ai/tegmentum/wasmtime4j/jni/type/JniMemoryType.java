package ai.tegmentum.wasmtime4j.jni.type;

import ai.tegmentum.wasmtime4j.MemoryType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
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

  /**
   * Creates a new JniMemoryType instance.
   *
   * @param minimum the minimum number of memory pages
   * @param maximum the maximum number of memory pages (null if unlimited)
   * @param is64Bit true if this is 64-bit addressable memory
   * @param isShared true if this is shared memory
   */
  public JniMemoryType(
      final long minimum, final Long maximum, final boolean is64Bit, final boolean isShared) {
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

    LOGGER.fine(
        String.format(
            "Created JniMemoryType: min=%d, max=%s, 64bit=%b, shared=%b",
            minimum, maximum, is64Bit, isShared));
  }

  /**
   * Creates a JniMemoryType from native memory type information.
   *
   * @param nativeHandle the native handle to the memory type
   * @return the JniMemoryType instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static JniMemoryType fromNative(final long nativeHandle) {
    JniValidation.requireValidHandle(nativeHandle, "nativeHandle");

    final long[] typeInfo = nativeGetMemoryTypeInfo(nativeHandle);
    if (typeInfo.length < 4) {
      throw new IllegalStateException("Invalid memory type info from native");
    }

    final long minimum = typeInfo[0];
    final Long maximum = typeInfo[1] == -1 ? null : typeInfo[1];
    final boolean is64Bit = typeInfo[2] != 0;
    final boolean isShared = typeInfo[3] != 0;

    return new JniMemoryType(minimum, maximum, is64Bit, isShared);
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
        && isShared == other.isShared();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(minimum, maximum, is64Bit, isShared);
  }

  @Override
  public String toString() {
    return String.format(
        "MemoryType{min=%d, max=%s, 64bit=%b, shared=%b}",
        minimum, maximum.map(String::valueOf).orElse("unlimited"), is64Bit, isShared);
  }

  /**
   * Native method to get memory type information.
   *
   * @param nativeHandle the native handle to the memory type
   * @return array containing [minimum, maximum(-1 if unlimited), is64Bit(0/1), isShared(0/1)]
   */
  private static native long[] nativeGetMemoryTypeInfo(long nativeHandle);
}
