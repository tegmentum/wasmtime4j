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

package ai.tegmentum.wasmtime4j.panama.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Panama WASI-NN (Neural Network) implementation classes.
 *
 * <p>These tests verify the class structure and API contracts of the Panama WASI-NN implementation
 * using reflection to avoid triggering native library loading.
 */
@DisplayName("Panama WASI-NN Implementation Tests")
class PanamaWasiNnTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiNnTest.class.getName());

  private static final String NN_CONTEXT_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.nn.PanaNnContext";
  private static final String NN_CONTEXT_FACTORY_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.nn.PanaNnContextFactory";
  private static final String NN_GRAPH_CLASS = "ai.tegmentum.wasmtime4j.panama.wasi.nn.PanaNnGraph";
  private static final String NN_GRAPH_EXEC_CONTEXT_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.nn.PanaNnGraphExecutionContext";

  /**
   * Loads a class without initializing it.
   *
   * @param className the fully qualified class name
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit(final String className) throws ClassNotFoundException {
    return Class.forName(className, false, getClass().getClassLoader());
  }

  @Nested
  @DisplayName("PanaNnContext Class Structure Tests")
  class NnContextClassStructureTests {

    @Test
    @DisplayName("PanaNnContext class should exist and be public final")
    void contextClassShouldExistAndBePublicFinal() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_CLASS);

      assertNotNull(clazz, "PanaNnContext class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      LOGGER.info("PanaNnContext class exists and is public final");
    }

    @Test
    @DisplayName("PanaNnContext should implement NnContext interface")
    void contextShouldImplementInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_CLASS);
      final Class<?>[] interfaces = clazz.getInterfaces();

      final Set<String> interfaceNames =
          Arrays.stream(interfaces).map(Class::getName).collect(Collectors.toSet());

      assertTrue(
          interfaceNames.contains("ai.tegmentum.wasmtime4j.wasi.nn.NnContext"),
          "Should implement NnContext interface");
      LOGGER.info("PanaNnContext implements NnContext interface");
    }

    @Test
    @DisplayName("PanaNnContext should have graph loading methods")
    void contextShouldHaveGraphLoadingMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("loadGraph");
      requiredMethods.add("loadGraphFromFile");
      requiredMethods.add("loadGraphByName");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(declaredMethods.contains(method), "PanaNnContext should have method: " + method);
      }
      LOGGER.info("PanaNnContext has graph loading methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanaNnContext should have capability query methods")
    void contextShouldHaveCapabilityMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("getSupportedEncodings");
      requiredMethods.add("getSupportedTargets");
      requiredMethods.add("isEncodingSupported");
      requiredMethods.add("isTargetSupported");
      requiredMethods.add("isAvailable");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(declaredMethods.contains(method), "PanaNnContext should have method: " + method);
      }
      LOGGER.info("PanaNnContext has capability query methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanaNnContext should have native handle field")
    void contextShouldHaveNativeHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_CLASS);

      boolean hasNativeHandle = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("nativeHandle")
            && field.getType().getName().equals("java.lang.foreign.MemorySegment")) {
          hasNativeHandle = true;
          break;
        }
      }
      assertTrue(hasNativeHandle, "Should have nativeHandle field of type MemorySegment");
      LOGGER.info("PanaNnContext has nativeHandle field");
    }

    @Test
    @DisplayName("PanaNnContext should have NativeResourceHandle for lifecycle management")
    void contextShouldHaveResourceHandle() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_CLASS);

      boolean hasResourceHandle = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("resourceHandle")
            && field.getType().getSimpleName().equals("NativeResourceHandle")) {
          hasResourceHandle = true;
          break;
        }
      }
      assertTrue(
          hasResourceHandle, "Should have resourceHandle field of type NativeResourceHandle");
      LOGGER.info("PanaNnContext has NativeResourceHandle for lifecycle management");
    }

    @Test
    @DisplayName("PanaNnContext should have logger field")
    void contextShouldHaveLoggerField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_CLASS);

      boolean hasLogger = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("LOGGER")
            && field.getType().equals(Logger.class)
            && Modifier.isStatic(field.getModifiers())
            && Modifier.isFinal(field.getModifiers())) {
          hasLogger = true;
          break;
        }
      }
      assertTrue(hasLogger, "Should have static final LOGGER field");
      LOGGER.info("PanaNnContext has LOGGER field");
    }
  }

  @Nested
  @DisplayName("PanaNnContextFactory Class Structure Tests")
  class NnContextFactoryClassStructureTests {

    @Test
    @DisplayName("PanaNnContextFactory class should exist and be public final")
    void factoryClassShouldExistAndBePublicFinal() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_FACTORY_CLASS);

      assertNotNull(clazz, "PanaNnContextFactory class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      LOGGER.info("PanaNnContextFactory class exists and is public final");
    }

    @Test
    @DisplayName("PanaNnContextFactory should implement NnContextFactory interface")
    void factoryShouldImplementInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_FACTORY_CLASS);
      final Class<?>[] interfaces = clazz.getInterfaces();

      final Set<String> interfaceNames =
          Arrays.stream(interfaces).map(Class::getName).collect(Collectors.toSet());

      assertTrue(
          interfaceNames.contains("ai.tegmentum.wasmtime4j.wasi.nn.NnContextFactory"),
          "Should implement NnContextFactory interface");
      LOGGER.info("PanaNnContextFactory implements NnContextFactory interface");
    }

    @Test
    @DisplayName("PanaNnContextFactory should have required factory methods")
    void factoryShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_FACTORY_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("createNnContext");
      requiredMethods.add("isNnAvailable");
      requiredMethods.add("getDefaultExecutionTarget");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method), "PanaNnContextFactory should have method: " + method);
      }
      LOGGER.info("PanaNnContextFactory has required methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanaNnContextFactory should have default constructor")
    void factoryShouldHaveDefaultConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_FACTORY_CLASS);

      boolean hasDefaultConstructor = false;
      for (final var constructor : clazz.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
          hasDefaultConstructor = true;
          break;
        }
      }
      assertTrue(hasDefaultConstructor, "Should have public default constructor");
      LOGGER.info("PanaNnContextFactory has default constructor");
    }
  }

  @Nested
  @DisplayName("PanaNnGraph Class Structure Tests")
  class NnGraphClassStructureTests {

    @Test
    @DisplayName("PanaNnGraph class should exist and be public final")
    void graphClassShouldExistAndBePublicFinal() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_CLASS);

      assertNotNull(clazz, "PanaNnGraph class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      LOGGER.info("PanaNnGraph class exists and is public final");
    }

    @Test
    @DisplayName("PanaNnGraph should implement NnGraph interface")
    void graphShouldImplementInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_CLASS);
      final Class<?>[] interfaces = clazz.getInterfaces();

      final Set<String> interfaceNames =
          Arrays.stream(interfaces).map(Class::getName).collect(Collectors.toSet());

      assertTrue(
          interfaceNames.contains("ai.tegmentum.wasmtime4j.wasi.nn.NnGraph"),
          "Should implement NnGraph interface");
      LOGGER.info("PanaNnGraph implements NnGraph interface");
    }

    @Test
    @DisplayName("PanaNnGraph should have required graph methods")
    void graphShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("getNativeHandle");
      requiredMethods.add("getEncoding");
      requiredMethods.add("getExecutionTarget");
      requiredMethods.add("createExecutionContext");
      requiredMethods.add("isValid");
      requiredMethods.add("getModelName");
      requiredMethods.add("close");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(declaredMethods.contains(method), "PanaNnGraph should have method: " + method);
      }
      LOGGER.info("PanaNnGraph has required methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanaNnGraph should have native handle field")
    void graphShouldHaveNativeHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_CLASS);

      boolean hasNativeHandle = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("nativeHandle")
            && field.getType().getName().equals("java.lang.foreign.MemorySegment")) {
          hasNativeHandle = true;
          break;
        }
      }
      assertTrue(hasNativeHandle, "Should have nativeHandle field of type MemorySegment");
      LOGGER.info("PanaNnGraph has nativeHandle field");
    }

    @Test
    @DisplayName("PanaNnGraph should have encoding and target fields")
    void graphShouldHaveEncodingAndTargetFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_CLASS);

      boolean hasEncoding = false;
      boolean hasTarget = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("encoding")) {
          hasEncoding = true;
        }
        if (field.getName().equals("executionTarget")) {
          hasTarget = true;
        }
      }
      assertTrue(hasEncoding, "Should have encoding field");
      assertTrue(hasTarget, "Should have executionTarget field");
      LOGGER.info("PanaNnGraph has encoding and executionTarget fields");
    }
  }

  @Nested
  @DisplayName("PanaNnGraphExecutionContext Class Structure Tests")
  class NnGraphExecutionContextClassStructureTests {

    @Test
    @DisplayName("PanaNnGraphExecutionContext class should exist and be public final")
    void execContextClassShouldExistAndBePublicFinal() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_EXEC_CONTEXT_CLASS);

      assertNotNull(clazz, "PanaNnGraphExecutionContext class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      LOGGER.info("PanaNnGraphExecutionContext class exists and is public final");
    }

    @Test
    @DisplayName("PanaNnGraphExecutionContext should implement NnGraphExecutionContext interface")
    void execContextShouldImplementInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_EXEC_CONTEXT_CLASS);
      final Class<?>[] interfaces = clazz.getInterfaces();

      final Set<String> interfaceNames =
          Arrays.stream(interfaces).map(Class::getName).collect(Collectors.toSet());

      assertTrue(
          interfaceNames.contains("ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext"),
          "Should implement NnGraphExecutionContext interface");
      LOGGER.info("PanaNnGraphExecutionContext implements NnGraphExecutionContext interface");
    }

    @Test
    @DisplayName("PanaNnGraphExecutionContext should have compute methods")
    void execContextShouldHaveComputeMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_EXEC_CONTEXT_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("compute");
      requiredMethods.add("computeByIndex");
      requiredMethods.add("computeNoInputs");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method),
            "PanaNnGraphExecutionContext should have method: " + method);
      }
      LOGGER.info("PanaNnGraphExecutionContext has compute methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanaNnGraphExecutionContext should have input/output methods")
    void execContextShouldHaveInputOutputMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_EXEC_CONTEXT_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("setInput");
      requiredMethods.add("getOutput");
      requiredMethods.add("getInputCount");
      requiredMethods.add("getOutputCount");
      requiredMethods.add("getInputMetadata");
      requiredMethods.add("getOutputMetadata");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method),
            "PanaNnGraphExecutionContext should have method: " + method);
      }
      LOGGER.info("PanaNnGraphExecutionContext has input/output methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanaNnGraphExecutionContext should have native handle field")
    void execContextShouldHaveNativeHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_EXEC_CONTEXT_CLASS);

      boolean hasNativeHandle = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("nativeHandle")
            && field.getType().getName().equals("java.lang.foreign.MemorySegment")) {
          hasNativeHandle = true;
          break;
        }
      }
      assertTrue(hasNativeHandle, "Should have nativeHandle field of type MemorySegment");
      LOGGER.info("PanaNnGraphExecutionContext has nativeHandle field");
    }

    @Test
    @DisplayName("PanaNnGraphExecutionContext should have graph reference")
    void execContextShouldHaveGraphReference() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_EXEC_CONTEXT_CLASS);

      boolean hasGraph = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("graph")) {
          hasGraph = true;
          break;
        }
      }
      assertTrue(hasGraph, "Should have graph field");
      LOGGER.info("PanaNnGraphExecutionContext has graph field");
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("PanaNnContext should implement AutoCloseable pattern")
    void contextShouldImplementAutoCloseablePattern() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_CLASS);

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(declaredMethods.contains("close"), "PanaNnContext should have close method");
      assertTrue(declaredMethods.contains("isValid"), "PanaNnContext should have isValid method");
      LOGGER.info("PanaNnContext implements AutoCloseable pattern");
    }

    @Test
    @DisplayName("PanaNnGraph should implement AutoCloseable pattern")
    void graphShouldImplementAutoCloseablePattern() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_CLASS);

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(declaredMethods.contains("close"), "PanaNnGraph should have close method");
      assertTrue(declaredMethods.contains("isValid"), "PanaNnGraph should have isValid method");
      LOGGER.info("PanaNnGraph implements AutoCloseable pattern");
    }

    @Test
    @DisplayName("PanaNnGraphExecutionContext should implement AutoCloseable pattern")
    void execContextShouldImplementAutoCloseablePattern() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_EXEC_CONTEXT_CLASS);

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(
          declaredMethods.contains("close"),
          "PanaNnGraphExecutionContext should have close method");
      assertTrue(
          declaredMethods.contains("isValid"),
          "PanaNnGraphExecutionContext should have isValid method");
      LOGGER.info("PanaNnGraphExecutionContext implements AutoCloseable pattern");
    }
  }

  @Nested
  @DisplayName("Panama FFI Pattern Tests")
  class PanamaFfiPatternTests {

    @Test
    @DisplayName("NN classes should have NativeResourceHandle for thread-safe closure")
    void nnClassesShouldHaveResourceHandle() throws ClassNotFoundException {
      final String[] classes = {NN_CONTEXT_CLASS, NN_GRAPH_CLASS, NN_GRAPH_EXEC_CONTEXT_CLASS};

      for (final String className : classes) {
        final Class<?> clazz = loadClassWithoutInit(className);

        boolean hasResourceHandle = false;
        for (final Field field : clazz.getDeclaredFields()) {
          if (field.getName().equals("resourceHandle")
              && field.getType().getSimpleName().equals("NativeResourceHandle")) {
            hasResourceHandle = true;
            break;
          }
        }
        assertTrue(
            hasResourceHandle,
            className + " should have resourceHandle field of type NativeResourceHandle");
      }
      LOGGER.info("NN classes have NativeResourceHandle for thread-safe closure");
    }

    @Test
    @DisplayName("NN classes should have logger fields")
    void nnClassesShouldHaveLoggerFields() throws ClassNotFoundException {
      final String[] classes = {
        NN_CONTEXT_CLASS, NN_CONTEXT_FACTORY_CLASS, NN_GRAPH_CLASS, NN_GRAPH_EXEC_CONTEXT_CLASS
      };

      for (final String className : classes) {
        final Class<?> clazz = loadClassWithoutInit(className);

        boolean hasLogger = false;
        for (final Field field : clazz.getDeclaredFields()) {
          if (field.getName().equals("LOGGER")
              && field.getType().equals(Logger.class)
              && Modifier.isStatic(field.getModifiers())) {
            hasLogger = true;
            break;
          }
        }
        assertTrue(hasLogger, className + " should have LOGGER field");
      }
      LOGGER.info("NN classes have LOGGER fields");
    }

    @Test
    @DisplayName("PanaNnGraphExecutionContext should have constants for limits")
    void execContextShouldHaveConstants() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_EXEC_CONTEXT_CLASS);

      boolean hasMaxDimensions = false;
      boolean hasMaxOutputSize = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("MAX_DIMENSIONS")
            && Modifier.isStatic(field.getModifiers())
            && Modifier.isFinal(field.getModifiers())) {
          hasMaxDimensions = true;
        }
        if (field.getName().equals("MAX_OUTPUT_SIZE")
            && Modifier.isStatic(field.getModifiers())
            && Modifier.isFinal(field.getModifiers())) {
          hasMaxOutputSize = true;
        }
      }
      assertTrue(hasMaxDimensions, "Should have MAX_DIMENSIONS constant");
      assertTrue(hasMaxOutputSize, "Should have MAX_OUTPUT_SIZE constant");
      LOGGER.info("PanaNnGraphExecutionContext has size limit constants");
    }
  }

  @Nested
  @DisplayName("Helper Method Tests")
  class HelperMethodTests {

    @Test
    @DisplayName("PanaNnGraph should have getNativeSegment helper method")
    void graphShouldHaveNativeSegmentHelper() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_GRAPH_CLASS);

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(
          declaredMethods.contains("getNativeSegment"),
          "PanaNnGraph should have getNativeSegment method");
      LOGGER.info("PanaNnGraph has getNativeSegment helper method");
    }

    @Test
    @DisplayName("NN classes should have ensureNotClosed helper method")
    void nnClassesShouldHaveEnsureNotClosed() throws ClassNotFoundException {
      final String[] classes = {NN_CONTEXT_CLASS, NN_GRAPH_CLASS, NN_GRAPH_EXEC_CONTEXT_CLASS};

      for (final String className : classes) {
        final Class<?> clazz = loadClassWithoutInit(className);

        final Set<String> declaredMethods =
            Arrays.stream(clazz.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(
            declaredMethods.contains("ensureNotClosed"),
            className + " should have ensureNotClosed method");
      }
      LOGGER.info("NN classes have ensureNotClosed helper method");
    }

    @Test
    @DisplayName("PanaNnContext should have getImplementationInfo method")
    void contextShouldHaveImplementationInfoMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NN_CONTEXT_CLASS);

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(
          declaredMethods.contains("getImplementationInfo"),
          "PanaNnContext should have getImplementationInfo method");
      LOGGER.info("PanaNnContext has getImplementationInfo method");
    }
  }
}
