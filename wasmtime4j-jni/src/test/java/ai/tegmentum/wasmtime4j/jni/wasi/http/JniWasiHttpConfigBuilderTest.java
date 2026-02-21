package ai.tegmentum.wasmtime4j.jni.wasi.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfigBuilder;
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
      assertThat(config.getAllowedHosts())
          .as("Default config should have no allowed hosts")
          .isEmpty();
    }

    @Test
    @DisplayName("New builder should produce config with empty blocked hosts")
    void newBuilderShouldHaveEmptyBlockedHosts() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().build();
      assertThat(config.getBlockedHosts())
          .as("Default config should have no blocked hosts")
          .isEmpty();
    }

    @Test
    @DisplayName("New builder should produce config with empty allowed methods")
    void newBuilderShouldHaveEmptyAllowedMethods() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().build();
      assertThat(config.getAllowedMethods())
          .as("Default config should have no allowed methods")
          .isEmpty();
    }

    @Test
    @DisplayName("Boolean defaults should match expected values")
    void booleanDefaultsShouldMatchExpected() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().build();

      assertThat(config.isHttpsRequired())
          .as("httpsRequired default should be false")
          .isFalse();
      assertThat(config.isCertificateValidationEnabled())
          .as("certificateValidation default should be true")
          .isTrue();
      assertThat(config.isHttp2Enabled())
          .as("http2 default should be true")
          .isTrue();
      assertThat(config.isConnectionPoolingEnabled())
          .as("connectionPooling default should be true")
          .isTrue();
      assertThat(config.isFollowRedirects())
          .as("followRedirects default should be true")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Null Validation")
  class NullValidation {

    @Test
    @DisplayName("allowHost(null) should throw IllegalArgumentException")
    void allowHostNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.allowHost(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("null");
    }

    @Test
    @DisplayName("allowHost(\"\") should throw IllegalArgumentException")
    void allowHostEmptyShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.allowHost(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("blockHost(null) should throw IllegalArgumentException")
    void blockHostNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.blockHost(null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("withConnectTimeout(null) should throw NullPointerException")
    void connectTimeoutNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.withConnectTimeout(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("allowHosts(null) should throw NullPointerException")
    void allowHostsNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.allowHosts(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("allowMethods(null) should throw NullPointerException")
    void allowMethodsNullShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.allowMethods((String[]) null))
          .isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  @DisplayName("Negative Value Validation")
  class NegativeValueValidation {

    @Test
    @DisplayName("withMaxConnections(0) should throw IllegalArgumentException")
    void maxConnectionsZeroShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.withMaxConnections(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("withMaxConnections(-1) should throw IllegalArgumentException")
    void maxConnectionsNegativeShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.withMaxConnections(-1))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("withMaxRequestBodySize(0) should throw IllegalArgumentException")
    void maxRequestBodySizeZeroShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.withMaxRequestBodySize(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("withMaxRedirects(-1) should throw IllegalArgumentException")
    void maxRedirectsNegativeShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.withMaxRedirects(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("withConnectTimeout(negative) should throw IllegalArgumentException")
    void connectTimeoutNegativeShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.withConnectTimeout(Duration.ofSeconds(-1)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("withMaxConnectionsPerHost(0) should throw IllegalArgumentException")
    void maxConnectionsPerHostZeroShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.withMaxConnectionsPerHost(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("withMaxResponseBodySize(-1) should throw IllegalArgumentException")
    void maxResponseBodySizeNegativeShouldThrow() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
      assertThatThrownBy(() -> builder.withMaxResponseBodySize(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("positive");
    }
  }

  @Nested
  @DisplayName("Builder Fluency")
  class BuilderFluency {

    @Test
    @DisplayName("Each setter should return the same builder reference")
    void settersShouldReturnSameBuilder() {
      final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();

      assertThat(builder.allowHost("example.com")).isSameAs(builder);
      assertThat(builder.allowAllHosts()).isSameAs(builder);
      assertThat(builder.blockHost("bad.com")).isSameAs(builder);
      assertThat(builder.withConnectTimeout(Duration.ofSeconds(5))).isSameAs(builder);
      assertThat(builder.withReadTimeout(Duration.ofSeconds(10))).isSameAs(builder);
      assertThat(builder.withWriteTimeout(Duration.ofSeconds(15))).isSameAs(builder);
      assertThat(builder.withMaxConnections(50)).isSameAs(builder);
      assertThat(builder.withMaxConnectionsPerHost(5)).isSameAs(builder);
      assertThat(builder.withMaxRequestBodySize(1024)).isSameAs(builder);
      assertThat(builder.withMaxResponseBodySize(2048)).isSameAs(builder);
      assertThat(builder.allowMethods("GET", "POST")).isSameAs(builder);
      assertThat(builder.requireHttps(true)).isSameAs(builder);
      assertThat(builder.withCertificateValidation(true)).isSameAs(builder);
      assertThat(builder.withHttp2(false)).isSameAs(builder);
      assertThat(builder.withConnectionPooling(false)).isSameAs(builder);
      assertThat(builder.followRedirects(false)).isSameAs(builder);
      assertThat(builder.withMaxRedirects(3)).isSameAs(builder);
      assertThat(builder.withUserAgent("test")).isSameAs(builder);
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

      assertThat(config).isNotNull();
      assertThat(config.getAllowedHosts()).contains("api.example.com");
      assertThat(config.getBlockedHosts()).contains("internal.example.com");
      assertThat(config.isHttpsRequired()).isTrue();
      assertThat(config.getMaxConnections().get()).isEqualTo(100);
      assertThat(config.getUserAgent().get()).isEqualTo("Wasmtime4J/1.0");
    }

    @Test
    @DisplayName("build() should return non-null config")
    void buildShouldReturnNonNull() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().build();
      assertThat(config).isNotNull();
    }
  }

  @Nested
  @DisplayName("Collection Behavior")
  class CollectionBehavior {

    @Test
    @DisplayName("allowAllHosts() should add wildcard")
    void allowAllHostsShouldAddWildcard() {
      final WasiHttpConfig config = new JniWasiHttpConfigBuilder().allowAllHosts().build();
      assertThat(config.getAllowedHosts()).contains("*");
    }

    @Test
    @DisplayName("blockHost after allowAllHosts should populate both sets")
    void blockHostAfterAllowAllShouldPopulateBothSets() {
      final WasiHttpConfig config =
          new JniWasiHttpConfigBuilder()
              .allowAllHosts()
              .blockHost("internal.com")
              .build();

      assertThat(config.getAllowedHosts()).contains("*");
      assertThat(config.getBlockedHosts()).contains("internal.com");
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
      assertThat(methods)
          .as("Second call to allowMethods should replace, not append")
          .hasSize(2)
          .contains("HEAD", "OPTIONS")
          .doesNotContain("GET", "POST");
    }

    @Test
    @DisplayName("allowHosts should filter null and empty entries")
    void allowHostsShouldFilterNullAndEmpty() {
      final WasiHttpConfig config =
          new JniWasiHttpConfigBuilder()
              .allowHosts(Arrays.asList("valid.com", null, "", "another.com"))
              .build();

      assertThat(config.getAllowedHosts())
          .hasSize(2)
          .contains("valid.com", "another.com")
          .doesNotContain("", null);
    }

    @Test
    @DisplayName("allowHosts with empty collection should not add any hosts")
    void allowHostsEmptyCollectionShouldNotAdd() {
      final WasiHttpConfig config =
          new JniWasiHttpConfigBuilder()
              .allowHosts(Collections.emptyList())
              .build();

      assertThat(config.getAllowedHosts()).isEmpty();
    }
  }
}
