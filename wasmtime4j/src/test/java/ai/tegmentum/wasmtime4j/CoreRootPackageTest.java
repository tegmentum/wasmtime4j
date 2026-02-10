/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for core root package classes (Phase 21).
 *
 * <p>This test file covers the existing core root package classes:
 *
 * <ul>
 *   <li>WasmExecutionContext - Execution context interface
 *   <li>WasmThreadLocalStorage - Thread-local storage interface
 *   <li>StoreLimitsBuilder - Builder for store limits
 *   <li>WasiLinker - WASI linking utility
 *   <li>ComponentEngineHealth - Health monitoring interface
 *   <li>ComponentEngineConfig - Engine configuration class
 *   <li>ComplexMarshalingService - Marshaling service class
 *   <li>ComponentEngineStatistics - Statistics interface
 *   <li>ComponentLinker - Component linking interface
 *   <li>InstanceManager - Instance pooling interface
 *   <li>WitInterfaceLinker - WIT interface linking class
 *   <li>WitTypeValidator - Type validation class
 *   <li>ComponentLifecycleManager - Lifecycle management interface
 *   <li>DebugFrame - Debug frame class
 *   <li>WasmMemory - Memory interface
 *   <li>StoreBuilder - Builder for stores
 * </ul>
 */
@DisplayName("Core Root Package Tests (Phase 21)")
class CoreRootPackageTest {

  // ========================================================================
  // WasmExecutionContext Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmExecutionContext Interface Tests")
  class WasmExecutionContextTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasmExecutionContext.class.isInterface(), "WasmExecutionContext should be an interface");
    }

    @Test
    @DisplayName("should have getExecutionStatistics method")
    void shouldHaveGetExecutionStatisticsMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("getExecutionStatistics");
      assertNotNull(method, "getExecutionStatistics method should exist");
      assertEquals(
          WasmExecutionContext.ExecutionStatistics.class,
          method.getReturnType(),
          "Return type should be ExecutionStatistics");
    }

    @Test
    @DisplayName("should have isProfileGuidedOptimizationEnabled method")
    void shouldHaveIsProfileGuidedOptimizationEnabledMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("isProfileGuidedOptimizationEnabled");
      assertNotNull(method, "isProfileGuidedOptimizationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getOptimizationLevel method")
    void shouldHaveGetOptimizationLevelMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("getOptimizationLevel");
      assertNotNull(method, "getOptimizationLevel method should exist");
      assertEquals(
          WasmExecutionContext.OptimizationLevel.class,
          method.getReturnType(),
          "Return type should be OptimizationLevel");
    }

    @Test
    @DisplayName("should have setOptimizationLevel method")
    void shouldHaveSetOptimizationLevelMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.class.getMethod(
              "setOptimizationLevel", WasmExecutionContext.OptimizationLevel.class);
      assertNotNull(method, "setOptimizationLevel method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getBranchPredictionStatistics method")
    void shouldHaveGetBranchPredictionStatisticsMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("getBranchPredictionStatistics");
      assertNotNull(method, "getBranchPredictionStatistics method should exist");
      assertEquals(
          WasmExecutionContext.BranchPredictionStatistics.class,
          method.getReturnType(),
          "Return type should be BranchPredictionStatistics");
    }

    @Test
    @DisplayName("should have resetStatistics method")
    void shouldHaveResetStatisticsMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("resetStatistics");
      assertNotNull(method, "resetStatistics method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have static createDefault factory method")
    void shouldHaveStaticCreateDefaultMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("createDefault");
      assertNotNull(method, "createDefault static method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "createDefault should be a static method");
      assertEquals(
          WasmExecutionContext.class,
          method.getReturnType(),
          "Return type should be WasmExecutionContext");
    }

    @Test
    @DisplayName("should have static create factory method with parameters")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.class.getMethod(
              "create", WasmExecutionContext.OptimizationLevel.class, boolean.class);
      assertNotNull(method, "create static method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be a static method");
      assertEquals(
          WasmExecutionContext.class,
          method.getReturnType(),
          "Return type should be WasmExecutionContext");
    }
  }

  // ========================================================================
  // WasmThreadLocalStorage Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmThreadLocalStorage Interface Tests")
  class WasmThreadLocalStorageTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasmThreadLocalStorage.class.isInterface(),
          "WasmThreadLocalStorage should be an interface");
    }

    @Test
    @DisplayName("should have putInt method")
    void shouldHavePutIntMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("putInt", String.class, int.class);
      assertNotNull(method, "putInt method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getInt method")
    void shouldHaveGetIntMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("getInt", String.class);
      assertNotNull(method, "getInt method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have putLong method")
    void shouldHavePutLongMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("putLong", String.class, long.class);
      assertNotNull(method, "putLong method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getLong method")
    void shouldHaveGetLongMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("getLong", String.class);
      assertNotNull(method, "getLong method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have putBytes method")
    void shouldHavePutBytesMethod() throws NoSuchMethodException {
      Method method =
          WasmThreadLocalStorage.class.getMethod("putBytes", String.class, byte[].class);
      assertNotNull(method, "putBytes method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getBytes method")
    void shouldHaveGetBytesMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("getBytes", String.class);
      assertNotNull(method, "getBytes method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
    }

    @Test
    @DisplayName("should have putString method")
    void shouldHavePutStringMethod() throws NoSuchMethodException {
      Method method =
          WasmThreadLocalStorage.class.getMethod("putString", String.class, String.class);
      assertNotNull(method, "putString method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getString method")
    void shouldHaveGetStringMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("getString", String.class);
      assertNotNull(method, "getString method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have remove method")
    void shouldHaveRemoveMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("remove", String.class);
      assertNotNull(method, "remove method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have contains method")
    void shouldHaveContainsMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("contains", String.class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getMemoryUsage method")
    void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
      Method method = WasmThreadLocalStorage.class.getMethod("getMemoryUsage");
      assertNotNull(method, "getMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // StoreLimitsBuilder Tests
  // ========================================================================

  @Nested
  @DisplayName("StoreLimitsBuilder Class Tests")
  class StoreLimitsBuilderTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(StoreLimitsBuilder.class.getModifiers()),
          "StoreLimitsBuilder should be a final class");
    }

    @Test
    @DisplayName("should have memorySize builder method")
    void shouldHaveMemorySizeMethod() throws NoSuchMethodException {
      Method method = StoreLimitsBuilder.class.getMethod("memorySize", long.class);
      assertNotNull(method, "memorySize method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "Return type should be StoreLimitsBuilder for fluent API");
    }

    @Test
    @DisplayName("should have tableElements builder method")
    void shouldHaveTableElementsMethod() throws NoSuchMethodException {
      Method method = StoreLimitsBuilder.class.getMethod("tableElements", long.class);
      assertNotNull(method, "tableElements method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "Return type should be StoreLimitsBuilder for fluent API");
    }

    @Test
    @DisplayName("should have instances builder method")
    void shouldHaveInstancesMethod() throws NoSuchMethodException {
      Method method = StoreLimitsBuilder.class.getMethod("instances", long.class);
      assertNotNull(method, "instances method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "Return type should be StoreLimitsBuilder for fluent API");
    }

    @Test
    @DisplayName("should have tables builder method")
    void shouldHaveTablesMethod() throws NoSuchMethodException {
      Method method = StoreLimitsBuilder.class.getMethod("tables", long.class);
      assertNotNull(method, "tables method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "Return type should be StoreLimitsBuilder for fluent API");
    }

    @Test
    @DisplayName("should have memories builder method")
    void shouldHaveMemoriesMethod() throws NoSuchMethodException {
      Method method = StoreLimitsBuilder.class.getMethod("memories", long.class);
      assertNotNull(method, "memories method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "Return type should be StoreLimitsBuilder for fluent API");
    }

    @Test
    @DisplayName("should have getMemorySize getter method")
    void shouldHaveGetMemorySizeMethod() throws NoSuchMethodException {
      Method method = StoreLimitsBuilder.class.getMethod("getMemorySize");
      assertNotNull(method, "getMemorySize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = StoreLimitsBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(StoreLimits.class, method.getReturnType(), "Return type should be StoreLimits");
    }

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      Method method = StoreLimitsBuilder.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }
  }

  // ========================================================================
  // WasiLinker Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiLinker Class Tests")
  class WasiLinkerTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiLinker.class.getModifiers()), "WasiLinker should be a final class");
    }

    @Test
    @DisplayName("should have addToLinker static method with context")
    void shouldHaveAddToLinkerWithContextMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("addToLinker", Linker.class, WasiContext.class);
      assertNotNull(method, "addToLinker method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "addToLinker should be a static method");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have addToLinker static method without context")
    void shouldHaveAddToLinkerWithoutContextMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("addToLinker", Linker.class);
      assertNotNull(method, "addToLinker method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "addToLinker should be a static method");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have createLinker static method with context")
    void shouldHaveCreateLinkerWithContextMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createLinker", Engine.class, WasiContext.class);
      assertNotNull(method, "createLinker method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "createLinker should be a static method");
      assertEquals(Linker.class, method.getReturnType(), "Return type should be Linker");
    }

    @Test
    @DisplayName("should have createLinker static method without context")
    void shouldHaveCreateLinkerWithoutContextMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("createLinker", Engine.class);
      assertNotNull(method, "createLinker method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "createLinker should be a static method");
      assertEquals(Linker.class, method.getReturnType(), "Return type should be Linker");
    }

    @Test
    @DisplayName("should have addPreview2ToLinker static method")
    void shouldHaveAddPreview2ToLinkerMethod() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod("addPreview2ToLinker", Linker.class, WasiContext.class);
      assertNotNull(method, "addPreview2ToLinker method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()),
          "addPreview2ToLinker should be a static method");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have createPreview2Linker static method")
    void shouldHaveCreatePreview2LinkerMethod() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod("createPreview2Linker", Engine.class, WasiContext.class);
      assertNotNull(method, "createPreview2Linker method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()),
          "createPreview2Linker should be a static method");
      assertEquals(Linker.class, method.getReturnType(), "Return type should be Linker");
    }

    @Test
    @DisplayName("should have createFullLinker static method")
    void shouldHaveCreateFullLinkerMethod() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod("createFullLinker", Engine.class, WasiContext.class);
      assertNotNull(method, "createFullLinker method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "createFullLinker should be a static method");
      assertEquals(Linker.class, method.getReturnType(), "Return type should be Linker");
    }

    @Test
    @DisplayName("should have hasWasiImports static method")
    void shouldHaveHasWasiImportsMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("hasWasiImports", Linker.class);
      assertNotNull(method, "hasWasiImports method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "hasWasiImports should be a static method");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have hasWasiPreview2Imports static method")
    void shouldHaveHasWasiPreview2ImportsMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("hasWasiPreview2Imports", Linker.class);
      assertNotNull(method, "hasWasiPreview2Imports method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()),
          "hasWasiPreview2Imports should be a static method");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // ComponentEngineHealth Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentEngineHealth Interface Tests")
  class ComponentEngineHealthTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ComponentEngineHealth.class.isInterface(),
          "ComponentEngineHealth should be an interface");
    }

    @Test
    @DisplayName("should have getHealthStatus method")
    void shouldHaveGetHealthStatusMethod() throws NoSuchMethodException {
      Method method = ComponentEngineHealth.class.getMethod("getHealthStatus");
      assertNotNull(method, "getHealthStatus method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have isHealthy method")
    void shouldHaveIsHealthyMethod() throws NoSuchMethodException {
      Method method = ComponentEngineHealth.class.getMethod("isHealthy");
      assertNotNull(method, "isHealthy method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getLastHealthCheckTime method")
    void shouldHaveGetLastHealthCheckTimeMethod() throws NoSuchMethodException {
      Method method = ComponentEngineHealth.class.getMethod("getLastHealthCheckTime");
      assertNotNull(method, "getLastHealthCheckTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // ComponentEngineConfig Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentEngineConfig Class Tests")
  class ComponentEngineConfigTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(ComponentEngineConfig.class.getModifiers()),
          "ComponentEngineConfig should be a final class");
    }

    @Test
    @DisplayName("should have static builder factory method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      Method method = ComponentEngineConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be a static method");
    }

    @Test
    @DisplayName("should have componentModelEnabled method")
    void shouldHaveComponentModelEnabledMethod() throws NoSuchMethodException {
      Method method = ComponentEngineConfig.class.getMethod("componentModelEnabled", boolean.class);
      assertNotNull(method, "componentModelEnabled method should exist");
      assertEquals(
          ComponentEngineConfig.class,
          method.getReturnType(),
          "Return type should be ComponentEngineConfig");
    }

    @Test
    @DisplayName("should have enableAdvancedOrchestration method")
    void shouldHaveEnableAdvancedOrchestrationMethod() throws NoSuchMethodException {
      Method method =
          ComponentEngineConfig.class.getMethod("enableAdvancedOrchestration", boolean.class);
      assertNotNull(method, "enableAdvancedOrchestration method should exist");
      assertEquals(
          ComponentEngineConfig.class,
          method.getReturnType(),
          "Return type should be ComponentEngineConfig");
    }

    @Test
    @DisplayName("should have enableHotSwapping method")
    void shouldHaveEnableHotSwappingMethod() throws NoSuchMethodException {
      Method method = ComponentEngineConfig.class.getMethod("enableHotSwapping", boolean.class);
      assertNotNull(method, "enableHotSwapping method should exist");
      assertEquals(
          ComponentEngineConfig.class,
          method.getReturnType(),
          "Return type should be ComponentEngineConfig");
    }

    @Test
    @DisplayName("should have toEngineConfig method")
    void shouldHaveToEngineConfigMethod() throws NoSuchMethodException {
      Method method = ComponentEngineConfig.class.getMethod("toEngineConfig");
      assertNotNull(method, "toEngineConfig method should exist");
      assertEquals(
          EngineConfig.class, method.getReturnType(), "Return type should be EngineConfig");
    }

    @Test
    @DisplayName("should have ComponentEngineConfigBuilder inner class")
    void shouldHaveBuilderInnerClass() {
      Class<?>[] innerClasses = ComponentEngineConfig.class.getDeclaredClasses();
      boolean hasBuilder =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("ComponentEngineConfigBuilder"));
      assertTrue(hasBuilder, "Should have ComponentEngineConfigBuilder inner class");
    }
  }

  // ========================================================================
  // ComplexMarshalingService Tests
  // ========================================================================

  @Nested
  @DisplayName("ComplexMarshalingService Class Tests")
  class ComplexMarshalingServiceTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(ComplexMarshalingService.class.getModifiers()),
          "ComplexMarshalingService should be a final class");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
      try {
        ComplexMarshalingService.class.getConstructor();
      } catch (NoSuchMethodException e) {
        org.junit.jupiter.api.Assertions.fail(
            "ComplexMarshalingService should have a default constructor");
      }
    }

    @Test
    @DisplayName("should have marshal method")
    void shouldHaveMarshalMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.class.getMethod("marshal", Object.class);
      assertNotNull(method, "marshal method should exist");
    }

    @Test
    @DisplayName("should have unmarshal method")
    void shouldHaveUnmarshalMethod() throws NoSuchMethodException {
      Method method =
          ComplexMarshalingService.class.getDeclaredMethod(
              "unmarshal", ComplexMarshalingService.MarshaledData.class, Class.class);
      assertNotNull(method, "unmarshal method should exist");
    }

    @Test
    @DisplayName("should have createComplexValue method")
    void shouldHaveCreateComplexValueMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.class.getMethod("createComplexValue", Object.class);
      assertNotNull(method, "createComplexValue method should exist");
      assertEquals(
          WasmComplexValue.class, method.getReturnType(), "Return type should be WasmComplexValue");
    }

    @Test
    @DisplayName("should have estimateSerializedSize method")
    void shouldHaveEstimateSerializedSizeMethod() throws NoSuchMethodException {
      Method method =
          ComplexMarshalingService.class.getMethod("estimateSerializedSize", Object.class);
      assertNotNull(method, "estimateSerializedSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have MarshaledData inner class")
    void shouldHaveMarshaledDataInnerClass() {
      Class<?>[] innerClasses = ComplexMarshalingService.class.getDeclaredClasses();
      boolean hasMarshaledData =
          Arrays.stream(innerClasses).anyMatch(c -> c.getSimpleName().equals("MarshaledData"));
      assertTrue(hasMarshaledData, "Should have MarshaledData inner class");
    }

    @Test
    @DisplayName("should have MarshalingStrategy inner enum")
    void shouldHaveMarshalingStrategyInnerEnum() {
      Class<?>[] innerClasses = ComplexMarshalingService.class.getDeclaredClasses();
      boolean hasMarshalingStrategy =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("MarshalingStrategy") && c.isEnum());
      assertTrue(hasMarshalingStrategy, "Should have MarshalingStrategy inner enum");
    }
  }

  // ========================================================================
  // ComponentEngineStatistics Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentEngineStatistics Interface Tests")
  class ComponentEngineStatisticsTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ComponentEngineStatistics.class.isInterface(),
          "ComponentEngineStatistics should be an interface");
    }

    @Test
    @DisplayName("should have getComponentCount method")
    void shouldHaveGetComponentCountMethod() throws NoSuchMethodException {
      Method method = ComponentEngineStatistics.class.getMethod("getComponentCount");
      assertNotNull(method, "getComponentCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getInstanceCount method")
    void shouldHaveGetInstanceCountMethod() throws NoSuchMethodException {
      Method method = ComponentEngineStatistics.class.getMethod("getInstanceCount");
      assertNotNull(method, "getInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getMemoryUsage method")
    void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ComponentEngineStatistics.class.getMethod("getMemoryUsage");
      assertNotNull(method, "getMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getUptime method")
    void shouldHaveGetUptimeMethod() throws NoSuchMethodException {
      Method method = ComponentEngineStatistics.class.getMethod("getUptime");
      assertNotNull(method, "getUptime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // ComponentLinker Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentLinker Interface Tests")
  class ComponentLinkerTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ComponentLinker.class.isInterface(), "ComponentLinker should be an interface");
    }

    @Test
    @DisplayName("should be a generic interface")
    void shouldBeGenericInterface() {
      TypeVariable<?>[] typeParams = ComponentLinker.class.getTypeParameters();
      assertEquals(1, typeParams.length, "ComponentLinker should have one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have defineFunction method with namespace and interface")
    void shouldHaveDefineFunctionWithNamespaceMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "defineFunction",
              String.class,
              String.class,
              String.class,
              ComponentHostFunction.class);
      assertNotNull(method, "defineFunction method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have defineFunction method with WIT path")
    void shouldHaveDefineFunctionWithWitPathMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod(
              "defineFunction", String.class, ComponentHostFunction.class);
      assertNotNull(method, "defineFunction method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have defineInterface method")
    void shouldHaveDefineInterfaceMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("defineInterface", String.class, String.class, Map.class);
      assertNotNull(method, "defineInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      Method method =
          ComponentLinker.class.getMethod("instantiate", Store.class, Component.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(
          ComponentInstance.class,
          method.getReturnType(),
          "Return type should be ComponentInstance");
    }

    @Test
    @DisplayName("should have enableWasiPreview2 method")
    void shouldHaveEnableWasiPreview2Method() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("enableWasiPreview2");
      assertNotNull(method, "enableWasiPreview2 method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Return type should be Engine");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have hasInterface method")
    void shouldHaveHasInterfaceMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("hasInterface", String.class, String.class);
      assertNotNull(method, "hasInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getDefinedInterfaces method")
    void shouldHaveGetDefinedInterfacesMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("getDefinedInterfaces");
      assertNotNull(method, "getDefinedInterfaces method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have static create factory method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method method = ComponentLinker.class.getMethod("create", Engine.class);
      assertNotNull(method, "create static method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be a static method");
      assertEquals(
          ComponentLinker.class, method.getReturnType(), "Return type should be ComponentLinker");
    }
  }

  // ========================================================================
  // InstanceManager Tests
  // ========================================================================

  @Nested
  @DisplayName("InstanceManager Interface Tests")
  class InstanceManagerTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(InstanceManager.class.isInterface(), "InstanceManager should be an interface");
    }

    @Test
    @DisplayName("should have static create factory method with Engine")
    void shouldHaveStaticCreateWithEngineMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("create", Engine.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be a static method");
      assertEquals(
          InstanceManager.class, method.getReturnType(), "Return type should be InstanceManager");
    }

    @Test
    @DisplayName("should have getInstance method with Module")
    void shouldHaveGetInstanceWithModuleMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("getInstance", Module.class);
      assertNotNull(method, "getInstance method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Return type should be Instance");
    }

    @Test
    @DisplayName("should have getInstance method with Module and Linker")
    void shouldHaveGetInstanceWithModuleAndLinkerMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("getInstance", Module.class, Linker.class);
      assertNotNull(method, "getInstance method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Return type should be Instance");
    }

    @Test
    @DisplayName("should have getInstanceAsync method")
    void shouldHaveGetInstanceAsyncMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("getInstanceAsync", Module.class);
      assertNotNull(method, "getInstanceAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have returnInstance method")
    void shouldHaveReturnInstanceMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("returnInstance", Instance.class);
      assertNotNull(method, "returnInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have createPool method")
    void shouldHaveCreatePoolMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("createPool", Module.class, int.class);
      assertNotNull(method, "createPool method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have destroyPool method")
    void shouldHaveDestroyPoolMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("destroyPool", Module.class);
      assertNotNull(method, "destroyPool method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getPoolStatistics method")
    void shouldHaveGetPoolStatisticsMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("getPoolStatistics");
      assertNotNull(method, "getPoolStatistics method should exist");
    }

    @Test
    @DisplayName("should have setAutoScalingEnabled method")
    void shouldHaveSetAutoScalingEnabledMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("setAutoScalingEnabled", boolean.class);
      assertNotNull(method, "setAutoScalingEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isAutoScalingEnabled method")
    void shouldHaveIsAutoScalingEnabledMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("isAutoScalingEnabled");
      assertNotNull(method, "isAutoScalingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have shutdown method with Duration")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("shutdown", Duration.class);
      assertNotNull(method, "shutdown method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = InstanceManager.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have ExportFormat inner enum")
    void shouldHaveExportFormatEnum() {
      Class<?>[] innerClasses = InstanceManager.class.getDeclaredClasses();
      boolean hasExportFormat =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("ExportFormat") && c.isEnum());
      assertTrue(hasExportFormat, "Should have ExportFormat inner enum");
    }
  }

  // ========================================================================
  // WitInterfaceLinker Tests
  // ========================================================================

  @Nested
  @DisplayName("WitInterfaceLinker Class Tests")
  class WitInterfaceLinkerTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(WitInterfaceLinker.class.getModifiers()),
          "WitInterfaceLinker should be a final class");
    }

    @Test
    @DisplayName("should have linkComponents method")
    void shouldHaveLinkComponentsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceLinker.class.getMethod("linkComponents", List.class);
      assertNotNull(method, "linkComponents method should exist");
    }

    @Test
    @DisplayName("should have validateInterfaceCompatibility method")
    void shouldHaveValidateInterfaceCompatibilityMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceLinker.class.getMethod(
              "validateInterfaceCompatibility",
              Component.class,
              Component.class,
              String.class);
      assertNotNull(method, "validateInterfaceCompatibility method should exist");
    }

    @Test
    @DisplayName("should have bindInterface method")
    void shouldHaveBindInterfaceMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceLinker.class.getMethod(
              "bindInterface", Component.class, Component.class, String.class);
      assertNotNull(method, "bindInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have unbindInterface method")
    void shouldHaveUnbindInterfaceMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceLinker.class.getMethod(
              "unbindInterface", String.class, String.class, String.class);
      assertNotNull(method, "unbindInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getAllBindings method")
    void shouldHaveGetAllBindingsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceLinker.class.getMethod("getAllBindings");
      assertNotNull(method, "getAllBindings method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getBindingsForComponent method")
    void shouldHaveGetBindingsForComponentMethod() throws NoSuchMethodException {
      Method method = WitInterfaceLinker.class.getMethod("getBindingsForComponent", String.class);
      assertNotNull(method, "getBindingsForComponent method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have clearBindings method")
    void shouldHaveClearBindingsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceLinker.class.getMethod("clearBindings");
      assertNotNull(method, "clearBindings method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have InterfaceBinding inner class")
    void shouldHaveInterfaceBindingInnerClass() {
      Class<?>[] innerClasses = WitInterfaceLinker.class.getDeclaredClasses();
      boolean hasInterfaceBinding =
          Arrays.stream(innerClasses).anyMatch(c -> c.getSimpleName().equals("InterfaceBinding"));
      assertTrue(hasInterfaceBinding, "Should have InterfaceBinding inner class");
    }

    @Test
    @DisplayName("should have ComponentLinkType inner enum")
    void shouldHaveComponentLinkTypeEnum() {
      Class<?>[] innerClasses = WitInterfaceLinker.class.getDeclaredClasses();
      boolean hasComponentLinkType =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("ComponentLinkType") && c.isEnum());
      assertTrue(hasComponentLinkType, "Should have ComponentLinkType inner enum");
    }
  }

  // ========================================================================
  // WitTypeValidator Tests
  // ========================================================================

  @Nested
  @DisplayName("WitTypeValidator Class Tests")
  class WitTypeValidatorTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(WitTypeValidator.class.getModifiers()),
          "WitTypeValidator should be a final class");
    }

    @Test
    @DisplayName("should have validateType method")
    void shouldHaveValidateTypeMethod() throws NoSuchMethodException {
      Method method = WitTypeValidator.class.getMethod("validateType", WitType.class);
      assertNotNull(method, "validateType method should exist");
    }

    @Test
    @DisplayName("should have validateInterface method")
    void shouldHaveValidateInterfaceMethod() throws NoSuchMethodException {
      Method method =
          WitTypeValidator.class.getMethod("validateInterface", WitInterfaceDefinition.class);
      assertNotNull(method, "validateInterface method should exist");
    }

    @Test
    @DisplayName("should have validateTypeCompatibility method")
    void shouldHaveValidateTypeCompatibilityMethod() throws NoSuchMethodException {
      Method method =
          WitTypeValidator.class.getMethod(
              "validateTypeCompatibility", WitType.class, WitType.class);
      assertNotNull(method, "validateTypeCompatibility method should exist");
    }

    @Test
    @DisplayName("should have WitTypeValidationResult inner class")
    void shouldHaveWitTypeValidationResultInnerClass() {
      Class<?>[] innerClasses = WitTypeValidator.class.getDeclaredClasses();
      boolean hasResult =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("WitTypeValidationResult"));
      assertTrue(hasResult, "Should have WitTypeValidationResult inner class");
    }

    @Test
    @DisplayName("should have WitInterfaceValidationResult inner class")
    void shouldHaveWitInterfaceValidationResultInnerClass() {
      Class<?>[] innerClasses = WitTypeValidator.class.getDeclaredClasses();
      boolean hasResult =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("WitInterfaceValidationResult"));
      assertTrue(hasResult, "Should have WitInterfaceValidationResult inner class");
    }

    @Test
    @DisplayName("should have WitTypeCompatibilityResult inner class")
    void shouldHaveWitTypeCompatibilityResultInnerClass() {
      Class<?>[] innerClasses = WitTypeValidator.class.getDeclaredClasses();
      boolean hasResult =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("WitTypeCompatibilityResult"));
      assertTrue(hasResult, "Should have WitTypeCompatibilityResult inner class");
    }
  }

  // ========================================================================
  // ComponentLifecycleManager Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentLifecycleManager Interface Tests")
  class ComponentLifecycleManagerTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ComponentLifecycleManager.class.isInterface(),
          "ComponentLifecycleManager should be an interface");
    }

    @Test
    @DisplayName("should have startComponent method")
    void shouldHaveStartComponentMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("startComponent", String.class);
      assertNotNull(method, "startComponent method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have stopComponent method with graceful flag")
    void shouldHaveStopComponentWithGracefulMethod() throws NoSuchMethodException {
      Method method =
          ComponentLifecycleManager.class.getMethod("stopComponent", String.class, boolean.class);
      assertNotNull(method, "stopComponent method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have restartComponent method")
    void shouldHaveRestartComponentMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("restartComponent", String.class);
      assertNotNull(method, "restartComponent method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have pauseComponent method")
    void shouldHavePauseComponentMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("pauseComponent", String.class);
      assertNotNull(method, "pauseComponent method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have resumeComponent method")
    void shouldHaveResumeComponentMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("resumeComponent", String.class);
      assertNotNull(method, "resumeComponent method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getComponentState method")
    void shouldHaveGetComponentStateMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("getComponentState", String.class);
      assertNotNull(method, "getComponentState method should exist");
      assertEquals(
          ComponentLifecycleState.class,
          method.getReturnType(),
          "Return type should be ComponentLifecycleState");
    }

    @Test
    @DisplayName("should have getAllComponentStates method")
    void shouldHaveGetAllComponentStatesMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("getAllComponentStates");
      assertNotNull(method, "getAllComponentStates method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have isComponentRunning method")
    void shouldHaveIsComponentRunningMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("isComponentRunning", String.class);
      assertNotNull(method, "isComponentRunning method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isComponentHealthy method")
    void shouldHaveIsComponentHealthyMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("isComponentHealthy", String.class);
      assertNotNull(method, "isComponentHealthy method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have performHealthCheck method")
    void shouldHavePerformHealthCheckMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("performHealthCheck", String.class);
      assertNotNull(method, "performHealthCheck method should exist");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      Method method = ComponentLifecycleManager.class.getMethod("shutdown");
      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have ComponentRestartPolicy inner enum")
    void shouldHaveComponentRestartPolicyEnum() {
      Class<?>[] innerClasses = ComponentLifecycleManager.class.getDeclaredClasses();
      boolean hasPolicy =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("ComponentRestartPolicy") && c.isEnum());
      assertTrue(hasPolicy, "Should have ComponentRestartPolicy inner enum");
    }
  }

  // ========================================================================
  // DebugFrame Tests
  // ========================================================================

  @Nested
  @DisplayName("DebugFrame Class Tests")
  class DebugFrameTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(DebugFrame.class.getModifiers()), "DebugFrame should be a final class");
    }

    @Test
    @DisplayName("should have getFunctionIndex method")
    void shouldHaveGetFunctionIndexMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getFunctionIndex");
      assertNotNull(method, "getFunctionIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getInstructionOffset method")
    void shouldHaveGetInstructionOffsetMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getInstructionOffset");
      assertNotNull(method, "getInstructionOffset method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getLocals method")
    void shouldHaveGetLocalsMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getLocals");
      assertNotNull(method, "getLocals method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getLocal method")
    void shouldHaveGetLocalMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getLocal", int.class);
      assertNotNull(method, "getLocal method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "Return type should be WasmValue");
    }

    @Test
    @DisplayName("should have getLocalCount method")
    void shouldHaveGetLocalCountMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getLocalCount");
      assertNotNull(method, "getLocalCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getOperandStack method")
    void shouldHaveGetOperandStackMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getOperandStack");
      assertNotNull(method, "getOperandStack method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getStackDepth method")
    void shouldHaveGetStackDepthMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getStackDepth");
      assertNotNull(method, "getStackDepth method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getAttributes method")
    void shouldHaveGetAttributesMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getAttributes");
      assertNotNull(method, "getAttributes method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have getAttribute method")
    void shouldHaveGetAttributeMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("getAttribute", String.class);
      assertNotNull(method, "getAttribute method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
    }

    @Test
    @DisplayName("should have hasAttribute method")
    void shouldHaveHasAttributeMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("hasAttribute", String.class);
      assertNotNull(method, "hasAttribute method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have static builder factory method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      Method method = DebugFrame.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be a static method");
    }

    @Test
    @DisplayName("should have Builder inner class")
    void shouldHaveBuilderInnerClass() {
      Class<?>[] innerClasses = DebugFrame.class.getDeclaredClasses();
      boolean hasBuilder =
          Arrays.stream(innerClasses).anyMatch(c -> c.getSimpleName().equals("Builder"));
      assertTrue(hasBuilder, "Should have Builder inner class");
    }
  }

  // ========================================================================
  // WasmMemory Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmMemory Interface Tests")
  class WasmMemoryTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmMemory.class.isInterface(), "WasmMemory should be an interface");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have grow method")
    void shouldHaveGrowMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("grow", int.class);
      assertNotNull(method, "grow method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getMaxSize method")
    void shouldHaveGetMaxSizeMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("getMaxSize");
      assertNotNull(method, "getMaxSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getMemoryType method")
    void shouldHaveGetMemoryTypeMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("getMemoryType");
      assertNotNull(method, "getMemoryType method should exist");
      assertEquals(MemoryType.class, method.getReturnType(), "Return type should be MemoryType");
    }

    @Test
    @DisplayName("should have getBuffer method")
    void shouldHaveGetBufferMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("getBuffer");
      assertNotNull(method, "getBuffer method should exist");
      assertEquals(ByteBuffer.class, method.getReturnType(), "Return type should be ByteBuffer");
    }

    @Test
    @DisplayName("should have readByte method")
    void shouldHaveReadByteMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("readByte", int.class);
      assertNotNull(method, "readByte method should exist");
      assertEquals(byte.class, method.getReturnType(), "Return type should be byte");
    }

    @Test
    @DisplayName("should have writeByte method")
    void shouldHaveWriteByteMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("writeByte", int.class, byte.class);
      assertNotNull(method, "writeByte method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have readBytes method")
    void shouldHaveReadBytesMethod() throws NoSuchMethodException {
      Method method =
          WasmMemory.class.getMethod("readBytes", int.class, byte[].class, int.class, int.class);
      assertNotNull(method, "readBytes method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have writeBytes method")
    void shouldHaveWriteBytesMethod() throws NoSuchMethodException {
      Method method =
          WasmMemory.class.getMethod("writeBytes", int.class, byte[].class, int.class, int.class);
      assertNotNull(method, "writeBytes method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have readInt32 method")
    void shouldHaveReadInt32Method() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("readInt32", long.class);
      assertNotNull(method, "readInt32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have writeInt32 method")
    void shouldHaveWriteInt32Method() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("writeInt32", long.class, int.class);
      assertNotNull(method, "writeInt32 method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have readInt64 method")
    void shouldHaveReadInt64Method() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("readInt64", long.class);
      assertNotNull(method, "readInt64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have writeInt64 method")
    void shouldHaveWriteInt64Method() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("writeInt64", long.class, long.class);
      assertNotNull(method, "writeInt64 method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have readFloat32 method")
    void shouldHaveReadFloat32Method() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("readFloat32", long.class);
      assertNotNull(method, "readFloat32 method should exist");
      assertEquals(float.class, method.getReturnType(), "Return type should be float");
    }

    @Test
    @DisplayName("should have writeFloat32 method")
    void shouldHaveWriteFloat32Method() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("writeFloat32", long.class, float.class);
      assertNotNull(method, "writeFloat32 method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have readFloat64 method")
    void shouldHaveReadFloat64Method() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("readFloat64", long.class);
      assertNotNull(method, "readFloat64 method should exist");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("should have writeFloat64 method")
    void shouldHaveWriteFloat64Method() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("writeFloat64", long.class, double.class);
      assertNotNull(method, "writeFloat64 method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have dataPtr method")
    void shouldHaveDataPtrMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("dataPtr");
      assertNotNull(method, "dataPtr method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have dataSize method")
    void shouldHaveDataSizeMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("dataSize");
      assertNotNull(method, "dataSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have pageSize method")
    void shouldHavePageSizeMethod() throws NoSuchMethodException {
      Method method = WasmMemory.class.getMethod("pageSize");
      assertNotNull(method, "pageSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }
  }

  // ========================================================================
  // StoreBuilder Tests
  // ========================================================================

  @Nested
  @DisplayName("StoreBuilder Class Tests")
  class StoreBuilderTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(StoreBuilder.class.getModifiers()),
          "StoreBuilder should be a final class");
    }

    @Test
    @DisplayName("should be a generic class")
    void shouldBeGenericClass() {
      TypeVariable<?>[] typeParams = StoreBuilder.class.getTypeParameters();
      assertEquals(1, typeParams.length, "StoreBuilder should have one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have withData method")
    void shouldHaveWithDataMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getMethod("withData", Object.class);
      assertNotNull(method, "withData method should exist");
      assertEquals(
          StoreBuilder.class,
          method.getReturnType(),
          "Return type should be StoreBuilder for fluent API");
    }

    @Test
    @DisplayName("should have withFuel method")
    void shouldHaveWithFuelMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getMethod("withFuel", long.class);
      assertNotNull(method, "withFuel method should exist");
      assertEquals(
          StoreBuilder.class,
          method.getReturnType(),
          "Return type should be StoreBuilder for fluent API");
    }

    @Test
    @DisplayName("should have withEpochDeadline method")
    void shouldHaveWithEpochDeadlineMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getMethod("withEpochDeadline", long.class);
      assertNotNull(method, "withEpochDeadline method should exist");
      assertEquals(
          StoreBuilder.class,
          method.getReturnType(),
          "Return type should be StoreBuilder for fluent API");
    }

    @Test
    @DisplayName("should have withLimits method")
    void shouldHaveWithLimitsMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getMethod("withLimits", StoreLimits.class);
      assertNotNull(method, "withLimits method should exist");
      assertEquals(
          StoreBuilder.class,
          method.getReturnType(),
          "Return type should be StoreBuilder for fluent API");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(Store.class, method.getReturnType(), "Return type should be Store");
    }

    @Test
    @DisplayName("should have getEngine getter method (package-private)")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getDeclaredMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Return type should be Engine");
    }

    @Test
    @DisplayName("should have getData getter method (package-private)")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getDeclaredMethod("getData");
      assertNotNull(method, "getData method should exist");
    }

    @Test
    @DisplayName("should have getFuel getter method (package-private)")
    void shouldHaveGetFuelMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getDeclaredMethod("getFuel");
      assertNotNull(method, "getFuel method should exist");
      assertEquals(Long.class, method.getReturnType(), "Return type should be Long");
    }

    @Test
    @DisplayName("should have getEpochDeadline getter method (package-private)")
    void shouldHaveGetEpochDeadlineMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getDeclaredMethod("getEpochDeadline");
      assertNotNull(method, "getEpochDeadline method should exist");
      assertEquals(Long.class, method.getReturnType(), "Return type should be Long");
    }

    @Test
    @DisplayName("should have getLimits getter method (package-private)")
    void shouldHaveGetLimitsMethod() throws NoSuchMethodException {
      Method method = StoreBuilder.class.getDeclaredMethod("getLimits");
      assertNotNull(method, "getLimits method should exist");
      assertEquals(StoreLimits.class, method.getReturnType(), "Return type should be StoreLimits");
    }
  }
}
