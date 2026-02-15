/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wast;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating WastRunner instances based on the available runtime.
 *
 * <p>This factory selects the appropriate WastRunner implementation based on the Java version and
 * available native bindings, following the same runtime selection logic as {@code
 * WasmRuntimeFactory}.
 */
final class WastRunnerFactory {

  private static final Logger LOGGER = Logger.getLogger(WastRunnerFactory.class.getName());

  private static final String PANAMA_RUNNER_CLASS =
      "ai.tegmentum.wasmtime4j.panama.PanamaWastRunner";
  private static final String JNI_RUNNER_CLASS = "ai.tegmentum.wasmtime4j.jni.JniWastRunner";

  private WastRunnerFactory() {
    // Utility class
  }

  /**
   * Creates a WastRunner using the best available implementation.
   *
   * @return a new WastRunner instance
   * @throws IllegalStateException if no implementation is available
   */
  static WastRunner createRunner() {
    final String override = System.getProperty("wasmtime4j.runtime");

    if ("panama".equalsIgnoreCase(override)) {
      return createPanamaRunner();
    } else if ("jni".equalsIgnoreCase(override)) {
      return createJniRunner();
    }

    // Auto-detect: Panama for Java 23+, JNI otherwise
    final int javaVersion = Runtime.version().feature();
    if (javaVersion >= 23) {
      try {
        return createPanamaRunner();
      } catch (IllegalStateException e) {
        LOGGER.log(Level.WARNING,
            "Panama WastRunner not available on Java " + javaVersion + ", falling back to JNI", e);
      }
    }

    return createJniRunner();
  }

  private static WastRunner createPanamaRunner() {
    return new DelegatingWastRunner(PANAMA_RUNNER_CLASS);
  }

  private static WastRunner createJniRunner() {
    return new DelegatingWastRunner(JNI_RUNNER_CLASS);
  }

  /**
   * WastRunner implementation that delegates to static methods on the underlying runner class.
   *
   * <p>Both JniWastRunner and PanamaWastRunner use static methods, so this wrapper adapts them to
   * the WastRunner interface.
   */
  private static final class DelegatingWastRunner implements WastRunner {

    private final Method executeFileMethod;
    private final Method executeStringMethod;
    private final Method executeBytesMethod;

    DelegatingWastRunner(final String className) {
      try {
        final Class<?> runnerClass = Class.forName(className);

        this.executeFileMethod = runnerClass.getMethod("executeWastFile", String.class);
        this.executeStringMethod =
            runnerClass.getMethod("executeWastString", String.class, String.class);
        this.executeBytesMethod =
            runnerClass.getMethod("executeWastBytes", String.class, byte[].class);

        LOGGER.fine("Created WastRunner using " + className);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("WastRunner class not found: " + className, e);
      } catch (NoSuchMethodException e) {
        throw new IllegalStateException(
            "WastRunner class missing required methods: " + className, e);
      }
    }

    @Override
    public WastExecutionResult executeWastFile(final String filePath) {
      if (filePath == null || filePath.isEmpty()) {
        throw new IllegalArgumentException("File path cannot be null or empty");
      }
      try {
        return (WastExecutionResult) executeFileMethod.invoke(null, filePath);
      } catch (java.lang.reflect.InvocationTargetException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof IllegalArgumentException) {
          throw (IllegalArgumentException) cause;
        }
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        throw new RuntimeException("WAST file execution failed", cause);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Cannot access WAST runner method", e);
      }
    }

    @Override
    public WastExecutionResult executeWastString(final String filename, final String wastContent) {
      if (filename == null || filename.isEmpty()) {
        throw new IllegalArgumentException("Filename cannot be null or empty");
      }
      if (wastContent == null || wastContent.isEmpty()) {
        throw new IllegalArgumentException("WAST content cannot be null or empty");
      }
      try {
        return (WastExecutionResult) executeStringMethod.invoke(null, filename, wastContent);
      } catch (java.lang.reflect.InvocationTargetException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof IllegalArgumentException) {
          throw (IllegalArgumentException) cause;
        }
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        throw new RuntimeException("WAST string execution failed", cause);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Cannot access WAST runner method", e);
      }
    }

    @Override
    public WastExecutionResult executeWastBytes(final String filename, final byte[] wastContent) {
      if (filename == null || filename.isEmpty()) {
        throw new IllegalArgumentException("Filename cannot be null or empty");
      }
      if (wastContent == null) {
        throw new IllegalArgumentException("WAST content cannot be null");
      }
      try {
        return (WastExecutionResult) executeBytesMethod.invoke(null, filename, wastContent);
      } catch (java.lang.reflect.InvocationTargetException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof IllegalArgumentException) {
          throw (IllegalArgumentException) cause;
        }
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        throw new RuntimeException("WAST bytes execution failed", cause);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Cannot access WAST runner method", e);
      }
    }
  }
}
