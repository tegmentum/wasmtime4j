package ai.tegmentum.wasmtime4j.comparison.wasmtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

  // Threshold for storing WAT in external file (50KB)
  private static final int WAT_FILE_THRESHOLD = 50 * 1024;

  private final Path outputDirectory;
  private final Path resourcesDirectory;

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
    // Resources go in src/test/resources relative to the test directory
    this.resourcesDirectory =
        outputDirectory.getParent().getParent().resolve("resources/wasmtime-tests");
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
    // Convert category to valid Java package name (lowercase, no underscores/hyphens)
    final String category =
        metadata
            .getCategory()
            .replace('-', '_')
            .replace('/', '.')
            .replaceAll("_", ""); // Remove underscores for valid package names
    return "ai.tegmentum.wasmtime4j.comparison.generated." + category.toLowerCase();
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
    code.append("import static org.junit.jupiter.api.Assertions.fail;\n\n");
    code.append("import ai.tegmentum.wasmtime4j.Engine;\n");
    code.append("import ai.tegmentum.wasmtime4j.Module;\n");
    code.append("import ai.tegmentum.wasmtime4j.Store;\n");
    code.append("import java.io.InputStream;\n");
    code.append("import org.junit.jupiter.api.DisplayName;\n");
    code.append("import org.junit.jupiter.api.Test;\n\n");

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
    code.append("  @DisplayName(\"").append(metadata.getTestId()).append("\")\n");
    code.append("  public void test").append(className.replace("Test", "")).append("() {\n");
    code.append("    // WAT code from original Wasmtime test:\n");

    // Add WAT code as comment (truncated if too large)
    final String watCode = metadata.getWatCode();
    final int watSize = watCode.getBytes(StandardCharsets.UTF_8).length;
    final boolean useExternalFile = watSize > WAT_FILE_THRESHOLD;

    if (useExternalFile) {
      code.append("    // WAT code is large (")
          .append(watSize / 1024)
          .append(" KB), loaded from external resource file\n\n");
    } else {
      final String[] watLines = watCode.split("\n");
      for (final String line : watLines) {
        code.append("    // ").append(line).append("\n");
      }
      code.append("\n");
    }

    // Generate WAT loading code
    try {
      if (useExternalFile) {
        // Save WAT to external file and generate load code
        final String resourcePath =
            saveWatToFile(watCode, metadata.getCategory(), metadata.getTestName());
        code.append(indent(generateWatLoadCode(resourcePath), 4));
      } else {
        // Inline WAT string
        code.append("    final String wat = \"\"\"\n");
        code.append(indent(escapeWatCode(watCode), 8));
        code.append("    \"\"\";\n");
      }
    } catch (final IOException e) {
      LOGGER.warning("Failed to save WAT to file, falling back to inline: " + e.getMessage());
      // Fallback to inline
      code.append("    final String wat = \"\"\"\n");
      code.append(indent(escapeWatCode(watCode), 8));
      code.append("    \"\"\";\n");
    }
    code.append("\n");

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
    return text.lines()
            .map(line -> indentation + line)
            .collect(java.util.stream.Collectors.joining("\n"))
        + "\n";
  }

  /**
   * Saves WAT code to an external resource file.
   *
   * @param watCode the WAT code to save
   * @param category test category (e.g., "misc_testsuite")
   * @param testName test name
   * @return path to the resource file relative to resources directory
   * @throws IOException if file cannot be written
   */
  private String saveWatToFile(final String watCode, final String category, final String testName)
      throws IOException {
    // Create category directory
    final Path categoryDir = resourcesDirectory.resolve(category);
    Files.createDirectories(categoryDir);

    // Save WAT file
    final String fileName = testName.toLowerCase().replace('_', '-') + ".wat";
    final Path watFile = categoryDir.resolve(fileName);
    Files.writeString(watFile, watCode, StandardCharsets.UTF_8);

    LOGGER.info("Saved large WAT to resource file: " + watFile);

    // Return resource path for loading
    return "/wasmtime-tests/" + category + "/" + fileName;
  }

  /**
   * Generates code to load WAT from a resource file.
   *
   * @param resourcePath path to the resource file
   * @return Java code to load WAT from file
   */
  private String generateWatLoadCode(final String resourcePath) {
    return String.format(
        """
        final String wat;
        try (final InputStream is = getClass().getResourceAsStream("%s")) {
          if (is == null) {
            throw new AssertionError("WAT resource not found: %s");
          }
          wat = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (final java.io.IOException e) {
          throw new AssertionError("Failed to load WAT resource: " + e.getMessage(), e);
        }
        """,
        resourcePath, resourcePath);
  }
}
