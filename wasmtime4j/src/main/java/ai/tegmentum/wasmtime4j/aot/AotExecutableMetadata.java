package ai.tegmentum.wasmtime4j.aot;

import ai.tegmentum.wasmtime4j.serialization.TargetPlatform;
import java.time.Instant;

/**
 * Metadata associated with an AOT-compiled WebAssembly executable.
 *
 * <p>AotExecutableMetadata provides information about the compilation process, target platform, and
 * other details relevant to the AOT executable.
 *
 * @since 1.0.0
 */
public interface AotExecutableMetadata {

  /**
   * Gets the version of the AOT compiler used to create this executable.
   *
   * @return the compiler version
   */
  String getCompilerVersion();

  /**
   * Gets the target platform this executable was compiled for.
   *
   * @return the target platform
   */
  TargetPlatform getTargetPlatform();

  /**
   * Gets the timestamp when this executable was compiled.
   *
   * @return the compilation timestamp
   */
  Instant getCompilationTimestamp();

  /**
   * Gets the AOT options used during compilation.
   *
   * @return the compilation options
   */
  AotOptions getCompilationOptions();

  /**
   * Gets the hash of the original WebAssembly module.
   *
   * @return the original module hash
   */
  String getOriginalModuleHash();

  /**
   * Gets the size of the original WebAssembly module in bytes.
   *
   * @return the original module size
   */
  long getOriginalModuleSize();

  /**
   * Gets the size of the compiled executable in bytes.
   *
   * @return the executable size
   */
  long getExecutableSize();

  /**
   * Gets the compilation time in milliseconds.
   *
   * @return the compilation time
   */
  long getCompilationTimeMs();

  /**
   * Checks if debug information was preserved in the executable.
   *
   * @return true if debug information is preserved, false otherwise
   */
  boolean hasDebugInfo();

  /**
   * Gets additional metadata properties.
   *
   * @return a string representation of additional metadata
   */
  String getAdditionalInfo();
}
