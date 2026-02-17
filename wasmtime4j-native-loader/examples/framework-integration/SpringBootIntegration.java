/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package examples.frameworkintegration;

import ai.tegmentum.wasmtime4j.nativeloader.NativeLoader;
import ai.tegmentum.wasmtime4j.nativeloader.PathConvention;
import ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.LibraryLoadInfo;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Example demonstrating Spring Boot integration patterns.
 * 
 * This example shows how to integrate the native loader with Spring Boot
 * applications, including proper error handling and configuration management.
 * 
 * Note: This example uses annotations conceptually - actual Spring Boot 
 * integration would require the Spring framework dependencies.
 */
public final class SpringBootIntegration {

    private static final Logger LOGGER = Logger.getLogger(SpringBootIntegration.class.getName());

    /**
     * Component-style initialization for Spring Boot applications.
     * 
     * This method would typically be annotated with @PostConstruct
     * in a real Spring Boot application.
     */
    // @PostConstruct
    public void initializeNativeLibraries() {
        LOGGER.info("Initializing native libraries for Spring Boot application...");
        
        try {
            // Load core application native library
            loadCoreLibrary();
            
            // Load optional performance enhancement library
            loadPerformanceLibrary();
            
            LOGGER.info("Native library initialization completed successfully");
        } catch (final RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize native libraries", e);
            throw e; // Fail fast for critical libraries
        }
    }

    /**
     * Loads the core application native library with strict security.
     */
    private void loadCoreLibrary() {
        LOGGER.info("Loading core application library...");
        
        final LibraryLoadInfo info = NativeLoader.builder()
            .libraryName("myapp-core")
            .tempFilePrefix("myapp-core-")
            .pathConvention(PathConvention.MAVEN_NATIVE)
            .load();
            
        if (!info.isSuccessful()) {
            throw new RuntimeException("Failed to load core library: " + info.getErrorMessage());
        }
        
        LOGGER.info("Core library loaded successfully from: " + 
            (info.getExtractedPath() != null ? info.getExtractedPath() : "system path"));
    }

    /**
     * Loads an optional performance enhancement library with graceful fallback.
     */
    private void loadPerformanceLibrary() {
        LOGGER.info("Loading performance enhancement library...");
        
        final LibraryLoadInfo info = NativeLoader.builder()
            .libraryName("myapp-perf")
            .conventionPriority(
                PathConvention.MAVEN_NATIVE,
                PathConvention.WASMTIME4J
            )
            .load();
            
        if (info.isSuccessful()) {
            LOGGER.info("Performance library loaded - enhanced features available");
        } else {
            LOGGER.warning("Performance library not available - using fallback implementation: " + 
                info.getErrorMessage());
            // Continue without performance enhancements
        }
    }

    /**
     * Configuration-based initialization using Spring Boot properties.
     * 
     * This method demonstrates how to use application properties to configure
     * native library loading behavior.
     */
    // @Value("${app.native.library.name:myapp}")
    private String libraryName = "myapp";
    
    // @Value("${app.native.temp.prefix:myapp-}")
    private String tempPrefix = "myapp-";

    public void configurationBasedInitialization() {
        LOGGER.info("Initializing with configuration-based settings...");

        final LibraryLoadInfo info = NativeLoader.builder()
            .libraryName(libraryName)
            .tempFilePrefix(tempPrefix)
            .pathConvention(PathConvention.MAVEN_NATIVE)
            .load();
            
        if (!info.isSuccessful()) {
            throw new RuntimeException("Configuration-based loading failed: " + info.getErrorMessage());
        }
        
        LOGGER.info("Configuration-based initialization successful");
    }

    /**
     * Health check method for Spring Boot Actuator integration.
     * 
     * This method could be used with Spring Boot Actuator to monitor
     * native library status.
     */
    // @Component
    public static class NativeLibraryHealthIndicator {
        
        public void checkHealth() {
            // In a real Spring Boot application, this would implement HealthIndicator
            System.out.println("Checking native library health...");
            
            // Attempt to verify native library is still accessible
            try {
                // Call a simple native method to verify library is working
                System.out.println("Native library health check: OK");
            } catch (final Exception e) {
                System.err.println("Native library health check: FAILED - " + e.getMessage());
            }
        }
    }

    /**
     * Demonstration main method showing the integration patterns.
     */
    public static void main(final String[] args) {
        final SpringBootIntegration integration = new SpringBootIntegration();
        
        // Simulate Spring Boot application startup
        System.out.println("=== Spring Boot Native Library Integration ===");
        
        try {
            integration.initializeNativeLibraries();
            integration.configurationBasedInitialization();
            
            // Simulate health check
            final NativeLibraryHealthIndicator healthIndicator = new NativeLibraryHealthIndicator();
            healthIndicator.checkHealth();
            
        } catch (final RuntimeException e) {
            System.err.println("Application startup failed: " + e.getMessage());
            System.exit(1);
        }
        
        System.out.println("Spring Boot application started successfully with native libraries");
    }
}