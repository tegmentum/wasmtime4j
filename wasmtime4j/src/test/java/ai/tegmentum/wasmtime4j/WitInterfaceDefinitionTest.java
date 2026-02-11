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

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitInterfaceDefinition} interface.
 *
 * <p>WitInterfaceDefinition represents a WebAssembly Interface Type (WIT) interface definition,
 * providing access to interface metadata, type definitions, function signatures, and validation
 * capabilities.
 */
@DisplayName("WitInterfaceDefinition Tests")
class WitInterfaceDefinitionTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WitInterfaceDefinition.class.getModifiers()),
          "WitInterfaceDefinition should be public");
      assertTrue(
          WitInterfaceDefinition.class.isInterface(),
          "WitInterfaceDefinition should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Declaration Tests")
  class MethodDeclarationTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = WitInterfaceDefinition.class.getMethod("getName");
      assertNotNull(method);
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      final Method method = WitInterfaceDefinition.class.getMethod("getVersion");
      assertNotNull(method);
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getPackageName method")
    void shouldHaveGetPackageNameMethod() throws NoSuchMethodException {
      final Method method = WitInterfaceDefinition.class.getMethod("getPackageName");
      assertNotNull(method);
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getFunctionNames method")
    void shouldHaveGetFunctionNamesMethod() throws NoSuchMethodException {
      final Method method = WitInterfaceDefinition.class.getMethod("getFunctionNames");
      assertNotNull(method);
      assertEquals(List.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getTypeNames method")
    void shouldHaveGetTypeNamesMethod() throws NoSuchMethodException {
      final Method method = WitInterfaceDefinition.class.getMethod("getTypeNames");
      assertNotNull(method);
      assertEquals(List.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getDependencies method")
    void shouldHaveGetDependenciesMethod() throws NoSuchMethodException {
      final Method method = WitInterfaceDefinition.class.getMethod("getDependencies");
      assertNotNull(method);
      assertEquals(Set.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have isCompatibleWith method")
    void shouldHaveIsCompatibleWithMethod() throws NoSuchMethodException {
      final Method method =
          WitInterfaceDefinition.class.getMethod("isCompatibleWith", WitInterfaceDefinition.class);
      assertNotNull(method);
      assertEquals(WitCompatibilityResult.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getWitText method")
    void shouldHaveGetWitTextMethod() throws NoSuchMethodException {
      final Method method = WitInterfaceDefinition.class.getMethod("getWitText");
      assertNotNull(method);
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getImportNames method")
    void shouldHaveGetImportNamesMethod() throws NoSuchMethodException {
      final Method method = WitInterfaceDefinition.class.getMethod("getImportNames");
      assertNotNull(method);
      assertEquals(List.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getExportNames method")
    void shouldHaveGetExportNamesMethod() throws NoSuchMethodException {
      final Method method = WitInterfaceDefinition.class.getMethod("getExportNames");
      assertNotNull(method);
      assertEquals(List.class, method.getReturnType());
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    @Test
    @DisplayName("should support basic interface implementation")
    void shouldSupportBasicInterfaceImplementation() {
      final WitInterfaceDefinition definition =
          createStubDefinition("test-interface", "0.1.0", "test:package");

      assertEquals("test-interface", definition.getName());
      assertEquals("0.1.0", definition.getVersion());
      assertEquals("test:package", definition.getPackageName());
    }

    @Test
    @DisplayName("should return function names")
    void shouldReturnFunctionNames() {
      final WitInterfaceDefinition definition =
          createStubDefinitionWithFunctions(
              "my-interface", List.of("process", "transform", "validate"));

      final List<String> functions = definition.getFunctionNames();

      assertEquals(3, functions.size());
      assertTrue(functions.contains("process"));
      assertTrue(functions.contains("transform"));
      assertTrue(functions.contains("validate"));
    }

    @Test
    @DisplayName("should return type names")
    void shouldReturnTypeNames() {
      final WitInterfaceDefinition definition =
          createStubDefinitionWithTypes("my-interface", List.of("user", "config", "result"));

      final List<String> types = definition.getTypeNames();

      assertEquals(3, types.size());
      assertTrue(types.contains("user"));
      assertTrue(types.contains("config"));
      assertTrue(types.contains("result"));
    }

    @Test
    @DisplayName("should return dependencies")
    void shouldReturnDependencies() {
      final WitInterfaceDefinition definition =
          createStubDefinitionWithDependencies(
              "my-interface", Set.of("wasi:http/types@0.2.0", "wasi:io/streams@0.2.0"));

      final Set<String> deps = definition.getDependencies();

      assertEquals(2, deps.size());
      assertTrue(deps.contains("wasi:http/types@0.2.0"));
      assertTrue(deps.contains("wasi:io/streams@0.2.0"));
    }

    @Test
    @DisplayName("should return WIT text")
    void shouldReturnWitText() {
      final String witText = "interface my-interface { greet: func(name: string) -> string }";
      final WitInterfaceDefinition definition =
          createStubDefinitionWithWitText("my-interface", witText);

      assertEquals(witText, definition.getWitText());
    }

    @Test
    @DisplayName("should return imports and exports")
    void shouldReturnImportsAndExports() {
      final WitInterfaceDefinition definition =
          createStubDefinitionWithImportsExports(
              "my-interface", List.of("import1", "import2"), List.of("export1"));

      assertEquals(2, definition.getImportNames().size());
      assertEquals(1, definition.getExportNames().size());
    }

    @Test
    @DisplayName("should check compatibility")
    void shouldCheckCompatibility() {
      final WitInterfaceDefinition definition1 =
          createStubDefinition("interface-a", "0.1.0", "pkg");
      final WitInterfaceDefinition definition2 =
          createStubDefinition("interface-b", "0.1.0", "pkg");

      final WitCompatibilityResult result = definition1.isCompatibleWith(definition2);

      assertNotNull(result);
      // Default stub returns compatible result
      assertTrue(result.isCompatible());
    }
  }

  @Nested
  @DisplayName("Empty Interface Tests")
  class EmptyInterfaceTests {

    @Test
    @DisplayName("should handle empty function list")
    void shouldHandleEmptyFunctionList() {
      final WitInterfaceDefinition definition =
          createStubDefinitionWithFunctions("empty-interface", List.of());

      assertTrue(definition.getFunctionNames().isEmpty());
    }

    @Test
    @DisplayName("should handle empty type list")
    void shouldHandleEmptyTypeList() {
      final WitInterfaceDefinition definition =
          createStubDefinitionWithTypes("empty-interface", List.of());

      assertTrue(definition.getTypeNames().isEmpty());
    }

    @Test
    @DisplayName("should handle empty dependencies")
    void shouldHandleEmptyDependencies() {
      final WitInterfaceDefinition definition =
          createStubDefinitionWithDependencies("empty-interface", Set.of());

      assertTrue(definition.getDependencies().isEmpty());
    }
  }

  /**
   * Creates a stub WIT interface definition.
   *
   * @param name interface name
   * @param version interface version
   * @param packageName package name
   * @return stub definition
   */
  private WitInterfaceDefinition createStubDefinition(
      final String name, final String version, final String packageName) {
    return new WitInterfaceDefinition() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getVersion() {
        return version;
      }

      @Override
      public String getPackageName() {
        return packageName;
      }

      @Override
      public List<String> getFunctionNames() {
        return List.of();
      }

      @Override
      public List<String> getTypeNames() {
        return List.of();
      }

      @Override
      public Set<String> getDependencies() {
        return Set.of();
      }

      @Override
      public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
        return WitCompatibilityResult.compatible("Compatible", Set.of());
      }

      @Override
      public String getWitText() {
        return "";
      }

      @Override
      public List<String> getImportNames() {
        return List.of();
      }

      @Override
      public List<String> getExportNames() {
        return List.of();
      }
    };
  }

  private WitInterfaceDefinition createStubDefinitionWithFunctions(
      final String name, final List<String> functions) {
    return new WitInterfaceDefinition() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getVersion() {
        return "0.1.0";
      }

      @Override
      public String getPackageName() {
        return "test:pkg";
      }

      @Override
      public List<String> getFunctionNames() {
        return List.copyOf(functions);
      }

      @Override
      public List<String> getTypeNames() {
        return List.of();
      }

      @Override
      public Set<String> getDependencies() {
        return Set.of();
      }

      @Override
      public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
        return WitCompatibilityResult.compatible("Compatible", Set.of());
      }

      @Override
      public String getWitText() {
        return "";
      }

      @Override
      public List<String> getImportNames() {
        return List.of();
      }

      @Override
      public List<String> getExportNames() {
        return List.of();
      }
    };
  }

  private WitInterfaceDefinition createStubDefinitionWithTypes(
      final String name, final List<String> types) {
    return new WitInterfaceDefinition() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getVersion() {
        return "0.1.0";
      }

      @Override
      public String getPackageName() {
        return "test:pkg";
      }

      @Override
      public List<String> getFunctionNames() {
        return List.of();
      }

      @Override
      public List<String> getTypeNames() {
        return List.copyOf(types);
      }

      @Override
      public Set<String> getDependencies() {
        return Set.of();
      }

      @Override
      public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
        return WitCompatibilityResult.compatible("Compatible", Set.of());
      }

      @Override
      public String getWitText() {
        return "";
      }

      @Override
      public List<String> getImportNames() {
        return List.of();
      }

      @Override
      public List<String> getExportNames() {
        return List.of();
      }
    };
  }

  private WitInterfaceDefinition createStubDefinitionWithDependencies(
      final String name, final Set<String> dependencies) {
    return new WitInterfaceDefinition() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getVersion() {
        return "0.1.0";
      }

      @Override
      public String getPackageName() {
        return "test:pkg";
      }

      @Override
      public List<String> getFunctionNames() {
        return List.of();
      }

      @Override
      public List<String> getTypeNames() {
        return List.of();
      }

      @Override
      public Set<String> getDependencies() {
        return Set.copyOf(dependencies);
      }

      @Override
      public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
        return WitCompatibilityResult.compatible("Compatible", Set.of());
      }

      @Override
      public String getWitText() {
        return "";
      }

      @Override
      public List<String> getImportNames() {
        return List.of();
      }

      @Override
      public List<String> getExportNames() {
        return List.of();
      }
    };
  }

  private WitInterfaceDefinition createStubDefinitionWithWitText(
      final String name, final String witText) {
    return new WitInterfaceDefinition() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getVersion() {
        return "0.1.0";
      }

      @Override
      public String getPackageName() {
        return "test:pkg";
      }

      @Override
      public List<String> getFunctionNames() {
        return List.of();
      }

      @Override
      public List<String> getTypeNames() {
        return List.of();
      }

      @Override
      public Set<String> getDependencies() {
        return Set.of();
      }

      @Override
      public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
        return WitCompatibilityResult.compatible("Compatible", Set.of());
      }

      @Override
      public String getWitText() {
        return witText;
      }

      @Override
      public List<String> getImportNames() {
        return List.of();
      }

      @Override
      public List<String> getExportNames() {
        return List.of();
      }
    };
  }

  private WitInterfaceDefinition createStubDefinitionWithImportsExports(
      final String name, final List<String> imports, final List<String> exports) {
    return new WitInterfaceDefinition() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getVersion() {
        return "0.1.0";
      }

      @Override
      public String getPackageName() {
        return "test:pkg";
      }

      @Override
      public List<String> getFunctionNames() {
        return List.of();
      }

      @Override
      public List<String> getTypeNames() {
        return List.of();
      }

      @Override
      public Set<String> getDependencies() {
        return Set.of();
      }

      @Override
      public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
        return WitCompatibilityResult.compatible("Compatible", Set.of());
      }

      @Override
      public String getWitText() {
        return "";
      }

      @Override
      public List<String> getImportNames() {
        return List.copyOf(imports);
      }

      @Override
      public List<String> getExportNames() {
        return List.copyOf(exports);
      }
    };
  }
}
