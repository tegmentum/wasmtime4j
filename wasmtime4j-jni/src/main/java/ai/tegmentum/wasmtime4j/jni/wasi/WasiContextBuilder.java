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

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.AbstractWasiContextBuilder;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * JNI implementation of the WASI context builder.
 *
 * <p>This builder provides a fluent API for configuring WASI contexts using JNI native bindings.
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
   * @throws JniException if the WASI context cannot be created
   */
  public WasiContext build() throws JniException {
    LOGGER.info(
        String.format(
            "Building WASI context with %d environment variables, %d arguments, %d preopen"
                + " directories",
            getEnvironment().size(), getArguments().size(), getPreopenedDirectories().size()));

    validateConfiguration();

    final String[] envArray = convertEnvironmentToArray();
    final String[] argArray = getArguments().toArray(new String[0]);
    final String[] preopenArray = convertPreopenDirectoriesToArray();
    final String workingDirStr = getWorkingDirectory().toString();

    try {
      final long nativeHandle =
          WasiContext.nativeCreate(envArray, argArray, preopenArray, workingDirStr);

      WasiContext.nativeSetNetworkConfig(
          nativeHandle, isAllowNetwork(), isAllowTcp(), isAllowUdp(), isAllowIpNameLookup());

      if (isAllowBlockingCurrentThread()) {
        WasiContext.nativeSetAllowBlocking(nativeHandle, true);
      }

      if (getInsecureRandomSeed() != 0) {
        WasiContext.nativeSetInsecureRandomSeed(nativeHandle, getInsecureRandomSeed(), 0);
      }

      WasiContext.nativeRebuildContext(nativeHandle);

      return new WasiContext(nativeHandle, this);

    } catch (final Exception e) {
      throw new JniException("Failed to create WASI context: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new builder instance.
   *
   * @return a new WasiContextBuilder
   */
  public static WasiContextBuilder builder() {
    return new WasiContextBuilder();
  }

  /**
   * Adds a preopen directory (alias for withPreopenDirectory).
   *
   * @param hostPath the host directory path
   * @param guestPath the guest directory path
   * @return this builder for method chaining
   * @throws IllegalArgumentException if hostPath is null
   */
  public WasiContextBuilder addPreopenedDirectory(final Path hostPath, final String guestPath) {
    Validation.requireNonNull(hostPath, "hostPath");
    return withPreopenDirectory(guestPath, hostPath.toString());
  }

  /**
   * Adds an environment variable (alias for withEnvironment).
   *
   * @param name the environment variable name
   * @param value the environment variable value
   * @return this builder for method chaining
   */
  public WasiContextBuilder addEnvironmentVariable(final String name, final String value) {
    return withEnvironment(name, value);
  }
}
