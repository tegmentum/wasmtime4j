/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentResourceDefinition} interface.
 *
 * <p>ComponentResourceDefinition defines a Component Model resource type.
 */
@DisplayName("ComponentResourceDefinition Tests")
class ComponentResourceDefinitionTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentResourceDefinition.class.getModifiers()),
          "ComponentResourceDefinition should be public");
      assertTrue(
          ComponentResourceDefinition.class.isInterface(),
          "ComponentResourceDefinition should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceDefinition.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getConstructor method")
    void shouldHaveGetConstructorMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceDefinition.class.getMethod("getConstructor");
      assertNotNull(method, "getConstructor method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getDestructor method")
    void shouldHaveGetDestructorMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceDefinition.class.getMethod("getDestructor");
      assertNotNull(method, "getDestructor method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getMethods method")
    void shouldHaveGetMethodsMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceDefinition.class.getMethod("getMethods");
      assertNotNull(method, "getMethods method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getMethod default method")
    void shouldHaveGetMethodDefaultMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceDefinition.class.getMethod("getMethod", String.class);
      assertNotNull(method, "getMethod method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertTrue(method.isDefault(), "getMethod should be a default method");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceDefinition.class.getMethod("builder", String.class);
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          ComponentResourceDefinition.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have ResourceConstructor nested interface")
    void shouldHaveResourceConstructorNestedInterface() {
      final var nestedClasses = ComponentResourceDefinition.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ResourceConstructor")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "ResourceConstructor should be an interface");
          assertTrue(
              nestedClass.isAnnotationPresent(FunctionalInterface.class),
              "ResourceConstructor should be a functional interface");
          break;
        }
      }
      assertTrue(found, "Should have ResourceConstructor nested interface");
    }

    @Test
    @DisplayName("should have ResourceMethod nested interface")
    void shouldHaveResourceMethodNestedInterface() {
      final var nestedClasses = ComponentResourceDefinition.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ResourceMethod")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "ResourceMethod should be an interface");
          assertTrue(
              nestedClass.isAnnotationPresent(FunctionalInterface.class),
              "ResourceMethod should be a functional interface");
          break;
        }
      }
      assertTrue(found, "Should have ResourceMethod nested interface");
    }
  }

  @Nested
  @DisplayName("Builder Nested Class Tests")
  class BuilderNestedClassTests {

    @Test
    @DisplayName("should have Builder nested class")
    void shouldHaveBuilderNestedClass() {
      final var nestedClasses = ComponentResourceDefinition.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Builder")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "Builder should be a class");
          break;
        }
      }
      assertTrue(found, "Should have Builder nested class");
    }
  }

  @Nested
  @DisplayName("Impl Nested Class Tests")
  class ImplNestedClassTests {

    @Test
    @DisplayName("should have Impl nested class")
    void shouldHaveImplNestedClass() {
      final var nestedClasses = ComponentResourceDefinition.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Impl")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "Impl should be a class");
          assertTrue(Modifier.isFinal(nestedClass.getModifiers()), "Impl should be final");
          break;
        }
      }
      assertTrue(found, "Should have Impl nested class");
    }
  }

  @Nested
  @DisplayName("Builder Behavior Tests")
  class BuilderBehaviorTests {

    @Test
    @DisplayName("builder should reject null name")
    void builderShouldRejectNullName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentResourceDefinition.builder(null),
          "Should throw for null name");
    }

    @Test
    @DisplayName("builder should reject empty name")
    void builderShouldRejectEmptyName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentResourceDefinition.builder(""),
          "Should throw for empty name");
    }

    @Test
    @DisplayName("builder should create definition with name only")
    void builderShouldCreateDefinitionWithNameOnly() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("test-resource").build();

      assertNotNull(def, "Should create definition");
      assertEquals("test-resource", def.getName(), "Name should match");
      assertTrue(def.getConstructor().isEmpty(), "Constructor should be empty");
      assertTrue(def.getDestructor().isEmpty(), "Destructor should be empty");
      assertTrue(def.getMethods().isEmpty(), "Methods should be empty");
    }

    @Test
    @DisplayName("builder should set constructor")
    void builderShouldSetConstructor() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("test-resource")
              .constructor(params -> "created")
              .build();

      assertTrue(def.getConstructor().isPresent(), "Constructor should be present");
    }

    @Test
    @DisplayName("builder should set destructor")
    void builderShouldSetDestructor() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("test-resource").destructor(s -> {}).build();

      assertTrue(def.getDestructor().isPresent(), "Destructor should be present");
    }

    @Test
    @DisplayName("builder should add methods")
    void builderShouldAddMethods() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("test-resource")
              .method("method1", (instance, params) -> List.of())
              .method("method2", (instance, params) -> List.of())
              .build();

      assertEquals(2, def.getMethods().size(), "Should have 2 methods");
      assertTrue(def.getMethod("method1").isPresent(), "method1 should be present");
      assertTrue(def.getMethod("method2").isPresent(), "method2 should be present");
      assertTrue(def.getMethod("nonexistent").isEmpty(), "nonexistent should be empty");
    }

    @Test
    @DisplayName("builder should reject null method name")
    void builderShouldRejectNullMethodName() {
      final ComponentResourceDefinition.Builder<String> builder =
          ComponentResourceDefinition.builder("test-resource");

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.method(null, (instance, params) -> List.of()),
          "Should throw for null method name");
    }

    @Test
    @DisplayName("builder should reject empty method name")
    void builderShouldRejectEmptyMethodName() {
      final ComponentResourceDefinition.Builder<String> builder =
          ComponentResourceDefinition.builder("test-resource");

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.method("", (instance, params) -> List.of()),
          "Should throw for empty method name");
    }

    @Test
    @DisplayName("builder constructor with supplier should work")
    void builderConstructorWithSupplierShouldWork() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("test-resource")
              .constructor(() -> "supplied-value")
              .build();

      assertTrue(def.getConstructor().isPresent(), "Constructor should be present");
    }

    @Test
    @DisplayName("builder should return fluent interface")
    void builderShouldReturnFluentInterface() {
      final ComponentResourceDefinition.Builder<String> builder =
          ComponentResourceDefinition.builder("test");

      final ComponentResourceDefinition.Builder<String> afterConstructor =
          builder.constructor(params -> "test");
      final ComponentResourceDefinition.Builder<String> afterDestructor =
          afterConstructor.destructor(s -> {});
      final ComponentResourceDefinition.Builder<String> afterMethod =
          afterDestructor.method("test", (i, p) -> List.of());

      // All should return the same builder for fluent chaining
      assertEquals(builder, afterConstructor, "Constructor should return same builder");
      assertEquals(builder, afterDestructor, "Destructor should return same builder");
      assertEquals(builder, afterMethod, "Method should return same builder");
    }
  }

  @Nested
  @DisplayName("Definition Behavior Tests")
  class DefinitionBehaviorTests {

    @Test
    @DisplayName("getMethods should return immutable map")
    void getMethodsShouldReturnImmutableMap() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("test-resource")
              .method("method1", (instance, params) -> List.of())
              .build();

      final Map<String, ComponentResourceDefinition.ResourceMethod<String>> methods =
          def.getMethods();
      assertThrows(
          UnsupportedOperationException.class,
          () -> methods.put("new-method", (i, p) -> List.of()),
          "Methods map should be immutable");
    }

    @Test
    @DisplayName("getMethod should return optional for existing method")
    void getMethodShouldReturnOptionalForExistingMethod() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("test-resource")
              .method("read", (instance, params) -> List.of())
              .build();

      assertTrue(def.getMethod("read").isPresent(), "Method should be present");
      assertNotNull(def.getMethod("read").get(), "Method should not be null");
    }

    @Test
    @DisplayName("getMethod should return empty for nonexistent method")
    void getMethodShouldReturnEmptyForNonexistentMethod() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("test-resource").build();

      assertTrue(def.getMethod("nonexistent").isEmpty(), "Method should not be present");
    }
  }
}
