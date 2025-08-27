package ai.tegmentum.wasmtime4j;

/**
 * Information about the WebAssembly runtime implementation.
 * 
 * <p>This class provides metadata about the runtime, including version information,
 * implementation type, and supported features.
 * 
 * @since 1.0.0
 */
public final class RuntimeInfo {
    
    private final String runtimeName;
    private final String runtimeVersion;
    private final String wasmtimeVersion;
    private final RuntimeType runtimeType;
    private final String javaVersion;
    private final String platformInfo;
    
    /**
     * Creates a new runtime information instance.
     * 
     * @param runtimeName the name of the runtime implementation
     * @param runtimeVersion the version of the runtime implementation
     * @param wasmtimeVersion the version of the underlying Wasmtime library
     * @param runtimeType the type of runtime (JNI or Panama)
     * @param javaVersion the Java version being used
     * @param platformInfo information about the platform
     */
    public RuntimeInfo(final String runtimeName,
                      final String runtimeVersion,
                      final String wasmtimeVersion,
                      final RuntimeType runtimeType,
                      final String javaVersion,
                      final String platformInfo) {
        this.runtimeName = runtimeName;
        this.runtimeVersion = runtimeVersion;
        this.wasmtimeVersion = wasmtimeVersion;
        this.runtimeType = runtimeType;
        this.javaVersion = javaVersion;
        this.platformInfo = platformInfo;
    }
    
    /**
     * Gets the name of the runtime implementation.
     * 
     * @return the runtime name
     */
    public String getRuntimeName() {
        return runtimeName;
    }
    
    /**
     * Gets the version of the runtime implementation.
     * 
     * @return the runtime version
     */
    public String getRuntimeVersion() {
        return runtimeVersion;
    }
    
    /**
     * Gets the version of the underlying Wasmtime library.
     * 
     * @return the Wasmtime version
     */
    public String getWasmtimeVersion() {
        return wasmtimeVersion;
    }
    
    /**
     * Gets the type of runtime implementation.
     * 
     * @return the runtime type
     */
    public RuntimeType getRuntimeType() {
        return runtimeType;
    }
    
    /**
     * Gets the Java version being used.
     * 
     * @return the Java version
     */
    public String getJavaVersion() {
        return javaVersion;
    }
    
    /**
     * Gets information about the platform.
     * 
     * @return the platform information
     */
    public String getPlatformInfo() {
        return platformInfo;
    }
    
    @Override
    public String toString() {
        return String.format("RuntimeInfo{name='%s', version='%s', wasmtime='%s', type=%s, java='%s', platform='%s'}",
                runtimeName, runtimeVersion, wasmtimeVersion, runtimeType, javaVersion, platformInfo);
    }
}