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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Collection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiHttpConfigBuilder} class.
 *
 * <p>PanamaWasiHttpConfigBuilder provides builder for WASI HTTP configuration.
 */
@DisplayName("PanamaWasiHttpConfigBuilder Tests")
class PanamaWasiHttpConfigBuilderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiHttpConfigBuilder.class.getModifiers()),
          "PanamaWasiHttpConfigBuilder should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiHttpConfigBuilder.class.getModifiers()),
          "PanamaWasiHttpConfigBuilder should be final");
    }

    @Test
    @DisplayName("should implement WasiHttpConfigBuilder interface")
    void shouldImplementWasiHttpConfigBuilderInterface() {
      assertTrue(
          WasiHttpConfigBuilder.class.isAssignableFrom(PanamaWasiHttpConfigBuilder.class),
          "PanamaWasiHttpConfigBuilder should implement WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = PanamaWasiHttpConfigBuilder.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Host Configuration Method Tests")
  class HostConfigurationMethodTests {

    @Test
    @DisplayName("should have allowHost method")
    void shouldHaveAllowHostMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfigBuilder.class.getMethod("allowHost", String.class);
      assertNotNull(method, "allowHost method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have allowHosts method")
    void shouldHaveAllowHostsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("allowHosts", Collection.class);
      assertNotNull(method, "allowHosts method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have allowAllHosts method")
    void shouldHaveAllowAllHostsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfigBuilder.class.getMethod("allowAllHosts");
      assertNotNull(method, "allowAllHosts method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have blockHost method")
    void shouldHaveBlockHostMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfigBuilder.class.getMethod("blockHost", String.class);
      assertNotNull(method, "blockHost method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have blockHosts method")
    void shouldHaveBlockHostsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("blockHosts", Collection.class);
      assertNotNull(method, "blockHosts method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Timeout Configuration Method Tests")
  class TimeoutConfigurationMethodTests {

    @Test
    @DisplayName("should have withConnectTimeout method")
    void shouldHaveWithConnectTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withConnectTimeout", Duration.class);
      assertNotNull(method, "withConnectTimeout method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have withReadTimeout method")
    void shouldHaveWithReadTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withReadTimeout", Duration.class);
      assertNotNull(method, "withReadTimeout method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have withWriteTimeout method")
    void shouldHaveWithWriteTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withWriteTimeout", Duration.class);
      assertNotNull(method, "withWriteTimeout method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Connection Limit Method Tests")
  class ConnectionLimitMethodTests {

    @Test
    @DisplayName("should have withMaxConnections method")
    void shouldHaveWithMaxConnectionsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withMaxConnections", int.class);
      assertNotNull(method, "withMaxConnections method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have withMaxConnectionsPerHost method")
    void shouldHaveWithMaxConnectionsPerHostMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withMaxConnectionsPerHost", int.class);
      assertNotNull(method, "withMaxConnectionsPerHost method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Body Size Limit Method Tests")
  class BodySizeLimitMethodTests {

    @Test
    @DisplayName("should have withMaxRequestBodySize method")
    void shouldHaveWithMaxRequestBodySizeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withMaxRequestBodySize", long.class);
      assertNotNull(method, "withMaxRequestBodySize method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have withMaxResponseBodySize method")
    void shouldHaveWithMaxResponseBodySizeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withMaxResponseBodySize", long.class);
      assertNotNull(method, "withMaxResponseBodySize method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("HTTP Methods Configuration Tests")
  class HttpMethodsConfigurationTests {

    @Test
    @DisplayName("should have allowMethods method")
    void shouldHaveAllowMethodsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("allowMethods", String[].class);
      assertNotNull(method, "allowMethods method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Security Configuration Method Tests")
  class SecurityConfigurationMethodTests {

    @Test
    @DisplayName("should have requireHttps method")
    void shouldHaveRequireHttpsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("requireHttps", boolean.class);
      assertNotNull(method, "requireHttps method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have withCertificateValidation method")
    void shouldHaveWithCertificateValidationMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withCertificateValidation", boolean.class);
      assertNotNull(method, "withCertificateValidation method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Protocol Configuration Method Tests")
  class ProtocolConfigurationMethodTests {

    @Test
    @DisplayName("should have withHttp2 method")
    void shouldHaveWithHttp2Method() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfigBuilder.class.getMethod("withHttp2", boolean.class);
      assertNotNull(method, "withHttp2 method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have withConnectionPooling method")
    void shouldHaveWithConnectionPoolingMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withConnectionPooling", boolean.class);
      assertNotNull(method, "withConnectionPooling method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Redirect Configuration Method Tests")
  class RedirectConfigurationMethodTests {

    @Test
    @DisplayName("should have followRedirects method")
    void shouldHaveFollowRedirectsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("followRedirects", boolean.class);
      assertNotNull(method, "followRedirects method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("should have withMaxRedirects method")
    void shouldHaveWithMaxRedirectsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withMaxRedirects", int.class);
      assertNotNull(method, "withMaxRedirects method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("User Agent Method Tests")
  class UserAgentMethodTests {

    @Test
    @DisplayName("should have withUserAgent method")
    void shouldHaveWithUserAgentMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiHttpConfigBuilder.class.getMethod("withUserAgent", String.class);
      assertNotNull(method, "withUserAgent method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "Should return WasiHttpConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiHttpConfigBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(WasiHttpConfig.class, method.getReturnType(), "Should return WasiHttpConfig");
    }
  }
}
