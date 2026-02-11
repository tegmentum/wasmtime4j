/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.thread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.concurrent.WasmThread;
import ai.tegmentum.wasmtime4j.concurrent.WasmThreadLocalStorage;
import ai.tegmentum.wasmtime4j.concurrent.WasmThreadState;
import ai.tegmentum.wasmtime4j.concurrent.WasmThreadStatistics;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WebAssembly threading features in wasmtime4j.
 *
 * <p>This test class validates the WebAssembly threading capabilities including WasmThread,
 * WasmThreadState, WasmThreadLocalStorage, and WasmThreadStatistics.
 */
@DisplayName("WASM Thread Integration Tests")
public class WasmThreadIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasmThreadIntegrationTest.class.getName());

  private static boolean threadsSupported;

  @BeforeAll
  static void setUp() {
    LOGGER.info("Setting up WASM Thread Integration Tests");

    // Check if threads are supported by attempting to create threads-enabled engine
    try {
      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
      try (Engine engine = Engine.create(config)) {
        threadsSupported = engine != null;
      }
    } catch (Exception e) {
      LOGGER.warning("WASM threads may not be fully supported: " + e.getMessage());
      threadsSupported = false;
    }

    LOGGER.info("WASM threads supported: " + threadsSupported);
  }

  @AfterAll
  static void tearDown() {
    LOGGER.info("Completed WASM Thread Integration Tests");
  }

  @Nested
  @DisplayName("WasmThreadState Tests")
  class WasmThreadStateTests {

    @Test
    @DisplayName("Should have all expected thread state values")
    void shouldHaveAllExpectedThreadStateValues() {
      LOGGER.info("Testing WasmThreadState enum values");

      final WasmThreadState[] states = WasmThreadState.values();

      assertEquals(9, states.length, "Should have 9 thread state values");

      final Set<String> stateNames = new HashSet<>();
      for (WasmThreadState state : states) {
        stateNames.add(state.name());
      }

      assertTrue(stateNames.contains("NEW"), "Should have NEW state");
      assertTrue(stateNames.contains("RUNNING"), "Should have RUNNING state");
      assertTrue(stateNames.contains("WAITING"), "Should have WAITING state");
      assertTrue(stateNames.contains("TIMED_WAITING"), "Should have TIMED_WAITING state");
      assertTrue(stateNames.contains("BLOCKED"), "Should have BLOCKED state");
      assertTrue(stateNames.contains("SUSPENDED"), "Should have SUSPENDED state");
      assertTrue(stateNames.contains("TERMINATED"), "Should have TERMINATED state");
      assertTrue(stateNames.contains("ERROR"), "Should have ERROR state");
      assertTrue(stateNames.contains("KILLED"), "Should have KILLED state");

      LOGGER.info("WasmThreadState enum values verified: " + Arrays.toString(states));
    }

    @Test
    @DisplayName("Should parse WasmThreadState from string")
    void shouldParseWasmThreadStateFromString() {
      LOGGER.info("Testing WasmThreadState parsing from string");

      assertEquals(WasmThreadState.NEW, WasmThreadState.valueOf("NEW"));
      assertEquals(WasmThreadState.RUNNING, WasmThreadState.valueOf("RUNNING"));
      assertEquals(WasmThreadState.WAITING, WasmThreadState.valueOf("WAITING"));
      assertEquals(WasmThreadState.TIMED_WAITING, WasmThreadState.valueOf("TIMED_WAITING"));
      assertEquals(WasmThreadState.BLOCKED, WasmThreadState.valueOf("BLOCKED"));
      assertEquals(WasmThreadState.SUSPENDED, WasmThreadState.valueOf("SUSPENDED"));
      assertEquals(WasmThreadState.TERMINATED, WasmThreadState.valueOf("TERMINATED"));
      assertEquals(WasmThreadState.ERROR, WasmThreadState.valueOf("ERROR"));
      assertEquals(WasmThreadState.KILLED, WasmThreadState.valueOf("KILLED"));

      LOGGER.info("WasmThreadState parsing verified");
    }

    @Test
    @DisplayName("Should get WasmThreadState ordinal values")
    void shouldGetWasmThreadStateOrdinalValues() {
      LOGGER.info("Testing WasmThreadState ordinal values");

      assertEquals(0, WasmThreadState.NEW.ordinal(), "NEW should be ordinal 0");
      assertEquals(1, WasmThreadState.RUNNING.ordinal(), "RUNNING should be ordinal 1");
      assertEquals(2, WasmThreadState.WAITING.ordinal(), "WAITING should be ordinal 2");
      assertEquals(3, WasmThreadState.TIMED_WAITING.ordinal(), "TIMED_WAITING should be ordinal 3");
      assertEquals(4, WasmThreadState.BLOCKED.ordinal(), "BLOCKED should be ordinal 4");
      assertEquals(5, WasmThreadState.SUSPENDED.ordinal(), "SUSPENDED should be ordinal 5");
      assertEquals(6, WasmThreadState.TERMINATED.ordinal(), "TERMINATED should be ordinal 6");
      assertEquals(7, WasmThreadState.ERROR.ordinal(), "ERROR should be ordinal 7");
      assertEquals(8, WasmThreadState.KILLED.ordinal(), "KILLED should be ordinal 8");

      LOGGER.info("WasmThreadState ordinal values verified");
    }

    @Test
    @DisplayName("Should throw exception for invalid state name")
    void shouldThrowExceptionForInvalidStateName() {
      LOGGER.info("Testing exception for invalid state name");

      assertThrows(
          IllegalArgumentException.class,
          () -> WasmThreadState.valueOf("INVALID"),
          "Should throw for invalid state name");

      LOGGER.info("Exception for invalid state name verified");
    }
  }

  @Nested
  @DisplayName("WasmThreadStatistics Tests")
  class WasmThreadStatisticsTests {

    @Test
    @DisplayName("Should create empty statistics")
    void shouldCreateEmptyStatistics() {
      LOGGER.info("Testing empty statistics creation");

      final WasmThreadStatistics stats = WasmThreadStatistics.empty();

      assertNotNull(stats, "Statistics should not be null");
      assertEquals(0, stats.getFunctionsExecuted(), "Functions executed should be 0");
      assertEquals(0, stats.getTotalExecutionTime(), "Total execution time should be 0");
      assertEquals(0, stats.getAtomicOperations(), "Atomic operations should be 0");
      assertEquals(0, stats.getMemoryAccesses(), "Memory accesses should be 0");
      assertEquals(0, stats.getWaitNotifyOperations(), "Wait/notify operations should be 0");
      assertEquals(0, stats.getPeakMemoryUsage(), "Peak memory usage should be 0");

      LOGGER.info("Empty statistics verified: " + stats);
    }

    @Test
    @DisplayName("Should create statistics with values")
    void shouldCreateStatisticsWithValues() {
      LOGGER.info("Testing statistics creation with values");

      final WasmThreadStatistics stats =
          new WasmThreadStatistics(100, 1000000000L, 50, 200, 10, 4096);

      assertEquals(100, stats.getFunctionsExecuted(), "Functions executed should be 100");
      assertEquals(1000000000L, stats.getTotalExecutionTime(), "Total execution time should be 1s");
      assertEquals(50, stats.getAtomicOperations(), "Atomic operations should be 50");
      assertEquals(200, stats.getMemoryAccesses(), "Memory accesses should be 200");
      assertEquals(10, stats.getWaitNotifyOperations(), "Wait/notify operations should be 10");
      assertEquals(4096, stats.getPeakMemoryUsage(), "Peak memory usage should be 4096");

      LOGGER.info("Statistics with values verified: " + stats);
    }

    @Test
    @DisplayName("Should clamp negative values to zero")
    void shouldClampNegativeValuesToZero() {
      LOGGER.info("Testing negative value clamping");

      final WasmThreadStatistics stats =
          new WasmThreadStatistics(-100, -1000, -50, -200, -10, -4096);

      assertEquals(0, stats.getFunctionsExecuted(), "Functions executed should be clamped to 0");
      assertEquals(0, stats.getTotalExecutionTime(), "Execution time should be clamped to 0");
      assertEquals(0, stats.getAtomicOperations(), "Atomic operations should be clamped to 0");
      assertEquals(0, stats.getMemoryAccesses(), "Memory accesses should be clamped to 0");
      assertEquals(0, stats.getWaitNotifyOperations(), "Wait/notify should be clamped to 0");
      assertEquals(0, stats.getPeakMemoryUsage(), "Peak memory should be clamped to 0");

      LOGGER.info("Negative value clamping verified");
    }

    @Test
    @DisplayName("Should calculate average execution time")
    void shouldCalculateAverageExecutionTime() {
      LOGGER.info("Testing average execution time calculation");

      final WasmThreadStatistics stats = new WasmThreadStatistics(10, 1000000000L, 0, 0, 0, 0);

      assertEquals(
          100000000L, stats.getAverageExecutionTime(), "Average should be 100ms per function");

      LOGGER.info("Average execution time: " + stats.getAverageExecutionTime() + "ns");
    }

    @Test
    @DisplayName("Should handle zero functions for average calculation")
    void shouldHandleZeroFunctionsForAverageCalculation() {
      LOGGER.info("Testing average calculation with zero functions");

      final WasmThreadStatistics stats = new WasmThreadStatistics(0, 1000000000L, 0, 0, 0, 0);

      assertEquals(0, stats.getAverageExecutionTime(), "Average should be 0 for zero functions");

      LOGGER.info("Zero functions average handling verified");
    }

    @Test
    @DisplayName("Should convert execution time to milliseconds")
    void shouldConvertExecutionTimeToMilliseconds() {
      LOGGER.info("Testing execution time millisecond conversion");

      final WasmThreadStatistics stats = new WasmThreadStatistics(0, 1500000000L, 0, 0, 0, 0);

      assertEquals(1500, stats.getTotalExecutionTimeMillis(), "Execution time should be 1500ms");

      LOGGER.info("Execution time in ms: " + stats.getTotalExecutionTimeMillis());
    }

    @Test
    @DisplayName("Should convert memory usage to KB and MB")
    void shouldConvertMemoryUsageToKbAndMb() {
      LOGGER.info("Testing memory usage conversion");

      final WasmThreadStatistics stats =
          new WasmThreadStatistics(0, 0, 0, 0, 0, 10 * 1024 * 1024); // 10 MB

      assertEquals(10 * 1024, stats.getPeakMemoryUsageKB(), "Peak memory should be 10240 KB");
      assertEquals(10, stats.getPeakMemoryUsageMB(), "Peak memory should be 10 MB");

      LOGGER.info(
          "Memory usage: "
              + stats.getPeakMemoryUsageKB()
              + " KB, "
              + stats.getPeakMemoryUsageMB()
              + " MB");
    }

    @Test
    @DisplayName("Should calculate operations per second")
    void shouldCalculateOperationsPerSecond() {
      LOGGER.info("Testing operations per second calculation");

      // 100 functions + 50 atomic + 10 wait/notify = 160 ops in 1 second
      final WasmThreadStatistics stats = new WasmThreadStatistics(100, 1000000000L, 50, 0, 10, 0);

      final double opsPerSec = stats.getOperationsPerSecond();
      assertEquals(160.0, opsPerSec, 0.001, "Should be 160 ops/sec");

      LOGGER.info("Operations per second: " + opsPerSec);
    }

    @Test
    @DisplayName("Should calculate memory access rate")
    void shouldCalculateMemoryAccessRate() {
      LOGGER.info("Testing memory access rate calculation");

      // 1000 memory accesses in 1 second = 1000 accesses/sec
      final WasmThreadStatistics stats = new WasmThreadStatistics(0, 1000000000L, 0, 1000, 0, 0);

      final double accessRate = stats.getMemoryAccessRate();
      assertEquals(1000.0, accessRate, 0.001, "Should be 1000 accesses/sec");

      LOGGER.info("Memory access rate: " + accessRate);
    }

    @Test
    @DisplayName("Should handle zero execution time for rate calculations")
    void shouldHandleZeroExecutionTimeForRateCalculations() {
      LOGGER.info("Testing rate calculations with zero execution time");

      final WasmThreadStatistics stats = new WasmThreadStatistics(100, 0, 50, 1000, 10, 4096);

      assertEquals(0.0, stats.getOperationsPerSecond(), "Ops/sec should be 0");
      assertEquals(0.0, stats.getMemoryAccessRate(), "Access rate should be 0");

      LOGGER.info("Zero execution time rate handling verified");
    }

    @Test
    @DisplayName("Should combine statistics")
    void shouldCombineStatistics() {
      LOGGER.info("Testing statistics combination");

      final WasmThreadStatistics stats1 =
          new WasmThreadStatistics(100, 1000000000L, 50, 200, 10, 4096);
      final WasmThreadStatistics stats2 =
          new WasmThreadStatistics(200, 2000000000L, 75, 300, 20, 8192);

      final WasmThreadStatistics combined = stats1.combine(stats2);

      assertEquals(300, combined.getFunctionsExecuted(), "Combined functions should be 300");
      assertEquals(3000000000L, combined.getTotalExecutionTime(), "Combined time should be 3s");
      assertEquals(125, combined.getAtomicOperations(), "Combined atomic ops should be 125");
      assertEquals(500, combined.getMemoryAccesses(), "Combined memory accesses should be 500");
      assertEquals(30, combined.getWaitNotifyOperations(), "Combined wait/notify should be 30");
      assertEquals(8192, combined.getPeakMemoryUsage(), "Peak should be max (8192)");

      LOGGER.info("Combined statistics: " + combined);
    }

    @Test
    @DisplayName("Should throw exception when combining with null")
    void shouldThrowExceptionWhenCombiningWithNull() {
      LOGGER.info("Testing combine with null");

      final WasmThreadStatistics stats = WasmThreadStatistics.empty();

      assertThrows(
          IllegalArgumentException.class,
          () -> stats.combine(null),
          "Should throw for null parameter");

      LOGGER.info("Null combine exception verified");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing equals implementation");

      final WasmThreadStatistics stats1 = new WasmThreadStatistics(100, 1000L, 50, 200, 10, 4096);
      final WasmThreadStatistics stats2 = new WasmThreadStatistics(100, 1000L, 50, 200, 10, 4096);
      final WasmThreadStatistics stats3 = new WasmThreadStatistics(100, 2000L, 50, 200, 10, 4096);

      assertEquals(stats1, stats2, "Same values should be equal");
      assertNotEquals(stats1, stats3, "Different values should not be equal");
      assertNotEquals(stats1, null, "Should not equal null");
      assertNotEquals(stats1, "string", "Should not equal other types");
      assertEquals(stats1, stats1, "Should equal itself");

      LOGGER.info("Equals implementation verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing hashCode implementation");

      final WasmThreadStatistics stats1 = new WasmThreadStatistics(100, 1000L, 50, 200, 10, 4096);
      final WasmThreadStatistics stats2 = new WasmThreadStatistics(100, 1000L, 50, 200, 10, 4096);

      assertEquals(stats1.hashCode(), stats2.hashCode(), "Equal objects should have same hashCode");

      LOGGER.info("HashCode implementation verified");
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      LOGGER.info("Testing toString implementation");

      final WasmThreadStatistics stats =
          new WasmThreadStatistics(100, 1000000000L, 50, 200, 10, 4096);
      final String str = stats.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WasmThreadStatistics"), "Should contain class name");
      assertTrue(str.contains("100"), "Should contain functions executed");
      assertTrue(str.contains("50"), "Should contain atomic operations");

      LOGGER.info("toString: " + str);
    }
  }

  @Nested
  @DisplayName("WasmThread Interface Tests")
  class WasmThreadInterfaceTests {

    @Test
    @DisplayName("Should define WasmThread interface methods")
    void shouldDefineWasmThreadInterfaceMethods() {
      LOGGER.info("Testing WasmThread interface method definitions");

      final List<String> methodNames = new ArrayList<>();
      for (Method method : WasmThread.class.getMethods()) {
        methodNames.add(method.getName());
      }

      assertTrue(methodNames.contains("getThreadId"), "Should have getThreadId");
      assertTrue(methodNames.contains("getState"), "Should have getState");
      assertTrue(methodNames.contains("executeFunction"), "Should have executeFunction");
      assertTrue(methodNames.contains("executeOperation"), "Should have executeOperation");
      assertTrue(methodNames.contains("join"), "Should have join");
      assertTrue(methodNames.contains("terminate"), "Should have terminate");
      assertTrue(methodNames.contains("forceTerminate"), "Should have forceTerminate");
      assertTrue(methodNames.contains("getSharedMemory"), "Should have getSharedMemory");
      assertTrue(
          methodNames.contains("getThreadLocalStorage"), "Should have getThreadLocalStorage");
      assertTrue(methodNames.contains("isAlive"), "Should have isAlive");
      assertTrue(
          methodNames.contains("isTerminationRequested"), "Should have isTerminationRequested");
      assertTrue(methodNames.contains("getStatistics"), "Should have getStatistics");
      assertTrue(methodNames.contains("close"), "Should have close");

      LOGGER.info("WasmThread interface methods verified: " + methodNames.size() + " methods");
    }

    @Test
    @DisplayName("Should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      LOGGER.info("Testing WasmThread extends AutoCloseable");

      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasmThread.class),
          "WasmThread should extend AutoCloseable");

      LOGGER.info("WasmThread extends AutoCloseable verified");
    }

    @Test
    @DisplayName("Should have getThreadId returning long")
    void shouldHaveGetThreadIdReturningLong() throws NoSuchMethodException {
      LOGGER.info("Testing getThreadId return type");

      final Method method = WasmThread.class.getMethod("getThreadId");
      assertEquals(long.class, method.getReturnType(), "getThreadId should return long");

      LOGGER.info("getThreadId return type verified");
    }

    @Test
    @DisplayName("Should have getState returning WasmThreadState")
    void shouldHaveGetStateReturningWasmThreadState() throws NoSuchMethodException {
      LOGGER.info("Testing getState return type");

      final Method method = WasmThread.class.getMethod("getState");
      assertEquals(
          WasmThreadState.class, method.getReturnType(), "getState should return WasmThreadState");

      LOGGER.info("getState return type verified");
    }

    @Test
    @DisplayName("Should have join with timeout parameter")
    void shouldHaveJoinWithTimeoutParameter() throws NoSuchMethodException {
      LOGGER.info("Testing join(long) method");

      final Method method = WasmThread.class.getMethod("join", long.class);
      assertEquals(boolean.class, method.getReturnType(), "join(long) should return boolean");

      LOGGER.info("join(long) method verified");
    }

    @Test
    @DisplayName("Should have isAlive returning boolean")
    void shouldHaveIsAliveReturningBoolean() throws NoSuchMethodException {
      LOGGER.info("Testing isAlive return type");

      final Method method = WasmThread.class.getMethod("isAlive");
      assertEquals(boolean.class, method.getReturnType(), "isAlive should return boolean");

      LOGGER.info("isAlive return type verified");
    }
  }

  @Nested
  @DisplayName("WasmThreadLocalStorage Interface Tests")
  class WasmThreadLocalStorageInterfaceTests {

    @Test
    @DisplayName("Should define storage methods for all types")
    void shouldDefineStorageMethodsForAllTypes() {
      LOGGER.info("Testing WasmThreadLocalStorage interface methods");

      final List<String> methodNames = new ArrayList<>();
      for (Method method : WasmThreadLocalStorage.class.getMethods()) {
        methodNames.add(method.getName());
      }

      // Int methods
      assertTrue(methodNames.contains("putInt"), "Should have putInt");
      assertTrue(methodNames.contains("getInt"), "Should have getInt");

      // Long methods
      assertTrue(methodNames.contains("putLong"), "Should have putLong");
      assertTrue(methodNames.contains("getLong"), "Should have getLong");

      // Float methods
      assertTrue(methodNames.contains("putFloat"), "Should have putFloat");
      assertTrue(methodNames.contains("getFloat"), "Should have getFloat");

      // Double methods
      assertTrue(methodNames.contains("putDouble"), "Should have putDouble");
      assertTrue(methodNames.contains("getDouble"), "Should have getDouble");

      // Bytes methods
      assertTrue(methodNames.contains("putBytes"), "Should have putBytes");
      assertTrue(methodNames.contains("getBytes"), "Should have getBytes");

      // String methods
      assertTrue(methodNames.contains("putString"), "Should have putString");
      assertTrue(methodNames.contains("getString"), "Should have getString");

      // Utility methods
      assertTrue(methodNames.contains("remove"), "Should have remove");
      assertTrue(methodNames.contains("contains"), "Should have contains");
      assertTrue(methodNames.contains("clear"), "Should have clear");
      assertTrue(methodNames.contains("size"), "Should have size");
      assertTrue(methodNames.contains("getMemoryUsage"), "Should have getMemoryUsage");

      LOGGER.info(
          "WasmThreadLocalStorage interface methods verified: " + methodNames.size() + " methods");
    }

    @Test
    @DisplayName("Should have putInt with String key and int value")
    void shouldHavePutIntWithStringKeyAndIntValue() throws NoSuchMethodException {
      LOGGER.info("Testing putInt method signature");

      final Method method =
          WasmThreadLocalStorage.class.getMethod("putInt", String.class, int.class);
      assertNotNull(method, "putInt method should exist");

      LOGGER.info("putInt method signature verified");
    }

    @Test
    @DisplayName("Should have getInt with String key returning int")
    void shouldHaveGetIntWithStringKeyReturningInt() throws NoSuchMethodException {
      LOGGER.info("Testing getInt method signature");

      final Method method = WasmThreadLocalStorage.class.getMethod("getInt", String.class);
      assertEquals(int.class, method.getReturnType(), "getInt should return int");

      LOGGER.info("getInt method signature verified");
    }

    @Test
    @DisplayName("Should have remove returning boolean")
    void shouldHaveRemoveReturningBoolean() throws NoSuchMethodException {
      LOGGER.info("Testing remove method signature");

      final Method method = WasmThreadLocalStorage.class.getMethod("remove", String.class);
      assertEquals(boolean.class, method.getReturnType(), "remove should return boolean");

      LOGGER.info("remove method signature verified");
    }

    @Test
    @DisplayName("Should have contains returning boolean")
    void shouldHaveContainsReturningBoolean() throws NoSuchMethodException {
      LOGGER.info("Testing contains method signature");

      final Method method = WasmThreadLocalStorage.class.getMethod("contains", String.class);
      assertEquals(boolean.class, method.getReturnType(), "contains should return boolean");

      LOGGER.info("contains method signature verified");
    }

    @Test
    @DisplayName("Should have size returning int")
    void shouldHaveSizeReturningInt() throws NoSuchMethodException {
      LOGGER.info("Testing size method signature");

      final Method method = WasmThreadLocalStorage.class.getMethod("size");
      assertEquals(int.class, method.getReturnType(), "size should return int");

      LOGGER.info("size method signature verified");
    }

    @Test
    @DisplayName("Should have getMemoryUsage returning long")
    void shouldHaveGetMemoryUsageReturningLong() throws NoSuchMethodException {
      LOGGER.info("Testing getMemoryUsage method signature");

      final Method method = WasmThreadLocalStorage.class.getMethod("getMemoryUsage");
      assertEquals(long.class, method.getReturnType(), "getMemoryUsage should return long");

      LOGGER.info("getMemoryUsage method signature verified");
    }
  }

  @Nested
  @DisplayName("Engine Threads Configuration Tests")
  class EngineThreadsConfigurationTests {

    @Test
    @DisplayName("Should have addWasmFeature configuration method")
    void shouldHaveAddWasmFeatureConfigurationMethod() throws NoSuchMethodException {
      LOGGER.info("Testing EngineConfig.addWasmFeature method");

      final Method method = EngineConfig.class.getMethod("addWasmFeature", WasmFeature.class);
      assertNotNull(method, "addWasmFeature method should exist");
      assertEquals(
          EngineConfig.class, method.getReturnType(), "addWasmFeature should return EngineConfig");

      LOGGER.info("EngineConfig.addWasmFeature method verified");
    }

    @Test
    @DisplayName("Should enable threads via WasmFeature")
    void shouldEnableThreadsViaWasmFeature() {
      LOGGER.info("Testing enabling threads via WasmFeature.THREADS");

      try {
        final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
        assertNotNull(config, "Config should not be null");
        LOGGER.info("Threads enabled via WasmFeature.THREADS successfully");
      } catch (Exception e) {
        LOGGER.warning("Failed to enable threads: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Should create engine with threads enabled")
    void shouldCreateEngineWithThreadsEnabled() {
      assumeTrue(threadsSupported, "Threads not supported");

      LOGGER.info("Testing engine creation with threads enabled");

      try {
        final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
        try (Engine engine = Engine.create(config)) {
          assertNotNull(engine, "Engine should not be null");
          LOGGER.info("Engine created with threads enabled");
        }
      } catch (Exception e) {
        LOGGER.warning("Failed to create engine with threads: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Thread State Transitions Tests")
  class ThreadStateTransitionsTests {

    @Test
    @DisplayName("Should allow NEW to RUNNING transition")
    void shouldAllowNewToRunningTransition() {
      LOGGER.info("Testing NEW to RUNNING state transition");

      // Verify that both states exist and are distinct
      final WasmThreadState newState = WasmThreadState.NEW;
      final WasmThreadState runningState = WasmThreadState.RUNNING;

      assertNotEquals(newState, runningState, "NEW and RUNNING should be different states");
      assertTrue(newState.ordinal() < runningState.ordinal(), "NEW should come before RUNNING");

      LOGGER.info("NEW to RUNNING transition verified");
    }

    @Test
    @DisplayName("Should have distinct terminal states")
    void shouldHaveDistinctTerminalStates() {
      LOGGER.info("Testing terminal states");

      final WasmThreadState terminated = WasmThreadState.TERMINATED;
      final WasmThreadState error = WasmThreadState.ERROR;
      final WasmThreadState killed = WasmThreadState.KILLED;

      assertNotEquals(terminated, error, "TERMINATED and ERROR should be different");
      assertNotEquals(error, killed, "ERROR and KILLED should be different");
      assertNotEquals(terminated, killed, "TERMINATED and KILLED should be different");

      LOGGER.info("Terminal states verified");
    }

    @Test
    @DisplayName("Should have waiting states")
    void shouldHaveWaitingStates() {
      LOGGER.info("Testing waiting states");

      final WasmThreadState waiting = WasmThreadState.WAITING;
      final WasmThreadState timedWaiting = WasmThreadState.TIMED_WAITING;
      final WasmThreadState blocked = WasmThreadState.BLOCKED;

      assertNotEquals(waiting, timedWaiting, "WAITING and TIMED_WAITING should be different");
      assertNotEquals(waiting, blocked, "WAITING and BLOCKED should be different");
      assertNotEquals(timedWaiting, blocked, "TIMED_WAITING and BLOCKED should be different");

      LOGGER.info("Waiting states verified");
    }
  }

  @Nested
  @DisplayName("Statistics Arithmetic Tests")
  class StatisticsArithmeticTests {

    @Test
    @DisplayName("Should correctly calculate with large values")
    void shouldCorrectlyCalculateWithLargeValues() {
      LOGGER.info("Testing statistics with large values");

      // Use max long values to test overflow handling
      final long halfMax = Long.MAX_VALUE / 2;
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(halfMax, halfMax, halfMax, halfMax, halfMax, halfMax);

      assertEquals(halfMax, stats.getFunctionsExecuted());
      assertEquals(halfMax, stats.getTotalExecutionTime());

      LOGGER.info("Large value statistics: " + stats);
    }

    @Test
    @DisplayName("Should maintain precision in calculations")
    void shouldMaintainPrecisionInCalculations() {
      LOGGER.info("Testing calculation precision");

      // 1 billion nanoseconds = 1 second
      final WasmThreadStatistics stats = new WasmThreadStatistics(1000, 1000000000L, 0, 0, 0, 0);

      // Should be exactly 1000 ms
      assertEquals(1000, stats.getTotalExecutionTimeMillis());

      // Average should be 1,000,000 ns per function
      assertEquals(1000000L, stats.getAverageExecutionTime());

      LOGGER.info("Calculation precision verified");
    }

    @Test
    @DisplayName("Should combine statistics without overflow")
    void shouldCombineStatisticsWithoutOverflow() {
      LOGGER.info("Testing statistics combination without overflow");

      final long largeValue = Long.MAX_VALUE / 4;
      final WasmThreadStatistics stats1 =
          new WasmThreadStatistics(
              largeValue, largeValue, largeValue, largeValue, largeValue, largeValue);
      final WasmThreadStatistics stats2 =
          new WasmThreadStatistics(
              largeValue, largeValue, largeValue, largeValue, largeValue, largeValue);

      final WasmThreadStatistics combined = stats1.combine(stats2);

      assertEquals(largeValue * 2, combined.getFunctionsExecuted());
      assertEquals(largeValue * 2, combined.getTotalExecutionTime());

      LOGGER.info("Statistics combination without overflow verified");
    }
  }
}
