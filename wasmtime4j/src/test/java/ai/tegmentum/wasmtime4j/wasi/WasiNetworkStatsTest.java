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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiNetworkStats} interface.
 *
 * <p>WasiNetworkStats provides network operation statistics.
 */
@DisplayName("WasiNetworkStats Tests")
class WasiNetworkStatsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiNetworkStats.class.getModifiers()),
          "WasiNetworkStats should be public");
      assertTrue(WasiNetworkStats.class.isInterface(), "WasiNetworkStats should be an interface");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getConnectionCount method")
    void shouldHaveGetConnectionCountMethod() throws NoSuchMethodException {
      final Method method = WasiNetworkStats.class.getMethod("getConnectionCount");
      assertNotNull(method, "getConnectionCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCurrentConnections method")
    void shouldHaveGetCurrentConnectionsMethod() throws NoSuchMethodException {
      final Method method = WasiNetworkStats.class.getMethod("getCurrentConnections");
      assertNotNull(method, "getCurrentConnections method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getBytesSent method")
    void shouldHaveGetBytesSentMethod() throws NoSuchMethodException {
      final Method method = WasiNetworkStats.class.getMethod("getBytesSent");
      assertNotNull(method, "getBytesSent method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getBytesReceived method")
    void shouldHaveGetBytesReceivedMethod() throws NoSuchMethodException {
      final Method method = WasiNetworkStats.class.getMethod("getBytesReceived");
      assertNotNull(method, "getBytesReceived method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getNetworkErrors method")
    void shouldHaveGetNetworkErrorsMethod() throws NoSuchMethodException {
      final Method method = WasiNetworkStats.class.getMethod("getNetworkErrors");
      assertNotNull(method, "getNetworkErrors method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Implementation Behavior Tests")
  class ImplementationBehaviorTests {

    @Test
    @DisplayName("implementation should track connection counts")
    void implementationShouldTrackConnectionCounts() {
      final WasiNetworkStats stats = createTestStats(100L, 5, 1024L, 2048L, 3L);

      assertEquals(100L, stats.getConnectionCount(), "Connection count should match");
      assertEquals(5, stats.getCurrentConnections(), "Current connections should match");
    }

    @Test
    @DisplayName("implementation should track bytes transferred")
    void implementationShouldTrackBytesTransferred() {
      final WasiNetworkStats stats = createTestStats(100L, 5, 50000L, 75000L, 3L);

      assertEquals(50000L, stats.getBytesSent(), "Bytes sent should match");
      assertEquals(75000L, stats.getBytesReceived(), "Bytes received should match");
    }

    @Test
    @DisplayName("implementation should track network errors")
    void implementationShouldTrackNetworkErrors() {
      final WasiNetworkStats stats = createTestStats(100L, 5, 1024L, 2048L, 10L);

      assertEquals(10L, stats.getNetworkErrors(), "Network errors should match");
    }

    private WasiNetworkStats createTestStats(
        final long connectionCount,
        final int currentConnections,
        final long bytesSent,
        final long bytesReceived,
        final long networkErrors) {
      return new WasiNetworkStats() {
        @Override
        public long getConnectionCount() {
          return connectionCount;
        }

        @Override
        public int getCurrentConnections() {
          return currentConnections;
        }

        @Override
        public long getBytesSent() {
          return bytesSent;
        }

        @Override
        public long getBytesReceived() {
          return bytesReceived;
        }

        @Override
        public long getNetworkErrors() {
          return networkErrors;
        }
      };
    }
  }
}
