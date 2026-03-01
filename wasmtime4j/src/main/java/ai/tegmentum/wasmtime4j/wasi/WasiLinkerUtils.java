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
package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;

/**
 * Utility class for adding WASI (WebAssembly System Interface) imports to a linker.
 *
 * <p>WasiLinker provides convenient methods for setting up WASI functionality in WebAssembly
 * modules by automatically defining all necessary WASI imports in a linker.
 *
 * <p>WASI enables WebAssembly modules to interact with the host system in a standardized way,
 * providing access to file systems, environment variables, command-line arguments, and other system
 * resources.
 *
 * @since 1.0.0
 */
public final class WasiLinkerUtils {

  // Prevent instantiation
  private WasiLinkerUtils() {}

  /** WASI Preview 1 module name. */
  public static final String WASI_P1_MODULE = "wasi_snapshot_preview1";

  /**
   * WASI Preview 1 import markers for hasImport() tracking. Each entry is {moduleName, fieldName}.
   */
  public static final String[][] WASI_P1_IMPORTS = {
    {WASI_P1_MODULE, "fd_write"},
    {WASI_P1_MODULE, "proc_exit"},
    {WASI_P1_MODULE, "fd_read"},
    {WASI_P1_MODULE, "fd_close"},
    {WASI_P1_MODULE, "environ_get"},
    {WASI_P1_MODULE, "environ_sizes_get"},
    {WASI_P1_MODULE, "args_get"},
    {WASI_P1_MODULE, "args_sizes_get"},
  };

  /**
   * WASI Preview 2 import markers for hasImport() tracking. Each entry is {moduleName, fieldName}.
   */
  public static final String[][] WASI_P2_IMPORTS = {
    {"wasi:filesystem/types", "filesystem"},
    {"wasi:io/streams", "input-stream"},
    {"wasi:sockets/network", "network"},
  };

  /**
   * Adds all WASI imports to the specified linker using the provided WASI context.
   *
   * <p>This method defines all necessary WASI functions in the linker, including:
   *
   * <ul>
   *   <li>File system operations (open, read, write, etc.)
   *   <li>Environment variable access
   *   <li>Command-line argument access
   *   <li>Time and random number functions
   *   <li>Process and memory management functions
   * </ul>
   *
   * @param linker the linker to add WASI imports to
   * @param context the WASI context containing configuration
   * @throws WasmException if adding WASI imports fails
   * @throws IllegalArgumentException if linker or context is null
   * @since 1.0.0
   */
  public static void addToLinker(Linker<WasiContext> linker, WasiContext context)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }

    // Implementation will be provided by the runtime
    WasmRuntimeFactory.create().addWasiToLinker(linker, context);
  }

  /**
   * Adds all WASI imports to the specified linker using a default WASI context.
   *
   * <p>This is a convenience method that creates a default WASI context with inherited stdio and no
   * file system access.
   *
   * @param linker the linker to add WASI imports to
   * @throws WasmException if adding WASI imports fails
   * @throws IllegalArgumentException if linker is null
   * @since 1.0.0
   */
  public static void addToLinker(Linker<WasiContext> linker) throws WasmException {
    WasiContext defaultContext = WasiContext.create().inheritStdio();
    addToLinker(linker, defaultContext);
  }

  /**
   * Adds all WASI imports to the specified linker using async-compatible host functions.
   *
   * <p>When used with async-enabled stores, the WASI host functions will cooperatively yield during
   * I/O operations, allowing other async tasks to make progress. This corresponds to Wasmtime's
   * {@code wasmtime_wasi::p2::add_to_linker_async()}.
   *
   * <p>The default implementation delegates to {@link #addToLinker(Linker, WasiContext)}. Runtime
   * implementations may override the underlying runtime method to use native async WASI functions.
   *
   * @param linker the linker to add async WASI imports to
   * @param context the WASI context containing configuration
   * @throws WasmException if adding WASI imports fails
   * @throws IllegalArgumentException if linker or context is null
   * @since 1.1.0
   */
  public static void addToLinkerAsync(Linker<WasiContext> linker, WasiContext context)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }

    // Default: delegates to sync variant. Runtimes may override addWasiToLinkerAsync()
    // to use wasmtime_wasi::p2::add_to_linker_async() for cooperative yielding.
    WasmRuntimeFactory.create().addWasiToLinkerAsync(linker, context);
  }

  /**
   * Adds all WASI imports to the specified linker using async-compatible host functions with a
   * default WASI context.
   *
   * @param linker the linker to add async WASI imports to
   * @throws WasmException if adding WASI imports fails
   * @throws IllegalArgumentException if linker is null
   * @since 1.1.0
   */
  public static void addToLinkerAsync(Linker<WasiContext> linker) throws WasmException {
    WasiContext defaultContext = WasiContext.create().inheritStdio();
    addToLinkerAsync(linker, defaultContext);
  }

  /**
   * Creates a new linker with WASI imports already configured.
   *
   * <p>This is a convenience method that creates a new linker and adds all WASI imports using the
   * provided context.
   *
   * @param engine the engine to create the linker for
   * @param context the WASI context containing configuration
   * @return a new linker with WASI imports configured
   * @throws WasmException if creating the linker or adding WASI imports fails
   * @throws IllegalArgumentException if engine or context is null
   * @since 1.0.0
   */
  public static Linker<WasiContext> createLinker(Engine engine, WasiContext context)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }

    Linker<WasiContext> linker = WasmRuntimeFactory.create().createLinker(engine);
    addToLinker(linker, context);
    return linker;
  }

  /**
   * Creates a new linker with WASI imports using a default context.
   *
   * <p>This is a convenience method that creates a new linker with WASI imports configured using a
   * default context (inherited stdio, no file system access).
   *
   * @param engine the engine to create the linker for
   * @return a new linker with default WASI imports configured
   * @throws WasmException if creating the linker or adding WASI imports fails
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  public static Linker<WasiContext> createLinker(Engine engine) throws WasmException {
    WasiContext defaultContext = WasiContext.create().inheritStdio();
    return createLinker(engine, defaultContext);
  }

  /**
   * Adds WASI Preview 2 imports to the specified linker using the provided WASI context.
   *
   * <p>This method defines all necessary WASI Preview 2 functions in the linker, including:
   *
   * <ul>
   *   <li>Component-based filesystem operations with async I/O
   *   <li>Stream-based networking with HTTP/TCP/UDP support
   *   <li>Enhanced process and environment management
   *   <li>Component model resource management
   *   <li>WIT interface definitions and type validation
   * </ul>
   *
   * @param linker the linker to add WASI Preview 2 imports to
   * @param context the WASI context containing configuration
   * @throws WasmException if adding WASI Preview 2 imports fails
   * @throws IllegalArgumentException if linker or context is null
   * @since 1.0.0
   */
  public static void addPreview2ToLinker(Linker<WasiContext> linker, WasiContext context)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }

    // Implementation will be provided by the runtime
    WasmRuntimeFactory.create().addWasiPreview2ToLinker(linker, context);
  }

  /**
   * Adds WASI Preview 2 imports to the specified linker using a default WASI context.
   *
   * <p>This is a convenience method that creates a default WASI context with Preview 2 capabilities
   * enabled, including async I/O and component model support.
   *
   * @param linker the linker to add WASI Preview 2 imports to
   * @throws WasmException if adding WASI Preview 2 imports fails
   * @throws IllegalArgumentException if linker is null
   * @since 1.0.0
   */
  public static void addPreview2ToLinker(Linker<WasiContext> linker) throws WasmException {
    WasiContext preview2Context = WasiContext.create().inheritStdio().setNetworkEnabled(true);
    addPreview2ToLinker(linker, preview2Context);
  }

  /**
   * Creates a new linker with WASI Preview 2 imports already configured.
   *
   * <p>This is a convenience method that creates a new linker and adds all WASI Preview 2 imports
   * using the provided context.
   *
   * @param engine the engine to create the linker for
   * @param context the WASI context containing configuration
   * @return a new linker with WASI Preview 2 imports configured
   * @throws WasmException if creating the linker or adding WASI Preview 2 imports fails
   * @throws IllegalArgumentException if engine or context is null
   * @since 1.0.0
   */
  public static Linker<WasiContext> createPreview2Linker(Engine engine, WasiContext context)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }

    Linker<WasiContext> linker = WasmRuntimeFactory.create().createLinker(engine);
    addPreview2ToLinker(linker, context);
    return linker;
  }

  /**
   * Creates a new linker with WASI Preview 2 imports using a default context.
   *
   * <p>This is a convenience method that creates a new linker with WASI Preview 2 imports
   * configured using a default context (inherited stdio, networking enabled).
   *
   * @param engine the engine to create the linker for
   * @return a new linker with default WASI Preview 2 imports configured
   * @throws WasmException if creating the linker or adding WASI Preview 2 imports fails
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  public static Linker<WasiContext> createPreview2Linker(Engine engine) throws WasmException {
    WasiContext preview2Context = WasiContext.create().inheritStdio().setNetworkEnabled(true);
    return createPreview2Linker(engine, preview2Context);
  }

  /**
   * Adds Component Model imports to the specified linker.
   *
   * <p>This method defines all necessary Component Model functions in the linker, including:
   *
   * <ul>
   *   <li>Component compilation and instantiation
   *   <li>WIT interface parsing and validation
   *   <li>Component linking and composition
   *   <li>Resource management and lifecycle
   * </ul>
   *
   * @param linker the linker to add Component Model imports to
   * @throws WasmException if adding Component Model imports fails
   * @throws IllegalArgumentException if linker is null
   * @since 1.0.0
   */
  public static void addComponentModelToLinker(Linker<WasiContext> linker) throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }

    // Implementation will be provided by the runtime
    WasmRuntimeFactory.create().addComponentModelToLinker(linker);
  }

  /**
   * Creates a new linker with both WASI Preview 2 and Component Model imports configured.
   *
   * <p>This is a convenience method that creates a comprehensive linker with full WASI Preview 2
   * and Component Model capabilities.
   *
   * @param engine the engine to create the linker for
   * @param context the WASI context containing configuration
   * @return a new linker with complete WASI Preview 2 and Component Model imports
   * @throws WasmException if creating the linker or adding imports fails
   * @throws IllegalArgumentException if engine or context is null
   * @since 1.0.0
   */
  public static Linker<WasiContext> createFullLinker(Engine engine, WasiContext context)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }

    Linker<WasiContext> linker = WasmRuntimeFactory.create().createLinker(engine);
    addPreview2ToLinker(linker, context);
    addComponentModelToLinker(linker);
    return linker;
  }

  /**
   * Creates a new linker with both WASI Preview 2 and Component Model imports using default
   * context.
   *
   * @param engine the engine to create the linker for
   * @return a new linker with complete WASI Preview 2 and Component Model imports
   * @throws WasmException if creating the linker or adding imports fails
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  public static Linker<WasiContext> createFullLinker(Engine engine) throws WasmException {
    WasiContext preview2Context = WasiContext.create().inheritStdio().setNetworkEnabled(true);
    return createFullLinker(engine, preview2Context);
  }

  /**
   * Checks if a linker has WASI imports configured.
   *
   * <p>This method checks for the presence of common WASI imports to determine if WASI
   * functionality has been added to the linker.
   *
   * @param linker the linker to check
   * @return true if WASI imports are present, false otherwise
   * @throws IllegalArgumentException if linker is null
   * @since 1.0.0
   */
  public static boolean hasWasiImports(Linker<?> linker) {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }

    // Check for common WASI imports
    for (final String[] entry : WASI_P1_IMPORTS) {
      if (linker.hasImport(entry[0], entry[1])) {
        return true;
      }
    }
    return linker.hasImport("wasi_unstable", "fd_write");
  }

  /**
   * Checks if a linker has WASI Preview 2 imports configured.
   *
   * <p>This method checks for the presence of common WASI Preview 2 imports to determine if WASI
   * Preview 2 functionality has been added to the linker.
   *
   * @param linker the linker to check
   * @return true if WASI Preview 2 imports are present, false otherwise
   * @throws IllegalArgumentException if linker is null
   * @since 1.0.0
   */
  public static boolean hasWasiPreview2Imports(Linker<?> linker) {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }

    // Check for common WASI Preview 2 imports
    for (final String[] entry : WASI_P2_IMPORTS) {
      if (linker.hasImport(entry[0], entry[1])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the current runtime supports the Component Model.
   *
   * @return true if the runtime supports the Component Model, false otherwise
   * @since 1.0.0
   */
  public static boolean runtimeSupportsComponentModel() {
    try {
      return WasmRuntimeFactory.create().supportsComponentModel();
    } catch (Exception e) {
      return false;
    }
  }
}
