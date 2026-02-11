package ai.tegmentum.wasmtime4j.examples.advanced;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;

/**
 * Advanced example demonstrating a plugin system using WebAssembly.
 * 
 * This example shows:
 * - Safe execution of untrusted plugin code
 * - Plugin lifecycle management
 * - Host function integration
 * - Resource limits and security
 * - Concurrent plugin execution
 * - Plugin API versioning
 */
public class PluginSystemExample {
    
    private final WasmRuntime runtime;
    private final Engine engine;
    private final ExecutorService pluginExecutor;
    private final Map<String, Plugin> loadedPlugins;
    
    public PluginSystemExample() throws WasmException {
        this.runtime = WasmRuntimeFactory.create();
        
        // Configure engine for plugin execution
        EngineConfig config = EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.SPEED)
            .build();
        this.engine = runtime.createEngine(config);
        
        this.pluginExecutor = Executors.newCachedThreadPool();
        this.loadedPlugins = new ConcurrentHashMap<>();
    }
    
    public static void main(String[] args) throws Exception {
        PluginSystemExample pluginSystem = new PluginSystemExample();
        
        try {
            // Load and register plugins
            pluginSystem.loadPlugin("text-processor", "plugins/text-processor.wasm");
            pluginSystem.loadPlugin("data-validator", "plugins/data-validator.wasm");
            pluginSystem.loadPlugin("crypto-util", "plugins/crypto-util.wasm");
            
            // Execute plugin operations
            pluginSystem.demonstratePluginUsage();
            
        } finally {
            pluginSystem.shutdown();
        }
    }
    
    /**
     * Plugin descriptor containing metadata and compiled module.
     */
    public static class Plugin {
        private final String id;
        private final String version;
        private final Module module;
        private final PluginManifest manifest;
        private volatile boolean enabled = true;
        
        public Plugin(String id, String version, Module module, PluginManifest manifest) {
            this.id = id;
            this.version = version;
            this.module = module;
            this.manifest = manifest;
        }
        
        // Getters
        public String getId() { return id; }
        public String getVersion() { return version; }
        public Module getModule() { return module; }
        public PluginManifest getManifest() { return manifest; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    /**
     * Plugin manifest describing capabilities and requirements.
     */
    public static class PluginManifest {
        private final String apiVersion;
        private final String[] requiredCapabilities;
        private final String[] exports;
        private final long maxMemory;
        private final long maxExecutionTime;
        
        public PluginManifest(String apiVersion, String[] requiredCapabilities, 
                             String[] exports, long maxMemory, long maxExecutionTime) {
            this.apiVersion = apiVersion;
            this.requiredCapabilities = requiredCapabilities;
            this.exports = exports;
            this.maxMemory = maxMemory;
            this.maxExecutionTime = maxExecutionTime;
        }
        
        // Getters
        public String getApiVersion() { return apiVersion; }
        public String[] getRequiredCapabilities() { return requiredCapabilities; }
        public String[] getExports() { return exports; }
        public long getMaxMemory() { return maxMemory; }
        public long getMaxExecutionTime() { return maxExecutionTime; }
    }
    
    /**
     * Load and validate a plugin from a WebAssembly file.
     */
    public void loadPlugin(String pluginId, String wasmPath) throws PluginException {
        try {
            // Load WebAssembly module
            byte[] wasmBytes = Files.readAllBytes(Paths.get(wasmPath));
            Module module = runtime.compileModule(engine, wasmBytes);
            
            // Create and validate plugin manifest
            PluginManifest manifest = extractManifest(module);
            validatePlugin(pluginId, manifest);
            
            // Create plugin descriptor
            Plugin plugin = new Plugin(pluginId, "1.0.0", module, manifest);
            
            // Register plugin
            loadedPlugins.put(pluginId, plugin);
            
            System.out.printf("Loaded plugin: %s (API: %s)%n", 
                pluginId, manifest.getApiVersion());
            
        } catch (IOException | WasmException e) {
            throw new PluginException("Failed to load plugin: " + pluginId, e);
        }
    }
    
    /**
     * Execute a plugin function with security controls.
     */
    public CompletableFuture<PluginResult> executePlugin(String pluginId, 
                                                         String functionName, 
                                                         PluginContext context) {
        return CompletableFuture.supplyAsync(() -> {
            Plugin plugin = loadedPlugins.get(pluginId);
            if (plugin == null) {
                throw new PluginException("Plugin not found: " + pluginId);
            }
            
            if (!plugin.isEnabled()) {
                throw new PluginException("Plugin is disabled: " + pluginId);
            }
            
            try {
                return executePluginSafely(plugin, functionName, context);
            } catch (Exception e) {
                throw new PluginException("Plugin execution failed: " + pluginId, e);
            }
            
        }, pluginExecutor);
    }
    
    private PluginResult executePluginSafely(Plugin plugin, String functionName, 
                                           PluginContext context) throws WasmException {
        
        // Create secure instance with host functions
        ImportMap imports = createPluginImports(plugin, context);
        Instance instance = runtime.instantiate(plugin.getModule(), imports);
        
        try {
            // Get plugin function
            WasmFunction function = instance.getFunction(functionName);
            if (function == null) {
                throw new PluginException("Function not found: " + functionName);
            }
            
            // Prepare input data
            WasmMemory memory = instance.getMemory("memory");
            int inputOffset = writeContextToMemory(memory, context);
            
            // Execute with timeout protection
            long startTime = System.nanoTime();
            WasmValue[] args = {WasmValue.i32(inputOffset), WasmValue.i32(context.getDataSize())};
            WasmValue[] results = executeWithTimeout(function, args, plugin.getManifest().getMaxExecutionTime());
            long executionTime = System.nanoTime() - startTime;
            
            // Read result data
            int resultOffset = results[0].asI32();
            int resultLength = results[1].asI32();
            byte[] resultData = memory.read(resultOffset, resultLength);
            
            return new PluginResult(
                plugin.getId(),
                functionName,
                resultData,
                executionTime / 1_000_000L, // Convert to milliseconds
                true
            );
            
        } finally {
            // Cleanup instance resources
            instance.close();
        }
    }
    
    /**
     * Create host functions available to plugins.
     */
    private ImportMap createPluginImports(Plugin plugin, PluginContext context) {
        ImportMap imports = new ImportMap();
        
        // Logging host function
        imports.addFunction("env", "plugin_log", createLogFunction(plugin.getId()));
        
        // Data validation host function  
        imports.addFunction("env", "validate_data", createValidationFunction());
        
        // Time access host function
        imports.addFunction("env", "get_time", createTimeFunction());
        
        // Plugin metadata access
        imports.addFunction("env", "get_plugin_info", createPluginInfoFunction(plugin));
        
        return imports;
    }
    
    private WasmFunction createLogFunction(String pluginId) {
        return WasmFunction.hostFunction(
            "plugin_log",
            FunctionType.of(new WasmValueType[]{WasmValueType.I32, WasmValueType.I32}, 
                           new WasmValueType[]{}),
            (args) -> {
                int messagePtr = args[0].asI32();
                int messageLen = args[1].asI32();
                
                // Validate parameters
                if (messageLen > 1024) { // 1KB limit
                    throw new SecurityException("Log message too long");
                }
                
                // Read message from plugin memory
                WasmMemory memory = getCurrentMemory();
                byte[] messageBytes = memory.read(messagePtr, messageLen);
                String message = new String(messageBytes, StandardCharsets.UTF_8);
                
                // Log with plugin context
                System.out.printf("[Plugin %s]: %s%n", pluginId, message);
                
                return new WasmValue[0];
            }
        );
    }
    
    private WasmFunction createValidationFunction() {
        return WasmFunction.hostFunction(
            "validate_data",
            FunctionType.of(new WasmValueType[]{WasmValueType.I32, WasmValueType.I32}, 
                           new WasmValueType[]{WasmValueType.I32}),
            (args) -> {
                int dataPtr = args[0].asI32();
                int dataLen = args[1].asI32();
                
                // Read data from plugin memory
                WasmMemory memory = getCurrentMemory();
                byte[] data = memory.read(dataPtr, dataLen);
                
                // Perform validation (example: check for malicious patterns)
                boolean isValid = validateData(data);
                
                return new WasmValue[]{WasmValue.i32(isValid ? 1 : 0)};
            }
        );
    }
    
    private WasmFunction createTimeFunction() {
        return WasmFunction.hostFunction(
            "get_time",
            FunctionType.of(new WasmValueType[]{}, new WasmValueType[]{WasmValueType.I64}),
            (args) -> {
                long currentTime = System.currentTimeMillis();
                return new WasmValue[]{WasmValue.i64(currentTime)};
            }
        );
    }
    
    private WasmFunction createPluginInfoFunction(Plugin plugin) {
        return WasmFunction.hostFunction(
            "get_plugin_info",
            FunctionType.of(new WasmValueType[]{WasmValueType.I32}, new WasmValueType[]{WasmValueType.I32}),
            (args) -> {
                int bufferPtr = args[0].asI32();
                
                // Create plugin info JSON
                String pluginInfo = String.format(
                    "{\"id\":\"%s\",\"version\":\"%s\",\"api\":\"%s\"}",
                    plugin.getId(),
                    plugin.getVersion(),
                    plugin.getManifest().getApiVersion()
                );
                
                // Write to plugin memory
                WasmMemory memory = getCurrentMemory();
                byte[] infoBytes = pluginInfo.getBytes(StandardCharsets.UTF_8);
                memory.write(bufferPtr, infoBytes);
                
                return new WasmValue[]{WasmValue.i32(infoBytes.length)};
            }
        );
    }
    
    /**
     * Demonstrate plugin usage with various scenarios.
     */
    private void demonstratePluginUsage() throws Exception {
        System.out.println("\n=== Plugin System Demo ===");
        
        // Text processing plugin
        PluginContext textContext = new PluginContext("Hello, Plugin World!".getBytes());
        CompletableFuture<PluginResult> textResult = executePlugin("text-processor", "process_text", textContext);
        
        // Data validation plugin
        byte[] testData = "{\"user\":\"john\",\"age\":25}".getBytes();
        PluginContext dataContext = new PluginContext(testData);
        CompletableFuture<PluginResult> validationResult = executePlugin("data-validator", "validate_json", dataContext);
        
        // Crypto utility plugin
        byte[] plaintext = "Secret Message".getBytes();
        PluginContext cryptoContext = new PluginContext(plaintext);
        CompletableFuture<PluginResult> cryptoResult = executePlugin("crypto-util", "encrypt", cryptoContext);
        
        // Wait for results
        PluginResult textRes = textResult.get();
        PluginResult validationRes = validationResult.get();
        PluginResult cryptoRes = cryptoResult.get();
        
        // Display results
        System.out.printf("Text processing result: %s (took %d ms)%n", 
            new String(textRes.getData()), textRes.getExecutionTimeMs());
            
        System.out.printf("Validation result: %s (took %d ms)%n",
            validationRes.isSuccess() ? "Valid" : "Invalid", validationRes.getExecutionTimeMs());
            
        System.out.printf("Encryption result: %d bytes (took %d ms)%n",
            cryptoRes.getData().length, cryptoRes.getExecutionTimeMs());
    }
    
    // Helper classes and methods
    
    public static class PluginContext {
        private final byte[] data;
        
        public PluginContext(byte[] data) {
            this.data = data;
        }
        
        public byte[] getData() { return data; }
        public int getDataSize() { return data.length; }
    }
    
    public static class PluginResult {
        private final String pluginId;
        private final String functionName;
        private final byte[] data;
        private final long executionTimeMs;
        private final boolean success;
        
        public PluginResult(String pluginId, String functionName, byte[] data, 
                          long executionTimeMs, boolean success) {
            this.pluginId = pluginId;
            this.functionName = functionName;
            this.data = data;
            this.executionTimeMs = executionTimeMs;
            this.success = success;
        }
        
        // Getters
        public String getPluginId() { return pluginId; }
        public String getFunctionName() { return functionName; }
        public byte[] getData() { return data; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public boolean isSuccess() { return success; }
    }
    
    public static class PluginException extends RuntimeException {
        public PluginException(String message) { super(message); }
        public PluginException(String message, Throwable cause) { super(message, cause); }
    }
    
    private PluginManifest extractManifest(Module module) {
        // In a real implementation, this would parse metadata from the module
        // For this example, we'll use default values
        return new PluginManifest(
            "1.0",
            new String[]{"logging", "validation"},
            new String[]{"process_text", "validate_json", "encrypt"},
            64 * 1024 * 1024, // 64MB memory limit
            5000 // 5 second execution limit
        );
    }
    
    private void validatePlugin(String pluginId, PluginManifest manifest) throws PluginException {
        // Validate API compatibility
        if (!"1.0".equals(manifest.getApiVersion())) {
            throw new PluginException("Unsupported API version: " + manifest.getApiVersion());
        }
        
        // Validate resource limits
        if (manifest.getMaxMemory() > 256 * 1024 * 1024) { // 256MB limit
            throw new PluginException("Plugin memory limit too high");
        }
        
        if (manifest.getMaxExecutionTime() > 30000) { // 30 second limit
            throw new PluginException("Plugin execution time limit too high");
        }
    }
    
    private int writeContextToMemory(WasmMemory memory, PluginContext context) {
        int offset = 1024; // Reserve first 1KB for plugin use
        memory.write(offset, context.getData());
        return offset;
    }
    
    private WasmValue[] executeWithTimeout(WasmFunction function, WasmValue[] args, long timeoutMs) 
            throws WasmException {
        // In a real implementation, this would use a timeout mechanism
        // For this example, we'll execute directly
        return function.call(args);
    }
    
    private boolean validateData(byte[] data) {
        // Simple validation - check for suspicious patterns
        String dataStr = new String(data, StandardCharsets.UTF_8);
        return !dataStr.contains("<script>") && !dataStr.contains("javascript:");
    }
    
    private WasmMemory getCurrentMemory() {
        // This would be implemented to get the current instance's memory
        // For this example, we'll throw an UnsupportedOperationException
        throw new UnsupportedOperationException("Memory access needs instance context");
    }
    
    public void shutdown() {
        pluginExecutor.shutdown();
        runtime.close();
    }
}