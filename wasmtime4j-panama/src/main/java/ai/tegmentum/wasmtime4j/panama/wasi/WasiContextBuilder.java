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

import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.wasi.AbstractWasiContextBuilder;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Panama implementation of the WASI context builder.
 *
 * <p>This builder provides a fluent API for configuring WASI contexts using Panama Foreign Function
 * Interface bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class WasiContextBuilder extends AbstractWasiContextBuilder<WasiContextBuilder> {

  private static final Logger LOGGER = Logger.getLogger(WasiContextBuilder.class.getName());

  /** Package-private constructor - use WasiContext.builder() to create. */
  WasiContextBuilder() {
    super();
  }

  /**
   * Creates a WASI context with the configured settings.
   *
   * @return the configured WASI context
   * @throws RuntimeException if the WASI context cannot be created
   */
  public WasiContext build() {
    LOGGER.info(
        String.format(
            "Building Panama WASI context with %d environment variables, %d arguments, %d preopen"
                + " directories",
            getEnvironment().size(), getArguments().size(), getPreopenedDirectories().size()));

    validateConfiguration();

    final ArenaResourceManager resourceManager = new ArenaResourceManager(Arena.ofConfined(), true);

    try {
      final String[] envArray = convertEnvironmentToArray();
      final String[] argArray = getArguments().toArray(new String[0]);
      final String[] preopenArray = convertPreopenDirectoriesToArray();
      final String workingDirStr = getWorkingDirectory().toString();

      final MemorySegment nativeHandle =
          nativeCreate(envArray, argArray, preopenArray, workingDirStr);

      final ai.tegmentum.wasmtime4j.panama.NativeWasiBindings bindings =
          ai.tegmentum.wasmtime4j.panama.NativeWasiBindings.getInstance();

      bindings.wasiContextSetNetworkConfig(
          nativeHandle,
          isAllowNetwork() ? 1 : 0,
          isAllowTcp() ? 1 : 0,
          isAllowUdp() ? 1 : 0,
          isAllowIpNameLookup() ? 1 : 0);

      if (isAllowBlockingCurrentThread()) {
        bindings.wasiContextSetAllowBlocking(nativeHandle, 1);
      }

      if (getInsecureRandomSeed() != 0) {
        bindings.wasiContextSetInsecureRandomSeed(nativeHandle, getInsecureRandomSeed(), 0);
      }

      bindings.wasiContextRebuild(nativeHandle);

      return new WasiContext(nativeHandle, resourceManager, this);

    } catch (final Exception e) {
      try {
        resourceManager.close();
      } catch (final Exception cleanupException) {
        LOGGER.warning(
            "Error cleaning up resource manager after failed context creation: "
                + cleanupException.getMessage());
        e.addSuppressed(cleanupException);
      }

      throw new RuntimeException("Failed to create Panama WASI context: " + e.getMessage(), e);
    }
  }

  /**
   * Native method to create a new WASI context using Panama FFI.
   *
   * @param environment environment variables as key=value pairs
   * @param arguments command-line arguments
   * @param preopenDirs pre-opened directory mappings
   * @param workingDir working directory path
   * @return the native handle for the created WASI context
   * @throws RuntimeException if the context cannot be created
   */
  private static MemorySegment nativeCreate(
      final String[] environment,
      final String[] arguments,
      final String[] preopenDirs,
      final String workingDir) {

    LOGGER.fine(
        String.format(
            "Native WASI context creation called with %d env vars, %d args, %d preopen dirs",
            environment.length, arguments.length, preopenDirs.length / 2));

    try {
      ai.tegmentum.wasmtime4j.panama.NativeWasiBindings bindings =
          ai.tegmentum.wasmtime4j.panama.NativeWasiBindings.getInstance();

      if (!bindings.isInitialized()) {
        throw new RuntimeException("Native function bindings not initialized");
      }

      MemorySegment contextHandle = bindings.wasiContextCreate();

      if (contextHandle == null || contextHandle.equals(MemorySegment.NULL)) {
        throw new RuntimeException("Failed to create native WASI context");
      }

      LOGGER.fine("Created native WASI context with handle: " + contextHandle.address());

      if (environment.length > 0) {
        try (Arena arena = Arena.ofConfined()) {
          for (String envVar : environment) {
            String[] parts = envVar.split("=", 2);
            if (parts.length == 2) {
              MemorySegment keySegment = arena.allocateFrom(parts[0]);
              MemorySegment valueSegment = arena.allocateFrom(parts[1]);

              int result = bindings.wasiContextSetEnv(contextHandle, keySegment, valueSegment);
              if (result != 0) {
                LOGGER.warning(
                    "Failed to set environment variable: "
                        + parts[0]
                        + ": "
                        + PanamaErrorMapper.getErrorDescription(result));
              }
            }
          }
        }
      }

      if (arguments.length > 0) {
        try (Arena arena = Arena.ofConfined()) {
          MemorySegment argsArray = arena.allocate(ValueLayout.ADDRESS, arguments.length);
          for (int i = 0; i < arguments.length; i++) {
            MemorySegment argString = arena.allocateFrom(arguments[i]);
            argsArray.setAtIndex(ValueLayout.ADDRESS, i, argString);
          }

          int result = bindings.wasiContextSetArgv(contextHandle, argsArray, arguments.length);
          if (result != 0) {
            LOGGER.warning(
                "Failed to set command line arguments: "
                    + PanamaErrorMapper.getErrorDescription(result));
          }
        }
      }

      if (preopenDirs.length > 0) {
        try (Arena arena = Arena.ofConfined()) {
          for (int i = 0; i < preopenDirs.length; i += 2) {
            String guestPath = preopenDirs[i];
            String hostPath = preopenDirs[i + 1];

            MemorySegment guestSegment = arena.allocateFrom(guestPath);
            MemorySegment hostSegment = arena.allocateFrom(hostPath);

            int result =
                bindings.wasiContextPreopenDirReadonly(contextHandle, hostSegment, guestSegment);

            if (result != 0) {
              LOGGER.warning(
                  "Failed to add directory mapping "
                      + guestPath
                      + " -> "
                      + hostPath
                      + ": "
                      + PanamaErrorMapper.getErrorDescription(result));
            }
          }
        }
      }

      return contextHandle;

    } catch (Exception e) {
      LOGGER.severe("Exception during native WASI context creation: " + e.getMessage());
      throw new RuntimeException("Failed to create native WASI context", e);
    }
  }
}
