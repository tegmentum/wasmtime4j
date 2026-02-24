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

package ai.tegmentum.wasmtime4j.type;

import java.util.Objects;
import java.util.Optional;

/**
 * A tagged union pairing an {@link ExternType} discriminant with the full {@link WasmType} data.
 *
 * <p>This provides type-safe access to the detailed type information for each kind of WebAssembly
 * external value (function, table, memory, global, tag).
 *
 * @since 1.0.0
 */
public final class ExternTypeInfo {

  private final ExternType kind;
  private final WasmType detailedType;

  private ExternTypeInfo(final ExternType kind, final WasmType detailedType) {
    this.kind = Objects.requireNonNull(kind, "kind cannot be null");
    this.detailedType = Objects.requireNonNull(detailedType, "detailedType cannot be null");
  }

  /**
   * Creates an ExternTypeInfo for a function type.
   *
   * @param funcType the function type
   * @return the extern type info
   */
  public static ExternTypeInfo forFunc(final FuncType funcType) {
    return new ExternTypeInfo(ExternType.FUNC, Objects.requireNonNull(funcType));
  }

  /**
   * Creates an ExternTypeInfo for a table type.
   *
   * @param tableType the table type
   * @return the extern type info
   */
  public static ExternTypeInfo forTable(final TableType tableType) {
    return new ExternTypeInfo(ExternType.TABLE, Objects.requireNonNull(tableType));
  }

  /**
   * Creates an ExternTypeInfo for a memory type.
   *
   * @param memoryType the memory type
   * @return the extern type info
   */
  public static ExternTypeInfo forMemory(final MemoryType memoryType) {
    return new ExternTypeInfo(ExternType.MEMORY, Objects.requireNonNull(memoryType));
  }

  /**
   * Creates an ExternTypeInfo for a global type.
   *
   * @param globalType the global type
   * @return the extern type info
   */
  public static ExternTypeInfo forGlobal(final GlobalType globalType) {
    return new ExternTypeInfo(ExternType.GLOBAL, Objects.requireNonNull(globalType));
  }

  /**
   * Creates an ExternTypeInfo for a tag type.
   *
   * @param tagType the tag type
   * @return the extern type info
   */
  public static ExternTypeInfo forTag(final TagType tagType) {
    Objects.requireNonNull(tagType, "tagType cannot be null");
    // TagType doesn't extend WasmType, so we use its underlying FunctionType
    return new ExternTypeInfo(ExternType.TAG, tagType.getFunctionType());
  }

  /**
   * Gets the extern type kind discriminant.
   *
   * @return the extern type kind
   */
  public ExternType getKind() {
    return kind;
  }

  /**
   * Gets the detailed type data.
   *
   * @return the full type information
   */
  public WasmType getDetailedType() {
    return detailedType;
  }

  /**
   * Extracts the function type if this is a function extern.
   *
   * @return the function type, or empty if this is not a function
   */
  public Optional<FuncType> asFuncType() {
    if (kind == ExternType.FUNC && detailedType instanceof FuncType) {
      return Optional.of((FuncType) detailedType);
    }
    return Optional.empty();
  }

  /**
   * Extracts the table type if this is a table extern.
   *
   * @return the table type, or empty if this is not a table
   */
  public Optional<TableType> asTableType() {
    if (kind == ExternType.TABLE && detailedType instanceof TableType) {
      return Optional.of((TableType) detailedType);
    }
    return Optional.empty();
  }

  /**
   * Extracts the memory type if this is a memory extern.
   *
   * @return the memory type, or empty if this is not a memory
   */
  public Optional<MemoryType> asMemoryType() {
    if (kind == ExternType.MEMORY && detailedType instanceof MemoryType) {
      return Optional.of((MemoryType) detailedType);
    }
    return Optional.empty();
  }

  /**
   * Extracts the global type if this is a global extern.
   *
   * @return the global type, or empty if this is not a global
   */
  public Optional<GlobalType> asGlobalType() {
    if (kind == ExternType.GLOBAL && detailedType instanceof GlobalType) {
      return Optional.of((GlobalType) detailedType);
    }
    return Optional.empty();
  }

  /**
   * Extracts the tag type (as a FuncType) if this is a tag extern.
   *
   * @return the tag's function type, or empty if this is not a tag
   */
  public Optional<FuncType> asTagType() {
    if (kind == ExternType.TAG && detailedType instanceof FuncType) {
      return Optional.of((FuncType) detailedType);
    }
    return Optional.empty();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ExternTypeInfo)) {
      return false;
    }
    final ExternTypeInfo other = (ExternTypeInfo) obj;
    return kind == other.kind && Objects.equals(detailedType, other.detailedType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, detailedType);
  }

  @Override
  public String toString() {
    return "ExternTypeInfo{kind=" + kind + ", type=" + detailedType + "}";
  }
}
