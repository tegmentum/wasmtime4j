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
package ai.tegmentum.wasmtime4j.tests.framework;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that ensures proper cleanup of runtime state between tests.
 *
 * <p>This extension handles two levels of cleanup:
 *
 * <ul>
 *   <li><b>After each test:</b> Clears the runtime selection so that subsequent tests start with a
 *       clean slate and are not affected by the runtime choice of the previous test.
 *   <li><b>After all tests in a class:</b> Attempts to clear native handle registries to prevent
 *       resource leaks and test isolation issues across test classes.
 * </ul>
 *
 * @since 1.0.0
 */
public class DualRuntimeCleanupExtension implements AfterEachCallback, AfterAllCallback {

  private static final Logger LOGGER =
      Logger.getLogger(DualRuntimeCleanupExtension.class.getName());

  private static final String TEST_UTILS_CLASS = "ai.tegmentum.wasmtime4j.test.TestUtils";
  private static final String ENGINE_CLASS = "ai.tegmentum.wasmtime4j.Engine";
  private static final String CLEAR_HANDLE_REGISTRIES = "clearHandleRegistries";

  @Override
  public void afterEach(final ExtensionContext context) {
    DualRuntimeTest.clearRuntimeSelection();
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    clearHandleRegistries();
  }

  /**
   * Attempts to clear native handle registries via reflection.
   *
   * <p>First tries {@code TestUtils.clearHandleRegistries()}, falling back to {@code
   * Engine.clearHandleRegistries()} if the test utility class is not available. Any reflection
   * failures are logged at {@link Level#FINE} and silently ignored.
   */
  private static void clearHandleRegistries() {
    // Try TestUtils first
    try {
      final Class<?> testUtilsClass = Class.forName(TEST_UTILS_CLASS);
      final Method method = testUtilsClass.getMethod(CLEAR_HANDLE_REGISTRIES);
      method.invoke(null);
      LOGGER.fine("Cleared handle registries via TestUtils");
      return;
    } catch (final Exception e) {
      LOGGER.log(Level.FINE, "TestUtils.clearHandleRegistries() not available", e);
    }

    // Fall back to Engine
    try {
      final Class<?> engineClass = Class.forName(ENGINE_CLASS);
      final Method method = engineClass.getMethod(CLEAR_HANDLE_REGISTRIES);
      method.invoke(null);
      LOGGER.fine("Cleared handle registries via Engine");
    } catch (final Exception e) {
      LOGGER.log(Level.FINE, "Engine.clearHandleRegistries() not available", e);
    }
  }
}
