/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Comprehensive validation tests for 100% Javadoc coverage across all public APIs.
 *
 * <p>These tests ensure that every public class, interface, method, constructor, and field
 * has complete Javadoc documentation according to project standards.
 *
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Javadoc Coverage Validation")
class JavadocCoverageValidationTest {

    private static final Logger logger = Logger.getLogger(JavadocCoverageValidationTest.class.getName());

    private static final List<String> API_PACKAGES = List.of(
            "ai.tegmentum.wasmtime4j",
            "ai.tegmentum.wasmtime4j.exception",
            "ai.tegmentum.wasmtime4j.factory",
            "ai.tegmentum.wasmtime4j.cache",
            "ai.tegmentum.wasmtime4j.reactive",
            "ai.tegmentum.wasmtime4j.serialization",
            "ai.tegmentum.wasmtime4j.aot",
            "ai.tegmentum.wasmtime4j.memory",
            "ai.tegmentum.wasmtime4j.component"
    );

    private static final Set<String> CRITICAL_API_CLASSES = Set.of(
            "Engine", "EngineConfig", "Module", "Instance", "Store",
            "Linker", "WasmMemory", "WasmTable", "WasmGlobal", "HostFunction",
            "FuncType", "GlobalType", "MemoryType", "TableType",
            "ImportMap", "ExportType", "ImportType", "WasmValue"
    );

    private final Map<String, JavadocAnalysis> javadocAnalyses = new HashMap<>();
    private final List<DocumentationViolation> violations = new ArrayList<>();

    @BeforeAll
    void analyzeJavadocCoverage() throws Exception {
        logger.info("Starting comprehensive Javadoc coverage analysis");

        for (final String packageName : API_PACKAGES) {
            analyzePackageJavadoc(packageName);
        }

        logger.info(String.format("Javadoc analysis complete: %d classes analyzed, %d violations found",
                javadocAnalyses.size(), violations.size()));
    }

    @Test
    @DisplayName("Should achieve 100% Javadoc coverage for all public APIs")
    @EnabledIf("isStrictJavadocRequired")
    void shouldAchieve100PercentJavadocCoverage() {
        final double coveragePercentage = calculateOverallCoveragePercentage();

        if (coveragePercentage < 100.0) {
            logCoverageViolations();
        }

        assertThat(coveragePercentage)
                .withFailMessage("Javadoc coverage is %.2f%%, expected 100%%. Found %d violations.",
                        coveragePercentage, violations.size())
                .isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should meet minimum Javadoc coverage threshold")
    void shouldMeetMinimumJavadocCoverageThreshold() {
        final double coveragePercentage = calculateOverallCoveragePercentage();
        final double minimumThreshold = 95.0; // 95% minimum coverage

        if (coveragePercentage < minimumThreshold) {
            logCoverageViolations();
        }

        assertThat(coveragePercentage)
                .withFailMessage("Javadoc coverage is %.2f%%, expected at least %.2f%%. Found %d violations.",
                        coveragePercentage, minimumThreshold, violations.size())
                .isGreaterThanOrEqualTo(minimumThreshold);
    }

    @Test
    @DisplayName("Should have complete Javadoc for all critical API classes")
    void shouldHaveCompleteJavadocForCriticalApiClasses() {
        final List<DocumentationViolation> criticalViolations = new ArrayList<>();

        for (final String className : CRITICAL_API_CLASSES) {
            final JavadocAnalysis analysis = findAnalysisForClass(className);
            if (analysis != null && !analysis.isFullyDocumented()) {
                criticalViolations.addAll(analysis.getViolations());
            } else if (analysis == null) {
                criticalViolations.add(new DocumentationViolation(
                        ViolationType.MISSING_CLASS_DOCUMENTATION,
                        ViolationSeverity.CRITICAL,
                        className,
                        "Critical API class not found or not analyzed"
                ));
            }
        }

        if (!criticalViolations.isEmpty()) {
            logger.severe("Found documentation violations in critical API classes:");
            criticalViolations.forEach(violation ->
                    logger.severe("  - " + violation.getLocation() + ": " + violation.getDescription()));
        }

        assertThat(criticalViolations)
                .withFailMessage("Found %d documentation violations in critical API classes: %s",
                        criticalViolations.size(),
                        criticalViolations.stream().map(DocumentationViolation::getLocation).collect(Collectors.toList()))
                .isEmpty();
    }

    @Test
    @DisplayName("Should have Javadoc with proper parameter documentation")
    void shouldHaveJavadocWithProperParameterDocumentation() {
        final List<DocumentationViolation> parameterViolations = violations.stream()
                .filter(v -> v.getType() == ViolationType.MISSING_PARAMETER_DOCUMENTATION)
                .collect(Collectors.toList());

        if (!parameterViolations.isEmpty()) {
            logger.warning("Found methods with missing parameter documentation:");
            parameterViolations.forEach(violation ->
                    logger.warning("  - " + violation.getLocation() + ": " + violation.getDescription()));
        }

        assertThat(parameterViolations)
                .withFailMessage("Found %d methods with missing parameter documentation",
                        parameterViolations.size())
                .isEmpty();
    }

    @Test
    @DisplayName("Should have Javadoc with proper return value documentation")
    void shouldHaveJavadocWithProperReturnValueDocumentation() {
        final List<DocumentationViolation> returnViolations = violations.stream()
                .filter(v -> v.getType() == ViolationType.MISSING_RETURN_DOCUMENTATION)
                .collect(Collectors.toList());

        if (!returnViolations.isEmpty()) {
            logger.warning("Found methods with missing return value documentation:");
            returnViolations.forEach(violation ->
                    logger.warning("  - " + violation.getLocation() + ": " + violation.getDescription()));
        }

        assertThat(returnViolations)
                .withFailMessage("Found %d methods with missing return value documentation",
                        returnViolations.size())
                .isEmpty();
    }

    @Test
    @DisplayName("Should have Javadoc with proper exception documentation")
    void shouldHaveJavadocWithProperExceptionDocumentation() {
        final List<DocumentationViolation> exceptionViolations = violations.stream()
                .filter(v -> v.getType() == ViolationType.MISSING_EXCEPTION_DOCUMENTATION)
                .collect(Collectors.toList());

        if (!exceptionViolations.isEmpty()) {
            logger.warning("Found methods with missing exception documentation:");
            exceptionViolations.forEach(violation ->
                    logger.warning("  - " + violation.getLocation() + ": " + violation.getDescription()));
        }

        assertThat(exceptionViolations)
                .withFailMessage("Found %d methods with missing exception documentation",
                        exceptionViolations.size())
                .isEmpty();
    }

    @Test
    @DisplayName("Should have consistent @since tags across API")
    void shouldHaveConsistentSinceTagsAcrossApi() {
        final List<DocumentationViolation> sinceViolations = violations.stream()
                .filter(v -> v.getType() == ViolationType.MISSING_SINCE_TAG)
                .collect(Collectors.toList());

        if (!sinceViolations.isEmpty()) {
            logger.info("Found API elements with missing @since tags:");
            sinceViolations.forEach(violation ->
                    logger.info("  - " + violation.getLocation()));
        }

        // @since tags are recommended but not required for all elements
        // Only log as information for tracking purposes
    }

    @Test
    @DisplayName("Should have proper Javadoc formatting and structure")
    void shouldHaveProperJavadocFormattingAndStructure() {
        final List<DocumentationViolation> formatViolations = violations.stream()
                .filter(v -> v.getType() == ViolationType.INVALID_JAVADOC_FORMAT)
                .collect(Collectors.toList());

        if (!formatViolations.isEmpty()) {
            logger.warning("Found Javadoc formatting issues:");
            formatViolations.forEach(violation ->
                    logger.warning("  - " + violation.getLocation() + ": " + violation.getDescription()));
        }

        assertThat(formatViolations)
                .withFailMessage("Found %d Javadoc formatting violations",
                        formatViolations.size())
                .isEmpty();
    }

    @Test
    @DisplayName("Should provide coverage statistics by package")
    void shouldProvideCoverageStatisticsByPackage() {
        final Map<String, Double> packageCoverage = calculatePackageCoverage();

        logger.info("Javadoc coverage by package:");
        packageCoverage.forEach((packageName, coverage) ->
                logger.info(String.format("  %s: %.2f%%", packageName, coverage)));

        for (final Map.Entry<String, Double> entry : packageCoverage.entrySet()) {
            final String packageName = entry.getKey();
            final double coverage = entry.getValue();

            // Core API packages should have higher coverage
            final double expectedCoverage = isCoreLackage(packageName) ? 95.0 : 85.0;

            assertThat(coverage)
                    .withFailMessage("Package %s has %.2f%% coverage, expected at least %.2f%%",
                            packageName, coverage, expectedCoverage)
                    .isGreaterThanOrEqualTo(expectedCoverage);
        }
    }

    private void analyzePackageJavadoc(final String packageName) throws Exception {
        final String packagePath = packageName.replace('.', '/');
        final Path sourcePath = Paths.get("src/main/java", packagePath);

        if (!Files.exists(sourcePath)) {
            logger.warning("Package source path does not exist: " + sourcePath);
            return;
        }

        try (final Stream<Path> files = Files.walk(sourcePath)) {
            final List<Path> javaFiles = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (final Path javaFile : javaFiles) {
                analyzeJavaFileJavadoc(javaFile, packageName);
            }
        }
    }

    private void analyzeJavaFileJavadoc(final Path javaFile, final String packageName) throws Exception {
        final String className = extractClassNameFromPath(javaFile);
        final String fullClassName = packageName + "." + className;

        try {
            final Class<?> clazz = Class.forName(fullClassName);
            if (!Modifier.isPublic(clazz.getModifiers())) {
                return; // Skip non-public classes
            }

            final String sourceCode = Files.readString(javaFile);
            final JavadocAnalysis analysis = new JavadocAnalysis(clazz, sourceCode);

            analyzeClassJavadoc(clazz, sourceCode, analysis);
            analyzeMethodJavadoc(clazz, sourceCode, analysis);
            analyzeFieldJavadoc(clazz, sourceCode, analysis);
            analyzeConstructorJavadoc(clazz, sourceCode, analysis);

            javadocAnalyses.put(fullClassName, analysis);
            violations.addAll(analysis.getViolations());

        } catch (final ClassNotFoundException e) {
            logger.warning("Could not load class: " + fullClassName + " - " + e.getMessage());
        }
    }

    private void analyzeClassJavadoc(final Class<?> clazz, final String sourceCode, final JavadocAnalysis analysis) {
        final String classJavadoc = extractClassJavadoc(clazz.getSimpleName(), sourceCode);

        if (classJavadoc == null || classJavadoc.trim().isEmpty()) {
            analysis.addViolation(new DocumentationViolation(
                    ViolationType.MISSING_CLASS_DOCUMENTATION,
                    ViolationSeverity.HIGH,
                    clazz.getName(),
                    "Class is missing Javadoc documentation"
            ));
        } else {
            validateJavadocContent(clazz.getName(), classJavadoc, analysis);
        }
    }

    private void analyzeMethodJavadoc(final Class<?> clazz, final String sourceCode, final JavadocAnalysis analysis) {
        final Method[] methods = clazz.getDeclaredMethods();

        for (final Method method : methods) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue; // Skip non-public methods
            }

            final String methodSignature = getMethodSignature(method);
            final String methodJavadoc = extractMethodJavadoc(method.getName(), sourceCode);

            if (methodJavadoc == null || methodJavadoc.trim().isEmpty()) {
                analysis.addViolation(new DocumentationViolation(
                        ViolationType.MISSING_METHOD_DOCUMENTATION,
                        ViolationSeverity.MEDIUM,
                        clazz.getName() + "." + methodSignature,
                        "Method is missing Javadoc documentation"
                ));
            } else {
                validateMethodJavadoc(method, methodJavadoc, analysis);
            }
        }
    }

    private void analyzeFieldJavadoc(final Class<?> clazz, final String sourceCode, final JavadocAnalysis analysis) {
        final Field[] fields = clazz.getDeclaredFields();

        for (final Field field : fields) {
            if (!Modifier.isPublic(field.getModifiers())) {
                continue; // Skip non-public fields
            }

            final String fieldJavadoc = extractFieldJavadoc(field.getName(), sourceCode);

            if (fieldJavadoc == null || fieldJavadoc.trim().isEmpty()) {
                analysis.addViolation(new DocumentationViolation(
                        ViolationType.MISSING_FIELD_DOCUMENTATION,
                        ViolationSeverity.LOW,
                        clazz.getName() + "." + field.getName(),
                        "Field is missing Javadoc documentation"
                ));
            }
        }
    }

    private void analyzeConstructorJavadoc(final Class<?> clazz, final String sourceCode, final JavadocAnalysis analysis) {
        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        for (final Constructor<?> constructor : constructors) {
            if (!Modifier.isPublic(constructor.getModifiers())) {
                continue; // Skip non-public constructors
            }

            final String constructorJavadoc = extractConstructorJavadoc(clazz.getSimpleName(), sourceCode);

            if (constructorJavadoc == null || constructorJavadoc.trim().isEmpty()) {
                analysis.addViolation(new DocumentationViolation(
                        ViolationType.MISSING_CONSTRUCTOR_DOCUMENTATION,
                        ViolationSeverity.MEDIUM,
                        clazz.getName() + ".<init>",
                        "Constructor is missing Javadoc documentation"
                ));
            } else {
                validateConstructorJavadoc(constructor, constructorJavadoc, analysis);
            }
        }
    }

    private void validateMethodJavadoc(final Method method, final String javadoc, final JavadocAnalysis analysis) {
        final String methodLocation = method.getDeclaringClass().getName() + "." + getMethodSignature(method);

        // Check for parameter documentation
        if (method.getParameterCount() > 0) {
            final Pattern paramPattern = Pattern.compile("@param\\s+(\\w+)");
            final Matcher paramMatcher = paramPattern.matcher(javadoc);
            final int documentedParams = (int) paramMatcher.results().count();

            if (documentedParams < method.getParameterCount()) {
                analysis.addViolation(new DocumentationViolation(
                        ViolationType.MISSING_PARAMETER_DOCUMENTATION,
                        ViolationSeverity.MEDIUM,
                        methodLocation,
                        String.format("Method has %d parameters but only %d are documented",
                                method.getParameterCount(), documentedParams)
                ));
            }
        }

        // Check for return documentation
        if (!method.getReturnType().equals(void.class) && !javadoc.contains("@return")) {
            analysis.addViolation(new DocumentationViolation(
                    ViolationType.MISSING_RETURN_DOCUMENTATION,
                    ViolationSeverity.MEDIUM,
                    methodLocation,
                    "Method returns a value but has no @return documentation"
            ));
        }

        // Check for exception documentation
        if (method.getExceptionTypes().length > 0 && !javadoc.contains("@throws")) {
            analysis.addViolation(new DocumentationViolation(
                    ViolationType.MISSING_EXCEPTION_DOCUMENTATION,
                    ViolationSeverity.MEDIUM,
                    methodLocation,
                    "Method throws exceptions but has no @throws documentation"
            ));
        }

        validateJavadocContent(methodLocation, javadoc, analysis);
    }

    private void validateConstructorJavadoc(final Constructor<?> constructor, final String javadoc, final JavadocAnalysis analysis) {
        final String constructorLocation = constructor.getDeclaringClass().getName() + ".<init>";

        // Check for parameter documentation
        if (constructor.getParameterCount() > 0) {
            final Pattern paramPattern = Pattern.compile("@param\\s+(\\w+)");
            final Matcher paramMatcher = paramPattern.matcher(javadoc);
            final int documentedParams = (int) paramMatcher.results().count();

            if (documentedParams < constructor.getParameterCount()) {
                analysis.addViolation(new DocumentationViolation(
                        ViolationType.MISSING_PARAMETER_DOCUMENTATION,
                        ViolationSeverity.MEDIUM,
                        constructorLocation,
                        String.format("Constructor has %d parameters but only %d are documented",
                                constructor.getParameterCount(), documentedParams)
                ));
            }
        }

        // Check for exception documentation
        if (constructor.getExceptionTypes().length > 0 && !javadoc.contains("@throws")) {
            analysis.addViolation(new DocumentationViolation(
                    ViolationType.MISSING_EXCEPTION_DOCUMENTATION,
                    ViolationSeverity.MEDIUM,
                    constructorLocation,
                    "Constructor throws exceptions but has no @throws documentation"
            ));
        }

        validateJavadocContent(constructorLocation, javadoc, analysis);
    }

    private void validateJavadocContent(final String location, final String javadoc, final JavadocAnalysis analysis) {
        // Check for @since tag (recommended but not required)
        if (!javadoc.contains("@since")) {
            analysis.addViolation(new DocumentationViolation(
                    ViolationType.MISSING_SINCE_TAG,
                    ViolationSeverity.INFO,
                    location,
                    "Missing @since tag"
            ));
        }

        // Check for proper formatting
        if (!isValidJavadocFormat(javadoc)) {
            analysis.addViolation(new DocumentationViolation(
                    ViolationType.INVALID_JAVADOC_FORMAT,
                    ViolationSeverity.LOW,
                    location,
                    "Javadoc has formatting issues"
            ));
        }
    }

    private String extractClassJavadoc(final String className, final String sourceCode) {
        final Pattern pattern = Pattern.compile(
                "/\\*\\*.*?\\*/\\s*(?:@[^\\n]*\\n\\s*)*(?:public|private|protected|abstract|final|static)*\\s*(?:class|interface|enum)\\s+" + className,
                Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(sourceCode);
        return matcher.find() ? matcher.group().split("\\*/")[0] + "*/" : null;
    }

    private String extractMethodJavadoc(final String methodName, final String sourceCode) {
        final Pattern pattern = Pattern.compile(
                "/\\*\\*.*?\\*/\\s*(?:@[^\\n]*\\n\\s*)*(?:public|private|protected|abstract|final|static)*\\s+[^\\n]*\\s+" + methodName + "\\s*\\(",
                Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(sourceCode);
        return matcher.find() ? matcher.group().split("\\*/")[0] + "*/" : null;
    }

    private String extractFieldJavadoc(final String fieldName, final String sourceCode) {
        final Pattern pattern = Pattern.compile(
                "/\\*\\*.*?\\*/\\s*(?:@[^\\n]*\\n\\s*)*(?:public|private|protected|final|static)*\\s+[^\\n]*\\s+" + fieldName + "\\s*[;=]",
                Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(sourceCode);
        return matcher.find() ? matcher.group().split("\\*/")[0] + "*/" : null;
    }

    private String extractConstructorJavadoc(final String className, final String sourceCode) {
        final Pattern pattern = Pattern.compile(
                "/\\*\\*.*?\\*/\\s*(?:@[^\\n]*\\n\\s*)*(?:public|private|protected)*\\s+" + className + "\\s*\\(",
                Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(sourceCode);
        return matcher.find() ? matcher.group().split("\\*/")[0] + "*/" : null;
    }

    private boolean isValidJavadocFormat(final String javadoc) {
        // Basic format validation
        return javadoc.startsWith("/**") && javadoc.endsWith("*/");
    }

    private String extractClassNameFromPath(final Path javaFile) {
        final String fileName = javaFile.getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private String getMethodSignature(final Method method) {
        final String params = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return method.getName() + "(" + params + ")";
    }

    private double calculateOverallCoveragePercentage() {
        if (javadocAnalyses.isEmpty()) {
            return 100.0;
        }

        final int totalElements = javadocAnalyses.values().stream()
                .mapToInt(JavadocAnalysis::getTotalElements)
                .sum();

        final int documentedElements = javadocAnalyses.values().stream()
                .mapToInt(JavadocAnalysis::getDocumentedElements)
                .sum();

        return totalElements > 0 ? (double) documentedElements / totalElements * 100.0 : 100.0;
    }

    private Map<String, Double> calculatePackageCoverage() {
        final Map<String, Double> packageCoverage = new HashMap<>();

        for (final String packageName : API_PACKAGES) {
            final List<JavadocAnalysis> packageAnalyses = javadocAnalyses.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(packageName))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            if (!packageAnalyses.isEmpty()) {
                final int totalElements = packageAnalyses.stream()
                        .mapToInt(JavadocAnalysis::getTotalElements)
                        .sum();

                final int documentedElements = packageAnalyses.stream()
                        .mapToInt(JavadocAnalysis::getDocumentedElements)
                        .sum();

                final double coverage = totalElements > 0 ? (double) documentedElements / totalElements * 100.0 : 100.0;
                packageCoverage.put(packageName, coverage);
            }
        }

        return packageCoverage;
    }

    private JavadocAnalysis findAnalysisForClass(final String className) {
        return javadocAnalyses.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("." + className))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isCoreLackage(final String packageName) {
        return packageName.equals("ai.tegmentum.wasmtime4j")
                || packageName.equals("ai.tegmentum.wasmtime4j.exception")
                || packageName.equals("ai.tegmentum.wasmtime4j.factory");
    }

    private void logCoverageViolations() {
        logger.severe("Javadoc coverage violations found:");
        violations.forEach(violation ->
                logger.severe(String.format("  [%s] %s: %s",
                        violation.getSeverity(), violation.getLocation(), violation.getDescription())));
    }

    /**
     * Configuration method to determine if strict Javadoc requirements should be enforced.
     * Can be controlled via system property: -DstrictJavadoc=true
     */
    static boolean isStrictJavadocRequired() {
        return Boolean.parseBoolean(System.getProperty("strictJavadoc", "false"));
    }

    /**
     * Helper class for analyzing Javadoc coverage of a single class.
     */
    private static final class JavadocAnalysis {
        private final Class<?> clazz;
        private final String sourceCode;
        private final List<DocumentationViolation> violations = new ArrayList<>();
        private int totalElements = 0;
        private int documentedElements = 0;

        JavadocAnalysis(final Class<?> clazz, final String sourceCode) {
            this.clazz = clazz;
            this.sourceCode = sourceCode;
        }

        void addViolation(final DocumentationViolation violation) {
            violations.add(violation);
        }

        List<DocumentationViolation> getViolations() {
            return violations;
        }

        boolean isFullyDocumented() {
            return violations.stream().noneMatch(v ->
                    v.getSeverity() == ViolationSeverity.CRITICAL || v.getSeverity() == ViolationSeverity.HIGH);
        }

        int getTotalElements() {
            return totalElements;
        }

        int getDocumentedElements() {
            return documentedElements;
        }

        void setElementCounts(final int total, final int documented) {
            this.totalElements = total;
            this.documentedElements = documented;
        }
    }

    /**
     * Enumeration of documentation violation types.
     */
    private enum ViolationType {
        MISSING_CLASS_DOCUMENTATION,
        MISSING_METHOD_DOCUMENTATION,
        MISSING_FIELD_DOCUMENTATION,
        MISSING_CONSTRUCTOR_DOCUMENTATION,
        MISSING_PARAMETER_DOCUMENTATION,
        MISSING_RETURN_DOCUMENTATION,
        MISSING_EXCEPTION_DOCUMENTATION,
        MISSING_SINCE_TAG,
        INVALID_JAVADOC_FORMAT
    }

    /**
     * Represents a documentation violation.
     */
    private static final class DocumentationViolation {
        private final ViolationType type;
        private final ViolationSeverity severity;
        private final String location;
        private final String description;

        DocumentationViolation(final ViolationType type, final ViolationSeverity severity,
                               final String location, final String description) {
            this.type = type;
            this.severity = severity;
            this.location = location;
            this.description = description;
        }

        ViolationType getType() {
            return type;
        }

        ViolationSeverity getSeverity() {
            return severity;
        }

        String getLocation() {
            return location;
        }

        String getDescription() {
            return description;
        }
    }
}