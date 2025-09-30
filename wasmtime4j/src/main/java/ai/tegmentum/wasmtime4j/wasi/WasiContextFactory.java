package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Factory for creating WASI contexts based on configuration.
 *
 * <p>This factory provides a unified interface for creating both WASI Preview 1 and Preview 2
 * contexts based on the configuration. It handles runtime selection between JNI and Panama
 * implementations automatically.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create Preview 1 context
 * WasiConfig preview1Config = WasiConfig.builder()
 *     .withWasiVersion(WasiVersion.PREVIEW_1)
 *     .build();
 * WasiContext context1 = WasiContextFactory.createContext(preview1Config);
 *
 * // Create Preview 2 context with async support
 * WasiConfig preview2Config = WasiConfig.builder()
 *     .withWasiVersion(WasiVersion.PREVIEW_2)
 *     .withAsyncOperations(true)
 *     .build();
 * WasiPreview2Context context2 = WasiContextFactory.createPreview2Context(preview2Config);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiContextFactory {

  private WasiContextFactory() {
    // Utility class - prevent instantiation
  }

  /**
   * Creates a WASI context based on the specified configuration.
   *
   * <p>This method automatically selects the appropriate WASI version based on the configuration
   * and returns either a Preview 1 or Preview 2 context.
   *
   * @param config the WASI configuration
   * @return a WASI context for the specified version
   * @throws IllegalArgumentException if the configuration is invalid
   * @throws UnsupportedOperationException if the requested WASI version is not supported
   */
  public static WasiContext createContext(final WasiConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }

    config.validate();

    switch (config.getWasiVersion()) {
      case PREVIEW_1:
        return createPreview1Context(config);
      case PREVIEW_2:
        // For general context interface, create a wrapper around Preview 2
        return new WasiPreview2ContextWrapper(createPreview2Context(config));
      default:
        throw new UnsupportedOperationException(
            "WASI version not supported: " + config.getWasiVersion());
    }
  }

  /**
   * Creates a WASI Preview 1 context with the specified configuration.
   *
   * @param config the WASI configuration (must specify Preview 1)
   * @return a WASI Preview 1 context
   * @throws IllegalArgumentException if the configuration doesn't specify Preview 1
   */
  public static WasiContext createPreview1Context(final WasiConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }

    if (config.getWasiVersion() != WasiVersion.PREVIEW_1) {
      throw new IllegalArgumentException("Configuration must specify WASI Preview 1");
    }

    config.validate();

    // Use runtime selection pattern to find appropriate implementation
    try {
      // Try Panama implementation first
      final Class<?> contextClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.wasi.PanamaWasiContext");
      return (WasiContext) contextClass.getMethod("create", WasiConfig.class).invoke(null, config);
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> contextClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.JniWasiContext");
        return (WasiContext)
            contextClass.getMethod("create", WasiConfig.class).invoke(null, config);
      } catch (final ClassNotFoundException e2) {
        throw new RuntimeException(
            "No WasiContext implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create WASI Preview 1 context", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create WASI Preview 1 context", e);
    }
  }

  /**
   * Creates a WASI Preview 2 context with the specified configuration.
   *
   * @param config the WASI configuration (must specify Preview 2)
   * @return a WASI Preview 2 context
   * @throws IllegalArgumentException if the configuration doesn't specify Preview 2
   */
  public static WasiPreview2Context createPreview2Context(final WasiConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }

    if (config.getWasiVersion() != WasiVersion.PREVIEW_2) {
      throw new IllegalArgumentException("Configuration must specify WASI Preview 2");
    }

    config.validate();

    return WasiPreview2Context.create(config);
  }

  /**
   * Determines if WASI Preview 2 is supported in the current runtime.
   *
   * @return true if Preview 2 is supported, false otherwise
   */
  public static boolean isPreview2Supported() {
    try {
      // Check if Preview 2 implementation classes are available
      Class.forName("ai.tegmentum.wasmtime4j.panama.wasi.PanamaWasiPreview2Context");
      return true;
    } catch (final ClassNotFoundException e) {
      try {
        Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.JniWasiPreview2Context");
        return true;
      } catch (final ClassNotFoundException e2) {
        return false;
      }
    }
  }

  /**
   * Determines if async operations are supported in the current runtime.
   *
   * <p>Async operations require WASI Preview 2 support.
   *
   * @return true if async operations are supported, false otherwise
   */
  public static boolean isAsyncOperationsSupported() {
    return isPreview2Supported();
  }

  /**
   * Gets the recommended WASI version for the current runtime.
   *
   * <p>Returns Preview 2 if supported, otherwise Preview 1.
   *
   * @return the recommended WASI version
   */
  public static WasiVersion getRecommendedVersion() {
    return isPreview2Supported() ? WasiVersion.PREVIEW_2 : WasiVersion.PREVIEW_1;
  }

  /** Wrapper class to adapt WasiPreview2Context to the general WasiContext interface. */
  private static final class WasiPreview2ContextWrapper implements WasiContext {
    private final WasiPreview2Context delegate;

    WasiPreview2ContextWrapper(final WasiPreview2Context delegate) {
      this.delegate = delegate;
    }

    public WasiConfig getConfig() {
      return delegate.getConfig();
    }

    @Override
    public WasiComponent createComponent(final byte[] wasmBytes) throws WasmException {
      // WasiPreview2Context doesn't have createComponent directly
      // This would need to be implemented using Preview 2 resource operations
      throw new UnsupportedOperationException(
          "Component creation not directly available in WASI Preview 2 wrapper. Use Preview 2"
              + " resource operations instead.");
    }

    @Override
    public boolean isValid() {
      // WasiPreview2Context doesn't have isValid(), so we check if it's not closed
      try {
        return delegate.getConfig() != null;
      } catch (Exception e) {
        return false;
      }
    }

    @Override
    public WasiFilesystem getFilesystem() throws WasmException {
      // WasiPreview2Context doesn't expose filesystem directly
      // This is a compatibility method for the unified interface
      throw new UnsupportedOperationException(
          "Direct filesystem access not available in WASI Preview 2. Use resource-based operations"
              + " instead.");
    }

    @Override
    public WasiRuntimeInfo getRuntimeInfo() {
      // WasiPreview2Context doesn't have getRuntimeInfo(), so we return a basic implementation
      return new WasiRuntimeInfo(WasiRuntimeType.PANAMA, "Preview 2", "wasmtime4j-preview2");
    }

    @Override
    public void close() {
      delegate.close();
    }

    /**
     * Gets the underlying Preview 2 context.
     *
     * @return the Preview 2 context
     */
    public WasiPreview2Context getPreview2Context() {
      return delegate;
    }
  }
}
