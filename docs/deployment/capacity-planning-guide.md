# Capacity Planning Guide

This guide provides comprehensive capacity planning strategies for Wasmtime4j deployments, covering resource estimation, scaling strategies, performance modeling, and infrastructure optimization for production environments.

## Table of Contents
- [Capacity Planning Overview](#capacity-planning-overview)
- [Resource Requirements Analysis](#resource-requirements-analysis)
- [Performance Modeling](#performance-modeling)
- [Scaling Strategies](#scaling-strategies)
- [Infrastructure Sizing](#infrastructure-sizing)
- [Cost Optimization](#cost-optimization)
- [Monitoring and Adjustment](#monitoring-and-adjustment)
- [Disaster Recovery Planning](#disaster-recovery-planning)

## Capacity Planning Overview

### Planning Methodology

```
┌─────────────────────────────────────────────────────────────┐
│                 Capacity Planning Process                   │
├─────────────────────────────────────────────────────────────┤
│ 1. Requirements Analysis                                    │
│    ├── Business requirements                               │
│    ├── Performance requirements                            │
│    ├── Availability requirements                           │
│    └── Growth projections                                  │
│                                                             │
│ 2. Baseline Measurement                                     │
│    ├── Current system metrics                              │
│    ├── WebAssembly module characteristics                  │
│    ├── Execution patterns                                  │
│    └── Resource utilization                                │
│                                                             │
│ 3. Performance Modeling                                     │
│    ├── Load testing                                        │
│    ├── Stress testing                                      │
│    ├── Capacity modeling                                   │
│    └── Bottleneck identification                           │
│                                                             │
│ 4. Infrastructure Design                                    │
│    ├── Hardware sizing                                     │
│    ├── Network capacity                                    │
│    ├── Storage requirements                                │
│    └── Redundancy planning                                 │
│                                                             │
│ 5. Implementation & Monitoring                             │
│    ├── Deployment strategy                                 │
│    ├── Performance monitoring                              │
│    ├── Capacity alerts                                     │
│    └── Continuous optimization                             │
└─────────────────────────────────────────────────────────────┘
```

### Key Metrics for Planning

```java
@Component
public class CapacityPlanningMetrics {

    // Core business metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);

    // Performance metrics
    private final ConcurrentHashMap<String, LatencyStats> moduleLatencies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ThroughputStats> moduleThroughput = new ConcurrentHashMap<>();

    // Resource metrics
    private final AtomicLong totalMemoryUsed = new AtomicLong(0);
    private final AtomicLong peakMemoryUsed = new AtomicLong(0);
    private final AtomicDouble averageCpuUsage = new AtomicDouble(0.0);

    public CapacityMetrics calculateCapacityMetrics(Duration period) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(period);

        return CapacityMetrics.builder()
            .requestRate(calculateRequestRate(startTime, endTime))
            .averageLatency(calculateAverageLatency(startTime, endTime))
            .p95Latency(calculateP95Latency(startTime, endTime))
            .errorRate(calculateErrorRate(startTime, endTime))
            .memoryUtilization(calculateMemoryUtilization())
            .cpuUtilization(calculateCpuUtilization())
            .concurrentExecutions(getCurrentConcurrentExecutions())
            .moduleDistribution(getModuleExecutionDistribution(startTime, endTime))
            .build();
    }

    public ResourcePrediction predictResourceNeeds(int expectedRequestsPerSecond,
                                                  Duration planningHorizon) {
        // Analyze historical patterns
        CapacityMetrics baseline = calculateCapacityMetrics(Duration.ofDays(7));

        // Calculate scaling factors
        double scaleFactor = expectedRequestsPerSecond / baseline.getRequestRate();

        // Predict resource requirements
        ResourceRequirements predicted = ResourceRequirements.builder()
            .cpuCores(Math.ceil(baseline.getCpuUtilization() * scaleFactor * 1.2)) // 20% buffer
            .memoryGb(Math.ceil((baseline.getMemoryUtilization() * scaleFactor * 1.3) / (1024 * 1024 * 1024))) // 30% buffer
            .storageGb(calculateStorageNeeds(expectedRequestsPerSecond, planningHorizon))
            .networkBandwidthMbps(calculateNetworkNeeds(expectedRequestsPerSecond))
            .build();

        return ResourcePrediction.builder()
            .targetThroughput(expectedRequestsPerSecond)
            .planningHorizon(planningHorizon)
            .predictedRequirements(predicted)
            .confidenceLevel(calculateConfidenceLevel(baseline))
            .recommendations(generateRecommendations(predicted, baseline))
            .build();
    }
}
```

## Resource Requirements Analysis

### WebAssembly Module Profiling

```java
@Component
public class ModuleResourceProfiler {

    public ModuleProfile analyzeModule(String moduleId, byte[] moduleBytes) {
        ModuleProfile.Builder profile = ModuleProfile.builder()
            .moduleId(moduleId)
            .moduleSize(moduleBytes.length)
            .analysisTimestamp(Instant.now());

        try {
            // Static analysis
            WasmModuleAnalysis staticAnalysis = performStaticAnalysis(moduleBytes);
            profile.staticAnalysis(staticAnalysis);

            // Runtime profiling
            RuntimeProfile runtimeProfile = performRuntimeProfiling(moduleId, moduleBytes);
            profile.runtimeProfile(runtimeProfile);

            // Resource estimation
            ResourceEstimates estimates = estimateResourceRequirements(staticAnalysis, runtimeProfile);
            profile.resourceEstimates(estimates);

            return profile.build();

        } catch (Exception e) {
            throw new AnalysisException("Failed to analyze module: " + moduleId, e);
        }
    }

    private WasmModuleAnalysis performStaticAnalysis(byte[] moduleBytes) {
        // Parse WebAssembly module structure
        WasmModule module = WasmParser.parse(moduleBytes);

        return WasmModuleAnalysis.builder()
            .functionCount(module.getFunctions().size())
            .memoryPages(module.getMemorySize())
            .importCount(module.getImports().size())
            .exportCount(module.getExports().size())
            .tableSize(module.getTableSize())
            .globalCount(module.getGlobals().size())
            .codeSize(module.getCodeSection().length)
            .dataSize(module.getDataSection().length)
            .complexity(calculateComplexity(module))
            .build();
    }

    private RuntimeProfile performRuntimeProfiling(String moduleId, byte[] moduleBytes) {
        List<ExecutionProfile> profiles = new ArrayList<>();

        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            Engine engine = runtime.createEngine();
            Module module = runtime.compileModule(engine, moduleBytes);

            // Profile different execution scenarios
            for (ProfileScenario scenario : getProfileScenarios()) {
                ExecutionProfile execProfile = profileExecution(runtime, module, scenario);
                profiles.add(execProfile);
            }
        }

        return RuntimeProfile.builder()
            .profiles(profiles)
            .compilationTime(calculateAverageCompilationTime(profiles))
            .instantiationTime(calculateAverageInstantiationTime(profiles))
            .averageExecutionTime(calculateAverageExecutionTime(profiles))
            .memoryFootprint(calculateMemoryFootprint(profiles))
            .cpuIntensity(calculateCpuIntensity(profiles))
            .build();
    }

    private ExecutionProfile profileExecution(WasmRuntime runtime, Module module,
                                            ProfileScenario scenario) {
        long startTime = System.nanoTime();

        // Measure compilation
        long compilationStart = System.nanoTime();
        Instance instance = runtime.instantiate(module);
        long compilationTime = System.nanoTime() - compilationStart;

        // Measure execution
        MemoryMonitor memoryMonitor = new MemoryMonitor();
        CpuMonitor cpuMonitor = new CpuMonitor();

        memoryMonitor.start();
        cpuMonitor.start();

        long executionStart = System.nanoTime();
        ExecutionResult result = executeScenario(instance, scenario);
        long executionTime = System.nanoTime() - executionStart;

        MemoryUsage memoryUsage = memoryMonitor.stop();
        CpuUsage cpuUsage = cpuMonitor.stop();

        long totalTime = System.nanoTime() - startTime;

        return ExecutionProfile.builder()
            .scenario(scenario)
            .totalTime(Duration.ofNanos(totalTime))
            .compilationTime(Duration.ofNanos(compilationTime))
            .executionTime(Duration.ofNanos(executionTime))
            .memoryUsage(memoryUsage)
            .cpuUsage(cpuUsage)
            .result(result)
            .build();
    }

    private ResourceEstimates estimateResourceRequirements(WasmModuleAnalysis staticAnalysis,
                                                         RuntimeProfile runtimeProfile) {
        // CPU estimation based on complexity and execution time
        double cpuCoresNeeded = estimateCpuRequirements(staticAnalysis, runtimeProfile);

        // Memory estimation based on module size and runtime footprint
        long memoryBytesNeeded = estimateMemoryRequirements(staticAnalysis, runtimeProfile);

        // Storage estimation for module cache and logs
        long storageBytesNeeded = estimateStorageRequirements(staticAnalysis, runtimeProfile);

        // Network estimation for module loading and data transfer
        long networkBandwidthNeeded = estimateNetworkRequirements(staticAnalysis, runtimeProfile);

        return ResourceEstimates.builder()
            .cpuCores(cpuCoresNeeded)
            .memoryBytes(memoryBytesNeeded)
            .storageBytes(storageBytesNeeded)
            .networkBandwidthBps(networkBandwidthNeeded)
            .scalingFactor(calculateScalingFactor(runtimeProfile))
            .confidence(calculateEstimateConfidence(staticAnalysis, runtimeProfile))
            .build();
    }

    private double estimateCpuRequirements(WasmModuleAnalysis staticAnalysis,
                                         RuntimeProfile runtimeProfile) {
        // Base CPU requirement from execution time
        double avgExecutionTimeMs = runtimeProfile.getAverageExecutionTime().toMillis();
        double cpuIntensity = runtimeProfile.getCpuIntensity();

        // Complexity factor from static analysis
        double complexityFactor = Math.log(staticAnalysis.getComplexity()) / Math.log(2);

        // Compilation overhead (amortized)
        double compilationOverhead = runtimeProfile.getCompilationTime().toMillis() / 1000.0;

        // CPU cores needed for 1 request per second
        double baseCpuNeeded = (avgExecutionTimeMs / 1000.0) * cpuIntensity * complexityFactor;

        // Add compilation overhead (assuming 10% cache miss rate)
        double totalCpuNeeded = baseCpuNeeded + (compilationOverhead * 0.1);

        return Math.max(0.1, totalCpuNeeded); // Minimum 0.1 cores
    }

    private long estimateMemoryRequirements(WasmModuleAnalysis staticAnalysis,
                                          RuntimeProfile runtimeProfile) {
        // Base memory from WebAssembly linear memory
        long wasmMemoryBytes = staticAnalysis.getMemoryPages() * 65536L;

        // Runtime overhead (JVM heap, native structures)
        long runtimeOverheadBytes = staticAnalysis.getCodeSize() * 3L; // 3x code size for JIT

        // Module cache memory
        long moduleCacheBytes = staticAnalysis.getCodeSize() + staticAnalysis.getDataSize();

        // Peak execution memory from profiling
        long peakExecutionMemory = runtimeProfile.getMemoryFootprint();

        // Total with safety factor
        long totalMemory = wasmMemoryBytes + runtimeOverheadBytes +
                          moduleCacheBytes + peakExecutionMemory;

        return Math.round(totalMemory * 1.5); // 50% safety margin
    }
}
```

### System Resource Modeling

```java
@Component
public class SystemResourceModel {

    public SystemCapacity modelSystemCapacity(List<ModuleProfile> moduleProfiles,
                                            TrafficPattern trafficPattern) {
        // Aggregate module requirements
        ResourceRequirements aggregatedRequirements = aggregateModuleRequirements(moduleProfiles);

        // Apply traffic patterns
        ResourceRequirements trafficAdjustedRequirements =
            applyTrafficPattern(aggregatedRequirements, trafficPattern);

        // Account for system overhead
        ResourceRequirements systemRequirements =
            addSystemOverhead(trafficAdjustedRequirements);

        // Calculate horizontal scaling requirements
        ScalingParameters scaling = calculateScalingParameters(systemRequirements, trafficPattern);

        return SystemCapacity.builder()
            .baseRequirements(systemRequirements)
            .scalingParameters(scaling)
            .recommendedConfiguration(generateRecommendedConfiguration(systemRequirements, scaling))
            .capacityLimits(calculateCapacityLimits(systemRequirements))
            .build();
    }

    private ResourceRequirements aggregateModuleRequirements(List<ModuleProfile> profiles) {
        double totalCpuCores = 0;
        long totalMemoryBytes = 0;
        long totalStorageBytes = 0;
        long totalNetworkBps = 0;

        for (ModuleProfile profile : profiles) {
            ResourceEstimates estimates = profile.getResourceEstimates();

            // Weight by execution frequency
            double executionWeight = profile.getExecutionFrequency();

            totalCpuCores += estimates.getCpuCores() * executionWeight;
            totalMemoryBytes += estimates.getMemoryBytes();  // Memory is not additive in the same way
            totalStorageBytes += estimates.getStorageBytes();
            totalNetworkBps += estimates.getNetworkBandwidthBps() * executionWeight;
        }

        return ResourceRequirements.builder()
            .cpuCores(totalCpuCores)
            .memoryBytes(Math.max(totalMemoryBytes, calculateMinimumMemory(profiles)))
            .storageBytes(totalStorageBytes)
            .networkBandwidthBps(totalNetworkBps)
            .build();
    }

    private ResourceRequirements applyTrafficPattern(ResourceRequirements base,
                                                   TrafficPattern pattern) {
        // Apply peak traffic multiplier
        double peakMultiplier = pattern.getPeakTrafficMultiplier();

        // Apply concurrency factor
        double concurrencyFactor = pattern.getConcurrencyFactor();

        return ResourceRequirements.builder()
            .cpuCores(base.getCpuCores() * peakMultiplier * concurrencyFactor)
            .memoryBytes(Math.round(base.getMemoryBytes() * concurrencyFactor))
            .storageBytes(base.getStorageBytes()) // Storage doesn't scale with traffic
            .networkBandwidthBps(Math.round(base.getNetworkBandwidthBps() * peakMultiplier))
            .build();
    }

    private ResourceRequirements addSystemOverhead(ResourceRequirements application) {
        // Operating system overhead
        double osOverheadCpu = 0.5; // 0.5 cores for OS
        long osOverheadMemory = 2L * 1024 * 1024 * 1024; // 2GB for OS

        // JVM overhead
        double jvmOverheadCpu = application.getCpuCores() * 0.1; // 10% JVM overhead
        long jvmOverheadMemory = Math.round(application.getMemoryBytes() * 0.3); // 30% JVM overhead

        // Monitoring and logging overhead
        double monitoringOverheadCpu = 0.2; // 0.2 cores for monitoring
        long monitoringOverheadMemory = 512L * 1024 * 1024; // 512MB for monitoring

        // Network overhead
        long networkOverheadBps = Math.round(application.getNetworkBandwidthBps() * 0.2); // 20% protocol overhead

        return ResourceRequirements.builder()
            .cpuCores(application.getCpuCores() + osOverheadCpu + jvmOverheadCpu + monitoringOverheadCpu)
            .memoryBytes(application.getMemoryBytes() + osOverheadMemory + jvmOverheadMemory + monitoringOverheadMemory)
            .storageBytes(application.getStorageBytes() + calculateLogStorageNeeds())
            .networkBandwidthBps(application.getNetworkBandwidthBps() + networkOverheadBps)
            .build();
    }

    private ScalingParameters calculateScalingParameters(ResourceRequirements requirements,
                                                       TrafficPattern pattern) {
        // Determine optimal instance sizing
        InstanceSize optimalSize = determineOptimalInstanceSize(requirements);

        // Calculate number of instances needed
        int minInstances = calculateMinimumInstances(requirements, optimalSize);
        int maxInstances = calculateMaximumInstances(requirements, optimalSize, pattern);

        // Determine scaling triggers
        ScalingTriggers triggers = calculateScalingTriggers(pattern);

        return ScalingParameters.builder()
            .optimalInstanceSize(optimalSize)
            .minInstances(minInstances)
            .maxInstances(maxInstances)
            .scalingTriggers(triggers)
            .scaleUpCooldown(Duration.ofMinutes(5))
            .scaleDownCooldown(Duration.ofMinutes(10))
            .build();
    }
}
```

## Performance Modeling

### Load Testing Framework

```java
@Component
public class PerformanceModelingService {

    public PerformanceModel buildPerformanceModel(String moduleId,
                                                List<LoadTestScenario> scenarios) {
        List<LoadTestResult> results = new ArrayList<>();

        for (LoadTestScenario scenario : scenarios) {
            LoadTestResult result = executeLoadTest(moduleId, scenario);
            results.add(result);
        }

        return PerformanceModel.builder()
            .moduleId(moduleId)
            .testResults(results)
            .performanceCharacteristics(analyzePerformance(results))
            .scalingModel(buildScalingModel(results))
            .bottleneckAnalysis(identifyBottlenecks(results))
            .recommendations(generatePerformanceRecommendations(results))
            .build();
    }

    private LoadTestResult executeLoadTest(String moduleId, LoadTestScenario scenario) {
        LoadTestConfiguration config = LoadTestConfiguration.builder()
            .moduleId(moduleId)
            .targetRps(scenario.getTargetRps())
            .duration(scenario.getDuration())
            .concurrency(scenario.getConcurrency())
            .rampUpTime(scenario.getRampUpTime())
            .dataSet(scenario.getDataSet())
            .build();

        LoadTestExecutor executor = new LoadTestExecutor(config);
        LoadTestMetrics metrics = executor.execute();

        return LoadTestResult.builder()
            .scenario(scenario)
            .configuration(config)
            .metrics(metrics)
            .resourceUsage(captureResourceUsage(config.getDuration()))
            .errorAnalysis(analyzeErrors(metrics))
            .build();
    }

    public static class LoadTestExecutor {
        private final LoadTestConfiguration config;
        private final WasmRuntime runtime;
        private final ExecutorService executorService;
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final List<Long> responseTimes = new CopyOnWriteArrayList<>();

        public LoadTestExecutor(LoadTestConfiguration config) {
            this.config = config;
            this.runtime = WasmRuntimeFactory.create();
            this.executorService = Executors.newFixedThreadPool(config.getConcurrency());
        }

        public LoadTestMetrics execute() {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + config.getDuration().toMillis();

            // Ramp up phase
            rampUp();

            // Steady state load
            applySteadyLoad(endTime);

            // Collect results
            executorService.shutdown();
            try {
                executorService.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return LoadTestMetrics.builder()
                .totalRequests(requestCount.get())
                .successfulRequests(successCount.get())
                .failedRequests(errorCount.get())
                .averageResponseTime(calculateAverageResponseTime())
                .p95ResponseTime(calculatePercentile(responseTimes, 0.95))
                .p99ResponseTime(calculatePercentile(responseTimes, 0.99))
                .maxResponseTime(responseTimes.stream().max(Long::compareTo).orElse(0L))
                .throughput(calculateThroughput())
                .errorRate(calculateErrorRate())
                .build();
        }

        private void rampUp() {
            Duration rampUpTime = config.getRampUpTime();
            int targetConcurrency = config.getConcurrency();

            for (int i = 1; i <= targetConcurrency; i++) {
                // Gradually increase load
                executorService.submit(new LoadTestWorker());

                try {
                    Thread.sleep(rampUpTime.toMillis() / targetConcurrency);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void applySteadyLoad(long endTime) {
            while (System.currentTimeMillis() < endTime && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(100); // Monitor every 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private class LoadTestWorker implements Runnable {
            @Override
            public void run() {
                try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
                    Engine engine = runtime.createEngine();
                    Module module = loadModule(config.getModuleId());
                    Instance instance = runtime.instantiate(module);

                    while (!Thread.currentThread().isInterrupted()) {
                        long requestStart = System.nanoTime();

                        try {
                            // Execute test request
                            executeTestRequest(instance);
                            successCount.incrementAndGet();

                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        } finally {
                            long responseTime = (System.nanoTime() - requestStart) / 1_000_000; // Convert to ms
                            responseTimes.add(responseTime);
                            requestCount.incrementAndGet();
                        }

                        // Rate limiting
                        try {
                            Thread.sleep(calculateSleepTime());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("Load test worker failed", e);
                }
            }

            private long calculateSleepTime() {
                // Calculate sleep time to maintain target RPS
                double targetRps = config.getTargetRps();
                double requestsPerThread = targetRps / config.getConcurrency();
                return Math.round(1000.0 / requestsPerThread);
            }
        }
    }

    private PerformanceCharacteristics analyzePerformance(List<LoadTestResult> results) {
        // Analyze latency patterns
        LatencyProfile latencyProfile = analyzeLatencyProfile(results);

        // Analyze throughput patterns
        ThroughputProfile throughputProfile = analyzeThroughputProfile(results);

        // Analyze resource consumption patterns
        ResourceConsumptionProfile resourceProfile = analyzeResourceConsumption(results);

        // Analyze error patterns
        ErrorProfile errorProfile = analyzeErrorProfile(results);

        return PerformanceCharacteristics.builder()
            .latencyProfile(latencyProfile)
            .throughputProfile(throughputProfile)
            .resourceProfile(resourceProfile)
            .errorProfile(errorProfile)
            .scalabilityFactors(calculateScalabilityFactors(results))
            .build();
    }

    private ScalingModel buildScalingModel(List<LoadTestResult> results) {
        // Linear regression for throughput vs resources
        LinearRegressionModel throughputModel = buildThroughputModel(results);

        // Response time model
        ResponseTimeModel responseTimeModel = buildResponseTimeModel(results);

        // Resource utilization model
        ResourceUtilizationModel resourceModel = buildResourceUtilizationModel(results);

        return ScalingModel.builder()
            .throughputModel(throughputModel)
            .responseTimeModel(responseTimeModel)
            .resourceModel(resourceModel)
            .predictiveAccuracy(calculateModelAccuracy(results))
            .validRanges(determineValidRanges(results))
            .build();
    }
}
```

## Scaling Strategies

### Horizontal Scaling Configuration

```java
@Configuration
public class HorizontalScalingConfiguration {

    @Bean
    public HorizontalPodAutoscaler wasmtime4jHPA() {
        return HorizontalPodAutoscaler.builder()
            .metadata(ObjectMeta.builder()
                .name("wasmtime4j-hpa")
                .namespace("production")
                .build())
            .spec(HorizontalPodAutoscalerSpec.builder()
                .scaleTargetRef(CrossVersionObjectReference.builder()
                    .apiVersion("apps/v1")
                    .kind("Deployment")
                    .name("wasmtime4j-app")
                    .build())
                .minReplicas(3)
                .maxReplicas(50)
                .metrics(Arrays.asList(
                    // CPU utilization
                    V2MetricSpec.builder()
                        .type("Resource")
                        .resource(ResourceMetricSource.builder()
                            .name("cpu")
                            .target(MetricTarget.builder()
                                .type("Utilization")
                                .averageUtilization(70)
                                .build())
                            .build())
                        .build(),

                    // Memory utilization
                    V2MetricSpec.builder()
                        .type("Resource")
                        .resource(ResourceMetricSource.builder()
                            .name("memory")
                            .target(MetricTarget.builder()
                                .type("Utilization")
                                .averageUtilization(80)
                                .build())
                            .build())
                        .build(),

                    // Custom metric: WebAssembly execution queue length
                    V2MetricSpec.builder()
                        .type("Pods")
                        .pods(PodsMetricSource.builder()
                            .metric(MetricIdentifier.builder()
                                .name("wasm_execution_queue_length")
                                .build())
                            .target(MetricTarget.builder()
                                .type("AverageValue")
                                .averageValue(Quantity.fromString("10"))
                                .build())
                            .build())
                        .build(),

                    // Custom metric: Average response time
                    V2MetricSpec.builder()
                        .type("Pods")
                        .pods(PodsMetricSource.builder()
                            .metric(MetricIdentifier.builder()
                                .name("wasm_avg_response_time_ms")
                                .build())
                            .target(MetricTarget.builder()
                                .type("AverageValue")
                                .averageValue(Quantity.fromString("1000"))
                                .build())
                            .build())
                        .build()
                ))
                .behavior(HorizontalPodAutoscalerBehavior.builder()
                    .scaleUp(HPAScalingRules.builder()
                        .stabilizationWindowSeconds(60)
                        .policies(Arrays.asList(
                            HPAScalingPolicy.builder()
                                .type("Percent")
                                .value(100) // Scale up by 100% max
                                .periodSeconds(60)
                                .build(),
                            HPAScalingPolicy.builder()
                                .type("Pods")
                                .value(5) // Add max 5 pods at once
                                .periodSeconds(60)
                                .build()
                        ))
                        .build())
                    .scaleDown(HPAScalingRules.builder()
                        .stabilizationWindowSeconds(300) // 5 minute stabilization
                        .policies(Arrays.asList(
                            HPAScalingPolicy.builder()
                                .type("Percent")
                                .value(50) // Scale down by 50% max
                                .periodSeconds(300)
                                .build(),
                            HPAScalingPolicy.builder()
                                .type("Pods")
                                .value(2) // Remove max 2 pods at once
                                .periodSeconds(300)
                                .build()
                        ))
                        .build())
                    .build())
                .build())
            .build();
    }

    @Bean
    public VerticalPodAutoscaler wasmtime4jVPA() {
        return VerticalPodAutoscaler.builder()
            .metadata(ObjectMeta.builder()
                .name("wasmtime4j-vpa")
                .namespace("production")
                .build())
            .spec(VerticalPodAutoscalerSpec.builder()
                .targetRef(CrossVersionObjectReference.builder()
                    .apiVersion("apps/v1")
                    .kind("Deployment")
                    .name("wasmtime4j-app")
                    .build())
                .updatePolicy(PodUpdatePolicy.builder()
                    .updateMode("Auto") // Can be "Auto", "Initial", "Off"
                    .build())
                .resourcePolicy(PodResourcePolicy.builder()
                    .containerPolicies(Arrays.asList(
                        ContainerResourcePolicy.builder()
                            .containerName("wasmtime4j")
                            .minAllowed(Map.of(
                                "cpu", Quantity.fromString("100m"),
                                "memory", Quantity.fromString("256Mi")
                            ))
                            .maxAllowed(Map.of(
                                "cpu", Quantity.fromString("4"),
                                "memory", Quantity.fromString("8Gi")
                            ))
                            .controlledResources(Arrays.asList("cpu", "memory"))
                            .build()
                    ))
                    .build())
                .build())
            .build();
    }
}
```

### Predictive Scaling

```java
@Component
public class PredictiveScalingService {

    private final TimeSeriesPredictor predictor;
    private final CapacityMetricsCollector metricsCollector;
    private final ScalingActionExecutor scalingExecutor;

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void evaluatePredictiveScaling() {
        try {
            // Collect recent metrics
            TimeSeriesData recentMetrics = metricsCollector.collectMetrics(Duration.ofHours(24));

            // Predict future demand
            PredictionResult prediction = predictor.predict(recentMetrics, Duration.ofHours(2));

            // Evaluate scaling needs
            ScalingRecommendation recommendation = evaluateScalingNeed(prediction);

            if (recommendation.isScalingNeeded()) {
                executeScalingAction(recommendation);
            }

        } catch (Exception e) {
            log.error("Predictive scaling evaluation failed", e);
        }
    }

    private ScalingRecommendation evaluateScalingNeed(PredictionResult prediction) {
        CurrentCapacity currentCapacity = getCurrentCapacity();
        PredictedDemand predictedDemand = prediction.getDemand();

        // Calculate capacity gap
        CapacityGap gap = calculateCapacityGap(currentCapacity, predictedDemand);

        if (gap.getType() == CapacityGapType.SHORTAGE) {
            return ScalingRecommendation.scaleUp()
                .targetCapacity(predictedDemand.getRequiredCapacity())
                .urgency(gap.getUrgency())
                .reason("Predicted capacity shortage in " + gap.getTimeToShortage())
                .confidence(prediction.getConfidence())
                .build();

        } else if (gap.getType() == CapacityGapType.EXCESS) {
            return ScalingRecommendation.scaleDown()
                .targetCapacity(predictedDemand.getRequiredCapacity())
                .urgency(Urgency.LOW)
                .reason("Predicted capacity excess for " + gap.getDurationOfExcess())
                .confidence(prediction.getConfidence())
                .build();

        } else {
            return ScalingRecommendation.noAction()
                .reason("Current capacity adequate for predicted demand")
                .build();
        }
    }

    public static class TimeSeriesPredictor {

        public PredictionResult predict(TimeSeriesData historicalData, Duration predictionHorizon) {
            // Implement time series forecasting
            // This could use ARIMA, exponential smoothing, or ML models

            // Decompose time series
            TimeSeriesDecomposition decomposition = decomposeTimeSeries(historicalData);

            // Identify patterns
            List<Pattern> patterns = identifyPatterns(decomposition);

            // Make predictions
            PredictedValues predictions = forecastValues(decomposition, patterns, predictionHorizon);

            // Calculate confidence intervals
            ConfidenceInterval confidence = calculateConfidence(predictions, historicalData);

            return PredictionResult.builder()
                .predictions(predictions)
                .confidence(confidence.getLevel())
                .patterns(patterns)
                .validUntil(Instant.now().plus(predictionHorizon))
                .build();
        }

        private TimeSeriesDecomposition decomposeTimeSeries(TimeSeriesData data) {
            // Decompose into trend, seasonal, and residual components
            TrendComponent trend = calculateTrend(data);
            SeasonalComponent seasonal = calculateSeasonal(data, trend);
            ResidualComponent residual = calculateResidual(data, trend, seasonal);

            return TimeSeriesDecomposition.builder()
                .trend(trend)
                .seasonal(seasonal)
                .residual(residual)
                .build();
        }

        private List<Pattern> identifyPatterns(TimeSeriesDecomposition decomposition) {
            List<Pattern> patterns = new ArrayList<>();

            // Daily patterns
            Pattern dailyPattern = identifyDailyPattern(decomposition.getSeasonal());
            if (dailyPattern.getSignificance() > 0.8) {
                patterns.add(dailyPattern);
            }

            // Weekly patterns
            Pattern weeklyPattern = identifyWeeklyPattern(decomposition.getSeasonal());
            if (weeklyPattern.getSignificance() > 0.8) {
                patterns.add(weeklyPattern);
            }

            // Growth trends
            Pattern growthPattern = identifyGrowthTrend(decomposition.getTrend());
            if (growthPattern.getSignificance() > 0.7) {
                patterns.add(growthPattern);
            }

            return patterns;
        }
    }
}
```

## Infrastructure Sizing

### Cloud Provider Sizing Guide

```java
@Component
public class CloudInfrastructureSizer {

    public CloudSizingRecommendation recommendInfrastructure(
            ResourceRequirements requirements,
            CloudProvider provider,
            DeploymentRegion region) {

        InstanceTypeCatalog catalog = getInstanceCatalog(provider, region);

        // Find optimal instance types
        List<InstanceTypeRecommendation> instanceRecommendations =
            findOptimalInstanceTypes(requirements, catalog);

        // Calculate storage requirements
        StorageRecommendation storageRecommendation =
            calculateStorageRequirements(requirements, provider);

        // Calculate network requirements
        NetworkRecommendation networkRecommendation =
            calculateNetworkRequirements(requirements, provider);

        // Calculate cost estimates
        CostEstimate costEstimate = calculateCosts(
            instanceRecommendations, storageRecommendation, networkRecommendation, provider);

        return CloudSizingRecommendation.builder()
            .provider(provider)
            .region(region)
            .instanceRecommendations(instanceRecommendations)
            .storageRecommendation(storageRecommendation)
            .networkRecommendation(networkRecommendation)
            .costEstimate(costEstimate)
            .scalingConfiguration(generateScalingConfiguration(instanceRecommendations))
            .build();
    }

    private List<InstanceTypeRecommendation> findOptimalInstanceTypes(
            ResourceRequirements requirements,
            InstanceTypeCatalog catalog) {

        List<InstanceTypeRecommendation> recommendations = new ArrayList<>();

        // CPU-optimized instances
        InstanceType cpuOptimized = findBestCpuOptimizedInstance(requirements, catalog);
        if (cpuOptimized != null) {
            recommendations.add(InstanceTypeRecommendation.builder()
                .instanceType(cpuOptimized)
                .recommendationType(RecommendationType.CPU_OPTIMIZED)
                .instanceCount(calculateInstanceCount(requirements, cpuOptimized))
                .utilizationEstimate(calculateUtilization(requirements, cpuOptimized))
                .costEfficiency(calculateCostEfficiency(requirements, cpuOptimized))
                .suitability(calculateSuitability(requirements, cpuOptimized))
                .build());
        }

        // Memory-optimized instances
        InstanceType memoryOptimized = findBestMemoryOptimizedInstance(requirements, catalog);
        if (memoryOptimized != null) {
            recommendations.add(InstanceTypeRecommendation.builder()
                .instanceType(memoryOptimized)
                .recommendationType(RecommendationType.MEMORY_OPTIMIZED)
                .instanceCount(calculateInstanceCount(requirements, memoryOptimized))
                .utilizationEstimate(calculateUtilization(requirements, memoryOptimized))
                .costEfficiency(calculateCostEfficiency(requirements, memoryOptimized))
                .suitability(calculateSuitability(requirements, memoryOptimized))
                .build());
        }

        // Balanced instances
        InstanceType balanced = findBestBalancedInstance(requirements, catalog);
        if (balanced != null) {
            recommendations.add(InstanceTypeRecommendation.builder()
                .instanceType(balanced)
                .recommendationType(RecommendationType.BALANCED)
                .instanceCount(calculateInstanceCount(requirements, balanced))
                .utilizationEstimate(calculateUtilization(requirements, balanced))
                .costEfficiency(calculateCostEfficiency(requirements, balanced))
                .suitability(calculateSuitability(requirements, balanced))
                .build());
        }

        // Sort by suitability score
        recommendations.sort((a, b) -> Double.compare(b.getSuitability(), a.getSuitability()));

        return recommendations;
    }

    // AWS-specific sizing
    public AWSInfrastructureRecommendation recommendAWSInfrastructure(
            ResourceRequirements requirements) {

        // EC2 instance recommendations
        List<EC2InstanceRecommendation> ec2Recommendations = recommendEC2Instances(requirements);

        // EKS cluster configuration
        EKSClusterRecommendation eksRecommendation = recommendEKSCluster(requirements);

        // RDS configuration for metadata storage
        RDSInstanceRecommendation rdsRecommendation = recommendRDSInstance(requirements);

        // ElastiCache for module caching
        ElastiCacheRecommendation cacheRecommendation = recommendElastiCache(requirements);

        // S3 for module storage
        S3StorageRecommendation s3Recommendation = recommendS3Storage(requirements);

        return AWSInfrastructureRecommendation.builder()
            .ec2Recommendations(ec2Recommendations)
            .eksRecommendation(eksRecommendation)
            .rdsRecommendation(rdsRecommendation)
            .cacheRecommendation(cacheRecommendation)
            .s3Recommendation(s3Recommendation)
            .networkConfiguration(recommendAWSNetworking(requirements))
            .securityConfiguration(recommendAWSSecurity())
            .monitoringConfiguration(recommendAWSMonitoring())
            .build();
    }

    private List<EC2InstanceRecommendation> recommendEC2Instances(ResourceRequirements requirements) {
        List<EC2InstanceRecommendation> recommendations = new ArrayList<>();

        // Compute-optimized for CPU-intensive WebAssembly workloads
        if (requirements.getCpuCores() > 8) {
            recommendations.add(EC2InstanceRecommendation.builder()
                .instanceType("c6i.4xlarge") // 16 vCPUs, 32GB RAM
                .instanceCount(Math.ceil(requirements.getCpuCores() / 16.0))
                .use("High CPU utilization WebAssembly modules")
                .monthlyEstimate(calculateEC2Cost("c6i.4xlarge",
                    Math.ceil(requirements.getCpuCores() / 16.0)))
                .build());
        }

        // Memory-optimized for memory-intensive workloads
        if (requirements.getMemoryBytes() > 32L * 1024 * 1024 * 1024) { // 32GB
            recommendations.add(EC2InstanceRecommendation.builder()
                .instanceType("r6i.2xlarge") // 8 vCPUs, 64GB RAM
                .instanceCount(Math.ceil(requirements.getMemoryBytes() / (64.0 * 1024 * 1024 * 1024)))
                .use("Memory-intensive WebAssembly modules")
                .monthlyEstimate(calculateEC2Cost("r6i.2xlarge",
                    Math.ceil(requirements.getMemoryBytes() / (64.0 * 1024 * 1024 * 1024))))
                .build());
        }

        // General purpose for balanced workloads
        recommendations.add(EC2InstanceRecommendation.builder()
            .instanceType("m6i.2xlarge") // 8 vCPUs, 32GB RAM
            .instanceCount(Math.max(
                Math.ceil(requirements.getCpuCores() / 8.0),
                Math.ceil(requirements.getMemoryBytes() / (32.0 * 1024 * 1024 * 1024))
            ))
            .use("Balanced WebAssembly workloads")
            .monthlyEstimate(calculateEC2Cost("m6i.2xlarge",
                Math.max(
                    Math.ceil(requirements.getCpuCores() / 8.0),
                    Math.ceil(requirements.getMemoryBytes() / (32.0 * 1024 * 1024 * 1024))
                )))
            .build());

        return recommendations;
    }
}
```

## Cost Optimization

### Cost Analysis and Optimization

```java
@Component
public class CostOptimizationService {

    public CostOptimizationReport analyzeCosts(InfrastructureConfiguration currentConfig,
                                             ResourceUtilization utilization,
                                             Duration analysisperiod) {

        // Current cost analysis
        CostBreakdown currentCosts = calculateCurrentCosts(currentConfig, analysisperiod);

        // Utilization analysis
        UtilizationAnalysis utilizationAnalysis = analyzeUtilization(utilization);

        // Optimization opportunities
        List<OptimizationOpportunity> opportunities = identifyOptimizations(
            currentConfig, utilizationAnalysis);

        // Projected savings
        CostSavings projectedSavings = calculateProjectedSavings(opportunities);

        return CostOptimizationReport.builder()
            .currentCosts(currentCosts)
            .utilizationAnalysis(utilizationAnalysis)
            .optimizationOpportunities(opportunities)
            .projectedSavings(projectedSavings)
            .implementationPlan(createImplementationPlan(opportunities))
            .build();
    }

    private List<OptimizationOpportunity> identifyOptimizations(
            InfrastructureConfiguration config,
            UtilizationAnalysis utilization) {

        List<OptimizationOpportunity> opportunities = new ArrayList<>();

        // Right-sizing opportunities
        opportunities.addAll(identifyRightSizingOpportunities(config, utilization));

        // Reserved instance opportunities
        opportunities.addAll(identifyReservedInstanceOpportunities(config));

        // Spot instance opportunities
        opportunities.addAll(identifySpotInstanceOpportunities(config));

        // Storage optimization opportunities
        opportunities.addAll(identifyStorageOptimizations(config, utilization));

        // Auto-scaling optimization
        opportunities.addAll(identifyAutoScalingOptimizations(config, utilization));

        // Workload scheduling optimization
        opportunities.addAll(identifySchedulingOptimizations(config, utilization));

        return opportunities.stream()
            .sorted((a, b) -> Double.compare(b.getPotentialSavings(), a.getPotentialSavings()))
            .collect(Collectors.toList());
    }

    private List<OptimizationOpportunity> identifyRightSizingOpportunities(
            InfrastructureConfiguration config,
            UtilizationAnalysis utilization) {

        List<OptimizationOpportunity> opportunities = new ArrayList<>();

        for (InstanceConfiguration instance : config.getInstances()) {
            InstanceUtilization instanceUtil = utilization.getInstanceUtilization(instance.getId());

            // Over-provisioned CPU
            if (instanceUtil.getAverageCpuUtilization() < 0.3) { // Less than 30%
                OptimizationOpportunity opportunity = OptimizationOpportunity.builder()
                    .type(OptimizationType.DOWNSIZE_INSTANCE)
                    .description("Downsize over-provisioned instance: " + instance.getId())
                    .currentCost(instance.getMonthlyCost())
                    .potentialSavings(calculateDownsizingSavings(instance, instanceUtil))
                    .confidence(0.8)
                    .implementation(createDownsizingPlan(instance, instanceUtil))
                    .risks(Arrays.asList("Potential performance impact during peak loads"))
                    .build();

                opportunities.add(opportunity);
            }

            // Over-provisioned memory
            if (instanceUtil.getAverageMemoryUtilization() < 0.4) { // Less than 40%
                OptimizationOpportunity opportunity = OptimizationOpportunity.builder()
                    .type(OptimizationType.MEMORY_OPTIMIZE)
                    .description("Switch to compute-optimized instance: " + instance.getId())
                    .currentCost(instance.getMonthlyCost())
                    .potentialSavings(calculateMemoryOptimizationSavings(instance, instanceUtil))
                    .confidence(0.7)
                    .implementation(createMemoryOptimizationPlan(instance))
                    .risks(Arrays.asList("Memory may be needed for specific WebAssembly modules"))
                    .build();

                opportunities.add(opportunity);
            }
        }

        return opportunities;
    }

    private List<OptimizationOpportunity> identifySpotInstanceOpportunities(
            InfrastructureConfiguration config) {

        List<OptimizationOpportunity> opportunities = new ArrayList<>();

        for (InstanceConfiguration instance : config.getInstances()) {
            if (instance.getWorkloadType() == WorkloadType.BATCH_PROCESSING ||
                instance.getWorkloadType() == WorkloadType.DEVELOPMENT) {

                OptimizationOpportunity opportunity = OptimizationOpportunity.builder()
                    .type(OptimizationType.SPOT_INSTANCES)
                    .description("Convert to spot instances: " + instance.getId())
                    .currentCost(instance.getMonthlyCost())
                    .potentialSavings(instance.getMonthlyCost() * 0.7) // 70% savings typical
                    .confidence(0.6) // Lower confidence due to interruption risk
                    .implementation(createSpotInstancePlan(instance))
                    .risks(Arrays.asList(
                        "Instance interruption risk",
                        "Need fault-tolerant application design",
                        "Potential for availability issues"
                    ))
                    .build();

                opportunities.add(opportunity);
            }
        }

        return opportunities;
    }

    private List<OptimizationOpportunity> identifySchedulingOptimizations(
            InfrastructureConfiguration config,
            UtilizationAnalysis utilization) {

        List<OptimizationOpportunity> opportunities = new ArrayList<>();

        // Identify workloads that can be time-shifted
        Map<String, UsagePattern> usagePatterns = utilization.getUsagePatterns();

        for (Map.Entry<String, UsagePattern> entry : usagePatterns.entrySet()) {
            String workloadId = entry.getKey();
            UsagePattern pattern = entry.getValue();

            if (pattern.hasLowUsagePeriods()) {
                OptimizationOpportunity opportunity = OptimizationOpportunity.builder()
                    .type(OptimizationType.WORKLOAD_SCHEDULING)
                    .description("Schedule non-critical workloads during low-usage periods: " + workloadId)
                    .currentCost(calculateWorkloadCost(workloadId, config))
                    .potentialSavings(calculateSchedulingOptimizationSavings(pattern))
                    .confidence(0.9)
                    .implementation(createSchedulingOptimizationPlan(workloadId, pattern))
                    .risks(Arrays.asList("May impact user experience for time-sensitive operations"))
                    .build();

                opportunities.add(opportunity);
            }
        }

        return opportunities;
    }

    public CostOptimizationStrategy developOptimizationStrategy(
            CostOptimizationReport report,
            BusinessConstraints constraints) {

        List<OptimizationOpportunity> prioritizedOpportunities = prioritizeOpportunities(
            report.getOptimizationOpportunities(), constraints);

        ImplementationPhases phases = planImplementationPhases(prioritizedOpportunities);

        RiskMitigation riskMitigation = planRiskMitigation(prioritizedOpportunities);

        MonitoringStrategy monitoring = planCostMonitoring(prioritizedOpportunities);

        return CostOptimizationStrategy.builder()
            .prioritizedOpportunities(prioritizedOpportunities)
            .implementationPhases(phases)
            .riskMitigation(riskMitigation)
            .monitoring(monitoring)
            .expectedROI(calculateExpectedROI(prioritizedOpportunities))
            .timeline(calculateImplementationTimeline(phases))
            .build();
    }
}
```

## Monitoring and Adjustment

### Continuous Capacity Monitoring

```java
@Component
public class ContinuousCapacityMonitor {

    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorCurrentCapacity() {
        try {
            CapacitySnapshot snapshot = captureCapacitySnapshot();
            analyzeCapacityTrends(snapshot);
            detectCapacityAnomalies(snapshot);
            updateCapacityForecasts(snapshot);

        } catch (Exception e) {
            log.error("Capacity monitoring failed", e);
        }
    }

    private CapacitySnapshot captureCapacitySnapshot() {
        return CapacitySnapshot.builder()
            .timestamp(Instant.now())
            .systemMetrics(captureSystemMetrics())
            .applicationMetrics(captureApplicationMetrics())
            .wasmMetrics(captureWasmMetrics())
            .resourceUtilization(captureResourceUtilization())
            .performanceMetrics(capturePerformanceMetrics())
            .build();
    }

    private void analyzeCapacityTrends(CapacitySnapshot snapshot) {
        // Store snapshot in time series database
        timeSeriesStorage.store(snapshot);

        // Calculate trend indicators
        List<CapacitySnapshot> recentSnapshots = timeSeriesStorage.getRecent(Duration.ofHours(6));
        TrendAnalysis trends = TrendAnalyzer.analyze(recentSnapshots);

        // Update trend metrics
        updateTrendMetrics(trends);

        // Check for concerning trends
        if (trends.hasNegativeTrends()) {
            alertManager.sendCapacityTrendAlert(trends);
        }
    }

    private void detectCapacityAnomalies(CapacitySnapshot snapshot) {
        // Compare against baseline
        CapacityBaseline baseline = baselineService.getCurrentBaseline();
        AnomalyDetectionResult anomalies = anomalyDetector.detect(snapshot, baseline);

        if (anomalies.hasAnomalies()) {
            for (CapacityAnomaly anomaly : anomalies.getAnomalies()) {
                handleCapacityAnomaly(anomaly);
            }
        }
    }

    private void handleCapacityAnomaly(CapacityAnomaly anomaly) {
        switch (anomaly.getSeverity()) {
            case CRITICAL:
                // Immediate action required
                emergencyScalingService.triggerEmergencyScaling(anomaly);
                alertManager.sendCriticalCapacityAlert(anomaly);
                break;

            case HIGH:
                // Proactive scaling
                proactiveScalingService.considerScaling(anomaly);
                alertManager.sendHighPriorityCapacityAlert(anomaly);
                break;

            case MEDIUM:
                // Monitor closely
                enhancedMonitoring.enable(anomaly.getAffectedResources());
                alertManager.sendMediumPriorityCapacityAlert(anomaly);
                break;

            case LOW:
                // Log for analysis
                log.info("Low severity capacity anomaly detected: {}", anomaly.getDescription());
                break;
        }
    }

    @EventListener
    public void handleCapacityThresholdBreach(CapacityThresholdBreachEvent event) {
        CapacityThreshold threshold = event.getThreshold();
        double currentValue = event.getCurrentValue();

        log.warn("Capacity threshold breached: {} = {}, threshold = {}",
                threshold.getMetricName(), currentValue, threshold.getValue());

        // Execute threshold-specific actions
        for (ThresholdAction action : threshold.getActions()) {
            try {
                action.execute(event);
            } catch (Exception e) {
                log.error("Failed to execute threshold action: {}", action.getName(), e);
            }
        }
    }

    @Component
    public static class CapacityThresholdManager {

        private final Map<String, CapacityThreshold> thresholds = new ConcurrentHashMap<>();

        @PostConstruct
        public void initializeThresholds() {
            // CPU utilization thresholds
            addThreshold(CapacityThreshold.builder()
                .name("cpu_utilization_warning")
                .metricName("cpu_utilization_percent")
                .operator(ThresholdOperator.GREATER_THAN)
                .value(75.0)
                .duration(Duration.ofMinutes(5))
                .actions(Arrays.asList(
                    new LogThresholdAction("CPU utilization warning"),
                    new MetricIncrementerAction("capacity.warnings.cpu"),
                    new ScalingConsiderationAction("cpu_pressure")
                ))
                .build());

            addThreshold(CapacityThreshold.builder()
                .name("cpu_utilization_critical")
                .metricName("cpu_utilization_percent")
                .operator(ThresholdOperator.GREATER_THAN)
                .value(90.0)
                .duration(Duration.ofMinutes(2))
                .actions(Arrays.asList(
                    new AlertThresholdAction(AlertSeverity.CRITICAL, "Critical CPU utilization"),
                    new EmergencyScalingAction("cpu_critical"),
                    new IncidentCreationAction("CPU capacity exhaustion")
                ))
                .build());

            // Memory utilization thresholds
            addThreshold(CapacityThreshold.builder()
                .name("memory_utilization_warning")
                .metricName("memory_utilization_percent")
                .operator(ThresholdOperator.GREATER_THAN)
                .value(80.0)
                .duration(Duration.ofMinutes(10))
                .actions(Arrays.asList(
                    new LogThresholdAction("Memory utilization warning"),
                    new ScalingConsiderationAction("memory_pressure")
                ))
                .build());

            // WebAssembly specific thresholds
            addThreshold(CapacityThreshold.builder()
                .name("wasm_execution_queue_length")
                .metricName("wasm_execution_queue_length")
                .operator(ThresholdOperator.GREATER_THAN)
                .value(50.0)
                .duration(Duration.ofMinutes(3))
                .actions(Arrays.asList(
                    new AlertThresholdAction(AlertSeverity.WARNING, "WebAssembly execution queue backing up"),
                    new ScalingConsiderationAction("execution_backlog")
                ))
                .build());

            addThreshold(CapacityThreshold.builder()
                .name("wasm_error_rate")
                .metricName("wasm_error_rate_percent")
                .operator(ThresholdOperator.GREATER_THAN)
                .value(5.0)
                .duration(Duration.ofMinutes(5))
                .actions(Arrays.asList(
                    new AlertThresholdAction(AlertSeverity.HIGH, "High WebAssembly error rate"),
                    new DiagnosticCollectionAction("wasm_errors"),
                    new CapacityReviewAction("error_related_capacity_issues")
                ))
                .build());
        }

        public void addThreshold(CapacityThreshold threshold) {
            thresholds.put(threshold.getName(), threshold);
        }

        public void removeThreshold(String name) {
            thresholds.remove(name);
        }

        public List<CapacityThreshold> getAllThresholds() {
            return new ArrayList<>(thresholds.values());
        }
    }
}
```

This comprehensive capacity planning guide provides the framework and tools necessary for effectively planning, implementing, and managing the capacity requirements of Wasmtime4j applications in production environments.