/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link NativeFunctionBindings}.
 *
 * <p>These tests use reflection to verify the class structure, field types, method signatures, and
 * inner classes without requiring native library initialization. This approach ensures tests remain
 * fast and don't rely on platform-specific native code.
 *
 * <p>The NativeFunctionBindings class provides type-safe wrappers for Wasmtime native functions
 * using the Panama Foreign Function API.
 */
@DisplayName("NativeFunctionBindings Tests")
class NativeFunctionBindingsTest {

  private static Class<?> getTestedClass() {
    return NativeFunctionBindings.class;
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(getTestedClass().getModifiers()),
          "NativeFunctionBindings should be a final class");
    }

    @Test
    @DisplayName("Should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(getTestedClass().getModifiers()),
          "NativeFunctionBindings should be a public class");
    }

    @Test
    @DisplayName("Should not extend any class other than Object")
    void shouldNotExtendAnyClassOtherThanObject() {
      assertEquals(
          Object.class,
          getTestedClass().getSuperclass(),
          "NativeFunctionBindings should extend Object");
    }

    @Test
    @DisplayName("Should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          getTestedClass().getInterfaces().length,
          "NativeFunctionBindings should not implement any interfaces");
    }

    @Test
    @DisplayName("Should be in panama package")
    void shouldBeInPanamaPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          getTestedClass().getPackageName(),
          "NativeFunctionBindings should be in panama package");
    }
  }

  @Nested
  @DisplayName("Singleton Pattern Tests")
  class SingletonPatternTests {

    @Test
    @DisplayName("Should have private constructor")
    void shouldHavePrivateConstructor() {
      Constructor<?>[] constructors = getTestedClass().getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly one constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()),
          "Constructor should be private for singleton pattern");
    }

    @Test
    @DisplayName("Should have getInstance static method")
    void shouldHaveGetInstanceStaticMethod() {
      assertDoesNotThrow(
          () -> {
            Method getInstance = getTestedClass().getDeclaredMethod("getInstance");
            assertNotNull(getInstance, "getInstance method should exist");
            assertTrue(
                Modifier.isStatic(getInstance.getModifiers()), "getInstance should be static");
            assertTrue(
                Modifier.isPublic(getInstance.getModifiers()), "getInstance should be public");
            assertEquals(
                getTestedClass(),
                getInstance.getReturnType(),
                "getInstance should return NativeFunctionBindings");
          });
    }

    @Test
    @DisplayName("Should have static instance field")
    void shouldHaveStaticInstanceField() {
      assertDoesNotThrow(
          () -> {
            Field instanceField = getTestedClass().getDeclaredField("instance");
            assertNotNull(instanceField, "instance field should exist");
            assertTrue(
                Modifier.isStatic(instanceField.getModifiers()), "instance field should be static");
            assertTrue(
                Modifier.isPrivate(instanceField.getModifiers()),
                "instance field should be private");
            assertTrue(
                Modifier.isVolatile(instanceField.getModifiers()),
                "instance field should be volatile for double-checked locking");
          });
    }

    @Test
    @DisplayName("Should have INSTANCE_LOCK field for double-checked locking")
    void shouldHaveInstanceLockField() {
      assertDoesNotThrow(
          () -> {
            Field lockField = getTestedClass().getDeclaredField("INSTANCE_LOCK");
            assertNotNull(lockField, "INSTANCE_LOCK field should exist");
            assertTrue(
                Modifier.isStatic(lockField.getModifiers()),
                "INSTANCE_LOCK field should be static");
            assertTrue(
                Modifier.isFinal(lockField.getModifiers()), "INSTANCE_LOCK field should be final");
            assertTrue(
                Modifier.isPrivate(lockField.getModifiers()),
                "INSTANCE_LOCK field should be private");
          });
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
            assertNotNull(loggerField, "LOGGER field should exist");
            assertTrue(
                Modifier.isStatic(loggerField.getModifiers()), "LOGGER field should be static");
            assertTrue(
                Modifier.isFinal(loggerField.getModifiers()), "LOGGER field should be final");
            assertTrue(
                Modifier.isPrivate(loggerField.getModifiers()), "LOGGER field should be private");
            assertEquals(
                Logger.class, loggerField.getType(), "LOGGER field should be of type Logger");
          });
    }

    @Test
    @DisplayName("Should have libraryLoader field")
    void shouldHaveLibraryLoaderField() {
      assertDoesNotThrow(
          () -> {
            Field field = getTestedClass().getDeclaredField("libraryLoader");
            assertNotNull(field, "libraryLoader field should exist");
            assertFalse(
                Modifier.isStatic(field.getModifiers()),
                "libraryLoader field should not be static");
            assertTrue(
                Modifier.isPrivate(field.getModifiers()), "libraryLoader field should be private");
            assertTrue(
                Modifier.isFinal(field.getModifiers()), "libraryLoader field should be final");
            assertEquals(
                NativeLibraryLoader.class,
                field.getType(),
                "libraryLoader field should be of type NativeLibraryLoader");
          });
    }

    @Test
    @DisplayName("Should have functionBindings field")
    void shouldHaveFunctionBindingsField() {
      assertDoesNotThrow(
          () -> {
            Field field = getTestedClass().getDeclaredField("functionBindings");
            assertNotNull(field, "functionBindings field should exist");
            assertFalse(
                Modifier.isStatic(field.getModifiers()),
                "functionBindings field should not be static");
            assertTrue(
                Modifier.isPrivate(field.getModifiers()),
                "functionBindings field should be private");
            assertTrue(
                Modifier.isFinal(field.getModifiers()), "functionBindings field should be final");
            assertEquals(
                ConcurrentHashMap.class,
                field.getType(),
                "functionBindings field should be of type ConcurrentHashMap");
          });
    }

    @Test
    @DisplayName("Should have initialized field")
    void shouldHaveInitializedField() {
      assertDoesNotThrow(
          () -> {
            Field field = getTestedClass().getDeclaredField("initialized");
            assertNotNull(field, "initialized field should exist");
            assertFalse(
                Modifier.isStatic(field.getModifiers()), "initialized field should not be static");
            assertTrue(
                Modifier.isPrivate(field.getModifiers()), "initialized field should be private");
            assertTrue(
                Modifier.isVolatile(field.getModifiers()), "initialized field should be volatile");
            assertEquals(
                boolean.class, field.getType(), "initialized field should be of type boolean");
          });
    }
  }

  @Nested
  @DisplayName("Engine Methods Tests")
  class EngineMethodsTests {

    @Test
    @DisplayName("Should have engineCreate method")
    void shouldHaveEngineCreateMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("engineCreate");
            assertNotNull(method, "engineCreate method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "engineCreate should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "engineCreate should return MemorySegment");
          });
    }

    @Test
    @DisplayName("Should have engineDestroy method")
    void shouldHaveEngineDestroyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("engineDestroy", MemorySegment.class);
            assertNotNull(method, "engineDestroy method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "engineDestroy should be public");
            assertEquals(void.class, method.getReturnType(), "engineDestroy should return void");
          });
    }

    @Test
    @DisplayName("Should have engineConfigure method")
    void shouldHaveEngineConfigureMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "engineConfigure",
                        MemorySegment.class,
                        MemorySegment.class,
                        MemorySegment.class);
            assertNotNull(method, "engineConfigure method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "engineConfigure should be public");
            assertEquals(int.class, method.getReturnType(), "engineConfigure should return int");
          });
    }

    @Test
    @DisplayName("Should have engineSetOptimizationLevel method")
    void shouldHaveEngineSetOptimizationLevelMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "engineSetOptimizationLevel", MemorySegment.class, int.class);
            assertNotNull(method, "engineSetOptimizationLevel method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "engineSetOptimizationLevel should be public");
            assertEquals(
                int.class, method.getReturnType(), "engineSetOptimizationLevel should return int");
          });
    }

    @Test
    @DisplayName("Should have engineGetOptimizationLevel method")
    void shouldHaveEngineGetOptimizationLevelMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("engineGetOptimizationLevel", MemorySegment.class);
            assertNotNull(method, "engineGetOptimizationLevel method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "engineGetOptimizationLevel should be public");
            assertEquals(
                int.class, method.getReturnType(), "engineGetOptimizationLevel should return int");
          });
    }

    @Test
    @DisplayName("Should have engineSetDebugInfo method")
    void shouldHaveEngineSetDebugInfoMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("engineSetDebugInfo", MemorySegment.class, boolean.class);
            assertNotNull(method, "engineSetDebugInfo method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "engineSetDebugInfo should be public");
            assertEquals(int.class, method.getReturnType(), "engineSetDebugInfo should return int");
          });
    }

    @Test
    @DisplayName("Should have engineIsDebugInfo method")
    void shouldHaveEngineIsDebugInfoMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("engineIsDebugInfo", MemorySegment.class);
            assertNotNull(method, "engineIsDebugInfo method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "engineIsDebugInfo should be public");
            assertEquals(
                boolean.class, method.getReturnType(), "engineIsDebugInfo should return boolean");
          });
    }
  }

  @Nested
  @DisplayName("Module Methods Tests")
  class ModuleMethodsTests {

    @Test
    @DisplayName("Should have moduleCompile method")
    void shouldHaveModuleCompileMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "moduleCompile",
                        MemorySegment.class,
                        MemorySegment.class,
                        long.class,
                        MemorySegment.class);
            assertNotNull(method, "moduleCompile method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "moduleCompile should be public");
            assertEquals(int.class, method.getReturnType(), "moduleCompile should return int");
          });
    }

    @Test
    @DisplayName("Should have moduleCompileWat method")
    void shouldHaveModuleCompileWatMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "moduleCompileWat",
                        MemorySegment.class,
                        MemorySegment.class,
                        MemorySegment.class);
            assertNotNull(method, "moduleCompileWat method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "moduleCompileWat should be public");
            assertEquals(int.class, method.getReturnType(), "moduleCompileWat should return int");
          });
    }

    @Test
    @DisplayName("Should have moduleSerialize method")
    void shouldHaveModuleSerializeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "moduleSerialize",
                        MemorySegment.class,
                        MemorySegment.class,
                        MemorySegment.class);
            assertNotNull(method, "moduleSerialize method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "moduleSerialize should be public");
            assertEquals(int.class, method.getReturnType(), "moduleSerialize should return int");
          });
    }

    @Test
    @DisplayName("Should have moduleDeserialize method")
    void shouldHaveModuleDeserializeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "moduleDeserialize",
                        MemorySegment.class,
                        MemorySegment.class,
                        long.class,
                        MemorySegment.class);
            assertNotNull(method, "moduleDeserialize method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "moduleDeserialize should be public");
            assertEquals(int.class, method.getReturnType(), "moduleDeserialize should return int");
          });
    }

    @Test
    @DisplayName("Should have moduleDestroy method")
    void shouldHaveModuleDestroyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("moduleDestroy", MemorySegment.class);
            assertNotNull(method, "moduleDestroy method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "moduleDestroy should be public");
            assertEquals(void.class, method.getReturnType(), "moduleDestroy should return void");
          });
    }

    @Test
    @DisplayName("Should have moduleCreate method")
    void shouldHaveModuleCreateMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "moduleCreate", MemorySegment.class, MemorySegment.class, long.class);
            assertNotNull(method, "moduleCreate method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "moduleCreate should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "moduleCreate should return MemorySegment");
          });
    }

    @Test
    @DisplayName("Should have moduleCreateWat method")
    void shouldHaveModuleCreateWatMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("moduleCreateWat", MemorySegment.class, MemorySegment.class);
            assertNotNull(method, "moduleCreateWat method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "moduleCreateWat should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "moduleCreateWat should return MemorySegment");
          });
    }

    @Test
    @DisplayName("Should have moduleImportsLen method")
    void shouldHaveModuleImportsLenMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("moduleImportsLen", MemorySegment.class);
            assertNotNull(method, "moduleImportsLen method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "moduleImportsLen should be public");
            assertEquals(long.class, method.getReturnType(), "moduleImportsLen should return long");
          });
    }

    @Test
    @DisplayName("Should have moduleExportsLen method")
    void shouldHaveModuleExportsLenMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("moduleExportsLen", MemorySegment.class);
            assertNotNull(method, "moduleExportsLen method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "moduleExportsLen should be public");
            assertEquals(long.class, method.getReturnType(), "moduleExportsLen should return long");
          });
    }
  }

  @Nested
  @DisplayName("Store Methods Tests")
  class StoreMethodsTests {

    @Test
    @DisplayName("Should have storeCreate method")
    void shouldHaveStoreCreateMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("storeCreate", MemorySegment.class);
            assertNotNull(method, "storeCreate method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "storeCreate should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "storeCreate should return MemorySegment");
          });
    }

    @Test
    @DisplayName("Should have storeDestroy method")
    void shouldHaveStoreDestroyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("storeDestroy", MemorySegment.class);
            assertNotNull(method, "storeDestroy method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "storeDestroy should be public");
            assertEquals(void.class, method.getReturnType(), "storeDestroy should return void");
          });
    }

    @Test
    @DisplayName("Should have storeSetFuel method")
    void shouldHaveStoreSetFuelMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("storeSetFuel", MemorySegment.class, long.class);
            assertNotNull(method, "storeSetFuel method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "storeSetFuel should be public");
            assertEquals(int.class, method.getReturnType(), "storeSetFuel should return int");
          });
    }

    @Test
    @DisplayName("Should have storeGetFuel method")
    void shouldHaveStoreGetFuelMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("storeGetFuel", MemorySegment.class, MemorySegment.class);
            assertNotNull(method, "storeGetFuel method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "storeGetFuel should be public");
            assertEquals(int.class, method.getReturnType(), "storeGetFuel should return int");
          });
    }

    @Test
    @DisplayName("Should have storeAddFuel method")
    void shouldHaveStoreAddFuelMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("storeAddFuel", MemorySegment.class, long.class);
            assertNotNull(method, "storeAddFuel method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "storeAddFuel should be public");
            assertEquals(int.class, method.getReturnType(), "storeAddFuel should return int");
          });
    }

    @Test
    @DisplayName("Should have storeGc method")
    void shouldHaveStoreGcMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("storeGc", MemorySegment.class);
            assertNotNull(method, "storeGc method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "storeGc should be public");
            assertEquals(int.class, method.getReturnType(), "storeGc should return int");
          });
    }
  }

  @Nested
  @DisplayName("Instance Methods Tests")
  class InstanceMethodsTests {

    @Test
    @DisplayName("Should have instanceCreate method")
    void shouldHaveInstanceCreateMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("instanceCreate", MemorySegment.class, MemorySegment.class);
            assertNotNull(method, "instanceCreate method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "instanceCreate should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "instanceCreate should return MemorySegment");
          });
    }

    @Test
    @DisplayName("Should have instanceDestroy method")
    void shouldHaveInstanceDestroyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("instanceDestroy", MemorySegment.class);
            assertNotNull(method, "instanceDestroy method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "instanceDestroy should be public");
            assertEquals(void.class, method.getReturnType(), "instanceDestroy should return void");
          });
    }

    @Test
    @DisplayName("Should have instanceExportsLen method")
    void shouldHaveInstanceExportsLenMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("instanceExportsLen", MemorySegment.class);
            assertNotNull(method, "instanceExportsLen method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "instanceExportsLen should be public");
            assertEquals(
                long.class, method.getReturnType(), "instanceExportsLen should return long");
          });
    }

    @Test
    @DisplayName("Should have instanceGetMemoryByName method")
    void shouldHaveInstanceGetMemoryByNameMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "instanceGetMemoryByName",
                        MemorySegment.class,
                        MemorySegment.class,
                        MemorySegment.class);
            assertNotNull(method, "instanceGetMemoryByName method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "instanceGetMemoryByName should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "instanceGetMemoryByName should return MemorySegment");
          });
    }
  }

  @Nested
  @DisplayName("Memory Atomic Methods Tests")
  class MemoryAtomicMethodsTests {

    @Test
    @DisplayName("Should have memoryAtomicCompareAndSwapI32 method")
    void shouldHaveMemoryAtomicCompareAndSwapI32Method() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "memoryAtomicCompareAndSwapI32",
                        MemorySegment.class,
                        MemorySegment.class,
                        long.class,
                        int.class,
                        int.class,
                        MemorySegment.class);
            assertNotNull(method, "memoryAtomicCompareAndSwapI32 method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "memoryAtomicCompareAndSwapI32 should be public");
            assertEquals(
                int.class,
                method.getReturnType(),
                "memoryAtomicCompareAndSwapI32 should return int");
          });
    }

    @Test
    @DisplayName("Should have memoryAtomicFence method")
    void shouldHaveMemoryAtomicFenceMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "memoryAtomicFence", MemorySegment.class, MemorySegment.class);
            assertNotNull(method, "memoryAtomicFence method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "memoryAtomicFence should be public");
            assertEquals(int.class, method.getReturnType(), "memoryAtomicFence should return int");
          });
    }
  }

  @Nested
  @DisplayName("WASI Context Methods Tests")
  class WasiContextMethodsTests {

    @Test
    @DisplayName("Should have wasiContextCreate method")
    void shouldHaveWasiContextCreateMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("wasiContextCreate");
            assertNotNull(method, "wasiContextCreate method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "wasiContextCreate should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "wasiContextCreate should return MemorySegment");
          });
    }
  }

  @Nested
  @DisplayName("Trap Methods Tests")
  class TrapMethodsTests {

    @Test
    @DisplayName("Should have trapParseCode method")
    void shouldHaveTrapParseCodeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("trapParseCode", String.class);
            assertNotNull(method, "trapParseCode method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "trapParseCode should be public");
            assertEquals(int.class, method.getReturnType(), "trapParseCode should return int");
          });
    }

    @Test
    @DisplayName("Should have trapCodeName method")
    void shouldHaveTrapCodeNameMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("trapCodeName", int.class);
            assertNotNull(method, "trapCodeName method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "trapCodeName should be public");
            assertEquals(String.class, method.getReturnType(), "trapCodeName should return String");
          });
    }

    @Test
    @DisplayName("Should have trapIsTrap method")
    void shouldHaveTrapIsTrapMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("trapIsTrap", String.class);
            assertNotNull(method, "trapIsTrap method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "trapIsTrap should be public");
            assertEquals(boolean.class, method.getReturnType(), "trapIsTrap should return boolean");
          });
    }

    @Test
    @DisplayName("Should have trapExtractFunctionName method")
    void shouldHaveTrapExtractFunctionNameMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("trapExtractFunctionName", String.class);
            assertNotNull(method, "trapExtractFunctionName method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "trapExtractFunctionName should be public");
            assertEquals(
                String.class,
                method.getReturnType(),
                "trapExtractFunctionName should return String");
          });
    }

    @Test
    @DisplayName("Should have trapExtractOffset method")
    void shouldHaveTrapExtractOffsetMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("trapExtractOffset", String.class);
            assertNotNull(method, "trapExtractOffset method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "trapExtractOffset should be public");
            assertEquals(
                long.class, method.getReturnType(), "trapExtractOffset should return long");
          });
    }
  }

  @Nested
  @DisplayName("Async Runtime Methods Tests")
  class AsyncRuntimeMethodsTests {

    @Test
    @DisplayName("Should have asyncRuntimeInit method")
    void shouldHaveAsyncRuntimeInitMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("asyncRuntimeInit");
            assertNotNull(method, "asyncRuntimeInit method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "asyncRuntimeInit should be public");
            assertEquals(int.class, method.getReturnType(), "asyncRuntimeInit should return int");
          });
    }

    @Test
    @DisplayName("Should have asyncRuntimeInfo method")
    void shouldHaveAsyncRuntimeInfoMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("asyncRuntimeInfo");
            assertNotNull(method, "asyncRuntimeInfo method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "asyncRuntimeInfo should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "asyncRuntimeInfo should return MemorySegment");
          });
    }

    @Test
    @DisplayName("Should have asyncRuntimeShutdown method")
    void shouldHaveAsyncRuntimeShutdownMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("asyncRuntimeShutdown");
            assertNotNull(method, "asyncRuntimeShutdown method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "asyncRuntimeShutdown should be public");
            assertEquals(
                int.class, method.getReturnType(), "asyncRuntimeShutdown should return int");
          });
    }
  }

  @Nested
  @DisplayName("Method Handle Methods Tests")
  class MethodHandleMethodsTests {

    @Test
    @DisplayName("Should have getFunction method")
    void shouldHaveGetFunctionMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("getFunction", String.class, FunctionDescriptor.class);
            assertNotNull(method, "getFunction method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getFunction should be public");
            assertEquals(
                MethodHandle.class,
                method.getReturnType(),
                "getFunction should return MethodHandle");
          });
    }

    @Test
    @DisplayName("Should have getTableSize method")
    void shouldHaveGetTableSizeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getTableSize");
            assertNotNull(method, "getTableSize method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getTableSize should be public");
            assertEquals(
                MethodHandle.class,
                method.getReturnType(),
                "getTableSize should return MethodHandle");
          });
    }

    @Test
    @DisplayName("Should have getTableGet method")
    void shouldHaveGetTableGetMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getTableGet");
            assertNotNull(method, "getTableGet method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getTableGet should be public");
            assertEquals(
                MethodHandle.class,
                method.getReturnType(),
                "getTableGet should return MethodHandle");
          });
    }

    @Test
    @DisplayName("Should have getTableSet method")
    void shouldHaveGetTableSetMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getTableSet");
            assertNotNull(method, "getTableSet method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getTableSet should be public");
            assertEquals(
                MethodHandle.class,
                method.getReturnType(),
                "getTableSet should return MethodHandle");
          });
    }

    @Test
    @DisplayName("Should have getTableGrow method")
    void shouldHaveGetTableGrowMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getTableGrow");
            assertNotNull(method, "getTableGrow method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getTableGrow should be public");
            assertEquals(
                MethodHandle.class,
                method.getReturnType(),
                "getTableGrow should return MethodHandle");
          });
    }

    @Test
    @DisplayName("Should have getTableDelete method")
    void shouldHaveGetTableDeleteMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("getTableDelete");
            assertNotNull(method, "getTableDelete method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getTableDelete should be public");
            assertEquals(
                MethodHandle.class,
                method.getReturnType(),
                "getTableDelete should return MethodHandle");
          });
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("Should have validatePointer private method")
    void shouldHaveValidatePointerPrivateMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("validatePointer", MemorySegment.class, String.class);
            assertNotNull(method, "validatePointer method should exist");
            assertTrue(
                Modifier.isPrivate(method.getModifiers()), "validatePointer should be private");
            assertEquals(void.class, method.getReturnType(), "validatePointer should return void");
          });
    }

    @Test
    @DisplayName("Should have validateSize private method")
    void shouldHaveValidateSizePrivateMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("validateSize", long.class, String.class);
            assertNotNull(method, "validateSize method should exist");
            assertTrue(Modifier.isPrivate(method.getModifiers()), "validateSize should be private");
            assertEquals(void.class, method.getReturnType(), "validateSize should return void");
          });
    }

    @Test
    @DisplayName("Should have initializeFunctionBindings private method")
    void shouldHaveInitializeFunctionBindingsPrivateMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("initializeFunctionBindings");
            assertNotNull(method, "initializeFunctionBindings method should exist");
            assertTrue(
                Modifier.isPrivate(method.getModifiers()),
                "initializeFunctionBindings should be private");
          });
    }
  }

  @Nested
  @DisplayName("Inner Class Tests")
  class InnerClassTests {

    @Test
    @DisplayName("Should have exactly 3 inner classes")
    void shouldHaveExactlyThreeInnerClasses() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      assertEquals(3, declaredClasses.length, "Should have exactly 3 inner classes");
    }

    @Test
    @DisplayName("Should have FunctionBinding inner class")
    void shouldHaveFunctionBindingInnerClass() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      boolean hasFunctionBinding =
          Arrays.stream(declaredClasses).anyMatch(c -> c.getSimpleName().equals("FunctionBinding"));
      assertTrue(hasFunctionBinding, "Should have FunctionBinding inner class");
    }

    @Test
    @DisplayName("Should have TrapCodes inner class")
    void shouldHaveTrapCodesInnerClass() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      boolean hasTrapCodes =
          Arrays.stream(declaredClasses).anyMatch(c -> c.getSimpleName().equals("TrapCodes"));
      assertTrue(hasTrapCodes, "Should have TrapCodes inner class");
    }

    @Test
    @DisplayName("Should have TrapInfo inner class")
    void shouldHaveTrapInfoInnerClass() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      boolean hasTrapInfo =
          Arrays.stream(declaredClasses).anyMatch(c -> c.getSimpleName().equals("TrapInfo"));
      assertTrue(hasTrapInfo, "Should have TrapInfo inner class");
    }
  }

  @Nested
  @DisplayName("TrapCodes Inner Class Tests")
  class TrapCodesInnerClassTests {

    private Class<?> getTrapCodesClass() {
      return NativeFunctionBindings.TrapCodes.class;
    }

    @Test
    @DisplayName("TrapCodes should be public static final")
    void trapCodesShouldBePublicStaticFinal() {
      int modifiers = getTrapCodesClass().getModifiers();
      assertTrue(Modifier.isPublic(modifiers), "TrapCodes should be public");
      assertTrue(Modifier.isStatic(modifiers), "TrapCodes should be static");
      assertTrue(Modifier.isFinal(modifiers), "TrapCodes should be final");
    }

    @Test
    @DisplayName("TrapCodes should have private constructor")
    void trapCodesShouldHavePrivateConstructor() {
      Constructor<?>[] constructors = getTrapCodesClass().getDeclaredConstructors();
      assertEquals(1, constructors.length, "TrapCodes should have exactly one constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()),
          "TrapCodes constructor should be private");
    }

    @Test
    @DisplayName("TrapCodes should have STACK_OVERFLOW constant")
    void trapCodesShouldHaveStackOverflowConstant() {
      assertEquals(0, NativeFunctionBindings.TrapCodes.STACK_OVERFLOW);
    }

    @Test
    @DisplayName("TrapCodes should have MEMORY_OUT_OF_BOUNDS constant")
    void trapCodesShouldHaveMemoryOutOfBoundsConstant() {
      assertEquals(1, NativeFunctionBindings.TrapCodes.MEMORY_OUT_OF_BOUNDS);
    }

    @Test
    @DisplayName("TrapCodes should have HEAP_MISALIGNED constant")
    void trapCodesShouldHaveHeapMisalignedConstant() {
      assertEquals(2, NativeFunctionBindings.TrapCodes.HEAP_MISALIGNED);
    }

    @Test
    @DisplayName("TrapCodes should have TABLE_OUT_OF_BOUNDS constant")
    void trapCodesShouldHaveTableOutOfBoundsConstant() {
      assertEquals(3, NativeFunctionBindings.TrapCodes.TABLE_OUT_OF_BOUNDS);
    }

    @Test
    @DisplayName("TrapCodes should have INDIRECT_CALL_TO_NULL constant")
    void trapCodesShouldHaveIndirectCallToNullConstant() {
      assertEquals(4, NativeFunctionBindings.TrapCodes.INDIRECT_CALL_TO_NULL);
    }

    @Test
    @DisplayName("TrapCodes should have BAD_SIGNATURE constant")
    void trapCodesShouldHaveBadSignatureConstant() {
      assertEquals(5, NativeFunctionBindings.TrapCodes.BAD_SIGNATURE);
    }

    @Test
    @DisplayName("TrapCodes should have INTEGER_OVERFLOW constant")
    void trapCodesShouldHaveIntegerOverflowConstant() {
      assertEquals(6, NativeFunctionBindings.TrapCodes.INTEGER_OVERFLOW);
    }

    @Test
    @DisplayName("TrapCodes should have INTEGER_DIVISION_BY_ZERO constant")
    void trapCodesShouldHaveIntegerDivisionByZeroConstant() {
      assertEquals(7, NativeFunctionBindings.TrapCodes.INTEGER_DIVISION_BY_ZERO);
    }

    @Test
    @DisplayName("TrapCodes should have BAD_CONVERSION_TO_INTEGER constant")
    void trapCodesShouldHaveBadConversionToIntegerConstant() {
      assertEquals(8, NativeFunctionBindings.TrapCodes.BAD_CONVERSION_TO_INTEGER);
    }

    @Test
    @DisplayName("TrapCodes should have UNREACHABLE_CODE_REACHED constant")
    void trapCodesShouldHaveUnreachableCodeReachedConstant() {
      assertEquals(9, NativeFunctionBindings.TrapCodes.UNREACHABLE_CODE_REACHED);
    }

    @Test
    @DisplayName("TrapCodes should have INTERRUPT constant")
    void trapCodesShouldHaveInterruptConstant() {
      assertEquals(10, NativeFunctionBindings.TrapCodes.INTERRUPT);
    }

    @Test
    @DisplayName("TrapCodes should have OUT_OF_FUEL constant")
    void trapCodesShouldHaveOutOfFuelConstant() {
      assertEquals(11, NativeFunctionBindings.TrapCodes.OUT_OF_FUEL);
    }

    @Test
    @DisplayName("TrapCodes should have NULL_REFERENCE constant")
    void trapCodesShouldHaveNullReferenceConstant() {
      assertEquals(12, NativeFunctionBindings.TrapCodes.NULL_REFERENCE);
    }

    @Test
    @DisplayName("TrapCodes should have ARRAY_OUT_OF_BOUNDS constant")
    void trapCodesShouldHaveArrayOutOfBoundsConstant() {
      assertEquals(13, NativeFunctionBindings.TrapCodes.ARRAY_OUT_OF_BOUNDS);
    }

    @Test
    @DisplayName("TrapCodes should have UNKNOWN constant")
    void trapCodesShouldHaveUnknownConstant() {
      assertEquals(14, NativeFunctionBindings.TrapCodes.UNKNOWN);
    }
  }

  @Nested
  @DisplayName("TrapInfo Inner Class Tests")
  class TrapInfoInnerClassTests {

    private Class<?> getTrapInfoClass() {
      return NativeFunctionBindings.TrapInfo.class;
    }

    @Test
    @DisplayName("TrapInfo should be public static final")
    void trapInfoShouldBePublicStaticFinal() {
      int modifiers = getTrapInfoClass().getModifiers();
      assertTrue(Modifier.isPublic(modifiers), "TrapInfo should be public");
      assertTrue(Modifier.isStatic(modifiers), "TrapInfo should be static");
      assertTrue(Modifier.isFinal(modifiers), "TrapInfo should be final");
    }

    @Test
    @DisplayName("TrapInfo should have public constructor")
    void trapInfoShouldHavePublicConstructor() {
      assertDoesNotThrow(
          () -> {
            Constructor<?> constructor =
                getTrapInfoClass().getDeclaredConstructor(int.class, long.class, boolean.class);
            assertNotNull(constructor, "TrapInfo should have constructor");
            assertTrue(
                Modifier.isPublic(constructor.getModifiers()),
                "TrapInfo constructor should be public");
          });
    }

    @Test
    @DisplayName("TrapInfo should have getTrapCode method")
    void trapInfoShouldHaveGetTrapCodeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTrapInfoClass().getDeclaredMethod("getTrapCode");
            assertNotNull(method, "getTrapCode method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "getTrapCode should be public");
            assertEquals(int.class, method.getReturnType(), "getTrapCode should return int");
          });
    }

    @Test
    @DisplayName("TrapInfo should have getInstructionOffset method")
    void trapInfoShouldHaveGetInstructionOffsetMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTrapInfoClass().getDeclaredMethod("getInstructionOffset");
            assertNotNull(method, "getInstructionOffset method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "getInstructionOffset should be public");
            assertEquals(
                long.class, method.getReturnType(), "getInstructionOffset should return long");
          });
    }

    @Test
    @DisplayName("TrapInfo should have isTrap method")
    void trapInfoShouldHaveIsTrapMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTrapInfoClass().getDeclaredMethod("isTrap");
            assertNotNull(method, "isTrap method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "isTrap should be public");
            assertEquals(boolean.class, method.getReturnType(), "isTrap should return boolean");
          });
    }

    @Test
    @DisplayName("TrapInfo should have toString method")
    void trapInfoShouldHaveToStringMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTrapInfoClass().getDeclaredMethod("toString");
            assertNotNull(method, "toString method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "toString should be public");
            assertEquals(String.class, method.getReturnType(), "toString should return String");
          });
    }

    @Test
    @DisplayName("TrapInfo instance should be constructable")
    void trapInfoInstanceShouldBeConstructable() {
      NativeFunctionBindings.TrapInfo trapInfo = new NativeFunctionBindings.TrapInfo(5, 100L, true);
      assertEquals(5, trapInfo.getTrapCode());
      assertEquals(100L, trapInfo.getInstructionOffset());
      assertTrue(trapInfo.isTrap());
    }

    @Test
    @DisplayName("TrapInfo toString should contain field values")
    void trapInfoToStringShouldContainFieldValues() {
      NativeFunctionBindings.TrapInfo trapInfo =
          new NativeFunctionBindings.TrapInfo(7, 256L, false);
      String str = trapInfo.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("7"), "toString should contain trapCode");
      assertTrue(str.contains("256"), "toString should contain instructionOffset");
      assertTrue(str.contains("false"), "toString should contain isTrap value");
    }
  }

  @Nested
  @DisplayName("FunctionBinding Inner Class Tests")
  class FunctionBindingInnerClassTests {

    @Test
    @DisplayName("FunctionBinding should be private final")
    void functionBindingShouldBePrivateFinal() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      Optional<Class<?>> functionBindingClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("FunctionBinding"))
              .findFirst();
      assertTrue(functionBindingClass.isPresent(), "FunctionBinding class should exist");

      int modifiers = functionBindingClass.get().getModifiers();
      assertTrue(Modifier.isPrivate(modifiers), "FunctionBinding should be private");
      assertTrue(Modifier.isFinal(modifiers), "FunctionBinding should be final");
    }

    @Test
    @DisplayName("FunctionBinding should have constructor with String and FunctionDescriptor")
    void functionBindingShouldHaveConstructor() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      Optional<Class<?>> functionBindingClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("FunctionBinding"))
              .findFirst();
      assertTrue(functionBindingClass.isPresent(), "FunctionBinding class should exist");

      assertDoesNotThrow(
          () -> {
            Constructor<?>[] constructors = functionBindingClass.get().getDeclaredConstructors();
            assertTrue(constructors.length > 0, "FunctionBinding should have a constructor");
            // The first parameter should be the enclosing class reference
            // Find constructor with String and FunctionDescriptor
            boolean found =
                Arrays.stream(constructors)
                    .anyMatch(
                        c -> {
                          Class<?>[] params = c.getParameterTypes();
                          return params.length >= 2
                              && params[params.length - 2] == String.class
                              && params[params.length - 1] == FunctionDescriptor.class;
                        });
            assertTrue(
                found,
                "FunctionBinding should have constructor with String and "
                    + "FunctionDescriptor parameters");
          });
    }

    @Test
    @DisplayName("FunctionBinding should have getFunctionName method")
    void functionBindingShouldHaveGetFunctionNameMethod() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      Optional<Class<?>> functionBindingClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("FunctionBinding"))
              .findFirst();
      assertTrue(functionBindingClass.isPresent(), "FunctionBinding class should exist");

      assertDoesNotThrow(
          () -> {
            Method method = functionBindingClass.get().getDeclaredMethod("getFunctionName");
            assertNotNull(method, "getFunctionName method should exist");
            assertEquals(
                String.class, method.getReturnType(), "getFunctionName should return String");
          });
    }

    @Test
    @DisplayName("FunctionBinding should have getDescriptor method")
    void functionBindingShouldHaveGetDescriptorMethod() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      Optional<Class<?>> functionBindingClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("FunctionBinding"))
              .findFirst();
      assertTrue(functionBindingClass.isPresent(), "FunctionBinding class should exist");

      assertDoesNotThrow(
          () -> {
            Method method = functionBindingClass.get().getDeclaredMethod("getDescriptor");
            assertNotNull(method, "getDescriptor method should exist");
            assertEquals(
                FunctionDescriptor.class,
                method.getReturnType(),
                "getDescriptor should return FunctionDescriptor");
          });
    }

    @Test
    @DisplayName("FunctionBinding should have getMethodHandle method")
    void functionBindingShouldHaveGetMethodHandleMethod() {
      Class<?>[] declaredClasses = getTestedClass().getDeclaredClasses();
      Optional<Class<?>> functionBindingClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("FunctionBinding"))
              .findFirst();
      assertTrue(functionBindingClass.isPresent(), "FunctionBinding class should exist");

      assertDoesNotThrow(
          () -> {
            Method method = functionBindingClass.get().getDeclaredMethod("getMethodHandle");
            assertNotNull(method, "getMethodHandle method should exist");
            assertEquals(
                Optional.class, method.getReturnType(), "getMethodHandle should return Optional");
          });
    }
  }

  @Nested
  @DisplayName("Utility Methods Tests")
  class UtilityMethodsTests {

    @Test
    @DisplayName("Should have isInitialized method")
    void shouldHaveIsInitializedMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("isInitialized");
            assertNotNull(method, "isInitialized method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "isInitialized should be public");
            assertEquals(
                boolean.class, method.getReturnType(), "isInitialized should return boolean");
          });
    }
  }

  @Nested
  @DisplayName("Global Methods Tests")
  class GlobalMethodsTests {

    @Test
    @DisplayName("Should have globalCreate method")
    void shouldHaveGlobalCreateMethod() {
      assertDoesNotThrow(
          () -> {
            // Actual signature: globalCreate(MemorySegment storePtr, int valueType,
            //                                int isMutable, WasmValue initialValue)
            Method method =
                getTestedClass()
                    .getDeclaredMethod(
                        "globalCreate",
                        MemorySegment.class,
                        int.class,
                        int.class,
                        ai.tegmentum.wasmtime4j.WasmValue.class);
            assertNotNull(method, "globalCreate method should exist");
            assertTrue(Modifier.isPublic(method.getModifiers()), "globalCreate should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "globalCreate should return MemorySegment");
          });
    }
  }

  @Nested
  @DisplayName("WASI-NN Methods Tests")
  class WasiNnMethodsTests {

    @Test
    @DisplayName("Should have wasiNnContextCreate method")
    void shouldHaveWasiNnContextCreateMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("wasiNnContextCreate");
            assertNotNull(method, "wasiNnContextCreate method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "wasiNnContextCreate should be public");
            assertEquals(
                MemorySegment.class,
                method.getReturnType(),
                "wasiNnContextCreate should return MemorySegment");
          });
    }

    @Test
    @DisplayName("Should have wasiNnIsAvailable method")
    void shouldHaveWasiNnIsAvailableMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = getTestedClass().getDeclaredMethod("wasiNnIsAvailable");
            assertNotNull(method, "wasiNnIsAvailable method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "wasiNnIsAvailable should be public");
            assertEquals(int.class, method.getReturnType(), "wasiNnIsAvailable should return int");
          });
    }

    @Test
    @DisplayName("Should have wasiNnContextClose method")
    void shouldHaveWasiNnContextCloseMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("wasiNnContextClose", MemorySegment.class);
            assertNotNull(method, "wasiNnContextClose method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()), "wasiNnContextClose should be public");
            assertEquals(
                void.class, method.getReturnType(), "wasiNnContextClose should return void");
          });
    }
  }

  @Nested
  @DisplayName("Pooling Allocator Methods Tests")
  class PoolingAllocatorMethodsTests {

    @Test
    @DisplayName("Should have poolingAllocatorDestroy method")
    void shouldHavePoolingAllocatorDestroyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass().getDeclaredMethod("poolingAllocatorDestroy", MemorySegment.class);
            assertNotNull(method, "poolingAllocatorDestroy method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "poolingAllocatorDestroy should be public");
            assertEquals(
                void.class, method.getReturnType(), "poolingAllocatorDestroy should return void");
          });
    }

    @Test
    @DisplayName("Should have poolingAllocatorWarmPools method")
    void shouldHavePoolingAllocatorWarmPoolsMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("poolingAllocatorWarmPools", MemorySegment.class);
            assertNotNull(method, "poolingAllocatorWarmPools method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "poolingAllocatorWarmPools should be public");
            assertEquals(
                boolean.class,
                method.getReturnType(),
                "poolingAllocatorWarmPools should return boolean");
          });
    }

    @Test
    @DisplayName("Should have poolingAllocatorPerformMaintenance method")
    void shouldHavePoolingAllocatorPerformMaintenanceMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                getTestedClass()
                    .getDeclaredMethod("poolingAllocatorPerformMaintenance", MemorySegment.class);
            assertNotNull(method, "poolingAllocatorPerformMaintenance method should exist");
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "poolingAllocatorPerformMaintenance should be public");
            assertEquals(
                boolean.class,
                method.getReturnType(),
                "poolingAllocatorPerformMaintenance should return boolean");
          });
    }
  }

  @Nested
  @DisplayName("Naming Convention Tests")
  class NamingConventionTests {

    @Test
    @DisplayName("All public methods should follow camelCase naming")
    void allPublicMethodsShouldFollowCamelCaseNaming() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      Set<String> nonCamelCaseMethods = new HashSet<>();

      for (Method method : methods) {
        if (Modifier.isPublic(method.getModifiers())) {
          String name = method.getName();
          // Check if first char is lowercase
          if (!Character.isLowerCase(name.charAt(0))) {
            nonCamelCaseMethods.add(name);
          }
          // Check if contains underscores (not camelCase)
          if (name.contains("_")) {
            nonCamelCaseMethods.add(name);
          }
        }
      }

      assertTrue(
          nonCamelCaseMethods.isEmpty(),
          "All public methods should follow camelCase naming. Non-compliant: "
              + nonCamelCaseMethods);
    }

    @Test
    @DisplayName("Public methods should have descriptive names")
    void publicMethodsShouldHaveDescriptiveNames() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      Set<String> shortNames = new HashSet<>();

      for (Method method : methods) {
        if (Modifier.isPublic(method.getModifiers())) {
          String name = method.getName();
          // Names should be at least 4 characters (excluding getter/setter patterns)
          if (name.length() < 4 && !name.startsWith("is") && !name.startsWith("get")) {
            shortNames.add(name);
          }
        }
      }

      assertTrue(
          shortNames.isEmpty(),
          "Public methods should have descriptive names (at least 4 chars). Short names: "
              + shortNames);
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("Should have significant number of public methods for FFI bindings")
    void shouldHaveSignificantNumberOfPublicMethods() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      long publicMethodCount =
          Arrays.stream(methods).filter(m -> Modifier.isPublic(m.getModifiers())).count();

      // The class has hundreds of FFI wrapper methods
      assertTrue(
          publicMethodCount > 100,
          "NativeFunctionBindings should have more than 100 public methods for FFI bindings. "
              + "Found: "
              + publicMethodCount);
    }

    @Test
    @DisplayName("Should have some private helper methods")
    void shouldHaveSomePrivateHelperMethods() {
      Method[] methods = getTestedClass().getDeclaredMethods();
      long privateMethodCount =
          Arrays.stream(methods).filter(m -> Modifier.isPrivate(m.getModifiers())).count();

      assertTrue(
          privateMethodCount >= 3,
          "NativeFunctionBindings should have at least 3 private helper methods. Found: "
              + privateMethodCount);
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Should use ConcurrentHashMap for functionBindings")
    void shouldUseConcurrentHashMapForFunctionBindings() {
      assertDoesNotThrow(
          () -> {
            Field field = getTestedClass().getDeclaredField("functionBindings");
            assertEquals(
                ConcurrentHashMap.class,
                field.getType(),
                "functionBindings should be ConcurrentHashMap for thread safety");
          });
    }

    @Test
    @DisplayName("Should have volatile instance field for singleton pattern")
    void shouldHaveVolatileInstanceFieldForSingletonPattern() {
      assertDoesNotThrow(
          () -> {
            Field field = getTestedClass().getDeclaredField("instance");
            assertTrue(
                Modifier.isVolatile(field.getModifiers()),
                "instance field should be volatile for proper double-checked locking");
          });
    }

    @Test
    @DisplayName("Should have volatile initialized field")
    void shouldHaveVolatileInitializedField() {
      assertDoesNotThrow(
          () -> {
            Field field = getTestedClass().getDeclaredField("initialized");
            assertTrue(
                Modifier.isVolatile(field.getModifiers()),
                "initialized field should be volatile for visibility across threads");
          });
    }
  }
}
