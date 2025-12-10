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

package ai.tegmentum.wasmtime4j.jni.wasi.keyvalue;

import ai.tegmentum.wasmtime4j.wasi.keyvalue.ConsistencyModel;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.EvictionPolicy;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.IsolationLevel;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueEntry;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueException;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueTransaction;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.WasiKeyValue;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI keyvalue interface.
 *
 * <p>This implementation uses JNI to call native WASI keyvalue functions.
 *
 * @since 1.0.0
 */
public final class JniWasiKeyValue implements WasiKeyValue {

    private static final Logger LOGGER = Logger.getLogger(JniWasiKeyValue.class.getName());

    /** Native context handle. */
    private volatile long contextHandle;

    /** Current consistency model. */
    private ConsistencyModel consistencyModel = ConsistencyModel.EVENTUAL;

    /** Whether the store is closed. */
    private volatile boolean closed = false;

    // Native library loading
    static {
        try {
            System.loadLibrary("wasmtime4j");
        } catch (UnsatisfiedLinkError e) {
            LOGGER.log(Level.SEVERE, "Failed to load native library", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    // Native method declarations
    private static native long nativeCreateContext();

    private static native void nativeDestroyContext(long handle);

    private static native long nativeGetContextId(long handle);

    private static native boolean nativeIsContextValid(long handle);

    private static native byte[] nativeGet(long handle, String key);

    private static native boolean nativeSet(long handle, String key, byte[] value);

    private static native boolean nativeDelete(long handle, String key);

    private static native boolean nativeExists(long handle, String key);

    private static native long nativeIncrement(long handle, String key, long delta);

    private static native long nativeSize(long handle);

    private static native boolean nativeClear(long handle);

    private static native String nativeKeys(long handle);

    private static native boolean nativeIsAvailable();

    /**
     * Creates a new JNI WASI keyvalue store.
     *
     * @throws KeyValueException if creation fails
     */
    public JniWasiKeyValue() throws KeyValueException {
        this.contextHandle = nativeCreateContext();
        if (this.contextHandle == 0) {
            throw new KeyValueException("Failed to create keyvalue context");
        }
        LOGGER.log(Level.FINE, "Created JNI WASI keyvalue context: {0}", contextHandle);
    }

    /**
     * Checks if WASI keyvalue support is available.
     *
     * @return true if available
     */
    public static boolean isAvailable() {
        try {
            return nativeIsAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    private void ensureOpen() throws KeyValueException {
        if (closed) {
            throw new KeyValueException("Keyvalue store is closed");
        }
    }

    @Override
    public Optional<byte[]> get(final String key) throws KeyValueException {
        ensureOpen();
        byte[] result = nativeGet(contextHandle, key);
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<KeyValueEntry> getEntry(final String key) throws KeyValueException {
        return get(key).map(value -> KeyValueEntry.builder(key, value).build());
    }

    @Override
    public void set(final String key, final byte[] value) throws KeyValueException {
        ensureOpen();
        if (!nativeSet(contextHandle, key, value)) {
            throw new KeyValueException("Failed to set value for key: " + key);
        }
    }

    @Override
    public void set(final String key, final byte[] value, final Duration ttl)
            throws KeyValueException {
        // TTL not supported in basic implementation, just set the value
        set(key, value);
    }

    @Override
    public boolean delete(final String key) throws KeyValueException {
        ensureOpen();
        return nativeDelete(contextHandle, key);
    }

    @Override
    public boolean exists(final String key) throws KeyValueException {
        ensureOpen();
        return nativeExists(contextHandle, key);
    }

    @Override
    public Set<String> keys(final String pattern) throws KeyValueException {
        // Pattern matching not supported, return all keys
        return keys();
    }

    @Override
    public Set<String> keys() throws KeyValueException {
        ensureOpen();
        String json = nativeKeys(contextHandle);
        if (json == null) {
            return new HashSet<>();
        }

        // Parse JSON array (simple implementation)
        Set<String> keySet = new HashSet<>();
        if (json.startsWith("[") && json.endsWith("]")) {
            String content = json.substring(1, json.length() - 1);
            if (!content.isEmpty()) {
                String[] parts = content.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                        keySet.add(trimmed.substring(1, trimmed.length() - 1));
                    }
                }
            }
        }
        return keySet;
    }

    @Override
    public boolean setIfAbsent(final String key, final byte[] value) throws KeyValueException {
        if (!exists(key)) {
            set(key, value);
            return true;
        }
        return false;
    }

    @Override
    public boolean setIfPresent(final String key, final byte[] value) throws KeyValueException {
        if (exists(key)) {
            set(key, value);
            return true;
        }
        return false;
    }

    @Override
    public boolean compareAndSwap(final String key, final byte[] expectedValue,
            final byte[] newValue) throws KeyValueException {
        Optional<byte[]> current = get(key);
        if (current.isPresent() && Arrays.equals(current.get(), expectedValue)) {
            set(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean compareVersionAndSwap(final String key, final long expectedVersion,
            final byte[] newValue) throws KeyValueException {
        // Version support not implemented
        throw new KeyValueException("Version-based CAS not supported");
    }

    @Override
    public long increment(final String key, final long delta) throws KeyValueException {
        ensureOpen();
        return nativeIncrement(contextHandle, key, delta);
    }

    @Override
    public Optional<byte[]> getAndDelete(final String key) throws KeyValueException {
        Optional<byte[]> value = get(key);
        if (value.isPresent()) {
            delete(key);
        }
        return value;
    }

    @Override
    public Optional<byte[]> getAndSet(final String key, final byte[] newValue)
            throws KeyValueException {
        Optional<byte[]> oldValue = get(key);
        set(key, newValue);
        return oldValue;
    }

    @Override
    public Map<String, byte[]> getMultiple(final Set<String> keySet) throws KeyValueException {
        Map<String, byte[]> result = new HashMap<>();
        for (String key : keySet) {
            get(key).ifPresent(value -> result.put(key, value));
        }
        return result;
    }

    @Override
    public void setMultiple(final Map<String, byte[]> entries) throws KeyValueException {
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Set<String> deleteMultiple(final Set<String> keySet) throws KeyValueException {
        Set<String> deleted = new HashSet<>();
        for (String key : keySet) {
            if (delete(key)) {
                deleted.add(key);
            }
        }
        return deleted;
    }

    // List operations - not supported in basic implementation
    @Override
    public long listAppend(final String key, final List<byte[]> values) throws KeyValueException {
        throw new KeyValueException("List operations not supported");
    }

    @Override
    public long listPrepend(final String key, final List<byte[]> values) throws KeyValueException {
        throw new KeyValueException("List operations not supported");
    }

    @Override
    public List<byte[]> listRange(final String key, final long start, final long end)
            throws KeyValueException {
        throw new KeyValueException("List operations not supported");
    }

    @Override
    public long listLength(final String key) throws KeyValueException {
        throw new KeyValueException("List operations not supported");
    }

    @Override
    public Optional<byte[]> listPop(final String key) throws KeyValueException {
        throw new KeyValueException("List operations not supported");
    }

    @Override
    public Optional<byte[]> listShift(final String key) throws KeyValueException {
        throw new KeyValueException("List operations not supported");
    }

    // Set operations - not supported in basic implementation
    @Override
    public long setAdd(final String key, final Set<byte[]> members) throws KeyValueException {
        throw new KeyValueException("Set operations not supported");
    }

    @Override
    public long setRemove(final String key, final Set<byte[]> members) throws KeyValueException {
        throw new KeyValueException("Set operations not supported");
    }

    @Override
    public Set<byte[]> setMembers(final String key) throws KeyValueException {
        throw new KeyValueException("Set operations not supported");
    }

    @Override
    public boolean setIsMember(final String key, final byte[] member) throws KeyValueException {
        throw new KeyValueException("Set operations not supported");
    }

    @Override
    public long setSize(final String key) throws KeyValueException {
        throw new KeyValueException("Set operations not supported");
    }

    // Hash operations - not supported in basic implementation
    @Override
    public void hashSet(final String key, final String field, final byte[] value)
            throws KeyValueException {
        throw new KeyValueException("Hash operations not supported");
    }

    @Override
    public Optional<byte[]> hashGet(final String key, final String field)
            throws KeyValueException {
        throw new KeyValueException("Hash operations not supported");
    }

    @Override
    public boolean hashDelete(final String key, final String field) throws KeyValueException {
        throw new KeyValueException("Hash operations not supported");
    }

    @Override
    public Map<String, byte[]> hashGetAll(final String key) throws KeyValueException {
        throw new KeyValueException("Hash operations not supported");
    }

    @Override
    public Set<String> hashKeys(final String key) throws KeyValueException {
        throw new KeyValueException("Hash operations not supported");
    }

    @Override
    public boolean hashExists(final String key, final String field) throws KeyValueException {
        throw new KeyValueException("Hash operations not supported");
    }

    // Transaction support - not supported in basic implementation
    @Override
    public KeyValueTransaction beginTransaction() throws KeyValueException {
        throw new KeyValueException("Transactions not supported");
    }

    @Override
    public KeyValueTransaction beginTransaction(final IsolationLevel isolationLevel)
            throws KeyValueException {
        throw new KeyValueException("Transactions not supported");
    }

    @Override
    public ConsistencyModel getConsistencyModel() {
        return consistencyModel;
    }

    @Override
    public void setConsistencyModel(final ConsistencyModel model) throws KeyValueException {
        this.consistencyModel = model;
    }

    @Override
    public EvictionPolicy getEvictionPolicy() {
        return EvictionPolicy.LRU;
    }

    // TTL operations - not supported in basic implementation
    @Override
    public Optional<Duration> getTtl(final String key) throws KeyValueException {
        return Optional.empty();
    }

    @Override
    public boolean setTtl(final String key, final Duration ttl) throws KeyValueException {
        return false;
    }

    @Override
    public boolean persist(final String key) throws KeyValueException {
        return exists(key);
    }

    @Override
    public long size() throws KeyValueException {
        ensureOpen();
        return nativeSize(contextHandle);
    }

    @Override
    public boolean isEmpty() throws KeyValueException {
        return size() == 0;
    }

    @Override
    public void clear() throws KeyValueException {
        ensureOpen();
        if (!nativeClear(contextHandle)) {
            throw new KeyValueException("Failed to clear store");
        }
    }

    @Override
    public void close() throws KeyValueException {
        if (closed) {
            return;
        }
        closed = true;

        if (contextHandle != 0) {
            nativeDestroyContext(contextHandle);
            contextHandle = 0;
        }
    }
}
