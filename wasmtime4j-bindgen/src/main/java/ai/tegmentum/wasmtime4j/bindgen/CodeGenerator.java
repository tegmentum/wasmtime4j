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

package ai.tegmentum.wasmtime4j.bindgen;

import ai.tegmentum.wasmtime4j.bindgen.generator.JavaCodeGenerator;
import ai.tegmentum.wasmtime4j.bindgen.generator.LegacyCodeGenerator;
import ai.tegmentum.wasmtime4j.bindgen.generator.ModernCodeGenerator;
import ai.tegmentum.wasmtime4j.bindgen.introspection.WasmIntrospector;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Main orchestrator for Java binding generation.
 *
 * <p>This class coordinates the parsing of WIT files, introspection of WASM modules, and generation
 * of Java source files based on the provided configuration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * BindgenConfig config = BindgenConfig.builder()
 *     .packageName("com.example.generated")
 *     .outputDirectory(Path.of("target/generated-sources"))
 *     .codeStyle(CodeStyle.MODERN)
 *     .addWitSource(Path.of("src/main/wit"))
 *     .build();
 *
 * CodeGenerator generator = new CodeGenerator(config);
 * List<GeneratedSource> sources = generator.generate();
 * }</pre>
 */
public final class CodeGenerator {

  private static final Logger LOGGER = Logger.getLogger(CodeGenerator.class.getName());

  private final BindgenConfig config;
  private final JavaCodeGenerator codeGenerator;

  /**
   * Creates a new CodeGenerator with the specified configuration.
   *
   * @param config the bindgen configuration
   * @throws BindgenException if configuration is invalid
   */
  public CodeGenerator(final BindgenConfig config) throws BindgenException {
    this.config = Objects.requireNonNull(config, "config");
    config.validate();

    // Select appropriate code generator based on style
    if (config.getCodeStyle() == CodeStyle.MODERN) {
      this.codeGenerator = new ModernCodeGenerator(config);
    } else {
      this.codeGenerator = new LegacyCodeGenerator(config);
    }
  }

  /**
   * Generates Java source files from the configured sources.
   *
   * @return the list of generated source files
   * @throws BindgenException if generation fails
   */
  public List<GeneratedSource> generate() throws BindgenException {
    List<GeneratedSource> allSources = new ArrayList<>();

    // Process WIT sources
    if (config.hasWitSources()) {
      LOGGER.info("Processing WIT sources...");
      for (Path witPath : config.getWitSources()) {
        BindgenModel model = parseWitSource(witPath);
        List<GeneratedSource> sources = codeGenerator.generate(model);
        allSources.addAll(sources);
        LOGGER.fine("Generated " + sources.size() + " sources from " + witPath);
      }
    }

    // Process WASM sources
    if (config.hasWasmSources()) {
      LOGGER.info("Processing WASM sources...");
      for (Path wasmPath : config.getWasmSources()) {
        BindgenModel model = introspectWasmModule(wasmPath);
        List<GeneratedSource> sources = codeGenerator.generate(model);
        allSources.addAll(sources);
        LOGGER.fine("Generated " + sources.size() + " sources from " + wasmPath);
      }
    }

    LOGGER.info("Generated " + allSources.size() + " total source files");
    return allSources;
  }

  /**
   * Generates Java source files and writes them to the output directory.
   *
   * @throws BindgenException if generation or writing fails
   */
  public void generateAndWrite() throws BindgenException {
    List<GeneratedSource> sources = generate();
    Path outputDir = config.getOutputDirectory();

    LOGGER.info("Writing generated sources to " + outputDir);
    for (GeneratedSource source : sources) {
      source.writeTo(outputDir);
      LOGGER.fine("Wrote " + source.getQualifiedName());
    }
  }

  /**
   * Parses a WIT source file or directory.
   *
   * @param witPath the path to the WIT file or directory
   * @return the parsed model
   * @throws BindgenException if parsing fails
   */
  private BindgenModel parseWitSource(final Path witPath) throws BindgenException {
    // TODO: Implement WIT parsing using existing WitInterfaceParser or custom implementation
    // For now, return an empty model as a placeholder
    LOGGER.warning("WIT parsing not yet implemented for: " + witPath);
    return BindgenModel.builder()
        .name(witPath.getFileName().toString())
        .sourceFile(witPath.toString())
        .build();
  }

  /**
   * Introspects a WASM module to extract type information.
   *
   * @param wasmPath the path to the WASM module
   * @return the introspected model
   * @throws BindgenException if introspection fails
   */
  private BindgenModel introspectWasmModule(final Path wasmPath) throws BindgenException {
    WasmIntrospector introspector = new WasmIntrospector();
    return introspector.introspect(wasmPath);
  }

  /**
   * Returns the configuration used by this generator.
   *
   * @return the bindgen configuration
   */
  public BindgenConfig getConfig() {
    return config;
  }
}
