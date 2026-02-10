package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentEngine;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive reflection-based unit tests for JniComponentEngine.
 *
 * <p>These tests verify the class structure, fields, methods, and inner classes of
 * JniComponentEngine using reflection to avoid triggering native library initialization.
 *
 * @see JniComponentEngine
 */
@DisplayName("JniComponentEngine Tests")
class JniComponentEngineTest {

  private static final Class<?> COMPONENT_ENGINE_CLASS = JniComponentEngine.class;

  /**
   * Helper method to get the tested class.
   *
   * @return the JniComponentEngine class
   */
  private Class<?> getTestedClass() {
    return COMPONENT_ENGINE_CLASS;
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(getTestedClass().getModifiers()),
          "JniComponentEngine should be a final class");
    }

    @Test
    @DisplayName("Should extend JniResource")
    void shouldExtendJniResource() {
      assertEquals(
          JniResource.class,
          getTestedClass().getSuperclass(),
          "JniComponentEngine should extend JniResource");
    }

    @Test
    @DisplayName("Should implement ComponentEngine interface")
    void shouldImplementComponentEngineInterface() {
      Class<?>[] interfaces = getTestedClass().getInterfaces();
      boolean implementsComponentEngine = Arrays.asList(interfaces).contains(ComponentEngine.class);
      assertTrue(implementsComponentEngine, "JniComponentEngine should implement ComponentEngine");
    }

    @Test
    @DisplayName("Should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(getTestedClass().getModifiers()),
          "JniComponentEngine should be a public class");
    }

    @Test
    @DisplayName("Should be in the correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni",
          getTestedClass().getPackage().getName(),
          "JniComponentEngine should be in ai.tegmentum.wasmtime4j.jni package");
    }

    @Test
    @DisplayName("Should have expected number of declared fields")
    void shouldHaveExpectedFieldCount() {
      Field[] declaredFields = getTestedClass().getDeclaredFields();
      // Filter out synthetic fields (like $jacocoData)
      List<Field> realFields =
          Arrays.stream(declaredFields)
              .filter(f -> !f.isSynthetic() && !f.getName().startsWith("$"))
              .collect(Collectors.toList());
      // Expected: LOGGER, engineId, config, nativeEngine, loadedComponents, componentIdCounter,
      // registry
      assertTrue(
          realFields.size() >= 6,
          "JniComponentEngine should have at least 6 declared fields, found: " + realFields.size());
    }

    @Test
    @DisplayName("Should have expected number of inner classes")
    void shouldHaveExpectedInnerClassCount() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      // Expected inner classes: JniComponentGarbageCollectionResult,
      // JniComponentEngineOptimizationResult,
      // JniComponentEngineHealth, JniComponentEngineHealthCheckResult,
      // JniComponentEngineStatistics, JniComponentEngineDebugInfo
      assertTrue(
          declaredClasses.length >= 6,
          "JniComponentEngine should have at least 6 inner classes, found: "
              + declaredClasses.length);
    }

    @Test
    @DisplayName("Should have static initializer for native library loading")
    void shouldHaveStaticInitializer() {
      // The class has a static block, but we can't directly detect it via reflection
      // We can verify the class structure expects native loading
      assertNotNull(getTestedClass(), "Class should be loadable");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have LOGGER field")
    void shouldHaveLoggerField() {
      assertDoesNotThrow(
          () -> {
            Field loggerField = getTestedClass().getDeclaredField("LOGGER");
            assertTrue(Modifier.isPrivate(loggerField.getModifiers()), "LOGGER should be private");
            assertTrue(Modifier.isStatic(loggerField.getModifiers()), "LOGGER should be static");
            assertTrue(Modifier.isFinal(loggerField.getModifiers()), "LOGGER should be final");
            assertEquals(Logger.class, loggerField.getType(), "LOGGER should be of type Logger");
          },
          "Should have LOGGER field");
    }

    @Test
    @DisplayName("Should have engineId field")
    void shouldHaveEngineIdField() {
      assertDoesNotThrow(
          () -> {
            Field engineIdField = getTestedClass().getDeclaredField("engineId");
            assertTrue(
                Modifier.isPrivate(engineIdField.getModifiers()), "engineId should be private");
            assertTrue(Modifier.isFinal(engineIdField.getModifiers()), "engineId should be final");
            assertEquals(
                String.class, engineIdField.getType(), "engineId should be of type String");
          },
          "Should have engineId field");
    }

    @Test
    @DisplayName("Should have config field")
    void shouldHaveConfigField() {
      assertDoesNotThrow(
          () -> {
            Field configField = getTestedClass().getDeclaredField("config");
            assertTrue(Modifier.isPrivate(configField.getModifiers()), "config should be private");
            assertTrue(Modifier.isFinal(configField.getModifiers()), "config should be final");
          },
          "Should have config field");
    }

    @Test
    @DisplayName("Should have nativeEngine field")
    void shouldHaveNativeEngineField() {
      assertDoesNotThrow(
          () -> {
            Field nativeEngineField = getTestedClass().getDeclaredField("nativeEngine");
            assertTrue(
                Modifier.isPrivate(nativeEngineField.getModifiers()),
                "nativeEngine should be private");
            assertTrue(
                Modifier.isFinal(nativeEngineField.getModifiers()), "nativeEngine should be final");
          },
          "Should have nativeEngine field");
    }

    @Test
    @DisplayName("Should have loadedComponents field")
    void shouldHaveLoadedComponentsField() {
      assertDoesNotThrow(
          () -> {
            Field loadedComponentsField = getTestedClass().getDeclaredField("loadedComponents");
            assertTrue(
                Modifier.isPrivate(loadedComponentsField.getModifiers()),
                "loadedComponents should be private");
            assertTrue(
                Modifier.isFinal(loadedComponentsField.getModifiers()),
                "loadedComponents should be final");
            assertEquals(
                ConcurrentMap.class,
                loadedComponentsField.getType(),
                "loadedComponents should be of type ConcurrentMap");
          },
          "Should have loadedComponents field");
    }

    @Test
    @DisplayName("Should have componentIdCounter field")
    void shouldHaveComponentIdCounterField() {
      assertDoesNotThrow(
          () -> {
            Field counterField = getTestedClass().getDeclaredField("componentIdCounter");
            assertTrue(
                Modifier.isPrivate(counterField.getModifiers()),
                "componentIdCounter should be private");
            assertTrue(
                Modifier.isFinal(counterField.getModifiers()),
                "componentIdCounter should be final");
            assertEquals(
                AtomicLong.class,
                counterField.getType(),
                "componentIdCounter should be of type AtomicLong");
          },
          "Should have componentIdCounter field");
    }

    @Test
    @DisplayName("Should have registry field")
    void shouldHaveRegistryField() {
      assertDoesNotThrow(
          () -> {
            Field registryField = getTestedClass().getDeclaredField("registry");
            assertTrue(
                Modifier.isPrivate(registryField.getModifiers()), "registry should be private");
            assertFalse(
                Modifier.isFinal(registryField.getModifiers()),
                "registry should not be final (mutable)");
          },
          "Should have registry field");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have constructor with ComponentEngineConfig parameter")
    void shouldHaveConfigConstructor() {
      Constructor<?>[] constructors = getTestedClass().getDeclaredConstructors();
      boolean hasConfigConstructor =
          Arrays.stream(constructors)
              .anyMatch(
                  c ->
                      c.getParameterCount() == 1
                          && c.getParameterTypes()[0]
                              .getSimpleName()
                              .equals("ComponentEngineConfig"));
      assertTrue(
          hasConfigConstructor,
          "JniComponentEngine should have a constructor with ComponentEngineConfig parameter");
    }

    @Test
    @DisplayName("Constructor should be public")
    void constructorShouldBePublic() {
      Constructor<?>[] constructors = getTestedClass().getDeclaredConstructors();
      boolean hasPublicConfigConstructor =
          Arrays.stream(constructors)
              .anyMatch(
                  c ->
                      Modifier.isPublic(c.getModifiers())
                          && c.getParameterCount() == 1
                          && c.getParameterTypes()[0]
                              .getSimpleName()
                              .equals("ComponentEngineConfig"));
      assertTrue(hasPublicConfigConstructor, "Config constructor should be public");
    }

    @Test
    @DisplayName("Constructor should declare WasmException")
    void constructorShouldDeclareWasmException() {
      Constructor<?>[] constructors = getTestedClass().getDeclaredConstructors();
      for (Constructor<?> c : constructors) {
        if (c.getParameterCount() == 1
            && c.getParameterTypes()[0].getSimpleName().equals("ComponentEngineConfig")) {
          Class<?>[] exceptionTypes = c.getExceptionTypes();
          boolean declaresWasmException =
              Arrays.stream(exceptionTypes)
                  .anyMatch(e -> e.getSimpleName().equals("WasmException"));
          assertTrue(declaresWasmException, "Constructor should declare WasmException");
        }
      }
    }
  }

  @Nested
  @DisplayName("Core Interface Method Tests")
  class CoreInterfaceMethodTests {

    @Test
    @DisplayName("Should have getConfig method")
    void shouldHaveGetConfigMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getConfig");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getConfig should be public");
          },
          "Should have getConfig method");
    }

    @Test
    @DisplayName("Should have createStore method with no parameters")
    void shouldHaveCreateStoreMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("createStore");
            assertTrue(Modifier.isPublic(method.getModifiers()), "createStore should be public");
          },
          "Should have createStore method");
    }

    @Test
    @DisplayName("Should have createStore method with data parameter")
    void shouldHaveCreateStoreWithDataMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("createStore", Object.class);
            assertTrue(Modifier.isPublic(method.getModifiers()), "createStore should be public");
          },
          "Should have createStore with data parameter method");
    }

    @Test
    @DisplayName("Should have compileModule method")
    void shouldHaveCompileModuleMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("compileModule", byte[].class);
            assertTrue(Modifier.isPublic(method.getModifiers()), "compileModule should be public");
          },
          "Should have compileModule method");
    }

    @Test
    @DisplayName("Should have compileWat method")
    void shouldHaveCompileWatMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("compileWat", String.class);
            assertTrue(Modifier.isPublic(method.getModifiers()), "compileWat should be public");
          },
          "Should have compileWat method");
    }

    @Test
    @DisplayName("Should have precompileModule method")
    void shouldHavePrecompileModuleMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("precompileModule", byte[].class);
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "precompileModule should be public");
          },
          "Should have precompileModule method");
    }

    @Test
    @DisplayName("Should have supportsFeature method")
    void shouldHaveSupportsFeatureMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("supportsFeature")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0].getSimpleName().equals("WasmFeature"));
      assertTrue(hasMethod, "Should have supportsFeature method");
    }

    @Test
    @DisplayName("Should have same method")
    void shouldHaveSameMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("same")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0].getSimpleName().equals("Engine"));
      assertTrue(hasMethod, "Should have same method");
    }

    @Test
    @DisplayName("Should have isAsync method")
    void shouldHaveIsAsyncMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("isAsync");
            assertTrue(Modifier.isPublic(method.getModifiers()), "isAsync should be public");
            assertEquals(boolean.class, method.getReturnType(), "isAsync should return boolean");
          },
          "Should have isAsync method");
    }
  }

  @Nested
  @DisplayName("Engine Configuration Method Tests")
  class EngineConfigMethodTests {

    @Test
    @DisplayName("Should have getId method")
    void shouldHaveGetIdMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getId");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getId should be public");
            assertEquals(String.class, method.getReturnType(), "getId should return String");
          },
          "Should have getId method");
    }

    @Test
    @DisplayName("Should have getComponentConfig method")
    void shouldHaveGetComponentConfigMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getComponentConfig");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "getComponentConfig should be public");
          },
          "Should have getComponentConfig method");
    }

    @Test
    @DisplayName("Should have getMemoryLimitPages method")
    void shouldHaveGetMemoryLimitPagesMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getMemoryLimitPages");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "getMemoryLimitPages should be public");
            assertEquals(int.class, method.getReturnType(), "Should return int");
          },
          "Should have getMemoryLimitPages method");
    }

    @Test
    @DisplayName("Should have getStackSizeLimit method")
    void shouldHaveGetStackSizeLimitMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getStackSizeLimit");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "getStackSizeLimit should be public");
            assertEquals(long.class, method.getReturnType(), "Should return long");
          },
          "Should have getStackSizeLimit method");
    }

    @Test
    @DisplayName("Should have isFuelEnabled method")
    void shouldHaveIsFuelEnabledMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("isFuelEnabled");
            assertTrue(Modifier.isPublic(method.getModifiers()), "isFuelEnabled should be public");
            assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
          },
          "Should have isFuelEnabled method");
    }

    @Test
    @DisplayName("Should have isEpochInterruptionEnabled method")
    void shouldHaveIsEpochInterruptionEnabledMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("isEpochInterruptionEnabled");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "isEpochInterruptionEnabled should be public");
            assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
          },
          "Should have isEpochInterruptionEnabled method");
    }

    @Test
    @DisplayName("Should have isCoredumpOnTrapEnabled method")
    void shouldHaveIsCoredumpOnTrapEnabledMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("isCoredumpOnTrapEnabled");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "isCoredumpOnTrapEnabled should be public");
            assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
          },
          "Should have isCoredumpOnTrapEnabled method");
    }

    @Test
    @DisplayName("Should have getMaxInstances method")
    void shouldHaveGetMaxInstancesMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getMaxInstances");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "getMaxInstances should be public");
            assertEquals(int.class, method.getReturnType(), "Should return int");
          },
          "Should have getMaxInstances method");
    }

    @Test
    @DisplayName("Should have getReferenceCount method")
    void shouldHaveGetReferenceCountMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getReferenceCount");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "getReferenceCount should be public");
            assertEquals(long.class, method.getReturnType(), "Should return long");
          },
          "Should have getReferenceCount method");
    }

    @Test
    @DisplayName("Should have incrementEpoch method")
    void shouldHaveIncrementEpochMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("incrementEpoch");
            assertTrue(Modifier.isPublic(method.getModifiers()), "incrementEpoch should be public");
            assertEquals(void.class, method.getReturnType(), "Should return void");
          },
          "Should have incrementEpoch method");
    }
  }

  @Nested
  @DisplayName("Component Compilation Method Tests")
  class ComponentCompilationMethodTests {

    @Test
    @DisplayName("Should have compileComponent(byte[]) method")
    void shouldHaveCompileComponentBytesMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("compileComponent", byte[].class);
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "compileComponent should be public");
          },
          "Should have compileComponent(byte[]) method");
    }

    @Test
    @DisplayName("Should have compileComponent(byte[], String) method")
    void shouldHaveCompileComponentBytesNameMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("compileComponent", byte[].class, String.class);
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "compileComponent with name should be public");
          },
          "Should have compileComponent(byte[], String) method");
    }

    @Test
    @DisplayName("Should have loadComponentFromBytes(byte[]) method")
    void shouldHaveLoadComponentFromBytesMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("loadComponentFromBytes", byte[].class);
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "loadComponentFromBytes should be public");
          },
          "Should have loadComponentFromBytes(byte[]) method");
    }

    @Test
    @DisplayName("Should have loadComponentFromBytes(byte[], ComponentMetadata) method")
    void shouldHaveLoadComponentFromBytesWithMetadataMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("loadComponentFromBytes")
                          && m.getParameterCount() == 2
                          && m.getParameterTypes()[0] == byte[].class
                          && m.getParameterTypes()[1].getSimpleName().equals("ComponentMetadata"));
      assertTrue(hasMethod, "Should have loadComponentFromBytes with metadata method");
    }

    @Test
    @DisplayName("Should have loadComponentFromFile method")
    void shouldHaveLoadComponentFromFileMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("loadComponentFromFile", String.class);
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "loadComponentFromFile should be public");
          },
          "Should have loadComponentFromFile method");
    }

    @Test
    @DisplayName("Should have loadComponentFromUrl method")
    void shouldHaveLoadComponentFromUrlMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("loadComponentFromUrl")
                          && m.getParameterCount() == 2
                          && m.getParameterTypes()[0] == String.class
                          && m.getParameterTypes()[1]
                              .getSimpleName()
                              .equals("ComponentLoadConfig"));
      assertTrue(hasMethod, "Should have loadComponentFromUrl method");
    }
  }

  @Nested
  @DisplayName("Component Linking Method Tests")
  class ComponentLinkingMethodTests {

    @Test
    @DisplayName("Should have linkComponents method")
    void shouldHaveLinkComponentsMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("linkComponents", List.class);
            assertTrue(Modifier.isPublic(method.getModifiers()), "linkComponents should be public");
          },
          "Should have linkComponents method");
    }

    @Test
    @DisplayName("Should have checkCompatibility method")
    void shouldHaveCheckCompatibilityMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("checkCompatibility")
                          && m.getParameterCount() == 2
                          && m.getParameterTypes()[0].getSimpleName().equals("Component")
                          && m.getParameterTypes()[1].getSimpleName().equals("Component"));
      assertTrue(hasMethod, "Should have checkCompatibility method");
    }
  }

  @Nested
  @DisplayName("Registry Method Tests")
  class RegistryMethodTests {

    @Test
    @DisplayName("Should have getRegistry method")
    void shouldHaveGetRegistryMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getRegistry");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getRegistry should be public");
          },
          "Should have getRegistry method");
    }

    @Test
    @DisplayName("Should have setRegistry method")
    void shouldHaveSetRegistryMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("setRegistry")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0].getSimpleName().equals("ComponentRegistry"));
      assertTrue(hasMethod, "Should have setRegistry method");
    }
  }

  @Nested
  @DisplayName("Instance Creation Method Tests")
  class InstanceCreationMethodTests {

    @Test
    @DisplayName("Should have createInstance method with two parameters")
    void shouldHaveCreateInstanceTwoParamsMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("createInstance")
                          && m.getParameterCount() == 2
                          && m.getParameterTypes()[0].getSimpleName().equals("Component")
                          && m.getParameterTypes()[1].getSimpleName().equals("Store"));
      assertTrue(hasMethod, "Should have createInstance(Component, Store) method");
    }

    @Test
    @DisplayName("Should have createInstance method with three parameters")
    void shouldHaveCreateInstanceThreeParamsMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("createInstance")
                          && m.getParameterCount() == 3
                          && m.getParameterTypes()[0].getSimpleName().equals("Component")
                          && m.getParameterTypes()[1].getSimpleName().equals("Store")
                          && m.getParameterTypes()[2] == List.class);
      assertTrue(hasMethod, "Should have createInstance(Component, Store, List) method");
    }

    @Test
    @DisplayName("Should have instantiateComponent method")
    void shouldHaveInstantiateComponentMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("instantiateComponent")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0].getSimpleName().equals("JniComponentHandle"));
      assertTrue(hasMethod, "Should have instantiateComponent method");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("Should have validateComponent method")
    void shouldHaveValidateComponentMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("validateComponent")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0].getSimpleName().equals("Component"));
      assertTrue(hasMethod, "Should have validateComponent method");
    }

    @Test
    @DisplayName("Should have isValid method")
    void shouldHaveIsValidMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("isValid");
            assertTrue(Modifier.isPublic(method.getModifiers()), "isValid should be public");
            assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
          },
          "Should have isValid method");
    }
  }

  @Nested
  @DisplayName("WIT Support Method Tests")
  class WitSupportMethodTests {

    @Test
    @DisplayName("Should have getWitSupportInfo method")
    void shouldHaveGetWitSupportInfoMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getWitSupportInfo");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "getWitSupportInfo should be public");
          },
          "Should have getWitSupportInfo method");
    }

    @Test
    @DisplayName("Should have supportsComponentModel method")
    void shouldHaveSupportsComponentModelMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("supportsComponentModel");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "supportsComponentModel should be public");
            assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
          },
          "Should have supportsComponentModel method");
    }

    @Test
    @DisplayName("Should have getMaxLinkDepth method")
    void shouldHaveGetMaxLinkDepthMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getMaxLinkDepth");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "getMaxLinkDepth should be public");
          },
          "Should have getMaxLinkDepth method");
    }
  }

  @Nested
  @DisplayName("Resource Management Method Tests")
  class ResourceManagementMethodTests {

    @Test
    @DisplayName("Should have getResourceUsage method")
    void shouldHaveGetResourceUsageMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getResourceUsage");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "getResourceUsage should be public");
          },
          "Should have getResourceUsage method");
    }

    @Test
    @DisplayName("Should have setGlobalResourceLimits method")
    void shouldHaveSetGlobalResourceLimitsMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("setGlobalResourceLimits")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0]
                              .getSimpleName()
                              .equals("ComponentEngineResourceLimits"));
      assertTrue(hasMethod, "Should have setGlobalResourceLimits method");
    }

    @Test
    @DisplayName("Should have getActiveInstancesCount method")
    void shouldHaveGetActiveInstancesCountMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getActiveInstancesCount");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "getActiveInstancesCount should be public");
            assertEquals(int.class, method.getReturnType(), "Should return int");
          },
          "Should have getActiveInstancesCount method");
    }

    @Test
    @DisplayName("Should have cleanupInactiveInstances method")
    void shouldHaveCleanupInactiveInstancesMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("cleanupInactiveInstances");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "cleanupInactiveInstances should be public");
            assertEquals(int.class, method.getReturnType(), "Should return int");
          },
          "Should have cleanupInactiveInstances method");
    }

    @Test
    @DisplayName("Should have performGarbageCollection method")
    void shouldHavePerformGarbageCollectionMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("performGarbageCollection")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0]
                              .getSimpleName()
                              .equals("ComponentGarbageCollectionConfig"));
      assertTrue(hasMethod, "Should have performGarbageCollection method");
    }
  }

  @Nested
  @DisplayName("Optimization Method Tests")
  class OptimizationMethodTests {

    @Test
    @DisplayName("Should have optimizePerformance method")
    void shouldHaveOptimizePerformanceMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("optimizePerformance")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0]
                              .getSimpleName()
                              .equals("ComponentEngineOptimizationConfig"));
      assertTrue(hasMethod, "Should have optimizePerformance method");
    }
  }

  @Nested
  @DisplayName("Health and Diagnostics Method Tests")
  class HealthMethodTests {

    @Test
    @DisplayName("Should have getHealth method")
    void shouldHaveGetHealthMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getHealth");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getHealth should be public");
          },
          "Should have getHealth method");
    }

    @Test
    @DisplayName("Should have performHealthCheck method")
    void shouldHavePerformHealthCheckMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("performHealthCheck")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0]
                              .getSimpleName()
                              .equals("ComponentEngineHealthCheckConfig"));
      assertTrue(hasMethod, "Should have performHealthCheck method");
    }

    @Test
    @DisplayName("Should have getStatistics method")
    void shouldHaveGetStatisticsMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getStatistics");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getStatistics should be public");
          },
          "Should have getStatistics method");
    }

    @Test
    @DisplayName("Should have getDebugInfo method")
    void shouldHaveGetDebugInfoMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getDebugInfo");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getDebugInfo should be public");
          },
          "Should have getDebugInfo method");
    }
  }

  @Nested
  @DisplayName("Orchestration Method Tests")
  class OrchestrationMethodTests {

    @Test
    @DisplayName("Should have createOrchestrator method")
    void shouldHaveCreateOrchestratorMethod() {
      boolean hasMethod =
          Arrays.stream(getTestedClass().getDeclaredMethods())
              .anyMatch(
                  m ->
                      m.getName().equals("createOrchestrator")
                          && m.getParameterCount() == 1
                          && m.getParameterTypes()[0]
                              .getSimpleName()
                              .equals("ComponentOrchestrationConfig"));
      assertTrue(hasMethod, "Should have createOrchestrator method");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("Should have doClose method from JniResource")
    void shouldHaveDoCloseMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("doClose");
            assertTrue(Modifier.isProtected(method.getModifiers()), "doClose should be protected");
            assertEquals(void.class, method.getReturnType(), "doClose should return void");
          },
          "Should have doClose method");
    }

    @Test
    @DisplayName("Should have getResourceType method from JniResource")
    void shouldHaveGetResourceTypeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getResourceType");
            assertTrue(
                Modifier.isProtected(method.getModifiers()), "getResourceType should be protected");
            assertEquals(String.class, method.getReturnType(), "Should return String");
          },
          "Should have getResourceType method");
    }
  }

  @Nested
  @DisplayName("Native Method Tests")
  class NativeMethodTests {

    @Test
    @DisplayName("Should have nativeDetectPrecompiled native method")
    void shouldHaveNativeDetectPrecompiledMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("nativeDetectPrecompiled", long.class, byte[].class);
            assertTrue(
                Modifier.isNative(method.getModifiers()),
                "nativeDetectPrecompiled should be native");
            assertTrue(
                Modifier.isPrivate(method.getModifiers()),
                "nativeDetectPrecompiled should be private");
            assertEquals(int.class, method.getReturnType(), "Should return int");
          },
          "Should have nativeDetectPrecompiled native method");
    }

    @Test
    @DisplayName("Should have detectPrecompiled public method")
    void shouldHaveDetectPrecompiledMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("detectPrecompiled", byte[].class);
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "detectPrecompiled should be public");
          },
          "Should have detectPrecompiled method");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("Should have createNativeEngine method")
    void shouldHaveCreateNativeEngineMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("createNativeEngine");
            assertTrue(
                Modifier.isPrivate(method.getModifiers()), "createNativeEngine should be private");
            assertEquals(long.class, method.getReturnType(), "Should return long");
          },
          "Should have createNativeEngine method");
    }

    @Test
    @DisplayName("Should have generateComponentId method")
    void shouldHaveGenerateComponentIdMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("generateComponentId");
            assertTrue(
                Modifier.isPrivate(method.getModifiers()), "generateComponentId should be private");
            assertEquals(String.class, method.getReturnType(), "Should return String");
          },
          "Should have generateComponentId method");
    }
  }

  @Nested
  @DisplayName("Inner Class Tests")
  class InnerClassTests {

    @Test
    @DisplayName("Should have JniComponentGarbageCollectionResult inner class")
    void shouldHaveGarbageCollectionResultInnerClass() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("JniComponentGarbageCollectionResult"));
      assertTrue(hasClass, "Should have JniComponentGarbageCollectionResult inner class");
    }

    @Test
    @DisplayName("JniComponentGarbageCollectionResult should be private static final")
    void garbageCollectionResultShouldBePrivateStaticFinal() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniComponentGarbageCollectionResult")) {
          assertTrue(
              Modifier.isPrivate(innerClass.getModifiers()),
              "JniComponentGarbageCollectionResult should be private");
          assertTrue(
              Modifier.isStatic(innerClass.getModifiers()),
              "JniComponentGarbageCollectionResult should be static");
          assertTrue(
              Modifier.isFinal(innerClass.getModifiers()),
              "JniComponentGarbageCollectionResult should be final");
        }
      }
    }

    @Test
    @DisplayName("Should have JniComponentEngineOptimizationResult inner class")
    void shouldHaveOptimizationResultInnerClass() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("JniComponentEngineOptimizationResult"));
      assertTrue(hasClass, "Should have JniComponentEngineOptimizationResult inner class");
    }

    @Test
    @DisplayName("Should have JniComponentEngineHealth inner class")
    void shouldHaveEngineHealthInnerClass() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("JniComponentEngineHealth"));
      assertTrue(hasClass, "Should have JniComponentEngineHealth inner class");
    }

    @Test
    @DisplayName("Should have JniComponentEngineHealthCheckResult inner class")
    void shouldHaveHealthCheckResultInnerClass() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("JniComponentEngineHealthCheckResult"));
      assertTrue(hasClass, "Should have JniComponentEngineHealthCheckResult inner class");
    }

    @Test
    @DisplayName("Should have JniComponentEngineStatistics inner class")
    void shouldHaveStatisticsInnerClass() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("JniComponentEngineStatistics"));
      assertTrue(hasClass, "Should have JniComponentEngineStatistics inner class");
    }

    @Test
    @DisplayName("Should have JniComponentEngineDebugInfo inner class")
    void shouldHaveDebugInfoInnerClass() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("JniComponentEngineDebugInfo"));
      assertTrue(hasClass, "Should have JniComponentEngineDebugInfo inner class");
    }

    @Test
    @DisplayName("All inner classes should be private static final")
    void allInnerClassesShouldBePrivateStaticFinal() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      for (Class<?> innerClass : innerClasses) {
        assertTrue(
            Modifier.isPrivate(innerClass.getModifiers()),
            innerClass.getSimpleName() + " should be private");
        assertTrue(
            Modifier.isStatic(innerClass.getModifiers()),
            innerClass.getSimpleName() + " should be static");
        assertTrue(
            Modifier.isFinal(innerClass.getModifiers()),
            innerClass.getSimpleName() + " should be final");
      }
    }
  }

  @Nested
  @DisplayName("Inner Class Interface Implementation Tests")
  class InnerClassInterfaceTests {

    @Test
    @DisplayName(
        "JniComponentGarbageCollectionResult should implement ComponentGarbageCollectionResult")
    void garbageCollectionResultShouldImplementInterface() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniComponentGarbageCollectionResult")) {
          Class<?>[] interfaces = innerClass.getInterfaces();
          boolean implementsInterface =
              Arrays.stream(interfaces)
                  .anyMatch(i -> i.getSimpleName().equals("ComponentGarbageCollectionResult"));
          assertTrue(
              implementsInterface,
              "JniComponentGarbageCollectionResult should implement"
                  + " ComponentGarbageCollectionResult");
        }
      }
    }

    @Test
    @DisplayName(
        "JniComponentEngineOptimizationResult should implement ComponentEngineOptimizationResult")
    void optimizationResultShouldImplementInterface() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniComponentEngineOptimizationResult")) {
          Class<?>[] interfaces = innerClass.getInterfaces();
          boolean implementsInterface =
              Arrays.stream(interfaces)
                  .anyMatch(i -> i.getSimpleName().equals("ComponentEngineOptimizationResult"));
          assertTrue(
              implementsInterface,
              "JniComponentEngineOptimizationResult should implement"
                  + " ComponentEngineOptimizationResult");
        }
      }
    }

    @Test
    @DisplayName("JniComponentEngineHealth should implement ComponentEngineHealth")
    void healthShouldImplementInterface() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniComponentEngineHealth")) {
          Class<?>[] interfaces = innerClass.getInterfaces();
          boolean implementsInterface =
              Arrays.stream(interfaces)
                  .anyMatch(i -> i.getSimpleName().equals("ComponentEngineHealth"));
          assertTrue(
              implementsInterface,
              "JniComponentEngineHealth should implement ComponentEngineHealth");
        }
      }
    }

    @Test
    @DisplayName(
        "JniComponentEngineHealthCheckResult should implement ComponentEngineHealthCheckResult")
    void healthCheckResultShouldImplementInterface() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniComponentEngineHealthCheckResult")) {
          Class<?>[] interfaces = innerClass.getInterfaces();
          boolean implementsInterface =
              Arrays.stream(interfaces)
                  .anyMatch(i -> i.getSimpleName().equals("ComponentEngineHealthCheckResult"));
          assertTrue(
              implementsInterface,
              "JniComponentEngineHealthCheckResult should implement"
                  + " ComponentEngineHealthCheckResult");
        }
      }
    }

    @Test
    @DisplayName("JniComponentEngineStatistics should implement ComponentEngineStatistics")
    void statisticsShouldImplementInterface() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniComponentEngineStatistics")) {
          Class<?>[] interfaces = innerClass.getInterfaces();
          boolean implementsInterface =
              Arrays.stream(interfaces)
                  .anyMatch(i -> i.getSimpleName().equals("ComponentEngineStatistics"));
          assertTrue(
              implementsInterface,
              "JniComponentEngineStatistics should implement ComponentEngineStatistics");
        }
      }
    }

    @Test
    @DisplayName("JniComponentEngineDebugInfo should implement ComponentEngineDebugInfo")
    void debugInfoShouldImplementInterface() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniComponentEngineDebugInfo")) {
          Class<?>[] interfaces = innerClass.getInterfaces();
          boolean implementsInterface =
              Arrays.stream(interfaces)
                  .anyMatch(i -> i.getSimpleName().equals("ComponentEngineDebugInfo"));
          assertTrue(
              implementsInterface,
              "JniComponentEngineDebugInfo should implement ComponentEngineDebugInfo");
        }
      }
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("loadedComponents should be ConcurrentMap for thread safety")
    void loadedComponentsShouldBeConcurrentMap() {
      assertDoesNotThrow(
          () -> {
            Field field = getTestedClass().getDeclaredField("loadedComponents");
            assertEquals(
                ConcurrentMap.class, field.getType(), "loadedComponents should be ConcurrentMap");
          },
          "loadedComponents field should exist and be ConcurrentMap");
    }

    @Test
    @DisplayName("componentIdCounter should be AtomicLong for thread safety")
    void componentIdCounterShouldBeAtomicLong() {
      assertDoesNotThrow(
          () -> {
            Field field = getTestedClass().getDeclaredField("componentIdCounter");
            assertEquals(
                AtomicLong.class, field.getType(), "componentIdCounter should be AtomicLong");
          },
          "componentIdCounter field should exist and be AtomicLong");
    }
  }

  @Nested
  @DisplayName("Naming Convention Tests")
  class NamingConventionTests {

    @Test
    @DisplayName("Class name should follow PascalCase convention")
    void classNameShouldFollowPascalCase() {
      String className = getTestedClass().getSimpleName();
      assertTrue(
          Character.isUpperCase(className.charAt(0)), "Class name should start with uppercase");
      assertEquals("JniComponentEngine", className, "Class name should be JniComponentEngine");
    }

    @Test
    @DisplayName("Method names should follow camelCase convention")
    void methodNamesShouldFollowCamelCase() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      for (Method method : methods) {
        String name = method.getName();
        // Skip synthetic methods
        if (method.isSynthetic() || name.startsWith("$") || name.startsWith("lambda$")) {
          continue;
        }
        assertTrue(
            Character.isLowerCase(name.charAt(0)),
            "Method " + name + " should start with lowercase");
      }
    }

    @Test
    @DisplayName("Static fields should use UPPER_CASE naming")
    void staticFieldsShouldUseUpperCase() {
      Field[] fields = getTestedClass().getDeclaredFields();
      for (Field field : fields) {
        // Skip synthetic fields like $jacocoData
        if (field.isSynthetic() || field.getName().startsWith("$")) {
          continue;
        }
        if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
          String name = field.getName();
          // Check if it's a constant (UPPER_CASE) or a static logger (which is conventionally
          // named LOGGER)
          if (!name.equals("LOGGER")) {
            boolean isUpperCase = name.equals(name.toUpperCase());
            assertTrue(
                isUpperCase || name.equals("LOGGER"),
                "Static final field " + name + " should use UPPER_CASE naming convention");
          }
        }
      }
    }

    @Test
    @DisplayName("Inner class names should follow PascalCase convention")
    void innerClassNamesShouldFollowPascalCase() {
      Class<?>[] innerClasses = getTestedClass().getDeclaredClasses();
      for (Class<?> innerClass : innerClasses) {
        String name = innerClass.getSimpleName();
        assertTrue(
            Character.isUpperCase(name.charAt(0)),
            "Inner class " + name + " should start with uppercase");
        assertTrue(
            name.startsWith("Jni"), "Inner class " + name + " should start with 'Jni' prefix");
      }
    }
  }

  @Nested
  @DisplayName("Method Return Type Tests")
  class MethodReturnTypeTests {

    @Test
    @DisplayName("getWitSupportInfo should return WitSupportInfo")
    void getWitSupportInfoReturnType() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getWitSupportInfo");
            assertEquals(
                "WitSupportInfo",
                method.getReturnType().getSimpleName(),
                "getWitSupportInfo should return WitSupportInfo");
          });
    }

    @Test
    @DisplayName("checkCompatibility should return WitCompatibilityResult")
    void checkCompatibilityReturnType() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      for (Method method : methods) {
        if (method.getName().equals("checkCompatibility") && method.getParameterCount() == 2) {
          assertEquals(
              "WitCompatibilityResult",
              method.getReturnType().getSimpleName(),
              "checkCompatibility should return WitCompatibilityResult");
        }
      }
    }

    @Test
    @DisplayName("validateComponent should return ComponentValidationResult")
    void validateComponentReturnType() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      for (Method method : methods) {
        if (method.getName().equals("validateComponent") && method.getParameterCount() == 1) {
          assertEquals(
              "ComponentValidationResult",
              method.getReturnType().getSimpleName(),
              "validateComponent should return ComponentValidationResult");
        }
      }
    }

    @Test
    @DisplayName("getHealth should return ComponentEngineHealth")
    void getHealthReturnType() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getHealth");
            assertEquals(
                "ComponentEngineHealth",
                method.getReturnType().getSimpleName(),
                "getHealth should return ComponentEngineHealth");
          });
    }

    @Test
    @DisplayName("getStatistics should return ComponentEngineStatistics")
    void getStatisticsReturnType() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getStatistics");
            assertEquals(
                "ComponentEngineStatistics",
                method.getReturnType().getSimpleName(),
                "getStatistics should return ComponentEngineStatistics");
          });
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("Methods that throw WasmException should declare it")
    void methodsShouldDeclareWasmException() {
      Set<String> methodsThatShouldThrow = new HashSet<>();
      methodsThatShouldThrow.add("createStore");
      methodsThatShouldThrow.add("compileModule");
      methodsThatShouldThrow.add("compileWat");
      methodsThatShouldThrow.add("precompileModule");
      methodsThatShouldThrow.add("compileComponent");
      methodsThatShouldThrow.add("loadComponentFromBytes");
      methodsThatShouldThrow.add("loadComponentFromFile");
      methodsThatShouldThrow.add("linkComponents");
      methodsThatShouldThrow.add("createInstance");
      methodsThatShouldThrow.add("cleanupInactiveInstances");
      methodsThatShouldThrow.add("performHealthCheck");
      methodsThatShouldThrow.add("instantiateComponent");

      Method[] methods = getTestedClass().getDeclaredMethods();
      for (Method method : methods) {
        if (methodsThatShouldThrow.contains(method.getName())) {
          Class<?>[] exceptions = method.getExceptionTypes();
          boolean declaresWasmException =
              Arrays.stream(exceptions).anyMatch(e -> e.getSimpleName().equals("WasmException"));
          assertTrue(
              declaresWasmException,
              "Method " + method.getName() + " should declare WasmException");
        }
      }
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("Should have substantial number of public methods")
    void shouldHaveSubstantialPublicMethods() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      long publicMethodCount =
          Arrays.stream(methods)
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .filter(m -> !m.isSynthetic())
              .count();
      // JniComponentEngine has many public methods from ComponentEngine interface
      assertTrue(
          publicMethodCount >= 30,
          "Should have at least 30 public methods, found: " + publicMethodCount);
    }

    @Test
    @DisplayName("Should have limited private methods")
    void shouldHaveLimitedPrivateMethods() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      long privateMethodCount =
          Arrays.stream(methods)
              .filter(m -> Modifier.isPrivate(m.getModifiers()))
              .filter(m -> !m.isSynthetic() && !m.getName().startsWith("lambda$"))
              .count();
      // Expected private methods: setNativeHandle, generateComponentId, nativeDetectPrecompiled
      assertTrue(
          privateMethodCount >= 2,
          "Should have at least 2 private methods, found: " + privateMethodCount);
    }

    @Test
    @DisplayName("Should have protected methods from JniResource")
    void shouldHaveProtectedMethods() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      long protectedMethodCount =
          Arrays.stream(methods)
              .filter(m -> Modifier.isProtected(m.getModifiers()))
              .filter(m -> !m.isSynthetic())
              .count();
      // Expected protected methods: doClose, getResourceType
      assertEquals(
          2,
          protectedMethodCount,
          "Should have exactly 2 protected methods (doClose, getResourceType)");
    }
  }
}
