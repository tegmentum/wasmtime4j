package ai.tegmentum.wasmtime4j.toolchain;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for WebAssembly toolchain implementations.
 *
 * <p>A toolchain represents a complete development environment for compiling source code
 * to WebAssembly. This includes compilers, linkers, optimizers, and associated tools.
 *
 * <p>Implementations must provide:
 * <ul>
 *   <li>Source code compilation to WebAssembly</li>
 *   <li>Dependency management and linking</li>
 *   <li>Optimization and post-processing</li>
 *   <li>Debugging and profiling capabilities</li>
 *   <li>Build system integration</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface Toolchain extends AutoCloseable {

    /**
     * Returns the name of this toolchain.
     *
     * @return the toolchain name
     */
    String getName();

    /**
     * Returns the version of this toolchain.
     *
     * @return the toolchain version
     */
    String getVersion();

    /**
     * Returns the type of this toolchain.
     *
     * @return the toolchain type
     */
    ToolchainType getType();

    /**
     * Returns the installation path of this toolchain.
     *
     * @return the installation path
     */
    Path getInstallationPath();

    /**
     * Returns the supported WebAssembly features.
     *
     * @return list of supported WebAssembly features
     */
    List<String> getSupportedFeatures();

    /**
     * Returns the supported target architectures.
     *
     * @return list of supported target architectures
     */
    List<String> getSupportedTargets();

    /**
     * Validates that this toolchain is properly installed and functional.
     *
     * @return a future containing the validation result
     * @throws WasmException if validation fails
     */
    CompletableFuture<ToolchainValidationResult> validate() throws WasmException;

    /**
     * Compiles source code to WebAssembly using this toolchain.
     *
     * @param request the compilation request
     * @return a future containing the compilation result
     * @throws WasmException if compilation fails
     * @throws IllegalArgumentException if request is null or invalid
     */
    CompletableFuture<CompilationResult> compile(CompilationRequest request) throws WasmException;

    /**
     * Links WebAssembly modules and dependencies.
     *
     * @param request the linking request
     * @return a future containing the linking result
     * @throws WasmException if linking fails
     * @throws IllegalArgumentException if request is null or invalid
     */
    CompletableFuture<LinkingResult> link(LinkingRequest request) throws WasmException;

    /**
     * Optimizes WebAssembly modules using toolchain-specific optimizers.
     *
     * @param request the optimization request
     * @return a future containing the optimization result
     * @throws WasmException if optimization fails
     * @throws IllegalArgumentException if request is null or invalid
     */
    CompletableFuture<OptimizationResult> optimize(OptimizationRequest request)
            throws WasmException;

    /**
     * Profiles compilation or execution using toolchain-specific profilers.
     *
     * @param request the profiling request
     * @return a future containing the profiling result
     * @throws WasmException if profiling fails
     * @throws IllegalArgumentException if request is null or invalid
     */
    CompletableFuture<ProfilingResult> profile(ProfilingRequest request) throws WasmException;

    /**
     * Debugs WebAssembly modules using toolchain-specific debuggers.
     *
     * @param request the debugging request
     * @return a future containing the debugging session
     * @throws WasmException if debugging fails
     * @throws IllegalArgumentException if request is null or invalid
     */
    CompletableFuture<DebuggingSession> debug(DebuggingRequest request) throws WasmException;

    /**
     * Returns toolchain-specific configuration options.
     *
     * @return map of configuration options
     */
    Map<String, Object> getConfiguration();

    /**
     * Updates toolchain configuration.
     *
     * @param configuration the new configuration options
     * @throws WasmException if configuration update fails
     * @throws IllegalArgumentException if configuration is null or invalid
     */
    void updateConfiguration(Map<String, Object> configuration) throws WasmException;

    /**
     * Returns the current health status of this toolchain.
     *
     * @return the health status
     */
    ToolchainHealthStatus getHealthStatus();

    /**
     * Returns detailed information about this toolchain.
     *
     * @return the toolchain information
     */
    ToolchainInfo getInfo();

    @Override
    void close() throws Exception;
}