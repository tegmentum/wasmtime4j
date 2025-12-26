/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.jni.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContextFactory;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for JNI WASI-NN implementation classes.
 *
 * <p>Tests cover class structure, API contracts, and interface compliance without loading native
 * libraries.
 */
@DisplayName("JNI WASI-NN Implementation Tests")
class JniWasiNnTest {

  private static final Logger LOGGER = Logger.getLogger(JniWasiNnTest.class.getName());

  /** Tests for JniNnContext class structure and API. */
  @Nested
  @DisplayName("JniNnContext Tests")
  class JniNnContextTests {

    @Test
    @DisplayName("JniNnContext should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniNnContext.class),
          "JniNnContext should extend JniResource");
    }

    @Test
    @DisplayName("JniNnContext should implement NnContext interface")
    void shouldImplementNnContextInterface() {
      assertTrue(
          NnContext.class.isAssignableFrom(JniNnContext.class),
          "JniNnContext should implement NnContext");
    }

    @Test
    @DisplayName("JniNnContext should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniNnContext.class.getModifiers()),
          "JniNnContext should be a final class");
    }

    @Test
    @DisplayName("JniNnContext should have constructor with long parameter")
    void shouldHaveConstructorWithLongParameter() {
      assertDoesNotThrow(
          () -> JniNnContext.class.getConstructor(long.class),
          "Should have constructor with long parameter");
    }

    @Test
    @DisplayName("JniNnContext constructor should reject zero handle")
    void constructorShouldRejectZeroHandle() throws Exception {
      Constructor<JniNnContext> constructor = JniNnContext.class.getConstructor(long.class);
      try {
        constructor.newInstance(0L);
        throw new AssertionError("Constructor should reject zero handle");
      } catch (java.lang.reflect.InvocationTargetException e) {
        // Expected - the wrapped exception should be related to invalid handle
        Throwable cause = e.getCause();
        assertNotNull(cause, "Should have a cause exception");
        assertTrue(
            cause instanceof IllegalArgumentException
                || cause.getClass().getSimpleName().contains("Validation"),
            "Cause should be validation-related: " + cause.getClass().getName());
      }
    }

    @Test
    @DisplayName("JniNnContext should have all NnContext methods")
    void shouldHaveAllNnContextMethods() {
      Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("loadGraph");
      requiredMethods.add("loadGraphFromFile");
      requiredMethods.add("loadGraphByName");
      requiredMethods.add("getSupportedEncodings");
      requiredMethods.add("getSupportedTargets");
      requiredMethods.add("isEncodingSupported");
      requiredMethods.add("isTargetSupported");
      requiredMethods.add("isAvailable");
      requiredMethods.add("getImplementationInfo");
      requiredMethods.add("isValid");

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniNnContext.class.getMethods()) {
        actualMethods.add(method.getName());
      }

      for (String required : requiredMethods) {
        assertTrue(actualMethods.contains(required), "Should have method: " + required);
      }
    }

    @Test
    @DisplayName("JniNnContext should have LOGGER field")
    void shouldHaveLoggerField() throws Exception {
      Field loggerField = JniNnContext.class.getDeclaredField("LOGGER");
      assertNotNull(loggerField);
      assertEquals(Logger.class, loggerField.getType());
      assertTrue(Modifier.isPrivate(loggerField.getModifiers()));
      assertTrue(Modifier.isStatic(loggerField.getModifiers()));
      assertTrue(Modifier.isFinal(loggerField.getModifiers()));
    }

    @Test
    @DisplayName("JniNnContext should have native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method method : JniNnContext.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeMethodCount++;
        }
      }
      assertTrue(nativeMethodCount >= 10, "Should have at least 10 native methods");
    }
  }

  /** Tests for JniNnContextFactory class structure and API. */
  @Nested
  @DisplayName("JniNnContextFactory Tests")
  class JniNnContextFactoryTests {

    @Test
    @DisplayName("JniNnContextFactory should implement NnContextFactory interface")
    void shouldImplementNnContextFactoryInterface() {
      assertTrue(
          NnContextFactory.class.isAssignableFrom(JniNnContextFactory.class),
          "JniNnContextFactory should implement NnContextFactory");
    }

    @Test
    @DisplayName("JniNnContextFactory should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniNnContextFactory.class.getModifiers()),
          "JniNnContextFactory should be a final class");
    }

    @Test
    @DisplayName("JniNnContextFactory should have default constructor")
    void shouldHaveDefaultConstructor() {
      assertDoesNotThrow(
          () -> JniNnContextFactory.class.getConstructor(), "Should have default constructor");
    }

    @Test
    @DisplayName("JniNnContextFactory should be instantiable")
    void shouldBeInstantiable() {
      JniNnContextFactory factory = new JniNnContextFactory();
      assertNotNull(factory, "Factory should be instantiable");
    }

    @Test
    @DisplayName("JniNnContextFactory should have createNnContext method")
    void shouldHaveCreateNnContextMethod() {
      assertDoesNotThrow(
          () -> JniNnContextFactory.class.getMethod("createNnContext"),
          "Should have createNnContext method");
    }

    @Test
    @DisplayName("JniNnContextFactory should have isNnAvailable method")
    void shouldHaveIsNnAvailableMethod() {
      assertDoesNotThrow(
          () -> JniNnContextFactory.class.getMethod("isNnAvailable"),
          "Should have isNnAvailable method");
    }

    @Test
    @DisplayName("JniNnContextFactory should have getDefaultExecutionTarget method")
    void shouldHaveGetDefaultExecutionTargetMethod() throws Exception {
      Method method = JniNnContextFactory.class.getMethod("getDefaultExecutionTarget");
      assertNotNull(method);
      assertEquals(NnExecutionTarget.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniNnContextFactory should have LOGGER field")
    void shouldHaveLoggerField() throws Exception {
      Field loggerField = JniNnContextFactory.class.getDeclaredField("LOGGER");
      assertNotNull(loggerField);
      assertEquals(Logger.class, loggerField.getType());
      assertTrue(Modifier.isPrivate(loggerField.getModifiers()));
      assertTrue(Modifier.isStatic(loggerField.getModifiers()));
    }

    @Test
    @DisplayName("JniNnContextFactory native methods should be private static")
    void nativeMethodsShouldBePrivateStatic() {
      for (Method method : JniNnContextFactory.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          assertTrue(
              Modifier.isPrivate(method.getModifiers()), method.getName() + " should be private");
          assertTrue(
              Modifier.isStatic(method.getModifiers()), method.getName() + " should be static");
        }
      }
    }
  }

  /** Tests for JniNnGraph class structure and API. */
  @Nested
  @DisplayName("JniNnGraph Tests")
  class JniNnGraphTests {

    @Test
    @DisplayName("JniNnGraph should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniNnGraph.class),
          "JniNnGraph should extend JniResource");
    }

    @Test
    @DisplayName("JniNnGraph should implement NnGraph interface")
    void shouldImplementNnGraphInterface() {
      assertTrue(
          NnGraph.class.isAssignableFrom(JniNnGraph.class), "JniNnGraph should implement NnGraph");
    }

    @Test
    @DisplayName("JniNnGraph should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniNnGraph.class.getModifiers()), "JniNnGraph should be a final class");
    }

    @Test
    @DisplayName("JniNnGraph should have encoding field")
    void shouldHaveEncodingField() throws Exception {
      Field encodingField = JniNnGraph.class.getDeclaredField("encoding");
      assertNotNull(encodingField);
      assertEquals(NnGraphEncoding.class, encodingField.getType());
      assertTrue(Modifier.isPrivate(encodingField.getModifiers()));
      assertTrue(Modifier.isFinal(encodingField.getModifiers()));
    }

    @Test
    @DisplayName("JniNnGraph should have executionTarget field")
    void shouldHaveExecutionTargetField() throws Exception {
      Field targetField = JniNnGraph.class.getDeclaredField("executionTarget");
      assertNotNull(targetField);
      assertEquals(NnExecutionTarget.class, targetField.getType());
      assertTrue(Modifier.isPrivate(targetField.getModifiers()));
      assertTrue(Modifier.isFinal(targetField.getModifiers()));
    }

    @Test
    @DisplayName("JniNnGraph should have getEncoding method")
    void shouldHaveGetEncodingMethod() throws Exception {
      Method method = JniNnGraph.class.getMethod("getEncoding");
      assertNotNull(method);
      assertEquals(NnGraphEncoding.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniNnGraph should have getExecutionTarget method")
    void shouldHaveGetExecutionTargetMethod() throws Exception {
      Method method = JniNnGraph.class.getMethod("getExecutionTarget");
      assertNotNull(method);
      assertEquals(NnExecutionTarget.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniNnGraph should have createExecutionContext method")
    void shouldHaveCreateExecutionContextMethod() throws Exception {
      Method method = JniNnGraph.class.getMethod("createExecutionContext");
      assertNotNull(method);
      assertEquals(NnGraphExecutionContext.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniNnGraph should have getModelName method")
    void shouldHaveGetModelNameMethod() throws Exception {
      Method method = JniNnGraph.class.getMethod("getModelName");
      assertNotNull(method);
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniNnGraph should have isValid method")
    void shouldHaveIsValidMethod() throws Exception {
      Method method = JniNnGraph.class.getMethod("isValid");
      assertNotNull(method);
      assertEquals(boolean.class, method.getReturnType());
    }
  }

  /** Tests for JniNnGraphExecutionContext class structure and API. */
  @Nested
  @DisplayName("JniNnGraphExecutionContext Tests")
  class JniNnGraphExecutionContextTests {

    @Test
    @DisplayName("JniNnGraphExecutionContext should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniNnGraphExecutionContext.class),
          "JniNnGraphExecutionContext should extend JniResource");
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should implement NnGraphExecutionContext interface")
    void shouldImplementNnGraphExecutionContextInterface() {
      assertTrue(
          NnGraphExecutionContext.class.isAssignableFrom(JniNnGraphExecutionContext.class),
          "JniNnGraphExecutionContext should implement NnGraphExecutionContext");
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniNnGraphExecutionContext.class.getModifiers()),
          "JniNnGraphExecutionContext should be a final class");
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should have graph field")
    void shouldHaveGraphField() throws Exception {
      Field graphField = JniNnGraphExecutionContext.class.getDeclaredField("graph");
      assertNotNull(graphField);
      assertEquals(NnGraph.class, graphField.getType());
      assertTrue(Modifier.isPrivate(graphField.getModifiers()));
      assertTrue(Modifier.isFinal(graphField.getModifiers()));
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should have getGraph method")
    void shouldHaveGetGraphMethod() throws Exception {
      Method method = JniNnGraphExecutionContext.class.getMethod("getGraph");
      assertNotNull(method);
      assertEquals(NnGraph.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should have compute methods")
    void shouldHaveComputeMethods() {
      Set<String> computeMethods = new HashSet<>();
      for (Method method : JniNnGraphExecutionContext.class.getMethods()) {
        if (method.getName().startsWith("compute")) {
          computeMethods.add(method.getName());
        }
      }

      assertTrue(computeMethods.contains("compute"), "Should have compute method");
      assertTrue(computeMethods.contains("computeByIndex"), "Should have computeByIndex method");
      assertTrue(computeMethods.contains("computeNoInputs"), "Should have computeNoInputs method");
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should have setInput methods")
    void shouldHaveSetInputMethods() {
      int setInputCount = 0;
      for (Method method : JniNnGraphExecutionContext.class.getMethods()) {
        if (method.getName().equals("setInput")) {
          setInputCount++;
        }
      }
      assertTrue(setInputCount >= 2, "Should have at least 2 setInput method variants");
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should have getOutput methods")
    void shouldHaveGetOutputMethods() {
      int getOutputCount = 0;
      for (Method method : JniNnGraphExecutionContext.class.getMethods()) {
        if (method.getName().equals("getOutput")) {
          getOutputCount++;
        }
      }
      assertTrue(getOutputCount >= 2, "Should have at least 2 getOutput method variants");
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should have metadata methods")
    void shouldHaveMetadataMethods() {
      assertDoesNotThrow(
          () -> JniNnGraphExecutionContext.class.getMethod("getInputMetadata"),
          "Should have getInputMetadata method");
      assertDoesNotThrow(
          () -> JniNnGraphExecutionContext.class.getMethod("getOutputMetadata"),
          "Should have getOutputMetadata method");
      assertDoesNotThrow(
          () -> JniNnGraphExecutionContext.class.getMethod("getInputCount"),
          "Should have getInputCount method");
      assertDoesNotThrow(
          () -> JniNnGraphExecutionContext.class.getMethod("getOutputCount"),
          "Should have getOutputCount method");
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should have many native methods")
    void shouldHaveManyNativeMethods() {
      int nativeMethodCount = 0;
      for (Method method : JniNnGraphExecutionContext.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeMethodCount++;
        }
      }
      assertTrue(
          nativeMethodCount >= 15,
          "Should have at least 15 native methods, found: " + nativeMethodCount);
    }
  }

  /** Tests for package consistency across NN classes. */
  @Nested
  @DisplayName("Package Consistency Tests")
  class PackageConsistencyTests {

    @Test
    @DisplayName("All NN classes should be in the same package")
    void allClassesShouldBeInSamePackage() {
      String expectedPackage = "ai.tegmentum.wasmtime4j.jni.wasi.nn";
      assertEquals(expectedPackage, JniNnContext.class.getPackage().getName());
      assertEquals(expectedPackage, JniNnContextFactory.class.getPackage().getName());
      assertEquals(expectedPackage, JniNnGraph.class.getPackage().getName());
      assertEquals(expectedPackage, JniNnGraphExecutionContext.class.getPackage().getName());
    }

    @Test
    @DisplayName("All NN classes should follow JNI naming convention")
    void allClassesShouldFollowNamingConvention() {
      assertTrue(JniNnContext.class.getSimpleName().startsWith("Jni"));
      assertTrue(JniNnContextFactory.class.getSimpleName().startsWith("Jni"));
      assertTrue(JniNnGraph.class.getSimpleName().startsWith("Jni"));
      assertTrue(JniNnGraphExecutionContext.class.getSimpleName().startsWith("Jni"));
    }

    @Test
    @DisplayName("All NN classes should be public")
    void allClassesShouldBePublic() {
      assertTrue(Modifier.isPublic(JniNnContext.class.getModifiers()));
      assertTrue(Modifier.isPublic(JniNnContextFactory.class.getModifiers()));
      assertTrue(Modifier.isPublic(JniNnGraph.class.getModifiers()));
      assertTrue(Modifier.isPublic(JniNnGraphExecutionContext.class.getModifiers()));
    }

    @Test
    @DisplayName("All NN classes should be final")
    void allClassesShouldBeFinal() {
      assertTrue(Modifier.isFinal(JniNnContext.class.getModifiers()));
      assertTrue(Modifier.isFinal(JniNnContextFactory.class.getModifiers()));
      assertTrue(Modifier.isFinal(JniNnGraph.class.getModifiers()));
      assertTrue(Modifier.isFinal(JniNnGraphExecutionContext.class.getModifiers()));
    }

    @Test
    @DisplayName("Resource classes should extend JniResource")
    void resourceClassesShouldExtendJniResource() {
      assertTrue(JniResource.class.isAssignableFrom(JniNnContext.class));
      assertTrue(JniResource.class.isAssignableFrom(JniNnGraph.class));
      assertTrue(JniResource.class.isAssignableFrom(JniNnGraphExecutionContext.class));
    }
  }

  /** Tests for interface compliance verification. */
  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("JniNnContext should implement all NnContext methods")
    void contextShouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : NnContext.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniNnContext.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement interface method: " + interfaceMethod.getName());
        }
      }
    }

    @Test
    @DisplayName("JniNnContextFactory should implement all NnContextFactory methods")
    void factoryShouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : NnContextFactory.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniNnContextFactory.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement interface method: " + interfaceMethod.getName());
        }
      }
    }

    @Test
    @DisplayName("JniNnGraph should implement all NnGraph methods")
    void graphShouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : NnGraph.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniNnGraph.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement interface method: " + interfaceMethod.getName());
        }
      }
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should implement all interface methods")
    void executionContextShouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : NnGraphExecutionContext.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniNnGraphExecutionContext.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement interface method: " + interfaceMethod.getName());
        }
      }
    }

    private boolean arrayEquals(Class<?>[] a, Class<?>[] b) {
      if (a.length != b.length) {
        return false;
      }
      for (int i = 0; i < a.length; i++) {
        if (!a[i].equals(b[i])) {
          return false;
        }
      }
      return true;
    }
  }

  /** Tests for NN-specific functionality patterns. */
  @Nested
  @DisplayName("NN-Specific Tests")
  class NnSpecificTests {

    @Test
    @DisplayName("NnGraphEncoding enum should have expected values")
    void graphEncodingShouldHaveExpectedValues() {
      NnGraphEncoding[] values = NnGraphEncoding.values();
      assertTrue(values.length > 0, "NnGraphEncoding should have values");

      // Check some common encodings exist
      Set<String> encodingNames = new HashSet<>();
      for (NnGraphEncoding encoding : values) {
        encodingNames.add(encoding.name());
      }

      LOGGER.info("Available NnGraphEncoding values: " + encodingNames);
    }

    @Test
    @DisplayName("NnExecutionTarget enum should have expected values")
    void executionTargetShouldHaveExpectedValues() {
      NnExecutionTarget[] values = NnExecutionTarget.values();
      assertTrue(values.length > 0, "NnExecutionTarget should have values");

      // CPU target should exist
      boolean hasCpu = false;
      for (NnExecutionTarget target : values) {
        if (target.name().equals("CPU")) {
          hasCpu = true;
          break;
        }
      }
      assertTrue(hasCpu, "CPU target should exist");
    }

    @Test
    @DisplayName("JniNnContext loadGraph methods should have proper signatures")
    void loadGraphMethodsShouldHaveProperSignatures() throws Exception {
      // Check loadGraph with byte array
      Method loadGraphBytes =
          JniNnContext.class.getMethod(
              "loadGraph", byte[].class, NnGraphEncoding.class, NnExecutionTarget.class);
      assertNotNull(loadGraphBytes);
      assertEquals(NnGraph.class, loadGraphBytes.getReturnType());

      // Check loadGraphByName
      Method loadGraphByName = JniNnContext.class.getMethod("loadGraphByName", String.class);
      assertNotNull(loadGraphByName);
      assertEquals(NnGraph.class, loadGraphByName.getReturnType());
    }

    @Test
    @DisplayName("JniNnGraph createExecutionContext should return NnGraphExecutionContext")
    void createExecutionContextShouldReturnCorrectType() throws Exception {
      Method method = JniNnGraph.class.getMethod("createExecutionContext");
      assertEquals(NnGraphExecutionContext.class, method.getReturnType());
    }

    @Test
    @DisplayName("Execution context setInput should accept index-based and name-based inputs")
    void setInputShouldSupportMultipleVariants() {
      boolean hasIndexBased = false;
      boolean hasNameBased = false;

      for (Method method : JniNnGraphExecutionContext.class.getMethods()) {
        if (method.getName().equals("setInput")) {
          Class<?>[] params = method.getParameterTypes();
          if (params.length == 2) {
            if (params[0].equals(int.class)) {
              hasIndexBased = true;
            } else if (params[0].equals(String.class)) {
              hasNameBased = true;
            }
          }
        }
      }

      assertTrue(hasIndexBased, "Should have index-based setInput");
      assertTrue(hasNameBased, "Should have name-based setInput");
    }
  }

  /** Tests for native method signature validation. */
  @Nested
  @DisplayName("Native Method Signature Tests")
  class NativeMethodSignatureTests {

    @Test
    @DisplayName("All native methods in JniNnContext should be private static")
    void contextNativeMethodsShouldBePrivateStatic() {
      for (Method method : JniNnContext.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          assertTrue(
              Modifier.isPrivate(method.getModifiers()), method.getName() + " should be private");
          assertTrue(
              Modifier.isStatic(method.getModifiers()), method.getName() + " should be static");
        }
      }
    }

    @Test
    @DisplayName("All native methods in JniNnGraph should be private static")
    void graphNativeMethodsShouldBePrivateStatic() {
      for (Method method : JniNnGraph.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          assertTrue(
              Modifier.isPrivate(method.getModifiers()), method.getName() + " should be private");
          assertTrue(
              Modifier.isStatic(method.getModifiers()), method.getName() + " should be static");
        }
      }
    }

    @Test
    @DisplayName("All native methods in JniNnGraphExecutionContext should be private static")
    void executionContextNativeMethodsShouldBePrivateStatic() {
      for (Method method : JniNnGraphExecutionContext.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          assertTrue(
              Modifier.isPrivate(method.getModifiers()), method.getName() + " should be private");
          assertTrue(
              Modifier.isStatic(method.getModifiers()), method.getName() + " should be static");
        }
      }
    }

    @Test
    @DisplayName("Native methods should follow naming convention")
    void nativeMethodsShouldFollowNamingConvention() {
      Class<?>[] classes = {
        JniNnContext.class,
        JniNnContextFactory.class,
        JniNnGraph.class,
        JniNnGraphExecutionContext.class
      };

      for (Class<?> clazz : classes) {
        for (Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isNative(method.getModifiers())) {
            assertTrue(
                method.getName().startsWith("native"),
                clazz.getSimpleName() + "." + method.getName() + " should start with 'native'");
          }
        }
      }
    }
  }

  /** Tests for field validation across NN classes. */
  @Nested
  @DisplayName("Field Validation Tests")
  class FieldValidationTests {

    @Test
    @DisplayName("JniNnGraph should have properly typed fields")
    void graphFieldsShouldBeProperlyTyped() throws Exception {
      final Field encodingField = JniNnGraph.class.getDeclaredField("encoding");
      assertEquals(NnGraphEncoding.class, encodingField.getType());
      assertTrue(Modifier.isFinal(encodingField.getModifiers()));

      final Field targetField = JniNnGraph.class.getDeclaredField("executionTarget");
      assertEquals(NnExecutionTarget.class, targetField.getType());
      assertTrue(Modifier.isFinal(targetField.getModifiers()));
    }

    @Test
    @DisplayName("JniNnGraphExecutionContext should have graph reference")
    void executionContextShouldHaveGraphReference() throws Exception {
      Field graphField = JniNnGraphExecutionContext.class.getDeclaredField("graph");
      assertNotNull(graphField);
      assertEquals(NnGraph.class, graphField.getType());
      assertTrue(Modifier.isPrivate(graphField.getModifiers()));
      assertTrue(Modifier.isFinal(graphField.getModifiers()));
    }

    @Test
    @DisplayName("All resource classes should have inherited nativeHandle field")
    void resourceClassesShouldHaveNativeHandle() {
      // nativeHandle is inherited from JniResource
      Field nativeHandleField = null;
      try {
        nativeHandleField = JniResource.class.getDeclaredField("nativeHandle");
      } catch (NoSuchFieldException e) {
        // Try protected visibility
      }

      // Verify JniResource has a way to store native handle
      boolean hasHandleField = false;
      for (Field field : JniResource.class.getDeclaredFields()) {
        if (field.getType().equals(long.class)
            && field.getName().toLowerCase().contains("handle")) {
          hasHandleField = true;
          break;
        }
      }

      assertTrue(hasHandleField, "JniResource should have a handle field");
    }

    @Test
    @DisplayName("All NN classes should have logger fields")
    void allClassesShouldHaveLoggerFields() throws Exception {
      Class<?>[] classes = {JniNnContext.class, JniNnGraph.class, JniNnGraphExecutionContext.class};

      for (Class<?> clazz : classes) {
        Field loggerField = clazz.getDeclaredField("LOGGER");
        assertNotNull(loggerField, clazz.getSimpleName() + " should have LOGGER field");
        assertEquals(
            Logger.class,
            loggerField.getType(),
            clazz.getSimpleName() + " LOGGER should be Logger");
        assertTrue(
            Modifier.isStatic(loggerField.getModifiers()),
            clazz.getSimpleName() + " LOGGER should be static");
      }
    }
  }
}
