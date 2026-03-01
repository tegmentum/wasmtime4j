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
package ai.tegmentum.wasmtime4j.jni.wasi.cli;

import ai.tegmentum.wasmtime4j.wasi.cli.WasiEnvironment;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiEnvironment interface.
 *
 * <p>This class provides access to WASI Preview 2 environment and command-line operations through
 * JNI calls to the native Wasmtime library.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class JniWasiEnvironment implements WasiEnvironment {

  private static final Logger LOGGER = Logger.getLogger(JniWasiEnvironment.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiEnvironment: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI environment with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is 0
   */
  public JniWasiEnvironment(final long contextHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI environment");
  }

  @Override
  public Map<String, String> getEnvironmentVariables() {
    try {
      final String[] envVars = nativeGetAll(contextHandle);
      if (envVars == null || envVars.length == 0) {
        return Collections.emptyMap();
      }

      final Map<String, String> vars = new HashMap<>();
      for (final String entry : envVars) {
        if (entry == null || entry.isEmpty()) {
          continue;
        }

        // Parse key=value format
        final int equalsPos = entry.indexOf('=');
        if (equalsPos > 0) {
          final String key = entry.substring(0, equalsPos);
          final String value = entry.substring(equalsPos + 1);
          vars.put(key, value);
        }
      }

      return Collections.unmodifiableMap(vars);

    } catch (final Exception e) {
      LOGGER.warning("Error getting environment variables: " + e.getMessage());
      return Collections.emptyMap();
    }
  }

  @Override
  public Optional<String> getVariable(final String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Variable name cannot be null or empty");
    }

    try {
      final String value = nativeGet(contextHandle, name);
      return Optional.ofNullable(value);
    } catch (final Exception e) {
      LOGGER.warning("Error getting environment variable '" + name + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public List<String> getArguments() {
    try {
      final String[] args = nativeGetArguments(contextHandle);
      if (args == null || args.length == 0) {
        return Collections.emptyList();
      }

      return Collections.unmodifiableList(java.util.Arrays.asList(args));

    } catch (final Exception e) {
      LOGGER.warning("Error getting command-line arguments: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  @Override
  public Optional<String> getInitialCwd() {
    try {
      final String cwd = nativeGetInitialCwd(contextHandle);
      return Optional.ofNullable(cwd);
    } catch (final Exception e) {
      LOGGER.warning("Error getting initial working directory: " + e.getMessage());
      return Optional.empty();
    }
  }

  // Native method declarations

  /**
   * Gets all environment variables.
   *
   * @param contextHandle the native context handle
   * @return array of environment variable strings in key=value format
   */
  private static native String[] nativeGetAll(long contextHandle);

  /**
   * Gets a specific environment variable.
   *
   * @param contextHandle the native context handle
   * @param name the variable name
   * @return the variable value, or null if not found
   */
  private static native String nativeGet(long contextHandle, String name);

  /**
   * Gets command-line arguments.
   *
   * @param contextHandle the native context handle
   * @return array of command-line argument strings
   */
  private static native String[] nativeGetArguments(long contextHandle);

  /**
   * Gets the initial working directory.
   *
   * @param contextHandle the native context handle
   * @return the initial working directory path, or null if not set
   */
  private static native String nativeGetInitialCwd(long contextHandle);
}
