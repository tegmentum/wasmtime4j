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

import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for {@link WeakEngine}.
 *
 * <p>Validates that weak engine references can be created, upgraded, and properly track the
 * lifecycle of the underlying engine across both JNI and Panama implementations.
 */
class WeakEngineTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WeakEngineTest.class.getName());

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testWeakEngineUpgradeWhileEngineAlive(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WeakEngine upgrade while engine is alive");
    try (Engine engine = Engine.create()) {
      final WeakEngine weak = engine.weak();
      assertThat(weak).as("weak() should return a non-null WeakEngine").isNotNull();
      assertThat(weak.isValid()).as("WeakEngine should be valid while engine is alive").isTrue();

      final Optional<Engine> upgraded = weak.upgrade();
      assertThat(upgraded).as("upgrade() should return the engine while it is alive").isPresent();
      assertThat(upgraded.get()).as("Upgraded engine should not be null").isNotNull();

      weak.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testWeakEngineAfterClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WeakEngine after closing the weak reference");
    try (Engine engine = Engine.create()) {
      final WeakEngine weak = engine.weak();
      assertThat(weak.isValid()).as("WeakEngine should be valid initially").isTrue();

      weak.close();
      assertThat(weak.isValid()).as("WeakEngine should be invalid after close").isFalse();

      final Optional<Engine> upgraded = weak.upgrade();
      assertThat(upgraded)
          .as("upgrade() should return empty after weak reference is closed")
          .isEmpty();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testMultipleWeakReferencesFromSameEngine(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multiple weak references from the same engine");
    try (Engine engine = Engine.create()) {
      final WeakEngine weak1 = engine.weak();
      final WeakEngine weak2 = engine.weak();

      assertThat(weak1.isValid()).as("First weak reference should be valid").isTrue();
      assertThat(weak2.isValid()).as("Second weak reference should be valid").isTrue();

      final Optional<Engine> upgraded1 = weak1.upgrade();
      final Optional<Engine> upgraded2 = weak2.upgrade();
      assertThat(upgraded1).as("First weak should upgrade").isPresent();
      assertThat(upgraded2).as("Second weak should upgrade").isPresent();

      // Closing one weak reference should not affect the other
      weak1.close();
      assertThat(weak1.isValid()).as("First weak should be invalid after close").isFalse();
      assertThat(weak2.isValid()).as("Second weak should still be valid").isTrue();

      final Optional<Engine> stillUpgraded = weak2.upgrade();
      assertThat(stillUpgraded)
          .as("Second weak should still upgrade after first is closed")
          .isPresent();

      weak2.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testWeakEngineDoubleClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WeakEngine double close is safe");
    try (Engine engine = Engine.create()) {
      final WeakEngine weak = engine.weak();
      weak.close();
      // Second close should not throw
      weak.close();
      assertThat(weak.isValid()).as("WeakEngine should remain invalid").isFalse();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testWeakEngineAfterEngineClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WeakEngine behavior after engine is closed");
    final WeakEngine weak;
    try (Engine engine = Engine.create()) {
      weak = engine.weak();
      assertThat(weak.isValid()).as("WeakEngine should be valid before engine close").isTrue();
    }
    // Engine is now closed; weak reference upgrade behavior depends on implementation.
    // The weak reference itself should still be closeable without error.
    weak.close();
  }
}
