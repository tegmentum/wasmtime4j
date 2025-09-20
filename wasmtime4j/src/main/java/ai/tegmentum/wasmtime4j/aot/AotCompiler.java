package ai.tegmentum.wasmtime4j.aot;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import ai.tegmentum.wasmtime4j.serialization.TargetPlatform;
import java.util.List;

/**
 * Ahead-of-Time (AOT) compiler interface for WebAssembly modules.
 *
 * <p>AotCompiler provides functionality to compile WebAssembly modules ahead of time for specific
 * target platforms, enabling optimized execution and reduced startup time. AOT compilation produces
 * platform-specific serialized modules that can be loaded efficiently at runtime.
 *
 * <p>AOT compilation is particularly beneficial for production deployments where startup time is
 * critical and the target platforms are known in advance.
 *
 * @since 1.0.0
 */
public interface AotCompiler {

  /**
   * Compiles a WebAssembly module ahead of time for the current platform.
   *
   * <p>This method performs ahead-of-time compilation using the provided engine configuration and
   * AOT options. The resulting serialized module is optimized for the current platform and can be
   * loaded efficiently at runtime.
   *
   * @param engine the engine to use for compilation
   * @param wasmBytes the WebAssembly bytecode to compile
   * @param options AOT compilation options
   * @return a serialized module optimized for the current platform
   * @throws WasmException if AOT compilation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  SerializedModule compileModule(
      final Engine engine, final byte[] wasmBytes, final AotOptions options) throws WasmException;

  /**
   * Compiles a WebAssembly module ahead of time for a specific target platform.
   *
   * <p>This method enables cross-compilation for different target platforms, allowing modules to be
   * compiled on one platform for deployment on another.
   *
   * @param engine the engine to use for compilation
   * @param wasmBytes the WebAssembly bytecode to compile
   * @param options AOT compilation options
   * @param targetPlatform the target platform to compile for
   * @return a serialized module optimized for the target platform
   * @throws WasmException if AOT compilation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  SerializedModule compileModule(
      final Engine engine,
      final byte[] wasmBytes,
      final AotOptions options,
      final TargetPlatform targetPlatform)
      throws WasmException;

  /**
   * Compiles an already-compiled module ahead of time.
   *
   * <p>This method performs AOT compilation on a module that has already been compiled from
   * WebAssembly bytecode. This can be useful for applying different optimization levels or
   * targeting different platforms.
   *
   * @param module the compiled module to process with AOT compilation
   * @param options AOT compilation options
   * @return a serialized module optimized for the current platform
   * @throws WasmException if AOT compilation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  SerializedModule compileModule(final Module module, final AotOptions options)
      throws WasmException;

  /**
   * Compiles an already-compiled module ahead of time for a specific target platform.
   *
   * @param module the compiled module to process with AOT compilation
   * @param options AOT compilation options
   * @param targetPlatform the target platform to compile for
   * @return a serialized module optimized for the target platform
   * @throws WasmException if AOT compilation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  SerializedModule compileModule(
      final Module module, final AotOptions options, final TargetPlatform targetPlatform)
      throws WasmException;

  /**
   * Creates an AOT executable from a serialized module.
   *
   * <p>An AOT executable represents a fully compiled and linked module that can be executed
   * directly without further compilation overhead. This provides the fastest possible startup time.
   *
   * @param module the serialized module to create an executable from
   * @param platform the target platform for the executable
   * @return an AOT executable ready for execution
   * @throws WasmException if executable creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  AotExecutable createExecutable(final SerializedModule module, final TargetPlatform platform)
      throws WasmException;

  /**
   * Creates an AOT executable from a serialized module for the current platform.
   *
   * @param module the serialized module to create an executable from
   * @return an AOT executable ready for execution on the current platform
   * @throws WasmException if executable creation fails
   * @throws IllegalArgumentException if module is null
   */
  AotExecutable createExecutable(final SerializedModule module) throws WasmException;

  /**
   * Gets the list of target platforms supported by this AOT compiler.
   *
   * <p>The supported platforms depend on the underlying Wasmtime configuration and available
   * cross-compilation toolchains.
   *
   * @return an immutable list of supported target platforms
   */
  List<TargetPlatform> getSupportedPlatforms();

  /**
   * Checks if the given target platform is supported for AOT compilation.
   *
   * @param platform the platform to check
   * @return true if the platform is supported, false otherwise
   * @throws IllegalArgumentException if platform is null
   */
  boolean isPlatformSupported(final TargetPlatform platform);

  /**
   * Gets the default AOT options for this compiler.
   *
   * <p>Default options provide reasonable settings for most use cases and can be customized as
   * needed.
   *
   * @return default AOT compilation options
   */
  AotOptions getDefaultOptions();

  /**
   * Validates that the given AOT options are compatible with this compiler.
   *
   * <p>This method checks that all options are supported and compatible with the compiler
   * configuration without performing actual compilation.
   *
   * @param options the AOT options to validate
   * @return true if the options are valid, false otherwise
   * @throws IllegalArgumentException if options is null
   */
  boolean validateOptions(final AotOptions options);

  /**
   * Gets information about the compiler version and capabilities.
   *
   * @return compiler information
   */
  AotCompilerInfo getCompilerInfo();
}
