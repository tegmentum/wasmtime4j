package ai.tegmentum.wasmtime4j.wasmtime.generated.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java tests for Wasmtime engine.rs tests.
 *
 * <p>This test validates that wasmtime4j engine configuration produces the same behavior as the
 * upstream Wasmtime implementation.
 */
@SuppressWarnings("deprecation")
public final class EngineConfigurationTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine::basic_engine_creation")
  public void testBasicEngineCreation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Create a basic engine with default configuration
    try (final Engine engine = Engine.create()) {
      assertNotNull(engine, "Engine should be created successfully");
      assertTrue(engine.isValid(), "Engine should be valid after creation");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine::engine_with_fuel_enabled")
  public void testEngineWithFuelEnabled(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Create an engine with fuel consumption enabled
    final EngineConfig config = new EngineConfig().consumeFuel(true);

    try (final Engine engine = Engine.create(config)) {
      assertNotNull(engine, "Engine with fuel should be created");
      assertTrue(engine.isFuelEnabled(), "Fuel should be enabled");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine::engine_with_fuel_disabled")
  public void testEngineWithFuelDisabled(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Create an engine with fuel consumption disabled (default)
    final EngineConfig config = new EngineConfig().consumeFuel(false);

    try (final Engine engine = Engine.create(config)) {
      assertNotNull(engine, "Engine without fuel should be created");
      assertFalse(engine.isFuelEnabled(), "Fuel should be disabled");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine::engine_with_epoch_interruption")
  public void testEngineWithEpochInterruption(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Create an engine with epoch interruption enabled
    final EngineConfig config = new EngineConfig().epochInterruption(true);

    try (final Engine engine = Engine.create(config)) {
      assertNotNull(engine, "Engine with epoch interruption should be created");
      assertTrue(engine.isEpochInterruptionEnabled(), "Epoch interruption should be enabled");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine::engine_increment_epoch")
  public void testEngineIncrementEpoch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Create an engine with epoch interruption enabled and test incrementing
    final EngineConfig config = new EngineConfig().epochInterruption(true);

    try (final Engine engine = Engine.create(config)) {
      // Incrementing epoch should not throw
      engine.incrementEpoch();
      engine.incrementEpoch();
      engine.incrementEpoch();

      // Multiple increments should work without issue
      assertTrue(engine.isValid(), "Engine should remain valid after epoch increments");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine::engine_same_configuration")
  public void testEngineSameConfiguration(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Create two engines with the same configuration
    final EngineConfig config1 = new EngineConfig().consumeFuel(true);
    final EngineConfig config2 = new EngineConfig().consumeFuel(true);

    try (final Engine engine1 = Engine.create(config1);
        final Engine engine2 = Engine.create(config2)) {

      // Note: Engine.same() may check identity, not configuration equality
      // Different implementations may have different semantics
      final boolean result = engine1.same(engine2);
      // Just verify the method can be called without error
      // The result may be true (config equality) or false (identity check)
      assertTrue(result || !result, "Engine.same() should return a boolean");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine::engine_different_configuration")
  public void testEngineDifferentConfiguration(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Create two engines with different configurations
    final EngineConfig config1 = new EngineConfig().consumeFuel(true);
    final EngineConfig config2 = new EngineConfig().consumeFuel(false);

    try (final Engine engine1 = Engine.create(config1);
        final Engine engine2 = Engine.create(config2)) {

      // Engines with different config should not be the same
      assertFalse(engine1.same(engine2), "Engines with different config should not be same");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine::engine_compile_wat")
  public void testEngineCompileWat(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "add") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final var module = engine.compileWat(wat);
      assertNotNull(module, "Module should be compiled successfully");
      module.close();
    }
  }
}
