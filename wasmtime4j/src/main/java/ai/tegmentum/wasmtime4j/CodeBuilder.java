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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for constructing WebAssembly modules programmatically.
 *
 * <p>CodeBuilder provides a fluent API for building WebAssembly modules without writing raw bytes
 * or WAT text. It generates valid WebAssembly binary format that can be compiled by an engine.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * byte[] wasmBytes = new CodeBuilder()
 *     .addFunction("add", FuncType.of(
 *         List.of(WasmValueType.I32, WasmValueType.I32),
 *         List.of(WasmValueType.I32)))
 *     .addExport("add", ExportKind.FUNCTION, 0)
 *     .build();
 *
 * Module module = engine.compileModule(wasmBytes);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class CodeBuilder {

  private final List<TypeEntry> types;
  private final List<FunctionEntry> functions;
  private final List<ExportEntry> exports;
  private final List<ImportEntry> imports;
  private final List<MemoryEntry> memories;
  private final List<TableEntry> tables;
  private final List<GlobalEntry> globals;

  /** Creates a new CodeBuilder. */
  public CodeBuilder() {
    this.types = new ArrayList<>();
    this.functions = new ArrayList<>();
    this.exports = new ArrayList<>();
    this.imports = new ArrayList<>();
    this.memories = new ArrayList<>();
    this.tables = new ArrayList<>();
    this.globals = new ArrayList<>();
  }

  /**
   * Adds a type definition.
   *
   * @param funcType the function type
   * @return this builder
   */
  public CodeBuilder addType(final FuncType funcType) {
    Objects.requireNonNull(funcType, "funcType cannot be null");
    types.add(new TypeEntry(funcType));
    return this;
  }

  /**
   * Adds a function import.
   *
   * @param moduleName the module name
   * @param fieldName the field name
   * @param typeIndex the type index
   * @return this builder
   */
  public CodeBuilder addFunctionImport(
      final String moduleName, final String fieldName, final int typeIndex) {
    Objects.requireNonNull(moduleName, "moduleName cannot be null");
    Objects.requireNonNull(fieldName, "fieldName cannot be null");
    imports.add(new ImportEntry(moduleName, fieldName, ImportKind.FUNCTION, typeIndex));
    return this;
  }

  /**
   * Adds a memory import.
   *
   * @param moduleName the module name
   * @param fieldName the field name
   * @param minPages the minimum pages
   * @param maxPages the maximum pages (-1 for no maximum)
   * @return this builder
   */
  public CodeBuilder addMemoryImport(
      final String moduleName, final String fieldName, final int minPages, final int maxPages) {
    Objects.requireNonNull(moduleName, "moduleName cannot be null");
    Objects.requireNonNull(fieldName, "fieldName cannot be null");
    imports.add(
        new ImportEntry(
            moduleName, fieldName, ImportKind.MEMORY, encodeMemoryLimits(minPages, maxPages)));
    return this;
  }

  /**
   * Adds a global import.
   *
   * @param moduleName the module name
   * @param fieldName the field name
   * @param valueType the value type of the global
   * @param mutable whether the global is mutable
   * @return this builder
   */
  public CodeBuilder addGlobalImport(
      final String moduleName,
      final String fieldName,
      final WasmValueType valueType,
      final boolean mutable) {
    Objects.requireNonNull(moduleName, "moduleName cannot be null");
    Objects.requireNonNull(fieldName, "fieldName cannot be null");
    Objects.requireNonNull(valueType, "valueType cannot be null");
    imports.add(
        new ImportEntry(
            moduleName, fieldName, ImportKind.GLOBAL, encodeGlobalType(valueType, mutable)));
    return this;
  }

  /**
   * Adds a function definition.
   *
   * @param typeIndex the type index
   * @param locals the local variable types
   * @param body the function body bytecode
   * @return this builder
   */
  public CodeBuilder addFunction(
      final int typeIndex, final List<WasmValueType> locals, final byte[] body) {
    functions.add(new FunctionEntry(typeIndex, locals, body));
    return this;
  }

  /**
   * Adds a memory definition.
   *
   * @param minPages the minimum pages
   * @param maxPages the maximum pages (-1 for no maximum)
   * @return this builder
   */
  public CodeBuilder addMemory(final int minPages, final int maxPages) {
    memories.add(new MemoryEntry(minPages, maxPages));
    return this;
  }

  /**
   * Adds a table definition.
   *
   * @param elementType the element type
   * @param minElements the minimum elements
   * @param maxElements the maximum elements (-1 for no maximum)
   * @return this builder
   */
  public CodeBuilder addTable(
      final WasmValueType elementType, final int minElements, final int maxElements) {
    tables.add(new TableEntry(elementType, minElements, maxElements));
    return this;
  }

  /**
   * Adds a global definition.
   *
   * @param valueType the value type
   * @param mutable whether the global is mutable
   * @param initValue the initial value
   * @return this builder
   */
  public CodeBuilder addGlobal(
      final WasmValueType valueType, final boolean mutable, final long initValue) {
    globals.add(new GlobalEntry(valueType, mutable, initValue));
    return this;
  }

  /**
   * Adds an export.
   *
   * @param name the export name
   * @param kind the export kind
   * @param index the index of the exported item
   * @return this builder
   */
  public CodeBuilder addExport(final String name, final ExportKind kind, final int index) {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(kind, "kind cannot be null");
    exports.add(new ExportEntry(name, kind, index));
    return this;
  }

  /**
   * Builds the WebAssembly binary.
   *
   * @return the WebAssembly bytecode
   * @throws WasmException if building fails
   */
  public byte[] build() throws WasmException {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      writeTo(baos);
      return baos.toByteArray();
    } catch (IOException e) {
      throw new WasmException("Failed to build WebAssembly module", e);
    }
  }

  /**
   * Writes the WebAssembly binary to an output stream.
   *
   * @param out the output stream
   * @throws IOException if writing fails
   */
  public void writeTo(final OutputStream out) throws IOException {
    // Magic number
    out.write(new byte[] {0x00, 0x61, 0x73, 0x6D});
    // Version
    out.write(new byte[] {0x01, 0x00, 0x00, 0x00});

    // Type section (1)
    if (!types.isEmpty()) {
      writeTypeSection(out);
    }

    // Import section (2)
    if (!imports.isEmpty()) {
      writeImportSection(out);
    }

    // Function section (3)
    if (!functions.isEmpty()) {
      writeFunctionSection(out);
    }

    // Table section (4)
    if (!tables.isEmpty()) {
      writeTableSection(out);
    }

    // Memory section (5)
    if (!memories.isEmpty()) {
      writeMemorySection(out);
    }

    // Global section (6)
    if (!globals.isEmpty()) {
      writeGlobalSection(out);
    }

    // Export section (7)
    if (!exports.isEmpty()) {
      writeExportSection(out);
    }

    // Code section (10)
    if (!functions.isEmpty()) {
      writeCodeSection(out);
    }
  }

  /**
   * Writes the WebAssembly binary to a file.
   *
   * @param path the output file path
   * @throws WasmException if writing fails
   */
  public void writeTo(final Path path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    try {
      Files.write(path, build());
    } catch (IOException e) {
      throw new WasmException("Failed to write module to " + path, e);
    }
  }

  private void writeTypeSection(final OutputStream out) throws IOException {
    ByteArrayOutputStream section = new ByteArrayOutputStream();
    writeUnsignedLeb128(section, types.size());
    for (TypeEntry type : types) {
      section.write(0x60); // func type
      writeUnsignedLeb128(section, type.funcType.getParams().size());
      for (WasmValueType param : type.funcType.getParams()) {
        section.write(getValueTypeByte(param));
      }
      writeUnsignedLeb128(section, type.funcType.getResults().size());
      for (WasmValueType result : type.funcType.getResults()) {
        section.write(getValueTypeByte(result));
      }
    }
    writeSection(out, 1, section.toByteArray());
  }

  private void writeImportSection(final OutputStream out) throws IOException {
    ByteArrayOutputStream section = new ByteArrayOutputStream();
    writeUnsignedLeb128(section, imports.size());
    for (ImportEntry imp : imports) {
      writeName(section, imp.moduleName);
      writeName(section, imp.fieldName);
      section.write(imp.kind.ordinal());
      if (imp.kind == ImportKind.MEMORY) {
        // Decode min/max from encoded value
        int encodedLimits = imp.typeIndex;
        int min = encodedLimits & 0xFFFF;
        int maxEncoded = (encodedLimits >> 16) & 0xFFFF;
        int max = (maxEncoded == 0) ? -1 : maxEncoded;
        writeLimits(section, min, max);
      } else if (imp.kind == ImportKind.GLOBAL) {
        // Decode value type and mutability from encoded value
        int encodedGlobal = imp.typeIndex;
        int valType = encodedGlobal & 0xFF;
        boolean mutable = (encodedGlobal & 0x100) != 0;
        section.write(valType);
        section.write(mutable ? 0x01 : 0x00);
      } else {
        writeUnsignedLeb128(section, imp.typeIndex);
      }
    }
    writeSection(out, 2, section.toByteArray());
  }

  private void writeFunctionSection(final OutputStream out) throws IOException {
    ByteArrayOutputStream section = new ByteArrayOutputStream();
    writeUnsignedLeb128(section, functions.size());
    for (FunctionEntry func : functions) {
      writeUnsignedLeb128(section, func.typeIndex);
    }
    writeSection(out, 3, section.toByteArray());
  }

  private void writeTableSection(final OutputStream out) throws IOException {
    ByteArrayOutputStream section = new ByteArrayOutputStream();
    writeUnsignedLeb128(section, tables.size());
    for (TableEntry table : tables) {
      section.write(getValueTypeByte(table.elementType));
      writeLimits(section, table.minElements, table.maxElements);
    }
    writeSection(out, 4, section.toByteArray());
  }

  private void writeMemorySection(final OutputStream out) throws IOException {
    ByteArrayOutputStream section = new ByteArrayOutputStream();
    writeUnsignedLeb128(section, memories.size());
    for (MemoryEntry mem : memories) {
      writeLimits(section, mem.minPages, mem.maxPages);
    }
    writeSection(out, 5, section.toByteArray());
  }

  private void writeGlobalSection(final OutputStream out) throws IOException {
    ByteArrayOutputStream section = new ByteArrayOutputStream();
    writeUnsignedLeb128(section, globals.size());
    for (GlobalEntry global : globals) {
      section.write(getValueTypeByte(global.valueType));
      section.write(global.mutable ? 1 : 0);
      writeConstExpr(section, global.valueType, global.initValue);
    }
    writeSection(out, 6, section.toByteArray());
  }

  private void writeExportSection(final OutputStream out) throws IOException {
    ByteArrayOutputStream section = new ByteArrayOutputStream();
    writeUnsignedLeb128(section, exports.size());
    for (ExportEntry export : exports) {
      writeName(section, export.name);
      section.write(export.kind.ordinal());
      writeUnsignedLeb128(section, export.index);
    }
    writeSection(out, 7, section.toByteArray());
  }

  private void writeCodeSection(final OutputStream out) throws IOException {
    ByteArrayOutputStream section = new ByteArrayOutputStream();
    writeUnsignedLeb128(section, functions.size());
    for (FunctionEntry func : functions) {
      ByteArrayOutputStream funcBody = new ByteArrayOutputStream();
      writeUnsignedLeb128(funcBody, func.locals.size());
      for (WasmValueType local : func.locals) {
        writeUnsignedLeb128(funcBody, 1);
        funcBody.write(getValueTypeByte(local));
      }
      funcBody.write(func.body);
      funcBody.write(0x0B); // end

      writeUnsignedLeb128(section, funcBody.size());
      section.write(funcBody.toByteArray());
    }
    writeSection(out, 10, section.toByteArray());
  }

  private void writeSection(final OutputStream out, final int sectionId, final byte[] content)
      throws IOException {
    out.write(sectionId);
    writeUnsignedLeb128(out, content.length);
    out.write(content);
  }

  private void writeName(final OutputStream out, final String name) throws IOException {
    byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
    writeUnsignedLeb128(out, bytes.length);
    out.write(bytes);
  }

  private void writeLimits(final OutputStream out, final int min, final int max)
      throws IOException {
    if (max < 0) {
      out.write(0x00);
      writeUnsignedLeb128(out, min);
    } else {
      out.write(0x01);
      writeUnsignedLeb128(out, min);
      writeUnsignedLeb128(out, max);
    }
  }

  private void writeConstExpr(final OutputStream out, final WasmValueType type, final long value)
      throws IOException {
    switch (type) {
      case I32:
        out.write(0x41); // i32.const
        writeSignedLeb128(out, (int) value);
        break;
      case I64:
        out.write(0x42); // i64.const
        writeSignedLeb128Long(out, value);
        break;
      case F32:
        out.write(0x43); // f32.const
        int bits = Float.floatToRawIntBits((float) value);
        out.write(bits & 0xFF);
        out.write((bits >> 8) & 0xFF);
        out.write((bits >> 16) & 0xFF);
        out.write((bits >> 24) & 0xFF);
        break;
      case F64:
        out.write(0x44); // f64.const
        long dbits = Double.doubleToRawLongBits((double) value);
        for (int i = 0; i < 8; i++) {
          out.write((int) ((dbits >> (i * 8)) & 0xFF));
        }
        break;
      default:
        out.write(0x41); // i32.const
        out.write(0x00);
    }
    out.write(0x0B); // end
  }

  private void writeUnsignedLeb128(final OutputStream out, final int value) throws IOException {
    int remaining = value;
    do {
      int b = remaining & 0x7F;
      remaining >>>= 7;
      if (remaining != 0) {
        b |= 0x80;
      }
      out.write(b);
    } while (remaining != 0);
  }

  private void writeSignedLeb128(final OutputStream out, final int value) throws IOException {
    int remaining = value;
    boolean more = true;
    while (more) {
      int b = remaining & 0x7F;
      remaining >>= 7;
      if ((remaining == 0 && (b & 0x40) == 0) || (remaining == -1 && (b & 0x40) != 0)) {
        more = false;
      } else {
        b |= 0x80;
      }
      out.write(b);
    }
  }

  private void writeSignedLeb128Long(final OutputStream out, final long value) throws IOException {
    long remaining = value;
    boolean more = true;
    while (more) {
      int b = (int) (remaining & 0x7F);
      remaining >>= 7;
      if ((remaining == 0 && (b & 0x40) == 0) || (remaining == -1 && (b & 0x40) != 0)) {
        more = false;
      } else {
        b |= 0x80;
      }
      out.write(b);
    }
  }

  private int getValueTypeByte(final WasmValueType type) {
    switch (type) {
      case I32:
        return 0x7F;
      case I64:
        return 0x7E;
      case F32:
        return 0x7D;
      case F64:
        return 0x7C;
      case V128:
        return 0x7B;
      case FUNCREF:
        return 0x70;
      case EXTERNREF:
        return 0x6F;
      default:
        return 0x7F;
    }
  }

  private int encodeMemoryLimits(final int min, final int max) {
    return (max < 0) ? min : (min | (max << 16));
  }

  private int encodeGlobalType(final WasmValueType valueType, final boolean mutable) {
    // Encode value type in low byte, mutability in high byte
    return getValueTypeByte(valueType) | (mutable ? 0x100 : 0);
  }

  // Internal entry types
  private static class TypeEntry {
    final FuncType funcType;

    TypeEntry(final FuncType funcType) {
      this.funcType = funcType;
    }
  }

  private static class FunctionEntry {
    final int typeIndex;
    final List<WasmValueType> locals;
    final byte[] body;

    FunctionEntry(final int typeIndex, final List<WasmValueType> locals, final byte[] body) {
      this.typeIndex = typeIndex;
      this.locals = new ArrayList<>(locals);
      this.body = body.clone();
    }
  }

  private static class ImportEntry {
    final String moduleName;
    final String fieldName;
    final ImportKind kind;
    final int typeIndex;

    ImportEntry(
        final String moduleName,
        final String fieldName,
        final ImportKind kind,
        final int typeIndex) {
      this.moduleName = moduleName;
      this.fieldName = fieldName;
      this.kind = kind;
      this.typeIndex = typeIndex;
    }
  }

  private static class ExportEntry {
    final String name;
    final ExportKind kind;
    final int index;

    ExportEntry(final String name, final ExportKind kind, final int index) {
      this.name = name;
      this.kind = kind;
      this.index = index;
    }
  }

  private static class MemoryEntry {
    final int minPages;
    final int maxPages;

    MemoryEntry(final int minPages, final int maxPages) {
      this.minPages = minPages;
      this.maxPages = maxPages;
    }
  }

  private static class TableEntry {
    final WasmValueType elementType;
    final int minElements;
    final int maxElements;

    TableEntry(final WasmValueType elementType, final int minElements, final int maxElements) {
      this.elementType = elementType;
      this.minElements = minElements;
      this.maxElements = maxElements;
    }
  }

  private static class GlobalEntry {
    final WasmValueType valueType;
    final boolean mutable;
    final long initValue;

    GlobalEntry(final WasmValueType valueType, final boolean mutable, final long initValue) {
      this.valueType = valueType;
      this.mutable = mutable;
      this.initValue = initValue;
    }
  }

  /** Import kinds. */
  public enum ImportKind {
    FUNCTION,
    TABLE,
    MEMORY,
    GLOBAL
  }

  /** Export kinds. */
  public enum ExportKind {
    FUNCTION,
    TABLE,
    MEMORY,
    GLOBAL
  }
}
