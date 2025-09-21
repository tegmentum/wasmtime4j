package ai.tegmentum.wasmtime4j.webassembly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for loading and managing WebAssembly test suites. Handles official WebAssembly
 * Specification tests and Wasmtime-specific tests.
 */
public final class WasmTestSuiteLoader {
  private static final Logger LOGGER = Logger.getLogger(WasmTestSuiteLoader.class.getName());

  /** Test suite categories for WebAssembly test execution. */
  public enum TestSuiteType {
    WEBASSEMBLY_SPEC("webassembly-spec"),
    WASMTIME_TESTS("wasmtime-tests"),
    WASI_TESTS("wasi-tests"),
    CUSTOM_TESTS("custom-tests");

    private final String directoryName;

    TestSuiteType(final String directoryName) {
      this.directoryName = directoryName;
    }

    public String getDirectoryName() {
      return directoryName;
    }
  }

  private WasmTestSuiteLoader() {
    // Utility class - prevent instantiation
  }

  /**
   * Loads all test cases for a specific test suite type.
   *
   * @param suiteType the test suite type to load
   * @return list of test case descriptors
   * @throws IOException if test files cannot be read
   */
  public static List<WasmTestCase> loadTestSuite(final TestSuiteType suiteType) throws IOException {
    final Path suiteDirectory = getTestSuiteDirectory(suiteType);

    if (!Files.exists(suiteDirectory)) {
      LOGGER.warning(
          "Test suite directory not found: "
              + suiteDirectory
              + ". Skipping "
              + suiteType.name()
              + " tests.");
      return new ArrayList<>();
    }

    LOGGER.info("Loading test suite: " + suiteType.name() + " from " + suiteDirectory);

    final List<WasmTestCase> testCases = new ArrayList<>();

    try (final Stream<Path> paths = Files.walk(suiteDirectory)) {
      final List<Path> wasmFiles =
          paths
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".wasm"))
              .collect(Collectors.toList());

      for (final Path wasmFile : wasmFiles) {
        final WasmTestCase testCase = createTestCase(wasmFile, suiteType);
        testCases.add(testCase);
      }
    }

    LOGGER.info("Loaded " + testCases.size() + " test cases for " + suiteType.name());
    return testCases;
  }

  /**
   * Loads a specific test case by name from any test suite.
   *
   * @param testName the test case name (without .wasm extension)
   * @return the test case if found
   * @throws IOException if test files cannot be read
   */
  public static Optional<WasmTestCase> loadTestCase(final String testName) throws IOException {
    for (final TestSuiteType suiteType : TestSuiteType.values()) {
      final Path suiteDirectory = getTestSuiteDirectory(suiteType);
      if (!Files.exists(suiteDirectory)) {
        continue;
      }

      final Path testFile = suiteDirectory.resolve(testName + ".wasm");
      if (Files.exists(testFile)) {
        return Optional.of(createTestCase(testFile, suiteType));
      }
    }

    return Optional.empty();
  }

  /**
   * Gets the directory path for a specific test suite type.
   *
   * @param suiteType the test suite type
   * @return the directory path for the test suite
   */
  public static Path getTestSuiteDirectory(final TestSuiteType suiteType) {
    return getTestResourcesPath().resolve("wasm").resolve(suiteType.getDirectoryName());
  }

  /**
   * Creates a test case descriptor from a WebAssembly file.
   *
   * @param wasmFile the WebAssembly file path
   * @param suiteType the test suite type
   * @return the test case descriptor
   * @throws IOException if the file cannot be read
   */
  private static WasmTestCase createTestCase(final Path wasmFile, final TestSuiteType suiteType)
      throws IOException {
    final String fileName = wasmFile.getFileName().toString();
    final String testName = fileName.substring(0, fileName.length() - 5); // Remove .wasm extension

    final byte[] moduleBytes = Files.readAllBytes(wasmFile);

    // Look for corresponding expected results file
    final Path expectedFile = wasmFile.getParent().resolve(testName + ".expected");
    final Optional<String> expectedResults =
        Files.exists(expectedFile) ? Optional.of(Files.readString(expectedFile)) : Optional.empty();

    // Look for corresponding test metadata file
    final Path metadataFile = wasmFile.getParent().resolve(testName + ".json");
    final Optional<String> metadata =
        Files.exists(metadataFile) ? Optional.of(Files.readString(metadataFile)) : Optional.empty();

    return new WasmTestCase(testName, suiteType, wasmFile, moduleBytes, expectedResults, metadata);
  }

  /**
   * Gets the test resources directory path.
   *
   * @return the test resources directory path
   */
  private static Path getTestResourcesPath() {
    final String resourcesPath = System.getProperty("wasmtime4j.test.resources");
    if (resourcesPath != null) {
      return Paths.get(resourcesPath);
    }
    return Paths.get("src", "test", "resources");
  }

  /**
   * Downloads and extracts official WebAssembly test suites if not present. This method should be
   * called during test initialization.
   *
   * @throws IOException if test suites cannot be downloaded or extracted
   */
  public static void ensureTestSuitesAvailable() throws IOException {
    final Path wasmTestsRoot = getTestResourcesPath().resolve("wasm");

    // Try to download test suites automatically if enabled
    WasmSpecTestDownloader.downloadTestSuitesIfEnabled(wasmTestsRoot);

    // Check if WebAssembly spec tests are available
    final Path specTestsDir =
        wasmTestsRoot.resolve(TestSuiteType.WEBASSEMBLY_SPEC.getDirectoryName());
    if (!Files.exists(specTestsDir) || isDirectoryEmpty(specTestsDir)) {
      LOGGER.info(
          "WebAssembly specification tests not found. "
              + "To download automatically, use: -Dwasmtime4j.test.download-suites=true "
              + "Or manually download from https://github.com/WebAssembly/spec/tree/main/test");
      Files.createDirectories(specTestsDir);
    } else {
      final long testFileCount = countTestFiles(specTestsDir, ".wasm");
      LOGGER.info("WebAssembly spec tests available: " + testFileCount + " WASM files found");
    }

    // Check if Wasmtime tests are available
    final Path wasmtimeTestsDir =
        wasmTestsRoot.resolve(TestSuiteType.WASMTIME_TESTS.getDirectoryName());
    if (!Files.exists(wasmtimeTestsDir) || isDirectoryEmpty(wasmtimeTestsDir)) {
      LOGGER.info(
          "Wasmtime tests not found. To download automatically, use:"
              + " -Dwasmtime4j.test.download-suites=true Or manually download from"
              + " https://github.com/bytecodealliance/wasmtime/tree/main/tests");
      Files.createDirectories(wasmtimeTestsDir);
    } else {
      final long testFileCount = countTestFiles(wasmtimeTestsDir, ".wasm");
      LOGGER.info("Wasmtime tests available: " + testFileCount + " WASM files found");
    }

    // Check if WASI tests are available
    final Path wasiTestsDir = wasmTestsRoot.resolve(TestSuiteType.WASI_TESTS.getDirectoryName());
    if (!Files.exists(wasiTestsDir)) {
      LOGGER.info("WASI tests not found. Creating directory for custom WASI tests.");
      Files.createDirectories(wasiTestsDir);
    }

    // Ensure custom tests directory exists
    final Path customTestsDir =
        wasmTestsRoot.resolve(TestSuiteType.CUSTOM_TESTS.getDirectoryName());
    if (!Files.exists(customTestsDir)) {
      Files.createDirectories(customTestsDir);
    }
  }

  /**
   * Validates that a WebAssembly module has the correct magic number and version.
   *
   * @param moduleBytes the WebAssembly module bytes
   * @return true if the module has valid WebAssembly header
   */
  public static boolean isValidWasmModule(final byte[] moduleBytes) {
    if (moduleBytes.length < 8) {
      return false;
    }

    // Check magic number: 0x00 0x61 0x73 0x6d
    if (moduleBytes[0] != 0x00
        || moduleBytes[1] != 0x61
        || moduleBytes[2] != 0x73
        || moduleBytes[3] != 0x6d) {
      return false;
    }

    // Check version: 0x01 0x00 0x00 0x00
    if (moduleBytes[4] != 0x01
        || moduleBytes[5] != 0x00
        || moduleBytes[6] != 0x00
        || moduleBytes[7] != 0x00) {
      return false;
    }

    return true;
  }

  /**
   * Gets statistics about loaded test suites.
   *
   * @return test suite statistics
   * @throws IOException if test directories cannot be read
   */
  public static WasmTestSuiteStats getTestSuiteStatistics() throws IOException {
    final WasmTestSuiteStats stats = new WasmTestSuiteStats();

    for (final TestSuiteType suiteType : TestSuiteType.values()) {
      final List<WasmTestCase> testCases = loadTestSuite(suiteType);
      stats.addSuiteStats(suiteType, testCases.size());
    }

    return stats;
  }

  /**
   * Checks if a directory is empty.
   *
   * @param directory the directory to check
   * @return true if the directory is empty or doesn't exist
   * @throws IOException if directory cannot be read
   */
  private static boolean isDirectoryEmpty(final Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return true;
    }

    try (final var files = Files.newDirectoryStream(directory)) {
      return !files.iterator().hasNext();
    }
  }

  /**
   * Counts the number of files with a specific extension in a directory.
   *
   * @param directory the directory to search
   * @param extension the file extension to count (e.g., ".wasm")
   * @return the number of files with the extension
   * @throws IOException if directory cannot be read
   */
  private static long countTestFiles(final Path directory, final String extension)
      throws IOException {
    if (!Files.exists(directory)) {
      return 0;
    }

    try (final Stream<Path> paths = Files.walk(directory)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(extension))
          .count();
    }
  }
}
