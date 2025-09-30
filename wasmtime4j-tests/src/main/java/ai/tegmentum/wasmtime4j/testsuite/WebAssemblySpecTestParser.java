package ai.tegmentum.wasmtime4j.testsuite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parser for official WebAssembly specification test files. Handles JSON-formatted spec test files
 * and extracts individual test cases.
 */
public final class WebAssemblySpecTestParser {

  private static final Logger LOGGER = Logger.getLogger(WebAssemblySpecTestParser.class.getName());

  private final ObjectMapper objectMapper;

  public WebAssemblySpecTestParser() {
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Parses a WebAssembly specification test file and extracts test cases.
   *
   * @param specTestFile path to the spec test JSON file
   * @return list of parsed test cases
   * @throws TestSuiteException if parsing fails
   */
  public List<WebAssemblyTestCase> parseSpecTestFile(final Path specTestFile)
      throws TestSuiteException {
    if (specTestFile == null || !Files.exists(specTestFile)) {
      throw new TestSuiteException("Spec test file does not exist: " + specTestFile);
    }

    try {
      LOGGER.fine("Parsing spec test file: " + specTestFile);

      final String content = Files.readString(specTestFile);
      final JsonNode rootNode = objectMapper.readTree(content);

      if (!rootNode.has("commands")) {
        throw new TestSuiteException("Invalid spec test file format: missing 'commands' field");
      }

      final JsonNode commandsNode = rootNode.get("commands");
      if (!commandsNode.isArray()) {
        throw new TestSuiteException("Invalid spec test file format: 'commands' is not an array");
      }

      final List<WebAssemblyTestCase> testCases = new ArrayList<>();
      final String baseTestId = generateBaseTestId(specTestFile);

      int commandIndex = 0;
      for (final JsonNode commandNode : commandsNode) {
        try {
          final WebAssemblyTestCase testCase =
              parseSpecCommand(commandNode, baseTestId, commandIndex, specTestFile);
          if (testCase != null) {
            testCases.add(testCase);
          }
        } catch (final Exception e) {
          LOGGER.warning(
              "Failed to parse command "
                  + commandIndex
                  + " in "
                  + specTestFile
                  + ": "
                  + e.getMessage());
        }
        commandIndex++;
      }

      LOGGER.fine("Parsed " + testCases.size() + " test cases from " + specTestFile);
      return testCases;

    } catch (final IOException e) {
      throw new TestSuiteException("Failed to read spec test file: " + specTestFile, e);
    } catch (final Exception e) {
      throw new TestSuiteException("Failed to parse spec test file: " + specTestFile, e);
    }
  }

  private WebAssemblyTestCase parseSpecCommand(
      final JsonNode commandNode,
      final String baseTestId,
      final int commandIndex,
      final Path specTestFile)
      throws TestSuiteException {

    if (!commandNode.has("type")) {
      return null; // Skip commands without type
    }

    final String commandType = commandNode.get("type").asText();
    final String testId = baseTestId + "_" + commandIndex + "_" + commandType;

    switch (commandType) {
      case "module":
        return parseModuleCommand(commandNode, testId, specTestFile);

      case "assert_return":
        return parseAssertReturnCommand(commandNode, testId, specTestFile);

      case "assert_trap":
        return parseAssertTrapCommand(commandNode, testId, specTestFile);

      case "assert_invalid":
        return parseAssertInvalidCommand(commandNode, testId, specTestFile);

      case "assert_malformed":
        return parseAssertMalformedCommand(commandNode, testId, specTestFile);

      case "assert_uninstantiable":
        return parseAssertUninstantiableCommand(commandNode, testId, specTestFile);

      case "invoke":
        return parseInvokeCommand(commandNode, testId, specTestFile);

      case "register":
        return parseRegisterCommand(commandNode, testId, specTestFile);

      default:
        LOGGER.fine("Skipping unsupported command type: " + commandType);
        return null;
    }
  }

  private WebAssemblyTestCase parseModuleCommand(
      final JsonNode commandNode, final String testId, final Path specTestFile) {
    return WebAssemblyTestCase.builder()
        .testId(testId)
        .testName("module_" + testId.substring(testId.lastIndexOf('_') + 1))
        .category(TestCategory.SPEC_CORE)
        .testFilePath(specTestFile)
        .description("WebAssembly module instantiation test")
        .expected(TestExpectedResult.PASS)
        .tags(List.of("spec", "module", "instantiation"))
        .complexity(TestComplexity.SIMPLE)
        .build();
  }

  private WebAssemblyTestCase parseAssertReturnCommand(
      final JsonNode commandNode, final String testId, final Path specTestFile) {
    return WebAssemblyTestCase.builder()
        .testId(testId)
        .testName("assert_return_" + testId.substring(testId.lastIndexOf('_') + 1))
        .category(TestCategory.SPEC_CORE)
        .testFilePath(specTestFile)
        .description("Function invocation should return expected value")
        .expected(TestExpectedResult.PASS)
        .tags(List.of("spec", "assert_return", "function_call"))
        .complexity(TestComplexity.SIMPLE)
        .build();
  }

  private WebAssemblyTestCase parseAssertTrapCommand(
      final JsonNode commandNode, final String testId, final Path specTestFile) {
    String trapMessage = "unknown trap";
    if (commandNode.has("text")) {
      trapMessage = commandNode.get("text").asText();
    }

    return WebAssemblyTestCase.builder()
        .testId(testId)
        .testName("assert_trap_" + testId.substring(testId.lastIndexOf('_') + 1))
        .category(TestCategory.SPEC_CORE)
        .testFilePath(specTestFile)
        .description("Function invocation should trap: " + trapMessage)
        .expected(TestExpectedResult.TRAP)
        .tags(List.of("spec", "assert_trap", "trap", "negative"))
        .complexity(TestComplexity.SIMPLE)
        .build();
  }

  private WebAssemblyTestCase parseAssertInvalidCommand(
      final JsonNode commandNode, final String testId, final Path specTestFile) {
    String errorMessage = "invalid module";
    if (commandNode.has("text")) {
      errorMessage = commandNode.get("text").asText();
    }

    return WebAssemblyTestCase.builder()
        .testId(testId)
        .testName("assert_invalid_" + testId.substring(testId.lastIndexOf('_') + 1))
        .category(TestCategory.SPEC_CORE)
        .testFilePath(specTestFile)
        .description("Module should be invalid: " + errorMessage)
        .expected(TestExpectedResult.FAIL)
        .tags(List.of("spec", "assert_invalid", "validation", "negative"))
        .complexity(TestComplexity.SIMPLE)
        .build();
  }

  private WebAssemblyTestCase parseAssertMalformedCommand(
      final JsonNode commandNode, final String testId, final Path specTestFile) {
    String errorMessage = "malformed module";
    if (commandNode.has("text")) {
      errorMessage = commandNode.get("text").asText();
    }

    return WebAssemblyTestCase.builder()
        .testId(testId)
        .testName("assert_malformed_" + testId.substring(testId.lastIndexOf('_') + 1))
        .category(TestCategory.SPEC_CORE)
        .testFilePath(specTestFile)
        .description("Module should be malformed: " + errorMessage)
        .expected(TestExpectedResult.FAIL)
        .tags(List.of("spec", "assert_malformed", "parsing", "negative"))
        .complexity(TestComplexity.SIMPLE)
        .build();
  }

  private WebAssemblyTestCase parseAssertUninstantiableCommand(
      final JsonNode commandNode, final String testId, final Path specTestFile) {
    String errorMessage = "uninstantiable module";
    if (commandNode.has("text")) {
      errorMessage = commandNode.get("text").asText();
    }

    return WebAssemblyTestCase.builder()
        .testId(testId)
        .testName("assert_uninstantiable_" + testId.substring(testId.lastIndexOf('_') + 1))
        .category(TestCategory.SPEC_CORE)
        .testFilePath(specTestFile)
        .description("Module should be uninstantiable: " + errorMessage)
        .expected(TestExpectedResult.FAIL)
        .tags(List.of("spec", "assert_uninstantiable", "instantiation", "negative"))
        .complexity(TestComplexity.SIMPLE)
        .build();
  }

  private WebAssemblyTestCase parseInvokeCommand(
      final JsonNode commandNode, final String testId, final Path specTestFile) {
    String functionName = "unknown";
    if (commandNode.has("field")) {
      functionName = commandNode.get("field").asText();
    }

    return WebAssemblyTestCase.builder()
        .testId(testId)
        .testName("invoke_" + functionName + "_" + testId.substring(testId.lastIndexOf('_') + 1))
        .category(TestCategory.SPEC_CORE)
        .testFilePath(specTestFile)
        .description("Invoke function: " + functionName)
        .expected(TestExpectedResult.PASS)
        .tags(List.of("spec", "invoke", "function_call"))
        .complexity(TestComplexity.SIMPLE)
        .build();
  }

  private WebAssemblyTestCase parseRegisterCommand(
      final JsonNode commandNode, final String testId, final Path specTestFile) {
    String moduleName = "unknown";
    if (commandNode.has("name")) {
      moduleName = commandNode.get("name").asText();
    }

    return WebAssemblyTestCase.builder()
        .testId(testId)
        .testName("register_" + moduleName + "_" + testId.substring(testId.lastIndexOf('_') + 1))
        .category(TestCategory.SPEC_CORE)
        .testFilePath(specTestFile)
        .description("Register module: " + moduleName)
        .expected(TestExpectedResult.PASS)
        .tags(List.of("spec", "register", "module"))
        .complexity(TestComplexity.SIMPLE)
        .build();
  }

  private String generateBaseTestId(final Path specTestFile) {
    final String fileName = specTestFile.getFileName().toString();
    return fileName.replaceAll("\\.json$", "").replaceAll("[^a-zA-Z0-9_]", "_");
  }
}
