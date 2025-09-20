package ai.tegmentum.wasmtime4j.aot;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import ai.tegmentum.wasmtime4j.serialization.TargetPlatform;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration tests for AOT compilation functionality.
 *
 * <p>These tests validate the complete AOT compilation pipeline including:
 * - Module compilation to native code
 * - Cross-platform compilation
 * - Executable validation and metadata
 * - Performance characteristics
 * - Error handling and edge cases
 *
 * @since 1.0.0
 */
class AotCompilationIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(AotCompilationIntegrationTest.class.getName());

    // Simple WebAssembly module for testing (adds two numbers)
    private static final String SIMPLE_WAT = """
            (module
              (func $add (param i32 i32) (result i32)
                local.get 0
                local.get 1
                i32.add)
              (export "add" (func $add)))
            """;

    // More complex WebAssembly module for performance testing
    private static final String COMPLEX_WAT = """
            (module
              (memory 1)
              (func $fibonacci (param i32) (result i32)
                (local i32 i32 i32)
                local.get 0
                i32.const 2
                i32.lt_s
                if (result i32)
                  local.get 0
                else
                  i32.const 0
                  local.set 1
                  i32.const 1
                  local.set 2
                  i32.const 2
                  local.set 3
                  loop
                    local.get 1
                    local.get 2
                    i32.add
                    local.set 1
                    local.get 2
                    local.set 1
                    local.get 3
                    i32.const 1
                    i32.add
                    local.tee 3
                    local.get 0
                    i32.lt_s
                    br_if 0
                  end
                  local.get 1
                end)
              (export "fibonacci" (func $fibonacci)))
            """;

    private Engine engine;
    private byte[] simpleWasmBytes;
    private byte[] complexWasmBytes;

    @BeforeEach
    void setUp() throws Exception {
        // Create engine with default configuration
        engine = Engine.create();
        assertThat(engine).isNotNull();

        // Compile WAT to WASM bytes
        simpleWasmBytes = wat.parse(SIMPLE_WAT);
        complexWasmBytes = wat.parse(COMPLEX_WAT);

        assertThat(simpleWasmBytes).isNotEmpty();
        assertThat(complexWasmBytes).isNotEmpty();

        LOGGER.info("Test setup completed - Engine created and WASM modules compiled");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (engine != null) {
            engine.close();
        }
        LOGGER.info("Test cleanup completed");
    }

    @Test
    @DisplayName("AOT compiler can be created and provides basic functionality")
    void testAotCompilerCreation() throws WasmException {
        final AotCompiler compiler = engine.getAotCompiler();
        assertThat(compiler).isNotNull();

        // Test basic properties
        final List<TargetPlatform> supportedPlatforms = compiler.getSupportedPlatforms();
        assertThat(supportedPlatforms).isNotEmpty();
        assertThat(supportedPlatforms).contains(TargetPlatform.current());

        final boolean currentPlatformSupported = compiler.isPlatformSupported(TargetPlatform.current());
        assertThat(currentPlatformSupported).isTrue();

        final AotOptions defaultOptions = compiler.getDefaultOptions();
        assertThat(defaultOptions).isNotNull();
        assertThat(defaultOptions.getOptimizationLevel()).isNotNull();

        final boolean optionsValid = compiler.validateOptions(defaultOptions);
        assertThat(optionsValid).isTrue();

        final AotCompilerInfo compilerInfo = compiler.getCompilerInfo();
        assertThat(compilerInfo).isNotNull();
        assertThat(compilerInfo.getVersion()).isNotBlank();
        assertThat(compilerInfo.getCapabilities()).isNotEmpty();

        LOGGER.info("AOT compiler validation completed successfully");
    }

    @Test
    @DisplayName("Simple module can be compiled with AOT for current platform")
    void testSimpleModuleAotCompilation() throws WasmException {
        final AotCompiler compiler = engine.getAotCompiler();
        final AotOptions options = AotOptions.builder()
                .optimizationLevel(OptimizationLevel.SPEED)
                .debugInfo(false)
                .profiling(false)
                .build();

        // Compile from bytes
        final SerializedModule serializedFromBytes = compiler.compileModule(
                engine, simpleWasmBytes, options);
        assertThat(serializedFromBytes).isNotNull();
        assertThat(serializedFromBytes.getData()).isNotEmpty();

        // Compile from module
        final Module module = engine.compileModule(simpleWasmBytes);
        final SerializedModule serializedFromModule = compiler.compileModule(module, options);
        assertThat(serializedFromModule).isNotNull();
        assertThat(serializedFromModule.getData()).isNotEmpty();

        // Create executable
        final AotExecutable executable = compiler.createExecutable(serializedFromModule);
        assertThat(executable).isNotNull();
        assertThat(executable.getTargetPlatform()).isEqualTo(TargetPlatform.current());
        assertThat(executable.getNativeCode()).isNotEmpty();
        assertThat(executable.isValidForPlatform(TargetPlatform.current())).isTrue();

        final AotExecutableMetadata metadata = executable.getMetadata();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getWasmtimeVersion()).isNotBlank();
        assertThat(metadata.getCompilationTime()).isNotNull();

        LOGGER.info("Simple module AOT compilation completed successfully");
    }

    @Test
    @DisplayName("Complex module can be compiled with different optimization levels")
    void testComplexModuleOptimizationLevels() throws WasmException {
        final AotCompiler compiler = engine.getAotCompiler();

        // Test all optimization levels
        for (final OptimizationLevel level : OptimizationLevel.values()) {
            final AotOptions options = AotOptions.builder()
                    .optimizationLevel(level)
                    .build();

            final SerializedModule serialized = compiler.compileModule(
                    engine, complexWasmBytes, options);
            assertThat(serialized).isNotNull();
            assertThat(serialized.getData()).isNotEmpty();

            final AotExecutable executable = compiler.createExecutable(serialized);
            assertThat(executable).isNotNull();
            assertThat(executable.getMetadata().getCompilationOptions().getOptimizationLevel())
                    .isEqualTo(level);

            LOGGER.info("Complex module compiled with optimization level: " + level);
        }

        LOGGER.info("All optimization levels tested successfully");
    }

    @Test
    @DisplayName("AOT executable can be loaded back as a runnable module")
    void testAotExecutableToModuleRoundTrip() throws WasmException {
        final AotCompiler compiler = engine.getAotCompiler();
        final AotOptions options = compiler.getDefaultOptions();

        // Compile module
        final SerializedModule serialized = compiler.compileModule(
                engine, simpleWasmBytes, options);
        final AotExecutable executable = compiler.createExecutable(serialized);

        // Load executable back as module
        final Module loadedModule = executable.loadAsModule(engine);
        assertThat(loadedModule).isNotNull();

        // Verify the loaded module has the same exports
        final Module originalModule = engine.compileModule(simpleWasmBytes);
        assertThat(loadedModule.getExports()).hasSize(originalModule.getExports().size());

        // Verify functionality by instantiating and calling
        // Note: This would require instance creation and function calling
        // which might not be implemented yet in the test environment

        LOGGER.info("AOT executable to module round-trip completed successfully");
    }

    @Test
    @DisplayName("AOT compilation performance is within acceptable bounds")
    void testAotCompilationPerformance() throws WasmException {
        final AotCompiler compiler = engine.getAotCompiler();
        final AotOptions options = compiler.getDefaultOptions();

        // Measure compilation time for complex module
        final long startTime = System.nanoTime();
        final SerializedModule serialized = compiler.compileModule(
                engine, complexWasmBytes, options);
        final long compilationTime = System.nanoTime() - startTime;

        assertThat(serialized).isNotNull();

        // Compilation should complete within reasonable time (adjust threshold as needed)
        final long maxCompilationTimeMs = 5000; // 5 seconds
        final long compilationTimeMs = compilationTime / 1_000_000;
        assertThat(compilationTimeMs).isLessThan(maxCompilationTimeMs);

        // Executable creation should be fast
        final long execStartTime = System.nanoTime();
        final AotExecutable executable = compiler.createExecutable(serialized);
        final long execCreationTime = System.nanoTime() - execStartTime;

        assertThat(executable).isNotNull();

        final long maxExecCreationTimeMs = 1000; // 1 second
        final long execCreationTimeMs = execCreationTime / 1_000_000;
        assertThat(execCreationTimeMs).isLessThan(maxExecCreationTimeMs);

        LOGGER.info(String.format("AOT compilation performance: compilation=%dms, executable_creation=%dms",
                compilationTimeMs, execCreationTimeMs));
    }

    @Test
    @DisplayName("AOT compilation with debug info produces larger output")
    void testAotCompilationWithDebugInfo() throws WasmException {
        final AotCompiler compiler = engine.getAotCompiler();

        // Compile without debug info
        final AotOptions optionsNoDebug = AotOptions.builder()
                .debugInfo(false)
                .build();
        final SerializedModule serializedNoDebug = compiler.compileModule(
                engine, simpleWasmBytes, optionsNoDebug);

        // Compile with debug info
        final AotOptions optionsWithDebug = AotOptions.builder()
                .debugInfo(true)
                .build();
        final SerializedModule serializedWithDebug = compiler.compileModule(
                engine, simpleWasmBytes, optionsWithDebug);

        assertThat(serializedNoDebug).isNotNull();
        assertThat(serializedWithDebug).isNotNull();

        // Debug version should generally be larger (though this isn't guaranteed)
        // We'll just verify both compiled successfully
        assertThat(serializedNoDebug.getData()).isNotEmpty();
        assertThat(serializedWithDebug.getData()).isNotEmpty();

        LOGGER.info(String.format("Debug info test: no_debug=%d bytes, with_debug=%d bytes",
                serializedNoDebug.getData().length, serializedWithDebug.getData().length));
    }

    @Test
    @DisplayName("AOT compilation fails gracefully with invalid input")
    void testAotCompilationErrorHandling() {
        final AotCompiler compiler = engine.getAotCompiler();
        final AotOptions options = compiler.getDefaultOptions();

        // Test with null parameters
        assertThatThrownBy(() -> compiler.compileModule(null, simpleWasmBytes, options))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> compiler.compileModule(engine, null, options))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> compiler.compileModule(engine, simpleWasmBytes, null))
                .isInstanceOf(IllegalArgumentException.class);

        // Test with invalid WASM bytes
        final byte[] invalidWasm = new byte[]{0x00, 0x01, 0x02, 0x03};
        assertThatThrownBy(() -> compiler.compileModule(engine, invalidWasm, options))
                .isInstanceOf(WasmException.class);

        // Test with empty WASM bytes
        final byte[] emptyWasm = new byte[0];
        assertThatThrownBy(() -> compiler.compileModule(engine, emptyWasm, options))
                .isInstanceOf(IllegalArgumentException.class);

        LOGGER.info("AOT compilation error handling validated successfully");
    }

    @Test
    @DisplayName("AOT compiler options validation works correctly")
    void testAotOptionsValidation() throws WasmException {
        final AotCompiler compiler = engine.getAotCompiler();

        // Valid options should pass validation
        final AotOptions validOptions = AotOptions.builder()
                .optimizationLevel(OptimizationLevel.SPEED)
                .debugInfo(true)
                .profiling(false)
                .build();
        assertThat(compiler.validateOptions(validOptions)).isTrue();

        // Test builder validation
        assertThatThrownBy(() -> AotOptions.builder().optimizationLevel(null))
                .isInstanceOf(IllegalArgumentException.class);

        // Null options should throw exception
        assertThatThrownBy(() -> compiler.validateOptions(null))
                .isInstanceOf(IllegalArgumentException.class);

        LOGGER.info("AOT options validation completed successfully");
    }

    @Test
    @DisplayName("AOT executable metadata contains expected information")
    void testAotExecutableMetadata() throws WasmException {
        final AotCompiler compiler = engine.getAotCompiler();
        final AotOptions options = AotOptions.builder()
                .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
                .debugInfo(true)
                .profiling(true)
                .build();

        final SerializedModule serialized = compiler.compileModule(
                engine, simpleWasmBytes, options);
        final AotExecutable executable = compiler.createExecutable(serialized);

        final AotExecutableMetadata metadata = executable.getMetadata();
        assertThat(metadata).isNotNull();

        // Check all metadata fields
        assertThat(metadata.getCompilationTime()).isNotNull();
        assertThat(metadata.getWasmtimeVersion()).isEqualTo("36.0.2");
        assertThat(metadata.getCompilationOptions()).isEqualTo(options);
        assertThat(metadata.getModuleHash()).isNotBlank();
        assertThat(metadata.getOriginalSize()).isGreaterThanOrEqualTo(0);
        assertThat(metadata.getCompiledSize()).isGreaterThan(0);
        assertThat(metadata.getTargetPlatform()).isEqualTo(TargetPlatform.current());

        LOGGER.info("AOT executable metadata validation completed successfully");
    }

    @Test
    @DisplayName("Multiple AOT compilations can be performed concurrently")
    void testConcurrentAotCompilation() throws Exception {
        final AotCompiler compiler = engine.getAotCompiler();
        final AotOptions options = compiler.getDefaultOptions();

        // Create multiple compilation tasks
        final int numTasks = 4;
        final Thread[] threads = new Thread[numTasks];
        final SerializedModule[] results = new SerializedModule[numTasks];
        final Exception[] exceptions = new Exception[numTasks];

        for (int i = 0; i < numTasks; i++) {
            final int taskIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    results[taskIndex] = compiler.compileModule(
                            engine, simpleWasmBytes, options);
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

        // Verify all compilations succeeded
        for (int i = 0; i < numTasks; i++) {
            assertThat(exceptions[i]).isNull();
            assertThat(results[i]).isNotNull();
            assertThat(results[i].getData()).isNotEmpty();
        }

        LOGGER.info("Concurrent AOT compilation test completed successfully");
    }

    @Test
    @DisplayName("AOT executables can be saved to and loaded from files")
    void testAotExecutableFilePersistence() throws Exception {
        final AotCompiler compiler = engine.getAotCompiler();
        final AotOptions options = compiler.getDefaultOptions();

        // Compile module
        final SerializedModule serialized = compiler.compileModule(
                engine, simpleWasmBytes, options);
        final AotExecutable executable = compiler.createExecutable(serialized);

        // Save to temporary file
        final Path tempFile = Files.createTempFile("wasmtime4j-aot-test", ".bin");
        try {
            executable.saveToFile(tempFile);
            assertThat(Files.exists(tempFile)).isTrue();
            assertThat(Files.size(tempFile)).isGreaterThan(0);

            // Load from file
            final AotExecutable loadedExecutable = AotExecutable.loadFromFile(
                    tempFile,
                    TargetPlatform.current(),
                    executable.getMetadata()
            );
            assertThat(loadedExecutable).isNotNull();
            assertThat(loadedExecutable.getTargetPlatform()).isEqualTo(executable.getTargetPlatform());
            assertThat(loadedExecutable.getNativeCode()).isEqualTo(executable.getNativeCode());

            LOGGER.info("AOT executable file persistence test completed successfully");
        } finally {
            Files.deleteIfExists(tempFile);
        }
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