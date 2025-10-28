package ai.tegmentum.wasmtime4j.comparison.codegen;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Code generator that converts WAST test files into Java comparison tests.
 *
 * <p>This tool parses existing generated test files that have WAST content embedded as comments and
 * WAT strings, then generates proper Java test methods using WastTestRunner and JavaPoet for
 * type-safe code generation.
 */
public final class WastTestGenerator {

  private static int moduleCount = 0;
  private static String testClassName = "";
  private static String testResourceDir = "";
  private static final List<WatFileToCreate> watFilesToCreate = new ArrayList<>();
  private static final CodeBlock.Builder methodBody = CodeBlock.builder();

  /**
   * Main entry point for WAST test generator.
   *
   * @param args command line arguments (WAST file path and optional output directory)
   * @throws IOException if file operations fail
   */
  public static void main(final String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Usage: WastTestGenerator <wast-file-path> [output-dir]");
      System.err.println(
          "Example: WastTestGenerator src/test/resources/wasm/wasmtime-tests/table_grow.wast");
      System.err.println(
          "  or:    WastTestGenerator src/test/resources/wasm/wasmtime-tests/table_grow.wast"
              + " wasmtime4j-comparison-tests/src/test/java");
      System.exit(1);
    }

    final Path wastFile = Paths.get(args[0]);
    if (!Files.exists(wastFile)) {
      System.err.println("WAST file not found: " + wastFile);
      System.exit(1);
    }

    // Determine output directory
    final Path outputDir;
    if (args.length >= 2) {
      outputDir = Paths.get(args[1]);
    } else {
      // Default to wasmtime4j-comparison-tests/src/test/java
      final Path projectRoot = findProjectRoot(wastFile);
      outputDir = projectRoot.resolve("wasmtime4j-comparison-tests/src/test/java");
    }

    System.out.println("Generating test code for: " + wastFile);
    System.out.println("Output directory: " + outputDir);

    final String wastContent = Files.readString(wastFile);

    // Extract test name from WAST filename (e.g., "table_grow_with_funcref.wast" ->
    // "TableGrowWithFuncrefTest")
    final String wastFileName = wastFile.getFileName().toString().replace(".wast", "");
    testClassName = toCamelCase(wastFileName) + "Test";

    // Determine package based on category (wasmtime-tests subdirectory structure)
    final String packageName = determinePackage(wastFile);
    testResourceDir = packageName;

    // Generate the Java file using JavaPoet
    final JavaFile javaFile = generateTestFileFromWast(wastContent, packageName, wastFileName);

    // Write the Java file
    javaFile.writeTo(outputDir);
    System.out.println(
        "Wrote Java file: "
            + outputDir.resolve(packageName.replace('.', '/')).resolve(testClassName + ".java"));

    // Write all WAT resource files
    writeWatFilesFromWast(wastFile, outputDir);

    System.out.println("\nGeneration complete!");
    System.out.println("Generated " + watFilesToCreate.size() + " WAT resource files");
  }

  /**
   * Converts snake_case or kebab-case to CamelCase.
   *
   * @param input the input string
   * @return CamelCase version
   */
  private static String toCamelCase(final String input) {
    final StringBuilder result = new StringBuilder();
    boolean capitalizeNext = true;
    for (final char c : input.toCharArray()) {
      if (c == '_' || c == '-') {
        capitalizeNext = true;
      } else if (capitalizeNext) {
        result.append(Character.toUpperCase(c));
        capitalizeNext = false;
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  /**
   * Determines the package name based on the WAST file location.
   *
   * @param wastFile the WAST file path
   * @return the package name
   */
  private static String determinePackage(final Path wastFile) {
    final String path = wastFile.toString();
    if (path.contains("misc_testsuite") || path.contains("wasmtime-tests")) {
      return "ai.tegmentum.wasmtime4j.comparison.generated.wasmtime";
    }
    return "ai.tegmentum.wasmtime4j.comparison.generated";
  }

  /**
   * Convert i64 literal to Java long literal with proper handling of unsigned values.
   *
   * <p>WebAssembly i64 values are unsigned, but Java long is signed. Values that exceed
   * Long.MAX_VALUE need to be converted to their two's complement signed representation.
   *
   * @param value the i64 value as a string (may be decimal or hex)
   * @return formatted Java long literal with 'L' suffix
   */
  private static String convertI64Literal(final String value) {
    try {
      // Try to parse as long first
      final long longValue = Long.parseLong(value);
      return longValue + "L";
    } catch (final NumberFormatException e) {
      // Value exceeds Long.MAX_VALUE, treat as unsigned and convert to signed
      try {
        final java.math.BigInteger bigInt = new java.math.BigInteger(value);
        // Convert unsigned to signed using two's complement
        // If value > Long.MAX_VALUE, subtract 2^64 to get signed representation
        if (bigInt.compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
          final java.math.BigInteger maxUnsigned = java.math.BigInteger.ONE.shiftLeft(64);
          final java.math.BigInteger signed = bigInt.subtract(maxUnsigned);
          return signed.toString() + "L";
        }
        return bigInt.toString() + "L";
      } catch (final Exception ex) {
        // Fallback - return original value with L suffix
        return value + "L";
      }
    }
  }

  /**
   * Ensure f64 value has a decimal point for Java double literal.
   *
   * <p>Java requires double literals to have a decimal point or exponential notation to distinguish
   * them from integer literals. This is especially important for large values that exceed
   * Integer.MAX_VALUE.
   *
   * @param value the f64 value as a string
   * @return formatted Java double literal
   */
  private static String ensureDecimalPoint(final String value) {
    // If already has decimal point or exponential notation, return as-is
    if (value.contains(".") || value.contains("e") || value.contains("E")) {
      return value;
    }
    // Add .0 to make it a valid double literal
    return value + ".0";
  }

  /**
   * Convert f32 literal to Java float literal.
   *
   * <p>WebAssembly f32 literals can be:
   *
   * <ul>
   *   <li>Decimal: 1.5, 3.14
   *   <li>Hex integer (reinterpreted as float bits): 0xf32
   *   <li>Hex float: 0x1.8p+1
   *   <li>Special values: nan, inf, -inf
   * </ul>
   *
   * <p>Hex integer literals like 0xf32 are treated as integer bit patterns and converted to their
   * decimal representation.
   *
   * @param value the f32 value as a string
   * @return formatted Java float literal with 'f' suffix
   */
  private static String convertF32Literal(final String value) {
    // Handle special values
    if (value.equals("nan")) {
      return "Float.NaN";
    }
    if (value.equals("inf")) {
      return "Float.POSITIVE_INFINITY";
    }
    if (value.equals("-inf")) {
      return "Float.NEGATIVE_INFINITY";
    }

    // Handle hex integer literals (0xXXX without 'p' exponent)
    if (value.startsWith("0x") && !value.contains("p") && !value.contains("P")) {
      try {
        // Parse as hex integer and convert to decimal
        final long bits = Long.parseLong(value.substring(2), 16);
        return bits + ".0f";
      } catch (final NumberFormatException e) {
        // If parsing fails, fall through to default handling
      }
    }

    // For regular decimal or hex float notation, append 'f' if not present
    if (value.endsWith("f") || value.endsWith("F")) {
      return value;
    }
    return value + "f";
  }

  /**
   * Convert f64 literal to Java double literal.
   *
   * <p>WebAssembly f64 literals can be:
   *
   * <ul>
   *   <li>Decimal: 1.5, 3.14
   *   <li>Hex integer (reinterpreted as double bits): 0xf64
   *   <li>Hex float: 0x1.8p+1
   *   <li>Special values: nan, inf, -inf
   * </ul>
   *
   * <p>Hex integer literals like 0xf64 are treated as integer bit patterns and converted to their
   * decimal representation.
   *
   * @param value the f64 value as a string
   * @return formatted Java double literal
   */
  private static String convertF64Literal(final String value) {
    // Handle special values
    if (value.equals("nan")) {
      return "Double.NaN";
    }
    if (value.equals("inf")) {
      return "Double.POSITIVE_INFINITY";
    }
    if (value.equals("-inf")) {
      return "Double.NEGATIVE_INFINITY";
    }

    // Handle hex integer literals (0xXXX without 'p' exponent)
    if (value.startsWith("0x") && !value.contains("p") && !value.contains("P")) {
      try {
        // Parse as hex integer and convert to decimal
        final long bits = Long.parseLong(value.substring(2), 16);
        return ensureDecimalPoint(String.valueOf(bits));
      } catch (final NumberFormatException e) {
        // If parsing fails, fall through to default handling
      }
    }

    // For regular decimal values, ensure decimal point
    return ensureDecimalPoint(value);
  }

  private static void writeWatFiles(final Path testFile) throws IOException {
    final Path projectRoot = findProjectRoot(testFile);
    final Path resourcesDir = projectRoot.resolve("src/test/resources");

    for (final WatFileToCreate watFile : watFilesToCreate) {
      final Path watFilePath = resourcesDir.resolve(watFile.path);
      Files.createDirectories(watFilePath.getParent());
      Files.writeString(watFilePath, watFile.content);
      System.out.println("Wrote WAT file: " + watFilePath);
    }
  }

  private static Path findProjectRoot(final Path testFile) {
    // Find the root by looking for wasmtime4j-comparison-tests subdirectory
    Path current = testFile.getParent();
    while (current != null) {
      if (Files.exists(current.resolve("wasmtime4j-comparison-tests"))
          && Files.isDirectory(current.resolve("wasmtime4j-comparison-tests"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new RuntimeException("Could not find project root (with wasmtime4j-comparison-tests)");
  }

  private static final class WatFileToCreate {
    final String path;
    final String content;

    WatFileToCreate(final String path, final String content) {
      this.path = path;
      this.content = content;
    }
  }

  private static JavaFile generateTestFile(final String content) {
    // Extract package name
    final Pattern packagePattern = Pattern.compile("package\\s+([^;]+);");
    final Matcher packageMatcher = packagePattern.matcher(content);
    final String packageName =
        packageMatcher.find() ? packageMatcher.group(1) : "ai.tegmentum.wasmtime4j.comparison";

    // Extract WAT content
    final Pattern watPattern =
        Pattern.compile("(?:final\\s+)?String\\s+wat\\s*=\\s*\"\"\"(.*?)\"\"\"", Pattern.DOTALL);
    final Matcher watMatcher = watPattern.matcher(content);
    if (!watMatcher.find()) {
      throw new RuntimeException("Could not find WAT content in test file");
    }
    final String watContent = watMatcher.group(1).trim();

    // Extract display name
    final Pattern displayNamePattern = Pattern.compile("@DisplayName\\(\"([^\"]+)\"\\)");
    final Matcher displayNameMatcher = displayNamePattern.matcher(content);
    final String displayName =
        displayNameMatcher.find() ? displayNameMatcher.group(1) : "Generated Test";

    // Extract method name
    final Pattern methodPattern = Pattern.compile("public void (\\w+)\\(");
    final Matcher methodMatcher = methodPattern.matcher(content);
    final String methodName = methodMatcher.find() ? methodMatcher.group(1) : "testGenerated";

    // Extract class javadoc
    final Pattern javadocPattern =
        Pattern.compile("/\\*\\*(.*?)\\*/\\s*(?:@DisplayName)?", Pattern.DOTALL);
    final Matcher javadocMatcher = javadocPattern.matcher(content);
    String javadoc = "";
    if (javadocMatcher.find()) {
      javadoc = javadocMatcher.group(1).trim();
    }

    // Reset module count and method body
    moduleCount = 0;
    watFilesToCreate.clear();
    methodBody.clear();

    // Build test method body
    methodBody.addStatement("setRuntime(runtime)");
    methodBody.add("\n");
    methodBody.beginControlFlow(
        "try (final $T runner = new $T())", WastTestRunner.class, WastTestRunner.class);
    methodBody.add("\n");

    // Parse and generate test code
    parseAndGenerate(watContent);

    methodBody.endControlFlow();

    // Create loadResource helper method
    final MethodSpec loadResourceMethod =
        MethodSpec.methodBuilder("loadResource")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(String.class)
            .addParameter(String.class, "path", Modifier.FINAL)
            .addException(IOException.class)
            .beginControlFlow(
                "try (final $T is = $L.class.getResourceAsStream(path))",
                InputStream.class,
                testClassName)
            .beginControlFlow("if (is == null)")
            .addStatement("throw new $T($S + path)", IOException.class, "Resource not found: ")
            .endControlFlow()
            .addStatement(
                "return new $T(is.readAllBytes(), $T.UTF_8)", String.class, StandardCharsets.class)
            .endControlFlow()
            .build();

    // Create test method
    final MethodSpec testMethod =
        MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ParameterizedTest.class)
            .addAnnotation(
                AnnotationSpec.builder(ArgumentsSource.class)
                    .addMember("value", "$T.class", ClassName.bestGuess("RuntimeProvider"))
                    .build())
            .addAnnotation(
                AnnotationSpec.builder(DisplayName.class)
                    .addMember("value", "$S", displayName)
                    .build())
            .addParameter(RuntimeType.class, "runtime", Modifier.FINAL)
            .addException(Exception.class)
            .addCode(methodBody.build())
            .build();

    // Create class
    final TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(testClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(DualRuntimeTest.class)
            .addMethod(loadResourceMethod)
            .addMethod(testMethod);

    // Add javadoc if present
    if (!javadoc.isEmpty()) {
      classBuilder.addJavadoc(javadoc + "\n");
    }

    final TypeSpec testClass = classBuilder.build();

    // Create Java file
    return JavaFile.builder(packageName, testClass).indent("  ").build();
  }

  /**
   * Generates a Java test file directly from WAST content.
   *
   * @param wastContent the WAST file content
   * @param packageName the package name for the generated test
   * @param testName the test name (from filename)
   * @return the generated JavaFile
   */
  private static JavaFile generateTestFileFromWast(
      final String wastContent, final String packageName, final String testName) {
    // Reset module count and method body
    moduleCount = 0;
    watFilesToCreate.clear();
    methodBody.clear();

    // Create method name from test name
    final String methodName = "test" + toCamelCase(testName);
    final String displayName = testName.replace('_', ' ').replace('-', ' ');

    // Build test method body
    methodBody.addStatement("setRuntime(runtime)");
    methodBody.add("\n");
    methodBody.beginControlFlow(
        "try (final $T runner = new $T())", WastTestRunner.class, WastTestRunner.class);
    methodBody.add("\n");

    // Parse and generate test code from WAST content
    parseAndGenerate(wastContent);

    methodBody.endControlFlow();

    // Create loadResource helper method
    final MethodSpec loadResourceMethod =
        MethodSpec.methodBuilder("loadResource")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(String.class)
            .addParameter(String.class, "path", Modifier.FINAL)
            .addException(IOException.class)
            .beginControlFlow(
                "try (final $T is = $L.class.getResourceAsStream(path))",
                InputStream.class,
                testClassName)
            .beginControlFlow("if (is == null)")
            .addStatement("throw new $T($S + path)", IOException.class, "Resource not found: ")
            .endControlFlow()
            .addStatement(
                "return new $T(is.readAllBytes(), $T.UTF_8)", String.class, StandardCharsets.class)
            .endControlFlow()
            .build();

    // Create test method
    final MethodSpec testMethod =
        MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ParameterizedTest.class)
            .addAnnotation(
                AnnotationSpec.builder(ArgumentsSource.class)
                    .addMember("value", "$T.class", ClassName.bestGuess("RuntimeProvider"))
                    .build())
            .addAnnotation(
                AnnotationSpec.builder(DisplayName.class)
                    .addMember("value", "$S", displayName)
                    .build())
            .addParameter(RuntimeType.class, "runtime", Modifier.FINAL)
            .addException(Exception.class)
            .addCode(methodBody.build())
            .build();

    // Create class with javadoc
    final String javadoc =
        String.format(
            "Generated test from WAST file: %s.wast\n\n"
                + "<p>This test validates that wasmtime4j produces the same results as the"
                + " upstream\n"
                + "Wasmtime implementation for this test case.",
            testName);

    final TypeSpec testClass =
        TypeSpec.classBuilder(testClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(DualRuntimeTest.class)
            .addJavadoc(javadoc)
            .addMethod(loadResourceMethod)
            .addMethod(testMethod)
            .build();

    return JavaFile.builder(packageName, testClass).indent("  ").build();
  }

  /**
   * Writes WAT resource files for WAST-generated tests.
   *
   * @param wastFile the original WAST file
   * @param outputDir the output directory for Java files
   * @throws IOException if writing fails
   */
  private static void writeWatFilesFromWast(final Path wastFile, final Path outputDir)
      throws IOException {
    // Convert outputDir from .../src/test/java to .../src/test/resources
    final Path resourcesDir = outputDir.getParent().resolve("resources");

    for (final WatFileToCreate watFile : watFilesToCreate) {
      final Path watFilePath = resourcesDir.resolve(watFile.path);
      Files.createDirectories(watFilePath.getParent());
      Files.writeString(watFilePath, watFile.content);
      System.out.println("  Wrote WAT file: " + watFilePath);
    }
  }

  private static void parseAndGenerate(final String wastContent) {
    final List<String> tokens = tokenize(wastContent);
    int i = 0;

    while (i < tokens.size()) {
      final String token = tokens.get(i);

      if (token.equals("(") && i + 1 < tokens.size()) {
        final String next = tokens.get(i + 1);

        if (next.equals("module")) {
          final ParseResult result = parseModule(tokens, i);
          generateModuleCode(result.content);
          i = result.endIndex;
        } else if (next.equals("assert_return")) {
          final ParseResult result = parseDirective(tokens, i);
          generateAssertReturn(result.content);
          i = result.endIndex;
        } else if (next.equals("assert_trap")) {
          final ParseResult result = parseDirective(tokens, i);
          generateAssertTrap(result.content);
          i = result.endIndex;
        } else if (next.equals("register")) {
          final ParseResult result = parseDirective(tokens, i);
          generateRegister(result.content);
          i = result.endIndex;
        } else if (next.equals("assert_invalid")
            || next.equals("assert_malformed")
            || next.equals("assert_unlinkable")) {
          final ParseResult result = parseDirective(tokens, i);
          generateAssertInvalid(result.content, next);
          i = result.endIndex;
        } else if (next.equals("invoke")) {
          final ParseResult result = parseDirective(tokens, i);
          generateInvoke(result.content);
          i = result.endIndex;
        } else {
          i++;
        }
      } else {
        i++;
      }
    }
  }

  private static List<String> tokenize(final String content) {
    final List<String> tokens = new ArrayList<>();
    final StringBuilder current = new StringBuilder();
    boolean inString = false;
    boolean inLineComment = false;
    boolean inBlockComment = false;

    for (int i = 0; i < content.length(); i++) {
      final char c = content.charAt(i);

      // Handle block comments: (;...;)
      if (!inString
          && !inLineComment
          && c == '('
          && i + 1 < content.length()
          && content.charAt(i + 1) == ';') {
        inBlockComment = true;
        i++; // Skip the ;
        continue;
      }

      if (inBlockComment) {
        if (c == ';' && i + 1 < content.length() && content.charAt(i + 1) == ')') {
          inBlockComment = false;
          i++; // Skip the )
        }
        continue;
      }

      // Handle line comments: ;;...
      if (!inString && c == ';' && i + 1 < content.length() && content.charAt(i + 1) == ';') {
        inLineComment = true;
        i++; // Skip second ;
        continue;
      }

      if (inLineComment) {
        if (c == '\n') {
          inLineComment = false;
        }
        continue;
      }

      // Handle strings
      if (c == '"') {
        if (inString) {
          current.append(c);
          tokens.add(current.toString());
          current.setLength(0);
          inString = false;
        } else {
          if (current.length() > 0) {
            tokens.add(current.toString());
            current.setLength(0);
          }
          inString = true;
          current.append(c);
        }
        continue;
      }

      if (inString) {
        current.append(c);
        continue;
      }

      // Handle parentheses
      if (c == '(' || c == ')') {
        if (current.length() > 0) {
          tokens.add(current.toString());
          current.setLength(0);
        }
        tokens.add(String.valueOf(c));
        continue;
      }

      // Handle whitespace
      if (Character.isWhitespace(c)) {
        if (current.length() > 0) {
          tokens.add(current.toString());
          current.setLength(0);
        }
        continue;
      }

      current.append(c);
    }

    if (current.length() > 0) {
      tokens.add(current.toString());
    }

    return tokens;
  }

  private static ParseResult parseModule(final List<String> tokens, final int startIndex) {
    final StringBuilder module = new StringBuilder();
    int depth = 0;
    int i = startIndex;

    while (i < tokens.size()) {
      final String token = tokens.get(i);

      if (token.equals("(")) {
        depth++;
      } else if (token.equals(")")) {
        depth--;
        if (depth == 0) {
          module.append(")");
          return new ParseResult(module.toString(), i + 1);
        }
      }

      module.append(token);
      if (i + 1 < tokens.size() && !tokens.get(i + 1).equals(")")) {
        module.append(" ");
      }
      i++;
    }

    return new ParseResult(module.toString(), i);
  }

  private static ParseResult parseDirective(final List<String> tokens, final int startIndex) {
    final StringBuilder directive = new StringBuilder();
    int depth = 0;
    int i = startIndex;

    while (i < tokens.size()) {
      final String token = tokens.get(i);

      if (token.equals("(")) {
        depth++;
      } else if (token.equals(")")) {
        depth--;
        if (depth == 0) {
          directive.append(")");
          return new ParseResult(directive.toString(), i + 1);
        }
      }

      directive.append(token);
      if (i + 1 < tokens.size() && !tokens.get(i + 1).equals(")")) {
        directive.append(" ");
      }
      i++;
    }

    return new ParseResult(directive.toString(), i);
  }

  private static void generateModuleCode(final String moduleWat) {
    moduleCount++;
    final String resourceName = String.format("%s_module%d.wat", testClassName, moduleCount);
    final String resourcePath = testResourceDir.replace(".", "/") + "/" + resourceName;

    methodBody.add("// Compile and instantiate module $L\n", moduleCount);
    methodBody.add("// WAT file: $L\n", resourcePath);
    methodBody.addStatement(
        "final $T moduleWat$L = loadResource($S)", String.class, moduleCount, "/" + resourcePath);
    methodBody.addStatement("runner.compileAndInstantiate(moduleWat$L)", moduleCount);
    methodBody.add("\n");

    // Add the WAT content to the list of files to create
    // Comments are already stripped during tokenization
    watFilesToCreate.add(new WatFileToCreate(resourcePath, moduleWat));
  }

  private static String formatWat(final String wat) {
    final StringBuilder formatted = new StringBuilder();
    int indentLevel = 0;
    final String[] tokens = wat.split("\\s+");
    int lineLength = 0;

    for (int i = 0; i < tokens.length; i++) {
      final String token = tokens[i];

      // Decrease indent before closing paren
      if (token.equals(")")) {
        indentLevel = Math.max(0, indentLevel - 1);
      }

      // Start new line if needed
      if (lineLength == 0) {
        formatted.append("  ".repeat(indentLevel));
      }

      formatted.append(token);
      lineLength += token.length();

      // Increase indent after opening paren
      if (token.equals("(")) {
        indentLevel++;
      }

      // Add newline after closing paren (except for simple cases)
      if (token.equals(")") && i + 1 < tokens.length) {
        formatted.append("\n");
        lineLength = 0;
      } else if (i + 1 < tokens.length) {
        formatted.append(" ");
        lineLength++;
      }

      // Force newline for long lines or after certain keywords
      if (lineLength > 80
          || (i + 1 < tokens.length
              && (token.equals("(module")
                  || token.equals("(func")
                  || token.equals("(table")
                  || token.equals("(memory")
                  || token.equals("(global")
                  || token.equals("(type")))) {
        if (i + 1 < tokens.length && !tokens[i + 1].equals(")")) {
          formatted.append("\n");
          lineLength = 0;
        }
      }
    }

    return formatted.toString();
  }

  private static void generateAssertReturn(final String directive) {
    // Parse: (assert_return (invoke "name" args...) results...)
    final Pattern namePattern = Pattern.compile("invoke\\s+\"([^\"]+)\"");
    final Matcher nameMatcher = namePattern.matcher(directive);

    if (!nameMatcher.find()) {
      methodBody.add("// $L\n", directive);
      methodBody.add("// TODO: Parse assert_return - no function name found\n");
      methodBody.add("\n");
      return;
    }

    final String functionName = nameMatcher.group(1);

    // Extract arguments and expected results
    final int invokeStart = directive.indexOf("( invoke");
    final int functionNameEnd = nameMatcher.end();

    // Find matching closing paren for invoke
    int depth = 0;
    int invokeEnd = -1;
    for (int i = invokeStart; i < directive.length(); i++) {
      if (directive.charAt(i) == '(') {
        depth++;
      }
      if (directive.charAt(i) == ')') {
        depth--;
        if (depth == 0) {
          invokeEnd = i;
          break;
        }
      }
    }

    if (invokeEnd == -1) {
      methodBody.add("// $L\n", directive);
      methodBody.add("// TODO: Parse assert_return - malformed invoke\n");
      methodBody.add("\n");
      return;
    }

    final String argsSection =
        directive.substring(functionNameEnd, invokeEnd).trim().replaceAll("\\)\\s*$", "").trim();
    final String afterInvoke = directive.substring(invokeEnd + 1).trim();
    final String expectedResults = afterInvoke.replaceAll("\\)\\s*$", "").trim();

    final List<String> args = parseWasmValues(argsSection);
    final List<String> expected = parseWasmValues(expectedResults);

    methodBody.add("// $L\n", directive);

    if (expected.isEmpty()) {
      // No expected results - just invoke
      if (args.isEmpty()) {
        methodBody.addStatement("runner.invoke($S)", functionName);
      } else {
        methodBody.add("runner.invoke($S", functionName);
        for (final String arg : args) {
          methodBody.add(", $L", arg);
        }
        methodBody.add(");\n");
      }
    } else {
      // Has expected results - use assertReturn
      methodBody.add("runner.assertReturn($S, new $T[] { ", functionName, WasmValue.class);
      for (int i = 0; i < expected.size(); i++) {
        if (i > 0) {
          methodBody.add(", ");
        }
        methodBody.add("$L", expected.get(i));
      }
      methodBody.add(" }");
      for (final String arg : args) {
        methodBody.add(", $L", arg);
      }
      methodBody.add(");\n");
    }

    methodBody.add("\n");
  }

  private static void generateAssertTrap(final String directive) {
    // Parse: (assert_trap (invoke "name" args...) "error message")
    final Pattern namePattern = Pattern.compile("invoke\\s+\"([^\"]+)\"");
    final Matcher nameMatcher = namePattern.matcher(directive);

    if (!nameMatcher.find()) {
      methodBody.add("// $L\n", directive);
      methodBody.add("// TODO: Parse assert_trap - no function name found\n");
      methodBody.add("\n");
      return;
    }

    final String functionName = nameMatcher.group(1);
    final int functionNameEnd = nameMatcher.end();

    // Find matching closing paren for invoke
    final int invokeStart = directive.indexOf("( invoke");
    int depth = 0;
    int invokeEnd = -1;
    for (int i = invokeStart; i < directive.length(); i++) {
      if (directive.charAt(i) == '(') {
        depth++;
      }
      if (directive.charAt(i) == ')') {
        depth--;
        if (depth == 0) {
          invokeEnd = i;
          break;
        }
      }
    }

    if (invokeEnd == -1) {
      methodBody.add("// $L\n", directive);
      methodBody.add("// TODO: Parse assert_trap - malformed invoke\n");
      methodBody.add("\n");
      return;
    }

    final String argsSection =
        directive.substring(functionNameEnd, invokeEnd).trim().replaceAll("\\)\\s*$", "").trim();

    // Parse error message
    final Pattern msgPattern = Pattern.compile("\"([^\"]+)\"");
    final Matcher msgMatcher = msgPattern.matcher(directive.substring(invokeEnd));
    final String errorMsg = msgMatcher.find() ? msgMatcher.group(1) : null;

    final List<String> args = parseWasmValues(argsSection);

    methodBody.add("// $L\n", directive);

    if (args.isEmpty()) {
      if (errorMsg != null) {
        methodBody.addStatement("runner.assertTrap($S, $S)", functionName, errorMsg);
      } else {
        methodBody.addStatement("runner.assertTrap($S, null)", functionName);
      }
    } else {
      methodBody.add("runner.assertTrap($S, ", functionName);
      if (errorMsg != null) {
        methodBody.add("$S", errorMsg);
      } else {
        methodBody.add("null");
      }
      for (final String arg : args) {
        methodBody.add(", $L", arg);
      }
      methodBody.add(");\n");
    }

    methodBody.add("\n");
  }

  private static void generateRegister(final String directive) {
    // Parse: (register "name") or (register "name" $module)
    // Format: ( register "module_name" )
    methodBody.add("// $L\n", directive);

    // Extract module name from quotes
    final int firstQuote = directive.indexOf('"');
    final int secondQuote = directive.indexOf('"', firstQuote + 1);

    if (firstQuote == -1 || secondQuote == -1) {
      methodBody.add("// TODO: Parse register - module name not found\n");
      methodBody.add("\n");
      return;
    }

    final String moduleName = directive.substring(firstQuote + 1, secondQuote);

    methodBody.addStatement("runner.registerModule($S)", moduleName);
    methodBody.add("\n");
  }

  private static void generateInvoke(final String directive) {
    // Parse: (invoke "name" args...)
    final Pattern namePattern = Pattern.compile("invoke\\s+\"([^\"]+)\"");
    final Matcher nameMatcher = namePattern.matcher(directive);

    if (!nameMatcher.find()) {
      methodBody.add("// $L\n", directive);
      methodBody.add("// TODO: Parse invoke - no function name found\n");
      methodBody.add("\n");
      return;
    }

    final String functionName = nameMatcher.group(1);
    final int functionNameEnd = nameMatcher.end();

    // Find matching closing paren for invoke
    int depth = 0;
    int invokeEnd = -1;
    for (int i = 0; i < directive.length(); i++) {
      if (directive.charAt(i) == '(') {
        depth++;
      }
      if (directive.charAt(i) == ')') {
        depth--;
        if (depth == 0) {
          invokeEnd = i;
          break;
        }
      }
    }

    if (invokeEnd == -1) {
      methodBody.add("// $L\n", directive);
      methodBody.add("// TODO: Parse invoke - malformed directive\n");
      methodBody.add("\n");
      return;
    }

    final String argsSection =
        directive.substring(functionNameEnd, invokeEnd).trim().replaceAll("\\)\\s*$", "").trim();
    final List<String> args = parseWasmValues(argsSection);

    methodBody.add("// $L\n", directive);

    // Generate standalone invoke call
    if (args.isEmpty()) {
      methodBody.addStatement("runner.invoke($S)", functionName);
    } else {
      methodBody.add("runner.invoke($S", functionName);
      for (final String arg : args) {
        methodBody.add(", $L", arg);
      }
      methodBody.add(");\n");
    }

    methodBody.add("\n");
  }

  private static void generateAssertInvalid(final String directive, final String assertType) {
    // Parse: (assert_invalid (module ...) "error message")
    final int moduleStart = directive.indexOf("( module");
    if (moduleStart == -1) {
      methodBody.add("// $L\n", directive);
      methodBody.add("// TODO: Parse $L - no module found\n", assertType);
      methodBody.add("\n");
      return;
    }

    // Find the matching closing paren for module
    int depth = 0;
    int moduleEnd = -1;
    for (int i = moduleStart; i < directive.length(); i++) {
      if (directive.charAt(i) == '(') {
        depth++;
      }
      if (directive.charAt(i) == ')') {
        depth--;
        if (depth == 0) {
          moduleEnd = i + 1;
          break;
        }
      }
    }

    if (moduleEnd == -1) {
      methodBody.add("// $L\n", directive);
      methodBody.add("// TODO: Parse $L - malformed module\n", assertType);
      methodBody.add("\n");
      return;
    }

    final String moduleWat = directive.substring(moduleStart, moduleEnd).trim();

    // Extract error message if present
    final Pattern msgPattern = Pattern.compile("\"([^\"]+)\"\\s*\\)\\s*$");
    final Matcher msgMatcher = msgPattern.matcher(directive);
    final String errorMsg = msgMatcher.find() ? msgMatcher.group(1) : null;

    moduleCount++;
    final String resourceName = String.format("%s_invalid%d.wat", testClassName, moduleCount);
    final String resourcePath = testResourceDir.replace(".", "/") + "/" + resourceName;

    methodBody.add("// $L\n", directive);
    methodBody.add("// Invalid WAT file: $L\n", resourcePath);
    methodBody.addStatement(
        "final $T invalidWat$L = loadResource($S)", String.class, moduleCount, "/" + resourcePath);

    if (errorMsg != null) {
      methodBody.addStatement("runner.assertInvalid(invalidWat$L, $S)", moduleCount, errorMsg);
    } else {
      methodBody.addStatement("runner.assertInvalid(invalidWat$L, null)", moduleCount);
    }

    methodBody.add("\n");

    // Add the WAT content to the list of files to create
    // Use raw WAT without formatting to preserve exact syntax
    watFilesToCreate.add(new WatFileToCreate(resourcePath, moduleWat));
  }

  private static List<String> parseWasmValues(final String valuesStr) {
    final List<String> result = new ArrayList<>();
    if (valuesStr.isEmpty()) {
      return result;
    }

    final List<String> tokens = tokenize(valuesStr);
    int i = 0;

    while (i < tokens.size()) {
      if (tokens.get(i).equals("(") && i + 1 < tokens.size()) {
        final String type = tokens.get(i + 1);

        if (type.equals("i32.const")) {
          final String value = tokens.get(i + 2);
          result.add(String.format("WasmValue.i32(%s)", value));
          i += 4; // Skip ( i32.const value )
        } else if (type.equals("i64.const")) {
          final String value = tokens.get(i + 2);
          result.add(String.format("WasmValue.i64(%s)", convertI64Literal(value)));
          i += 4;
        } else if (type.equals("f32.const")) {
          final String value = tokens.get(i + 2);
          result.add(String.format("WasmValue.f32(%s)", convertF32Literal(value)));
          i += 4;
        } else if (type.equals("f64.const")) {
          final String value = tokens.get(i + 2);
          result.add(String.format("WasmValue.f64(%s)", convertF64Literal(value)));
          i += 4;
        } else if (type.equals("ref.null")) {
          result.add("WasmValue.externref(null)");
          i += 4; // Skip ( ref.null extern )
        } else if (type.equals("ref.extern")) {
          final String value = tokens.get(i + 2);
          result.add(String.format("WasmValue.externref(%sL)", value));
          i += 4;
        } else {
          i++;
        }
      } else {
        i++;
      }
    }

    return result;
  }

  private static final class ParseResult {
    final String content;
    final int endIndex;

    ParseResult(final String content, final int endIndex) {
      this.content = content;
      this.endIndex = endIndex;
    }
  }
}
