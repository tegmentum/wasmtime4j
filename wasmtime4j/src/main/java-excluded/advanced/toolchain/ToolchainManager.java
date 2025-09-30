package ai.tegmentum.wasmtime4j.toolchain;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Central manager for WebAssembly toolchain integration and orchestration.
 *
 * <p>The ToolchainManager provides a unified interface for integrating with various WebAssembly
 * development toolchains including LLVM, Rust, C/C++, and custom compiler backends. It handles
 * toolchain discovery, configuration, and execution while providing comprehensive error handling
 * and resource management.
 *
 * <p>Key features:
 * <ul>
 *   <li>Automatic toolchain discovery and validation</li>
 *   <li>Multi-toolchain project support</li>
 *   <li>Parallel compilation and optimization</li>
 *   <li>Build system integration</li>
 *   <li>Comprehensive debugging and profiling support</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * try (ToolchainManager manager = ToolchainManager.create()) {
 *     // Auto-discover available toolchains
 *     manager.discoverToolchains();
 *
 *     // Configure Rust toolchain
 *     RustToolchain rustToolchain = manager.getRustToolchain()
 *         .orElseThrow(() -> new WasmException("Rust toolchain not found"));
 *
 *     // Compile Rust project to WebAssembly
 *     CompilationResult result = rustToolchain.compile(
 *         CompilationRequest.builder()
 *             .sourceDirectory(Paths.get("src/rust-project"))
 *             .targetDirectory(Paths.get("target/wasm"))
 *             .optimizationLevel(OptimizationLevel.RELEASE)
 *             .features(List.of("wasi", "simd"))
 *             .build()
 *     );
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ToolchainManager implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ToolchainManager.class.getName());

    private final ToolchainRegistry registry;
    private final ToolchainConfiguration configuration;
    private final ToolchainExecutor executor;
    private volatile boolean closed = false;

    private ToolchainManager(final ToolchainConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Toolchain configuration cannot be null");
        }

        this.configuration = configuration;
        this.registry = new ToolchainRegistry(configuration);
        this.executor = new ToolchainExecutor(configuration);

        LOGGER.info("ToolchainManager initialized with configuration: " + configuration);
    }

    /**
     * Creates a new ToolchainManager with default configuration.
     *
     * @return a new ToolchainManager instance
     * @throws WasmException if the manager cannot be initialized
     */
    public static ToolchainManager create() throws WasmException {
        return create(ToolchainConfiguration.defaultConfiguration());
    }

    /**
     * Creates a new ToolchainManager with the specified configuration.
     *
     * @param configuration the toolchain configuration
     * @return a new ToolchainManager instance
     * @throws WasmException if the manager cannot be initialized
     * @throws IllegalArgumentException if configuration is null
     */
    public static ToolchainManager create(final ToolchainConfiguration configuration)
            throws WasmException {
        return new ToolchainManager(configuration);
    }

    /**
     * Discovers and registers all available WebAssembly toolchains on the system.
     *
     * <p>This method searches for toolchains in standard locations and validates their
     * capabilities. Discovered toolchains are automatically registered and can be retrieved
     * using the appropriate getter methods.
     *
     * @return a future containing the discovery results
     * @throws WasmException if discovery fails
     * @throws IllegalStateException if the manager is closed
     */
    public CompletableFuture<ToolchainDiscoveryResult> discoverToolchains() throws WasmException {
        validateNotClosed();

        LOGGER.info("Starting toolchain discovery");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return registry.discoverAll();
            } catch (final Exception e) {
                LOGGER.severe("Toolchain discovery failed: " + e.getMessage());
                throw new RuntimeException("Toolchain discovery failed", e);
            }
        }, executor.getDiscoveryExecutor());
    }

    /**
     * Registers a custom toolchain with the manager.
     *
     * @param toolchain the toolchain to register
     * @throws WasmException if registration fails
     * @throws IllegalArgumentException if toolchain is null
     * @throws IllegalStateException if the manager is closed
     */
    public void registerToolchain(final Toolchain toolchain) throws WasmException {
        validateNotClosed();

        if (toolchain == null) {
            throw new IllegalArgumentException("Toolchain cannot be null");
        }

        LOGGER.info("Registering custom toolchain: " + toolchain.getName());
        registry.register(toolchain);
    }

    /**
     * Returns the LLVM toolchain if available.
     *
     * @return an Optional containing the LLVM toolchain, or empty if not available
     * @throws IllegalStateException if the manager is closed
     */
    public Optional<LlvmToolchain> getLlvmToolchain() {
        validateNotClosed();
        return registry.getToolchain(ToolchainType.LLVM, LlvmToolchain.class);
    }

    /**
     * Returns the Rust toolchain if available.
     *
     * @return an Optional containing the Rust toolchain, or empty if not available
     * @throws IllegalStateException if the manager is closed
     */
    public Optional<RustToolchain> getRustToolchain() {
        validateNotClosed();
        return registry.getToolchain(ToolchainType.RUST, RustToolchain.class);
    }

    /**
     * Returns the C/C++ toolchain if available.
     *
     * @return an Optional containing the C/C++ toolchain, or empty if not available
     * @throws IllegalStateException if the manager is closed
     */
    public Optional<CppToolchain> getCppToolchain() {
        validateNotClosed();
        return registry.getToolchain(ToolchainType.CPP, CppToolchain.class);
    }

    /**
     * Returns the Emscripten toolchain if available.
     *
     * @return an Optional containing the Emscripten toolchain, or empty if not available
     * @throws IllegalStateException if the manager is closed
     */
    public Optional<EmscriptenToolchain> getEmscriptenToolchain() {
        validateNotClosed();
        return registry.getToolchain(ToolchainType.EMSCRIPTEN, EmscriptenToolchain.class);
    }

    /**
     * Returns a custom toolchain by name if available.
     *
     * @param name the name of the custom toolchain
     * @param type the expected type of the toolchain
     * @param <T> the toolchain type
     * @return an Optional containing the custom toolchain, or empty if not available
     * @throws IllegalArgumentException if name or type is null
     * @throws IllegalStateException if the manager is closed
     */
    public <T extends Toolchain> Optional<T> getCustomToolchain(final String name,
            final Class<T> type) {
        validateNotClosed();

        if (name == null) {
            throw new IllegalArgumentException("Toolchain name cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Toolchain type cannot be null");
        }

        return registry.getCustomToolchain(name, type);
    }

    /**
     * Returns all registered toolchains.
     *
     * @return a map of toolchain types to toolchain instances
     * @throws IllegalStateException if the manager is closed
     */
    public Map<ToolchainType, List<Toolchain>> getAllToolchains() {
        validateNotClosed();
        return registry.getAllToolchains();
    }

    /**
     * Compiles a WebAssembly project using the specified toolchain.
     *
     * @param request the compilation request
     * @return a future containing the compilation result
     * @throws WasmException if compilation fails
     * @throws IllegalArgumentException if request is null
     * @throws IllegalStateException if the manager is closed
     */
    public CompletableFuture<CompilationResult> compile(final CompilationRequest request)
            throws WasmException {
        validateNotClosed();

        if (request == null) {
            throw new IllegalArgumentException("Compilation request cannot be null");
        }

        LOGGER.info("Starting compilation with toolchain: " + request.getToolchainType());
        return executor.compile(request);
    }

    /**
     * Optimizes a WebAssembly module using available optimization toolchains.
     *
     * @param request the optimization request
     * @return a future containing the optimization result
     * @throws WasmException if optimization fails
     * @throws IllegalArgumentException if request is null
     * @throws IllegalStateException if the manager is closed
     */
    public CompletableFuture<OptimizationResult> optimize(final OptimizationRequest request)
            throws WasmException {
        validateNotClosed();

        if (request == null) {
            throw new IllegalArgumentException("Optimization request cannot be null");
        }

        LOGGER.info("Starting optimization pipeline");
        return executor.optimize(request);
    }

    /**
     * Profiles a WebAssembly compilation or execution using integrated profiling tools.
     *
     * @param request the profiling request
     * @return a future containing the profiling result
     * @throws WasmException if profiling fails
     * @throws IllegalArgumentException if request is null
     * @throws IllegalStateException if the manager is closed
     */
    public CompletableFuture<ProfilingResult> profile(final ProfilingRequest request)
            throws WasmException {
        validateNotClosed();

        if (request == null) {
            throw new IllegalArgumentException("Profiling request cannot be null");
        }

        LOGGER.info("Starting profiling session");
        return executor.profile(request);
    }

    /**
     * Integrates with external build systems for seamless WebAssembly compilation.
     *
     * @param buildSystem the build system type
     * @param projectPath the project path
     * @param configuration the build configuration
     * @return a future containing the build integration result
     * @throws WasmException if integration fails
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalStateException if the manager is closed
     */
    public CompletableFuture<BuildIntegrationResult> integrateBuildSystem(
            final BuildSystemType buildSystem,
            final Path projectPath,
            final BuildConfiguration configuration) throws WasmException {
        validateNotClosed();

        if (buildSystem == null) {
            throw new IllegalArgumentException("Build system type cannot be null");
        }
        if (projectPath == null) {
            throw new IllegalArgumentException("Project path cannot be null");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("Build configuration cannot be null");
        }

        LOGGER.info("Integrating with build system: " + buildSystem + " at " + projectPath);
        return executor.integrateBuildSystem(buildSystem, projectPath, configuration);
    }

    /**
     * Returns the current toolchain configuration.
     *
     * @return the toolchain configuration
     * @throws IllegalStateException if the manager is closed
     */
    public ToolchainConfiguration getConfiguration() {
        validateNotClosed();
        return configuration;
    }

    /**
     * Returns toolchain health and status information.
     *
     * @return the toolchain health status
     * @throws IllegalStateException if the manager is closed
     */
    public ToolchainHealthStatus getHealthStatus() {
        validateNotClosed();
        return registry.getHealthStatus();
    }

    /**
     * Validates that the manager is not closed.
     *
     * @throws IllegalStateException if the manager is closed
     */
    private void validateNotClosed() {
        if (closed) {
            throw new IllegalStateException("ToolchainManager is closed");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        LOGGER.info("Closing ToolchainManager");

        try {
            executor.close();
        } catch (final Exception e) {
            LOGGER.warning("Error closing executor: " + e.getMessage());
        }

        try {
            registry.close();
        } catch (final Exception e) {
            LOGGER.warning("Error closing registry: " + e.getMessage());
        }

        closed = true;
        LOGGER.info("ToolchainManager closed successfully");
    }
}