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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the {@link CodeBuilder} interface.
 *
 * <p>Verifies the default file-based methods: wasmBinaryFile, wasmBinaryOrTextFile,
 * dwarfPackageFile, compileTimeBuiltinsBinaryFile, compileTimeBuiltinsBinaryOrTextFile.
 */
@DisplayName("CodeBuilder Tests")
class CodeBuilderTest {

  /**
   * Stub CodeBuilder that records method calls for verification. The abstract methods simply record
   * arguments and return {@code this}.
   */
  private static final class RecordingCodeBuilder implements CodeBuilder {

    byte[] lastWasmBinary;
    byte[] lastWasmBinaryOrText;
    byte[] lastDwarfPackage;
    String lastBuiltinName;
    byte[] lastBuiltinBinary;
    String lastBuiltinBinaryOrTextName;
    byte[] lastBuiltinBinaryOrText;

    @Override
    public CodeBuilder wasmBinary(final byte[] bytes) throws WasmException {
      lastWasmBinary = bytes;
      return this;
    }

    @Override
    public CodeBuilder wasmBinaryOrText(final byte[] bytes) throws WasmException {
      lastWasmBinaryOrText = bytes;
      return this;
    }

    @Override
    public CodeBuilder dwarfPackage(final byte[] bytes) throws WasmException {
      lastDwarfPackage = bytes;
      return this;
    }

    @Override
    public CodeBuilder hint(final CodeHint hint) {
      return this;
    }

    @Override
    public CodeBuilder compileTimeBuiltinsBinary(final String name, final byte[] bytes) {
      lastBuiltinName = name;
      lastBuiltinBinary = bytes;
      return this;
    }

    @Override
    public CodeBuilder compileTimeBuiltinsBinaryOrText(final String name, final byte[] bytes) {
      lastBuiltinBinaryOrTextName = name;
      lastBuiltinBinaryOrText = bytes;
      return this;
    }

    @Override
    public CodeBuilder exposeUnsafeIntrinsics(final String importName) {
      return this;
    }

    @Override
    public Module compileModule() throws WasmException {
      return null;
    }

    @Override
    public byte[] compileModuleSerialized() throws WasmException {
      return new byte[0];
    }

    @Override
    public long compileComponent() throws WasmException {
      return 0;
    }

    @Override
    public byte[] compileComponentSerialized() throws WasmException {
      return new byte[0];
    }

    @Override
    public void close() {
      // no-op
    }
  }

  @Nested
  @DisplayName("wasmBinaryFile Tests")
  class WasmBinaryFileTests {

    @Test
    @DisplayName("should read file and delegate to wasmBinary")
    void shouldReadFileAndDelegateToWasmBinary(@TempDir final Path tempDir) throws Exception {
      final byte[] content = {0x00, 0x61, 0x73, 0x6D};
      final Path file = tempDir.resolve("test.wasm");
      Files.write(file, content);

      final RecordingCodeBuilder builder = new RecordingCodeBuilder();
      final CodeBuilder result = builder.wasmBinaryFile(file);

      assertSame(builder, result, "Should return this for chaining");
      assertArrayEquals(content, builder.lastWasmBinary, "Should pass file bytes to wasmBinary");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null path")
    void shouldThrowForNullPath() {
      final RecordingCodeBuilder builder = new RecordingCodeBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.wasmBinaryFile(null),
          "Should throw IllegalArgumentException for null path");
    }

    @Test
    @DisplayName("should throw IOException for nonexistent file")
    void shouldThrowForNonexistentFile(@TempDir final Path tempDir) {
      final RecordingCodeBuilder builder = new RecordingCodeBuilder();
      final Path missing = tempDir.resolve("nonexistent.wasm");

      assertThrows(
          IOException.class,
          () -> builder.wasmBinaryFile(missing),
          "Should throw IOException for nonexistent file");
    }
  }

  @Nested
  @DisplayName("wasmBinaryOrTextFile Tests")
  class WasmBinaryOrTextFileTests {

    @Test
    @DisplayName("should read file and delegate to wasmBinaryOrText")
    void shouldReadFileAndDelegateToWasmBinaryOrText(@TempDir final Path tempDir) throws Exception {
      final byte[] content = "(module)".getBytes();
      final Path file = tempDir.resolve("test.wat");
      Files.write(file, content);

      final RecordingCodeBuilder builder = new RecordingCodeBuilder();
      final CodeBuilder result = builder.wasmBinaryOrTextFile(file);

      assertSame(builder, result, "Should return this for chaining");
      assertArrayEquals(
          content, builder.lastWasmBinaryOrText, "Should pass file bytes to wasmBinaryOrText");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null path")
    void shouldThrowForNullPath() {
      final RecordingCodeBuilder builder = new RecordingCodeBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.wasmBinaryOrTextFile(null),
          "Should throw IllegalArgumentException for null path");
    }
  }

  @Nested
  @DisplayName("dwarfPackageFile Tests")
  class DwarfPackageFileTests {

    @Test
    @DisplayName("should read file and delegate to dwarfPackage")
    void shouldReadFileAndDelegateToDwarfPackage(@TempDir final Path tempDir) throws Exception {
      final byte[] content = {0x01, 0x02, 0x03};
      final Path file = tempDir.resolve("debug.dwp");
      Files.write(file, content);

      final RecordingCodeBuilder builder = new RecordingCodeBuilder();
      final CodeBuilder result = builder.dwarfPackageFile(file);

      assertSame(builder, result, "Should return this for chaining");
      assertArrayEquals(
          content, builder.lastDwarfPackage, "Should pass file bytes to dwarfPackage");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null path")
    void shouldThrowForNullPath() {
      final RecordingCodeBuilder builder = new RecordingCodeBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.dwarfPackageFile(null),
          "Should throw IllegalArgumentException for null path");
    }
  }

  @Nested
  @DisplayName("compileTimeBuiltinsBinaryFile Tests")
  class CompileTimeBuiltinsBinaryFileTests {

    @Test
    @DisplayName("should read file and delegate to compileTimeBuiltinsBinary")
    void shouldReadFileAndDelegate(@TempDir final Path tempDir) throws Exception {
      final byte[] content = {0x00, 0x61, 0x73, 0x6D};
      final Path file = tempDir.resolve("builtin.wasm");
      Files.write(file, content);

      final RecordingCodeBuilder builder = new RecordingCodeBuilder();
      final CodeBuilder result = builder.compileTimeBuiltinsBinaryFile("myBuiltin", file);

      assertSame(builder, result, "Should return this for chaining");
      assertNotNull(builder.lastBuiltinName, "Builtin name should be set");
      assertArrayEquals(
          content, builder.lastBuiltinBinary, "Should pass file bytes to delegate method");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null name")
    void shouldThrowForNullName(@TempDir final Path tempDir) throws Exception {
      final Path file = tempDir.resolve("builtin.wasm");
      Files.write(file, new byte[] {0x00});

      final RecordingCodeBuilder builder = new RecordingCodeBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.compileTimeBuiltinsBinaryFile(null, file),
          "Should throw IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null path")
    void shouldThrowForNullPath() {
      final RecordingCodeBuilder builder = new RecordingCodeBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.compileTimeBuiltinsBinaryFile("name", null),
          "Should throw IllegalArgumentException for null path");
    }
  }

  @Nested
  @DisplayName("compileTimeBuiltinsBinaryOrTextFile Tests")
  class CompileTimeBuiltinsBinaryOrTextFileTests {

    @Test
    @DisplayName("should read file and delegate to compileTimeBuiltinsBinaryOrText")
    void shouldReadFileAndDelegate(@TempDir final Path tempDir) throws Exception {
      final byte[] content = "(component)".getBytes();
      final Path file = tempDir.resolve("builtin.wat");
      Files.write(file, content);

      final RecordingCodeBuilder builder = new RecordingCodeBuilder();
      final CodeBuilder result = builder.compileTimeBuiltinsBinaryOrTextFile("myBuiltin", file);

      assertSame(builder, result, "Should return this for chaining");
      assertArrayEquals(
          content, builder.lastBuiltinBinaryOrText, "Should pass file bytes to delegate method");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null name")
    void shouldThrowForNullName(@TempDir final Path tempDir) throws Exception {
      final Path file = tempDir.resolve("builtin.wat");
      Files.write(file, new byte[] {0x00});

      final RecordingCodeBuilder builder = new RecordingCodeBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.compileTimeBuiltinsBinaryOrTextFile(null, file),
          "Should throw IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null path")
    void shouldThrowForNullPath() {
      final RecordingCodeBuilder builder = new RecordingCodeBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.compileTimeBuiltinsBinaryOrTextFile("name", null),
          "Should throw IllegalArgumentException for null path");
    }
  }

  @Nested
  @DisplayName("Empty File Tests")
  class EmptyFileTests {

    @Test
    @DisplayName("wasmBinaryFile should pass empty content for empty file")
    void wasmBinaryFileShouldHandleEmptyFile(@TempDir final Path tempDir) throws Exception {
      final Path file = tempDir.resolve("empty.wasm");
      Files.write(file, new byte[0]);

      final RecordingCodeBuilder builder = new RecordingCodeBuilder();
      builder.wasmBinaryFile(file);

      assertNotNull(builder.lastWasmBinary, "Should have called wasmBinary");
      assertArrayEquals(new byte[0], builder.lastWasmBinary, "Should pass empty byte array");
    }
  }
}
