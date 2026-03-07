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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
      assertNotNull(weak, "weak() should return a non-null WeakEngine");
      assertTrue(weak.isValid(), "WeakEngine should be valid while engine is alive");

      final Optional<Engine> upgraded = weak.upgrade();
      assertTrue(upgraded.isPresent(), "upgrade() should return the engine while it is alive");
      assertNotNull(upgraded.get(), "Upgraded engine should not be null");

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
      assertTrue(weak.isValid(), "WeakEngine should be valid initially");

      weak.close();
      assertFalse(weak.isValid(), "WeakEngine should be invalid after close");

      final Optional<Engine> upgraded = weak.upgrade();
      assertTrue(upgraded.isEmpty(), "upgrade() should return empty after weak reference is closed");
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

      assertTrue(weak1.isValid(), "First weak reference should be valid");
      assertTrue(weak2.isValid(), "Second weak reference should be valid");

      final Optional<Engine> upgraded1 = weak1.upgrade();
      final Optional<Engine> upgraded2 = weak2.upgrade();
      assertTrue(upgraded1.isPresent(), "First weak should upgrade");
      assertTrue(upgraded2.isPresent(), "Second weak should upgrade");

      // Closing one weak reference should not affect the other
      weak1.close();
      assertFalse(weak1.isValid(), "First weak should be invalid after close");
      assertTrue(weak2.isValid(), "Second weak should still be valid");

      final Optional<Engine> stillUpgraded = weak2.upgrade();
      assertTrue(stillUpgraded.isPresent(), "Second weak should still upgrade after first is closed");

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
      assertFalse(weak.isValid(), "WeakEngine should remain invalid");
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
      assertTrue(weak.isValid(), "WeakEngine should be valid before engine close");
    }
    // Engine is now closed; weak reference upgrade behavior depends on implementation.
    // The weak reference itself should still be closeable without error.
    weak.close();
  }
}
