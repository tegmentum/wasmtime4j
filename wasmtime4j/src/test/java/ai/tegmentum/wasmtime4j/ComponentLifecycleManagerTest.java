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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager.ComponentHealthCheckResult;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager.ComponentLifecycleStatistics;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager.ComponentRestartConfig;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager.ComponentRestartPolicy;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager.ComponentShutdownConfig;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager.ComponentStartupConfig;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager.LifecycleManagerShutdownConfig;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager.OverallLifecycleStatistics;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleState;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentLifecycleManager} interface.
 *
 * <p>ComponentLifecycleManager provides comprehensive lifecycle management for WebAssembly
 * components including start, stop, restart, pause, and resume operations.
 */
@DisplayName("ComponentLifecycleManager Tests")
class ComponentLifecycleManagerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentLifecycleManager.class.getModifiers()),
          "ComponentLifecycleManager should be public");
      assertTrue(
          ComponentLifecycleManager.class.isInterface(),
          "ComponentLifecycleManager should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have startComponent method with componentId")
    void shouldHaveStartComponentMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("startComponent", String.class);
      assertNotNull(method, "startComponent(String) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have startComponent method with config")
    void shouldHaveStartComponentMethodWithConfig() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod(
              "startComponent", String.class, ComponentStartupConfig.class);
      assertNotNull(method, "startComponent(String, Config) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have startComponents method")
    void shouldHaveStartComponentsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("startComponents", List.class);
      assertNotNull(method, "startComponents method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have stopComponent method with force flag")
    void shouldHaveStopComponentMethodWithForce() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("stopComponent", String.class, boolean.class);
      assertNotNull(method, "stopComponent(String, boolean) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stopComponent method with config")
    void shouldHaveStopComponentMethodWithConfig() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod(
              "stopComponent", String.class, ComponentShutdownConfig.class);
      assertNotNull(method, "stopComponent(String, Config) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stopComponents method")
    void shouldHaveStopComponentsMethod() throws NoSuchMethodException {
      final Method method = ComponentLifecycleManager.class.getMethod("stopComponents", List.class);
      assertNotNull(method, "stopComponents method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have restartComponent method")
    void shouldHaveRestartComponentMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("restartComponent", String.class);
      assertNotNull(method, "restartComponent(String) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have restartComponent method with config")
    void shouldHaveRestartComponentMethodWithConfig() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod(
              "restartComponent", String.class, ComponentRestartConfig.class);
      assertNotNull(method, "restartComponent(String, Config) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have pauseComponent method")
    void shouldHavePauseComponentMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("pauseComponent", String.class);
      assertNotNull(method, "pauseComponent method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have resumeComponent method")
    void shouldHaveResumeComponentMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("resumeComponent", String.class);
      assertNotNull(method, "resumeComponent method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getComponentState method")
    void shouldHaveGetComponentStateMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("getComponentState", String.class);
      assertNotNull(method, "getComponentState method should exist");
      assertEquals(
          ComponentLifecycleState.class,
          method.getReturnType(),
          "Should return ComponentLifecycleState");
    }

    @Test
    @DisplayName("should have getAllComponentStates method")
    void shouldHaveGetAllComponentStatesMethod() throws NoSuchMethodException {
      final Method method = ComponentLifecycleManager.class.getMethod("getAllComponentStates");
      assertNotNull(method, "getAllComponentStates method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getComponentsInState method")
    void shouldHaveGetComponentsInStateMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod(
              "getComponentsInState", ComponentLifecycleState.class);
      assertNotNull(method, "getComponentsInState method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have isComponentRunning method")
    void shouldHaveIsComponentRunningMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("isComponentRunning", String.class);
      assertNotNull(method, "isComponentRunning method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isComponentHealthy method")
    void shouldHaveIsComponentHealthyMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("isComponentHealthy", String.class);
      assertNotNull(method, "isComponentHealthy method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have performHealthCheck method")
    void shouldHavePerformHealthCheckMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("performHealthCheck", String.class);
      assertNotNull(method, "performHealthCheck method should exist");
      assertEquals(
          ComponentHealthCheckResult.class,
          method.getReturnType(),
          "Should return ComponentHealthCheckResult");
    }

    @Test
    @DisplayName("should have performHealthCheckAll method")
    void shouldHavePerformHealthCheckAllMethod() throws NoSuchMethodException {
      final Method method = ComponentLifecycleManager.class.getMethod("performHealthCheckAll");
      assertNotNull(method, "performHealthCheckAll method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have enableAutoRestart method")
    void shouldHaveEnableAutoRestartMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod(
              "enableAutoRestart", String.class, ComponentRestartPolicy.class);
      assertNotNull(method, "enableAutoRestart method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have disableAutoRestart method")
    void shouldHaveDisableAutoRestartMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("disableAutoRestart", String.class);
      assertNotNull(method, "disableAutoRestart method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getRestartPolicy method")
    void shouldHaveGetRestartPolicyMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("getRestartPolicy", String.class);
      assertNotNull(method, "getRestartPolicy method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getLifecycleStatistics method")
    void shouldHaveGetLifecycleStatisticsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod("getLifecycleStatistics", String.class);
      assertNotNull(method, "getLifecycleStatistics method should exist");
      assertEquals(
          ComponentLifecycleStatistics.class,
          method.getReturnType(),
          "Should return ComponentLifecycleStatistics");
    }

    @Test
    @DisplayName("should have getOverallStatistics method")
    void shouldHaveGetOverallStatisticsMethod() throws NoSuchMethodException {
      final Method method = ComponentLifecycleManager.class.getMethod("getOverallStatistics");
      assertNotNull(method, "getOverallStatistics method should exist");
      assertEquals(
          OverallLifecycleStatistics.class,
          method.getReturnType(),
          "Should return OverallLifecycleStatistics");
    }

    @Test
    @DisplayName("should have shutdown method without parameters")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = ComponentLifecycleManager.class.getMethod("shutdown");
      assertNotNull(method, "shutdown() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have shutdown method with config")
    void shouldHaveShutdownMethodWithConfig() throws NoSuchMethodException {
      final Method method =
          ComponentLifecycleManager.class.getMethod(
              "shutdown", LifecycleManagerShutdownConfig.class);
      assertNotNull(method, "shutdown(Config) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Nested Class Tests")
  class NestedClassTests {

    @Test
    @DisplayName("should have ComponentStartupConfig nested class")
    void shouldHaveComponentStartupConfigNestedClass() {
      Class<?>[] nestedClasses = ComponentLifecycleManager.class.getDeclaredClasses();
      boolean found = false;
      for (Class<?> nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ComponentStartupConfig")) {
          found = true;
          assertTrue(
              Modifier.isFinal(nestedClass.getModifiers()),
              "ComponentStartupConfig should be final");
          break;
        }
      }
      assertTrue(found, "Should have ComponentStartupConfig nested class");
    }

    @Test
    @DisplayName("should have ComponentShutdownConfig nested class")
    void shouldHaveComponentShutdownConfigNestedClass() {
      Class<?>[] nestedClasses = ComponentLifecycleManager.class.getDeclaredClasses();
      boolean found = false;
      for (Class<?> nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ComponentShutdownConfig")) {
          found = true;
          assertTrue(
              Modifier.isFinal(nestedClass.getModifiers()),
              "ComponentShutdownConfig should be final");
          break;
        }
      }
      assertTrue(found, "Should have ComponentShutdownConfig nested class");
    }

    @Test
    @DisplayName("should have ComponentRestartConfig nested class")
    void shouldHaveComponentRestartConfigNestedClass() {
      Class<?>[] nestedClasses = ComponentLifecycleManager.class.getDeclaredClasses();
      boolean found = false;
      for (Class<?> nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ComponentRestartConfig")) {
          found = true;
          assertTrue(
              Modifier.isFinal(nestedClass.getModifiers()),
              "ComponentRestartConfig should be final");
          break;
        }
      }
      assertTrue(found, "Should have ComponentRestartConfig nested class");
    }

    @Test
    @DisplayName("should have ComponentRestartPolicy enum")
    void shouldHaveComponentRestartPolicyEnum() {
      final ComponentRestartPolicy[] policies = ComponentRestartPolicy.values();
      assertEquals(4, policies.length, "Should have 4 restart policies");
      assertEquals(ComponentRestartPolicy.NEVER, ComponentRestartPolicy.valueOf("NEVER"));
      assertEquals(ComponentRestartPolicy.ALWAYS, ComponentRestartPolicy.valueOf("ALWAYS"));
      assertEquals(ComponentRestartPolicy.ON_FAILURE, ComponentRestartPolicy.valueOf("ON_FAILURE"));
      assertEquals(
          ComponentRestartPolicy.UNLESS_STOPPED, ComponentRestartPolicy.valueOf("UNLESS_STOPPED"));
    }
  }

  @Nested
  @DisplayName("ComponentStartupConfig Tests")
  class ComponentStartupConfigTests {

    @Test
    @DisplayName("should create startup config with all parameters")
    void shouldCreateStartupConfigWithAllParameters() {
      final ComponentStartupConfig config = new ComponentStartupConfig(5000L, true, true);

      assertEquals(5000L, config.getTimeoutMillis(), "Timeout should be set");
      assertTrue(config.isValidateDependencies(), "Validate dependencies should be true");
      assertTrue(config.isWaitForDependencies(), "Wait for dependencies should be true");
    }

    @Test
    @DisplayName("should create startup config with false flags")
    void shouldCreateStartupConfigWithFalseFlags() {
      final ComponentStartupConfig config = new ComponentStartupConfig(1000L, false, false);

      assertEquals(1000L, config.getTimeoutMillis(), "Timeout should be set");
      assertFalse(config.isValidateDependencies(), "Validate dependencies should be false");
      assertFalse(config.isWaitForDependencies(), "Wait for dependencies should be false");
    }
  }

  @Nested
  @DisplayName("ComponentShutdownConfig Tests")
  class ComponentShutdownConfigTests {

    @Test
    @DisplayName("should create shutdown config with all parameters")
    void shouldCreateShutdownConfigWithAllParameters() {
      final ComponentShutdownConfig config = new ComponentShutdownConfig(3000L, true, false);

      assertEquals(3000L, config.getTimeoutMillis(), "Timeout should be set");
      assertTrue(config.isGraceful(), "Graceful should be true");
      assertFalse(config.isForce(), "Force should be false");
    }
  }

  @Nested
  @DisplayName("ComponentRestartConfig Tests")
  class ComponentRestartConfigTests {

    @Test
    @DisplayName("should create restart config with nested configs")
    void shouldCreateRestartConfigWithNestedConfigs() {
      final ComponentShutdownConfig shutdownConfig =
          new ComponentShutdownConfig(3000L, true, false);
      final ComponentStartupConfig startupConfig = new ComponentStartupConfig(5000L, true, true);
      final ComponentRestartConfig config =
          new ComponentRestartConfig(shutdownConfig, startupConfig, 1000L);

      assertEquals(shutdownConfig, config.getShutdownConfig(), "Shutdown config should match");
      assertEquals(startupConfig, config.getStartupConfig(), "Startup config should match");
      assertEquals(1000L, config.getDelayBetweenMillis(), "Delay should be set");
    }
  }

  @Nested
  @DisplayName("ComponentHealthCheckResult Tests")
  class ComponentHealthCheckResultTests {

    @Test
    @DisplayName("should create healthy result")
    void shouldCreateHealthyResult() {
      final Instant now = Instant.now();
      final ComponentHealthCheckResult result =
          new ComponentHealthCheckResult(true, "All good", now, Map.of("key", "value"));

      assertTrue(result.isHealthy(), "Should be healthy");
      assertEquals("All good", result.getMessage(), "Message should match");
      assertEquals(now, result.getTimestamp(), "Timestamp should match");
      assertNotNull(result.getDetails(), "Details should not be null");
    }

    @Test
    @DisplayName("should create unhealthy result")
    void shouldCreateUnhealthyResult() {
      final Instant now = Instant.now();
      final ComponentHealthCheckResult result =
          new ComponentHealthCheckResult(false, "Error occurred", now, null);

      assertFalse(result.isHealthy(), "Should not be healthy");
      assertEquals("Error occurred", result.getMessage(), "Message should match");
    }
  }

  @Nested
  @DisplayName("ComponentLifecycleStatistics Tests")
  class ComponentLifecycleStatisticsTests {

    @Test
    @DisplayName("should create lifecycle statistics with all fields")
    void shouldCreateLifecycleStatisticsWithAllFields() {
      final Instant start = Instant.now();
      final Instant stop = Instant.now();
      final ComponentLifecycleStatistics stats =
          new ComponentLifecycleStatistics("comp-1", 5, 3, 2, 1, start, stop, 100000L);

      assertEquals("comp-1", stats.getComponentId(), "Component ID should match");
      assertEquals(5, stats.getStartCount(), "Start count should match");
      assertEquals(3, stats.getStopCount(), "Stop count should match");
      assertEquals(2, stats.getRestartCount(), "Restart count should match");
      assertEquals(1, stats.getFailureCount(), "Failure count should match");
      assertEquals(start, stats.getLastStartTime(), "Last start time should match");
      assertEquals(stop, stats.getLastStopTime(), "Last stop time should match");
      assertEquals(100000L, stats.getTotalUptime(), "Total uptime should match");
    }
  }

  @Nested
  @DisplayName("OverallLifecycleStatistics Tests")
  class OverallLifecycleStatisticsTests {

    @Test
    @DisplayName("should create overall statistics with all fields")
    void shouldCreateOverallStatisticsWithAllFields() {
      final OverallLifecycleStatistics stats =
          new OverallLifecycleStatistics(10, 8, 2, 500000L, 15);

      assertEquals(10, stats.getTotalComponents(), "Total components should match");
      assertEquals(8, stats.getRunningComponents(), "Running components should match");
      assertEquals(2, stats.getFailedComponents(), "Failed components should match");
      assertEquals(500000L, stats.getTotalUptime(), "Total uptime should match");
      assertEquals(15, stats.getTotalRestarts(), "Total restarts should match");
    }
  }

  @Nested
  @DisplayName("LifecycleManagerShutdownConfig Tests")
  class LifecycleManagerShutdownConfigTests {

    @Test
    @DisplayName("should create manager shutdown config")
    void shouldCreateManagerShutdownConfig() {
      final LifecycleManagerShutdownConfig config =
          new LifecycleManagerShutdownConfig(30000L, true, false);

      assertEquals(30000L, config.getTimeoutMillis(), "Timeout should match");
      assertTrue(config.isGraceful(), "Graceful should be true");
      assertFalse(config.isForceStopComponents(), "Force stop should be false");
    }
  }
}
