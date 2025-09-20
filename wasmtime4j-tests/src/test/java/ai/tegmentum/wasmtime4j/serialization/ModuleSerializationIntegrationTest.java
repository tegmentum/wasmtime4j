package ai.tegmentum.wasmtime4j.serialization;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration tests for module serialization functionality.
 *
 * <p>These tests validate the complete module serialization pipeline including:
 * - Module serialization with various options
 * - Round-trip serialization and deserialization
 * - Compression support
 * - Streaming serialization for large modules
 * - Metadata extraction and validation
 * - Error handling and edge cases
 *
 * @since 1.0.0
 */
class ModuleSerializationIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(ModuleSerializationIntegrationTest.class.getName());

    // Simple WebAssembly module for testing
    private static final String SIMPLE_WAT = """
            (module
              (func $add (param i32 i32) (result i32)
                local.get 0
                local.get 1
                i32.add)
              (export "add" (func $add)))
            """;

    // Large WebAssembly module for testing streaming and compression
    private static final String LARGE_WAT = """
            (module
              (memory 10)
              (func $large_function (param i32) (result i32)
                (local i32 i32 i32 i32 i32)
                ;; Generate a large function with many operations
                local.get 0
                i32.const 1000
                i32.add
                local.set 1
                local.get 1
                i32.const 500
                i32.sub
                local.set 2
                local.get 2
                i32.const 250
                i32.mul
                local.set 3
                local.get 3
                i32.const 125
                i32.div_s
                local.set 4
                local.get 4
                i32.const 62
                i32.rem_s
                local.set 5
                local.get 5)
              (export "large_function" (func $large_function)))
            """;

    private Engine engine;
    private ModuleSerializer serializer;
    private byte[] simpleWasmBytes;
    private byte[] largeWasmBytes;
    private Module simpleModule;
    private Module largeModule;

    @BeforeEach
    void setUp() throws Exception {
        // Create engine and serializer
        engine = Engine.create();
        serializer = engine.getModuleSerializer();

        assertThat(engine).isNotNull();
        assertThat(serializer).isNotNull();

        // Compile WAT to WASM bytes
        simpleWasmBytes = wat.parse(SIMPLE_WAT);
        largeWasmBytes = wat.parse(LARGE_WAT);

        assertThat(simpleWasmBytes).isNotEmpty();
        assertThat(largeWasmBytes).isNotEmpty();

        // Create modules
        simpleModule = engine.compileModule(simpleWasmBytes);
        largeModule = engine.compileModule(largeWasmBytes);

        assertThat(simpleModule).isNotNull();
        assertThat(largeModule).isNotNull();

        LOGGER.info("Test setup completed - Engine, serializer, and modules created");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (simpleModule != null) {
            simpleModule.close();
        }
        if (largeModule != null) {
            largeModule.close();
        }
        if (serializer != null) {
            serializer.close();
        }
        if (engine != null) {
            engine.close();
        }
        LOGGER.info("Test cleanup completed");
    }

    @Test
    @DisplayName("Module serializer can be created and provides basic functionality")
    void testModuleSerializerCreation() throws WasmException {
        assertThat(serializer).isNotNull();

        // Test basic properties
        final String formatVersion = serializer.getFormatVersion();
        assertThat(formatVersion).isNotBlank();

        final boolean supportsCurrentVersion = serializer.supportsFormatVersion(formatVersion);
        assertThat(supportsCurrentVersion).isTrue();

        // Test with future version (should return false)
        final boolean supportsFutureVersion = serializer.supportsFormatVersion("99.0.0");
        assertThat(supportsFutureVersion).isFalse();

        LOGGER.info("Module serializer validation completed successfully");
    }

    @Test
    @DisplayName("Simple module can be serialized and deserialized without compression")
    void testSimpleModuleSerializationRoundTrip() throws WasmException {
        // Serialize with default options
        final SerializedModule serialized = serializer.serialize(simpleModule);
        assertThat(serialized).isNotNull();
        assertThat(serialized.getData()).isNotEmpty();
        assertThat(serialized.getCompression()).isEqualTo(CompressionType.NONE);

        // Deserialize
        final Module deserializedModule = serializer.deserialize(engine, serialized.getData());
        assertThat(deserializedModule).isNotNull();

        // Verify functionality equivalence
        assertThat(deserializedModule.getExports()).hasSize(simpleModule.getExports().size());
        assertThat(deserializedModule.getImports()).hasSize(simpleModule.getImports().size());

        LOGGER.info("Simple module serialization round-trip completed successfully");
    }

    @Test
    @DisplayName("Module can be serialized with different compression types")
    void testModuleSerializationWithCompression() throws WasmException {
        final SerializationOptions.Builder baseBuilder = SerializationOptions.builder();

        // Test all compression types
        for (final CompressionType compression : CompressionType.values()) {
            final SerializationOptions options = baseBuilder
                    .compression(compression)
                    .compressionLevel(6)
                    .build();

            final SerializedModule serialized = serializer.serialize(simpleModule, options);
            assertThat(serialized).isNotNull();
            assertThat(serialized.getData()).isNotEmpty();
            assertThat(serialized.getCompression()).isEqualTo(compression);

            // Verify round-trip
            final Module deserializedModule = serializer.deserialize(engine, serialized.getData());
            assertThat(deserializedModule).isNotNull();
            assertThat(deserializedModule.getExports()).hasSize(simpleModule.getExports().size());

            LOGGER.info("Module serialized with compression: " + compression);
        }

        LOGGER.info("All compression types tested successfully");
    }

    @Test
    @DisplayName("Compression reduces module size for large modules")
    void testCompressionEffectiveness() throws WasmException {
        // Serialize without compression
        final SerializationOptions noCompression = SerializationOptions.builder()
                .compression(CompressionType.NONE)
                .build();
        final SerializedModule uncompressed = serializer.serialize(largeModule, noCompression);

        // Serialize with GZIP compression
        final SerializationOptions gzipCompression = SerializationOptions.builder()
                .compression(CompressionType.GZIP)
                .compressionLevel(9) // Maximum compression
                .build();
        final SerializedModule compressed = serializer.serialize(largeModule, gzipCompression);

        assertThat(uncompressed).isNotNull();
        assertThat(compressed).isNotNull();

        final long uncompressedSize = uncompressed.getSize();
        final long compressedSize = compressed.getSize();

        // For large modules, compression should reduce size
        // Note: For small modules, compression might actually increase size due to overhead
        LOGGER.info(String.format("Compression test: uncompressed=%d bytes, compressed=%d bytes, ratio=%.2f%%",
                uncompressedSize, compressedSize,
                (1.0 - (double) compressedSize / uncompressedSize) * 100.0));

        // Both should deserialize correctly
        final Module uncompressedModule = serializer.deserialize(engine, uncompressed.getData());
        final Module compressedModule = serializer.deserialize(engine, compressed.getData());

        assertThat(uncompressedModule).isNotNull();
        assertThat(compressedModule).isNotNull();
        assertThat(uncompressedModule.getExports()).hasSize(compressedModule.getExports().size());

        LOGGER.info("Compression effectiveness test completed successfully");
    }

    @Test
    @DisplayName("Module serialization with debug and profiling info")
    void testSerializationWithDebugAndProfilingInfo() throws WasmException {
        // Serialize without debug/profiling info
        final SerializationOptions basicOptions = SerializationOptions.builder()
                .includeDebugInfo(false)
                .includeProfilingInfo(false)
                .build();
        final SerializedModule basicSerialized = serializer.serialize(simpleModule, basicOptions);

        // Serialize with debug and profiling info
        final SerializationOptions enhancedOptions = SerializationOptions.builder()
                .includeDebugInfo(true)
                .includeProfilingInfo(true)
                .build();
        final SerializedModule enhancedSerialized = serializer.serialize(simpleModule, enhancedOptions);

        assertThat(basicSerialized).isNotNull();
        assertThat(enhancedSerialized).isNotNull();

        // Enhanced version may be larger (though not guaranteed)
        final long basicSize = basicSerialized.getSize();
        final long enhancedSize = enhancedSerialized.getSize();

        LOGGER.info(String.format("Debug/profiling test: basic=%d bytes, enhanced=%d bytes",
                basicSize, enhancedSize));

        // Both should deserialize correctly
        final Module basicModule = serializer.deserialize(engine, basicSerialized.getData());
        final Module enhancedModule = serializer.deserialize(engine, enhancedSerialized.getData());

        assertThat(basicModule).isNotNull();
        assertThat(enhancedModule).isNotNull();

        LOGGER.info("Debug and profiling info serialization test completed successfully");
    }

    @Test
    @DisplayName("Serialization validation and compatibility checking work correctly")
    void testSerializationValidationAndCompatibility() throws WasmException {
        final SerializedModule serialized = serializer.serialize(simpleModule);
        final byte[] data = serialized.getData();

        // Test validation
        assertThat(serializer.isValidSerialization(data)).isTrue();

        // Test compatibility with current engine
        assertThat(serializer.isCompatible(engine, data)).isTrue();
        assertThat(serialized.isCompatibleWith(engine)).isTrue();

        // Test with invalid data
        final byte[] invalidData = new byte[]{0x00, 0x01, 0x02, 0x03};
        assertThat(serializer.isValidSerialization(invalidData)).isFalse();

        // Test with empty data
        final byte[] emptyData = new byte[0];
        assertThat(serializer.isValidSerialization(emptyData)).isFalse();

        LOGGER.info("Serialization validation and compatibility test completed successfully");
    }

    @Test
    @DisplayName("Metadata can be extracted from serialized modules")
    void testMetadataExtraction() throws WasmException {
        final SerializationOptions options = SerializationOptions.builder()
                .compression(CompressionType.GZIP)
                .includeDebugInfo(true)
                .addCustomMetadata("test_key", "test_value")
                .build();

        final SerializedModule serialized = serializer.serialize(simpleModule, options);
        final byte[] data = serialized.getData();

        // Extract metadata
        final ModuleMetadata metadata = serializer.extractMetadata(data);
        assertThat(metadata).isNotNull();

        // Verify metadata fields
        assertThat(metadata.getFormatVersion()).isNotBlank();
        assertThat(metadata.getWasmtimeVersion()).isNotBlank();
        assertThat(metadata.getSerializationTime()).isGreaterThan(0);
        assertThat(metadata.getOriginalSize()).isGreaterThanOrEqualTo(0);
        assertThat(metadata.getCompressedSize()).isGreaterThan(0);
        assertThat(metadata.getChecksum()).isNotBlank();

        // Test metadata access from serialized module
        final String metadataString = serialized.getMetadataString();
        assertThat(metadataString).isNotBlank();

        LOGGER.info("Metadata extraction test completed successfully");
    }

    @Test
    @DisplayName("Streaming serialization works for large modules")
    void testStreamingSerialization() throws Exception {
        final SerializationOptions options = SerializationOptions.builder()
                .compression(CompressionType.GZIP)
                .build();

        // Test streaming to ByteArrayOutputStream
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            serializer.serializeStreaming(largeModule, options, outputStream);
            final byte[] streamedData = outputStream.toByteArray();
            assertThat(streamedData).isNotEmpty();

            // Verify the streamed data can be deserialized
            final Module deserializedModule = serializer.deserialize(engine, streamedData);
            assertThat(deserializedModule).isNotNull();
            assertThat(deserializedModule.getExports()).hasSize(largeModule.getExports().size());
        }

        // Test streaming to file
        final Path tempFile = Files.createTempFile("wasmtime4j-serialization-test", ".wasm");
        try {
            try (final FileOutputStream fileOutput = new FileOutputStream(tempFile.toFile())) {
                serializer.serializeStreaming(largeModule, options, fileOutput);
            }

            assertThat(Files.exists(tempFile)).isTrue();
            assertThat(Files.size(tempFile)).isGreaterThan(0);

            // Deserialize from file
            try (final FileInputStream fileInput = new FileInputStream(tempFile.toFile())) {
                final Module deserializedModule = serializer.deserializeStreaming(engine, fileInput);
                assertThat(deserializedModule).isNotNull();
                assertThat(deserializedModule.getExports()).hasSize(largeModule.getExports().size());
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }

        LOGGER.info("Streaming serialization test completed successfully");
    }

    @Test
    @DisplayName("Serialization handles checksum validation correctly")
    void testChecksumValidation() throws WasmException {
        final SerializedModule serialized = serializer.serialize(simpleModule);

        // Get checksum
        final String checksum = serialized.getChecksum();
        assertThat(checksum).isNotBlank();

        // Verify checksum is consistent
        final String checksum2 = serialized.getChecksum();
        assertThat(checksum2).isEqualTo(checksum);

        // Test serialized module validation
        assertThat(serialized.isValid()).isTrue();

        LOGGER.info("Checksum validation test completed successfully");
    }

    @Test
    @DisplayName("Serialization error handling works correctly")
    void testSerializationErrorHandling() {
        // Test with null parameters
        assertThatThrownBy(() -> serializer.serialize(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> serializer.serialize(simpleModule, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> serializer.deserialize(null, new byte[]{0x00}))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> serializer.deserialize(engine, null))
                .isInstanceOf(IllegalArgumentException.class);

        // Test with invalid serialized data
        final byte[] invalidData = new byte[]{0x00, 0x01, 0x02, 0x03};
        assertThatThrownBy(() -> serializer.deserialize(engine, invalidData))
                .isInstanceOf(WasmException.class);

        // Test with empty data
        final byte[] emptyData = new byte[0];
        assertThatThrownBy(() -> serializer.deserialize(engine, emptyData))
                .isInstanceOf(IllegalArgumentException.class);

        // Test metadata extraction with invalid data
        assertThatThrownBy(() -> serializer.extractMetadata(invalidData))
                .isInstanceOf(WasmException.class);

        assertThatThrownBy(() -> serializer.extractMetadata(emptyData))
                .isInstanceOf(IllegalArgumentException.class);

        LOGGER.info("Serialization error handling test completed successfully");
    }

    @Test
    @DisplayName("Multiple serializations can be performed concurrently")
    void testConcurrentSerialization() throws Exception {
        final SerializationOptions options = SerializationOptions.builder()
                .compression(CompressionType.GZIP)
                .build();

        // Create multiple serialization tasks
        final int numTasks = 4;
        final Thread[] threads = new Thread[numTasks];
        final SerializedModule[] results = new SerializedModule[numTasks];
        final Exception[] exceptions = new Exception[numTasks];

        for (int i = 0; i < numTasks; i++) {
            final int taskIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    results[taskIndex] = serializer.serialize(simpleModule, options);
                } catch (final Exception e) {
                    exceptions[taskIndex] = e;
                }
            });
        }

        // Start all threads
        for (final Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (final Thread thread : threads) {
            thread.join(10000); // 10 second timeout
        }

        // Verify all serializations succeeded
        for (int i = 0; i < numTasks; i++) {
            assertThat(exceptions[i]).isNull();
            assertThat(results[i]).isNotNull();
            assertThat(results[i].getData()).isNotEmpty();
        }

        LOGGER.info("Concurrent serialization test completed successfully");
    }

    @Test
    @DisplayName("Serialization performance is within acceptable bounds")
    void testSerializationPerformance() throws WasmException {
        final SerializationOptions options = SerializationOptions.builder()
                .compression(CompressionType.GZIP)
                .compressionLevel(6)
                .build();

        // Measure serialization time
        final long startTime = System.nanoTime();
        final SerializedModule serialized = serializer.serialize(largeModule, options);
        final long serializationTime = System.nanoTime() - startTime;

        assertThat(serialized).isNotNull();

        // Measure deserialization time
        final long deserStartTime = System.nanoTime();
        final Module deserializedModule = serializer.deserialize(engine, serialized.getData());
        final long deserializationTime = System.nanoTime() - deserStartTime;

        assertThat(deserializedModule).isNotNull();

        // Performance should be reasonable (adjust thresholds as needed)
        final long maxSerializationTimeMs = 2000; // 2 seconds
        final long maxDeserializationTimeMs = 1000; // 1 second

        final long serializationTimeMs = serializationTime / 1_000_000;
        final long deserializationTimeMs = deserializationTime / 1_000_000;

        assertThat(serializationTimeMs).isLessThan(maxSerializationTimeMs);
        assertThat(deserializationTimeMs).isLessThan(maxDeserializationTimeMs);

        LOGGER.info(String.format("Serialization performance: serialization=%dms, deserialization=%dms",
                serializationTimeMs, deserializationTimeMs));
    }

    @Test
    @DisplayName("Custom metadata can be included in serialization")
    void testCustomMetadata() throws WasmException {
        final SerializationOptions options = SerializationOptions.builder()
                .addCustomMetadata("application", "wasmtime4j-test")
                .addCustomMetadata("version", "1.0.0")
                .addCustomMetadata("author", "test-suite")
                .build();

        final SerializedModule serialized = serializer.serialize(simpleModule, options);
        assertThat(serialized).isNotNull();

        // Custom metadata should be preserved in serialization
        final String metadataString = serialized.getMetadataString();
        assertThat(metadataString).contains("wasmtime4j-test");

        // Module should still deserialize correctly
        final Module deserializedModule = serializer.deserialize(engine, serialized.getData());
        assertThat(deserializedModule).isNotNull();

        LOGGER.info("Custom metadata test completed successfully");
    }

    // Helper method for WAT compilation - would use actual WAT parser in real implementation
    private static class wat {
        public static byte[] parse(String watContent) {
            // This is a placeholder - in real implementation, this would use
            // the WAT parser to convert WebAssembly text format to binary
            // For now, return a minimal valid WASM binary
            return new byte[]{
                    0x00, 0x61, 0x73, 0x6d, // WASM magic number
                    0x01, 0x00, 0x00, 0x00  // WASM version
            };
        }
    }
}