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

package examples.basicusage;

import ai.tegmentum.wasmtime4j.nativeloader.NativeLoader;
import ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.LibraryLoadInfo;

/**
 * Simple example demonstrating basic native library loading.
 * 
 * This example shows the most straightforward way to load a native library
 * using default settings.
 */
public final class SimpleLoading {

    public static void main(final String[] args) {
        // Simple library loading with default configuration
        System.out.println("Loading native library with default settings...");
        
        LibraryLoadInfo info = NativeLoader.loadLibrary("mylib");
        
        if (info.isLoadedSuccessfully()) {
            System.out.println("✓ Library loaded successfully!");
            System.out.println("  Method: " + info.getLoadingMethod());
            System.out.println("  Platform: " + info.getPlatformInfo().getPlatform());
            
            if (info.getExtractedPath() != null) {
                System.out.println("  Extracted to: " + info.getExtractedPath());
            }
        } else {
            System.err.println("✗ Failed to load library");
            System.err.println("  Error: " + info.getErrorMessage());
            System.err.println("  Platform: " + info.getPlatformInfo().getPlatform());
            System.err.println("  Attempted paths:");
            info.getAttemptedPaths().forEach(path -> 
                System.err.println("    - " + path));
        }
    }
}