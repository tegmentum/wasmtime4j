package ai.tegmentum.wasmtime4j.comparison.wasmtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates equivalent Java tests from Wasmtime Rust tests.
 *
 * <p>This generator creates wasmtime4j test code that performs the same operations as the upstream
 * Wasmtime Rust tests, allowing direct comparison of behavior.
 */
public final class EquivalentJavaTestGenerator {

  private static final Logger LOGGER =
      Logger.getLogger(EquivalentJavaTestGenerator.class.getName());

  private final Path outputDirectory;

  /**
   * Creates a new test generator.
   *
   * @param outputDirectory directory where generated tests will be written
   */
  public EquivalentJavaTestGenerator(final Path outputDirectory) {
    if (outputDirectory == null) {
      throw new IllegalArgumentException("outputDirectory cannot be null");
    }
    this.outputDirectory = outputDirectory;
  }

  /**
   * Generates Java test code from Wasmtime test metadata.
   *
   * @param metadata test metadata from Wasmtime
   * @return path to generated test file
   * @throws IOException if generation fails
   */
  public Path generateTest(final WasmtimeTestMetadata metadata) throws IOException {
    LOGGER.info("Generating Java test for: " + metadata.getTestId());

    final String className = generateClassName(metadata);
    final String packageName = generatePackageName(metadata);
    final String javaCode = generateJavaCode(metadata, className, packageName);

    final Path packagePath = outputDirectory.resolve(packageName.replace('.', '/'));
    Files.createDirectories(packagePath);

    final Path testFile = packagePath.resolve(className + ".java");
    Files.writeString(testFile, javaCode);

    LOGGER.info("Generated test file: " + testFile);
    return testFile;
  }

  /**
   * Generates all Java tests from a list of Wasmtime test metadata.
   *
   * @param metadataList list of test metadata
   * @return list of generated test file paths
   * @throws IOException if generation fails
   */
  public List<Path> generateAllTests(final List<WasmtimeTestMetadata> metadataList)
      throws IOException {
    LOGGER.info("Generating " + metadataList.size() + " Java tests");

    final List<Path> generatedFiles =
        metadataList.stream()
            .map(
                metadata -> {
                  try {
                    return generateTest(metadata);
                  } catch (final IOException e) {
                    LOGGER.warning(
                        "Failed to generate test for "
                            + metadata.getTestId()
                            + ": "
                            + e.getMessage());
                    return null;
                  }
                })
            .filter(path -> path != null)
            .toList();

    LOGGER.info("Successfully generated " + generatedFiles.size() + " Java tests");
    return generatedFiles;
  }

  /**
   * Generates Java class name from test metadata.
   *
   * @param metadata test metadata
   * @return Java class name
   */
  private String generateClassName(final WasmtimeTestMetadata metadata) {
    // Convert test-name or test_name to TestName
    // Replace hyphens and underscores with spaces, then camelCase
    final String sanitized = metadata.getTestName().replace('-', '_');
    final String[] parts = sanitized.split("_");
    final StringBuilder className = new StringBuilder();
    for (final String part : parts) {
      if (!part.isEmpty()) {
        className.append(Character.toUpperCase(part.charAt(0)));
        if (part.length() > 1) {
          className.append(part.substring(1));
        }
      }
    }
    className.append("Test");
    return className.toString();
  }

  /**
   * Generates Java package name from test metadata.
   *
   * @param metadata test metadata
   * @return package name
   */
  private String generatePackageName(final WasmtimeTestMetadata metadata) {
    final String category = metadata.getCategory().replace('-', '_').replace('/', '.');
    return "ai.tegmentum.wasmtime4j.comparison.generated." + category;
  }

  /**
   * Generates complete Java test code.
   *
   * @param metadata test metadata
   * @param className Java class name
   * @param packageName Java package name
   * @return generated Java source code
   */
  private String generateJavaCode(
      final WasmtimeTestMetadata metadata, final String className, final String packageName) {

    final StringBuilder code = new StringBuilder();

    // Package declaration
    code.append("package ").append(packageName).append(";\n\n");

    // Imports
    code.append("import org.junit.jupiter.api.Test;\n");
    code.append("import org.junit.jupiter.api.DisplayName;\n");
    code.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
    code.append("import ai.tegmentum.wasmtime4j.*;\n\n");

    // Class documentation
    code.append("/**\n");
    code.append(" * Equivalent Java test for Wasmtime test: ")
        .append(metadata.getTestId())
        .append("\n");
    code.append(" *\n");
    code.append(" * Original source: ")
        .append(metadata.getSourceFile().getFileName())
        .append(":")
        .append(metadata.getLineNumber())
        .append("\n");
    code.append(" * Category: ").append(metadata.getCategory()).append("\n");
    code.append(" *\n");
    code.append(" * This test validates that wasmtime4j produces the same results as\n");
    code.append(" * the upstream Wasmtime implementation for this test case.\n");
    code.append(" */\n");

    // Class declaration
    code.append("public final class ").append(className).append(" {\n\n");

    // Test method
    code.append("  @Test\n");
    code.append("  @DisplayName(\"")
        .append(metadata.getTestId())
        .append("\")\n");
    code.append("  public void test").append(className.replace("Test", "")).append("() {\n");
    code.append("    // WAT code from original Wasmtime test:\n");

    // Add WAT code as comment
    final String[] watLines = metadata.getWatCode().split("\n");
    for (final String line : watLines) {
      code.append("    // ").append(line).append("\n");
    }
    code.append("\n");

    // Generate test body
    code.append("    final String wat = \"\"\"\n");
    code.append(indent(escapeWatCode(metadata.getWatCode()), 8));
    code.append("    \"\"\";\n\n");

    code.append("    // TODO: Implement equivalent wasmtime4j test logic\n");
    code.append("    // 1. Create Engine\n");
    code.append("    // 2. Compile WAT to Module\n");
    code.append("    // 3. Instantiate Module\n");
    code.append("    // 4. Call exported functions\n");
    code.append("    // 5. Assert expected results\n\n");

    // Add expected results as comments
    if (!metadata.getExpectedResults().isEmpty()) {
      code.append("    // Expected results from original test:\n");
      for (final String result : metadata.getExpectedResults()) {
        code.append("    // ").append(result).append("\n");
      }
    }

    code.append("    fail(\"Test not yet implemented - awaiting test framework completion\");\n");
    code.append("  }\n");

    code.append("}\n");

    return code.toString();
  }

  /**
   * Escapes WAT code for Java string literals.
   *
   * <p>This method escapes backslashes in WAT code to prevent illegal escape sequences in Java
   * string literals. For example, "\01" becomes "\\01".
   *
   * @param watCode the WAT code to escape
   * @return escaped WAT code safe for Java string literals
   */
  private String escapeWatCode(final String watCode) {
    // Escape backslashes to prevent illegal escape sequences in Java strings
    return watCode.replace("\\", "\\\\");
  }

  /**
   * Indents text by the specified number of spaces.
   *
   * @param text text to indent
   * @param spaces number of spaces
   * @return indented text
   */
  private String indent(final String text, final int spaces) {
    final String indentation = " ".repeat(spaces);
    return text.lines().map(line -> indentation + line).collect(java.util.stream.Collectors.joining("\n")) + "\n";
  }
}
