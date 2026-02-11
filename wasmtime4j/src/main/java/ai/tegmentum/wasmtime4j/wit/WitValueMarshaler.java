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

package ai.tegmentum.wasmtime4j.wit;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Value marshaler for WebAssembly Interface Type (WIT) values.
 *
 * <p>This class provides bidirectional marshaling between Java objects and WIT values, supporting
 * all WIT type categories including primitives, composites, and resources.
 *
 * @since 1.0.0
 */
@SuppressFBWarnings(
    value = "REC_CATCH_EXCEPTION",
    justification =
        "Broad exception catching for defensive reflection-based marshaling;"
            + " skips inaccessible methods without interrupting the marshaling process")
public final class WitValueMarshaler {

  private final Map<String, ValueConverter> converters;

  /** Creates a new WIT value marshaler. */
  public WitValueMarshaler() {
    this.converters = new ConcurrentHashMap<>();
    initializeBuiltInConverters();
  }

  /**
   * Marshals a Java value to WIT representation.
   *
   * @param value the Java value
   * @param witType the target WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  public Object marshalToWit(final Object value, final WitType witType) throws WasmException {
    Objects.requireNonNull(witType, "witType");

    if (value == null) {
      if (witType.getKind().getCategory() == WitTypeCategory.OPTION) {
        return new WitOption(Optional.empty());
      } else {
        throw new WasmException(
            "Cannot marshal null value to non-option type: " + witType.getName());
      }
    }

    final String converterKey = getConverterKey(witType);
    final ValueConverter converter = converters.get(converterKey);
    if (converter != null) {
      return converter.toWit(value);
    }

    // Generic marshaling based on type category
    switch (witType.getKind().getCategory()) {
      case PRIMITIVE:
        return marshalPrimitiveToWit(value, witType);
      case RECORD:
        return marshalRecordToWit(value, witType);
      case VARIANT:
        return marshalVariantToWit(value, witType);
      case ENUM:
        return marshalEnumToWit(value, witType);
      case FLAGS:
        return marshalFlagsToWit(value, witType);
      case LIST:
        return marshalListToWit(value, witType);
      case OPTION:
        return marshalOptionToWit(value, witType);
      case RESULT:
        return marshalResultToWit(value, witType);
      case RESOURCE:
        return marshalResourceToWit(value, witType);
      default:
        throw new WasmException(
            "Unsupported WIT type category: " + witType.getKind().getCategory());
    }
  }

  /**
   * Marshals a WIT value to Java representation.
   *
   * @param value the WIT value
   * @param witType the source WIT type
   * @return the Java representation
   * @throws WasmException if marshaling fails
   */
  public Object marshalToJava(final Object value, final WitType witType) throws WasmException {
    Objects.requireNonNull(witType, "witType");

    if (value == null) {
      return null;
    }

    final String converterKey = getConverterKey(witType);
    final ValueConverter converter = converters.get(converterKey);
    if (converter != null) {
      return converter.fromWit(value);
    }

    // Generic marshaling based on type category
    switch (witType.getKind().getCategory()) {
      case PRIMITIVE:
        return marshalPrimitiveToJava(value, witType);
      case RECORD:
        return marshalRecordToJava(value, witType);
      case VARIANT:
        return marshalVariantToJava(value, witType);
      case ENUM:
        return marshalEnumToJava(value, witType);
      case FLAGS:
        return marshalFlagsToJava(value, witType);
      case LIST:
        return marshalListToJava(value, witType);
      case OPTION:
        return marshalOptionToJava(value, witType);
      case RESULT:
        return marshalResultToJava(value, witType);
      case RESOURCE:
        return marshalResourceToJava(value, witType);
      default:
        throw new WasmException(
            "Unsupported WIT type category: " + witType.getKind().getCategory());
    }
  }

  /**
   * Registers a custom value converter.
   *
   * @param witType the WIT type
   * @param converter the value converter
   */
  public void registerConverter(final WitType witType, final ValueConverter converter) {
    Objects.requireNonNull(witType, "witType");
    Objects.requireNonNull(converter, "converter");

    final String key = getConverterKey(witType);
    converters.put(key, converter);
  }

  /**
   * Marshals a primitive value to WIT.
   *
   * @param value the Java value
   * @param witType the WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalPrimitiveToWit(final Object value, final WitType witType)
      throws WasmException {
    final String typeName = witType.getName();

    switch (typeName) {
      case "bool":
        if (value instanceof Boolean) {
          return value;
        } else {
          throw new WasmException("Expected Boolean for bool type, got: " + value.getClass());
        }
      case "s8":
      case "u8":
        if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
          return ((Number) value).byteValue();
        } else {
          throw new WasmException(
              "Expected Number for " + typeName + " type, got: " + value.getClass());
        }
      case "s16":
      case "u16":
        if (value instanceof Number) {
          return ((Number) value).shortValue();
        } else {
          throw new WasmException(
              "Expected Number for " + typeName + " type, got: " + value.getClass());
        }
      case "s32":
      case "u32":
        if (value instanceof Number) {
          return ((Number) value).intValue();
        } else {
          throw new WasmException(
              "Expected Number for " + typeName + " type, got: " + value.getClass());
        }
      case "s64":
      case "u64":
        if (value instanceof Number) {
          return ((Number) value).longValue();
        } else {
          throw new WasmException(
              "Expected Number for " + typeName + " type, got: " + value.getClass());
        }
      case "float32":
        if (value instanceof Number) {
          return ((Number) value).floatValue();
        } else {
          throw new WasmException("Expected Number for float32 type, got: " + value.getClass());
        }
      case "float64":
        if (value instanceof Number) {
          return ((Number) value).doubleValue();
        } else {
          throw new WasmException("Expected Number for float64 type, got: " + value.getClass());
        }
      case "char":
        if (value instanceof Character) {
          return value;
        } else if (value instanceof String && ((String) value).length() == 1) {
          return ((String) value).charAt(0);
        } else {
          throw new WasmException("Expected Character for char type, got: " + value.getClass());
        }
      case "string":
        if (value instanceof String) {
          return value;
        } else {
          return value.toString();
        }
      default:
        throw new WasmException("Unknown primitive type: " + typeName);
    }
  }

  /**
   * Marshals a primitive WIT value to Java.
   *
   * @param value the WIT value
   * @param witType the WIT type
   * @return the Java representation
   */
  private Object marshalPrimitiveToJava(final Object value, final WitType witType) {
    // For primitives, the WIT representation is typically the same as Java
    return value;
  }

  /**
   * Marshals a record value to WIT.
   *
   * @param value the Java value
   * @param witType the WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalRecordToWit(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> recordMap = (Map<String, Object>) value;

      // Get field types from the WIT type to recursively marshal nested values
      final Map<String, WitType> fieldTypes = getRecordFieldTypes(witType);
      final Map<String, Object> marshaledFields = new java.util.HashMap<>();

      for (final Map.Entry<String, Object> entry : recordMap.entrySet()) {
        final String fieldName = entry.getKey();
        final Object fieldValue = entry.getValue();
        final WitType fieldType = fieldTypes.get(fieldName);

        if (fieldType != null) {
          // Recursively marshal the field value
          marshaledFields.put(fieldName, marshalToWit(fieldValue, fieldType));
        } else {
          // Field not in type definition, pass through as-is
          marshaledFields.put(fieldName, fieldValue);
        }
      }

      return new WitRecord(marshaledFields);
    } else {
      // Use reflection to extract fields from Java object
      return marshalObjectToRecord(value);
    }
  }

  /**
   * Gets the field types from a record WIT type.
   *
   * @param witType the record type
   * @return map of field names to types
   */
  private Map<String, WitType> getRecordFieldTypes(final WitType witType) {
    return witType.getKind().getRecordFields();
  }

  /**
   * Marshals a record WIT value to Java.
   *
   * @param value the WIT value
   * @param witType the WIT type
   * @return the Java representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalRecordToJava(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof WitRecord) {
      return ((WitRecord) value).getFields();
    } else if (value instanceof Map) {
      return value;
    } else {
      throw new WasmException(
          "Expected WitRecord or Map for record type, got: " + value.getClass());
    }
  }

  /**
   * Marshals a variant value to WIT.
   *
   * @param value the Java value
   * @param witType the WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalVariantToWit(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof WitVariant) {
      return value;
    } else if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> variantMap = (Map<String, Object>) value;
      if (variantMap.size() != 1) {
        throw new WasmException("Variant map must have exactly one entry");
      }
      final Map.Entry<String, Object> entry = variantMap.entrySet().iterator().next();
      return new WitVariant(entry.getKey(), Optional.ofNullable(entry.getValue()));
    } else {
      throw new WasmException(
          "Expected WitVariant or single-entry Map for variant type, got: " + value.getClass());
    }
  }

  /**
   * Marshals a variant WIT value to Java.
   *
   * @param value the WIT value
   * @param witType the WIT type
   * @return the Java representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalVariantToJava(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof WitVariant) {
      final WitVariant variant = (WitVariant) value;
      final Map<String, Object> resultMap = new HashMap<>();
      resultMap.put(variant.getCaseName(), variant.getValue().orElse(null));
      return resultMap;
    } else {
      throw new WasmException("Expected WitVariant for variant type, got: " + value.getClass());
    }
  }

  /**
   * Marshals an enum value to WIT.
   *
   * @param value the Java value
   * @param witType the WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalEnumToWit(final Object value, final WitType witType) throws WasmException {
    if (value instanceof String) {
      return new WitEnum((String) value);
    } else if (value instanceof Enum) {
      return new WitEnum(((Enum<?>) value).name());
    } else {
      throw new WasmException("Expected String or Enum for enum type, got: " + value.getClass());
    }
  }

  /**
   * Marshals an enum WIT value to Java.
   *
   * @param value the WIT value
   * @param witType the WIT type
   * @return the Java representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalEnumToJava(final Object value, final WitType witType) throws WasmException {
    if (value instanceof WitEnum) {
      return ((WitEnum) value).getValue();
    } else if (value instanceof String) {
      return value;
    } else {
      throw new WasmException("Expected WitEnum or String for enum type, got: " + value.getClass());
    }
  }

  /**
   * Marshals a flags value to WIT.
   *
   * @param value the Java value
   * @param witType the WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalFlagsToWit(final Object value, final WitType witType) throws WasmException {
    if (value instanceof List) {
      @SuppressWarnings("unchecked")
      final List<String> flagList = (List<String>) value;
      return new WitFlags(flagList);
    } else if (value instanceof String[]) {
      return new WitFlags(List.of((String[]) value));
    } else {
      throw new WasmException(
          "Expected List<String> or String[] for flags type, got: " + value.getClass());
    }
  }

  /**
   * Marshals a flags WIT value to Java.
   *
   * @param value the WIT value
   * @param witType the WIT type
   * @return the Java representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalFlagsToJava(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof WitFlags) {
      return ((WitFlags) value).getFlags();
    } else if (value instanceof List) {
      return value;
    } else {
      throw new WasmException("Expected WitFlags or List for flags type, got: " + value.getClass());
    }
  }

  /**
   * Marshals a list value to WIT.
   *
   * @param value the Java value
   * @param witType the WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalListToWit(final Object value, final WitType witType) throws WasmException {
    if (value instanceof List) {
      return new WitList((List<?>) value);
    } else if (value.getClass().isArray()) {
      return new WitList(List.of((Object[]) value));
    } else {
      throw new WasmException("Expected List or array for list type, got: " + value.getClass());
    }
  }

  /**
   * Marshals a list WIT value to Java.
   *
   * @param value the WIT value
   * @param witType the WIT type
   * @return the Java representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalListToJava(final Object value, final WitType witType) throws WasmException {
    if (value instanceof WitList) {
      return ((WitList) value).getElements();
    } else if (value instanceof List) {
      return value;
    } else {
      throw new WasmException("Expected WitList or List for list type, got: " + value.getClass());
    }
  }

  /**
   * Marshals an option value to WIT.
   *
   * @param value the Java value
   * @param witType the WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalOptionToWit(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof Optional) {
      return new WitOption((Optional<?>) value);
    } else {
      return new WitOption(Optional.ofNullable(value));
    }
  }

  /**
   * Marshals an option WIT value to Java.
   *
   * @param value the WIT value
   * @param witType the WIT type
   * @return the Java representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalOptionToJava(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof WitOption) {
      return ((WitOption) value).getValue();
    } else if (value instanceof Optional) {
      return value;
    } else {
      throw new WasmException(
          "Expected WitOption or Optional for option type, got: " + value.getClass());
    }
  }

  /**
   * Marshals a result value to WIT.
   *
   * @param value the Java value
   * @param witType the WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalResultToWit(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof WitResult) {
      return value;
    } else {
      // Assume success value
      return WitResult.ok(value);
    }
  }

  /**
   * Marshals a result WIT value to Java.
   *
   * @param value the WIT value
   * @param witType the WIT type
   * @return the Java representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalResultToJava(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof WitResult) {
      final WitResult result = (WitResult) value;
      if (result.isOk()) {
        return result.getOkValue();
      } else {
        throw new WasmException("Result contains error: " + result.getErrorValue());
      }
    } else {
      throw new WasmException("Expected WitResult for result type, got: " + value.getClass());
    }
  }

  /**
   * Marshals a resource value to WIT.
   *
   * @param value the Java value
   * @param witType the WIT type
   * @return the WIT representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalResourceToWit(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof WitResource) {
      return value;
    } else {
      // Create resource handle
      return new WitResource(value.hashCode(), value);
    }
  }

  /**
   * Marshals a resource WIT value to Java.
   *
   * @param value the WIT value
   * @param witType the WIT type
   * @return the Java representation
   * @throws WasmException if marshaling fails
   */
  private Object marshalResourceToJava(final Object value, final WitType witType)
      throws WasmException {
    if (value instanceof WitResource) {
      return ((WitResource) value).getValue();
    } else {
      throw new WasmException("Expected WitResource for resource type, got: " + value.getClass());
    }
  }

  /**
   * Marshals a Java object to a WIT record using reflection.
   *
   * @param obj the Java object
   * @return the WIT record
   */
  private WitRecord marshalObjectToRecord(final Object obj) {
    final Map<String, Object> fields = new HashMap<>();

    // Use reflection to extract public fields and getters
    final Class<?> objClass = obj.getClass();

    // Extract public fields
    for (final java.lang.reflect.Field field : objClass.getFields()) {
      try {
        final Object fieldValue = field.get(obj);
        fields.put(field.getName(), fieldValue);
      } catch (final IllegalAccessException e) {
        // Skip inaccessible fields
      }
    }

    // Extract getter methods
    for (final java.lang.reflect.Method method : objClass.getMethods()) {
      if (isGetter(method)) {
        try {
          final Object value = method.invoke(obj);
          final String fieldName = getFieldNameFromGetter(method.getName());
          fields.put(fieldName, value);
        } catch (final Exception e) {
          // Skip failed method invocations
        }
      }
    }

    return new WitRecord(fields);
  }

  /**
   * Checks if a method is a getter.
   *
   * @param method the method
   * @return true if getter, false otherwise
   */
  private boolean isGetter(final java.lang.reflect.Method method) {
    final String name = method.getName();
    return (name.startsWith("get") || name.startsWith("is"))
        && method.getParameterCount() == 0
        && !name.equals("getClass");
  }

  /**
   * Extracts field name from getter method name.
   *
   * @param getterName the getter method name
   * @return the field name
   */
  private String getFieldNameFromGetter(final String getterName) {
    String fieldName;
    if (getterName.startsWith("get")) {
      fieldName = getterName.substring(3);
    } else if (getterName.startsWith("is")) {
      fieldName = getterName.substring(2);
    } else {
      return getterName;
    }

    // Convert first character to lowercase
    if (!fieldName.isEmpty()) {
      fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    return fieldName;
  }

  /**
   * Gets the converter key for a WIT type.
   *
   * @param witType the WIT type
   * @return the converter key
   */
  private String getConverterKey(final WitType witType) {
    return witType.getKind().getCategory() + ":" + witType.getName();
  }

  /** Initializes built-in value converters. */
  private void initializeBuiltInConverters() {
    // No built-in converters needed for basic marshaling
    // Custom converters can be registered as needed
  }

  /** Interface for value converters. */
  public interface ValueConverter {
    /**
     * Converts Java value to WIT representation.
     *
     * @param value the Java value
     * @return the WIT representation
     */
    Object toWit(Object value);

    /**
     * Converts WIT representation to Java value.
     *
     * @param value the WIT representation
     * @return the Java value
     */
    Object fromWit(Object value);
  }

  // WIT value wrapper classes

  /** WIT record representation. */
  public static final class WitRecord {
    private final Map<String, Object> fields;

    public WitRecord(final Map<String, Object> fields) {
      this.fields = Map.copyOf(fields);
    }

    public Map<String, Object> getFields() {
      return new java.util.HashMap<>(fields);
    }
  }

  /** WIT variant representation. */
  public static final class WitVariant {
    private final String caseName;
    private final Optional<Object> value;

    public WitVariant(final String caseName, final Optional<Object> value) {
      this.caseName = Objects.requireNonNull(caseName);
      this.value = Objects.requireNonNull(value);
    }

    public String getCaseName() {
      return caseName;
    }

    public Optional<Object> getValue() {
      return value;
    }
  }

  /** WIT enum representation. */
  public static final class WitEnum {
    private final String value;

    public WitEnum(final String value) {
      this.value = Objects.requireNonNull(value);
    }

    public String getValue() {
      return value;
    }
  }

  /** WIT flags representation. */
  public static final class WitFlags {
    private final List<String> flags;

    public WitFlags(final List<String> flags) {
      this.flags = List.copyOf(flags);
    }

    public List<String> getFlags() {
      return new java.util.ArrayList<>(flags);
    }
  }

  /** WIT list representation. */
  public static final class WitList {
    private final List<?> elements;

    public WitList(final List<?> elements) {
      this.elements = List.copyOf(elements);
    }

    public List<?> getElements() {
      return new java.util.ArrayList<>(elements);
    }
  }

  /** WIT option representation. */
  public static final class WitOption {
    private final Optional<?> value;

    public WitOption(final Optional<?> value) {
      this.value = Objects.requireNonNull(value);
    }

    public Optional<?> getValue() {
      return value;
    }
  }

  /** WIT result representation. */
  public static final class WitResult {
    private final boolean isOk;
    private final Object value;

    private WitResult(final boolean isOk, final Object value) {
      this.isOk = isOk;
      this.value = value;
    }

    public static WitResult ok(final Object value) {
      return new WitResult(true, value);
    }

    public static WitResult error(final Object error) {
      return new WitResult(false, error);
    }

    public boolean isOk() {
      return isOk;
    }

    public boolean isError() {
      return !isOk;
    }

    public Object getOkValue() {
      return isOk ? value : null;
    }

    public Object getErrorValue() {
      return !isOk ? value : null;
    }
  }

  /** WIT resource representation. */
  public static final class WitResource {
    private final int handle;
    private final Object value;

    public WitResource(final int handle, final Object value) {
      this.handle = handle;
      this.value = value;
    }

    public int getHandle() {
      return handle;
    }

    public Object getValue() {
      return value;
    }
  }
}
