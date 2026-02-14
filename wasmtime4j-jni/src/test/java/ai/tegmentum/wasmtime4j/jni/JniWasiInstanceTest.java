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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for JniWasiInstance class.
 *
 * <p>These tests use reflection to verify class structure, fields, and method signatures without
 * requiring native library initialization. This approach ensures full test coverage for code
 * structure and API contract verification.
 */
@DisplayName("JniWasiInstance Tests")
class JniWasiInstanceTest {

  private static final String CLASS_NAME = "ai.tegmentum.wasmtime4j.jni.JniWasiInstance";

  private Class<?> getTestedClass() throws ClassNotFoundException {
    return Class.forName(CLASS_NAME);
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniWasiInstance should be a final class");
    }

    @Test
    @DisplayName("Should implement WasiInstance interface")
    void shouldImplementWasiInstance() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      assertTrue(
          WasiInstance.class.isAssignableFrom(clazz),
          "JniWasiInstance should implement WasiInstance interface");
    }

    @Test
    @DisplayName("Should be public")
    void shouldBePublic() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "JniWasiInstance should be public");
    }

    @Test
    @DisplayName("Should have correct package")
    void shouldHaveCorrectPackage() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni",
          clazz.getPackage().getName(),
          "JniWasiInstance should be in jni package");
    }

    @Test
    @DisplayName("Should implement AutoCloseable through WasiInstance")
    void shouldImplementAutoCloseable() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      assertTrue(
          AutoCloseable.class.isAssignableFrom(clazz),
          "JniWasiInstance should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Field Declaration Tests")
  class FieldDeclarationTests {

    @Test
    @DisplayName("Should have LOGGER field")
    void shouldHaveLoggerField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("LOGGER");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be of type Logger");
    }

    @Test
    @DisplayName("Should have NEXT_INSTANCE_ID field")
    void shouldHaveNextInstanceIdField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("NEXT_INSTANCE_ID");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "NEXT_INSTANCE_ID should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "NEXT_INSTANCE_ID should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "NEXT_INSTANCE_ID should be final");
      assertEquals(
          AtomicLong.class, field.getType(), "NEXT_INSTANCE_ID should be of type AtomicLong");
    }

    @Test
    @DisplayName("Should have instanceId field")
    void shouldHaveInstanceIdField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("instanceId");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "instanceId should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "instanceId should be final");
      assertEquals(long.class, field.getType(), "instanceId should be of type long");
    }

    @Test
    @DisplayName("Should have component field")
    void shouldHaveComponentField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("component");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "component should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "component should be final");
      assertEquals(
          JniWasiComponent.class, field.getType(), "component should be of type JniWasiComponent");
    }

    @Test
    @DisplayName("Should have instanceHandle field")
    void shouldHaveInstanceHandleField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("instanceHandle");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "instanceHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "instanceHandle should be final");
      assertTrue(
          field.getType().getName().contains("JniComponentInstanceHandle"),
          "instanceHandle should be JniComponentInstanceHandle type");
    }

    @Test
    @DisplayName("Should have config field")
    void shouldHaveConfigField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("config");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "config should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "config should be final");
    }

    @Test
    @DisplayName("Should have createdAt field")
    void shouldHaveCreatedAtField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("createdAt");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "createdAt should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "createdAt should be final");
      assertEquals(Instant.class, field.getType(), "createdAt should be of type Instant");
    }

    @Test
    @DisplayName("Should have properties field")
    void shouldHavePropertiesField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("properties");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "properties should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "properties should be final");
      assertEquals(Map.class, field.getType(), "properties should be of type Map");
    }

    @Test
    @DisplayName("Should have resources field")
    void shouldHaveResourcesField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("resources");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "resources should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "resources should be final");
      assertEquals(List.class, field.getType(), "resources should be of type List");
    }

    @Test
    @DisplayName("Should have volatile state field")
    void shouldHaveStateField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("state");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "state should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "state should be volatile");
    }

    @Test
    @DisplayName("Should have volatile lastActivityAt field")
    void shouldHaveLastActivityAtField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("lastActivityAt");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "lastActivityAt should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "lastActivityAt should be volatile");
      assertEquals(Instant.class, field.getType(), "lastActivityAt should be of type Instant");
    }

    @Test
    @DisplayName("Should have volatile closed field")
    void shouldHaveClosedField() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("closed");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "closed should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "closed should be volatile");
      assertEquals(boolean.class, field.getType(), "closed should be of type boolean");
    }

    @Test
    @DisplayName("Should have volatile cachedExportedFunctions field")
    void shouldHaveCachedExportedFunctionsField()
        throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("cachedExportedFunctions");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "cachedExportedFunctions should be private");
      assertTrue(
          Modifier.isVolatile(field.getModifiers()), "cachedExportedFunctions should be volatile");
      assertEquals(List.class, field.getType(), "cachedExportedFunctions should be of type List");
    }

    @Test
    @DisplayName("Should have volatile cachedExportedInterfaces field")
    void shouldHaveCachedExportedInterfacesField()
        throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("cachedExportedInterfaces");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "cachedExportedInterfaces should be private");
      assertTrue(
          Modifier.isVolatile(field.getModifiers()), "cachedExportedInterfaces should be volatile");
      assertEquals(List.class, field.getType(), "cachedExportedInterfaces should be of type List");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have exactly one public constructor")
    void shouldHaveOnePublicConstructor() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      Constructor<?>[] constructors = clazz.getConstructors();
      assertEquals(1, constructors.length, "Should have exactly one public constructor");
    }

    @Test
    @DisplayName("Constructor should have three parameters")
    void constructorShouldHaveThreeParameters() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      Constructor<?>[] constructors = clazz.getConstructors();
      assertEquals(3, constructors[0].getParameterCount(), "Constructor should have 3 parameters");
    }

    @Test
    @DisplayName("Constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      Constructor<?>[] constructors = clazz.getConstructors();
      Class<?>[] paramTypes = constructors[0].getParameterTypes();

      assertEquals(
          JniWasiComponent.class, paramTypes[0], "First parameter should be JniWasiComponent");
      assertTrue(
          paramTypes[1].getName().contains("JniComponentInstanceHandle"),
          "Second parameter should be JniComponentInstanceHandle");
    }
  }

  @Nested
  @DisplayName("WasiInstance Interface Method Tests")
  class WasiInstanceInterfaceMethodTests {

    @Test
    @DisplayName("Should have getId method")
    void shouldHaveGetIdMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getId");
      assertEquals(long.class, method.getReturnType(), "getId should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getId should be public");
    }

    @Test
    @DisplayName("Should have getComponent method")
    void shouldHaveGetComponentMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getComponent");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getComponent should be public");
    }

    @Test
    @DisplayName("Should have getConfig method")
    void shouldHaveGetConfigMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getConfig");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getConfig should be public");
    }

    @Test
    @DisplayName("Should have getState method")
    void shouldHaveGetStateMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getState");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getState should be public");
    }

    @Test
    @DisplayName("Should have getCreatedAt method")
    void shouldHaveGetCreatedAtMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getCreatedAt");
      assertEquals(Instant.class, method.getReturnType(), "getCreatedAt should return Instant");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getCreatedAt should be public");
    }

    @Test
    @DisplayName("Should have getLastActivityAt method")
    void shouldHaveGetLastActivityAtMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getLastActivityAt");
      assertEquals(
          Optional.class, method.getReturnType(), "getLastActivityAt should return Optional");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getLastActivityAt should be public");
    }
  }

  @Nested
  @DisplayName("Function Call Method Tests")
  class FunctionCallMethodTests {

    @Test
    @DisplayName("Should have call method with varargs")
    void shouldHaveCallMethodWithVarargs() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("call", String.class, Object[].class);
      assertEquals(Object.class, method.getReturnType(), "call should return Object");
      assertTrue(Modifier.isPublic(method.getModifiers()), "call should be public");
    }

    @Test
    @DisplayName("Should have call method with timeout")
    void shouldHaveCallMethodWithTimeout() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("call", String.class, Duration.class, Object[].class);
      assertEquals(Object.class, method.getReturnType(), "call should return Object");
      assertTrue(Modifier.isPublic(method.getModifiers()), "call should be public");
    }

    @Test
    @DisplayName("Should have callAsync method")
    void shouldHaveCallAsyncMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("callAsync", String.class, Object[].class);
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "callAsync should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "callAsync should be public");
    }

    @Test
    @DisplayName("Call method should declare WasmException")
    void callMethodShouldDeclareWasmException()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("call", String.class, Object[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "call should declare exceptions");
      boolean hasWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(hasWasmException, "call should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Export Methods Tests")
  class ExportMethodsTests {

    @Test
    @DisplayName("Should have getExportedFunctions method")
    void shouldHaveGetExportedFunctionsMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getExportedFunctions");
      assertEquals(List.class, method.getReturnType(), "getExportedFunctions should return List");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getExportedFunctions should be public");
    }

    @Test
    @DisplayName("Should have getExportedInterfaces method")
    void shouldHaveGetExportedInterfacesMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getExportedInterfaces");
      assertEquals(List.class, method.getReturnType(), "getExportedInterfaces should return List");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "getExportedInterfaces should be public");
    }

    @Test
    @DisplayName("Should have getFunctionMetadata method")
    void shouldHaveGetFunctionMetadataMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getFunctionMetadata", String.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "getFunctionMetadata should be public");
    }
  }

  @Nested
  @DisplayName("Resource Management Method Tests")
  class ResourceManagementMethodTests {

    @Test
    @DisplayName("Should have getResources method without parameters")
    void shouldHaveGetResourcesMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getResources");
      assertEquals(List.class, method.getReturnType(), "getResources should return List");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getResources should be public");
    }

    @Test
    @DisplayName("Should have getResources method with type parameter")
    void shouldHaveGetResourcesWithTypeMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getResources", String.class);
      assertEquals(List.class, method.getReturnType(), "getResources should return List");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getResources should be public");
    }

    @Test
    @DisplayName("Should have getResource method")
    void shouldHaveGetResourceMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getResource", long.class);
      assertEquals(Optional.class, method.getReturnType(), "getResource should return Optional");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getResource should be public");
    }

    @Test
    @DisplayName("Should have createResource method")
    void shouldHaveCreateResourceMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("createResource", String.class, Object[].class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "createResource should be public");
    }
  }

  @Nested
  @DisplayName("Statistics and Monitoring Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("Should have getStats method")
    void shouldHaveGetStatsMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getStats");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getStats should be public");
    }

    @Test
    @DisplayName("Should have getMemoryInfo method")
    void shouldHaveGetMemoryInfoMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getMemoryInfo");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getMemoryInfo should be public");
    }
  }

  @Nested
  @DisplayName("Lifecycle Control Method Tests")
  class LifecycleControlMethodTests {

    @Test
    @DisplayName("Should have suspend method")
    void shouldHaveSuspendMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("suspend");
      assertEquals(void.class, method.getReturnType(), "suspend should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "suspend should be public");
    }

    @Test
    @DisplayName("Should have resume method")
    void shouldHaveResumeMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("resume");
      assertEquals(void.class, method.getReturnType(), "resume should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "resume should be public");
    }

    @Test
    @DisplayName("Should have terminate method")
    void shouldHaveTerminateMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("terminate");
      assertEquals(void.class, method.getReturnType(), "terminate should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "terminate should be public");
    }

    @Test
    @DisplayName("Suspend should declare WasmException")
    void suspendShouldDeclareException() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("suspend");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "suspend should declare exceptions");
    }

    @Test
    @DisplayName("Resume should declare WasmException")
    void resumeShouldDeclareException() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("resume");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "resume should declare exceptions");
    }

    @Test
    @DisplayName("Terminate should declare WasmException")
    void terminateShouldDeclareException() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("terminate");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "terminate should declare exceptions");
    }
  }

  @Nested
  @DisplayName("State Query Method Tests")
  class StateQueryMethodTests {

    @Test
    @DisplayName("Should have isValid method")
    void shouldHaveIsValidMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("isValid");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isValid should be public");
    }

    @Test
    @DisplayName("Should have isExecuting method")
    void shouldHaveIsExecutingMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("isExecuting");
      assertEquals(boolean.class, method.getReturnType(), "isExecuting should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isExecuting should be public");
    }
  }

  @Nested
  @DisplayName("Property Management Method Tests")
  class PropertyManagementMethodTests {

    @Test
    @DisplayName("Should have setProperty method")
    void shouldHaveSetPropertyMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("setProperty", String.class, Object.class);
      assertEquals(void.class, method.getReturnType(), "setProperty should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "setProperty should be public");
    }

    @Test
    @DisplayName("Should have getProperty method")
    void shouldHaveGetPropertyMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getProperty", String.class);
      assertEquals(Optional.class, method.getReturnType(), "getProperty should return Optional");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getProperty should be public");
    }

    @Test
    @DisplayName("Should have getProperties method")
    void shouldHaveGetPropertiesMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("getProperties");
      assertEquals(Map.class, method.getReturnType(), "getProperties should return Map");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getProperties should be public");
    }
  }

  @Nested
  @DisplayName("Close Method Tests")
  class CloseMethodTests {

    @Test
    @DisplayName("Should have close method")
    void shouldHaveCloseMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("close");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "close should be public");
    }

    @Test
    @DisplayName("Close method should not throw checked exceptions")
    void closeMethodShouldNotThrowCheckedException()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getMethod("close");
      Class<?>[] exceptions = method.getExceptionTypes();
      for (Class<?> ex : exceptions) {
        assertTrue(
            RuntimeException.class.isAssignableFrom(ex) || Error.class.isAssignableFrom(ex),
            "close should not throw checked exceptions except for Exception itself");
      }
    }
  }

  @Nested
  @DisplayName("Package-Private Method Tests")
  class PackagePrivateMethodTests {

    @Test
    @DisplayName("Should have getInstanceHandle method")
    void shouldHaveGetInstanceHandleMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getDeclaredMethod("getInstanceHandle");
      assertFalse(
          Modifier.isPublic(method.getModifiers()), "getInstanceHandle should not be public");
      assertFalse(
          Modifier.isPrivate(method.getModifiers()), "getInstanceHandle should not be private");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("Should have ensureNotClosed method")
    void shouldHaveEnsureNotClosedMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getDeclaredMethod("ensureNotClosed");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "ensureNotClosed should be private");
      assertEquals(void.class, method.getReturnType(), "ensureNotClosed should return void");
    }

    @Test
    @DisplayName("Should have ensureCallableState method")
    void shouldHaveEnsureCallableStateMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getDeclaredMethod("ensureCallableState");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "ensureCallableState should be private");
      assertEquals(void.class, method.getReturnType(), "ensureCallableState should return void");
    }

    @Test
    @DisplayName("Should have updateLastActivity method")
    void shouldHaveUpdateLastActivityMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getDeclaredMethod("updateLastActivity");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "updateLastActivity should be private");
      assertEquals(void.class, method.getReturnType(), "updateLastActivity should return void");
    }

    @Test
    @DisplayName("Should have setState method")
    void shouldHaveSetStateMethod() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      boolean found = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.getName().equals("setState") && method.getParameterCount() == 1) {
          assertTrue(Modifier.isPrivate(method.getModifiers()), "setState should be private");
          assertEquals(void.class, method.getReturnType(), "setState should return void");
          found = true;
          break;
        }
      }
      assertTrue(found, "setState method should exist");
    }

    @Test
    @DisplayName("Should have convertToWitValue method")
    void shouldHaveConvertToWitValueMethod() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      boolean found = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.getName().equals("convertToWitValue")) {
          assertTrue(
              Modifier.isPrivate(method.getModifiers()), "convertToWitValue should be private");
          found = true;
          break;
        }
      }
      assertTrue(found, "convertToWitValue method should exist");
    }

    @Test
    @DisplayName("Should have extractExportedFunctions method")
    void shouldHaveExtractExportedFunctionsMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getDeclaredMethod("extractExportedFunctions");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "extractExportedFunctions should be private");
      assertEquals(
          List.class, method.getReturnType(), "extractExportedFunctions should return List");
    }

    @Test
    @DisplayName("Should have extractExportedInterfaces method")
    void shouldHaveExtractExportedInterfacesMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> clazz = getTestedClass();
      Method method = clazz.getDeclaredMethod("extractExportedInterfaces");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "extractExportedInterfaces should be private");
      assertEquals(
          List.class, method.getReturnType(), "extractExportedInterfaces should return List");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("Should have at least 25 public methods")
    void shouldHaveMinimumPublicMethods() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      long publicMethodCount =
          Arrays.stream(clazz.getMethods())
              .filter(m -> m.getDeclaringClass() == clazz)
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertTrue(
          publicMethodCount >= 25,
          "Should have at least 25 public methods, found: " + publicMethodCount);
    }

    @Test
    @DisplayName("Should have declared methods for all WasiInstance interface methods")
    void shouldImplementAllInterfaceMethods() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      Method[] interfaceMethods = WasiInstance.class.getMethods();

      Set<String> missingMethods = new HashSet<>();
      for (Method interfaceMethod : interfaceMethods) {
        try {
          clazz.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
          missingMethods.add(interfaceMethod.getName());
        }
      }

      assertTrue(
          missingMethods.isEmpty(), "Missing WasiInstance interface methods: " + missingMethods);
    }
  }

  @Nested
  @DisplayName("Static Field Count Tests")
  class StaticFieldCountTests {

    @Test
    @DisplayName("Should have exactly 3 static fields")
    void shouldHaveThreeStaticFields() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      long staticFieldCount =
          Arrays.stream(clazz.getDeclaredFields())
              .filter(f -> Modifier.isStatic(f.getModifiers()))
              .filter(f -> !f.isSynthetic() && !f.getName().startsWith("$"))
              .count();
      assertEquals(2, staticFieldCount, "Should have exactly 2 static fields");
    }

    @Test
    @DisplayName("Static fields should all be private and final")
    void staticFieldsShouldBePrivateFinal() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      List<Field> staticFields =
          Arrays.stream(clazz.getDeclaredFields())
              .filter(f -> Modifier.isStatic(f.getModifiers()))
              .filter(f -> !f.isSynthetic() && !f.getName().startsWith("$"))
              .collect(Collectors.toList());

      for (Field field : staticFields) {
        assertTrue(
            Modifier.isPrivate(field.getModifiers()),
            "Static field " + field.getName() + " should be private");
        assertTrue(
            Modifier.isFinal(field.getModifiers()),
            "Static field " + field.getName() + " should be final");
      }
    }
  }

  @Nested
  @DisplayName("Instance Field Count Tests")
  class InstanceFieldCountTests {

    @Test
    @DisplayName("Should have 12 instance fields")
    void shouldHaveTwelveInstanceFields() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      long instanceFieldCount =
          Arrays.stream(clazz.getDeclaredFields())
              .filter(f -> !Modifier.isStatic(f.getModifiers()))
              .count();
      assertEquals(
          12,
          instanceFieldCount,
          "Should have exactly 12 instance fields, found: " + instanceFieldCount);
    }

    @Test
    @DisplayName("Should have 5 volatile instance fields")
    void shouldHaveFiveVolatileFields() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      List<String> volatileFields =
          Arrays.stream(clazz.getDeclaredFields())
              .filter(f -> !Modifier.isStatic(f.getModifiers()))
              .filter(f -> Modifier.isVolatile(f.getModifiers()))
              .map(Field::getName)
              .collect(Collectors.toList());
      assertEquals(
          5,
          volatileFields.size(),
          "Should have exactly 5 volatile fields, found: " + volatileFields);
    }

    @Test
    @DisplayName("Should have 7 final instance fields")
    void shouldHaveSevenFinalFields() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      List<String> finalFields =
          Arrays.stream(clazz.getDeclaredFields())
              .filter(f -> !Modifier.isStatic(f.getModifiers()))
              .filter(f -> Modifier.isFinal(f.getModifiers()))
              .map(Field::getName)
              .collect(Collectors.toList());
      assertEquals(
          7,
          finalFields.size(),
          "Should have exactly 7 final instance fields, found: " + finalFields);
    }
  }

  @Nested
  @DisplayName("Field Naming Convention Tests")
  class FieldNamingConventionTests {

    @Test
    @DisplayName("Static fields should use UPPER_CASE naming")
    void staticFieldsShouldUseUpperCase() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      List<Field> staticFields =
          Arrays.stream(clazz.getDeclaredFields())
              .filter(f -> Modifier.isStatic(f.getModifiers()))
              .filter(f -> !f.isSynthetic() && !f.getName().startsWith("$"))
              .collect(Collectors.toList());

      for (Field field : staticFields) {
        String name = field.getName();
        assertTrue(
            name.equals(name.toUpperCase()) || name.matches("^[A-Z][A-Z_0-9]*$"),
            "Static field " + name + " should use UPPER_CASE naming convention");
      }
    }

    @Test
    @DisplayName("Instance fields should use camelCase naming")
    void instanceFieldsShouldUseCamelCase() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      List<Field> instanceFields =
          Arrays.stream(clazz.getDeclaredFields())
              .filter(f -> !Modifier.isStatic(f.getModifiers()))
              .collect(Collectors.toList());

      for (Field field : instanceFields) {
        String name = field.getName();
        assertTrue(
            Character.isLowerCase(name.charAt(0)),
            "Instance field " + name + " should start with lowercase (camelCase)");
      }
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("State field should be volatile for thread safety")
    void stateFieldShouldBeVolatile() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("state");
      assertTrue(
          Modifier.isVolatile(field.getModifiers()), "state should be volatile for thread safety");
    }

    @Test
    @DisplayName("Closed field should be volatile for thread safety")
    void closedFieldShouldBeVolatile() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();
      Field field = clazz.getDeclaredField("closed");
      assertTrue(
          Modifier.isVolatile(field.getModifiers()), "closed should be volatile for thread safety");
    }

    @Test
    @DisplayName("Cached fields should be volatile for thread safety")
    void cachedFieldsShouldBeVolatile() throws ClassNotFoundException, NoSuchFieldException {
      Class<?> clazz = getTestedClass();

      Field cachedFunctions = clazz.getDeclaredField("cachedExportedFunctions");
      assertTrue(
          Modifier.isVolatile(cachedFunctions.getModifiers()),
          "cachedExportedFunctions should be volatile");

      Field cachedInterfaces = clazz.getDeclaredField("cachedExportedInterfaces");
      assertTrue(
          Modifier.isVolatile(cachedInterfaces.getModifiers()),
          "cachedExportedInterfaces should be volatile");
    }
  }

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("Optional returning methods should return Optional")
    void optionalReturningMethodsShouldReturnOptional() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      String[] optionalMethods = {"getLastActivityAt", "getProperty", "getResource"};

      for (String methodName : optionalMethods) {
        for (Method method : clazz.getDeclaredMethods()) {
          if (method.getName().equals(methodName)) {
            assertEquals(
                Optional.class, method.getReturnType(), methodName + " should return Optional");
            break;
          }
        }
      }
    }

    @Test
    @DisplayName("Collection returning methods should return appropriate collection types")
    void collectionReturningMethodsShouldReturnCollections() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      String[] listMethods = {"getExportedFunctions", "getExportedInterfaces", "getResources"};

      for (String methodName : listMethods) {
        for (Method method : clazz.getDeclaredMethods()) {
          if (method.getName().equals(methodName)) {
            assertEquals(List.class, method.getReturnType(), methodName + " should return List");
            break;
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("Annotation Tests")
  class AnnotationTests {

    @Test
    @DisplayName("Class should not have deprecated annotation")
    void classShouldNotBeDeprecated() throws ClassNotFoundException {
      Class<?> clazz = getTestedClass();
      assertFalse(
          clazz.isAnnotationPresent(Deprecated.class), "JniWasiInstance should not be deprecated");
    }
  }
}
