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
    assertTrue(
        json.contains("\"enableCompiler\":false"),
        "JSON should contain enableCompiler:false but was: " + json);
  }

  @Test
  @DisplayName("Should have emitClif null by default")
  void shouldHaveEmitClifNullByDefault() {
    final EngineConfig config = new EngineConfig();
    assertNull(config.getEmitClif(), "emitClif should be null by default");
  }

  @Test
  @DisplayName("Should set emitClif directory path")
  void shouldSetEmitClifDirectoryPath() {
    final EngineConfig config = new EngineConfig().emitClif("/tmp/clif-output");
    assertEquals("/tmp/clif-output", config.getEmitClif(), "emitClif should be set to the path");
  }

  @Test
  @DisplayName("Should clear emitClif when set to null")
  void shouldClearEmitClifWhenSetToNull() {
    final EngineConfig config = new EngineConfig().emitClif("/tmp/clif-output").emitClif(null);
    assertNull(config.getEmitClif(), "emitClif should be null after clearing");
  }

  @Test
  @DisplayName("Should preserve emitClif in copy")
  void shouldPreserveEmitClifInCopy() {
    final EngineConfig config = new EngineConfig().emitClif("/tmp/clif-output");
    final EngineConfig copy = config.copy();
    assertEquals("/tmp/clif-output", copy.getEmitClif(), "copy should preserve emitClif path");
  }

  @Test
  @DisplayName("Should serialize emitClif to JSON when set")
  void shouldSerializeEmitClifToJsonWhenSet() {
    final EngineConfig config = new EngineConfig().emitClif("/tmp/clif-output");
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"emitClif\":\"/tmp/clif-output\""),
        "JSON should contain emitClif path but was: " + json);
  }

  @Test
  @DisplayName("Should not serialize emitClif to JSON when null")
  void shouldNotSerializeEmitClifToJsonWhenNull() {
    final EngineConfig config = new EngineConfig();
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertFalse(
        json.contains("emitClif"), "JSON should not contain emitClif when null but was: " + json);
  }

  @Test
  @DisplayName("Should support method chaining for emitClif")
  void shouldSupportMethodChainingForEmitClif() {
    final EngineConfig config =
        new EngineConfig().emitClif("/tmp/clif-output").debugInfo(true).enableCompiler(true);

    assertEquals("/tmp/clif-output", config.getEmitClif(), "emitClif should be set");
    assertTrue(config.isDebugInfo(), "debugInfo should be enabled");
    assertTrue(config.isEnableCompiler(), "enableCompiler should be enabled");
  }

  // ===== x86FloatAbiOk tests =====

  @Test
  @DisplayName("Should have x86FloatAbiOk disabled by default")
  void shouldHaveX86FloatAbiOkDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isX86FloatAbiOk(), "x86FloatAbiOk should be disabled by default");
  }

  @Test
  @DisplayName("Should enable x86FloatAbiOk when set to true")
  void shouldEnableX86FloatAbiOkWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().x86FloatAbiOk(true);
    assertTrue(config.isX86FloatAbiOk(), "x86FloatAbiOk should be enabled");
  }

  @Test
  @DisplayName("Should preserve x86FloatAbiOk in copy")
  void shouldPreserveX86FloatAbiOkInCopy() {
    final EngineConfig config = new EngineConfig().x86FloatAbiOk(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isX86FloatAbiOk(), "copy should preserve x86FloatAbiOk=true");
  }

  @Test
  @DisplayName("Should serialize x86FloatAbiOk to JSON")
  void shouldSerializeX86FloatAbiOkToJson() {
    final EngineConfig config = new EngineConfig().x86FloatAbiOk(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"x86FloatAbiOk\":true"),
        "JSON should contain x86FloatAbiOk:true but was: " + json);
  }

  // ===== signalsBasedTraps tests =====

  @Test
  @DisplayName("Should have signalsBasedTraps disabled by default")
  void shouldHaveSignalsBasedTrapsDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isSignalsBasedTraps(), "signalsBasedTraps should be disabled by default");
  }

  @Test
  @DisplayName("Should accept signalsBasedTraps(true) even though native overrides it")
  void shouldAcceptSignalsBasedTrapsTrue() {
    final EngineConfig config = new EngineConfig().signalsBasedTraps(true);
    assertTrue(
        config.isSignalsBasedTraps(),
        "signalsBasedTraps should reflect the set value on the Java side");
  }

  @Test
  @DisplayName("Should preserve signalsBasedTraps in copy")
  void shouldPreserveSignalsBasedTrapsInCopy() {
    final EngineConfig config = new EngineConfig().signalsBasedTraps(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isSignalsBasedTraps(), "copy should preserve signalsBasedTraps=true");
  }

  @Test
  @DisplayName("Should serialize signalsBasedTraps to JSON")
  void shouldSerializeSignalsBasedTrapsToJson() {
    final EngineConfig config = new EngineConfig().signalsBasedTraps(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"signalsBasedTraps\":true"),
        "JSON should contain signalsBasedTraps:true but was: " + json);
  }

  @Test
  @DisplayName("Should support method chaining for new config options")
  void shouldSupportMethodChainingForNewConfigOptions() {
    final EngineConfig config =
        new EngineConfig().x86FloatAbiOk(true).signalsBasedTraps(false).enableCompiler(true);

    assertTrue(config.isX86FloatAbiOk(), "x86FloatAbiOk should be enabled");
    assertFalse(config.isSignalsBasedTraps(), "signalsBasedTraps should be disabled");
    assertTrue(config.isEnableCompiler(), "enableCompiler should be enabled");
  }

  @Test
  @DisplayName("Should have concurrencySupport disabled by default")
  void shouldHaveConcurrencySupportDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isConcurrencySupport(), "concurrencySupport should be disabled by default");
  }

  @Test
  @DisplayName("Should enable concurrencySupport when set to true")
  void shouldEnableConcurrencySupportWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().concurrencySupport(true);
    assertTrue(config.isConcurrencySupport(), "concurrencySupport should be enabled");
  }

  @Test
  @DisplayName("Should preserve concurrencySupport in copy")
  void shouldPreserveConcurrencySupportInCopy() {
    final EngineConfig original = new EngineConfig().concurrencySupport(true);
    final EngineConfig copied = original.copy();
    assertTrue(copied.isConcurrencySupport(), "copied config should preserve concurrencySupport");
  }

  @Test
  @DisplayName("Should serialize concurrencySupport to JSON")
  void shouldSerializeConcurrencySupportToJson() {
    final EngineConfig config = new EngineConfig().concurrencySupport(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"concurrencySupport\":true"),
        "JSON should contain concurrencySupport:true, got: " + json);
  }
}
