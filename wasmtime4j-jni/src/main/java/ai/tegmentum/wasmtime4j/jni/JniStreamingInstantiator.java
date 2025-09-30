package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstantiationConfig;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.StreamingInstantiator;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * JNI implementation of StreamingInstantiator.
 *
 * <p>This class provides streaming instantiation capabilities for WebAssembly modules that were
 * compiled through streaming compilation, enabling fast instantiation with pre-optimized modules.
 *
 * @since 1.0.0
 */
public final class JniStreamingInstantiator implements StreamingInstantiator {

  private static final Logger LOGGER = Logger.getLogger(JniStreamingInstantiator.class.getName());

  private final Module module;
  private final InstantiationConfig config;

  /**
   * Creates a new JNI streaming instantiator.
   *
   * @param module the compiled module ready for instantiation
   * @param config instantiation configuration
   */
  JniStreamingInstantiator(final Module module, final InstantiationConfig config) {
    this.module = JniValidation.requireNonNull(module, "module");
    this.config = JniValidation.requireNonNull(config, "config");
    LOGGER.fine("Created JNI streaming instantiator");
  }

  /**
   * Instantiates the WebAssembly module with the given store.
   *
   * <p>This method creates a new instance of the WebAssembly module using the provided store. The
   * instantiation is optimized due to the pre-compiled and prepared module state.
   *
   * @param store the store to create the instance in
   * @return a CompletableFuture that completes with the new instance
   * @throws IllegalArgumentException if store is null
   */
  @Override
  public CompletableFuture<Instance> instantiate(final Store store) {
    JniValidation.requireNonNull(store, "store");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return instantiateSync(store);
          } catch (final WasmException e) {
            throw new RuntimeException("Failed to instantiate module", e);
          }
        });
  }

  /**
   * Instantiates the WebAssembly module synchronously.
   *
   * <p>This method blocks until instantiation is complete and returns the new instance.
   *
   * @param store the store to create the instance in
   * @return the new instance
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if store is null
   */
  @Override
  public Instance instantiateSync(final Store store) throws WasmException {
    JniValidation.requireNonNull(store, "store");

    try {
      // Use the standard instantiation process
      // In a real implementation, this would use optimized instantiation paths
      return module.instantiate(store);
    } catch (final Exception e) {
      throw new WasmException("Failed to instantiate streaming module: " + e.getMessage(), e);
    }
  }

  /**
   * Instantiates the WebAssembly module with the given store and linker.
   *
   * <p>This method creates a new instance using the provided store and linker for import
   * resolution. The linker provides imports that the module requires.
   *
   * @param store the store to create the instance in
   * @param linker the linker for import resolution
   * @return a CompletableFuture that completes with the new instance
   * @throws IllegalArgumentException if store or linker is null
   */
  @Override
  public CompletableFuture<Instance> instantiate(final Store store, final Linker linker) {
    JniValidation.requireNonNull(store, "store");
    JniValidation.requireNonNull(linker, "linker");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return instantiateSync(store, linker);
          } catch (final WasmException e) {
            throw new RuntimeException("Failed to instantiate module with linker", e);
          }
        });
  }

  /**
   * Instantiates the WebAssembly module synchronously with linker.
   *
   * <p>This method blocks until instantiation is complete and returns the new instance.
   *
   * @param store the store to create the instance in
   * @param linker the linker for import resolution
   * @return the new instance
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if store or linker is null
   */
  @Override
  public Instance instantiateSync(final Store store, final Linker linker) throws WasmException {
    JniValidation.requireNonNull(store, "store");
    JniValidation.requireNonNull(linker, "linker");

    try {
      // Use linker for instantiation
      return linker.instantiate(store, module);
    } catch (final Exception e) {
      throw new WasmException(
          "Failed to instantiate streaming module with linker: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the compiled module.
   *
   * @return the compiled WebAssembly module
   */
  @Override
  public Module getModule() {
    return module;
  }

  /**
   * Gets the instantiation configuration.
   *
   * @return the instantiation configuration
   */
  @Override
  public InstantiationConfig getConfig() {
    return config;
  }

  /**
   * Checks if the module is ready for instantiation.
   *
   * <p>For streaming instantiators, this typically returns true since the module has already been
   * compiled and prepared.
   *
   * @return true if ready for instantiation
   */
  @Override
  public boolean isReady() {
    return true; // Streaming instantiators are always ready after compilation
  }

  /**
   * Gets information about the module's imports.
   *
   * @return array of import information
   */
  @Override
  public String[] getImports() {
    try {
      return module.getImports();
    } catch (final Exception e) {
      LOGGER.warning("Failed to get module imports: " + e.getMessage());
      return new String[0];
    }
  }

  /**
   * Gets information about the module's exports.
   *
   * @return array of export information
   */
  @Override
  public String[] getExports() {
    try {
      return module.getExports();
    } catch (final Exception e) {
      LOGGER.warning("Failed to get module exports: " + e.getMessage());
      return new String[0];
    }
  }

  /**
   * Estimates the memory requirements for instantiation.
   *
   * @return estimated memory usage in bytes
   */
  @Override
  public long getEstimatedMemoryUsage() {
    // This is a rough estimate - in a real implementation, this would
    // analyze the module's memory requirements more precisely
    try {
      // Base estimate: module size plus some overhead
      return module.getSerializedSize() + (1024 * 1024); // 1MB overhead
    } catch (final Exception e) {
      LOGGER.warning("Failed to estimate memory usage: " + e.getMessage());
      return 1024 * 1024; // Default 1MB estimate
    }
  }

  @Override
  public String toString() {
    return "JniStreamingInstantiator{"
        + "module="
        + module
        + ", config="
        + config
        + ", ready="
        + isReady()
        + ", estimatedMemory="
        + getEstimatedMemoryUsage() / (1024 * 1024)
        + "MB"
        + '}';
  }
}
