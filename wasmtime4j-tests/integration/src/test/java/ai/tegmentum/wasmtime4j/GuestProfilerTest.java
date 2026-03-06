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
package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.debug.GuestProfiler;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for {@link GuestProfiler}.
 *
 * <p>Validates that guest profilers can be created, sample, and produce profile data across both
 * JNI and Panama implementations.
 */
class GuestProfilerTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(GuestProfilerTest.class.getName());

  private static final String SIMPLE_WAT =
      "(module\n" + "  (func (export \"get42\") (result i32) i32.const 42)\n" + ")";

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCreateGuestProfiler(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing createGuestProfiler");
    try (Engine engine = Engine.create()) {
      final Module module = Module.compile(engine, SIMPLE_WAT.getBytes(StandardCharsets.UTF_8));

      final GuestProfiler profiler =
          engine.createGuestProfiler("test-module", Duration.ofMillis(1), Map.of("main", module));
      assertThat(profiler).as("createGuestProfiler should return non-null").isNotNull();
      assertThat(profiler.isActive()).as("New profiler should be active").isTrue();

      profiler.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testGuestProfilerFinishProducesJson(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing GuestProfiler finish produces JSON");
    try (Engine engine = Engine.create()) {
      final Module module = Module.compile(engine, SIMPLE_WAT.getBytes(StandardCharsets.UTF_8));

      try (GuestProfiler profiler =
          engine.createGuestProfiler("test", Duration.ofMillis(1), Map.of("main", module))) {

        // Finish without any samples — should still produce valid profile JSON
        final byte[] profileData = profiler.finish();
        assertThat(profileData)
            .as("Profile data should not be null or empty")
            .isNotNull()
            .isNotEmpty();

        // Profile data should be valid JSON (starts with '{')
        final String json = new String(profileData, StandardCharsets.UTF_8);
        LOGGER.info("[" + runtime + "] Profile JSON length: " + json.length() + " chars");
        assertThat(json).as("Profile should be JSON").startsWith("{");

        assertThat(profiler.isActive()).as("Profiler should be inactive after finish").isFalse();
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testGuestProfilerSampleAndFinish(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing GuestProfiler sample and finish");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {
      final Module module = Module.compile(engine, SIMPLE_WAT.getBytes(StandardCharsets.UTF_8));

      try (GuestProfiler profiler =
          engine.createGuestProfiler("test", Duration.ofMillis(1), Map.of("main", module))) {

        // Take a sample
        profiler.sample(store, Duration.ZERO);

        // Finish and verify we get profile data
        final byte[] profileData = profiler.finish();
        assertThat(profileData).as("Profile data after sample should not be empty").isNotEmpty();

        final String json = new String(profileData, StandardCharsets.UTF_8);
        assertThat(json).as("Profile should be valid JSON").startsWith("{");
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testGuestProfilerNullArgumentsThrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing GuestProfiler null argument validation");
    try (Engine engine = Engine.create()) {
      final Module module = Module.compile(engine, SIMPLE_WAT.getBytes(StandardCharsets.UTF_8));

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.createGuestProfiler(null, Duration.ofMillis(1), Map.of("m", module)),
          "Null moduleName should throw");

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.createGuestProfiler("test", null, Map.of("m", module)),
          "Null interval should throw");

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.createGuestProfiler("test", Duration.ofMillis(1), null),
          "Null modules should throw");

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.createGuestProfiler("test", Duration.ofMillis(1), Map.of()),
          "Empty modules should throw");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testGuestProfilerCloseIsIdempotent(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing GuestProfiler double close");
    try (Engine engine = Engine.create()) {
      final Module module = Module.compile(engine, SIMPLE_WAT.getBytes(StandardCharsets.UTF_8));

      final GuestProfiler profiler =
          engine.createGuestProfiler("test", Duration.ofMillis(1), Map.of("main", module));
      profiler.close();
      // Second close should not throw
      profiler.close();
      assertThat(profiler.isActive()).as("Profiler should be inactive after close").isFalse();

      module.close();
    }
  }
}
