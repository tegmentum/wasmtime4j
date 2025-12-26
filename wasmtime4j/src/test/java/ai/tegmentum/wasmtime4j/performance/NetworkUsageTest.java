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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetworkUsage} class.
 *
 * <p>NetworkUsage provides detailed network usage information for monitoring network I/O
 * operations.
 */
@DisplayName("NetworkUsage Tests")
class NetworkUsageTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(NetworkUsage.class.getModifiers()), "NetworkUsage should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(NetworkUsage.class.getModifiers()), "NetworkUsage should be public");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          NetworkUsage.class.getConstructor(
              long.class, long.class, long.class, long.class, int.class, Duration.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getBytesReceived method")
    void shouldHaveGetBytesReceivedMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getBytesReceived");
      assertNotNull(method, "getBytesReceived method should exist");
      assertEquals(long.class, method.getReturnType(), "getBytesReceived should return long");
    }

    @Test
    @DisplayName("should have getBytesSent method")
    void shouldHaveGetBytesSentMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getBytesSent");
      assertNotNull(method, "getBytesSent method should exist");
      assertEquals(long.class, method.getReturnType(), "getBytesSent should return long");
    }

    @Test
    @DisplayName("should have getPacketsReceived method")
    void shouldHaveGetPacketsReceivedMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getPacketsReceived");
      assertNotNull(method, "getPacketsReceived method should exist");
      assertEquals(long.class, method.getReturnType(), "getPacketsReceived should return long");
    }

    @Test
    @DisplayName("should have getPacketsSent method")
    void shouldHaveGetPacketsSentMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getPacketsSent");
      assertNotNull(method, "getPacketsSent method should exist");
      assertEquals(long.class, method.getReturnType(), "getPacketsSent should return long");
    }

    @Test
    @DisplayName("should have getActiveConnections method")
    void shouldHaveGetActiveConnectionsMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getActiveConnections");
      assertNotNull(method, "getActiveConnections method should exist");
      assertEquals(int.class, method.getReturnType(), "getActiveConnections should return int");
    }

    @Test
    @DisplayName("should have getAverageConnectionTime method")
    void shouldHaveGetAverageConnectionTimeMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getAverageConnectionTime");
      assertNotNull(method, "getAverageConnectionTime method should exist");
      assertEquals(
          Duration.class,
          method.getReturnType(),
          "getAverageConnectionTime should return Duration");
    }
  }

  @Nested
  @DisplayName("Derived Metric Method Tests")
  class DerivedMetricMethodTests {

    @Test
    @DisplayName("should have getTotalBytesTransferred method")
    void shouldHaveGetTotalBytesTransferredMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getTotalBytesTransferred");
      assertNotNull(method, "getTotalBytesTransferred method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getTotalBytesTransferred should return long");
    }

    @Test
    @DisplayName("should have getTotalPacketsTransferred method")
    void shouldHaveGetTotalPacketsTransferredMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getTotalPacketsTransferred");
      assertNotNull(method, "getTotalPacketsTransferred method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getTotalPacketsTransferred should return long");
    }

    @Test
    @DisplayName("should have getReceiveThroughput method")
    void shouldHaveGetReceiveThroughputMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getReceiveThroughput", Duration.class);
      assertNotNull(method, "getReceiveThroughput method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getReceiveThroughput should return double");
    }

    @Test
    @DisplayName("should have getSendThroughput method")
    void shouldHaveGetSendThroughputMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getSendThroughput", Duration.class);
      assertNotNull(method, "getSendThroughput method should exist");
      assertEquals(double.class, method.getReturnType(), "getSendThroughput should return double");
    }

    @Test
    @DisplayName("should have getPacketRate method")
    void shouldHaveGetPacketRateMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getPacketRate", Duration.class);
      assertNotNull(method, "getPacketRate method should exist");
      assertEquals(double.class, method.getReturnType(), "getPacketRate should return double");
    }

    @Test
    @DisplayName("should have getAverageBytesPerPacket method")
    void shouldHaveGetAverageBytesPerPacketMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getAverageBytesPerPacket");
      assertNotNull(method, "getAverageBytesPerPacket method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getAverageBytesPerPacket should return double");
    }

    @Test
    @DisplayName("should have getSendReceiveRatio method")
    void shouldHaveGetSendReceiveRatioMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getSendReceiveRatio");
      assertNotNull(method, "getSendReceiveRatio method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getSendReceiveRatio should return double");
    }
  }

  @Nested
  @DisplayName("Analysis Method Tests")
  class AnalysisMethodTests {

    @Test
    @DisplayName("should have isBulkDataTransfer method")
    void shouldHaveIsBulkDataTransferMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("isBulkDataTransfer");
      assertNotNull(method, "isBulkDataTransfer method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isBulkDataTransfer should return boolean");
    }

    @Test
    @DisplayName("should have isInteractiveTraffic method")
    void shouldHaveIsInteractiveTrafficMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("isInteractiveTraffic");
      assertNotNull(method, "isInteractiveTraffic method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isInteractiveTraffic should return boolean");
    }

    @Test
    @DisplayName("should have hasHighConnectionCount method")
    void shouldHaveHasHighConnectionCountMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("hasHighConnectionCount");
      assertNotNull(method, "hasHighConnectionCount method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "hasHighConnectionCount should return boolean");
    }

    @Test
    @DisplayName("should have getEfficiencyScore method")
    void shouldHaveGetEfficiencyScoreMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("getEfficiencyScore");
      assertNotNull(method, "getEfficiencyScore method should exist");
      assertEquals(double.class, method.getReturnType(), "getEfficiencyScore should return double");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have formatBytes static method")
    void shouldHaveFormatBytesStaticMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("formatBytes", long.class);
      assertNotNull(method, "formatBytes method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "formatBytes should be static");
      assertEquals(String.class, method.getReturnType(), "formatBytes should return String");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = NetworkUsage.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final NetworkUsage networkUsage =
          new NetworkUsage(
              1024L * 1024L, // bytesReceived
              512L * 1024L, // bytesSent
              1000L, // packetsReceived
              500L, // packetsSent
              10, // activeConnections
              Duration.ofMinutes(5)); // averageConnectionTime

      assertEquals(1024L * 1024L, networkUsage.getBytesReceived(), "Bytes received should match");
      assertEquals(512L * 1024L, networkUsage.getBytesSent(), "Bytes sent should match");
      assertEquals(1000L, networkUsage.getPacketsReceived(), "Packets received should match");
      assertEquals(500L, networkUsage.getPacketsSent(), "Packets sent should match");
      assertEquals(10, networkUsage.getActiveConnections(), "Active connections should match");
      assertEquals(
          Duration.ofMinutes(5),
          networkUsage.getAverageConnectionTime(),
          "Avg connection time should match");
    }

    @Test
    @DisplayName("should clamp negative values to zero")
    void shouldClampNegativeValuesToZero() {
      final NetworkUsage networkUsage =
          new NetworkUsage(-100L, -50L, -10L, -5L, -1, Duration.ofSeconds(10));

      assertEquals(0L, networkUsage.getBytesReceived(), "Bytes received should be clamped to 0");
      assertEquals(0L, networkUsage.getBytesSent(), "Bytes sent should be clamped to 0");
      assertEquals(
          0L, networkUsage.getPacketsReceived(), "Packets received should be clamped to 0");
      assertEquals(0L, networkUsage.getPacketsSent(), "Packets sent should be clamped to 0");
      assertEquals(
          0, networkUsage.getActiveConnections(), "Active connections should be clamped to 0");
    }

    @Test
    @DisplayName("should throw exception for negative duration")
    void shouldThrowExceptionForNegativeDuration() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new NetworkUsage(100L, 50L, 10L, 5L, 1, Duration.ofSeconds(-10)),
          "Should throw exception for negative duration");
    }

    @Test
    @DisplayName("should calculate total bytes transferred correctly")
    void shouldCalculateTotalBytesTransferredCorrectly() {
      final NetworkUsage networkUsage =
          new NetworkUsage(1024L, 512L, 100L, 50L, 5, Duration.ofSeconds(30));

      assertEquals(1536L, networkUsage.getTotalBytesTransferred(), "Total bytes should be 1536");
    }

    @Test
    @DisplayName("should calculate total packets transferred correctly")
    void shouldCalculateTotalPacketsTransferredCorrectly() {
      final NetworkUsage networkUsage =
          new NetworkUsage(1024L, 512L, 100L, 50L, 5, Duration.ofSeconds(30));

      assertEquals(150L, networkUsage.getTotalPacketsTransferred(), "Total packets should be 150");
    }

    @Test
    @DisplayName("should detect bulk data transfer")
    void shouldDetectBulkDataTransfer() {
      // Large packets (> 1KB average) indicate bulk transfer
      final NetworkUsage bulkUsage =
          new NetworkUsage(10240L, 5120L, 5L, 2L, 1, Duration.ofSeconds(30));
      assertTrue(bulkUsage.isBulkDataTransfer(), "Should detect bulk data transfer");

      // Small packets indicate interactive traffic
      final NetworkUsage interactiveUsage =
          new NetworkUsage(1024L, 512L, 100L, 50L, 1, Duration.ofSeconds(30));
      assertFalse(interactiveUsage.isBulkDataTransfer(), "Should not detect bulk data transfer");
    }

    @Test
    @DisplayName("should detect high connection count")
    void shouldDetectHighConnectionCount() {
      final NetworkUsage highConnections =
          new NetworkUsage(1024L, 512L, 100L, 50L, 150, Duration.ofSeconds(30));
      assertTrue(highConnections.hasHighConnectionCount(), "Should detect high connection count");

      final NetworkUsage normalConnections =
          new NetworkUsage(1024L, 512L, 100L, 50L, 50, Duration.ofSeconds(30));
      assertFalse(
          normalConnections.hasHighConnectionCount(), "Should not detect high connection count");
    }

    @Test
    @DisplayName("should format bytes correctly")
    void shouldFormatBytesCorrectly() {
      assertEquals("0 B", NetworkUsage.formatBytes(0), "Should format 0 bytes");
      assertEquals("1023 B", NetworkUsage.formatBytes(1023), "Should format bytes");
      assertEquals("1.0 KB", NetworkUsage.formatBytes(1024), "Should format KB");
      assertEquals("1.0 MB", NetworkUsage.formatBytes(1024L * 1024L), "Should format MB");
      assertEquals("1.0 GB", NetworkUsage.formatBytes(1024L * 1024L * 1024L), "Should format GB");
    }
  }
}
