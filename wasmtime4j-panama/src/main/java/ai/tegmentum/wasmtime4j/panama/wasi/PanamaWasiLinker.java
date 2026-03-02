/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeWasiBindings;
import ai.tegmentum.wasmtime4j.panama.PanamaEngine;
import ai.tegmentum.wasmtime4j.panama.PanamaLinker;
import ai.tegmentum.wasmtime4j.panama.PanamaStore;
import ai.tegmentum.wasmtime4j.panama.PanamaWasiContext;
import ai.tegmentum.wasmtime4j.panama.PanamaWasmRuntime;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiLinker;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Panama implementation of a WASI-enabled linker.
 *
 * <p>This linker wraps a standard Panama linker and adds WASI functionality. Configuration methods
 * store settings that are applied when {@link #instantiate(Store, Module)} is called.
 *
 * <p>The linker accumulates configuration through method calls and creates a properly configured
 * WASI context at instantiation time. This allows for flexible, incremental configuration of WASI
 * settings.
 *
 * @since 1.0.0
 */
public final class PanamaWasiLinker implements WasiLinker {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiLinker.class.getName());
  private static final NativeWasiBindings NATIVE_BINDINGS = NativeWasiBindings.getInstance();

  private final PanamaLinker<Object> linker;
  private final PanamaEngine engine;
  private final WasiConfig initialConfig;
  private final NativeResourceHandle resourceHandle;

  // Accumulated configuration
  private final Map<Path, DirectoryMapping> directoryMappings;
  private final Map<String, String> environmentVariables;
  private final List<String> arguments;
  private WasiStdioConfig stdinConfig;
  private WasiStdioConfig stdoutConfig;
  private WasiStdioConfig stderrConfig;
  private boolean networkEnabled;
  private Long maxFileSize;
  private Integer maxOpenFiles;
  private boolean inheritAllEnvironment;
  private List<String> inheritedEnvironmentVariables;

  /**
   * Creates a new WASI linker with the specified configuration.
   *
   * @param linker the Panama linker instance
   * @param engine the engine
   * @param config initial WASI configuration (may be null for defaults)
   */
  public PanamaWasiLinker(
      final PanamaLinker<Object> linker, final PanamaEngine engine, final WasiConfig config) {
    this.linker = linker;
    this.engine = engine;
    this.initialConfig = config;

    // Initialize configuration storage
    this.directoryMappings = new HashMap<>();
    this.environmentVariables = new HashMap<>();
    this.arguments = new ArrayList<>();
    this.stdinConfig = null;
    this.stdoutConfig = null;
    this.stderrConfig = null;
    this.networkEnabled = false;
    this.maxFileSize = null;
    this.maxOpenFiles = null;
    this.inheritAllEnvironment = false;
    this.inheritedEnvironmentVariables = null;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWasiLinker",
            () -> {
              linker.close();
            });

    LOGGER.fine("Created PanamaWasiLinker");
  }

  @Override
  public void allowDirectoryAccess(final Path hostPath, final String guestPath)
      throws WasmException {
    ensureNotClosed();
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    if (guestPath == null) {
      throw new IllegalArgumentException("Guest path cannot be null");
    }

    directoryMappings.put(hostPath, new DirectoryMapping(guestPath));
    LOGGER.fine("Added directory mapping: " + hostPath + " -> " + guestPath);
  }

  @Override
  public void setEnvironmentVariable(final String name, final String value) {
    ensureNotClosed();
    if (name == null) {
      throw new IllegalArgumentException("Environment variable name cannot be null");
    }

    environmentVariables.put(name, value);
    LOGGER.fine("Set environment variable: " + name);
  }

  @Override
  public void setEnvironmentVariables(final Map<String, String> environment) {
    ensureNotClosed();
    if (environment == null) {
      throw new IllegalArgumentException("Environment map cannot be null");
    }

    environmentVariables.putAll(environment);
    LOGGER.fine("Added " + environment.size() + " environment variables");
  }

  @Override
  public void inheritEnvironment() throws WasmException {
    ensureNotClosed();
    inheritAllEnvironment = true;
    LOGGER.fine("Enabled full environment inheritance");
  }

  @Override
  public void inheritEnvironmentVariables(final List<String> variableNames) throws WasmException {
    ensureNotClosed();
    if (variableNames == null) {
      throw new IllegalArgumentException("Variable names list cannot be null");
    }

    inheritedEnvironmentVariables = new ArrayList<>(variableNames);
    LOGGER.fine("Set " + variableNames.size() + " environment variables to inherit");
  }

  @Override
  public void setArguments(final List<String> args) {
    ensureNotClosed();
    if (args == null) {
      throw new IllegalArgumentException("Arguments list cannot be null");
    }

    arguments.clear();
    arguments.addAll(args);
    LOGGER.fine("Set " + args.size() + " command line arguments");
  }

  @Override
  public void configureStdin(final WasiStdioConfig config) throws WasmException {
    ensureNotClosed();
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    stdinConfig = config;
    LOGGER.fine("Configured stdin: " + config.getType());
  }

  @Override
  public void configureStdout(final WasiStdioConfig config) throws WasmException {
    ensureNotClosed();
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    stdoutConfig = config;
    LOGGER.fine("Configured stdout: " + config.getType());
  }

  @Override
  public void configureStderr(final WasiStdioConfig config) throws WasmException {
    ensureNotClosed();
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    stderrConfig = config;
    LOGGER.fine("Configured stderr: " + config.getType());
  }

  @Override
  public void enableNetworkAccess() throws WasmException {
    ensureNotClosed();
    networkEnabled = true;
    LOGGER.fine("Enabled network access");
  }

  @Override
  public void disableNetworkAccess() {
    ensureNotClosed();
    networkEnabled = false;
    LOGGER.fine("Disabled network access");
  }

  @Override
  public void setMaxFileSize(final Long maxSizeBytes) {
    ensureNotClosed();
    maxFileSize = maxSizeBytes;
    LOGGER.fine("Set max file size: " + maxSizeBytes);
  }

  @Override
  public void setMaxOpenFiles(final Integer max) {
    ensureNotClosed();
    maxOpenFiles = max;
    LOGGER.fine("Set max open files: " + max);
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    ensureNotClosed();
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }

    // Build WASI context from accumulated configuration
    final PanamaWasiContext context = buildWasiContext();

    // Get the native store handle
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore instance for Panama runtime");
    }
    final PanamaStore panamaStore = (PanamaStore) store;

    // Add WASI context to the store
    final int addResult =
        NATIVE_BINDINGS.wasiCtxAddToStore(context.getNativeHandle(), panamaStore.getNativeStore());
    if (addResult != 0) {
      throw PanamaErrorMapper.mapNativeError(addResult, "Failed to add WASI context to store");
    }

    // Enable WASI on the linker
    linker.enableWasi();

    // Instantiate with the configured linker
    return linker.instantiate(store, module);
  }

  /**
   * Builds a PanamaWasiContext from the accumulated configuration.
   *
   * @return configured PanamaWasiContext
   * @throws WasmException if context creation fails
   */
  private PanamaWasiContext buildWasiContext() throws WasmException {
    final PanamaWasmRuntime runtime = new PanamaWasmRuntime();
    final WasiContext baseContext = runtime.createWasiContext();
    if (!(baseContext instanceof PanamaWasiContext)) {
      throw new WasmException("Expected PanamaWasiContext but got " + baseContext.getClass());
    }
    final PanamaWasiContext context = (PanamaWasiContext) baseContext;

    // Apply arguments
    if (!arguments.isEmpty()) {
      context.setArgv(arguments.toArray(new String[0]));
    }

    // Apply environment variables
    if (inheritAllEnvironment) {
      context.inheritEnv();
    } else if (inheritedEnvironmentVariables != null) {
      // Inherit specific variables from host environment
      for (final String varName : inheritedEnvironmentVariables) {
        final String value = System.getenv(varName);
        if (value != null) {
          context.setEnv(varName, value);
        }
      }
    }

    // Apply explicitly set environment variables (these override inherited ones)
    for (final Map.Entry<String, String> entry : environmentVariables.entrySet()) {
      context.setEnv(entry.getKey(), entry.getValue());
    }

    // Apply directory mappings
    for (final Map.Entry<Path, DirectoryMapping> entry : directoryMappings.entrySet()) {
      final Path hostPath = entry.getKey();
      final DirectoryMapping mapping = entry.getValue();

      context.preopenedDir(hostPath, mapping.guestPath);
    }

    // Apply stdio configuration
    if (stdinConfig != null) {
      applyStdinConfig(context, stdinConfig);
    }
    if (stdoutConfig != null) {
      applyStdoutConfig(context, stdoutConfig);
    }
    if (stderrConfig != null) {
      applyStderrConfig(context, stderrConfig);
    }

    // If no stdio config, inherit by default
    if (stdinConfig == null && stdoutConfig == null && stderrConfig == null) {
      context.inheritStdio();
    }

    // Apply network access
    context.setNetworkEnabled(networkEnabled);

    // Apply resource limits
    if (maxOpenFiles != null) {
      context.setMaxOpenFiles(maxOpenFiles);
    }

    // Note: maxFileSize is not directly supported by WasiContext
    // It would need native support to implement properly
    if (maxFileSize != null) {
      LOGGER.warning("maxFileSize limit is configured but not enforced at WASI level");
    }

    return context;
  }

  /**
   * Applies stdin configuration to the WASI context.
   *
   * @param context the WASI context
   * @param config the stdin configuration
   */
  private void applyStdinConfig(final PanamaWasiContext context, final WasiStdioConfig config) {
    switch (config.getType()) {
      case INHERIT:
        // Will be handled by inheritStdio or individually
        break;
      case FILE:
      case FILE_APPEND:
        context.setStdin(config.getFilePath());
        break;
      case NULL:
        // Null stdin - no input available
        // This is the default if not inherited
        break;
      case INPUT_STREAM:
        // Read all bytes from InputStream and pass to native stdin buffer
        try {
          final InputStream inputStream = config.getInputStream();
          final byte[] data = inputStream.readAllBytes();
          context.setStdinBytes(data);
          LOGGER.fine("Set stdin from InputStream with " + data.length + " bytes");
        } catch (IOException e) {
          throw new RuntimeException("Failed to read stdin from InputStream", e);
        }
        break;
      default:
        break;
    }
  }

  /**
   * Applies stdout configuration to the WASI context.
   *
   * @param context the WASI context
   * @param config the stdout configuration
   */
  private void applyStdoutConfig(final PanamaWasiContext context, final WasiStdioConfig config) {
    switch (config.getType()) {
      case INHERIT:
        // Will be handled by inheritStdio or individually
        break;
      case FILE:
        context.setStdout(config.getFilePath());
        break;
      case FILE_APPEND:
        context.setStdoutAppend(config.getFilePath());
        break;
      case NULL:
        // Null stdout - output discarded
        break;
      case OUTPUT_STREAM:
        // Java OutputStream requires special handling at native level
        LOGGER.warning("OutputStream stdout configuration requires native stream bridging");
        break;
      default:
        break;
    }
  }

  /**
   * Applies stderr configuration to the WASI context.
   *
   * @param context the WASI context
   * @param config the stderr configuration
   */
  private void applyStderrConfig(final PanamaWasiContext context, final WasiStdioConfig config) {
    switch (config.getType()) {
      case INHERIT:
        // Will be handled by inheritStdio or individually
        break;
      case FILE:
        context.setStderr(config.getFilePath());
        break;
      case FILE_APPEND:
        context.setStderrAppend(config.getFilePath());
        break;
      case NULL:
        // Null stderr - error output discarded
        break;
      case OUTPUT_STREAM:
        // Java OutputStream requires special handling at native level
        LOGGER.warning("OutputStream stderr configuration requires native stream bridging");
        break;
      default:
        break;
    }
  }

  @Override
  public Linker<?> getLinker() {
    ensureNotClosed();
    return linker;
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public WasiConfig getConfig() {
    return initialConfig;
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed() && linker.isValid();
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /** Internal class to hold directory mapping configuration. */
  private static final class DirectoryMapping {
    final String guestPath;

    DirectoryMapping(final String guestPath) {
      this.guestPath = guestPath;
    }
  }
}
