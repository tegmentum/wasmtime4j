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
package ai.tegmentum.wasmtime4j.testing;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 execution condition that checks if a WebAssembly runtime is available.
 *
 * <p>This condition evaluates whether either the JNI or Panama runtime implementation is available
 * for executing tests that require the native Wasmtime library. Tests that use this condition will
 * be skipped with a descriptive message if no runtime is available.
 *
 * <p>This condition is automatically applied when using the {@link RequiresWasmRuntime} annotation.
 *
 * <p>The condition performs the following checks:
 *
 * <ol>
 *   <li>Attempts to detect JNI runtime availability via {@link WasmRuntimeFactory}
 *   <li>Attempts to detect Panama runtime availability via {@link WasmRuntimeFactory}
 *   <li>Returns enabled if either runtime is available, disabled otherwise
 * </ol>
 *
 * @since 1.0.0
 * @see RequiresWasmRuntime
 */
public class WasmRuntimeCondition implements ExecutionCondition {

  private static final Logger LOGGER = Logger.getLogger(WasmRuntimeCondition.class.getName());

  private static volatile Boolean runtimeAvailable;
  private static volatile String availableRuntimeType;

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(
      final ExtensionContext extensionContext) {
    if (isRuntimeAvailable()) {
      return ConditionEvaluationResult.enabled(
          "WebAssembly runtime is available (" + availableRuntimeType + ")");
    } else {
      return ConditionEvaluationResult.disabled(
          "WebAssembly runtime is not available. "
              + "Neither JNI nor Panama runtime implementations could be loaded. "
              + "This test requires the native Wasmtime library to be present.");
    }
  }

  /**
   * Checks if a WebAssembly runtime is available.
   *
   * <p>This method checks for both JNI and Panama runtime implementations and caches the result for
   * performance.
   *
   * @return true if either JNI or Panama runtime is available
   */
  public static boolean isRuntimeAvailable() {
    if (runtimeAvailable != null) {
      return runtimeAvailable;
    }

    synchronized (WasmRuntimeCondition.class) {
      if (runtimeAvailable != null) {
        return runtimeAvailable;
      }

      try {
        if (WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI)) {
          runtimeAvailable = true;
          availableRuntimeType = "JNI";
          LOGGER.info("JNI WebAssembly runtime is available for tests");
          return true;
        }
      } catch (final Exception e) {
        LOGGER.fine("JNI runtime check failed: " + e.getMessage());
      }

      try {
        if (WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA)) {
          runtimeAvailable = true;
          availableRuntimeType = "Panama";
          LOGGER.info("Panama WebAssembly runtime is available for tests");
          return true;
        }
      } catch (final Exception e) {
        LOGGER.fine("Panama runtime check failed: " + e.getMessage());
      }

      runtimeAvailable = false;
      availableRuntimeType = "none";
      LOGGER.warning(
          "No WebAssembly runtime available. Tests requiring @RequiresWasmRuntime will be"
              + " skipped.");
      return false;
    }
  }

  /**
   * Gets the type of runtime that is available.
   *
   * @return "JNI", "Panama", or "none" depending on availability
   */
  public static String getAvailableRuntimeType() {
    isRuntimeAvailable();
    return availableRuntimeType;
  }

  /**
   * Clears the cached runtime availability check. This is primarily useful for testing the
   * condition itself.
   */
  public static void clearCache() {
    synchronized (WasmRuntimeCondition.class) {
      runtimeAvailable = null;
      availableRuntimeType = null;
    }
  }
}
