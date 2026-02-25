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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Describes a Component Model type with full type information.
 *
 * <p>While {@link ComponentType} represents the kind of type (primitive, list, record, etc.),
 * ComponentTypeDescriptor provides complete type information including element types for
 * collections, field types for records, and case types for variants.
 *
 * @since 1.0.0
 */
public interface ComponentTypeDescriptor {

  /**
   * Gets the type kind.
   *
   * @return the component type
   */
  ComponentType getType();

  /**
   * Gets the optional parameter name (for function parameters and record fields).
   *
   * @return the optional name
   */
  Optional<String> getName();

  /**
   * Gets the element type for list types.
   *
   * @return the element type descriptor
   * @throws IllegalStateException if this is not a list type
   */
  ComponentTypeDescriptor getElementType();

  /**
   * Gets the field descriptors for record types.
   *
   * @return map of field names to type descriptors
   * @throws IllegalStateException if this is not a record type
   */
  Map<String, ComponentTypeDescriptor> getRecordFields();

  /**
   * Gets the element type descriptors for tuple types.
   *
   * @return list of element type descriptors
   * @throws IllegalStateException if this is not a tuple type
   */
  List<ComponentTypeDescriptor> getTupleElements();

  /**
   * Gets the case descriptors for variant types.
   *
   * @return map of case names to optional payload type descriptors
   * @throws IllegalStateException if this is not a variant type
   */
  Map<String, Optional<ComponentTypeDescriptor>> getVariantCases();

  /**
   * Gets the case names for enum types.
   *
   * @return list of enum case names
   * @throws IllegalStateException if this is not an enum type
   */
  List<String> getEnumCases();

  /**
   * Gets the inner type for option types.
   *
   * @return the inner type descriptor
   * @throws IllegalStateException if this is not an option type
   */
  ComponentTypeDescriptor getOptionType();

  /**
   * Gets the ok type for result types.
   *
   * @return the optional ok type descriptor
   * @throws IllegalStateException if this is not a result type
   */
  Optional<ComponentTypeDescriptor> getResultOkType();

  /**
   * Gets the err type for result types.
   *
   * @return the optional err type descriptor
   * @throws IllegalStateException if this is not a result type
   */
  Optional<ComponentTypeDescriptor> getResultErrType();

  /**
   * Gets the flag names for flags types.
   *
   * @return list of flag names
   * @throws IllegalStateException if this is not a flags type
   */
  List<String> getFlagNames();

  /**
   * Gets the resource type name for resource handle types.
   *
   * @return the resource type name
   * @throws IllegalStateException if this is not a resource handle type
   */
  String getResourceTypeName();

  /**
   * Gets the resource type ID for resource handle types (own/borrow).
   *
   * @return the opaque resource type identifier
   * @throws IllegalStateException if this is not a resource handle type
   */
  long getResourceTypeId();

  /**
   * Returns true if this is an owned resource handle (own type).
   *
   * @return true for OWN, false for BORROW
   * @throws IllegalStateException if this is not a resource handle type
   */
  boolean isResourceOwned();

  /**
   * Gets the payload type for future types.
   *
   * @return the optional payload type (empty if future has no payload)
   * @throws IllegalStateException if this is not a future type
   */
  Optional<ComponentTypeDescriptor> getFuturePayloadType();

  /**
   * Gets the element type for stream types.
   *
   * @return the optional element type (empty if stream has no element type)
   * @throws IllegalStateException if this is not a stream type
   */
  Optional<ComponentTypeDescriptor> getStreamElementType();

  // ========== Factory methods for primitive types ==========

  /** Creates a bool type descriptor. */
  static ComponentTypeDescriptor bool() {
    return new PrimitiveImpl(ComponentType.BOOL, null);
  }

  /** Creates an s8 type descriptor. */
  static ComponentTypeDescriptor s8() {
    return new PrimitiveImpl(ComponentType.S8, null);
  }

  /** Creates an s16 type descriptor. */
  static ComponentTypeDescriptor s16() {
    return new PrimitiveImpl(ComponentType.S16, null);
  }

  /** Creates an s32 type descriptor. */
  static ComponentTypeDescriptor s32() {
    return new PrimitiveImpl(ComponentType.S32, null);
  }

  /** Creates an s64 type descriptor. */
  static ComponentTypeDescriptor s64() {
    return new PrimitiveImpl(ComponentType.S64, null);
  }

  /** Creates a u8 type descriptor. */
  static ComponentTypeDescriptor u8() {
    return new PrimitiveImpl(ComponentType.U8, null);
  }

  /** Creates a u16 type descriptor. */
  static ComponentTypeDescriptor u16() {
    return new PrimitiveImpl(ComponentType.U16, null);
  }

  /** Creates a u32 type descriptor. */
  static ComponentTypeDescriptor u32() {
    return new PrimitiveImpl(ComponentType.U32, null);
  }

  /** Creates a u64 type descriptor. */
  static ComponentTypeDescriptor u64() {
    return new PrimitiveImpl(ComponentType.U64, null);
  }

  /** Creates an f32 type descriptor. */
  static ComponentTypeDescriptor f32() {
    return new PrimitiveImpl(ComponentType.F32, null);
  }

  /** Creates an f64 type descriptor. */
  static ComponentTypeDescriptor f64() {
    return new PrimitiveImpl(ComponentType.F64, null);
  }

  /** Creates a char type descriptor. */
  static ComponentTypeDescriptor char_() {
    return new PrimitiveImpl(ComponentType.CHAR, null);
  }

  /** Creates a string type descriptor. */
  static ComponentTypeDescriptor string() {
    return new PrimitiveImpl(ComponentType.STRING, null);
  }

  /**
   * Creates a named type descriptor (for parameters and fields).
   *
   * @param name the parameter/field name
   * @param inner the inner type
   * @return a new named type descriptor
   */
  static ComponentTypeDescriptor named(final String name, final ComponentTypeDescriptor inner) {
    return new NamedImpl(name, inner);
  }

  /**
   * Creates a list type descriptor.
   *
   * @param elementType the element type
   * @return a new list type descriptor
   */
  static ComponentTypeDescriptor list(final ComponentTypeDescriptor elementType) {
    return new ListImpl(elementType);
  }

  /**
   * Creates an option type descriptor.
   *
   * @param innerType the inner type
   * @return a new option type descriptor
   */
  static ComponentTypeDescriptor option(final ComponentTypeDescriptor innerType) {
    return new OptionImpl(innerType);
  }

  /**
   * Creates a result type descriptor.
   *
   * @param okType the ok type (may be null)
   * @param errType the err type (may be null)
   * @return a new result type descriptor
   */
  static ComponentTypeDescriptor result(
      final ComponentTypeDescriptor okType, final ComponentTypeDescriptor errType) {
    return new ResultImpl(okType, errType);
  }

  /**
   * Creates a future type descriptor.
   *
   * @param payloadType the optional payload type (null for void future)
   * @return a new future type descriptor
   */
  static ComponentTypeDescriptor future(final ComponentTypeDescriptor payloadType) {
    return new FutureImpl(payloadType);
  }

  /**
   * Creates a stream type descriptor.
   *
   * @param elementType the optional element type (null for void stream)
   * @return a new stream type descriptor
   */
  static ComponentTypeDescriptor stream(final ComponentTypeDescriptor elementType) {
    return new StreamImpl(elementType);
  }

  /**
   * Creates an own resource handle type descriptor.
   *
   * @param resourceTypeName the resource type name
   * @param resourceTypeId the opaque resource type identifier
   * @return a new own resource handle type descriptor
   */
  static ComponentTypeDescriptor own(final String resourceTypeName, final long resourceTypeId) {
    return new ResourceHandleImpl(ComponentType.OWN, resourceTypeName, resourceTypeId, true);
  }

  /**
   * Creates a borrow resource handle type descriptor.
   *
   * @param resourceTypeName the resource type name
   * @param resourceTypeId the opaque resource type identifier
   * @return a new borrow resource handle type descriptor
   */
  static ComponentTypeDescriptor borrow(final String resourceTypeName, final long resourceTypeId) {
    return new ResourceHandleImpl(ComponentType.BORROW, resourceTypeName, resourceTypeId, false);
  }

  /**
   * Creates a record type descriptor.
   *
   * @param fields the record fields as name-to-type map
   * @return a new record type descriptor
   */
  static ComponentTypeDescriptor record(final Map<String, ComponentTypeDescriptor> fields) {
    return new RecordImpl(fields);
  }

  /**
   * Creates a tuple type descriptor.
   *
   * @param elements the tuple element types
   * @return a new tuple type descriptor
   */
  static ComponentTypeDescriptor tuple(final List<ComponentTypeDescriptor> elements) {
    return new TupleImpl(elements);
  }

  /**
   * Creates a variant type descriptor.
   *
   * @param cases the variant cases as name-to-optional-payload map
   * @return a new variant type descriptor
   */
  static ComponentTypeDescriptor variant(
      final Map<String, Optional<ComponentTypeDescriptor>> cases) {
    return new VariantImpl(cases);
  }

  /**
   * Creates an enum type descriptor.
   *
   * @param cases the enum case names
   * @return a new enum type descriptor
   */
  static ComponentTypeDescriptor enum_(final List<String> cases) {
    return new EnumImpl(cases);
  }

  /**
   * Creates a flags type descriptor.
   *
   * @param names the flag names
   * @return a new flags type descriptor
   */
  static ComponentTypeDescriptor flags(final List<String> names) {
    return new FlagsImpl(names);
  }

  /**
   * Creates an error-context type descriptor.
   *
   * @return a new error-context type descriptor
   */
  static ComponentTypeDescriptor errorContext() {
    return new PrimitiveImpl(ComponentType.ERROR_CONTEXT, null);
  }

  /** Primitive type descriptor implementation. */
  final class PrimitiveImpl implements ComponentTypeDescriptor {
    private final ComponentType type;
    private final String name;

    PrimitiveImpl(final ComponentType type, final String name) {
      this.type = type;
      this.name = name;
    }

    @Override
    public ComponentType getType() {
      return type;
    }

    @Override
    public Optional<String> getName() {
      return Optional.ofNullable(name);
    }

    @Override
    public ComponentTypeDescriptor getElementType() {
      throw new IllegalStateException("Not a list type");
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      throw new IllegalStateException("Not a record type");
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      throw new IllegalStateException("Not a tuple type");
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      throw new IllegalStateException("Not a variant type");
    }

    @Override
    public List<String> getEnumCases() {
      throw new IllegalStateException("Not an enum type");
    }

    @Override
    public ComponentTypeDescriptor getOptionType() {
      throw new IllegalStateException("Not an option type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultOkType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultErrType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public List<String> getFlagNames() {
      throw new IllegalStateException("Not a flags type");
    }

    @Override
    public String getResourceTypeName() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public long getResourceTypeId() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public boolean isResourceOwned() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getFuturePayloadType() {
      throw new IllegalStateException("Not a future type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getStreamElementType() {
      throw new IllegalStateException("Not a stream type");
    }

    @Override
    public String toString() {
      return type.name().toLowerCase(Locale.ROOT);
    }
  }

  /** Named type descriptor wrapper. */
  final class NamedImpl implements ComponentTypeDescriptor {
    private final String name;
    private final ComponentTypeDescriptor inner;

    NamedImpl(final String name, final ComponentTypeDescriptor inner) {
      this.name = name;
      this.inner = inner;
    }

    @Override
    public ComponentType getType() {
      return inner.getType();
    }

    @Override
    public Optional<String> getName() {
      return Optional.of(name);
    }

    @Override
    public ComponentTypeDescriptor getElementType() {
      return inner.getElementType();
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      return inner.getRecordFields();
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      return inner.getTupleElements();
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      return inner.getVariantCases();
    }

    @Override
    public List<String> getEnumCases() {
      return inner.getEnumCases();
    }

    @Override
    public ComponentTypeDescriptor getOptionType() {
      return inner.getOptionType();
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultOkType() {
      return inner.getResultOkType();
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultErrType() {
      return inner.getResultErrType();
    }

    @Override
    public List<String> getFlagNames() {
      return inner.getFlagNames();
    }

    @Override
    public String getResourceTypeName() {
      return inner.getResourceTypeName();
    }

    @Override
    public long getResourceTypeId() {
      return inner.getResourceTypeId();
    }

    @Override
    public boolean isResourceOwned() {
      return inner.isResourceOwned();
    }

    @Override
    public Optional<ComponentTypeDescriptor> getFuturePayloadType() {
      return inner.getFuturePayloadType();
    }

    @Override
    public Optional<ComponentTypeDescriptor> getStreamElementType() {
      return inner.getStreamElementType();
    }

    @Override
    public String toString() {
      return name + ": " + inner;
    }
  }

  /** List type descriptor. */
  final class ListImpl implements ComponentTypeDescriptor {
    private final ComponentTypeDescriptor elementType;

    ListImpl(final ComponentTypeDescriptor elementType) {
      this.elementType = elementType;
    }

    @Override
    public ComponentType getType() {
      return ComponentType.LIST;
    }

    @Override
    public Optional<String> getName() {
      return Optional.empty();
    }

    @Override
    public ComponentTypeDescriptor getElementType() {
      return elementType;
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      throw new IllegalStateException("Not a record type");
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      throw new IllegalStateException("Not a tuple type");
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      throw new IllegalStateException("Not a variant type");
    }

    @Override
    public List<String> getEnumCases() {
      throw new IllegalStateException("Not an enum type");
    }

    @Override
    public ComponentTypeDescriptor getOptionType() {
      throw new IllegalStateException("Not an option type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultOkType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultErrType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public List<String> getFlagNames() {
      throw new IllegalStateException("Not a flags type");
    }

    @Override
    public String getResourceTypeName() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public long getResourceTypeId() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public boolean isResourceOwned() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getFuturePayloadType() {
      throw new IllegalStateException("Not a future type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getStreamElementType() {
      throw new IllegalStateException("Not a stream type");
    }

    @Override
    public String toString() {
      return "list<" + elementType + ">";
    }
  }

  /** Option type descriptor. */
  final class OptionImpl implements ComponentTypeDescriptor {
    private final ComponentTypeDescriptor innerType;

    OptionImpl(final ComponentTypeDescriptor innerType) {
      this.innerType = innerType;
    }

    @Override
    public ComponentType getType() {
      return ComponentType.OPTION;
    }

    @Override
    public Optional<String> getName() {
      return Optional.empty();
    }

    @Override
    public ComponentTypeDescriptor getElementType() {
      throw new IllegalStateException("Not a list type");
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      throw new IllegalStateException("Not a record type");
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      throw new IllegalStateException("Not a tuple type");
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      throw new IllegalStateException("Not a variant type");
    }

    @Override
    public List<String> getEnumCases() {
      throw new IllegalStateException("Not an enum type");
    }

    @Override
    public ComponentTypeDescriptor getOptionType() {
      return innerType;
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultOkType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultErrType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public List<String> getFlagNames() {
      throw new IllegalStateException("Not a flags type");
    }

    @Override
    public String getResourceTypeName() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public long getResourceTypeId() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public boolean isResourceOwned() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getFuturePayloadType() {
      throw new IllegalStateException("Not a future type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getStreamElementType() {
      throw new IllegalStateException("Not a stream type");
    }

    @Override
    public String toString() {
      return "option<" + innerType + ">";
    }
  }

  /** Result type descriptor. */
  final class ResultImpl implements ComponentTypeDescriptor {
    private final ComponentTypeDescriptor okType;
    private final ComponentTypeDescriptor errType;

    ResultImpl(final ComponentTypeDescriptor okType, final ComponentTypeDescriptor errType) {
      this.okType = okType;
      this.errType = errType;
    }

    @Override
    public ComponentType getType() {
      return ComponentType.RESULT;
    }

    @Override
    public Optional<String> getName() {
      return Optional.empty();
    }

    @Override
    public ComponentTypeDescriptor getElementType() {
      throw new IllegalStateException("Not a list type");
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      throw new IllegalStateException("Not a record type");
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      throw new IllegalStateException("Not a tuple type");
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      throw new IllegalStateException("Not a variant type");
    }

    @Override
    public List<String> getEnumCases() {
      throw new IllegalStateException("Not an enum type");
    }

    @Override
    public ComponentTypeDescriptor getOptionType() {
      throw new IllegalStateException("Not an option type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultOkType() {
      return Optional.ofNullable(okType);
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultErrType() {
      return Optional.ofNullable(errType);
    }

    @Override
    public List<String> getFlagNames() {
      throw new IllegalStateException("Not a flags type");
    }

    @Override
    public String getResourceTypeName() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public long getResourceTypeId() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public boolean isResourceOwned() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getFuturePayloadType() {
      throw new IllegalStateException("Not a future type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getStreamElementType() {
      throw new IllegalStateException("Not a stream type");
    }

    @Override
    public String toString() {
      final String ok = okType != null ? okType.toString() : "_";
      final String err = errType != null ? errType.toString() : "_";
      return "result<" + ok + ", " + err + ">";
    }
  }

  /** Future type descriptor. */
  final class FutureImpl implements ComponentTypeDescriptor {
    private final ComponentTypeDescriptor payloadType;

    FutureImpl(final ComponentTypeDescriptor payloadType) {
      this.payloadType = payloadType;
    }

    @Override
    public ComponentType getType() {
      return ComponentType.FUTURE;
    }

    @Override
    public Optional<String> getName() {
      return Optional.empty();
    }

    @Override
    public ComponentTypeDescriptor getElementType() {
      throw new IllegalStateException("Not a list type");
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      throw new IllegalStateException("Not a record type");
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      throw new IllegalStateException("Not a tuple type");
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      throw new IllegalStateException("Not a variant type");
    }

    @Override
    public List<String> getEnumCases() {
      throw new IllegalStateException("Not an enum type");
    }

    @Override
    public ComponentTypeDescriptor getOptionType() {
      throw new IllegalStateException("Not an option type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultOkType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultErrType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public List<String> getFlagNames() {
      throw new IllegalStateException("Not a flags type");
    }

    @Override
    public String getResourceTypeName() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public long getResourceTypeId() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public boolean isResourceOwned() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getFuturePayloadType() {
      return Optional.ofNullable(payloadType);
    }

    @Override
    public Optional<ComponentTypeDescriptor> getStreamElementType() {
      throw new IllegalStateException("Not a stream type");
    }

    @Override
    public String toString() {
      return payloadType != null ? "future<" + payloadType + ">" : "future";
    }
  }

  /** Stream type descriptor. */
  final class StreamImpl implements ComponentTypeDescriptor {
    private final ComponentTypeDescriptor elementType;

    StreamImpl(final ComponentTypeDescriptor elementType) {
      this.elementType = elementType;
    }

    @Override
    public ComponentType getType() {
      return ComponentType.STREAM;
    }

    @Override
    public Optional<String> getName() {
      return Optional.empty();
    }

    @Override
    public ComponentTypeDescriptor getElementType() {
      throw new IllegalStateException("Not a list type");
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      throw new IllegalStateException("Not a record type");
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      throw new IllegalStateException("Not a tuple type");
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      throw new IllegalStateException("Not a variant type");
    }

    @Override
    public List<String> getEnumCases() {
      throw new IllegalStateException("Not an enum type");
    }

    @Override
    public ComponentTypeDescriptor getOptionType() {
      throw new IllegalStateException("Not an option type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultOkType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultErrType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public List<String> getFlagNames() {
      throw new IllegalStateException("Not a flags type");
    }

    @Override
    public String getResourceTypeName() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public long getResourceTypeId() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public boolean isResourceOwned() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getFuturePayloadType() {
      throw new IllegalStateException("Not a future type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getStreamElementType() {
      return Optional.ofNullable(elementType);
    }

    @Override
    public String toString() {
      return elementType != null ? "stream<" + elementType + ">" : "stream";
    }
  }

  /** Resource handle type descriptor (for own and borrow types). */
  final class ResourceHandleImpl implements ComponentTypeDescriptor {
    private final ComponentType type;
    private final String resourceTypeName;
    private final long resourceTypeId;
    private final boolean owned;

    ResourceHandleImpl(
        final ComponentType type,
        final String resourceTypeName,
        final long resourceTypeId,
        final boolean owned) {
      this.type = type;
      this.resourceTypeName = resourceTypeName;
      this.resourceTypeId = resourceTypeId;
      this.owned = owned;
    }

    @Override
    public ComponentType getType() {
      return type;
    }

    @Override
    public Optional<String> getName() {
      return Optional.empty();
    }

    @Override
    public ComponentTypeDescriptor getElementType() {
      throw new IllegalStateException("Not a list type");
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      throw new IllegalStateException("Not a record type");
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      throw new IllegalStateException("Not a tuple type");
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      throw new IllegalStateException("Not a variant type");
    }

    @Override
    public List<String> getEnumCases() {
      throw new IllegalStateException("Not an enum type");
    }

    @Override
    public ComponentTypeDescriptor getOptionType() {
      throw new IllegalStateException("Not an option type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultOkType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultErrType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public List<String> getFlagNames() {
      throw new IllegalStateException("Not a flags type");
    }

    @Override
    public String getResourceTypeName() {
      return resourceTypeName;
    }

    @Override
    public long getResourceTypeId() {
      return resourceTypeId;
    }

    @Override
    public boolean isResourceOwned() {
      return owned;
    }

    @Override
    public Optional<ComponentTypeDescriptor> getFuturePayloadType() {
      throw new IllegalStateException("Not a future type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getStreamElementType() {
      throw new IllegalStateException("Not a stream type");
    }

    @Override
    public String toString() {
      return (owned ? "own<" : "borrow<") + resourceTypeName + ">";
    }
  }

  /** Record type descriptor implementation. */
  final class RecordImpl extends AbstractCompoundImpl {
    private final Map<String, ComponentTypeDescriptor> fields;

    RecordImpl(final Map<String, ComponentTypeDescriptor> fields) {
      super(ComponentType.RECORD);
      this.fields = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(fields));
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      return fields;
    }

    @Override
    public String toString() {
      return "record{" + fields.size() + " fields}";
    }
  }

  /** Tuple type descriptor implementation. */
  final class TupleImpl extends AbstractCompoundImpl {
    private final List<ComponentTypeDescriptor> elements;

    TupleImpl(final List<ComponentTypeDescriptor> elements) {
      super(ComponentType.TUPLE);
      this.elements = List.copyOf(elements);
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      return elements;
    }

    @Override
    public String toString() {
      return "tuple<" + elements.size() + ">";
    }
  }

  /** Variant type descriptor implementation. */
  final class VariantImpl extends AbstractCompoundImpl {
    private final Map<String, Optional<ComponentTypeDescriptor>> cases;

    VariantImpl(final Map<String, Optional<ComponentTypeDescriptor>> cases) {
      super(ComponentType.VARIANT);
      this.cases = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(cases));
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      return cases;
    }

    @Override
    public String toString() {
      return "variant{" + cases.size() + " cases}";
    }
  }

  /** Enum type descriptor implementation. */
  final class EnumImpl extends AbstractCompoundImpl {
    private final List<String> cases;

    EnumImpl(final List<String> cases) {
      super(ComponentType.ENUM);
      this.cases = List.copyOf(cases);
    }

    @Override
    public List<String> getEnumCases() {
      return cases;
    }

    @Override
    public String toString() {
      return "enum{" + cases.size() + " cases}";
    }
  }

  /** Flags type descriptor implementation. */
  final class FlagsImpl extends AbstractCompoundImpl {
    private final List<String> names;

    FlagsImpl(final List<String> names) {
      super(ComponentType.FLAGS);
      this.names = List.copyOf(names);
    }

    @Override
    public List<String> getFlagNames() {
      return names;
    }

    @Override
    public String toString() {
      return "flags{" + names.size() + "}";
    }
  }

  /**
   * Abstract base for compound type descriptors that throws on all accessors by default. Subclasses
   * override only the methods relevant to their type.
   */
  abstract class AbstractCompoundImpl implements ComponentTypeDescriptor {
    private final ComponentType type;

    AbstractCompoundImpl(final ComponentType type) {
      this.type = type;
    }

    @Override
    public ComponentType getType() {
      return type;
    }

    @Override
    public Optional<String> getName() {
      return Optional.empty();
    }

    @Override
    public ComponentTypeDescriptor getElementType() {
      throw new IllegalStateException("Not a list type");
    }

    @Override
    public Map<String, ComponentTypeDescriptor> getRecordFields() {
      throw new IllegalStateException("Not a record type");
    }

    @Override
    public List<ComponentTypeDescriptor> getTupleElements() {
      throw new IllegalStateException("Not a tuple type");
    }

    @Override
    public Map<String, Optional<ComponentTypeDescriptor>> getVariantCases() {
      throw new IllegalStateException("Not a variant type");
    }

    @Override
    public List<String> getEnumCases() {
      throw new IllegalStateException("Not an enum type");
    }

    @Override
    public ComponentTypeDescriptor getOptionType() {
      throw new IllegalStateException("Not an option type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultOkType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getResultErrType() {
      throw new IllegalStateException("Not a result type");
    }

    @Override
    public List<String> getFlagNames() {
      throw new IllegalStateException("Not a flags type");
    }

    @Override
    public String getResourceTypeName() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public long getResourceTypeId() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public boolean isResourceOwned() {
      throw new IllegalStateException("Not a resource type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getFuturePayloadType() {
      throw new IllegalStateException("Not a future type");
    }

    @Override
    public Optional<ComponentTypeDescriptor> getStreamElementType() {
      throw new IllegalStateException("Not a stream type");
    }
  }
}
