package ai.tegmentum.wasmtime4j.testsuite;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Engine for discovering WebAssembly test cases from various sources.
 * Supports official WebAssembly specification tests, Wasmtime tests, and custom Java-specific tests.
 */
public final class TestDiscoveryEngine {

    private static final Logger LOGGER = Logger.getLogger(TestDiscoveryEngine.class.getName());

    private static final Pattern WASM_FILE_PATTERN = Pattern.compile(".*\\.wasm$");
    private static final Pattern WAT_FILE_PATTERN = Pattern.compile(".*\\.wat$");
    private static final Pattern JSON_FILE_PATTERN = Pattern.compile(".*\\.json$");

    private final TestSuiteConfiguration configuration;
    private final ConcurrentMap<String, WebAssemblyTestCase> discoveredTestsCache;

    public TestDiscoveryEngine(final TestSuiteConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        this.configuration = configuration;
        this.discoveredTestsCache = new ConcurrentHashMap<>();
    }

    /**
     * Discovers official WebAssembly specification tests.
     *
     * @return collection of discovered spec tests
     * @throws TestSuiteException if discovery fails
     */
    public Collection<WebAssemblyTestCase> discoverOfficialSpecTests() throws TestSuiteException {
        LOGGER.info("Discovering official WebAssembly specification tests");

        try {
            final Path specTestsDir = configuration.getTestSuiteBaseDirectory()
                .resolve("spec-tests");

            if (!Files.exists(specTestsDir)) {
                LOGGER.warning("Official spec tests directory not found: " + specTestsDir);
                return List.of();
            }

            final List<WebAssemblyTestCase> specTests = new ArrayList<>();

            // Discover core WebAssembly tests
            specTests.addAll(discoverSpecTestsInDirectory(
                specTestsDir.resolve("core"), TestCategory.SPEC_CORE));

            // Discover proposal tests
            final Path proposalsDir = specTestsDir.resolve("proposals");
            if (Files.exists(proposalsDir)) {
                try (final DirectoryStream<Path> proposalDirs =
                         Files.newDirectoryStream(proposalsDir, Files::isDirectory)) {
                    for (final Path proposalDir : proposalDirs) {
                        final String proposalName = proposalDir.getFileName().toString();
                        specTests.addAll(discoverSpecTestsInDirectory(
                            proposalDir, TestCategory.fromProposalName(proposalName)));
                    }
                }
            }

            LOGGER.info("Discovered " + specTests.size() + " official specification tests");
            return specTests;

        } catch (final IOException e) {
            throw new TestSuiteException("Failed to discover official spec tests", e);
        }
    }

    /**
     * Discovers Wasmtime-specific tests.
     *
     * @return collection of discovered Wasmtime tests
     * @throws TestSuiteException if discovery fails
     */
    public Collection<WebAssemblyTestCase> discoverWasmtimeTests() throws TestSuiteException {
        LOGGER.info("Discovering Wasmtime-specific tests");

        try {
            final Path wasmtimeTestsDir = configuration.getTestSuiteBaseDirectory()
                .resolve("wasmtime-tests");

            if (!Files.exists(wasmtimeTestsDir)) {
                LOGGER.warning("Wasmtime tests directory not found: " + wasmtimeTestsDir);
                return List.of();
            }

            final List<WebAssemblyTestCase> wasmtimeTests = new ArrayList<>();

            // Discover regression tests
            wasmtimeTests.addAll(discoverTestsInDirectory(
                wasmtimeTestsDir.resolve("regression"), TestCategory.WASMTIME_REGRESSION));

            // Discover performance tests
            wasmtimeTests.addAll(discoverTestsInDirectory(
                wasmtimeTestsDir.resolve("performance"), TestCategory.WASMTIME_PERFORMANCE));

            // Discover WASI tests
            wasmtimeTests.addAll(discoverTestsInDirectory(
                wasmtimeTestsDir.resolve("wasi"), TestCategory.WASI));

            // Discover component model tests
            wasmtimeTests.addAll(discoverTestsInDirectory(
                wasmtimeTestsDir.resolve("component"), TestCategory.COMPONENT_MODEL));

            LOGGER.info("Discovered " + wasmtimeTests.size() + " Wasmtime-specific tests");
            return wasmtimeTests;

        } catch (final IOException e) {
            throw new TestSuiteException("Failed to discover Wasmtime tests", e);
        }
    }

    /**
     * Discovers custom Java-specific tests.
     *
     * @return collection of discovered custom tests
     * @throws TestSuiteException if discovery fails
     */
    public Collection<WebAssemblyTestCase> discoverCustomTests() throws TestSuiteException {
        LOGGER.info("Discovering custom Java-specific tests");

        try {
            final Path customTestsDir = configuration.getTestSuiteBaseDirectory()
                .resolve("custom-tests");

            if (!Files.exists(customTestsDir)) {
                LOGGER.warning("Custom tests directory not found: " + customTestsDir);
                return List.of();
            }

            final List<WebAssemblyTestCase> customTests = new ArrayList<>();

            // Discover JNI-specific tests
            customTests.addAll(discoverTestsInDirectory(
                customTestsDir.resolve("jni"), TestCategory.JAVA_JNI));

            // Discover Panama-specific tests
            customTests.addAll(discoverTestsInDirectory(
                customTestsDir.resolve("panama"), TestCategory.JAVA_PANAMA));

            // Discover interop tests
            customTests.addAll(discoverTestsInDirectory(
                customTestsDir.resolve("interop"), TestCategory.JAVA_INTEROP));

            // Discover memory management tests
            customTests.addAll(discoverTestsInDirectory(
                customTestsDir.resolve("memory"), TestCategory.JAVA_MEMORY));

            // Discover performance tests
            customTests.addAll(discoverTestsInDirectory(
                customTestsDir.resolve("performance"), TestCategory.JAVA_PERFORMANCE));

            LOGGER.info("Discovered " + customTests.size() + " custom Java-specific tests");
            return customTests;

        } catch (final IOException e) {
            throw new TestSuiteException("Failed to discover custom tests", e);
        }
    }

    private Collection<WebAssemblyTestCase> discoverSpecTestsInDirectory(
            final Path directory, final TestCategory category) throws IOException {

        if (!Files.exists(directory)) {
            return List.of();
        }

        final List<WebAssemblyTestCase> tests = new ArrayList<>();

        try (final Stream<Path> files = Files.walk(directory)) {
            final List<Path> jsonFiles = files
                .filter(Files::isRegularFile)
                .filter(path -> JSON_FILE_PATTERN.matcher(path.getFileName().toString()).matches())
                .collect(Collectors.toList());

            for (final Path jsonFile : jsonFiles) {
                try {
                    final WebAssemblyTestCase testCase = createSpecTestCaseFromJson(jsonFile, category);
                    if (testCase != null) {
                        tests.add(testCase);
                        cacheDiscoveredTest(testCase);
                    }
                } catch (final Exception e) {
                    LOGGER.warning("Failed to create test case from " + jsonFile + ": " + e.getMessage());
                }
            }
        }

        return tests;
    }

    private Collection<WebAssemblyTestCase> discoverTestsInDirectory(
            final Path directory, final TestCategory category) throws IOException {

        if (!Files.exists(directory)) {
            return List.of();
        }

        final List<WebAssemblyTestCase> tests = new ArrayList<>();

        try (final Stream<Path> files = Files.walk(directory)) {
            final List<Path> wasmFiles = files
                .filter(Files::isRegularFile)
                .filter(path -> {
                    final String fileName = path.getFileName().toString();
                    return WASM_FILE_PATTERN.matcher(fileName).matches() ||
                           WAT_FILE_PATTERN.matcher(fileName).matches();
                })
                .collect(Collectors.toList());

            for (final Path wasmFile : wasmFiles) {
                try {
                    final WebAssemblyTestCase testCase = createTestCaseFromWasmFile(wasmFile, category);
                    if (testCase != null) {
                        tests.add(testCase);
                        cacheDiscoveredTest(testCase);
                    }
                } catch (final Exception e) {
                    LOGGER.warning("Failed to create test case from " + wasmFile + ": " + e.getMessage());
                }
            }
        }

        return tests;
    }

    private WebAssemblyTestCase createSpecTestCaseFromJson(
            final Path jsonFile, final TestCategory category) throws TestSuiteException {

        try {
            final WebAssemblySpecTestParser parser = new WebAssemblySpecTestParser();
            final List<WebAssemblyTestCase> testCases = parser.parseSpecTestFile(jsonFile);

            if (testCases.isEmpty()) {
                return null;
            }

            // For spec tests, create a composite test case that includes all sub-tests
            return WebAssemblyTestCase.builder()
                .testId(generateTestId(jsonFile))
                .testName(jsonFile.getFileName().toString().replaceAll("\\.json$", ""))
                .category(category)
                .testFilePath(jsonFile)
                .description("WebAssembly specification test suite: " + jsonFile.getFileName())
                .subTests(testCases)
                .expected(TestExpectedResult.PASS) // Default expectation
                .tags(List.of("spec", category.name().toLowerCase()))
                .build();

        } catch (final Exception e) {
            throw new TestSuiteException("Failed to create spec test case from " + jsonFile, e);
        }
    }

    private WebAssemblyTestCase createTestCaseFromWasmFile(
            final Path wasmFile, final TestCategory category) throws TestSuiteException {

        try {
            final String fileName = wasmFile.getFileName().toString();
            final String testName = fileName.replaceAll("\\.(wasm|wat)$", "");

            return WebAssemblyTestCase.builder()
                .testId(generateTestId(wasmFile))
                .testName(testName)
                .category(category)
                .testFilePath(wasmFile)
                .description("WebAssembly test: " + testName)
                .expected(inferExpectedResult(wasmFile, category))
                .tags(generateTestTags(wasmFile, category))
                .build();

        } catch (final Exception e) {
            throw new TestSuiteException("Failed to create test case from " + wasmFile, e);
        }
    }

    private String generateTestId(final Path testFile) {
        final String relativePath = configuration.getTestSuiteBaseDirectory()
            .relativize(testFile)
            .toString();
        return relativePath.replaceAll("[/\\\\]", "_")
            .replaceAll("\\.(wasm|wat|json)$", "");
    }

    private TestExpectedResult inferExpectedResult(final Path testFile, final TestCategory category) {
        final String fileName = testFile.getFileName().toString().toLowerCase();

        // Infer expected result based on file naming conventions
        if (fileName.contains("invalid") || fileName.contains("fail") || fileName.contains("error")) {
            return TestExpectedResult.FAIL;
        }
        if (fileName.contains("trap") || fileName.contains("unreachable")) {
            return TestExpectedResult.TRAP;
        }
        if (fileName.contains("timeout") || fileName.contains("infinite")) {
            return TestExpectedResult.TIMEOUT;
        }

        return TestExpectedResult.PASS;
    }

    private List<String> generateTestTags(final Path testFile, final TestCategory category) {
        final List<String> tags = new ArrayList<>();
        tags.add(category.name().toLowerCase());

        final String fileName = testFile.getFileName().toString().toLowerCase();
        final String parentDirName = testFile.getParent().getFileName().toString().toLowerCase();

        // Add feature-specific tags based on file path
        if (fileName.contains("memory") || parentDirName.contains("memory")) {
            tags.add("memory");
        }
        if (fileName.contains("func") || parentDirName.contains("func")) {
            tags.add("function");
        }
        if (fileName.contains("table") || parentDirName.contains("table")) {
            tags.add("table");
        }
        if (fileName.contains("global") || parentDirName.contains("global")) {
            tags.add("global");
        }
        if (fileName.contains("import") || parentDirName.contains("import")) {
            tags.add("import");
        }
        if (fileName.contains("export") || parentDirName.contains("export")) {
            tags.add("export");
        }
        if (fileName.contains("simd") || parentDirName.contains("simd")) {
            tags.add("simd");
        }
        if (fileName.contains("thread") || parentDirName.contains("thread")) {
            tags.add("threading");
        }
        if (fileName.contains("atomic") || parentDirName.contains("atomic")) {
            tags.add("atomic");
        }

        return tags;
    }

    private void cacheDiscoveredTest(final WebAssemblyTestCase testCase) {
        discoveredTestsCache.put(testCase.getTestId(), testCase);
    }

    /**
     * Gets a previously discovered test case by ID.
     *
     * @param testId test case ID
     * @return test case if found, null otherwise
     */
    public WebAssemblyTestCase getCachedTestCase(final String testId) {
        return discoveredTestsCache.get(testId);
    }

    /**
     * Gets all cached discovered test cases.
     *
     * @return collection of cached test cases
     */
    public Collection<WebAssemblyTestCase> getAllCachedTests() {
        return List.copyOf(discoveredTestsCache.values());
    }

    /**
     * Clears the discovered tests cache.
     */
    public void clearCache() {
        discoveredTestsCache.clear();
    }
}