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

/** Tests for {@link BindgenInterface}. */
@DisplayName("BindgenInterface Tests")
class BindgenInterfaceTest {

  private static final Logger LOGGER = Logger.getLogger(BindgenInterfaceTest.class.getName());

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create interface with builder and default values")
    void shouldCreateInterfaceWithDefaultValues() {
      LOGGER.info("Testing builder with default values");

      BindgenInterface iface = BindgenInterface.builder()
          .name("my-interface")
          .build();

      assertThat(iface.getName()).isEqualTo("my-interface");
      assertThat(iface.getPackageName()).isEmpty();
      assertThat(iface.getTypes()).isEmpty();
      assertThat(iface.getFunctions()).isEmpty();
      assertThat(iface.getDocumentation()).isEmpty();
    }

    @Test
    @DisplayName("should create interface with package name")
    void shouldCreateInterfaceWithPackageName() {
      LOGGER.info("Testing builder with package name");

      BindgenInterface iface = BindgenInterface.builder()
          .name("types")
          .packageName("wasi:io")
          .build();

      assertThat(iface.getName()).isEqualTo("types");
      assertThat(iface.getPackageName()).hasValue("wasi:io");
    }

    @Test
    @DisplayName("should create interface with types using addType")
    void shouldCreateInterfaceWithTypesUsingAddType() {
      LOGGER.info("Testing builder with addType()");

      BindgenType type1 = BindgenType.primitive("i32");
      BindgenType type2 = BindgenType.builder().name("MyRecord").kind(BindgenType.Kind.RECORD).build();

      BindgenInterface iface = BindgenInterface.builder()
          .name("types")
          .addType(type1)
          .addType(type2)
          .build();

      assertThat(iface.getTypes()).hasSize(2);
      assertThat(iface.getTypes()).containsExactly(type1, type2);
    }

    @Test
    @DisplayName("should create interface with types() method")
    void shouldCreateInterfaceWithTypesMethod() {
      LOGGER.info("Testing builder with types() list method");

      List<BindgenType> types = Arrays.asList(
          BindgenType.primitive("string"),
          BindgenType.primitive("bool"));

      BindgenInterface iface = BindgenInterface.builder()
          .name("primitives")
          .types(types)
          .build();

      assertThat(iface.getTypes()).hasSize(2);
    }

    @Test
    @DisplayName("should create interface with functions using addFunction")
    void shouldCreateInterfaceWithFunctionsUsingAddFunction() {
      LOGGER.info("Testing builder with addFunction()");

      BindgenFunction func1 = BindgenFunction.builder().name("get").build();
      BindgenFunction func2 = BindgenFunction.builder().name("set").build();

      BindgenInterface iface = BindgenInterface.builder()
          .name("api")
          .addFunction(func1)
          .addFunction(func2)
          .build();

      assertThat(iface.getFunctions()).hasSize(2);
      assertThat(iface.getFunctions()).containsExactly(func1, func2);
    }

    @Test
    @DisplayName("should create interface with functions() method")
    void shouldCreateInterfaceWithFunctionsMethod() {
      LOGGER.info("Testing builder with functions() list method");

      List<BindgenFunction> functions = Arrays.asList(
          BindgenFunction.builder().name("read").build(),
          BindgenFunction.builder().name("write").build());

      BindgenInterface iface = BindgenInterface.builder()
          .name("io")
          .functions(functions)
          .build();

      assertThat(iface.getFunctions()).hasSize(2);
    }

    @Test
    @DisplayName("should create interface with documentation")
    void shouldCreateInterfaceWithDocumentation() {
      LOGGER.info("Testing builder with documentation");

      BindgenInterface iface = BindgenInterface.builder()
          .name("documented")
          .documentation("This interface provides utility functions")
          .build();

      assertThat(iface.getDocumentation()).hasValue("This interface provides utility functions");
    }

    @Test
    @DisplayName("should create fully configured interface")
    void shouldCreateFullyConfiguredInterface() {
      LOGGER.info("Testing builder with all options");

      BindgenType type = BindgenType.primitive("i32");
      BindgenFunction function = BindgenFunction.builder().name("compute").build();

      BindgenInterface iface = BindgenInterface.builder()
          .name("calculator")
          .packageName("math:core")
          .addType(type)
          .addFunction(function)
          .documentation("Calculator interface")
          .build();

      assertThat(iface.getName()).isEqualTo("calculator");
      assertThat(iface.getPackageName()).hasValue("math:core");
      assertThat(iface.getTypes()).hasSize(1);
      assertThat(iface.getFunctions()).hasSize(1);
      assertThat(iface.getDocumentation()).isPresent();
    }
  }

  @Nested
  @DisplayName("Fully Qualified Name Tests")
  class FullyQualifiedNameTests {

    @Test
    @DisplayName("should return name only when no package name")
    void shouldReturnNameOnlyWhenNoPackageName() {
      LOGGER.info("Testing getFullyQualifiedName() without package");

      BindgenInterface iface = BindgenInterface.builder()
          .name("types")
          .build();

      assertThat(iface.getFullyQualifiedName()).isEqualTo("types");
    }

    @Test
    @DisplayName("should return package/name when package name is set")
    void shouldReturnPackageSlashNameWhenPackageSet() {
      LOGGER.info("Testing getFullyQualifiedName() with package");

      BindgenInterface iface = BindgenInterface.builder()
          .name("types")
          .packageName("wasi:io")
          .build();

      assertThat(iface.getFullyQualifiedName()).isEqualTo("wasi:io/types");
    }

    @Test
    @DisplayName("should return name only when package name is empty string")
    void shouldReturnNameOnlyWhenPackageNameIsEmpty() {
      LOGGER.info("Testing getFullyQualifiedName() with empty package");

      BindgenInterface iface = BindgenInterface.builder()
          .name("types")
          .packageName("")
          .build();

      assertThat(iface.getFullyQualifiedName()).isEqualTo("types");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName() should return interface name")
    void getNameShouldReturnInterfaceName() {
      BindgenInterface iface = BindgenInterface.builder().name("test-interface").build();

      assertThat(iface.getName()).isEqualTo("test-interface");
    }

    @Test
    @DisplayName("getPackageName() should return empty when not set")
    void getPackageNameShouldReturnEmptyWhenNotSet() {
      BindgenInterface iface = BindgenInterface.builder().name("test").build();

      assertThat(iface.getPackageName()).isEmpty();
    }

    @Test
    @DisplayName("getPackageName() should return value when set")
    void getPackageNameShouldReturnValueWhenSet() {
      BindgenInterface iface = BindgenInterface.builder()
          .name("test")
          .packageName("my:pkg")
          .build();

      assertThat(iface.getPackageName()).hasValue("my:pkg");
    }

    @Test
    @DisplayName("getTypes() should return empty list when no types")
    void getTypesShouldReturnEmptyListWhenNoTypes() {
      BindgenInterface iface = BindgenInterface.builder().name("empty").build();

      assertThat(iface.getTypes()).isEmpty();
    }

    @Test
    @DisplayName("getFunctions() should return empty list when no functions")
    void getFunctionsShouldReturnEmptyListWhenNoFunctions() {
      BindgenInterface iface = BindgenInterface.builder().name("empty").build();

      assertThat(iface.getFunctions()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal when name and packageName match")
    void shouldBeEqualWhenNameAndPackageNameMatch() {
      LOGGER.info("Testing equals() for matching interfaces");

      BindgenInterface iface1 = BindgenInterface.builder()
          .name("types")
          .packageName("wasi:io")
          .build();

      BindgenInterface iface2 = BindgenInterface.builder()
          .name("types")
          .packageName("wasi:io")
          .build();

      assertThat(iface1).isEqualTo(iface2);
      assertThat(iface1.hashCode()).isEqualTo(iface2.hashCode());
    }

    @Test
    @DisplayName("should be equal even when types differ")
    void shouldBeEqualEvenWhenTypesDiffer() {
      LOGGER.info("Testing equals() ignores types");

      BindgenInterface iface1 = BindgenInterface.builder()
          .name("types")
          .addType(BindgenType.primitive("i32"))
          .build();

      BindgenInterface iface2 = BindgenInterface.builder()
          .name("types")
          .addType(BindgenType.primitive("i64"))
          .build();

      assertThat(iface1).isEqualTo(iface2);
    }

    @Test
    @DisplayName("should not be equal when names differ")
    void shouldNotBeEqualWhenNamesDiffer() {
      LOGGER.info("Testing equals() for different names");

      BindgenInterface iface1 = BindgenInterface.builder().name("types").build();
      BindgenInterface iface2 = BindgenInterface.builder().name("functions").build();

      assertThat(iface1).isNotEqualTo(iface2);
    }

    @Test
    @DisplayName("should not be equal when packageNames differ")
    void shouldNotBeEqualWhenPackageNamesDiffer() {
      LOGGER.info("Testing equals() for different package names");

      BindgenInterface iface1 = BindgenInterface.builder()
          .name("types")
          .packageName("pkg1")
          .build();

      BindgenInterface iface2 = BindgenInterface.builder()
          .name("types")
          .packageName("pkg2")
          .build();

      assertThat(iface1).isNotEqualTo(iface2);
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      BindgenInterface iface = BindgenInterface.builder().name("test").build();

      assertThat(iface).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
      BindgenInterface iface = BindgenInterface.builder().name("test").build();

      assertThat(iface).isNotEqualTo("test");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      BindgenInterface iface = BindgenInterface.builder().name("test").build();

      assertThat(iface).isEqualTo(iface);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should include name and counts in toString()")
    void shouldIncludeNameAndCountsInToString() {
      LOGGER.info("Testing toString() output");

      BindgenInterface iface = BindgenInterface.builder()
          .name("api")
          .addType(BindgenType.primitive("i32"))
          .addFunction(BindgenFunction.builder().name("get").build())
          .build();

      String toString = iface.toString();

      assertThat(toString).contains("name='api'");
      assertThat(toString).contains("types=1");
      assertThat(toString).contains("functions=1");
      assertThat(toString).startsWith("BindgenInterface{");
      assertThat(toString).endsWith("}");
    }

    @Test
    @DisplayName("should include fully qualified name in toString()")
    void shouldIncludeFullyQualifiedNameInToString() {
      BindgenInterface iface = BindgenInterface.builder()
          .name("types")
          .packageName("wasi:io")
          .build();

      String toString = iface.toString();

      assertThat(toString).contains("name='wasi:io/types'");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("types list should be immutable")
    void typesListShouldBeImmutable() {
      LOGGER.info("Testing that types list is immutable");

      BindgenInterface iface = BindgenInterface.builder()
          .name("test")
          .addType(BindgenType.primitive("i32"))
          .build();

      List<BindgenType> types = iface.getTypes();

      assertThatThrownBy(() -> types.add(BindgenType.primitive("i64")))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("functions list should be immutable")
    void functionsListShouldBeImmutable() {
      LOGGER.info("Testing that functions list is immutable");

      BindgenInterface iface = BindgenInterface.builder()
          .name("test")
          .addFunction(BindgenFunction.builder().name("get").build())
          .build();

      List<BindgenFunction> functions = iface.getFunctions();

      assertThatThrownBy(() -> functions.add(BindgenFunction.builder().name("set").build()))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
