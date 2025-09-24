package ai.tegmentum.wasmtime4j.maven;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
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
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Maven goal for optimizing WebAssembly modules during build.
 * Applies various optimization strategies to improve performance and reduce size.
 */
@Mojo(name = "optimize", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public final class WasmOptimizeMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Directory containing WebAssembly modules to optimize.
     */
    @Parameter(property = "wasmtime4j.optimize.sourceDirectory", defaultValue = "${project.build.directory}/wasm")
    private File sourceDirectory;

    /**
     * Output directory for optimized modules.
     */
    @Parameter(property = "wasmtime4j.optimize.outputDirectory", defaultValue = "${project.build.directory}/wasm-optimized")
    private File outputDirectory;

    /**
     * File patterns to include in optimization.
     */
    @Parameter(property = "wasmtime4j.optimize.includes")
    private List<String> includes = List.of("**/*.wasm");

    /**
     * File patterns to exclude from optimization.
     */
    @Parameter(property = "wasmtime4j.optimize.excludes")
    private List<String> excludes = new ArrayList<>();

    /**
     * Optimization strategy to apply.
     */
    @Parameter(property = "wasmtime4j.optimize.strategy", defaultValue = "AUTO")
    private String optimizationStrategy;

    /**
     * Whether to enable dead code elimination.
     */
    @Parameter(property = "wasmtime4j.optimize.deadCodeElimination", defaultValue = "true")
    private boolean enableDeadCodeElimination;

    /**
     * Whether to enable function inlining.
     */
    @Parameter(property = "wasmtime4j.optimize.functionInlining", defaultValue = "true")
    private boolean enableFunctionInlining;

    /**
     * Whether to enable loop optimization.
     */
    @Parameter(property = "wasmtime4j.optimize.loopOptimization", defaultValue = "true")
    private boolean enableLoopOptimization;

    /**
     * Whether to enable constant folding.
     */
    @Parameter(property = "wasmtime4j.optimize.constantFolding", defaultValue = "true")
    private boolean enableConstantFolding;

    /**
     * Maximum number of optimization passes.
     */
    @Parameter(property = "wasmtime4j.optimize.maxPasses", defaultValue = "3")
    private int maxOptimizationPasses;

    /**
     * Whether to backup original modules.
     */
    @Parameter(property = "wasmtime4j.optimize.backup", defaultValue = "true")
    private boolean backupOriginals;

    /**
     * Whether to validate optimized modules.
     */
    @Parameter(property = "wasmtime4j.optimize.validate", defaultValue = "true")
    private boolean validateOptimized;

    /**
     * Whether to perform A/B testing of optimization strategies.
     */
    @Parameter(property = "wasmtime4j.optimize.abTest", defaultValue = "false")
    private boolean performABTesting;

    /**
     * Whether to fail the build on optimization errors.
     */
    @Parameter(property = "wasmtime4j.optimize.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Whether to skip optimization.
     */
    @Parameter(property = "wasmtime4j.optimize.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("WebAssembly module optimization skipped");
            return;
        }

        if (!sourceDirectory.exists()) {
            getLog().warn("Source directory does not exist: " + sourceDirectory.getAbsolutePath());
            return;
        }

        try {
            getLog().info("Optimizing WebAssembly modules in: " + sourceDirectory.getAbsolutePath());

            final Engine engine = Engine.newEngine();
            final List<Path> moduleFiles = findModuleFiles();

            if (moduleFiles.isEmpty()) {
                getLog().info("No WebAssembly modules found for optimization");
                return;
            }

            getLog().info("Found " + moduleFiles.size() + " module(s) to optimize");

            // Create output directory
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory: " + outputDirectory);
            }

            // Create backup directory if enabled
            File backupDirectory = null;
            if (backupOriginals) {
                backupDirectory = new File(outputDirectory.getParentFile(), "wasm-backup");
                if (!backupDirectory.exists() && !backupDirectory.mkdirs()) {
                    throw new MojoExecutionException("Failed to create backup directory: " + backupDirectory);
                }
            }

            final ModuleOptimizer.OptimizationConfiguration config = createOptimizationConfiguration();
            final ModuleOptimizer optimizer = new ModuleOptimizer(engine, config);

            final List<OptimizationResult> results = new ArrayList<>();
            boolean hasErrors = false;

            for (final Path moduleFile : moduleFiles) {
                try {
                    getLog().info("Optimizing module: " + moduleFile.getFileName());

                    final OptimizationResult result = optimizeModule(optimizer, moduleFile, backupDirectory);
                    results.add(result);

                    if (!result.isSuccessful()) {
                        hasErrors = true;
                        getLog().error("Optimization failed for module: " + moduleFile.getFileName());
                        getLog().error("  Error: " + result.getError());

                        if (failOnError) {
                            throw new MojoExecutionException("Module optimization failed: " + result.getError());
                        }
                    } else {
                        getLog().info("Module optimized successfully:");
                        getLog().info("  Size reduction: " + String.format("%.1f%%", result.getSizeReduction()));
                        getLog().info("  Performance improvement: " + String.format("%.1f%%", result.getPerformanceImprovement()));
                    }

                } catch (final Exception e) {
                    hasErrors = true;
                    final String message = "Failed to optimize module: " + moduleFile.getFileName() + " - " + e.getMessage();
                    getLog().error(message, e);

                    if (failOnError) {
                        throw new MojoExecutionException(message, e);
                    }
                }
            }

            // Generate optimization report
            generateOptimizationReport(results);

            if (hasErrors && failOnError) {
                throw new MojoFailureException("WebAssembly module optimization found errors");
            }

            getLog().info("WebAssembly module optimization completed successfully");

        } catch (final Exception e) {
            if (e instanceof MojoExecutionException || e instanceof MojoFailureException) {
                throw e;
            }
            throw new MojoExecutionException("WebAssembly module optimization failed", e);
        }
    }

    private List<Path> findModuleFiles() throws IOException {
        final List<Path> moduleFiles = new ArrayList<>();

        Files.walk(sourceDirectory.toPath())
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

    private ModuleOptimizer.OptimizationConfiguration createOptimizationConfiguration() {
        return new ModuleOptimizer.OptimizationConfiguration(
            enableDeadCodeElimination,
            enableFunctionInlining,
            enableLoopOptimization,
            enableConstantFolding,
            ai.tegmentum.wasmtime4j.OptimizationLevel.SPEED,
            maxOptimizationPasses
        );
    }

    private OptimizationResult optimizeModule(final ModuleOptimizer optimizer,
                                            final Path moduleFile,
                                            final File backupDirectory) throws Exception {
        // Read original module
        final byte[] originalBytes = Files.readAllBytes(moduleFile);
        final Module originalModule = Module.fromBinary(Engine.newEngine(), originalBytes);

        // Backup original if enabled
        if (backupDirectory != null) {
            final Path backupFile = backupDirectory.toPath().resolve(moduleFile.getFileName());
            Files.copy(moduleFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // Perform optimization
        ModuleOptimizer.OptimizationResult optimizationResult;

        if ("AUTO".equalsIgnoreCase(optimizationStrategy)) {
            // Use automatic optimization
            final CompletableFuture<ModuleOptimizer.OptimizationResult> future = optimizer.autoOptimize(originalModule);
            optimizationResult = future.get();
        } else if (performABTesting) {
            // Perform A/B testing of multiple strategies
            final List<ModuleOptimizer.OptimizationStrategy> strategies = generateOptimizationStrategies();
            final CompletableFuture<ModuleOptimizer.ABTestingResult> future = optimizer.performABTesting(originalModule, strategies);
            final ModuleOptimizer.ABTestingResult abResult = future.get();
            optimizationResult = abResult.getWinner();
        } else {
            // Use specific strategy
            final ModuleOptimizer.OptimizationStrategy strategy = createOptimizationStrategy();
            final CompletableFuture<ModuleOptimizer.OptimizationResult> future = optimizer.optimizeModule(originalModule, strategy);
            optimizationResult = future.get();
        }

        if (!optimizationResult.isSuccessful()) {
            return OptimizationResult.failed(moduleFile, optimizationResult.getError());
        }

        // Validate optimized module if enabled
        if (validateOptimized) {
            final ModuleOptimizer.OptimizationValidation validation = optimizer.validateOptimization(
                originalModule, optimizationResult.getOptimizedModule());

            if (validation.hasErrors()) {
                return OptimizationResult.failed(moduleFile, "Optimized module validation failed");
            }
        }

        // Write optimized module to output directory
        final Path outputFile = outputDirectory.toPath().resolve(moduleFile.getFileName());
        final byte[] optimizedBytes = getModuleBytes(optimizationResult.getOptimizedModule());
        Files.write(outputFile, optimizedBytes);

        // Calculate optimization metrics
        final long originalSize = originalBytes.length;
        final long optimizedSize = optimizedBytes.length;
        final double sizeReduction = ((double) (originalSize - optimizedSize) / originalSize) * 100.0;
        final double performanceImprovement = optimizationResult.getImpact().getPerformanceImprovement();

        return OptimizationResult.successful(
            moduleFile,
            outputFile,
            originalSize,
            optimizedSize,
            sizeReduction,
            performanceImprovement,
            optimizationResult.getOptimizationTimeNs()
        );
    }

    private List<ModuleOptimizer.OptimizationStrategy> generateOptimizationStrategies() {
        final List<ModuleOptimizer.OptimizationStrategy> strategies = new ArrayList<>();

        // Basic optimization strategy
        strategies.add(new ModuleOptimizer.OptimizationStrategy(
            "Basic",
            "Basic optimizations with minimal impact",
            List.of(ModuleOptimizer.OptimizationType.DEAD_CODE_ELIMINATION),
            ModuleOptimizer.OptimizationImpactLevel.LOW,
            new java.util.HashMap<>()
        ));

        // Aggressive optimization strategy
        strategies.add(new ModuleOptimizer.OptimizationStrategy(
            "Aggressive",
            "Aggressive optimizations for maximum performance",
            List.of(
                ModuleOptimizer.OptimizationType.DEAD_CODE_ELIMINATION,
                ModuleOptimizer.OptimizationType.FUNCTION_INLINING,
                ModuleOptimizer.OptimizationType.LOOP_OPTIMIZATION,
                ModuleOptimizer.OptimizationType.CONSTANT_FOLDING
            ),
            ModuleOptimizer.OptimizationImpactLevel.HIGH,
            new java.util.HashMap<>()
        ));

        // Size optimization strategy
        strategies.add(new ModuleOptimizer.OptimizationStrategy(
            "Size",
            "Optimizations focused on reducing module size",
            List.of(
                ModuleOptimizer.OptimizationType.DEAD_CODE_ELIMINATION,
                ModuleOptimizer.OptimizationType.MEMORY_OPTIMIZATION
            ),
            ModuleOptimizer.OptimizationImpactLevel.MEDIUM,
            new java.util.HashMap<>()
        ));

        return strategies;
    }

    private ModuleOptimizer.OptimizationStrategy createOptimizationStrategy() {
        final List<ModuleOptimizer.OptimizationType> optimizations = new ArrayList<>();

        if (enableDeadCodeElimination) {
            optimizations.add(ModuleOptimizer.OptimizationType.DEAD_CODE_ELIMINATION);
        }
        if (enableFunctionInlining) {
            optimizations.add(ModuleOptimizer.OptimizationType.FUNCTION_INLINING);
        }
        if (enableLoopOptimization) {
            optimizations.add(ModuleOptimizer.OptimizationType.LOOP_OPTIMIZATION);
        }
        if (enableConstantFolding) {
            optimizations.add(ModuleOptimizer.OptimizationType.CONSTANT_FOLDING);
        }

        return new ModuleOptimizer.OptimizationStrategy(
            optimizationStrategy,
            "Custom optimization strategy",
            optimizations,
            ModuleOptimizer.OptimizationImpactLevel.MEDIUM,
            new java.util.HashMap<>()
        );
    }

    private byte[] getModuleBytes(final Module module) {
        // This would require access to module's internal representation
        // For now, return placeholder
        return new byte[0];
    }

    private void generateOptimizationReport(final List<OptimizationResult> results) throws IOException {
        final Path reportFile = outputDirectory.toPath().resolve("optimization-report.json");

        final StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(java.time.Instant.now()).append("\",\n");
        json.append("  \"project\": \"").append(project.getArtifactId()).append("\",\n");
        json.append("  \"version\": \"").append(project.getVersion()).append("\",\n");
        json.append("  \"totalModules\": ").append(results.size()).append(",\n");
        json.append("  \"strategy\": \"").append(optimizationStrategy).append("\",\n");

        long totalOriginalSize = 0;
        long totalOptimizedSize = 0;
        int successfulOptimizations = 0;

        for (final OptimizationResult result : results) {
            if (result.isSuccessful()) {
                totalOriginalSize += result.getOriginalSize();
                totalOptimizedSize += result.getOptimizedSize();
                successfulOptimizations++;
            }
        }

        final double overallSizeReduction = totalOriginalSize > 0 ?
            ((double) (totalOriginalSize - totalOptimizedSize) / totalOriginalSize) * 100.0 : 0.0;

        json.append("  \"summary\": {\n");
        json.append("    \"successfulOptimizations\": ").append(successfulOptimizations).append(",\n");
        json.append("    \"totalOriginalSize\": ").append(totalOriginalSize).append(",\n");
        json.append("    \"totalOptimizedSize\": ").append(totalOptimizedSize).append(",\n");
        json.append("    \"overallSizeReduction\": ").append(String.format("%.2f", overallSizeReduction)).append("\n");
        json.append("  },\n");

        json.append("  \"modules\": [\n");

        for (int i = 0; i < results.size(); i++) {
            final OptimizationResult result = results.get(i);
            json.append("    {\n");
            json.append("      \"file\": \"").append(result.getModuleFile().getFileName()).append("\",\n");
            json.append("      \"successful\": ").append(result.isSuccessful()).append(",\n");

            if (result.isSuccessful()) {
                json.append("      \"originalSize\": ").append(result.getOriginalSize()).append(",\n");
                json.append("      \"optimizedSize\": ").append(result.getOptimizedSize()).append(",\n");
                json.append("      \"sizeReduction\": ").append(String.format("%.2f", result.getSizeReduction())).append(",\n");
                json.append("      \"performanceImprovement\": ").append(String.format("%.2f", result.getPerformanceImprovement())).append(",\n");
                json.append("      \"optimizationTimeMs\": ").append(result.getOptimizationTimeNs() / 1_000_000).append("\n");
            } else {
                json.append("      \"error\": \"").append(result.getError()).append("\"\n");
            }

            json.append("    }");
            if (i < results.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        Files.write(reportFile, json.toString().getBytes());
        getLog().info("Optimization report generated: " + reportFile.toAbsolutePath());
    }

    /**
     * Optimization result for a single module.
     */
    private static final class OptimizationResult {
        private final Path moduleFile;
        private final Path outputFile;
        private final boolean successful;
        private final String error;
        private final long originalSize;
        private final long optimizedSize;
        private final double sizeReduction;
        private final double performanceImprovement;
        private final long optimizationTimeNs;

        private OptimizationResult(final Path moduleFile, final Path outputFile,
                                  final boolean successful, final String error,
                                  final long originalSize, final long optimizedSize,
                                  final double sizeReduction, final double performanceImprovement,
                                  final long optimizationTimeNs) {
            this.moduleFile = moduleFile;
            this.outputFile = outputFile;
            this.successful = successful;
            this.error = error;
            this.originalSize = originalSize;
            this.optimizedSize = optimizedSize;
            this.sizeReduction = sizeReduction;
            this.performanceImprovement = performanceImprovement;
            this.optimizationTimeNs = optimizationTimeNs;
        }

        public static OptimizationResult successful(final Path moduleFile, final Path outputFile,
                                                   final long originalSize, final long optimizedSize,
                                                   final double sizeReduction, final double performanceImprovement,
                                                   final long optimizationTimeNs) {
            return new OptimizationResult(moduleFile, outputFile, true, null,
                originalSize, optimizedSize, sizeReduction, performanceImprovement, optimizationTimeNs);
        }

        public static OptimizationResult failed(final Path moduleFile, final String error) {
            return new OptimizationResult(moduleFile, null, false, error, 0, 0, 0.0, 0.0, 0);
        }

        public Path getModuleFile() { return moduleFile; }
        public Path getOutputFile() { return outputFile; }
        public boolean isSuccessful() { return successful; }
        public String getError() { return error; }
        public long getOriginalSize() { return originalSize; }
        public long getOptimizedSize() { return optimizedSize; }
        public double getSizeReduction() { return sizeReduction; }
        public double getPerformanceImprovement() { return performanceImprovement; }
        public long getOptimizationTimeNs() { return optimizationTimeNs; }
    }
}