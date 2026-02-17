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

package examples.advancedusage;

import ai.tegmentum.wasmtime4j.nativeloader.NativeLoader;
import ai.tegmentum.wasmtime4j.nativeloader.PathConvention;
import ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.LibraryLoadInfo;

/**
 * Advanced example demonstrating custom configuration options.
 * 
 * This example shows how to use the builder API for specialized loading
 * scenarios with path conventions and fallback strategies.
 */
public final class CustomConfiguration {

    public static void main(final String[] args) {
        demonstratePathConventions();
        demonstrateCustomPathPatterns();
        demonstrateFallbackStrategies();
    }

    /**
     * Demonstrates different path conventions for various packaging tools.
     */
    private static void demonstratePathConventions() {
        System.out.println("\n=== Path Convention Examples ===");
        
        // Maven-style native packaging
        System.out.println("Loading with Maven Native convention...");
        LibraryLoadInfo mavenInfo = NativeLoader.builder()
            .libraryName("maven-lib")
            .pathConvention(PathConvention.MAVEN_NATIVE)
            .load();
            
        printResult("MAVEN_NATIVE", mavenInfo);
        
        // Gradle-style native packaging
        System.out.println("Loading with Gradle Native convention...");
        LibraryLoadInfo gradleInfo = NativeLoader.builder()
            .libraryName("gradle-lib")
            .pathConvention(PathConvention.JNA)
            .load();
            
        printResult("GRADLE_NATIVE", gradleInfo);
    }

    /**
     * Demonstrates custom path patterns with placeholder substitution.
     */
    private static void demonstrateCustomPathPatterns() {
        System.out.println("\n=== Custom Path Pattern Examples ===");
        
        // Custom path pattern for specialized packaging
        System.out.println("Loading with custom path pattern...");
        LibraryLoadInfo customInfo = NativeLoader.builder()
            .libraryName("custom-lib")
            .customPathPattern("/native-libs/{platform}/{lib}{name}{ext}")
            .load();
            
        printResult("CUSTOM_PATTERN", customInfo);
        
        // Alternative custom pattern
        System.out.println("Loading with alternative custom pattern...");
        LibraryLoadInfo altInfo = NativeLoader.builder()
            .libraryName("alt-lib")
            .customPathPattern("/libs/{os}/{arch}/{name}.{ext}")
            .load();
            
        printResult("ALT_PATTERN", altInfo);
    }

    /**
     * Demonstrates fallback strategies with different conventions.
     */
    private static void demonstrateFallbackStrategies() {
        System.out.println("\n=== Fallback Strategy Examples ===");

        // Using a specific convention
        System.out.println("Loading with Maven Native convention...");
        LibraryLoadInfo fallbackInfo = NativeLoader.builder()
            .libraryName("multi-lib")
            .pathConvention(PathConvention.MAVEN_NATIVE)
            .load();

        printResult("MAVEN_NATIVE", fallbackInfo);

        if (fallbackInfo.getUsedConvention() != null) {
            System.out.println("  Used convention: " + fallbackInfo.getUsedConvention());
        }
    }

    /**
     * Helper method to print loading results consistently.
     */
    private static void printResult(final String label, final LibraryLoadInfo info) {
        if (info.isSuccessful()) {
            System.out.println("  ✓ " + label + " - SUCCESS");
            System.out.println("    Method: " + info.getLoadingMethod());
            if (info.getUsedConvention() != null) {
                System.out.println("    Convention: " + info.getUsedConvention());
            }
        } else {
            System.out.println("  ✗ " + label + " - FAILED");
            System.out.println("    Error: " + info.getErrorMessage());
            System.out.println("    Attempted: " + info.getAttemptedPaths().size() + " paths");
        }
    }
}