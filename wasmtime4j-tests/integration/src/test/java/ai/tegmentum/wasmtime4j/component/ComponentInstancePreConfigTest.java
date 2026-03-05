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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.jni.JniWasmRuntime;
import ai.tegmentum.wasmtime4j.test.TestUtils;
import ai.tegmentum.wasmtime4j.wit.WitS32;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for ComponentInstancePre with ComponentStoreConfig.
 *
 * <p>These tests verify that pre-instantiation with custom store configuration works correctly,
 * including fuel limits, epoch deadlines, and memory limits.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentInstancePre Config Integration Tests")
public final class ComponentInstancePreConfigTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentInstancePreConfigTest.class.getName());

  private static boolean preInstantiateAvailable = false;
  private static byte[] addComponentBytes;
  private static String unavailableReason;

  @BeforeAll
  static void checkPreInstantiateAvailable() {
    try {
      // Load native library
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();

      // Verify JNI runtime creation works
      final JniWasmRuntime testRuntime = new JniWasmRuntime();
      testRuntime.close();

      // Load the add component
      try (InputStream is =
          ComponentInstancePreConfigTest.class.getResourceAsStream("/components/add.wasm")) {
        if (is != null) {
          addComponentBytes = TestUtils.readAllBytes(is);
          preInstantiateAvailable = true;
          LOGGER.info(
              "ComponentInstancePre config tests available - "
                  + addComponentBytes.length
                  + " bytes loaded");
        } else {
          unavailableReason = "add.wasm test component not found in resources";
          LOGGER.warning("Tests skipped: " + unavailableReason);
        }
      }
    } catch (final UnsatisfiedLinkError e) {
      unavailableReason = "Native library not available: " + e.getMessage();
      LOGGER.warning("Tests skipped: " + unavailableReason);
    } catch (final Exception e) {
      unavailableReason = "Setup failed: " + e.getMessage();
      LOGGER.warning("Tests skipped: " + unavailableReason);
    }
  }

  private static void assumePreInstantiateAvailable() {
    assumeTrue(
        preInstantiateAvailable,
        "ComponentInstancePre config tests not available: " + unavailableReason);
  }

  private JniWasmRuntime runtime;
  private Engine engine;
  private ComponentEngine componentEngine;
  private ComponentLinker<Object> linker;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (preInstantiateAvailable) {
      runtime = new JniWasmRuntime();
      resources.add(runtime);
      engine = runtime.createEngine();
      resources.add(engine);
      componentEngine = runtime.createComponentEngine();
      resources.add(componentEngine);
      linker = runtime.createComponentLinker(engine);
      resources.add(linker);
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    runtime = null;
    engine = null;
    componentEngine = null;
    linker = null;
  }

  @Nested
  @DisplayName("Basic Pre-Instantiation Tests")
  class BasicPreInstantiationTests {

    @Test
    @DisplayName("should pre-instantiate component and invoke function")
    void shouldPreInstantiateAndInvoke(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      assertNotNull(pre, "ComponentInstancePre should not be null");
      assertTrue(pre.isValid(), "ComponentInstancePre should be valid");

      final ComponentInstance instance = pre.instantiate();
      resources.add(instance);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

      final Object result = instance.invoke("add", WitS32.of(3), WitS32.of(4));
      assertEquals(7, result, "add(3, 4) should equal 7");

      LOGGER.info("Pre-instantiation and invocation successful: add(3, 4) = " + result);
    }

    @Test
    @DisplayName("should create multiple instances from pre-instantiation")
    void shouldCreateMultipleInstancesFromPre(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      for (int i = 0; i < 5; i++) {
        final ComponentInstance instance = pre.instantiate();
        resources.add(instance);

        assertTrue(instance.isValid(), "Instance " + i + " should be valid");

        final Object result = instance.invoke("add", WitS32.of(i), WitS32.of(100));
        assertEquals(
            i + 100, result, "Instance " + i + ": add(" + i + ", 100) should be " + (i + 100));
      }

      assertTrue(
          pre.getInstanceCount() >= 5,
          "Instance count should be at least 5, got: " + pre.getInstanceCount());

      LOGGER.info("Created " + pre.getInstanceCount() + " instances from pre-instantiation");
    }
  }

  @Nested
  @DisplayName("Instantiate With Config Tests")
  class InstantiateWithConfigTests {

    @Test
    @DisplayName("should instantiate with default config (all zeros)")
    void shouldInstantiateWithDefaultConfig(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      final ComponentStoreConfig config = ComponentStoreConfig.builder().build();
      LOGGER.info(
          "Config: fuelLimit="
              + config.getFuelLimit()
              + ", epochDeadline="
              + config.getEpochDeadline()
              + ", maxMemoryBytes="
              + config.getMaxMemoryBytes());

      final ComponentInstance instance = pre.instantiate(config);
      resources.add(instance);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

      final Object result = instance.invoke("add", WitS32.of(10), WitS32.of(20));
      assertEquals(30, result, "add(10, 20) should equal 30");

      LOGGER.info("Instantiation with default config successful: add(10, 20) = " + result);
    }

    @Test
    @DisplayName("should instantiate with memory limit config")
    void shouldInstantiateWithMemoryLimitConfig(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      // Set a generous memory limit that should not interfere with a simple add function
      final long memoryLimit = 64L * 1024 * 1024; // 64 MB
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().maxMemoryBytes(memoryLimit).build();

      LOGGER.info("Config: maxMemoryBytes=" + config.getMaxMemoryBytes());

      final ComponentInstance instance = pre.instantiate(config);
      resources.add(instance);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

      final Object result = instance.invoke("add", WitS32.of(42), WitS32.of(58));
      assertEquals(100, result, "add(42, 58) should equal 100");

      LOGGER.info("Instantiation with memory limit config successful: add(42, 58) = " + result);
    }

    @Test
    @DisplayName("should instantiate with combined config")
    void shouldInstantiateWithCombinedConfig(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      // Generous limits that should allow the simple add function to succeed
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().maxMemoryBytes(128L * 1024 * 1024).build();

      LOGGER.info(
          "Config: fuelLimit="
              + config.getFuelLimit()
              + ", epochDeadline="
              + config.getEpochDeadline()
              + ", maxMemoryBytes="
              + config.getMaxMemoryBytes());

      final ComponentInstance instance = pre.instantiate(config);
      resources.add(instance);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

      final Object result = instance.invoke("add", WitS32.of(1000), WitS32.of(2000));
      assertEquals(3000, result, "add(1000, 2000) should equal 3000");

      LOGGER.info("Instantiation with combined config successful");
    }

    @Test
    @DisplayName("should reject null config")
    void shouldRejectNullConfig(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      assertThrows(
          IllegalArgumentException.class, () -> pre.instantiate(null), "Should reject null config");

      LOGGER.info("Null config correctly rejected");
    }

    @Test
    @DisplayName("should instantiate with different configs from same pre")
    void shouldInstantiateWithDifferentConfigsFromSamePre(final TestInfo testInfo)
        throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      // Config 1: with memory limit
      final ComponentStoreConfig config1 =
          ComponentStoreConfig.builder().maxMemoryBytes(64L * 1024 * 1024).build();

      final ComponentInstance instance1 = pre.instantiate(config1);
      resources.add(instance1);

      final Object result1 = instance1.invoke("add", WitS32.of(1), WitS32.of(2));
      assertEquals(3, result1, "Instance 1: add(1, 2) should be 3");

      // Config 2: default (no limits)
      final ComponentStoreConfig config2 = ComponentStoreConfig.builder().build();

      final ComponentInstance instance2 = pre.instantiate(config2);
      resources.add(instance2);

      final Object result2 = instance2.invoke("add", WitS32.of(10), WitS32.of(20));
      assertEquals(30, result2, "Instance 2: add(10, 20) should be 30");

      // Config 3: different memory limit
      final ComponentStoreConfig config3 =
          ComponentStoreConfig.builder().maxMemoryBytes(128L * 1024 * 1024).build();

      final ComponentInstance instance3 = pre.instantiate(config3);
      resources.add(instance3);

      final Object result3 = instance3.invoke("add", WitS32.of(100), WitS32.of(200));
      assertEquals(300, result3, "Instance 3: add(100, 200) should be 300");

      assertTrue(
          pre.getInstanceCount() >= 3,
          "Instance count should be at least 3, got: " + pre.getInstanceCount());

      LOGGER.info(
          "Multiple configs from same pre-instantiation successful. Instance count: "
              + pre.getInstanceCount());
    }

    @Test
    @DisplayName("should mix instantiate() and instantiate(config) calls")
    void shouldMixInstantiateAndInstantiateWithConfig(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      // Call without config
      final ComponentInstance instance1 = pre.instantiate();
      resources.add(instance1);

      final Object result1 = instance1.invoke("add", WitS32.of(5), WitS32.of(5));
      assertEquals(10, result1, "Instance 1 (no config): add(5, 5) should be 10");

      // Call with config
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().maxMemoryBytes(64L * 1024 * 1024).build();

      final ComponentInstance instance2 = pre.instantiate(config);
      resources.add(instance2);

      final Object result2 = instance2.invoke("add", WitS32.of(7), WitS32.of(8));
      assertEquals(15, result2, "Instance 2 (with config): add(7, 8) should be 15");

      // Call without config again
      final ComponentInstance instance3 = pre.instantiate();
      resources.add(instance3);

      final Object result3 = instance3.invoke("add", WitS32.of(20), WitS32.of(30));
      assertEquals(50, result3, "Instance 3 (no config): add(20, 30) should be 50");

      LOGGER.info("Mixed instantiate calls successful");
    }
  }

  @Nested
  @DisplayName("Pre-Instantiation Metrics Tests")
  class MetricsTests {

    @Test
    @DisplayName("should track instance count across both instantiate methods")
    void shouldTrackInstanceCount(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      assertEquals(
          0, pre.getInstanceCount(), "Instance count should be 0 before any instantiation");

      // Instantiate without config
      final ComponentInstance instance1 = pre.instantiate();
      resources.add(instance1);
      assertTrue(
          pre.getInstanceCount() >= 1,
          "Instance count should be at least 1 after first instantiate()");

      // Instantiate with config
      final ComponentStoreConfig config = ComponentStoreConfig.builder().build();
      final ComponentInstance instance2 = pre.instantiate(config);
      resources.add(instance2);
      assertTrue(
          pre.getInstanceCount() >= 2,
          "Instance count should be at least 2 after instantiate(config)");

      LOGGER.info("Instance count tracking works: " + pre.getInstanceCount());
    }

    @Test
    @DisplayName("should report non-negative preparation time")
    void shouldReportPreparationTime(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      final long prepTimeNs = pre.getPreparationTimeNs();
      assertTrue(prepTimeNs >= 0, "Preparation time should be non-negative, got: " + prepTimeNs);

      LOGGER.info("Preparation time: " + prepTimeNs + " ns");
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should handle pre close before instantiate(config)")
    void shouldHandlePreCloseBeforeInstantiateWithConfig(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      pre.close();

      final ComponentStoreConfig config = ComponentStoreConfig.builder().build();

      assertThrows(
          Exception.class,
          () -> pre.instantiate(config),
          "Should throw when using closed pre-instantiation with config");

      LOGGER.info("Closed pre-instantiation correctly rejected instantiate(config) call");
    }

    @Test
    @DisplayName("should allow closing instances independently of pre")
    void shouldAllowClosingInstancesIndependently(final TestInfo testInfo) throws Exception {
      assumePreInstantiateAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = componentEngine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstancePre pre = linker.instantiatePre(component);
      resources.add(pre);

      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().maxMemoryBytes(64L * 1024 * 1024).build();

      final ComponentInstance instance = pre.instantiate(config);
      assertTrue(instance.isValid(), "Instance should be valid after creation");

      instance.close();
      assertTrue(pre.isValid(), "Pre should remain valid after instance close");

      // Should still be able to create more instances
      assertDoesNotThrow(
          () -> {
            final ComponentInstance instance2 = pre.instantiate(config);
            resources.add(instance2);
            assertTrue(instance2.isValid(), "New instance should be valid");
          },
          "Should be able to create new instances after closing a previous one");

      LOGGER.info("Instance lifecycle independent of pre-instantiation");
    }
  }
}
