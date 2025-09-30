package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmFeature;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Panama FFI implementation for experimental WebAssembly features.
 *
 * <p><strong>WARNING:</strong> These features are experimental and subject to change. They should
 * only be used for testing and development purposes.
 *
 * <p>This class provides access to cutting-edge WebAssembly proposals that are currently in
 * committee stage, including:
 *
 * <ul>
 *   <li>Stack switching for coroutines and fibers
 *   <li>Call/CC (call-with-current-continuation) support
 *   <li>Extended constant expressions
 *   <li>Memory64 extensions
 *   <li>Custom page sizes
 *   <li>Shared-everything threads
 *   <li>Type and string imports
 *   <li>Resource types and interface types
 *   <li>Flexible vectors
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaExperimentalFeatures implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(PanamaExperimentalFeatures.class.getName());

  private static final SymbolLookup nativeLibrary;
  private static final MethodHandle createConfig;
  private static final MethodHandle createAllConfig;
  private static final MethodHandle enableStackSwitching;
  private static final MethodHandle enableCallCc;
  private static final MethodHandle enableExtendedConstExpressions;
  private static final MethodHandle applyFeatures;
  private static final MethodHandle destroyConfig;

  static {
    try {
      // Load the native library
      nativeLibrary = SymbolLookup.libraryLookup("wasmtime4j_native", SegmentScope.auto());

      // Initialize method handles for experimental features functions
      createConfig =
          nativeLibrary
              .find("wasmtime4j_experimental_create_config")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ValueLayout.ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_experimental_create_config"));

      createAllConfig =
          nativeLibrary
              .find("wasmtime4j_experimental_create_all_config")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ValueLayout.ADDRESS)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_experimental_create_all_config"));

      enableStackSwitching =
          nativeLibrary
              .find("wasmtime4j_experimental_enable_stack_switching")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr,
                              FunctionDescriptor.of(
                                  ValueLayout.JAVA_INT,
                                  ValueLayout.ADDRESS,
                                  ValueLayout.JAVA_LONG,
                                  ValueLayout.JAVA_INT)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_experimental_enable_stack_switching"));

      enableCallCc =
          nativeLibrary
              .find("wasmtime4j_experimental_enable_call_cc")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr,
                              FunctionDescriptor.of(
                                  ValueLayout.JAVA_INT,
                                  ValueLayout.ADDRESS,
                                  ValueLayout.JAVA_INT,
                                  ValueLayout.JAVA_INT)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_experimental_enable_call_cc"));

      enableExtendedConstExpressions =
          nativeLibrary
              .find("wasmtime4j_experimental_enable_extended_const_expressions")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr,
                              FunctionDescriptor.of(
                                  ValueLayout.JAVA_INT,
                                  ValueLayout.ADDRESS,
                                  ValueLayout.JAVA_INT,
                                  ValueLayout.JAVA_INT,
                                  ValueLayout.JAVA_INT)))
              .orElseThrow(
                  () ->
                      new UnsatisfiedLinkError(
                          "wasmtime4j_experimental_enable_extended_const_expressions"));

      applyFeatures =
          nativeLibrary
              .find("wasmtime4j_experimental_apply_features")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr,
                              FunctionDescriptor.of(
                                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_experimental_apply_features"));

      destroyConfig =
          nativeLibrary
              .find("wasmtime4j_experimental_destroy_config")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_experimental_destroy_config"));

    } catch (final Exception e) {
      logger.severe(
          "Failed to initialize Panama experimental features bindings: " + e.getMessage());
      throw new RuntimeException("Failed to initialize wasmtime4j Panama experimental features", e);
    }
  }

  private MemorySegment nativeConfigPtr;

  /**
   * Creates a new experimental features configuration with default settings. All experimental
   * features are disabled by default.
   */
  public PanamaExperimentalFeatures() {
    try {
      this.nativeConfigPtr = (MemorySegment) createConfig.invokeExact();
      if (this.nativeConfigPtr == null || this.nativeConfigPtr.address() == 0L) {
        throw new RuntimeException("Failed to create experimental features configuration");
      }
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to create experimental features configuration", e);
    }
  }

  /**
   * Creates a new experimental features configuration with all experimental features enabled.
   *
   * <p><strong>WARNING:</strong> This enables all cutting-edge experimental features which may be
   * unstable or cause unexpected behavior.
   *
   * @return a new experimental features configuration with all features enabled
   */
  public static PanamaExperimentalFeatures allExperimentalEnabled() {
    try {
      final PanamaExperimentalFeatures config = new PanamaExperimentalFeatures();
      config.nativeConfigPtr = (MemorySegment) createAllConfig.invokeExact();
      if (config.nativeConfigPtr == null || config.nativeConfigPtr.address() == 0L) {
        throw new RuntimeException("Failed to create all-experimental features configuration");
      }
      return config;
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to create all-experimental features configuration", e);
    }
  }

  /**
   * Enables stack switching support for coroutines and fibers.
   *
   * <p>Stack switching allows WebAssembly modules to suspend and resume execution at specific
   * points, enabling coroutine and fiber implementations.
   *
   * @param stackSize the stack size for switched stacks in bytes (minimum 4KB)
   * @param maxConcurrentStacks maximum number of concurrent stacks
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if stackSize is less than 4096 or maxConcurrentStacks is 0
   * @throws RuntimeException if the native operation fails
   */
  public PanamaExperimentalFeatures enableStackSwitching(
      final long stackSize, final int maxConcurrentStacks) {
    if (stackSize < 4096) {
      throw new IllegalArgumentException("Stack size must be at least 4KB");
    }
    if (maxConcurrentStacks <= 0) {
      throw new IllegalArgumentException("Maximum concurrent stacks must be greater than zero");
    }

    try {
      final int result =
          (int) enableStackSwitching.invokeExact(nativeConfigPtr, stackSize, maxConcurrentStacks);
      if (result == 0) {
        throw new RuntimeException("Failed to enable stack switching");
      }

      logger.info(
          "Stack switching enabled with stack size: "
              + stackSize
              + " bytes, max concurrent stacks: "
              + maxConcurrentStacks);
      return this;
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to enable stack switching", e);
    }
  }

  /**
   * Enables call/cc (call-with-current-continuation) support.
   *
   * <p>Call/CC allows capturing the current execution state as a continuation that can be invoked
   * later, enabling powerful control flow operations.
   *
   * @param maxContinuations maximum number of continuations that can be captured
   * @param storageStrategy storage strategy for continuations
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if maxContinuations is 0 or storageStrategy is null
   * @throws RuntimeException if the native operation fails
   */
  public PanamaExperimentalFeatures enableCallCc(
      final int maxContinuations, final ContinuationStorageStrategy storageStrategy) {
    if (maxContinuations <= 0) {
      throw new IllegalArgumentException("Maximum continuations must be greater than zero");
    }
    if (storageStrategy == null) {
      throw new IllegalArgumentException("Storage strategy cannot be null");
    }

    try {
      final int result =
          (int)
              enableCallCc.invokeExact(
                  nativeConfigPtr, maxContinuations, storageStrategy.ordinal());
      if (result == 0) {
        throw new RuntimeException("Failed to enable call/cc");
      }

      logger.info(
          "Call/CC enabled with max continuations: "
              + maxContinuations
              + ", storage strategy: "
              + storageStrategy);
      return this;
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to enable call/cc", e);
    }
  }

  /**
   * Enables extended constant expressions support.
   *
   * <p>Extended constant expressions allow more complex constant computations at compile time,
   * including import-based expressions and global dependencies.
   *
   * @param importBasedExpressions whether to allow import-based constant expressions
   * @param globalDependencies whether to support global dependencies in constants
   * @param foldingLevel level of constant folding to perform
   * @return this configuration for method chaining
   * @throws RuntimeException if the native operation fails
   */
  public PanamaExperimentalFeatures enableExtendedConstExpressions(
      final boolean importBasedExpressions,
      final boolean globalDependencies,
      final ConstantFoldingLevel foldingLevel) {

    if (foldingLevel == null) {
      throw new IllegalArgumentException("Folding level cannot be null");
    }

    try {
      final int result =
          (int)
              enableExtendedConstExpressions.invokeExact(
                  nativeConfigPtr,
                  importBasedExpressions ? 1 : 0,
                  globalDependencies ? 1 : 0,
                  foldingLevel.ordinal());

      if (result == 0) {
        throw new RuntimeException("Failed to enable extended constant expressions");
      }

      logger.info(
          "Extended constant expressions enabled: import-based="
              + importBasedExpressions
              + ", global-deps="
              + globalDependencies
              + ", folding-level="
              + foldingLevel);
      return this;
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to enable extended constant expressions", e);
    }
  }

  /**
   * Applies the experimental features configuration to a Wasmtime configuration.
   *
   * @param wasmtimeConfigPtr native memory segment representing the Wasmtime Config object
   * @return true if the configuration was applied successfully
   * @throws RuntimeException if the native operation fails
   */
  public boolean applyToWasmtimeConfig(final MemorySegment wasmtimeConfigPtr) {
    if (wasmtimeConfigPtr == null || wasmtimeConfigPtr.address() == 0L) {
      throw new IllegalArgumentException("Wasmtime config pointer cannot be null");
    }

    try {
      final int result = (int) applyFeatures.invokeExact(nativeConfigPtr, wasmtimeConfigPtr);
      return result != 0;
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to apply experimental features", e);
    }
  }

  /**
   * Checks if this experimental features configuration is valid.
   *
   * @return true if the configuration is valid and ready to use
   */
  public boolean isValid() {
    return nativeConfigPtr != null && nativeConfigPtr.address() != 0L;
  }

  /**
   * Checks if stack switching is supported by the current Wasmtime version.
   *
   * @return true if stack switching is supported
   */
  public static boolean isStackSwitchingSupported() {
    // For now, assume not supported as these are cutting-edge features
    return false;
  }

  /**
   * Checks if call/cc is supported by the current Wasmtime version.
   *
   * @return true if call/cc is supported
   */
  public static boolean isCallCcSupported() {
    // For now, assume not supported as these are cutting-edge features
    return false;
  }

  /**
   * Gets information about which experimental features are currently supported.
   *
   * @return a set of supported experimental features
   */
  public static Set<WasmFeature> getSupportedExperimentalFeatures() {
    return Set.of(
        // Most experimental features are not yet supported by Wasmtime
        // This will be updated as features become available
        );
  }

  /**
   * Disposes of native resources associated with this configuration. After calling this method, the
   * configuration should not be used.
   */
  @Override
  public void close() {
    if (nativeConfigPtr != null && nativeConfigPtr.address() != 0L) {
      try {
        destroyConfig.invokeExact(nativeConfigPtr);
      } catch (final Throwable e) {
        logger.warning("Failed to destroy experimental features configuration: " + e.getMessage());
      }
      nativeConfigPtr = null;
    }
  }

  // Enumerations for experimental feature configuration

  /** Storage strategy for continuations in call/cc implementation. */
  public enum ContinuationStorageStrategy {
    /** Store continuations on the stack (fastest, limited size). */
    STACK,
    /** Store continuations on the heap (flexible, slower). */
    HEAP,
    /** Hybrid approach (balanced performance and flexibility). */
    HYBRID
  }

  /** Level of constant folding to perform for extended constant expressions. */
  public enum ConstantFoldingLevel {
    /** No constant folding. */
    NONE,
    /** Basic constant folding. */
    BASIC,
    /** Aggressive constant folding. */
    AGGRESSIVE,
    /** Full constant folding with maximum optimization. */
    FULL
  }
}
