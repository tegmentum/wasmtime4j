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

/** Tests for {@link BindgenVariantCase}. */
@DisplayName("BindgenVariantCase Tests")
class BindgenVariantCaseTest {

  private static final Logger LOGGER = Logger.getLogger(BindgenVariantCaseTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create variant case with name only")
    void shouldCreateVariantCaseWithNameOnly() {
      LOGGER.info("Testing single-arg constructor");

      BindgenVariantCase variantCase = new BindgenVariantCase("none");

      assertThat(variantCase.getName()).isEqualTo("none");
      assertThat(variantCase.getPayload()).isEmpty();
      assertThat(variantCase.hasPayload()).isFalse();
      assertThat(variantCase.getDocumentation()).isEmpty();
    }

    @Test
    @DisplayName("should create variant case with name and payload")
    void shouldCreateVariantCaseWithNameAndPayload() {
      LOGGER.info("Testing two-arg constructor with payload");

      BindgenType payload = BindgenType.primitive("i32");
      BindgenVariantCase variantCase = new BindgenVariantCase("some", payload);

      assertThat(variantCase.getName()).isEqualTo("some");
      assertThat(variantCase.getPayload()).hasValue(payload);
      assertThat(variantCase.hasPayload()).isTrue();
      assertThat(variantCase.getDocumentation()).isEmpty();
    }

    @Test
    @DisplayName("should create variant case with name, payload, and documentation")
    void shouldCreateVariantCaseWithDocumentation() {
      LOGGER.info("Testing three-arg constructor with documentation");

      BindgenType payload = BindgenType.primitive("string");
      BindgenVariantCase variantCase =
          new BindgenVariantCase("error", payload, "An error occurred");

      assertThat(variantCase.getName()).isEqualTo("error");
      assertThat(variantCase.getPayload()).hasValue(payload);
      assertThat(variantCase.hasPayload()).isTrue();
      assertThat(variantCase.getDocumentation()).hasValue("An error occurred");
    }

    @Test
    @DisplayName("should create variant case with null payload and documentation")
    void shouldCreateVariantCaseWithNullPayloadAndDocumentation() {
      LOGGER.info("Testing three-arg constructor with null payload");

      BindgenVariantCase variantCase = new BindgenVariantCase("empty", null, "No value");

      assertThat(variantCase.getName()).isEqualTo("empty");
      assertThat(variantCase.getPayload()).isEmpty();
      assertThat(variantCase.hasPayload()).isFalse();
      assertThat(variantCase.getDocumentation()).hasValue("No value");
    }

    @Test
    @DisplayName("should throw NullPointerException when name is null (single arg)")
    void shouldThrowWhenNameIsNullSingleArg() {
      LOGGER.info("Testing single-arg constructor with null name");

      assertThatThrownBy(() -> new BindgenVariantCase(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("name");
    }

    @Test
    @DisplayName("should throw NullPointerException when name is null (two args)")
    void shouldThrowWhenNameIsNullTwoArgs() {
      LOGGER.info("Testing two-arg constructor with null name");

      BindgenType payload = BindgenType.primitive("i32");

      assertThatThrownBy(() -> new BindgenVariantCase(null, payload))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("name");
    }

    @Test
    @DisplayName("should throw NullPointerException when name is null (three args)")
    void shouldThrowWhenNameIsNullThreeArgs() {
      LOGGER.info("Testing three-arg constructor with null name");

      BindgenType payload = BindgenType.primitive("i32");

      assertThatThrownBy(() -> new BindgenVariantCase(null, payload, "docs"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("name");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName() should return case name")
    void getNameShouldReturnCaseName() {
      BindgenVariantCase variantCase = new BindgenVariantCase("myCase");

      assertThat(variantCase.getName()).isEqualTo("myCase");
    }

    @Test
    @DisplayName("getPayload() should return empty when no payload")
    void getPayloadShouldReturnEmptyWhenNoPayload() {
      BindgenVariantCase variantCase = new BindgenVariantCase("noPayload");

      assertThat(variantCase.getPayload()).isEmpty();
    }

    @Test
    @DisplayName("getPayload() should return value when payload exists")
    void getPayloadShouldReturnValueWhenPayloadExists() {
      BindgenType payload = BindgenType.primitive("u64");
      BindgenVariantCase variantCase = new BindgenVariantCase("withPayload", payload);

      assertThat(variantCase.getPayload()).hasValue(payload);
    }

    @Test
    @DisplayName("hasPayload() should return false when no payload")
    void hasPayloadShouldReturnFalseWhenNoPayload() {
      BindgenVariantCase variantCase = new BindgenVariantCase("noPayload");

      assertThat(variantCase.hasPayload()).isFalse();
    }

    @Test
    @DisplayName("hasPayload() should return true when payload exists")
    void hasPayloadShouldReturnTrueWhenPayloadExists() {
      BindgenVariantCase variantCase =
          new BindgenVariantCase("withPayload", BindgenType.primitive("i32"));

      assertThat(variantCase.hasPayload()).isTrue();
    }

    @Test
    @DisplayName("getDocumentation() should return empty when not set")
    void getDocumentationShouldReturnEmptyWhenNotSet() {
      BindgenVariantCase variantCase = new BindgenVariantCase("case");

      assertThat(variantCase.getDocumentation()).isEmpty();
    }

    @Test
    @DisplayName("getDocumentation() should return value when set")
    void getDocumentationShouldReturnValueWhenSet() {
      BindgenVariantCase variantCase = new BindgenVariantCase("case", null, "Case documentation");

      assertThat(variantCase.getDocumentation()).hasValue("Case documentation");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal when name and payload match (no payload)")
    void shouldBeEqualWhenNameMatchNoPayload() {
      LOGGER.info("Testing equals() for matching cases without payload");

      BindgenVariantCase case1 = new BindgenVariantCase("none");
      BindgenVariantCase case2 = new BindgenVariantCase("none");

      assertThat(case1).isEqualTo(case2);
      assertThat(case1.hashCode()).isEqualTo(case2.hashCode());
    }

    @Test
    @DisplayName("should be equal when name and payload match (with payload)")
    void shouldBeEqualWhenNameAndPayloadMatch() {
      LOGGER.info("Testing equals() for matching cases with payload");

      BindgenType payload = BindgenType.primitive("i32");
      BindgenVariantCase case1 = new BindgenVariantCase("some", payload);
      BindgenVariantCase case2 = new BindgenVariantCase("some", payload);

      assertThat(case1).isEqualTo(case2);
      assertThat(case1.hashCode()).isEqualTo(case2.hashCode());
    }

    @Test
    @DisplayName("should be equal even when documentation differs")
    void shouldBeEqualEvenWhenDocumentationDiffers() {
      LOGGER.info("Testing equals() ignores documentation");

      BindgenType payload = BindgenType.primitive("i32");
      BindgenVariantCase case1 = new BindgenVariantCase("case", payload, "Doc 1");
      BindgenVariantCase case2 = new BindgenVariantCase("case", payload, "Doc 2");

      assertThat(case1).isEqualTo(case2);
      assertThat(case1.hashCode()).isEqualTo(case2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when names differ")
    void shouldNotBeEqualWhenNamesDiffer() {
      LOGGER.info("Testing equals() for different names");

      BindgenVariantCase case1 = new BindgenVariantCase("case1");
      BindgenVariantCase case2 = new BindgenVariantCase("case2");

      assertThat(case1).isNotEqualTo(case2);
    }

    @Test
    @DisplayName("should not be equal when payloads differ")
    void shouldNotBeEqualWhenPayloadsDiffer() {
      LOGGER.info("Testing equals() for different payloads");

      BindgenVariantCase case1 = new BindgenVariantCase("case", BindgenType.primitive("i32"));
      BindgenVariantCase case2 = new BindgenVariantCase("case", BindgenType.primitive("i64"));

      assertThat(case1).isNotEqualTo(case2);
    }

    @Test
    @DisplayName("should not be equal when one has payload and other does not")
    void shouldNotBeEqualWhenPayloadPresenceDiffers() {
      LOGGER.info("Testing equals() for payload presence difference");

      BindgenVariantCase case1 = new BindgenVariantCase("case");
      BindgenVariantCase case2 = new BindgenVariantCase("case", BindgenType.primitive("i32"));

      assertThat(case1).isNotEqualTo(case2);
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      BindgenVariantCase variantCase = new BindgenVariantCase("case");

      assertThat(variantCase).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
      BindgenVariantCase variantCase = new BindgenVariantCase("case");

      assertThat(variantCase).isNotEqualTo("case");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      BindgenVariantCase variantCase = new BindgenVariantCase("case");

      assertThat(variantCase).isEqualTo(variantCase);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should include name in toString() for case without payload")
    void shouldIncludeNameInToStringWithoutPayload() {
      LOGGER.info("Testing toString() output without payload");

      BindgenVariantCase variantCase = new BindgenVariantCase("none");

      String toString = variantCase.toString();

      assertThat(toString).contains("name='none'");
      assertThat(toString).startsWith("BindgenVariantCase{");
      assertThat(toString).endsWith("}");
      assertThat(toString).doesNotContain("payload=");
    }

    @Test
    @DisplayName("should include name and payload in toString() for case with payload")
    void shouldIncludeNameAndPayloadInToStringWithPayload() {
      LOGGER.info("Testing toString() output with payload");

      BindgenVariantCase variantCase = new BindgenVariantCase("some", BindgenType.primitive("i32"));

      String toString = variantCase.toString();

      assertThat(toString).contains("name='some'");
      assertThat(toString).contains("payload=");
      assertThat(toString).startsWith("BindgenVariantCase{");
      assertThat(toString).endsWith("}");
    }
  }
}
