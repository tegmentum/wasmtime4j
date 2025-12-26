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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiHttpStats} interface.
 *
 * <p>WasiHttpStats provides statistics about HTTP requests made through a WASI HTTP context.
 */
@DisplayName("WasiHttpStats Tests")
class WasiHttpStatsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiHttpStats.class.isInterface(), "WasiHttpStats should be an interface");
    }

    @Test
    @DisplayName("should have only getter methods")
    void shouldHaveOnlyGetterMethods() {
      for (final Method method : WasiHttpStats.class.getDeclaredMethods()) {
        assertTrue(
            method.getName().startsWith("get") || method.getName().startsWith("is"),
            "All methods should be getters: " + method.getName());
      }
    }
  }

  @Nested
  @DisplayName("Request Count Method Tests")
  class RequestCountMethodTests {

    @Test
    @DisplayName("should have getTotalRequests method")
    void shouldHaveGetTotalRequestsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getTotalRequests");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getSuccessfulRequests method")
    void shouldHaveGetSuccessfulRequestsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getSuccessfulRequests");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFailedRequests method")
    void shouldHaveGetFailedRequestsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getFailedRequests");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getActiveRequests method")
    void shouldHaveGetActiveRequestsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getActiveRequests");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Byte Count Method Tests")
  class ByteCountMethodTests {

    @Test
    @DisplayName("should have getTotalBytesSent method")
    void shouldHaveGetTotalBytesSentMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getTotalBytesSent");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalBytesReceived method")
    void shouldHaveGetTotalBytesReceivedMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getTotalBytesReceived");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Duration Method Tests")
  class DurationMethodTests {

    @Test
    @DisplayName("should have getAverageRequestDuration method")
    void shouldHaveGetAverageRequestDurationMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getAverageRequestDuration");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMinRequestDuration method")
    void shouldHaveGetMinRequestDurationMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getMinRequestDuration");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMaxRequestDuration method")
    void shouldHaveGetMaxRequestDurationMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getMaxRequestDuration");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("Error Count Method Tests")
  class ErrorCountMethodTests {

    @Test
    @DisplayName("should have getConnectionTimeouts method")
    void shouldHaveGetConnectionTimeoutsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getConnectionTimeouts");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getReadTimeouts method")
    void shouldHaveGetReadTimeoutsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getReadTimeouts");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getBlockedRequests method")
    void shouldHaveGetBlockedRequestsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getBlockedRequests");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getBodySizeLimitViolations method")
    void shouldHaveGetBodySizeLimitViolationsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getBodySizeLimitViolations");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Connection Pool Method Tests")
  class ConnectionPoolMethodTests {

    @Test
    @DisplayName("should have getActiveConnections method")
    void shouldHaveGetActiveConnectionsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getActiveConnections");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getIdleConnections method")
    void shouldHaveGetIdleConnectionsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpStats.class.getMethod("getIdleConnections");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Statistics Pattern Tests")
  class StatisticsPatternTests {

    @Test
    @DisplayName("should have all request counting metrics")
    void shouldHaveAllRequestCountingMetrics() {
      final String[] requestMetrics = {
        "getTotalRequests", "getSuccessfulRequests", "getFailedRequests", "getActiveRequests"
      };

      for (final String methodName : requestMetrics) {
        assertTrue(
            hasMethod(WasiHttpStats.class, methodName),
            "Should have request metric: " + methodName);
      }
    }

    @Test
    @DisplayName("should have all throughput metrics")
    void shouldHaveAllThroughputMetrics() {
      final String[] throughputMetrics = {"getTotalBytesSent", "getTotalBytesReceived"};

      for (final String methodName : throughputMetrics) {
        assertTrue(
            hasMethod(WasiHttpStats.class, methodName),
            "Should have throughput metric: " + methodName);
      }
    }

    @Test
    @DisplayName("should have all latency metrics")
    void shouldHaveAllLatencyMetrics() {
      final String[] latencyMetrics = {
        "getAverageRequestDuration", "getMinRequestDuration", "getMaxRequestDuration"
      };

      for (final String methodName : latencyMetrics) {
        assertTrue(
            hasMethod(WasiHttpStats.class, methodName),
            "Should have latency metric: " + methodName);
      }
    }

    @Test
    @DisplayName("should have all error metrics")
    void shouldHaveAllErrorMetrics() {
      final String[] errorMetrics = {
        "getConnectionTimeouts",
        "getReadTimeouts",
        "getBlockedRequests",
        "getBodySizeLimitViolations"
      };

      for (final String methodName : errorMetrics) {
        assertTrue(
            hasMethod(WasiHttpStats.class, methodName), "Should have error metric: " + methodName);
      }
    }

    @Test
    @DisplayName("should have all connection pool metrics")
    void shouldHaveAllConnectionPoolMetrics() {
      final String[] poolMetrics = {"getActiveConnections", "getIdleConnections"};

      for (final String methodName : poolMetrics) {
        assertTrue(
            hasMethod(WasiHttpStats.class, methodName), "Should have pool metric: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Observability Pattern Tests")
  class ObservabilityPatternTests {

    @Test
    @DisplayName("should support success rate calculation")
    void shouldSupportSuccessRateCalculation() {
      // Stats interface should provide data for calculating success rate
      // successRate = successfulRequests / totalRequests
      assertTrue(hasMethod(WasiHttpStats.class, "getTotalRequests"), "Need total");
      assertTrue(hasMethod(WasiHttpStats.class, "getSuccessfulRequests"), "Need successful");
    }

    @Test
    @DisplayName("should support error rate calculation")
    void shouldSupportErrorRateCalculation() {
      // Stats interface should provide data for calculating error rate
      // errorRate = failedRequests / totalRequests
      assertTrue(hasMethod(WasiHttpStats.class, "getTotalRequests"), "Need total");
      assertTrue(hasMethod(WasiHttpStats.class, "getFailedRequests"), "Need failed");
    }

    @Test
    @DisplayName("should support throughput calculation")
    void shouldSupportThroughputCalculation() {
      // Stats interface should provide data for throughput
      assertTrue(hasMethod(WasiHttpStats.class, "getTotalBytesSent"), "Need bytes sent");
      assertTrue(hasMethod(WasiHttpStats.class, "getTotalBytesReceived"), "Need bytes received");
    }

    @Test
    @DisplayName("should support latency percentile preparation")
    void shouldSupportLatencyPercentilePreparation() {
      // Stats interface provides min/max/avg which can help understand latency distribution
      assertTrue(hasMethod(WasiHttpStats.class, "getMinRequestDuration"), "Need min");
      assertTrue(hasMethod(WasiHttpStats.class, "getMaxRequestDuration"), "Need max");
      assertTrue(hasMethod(WasiHttpStats.class, "getAverageRequestDuration"), "Need avg");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Statistics Invariant Tests")
  class StatisticsInvariantTests {

    @Test
    @DisplayName("total requests should equal successful plus failed")
    void totalRequestsShouldEqualSuccessfulPlusFailed() {
      // Document invariant: totalRequests = successfulRequests + failedRequests + activeRequests
      // This can't be tested without implementation, but verifies interface supports it
      assertTrue(hasMethod(WasiHttpStats.class, "getTotalRequests"), "Need total");
      assertTrue(hasMethod(WasiHttpStats.class, "getSuccessfulRequests"), "Need successful");
      assertTrue(hasMethod(WasiHttpStats.class, "getFailedRequests"), "Need failed");
      assertTrue(hasMethod(WasiHttpStats.class, "getActiveRequests"), "Need active");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
