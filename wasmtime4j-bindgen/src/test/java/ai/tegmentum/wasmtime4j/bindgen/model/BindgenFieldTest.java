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

package ai.tegmentum.wasmtime4j.bindgen.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link BindgenField}. */
@DisplayName("BindgenField Tests")
class BindgenFieldTest {

  private static final Logger LOGGER = Logger.getLogger(BindgenFieldTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create field with name and type only")
    void shouldCreateFieldWithNameAndTypeOnly() {
      LOGGER.info("Testing two-arg constructor");

      BindgenType type = BindgenType.primitive("string");
      BindgenField field = new BindgenField("name", type);

      assertThat(field.getName()).isEqualTo("name");
      assertThat(field.getType()).isEqualTo(type);
      assertThat(field.getDocumentation()).isEmpty();
    }

    @Test
    @DisplayName("should create field with name, type, and documentation")
    void shouldCreateFieldWithDocumentation() {
      LOGGER.info("Testing three-arg constructor with documentation");

      BindgenType type = BindgenType.primitive("i32");
      BindgenField field = new BindgenField("age", type, "The person's age in years");

      assertThat(field.getName()).isEqualTo("age");
      assertThat(field.getType()).isEqualTo(type);
      assertThat(field.getDocumentation()).hasValue("The person's age in years");
    }

    @Test
    @DisplayName("should create field with null documentation")
    void shouldCreateFieldWithNullDocumentation() {
      LOGGER.info("Testing three-arg constructor with null documentation");

      BindgenType type = BindgenType.primitive("bool");
      BindgenField field = new BindgenField("active", type, null);

      assertThat(field.getName()).isEqualTo("active");
      assertThat(field.getDocumentation()).isEmpty();
    }

    @Test
    @DisplayName("should throw NullPointerException when name is null")
    void shouldThrowWhenNameIsNull() {
      LOGGER.info("Testing constructor with null name");

      BindgenType type = BindgenType.primitive("i32");

      assertThatThrownBy(() -> new BindgenField(null, type))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("name");
    }

    @Test
    @DisplayName("should throw NullPointerException when type is null")
    void shouldThrowWhenTypeIsNull() {
      LOGGER.info("Testing constructor with null type");

      assertThatThrownBy(() -> new BindgenField("field", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("type");
    }

    @Test
    @DisplayName("should throw NullPointerException when name is null with documentation")
    void shouldThrowWhenNameIsNullWithDocumentation() {
      LOGGER.info("Testing three-arg constructor with null name");

      BindgenType type = BindgenType.primitive("i32");

      assertThatThrownBy(() -> new BindgenField(null, type, "docs"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("name");
    }

    @Test
    @DisplayName("should throw NullPointerException when type is null with documentation")
    void shouldThrowWhenTypeIsNullWithDocumentation() {
      LOGGER.info("Testing three-arg constructor with null type");

      assertThatThrownBy(() -> new BindgenField("field", null, "docs"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("type");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName() should return field name")
    void getNameShouldReturnFieldName() {
      BindgenField field = new BindgenField("myField", BindgenType.primitive("string"));

      assertThat(field.getName()).isEqualTo("myField");
    }

    @Test
    @DisplayName("getType() should return field type")
    void getTypeShouldReturnFieldType() {
      BindgenType type = BindgenType.primitive("u64");
      BindgenField field = new BindgenField("counter", type);

      assertThat(field.getType()).isEqualTo(type);
      assertThat(field.getType().getName()).isEqualTo("u64");
    }

    @Test
    @DisplayName("getDocumentation() should return empty when not set")
    void getDocumentationShouldReturnEmptyWhenNotSet() {
      BindgenField field = new BindgenField("field", BindgenType.primitive("i32"));

      assertThat(field.getDocumentation()).isEmpty();
    }

    @Test
    @DisplayName("getDocumentation() should return value when set")
    void getDocumentationShouldReturnValueWhenSet() {
      BindgenField field = new BindgenField("field", BindgenType.primitive("i32"), "Field docs");

      assertThat(field.getDocumentation()).hasValue("Field docs");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal when name and type match")
    void shouldBeEqualWhenNameAndTypeMatch() {
      LOGGER.info("Testing equals() for matching fields");

      BindgenType type = BindgenType.primitive("i32");
      BindgenField field1 = new BindgenField("value", type);
      BindgenField field2 = new BindgenField("value", type);

      assertThat(field1).isEqualTo(field2);
      assertThat(field1.hashCode()).isEqualTo(field2.hashCode());
    }

    @Test
    @DisplayName("should be equal even when documentation differs")
    void shouldBeEqualEvenWhenDocumentationDiffers() {
      LOGGER.info("Testing equals() ignores documentation");

      BindgenType type = BindgenType.primitive("i32");
      BindgenField field1 = new BindgenField("value", type, "Doc 1");
      BindgenField field2 = new BindgenField("value", type, "Doc 2");

      assertThat(field1).isEqualTo(field2);
      assertThat(field1.hashCode()).isEqualTo(field2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when names differ")
    void shouldNotBeEqualWhenNamesDiffer() {
      LOGGER.info("Testing equals() for different names");

      BindgenType type = BindgenType.primitive("i32");
      BindgenField field1 = new BindgenField("field1", type);
      BindgenField field2 = new BindgenField("field2", type);

      assertThat(field1).isNotEqualTo(field2);
    }

    @Test
    @DisplayName("should not be equal when types differ")
    void shouldNotBeEqualWhenTypesDiffer() {
      LOGGER.info("Testing equals() for different types");

      BindgenField field1 = new BindgenField("field", BindgenType.primitive("i32"));
      BindgenField field2 = new BindgenField("field", BindgenType.primitive("i64"));

      assertThat(field1).isNotEqualTo(field2);
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      BindgenField field = new BindgenField("field", BindgenType.primitive("i32"));

      assertThat(field).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
      BindgenField field = new BindgenField("field", BindgenType.primitive("i32"));

      assertThat(field).isNotEqualTo("field");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      BindgenField field = new BindgenField("field", BindgenType.primitive("i32"));

      assertThat(field).isEqualTo(field);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should include name and type in toString()")
    void shouldIncludeNameAndTypeInToString() {
      LOGGER.info("Testing toString() output");

      BindgenField field = new BindgenField("myField", BindgenType.primitive("string"));

      String toString = field.toString();

      assertThat(toString).contains("name='myField'");
      assertThat(toString).contains("type=");
      assertThat(toString).startsWith("BindgenField{");
      assertThat(toString).endsWith("}");
    }
  }
}
