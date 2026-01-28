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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InstanceManager} interface.
 *
 * <p>InstanceManager provides production-ready instance management with pooling, scaling, and
 * health monitoring for WebAssembly instances.
 */
@DisplayName("InstanceManager Interface Tests")
class InstanceManagerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(InstanceManager.class.isInterface(), "InstanceManager should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      final Class<?>[] interfaces = InstanceManager.class.getInterfaces();
      boolean extendsAutoCloseable = false;
      for (final Class<?> iface : interfaces) {
        if (iface == AutoCloseable.class) {
          extendsAutoCloseable = true;
          break;
        }
      }
      assertTrue(extendsAutoCloseable, "Should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static create method with Engine parameter")
    void shouldHaveStaticCreateMethodWithEngine() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("create", Engine.class);
      assertNotNull(method, "create(Engine) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(InstanceManager.class, method.getReturnType(), "Should return InstanceManager");
    }

    @Test
    @DisplayName("should have static create method with Engine and config parameters")
    void shouldHaveStaticCreateMethodWithEngineAndConfig() throws NoSuchMethodException {
      final Method method =
          InstanceManager.class.getMethod(
              "create", Engine.class, InstanceManager.InstanceManagerConfig.class);
      assertNotNull(method, "create(Engine, Config) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(InstanceManager.class, method.getReturnType(), "Should return InstanceManager");
    }
  }

  @Nested
  @DisplayName("Instance Retrieval Method Tests")
  class InstanceRetrievalMethodTests {

    @Test
    @DisplayName("should have getInstance method with Module parameter")
    void shouldHaveGetInstanceMethodWithModule() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getInstance", Module.class);
      assertNotNull(method, "getInstance(Module) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("should have getInstance method with Module and Linker parameters")
    void shouldHaveGetInstanceMethodWithModuleAndLinker() throws NoSuchMethodException {
      final Method method =
          InstanceManager.class.getMethod("getInstance", Module.class, Linker.class);
      assertNotNull(method, "getInstance(Module, Linker) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("should have getInstanceAsync method")
    void shouldHaveGetInstanceAsyncMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getInstanceAsync", Module.class);
      assertNotNull(method, "getInstanceAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have returnInstance method")
    void shouldHaveReturnInstanceMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("returnInstance", Instance.class);
      assertNotNull(method, "returnInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Pool Management Method Tests")
  class PoolManagementMethodTests {

    @Test
    @DisplayName("should have createPool method")
    void shouldHaveCreatePoolMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("createPool", Module.class, int.class);
      assertNotNull(method, "createPool method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have destroyPool method")
    void shouldHaveDestroyPoolMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("destroyPool", Module.class);
      assertNotNull(method, "destroyPool method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPoolStatistics method")
    void shouldHaveGetPoolStatisticsMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getPoolStatistics");
      assertNotNull(method, "getPoolStatistics method should exist");
    }
  }

  @Nested
  @DisplayName("Health Monitoring Method Tests")
  class HealthMonitoringMethodTests {

    @Test
    @DisplayName("should have getInstanceHealth method")
    void shouldHaveGetInstanceHealthMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getInstanceHealth");
      assertNotNull(method, "getInstanceHealth method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have performHealthCheck method")
    void shouldHavePerformHealthCheckMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("performHealthCheck");
      assertNotNull(method, "performHealthCheck method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Scaling Method Tests")
  class ScalingMethodTests {

    @Test
    @DisplayName("should have setAutoScalingEnabled method")
    void shouldHaveSetAutoScalingEnabledMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("setAutoScalingEnabled", boolean.class);
      assertNotNull(method, "setAutoScalingEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isAutoScalingEnabled method")
    void shouldHaveIsAutoScalingEnabledMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("isAutoScalingEnabled");
      assertNotNull(method, "isAutoScalingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have scalePool method")
    void shouldHaveScalePoolMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("scalePool", Module.class, int.class);
      assertNotNull(method, "scalePool method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have balanceLoad method")
    void shouldHaveBalanceLoadMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("balanceLoad");
      assertNotNull(method, "balanceLoad method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Checkpoint Method Tests")
  class CheckpointMethodTests {

    @Test
    @DisplayName("should have createCheckpoint method")
    void shouldHaveCreateCheckpointMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("createCheckpoint", Instance.class);
      assertNotNull(method, "createCheckpoint method should exist");
    }

    @Test
    @DisplayName("should have restoreFromCheckpoint method")
    void shouldHaveRestoreFromCheckpointMethod() throws NoSuchMethodException {
      final Method method =
          InstanceManager.class.getMethod(
              "restoreFromCheckpoint", InstanceManager.InstanceCheckpoint.class);
      assertNotNull(method, "restoreFromCheckpoint method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
    }

    @Test
    @DisplayName("should have updateConfig method")
    void shouldHaveUpdateConfigMethod() throws NoSuchMethodException {
      final Method method =
          InstanceManager.class.getMethod(
              "updateConfig", InstanceManager.InstanceManagerConfig.class);
      assertNotNull(method, "updateConfig method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Metrics and Export Method Tests")
  class MetricsAndExportMethodTests {

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getPerformanceMetrics");
      assertNotNull(method, "getPerformanceMetrics method should exist");
    }

    @Test
    @DisplayName("should have exportState method")
    void shouldHaveExportStateMethod() throws NoSuchMethodException {
      final Method method =
          InstanceManager.class.getMethod("exportState", InstanceManager.ExportFormat.class);
      assertNotNull(method, "exportState method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Maintenance and Shutdown Method Tests")
  class MaintenanceAndShutdownMethodTests {

    @Test
    @DisplayName("should have performMaintenance method")
    void shouldHavePerformMaintenanceMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("performMaintenance");
      assertNotNull(method, "performMaintenance method should exist");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("shutdown", Duration.class);
      assertNotNull(method, "shutdown method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have InstanceManagerConfig nested interface")
    void shouldHaveInstanceManagerConfigNestedInterface() {
      final Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("InstanceManagerConfig")) {
          found = true;
          assertTrue(clazz.isInterface(), "InstanceManagerConfig should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have InstanceManagerConfig nested interface");
    }

    @Test
    @DisplayName("should have InstancePoolStatistics nested interface")
    void shouldHaveInstancePoolStatisticsNestedInterface() {
      final Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("InstancePoolStatistics")) {
          found = true;
          assertTrue(clazz.isInterface(), "InstancePoolStatistics should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have InstancePoolStatistics nested interface");
    }

    @Test
    @DisplayName("should have InstanceHealthStatus nested interface")
    void shouldHaveInstanceHealthStatusNestedInterface() {
      final Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("InstanceHealthStatus")) {
          found = true;
          assertTrue(clazz.isInterface(), "InstanceHealthStatus should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have InstanceHealthStatus nested interface");
    }

    @Test
    @DisplayName("should have InstanceCheckpoint nested interface")
    void shouldHaveInstanceCheckpointNestedInterface() {
      final Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("InstanceCheckpoint")) {
          found = true;
          assertTrue(clazz.isInterface(), "InstanceCheckpoint should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have InstanceCheckpoint nested interface");
    }

    @Test
    @DisplayName("should have InstancePerformanceMetrics nested interface")
    void shouldHaveInstancePerformanceMetricsNestedInterface() {
      final Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("InstancePerformanceMetrics")) {
          found = true;
          assertTrue(clazz.isInterface(), "InstancePerformanceMetrics should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have InstancePerformanceMetrics nested interface");
    }

    @Test
    @DisplayName("should have MaintenanceSummary nested interface")
    void shouldHaveMaintenanceSummaryNestedInterface() {
      final Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("MaintenanceSummary")) {
          found = true;
          assertTrue(clazz.isInterface(), "MaintenanceSummary should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have MaintenanceSummary nested interface");
    }
  }

  @Nested
  @DisplayName("Nested Enum Tests")
  class NestedEnumTests {

    @Test
    @DisplayName("should have ExportFormat enum")
    void shouldHaveExportFormatEnum() {
      final Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ExportFormat")) {
          found = true;
          assertTrue(clazz.isEnum(), "ExportFormat should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have ExportFormat enum");
    }

    @Test
    @DisplayName("ExportFormat enum should have expected values")
    void exportFormatEnumShouldHaveExpectedValues() {
      final InstanceManager.ExportFormat[] values = InstanceManager.ExportFormat.values();
      final Set<String> valueNames = new HashSet<>();
      for (final InstanceManager.ExportFormat value : values) {
        valueNames.add(value.name());
      }

      assertTrue(valueNames.contains("JSON"), "Should have JSON");
      assertTrue(valueNames.contains("XML"), "Should have XML");
      assertTrue(valueNames.contains("CSV"), "Should have CSV");
      assertTrue(valueNames.contains("YAML"), "Should have YAML");
    }
  }

  @Nested
  @DisplayName("InstanceManagerConfig Builder Tests")
  class InstanceManagerConfigBuilderTests {

    @Test
    @DisplayName("InstanceManagerConfig should have builder method")
    void instanceManagerConfigShouldHaveBuilderMethod() throws NoSuchMethodException {
      final Class<?> configClass = InstanceManager.InstanceManagerConfig.class;
      final Method method = configClass.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("InstanceManagerConfig should have defaultConfig method")
    void instanceManagerConfigShouldHaveDefaultConfigMethod() throws NoSuchMethodException {
      final Class<?> configClass = InstanceManager.InstanceManagerConfig.class;
      final Method method = configClass.getMethod("defaultConfig");
      assertNotNull(method, "defaultConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaultConfig should be static");
    }

    @Test
    @DisplayName("InstanceManagerConfig should have getDefaultPoolSize method")
    void instanceManagerConfigShouldHaveGetDefaultPoolSizeMethod() throws NoSuchMethodException {
      final Class<?> configClass = InstanceManager.InstanceManagerConfig.class;
      final Method method = configClass.getMethod("getDefaultPoolSize");
      assertNotNull(method, "getDefaultPoolSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("InstanceManagerConfig should have isAutoScalingEnabled method")
    void instanceManagerConfigShouldHaveIsAutoScalingEnabledMethod() throws NoSuchMethodException {
      final Class<?> configClass = InstanceManager.InstanceManagerConfig.class;
      final Method method = configClass.getMethod("isAutoScalingEnabled");
      assertNotNull(method, "isAutoScalingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("InstanceManagerConfig should have isHealthMonitoringEnabled method")
    void instanceManagerConfigShouldHaveIsHealthMonitoringEnabledMethod()
        throws NoSuchMethodException {
      final Class<?> configClass = InstanceManager.InstanceManagerConfig.class;
      final Method method = configClass.getMethod("isHealthMonitoringEnabled");
      assertNotNull(method, "isHealthMonitoringEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }
}
