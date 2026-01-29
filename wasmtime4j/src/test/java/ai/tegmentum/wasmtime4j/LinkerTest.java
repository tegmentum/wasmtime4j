package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Linker} interface.
 *
 * <p>This test class verifies the structure and contract of the Linker interface, which provides
 * the mechanism to define host functions and resolve imports before instantiating WebAssembly
 * modules.
 */
@DisplayName("Linker Interface Tests")
class LinkerTest {

  @Nested
  @DisplayName("Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("Linker should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Linker.class.isInterface(), "Linker should be an interface");
    }

    @Test
    @DisplayName("Linker should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(Closeable.class.isAssignableFrom(Linker.class), "Linker should extend Closeable");
    }

    @Test
    @DisplayName("Linker should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Linker.class.getModifiers()), "Linker should be a public interface");
    }

    @Test
    @DisplayName("Linker should be generic with type parameter T")
    void shouldBeGenericWithTypeParameterT() {
      final TypeVariable<?>[] typeParameters = Linker.class.getTypeParameters();
      assertEquals(1, typeParameters.length, "Linker should have exactly one type parameter");
      assertEquals("T", typeParameters[0].getName(), "Type parameter should be named T");
    }
  }

  @Nested
  @DisplayName("Host Function Definition Method Tests")
  class HostFunctionDefinitionMethodTests {

    @Test
    @DisplayName("Should have defineHostFunction method")
    void shouldHaveDefineHostFunctionMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod(
              "defineHostFunction",
              String.class,
              String.class,
              FunctionType.class,
              HostFunction.class);
      assertNotNull(method, "defineHostFunction method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have funcNewUnchecked method")
    void shouldHaveFuncNewUncheckedMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod(
              "funcNewUnchecked",
              Store.class,
              String.class,
              String.class,
              FunctionType.class,
              HostFunction.class);
      assertNotNull(method, "funcNewUnchecked method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Resource Definition Method Tests")
  class ResourceDefinitionMethodTests {

    @Test
    @DisplayName("Should have defineMemory method")
    void shouldHaveDefineMemoryMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod(
              "defineMemory", Store.class, String.class, String.class, WasmMemory.class);
      assertNotNull(method, "defineMemory method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have defineTable method")
    void shouldHaveDefineTableMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod(
              "defineTable", Store.class, String.class, String.class, WasmTable.class);
      assertNotNull(method, "defineTable method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have defineGlobal method")
    void shouldHaveDefineGlobalMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod(
              "defineGlobal", Store.class, String.class, String.class, WasmGlobal.class);
      assertNotNull(method, "defineGlobal method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have defineInstance method")
    void shouldHaveDefineInstanceMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("defineInstance", String.class, Instance.class);
      assertNotNull(method, "defineInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have defineName method")
    void shouldHaveDefineNameMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod("defineName", Store.class, String.class, Extern.class);
      assertNotNull(method, "defineName method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Alias Method Tests")
  class AliasMethodTests {

    @Test
    @DisplayName("Should have alias method")
    void shouldHaveAliasMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod("alias", String.class, String.class, String.class, String.class);
      assertNotNull(method, "alias method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have aliasModule default method")
    void shouldHaveAliasModuleMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("aliasModule", String.class, String.class);
      assertNotNull(method, "aliasModule method should exist");
      assertTrue(method.isDefault(), "aliasModule should be a default method");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Instantiation Method Tests")
  class InstantiationMethodTests {

    @Test
    @DisplayName("Should have instantiate(Store, Module) method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("instantiate", Store.class, Module.class);
      assertNotNull(method, "instantiate(Store, Module) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("Should have instantiate(Store, String, Module) method")
    void shouldHaveInstantiateWithNameMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod("instantiate", Store.class, String.class, Module.class);
      assertNotNull(method, "instantiate(Store, String, Module) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("Should have instantiatePre method")
    void shouldHaveInstantiatePreMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("instantiatePre", Module.class);
      assertNotNull(method, "instantiatePre(Module) method should exist");
      assertEquals(InstancePre.class, method.getReturnType(), "Should return InstancePre");
    }
  }

  @Nested
  @DisplayName("Async Instantiation Method Tests")
  class AsyncInstantiationMethodTests {

    @Test
    @DisplayName("Should have instantiateAsync(Store, Module) default method")
    void shouldHaveInstantiateAsyncMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("instantiateAsync", Store.class, Module.class);
      assertNotNull(method, "instantiateAsync(Store, Module) method should exist");
      assertTrue(method.isDefault(), "instantiateAsync should be a default method");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("Should have instantiateAsync(Store, String, Module) default method")
    void shouldHaveInstantiateAsyncWithNameMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod("instantiateAsync", Store.class, String.class, Module.class);
      assertNotNull(method, "instantiateAsync(Store, String, Module) method should exist");
      assertTrue(method.isDefault(), "instantiateAsync should be a default method");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("Should have moduleAsync default method")
    void shouldHaveModuleAsyncMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod("moduleAsync", Store.class, String.class, Module.class);
      assertNotNull(method, "moduleAsync method should exist");
      assertTrue(method.isDefault(), "moduleAsync should be a default method");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("WASI Method Tests")
  class WasiMethodTests {

    @Test
    @DisplayName("Should have enableWasi method")
    void shouldHaveEnableWasiMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("enableWasi");
      assertNotNull(method, "enableWasi() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("Should have allowShadowing method")
    void shouldHaveAllowShadowingMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("allowShadowing", boolean.class);
      assertNotNull(method, "allowShadowing(boolean) method should exist");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker for chaining");
    }

    @Test
    @DisplayName("Should have allowUnknownExports method")
    void shouldHaveAllowUnknownExportsMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("allowUnknownExports", boolean.class);
      assertNotNull(method, "allowUnknownExports(boolean) method should exist");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker for chaining");
    }
  }

  @Nested
  @DisplayName("Import Resolution Method Tests")
  class ImportResolutionMethodTests {

    @Test
    @DisplayName("Should have hasImport method")
    void shouldHaveHasImportMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("hasImport", String.class, String.class);
      assertNotNull(method, "hasImport(String, String) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have defineUnknownImportsAsTraps method")
    void shouldHaveDefineUnknownImportsAsTrapsMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod("defineUnknownImportsAsTraps", Store.class, Module.class);
      assertNotNull(method, "defineUnknownImportsAsTraps method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have defineUnknownImportsAsDefaultValues method")
    void shouldHaveDefineUnknownImportsAsDefaultValuesMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod("defineUnknownImportsAsDefaultValues", Store.class, Module.class);
      assertNotNull(method, "defineUnknownImportsAsDefaultValues method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Dependency Resolution Method Tests")
  class DependencyResolutionMethodTests {

    @Test
    @DisplayName("Should have resolveDependencies method")
    void shouldHaveResolveDependenciesMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("resolveDependencies", Module[].class);
      assertNotNull(method, "resolveDependencies(Module...) method should exist");
      assertEquals(
          DependencyResolution.class, method.getReturnType(), "Should return DependencyResolution");
    }

    @Test
    @DisplayName("Should have validateImports method")
    void shouldHaveValidateImportsMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("validateImports", Module[].class);
      assertNotNull(method, "validateImports(Module...) method should exist");
      assertEquals(
          ImportValidation.class, method.getReturnType(), "Should return ImportValidation");
    }
  }

  @Nested
  @DisplayName("Registry Method Tests")
  class RegistryMethodTests {

    @Test
    @DisplayName("Should have getImportRegistry method")
    void shouldHaveGetImportRegistryMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("getImportRegistry");
      assertNotNull(method, "getImportRegistry() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have iter method")
    void shouldHaveIterMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("iter");
      assertNotNull(method, "iter() method should exist");
      assertEquals(Iterable.class, method.getReturnType(), "Should return Iterable");
    }

    @Test
    @DisplayName("Should have getByImport method")
    void shouldHaveGetByImportMethod() throws NoSuchMethodException {
      final Method method =
          Linker.class.getMethod("getByImport", Store.class, String.class, String.class);
      assertNotNull(method, "getByImport method should exist");
      assertEquals(Extern.class, method.getReturnType(), "Should return Extern");
    }

    @Test
    @DisplayName("Should have getDefault method")
    void shouldHaveGetDefaultMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("getDefault", Store.class, String.class);
      assertNotNull(method, "getDefault method should exist");
      assertEquals(WasmFunction.class, method.getReturnType(), "Should return WasmFunction");
    }
  }

  @Nested
  @DisplayName("Engine and Validity Method Tests")
  class EngineAndValidityMethodTests {

    @Test
    @DisplayName("Should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("getEngine");
      assertNotNull(method, "getEngine() method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("Should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("isValid");
      assertNotNull(method, "isValid() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("close");
      assertNotNull(method, "close() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("Should have static create(Engine) method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      final Method method = Linker.class.getMethod("create", Engine.class);
      assertNotNull(method, "create(Engine) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }
  }

  @Nested
  @DisplayName("Inner Class Tests")
  class InnerClassTests {

    @Test
    @DisplayName("Should have LinkerDefinition inner class")
    void shouldHaveLinkerDefinitionInnerClass() {
      assertNotNull(
          Linker.LinkerDefinition.class, "Linker should have LinkerDefinition inner class");
      assertTrue(
          Modifier.isFinal(Linker.LinkerDefinition.class.getModifiers()),
          "LinkerDefinition should be final");
    }

    @Test
    @DisplayName("LinkerDefinition should have getModuleName method")
    void linkerDefinitionShouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      final Method method = Linker.LinkerDefinition.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName() method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("LinkerDefinition should have getName method")
    void linkerDefinitionShouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = Linker.LinkerDefinition.class.getMethod("getName");
      assertNotNull(method, "getName() method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("LinkerDefinition should have getType method")
    void linkerDefinitionShouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = Linker.LinkerDefinition.class.getMethod("getType");
      assertNotNull(method, "getType() method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Should return ExternType");
    }
  }
}
