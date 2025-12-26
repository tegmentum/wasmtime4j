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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Store;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiHttpContext} interface.
 *
 * <p>WasiHttpContext provides the capability for WebAssembly components to make outbound HTTP
 * requests according to the WASI HTTP specification.
 */
@DisplayName("WasiHttpContext Tests")
class WasiHttpContextTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiHttpContext.class.isInterface(), "WasiHttpContext should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiHttpContext.class.getModifiers()),
          "WasiHttpContext should be public");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasiHttpContext.class),
          "WasiHttpContext should extend Closeable");
    }

    @Test
    @DisplayName("should have addToLinker method")
    void shouldHaveAddToLinkerMethod() throws NoSuchMethodException {
      final Method method =
          WasiHttpContext.class.getMethod("addToLinker", Linker.class, Store.class);
      assertNotNull(method, "addToLinker method should exist");
      assertEquals(void.class, method.getReturnType(), "addToLinker should return void");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          WasiHttpConfig.class, method.getReturnType(), "getConfig should return WasiHttpConfig");
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          WasiHttpStats.class, method.getReturnType(), "getStats should return WasiHttpStats");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have isHostAllowed method")
    void shouldHaveIsHostAllowedMethod() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("isHostAllowed", String.class);
      assertNotNull(method, "isHostAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isHostAllowed should return boolean");
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("resetStats");
      assertNotNull(method, "resetStats method should exist");
      assertEquals(void.class, method.getReturnType(), "resetStats should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("addToLinker should take Linker and Store parameters")
    void addToLinkerShouldTakeLinkerAndStoreParameters() throws NoSuchMethodException {
      final Method method =
          WasiHttpContext.class.getMethod("addToLinker", Linker.class, Store.class);
      assertEquals(2, method.getParameterCount(), "addToLinker should take two parameters");
      assertEquals(Linker.class, method.getParameterTypes()[0], "First parameter should be Linker");
      assertEquals(Store.class, method.getParameterTypes()[1], "Second parameter should be Store");
    }

    @Test
    @DisplayName("isHostAllowed should take String parameter")
    void isHostAllowedShouldTakeStringParameter() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("isHostAllowed", String.class);
      assertEquals(1, method.getParameterCount(), "isHostAllowed should take one parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("getConfig should take no parameters")
    void getConfigShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("getConfig");
      assertEquals(0, method.getParameterCount(), "getConfig should take no parameters");
    }

    @Test
    @DisplayName("getStats should take no parameters")
    void getStatsShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("getStats");
      assertEquals(0, method.getParameterCount(), "getStats should take no parameters");
    }

    @Test
    @DisplayName("isValid should take no parameters")
    void isValidShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("isValid");
      assertEquals(0, method.getParameterCount(), "isValid should take no parameters");
    }

    @Test
    @DisplayName("resetStats should take no parameters")
    void resetStatsShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiHttpContext.class.getMethod("resetStats");
      assertEquals(0, method.getParameterCount(), "resetStats should take no parameters");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return valid initially")
    void mockShouldReturnValidInitially() {
      final MockWasiHttpContext context = new MockWasiHttpContext();

      assertTrue(context.isValid(), "Should be valid initially");
    }

    @Test
    @DisplayName("mock should return invalid after close")
    void mockShouldReturnInvalidAfterClose() {
      final MockWasiHttpContext context = new MockWasiHttpContext();

      context.close();

      assertFalse(context.isValid(), "Should be invalid after close");
    }

    @Test
    @DisplayName("mock should return config")
    void mockShouldReturnConfig() {
      final MockWasiHttpContext context = new MockWasiHttpContext();

      final WasiHttpConfig config = context.getConfig();

      assertNotNull(config, "getConfig should return non-null");
    }

    @Test
    @DisplayName("mock should return stats")
    void mockShouldReturnStats() {
      final MockWasiHttpContext context = new MockWasiHttpContext();

      final WasiHttpStats stats = context.getStats();

      assertNotNull(stats, "getStats should return non-null");
    }

    @Test
    @DisplayName("mock should check allowed hosts")
    void mockShouldCheckAllowedHosts() {
      final MockWasiHttpContext context = new MockWasiHttpContext();
      context.addAllowedHost("api.example.com");

      assertTrue(context.isHostAllowed("api.example.com"), "Should allow configured host");
      assertFalse(context.isHostAllowed("other.com"), "Should not allow non-configured host");
    }

    @Test
    @DisplayName("mock should reset stats")
    void mockShouldResetStats() {
      final MockWasiHttpContext context = new MockWasiHttpContext();
      context.incrementRequestCount();

      context.resetStats();

      assertEquals(0, context.getRequestCount(), "Stats should be reset");
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (final Method method : WasiHttpContext.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          assertTrue(
              Modifier.isPublic(method.getModifiers()),
              "Method " + method.getName() + " should be public");
        }
      }
    }

    @Test
    @DisplayName("interface should have expected method count")
    void interfaceShouldHaveExpectedMethodCount() {
      int methodCount = 0;
      for (final Method method : WasiHttpContext.class.getDeclaredMethods()) {
        if (!method.isSynthetic() && !Modifier.isStatic(method.getModifiers())) {
          methodCount++;
        }
      }
      assertEquals(7, methodCount, "WasiHttpContext should have exactly 7 declared methods");
    }
  }

  /** Mock implementation of WasiHttpContext for testing. */
  private static class MockWasiHttpContext implements WasiHttpContext {
    private boolean valid = true;
    private final java.util.Set<String> allowedHosts = new java.util.HashSet<>();
    private int requestCount = 0;
    private final MockWasiHttpConfig config = new MockWasiHttpConfig();
    private final MockWasiHttpStats stats = new MockWasiHttpStats();

    @Override
    public void addToLinker(final Linker<?> linker, final Store store) {
      // No-op for mock
    }

    @Override
    public WasiHttpConfig getConfig() {
      return config;
    }

    @Override
    public WasiHttpStats getStats() {
      return stats;
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    @Override
    public boolean isHostAllowed(final String host) {
      return allowedHosts.contains(host);
    }

    @Override
    public void resetStats() {
      requestCount = 0;
    }

    @Override
    public void close() {
      valid = false;
    }

    public void addAllowedHost(final String host) {
      allowedHosts.add(host);
    }

    public void incrementRequestCount() {
      requestCount++;
    }

    public int getRequestCount() {
      return requestCount;
    }
  }

  /** Mock implementation of WasiHttpConfig for testing. */
  private static class MockWasiHttpConfig implements WasiHttpConfig {
    @Override
    public java.util.Set<String> getAllowedHosts() {
      return java.util.Collections.emptySet();
    }

    @Override
    public java.util.Set<String> getBlockedHosts() {
      return java.util.Collections.emptySet();
    }

    @Override
    public java.util.Optional<java.time.Duration> getConnectTimeout() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<java.time.Duration> getReadTimeout() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<java.time.Duration> getWriteTimeout() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Integer> getMaxConnections() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Integer> getMaxConnectionsPerHost() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Long> getMaxRequestBodySize() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Long> getMaxResponseBodySize() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.List<String> getAllowedMethods() {
      return java.util.Collections.emptyList();
    }

    @Override
    public boolean isHttpsRequired() {
      return false;
    }

    @Override
    public boolean isCertificateValidationEnabled() {
      return true;
    }

    @Override
    public boolean isHttp2Enabled() {
      return false;
    }

    @Override
    public boolean isConnectionPoolingEnabled() {
      return true;
    }

    @Override
    public boolean isFollowRedirects() {
      return true;
    }

    @Override
    public java.util.Optional<Integer> getMaxRedirects() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<String> getUserAgent() {
      return java.util.Optional.empty();
    }

    @Override
    public WasiHttpConfigBuilder toBuilder() {
      throw new UnsupportedOperationException("Mock does not support toBuilder");
    }

    @Override
    public void validate() {
      // Mock always valid
    }
  }

  /** Mock implementation of WasiHttpStats for testing. */
  private static class MockWasiHttpStats implements WasiHttpStats {
    @Override
    public long getTotalRequests() {
      return 0;
    }

    @Override
    public long getSuccessfulRequests() {
      return 0;
    }

    @Override
    public long getFailedRequests() {
      return 0;
    }

    @Override
    public int getActiveRequests() {
      return 0;
    }

    @Override
    public long getTotalBytesSent() {
      return 0;
    }

    @Override
    public long getTotalBytesReceived() {
      return 0;
    }

    @Override
    public java.time.Duration getAverageRequestDuration() {
      return java.time.Duration.ZERO;
    }

    @Override
    public java.time.Duration getMinRequestDuration() {
      return java.time.Duration.ZERO;
    }

    @Override
    public java.time.Duration getMaxRequestDuration() {
      return java.time.Duration.ZERO;
    }

    @Override
    public long getConnectionTimeouts() {
      return 0;
    }

    @Override
    public long getReadTimeouts() {
      return 0;
    }

    @Override
    public long getBlockedRequests() {
      return 0;
    }

    @Override
    public long getBodySizeLimitViolations() {
      return 0;
    }

    @Override
    public int getActiveConnections() {
      return 0;
    }

    @Override
    public int getIdleConnections() {
      return 0;
    }
  }
}
