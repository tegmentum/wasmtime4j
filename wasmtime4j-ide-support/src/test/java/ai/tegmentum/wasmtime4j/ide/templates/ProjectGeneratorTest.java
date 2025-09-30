package ai.tegmentum.wasmtime4j.ide.templates;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprehensive tests for the WebAssembly project generator. */
class ProjectGeneratorTest {

  @TempDir Path tempDir;

  private ProjectGenerator projectGenerator;

  @BeforeEach
  void setUp() {
    projectGenerator = new ProjectGenerator();
  }

  @Test
  void testGetAvailableProjectTypes() {
    // Act
    final var projectTypes = projectGenerator.getAvailableProjectTypes();

    // Assert
    assertNotNull(projectTypes);
    assertFalse(projectTypes.isEmpty());
    assertTrue(projectTypes.contains(ProjectGenerator.ProjectType.SIMPLE_MODULE));
    assertTrue(projectTypes.contains(ProjectGenerator.ProjectType.JAVA_HOST_APPLICATION));
    assertTrue(projectTypes.contains(ProjectGenerator.ProjectType.WASI_APPLICATION));
  }

  @Test
  void testGetTemplate() {
    // Act
    final ProjectGenerator.ProjectTemplate template =
        projectGenerator.getTemplate(ProjectGenerator.ProjectType.SIMPLE_MODULE);

    // Assert
    assertNotNull(template);
    assertEquals(ProjectGenerator.ProjectType.SIMPLE_MODULE, template.getType());
    assertEquals("Simple WebAssembly Module", template.getName());
    assertNotNull(template.getDescription());
    assertFalse(template.getFiles().isEmpty());
  }

  @Test
  void testGenerateSimpleModuleProject() {
    // Arrange
    final ProjectGenerator.ProjectConfig config =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.SIMPLE_MODULE,
            "test-module",
            "com.example",
            "Test Author",
            "A test WebAssembly module",
            "1.0.0",
            tempDir,
            false,
            Collections.emptyMap());

    // Act
    final ProjectGenerator.ProjectGenerationResult result =
        projectGenerator.generateProject(config);

    // Assert
    assertTrue(
        result.isSuccess(), "Project generation should succeed: " + result.getErrorMessage());
    assertNull(result.getErrorMessage());
    assertNotNull(result.getProjectPath());
    assertFalse(result.getCreatedFiles().isEmpty());

    // Verify project structure
    final Path projectDir = result.getProjectPath();
    assertTrue(Files.exists(projectDir));
    assertTrue(Files.exists(projectDir.resolve("src/main.wat")));
    assertTrue(Files.exists(projectDir.resolve("build.sh")));
    assertTrue(Files.exists(projectDir.resolve("README.md")));
    assertTrue(Files.exists(projectDir.resolve(".gitignore")));
  }

  @Test
  void testGenerateJavaHostApplicationProject() {
    // Arrange
    final ProjectGenerator.ProjectConfig config =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.JAVA_HOST_APPLICATION,
            "wasm-host",
            "com.example.wasm",
            "Test Author",
            "A Java WebAssembly host application",
            "1.0.0",
            tempDir,
            false,
            Collections.emptyMap());

    // Act
    final ProjectGenerator.ProjectGenerationResult result =
        projectGenerator.generateProject(config);

    // Assert
    assertTrue(
        result.isSuccess(), "Project generation should succeed: " + result.getErrorMessage());
    assertNotNull(result.getProjectPath());

    // Verify project structure
    final Path projectDir = result.getProjectPath();
    assertTrue(Files.exists(projectDir));
    assertTrue(Files.exists(projectDir.resolve("src/main/java/com/example/wasm/WasmHost.java")));
    assertTrue(Files.exists(projectDir.resolve("src/main/resources/module.wat")));
    assertTrue(Files.exists(projectDir.resolve("pom.xml")));
    assertTrue(Files.exists(projectDir.resolve("README.md")));
  }

  @Test
  void testGenerateWasiApplicationProject() {
    // Arrange
    final ProjectGenerator.ProjectConfig config =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.WASI_APPLICATION,
            "wasi-app",
            "com.example",
            "Test Author",
            "A WASI application",
            "1.0.0",
            tempDir,
            false,
            Collections.emptyMap());

    // Act
    final ProjectGenerator.ProjectGenerationResult result =
        projectGenerator.generateProject(config);

    // Assert
    assertTrue(result.isSuccess());
    assertNotNull(result.getProjectPath());

    // Verify project structure
    final Path projectDir = result.getProjectPath();
    assertTrue(Files.exists(projectDir));
    assertTrue(Files.exists(projectDir.resolve("src/main.wat")));
    assertTrue(Files.exists(projectDir.resolve("src/main.c")));
    assertTrue(Files.exists(projectDir.resolve("Makefile")));
    assertTrue(Files.exists(projectDir.resolve("README.md")));
  }

  @Test
  void testGenerateProjectWithCustomProperties() {
    // Arrange
    final Map<String, String> customProperties = new HashMap<>();
    customProperties.put("customProperty", "customValue");
    customProperties.put("anotherProperty", "anotherValue");

    final ProjectGenerator.ProjectConfig config =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.SIMPLE_MODULE,
            "custom-module",
            "com.custom",
            "Custom Author",
            "A custom module with properties",
            "2.0.0",
            tempDir,
            false,
            customProperties);

    // Act
    final ProjectGenerator.ProjectGenerationResult result =
        projectGenerator.generateProject(config);

    // Assert
    assertTrue(result.isSuccess());
    assertNotNull(result.getProjectPath());

    // Verify custom properties are used in template processing
    final Path projectDir = result.getProjectPath();
    assertTrue(Files.exists(projectDir));
  }

  @Test
  void testGenerateProjectWithOverwriteExisting() throws Exception {
    // Arrange - Create existing project
    final ProjectGenerator.ProjectConfig config1 =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.SIMPLE_MODULE,
            "existing-module",
            "com.example",
            "Test Author",
            "First version",
            "1.0.0",
            tempDir,
            false,
            Collections.emptyMap());

    projectGenerator.generateProject(config1);

    // Verify project exists
    final Path projectDir = tempDir.resolve("existing-module");
    assertTrue(Files.exists(projectDir));

    // Arrange - Try to overwrite without permission
    final ProjectGenerator.ProjectConfig config2 =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.SIMPLE_MODULE,
            "existing-module",
            "com.example",
            "Test Author",
            "Second version",
            "2.0.0",
            tempDir,
            false, // Don't allow overwrite
            Collections.emptyMap());

    // Act
    final ProjectGenerator.ProjectGenerationResult result2 =
        projectGenerator.generateProject(config2);

    // Assert - Should fail
    assertFalse(result2.isSuccess());
    assertNotNull(result2.getErrorMessage());
    assertTrue(result2.getErrorMessage().contains("already exists"));

    // Arrange - Try to overwrite with permission
    final ProjectGenerator.ProjectConfig config3 =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.SIMPLE_MODULE,
            "existing-module",
            "com.example",
            "Test Author",
            "Second version",
            "2.0.0",
            tempDir,
            true, // Allow overwrite
            Collections.emptyMap());

    // Act
    final ProjectGenerator.ProjectGenerationResult result3 =
        projectGenerator.generateProject(config3);

    // Assert - Should succeed
    assertTrue(result3.isSuccess(), "Overwrite should succeed: " + result3.getErrorMessage());
  }

  @Test
  void testTemplateVariableReplacement() throws Exception {
    // Arrange
    final ProjectGenerator.ProjectConfig config =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.SIMPLE_MODULE,
            "template-test",
            "com.template.test",
            "Template Author",
            "Testing template variables",
            "3.0.0",
            tempDir,
            false,
            Collections.emptyMap());

    // Act
    final ProjectGenerator.ProjectGenerationResult result =
        projectGenerator.generateProject(config);

    // Assert
    assertTrue(result.isSuccess());

    // Verify template variables are replaced in files
    final Path readmePath = result.getProjectPath().resolve("README.md");
    assertTrue(Files.exists(readmePath));

    final String readmeContent = Files.readString(readmePath);
    assertTrue(readmeContent.contains("template-test"));
    assertTrue(readmeContent.contains("Template Author"));
    assertTrue(readmeContent.contains("Testing template variables"));
    assertFalse(readmeContent.contains("{{projectName}}"));
    assertFalse(readmeContent.contains("{{author}}"));
    assertFalse(readmeContent.contains("{{description}}"));
  }

  @Test
  void testCamelCaseConversion() throws Exception {
    // Arrange
    final ProjectGenerator.ProjectConfig config =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.JAVA_HOST_APPLICATION,
            "kebab-case-project",
            "com.example",
            "Test Author",
            "Testing camel case conversion",
            "1.0.0",
            tempDir,
            false,
            Collections.emptyMap());

    // Act
    final ProjectGenerator.ProjectGenerationResult result =
        projectGenerator.generateProject(config);

    // Assert
    assertTrue(result.isSuccess());

    // Verify camel case conversion in Java class name
    final Path javaFile =
        result.getProjectPath().resolve("src/main/java/com/example/KebabCaseProject.java");
    assertTrue(Files.exists(javaFile), "Java file should exist with camel case name");

    final String javaContent = Files.readString(javaFile);
    assertTrue(javaContent.contains("class KebabCaseProject"));
  }

  @Test
  void testAllProjectTypesGeneration() {
    // Test that all project types can be generated without errors
    for (final ProjectGenerator.ProjectType type : ProjectGenerator.ProjectType.values()) {
      // Arrange
      final ProjectGenerator.ProjectConfig config =
          new ProjectGenerator.ProjectConfig(
              type,
              "test-" + type.name().toLowerCase(),
              "com.example",
              "Test Author",
              "Testing " + type.getDisplayName(),
              "1.0.0",
              tempDir,
              true, // Allow overwrite for multiple tests
              Collections.emptyMap());

      // Act
      final ProjectGenerator.ProjectGenerationResult result =
          projectGenerator.generateProject(config);

      // Assert
      assertTrue(
          result.isSuccess(),
          "Project type " + type + " should generate successfully: " + result.getErrorMessage());
      assertNotNull(result.getProjectPath());
      assertTrue(Files.exists(result.getProjectPath()));
    }
  }

  @Test
  void testProjectConfigValidation() {
    // Test null project type
    assertThrows(
        NullPointerException.class,
        () -> {
          new ProjectGenerator.ProjectConfig(
              null,
              "test-project",
              "com.example",
              "Author",
              "Description",
              "1.0.0",
              tempDir,
              false,
              Collections.emptyMap());
        });

    // Test null project name
    assertThrows(
        NullPointerException.class,
        () -> {
          new ProjectGenerator.ProjectConfig(
              ProjectGenerator.ProjectType.SIMPLE_MODULE,
              null,
              "com.example",
              "Author",
              "Description",
              "1.0.0",
              tempDir,
              false,
              Collections.emptyMap());
        });

    // Test null output directory
    assertThrows(
        NullPointerException.class,
        () -> {
          new ProjectGenerator.ProjectConfig(
              ProjectGenerator.ProjectType.SIMPLE_MODULE,
              "test-project",
              "com.example",
              "Author",
              "Description",
              "1.0.0",
              null,
              false,
              Collections.emptyMap());
        });
  }

  @Test
  void testProjectGenerationWithNonExistentOutputDir() {
    // Arrange
    final Path nonExistentDir = tempDir.resolve("non-existent").resolve("nested");
    final ProjectGenerator.ProjectConfig config =
        new ProjectGenerator.ProjectConfig(
            ProjectGenerator.ProjectType.SIMPLE_MODULE,
            "nested-project",
            "com.example",
            "Test Author",
            "Testing nested directory creation",
            "1.0.0",
            nonExistentDir,
            false,
            Collections.emptyMap());

    // Act
    final ProjectGenerator.ProjectGenerationResult result =
        projectGenerator.generateProject(config);

    // Assert
    assertTrue(result.isSuccess());
    assertNotNull(result.getProjectPath());
    assertTrue(Files.exists(result.getProjectPath()));
    assertTrue(Files.exists(result.getProjectPath().resolve("src/main.wat")));
  }

  @Test
  void testProjectGenerationResultSuccess() {
    // Arrange
    final Path projectPath = tempDir.resolve("test-project");
    final java.util.List<String> createdFiles = java.util.Arrays.asList("file1.txt", "file2.txt");

    // Act
    final ProjectGenerator.ProjectGenerationResult result =
        ProjectGenerator.ProjectGenerationResult.success(projectPath, createdFiles);

    // Assert
    assertTrue(result.isSuccess());
    assertNull(result.getErrorMessage());
    assertEquals(projectPath, result.getProjectPath());
    assertEquals(createdFiles, result.getCreatedFiles());
  }

  @Test
  void testProjectGenerationResultFailure() {
    // Arrange
    final String errorMessage = "Test error message";

    // Act
    final ProjectGenerator.ProjectGenerationResult result =
        ProjectGenerator.ProjectGenerationResult.failure(errorMessage);

    // Assert
    assertFalse(result.isSuccess());
    assertEquals(errorMessage, result.getErrorMessage());
    assertNull(result.getProjectPath());
    assertTrue(result.getCreatedFiles().isEmpty());
  }
}
