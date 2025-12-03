/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.maven;

import ai.tegmentum.wasmtime4j.maven.wit.WitJavaGenerator;
import ai.tegmentum.wasmtime4j.maven.wit.WitParser;
import ai.tegmentum.wasmtime4j.maven.wit.WitWorld;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Maven goal for generating Java source code from WIT (WebAssembly Interface Type) files.
 *
 * <p>This goal parses .wit files and generates type-safe Java records, enums, and interfaces
 * for interacting with WebAssembly components.
 *
 * <p>Usage in pom.xml:
 * <pre>
 * &lt;plugin&gt;
 *     &lt;groupId&gt;ai.tegmentum&lt;/groupId&gt;
 *     &lt;artifactId&gt;wasmtime4j-maven-plugin&lt;/artifactId&gt;
 *     &lt;version&gt;${wasmtime4j.version}&lt;/version&gt;
 *     &lt;executions&gt;
 *         &lt;execution&gt;
 *             &lt;goals&gt;
 *                 &lt;goal&gt;wit-codegen&lt;/goal&gt;
 *             &lt;/goals&gt;
 *             &lt;configuration&gt;
 *                 &lt;witDirectory&gt;${project.basedir}/src/main/wit&lt;/witDirectory&gt;
 *                 &lt;outputDirectory&gt;${project.build.directory}/generated-sources/wit&lt;/outputDirectory&gt;
 *                 &lt;packageName&gt;com.example.wasm&lt;/packageName&gt;
 *             &lt;/configuration&gt;
 *         &lt;/execution&gt;
 *     &lt;/executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 */
@Mojo(name = "wit-codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public final class WitCodegenMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Directory containing WIT files to process.
     */
    @Parameter(property = "wasmtime4j.wit.sourceDirectory", defaultValue = "${project.basedir}/src/main/wit")
    private File witDirectory;

    /**
     * Output directory for generated Java sources.
     */
    @Parameter(property = "wasmtime4j.wit.outputDirectory",
               defaultValue = "${project.build.directory}/generated-sources/wit")
    private File outputDirectory;

    /**
     * Base package name for generated Java classes.
     */
    @Parameter(property = "wasmtime4j.wit.packageName", required = true)
    private String packageName;

    /**
     * File patterns to include. Default is all .wit files.
     */
    @Parameter(property = "wasmtime4j.wit.includes")
    private List<String> includes = List.of("**/*.wit");

    /**
     * File patterns to exclude.
     */
    @Parameter(property = "wasmtime4j.wit.excludes")
    private List<String> excludes = new ArrayList<>();

    /**
     * Whether to skip the code generation.
     */
    @Parameter(property = "wasmtime4j.wit.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Whether to add the output directory to the project's compile source roots.
     */
    @Parameter(property = "wasmtime4j.wit.addCompileSourceRoot", defaultValue = "true")
    private boolean addCompileSourceRoot;

    /**
     * Whether to fail the build on parsing errors.
     */
    @Parameter(property = "wasmtime4j.wit.failOnError", defaultValue = "true")
    private boolean failOnError;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("WIT code generation skipped");
            return;
        }

        if (!witDirectory.exists()) {
            getLog().info("WIT directory does not exist: " + witDirectory.getAbsolutePath());
            return;
        }

        if (packageName == null || packageName.trim().isEmpty()) {
            throw new MojoExecutionException("packageName is required for WIT code generation");
        }

        try {
            getLog().info("Generating Java sources from WIT files in: " + witDirectory.getAbsolutePath());

            // Create output directory
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory: " + outputDirectory);
            }

            // Find WIT files
            final List<Path> witFiles = findWitFiles();

            if (witFiles.isEmpty()) {
                getLog().info("No WIT files found for code generation");
                return;
            }

            getLog().info("Found " + witFiles.size() + " WIT file(s) to process");

            final WitParser parser = new WitParser();
            final WitJavaGenerator generator = new WitJavaGenerator(packageName, outputDirectory.toPath());
            final List<Path> allGeneratedFiles = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;

            for (final Path witFile : witFiles) {
                try {
                    getLog().info("Processing: " + witFile.getFileName());

                    final WitWorld world = parser.parse(witFile);
                    getLog().debug("Parsed world: " + world.getName());

                    final List<Path> generatedFiles = generator.generate(world);
                    allGeneratedFiles.addAll(generatedFiles);
                    successCount++;

                    for (final Path generated : generatedFiles) {
                        getLog().debug("  Generated: " + generated.getFileName());
                    }

                } catch (final WitParser.WitParseException e) {
                    errorCount++;
                    final String message = "Failed to parse WIT file: " + witFile.getFileName() + " - " + e.getMessage();
                    if (failOnError) {
                        throw new MojoExecutionException(message, e);
                    } else {
                        getLog().warn(message);
                    }
                } catch (final IOException e) {
                    errorCount++;
                    final String message = "Failed to process WIT file: " + witFile.getFileName() + " - " + e.getMessage();
                    if (failOnError) {
                        throw new MojoExecutionException(message, e);
                    } else {
                        getLog().warn(message);
                    }
                }
            }

            // Add output directory to compile source roots
            if (addCompileSourceRoot && !allGeneratedFiles.isEmpty()) {
                project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
                getLog().info("Added compile source root: " + outputDirectory.getAbsolutePath());
            }

            getLog().info("WIT code generation completed: " + successCount + " succeeded, " + errorCount + " failed");
            getLog().info("Generated " + allGeneratedFiles.size() + " Java source file(s)");

        } catch (final Exception e) {
            if (e instanceof MojoExecutionException) {
                throw (MojoExecutionException) e;
            }
            throw new MojoExecutionException("WIT code generation failed", e);
        }
    }

    private List<Path> findWitFiles() throws IOException {
        final List<Path> witFiles = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(witDirectory.toPath())) {
            stream.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".wit"))
                .filter(this::matchesIncludes)
                .filter(this::doesNotMatchExcludes)
                .forEach(witFiles::add);
        }

        return witFiles;
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
        if (pattern.startsWith("**/*.")) {
            final String extension = pattern.substring(4);
            return fileName.endsWith(extension);
        }
        return fileName.matches(pattern.replace("*", ".*"));
    }
}
