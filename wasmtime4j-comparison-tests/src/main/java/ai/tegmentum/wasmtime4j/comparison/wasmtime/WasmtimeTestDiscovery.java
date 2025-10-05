package ai.tegmentum.wasmtime4j.comparison.wasmtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Discovers tests from the upstream Wasmtime repository.
 *
 * <p>This class parses Rust test files from https://github.com/bytecodealliance/wasmtime to
 * extract test metadata that can be used to generate equivalent wasmtime4j tests.
 */
public final class WasmtimeTestDiscovery {

  private static final Logger LOGGER = Logger.getLogger(WasmtimeTestDiscovery.class.getName());

  // Pattern to match #[wasmtime_test] or #[test] annotations
  private static final Pattern TEST_ANNOTATION_PATTERN =
      Pattern.compile("^\\s*#\\[(wasmtime_test|test)\\]");

  // Pattern to match function declaration after test annotation
  private static final Pattern FUNCTION_PATTERN =
      Pattern.compile("^\\s*fn\\s+(\\w+)\\s*\\(");

  // Pattern to match WAT code in wat::parse_str
  // Handles both r#"..."# and r"..." formats
  private static final Pattern WAT_PATTERN =
      Pattern.compile("wat::parse_str\\s*\\(\\s*r#?\"([\\s\\S]*?)\"#?\\s*\\)", Pattern.DOTALL);

  // Pattern to match assert_eq! statements
  private static final Pattern ASSERT_PATTERN =
      Pattern.compile("assert_eq!\\s*\\(([^)]+)\\)");

  private final Path wasmtimeRepoPath;

  /**
   * Creates a new Wasmtime test discovery engine.
   *
   * @param wasmtimeRepoPath path to the Wasmtime repository
   */
  public WasmtimeTestDiscovery(final Path wasmtimeRepoPath) {
    if (wasmtimeRepoPath == null) {
      throw new IllegalArgumentException("wasmtimeRepoPath cannot be null");
    }
    if (!Files.isDirectory(wasmtimeRepoPath)) {
      throw new IllegalArgumentException(
          "wasmtimeRepoPath must be a directory: " + wasmtimeRepoPath);
    }
    this.wasmtimeRepoPath = wasmtimeRepoPath;
  }

  /**
   * Discovers all relevant tests from the Wasmtime repository.
   *
   * <p>This focuses on Wasmtime's integration tests (tests/all/) which test the Wasmtime API
   * behavior, not the WebAssembly specification compliance tests. We assume Wasmtime correctly
   * implements the WebAssembly spec.
   *
   * @return list of discovered test metadata
   * @throws IOException if test discovery fails
   */
  public List<WasmtimeTestMetadata> discoverAllTests() throws IOException {
    final List<WasmtimeTestMetadata> allTests = new ArrayList<>();

    // Discover integration tests from tests/all/
    // These test Wasmtime's API behavior - exactly what we need to replicate
    allTests.addAll(discoverIntegrationTests());

    // Discover WAST tests from tests/misc_testsuite/
    // These test Wasmtime-specific features beyond the spec
    allTests.addAll(discoverWastTests());

    // NOTE: We explicitly skip tests/spec_testsuite and tests/wasi_testsuite
    // Those test WebAssembly spec compliance, which we assume Wasmtime handles correctly

    LOGGER.info(
        "Discovered "
            + allTests.size()
            + " Wasmtime integration tests (excluding spec conformance tests)");
    return allTests;
  }

  /**
   * Discovers integration tests from tests/all/ directory.
   *
   * @return list of integration test metadata
   * @throws IOException if discovery fails
   */
  public List<WasmtimeTestMetadata> discoverIntegrationTests() throws IOException {
    final Path testsAllPath = wasmtimeRepoPath.resolve("tests/all");
    if (!Files.isDirectory(testsAllPath)) {
      LOGGER.warning("Integration tests directory not found: " + testsAllPath);
      return List.of();
    }

    final List<WasmtimeTestMetadata> tests = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(testsAllPath)) {
      paths
          .filter(path -> path.toString().endsWith(".rs"))
          .forEach(
              path -> {
                try {
                  tests.addAll(parseRustTestFile(path));
                } catch (final IOException e) {
                  LOGGER.warning(
                      "Failed to parse test file " + path + ": " + e.getMessage());
                }
              });
    }

    LOGGER.info("Discovered " + tests.size() + " integration tests from tests/all/");
    return tests;
  }

  /**
   * Discovers WAST tests from tests/misc_testsuite/ directory.
   *
   * @return list of WAST test metadata
   * @throws IOException if discovery fails
   */
  public List<WasmtimeTestMetadata> discoverWastTests() throws IOException {
    final Path wastPath = wasmtimeRepoPath.resolve("tests/misc_testsuite");
    if (!Files.isDirectory(wastPath)) {
      LOGGER.warning("WAST tests directory not found: " + wastPath);
      return List.of();
    }

    final List<WasmtimeTestMetadata> tests = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(wastPath)) {
      paths
          .filter(path -> path.toString().endsWith(".wast"))
          .forEach(
              path -> {
                try {
                  tests.add(parseWastFile(path));
                } catch (final IOException e) {
                  LOGGER.warning(
                      "Failed to parse WAST file " + path + ": " + e.getMessage());
                }
              });
    }

    LOGGER.info("Discovered " + tests.size() + " WAST tests from tests/misc_testsuite/");
    return tests;
  }

  /**
   * Parses a Rust test file to extract test metadata.
   *
   * @param rustFile path to Rust test file
   * @return list of test metadata from the file
   * @throws IOException if parsing fails
   */
  private List<WasmtimeTestMetadata> parseRustTestFile(final Path rustFile) throws IOException {
    final List<String> lines = Files.readAllLines(rustFile);
    final List<WasmtimeTestMetadata> tests = new ArrayList<>();

    for (int i = 0; i < lines.size(); i++) {
      final String line = lines.get(i);

      // Look for test annotation
      if (TEST_ANNOTATION_PATTERN.matcher(line).find()) {
        // Next non-empty line should be the function declaration
        for (int j = i + 1; j < lines.size(); j++) {
          final String funcLine = lines.get(j);
          if (funcLine.trim().isEmpty() || funcLine.trim().startsWith("#")) {
            continue;
          }

          final Matcher matcher = FUNCTION_PATTERN.matcher(funcLine);
          if (matcher.find()) {
            final String testName = matcher.group(1);
            final WasmtimeTestMetadata metadata =
                extractTestMetadata(rustFile, testName, j + 1, lines, j);
            if (metadata != null) {
              tests.add(metadata);
            }
          }
          break;
        }
      }
    }

    return tests;
  }

  /**
   * Extracts test metadata from a test function.
   *
   * @param sourceFile source file containing the test
   * @param testName name of the test function
   * @param startLine line number where test starts
   * @param lines all lines in the file
   * @param currentLine current line being processed
   * @return test metadata or null if extraction fails
   */
  private WasmtimeTestMetadata extractTestMetadata(
      final Path sourceFile,
      final String testName,
      final int startLine,
      final List<String> lines,
      final int currentLine) {

    // Extract category from file path
    final String category = extractCategory(sourceFile);

    // Find WAT code in the test - need to combine multiple lines for the regex
    String watCode = null;
    final List<String> expectedResults = new ArrayList<>();

    // Combine multiple lines to handle multi-line WAT code
    final StringBuilder functionCode = new StringBuilder();
    int braceDepth = 0;
    boolean foundStartBrace = false;

    for (int i = currentLine; i < Math.min(lines.size(), currentLine + 300); i++) {
      final String line = lines.get(i);
      functionCode.append(line).append("\n");

      // Track brace depth
      for (final char c : line.toCharArray()) {
        if (c == '{') {
          braceDepth++;
          foundStartBrace = true;
        } else if (c == '}') {
          braceDepth--;
        }
      }

      // Stop at end of function
      if (foundStartBrace && braceDepth == 0) {
        break;
      }
    }

    final String functionText = functionCode.toString();

    // Extract WAT code from the combined text
    final Matcher watMatcher = WAT_PATTERN.matcher(functionText);
    if (watMatcher.find()) {
      watCode = watMatcher.group(1).trim();
    }

    // Extract assertions
    final Matcher assertMatcher = ASSERT_PATTERN.matcher(functionText);
    while (assertMatcher.find()) {
      expectedResults.add(assertMatcher.group(1).trim());
    }

    if (watCode == null) {
      // Test doesn't have WAT code, might be a different kind of test
      return null;
    }

    return WasmtimeTestMetadata.builder()
        .testName(testName)
        .category(category)
        .sourceFile(sourceFile)
        .lineNumber(startLine)
        .watCode(watCode)
        .expectedResults(expectedResults)
        .requiresWasi(watCode.contains("wasi"))
        .requiresComponent(category.contains("component"))
        .requiresThreads(watCode.contains("thread") || category.contains("thread"))
        .requiresGc(watCode.contains("struct") || watCode.contains("array"))
        .build();
  }

  /**
   * Parses a WAST test file.
   *
   * @param wastFile path to WAST file
   * @return test metadata
   * @throws IOException if parsing fails
   */
  private WasmtimeTestMetadata parseWastFile(final Path wastFile) throws IOException {
    final String content = Files.readString(wastFile);
    final String testName = wastFile.getFileName().toString().replace(".wast", "");

    return WasmtimeTestMetadata.builder()
        .testName(testName)
        .category("misc_testsuite")
        .sourceFile(wastFile)
        .lineNumber(1)
        .watCode(content)
        .build();
  }

  /**
   * Extracts category from file path.
   *
   * @param path file path
   * @return category name
   */
  private String extractCategory(final Path path) {
    final Path relativePath = wasmtimeRepoPath.relativize(path);
    final String pathStr = relativePath.toString();

    // Extract from "tests/all/category.rs" or "tests/all/category/subcategory.rs"
    if (pathStr.startsWith("tests/all/")) {
      final String remainder = pathStr.substring("tests/all/".length());
      final int slashIndex = remainder.indexOf('/');
      if (slashIndex > 0) {
        return remainder.substring(0, slashIndex);
      }
      return remainder.replace(".rs", "");
    }

    return "unknown";
  }

  /**
   * Creates a default discovery instance using the standard Wasmtime repository location.
   *
   * @return test discovery instance
   */
  public static WasmtimeTestDiscovery createDefault() {
    // Try to find wasmtime repository in common locations
    final String userHome = System.getProperty("user.home");
    final Path[] possiblePaths = {
      Paths.get(userHome, "git", "wasmtime"),
      Paths.get(userHome, "workspace", "wasmtime"),
      Paths.get("/usr/local/src/wasmtime"),
      Paths.get("../../wasmtime")
    };

    for (final Path path : possiblePaths) {
      if (Files.isDirectory(path)) {
        LOGGER.info("Found Wasmtime repository at: " + path);
        return new WasmtimeTestDiscovery(path);
      }
    }

    throw new IllegalStateException(
        "Could not find Wasmtime repository. Please set WASMTIME_REPO_PATH environment variable.");
  }
}
