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
package ai.tegmentum.wasmtime4j.panama.wasi.cli;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.cli.WasiEnvironment;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiEnvironment interface.
 *
 * <p>This class provides access to WASI Preview 2 environment and command-line operations through
 * Panama Foreign Function API calls to the native Wasmtime library.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class PanamaWasiEnvironment implements WasiEnvironment {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiEnvironment.class.getName());

  // Panama FFI function handles
  private static final MethodHandle GET_ALL_HANDLE;
  private static final MethodHandle GET_HANDLE;
  private static final MethodHandle GET_ARGUMENTS_HANDLE;
  private static final MethodHandle GET_INITIAL_CWD_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeLibraryLoader.getInstance().getSymbolLookup();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_environment_get_all(context, out_env_vars, out_env_vars_len)
      GET_ALL_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_environment_get_all").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_environment_get(context, name, name_len, out_value,
      // out_value_len)
      GET_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_environment_get").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_environment_get_arguments(context, out_args, out_args_len)
      GET_ARGUMENTS_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_environment_get_arguments").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_environment_get_initial_cwd(context, out_cwd, out_cwd_len)
      GET_INITIAL_CWD_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_environment_get_initial_cwd").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe(
          "Failed to initialize Panama FFI handles for WasiEnvironment: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI environment with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is null
   */
  public PanamaWasiEnvironment(final MemorySegment contextHandle) {
    Validation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI environment");
  }

  @Override
  public Map<String, String> getEnvironmentVariables() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outEnvVars = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outEnvVarsLen = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) GET_ALL_HANDLE.invokeExact(contextHandle, outEnvVars, outEnvVarsLen);

      if (result != 0) {
        LOGGER.warning("Failed to get environment variables");
        return Collections.emptyMap();
      }

      final MemorySegment envVarsPtr = outEnvVars.get(ValueLayout.ADDRESS, 0);
      final int envVarsLen = outEnvVarsLen.get(ValueLayout.JAVA_INT, 0);

      if (envVarsPtr == null || envVarsPtr.address() == 0 || envVarsLen == 0) {
        return Collections.emptyMap();
      }

      // Parse null-terminated key=value pairs from the buffer
      final Map<String, String> vars = new HashMap<>();
      int offset = 0;
      while (offset < envVarsLen) {
        final String entry = envVarsPtr.getString(offset, StandardCharsets.UTF_8);
        if (entry.isEmpty()) {
          break;
        }

        // Parse key=value format
        final int equalsPos = entry.indexOf('=');
        if (equalsPos > 0) {
          final String key = entry.substring(0, equalsPos);
          final String value = entry.substring(equalsPos + 1);
          vars.put(key, value);
        }

        offset += entry.getBytes(StandardCharsets.UTF_8).length + 1; // +1 for null terminator
      }

      return Collections.unmodifiableMap(vars);

    } catch (final Throwable e) {
      LOGGER.warning("Error getting environment variables: " + e.getMessage());
      return Collections.emptyMap();
    }
  }

  @Override
  public Optional<String> getVariable(final String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Variable name cannot be null or empty");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
      final MemorySegment nameSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);
      final MemorySegment outValue = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outValueLen = arena.allocate(ValueLayout.JAVA_INT);

      final int result =
          (int)
              GET_HANDLE.invokeExact(
                  contextHandle, nameSegment, nameBytes.length, outValue, outValueLen);

      if (result != 0) {
        return Optional.empty();
      }

      final MemorySegment valuePtr = outValue.get(ValueLayout.ADDRESS, 0);
      final int valueLen = outValueLen.get(ValueLayout.JAVA_INT, 0);

      if (valuePtr == null || valuePtr.address() == 0 || valueLen == 0) {
        return Optional.empty();
      }

      return Optional.of(valuePtr.getString(0, StandardCharsets.UTF_8));

    } catch (final Throwable e) {
      LOGGER.warning("Error getting environment variable '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public List<String> getArguments() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outArgs = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outArgsLen = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) GET_ARGUMENTS_HANDLE.invokeExact(contextHandle, outArgs, outArgsLen);

      if (result != 0) {
        LOGGER.warning("Failed to get command-line arguments");
        return Collections.emptyList();
      }

      final MemorySegment argsPtr = outArgs.get(ValueLayout.ADDRESS, 0);
      final int argsLen = outArgsLen.get(ValueLayout.JAVA_INT, 0);

      if (argsPtr == null || argsPtr.address() == 0 || argsLen == 0) {
        return Collections.emptyList();
      }

      // Parse null-terminated argument strings from the buffer
      final List<String> args = new ArrayList<>();
      int offset = 0;
      while (offset < argsLen) {
        final String arg = argsPtr.getString(offset, StandardCharsets.UTF_8);
        if (arg.isEmpty()) {
          break;
        }
        args.add(arg);
        offset += arg.getBytes(StandardCharsets.UTF_8).length + 1; // +1 for null terminator
      }

      return Collections.unmodifiableList(args);

    } catch (final Throwable e) {
      LOGGER.warning("Error getting command-line arguments: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  @Override
  public Optional<String> getInitialCwd() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outCwd = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outCwdLen = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) GET_INITIAL_CWD_HANDLE.invokeExact(contextHandle, outCwd, outCwdLen);

      if (result != 0) {
        return Optional.empty();
      }

      final MemorySegment cwdPtr = outCwd.get(ValueLayout.ADDRESS, 0);
      final int cwdLen = outCwdLen.get(ValueLayout.JAVA_INT, 0);

      if (cwdPtr == null || cwdPtr.address() == 0 || cwdLen == 0) {
        return Optional.empty();
      }

      return Optional.of(cwdPtr.getString(0, StandardCharsets.UTF_8));

    } catch (final Throwable e) {
      LOGGER.warning("Error getting initial working directory: " + e.getMessage());
      return Optional.empty();
    }
  }
}
