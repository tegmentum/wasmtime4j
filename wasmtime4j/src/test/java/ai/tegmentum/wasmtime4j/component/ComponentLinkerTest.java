package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentLinker;
import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ComponentLinker} interface.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Interface structure and method signatures
 *   <li>Inheritance from Closeable
 *   <li>Type parameter handling
 *   <li>Method return types and exception declarations
 * </ul>
 *
 * <p>Note: These tests focus on the interface contract and expected behavior.
 * Implementation-specific tests are in the JNI/Panama modules.
 */
@DisplayName("ComponentLinker Tests")
class ComponentLinkerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ComponentLinker should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ComponentLinker.class.isInterface(), "ComponentLinker should be an interface");
    }

    @Test
    @DisplayName("ComponentLinker should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(ComponentLinker.class),
          "ComponentLinker should extend Closeable");
    }

    @Test
    @DisplayName("ComponentLinker should be generic with type parameter T")
    void shouldBeGenericWithTypeParameterT() {
      assertEquals(
          1,
          ComponentLinker.class.getTypeParameters().length,
          "ComponentLinker should have exactly one type parameter");
      assertEquals(
          "T",
          ComponentLinker.class.getTypeParameters()[0].getName(),
          "Type parameter should be named T");
    }
  }

  @Nested
  @DisplayName("Method Existence Tests")
  class MethodExistenceTests {

    @Test
    @DisplayName("should have defineFunction with namespace/interface/function/implementation")
    void shouldHaveDefineFunctionWithFourParams() throws Exception {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "defineFunction",
              String.class,
              String.class,
              String.class,
              Class.forName("ai.tegmentum.wasmtime4j.ComponentHostFunction")),
          "ComponentLinker should have defineFunction(String, String, String,"
              + " ComponentHostFunction)");
    }

    @Test
    @DisplayName("should have defineFunction with witPath and implementation")
    void shouldHaveDefineFunctionWithTwoParams() throws Exception {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "defineFunction",
              String.class,
              Class.forName("ai.tegmentum.wasmtime4j.ComponentHostFunction")),
          "ComponentLinker should have defineFunction(String, ComponentHostFunction)");
    }

    @Test
    @DisplayName("should have defineInterface method")
    void shouldHaveDefineInterfaceMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod("defineInterface", String.class, String.class, Map.class),
          "ComponentLinker should have defineInterface(String, String, Map)");
    }

    @Test
    @DisplayName("should have defineResource method")
    void shouldHaveDefineResourceMethod() throws Exception {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "defineResource",
              String.class,
              String.class,
              String.class,
              Class.forName("ai.tegmentum.wasmtime4j.ComponentResourceDefinition")),
          "ComponentLinker should have defineResource method");
    }

    @Test
    @DisplayName("should have linkInstance method")
    void shouldHaveLinkInstanceMethod() throws Exception {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "linkInstance", Class.forName("ai.tegmentum.wasmtime4j.ComponentInstance")),
          "ComponentLinker should have linkInstance(ComponentInstance)");
    }

    @Test
    @DisplayName("should have linkComponent method")
    void shouldHaveLinkComponentMethod() throws Exception {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "linkComponent",
              Class.forName("ai.tegmentum.wasmtime4j.Store"),
              Class.forName("ai.tegmentum.wasmtime4j.ComponentSimple")),
          "ComponentLinker should have linkComponent(Store, ComponentSimple)");
    }

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() throws Exception {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "instantiate",
              Class.forName("ai.tegmentum.wasmtime4j.Store"),
              Class.forName("ai.tegmentum.wasmtime4j.ComponentSimple")),
          "ComponentLinker should have instantiate(Store, ComponentSimple)");
    }

    @Test
    @DisplayName("should have enableWasiPreview2 method without params")
    void shouldHaveEnableWasiPreview2NoParams() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod("enableWasiPreview2"),
          "ComponentLinker should have enableWasiPreview2()");
    }

    @Test
    @DisplayName("should have enableWasiPreview2 method with config")
    void shouldHaveEnableWasiPreview2WithConfig() throws Exception {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "enableWasiPreview2", Class.forName("ai.tegmentum.wasmtime4j.WasiPreview2Config")),
          "ComponentLinker should have enableWasiPreview2(WasiPreview2Config)");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod("getEngine"), "ComponentLinker should have getEngine()");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod("isValid"), "ComponentLinker should have isValid()");
    }

    @Test
    @DisplayName("should have hasInterface method")
    void shouldHaveHasInterfaceMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod("hasInterface", String.class, String.class),
          "ComponentLinker should have hasInterface(String, String)");
    }

    @Test
    @DisplayName("should have hasFunction method")
    void shouldHaveHasFunctionMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod("hasFunction", String.class, String.class, String.class),
          "ComponentLinker should have hasFunction(String, String, String)");
    }

    @Test
    @DisplayName("should have getDefinedInterfaces method")
    void shouldHaveGetDefinedInterfacesMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod("getDefinedInterfaces"),
          "ComponentLinker should have getDefinedInterfaces()");
    }

    @Test
    @DisplayName("should have getDefinedFunctions method")
    void shouldHaveGetDefinedFunctionsMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod("getDefinedFunctions", String.class, String.class),
          "ComponentLinker should have getDefinedFunctions(String, String)");
    }

    @Test
    @DisplayName("should have validateImports method")
    void shouldHaveValidateImportsMethod() throws Exception {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "validateImports", Class.forName("ai.tegmentum.wasmtime4j.ComponentSimple")),
          "ComponentLinker should have validateImports(ComponentSimple)");
    }

    @Test
    @DisplayName("should have aliasInterface method")
    void shouldHaveAliasInterfaceMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "aliasInterface", String.class, String.class, String.class, String.class),
          "ComponentLinker should have aliasInterface(String, String, String, String)");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentLinker.class.getMethod("close"), "ComponentLinker should have close()");
    }

    @Test
    @DisplayName("should have static create method")
    void shouldHaveStaticCreateMethod() throws Exception {
      assertNotNull(
          ComponentLinker.class.getMethod(
              "create", Class.forName("ai.tegmentum.wasmtime4j.Engine")),
          "ComponentLinker should have static create(Engine)");
    }
  }

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("getEngine should return Engine")
    void getEngineShouldReturnEngine() throws Exception {
      Class<?> returnType = ComponentLinker.class.getMethod("getEngine").getReturnType();
      assertEquals("Engine", returnType.getSimpleName(), "getEngine should return Engine");
    }

    @Test
    @DisplayName("isValid should return boolean")
    void isValidShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType = ComponentLinker.class.getMethod("isValid").getReturnType();
      assertEquals(boolean.class, returnType, "isValid should return boolean");
    }

    @Test
    @DisplayName("hasInterface should return boolean")
    void hasInterfaceShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType =
          ComponentLinker.class
              .getMethod("hasInterface", String.class, String.class)
              .getReturnType();
      assertEquals(boolean.class, returnType, "hasInterface should return boolean");
    }

    @Test
    @DisplayName("hasFunction should return boolean")
    void hasFunctionShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType =
          ComponentLinker.class
              .getMethod("hasFunction", String.class, String.class, String.class)
              .getReturnType();
      assertEquals(boolean.class, returnType, "hasFunction should return boolean");
    }

    @Test
    @DisplayName("getDefinedInterfaces should return Set")
    void getDefinedInterfacesShouldReturnSet() throws NoSuchMethodException {
      Class<?> returnType = ComponentLinker.class.getMethod("getDefinedInterfaces").getReturnType();
      assertEquals(Set.class, returnType, "getDefinedInterfaces should return Set");
    }

    @Test
    @DisplayName("getDefinedFunctions should return Set")
    void getDefinedFunctionsShouldReturnSet() throws NoSuchMethodException {
      Class<?> returnType =
          ComponentLinker.class
              .getMethod("getDefinedFunctions", String.class, String.class)
              .getReturnType();
      assertEquals(Set.class, returnType, "getDefinedFunctions should return Set");
    }

    @Test
    @DisplayName("instantiate should return ComponentInstance")
    void instantiateShouldReturnComponentInstance() throws Exception {
      Class<?> returnType =
          ComponentLinker.class
              .getMethod(
                  "instantiate",
                  Class.forName("ai.tegmentum.wasmtime4j.Store"),
                  Class.forName("ai.tegmentum.wasmtime4j.ComponentSimple"))
              .getReturnType();
      assertEquals(
          "ComponentInstance",
          returnType.getSimpleName(),
          "instantiate should return ComponentInstance");
    }

    @Test
    @DisplayName("linkComponent should return ComponentInstance")
    void linkComponentShouldReturnComponentInstance() throws Exception {
      Class<?> returnType =
          ComponentLinker.class
              .getMethod(
                  "linkComponent",
                  Class.forName("ai.tegmentum.wasmtime4j.Store"),
                  Class.forName("ai.tegmentum.wasmtime4j.ComponentSimple"))
              .getReturnType();
      assertEquals(
          "ComponentInstance",
          returnType.getSimpleName(),
          "linkComponent should return ComponentInstance");
    }

    @Test
    @DisplayName("validateImports should return ComponentImportValidation")
    void validateImportsShouldReturnComponentImportValidation() throws Exception {
      Class<?> returnType =
          ComponentLinker.class
              .getMethod(
                  "validateImports", Class.forName("ai.tegmentum.wasmtime4j.ComponentSimple"))
              .getReturnType();
      assertEquals(
          "ComponentImportValidation",
          returnType.getSimpleName(),
          "validateImports should return ComponentImportValidation");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("defineFunction should declare WasmException")
    void defineFunctionShouldDeclareWasmException() throws Exception {
      Class<?>[] exceptionTypes =
          ComponentLinker.class
              .getMethod(
                  "defineFunction",
                  String.class,
                  Class.forName("ai.tegmentum.wasmtime4j.ComponentHostFunction"))
              .getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "defineFunction should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "defineFunction should declare WasmException");
    }

    @Test
    @DisplayName("instantiate should declare WasmException")
    void instantiateShouldDeclareWasmException() throws Exception {
      Class<?>[] exceptionTypes =
          ComponentLinker.class
              .getMethod(
                  "instantiate",
                  Class.forName("ai.tegmentum.wasmtime4j.Store"),
                  Class.forName("ai.tegmentum.wasmtime4j.ComponentSimple"))
              .getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "instantiate should declare at least one exception");

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
    @DisplayName("enableWasiPreview2 should declare WasmException")
    void enableWasiPreview2ShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          ComponentLinker.class.getMethod("enableWasiPreview2").getExceptionTypes();
      assertTrue(
          exceptionTypes.length > 0, "enableWasiPreview2 should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "enableWasiPreview2 should declare WasmException");
    }

    @Test
    @DisplayName("aliasInterface should declare WasmException")
    void aliasInterfaceShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          ComponentLinker.class
              .getMethod("aliasInterface", String.class, String.class, String.class, String.class)
              .getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "aliasInterface should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "aliasInterface should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Related Type Tests")
  class RelatedTypeTests {

    @Test
    @DisplayName("Engine should exist")
    void engineShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.Engine");
        assertNotNull(clazz, "Engine class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("Engine class should exist", e);
      }
    }

    @Test
    @DisplayName("Store should exist")
    void storeShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.Store");
        assertNotNull(clazz, "Store class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("Store class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentSimple should exist")
    void componentSimpleShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentSimple");
        assertNotNull(clazz, "ComponentSimple class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentSimple class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentInstance should exist")
    void componentInstanceShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentInstance");
        assertNotNull(clazz, "ComponentInstance class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentInstance class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentHostFunction should exist")
    void componentHostFunctionShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentHostFunction");
        assertNotNull(clazz, "ComponentHostFunction class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentHostFunction class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentResourceDefinition should exist")
    void componentResourceDefinitionShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentResourceDefinition");
        assertNotNull(clazz, "ComponentResourceDefinition class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentResourceDefinition class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiPreview2Config should exist")
    void wasiPreview2ConfigShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.WasiPreview2Config");
        assertNotNull(clazz, "WasiPreview2Config class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiPreview2Config class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentImportValidation should exist")
    void componentImportValidationShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentImportValidation");
        assertNotNull(clazz, "ComponentImportValidation class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentImportValidation class should exist", e);
      }
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (java.lang.reflect.Method method : ComponentLinker.class.getDeclaredMethods()) {
        // Skip synthetic methods added by instrumentation (e.g., Jacoco's $jacocoInit)
        if (method.isSynthetic()) {
          continue;
        }
        assertTrue(
            java.lang.reflect.Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }

    @Test
    @DisplayName("should not have any fields")
    void shouldNotHaveFields() {
      assertEquals(
          0,
          ComponentLinker.class.getDeclaredFields().length,
          "ComponentLinker interface should not have any fields");
    }

    @Test
    @DisplayName("create method should be static")
    void createMethodShouldBeStatic() throws Exception {
      java.lang.reflect.Method createMethod =
          ComponentLinker.class.getMethod(
              "create", Class.forName("ai.tegmentum.wasmtime4j.Engine"));
      assertTrue(
          java.lang.reflect.Modifier.isStatic(createMethod.getModifiers()),
          "create method should be static");
    }
  }
}
