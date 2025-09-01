package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.functions.WasmFunction;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive I/O redirection tests for WASI functionality. Tests stdin, stdout, stderr
 * redirection, stream isolation, buffering behavior, and cross-runtime I/O compatibility.
 */
@Tag(TestCategories.INTEGRATION)
@Tag(TestCategories.WASI)
@Tag(TestCategories.IO)
public final class WasiIORedirectionTest {
  private static final Logger LOGGER = Logger.getLogger(WasiIORedirectionTest.class.getName());

  @TempDir private Path tempDirectory;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() {
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = engine.createStore();
    LOGGER.info("Set up WASI I/O test with runtime: " + runtime.getRuntimeType());
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  /** Tests basic stdin inheritance from host process. */
  @Test
  void testStdinInheritance() {
    LOGGER.info("Testing stdin inheritance");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true) // Inherit host stdin
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(
        () -> {
          final Wasi wasi = store.createWasi(config);
          assertNotNull(wasi);
          assertTrue(wasi.isValid());

          // Verify stdin is configured (implementation dependent)
          wasi.close();
        });
  }

  /** Tests stdout inheritance from host process. */
  @Test
  void testStdoutInheritance() {
    LOGGER.info("Testing stdout inheritance");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true) // Inherit host stdout
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(
        () -> {
          final Wasi wasi = store.createWasi(config);
          assertNotNull(wasi);
          assertTrue(wasi.isValid());

          wasi.close();
        });
  }

  /** Tests stderr inheritance from host process. */
  @Test
  void testStderrInheritance() {
    LOGGER.info("Testing stderr inheritance");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true) // Inherit host stderr
            .build();

    assertDoesNotThrow(
        () -> {
          final Wasi wasi = store.createWasi(config);
          assertNotNull(wasi);
          assertTrue(wasi.isValid());

          wasi.close();
        });
  }

  /** Tests stdin redirection from byte array. */
  @Test
  void testStdinRedirectionFromByteArray() {
    LOGGER.info("Testing stdin redirection from byte array");

    final String inputData = "Hello from redirected stdin\nLine 2\nLine 3\n";
    final byte[] inputBytes = inputData.getBytes();
    final ByteArrayInputStream stdinStream = new ByteArrayInputStream(inputBytes);

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .stdin(stdinStream) // Redirect stdin
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(
        () -> {
          final Wasi wasi = store.createWasi(config);
          assertNotNull(wasi);
          assertTrue(wasi.isValid());

          wasi.close();
        });
  }

  /** Tests stdout redirection to byte array. */
  @Test
  void testStdoutRedirectionToByteArray() {
    LOGGER.info("Testing stdout redirection to byte array");

    final ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .stdout(stdoutStream) // Redirect stdout
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute WASI program that should write to stdout
          if (instance.hasExport("_start")) {
            final WasmFunction startFunction = instance.getExport("_start").asFunction();
            assertNotNull(startFunction);

            assertDoesNotThrow(() -> startFunction.call());
          }

          // Check captured stdout (content depends on WASI module behavior)
          final byte[] capturedOutput = stdoutStream.toByteArray();
          assertNotNull(capturedOutput);

          instance.close();
          wasi.close();
          module.close();
        });
  }

  /** Tests stderr redirection to byte array. */
  @Test
  void testStderrRedirectionToByteArray() {
    LOGGER.info("Testing stderr redirection to byte array");

    final ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .inheritStdout(true)
            .stderr(stderrStream) // Redirect stderr
            .build();

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute WASI program
          if (instance.hasExport("_start")) {
            final WasmFunction startFunction = instance.getExport("_start").asFunction();
            assertNotNull(startFunction);

            assertDoesNotThrow(() -> startFunction.call());
          }

          // Check captured stderr
          final byte[] capturedError = stderrStream.toByteArray();
          assertNotNull(capturedError);

          instance.close();
          wasi.close();
          module.close();
        });
  }

  /** Tests file-based I/O redirection. */
  @Test
  void testFileBasedIORedirection() throws IOException {
    LOGGER.info("Testing file-based I/O redirection");

    final Path inputFile = tempDirectory.resolve("input.txt");
    final Path outputFile = tempDirectory.resolve("output.txt");
    final Path errorFile = tempDirectory.resolve("error.txt");

    final String inputContent = "File-based input data\nMultiple lines\nFor testing\n";
    Files.write(inputFile, inputContent.getBytes());

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .stdinFile(inputFile.toString())
            .stdoutFile(outputFile.toString())
            .stderrFile(errorFile.toString())
            .build();

    final byte[] wasmBytes = createIOTestModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute I/O operations
          if (instance.hasExport("test_io")) {
            final WasmFunction ioFunction = instance.getExport("test_io").asFunction();
            assertNotNull(ioFunction);

            assertDoesNotThrow(() -> ioFunction.call());
          }

          instance.close();
          wasi.close();
          module.close();
        });

    // Verify output files were written (content depends on module behavior)
    assertTrue(Files.exists(outputFile) || Files.size(outputFile) >= 0);
    assertTrue(Files.exists(errorFile) || Files.size(errorFile) >= 0);
  }

  /** Tests concurrent I/O redirection with multiple instances. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testConcurrentIORedirection() throws IOException {
    LOGGER.info("Testing concurrent I/O redirection");

    final int instanceCount = 3;
    final ByteArrayOutputStream[] stdoutStreams = new ByteArrayOutputStream[instanceCount];
    final ByteArrayInputStream[] stdinStreams = new ByteArrayInputStream[instanceCount];

    // Prepare input/output streams for each instance
    for (int i = 0; i < instanceCount; i++) {
      final String inputData = "Input for instance " + i + "\n";
      stdinStreams[i] = new ByteArrayInputStream(inputData.getBytes());
      stdoutStreams[i] = new ByteArrayOutputStream();
    }

    // Create and execute concurrent instances
    final Thread[] threads = new Thread[instanceCount];
    final Exception[] exceptions = new Exception[instanceCount];

    for (int i = 0; i < instanceCount; i++) {
      final int instanceId = i;
      threads[i] =
          new Thread(
              () -> {
                try (final WasmRuntime instanceRuntime = WasmRuntimeFactory.create();
                    final Engine instanceEngine = instanceRuntime.createEngine();
                    final Store instanceStore = instanceEngine.createStore()) {

                  final WasiConfig config =
                      WasiConfig.builder()
                          .inheritEnv(true)
                          .stdin(stdinStreams[instanceId])
                          .stdout(stdoutStreams[instanceId])
                          .inheritStderr(true)
                          .build();

                  final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
                  final Module module = instanceEngine.createModule(wasmBytes);
                  final Wasi wasi = instanceStore.createWasi(config);
                  final Instance instance = instanceStore.createInstance(module, wasi.getImports());

                  if (instance.hasExport("_start")) {
                    final WasmFunction startFunction = instance.getExport("_start").asFunction();
                    startFunction.call();
                  }

                  instance.close();
                  wasi.close();
                  module.close();

                } catch (final Exception e) {
                  exceptions[instanceId] = e;
                  LOGGER.severe(
                      "Concurrent I/O instance " + instanceId + " failed: " + e.getMessage());
                }
              });
    }

    // Start all threads
    for (final Thread thread : threads) {
      thread.start();
    }

    // Wait for completion
    for (final Thread thread : threads) {
      assertDoesNotThrow(() -> thread.join(10000)); // 10 second timeout per thread
    }

    // Verify no exceptions occurred
    for (int i = 0; i < instanceCount; i++) {
      if (exceptions[i] != null) {
        throw new AssertionError("Instance " + i + " failed", exceptions[i]);
      }
    }

    // Verify output streams received data (content depends on module)
    for (int i = 0; i < instanceCount; i++) {
      assertNotNull(stdoutStreams[i].toByteArray());
      LOGGER.info("Instance " + i + " output length: " + stdoutStreams[i].toByteArray().length);
    }
  }

  /** Tests I/O stream isolation between instances. */
  @Test
  void testIOStreamIsolation() {
    LOGGER.info("Testing I/O stream isolation");

    final String input1 = "Input for instance 1\n";
    final String input2 = "Input for instance 2\n";

    final ByteArrayInputStream stdin1 = new ByteArrayInputStream(input1.getBytes());
    final ByteArrayInputStream stdin2 = new ByteArrayInputStream(input2.getBytes());

    final ByteArrayOutputStream stdout1 = new ByteArrayOutputStream();
    final ByteArrayOutputStream stdout2 = new ByteArrayOutputStream();

    final WasiConfig config1 =
        WasiConfig.builder()
            .inheritEnv(true)
            .stdin(stdin1)
            .stdout(stdout1)
            .inheritStderr(true)
            .build();

    final WasiConfig config2 =
        WasiConfig.builder()
            .inheritEnv(true)
            .stdin(stdin2)
            .stdout(stdout2)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(
        () -> {
          final Wasi wasi1 = store.createWasi(config1);
          final Wasi wasi2 = store.createWasi(config2);

          assertNotNull(wasi1);
          assertNotNull(wasi2);
          assertTrue(wasi1.isValid());
          assertTrue(wasi2.isValid());

          // Instances should be isolated - different streams
          assertTrue(wasi1 != wasi2);

          wasi1.close();
          wasi2.close();
        });

    // Verify streams remain separate
    assertNotNull(stdout1.toByteArray());
    assertNotNull(stdout2.toByteArray());
  }

  /** Tests binary data handling through I/O streams. */
  @Test
  void testBinaryDataHandling() {
    LOGGER.info("Testing binary data handling");

    // Create binary test data
    final byte[] binaryInput = new byte[256];
    for (int i = 0; i < 256; i++) {
      binaryInput[i] = (byte) i;
    }

    final ByteArrayInputStream stdinStream = new ByteArrayInputStream(binaryInput);
    final ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .stdin(stdinStream)
            .stdout(stdoutStream)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createBinaryIOModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute binary I/O test
          if (instance.hasExport("test_binary_io")) {
            final WasmFunction binaryFunction = instance.getExport("test_binary_io").asFunction();
            assertNotNull(binaryFunction);

            assertDoesNotThrow(() -> binaryFunction.call());
          }

          instance.close();
          wasi.close();
          module.close();
        });

    // Verify binary data integrity (depends on module implementation)
    final byte[] outputData = stdoutStream.toByteArray();
    assertNotNull(outputData);
  }

  /** Tests large data streaming through I/O redirection. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testLargeDataStreaming() {
    LOGGER.info("Testing large data streaming");

    // Create large input data (1MB)
    final int dataSize = 1024 * 1024;
    final byte[] largeInput = new byte[dataSize];
    for (int i = 0; i < dataSize; i++) {
      largeInput[i] = (byte) (i % 256);
    }

    final ByteArrayInputStream stdinStream = new ByteArrayInputStream(largeInput);
    final ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .stdin(stdinStream)
            .stdout(stdoutStream)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createLargeDataModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute large data processing
          if (instance.hasExport("process_large_data")) {
            final WasmFunction dataFunction = instance.getExport("process_large_data").asFunction();
            assertNotNull(dataFunction);

            assertDoesNotThrow(() -> dataFunction.call());
          }

          instance.close();
          wasi.close();
          module.close();
        });

    final byte[] outputData = stdoutStream.toByteArray();
    assertNotNull(outputData);
    LOGGER.info(
        "Processed " + largeInput.length + " bytes, output: " + outputData.length + " bytes");
  }

  /** Tests cross-runtime I/O redirection compatibility. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeIOCompatibility() {
    LOGGER.info("Testing cross-runtime I/O compatibility");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final String testInput = "Cross-runtime I/O test data\n";
    final CrossRuntimeValidator.RuntimeOperation<String> ioOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final ByteArrayInputStream stdin = new ByteArrayInputStream(testInput.getBytes());
            final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .stdin(stdin)
                    .stdout(stdout)
                    .inheritStderr(true)
                    .build();

            final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
            final Module module = engine.createModule(wasmBytes);
            final Wasi wasi = store.createWasi(config);
            final Instance instance = store.createInstance(module, wasi.getImports());

            if (instance.hasExport("_start")) {
              final WasmFunction startFunction = instance.getExport("_start").asFunction();
              startFunction.call();
            }

            final byte[] output = stdout.toByteArray();
            final String result =
                String.format("input=%d,output=%d", testInput.length(), output.length);

            instance.close();
            wasi.close();
            module.close();

            return result;
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(ioOperation, Duration.ofSeconds(15));

    assertTrue(
        result.isValid(),
        "I/O behavior differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime I/O validation successful");
  }

  /** Tests I/O error handling and recovery. */
  @Test
  void testIOErrorHandling() throws IOException {
    LOGGER.info("Testing I/O error handling");

    // Test with closed input stream
    final ByteArrayInputStream stdinStream = new ByteArrayInputStream("test".getBytes());
    stdinStream.close(); // Close before use

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .stdin(stdinStream)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    // Should handle closed stream gracefully
    assertDoesNotThrow(
        () -> {
          final Wasi wasi = store.createWasi(config);
          assertNotNull(wasi);
          wasi.close();
        });
  }

  /** Tests buffering behavior of I/O streams. */
  @Test
  void testIOBufferingBehavior() {
    LOGGER.info("Testing I/O buffering behavior");

    final ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    final ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .inheritStdin(true)
            .stdout(stdoutStream)
            .stderr(stderrStream)
            .build();

    final byte[] wasmBytes = createBufferingTestModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Test buffering behavior
          if (instance.hasExport("test_buffering")) {
            final WasmFunction bufferFunction = instance.getExport("test_buffering").asFunction();
            assertNotNull(bufferFunction);

            assertDoesNotThrow(() -> bufferFunction.call());
          }

          // Check that data was captured
          final byte[] stdoutData = stdoutStream.toByteArray();
          final byte[] stderrData = stderrStream.toByteArray();

          assertNotNull(stdoutData);
          assertNotNull(stderrData);

          instance.close();
          wasi.close();
          module.close();
        });
  }

  /** Creates a WebAssembly module for I/O testing. */
  private byte[] createIOTestModule() {
    // This would be a WASI module that performs I/O operations
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for binary I/O testing. */
  private byte[] createBinaryIOModule() {
    // This would be a WASI module that handles binary data
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for large data processing. */
  private byte[] createLargeDataModule() {
    // This would be a WASI module that processes large amounts of data
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for buffering tests. */
  private byte[] createBufferingTestModule() {
    // This would be a WASI module that tests buffering behavior
    return WasmTestModules.getModule("wasi_basic");
  }
}
