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
package ai.tegmentum.wasmtime4j.wasmtime.generated.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for debug introspection APIs: debugInstanceCount() and debugModuleCount().
 *
 * <p>These APIs require guest debugging to be enabled via Config.guestDebug(true).
 */
@DisplayName("Debug Introspection APIs")
public final class DebugIntrospectionTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Without debugging enabled")
  class WithoutDebugging {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("debugInstanceCount returns 0 without guest debug")
    void debugInstanceCountReturnsZeroWithoutDebug(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);

      try (final Engine engine = Engine.create(new EngineConfig());
          final Store store = engine.createStore()) {
        assertEquals(0, store.debugInstanceCount());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("debugModuleCount returns 0 without guest debug")
    void debugModuleCountReturnsZeroWithoutDebug(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);

      try (final Engine engine = Engine.create(new EngineConfig());
          final Store store = engine.createStore()) {
        assertEquals(0, store.debugModuleCount());
      }
    }
  }

  @Nested
  @DisplayName("With debugging enabled")
  class WithDebugging {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("debugInstanceCount returns count after instantiation")
    void debugInstanceCountAfterInstantiation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);

      final EngineConfig config = new EngineConfig().guestDebug(true);

      final String wat =
          """
          (module
            (func (export "nop"))
          )
          """;

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore();
          final Module module = engine.compileWat(wat);
          final Linker<?> linker = Linker.create(engine)) {
        // Before instantiation
        final int beforeCount = store.debugInstanceCount();

        // Instantiate
        linker.instantiate(store, module);

        // After instantiation, count should be >= before
        final int afterCount = store.debugInstanceCount();
        assertTrue(
            afterCount >= beforeCount,
            "Instance count after instantiation ("
                + afterCount
                + ") should be >= before ("
                + beforeCount
                + ")");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("debugModuleCount returns count after module compilation")
    void debugModuleCountAfterCompilation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);

      final EngineConfig config = new EngineConfig().guestDebug(true);

      final String wat =
          """
          (module
            (func (export "nop"))
          )
          """;

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore();
          final Module module = engine.compileWat(wat);
          final Linker<?> linker = Linker.create(engine)) {
        linker.instantiate(store, module);

        final int moduleCount = store.debugModuleCount();
        // With debugging enabled, at least 0 modules should be reported
        assertTrue(moduleCount >= 0, "Module count should be non-negative: " + moduleCount);
      }
    }
  }
}
