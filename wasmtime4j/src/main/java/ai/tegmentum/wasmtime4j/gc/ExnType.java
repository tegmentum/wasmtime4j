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
package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.TagType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a WebAssembly GC exception type.
 *
 * <p>An ExnType wraps a {@link TagType} and provides field-level access to the exception payload
 * types. Each field corresponds to a parameter type in the underlying tag's function signature.
 *
 * <p>This maps to Wasmtime's {@code ExnType} which provides introspection over the types of values
 * carried by GC exception references.
 *
 * @since 1.1.0
 */
public final class ExnType {

  private final TagType tagType;
  private final List<WasmValueType> fields;
  private final Finality finality;
  private final Engine engine;

  /**
   * Creates a new ExnType from a TagType.
   *
   * @param tagType the tag type describing the exception signature
   * @throws IllegalArgumentException if tagType is null
   */
  public ExnType(final TagType tagType) {
    if (tagType == null) {
      throw new IllegalArgumentException("tagType cannot be null");
    }
    this.tagType = tagType;

    final FunctionType funcType = tagType.getFunctionType();
    final WasmValueType[] paramTypes = funcType.getParamTypes();
    final List<WasmValueType> fieldList = new ArrayList<>(paramTypes.length);
    for (final WasmValueType paramType : paramTypes) {
      fieldList.add(paramType);
    }
    this.fields = Collections.unmodifiableList(fieldList);
    this.finality = Finality.FINAL;
    this.engine = null;
  }

  /**
   * Creates a new ExnType from an engine and a list of field value types.
   *
   * <p>This is the standalone constructor that does not require a TagType, useful for defining
   * exception types directly from their payload types.
   *
   * @param engine the engine this type is associated with
   * @param fieldTypes the payload field value types
   * @throws IllegalArgumentException if engine or fieldTypes is null
   * @since 1.1.0
   */
  public ExnType(final Engine engine, final List<WasmValueType> fieldTypes) {
    if (engine == null) {
      throw new IllegalArgumentException("engine cannot be null");
    }
    if (fieldTypes == null) {
      throw new IllegalArgumentException("fieldTypes cannot be null");
    }
    this.tagType = null;
    this.fields = Collections.unmodifiableList(new ArrayList<>(fieldTypes));
    this.finality = Finality.FINAL;
    this.engine = engine;
  }

  /**
   * Creates a new ExnType with a specified finality, engine, and field types.
   *
   * @param engine the engine this type is associated with
   * @param fieldTypes the payload field value types
   * @param finality the finality of this type
   * @throws IllegalArgumentException if any argument is null
   * @since 1.1.0
   */
  public ExnType(
      final Engine engine, final List<WasmValueType> fieldTypes, final Finality finality) {
    if (engine == null) {
      throw new IllegalArgumentException("engine cannot be null");
    }
    if (fieldTypes == null) {
      throw new IllegalArgumentException("fieldTypes cannot be null");
    }
    if (finality == null) {
      throw new IllegalArgumentException("finality cannot be null");
    }
    this.tagType = null;
    this.fields = Collections.unmodifiableList(new ArrayList<>(fieldTypes));
    this.finality = finality;
    this.engine = engine;
  }

  /**
   * Gets the underlying tag type.
   *
   * <p>May return null if this ExnType was created with the standalone constructor.
   *
   * @return the tag type, or null if created without a tag type
   */
  public TagType tagType() {
    return tagType;
  }

  /**
   * Gets the finality of this exception type.
   *
   * @return the finality (defaults to {@link Finality#FINAL})
   * @since 1.1.0
   */
  public Finality finality() {
    return finality;
  }

  /**
   * Gets the engine this exception type was created with, if any.
   *
   * @return the engine, or empty if not bound to an engine
   * @since 1.1.0
   */
  public Optional<Engine> engine() {
    return Optional.ofNullable(engine);
  }

  /**
   * Gets the type of a specific field by index.
   *
   * @param index the zero-based field index
   * @return the field's value type
   * @throws IndexOutOfBoundsException if index is out of range
   */
  public WasmValueType field(final int index) {
    return fields.get(index);
  }

  /**
   * Gets all field types of this exception type.
   *
   * @return an unmodifiable list of field value types
   */
  public List<WasmValueType> fields() {
    return fields;
  }

  /**
   * Gets the number of fields in this exception type.
   *
   * @return the field count
   */
  public int fieldCount() {
    return fields.size();
  }

  /**
   * Checks if this exception type matches another for compatibility.
   *
   * <p>Two ExnTypes match if they have the same number of fields and each field type matches.
   *
   * @param other the other exception type
   * @return true if the types match
   */
  public boolean matches(final ExnType other) {
    if (other == null) {
      return false;
    }
    return fields.equals(other.fields);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ExnType exnType = (ExnType) obj;
    return Objects.equals(fields, exnType.fields);
  }

  @Override
  public int hashCode() {
    return fields.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("exn(");
    for (int i = 0; i < fields.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(fields.get(i).name().toLowerCase(java.util.Locale.ROOT));
    }
    sb.append(')');
    return sb.toString();
  }
}
