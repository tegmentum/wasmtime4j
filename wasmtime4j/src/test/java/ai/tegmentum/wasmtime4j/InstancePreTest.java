package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link InstancePre} interface.
 *
 * <p>This test class verifies the structure and contract of the InstancePre interface, which
 * represents a pre-instantiated WebAssembly module optimized for fast instantiation.
 */
@DisplayName("InstancePre Interface Tests")
class InstancePreTest {

  @Nested
  @DisplayName("Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("InstancePre should be an interface")
    void shouldBeAnInterface() {
      assertTrue(InstancePre.class.isInterface(), "InstancePre should be an interface");
    }

    @Test
    @DisplayName("InstancePre should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(InstancePre.class),
          "InstancePre should extend Closeable");
    }

    @Test
    @DisplayName("InstancePre should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(InstancePre.class.getModifiers()),
          "InstancePre should be a public interface");
    }
  }

  @Nested
  @DisplayName("Instantiation Method Tests")
  class InstantiationMethodTests {

    @Test
    @DisplayName("Should have instantiate(Store) method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("instantiate", Store.class);
      assertNotNull(method, "instantiate(Store) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("Should have instantiate(Store, ImportMap) method")
    void shouldHaveInstantiateWithImportsMethod() throws NoSuchMethodException {
      final Method method =
          InstancePre.class.getMethod("instantiate", Store.class, ImportMap.class);
      assertNotNull(method, "instantiate(Store, ImportMap) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }
  }

  @Nested
  @DisplayName("Async Instantiation Method Tests")
  class AsyncInstantiationMethodTests {

    @Test
    @DisplayName("Should have instantiateAsync(Store) method")
    void shouldHaveInstantiateAsyncMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("instantiateAsync", Store.class);
      assertNotNull(method, "instantiateAsync(Store) method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("Should have instantiateAsync(Store, ImportMap) method")
    void shouldHaveInstantiateAsyncWithImportsMethod() throws NoSuchMethodException {
      final Method method =
          InstancePre.class.getMethod("instantiateAsync", Store.class, ImportMap.class);
      assertNotNull(method, "instantiateAsync(Store, ImportMap) method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("Should have getModule() method")
    void shouldHaveGetModuleMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("getModule");
      assertNotNull(method, "getModule() method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("Should have getEngine() method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("getEngine");
      assertNotNull(method, "getEngine() method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }
  }

  @Nested
  @DisplayName("Validity Method Tests")
  class ValidityMethodTests {

    @Test
    @DisplayName("Should have isValid() method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("isValid");
      assertNotNull(method, "isValid() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("Should have getInstanceCount() method")
    void shouldHaveGetInstanceCountMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("getInstanceCount");
      assertNotNull(method, "getInstanceCount() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have getStatistics() method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics() method should exist");
      assertEquals(
          PreInstantiationStatistics.class,
          method.getReturnType(),
          "Should return PreInstantiationStatistics");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("Should have close() method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("close");
      assertNotNull(method, "close() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
