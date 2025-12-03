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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for WebAssembly Interface Type (WIT) files.
 *
 * <p>Parses .wit files into an AST representation for code generation.
 *
 * @since 1.0.0
 */
public final class WitParser {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "^\\s*package\\s+([a-zA-Z][a-zA-Z0-9-]*:[a-zA-Z][a-zA-Z0-9-]*(?:@[0-9]+\\.[0-9]+\\.[0-9]+)?);?"
    );

    private static final Pattern WORLD_PATTERN = Pattern.compile(
        "\\bworld\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*\\{"
    );

    private static final Pattern INTERFACE_PATTERN = Pattern.compile(
        "^\\s*interface\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*\\{"
    );

    private static final Pattern RECORD_PATTERN = Pattern.compile(
        "^\\s*record\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*\\{"
    );

    private static final Pattern VARIANT_PATTERN = Pattern.compile(
        "^\\s*variant\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*\\{"
    );

    private static final Pattern ENUM_PATTERN = Pattern.compile(
        "^\\s*enum\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*\\{"
    );

    private static final Pattern FLAGS_PATTERN = Pattern.compile(
        "^\\s*flags\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*\\{"
    );

    private static final Pattern TYPE_ALIAS_PATTERN = Pattern.compile(
        "^\\s*type\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*=\\s*(.+)\\s*;?"
    );

    private static final Pattern RESOURCE_PATTERN = Pattern.compile(
        "^\\s*resource\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*\\{"
    );

    private static final Pattern IMPORT_FUNC_PATTERN = Pattern.compile(
        "^\\s*import\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*:\\s*func\\s*\\((.*)\\)\\s*(?:->\\s*(.+))?;?"
    );

    private static final Pattern EXPORT_FUNC_PATTERN = Pattern.compile(
        "^\\s*export\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*:\\s*func\\s*\\((.*)\\)\\s*(?:->\\s*(.+))?;?"
    );

    private static final Pattern IMPORT_INTERFACE_PATTERN = Pattern.compile(
        "^\\s*import\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*;?"
    );

    private static final Pattern EXPORT_INTERFACE_PATTERN = Pattern.compile(
        "^\\s*export\\s+([a-zA-Z][a-zA-Z0-9-]*)\\s*;?"
    );

    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "^\\s*([a-zA-Z][a-zA-Z0-9-]*)\\s*:\\s*(.+?)\\s*,?$"
    );

    private static final Pattern FUNC_PATTERN = Pattern.compile(
        "^\\s*([a-zA-Z][a-zA-Z0-9-]*)\\s*:\\s*func\\s*\\((.*)\\)\\s*(?:->\\s*(.+))?;?"
    );

    private String content;
    private int pos;

    /**
     * Parses a WIT file from the given path.
     *
     * @param path the path to the .wit file
     * @return the parsed WitWorld
     * @throws IOException if the file cannot be read
     * @throws WitParseException if the file cannot be parsed
     */
    public WitWorld parse(final Path path) throws IOException, WitParseException {
        this.content = Files.readString(path);
        this.pos = 0;

        // Remove comments
        content = removeComments(content);

        String packageName = null;
        WitWorld.Builder worldBuilder = null;

        // Parse package declaration
        final Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
        if (packageMatcher.find()) {
            packageName = packageMatcher.group(1);
        }

        // Find and parse world
        final Matcher worldMatcher = WORLD_PATTERN.matcher(content);
        if (worldMatcher.find()) {
            final String worldName = worldMatcher.group(1);
            worldBuilder = WitWorld.builder(worldName);
            if (packageName != null) {
                worldBuilder.packageName(packageName);
            }

            // Parse world body
            final int worldStart = worldMatcher.end();
            final int worldEnd = findMatchingBrace(content, worldStart - 1);
            if (worldEnd == -1) {
                throw new WitParseException("Unclosed world block at position " + worldStart);
            }

            final String worldBody = content.substring(worldStart, worldEnd);
            parseWorldBody(worldBody, worldBuilder);
        }

        if (worldBuilder == null) {
            throw new WitParseException("No world definition found in WIT file: " + path);
        }

        return worldBuilder.build();
    }

    private void parseWorldBody(final String body, final WitWorld.Builder builder) throws WitParseException {
        final String[] lines = body.split("\n");
        int i = 0;

        while (i < lines.length) {
            final String line = lines[i].trim();

            if (line.isEmpty() || line.startsWith("//")) {
                i++;
                continue;
            }

            // Parse record
            Matcher matcher = RECORD_PATTERN.matcher(line);
            if (matcher.find()) {
                final String recordName = matcher.group(1);
                final StringBuilder recordBody = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("}")) {
                    recordBody.append(lines[i]).append("\n");
                    i++;
                }
                builder.addType(parseRecord(recordName, recordBody.toString()));
                i++;
                continue;
            }

            // Parse enum
            matcher = ENUM_PATTERN.matcher(line);
            if (matcher.find()) {
                final String enumName = matcher.group(1);
                final StringBuilder enumBody = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("}")) {
                    enumBody.append(lines[i]).append("\n");
                    i++;
                }
                builder.addType(parseEnum(enumName, enumBody.toString()));
                i++;
                continue;
            }

            // Parse variant
            matcher = VARIANT_PATTERN.matcher(line);
            if (matcher.find()) {
                final String variantName = matcher.group(1);
                final StringBuilder variantBody = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("}")) {
                    variantBody.append(lines[i]).append("\n");
                    i++;
                }
                builder.addType(parseVariant(variantName, variantBody.toString()));
                i++;
                continue;
            }

            // Parse flags
            matcher = FLAGS_PATTERN.matcher(line);
            if (matcher.find()) {
                final String flagsName = matcher.group(1);
                final StringBuilder flagsBody = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("}")) {
                    flagsBody.append(lines[i]).append("\n");
                    i++;
                }
                builder.addType(parseFlags(flagsName, flagsBody.toString()));
                i++;
                continue;
            }

            // Parse type alias
            matcher = TYPE_ALIAS_PATTERN.matcher(line);
            if (matcher.find()) {
                final String aliasName = matcher.group(1);
                final String targetType = matcher.group(2).trim();
                builder.addType(new WitDefinition.TypeAlias(aliasName, parseType(targetType)));
                i++;
                continue;
            }

            // Parse import function
            matcher = IMPORT_FUNC_PATTERN.matcher(line);
            if (matcher.find()) {
                final String funcName = matcher.group(1);
                final String params = matcher.group(2);
                final String results = matcher.group(3);
                builder.addImport(parseFunction(funcName, params, results));
                i++;
                continue;
            }

            // Parse export function
            matcher = EXPORT_FUNC_PATTERN.matcher(line);
            if (matcher.find()) {
                final String funcName = matcher.group(1);
                final String params = matcher.group(2);
                final String results = matcher.group(3);
                builder.addExport(parseFunction(funcName, params, results));
                i++;
                continue;
            }

            // Parse import interface reference
            matcher = IMPORT_INTERFACE_PATTERN.matcher(line);
            if (matcher.find() && !line.contains("func")) {
                builder.addImportedInterface(matcher.group(1));
                i++;
                continue;
            }

            // Parse export interface reference
            matcher = EXPORT_INTERFACE_PATTERN.matcher(line);
            if (matcher.find() && !line.contains("func")) {
                builder.addExportedInterface(matcher.group(1));
                i++;
                continue;
            }

            i++;
        }
    }

    private WitDefinition.RecordDef parseRecord(final String name, final String body) throws WitParseException {
        final List<WitDefinition.Field> fields = new ArrayList<>();
        final String[] lines = body.split("\n");

        for (final String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                continue;
            }

            final Matcher matcher = FIELD_PATTERN.matcher(trimmed);
            if (matcher.find()) {
                final String fieldName = matcher.group(1);
                final String fieldType = matcher.group(2).replaceAll(",\\s*$", "").trim();
                fields.add(new WitDefinition.Field(fieldName, parseType(fieldType)));
            }
        }

        return new WitDefinition.RecordDef(name, fields);
    }

    private WitDefinition.EnumDef parseEnum(final String name, final String body) {
        final List<String> values = new ArrayList<>();
        final String[] lines = body.split("\n");

        for (final String line : lines) {
            final String trimmed = line.trim().replaceAll(",\\s*$", "");
            if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
                values.add(trimmed);
            }
        }

        return new WitDefinition.EnumDef(name, values);
    }

    private WitDefinition.VariantDef parseVariant(final String name, final String body) throws WitParseException {
        final List<WitDefinition.Case> cases = new ArrayList<>();
        final String[] lines = body.split("\n");

        for (final String line : lines) {
            final String trimmed = line.trim().replaceAll(",\\s*$", "");
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                continue;
            }

            // Check for case with payload: case-name(type)
            final int parenStart = trimmed.indexOf('(');
            if (parenStart > 0) {
                final String caseName = trimmed.substring(0, parenStart).trim();
                final int parenEnd = trimmed.lastIndexOf(')');
                if (parenEnd > parenStart) {
                    final String payloadType = trimmed.substring(parenStart + 1, parenEnd).trim();
                    cases.add(new WitDefinition.Case(caseName, parseType(payloadType)));
                } else {
                    cases.add(new WitDefinition.Case(caseName, null));
                }
            } else {
                // No payload
                cases.add(new WitDefinition.Case(trimmed, null));
            }
        }

        return new WitDefinition.VariantDef(name, cases);
    }

    private WitDefinition.FlagsDef parseFlags(final String name, final String body) {
        final List<String> flags = new ArrayList<>();
        final String[] lines = body.split("\n");

        for (final String line : lines) {
            final String trimmed = line.trim().replaceAll(",\\s*$", "");
            if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
                flags.add(trimmed);
            }
        }

        return new WitDefinition.FlagsDef(name, flags);
    }

    private WitDefinition.FuncDef parseFunction(final String name, final String params, final String results)
            throws WitParseException {
        final List<WitDefinition.Param> paramList = new ArrayList<>();
        final List<WitType> resultList = new ArrayList<>();

        // Parse parameters
        if (params != null && !params.trim().isEmpty()) {
            final String[] paramParts = splitParams(params);
            for (final String param : paramParts) {
                final String trimmed = param.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                final int colonPos = trimmed.indexOf(':');
                if (colonPos > 0) {
                    final String paramName = trimmed.substring(0, colonPos).trim();
                    final String paramType = trimmed.substring(colonPos + 1).trim();
                    paramList.add(new WitDefinition.Param(paramName, parseType(paramType)));
                }
            }
        }

        // Parse results
        if (results != null && !results.trim().isEmpty()) {
            final String resultsTrimmed = results.trim();
            // Check for tuple return: (type1, type2)
            if (resultsTrimmed.startsWith("tuple<") || resultsTrimmed.startsWith("(")) {
                resultList.add(parseType(resultsTrimmed));
            } else {
                // Single return type
                resultList.add(parseType(resultsTrimmed));
            }
        }

        return new WitDefinition.FuncDef(name, paramList, resultList);
    }

    private WitType parseType(final String typeStr) throws WitParseException {
        final String type = typeStr.trim();

        // Primitive types
        return switch (type) {
            case "bool" -> new WitType.Bool();
            case "s8" -> new WitType.S8();
            case "u8" -> new WitType.U8();
            case "s16" -> new WitType.S16();
            case "u16" -> new WitType.U16();
            case "s32" -> new WitType.S32();
            case "u32" -> new WitType.U32();
            case "s64" -> new WitType.S64();
            case "u64" -> new WitType.U64();
            case "f32", "float32" -> new WitType.F32();
            case "f64", "float64" -> new WitType.F64();
            case "char" -> new WitType.Char();
            case "string" -> new WitType.WitString();
            default -> {
                // List type
                if (type.startsWith("list<")) {
                    final String inner = extractGenericInner(type, "list");
                    yield new WitType.WitList(parseType(inner));
                }
                // Option type
                if (type.startsWith("option<")) {
                    final String inner = extractGenericInner(type, "option");
                    yield new WitType.WitOption(parseType(inner));
                }
                // Result type
                if (type.startsWith("result<")) {
                    yield parseResultType(type);
                }
                // Tuple type
                if (type.startsWith("tuple<")) {
                    yield parseTupleType(type);
                }
                // Named type reference
                yield new WitType.TypeRef(type);
            }
        };
    }

    private WitType.WitResult parseResultType(final String type) throws WitParseException {
        final String inner = extractGenericInner(type, "result");
        final String[] parts = splitTopLevel(inner, ',');

        WitType okType = null;
        WitType errType = null;

        if (parts.length >= 1 && !parts[0].trim().equals("_")) {
            okType = parseType(parts[0].trim());
        }
        if (parts.length >= 2 && !parts[1].trim().equals("_")) {
            errType = parseType(parts[1].trim());
        }

        return new WitType.WitResult(okType, errType);
    }

    private WitType.WitTuple parseTupleType(final String type) throws WitParseException {
        final String inner = extractGenericInner(type, "tuple");
        final String[] parts = splitTopLevel(inner, ',');

        final List<WitType> types = new ArrayList<>();
        for (final String part : parts) {
            final String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                types.add(parseType(trimmed));
            }
        }

        return new WitType.WitTuple(types);
    }

    private String extractGenericInner(final String type, final String prefix) {
        final int start = type.indexOf('<') + 1;
        final int end = findMatchingAngleBracket(type, start - 1);
        return type.substring(start, end);
    }

    private int findMatchingAngleBracket(final String str, final int openPos) {
        int depth = 0;
        for (int i = openPos; i < str.length(); i++) {
            if (str.charAt(i) == '<') {
                depth++;
            } else if (str.charAt(i) == '>') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return str.length();
    }

    private int findMatchingBrace(final String str, final int openPos) {
        int depth = 0;
        for (int i = openPos; i < str.length(); i++) {
            if (str.charAt(i) == '{') {
                depth++;
            } else if (str.charAt(i) == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String[] splitParams(final String params) {
        return splitTopLevel(params, ',');
    }

    private String[] splitTopLevel(final String str, final char delimiter) {
        final List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;

        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c == '<' || c == '(' || c == '{') {
                depth++;
            } else if (c == '>' || c == ')' || c == '}') {
                depth--;
            } else if (c == delimiter && depth == 0) {
                parts.add(str.substring(start, i));
                start = i + 1;
            }
        }

        if (start < str.length()) {
            parts.add(str.substring(start));
        }

        return parts.toArray(new String[0]);
    }

    private String removeComments(final String content) {
        final StringBuilder result = new StringBuilder();
        final String[] lines = content.split("\n");

        boolean inBlockComment = false;

        for (final String line : lines) {
            if (inBlockComment) {
                final int endIdx = line.indexOf("*/");
                if (endIdx >= 0) {
                    inBlockComment = false;
                    result.append(line.substring(endIdx + 2)).append("\n");
                }
                continue;
            }

            String processedLine = line;

            // Remove block comments on single line
            while (processedLine.contains("/*") && processedLine.contains("*/")) {
                final int startIdx = processedLine.indexOf("/*");
                final int endIdx = processedLine.indexOf("*/");
                processedLine = processedLine.substring(0, startIdx) + processedLine.substring(endIdx + 2);
            }

            // Start of block comment without end
            if (processedLine.contains("/*")) {
                final int startIdx = processedLine.indexOf("/*");
                processedLine = processedLine.substring(0, startIdx);
                inBlockComment = true;
            }

            // Remove line comments
            final int commentIdx = processedLine.indexOf("//");
            if (commentIdx >= 0) {
                processedLine = processedLine.substring(0, commentIdx);
            }

            result.append(processedLine).append("\n");
        }

        return result.toString();
    }

    /**
     * Exception thrown when WIT parsing fails.
     */
    public static class WitParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public WitParseException(final String message) {
            super(message);
        }

        public WitParseException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
