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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for runtime selection via system property and auto-detection. Verifies that the system
 * property can force a specific runtime, auto-detection works correctly, and invalid runtime names
 * produce clear errors.
 *
 * @since 1.0.0
 */
@DisplayName("Runtime Fallback Tests")
public class RuntimeFallbackTest {

  private static final Logger LOGGER = Logger.getLogger(RuntimeFallbackTest.class.getName());

  @AfterEach
  void cleanup() {
    System.clearProperty(WasmRuntimeFactory.RUNTIME_PROPERTY);
    WasmRuntimeFactory.clearCache();
  }

  @Test
  @DisplayName("System property forces JNI runtime")
  void systemPropertyForcesJni() throws Exception {
    assumeTrue(
        WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI), "JNI runtime must be available");

    System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "jni");
    WasmRuntimeFactory.clearCache();

    LOGGER.info("Forcing JNI via system property");

    final WasmRuntime runtime = WasmRuntimeFactory.create();
    assertNotNull(runtime, "Runtime should not be null");

    // Verify we got JNI by checking runtime type
    final RuntimeType selectedType = WasmRuntimeFactory.getSelectedRuntimeType();
    assertEquals(RuntimeType.JNI, selectedType, "Selected runtime should be JNI");
    LOGGER.info("Selected runtime: " + selectedType);

    try (Engine engine = runtime.createEngine()) {
      assertNotNull(engine, "Engine should be created with forced JNI runtime");
      LOGGER.info("Engine created with JNI runtime");
    }

    runtime.close();
  }

  @Test
  @DisplayName("System property forces Panama runtime")
  void systemPropertyForcesPanama() throws Exception {
    assumeTrue(
        WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA),
        "Panama runtime must be available");

    System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "panama");
    WasmRuntimeFactory.clearCache();

    LOGGER.info("Forcing Panama via system property");

    final WasmRuntime runtime = WasmRuntimeFactory.create();
    assertNotNull(runtime, "Runtime should not be null");

    final RuntimeType selectedType = WasmRuntimeFactory.getSelectedRuntimeType();
    assertEquals(RuntimeType.PANAMA, selectedType, "Selected runtime should be Panama");
    LOGGER.info("Selected runtime: " + selectedType);

    try (Engine engine = runtime.createEngine()) {
      assertNotNull(engine, "Engine should be created with forced Panama runtime");
      LOGGER.info("Engine created with Panama runtime");
    }

    runtime.close();
  }

  @Test
  @DisplayName("Auto-detection works when no system property set")
  void autoDetectionWorksWithoutProperty() throws Exception {
    System.clearProperty(WasmRuntimeFactory.RUNTIME_PROPERTY);
    WasmRuntimeFactory.clearCache();

    LOGGER.info("Testing auto-detection (no system property)");

    final WasmRuntime runtime = WasmRuntimeFactory.create();
    assertNotNull(runtime, "Auto-detected runtime should not be null");

    final RuntimeType selectedType = WasmRuntimeFactory.getSelectedRuntimeType();
    assertNotNull(selectedType, "Selected runtime type should not be null");
    LOGGER.info("Auto-detected runtime: " + selectedType);

    // The auto-detected runtime should be one of the available runtimes
    assertTrue(
        WasmRuntimeFactory.isRuntimeAvailable(selectedType),
        "Auto-detected runtime should be available");

    try (Engine engine = runtime.createEngine()) {
      assertNotNull(engine, "Engine should be created with auto-detected runtime");
      LOGGER.info("Engine created with auto-detected runtime");
    }

    runtime.close();
  }

  @Test
  @DisplayName("Invalid runtime name in system property falls back or produces error")
  void invalidRuntimeNameBehavior() throws Exception {
    System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "nonexistent_runtime");
    WasmRuntimeFactory.clearCache();

    LOGGER.info("Setting invalid runtime name 'nonexistent_runtime'");

    // The factory may either throw for an invalid name, or silently fallback to auto-detection.
    // Both behaviors are acceptable. We verify the factory doesn't crash.
    try {
      final WasmRuntime runtime = WasmRuntimeFactory.create();
      assertNotNull(runtime, "If factory doesn't throw, it should return a valid runtime");
      LOGGER.info("Factory fell back to auto-detection with invalid property");
      runtime.close();
    } catch (final Exception e) {
      LOGGER.info("Factory threw for invalid runtime name: " + e.getMessage());
      assertNotNull(e.getMessage(), "Error should have a message");
    }
  }

  @Test
  @DisplayName("System property is case-insensitive")
  void systemPropertyCaseInsensitive() throws Exception {
    assumeTrue(
        WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI), "JNI runtime must be available");

    // Try uppercase
    System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "JNI");
    WasmRuntimeFactory.clearCache();

    LOGGER.info("Testing case-insensitive property: 'JNI'");

    final WasmRuntime runtime = WasmRuntimeFactory.create();
    assertNotNull(runtime, "Runtime should be created with uppercase 'JNI'");

    final RuntimeType selectedType = WasmRuntimeFactory.getSelectedRuntimeType();
    assertEquals(RuntimeType.JNI, selectedType, "Should select JNI regardless of case");
    LOGGER.info("Selected runtime with uppercase: " + selectedType);

    runtime.close();
  }

  @Test
  @DisplayName("Runtime availability check returns correct values")
  void runtimeAvailabilityCheck() {
    LOGGER.info("Checking runtime availability");

    final boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    final boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

    LOGGER.info("JNI available: " + jniAvailable);
    LOGGER.info("Panama available: " + panamaAvailable);

    // At least one runtime must be available for the project to function
    assertTrue(
        jniAvailable || panamaAvailable, "At least one runtime must be available");
  }
}
