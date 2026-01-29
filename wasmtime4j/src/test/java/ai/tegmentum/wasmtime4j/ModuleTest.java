package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Module} interface.
 *
 * <p>This test class verifies the structure and contract of the Module interface, which represents
 * a compiled WebAssembly module.
 */
@DisplayName("Module Interface Tests")
class ModuleTest {

  @Nested
  @DisplayName("Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("Module should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Module.class.isInterface(), "Module should be an interface");
    }

    @Test
    @DisplayName("Module should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(Closeable.class.isAssignableFrom(Module.class), "Module should extend Closeable");
    }

    @Test
    @DisplayName("Module should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Module.class.getModifiers()), "Module should be a public interface");
    }
  }

  @Nested
  @DisplayName("Instantiation Method Tests")
  class InstantiationMethodTests {

    @Test
    @DisplayName("Should have instantiate(Store) method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("instantiate", Store.class);
      assertNotNull(method, "instantiate(Store) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("Should have instantiate(Store, ImportMap) method")
    void shouldHaveInstantiateWithImportsMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("instantiate", Store.class, ImportMap.class);
      assertNotNull(method, "instantiate(Store, ImportMap) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }
  }

  @Nested
  @DisplayName("Export Method Tests")
  class ExportMethodTests {

    @Test
    @DisplayName("Should have getExports() method")
    void shouldHaveGetExportsMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getExports");
      assertNotNull(method, "getExports() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have getExportDescriptors() method")
    void shouldHaveGetExportDescriptorsMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getExportDescriptors");
      assertNotNull(method, "getExportDescriptors() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have getModuleExports() method")
    void shouldHaveGetModuleExportsMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getModuleExports");
      assertNotNull(method, "getModuleExports() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have hasExport(String) method")
    void shouldHaveHasExportMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("hasExport", String.class);
      assertNotNull(method, "hasExport(String) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Import Method Tests")
  class ImportMethodTests {

    @Test
    @DisplayName("Should have getImports() method")
    void shouldHaveGetImportsMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getImports");
      assertNotNull(method, "getImports() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have getImportDescriptors() method")
    void shouldHaveGetImportDescriptorsMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getImportDescriptors");
      assertNotNull(method, "getImportDescriptors() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have getModuleImports() method")
    void shouldHaveGetModuleImportsMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getModuleImports");
      assertNotNull(method, "getModuleImports() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have hasImport(String, String) method")
    void shouldHaveHasImportMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("hasImport", String.class, String.class);
      assertNotNull(method, "hasImport(String, String) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Type Introspection Method Tests")
  class TypeIntrospectionMethodTests {

    @Test
    @DisplayName("Should have getFunctionType(String) method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getFunctionType", String.class);
      assertNotNull(method, "getFunctionType(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getGlobalType(String) method")
    void shouldHaveGetGlobalTypeMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getGlobalType", String.class);
      assertNotNull(method, "getGlobalType(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getMemoryType(String) method")
    void shouldHaveGetMemoryTypeMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getMemoryType", String.class);
      assertNotNull(method, "getMemoryType(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getTableType(String) method")
    void shouldHaveGetTableTypeMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getTableType", String.class);
      assertNotNull(method, "getTableType(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getFunctionTypes() method")
    void shouldHaveGetFunctionTypesMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getFunctionTypes");
      assertNotNull(method, "getFunctionTypes() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have getMemoryTypes() method")
    void shouldHaveGetMemoryTypesMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getMemoryTypes");
      assertNotNull(method, "getMemoryTypes() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have getTableTypes() method")
    void shouldHaveGetTableTypesMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getTableTypes");
      assertNotNull(method, "getTableTypes() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have getGlobalTypes() method")
    void shouldHaveGetGlobalTypesMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getGlobalTypes");
      assertNotNull(method, "getGlobalTypes() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("Should have validateImports(ImportMap) method")
    void shouldHaveValidateImportsMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("validateImports", ImportMap.class);
      assertNotNull(method, "validateImports(ImportMap) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have validateImportsDetailed(ImportMap) method")
    void shouldHaveValidateImportsDetailedMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("validateImportsDetailed", ImportMap.class);
      assertNotNull(method, "validateImportsDetailed(ImportMap) method should exist");
      assertEquals(
          ImportValidation.class, method.getReturnType(), "Should return ImportValidation");
    }

    @Test
    @DisplayName("Should have static validate(Engine, byte[]) method")
    void shouldHaveStaticValidateMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("validate", Engine.class, byte[].class);
      assertNotNull(method, "validate(Engine, byte[]) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "validate should be static");
      assertEquals(
          ModuleValidationResult.class,
          method.getReturnType(),
          "Should return ModuleValidationResult");
    }
  }

  @Nested
  @DisplayName("Engine and State Method Tests")
  class EngineAndStateMethodTests {

    @Test
    @DisplayName("Should have getEngine() method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getEngine");
      assertNotNull(method, "getEngine() method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("Should have getName() method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getName");
      assertNotNull(method, "getName() method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Should have isValid() method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("isValid");
      assertNotNull(method, "isValid() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have close() method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("close");
      assertNotNull(method, "close() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Serialization Method Tests")
  class SerializationMethodTests {

    @Test
    @DisplayName("Should have serialize() method")
    void shouldHaveSerializeMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("serialize");
      assertNotNull(method, "serialize() method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("Should have static deserialize(Engine, byte[]) method")
    void shouldHaveStaticDeserializeMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("deserialize", Engine.class, byte[].class);
      assertNotNull(method, "deserialize(Engine, byte[]) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "deserialize should be static");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("Should have static deserializeFile(Engine, Path) method")
    void shouldHaveStaticDeserializeFileMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("deserializeFile", Engine.class, Path.class);
      assertNotNull(method, "deserializeFile(Engine, Path) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "deserializeFile should be static");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("Should have isSerializable() default method")
    void shouldHaveIsSerializableDefaultMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("isSerializable");
      assertNotNull(method, "isSerializable() method should exist");
      assertTrue(method.isDefault(), "isSerializable() should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Custom Sections Method Tests")
  class CustomSectionsMethodTests {

    @Test
    @DisplayName("Should have getCustomSections() method")
    void shouldHaveGetCustomSectionsMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("getCustomSections");
      assertNotNull(method, "getCustomSections() method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Resource Method Tests")
  class ResourceMethodTests {

    @Test
    @DisplayName("Should have resourcesRequired() default method")
    void shouldHaveResourcesRequiredDefaultMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("resourcesRequired");
      assertNotNull(method, "resourcesRequired() method should exist");
      assertTrue(method.isDefault(), "resourcesRequired() should be a default method");
      assertEquals(
          ResourcesRequired.class, method.getReturnType(), "Should return ResourcesRequired");
    }

    @Test
    @DisplayName("Should have functions() default method")
    void shouldHaveFunctionsDefaultMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("functions");
      assertNotNull(method, "functions() method should exist");
      assertTrue(method.isDefault(), "functions() should be a default method");
      assertEquals(Iterable.class, method.getReturnType(), "Should return Iterable");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("Should have static compile(Engine, byte[]) method")
    void shouldHaveStaticCompileMethod() throws NoSuchMethodException {
      final Method method = Module.class.getMethod("compile", Engine.class, byte[].class);
      assertNotNull(method, "compile(Engine, byte[]) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "compile should be static");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }
  }

  @Nested
  @DisplayName("Inner Class Tests")
  class InnerClassTests {

    @Test
    @DisplayName("Should have ModuleImageRange inner class")
    void shouldHaveModuleImageRangeInnerClass() {
      final Class<?>[] declaredClasses = Module.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("ModuleImageRange".equals(clazz.getSimpleName())) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Module should have ModuleImageRange inner class");
    }

    @Test
    @DisplayName("ModuleImageRange should be final")
    void moduleImageRangeShouldBeFinal() throws ClassNotFoundException {
      final Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.Module$ModuleImageRange");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "ModuleImageRange should be final");
    }

    @Test
    @DisplayName("ModuleImageRange should have getStartAddress() method")
    void moduleImageRangeShouldHaveGetStartAddressMethod() throws Exception {
      final Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.Module$ModuleImageRange");
      final Method method = clazz.getMethod("getStartAddress");
      assertNotNull(method, "getStartAddress() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ModuleImageRange should have getLength() method")
    void moduleImageRangeShouldHaveGetLengthMethod() throws Exception {
      final Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.Module$ModuleImageRange");
      final Method method = clazz.getMethod("getLength");
      assertNotNull(method, "getLength() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ModuleImageRange should have getEndAddress() method")
    void moduleImageRangeShouldHaveGetEndAddressMethod() throws Exception {
      final Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.Module$ModuleImageRange");
      final Method method = clazz.getMethod("getEndAddress");
      assertNotNull(method, "getEndAddress() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }
}
