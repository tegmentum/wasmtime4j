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

package ai.tegmentum.wasmtime4j.jni.wasi.threads;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniLibraryLoader;
import ai.tegmentum.wasmtime4j.jni.JniLinker;
import ai.tegmentum.wasmtime4j.jni.JniModule;
import ai.tegmentum.wasmtime4j.jni.JniStore;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContextBuilder;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of {@link WasiThreadsProvider}.
 *
 * <p>This provider creates JNI-based WASI-Threads contexts for thread spawning support in
 * WebAssembly modules. It is discovered via the ServiceLoader mechanism.
 *
 * @since 1.0.0
 */
public final class JniWasiThreadsProvider implements WasiThreadsProvider {

    private static final Logger LOGGER = Logger.getLogger(JniWasiThreadsProvider.class.getName());

    /** Cached availability check result. */
    private static volatile Boolean available;

    /**
     * Creates a new JNI WASI-Threads provider.
     *
     * <p>This no-argument constructor is required for ServiceLoader discovery.
     */
    public JniWasiThreadsProvider() {
        // ServiceLoader requires a public no-arg constructor
    }

    @Override
    public boolean isAvailable() {
        if (available == null) {
            synchronized (JniWasiThreadsProvider.class) {
                if (available == null) {
                    available = checkAvailability();
                }
            }
        }
        return available;
    }

    @Override
    public WasiThreadsContextBuilder createBuilder() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException(
                    "WASI-Threads is not available in this JNI runtime");
        }
        return new JniWasiThreadsContextBuilder();
    }

    @Override
    public void addToLinker(final Linker<?> linker, final Store store, final Module module)
            throws WasmException {
        JniValidation.requireNonNull(linker, "linker");
        JniValidation.requireNonNull(store, "store");
        JniValidation.requireNonNull(module, "module");

        if (!isAvailable()) {
            throw new UnsupportedOperationException(
                    "WASI-Threads is not available in this JNI runtime");
        }

        // Validate that we have JNI implementations
        if (!(linker instanceof JniLinker)) {
            throw new WasmException(
                    "Expected JNI linker implementation, got: " + linker.getClass().getName());
        }
        if (!(store instanceof JniStore)) {
            throw new WasmException(
                    "Expected JNI store implementation, got: " + store.getClass().getName());
        }
        if (!(module instanceof JniModule)) {
            throw new WasmException(
                    "Expected JNI module implementation, got: " + module.getClass().getName());
        }

        try {
            final long linkerHandle = ((JniLinker) linker).getNativeHandle();
            final long storeHandle = ((JniStore) store).getNativeHandle();
            final long moduleHandle = ((JniModule) module).getNativeHandle();

            LOGGER.fine(
                    String.format(
                            "Adding thread-spawn to linker: linker=0x%x, store=0x%x, module=0x%x",
                            linkerHandle, storeHandle, moduleHandle));

            JniWasiThreadsContext.nativeAddToLinker(linkerHandle, storeHandle, moduleHandle);

            LOGGER.info("Successfully added thread-spawn function to linker");
        } catch (final JniException e) {
            throw new WasmException("Failed to add thread-spawn to linker: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if WASI-Threads support is available.
     *
     * @return true if available, false otherwise
     */
    private static boolean checkAvailability() {
        try {
            // First ensure native library is loaded
            if (!JniLibraryLoader.isLoaded()) {
                LOGGER.fine("JNI native library not loaded, WASI-Threads unavailable");
                return false;
            }

            // Check native support
            final boolean supported = JniWasiThreadsContext.nativeIsSupported();
            LOGGER.info("WASI-Threads support check: " + supported);
            return supported;
        } catch (final UnsatisfiedLinkError e) {
            LOGGER.log(
                    Level.FINE,
                    "WASI-Threads native methods not available: " + e.getMessage(),
                    e);
            return false;
        } catch (final Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error checking WASI-Threads availability: " + e.getMessage(),
                    e);
            return false;
        }
    }
}
