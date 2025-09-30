package ai.tegmentum.wasmtime4j.toolchain;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * LLVM toolchain implementation for WebAssembly compilation and optimization.
 *
 * <p>This toolchain provides comprehensive LLVM integration including:
 * <ul>
 *   <li>Clang/LLVM WebAssembly compilation</li>
 *   <li>LLD WebAssembly linker integration</li>
 *   <li>LLVM optimization passes</li>
 *   <li>WebAssembly-specific code generation</li>
 *   <li>DWARF debugging information</li>
 *   <li>LLVM profiling and analysis tools</li>
 * </ul>
 *
 * <p>The LLVM toolchain supports both C and C++ source compilation with extensive
 * WebAssembly feature support including SIMD, threads, bulk memory, and reference types.
 *
 * @since 1.0.0
 */
public final class LlvmToolchain implements Toolchain {

    private static final Logger LOGGER = Logger.getLogger(LlvmToolchain.class.getName());

    private static final String DEFAULT_CLANG_BINARY = "clang";
    private static final String DEFAULT_CLANGXX_BINARY = "clang++";
    private static final String DEFAULT_LLD_BINARY = "wasm-ld";
    private static final String DEFAULT_OPT_BINARY = "opt";
    private static final String DEFAULT_LLC_BINARY = "llc";

    private final String name;
    private final String version;
    private final Path installationPath;
    private final Map<String, Object> configuration;
    private final ExecutorService executor;
    private volatile boolean closed = false;

    private LlvmToolchain(final Builder builder) {
        this.name = builder.name != null ? builder.name : "LLVM";
        this.version = builder.version;
        this.installationPath = builder.installationPath;
        this.configuration = new HashMap<>(builder.configuration);
        this.executor = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r, "llvm-toolchain-" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });

        LOGGER.info("Initialized LLVM toolchain: " + name + " v" + version + " at " + installationPath);
    }

    /**
     * Creates a new LLVM toolchain builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Auto-discovers and creates an LLVM toolchain instance.
     *
     * @return an LLVM toolchain instance, or null if not found
     * @throws WasmException if discovery fails
     */
    public static LlvmToolchain discover() throws WasmException {
        LOGGER.info("Discovering LLVM toolchain");

        // Try common installation paths
        final List<Path> searchPaths = Arrays.asList(
                Paths.get("/usr/bin"),
                Paths.get("/usr/local/bin"),
                Paths.get("/opt/homebrew/bin"),
                Paths.get("/opt/llvm/bin"),
                Paths.get(System.getProperty("user.home"), ".local", "bin")
        );

        for (final Path searchPath : searchPaths) {
            final Path clangPath = searchPath.resolve(DEFAULT_CLANG_BINARY);
            if (Files.isExecutable(clangPath)) {
                try {
                    final String version = getClangVersion(clangPath);
                    LOGGER.info("Found LLVM toolchain at " + searchPath + " version " + version);

                    return builder()
                            .installationPath(searchPath)
                            .version(version)
                            .build();
                } catch (final Exception e) {
                    LOGGER.warning("Failed to validate LLVM at " + searchPath + ": " + e.getMessage());
                }
            }
        }

        LOGGER.warning("LLVM toolchain not found in standard locations");
        return null;
    }

    /**
     * Gets the Clang version from the specified binary.
     *
     * @param clangPath the path to the Clang binary
     * @return the version string
     * @throws IOException if version detection fails
     */
    private static String getClangVersion(final Path clangPath) throws IOException {
        try {
            final Process process = new ProcessBuilder(clangPath.toString(), "--version")
                    .redirectErrorStream(true)
                    .start();

            final String output = new String(process.getInputStream().readAllBytes());
            final int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("Clang version check failed with exit code " + exitCode);
            }

            // Parse version from output like "clang version 15.0.0"
            final String[] lines = output.split("\n");
            for (final String line : lines) {
                if (line.contains("clang version")) {
                    final String[] parts = line.split("\\s+");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if ("version".equals(parts[i])) {
                            return parts[i + 1];
                        }
                    }
                }
            }

            throw new IOException("Could not parse Clang version from output: " + output);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while checking Clang version", e);
        }
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
    public ToolchainType getType() {
        return ToolchainType.LLVM;
    }

    @Override
    public Path getInstallationPath() {
        return installationPath;
    }

    @Override
    public List<String> getSupportedFeatures() {
        return Arrays.asList(
                "mutable-globals",
                "sign-ext",
                "bulk-memory",
                "simd128",
                "atomics",
                "threads",
                "reference-types",
                "multi-value",
                "tail-call",
                "exception-handling"
        );
    }

    @Override
    public List<String> getSupportedTargets() {
        return Arrays.asList(
                "wasm32-unknown-unknown",
                "wasm32-wasi",
                "wasm64-unknown-unknown"
        );
    }

    @Override
    public CompletableFuture<ToolchainValidationResult> validate() throws WasmException {
        validateNotClosed();

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Validating LLVM toolchain");

                final List<String> issues = new ArrayList<>();
                boolean valid = true;

                // Check Clang
                final Path clangPath = installationPath.resolve(DEFAULT_CLANG_BINARY);
                if (!Files.isExecutable(clangPath)) {
                    issues.add("Clang binary not found or not executable: " + clangPath);
                    valid = false;
                }

                // Check Clang++
                final Path clangxxPath = installationPath.resolve(DEFAULT_CLANGXX_BINARY);
                if (!Files.isExecutable(clangxxPath)) {
                    issues.add("Clang++ binary not found or not executable: " + clangxxPath);
                    valid = false;
                }

                // Check WebAssembly linker
                final Path lldPath = installationPath.resolve(DEFAULT_LLD_BINARY);
                if (!Files.isExecutable(lldPath)) {
                    issues.add("WebAssembly linker not found or not executable: " + lldPath);
                    valid = false;
                }

                // Check optimizer
                final Path optPath = installationPath.resolve(DEFAULT_OPT_BINARY);
                if (!Files.isExecutable(optPath)) {
                    issues.add("LLVM optimizer not found or not executable: " + optPath);
                    // Not fatal, mark as warning
                }

                // Test WebAssembly target support
                if (valid) {
                    try {
                        testWebAssemblySupport(clangPath);
                    } catch (final Exception e) {
                        issues.add("WebAssembly target not supported: " + e.getMessage());
                        valid = false;
                    }
                }

                final ToolchainValidationResult result = ToolchainValidationResult.builder()
                        .toolchainName(name)
                        .valid(valid)
                        .issues(issues)
                        .capabilities(getSupportedFeatures())
                        .build();

                LOGGER.info("LLVM toolchain validation completed: " + (valid ? "PASS" : "FAIL"));
                return result;

            } catch (final Exception e) {
                LOGGER.severe("LLVM toolchain validation failed: " + e.getMessage());
                throw new RuntimeException("Validation failed", e);
            }
        }, executor);
    }

    /**
     * Tests WebAssembly target support by compiling a simple test program.
     *
     * @param clangPath the path to the Clang binary
     * @throws IOException if the test fails
     */
    private void testWebAssemblySupport(final Path clangPath) throws IOException {
        try {
            // Create temporary test file
            final Path tempDir = Files.createTempDirectory("llvm-wasm-test");
            final Path testFile = tempDir.resolve("test.c");
            Files.writeString(testFile, "int main() { return 0; }");

            // Compile to WebAssembly
            final Process process = new ProcessBuilder(
                    clangPath.toString(),
                    "--target=wasm32-unknown-unknown",
                    "-nostdlib",
                    "-Wl,--no-entry",
                    "-Wl,--export=main",
                    "-o", tempDir.resolve("test.wasm").toString(),
                    testFile.toString()
            ).start();

            final int exitCode = process.waitFor();

            // Cleanup
            Files.deleteIfExists(testFile);
            Files.deleteIfExists(tempDir.resolve("test.wasm"));
            Files.deleteIfExists(tempDir);

            if (exitCode != 0) {
                final String error = new String(process.getErrorStream().readAllBytes());
                throw new IOException("WebAssembly compilation test failed: " + error);
            }

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during WebAssembly support test", e);
        }
    }

    @Override
    public CompletableFuture<CompilationResult> compile(final CompilationRequest request)
            throws WasmException {
        validateNotClosed();

        if (request == null) {
            throw new IllegalArgumentException("Compilation request cannot be null");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Starting LLVM compilation for " + request.getSourceFiles().size() + " files");

                final LlvmCompiler compiler = new LlvmCompiler(this, request);
                return compiler.compile();

            } catch (final Exception e) {
                LOGGER.severe("LLVM compilation failed: " + e.getMessage());
                throw new RuntimeException("Compilation failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<LinkingResult> link(final LinkingRequest request) throws WasmException {
        validateNotClosed();

        if (request == null) {
            throw new IllegalArgumentException("Linking request cannot be null");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Starting LLVM linking for " + request.getObjectFiles().size() + " objects");

                final LlvmLinker linker = new LlvmLinker(this, request);
                return linker.link();

            } catch (final Exception e) {
                LOGGER.severe("LLVM linking failed: " + e.getMessage());
                throw new RuntimeException("Linking failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<OptimizationResult> optimize(final OptimizationRequest request)
            throws WasmException {
        validateNotClosed();

        if (request == null) {
            throw new IllegalArgumentException("Optimization request cannot be null");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Starting LLVM optimization with level " + request.getLevel());

                final LlvmOptimizer optimizer = new LlvmOptimizer(this, request);
                return optimizer.optimize();

            } catch (final Exception e) {
                LOGGER.severe("LLVM optimization failed: " + e.getMessage());
                throw new RuntimeException("Optimization failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<ProfilingResult> profile(final ProfilingRequest request)
            throws WasmException {
        validateNotClosed();

        if (request == null) {
            throw new IllegalArgumentException("Profiling request cannot be null");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Starting LLVM profiling session");

                final LlvmProfiler profiler = new LlvmProfiler(this, request);
                return profiler.profile();

            } catch (final Exception e) {
                LOGGER.severe("LLVM profiling failed: " + e.getMessage());
                throw new RuntimeException("Profiling failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<DebuggingSession> debug(final DebuggingRequest request)
            throws WasmException {
        validateNotClosed();

        if (request == null) {
            throw new IllegalArgumentException("Debugging request cannot be null");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Starting LLVM debugging session");

                final LlvmDebugger debugger = new LlvmDebugger(this, request);
                return debugger.startSession();

            } catch (final Exception e) {
                LOGGER.severe("LLVM debugging failed: " + e.getMessage());
                throw new RuntimeException("Debugging failed", e);
            }
        }, executor);
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return new HashMap<>(configuration);
    }

    @Override
    public void updateConfiguration(final Map<String, Object> newConfiguration)
            throws WasmException {
        validateNotClosed();

        if (newConfiguration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        synchronized (configuration) {
            configuration.clear();
            configuration.putAll(newConfiguration);
        }

        LOGGER.info("Updated LLVM toolchain configuration");
    }

    @Override
    public ToolchainHealthStatus getHealthStatus() {
        if (closed) {
            return ToolchainHealthStatus.UNAVAILABLE;
        }

        // Perform basic health checks
        final Path clangPath = installationPath.resolve(DEFAULT_CLANG_BINARY);
        if (!Files.isExecutable(clangPath)) {
            return ToolchainHealthStatus.UNHEALTHY;
        }

        return ToolchainHealthStatus.HEALTHY;
    }

    @Override
    public ToolchainInfo getInfo() {
        return ToolchainInfo.builder()
                .name(name)
                .version(version)
                .type(getType())
                .installationPath(installationPath)
                .supportedFeatures(getSupportedFeatures())
                .supportedTargets(getSupportedTargets())
                .configuration(getConfiguration())
                .healthStatus(getHealthStatus())
                .build();
    }

    /**
     * Returns the path to the Clang binary.
     *
     * @return the Clang binary path
     */
    public Path getClangPath() {
        return installationPath.resolve(DEFAULT_CLANG_BINARY);
    }

    /**
     * Returns the path to the Clang++ binary.
     *
     * @return the Clang++ binary path
     */
    public Path getClangxxPath() {
        return installationPath.resolve(DEFAULT_CLANGXX_BINARY);
    }

    /**
     * Returns the path to the WebAssembly linker binary.
     *
     * @return the linker binary path
     */
    public Path getLinkerPath() {
        return installationPath.resolve(DEFAULT_LLD_BINARY);
    }

    /**
     * Returns the path to the LLVM optimizer binary.
     *
     * @return the optimizer binary path
     */
    public Path getOptimizerPath() {
        return installationPath.resolve(DEFAULT_OPT_BINARY);
    }

    /**
     * Returns the executor service for asynchronous operations.
     *
     * @return the executor service
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Validates that the toolchain is not closed.
     *
     * @throws IllegalStateException if the toolchain is closed
     */
    private void validateNotClosed() {
        if (closed) {
            throw new IllegalStateException("LLVM toolchain is closed");
        }
    }

    @Override
    public void close() throws Exception {
        if (closed) {
            return;
        }

        LOGGER.info("Closing LLVM toolchain");

        executor.shutdown();
        closed = true;

        LOGGER.info("LLVM toolchain closed successfully");
    }

    /**
     * Builder for creating LLVM toolchain instances.
     */
    public static final class Builder {
        private String name;
        private String version;
        private Path installationPath;
        private final Map<String, Object> configuration = new HashMap<>();

        private Builder() {
        }

        /**
         * Sets the toolchain name.
         *
         * @param name the toolchain name
         * @return this builder
         */
        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the toolchain version.
         *
         * @param version the toolchain version
         * @return this builder
         */
        public Builder version(final String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the installation path.
         *
         * @param installationPath the installation path
         * @return this builder
         */
        public Builder installationPath(final Path installationPath) {
            this.installationPath = installationPath;
            return this;
        }

        /**
         * Adds a configuration option.
         *
         * @param key the configuration key
         * @param value the configuration value
         * @return this builder
         */
        public Builder configuration(final String key, final Object value) {
            this.configuration.put(key, value);
            return this;
        }

        /**
         * Sets the configuration options.
         *
         * @param configuration the configuration options
         * @return this builder
         */
        public Builder configuration(final Map<String, Object> configuration) {
            this.configuration.clear();
            if (configuration != null) {
                this.configuration.putAll(configuration);
            }
            return this;
        }

        /**
         * Builds the LLVM toolchain instance.
         *
         * @return a new LLVM toolchain instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public LlvmToolchain build() {
            if (installationPath == null) {
                throw new IllegalArgumentException("Installation path is required");
            }
            if (version == null) {
                throw new IllegalArgumentException("Version is required");
            }

            return new LlvmToolchain(this);
        }
    }
}