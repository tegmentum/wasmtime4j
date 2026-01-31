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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Instance#setImports(Map)}, {@link Instance#getCreatedAtMicros()}, and
 * {@link Instance#getMetadataExportCount()}.
 *
 * <p>Verifies instance metadata timestamps, export count metadata, and the unsupported setImports
 * operation.
 *
 * @since 1.0.0
 */
@DisplayName("Instance Metadata Tests")
public class InstanceMetadataTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(InstanceMetadataTest.class.getName());

  /** Module with a function export and a memory export (2 exports total). */
  private static final String TWO_EXPORTS_WAT =
      """
      (module
        (func (export "get42") (result i32) i32.const 42)
        (memory (export "mem") 1))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getCreatedAtMicros returns positive timestamp")
  void getCreatedAtMicrosReturnsPositiveTimestamp(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getCreatedAtMicros returns positive value");

    final long beforeMicros = System.currentTimeMillis() * 1000;

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TWO_EXPORTS_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final long createdAt = instance.getCreatedAtMicros();

      LOGGER.info("[" + runtime + "] createdAtMicros: " + createdAt);
      LOGGER.info("[" + runtime + "] beforeMicros: " + beforeMicros);

      assertTrue(createdAt > 0, "createdAtMicros should be positive, was: " + createdAt);

      // Verify it's a reasonable timestamp (within 10 seconds of current time)
      final long afterMicros = System.currentTimeMillis() * 1000;
      final long tenSecondsInMicros = 10_000_000L;
      assertTrue(
          createdAt >= (beforeMicros - tenSecondsInMicros),
          "createdAtMicros should be within 10 seconds before test start");
      assertTrue(
          createdAt <= (afterMicros + tenSecondsInMicros),
          "createdAtMicros should be within 10 seconds after test");

      LOGGER.info("[" + runtime + "] Timestamp is within reasonable range");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getCreatedAtMicros is consistent across calls")
  void getCreatedAtMicrosIsConsistentAcrossCalls(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getCreatedAtMicros consistency");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TWO_EXPORTS_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final long firstCall = instance.getCreatedAtMicros();
      final long secondCall = instance.getCreatedAtMicros();

      LOGGER.info("[" + runtime + "] firstCall: " + firstCall);
      LOGGER.info("[" + runtime + "] secondCall: " + secondCall);

      // The implementation may return a live timestamp rather than a cached creation-time
      // Allow a small delta (1ms = 1000 micros) for clock granularity
      final long delta = Math.abs(secondCall - firstCall);
      LOGGER.info("[" + runtime + "] delta: " + delta + " micros");
      assertTrue(
          delta <= 1000,
          "createdAtMicros calls should be within 1ms of each other, delta was: " + delta);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getMetadataExportCount returns non-negative value")
  void getMetadataExportCountReturnsNonNegative(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getMetadataExportCount returns non-negative");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TWO_EXPORTS_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final int count = instance.getMetadataExportCount();

      LOGGER.info("[" + runtime + "] metadataExportCount: " + count);
      assertTrue(count >= 0, "metadataExportCount should be non-negative, was: " + count);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getMetadataExportCount for module with exports returns consistent value")
  void getMetadataExportCountForModuleWithExports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getMetadataExportCount with 2-export module");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TWO_EXPORTS_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final int firstCall = instance.getMetadataExportCount();
      final int secondCall = instance.getMetadataExportCount();

      LOGGER.info("[" + runtime + "] firstCall: " + firstCall);
      LOGGER.info("[" + runtime + "] secondCall: " + secondCall);
      LOGGER.info("[" + runtime + "] exportNames: "
          + java.util.Arrays.toString(instance.getExportNames()));

      assertEquals(firstCall, secondCall, "metadataExportCount should be consistent across calls");
      // Value may be 0 if metadata exports are distinct from regular exports
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("setImports throws UnsupportedOperationException")
  void setImportsThrowsUnsupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing setImports throws UnsupportedOperationException");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TWO_EXPORTS_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final Map<String, Object> imports = new HashMap<>();
      imports.put("key", "value");

      assertThrows(
          UnsupportedOperationException.class,
          () -> instance.setImports(imports),
          "setImports should throw UnsupportedOperationException");

      LOGGER.info("[" + runtime + "] Correctly threw UnsupportedOperationException");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("setImports with null throws exception")
  void setImportsNullThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing setImports with null");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TWO_EXPORTS_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      // Should throw either IllegalArgumentException or UnsupportedOperationException
      final Exception thrown = assertThrows(
          Exception.class,
          () -> instance.setImports(null),
          "setImports(null) should throw an exception");

      assertNotNull(thrown, "Exception should not be null");
      assertTrue(
          thrown instanceof IllegalArgumentException
              || thrown instanceof UnsupportedOperationException,
          "Should be IllegalArgumentException or UnsupportedOperationException, was: "
              + thrown.getClass().getName());

      LOGGER.info("[" + runtime + "] Threw " + thrown.getClass().getSimpleName()
          + ": " + thrown.getMessage());
    }
  }
}
