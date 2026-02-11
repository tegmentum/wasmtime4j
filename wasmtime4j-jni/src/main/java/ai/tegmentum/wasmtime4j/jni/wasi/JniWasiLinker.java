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

package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniEngine;
import ai.tegmentum.wasmtime4j.jni.JniLinker;
import ai.tegmentum.wasmtime4j.jni.JniWasmRuntime;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiLinker;
import ai.tegmentum.wasmtime4j.wasi.WasiPermissions;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JNI implementation of a WASI-enabled linker.
 *
 * <p>This linker wraps a standard JNI linker and adds WASI functionality. Configuration methods
 * store settings that are applied when {@link #instantiate(Store, Module)} is called.
 *
 * <p>The linker accumulates configuration through method calls and creates a properly configured
 * WASI context at instantiation time. This allows for flexible, incremental configuration of WASI
 * settings.
 *
 * @since 1.0.0
 */
public final class JniWasiLinker implements WasiLinker {

  private static final Logger LOGGER = Logger.getLogger(JniWasiLinker.class.getName());

  private final JniLinker<Object> linker;
  private final JniEngine engine;
  private final WasiConfig initialConfig;
  private boolean closed;

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
   * @param linkerHandle native linker handle
   * @param engine the engine
   * @param config initial WASI configuration (may be null for defaults)
   */
  public JniWasiLinker(final long linkerHandle, final JniEngine engine, final WasiConfig config) {
    this.linker = new JniLinker<>(linkerHandle, engine);
    this.engine = engine;
    this.initialConfig = config;
    this.closed = false;

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

    LOGGER.fine("Created JniWasiLinker with handle: " + linkerHandle);
  }

  @Override
  public void allowDirectoryAccess(
      final Path hostPath, final String guestPath, final WasiPermissions permissions)
      throws WasmException {
    ensureNotClosed();
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    if (guestPath == null) {
      throw new IllegalArgumentException("Guest path cannot be null");
    }
    if (permissions == null) {
      throw new IllegalArgumentException("Permissions cannot be null");
    }

    directoryMappings.put(hostPath, new DirectoryMapping(guestPath, permissions));
    LOGGER.fine("Added directory mapping: " + hostPath + " -> " + guestPath);
  }

  @Override
  public void allowDirectoryAccess(final Path hostPath, final String guestPath)
      throws WasmException {
    // Default permissions: 0755 (rwxr-xr-x)
    allowDirectoryAccess(hostPath, guestPath, WasiPermissions.of(0755));
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
    final WasiContext context = buildWasiContext();

    // Add WASI to linker with the configured context
    // Note: We use raw type cast here because JniWasiLinker wraps a generic linker
    // but addWasiToLinker expects Linker<WasiContext>. This is safe because the linker
    // is only used for WASI module instantiation within this class.
    final JniWasmRuntime runtime = new JniWasmRuntime();
    @SuppressWarnings("unchecked")
    final Linker<WasiContext> wasiLinker = (Linker<WasiContext>) (Linker<?>) linker;
    runtime.addWasiToLinker(wasiLinker, context);

    // Instantiate with the configured linker
    return linker.instantiate(store, module);
  }

  /**
   * Builds a WasiContext from the accumulated configuration.
   *
   * @return configured WasiContext
   * @throws WasmException if context creation fails
   */
  private WasiContext buildWasiContext() throws WasmException {
    final JniWasmRuntime runtime = new JniWasmRuntime();
    final WasiContext context = runtime.createWasiContext();

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

      // Check if read-only based on permissions
      final boolean readOnly = !mapping.permissions.isOwnerWrite();
      if (readOnly) {
        context.preopenedDirReadOnly(hostPath, mapping.guestPath);
      } else {
        context.preopenedDir(hostPath, mapping.guestPath);
      }
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
  private void applyStdinConfig(final WasiContext context, final WasiStdioConfig config) {
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
          final byte[] data = readAllBytesFromStream(inputStream);
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
  private void applyStdoutConfig(final WasiContext context, final WasiStdioConfig config) {
    switch (config.getType()) {
      case INHERIT:
        // Will be handled by inheritStdio or individually
        break;
      case FILE:
      case FILE_APPEND:
        context.setStdout(config.getFilePath());
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
  private void applyStderrConfig(final WasiContext context, final WasiStdioConfig config) {
    switch (config.getType()) {
      case INHERIT:
        // Will be handled by inheritStdio or individually
        break;
      case FILE:
      case FILE_APPEND:
        context.setStderr(config.getFilePath());
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
    return !closed && linker.isValid();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      linker.close();
      LOGGER.fine("Closed JniWasiLinker");
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("WASI linker has been closed");
    }
  }

  /**
   * Reads all bytes from an InputStream (Java 8 compatible implementation).
   *
   * @param inputStream the input stream to read from
   * @return all bytes from the stream
   * @throws IOException if an I/O error occurs
   */
  private static byte[] readAllBytesFromStream(final InputStream inputStream) throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final byte[] data = new byte[8192];
    int bytesRead;
    while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, bytesRead);
    }
    return buffer.toByteArray();
  }

  /** Internal class to hold directory mapping configuration. */
  private static final class DirectoryMapping {
    final String guestPath;
    final WasiPermissions permissions;

    DirectoryMapping(final String guestPath, final WasiPermissions permissions) {
      this.guestPath = guestPath;
      this.permissions = permissions;
    }
  }
}
