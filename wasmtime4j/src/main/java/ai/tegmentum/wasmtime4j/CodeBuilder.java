package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A builder for compiling WebAssembly modules and components with fine-grained control.
 *
 * <p>CodeBuilder provides a stateful builder pattern for WebAssembly compilation, allowing callers
 * to set the wasm source (binary or text), DWARF debug packages, and compilation hints before
 * triggering compilation.
 *
 * <p>This interface mirrors Wasmtime's {@code CodeBuilder} API and provides the most flexible
 * compilation path. For simple cases, use {@link Engine#compileModule(byte[])} directly.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (Engine engine = Engine.create();
 *      CodeBuilder builder = engine.codeBuilder()) {
 *     builder.wasmBinary(wasmBytes);
 *     builder.hint(CodeHint.MODULE);
 *     Module module = builder.compileModule();
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public interface CodeBuilder extends AutoCloseable {

  /**
   * Sets the WebAssembly binary bytes to compile.
   *
   * @param bytes the WebAssembly binary bytes
   * @return this builder for method chaining
   * @throws WasmException if the bytes cannot be set
   * @throws IllegalArgumentException if bytes is null or empty
   */
  CodeBuilder wasmBinary(byte[] bytes) throws WasmException;

  /**
   * Sets WebAssembly binary or text (WAT) bytes to compile.
   *
   * <p>If the bytes begin with the WebAssembly magic number, they are treated as binary. Otherwise,
   * they are parsed as WAT text format.
   *
   * @param bytes the WebAssembly binary or WAT text bytes
   * @return this builder for method chaining
   * @throws WasmException if the bytes cannot be set
   * @throws IllegalArgumentException if bytes is null or empty
   */
  CodeBuilder wasmBinaryOrText(byte[] bytes) throws WasmException;

  /**
   * Sets the WebAssembly binary from a file path.
   *
   * @param path the path to the WebAssembly binary file
   * @return this builder for method chaining
   * @throws WasmException if the file cannot be read
   * @throws IOException if an I/O error occurs reading the file
   * @throws IllegalArgumentException if path is null
   */
  default CodeBuilder wasmBinaryFile(final Path path) throws WasmException, IOException {
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    return wasmBinary(Files.readAllBytes(path));
  }

  /**
   * Sets WebAssembly binary or text from a file path.
   *
   * @param path the path to the WebAssembly binary or WAT file
   * @return this builder for method chaining
   * @throws WasmException if the file cannot be read
   * @throws IOException if an I/O error occurs reading the file
   * @throws IllegalArgumentException if path is null
   */
  default CodeBuilder wasmBinaryOrTextFile(final Path path) throws WasmException, IOException {
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    return wasmBinaryOrText(Files.readAllBytes(path));
  }

  /**
   * Sets the DWARF debug package bytes.
   *
   * <p>A DWARF package ({@code .dwp} file contents) provides additional debug information that is
   * merged into the compiled output for enhanced debugging and profiling.
   *
   * @param bytes the DWARF debug package bytes
   * @return this builder for method chaining
   * @throws WasmException if the DWARF package cannot be set
   * @throws IllegalArgumentException if bytes is null or empty
   */
  CodeBuilder dwarfPackage(byte[] bytes) throws WasmException;

  /**
   * Sets the DWARF debug package from a file path.
   *
   * @param path the path to the DWARF debug package file
   * @return this builder for method chaining
   * @throws WasmException if the file cannot be read
   * @throws IOException if an I/O error occurs reading the file
   * @throws IllegalArgumentException if path is null
   */
  default CodeBuilder dwarfPackageFile(final Path path) throws WasmException, IOException {
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    return dwarfPackage(Files.readAllBytes(path));
  }

  /**
   * Sets a compilation hint for the expected code type.
   *
   * <p>If the hint does not match the actual code, compilation will still succeed but may not be
   * optimally tuned.
   *
   * @param hint the code hint
   * @return this builder for method chaining
   * @throws IllegalArgumentException if hint is null
   */
  CodeBuilder hint(CodeHint hint);

  /**
   * Adds a compile-time builtin component from binary WebAssembly bytes.
   *
   * <p>This registers a component with the given name that will be available at compile time. The
   * bytes must be valid WebAssembly binary format.
   *
   * @param name the name of the builtin component
   * @param bytes the WebAssembly binary bytes of the builtin component
   * @return this builder for method chaining
   * @throws IllegalArgumentException if name is null or empty, or bytes is null or empty
   */
  CodeBuilder compileTimeBuiltinsBinary(String name, byte[] bytes);

  /**
   * Adds a compile-time builtin component from binary or text WebAssembly bytes.
   *
   * <p>If the bytes begin with the WebAssembly magic number, they are treated as binary. Otherwise,
   * they are parsed as WAT text format.
   *
   * @param name the name of the builtin component
   * @param bytes the WebAssembly binary or WAT text bytes of the builtin component
   * @return this builder for method chaining
   * @throws IllegalArgumentException if name is null or empty, or bytes is null or empty
   */
  CodeBuilder compileTimeBuiltinsBinaryOrText(String name, byte[] bytes);

  /**
   * Adds a compile-time builtin component from a binary WebAssembly file.
   *
   * @param name the name of the builtin component
   * @param path the path to the WebAssembly binary file
   * @return this builder for method chaining
   * @throws IOException if an I/O error occurs reading the file
   * @throws IllegalArgumentException if name or path is null
   */
  default CodeBuilder compileTimeBuiltinsBinaryFile(final String name, final Path path)
      throws IOException {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    return compileTimeBuiltinsBinary(name, Files.readAllBytes(path));
  }

  /**
   * Adds a compile-time builtin component from a binary or text WebAssembly file.
   *
   * @param name the name of the builtin component
   * @param path the path to the WebAssembly binary or WAT file
   * @return this builder for method chaining
   * @throws IOException if an I/O error occurs reading the file
   * @throws IllegalArgumentException if name or path is null
   */
  default CodeBuilder compileTimeBuiltinsBinaryOrTextFile(final String name, final Path path)
      throws IOException {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    return compileTimeBuiltinsBinaryOrText(name, Files.readAllBytes(path));
  }

  /**
   * Exposes unsafe Wasm intrinsics at the given import name.
   *
   * <p>This enables access to low-level, potentially unsafe WebAssembly intrinsics that are
   * imported by the given name. Use with caution as these intrinsics can break sandboxing
   * guarantees.
   *
   * @param importName the import name under which intrinsics will be available
   * @return this builder for method chaining
   * @throws IllegalArgumentException if importName is null or empty
   */
  CodeBuilder exposeUnsafeIntrinsics(String importName);

  /**
   * Compiles the configured WebAssembly bytes into a module.
   *
   * @return the compiled module
   * @throws WasmException if compilation fails
   * @throws IllegalStateException if no WebAssembly bytes have been set
   */
  Module compileModule() throws WasmException;

  /**
   * Compiles the configured WebAssembly bytes into serialized module bytes.
   *
   * <p>The serialized output can be cached and later deserialized with {@link
   * Engine#deserializeModule(byte[])} for faster startup.
   *
   * @return the serialized module bytes
   * @throws WasmException if compilation fails
   * @throws IllegalStateException if no WebAssembly bytes have been set
   */
  byte[] compileModuleSerialized() throws WasmException;

  /**
   * Compiles the configured WebAssembly bytes into a component.
   *
   * @return the compiled component as an opaque native handle
   * @throws WasmException if compilation fails or component model is not enabled
   * @throws IllegalStateException if no WebAssembly bytes have been set
   */
  long compileComponent() throws WasmException;

  /**
   * Compiles the configured WebAssembly bytes into serialized component bytes.
   *
   * @return the serialized component bytes
   * @throws WasmException if compilation fails or component model is not enabled
   * @throws IllegalStateException if no WebAssembly bytes have been set
   */
  byte[] compileComponentSerialized() throws WasmException;

  /** Closes this code builder, releasing native resources. */
  @Override
  void close();
}
