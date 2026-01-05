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

import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfigBuilder;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiHttpConfig} class.
 *
 * <p>PanamaWasiHttpConfig provides Panama implementation of WASI HTTP configuration.
 */
@DisplayName("PanamaWasiHttpConfig Tests")
class PanamaWasiHttpConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiHttpConfig.class.getModifiers()),
          "PanamaWasiHttpConfig should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiHttpConfig.class.getModifiers()),
          "PanamaWasiHttpConfig should be final");
    }

    @Test
    @DisplayName("should implement WasiHttpConfig interface")
    void shouldImplementWasiHttpConfigInterface() {
      assertTrue(
          WasiHttpConfig.class.isAssignableFrom(PanamaWasiHttpConfig.class),
          "PanamaWasiHttpConfig should implement WasiHttpConfig");
    }
  }

  @Nested
  @DisplayName("Host Configuration Method Tests")
  class HostConfigurationMethodTests {

    @Test
    @DisplayName("should have getAllowedHosts method")
    void shouldHaveGetAllowedHostsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getAllowedHosts");
      assertNotNull(method, "getAllowedHosts method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getBlockedHosts method")
    void shouldHaveGetBlockedHostsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getBlockedHosts");
      assertNotNull(method, "getBlockedHosts method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }
  }

  @Nested
  @DisplayName("Timeout Method Tests")
  class TimeoutMethodTests {

    @Test
    @DisplayName("should have getConnectTimeout method")
    void shouldHaveGetConnectTimeoutMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getConnectTimeout");
      assertNotNull(method, "getConnectTimeout method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getReadTimeout method")
    void shouldHaveGetReadTimeoutMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getReadTimeout");
      assertNotNull(method, "getReadTimeout method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getWriteTimeout method")
    void shouldHaveGetWriteTimeoutMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getWriteTimeout");
      assertNotNull(method, "getWriteTimeout method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Connection Limit Method Tests")
  class ConnectionLimitMethodTests {

    @Test
    @DisplayName("should have getMaxConnections method")
    void shouldHaveGetMaxConnectionsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getMaxConnections");
      assertNotNull(method, "getMaxConnections method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getMaxConnectionsPerHost method")
    void shouldHaveGetMaxConnectionsPerHostMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getMaxConnectionsPerHost");
      assertNotNull(method, "getMaxConnectionsPerHost method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Body Size Limit Method Tests")
  class BodySizeLimitMethodTests {

    @Test
    @DisplayName("should have getMaxRequestBodySize method")
    void shouldHaveGetMaxRequestBodySizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getMaxRequestBodySize");
      assertNotNull(method, "getMaxRequestBodySize method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getMaxResponseBodySize method")
    void shouldHaveGetMaxResponseBodySizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getMaxResponseBodySize");
      assertNotNull(method, "getMaxResponseBodySize method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("HTTP Methods Configuration Tests")
  class HttpMethodsConfigurationTests {

    @Test
    @DisplayName("should have getAllowedMethods method")
    void shouldHaveGetAllowedMethodsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getAllowedMethods");
      assertNotNull(method, "getAllowedMethods method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Security Configuration Method Tests")
  class SecurityConfigurationMethodTests {

    @Test
    @DisplayName("should have isHttpsRequired method")
    void shouldHaveIsHttpsRequiredMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("isHttpsRequired");
      assertNotNull(method, "isHttpsRequired method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isCertificateValidationEnabled method")
    void shouldHaveIsCertificateValidationEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("isCertificateValidationEnabled");
      assertNotNull(method, "isCertificateValidationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Protocol Configuration Method Tests")
  class ProtocolConfigurationMethodTests {

    @Test
    @DisplayName("should have isHttp2Enabled method")
    void shouldHaveIsHttp2EnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("isHttp2Enabled");
      assertNotNull(method, "isHttp2Enabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isConnectionPoolingEnabled method")
    void shouldHaveIsConnectionPoolingEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("isConnectionPoolingEnabled");
      assertNotNull(method, "isConnectionPoolingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Redirect Configuration Method Tests")
  class RedirectConfigurationMethodTests {

    @Test
    @DisplayName("should have isFollowRedirects method")
    void shouldHaveIsFollowRedirectsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("isFollowRedirects");
      assertNotNull(method, "isFollowRedirects method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMaxRedirects method")
    void shouldHaveGetMaxRedirectsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getMaxRedirects");
      assertNotNull(method, "getMaxRedirects method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("User Agent Method Tests")
  class UserAgentMethodTests {

    @Test
    @DisplayName("should have getUserAgent method")
    void shouldHaveGetUserAgentMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("getUserAgent");
      assertNotNull(method, "getUserAgent method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Builder Method Tests")
  class BuilderMethodTests {

    @Test
    @DisplayName("should have toBuilder method")
    void shouldHaveToBuilderMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("toBuilder");
      assertNotNull(method, "toBuilder method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfig.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }
}
