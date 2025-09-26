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
 * Rust toolchain implementation for WebAssembly compilation and development.
 *
 * <p>This toolchain provides comprehensive Rust integration including:
 * <ul>
 *   <li>Rustc WebAssembly compilation</li>
 *   <li>Cargo build system integration</li>
 *   <li>wasm-pack for npm package generation</li>
 *   <li>cargo-generate for project templates</li>
 *   <li>wasm-bindgen for JavaScript interop</li>
 *   <li>WASI and web target support</li>
 * </ul>
 *
 * <p>The Rust toolchain supports all modern WebAssembly features and provides
 * first-class integration with the Rust ecosystem for WebAssembly development.
 *
 * @since 1.0.0
 */
public final class RustToolchain implements Toolchain {

    private static final Logger LOGGER = Logger.getLogger(RustToolchain.class.getName());

    private static final String DEFAULT_RUSTC_BINARY = "rustc";
    private static final String DEFAULT_CARGO_BINARY = "cargo";
    private static final String DEFAULT_WASM_PACK_BINARY = "wasm-pack";
    private static final String DEFAULT_CARGO_GENERATE_BINARY = "cargo-generate";

    private final String name;
    private final String version;
    private final Path installationPath;
    private final Map<String, Object> configuration;
    private final ExecutorService executor;
    private volatile boolean closed = false;

    private RustToolchain(final Builder builder) {
        this.name = builder.name != null ? builder.name : "Rust";
        this.version = builder.version;
        this.installationPath = builder.installationPath;
        this.configuration = new HashMap<>(builder.configuration);
        this.executor = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r, "rust-toolchain-" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });

        LOGGER.info("Initialized Rust toolchain: " + name + " v" + version + " at " + installationPath);
    }

    /**
     * Creates a new Rust toolchain builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Auto-discovers and creates a Rust toolchain instance.
     *
     * @return a Rust toolchain instance, or null if not found
     * @throws WasmException if discovery fails
     */
    public static RustToolchain discover() throws WasmException {
        LOGGER.info("Discovering Rust toolchain");

        // Check for rustup installation first
        final Path rustupPath = findBinary("rustup");
        if (rustupPath != null) {
            try {
                return discoverViaRustup(rustupPath);
            } catch (final Exception e) {
                LOGGER.warning("Failed to discover via rustup: " + e.getMessage());
            }
        }

        // Fall back to direct binary search
        final Path cargoHome = findCargoHome();
        if (cargoHome != null) {
            final Path binPath = cargoHome.resolve("bin");
            final Path rustcPath = binPath.resolve(DEFAULT_RUSTC_BINARY);
            final Path cargoPath = binPath.resolve(DEFAULT_CARGO_BINARY);

            if (Files.isExecutable(rustcPath) && Files.isExecutable(cargoPath)) {
                try {
                    final String version = getRustcVersion(rustcPath);
                    LOGGER.info("Found Rust toolchain at " + binPath + " version " + version);

                    return builder()
                            .installationPath(binPath)
                            .version(version)
                            .build();
                } catch (final Exception e) {
                    LOGGER.warning("Failed to validate Rust at " + binPath + ": " + e.getMessage());
                }
            }
        }

        // Try common system paths
        final List<Path> searchPaths = Arrays.asList(
                Paths.get("/usr/bin"),
                Paths.get("/usr/local/bin"),
                Paths.get("/opt/homebrew/bin"),
                Paths.get(System.getProperty("user.home"), ".local", "bin")
        );

        for (final Path searchPath : searchPaths) {
            final Path rustcPath = searchPath.resolve(DEFAULT_RUSTC_BINARY);
            final Path cargoPath = searchPath.resolve(DEFAULT_CARGO_BINARY);

            if (Files.isExecutable(rustcPath) && Files.isExecutable(cargoPath)) {
                try {
                    final String version = getRustcVersion(rustcPath);
                    LOGGER.info("Found Rust toolchain at " + searchPath + " version " + version);

                    return builder()
                            .installationPath(searchPath)
                            .version(version)
                            .build();
                } catch (final Exception e) {
                    LOGGER.warning("Failed to validate Rust at " + searchPath + ": " + e.getMessage());
                }
            }
        }

        LOGGER.warning("Rust toolchain not found");
        return null;
    }

    /**
     * Discovers Rust toolchain via rustup.
     *
     * @param rustupPath the path to rustup binary
     * @return a Rust toolchain instance
     * @throws IOException if discovery fails
     */
    private static RustToolchain discoverViaRustup(final Path rustupPath) throws IOException {
        try {
            // Get active toolchain
            final Process process = new ProcessBuilder(rustupPath.toString(), "show", "active-toolchain")
                    .redirectErrorStream(true)
                    .start();

            final String output = new String(process.getInputStream().readAllBytes());
            final int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("Rustup show failed with exit code " + exitCode);
            }

            // Parse toolchain info
            final String[] parts = output.trim().split("\\s+");
            final String toolchainName = parts[0];
            final String version = toolchainName.contains("-") ? toolchainName.split("-")[0] : toolchainName;

            // Get toolchain directory
            final Process pathProcess = new ProcessBuilder(rustupPath.toString(), "which", "rustc")
                    .redirectErrorStream(true)
                    .start();

            final String pathOutput = new String(pathProcess.getInputStream().readAllBytes());
            final int pathExitCode = pathProcess.waitFor();

            if (pathExitCode != 0) {
                throw new IOException("Rustup which failed with exit code " + pathExitCode);
            }

            final Path rustcPath = Paths.get(pathOutput.trim());
            final Path binPath = rustcPath.getParent();

            return builder()
                    .name("Rust (" + toolchainName + ")")
                    .installationPath(binPath)
                    .version(version)
                    .build();

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during rustup discovery", e);
        }
    }

    /**
     * Finds a binary in the system PATH.
     *
     * @param binaryName the binary name
     * @return the path to the binary, or null if not found
     */
    private static Path findBinary(final String binaryName) {
        final String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return null;
        }

        for (final String pathEntry : pathEnv.split(System.getProperty("path.separator"))) {
            final Path binaryPath = Paths.get(pathEntry, binaryName);
            if (Files.isExecutable(binaryPath)) {
                return binaryPath;
            }
        }

        return null;
    }

    /**
     * Finds the Cargo home directory.
     *
     * @return the Cargo home path, or null if not found
     */
    private static Path findCargoHome() {
        final String cargoHome = System.getenv("CARGO_HOME");
        if (cargoHome != null) {
            return Paths.get(cargoHome);
        }

        final String userHome = System.getProperty("user.home");
        if (userHome != null) {
            final Path defaultCargoHome = Paths.get(userHome, ".cargo");
            if (Files.isDirectory(defaultCargoHome)) {
                return defaultCargoHome;
            }
        }

        return null;
    }

    /**
     * Gets the rustc version from the specified binary.
     *
     * @param rustcPath the path to the rustc binary
     * @return the version string
     * @throws IOException if version detection fails
     */
    private static String getRustcVersion(final Path rustcPath) throws IOException {
        try {
            final Process process = new ProcessBuilder(rustcPath.toString(), "--version")
                    .redirectErrorStream(true)
                    .start();

            final String output = new String(process.getInputStream().readAllBytes());
            final int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("Rustc version check failed with exit code " + exitCode);
            }

            // Parse version from output like "rustc 1.70.0 (90c541806 2023-05-31)"
            final String[] parts = output.trim().split("\\s+");
            if (parts.length >= 2) {
                return parts[1];
            }

            throw new IOException("Could not parse rustc version from output: " + output);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while checking rustc version", e);
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
        return ToolchainType.RUST;
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
                "exception-handling",
                "gc"
        );
    }

    @Override
    public List<String> getSupportedTargets() {
        return Arrays.asList(
                "wasm32-unknown-unknown",
                "wasm32-wasi",
                "wasm32-unknown-emscripten"
        );
    }

    @Override
    public CompletableFuture<ToolchainValidationResult> validate() throws WasmException {
        validateNotClosed();

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Validating Rust toolchain");

                final List<String> issues = new ArrayList<>();
                boolean valid = true;

                // Check rustc
                final Path rustcPath = installationPath.resolve(DEFAULT_RUSTC_BINARY);
                if (!Files.isExecutable(rustcPath)) {
                    issues.add("Rustc binary not found or not executable: " + rustcPath);
                    valid = false;
                }

                // Check cargo
                final Path cargoPath = installationPath.resolve(DEFAULT_CARGO_BINARY);
                if (!Files.isExecutable(cargoPath)) {
                    issues.add("Cargo binary not found or not executable: " + cargoPath);
                    valid = false;
                }

                // Check WebAssembly targets
                if (valid) {
                    try {
                        checkWebAssemblyTargets(rustcPath);
                    } catch (final Exception e) {
                        issues.add("WebAssembly targets not available: " + e.getMessage());
                        valid = false;
                    }
                }

                // Check optional tools (wasm-pack, cargo-generate)
                checkOptionalTools(issues);

                final ToolchainValidationResult result = ToolchainValidationResult.builder()
                        .toolchainName(name)
                        .valid(valid)
                        .issues(issues)
                        .capabilities(getSupportedFeatures())
                        .build();

                LOGGER.info("Rust toolchain validation completed: " + (valid ? "PASS" : "FAIL"));
                return result;

            } catch (final Exception e) {
                LOGGER.severe("Rust toolchain validation failed: " + e.getMessage());
                throw new RuntimeException("Validation failed", e);
            }
        }, executor);
    }

    /**
     * Checks if WebAssembly targets are installed.
     *
     * @param rustcPath the path to rustc binary
     * @throws IOException if target check fails
     */
    private void checkWebAssemblyTargets(final Path rustcPath) throws IOException {
        try {
            final Process process = new ProcessBuilder(rustcPath.toString(), "--print", "target-list")
                    .redirectErrorStream(true)
                    .start();

            final String output = new String(process.getInputStream().readAllBytes());
            final int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("Target list check failed with exit code " + exitCode);
            }

            final boolean hasWasm32 = output.contains("wasm32-unknown-unknown");
            final boolean hasWasi = output.contains("wasm32-wasi");

            if (!hasWasm32 || !hasWasi) {
                throw new IOException("Required WebAssembly targets not installed: " +
                        (hasWasm32 ? "" : "wasm32-unknown-unknown ") +
                        (hasWasi ? "" : "wasm32-wasi"));
            }

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during target check", e);
        }
    }

    /**
     * Checks for optional tools and adds warnings if missing.
     *
     * @param issues the list to add issues to
     */
    private void checkOptionalTools(final List<String> issues) {
        final Path wasmPackPath = installationPath.resolve(DEFAULT_WASM_PACK_BINARY);
        if (!Files.isExecutable(wasmPackPath)) {
            issues.add("wasm-pack not found (optional): install with 'cargo install wasm-pack'");
        }

        final Path cargoGeneratePath = installationPath.resolve(DEFAULT_CARGO_GENERATE_BINARY);
        if (!Files.isExecutable(cargoGeneratePath)) {
            issues.add("cargo-generate not found (optional): install with 'cargo install cargo-generate'");
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
                LOGGER.info("Starting Rust compilation");

                final RustCompiler compiler = new RustCompiler(this, request);
                return compiler.compile();

            } catch (final Exception e) {
                LOGGER.severe("Rust compilation failed: " + e.getMessage());
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
                LOGGER.info("Starting Rust linking");

                final RustLinker linker = new RustLinker(this, request);
                return linker.link();

            } catch (final Exception e) {
                LOGGER.severe("Rust linking failed: " + e.getMessage());
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
                LOGGER.info("Starting Rust optimization");

                final RustOptimizer optimizer = new RustOptimizer(this, request);
                return optimizer.optimize();

            } catch (final Exception e) {
                LOGGER.severe("Rust optimization failed: " + e.getMessage());
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
                LOGGER.info("Starting Rust profiling session");

                final RustProfiler profiler = new RustProfiler(this, request);
                return profiler.profile();

            } catch (final Exception e) {
                LOGGER.severe("Rust profiling failed: " + e.getMessage());
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
                LOGGER.info("Starting Rust debugging session");

                final RustDebugger debugger = new RustDebugger(this, request);
                return debugger.startSession();

            } catch (final Exception e) {
                LOGGER.severe("Rust debugging failed: " + e.getMessage());
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

        LOGGER.info("Updated Rust toolchain configuration");
    }

    @Override
    public ToolchainHealthStatus getHealthStatus() {
        if (closed) {
            return ToolchainHealthStatus.UNAVAILABLE;
        }

        final Path rustcPath = installationPath.resolve(DEFAULT_RUSTC_BINARY);
        final Path cargoPath = installationPath.resolve(DEFAULT_CARGO_BINARY);

        if (!Files.isExecutable(rustcPath) || !Files.isExecutable(cargoPath)) {
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
     * Returns the path to the rustc binary.
     *
     * @return the rustc binary path
     */
    public Path getRustcPath() {
        return installationPath.resolve(DEFAULT_RUSTC_BINARY);
    }

    /**
     * Returns the path to the cargo binary.
     *
     * @return the cargo binary path
     */
    public Path getCargoPath() {
        return installationPath.resolve(DEFAULT_CARGO_BINARY);
    }

    /**
     * Returns the path to the wasm-pack binary if available.
     *
     * @return the wasm-pack binary path, or null if not available
     */
    public Path getWasmPackPath() {
        final Path path = installationPath.resolve(DEFAULT_WASM_PACK_BINARY);
        return Files.isExecutable(path) ? path : null;
    }

    /**
     * Returns the path to the cargo-generate binary if available.
     *
     * @return the cargo-generate binary path, or null if not available
     */
    public Path getCargoGeneratePath() {
        final Path path = installationPath.resolve(DEFAULT_CARGO_GENERATE_BINARY);
        return Files.isExecutable(path) ? path : null;
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
            throw new IllegalStateException("Rust toolchain is closed");
        }
    }

    @Override
    public void close() throws Exception {
        if (closed) {
            return;
        }

        LOGGER.info("Closing Rust toolchain");

        executor.shutdown();
        closed = true;

        LOGGER.info("Rust toolchain closed successfully");
    }

    /**
     * Builder for creating Rust toolchain instances.
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
         * Builds the Rust toolchain instance.
         *
         * @return a new Rust toolchain instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public RustToolchain build() {
            if (installationPath == null) {
                throw new IllegalArgumentException("Installation path is required");
            }
            if (version == null) {
                throw new IllegalArgumentException("Version is required");
            }

            return new RustToolchain(this);
        }
    }
}