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

/** Tests for {@link BindgenParameter}. */
@DisplayName("BindgenParameter Tests")
class BindgenParameterTest {

  private static final Logger LOGGER = Logger.getLogger(BindgenParameterTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create parameter with valid name and type")
    void shouldCreateParameterWithValidNameAndType() {
      LOGGER.info("Testing constructor with valid parameters");

      BindgenType type = BindgenType.primitive("i32");
      BindgenParameter param = new BindgenParameter("count", type);

      assertThat(param.getName()).isEqualTo("count");
      assertThat(param.getType()).isEqualTo(type);
    }

    @Test
    @DisplayName("should throw NullPointerException when name is null")
    void shouldThrowWhenNameIsNull() {
      LOGGER.info("Testing constructor with null name");

      BindgenType type = BindgenType.primitive("i32");

      assertThatThrownBy(() -> new BindgenParameter(null, type))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("name");
    }

    @Test
    @DisplayName("should throw NullPointerException when type is null")
    void shouldThrowWhenTypeIsNull() {
      LOGGER.info("Testing constructor with null type");

      assertThatThrownBy(() -> new BindgenParameter("param", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("type");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName() should return parameter name")
    void getNameShouldReturnParameterName() {
      BindgenParameter param = new BindgenParameter("value", BindgenType.primitive("string"));

      assertThat(param.getName()).isEqualTo("value");
    }

    @Test
    @DisplayName("getType() should return parameter type")
    void getTypeShouldReturnParameterType() {
      BindgenType type = BindgenType.primitive("u64");
      BindgenParameter param = new BindgenParameter("offset", type);

      assertThat(param.getType()).isEqualTo(type);
      assertThat(param.getType().getName()).isEqualTo("u64");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal when name and type match")
    void shouldBeEqualWhenNameAndTypeMatch() {
      LOGGER.info("Testing equals() for matching parameters");

      BindgenType type = BindgenType.primitive("i32");
      BindgenParameter param1 = new BindgenParameter("value", type);
      BindgenParameter param2 = new BindgenParameter("value", type);

      assertThat(param1).isEqualTo(param2);
      assertThat(param1.hashCode()).isEqualTo(param2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when names differ")
    void shouldNotBeEqualWhenNamesDiffer() {
      LOGGER.info("Testing equals() for different names");

      BindgenType type = BindgenType.primitive("i32");
      BindgenParameter param1 = new BindgenParameter("value1", type);
      BindgenParameter param2 = new BindgenParameter("value2", type);

      assertThat(param1).isNotEqualTo(param2);
    }

    @Test
    @DisplayName("should not be equal when types differ")
    void shouldNotBeEqualWhenTypesDiffer() {
      LOGGER.info("Testing equals() for different types");

      BindgenParameter param1 = new BindgenParameter("value", BindgenType.primitive("i32"));
      BindgenParameter param2 = new BindgenParameter("value", BindgenType.primitive("i64"));

      assertThat(param1).isNotEqualTo(param2);
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      BindgenParameter param = new BindgenParameter("value", BindgenType.primitive("i32"));

      assertThat(param).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
      BindgenParameter param = new BindgenParameter("value", BindgenType.primitive("i32"));

      assertThat(param).isNotEqualTo("value");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      BindgenParameter param = new BindgenParameter("value", BindgenType.primitive("i32"));

      assertThat(param).isEqualTo(param);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should format as name: typeName")
    void shouldFormatAsNameColonTypeName() {
      LOGGER.info("Testing toString() output format");

      BindgenParameter param = new BindgenParameter("count", BindgenType.primitive("i32"));

      String toString = param.toString();

      assertThat(toString).isEqualTo("count: i32");
    }

    @Test
    @DisplayName("should handle complex type names")
    void shouldHandleComplexTypeNames() {
      BindgenType listType = BindgenType.list(BindgenType.primitive("string"));
      BindgenParameter param = new BindgenParameter("names", listType);

      String toString = param.toString();

      assertThat(toString).isEqualTo("names: list<string>");
    }
  }
}
