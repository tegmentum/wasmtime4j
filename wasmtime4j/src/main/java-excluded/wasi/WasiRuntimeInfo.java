package ai.tegmentum.wasmtime4j.wasi;

/**
 * Information about the WASI runtime implementation.
 *
 * <p>This class provides details about the currently active WASI runtime, including the
 * implementation type, version information, and capabilities.
 *
 * @since 1.0.0
 */
public final class WasiRuntimeInfo {

  private final WasiRuntimeType runtimeType;
  private final String version;
  private final String wasmtimeVersion;

  /**
   * Creates a new WASI runtime information instance.
   *
   * @param runtimeType the type of runtime implementation
   * @param version the version of the Java bindings
   * @param wasmtimeVersion the version of the underlying Wasmtime library
   */
  public WasiRuntimeInfo(
      final WasiRuntimeType runtimeType, final String version, final String wasmtimeVersion) {
    this.runtimeType = runtimeType;
    this.version = version;
    this.wasmtimeVersion = wasmtimeVersion;
  }

  /**
   * Gets the type of runtime implementation.
   *
   * @return the runtime type (JNI or Panama)
   */
  public WasiRuntimeType getRuntimeType() {
    return runtimeType;
  }

  /**
   * Gets the version of the Java bindings.
   *
   * @return the binding version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Gets the version of the underlying Wasmtime library.
   *
   * @return the Wasmtime version
   */
  public String getWasmtimeVersion() {
    return wasmtimeVersion;
  }

  @Override
  public String toString() {
    return String.format(
        "WasiRuntimeInfo{type=%s, version=%s, wasmtime=%s}", runtimeType, version, wasmtimeVersion);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasiRuntimeInfo that = (WasiRuntimeInfo) obj;

    if (runtimeType != that.runtimeType) {
      return false;
    }
    if (version != null ? !version.equals(that.version) : that.version != null) {
      return false;
    }
    return wasmtimeVersion != null
        ? wasmtimeVersion.equals(that.wasmtimeVersion)
        : that.wasmtimeVersion == null;
  }

  @Override
  public int hashCode() {
    int result = runtimeType != null ? runtimeType.hashCode() : 0;
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (wasmtimeVersion != null ? wasmtimeVersion.hashCode() : 0);
    return result;
  }
}
