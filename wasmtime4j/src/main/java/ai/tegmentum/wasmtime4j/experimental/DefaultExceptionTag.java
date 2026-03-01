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
package ai.tegmentum.wasmtime4j.experimental;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link ExceptionHandler.ExceptionTag}.
 *
 * <p>Represents a WebAssembly exception tag with a unique identifier, name, and parameter types.
 *
 * @since 1.0.0
 */
public final class DefaultExceptionTag implements ExceptionHandler.ExceptionTag {

  private final long tagHandle;
  private final String tagName;
  private final List<WasmValueType> parameterTypes;
  private final boolean gcAware;

  /**
   * Creates a new exception tag.
   *
   * @param tagHandle the unique handle for this tag
   * @param tagName the tag name
   * @param parameterTypes the parameter types
   * @param gcAware whether this tag supports GC references
   * @throws NullPointerException if tagName or parameterTypes is null
   * @throws IllegalArgumentException if tagName is empty
   */
  public DefaultExceptionTag(
      final long tagHandle,
      final String tagName,
      final List<WasmValueType> parameterTypes,
      final boolean gcAware) {
    Objects.requireNonNull(tagName, "Tag name cannot be null");
    Objects.requireNonNull(parameterTypes, "Parameter types cannot be null");
    if (tagName.trim().isEmpty()) {
      throw new IllegalArgumentException("Tag name cannot be empty");
    }
    this.tagHandle = tagHandle;
    this.tagName = tagName.trim();
    this.parameterTypes = Collections.unmodifiableList(parameterTypes);
    this.gcAware = gcAware;
  }

  /**
   * Creates a new exception tag without GC support.
   *
   * @param tagHandle the unique handle for this tag
   * @param tagName the tag name
   * @param parameterTypes the parameter types
   * @throws NullPointerException if tagName or parameterTypes is null
   * @throws IllegalArgumentException if tagName is empty
   */
  public DefaultExceptionTag(
      final long tagHandle, final String tagName, final List<WasmValueType> parameterTypes) {
    this(tagHandle, tagName, parameterTypes, false);
  }

  @Override
  public long getTagHandle() {
    return tagHandle;
  }

  @Override
  public String getTagName() {
    return tagName;
  }

  @Override
  public String getTagType() {
    if (parameterTypes.isEmpty()) {
      return "[]";
    }
    final StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < parameterTypes.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(parameterTypes.get(i).name());
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public List<WasmValueType> getParameterTypes() {
    return parameterTypes;
  }

  @Override
  public boolean isGcAware() {
    return gcAware;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DefaultExceptionTag)) {
      return false;
    }
    final DefaultExceptionTag other = (DefaultExceptionTag) obj;
    return tagHandle == other.tagHandle;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(tagHandle);
  }

  @Override
  public String toString() {
    return "DefaultExceptionTag{"
        + "handle="
        + tagHandle
        + ", name='"
        + tagName
        + '\''
        + ", types="
        + getTagType()
        + ", gcAware="
        + gcAware
        + '}';
  }
}
