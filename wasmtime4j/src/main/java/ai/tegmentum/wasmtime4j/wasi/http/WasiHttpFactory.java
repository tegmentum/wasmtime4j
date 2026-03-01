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
package ai.tegmentum.wasmtime4j.wasi.http;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Factory for creating WASI HTTP contexts.
 *
 * <p>WasiHttpFactory provides methods for creating {@link WasiHttpContext} instances with various
 * configurations. The factory automatically selects the appropriate implementation based on the
 * runtime environment (Panama for Java 23+, JNI for earlier versions).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a context with default configuration (blocks all hosts)
 * WasiHttpContext context = WasiHttpFactory.createContext();
 *
 * // Create a context with custom configuration
 * WasiHttpConfig config = WasiHttpConfig.builder()
 *     .allowHost("api.example.com")
 *     .withConnectTimeout(Duration.ofSeconds(30))
 *     .build();
 * WasiHttpContext customContext = WasiHttpFactory.createContext(config);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiHttpFactory {

  private WasiHttpFactory() {
    // Utility class - prevent instantiation
  }

  /**
   * Creates a new WASI HTTP context with default configuration.
   *
   * <p>The default configuration blocks all outbound HTTP requests. Use {@link
   * #createContext(WasiHttpConfig)} to allow specific hosts.
   *
   * @return a new WasiHttpContext instance
   * @throws WasmException if the context cannot be created
   */
  public static WasiHttpContext createContext() throws WasmException {
    return createContext(WasiHttpConfig.defaultConfig());
  }

  /**
   * Creates a new WASI HTTP context with the specified configuration.
   *
   * @param config the HTTP configuration
   * @return a new WasiHttpContext instance
   * @throws WasmException if the context cannot be created
   * @throws IllegalArgumentException if config is null
   */
  public static WasiHttpContext createContext(final WasiHttpConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null");
    }

    config.validate();

    try {
      final Class<?> contextClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.wasi.http.PanamaWasiHttpContext");
      return (WasiHttpContext)
          contextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
    } catch (final ClassNotFoundException e) {
      try {
        final Class<?> contextClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
        return (WasiHttpContext)
            contextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      } catch (final ClassNotFoundException e2) {
        throw new WasmException(
            "No WasiHttpContext implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new WasmException("Failed to create WASI HTTP context: " + e2.getMessage(), e2);
      }
    } catch (final Exception e) {
      throw new WasmException("Failed to create WASI HTTP context: " + e.getMessage(), e);
    }
  }

  /**
   * Checks if WASI HTTP support is available.
   *
   * @return true if WASI HTTP support is available, false otherwise
   */
  public static boolean isAvailable() {
    try {
      Class.forName("ai.tegmentum.wasmtime4j.panama.wasi.http.PanamaWasiHttpContext");
      return true;
    } catch (final ClassNotFoundException e) {
      try {
        Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
        return true;
      } catch (final ClassNotFoundException e2) {
        return false;
      }
    }
  }

  /**
   * Gets the name of the WASI HTTP implementation that will be used.
   *
   * @return the implementation name ("Panama", "JNI", or "None")
   */
  public static String getImplementationName() {
    try {
      Class.forName("ai.tegmentum.wasmtime4j.panama.wasi.http.PanamaWasiHttpContext");
      return "Panama";
    } catch (final ClassNotFoundException e) {
      try {
        Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
        return "JNI";
      } catch (final ClassNotFoundException e2) {
        return "None";
      }
    }
  }
}
