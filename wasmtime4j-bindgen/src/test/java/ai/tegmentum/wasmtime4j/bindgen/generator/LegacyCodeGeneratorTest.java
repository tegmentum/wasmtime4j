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

package ai.tegmentum.wasmtime4j.bindgen.generator;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.bindgen.BindgenConfig;
import ai.tegmentum.wasmtime4j.bindgen.BindgenException;
import ai.tegmentum.wasmtime4j.bindgen.CodeStyle;
import ai.tegmentum.wasmtime4j.bindgen.GeneratedSource;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenField;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenFunction;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenInterface;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenParameter;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenType;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenVariantCase;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link LegacyCodeGenerator}. */
@DisplayName("LegacyCodeGenerator Tests")
class LegacyCodeGeneratorTest {

  private static final Logger LOGGER = Logger.getLogger(LegacyCodeGeneratorTest.class.getName());
  private static final String TEST_PACKAGE = "com.example.legacy";

  private LegacyCodeGenerator generator;
  private BindgenConfig config;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    config = BindgenConfig.builder()
        .codeStyle(CodeStyle.LEGACY)
        .packageName(TEST_PACKAGE)
        .outputDirectory(tempDir)
        .generateJavadoc(true)
        .generateBuilders(true)
        .build();
    generator = new LegacyCodeGenerator(config);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create generator with valid config")
    void shouldCreateGeneratorWithValidConfig(@TempDir Path tempDir) {
      LOGGER.info("Testing constructor");

      BindgenConfig cfg = BindgenConfig.builder()
          .codeStyle(CodeStyle.LEGACY)
          .packageName("com.test")
          .outputDirectory(tempDir)
          .build();

      LegacyCodeGenerator gen = new LegacyCodeGenerator(cfg);

      assertThat(gen).isNotNull();
    }
  }

  @Nested
  @DisplayName("Record Generation Tests")
  class RecordGenerationTests {

    @Test
    @DisplayName("should generate POJO class for record type")
    void shouldGeneratePojoClassForRecord() throws BindgenException {
      LOGGER.info("Testing POJO record generation");

      BindgenType recordType = BindgenType.builder()
          .name("person")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("first-name", BindgenType.primitive("string")))
          .addField(new BindgenField("age", BindgenType.primitive("u32")))
          .build();

      GeneratedSource source = generator.generateType(recordType);

      String content = source.getContent();
      assertThat(content).contains("public final class Person");
      assertThat(content).contains("private final String firstName");
      assertThat(content).contains("private final int age");
    }

    @Test
    @DisplayName("should generate traditional getXxx style getters")
    void shouldGenerateTraditionalGetters() throws BindgenException {
      LOGGER.info("Testing traditional getters");

      BindgenType recordType = BindgenType.builder()
          .name("point")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("x", BindgenType.primitive("f32")))
          .addField(new BindgenField("y", BindgenType.primitive("f32")))
          .build();

      GeneratedSource source = generator.generateType(recordType);

      String content = source.getContent();
      // Legacy style uses getXxx() not xxx()
      assertThat(content).contains("public float getX()");
      assertThat(content).contains("public float getY()");
    }

    @Test
    @DisplayName("should generate builder when configured")
    void shouldGenerateBuilderWhenConfigured() throws BindgenException {
      LOGGER.info("Testing builder generation");

      BindgenType recordType = BindgenType.builder()
          .name("config")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("name", BindgenType.primitive("string")))
          .addField(new BindgenField("value", BindgenType.primitive("i32")))
          .build();

      GeneratedSource source = generator.generateType(recordType);

      String content = source.getContent();
      assertThat(content).contains("public static final class Builder");
      assertThat(content).contains("public static Builder builder()");
      assertThat(content).contains("public Builder name(");
      assertThat(content).contains("public Builder value(");
      assertThat(content).contains("public Config build()");
    }

    @Test
    @DisplayName("should not generate builder when disabled")
    void shouldNotGenerateBuilderWhenDisabled(@TempDir Path tempDir) throws BindgenException {
      LOGGER.info("Testing builder disabled");

      BindgenConfig noBuilderConfig = BindgenConfig.builder()
          .codeStyle(CodeStyle.LEGACY)
          .packageName(TEST_PACKAGE)
          .outputDirectory(tempDir)
          .generateBuilders(false)
          .build();
      LegacyCodeGenerator noBuilderGenerator = new LegacyCodeGenerator(noBuilderConfig);

      BindgenType recordType = BindgenType.builder()
          .name("simple")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("value", BindgenType.primitive("i32")))
          .build();

      GeneratedSource source = noBuilderGenerator.generateType(recordType);

      String content = source.getContent();
      assertThat(content).doesNotContain("class Builder");
      assertThat(content).doesNotContain("public static Builder builder()");
    }

    @Test
    @DisplayName("should not generate builder for empty record")
    void shouldNotGenerateBuilderForEmptyRecord() throws BindgenException {
      BindgenType emptyRecord = BindgenType.builder()
          .name("empty")
          .kind(BindgenType.Kind.RECORD)
          .build();

      GeneratedSource source = generator.generateType(emptyRecord);

      String content = source.getContent();
      // Builder is not useful for empty records
      assertThat(content).doesNotContain("class Builder");
    }

    @Test
    @DisplayName("should generate equals method")
    void shouldGenerateEqualsMethod() throws BindgenException {
      BindgenType recordType = BindgenType.builder()
          .name("simple")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("value", BindgenType.primitive("i32")))
          .build();

      GeneratedSource source = generator.generateType(recordType);

      String content = source.getContent();
      assertThat(content).contains("@Override");
      assertThat(content).contains("public boolean equals(Object obj)");
      assertThat(content).contains("if (this == obj) return true");
    }

    @Test
    @DisplayName("should generate hashCode method")
    void shouldGenerateHashCodeMethod() throws BindgenException {
      BindgenType recordType = BindgenType.builder()
          .name("simple")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("value", BindgenType.primitive("i32")))
          .build();

      GeneratedSource source = generator.generateType(recordType);

      String content = source.getContent();
      assertThat(content).contains("@Override");
      assertThat(content).contains("public int hashCode()");
    }

    @Test
    @DisplayName("should generate toString method")
    void shouldGenerateToStringMethod() throws BindgenException {
      BindgenType recordType = BindgenType.builder()
          .name("simple")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("value", BindgenType.primitive("i32")))
          .build();

      GeneratedSource source = generator.generateType(recordType);

      String content = source.getContent();
      assertThat(content).contains("@Override");
      assertThat(content).contains("public String toString()");
    }

    @Test
    @DisplayName("should include Javadoc when configured")
    void shouldIncludeJavadocWhenConfigured() throws BindgenException {
      BindgenType recordType = BindgenType.builder()
          .name("documented")
          .kind(BindgenType.Kind.RECORD)
          .documentation("This is a documented type.")
          .addField(new BindgenField("value", BindgenType.primitive("i32"), "The value."))
          .build();

      GeneratedSource source = generator.generateType(recordType);

      String content = source.getContent();
      assertThat(content).contains("This is a documented type.");
    }
  }

  @Nested
  @DisplayName("Variant Generation Tests")
  class VariantGenerationTests {

    @Test
    @DisplayName("should generate abstract class for variant")
    void shouldGenerateAbstractClassForVariant() throws BindgenException {
      LOGGER.info("Testing variant abstract class generation");

      BindgenType variantType = BindgenType.builder()
          .name("result-type")
          .kind(BindgenType.Kind.VARIANT)
          .addCase(new BindgenVariantCase("success", BindgenType.primitive("string")))
          .addCase(new BindgenVariantCase("error", BindgenType.primitive("string")))
          .build();

      GeneratedSource source = generator.generateType(variantType);

      String content = source.getContent();
      assertThat(content).contains("public abstract class ResultType");
    }

    @Test
    @DisplayName("should generate visitor interface for variant")
    void shouldGenerateVisitorInterface() throws BindgenException {
      LOGGER.info("Testing visitor pattern generation");

      BindgenType variantType = BindgenType.builder()
          .name("option")
          .kind(BindgenType.Kind.VARIANT)
          .addCase(new BindgenVariantCase("some", BindgenType.primitive("i32")))
          .addCase(new BindgenVariantCase("none"))
          .build();

      GeneratedSource source = generator.generateType(variantType);

      String content = source.getContent();
      assertThat(content).contains("public interface Visitor<T>");
      assertThat(content).contains("T visitSome(Some value)");
      assertThat(content).contains("T visitNone(None value)");
    }

    @Test
    @DisplayName("should generate accept method for visitor pattern")
    void shouldGenerateAcceptMethod() throws BindgenException {
      BindgenType variantType = BindgenType.builder()
          .name("either")
          .kind(BindgenType.Kind.VARIANT)
          .addCase(new BindgenVariantCase("left", BindgenType.primitive("string")))
          .addCase(new BindgenVariantCase("right", BindgenType.primitive("i32")))
          .build();

      GeneratedSource source = generator.generateType(variantType);

      String content = source.getContent();
      assertThat(content).contains("public abstract <T> T accept(Visitor<T> visitor)");
    }

    @Test
    @DisplayName("should generate case classes extending base class")
    void shouldGenerateCaseClassesExtendingBase() throws BindgenException {
      BindgenType variantType = BindgenType.builder()
          .name("my-variant")
          .kind(BindgenType.Kind.VARIANT)
          .addCase(new BindgenVariantCase("case-one"))
          .addCase(new BindgenVariantCase("case-two", BindgenType.primitive("i32")))
          .build();

      GeneratedSource source = generator.generateType(variantType);

      String content = source.getContent();
      assertThat(content).contains("public static final class CaseOne extends MyVariant");
      assertThat(content).contains("public static final class CaseTwo extends MyVariant");
    }

    @Test
    @DisplayName("should generate getValue for case with payload")
    void shouldGenerateGetValueForPayloadCase() throws BindgenException {
      BindgenType variantType = BindgenType.builder()
          .name("wrapped")
          .kind(BindgenType.Kind.VARIANT)
          .addCase(new BindgenVariantCase("value", BindgenType.primitive("string")))
          .build();

      GeneratedSource source = generator.generateType(variantType);

      String content = source.getContent();
      // Legacy style uses getValue() not value()
      assertThat(content).contains("public String getValue()");
    }
  }

  @Nested
  @DisplayName("Resource Generation Tests")
  class ResourceGenerationTests {

    @Test
    @DisplayName("should generate resource class implementing AutoCloseable")
    void shouldGenerateResourceClass() throws BindgenException {
      LOGGER.info("Testing resource generation");

      BindgenType resourceType = BindgenType.builder()
          .name("file-handle")
          .kind(BindgenType.Kind.RESOURCE)
          .build();

      GeneratedSource source = generator.generateType(resourceType);

      String content = source.getContent();
      assertThat(content).contains("public class FileHandle implements AutoCloseable");
      assertThat(content).contains("private final long handle");
      assertThat(content).contains("public void close()");
    }

    @Test
    @DisplayName("should generate getHandle accessor for resource")
    void shouldGenerateGetHandleAccessor() throws BindgenException {
      BindgenType resourceType = BindgenType.builder()
          .name("stream")
          .kind(BindgenType.Kind.RESOURCE)
          .build();

      GeneratedSource source = generator.generateType(resourceType);

      String content = source.getContent();
      // Legacy style uses getHandle() not handle()
      assertThat(content).contains("public long getHandle()");
    }
  }

  @Nested
  @DisplayName("Enum Generation Tests")
  class EnumGenerationTests {

    @Test
    @DisplayName("should generate Java enum for WIT enum")
    void shouldGenerateJavaEnum() throws BindgenException {
      LOGGER.info("Testing enum generation");

      BindgenType enumType = BindgenType.builder()
          .name("direction")
          .kind(BindgenType.Kind.ENUM)
          .addEnumValue("north")
          .addEnumValue("south")
          .addEnumValue("east")
          .addEnumValue("west")
          .build();

      GeneratedSource source = generator.generateType(enumType);

      String content = source.getContent();
      assertThat(content).contains("public enum Direction");
      assertThat(content).contains("NORTH");
      assertThat(content).contains("SOUTH");
      assertThat(content).contains("EAST");
      assertThat(content).contains("WEST");
    }
  }

  @Nested
  @DisplayName("Interface Generation Tests")
  class InterfaceGenerationTests {

    @Test
    @DisplayName("should generate Java interface for WIT interface")
    void shouldGenerateJavaInterface() throws BindgenException {
      LOGGER.info("Testing interface generation");

      BindgenFunction func = BindgenFunction.builder()
          .name("process")
          .addParameter(new BindgenParameter("input", BindgenType.primitive("string")))
          .returnType(BindgenType.primitive("i32"))
          .build();

      BindgenInterface iface = BindgenInterface.builder()
          .name("processor")
          .addFunction(func)
          .build();

      GeneratedSource source = generator.generateInterface(iface);

      String content = source.getContent();
      assertThat(content).contains("public interface Processor");
      assertThat(content).contains("int process(String input)");
    }
  }

  @Nested
  @DisplayName("Comparison with Modern Generator")
  class ComparisonWithModernTests {

    @Test
    @DisplayName("legacy should use getXxx while modern uses xxx")
    void legacyShouldUseGetXxxWhileModernUsesXxx(@TempDir Path tempDir) throws BindgenException {
      LOGGER.info("Testing getter style difference");

      BindgenType recordType = BindgenType.builder()
          .name("test")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("value", BindgenType.primitive("i32")))
          .build();

      // Legacy generator
      GeneratedSource legacySource = generator.generateType(recordType);

      // Modern generator
      BindgenConfig modernConfig = BindgenConfig.builder()
          .codeStyle(CodeStyle.MODERN)
          .packageName(TEST_PACKAGE)
          .outputDirectory(tempDir)
          .build();
      ModernCodeGenerator modernGen = new ModernCodeGenerator(modernConfig);
      GeneratedSource modernSource = modernGen.generateType(recordType);

      assertThat(legacySource.getContent()).contains("public int getValue()");
      assertThat(modernSource.getContent()).contains("public int value()");
    }

    @Test
    @DisplayName("legacy should use abstract class for variant while modern uses interface")
    void legacyShouldUseAbstractClassWhileModernUsesInterface(@TempDir Path tempDir)
        throws BindgenException {
      LOGGER.info("Testing variant type difference");

      BindgenType variantType = BindgenType.builder()
          .name("option")
          .kind(BindgenType.Kind.VARIANT)
          .addCase(new BindgenVariantCase("some", BindgenType.primitive("i32")))
          .addCase(new BindgenVariantCase("none"))
          .build();

      // Legacy generator
      GeneratedSource legacySource = generator.generateType(variantType);

      // Modern generator
      BindgenConfig modernConfig = BindgenConfig.builder()
          .codeStyle(CodeStyle.MODERN)
          .packageName(TEST_PACKAGE)
          .outputDirectory(tempDir)
          .build();
      ModernCodeGenerator modernGen = new ModernCodeGenerator(modernConfig);
      GeneratedSource modernSource = modernGen.generateType(variantType);

      assertThat(legacySource.getContent()).contains("public abstract class Option");
      assertThat(modernSource.getContent()).contains("public interface Option");
    }
  }
}
