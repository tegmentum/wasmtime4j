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

  // ===== wasmReferenceTypes tests =====

  @Test
  @DisplayName("Should have wasmReferenceTypes enabled by default")
  void shouldHaveWasmReferenceTypesEnabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertTrue(config.isWasmReferenceTypes(), "wasmReferenceTypes should be enabled by default");
  }

  @Test
  @DisplayName("Should allow disabling wasmReferenceTypes")
  void shouldAllowDisablingWasmReferenceTypes() {
    final EngineConfig config = new EngineConfig().wasmReferenceTypes(false);
    assertFalse(
        config.isWasmReferenceTypes(), "wasmReferenceTypes should be disabled after setting false");
  }

  @Test
  @DisplayName("Should preserve wasmReferenceTypes in copy")
  void shouldPreserveWasmReferenceTypesInCopy() {
    final EngineConfig config = new EngineConfig().wasmReferenceTypes(false);
    final EngineConfig copy = config.copy();
    assertFalse(copy.isWasmReferenceTypes(), "copy should preserve wasmReferenceTypes=false");
  }

  @Test
  @DisplayName("Should serialize wasmReferenceTypes to JSON")
  void shouldSerializeWasmReferenceTypesToJson() {
    final EngineConfig config = new EngineConfig().wasmReferenceTypes(false);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmReferenceTypes\":false"),
        "JSON should contain wasmReferenceTypes:false but was: " + json);
  }

  // ===== wasmSimd tests =====

  @Test
  @DisplayName("Should have wasmSimd enabled by default")
  void shouldHaveWasmSimdEnabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertTrue(config.isWasmSimd(), "wasmSimd should be enabled by default");
  }

  @Test
  @DisplayName("Should allow disabling wasmSimd")
  void shouldAllowDisablingWasmSimd() {
    final EngineConfig config = new EngineConfig().wasmSimd(false);
    assertFalse(config.isWasmSimd(), "wasmSimd should be disabled after setting false");
  }

  @Test
  @DisplayName("Should preserve wasmSimd in copy")
  void shouldPreserveWasmSimdInCopy() {
    final EngineConfig config = new EngineConfig().wasmSimd(false);
    final EngineConfig copy = config.copy();
    assertFalse(copy.isWasmSimd(), "copy should preserve wasmSimd=false");
  }

  @Test
  @DisplayName("Should serialize wasmSimd to JSON")
  void shouldSerializeWasmSimdToJson() {
    final EngineConfig config = new EngineConfig().wasmSimd(false);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmSimd\":false"), "JSON should contain wasmSimd:false but was: " + json);
  }

  // ===== wasmRelaxedSimd tests =====

  @Test
  @DisplayName("Should have wasmRelaxedSimd disabled by default")
  void shouldHaveWasmRelaxedSimdDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isWasmRelaxedSimd(), "wasmRelaxedSimd should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmRelaxedSimd when set to true")
  void shouldEnableWasmRelaxedSimdWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmRelaxedSimd(true);
    assertTrue(config.isWasmRelaxedSimd(), "wasmRelaxedSimd should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmRelaxedSimd in copy")
  void shouldPreserveWasmRelaxedSimdInCopy() {
    final EngineConfig config = new EngineConfig().wasmRelaxedSimd(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isWasmRelaxedSimd(), "copy should preserve wasmRelaxedSimd=true");
  }

  @Test
  @DisplayName("Should serialize wasmRelaxedSimd to JSON")
  void shouldSerializeWasmRelaxedSimdToJson() {
    final EngineConfig config = new EngineConfig().wasmRelaxedSimd(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmRelaxedSimd\":true"),
        "JSON should contain wasmRelaxedSimd:true but was: " + json);
  }

  // ===== wasmMultiValue tests =====

  @Test
  @DisplayName("Should have wasmMultiValue enabled by default")
  void shouldHaveWasmMultiValueEnabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertTrue(config.isWasmMultiValue(), "wasmMultiValue should be enabled by default");
  }

  @Test
  @DisplayName("Should allow disabling wasmMultiValue")
  void shouldAllowDisablingWasmMultiValue() {
    final EngineConfig config = new EngineConfig().wasmMultiValue(false);
    assertFalse(config.isWasmMultiValue(), "wasmMultiValue should be disabled after setting false");
  }

  @Test
  @DisplayName("Should preserve wasmMultiValue in copy")
  void shouldPreserveWasmMultiValueInCopy() {
    final EngineConfig config = new EngineConfig().wasmMultiValue(false);
    final EngineConfig copy = config.copy();
    assertFalse(copy.isWasmMultiValue(), "copy should preserve wasmMultiValue=false");
  }

  @Test
  @DisplayName("Should serialize wasmMultiValue to JSON")
  void shouldSerializeWasmMultiValueToJson() {
    final EngineConfig config = new EngineConfig().wasmMultiValue(false);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmMultiValue\":false"),
        "JSON should contain wasmMultiValue:false but was: " + json);
  }

  // ===== wasmBulkMemory tests =====

  @Test
  @DisplayName("Should have wasmBulkMemory enabled by default")
  void shouldHaveWasmBulkMemoryEnabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertTrue(config.isWasmBulkMemory(), "wasmBulkMemory should be enabled by default");
  }

  @Test
  @DisplayName("Should allow disabling wasmBulkMemory")
  void shouldAllowDisablingWasmBulkMemory() {
    final EngineConfig config = new EngineConfig().wasmBulkMemory(false);
    assertFalse(config.isWasmBulkMemory(), "wasmBulkMemory should be disabled after setting false");
  }

  @Test
  @DisplayName("Should preserve wasmBulkMemory in copy")
  void shouldPreserveWasmBulkMemoryInCopy() {
    final EngineConfig config = new EngineConfig().wasmBulkMemory(false);
    final EngineConfig copy = config.copy();
    assertFalse(copy.isWasmBulkMemory(), "copy should preserve wasmBulkMemory=false");
  }

  @Test
  @DisplayName("Should serialize wasmBulkMemory to JSON")
  void shouldSerializeWasmBulkMemoryToJson() {
    final EngineConfig config = new EngineConfig().wasmBulkMemory(false);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmBulkMemory\":false"),
        "JSON should contain wasmBulkMemory:false but was: " + json);
  }

  // ===== wasmThreads tests =====

  @Test
  @DisplayName("Should have wasmThreads disabled by default")
  void shouldHaveWasmThreadsDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isWasmThreads(), "wasmThreads should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmThreads when set to true")
  void shouldEnableWasmThreadsWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmThreads(true);
    assertTrue(config.isWasmThreads(), "wasmThreads should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmThreads in copy")
  void shouldPreserveWasmThreadsInCopy() {
    final EngineConfig config = new EngineConfig().wasmThreads(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isWasmThreads(), "copy should preserve wasmThreads=true");
  }

  @Test
  @DisplayName("Should serialize wasmThreads to JSON")
  void shouldSerializeWasmThreadsToJson() {
    final EngineConfig config = new EngineConfig().wasmThreads(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmThreads\":true"),
        "JSON should contain wasmThreads:true but was: " + json);
  }

  // ===== wasmTailCall tests =====

  @Test
  @DisplayName("Should have wasmTailCall disabled by default")
  void shouldHaveWasmTailCallDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isWasmTailCall(), "wasmTailCall should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmTailCall when set to true")
  void shouldEnableWasmTailCallWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmTailCall(true);
    assertTrue(config.isWasmTailCall(), "wasmTailCall should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmTailCall in copy")
  void shouldPreserveWasmTailCallInCopy() {
    final EngineConfig config = new EngineConfig().wasmTailCall(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isWasmTailCall(), "copy should preserve wasmTailCall=true");
  }

  @Test
  @DisplayName("Should serialize wasmTailCall to JSON")
  void shouldSerializeWasmTailCallToJson() {
    final EngineConfig config = new EngineConfig().wasmTailCall(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmTailCall\":true"),
        "JSON should contain wasmTailCall:true but was: " + json);
  }

  // ===== wasmMultiMemory tests =====

  @Test
  @DisplayName("Should have wasmMultiMemory disabled by default")
  void shouldHaveWasmMultiMemoryDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isWasmMultiMemory(), "wasmMultiMemory should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmMultiMemory when set to true")
  void shouldEnableWasmMultiMemoryWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmMultiMemory(true);
    assertTrue(config.isWasmMultiMemory(), "wasmMultiMemory should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmMultiMemory in copy")
  void shouldPreserveWasmMultiMemoryInCopy() {
    final EngineConfig config = new EngineConfig().wasmMultiMemory(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isWasmMultiMemory(), "copy should preserve wasmMultiMemory=true");
  }

  @Test
  @DisplayName("Should serialize wasmMultiMemory to JSON")
  void shouldSerializeWasmMultiMemoryToJson() {
    final EngineConfig config = new EngineConfig().wasmMultiMemory(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmMultiMemory\":true"),
        "JSON should contain wasmMultiMemory:true but was: " + json);
  }

  // ===== wasmMemory64 tests =====

  @Test
  @DisplayName("Should have wasmMemory64 disabled by default")
  void shouldHaveWasmMemory64DisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isWasmMemory64(), "wasmMemory64 should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmMemory64 when set to true")
  void shouldEnableWasmMemory64WhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmMemory64(true);
    assertTrue(config.isWasmMemory64(), "wasmMemory64 should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmMemory64 in copy")
  void shouldPreserveWasmMemory64InCopy() {
    final EngineConfig config = new EngineConfig().wasmMemory64(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isWasmMemory64(), "copy should preserve wasmMemory64=true");
  }

  @Test
  @DisplayName("Should serialize wasmMemory64 to JSON")
  void shouldSerializeWasmMemory64ToJson() {
    final EngineConfig config = new EngineConfig().wasmMemory64(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmMemory64\":true"),
        "JSON should contain wasmMemory64:true but was: " + json);
  }

  // ===== wasmStackSwitching tests =====

  @Test
  @DisplayName("Should have wasmStackSwitching disabled by default")
  void shouldHaveWasmStackSwitchingDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isWasmStackSwitching(), "wasmStackSwitching should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmStackSwitching when set to true")
  void shouldEnableWasmStackSwitchingWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmStackSwitching(true);
    assertTrue(config.isWasmStackSwitching(), "wasmStackSwitching should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmStackSwitching in copy")
  void shouldPreserveWasmStackSwitchingInCopy() {
    final EngineConfig config = new EngineConfig().wasmStackSwitching(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isWasmStackSwitching(), "copy should preserve wasmStackSwitching=true");
  }

  @Test
  @DisplayName("Should serialize wasmStackSwitching to JSON")
  void shouldSerializeWasmStackSwitchingToJson() {
    final EngineConfig config = new EngineConfig().wasmStackSwitching(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmStackSwitching\":true"),
        "JSON should contain wasmStackSwitching:true but was: " + json);
  }

  // ===== wasmExtendedConstExpressions tests =====

  @Test
  @DisplayName("Should have wasmExtendedConstExpressions disabled by default")
  void shouldHaveWasmExtendedConstExpressionsDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(
        config.isWasmExtendedConstExpressions(),
        "wasmExtendedConstExpressions should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmExtendedConstExpressions when set to true")
  void shouldEnableWasmExtendedConstExpressionsWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmExtendedConstExpressions(true);
    assertTrue(
        config.isWasmExtendedConstExpressions(), "wasmExtendedConstExpressions should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmExtendedConstExpressions in copy")
  void shouldPreserveWasmExtendedConstExpressionsInCopy() {
    final EngineConfig config = new EngineConfig().wasmExtendedConstExpressions(true);
    final EngineConfig copy = config.copy();
    assertTrue(
        copy.isWasmExtendedConstExpressions(),
        "copy should preserve wasmExtendedConstExpressions=true");
  }

  @Test
  @DisplayName("Should serialize wasmExtendedConstExpressions to JSON")
  void shouldSerializeWasmExtendedConstExpressionsToJson() {
    final EngineConfig config = new EngineConfig().wasmExtendedConstExpressions(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmExtendedConst\":true"),
        "JSON should contain wasmExtendedConst:true but was: " + json);
  }

  // ===== wasmCustomPageSizes tests =====

  @Test
  @DisplayName("Should have wasmCustomPageSizes disabled by default")
  void shouldHaveWasmCustomPageSizesDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(
        config.isWasmCustomPageSizes(), "wasmCustomPageSizes should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmCustomPageSizes when set to true")
  void shouldEnableWasmCustomPageSizesWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmCustomPageSizes(true);
    assertTrue(config.isWasmCustomPageSizes(), "wasmCustomPageSizes should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmCustomPageSizes in copy")
  void shouldPreserveWasmCustomPageSizesInCopy() {
    final EngineConfig config = new EngineConfig().wasmCustomPageSizes(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isWasmCustomPageSizes(), "copy should preserve wasmCustomPageSizes=true");
  }

  @Test
  @DisplayName("Should serialize wasmCustomPageSizes to JSON")
  void shouldSerializeWasmCustomPageSizesToJson() {
    final EngineConfig config = new EngineConfig().wasmCustomPageSizes(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmCustomPageSizes\":true"),
        "JSON should contain wasmCustomPageSizes:true but was: " + json);
  }

  // ===== wasmSharedEverythingThreads tests =====

  @Test
  @DisplayName("Should have wasmSharedEverythingThreads disabled by default")
  void shouldHaveWasmSharedEverythingThreadsDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(
        config.isWasmSharedEverythingThreads(),
        "wasmSharedEverythingThreads should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmSharedEverythingThreads when set to true")
  void shouldEnableWasmSharedEverythingThreadsWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmSharedEverythingThreads(true);
    assertTrue(
        config.isWasmSharedEverythingThreads(), "wasmSharedEverythingThreads should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmSharedEverythingThreads in copy")
  void shouldPreserveWasmSharedEverythingThreadsInCopy() {
    final EngineConfig config = new EngineConfig().wasmSharedEverythingThreads(true);
    final EngineConfig copy = config.copy();
    assertTrue(
        copy.isWasmSharedEverythingThreads(),
        "copy should preserve wasmSharedEverythingThreads=true");
  }

  @Test
  @DisplayName("Should serialize wasmSharedEverythingThreads to JSON")
  void shouldSerializeWasmSharedEverythingThreadsToJson() {
    final EngineConfig config = new EngineConfig().wasmSharedEverythingThreads(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmSharedEverythingThreads\":true"),
        "JSON should contain wasmSharedEverythingThreads:true but was: " + json);
  }

  // ===== wasmComponentModel tests =====

  @Test
  @DisplayName("Should have wasmComponentModel disabled by default")
  void shouldHaveWasmComponentModelDisabledByDefault() {
    final EngineConfig config = new EngineConfig();
    assertFalse(config.isWasmComponentModel(), "wasmComponentModel should be disabled by default");
  }

  @Test
  @DisplayName("Should enable wasmComponentModel when set to true")
  void shouldEnableWasmComponentModelWhenSetToTrue() {
    final EngineConfig config = new EngineConfig().wasmComponentModel(true);
    assertTrue(config.isWasmComponentModel(), "wasmComponentModel should be enabled");
  }

  @Test
  @DisplayName("Should preserve wasmComponentModel in copy")
  void shouldPreserveWasmComponentModelInCopy() {
    final EngineConfig config = new EngineConfig().wasmComponentModel(true);
    final EngineConfig copy = config.copy();
    assertTrue(copy.isWasmComponentModel(), "copy should preserve wasmComponentModel=true");
  }

  @Test
  @DisplayName("Should serialize wasmComponentModel to JSON")
  void shouldSerializeWasmComponentModelToJson() {
    final EngineConfig config = new EngineConfig().wasmComponentModel(true);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(
        json.contains("\"wasmComponentModel\":true"),
        "JSON should contain wasmComponentModel:true but was: " + json);
  }
}
