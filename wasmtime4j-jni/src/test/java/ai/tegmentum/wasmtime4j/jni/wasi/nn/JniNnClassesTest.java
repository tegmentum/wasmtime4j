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
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for JNI Neural Network implementation classes.
 *
 * <p>This test class verifies the structure and API conformance of the JNI-based WASI-NN
 * implementation classes including JniNnContext, JniNnContextFactory, JniNnGraph, and
 * JniNnGraphExecutionContext.
 */
@DisplayName("JNI Neural Network Classes Tests")
class JniNnClassesTest {

  @Nested
  @DisplayName("JniNnContext Tests")
  class JniNnContextTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(JniNnContext.class.getModifiers()), "JniNnContext should be public");
      assertTrue(
          Modifier.isFinal(JniNnContext.class.getModifiers()), "JniNnContext should be final");
    }

    @Test
    @DisplayName("should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniNnContext.class),
          "JniNnContext should extend JniResource");
    }

    @Test
    @DisplayName("should implement NnContext interface")
    void shouldImplementNnContextInterface() {
      assertTrue(
          NnContext.class.isAssignableFrom(JniNnContext.class),
          "JniNnContext should implement NnContext");
    }

    @Test
    @DisplayName("should have package-private constructor with nativeHandle")
    void shouldHaveConstructorWithNativeHandle() throws NoSuchMethodException {
      final Constructor<?> constructor = JniNnContext.class.getDeclaredConstructor(long.class);
      assertNotNull(constructor, "Constructor with nativeHandle should exist");
      assertTrue(
          !Modifier.isPublic(constructor.getModifiers())
              && !Modifier.isProtected(constructor.getModifiers()),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("should have loadGraph method")
    void shouldHaveLoadGraphMethod() throws NoSuchMethodException {
      final Method method =
          JniNnContext.class.getMethod(
              "loadGraph", byte[].class, NnGraphEncoding.class, NnExecutionTarget.class);
      assertNotNull(method, "loadGraph method should exist");
      assertEquals(NnGraph.class, method.getReturnType(), "Should return NnGraph");
    }

    @Test
    @DisplayName("should have loadGraphFromFile method")
    void shouldHaveLoadGraphFromFileMethod() throws NoSuchMethodException {
      final Method method =
          JniNnContext.class.getMethod(
              "loadGraphFromFile", String.class, NnGraphEncoding.class, NnExecutionTarget.class);
      assertNotNull(method, "loadGraphFromFile method should exist");
      assertEquals(NnGraph.class, method.getReturnType(), "Should return NnGraph");
    }

    @Test
    @DisplayName("should have loadGraphByName method")
    void shouldHaveLoadGraphByNameMethod() throws NoSuchMethodException {
      final Method method =
          JniNnContext.class.getMethod(
              "loadGraphByName", String.class, NnGraphEncoding.class, NnExecutionTarget.class);
      assertNotNull(method, "loadGraphByName method should exist");
      assertEquals(NnGraph.class, method.getReturnType(), "Should return NnGraph");
    }

    @Test
    @DisplayName("should have getSupportedEncodings method")
    void shouldHaveGetSupportedEncodingsMethod() throws NoSuchMethodException {
      final Method method = JniNnContext.class.getMethod("getSupportedEncodings");
      assertNotNull(method, "getSupportedEncodings method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getSupportedTargets method")
    void shouldHaveGetSupportedTargetsMethod() throws NoSuchMethodException {
      final Method method = JniNnContext.class.getMethod("getSupportedTargets");
      assertNotNull(method, "getSupportedTargets method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniNnContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("JniNnContextFactory Tests")
  class JniNnContextFactoryTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(JniNnContextFactory.class.getModifiers()),
          "JniNnContextFactory should be public");
      assertTrue(
          Modifier.isFinal(JniNnContextFactory.class.getModifiers()),
          "JniNnContextFactory should be final");
    }

    @Test
    @DisplayName("should implement NnContextFactory interface")
    void shouldImplementNnContextFactoryInterface() {
      assertTrue(
          NnContextFactory.class.isAssignableFrom(JniNnContextFactory.class),
          "JniNnContextFactory should implement NnContextFactory");
    }

    @Test
    @DisplayName("should have public default constructor")
    void shouldHavePublicDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = JniNnContextFactory.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have createNnContext method")
    void shouldHaveCreateNnContextMethod() throws NoSuchMethodException {
      final Method method = JniNnContextFactory.class.getMethod("createNnContext");
      assertNotNull(method, "createNnContext method should exist");
      assertEquals(NnContext.class, method.getReturnType(), "Should return NnContext");
    }

    @Test
    @DisplayName("should have isNnAvailable method")
    void shouldHaveIsNnAvailableMethod() throws NoSuchMethodException {
      final Method method = JniNnContextFactory.class.getMethod("isNnAvailable");
      assertNotNull(method, "isNnAvailable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getDefaultExecutionTarget method")
    void shouldHaveGetDefaultExecutionTargetMethod() throws NoSuchMethodException {
      final Method method = JniNnContextFactory.class.getMethod("getDefaultExecutionTarget");
      assertNotNull(method, "getDefaultExecutionTarget method should exist");
      assertEquals(
          NnExecutionTarget.class, method.getReturnType(), "Should return NnExecutionTarget");
    }
  }

  @Nested
  @DisplayName("JniNnGraph Tests")
  class JniNnGraphTests {

    private Class<?> getJniNnGraphClass() throws ClassNotFoundException {
      return Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.nn.JniNnGraph");
    }

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = getJniNnGraphClass();
      assertTrue(
          !Modifier.isPublic(clazz.getModifiers())
              && !Modifier.isProtected(clazz.getModifiers())
              && !Modifier.isPrivate(clazz.getModifiers()),
          "JniNnGraph should be package-private");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniNnGraph should be final");
    }

    @Test
    @DisplayName("should extend JniResource")
    void shouldExtendJniResource() throws ClassNotFoundException {
      final Class<?> clazz = getJniNnGraphClass();
      assertTrue(JniResource.class.isAssignableFrom(clazz), "JniNnGraph should extend JniResource");
    }

    @Test
    @DisplayName("should implement NnGraph interface")
    void shouldImplementNnGraphInterface() throws ClassNotFoundException {
      final Class<?> clazz = getJniNnGraphClass();
      assertTrue(NnGraph.class.isAssignableFrom(clazz), "JniNnGraph should implement NnGraph");
    }

    @Test
    @DisplayName("should have constructor with nativeHandle, encoding, and executionTarget")
    void shouldHaveConstructorWithParameters()
        throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = getJniNnGraphClass();
      final Constructor<?> constructor =
          clazz.getDeclaredConstructor(long.class, NnGraphEncoding.class, NnExecutionTarget.class);
      assertNotNull(constructor, "Constructor should exist");
    }

    @Test
    @DisplayName("should have getEncoding method")
    void shouldHaveGetEncodingMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = getJniNnGraphClass();
      final Method method = clazz.getMethod("getEncoding");
      assertNotNull(method, "getEncoding method should exist");
      assertEquals(NnGraphEncoding.class, method.getReturnType(), "Should return NnGraphEncoding");
    }

    @Test
    @DisplayName("should have getExecutionTarget method")
    void shouldHaveGetExecutionTargetMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = getJniNnGraphClass();
      final Method method = clazz.getMethod("getExecutionTarget");
      assertNotNull(method, "getExecutionTarget method should exist");
      assertEquals(
          NnExecutionTarget.class, method.getReturnType(), "Should return NnExecutionTarget");
    }

    @Test
    @DisplayName("should have createExecutionContext method")
    void shouldHaveCreateExecutionContextMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = getJniNnGraphClass();
      final Method method = clazz.getMethod("createExecutionContext");
      assertNotNull(method, "createExecutionContext method should exist");
      assertEquals(
          NnGraphExecutionContext.class,
          method.getReturnType(),
          "Should return NnGraphExecutionContext");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = getJniNnGraphClass();
      final Method method = clazz.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getModelName method")
    void shouldHaveGetModelNameMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = getJniNnGraphClass();
      final Method method = clazz.getMethod("getModelName");
      assertNotNull(method, "getModelName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws ClassNotFoundException, NoSuchMethodException {
      final Class<?> clazz = getJniNnGraphClass();
      final Method method = clazz.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("JniNnGraphExecutionContext Tests")
  class JniNnGraphExecutionContextTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(JniNnGraphExecutionContext.class.getModifiers()),
          "JniNnGraphExecutionContext should be public");
      assertTrue(
          Modifier.isFinal(JniNnGraphExecutionContext.class.getModifiers()),
          "JniNnGraphExecutionContext should be final");
    }

    @Test
    @DisplayName("should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniNnGraphExecutionContext.class),
          "JniNnGraphExecutionContext should extend JniResource");
    }

    @Test
    @DisplayName("should implement NnGraphExecutionContext interface")
    void shouldImplementNnGraphExecutionContextInterface() {
      assertTrue(
          NnGraphExecutionContext.class.isAssignableFrom(JniNnGraphExecutionContext.class),
          "JniNnGraphExecutionContext should implement NnGraphExecutionContext");
    }

    @Test
    @DisplayName("should have package-private constructor with nativeHandle and graph")
    void shouldHaveConstructorWithParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          JniNnGraphExecutionContext.class.getDeclaredConstructor(long.class, NnGraph.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(
          !Modifier.isPublic(constructor.getModifiers())
              && !Modifier.isProtected(constructor.getModifiers()),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("should have getGraph method")
    void shouldHaveGetGraphMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("getGraph");
      assertNotNull(method, "getGraph method should exist");
      assertEquals(NnGraph.class, method.getReturnType(), "Should return NnGraph");
    }

    @Test
    @DisplayName("should have compute method with List parameter")
    void shouldHaveComputeWithListMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("compute", List.class);
      assertNotNull(method, "compute method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have computeByIndex method")
    void shouldHaveComputeByIndexMethod() throws NoSuchMethodException {
      final Method method =
          JniNnGraphExecutionContext.class.getMethod("computeByIndex", NnTensor[].class);
      assertNotNull(method, "computeByIndex method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have setInput by index method")
    void shouldHaveSetInputByIndexMethod() throws NoSuchMethodException {
      final Method method =
          JniNnGraphExecutionContext.class.getMethod("setInput", int.class, NnTensor.class);
      assertNotNull(method, "setInput by index method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setInput by name method")
    void shouldHaveSetInputByNameMethod() throws NoSuchMethodException {
      final Method method =
          JniNnGraphExecutionContext.class.getMethod("setInput", String.class, NnTensor.class);
      assertNotNull(method, "setInput by name method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have computeNoInputs method")
    void shouldHaveComputeNoInputsMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("computeNoInputs");
      assertNotNull(method, "computeNoInputs method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getOutput by index method")
    void shouldHaveGetOutputByIndexMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("getOutput", int.class);
      assertNotNull(method, "getOutput by index method should exist");
      assertEquals(NnTensor.class, method.getReturnType(), "Should return NnTensor");
    }

    @Test
    @DisplayName("should have getOutput by name method")
    void shouldHaveGetOutputByNameMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("getOutput", String.class);
      assertNotNull(method, "getOutput by name method should exist");
      assertEquals(NnTensor.class, method.getReturnType(), "Should return NnTensor");
    }

    @Test
    @DisplayName("should have getInputCount method")
    void shouldHaveGetInputCountMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("getInputCount");
      assertNotNull(method, "getInputCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getOutputCount method")
    void shouldHaveGetOutputCountMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("getOutputCount");
      assertNotNull(method, "getOutputCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getInputMetadata method")
    void shouldHaveGetInputMetadataMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("getInputMetadata");
      assertNotNull(method, "getInputMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getOutputMetadata method")
    void shouldHaveGetOutputMetadataMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("getOutputMetadata");
      assertNotNull(method, "getOutputMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniNnGraphExecutionContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
