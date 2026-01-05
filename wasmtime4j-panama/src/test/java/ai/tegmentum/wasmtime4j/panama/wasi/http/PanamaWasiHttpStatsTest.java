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

package ai.tegmentum.wasmtime4j.panama.wasi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpStats;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiHttpStats} class.
 *
 * <p>PanamaWasiHttpStats provides Panama implementation of WASI HTTP statistics.
 */
@DisplayName("PanamaWasiHttpStats Tests")
class PanamaWasiHttpStatsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiHttpStats.class.getModifiers()),
          "PanamaWasiHttpStats should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiHttpStats.class.getModifiers()),
          "PanamaWasiHttpStats should be final");
    }

    @Test
    @DisplayName("should implement WasiHttpStats interface")
    void shouldImplementWasiHttpStatsInterface() {
      assertTrue(
          WasiHttpStats.class.isAssignableFrom(PanamaWasiHttpStats.class),
          "PanamaWasiHttpStats should implement WasiHttpStats");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with MemorySegment and NativeFunctionBindings")
    void shouldHaveNativeConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaWasiHttpStats.class.getConstructor(
              MemorySegment.class, NativeFunctionBindings.class);
      assertNotNull(
          constructor, "Constructor with MemorySegment, NativeFunctionBindings should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Request Count Method Tests")
  class RequestCountMethodTests {

    @Test
    @DisplayName("should have getTotalRequests method")
    void shouldHaveGetTotalRequestsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getTotalRequests");
      assertNotNull(method, "getTotalRequests method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getSuccessfulRequests method")
    void shouldHaveGetSuccessfulRequestsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getSuccessfulRequests");
      assertNotNull(method, "getSuccessfulRequests method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFailedRequests method")
    void shouldHaveGetFailedRequestsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getFailedRequests");
      assertNotNull(method, "getFailedRequests method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getActiveRequests method")
    void shouldHaveGetActiveRequestsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getActiveRequests");
      assertNotNull(method, "getActiveRequests method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Byte Transfer Method Tests")
  class ByteTransferMethodTests {

    @Test
    @DisplayName("should have getTotalBytesSent method")
    void shouldHaveGetTotalBytesSentMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getTotalBytesSent");
      assertNotNull(method, "getTotalBytesSent method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalBytesReceived method")
    void shouldHaveGetTotalBytesReceivedMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getTotalBytesReceived");
      assertNotNull(method, "getTotalBytesReceived method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Duration Method Tests")
  class DurationMethodTests {

    @Test
    @DisplayName("should have getAverageRequestDuration method")
    void shouldHaveGetAverageRequestDurationMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getAverageRequestDuration");
      assertNotNull(method, "getAverageRequestDuration method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMinRequestDuration method")
    void shouldHaveGetMinRequestDurationMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getMinRequestDuration");
      assertNotNull(method, "getMinRequestDuration method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMaxRequestDuration method")
    void shouldHaveGetMaxRequestDurationMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getMaxRequestDuration");
      assertNotNull(method, "getMaxRequestDuration method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("Timeout Method Tests")
  class TimeoutMethodTests {

    @Test
    @DisplayName("should have getConnectionTimeouts method")
    void shouldHaveGetConnectionTimeoutsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getConnectionTimeouts");
      assertNotNull(method, "getConnectionTimeouts method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getReadTimeouts method")
    void shouldHaveGetReadTimeoutsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getReadTimeouts");
      assertNotNull(method, "getReadTimeouts method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Security Stats Method Tests")
  class SecurityStatsMethodTests {

    @Test
    @DisplayName("should have getBlockedRequests method")
    void shouldHaveGetBlockedRequestsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getBlockedRequests");
      assertNotNull(method, "getBlockedRequests method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getBodySizeLimitViolations method")
    void shouldHaveGetBodySizeLimitViolationsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getBodySizeLimitViolations");
      assertNotNull(method, "getBodySizeLimitViolations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Connection Pool Method Tests")
  class ConnectionPoolMethodTests {

    @Test
    @DisplayName("should have getActiveConnections method")
    void shouldHaveGetActiveConnectionsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getActiveConnections");
      assertNotNull(method, "getActiveConnections method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getIdleConnections method")
    void shouldHaveGetIdleConnectionsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("getIdleConnections");
      assertNotNull(method, "getIdleConnections method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpStats.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
