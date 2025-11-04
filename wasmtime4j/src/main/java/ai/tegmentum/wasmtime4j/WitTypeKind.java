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

package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the kind of a WIT type, defining its structure and behavior.
 *
 * <p>WIT type kinds include primitives, records, variants, enums, flags, lists, options, results,
 * and resources, each with specific semantics and validation rules.
 *
 * @since 1.0.0
 */
public abstract class WitTypeKind {

  /**
   * Creates a primitive type kind.
   *
   * @param primitive the primitive type
   * @return a primitive type kind
   */
  public static WitTypeKind primitive(final WitPrimitiveType primitive) {
    return new PrimitiveTypeKind(primitive);
  }

  /**
   * Creates a record type kind.
   *
   * @param fields the record fields
   * @return a record type kind
   */
  public static WitTypeKind record(final Map<String, WitType> fields) {
    return new RecordTypeKind(fields);
  }

  /**
   * Creates a variant type kind.
   *
   * @param cases the variant cases
   * @return a variant type kind
   */
  public static WitTypeKind variant(final Map<String, Optional<WitType>> cases) {
    return new VariantTypeKind(cases);
  }

  /**
   * Creates an enum type kind.
   *
   * @param values the enum values
   * @return an enum type kind
   */
  public static WitTypeKind enumType(final List<String> values) {
    return new EnumTypeKind(values);
  }

  /**
   * Creates a flags type kind.
   *
   * @param flags the flag names
   * @return a flags type kind
   */
  public static WitTypeKind flags(final List<String> flags) {
    return new FlagsTypeKind(flags);
  }

  /**
   * Creates a list type kind.
   *
   * @param elementType the element type
   * @return a list type kind
   */
  public static WitTypeKind list(final WitType elementType) {
    return new ListTypeKind(elementType);
  }

  /**
   * Creates an option type kind.
   *
   * @param innerType the inner type
   * @return an option type kind
   */
  public static WitTypeKind option(final WitType innerType) {
    return new OptionTypeKind(innerType);
  }

  /**
   * Creates a result type kind.
   *
   * @param okType the success type
   * @param errorType the error type
   * @return a result type kind
   */
  public static WitTypeKind result(
      final Optional<WitType> okType, final Optional<WitType> errorType) {
    return new ResultTypeKind(okType, errorType);
  }

  /**
   * Creates a resource type kind.
   *
   * @param resourceId the resource identifier
   * @return a resource type kind
   */
  public static WitTypeKind resource(final String resourceId) {
    return new ResourceTypeKind(resourceId);
  }

  /**
   * Checks if this type kind is compatible with another type kind.
   *
   * @param other the other type kind
   * @return true if compatible, false otherwise
   */
  public abstract boolean isCompatibleWith(WitTypeKind other);

  /**
   * Gets the size in bytes for this type kind (for primitive types).
   *
   * @return the size in bytes, or empty for composite types
   */
  public abstract Optional<Integer> getSizeBytes();

  /**
   * Checks if this is a primitive type kind.
   *
   * @return true if primitive, false otherwise
   */
  public abstract boolean isPrimitive();

  /**
   * Checks if this is a composite type kind.
   *
   * @return true if composite, false otherwise
   */
  public abstract boolean isComposite();

  /**
   * Checks if this is a resource type kind.
   *
   * @return true if resource, false otherwise
   */
  public abstract boolean isResource();

  /**
   * Gets the type category for validation and marshalling.
   *
   * @return the type category
   */
  public abstract WitTypeCategory getCategory();

  /**
   * Gets the primitive type (only for primitive type kinds).
   *
   * @return the primitive type, or empty for non-primitive types
   */
  public Optional<WitPrimitiveType> getPrimitiveType() {
    return Optional.empty(); // Default implementation for non-primitive types
  }

  /**
   * Gets the record field types (only for record types).
   *
   * @return map of field names to types, or empty map for non-record types
   */
  public Map<String, WitType> getRecordFields() {
    return Map.of(); // Default implementation for non-record types
  }

  /** Primitive type kind implementation. */
  private static final class PrimitiveTypeKind extends WitTypeKind {
    private final WitPrimitiveType primitive;

    private PrimitiveTypeKind(final WitPrimitiveType primitive) {
      this.primitive = Objects.requireNonNull(primitive, "primitive");
    }

    @Override
    public boolean isCompatibleWith(final WitTypeKind other) {
      if (!(other instanceof PrimitiveTypeKind)) {
        return false;
      }
      return primitive == ((PrimitiveTypeKind) other).primitive;
    }

    @Override
    public Optional<Integer> getSizeBytes() {
      final int size = primitive.getSizeBytes();
      return size < 0 ? Optional.empty() : Optional.of(size);
    }

    @Override
    public boolean isPrimitive() {
      return true;
    }

    @Override
    public boolean isComposite() {
      return false;
    }

    @Override
    public boolean isResource() {
      return false;
    }

    @Override
    public WitTypeCategory getCategory() {
      return WitTypeCategory.PRIMITIVE;
    }

    @Override
    public Optional<WitPrimitiveType> getPrimitiveType() {
      return Optional.of(primitive);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof PrimitiveTypeKind)) {
        return false;
      }
      return primitive == ((PrimitiveTypeKind) obj).primitive;
    }

    @Override
    public int hashCode() {
      return Objects.hash(primitive);
    }

    @Override
    public String toString() {
      return "PrimitiveTypeKind{primitive=" + primitive + '}';
    }
  }

  /** Record type kind implementation. */
  private static final class RecordTypeKind extends WitTypeKind {
    private final Map<String, WitType> fields;

    private RecordTypeKind(final Map<String, WitType> fields) {
      this.fields = Map.copyOf(Objects.requireNonNull(fields, "fields"));
    }

    @Override
    public boolean isCompatibleWith(final WitTypeKind other) {
      if (!(other instanceof RecordTypeKind)) {
        return false;
      }
      final RecordTypeKind otherRecord = (RecordTypeKind) other;
      return fields.equals(otherRecord.fields);
    }

    @Override
    public Optional<Integer> getSizeBytes() {
      return Optional.empty(); // Variable size composite type
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isComposite() {
      return true;
    }

    @Override
    public boolean isResource() {
      return false;
    }

    @Override
    public WitTypeCategory getCategory() {
      return WitTypeCategory.RECORD;
    }

    @Override
    public Map<String, WitType> getRecordFields() {
      return fields;
    }

    public Map<String, WitType> getFields() {
      return fields;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof RecordTypeKind)) {
        return false;
      }
      return fields.equals(((RecordTypeKind) obj).fields);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fields);
    }

    @Override
    public String toString() {
      return "RecordTypeKind{fields=" + fields + '}';
    }
  }

  /** Variant type kind implementation. */
  private static final class VariantTypeKind extends WitTypeKind {
    private final Map<String, Optional<WitType>> cases;

    private VariantTypeKind(final Map<String, Optional<WitType>> cases) {
      this.cases = Map.copyOf(Objects.requireNonNull(cases, "cases"));
    }

    @Override
    public boolean isCompatibleWith(final WitTypeKind other) {
      if (!(other instanceof VariantTypeKind)) {
        return false;
      }
      final VariantTypeKind otherVariant = (VariantTypeKind) other;
      return cases.equals(otherVariant.cases);
    }

    @Override
    public Optional<Integer> getSizeBytes() {
      return Optional.empty(); // Variable size composite type
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isComposite() {
      return true;
    }

    @Override
    public boolean isResource() {
      return false;
    }

    @Override
    public WitTypeCategory getCategory() {
      return WitTypeCategory.VARIANT;
    }

    public Map<String, Optional<WitType>> getCases() {
      return cases;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof VariantTypeKind)) {
        return false;
      }
      return cases.equals(((VariantTypeKind) obj).cases);
    }

    @Override
    public int hashCode() {
      return Objects.hash(cases);
    }

    @Override
    public String toString() {
      return "VariantTypeKind{cases=" + cases + '}';
    }
  }

  /** Enum type kind implementation. */
  private static final class EnumTypeKind extends WitTypeKind {
    private final List<String> values;

    private EnumTypeKind(final List<String> values) {
      this.values = List.copyOf(Objects.requireNonNull(values, "values"));
    }

    @Override
    public boolean isCompatibleWith(final WitTypeKind other) {
      if (!(other instanceof EnumTypeKind)) {
        return false;
      }
      final EnumTypeKind otherEnum = (EnumTypeKind) other;
      return values.equals(otherEnum.values);
    }

    @Override
    public Optional<Integer> getSizeBytes() {
      return Optional.of(4); // Enum typically represented as u32
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isComposite() {
      return true;
    }

    @Override
    public boolean isResource() {
      return false;
    }

    @Override
    public WitTypeCategory getCategory() {
      return WitTypeCategory.ENUM;
    }

    public List<String> getValues() {
      return values;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof EnumTypeKind)) {
        return false;
      }
      return values.equals(((EnumTypeKind) obj).values);
    }

    @Override
    public int hashCode() {
      return Objects.hash(values);
    }

    @Override
    public String toString() {
      return "EnumTypeKind{values=" + values + '}';
    }
  }

  /** Flags type kind implementation. */
  private static final class FlagsTypeKind extends WitTypeKind {
    private final List<String> flags;

    private FlagsTypeKind(final List<String> flags) {
      this.flags = List.copyOf(Objects.requireNonNull(flags, "flags"));
    }

    @Override
    public boolean isCompatibleWith(final WitTypeKind other) {
      if (!(other instanceof FlagsTypeKind)) {
        return false;
      }
      final FlagsTypeKind otherFlags = (FlagsTypeKind) other;
      return flags.equals(otherFlags.flags);
    }

    @Override
    public Optional<Integer> getSizeBytes() {
      // Size depends on number of flags (1, 2, 4, or 8 bytes)
      final int flagCount = flags.size();
      if (flagCount <= 8) {
        return Optional.of(1);
      }
      if (flagCount <= 16) {
        return Optional.of(2);
      }
      if (flagCount <= 32) {
        return Optional.of(4);
      }
      return Optional.of(8);
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isComposite() {
      return true;
    }

    @Override
    public boolean isResource() {
      return false;
    }

    @Override
    public WitTypeCategory getCategory() {
      return WitTypeCategory.FLAGS;
    }

    public List<String> getFlags() {
      return flags;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof FlagsTypeKind)) {
        return false;
      }
      return flags.equals(((FlagsTypeKind) obj).flags);
    }

    @Override
    public int hashCode() {
      return Objects.hash(flags);
    }

    @Override
    public String toString() {
      return "FlagsTypeKind{flags=" + flags + '}';
    }
  }

  /** List type kind implementation. */
  private static final class ListTypeKind extends WitTypeKind {
    private final WitType elementType;

    private ListTypeKind(final WitType elementType) {
      this.elementType = Objects.requireNonNull(elementType, "elementType");
    }

    @Override
    public boolean isCompatibleWith(final WitTypeKind other) {
      if (!(other instanceof ListTypeKind)) {
        return false;
      }
      final ListTypeKind otherList = (ListTypeKind) other;
      return elementType.isCompatibleWith(otherList.elementType);
    }

    @Override
    public Optional<Integer> getSizeBytes() {
      return Optional.empty(); // Variable size
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isComposite() {
      return true;
    }

    @Override
    public boolean isResource() {
      return false;
    }

    @Override
    public WitTypeCategory getCategory() {
      return WitTypeCategory.LIST;
    }

    public WitType getElementType() {
      return elementType;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ListTypeKind)) {
        return false;
      }
      return elementType.equals(((ListTypeKind) obj).elementType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(elementType);
    }

    @Override
    public String toString() {
      return "ListTypeKind{elementType=" + elementType + '}';
    }
  }

  /** Option type kind implementation. */
  private static final class OptionTypeKind extends WitTypeKind {
    private final WitType innerType;

    private OptionTypeKind(final WitType innerType) {
      this.innerType = Objects.requireNonNull(innerType, "innerType");
    }

    @Override
    public boolean isCompatibleWith(final WitTypeKind other) {
      if (!(other instanceof OptionTypeKind)) {
        return false;
      }
      final OptionTypeKind otherOption = (OptionTypeKind) other;
      return innerType.isCompatibleWith(otherOption.innerType);
    }

    @Override
    public Optional<Integer> getSizeBytes() {
      return Optional.empty(); // Variable size based on inner type
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isComposite() {
      return true;
    }

    @Override
    public boolean isResource() {
      return false;
    }

    @Override
    public WitTypeCategory getCategory() {
      return WitTypeCategory.OPTION;
    }

    public WitType getInnerType() {
      return innerType;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof OptionTypeKind)) {
        return false;
      }
      return innerType.equals(((OptionTypeKind) obj).innerType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(innerType);
    }

    @Override
    public String toString() {
      return "OptionTypeKind{innerType=" + innerType + '}';
    }
  }

  /** Result type kind implementation. */
  private static final class ResultTypeKind extends WitTypeKind {
    private final Optional<WitType> okType;
    private final Optional<WitType> errorType;

    private ResultTypeKind(final Optional<WitType> okType, final Optional<WitType> errorType) {
      this.okType = Objects.requireNonNull(okType, "okType");
      this.errorType = Objects.requireNonNull(errorType, "errorType");
    }

    @Override
    public boolean isCompatibleWith(final WitTypeKind other) {
      if (!(other instanceof ResultTypeKind)) {
        return false;
      }
      final ResultTypeKind otherResult = (ResultTypeKind) other;
      return Objects.equals(okType, otherResult.okType)
          && Objects.equals(errorType, otherResult.errorType);
    }

    @Override
    public Optional<Integer> getSizeBytes() {
      return Optional.empty(); // Variable size based on inner types
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isComposite() {
      return true;
    }

    @Override
    public boolean isResource() {
      return false;
    }

    @Override
    public WitTypeCategory getCategory() {
      return WitTypeCategory.RESULT;
    }

    public Optional<WitType> getOkType() {
      return okType;
    }

    public Optional<WitType> getErrorType() {
      return errorType;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ResultTypeKind)) {
        return false;
      }
      final ResultTypeKind that = (ResultTypeKind) obj;
      return Objects.equals(okType, that.okType) && Objects.equals(errorType, that.errorType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(okType, errorType);
    }

    @Override
    public String toString() {
      return "ResultTypeKind{okType=" + okType + ", errorType=" + errorType + '}';
    }
  }

  /** Resource type kind implementation. */
  private static final class ResourceTypeKind extends WitTypeKind {
    private final String resourceId;

    private ResourceTypeKind(final String resourceId) {
      this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
    }

    @Override
    public boolean isCompatibleWith(final WitTypeKind other) {
      if (!(other instanceof ResourceTypeKind)) {
        return false;
      }
      final ResourceTypeKind otherResource = (ResourceTypeKind) other;
      return resourceId.equals(otherResource.resourceId);
    }

    @Override
    public Optional<Integer> getSizeBytes() {
      return Optional.of(4); // Resource handle is typically a u32
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isComposite() {
      return false;
    }

    @Override
    public boolean isResource() {
      return true;
    }

    @Override
    public WitTypeCategory getCategory() {
      return WitTypeCategory.RESOURCE;
    }

    public String getResourceId() {
      return resourceId;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ResourceTypeKind)) {
        return false;
      }
      return resourceId.equals(((ResourceTypeKind) obj).resourceId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(resourceId);
    }

    @Override
    public String toString() {
      return "ResourceTypeKind{resourceId='" + resourceId + "'}";
    }
  }
}
