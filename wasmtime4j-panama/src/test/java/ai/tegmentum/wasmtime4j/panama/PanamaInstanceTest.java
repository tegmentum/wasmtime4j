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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link PanamaInstance}, {@link PanamaInstanceGlobal}, and {@link
 * PanamaInstancePre}.
 *
 * <p>These tests verify validation, state management, and behavioral contracts of the Panama
 * Instance classes.
 */
@DisplayName("Panama Instance Tests")
class PanamaInstanceTest {

  @Nested
  @DisplayName("PanamaInstance Validation Tests")
  class PanamaInstanceValidationTests {

    @Test
    @DisplayName("PanamaInstance constructor should reject null module")
    void panamaInstanceConstructorShouldRejectNullModule() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaInstance(null, null));
      assertTrue(
          ex.getMessage().contains("Module cannot be null"),
          "Expected message to contain 'Module cannot be null': " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("PanamaInstancePre Validation Tests")
  class PanamaInstancePreValidationTests {

    @Test
    @DisplayName("PanamaInstancePre constructor should reject null native instance")
    void panamaInstancePreConstructorShouldRejectNullNativeInstance() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> new PanamaInstancePre(null, null, null));
      assertTrue(
          ex.getMessage().contains("Native InstancePre cannot be null"),
          "Expected message to contain 'Native InstancePre cannot be null': " + ex.getMessage());
    }

    @Test
    @DisplayName("PanamaInstancePre constructor should reject MemorySegment.NULL")
    void panamaInstancePreConstructorShouldRejectNullSegment() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new PanamaInstancePre(MemorySegment.NULL, null, null));
      assertTrue(
          ex.getMessage().contains("Native InstancePre cannot be null"),
          "Expected message to contain 'Native InstancePre cannot be null': " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("PreInstantiationStatistics Tests")
  class PreInstantiationStatisticsTests {

    @Test
    @DisplayName("PreInstantiationStatistics builder should support creationTime")
    void preInstantiationStatisticsBuilderShouldSupportCreationTime() {
      final ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics stats =
          ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics.builder()
              .creationTime(Instant.now())
              .build();
      assertNotNull(stats, "PreInstantiationStatistics should not be null");
    }

    @Test
    @DisplayName("PreInstantiationStatistics builder should support preparationTime")
    void preInstantiationStatisticsBuilderShouldSupportPreparationTime() {
      final ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics stats =
          ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics.builder()
              .preparationTime(Duration.ofMillis(100))
              .build();
      assertNotNull(stats, "PreInstantiationStatistics should not be null");
    }

    @Test
    @DisplayName("PreInstantiationStatistics builder should support instancesCreated")
    void preInstantiationStatisticsBuilderShouldSupportInstancesCreated() {
      final ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics stats =
          ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics.builder()
              .instancesCreated(5L)
              .build();
      assertNotNull(stats, "PreInstantiationStatistics should not be null");
    }

    @Test
    @DisplayName("PreInstantiationStatistics builder should support averageInstantiationTime")
    void preInstantiationStatisticsBuilderShouldSupportAverageInstantiationTime() {
      final ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics stats =
          ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics.builder()
              .averageInstantiationTime(Duration.ofNanos(50000))
              .build();
      assertNotNull(stats, "PreInstantiationStatistics should not be null");
    }
  }

  /** Close safety tests for PanamaInstance requiring native library. */
  @Nested
  @DisplayName("Closed Instance Detection Tests")
  class ClosedInstanceDetectionTests {

    private static final Logger CLOSE_LOGGER = Logger.getLogger("ClosedInstanceDetectionTests");

    private static boolean nativeAvailable;

    static {
      try {
        new PanamaEngine().close();
        nativeAvailable = true;
      } catch (final Exception | UnsatisfiedLinkError e) {
        nativeAvailable = false;
      }
    }

    @Test
    @DisplayName("method on closed instance should throw IllegalStateException")
    void methodOnClosedInstanceShouldThrow() throws Exception {
      assumeTrue(nativeAvailable, "Native library not available");

      final Path wasmPath =
          Paths.get(getClass().getClassLoader().getResource("wasm/exports-test.wasm").toURI());
      final byte[] wasmBytes = Files.readAllBytes(wasmPath);

      final PanamaEngine engine = new PanamaEngine();
      final PanamaStore store = new PanamaStore(engine);
      final PanamaModule module = new PanamaModule(engine, wasmBytes);
      final PanamaInstance instance = new PanamaInstance(module, store);

      instance.close();
      CLOSE_LOGGER.info("Instance closed, attempting operations");

      assertThrows(
          IllegalStateException.class,
          () -> instance.getFunction("add"),
          "getFunction() on closed instance should throw IllegalStateException");
      assertThrows(
          IllegalStateException.class,
          () -> instance.getMemory("memory"),
          "getMemory() on closed instance should throw IllegalStateException");
      CLOSE_LOGGER.info("IllegalStateException thrown as expected for closed instance operations");

      store.close();
      module.close();
      engine.close();
    }

    @Test
    @DisplayName("double close should be safe")
    void doubleCloseShouldBeSafe() throws Exception {
      assumeTrue(nativeAvailable, "Native library not available");

      final Path wasmPath =
          Paths.get(getClass().getClassLoader().getResource("wasm/exports-test.wasm").toURI());
      final byte[] wasmBytes = Files.readAllBytes(wasmPath);

      final PanamaEngine engine = new PanamaEngine();
      final PanamaStore store = new PanamaStore(engine);
      final PanamaModule module = new PanamaModule(engine, wasmBytes);
      final PanamaInstance instance = new PanamaInstance(module, store);

      instance.close();
      CLOSE_LOGGER.info("First close completed");

      assertDoesNotThrow(instance::close, "Second close should not throw");
      CLOSE_LOGGER.info("Second close completed without exception");

      assertThrows(
          IllegalStateException.class,
          () -> instance.getFunction("add"),
          "getFunction() after double close should still throw");
      CLOSE_LOGGER.info("IllegalStateException confirmed after double close");

      store.close();
      module.close();
      engine.close();
    }
  }
}
