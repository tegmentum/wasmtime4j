package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.ModuleExport;

/**
 * JNI implementation of {@link ModuleExport}.
 *
 * <p>Wraps a native pointer to a Wasmtime ModuleExport struct for O(1) export lookups.
 *
 * @since 1.1.0
 */
final class JniModuleExport implements ModuleExport {

  private final String name;
  private final long nativeHandle;

  JniModuleExport(final String name, final long nativeHandle) {
    this.name = name;
    this.nativeHandle = nativeHandle;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public long nativeHandle() {
    return nativeHandle;
  }
}
