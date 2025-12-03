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
import ai.tegmentum.wasmtime4j.jni.JniLinker;
import ai.tegmentum.wasmtime4j.jni.JniModule;
import ai.tegmentum.wasmtime4j.jni.JniStore;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContextBuilder;
import java.util.logging.Logger;

/**
 * JNI implementation of {@link WasiThreadsContextBuilder}.
 *
 * <p>This builder creates JNI-based WASI-Threads contexts for WebAssembly thread spawning.
 *
 * @since 1.0.0
 */
public final class JniWasiThreadsContextBuilder implements WasiThreadsContextBuilder {

    private static final Logger LOGGER =
            Logger.getLogger(JniWasiThreadsContextBuilder.class.getName());

    /** The WebAssembly module for thread spawning. */
    private Module module;

    /** The linker with WASI imports. */
    private Linker<?> linker;

    /** The store for the main thread. */
    private Store store;

    /**
     * Creates a new JNI WASI-Threads context builder.
     */
    public JniWasiThreadsContextBuilder() {
        // Default constructor
    }

    @Override
    public WasiThreadsContextBuilder withModule(final Module module) {
        JniValidation.requireNonNull(module, "module");
        this.module = module;
        return this;
    }

    @Override
    public WasiThreadsContextBuilder withLinker(final Linker<?> linker) {
        JniValidation.requireNonNull(linker, "linker");
        this.linker = linker;
        return this;
    }

    @Override
    public WasiThreadsContextBuilder withStore(final Store store) {
        JniValidation.requireNonNull(store, "store");
        this.store = store;
        return this;
    }

    @Override
    public WasiThreadsContext build() throws WasmException {
        validateConfiguration();

        try {
            // Get native handles from JNI implementations
            final long moduleHandle = getNativeHandle(module, "module");
            final long linkerHandle = getNativeHandle(linker, "linker");
            final long storeHandle = getNativeHandle(store, "store");

            LOGGER.fine(
                    String.format(
                            "Creating WASI-Threads context with module=0x%x, linker=0x%x,"
                                    + " store=0x%x",
                            moduleHandle, linkerHandle, storeHandle));

            // Create native WASI-Threads context
            final long nativeHandle =
                    JniWasiThreadsContext.nativeCreate(moduleHandle, linkerHandle, storeHandle);

            // Determine if WASI-Threads is enabled
            final boolean enabled = JniWasiThreadsContext.nativeIsSupported();

            LOGGER.info(
                    String.format(
                            "Created WASI-Threads context with handle: 0x%x, enabled: %s",
                            nativeHandle, enabled));

            return new JniWasiThreadsContext(nativeHandle, enabled);
        } catch (final JniException e) {
            throw new WasmException("Failed to create WASI-Threads context: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that all required components have been configured.
     *
     * @throws IllegalStateException if required components are missing
     */
    private void validateConfiguration() {
        if (module == null) {
            throw new IllegalStateException("Module must be set before building");
        }
        if (linker == null) {
            throw new IllegalStateException("Linker must be set before building");
        }
        if (store == null) {
            throw new IllegalStateException("Store must be set before building");
        }
    }

    /**
     * Gets the native handle from a JNI resource.
     *
     * @param resource the resource to get the handle from
     * @param name the name of the resource for error messages
     * @return the native handle
     * @throws WasmException if the resource is not a JNI implementation
     */
    private long getNativeHandle(final Object resource, final String name) throws WasmException {
        if (resource instanceof JniModule) {
            return ((JniModule) resource).getNativeHandle();
        } else if (resource instanceof JniLinker) {
            return ((JniLinker) resource).getNativeHandle();
        } else if (resource instanceof JniStore) {
            return ((JniStore) resource).getNativeHandle();
        } else {
            throw new WasmException(
                    String.format(
                            "Expected JNI implementation for %s, got: %s",
                            name, resource.getClass().getName()));
        }
    }
}
