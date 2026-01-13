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
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenModel;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenParameter;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenType;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenVariantCase;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link ModernCodeGenerator}. */
@DisplayName("ModernCodeGenerator Tests")
class ModernCodeGeneratorTest {

  private static final Logger LOGGER = Logger.getLogger(ModernCodeGeneratorTest.class.getName());
  private static final String TEST_PACKAGE = "com.example.generated";

  private ModernCodeGenerator generator;
  private BindgenConfig config;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    config = BindgenConfig.builder()
        .codeStyle(CodeStyle.MODERN)
        .packageName(TEST_PACKAGE)
        .outputDirectory(tempDir)
        .generateJavadoc(true)
        .generateBuilders(false)
        .build();
    generator = new ModernCodeGenerator(config);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create generator with valid config")
    void shouldCreateGeneratorWithValidConfig(@TempDir Path tempDir) {
      LOGGER.info("Testing constructor");

      BindgenConfig cfg = BindgenConfig.builder()
          .codeStyle(CodeStyle.MODERN)
          .packageName("com.test")
          .outputDirectory(tempDir)
          .build();

      ModernCodeGenerator gen = new ModernCodeGenerator(cfg);

      assertThat(gen).isNotNull();
    }
  }

  @Nested
  @DisplayName("Record Generation Tests")
  class RecordGenerationTests {

    @Test
    @DisplayName("should generate class for record type with fields")
    void shouldGenerateClassForRecordWithFields() throws BindgenException {
      LOGGER.info("Testing record generation with fields");

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
    @DisplayName("should generate record-style getters (fieldName() not getFieldName())")
    void shouldGenerateRecordStyleGetters() throws BindgenException {
      LOGGER.info("Testing record-style getters");

      BindgenType recordType = BindgenType.builder()
          .name("point")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("x", BindgenType.primitive("f32")))
          .addField(new BindgenField("y", BindgenType.primitive("f32")))
          .build();

      GeneratedSource source = generator.generateType(recordType);

      String content = source.getContent();
      // Modern style uses fieldName() not getFieldName()
      assertThat(content).contains("public float x()");
      assertThat(content).contains("public float y()");
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
    @DisplayName("should generate empty record with no fields")
    void shouldGenerateEmptyRecordWithNoFields() throws BindgenException {
      LOGGER.info("Testing empty record generation");

      BindgenType emptyRecord = BindgenType.builder()
          .name("empty")
          .kind(BindgenType.Kind.RECORD)
          .build();

      GeneratedSource source = generator.generateType(emptyRecord);

      String content = source.getContent();
      assertThat(content).contains("public final class Empty");
    }

    @Test
    @DisplayName("should include Javadoc when configured")
    void shouldIncludeJavadocWhenConfigured() throws BindgenException {
      BindgenType recordType = BindgenType.builder()
          .name("documented")
          .kind(BindgenType.Kind.RECORD)
          .documentation("This is a documented type.")
          .addField(new BindgenField("value", BindgenType.primitive("i32"), "The value field."))
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
    @DisplayName("should generate interface for variant type")
    void shouldGenerateInterfaceForVariant() throws BindgenException {
      LOGGER.info("Testing variant generation");

      BindgenType variantType = BindgenType.builder()
          .name("result-type")
          .kind(BindgenType.Kind.VARIANT)
          .addCase(new BindgenVariantCase("success", BindgenType.primitive("string")))
          .addCase(new BindgenVariantCase("error", BindgenType.primitive("string")))
          .build();

      GeneratedSource source = generator.generateType(variantType);

      String content = source.getContent();
      assertThat(content).contains("public interface ResultType");
    }

    @Test
    @DisplayName("should generate nested case classes for variant")
    void shouldGenerateNestedCaseClasses() throws BindgenException {
      BindgenType variantType = BindgenType.builder()
          .name("option-int")
          .kind(BindgenType.Kind.VARIANT)
          .addCase(new BindgenVariantCase("some", BindgenType.primitive("i32")))
          .addCase(new BindgenVariantCase("none"))
          .build();

      GeneratedSource source = generator.generateType(variantType);

      String content = source.getContent();
      // Classes nested in interfaces are implicitly public and static
      assertThat(content).contains("final class Some");
      assertThat(content).contains("final class None");
    }

    @Test
    @DisplayName("should generate payload accessor for case with payload")
    void shouldGeneratePayloadAccessor() throws BindgenException {
      BindgenType variantType = BindgenType.builder()
          .name("maybe-string")
          .kind(BindgenType.Kind.VARIANT)
          .addCase(new BindgenVariantCase("just", BindgenType.primitive("string")))
          .build();

      GeneratedSource source = generator.generateType(variantType);

      String content = source.getContent();
      assertThat(content).contains("public String value()");
    }
  }

  @Nested
  @DisplayName("Enum Generation Tests")
  class EnumGenerationTests {

    @Test
    @DisplayName("should generate Java enum for WIT enum type")
    void shouldGenerateJavaEnumForWitEnum() throws BindgenException {
      LOGGER.info("Testing enum generation");

      BindgenType enumType = BindgenType.builder()
          .name("color")
          .kind(BindgenType.Kind.ENUM)
          .addEnumValue("red")
          .addEnumValue("green")
          .addEnumValue("blue")
          .build();

      GeneratedSource source = generator.generateType(enumType);

      String content = source.getContent();
      assertThat(content).contains("public enum Color");
      assertThat(content).contains("RED");
      assertThat(content).contains("GREEN");
      assertThat(content).contains("BLUE");
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
    @DisplayName("should generate handle accessor for resource")
    void shouldGenerateHandleAccessor() throws BindgenException {
      BindgenType resourceType = BindgenType.builder()
          .name("stream")
          .kind(BindgenType.Kind.RESOURCE)
          .build();

      GeneratedSource source = generator.generateType(resourceType);

      String content = source.getContent();
      // Modern style uses handle() not getHandle()
      assertThat(content).contains("public long handle()");
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

    @Test
    @DisplayName("should generate abstract method declarations")
    void shouldGenerateAbstractMethods() throws BindgenException {
      BindgenFunction func = BindgenFunction.builder()
          .name("do-something")
          .build();

      BindgenInterface iface = BindgenInterface.builder()
          .name("service")
          .addFunction(func)
          .build();

      GeneratedSource source = generator.generateInterface(iface);

      String content = source.getContent();
      // Interface methods are implicitly public abstract
      assertThat(content).contains("void doSomething()");
    }
  }

  @Nested
  @DisplayName("Model Generation Tests")
  class ModelGenerationTests {

    @Test
    @DisplayName("should generate all types from model")
    void shouldGenerateAllTypesFromModel() throws BindgenException {
      LOGGER.info("Testing model generation");

      BindgenType type1 = BindgenType.builder()
          .name("type-one")
          .kind(BindgenType.Kind.RECORD)
          .addField(new BindgenField("value", BindgenType.primitive("i32")))
          .build();

      BindgenType type2 = BindgenType.builder()
          .name("type-two")
          .kind(BindgenType.Kind.ENUM)
          .addEnumValue("a")
          .addEnumValue("b")
          .build();

      BindgenModel model = BindgenModel.builder()
          .name("test-model")
          .addType(type1)
          .addType(type2)
          .build();

      List<GeneratedSource> sources = generator.generate(model);

      assertThat(sources).hasSize(2);
      assertThat(sources).anyMatch(s -> s.getClassName().equals("TypeOne"));
      assertThat(sources).anyMatch(s -> s.getClassName().equals("TypeTwo"));
    }

    @Test
    @DisplayName("should generate interfaces from model")
    void shouldGenerateInterfacesFromModel() throws BindgenException {
      BindgenInterface iface = BindgenInterface.builder()
          .name("api")
          .addFunction(BindgenFunction.builder().name("call").build())
          .build();

      BindgenModel model = BindgenModel.builder()
          .name("api-model")
          .addInterface(iface)
          .build();

      List<GeneratedSource> sources = generator.generate(model);

      assertThat(sources).anyMatch(s -> s.getClassName().equals("Api"));
    }
  }

  @Nested
  @DisplayName("Package and Naming Tests")
  class PackageAndNamingTests {

    @Test
    @DisplayName("should use configured package name")
    void shouldUseConfiguredPackageName() throws BindgenException {
      BindgenType type = BindgenType.builder()
          .name("test")
          .kind(BindgenType.Kind.RECORD)
          .build();

      GeneratedSource source = generator.generateType(type);

      assertThat(source.getPackageName()).isEqualTo(TEST_PACKAGE);
      assertThat(source.getContent()).contains("package " + TEST_PACKAGE);
    }

    @Test
    @DisplayName("should convert kebab-case names to PascalCase")
    void shouldConvertKebabCaseToPascalCase() throws BindgenException {
      BindgenType type = BindgenType.builder()
          .name("my-kebab-case-type")
          .kind(BindgenType.Kind.RECORD)
          .build();

      GeneratedSource source = generator.generateType(type);

      assertThat(source.getClassName()).isEqualTo("MyKebabCaseType");
    }
  }
}
