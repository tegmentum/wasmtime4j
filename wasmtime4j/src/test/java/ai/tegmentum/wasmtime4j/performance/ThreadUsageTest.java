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

package ai.tegmentum.wasmtime4j.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ThreadUsage} class.
 *
 * <p>ThreadUsage provides detailed thread usage information for monitoring.
 */
@DisplayName("ThreadUsage Tests")
class ThreadUsageTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(ThreadUsage.class.getModifiers()), "ThreadUsage should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ThreadUsage.class.getModifiers()), "ThreadUsage should be public");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          ThreadUsage.class.getConstructor(
              int.class, int.class, int.class, Map.class, Duration.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getActiveThreads method")
    void shouldHaveGetActiveThreadsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getActiveThreads");
      assertNotNull(method, "getActiveThreads method should exist");
      assertEquals(int.class, method.getReturnType(), "getActiveThreads should return int");
    }

    @Test
    @DisplayName("should have getPeakThreads method")
    void shouldHaveGetPeakThreadsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getPeakThreads");
      assertNotNull(method, "getPeakThreads method should exist");
      assertEquals(int.class, method.getReturnType(), "getPeakThreads should return int");
    }

    @Test
    @DisplayName("should have getTotalStartedThreads method")
    void shouldHaveGetTotalStartedThreadsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getTotalStartedThreads");
      assertNotNull(method, "getTotalStartedThreads method should exist");
      assertEquals(int.class, method.getReturnType(), "getTotalStartedThreads should return int");
    }

    @Test
    @DisplayName("should have getThreadsByState method")
    void shouldHaveGetThreadsByStateMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getThreadsByState");
      assertNotNull(method, "getThreadsByState method should exist");
      assertEquals(Map.class, method.getReturnType(), "getThreadsByState should return Map");
    }

    @Test
    @DisplayName("should have getAverageThreadLifetime method")
    void shouldHaveGetAverageThreadLifetimeMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getAverageThreadLifetime");
      assertNotNull(method, "getAverageThreadLifetime method should exist");
      assertEquals(
          Duration.class,
          method.getReturnType(),
          "getAverageThreadLifetime should return Duration");
    }
  }

  @Nested
  @DisplayName("State Query Method Tests")
  class StateQueryMethodTests {

    @Test
    @DisplayName("should have getThreadsInState method")
    void shouldHaveGetThreadsInStateMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getThreadsInState", String.class);
      assertNotNull(method, "getThreadsInState method should exist");
      assertEquals(int.class, method.getReturnType(), "getThreadsInState should return int");
    }

    @Test
    @DisplayName("should have getRunnableThreads method")
    void shouldHaveGetRunnableThreadsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getRunnableThreads");
      assertNotNull(method, "getRunnableThreads method should exist");
      assertEquals(int.class, method.getReturnType(), "getRunnableThreads should return int");
    }

    @Test
    @DisplayName("should have getBlockedThreads method")
    void shouldHaveGetBlockedThreadsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getBlockedThreads");
      assertNotNull(method, "getBlockedThreads method should exist");
      assertEquals(int.class, method.getReturnType(), "getBlockedThreads should return int");
    }

    @Test
    @DisplayName("should have getWaitingThreads method")
    void shouldHaveGetWaitingThreadsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getWaitingThreads");
      assertNotNull(method, "getWaitingThreads method should exist");
      assertEquals(int.class, method.getReturnType(), "getWaitingThreads should return int");
    }
  }

  @Nested
  @DisplayName("Analysis Method Tests")
  class AnalysisMethodTests {

    @Test
    @DisplayName("should have getThreadUtilization method")
    void shouldHaveGetThreadUtilizationMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getThreadUtilization");
      assertNotNull(method, "getThreadUtilization method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getThreadUtilization should return double");
    }

    @Test
    @DisplayName("should have getThreadEfficiency method")
    void shouldHaveGetThreadEfficiencyMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getThreadEfficiency");
      assertNotNull(method, "getThreadEfficiency method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getThreadEfficiency should return double");
    }

    @Test
    @DisplayName("should have hasThreadContention method")
    void shouldHaveHasThreadContentionMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("hasThreadContention");
      assertNotNull(method, "hasThreadContention method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "hasThreadContention should return boolean");
    }

    @Test
    @DisplayName("should have hasExcessiveThreadCreation method")
    void shouldHaveHasExcessiveThreadCreationMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("hasExcessiveThreadCreation");
      assertNotNull(method, "hasExcessiveThreadCreation method should exist");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "hasExcessiveThreadCreation should return boolean");
    }

    @Test
    @DisplayName("should have isThreadPoolUndersized method")
    void shouldHaveIsThreadPoolUndersizedMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("isThreadPoolUndersized");
      assertNotNull(method, "isThreadPoolUndersized method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isThreadPoolUndersized should return boolean");
    }

    @Test
    @DisplayName("should have getManagementScore method")
    void shouldHaveGetManagementScoreMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getManagementScore");
      assertNotNull(method, "getManagementScore method should exist");
      assertEquals(double.class, method.getReturnType(), "getManagementScore should return double");
    }

    @Test
    @DisplayName("should have getThreadChurnRate method")
    void shouldHaveGetThreadChurnRateMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("getThreadChurnRate");
      assertNotNull(method, "getThreadChurnRate method should exist");
      assertEquals(double.class, method.getReturnType(), "getThreadChurnRate should return double");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = ThreadUsage.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final Map<String, Integer> threadsByState = Map.of("RUNNABLE", 8, "BLOCKED", 2, "WAITING", 5);

      final ThreadUsage threadUsage =
          new ThreadUsage(15, 20, 100, threadsByState, Duration.ofMinutes(5));

      assertEquals(15, threadUsage.getActiveThreads(), "Active threads should match");
      assertEquals(20, threadUsage.getPeakThreads(), "Peak threads should match");
      assertEquals(100, threadUsage.getTotalStartedThreads(), "Total started threads should match");
      assertEquals(
          Duration.ofMinutes(5),
          threadUsage.getAverageThreadLifetime(),
          "Avg lifetime should match");
    }

    @Test
    @DisplayName("should calculate thread utilization correctly")
    void shouldCalculateThreadUtilizationCorrectly() {
      final ThreadUsage threadUsage = new ThreadUsage(10, 20, 100, Map.of(), Duration.ofMinutes(5));

      assertEquals(
          0.5, threadUsage.getThreadUtilization(), 0.001, "Thread utilization should be 50%");
    }

    @Test
    @DisplayName("should get threads in specific state")
    void shouldGetThreadsInSpecificState() {
      final Map<String, Integer> threadsByState = Map.of("RUNNABLE", 8, "BLOCKED", 2);

      final ThreadUsage threadUsage =
          new ThreadUsage(10, 20, 100, threadsByState, Duration.ofMinutes(5));

      assertEquals(8, threadUsage.getThreadsInState("RUNNABLE"), "Runnable threads should match");
      assertEquals(2, threadUsage.getBlockedThreads(), "Blocked threads should match");
      assertEquals(0, threadUsage.getThreadsInState("UNKNOWN"), "Unknown state should return 0");
    }
  }
}
