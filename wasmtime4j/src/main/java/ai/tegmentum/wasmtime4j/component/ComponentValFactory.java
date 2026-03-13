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
package ai.tegmentum.wasmtime4j.component;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Factory for creating Component Model values.
 *
 * <p>This factory is used by the {@link ComponentVal} static factory methods. The actual
 * implementation is provided by the runtime (JNI or Panama) via service provider interface.
 *
 * @since 1.0.0
 */
public abstract class ComponentValFactory {

  /** The singleton instance of the factory. */
  public static final ComponentValFactory INSTANCE = loadFactory();

  private static ComponentValFactory loadFactory() {
    // Try to load from ServiceLoader first
    final ServiceLoader<ComponentValFactory> loader = ServiceLoader.load(ComponentValFactory.class);

    final Iterator<ComponentValFactory> iterator = loader.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }

    // Fall back to default implementation
    return new DefaultImpl();
  }

  /** Creates a boolean component value. */
  public abstract ComponentVal createBool(boolean value);

  /** Creates a signed 8-bit integer component value. */
  public abstract ComponentVal createS8(byte value);

  /** Creates a signed 16-bit integer component value. */
  public abstract ComponentVal createS16(short value);

  /** Creates a signed 32-bit integer component value. */
  public abstract ComponentVal createS32(int value);

  /** Creates a signed 64-bit integer component value. */
  public abstract ComponentVal createS64(long value);

  /** Creates an unsigned 8-bit integer component value. */
  public abstract ComponentVal createU8(short value);

  /** Creates an unsigned 16-bit integer component value. */
  public abstract ComponentVal createU16(int value);

  /** Creates an unsigned 32-bit integer component value. */
  public abstract ComponentVal createU32(long value);

  /** Creates an unsigned 64-bit integer component value. */
  public abstract ComponentVal createU64(long value);

  /** Creates a 32-bit float component value. */
  public abstract ComponentVal createF32(float value);

  /** Creates a 64-bit float component value. */
  public abstract ComponentVal createF64(double value);

  /** Creates a Unicode character component value. */
  public abstract ComponentVal createChar(char value);

  /** Creates a string component value. */
  public abstract ComponentVal createString(String value);

  /** Creates a list component value. */
  public abstract ComponentVal createList(List<ComponentVal> elements);

  /** Creates a record component value. */
  public abstract ComponentVal createRecord(Map<String, ComponentVal> fields);

  /** Creates a tuple component value. */
  public abstract ComponentVal createTuple(List<ComponentVal> elements);

  /** Creates a variant component value. */
  public abstract ComponentVal createVariant(String caseName, ComponentVal payload);

  /** Creates an enum component value. */
  public abstract ComponentVal createEnum(String caseName);

  /** Creates a some (present) option component value. */
  public abstract ComponentVal createSome(ComponentVal value);

  /** Creates a none (absent) option component value. */
  public abstract ComponentVal createNone();

  /** Creates an ok result component value. */
  public abstract ComponentVal createOk(ComponentVal value);

  /** Creates an err result component value. */
  public abstract ComponentVal createErr(ComponentVal error);

  /** Creates a flags component value. */
  public abstract ComponentVal createFlags(Set<String> enabledFlags);

  /** Creates a future handle component value. */
  public abstract ComponentVal createFuture(long handle);

  /** Creates a stream handle component value. */
  public abstract ComponentVal createStream(long handle);

  /** Creates an error context handle component value. */
  public abstract ComponentVal createErrorContext(long handle);

  /** Creates an own resource component value wrapping a resource handle. */
  public abstract ComponentVal createOwn(ComponentResourceHandle handle);

  /** Creates a borrow resource component value wrapping a resource handle. */
  public abstract ComponentVal createBorrow(ComponentResourceHandle handle);

  /**
   * Creates a {@code list<u8>} component value efficiently from a byte array.
   *
   * <p>The default implementation wraps the byte array to avoid per-element boxing.
   *
   * @param bytes the byte array
   * @return a new ComponentVal of type list containing u8 elements
   */
  public ComponentVal createListU8(final byte[] bytes) {
    return new ByteArrayListVal(bytes);
  }

  /** Default implementation of ComponentValFactory using simple Java objects. */
  static final class DefaultImpl extends ComponentValFactory {

    @Override
    public ComponentVal createBool(final boolean value) {
      return new SimpleVal(ComponentType.BOOL, value);
    }

    @Override
    public ComponentVal createS8(final byte value) {
      return new SimpleVal(ComponentType.S8, value);
    }

    @Override
    public ComponentVal createS16(final short value) {
      return new SimpleVal(ComponentType.S16, value);
    }

    @Override
    public ComponentVal createS32(final int value) {
      return new SimpleVal(ComponentType.S32, value);
    }

    @Override
    public ComponentVal createS64(final long value) {
      return new SimpleVal(ComponentType.S64, value);
    }

    @Override
    public ComponentVal createU8(final short value) {
      if (value < 0 || value > 255) {
        throw new IllegalArgumentException("u8 value out of range: " + value);
      }
      return new SimpleVal(ComponentType.U8, value);
    }

    @Override
    public ComponentVal createU16(final int value) {
      if (value < 0 || value > 65535) {
        throw new IllegalArgumentException("u16 value out of range: " + value);
      }
      return new SimpleVal(ComponentType.U16, value);
    }

    @Override
    public ComponentVal createU32(final long value) {
      if (value < 0 || value > 4294967295L) {
        throw new IllegalArgumentException("u32 value out of range: " + value);
      }
      return new SimpleVal(ComponentType.U32, value);
    }

    @Override
    public ComponentVal createU64(final long value) {
      return new SimpleVal(ComponentType.U64, value);
    }

    @Override
    public ComponentVal createF32(final float value) {
      return new SimpleVal(ComponentType.F32, value);
    }

    @Override
    public ComponentVal createF64(final double value) {
      return new SimpleVal(ComponentType.F64, value);
    }

    @Override
    public ComponentVal createChar(final char value) {
      return new SimpleVal(ComponentType.CHAR, value);
    }

    @Override
    public ComponentVal createString(final String value) {
      if (value == null) {
        throw new IllegalArgumentException("String value cannot be null");
      }
      return new SimpleVal(ComponentType.STRING, value);
    }

    @Override
    public ComponentVal createList(final List<ComponentVal> elements) {
      if (elements == null) {
        throw new IllegalArgumentException("List elements cannot be null");
      }
      return new SimpleVal(ComponentType.LIST, Collections.unmodifiableList(new ArrayList<>(elements)));
    }

    @Override
    public ComponentVal createRecord(final Map<String, ComponentVal> fields) {
      if (fields == null) {
        throw new IllegalArgumentException("Record fields cannot be null");
      }
      return new SimpleVal(ComponentType.RECORD, Collections.unmodifiableMap(new HashMap<>(fields)));
    }

    @Override
    public ComponentVal createTuple(final List<ComponentVal> elements) {
      if (elements == null) {
        throw new IllegalArgumentException("Tuple elements cannot be null");
      }
      return new SimpleVal(ComponentType.TUPLE, Collections.unmodifiableList(new ArrayList<>(elements)));
    }

    @Override
    public ComponentVal createVariant(final String caseName, final ComponentVal payload) {
      if (caseName == null) {
        throw new IllegalArgumentException("Variant case name cannot be null");
      }
      return new SimpleVal(ComponentType.VARIANT, ComponentVariant.of(caseName, payload));
    }

    @Override
    public ComponentVal createEnum(final String caseName) {
      if (caseName == null) {
        throw new IllegalArgumentException("Enum case name cannot be null");
      }
      return new SimpleVal(ComponentType.ENUM, caseName);
    }

    @Override
    public ComponentVal createSome(final ComponentVal value) {
      if (value == null) {
        throw new IllegalArgumentException("Option some value cannot be null");
      }
      return new SimpleVal(ComponentType.OPTION, Optional.of(value));
    }

    @Override
    public ComponentVal createNone() {
      return new SimpleVal(ComponentType.OPTION, Optional.empty());
    }

    @Override
    public ComponentVal createOk(final ComponentVal value) {
      return new SimpleVal(ComponentType.RESULT, ComponentResult.ok(value));
    }

    @Override
    public ComponentVal createErr(final ComponentVal error) {
      return new SimpleVal(ComponentType.RESULT, ComponentResult.err(error));
    }

    @Override
    public ComponentVal createFlags(final Set<String> enabledFlags) {
      if (enabledFlags == null) {
        throw new IllegalArgumentException("Flags cannot be null");
      }
      return new SimpleVal(ComponentType.FLAGS, Collections.unmodifiableSet(new HashSet<>(enabledFlags)));
    }

    @Override
    public ComponentVal createFuture(final long handle) {
      return new SimpleVal(ComponentType.FUTURE, handle);
    }

    @Override
    public ComponentVal createStream(final long handle) {
      return new SimpleVal(ComponentType.STREAM, handle);
    }

    @Override
    public ComponentVal createErrorContext(final long handle) {
      return new SimpleVal(ComponentType.ERROR_CONTEXT, handle);
    }

    @Override
    public ComponentVal createOwn(final ComponentResourceHandle handle) {
      if (handle == null) {
        throw new IllegalArgumentException("Resource handle cannot be null");
      }
      return new SimpleVal(ComponentType.OWN, handle);
    }

    @Override
    public ComponentVal createBorrow(final ComponentResourceHandle handle) {
      if (handle == null) {
        throw new IllegalArgumentException("Resource handle cannot be null");
      }
      return new SimpleVal(ComponentType.BORROW, handle);
    }
  }

  /** Simple implementation of ComponentVal for the default factory. */
  @SuppressWarnings("unchecked")
  static final class SimpleVal implements ComponentVal {
    private final ComponentType type;
    private final Object value;

    SimpleVal(final ComponentType type, final Object value) {
      this.type = type;
      this.value = value;
    }

    @Override
    public ComponentType getType() {
      return type;
    }

    @Override
    public boolean isBool() {
      return type == ComponentType.BOOL;
    }

    @Override
    public boolean isS8() {
      return type == ComponentType.S8;
    }

    @Override
    public boolean isS16() {
      return type == ComponentType.S16;
    }

    @Override
    public boolean isS32() {
      return type == ComponentType.S32;
    }

    @Override
    public boolean isS64() {
      return type == ComponentType.S64;
    }

    @Override
    public boolean isU8() {
      return type == ComponentType.U8;
    }

    @Override
    public boolean isU16() {
      return type == ComponentType.U16;
    }

    @Override
    public boolean isU32() {
      return type == ComponentType.U32;
    }

    @Override
    public boolean isU64() {
      return type == ComponentType.U64;
    }

    @Override
    public boolean isF32() {
      return type == ComponentType.F32;
    }

    @Override
    public boolean isF64() {
      return type == ComponentType.F64;
    }

    @Override
    public boolean isChar() {
      return type == ComponentType.CHAR;
    }

    @Override
    public boolean isString() {
      return type == ComponentType.STRING;
    }

    @Override
    public boolean isList() {
      return type == ComponentType.LIST;
    }

    @Override
    public boolean isRecord() {
      return type == ComponentType.RECORD;
    }

    @Override
    public boolean isTuple() {
      return type == ComponentType.TUPLE;
    }

    @Override
    public boolean isVariant() {
      return type == ComponentType.VARIANT;
    }

    @Override
    public boolean isEnum() {
      return type == ComponentType.ENUM;
    }

    @Override
    public boolean isOption() {
      return type == ComponentType.OPTION;
    }

    @Override
    public boolean isResult() {
      return type == ComponentType.RESULT;
    }

    @Override
    public boolean isFlags() {
      return type == ComponentType.FLAGS;
    }

    @Override
    public boolean isResource() {
      return type == ComponentType.OWN || type == ComponentType.BORROW;
    }

    @Override
    public boolean isFuture() {
      return type == ComponentType.FUTURE;
    }

    @Override
    public boolean isStream() {
      return type == ComponentType.STREAM;
    }

    @Override
    public boolean isErrorContext() {
      return type == ComponentType.ERROR_CONTEXT;
    }

    @Override
    public boolean asBool() {
      checkType(ComponentType.BOOL);
      return (Boolean) value;
    }

    @Override
    public byte asS8() {
      checkType(ComponentType.S8);
      return (Byte) value;
    }

    @Override
    public short asS16() {
      checkType(ComponentType.S16);
      return (Short) value;
    }

    @Override
    public int asS32() {
      checkType(ComponentType.S32);
      return (Integer) value;
    }

    @Override
    public long asS64() {
      checkType(ComponentType.S64);
      return (Long) value;
    }

    @Override
    public short asU8() {
      checkType(ComponentType.U8);
      return (Short) value;
    }

    @Override
    public int asU16() {
      checkType(ComponentType.U16);
      return (Integer) value;
    }

    @Override
    public long asU32() {
      checkType(ComponentType.U32);
      return (Long) value;
    }

    @Override
    public long asU64() {
      checkType(ComponentType.U64);
      return (Long) value;
    }

    @Override
    public float asF32() {
      checkType(ComponentType.F32);
      return (Float) value;
    }

    @Override
    public double asF64() {
      checkType(ComponentType.F64);
      return (Double) value;
    }

    @Override
    public char asChar() {
      checkType(ComponentType.CHAR);
      return (Character) value;
    }

    @Override
    public String asString() {
      checkType(ComponentType.STRING);
      return (String) value;
    }

    @Override
    public List<ComponentVal> asList() {
      checkType(ComponentType.LIST);
      return (List<ComponentVal>) value;
    }

    @Override
    public Map<String, ComponentVal> asRecord() {
      checkType(ComponentType.RECORD);
      return (Map<String, ComponentVal>) value;
    }

    @Override
    public List<ComponentVal> asTuple() {
      checkType(ComponentType.TUPLE);
      return (List<ComponentVal>) value;
    }

    @Override
    public ComponentVariant asVariant() {
      checkType(ComponentType.VARIANT);
      return (ComponentVariant) value;
    }

    @Override
    public String asEnum() {
      checkType(ComponentType.ENUM);
      return (String) value;
    }

    @Override
    public Optional<ComponentVal> asSome() {
      checkType(ComponentType.OPTION);
      return (Optional<ComponentVal>) value;
    }

    @Override
    public ComponentResult asResult() {
      checkType(ComponentType.RESULT);
      return (ComponentResult) value;
    }

    @Override
    public Set<String> asFlags() {
      checkType(ComponentType.FLAGS);
      return (Set<String>) value;
    }

    @Override
    public ComponentResourceHandle asResource() {
      if (type != ComponentType.OWN && type != ComponentType.BORROW) {
        throw new IllegalStateException("Not a resource type: " + type);
      }
      return (ComponentResourceHandle) value;
    }

    @Override
    public long asFutureHandle() {
      checkType(ComponentType.FUTURE);
      return (Long) value;
    }

    @Override
    public long asStreamHandle() {
      checkType(ComponentType.STREAM);
      return (Long) value;
    }

    @Override
    public long asErrorContextHandle() {
      checkType(ComponentType.ERROR_CONTEXT);
      return (Long) value;
    }

    private void checkType(final ComponentType expected) {
      if (type != expected) {
        throw new IllegalStateException("Expected " + expected + " but was " + type);
      }
    }

    @Override
    public String toString() {
      return type.name().toLowerCase(Locale.ROOT) + "(" + value + ")";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof SimpleVal)) {
        return false;
      }
      final SimpleVal other = (SimpleVal) obj;
      return type == other.type && java.util.Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(type, value);
    }
  }

  /**
   * Efficient ComponentVal for {@code list<u8>} backed by a byte array.
   *
   * <p>Avoids per-element boxing when creating or accessing large byte lists. Supports both {@link
   * ComponentVal#asList()} (materializes on demand) and {@link ComponentVal#asByteArray()} (zero
   * copy).
   */
  static final class ByteArrayListVal implements ComponentVal {
    private final byte[] data;

    ByteArrayListVal(final byte[] data) {
      this.data = Arrays.copyOf(data, data.length);
    }

    @Override
    public ComponentType getType() {
      return ComponentType.LIST;
    }

    @Override
    public boolean isBool() {
      return false;
    }

    @Override
    public boolean isS8() {
      return false;
    }

    @Override
    public boolean isS16() {
      return false;
    }

    @Override
    public boolean isS32() {
      return false;
    }

    @Override
    public boolean isS64() {
      return false;
    }

    @Override
    public boolean isU8() {
      return false;
    }

    @Override
    public boolean isU16() {
      return false;
    }

    @Override
    public boolean isU32() {
      return false;
    }

    @Override
    public boolean isU64() {
      return false;
    }

    @Override
    public boolean isF32() {
      return false;
    }

    @Override
    public boolean isF64() {
      return false;
    }

    @Override
    public boolean isChar() {
      return false;
    }

    @Override
    public boolean isString() {
      return false;
    }

    @Override
    public boolean isList() {
      return true;
    }

    @Override
    public boolean isRecord() {
      return false;
    }

    @Override
    public boolean isTuple() {
      return false;
    }

    @Override
    public boolean isVariant() {
      return false;
    }

    @Override
    public boolean isEnum() {
      return false;
    }

    @Override
    public boolean isOption() {
      return false;
    }

    @Override
    public boolean isResult() {
      return false;
    }

    @Override
    public boolean isFlags() {
      return false;
    }

    @Override
    public boolean isResource() {
      return false;
    }

    @Override
    public boolean isFuture() {
      return false;
    }

    @Override
    public boolean isStream() {
      return false;
    }

    @Override
    public boolean isErrorContext() {
      return false;
    }

    @Override
    public boolean asBool() {
      throw new IllegalStateException("Expected BOOL but was LIST");
    }

    @Override
    public byte asS8() {
      throw new IllegalStateException("Expected S8 but was LIST");
    }

    @Override
    public short asS16() {
      throw new IllegalStateException("Expected S16 but was LIST");
    }

    @Override
    public int asS32() {
      throw new IllegalStateException("Expected S32 but was LIST");
    }

    @Override
    public long asS64() {
      throw new IllegalStateException("Expected S64 but was LIST");
    }

    @Override
    public short asU8() {
      throw new IllegalStateException("Expected U8 but was LIST");
    }

    @Override
    public int asU16() {
      throw new IllegalStateException("Expected U16 but was LIST");
    }

    @Override
    public long asU32() {
      throw new IllegalStateException("Expected U32 but was LIST");
    }

    @Override
    public long asU64() {
      throw new IllegalStateException("Expected U64 but was LIST");
    }

    @Override
    public float asF32() {
      throw new IllegalStateException("Expected F32 but was LIST");
    }

    @Override
    public double asF64() {
      throw new IllegalStateException("Expected F64 but was LIST");
    }

    @Override
    public char asChar() {
      throw new IllegalStateException("Expected CHAR but was LIST");
    }

    @Override
    public String asString() {
      throw new IllegalStateException("Expected STRING but was LIST");
    }

    @Override
    public List<ComponentVal> asList() {
      return new AbstractList<ComponentVal>() {
        @Override
        public ComponentVal get(final int index) {
          return ComponentVal.u8((short) (data[index] & 0xFF));
        }

        @Override
        public int size() {
          return data.length;
        }
      };
    }

    @Override
    public byte[] asByteArray() {
      return Arrays.copyOf(data, data.length);
    }

    @Override
    public int listSize() {
      return data.length;
    }

    @Override
    public Map<String, ComponentVal> asRecord() {
      throw new IllegalStateException("Expected RECORD but was LIST");
    }

    @Override
    public List<ComponentVal> asTuple() {
      throw new IllegalStateException("Expected TUPLE but was LIST");
    }

    @Override
    public ComponentVariant asVariant() {
      throw new IllegalStateException("Expected VARIANT but was LIST");
    }

    @Override
    public String asEnum() {
      throw new IllegalStateException("Expected ENUM but was LIST");
    }

    @Override
    public Optional<ComponentVal> asSome() {
      throw new IllegalStateException("Expected OPTION but was LIST");
    }

    @Override
    public ComponentResult asResult() {
      throw new IllegalStateException("Expected RESULT but was LIST");
    }

    @Override
    public Set<String> asFlags() {
      throw new IllegalStateException("Expected FLAGS but was LIST");
    }

    @Override
    public ComponentResourceHandle asResource() {
      throw new IllegalStateException("Expected OWN/BORROW but was LIST");
    }

    @Override
    public long asFutureHandle() {
      throw new IllegalStateException("Expected FUTURE but was LIST");
    }

    @Override
    public long asStreamHandle() {
      throw new IllegalStateException("Expected STREAM but was LIST");
    }

    @Override
    public long asErrorContextHandle() {
      throw new IllegalStateException("Expected ERROR_CONTEXT but was LIST");
    }

    @Override
    public String toString() {
      return "list<u8>(length=" + data.length + ")";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ByteArrayListVal)) {
        return false;
      }
      return Arrays.equals(data, ((ByteArrayListVal) obj).data);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(data);
    }
  }
}
