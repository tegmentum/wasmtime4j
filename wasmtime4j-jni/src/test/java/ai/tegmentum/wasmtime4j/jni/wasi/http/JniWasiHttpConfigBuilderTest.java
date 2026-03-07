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
package ai.tegmentum.wasmtime4j.jni.wasi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasiHttpConfigBuilder}.
 *
 * <p>Verifies default values, null and negative validation, builder fluency (method chaining),
 * collection behavior, and build() output.
 */
@DisplayName("JniWasiHttpConfigBuilder Tests")
class JniWasiHttpConfigBuilderTest {

  private static final Logger LOGGER =
      Logger.getLogger(JniWasiHttpConfigBuilderTest.class.getName());

  @Nested
  @DisplayName("Default Values")
  class DefaultValues {

    @Test
    @DisplayName("New builder should produce config with empty allowed hosts")
    void newBuilderShouldHaveEmptyAllowedHosts() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().build();
      assertTrue(
          config.getAllowedHosts().isEmpty(),
          "Default config should have no allowed hosts");
    }

    @Test
    @DisplayName("New builder should produce config with empty blocked hosts")
    void newBuilderShouldHaveEmptyBlockedHosts() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().build();
      assertTrue(
          config.getBlockedHosts().isEmpty(),
          "Default config should have no blocked hosts");
    }

    @Test
    @DisplayName("New builder should produce config with empty allowed methods")
    void newBuilderShouldHaveEmptyAllowedMethods() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().build();
      assertTrue(
          config.getAllowedMethods().isEmpty(),
          "Default config should have no allowed methods");
    }

    @Test
    @DisplayName("Boolean defaults should match expected values")
    void booleanDefaultsShouldMatchExpected() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().build();

      assertFalse(config.isHttpsRequired(), "httpsRequired default should be false");
      assertTrue(
          config.isCertificateValidationEnabled(),
          "certificateValidation default should be true");
      assertTrue(config.isHttp2Enabled(), "http2 default should be true");
      assertTrue(
          config.isConnectionPoolingEnabled(),
          "connectionPooling default should be true");
      assertTrue(config.isFollowRedirects(), "followRedirects default should be true");
    }
  }

  @Nested
  @DisplayName("Null Validation")
  class NullValidation {

    @Test
    @DisplayName("allowHost(null) should throw IllegalArgumentException")
    void allowHostNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> builder.allowHost(null));
      assertTrue(e.getMessage().contains("null"),
          "Expected message to contain: null");
    }

    @Test
    @DisplayName("allowHost(\"\") should throw IllegalArgumentException")
    void allowHostEmptyShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> builder.allowHost(""));
      assertTrue(e.getMessage().contains("empty"),
          "Expected message to contain: empty");
    }

    @Test
    @DisplayName("blockHost(null) should throw IllegalArgumentException")
    void blockHostNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThrows(IllegalArgumentException.class, () -> builder.blockHost(null));
    }

    @Test
    @DisplayName("withConnectTimeout(null) should throw NullPointerException")
    void connectTimeoutNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThrows(NullPointerException.class, () -> builder.withConnectTimeout(null));
    }

    @Test
    @DisplayName("allowHosts(null) should throw NullPointerException")
    void allowHostsNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThrows(NullPointerException.class, () -> builder.allowHosts(null));
    }

    @Test
    @DisplayName("allowMethods(null) should throw NullPointerException")
    void allowMethodsNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThrows(NullPointerException.class, () -> builder.allowMethods((String[]) null));
    }
  }

  @Nested
  @DisplayName("Negative Value Validation")
  class NegativeValueValidation {

    @Test
    @DisplayName("withMaxConnections(0) should throw IllegalArgumentException")
    void maxConnectionsZeroShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> builder.withMaxConnections(0));
      assertTrue(e.getMessage().contains("positive"),
          "Expected message to contain: positive");
    }

    @Test
    @DisplayName("withMaxConnections(-1) should throw IllegalArgumentException")
    void maxConnectionsNegativeShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThrows(IllegalArgumentException.class, () -> builder.withMaxConnections(-1));
    }

    @Test
    @DisplayName("withMaxRequestBodySize(0) should throw IllegalArgumentException")
    void maxRequestBodySizeZeroShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> builder.withMaxRequestBodySize(0));
      assertTrue(e.getMessage().contains("positive"),
          "Expected message to contain: positive");
    }

    @Test
    @DisplayName("withMaxRedirects(-1) should throw IllegalArgumentException")
    void maxRedirectsNegativeShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> builder.withMaxRedirects(-1));
      assertTrue(e.getMessage().contains("negative"),
          "Expected message to contain: negative");
    }

    @Test
    @DisplayName("withConnectTimeout(negative) should throw IllegalArgumentException")
    void connectTimeoutNegativeShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.withConnectTimeout(Duration.ofSeconds(-1)));
      assertTrue(e.getMessage().contains("negative"),
          "Expected message to contain: negative");
    }

    @Test
    @DisplayName("withMaxConnectionsPerHost(0) should throw IllegalArgumentException")
    void maxConnectionsPerHostZeroShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> builder.withMaxConnectionsPerHost(0));
      assertTrue(e.getMessage().contains("positive"),
          "Expected message to contain: positive");
    }

    @Test
    @DisplayName("withMaxResponseBodySize(-1) should throw IllegalArgumentException")
    void maxResponseBodySizeNegativeShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> builder.withMaxResponseBodySize(-1));
      assertTrue(e.getMessage().contains("positive"),
          "Expected message to contain: positive");
    }
  }

  @Nested
  @DisplayName("Builder Fluency")
  class BuilderFluency {

    @Test
    @DisplayName("Each setter should return the same builder reference")
    void settersShouldReturnSameBuilder() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();

      assertSame(builder, builder.allowHost("example.com"));
      assertSame(builder, builder.allowAllHosts());
      assertSame(builder, builder.blockHost("bad.com"));
      assertSame(builder, builder.withConnectTimeout(Duration.ofSeconds(5)));
      assertSame(builder, builder.withReadTimeout(Duration.ofSeconds(10)));
      assertSame(builder, builder.withWriteTimeout(Duration.ofSeconds(15)));
      assertSame(builder, builder.withMaxConnections(50));
      assertSame(builder, builder.withMaxConnectionsPerHost(5));
      assertSame(builder, builder.withMaxRequestBodySize(1024));
      assertSame(builder, builder.withMaxResponseBodySize(2048));
      assertSame(builder, builder.allowMethods("GET", "POST"));
      assertSame(builder, builder.requireHttps(true));
      assertSame(builder, builder.withCertificateValidation(true));
      assertSame(builder, builder.withHttp2(false));
      assertSame(builder, builder.withConnectionPooling(false));
      assertSame(builder, builder.followRedirects(false));
      assertSame(builder, builder.withMaxRedirects(3));
      assertSame(builder, builder.withUserAgent("test"));
    }

    @Test
    @DisplayName("Method chaining should build valid config")
    void methodChainingBuild() {
      final WasiHttpConfig config =
          new JniWasiHttpConfigBuilder()
              .allowHost("api.example.com")
              .blockHost("internal.example.com")
              .withConnectTimeout(Duration.ofSeconds(30))
              .withMaxConnections(100)
              .requireHttps(true)
              .withUserAgent("Wasmtime4J/1.0")
              .build();

      assertNotNull(config);
      assertTrue(
          config.getAllowedHosts().contains("api.example.com"),
          "Expected allowed hosts to contain: api.example.com");
      assertTrue(
          config.getBlockedHosts().contains("internal.example.com"),
          "Expected blocked hosts to contain: internal.example.com");
      assertTrue(config.isHttpsRequired());
      assertEquals(100, config.getMaxConnections().get());
      assertEquals("Wasmtime4J/1.0", config.getUserAgent().get());
    }

    @Test
    @DisplayName("build() should return non-null config")
    void buildShouldReturnNonNull() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().build();
      assertNotNull(config);
    }
  }

  @Nested
  @DisplayName("Collection Behavior")
  class CollectionBehavior {

    @Test
    @DisplayName("allowAllHosts() should add wildcard")
    void allowAllHostsShouldAddWildcard() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().allowAllHosts().build();
      assertTrue(
          config.getAllowedHosts().contains("*"),
          "Expected allowed hosts to contain: *");
    }

    @Test
    @DisplayName("blockHost after allowAllHosts should populate both sets")
    void blockHostAfterAllowAllShouldPopulateBothSets() {
      final WasiHttpConfig config =
          new JniWasiHttpConfigBuilder().allowAllHosts().blockHost("internal.com").build();

      assertTrue(
          config.getAllowedHosts().contains("*"),
          "Expected allowed hosts to contain: *");
      assertTrue(
          config.getBlockedHosts().contains("internal.com"),
          "Expected blocked hosts to contain: internal.com");
    }

    @Test
    @DisplayName("allowMethods should clear and set new methods")
    void allowMethodsShouldClearAndSet() {
      final WasiHttpConfig config =
          new JniWasiHttpConfigBuilder()
              .allowMethods("GET", "POST")
              .allowMethods("HEAD", "OPTIONS")
              .build();

      final List<String> methods = config.getAllowedMethods();
      assertEquals(2, methods.size(),
          "Second call to allowMethods should replace, not append");
      assertTrue(methods.contains("HEAD"),
          "Expected methods to contain: HEAD");
      assertTrue(methods.contains("OPTIONS"),
          "Expected methods to contain: OPTIONS");
      assertFalse(methods.contains("GET"),
          "Expected methods not to contain: GET");
      assertFalse(methods.contains("POST"),
          "Expected methods not to contain: POST");
    }

    @Test
    @DisplayName("allowHosts should filter null and empty entries")
    void allowHostsShouldFilterNullAndEmpty() {
      final WasiHttpConfig config =
          new JniWasiHttpConfigBuilder()
              .allowHosts(Arrays.asList("valid.com", null, "", "another.com"))
              .build();

      assertEquals(2, config.getAllowedHosts().size());
      assertTrue(config.getAllowedHosts().contains("valid.com"),
          "Expected hosts to contain: valid.com");
      assertTrue(config.getAllowedHosts().contains("another.com"),
          "Expected hosts to contain: another.com");
      assertFalse(config.getAllowedHosts().contains(""),
          "Expected hosts not to contain empty string");
      assertFalse(config.getAllowedHosts().contains(null),
          "Expected hosts not to contain null");
    }

    @Test
    @DisplayName("allowHosts with empty collection should not add any hosts")
    void allowHostsEmptyCollectionShouldNotAdd() {
      final WasiHttpConfig config =
          new JniWasiHttpConfigBuilder().allowHosts(Collections.emptyList()).build();

      assertTrue(config.getAllowedHosts().isEmpty());
    }
  }
}
