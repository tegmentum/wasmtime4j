package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Caller} interface.
 *
 * <p>This test class verifies the structure and contract of the Caller interface, which provides
 * access to the calling WebAssembly instance context within host functions.
 */
@DisplayName("Caller Interface Tests")
class CallerTest {

  @Nested
  @DisplayName("Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("Caller should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Caller.class.isInterface(), "Caller should be an interface");
    }

    @Test
    @DisplayName("Caller should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Caller.class.getModifiers()), "Caller should be a public interface");
    }

    @Test
    @DisplayName("Caller should be generic with type parameter T")
    void shouldBeGenericWithTypeParameterT() {
      final TypeVariable<?>[] typeParameters = Caller.class.getTypeParameters();
      assertEquals(1, typeParameters.length, "Caller should have exactly one type parameter");
      assertEquals("T", typeParameters[0].getName(), "Type parameter should be named T");
    }
  }

  @Nested
  @DisplayName("Data Access Method Tests")
  class DataAccessMethodTests {

    @Test
    @DisplayName("Should have data() method")
    void shouldHaveDataMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("data");
      assertNotNull(method, "data() method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (generic T)");
    }
  }

  @Nested
  @DisplayName("Export Access Method Tests")
  class ExportAccessMethodTests {

    @Test
    @DisplayName("Should have getExport(String) method")
    void shouldHaveGetExportMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("getExport", String.class);
      assertNotNull(method, "getExport(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getFunction(String) method")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("getFunction", String.class);
      assertNotNull(method, "getFunction(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getMemory(String) method")
    void shouldHaveGetMemoryByNameMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("getMemory", String.class);
      assertNotNull(method, "getMemory(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getMemory() default method")
    void shouldHaveGetMemoryDefaultMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("getMemory");
      assertNotNull(method, "getMemory() method should exist");
      assertTrue(method.isDefault(), "getMemory() should be a default method");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getTable(String) method")
    void shouldHaveGetTableMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("getTable", String.class);
      assertNotNull(method, "getTable(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getGlobal(String) method")
    void shouldHaveGetGlobalMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("getGlobal", String.class);
      assertNotNull(method, "getGlobal(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have hasExport(String) method")
    void shouldHaveHasExportMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("hasExport", String.class);
      assertNotNull(method, "hasExport(String) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have getExportByModuleExport(ModuleExport) method")
    void shouldHaveGetExportByModuleExportMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("getExportByModuleExport", ModuleExport.class);
      assertNotNull(method, "getExportByModuleExport(ModuleExport) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Fuel Method Tests")
  class FuelMethodTests {

    @Test
    @DisplayName("Should have fuelConsumed() method")
    void shouldHaveFuelConsumedMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("fuelConsumed");
      assertNotNull(method, "fuelConsumed() method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have fuelRemaining() method")
    void shouldHaveFuelRemainingMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("fuelRemaining");
      assertNotNull(method, "fuelRemaining() method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have addFuel(long) method")
    void shouldHaveAddFuelMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("addFuel", long.class);
      assertNotNull(method, "addFuel(long) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have fuelAsyncYieldInterval() method")
    void shouldHaveFuelAsyncYieldIntervalMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("fuelAsyncYieldInterval");
      assertNotNull(method, "fuelAsyncYieldInterval() method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have setFuelAsyncYieldInterval(long) method")
    void shouldHaveSetFuelAsyncYieldIntervalMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("setFuelAsyncYieldInterval", long.class);
      assertNotNull(method, "setFuelAsyncYieldInterval(long) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Epoch Method Tests")
  class EpochMethodTests {

    @Test
    @DisplayName("Should have hasEpochDeadline() method")
    void shouldHaveHasEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("hasEpochDeadline");
      assertNotNull(method, "hasEpochDeadline() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have epochDeadline() method")
    void shouldHaveEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("epochDeadline");
      assertNotNull(method, "epochDeadline() method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have setEpochDeadline(long) method")
    void shouldHaveSetEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("setEpochDeadline", long.class);
      assertNotNull(method, "setEpochDeadline(long) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Engine Method Tests")
  class EngineMethodTests {

    @Test
    @DisplayName("Should have engine() method")
    void shouldHaveEngineMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("engine");
      assertNotNull(method, "engine() method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }
  }

  @Nested
  @DisplayName("GC Method Tests")
  class GcMethodTests {

    @Test
    @DisplayName("Should have gc() method")
    void shouldHaveGcMethod() throws NoSuchMethodException {
      final Method method = Caller.class.getMethod("gc");
      assertNotNull(method, "gc() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
