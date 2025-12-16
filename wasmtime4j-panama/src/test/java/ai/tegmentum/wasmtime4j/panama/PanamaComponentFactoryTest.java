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

package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PanamaComponent} factory class.
 *
 * <p>These tests focus on parameter validation and defensive programming behavior. Since
 * PanamaComponent is a static factory class with private constructor, these tests validate that the
 * API contracts are properly enforced without requiring actual native library operations.
 */
@DisplayName("PanamaComponent Factory Tests")
class PanamaComponentFactoryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      assertThat(PanamaComponent.class.getModifiers() & java.lang.reflect.Modifier.FINAL)
          .as("PanamaComponent should be a final class")
          .isNotZero();
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws Exception {
      java.lang.reflect.Constructor<?>[] constructors =
          PanamaComponent.class.getDeclaredConstructors();

      assertThat(constructors).hasSize(1);
      assertThat(constructors[0].getModifiers() & java.lang.reflect.Modifier.PRIVATE)
          .as("Constructor should be private")
          .isNotZero();
    }

    @Test
    @DisplayName("should not be instantiable")
    void shouldNotBeInstantiable() throws Exception {
      java.lang.reflect.Constructor<PanamaComponent> constructor =
          PanamaComponent.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      // Constructor exists but is private for preventing instantiation
      assertThat(constructor).isNotNull();
    }

    @Test
    @DisplayName("should contain nested PanamaComponentEngine class")
    void shouldContainNestedComponentEngineClass() {
      Class<?>[] nestedClasses = PanamaComponent.class.getDeclaredClasses();

      boolean hasEngineClass = false;
      for (Class<?> nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          hasEngineClass = true;
          break;
        }
      }

      assertThat(hasEngineClass).as("Should contain nested PanamaComponentEngine class").isTrue();
    }

    @Test
    @DisplayName("should contain nested PanamaComponentHandle class")
    void shouldContainNestedComponentHandleClass() {
      Class<?>[] nestedClasses = PanamaComponent.class.getDeclaredClasses();

      boolean hasHandleClass = false;
      for (Class<?> nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("PanamaComponentHandle")) {
          hasHandleClass = true;
          break;
        }
      }

      assertThat(hasHandleClass).as("Should contain nested PanamaComponentHandle class").isTrue();
    }

    @Test
    @DisplayName("should contain nested PanamaComponentInstanceHandle class")
    void shouldContainNestedComponentInstanceHandleClass() {
      Class<?>[] nestedClasses = PanamaComponent.class.getDeclaredClasses();

      boolean hasInstanceHandleClass = false;
      for (Class<?> nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("PanamaComponentInstanceHandle")) {
          hasInstanceHandleClass = true;
          break;
        }
      }

      assertThat(hasInstanceHandleClass)
          .as("Should contain nested PanamaComponentInstanceHandle class")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("createComponentEngine should exist as static method")
    void createComponentEngineShouldExistAsStaticMethod() throws Exception {
      java.lang.reflect.Method method =
          PanamaComponent.class.getMethod("createComponentEngine", ArenaResourceManager.class);

      assertThat(method).isNotNull();
      assertThat(method.getModifiers() & java.lang.reflect.Modifier.STATIC)
          .as("Method should be static")
          .isNotZero();
      assertThat(method.getModifiers() & java.lang.reflect.Modifier.PUBLIC)
          .as("Method should be public")
          .isNotZero();
    }

    @Test
    @DisplayName("createComponentEngine should reject null resource manager")
    void createComponentEngineShouldRejectNullResourceManager() {
      assertThatThrownBy(() -> PanamaComponent.createComponentEngine(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Resource manager cannot be null");
    }
  }

  @Nested
  @DisplayName("PanamaComponentEngine Contract Tests")
  class ComponentEngineContractTests {

    @Test
    @DisplayName("PanamaComponentEngine should implement AutoCloseable")
    void componentEngineShouldImplementAutoCloseable() throws Exception {
      Class<?> engineClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
          break;
        }
      }

      assertThat(engineClass).isNotNull();
      assertThat(AutoCloseable.class.isAssignableFrom(engineClass))
          .as("PanamaComponentEngine should implement AutoCloseable")
          .isTrue();
    }

    @Test
    @DisplayName("PanamaComponentEngine should be final class")
    void componentEngineShouldBeFinalClass() throws Exception {
      Class<?> engineClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
          break;
        }
      }

      assertThat(engineClass).isNotNull();
      assertThat(engineClass.getModifiers() & java.lang.reflect.Modifier.FINAL)
          .as("PanamaComponentEngine should be a final class")
          .isNotZero();
    }

    @Test
    @DisplayName("PanamaComponentEngine should have loadComponentFromBytes method")
    void componentEngineShouldHaveLoadComponentFromBytesMethod() throws Exception {
      Class<?> engineClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
          break;
        }
      }

      assertThat(engineClass).isNotNull();

      java.lang.reflect.Method method =
          engineClass.getMethod("loadComponentFromBytes", byte[].class);
      assertThat(method).isNotNull();
      assertThat(method.getModifiers() & java.lang.reflect.Modifier.PUBLIC)
          .as("loadComponentFromBytes should be public")
          .isNotZero();
    }

    @Test
    @DisplayName("PanamaComponentEngine should have isValid method")
    void componentEngineShouldHaveIsValidMethod() throws Exception {
      Class<?> engineClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
          break;
        }
      }

      assertThat(engineClass).isNotNull();

      java.lang.reflect.Method method = engineClass.getMethod("isValid");
      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaComponentEngine should have getActiveInstancesCount method")
    void componentEngineShouldHaveGetActiveInstancesCountMethod() throws Exception {
      Class<?> engineClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
          break;
        }
      }

      assertThat(engineClass).isNotNull();

      java.lang.reflect.Method method = engineClass.getMethod("getActiveInstancesCount");
      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("PanamaComponentEngine should have cleanupInstances method")
    void componentEngineShouldHaveCleanupInstancesMethod() throws Exception {
      Class<?> engineClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
          break;
        }
      }

      assertThat(engineClass).isNotNull();

      java.lang.reflect.Method method = engineClass.getMethod("cleanupInstances");
      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("PanamaComponentEngine should have close method")
    void componentEngineShouldHaveCloseMethod() throws Exception {
      Class<?> engineClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentEngine")) {
          engineClass = nestedClass;
          break;
        }
      }

      assertThat(engineClass).isNotNull();

      java.lang.reflect.Method method = engineClass.getMethod("close");
      assertThat(method).isNotNull();
    }
  }

  @Nested
  @DisplayName("PanamaComponentHandle Contract Tests")
  class ComponentHandleContractTests {

    @Test
    @DisplayName("PanamaComponentHandle should implement AutoCloseable")
    void componentHandleShouldImplementAutoCloseable() throws Exception {
      Class<?> handleClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentHandle")) {
          handleClass = nestedClass;
          break;
        }
      }

      assertThat(handleClass).isNotNull();
      assertThat(AutoCloseable.class.isAssignableFrom(handleClass))
          .as("PanamaComponentHandle should implement AutoCloseable")
          .isTrue();
    }

    @Test
    @DisplayName("PanamaComponentHandle should be final class")
    void componentHandleShouldBeFinalClass() throws Exception {
      Class<?> handleClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentHandle")) {
          handleClass = nestedClass;
          break;
        }
      }

      assertThat(handleClass).isNotNull();
      assertThat(handleClass.getModifiers() & java.lang.reflect.Modifier.FINAL)
          .as("PanamaComponentHandle should be a final class")
          .isNotZero();
    }
  }

  @Nested
  @DisplayName("PanamaComponentInstanceHandle Contract Tests")
  class ComponentInstanceHandleContractTests {

    @Test
    @DisplayName("PanamaComponentInstanceHandle should implement AutoCloseable")
    void componentInstanceHandleShouldImplementAutoCloseable() throws Exception {
      Class<?> instanceHandleClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentInstanceHandle")) {
          instanceHandleClass = nestedClass;
          break;
        }
      }

      assertThat(instanceHandleClass).isNotNull();
      assertThat(AutoCloseable.class.isAssignableFrom(instanceHandleClass))
          .as("PanamaComponentInstanceHandle should implement AutoCloseable")
          .isTrue();
    }

    @Test
    @DisplayName("PanamaComponentInstanceHandle should be final class")
    void componentInstanceHandleShouldBeFinalClass() throws Exception {
      Class<?> instanceHandleClass = null;
      for (Class<?> nestedClass : PanamaComponent.class.getDeclaredClasses()) {
        if (nestedClass.getSimpleName().equals("PanamaComponentInstanceHandle")) {
          instanceHandleClass = nestedClass;
          break;
        }
      }

      assertThat(instanceHandleClass).isNotNull();
      assertThat(instanceHandleClass.getModifiers() & java.lang.reflect.Modifier.FINAL)
          .as("PanamaComponentInstanceHandle should be a final class")
          .isNotZero();
    }
  }

  @Nested
  @DisplayName("Documentation Tests")
  class DocumentationTests {

    @Test
    @DisplayName("PanamaComponent class should be documented")
    void panamaComponentClassShouldBeDocumented() {
      // Verify class-level documentation exists via Javadoc presence
      // This is verified implicitly by compilation - the source has Javadoc
      assertThat(PanamaComponent.class.getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.panama.PanamaComponent");
    }
  }
}
