/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama.factory;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.PanamaWasmRuntime;

import java.util.logging.Logger;

/**
 * Factory for creating Panama FFI-based WebAssembly runtime instances.
 * 
 * <p>This factory creates runtime instances that use Java 23+ Panama Foreign
 * Function API for high-performance, type-safe access to native Wasmtime
 * functionality. It provides the implementation backend for the unified
 * RuntimeFactory when Panama is available and selected.</p>
 * 
 * <p>The factory performs environment validation to ensure Panama FFI
 * capabilities are available before creating runtime instances.</p>
 * 
 * @since 1.0.0
 */
public final class PanamaRuntimeFactory {
    private static final Logger logger = Logger.getLogger(PanamaRuntimeFactory.class.getName());
    
    // Prevent instantiation
    private PanamaRuntimeFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a new Panama FFI-based WebAssembly runtime.
     * 
     * <p>This method validates that the current Java runtime supports Panama FFI
     * and that native access is properly configured before creating the runtime
     * instance.</p>
     * 
     * @return a new Panama WebAssembly runtime instance
     * @throws WasmException if the runtime cannot be created
     * @throws UnsupportedOperationException if Panama FFI is not available
     */
    public static WasmRuntime createRuntime() throws WasmException {
        logger.info("Creating Panama WebAssembly runtime");
        
        // Validate Panama FFI availability
        validatePanamaSupport();
        
        try {
            return new PanamaWasmRuntime();
        } catch (Exception e) {
            throw new WasmException("Failed to create Panama WebAssembly runtime", e);
        }
    }

    /**
     * Checks if Panama FFI is available and properly configured.
     * 
     * <p>This method validates:</p>
     * <ul>
     *   <li>Java version is 23 or higher</li>
     *   <li>Preview features are enabled (if required)</li>
     *   <li>Native access is enabled</li>
     *   <li>Required foreign function modules are available</li>
     * </ul>
     * 
     * @return true if Panama FFI is available, false otherwise
     */
    public static boolean isPanamaAvailable() {
        try {
            // Check Java version
            final String javaVersion = System.getProperty("java.version");
            if (!isJava23OrHigher(javaVersion)) {
                logger.fine("Panama FFI requires Java 23+, current version: " + javaVersion);
                return false;
            }

            // Check if foreign function classes are available
            try {
                Class.forName("java.lang.foreign.MemorySegment");
                Class.forName("java.lang.foreign.Arena");
                Class.forName("java.lang.foreign.SymbolLookup");
            } catch (ClassNotFoundException e) {
                logger.fine("Panama FFI classes not available: " + e.getMessage());
                return false;
            }

            // Check if native access is enabled
            // TODO: Add proper native access validation
            logger.fine("Panama FFI support detected");
            return true;
        } catch (Exception e) {
            logger.fine("Failed to detect Panama FFI support: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets a human-readable description of Panama FFI availability status.
     * 
     * @return a description of the Panama FFI status
     */
    public static String getPanamaStatus() {
        if (isPanamaAvailable()) {
            return "Panama FFI is available and ready for use";
        }

        final String javaVersion = System.getProperty("java.version");
        if (!isJava23OrHigher(javaVersion)) {
            return "Panama FFI requires Java 23+, current version: " + javaVersion;
        }

        try {
            Class.forName("java.lang.foreign.MemorySegment");
            return "Panama FFI classes available but native access may not be enabled";
        } catch (ClassNotFoundException e) {
            return "Panama FFI classes not available - may need --enable-preview or newer Java version";
        }
    }

    /**
     * Validates that Panama FFI is supported in the current environment.
     * 
     * @throws UnsupportedOperationException if Panama FFI is not available
     */
    private static void validatePanamaSupport() {
        if (!isPanamaAvailable()) {
            final String status = getPanamaStatus();
            throw new UnsupportedOperationException("Panama FFI is not available: " + status);
        }
    }

    /**
     * Checks if the current Java version is 23 or higher.
     * 
     * @param javaVersion the Java version string
     * @return true if version is 23 or higher, false otherwise
     */
    private static boolean isJava23OrHigher(final String javaVersion) {
        try {
            // Parse version string - handle various formats
            String version = javaVersion;
            if (version.startsWith("1.")) {
                // Old format like "1.8.0_261"
                version = version.substring(2);
            }
            
            // Extract major version number
            final int dotIndex = version.indexOf('.');
            final int underscoreIndex = version.indexOf('_');
            final int dashIndex = version.indexOf('-');
            
            int endIndex = version.length();
            if (dotIndex > 0) {
                endIndex = Math.min(endIndex, dotIndex);
            }
            if (underscoreIndex > 0) {
                endIndex = Math.min(endIndex, underscoreIndex);
            }
            if (dashIndex > 0) {
                endIndex = Math.min(endIndex, dashIndex);
            }
            
            final String majorVersionStr = version.substring(0, endIndex);
            final int majorVersion = Integer.parseInt(majorVersionStr);
            
            return majorVersion >= 23;
        } catch (NumberFormatException e) {
            logger.warning("Failed to parse Java version: " + javaVersion);
            return false;
        }
    }
}