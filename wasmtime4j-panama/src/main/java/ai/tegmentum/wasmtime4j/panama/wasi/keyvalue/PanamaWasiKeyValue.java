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

package ai.tegmentum.wasmtime4j.panama.wasi.keyvalue;

import ai.tegmentum.wasmtime4j.wasi.keyvalue.ConsistencyModel;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.EvictionPolicy;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.IsolationLevel;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueEntry;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueException;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueTransaction;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.WasiKeyValue;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WASI keyvalue interface.
 *
 * <p>This implementation uses Java's Foreign Function and Memory API (Panama)
 * to call native WASI keyvalue functions.
 *
 * @since 1.0.0
 */
public final class PanamaWasiKeyValue implements WasiKeyValue {

    private static final Logger LOGGER = Logger.getLogger(PanamaWasiKeyValue.class.getName());

    /** Native context handle. */
    private volatile long contextHandle;

    /** Arena for memory management. */
    private final Arena arena;

    /** Current consistency model. */
    private ConsistencyModel consistencyModel = ConsistencyModel.EVENTUAL;

    /** Whether the store is closed. */
    private volatile boolean closed = false;

    // Native method handles
    private static final MethodHandle CREATE_CONTEXT;
    private static final MethodHandle DESTROY_CONTEXT;
    private static final MethodHandle GET;
    private static final MethodHandle SET;
    private static final MethodHandle DELETE;
    private static final MethodHandle EXISTS;
    private static final MethodHandle INCREMENT;
    private static final MethodHandle SIZE;
    private static final MethodHandle CLEAR;
    private static final MethodHandle KEYS;
    private static final MethodHandle FREE_BYTES;
    private static final MethodHandle FREE_STRING;
    private static final MethodHandle IS_AVAILABLE;

    static {
        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup lookup = SymbolLookup.loaderLookup();

            CREATE_CONTEXT = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_context_create").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS)
            );

            DESTROY_CONTEXT = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_context_destroy").orElseThrow(),
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );

            GET = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_get").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,  // context
                    ValueLayout.ADDRESS,  // key
                    ValueLayout.ADDRESS,  // out_value
                    ValueLayout.ADDRESS   // out_value_len
                )
            );

            SET = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_set").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,  // context
                    ValueLayout.ADDRESS,  // key
                    ValueLayout.ADDRESS,  // value
                    ValueLayout.JAVA_LONG // value_len
                )
            );

            DELETE = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_delete").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,  // context
                    ValueLayout.ADDRESS   // key
                )
            );

            EXISTS = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_exists").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,  // context
                    ValueLayout.ADDRESS   // key
                )
            );

            INCREMENT = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_increment").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,  // context
                    ValueLayout.ADDRESS,  // key
                    ValueLayout.JAVA_LONG, // delta
                    ValueLayout.ADDRESS   // out_value
                )
            );

            SIZE = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_size").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS   // context
                )
            );

            CLEAR = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_clear").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS   // context
                )
            );

            KEYS = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_keys").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,  // context
                    ValueLayout.ADDRESS   // out_json
                )
            );

            FREE_BYTES = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_free_bytes").orElseThrow(),
                FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS,  // ptr
                    ValueLayout.JAVA_LONG // len
                )
            );

            FREE_STRING = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_free_string").orElseThrow(),
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );

            IS_AVAILABLE = linker.downcallHandle(
                lookup.find("wasmtime4j_panama_wasi_keyvalue_is_available").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );

        } catch (Exception e) {
            throw new ExceptionInInitializerError(
                "Failed to initialize Panama WASI keyvalue bindings: " + e.getMessage());
        }
    }

    /**
     * Creates a new Panama WASI keyvalue store.
     *
     * @throws KeyValueException if creation fails
     */
    public PanamaWasiKeyValue() throws KeyValueException {
        this.arena = Arena.ofShared();
        try {
            MemorySegment handle = (MemorySegment) CREATE_CONTEXT.invokeExact();
            if (handle.equals(MemorySegment.NULL)) {
                throw new KeyValueException("Failed to create keyvalue context");
            }
            this.contextHandle = handle.address();
            LOGGER.log(Level.FINE, "Created Panama WASI keyvalue context: {0}", contextHandle);
        } catch (Throwable t) {
            arena.close();
            throw new KeyValueException("Failed to create keyvalue context", t);
        }
    }

    /**
     * Checks if WASI keyvalue support is available.
     *
     * @return true if available
     */
    public static boolean isAvailable() {
        try {
            int result = (int) IS_AVAILABLE.invokeExact();
            return result == 1;
        } catch (Throwable t) {
            return false;
        }
    }

    private void ensureOpen() throws KeyValueException {
        if (closed) {
            throw new KeyValueException("Keyvalue store is closed");
        }
    }

    private MemorySegment toNativeString(final String str) {
        byte[] bytes = (str + "\0").getBytes(StandardCharsets.UTF_8);
        MemorySegment segment = arena.allocate(bytes.length);
        segment.copyFrom(MemorySegment.ofArray(bytes));
        return segment;
    }

    @Override
    public Optional<byte[]> get(final String key) throws KeyValueException {
        ensureOpen();
        try {
            MemorySegment keySegment = toNativeString(key);
            MemorySegment outValue = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment outLen = arena.allocate(ValueLayout.JAVA_LONG);

            int result = (int) GET.invokeExact(
                MemorySegment.ofAddress(contextHandle),
                keySegment,
                outValue,
                outLen
            );

            if (result == 1) {
                // Key not found
                return Optional.empty();
            } else if (result == 0) {
                // Key found
                MemorySegment valuePtr = outValue.get(ValueLayout.ADDRESS, 0);
                long valueLen = outLen.get(ValueLayout.JAVA_LONG, 0);

                if (valuePtr.equals(MemorySegment.NULL) || valueLen == 0) {
                    return Optional.of(new byte[0]);
                }

                byte[] value = valuePtr.reinterpret(valueLen).toArray(ValueLayout.JAVA_BYTE);

                // Free native memory
                FREE_BYTES.invokeExact(valuePtr, valueLen);

                return Optional.of(value);
            } else {
                throw new KeyValueException("Failed to get value for key: " + key);
            }
        } catch (KeyValueException e) {
            throw e;
        } catch (Throwable t) {
            throw new KeyValueException("Failed to get value for key: " + key, t);
        }
    }

    @Override
    public Optional<KeyValueEntry> getEntry(final String key) throws KeyValueException {
        return get(key).map(value -> KeyValueEntry.builder(key, value).build());
    }

    @Override
    public void set(final String key, final byte[] value) throws KeyValueException {
        ensureOpen();
        try {
            MemorySegment keySegment = toNativeString(key);
            MemorySegment valueSegment = value.length > 0
                ? arena.allocate(value.length).copyFrom(MemorySegment.ofArray(value))
                : MemorySegment.NULL;

            int result = (int) SET.invokeExact(
                MemorySegment.ofAddress(contextHandle),
                keySegment,
                valueSegment,
                (long) value.length
            );

            if (result != 0) {
                throw new KeyValueException("Failed to set value for key: " + key);
            }
        } catch (KeyValueException e) {
            throw e;
        } catch (Throwable t) {
            throw new KeyValueException("Failed to set value for key: " + key, t);
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
        try {
            MemorySegment keySegment = toNativeString(key);

            int result = (int) DELETE.invokeExact(
                MemorySegment.ofAddress(contextHandle),
                keySegment
            );

            return result == 1;
        } catch (Throwable t) {
            throw new KeyValueException("Failed to delete key: " + key, t);
        }
    }

    @Override
    public boolean exists(final String key) throws KeyValueException {
        ensureOpen();
        try {
            MemorySegment keySegment = toNativeString(key);

            int result = (int) EXISTS.invokeExact(
                MemorySegment.ofAddress(contextHandle),
                keySegment
            );

            return result == 1;
        } catch (Throwable t) {
            throw new KeyValueException("Failed to check key existence: " + key, t);
        }
    }

    @Override
    public Set<String> keys(final String pattern) throws KeyValueException {
        // Pattern matching not supported, return all keys
        return keys();
    }

    @Override
    public Set<String> keys() throws KeyValueException {
        ensureOpen();
        try {
            MemorySegment outJson = arena.allocate(ValueLayout.ADDRESS);

            int result = (int) KEYS.invokeExact(
                MemorySegment.ofAddress(contextHandle),
                outJson
            );

            if (result != 0) {
                throw new KeyValueException("Failed to get keys");
            }

            MemorySegment jsonPtr = outJson.get(ValueLayout.ADDRESS, 0);
            if (jsonPtr.equals(MemorySegment.NULL)) {
                return new HashSet<>();
            }

            // Read the null-terminated string
            String json = jsonPtr.reinterpret(Long.MAX_VALUE).getString(0);

            // Free native memory
            FREE_STRING.invokeExact(jsonPtr);

            // Parse JSON array (simple implementation)
            Set<String> keySet = new HashSet<>();
            if (json != null && json.startsWith("[") && json.endsWith("]")) {
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
        } catch (KeyValueException e) {
            throw e;
        } catch (Throwable t) {
            throw new KeyValueException("Failed to get keys", t);
        }
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
        if (current.isPresent() && java.util.Arrays.equals(current.get(), expectedValue)) {
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
        try {
            MemorySegment keySegment = toNativeString(key);
            MemorySegment outValue = arena.allocate(ValueLayout.JAVA_LONG);

            int result = (int) INCREMENT.invokeExact(
                MemorySegment.ofAddress(contextHandle),
                keySegment,
                delta,
                outValue
            );

            if (result != 0) {
                throw new KeyValueException("Failed to increment key: " + key);
            }

            return outValue.get(ValueLayout.JAVA_LONG, 0);
        } catch (KeyValueException e) {
            throw e;
        } catch (Throwable t) {
            throw new KeyValueException("Failed to increment key: " + key, t);
        }
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
        try {
            long result = (long) SIZE.invokeExact(
                MemorySegment.ofAddress(contextHandle)
            );
            if (result < 0) {
                throw new KeyValueException("Failed to get size");
            }
            return result;
        } catch (KeyValueException e) {
            throw e;
        } catch (Throwable t) {
            throw new KeyValueException("Failed to get size", t);
        }
    }

    @Override
    public boolean isEmpty() throws KeyValueException {
        return size() == 0;
    }

    @Override
    public void clear() throws KeyValueException {
        ensureOpen();
        try {
            int result = (int) CLEAR.invokeExact(
                MemorySegment.ofAddress(contextHandle)
            );
            if (result != 0) {
                throw new KeyValueException("Failed to clear store");
            }
        } catch (KeyValueException e) {
            throw e;
        } catch (Throwable t) {
            throw new KeyValueException("Failed to clear store", t);
        }
    }

    @Override
    public void close() throws KeyValueException {
        if (closed) {
            return;
        }
        closed = true;

        try {
            if (contextHandle != 0) {
                DESTROY_CONTEXT.invokeExact(MemorySegment.ofAddress(contextHandle));
                contextHandle = 0;
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error destroying keyvalue context", t);
        } finally {
            arena.close();
        }
    }
}
