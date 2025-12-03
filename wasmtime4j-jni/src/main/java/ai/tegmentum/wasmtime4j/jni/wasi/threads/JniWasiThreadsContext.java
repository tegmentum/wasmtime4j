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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * JNI implementation of {@link WasiThreadsContext} for WASI-Threads support.
 *
 * <p>This class provides thread spawning capabilities for WebAssembly modules that use the
 * wasi-threads proposal. It manages the native context for thread spawning and tracks thread IDs.
 *
 * <p><strong>Important limitations:</strong>
 *
 * <ul>
 *   <li>A trap or WASI exit in one thread will exit the entire process
 *   <li>Not suitable for multi-tenant embeddings
 *   <li>Requires WASI Preview 1 (not compatible with WASI 0.2)
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniWasiThreadsContext extends JniResource implements WasiThreadsContext {

    private static final Logger LOGGER = Logger.getLogger(JniWasiThreadsContext.class.getName());

    /** Maximum valid thread ID as per WASI-Threads specification (0x1FFFFFFF). */
    private static final int MAX_THREAD_ID = 0x1FFFFFFF;

    /** Counter for tracking the maximum assigned thread ID. */
    private final AtomicInteger maxThreadId = new AtomicInteger(0);

    /** Counter for tracking the current number of active threads. */
    private final AtomicInteger threadCount = new AtomicInteger(1); // Main thread starts at 1

    /** Flag indicating if WASI-Threads support is enabled. */
    private final boolean enabled;

    /**
     * Creates a new JNI WASI-Threads context with the specified native handle.
     *
     * @param nativeHandle the native handle for the WASI-Threads context
     * @param enabled whether WASI-Threads support is enabled
     */
    JniWasiThreadsContext(final long nativeHandle, final boolean enabled) {
        super(nativeHandle);
        this.enabled = enabled;

        LOGGER.info(
                String.format(
                        "Created WASI-Threads context with handle: 0x%x, enabled: %s",
                        nativeHandle, enabled));
    }

    @Override
    public int spawn(final int threadStartArg) throws WasmException {
        ensureNotClosed();

        if (!enabled) {
            throw new WasmException("WASI-Threads is not enabled for this context");
        }

        try {
            final int threadId = nativeSpawn(nativeHandle, threadStartArg);

            if (threadId > 0) {
                // Update tracking counters
                threadCount.incrementAndGet();

                // Update max thread ID if this is higher
                int currentMax;
                do {
                    currentMax = maxThreadId.get();
                    if (threadId <= currentMax) {
                        break;
                    }
                } while (!maxThreadId.compareAndSet(currentMax, threadId));

                LOGGER.fine(
                        String.format(
                                "Spawned thread with ID: %d, arg: %d", threadId, threadStartArg));
            } else {
                LOGGER.warning(
                        String.format(
                                "Failed to spawn thread with arg: %d, returned: %d",
                                threadStartArg, threadId));
            }

            return threadId;
        } catch (final JniException e) {
            throw new WasmException("Failed to spawn thread: " + e.getMessage(), e);
        }
    }

    @Override
    public int getThreadCount() {
        ensureNotClosed();
        return threadCount.get();
    }

    @Override
    public boolean isEnabled() {
        return enabled && !isClosed();
    }

    @Override
    public int getMaxThreadId() {
        ensureNotClosed();
        return maxThreadId.get();
    }

    @Override
    public boolean isValid() {
        return !isClosed() && enabled;
    }

    /**
     * Decrements the thread count when a thread completes.
     *
     * <p>This method should be called by native code when a spawned thread finishes execution.
     *
     * @param threadId the ID of the thread that completed
     */
    void onThreadCompleted(final int threadId) {
        JniValidation.requirePositive(threadId, "threadId");

        final int remaining = threadCount.decrementAndGet();
        LOGGER.fine(
                String.format(
                        "Thread %d completed, remaining threads: %d", threadId, remaining));
    }

    @Override
    protected void doClose() throws Exception {
        LOGGER.fine("Closing WASI-Threads context with handle: 0x" + Long.toHexString(nativeHandle));

        // Wait for all threads to complete or terminate them
        final int remaining = threadCount.get();
        if (remaining > 1) {
            LOGGER.warning(
                    String.format(
                            "Closing WASI-Threads context with %d threads still active",
                            remaining - 1));
        }

        nativeClose(nativeHandle);

        LOGGER.info("WASI-Threads context closed successfully");
    }

    @Override
    protected String getResourceType() {
        return "WasiThreadsContext";
    }

    // Native methods

    /**
     * Native method to spawn a new thread.
     *
     * @param handle the native handle for the WASI-Threads context
     * @param threadStartArg the argument to pass to the thread start function
     * @return a positive thread ID on success (1 to 0x1FFFFFFF), or -1 on failure
     * @throws JniException if an error occurs during thread spawning
     */
    private static native int nativeSpawn(long handle, int threadStartArg) throws JniException;

    /**
     * Native method to close the WASI-Threads context.
     *
     * @param handle the native handle for the WASI-Threads context
     */
    private static native void nativeClose(long handle);

    /**
     * Native method to check if WASI-Threads support is available.
     *
     * @return true if WASI-Threads is supported, false otherwise
     */
    static native boolean nativeIsSupported();

    /**
     * Native method to create a new WASI-Threads context.
     *
     * @param moduleHandle the native handle for the WebAssembly module
     * @param linkerHandle the native handle for the linker
     * @param storeHandle the native handle for the store
     * @return the native handle for the created WASI-Threads context
     * @throws JniException if context creation fails
     */
    static native long nativeCreate(long moduleHandle, long linkerHandle, long storeHandle)
            throws JniException;

    /**
     * Native method to add the thread-spawn function to a linker.
     *
     * @param linkerHandle the native handle for the linker
     * @param storeHandle the native handle for the store
     * @param moduleHandle the native handle for the module
     * @throws JniException if adding to linker fails
     */
    static native void nativeAddToLinker(long linkerHandle, long storeHandle, long moduleHandle)
            throws JniException;
}
