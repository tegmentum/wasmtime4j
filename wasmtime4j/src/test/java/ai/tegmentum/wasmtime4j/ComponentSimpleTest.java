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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentSimple} interface.
 *
 * <p>ComponentSimple provides the core WebAssembly Component Model interface with essential
 * functionality for component management.
 */
@DisplayName("ComponentSimple Tests")
class ComponentSimpleTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentSimple.class.getModifiers()),
          "ComponentSimple should be public");
      assertTrue(ComponentSimple.class.isInterface(), "ComponentSimple should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentSimple.class),
          "ComponentSimple should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Identity Method Tests")
  class IdentityMethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(
          ComponentVersion.class, method.getReturnType(), "Should return ComponentVersion");
    }
  }

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(
          ComponentMetadata.class, method.getReturnType(), "Should return ComponentMetadata");
    }

    @Test
    @DisplayName("should have getResourceUsage method")
    void shouldHaveGetResourceUsageMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getResourceUsage");
      assertNotNull(method, "getResourceUsage method should exist");
      assertEquals(
          ComponentResourceUsage.class,
          method.getReturnType(),
          "Should return ComponentResourceUsage");
    }

    @Test
    @DisplayName("should have getLifecycleState method")
    void shouldHaveGetLifecycleStateMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getLifecycleState");
      assertNotNull(method, "getLifecycleState method should exist");
      assertEquals(
          ComponentLifecycleState.class,
          method.getReturnType(),
          "Should return ComponentLifecycleState");
    }
  }

  @Nested
  @DisplayName("Interface Export/Import Method Tests")
  class InterfaceMethodTests {

    @Test
    @DisplayName("should have exportsInterface method")
    void shouldHaveExportsInterfaceMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("exportsInterface", String.class);
      assertNotNull(method, "exportsInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have importsInterface method")
    void shouldHaveImportsInterfaceMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("importsInterface", String.class);
      assertNotNull(method, "importsInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getExportedInterfaces method")
    void shouldHaveGetExportedInterfacesMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getExportedInterfaces");
      assertNotNull(method, "getExportedInterfaces method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getImportedInterfaces method")
    void shouldHaveGetImportedInterfacesMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getImportedInterfaces");
      assertNotNull(method, "getImportedInterfaces method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }
  }

  @Nested
  @DisplayName("Instantiation Method Tests")
  class InstantiationMethodTests {

    @Test
    @DisplayName("should have instantiate method without config")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("instantiate");
      assertNotNull(method, "instantiate method should exist");
      assertEquals(
          ComponentInstance.class, method.getReturnType(), "Should return ComponentInstance");
    }

    @Test
    @DisplayName("should have instantiate method with config")
    void shouldHaveInstantiateMethodWithConfig() throws NoSuchMethodException {
      final Method method =
          ComponentSimple.class.getMethod("instantiate", ComponentInstanceConfig.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(
          ComponentInstance.class, method.getReturnType(), "Should return ComponentInstance");
    }
  }

  @Nested
  @DisplayName("Dependency Method Tests")
  class DependencyMethodTests {

    @Test
    @DisplayName("should have getDependencyGraph method")
    void shouldHaveGetDependencyGraphMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getDependencyGraph");
      assertNotNull(method, "getDependencyGraph method should exist");
      assertEquals(
          ComponentDependencyGraph.class,
          method.getReturnType(),
          "Should return ComponentDependencyGraph");
    }

    @Test
    @DisplayName("should have resolveDependencies method")
    void shouldHaveResolveDependenciesMethod() throws NoSuchMethodException {
      final Method method =
          ComponentSimple.class.getMethod("resolveDependencies", ComponentRegistry.class);
      assertNotNull(method, "resolveDependencies method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }
  }

  @Nested
  @DisplayName("Compatibility Method Tests")
  class CompatibilityMethodTests {

    @Test
    @DisplayName("should have checkCompatibility method")
    void shouldHaveCheckCompatibilityMethod() throws NoSuchMethodException {
      final Method method =
          ComponentSimple.class.getMethod("checkCompatibility", ComponentSimple.class);
      assertNotNull(method, "checkCompatibility method should exist");
      assertEquals(
          ComponentCompatibility.class,
          method.getReturnType(),
          "Should return ComponentCompatibility");
    }

    @Test
    @DisplayName("should have getWitInterface method")
    void shouldHaveGetWitInterfaceMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getWitInterface");
      assertNotNull(method, "getWitInterface method should exist");
      assertEquals(
          WitInterfaceDefinition.class,
          method.getReturnType(),
          "Should return WitInterfaceDefinition");
    }

    @Test
    @DisplayName("should have checkWitCompatibility method")
    void shouldHaveCheckWitCompatibilityMethod() throws NoSuchMethodException {
      final Method method =
          ComponentSimple.class.getMethod("checkWitCompatibility", ComponentSimple.class);
      assertNotNull(method, "checkWitCompatibility method should exist");
      assertEquals(
          WitCompatibilityResult.class,
          method.getReturnType(),
          "Should return WitCompatibilityResult");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method =
          ComponentSimple.class.getMethod("validate", ComponentValidationConfig.class);
      assertNotNull(method, "validate method should exist");
      assertEquals(
          ComponentValidationResult.class,
          method.getReturnType(),
          "Should return ComponentValidationResult");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("getSize should declare WasmException")
    void getSizeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getSize");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "getSize should declare WasmException");
    }

    @Test
    @DisplayName("instantiate should declare WasmException")
    void instantiateShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("instantiate");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "instantiate should declare WasmException");
    }

    @Test
    @DisplayName("exportsInterface should declare WasmException")
    void exportsInterfaceShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("exportsInterface", String.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "exportsInterface should declare WasmException");
    }

    @Test
    @DisplayName("getDependencyGraph should declare WasmException")
    void getDependencyGraphShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentSimple.class.getMethod("getDependencyGraph");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "getDependencyGraph should declare WasmException");
    }
  }
}
