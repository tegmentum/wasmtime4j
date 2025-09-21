# Documentation Testing and Validation Framework

This guide describes the comprehensive testing framework for validating all code examples, documentation links, and ensuring the documentation stays current with the API.

## Table of Contents

1. [Overview](#overview)
2. [Code Example Testing](#code-example-testing)
3. [Link Validation](#link-validation)
4. [Documentation Sync Testing](#documentation-sync-testing)
5. [Integration with CI/CD](#integration-with-cicd)
6. [Writing Testable Documentation](#writing-testable-documentation)
7. [Automated Documentation Updates](#automated-documentation-updates)

## Overview

The documentation testing framework ensures that:
- All code examples in documentation compile and run correctly
- All links (internal and external) are valid
- Documentation examples match the current API
- Documentation is automatically updated when APIs change
- Examples are tested against multiple Java versions and platforms

## Code Example Testing

### Documentation Test Framework

```java
package ai.tegmentum.wasmtime4j.docs.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Framework for testing code examples in documentation.
 * This ensures all documented examples actually work.
 */
@ExtendWith(DocumentationTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class DocumentationTestBase {

    protected WasmRuntime runtime;
    protected Engine engine;
    protected DocumentationExampleRunner exampleRunner;

    @BeforeEach
    public void setupDocumentationTest() throws Exception {
        // Setup common test infrastructure
        this.runtime = WasmRuntimeFactory.create();
        this.engine = runtime.createEngine(createTestEngineConfig());
        this.exampleRunner = DocumentationExampleRunner.create(runtime, engine);
    }

    @AfterEach
    public void teardownDocumentationTest() throws Exception {
        if (runtime != null) {
            runtime.close();
        }
    }

    private EngineConfig createTestEngineConfig() {
        return new EngineConfig()
            .optimizationLevel(OptimizationLevel.NONE) // Fast compilation for tests
            .debugInfo(true)
            .parallelCompilation(false); // Deterministic behavior
    }

    /**
     * Run a code example from documentation and verify it works.
     */
    protected void runDocumentationExample(String exampleName, String javaCode) throws Exception {
        exampleRunner.compileAndRun(exampleName, javaCode);
    }

    /**
     * Verify that a code snippet compiles without running it.
     */
    protected void verifyCompilation(String snippetName, String javaCode) throws Exception {
        exampleRunner.verifyCompilation(snippetName, javaCode);
    }

    /**
     * Run an example and verify its output matches expected results.
     */
    protected void runAndVerifyOutput(String exampleName, String javaCode, String expectedOutput) throws Exception {
        String actualOutput = exampleRunner.runAndCaptureOutput(exampleName, javaCode);
        assertEquals(expectedOutput.trim(), actualOutput.trim());
    }
}
```

### Example Runner Implementation

```java
package ai.tegmentum.wasmtime4j.docs.test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class DocumentationExampleRunner {

    private final WasmRuntime runtime;
    private final Engine engine;
    private final Path tempDir;

    public static DocumentationExampleRunner create(WasmRuntime runtime, Engine engine) throws Exception {
        Path tempDir = Files.createTempDirectory("doc-examples");
        return new DocumentationExampleRunner(runtime, engine, tempDir);
    }

    private DocumentationExampleRunner(WasmRuntime runtime, Engine engine, Path tempDir) {
        this.runtime = runtime;
        this.engine = engine;
        this.tempDir = tempDir;
    }

    public void compileAndRun(String exampleName, String javaCode) throws Exception {
        // Create complete Java class
        String completeClass = wrapInClass(exampleName, javaCode);

        // Compile the code
        Class<?> compiledClass = compileJavaCode(exampleName, completeClass);

        // Run the main method
        Method mainMethod = compiledClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) new String[0]);
    }

    public void verifyCompilation(String snippetName, String javaCode) throws Exception {
        String completeClass = wrapInClass(snippetName, javaCode);
        compileJavaCode(snippetName, completeClass);
        // If compilation succeeds, the test passes
    }

    public String runAndCaptureOutput(String exampleName, String javaCode) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            // Redirect System.out to capture output
            System.setOut(new PrintStream(outputStream));

            compileAndRun(exampleName, javaCode);

            return outputStream.toString();
        } finally {
            System.setOut(originalOut);
        }
    }

    private String wrapInClass(String className, String javaCode) {
        return String.format("""
            package ai.tegmentum.wasmtime4j.docs.examples;

            import ai.tegmentum.wasmtime4j.*;
            import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
            import ai.tegmentum.wasmtime4j.component.*;
            import ai.tegmentum.wasmtime4j.async.*;
            import ai.tegmentum.wasmtime4j.security.*;
            import ai.tegmentum.wasmtime4j.wasi.*;
            import ai.tegmentum.wasmtime4j.performance.*;
            import ai.tegmentum.wasmtime4j.resource.*;

            import java.nio.file.Files;
            import java.nio.file.Paths;
            import java.util.*;
            import java.util.concurrent.*;
            import java.time.Duration;

            public class %s {
                public static void main(String[] args) throws Exception {
                    %s
                }
            }
            """, sanitizeClassName(className), javaCode);
    }

    private String sanitizeClassName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("^[0-9]", "_$0");
    }

    private Class<?> compileJavaCode(String className, String javaCode) throws Exception {
        // Write source file
        Path sourceFile = tempDir.resolve(sanitizeClassName(className) + ".java");
        Files.write(sourceFile, javaCode.getBytes());

        // Compile
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> compilationUnits =
            fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile.toFile()));

        List<String> options = Arrays.asList(
            "-cp", System.getProperty("java.class.path") + ":" + tempDir.toString(),
            "-d", tempDir.toString()
        );

        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, null, options, null, compilationUnits);

        boolean success = task.call();
        fileManager.close();

        if (!success) {
            throw new RuntimeException("Compilation failed for " + className);
        }

        // Load compiled class
        ClassLoader classLoader = new DirectoryClassLoader(tempDir);
        return classLoader.loadClass("ai.tegmentum.wasmtime4j.docs.examples." + sanitizeClassName(className));
    }
}
```

### Specific Documentation Tests

```java
package ai.tegmentum.wasmtime4j.docs.test;

import org.junit.jupiter.api.Test;

public class ApiReferenceDocumentationTest extends DocumentationTestBase {

    @Test
    public void testBasicEngineUsage() throws Exception {
        String exampleCode = """
            // Create an engine with default configuration
            try (Engine engine = Engine.create()) {
                System.out.println("Engine created successfully");
            }

            // Create an engine with custom configuration
            EngineConfig config = new EngineConfig()
                .optimizationLevel(OptimizationLevel.SPEED)
                .debugInfo(true)
                .parallelCompilation(true);

            try (Engine engine = Engine.create(config)) {
                System.out.println("Configured engine created successfully");
            }
            """;

        runDocumentationExample("BasicEngineUsage", exampleCode);
    }

    @Test
    public void testRuntimeSelection() throws Exception {
        String exampleCode = """
            // Automatic runtime selection (recommended)
            try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
                RuntimeInfo info = runtime.getRuntimeInfo();
                System.out.println("Runtime type: " + info.getRuntimeType());
                System.out.println("Wasmtime version: " + info.getWasmtimeVersion());
            }
            """;

        runDocumentationExample("RuntimeSelection", exampleCode);
    }

    @Test
    public void testModuleCompilation() throws Exception {
        String exampleCode = """
            byte[] wasmBytes = createSimpleAddModule();

            try (WasmRuntime runtime = WasmRuntimeFactory.create();
                 Engine engine = runtime.createEngine()) {

                Module module = runtime.compileModule(engine, wasmBytes);
                System.out.println("Module compiled successfully");

                // Get module information
                ModuleInfo info = module.getInfo();
                System.out.println("Exports: " + info.getExports().size());
                System.out.println("Imports: " + info.getImports().size());
            }

            // Helper method
            private static byte[] createSimpleAddModule() {
                return new byte[] {
                    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
                    0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f,
                    0x03, 0x02, 0x01, 0x00,
                    0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00,
                    0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b
                };
            }
            """;

        runDocumentationExample("ModuleCompilation", exampleCode);
    }

    @Test
    public void testFunctionCallExample() throws Exception {
        String exampleCode = """
            byte[] wasmBytes = createSimpleAddModule();

            try (WasmRuntime runtime = WasmRuntimeFactory.create();
                 Engine engine = runtime.createEngine()) {

                Module module = runtime.compileModule(engine, wasmBytes);
                Store store = runtime.createStore(engine);
                Instance instance = runtime.instantiate(module);

                WasmFunction addFunction = instance.getFunction("add")
                    .orElseThrow(() -> new RuntimeException("Function 'add' not found"));

                WasmValue[] result = addFunction.call(
                    WasmValue.i32(5),
                    WasmValue.i32(3)
                );

                System.out.println("5 + 3 = " + result[0].asInt());
            }

            private static byte[] createSimpleAddModule() {
                return new byte[] {
                    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
                    0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f,
                    0x03, 0x02, 0x01, 0x00,
                    0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00,
                    0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b
                };
            }
            """;

        runAndVerifyOutput("FunctionCall", exampleCode, "5 + 3 = 8");
    }
}

public class GettingStartedDocumentationTest extends DocumentationTestBase {

    @Test
    public void testQuickStartExample() throws Exception {
        // Test the complete quick start example from the getting started guide
        String quickStartCode = Files.readString(
            Paths.get("docs/guides/getting-started-complete.md")
                .resolve("quick-start-example.java"));

        runDocumentationExample("QuickStart", extractJavaCode(quickStartCode));
    }

    @Test
    public void testStepByStepTutorial() throws Exception {
        // Test each step of the tutorial independently
        testStep1_CreatingModule();
        testStep2_LoadingModule();
        testStep3_WorkingWithMemory();
        testStep4_HostFunctions();
        testStep5_Configuration();
    }

    private void testStep1_CreatingModule() throws Exception {
        // Extract and test Step 1 code
        String stepCode = extractStepCode("docs/guides/getting-started-complete.md", "Step 1");
        runDocumentationExample("Step1", stepCode);
    }

    // Similar methods for other steps...
}
```

### Maven Plugin for Documentation Testing

```xml
<!-- pom.xml -->
<plugin>
    <groupId>ai.tegmentum</groupId>
    <artifactId>documentation-test-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <id>test-documentation</id>
            <phase>test</phase>
            <goals>
                <goal>test-docs</goal>
            </goals>
            <configuration>
                <documentationDirectory>docs</documentationDirectory>
                <includePatterns>
                    <pattern>**/*.md</pattern>
                    <pattern>**/*.java</pattern>
                </includePatterns>
                <excludePatterns>
                    <pattern>**/target/**</pattern>
                </excludePatterns>
                <testTimeout>30000</testTimeout>
                <parallel>true</parallel>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Link Validation

### Link Checker Implementation

```java
package ai.tegmentum.wasmtime4j.docs.test;

import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LinkValidationTest {

    private static final Pattern MARKDOWN_LINK_PATTERN =
        Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");

    private static final Pattern HTML_LINK_PATTERN =
        Pattern.compile("<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>");

    @Test
    public void validateAllDocumentationLinks() throws Exception {
        List<LinkValidationResult> results = new ArrayList<>();

        // Find all documentation files
        try (Stream<Path> paths = Files.walk(Paths.get("docs"))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".md") ||
                               path.toString().endsWith(".html"))
                 .forEach(path -> {
                     try {
                         results.addAll(validateLinksInFile(path));
                     } catch (Exception e) {
                         results.add(new LinkValidationResult(
                             path.toString(), "", false, "Failed to process file: " + e.getMessage()));
                     }
                 });
        }

        // Report results
        List<LinkValidationResult> failures = results.stream()
            .filter(result -> !result.isValid())
            .toList();

        if (!failures.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Link validation failures:\n");
            failures.forEach(failure ->
                errorMessage.append(String.format("  %s: %s - %s\n",
                    failure.getFile(), failure.getLink(), failure.getError())));

            throw new AssertionError(errorMessage.toString());
        }

        System.out.println("Validated " + results.size() + " links successfully");
    }

    private List<LinkValidationResult> validateLinksInFile(Path filePath) throws Exception {
        List<LinkValidationResult> results = new ArrayList<>();
        String content = Files.readString(filePath);
        String fileName = filePath.toString();

        if (fileName.endsWith(".md")) {
            results.addAll(validateMarkdownLinks(fileName, content));
        } else if (fileName.endsWith(".html")) {
            results.addAll(validateHtmlLinks(fileName, content));
        }

        return results;
    }

    private List<LinkValidationResult> validateMarkdownLinks(String fileName, String content) {
        List<LinkValidationResult> results = new ArrayList<>();
        Matcher matcher = MARKDOWN_LINK_PATTERN.matcher(content);

        while (matcher.find()) {
            String linkText = matcher.group(1);
            String linkUrl = matcher.group(2);

            LinkValidationResult result = validateLink(fileName, linkUrl);
            results.add(result);
        }

        return results;
    }

    private List<LinkValidationResult> validateHtmlLinks(String fileName, String content) throws Exception {
        List<LinkValidationResult> results = new ArrayList<>();
        Document doc = Jsoup.parse(content);
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String linkUrl = link.attr("href");
            LinkValidationResult result = validateLink(fileName, linkUrl);
            results.add(result);
        }

        return results;
    }

    private LinkValidationResult validateLink(String fileName, String linkUrl) {
        try {
            if (linkUrl.startsWith("#")) {
                // Fragment link - validate anchor exists in same file
                return validateFragmentLink(fileName, linkUrl);
            } else if (linkUrl.startsWith("http://") || linkUrl.startsWith("https://")) {
                // External link - validate HTTP response
                return validateExternalLink(fileName, linkUrl);
            } else {
                // Internal link - validate file exists
                return validateInternalLink(fileName, linkUrl);
            }
        } catch (Exception e) {
            return new LinkValidationResult(fileName, linkUrl, false, e.getMessage());
        }
    }

    private LinkValidationResult validateFragmentLink(String fileName, String fragment) throws Exception {
        // Read file and check if fragment anchor exists
        String content = Files.readString(Paths.get(fileName));
        String anchorId = fragment.substring(1); // Remove #

        // Check for markdown headers that would generate this anchor
        Pattern headerPattern = Pattern.compile("#+\\s+(.+)");
        Matcher matcher = headerPattern.matcher(content);

        while (matcher.find()) {
            String headerText = matcher.group(1);
            String generatedId = headerText.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");

            if (generatedId.equals(anchorId)) {
                return new LinkValidationResult(fileName, fragment, true, "");
            }
        }

        return new LinkValidationResult(fileName, fragment, false, "Anchor not found");
    }

    private LinkValidationResult validateExternalLink(String fileName, String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "Wasmtime4j-Doc-Validator/1.0");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 400) {
                return new LinkValidationResult(fileName, url, true, "");
            } else {
                return new LinkValidationResult(fileName, url, false,
                    "HTTP " + responseCode + ": " + connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }
    }

    private LinkValidationResult validateInternalLink(String fileName, String linkUrl) throws Exception {
        Path basePath = Paths.get(fileName).getParent();
        Path targetPath = basePath.resolve(linkUrl).normalize();

        if (Files.exists(targetPath)) {
            return new LinkValidationResult(fileName, linkUrl, true, "");
        } else {
            return new LinkValidationResult(fileName, linkUrl, false, "File not found: " + targetPath);
        }
    }

    private static class LinkValidationResult {
        private final String file;
        private final String link;
        private final boolean valid;
        private final String error;

        public LinkValidationResult(String file, String link, boolean valid, String error) {
            this.file = file;
            this.link = link;
            this.valid = valid;
            this.error = error;
        }

        public String getFile() { return file; }
        public String getLink() { return link; }
        public boolean isValid() { return valid; }
        public String getError() { return error; }
    }
}
```

## Documentation Sync Testing

### API Change Detection

```java
package ai.tegmentum.wasmtime4j.docs.test;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class ApiDocumentationSyncTest {

    @Test
    public void verifyAllPublicMethodsAreDocumented() throws Exception {
        Set<String> undocumentedMethods = new HashSet<>();

        // Get all public API classes
        Class<?>[] apiClasses = {
            Engine.class,
            WasmRuntime.class,
            Module.class,
            Store.class,
            Instance.class,
            WasmFunction.class,
            WasmMemory.class,
            WasmGlobal.class,
            WasmTable.class,
            // Add all other public API classes
        };

        for (Class<?> apiClass : apiClasses) {
            undocumentedMethods.addAll(findUndocumentedMethods(apiClass));
        }

        if (!undocumentedMethods.isEmpty()) {
            String errorMessage = "The following public methods are not documented:\n" +
                undocumentedMethods.stream()
                    .sorted()
                    .collect(Collectors.joining("\n"));

            throw new AssertionError(errorMessage);
        }
    }

    private Set<String> findUndocumentedMethods(Class<?> clazz) throws Exception {
        Set<String> undocumented = new HashSet<>();

        // Get documentation content
        String apiDocContent = Files.readString(Paths.get("docs/api/api-reference.md"));

        // Get all public methods
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers()) &&
                !isInherited(method) &&
                !isDeprecated(method)) {

                String methodSignature = generateMethodSignature(method);

                // Check if method is mentioned in documentation
                if (!apiDocContent.contains(method.getName()) ||
                    !containsMethodExample(apiDocContent, method)) {
                    undocumented.add(clazz.getSimpleName() + "." + methodSignature);
                }
            }
        }

        return undocumented;
    }

    private boolean containsMethodExample(String docContent, Method method) {
        // Look for code examples that use this method
        String methodName = method.getName();

        // Simple heuristic: look for method calls in code blocks
        Pattern codeBlockPattern = Pattern.compile("```java([\\s\\S]*?)```");
        Matcher matcher = codeBlockPattern.matcher(docContent);

        while (matcher.find()) {
            String codeBlock = matcher.group(1);
            if (codeBlock.contains("." + methodName + "(") ||
                codeBlock.contains(methodName + "(")) {
                return true;
            }
        }

        return false;
    }

    @Test
    public void verifyDocumentationExamplesUseCurrentApi() throws Exception {
        List<ApiChangeViolation> violations = new ArrayList<>();

        // Extract all code examples from documentation
        List<CodeExample> examples = extractCodeExamples();

        for (CodeExample example : examples) {
            violations.addAll(validateCodeExample(example));
        }

        if (!violations.isEmpty()) {
            String errorMessage = "Documentation examples use outdated API:\n" +
                violations.stream()
                    .map(v -> v.getFile() + ": " + v.getViolation())
                    .collect(Collectors.joining("\n"));

            throw new AssertionError(errorMessage);
        }
    }

    private List<CodeExample> extractCodeExamples() throws Exception {
        List<CodeExample> examples = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get("docs"))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".md"))
                 .forEach(path -> {
                     try {
                         examples.addAll(extractExamplesFromFile(path));
                     } catch (Exception e) {
                         // Log error and continue
                     }
                 });
        }

        return examples;
    }

    private List<ApiChangeViolation> validateCodeExample(CodeExample example) {
        List<ApiChangeViolation> violations = new ArrayList<>();

        // Check for deprecated method usage
        violations.addAll(checkForDeprecatedMethods(example));

        // Check for removed methods
        violations.addAll(checkForRemovedMethods(example));

        // Check for incorrect parameter types
        violations.addAll(checkParameterTypes(example));

        return violations;
    }
}
```

## Integration with CI/CD

### GitHub Actions Workflow

```yaml
# .github/workflows/documentation-validation.yml
name: Documentation Validation

on:
  push:
    branches: [ main, develop ]
    paths: [ 'docs/**', 'src/**' ]
  pull_request:
    branches: [ main ]
    paths: [ 'docs/**', 'src/**' ]

jobs:
  validate-documentation:
    runs-on: ubuntu-latest

    strategy:
      matrix:
        java-version: [11, 17, 21, 23]

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK ${{ matrix.java-version }}
      uses: actions/setup-java@v3
      with:
        java-version: ${{ matrix.java-version }}
        distribution: 'temurin'

    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2

    - name: Install system dependencies
      run: |
        sudo apt-get update
        sudo apt-get install -y libc6-dev

    - name: Test documentation code examples
      run: ./mvnw test -Dtest=DocumentationTest* -q

    - name: Validate documentation links
      run: ./mvnw test -Dtest=LinkValidationTest -q

    - name: Check API documentation sync
      run: ./mvnw test -Dtest=ApiDocumentationSyncTest -q

    - name: Generate documentation test report
      if: always()
      run: |
        ./mvnw site -DskipTests
        mkdir -p docs-validation-report
        cp -r target/site/* docs-validation-report/

    - name: Upload documentation test results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: documentation-validation-results-java-${{ matrix.java-version }}
        path: docs-validation-report/

  validate-cross-platform:
    runs-on: ${{ matrix.os }}

    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        java-version: [11, 23]

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK ${{ matrix.java-version }}
      uses: actions/setup-java@v3
      with:
        java-version: ${{ matrix.java-version }}
        distribution: 'temurin'

    - name: Test platform-specific examples
      run: ./mvnw test -Dtest=PlatformSpecificDocumentationTest -q
```

### Pre-commit Hook

```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "Running documentation validation..."

# Test documentation examples
./mvnw test -Dtest=DocumentationTest* -q
if [ $? -ne 0 ]; then
    echo "❌ Documentation examples failed to compile/run"
    echo "Please fix the examples before committing"
    exit 1
fi

# Validate links
./mvnw test -Dtest=LinkValidationTest -q
if [ $? -ne 0 ]; then
    echo "❌ Documentation contains broken links"
    echo "Please fix the links before committing"
    exit 1
fi

# Check API sync
./mvnw test -Dtest=ApiDocumentationSyncTest -q
if [ $? -ne 0 ]; then
    echo "❌ Documentation is out of sync with API"
    echo "Please update documentation to match current API"
    exit 1
fi

echo "✅ All documentation validation passed"
```

## Writing Testable Documentation

### Best Practices

1. **Use Complete, Runnable Examples**
```markdown
<!-- Good: Complete example that can be tested -->
```java
public class CompleteExample {
    public static void main(String[] args) throws Exception {
        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            byte[] wasmBytes = loadExample();
            Module module = runtime.compileModule(engine, wasmBytes);
            // ... rest of example
        }
    }

    private static byte[] loadExample() {
        // Return valid WASM bytecode
        return new byte[] { /* ... */ };
    }
}
```

<!-- Bad: Incomplete snippet that can't be tested -->
```java
Module module = runtime.compileModule(engine, wasmBytes);
```
```

2. **Include Expected Output**
```markdown
```java
// Example that prints specific output
System.out.println("Result: " + result[0].asInt());
```

Expected output:
```
Result: 42
```
```

3. **Use Testable WASM Modules**
```java
// Include helper methods for creating test modules
private static byte[] createSimpleAddModule() {
    return new byte[] {
        0x00, 0x61, 0x73, 0x6d, // Magic number
        0x01, 0x00, 0x00, 0x00, // Version
        // ... complete module bytes
    };
}
```

## Automated Documentation Updates

### API Change Detection and Updates

```java
package ai.tegmentum.wasmtime4j.docs.tools;

/**
 * Tool to automatically update documentation when APIs change.
 */
public class DocumentationUpdater {

    public void updateDocumentationForApiChanges() throws Exception {
        // Detect API changes
        List<ApiChange> changes = detectApiChanges();

        if (changes.isEmpty()) {
            System.out.println("No API changes detected");
            return;
        }

        // Update affected documentation
        for (ApiChange change : changes) {
            updateDocumentationForChange(change);
        }

        // Generate summary report
        generateUpdateReport(changes);
    }

    private List<ApiChange> detectApiChanges() throws Exception {
        // Compare current API with previous version
        ApiComparator comparator = new ApiComparator();
        return comparator.compareWithPreviousVersion();
    }

    private void updateDocumentationForChange(ApiChange change) throws Exception {
        switch (change.getType()) {
            case METHOD_ADDED:
                addMethodDocumentation(change);
                break;
            case METHOD_REMOVED:
                removeMethodDocumentation(change);
                break;
            case METHOD_SIGNATURE_CHANGED:
                updateMethodSignature(change);
                break;
            case CLASS_ADDED:
                addClassDocumentation(change);
                break;
            // Handle other change types
        }
    }

    private void addMethodDocumentation(ApiChange change) throws Exception {
        // Generate documentation for new method
        String documentation = generateMethodDocumentation(change.getMethod());

        // Insert into appropriate documentation file
        insertIntoApiReference(change.getClassName(), documentation);

        // Generate example
        String example = generateMethodExample(change.getMethod());
        insertIntoExamples(change.getClassName(), example);
    }
}
```

This comprehensive documentation testing framework ensures that all documentation remains accurate, up-to-date, and functional throughout the development lifecycle.