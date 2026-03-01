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
package ai.tegmentum.wasmtime4j.wit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a WIT tuple value (fixed-size, heterogeneous sequence).
 *
 * <p>Tuples are fixed-size collections where each position can have a different WIT type. Unlike
 * lists, tuples have a fixed number of elements determined at compile time, and each element can
 * have a different type.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a tuple of (string, s32)
 * WitTuple pair = WitTuple.of(
 *     WitString.of("Alice"),
 *     WitS32.of(42)
 * );
 *
 * // Create an empty tuple (unit type)
 * WitTuple unit = WitTuple.empty();
 *
 * // Create with explicit element types
 * WitTuple typed = WitTuple.builder()
 *     .add(WitType.createString(), WitString.of("value"))
 *     .add(WitType.createS32(), WitS32.of(123))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WitTuple extends WitValue {

  private final List<WitValue> elements;
  private final List<WitType> elementTypes;

  /**
   * Creates a new WIT tuple value.
   *
   * @param elementTypes the types of elements in this tuple
   * @param elements the tuple elements (must match elementTypes in count and type)
   */
  private WitTuple(final List<WitType> elementTypes, final List<WitValue> elements) {
    super(WitType.tuple(elementTypes));
    if (elementTypes == null) {
      throw new IllegalArgumentException("Element types cannot be null");
    }
    if (elements == null) {
      throw new IllegalArgumentException("Elements cannot be null");
    }
    if (elementTypes.size() != elements.size()) {
      throw new IllegalArgumentException(
          String.format(
              "Element types count (%d) must match elements count (%d)",
              elementTypes.size(), elements.size()));
    }
    this.elementTypes = new ArrayList<>(elementTypes);
    // Defensive copy
    this.elements = new ArrayList<>(elements);
    validate();
  }

  /**
   * Creates a WIT tuple from a variable number of elements. The element types are inferred from
   * each element.
   *
   * @param elements the tuple elements
   * @return a WIT tuple value
   */
  public static WitTuple of(final WitValue... elements) {
    if (elements == null) {
      throw new IllegalArgumentException("Elements cannot be null");
    }
    final List<WitType> types = new ArrayList<>(elements.length);
    final List<WitValue> values = new ArrayList<>(elements.length);
    for (final WitValue element : elements) {
      if (element == null) {
        throw new IllegalArgumentException("Tuple element cannot be null");
      }
      types.add(element.getType());
      values.add(element);
    }
    return new WitTuple(types, values);
  }

  /**
   * Creates a WIT tuple from a Java List. The element types are inferred from each element.
   *
   * @param elements the tuple elements
   * @return a WIT tuple value
   */
  public static WitTuple of(final List<WitValue> elements) {
    if (elements == null) {
      throw new IllegalArgumentException("Elements cannot be null");
    }
    final List<WitType> types = new ArrayList<>(elements.size());
    for (final WitValue element : elements) {
      if (element == null) {
        throw new IllegalArgumentException("Tuple element cannot be null");
      }
      types.add(element.getType());
    }
    return new WitTuple(types, elements);
  }

  /**
   * Creates an empty WIT tuple (unit type).
   *
   * @return an empty WIT tuple value
   */
  public static WitTuple empty() {
    return new WitTuple(Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Creates a builder for constructing WIT tuples.
   *
   * @return a new tuple builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the element types of this tuple.
   *
   * @return an unmodifiable list of element types
   */
  public List<WitType> getElementTypes() {
    return Collections.unmodifiableList(elementTypes);
  }

  /**
   * Gets the elements in this tuple.
   *
   * @return an unmodifiable list of elements
   */
  public List<WitValue> getElements() {
    return Collections.unmodifiableList(elements);
  }

  /**
   * Gets the element at the specified index.
   *
   * @param index the element index
   * @return the element at the index
   * @throws IndexOutOfBoundsException if index is out of range
   */
  public WitValue get(final int index) {
    return elements.get(index);
  }

  /**
   * Gets the element type at the specified index.
   *
   * @param index the element index
   * @return the element type at the index
   * @throws IndexOutOfBoundsException if index is out of range
   */
  public WitType getTypeAt(final int index) {
    return elementTypes.get(index);
  }

  /**
   * Gets the number of elements in this tuple.
   *
   * @return the element count
   */
  public int size() {
    return elements.size();
  }

  /**
   * Checks if this tuple is empty (unit type).
   *
   * @return true if the tuple has no elements
   */
  public boolean isEmpty() {
    return elements.isEmpty();
  }

  @Override
  public List<Object> toJava() {
    return elements.stream().map(WitValue::toJava).collect(Collectors.toList());
  }

  @Override
  protected void validate() {
    // Validate that all elements match their declared types
    for (int i = 0; i < elements.size(); i++) {
      final WitValue element = elements.get(i);
      final WitType expectedType = elementTypes.get(i);
      if (element == null) {
        throw new IllegalArgumentException("Tuple element at index " + i + " cannot be null");
      }
      if (!element.getType().equals(expectedType)) {
        throw new IllegalArgumentException(
            String.format(
                "Tuple element at index %d has type %s but expected %s",
                i, element.getType().getName(), expectedType.getName()));
      }
    }
  }

  @Override
  public String toString() {
    return "WitTuple" + elements;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitTuple)) {
      return false;
    }
    final WitTuple other = (WitTuple) obj;
    return elementTypes.equals(other.elementTypes) && elements.equals(other.elements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), elementTypes, elements);
  }

  /** Builder for constructing WIT tuples with a fluent API. */
  public static final class Builder {
    private final List<WitType> elementTypes = new ArrayList<>();
    private final List<WitValue> elements = new ArrayList<>();

    private Builder() {}

    /**
     * Adds an element to the tuple with explicit type.
     *
     * @param type the element type
     * @param element the element to add (must not be null and must match type)
     * @return this builder
     * @throws IllegalArgumentException if element is null or type doesn't match
     */
    public Builder add(final WitType type, final WitValue element) {
      if (type == null) {
        throw new IllegalArgumentException("Type cannot be null");
      }
      if (element == null) {
        throw new IllegalArgumentException("Element cannot be null");
      }
      if (!element.getType().equals(type)) {
        throw new IllegalArgumentException(
            String.format(
                "Element type %s does not match declared type %s",
                element.getType().getName(), type.getName()));
      }
      elementTypes.add(type);
      elements.add(element);
      return this;
    }

    /**
     * Adds an element to the tuple, inferring the type from the element.
     *
     * @param element the element to add (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if element is null
     */
    public Builder add(final WitValue element) {
      if (element == null) {
        throw new IllegalArgumentException("Element cannot be null");
      }
      elementTypes.add(element.getType());
      elements.add(element);
      return this;
    }

    /**
     * Builds the WIT tuple.
     *
     * @return the constructed WIT tuple
     */
    public WitTuple build() {
      return new WitTuple(elementTypes, elements);
    }
  }
}
