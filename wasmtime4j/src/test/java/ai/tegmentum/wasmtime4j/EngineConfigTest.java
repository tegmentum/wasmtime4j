package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for EngineConfig.
 *
 * <p>Tests verify configuration options and factory methods.
 */
class EngineConfigTest {

  @Test
  @DisplayName("Should have guestDebug disabled by default")
  void shouldHaveGuestDebugDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isGuestDebug(), "guestDebug should be disabled by default");
  }

  @Test
  @DisplayName("Should enable guestDebug when set to true")
  void shouldEnableGuestDebugWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().guestDebug(true);
    assertTrue(config.isGuestDebug(), "guestDebug should be enabled");
  }

  @Test
  @DisplayName("Should disable guestDebug when set to false")
  void shouldDisableGuestDebugWhenSetToFalse() {
    final EngineConfig config = new EngineConfig().guestDebug(true).guestDebug(false);
    assertFalse(config.isGuestDebug(), "guestDebug should be disabled");
  }

  @Test
  @DisplayName("Should enable guestDebug in forDebug factory method")
  void shouldEnableGuestDebugInForDebugFactory() {
    final EngineConfig config = EngineConfig.forDebug();
    assertTrue(config.isGuestDebug(), "forDebug() should enable guestDebug");
    assertTrue(config.isDebugInfo(), "forDebug() should enable debugInfo");
    assertEquals(
        OptimizationLevel.NONE,
        config.getOptimizationLevel(),
        "forDebug() should set optimization to NONE");
    assertTrue(
        config.isCraneliftDebugVerifier(), "forDebug() should enable cranelift debug verifier");
  }

  @Test
  @DisplayName("Should support method chaining for guestDebug")
  void shouldSupportMethodChainingForGuestDebug() {
    final EngineConfig config =
        new EngineConfig()
            .guestDebug(true)
            .debugInfo(true)
            .optimizationLevel(OptimizationLevel.NONE);

    assertTrue(config.isGuestDebug(), "guestDebug should be enabled");
    assertTrue(config.isDebugInfo(), "debugInfo should be enabled");
    assertEquals(
        OptimizationLevel.NONE, config.getOptimizationLevel(), "optimization level should be NONE");
  }

  @Test
  @DisplayName("Should have debugInfo disabled by default")
  void shouldHaveDebugInfoDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isDebugInfo(), "debugInfo should be disabled by default");
  }

  @Test
  @DisplayName("Should enable debugInfo when set to true")
  void shouldEnableDebugInfoWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().debugInfo(true);
    assertTrue(config.isDebugInfo(), "debugInfo should be enabled");
  }

  @Test
  @DisplayName("Should have correct defaults for factory methods")
  void shouldHaveCorrectDefaultsForFactoryMethods() {
    final EngineConfig sizeConfig = EngineConfig.forSize();
    assertEquals(
        OptimizationLevel.SIZE,
        sizeConfig.getOptimizationLevel(),
        "forSize() should use SIZE optimization");
    assertTrue(sizeConfig.isParallelCompilation(), "forSize() should enable parallel compilation");
  }

  @Test
  @DisplayName("Should have wasmMemory64 disabled by default")
  void shouldHaveWasmMemory64DisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isWasmMemory64(), "wasmMemory64 should be disabled by default");
  }

  @Test
  @DisplayName("Should have isWasmMemory64 getter method")
  void shouldHaveIsWasmMemory64GetterMethod() {
    final EngineConfig config = new EngineConfig();
    // Verify the getter method exists and returns a boolean
    assertNotNull(
        Boolean.valueOf(config.isWasmMemory64()), "isWasmMemory64() should return a boolean value");
  }

  @Test
  @DisplayName("Should have enableCompiler enabled by default")
  void shouldHaveEnableCompilerEnabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertTrue(config.isEnableCompiler(), "enableCompiler should be enabled by default");
  }

  @Test
  @DisplayName("Should allow disabling the compiler")
  void shouldAllowDisablingCompiler() {
    final EngineConfig config = new EngineConfig().enableCompiler(false);
    assertFalse(config.isEnableCompiler(), "enableCompiler should be disabled after setting false");
  }

  @Test
  @DisplayName("Should preserve enableCompiler in copy")
  void shouldPreserveEnableCompilerInCopy() {
    final EngineConfig config = new EngineConfig().enableCompiler(false);
    final EngineConfig copy = config.copy();
    assertFalse(copy.isEnableCompiler(), "copy should preserve enableCompiler=false");
  }

  @Test
  @DisplayName("Should serialize enableCompiler to JSON")
  void shouldSerializeEnableCompilerToJson() {
    final EngineConfig config = new EngineConfig().enableCompiler(false);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(json.contains("\"enableCompiler\":false"),
        "JSON should contain enableCompiler:false but was: " + json);
  }
}
