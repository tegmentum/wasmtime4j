package ai.tegmentum.wasmtime4j.webassembly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Represents a single WebAssembly test case with associated metadata. */
public final class WasmTestCase {
  private final String testName;
  private final WasmTestSuiteLoader.TestSuiteType suiteType;
  private final Path filePath;
  private final byte[] moduleBytes;
  private final Optional<String> expectedResults;
  private final Optional<String> metadata;

  /**
   * Creates a new WebAssembly test case.
   *
   * @param testName the name of the test case
   * @param suiteType the test suite type this test belongs to
   * @param filePath the path to the WebAssembly file
   * @param moduleBytes the WebAssembly module bytes
   * @param expectedResults optional expected results for the test
   * @param metadata optional test metadata in JSON format
   */
  public WasmTestCase(
      final String testName,
      final WasmTestSuiteLoader.TestSuiteType suiteType,
      final Path filePath,
      final byte[] moduleBytes,
      final Optional<String> expectedResults,
      final Optional<String> metadata) {
    this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    this.suiteType = Objects.requireNonNull(suiteType, "suiteType cannot be null");
    this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
    this.moduleBytes = Objects.requireNonNull(moduleBytes, "moduleBytes cannot be null").clone();
    this.expectedResults =
        Objects.requireNonNull(expectedResults, "expectedResults cannot be null");
    this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
  }

  /**
   * Gets the test name.
   *
   * @return the test name
   */
  public String getTestName() {
    return testName;
  }

  /**
   * Gets the test suite type.
   *
   * @return the test suite type
   */
  public WasmTestSuiteLoader.TestSuiteType getSuiteType() {
    return suiteType;
  }

  /**
   * Gets the file path.
   *
   * @return the file path
   */
  public Path getFilePath() {
    return filePath;
  }

  /**
   * Gets the WebAssembly module bytes.
   *
   * @return a copy of the module bytes
   */
  public byte[] getModuleBytes() {
    return moduleBytes.clone();
  }

  /**
   * Gets the expected results for this test case.
   *
   * @return the expected results if available
   */
  public Optional<String> getExpectedResults() {
    return expectedResults;
  }

  /**
   * Gets the test metadata.
   *
   * @return the test metadata if available
   */
  public Optional<String> getMetadata() {
    return metadata;
  }

  /**
   * Checks if this test case has expected results.
   *
   * @return true if expected results are available
   */
  public boolean hasExpectedResults() {
    return expectedResults.isPresent();
  }

  /**
   * Checks if this test case has metadata.
   *
   * @return true if metadata is available
   */
  public boolean hasMetadata() {
    return metadata.isPresent();
  }

  /**
   * Gets the size of the WebAssembly module in bytes.
   *
   * @return the module size in bytes
   */
  public int getModuleSize() {
    return moduleBytes.length;
  }

  /**
   * Validates that this test case has a valid WebAssembly module.
   *
   * @return true if the module is valid
   */
  public boolean isValidWasmModule() {
    return WasmTestSuiteLoader.isValidWasmModule(moduleBytes);
  }

  /**
   * Gets a display name for this test case suitable for test reporting.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return suiteType.name().toLowerCase() + ":" + testName;
  }

  /**
   * Checks if this is a negative test case (expected to fail). Determines this based on test name
   * conventions and metadata.
   *
   * @return true if this is expected to be a failing test case
   */
  public boolean isNegativeTest() {
    // Check common negative test naming patterns
    final String lowerTestName = testName.toLowerCase();

    return lowerTestName.contains("invalid")
        || lowerTestName.contains("error")
        || lowerTestName.contains("fail")
        || lowerTestName.contains("bad")
        || lowerTestName.contains("malformed")
        || lowerTestName.contains("corrupt");
  }

  /**
   * Checks if this test case requires WASI functionality.
   *
   * @return true if WASI is required
   */
  public boolean requiresWasi() {
    return suiteType == WasmTestSuiteLoader.TestSuiteType.WASI_TESTS
        || testName.toLowerCase().contains("wasi");
  }

  /**
   * Creates a test case from a WebAssembly file with automatic metadata detection.
   *
   * @param wasmFile the WebAssembly file path
   * @param suiteType the test suite type
   * @return the test case created from the file
   * @throws IOException if the file cannot be read
   */
  public static WasmTestCase fromFile(
      final Path wasmFile, final WasmTestSuiteLoader.TestSuiteType suiteType) throws IOException {
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

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasmTestCase that = (WasmTestCase) obj;
    return Objects.equals(testName, that.testName)
        && suiteType == that.suiteType
        && Objects.equals(filePath, that.filePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(testName, suiteType, filePath);
  }

  @Override
  public String toString() {
    return "WasmTestCase{"
        + "testName='"
        + testName
        + '\''
        + ", suiteType="
        + suiteType
        + ", moduleSize="
        + moduleBytes.length
        + ", hasExpectedResults="
        + hasExpectedResults()
        + ", hasMetadata="
        + hasMetadata()
        + '}';
  }
}
