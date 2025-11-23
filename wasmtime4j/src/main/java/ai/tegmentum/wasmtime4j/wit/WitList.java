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

import ai.tegmentum.wasmtime4j.WitType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a WIT list value (variable-length array).
 *
 * <p>Lists are homogeneous collections where all elements have the same WIT type. Lists are
 * dynamically sized and preserve element order.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a list of integers
 * WitList numbers = WitList.of(
 *     WitS32.of(1),
 *     WitS32.of(2),
 *     WitS32.of(3)
 * );
 *
 * // Create a list of strings
 * WitList names = WitList.builder(WitType.createString())
 *     .add(WitString.of("Alice"))
 *     .add(WitString.of("Bob"))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WitList extends WitValue {

  private final List<WitValue> elements;
  private final WitType elementType;

  /**
   * Creates a new WIT list value.
   *
   * @param elementType the type of elements in this list
   * @param elements the list elements (must all be of elementType)
   */
  private WitList(final WitType elementType, final List<WitValue> elements) {
    super(WitType.list(elementType));
    if (elementType == null) {
      throw new IllegalArgumentException("Element type cannot be null");
    }
    if (elements == null) {
      throw new IllegalArgumentException("Elements cannot be null");
    }
    this.elementType = elementType;
    // Defensive copy
    this.elements = new ArrayList<>(elements);
    validate();
  }

  /**
   * Creates a WIT list from a variable number of elements. The element type is inferred from the
   * first element.
   *
   * @param elements the list elements (must not be empty, all must have same type)
   * @return a WIT list value
   * @throws IllegalArgumentException if elements is empty or types don't match
   */
  public static WitList of(final WitValue... elements) {
    if (elements == null || elements.length == 0) {
      throw new IllegalArgumentException("List must have at least one element to infer type");
    }
    final WitType elementType = elements[0].getType();
    final List<WitValue> elementList = new ArrayList<>(elements.length);
    for (final WitValue element : elements) {
      elementList.add(element);
    }
    return new WitList(elementType, elementList);
  }

  /**
   * Creates a WIT list from a Java List. The element type is inferred from the first element.
   *
   * @param elements the list elements (must not be empty, all must have same type)
   * @return a WIT list value
   * @throws IllegalArgumentException if elements is empty or types don't match
   */
  public static WitList of(final List<WitValue> elements) {
    if (elements == null || elements.isEmpty()) {
      throw new IllegalArgumentException("List must have at least one element to infer type");
    }
    final WitType elementType = elements.get(0).getType();
    return new WitList(elementType, elements);
  }

  /**
   * Creates an empty WIT list with the specified element type.
   *
   * @param elementType the type of elements this list will contain
   * @return an empty WIT list value
   */
  public static WitList empty(final WitType elementType) {
    return new WitList(elementType, Collections.emptyList());
  }

  /**
   * Creates a builder for constructing WIT lists.
   *
   * @param elementType the type of elements the list will contain
   * @return a new list builder
   */
  public static Builder builder(final WitType elementType) {
    return new Builder(elementType);
  }

  /**
   * Gets the element type of this list.
   *
   * @return the element type
   */
  public WitType getElementType() {
    return elementType;
  }

  /**
   * Gets the elements in this list.
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
   * Gets the number of elements in this list.
   *
   * @return the element count
   */
  public int size() {
    return elements.size();
  }

  /**
   * Checks if this list is empty.
   *
   * @return true if the list has no elements
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
    // Validate that all elements match the declared element type
    for (int i = 0; i < elements.size(); i++) {
      final WitValue element = elements.get(i);
      if (element == null) {
        throw new IllegalArgumentException("List element at index " + i + " cannot be null");
      }
      if (!element.getType().equals(elementType)) {
        throw new IllegalArgumentException(
            String.format(
                "List element at index %d has type %s but expected %s",
                i, element.getType().getName(), elementType.getName()));
      }
    }
  }

  @Override
  public String toString() {
    return "WitList" + elements;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitList)) {
      return false;
    }
    final WitList other = (WitList) obj;
    return elementType.equals(other.elementType) && elements.equals(other.elements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), elementType, elements);
  }

  /** Builder for constructing WIT lists with a fluent API. */
  public static final class Builder {
    private final WitType elementType;
    private final List<WitValue> elements = new ArrayList<>();

    private Builder(final WitType elementType) {
      if (elementType == null) {
        throw new IllegalArgumentException("Element type cannot be null");
      }
      this.elementType = elementType;
    }

    /**
     * Adds an element to the list.
     *
     * @param element the element to add (must not be null and must match element type)
     * @return this builder
     * @throws IllegalArgumentException if element is null or type doesn't match
     */
    public Builder add(final WitValue element) {
      if (element == null) {
        throw new IllegalArgumentException("Element cannot be null");
      }
      if (!element.getType().equals(elementType)) {
        throw new IllegalArgumentException(
            String.format(
                "Element type %s does not match list element type %s",
                element.getType().getName(), elementType.getName()));
      }
      elements.add(element);
      return this;
    }

    /**
     * Adds multiple elements to the list.
     *
     * @param elements the elements to add
     * @return this builder
     */
    public Builder addAll(final List<WitValue> elements) {
      for (final WitValue element : elements) {
        add(element);
      }
      return this;
    }

    /**
     * Builds the WIT list.
     *
     * @return the constructed WIT list
     */
    public WitList build() {
      return new WitList(elementType, elements);
    }
  }
}
