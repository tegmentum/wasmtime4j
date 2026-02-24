package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ModuleExport;
import java.lang.foreign.MemorySegment;

/**
 * Panama FFI implementation of {@link ModuleExport}.
 *
 * <p>Wraps a native pointer to a Wasmtime ModuleExport for O(1) export lookups. The native handle
 * must be destroyed when no longer needed.
 *
 * @since 1.1.0
 */
final class PanamaModuleExport implements ModuleExport {

  private final String name;
  private final MemorySegment nativePtr;

  PanamaModuleExport(final String name, final MemorySegment nativePtr) {
    this.name = name;
    this.nativePtr = nativePtr;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public long nativeHandle() {
    return nativePtr.address();
  }

  /**
   * Gets the native memory segment pointer.
   *
   * @return the native pointer
   */
  MemorySegment getNativePtr() {
    return nativePtr;
  }
}
