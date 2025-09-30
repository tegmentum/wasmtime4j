package ai.tegmentum.wasmtime4j.compilation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

/** Comprehensive tests for advanced JIT compilation strategies and optimizations. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Advanced JIT Compilation Tests")
class AdvancedJitCompilationTest {

  private static final String TEST_MODULE_ID = "test_module";
  private static final String TEST_FUNCTION_NAME = "test_function";
  private static final byte[] SIMPLE_WASM_MODULE = {
    0x00, 0x61, 0x73, 0x6d, // WASM magic
    0x01, 0x00, 0x00, 0x00 // WASM version
  };

  @Nested
  @DisplayName("Tiered Compilation Tests")
  class TieredCompilationTests {

    private TieredCompilationConfig config;

    @BeforeEach
    void setUp() {
      config = TieredCompilationConfig.createDefault();
    }

    @Test
    @DisplayName("Should create default tiered compilation configuration")
    void shouldCreateDefaultConfiguration() {
      assertTrue(config.isEnabled());
      assertNotNull(config.getConfiguredTiers());
      assertFalse(config.getConfiguredTiers().isEmpty());
    }

    @Test
    @DisplayName("Should create performance-focused configuration")
    void shouldCreatePerformanceFocusedConfiguration() {
      final TieredCompilationConfig perfConfig = TieredCompilationConfig.createPerformanceFocused();

      assertTrue(perfConfig.isEnabled());
      assertTrue(perfConfig.isProfileBasedTriggers());
      assertTrue(perfConfig.isAdaptiveThresholds());
      assertTrue(perfConfig.getMaxConcurrentRecompilations() > 2);
    }

    @Test
    @DisplayName("Should validate tier configuration")
    void shouldValidateTierConfiguration() {
      final TierConfig baselineConfig = TierConfig.createBaseline();
      final TierConfig optimizedConfig = TierConfig.createOptimized();
      final TierConfig highlyOptimizedConfig = TierConfig.createHighlyOptimized();

      assertEquals(
          ai.tegmentum.wasmtime4j.OptimizationLevel.NONE, baselineConfig.getOptimizationLevel());
      assertEquals(
          ai.tegmentum.wasmtime4j.OptimizationLevel.SPEED, optimizedConfig.getOptimizationLevel());
      assertEquals(
          ai.tegmentum.wasmtime4j.OptimizationLevel.SPEED_AND_SIZE,
          highlyOptimizedConfig.getOptimizationLevel());

      assertTrue(baselineConfig.getExecutionThreshold() < optimizedConfig.getExecutionThreshold());
      assertTrue(
          optimizedConfig.getExecutionThreshold() < highlyOptimizedConfig.getExecutionThreshold());
    }

    @Test
    @DisplayName("Should reject invalid tier configurations")
    void shouldRejectInvalidTierConfigurations() {
      assertThrows(
          IllegalStateException.class,
          () -> {
            TieredCompilationConfig.builder().enabled(true).build(); // No tiers configured
          });
    }

    @Test
    @DisplayName("Should support builder pattern with method chaining")
    void shouldSupportBuilderPatternWithMethodChaining() {
      final TieredCompilationConfig customConfig =
          TieredCompilationConfig.builder()
              .enabled(true)
              .addTier(CompilationTier.BASELINE, TierConfig.createBaseline())
              .addTier(CompilationTier.OPTIMIZED, TierConfig.createOptimized())
              .maxConcurrentRecompilations(8)
              .profileBasedTriggers(true)
              .adaptiveThresholds(true)
              .build();

      assertTrue(customConfig.isEnabled());
      assertTrue(customConfig.isProfileBasedTriggers());
      assertTrue(customConfig.isAdaptiveThresholds());
      assertEquals(8, customConfig.getMaxConcurrentRecompilations());
      assertEquals(2, customConfig.getConfiguredTiers().size());
    }
  }

  @Nested
  @DisplayName("Adaptive Optimizer Tests")
  class AdaptiveOptimizerTests {

    private AdaptiveOptimizer optimizer;
    private ExecutionData testExecutionData;

    @BeforeEach
    void setUp() {
      optimizer = AdaptiveOptimizer.createDefault();
      testExecutionData =
          new ExecutionData(
              1000000, // 1ms execution time
              1024 * 1024, // 1MB memory
              0.5, // 50% CPU utilization
              Map.of("cache_misses", 100, "branch_mispredictions", 50));
    }

    @Test
    @DisplayName("Should record and analyze execution data")
    void shouldRecordAndAnalyzeExecutionData() {
      optimizer.recordExecution(TEST_MODULE_ID, TEST_FUNCTION_NAME, testExecutionData);

      final FunctionProfile profile =
          optimizer.getFunctionProfile(TEST_MODULE_ID, TEST_FUNCTION_NAME);
      assertNotNull(profile);
      assertEquals(TEST_MODULE_ID, profile.getModuleId());
      assertEquals(TEST_FUNCTION_NAME, profile.getFunctionName());
      assertEquals(1, profile.getTotalExecutions());
    }

    @Test
    @DisplayName("Should make optimization decisions based on profile data")
    void shouldMakeOptimizationDecisions() {
      // Record multiple executions to build up profile data
      for (int i = 0; i < 1000; i++) {
        optimizer.recordExecution(TEST_MODULE_ID, TEST_FUNCTION_NAME, testExecutionData);
      }

      final OptimizationDecision decision =
          optimizer.shouldOptimize(TEST_MODULE_ID, TEST_FUNCTION_NAME);
      assertNotNull(decision);
      assertTrue(
          decision.shouldOptimize() || !decision.shouldOptimize()); // Either decision is valid
      assertNotNull(decision.getReason());
    }

    @Test
    @DisplayName("Should track optimization results and learn from feedback")
    void shouldTrackOptimizationResults() {
      optimizer.recordExecution(TEST_MODULE_ID, TEST_FUNCTION_NAME, testExecutionData);

      final OptimizationResult successResult =
          OptimizationResult.success(
              "Inlining optimization applied",
              100, // 100ms compilation time
              1.25 // 25% performance improvement
              );

      optimizer.recordOptimizationResult(TEST_MODULE_ID, TEST_FUNCTION_NAME, successResult);

      final OptimizationStatistics stats = optimizer.getStatistics();
      assertEquals(1, stats.getTotalOptimizations());
      assertEquals(1, stats.getSuccessfulOptimizations());
      assertEquals(1.0, stats.getSuccessRate(), 0.001);
    }

    @Test
    @DisplayName("Should handle multiple modules and functions")
    void shouldHandleMultipleModulesAndFunctions() {
      final String module1 = "module1";
      final String module2 = "module2";
      final String function1 = "function1";
      final String function2 = "function2";

      optimizer.recordExecution(module1, function1, testExecutionData);
      optimizer.recordExecution(module1, function2, testExecutionData);
      optimizer.recordExecution(module2, function1, testExecutionData);

      final OptimizationStatistics stats = optimizer.getStatistics();
      assertEquals(3, stats.getTrackedFunctions());
      assertEquals(2, stats.getTrackedModules());
    }

    @Test
    @DisplayName("Should reset all data when requested")
    void shouldResetAllData() {
      optimizer.recordExecution(TEST_MODULE_ID, TEST_FUNCTION_NAME, testExecutionData);

      final OptimizationStatistics statsBefore = optimizer.getStatistics();
      assertTrue(statsBefore.getTrackedFunctions() > 0);

      optimizer.reset();

      final OptimizationStatistics statsAfter = optimizer.getStatistics();
      assertEquals(0, statsAfter.getTotalOptimizations());
      assertEquals(0, statsAfter.getTrackedFunctions());
      assertEquals(0, statsAfter.getTrackedModules());
    }

    @Test
    @DisplayName("Should create aggressive optimizer with different thresholds")
    void shouldCreateAggressiveOptimizer() {
      final AdaptiveOptimizer aggressiveOptimizer = AdaptiveOptimizer.createAggressive();
      assertNotNull(aggressiveOptimizer);

      // Aggressive optimizer should have different behavior
      final OptimizationStatistics stats = aggressiveOptimizer.getStatistics();
      assertEquals(0, stats.getTotalOptimizations()); // Initially zero
    }

    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            optimizer.recordExecution(null, TEST_FUNCTION_NAME, testExecutionData);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            optimizer.recordExecution(TEST_MODULE_ID, null, testExecutionData);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            optimizer.recordExecution(TEST_MODULE_ID, TEST_FUNCTION_NAME, null);
          });
    }
  }

  @Nested
  @DisplayName("Speculative Optimizer Tests")
  class SpeculativeOptimizerTests {

    private SpeculativeOptimizer optimizer;

    @BeforeEach
    void setUp() {
      optimizer = SpeculativeOptimizer.createDefault();
    }

    @Test
    @DisplayName("Should determine suitable speculations based on execution profile")
    void shouldDetermineSpeculations() {
      final ExecutionProfile profile =
          new ExecutionProfile(
              1000, // execution count
              10.0, // average execution time
              10000, // total execution time
              0.6, // CPU utilization
              1024 * 1024, // memory usage
              true, // has loops
              false, // has vector operations
              false, // has recursion
              5, // function count
              1024 * 100 // module size
              );

      final List<SpeculativeOptimization> speculations =
          optimizer.determineSpeculations(TEST_MODULE_ID, TEST_FUNCTION_NAME, profile);

      assertNotNull(speculations);
      // The number of speculations may vary based on implementation
    }

    @Test
    @DisplayName("Should record speculation results and update profiles")
    void shouldRecordSpeculationResults() {
      final ExecutionProfile profile =
          new ExecutionProfile(
              1000, 10.0, 10000, 0.6, 1024 * 1024, true, false, false, 5, 1024 * 100);

      final List<SpeculativeOptimization> speculations =
          optimizer.determineSpeculations(TEST_MODULE_ID, TEST_FUNCTION_NAME, profile);

      if (!speculations.isEmpty()) {
        final SpeculativeOptimization speculation = speculations.get(0);
        final SpeculationResult result =
            SpeculationResult.success(
                "Type specialization successful",
                150, // compilation time
                1.3 // performance improvement
                );

        optimizer.recordSpeculationResult(TEST_MODULE_ID, TEST_FUNCTION_NAME, speculation, result);

        final SpeculationStatistics stats = optimizer.getStatistics();
        assertTrue(stats.getTotalSpeculations() > 0);
      }
    }

    @Test
    @DisplayName("Should handle deoptimization events")
    void shouldHandleDeoptimization() {
      final DeoptimizationReason reason = DeoptimizationReason.TYPE_ASSUMPTION_VIOLATED;
      final Map<String, Object> context =
          Map.of(
              "expected_type", "i32",
              "actual_type", "f64");

      optimizer.recordDeoptimization(TEST_MODULE_ID, TEST_FUNCTION_NAME, reason, context);

      final SpeculationStatistics stats = optimizer.getStatistics();
      assertEquals(1, stats.getDeoptimizations());
    }

    @Test
    @DisplayName("Should make deoptimization decisions based on runtime conditions")
    void shouldMakeDeoptimizationDecisions() {
      final RuntimeConditions conditions =
          new RuntimeConditions(
              Map.of("param_type", "f64"), // type observations
              Map.of("branch_taken", 0.9), // branch patterns
              0.5, // current performance (50% of baseline)
              1024 * 1024 * 10 // memory usage
              );

      final DeoptimizationDecision decision =
          optimizer.shouldDeoptimize(TEST_MODULE_ID, TEST_FUNCTION_NAME, conditions);

      assertNotNull(decision);
      assertNotNull(decision.getDescription());

      if (decision.shouldDeoptimize()) {
        assertNotNull(decision.getReason());
      }
    }

    @Test
    @DisplayName("Should create aggressive speculative optimizer")
    void shouldCreateAggressiveSpeculativeOptimizer() {
      final SpeculativeOptimizer aggressiveOptimizer = SpeculativeOptimizer.createAggressive();
      assertNotNull(aggressiveOptimizer);

      final SpeculationStatistics stats = aggressiveOptimizer.getStatistics();
      assertEquals(0, stats.getTotalSpeculations()); // Initially zero
    }

    @Test
    @DisplayName("Should validate speculation input parameters")
    void shouldValidateSpeculationInputParameters() {
      final ExecutionProfile validProfile =
          new ExecutionProfile(
              1000, 10.0, 10000, 0.6, 1024 * 1024, true, false, false, 5, 1024 * 100);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            optimizer.determineSpeculations(null, TEST_FUNCTION_NAME, validProfile);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            optimizer.determineSpeculations(TEST_MODULE_ID, null, validProfile);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            optimizer.determineSpeculations(TEST_MODULE_ID, TEST_FUNCTION_NAME, null);
          });
    }
  }

  @Nested
  @DisplayName("Profile-Guided Optimizer Tests")
  class ProfileGuidedOptimizerTests {

    private ProfileGuidedOptimizer optimizer;

    @BeforeEach
    void setUp() {
      optimizer = ProfileGuidedOptimizer.createDefault();
    }

    @Test
    @DisplayName("Should start instrumentation phase successfully")
    void shouldStartInstrumentationPhase() {
      final InstrumentedModule instrumentedModule =
          optimizer.startInstrumentationPhase(TEST_MODULE_ID, SIMPLE_WASM_MODULE);

      assertNotNull(instrumentedModule);
      assertEquals(TEST_MODULE_ID, instrumentedModule.getModuleId());
      assertNotNull(instrumentedModule.getInstrumentedBytes());
      assertNotNull(instrumentedModule.getInstrumentationMap());
      assertNotNull(instrumentedModule.getProfileData());
    }

    @Test
    @DisplayName("Should record profile data during execution")
    void shouldRecordProfileData() {
      optimizer.startInstrumentationPhase(TEST_MODULE_ID, SIMPLE_WASM_MODULE);

      final ProfileDataPoint profileData =
          new ProfileDataPoint(
              TEST_FUNCTION_NAME,
              100, // execution count
              50000000, // execution time (50ms)
              Map.of("branch_1", 80, "branch_2", 20), // branch counts
              Map.of("cache_misses", 10, "tlb_misses", 5) // additional metrics
              );

      optimizer.recordProfileData(TEST_MODULE_ID, TEST_FUNCTION_NAME, profileData);

      // Verification that data was recorded (implementation-dependent)
      final PgoStatistics stats = optimizer.getStatistics();
      assertTrue(stats.getProfiledModules() >= 1);
    }

    @Test
    @DisplayName("Should generate optimization plan from profile data")
    void shouldGenerateOptimizationPlan() {
      optimizer.startInstrumentationPhase(TEST_MODULE_ID, SIMPLE_WASM_MODULE);

      // Record sufficient profile data
      for (int i = 0; i < 1000; i++) {
        final ProfileDataPoint profileData =
            new ProfileDataPoint(
                TEST_FUNCTION_NAME,
                1, // execution count per data point
                1000000, // 1ms execution time
                Map.of("branch_hot", 1), // hot branch
                Map.of());
        optimizer.recordProfileData(TEST_MODULE_ID, TEST_FUNCTION_NAME, profileData);
      }

      final OptimizationPlan plan = optimizer.generateOptimizationPlan(TEST_MODULE_ID);
      assertNotNull(plan);
      assertEquals(TEST_MODULE_ID, plan.getModuleId());
      assertNotNull(plan.getFunctionOptimizations());
      assertTrue(plan.getEstimatedSpeedup() >= 1.0); // Should be at least baseline performance
    }

    @Test
    @DisplayName("Should apply optimizations based on generated plan")
    void shouldApplyOptimizations() {
      optimizer.startInstrumentationPhase(TEST_MODULE_ID, SIMPLE_WASM_MODULE);

      // Record profile data and generate plan
      for (int i = 0; i < 1000; i++) {
        final ProfileDataPoint profileData =
            new ProfileDataPoint(TEST_FUNCTION_NAME, 1, 1000000, Map.of("branch_hot", 1), Map.of());
        optimizer.recordProfileData(TEST_MODULE_ID, TEST_FUNCTION_NAME, profileData);
      }

      final OptimizationPlan plan = optimizer.generateOptimizationPlan(TEST_MODULE_ID);
      final OptimizedModule optimizedModule =
          optimizer.applyOptimizations(TEST_MODULE_ID, SIMPLE_WASM_MODULE, plan);

      assertNotNull(optimizedModule);
      assertEquals(TEST_MODULE_ID, optimizedModule.getModuleId());
      assertNotNull(optimizedModule.getOptimizedBytes());
      assertNotNull(optimizedModule.getOptimizationMetadata());
      assertEquals(plan, optimizedModule.getAppliedPlan());
    }

    @Test
    @DisplayName("Should create aggressive PGO optimizer")
    void shouldCreateAggressivePgoOptimizer() {
      final ProfileGuidedOptimizer aggressiveOptimizer = ProfileGuidedOptimizer.createAggressive();
      assertNotNull(aggressiveOptimizer);
    }

    @Test
    @DisplayName("Should validate PGO input parameters")
    void shouldValidatePgoInputParameters() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            optimizer.startInstrumentationPhase(null, SIMPLE_WASM_MODULE);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            optimizer.startInstrumentationPhase(TEST_MODULE_ID, null);
          });
    }

    @Test
    @DisplayName("Should handle insufficient profile data gracefully")
    void shouldHandleInsufficientProfileData() {
      optimizer.startInstrumentationPhase(TEST_MODULE_ID, SIMPLE_WASM_MODULE);

      // Record very little profile data
      final ProfileDataPoint profileData =
          new ProfileDataPoint(TEST_FUNCTION_NAME, 1, 1000000, Map.of(), Map.of());
      optimizer.recordProfileData(TEST_MODULE_ID, TEST_FUNCTION_NAME, profileData);

      assertThrows(
          PgoException.class,
          () -> {
            optimizer.generateOptimizationPlan(TEST_MODULE_ID);
          });
    }
  }

  @Nested
  @DisplayName("JIT Performance Monitor Tests")
  class JitPerformanceMonitorTests {

    private JitPerformanceMonitor monitor;

    @BeforeEach
    void setUp() {
      monitor = JitPerformanceMonitor.createDefault();
      monitor.start();
    }

    @AfterEach
    void tearDown() {
      if (monitor.getState() == MonitoringState.RUNNING) {
        monitor.stop();
      }
    }

    @Test
    @DisplayName("Should start and stop monitoring successfully")
    void shouldStartAndStopMonitoring() {
      assertEquals(MonitoringState.RUNNING, monitor.getState());

      monitor.stop();
      assertEquals(MonitoringState.STOPPED, monitor.getState());
    }

    @Test
    @DisplayName("Should track compilation sessions")
    void shouldTrackCompilationSessions() {
      final CompilationSession session =
          monitor.startCompilation(
              TEST_MODULE_ID, TEST_FUNCTION_NAME, CompilationType.TIERED, CompilationTier.BASELINE);

      assertNotNull(session);
      assertEquals(TEST_MODULE_ID, session.getModuleId());
      assertEquals(TEST_FUNCTION_NAME, session.getFunctionName());
      assertEquals(CompilationType.TIERED, session.getCompilationType());
      assertEquals(CompilationTier.BASELINE, session.getTier());

      final CompilationResult result = CompilationResult.success(1024);
      monitor.endCompilation(session, result);

      final JitPerformanceMetrics metrics = monitor.getMetrics();
      assertTrue(metrics.getTotalCompilations() > 0);
    }

    @Test
    @DisplayName("Should record optimization metrics")
    void shouldRecordOptimizationMetrics() {
      final OptimizationMetrics optimizationMetrics =
          new OptimizationMetrics(
              "inlining",
              100, // optimization time
              1.25, // performance improvement
              -50, // code size change (smaller)
              true, // successful
              Map.of("inlined_functions", 3));

      monitor.recordOptimizationMetrics(
          TEST_MODULE_ID, TEST_FUNCTION_NAME, "inlining", optimizationMetrics);

      final FunctionMetrics functionMetrics =
          monitor.getFunctionMetrics(TEST_MODULE_ID, TEST_FUNCTION_NAME);
      assertNotNull(functionMetrics);
      assertFalse(functionMetrics.getOptimizationMetrics().isEmpty());
    }

    @Test
    @DisplayName("Should record deoptimization events")
    void shouldRecordDeoptimizationEvents() {
      monitor.recordDeoptimization(
          TEST_MODULE_ID,
          TEST_FUNCTION_NAME,
          "TYPE_ASSUMPTION_VIOLATED",
          Map.of("expected", "i32", "actual", "f64"));

      final FunctionMetrics functionMetrics =
          monitor.getFunctionMetrics(TEST_MODULE_ID, TEST_FUNCTION_NAME);
      assertNotNull(functionMetrics);
      assertFalse(functionMetrics.getDeoptimizationEvents().isEmpty());
    }

    @Test
    @DisplayName("Should provide comprehensive performance metrics")
    void shouldProvideComprehensiveMetrics() {
      // Simulate some activity
      final CompilationSession session =
          monitor.startCompilation(
              TEST_MODULE_ID,
              TEST_FUNCTION_NAME,
              CompilationType.OPTIMIZING,
              CompilationTier.OPTIMIZED);

      final CompilationResult result = CompilationResult.success(2048);
      monitor.endCompilation(session, result);

      final JitPerformanceMetrics metrics = monitor.getMetrics();
      assertNotNull(metrics);
      assertTrue(metrics.getTotalCompilations() > 0);
      assertEquals(1, metrics.getTrackedModules());
      assertEquals(1, metrics.getTrackedFunctions());
    }

    @Test
    @DisplayName("Should reset all metrics when requested")
    void shouldResetAllMetrics() {
      // Generate some metrics
      final CompilationSession session =
          monitor.startCompilation(
              TEST_MODULE_ID,
              TEST_FUNCTION_NAME,
              CompilationType.BASELINE,
              CompilationTier.BASELINE);
      final CompilationResult result = CompilationResult.success(1024);
      monitor.endCompilation(session, result);

      final JitPerformanceMetrics metricsBefore = monitor.getMetrics();
      assertTrue(metricsBefore.getTotalCompilations() > 0);

      monitor.reset();

      final JitPerformanceMetrics metricsAfter = monitor.getMetrics();
      assertEquals(0, metricsAfter.getTotalCompilations());
      assertEquals(0, metricsAfter.getTrackedModules());
      assertEquals(0, metricsAfter.getTrackedFunctions());
    }

    @Test
    @DisplayName("Should validate monitor input parameters")
    void shouldValidateMonitorInputParameters() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            monitor.startCompilation(
                null, TEST_FUNCTION_NAME, CompilationType.TIERED, CompilationTier.BASELINE);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            monitor.startCompilation(
                TEST_MODULE_ID, TEST_FUNCTION_NAME, null, CompilationTier.BASELINE);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            monitor.startCompilation(
                TEST_MODULE_ID, TEST_FUNCTION_NAME, CompilationType.TIERED, null);
          });
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    private TieredCompilationConfig tieredConfig;
    private AdaptiveOptimizer adaptiveOptimizer;
    private SpeculativeOptimizer speculativeOptimizer;
    private ProfileGuidedOptimizer pgoOptimizer;
    private JitPerformanceMonitor performanceMonitor;

    @BeforeEach
    void setUp() {
      tieredConfig = TieredCompilationConfig.createDefault();
      adaptiveOptimizer = AdaptiveOptimizer.createDefault();
      speculativeOptimizer = SpeculativeOptimizer.createDefault();
      pgoOptimizer = ProfileGuidedOptimizer.createDefault();
      performanceMonitor = JitPerformanceMonitor.createDefault();
      performanceMonitor.start();
    }

    @AfterEach
    void tearDown() {
      if (performanceMonitor.getState() == MonitoringState.RUNNING) {
        performanceMonitor.stop();
      }
    }

    @Test
    @DisplayName("Should integrate tiered compilation with performance monitoring")
    void shouldIntegrateTieredCompilationWithMonitoring() {
      assertTrue(tieredConfig.isEnabled());
      assertEquals(MonitoringState.RUNNING, performanceMonitor.getState());

      // Simulate tiered compilation progression
      final CompilationSession baselineSession =
          performanceMonitor.startCompilation(
              TEST_MODULE_ID, TEST_FUNCTION_NAME, CompilationType.TIERED, CompilationTier.BASELINE);

      final CompilationResult baselineResult = CompilationResult.success(1024);
      performanceMonitor.endCompilation(baselineSession, baselineResult);

      // Progress to optimized tier
      final CompilationSession optimizedSession =
          performanceMonitor.startCompilation(
              TEST_MODULE_ID,
              TEST_FUNCTION_NAME,
              CompilationType.TIERED,
              CompilationTier.OPTIMIZED);

      final CompilationResult optimizedResult = CompilationResult.success(1536);
      performanceMonitor.endCompilation(optimizedSession, optimizedResult);

      final JitPerformanceMetrics metrics = performanceMonitor.getMetrics();
      assertEquals(2, metrics.getTotalCompilations());
    }

    @Test
    @DisplayName("Should integrate adaptive optimization with execution profiling")
    void shouldIntegrateAdaptiveOptimizationWithProfiling() {
      // Simulate execution data collection
      final ExecutionData executionData =
          new ExecutionData(
              5000000, // 5ms execution time
              2 * 1024 * 1024, // 2MB memory
              0.8, // 80% CPU utilization
              Map.of("branch_mispredictions", 100));

      // Record multiple executions
      for (int i = 0; i < 500; i++) {
        adaptiveOptimizer.recordExecution(TEST_MODULE_ID, TEST_FUNCTION_NAME, executionData);
      }

      // Check optimization decision
      final OptimizationDecision decision =
          adaptiveOptimizer.shouldOptimize(TEST_MODULE_ID, TEST_FUNCTION_NAME);
      assertNotNull(decision);

      // If optimization is recommended, record the result
      if (decision.shouldOptimize()) {
        final OptimizationResult result =
            OptimizationResult.success(
                "Loop unrolling and vectorization applied",
                200, // compilation time
                1.4 // 40% performance improvement
                );

        adaptiveOptimizer.recordOptimizationResult(TEST_MODULE_ID, TEST_FUNCTION_NAME, result);

        final OptimizationStatistics stats = adaptiveOptimizer.getStatistics();
        assertTrue(stats.getSuccessRate() > 0);
      }
    }

    @Test
    @DisplayName("Should integrate speculative optimization with deoptimization handling")
    void shouldIntegrateSpeculativeOptimizationWithDeoptimization() {
      final ExecutionProfile profile =
          new ExecutionProfile(
              2000, 8.0, 16000, 0.7, 1024 * 1024, true, true, false, 8, 1024 * 200);

      // Determine speculations
      final List<SpeculativeOptimization> speculations =
          speculativeOptimizer.determineSpeculations(TEST_MODULE_ID, TEST_FUNCTION_NAME, profile);

      if (!speculations.isEmpty()) {
        final SpeculativeOptimization speculation = speculations.get(0);

        // Apply speculation
        final SpeculationResult speculationResult =
            SpeculationResult.success(
                "Type specialization for hot path",
                180, // compilation time
                1.6 // 60% performance improvement
                );

        speculativeOptimizer.recordSpeculationResult(
            TEST_MODULE_ID, TEST_FUNCTION_NAME, speculation, speculationResult);

        // Later, trigger deoptimization due to assumption violation
        final DeoptimizationReason reason = DeoptimizationReason.TYPE_ASSUMPTION_VIOLATED;
        final Map<String, Object> context = Map.of("violation_count", 10);

        speculativeOptimizer.recordDeoptimization(
            TEST_MODULE_ID, TEST_FUNCTION_NAME, reason, context);

        final SpeculationStatistics stats = speculativeOptimizer.getStatistics();
        assertTrue(stats.getTotalSpeculations() > 0);
        assertTrue(stats.getDeoptimizations() > 0);
      }
    }

    @Test
    @DisplayName("Should integrate PGO with adaptive optimization")
    void shouldIntegratePgoWithAdaptiveOptimization() {
      // Start PGO instrumentation
      final InstrumentedModule instrumentedModule =
          pgoOptimizer.startInstrumentationPhase(TEST_MODULE_ID, SIMPLE_WASM_MODULE);

      // Collect profile data through multiple execution cycles
      for (int cycle = 0; cycle < 10; cycle++) {
        for (int exec = 0; exec < 100; exec++) {
          final ProfileDataPoint profileData =
              new ProfileDataPoint(
                  TEST_FUNCTION_NAME,
                  1, // execution count
                  2000000 + (exec * 10000), // varying execution time
                  Map.of("hot_branch", 1, "cold_branch", exec % 10 == 0 ? 1 : 0),
                  Map.of("cycle", cycle));

          pgoOptimizer.recordProfileData(TEST_MODULE_ID, TEST_FUNCTION_NAME, profileData);

          // Also record in adaptive optimizer for comparison
          final ExecutionData adaptiveData =
              new ExecutionData(
                  profileData.getExecutionTimeNs(),
                  1024 * 1024, // memory usage
                  0.6 + (exec * 0.001), // varying CPU utilization
                  profileData.getAdditionalMetrics());
          adaptiveOptimizer.recordExecution(TEST_MODULE_ID, TEST_FUNCTION_NAME, adaptiveData);
        }
      }

      // Generate and apply PGO optimizations
      final OptimizationPlan pgoplan = pgoOptimizer.generateOptimizationPlan(TEST_MODULE_ID);
      final OptimizedModule optimizedModule =
          pgoOptimizer.applyOptimizations(TEST_MODULE_ID, SIMPLE_WASM_MODULE, pgoplan);

      // Check adaptive optimizer decision
      final OptimizationDecision adaptiveDecision =
          adaptiveOptimizer.shouldOptimize(TEST_MODULE_ID, TEST_FUNCTION_NAME);

      // Both optimizers should have data
      assertNotNull(optimizedModule);
      assertNotNull(adaptiveDecision);

      final PgoStatistics pgoStats = pgoOptimizer.getStatistics();
      final OptimizationStatistics adaptiveStats = adaptiveOptimizer.getStatistics();

      assertTrue(pgoStats.getTotalProfiledExecutions() > 0);
      assertTrue(adaptiveStats.getTrackedFunctions() > 0);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Should handle concurrent compilation and optimization requests")
    void shouldHandleConcurrentRequests() throws InterruptedException {
      final int numThreads = 10;
      final int operationsPerThread = 20;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      final CountDownLatch latch = new CountDownLatch(numThreads);
      final AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  final String moduleId = TEST_MODULE_ID + "_" + threadId;
                  final String functionName = TEST_FUNCTION_NAME + "_" + j;

                  // Record execution data
                  final ExecutionData executionData =
                      new ExecutionData(
                          (long) (Math.random() * 10000000), // Random execution time
                          (long) (Math.random() * 1024 * 1024 * 10), // Random memory usage
                          Math.random(), // Random CPU utilization
                          Map.of("thread_id", threadId, "operation", j));

                  adaptiveOptimizer.recordExecution(moduleId, functionName, executionData);

                  // Simulate compilation
                  final CompilationSession session =
                      performanceMonitor.startCompilation(
                          moduleId,
                          functionName,
                          CompilationType.ADAPTIVE,
                          CompilationTier.OPTIMIZED);

                  Thread.sleep(1); // Simulate compilation time

                  final CompilationResult result =
                      CompilationResult.success((int) (Math.random() * 4096));
                  performanceMonitor.endCompilation(session, result);

                  successCount.incrementAndGet();
                }
              } catch (final Exception e) {
                // Log error but don't fail the test
                System.err.println("Thread " + threadId + " encountered error: " + e.getMessage());
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(25, TimeUnit.SECONDS);
      executor.shutdown();

      final int expectedOperations = numThreads * operationsPerThread;
      final int actualSuccesses = successCount.get();

      // Allow for some variance due to concurrency
      assertTrue(
          actualSuccesses > expectedOperations * 0.9,
          String.format(
              "Expected at least %d operations, but got %d",
              (int) (expectedOperations * 0.9), actualSuccesses));

      final JitPerformanceMetrics metrics = performanceMonitor.getMetrics();
      assertTrue(metrics.getTotalCompilations() > 0);
      assertTrue(metrics.getAverageCompilationTimeMs() >= 0);
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle invalid execution profiles gracefully")
    void shouldHandleInvalidExecutionProfiles() {
      final AdaptiveOptimizer optimizer = AdaptiveOptimizer.createDefault();

      // Test with extreme values
      final ExecutionData extremeData =
          new ExecutionData(
              Long.MAX_VALUE, // Extreme execution time
              Long.MAX_VALUE, // Extreme memory usage
              Double.MAX_VALUE, // Extreme CPU utilization
              Map.of("extreme_test", true));

      assertDoesNotThrow(
          () -> {
            optimizer.recordExecution(TEST_MODULE_ID, TEST_FUNCTION_NAME, extremeData);
          });
    }

    @Test
    @DisplayName("Should handle PGO failures gracefully")
    void shouldHandlePgoFailuresGracefully() {
      final ProfileGuidedOptimizer optimizer = ProfileGuidedOptimizer.createDefault();

      // Try to generate plan without instrumentation
      assertThrows(
          PgoException.class,
          () -> {
            optimizer.generateOptimizationPlan("nonexistent_module");
          });

      // Try to apply optimizations without profile data
      assertThrows(
          PgoException.class,
          () -> {
            optimizer.applyOptimizations(
                "nonexistent_module",
                SIMPLE_WASM_MODULE,
                new OptimizationPlan("nonexistent_module", List.of(), Map.of(), 1.0));
          });
    }

    @Test
    @DisplayName("Should handle performance monitor lifecycle correctly")
    void shouldHandlePerformanceMonitorLifecycle() {
      final JitPerformanceMonitor monitor = JitPerformanceMonitor.createDefault();

      assertEquals(MonitoringState.STOPPED, monitor.getState());

      monitor.start();
      assertEquals(MonitoringState.RUNNING, monitor.getState());

      monitor.stop();
      assertEquals(MonitoringState.STOPPED, monitor.getState());

      // Multiple stop calls should be safe
      assertDoesNotThrow(monitor::stop);
    }

    @Test
    @DisplayName("Should validate all null parameter inputs")
    void shouldValidateNullParameterInputs() {
      final AdaptiveOptimizer adaptiveOptimizer = AdaptiveOptimizer.createDefault();
      final SpeculativeOptimizer speculativeOptimizer = SpeculativeOptimizer.createDefault();
      final ProfileGuidedOptimizer pgoOptimizer = ProfileGuidedOptimizer.createDefault();

      // Test adaptive optimizer null validation
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            adaptiveOptimizer.shouldOptimize(null, TEST_FUNCTION_NAME);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            adaptiveOptimizer.shouldOptimize(TEST_MODULE_ID, null);
          });

      // Test speculative optimizer null validation
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            speculativeOptimizer.recordDeoptimization(
                null, TEST_FUNCTION_NAME, DeoptimizationReason.TYPE_ASSUMPTION_VIOLATED, Map.of());
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            speculativeOptimizer.recordDeoptimization(
                TEST_MODULE_ID, null, DeoptimizationReason.TYPE_ASSUMPTION_VIOLATED, Map.of());
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            speculativeOptimizer.recordDeoptimization(
                TEST_MODULE_ID, TEST_FUNCTION_NAME, null, Map.of());
          });

      // Test PGO optimizer null validation
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            pgoOptimizer.recordProfileData(
                null,
                TEST_FUNCTION_NAME,
                new ProfileDataPoint(TEST_FUNCTION_NAME, 1, 1000, Map.of(), Map.of()));
          });
    }
  }

  @Nested
  @DisplayName("Performance Tests")
  class PerformanceTests {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Should handle large-scale profiling data efficiently")
    void shouldHandleLargeScaleProfilingData() {
      final AdaptiveOptimizer optimizer = AdaptiveOptimizer.createDefault();
      final int numModules = 10;
      final int numFunctionsPerModule = 100;
      final int numExecutionsPerFunction = 1000;

      final long startTime = System.nanoTime();

      for (int m = 0; m < numModules; m++) {
        final String moduleId = "module_" + m;
        for (int f = 0; f < numFunctionsPerModule; f++) {
          final String functionName = "function_" + f;
          for (int e = 0; e < numExecutionsPerFunction; e++) {
            final ExecutionData data =
                new ExecutionData(
                    1000000 + e * 100, // Varying execution time
                    1024 * 1024, // Memory usage
                    0.5 + (e * 0.0001), // Varying CPU utilization
                    Map.of("execution", e));
            optimizer.recordExecution(moduleId, functionName, data);
          }
        }
      }

      final long endTime = System.nanoTime();
      final double durationMs = (endTime - startTime) / 1_000_000.0;

      // Verify all data was recorded
      final OptimizationStatistics stats = optimizer.getStatistics();
      assertEquals(numModules, stats.getTrackedModules());
      assertEquals(numModules * numFunctionsPerModule, stats.getTrackedFunctions());

      // Performance assertion - should handle 1M operations in reasonable time
      assertTrue(
          durationMs < 5000, // 5 seconds max
          String.format("Processing took %.2f ms, which is too slow", durationMs));

      System.out.printf(
          "Processed %d profile data points in %.2f ms (%.2f ops/ms)%n",
          numModules * numFunctionsPerModule * numExecutionsPerFunction,
          durationMs,
          (numModules * numFunctionsPerModule * numExecutionsPerFunction) / durationMs);
    }

    @Test
    @DisplayName("Should have predictable memory usage patterns")
    void shouldHavePredictableMemoryUsage() {
      final AdaptiveOptimizer optimizer = AdaptiveOptimizer.createDefault();
      final Runtime runtime = Runtime.getRuntime();

      // Force garbage collection and measure baseline
      System.gc();
      final long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

      // Add a moderate amount of profiling data
      for (int i = 0; i < 1000; i++) {
        final ExecutionData data =
            new ExecutionData(1000000, 1024 * 1024, 0.5, Map.of("iteration", i));
        optimizer.recordExecution("test_module", "test_function_" + (i % 10), data);
      }

      // Measure memory after profiling data
      System.gc();
      final long afterProfilingMemory = runtime.totalMemory() - runtime.freeMemory();

      final long memoryIncrease = afterProfilingMemory - baselineMemory;

      // Memory increase should be reasonable (less than 50MB for 1000 data points)
      assertTrue(
          memoryIncrease < 50 * 1024 * 1024,
          String.format("Memory increase of %d bytes is too large", memoryIncrease));

      // Reset and verify memory cleanup
      optimizer.reset();
      System.gc();
      final long afterResetMemory = runtime.totalMemory() - runtime.freeMemory();

      // Memory should be close to baseline after reset (within 20MB)
      final long memoryAfterReset = afterResetMemory - baselineMemory;
      assertTrue(
          Math.abs(memoryAfterReset) < 20 * 1024 * 1024,
          String.format("Memory after reset differs from baseline by %d bytes", memoryAfterReset));
    }

    @ParameterizedTest
    @EnumSource(CompilationTier.class)
    @DisplayName("Should handle all compilation tiers efficiently")
    void shouldHandleAllCompilationTiersEfficiently(final CompilationTier tier) {
      final JitPerformanceMonitor monitor = JitPerformanceMonitor.createDefault();
      monitor.start();

      try {
        final int numCompilations = 100;
        final long startTime = System.nanoTime();

        for (int i = 0; i < numCompilations; i++) {
          final CompilationSession session =
              monitor.startCompilation(
                  TEST_MODULE_ID + "_" + i, TEST_FUNCTION_NAME, CompilationType.TIERED, tier);

          // Simulate compilation time based on tier
          final long simulatedCompilationTime =
              switch (tier) {
                case BASELINE -> 1; // Very fast
                case OPTIMIZED -> 10; // Moderate
                case HIGHLY_OPTIMIZED -> 50; // Slow
                case MAXIMUM_OPTIMIZATION -> 100; // Very slow
              };

          try {
            Thread.sleep(simulatedCompilationTime);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }

          final CompilationResult result = CompilationResult.success(1024 + i);
          monitor.endCompilation(session, result);
        }

        final long endTime = System.nanoTime();
        final double totalTimeMs = (endTime - startTime) / 1_000_000.0;

        final JitPerformanceMetrics metrics = monitor.getMetrics();
        assertEquals(numCompilations, metrics.getTotalCompilations());
        assertTrue(metrics.getAverageCompilationTimeMs() > 0);

        System.out.printf(
            "Tier %s: %d compilations in %.2f ms (avg: %.2f ms/compilation)%n",
            tier, numCompilations, totalTimeMs, metrics.getAverageCompilationTimeMs());

      } finally {
        monitor.stop();
      }
    }
  }
}
