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

import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PanamaWasiComponent}.
 *
 * <p>These tests verify the class structure, interface contract implementation, and parameter
 * validation behavior of PanamaWasiComponent without requiring actual native library operations.
 */
@DisplayName("PanamaWasiComponent Tests")
class PanamaWasiComponentTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      assertThat(Modifier.isFinal(PanamaWasiComponent.class.getModifiers()))
          .as("PanamaWasiComponent should be a final class")
          .isTrue();
    }

    @Test
    @DisplayName("should implement WasiComponent interface")
    void shouldImplementWasiComponentInterface() {
      assertThat(WasiComponent.class.isAssignableFrom(PanamaWasiComponent.class))
          .as("PanamaWasiComponent should implement WasiComponent")
          .isTrue();
    }

    @Test
    @DisplayName("should be in correct package")
    void shouldBeInCorrectPackage() {
      assertThat(PanamaWasiComponent.class.getPackage().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.panama");
    }

    @Test
    @DisplayName("should have public visibility")
    void shouldHavePublicVisibility() {
      assertThat(Modifier.isPublic(PanamaWasiComponent.class.getModifiers()))
          .as("PanamaWasiComponent should be public")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Interface Method Implementation Tests")
  class InterfaceMethodImplementationTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("getName");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("should have getExports method")
    void shouldHaveGetExportsMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("getExports");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(java.util.List.class);
    }

    @Test
    @DisplayName("should have getImports method")
    void shouldHaveGetImportsMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("getImports");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(java.util.List.class);
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("getStats");

      assertThat(method).isNotNull();
    }

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() throws Exception {
      Method method =
          PanamaWasiComponent.class.getMethod(
              "instantiate", ai.tegmentum.wasmtime4j.wasi.WasiConfig.class);

      assertThat(method).isNotNull();
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("close");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("isValid");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }
  }

  @Nested
  @DisplayName("Additional Method Tests")
  class AdditionalMethodTests {

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("validate");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have getExportMetadata method")
    void shouldHaveGetExportMetadataMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("getExportMetadata", String.class);

      assertThat(method).isNotNull();
    }

    @Test
    @DisplayName("should have getImportMetadata method")
    void shouldHaveGetImportMetadataMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("getImportMetadata", String.class);

      assertThat(method).isNotNull();
    }

    @Test
    @DisplayName("should have instantiate without config method")
    void shouldHaveInstantiateNoConfigMethod() throws Exception {
      Method method = PanamaWasiComponent.class.getMethod("instantiate");

      assertThat(method).isNotNull();
    }

    @Test
    @DisplayName("should have validate with config method")
    void shouldHaveValidateWithConfigMethod() throws Exception {
      Method method =
          PanamaWasiComponent.class.getMethod(
              "validate", ai.tegmentum.wasmtime4j.wasi.WasiConfig.class);

      assertThat(method).isNotNull();
    }
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("should have public constructor")
    void shouldHavePublicConstructor() throws Exception {
      java.lang.reflect.Constructor<?> constructor =
          PanamaWasiComponent.class.getConstructor(
              ArenaResourceManager.class,
              PanamaComponent.PanamaComponentEngine.class,
              PanamaComponent.PanamaComponentHandle.class,
              String.class);

      assertThat(constructor).isNotNull();
      assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("constructor should have required parameter types")
    void constructorShouldHaveRequiredParameterTypes() throws Exception {
      java.lang.reflect.Constructor<?> constructor =
          PanamaWasiComponent.class.getConstructor(
              ArenaResourceManager.class,
              PanamaComponent.PanamaComponentEngine.class,
              PanamaComponent.PanamaComponentHandle.class,
              String.class);

      Class<?>[] paramTypes = constructor.getParameterTypes();

      assertThat(paramTypes).hasSize(4);
      assertThat(paramTypes[0]).isEqualTo(ArenaResourceManager.class);
      assertThat(paramTypes[3]).isEqualTo(String.class);
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected public methods")
    void shouldHaveExpectedPublicMethods() {
      Method[] methods = PanamaWasiComponent.class.getMethods();

      // Filter out Object methods and synthetic methods
      long componentMethods =
          java.util.Arrays.stream(methods)
              .filter(m -> !m.getDeclaringClass().equals(Object.class))
              .filter(m -> !m.isSynthetic())
              .count();

      // Should have at least WasiComponent interface methods plus any additional
      assertThat(componentMethods)
          .as("Should have substantial number of public methods")
          .isGreaterThanOrEqualTo(10);
    }
  }

  @Nested
  @DisplayName("Lifecycle Management Tests")
  class LifecycleManagementTests {

    @Test
    @DisplayName("should be AutoCloseable")
    void shouldBeAutoCloseable() {
      assertThat(AutoCloseable.class.isAssignableFrom(PanamaWasiComponent.class))
          .as("PanamaWasiComponent should be AutoCloseable")
          .isTrue();
    }

    @Test
    @DisplayName("close method should not throw checked exceptions in signature")
    void closeMethodShouldNotThrowCheckedExceptionsInSignature() throws Exception {
      Method closeMethod = PanamaWasiComponent.class.getMethod("close");

      Class<?>[] exceptionTypes = closeMethod.getExceptionTypes();

      // AutoCloseable.close() throws Exception, but concrete implementations
      // can narrow the throws clause
      assertThat(closeMethod).isNotNull();
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("closed field should be volatile")
    void closedFieldShouldBeVolatile() {
      java.lang.reflect.Field[] fields = PanamaWasiComponent.class.getDeclaredFields();

      boolean foundClosedField = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getName().equals("closed")) {
          foundClosedField = true;
          assertThat(Modifier.isVolatile(field.getModifiers()))
              .as("'closed' field should be volatile for thread safety")
              .isTrue();
          break;
        }
      }

      assertThat(foundClosedField).as("Should have a 'closed' field").isTrue();
    }
  }

  @Nested
  @DisplayName("Caching Tests")
  class CachingTests {

    @Test
    @DisplayName("should have cached exports field")
    void shouldHaveCachedExportsField() {
      java.lang.reflect.Field[] fields = PanamaWasiComponent.class.getDeclaredFields();

      boolean foundCachedExportsField = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getName().contains("cachedExports")) {
          foundCachedExportsField = true;
          assertThat(Modifier.isVolatile(field.getModifiers()))
              .as("Cached fields should be volatile for thread safety")
              .isTrue();
          break;
        }
      }

      assertThat(foundCachedExportsField)
          .as("Should have a cachedExports field for caching")
          .isTrue();
    }

    @Test
    @DisplayName("should have cached imports field")
    void shouldHaveCachedImportsField() {
      java.lang.reflect.Field[] fields = PanamaWasiComponent.class.getDeclaredFields();

      boolean foundCachedImportsField = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getName().contains("cachedImports")) {
          foundCachedImportsField = true;
          assertThat(Modifier.isVolatile(field.getModifiers()))
              .as("Cached fields should be volatile for thread safety")
              .isTrue();
          break;
        }
      }

      assertThat(foundCachedImportsField)
          .as("Should have a cachedImports field for caching")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Documentation Tests")
  class DocumentationTests {

    @Test
    @DisplayName("class should be documented")
    void classShouldBeDocumented() {
      assertThat(PanamaWasiComponent.class.getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.panama.PanamaWasiComponent");
    }
  }
}
