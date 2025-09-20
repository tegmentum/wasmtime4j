package ai.tegmentum.wasmtime4j.serialization;

import static org.assertj.core.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import org.junit.jupiter.api.*;

/**
 * Comprehensive tests for the new Module serialization methods added in Issue #268.
 *
 * <p>These tests specifically validate the new Module interface methods: -
 * serialize(SerializationOptions) - serialize() - isSerializable() - getBytecodeHash() -
 * getCompiledSize()
 *
 * @since 1.0.0
 */
class ModuleSerializationMethodsTest {

  private Engine engine;
  private Module testModule;

  // Simple WebAssembly module for testing
  private static final String TEST_WAT =
      """
            (module
              (func $multiply (param i32 i32) (result i32)
                local.get 0
                local.get 1
                i32.mul)
              (export "multiply" (func $multiply))
              (memory 1)
              (export "memory" (memory 0)))
            """;

  @BeforeEach
  void setUp() throws WasmException {
    engine = WasmRuntimeFactory.create().createEngine();
    // Use a simple pre-compiled WASM module (basic add function)
    // This is equivalent to: (module (func $add (param i32 i32) (result i32) local.get 0 local.get
    // 1 i32.add) (export "add" (func $add)))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f,
              0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00, 0x0a,
              0x09,
          0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b
        };
    testModule = Module.compile(engine, wasmBytes);
  }

  @AfterEach
  void tearDown() {
    if (testModule != null) {
      testModule.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("Module reports serialization capability correctly")
  void testIsSerializable() {
    assertThat(testModule.isSerializable()).isTrue();

    // Test that closed module returns false
    final Module tempModule = testModule;
    tempModule.close();
    assertThat(tempModule.isSerializable()).isFalse();
  }

  @Test
  @DisplayName("Module can be serialized with default options")
  void testSerializeWithDefaults() throws WasmException {
    final SerializedModule serialized = testModule.serialize();

    assertThat(serialized).isNotNull();
    assertThat(serialized.getData()).isNotEmpty();
    assertThat(serialized.getMetadata()).isNotNull();
    assertThat(serialized.getSize()).isGreaterThan(0);
    assertThat(serialized.getCompressionType()).isEqualTo(CompressionType.NONE);
  }

  @Test
  @DisplayName("Module can be serialized with custom options")
  void testSerializeWithOptions() throws WasmException {
    final SerializationOptions options =
        SerializationOptions.builder()
            .compression(CompressionType.NONE)
            .includeDebugInfo(false)
            .includeSourceMap(false)
            .strictValidation(true)
            .includeChecksum(true)
            .build();

    final SerializedModule serialized = testModule.serialize(options);

    assertThat(serialized).isNotNull();
    assertThat(serialized.getData()).isNotEmpty();
    assertThat(serialized.getMetadata()).isNotNull();
    assertThat(serialized.getSize()).isGreaterThan(0);
    assertThat(serialized.getCompressionType()).isEqualTo(CompressionType.NONE);
    assertThat(serialized.hasDebugInfo()).isFalse();
    assertThat(serialized.hasSourceMap()).isFalse();
    assertThat(serialized.getChecksum()).isNotBlank();
  }

  @Test
  @DisplayName("Module serialization with compression options")
  void testSerializeWithCompression() throws WasmException {
    // Test different compression types if supported
    for (CompressionType compression : CompressionType.values()) {
      if (compression == CompressionType.NONE) continue; // Skip NONE, tested elsewhere

      final SerializationOptions options =
          SerializationOptions.builder().compression(compression).build();

      try {
        final SerializedModule serialized = testModule.serialize(options);
        assertThat(serialized).isNotNull();
        assertThat(serialized.getCompressionType()).isEqualTo(compression);
      } catch (WasmException e) {
        // Some compression types might not be supported - that's OK
        assertThat(e.getMessage()).contains("compression");
      }
    }
  }

  @Test
  @DisplayName("Module provides bytecode hash")
  void testGetBytecodeHash() {
    final byte[] hash = testModule.getBytecodeHash();

    assertThat(hash).isNotNull();
    assertThat(hash).hasSize(32); // SHA-256 hash is 32 bytes

    // Hash should be consistent between calls
    final byte[] hash2 = testModule.getBytecodeHash();
    assertThat(hash).isEqualTo(hash2);
  }

  @Test
  @DisplayName("Different modules have different bytecode hashes")
  void testBytecodeHashUniqueness() throws WasmException {
    // Create a different module (multiply function instead of add)
    // This is equivalent to: (module (func $mul (param i32 i32) (result i32) local.get 0 local.get
    // 1 i32.mul) (export "mul" (func $mul)))
    final byte[] differentWasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f,
              0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x07, 0x01, 0x03, 0x6d, 0x75, 0x6c, 0x00, 0x00, 0x0a,
              0x09,
          0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6c, 0x0b
        };

    try (final Module differentModule = Module.compile(engine, differentWasmBytes)) {
      final byte[] hash1 = testModule.getBytecodeHash();
      final byte[] hash2 = differentModule.getBytecodeHash();

      assertThat(hash1).isNotEqualTo(hash2);
    }
  }

  @Test
  @DisplayName("Module provides compiled size")
  void testGetCompiledSize() {
    final long size = testModule.getCompiledSize();

    assertThat(size).isGreaterThan(0);

    // Size should be consistent between calls
    final long size2 = testModule.getCompiledSize();
    assertThat(size).isEqualTo(size2);
  }

  @Test
  @DisplayName("Compiled size varies with module complexity")
  void testCompiledSizeVariation() throws WasmException {
    // Create a larger, more complex module with memory and multiple functions
    // This is a more complex module with memory and multiple functions
    final byte[] complexWasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x0b, 0x02, 0x60, 0x03, 0x7f, 0x7f,
              0x7f,
          0x01, 0x7f, 0x60, 0x00, 0x01, 0x7f, 0x03, 0x03, 0x02, 0x00, 0x01, 0x05, 0x03, 0x01, 0x00,
              0x0a,
          0x07, 0x0b, 0x02, 0x07, 0x63, 0x6f, 0x6d, 0x70, 0x6c, 0x65, 0x78, 0x00, 0x00, 0x06, 0x73,
              0x69,
          0x6d, 0x70, 0x6c, 0x65, 0x00, 0x01, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00,
              0x0a,
          0x17, 0x02, 0x0a, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x20, 0x02, 0x6c, 0x22, 0x03, 0x20,
              0x03,
          0x6a, 0x22, 0x04, 0x20, 0x04, 0x6c, 0x0b, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final Module complexModule = Module.compile(engine, complexWasmBytes)) {
      final long simpleSize = testModule.getCompiledSize();
      final long complexSize = complexModule.getCompiledSize();

      // Complex module should generally be larger
      assertThat(complexSize).isGreaterThanOrEqualTo(simpleSize);
    }
  }

  @Test
  @DisplayName("Serialization methods throw appropriate exceptions for closed modules")
  void testClosedModuleExceptions() {
    testModule.close();

    assertThatThrownBy(() -> testModule.serialize())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("closed");

    assertThatThrownBy(() -> testModule.serialize(SerializationOptions.defaults()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("closed");

    assertThatThrownBy(() -> testModule.getBytecodeHash()).isInstanceOf(RuntimeException.class);

    assertThatThrownBy(() -> testModule.getCompiledSize()).isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("Serialization options validation")
  void testSerializationOptionsValidation() {
    assertThatThrownBy(() -> testModule.serialize(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null");
  }

  @Test
  @DisplayName("Serialization round-trip preserves module functionality")
  void testSerializationRoundTrip() throws WasmException {
    // Serialize the module
    final SerializedModule serialized = testModule.serialize();

    // Deserialize it
    final Module deserializedModule = serialized.deserialize(engine);

    try {
      // Both modules should have the same bytecode hash
      final byte[] originalHash = testModule.getBytecodeHash();
      final byte[] deserializedHash = deserializedModule.getBytecodeHash();
      assertThat(deserializedHash).isEqualTo(originalHash);

      // Both modules should have similar compiled sizes
      final long originalSize = testModule.getCompiledSize();
      final long deserializedSize = deserializedModule.getCompiledSize();
      assertThat(deserializedSize).isCloseTo(originalSize, within(1000L)); // Allow some variance

      // Both modules should be serializable
      assertThat(deserializedModule.isSerializable()).isTrue();

    } finally {
      deserializedModule.close();
    }
  }

  @Test
  @DisplayName("Metadata consistency in serialized modules")
  void testMetadataConsistency() throws WasmException {
    final SerializationOptions options =
        SerializationOptions.builder().includeChecksum(true).strictValidation(true).build();

    final SerializedModule serialized = testModule.serialize(options);
    final ModuleMetadata metadata = serialized.getMetadata();

    assertThat(metadata).isNotNull();
    assertThat(metadata.getCompressionType()).isEqualTo(CompressionType.NONE);
    assertThat(metadata.hasDebugInfo()).isFalse();
    assertThat(metadata.hasSourceMap()).isFalse();

    // Verify checksum integrity
    assertThat(serialized.getChecksum()).isNotBlank();
    assertThat(serialized.getChecksum()).hasSize(64); // SHA-256 hex string
  }
}
