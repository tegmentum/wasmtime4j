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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AbstractWasiHttpStats} getters and toString. */
@DisplayName("AbstractWasiHttpStats Tests")
class AbstractWasiHttpStatsTest {

  /** Concrete test subclass for AbstractWasiHttpStats. */
  private static final class TestHttpStats extends AbstractWasiHttpStats {
    TestHttpStats(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        int activeRequests,
        long bytesSent,
        long bytesReceived,
        long connectionTimeouts,
        long readTimeouts,
        long blockedRequests,
        long bodySizeViolations,
        int activeConnections,
        int idleConnections,
        long avgDurationMs,
        long minDurationMs,
        long maxDurationMs) {
      super(
          totalRequests,
          successfulRequests,
          failedRequests,
          activeRequests,
          bytesSent,
          bytesReceived,
          connectionTimeouts,
          readTimeouts,
          blockedRequests,
          bodySizeViolations,
          activeConnections,
          idleConnections,
          avgDurationMs,
          minDurationMs,
          maxDurationMs);
    }
  }

  private TestHttpStats createStats() {
    return new TestHttpStats(100, 90, 10, 5, 50000, 75000, 2, 3, 1, 4, 8, 12, 150, 10, 500);
  }

  @Nested
  @DisplayName("Getter Methods")
  class GetterMethods {

    @Test
    @DisplayName("should return correct totalRequests")
    void shouldReturnCorrectTotalRequests() {
      assertEquals(100, createStats().totalRequests());
    }

    @Test
    @DisplayName("should return correct successfulRequests")
    void shouldReturnCorrectSuccessfulRequests() {
      assertEquals(90, createStats().successfulRequests());
    }

    @Test
    @DisplayName("should return correct failedRequests")
    void shouldReturnCorrectFailedRequests() {
      assertEquals(10, createStats().failedRequests());
    }

    @Test
    @DisplayName("should return correct activeRequests")
    void shouldReturnCorrectActiveRequests() {
      assertEquals(5, createStats().activeRequests());
    }

    @Test
    @DisplayName("should return correct bytesSent")
    void shouldReturnCorrectBytesSent() {
      assertEquals(50000, createStats().bytesSent());
    }

    @Test
    @DisplayName("should return correct bytesReceived")
    void shouldReturnCorrectBytesReceived() {
      assertEquals(75000, createStats().bytesReceived());
    }

    @Test
    @DisplayName("should return correct connectionTimeouts")
    void shouldReturnCorrectConnectionTimeouts() {
      assertEquals(2, createStats().connectionTimeouts());
    }

    @Test
    @DisplayName("should return correct readTimeouts")
    void shouldReturnCorrectReadTimeouts() {
      assertEquals(3, createStats().readTimeouts());
    }

    @Test
    @DisplayName("should return correct blockedRequests")
    void shouldReturnCorrectBlockedRequests() {
      assertEquals(1, createStats().blockedRequests());
    }

    @Test
    @DisplayName("should return correct bodySizeViolations")
    void shouldReturnCorrectBodySizeViolations() {
      assertEquals(4, createStats().bodySizeViolations());
    }

    @Test
    @DisplayName("should return correct activeConnections")
    void shouldReturnCorrectActiveConnections() {
      assertEquals(8, createStats().activeConnections());
    }

    @Test
    @DisplayName("should return correct idleConnections")
    void shouldReturnCorrectIdleConnections() {
      assertEquals(12, createStats().idleConnections());
    }

    @Test
    @DisplayName("should return correct avgDurationMs")
    void shouldReturnCorrectAvgDurationMs() {
      assertEquals(150, createStats().avgDurationMs());
    }

    @Test
    @DisplayName("should return correct minDurationMs")
    void shouldReturnCorrectMinDurationMs() {
      assertEquals(10, createStats().minDurationMs());
    }

    @Test
    @DisplayName("should return correct maxDurationMs")
    void shouldReturnCorrectMaxDurationMs() {
      assertEquals(500, createStats().maxDurationMs());
    }
  }

  @Nested
  @DisplayName("Zero Value Stats")
  class ZeroValueStats {

    @Test
    @DisplayName("should handle all zero values")
    void shouldHandleAllZeroValues() {
      TestHttpStats stats = new TestHttpStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

      assertEquals(0, stats.totalRequests());
      assertEquals(0, stats.successfulRequests());
      assertEquals(0, stats.activeRequests());
      assertEquals(0, stats.avgDurationMs());
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain all field values")
    void toStringShouldContainAllFieldValues() {
      String str = createStats().toString();

      assertNotNull(str);
      assertTrue(str.startsWith("WasiHttpStats{"));
      assertTrue(str.contains("totalRequests=100"));
      assertTrue(str.contains("successfulRequests=90"));
      assertTrue(str.contains("failedRequests=10"));
      assertTrue(str.contains("activeRequests=5"));
      assertTrue(str.contains("bytesSent=50000"));
      assertTrue(str.contains("bytesReceived=75000"));
      assertTrue(str.contains("connectionTimeouts=2"));
      assertTrue(str.contains("readTimeouts=3"));
      assertTrue(str.contains("blockedRequests=1"));
      assertTrue(str.contains("bodySizeViolations=4"));
      assertTrue(str.contains("activeConnections=8"));
      assertTrue(str.contains("idleConnections=12"));
      assertTrue(str.contains("avgDurationMs=150"));
      assertTrue(str.contains("minDurationMs=10"));
      assertTrue(str.contains("maxDurationMs=500"));
    }
  }
}
