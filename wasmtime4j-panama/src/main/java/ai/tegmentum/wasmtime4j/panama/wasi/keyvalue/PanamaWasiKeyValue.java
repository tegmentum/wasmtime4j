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

import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueEntry;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueException;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.WasiKeyValue;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WASI keyvalue interface.
 *
 * <p>This implementation uses Java's Foreign Function and Memory API (Panama) to call native WASI
 * keyvalue functions.
 *
 * @since 1.0.0
 */
public final class PanamaWasiKeyValue implements WasiKeyValue {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiKeyValue.class.getName());

  /** Native context handle. */
  private volatile long contextHandle;

  /** Arena for memory management. */
  private final Arena arena;

  /** Resource lifecycle handle. */
  private final NativeResourceHandle resourceHandle;

  // Native method handles
  private static final MethodHandle CREATE_CONTEXT;
  private static final MethodHandle DESTROY_CONTEXT;
  private static final MethodHandle GET;
  private static final MethodHandle SET;
  private static final MethodHandle DELETE;
  private static final MethodHandle EXISTS;
  private static final MethodHandle KEYS;
  private static final MethodHandle FREE_BYTES;
  private static final MethodHandle FREE_STRING;
  private static final MethodHandle IS_AVAILABLE;

  static {
    try {
      Linker linker = Linker.nativeLinker();
      SymbolLookup lookup = SymbolLookup.loaderLookup();

      CREATE_CONTEXT =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_context_create").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.ADDRESS));

      DESTROY_CONTEXT =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_context_destroy").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      GET =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_get").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // context
                  ValueLayout.ADDRESS, // key
                  ValueLayout.ADDRESS, // out_value
                  ValueLayout.ADDRESS // out_value_len
                  ));

      SET =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_set").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // context
                  ValueLayout.ADDRESS, // key
                  ValueLayout.ADDRESS, // value
                  ValueLayout.JAVA_LONG // value_len
                  ));

      DELETE =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_delete").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // context
                  ValueLayout.ADDRESS // key
                  ));

      EXISTS =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_exists").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // context
                  ValueLayout.ADDRESS // key
                  ));

      KEYS =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_keys").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // context
                  ValueLayout.ADDRESS // out_json
                  ));

      FREE_BYTES =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_free_bytes").orElseThrow(),
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS, // ptr
                  ValueLayout.JAVA_LONG // len
                  ));

      FREE_STRING =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_free_string").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      IS_AVAILABLE =
          linker.downcallHandle(
              lookup.find("wasmtime4j_panama_wasi_keyvalue_is_available").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.JAVA_INT));

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

      // Capture handle values for safety net (must not capture 'this')
      final long safetyCtxHandle = this.contextHandle;
      final Arena safetyArena = this.arena;
      this.resourceHandle =
          new NativeResourceHandle(
              "PanamaWasiKeyValue",
              () -> {
                try {
                  if (safetyCtxHandle != 0) {
                    DESTROY_CONTEXT.invokeExact(MemorySegment.ofAddress(safetyCtxHandle));
                  }
                } catch (final Throwable t) {
                  throw new Exception("Error destroying keyvalue context: " + safetyCtxHandle, t);
                } finally {
                  safetyArena.close();
                }
              },
              this,
              () -> {
                try {
                  if (safetyCtxHandle != 0) {
                    DESTROY_CONTEXT.invokeExact(MemorySegment.ofAddress(safetyCtxHandle));
                  }
                } catch (final Throwable t) {
                  LOGGER.warning(
                      "Safety net failed to destroy keyvalue context "
                          + safetyCtxHandle
                          + ": "
                          + t);
                } finally {
                  safetyArena.close();
                }
              });

      LOGGER.log(Level.FINE, "Created Panama WASI keyvalue context: {0}", contextHandle);
    } catch (KeyValueException e) {
      arena.close();
      throw e;
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
    if (resourceHandle.isClosed()) {
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

      int result =
          (int)
              GET.invokeExact(MemorySegment.ofAddress(contextHandle), keySegment, outValue, outLen);

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
      MemorySegment valueSegment =
          value.length > 0
              ? arena.allocate(value.length).copyFrom(MemorySegment.ofArray(value))
              : MemorySegment.NULL;

      int result =
          (int)
              SET.invokeExact(
                  MemorySegment.ofAddress(contextHandle),
                  keySegment,
                  valueSegment,
                  (long) value.length);

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
  public boolean delete(final String key) throws KeyValueException {
    ensureOpen();
    try {
      MemorySegment keySegment = toNativeString(key);

      int result = (int) DELETE.invokeExact(MemorySegment.ofAddress(contextHandle), keySegment);

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

      int result = (int) EXISTS.invokeExact(MemorySegment.ofAddress(contextHandle), keySegment);

      return result == 1;
    } catch (Throwable t) {
      throw new KeyValueException("Failed to check key existence: " + key, t);
    }
  }

  @Override
  public Set<String> keys() throws KeyValueException {
    ensureOpen();
    try {
      MemorySegment outJson = arena.allocate(ValueLayout.ADDRESS);

      int result = (int) KEYS.invokeExact(MemorySegment.ofAddress(contextHandle), outJson);

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
  public void close() throws KeyValueException {
    resourceHandle.close();
  }
}
