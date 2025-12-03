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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates Java source code from WIT world definitions.
 *
 * <p>Converts WIT types to Java records, enums, and interfaces for type-safe
 * WebAssembly component interaction.
 *
 * @since 1.0.0
 */
public final class WitJavaGenerator {

    private static final String INDENT = "    ";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String basePackage;
    private final Path outputDirectory;

    /**
     * Creates a new Java generator.
     *
     * @param basePackage     the base Java package for generated code
     * @param outputDirectory the output directory for generated files
     */
    public WitJavaGenerator(final String basePackage, final Path outputDirectory) {
        this.basePackage = basePackage;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Generates Java source files from a WIT world.
     *
     * @param world the parsed WIT world
     * @return list of generated file paths
     * @throws IOException if file writing fails
     */
    public List<Path> generate(final WitWorld world) throws IOException {
        final List<Path> generatedFiles = new ArrayList<>();
        final String worldClassName = WitType.toPascalCase(world.getName());
        final String packageName = basePackage;

        // Create output directory
        final Path packageDir = outputDirectory.resolve(packageName.replace('.', '/'));
        Files.createDirectories(packageDir);

        // Generate type definitions
        for (final WitDefinition type : world.getTypes()) {
            final Path typePath = generateTypeDefinition(type, packageName, packageDir);
            if (typePath != null) {
                generatedFiles.add(typePath);
            }
        }

        // Generate world interface with exports
        if (!world.getExports().isEmpty()) {
            final Path exportsPath = generateExportsInterface(world, worldClassName, packageName, packageDir);
            generatedFiles.add(exportsPath);
        }

        // Generate imports interface if there are imports
        if (!world.getImports().isEmpty()) {
            final Path importsPath = generateImportsInterface(world, worldClassName, packageName, packageDir);
            generatedFiles.add(importsPath);
        }

        return generatedFiles;
    }

    private Path generateTypeDefinition(final WitDefinition type, final String packageName, final Path packageDir)
            throws IOException {
        final StringBuilder sb = new StringBuilder();

        // File header
        appendHeader(sb, packageName);

        if (type instanceof WitDefinition.RecordDef) {
            generateRecord(sb, (WitDefinition.RecordDef) type);
        } else if (type instanceof WitDefinition.EnumDef) {
            generateEnum(sb, (WitDefinition.EnumDef) type);
        } else if (type instanceof WitDefinition.VariantDef) {
            generateVariant(sb, (WitDefinition.VariantDef) type);
        } else if (type instanceof WitDefinition.FlagsDef) {
            generateFlags(sb, (WitDefinition.FlagsDef) type);
        } else if (type instanceof WitDefinition.TypeAlias) {
            // Type aliases don't generate separate files
            return null;
        } else {
            return null;
        }

        final String className = WitType.toPascalCase(type.name());
        final Path filePath = packageDir.resolve(className + ".java");
        Files.writeString(filePath, sb.toString());
        return filePath;
    }

    private void generateRecord(final StringBuilder sb, final WitDefinition.RecordDef record) {
        final String className = WitType.toPascalCase(record.name());

        sb.append("/**\n");
        sb.append(" * WIT record type: ").append(record.name()).append("\n");
        sb.append(" *\n");
        sb.append(" * <p>Generated from WIT definition.\n");
        sb.append(" */\n");
        sb.append("public record ").append(className).append("(\n");

        for (int i = 0; i < record.fields().size(); i++) {
            final WitDefinition.Field field = record.fields().get(i);
            final String fieldName = WitType.toCamelCase(field.name());
            final String fieldType = field.type().toJavaType();

            sb.append(INDENT).append(fieldType).append(" ").append(fieldName);
            if (i < record.fields().size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(") {\n");

        // Add validation in constructor
        if (!record.fields().isEmpty()) {
            sb.append("\n");
            sb.append(INDENT).append("/**\n");
            sb.append(INDENT).append(" * Validates record fields.\n");
            sb.append(INDENT).append(" */\n");
            sb.append(INDENT).append("public ").append(className).append(" {\n");
            for (final WitDefinition.Field field : record.fields()) {
                final String fieldName = WitType.toCamelCase(field.name());
                final String fieldType = field.type().toJavaType();
                // Only validate reference types
                if (!isPrimitive(fieldType)) {
                    sb.append(INDENT).append(INDENT);
                    sb.append("java.util.Objects.requireNonNull(").append(fieldName);
                    sb.append(", \"").append(fieldName).append(" cannot be null\");\n");
                }
            }
            sb.append(INDENT).append("}\n");
        }

        sb.append("}\n");
    }

    private void generateEnum(final StringBuilder sb, final WitDefinition.EnumDef enumDef) {
        final String className = WitType.toPascalCase(enumDef.name());

        sb.append("/**\n");
        sb.append(" * WIT enum type: ").append(enumDef.name()).append("\n");
        sb.append(" *\n");
        sb.append(" * <p>Generated from WIT definition.\n");
        sb.append(" */\n");
        sb.append("public enum ").append(className).append(" {\n");

        for (int i = 0; i < enumDef.values().size(); i++) {
            final String value = enumDef.values().get(i);
            final String enumValue = toEnumConstant(value);
            sb.append(INDENT).append(enumValue);
            if (i < enumDef.values().size() - 1) {
                sb.append(",");
            } else {
                sb.append(";");
            }
            sb.append("\n");
        }

        // Add ordinal-based lookup
        sb.append("\n");
        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * Returns the enum value for the given ordinal.\n");
        sb.append(INDENT).append(" *\n");
        sb.append(INDENT).append(" * @param ordinal the ordinal value\n");
        sb.append(INDENT).append(" * @return the enum value\n");
        sb.append(INDENT).append(" * @throws IllegalArgumentException if ordinal is out of range\n");
        sb.append(INDENT).append(" */\n");
        sb.append(INDENT).append("public static ").append(className).append(" fromOrdinal(final int ordinal) {\n");
        sb.append(INDENT).append(INDENT).append("final ").append(className).append("[] values = values();\n");
        sb.append(INDENT).append(INDENT).append("if (ordinal < 0 || ordinal >= values.length) {\n");
        sb.append(INDENT).append(INDENT).append(INDENT);
        sb.append("throw new IllegalArgumentException(\"Invalid ordinal: \" + ordinal);\n");
        sb.append(INDENT).append(INDENT).append("}\n");
        sb.append(INDENT).append(INDENT).append("return values[ordinal];\n");
        sb.append(INDENT).append("}\n");

        sb.append("}\n");
    }

    private void generateVariant(final StringBuilder sb, final WitDefinition.VariantDef variant) {
        final String className = WitType.toPascalCase(variant.name());

        sb.append("/**\n");
        sb.append(" * WIT variant type: ").append(variant.name()).append("\n");
        sb.append(" *\n");
        sb.append(" * <p>Generated from WIT definition.\n");
        sb.append(" */\n");
        sb.append("public sealed interface ").append(className).append(" {\n\n");

        // Generate case records
        for (final WitDefinition.Case variantCase : variant.cases()) {
            final String caseName = WitType.toPascalCase(variantCase.name());

            sb.append(INDENT).append("/**\n");
            sb.append(INDENT).append(" * Variant case: ").append(variantCase.name()).append("\n");
            sb.append(INDENT).append(" */\n");

            if (variantCase.payload() != null) {
                final String payloadType = variantCase.payload().toJavaType();
                sb.append(INDENT).append("record ").append(caseName);
                sb.append("(").append(payloadType).append(" value) implements ");
                sb.append(className).append(" {\n");
                sb.append(INDENT).append(INDENT).append("public ").append(caseName).append(" {\n");
                if (!isPrimitive(payloadType)) {
                    sb.append(INDENT).append(INDENT).append(INDENT);
                    sb.append("java.util.Objects.requireNonNull(value, \"value cannot be null\");\n");
                }
                sb.append(INDENT).append(INDENT).append("}\n");
                sb.append(INDENT).append("}\n\n");
            } else {
                sb.append(INDENT).append("record ").append(caseName);
                sb.append("() implements ").append(className).append(" {}\n\n");
            }
        }

        sb.append("}\n");
    }

    private void generateFlags(final StringBuilder sb, final WitDefinition.FlagsDef flags) {
        final String className = WitType.toPascalCase(flags.name());

        sb.append("import java.util.EnumSet;\n");
        sb.append("import java.util.Set;\n\n");

        sb.append("/**\n");
        sb.append(" * WIT flags type: ").append(flags.name()).append("\n");
        sb.append(" *\n");
        sb.append(" * <p>Generated from WIT definition.\n");
        sb.append(" */\n");
        sb.append("public enum ").append(className).append(" {\n");

        for (int i = 0; i < flags.flags().size(); i++) {
            final String flag = flags.flags().get(i);
            final String flagConstant = toEnumConstant(flag);
            sb.append(INDENT).append(flagConstant);
            if (i < flags.flags().size() - 1) {
                sb.append(",");
            } else {
                sb.append(";");
            }
            sb.append("\n");
        }

        // Add helper methods for flags
        sb.append("\n");
        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * Creates an empty flags set.\n");
        sb.append(INDENT).append(" *\n");
        sb.append(INDENT).append(" * @return empty flags set\n");
        sb.append(INDENT).append(" */\n");
        sb.append(INDENT).append("public static Set<").append(className).append("> none() {\n");
        sb.append(INDENT).append(INDENT).append("return EnumSet.noneOf(").append(className).append(".class);\n");
        sb.append(INDENT).append("}\n\n");

        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * Creates a flags set with all flags.\n");
        sb.append(INDENT).append(" *\n");
        sb.append(INDENT).append(" * @return all flags set\n");
        sb.append(INDENT).append(" */\n");
        sb.append(INDENT).append("public static Set<").append(className).append("> all() {\n");
        sb.append(INDENT).append(INDENT).append("return EnumSet.allOf(").append(className).append(".class);\n");
        sb.append(INDENT).append("}\n");

        sb.append("}\n");
    }

    private Path generateExportsInterface(final WitWorld world, final String worldClassName,
            final String packageName, final Path packageDir) throws IOException {
        final StringBuilder sb = new StringBuilder();
        appendHeader(sb, packageName);

        sb.append("/**\n");
        sb.append(" * Exports interface for WIT world: ").append(world.getName()).append("\n");
        sb.append(" *\n");
        sb.append(" * <p>Implement this interface to provide exported functions to a WebAssembly component.\n");
        sb.append(" */\n");
        sb.append("public interface ").append(worldClassName).append("Exports {\n\n");

        for (final WitDefinition.FuncDef func : world.getExports()) {
            generateFunctionSignature(sb, func, false);
        }

        sb.append("}\n");

        final Path filePath = packageDir.resolve(worldClassName + "Exports.java");
        Files.writeString(filePath, sb.toString());
        return filePath;
    }

    private Path generateImportsInterface(final WitWorld world, final String worldClassName,
            final String packageName, final Path packageDir) throws IOException {
        final StringBuilder sb = new StringBuilder();
        appendHeader(sb, packageName);

        sb.append("/**\n");
        sb.append(" * Imports interface for WIT world: ").append(world.getName()).append("\n");
        sb.append(" *\n");
        sb.append(" * <p>Implement this interface to provide imported functions to a WebAssembly component.\n");
        sb.append(" */\n");
        sb.append("public interface ").append(worldClassName).append("Imports {\n\n");

        for (final WitDefinition.FuncDef func : world.getImports()) {
            generateFunctionSignature(sb, func, true);
        }

        sb.append("}\n");

        final Path filePath = packageDir.resolve(worldClassName + "Imports.java");
        Files.writeString(filePath, sb.toString());
        return filePath;
    }

    private void generateFunctionSignature(final StringBuilder sb, final WitDefinition.FuncDef func,
            final boolean isImport) {
        final String methodName = WitType.toCamelCase(func.name());

        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * WIT function: ").append(func.name()).append("\n");

        // Document parameters
        for (final WitDefinition.Param param : func.params()) {
            sb.append(INDENT).append(" *\n");
            sb.append(INDENT).append(" * @param ").append(WitType.toCamelCase(param.name()));
            sb.append(" the ").append(param.name()).append(" parameter\n");
        }

        // Document return
        if (!func.results().isEmpty()) {
            sb.append(INDENT).append(" *\n");
            sb.append(INDENT).append(" * @return the result\n");
        }

        sb.append(INDENT).append(" */\n");
        sb.append(INDENT);

        // Return type
        if (func.results().isEmpty()) {
            sb.append("void ");
        } else if (func.results().size() == 1) {
            sb.append(func.results().get(0).toJavaType()).append(" ");
        } else {
            // Multiple returns - use tuple
            sb.append("Object[] ");
        }

        sb.append(methodName).append("(");

        // Parameters
        for (int i = 0; i < func.params().size(); i++) {
            final WitDefinition.Param param = func.params().get(i);
            final String paramName = WitType.toCamelCase(param.name());
            final String paramType = param.type().toJavaType();

            sb.append(paramType).append(" ").append(paramName);
            if (i < func.params().size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(");\n\n");
    }

    private void appendHeader(final StringBuilder sb, final String packageName) {
        sb.append("/*\n");
        sb.append(" * Generated by wasmtime4j WIT code generator\n");
        sb.append(" * Generated at: ").append(LocalDateTime.now().format(DATE_FORMAT)).append("\n");
        sb.append(" *\n");
        sb.append(" * DO NOT EDIT - This file is auto-generated from WIT definitions.\n");
        sb.append(" */\n\n");
        sb.append("package ").append(packageName).append(";\n\n");
    }

    private String toEnumConstant(final String name) {
        // Convert kebab-case to SCREAMING_SNAKE_CASE
        return name.toUpperCase().replace('-', '_');
    }

    private boolean isPrimitive(final String type) {
        return switch (type) {
            case "boolean", "byte", "short", "int", "long", "float", "double", "char" -> true;
            default -> false;
        };
    }
}
