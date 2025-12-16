package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link WasiLinker} interface.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Interface contract and method signatures
 *   <li>Directory access configuration methods
 *   <li>Environment variable configuration methods
 *   <li>Standard I/O configuration methods
 *   <li>Network access configuration methods
 *   <li>Static factory methods
 * </ul>
 *
 * <p>Note: These tests focus on the interface contract and expected behavior.
 * Implementation-specific tests are in the JNI/Panama modules.
 */
@DisplayName("WasiLinker Tests")
class WasiLinkerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiLinker should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiLinker.class.isInterface(), "WasiLinker should be an interface");
    }

    @Test
    @DisplayName("WasiLinker should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasiLinker.class), "WasiLinker should extend Closeable");
    }

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (java.lang.reflect.Method method : WasiLinker.class.getDeclaredMethods()) {
        // Skip synthetic methods added by instrumentation (e.g., Jacoco's $jacocoInit)
        if (method.isSynthetic()) {
          continue;
        }
        assertTrue(
            java.lang.reflect.Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static create method with Engine parameter")
    void shouldHaveCreateMethodWithEngine() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("create", Engine.class),
          "WasiLinker should have static create(Engine) method");
    }

    @Test
    @DisplayName("create(Engine) should be static")
    void createEngineShouldBeStatic() throws NoSuchMethodException {
      assertTrue(
          java.lang.reflect.Modifier.isStatic(
              WasiLinker.class.getMethod("create", Engine.class).getModifiers()),
          "create(Engine) should be static");
    }

    @Test
    @DisplayName("create(Engine) should return WasiLinker")
    void createEngineShouldReturnWasiLinker() throws NoSuchMethodException {
      Class<?> returnType = WasiLinker.class.getMethod("create", Engine.class).getReturnType();
      assertEquals(WasiLinker.class, returnType, "create(Engine) should return WasiLinker");
    }

    @Test
    @DisplayName("should have static create method with Engine and WasiConfig parameters")
    void shouldHaveCreateMethodWithEngineAndConfig() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("create", Engine.class, WasiConfig.class),
          "WasiLinker should have static create(Engine, WasiConfig) method");
    }

    @Test
    @DisplayName("create(Engine, WasiConfig) should be static")
    void createEngineConfigShouldBeStatic() throws NoSuchMethodException {
      assertTrue(
          java.lang.reflect.Modifier.isStatic(
              WasiLinker.class.getMethod("create", Engine.class, WasiConfig.class).getModifiers()),
          "create(Engine, WasiConfig) should be static");
    }
  }

  @Nested
  @DisplayName("Directory Access Method Tests")
  class DirectoryAccessMethodTests {

    @Test
    @DisplayName("should have allowDirectoryAccess method with permissions")
    void shouldHaveAllowDirectoryAccessWithPermissions() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod(
              "allowDirectoryAccess", Path.class, String.class, WasiPermissions.class),
          "WasiLinker should have allowDirectoryAccess with permissions");
    }

    @Test
    @DisplayName("allowDirectoryAccess with permissions should return void")
    void allowDirectoryAccessWithPermissionsShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType =
          WasiLinker.class
              .getMethod("allowDirectoryAccess", Path.class, String.class, WasiPermissions.class)
              .getReturnType();
      assertEquals(void.class, returnType, "allowDirectoryAccess should return void");
    }

    @Test
    @DisplayName("should have allowDirectoryAccess method without permissions")
    void shouldHaveAllowDirectoryAccessWithoutPermissions() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("allowDirectoryAccess", Path.class, String.class),
          "WasiLinker should have allowDirectoryAccess without permissions");
    }
  }

  @Nested
  @DisplayName("Environment Variable Method Tests")
  class EnvironmentVariableMethodTests {

    @Test
    @DisplayName("should have setEnvironmentVariable method")
    void shouldHaveSetEnvironmentVariableMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("setEnvironmentVariable", String.class, String.class),
          "WasiLinker should have setEnvironmentVariable method");
    }

    @Test
    @DisplayName("should have setEnvironmentVariables method")
    void shouldHaveSetEnvironmentVariablesMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("setEnvironmentVariables", Map.class),
          "WasiLinker should have setEnvironmentVariables method");
    }

    @Test
    @DisplayName("should have inheritEnvironment method")
    void shouldHaveInheritEnvironmentMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("inheritEnvironment"),
          "WasiLinker should have inheritEnvironment method");
    }

    @Test
    @DisplayName("should have inheritEnvironmentVariables method")
    void shouldHaveInheritEnvironmentVariablesMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("inheritEnvironmentVariables", List.class),
          "WasiLinker should have inheritEnvironmentVariables method");
    }
  }

  @Nested
  @DisplayName("Arguments Method Tests")
  class ArgumentsMethodTests {

    @Test
    @DisplayName("should have setArguments method")
    void shouldHaveSetArgumentsMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("setArguments", List.class),
          "WasiLinker should have setArguments method");
    }

    @Test
    @DisplayName("setArguments should return void")
    void setArgumentsShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType = WasiLinker.class.getMethod("setArguments", List.class).getReturnType();
      assertEquals(void.class, returnType, "setArguments should return void");
    }
  }

  @Nested
  @DisplayName("Stdio Configuration Method Tests")
  class StdioConfigurationMethodTests {

    @Test
    @DisplayName("should have configureStdin method")
    void shouldHaveConfigureStdinMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("configureStdin", WasiStdioConfig.class),
          "WasiLinker should have configureStdin method");
    }

    @Test
    @DisplayName("should have configureStdout method")
    void shouldHaveConfigureStdoutMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("configureStdout", WasiStdioConfig.class),
          "WasiLinker should have configureStdout method");
    }

    @Test
    @DisplayName("should have configureStderr method")
    void shouldHaveConfigureStderrMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("configureStderr", WasiStdioConfig.class),
          "WasiLinker should have configureStderr method");
    }
  }

  @Nested
  @DisplayName("Network Access Method Tests")
  class NetworkAccessMethodTests {

    @Test
    @DisplayName("should have enableNetworkAccess method")
    void shouldHaveEnableNetworkAccessMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("enableNetworkAccess"),
          "WasiLinker should have enableNetworkAccess method");
    }

    @Test
    @DisplayName("should have disableNetworkAccess method")
    void shouldHaveDisableNetworkAccessMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("disableNetworkAccess"),
          "WasiLinker should have disableNetworkAccess method");
    }

    @Test
    @DisplayName("disableNetworkAccess should return void")
    void disableNetworkAccessShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType = WasiLinker.class.getMethod("disableNetworkAccess").getReturnType();
      assertEquals(void.class, returnType, "disableNetworkAccess should return void");
    }
  }

  @Nested
  @DisplayName("Resource Limit Method Tests")
  class ResourceLimitMethodTests {

    @Test
    @DisplayName("should have setMaxFileSize method")
    void shouldHaveSetMaxFileSizeMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("setMaxFileSize", Long.class),
          "WasiLinker should have setMaxFileSize method");
    }

    @Test
    @DisplayName("should have setMaxOpenFiles method")
    void shouldHaveSetMaxOpenFilesMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("setMaxOpenFiles", Integer.class),
          "WasiLinker should have setMaxOpenFiles method");
    }
  }

  @Nested
  @DisplayName("Instantiation Method Tests")
  class InstantiationMethodTests {

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("instantiate", Store.class, Module.class),
          "WasiLinker should have instantiate method");
    }

    @Test
    @DisplayName("instantiate should return Instance")
    void instantiateShouldReturnInstance() throws NoSuchMethodException {
      Class<?> returnType =
          WasiLinker.class.getMethod("instantiate", Store.class, Module.class).getReturnType();
      assertEquals(Instance.class, returnType, "instantiate should return Instance");
    }

    @Test
    @DisplayName("instantiate should declare WasmException")
    void instantiateShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          WasiLinker.class.getMethod("instantiate", Store.class, Module.class).getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "instantiate should declare at least one exception");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getLinker method")
    void shouldHaveGetLinkerMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("getLinker"), "WasiLinker should have getLinker method");
    }

    @Test
    @DisplayName("getLinker should return Linker")
    void getLinkerShouldReturnLinker() throws NoSuchMethodException {
      Class<?> returnType = WasiLinker.class.getMethod("getLinker").getReturnType();
      assertEquals(Linker.class, returnType, "getLinker should return Linker");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("getEngine"), "WasiLinker should have getEngine method");
    }

    @Test
    @DisplayName("getEngine should return Engine")
    void getEngineShouldReturnEngine() throws NoSuchMethodException {
      Class<?> returnType = WasiLinker.class.getMethod("getEngine").getReturnType();
      assertEquals(Engine.class, returnType, "getEngine should return Engine");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiLinker.class.getMethod("getConfig"), "WasiLinker should have getConfig method");
    }

    @Test
    @DisplayName("getConfig should return WasiConfig")
    void getConfigShouldReturnWasiConfig() throws NoSuchMethodException {
      Class<?> returnType = WasiLinker.class.getMethod("getConfig").getReturnType();
      assertEquals(WasiConfig.class, returnType, "getConfig should return WasiConfig");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      assertNotNull(WasiLinker.class.getMethod("isValid"), "WasiLinker should have isValid method");
    }

    @Test
    @DisplayName("isValid should return boolean")
    void isValidShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType = WasiLinker.class.getMethod("isValid").getReturnType();
      assertEquals(boolean.class, returnType, "isValid should return boolean");
    }

    @Test
    @DisplayName("should have close method from Closeable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      assertNotNull(WasiLinker.class.getMethod("close"), "WasiLinker should have close method");
    }

    @Test
    @DisplayName("close should return void")
    void closeShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType = WasiLinker.class.getMethod("close").getReturnType();
      assertEquals(void.class, returnType, "close should return void");
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
    @DisplayName("Module should exist")
    void moduleShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.Module");
        assertNotNull(clazz, "Module class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("Module class should exist", e);
      }
    }

    @Test
    @DisplayName("Instance should exist")
    void instanceShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.Instance");
        assertNotNull(clazz, "Instance class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("Instance class should exist", e);
      }
    }

    @Test
    @DisplayName("Linker should exist")
    void linkerShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.Linker");
        assertNotNull(clazz, "Linker class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("Linker class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiStdioConfig should exist")
    void wasiStdioConfigShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig");
        assertNotNull(clazz, "WasiStdioConfig class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiStdioConfig class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiPermissions should exist")
    void wasiPermissionsShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiPermissions");
        assertNotNull(clazz, "WasiPermissions class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiPermissions class should exist", e);
      }
    }
  }

  @Nested
  @DisplayName("Interface Method Count Tests")
  class InterfaceMethodCountTests {

    @Test
    @DisplayName("interface should have expected method count")
    void interfaceShouldHaveExpectedMethodCount() {
      // Count non-static methods
      int instanceMethodCount = 0;
      for (java.lang.reflect.Method method : WasiLinker.class.getDeclaredMethods()) {
        if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
          instanceMethodCount++;
        }
      }
      // allowDirectoryAccess (2 overloads), setEnvironmentVariable, setEnvironmentVariables,
      // inheritEnvironment, inheritEnvironmentVariables, setArguments,
      // configureStdin, configureStdout, configureStderr,
      // enableNetworkAccess, disableNetworkAccess, setMaxFileSize, setMaxOpenFiles,
      // instantiate, getLinker, getEngine, getConfig, isValid, close = 20 methods
      assertEquals(20, instanceMethodCount, "WasiLinker should have 20 instance methods");
    }
  }
}
