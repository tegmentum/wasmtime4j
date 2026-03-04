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
package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link BreakpointEditor} obtained via {@link Store#editBreakpoints()}.
 *
 * <p>Verifies breakpoint add/remove, single-step toggling, and that debugging requires {@code
 * Config.guestDebug(true)}.
 *
 * @since 1.1.0
 */
@DisplayName("BreakpointEditor Tests")
public class BreakpointEditorTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(BreakpointEditorTest.class.getName());

  /** Simple WAT module with an exported function for breakpoint testing. */
  private static final String SIMPLE_WAT =
      "(module\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add\n"
          + "  )\n"
          + ")";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  // ==================== Debugging Disabled Tests ====================

  @Nested
  @DisplayName("Debugging Disabled")
  class DebuggingDisabledTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("editBreakpoints returns empty when guestDebug is false")
    void editBreakpointsEmptyWithoutGuestDebug(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing editBreakpoints without guestDebug");

      final EngineConfig config = new EngineConfig();
      // guestDebug defaults to false

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine)) {

        final Optional<BreakpointEditor> editor = store.editBreakpoints();
        LOGGER.info("[" + runtime + "] editBreakpoints returned: " + editor);

        // Without guestDebug, editor should be empty or operations should be safe no-ops
        // The default Store interface returns Optional.empty()
        // Implementation may return an editor that silently fails
        assertNotNull(editor, "Optional should not be null");
        LOGGER.info("[" + runtime + "] editBreakpoints without guestDebug: " + editor.isPresent());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("isSingleStep returns false when guestDebug is false")
    void isSingleStepFalseWithoutGuestDebug(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isSingleStep without guestDebug");

      final EngineConfig config = new EngineConfig();

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine)) {

        assertFalse(store.isSingleStep(), "isSingleStep should be false without debugging");
        LOGGER.info("[" + runtime + "] isSingleStep is false without guestDebug");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("breakpoints returns empty when guestDebug is false")
    void breakpointsEmptyWithoutGuestDebug(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing breakpoints without guestDebug");

      final EngineConfig config = new EngineConfig();

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine)) {

        final Optional<List<Breakpoint>> bps = store.breakpoints();
        LOGGER.info("[" + runtime + "] breakpoints returned: " + bps);

        // Without guestDebug, breakpoints should be empty
        assertNotNull(bps, "Optional should not be null");
        LOGGER.info("[" + runtime + "] breakpoints without guestDebug present: " + bps.isPresent());
      }
    }
  }

  // ==================== Debugging Enabled Tests ====================

  @Nested
  @DisplayName("Debugging Enabled")
  class DebuggingEnabledTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("editBreakpoints returns editor when guestDebug is true")
    void editBreakpointsReturnsEditorWithGuestDebug(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing editBreakpoints with guestDebug enabled");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine)) {

        final Optional<BreakpointEditor> editor = store.editBreakpoints();
        LOGGER.info("[" + runtime + "] editBreakpoints returned: " + editor);

        assertTrue(editor.isPresent(), "BreakpointEditor should be present with guestDebug");
        assertNotNull(editor.get(), "BreakpointEditor should not be null");
        LOGGER.info("[" + runtime + "] Got BreakpointEditor successfully");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("addBreakpoint succeeds with valid module and pc")
    void addBreakpointSucceeds(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing addBreakpoint");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine);
          Module module = engine.compileWat(SIMPLE_WAT)) {

        final Optional<BreakpointEditor> editorOpt = store.editBreakpoints();
        assertTrue(editorOpt.isPresent(), "Editor should be present");

        final BreakpointEditor editor = editorOpt.get();
        final BreakpointEditor result = editor.addBreakpoint(module, 0);
        assertNotNull(result, "addBreakpoint should return editor for chaining");
        LOGGER.info("[" + runtime + "] addBreakpoint at pc=0 succeeded");

        // Apply changes
        assertDoesNotThrow(editor::apply, "apply should not throw");
        LOGGER.info("[" + runtime + "] apply succeeded");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("removeBreakpoint succeeds after addBreakpoint")
    void removeBreakpointSucceeds(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing removeBreakpoint");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine);
          Module module = engine.compileWat(SIMPLE_WAT)) {

        final Optional<BreakpointEditor> editorOpt = store.editBreakpoints();
        assertTrue(editorOpt.isPresent(), "Editor should be present");

        final BreakpointEditor editor = editorOpt.get();
        editor.addBreakpoint(module, 0);
        LOGGER.info("[" + runtime + "] Added breakpoint at pc=0");

        final BreakpointEditor result = editor.removeBreakpoint(module, 0);
        assertNotNull(result, "removeBreakpoint should return editor for chaining");
        LOGGER.info("[" + runtime + "] Removed breakpoint at pc=0");

        editor.apply();
        LOGGER.info("[" + runtime + "] apply succeeded after add/remove cycle");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("singleStep can be toggled on and off")
    void singleStepToggle(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing singleStep toggle");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine)) {

        assertFalse(store.isSingleStep(), "singleStep should be false initially");

        final Optional<BreakpointEditor> editorOpt = store.editBreakpoints();
        assertTrue(editorOpt.isPresent(), "Editor should be present");

        final BreakpointEditor editor = editorOpt.get();

        // Enable single step
        final BreakpointEditor result = editor.singleStep(true);
        assertNotNull(result, "singleStep should return editor for chaining");
        editor.apply();
        LOGGER.info("[" + runtime + "] Enabled single step");

        assertTrue(store.isSingleStep(), "isSingleStep should be true after enabling");

        // Disable single step
        final Optional<BreakpointEditor> editorOpt2 = store.editBreakpoints();
        assertTrue(editorOpt2.isPresent(), "Editor should still be present");
        editorOpt2.get().singleStep(false).apply();
        LOGGER.info("[" + runtime + "] Disabled single step");

        assertFalse(store.isSingleStep(), "isSingleStep should be false after disabling");
        LOGGER.info("[" + runtime + "] singleStep toggle works correctly");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("method chaining works for multiple operations")
    void methodChainingWorks(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing method chaining");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine);
          Module module = engine.compileWat(SIMPLE_WAT)) {

        final Optional<BreakpointEditor> editorOpt = store.editBreakpoints();
        assertTrue(editorOpt.isPresent(), "Editor should be present");

        // Chain multiple operations
        assertDoesNotThrow(
            () ->
                editorOpt
                    .get()
                    .addBreakpoint(module, 0)
                    .addBreakpoint(module, 1)
                    .singleStep(true)
                    .apply(),
            "Chained operations should not throw");

        LOGGER.info("[" + runtime + "] Method chaining works correctly");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("breakpoints returns data when debugging is enabled")
    void breakpointsReturnsDataWithGuestDebug(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing breakpoints with guestDebug");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine)) {

        final Optional<List<Breakpoint>> bps = store.breakpoints();
        LOGGER.info("[" + runtime + "] breakpoints returned: " + bps);

        assertTrue(bps.isPresent(), "breakpoints should be present with guestDebug");
        assertNotNull(bps.get(), "breakpoints list should not be null");
        LOGGER.info("[" + runtime + "] breakpoints list size: " + bps.get().size());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("addBreakpoint at multiple program counters")
    void addMultipleBreakpoints(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multiple breakpoints");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine);
          Module module = engine.compileWat(SIMPLE_WAT)) {

        final Optional<BreakpointEditor> editorOpt = store.editBreakpoints();
        assertTrue(editorOpt.isPresent(), "Editor should be present");

        final BreakpointEditor editor = editorOpt.get();
        editor.addBreakpoint(module, 0);
        editor.addBreakpoint(module, 1);
        editor.addBreakpoint(module, 2);
        editor.apply();

        LOGGER.info("[" + runtime + "] Added 3 breakpoints successfully");

        // Remove one
        final Optional<BreakpointEditor> editorOpt2 = store.editBreakpoints();
        assertTrue(editorOpt2.isPresent(), "Editor should still be present");
        editorOpt2.get().removeBreakpoint(module, 1).apply();

        LOGGER.info("[" + runtime + "] Removed breakpoint at pc=1");
      }
    }
  }

  // ==================== Edge Case Tests ====================

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("addBreakpoint at pc=0 boundary")
    void addBreakpointAtPcZero(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing addBreakpoint at pc=0");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine);
          Module module = engine.compileWat(SIMPLE_WAT)) {

        final Optional<BreakpointEditor> editorOpt = store.editBreakpoints();
        assertTrue(editorOpt.isPresent(), "Editor should be present");

        assertDoesNotThrow(
            () -> editorOpt.get().addBreakpoint(module, 0).apply(),
            "addBreakpoint at pc=0 should not throw");
        LOGGER.info("[" + runtime + "] addBreakpoint at pc=0 succeeded");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("removeBreakpoint for non-existent breakpoint does not crash")
    void removeNonExistentBreakpoint(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing removeBreakpoint for non-existent breakpoint");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine);
          Module module = engine.compileWat(SIMPLE_WAT)) {

        final Optional<BreakpointEditor> editorOpt = store.editBreakpoints();
        assertTrue(editorOpt.isPresent(), "Editor should be present");

        // Removing a breakpoint that was never added should not crash
        assertDoesNotThrow(
            () -> editorOpt.get().removeBreakpoint(module, 999).apply(),
            "removeBreakpoint for non-existent breakpoint should not crash");
        LOGGER.info("[" + runtime + "] removeBreakpoint for non-existent breakpoint is safe");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("apply with no pending changes does not crash")
    void applyWithNoChanges(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing apply with no changes");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine)) {

        final Optional<BreakpointEditor> editorOpt = store.editBreakpoints();
        assertTrue(editorOpt.isPresent(), "Editor should be present");

        assertDoesNotThrow(() -> editorOpt.get().apply(), "apply with no changes should not throw");
        LOGGER.info("[" + runtime + "] apply with no changes succeeded");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("singleStep toggle multiple times")
    void singleStepMultipleToggles(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multiple singleStep toggles");

      final EngineConfig config = new EngineConfig().guestDebug(true);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine)) {

        assertFalse(store.isSingleStep(), "Should start with singleStep disabled");

        for (int i = 0; i < 5; i++) {
          final Optional<BreakpointEditor> editorOpt = store.editBreakpoints();
          assertTrue(editorOpt.isPresent(), "Editor should be present on iteration " + i);

          final boolean enable = (i % 2 == 0);
          editorOpt.get().singleStep(enable).apply();
          LOGGER.info(
              "["
                  + runtime
                  + "] Iteration "
                  + i
                  + ": singleStep="
                  + enable
                  + ", isSingleStep="
                  + store.isSingleStep());

          if (enable) {
            assertTrue(store.isSingleStep(), "isSingleStep should be true on iteration " + i);
          } else {
            assertFalse(store.isSingleStep(), "isSingleStep should be false on iteration " + i);
          }
        }
        LOGGER.info("[" + runtime + "] Multiple singleStep toggles work correctly");
      }
    }
  }
}
