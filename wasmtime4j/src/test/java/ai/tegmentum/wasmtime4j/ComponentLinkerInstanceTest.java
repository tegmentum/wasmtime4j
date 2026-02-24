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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.component.ComponentHostFunction;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentLinkerInstance;
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentLinkerInstance} and its {@link ComponentLinkerInstance.Scoped} inner
 * class.
 *
 * <p>ComponentLinkerInstance provides a builder-pattern for defining host functions and resources
 * within a scoped interface path of a {@link ComponentLinker}. The Scoped implementation records the
 * interface path and delegates to the underlying linker.
 */
@DisplayName("ComponentLinkerInstance Tests")
class ComponentLinkerInstanceTest {

  @Nested
  @DisplayName("Scoped Constructor Tests")
  class ScopedConstructorTests {

    @Test
    @DisplayName("should create Scoped with empty scope path")
    void shouldCreateScopedWithEmptyScopePath() {
      final ComponentLinker<?> linker = new StubComponentLinker();
      final ComponentLinkerInstance.Scoped scoped =
          new ComponentLinkerInstance.Scoped(linker, "");

      assertNotNull(scoped, "Scoped should be created");
      assertEquals("", scoped.getScopePath(), "Scope path should be empty for root");
    }

    @Test
    @DisplayName("should create Scoped with interface scope path")
    void shouldCreateScopedWithInterfaceScopePath() {
      final ComponentLinker<?> linker = new StubComponentLinker();
      final ComponentLinkerInstance.Scoped scoped =
          new ComponentLinkerInstance.Scoped(linker, "wasi:cli/stdout@0.2.0");

      assertEquals(
          "wasi:cli/stdout@0.2.0", scoped.getScopePath(),
          "Scope path should be the interface path");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when linker is null")
    void shouldThrowWhenLinkerNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentLinkerInstance.Scoped(null, ""),
          "Should throw IllegalArgumentException for null linker");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when scopePath is null")
    void shouldThrowWhenScopePathNull() {
      final ComponentLinker<?> linker = new StubComponentLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentLinkerInstance.Scoped(linker, null),
          "Should throw IllegalArgumentException for null scopePath");
    }
  }

  @Nested
  @DisplayName("FuncNew Tests")
  class FuncNewTests {

    @Test
    @DisplayName("should build root-level WIT path when scope is empty")
    void shouldBuildRootLevelPath() throws WasmException {
      final RecordingComponentLinker linker = new RecordingComponentLinker();
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(linker, "");

      instance.funcNew("my-function", StubComponentLinker.STUB_FUNC);

      assertEquals(
          "my-function", linker.lastDefinedFunctionWitPath,
          "Root scope should use function name directly as WIT path");
    }

    @Test
    @DisplayName("should build scoped WIT path with hash separator")
    void shouldBuildScopedPathWithHash() throws WasmException {
      final RecordingComponentLinker linker = new RecordingComponentLinker();
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(linker, "wasi:cli/stdout@0.2.0");

      instance.funcNew("print", StubComponentLinker.STUB_FUNC);

      assertEquals(
          "wasi:cli/stdout@0.2.0#print", linker.lastDefinedFunctionWitPath,
          "Scoped path should be scope + '#' + function name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when name is null")
    void shouldThrowWhenNameNull() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "scope");

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.funcNew(null, StubComponentLinker.STUB_FUNC),
          "Should throw IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when name is empty")
    void shouldThrowWhenNameEmpty() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "scope");

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.funcNew("", StubComponentLinker.STUB_FUNC),
          "Should throw IllegalArgumentException for empty name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when implementation is null")
    void shouldThrowWhenImplementationNull() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "scope");

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.funcNew("func", null),
          "Should throw IllegalArgumentException for null implementation");
    }
  }

  @Nested
  @DisplayName("FuncNewAsync Tests")
  class FuncNewAsyncTests {

    @Test
    @DisplayName("should build scoped WIT path for async function")
    void shouldBuildScopedPathForAsync() throws WasmException {
      final RecordingComponentLinker linker = new RecordingComponentLinker();
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(linker, "wasi:http/handler@0.2.0");

      instance.funcNewAsync("handle", StubComponentLinker.STUB_FUNC);

      assertEquals(
          "wasi:http/handler@0.2.0#handle", linker.lastDefinedAsyncFunctionWitPath,
          "Async scoped path should be scope + '#' + function name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when name is null")
    void shouldThrowWhenNameNull() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "scope");

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.funcNewAsync(null, StubComponentLinker.STUB_FUNC),
          "Should throw IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when implementation is null")
    void shouldThrowWhenImplementationNull() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "scope");

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.funcNewAsync("func", null),
          "Should throw IllegalArgumentException for null implementation");
    }
  }

  @Nested
  @DisplayName("Instance Nesting Tests")
  class InstanceNestingTests {

    @Test
    @DisplayName("should build nested path from root")
    void shouldBuildNestedPathFromRoot() throws WasmException {
      final ComponentLinkerInstance root =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "");
      final ComponentLinkerInstance nested = root.instance("wasi:cli/stdout@0.2.0");

      assertNotNull(nested, "Nested instance should be created");
      assertEquals(
          "wasi:cli/stdout@0.2.0",
          ((ComponentLinkerInstance.Scoped) nested).getScopePath(),
          "Nested path should be the instance name");
    }

    @Test
    @DisplayName("should build nested path from existing scope")
    void shouldBuildNestedPathFromExistingScope() throws WasmException {
      final ComponentLinkerInstance parent =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "wasi:cli");
      final ComponentLinkerInstance nested = parent.instance("stdout@0.2.0");

      assertEquals(
          "wasi:cli/stdout@0.2.0",
          ((ComponentLinkerInstance.Scoped) nested).getScopePath(),
          "Nested path should be parent scope + '/' + instance name");
    }

    @Test
    @DisplayName("should support multiple levels of nesting")
    void shouldSupportMultipleLevelsOfNesting() throws WasmException {
      final ComponentLinkerInstance root =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "");
      final ComponentLinkerInstance level1 = root.instance("ns");
      final ComponentLinkerInstance level2 = level1.instance("pkg");
      final ComponentLinkerInstance level3 = level2.instance("iface");

      assertEquals(
          "ns/pkg/iface",
          ((ComponentLinkerInstance.Scoped) level3).getScopePath(),
          "Three levels of nesting should build path with '/' separators");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when instance name is null")
    void shouldThrowWhenInstanceNameNull() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "");

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.instance(null),
          "Should throw IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when instance name is empty")
    void shouldThrowWhenInstanceNameEmpty() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "");

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.instance(""),
          "Should throw IllegalArgumentException for empty name");
    }
  }

  @Nested
  @DisplayName("Resource Tests")
  class ResourceTests {

    @Test
    @DisplayName("should throw WasmException when scope is root (empty path)")
    void shouldThrowWhenScopeIsRoot() {
      final ComponentLinkerInstance root =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "");

      assertThrows(
          WasmException.class,
          () -> root.resource("my-resource", StubComponentLinker.STUB_RESOURCE_DEF),
          "Should throw WasmException when defining resource at root scope");
    }

    @Test
    @DisplayName("should throw WasmException when scope has no slash")
    void shouldThrowWhenScopeHasNoSlash() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(new StubComponentLinker(), "no-slash");

      assertThrows(
          WasmException.class,
          () -> instance.resource("my-resource", StubComponentLinker.STUB_RESOURCE_DEF),
          "Should throw WasmException when scope path has no '/' separator");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when resource name is null")
    void shouldThrowWhenResourceNameNull() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(
              new StubComponentLinker(), "wasi:cli/stdout@0.2.0");

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.resource(null, StubComponentLinker.STUB_RESOURCE_DEF),
          "Should throw IllegalArgumentException for null resource name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when definition is null")
    void shouldThrowWhenDefinitionNull() {
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(
              new StubComponentLinker(), "wasi:cli/stdout@0.2.0");

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.resource("my-resource", null),
          "Should throw IllegalArgumentException for null definition");
    }

    @Test
    @DisplayName("should parse namespace and interface correctly")
    void shouldParseNamespaceAndInterface() throws WasmException {
      final RecordingComponentLinker linker = new RecordingComponentLinker();
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(linker, "wasi:cli/stdout@0.2.0");

      instance.resource("my-resource", StubComponentLinker.STUB_RESOURCE_DEF);

      assertEquals("wasi:cli", linker.lastResourceNamespace, "Namespace should be 'wasi:cli'");
      assertEquals(
          "stdout", linker.lastResourceInterfaceName,
          "Interface name should be 'stdout' (version stripped)");
      assertEquals(
          "my-resource", linker.lastResourceName,
          "Resource name should be 'my-resource'");
    }

    @Test
    @DisplayName("should parse interface without version suffix")
    void shouldParseInterfaceWithoutVersion() throws WasmException {
      final RecordingComponentLinker linker = new RecordingComponentLinker();
      final ComponentLinkerInstance instance =
          new ComponentLinkerInstance.Scoped(linker, "custom:pkg/interface-name");

      instance.resource("res", StubComponentLinker.STUB_RESOURCE_DEF);

      assertEquals(
          "custom:pkg", linker.lastResourceNamespace,
          "Namespace should be 'custom:pkg'");
      assertEquals(
          "interface-name", linker.lastResourceInterfaceName,
          "Interface name should be 'interface-name'");
    }
  }

  // ========================================================================
  // Test Stubs
  // ========================================================================

  /**
   * Recording linker that captures the arguments passed to defineFunction, defineFunctionAsync, and
   * defineResource for assertion.
   */
  private static class RecordingComponentLinker extends StubComponentLinker {

    String lastDefinedFunctionWitPath;
    String lastDefinedAsyncFunctionWitPath;
    String lastResourceNamespace;
    String lastResourceInterfaceName;
    String lastResourceName;

    @Override
    public void defineFunction(
        final String witPath, final ComponentHostFunction implementation) throws WasmException {
      this.lastDefinedFunctionWitPath = witPath;
    }

    @Override
    public void defineFunctionAsync(
        final String witPath, final ComponentHostFunction implementation) throws WasmException {
      this.lastDefinedAsyncFunctionWitPath = witPath;
    }

    @Override
    public void defineResource(
        final String interfaceNamespace,
        final String interfaceName,
        final String resourceName,
        final ComponentResourceDefinition<?> resourceDefinition)
        throws WasmException {
      this.lastResourceNamespace = interfaceNamespace;
      this.lastResourceInterfaceName = interfaceName;
      this.lastResourceName = resourceName;
    }
  }

  /**
   * Minimal stub implementation of ComponentLinker for testing ComponentLinkerInstance.Scoped without
   * needing a real native linker.
   */
  @SuppressWarnings("unchecked")
  private static class StubComponentLinker implements ComponentLinker<Object> {

    static final ComponentHostFunction STUB_FUNC = params -> Collections.emptyList();
    static final ComponentResourceDefinition<Object> STUB_RESOURCE_DEF =
        new ComponentResourceDefinition<>() {
          @Override
          public String getName() {
            return "stub-resource";
          }

          @Override
          public Optional<ComponentResourceDefinition.ResourceConstructor<Object>> getConstructor() {
            return Optional.empty();
          }

          @Override
          public Optional<Consumer<Object>> getDestructor() {
            return Optional.empty();
          }

          @Override
          public Map<String, ComponentResourceDefinition.ResourceMethod<Object>> getMethods() {
            return Collections.emptyMap();
          }
        };

    @Override
    public void defineFunction(
        final String interfaceNamespace,
        final String interfaceName,
        final String functionName,
        final ComponentHostFunction implementation) throws WasmException {}

    @Override
    public void defineFunction(
        final String witPath, final ComponentHostFunction implementation) throws WasmException {}

    @Override
    public void defineFunctionAsync(
        final String interfaceNamespace,
        final String interfaceName,
        final String functionName,
        final ComponentHostFunction implementation) throws WasmException {}

    @Override
    public void defineFunctionAsync(
        final String witPath, final ComponentHostFunction implementation) throws WasmException {}

    @Override
    public void defineInterface(
        final String interfaceNamespace,
        final String interfaceName,
        final java.util.Map<String, ComponentHostFunction> functions) throws WasmException {}

    @Override
    public void defineResource(
        final String interfaceNamespace,
        final String interfaceName,
        final String resourceName,
        final ComponentResourceDefinition<?> resourceDefinition) throws WasmException {}

    @Override
    public void linkInstance(
        final ai.tegmentum.wasmtime4j.component.ComponentInstance instance) {}

    @Override
    public ai.tegmentum.wasmtime4j.component.ComponentInstance linkComponent(
        final Store store,
        final ai.tegmentum.wasmtime4j.component.Component component) {
      return null;
    }

    @Override
    public ai.tegmentum.wasmtime4j.component.ComponentInstance instantiate(
        final Store store,
        final ai.tegmentum.wasmtime4j.component.Component component) {
      return null;
    }

    @Override
    public ai.tegmentum.wasmtime4j.component.ComponentInstancePre instantiatePre(
        final ai.tegmentum.wasmtime4j.component.Component component) {
      return null;
    }

    @Override
    public void enableWasiPreview2() {}

    @Override
    public void enableWasiPreview2(
        final ai.tegmentum.wasmtime4j.wasi.WasiPreview2Config config) {}

    @Override
    public Engine getEngine() {
      return null;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public boolean hasInterface(final String interfaceNamespace, final String interfaceName) {
      return false;
    }

    @Override
    public boolean hasFunction(
        final String interfaceNamespace,
        final String interfaceName,
        final String functionName) {
      return false;
    }

    @Override
    public java.util.Set<String> getDefinedInterfaces() {
      return java.util.Collections.emptySet();
    }

    @Override
    public java.util.Set<String> getDefinedFunctions(
        final String interfaceNamespace, final String interfaceName) {
      return java.util.Collections.emptySet();
    }

    @Override
    public void aliasInterface(
        final String fromNamespace,
        final String fromInterface,
        final String toNamespace,
        final String toInterface) {}

    @Override
    public void allowShadowing(final boolean allow) {}

    @Override
    public void defineUnknownImportsAsTraps(
        final ai.tegmentum.wasmtime4j.component.Component component) {}

    @Override
    public void close() {}
  }
}
