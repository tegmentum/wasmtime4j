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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the InstanceManager interface.
 *
 * <p>InstanceManager provides production-ready instance management with pooling, scaling, and
 * health monitoring. This test verifies the interface structure, nested interfaces, and API
 * conformance.
 */
@DisplayName("InstanceManager Interface Tests")
class InstanceManagerTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(InstanceManager.class.isInterface(), "InstanceManager should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(InstanceManager.class.getModifiers()),
          "InstanceManager should be public");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(InstanceManager.class),
          "InstanceManager should extend AutoCloseable");
    }
  }

  // ========================================================================
  // Instance Pool Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Pool Method Tests")
  class InstancePoolMethodTests {

    @Test
    @DisplayName("should have getInstance method with module parameter")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getInstance", Module.class);
      assertNotNull(method, "getInstance(Module) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "getInstance should return Instance");
    }

    @Test
    @DisplayName("should have getInstance method with module and linker")
    void shouldHaveGetInstanceWithLinkerMethod() throws NoSuchMethodException {
      final Method method =
          InstanceManager.class.getMethod("getInstance", Module.class, Linker.class);
      assertNotNull(method, "getInstance(Module, Linker) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "getInstance should return Instance");
    }

    @Test
    @DisplayName("should have getInstanceAsync method")
    void shouldHaveGetInstanceAsyncMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getInstanceAsync", Module.class);
      assertNotNull(method, "getInstanceAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "getInstanceAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("should have returnInstance method")
    void shouldHaveReturnInstanceMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("returnInstance", Instance.class);
      assertNotNull(method, "returnInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "returnInstance should return void");
    }

    @Test
    @DisplayName("should have createPool method")
    void shouldHaveCreatePoolMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("createPool", Module.class, int.class);
      assertNotNull(method, "createPool method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "createPool should return CompletableFuture");
    }

    @Test
    @DisplayName("should have destroyPool method")
    void shouldHaveDestroyPoolMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("destroyPool", Module.class);
      assertNotNull(method, "destroyPool method should exist");
      assertEquals(int.class, method.getReturnType(), "destroyPool should return int");
    }
  }

  // ========================================================================
  // Statistics Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getPoolStatistics method")
    void shouldHaveGetPoolStatisticsMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getPoolStatistics");
      assertNotNull(method, "getPoolStatistics method should exist");
      assertEquals(
          InstanceManager.InstancePoolStatistics.class,
          method.getReturnType(),
          "getPoolStatistics should return InstancePoolStatistics");
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getPerformanceMetrics");
      assertNotNull(method, "getPerformanceMetrics method should exist");
      assertEquals(
          InstanceManager.InstancePerformanceMetrics.class,
          method.getReturnType(),
          "getPerformanceMetrics should return InstancePerformanceMetrics");
    }
  }

  // ========================================================================
  // Health Monitoring Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Health Monitoring Method Tests")
  class HealthMonitoringMethodTests {

    @Test
    @DisplayName("should have getInstanceHealth method")
    void shouldHaveGetInstanceHealthMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getInstanceHealth");
      assertNotNull(method, "getInstanceHealth method should exist");
      assertEquals(List.class, method.getReturnType(), "getInstanceHealth should return List");
    }

    @Test
    @DisplayName("should have performHealthCheck method")
    void shouldHavePerformHealthCheckMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("performHealthCheck");
      assertNotNull(method, "performHealthCheck method should exist");
      assertEquals(int.class, method.getReturnType(), "performHealthCheck should return int");
    }
  }

  // ========================================================================
  // Scaling Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Scaling Method Tests")
  class ScalingMethodTests {

    @Test
    @DisplayName("should have setAutoScalingEnabled method")
    void shouldHaveSetAutoScalingEnabledMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("setAutoScalingEnabled", boolean.class);
      assertNotNull(method, "setAutoScalingEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "setAutoScalingEnabled should return void");
    }

    @Test
    @DisplayName("should have isAutoScalingEnabled method")
    void shouldHaveIsAutoScalingEnabledMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("isAutoScalingEnabled");
      assertNotNull(method, "isAutoScalingEnabled method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isAutoScalingEnabled should return boolean");
    }

    @Test
    @DisplayName("should have scalePool method")
    void shouldHaveScalePoolMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("scalePool", Module.class, int.class);
      assertNotNull(method, "scalePool method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "scalePool should return CompletableFuture");
    }

    @Test
    @DisplayName("should have balanceLoad method")
    void shouldHaveBalanceLoadMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("balanceLoad");
      assertNotNull(method, "balanceLoad method should exist");
      assertEquals(int.class, method.getReturnType(), "balanceLoad should return int");
    }
  }

  // ========================================================================
  // Checkpoint Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Checkpoint Method Tests")
  class CheckpointMethodTests {

    @Test
    @DisplayName("should have createCheckpoint method")
    void shouldHaveCreateCheckpointMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("createCheckpoint", Instance.class);
      assertNotNull(method, "createCheckpoint method should exist");
      assertEquals(
          InstanceManager.InstanceCheckpoint.class,
          method.getReturnType(),
          "createCheckpoint should return InstanceCheckpoint");
    }

    @Test
    @DisplayName("should have restoreFromCheckpoint method")
    void shouldHaveRestoreFromCheckpointMethod() throws NoSuchMethodException {
      final Method method =
          InstanceManager.class.getMethod(
              "restoreFromCheckpoint", InstanceManager.InstanceCheckpoint.class);
      assertNotNull(method, "restoreFromCheckpoint method should exist");
      assertEquals(
          Instance.class, method.getReturnType(), "restoreFromCheckpoint should return Instance");
    }
  }

  // ========================================================================
  // Configuration Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.class,
          method.getReturnType(),
          "getConfig should return InstanceManagerConfig");
    }

    @Test
    @DisplayName("should have updateConfig method")
    void shouldHaveUpdateConfigMethod() throws NoSuchMethodException {
      final Method method =
          InstanceManager.class.getMethod(
              "updateConfig", InstanceManager.InstanceManagerConfig.class);
      assertNotNull(method, "updateConfig method should exist");
      assertEquals(void.class, method.getReturnType(), "updateConfig should return void");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have shutdown method with duration")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("shutdown", Duration.class);
      assertNotNull(method, "shutdown method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "shutdown should return CompletableFuture");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }

    @Test
    @DisplayName("should have performMaintenance method")
    void shouldHavePerformMaintenanceMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.class.getMethod("performMaintenance");
      assertNotNull(method, "performMaintenance method should exist");
      assertEquals(
          InstanceManager.MaintenanceSummary.class,
          method.getReturnType(),
          "performMaintenance should return MaintenanceSummary");
    }
  }

  // ========================================================================
  // Export Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Export Method Tests")
  class ExportMethodTests {

    @Test
    @DisplayName("should have exportState method")
    void shouldHaveExportStateMethod() throws NoSuchMethodException {
      final Method method =
          InstanceManager.class.getMethod("exportState", InstanceManager.ExportFormat.class);
      assertNotNull(method, "exportState method should exist");
      assertEquals(String.class, method.getReturnType(), "exportState should return String");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have InstanceManagerConfig nested interface")
    void shouldHaveInstanceManagerConfigInterface() {
      Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("InstanceManagerConfig") && c.isInterface());
      assertTrue(found, "InstanceManagerConfig should be a nested interface");
    }

    @Test
    @DisplayName("should have InstancePoolStatistics nested interface")
    void shouldHaveInstancePoolStatisticsInterface() {
      Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("InstancePoolStatistics") && c.isInterface());
      assertTrue(found, "InstancePoolStatistics should be a nested interface");
    }

    @Test
    @DisplayName("should have InstanceHealthStatus nested interface")
    void shouldHaveInstanceHealthStatusInterface() {
      Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("InstanceHealthStatus") && c.isInterface());
      assertTrue(found, "InstanceHealthStatus should be a nested interface");
    }

    @Test
    @DisplayName("should have InstancePerformanceMetrics nested interface")
    void shouldHaveInstancePerformanceMetricsInterface() {
      Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(
                  c -> c.getSimpleName().equals("InstancePerformanceMetrics") && c.isInterface());
      assertTrue(found, "InstancePerformanceMetrics should be a nested interface");
    }

    @Test
    @DisplayName("should have ModulePoolStatistics nested interface")
    void shouldHaveModulePoolStatisticsInterface() {
      Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("ModulePoolStatistics") && c.isInterface());
      assertTrue(found, "ModulePoolStatistics should be a nested interface");
    }

    @Test
    @DisplayName("should have InstanceCheckpoint nested interface")
    void shouldHaveInstanceCheckpointInterface() {
      Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("InstanceCheckpoint") && c.isInterface());
      assertTrue(found, "InstanceCheckpoint should be a nested interface");
    }

    @Test
    @DisplayName("should have MaintenanceSummary nested interface")
    void shouldHaveMaintenanceSummaryInterface() {
      Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("MaintenanceSummary") && c.isInterface());
      assertTrue(found, "MaintenanceSummary should be a nested interface");
    }

    @Test
    @DisplayName("should have ExportFormat nested enum")
    void shouldHaveExportFormatEnum() {
      Class<?>[] declaredClasses = InstanceManager.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("ExportFormat") && c.isEnum());
      assertTrue(found, "ExportFormat should be a nested enum");
    }
  }

  // ========================================================================
  // ExportFormat Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExportFormat Enum Tests")
  class ExportFormatEnumTests {

    @Test
    @DisplayName("ExportFormat should have JSON value")
    void exportFormatShouldHaveJsonValue() {
      InstanceManager.ExportFormat json = InstanceManager.ExportFormat.JSON;
      assertNotNull(json, "JSON value should exist");
    }

    @Test
    @DisplayName("ExportFormat should have XML value")
    void exportFormatShouldHaveXmlValue() {
      InstanceManager.ExportFormat xml = InstanceManager.ExportFormat.XML;
      assertNotNull(xml, "XML value should exist");
    }

    @Test
    @DisplayName("ExportFormat should have CSV value")
    void exportFormatShouldHaveCsvValue() {
      InstanceManager.ExportFormat csv = InstanceManager.ExportFormat.CSV;
      assertNotNull(csv, "CSV value should exist");
    }

    @Test
    @DisplayName("ExportFormat should have YAML value")
    void exportFormatShouldHaveYamlValue() {
      InstanceManager.ExportFormat yaml = InstanceManager.ExportFormat.YAML;
      assertNotNull(yaml, "YAML value should exist");
    }

    @Test
    @DisplayName("ExportFormat should have exactly 4 values")
    void exportFormatShouldHaveExactlyFourValues() {
      assertEquals(
          4,
          InstanceManager.ExportFormat.values().length,
          "ExportFormat should have exactly 4 values");
    }
  }

  // ========================================================================
  // InstanceManagerConfig Tests
  // ========================================================================

  @Nested
  @DisplayName("InstanceManagerConfig Tests")
  class InstanceManagerConfigTests {

    @Test
    @DisplayName("InstanceManagerConfig should have getDefaultPoolSize method")
    void shouldHaveGetDefaultPoolSizeMethod() throws NoSuchMethodException {
      final Method method =
          InstanceManager.InstanceManagerConfig.class.getMethod("getDefaultPoolSize");
      assertNotNull(method, "getDefaultPoolSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getDefaultPoolSize should return int");
    }

    @Test
    @DisplayName("InstanceManagerConfig should have getMaxPoolSize method")
    void shouldHaveGetMaxPoolSizeMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.InstanceManagerConfig.class.getMethod("getMaxPoolSize");
      assertNotNull(method, "getMaxPoolSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getMaxPoolSize should return int");
    }

    @Test
    @DisplayName("InstanceManagerConfig should have isAutoScalingEnabled method")
    void shouldHaveIsAutoScalingEnabledMethod() throws NoSuchMethodException {
      final Method method =
          InstanceManager.InstanceManagerConfig.class.getMethod("isAutoScalingEnabled");
      assertNotNull(method, "isAutoScalingEnabled method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isAutoScalingEnabled should return boolean");
    }

    @Test
    @DisplayName("InstanceManagerConfig should have builder static method")
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.InstanceManagerConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("InstanceManagerConfig should have defaultConfig static method")
    void shouldHaveDefaultConfigStaticMethod() throws NoSuchMethodException {
      final Method method = InstanceManager.InstanceManagerConfig.class.getMethod("defaultConfig");
      assertNotNull(method, "defaultConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaultConfig should be static");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected core methods")
    void shouldHaveAllCoreCoreMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getInstance",
              "getInstanceAsync",
              "returnInstance",
              "createPool",
              "destroyPool",
              "getPoolStatistics",
              "getInstanceHealth",
              "performHealthCheck",
              "setAutoScalingEnabled",
              "isAutoScalingEnabled",
              "scalePool",
              "balanceLoad",
              "createCheckpoint",
              "restoreFromCheckpoint",
              "getConfig",
              "updateConfig",
              "getPerformanceMetrics",
              "exportState",
              "performMaintenance",
              "shutdown",
              "close");

      Set<String> actualMethods =
          Arrays.stream(InstanceManager.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "InstanceManager should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have all expected nested types")
    void shouldHaveAllNestedTypes() {
      Set<String> expectedTypes =
          Set.of(
              "InstanceManagerConfig",
              "InstancePoolStatistics",
              "InstanceHealthStatus",
              "InstancePerformanceMetrics",
              "ModulePoolStatistics",
              "InstanceCheckpoint",
              "MaintenanceSummary",
              "ExportFormat");

      Set<String> actualTypes =
          Arrays.stream(InstanceManager.class.getDeclaredClasses())
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      for (String expected : expectedTypes) {
        assertTrue(
            actualTypes.contains(expected), "InstanceManager should have nested type: " + expected);
      }
    }
  }
}
