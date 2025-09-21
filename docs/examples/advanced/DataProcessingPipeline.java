package examples.advanced;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.component.*;
import ai.tegmentum.wasmtime4j.async.*;
import ai.tegmentum.wasmtime4j.performance.PerformanceMonitor;
import ai.tegmentum.wasmtime4j.performance.ProfileReport;
import ai.tegmentum.wasmtime4j.resource.ResourcePool;
import ai.tegmentum.wasmtime4j.resource.PoolConfiguration;
import ai.tegmentum.wasmtime4j.resource.PooledResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Advanced example demonstrating a complete data processing pipeline using Wasmtime4j.
 *
 * This example shows:
 * - Component model usage for modular data processing
 * - Async and reactive data processing
 * - Stream processing with backpressure
 * - Performance monitoring and optimization
 * - Error handling and recovery strategies
 * - Resource pooling for high throughput
 * - Memory-efficient processing of large datasets
 */
public class DataProcessingPipeline {

    private static final Logger logger = Logger.getLogger(DataProcessingPipeline.class.getName());

    private final WasmRuntime runtime;
    private final ResourcePool<Engine> enginePool;
    private final AsyncExecutorManager asyncExecutor;
    private final PerformanceMonitor performanceMonitor;
    private final Map<String, PipelineStage> stages = new LinkedHashMap<>();
    private final AtomicLong processedRecords = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    public DataProcessingPipeline() throws Exception {
        // Initialize runtime
        this.runtime = WasmRuntimeFactory.create();

        // Create engine pool optimized for data processing
        PoolConfiguration poolConfig = PoolConfiguration.builder()
            .initialSize(Runtime.getRuntime().availableProcessors())
            .maxSize(Runtime.getRuntime().availableProcessors() * 2)
            .maxIdleTime(Duration.ofMinutes(5))
            .validationQuery(Engine::isValid)
            .build();

        this.enginePool = ResourcePool.<Engine>builder()
            .configuration(poolConfig)
            .factory(() -> {
                EngineConfig config = new EngineConfig()
                    .optimizationLevel(OptimizationLevel.SPEED)
                    .parallelCompilation(true)
                    .simdSupport(true)               // Enable SIMD for data processing
                    .bulkMemoryOperations(true)      // Enable bulk memory ops
                    .multiThreading(false);          // Disable for deterministic processing
                return runtime.createEngine(config);
            })
            .build();

        // Initialize async execution manager
        this.asyncExecutor = AsyncExecutorManager.create(
            Runtime.getRuntime().availableProcessors() * 2
        );

        // Initialize performance monitoring
        this.performanceMonitor = PerformanceMonitor.create();
        this.performanceMonitor.startMonitoring();

        logger.info("Data processing pipeline initialized");
        logger.info("Available processors: " + Runtime.getRuntime().availableProcessors());
        logger.info("Runtime type: " + runtime.getRuntimeInfo().getRuntimeType());
    }

    /**
     * Adds a processing stage to the pipeline.
     *
     * @param stageName unique name for the stage
     * @param componentPath path to the WebAssembly component
     * @param functionName name of the processing function in the component
     * @param parallelism level of parallelism for this stage
     */
    public void addStage(String stageName, String componentPath, String functionName, int parallelism) throws Exception {
        logger.info("Adding pipeline stage: " + stageName);

        // Load component
        byte[] componentBytes = Files.readAllBytes(Paths.get(componentPath));

        try (PooledResource<Engine> pooledEngine = enginePool.acquire()) {
            Engine engine = pooledEngine.getResource();
            Component component = Component.compile(engine, componentBytes);

            // Validate component exports
            if (!component.hasExport(functionName)) {
                throw new IllegalArgumentException("Component must export function: " + functionName);
            }

            // Create stage metadata
            PipelineStage stage = new PipelineStage(
                stageName,
                component,
                functionName,
                parallelism,
                Paths.get(componentPath)
            );

            stages.put(stageName, stage);
            logger.info("Successfully added stage: " + stageName + " with parallelism: " + parallelism);
        }
    }

    /**
     * Processes data through the entire pipeline.
     */
    public CompletableFuture<ProcessingResult> processDataAsync(DataBatch inputData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processDataInternal(inputData);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Pipeline processing failed", e);
                errorCount.incrementAndGet();
                throw new RuntimeException("Pipeline processing failed", e);
            }
        }, asyncExecutor.getExecutor());
    }

    private ProcessingResult processDataInternal(DataBatch inputData) throws Exception {
        long startTime = System.currentTimeMillis();
        DataBatch currentData = inputData;

        logger.info("Starting pipeline processing for batch of " + inputData.getRecords().size() + " records");

        // Process through each stage
        for (Map.Entry<String, PipelineStage> entry : stages.entrySet()) {
            String stageName = entry.getKey();
            PipelineStage stage = entry.getValue();

            logger.fine("Processing stage: " + stageName);

            long stageStartTime = System.currentTimeMillis();
            currentData = processStage(stage, currentData);
            long stageEndTime = System.currentTimeMillis();

            logger.fine("Stage " + stageName + " completed in " + (stageEndTime - stageStartTime) + "ms");

            // Record stage performance
            performanceMonitor.recordFunctionCall(
                stageName,
                Duration.ofMillis(stageEndTime - stageStartTime)
            );
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        processedRecords.addAndGet(inputData.getRecords().size());

        logger.info("Pipeline processing completed in " + totalTime + "ms");

        return new ProcessingResult(
            currentData,
            totalTime,
            inputData.getRecords().size(),
            currentData.getRecords().size()
        );
    }

    private DataBatch processStage(PipelineStage stage, DataBatch inputData) throws Exception {
        if (stage.getParallelism() == 1) {
            // Sequential processing
            return processStageSequential(stage, inputData);
        } else {
            // Parallel processing
            return processStageParallel(stage, inputData);
        }
    }

    private DataBatch processStageSequential(PipelineStage stage, DataBatch inputData) throws Exception {
        try (PooledResource<Engine> pooledEngine = enginePool.acquire()) {
            Engine engine = pooledEngine.getResource();
            Store store = runtime.createStore(engine);

            // Create component linker
            ComponentLinker linker = ComponentLinker.create(engine);

            // Setup host functions for data processing
            setupDataProcessingHostFunctions(linker);

            // Instantiate component
            ComponentInstance instance = linker.instantiate(store, stage.getComponent());
            ComponentFunction processFunction = instance.getFunction(stage.getFunctionName());

            List<DataRecord> processedRecords = new ArrayList<>();

            for (DataRecord record : inputData.getRecords()) {
                try {
                    // Convert record to component value
                    ComponentValue input = recordToComponentValue(record);

                    // Process the record
                    ComponentValue[] result = processFunction.call(input);

                    // Convert back to data record
                    DataRecord processedRecord = componentValueToRecord(result[0]);
                    processedRecords.add(processedRecord);

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to process record in stage " + stage.getName(), e);
                    errorCount.incrementAndGet();
                    // Continue processing other records
                }
            }

            return new DataBatch(processedRecords, inputData.getMetadata());
        }
    }

    private DataBatch processStageParallel(PipelineStage stage, DataBatch inputData) throws Exception {
        int batchSize = Math.max(1, inputData.getRecords().size() / stage.getParallelism());
        List<List<DataRecord>> batches = partitionList(inputData.getRecords(), batchSize);

        // Process batches in parallel
        List<CompletableFuture<List<DataRecord>>> futures = batches.stream()
            .map(batch -> processRecordBatchAsync(stage, batch))
            .collect(Collectors.toList());

        // Wait for all batches to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        try {
            allFutures.get(30, TimeUnit.SECONDS); // Timeout after 30 seconds
        } catch (TimeoutException e) {
            logger.severe("Parallel processing timed out for stage: " + stage.getName());
            throw new RuntimeException("Stage processing timeout", e);
        }

        // Collect results
        List<DataRecord> allProcessedRecords = new ArrayList<>();
        for (CompletableFuture<List<DataRecord>> future : futures) {
            allProcessedRecords.addAll(future.get());
        }

        return new DataBatch(allProcessedRecords, inputData.getMetadata());
    }

    private CompletableFuture<List<DataRecord>> processRecordBatchAsync(PipelineStage stage, List<DataRecord> batch) {
        return CompletableFuture.supplyAsync(() -> {
            try (PooledResource<Engine> pooledEngine = enginePool.acquire()) {
                Engine engine = pooledEngine.getResource();
                Store store = runtime.createStore(engine);

                ComponentLinker linker = ComponentLinker.create(engine);
                setupDataProcessingHostFunctions(linker);

                ComponentInstance instance = linker.instantiate(store, stage.getComponent());
                ComponentFunction processFunction = instance.getFunction(stage.getFunctionName());

                List<DataRecord> processedRecords = new ArrayList<>();

                for (DataRecord record : batch) {
                    try {
                        ComponentValue input = recordToComponentValue(record);
                        ComponentValue[] result = processFunction.call(input);
                        DataRecord processedRecord = componentValueToRecord(result[0]);
                        processedRecords.add(processedRecord);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to process record in parallel batch", e);
                        errorCount.incrementAndGet();
                    }
                }

                return processedRecords;

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Batch processing failed", e);
                throw new RuntimeException("Batch processing failed", e);
            }
        }, asyncExecutor.getExecutor());
    }

    private void setupDataProcessingHostFunctions(ComponentLinker linker) {
        // Log function for debugging
        linker.defineFunction("env", "log", (args) -> {
            String message = args[0].asString();
            logger.fine("Component log: " + message);
            return new ComponentValue[0];
        });

        // Current timestamp function
        linker.defineFunction("env", "current_timestamp", (args) -> {
            return new ComponentValue[] { ComponentValue.u64(System.currentTimeMillis()) };
        });

        // Random number generator
        linker.defineFunction("env", "random", (args) -> {
            return new ComponentValue[] { ComponentValue.f64(Math.random()) };
        });

        // Memory allocation helper (for large data processing)
        linker.defineFunction("env", "allocate_buffer", (args) -> {
            int size = (int) args[0].asU32();
            // In real implementation, this would allocate memory in the component
            return new ComponentValue[] { ComponentValue.u32(1000) }; // Return mock address
        });
    }

    private ComponentValue recordToComponentValue(DataRecord record) {
        // Convert DataRecord to ComponentValue
        // This would depend on your specific data schema
        return ComponentValue.record()
            .field("id", ComponentValue.string(record.getId()))
            .field("data", ComponentValue.string(record.getData()))
            .field("timestamp", ComponentValue.u64(record.getTimestamp()))
            .build();
    }

    private DataRecord componentValueToRecord(ComponentValue value) {
        // Convert ComponentValue back to DataRecord
        // This would depend on your specific data schema
        String id = value.getField("id").asString();
        String data = value.getField("data").asString();
        long timestamp = value.getField("timestamp").asU64();
        return new DataRecord(id, data, timestamp);
    }

    private static <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    /**
     * Processes a stream of data with backpressure control.
     */
    public void processStreamAsync(DataStream stream, DataSink sink) {
        StreamingOptions options = StreamingOptions.builder()
            .bufferSize(1000)
            .backpressureThreshold(0.8)
            .maxConcurrency(Runtime.getRuntime().availableProcessors())
            .build();

        StreamingModule streamProcessor = StreamingModule.create(options);

        // Setup stream processing pipeline
        streamProcessor
            .source(stream)
            .map(batch -> {
                try {
                    return processDataInternal(batch);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Stream processing error", e);
                    throw new RuntimeException(e);
                }
            })
            .filter(result -> result.getOutputRecordCount() > 0)
            .sink(result -> {
                try {
                    sink.write(result.getProcessedData());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to write to sink", e);
                }
            })
            .onError(error -> {
                logger.log(Level.SEVERE, "Stream processing pipeline error", error);
                errorCount.incrementAndGet();
            })
            .start();

        logger.info("Stream processing started");
    }

    /**
     * Gets pipeline performance statistics.
     */
    public PipelineStats getStats() {
        ProfileReport performanceReport = performanceMonitor.generateReport();

        return new PipelineStats(
            stages.size(),
            processedRecords.get(),
            errorCount.get(),
            enginePool.getStatistics(),
            performanceReport
        );
    }

    /**
     * Optimizes the pipeline based on performance metrics.
     */
    public void optimizePipeline() {
        ProfileReport report = performanceMonitor.generateReport();

        logger.info("Optimizing pipeline based on performance metrics");

        // Analyze stage performance and adjust parallelism
        for (Map.Entry<String, PipelineStage> entry : stages.entrySet()) {
            String stageName = entry.getKey();
            PipelineStage stage = entry.getValue();

            // Get performance metrics for this stage
            Duration avgTime = report.getFunctionProfile(stageName)
                .map(profile -> profile.getAverageTime())
                .orElse(Duration.ZERO);

            // If stage is slow, consider increasing parallelism
            if (avgTime.toMillis() > 1000 && stage.getParallelism() < 8) {
                int newParallelism = Math.min(8, stage.getParallelism() * 2);
                stage.setParallelism(newParallelism);
                logger.info("Increased parallelism for stage " + stageName + " to " + newParallelism);
            }
        }

        // Adjust engine pool size based on load
        long totalProcessed = processedRecords.get();
        if (totalProcessed > 100000) {
            // High load - consider expanding pool
            logger.info("High processing volume detected, consider expanding engine pool");
        }
    }

    /**
     * Shuts down the pipeline and releases resources.
     */
    public void shutdown() {
        try {
            performanceMonitor.stop();
            asyncExecutor.shutdown();
            enginePool.close();
            runtime.close();
            logger.info("Data processing pipeline shut down successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during pipeline shutdown", e);
        }
    }

    // Data model classes

    public static class DataRecord {
        private final String id;
        private final String data;
        private final long timestamp;

        public DataRecord(String id, String data, long timestamp) {
            this.id = id;
            this.data = data;
            this.timestamp = timestamp;
        }

        public String getId() { return id; }
        public String getData() { return data; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return "DataRecord{id='" + id + "', data='" + data + "', timestamp=" + timestamp + '}';
        }
    }

    public static class DataBatch {
        private final List<DataRecord> records;
        private final Map<String, Object> metadata;

        public DataBatch(List<DataRecord> records, Map<String, Object> metadata) {
            this.records = new ArrayList<>(records);
            this.metadata = new HashMap<>(metadata);
        }

        public List<DataRecord> getRecords() { return records; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    public interface DataStream {
        CompletableFuture<DataBatch> nextBatch();
        boolean hasMore();
        void close();
    }

    public interface DataSink {
        void write(DataBatch batch) throws IOException;
        void flush() throws IOException;
        void close() throws IOException;
    }

    public static class ProcessingResult {
        private final DataBatch processedData;
        private final long processingTimeMs;
        private final int inputRecordCount;
        private final int outputRecordCount;

        public ProcessingResult(DataBatch processedData, long processingTimeMs, int inputRecordCount, int outputRecordCount) {
            this.processedData = processedData;
            this.processingTimeMs = processingTimeMs;
            this.inputRecordCount = inputRecordCount;
            this.outputRecordCount = outputRecordCount;
        }

        public DataBatch getProcessedData() { return processedData; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public int getInputRecordCount() { return inputRecordCount; }
        public int getOutputRecordCount() { return outputRecordCount; }
    }

    private static class PipelineStage {
        private final String name;
        private final Component component;
        private final String functionName;
        private int parallelism;
        private final Path sourcePath;

        public PipelineStage(String name, Component component, String functionName, int parallelism, Path sourcePath) {
            this.name = name;
            this.component = component;
            this.functionName = functionName;
            this.parallelism = parallelism;
            this.sourcePath = sourcePath;
        }

        public String getName() { return name; }
        public Component getComponent() { return component; }
        public String getFunctionName() { return functionName; }
        public int getParallelism() { return parallelism; }
        public void setParallelism(int parallelism) { this.parallelism = parallelism; }
        public Path getSourcePath() { return sourcePath; }
    }

    public static class PipelineStats {
        private final int stageCount;
        private final long processedRecords;
        private final long errorCount;
        private final Object poolStats;
        private final ProfileReport performanceReport;

        public PipelineStats(int stageCount, long processedRecords, long errorCount, Object poolStats, ProfileReport performanceReport) {
            this.stageCount = stageCount;
            this.processedRecords = processedRecords;
            this.errorCount = errorCount;
            this.poolStats = poolStats;
            this.performanceReport = performanceReport;
        }

        public int getStageCount() { return stageCount; }
        public long getProcessedRecords() { return processedRecords; }
        public long getErrorCount() { return errorCount; }
        public Object getPoolStats() { return poolStats; }
        public ProfileReport getPerformanceReport() { return performanceReport; }
    }

    // Example usage and testing
    public static void main(String[] args) throws Exception {
        DataProcessingPipeline pipeline = new DataProcessingPipeline();

        try {
            // Setup pipeline stages
            pipeline.addStage("filter", "components/data-filter.wasm", "filter_records", 2);
            pipeline.addStage("transform", "components/data-transform.wasm", "transform_data", 4);
            pipeline.addStage("aggregate", "components/data-aggregate.wasm", "aggregate_results", 1);

            // Create sample data
            List<DataRecord> sampleRecords = Arrays.asList(
                new DataRecord("1", "sample data 1", System.currentTimeMillis()),
                new DataRecord("2", "sample data 2", System.currentTimeMillis()),
                new DataRecord("3", "sample data 3", System.currentTimeMillis())
            );

            DataBatch inputBatch = new DataBatch(sampleRecords, Map.of("source", "test"));

            // Process data asynchronously
            CompletableFuture<ProcessingResult> resultFuture = pipeline.processDataAsync(inputBatch);

            // Wait for result
            ProcessingResult result = resultFuture.get(30, TimeUnit.SECONDS);

            System.out.println("Processing completed:");
            System.out.println("  Input records: " + result.getInputRecordCount());
            System.out.println("  Output records: " + result.getOutputRecordCount());
            System.out.println("  Processing time: " + result.getProcessingTimeMs() + "ms");

            // Optimize pipeline based on performance
            pipeline.optimizePipeline();

            // Get and display statistics
            PipelineStats stats = pipeline.getStats();
            System.out.println("Pipeline statistics:");
            System.out.println("  Stages: " + stats.getStageCount());
            System.out.println("  Total processed: " + stats.getProcessedRecords());
            System.out.println("  Errors: " + stats.getErrorCount());

        } finally {
            pipeline.shutdown();
        }
    }
}