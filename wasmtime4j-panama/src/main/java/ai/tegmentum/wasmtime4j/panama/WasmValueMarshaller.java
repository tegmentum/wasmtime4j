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
package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ExternRef;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Shared utility for marshalling {@link WasmValue} instances to and from the Wasmtime native tagged
 * union layout.
 *
 * <p>The native layout is 20 bytes per value: a 4-byte integer tag followed by a 16-byte union
 * holding the value. The value at offset+4 may not be naturally aligned, so unaligned layouts are
 * used for 8-byte reads/writes.
 *
 * <p>Tag values: 0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FUNCREF, 6=EXTERNREF, 7=CONTREF.
 *
 * @since 1.0.0
 */
final class WasmValueMarshaller {

  /** Size in bytes of each WasmValue in the native tagged union layout. */
  static final int WASM_VALUE_SIZE = 20;

  /** Offset of the value data within each WasmValue slot. */
  private static final int VALUE_OFFSET = 4;

  private WasmValueMarshaller() {}

  /**
   * Unmarshals a WasmValue from native memory.
   *
   * @param ptr pointer to the WasmValue array
   * @param index index in the array
   * @param externRefLookup optional function to look up ExternRef by ID (may be null)
   * @return the unmarshalled WasmValue
   */
  static WasmValue unmarshalWasmValue(
      final MemorySegment ptr,
      final int index,
      final Function<Long, ExternRef<?>> externRefLookup) {
    final long offset = (long) index * WASM_VALUE_SIZE;
    final int tag = ptr.get(ValueLayout.JAVA_INT, offset);

    switch (tag) {
      case 0: // I32
        return WasmValue.i32(ptr.get(ValueLayout.JAVA_INT, offset + VALUE_OFFSET));

      case 1: // I64
        return WasmValue.i64(ptr.get(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET));

      case 2: // F32
        return WasmValue.f32(ptr.get(ValueLayout.JAVA_FLOAT, offset + VALUE_OFFSET));

      case 3: // F64
        return WasmValue.f64(ptr.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset + VALUE_OFFSET));

      case 4: // V128
        final byte[] v128Bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
          v128Bytes[i] = ptr.get(ValueLayout.JAVA_BYTE, offset + VALUE_OFFSET + i);
        }
        return WasmValue.v128(v128Bytes);

      case 5: // FUNCREF
        final long funcId = ptr.get(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET);
        if (funcId == 0L) {
          return WasmValue.funcref((Object) null);
        }
        final FunctionReference funcRef = PanamaFunctionReference.getFunctionReferenceById(funcId);
        if (funcRef != null) {
          return WasmValue.funcref(funcRef);
        }
        return WasmValue.funcref(funcId);

      case 6: // EXTERNREF
        final long externId = ptr.get(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET);
        if (externId == 0L) {
          return WasmValue.externref((Object) null);
        }
        if (externRefLookup != null) {
          final ExternRef<?> externRef = externRefLookup.apply(externId);
          if (externRef != null) {
            return WasmValue.externref(externRef.get());
          }
        }
        return WasmValue.externref(externId);

      case 7: // CONTREF
        return WasmValue.nullContRef();

      default:
        throw new IllegalArgumentException("Unknown WasmValue tag: " + tag);
    }
  }

  /**
   * Marshals a WasmValue to native memory.
   *
   * @param value the WasmValue to marshal
   * @param ptr pointer to the WasmValue array
   * @param index index in the array
   * @param externRefRegistrar optional callback to register ExternRef by ID (may be null)
   */
  static void marshalWasmValue(
      final WasmValue value,
      final MemorySegment ptr,
      final int index,
      final BiConsumer<Long, ExternRef<?>> externRefRegistrar) {
    final long offset = (long) index * WASM_VALUE_SIZE;

    switch (value.getType()) {
      case I32:
        ptr.set(ValueLayout.JAVA_INT, offset, 0);
        ptr.set(ValueLayout.JAVA_INT, offset + VALUE_OFFSET, value.asInt());
        break;

      case I64:
        ptr.set(ValueLayout.JAVA_INT, offset, 1);
        ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET, value.asLong());
        break;

      case F32:
        ptr.set(ValueLayout.JAVA_INT, offset, 2);
        ptr.set(ValueLayout.JAVA_FLOAT, offset + VALUE_OFFSET, value.asFloat());
        break;

      case F64:
        ptr.set(ValueLayout.JAVA_INT, offset, 3);
        ptr.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset + VALUE_OFFSET, value.asDouble());
        break;

      case V128:
        ptr.set(ValueLayout.JAVA_INT, offset, 4);
        final byte[] v128Bytes = value.asV128();
        for (int i = 0; i < 16; i++) {
          ptr.set(ValueLayout.JAVA_BYTE, offset + VALUE_OFFSET + i, v128Bytes[i]);
        }
        break;

      case FUNCREF:
        ptr.set(ValueLayout.JAVA_INT, offset, 5);
        final Object funcValue = value.getValue();
        if (funcValue == null) {
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET, 0L);
        } else if (funcValue instanceof FunctionReference) {
          ptr.set(
              ValueLayout.JAVA_LONG_UNALIGNED,
              offset + VALUE_OFFSET,
              ((FunctionReference) funcValue).getId());
        } else if (funcValue instanceof Long) {
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET, (Long) funcValue);
        } else {
          throw new IllegalArgumentException(
              "FUNCREF value must be FunctionReference or Long, got: " + funcValue.getClass());
        }
        break;

      case EXTERNREF:
        ptr.set(ValueLayout.JAVA_INT, offset, 6);
        final Object externValue = value.getValue();
        if (externValue == null) {
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET, 0L);
        } else if (externValue instanceof ExternRef) {
          final ExternRef<?> externRef = (ExternRef<?>) externValue;
          final long externId = externRef.getId();
          if (externRefRegistrar != null) {
            externRefRegistrar.accept(externId, externRef);
          }
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET, externId);
        } else if (externValue instanceof Long) {
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET, (Long) externValue);
        } else {
          final ExternRef<Object> newRef = ExternRef.of(externValue);
          final long externId = newRef.getId();
          if (externRefRegistrar != null) {
            externRefRegistrar.accept(externId, newRef);
          }
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET, externId);
        }
        break;

      case CONTREF:
        ptr.set(ValueLayout.JAVA_INT, offset, 7);
        ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + VALUE_OFFSET, 0L);
        break;

      default:
        throw new IllegalArgumentException("Unsupported WasmValue type: " + value.getType());
    }
  }
}
