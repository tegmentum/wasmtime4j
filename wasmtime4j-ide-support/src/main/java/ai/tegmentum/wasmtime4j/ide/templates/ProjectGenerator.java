package ai.tegmentum.wasmtime4j.ide.templates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates WebAssembly project templates and scaffolding for development.
 * Provides various project types and configurations for different use cases.
 */
public final class ProjectGenerator {

    private static final Logger LOGGER = Logger.getLogger(ProjectGenerator.class.getName());

    private final Map<ProjectType, ProjectTemplate> templates;

    /**
     * Creates a new project generator.
     */
    public ProjectGenerator() {
        this.templates = new EnumMap<>(ProjectType.class);
        initializeTemplates();
    }

    /**
     * Gets all available project types.
     *
     * @return Set of available project types
     */
    public Set<ProjectType> getAvailableProjectTypes() {
        return EnumSet.allOf(ProjectType.class);
    }

    /**
     * Gets the template for a specific project type.
     *
     * @param type Project type
     * @return Project template
     */
    public ProjectTemplate getTemplate(final ProjectType type) {
        return templates.get(Objects.requireNonNull(type, "Project type cannot be null"));
    }

    /**
     * Generates a new project from a template.
     *
     * @param config Project configuration
     * @return Generation result
     */
    public ProjectGenerationResult generateProject(final ProjectConfig config) {
        Objects.requireNonNull(config, "Project config cannot be null");

        try {
            LOGGER.info("Generating project: " + config.getProjectName() + " (" + config.getProjectType() + ")");

            final ProjectTemplate template = templates.get(config.getProjectType());
            if (template == null) {
                return ProjectGenerationResult.failure("Unknown project type: " + config.getProjectType());
            }

            // Create project directory
            final Path projectDir = config.getOutputDirectory().resolve(config.getProjectName());
            if (Files.exists(projectDir)) {
                if (!config.isOverwriteExisting()) {
                    return ProjectGenerationResult.failure("Project directory already exists: " + projectDir);
                }
                // Clean existing directory
                deleteDirectoryRecursively(projectDir);
            }

            Files.createDirectories(projectDir);

            // Generate project structure
            final List<String> createdFiles = new ArrayList<>();
            for (final TemplateFile templateFile : template.getFiles()) {
                final Path targetPath = projectDir.resolve(templateFile.getRelativePath());
                Files.createDirectories(targetPath.getParent());

                final String content = processTemplate(templateFile.getContent(), config);
                Files.write(targetPath, content.getBytes());
                createdFiles.add(targetPath.toString());

                LOGGER.fine("Created file: " + targetPath);
            }

            // Copy binary resources if any
            for (final TemplateResource resource : template.getResources()) {
                final Path targetPath = projectDir.resolve(resource.getRelativePath());
                Files.createDirectories(targetPath.getParent());
                Files.copy(resource.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                createdFiles.add(targetPath.toString());

                LOGGER.fine("Copied resource: " + targetPath);
            }

            LOGGER.info("Project generated successfully: " + projectDir);
            return ProjectGenerationResult.success(projectDir, createdFiles);

        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate project: " + config.getProjectName(), e);
            return ProjectGenerationResult.failure("IO error: " + e.getMessage());
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error generating project: " + config.getProjectName(), e);
            return ProjectGenerationResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    private void initializeTemplates() {
        // Initialize all project templates
        templates.put(ProjectType.SIMPLE_MODULE, createSimpleModuleTemplate());
        templates.put(ProjectType.JAVA_HOST_APPLICATION, createJavaHostApplicationTemplate());
        templates.put(ProjectType.WASI_APPLICATION, createWasiApplicationTemplate());
        templates.put(ProjectType.LIBRARY_MODULE, createLibraryModuleTemplate());
        templates.put(ProjectType.PERFORMANCE_BENCHMARK, createPerformanceBenchmarkTemplate());
        templates.put(ProjectType.TESTING_FRAMEWORK, createTestingFrameworkTemplate());
    }

    private ProjectTemplate createSimpleModuleTemplate() {
        final List<TemplateFile> files = Arrays.asList(
            new TemplateFile("src/main.wat", createSimpleWatContent()),
            new TemplateFile("build.sh", createSimpleBuildScript()),
            new TemplateFile("README.md", createSimpleReadme()),
            new TemplateFile(".gitignore", createGitignore())
        );

        return new ProjectTemplate(
            ProjectType.SIMPLE_MODULE,
            "Simple WebAssembly Module",
            "A basic WebAssembly module with essential structure",
            files,
            Collections.emptyList()
        );
    }

    private ProjectTemplate createJavaHostApplicationTemplate() {
        final List<TemplateFile> files = Arrays.asList(
            new TemplateFile("src/main/java/{{packagePath}}/{{className}}.java", createJavaHostContent()),
            new TemplateFile("src/main/resources/module.wat", createHostWatContent()),
            new TemplateFile("pom.xml", createMavenPom()),
            new TemplateFile("README.md", createJavaHostReadme()),
            new TemplateFile(".gitignore", createGitignore())
        );

        return new ProjectTemplate(
            ProjectType.JAVA_HOST_APPLICATION,
            "Java Host Application",
            "A Java application that hosts and executes WebAssembly modules",
            files,
            Collections.emptyList()
        );
    }

    private ProjectTemplate createWasiApplicationTemplate() {
        final List<TemplateFile> files = Arrays.asList(
            new TemplateFile("src/main.wat", createWasiWatContent()),
            new TemplateFile("src/main.c", createWasiCContent()),
            new TemplateFile("Makefile", createWasiMakefile()),
            new TemplateFile("README.md", createWasiReadme()),
            new TemplateFile(".gitignore", createGitignore())
        );

        return new ProjectTemplate(
            ProjectType.WASI_APPLICATION,
            "WASI Application",
            "A WebAssembly System Interface (WASI) application",
            files,
            Collections.emptyList()
        );
    }

    private ProjectTemplate createLibraryModuleTemplate() {
        final List<TemplateFile> files = Arrays.asList(
            new TemplateFile("src/lib.wat", createLibraryWatContent()),
            new TemplateFile("include/{{projectName}}.h", createLibraryHeaderContent()),
            new TemplateFile("examples/example.c", createLibraryExampleContent()),
            new TemplateFile("Makefile", createLibraryMakefile()),
            new TemplateFile("README.md", createLibraryReadme()),
            new TemplateFile(".gitignore", createGitignore())
        );

        return new ProjectTemplate(
            ProjectType.LIBRARY_MODULE,
            "Library Module",
            "A reusable WebAssembly library module",
            files,
            Collections.emptyList()
        );
    }

    private ProjectTemplate createPerformanceBenchmarkTemplate() {
        final List<TemplateFile> files = Arrays.asList(
            new TemplateFile("src/benchmark.wat", createBenchmarkWatContent()),
            new TemplateFile("src/main/java/{{packagePath}}/{{className}}Benchmark.java", createBenchmarkJavaContent()),
            new TemplateFile("pom.xml", createBenchmarkMavenPom()),
            new TemplateFile("run-benchmark.sh", createBenchmarkScript()),
            new TemplateFile("README.md", createBenchmarkReadme()),
            new TemplateFile(".gitignore", createGitignore())
        );

        return new ProjectTemplate(
            ProjectType.PERFORMANCE_BENCHMARK,
            "Performance Benchmark",
            "A WebAssembly performance benchmark suite",
            files,
            Collections.emptyList()
        );
    }

    private ProjectTemplate createTestingFrameworkTemplate() {
        final List<TemplateFile> files = Arrays.asList(
            new TemplateFile("src/test/java/{{packagePath}}/{{className}}Test.java", createTestJavaContent()),
            new TemplateFile("src/test/resources/test-module.wat", createTestWatContent()),
            new TemplateFile("pom.xml", createTestMavenPom()),
            new TemplateFile("src/test/resources/test-cases.json", createTestCasesJson()),
            new TemplateFile("README.md", createTestReadme()),
            new TemplateFile(".gitignore", createGitignore())
        );

        return new ProjectTemplate(
            ProjectType.TESTING_FRAMEWORK,
            "Testing Framework",
            "A comprehensive WebAssembly testing framework",
            files,
            Collections.emptyList()
        );
    }

    private String processTemplate(final String template, final ProjectConfig config) {
        String result = template;

        // Replace template variables
        result = result.replace("{{projectName}}", config.getProjectName());
        result = result.replace("{{className}}", toCamelCase(config.getProjectName()));
        result = result.replace("{{packageName}}", config.getPackageName());
        result = result.replace("{{packagePath}}", config.getPackageName().replace('.', '/'));
        result = result.replace("{{author}}", config.getAuthor());
        result = result.replace("{{description}}", config.getDescription());
        result = result.replace("{{version}}", config.getVersion());

        // Add custom properties
        for (final Map.Entry<String, String> entry : config.getProperties().entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        return result;
    }

    private String toCamelCase(final String input) {
        final StringBuilder result = new StringBuilder();
        boolean nextUpperCase = true;

        for (final char c : input.toCharArray()) {
            if (c == '-' || c == '_' || c == ' ') {
                nextUpperCase = true;
            } else if (nextUpperCase) {
                result.append(Character.toUpperCase(c));
                nextUpperCase = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private void deleteDirectoryRecursively(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (final IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to delete: " + path, e);
                }
            });
    }

    // Template content creation methods

    private String createSimpleWatContent() {
        return "(module\n" +
               "  ;; {{description}}\n" +
               "  ;; Generated by Wasmtime4j Project Generator\n" +
               "\n" +
               "  (func $add (param $lhs i32) (param $rhs i32) (result i32)\n" +
               "    local.get $lhs\n" +
               "    local.get $rhs\n" +
               "    i32.add)\n" +
               "\n" +
               "  (export \"add\" (func $add))\n" +
               ")\n";
    }

    private String createSimpleBuildScript() {
        return "#!/bin/bash\n" +
               "# Build script for {{projectName}}\n" +
               "# Generated by Wasmtime4j Project Generator\n" +
               "\n" +
               "set -e\n" +
               "\n" +
               "echo \"Building {{projectName}}...\"\n" +
               "\n" +
               "# Convert WAT to WASM (requires wat2wasm tool)\n" +
               "if command -v wat2wasm >/dev/null 2>&1; then\n" +
               "    wat2wasm src/main.wat -o build/{{projectName}}.wasm\n" +
               "    echo \"Built: build/{{projectName}}.wasm\"\n" +
               "else\n" +
               "    echo \"Error: wat2wasm tool not found. Please install WABT tools.\"\n" +
               "    exit 1\n" +
               "fi\n" +
               "\n" +
               "echo \"Build complete!\"\n";
    }

    private String createSimpleReadme() {
        return "# {{projectName}}\n" +
               "\n" +
               "{{description}}\n" +
               "\n" +
               "Generated by Wasmtime4j Project Generator.\n" +
               "\n" +
               "## Building\n" +
               "\n" +
               "```bash\n" +
               "chmod +x build.sh\n" +
               "./build.sh\n" +
               "```\n" +
               "\n" +
               "## Running\n" +
               "\n" +
               "Use Wasmtime4j to load and execute the generated WASM module:\n" +
               "\n" +
               "```java\n" +
               "Engine engine = Engine.newEngine();\n" +
               "Store store = Store.newStore(engine);\n" +
               "Module module = Module.fromFile(engine, \"build/{{projectName}}.wasm\");\n" +
               "Instance instance = Instance.newInstance(store, module, new Object[0]);\n" +
               "```\n" +
               "\n" +
               "## Author\n" +
               "\n" +
               "{{author}}\n" +
               "\n" +
               "## License\n" +
               "\n" +
               "Apache License 2.0\n";
    }

    private String createJavaHostContent() {
        return "package {{packageName}};\n" +
               "\n" +
               "import ai.tegmentum.wasmtime4j.*;\n" +
               "import java.nio.file.Path;\n" +
               "import java.nio.file.Paths;\n" +
               "\n" +
               "/**\n" +
               " * {{description}}\n" +
               " * Generated by Wasmtime4j Project Generator\n" +
               " */\n" +
               "public final class {{className}} {\n" +
               "\n" +
               "    public static void main(final String[] args) {\n" +
               "        try {\n" +
               "            // Initialize Wasmtime4j engine\n" +
               "            final Engine engine = Engine.newEngine();\n" +
               "            final Store store = Store.newStore(engine);\n" +
               "\n" +
               "            // Load WebAssembly module\n" +
               "            final Path modulePath = Paths.get(\"src/main/resources/module.wasm\");\n" +
               "            final Module module = Module.fromFile(engine, modulePath.toString());\n" +
               "\n" +
               "            // Create instance\n" +
               "            final Instance instance = Instance.newInstance(store, module, new Object[0]);\n" +
               "\n" +
               "            // Get exported function\n" +
               "            final Function addFunction = instance.getExport(\"add\").asFunction();\n" +
               "\n" +
               "            // Call function\n" +
               "            final WasmValue[] args = { WasmValue.fromI32(5), WasmValue.fromI32(3) };\n" +
               "            final WasmValue[] result = addFunction.call(args);\n" +
               "\n" +
               "            System.out.println(\"Result: \" + result[0].asI32());\n" +
               "\n" +
               "        } catch (final Exception e) {\n" +
               "            System.err.println(\"Error: \" + e.getMessage());\n" +
               "            e.printStackTrace();\n" +
               "        }\n" +
               "    }\n" +
               "}\n";
    }

    private String createHostWatContent() {
        return createSimpleWatContent();
    }

    private String createMavenPom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
               "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
               "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n" +
               "         http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
               "    <modelVersion>4.0.0</modelVersion>\n" +
               "\n" +
               "    <groupId>{{packageName}}</groupId>\n" +
               "    <artifactId>{{projectName}}</artifactId>\n" +
               "    <version>{{version}}</version>\n" +
               "    <packaging>jar</packaging>\n" +
               "\n" +
               "    <name>{{projectName}}</name>\n" +
               "    <description>{{description}}</description>\n" +
               "\n" +
               "    <properties>\n" +
               "        <maven.compiler.source>11</maven.compiler.source>\n" +
               "        <maven.compiler.target>11</maven.compiler.target>\n" +
               "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
               "        <wasmtime4j.version>1.0.0-SNAPSHOT</wasmtime4j.version>\n" +
               "    </properties>\n" +
               "\n" +
               "    <dependencies>\n" +
               "        <dependency>\n" +
               "            <groupId>ai.tegmentum</groupId>\n" +
               "            <artifactId>wasmtime4j</artifactId>\n" +
               "            <version>${wasmtime4j.version}</version>\n" +
               "        </dependency>\n" +
               "    </dependencies>\n" +
               "\n" +
               "    <build>\n" +
               "        <plugins>\n" +
               "            <plugin>\n" +
               "                <groupId>org.apache.maven.plugins</groupId>\n" +
               "                <artifactId>maven-compiler-plugin</artifactId>\n" +
               "                <version>3.11.0</version>\n" +
               "                <configuration>\n" +
               "                    <source>11</source>\n" +
               "                    <target>11</target>\n" +
               "                </configuration>\n" +
               "            </plugin>\n" +
               "        </plugins>\n" +
               "    </build>\n" +
               "</project>\n";
    }

    private String createJavaHostReadme() {
        return "# {{projectName}}\n" +
               "\n" +
               "{{description}}\n" +
               "\n" +
               "A Java application that hosts and executes WebAssembly modules using Wasmtime4j.\n" +
               "\n" +
               "## Building\n" +
               "\n" +
               "```bash\n" +
               "mvn clean compile\n" +
               "```\n" +
               "\n" +
               "## Running\n" +
               "\n" +
               "```bash\n" +
               "mvn exec:java -Dexec.mainClass=\"{{packageName}}.{{className}}\"\n" +
               "```\n" +
               "\n" +
               "## Structure\n" +
               "\n" +
               "- `src/main/java/` - Java host application code\n" +
               "- `src/main/resources/` - WebAssembly modules and resources\n" +
               "\n" +
               "## Author\n" +
               "\n" +
               "{{author}}\n";
    }

    private String createWasiWatContent() {
        return "(module\n" +
               "  ;; WASI Application: {{description}}\n" +
               "  ;; Generated by Wasmtime4j Project Generator\n" +
               "\n" +
               "  (import \"wasi_snapshot_preview1\" \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))\n" +
               "\n" +
               "  (memory (export \"memory\") 1)\n" +
               "\n" +
               "  (data (i32.const 8) \"Hello, WASI!\\n\")\n" +
               "\n" +
               "  (func $main (export \"_start\")\n" +
               "    (i32.store (i32.const 0) (i32.const 8))   ;; iov.iov_base - pointer to string\n" +
               "    (i32.store (i32.const 4) (i32.const 13))  ;; iov.iov_len - string length\n" +
               "\n" +
               "    (call $fd_write\n" +
               "      (i32.const 1)   ;; file_descriptor - stdout\n" +
               "      (i32.const 0)   ;; *iovs - pointer to iov array\n" +
               "      (i32.const 1)   ;; iovs_len - number of iov entries\n" +
               "      (i32.const 20)) ;; nwritten - where to write number of bytes written\n" +
               "    drop\n" +
               "  )\n" +
               ")\n";
    }

    private String createWasiCContent() {
        return "#include <stdio.h>\n" +
               "#include <string.h>\n" +
               "\n" +
               "// {{description}}\n" +
               "// Generated by Wasmtime4j Project Generator\n" +
               "\n" +
               "int main(int argc, char* argv[]) {\n" +
               "    printf(\"Hello from {{projectName}}!\\n\");\n" +
               "    \n" +
               "    if (argc > 1) {\n" +
               "        printf(\"Arguments:\\n\");\n" +
               "        for (int i = 1; i < argc; i++) {\n" +
               "            printf(\"  %d: %s\\n\", i, argv[i]);\n" +
               "        }\n" +
               "    }\n" +
               "    \n" +
               "    return 0;\n" +
               "}\n";
    }

    private String createWasiMakefile() {
        return "# Makefile for {{projectName}}\n" +
               "# Generated by Wasmtime4j Project Generator\n" +
               "\n" +
               "CC = clang\n" +
               "WASM_CC = $(CC) --target=wasm32-wasi\n" +
               "WAT2WASM = wat2wasm\n" +
               "\n" +
               "SRCDIR = src\n" +
               "BUILDDIR = build\n" +
               "\n" +
               "# Create build directory\n" +
               "$(BUILDDIR):\n" +
               "\tmkdir -p $(BUILDDIR)\n" +
               "\n" +
               "# Build from C source\n" +
               "$(BUILDDIR)/{{projectName}}.wasm: $(SRCDIR)/main.c | $(BUILDDIR)\n" +
               "\t$(WASM_CC) -o $@ $<\n" +
               "\n" +
               "# Build from WAT source\n" +
               "$(BUILDDIR)/{{projectName}}-wat.wasm: $(SRCDIR)/main.wat | $(BUILDDIR)\n" +
               "\t$(WAT2WASM) $< -o $@\n" +
               "\n" +
               ".PHONY: all clean c-build wat-build\n" +
               "\n" +
               "all: c-build wat-build\n" +
               "\n" +
               "c-build: $(BUILDDIR)/{{projectName}}.wasm\n" +
               "\n" +
               "wat-build: $(BUILDDIR)/{{projectName}}-wat.wasm\n" +
               "\n" +
               "clean:\n" +
               "\trm -rf $(BUILDDIR)\n" +
               "\n" +
               "run-c: $(BUILDDIR)/{{projectName}}.wasm\n" +
               "\twasmtime $< -- arg1 arg2\n" +
               "\n" +
               "run-wat: $(BUILDDIR)/{{projectName}}-wat.wasm\n" +
               "\twasmtime $<\n";
    }

    private String createWasiReadme() {
        return "# {{projectName}}\n" +
               "\n" +
               "{{description}}\n" +
               "\n" +
               "A WebAssembly System Interface (WASI) application.\n" +
               "\n" +
               "## Prerequisites\n" +
               "\n" +
               "- Clang with WASI support\n" +
               "- WABT tools (wat2wasm, wasm2wat)\n" +
               "- Wasmtime runtime\n" +
               "\n" +
               "## Building\n" +
               "\n" +
               "Build from C source:\n" +
               "```bash\n" +
               "make c-build\n" +
               "```\n" +
               "\n" +
               "Build from WAT source:\n" +
               "```bash\n" +
               "make wat-build\n" +
               "```\n" +
               "\n" +
               "Build both:\n" +
               "```bash\n" +
               "make all\n" +
               "```\n" +
               "\n" +
               "## Running\n" +
               "\n" +
               "With Wasmtime:\n" +
               "```bash\n" +
               "wasmtime build/{{projectName}}.wasm\n" +
               "```\n" +
               "\n" +
               "With Wasmtime4j (from Java):\n" +
               "```java\n" +
               "WasiContextBuilder wasiBuilder = WasiContext.newBuilder()\n" +
               "    .inheritStdio()\n" +
               "    .inheritEnv()\n" +
               "    .addArg(\"{{projectName}}\")\n" +
               "    .addArg(\"arg1\")\n" +
               "    .addArg(\"arg2\");\n" +
               "\n" +
               "Engine engine = Engine.newEngine();\n" +
               "Store store = Store.newStore(engine);\n" +
               "WasiContext wasi = wasiBuilder.build();\n" +
               "Module module = Module.fromFile(engine, \"build/{{projectName}}.wasm\");\n" +
               "Instance instance = Instance.newInstance(store, module, wasi.getImports());\n" +
               "\n" +
               "Function start = instance.getExport(\"_start\").asFunction();\n" +
               "start.call(new WasmValue[0]);\n" +
               "```\n" +
               "\n" +
               "## Author\n" +
               "\n" +
               "{{author}}\n";
    }

    // Additional template methods would continue here...
    private String createLibraryWatContent() { return ""; }
    private String createLibraryHeaderContent() { return ""; }
    private String createLibraryExampleContent() { return ""; }
    private String createLibraryMakefile() { return ""; }
    private String createLibraryReadme() { return ""; }
    private String createBenchmarkWatContent() { return ""; }
    private String createBenchmarkJavaContent() { return ""; }
    private String createBenchmarkMavenPom() { return ""; }
    private String createBenchmarkScript() { return ""; }
    private String createBenchmarkReadme() { return ""; }
    private String createTestJavaContent() { return ""; }
    private String createTestWatContent() { return ""; }
    private String createTestMavenPom() { return ""; }
    private String createTestCasesJson() { return ""; }
    private String createTestReadme() { return ""; }

    private String createGitignore() {
        return "# Build outputs\n" +
               "build/\n" +
               "target/\n" +
               "*.wasm\n" +
               "\n" +
               "# IDE files\n" +
               ".idea/\n" +
               ".vscode/\n" +
               "*.iml\n" +
               "\n" +
               "# OS files\n" +
               ".DS_Store\n" +
               "Thumbs.db\n" +
               "\n" +
               "# Temporary files\n" +
               "*.tmp\n" +
               "*.temp\n" +
               "*.log\n";
    }

    // Enums and data classes

    public enum ProjectType {
        SIMPLE_MODULE("Simple WebAssembly Module"),
        JAVA_HOST_APPLICATION("Java Host Application"),
        WASI_APPLICATION("WASI Application"),
        LIBRARY_MODULE("Library Module"),
        PERFORMANCE_BENCHMARK("Performance Benchmark"),
        TESTING_FRAMEWORK("Testing Framework");

        private final String displayName;

        ProjectType(final String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final class ProjectConfig {
        private final ProjectType projectType;
        private final String projectName;
        private final String packageName;
        private final String author;
        private final String description;
        private final String version;
        private final Path outputDirectory;
        private final boolean overwriteExisting;
        private final Map<String, String> properties;

        public ProjectConfig(final ProjectType projectType, final String projectName,
                            final String packageName, final String author, final String description,
                            final String version, final Path outputDirectory,
                            final boolean overwriteExisting, final Map<String, String> properties) {
            this.projectType = projectType;
            this.projectName = projectName;
            this.packageName = packageName;
            this.author = author;
            this.description = description;
            this.version = version;
            this.outputDirectory = outputDirectory;
            this.overwriteExisting = overwriteExisting;
            this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
        }

        public ProjectType getProjectType() { return projectType; }
        public String getProjectName() { return projectName; }
        public String getPackageName() { return packageName; }
        public String getAuthor() { return author; }
        public String getDescription() { return description; }
        public String getVersion() { return version; }
        public Path getOutputDirectory() { return outputDirectory; }
        public boolean isOverwriteExisting() { return overwriteExisting; }
        public Map<String, String> getProperties() { return properties; }
    }

    public static final class ProjectTemplate {
        private final ProjectType type;
        private final String name;
        private final String description;
        private final List<TemplateFile> files;
        private final List<TemplateResource> resources;

        public ProjectTemplate(final ProjectType type, final String name, final String description,
                              final List<TemplateFile> files, final List<TemplateResource> resources) {
            this.type = type;
            this.name = name;
            this.description = description;
            this.files = Collections.unmodifiableList(new ArrayList<>(files));
            this.resources = Collections.unmodifiableList(new ArrayList<>(resources));
        }

        public ProjectType getType() { return type; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<TemplateFile> getFiles() { return files; }
        public List<TemplateResource> getResources() { return resources; }
    }

    public static final class TemplateFile {
        private final String relativePath;
        private final String content;

        public TemplateFile(final String relativePath, final String content) {
            this.relativePath = relativePath;
            this.content = content;
        }

        public String getRelativePath() { return relativePath; }
        public String getContent() { return content; }
    }

    public static final class TemplateResource {
        private final String relativePath;
        private final java.io.InputStream inputStream;

        public TemplateResource(final String relativePath, final java.io.InputStream inputStream) {
            this.relativePath = relativePath;
            this.inputStream = inputStream;
        }

        public String getRelativePath() { return relativePath; }
        public java.io.InputStream getInputStream() { return inputStream; }
    }

    public static final class ProjectGenerationResult {
        private final boolean success;
        private final String errorMessage;
        private final Path projectPath;
        private final List<String> createdFiles;

        private ProjectGenerationResult(final boolean success, final String errorMessage,
                                       final Path projectPath, final List<String> createdFiles) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.projectPath = projectPath;
            this.createdFiles = createdFiles != null
                ? Collections.unmodifiableList(new ArrayList<>(createdFiles))
                : Collections.emptyList();
        }

        public static ProjectGenerationResult success(final Path projectPath, final List<String> createdFiles) {
            return new ProjectGenerationResult(true, null, projectPath, createdFiles);
        }

        public static ProjectGenerationResult failure(final String errorMessage) {
            return new ProjectGenerationResult(false, errorMessage, null, null);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public Path getProjectPath() { return projectPath; }
        public List<String> getCreatedFiles() { return createdFiles; }
    }
}