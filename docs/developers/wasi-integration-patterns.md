# WASI Integration Patterns

This guide provides comprehensive patterns and examples for integrating WebAssembly System Interface (WASI) testing into the Wasmtime4j comparison framework. WASI testing requires special handling due to its system interface requirements and cross-platform considerations.

## Overview

WASI (WebAssembly System Interface) provides a standardized API for WebAssembly modules to interact with the host system in a secure and portable way. Testing WASI functionality requires:

- **File System Simulation**: Virtual file systems for testing I/O operations
- **Environment Management**: Controlled environment variable handling
- **Process Isolation**: Secure execution environments for system calls
- **Cross-Platform Compatibility**: Consistent behavior across operating systems

## WASI Testing Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    WASI Testing Framework                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ WASI Modules │  │  Test Env    │  │ Verification │          │
│  │              │  │              │  │              │          │
│  │ • File I/O   │  │ • VFS        │  │ • Output     │          │
│  │ • Args/Env   │  │ • Mock Env   │  │ • Exit Codes │          │
│  │ • Random     │  │ • Sandboxed  │  │ • Side Effects│          │
│  │ • Clock      │  │ • Isolated   │  │ • Security   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│         │                   │                   │               │
│         └───────────────────┼───────────────────┘               │
│                             │                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Runtime Comparison                         │   │
│  │                                                         │   │
│  │ • JNI WASI Implementation                              │   │
│  │ • Panama WASI Implementation                           │   │
│  │ • Behavioral Compatibility                             │   │
│  │ • Performance Characteristics                          │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## WASI Test Integrator

### Core WASI Test Integrator

```java
package ai.tegmentum.wasmtime4j.comparison.integrators;

import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiFileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Specialized integrator for WASI (WebAssembly System Interface) tests.
 * Handles file system virtualization, environment isolation, and system call testing.
 */
public final class WasiTestIntegrator implements TestSuiteIntegrator {
    private static final Logger LOGGER = Logger.getLogger(WasiTestIntegrator.class.getName());

    private final WasiTestConfiguration configuration;
    private final WasiEnvironmentManager environmentManager;
    private final WasiFileSystemManager fileSystemManager;

    public WasiTestIntegrator(final WasiTestConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
        this.environmentManager = new WasiEnvironmentManager(configuration);
        this.fileSystemManager = new WasiFileSystemManager(configuration);
    }

    @Override
    public String getName() {
        return "wasi-integrator";
    }

    @Override
    public CompletableFuture<List<TestExecutionResult>> executeTests(
            final TestSource testSource,
            final Set<RuntimeType> runtimeTypes,
            final Map<String, Object> configuration) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Starting WASI test execution for: " + testSource.getName());

                final List<WasiTestCase> wasiTests = loadWasiTests(testSource);
                final List<TestExecutionResult> allResults = new ArrayList<>();

                for (final RuntimeType runtime : runtimeTypes) {
                    final List<TestExecutionResult> runtimeResults =
                        executeWasiTestsForRuntime(wasiTests, runtime);
                    allResults.addAll(runtimeResults);
                }

                LOGGER.info("WASI test execution complete: " + allResults.size() + " results");
                return allResults;

            } catch (Exception e) {
                LOGGER.severe("WASI test execution failed: " + e.getMessage());
                throw new TestIntegrationException("Failed to execute WASI tests", e);
            }
        });
    }

    private List<TestExecutionResult> executeWasiTestsForRuntime(
            final List<WasiTestCase> testCases,
            final RuntimeType runtime) {

        final List<TestExecutionResult> results = new ArrayList<>();

        for (final WasiTestCase testCase : testCases) {
            try {
                final TestExecutionResult result = executeWasiTest(testCase, runtime);
                results.add(result);

                if (configuration.isFailFast() && result.getStatus() == TestStatus.FAILED) {
                    LOGGER.warning("Failing fast due to WASI test failure: " + testCase.getName());
                    break;
                }

            } catch (Exception e) {
                results.add(createErrorResult(testCase, runtime, e));
            }
        }

        return results;
    }

    private TestExecutionResult executeWasiTest(final WasiTestCase testCase,
                                              final RuntimeType runtime) {
        final Instant startTime = Instant.now();

        try (WasiTestEnvironment testEnv = createTestEnvironment(testCase, runtime)) {
            LOGGER.fine("Executing WASI test: " + testCase.getName() + " on " + runtime);

            // Setup WASI context
            final WasiContext wasiContext = createWasiContext(testCase, testEnv);

            // Execute the WASI module
            final WasiExecutionResult executionResult = executeWasiModule(
                testCase, wasiContext, runtime, testEnv
            );

            // Verify results
            final WasiVerificationResult verificationResult =
                verifyWasiExecution(testCase, executionResult, testEnv);

            final Duration executionTime = Duration.between(startTime, Instant.now());

            return TestExecutionResult.builder()
                .testName(testCase.getName())
                .testType("wasi")
                .runtime(runtime)
                .status(verificationResult.isSuccess() ? TestStatus.PASSED : TestStatus.FAILED)
                .executionTime(executionTime)
                .output(executionResult.getStdout())
                .errorOutput(executionResult.getStderr())
                .metadata(createWasiResultMetadata(testCase, executionResult, verificationResult))
                .build();

        } catch (WasiExecutionException e) {
            return createWasiErrorResult(testCase, runtime, startTime, e);
        }
    }

    private WasiTestEnvironment createTestEnvironment(final WasiTestCase testCase,
                                                    final RuntimeType runtime) {
        return WasiTestEnvironment.builder()
            .runtime(runtime)
            .workingDirectory(fileSystemManager.createWorkingDirectory(testCase))
            .environment(environmentManager.createEnvironment(testCase))
            .fileSystem(fileSystemManager.createFileSystem(testCase))
            .arguments(testCase.getArguments())
            .preOpenDirectories(testCase.getPreOpenDirectories())
            .allowedSystemCalls(testCase.getAllowedSystemCalls())
            .timeoutSettings(configuration.getTimeoutSettings())
            .build();
    }

    private WasiContext createWasiContext(final WasiTestCase testCase,
                                        final WasiTestEnvironment testEnv) {
        final WasiConfig wasiConfig = WasiConfig.builder()
            .arguments(testEnv.getArguments())
            .environment(testEnv.getEnvironment())
            .preOpenDirectories(testEnv.getPreOpenDirectories())
            .inheritStdin(testCase.shouldInheritStdin())
            .inheritStdout(testCase.shouldInheritStdout())
            .inheritStderr(testCase.shouldInheritStderr())
            .build();

        return WasiContext.create(wasiConfig);
    }
}
```

### WASI Test Case Definition

```java
/**
 * Represents a WASI test case with system interface requirements.
 */
public final class WasiTestCase {
    private final String name;
    private final String description;
    private final byte[] wasmModule;
    private final List<String> arguments;
    private final Map<String, String> environment;
    private final List<WasiPreOpenDirectory> preOpenDirectories;
    private final Set<String> allowedSystemCalls;
    private final WasiExpectedResults expectedResults;
    private final WasiTestType testType;
    private final Map<String, Object> metadata;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String description;
        private byte[] wasmModule;
        private List<String> arguments = new ArrayList<>();
        private Map<String, String> environment = new HashMap<>();
        private List<WasiPreOpenDirectory> preOpenDirectories = new ArrayList<>();
        private Set<String> allowedSystemCalls = new HashSet<>();
        private WasiExpectedResults expectedResults;
        private WasiTestType testType = WasiTestType.FUNCTIONAL;
        private Map<String, Object> metadata = new HashMap<>();

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder wasmModule(final byte[] wasmModule) {
            this.wasmModule = wasmModule.clone();
            return this;
        }

        public Builder argument(final String argument) {
            this.arguments.add(argument);
            return this;
        }

        public Builder arguments(final List<String> arguments) {
            this.arguments.addAll(arguments);
            return this;
        }

        public Builder environmentVariable(final String key, final String value) {
            this.environment.put(key, value);
            return this;
        }

        public Builder environment(final Map<String, String> environment) {
            this.environment.putAll(environment);
            return this;
        }

        public Builder preOpenDirectory(final String guestPath, final String hostPath) {
            this.preOpenDirectories.add(new WasiPreOpenDirectory(guestPath, hostPath));
            return this;
        }

        public Builder allowSystemCall(final String systemCall) {
            this.allowedSystemCalls.add(systemCall);
            return this;
        }

        public Builder expectedResults(final WasiExpectedResults expectedResults) {
            this.expectedResults = expectedResults;
            return this;
        }

        public Builder testType(final WasiTestType testType) {
            this.testType = testType;
            return this;
        }

        public WasiTestCase build() {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(wasmModule, "wasmModule cannot be null");

            return new WasiTestCase(
                name, description, wasmModule, arguments, environment,
                preOpenDirectories, allowedSystemCalls, expectedResults,
                testType, metadata
            );
        }
    }

    // Getters and utility methods...

    public boolean shouldInheritStdin() {
        return testType == WasiTestType.INTERACTIVE;
    }

    public boolean shouldInheritStdout() {
        return !expectedResults.hasStdoutExpectations();
    }

    public boolean shouldInheritStderr() {
        return !expectedResults.hasStderrExpectations();
    }
}

/**
 * WASI test types.
 */
public enum WasiTestType {
    FUNCTIONAL,     // Basic functionality testing
    SECURITY,       // Security-focused testing
    PERFORMANCE,    // Performance measurement
    COMPATIBILITY,  // Cross-platform compatibility
    INTERACTIVE,    // Interactive I/O testing
    STRESS          // Stress and resource testing
}

/**
 * Expected results for WASI test execution.
 */
public final class WasiExpectedResults {
    private final OptionalInt expectedExitCode;
    private final Optional<String> expectedStdout;
    private final Optional<String> expectedStderr;
    private final Optional<Pattern> stdoutPattern;
    private final Optional<Pattern> stderrPattern;
    private final List<WasiFileExpectation> fileExpectations;
    private final Map<String, Object> customExpectations;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private OptionalInt expectedExitCode = OptionalInt.empty();
        private Optional<String> expectedStdout = Optional.empty();
        private Optional<String> expectedStderr = Optional.empty();
        private Optional<Pattern> stdoutPattern = Optional.empty();
        private Optional<Pattern> stderrPattern = Optional.empty();
        private List<WasiFileExpectation> fileExpectations = new ArrayList<>();
        private Map<String, Object> customExpectations = new HashMap<>();

        public Builder exitCode(final int exitCode) {
            this.expectedExitCode = OptionalInt.of(exitCode);
            return this;
        }

        public Builder stdout(final String stdout) {
            this.expectedStdout = Optional.of(stdout);
            return this;
        }

        public Builder stdoutPattern(final String pattern) {
            this.stdoutPattern = Optional.of(Pattern.compile(pattern));
            return this;
        }

        public Builder fileCreated(final String path) {
            this.fileExpectations.add(WasiFileExpectation.created(path));
            return this;
        }

        public Builder fileContent(final String path, final String expectedContent) {
            this.fileExpectations.add(WasiFileExpectation.content(path, expectedContent));
            return this;
        }

        public WasiExpectedResults build() {
            return new WasiExpectedResults(
                expectedExitCode, expectedStdout, expectedStderr,
                stdoutPattern, stderrPattern, fileExpectations, customExpectations
            );
        }
    }

    public boolean hasStdoutExpectations() {
        return expectedStdout.isPresent() || stdoutPattern.isPresent();
    }

    public boolean hasStderrExpectations() {
        return expectedStderr.isPresent() || stderrPattern.isPresent();
    }
}
```

## WASI Environment Management

### Virtual File System Management

```java
package ai.tegmentum.wasmtime4j.comparison.wasi;

/**
 * Manages virtual file systems for WASI testing.
 */
public final class WasiFileSystemManager {
    private static final Logger LOGGER = Logger.getLogger(WasiFileSystemManager.class.getName());

    private final WasiTestConfiguration configuration;
    private final Path baseTestDirectory;

    public WasiFileSystemManager(final WasiTestConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
        this.baseTestDirectory = configuration.getBaseTestDirectory();
    }

    /**
     * Creates an isolated file system for a WASI test.
     */
    public WasiFileSystem createFileSystem(final WasiTestCase testCase) {
        try {
            final Path testDirectory = createTestDirectory(testCase);
            final WasiFileSystem fileSystem = new VirtualWasiFileSystem(testDirectory);

            // Setup initial file structure
            setupInitialFiles(fileSystem, testCase);

            // Configure permissions
            configurePermissions(fileSystem, testCase);

            return fileSystem;

        } catch (IOException e) {
            throw new WasiTestException("Failed to create file system for test: " + testCase.getName(), e);
        }
    }

    /**
     * Creates a working directory for the test.
     */
    public Path createWorkingDirectory(final WasiTestCase testCase) {
        try {
            final Path workingDir = baseTestDirectory.resolve("wasi-test-" + testCase.getName() + "-" + System.currentTimeMillis());
            Files.createDirectories(workingDir);

            // Setup test-specific directory structure
            setupDirectoryStructure(workingDir, testCase);

            return workingDir;

        } catch (IOException e) {
            throw new WasiTestException("Failed to create working directory", e);
        }
    }

    private void setupInitialFiles(final WasiFileSystem fileSystem,
                                 final WasiTestCase testCase) {
        // Create initial files based on test requirements
        final List<WasiFileSetup> fileSetups = testCase.getInitialFileSetup();

        for (final WasiFileSetup setup : fileSetups) {
            switch (setup.getType()) {
                case TEXT_FILE:
                    fileSystem.createTextFile(setup.getPath(), setup.getContent());
                    break;
                case BINARY_FILE:
                    fileSystem.createBinaryFile(setup.getPath(), setup.getBinaryContent());
                    break;
                case DIRECTORY:
                    fileSystem.createDirectory(setup.getPath());
                    break;
                case SYMLINK:
                    fileSystem.createSymlink(setup.getPath(), setup.getTarget());
                    break;
            }
        }
    }

    private void configurePermissions(final WasiFileSystem fileSystem,
                                    final WasiTestCase testCase) {
        final List<WasiPermissionSetup> permissions = testCase.getPermissionSetup();

        for (final WasiPermissionSetup permission : permissions) {
            fileSystem.setPermissions(permission.getPath(), permission.getPermissions());
        }
    }
}

/**
 * Virtual file system implementation for WASI testing.
 */
public final class VirtualWasiFileSystem implements WasiFileSystem {
    private final Path rootDirectory;
    private final Map<String, FileMetadata> fileMetadata;
    private final Set<String> allowedPaths;

    public VirtualWasiFileSystem(final Path rootDirectory) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory cannot be null");
        this.fileMetadata = new ConcurrentHashMap<>();
        this.allowedPaths = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void createTextFile(final String path, final String content) {
        validatePath(path);
        try {
            final Path actualPath = rootDirectory.resolve(path);
            Files.createDirectories(actualPath.getParent());
            Files.write(actualPath, content.getBytes(StandardCharsets.UTF_8));

            fileMetadata.put(path, FileMetadata.textFile(content.length()));
            allowedPaths.add(path);

            LOGGER.fine("Created text file: " + path + " (" + content.length() + " bytes)");

        } catch (IOException e) {
            throw new WasiFileSystemException("Failed to create text file: " + path, e);
        }
    }

    @Override
    public void createBinaryFile(final String path, final byte[] content) {
        validatePath(path);
        try {
            final Path actualPath = rootDirectory.resolve(path);
            Files.createDirectories(actualPath.getParent());
            Files.write(actualPath, content);

            fileMetadata.put(path, FileMetadata.binaryFile(content.length));
            allowedPaths.add(path);

            LOGGER.fine("Created binary file: " + path + " (" + content.length + " bytes)");

        } catch (IOException e) {
            throw new WasiFileSystemException("Failed to create binary file: " + path, e);
        }
    }

    @Override
    public boolean exists(final String path) {
        validatePath(path);
        return Files.exists(rootDirectory.resolve(path));
    }

    @Override
    public byte[] readFile(final String path) {
        validatePath(path);
        validateAllowedPath(path);

        try {
            return Files.readAllBytes(rootDirectory.resolve(path));
        } catch (IOException e) {
            throw new WasiFileSystemException("Failed to read file: " + path, e);
        }
    }

    @Override
    public void cleanup() {
        try {
            FileUtils.deleteRecursively(rootDirectory);
            LOGGER.fine("Cleaned up virtual file system: " + rootDirectory);
        } catch (IOException e) {
            LOGGER.warning("Failed to cleanup virtual file system: " + e.getMessage());
        }
    }

    private void validatePath(final String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        if (path.contains("..")) {
            throw new SecurityException("Path traversal not allowed: " + path);
        }
    }

    private void validateAllowedPath(final String path) {
        if (!allowedPaths.contains(path)) {
            throw new SecurityException("Access denied to path: " + path);
        }
    }
}
```

### Environment Variable Management

```java
/**
 * Manages environment variables for WASI tests.
 */
public final class WasiEnvironmentManager {
    private static final Logger LOGGER = Logger.getLogger(WasiEnvironmentManager.class.getName());

    private final WasiTestConfiguration configuration;
    private final Set<String> allowedVariables;
    private final Map<String, String> defaultVariables;

    public WasiEnvironmentManager(final WasiTestConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
        this.allowedVariables = configuration.getAllowedEnvironmentVariables();
        this.defaultVariables = configuration.getDefaultEnvironmentVariables();
    }

    /**
     * Creates an isolated environment for a WASI test.
     */
    public Map<String, String> createEnvironment(final WasiTestCase testCase) {
        final Map<String, String> environment = new HashMap<>();

        // Add default variables
        environment.putAll(defaultVariables);

        // Add test-specific variables
        final Map<String, String> testVariables = testCase.getEnvironment();
        for (final Map.Entry<String, String> entry : testVariables.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();

            if (isAllowedVariable(key)) {
                environment.put(key, value);
                LOGGER.fine("Added environment variable: " + key + "=" + value);
            } else {
                LOGGER.warning("Blocked environment variable: " + key);
            }
        }

        // Add required WASI variables
        addRequiredWasiVariables(environment, testCase);

        return Collections.unmodifiableMap(environment);
    }

    private boolean isAllowedVariable(final String key) {
        // Always allow WASI-specific variables
        if (key.startsWith("WASI_") || key.startsWith("__WASI_")) {
            return true;
        }

        // Check configuration
        return allowedVariables.contains(key) ||
               allowedVariables.contains("*") ||
               allowedVariables.stream().anyMatch(pattern -> key.matches(pattern));
    }

    private void addRequiredWasiVariables(final Map<String, String> environment,
                                        final WasiTestCase testCase) {
        // Add standard WASI environment variables
        environment.putIfAbsent("PWD", "/");
        environment.putIfAbsent("HOME", "/home/wasi");
        environment.putIfAbsent("PATH", "/usr/bin:/bin");

        // Add test-specific WASI variables
        if (testCase.getTestType() == WasiTestType.SECURITY) {
            environment.put("WASI_SECURITY_MODE", "strict");
        }
    }
}
```

## WASI Test Verification

### Result Verification

```java
/**
 * Verifies WASI test execution results against expectations.
 */
public final class WasiResultVerifier {
    private static final Logger LOGGER = Logger.getLogger(WasiResultVerifier.class.getName());

    /**
     * Verifies the execution result against expected outcomes.
     */
    public WasiVerificationResult verify(final WasiTestCase testCase,
                                       final WasiExecutionResult executionResult,
                                       final WasiTestEnvironment testEnvironment) {

        final WasiExpectedResults expected = testCase.getExpectedResults();
        final WasiVerificationResult.Builder resultBuilder = WasiVerificationResult.builder();

        // Verify exit code
        verifyExitCode(expected, executionResult, resultBuilder);

        // Verify stdout
        verifyStdout(expected, executionResult, resultBuilder);

        // Verify stderr
        verifyStderr(expected, executionResult, resultBuilder);

        // Verify file system changes
        verifyFileSystemChanges(expected, testEnvironment, resultBuilder);

        // Verify custom expectations
        verifyCustomExpectations(expected, executionResult, testEnvironment, resultBuilder);

        final WasiVerificationResult result = resultBuilder.build();

        LOGGER.info("WASI verification result for " + testCase.getName() + ": " +
                   (result.isSuccess() ? "PASSED" : "FAILED"));

        return result;
    }

    private void verifyExitCode(final WasiExpectedResults expected,
                              final WasiExecutionResult executionResult,
                              final WasiVerificationResult.Builder resultBuilder) {

        if (expected.getExpectedExitCode().isPresent()) {
            final int expectedCode = expected.getExpectedExitCode().getAsInt();
            final int actualCode = executionResult.getExitCode();

            if (expectedCode == actualCode) {
                resultBuilder.addSuccess("Exit code matches: " + actualCode);
            } else {
                resultBuilder.addFailure("Exit code mismatch: expected " + expectedCode +
                                       " but got " + actualCode);
            }
        }
    }

    private void verifyStdout(final WasiExpectedResults expected,
                            final WasiExecutionResult executionResult,
                            final WasiVerificationResult.Builder resultBuilder) {

        final String actualStdout = executionResult.getStdout();

        // Verify exact stdout match
        if (expected.getExpectedStdout().isPresent()) {
            final String expectedStdout = expected.getExpectedStdout().get();
            if (expectedStdout.equals(actualStdout)) {
                resultBuilder.addSuccess("Stdout matches exactly");
            } else {
                resultBuilder.addFailure("Stdout mismatch:\nExpected: " + expectedStdout +
                                       "\nActual: " + actualStdout);
            }
        }

        // Verify stdout pattern match
        if (expected.getStdoutPattern().isPresent()) {
            final Pattern pattern = expected.getStdoutPattern().get();
            if (pattern.matcher(actualStdout).matches()) {
                resultBuilder.addSuccess("Stdout matches pattern: " + pattern.pattern());
            } else {
                resultBuilder.addFailure("Stdout does not match pattern: " + pattern.pattern() +
                                       "\nActual: " + actualStdout);
            }
        }
    }

    private void verifyFileSystemChanges(final WasiExpectedResults expected,
                                       final WasiTestEnvironment testEnvironment,
                                       final WasiVerificationResult.Builder resultBuilder) {

        final WasiFileSystem fileSystem = testEnvironment.getFileSystem();

        for (final WasiFileExpectation expectation : expected.getFileExpectations()) {
            switch (expectation.getType()) {
                case FILE_CREATED:
                    verifyFileCreated(expectation, fileSystem, resultBuilder);
                    break;
                case FILE_CONTENT:
                    verifyFileContent(expectation, fileSystem, resultBuilder);
                    break;
                case FILE_DELETED:
                    verifyFileDeleted(expectation, fileSystem, resultBuilder);
                    break;
                case DIRECTORY_CREATED:
                    verifyDirectoryCreated(expectation, fileSystem, resultBuilder);
                    break;
            }
        }
    }

    private void verifyFileCreated(final WasiFileExpectation expectation,
                                 final WasiFileSystem fileSystem,
                                 final WasiVerificationResult.Builder resultBuilder) {

        final String path = expectation.getPath();
        if (fileSystem.exists(path)) {
            resultBuilder.addSuccess("File created: " + path);
        } else {
            resultBuilder.addFailure("Expected file was not created: " + path);
        }
    }

    private void verifyFileContent(final WasiFileExpectation expectation,
                                 final WasiFileSystem fileSystem,
                                 final WasiVerificationResult.Builder resultBuilder) {

        final String path = expectation.getPath();
        if (!fileSystem.exists(path)) {
            resultBuilder.addFailure("File does not exist for content verification: " + path);
            return;
        }

        try {
            final byte[] actualContent = fileSystem.readFile(path);
            final String actualText = new String(actualContent, StandardCharsets.UTF_8);
            final String expectedContent = expectation.getExpectedContent();

            if (expectedContent.equals(actualText)) {
                resultBuilder.addSuccess("File content matches: " + path);
            } else {
                resultBuilder.addFailure("File content mismatch in " + path +
                                       ":\nExpected: " + expectedContent +
                                       "\nActual: " + actualText);
            }

        } catch (Exception e) {
            resultBuilder.addFailure("Failed to read file for content verification: " + path +
                                   " - " + e.getMessage());
        }
    }
}

/**
 * Result of WASI test verification.
 */
public final class WasiVerificationResult {
    private final boolean success;
    private final List<String> successMessages;
    private final List<String> failureMessages;
    private final Map<String, Object> metadata;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<String> successMessages = new ArrayList<>();
        private final List<String> failureMessages = new ArrayList<>();
        private final Map<String, Object> metadata = new HashMap<>();

        public Builder addSuccess(final String message) {
            successMessages.add(message);
            return this;
        }

        public Builder addFailure(final String message) {
            failureMessages.add(message);
            return this;
        }

        public Builder metadata(final String key, final Object value) {
            metadata.put(key, value);
            return this;
        }

        public WasiVerificationResult build() {
            return new WasiVerificationResult(
                failureMessages.isEmpty(),
                new ArrayList<>(successMessages),
                new ArrayList<>(failureMessages),
                new HashMap<>(metadata)
            );
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getSuccessMessages() {
        return Collections.unmodifiableList(successMessages);
    }

    public List<String> getFailureMessages() {
        return Collections.unmodifiableList(failureMessages);
    }

    public String getSummary() {
        final StringBuilder summary = new StringBuilder();
        summary.append("Success: ").append(success).append("\n");
        summary.append("Successes: ").append(successMessages.size()).append("\n");
        summary.append("Failures: ").append(failureMessages.size()).append("\n");

        if (!failureMessages.isEmpty()) {
            summary.append("Failure details:\n");
            failureMessages.forEach(msg -> summary.append("  - ").append(msg).append("\n"));
        }

        return summary.toString();
    }
}
```

## Cross-Platform WASI Testing

### Platform-Specific Considerations

```java
/**
 * Handles platform-specific WASI testing concerns.
 */
public final class CrossPlatformWasiTester {

    /**
     * Executes WASI tests with platform-specific adaptations.
     */
    public List<TestExecutionResult> executeWithPlatformAdaptations(
            final List<WasiTestCase> testCases,
            final Set<RuntimeType> runtimeTypes) {

        final OperatingSystem currentOS = detectOperatingSystem();
        final List<TestExecutionResult> results = new ArrayList<>();

        for (final WasiTestCase testCase : testCases) {
            // Adapt test case for current platform
            final WasiTestCase adaptedTestCase = adaptTestCaseForPlatform(testCase, currentOS);

            for (final RuntimeType runtime : runtimeTypes) {
                final TestExecutionResult result = executeWithPlatformContext(
                    adaptedTestCase, runtime, currentOS
                );
                results.add(result);
            }
        }

        return results;
    }

    private WasiTestCase adaptTestCaseForPlatform(final WasiTestCase testCase,
                                                final OperatingSystem os) {

        final WasiTestCase.Builder builder = WasiTestCase.builder()
            .name(testCase.getName())
            .description(testCase.getDescription())
            .wasmModule(testCase.getWasmModule())
            .testType(testCase.getTestType());

        // Adapt arguments for platform
        adaptArgumentsForPlatform(builder, testCase.getArguments(), os);

        // Adapt environment variables for platform
        adaptEnvironmentForPlatform(builder, testCase.getEnvironment(), os);

        // Adapt file paths for platform
        adaptFilePathsForPlatform(builder, testCase.getPreOpenDirectories(), os);

        return builder.build();
    }

    private void adaptArgumentsForPlatform(final WasiTestCase.Builder builder,
                                         final List<String> arguments,
                                         final OperatingSystem os) {

        for (final String arg : arguments) {
            String adaptedArg = arg;

            // Adapt path separators
            if (os == OperatingSystem.WINDOWS) {
                adaptedArg = adaptedArg.replace('/', '\\');
            } else {
                adaptedArg = adaptedArg.replace('\\', '/');
            }

            builder.argument(adaptedArg);
        }
    }

    private void adaptEnvironmentForPlatform(final WasiTestCase.Builder builder,
                                           final Map<String, String> environment,
                                           final OperatingSystem os) {

        for (final Map.Entry<String, String> entry : environment.entrySet()) {
            final String key = entry.getKey();
            String value = entry.getValue();

            // Adapt path environment variables
            if (isPathEnvironmentVariable(key)) {
                value = adaptPathForPlatform(value, os);
            }

            // Adapt platform-specific variables
            if (os == OperatingSystem.WINDOWS && key.equals("PATH")) {
                value = adaptWindowsPath(value);
            }

            builder.environmentVariable(key, value);
        }
    }

    private boolean isPathEnvironmentVariable(final String key) {
        return key.equals("PATH") || key.equals("LD_LIBRARY_PATH") ||
               key.equals("DYLD_LIBRARY_PATH") || key.endsWith("_PATH");
    }

    private String adaptPathForPlatform(final String path, final OperatingSystem os) {
        if (os == OperatingSystem.WINDOWS) {
            return path.replace('/', '\\').replace(":", ";");
        } else {
            return path.replace('\\', '/').replace(";", ":");
        }
    }
}

/**
 * Operating system detection.
 */
public enum OperatingSystem {
    WINDOWS,
    LINUX,
    MACOS,
    UNKNOWN;

    public static OperatingSystem detect() {
        final String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            return WINDOWS;
        } else if (osName.contains("linux")) {
            return LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return MACOS;
        } else {
            return UNKNOWN;
        }
    }
}
```

## WASI Test Configuration

### JSON Test Configuration

```json
{
  "wasiTestSuite": {
    "name": "WASI Compatibility Tests",
    "version": "1.0.0",
    "description": "Comprehensive WASI functionality testing"
  },
  "configuration": {
    "defaultTimeout": 60,
    "allowedSystemCalls": [
      "fd_read", "fd_write", "fd_close", "fd_seek",
      "path_open", "path_create_directory", "path_remove_directory",
      "environ_get", "environ_sizes_get",
      "args_get", "args_sizes_get",
      "random_get", "clock_time_get"
    ],
    "allowedEnvironmentVariables": [
      "HOME", "PATH", "PWD", "USER", "WASI_*"
    ],
    "fileSystemLimits": {
      "maxFileSize": "10MB",
      "maxFiles": 100,
      "maxDirectories": 50
    }
  },
  "tests": [
    {
      "name": "file_io_basic",
      "description": "Basic file I/O operations",
      "type": "functional",
      "wasmFile": "wasi/file_io_basic.wasm",
      "arguments": ["input.txt", "output.txt"],
      "environment": {
        "HOME": "/home/wasi",
        "PATH": "/usr/bin:/bin"
      },
      "preOpenDirectories": [
        {
          "guestPath": "/workspace",
          "hostPath": "test-workspace"
        }
      ],
      "initialFiles": [
        {
          "path": "/workspace/input.txt",
          "type": "text",
          "content": "Hello, WASI World!\n"
        }
      ],
      "expectedResults": {
        "exitCode": 0,
        "stdout": "File processing complete\n",
        "fileExpectations": [
          {
            "type": "file_created",
            "path": "/workspace/output.txt"
          },
          {
            "type": "file_content",
            "path": "/workspace/output.txt",
            "expectedContent": "HELLO, WASI WORLD!\n"
          }
        ]
      },
      "metadata": {
        "category": "file_io",
        "priority": "high"
      }
    },
    {
      "name": "environment_access",
      "description": "Environment variable access testing",
      "type": "functional",
      "wasmFile": "wasi/env_access.wasm",
      "environment": {
        "TEST_VAR": "test_value",
        "NUMERIC_VAR": "12345"
      },
      "expectedResults": {
        "exitCode": 0,
        "stdoutPattern": ".*TEST_VAR=test_value.*NUMERIC_VAR=12345.*"
      }
    },
    {
      "name": "security_sandbox",
      "description": "Security sandbox enforcement",
      "type": "security",
      "wasmFile": "wasi/security_test.wasm",
      "allowedSystemCalls": [
        "fd_read", "fd_write"
      ],
      "expectedResults": {
        "exitCode": 1,
        "stderrPattern": ".*access denied.*"
      },
      "metadata": {
        "category": "security",
        "severity": "critical"
      }
    }
  ]
}
```

## Integration Examples

### Maven Integration

```xml
<plugin>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j-wasi-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <wasiTestSuites>
            <testSuite>
                <name>wasi-core-tests</name>
                <configFile>src/test/resources/wasi-tests.json</configFile>
                <workingDirectory>${project.build.directory}/wasi-test-workspace</workingDirectory>
            </testSuite>
        </wasiTestSuites>
        <runtimeTypes>
            <runtimeType>JNI</runtimeType>
            <runtimeType>PANAMA</runtimeType>
        </runtimeTypes>
        <crossPlatformTesting>true</crossPlatformTesting>
        <generateDetailedReports>true</generateDetailedReports>
    </configuration>
    <executions>
        <execution>
            <id>run-wasi-tests</id>
            <phase>test</phase>
            <goals>
                <goal>run-wasi-tests</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Programmatic Usage

```java
public class WasiTestingExample {

    public static void main(String[] args) {
        // Create WASI test configuration
        final WasiTestConfiguration config = WasiTestConfiguration.builder()
            .baseTestDirectory(Paths.get("target/wasi-tests"))
            .executionTimeout(Duration.ofMinutes(5))
            .allowedEnvironmentVariables(Set.of("HOME", "PATH", "PWD", "WASI_*"))
            .fileSystemLimits(WasiFileSystemLimits.builder()
                .maxFileSize(ByteSize.ofMegabytes(10))
                .maxFiles(100)
                .build())
            .build();

        // Create WASI test integrator
        final WasiTestIntegrator integrator = new WasiTestIntegrator(config);

        // Define test source
        final TestSource wasiTestSource = new FileTestSource(
            "wasi-tests",
            Paths.get("src/test/resources/wasi-tests.json"),
            TestFileFormat.JSON
        );

        // Execute WASI tests
        final Set<RuntimeType> runtimes = Set.of(RuntimeType.JNI, RuntimeType.PANAMA);
        final CompletableFuture<List<TestExecutionResult>> future =
            integrator.executeTests(wasiTestSource, runtimes, Map.of());

        try {
            final List<TestExecutionResult> results = future.get(10, TimeUnit.MINUTES);
            analyzeWasiResults(results);
        } catch (Exception e) {
            System.err.println("WASI test execution failed: " + e.getMessage());
        }
    }

    private static void analyzeWasiResults(final List<TestExecutionResult> results) {
        final Map<RuntimeType, Long> passByRuntime = results.stream()
            .filter(r -> r.getStatus() == TestStatus.PASSED)
            .collect(Collectors.groupingBy(
                TestExecutionResult::getRuntime,
                Collectors.counting()
            ));

        System.out.println("WASI Test Results:");
        System.out.println("Total tests: " + results.size());

        for (final Map.Entry<RuntimeType, Long> entry : passByRuntime.entrySet()) {
            System.out.println(entry.getKey() + " passed: " + entry.getValue());
        }
    }
}
```

## Best Practices

### 1. Security Considerations

```java
// Always validate WASI paths
private void validateWasiPath(final String path) {
    if (path.contains("..")) {
        throw new SecurityException("Path traversal not allowed");
    }

    if (!path.startsWith("/")) {
        throw new SecurityException("Absolute paths required");
    }
}

// Limit resource usage
private void enforceResourceLimits(final WasiTestCase testCase) {
    if (testCase.getInitialFileSetup().size() > MAX_FILES) {
        throw new IllegalArgumentException("Too many initial files");
    }

    final long totalSize = testCase.getInitialFileSetup().stream()
        .mapToLong(setup -> setup.getSize())
        .sum();

    if (totalSize > MAX_TOTAL_FILE_SIZE) {
        throw new IllegalArgumentException("Total file size exceeds limit");
    }
}
```

### 2. Cross-Platform Compatibility

```java
// Use platform-agnostic paths in tests
private String normalizePath(final String path) {
    return path.replace('\\', '/');
}

// Handle platform-specific behavior
private WasiExpectedResults adaptExpectedResults(final WasiExpectedResults original,
                                               final OperatingSystem platform) {
    if (platform == OperatingSystem.WINDOWS) {
        // Windows-specific adaptations
        return original.withAdaptedPaths(this::adaptPathForWindows);
    }
    return original;
}
```

### 3. Error Handling

```java
// Robust WASI test execution
private TestExecutionResult executeWasiTestRobustly(final WasiTestCase testCase,
                                                   final RuntimeType runtime) {
    try (WasiTestEnvironment env = createTestEnvironment(testCase, runtime)) {
        return executeWasiTest(testCase, runtime, env);
    } catch (WasiSecurityException e) {
        return createSecurityViolationResult(testCase, runtime, e);
    } catch (WasiTimeoutException e) {
        return createTimeoutResult(testCase, runtime, e);
    } catch (Exception e) {
        return createUnexpectedErrorResult(testCase, runtime, e);
    }
}
```

## Next Steps

- Review [Extension Guide](extending-analysis-framework.md) for framework integration
- Check out [Custom Test Integration](custom-test-integration.md) for general test patterns
- Explore [Example WASI Tests](../examples/wasi-tests/) for working examples
- Read [API Reference](api-reference.md) for detailed interface documentation