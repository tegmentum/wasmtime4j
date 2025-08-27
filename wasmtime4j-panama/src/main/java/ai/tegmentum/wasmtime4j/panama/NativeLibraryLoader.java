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

package ai.tegmentum.wasmtime4j.panama;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native library loader for Panama FFI implementation.
 *
 * <p>This class handles platform detection, native library loading, and function discovery
 * for the wasmtime4j-panama module. It provides automatic platform detection,
 * function symbol lookup, and resource management using Arena-based cleanup.
 */
public final class NativeLibraryLoader {

    private static final Logger LOGGER = Logger.getLogger(NativeLibraryLoader.class.getName());
    
    // Platform detection constants
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();
    
    // Native library constants
    private static final String LIBRARY_NAME = "wasmtime4j";
    private static final String NATIVES_PREFIX = "/natives/";
    
    // Platform-specific library extensions
    private static final String WINDOWS_EXTENSION = ".dll";
    private static final String UNIX_EXTENSION = ".so";
    private static final String MACOS_EXTENSION = ".dylib";
    
    // Singleton instance
    private static volatile NativeLibraryLoader instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    // Internal state
    private final String platformPath;
    private final String libraryFileName;
    private final SymbolLookup symbolLookup;
    private final Arena libraryArena;
    private final ConcurrentHashMap<String, MethodHandle> methodHandleCache;
    
    // Status tracking
    private volatile boolean loaded = false;
    private volatile Exception loadingError;

    /**
     * Private constructor for singleton pattern.
     *
     * @throws IllegalStateException if library loading fails
     */
    private NativeLibraryLoader() {
        this.platformPath = detectPlatformPath();
        this.libraryFileName = detectLibraryFileName();
        this.libraryArena = Arena.ofShared();
        this.methodHandleCache = new ConcurrentHashMap<>();
        
        SymbolLookup tempSymbolLookup;
        try {
            tempSymbolLookup = loadNativeLibrary();
            this.loaded = true;
            LOGGER.info("Successfully loaded native library for platform: " + this.platformPath);
        } catch (Exception e) {
            this.loadingError = e;
            tempSymbolLookup = null;
            LOGGER.log(Level.SEVERE, "Failed to load native library for platform: " + this.platformPath, e);
            throw new IllegalStateException("Failed to load native library", e);
        }
        this.symbolLookup = tempSymbolLookup;
    }

    /**
     * Gets the singleton instance of the native library loader.
     *
     * @return the singleton instance
     * @throws IllegalStateException if library loading failed
     */
    public static NativeLibraryLoader getInstance() {
        NativeLibraryLoader result = instance;
        if (result == null) {
            synchronized (INSTANCE_LOCK) {
                result = instance;
                if (result == null) {
                    instance = result = new NativeLibraryLoader();
                }
            }
        }
        return result;
    }

    /**
     * Checks if the native library is loaded and available.
     *
     * @return true if loaded successfully, false otherwise
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Gets the loading error if library loading failed.
     *
     * @return the loading error, or null if no error occurred
     */
    public Optional<Exception> getLoadingError() {
        return Optional.ofNullable(loadingError);
    }

    /**
     * Gets the platform path used for library loading.
     *
     * @return the platform-specific path
     */
    public String getPlatformPath() {
        return platformPath;
    }

    /**
     * Gets the library file name used for loading.
     *
     * @return the library file name
     */
    public String getLibraryFileName() {
        return libraryFileName;
    }

    /**
     * Looks up a function symbol and returns a MethodHandle.
     *
     * @param functionName the name of the function to look up
     * @param descriptor the function descriptor defining signature
     * @return optional containing the MethodHandle, or empty if not found
     */
    public Optional<MethodHandle> lookupFunction(final String functionName, 
                                                final FunctionDescriptor descriptor) {
        if (!loaded || symbolLookup == null) {
            LOGGER.warning("Attempted to lookup function before library was loaded: " + functionName);
            return Optional.empty();
        }
        
        // Check cache first
        String cacheKey = functionName + "_" + descriptor.hashCode();
        MethodHandle cached = methodHandleCache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        try {
            Optional<MemorySegment> symbol = symbolLookup.find(functionName);
            if (symbol.isEmpty()) {
                LOGGER.warning("Function symbol not found: " + functionName);
                return Optional.empty();
            }
            
            Linker linker = Linker.nativeLinker();
            MethodHandle handle = linker.downcallHandle(symbol.get(), descriptor);
            
            // Cache the handle for future use
            methodHandleCache.put(cacheKey, handle);
            
            LOGGER.fine("Successfully looked up function: " + functionName);
            return Optional.of(handle);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to lookup function: " + functionName, e);
            return Optional.empty();
        }
    }

    /**
     * Gets the Arena used for library resource management.
     *
     * @return the library arena
     */
    public Arena getLibraryArena() {
        return libraryArena;
    }

    /**
     * Clears the method handle cache.
     */
    public void clearMethodHandleCache() {
        methodHandleCache.clear();
        LOGGER.fine("Cleared method handle cache");
    }

    /**
     * Closes the library and cleans up resources.
     * This should only be called during JVM shutdown.
     */
    public void close() {
        try {
            clearMethodHandleCache();
            if (libraryArena != null && libraryArena.scope().isAlive()) {
                libraryArena.close();
            }
            loaded = false;
            LOGGER.info("Closed native library loader");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during library loader shutdown", e);
        }
    }

    /**
     * Detects the platform-specific path for the native library.
     *
     * @return the platform path
     * @throws IllegalStateException if platform is not supported
     */
    private String detectPlatformPath() {
        String osName = normalizeOs();
        String osArch = normalizeArch();
        
        String platformPath = osName + "-" + osArch;
        LOGGER.fine("Detected platform path: " + platformPath);
        
        return platformPath;
    }

    /**
     * Detects the platform-specific library file name.
     *
     * @return the library file name
     */
    private String detectLibraryFileName() {
        String extension;
        if (OS_NAME.contains("windows")) {
            extension = WINDOWS_EXTENSION;
        } else if (OS_NAME.contains("mac") || OS_NAME.contains("darwin")) {
            extension = MACOS_EXTENSION;
        } else {
            extension = UNIX_EXTENSION;
        }
        
        String fileName = System.mapLibraryName(LIBRARY_NAME);
        if (!fileName.endsWith(extension)) {
            fileName = LIBRARY_NAME + extension;
        }
        
        LOGGER.fine("Detected library file name: " + fileName);
        return fileName;
    }

    /**
     * Normalizes the operating system name.
     *
     * @return normalized OS name
     */
    private String normalizeOs() {
        if (OS_NAME.contains("windows")) {
            return "windows";
        } else if (OS_NAME.contains("mac") || OS_NAME.contains("darwin")) {
            return "macos";
        } else if (OS_NAME.contains("linux")) {
            return "linux";
        } else {
            throw new IllegalStateException("Unsupported operating system: " + OS_NAME);
        }
    }

    /**
     * Normalizes the architecture name.
     *
     * @return normalized architecture name
     */
    private String normalizeArch() {
        if (OS_ARCH.contains("amd64") || OS_ARCH.contains("x86_64")) {
            return "x64";
        } else if (OS_ARCH.contains("aarch64") || OS_ARCH.contains("arm64")) {
            return "aarch64";
        } else if (OS_ARCH.contains("x86") || OS_ARCH.contains("i386")) {
            return "x86";
        } else {
            throw new IllegalStateException("Unsupported architecture: " + OS_ARCH);
        }
    }

    /**
     * Loads the native library and returns a SymbolLookup.
     *
     * @return SymbolLookup for the loaded library
     * @throws IOException if library loading fails
     */
    private SymbolLookup loadNativeLibrary() throws IOException {
        // First try loading from java.library.path
        try {
            System.loadLibrary(LIBRARY_NAME);
            LOGGER.info("Loaded native library from java.library.path");
            return SymbolLookup.loaderLookup();
        } catch (UnsatisfiedLinkError e) {
            LOGGER.fine("Library not found in java.library.path, trying embedded resources");
        }
        
        // Try loading from embedded resources
        String resourcePath = NATIVES_PREFIX + platformPath + "/" + libraryFileName;
        
        try (InputStream libStream = getClass().getResourceAsStream(resourcePath)) {
            if (libStream == null) {
                throw new IOException("Native library not found in resources: " + resourcePath);
            }
            
            // Create temporary file for library extraction
            Path tempDir = Files.createTempDirectory("wasmtime4j-panama");
            Path tempLibrary = tempDir.resolve(libraryFileName);
            
            // Mark temporary files for deletion on exit
            tempDir.toFile().deleteOnExit();
            tempLibrary.toFile().deleteOnExit();
            
            // Copy library to temporary location
            Files.copy(libStream, tempLibrary, StandardCopyOption.REPLACE_EXISTING);
            
            // Load library from temporary location
            System.load(tempLibrary.toString());
            
            LOGGER.info("Loaded native library from embedded resources: " + resourcePath);
            return SymbolLookup.loaderLookup();
        }
    }
}