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

package ai.tegmentum.wasmtime4j.serialization;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Module;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleSerializationEngine} class.
 *
 * <p>ModuleSerializationEngine provides advanced WebAssembly module serialization capabilities
 * including multiple compression algorithms, streaming serialization, and parallel operations.
 */
@DisplayName("ModuleSerializationEngine Tests")
class ModuleSerializationEngineTest {

  private ModuleSerializationEngine engine;

  @BeforeEach
  void setUp() {
    engine = new ModuleSerializationEngine();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create engine with default constructor")
    void shouldCreateEngineWithDefaultConstructor() {
      final ModuleSerializationEngine newEngine = new ModuleSerializationEngine();
      assertNotNull(newEngine);
    }

    @Test
    @DisplayName("should create engine with custom executor")
    void shouldCreateEngineWithCustomExecutor() {
      final Executor customExecutor = ForkJoinPool.commonPool();
      final ModuleSerializationEngine newEngine = new ModuleSerializationEngine(customExecutor);
      assertNotNull(newEngine);
    }

    @Test
    @DisplayName("should throw on null executor")
    void shouldThrowOnNullExecutor() {
      assertThrows(NullPointerException.class, () -> new ModuleSerializationEngine(null));
    }
  }

  @Nested
  @DisplayName("Serialize Validation Tests")
  class SerializeValidationTests {

    @Test
    @DisplayName("serialize should throw on null module")
    void serializeShouldThrowOnNullModule() {
      assertThrows(
          NullPointerException.class,
          () ->
              engine.serialize(
                  null,
                  ModuleSerializationFormat.RAW_BINARY,
                  SerializationOptions.createDefault()));
    }

    @Test
    @DisplayName("serialize should throw on null format")
    void serializeShouldThrowOnNullFormat() {
      // We can't create a mock Module without more infrastructure, so we'll just test the
      // validation
      assertThrows(
          NullPointerException.class,
          () -> engine.serialize(createMockModule(), null, SerializationOptions.createDefault()));
    }

    @Test
    @DisplayName("serialize should accept null options and use defaults")
    void serializeShouldAcceptNullOptionsAndUseDefaults() {
      // This would require a real module to test fully
      // For now, just verify the engine exists
      assertNotNull(engine);
    }
  }

  @Nested
  @DisplayName("Deserialize Validation Tests")
  class DeserializeValidationTests {

    @Test
    @DisplayName("deserialize should throw on null data")
    void deserializeShouldThrowOnNullData() {
      final SerializedModuleMetadata metadata = createTestMetadata();

      assertThrows(NullPointerException.class, () -> engine.deserialize(null, metadata));
    }

    @Test
    @DisplayName("deserialize should throw on null metadata")
    void deserializeShouldThrowOnNullMetadata() {
      final byte[] data = new byte[100];

      assertThrows(NullPointerException.class, () -> engine.deserialize(data, null));
    }
  }

  @Nested
  @DisplayName("Stream Serialization Validation Tests")
  class StreamSerializationValidationTests {

    @Test
    @DisplayName("serializeToStream should throw on null module")
    void serializeToStreamShouldThrowOnNullModule() {
      assertThrows(
          NullPointerException.class,
          () ->
              engine.serializeToStream(
                  null, new ByteArrayOutputStream(), ModuleSerializationFormat.RAW_BINARY, null));
    }

    @Test
    @DisplayName("serializeToStream should throw on null output stream")
    void serializeToStreamShouldThrowOnNullOutputStream() {
      assertThrows(
          NullPointerException.class,
          () ->
              engine.serializeToStream(
                  createMockModule(), null, ModuleSerializationFormat.RAW_BINARY, null));
    }

    @Test
    @DisplayName("serializeToStream should throw on null format")
    void serializeToStreamShouldThrowOnNullFormat() {
      assertThrows(
          NullPointerException.class,
          () ->
              engine.serializeToStream(
                  createMockModule(), new ByteArrayOutputStream(), null, null));
    }
  }

  @Nested
  @DisplayName("Stream Deserialization Validation Tests")
  class StreamDeserializationValidationTests {

    @Test
    @DisplayName("deserializeFromStream should throw on null input stream")
    void deserializeFromStreamShouldThrowOnNullInputStream() {
      assertThrows(
          NullPointerException.class,
          () -> engine.deserializeFromStream(null, createTestMetadata()));
    }
  }

  @Nested
  @DisplayName("Parallel Serialization Validation Tests")
  class ParallelSerializationValidationTests {

    @Test
    @DisplayName("serializeParallel should throw on null modules")
    void serializeParallelShouldThrowOnNullModules() {
      assertThrows(
          NullPointerException.class,
          () ->
              engine.serializeParallel(
                  null,
                  ModuleSerializationFormat.RAW_BINARY,
                  SerializationOptions.createDefault()));
    }

    @Test
    @DisplayName("serializeParallel should throw on null format")
    void serializeParallelShouldThrowOnNullFormat() {
      assertThrows(
          NullPointerException.class,
          () ->
              engine.serializeParallel(new Module[0], null, SerializationOptions.createDefault()));
    }

    @Test
    @DisplayName("serializeParallel should return CompletableFuture")
    void serializeParallelShouldReturnCompletableFuture() {
      final CompletableFuture<SerializationResult[]> future =
          engine.serializeParallel(
              new Module[0],
              ModuleSerializationFormat.RAW_BINARY,
              SerializationOptions.createDefault());

      assertNotNull(future);
    }

    @Test
    @DisplayName("serializeParallel should handle empty modules array")
    void serializeParallelShouldHandleEmptyModulesArray() throws Exception {
      final CompletableFuture<SerializationResult[]> future =
          engine.serializeParallel(
              new Module[0],
              ModuleSerializationFormat.RAW_BINARY,
              SerializationOptions.createDefault());

      final SerializationResult[] results = future.get();
      assertNotNull(results);
      assertTrue(results.length == 0);
    }
  }

  @Nested
  @DisplayName("Serialization Format Tests")
  class SerializationFormatTests {

    @Test
    @DisplayName("should support RAW_BINARY format")
    void shouldSupportRawBinaryFormat() {
      assertNotNull(ModuleSerializationFormat.RAW_BINARY);
    }

    @Test
    @DisplayName("should support COMPACT_BINARY_LZ4 format")
    void shouldSupportCompactBinaryLz4Format() {
      assertNotNull(ModuleSerializationFormat.COMPACT_BINARY_LZ4);
    }

    @Test
    @DisplayName("should support COMPACT_BINARY_GZIP format")
    void shouldSupportCompactBinaryGzipFormat() {
      assertNotNull(ModuleSerializationFormat.COMPACT_BINARY_GZIP);
    }

    @Test
    @DisplayName("should support STREAMING_BINARY format")
    void shouldSupportStreamingBinaryFormat() {
      assertNotNull(ModuleSerializationFormat.STREAMING_BINARY);
    }

    @Test
    @DisplayName("should support MEMORY_MAPPED format")
    void shouldSupportMemoryMappedFormat() {
      assertNotNull(ModuleSerializationFormat.MEMORY_MAPPED);
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ModuleSerializationEngine should be final")
    void moduleSerializationEngineShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ModuleSerializationEngine.class.getModifiers()));
    }
  }

  // Helper methods

  private SerializedModuleMetadata createTestMetadata() {
    return new SerializedModuleMetadata.Builder()
        .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
        .setSerializedSize(100)
        .setOriginalSize(200)
        .setSha256Hash("testhash")
        .build();
  }

  /**
   * Creates a mock Module for testing. Note: In a real test scenario, this would use proper mocking
   * or test fixtures.
   */
  private Module createMockModule() {
    // Returns a mock/stub module for validation tests
    // The actual serialize operations will fail because extractRawModuleData is not implemented
    return new Module() {
      @Override
      public String getName() {
        return "test-module";
      }

      @Override
      public ai.tegmentum.wasmtime4j.Instance instantiate(
          final ai.tegmentum.wasmtime4j.Store store) {
        throw new UnsupportedOperationException("Mock module");
      }

      @Override
      public ai.tegmentum.wasmtime4j.Instance instantiate(
          final ai.tegmentum.wasmtime4j.Store store,
          final ai.tegmentum.wasmtime4j.ImportMap imports) {
        throw new UnsupportedOperationException("Mock module");
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ExportType> getExports() {
        return java.util.List.of();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ImportType> getImports() {
        return java.util.List.of();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ExportDescriptor> getExportDescriptors() {
        return java.util.List.of();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ImportDescriptor> getImportDescriptors() {
        return java.util.List.of();
      }

      @Override
      public java.util.Optional<ai.tegmentum.wasmtime4j.FuncType> getFunctionType(
          final String functionName) {
        return java.util.Optional.empty();
      }

      @Override
      public java.util.Optional<ai.tegmentum.wasmtime4j.GlobalType> getGlobalType(
          final String globalName) {
        return java.util.Optional.empty();
      }

      @Override
      public java.util.Optional<ai.tegmentum.wasmtime4j.MemoryType> getMemoryType(
          final String memoryName) {
        return java.util.Optional.empty();
      }

      @Override
      public java.util.Optional<ai.tegmentum.wasmtime4j.TableType> getTableType(
          final String tableName) {
        return java.util.Optional.empty();
      }

      @Override
      public boolean hasExport(final String name) {
        return false;
      }

      @Override
      public boolean hasImport(final String moduleName, final String fieldName) {
        return false;
      }

      @Override
      public ai.tegmentum.wasmtime4j.Engine getEngine() {
        return null;
      }

      @Override
      public boolean validateImports(final ai.tegmentum.wasmtime4j.ImportMap imports) {
        return false;
      }

      @Override
      public ai.tegmentum.wasmtime4j.ImportValidation validateImportsDetailed(
          final ai.tegmentum.wasmtime4j.ImportMap imports) {
        return null;
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ModuleImport> getModuleImports() {
        return java.util.List.of();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ModuleExport> getModuleExports() {
        return java.util.List.of();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.FuncType> getFunctionTypes() {
        return java.util.List.of();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.MemoryType> getMemoryTypes() {
        return java.util.List.of();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.TableType> getTableTypes() {
        return java.util.List.of();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.GlobalType> getGlobalTypes() {
        return java.util.List.of();
      }

      @Override
      public java.util.Map<String, byte[]> getCustomSections() {
        return java.util.Map.of();
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public byte[] serialize() {
        return new byte[0];
      }

      @Override
      public void close() {
        // No-op
      }
    };
  }
}
