package com.example.wasmintegration.controller;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.example.wasmintegration.service.WebAssemblyService;
import com.example.wasmintegration.service.WebAssemblyService.WasmExecutionResult;
import com.example.wasmintegration.service.WebAssemblyService.WebAssemblyMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller providing WebAssembly execution endpoints.
 * Demonstrates various integration patterns for web applications.
 */
@RestController
@RequestMapping("/api/wasm")
public class WebAssemblyController {
    
    @Autowired
    private WebAssemblyService webAssemblyService;
    
    /**
     * Execute a simple math function.
     * GET /api/wasm/math/add?a=5&b=3
     */
    @GetMapping("/math/add")
    public ResponseEntity<?> addNumbers(@RequestParam int a, @RequestParam int b) {
        try {
            WasmExecutionResult result = webAssemblyService.executeFromResource(
                "wasm/math.wasm", "add", a, b);
                
            return ResponseEntity.ok(Map.of(
                "result", result.getIntResult(0),
                "executionTimeMs", result.getExecutionTimeMillis(),
                "inputs", Map.of("a", a, "b", b)
            ));
            
        } catch (WasmException | IOException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "WebAssembly execution failed: " + e.getMessage()));
        }
    }
    
    /**
     * Execute a text processing function.
     * POST /api/wasm/text/process
     * Body: { "text": "Hello, World!", "operation": "uppercase" }
     */
    @PostMapping("/text/process")
    public ResponseEntity<?> processText(@RequestBody Map<String, String> request) {
        String inputText = request.get("text");
        String operation = request.getOrDefault("operation", "uppercase");
        
        if (inputText == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing 'text' field"));
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            String result = webAssemblyService.processText(
                "wasm/text-processor.wasm", 
                operation, 
                inputText);
                
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ResponseEntity.ok(Map.of(
                "input", inputText,
                "output", result,
                "operation", operation,
                "executionTimeMs", executionTime
            ));
            
        } catch (WasmException | IOException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Text processing failed: " + e.getMessage()));
        }
    }
    
    /**
     * Execute WebAssembly asynchronously.
     * POST /api/wasm/async/calculate
     */
    @PostMapping("/async/calculate")
    public CompletableFuture<ResponseEntity<?>> calculateAsync(
            @RequestBody Map<String, Object> request) {
        
        String operation = (String) request.get("operation");
        Integer[] numbers = ((java.util.List<Integer>) request.get("numbers"))
            .toArray(new Integer[0]);
        
        return webAssemblyService.executeAsync("wasm/calculator.wasm", operation, (Object[]) numbers)
            .thenApply(result -> ResponseEntity.ok(Map.of(
                "result", result.getIntResult(0),
                "executionTimeMs", result.getExecutionTimeMillis(),
                "operation", operation,
                "inputCount", numbers.length
            )))
            .exceptionally(throwable -> ResponseEntity.internalServerError()
                .body(Map.of("error", "Async execution failed: " + throwable.getMessage())));
    }
    
    /**
     * Process uploaded binary data with WebAssembly.
     * POST /api/wasm/data/process
     */
    @PostMapping("/data/process")
    public ResponseEntity<?> processData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "compress") String operation) {
        
        try {
            byte[] inputData = file.getBytes();
            long startTime = System.currentTimeMillis();
            
            byte[] processedData = webAssemblyService.processData(
                "wasm/data-processor.wasm", 
                operation, 
                inputData);
                
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/octet-stream")
                .header("X-Original-Size", String.valueOf(inputData.length))
                .header("X-Processed-Size", String.valueOf(processedData.length))
                .header("X-Execution-Time-Ms", String.valueOf(executionTime))
                .header("X-Operation", operation)
                .body(processedData);
                
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Data processing failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get WebAssembly runtime metrics for monitoring.
     * GET /api/wasm/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<WebAssemblyMetrics> getMetrics() {
        WebAssemblyMetrics metrics = webAssemblyService.getMetrics();
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Health check endpoint for WebAssembly runtime.
     * GET /api/wasm/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            WebAssemblyMetrics metrics = webAssemblyService.getMetrics();
            
            if (metrics.isRuntimeValid()) {
                return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "runtime", metrics.getRuntimeType(),
                    "version", metrics.getRuntimeVersion(),
                    "cachedModules", metrics.getCachedModules()
                ));
            } else {
                return ResponseEntity.status(503)
                    .body(Map.of("status", "DOWN", "reason", "Runtime invalid"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(503)
                .body(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }
    
    /**
     * Benchmark endpoint for performance testing.
     * POST /api/wasm/benchmark
     */
    @PostMapping("/benchmark")
    public ResponseEntity<?> benchmark(@RequestBody Map<String, Object> request) {
        String module = (String) request.getOrDefault("module", "wasm/benchmark.wasm");
        String function = (String) request.getOrDefault("function", "fibonacci");
        Integer iterations = (Integer) request.getOrDefault("iterations", 1000);
        Integer input = (Integer) request.getOrDefault("input", 20);
        
        try {
            long totalTime = 0;
            long minTime = Long.MAX_VALUE;
            long maxTime = Long.MIN_VALUE;
            
            // Warm up
            for (int i = 0; i < 100; i++) {
                webAssemblyService.executeFromResource(module, function, input);
            }
            
            // Benchmark
            for (int i = 0; i < iterations; i++) {
                WasmExecutionResult result = webAssemblyService.executeFromResource(
                    module, function, input);
                    
                long executionTime = result.getExecutionTimeNanos();
                totalTime += executionTime;
                minTime = Math.min(minTime, executionTime);
                maxTime = Math.max(maxTime, executionTime);
            }
            
            double averageTimeMs = (totalTime / (double) iterations) / 1_000_000.0;
            double minTimeMs = minTime / 1_000_000.0;
            double maxTimeMs = maxTime / 1_000_000.0;
            double throughput = 1000.0 / averageTimeMs; // ops/second
            
            return ResponseEntity.ok(Map.of(
                "module", module,
                "function", function,
                "iterations", iterations,
                "input", input,
                "averageTimeMs", averageTimeMs,
                "minTimeMs", minTimeMs,
                "maxTimeMs", maxTimeMs,
                "throughputOpsPerSecond", throughput
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Benchmark failed: " + e.getMessage()));
        }
    }
}