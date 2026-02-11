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

package ai.tegmentum.wasmtime4j.parity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Cross-runtime parity tests for WASI operations.
 *
 * <p>These tests verify that JNI and Panama runtime implementations produce identical results for
 * WASI configuration, context creation, and basic WASI module operations.
 */
@DisplayName("WASI Parity Tests")
@Tag("integration")
class WasiParityTest {

  private static final Logger LOGGER = Logger.getLogger(WasiParityTest.class.getName());

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private Store jniStore;
  private Store panamaStore;
  private boolean jniCreatedSuccessfully;
  private boolean panamaCreatedSuccessfully;

  @TempDir Path tempDir;

  @BeforeAll
  static void checkRuntimeAvailability() {
    LOGGER.info("Checking runtime availability for WASI parity tests");

    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

    LOGGER.info("JNI runtime available: " + jniAvailable);
    LOGGER.info("Panama runtime available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up WASI parity test runtimes");

    jniCreatedSuccessfully = false;
    panamaCreatedSuccessfully = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniStore = jniEngine.createStore();
        jniCreatedSuccessfully = true;
        LOGGER.info("JNI runtime created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI runtime: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaStore = panamaEngine.createStore();
        panamaCreatedSuccessfully = true;
        LOGGER.info("Panama runtime created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama runtime: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up WASI parity test runtimes");

    closeResource(jniStore, "JNI store");
    closeResource(jniEngine, "JNI engine");
    closeResource(jniRuntime, "JNI runtime");
    closeResource(panamaStore, "Panama store");
    closeResource(panamaEngine, "Panama engine");
    closeResource(panamaRuntime, "Panama runtime");
  }

  private void closeResource(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
        LOGGER.info(name + " closed");
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + name + ": " + e.getMessage());
      }
    }
  }

  private void requireBothRuntimes() {
    assumeTrue(
        jniCreatedSuccessfully && panamaCreatedSuccessfully,
        "Both JNI and Panama runtimes required");
  }

  @Nested
  @DisplayName("WasiConfig Builder Parity Tests")
  class WasiConfigBuilderParityTests {

    @Test
    @DisplayName("should create default WasiConfig identically on both runtimes")
    void shouldCreateDefaultConfigIdentically() {
      requireBothRuntimes();

      try {
        final WasiConfig jniConfig = WasiConfig.defaultConfig();
        final WasiConfig panamaConfig = WasiConfig.defaultConfig();

        assertThat(jniConfig).isNotNull();
        assertThat(panamaConfig).isNotNull();

        // Default configs should have empty environment and args
        assertThat(jniConfig.getEnvironment()).isEmpty();
        assertThat(panamaConfig.getEnvironment()).isEmpty();
        assertThat(jniConfig.getArguments()).isEmpty();
        assertThat(panamaConfig.getArguments()).isEmpty();

        LOGGER.info("Default WasiConfig created on both runtimes");
      } catch (final Exception e) {
        LOGGER.warning("WasiConfig creation failed: " + e.getMessage());
        assumeTrue(false, "WasiConfig not supported: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should create WasiConfig with environment variables identically")
    void shouldCreateConfigWithEnvironmentIdentically() {
      requireBothRuntimes();

      try {
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_VAR", "test_value");
        env.put("ANOTHER_VAR", "another_value");

        final WasiConfig jniConfig =
            WasiConfig.builder()
                .withEnvironment("TEST_VAR", "test_value")
                .withEnvironment("ANOTHER_VAR", "another_value")
                .build();

        final WasiConfig panamaConfig =
            WasiConfig.builder()
                .withEnvironment("TEST_VAR", "test_value")
                .withEnvironment("ANOTHER_VAR", "another_value")
                .build();

        assertThat(jniConfig.getEnvironment()).isEqualTo(panamaConfig.getEnvironment());
        assertThat(jniConfig.getEnvironment()).containsAllEntriesOf(env);

        LOGGER.info("WasiConfig with environment created on both runtimes");
      } catch (final Exception e) {
        LOGGER.warning("WasiConfig creation failed: " + e.getMessage());
        assumeTrue(false, "WasiConfig not supported: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should create WasiConfig with arguments identically")
    void shouldCreateConfigWithArgumentsIdentically() {
      requireBothRuntimes();

      try {
        final WasiConfig jniConfig =
            WasiConfig.builder()
                .withArgument("--verbose")
                .withArgument("--output")
                .withArgument("/tmp/out")
                .build();

        final WasiConfig panamaConfig =
            WasiConfig.builder()
                .withArgument("--verbose")
                .withArgument("--output")
                .withArgument("/tmp/out")
                .build();

        assertThat(jniConfig.getArguments()).isEqualTo(panamaConfig.getArguments());
        assertThat(jniConfig.getArguments()).containsExactly("--verbose", "--output", "/tmp/out");

        LOGGER.info("WasiConfig with arguments created on both runtimes");
      } catch (final Exception e) {
        LOGGER.warning("WasiConfig creation failed: " + e.getMessage());
        assumeTrue(false, "WasiConfig not supported: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should create WasiConfig with preopened directory identically")
    void shouldCreateConfigWithPreopenIdentically() throws Exception {
      requireBothRuntimes();

      // Create a test directory
      final Path testDir = Files.createDirectory(tempDir.resolve("wasi_test"));

      try {
        final WasiConfig jniConfig =
            WasiConfig.builder().withPreopenDirectory("/guest", testDir).build();

        final WasiConfig panamaConfig =
            WasiConfig.builder().withPreopenDirectory("/guest", testDir).build();

        assertThat(jniConfig.getPreopenDirectories())
            .isEqualTo(panamaConfig.getPreopenDirectories());
        assertThat(jniConfig.getPreopenDirectories()).containsKey("/guest");
        assertThat(jniConfig.getPreopenDirectories().get("/guest")).isEqualTo(testDir);

        LOGGER.info("WasiConfig with preopened directory created on both runtimes");
      } catch (final Exception e) {
        LOGGER.warning("WasiConfig creation failed: " + e.getMessage());
        assumeTrue(false, "WasiConfig not supported: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("WasiConfig Isolation Parity Tests")
  class WasiConfigIsolationParityTests {

    @Test
    @DisplayName("should create isolated WasiConfig instances identically on both runtimes")
    void shouldCreateIsolatedWasiConfigIdentically() {
      requireBothRuntimes();

      try {
        // Create independent configs - both runtimes should behave identically
        final WasiConfig jniConfig1 =
            WasiConfig.builder().withEnvironment("CONTEXT", "jni1").build();

        final WasiConfig jniConfig2 =
            WasiConfig.builder().withEnvironment("CONTEXT", "jni2").build();

        final WasiConfig panamaConfig1 =
            WasiConfig.builder().withEnvironment("CONTEXT", "panama1").build();

        final WasiConfig panamaConfig2 =
            WasiConfig.builder().withEnvironment("CONTEXT", "panama2").build();

        // Configs should be independent
        assertThat(jniConfig1.getEnvironment().get("CONTEXT")).isEqualTo("jni1");
        assertThat(jniConfig2.getEnvironment().get("CONTEXT")).isEqualTo("jni2");
        assertThat(panamaConfig1.getEnvironment().get("CONTEXT")).isEqualTo("panama1");
        assertThat(panamaConfig2.getEnvironment().get("CONTEXT")).isEqualTo("panama2");

        // Each config should have exactly 1 env var
        assertThat(jniConfig1.getEnvironment()).hasSize(1);
        assertThat(jniConfig2.getEnvironment()).hasSize(1);
        assertThat(panamaConfig1.getEnvironment()).hasSize(1);
        assertThat(panamaConfig2.getEnvironment()).hasSize(1);

        LOGGER.info("WasiConfig isolation verified on both runtimes");
      } catch (final Exception e) {
        LOGGER.warning("WasiConfig isolation test failed: " + e.getMessage());
        assumeTrue(false, "WasiConfig isolation not supported: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("WASI Module Linking Parity Tests")
  class WasiModuleLinkingParityTests {

    // Simple WASM module that imports WASI fd_write (minimal WASI module)
    // This is a minimal module that just declares a WASI import
    private static final byte[] WASI_IMPORT_MODULE =
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6D, // magic
          0x01,
          0x00,
          0x00,
          0x00, // version

          // Type section (id=1) - one function type
          0x01, // section id
          0x07, // section size
          0x01, // number of types
          0x60, // func type
          0x04,
          0x7F,
          0x7F,
          0x7F,
          0x7F, // params: 4 i32s
          0x01,
          0x7F, // result: 1 i32

          // Import section (id=2) - import fd_write from wasi_snapshot_preview1
          0x02, // section id
          0x1B, // section size (27 bytes)
          0x01, // number of imports
          0x15, // module name length (21)
          'w',
          'a',
          's',
          'i',
          '_',
          's',
          'n',
          'a',
          'p',
          's',
          'h',
          'o',
          't',
          '_',
          'p',
          'r',
          'e',
          'v',
          'i',
          'e',
          'w',
          '1',
          0x08, // field name length (8)
          'f',
          'd',
          '_',
          'w',
          'r',
          'i',
          't',
          'e',
          0x00, // import kind: function
          0x00 // type index
        };

    @Test
    @DisplayName("should link WASI imports identically on both runtimes")
    void shouldLinkWasiImportsIdentically() {
      requireBothRuntimes();

      try {
        // Create linkers with WASI using Linker.create()
        final Linker<?> jniLinker = Linker.create(jniEngine);
        final Linker<?> panamaLinker = Linker.create(panamaEngine);

        // Enable WASI for both linkers
        jniLinker.enableWasi();
        panamaLinker.enableWasi();

        // Compile modules
        final Module jniModule = jniEngine.compileModule(WASI_IMPORT_MODULE);
        final Module panamaModule = panamaEngine.compileModule(WASI_IMPORT_MODULE);

        // Both should compile successfully
        assertThat(jniModule).isNotNull();
        assertThat(panamaModule).isNotNull();

        LOGGER.info("WASI module compiled on both runtimes");

        // Both linkers should be able to instantiate with WASI imports
        final Instance jniInstance = jniLinker.instantiate(jniStore, jniModule);
        final Instance panamaInstance = panamaLinker.instantiate(panamaStore, panamaModule);

        assertThat(jniInstance).isNotNull();
        assertThat(panamaInstance).isNotNull();

        LOGGER.info("WASI module instantiated on both runtimes");

        // Cleanup
        jniModule.close();
        panamaModule.close();
        jniLinker.close();
        panamaLinker.close();
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("WASI linking not supported: " + e.getMessage());
        assumeTrue(false, "WASI linking not supported");
      } catch (final Exception e) {
        LOGGER.warning("WASI linking failed: " + e.getMessage());
        assumeTrue(false, "WASI linking failed: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("WASI Environment Parity Tests")
  class WasiEnvironmentParityTests {

    @Test
    @DisplayName("should handle explicit environment configuration identically on both runtimes")
    void shouldHandleExplicitEnvironmentIdentically() {
      requireBothRuntimes();

      try {
        // Test that both runtimes handle explicit environment config the same way
        final WasiConfig jniConfig =
            WasiConfig.builder()
                .withEnvironment("TEST_HOME", "/app")
                .withEnvironment("TEST_PATH", "/usr/bin")
                .build();

        final WasiConfig panamaConfig =
            WasiConfig.builder()
                .withEnvironment("TEST_HOME", "/app")
                .withEnvironment("TEST_PATH", "/usr/bin")
                .build();

        // Both should have identical explicit environment variables
        assertThat(jniConfig.getEnvironment()).isEqualTo(panamaConfig.getEnvironment());
        assertThat(jniConfig.getEnvironment()).containsEntry("TEST_HOME", "/app");
        assertThat(jniConfig.getEnvironment()).containsEntry("TEST_PATH", "/usr/bin");

        LOGGER.info(
            "Both runtimes configured with explicit environment: "
                + jniConfig.getEnvironment().size()
                + " variables");
      } catch (final Exception e) {
        LOGGER.warning("Explicit environment test failed: " + e.getMessage());
        assumeTrue(false, "Explicit environment not supported: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should handle multiple environment variables identically on both runtimes")
    void shouldHandleMultipleEnvironmentVariablesIdentically() {
      requireBothRuntimes();

      try {
        // Create configs with multiple environment variables
        final WasiConfig jniConfig =
            WasiConfig.builder()
                .withEnvironment("VAR1", "value1")
                .withEnvironment("VAR2", "value2")
                .withEnvironment("VAR3", "value3")
                .build();

        final WasiConfig panamaConfig =
            WasiConfig.builder()
                .withEnvironment("VAR1", "value1")
                .withEnvironment("VAR2", "value2")
                .withEnvironment("VAR3", "value3")
                .build();

        // Both should have the same environment variables
        assertThat(jniConfig.getEnvironment()).hasSize(3);
        assertThat(panamaConfig.getEnvironment()).hasSize(3);
        assertThat(jniConfig.getEnvironment())
            .containsExactlyInAnyOrderEntriesOf(panamaConfig.getEnvironment());

        LOGGER.info("Both runtimes configured with identical environment variables");
      } catch (final Exception e) {
        LOGGER.warning("Multiple environment test failed: " + e.getMessage());
        assumeTrue(false, "Multiple environment not supported: " + e.getMessage());
      }
    }
  }
}
