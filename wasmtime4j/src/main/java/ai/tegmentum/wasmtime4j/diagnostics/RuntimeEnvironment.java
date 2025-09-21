package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.Map;
import java.util.Properties;

/**
 * Runtime environment information for WebAssembly diagnostics.
 *
 * <p>This interface provides comprehensive information about the runtime environment
 * when an error occurred, including JVM, OS, and WebAssembly runtime details.
 *
 * @since 1.0.0
 */
public interface RuntimeEnvironment {

    /**
     * Gets the Java Virtual Machine information.
     *
     * @return the JVM information
     */
    JvmInfo getJvmInfo();

    /**
     * Gets the operating system information.
     *
     * @return the OS information
     */
    OsInfo getOsInfo();

    /**
     * Gets the WebAssembly runtime information.
     *
     * @return the WebAssembly runtime information
     */
    WasmRuntimeInfo getWasmRuntimeInfo();

    /**
     * Gets the system properties.
     *
     * @return the system properties
     */
    Properties getSystemProperties();

    /**
     * Gets the environment variables.
     *
     * @return the environment variables
     */
    Map<String, String> getEnvironmentVariables();

    /**
     * Gets additional runtime properties.
     *
     * @return map of runtime properties
     */
    Map<String, Object> getProperties();

    /**
     * Gets the runtime environment collection timestamp.
     *
     * @return the collection timestamp
     */
    long getCollectionTimestamp();

    /**
     * Creates a RuntimeEnvironment snapshot of the current environment.
     *
     * @return the current runtime environment
     */
    static RuntimeEnvironment snapshot() {
        return new RuntimeEnvironmentImpl(
            JvmInfo.current(),
            OsInfo.current(),
            WasmRuntimeInfo.current(),
            System.currentTimeMillis()
        );
    }

    /**
     * JVM information interface.
     */
    interface JvmInfo {
        String getVendor();
        String getVersion();
        String getName();
        String getSpecVersion();
        long getMaxMemory();
        long getTotalMemory();
        long getFreeMemory();
        int getAvailableProcessors();

        static JvmInfo current() {
            final Runtime runtime = Runtime.getRuntime();
            return new JvmInfoImpl(
                System.getProperty("java.vm.vendor"),
                System.getProperty("java.vm.version"),
                System.getProperty("java.vm.name"),
                System.getProperty("java.specification.version"),
                runtime.maxMemory(),
                runtime.totalMemory(),
                runtime.freeMemory(),
                runtime.availableProcessors()
            );
        }
    }

    /**
     * Operating system information interface.
     */
    interface OsInfo {
        String getName();
        String getVersion();
        String getArch();

        static OsInfo current() {
            return new OsInfoImpl(
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch")
            );
        }
    }

    /**
     * WebAssembly runtime information interface.
     */
    interface WasmRuntimeInfo {
        String getRuntimeType();
        String getVersion();
        boolean isJniEnabled();
        boolean isPanamaEnabled();

        static WasmRuntimeInfo current() {
            return new WasmRuntimeInfoImpl(
                "wasmtime4j",
                "1.0.0",
                true, // Assume JNI is available
                isJava23OrLater() // Panama available on Java 23+
            );
        }

        private static boolean isJava23OrLater() {
            try {
                final String version = System.getProperty("java.specification.version");
                final int majorVersion = Integer.parseInt(version);
                return majorVersion >= 23;
            } catch (final NumberFormatException e) {
                return false;
            }
        }
    }

    /**
     * Simple implementations.
     */
    final class RuntimeEnvironmentImpl implements RuntimeEnvironment {
        private final JvmInfo jvmInfo;
        private final OsInfo osInfo;
        private final WasmRuntimeInfo wasmRuntimeInfo;
        private final long collectionTimestamp;

        public RuntimeEnvironmentImpl(final JvmInfo jvmInfo, final OsInfo osInfo,
                                     final WasmRuntimeInfo wasmRuntimeInfo,
                                     final long collectionTimestamp) {
            this.jvmInfo = jvmInfo;
            this.osInfo = osInfo;
            this.wasmRuntimeInfo = wasmRuntimeInfo;
            this.collectionTimestamp = collectionTimestamp;
        }

        @Override
        public JvmInfo getJvmInfo() {
            return jvmInfo;
        }

        @Override
        public OsInfo getOsInfo() {
            return osInfo;
        }

        @Override
        public WasmRuntimeInfo getWasmRuntimeInfo() {
            return wasmRuntimeInfo;
        }

        @Override
        public Properties getSystemProperties() {
            return System.getProperties();
        }

        @Override
        public Map<String, String> getEnvironmentVariables() {
            return System.getenv();
        }

        @Override
        public Map<String, Object> getProperties() {
            return Map.of(
                "jvmInfo", jvmInfo,
                "osInfo", osInfo,
                "wasmRuntimeInfo", wasmRuntimeInfo,
                "collectionTimestamp", collectionTimestamp
            );
        }

        @Override
        public long getCollectionTimestamp() {
            return collectionTimestamp;
        }
    }

    final class JvmInfoImpl implements JvmInfo {
        private final String vendor;
        private final String version;
        private final String name;
        private final String specVersion;
        private final long maxMemory;
        private final long totalMemory;
        private final long freeMemory;
        private final int availableProcessors;

        public JvmInfoImpl(final String vendor, final String version, final String name,
                          final String specVersion, final long maxMemory, final long totalMemory,
                          final long freeMemory, final int availableProcessors) {
            this.vendor = vendor;
            this.version = version;
            this.name = name;
            this.specVersion = specVersion;
            this.maxMemory = maxMemory;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.availableProcessors = availableProcessors;
        }

        @Override
        public String getVendor() {
            return vendor;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getSpecVersion() {
            return specVersion;
        }

        @Override
        public long getMaxMemory() {
            return maxMemory;
        }

        @Override
        public long getTotalMemory() {
            return totalMemory;
        }

        @Override
        public long getFreeMemory() {
            return freeMemory;
        }

        @Override
        public int getAvailableProcessors() {
            return availableProcessors;
        }
    }

    final class OsInfoImpl implements OsInfo {
        private final String name;
        private final String version;
        private final String arch;

        public OsInfoImpl(final String name, final String version, final String arch) {
            this.name = name;
            this.version = version;
            this.arch = arch;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getArch() {
            return arch;
        }
    }

    final class WasmRuntimeInfoImpl implements WasmRuntimeInfo {
        private final String runtimeType;
        private final String version;
        private final boolean jniEnabled;
        private final boolean panamaEnabled;

        public WasmRuntimeInfoImpl(final String runtimeType, final String version,
                                  final boolean jniEnabled, final boolean panamaEnabled) {
            this.runtimeType = runtimeType;
            this.version = version;
            this.jniEnabled = jniEnabled;
            this.panamaEnabled = panamaEnabled;
        }

        @Override
        public String getRuntimeType() {
            return runtimeType;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public boolean isJniEnabled() {
            return jniEnabled;
        }

        @Override
        public boolean isPanamaEnabled() {
            return panamaEnabled;
        }
    }
}