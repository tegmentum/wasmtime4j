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

/** Tests for {@link BindgenModel}. */
@DisplayName("BindgenModel Tests")
class BindgenModelTest {

  private static final Logger LOGGER = Logger.getLogger(BindgenModelTest.class.getName());

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create model with builder and default values")
    void shouldCreateModelWithDefaultValues() {
      LOGGER.info("Testing builder with default values");

      BindgenModel model = BindgenModel.builder().build();

      assertThat(model.getName()).isEmpty();
      assertThat(model.getInterfaces()).isEmpty();
      assertThat(model.getTypes()).isEmpty();
      assertThat(model.getFunctions()).isEmpty();
      assertThat(model.getSourceFile()).isEmpty();
      assertThat(model.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("should create model with name")
    void shouldCreateModelWithName() {
      LOGGER.info("Testing builder with name");

      BindgenModel model = BindgenModel.builder()
          .name("my-module")
          .build();

      assertThat(model.getName()).isEqualTo("my-module");
    }

    @Test
    @DisplayName("should create model with interfaces using addInterface")
    void shouldCreateModelWithInterfacesUsingAddInterface() {
      LOGGER.info("Testing builder with addInterface()");

      BindgenInterface iface1 = BindgenInterface.builder().name("types").build();
      BindgenInterface iface2 = BindgenInterface.builder().name("streams").build();

      BindgenModel model = BindgenModel.builder()
          .name("wasi")
          .addInterface(iface1)
          .addInterface(iface2)
          .build();

      assertThat(model.getInterfaces()).hasSize(2);
      assertThat(model.getInterfaces()).containsExactly(iface1, iface2);
    }

    @Test
    @DisplayName("should create model with interfaces() method")
    void shouldCreateModelWithInterfacesMethod() {
      LOGGER.info("Testing builder with interfaces() list method");

      List<BindgenInterface> interfaces = Arrays.asList(
          BindgenInterface.builder().name("io").build(),
          BindgenInterface.builder().name("fs").build());

      BindgenModel model = BindgenModel.builder()
          .name("wasi")
          .interfaces(interfaces)
          .build();

      assertThat(model.getInterfaces()).hasSize(2);
    }

    @Test
    @DisplayName("should create model with types using addType")
    void shouldCreateModelWithTypesUsingAddType() {
      LOGGER.info("Testing builder with addType()");

      BindgenType type1 = BindgenType.primitive("i32");
      BindgenType type2 = BindgenType.builder().name("MyRecord").kind(BindgenType.Kind.RECORD).build();

      BindgenModel model = BindgenModel.builder()
          .name("module")
          .addType(type1)
          .addType(type2)
          .build();

      assertThat(model.getTypes()).hasSize(2);
      assertThat(model.getTypes()).containsExactly(type1, type2);
    }

    @Test
    @DisplayName("should create model with types() method")
    void shouldCreateModelWithTypesMethod() {
      LOGGER.info("Testing builder with types() list method");

      List<BindgenType> types = Arrays.asList(
          BindgenType.primitive("string"),
          BindgenType.primitive("bool"));

      BindgenModel model = BindgenModel.builder()
          .name("module")
          .types(types)
          .build();

      assertThat(model.getTypes()).hasSize(2);
    }

    @Test
    @DisplayName("should create model with functions using addFunction")
    void shouldCreateModelWithFunctionsUsingAddFunction() {
      LOGGER.info("Testing builder with addFunction()");

      BindgenFunction func1 = BindgenFunction.builder().name("init").build();
      BindgenFunction func2 = BindgenFunction.builder().name("cleanup").build();

      BindgenModel model = BindgenModel.builder()
          .name("module")
          .addFunction(func1)
          .addFunction(func2)
          .build();

      assertThat(model.getFunctions()).hasSize(2);
      assertThat(model.getFunctions()).containsExactly(func1, func2);
    }

    @Test
    @DisplayName("should create model with functions() method")
    void shouldCreateModelWithFunctionsMethod() {
      LOGGER.info("Testing builder with functions() list method");

      List<BindgenFunction> functions = Arrays.asList(
          BindgenFunction.builder().name("start").build(),
          BindgenFunction.builder().name("stop").build());

      BindgenModel model = BindgenModel.builder()
          .name("module")
          .functions(functions)
          .build();

      assertThat(model.getFunctions()).hasSize(2);
    }

    @Test
    @DisplayName("should create model with source file")
    void shouldCreateModelWithSourceFile() {
      LOGGER.info("Testing builder with source file");

      BindgenModel model = BindgenModel.builder()
          .name("module")
          .sourceFile("/path/to/module.wasm")
          .build();

      assertThat(model.getSourceFile()).hasValue("/path/to/module.wasm");
    }
  }

  @Nested
  @DisplayName("Type Registry Tests")
  class TypeRegistryTests {

    @Test
    @DisplayName("should register types added via addType")
    void shouldRegisterTypesAddedViaAddType() {
      LOGGER.info("Testing type registration via addType()");

      BindgenType type = BindgenType.builder().name("MyType").kind(BindgenType.Kind.RECORD).build();

      BindgenModel model = BindgenModel.builder()
          .addType(type)
          .build();

      assertThat(model.hasType("MyType")).isTrue();
      assertThat(model.lookupType("MyType")).hasValue(type);
    }

    @Test
    @DisplayName("should register types added via types()")
    void shouldRegisterTypesAddedViaTypesMethod() {
      LOGGER.info("Testing type registration via types()");

      List<BindgenType> types = Arrays.asList(
          BindgenType.builder().name("Type1").kind(BindgenType.Kind.RECORD).build(),
          BindgenType.builder().name("Type2").kind(BindgenType.Kind.ENUM).build());

      BindgenModel model = BindgenModel.builder()
          .types(types)
          .build();

      assertThat(model.hasType("Type1")).isTrue();
      assertThat(model.hasType("Type2")).isTrue();
    }

    @Test
    @DisplayName("should register types from interfaces")
    void shouldRegisterTypesFromInterfaces() {
      LOGGER.info("Testing type registration from interfaces");

      BindgenType typeInInterface = BindgenType.builder()
          .name("InterfaceType")
          .kind(BindgenType.Kind.RECORD)
          .build();

      BindgenInterface iface = BindgenInterface.builder()
          .name("my-interface")
          .addType(typeInInterface)
          .build();

      BindgenModel model = BindgenModel.builder()
          .addInterface(iface)
          .build();

      assertThat(model.hasType("InterfaceType")).isTrue();
      assertThat(model.lookupType("InterfaceType")).hasValue(typeInInterface);
    }

    @Test
    @DisplayName("lookupType() should return empty for non-existent type")
    void lookupTypeShouldReturnEmptyForNonExistentType() {
      LOGGER.info("Testing lookupType() for missing type");

      BindgenModel model = BindgenModel.builder().build();

      assertThat(model.lookupType("NonExistent")).isEmpty();
    }

    @Test
    @DisplayName("hasType() should return false for non-existent type")
    void hasTypeShouldReturnFalseForNonExistentType() {
      BindgenModel model = BindgenModel.builder().build();

      assertThat(model.hasType("NonExistent")).isFalse();
    }

    @Test
    @DisplayName("getTypeNames() should return all registered type names")
    void getTypeNamesShouldReturnAllRegisteredTypeNames() {
      LOGGER.info("Testing getTypeNames()");

      BindgenModel model = BindgenModel.builder()
          .addType(BindgenType.builder().name("Type1").kind(BindgenType.Kind.RECORD).build())
          .addType(BindgenType.builder().name("Type2").kind(BindgenType.Kind.ENUM).build())
          .build();

      assertThat(model.getTypeNames()).containsExactlyInAnyOrder("Type1", "Type2");
    }

    @Test
    @DisplayName("getTypeCount() should return correct count")
    void getTypeCountShouldReturnCorrectCount() {
      LOGGER.info("Testing getTypeCount()");

      BindgenModel model = BindgenModel.builder()
          .addType(BindgenType.builder().name("Type1").build())
          .addType(BindgenType.builder().name("Type2").build())
          .addType(BindgenType.builder().name("Type3").build())
          .build();

      assertThat(model.getTypeCount()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("Function Count Tests")
  class FunctionCountTests {

    @Test
    @DisplayName("getTotalFunctionCount() should count top-level functions")
    void getTotalFunctionCountShouldCountTopLevelFunctions() {
      LOGGER.info("Testing getTotalFunctionCount() for top-level functions");

      BindgenModel model = BindgenModel.builder()
          .addFunction(BindgenFunction.builder().name("func1").build())
          .addFunction(BindgenFunction.builder().name("func2").build())
          .build();

      assertThat(model.getTotalFunctionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("getTotalFunctionCount() should include functions from interfaces")
    void getTotalFunctionCountShouldIncludeFunctionsFromInterfaces() {
      LOGGER.info("Testing getTotalFunctionCount() including interface functions");

      BindgenInterface iface = BindgenInterface.builder()
          .name("api")
          .addFunction(BindgenFunction.builder().name("ifaceFunc1").build())
          .addFunction(BindgenFunction.builder().name("ifaceFunc2").build())
          .build();

      BindgenModel model = BindgenModel.builder()
          .addInterface(iface)
          .addFunction(BindgenFunction.builder().name("topLevelFunc").build())
          .build();

      assertThat(model.getTotalFunctionCount()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("isEmpty Tests")
  class IsEmptyTests {

    @Test
    @DisplayName("isEmpty() should return true for empty model")
    void isEmptyShouldReturnTrueForEmptyModel() {
      BindgenModel model = BindgenModel.builder().build();

      assertThat(model.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty() should return false when interfaces exist")
    void isEmptyShouldReturnFalseWhenInterfacesExist() {
      BindgenModel model = BindgenModel.builder()
          .addInterface(BindgenInterface.builder().name("test").build())
          .build();

      assertThat(model.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("isEmpty() should return false when types exist")
    void isEmptyShouldReturnFalseWhenTypesExist() {
      BindgenModel model = BindgenModel.builder()
          .addType(BindgenType.primitive("i32"))
          .build();

      assertThat(model.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("isEmpty() should return false when functions exist")
    void isEmptyShouldReturnFalseWhenFunctionsExist() {
      BindgenModel model = BindgenModel.builder()
          .addFunction(BindgenFunction.builder().name("func").build())
          .build();

      assertThat(model.isEmpty()).isFalse();
    }
  }

  @Nested
  @DisplayName("Merge Tests")
  class MergeTests {

    @Test
    @DisplayName("merge() should combine interfaces from both models")
    void mergeShouldCombineInterfaces() {
      LOGGER.info("Testing merge() for interfaces");

      BindgenInterface iface1 = BindgenInterface.builder().name("iface1").build();
      BindgenInterface iface2 = BindgenInterface.builder().name("iface2").build();

      BindgenModel model1 = BindgenModel.builder()
          .name("model1")
          .addInterface(iface1)
          .build();

      BindgenModel model2 = BindgenModel.builder()
          .name("model2")
          .addInterface(iface2)
          .build();

      BindgenModel merged = model1.merge(model2);

      assertThat(merged.getName()).isEqualTo("model1");
      assertThat(merged.getInterfaces()).hasSize(2);
      assertThat(merged.getInterfaces()).containsExactly(iface1, iface2);
    }

    @Test
    @DisplayName("merge() should combine types from both models")
    void mergeShouldCombineTypes() {
      LOGGER.info("Testing merge() for types");

      BindgenType type1 = BindgenType.builder().name("Type1").build();
      BindgenType type2 = BindgenType.builder().name("Type2").build();

      BindgenModel model1 = BindgenModel.builder().addType(type1).build();
      BindgenModel model2 = BindgenModel.builder().addType(type2).build();

      BindgenModel merged = model1.merge(model2);

      assertThat(merged.getTypes()).hasSize(2);
    }

    @Test
    @DisplayName("merge() should combine functions from both models")
    void mergeShouldCombineFunctions() {
      LOGGER.info("Testing merge() for functions");

      BindgenFunction func1 = BindgenFunction.builder().name("func1").build();
      BindgenFunction func2 = BindgenFunction.builder().name("func2").build();

      BindgenModel model1 = BindgenModel.builder().addFunction(func1).build();
      BindgenModel model2 = BindgenModel.builder().addFunction(func2).build();

      BindgenModel merged = model1.merge(model2);

      assertThat(merged.getFunctions()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal when name, interfaces, types, and functions match")
    void shouldBeEqualWhenAllFieldsMatch() {
      LOGGER.info("Testing equals() for matching models");

      BindgenType type = BindgenType.primitive("i32");
      BindgenFunction func = BindgenFunction.builder().name("func").build();

      BindgenModel model1 = BindgenModel.builder()
          .name("module")
          .addType(type)
          .addFunction(func)
          .build();

      BindgenModel model2 = BindgenModel.builder()
          .name("module")
          .addType(type)
          .addFunction(func)
          .build();

      assertThat(model1).isEqualTo(model2);
      assertThat(model1.hashCode()).isEqualTo(model2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when names differ")
    void shouldNotBeEqualWhenNamesDiffer() {
      LOGGER.info("Testing equals() for different names");

      BindgenModel model1 = BindgenModel.builder().name("module1").build();
      BindgenModel model2 = BindgenModel.builder().name("module2").build();

      assertThat(model1).isNotEqualTo(model2);
    }

    @Test
    @DisplayName("should not be equal when types differ")
    void shouldNotBeEqualWhenTypesDiffer() {
      BindgenModel model1 = BindgenModel.builder()
          .name("module")
          .addType(BindgenType.primitive("i32"))
          .build();

      BindgenModel model2 = BindgenModel.builder()
          .name("module")
          .addType(BindgenType.primitive("i64"))
          .build();

      assertThat(model1).isNotEqualTo(model2);
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      BindgenModel model = BindgenModel.builder().name("module").build();

      assertThat(model).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
      BindgenModel model = BindgenModel.builder().name("module").build();

      assertThat(model).isNotEqualTo("module");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      BindgenModel model = BindgenModel.builder().name("module").build();

      assertThat(model).isEqualTo(model);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should include name and counts in toString()")
    void shouldIncludeNameAndCountsInToString() {
      LOGGER.info("Testing toString() output");

      BindgenModel model = BindgenModel.builder()
          .name("my-module")
          .addInterface(BindgenInterface.builder().name("api").build())
          .addType(BindgenType.primitive("i32"))
          .addFunction(BindgenFunction.builder().name("init").build())
          .build();

      String toString = model.toString();

      assertThat(toString).contains("name='my-module'");
      assertThat(toString).contains("interfaces=1");
      assertThat(toString).contains("types=1");
      assertThat(toString).contains("functions=1");
      assertThat(toString).startsWith("BindgenModel{");
      assertThat(toString).endsWith("}");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("interfaces list should be immutable")
    void interfacesListShouldBeImmutable() {
      LOGGER.info("Testing that interfaces list is immutable");

      BindgenModel model = BindgenModel.builder()
          .addInterface(BindgenInterface.builder().name("test").build())
          .build();

      List<BindgenInterface> interfaces = model.getInterfaces();

      assertThatThrownBy(() -> interfaces.add(BindgenInterface.builder().name("new").build()))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("types list should be immutable")
    void typesListShouldBeImmutable() {
      LOGGER.info("Testing that types list is immutable");

      BindgenModel model = BindgenModel.builder()
          .addType(BindgenType.primitive("i32"))
          .build();

      List<BindgenType> types = model.getTypes();

      assertThatThrownBy(() -> types.add(BindgenType.primitive("i64")))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("functions list should be immutable")
    void functionsListShouldBeImmutable() {
      LOGGER.info("Testing that functions list is immutable");

      BindgenModel model = BindgenModel.builder()
          .addFunction(BindgenFunction.builder().name("func").build())
          .build();

      List<BindgenFunction> functions = model.getFunctions();

      assertThatThrownBy(() -> functions.add(BindgenFunction.builder().name("new").build()))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
