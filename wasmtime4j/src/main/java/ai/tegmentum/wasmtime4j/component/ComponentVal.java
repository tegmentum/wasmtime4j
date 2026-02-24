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

package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a Component Model value that can be passed to and from WebAssembly components.
 *
 * <p>The Component Model defines a rich type system that extends beyond the basic WebAssembly value
 * types. ComponentVal supports all Component Model types including:
 *
 * <ul>
 *   <li>Primitive types: bool, s8, s16, s32, s64, u8, u16, u32, u64, f32, f64, char, string
 *   <li>Compound types: list, record, tuple, variant, enum, option, result, flags
 *   <li>Handle types: own, borrow (for resources)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create primitive values
 * ComponentVal str = ComponentVal.string("hello");
 * ComponentVal num = ComponentVal.s32(42);
 * ComponentVal flag = ComponentVal.bool(true);
 *
 * // Create compound values
 * ComponentVal list = ComponentVal.list(ComponentVal.s32(1), ComponentVal.s32(2));
 * ComponentVal record = ComponentVal.record(Map.of("name", ComponentVal.string("Alice")));
 * ComponentVal option = ComponentVal.some(ComponentVal.string("value"));
 * ComponentVal result = ComponentVal.ok(ComponentVal.s32(100));
 *
 * // Access values
 * String s = str.asString();
 * int n = num.asS32();
 * Optional<ComponentVal> inner = option.asSome();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentVal {

  /**
   * Gets the Component Model type of this value.
   *
   * @return the component type
   */
  ComponentType getType();

  // ========== Type checking methods ==========

  /**
   * Checks if this value is a boolean.
   *
   * @return true if this is a boolean value
   */
  boolean isBool();

  /**
   * Checks if this value is a signed 8-bit integer.
   *
   * @return true if this is an s8 value
   */
  boolean isS8();

  /**
   * Checks if this value is a signed 16-bit integer.
   *
   * @return true if this is an s16 value
   */
  boolean isS16();

  /**
   * Checks if this value is a signed 32-bit integer.
   *
   * @return true if this is an s32 value
   */
  boolean isS32();

  /**
   * Checks if this value is a signed 64-bit integer.
   *
   * @return true if this is an s64 value
   */
  boolean isS64();

  /**
   * Checks if this value is an unsigned 8-bit integer.
   *
   * @return true if this is a u8 value
   */
  boolean isU8();

  /**
   * Checks if this value is an unsigned 16-bit integer.
   *
   * @return true if this is a u16 value
   */
  boolean isU16();

  /**
   * Checks if this value is an unsigned 32-bit integer.
   *
   * @return true if this is a u32 value
   */
  boolean isU32();

  /**
   * Checks if this value is an unsigned 64-bit integer.
   *
   * @return true if this is a u64 value
   */
  boolean isU64();

  /**
   * Checks if this value is a 32-bit float.
   *
   * @return true if this is an f32 value
   */
  boolean isF32();

  /**
   * Checks if this value is a 64-bit float.
   *
   * @return true if this is an f64 value
   */
  boolean isF64();

  /**
   * Checks if this value is a Unicode character.
   *
   * @return true if this is a char value
   */
  boolean isChar();

  /**
   * Checks if this value is a string.
   *
   * @return true if this is a string value
   */
  boolean isString();

  /**
   * Checks if this value is a list.
   *
   * @return true if this is a list value
   */
  boolean isList();

  /**
   * Checks if this value is a record.
   *
   * @return true if this is a record value
   */
  boolean isRecord();

  /**
   * Checks if this value is a tuple.
   *
   * @return true if this is a tuple value
   */
  boolean isTuple();

  /**
   * Checks if this value is a variant.
   *
   * @return true if this is a variant value
   */
  boolean isVariant();

  /**
   * Checks if this value is an enum.
   *
   * @return true if this is an enum value
   */
  boolean isEnum();

  /**
   * Checks if this value is an option.
   *
   * @return true if this is an option value
   */
  boolean isOption();

  /**
   * Checks if this value is a result.
   *
   * @return true if this is a result value
   */
  boolean isResult();

  /**
   * Checks if this value is a flags value.
   *
   * @return true if this is a flags value
   */
  boolean isFlags();

  /**
   * Checks if this value is a resource handle (own or borrow).
   *
   * @return true if this is a resource handle
   */
  boolean isResource();

  /**
   * Checks if this value is a future handle (component-model-async).
   *
   * @return true if this is a future handle
   */
  boolean isFuture();

  /**
   * Checks if this value is a stream handle (component-model-async).
   *
   * @return true if this is a stream handle
   */
  boolean isStream();

  /**
   * Checks if this value is an error context handle (component-model-async).
   *
   * @return true if this is an error context handle
   */
  boolean isErrorContext();

  // ========== Value extraction methods ==========

  /**
   * Gets this value as a boolean.
   *
   * @return the boolean value
   * @throws IllegalStateException if this is not a boolean value
   */
  boolean asBool();

  /**
   * Gets this value as a signed 8-bit integer.
   *
   * @return the s8 value
   * @throws IllegalStateException if this is not an s8 value
   */
  byte asS8();

  /**
   * Gets this value as a signed 16-bit integer.
   *
   * @return the s16 value
   * @throws IllegalStateException if this is not an s16 value
   */
  short asS16();

  /**
   * Gets this value as a signed 32-bit integer.
   *
   * @return the s32 value
   * @throws IllegalStateException if this is not an s32 value
   */
  int asS32();

  /**
   * Gets this value as a signed 64-bit integer.
   *
   * @return the s64 value
   * @throws IllegalStateException if this is not an s64 value
   */
  long asS64();

  /**
   * Gets this value as an unsigned 8-bit integer (returned as short to preserve unsigned range).
   *
   * @return the u8 value as a short (0-255)
   * @throws IllegalStateException if this is not a u8 value
   */
  short asU8();

  /**
   * Gets this value as an unsigned 16-bit integer (returned as int to preserve unsigned range).
   *
   * @return the u16 value as an int (0-65535)
   * @throws IllegalStateException if this is not a u16 value
   */
  int asU16();

  /**
   * Gets this value as an unsigned 32-bit integer (returned as long to preserve unsigned range).
   *
   * @return the u32 value as a long (0-4294967295)
   * @throws IllegalStateException if this is not a u32 value
   */
  long asU32();

  /**
   * Gets this value as an unsigned 64-bit integer.
   *
   * <p>Note: Java does not have an unsigned 64-bit type. The returned long may be negative if the
   * value exceeds Long.MAX_VALUE. Use {@link Long#toUnsignedString(long)} for correct display.
   *
   * @return the u64 value
   * @throws IllegalStateException if this is not a u64 value
   */
  long asU64();

  /**
   * Gets this value as a 32-bit float.
   *
   * @return the f32 value
   * @throws IllegalStateException if this is not an f32 value
   */
  float asF32();

  /**
   * Gets this value as a 64-bit float.
   *
   * @return the f64 value
   * @throws IllegalStateException if this is not an f64 value
   */
  double asF64();

  /**
   * Gets this value as a Unicode character.
   *
   * @return the char value
   * @throws IllegalStateException if this is not a char value
   */
  char asChar();

  /**
   * Gets this value as a string.
   *
   * @return the string value
   * @throws IllegalStateException if this is not a string value
   */
  String asString();

  /**
   * Gets this value as a list.
   *
   * @return the list elements
   * @throws IllegalStateException if this is not a list value
   */
  List<ComponentVal> asList();

  /**
   * Gets this value as a record.
   *
   * @return the record fields as a map of field name to value
   * @throws IllegalStateException if this is not a record value
   */
  Map<String, ComponentVal> asRecord();

  /**
   * Gets this value as a tuple.
   *
   * @return the tuple elements
   * @throws IllegalStateException if this is not a tuple value
   */
  List<ComponentVal> asTuple();

  /**
   * Gets this value as a variant.
   *
   * @return the variant case name and optional payload
   * @throws IllegalStateException if this is not a variant value
   */
  ComponentVariant asVariant();

  /**
   * Gets this value as an enum.
   *
   * @return the enum case name
   * @throws IllegalStateException if this is not an enum value
   */
  String asEnum();

  /**
   * Gets this value as an option, returning the inner value if present.
   *
   * @return the optional inner value
   * @throws IllegalStateException if this is not an option value
   */
  Optional<ComponentVal> asSome();

  /**
   * Gets this value as a result.
   *
   * @return the result value
   * @throws IllegalStateException if this is not a result value
   */
  ComponentResult asResult();

  /**
   * Gets this value as flags.
   *
   * @return the set of enabled flag names
   * @throws IllegalStateException if this is not a flags value
   */
  java.util.Set<String> asFlags();

  /**
   * Gets this value as a resource handle.
   *
   * @return the resource handle
   * @throws IllegalStateException if this is not a resource handle
   */
  ComponentResourceHandle asResource();

  /**
   * Gets this value as a future handle.
   *
   * <p>Future handles are opaque identifiers for in-flight async operations in the
   * Component Model async extension. They can be passed back to component functions
   * that expect future parameters.
   *
   * @return the opaque future handle ID
   * @throws IllegalStateException if this is not a future value
   */
  long asFutureHandle();

  /**
   * Gets this value as a stream handle.
   *
   * <p>Stream handles are opaque identifiers for async data streams in the
   * Component Model async extension.
   *
   * @return the opaque stream handle ID
   * @throws IllegalStateException if this is not a stream value
   */
  long asStreamHandle();

  /**
   * Gets this value as an error context handle.
   *
   * <p>Error context handles are opaque identifiers for error information in the
   * Component Model async extension.
   *
   * @return the opaque error context handle ID
   * @throws IllegalStateException if this is not an error context value
   */
  long asErrorContextHandle();

  // ========== Factory methods for primitive types ==========

  /**
   * Creates a boolean component value.
   *
   * @param value the boolean value
   * @return a new ComponentVal
   */
  static ComponentVal bool(final boolean value) {
    return ComponentValFactory.INSTANCE.createBool(value);
  }

  /**
   * Creates a signed 8-bit integer component value.
   *
   * @param value the s8 value
   * @return a new ComponentVal
   */
  static ComponentVal s8(final byte value) {
    return ComponentValFactory.INSTANCE.createS8(value);
  }

  /**
   * Creates a signed 16-bit integer component value.
   *
   * @param value the s16 value
   * @return a new ComponentVal
   */
  static ComponentVal s16(final short value) {
    return ComponentValFactory.INSTANCE.createS16(value);
  }

  /**
   * Creates a signed 32-bit integer component value.
   *
   * @param value the s32 value
   * @return a new ComponentVal
   */
  static ComponentVal s32(final int value) {
    return ComponentValFactory.INSTANCE.createS32(value);
  }

  /**
   * Creates a signed 64-bit integer component value.
   *
   * @param value the s64 value
   * @return a new ComponentVal
   */
  static ComponentVal s64(final long value) {
    return ComponentValFactory.INSTANCE.createS64(value);
  }

  /**
   * Creates an unsigned 8-bit integer component value.
   *
   * @param value the u8 value (0-255)
   * @return a new ComponentVal
   * @throws IllegalArgumentException if value is out of range
   */
  static ComponentVal u8(final short value) {
    return ComponentValFactory.INSTANCE.createU8(value);
  }

  /**
   * Creates an unsigned 16-bit integer component value.
   *
   * @param value the u16 value (0-65535)
   * @return a new ComponentVal
   * @throws IllegalArgumentException if value is out of range
   */
  static ComponentVal u16(final int value) {
    return ComponentValFactory.INSTANCE.createU16(value);
  }

  /**
   * Creates an unsigned 32-bit integer component value.
   *
   * @param value the u32 value (0-4294967295)
   * @return a new ComponentVal
   * @throws IllegalArgumentException if value is out of range
   */
  static ComponentVal u32(final long value) {
    return ComponentValFactory.INSTANCE.createU32(value);
  }

  /**
   * Creates an unsigned 64-bit integer component value.
   *
   * @param value the u64 value
   * @return a new ComponentVal
   */
  static ComponentVal u64(final long value) {
    return ComponentValFactory.INSTANCE.createU64(value);
  }

  /**
   * Creates a 32-bit float component value.
   *
   * @param value the f32 value
   * @return a new ComponentVal
   */
  static ComponentVal f32(final float value) {
    return ComponentValFactory.INSTANCE.createF32(value);
  }

  /**
   * Creates a 64-bit float component value.
   *
   * @param value the f64 value
   * @return a new ComponentVal
   */
  static ComponentVal f64(final double value) {
    return ComponentValFactory.INSTANCE.createF64(value);
  }

  /**
   * Creates a Unicode character component value.
   *
   * @param value the char value
   * @return a new ComponentVal
   */
  static ComponentVal char_(final char value) {
    return ComponentValFactory.INSTANCE.createChar(value);
  }

  /**
   * Creates a string component value.
   *
   * @param value the string value
   * @return a new ComponentVal
   * @throws IllegalArgumentException if value is null
   */
  static ComponentVal string(final String value) {
    return ComponentValFactory.INSTANCE.createString(value);
  }

  // ========== Factory methods for compound types ==========

  /**
   * Creates a list component value.
   *
   * @param elements the list elements
   * @return a new ComponentVal
   */
  static ComponentVal list(final ComponentVal... elements) {
    return ComponentValFactory.INSTANCE.createList(java.util.Arrays.asList(elements));
  }

  /**
   * Creates a list component value from a Java list.
   *
   * @param elements the list elements
   * @return a new ComponentVal
   * @throws IllegalArgumentException if elements is null
   */
  static ComponentVal list(final List<ComponentVal> elements) {
    return ComponentValFactory.INSTANCE.createList(elements);
  }

  /**
   * Creates a record component value.
   *
   * @param fields the record fields as a map of field name to value
   * @return a new ComponentVal
   * @throws IllegalArgumentException if fields is null
   */
  static ComponentVal record(final Map<String, ComponentVal> fields) {
    return ComponentValFactory.INSTANCE.createRecord(fields);
  }

  /**
   * Creates a tuple component value.
   *
   * @param elements the tuple elements
   * @return a new ComponentVal
   */
  static ComponentVal tuple(final ComponentVal... elements) {
    return ComponentValFactory.INSTANCE.createTuple(java.util.Arrays.asList(elements));
  }

  /**
   * Creates a variant component value.
   *
   * @param caseName the variant case name
   * @param payload the optional payload value
   * @return a new ComponentVal
   * @throws IllegalArgumentException if caseName is null
   */
  static ComponentVal variant(final String caseName, final ComponentVal payload) {
    return ComponentValFactory.INSTANCE.createVariant(caseName, payload);
  }

  /**
   * Creates a variant component value without a payload.
   *
   * @param caseName the variant case name
   * @return a new ComponentVal
   * @throws IllegalArgumentException if caseName is null
   */
  static ComponentVal variant(final String caseName) {
    return ComponentValFactory.INSTANCE.createVariant(caseName, null);
  }

  /**
   * Creates an enum component value.
   *
   * @param caseName the enum case name
   * @return a new ComponentVal
   * @throws IllegalArgumentException if caseName is null
   */
  static ComponentVal enum_(final String caseName) {
    return ComponentValFactory.INSTANCE.createEnum(caseName);
  }

  /**
   * Creates a some (present) option component value.
   *
   * @param value the inner value
   * @return a new ComponentVal
   * @throws IllegalArgumentException if value is null
   */
  static ComponentVal some(final ComponentVal value) {
    return ComponentValFactory.INSTANCE.createSome(value);
  }

  /**
   * Creates a none (absent) option component value.
   *
   * @return a new ComponentVal
   */
  static ComponentVal none() {
    return ComponentValFactory.INSTANCE.createNone();
  }

  /**
   * Creates an ok result component value.
   *
   * @param value the success value (may be null for result<_, E>)
   * @return a new ComponentVal
   */
  static ComponentVal ok(final ComponentVal value) {
    return ComponentValFactory.INSTANCE.createOk(value);
  }

  /**
   * Creates an ok result component value with no payload.
   *
   * @return a new ComponentVal
   */
  static ComponentVal ok() {
    return ComponentValFactory.INSTANCE.createOk(null);
  }

  /**
   * Creates an err result component value.
   *
   * @param error the error value (may be null for {@code result<T, _>})
   * @return a new ComponentVal
   */
  static ComponentVal err(final ComponentVal error) {
    return ComponentValFactory.INSTANCE.createErr(error);
  }

  /**
   * Creates an err result component value with no payload.
   *
   * @return a new ComponentVal
   */
  static ComponentVal err() {
    return ComponentValFactory.INSTANCE.createErr(null);
  }

  /**
   * Creates a flags component value.
   *
   * @param enabledFlags the set of enabled flag names
   * @return a new ComponentVal
   * @throws IllegalArgumentException if enabledFlags is null
   */
  static ComponentVal flags(final java.util.Set<String> enabledFlags) {
    return ComponentValFactory.INSTANCE.createFlags(enabledFlags);
  }

  /**
   * Creates a flags component value.
   *
   * @param enabledFlags the enabled flag names
   * @return a new ComponentVal
   */
  static ComponentVal flags(final String... enabledFlags) {
    return ComponentValFactory.INSTANCE.createFlags(
        new java.util.HashSet<>(java.util.Arrays.asList(enabledFlags)));
  }

  /**
   * Creates a future handle component value.
   *
   * @param handle the opaque future handle ID
   * @return a new ComponentVal
   */
  static ComponentVal future(final long handle) {
    return ComponentValFactory.INSTANCE.createFuture(handle);
  }

  /**
   * Creates a stream handle component value.
   *
   * @param handle the opaque stream handle ID
   * @return a new ComponentVal
   */
  static ComponentVal stream(final long handle) {
    return ComponentValFactory.INSTANCE.createStream(handle);
  }

  /**
   * Creates an error context handle component value.
   *
   * @param handle the opaque error context handle ID
   * @return a new ComponentVal
   */
  static ComponentVal errorContext(final long handle) {
    return ComponentValFactory.INSTANCE.createErrorContext(handle);
  }

  // ===== WAVE Serialization =====

  /**
   * Serializes this value to the WAVE (WebAssembly Value Encoding) text format.
   *
   * <p>WAVE is Wasmtime's human-readable text encoding for Component Model values. It produces
   * strings like {@code "hello"}, {@code 42}, {@code some("world")}, {@code {name: "Alice"}}.
   *
   * <p>The default implementation provides a best-effort Java-side serialization. Implementations
   * may override to use native Wasmtime's {@code Val::to_wave()} for exact compatibility.
   *
   * @return the WAVE text representation of this value
   * @since 1.1.0
   */
  default String toWave() {
    final ComponentType type = getType();
    switch (type) {
      case BOOL:
        return String.valueOf(asBool());
      case S8:
        return String.valueOf(asS8());
      case S16:
        return String.valueOf(asS16());
      case S32:
        return String.valueOf(asS32());
      case S64:
        return String.valueOf(asS64());
      case U8:
        return String.valueOf(asU8());
      case U16:
        return String.valueOf(asU16());
      case U32:
        return String.valueOf(asU32());
      case U64:
        return String.valueOf(asU64());
      case F32:
        return String.valueOf(asF32());
      case F64:
        return String.valueOf(asF64());
      case CHAR:
        return "'" + asChar() + "'";
      case STRING:
        return "\"" + asString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
      case OPTION:
        final Optional<ComponentVal> optVal = asSome();
        return optVal.map(v -> "some(" + v.toWave() + ")").orElse("none");
      case LIST:
        final List<ComponentVal> listVal = asList();
        final StringBuilder listBuilder = new StringBuilder("[");
        for (int i = 0; i < listVal.size(); i++) {
          if (i > 0) {
            listBuilder.append(", ");
          }
          listBuilder.append(listVal.get(i).toWave());
        }
        listBuilder.append("]");
        return listBuilder.toString();
      case RECORD:
        final Map<String, ComponentVal> recordVal = asRecord();
        final StringBuilder recBuilder = new StringBuilder("{");
        boolean first = true;
        for (final Map.Entry<String, ComponentVal> entry : recordVal.entrySet()) {
          if (!first) {
            recBuilder.append(", ");
          }
          first = false;
          recBuilder.append(entry.getKey()).append(": ").append(entry.getValue().toWave());
        }
        recBuilder.append("}");
        return recBuilder.toString();
      case TUPLE:
        final List<ComponentVal> tupleVal = asTuple();
        final StringBuilder tupleBuilder = new StringBuilder("(");
        for (int i = 0; i < tupleVal.size(); i++) {
          if (i > 0) {
            tupleBuilder.append(", ");
          }
          tupleBuilder.append(tupleVal.get(i).toWave());
        }
        tupleBuilder.append(")");
        return tupleBuilder.toString();
      case RESULT:
        final ComponentResult resultVal = asResult();
        if (resultVal.isOk()) {
          final Optional<ComponentVal> okInner = resultVal.getOk();
          return okInner.map(v -> "ok(" + v.toWave() + ")").orElse("ok");
        } else {
          final Optional<ComponentVal> errInner = resultVal.getErr();
          return errInner.map(v -> "err(" + v.toWave() + ")").orElse("err");
        }
      default:
        return "<" + type.name().toLowerCase() + ">";
    }
  }

  /**
   * Deserializes a component value from the WAVE (WebAssembly Value Encoding) text format.
   *
   * <p>This is the inverse of {@link #toWave()}. The default implementation throws
   * {@link UnsupportedOperationException} because correct parsing requires type information
   * that is only available through the native Wasmtime {@code Val::from_wave()} implementation.
   *
   * <p>Implementations should override to use native Wasmtime's WAVE parser.
   *
   * @param wave the WAVE text representation to parse
   * @return the parsed ComponentVal
   * @throws UnsupportedOperationException if native WAVE parsing is not available
   * @throws IllegalArgumentException if wave is null
   * @since 1.1.0
   */
  static ComponentVal fromWave(final String wave) {
    if (wave == null) {
      throw new IllegalArgumentException("WAVE string cannot be null");
    }
    throw new UnsupportedOperationException(
        "WAVE parsing requires native Wasmtime support with type context; "
            + "use the native runtime's WAVE parser instead");
  }
}
