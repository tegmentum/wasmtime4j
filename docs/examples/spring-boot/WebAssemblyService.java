package com.example.wasmintegration.service;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.example.wasmintegration.config.WebAssemblyConfiguration.WebAssemblyModuleCache;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spring service for executing WebAssembly modules.
 * Provides high-level API for WebAssembly operations with Spring integration.
 */
@Service
public class WebAssemblyService {
    
    private final WasmRuntime runtime;
    private final WebAssemblyModuleCache moduleCache;
    private final ResourceLoader resourceLoader;
    private final ExecutorService executorService;
    
    public WebAssemblyService(WasmRuntime runtime, 
                            WebAssemblyModuleCache moduleCache,
                            ResourceLoader resourceLoader) {
        this.runtime = runtime;
        this.moduleCache = moduleCache;
        this.resourceLoader = resourceLoader;
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Load and execute a WebAssembly module from classpath resources.
     */
    public WasmExecutionResult executeFromResource(String resourcePath, 
                                                 String functionName, 
                                                 Object... args) throws WasmException, IOException {
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
        Module module = moduleCache.getOrCompileModule(resourcePath, resource);
        
        return executeFunction(module, functionName, args);
    }
    
    /**
     * Execute a function in a WebAssembly module with typed arguments.
     */
    public WasmExecutionResult executeFunction(Module module, 
                                             String functionName, 
                                             Object... args) throws WasmException {
        
        Instance instance = runtime.instantiate(module);
        WasmFunction function = instance.getFunction(functionName);
        
        // Convert Java arguments to WebAssembly values
        WasmValue[] wasmArgs = convertToWasmValues(args);
        
        // Execute function and measure performance
        long startTime = System.nanoTime();
        WasmValue[] results = function.call(wasmArgs);
        long executionTime = System.nanoTime() - startTime;
        
        return new WasmExecutionResult(results, executionTime);
    }
    
    /**
     * Execute a WebAssembly function asynchronously.
     */
    public CompletableFuture<WasmExecutionResult> executeAsync(String resourcePath,
                                                              String functionName,
                                                              Object... args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeFromResource(resourcePath, functionName, args);
            } catch (Exception e) {
                throw new RuntimeException("Async WebAssembly execution failed", e);
            }
        }, executorService);
    }
    
    /**
     * Execute a text processing WebAssembly module.
     * Handles string marshalling automatically.
     */
    public String processText(String resourcePath, 
                            String functionName, 
                            String inputText) throws WasmException, IOException {
        
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
        Module module = moduleCache.getOrCompileModule(resourcePath, resource);
        Instance instance = runtime.instantiate(module);
        
        // Get memory for string operations
        WasmMemory memory = instance.getMemory("memory");
        
        // Write input string to WebAssembly memory
        byte[] inputBytes = inputText.getBytes(StandardCharsets.UTF_8);
        int inputOffset = 1024; // Reserve first 1KB for other uses
        memory.write(inputOffset, inputBytes);
        
        // Call the processing function
        WasmFunction function = instance.getFunction(functionName);
        WasmValue[] args = {
            WasmValue.i32(inputOffset),
            WasmValue.i32(inputBytes.length),
            WasmValue.i32(inputOffset + inputBytes.length + 1024) // Output offset
        };
        
        WasmValue[] results = function.call(args);
        int outputLength = results[0].asI32();
        
        // Read processed string from memory
        byte[] outputBytes = memory.read(inputOffset + inputBytes.length + 1024, outputLength);
        return new String(outputBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Execute a data processing pipeline using WebAssembly.
     */
    public byte[] processData(String resourcePath,
                            String functionName,
                            byte[] inputData) throws WasmException, IOException {
        
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
        Module module = moduleCache.getOrCompileModule(resourcePath, resource);
        Instance instance = runtime.instantiate(module);
        
        WasmMemory memory = instance.getMemory("memory");
        
        // Write input data to memory
        int inputOffset = 0;
        memory.write(inputOffset, inputData);
        
        // Execute processing function
        WasmFunction function = instance.getFunction(functionName);
        WasmValue[] args = {
            WasmValue.i32(inputOffset),
            WasmValue.i32(inputData.length),
            WasmValue.i32(inputData.length + 1024) // Output offset
        };
        
        WasmValue[] results = function.call(args);
        int outputLength = results[0].asI32();
        
        // Read processed data
        return memory.read(inputData.length + 1024, outputLength);
    }
    
    /**
     * Get runtime information for monitoring and diagnostics.
     */
    public WebAssemblyMetrics getMetrics() {
        RuntimeInfo runtimeInfo = runtime.getRuntimeInfo();
        int cachedModules = moduleCache.getCacheSize();
        
        return new WebAssemblyMetrics(
            runtimeInfo.getRuntimeType().toString(),
            runtimeInfo.getVersion(),
            cachedModules,
            runtime.isValid()
        );
    }
    
    private WasmValue[] convertToWasmValues(Object[] args) {
        WasmValue[] wasmValues = new WasmValue[args.length];
        
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            
            if (arg instanceof Integer) {
                wasmValues[i] = WasmValue.i32((Integer) arg);
            } else if (arg instanceof Long) {
                wasmValues[i] = WasmValue.i64((Long) arg);
            } else if (arg instanceof Float) {
                wasmValues[i] = WasmValue.f32((Float) arg);
            } else if (arg instanceof Double) {
                wasmValues[i] = WasmValue.f64((Double) arg);
            } else {
                throw new IllegalArgumentException("Unsupported argument type: " + arg.getClass());
            }
        }
        
        return wasmValues;
    }
    
    /**
     * Result object for WebAssembly function execution.
     */
    public static class WasmExecutionResult {
        private final WasmValue[] results;
        private final long executionTimeNanos;
        
        public WasmExecutionResult(WasmValue[] results, long executionTimeNanos) {
            this.results = results;
            this.executionTimeNanos = executionTimeNanos;
        }
        
        public WasmValue[] getResults() {
            return results;
        }
        
        public long getExecutionTimeNanos() {
            return executionTimeNanos;
        }
        
        public double getExecutionTimeMillis() {
            return executionTimeNanos / 1_000_000.0;
        }
        
        public int getIntResult(int index) {
            return results[index].asI32();
        }
        
        public long getLongResult(int index) {
            return results[index].asI64();
        }
        
        public float getFloatResult(int index) {
            return results[index].asF32();
        }
        
        public double getDoubleResult(int index) {
            return results[index].asF64();
        }
    }
    
    /**
     * Metrics for monitoring WebAssembly operations.
     */
    public static class WebAssemblyMetrics {
        private final String runtimeType;
        private final String runtimeVersion;
        private final int cachedModules;
        private final boolean runtimeValid;
        
        public WebAssemblyMetrics(String runtimeType, String runtimeVersion, 
                                int cachedModules, boolean runtimeValid) {
            this.runtimeType = runtimeType;
            this.runtimeVersion = runtimeVersion;
            this.cachedModules = cachedModules;
            this.runtimeValid = runtimeValid;
        }
        
        // Getters
        public String getRuntimeType() { return runtimeType; }
        public String getRuntimeVersion() { return runtimeVersion; }
        public int getCachedModules() { return cachedModules; }
        public boolean isRuntimeValid() { return runtimeValid; }
    }
}