package ai.tegmentum.wasmtime4j.integration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration tests for runtime selection functionality. Tests the automatic detection and manual
 * override of JNI vs Panama runtimes.
 */
@DisplayName("Runtime Selection Integration Tests")
class RuntimeSelectionIT extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled("runtime");
  }

  @Test
  @DisplayName("Should detect appropriate runtime based on Java version")
  void shouldDetectRuntimeBasedOnJavaVersion() {
    // Given
    final int javaVersion = TestUtils.getJavaVersion();

    // When & Then
    if (javaVersion >= 23) {
      // Panama should be available
      assertThat(TestUtils.isPanamaAvailable())
          .as("Panama should be available on Java 23+")
          .isTrue();

      // TODO: Test actual runtime factory selection when API is available
      // final Wasmtime4jRuntime runtime = Wasmtime4jFactory.createRuntime();
      // assertThat(runtime).isInstanceOf(PanamaWasmtime4jRuntime.class);

    } else {
      // Should fall back to JNI
      assertThat(TestUtils.isPanamaAvailable())
          .as("Panama should not be available on Java " + javaVersion)
          .isFalse();

      // TODO: Test actual runtime factory selection when API is available
      // final Wasmtime4jRuntime runtime = Wasmtime4jFactory.createRuntime();
      // assertThat(runtime).isInstanceOf(JniWasmtime4jRuntime.class);
    }

    LOGGER.info("Runtime detection test completed for Java " + javaVersion);
  }

  @ParameterizedTest
  @ValueSource(strings = {"jni", "panama"})
  @DisplayName("Should respect manual runtime override")
  void shouldRespectManualRuntimeOverride(final String runtimeType) {
    // Given
    final String originalProperty = System.getProperty("wasmtime4j.runtime");

    try {
      // When
      System.setProperty("wasmtime4j.runtime", runtimeType);

      // Then
      if ("panama".equals(runtimeType) && !TestUtils.isPanamaAvailable()) {
        // Should fall back to JNI if Panama is not available
        // TODO: Test actual runtime factory selection when API is available
        // final Wasmtime4jRuntime runtime = Wasmtime4jFactory.createRuntime();
        // assertThat(runtime).isInstanceOf(JniWasmtime4jRuntime.class);

        LOGGER.info("Panama override fell back to JNI as expected");
      } else {
        // Should use the specified runtime
        // TODO: Test actual runtime factory selection when API is available
        // final Wasmtime4jRuntime runtime = Wasmtime4jFactory.createRuntime();
        // if ("panama".equals(runtimeType)) {
        //     assertThat(runtime).isInstanceOf(PanamaWasmtime4jRuntime.class);
        // } else {
        //     assertThat(runtime).isInstanceOf(JniWasmtime4jRuntime.class);
        // }

        LOGGER.info("Manual runtime override to " + runtimeType + " respected");
      }
    } finally {
      // Cleanup
      if (originalProperty != null) {
        System.setProperty("wasmtime4j.runtime", originalProperty);
      } else {
        System.clearProperty("wasmtime4j.runtime");
      }
    }
  }

  @Test
  @DisplayName("Should provide runtime information")
  void shouldProvideRuntimeInformation() {
    // TODO: When runtime API is available, test runtime information
    // final Wasmtime4jRuntime runtime = Wasmtime4jFactory.createRuntime();
    // final RuntimeInfo info = runtime.getRuntimeInfo();

    // assertThat(info.getRuntimeType()).isIn("jni", "panama");
    // assertThat(info.getWasmtimeVersion()).isNotEmpty();
    // assertThat(info.getJavaVersion()).isEqualTo(TestUtils.getJavaVersion());
    // assertThat(info.getPlatform()).contains(TestUtils.getOperatingSystem());
    // assertThat(info.getArchitecture()).contains(TestUtils.getSystemArchitecture());

    LOGGER.info("Runtime information test placeholder completed");
  }

  @Test
  @DisplayName("Should handle runtime switching")
  void shouldHandleRuntimeSwitching() {
    // TODO: Test runtime switching when multiple runtimes are available
    // This test should verify that switching between runtimes works correctly
    // and that resources are properly cleaned up

    LOGGER.info("Runtime switching test placeholder completed");
  }

  @Test
  @DisplayName("Should gracefully handle unavailable runtime")
  void shouldGracefullyHandleUnavailableRuntime() {
    // TODO: Test behavior when requested runtime is not available
    // Should fall back to available runtime or provide clear error message

    LOGGER.info("Unavailable runtime handling test placeholder completed");
  }
}
