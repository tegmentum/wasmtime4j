package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentInstance;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ComponentInstance} interface.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Interface structure and method signatures
 *   <li>Inheritance from AutoCloseable
 *   <li>Method return types and exception declarations
 *   <li>Related type existence
 * </ul>
 *
 * <p>Note: These tests focus on the interface contract and expected behavior.
 * Implementation-specific tests are in the JNI/Panama modules.
 */
@DisplayName("ComponentInstance Tests")
class ComponentInstanceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ComponentInstance should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ComponentInstance.class.isInterface(), "ComponentInstance should be an interface");
    }

    @Test
    @DisplayName("ComponentInstance should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentInstance.class),
          "ComponentInstance should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Method Existence Tests")
  class MethodExistenceTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("getId"), "ComponentInstance should have getId()");
    }

    @Test
    @DisplayName("should have getComponent method")
    void shouldHaveGetComponentMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("getComponent"),
          "ComponentInstance should have getComponent()");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("getState"),
          "ComponentInstance should have getState()");
    }

    @Test
    @DisplayName("should have invoke method")
    void shouldHaveInvokeMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("invoke", String.class, Object[].class),
          "ComponentInstance should have invoke(String, Object...)");
    }

    @Test
    @DisplayName("should have hasFunction method")
    void shouldHaveHasFunctionMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("hasFunction", String.class),
          "ComponentInstance should have hasFunction(String)");
    }

    @Test
    @DisplayName("should have getFunc method")
    void shouldHaveGetFuncMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("getFunc", String.class),
          "ComponentInstance should have getFunc(String)");
    }

    @Test
    @DisplayName("should have getExportedFunctions method")
    void shouldHaveGetExportedFunctionsMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("getExportedFunctions"),
          "ComponentInstance should have getExportedFunctions()");
    }

    @Test
    @DisplayName("should have getExportedInterfaces method")
    void shouldHaveGetExportedInterfacesMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("getExportedInterfaces"),
          "ComponentInstance should have getExportedInterfaces()");
    }

    @Test
    @DisplayName("should have bindInterface method")
    void shouldHaveBindInterfaceMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("bindInterface", String.class, Object.class),
          "ComponentInstance should have bindInterface(String, Object)");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("getConfig"),
          "ComponentInstance should have getConfig()");
    }

    @Test
    @DisplayName("should have getResourceUsage method")
    void shouldHaveGetResourceUsageMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("getResourceUsage"),
          "ComponentInstance should have getResourceUsage()");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("isValid"), "ComponentInstance should have isValid()");
    }

    @Test
    @DisplayName("should have pause method")
    void shouldHavePauseMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("pause"), "ComponentInstance should have pause()");
    }

    @Test
    @DisplayName("should have resume method")
    void shouldHaveResumeMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("resume"), "ComponentInstance should have resume()");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("stop"), "ComponentInstance should have stop()");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentInstance.class.getMethod("close"), "ComponentInstance should have close()");
    }
  }

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("getId should return String")
    void getIdShouldReturnString() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("getId").getReturnType();
      assertEquals(String.class, returnType, "getId should return String");
    }

    @Test
    @DisplayName("getComponent should return ComponentSimple")
    void getComponentShouldReturnComponentSimple() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("getComponent").getReturnType();
      assertEquals(
          "ComponentSimple",
          returnType.getSimpleName(),
          "getComponent should return ComponentSimple");
    }

    @Test
    @DisplayName("getState should return ComponentInstanceState")
    void getStateShouldReturnComponentInstanceState() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("getState").getReturnType();
      assertEquals(
          "ComponentInstanceState",
          returnType.getSimpleName(),
          "getState should return ComponentInstanceState");
    }

    @Test
    @DisplayName("invoke should return Object")
    void invokeShouldReturnObject() throws NoSuchMethodException {
      Class<?> returnType =
          ComponentInstance.class.getMethod("invoke", String.class, Object[].class).getReturnType();
      assertEquals(Object.class, returnType, "invoke should return Object");
    }

    @Test
    @DisplayName("hasFunction should return boolean")
    void hasFunctionShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType =
          ComponentInstance.class.getMethod("hasFunction", String.class).getReturnType();
      assertEquals(boolean.class, returnType, "hasFunction should return boolean");
    }

    @Test
    @DisplayName("getFunc should return Optional")
    void getFuncShouldReturnOptional() throws NoSuchMethodException {
      Class<?> returnType =
          ComponentInstance.class.getMethod("getFunc", String.class).getReturnType();
      assertEquals(Optional.class, returnType, "getFunc should return Optional");
    }

    @Test
    @DisplayName("getExportedFunctions should return Set")
    void getExportedFunctionsShouldReturnSet() throws NoSuchMethodException {
      Class<?> returnType =
          ComponentInstance.class.getMethod("getExportedFunctions").getReturnType();
      assertEquals(Set.class, returnType, "getExportedFunctions should return Set");
    }

    @Test
    @DisplayName("getExportedInterfaces should return Map")
    void getExportedInterfacesShouldReturnMap() throws NoSuchMethodException {
      Class<?> returnType =
          ComponentInstance.class.getMethod("getExportedInterfaces").getReturnType();
      assertEquals(Map.class, returnType, "getExportedInterfaces should return Map");
    }

    @Test
    @DisplayName("getConfig should return ComponentInstanceConfig")
    void getConfigShouldReturnComponentInstanceConfig() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("getConfig").getReturnType();
      assertEquals(
          "ComponentInstanceConfig",
          returnType.getSimpleName(),
          "getConfig should return ComponentInstanceConfig");
    }

    @Test
    @DisplayName("getResourceUsage should return ComponentResourceUsage")
    void getResourceUsageShouldReturnComponentResourceUsage() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("getResourceUsage").getReturnType();
      assertEquals(
          "ComponentResourceUsage",
          returnType.getSimpleName(),
          "getResourceUsage should return ComponentResourceUsage");
    }

    @Test
    @DisplayName("isValid should return boolean")
    void isValidShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("isValid").getReturnType();
      assertEquals(boolean.class, returnType, "isValid should return boolean");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("invoke should declare WasmException")
    void invokeShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          ComponentInstance.class
              .getMethod("invoke", String.class, Object[].class)
              .getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "invoke should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "invoke should declare WasmException");
    }

    @Test
    @DisplayName("getFunc should declare WasmException")
    void getFuncShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          ComponentInstance.class.getMethod("getFunc", String.class).getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "getFunc should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "getFunc should declare WasmException");
    }

    @Test
    @DisplayName("getExportedInterfaces should declare WasmException")
    void getExportedInterfacesShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          ComponentInstance.class.getMethod("getExportedInterfaces").getExceptionTypes();
      assertTrue(
          exceptionTypes.length > 0, "getExportedInterfaces should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "getExportedInterfaces should declare WasmException");
    }

    @Test
    @DisplayName("bindInterface should declare WasmException")
    void bindInterfaceShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          ComponentInstance.class
              .getMethod("bindInterface", String.class, Object.class)
              .getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "bindInterface should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "bindInterface should declare WasmException");
    }

    @Test
    @DisplayName("pause should declare WasmException")
    void pauseShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes = ComponentInstance.class.getMethod("pause").getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "pause should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "pause should declare WasmException");
    }

    @Test
    @DisplayName("resume should declare WasmException")
    void resumeShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes = ComponentInstance.class.getMethod("resume").getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "resume should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "resume should declare WasmException");
    }

    @Test
    @DisplayName("stop should declare WasmException")
    void stopShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes = ComponentInstance.class.getMethod("stop").getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "stop should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "stop should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Related Type Tests")
  class RelatedTypeTests {

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
    @DisplayName("ComponentInstanceState should exist")
    void componentInstanceStateShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentInstanceState");
        assertNotNull(clazz, "ComponentInstanceState class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentInstanceState class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentFunction should exist")
    void componentFunctionShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentFunction");
        assertNotNull(clazz, "ComponentFunction class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentFunction class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentInstanceConfig should exist")
    void componentInstanceConfigShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentInstanceConfig");
        assertNotNull(clazz, "ComponentInstanceConfig class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentInstanceConfig class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentResourceUsage should exist")
    void componentResourceUsageShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentResourceUsage");
        assertNotNull(clazz, "ComponentResourceUsage class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentResourceUsage class should exist", e);
      }
    }

    @Test
    @DisplayName("WitInterfaceDefinition should exist")
    void witInterfaceDefinitionShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.WitInterfaceDefinition");
        assertNotNull(clazz, "WitInterfaceDefinition class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WitInterfaceDefinition class should exist", e);
      }
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (java.lang.reflect.Method method : ComponentInstance.class.getDeclaredMethods()) {
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
          ComponentInstance.class.getDeclaredFields().length,
          "ComponentInstance interface should not have any fields");
    }

    @Test
    @DisplayName("invoke method should accept varargs")
    void invokeMethodShouldAcceptVarargs() throws NoSuchMethodException {
      java.lang.reflect.Method invokeMethod =
          ComponentInstance.class.getMethod("invoke", String.class, Object[].class);
      assertTrue(invokeMethod.isVarArgs(), "invoke method should accept varargs");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("pause method should return void")
    void pauseMethodShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("pause").getReturnType();
      assertEquals(void.class, returnType, "pause should return void");
    }

    @Test
    @DisplayName("resume method should return void")
    void resumeMethodShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("resume").getReturnType();
      assertEquals(void.class, returnType, "resume should return void");
    }

    @Test
    @DisplayName("stop method should return void")
    void stopMethodShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("stop").getReturnType();
      assertEquals(void.class, returnType, "stop should return void");
    }

    @Test
    @DisplayName("close method should return void")
    void closeMethodShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType = ComponentInstance.class.getMethod("close").getReturnType();
      assertEquals(void.class, returnType, "close should return void");
    }
  }
}
