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
package ai.tegmentum.wasmtime4j.wasmtime.generated.componentmodelasync;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.jni.JniComponent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for WebAssembly Component Model Async features from Wasmtime 41.0.0.
 *
 * <p>These tests validate that the component model async WAST files from the upstream Wasmtime test
 * suite are properly packaged and can be loaded for testing.
 *
 * <p>Test files are sourced from: tests/misc_testsuite/component-model/async/
 *
 * <p>Note: Full compilation of these component model async WAST files requires enabling the
 * COMPONENT_MODEL_ASYNC and COMPONENT_MODEL_ASYNC_BUILTINS features in the engine configuration.
 * These tests currently validate the test resources are present and properly formatted.
 *
 * @since 1.0.0
 */
@DisplayName("Component Model Async Tests (Wasmtime 41.0.0)")
public final class ComponentModelAsyncTest {

  private static final Logger LOGGER = Logger.getLogger(ComponentModelAsyncTest.class.getName());

  private static final String RESOURCE_PATH =
      "/ai/tegmentum/wasmtime4j/wasmtime/generated/component-model-async/";

  private static boolean componentEngineAvailable = false;

  /** All component model async test files from Wasmtime 41.0.0. */
  private static final String[] TEST_FILES = {
    "async-builtins.wast",
    "backpressure-deadlock.wast",
    "backpressure-overflow.wast",
    "error-context.wast",
    "fused.wast",
    "future-cancel-read-dropped.wast",
    "future-cancel-write-completed.wast",
    "future-cancel-write-dropped.wast",
    "future-read.wast",
    "futures-must-write.wast",
    "futures.wast",
    "intra-futures.wast",
    "intra-streams.wast",
    "lift.wast",
    "lower.wast",
    "many-params-with-retptr.wast",
    "partial-stream-copies.wast",
    "stackful.wast",
    "streams.wast",
    "subtask-wait.wast",
    "sync-streams.wast",
    "task-builtins.wast",
    "trap-if-done.wast",
    "wait-forever.wast",
    "wait-forever2.wast",
    "yield-when-cancelled.wast"
  };

  @BeforeAll
  static void checkComponentEngineAvailable() {
    try {
      // Try to create a component engine to check if native implementation works
      try (JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
        componentEngineAvailable = engine != null && engine.isValid();
        if (componentEngineAvailable) {
          LOGGER.info("Component engine is available");
        }
      }
    } catch (final Exception e) {
      componentEngineAvailable = false;
      LOGGER.warning("Component engine not available: " + e.getMessage());
    }
  }

  private static void assumeComponentEngineAvailable() {
    assumeTrue(componentEngineAvailable, "Component engine not available - skipping test.");
  }

  /**
   * Provides test arguments for each WAST file.
   *
   * @return stream of test arguments containing file name and display name
   */
  static Stream<Arguments> provideTestFiles() {
    return Stream.of(TEST_FILES)
        .map(file -> Arguments.of(file, file.replace(".wast", "").replace("-", " ")));
  }

  /**
   * Loads a WAST resource file as a string.
   *
   * @param filename the name of the WAST file
   * @return the file content
   * @throws IOException if the file cannot be read
   */
  private static String loadWastResource(final String filename) throws IOException {
    final String path = RESOURCE_PATH + filename;
    try (InputStream is = ComponentModelAsyncTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Tests that a component model async WAST file can be loaded and parsed.
   *
   * <p>This validates that the WAST file is present in test resources and contains valid component
   * model async constructs.
   *
   * @param filename the WAST file to test
   * @param displayName the display name for the test
   */
  @ParameterizedTest(name = "{1}")
  @MethodSource("provideTestFiles")
  @DisplayName("Load component async WAST")
  void testLoadComponentAsyncWast(final String filename, final String displayName)
      throws Exception {
    final String wastContent = loadWastResource(filename);
    assertNotNull(wastContent, "WAST content should not be null");
    assertTrue(wastContent.length() > 0, "WAST content should not be empty");

    // Verify the file contains component model async directives
    assertTrue(
        wastContent.contains(";;! component_model_async = true")
            || wastContent.contains("(component"),
        "WAST file should contain component model async directives or component definitions");

    LOGGER.info(
        String.format(
            "Successfully loaded component async WAST: %s (%d bytes)",
            filename, wastContent.length()));
  }

  /**
   * Tests that component definitions can be extracted from WAST files.
   *
   * <p>This validates that the WAST files contain well-formed component definitions that can be
   * parsed for compilation testing.
   *
   * @param filename the WAST file to analyze
   * @param displayName the display name for the test
   */
  @ParameterizedTest(name = "{1}")
  @MethodSource("provideTestFiles")
  @DisplayName("Extract component definitions")
  void testExtractComponentDefinitions(final String filename, final String displayName)
      throws Exception {
    final String wastContent = loadWastResource(filename);

    // Count component definitions in the file
    int componentCount = 0;
    int searchStart = 0;
    while (true) {
      final int componentStart = wastContent.indexOf("(component", searchStart);
      if (componentStart < 0) {
        break;
      }

      // Find matching closing paren
      int depth = 0;
      int componentEnd = -1;
      for (int i = componentStart; i < wastContent.length(); i++) {
        final char c = wastContent.charAt(i);
        if (c == '(') {
          depth++;
        } else if (c == ')') {
          depth--;
          if (depth == 0) {
            componentEnd = i + 1;
            break;
          }
        }
      }

      if (componentEnd > componentStart) {
        componentCount++;
        searchStart = componentEnd;
      } else {
        break;
      }
    }

    assertTrue(componentCount > 0, "WAST file should contain at least one component definition");

    LOGGER.info(String.format("Found %d component definition(s) in %s", componentCount, filename));
  }

  /**
   * Tests that the component engine can be created for async feature testing.
   *
   * <p>This validates that the native component engine infrastructure is available, which is
   * required for component model async feature testing.
   *
   * @param filename the WAST file (for parameterized context)
   * @param displayName the display name for the test
   */
  @ParameterizedTest(name = "{1}")
  @MethodSource("provideTestFiles")
  @DisplayName("Component engine availability")
  void testComponentEngineAvailable(final String filename, final String displayName)
      throws Exception {
    assumeComponentEngineAvailable();

    try (JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      assertNotNull(engine, "Component engine should be created");
      assertTrue(engine.isValid(), "Component engine should be valid");

      LOGGER.info(String.format("Component engine ready for async testing: %s", filename));
    }
  }
}
