package ai.tegmentum.wasmtime4j.maven;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.dev.ModuleInspector;
import ai.tegmentum.wasmtime4j.dev.ModuleOptimizer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

/**
 * Maven goal for analyzing WebAssembly modules during build.
 * Provides comprehensive module inspection, validation, and optimization recommendations.
 */
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.VERIFY)
public final class WasmAnalyzeMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Directory containing WebAssembly modules to analyze.
     */
    @Parameter(property = "wasmtime4j.analyze.moduleDirectory", defaultValue = "${project.build.directory}/wasm")
    private File moduleDirectory;

    /**
     * File patterns to include in analysis.
     */
    @Parameter(property = "wasmtime4j.analyze.includes")
    private List<String> includes = List.of("**/*.wasm", "**/*.wat");

    /**
     * File patterns to exclude from analysis.
     */
    @Parameter(property = "wasmtime4j.analyze.excludes")
    private List<String> excludes = new ArrayList<>();

    /**
     * Whether to fail the build on analysis errors.
     */
    @Parameter(property = "wasmtime4j.analyze.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Whether to perform security analysis.
     */
    @Parameter(property = "wasmtime4j.analyze.security", defaultValue = "true")
    private boolean performSecurityAnalysis;

    /**
     * Whether to generate optimization recommendations.
     */
    @Parameter(property = "wasmtime4j.analyze.optimize", defaultValue = "true")
    private boolean generateOptimizationRecommendations;

    /**
     * Output directory for analysis reports.
     */
    @Parameter(property = "wasmtime4j.analyze.outputDirectory", defaultValue = "${project.build.directory}/wasm-analysis")
    private File outputDirectory;

    /**
     * Report format (JSON, XML, HTML).
     */
    @Parameter(property = "wasmtime4j.analyze.reportFormat", defaultValue = "JSON")
    private String reportFormat;

    /**
     * Whether to skip the analysis.
     */
    @Parameter(property = "wasmtime4j.analyze.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("WebAssembly module analysis skipped");
            return;
        }

        if (!moduleDirectory.exists()) {
            getLog().warn("Module directory does not exist: " + moduleDirectory.getAbsolutePath());
            return;
        }

        try {
            getLog().info("Analyzing WebAssembly modules in: " + moduleDirectory.getAbsolutePath());

            final Engine engine = Engine.newEngine();
            final List<Path> moduleFiles = findModuleFiles();

            if (moduleFiles.isEmpty()) {
                getLog().info("No WebAssembly modules found for analysis");
                return;
            }

            getLog().info("Found " + moduleFiles.size() + " module(s) to analyze");

            // Create output directory
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory: " + outputDirectory);
            }

            final List<ModuleAnalysisResult> results = new ArrayList<>();
            boolean hasErrors = false;

            for (final Path moduleFile : moduleFiles) {
                try {
                    getLog().info("Analyzing module: " + moduleFile.getFileName());
                    final ModuleAnalysisResult result = analyzeModule(engine, moduleFile);
                    results.add(result);

                    if (result.hasErrors()) {
                        hasErrors = true;
                        getLog().error("Analysis found errors in module: " + moduleFile.getFileName());
                        for (final String error : result.getErrors()) {
                            getLog().error("  " + error);
                        }
                    }

                    if (result.hasWarnings()) {
                        for (final String warning : result.getWarnings()) {
                            getLog().warn("  " + warning);
                        }
                    }

                } catch (final Exception e) {
                    hasErrors = true;
                    final String message = "Failed to analyze module: " + moduleFile.getFileName() + " - " + e.getMessage();
                    getLog().error(message, e);

                    if (failOnError) {
                        throw new MojoExecutionException(message, e);
                    }
                }
            }

            // Generate combined report
            generateAnalysisReport(results);

            if (hasErrors && failOnError) {
                throw new MojoFailureException("WebAssembly module analysis found errors");
            }

            getLog().info("WebAssembly module analysis completed successfully");

        } catch (final Exception e) {
            if (e instanceof MojoExecutionException || e instanceof MojoFailureException) {
                throw e;
            }
            throw new MojoExecutionException("WebAssembly module analysis failed", e);
        }
    }

    private List<Path> findModuleFiles() throws IOException {
        final List<Path> moduleFiles = new ArrayList<>();

        Files.walk(moduleDirectory.toPath())
            .filter(Files::isRegularFile)
            .filter(this::matchesIncludes)
            .filter(this::doesNotMatchExcludes)
            .forEach(moduleFiles::add);

        return moduleFiles;
    }

    private boolean matchesIncludes(final Path file) {
        if (includes.isEmpty()) {
            return true;
        }

        final String fileName = file.getFileName().toString();
        return includes.stream().anyMatch(pattern -> matchesPattern(fileName, pattern));
    }

    private boolean doesNotMatchExcludes(final Path file) {
        if (excludes.isEmpty()) {
            return true;
        }

        final String fileName = file.getFileName().toString();
        return excludes.stream().noneMatch(pattern -> matchesPattern(fileName, pattern));
    }

    private boolean matchesPattern(final String fileName, final String pattern) {
        // Simple pattern matching - real implementation would use more sophisticated matching
        if (pattern.startsWith("**/*.")) {
            final String extension = pattern.substring(4);
            return fileName.endsWith(extension);
        }
        return fileName.matches(pattern.replace("*", ".*"));
    }

    private ModuleAnalysisResult analyzeModule(final Engine engine, final Path moduleFile) throws Exception {
        final byte[] moduleBytes = Files.readAllBytes(moduleFile);
        final Module module = Module.fromBinary(engine, moduleBytes);

        final ModuleInspector inspector = new ModuleInspector(module);
        final ModuleInspector.ModuleAnalysis analysis = inspector.analyze();
        final ModuleInspector.ModuleValidationReport validation = inspector.validate();

        final List<String> errors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<String> recommendations = new ArrayList<>();

        // Process validation issues
        for (final ModuleInspector.ValidationIssue issue : validation.getIssues()) {
            switch (issue.getSeverity()) {
                case HIGH:
                case CRITICAL:
                    errors.add(issue.getMessage() + ": " + issue.getDescription());
                    break;
                case MEDIUM:
                    warnings.add(issue.getMessage() + ": " + issue.getDescription());
                    break;
                case LOW:
                    // Info level - don't add to warnings
                    break;
            }
        }

        // Generate optimization recommendations if enabled
        if (generateOptimizationRecommendations) {
            final ModuleOptimizer optimizer = new ModuleOptimizer(engine);
            final ModuleOptimizer.OptimizationAnalysis optimization = optimizer.analyzeModule(module);

            for (final ModuleOptimizer.OptimizationOpportunity opportunity : optimization.getOpportunities()) {
                recommendations.add(opportunity.getDescription() + " (Impact: " + opportunity.getExpectedBenefit() + ")");
            }
        }

        // Security analysis if enabled
        if (performSecurityAnalysis) {
            final ModuleInspector.SecurityAnalysis security = analysis.getSecurity();
            if (security.getRiskScore() > 5) {
                warnings.add("High security risk score: " + security.getRiskScore());
            }
            if (security.hasUnboundedLoops()) {
                warnings.add("Module contains potentially unbounded loops");
            }
            if (security.hasLargeMemoryAccess()) {
                warnings.add("Module performs large memory operations");
            }
        }

        return new ModuleAnalysisResult(
            moduleFile,
            analysis,
            validation,
            errors,
            warnings,
            recommendations
        );
    }

    private void generateAnalysisReport(final List<ModuleAnalysisResult> results) throws IOException {
        final String reportFileName = "wasm-analysis-report." + reportFormat.toLowerCase();
        final Path reportFile = outputDirectory.toPath().resolve(reportFileName);

        switch (reportFormat.toUpperCase()) {
            case "JSON":
                generateJsonReport(results, reportFile);
                break;
            case "XML":
                generateXmlReport(results, reportFile);
                break;
            case "HTML":
                generateHtmlReport(results, reportFile);
                break;
            default:
                throw new IllegalArgumentException("Unsupported report format: " + reportFormat);
        }

        getLog().info("Analysis report generated: " + reportFile.toAbsolutePath());
    }

    private void generateJsonReport(final List<ModuleAnalysisResult> results, final Path reportFile) throws IOException {
        final StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(java.time.Instant.now()).append("\",\n");
        json.append("  \"project\": \"").append(project.getArtifactId()).append("\",\n");
        json.append("  \"version\": \"").append(project.getVersion()).append("\",\n");
        json.append("  \"totalModules\": ").append(results.size()).append(",\n");
        json.append("  \"modules\": [\n");

        for (int i = 0; i < results.size(); i++) {
            final ModuleAnalysisResult result = results.get(i);
            json.append("    {\n");
            json.append("      \"file\": \"").append(result.getModuleFile().getFileName()).append("\",\n");
            json.append("      \"size\": ").append(result.getAnalysis().getSize().getTotalSize()).append(",\n");
            json.append("      \"functions\": ").append(result.getAnalysis().getFunctions().size()).append(",\n");
            json.append("      \"exports\": ").append(result.getAnalysis().getExports().size()).append(",\n");
            json.append("      \"imports\": ").append(result.getAnalysis().getImports().size()).append(",\n");
            json.append("      \"errors\": ").append(result.getErrors().size()).append(",\n");
            json.append("      \"warnings\": ").append(result.getWarnings().size()).append(",\n");
            json.append("      \"recommendations\": ").append(result.getRecommendations().size()).append("\n");
            json.append("    }");
            if (i < results.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        Files.write(reportFile, json.toString().getBytes());
    }

    private void generateXmlReport(final List<ModuleAnalysisResult> results, final Path reportFile) throws IOException {
        final StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<wasmAnalysisReport>\n");
        xml.append("  <metadata>\n");
        xml.append("    <timestamp>").append(java.time.Instant.now()).append("</timestamp>\n");
        xml.append("    <project>").append(project.getArtifactId()).append("</project>\n");
        xml.append("    <version>").append(project.getVersion()).append("</version>\n");
        xml.append("    <totalModules>").append(results.size()).append("</totalModules>\n");
        xml.append("  </metadata>\n");
        xml.append("  <modules>\n");

        for (final ModuleAnalysisResult result : results) {
            xml.append("    <module>\n");
            xml.append("      <file>").append(result.getModuleFile().getFileName()).append("</file>\n");
            xml.append("      <size>").append(result.getAnalysis().getSize().getTotalSize()).append("</size>\n");
            xml.append("      <functions>").append(result.getAnalysis().getFunctions().size()).append("</functions>\n");
            xml.append("      <exports>").append(result.getAnalysis().getExports().size()).append("</exports>\n");
            xml.append("      <imports>").append(result.getAnalysis().getImports().size()).append("</imports>\n");
            xml.append("      <errors>").append(result.getErrors().size()).append("</errors>\n");
            xml.append("      <warnings>").append(result.getWarnings().size()).append("</warnings>\n");
            xml.append("      <recommendations>").append(result.getRecommendations().size()).append("</recommendations>\n");
            xml.append("    </module>\n");
        }

        xml.append("  </modules>\n");
        xml.append("</wasmAnalysisReport>\n");

        Files.write(reportFile, xml.toString().getBytes());
    }

    private void generateHtmlReport(final List<ModuleAnalysisResult> results, final Path reportFile) throws IOException {
        final StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <title>WebAssembly Module Analysis Report</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; }\n");
        html.append("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("    th { background-color: #f2f2f2; }\n");
        html.append("    .error { color: red; }\n");
        html.append("    .warning { color: orange; }\n");
        html.append("    .success { color: green; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <h1>WebAssembly Module Analysis Report</h1>\n");
        html.append("  <p><strong>Project:</strong> ").append(project.getArtifactId()).append("</p>\n");
        html.append("  <p><strong>Version:</strong> ").append(project.getVersion()).append("</p>\n");
        html.append("  <p><strong>Generated:</strong> ").append(java.time.Instant.now()).append("</p>\n");
        html.append("  <h2>Module Summary</h2>\n");
        html.append("  <table>\n");
        html.append("    <tr>\n");
        html.append("      <th>Module</th>\n");
        html.append("      <th>Size (bytes)</th>\n");
        html.append("      <th>Functions</th>\n");
        html.append("      <th>Exports</th>\n");
        html.append("      <th>Imports</th>\n");
        html.append("      <th>Status</th>\n");
        html.append("    </tr>\n");

        for (final ModuleAnalysisResult result : results) {
            html.append("    <tr>\n");
            html.append("      <td>").append(result.getModuleFile().getFileName()).append("</td>\n");
            html.append("      <td>").append(result.getAnalysis().getSize().getTotalSize()).append("</td>\n");
            html.append("      <td>").append(result.getAnalysis().getFunctions().size()).append("</td>\n");
            html.append("      <td>").append(result.getAnalysis().getExports().size()).append("</td>\n");
            html.append("      <td>").append(result.getAnalysis().getImports().size()).append("</td>\n");

            if (result.hasErrors()) {
                html.append("      <td class=\"error\">").append(result.getErrors().size()).append(" errors</td>\n");
            } else if (result.hasWarnings()) {
                html.append("      <td class=\"warning\">").append(result.getWarnings().size()).append(" warnings</td>\n");
            } else {
                html.append("      <td class=\"success\">OK</td>\n");
            }

            html.append("    </tr>\n");
        }

        html.append("  </table>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        Files.write(reportFile, html.toString().getBytes());
    }

    /**
     * Analysis result for a single module.
     */
    private static final class ModuleAnalysisResult {
        private final Path moduleFile;
        private final ModuleInspector.ModuleAnalysis analysis;
        private final ModuleInspector.ModuleValidationReport validation;
        private final List<String> errors;
        private final List<String> warnings;
        private final List<String> recommendations;

        public ModuleAnalysisResult(final Path moduleFile,
                                   final ModuleInspector.ModuleAnalysis analysis,
                                   final ModuleInspector.ModuleValidationReport validation,
                                   final List<String> errors,
                                   final List<String> warnings,
                                   final List<String> recommendations) {
            this.moduleFile = moduleFile;
            this.analysis = analysis;
            this.validation = validation;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
            this.recommendations = new ArrayList<>(recommendations);
        }

        public Path getModuleFile() { return moduleFile; }
        public ModuleInspector.ModuleAnalysis getAnalysis() { return analysis; }
        public ModuleInspector.ModuleValidationReport getValidation() { return validation; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getRecommendations() { return recommendations; }

        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
    }
}