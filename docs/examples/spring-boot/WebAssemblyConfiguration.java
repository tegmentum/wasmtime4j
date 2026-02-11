package com.example.wasmintegration.config;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;

/**
 * Spring Boot configuration for WebAssembly integration using Wasmtime4j.
 * 
 * This configuration provides:
 * - Singleton WasmRuntime bean
 * - Optimized Engine configuration
 * - Module caching service
 * - Automatic resource cleanup
 */
@Configuration
@ConfigurationProperties(prefix = "webassembly")
public class WebAssemblyConfiguration {

    @Value("${webassembly.optimization-level:SPEED}")
    private OptimizationLevel optimizationLevel;
    
    @Value("${webassembly.enable-profiling:false}")
    private boolean enableProfiling;
    
    @Value("${webassembly.enable-debug:false}")
    private boolean enableDebugInfo;
    
    @Value("${webassembly.runtime-type:AUTO}")
    private String runtimeType;
    
    private WasmRuntime wasmRuntime;
    
    /**
     * Creates a configured WasmRuntime bean.
     * This runtime will be automatically closed when the Spring context shuts down.
     */
    @Bean
    public WasmRuntime wasmRuntime() throws WasmException {
        if ("AUTO".equals(runtimeType)) {
            wasmRuntime = WasmRuntimeFactory.create();
        } else {
            RuntimeType type = RuntimeType.valueOf(runtimeType.toUpperCase());
            wasmRuntime = WasmRuntimeFactory.create(type);
        }
        
        return wasmRuntime;
    }
    
    /**
     * Creates an optimized Engine for WebAssembly compilation and execution.
     */
    @Bean
    public Engine wasmEngine(WasmRuntime runtime) throws WasmException {
        EngineConfig config = EngineConfig.builder()
            .optimizationLevel(optimizationLevel)
            .enableProfiling(enableProfiling)
            .enableDebugInfo(enableDebugInfo)
            .build();
            
        return runtime.createEngine(config);
    }
    
    /**
     * Module cache service for improved performance.
     */
    @Bean
    public WebAssemblyModuleCache moduleCache(Engine engine) {
        return new WebAssemblyModuleCache(engine);
    }
    
    /**
     * Service for managing WebAssembly modules and instances.
     */
    @Bean
    public WebAssemblyService webAssemblyService(
            WasmRuntime runtime, 
            WebAssemblyModuleCache moduleCache) {
        return new WebAssemblyService(runtime, moduleCache);
    }
    
    @PreDestroy
    public void cleanup() {
        if (wasmRuntime != null) {
            wasmRuntime.close();
        }
    }
    
    /**
     * Caching service for compiled WebAssembly modules.
     */
    public static class WebAssemblyModuleCache {
        private final Engine engine;
        private final Map<String, Module> moduleCache = new ConcurrentHashMap<>();
        
        public WebAssemblyModuleCache(Engine engine) {
            this.engine = engine;
        }
        
        public Module getOrCompileModule(String moduleId, byte[] wasmBytes) throws WasmException {
            return moduleCache.computeIfAbsent(moduleId, id -> {
                try {
                    return engine.compileModule(wasmBytes);
                } catch (WasmException e) {
                    throw new RuntimeException("Failed to compile module: " + moduleId, e);
                }
            });
        }
        
        public Module getOrCompileModule(String moduleId, Resource wasmResource) 
                throws WasmException, IOException {
            return getOrCompileModule(moduleId, wasmResource.getInputStream().readAllBytes());
        }
        
        public void evictModule(String moduleId) {
            moduleCache.remove(moduleId);
        }
        
        public void clearCache() {
            moduleCache.clear();
        }
        
        public int getCacheSize() {
            return moduleCache.size();
        }
    }
}