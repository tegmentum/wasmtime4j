package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmFeature;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JNI implementation for experimental WebAssembly features.
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
public final class JniExperimentalFeatures {

  private static final Logger logger = Logger.getLogger(JniExperimentalFeatures.class.getName());

  private long nativeConfigPtr = 0L;

  static {
    // Load the native library
    try {
      System.loadLibrary("wasmtime4j_native");
    } catch (final UnsatisfiedLinkError e) {
      logger.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Failed to load wasmtime4j native library", e);
    }
  }

  /**
   * Creates a new experimental features configuration with default settings. All experimental
   * features are disabled by default.
   */
  public JniExperimentalFeatures() {
    this.nativeConfigPtr = nativeCreateExperimentalConfig();
    if (this.nativeConfigPtr == 0L) {
      throw new RuntimeException("Failed to create experimental features configuration");
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
  public static JniExperimentalFeatures allExperimentalEnabled() {
    final JniExperimentalFeatures config = new JniExperimentalFeatures();
    config.nativeConfigPtr = nativeCreateAllExperimentalConfig();
    if (config.nativeConfigPtr == 0L) {
      throw new RuntimeException("Failed to create all-experimental features configuration");
    }
    return config;
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
  public JniExperimentalFeatures enableStackSwitching(
      final long stackSize, final int maxConcurrentStacks) {
    if (stackSize < 4096) {
      throw new IllegalArgumentException("Stack size must be at least 4KB");
    }
    if (maxConcurrentStacks <= 0) {
      throw new IllegalArgumentException("Maximum concurrent stacks must be greater than zero");
    }

    if (!nativeEnableStackSwitching(nativeConfigPtr, stackSize, maxConcurrentStacks)) {
      throw new RuntimeException("Failed to enable stack switching");
    }

    logger.info(
        "Stack switching enabled with stack size: "
            + stackSize
            + " bytes, max concurrent stacks: "
            + maxConcurrentStacks);
    return this;
  }

  /**
   * Enables call/cc (call-with-current-continuation) support.
   *
   * <p>Call/CC allows capturing the current execution state as a continuation that can be invoked
   * later, enabling powerful control flow operations.
   *
   * @param maxContinuations maximum number of continuations that can be captured
   * @param storageStrategy storage strategy for continuations (0=Stack, 1=Heap, 2=Hybrid)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if maxContinuations is 0 or storageStrategy is invalid
   * @throws RuntimeException if the native operation fails
   */
  public JniExperimentalFeatures enableCallCc(
      final int maxContinuations, final ContinuationStorageStrategy storageStrategy) {
    if (maxContinuations <= 0) {
      throw new IllegalArgumentException("Maximum continuations must be greater than zero");
    }
    if (storageStrategy == null) {
      throw new IllegalArgumentException("Storage strategy cannot be null");
    }

    if (!nativeEnableCallCc(nativeConfigPtr, maxContinuations, storageStrategy.ordinal())) {
      throw new RuntimeException("Failed to enable call/cc");
    }

    logger.info(
        "Call/CC enabled with max continuations: "
            + maxContinuations
            + ", storage strategy: "
            + storageStrategy);
    return this;
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
  public JniExperimentalFeatures enableExtendedConstExpressions(
      final boolean importBasedExpressions,
      final boolean globalDependencies,
      final ConstantFoldingLevel foldingLevel) {

    if (foldingLevel == null) {
      throw new IllegalArgumentException("Folding level cannot be null");
    }

    if (!nativeEnableExtendedConstExpressions(
        nativeConfigPtr, importBasedExpressions, globalDependencies, foldingLevel.ordinal())) {
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
  }

  /**
   * Applies the experimental features configuration to a Wasmtime configuration.
   *
   * @param wasmtimeConfigPtr native pointer to the Wasmtime Config object
   * @return true if the configuration was applied successfully
   * @throws RuntimeException if the native operation fails
   */
  public boolean applyToWasmtimeConfig(final long wasmtimeConfigPtr) {
    if (wasmtimeConfigPtr == 0L) {
      throw new IllegalArgumentException("Wasmtime config pointer cannot be null");
    }

    return nativeApplyExperimentalFeatures(nativeConfigPtr, wasmtimeConfigPtr);
  }

  /**
   * Checks if this experimental features configuration is valid.
   *
   * @return true if the configuration is valid and ready to use
   */
  public boolean isValid() {
    return nativeConfigPtr != 0L;
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
   * Enables flexible vectors support for dynamic vector operations.
   *
   * <p>Flexible vectors allow WebAssembly modules to use dynamic vector sizing and advanced SIMD
   * operations with runtime optimization.
   *
   * @param dynamicSizing whether to enable dynamic vector sizing
   * @param autoVectorization whether to enable automatic vectorization
   * @param simdIntegration whether to enable SIMD integration
   * @return this configuration for method chaining
   * @throws RuntimeException if the native operation fails
   */
  public JniExperimentalFeatures enableFlexibleVectors(
      final boolean dynamicSizing, final boolean autoVectorization, final boolean simdIntegration) {

    if (!nativeEnableFlexibleVectors(
        nativeConfigPtr, dynamicSizing, autoVectorization, simdIntegration)) {
      throw new RuntimeException("Failed to enable flexible vectors");
    }

    logger.info(
        "Flexible vectors enabled: dynamic-sizing="
            + dynamicSizing
            + ", auto-vectorization="
            + autoVectorization
            + ", simd-integration="
            + simdIntegration);
    return this;
  }

  /**
   * Enables string imports support for efficient string handling.
   *
   * <p>String imports allow WebAssembly modules to import and export strings efficiently with
   * multiple encoding formats and optimization strategies.
   *
   * @param encodingFormat string encoding format to use
   * @param stringInterning whether to enable string interning for deduplication
   * @param lazyDecoding whether to enable lazy decoding for performance
   * @param jsInterop whether to enable JavaScript interop support
   * @return this configuration for method chaining
   * @throws RuntimeException if the native operation fails
   */
  public JniExperimentalFeatures enableStringImports(
      final StringEncodingFormat encodingFormat,
      final boolean stringInterning,
      final boolean lazyDecoding,
      final boolean jsInterop) {

    if (encodingFormat == null) {
      throw new IllegalArgumentException("Encoding format cannot be null");
    }

    if (!nativeEnableStringImports(
        nativeConfigPtr, encodingFormat.ordinal(), stringInterning, lazyDecoding, jsInterop)) {
      throw new RuntimeException("Failed to enable string imports");
    }

    logger.info(
        "String imports enabled: encoding="
            + encodingFormat
            + ", interning="
            + stringInterning
            + ", lazy-decoding="
            + lazyDecoding
            + ", js-interop="
            + jsInterop);
    return this;
  }

  /**
   * Enables resource types support for advanced resource management.
   *
   * <p>Resource types provide WebAssembly modules with sophisticated resource management
   * capabilities including automatic cleanup and reference counting.
   *
   * @param automaticCleanup whether to enable automatic resource cleanup
   * @param referenceCounting whether to enable reference counting
   * @param cleanupStrategy cleanup strategy for resource management
   * @return this configuration for method chaining
   * @throws RuntimeException if the native operation fails
   */
  public JniExperimentalFeatures enableResourceTypes(
      final boolean automaticCleanup,
      final boolean referenceCounting,
      final ResourceCleanupStrategy cleanupStrategy) {

    if (cleanupStrategy == null) {
      throw new IllegalArgumentException("Cleanup strategy cannot be null");
    }

    if (!nativeEnableResourceTypes(
        nativeConfigPtr, automaticCleanup, referenceCounting, cleanupStrategy.ordinal())) {
      throw new RuntimeException("Failed to enable resource types");
    }

    logger.info(
        "Resource types enabled: auto-cleanup="
            + automaticCleanup
            + ", ref-counting="
            + referenceCounting
            + ", cleanup-strategy="
            + cleanupStrategy);
    return this;
  }

  /**
   * Enables type imports support for dynamic type system integration.
   *
   * <p>Type imports allow WebAssembly modules to import and export types dynamically with
   * configurable validation and resolution mechanisms.
   *
   * @param validationStrategy type validation strategy to use
   * @param resolutionMechanism import resolution mechanism
   * @param structuralCompatibility whether to enable structural compatibility checking
   * @return this configuration for method chaining
   * @throws RuntimeException if the native operation fails
   */
  public JniExperimentalFeatures enableTypeImports(
      final TypeValidationStrategy validationStrategy,
      final ImportResolutionMechanism resolutionMechanism,
      final boolean structuralCompatibility) {

    if (validationStrategy == null) {
      throw new IllegalArgumentException("Validation strategy cannot be null");
    }
    if (resolutionMechanism == null) {
      throw new IllegalArgumentException("Resolution mechanism cannot be null");
    }

    if (!nativeEnableTypeImports(
        nativeConfigPtr,
        validationStrategy.ordinal(),
        resolutionMechanism.ordinal(),
        structuralCompatibility)) {
      throw new RuntimeException("Failed to enable type imports");
    }

    logger.info(
        "Type imports enabled: validation="
            + validationStrategy
            + ", resolution="
            + resolutionMechanism
            + ", structural-compat="
            + structuralCompatibility);
    return this;
  }

  /**
   * Enables shared-everything threads support for advanced concurrency.
   *
   * <p>Shared-everything threads enhance the WebAssembly threads proposal with shared state
   * management and advanced synchronization primitives.
   *
   * @param minThreads minimum number of threads in the thread pool
   * @param maxThreads maximum number of threads in the thread pool
   * @param globalStateSharing whether to enable global state sharing
   * @param atomicOperations whether to enable atomic operations
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if thread counts are invalid
   * @throws RuntimeException if the native operation fails
   */
  public JniExperimentalFeatures enableSharedEverythingThreads(
      final int minThreads,
      final int maxThreads,
      final boolean globalStateSharing,
      final boolean atomicOperations) {

    if (minThreads <= 0) {
      throw new IllegalArgumentException("Minimum threads must be greater than zero");
    }
    if (maxThreads < minThreads) {
      throw new IllegalArgumentException("Maximum threads cannot be less than minimum threads");
    }

    if (!nativeEnableSharedEverythingThreads(
        nativeConfigPtr, minThreads, maxThreads, globalStateSharing, atomicOperations)) {
      throw new RuntimeException("Failed to enable shared-everything threads");
    }

    logger.info(
        "Shared-everything threads enabled: min-threads="
            + minThreads
            + ", max-threads="
            + maxThreads
            + ", global-state="
            + globalStateSharing
            + ", atomic-ops="
            + atomicOperations);
    return this;
  }

  /**
   * Enables custom page sizes support for flexible memory management.
   *
   * <p>Custom page sizes allow WebAssembly modules to use non-standard memory page sizes for
   * optimized memory access patterns.
   *
   * @param pageSize custom page size in bytes
   * @param strategy page size strategy to use
   * @param strictAlignment whether to enforce strict alignment requirements
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if page size is invalid
   * @throws RuntimeException if the native operation fails
   */
  public JniExperimentalFeatures enableCustomPageSizes(
      final int pageSize, final PageSizeStrategy strategy, final boolean strictAlignment) {

    if (pageSize <= 0 || (pageSize & (pageSize - 1)) != 0) {
      throw new IllegalArgumentException("Page size must be a positive power of 2");
    }
    if (strategy == null) {
      throw new IllegalArgumentException("Page size strategy cannot be null");
    }

    if (!nativeEnableCustomPageSizes(
        nativeConfigPtr, pageSize, strategy.ordinal(), strictAlignment)) {
      throw new RuntimeException("Failed to enable custom page sizes");
    }

    logger.info(
        "Custom page sizes enabled: page-size="
            + pageSize
            + ", strategy="
            + strategy
            + ", strict-alignment="
            + strictAlignment);
    return this;
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
   * Checks if a specific experimental feature is supported by the current Wasmtime version.
   *
   * @param feature the experimental feature to check
   * @return true if the feature is supported
   */
  public static boolean isExperimentalFeatureSupported(final ExperimentalFeatureId feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }
    return nativeGetFeatureSupport(feature.getId());
  }

  /**
   * Gets information about which experimental features are currently supported.
   *
   * @return a set of supported experimental features
   */
  public static Set<WasmFeature> getSupportedExperimentalFeatures() {
    Set<WasmFeature> supportedFeatures = EnumSet.noneOf(WasmFeature.class);

    // Check which experimental features are actually supported
    if (isExperimentalFeatureSupported(ExperimentalFeatureId.STACK_SWITCHING)) {
      supportedFeatures.add(WasmFeature.STACK_SWITCHING);
    }
    if (isExperimentalFeatureSupported(ExperimentalFeatureId.EXTENDED_CONST_EXPRESSIONS)) {
      supportedFeatures.add(WasmFeature.EXTENDED_CONST_EXPRESSIONS);
    }
    if (isExperimentalFeatureSupported(ExperimentalFeatureId.MEMORY64_EXTENDED)) {
      supportedFeatures.add(WasmFeature.MEMORY64_EXTENDED);
    }
    if (isExperimentalFeatureSupported(ExperimentalFeatureId.FLEXIBLE_VECTORS)) {
      supportedFeatures.add(WasmFeature.FLEXIBLE_VECTORS);
    }
    if (isExperimentalFeatureSupported(ExperimentalFeatureId.STRING_IMPORTS)) {
      supportedFeatures.add(WasmFeature.STRING_IMPORTS);
    }
    if (isExperimentalFeatureSupported(ExperimentalFeatureId.RESOURCE_TYPES)) {
      supportedFeatures.add(WasmFeature.RESOURCE_TYPES);
    }
    if (isExperimentalFeatureSupported(ExperimentalFeatureId.TYPE_IMPORTS)) {
      supportedFeatures.add(WasmFeature.TYPE_IMPORTS);
    }
    if (isExperimentalFeatureSupported(ExperimentalFeatureId.SHARED_EVERYTHING_THREADS)) {
      supportedFeatures.add(WasmFeature.SHARED_EVERYTHING_THREADS);
    }
    if (isExperimentalFeatureSupported(ExperimentalFeatureId.CUSTOM_PAGE_SIZES)) {
      supportedFeatures.add(WasmFeature.CUSTOM_PAGE_SIZES);
    }

    return supportedFeatures;
  }

  /**
   * Disposes of native resources associated with this configuration. After calling this method, the
   * configuration should not be used.
   */
  public void dispose() {
    if (nativeConfigPtr != 0L) {
      nativeDestroyExperimentalConfig(nativeConfigPtr);
      nativeConfigPtr = 0L;
    }
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      dispose();
    } finally {
      super.finalize();
    }
  }

  // Native method declarations

  private static native long nativeCreateExperimentalConfig();

  private static native long nativeCreateAllExperimentalConfig();

  private static native boolean nativeEnableStackSwitching(
      long configPtr, long stackSize, int maxStacks);

  private static native boolean nativeEnableCallCc(
      long configPtr, int maxContinuations, int storageStrategy);

  private static native boolean nativeEnableExtendedConstExpressions(
      long configPtr, boolean importBased, boolean globalDeps, int foldingLevel);

  private static native boolean nativeApplyExperimentalFeatures(
      long experimentalConfigPtr, long wasmtimeConfigPtr);

  private static native void nativeDestroyExperimentalConfig(long configPtr);

  private static native boolean nativeEnableFlexibleVectors(
      long configPtr, boolean dynamicSizing, boolean autoVectorization, boolean simdIntegration);

  private static native boolean nativeEnableStringImports(
      long configPtr,
      int encodingFormat,
      boolean stringInterning,
      boolean lazyDecoding,
      boolean jsInterop);

  private static native boolean nativeEnableResourceTypes(
      long configPtr, boolean automaticCleanup, boolean referenceCounting, int cleanupStrategy);

  private static native boolean nativeEnableTypeImports(
      long configPtr,
      int validationStrategy,
      int resolutionMechanism,
      boolean structuralCompatibility);

  private static native boolean nativeEnableSharedEverythingThreads(
      long configPtr,
      int minThreads,
      int maxThreads,
      boolean globalStateSharing,
      boolean atomicOperations);

  private static native boolean nativeEnableCustomPageSizes(
      long configPtr, int pageSize, int strategy, boolean strictAlignment);

  private static native boolean nativeGetFeatureSupport(int featureId);

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

  /** String encoding format for string imports. */
  public enum StringEncodingFormat {
    /** UTF-8 encoding (standard and efficient). */
    UTF8,
    /** UTF-16 encoding (good for JavaScript interop). */
    UTF16,
    /** Latin-1 encoding (efficient for ASCII data). */
    LATIN1,
    /** Custom encoding format. */
    CUSTOM
  }

  /** Resource cleanup strategy for resource types. */
  public enum ResourceCleanupStrategy {
    /** Manual cleanup (user-controlled). */
    MANUAL,
    /** Automatic cleanup (garbage collection integrated). */
    AUTOMATIC,
    /** Hybrid approach (user-controlled with automatic fallback). */
    HYBRID
  }

  /** Type validation strategy for type imports. */
  public enum TypeValidationStrategy {
    /** Strict validation (enforce all type constraints). */
    STRICT,
    /** Relaxed validation (allow compatible type coercions). */
    RELAXED,
    /** Dynamic validation (runtime type checking). */
    DYNAMIC
  }

  /** Import resolution mechanism for type imports. */
  public enum ImportResolutionMechanism {
    /** Static resolution (compile-time binding). */
    STATIC,
    /** Dynamic resolution (runtime binding). */
    DYNAMIC,
    /** Lazy resolution (on-demand binding). */
    LAZY
  }

  /** Page size strategy for custom page sizes. */
  public enum PageSizeStrategy {
    /** Use system default page size. */
    SYSTEM,
    /** Use optimal page size for platform. */
    OPTIMAL,
    /** Use custom specified page size. */
    CUSTOM
  }

  /** Experimental feature identifier for feature detection. */
  public enum ExperimentalFeatureId {
    STACK_SWITCHING(0),
    CALL_CC(1),
    EXTENDED_CONST_EXPRESSIONS(2),
    MEMORY64_EXTENDED(3),
    CUSTOM_PAGE_SIZES(4),
    SHARED_EVERYTHING_THREADS(5),
    TYPE_IMPORTS(6),
    STRING_IMPORTS(7),
    RESOURCE_TYPES(8),
    INTERFACE_TYPES(9),
    FLEXIBLE_VECTORS(10);

    private final int id;

    ExperimentalFeatureId(final int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }
  }
}
