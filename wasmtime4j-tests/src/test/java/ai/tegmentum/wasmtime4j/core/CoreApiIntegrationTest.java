/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.FrameInfo;
import ai.tegmentum.wasmtime4j.FunctionContext;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.ImportDescriptor;
import ai.tegmentum.wasmtime4j.InstanceManager;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.WasmExecutionContext;
import ai.tegmentum.wasmtime4j.WasmInstance;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Core API interfaces and classes.
 *
 * <p>Tests WasmInstance, InstanceManager, FunctionContext, ExportDescriptor, ImportDescriptor,
 * FrameInfo, and related core APIs.
 */
@DisplayName("Core API Integration Tests")
public class CoreApiIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(CoreApiIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Core API Integration Tests");
  }

  @Nested
  @DisplayName("WasmInstance Interface Tests")
  class WasmInstanceInterfaceTests {

    @Test
    @DisplayName("Should verify WasmInstance is an interface extending Closeable")
    void shouldVerifyWasmInstanceIsInterface() {
      LOGGER.info("Testing WasmInstance interface structure");

      assertTrue(WasmInstance.class.isInterface(), "WasmInstance should be an interface");
      assertTrue(
          java.io.Closeable.class.isAssignableFrom(WasmInstance.class),
          "WasmInstance should extend Closeable");

      LOGGER.info("WasmInstance interface structure verified");
    }

    @Test
    @DisplayName("Should have export retrieval methods")
    void shouldHaveExportRetrievalMethods() throws Exception {
      LOGGER.info("Testing WasmInstance export retrieval methods");

      Method getFunction = WasmInstance.class.getMethod("getFunction", String.class);
      assertNotNull(getFunction, "getFunction method should exist");
      assertEquals(Optional.class, getFunction.getReturnType(), "Should return Optional");

      Method getMemory = WasmInstance.class.getMethod("getMemory", String.class);
      assertNotNull(getMemory, "getMemory method should exist");

      Method getTable = WasmInstance.class.getMethod("getTable", String.class);
      assertNotNull(getTable, "getTable method should exist");

      Method getGlobal = WasmInstance.class.getMethod("getGlobal", String.class);
      assertNotNull(getGlobal, "getGlobal method should exist");

      Method getDefaultMemory = WasmInstance.class.getMethod("getDefaultMemory");
      assertNotNull(getDefaultMemory, "getDefaultMemory method should exist");

      LOGGER.info("WasmInstance export retrieval methods verified");
    }

    @Test
    @DisplayName("Should have export listing methods")
    void shouldHaveExportListingMethods() throws Exception {
      LOGGER.info("Testing WasmInstance export listing methods");

      Method getFunctionNames = WasmInstance.class.getMethod("getFunctionNames");
      assertNotNull(getFunctionNames, "getFunctionNames method should exist");
      assertEquals(List.class, getFunctionNames.getReturnType(), "Should return List");

      Method getMemoryNames = WasmInstance.class.getMethod("getMemoryNames");
      assertNotNull(getMemoryNames, "getMemoryNames method should exist");

      Method getTableNames = WasmInstance.class.getMethod("getTableNames");
      assertNotNull(getTableNames, "getTableNames method should exist");

      Method getGlobalNames = WasmInstance.class.getMethod("getGlobalNames");
      assertNotNull(getGlobalNames, "getGlobalNames method should exist");

      LOGGER.info("WasmInstance export listing methods verified");
    }

    @Test
    @DisplayName("Should have instance lifecycle methods")
    void shouldHaveInstanceLifecycleMethods() throws Exception {
      LOGGER.info("Testing WasmInstance lifecycle methods");

      Method isValid = WasmInstance.class.getMethod("isValid");
      assertNotNull(isValid, "isValid method should exist");
      assertEquals(boolean.class, isValid.getReturnType(), "Should return boolean");

      Method getModule = WasmInstance.class.getMethod("getModule");
      assertNotNull(getModule, "getModule method should exist");

      Method close = WasmInstance.class.getMethod("close");
      assertNotNull(close, "close method should exist");

      LOGGER.info("WasmInstance lifecycle methods verified");
    }

    @Test
    @DisplayName("Should have ExportType enum")
    void shouldHaveExportTypeEnum() {
      LOGGER.info("Testing WasmInstance.ExportType enum");

      WasmInstance.ExportType[] types = WasmInstance.ExportType.values();
      assertEquals(4, types.length, "Should have 4 export types");

      assertNotNull(WasmInstance.ExportType.FUNCTION, "FUNCTION should exist");
      assertNotNull(WasmInstance.ExportType.MEMORY, "MEMORY should exist");
      assertNotNull(WasmInstance.ExportType.TABLE, "TABLE should exist");
      assertNotNull(WasmInstance.ExportType.GLOBAL, "GLOBAL should exist");

      LOGGER.info("WasmInstance.ExportType enum verified");
    }
  }

  @Nested
  @DisplayName("InstanceManager Interface Tests")
  class InstanceManagerInterfaceTests {

    @Test
    @DisplayName("Should verify InstanceManager is an interface extending AutoCloseable")
    void shouldVerifyInstanceManagerIsInterface() {
      LOGGER.info("Testing InstanceManager interface structure");

      assertTrue(InstanceManager.class.isInterface(), "InstanceManager should be an interface");
      assertTrue(
          AutoCloseable.class.isAssignableFrom(InstanceManager.class),
          "InstanceManager should extend AutoCloseable");

      LOGGER.info("InstanceManager interface structure verified");
    }

    @Test
    @DisplayName("Should have static factory methods")
    void shouldHaveStaticFactoryMethods() throws Exception {
      LOGGER.info("Testing InstanceManager static factory methods");

      Method[] methods = InstanceManager.class.getMethods();
      int createMethods = 0;
      for (Method m : methods) {
        if (m.getName().equals("create") && Modifier.isStatic(m.getModifiers())) {
          createMethods++;
        }
      }
      assertTrue(createMethods >= 2, "Should have at least 2 create methods");

      LOGGER.info("InstanceManager static factory methods verified");
    }

    @Test
    @DisplayName("Should have instance pool methods")
    void shouldHaveInstancePoolMethods() throws Exception {
      LOGGER.info("Testing InstanceManager pool methods");

      assertNotNull(
          InstanceManager.class.getMethod("getInstance", ai.tegmentum.wasmtime4j.Module.class),
          "getInstance(Module) should exist");

      assertNotNull(
          InstanceManager.class.getMethod("returnInstance", ai.tegmentum.wasmtime4j.Instance.class),
          "returnInstance should exist");

      assertNotNull(
          InstanceManager.class.getMethod(
              "createPool", ai.tegmentum.wasmtime4j.Module.class, int.class),
          "createPool should exist");

      assertNotNull(
          InstanceManager.class.getMethod("destroyPool", ai.tegmentum.wasmtime4j.Module.class),
          "destroyPool should exist");

      LOGGER.info("InstanceManager pool methods verified");
    }

    @Test
    @DisplayName("Should have scaling methods")
    void shouldHaveScalingMethods() throws Exception {
      LOGGER.info("Testing InstanceManager scaling methods");

      assertNotNull(
          InstanceManager.class.getMethod("setAutoScalingEnabled", boolean.class),
          "setAutoScalingEnabled should exist");

      assertNotNull(
          InstanceManager.class.getMethod("isAutoScalingEnabled"),
          "isAutoScalingEnabled should exist");

      assertNotNull(
          InstanceManager.class.getMethod(
              "scalePool", ai.tegmentum.wasmtime4j.Module.class, int.class),
          "scalePool should exist");

      assertNotNull(InstanceManager.class.getMethod("balanceLoad"), "balanceLoad should exist");

      LOGGER.info("InstanceManager scaling methods verified");
    }

    @Test
    @DisplayName("Should have health monitoring methods")
    void shouldHaveHealthMonitoringMethods() throws Exception {
      LOGGER.info("Testing InstanceManager health methods");

      assertNotNull(
          InstanceManager.class.getMethod("getPoolStatistics"), "getPoolStatistics should exist");

      assertNotNull(
          InstanceManager.class.getMethod("getInstanceHealth"), "getInstanceHealth should exist");

      assertNotNull(
          InstanceManager.class.getMethod("performHealthCheck"), "performHealthCheck should exist");

      LOGGER.info("InstanceManager health methods verified");
    }

    @Test
    @DisplayName("Should have checkpoint methods")
    void shouldHaveCheckpointMethods() throws Exception {
      LOGGER.info("Testing InstanceManager checkpoint methods");

      assertNotNull(
          InstanceManager.class.getMethod(
              "createCheckpoint", ai.tegmentum.wasmtime4j.Instance.class),
          "createCheckpoint should exist");

      assertNotNull(
          InstanceManager.class.getMethod(
              "restoreFromCheckpoint", InstanceManager.InstanceCheckpoint.class),
          "restoreFromCheckpoint should exist");

      LOGGER.info("InstanceManager checkpoint methods verified");
    }

    @Test
    @DisplayName("Should have nested configuration interface")
    void shouldHaveNestedConfigurationInterface() {
      LOGGER.info("Testing InstanceManager nested interfaces");

      assertTrue(
          InstanceManager.InstanceManagerConfig.class.isInterface(),
          "InstanceManagerConfig should be an interface");

      assertTrue(
          InstanceManager.InstancePoolStatistics.class.isInterface(),
          "InstancePoolStatistics should be an interface");

      assertTrue(
          InstanceManager.InstanceHealthStatus.class.isInterface(),
          "InstanceHealthStatus should be an interface");

      assertTrue(
          InstanceManager.InstancePerformanceMetrics.class.isInterface(),
          "InstancePerformanceMetrics should be an interface");

      assertTrue(
          InstanceManager.InstanceCheckpoint.class.isInterface(),
          "InstanceCheckpoint should be an interface");

      assertTrue(
          InstanceManager.MaintenanceSummary.class.isInterface(),
          "MaintenanceSummary should be an interface");

      LOGGER.info("InstanceManager nested interfaces verified");
    }

    @Test
    @DisplayName("Should have ExportFormat enum")
    void shouldHaveExportFormatEnum() {
      LOGGER.info("Testing InstanceManager.ExportFormat enum");

      InstanceManager.ExportFormat[] formats = InstanceManager.ExportFormat.values();
      assertEquals(4, formats.length, "Should have 4 export formats");

      assertNotNull(InstanceManager.ExportFormat.JSON, "JSON should exist");
      assertNotNull(InstanceManager.ExportFormat.XML, "XML should exist");
      assertNotNull(InstanceManager.ExportFormat.CSV, "CSV should exist");
      assertNotNull(InstanceManager.ExportFormat.YAML, "YAML should exist");

      LOGGER.info("InstanceManager.ExportFormat enum verified");
    }
  }

  @Nested
  @DisplayName("InstancePre Interface Tests")
  class InstancePreInterfaceTests {

    @Test
    @DisplayName("Should verify InstancePre is an interface")
    void shouldVerifyInstancePreIsInterface() {
      LOGGER.info("Testing InstancePre interface structure");

      assertTrue(InstancePre.class.isInterface(), "InstancePre should be an interface");

      LOGGER.info("InstancePre interface structure verified");
    }

    @Test
    @DisplayName("Should have instantiation methods")
    void shouldHaveInstantiationMethods() throws Exception {
      LOGGER.info("Testing InstancePre instantiation methods");

      Method instantiate =
          InstancePre.class.getMethod("instantiate", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(instantiate, "instantiate method should exist");

      LOGGER.info("InstancePre instantiation methods verified");
    }
  }

  @Nested
  @DisplayName("FunctionContext Interface Tests")
  class FunctionContextInterfaceTests {

    @Test
    @DisplayName("Should verify FunctionContext is an interface")
    void shouldVerifyFunctionContextIsInterface() {
      LOGGER.info("Testing FunctionContext interface structure");

      assertTrue(FunctionContext.class.isInterface(), "FunctionContext should be an interface");

      LOGGER.info("FunctionContext interface structure verified");
    }

    @Test
    @DisplayName("Should have function info methods")
    void shouldHaveFunctionInfoMethods() throws Exception {
      LOGGER.info("Testing FunctionContext function info methods");

      assertNotNull(
          FunctionContext.class.getMethod("getFunctionName"), "getFunctionName should exist");
      assertNotNull(
          FunctionContext.class.getMethod("getFunctionIndex"), "getFunctionIndex should exist");
      assertNotNull(
          FunctionContext.class.getMethod("getFunctionType"), "getFunctionType should exist");
      assertNotNull(
          FunctionContext.class.getMethod("getParameterTypes"), "getParameterTypes should exist");
      assertNotNull(
          FunctionContext.class.getMethod("getReturnTypes"), "getReturnTypes should exist");

      LOGGER.info("FunctionContext function info methods verified");
    }

    @Test
    @DisplayName("Should have tail call methods")
    void shouldHaveTailCallMethods() throws Exception {
      LOGGER.info("Testing FunctionContext tail call methods");

      assertNotNull(
          FunctionContext.class.getMethod("supportsTailCalls"), "supportsTailCalls should exist");
      assertNotNull(FunctionContext.class.getMethod("isRecursive"), "isRecursive should exist");
      assertNotNull(
          FunctionContext.class.getMethod("isTailRecursive"), "isTailRecursive should exist");
      assertNotNull(
          FunctionContext.class.getMethod("getRecursionDepthLimit"),
          "getRecursionDepthLimit should exist");
      assertNotNull(
          FunctionContext.class.getMethod("getCurrentCallDepth"),
          "getCurrentCallDepth should exist");

      LOGGER.info("FunctionContext tail call methods verified");
    }

    @Test
    @DisplayName("Should have optimization and metrics methods")
    void shouldHaveOptimizationAndMetricsMethods() throws Exception {
      LOGGER.info("Testing FunctionContext optimization methods");

      assertNotNull(
          FunctionContext.class.getMethod("getOptimization"), "getOptimization should exist");
      assertNotNull(FunctionContext.class.getMethod("getMetrics"), "getMetrics should exist");

      LOGGER.info("FunctionContext optimization methods verified");
    }

    @Test
    @DisplayName("Should have nested interfaces")
    void shouldHaveNestedInterfaces() {
      LOGGER.info("Testing FunctionContext nested interfaces");

      assertTrue(
          FunctionContext.FunctionOptimization.class.isInterface(),
          "FunctionOptimization should be an interface");
      assertTrue(
          FunctionContext.FunctionMetrics.class.isInterface(),
          "FunctionMetrics should be an interface");

      LOGGER.info("FunctionContext nested interfaces verified");
    }

    @Test
    @DisplayName("Should create FunctionContext via static factory")
    void shouldCreateFunctionContextViaStaticFactory() {
      LOGGER.info("Testing FunctionContext.create()");

      FunctionType funcType =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      FunctionContext ctx = FunctionContext.create("testFunc", 0, funcType);
      assertNotNull(ctx, "FunctionContext should not be null");
      assertEquals("testFunc", ctx.getFunctionName(), "Function name should match");
      assertEquals(0, ctx.getFunctionIndex(), "Function index should match");
      assertEquals(funcType, ctx.getFunctionType(), "Function type should match");

      LOGGER.info("FunctionContext.create() verified");
    }

    @Test
    @DisplayName("Should create FunctionContext with full parameters")
    void shouldCreateFunctionContextWithFullParameters() {
      LOGGER.info("Testing FunctionContext.create() with full parameters");

      FunctionType funcType =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I64});

      FunctionContext ctx = FunctionContext.create("add", 5, funcType, true, 128);
      assertNotNull(ctx, "FunctionContext should not be null");
      assertEquals("add", ctx.getFunctionName(), "Function name should match");
      assertEquals(5, ctx.getFunctionIndex(), "Function index should match");
      assertTrue(ctx.supportsTailCalls(), "Should support tail calls");
      assertEquals(128, ctx.getStackFrameSize(), "Stack frame size should match");

      LOGGER.info("FunctionContext.create() with full parameters verified");
    }

    @Test
    @DisplayName("Should have working optimization info")
    void shouldHaveWorkingOptimizationInfo() {
      LOGGER.info("Testing FunctionContext optimization info");

      FunctionType funcType =
          FunctionType.of(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
      FunctionContext ctx = FunctionContext.create("test", 0, funcType);

      FunctionContext.FunctionOptimization opt = ctx.getOptimization();
      assertNotNull(opt, "Optimization should not be null");

      // Test all optimization methods don't throw
      boolean jit = opt.isJitCompiled();
      boolean inlined = opt.isInlined();
      boolean tailCallOpt = opt.hasTailCallOptimization();
      boolean framePointer = opt.hasFramePointerElimination();
      WasmExecutionContext.OptimizationLevel level = opt.getOptimizationLevel();
      int cost = opt.getCompilationCost();
      double gain = opt.getPerformanceGain();

      assertNotNull(level, "Optimization level should not be null");
      assertTrue(cost >= 0, "Compilation cost should be non-negative");
      assertTrue(gain >= 0, "Performance gain should be non-negative");

      LOGGER.info("FunctionContext optimization info verified");
    }

    @Test
    @DisplayName("Should have working metrics info")
    void shouldHaveWorkingMetricsInfo() {
      LOGGER.info("Testing FunctionContext metrics info");

      FunctionType funcType = FunctionType.of(new WasmValueType[0], new WasmValueType[0]);
      FunctionContext ctx = FunctionContext.create("test", 0, funcType);

      FunctionContext.FunctionMetrics metrics = ctx.getMetrics();
      assertNotNull(metrics, "Metrics should not be null");

      assertEquals(0, metrics.getCallCount(), "Initial call count should be 0");
      assertEquals(0, metrics.getTotalExecutionTime(), "Initial execution time should be 0");
      assertEquals(0.0, metrics.getAverageExecutionTime(), 0.001, "Initial avg time should be 0");
      assertEquals(0, metrics.getTailCallCount(), "Initial tail call count should be 0");

      LOGGER.info("FunctionContext metrics info verified");
    }
  }

  @Nested
  @DisplayName("ExportDescriptor Interface Tests")
  class ExportDescriptorInterfaceTests {

    @Test
    @DisplayName("Should verify ExportDescriptor is an interface")
    void shouldVerifyExportDescriptorIsInterface() {
      LOGGER.info("Testing ExportDescriptor interface structure");

      assertTrue(ExportDescriptor.class.isInterface(), "ExportDescriptor should be an interface");

      LOGGER.info("ExportDescriptor interface structure verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing ExportDescriptor methods");

      assertNotNull(ExportDescriptor.class.getMethod("getName"), "getName should exist");
      assertNotNull(ExportDescriptor.class.getMethod("getType"), "getType should exist");
      assertNotNull(ExportDescriptor.class.getMethod("getKind"), "getKind should exist");

      LOGGER.info("ExportDescriptor methods verified");
    }

    @Test
    @DisplayName("Should have type checking methods")
    void shouldHaveTypeCheckingMethods() throws Exception {
      LOGGER.info("Testing ExportDescriptor type checking methods");

      assertNotNull(ExportDescriptor.class.getMethod("isFunction"), "isFunction should exist");
      assertNotNull(ExportDescriptor.class.getMethod("isGlobal"), "isGlobal should exist");
      assertNotNull(ExportDescriptor.class.getMethod("isMemory"), "isMemory should exist");
      assertNotNull(ExportDescriptor.class.getMethod("isTable"), "isTable should exist");

      LOGGER.info("ExportDescriptor type checking methods verified");
    }

    @Test
    @DisplayName("Should have type cast methods")
    void shouldHaveTypeCastMethods() throws Exception {
      LOGGER.info("Testing ExportDescriptor type cast methods");

      assertNotNull(
          ExportDescriptor.class.getMethod("asFunctionType"), "asFunctionType should exist");
      assertNotNull(ExportDescriptor.class.getMethod("asGlobalType"), "asGlobalType should exist");
      assertNotNull(ExportDescriptor.class.getMethod("asMemoryType"), "asMemoryType should exist");
      assertNotNull(ExportDescriptor.class.getMethod("asTableType"), "asTableType should exist");

      LOGGER.info("ExportDescriptor type cast methods verified");
    }
  }

  @Nested
  @DisplayName("ImportDescriptor Interface Tests")
  class ImportDescriptorInterfaceTests {

    @Test
    @DisplayName("Should verify ImportDescriptor is an interface")
    void shouldVerifyImportDescriptorIsInterface() {
      LOGGER.info("Testing ImportDescriptor interface structure");

      assertTrue(ImportDescriptor.class.isInterface(), "ImportDescriptor should be an interface");

      LOGGER.info("ImportDescriptor interface structure verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing ImportDescriptor methods");

      assertNotNull(
          ImportDescriptor.class.getMethod("getModuleName"), "getModuleName should exist");
      assertNotNull(ImportDescriptor.class.getMethod("getName"), "getName should exist");
      assertNotNull(ImportDescriptor.class.getMethod("getType"), "getType should exist");
      assertNotNull(ImportDescriptor.class.getMethod("getKind"), "getKind should exist");

      LOGGER.info("ImportDescriptor methods verified");
    }

    @Test
    @DisplayName("Should have type checking methods")
    void shouldHaveTypeCheckingMethods() throws Exception {
      LOGGER.info("Testing ImportDescriptor type checking methods");

      assertNotNull(ImportDescriptor.class.getMethod("isFunction"), "isFunction should exist");
      assertNotNull(ImportDescriptor.class.getMethod("isGlobal"), "isGlobal should exist");
      assertNotNull(ImportDescriptor.class.getMethod("isMemory"), "isMemory should exist");
      assertNotNull(ImportDescriptor.class.getMethod("isTable"), "isTable should exist");

      LOGGER.info("ImportDescriptor type checking methods verified");
    }
  }

  @Nested
  @DisplayName("FrameInfo Class Tests")
  class FrameInfoClassTests {

    @Test
    @DisplayName("Should verify FrameInfo is a final class")
    void shouldVerifyFrameInfoIsFinalClass() {
      LOGGER.info("Testing FrameInfo class structure");

      assertTrue(Modifier.isFinal(FrameInfo.class.getModifiers()), "FrameInfo should be final");
      assertFalse(FrameInfo.class.isInterface(), "FrameInfo should not be an interface");

      LOGGER.info("FrameInfo class structure verified");
    }

    @Test
    @DisplayName("Should create FrameInfo with all parameters")
    void shouldCreateFrameInfoWithAllParameters() {
      LOGGER.info("Testing FrameInfo constructor");

      FrameInfo frame = new FrameInfo(5, null, "myFunction", 100, 20, Collections.emptyList());

      assertEquals(5, frame.getFuncIndex(), "Function index should match");
      assertEquals(Optional.of("myFunction"), frame.getFuncName(), "Function name should match");
      assertEquals(Optional.of(100), frame.getModuleOffset(), "Module offset should match");
      assertEquals(Optional.of(20), frame.getFuncOffset(), "Function offset should match");
      assertTrue(frame.getSymbols().isEmpty(), "Symbols should be empty");

      LOGGER.info("FrameInfo constructor verified");
    }

    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
      LOGGER.info("Testing FrameInfo with null optional fields");

      FrameInfo frame = new FrameInfo(0, null, null, null, null, null);

      assertEquals(0, frame.getFuncIndex(), "Function index should be 0");
      assertEquals(Optional.empty(), frame.getFuncName(), "Function name should be empty");
      assertEquals(Optional.empty(), frame.getModuleOffset(), "Module offset should be empty");
      assertEquals(Optional.empty(), frame.getFuncOffset(), "Function offset should be empty");
      assertTrue(frame.getSymbols().isEmpty(), "Symbols should be empty");

      LOGGER.info("FrameInfo null handling verified");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing FrameInfo equals");

      FrameInfo frame1 = new FrameInfo(5, null, "func", 100, 20, Collections.emptyList());
      FrameInfo frame2 = new FrameInfo(5, null, "func", 100, 20, Collections.emptyList());
      FrameInfo frame3 = new FrameInfo(6, null, "func", 100, 20, Collections.emptyList());

      assertEquals(frame1, frame1, "Frame should equal itself");
      assertEquals(frame1, frame2, "Equal frames should be equal");
      assertNotEquals(frame1, frame3, "Different frames should not be equal");
      assertNotEquals(frame1, null, "Frame should not equal null");

      LOGGER.info("FrameInfo equals verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing FrameInfo hashCode");

      FrameInfo frame1 = new FrameInfo(5, null, "func", 100, 20, Collections.emptyList());
      FrameInfo frame2 = new FrameInfo(5, null, "func", 100, 20, Collections.emptyList());

      assertEquals(frame1.hashCode(), frame2.hashCode(), "Equal frames should have same hashCode");

      LOGGER.info("FrameInfo hashCode verified");
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      LOGGER.info("Testing FrameInfo toString");

      FrameInfo frame = new FrameInfo(5, null, "myFunction", 100, 20, Collections.emptyList());
      String str = frame.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("FrameInfo"), "toString should contain class name");
      assertTrue(str.contains("5"), "toString should contain function index");
      assertTrue(str.contains("myFunction"), "toString should contain function name");

      LOGGER.info("FrameInfo toString: " + str);
    }
  }

  @Nested
  @DisplayName("WasmExecutionContext Interface Tests")
  class WasmExecutionContextInterfaceTests {

    @Test
    @DisplayName("Should verify WasmExecutionContext is an interface")
    void shouldVerifyWasmExecutionContextIsInterface() {
      LOGGER.info("Testing WasmExecutionContext interface structure");

      assertTrue(
          WasmExecutionContext.class.isInterface(), "WasmExecutionContext should be an interface");

      LOGGER.info("WasmExecutionContext interface structure verified");
    }

    @Test
    @DisplayName("Should have OptimizationLevel enum")
    void shouldHaveOptimizationLevelEnum() {
      LOGGER.info("Testing WasmExecutionContext.OptimizationLevel enum");

      WasmExecutionContext.OptimizationLevel[] levels =
          WasmExecutionContext.OptimizationLevel.values();
      assertTrue(levels.length >= 3, "Should have at least 3 optimization levels");

      assertNotNull(WasmExecutionContext.OptimizationLevel.NONE, "NONE should exist");
      assertNotNull(WasmExecutionContext.OptimizationLevel.BASIC, "BASIC should exist");

      LOGGER.info("WasmExecutionContext.OptimizationLevel enum verified");
    }
  }

  @Nested
  @DisplayName("InstanceManagerConfig Tests")
  class InstanceManagerConfigTests {

    @Test
    @DisplayName("Should create default config")
    void shouldCreateDefaultConfig() {
      LOGGER.info("Testing InstanceManagerConfig.defaultConfig()");

      InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.defaultConfig();
      assertNotNull(config, "Default config should not be null");

      // Verify config methods don't throw
      int defaultPoolSize = config.getDefaultPoolSize();
      int maxPoolSize = config.getMaxPoolSize();
      double threshold = config.getScalingThreshold();

      assertTrue(defaultPoolSize >= 0, "Default pool size should be non-negative");
      assertTrue(maxPoolSize >= defaultPoolSize, "Max pool size should be >= default");
      assertTrue(threshold >= 0.0 && threshold <= 1.0, "Threshold should be between 0 and 1");
      assertNotNull(config.getHealthCheckInterval(), "Health interval should not be null");
      assertNotNull(config.getInstanceTimeout(), "Timeout should not be null");

      // Verify boolean config methods don't throw (values not asserted since defaults vary)
      config.isAutoScalingEnabled();
      config.isHealthMonitoringEnabled();
      config.isMigrationEnabled();
      config.isCheckpointingEnabled();

      LOGGER.info("InstanceManagerConfig.defaultConfig() verified");
    }

    @Test
    @DisplayName("Should create config via builder")
    void shouldCreateConfigViaBuilder() {
      LOGGER.info("Testing InstanceManagerConfig.builder()");

      InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      assertNotNull(builder, "Builder should not be null");

      InstanceManager.InstanceManagerConfig config =
          builder
              .defaultPoolSize(10)
              .maxPoolSize(100)
              .autoScalingEnabled(true)
              .scalingThreshold(0.8)
              .healthMonitoringEnabled(true)
              .healthCheckInterval(Duration.ofSeconds(30))
              .migrationEnabled(false)
              .checkpointingEnabled(true)
              .instanceTimeout(Duration.ofMinutes(5))
              .build();

      assertNotNull(config, "Config should not be null");
      assertEquals(10, config.getDefaultPoolSize(), "Default pool size should match");
      assertEquals(100, config.getMaxPoolSize(), "Max pool size should match");
      assertTrue(config.isAutoScalingEnabled(), "Auto scaling should be enabled");
      assertEquals(0.8, config.getScalingThreshold(), 0.001, "Threshold should match");

      LOGGER.info("InstanceManagerConfig.builder() verified");
    }
  }
}
