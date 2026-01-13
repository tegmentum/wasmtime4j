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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link BindgenFunction}. */
@DisplayName("BindgenFunction Tests")
class BindgenFunctionTest {

  private static final Logger LOGGER = Logger.getLogger(BindgenFunctionTest.class.getName());

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create function with builder and default values")
    void shouldCreateFunctionWithDefaultValues() {
      LOGGER.info("Testing builder with default values");

      BindgenFunction function = BindgenFunction.builder()
          .name("doSomething")
          .build();

      assertThat(function.getName()).isEqualTo("doSomething");
      assertThat(function.getParameters()).isEmpty();
      assertThat(function.getReturnType()).isEmpty();
      assertThat(function.getDocumentation()).isEmpty();
      assertThat(function.isAsync()).isFalse();
      assertThat(function.isConstructor()).isFalse();
      assertThat(function.isStatic()).isFalse();
      assertThat(function.hasReturnType()).isFalse();
    }

    @Test
    @DisplayName("should create function with parameters using addParameter")
    void shouldCreateFunctionWithParametersUsingAddParameter() {
      LOGGER.info("Testing builder with addParameter()");

      BindgenType i32Type = BindgenType.primitive("i32");
      BindgenType stringType = BindgenType.primitive("string");
      BindgenParameter param1 = new BindgenParameter("count", i32Type);
      BindgenParameter param2 = new BindgenParameter("name", stringType);

      BindgenFunction function = BindgenFunction.builder()
          .name("greet")
          .addParameter(param1)
          .addParameter(param2)
          .build();

      assertThat(function.getParameters()).hasSize(2);
      assertThat(function.getParameters()).containsExactly(param1, param2);
    }

    @Test
    @DisplayName("should create function with parameters using name and type shorthand")
    void shouldCreateFunctionWithParametersUsingShorthand() {
      LOGGER.info("Testing builder with addParameter(name, type) shorthand");

      BindgenType i32Type = BindgenType.primitive("i32");

      BindgenFunction function = BindgenFunction.builder()
          .name("increment")
          .addParameter("value", i32Type)
          .build();

      assertThat(function.getParameters()).hasSize(1);
      assertThat(function.getParameters().get(0).getName()).isEqualTo("value");
      assertThat(function.getParameters().get(0).getType()).isEqualTo(i32Type);
    }

    @Test
    @DisplayName("should create function with parameters() method")
    void shouldCreateFunctionWithParametersMethod() {
      LOGGER.info("Testing builder with parameters() list method");

      List<BindgenParameter> params = Arrays.asList(
          new BindgenParameter("a", BindgenType.primitive("i32")),
          new BindgenParameter("b", BindgenType.primitive("i32")));

      BindgenFunction function = BindgenFunction.builder()
          .name("add")
          .parameters(params)
          .build();

      assertThat(function.getParameters()).hasSize(2);
    }

    @Test
    @DisplayName("should create function with return type")
    void shouldCreateFunctionWithReturnType() {
      LOGGER.info("Testing builder with return type");

      BindgenType returnType = BindgenType.primitive("string");

      BindgenFunction function = BindgenFunction.builder()
          .name("getName")
          .returnType(returnType)
          .build();

      assertThat(function.getReturnType()).hasValue(returnType);
      assertThat(function.hasReturnType()).isTrue();
    }

    @Test
    @DisplayName("should create function with documentation")
    void shouldCreateFunctionWithDocumentation() {
      LOGGER.info("Testing builder with documentation");

      BindgenFunction function = BindgenFunction.builder()
          .name("calculate")
          .documentation("Calculates the result based on input")
          .build();

      assertThat(function.getDocumentation()).hasValue("Calculates the result based on input");
    }

    @Test
    @DisplayName("should create async function")
    void shouldCreateAsyncFunction() {
      LOGGER.info("Testing builder with async flag");

      BindgenFunction function = BindgenFunction.builder()
          .name("fetchData")
          .async(true)
          .build();

      assertThat(function.isAsync()).isTrue();
    }

    @Test
    @DisplayName("should create constructor function")
    void shouldCreateConstructorFunction() {
      LOGGER.info("Testing builder with constructor flag");

      BindgenFunction function = BindgenFunction.builder()
          .name("new")
          .constructor(true)
          .build();

      assertThat(function.isConstructor()).isTrue();
    }

    @Test
    @DisplayName("should create static function")
    void shouldCreateStaticFunction() {
      LOGGER.info("Testing builder with static flag");

      BindgenFunction function = BindgenFunction.builder()
          .name("getInstance")
          .staticMethod(true)
          .build();

      assertThat(function.isStatic()).isTrue();
    }

    @Test
    @DisplayName("should create fully configured function")
    void shouldCreateFullyConfiguredFunction() {
      LOGGER.info("Testing builder with all options");

      BindgenType i32Type = BindgenType.primitive("i32");
      BindgenType stringType = BindgenType.primitive("string");

      BindgenFunction function = BindgenFunction.builder()
          .name("process")
          .addParameter("input", i32Type)
          .returnType(stringType)
          .documentation("Processes the input and returns a string")
          .async(true)
          .constructor(false)
          .staticMethod(true)
          .build();

      assertThat(function.getName()).isEqualTo("process");
      assertThat(function.getParameters()).hasSize(1);
      assertThat(function.getReturnType()).hasValue(stringType);
      assertThat(function.getDocumentation()).isPresent();
      assertThat(function.isAsync()).isTrue();
      assertThat(function.isConstructor()).isFalse();
      assertThat(function.isStatic()).isTrue();
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName() should return function name")
    void getNameShouldReturnFunctionName() {
      BindgenFunction function = BindgenFunction.builder()
          .name("testFunction")
          .build();

      assertThat(function.getName()).isEqualTo("testFunction");
    }

    @Test
    @DisplayName("getParameters() should return empty list when no parameters")
    void getParametersShouldReturnEmptyListWhenNoParameters() {
      BindgenFunction function = BindgenFunction.builder()
          .name("noParams")
          .build();

      assertThat(function.getParameters()).isEmpty();
    }

    @Test
    @DisplayName("getReturnType() should return empty when no return type")
    void getReturnTypeShouldReturnEmptyWhenNoReturnType() {
      BindgenFunction function = BindgenFunction.builder()
          .name("voidFunction")
          .build();

      assertThat(function.getReturnType()).isEmpty();
    }

    @Test
    @DisplayName("hasReturnType() should return false for void functions")
    void hasReturnTypeShouldReturnFalseForVoidFunctions() {
      BindgenFunction function = BindgenFunction.builder()
          .name("voidFunction")
          .build();

      assertThat(function.hasReturnType()).isFalse();
    }

    @Test
    @DisplayName("hasReturnType() should return true when return type is set")
    void hasReturnTypeShouldReturnTrueWhenReturnTypeIsSet() {
      BindgenFunction function = BindgenFunction.builder()
          .name("returningFunction")
          .returnType(BindgenType.primitive("i32"))
          .build();

      assertThat(function.hasReturnType()).isTrue();
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal when name and parameters match")
    void shouldBeEqualWhenNameAndParametersMatch() {
      LOGGER.info("Testing equals() for matching functions");

      BindgenType i32Type = BindgenType.primitive("i32");

      BindgenFunction function1 = BindgenFunction.builder()
          .name("add")
          .addParameter("a", i32Type)
          .addParameter("b", i32Type)
          .build();

      BindgenFunction function2 = BindgenFunction.builder()
          .name("add")
          .addParameter("a", i32Type)
          .addParameter("b", i32Type)
          .build();

      assertThat(function1).isEqualTo(function2);
      assertThat(function1.hashCode()).isEqualTo(function2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when names differ")
    void shouldNotBeEqualWhenNamesDiffer() {
      LOGGER.info("Testing equals() for different names");

      BindgenFunction function1 = BindgenFunction.builder().name("func1").build();
      BindgenFunction function2 = BindgenFunction.builder().name("func2").build();

      assertThat(function1).isNotEqualTo(function2);
    }

    @Test
    @DisplayName("should not be equal when parameters differ")
    void shouldNotBeEqualWhenParametersDiffer() {
      LOGGER.info("Testing equals() for different parameters");

      BindgenFunction function1 = BindgenFunction.builder()
          .name("func")
          .addParameter("a", BindgenType.primitive("i32"))
          .build();

      BindgenFunction function2 = BindgenFunction.builder()
          .name("func")
          .addParameter("b", BindgenType.primitive("i64"))
          .build();

      assertThat(function1).isNotEqualTo(function2);
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      BindgenFunction function = BindgenFunction.builder().name("func").build();

      assertThat(function).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
      BindgenFunction function = BindgenFunction.builder().name("func").build();

      assertThat(function).isNotEqualTo("func");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      BindgenFunction function = BindgenFunction.builder().name("func").build();

      assertThat(function).isEqualTo(function);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should include name in toString()")
    void shouldIncludeNameInToString() {
      LOGGER.info("Testing toString() output");

      BindgenFunction function = BindgenFunction.builder()
          .name("myFunction")
          .build();

      String toString = function.toString();

      assertThat(toString).contains("name='myFunction'");
      assertThat(toString).startsWith("BindgenFunction{");
    }

    @Test
    @DisplayName("should include parameter count in toString()")
    void shouldIncludeParameterCountInToString() {
      BindgenFunction function = BindgenFunction.builder()
          .name("func")
          .addParameter("a", BindgenType.primitive("i32"))
          .addParameter("b", BindgenType.primitive("i32"))
          .build();

      String toString = function.toString();

      assertThat(toString).contains("params=2");
    }

    @Test
    @DisplayName("should include return type in toString() when present")
    void shouldIncludeReturnTypeInToStringWhenPresent() {
      BindgenFunction function = BindgenFunction.builder()
          .name("func")
          .returnType(BindgenType.primitive("string"))
          .build();

      String toString = function.toString();

      assertThat(toString).contains("returns=string");
    }

    @Test
    @DisplayName("should not include returns when no return type")
    void shouldNotIncludeReturnsWhenNoReturnType() {
      BindgenFunction function = BindgenFunction.builder()
          .name("func")
          .build();

      String toString = function.toString();

      assertThat(toString).doesNotContain("returns=");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("parameters list should be immutable")
    void parametersListShouldBeImmutable() {
      LOGGER.info("Testing that parameters list is immutable");

      BindgenFunction function = BindgenFunction.builder()
          .name("func")
          .addParameter("a", BindgenType.primitive("i32"))
          .build();

      List<BindgenParameter> params = function.getParameters();

      assertThatThrownBy(() -> params.add(
          new BindgenParameter("b", BindgenType.primitive("i32"))))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
