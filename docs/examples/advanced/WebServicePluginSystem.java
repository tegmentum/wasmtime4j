package examples.advanced;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.security.*;
import ai.tegmentum.wasmtime4j.wasi.*;
import ai.tegmentum.wasmtime4j.performance.PerformanceMonitor;
import ai.tegmentum.wasmtime4j.resource.ResourcePool;
import ai.tegmentum.wasmtime4j.resource.PoolConfiguration;
import ai.tegmentum.wasmtime4j.resource.PooledResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Advanced example demonstrating a complete web service plugin system using Wasmtime4j.
 *
 * This example shows:
 * - Plugin loading and sandboxing
 * - HTTP request/response handling through WASM
 * - Security policies and resource limits
 * - Performance monitoring and optimization
 * - Resource pooling for production use
 * - Error handling and recovery
 * - Hot reloading of plugins
 */
public class WebServicePluginSystem {

    private static final Logger logger = Logger.getLogger(WebServicePluginSystem.class.getName());

    private final WasmRuntime runtime;
    private final ResourcePool<Engine> enginePool;
    private final Map<String, PluginMetadata> loadedPlugins = new ConcurrentHashMap<>();
    private final SecurityPolicy securityPolicy;
    private final PerformanceMonitor performanceMonitor;

    public WebServicePluginSystem() throws Exception {
        // Initialize runtime with automatic implementation selection
        this.runtime = WasmRuntimeFactory.create();

        // Create engine pool for better performance under load
        PoolConfiguration poolConfig = PoolConfiguration.builder()
            .initialSize(5)
            .maxSize(20)
            .maxIdleTime(Duration.ofMinutes(10))
            .validationQuery(Engine::isValid)
            .build();

        this.enginePool = ResourcePool.<Engine>builder()
            .configuration(poolConfig)
            .factory(() -> {
                EngineConfig config = new EngineConfig()
                    .optimizationLevel(OptimizationLevel.SPEED)
                    .parallelCompilation(true)
                    .consumeFuel(true)
                    .maxFuel(5000000); // Prevent infinite loops
                return runtime.createEngine(config);
            })
            .build();

        // Create security policy for plugins
        this.securityPolicy = SecurityPolicy.builder()
            .allowMemoryAccess(MemoryAccess.READ_WRITE)
            .allowFunction("env", "http_get_header")
            .allowFunction("env", "http_set_header")
            .allowFunction("env", "http_set_status")
            .allowFunction("env", "http_write_body")
            .allowFunction("env", "log")
            .allowFunction("env", "get_current_time")
            .denyFunction("env", "system") // Block system calls
            .denyFunction("env", "file_open") // Block file access
            .setResourceLimits(ResourceLimits.builder()
                .maxMemory(32 * 1024 * 1024) // 32MB per plugin
                .maxFuel(5000000)
                .maxStackDepth(1000)
                .maxExecutionTime(Duration.ofSeconds(30))
                .build())
            .setRateLimits(RateLimits.builder()
                .maxRequestsPerSecond(100)
                .maxConcurrentRequests(10)
                .build())
            .build();

        // Initialize performance monitoring
        this.performanceMonitor = PerformanceMonitor.create();
        this.performanceMonitor.startMonitoring();

        logger.info("WebService plugin system initialized");
        logger.info("Runtime type: " + runtime.getRuntimeInfo().getRuntimeType());
    }

    /**
     * Loads a plugin from a WebAssembly file.
     *
     * @param pluginName unique name for the plugin
     * @param pluginPath path to the .wasm file
     * @param routePattern URL pattern this plugin handles (e.g., "/api/v1/users/*")
     * @throws Exception if plugin loading fails
     */
    public void loadPlugin(String pluginName, String pluginPath, String routePattern) throws Exception {
        logger.info("Loading plugin: " + pluginName + " from " + pluginPath);

        // Read WebAssembly bytecode
        byte[] wasmBytes = Files.readAllBytes(Paths.get(pluginPath));

        // Get engine from pool
        try (PooledResource<Engine> pooledEngine = enginePool.acquire()) {
            Engine engine = pooledEngine.getResource();

            // Compile the plugin module
            Module module = runtime.compileModule(engine, wasmBytes);

            // Validate that the module exports required functions
            if (!module.hasExport("handle_request")) {
                throw new IllegalArgumentException("Plugin must export 'handle_request' function");
            }

            // Create plugin metadata
            PluginMetadata metadata = new PluginMetadata(
                pluginName,
                routePattern,
                module,
                Paths.get(pluginPath).toAbsolutePath(),
                System.currentTimeMillis()
            );

            // Store loaded plugin
            loadedPlugins.put(pluginName, metadata);

            logger.info("Successfully loaded plugin: " + pluginName);
        }
    }

    /**
     * Handles an HTTP request by routing to the appropriate plugin.
     */
    public CompletableFuture<HttpResponse> handleRequest(HttpRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Find matching plugin
                PluginMetadata plugin = findMatchingPlugin(request.getPath());
                if (plugin == null) {
                    return new HttpResponse(404, "Not Found", "No plugin found for path: " + request.getPath());
                }

                // Execute plugin with security sandbox
                return executePluginSafely(plugin, request);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling request", e);
                return new HttpResponse(500, "Internal Server Error", "Plugin execution failed");
            }
        });
    }

    private PluginMetadata findMatchingPlugin(String path) {
        for (PluginMetadata plugin : loadedPlugins.values()) {
            if (pathMatches(path, plugin.getRoutePattern())) {
                return plugin;
            }
        }
        return null;
    }

    private boolean pathMatches(String path, String pattern) {
        // Simple pattern matching - in production, use a proper routing library
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    private HttpResponse executePluginSafely(PluginMetadata plugin, HttpRequest request) throws Exception {
        // Get engine from pool
        try (PooledResource<Engine> pooledEngine = enginePool.acquire()) {
            Engine engine = pooledEngine.getResource();

            // Create isolated store for this request
            Store store = runtime.createStore(engine);

            // Set up security sandbox
            Sandbox sandbox = Sandbox.builder()
                .withPolicy(securityPolicy)
                .withAuditLogging(true)
                .build();

            // Create WASI context for the plugin (limited access)
            WasiConfig wasiConfig = WasiConfig.builder()
                .inheritStderr() // Allow error logging
                .build();

            WasiContext wasiContext = WasiContext.create(wasiConfig);

            // Create linker with host functions
            Linker linker = runtime.createLinker(engine);
            wasiContext.addToLinker(linker);

            // Define HTTP-related host functions
            setupHttpHostFunctions(linker, request);

            // Setup logging function
            linker.defineHostFunction("env", "log", (args) -> {
                String message = readStringFromMemory(store, args[0].asInt(), args[1].asInt());
                logger.info("[Plugin " + plugin.getName() + "] " + message);
                return new WasmValue[0];
            });

            // Setup time function
            linker.defineHostFunction("env", "get_current_time", (args) -> {
                return new WasmValue[] { WasmValue.i64(System.currentTimeMillis()) };
            });

            // Instantiate plugin in sandbox
            HttpResponse response = sandbox.execute(store, (s) -> {
                try {
                    Instance instance = linker.instantiate(s, plugin.getModule());

                    // Get the handle_request function
                    WasmFunction handleRequest = instance.getFunction("handle_request")
                        .orElseThrow(() -> new RuntimeException("Plugin missing handle_request function"));

                    // Serialize request to plugin memory
                    RequestContext requestContext = new RequestContext(instance, request);
                    int requestAddr = requestContext.serializeRequest();

                    // Call the plugin function
                    long startTime = System.nanoTime();
                    WasmValue[] result = handleRequest.call(WasmValue.i32(requestAddr));
                    long endTime = System.nanoTime();

                    // Record performance metrics
                    performanceMonitor.recordFunctionCall(
                        plugin.getName() + ".handle_request",
                        Duration.ofNanos(endTime - startTime)
                    );

                    // Deserialize response from plugin memory
                    return requestContext.deserializeResponse(result[0].asInt());

                } catch (Exception e) {
                    throw new RuntimeException("Plugin execution failed", e);
                }
            });

            return response;
        }
    }

    private void setupHttpHostFunctions(Linker linker, HttpRequest request) {
        final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();

        // Allow plugins to read request headers
        linker.defineHostFunction("env", "http_get_header", (args) -> {
            String headerName = readStringFromMemory(null, args[0].asInt(), args[1].asInt());
            String headerValue = request.getHeaders().get(headerName);
            if (headerValue != null) {
                return writeStringToMemory(null, headerValue);
            } else {
                return new WasmValue[] { WasmValue.i32(0), WasmValue.i32(0) };
            }
        });

        // Allow plugins to set response headers
        linker.defineHostFunction("env", "http_set_header", (args) -> {
            String headerName = readStringFromMemory(null, args[0].asInt(), args[1].asInt());
            String headerValue = readStringFromMemory(null, args[2].asInt(), args[3].asInt());
            responseBuilder.setHeader(headerName, headerValue);
            return new WasmValue[0];
        });

        // Allow plugins to set response status
        linker.defineHostFunction("env", "http_set_status", (args) -> {
            int status = args[0].asInt();
            responseBuilder.setStatus(status);
            return new WasmValue[0];
        });

        // Allow plugins to write response body
        linker.defineHostFunction("env", "http_write_body", (args) -> {
            String body = readStringFromMemory(null, args[0].asInt(), args[1].asInt());
            responseBuilder.appendBody(body);
            return new WasmValue[0];
        });
    }

    private String readStringFromMemory(Store store, int addr, int len) {
        // Implementation would read from WASM memory
        // This is a simplified placeholder
        return "string_from_memory";
    }

    private WasmValue[] writeStringToMemory(Store store, String str) {
        // Implementation would write to WASM memory and return address/length
        // This is a simplified placeholder
        return new WasmValue[] { WasmValue.i32(1000), WasmValue.i32(str.length()) };
    }

    /**
     * Reloads a plugin if the source file has been modified.
     */
    public void checkForPluginUpdates() {
        for (Map.Entry<String, PluginMetadata> entry : loadedPlugins.entrySet()) {
            String pluginName = entry.getKey();
            PluginMetadata metadata = entry.getValue();

            try {
                long currentModTime = Files.getLastModifiedTime(metadata.getSourcePath()).toMillis();
                if (currentModTime > metadata.getLoadTime()) {
                    logger.info("Reloading updated plugin: " + pluginName);
                    String routePattern = metadata.getRoutePattern();
                    loadPlugin(pluginName, metadata.getSourcePath().toString(), routePattern);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to check plugin modification time: " + pluginName, e);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to reload plugin: " + pluginName, e);
            }
        }
    }

    /**
     * Gets plugin statistics and performance metrics.
     */
    public PluginSystemStats getStats() {
        return new PluginSystemStats(
            loadedPlugins.size(),
            enginePool.getStatistics(),
            performanceMonitor.generateReport()
        );
    }

    /**
     * Shuts down the plugin system and releases resources.
     */
    public void shutdown() {
        try {
            performanceMonitor.stop();
            enginePool.close();
            runtime.close();
            logger.info("Plugin system shut down successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during shutdown", e);
        }
    }

    // Helper classes

    public static class HttpRequest {
        private final String method;
        private final String path;
        private final Map<String, String> headers;
        private final String body;

        public HttpRequest(String method, String path, Map<String, String> headers, String body) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.body = body;
        }

        public String getMethod() { return method; }
        public String getPath() { return path; }
        public Map<String, String> getHeaders() { return headers; }
        public String getBody() { return body; }
    }

    public static class HttpResponse {
        private final int status;
        private final String statusText;
        private final Map<String, String> headers;
        private final String body;

        public HttpResponse(int status, String statusText, String body) {
            this(status, statusText, Map.of(), body);
        }

        public HttpResponse(int status, String statusText, Map<String, String> headers, String body) {
            this.status = status;
            this.statusText = statusText;
            this.headers = headers;
            this.body = body;
        }

        public int getStatus() { return status; }
        public String getStatusText() { return statusText; }
        public Map<String, String> getHeaders() { return headers; }
        public String getBody() { return body; }
    }

    private static class HttpResponseBuilder {
        private int status = 200;
        private final Map<String, String> headers = new ConcurrentHashMap<>();
        private final StringBuilder body = new StringBuilder();

        public void setStatus(int status) {
            this.status = status;
        }

        public void setHeader(String name, String value) {
            headers.put(name, value);
        }

        public void appendBody(String content) {
            body.append(content);
        }

        public HttpResponse build() {
            return new HttpResponse(status, getStatusText(status), headers, body.toString());
        }

        private String getStatusText(int status) {
            switch (status) {
                case 200: return "OK";
                case 400: return "Bad Request";
                case 404: return "Not Found";
                case 500: return "Internal Server Error";
                default: return "Unknown";
            }
        }
    }

    private static class PluginMetadata {
        private final String name;
        private final String routePattern;
        private final Module module;
        private final Path sourcePath;
        private final long loadTime;

        public PluginMetadata(String name, String routePattern, Module module, Path sourcePath, long loadTime) {
            this.name = name;
            this.routePattern = routePattern;
            this.module = module;
            this.sourcePath = sourcePath;
            this.loadTime = loadTime;
        }

        public String getName() { return name; }
        public String getRoutePattern() { return routePattern; }
        public Module getModule() { return module; }
        public Path getSourcePath() { return sourcePath; }
        public long getLoadTime() { return loadTime; }
    }

    private static class RequestContext {
        private final Instance instance;
        private final HttpRequest request;

        public RequestContext(Instance instance, HttpRequest request) {
            this.instance = instance;
            this.request = request;
        }

        public int serializeRequest() {
            // Serialize HTTP request into WASM memory
            // This would involve writing the request data to the module's linear memory
            // and returning the address where it was written
            return 1000; // Placeholder address
        }

        public HttpResponse deserializeResponse(int responseAddr) {
            // Deserialize HTTP response from WASM memory
            // This would read the response data from the module's linear memory
            return new HttpResponse(200, "OK", "Response from plugin");
        }
    }

    public static class PluginSystemStats {
        private final int loadedPlugins;
        private final Object poolStats;
        private final Object performanceReport;

        public PluginSystemStats(int loadedPlugins, Object poolStats, Object performanceReport) {
            this.loadedPlugins = loadedPlugins;
            this.poolStats = poolStats;
            this.performanceReport = performanceReport;
        }

        public int getLoadedPlugins() { return loadedPlugins; }
        public Object getPoolStats() { return poolStats; }
        public Object getPerformanceReport() { return performanceReport; }
    }

    // Example usage and testing
    public static void main(String[] args) throws Exception {
        WebServicePluginSystem pluginSystem = new WebServicePluginSystem();

        try {
            // Load example plugins
            pluginSystem.loadPlugin("user-service", "plugins/user-service.wasm", "/api/v1/users/*");
            pluginSystem.loadPlugin("auth-service", "plugins/auth-service.wasm", "/api/v1/auth/*");

            // Simulate HTTP requests
            HttpRequest request1 = new HttpRequest(
                "GET",
                "/api/v1/users/123",
                Map.of("Authorization", "Bearer token123"),
                ""
            );

            HttpRequest request2 = new HttpRequest(
                "POST",
                "/api/v1/auth/login",
                Map.of("Content-Type", "application/json"),
                "{\"username\": \"alice\", \"password\": \"secret\"}"
            );

            // Handle requests asynchronously
            CompletableFuture<HttpResponse> response1 = pluginSystem.handleRequest(request1);
            CompletableFuture<HttpResponse> response2 = pluginSystem.handleRequest(request2);

            // Wait for responses
            HttpResponse result1 = response1.get(5, TimeUnit.SECONDS);
            HttpResponse result2 = response2.get(5, TimeUnit.SECONDS);

            System.out.println("Response 1: " + result1.getStatus() + " - " + result1.getBody());
            System.out.println("Response 2: " + result2.getStatus() + " - " + result2.getBody());

            // Check for plugin updates periodically
            pluginSystem.checkForPluginUpdates();

            // Get system statistics
            PluginSystemStats stats = pluginSystem.getStats();
            System.out.println("Loaded plugins: " + stats.getLoadedPlugins());

        } finally {
            pluginSystem.shutdown();
        }
    }
}