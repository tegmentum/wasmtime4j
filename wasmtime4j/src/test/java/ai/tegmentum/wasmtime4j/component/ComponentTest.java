package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Component} interface.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Interface structure and hierarchy
 *   <li>Inheritance from ComponentSimple
 *   <li>Interface contract expectations
 * </ul>
 *
 * <p>Note: These tests focus on the interface contract and expected behavior.
 * Implementation-specific tests are in the JNI/Panama modules.
 */
@DisplayName("Component Tests")
class ComponentTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("Component should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Component.class.isInterface(), "Component should be an interface");
    }

    @Test
    @DisplayName("Component should extend ComponentSimple")
    void shouldExtendComponentSimple() {
      assertTrue(
          ComponentSimple.class.isAssignableFrom(Component.class),
          "Component should extend ComponentSimple");
    }

    @Test
    @DisplayName("Component should extend AutoCloseable via ComponentSimple")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(Component.class),
          "Component should extend AutoCloseable");
    }

    @Test
    @DisplayName("Component should have no additional declared methods")
    void shouldHaveNoAdditionalDeclaredMethods() {
      // Component is an extension point - currently just extends ComponentSimple
      int declaredMethodCount = Component.class.getDeclaredMethods().length;
      assertEquals(
          0,
          declaredMethodCount,
          "Component should have no additional declared methods (it's an extension point)");
    }
  }

  @Nested
  @DisplayName("Inherited Method Tests")
  class InheritedMethodTests {

    @Test
    @DisplayName("should inherit getId method from ComponentSimple")
    void shouldInheritGetIdMethod() throws NoSuchMethodException {
      assertNotNull(Component.class.getMethod("getId"), "Component should inherit getId() method");
    }

    @Test
    @DisplayName("should inherit getVersion method from ComponentSimple")
    void shouldInheritGetVersionMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("getVersion"), "Component should inherit getVersion() method");
    }

    @Test
    @DisplayName("should inherit getSize method from ComponentSimple")
    void shouldInheritGetSizeMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("getSize"), "Component should inherit getSize() method");
    }

    @Test
    @DisplayName("should inherit getMetadata method from ComponentSimple")
    void shouldInheritGetMetadataMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("getMetadata"),
          "Component should inherit getMetadata() method");
    }

    @Test
    @DisplayName("should inherit exportsInterface method from ComponentSimple")
    void shouldInheritExportsInterfaceMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("exportsInterface", String.class),
          "Component should inherit exportsInterface(String) method");
    }

    @Test
    @DisplayName("should inherit importsInterface method from ComponentSimple")
    void shouldInheritImportsInterfaceMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("importsInterface", String.class),
          "Component should inherit importsInterface(String) method");
    }

    @Test
    @DisplayName("should inherit getExportedInterfaces method from ComponentSimple")
    void shouldInheritGetExportedInterfacesMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("getExportedInterfaces"),
          "Component should inherit getExportedInterfaces() method");
    }

    @Test
    @DisplayName("should inherit getImportedInterfaces method from ComponentSimple")
    void shouldInheritGetImportedInterfacesMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("getImportedInterfaces"),
          "Component should inherit getImportedInterfaces() method");
    }

    @Test
    @DisplayName("should inherit instantiate method from ComponentSimple")
    void shouldInheritInstantiateMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("instantiate"),
          "Component should inherit instantiate() method");
    }

    @Test
    @DisplayName("should inherit getDependencyGraph method from ComponentSimple")
    void shouldInheritGetDependencyGraphMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("getDependencyGraph"),
          "Component should inherit getDependencyGraph() method");
    }

    @Test
    @DisplayName("should inherit getWitInterface method from ComponentSimple")
    void shouldInheritGetWitInterfaceMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("getWitInterface"),
          "Component should inherit getWitInterface() method");
    }

    @Test
    @DisplayName("should inherit getResourceUsage method from ComponentSimple")
    void shouldInheritGetResourceUsageMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("getResourceUsage"),
          "Component should inherit getResourceUsage() method");
    }

    @Test
    @DisplayName("should inherit getLifecycleState method from ComponentSimple")
    void shouldInheritGetLifecycleStateMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("getLifecycleState"),
          "Component should inherit getLifecycleState() method");
    }

    @Test
    @DisplayName("should inherit isValid method from ComponentSimple")
    void shouldInheritIsValidMethod() throws NoSuchMethodException {
      assertNotNull(
          Component.class.getMethod("isValid"), "Component should inherit isValid() method");
    }

    @Test
    @DisplayName("should inherit close method from AutoCloseable")
    void shouldInheritCloseMethod() throws NoSuchMethodException {
      assertNotNull(Component.class.getMethod("close"), "Component should inherit close() method");
    }
  }

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("getId should return String")
    void getIdShouldReturnString() throws NoSuchMethodException {
      Class<?> returnType = Component.class.getMethod("getId").getReturnType();
      assertEquals(String.class, returnType, "getId should return String");
    }

    @Test
    @DisplayName("getSize should return long")
    void getSizeShouldReturnLong() throws NoSuchMethodException {
      Class<?> returnType = Component.class.getMethod("getSize").getReturnType();
      assertEquals(long.class, returnType, "getSize should return long");
    }

    @Test
    @DisplayName("isValid should return boolean")
    void isValidShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType = Component.class.getMethod("isValid").getReturnType();
      assertEquals(boolean.class, returnType, "isValid should return boolean");
    }

    @Test
    @DisplayName("exportsInterface should return boolean")
    void exportsInterfaceShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType =
          Component.class.getMethod("exportsInterface", String.class).getReturnType();
      assertEquals(boolean.class, returnType, "exportsInterface should return boolean");
    }

    @Test
    @DisplayName("importsInterface should return boolean")
    void importsInterfaceShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType =
          Component.class.getMethod("importsInterface", String.class).getReturnType();
      assertEquals(boolean.class, returnType, "importsInterface should return boolean");
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
    @DisplayName("ComponentVersion should exist as return type")
    void componentVersionShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentVersion");
        assertNotNull(clazz, "ComponentVersion class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentVersion class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentMetadata should exist as return type")
    void componentMetadataShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentMetadata");
        assertNotNull(clazz, "ComponentMetadata class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentMetadata class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentInstance should exist as return type")
    void componentInstanceShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentInstance");
        assertNotNull(clazz, "ComponentInstance class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentInstance class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentLifecycleState should exist as return type")
    void componentLifecycleStateShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentLifecycleState");
        assertNotNull(clazz, "ComponentLifecycleState class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentLifecycleState class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentResourceUsage should exist as return type")
    void componentResourceUsageShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentResourceUsage");
        assertNotNull(clazz, "ComponentResourceUsage class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentResourceUsage class should exist", e);
      }
    }

    @Test
    @DisplayName("WitInterfaceDefinition should exist as return type")
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
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("getSize should declare WasmException")
    void getSizeShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes = Component.class.getMethod("getSize").getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "getSize should declare at least one exception");

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
      Class<?>[] exceptionTypes = Component.class.getMethod("instantiate").getExceptionTypes();
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
    @DisplayName("exportsInterface should declare WasmException")
    void exportsInterfaceShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          Component.class.getMethod("exportsInterface", String.class).getExceptionTypes();
      assertTrue(
          exceptionTypes.length > 0, "exportsInterface should declare at least one exception");

      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "exportsInterface should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("Component should be assignable to ComponentSimple")
    void shouldBeAssignableToComponentSimple() {
      assertTrue(
          ComponentSimple.class.isAssignableFrom(Component.class),
          "Component should be assignable to ComponentSimple");
    }

    @Test
    @DisplayName("Component should not be a class")
    void shouldNotBeAClass() {
      assertFalse(
          java.lang.reflect.Modifier.isAbstract(Component.class.getModifiers())
              && !Component.class.isInterface(),
          "Component should be an interface, not an abstract class");
    }

    @Test
    @DisplayName("all inherited methods should be public")
    void allMethodsShouldBePublic() {
      for (java.lang.reflect.Method method : Component.class.getMethods()) {
        // Skip synthetic methods added by instrumentation (e.g., Jacoco's $jacocoInit)
        if (method.isSynthetic()) {
          continue;
        }
        // Skip Object methods
        if (method.getDeclaringClass() == Object.class) {
          continue;
        }
        assertTrue(
            java.lang.reflect.Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }
  }
}
