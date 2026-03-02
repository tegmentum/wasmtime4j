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
package ai.tegmentum.wasmtime4j.panama;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Native function bindings for WASI context operations.
 *
 * <p>Provides type-safe wrappers for all WASI context native functions including context lifecycle
 * management, environment and argument configuration, stdio redirection, directory pre-opening,
 * output capture, and linker integration.
 */
public final class NativeWasiBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeWasiBindings.class.getName());

  /** Initialization-on-demand holder for thread-safe lazy singleton. */
  private static final class Holder {
    static final NativeWasiBindings INSTANCE = new NativeWasiBindings();
  }

  private NativeWasiBindings() {
    super();
    initializeBindings();
    markInitialized();
    LOGGER.fine("Initialized NativeWasiBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeWasiBindings getInstance() {
    return Holder.INSTANCE;
  }

  private void initializeBindings() {
    // WASI context lifecycle functions
    addFunctionBinding(
        "wasmtime4j_wasi_context_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // WASI context argument and environment functions
    addFunctionBinding(
        "wasmtime4j_wasi_context_set_argv",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_env",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_inherit_env",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // WASI context stdio functions
    addFunctionBinding(
        "wasmtime4j_wasi_context_inherit_stdio",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_stdin",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_stdin_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_stdout",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_stderr",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // WASI context directory pre-opening functions
    addFunctionBinding(
        "wasmtime4j_wasi_context_preopen_dir",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_preopen_dir_readonly",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_preopen_dir_with_perms",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    // WASI context environment and argument getters
    addFunctionBinding(
        "wasmtime4j_wasi_context_get_environment",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // ctx_ptr
            ValueLayout.ADDRESS, // out_ptr
            ValueLayout.ADDRESS)); // out_len

    addFunctionBinding(
        "wasmtime4j_wasi_context_get_arguments",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // ctx_ptr
            ValueLayout.ADDRESS, // out_ptr
            ValueLayout.ADDRESS)); // out_len

    // WASI output capture functions
    addFunctionBinding(
        "wasmtime4j_wasi_context_enable_output_capture",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_get_stdout_capture",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return buffer pointer
            ValueLayout.ADDRESS, // ctx_ptr
            ValueLayout.ADDRESS)); // data_len_out

    addFunctionBinding(
        "wasmtime4j_wasi_context_get_stderr_capture",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return buffer pointer
            ValueLayout.ADDRESS, // ctx_ptr
            ValueLayout.ADDRESS)); // data_len_out

    addFunctionBinding(
        "wasmtime4j_wasi_free_capture_buffer", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_has_stdout_capture",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_wasi_context_has_stderr_capture",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // WASI context configuration functions
    addFunctionBinding(
        "wasmtime4j_wasi_context_set_network_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_allow_blocking",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_wasi_context_set_insecure_random_seed",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG));

    // WASI store and linker integration
    addFunctionBinding(
        "wasi_ctx_add_to_store",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_linker_add_wasi",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
  }

  // ===== WASI Context Lifecycle =====

  /**
   * Creates a new WASI context.
   *
   * @return pointer to the WASI context, or null on failure
   */
  public MemorySegment wasiContextCreate() {
    return callNativeFunction("wasmtime4j_wasi_context_create", MemorySegment.class);
  }

  /**
   * Destroys a WASI context.
   *
   * @param contextHandle the WASI context handle
   */
  public void wasiContextDestroy(final MemorySegment contextHandle) {
    if (contextHandle != null && !contextHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_wasi_context_destroy", Void.class, contextHandle);
    }
  }

  // ===== WASI Context Arguments and Environment =====

  /**
   * Sets command line arguments for the WASI context.
   *
   * @param contextHandle the WASI context handle
   * @param args pointer to array of C strings
   * @param argCount number of arguments
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetArgv(
      final MemorySegment contextHandle, final MemorySegment args, final long argCount) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_argv", Integer.class, contextHandle, args, argCount);
  }

  /**
   * Sets an environment variable in the WASI context.
   *
   * @param contextHandle the WASI context handle
   * @param key the environment variable name (C string)
   * @param value the environment variable value (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetEnv(
      final MemorySegment contextHandle, final MemorySegment key, final MemorySegment value) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(key, "key");
    validatePointer(value, "value");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_env", Integer.class, contextHandle, key, value);
  }

  /**
   * Inherits all host environment variables.
   *
   * @param contextHandle the WASI context handle
   * @return 0 on success, non-zero on error
   */
  public int wasiContextInheritEnv(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction("wasmtime4j_wasi_context_inherit_env", Integer.class, contextHandle);
  }

  /**
   * Inherits host command-line arguments.
   *
   * @param contextHandle the WASI context handle
   * @return 0 on success, non-zero on error
   */
  public int wasiContextInheritArgs(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction("wasmtime4j_wasi_context_inherit_args", Integer.class, contextHandle);
  }

  // ===== WASI Context Environment and Argument Getters =====

  /**
   * Gets the environment variables from the WASI context.
   *
   * <p>Returns environment as "key=value\n" pairs in the output buffer. The caller must free the
   * buffer using {@link #wasiFreeCaptureBuffer(MemorySegment)}.
   *
   * @param contextHandle the WASI context handle
   * @param outPtr output pointer for the data buffer
   * @param outLen output pointer for the data length
   * @return 0 on success, non-zero on error
   */
  public int wasiContextGetEnvironment(
      final MemorySegment contextHandle, final MemorySegment outPtr, final MemorySegment outLen) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(outPtr, "outPtr");
    validatePointer(outLen, "outLen");
    return callNativeFunction(
        "wasmtime4j_wasi_context_get_environment", Integer.class, contextHandle, outPtr, outLen);
  }

  /**
   * Gets the arguments from the WASI context.
   *
   * <p>Returns arguments as "arg1\narg2\n..." in the output buffer. The caller must free the buffer
   * using {@link #wasiFreeCaptureBuffer(MemorySegment)}.
   *
   * @param contextHandle the WASI context handle
   * @param outPtr output pointer for the data buffer
   * @param outLen output pointer for the data length
   * @return 0 on success, non-zero on error
   */
  public int wasiContextGetArguments(
      final MemorySegment contextHandle, final MemorySegment outPtr, final MemorySegment outLen) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(outPtr, "outPtr");
    validatePointer(outLen, "outLen");
    return callNativeFunction(
        "wasmtime4j_wasi_context_get_arguments", Integer.class, contextHandle, outPtr, outLen);
  }

  // ===== WASI Context Stdio =====

  /**
   * Inherits host stdio streams.
   *
   * @param contextHandle the WASI context handle
   * @return 0 on success, non-zero on error
   */
  public int wasiContextInheritStdio(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_inherit_stdio", Integer.class, contextHandle);
  }

  /**
   * Sets stdin to read from a file.
   *
   * @param contextHandle the WASI context handle
   * @param path the file path (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStdin(final MemorySegment contextHandle, final MemorySegment path) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(path, "path");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stdin", Integer.class, contextHandle, path);
  }

  /**
   * Sets stdin from binary data buffer (supports binary data with null bytes).
   *
   * @param contextHandle the WASI context handle
   * @param dataPtr pointer to the binary data
   * @param dataLen length of the data in bytes
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStdinBytes(
      final MemorySegment contextHandle, final MemorySegment dataPtr, final long dataLen) {
    validatePointer(contextHandle, "contextHandle");
    // dataPtr can be NULL for empty input
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stdin_bytes",
        Integer.class,
        contextHandle,
        dataPtr == null ? MemorySegment.NULL : dataPtr,
        dataLen);
  }

  /**
   * Sets stdout to write to a file.
   *
   * @param contextHandle the WASI context handle
   * @param path the file path (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStdout(final MemorySegment contextHandle, final MemorySegment path) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(path, "path");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stdout", Integer.class, contextHandle, path);
  }

  /**
   * Sets stderr to write to a file.
   *
   * @param contextHandle the WASI context handle
   * @param path the file path (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStderr(final MemorySegment contextHandle, final MemorySegment path) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(path, "path");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stderr", Integer.class, contextHandle, path);
  }

  /**
   * Sets stdout to append to a file.
   *
   * @param contextHandle the WASI context handle
   * @param path the file path (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStdoutAppend(
      final MemorySegment contextHandle, final MemorySegment path) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(path, "path");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stdout_append", Integer.class, contextHandle, path);
  }

  /**
   * Sets stderr to append to a file.
   *
   * @param contextHandle the WASI context handle
   * @param path the file path (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextSetStderrAppend(
      final MemorySegment contextHandle, final MemorySegment path) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(path, "path");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_stderr_append", Integer.class, contextHandle, path);
  }

  // ===== WASI Context Directory Pre-Opening =====

  /**
   * Adds a pre-opened directory with full permissions.
   *
   * @param contextHandle the WASI context handle
   * @param hostPath the host filesystem path (C string)
   * @param guestPath the guest path visible to WASI module (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextPreopenDir(
      final MemorySegment contextHandle,
      final MemorySegment hostPath,
      final MemorySegment guestPath) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(hostPath, "hostPath");
    validatePointer(guestPath, "guestPath");
    return callNativeFunction(
        "wasmtime4j_wasi_context_preopen_dir", Integer.class, contextHandle, hostPath, guestPath);
  }

  /**
   * Adds a pre-opened directory with read-only permissions.
   *
   * @param contextHandle the WASI context handle
   * @param hostPath the host filesystem path (C string)
   * @param guestPath the guest path visible to WASI module (C string)
   * @return 0 on success, non-zero on error
   */
  public int wasiContextPreopenDirReadonly(
      final MemorySegment contextHandle,
      final MemorySegment hostPath,
      final MemorySegment guestPath) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(hostPath, "hostPath");
    validatePointer(guestPath, "guestPath");
    return callNativeFunction(
        "wasmtime4j_wasi_context_preopen_dir_readonly",
        Integer.class,
        contextHandle,
        hostPath,
        guestPath);
  }

  /**
   * Adds a pre-opened directory with custom permissions.
   *
   * @param contextHandle the WASI context handle
   * @param hostPath the host filesystem path (C string)
   * @param guestPath the guest path visible to WASI module (C string)
   * @param canRead whether reading is allowed
   * @param canWrite whether writing is allowed
   * @param canCreate whether file creation is allowed
   * @return 0 on success, non-zero on error
   */
  public int wasiContextPreopenDirWithPerms(
      final MemorySegment contextHandle,
      final MemorySegment hostPath,
      final MemorySegment guestPath,
      final int canRead,
      final int canWrite,
      final int canCreate) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(hostPath, "hostPath");
    validatePointer(guestPath, "guestPath");
    return callNativeFunction(
        "wasmtime4j_wasi_context_preopen_dir_with_perms",
        Integer.class,
        contextHandle,
        hostPath,
        guestPath,
        canRead,
        canWrite,
        canCreate);
  }

  // ===== WASI Output Capture =====

  /**
   * Enables output capture for stdout and stderr.
   *
   * <p>This configures the WASI context to capture stdout and stderr to internal buffers instead of
   * inheriting from the host process.
   *
   * @param contextHandle the WASI context handle
   * @return 0 on success, non-zero on error
   */
  public int wasiContextEnableOutputCapture(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_enable_output_capture", Integer.class, contextHandle);
  }

  /**
   * Gets captured stdout data.
   *
   * <p>Returns a pointer to the captured stdout data and sets the length in the output parameter.
   * The caller must free the returned buffer using {@link #wasiFreeCaptureBuffer(MemorySegment)}.
   *
   * @param contextHandle the WASI context handle
   * @param lengthOut output parameter for the data length
   * @return pointer to captured data, or NULL if capture is not enabled or empty
   */
  public MemorySegment wasiContextGetStdoutCapture(
      final MemorySegment contextHandle, final MemorySegment lengthOut) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(lengthOut, "lengthOut");
    return callNativeFunction(
        "wasmtime4j_wasi_context_get_stdout_capture",
        MemorySegment.class,
        contextHandle,
        lengthOut);
  }

  /**
   * Gets captured stderr data.
   *
   * <p>Returns a pointer to the captured stderr data and sets the length in the output parameter.
   * The caller must free the returned buffer using {@link #wasiFreeCaptureBuffer(MemorySegment)}.
   *
   * @param contextHandle the WASI context handle
   * @param lengthOut output parameter for the data length
   * @return pointer to captured data, or NULL if capture is not enabled or empty
   */
  public MemorySegment wasiContextGetStderrCapture(
      final MemorySegment contextHandle, final MemorySegment lengthOut) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(lengthOut, "lengthOut");
    return callNativeFunction(
        "wasmtime4j_wasi_context_get_stderr_capture",
        MemorySegment.class,
        contextHandle,
        lengthOut);
  }

  /**
   * Frees a capture buffer allocated by {@link #wasiContextGetStdoutCapture(MemorySegment,
   * MemorySegment)} or {@link #wasiContextGetStderrCapture(MemorySegment, MemorySegment)}.
   *
   * @param buffer pointer to the buffer to free (can be NULL)
   */
  public void wasiFreeCaptureBuffer(final MemorySegment buffer) {
    // buffer can be NULL
    callNativeFunction("wasmtime4j_wasi_free_capture_buffer", Void.class, buffer);
  }

  /**
   * Checks if stdout capture is enabled.
   *
   * @param contextHandle the WASI context handle
   * @return 1 if capture is enabled, 0 if not, -1 on error
   */
  public int wasiContextHasStdoutCapture(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_has_stdout_capture", Integer.class, contextHandle);
  }

  /**
   * Checks if stderr capture is enabled.
   *
   * @param contextHandle the WASI context handle
   * @return 1 if capture is enabled, 0 if not, -1 on error
   */
  public int wasiContextHasStderrCapture(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_has_stderr_capture", Integer.class, contextHandle);
  }

  // ===== WASI Store and Linker Integration =====

  /**
   * Adds a WASI context to a Store.
   *
   * <p>This must be called before instantiating WASI-enabled modules. The context will be used by
   * WASI imports when they are called.
   *
   * @param contextHandle the WASI context handle
   * @param storeHandle the Store handle
   * @return 0 on success, non-zero on error
   */
  public int wasiCtxAddToStore(final MemorySegment contextHandle, final MemorySegment storeHandle) {
    validatePointer(contextHandle, "contextHandle");
    validatePointer(storeHandle, "storeHandle");
    return callNativeFunction("wasi_ctx_add_to_store", Integer.class, contextHandle, storeHandle);
  }

  /**
   * Adds WASI Preview 1 imports to a linker.
   *
   * <p>Configures the linker to extract WASI context from store data when instantiating modules.
   * The store must have a WASI context attached before instantiating WASI-enabled modules.
   *
   * @param linkerHandle pointer to the linker
   * @return 0 on success, non-zero on failure
   */
  public int linkerAddWasi(final MemorySegment linkerHandle) {
    validatePointer(linkerHandle, "linkerHandle");
    return callNativeFunction("wasmtime4j_linker_add_wasi", Integer.class, linkerHandle);
  }

  // ===== WASI Context Configuration =====

  /**
   * Sets network configuration on a WASI context.
   *
   * @param contextHandle the WASI context handle
   * @param allowNetwork 1 to allow network access (inherit_network), 0 to deny
   * @param allowTcp 1 to allow TCP socket creation, 0 to deny
   * @param allowUdp 1 to allow UDP socket creation, 0 to deny
   * @param allowIpNameLookup 1 to allow IP name lookups (DNS), 0 to deny
   * @return 0 on success, non-zero on failure
   */
  public int wasiContextSetNetworkConfig(
      final MemorySegment contextHandle,
      final int allowNetwork,
      final int allowTcp,
      final int allowUdp,
      final int allowIpNameLookup) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_network_config",
        Integer.class,
        contextHandle,
        allowNetwork,
        allowTcp,
        allowUdp,
        allowIpNameLookup);
  }

  /**
   * Sets whether blocking the current thread is allowed.
   *
   * @param contextHandle the WASI context handle
   * @param allow 1 to allow blocking, 0 to deny
   * @return 0 on success, non-zero on failure
   */
  public int wasiContextSetAllowBlocking(final MemorySegment contextHandle, final int allow) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_allow_blocking", Integer.class, contextHandle, allow);
  }

  /**
   * Sets the insecure random seed for deterministic testing.
   *
   * @param contextHandle the WASI context handle
   * @param seedLo low 64 bits of the u128 seed
   * @param seedHi high 64 bits of the u128 seed
   * @return 0 on success, non-zero on failure
   */
  public int wasiContextSetInsecureRandomSeed(
      final MemorySegment contextHandle, final long seedLo, final long seedHi) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_wasi_context_set_insecure_random_seed",
        Integer.class,
        contextHandle,
        seedLo,
        seedHi);
  }

  /**
   * Rebuilds the WASI context after configuration changes.
   *
   * <p>Must be called after setting network config, blocking, or random seed to apply the changes.
   * This avoids redundant rebuilds when multiple settings are configured at once.
   *
   * @param contextHandle the WASI context handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiContextRebuild(final MemorySegment contextHandle) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction("wasmtime4j_wasi_context_rebuild", Integer.class, contextHandle);
  }
}
