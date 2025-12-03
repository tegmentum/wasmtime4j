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

package ai.tegmentum.wasmtime4j.maven.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for WIT parsing and Java code generation.
 */
class WitCodegenTest {

    @TempDir
    Path tempDir;

    private Path outputDir;

    @BeforeEach
    void setUp() throws IOException {
        outputDir = tempDir.resolve("generated");
        Files.createDirectories(outputDir);
    }

    @Test
    void testParseCalculatorWorld() throws Exception {
        // Get the calculator.wit file from test resources
        final Path witFile = Path.of("src/test/resources/wit/calculator.wit");
        assertTrue(Files.exists(witFile), "calculator.wit should exist");

        final WitParser parser = new WitParser();
        final WitWorld world = parser.parse(witFile);

        assertNotNull(world, "Parsed world should not be null");
        assertEquals("calculator", world.getName(), "World name should be 'calculator'");
        assertEquals("example:calculator@1.0.0", world.getPackageName(), "Package name should match");

        // Verify types
        final List<WitDefinition> types = world.getTypes();
        assertFalse(types.isEmpty(), "World should have type definitions");

        // Check enum
        boolean hasOperationEnum = types.stream()
                .anyMatch(t -> t instanceof WitDefinition.EnumDef && "operation".equals(t.name()));
        assertTrue(hasOperationEnum, "Should have 'operation' enum");

        // Check variant
        boolean hasCalculationResult = types.stream()
                .anyMatch(t -> t instanceof WitDefinition.VariantDef && "calculation-result".equals(t.name()));
        assertTrue(hasCalculationResult, "Should have 'calculation-result' variant");

        // Check record
        boolean hasPointRecord = types.stream()
                .anyMatch(t -> t instanceof WitDefinition.RecordDef && "point".equals(t.name()));
        assertTrue(hasPointRecord, "Should have 'point' record");

        // Check flags
        boolean hasPermissionsFlags = types.stream()
                .anyMatch(t -> t instanceof WitDefinition.FlagsDef && "permissions".equals(t.name()));
        assertTrue(hasPermissionsFlags, "Should have 'permissions' flags");

        // Check exports
        final List<WitDefinition.FuncDef> exports = world.getExports();
        assertEquals(2, exports.size(), "Should have 2 exported functions");

        boolean hasCalculateExport = exports.stream().anyMatch(f -> "calculate".equals(f.name()));
        assertTrue(hasCalculateExport, "Should have 'calculate' export");

        boolean hasDistanceExport = exports.stream().anyMatch(f -> "distance".equals(f.name()));
        assertTrue(hasDistanceExport, "Should have 'distance' export");

        // Check imports
        final List<WitDefinition.FuncDef> imports = world.getImports();
        assertEquals(1, imports.size(), "Should have 1 imported function");
        assertEquals("log", imports.get(0).name(), "Import should be 'log'");
    }

    @Test
    void testGenerateJavaFromCalculatorWorld() throws Exception {
        final Path witFile = Path.of("src/test/resources/wit/calculator.wit");
        assertTrue(Files.exists(witFile), "calculator.wit should exist");

        final WitParser parser = new WitParser();
        final WitWorld world = parser.parse(witFile);

        final WitJavaGenerator generator = new WitJavaGenerator("com.example.calculator", outputDir);
        final List<Path> generatedFiles = generator.generate(world);

        assertFalse(generatedFiles.isEmpty(), "Should generate files");

        // Check that expected files were generated
        final Path packageDir = outputDir.resolve("com/example/calculator");
        assertTrue(Files.isDirectory(packageDir), "Package directory should exist");

        // Check enum file
        final Path operationFile = packageDir.resolve("Operation.java");
        assertTrue(Files.exists(operationFile), "Operation.java should be generated");
        final String operationContent = Files.readString(operationFile);
        assertTrue(operationContent.contains("public enum Operation"), "Should define Operation enum");
        assertTrue(operationContent.contains("ADD"), "Should have ADD constant");
        assertTrue(operationContent.contains("SUBTRACT"), "Should have SUBTRACT constant");
        assertTrue(operationContent.contains("MULTIPLY"), "Should have MULTIPLY constant");
        assertTrue(operationContent.contains("DIVIDE"), "Should have DIVIDE constant");

        // Check variant file
        final Path resultFile = packageDir.resolve("CalculationResult.java");
        assertTrue(Files.exists(resultFile), "CalculationResult.java should be generated");
        final String resultContent = Files.readString(resultFile);
        assertTrue(resultContent.contains("public sealed interface CalculationResult"),
                "Should define sealed interface");
        assertTrue(resultContent.contains("record Success"), "Should have Success case");
        assertTrue(resultContent.contains("record Error"), "Should have Error case");

        // Check record file
        final Path pointFile = packageDir.resolve("Point.java");
        assertTrue(Files.exists(pointFile), "Point.java should be generated");
        final String pointContent = Files.readString(pointFile);
        assertTrue(pointContent.contains("public record Point"), "Should define Point record");
        assertTrue(pointContent.contains("double x"), "Should have x field");
        assertTrue(pointContent.contains("double y"), "Should have y field");

        // Check flags file
        final Path permissionsFile = packageDir.resolve("Permissions.java");
        assertTrue(Files.exists(permissionsFile), "Permissions.java should be generated");
        final String permissionsContent = Files.readString(permissionsFile);
        assertTrue(permissionsContent.contains("public enum Permissions"), "Should define Permissions enum");
        assertTrue(permissionsContent.contains("READ"), "Should have READ flag");
        assertTrue(permissionsContent.contains("WRITE"), "Should have WRITE flag");
        assertTrue(permissionsContent.contains("EXECUTE"), "Should have EXECUTE flag");

        // Check exports interface
        final Path exportsFile = packageDir.resolve("CalculatorExports.java");
        assertTrue(Files.exists(exportsFile), "CalculatorExports.java should be generated");
        final String exportsContent = Files.readString(exportsFile);
        assertTrue(exportsContent.contains("public interface CalculatorExports"),
                "Should define exports interface");
        assertTrue(exportsContent.contains("calculate("), "Should have calculate method");
        assertTrue(exportsContent.contains("distance("), "Should have distance method");

        // Check imports interface
        final Path importsFile = packageDir.resolve("CalculatorImports.java");
        assertTrue(Files.exists(importsFile), "CalculatorImports.java should be generated");
        final String importsContent = Files.readString(importsFile);
        assertTrue(importsContent.contains("public interface CalculatorImports"),
                "Should define imports interface");
        assertTrue(importsContent.contains("log("), "Should have log method");

        System.out.println("Generated files:");
        for (final Path file : generatedFiles) {
            System.out.println("  - " + file);
        }
    }

    @Test
    void testWitTypeToPascalCase() {
        assertEquals("HelloWorld", WitType.toPascalCase("hello-world"));
        assertEquals("MyVar", WitType.toPascalCase("my-var"));
        assertEquals("Simple", WitType.toPascalCase("simple"));
        assertEquals("XYZ", WitType.toPascalCase("x-y-z"));
    }

    @Test
    void testWitTypeToCamelCase() {
        assertEquals("helloWorld", WitType.toCamelCase("hello-world"));
        assertEquals("myVar", WitType.toCamelCase("my-var"));
        assertEquals("simple", WitType.toCamelCase("simple"));
        assertEquals("xYZ", WitType.toCamelCase("x-y-z"));
    }

    @Test
    void testWitTypeToJavaType() {
        // Test primitive types
        assertEquals("boolean", new WitType.Bool().toJavaType());
        assertEquals("short", new WitType.U8().toJavaType());  // Java has no unsigned byte
        assertEquals("int", new WitType.U16().toJavaType());   // Java has no unsigned short
        assertEquals("long", new WitType.U32().toJavaType());  // Java has no unsigned int
        assertEquals("java.math.BigInteger", new WitType.U64().toJavaType());  // Java has no unsigned long
        assertEquals("byte", new WitType.S8().toJavaType());
        assertEquals("short", new WitType.S16().toJavaType());
        assertEquals("int", new WitType.S32().toJavaType());
        assertEquals("long", new WitType.S64().toJavaType());
        assertEquals("float", new WitType.F32().toJavaType());
        assertEquals("double", new WitType.F64().toJavaType());
        assertEquals("String", new WitType.WitString().toJavaType());
        assertEquals("int", new WitType.Char().toJavaType());  // Unicode code point

        // Test composite types
        assertEquals("java.util.List<String>",
                new WitType.WitList(new WitType.WitString()).toJavaType());
        assertEquals("java.util.Optional<Integer>",
                new WitType.WitOption(new WitType.S32()).toJavaType());
    }
}
