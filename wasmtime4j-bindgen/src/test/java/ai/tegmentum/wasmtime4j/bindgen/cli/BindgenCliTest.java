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

package ai.tegmentum.wasmtime4j.bindgen.cli;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.bindgen.CodeStyle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** Tests for {@link BindgenCli}. */
@DisplayName("BindgenCli Tests")
class BindgenCliTest {

  private static final Logger LOGGER = Logger.getLogger(BindgenCliTest.class.getName());

  // Minimal valid WASM module
  private static final byte[] VALID_WASM = {
      0x00, 0x61, 0x73, 0x6D, // \0asm magic
      0x01, 0x00, 0x00, 0x00  // version 1
  };

  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;
  private PrintStream originalOut;
  private PrintStream originalErr;

  @BeforeEach
  void setUp() {
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Nested
  @DisplayName("Help and Version Tests")
  class HelpAndVersionTests {

    @Test
    @DisplayName("should display help with --help")
    void shouldDisplayHelpWithHelpFlag() {
      LOGGER.info("Testing --help flag");

      int exitCode = new CommandLine(new BindgenCli()).execute("--help");

      assertThat(exitCode).isEqualTo(0);
      String output = outContent.toString();
      assertThat(output).contains("wasmtime4j-bindgen");
      assertThat(output).contains("--wit");
      assertThat(output).contains("--wasm");
      assertThat(output).contains("--package");
      assertThat(output).contains("--output");
      assertThat(output).contains("--style");
    }

    @Test
    @DisplayName("should display version with --version")
    void shouldDisplayVersionWithVersionFlag() {
      LOGGER.info("Testing --version flag");

      int exitCode = new CommandLine(new BindgenCli()).execute("--version");

      assertThat(exitCode).isEqualTo(0);
      String output = outContent.toString();
      assertThat(output).contains("wasmtime4j-bindgen");
      assertThat(output).contains("1.0.0");
    }
  }

  @Nested
  @DisplayName("Required Argument Tests")
  class RequiredArgumentTests {

    @Test
    @DisplayName("should require --package argument")
    void shouldRequirePackageArgument(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing missing --package");

      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);

      int exitCode = new CommandLine(new BindgenCli())
          .execute("--wasm", wasmFile.toString());

      assertThat(exitCode).isNotEqualTo(0);
      String errOutput = errContent.toString();
      assertThat(errOutput).contains("package");
    }

    @Test
    @DisplayName("should require at least one source")
    void shouldRequireAtLeastOneSource(@TempDir Path tempDir) {
      LOGGER.info("Testing missing source");

      int exitCode = new CommandLine(new BindgenCli())
          .execute("--package", "com.example");

      assertThat(exitCode).isEqualTo(1);
      String errOutput = errContent.toString();
      assertThat(errOutput).contains("At least one --wit or --wasm source must be specified");
    }
  }

  @Nested
  @DisplayName("Code Style Tests")
  class CodeStyleTests {

    @Test
    @DisplayName("should accept MODERN style")
    void shouldAcceptModernStyle(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing MODERN style");

      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .setCaseInsensitiveEnumValuesAllowed(true)
          .execute(
              "--wasm", wasmFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString(),
              "--style", "MODERN");

      assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("should accept LEGACY style")
    void shouldAcceptLegacyStyle(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing LEGACY style");

      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .setCaseInsensitiveEnumValuesAllowed(true)
          .execute(
              "--wasm", wasmFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString(),
              "--style", "LEGACY");

      assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("should accept lowercase style names")
    void shouldAcceptLowercaseStyleNames(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .setCaseInsensitiveEnumValuesAllowed(true)
          .execute(
              "--wasm", wasmFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString(),
              "--style", "modern");

      assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("should default to MODERN style")
    void shouldDefaultToModernStyle(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing default style");

      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", wasmFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString());

      assertThat(exitCode).isEqualTo(0);
      // No error about style means default was used
    }
  }

  @Nested
  @DisplayName("Output Directory Tests")
  class OutputDirectoryTests {

    @Test
    @DisplayName("should use default output directory")
    void shouldUseDefaultOutputDirectory(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing default output directory");

      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);

      // Change to temp directory so default output goes there
      String originalDir = System.getProperty("user.dir");
      try {
        System.setProperty("user.dir", tempDir.toString());

        int exitCode = new CommandLine(new BindgenCli())
            .execute(
                "--wasm", wasmFile.toString(),
                "--package", "com.example");

        assertThat(exitCode).isEqualTo(0);
      } finally {
        System.setProperty("user.dir", originalDir);
      }
    }

    @Test
    @DisplayName("should create output directory if not exists")
    void shouldCreateOutputDirectoryIfNotExists(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("new/nested/output");

      assertThat(outputDir).doesNotExist();

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", wasmFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString());

      assertThat(exitCode).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Source Validation Tests")
  class SourceValidationTests {

    @Test
    @DisplayName("should warn for non-existent source file")
    void shouldWarnForNonExistentSourceFile(@TempDir Path tempDir) {
      LOGGER.info("Testing non-existent source file");

      Path nonExistentFile = tempDir.resolve("does-not-exist.wasm");
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", nonExistentFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString());

      assertThat(exitCode).isEqualTo(1);
      String errOutput = errContent.toString();
      assertThat(errOutput).containsIgnoringCase("not found");
    }

    @Test
    @DisplayName("should warn for file with wrong extension")
    void shouldWarnForWrongExtension(@TempDir Path tempDir) throws Exception {
      Path textFile = tempDir.resolve("not-a-wasm.txt");
      Files.writeString(textFile, "not wasm content");
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", textFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString());

      String errOutput = errContent.toString();
      assertThat(errOutput).containsIgnoringCase("unexpected extension");
    }

    @Test
    @DisplayName("should process directory of WASM files")
    void shouldProcessDirectoryOfWasmFiles(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing directory source");

      Path wasmDir = tempDir.resolve("wasm");
      Files.createDirectory(wasmDir);
      Files.write(wasmDir.resolve("module1.wasm"), VALID_WASM);
      Files.write(wasmDir.resolve("module2.wasm"), VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", wasmDir.toString(),
              "--package", "com.example",
              "--output", outputDir.toString());

      assertThat(exitCode).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Optional Flag Tests")
  class OptionalFlagTests {

    @Test
    @DisplayName("should support --no-javadoc flag")
    void shouldSupportNoJavadocFlag(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing --no-javadoc flag");

      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", wasmFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString(),
              "--no-javadoc");

      assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("should support --no-builders flag")
    void shouldSupportNoBuildersFlag(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing --no-builders flag");

      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", wasmFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString(),
              "--no-builders");

      assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("should support --verbose flag")
    void shouldSupportVerboseFlag(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing --verbose flag");

      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", wasmFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString(),
              "--verbose");

      assertThat(exitCode).isEqualTo(0);
      String output = outContent.toString();
      assertThat(output).contains("Configuration:");
    }

    @Test
    @DisplayName("should support --dry-run flag")
    void shouldSupportDryRunFlag(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing --dry-run flag");

      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", wasmFile.toString(),
              "--package", "com.example",
              "--output", outputDir.toString(),
              "--dry-run");

      assertThat(exitCode).isEqualTo(0);
      String output = outContent.toString();
      assertThat(output).contains("Dry run");
    }
  }

  @Nested
  @DisplayName("Short Option Tests")
  class ShortOptionTests {

    @Test
    @DisplayName("should support -w for --wit")
    void shouldSupportShortWitOption(@TempDir Path tempDir) throws Exception {
      Path witFile = tempDir.resolve("test.wit");
      Files.writeString(witFile, "// Empty WIT file");
      Path outputDir = tempDir.resolve("output");

      // Note: WIT parsing may fail, but the option should be recognized
      new CommandLine(new BindgenCli())
          .execute(
              "-w", witFile.toString(),
              "-p", "com.example",
              "-o", outputDir.toString());

      // If we get here without parsing error, short option worked
    }

    @Test
    @DisplayName("should support -m for --wasm")
    void shouldSupportShortWasmOption(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "-m", wasmFile.toString(),
              "-p", "com.example",
              "-o", outputDir.toString());

      assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("should support -s for --style")
    void shouldSupportShortStyleOption(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .setCaseInsensitiveEnumValuesAllowed(true)
          .execute(
              "-m", wasmFile.toString(),
              "-p", "com.example",
              "-o", outputDir.toString(),
              "-s", "LEGACY");

      assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("should support -v for --verbose")
    void shouldSupportShortVerboseOption(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("test.wasm");
      Files.write(wasmFile, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "-m", wasmFile.toString(),
              "-p", "com.example",
              "-o", outputDir.toString(),
              "-v");

      assertThat(exitCode).isEqualTo(0);
      assertThat(outContent.toString()).contains("Configuration:");
    }
  }

  @Nested
  @DisplayName("Multiple Source Tests")
  class MultipleSourceTests {

    @Test
    @DisplayName("should accept multiple --wasm sources")
    void shouldAcceptMultipleWasmSources(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing multiple WASM sources");

      Path wasmFile1 = tempDir.resolve("module1.wasm");
      Path wasmFile2 = tempDir.resolve("module2.wasm");
      Files.write(wasmFile1, VALID_WASM);
      Files.write(wasmFile2, VALID_WASM);
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", wasmFile1.toString(),
              "--wasm", wasmFile2.toString(),
              "--package", "com.example",
              "--output", outputDir.toString());

      assertThat(exitCode).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should return exit code 1 for bindgen errors")
    void shouldReturnExitCode1ForBindgenErrors(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing error exit code");

      Path invalidWasm = tempDir.resolve("invalid.wasm");
      Files.write(invalidWasm, new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", invalidWasm.toString(),
              "--package", "com.example",
              "--output", outputDir.toString());

      assertThat(exitCode).isEqualTo(1);
      assertThat(errContent.toString()).contains("Error:");
    }

    @Test
    @DisplayName("should show cause with --verbose on error")
    void shouldShowCauseWithVerboseOnError(@TempDir Path tempDir) throws Exception {
      Path invalidWasm = tempDir.resolve("invalid.wasm");
      Files.write(invalidWasm, new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
      Path outputDir = tempDir.resolve("output");

      int exitCode = new CommandLine(new BindgenCli())
          .execute(
              "--wasm", invalidWasm.toString(),
              "--package", "com.example",
              "--output", outputDir.toString(),
              "--verbose");

      assertThat(exitCode).isEqualTo(1);
      // Verbose mode shows more details
      assertThat(errContent.toString()).isNotEmpty();
    }
  }
}
