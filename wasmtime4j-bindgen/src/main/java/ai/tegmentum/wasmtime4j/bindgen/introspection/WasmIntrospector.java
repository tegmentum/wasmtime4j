/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
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

package ai.tegmentum.wasmtime4j.bindgen.introspection;

import ai.tegmentum.wasmtime4j.bindgen.BindgenException;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenFunction;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenInterface;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenModel;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenParameter;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Introspects WASM modules to extract type information for code generation.
 *
 * <p>This class analyzes WASM binaries to extract their exports and imports,
 * creating a bindgen model that can be used for Java code generation.
 */
public final class WasmIntrospector {

  private static final Logger LOGGER = Logger.getLogger(WasmIntrospector.class.getName());

  // WASM magic number and version
  private static final byte[] WASM_MAGIC = {0x00, 0x61, 0x73, 0x6D}; // \0asm
  private static final byte[] WASM_VERSION = {0x01, 0x00, 0x00, 0x00}; // version 1

  /**
   * Creates a new WasmIntrospector.
   */
  public WasmIntrospector() {
    // Default constructor
  }

  /**
   * Introspects a WASM module and extracts its type information.
   *
   * @param wasmPath the path to the WASM module
   * @return the introspected model
   * @throws BindgenException if introspection fails
   */
  public BindgenModel introspect(final Path wasmPath) throws BindgenException {
    LOGGER.info("Introspecting WASM module: " + wasmPath);

    try {
      byte[] wasmBytes = Files.readAllBytes(wasmPath);
      validateWasmHeader(wasmBytes, wasmPath.toString());

      String moduleName = extractModuleName(wasmPath);
      List<BindgenFunction> exports = parseExports(wasmBytes);
      List<BindgenFunction> imports = parseImports(wasmBytes);

      // Create interface for exports
      BindgenInterface exportInterface = BindgenInterface.builder()
          .name(moduleName + "Exports")
          .functions(exports)
          .documentation("Exported functions from " + moduleName)
          .build();

      // Create interface for imports if any
      List<BindgenInterface> interfaces = new ArrayList<>();
      interfaces.add(exportInterface);

      if (!imports.isEmpty()) {
        BindgenInterface importInterface = BindgenInterface.builder()
            .name(moduleName + "Imports")
            .functions(imports)
            .documentation("Import functions required by " + moduleName)
            .build();
        interfaces.add(importInterface);
      }

      return BindgenModel.builder()
          .name(moduleName)
          .interfaces(interfaces)
          .sourceFile(wasmPath.toString())
          .build();

    } catch (IOException e) {
      throw BindgenException.wasmIntrospectionError(wasmPath.toString(), e);
    }
  }

  /**
   * Validates the WASM module header.
   *
   * @param wasmBytes the WASM binary bytes
   * @param fileName the file name for error messages
   * @throws BindgenException if the header is invalid
   */
  private void validateWasmHeader(final byte[] wasmBytes, final String fileName)
      throws BindgenException {
    if (wasmBytes.length < 8) {
      throw new BindgenException("Invalid WASM file (too small): " + fileName);
    }

    // Check magic number
    for (int i = 0; i < WASM_MAGIC.length; i++) {
      if (wasmBytes[i] != WASM_MAGIC[i]) {
        throw new BindgenException("Invalid WASM magic number in: " + fileName);
      }
    }

    // Check version
    for (int i = 0; i < WASM_VERSION.length; i++) {
      if (wasmBytes[i + 4] != WASM_VERSION[i]) {
        LOGGER.warning("Unexpected WASM version in: " + fileName);
      }
    }
  }

  /**
   * Extracts the module name from the file path.
   *
   * @param wasmPath the path to the WASM module
   * @return the module name
   */
  private String extractModuleName(final Path wasmPath) {
    String fileName = wasmPath.getFileName().toString();
    // Remove .wasm extension
    if (fileName.endsWith(".wasm")) {
      fileName = fileName.substring(0, fileName.length() - 5);
    }
    // Convert to PascalCase
    return toPascalCase(fileName);
  }

  /**
   * Parses the export section of the WASM module.
   *
   * <p>Note: This is a simplified implementation. A full implementation would
   * use the wasmtime4j Module API to properly parse the WASM binary.
   *
   * @param wasmBytes the WASM binary bytes
   * @return the list of exported functions
   */
  private List<BindgenFunction> parseExports(final byte[] wasmBytes) {
    List<BindgenFunction> exports = new ArrayList<>();

    // TODO: Use wasmtime4j Module.getExportDescriptors() for proper parsing
    // This is a placeholder that demonstrates the expected output structure

    // For now, we detect common patterns in the binary
    // A real implementation would parse the WASM sections properly

    LOGGER.fine("Parsing exports from WASM binary (" + wasmBytes.length + " bytes)");

    return exports;
  }

  /**
   * Parses the import section of the WASM module.
   *
   * @param wasmBytes the WASM binary bytes
   * @return the list of imported functions
   */
  private List<BindgenFunction> parseImports(final byte[] wasmBytes) {
    List<BindgenFunction> imports = new ArrayList<>();

    // TODO: Use wasmtime4j Module.getImportDescriptors() for proper parsing
    // This is a placeholder that demonstrates the expected output structure

    LOGGER.fine("Parsing imports from WASM binary (" + wasmBytes.length + " bytes)");

    return imports;
  }

  /**
   * Converts a snake_case or kebab-case string to PascalCase.
   *
   * @param input the input string
   * @return the PascalCase string
   */
  private String toPascalCase(final String input) {
    StringBuilder result = new StringBuilder();
    boolean capitalizeNext = true;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c == '-' || c == '_') {
        capitalizeNext = true;
      } else if (capitalizeNext) {
        result.append(Character.toUpperCase(c));
        capitalizeNext = false;
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }

  /**
   * Maps a WASM value type string to a BindgenType.
   *
   * @param wasmType the WASM type string (e.g., "i32", "i64", "f32", "f64")
   * @return the bindgen type
   */
  private BindgenType mapWasmValueType(final String wasmType) {
    switch (wasmType.toLowerCase()) {
      case "i32":
        return BindgenType.primitive("i32");
      case "i64":
        return BindgenType.primitive("i64");
      case "f32":
        return BindgenType.primitive("f32");
      case "f64":
        return BindgenType.primitive("f64");
      case "v128":
        return BindgenType.primitive("v128");
      case "funcref":
        return BindgenType.reference("FunctionReference");
      case "externref":
        return BindgenType.reference("Object");
      default:
        return BindgenType.reference(wasmType);
    }
  }

  /**
   * Creates a BindgenFunction from parsed export/import data.
   *
   * @param name the function name
   * @param params the parameter types
   * @param results the return types
   * @return the bindgen function
   */
  private BindgenFunction createFunction(
      final String name, final List<String> params, final List<String> results) {

    BindgenFunction.Builder builder = BindgenFunction.builder().name(name);

    // Add parameters
    for (int i = 0; i < params.size(); i++) {
      String paramType = params.get(i);
      builder.addParameter(new BindgenParameter("param" + i, mapWasmValueType(paramType)));
    }

    // Add return type (WASM supports multiple returns, we take first for simplicity)
    if (!results.isEmpty()) {
      builder.returnType(mapWasmValueType(results.get(0)));
    }

    return builder.build();
  }
}
