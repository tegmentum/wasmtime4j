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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CallGraphAnalysis class.
 *
 * <p>CallGraphAnalysis provides insights into function call relationships and execution patterns.
 * This test verifies the class structure, builder pattern, and nested types.
 */
@DisplayName("CallGraphAnalysis Class Tests")
class CallGraphAnalysisTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(CallGraphAnalysis.class.getModifiers()),
          "CallGraphAnalysis should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CallGraphAnalysis.class.getModifiers()),
          "CallGraphAnalysis should be public");
    }

    @Test
    @DisplayName("should not be abstract")
    void shouldNotBeAbstract() {
      assertTrue(
          !Modifier.isAbstract(CallGraphAnalysis.class.getModifiers()),
          "CallGraphAnalysis should not be abstract");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have builder factory method")
    void shouldHaveBuilderFactoryMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }
  }

  // ========================================================================
  // Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Methods Tests")
  class GetterMethodsTests {

    @Test
    @DisplayName("should have getFunctions method")
    void shouldHaveGetFunctionsMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getFunctions");
      assertNotNull(method, "getFunctions method should exist");
      assertEquals(Map.class, method.getReturnType(), "getFunctions should return Map");
    }

    @Test
    @DisplayName("should have getCallEdges method")
    void shouldHaveGetCallEdgesMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getCallEdges");
      assertNotNull(method, "getCallEdges method should exist");
      assertEquals(List.class, method.getReturnType(), "getCallEdges should return List");
    }

    @Test
    @DisplayName("should have getEntryPoints method")
    void shouldHaveGetEntryPointsMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getEntryPoints");
      assertNotNull(method, "getEntryPoints method should exist");
      assertEquals(Set.class, method.getReturnType(), "getEntryPoints should return Set");
    }

    @Test
    @DisplayName("should have getLeafFunctions method")
    void shouldHaveGetLeafFunctionsMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getLeafFunctions");
      assertNotNull(method, "getLeafFunctions method should exist");
      assertEquals(Set.class, method.getReturnType(), "getLeafFunctions should return Set");
    }

    @Test
    @DisplayName("should have getMaxCallDepth method")
    void shouldHaveGetMaxCallDepthMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getMaxCallDepth");
      assertNotNull(method, "getMaxCallDepth method should exist");
      assertEquals(int.class, method.getReturnType(), "getMaxCallDepth should return int");
    }

    @Test
    @DisplayName("should have getAnalysisTime method")
    void shouldHaveGetAnalysisTimeMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getAnalysisTime");
      assertNotNull(method, "getAnalysisTime method should exist");
      assertEquals(
          Duration.class, method.getReturnType(), "getAnalysisTime should return Duration");
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
    }
  }

  // ========================================================================
  // Query Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Query Methods Tests")
  class QueryMethodsTests {

    @Test
    @DisplayName("should have getCallers method")
    void shouldHaveGetCallersMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getCallers", String.class);
      assertNotNull(method, "getCallers method should exist");
      assertEquals(Set.class, method.getReturnType(), "getCallers should return Set");
    }

    @Test
    @DisplayName("should have getCallees method")
    void shouldHaveGetCalleesMethod() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getCallees", String.class);
      assertNotNull(method, "getCallees method should exist");
      assertEquals(Set.class, method.getReturnType(), "getCallees should return Set");
    }

    @Test
    @DisplayName("should have hasCallPath method")
    void shouldHaveHasCallPathMethod() throws NoSuchMethodException {
      final Method method =
          CallGraphAnalysis.class.getMethod("hasCallPath", String.class, String.class);
      assertNotNull(method, "hasCallPath method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasCallPath should return boolean");
    }

    @Test
    @DisplayName("should have findCallPath method")
    void shouldHaveFindCallPathMethod() throws NoSuchMethodException {
      final Method method =
          CallGraphAnalysis.class.getMethod("findCallPath", String.class, String.class);
      assertNotNull(method, "findCallPath method should exist");
      assertEquals(Optional.class, method.getReturnType(), "findCallPath should return Optional");
    }
  }

  // ========================================================================
  // Builder Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Builder Class Tests")
  class BuilderClassTests {

    @Test
    @DisplayName("should have Builder nested class")
    void shouldHaveBuilderNestedClass() {
      Class<?>[] nestedClasses = CallGraphAnalysis.class.getDeclaredClasses();
      boolean hasBuilder =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("Builder"));
      assertTrue(hasBuilder, "CallGraphAnalysis should have Builder nested class");
    }

    @Test
    @DisplayName("Builder should be public")
    void builderShouldBePublic() throws ClassNotFoundException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
    }

    @Test
    @DisplayName("Builder should be static")
    void builderShouldBeStatic() throws ClassNotFoundException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
    }

    @Test
    @DisplayName("Builder should be final")
    void builderShouldBeFinal() throws ClassNotFoundException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
    }

    @Test
    @DisplayName("Builder should have functions method")
    void builderShouldHaveFunctionsMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      final Method method = builderClass.getMethod("functions", Map.class);
      assertNotNull(method, "functions method should exist");
      assertEquals(builderClass, method.getReturnType(), "functions should return Builder");
    }

    @Test
    @DisplayName("Builder should have callEdges method")
    void builderShouldHaveCallEdgesMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      final Method method = builderClass.getMethod("callEdges", List.class);
      assertNotNull(method, "callEdges method should exist");
      assertEquals(builderClass, method.getReturnType(), "callEdges should return Builder");
    }

    @Test
    @DisplayName("Builder should have entryPoints method")
    void builderShouldHaveEntryPointsMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      final Method method = builderClass.getMethod("entryPoints", Set.class);
      assertNotNull(method, "entryPoints method should exist");
      assertEquals(builderClass, method.getReturnType(), "entryPoints should return Builder");
    }

    @Test
    @DisplayName("Builder should have leafFunctions method")
    void builderShouldHaveLeafFunctionsMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      final Method method = builderClass.getMethod("leafFunctions", Set.class);
      assertNotNull(method, "leafFunctions method should exist");
      assertEquals(builderClass, method.getReturnType(), "leafFunctions should return Builder");
    }

    @Test
    @DisplayName("Builder should have maxCallDepth method")
    void builderShouldHaveMaxCallDepthMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      final Method method = builderClass.getMethod("maxCallDepth", int.class);
      assertNotNull(method, "maxCallDepth method should exist");
      assertEquals(builderClass, method.getReturnType(), "maxCallDepth should return Builder");
    }

    @Test
    @DisplayName("Builder should have analysisTime method")
    void builderShouldHaveAnalysisTimeMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      final Method method = builderClass.getMethod("analysisTime", Duration.class);
      assertNotNull(method, "analysisTime method should exist");
      assertEquals(builderClass, method.getReturnType(), "analysisTime should return Builder");
    }

    @Test
    @DisplayName("Builder should have metrics method")
    void builderShouldHaveMetricsMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      Class<?> metricsClass =
          Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallGraphMetrics");
      final Method method = builderClass.getMethod("metrics", metricsClass);
      assertNotNull(method, "metrics method should exist");
      assertEquals(builderClass, method.getReturnType(), "metrics should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$Builder");
      final Method method = builderClass.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          CallGraphAnalysis.class, method.getReturnType(), "build should return CallGraphAnalysis");
    }
  }

  // ========================================================================
  // FunctionNode Class Tests
  // ========================================================================

  @Nested
  @DisplayName("FunctionNode Class Tests")
  class FunctionNodeClassTests {

    @Test
    @DisplayName("should have FunctionNode nested class")
    void shouldHaveFunctionNodeNestedClass() {
      Class<?>[] nestedClasses = CallGraphAnalysis.class.getDeclaredClasses();
      boolean hasFunctionNode =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("FunctionNode"));
      assertTrue(hasFunctionNode, "CallGraphAnalysis should have FunctionNode nested class");
    }

    @Test
    @DisplayName("FunctionNode should be public")
    void functionNodeShouldBePublic() throws ClassNotFoundException {
      Class<?> nodeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$FunctionNode");
      assertTrue(Modifier.isPublic(nodeClass.getModifiers()), "FunctionNode should be public");
    }

    @Test
    @DisplayName("FunctionNode should be static")
    void functionNodeShouldBeStatic() throws ClassNotFoundException {
      Class<?> nodeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$FunctionNode");
      assertTrue(Modifier.isStatic(nodeClass.getModifiers()), "FunctionNode should be static");
    }

    @Test
    @DisplayName("FunctionNode should be final")
    void functionNodeShouldBeFinal() throws ClassNotFoundException {
      Class<?> nodeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$FunctionNode");
      assertTrue(Modifier.isFinal(nodeClass.getModifiers()), "FunctionNode should be final");
    }

    @Test
    @DisplayName("FunctionNode should have getName method")
    void functionNodeShouldHaveGetNameMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> nodeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$FunctionNode");
      final Method method = nodeClass.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("FunctionNode should have getCallCount method")
    void functionNodeShouldHaveGetCallCountMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> nodeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$FunctionNode");
      final Method method = nodeClass.getMethod("getCallCount");
      assertNotNull(method, "getCallCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getCallCount should return long");
    }

    @Test
    @DisplayName("FunctionNode should have getTotalTime method")
    void functionNodeShouldHaveGetTotalTimeMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> nodeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$FunctionNode");
      final Method method = nodeClass.getMethod("getTotalTime");
      assertNotNull(method, "getTotalTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getTotalTime should return Duration");
    }

    @Test
    @DisplayName("FunctionNode should have getExclusiveTime method")
    void functionNodeShouldHaveGetExclusiveTimeMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> nodeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$FunctionNode");
      final Method method = nodeClass.getMethod("getExclusiveTime");
      assertNotNull(method, "getExclusiveTime method should exist");
      assertEquals(
          Duration.class, method.getReturnType(), "getExclusiveTime should return Duration");
    }

    @Test
    @DisplayName("FunctionNode should have getFanIn method")
    void functionNodeShouldHaveGetFanInMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> nodeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$FunctionNode");
      final Method method = nodeClass.getMethod("getFanIn");
      assertNotNull(method, "getFanIn method should exist");
      assertEquals(int.class, method.getReturnType(), "getFanIn should return int");
    }

    @Test
    @DisplayName("FunctionNode should have getFanOut method")
    void functionNodeShouldHaveGetFanOutMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> nodeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$FunctionNode");
      final Method method = nodeClass.getMethod("getFanOut");
      assertNotNull(method, "getFanOut method should exist");
      assertEquals(int.class, method.getReturnType(), "getFanOut should return int");
    }
  }

  // ========================================================================
  // CallEdge Class Tests
  // ========================================================================

  @Nested
  @DisplayName("CallEdge Class Tests")
  class CallEdgeClassTests {

    @Test
    @DisplayName("should have CallEdge nested class")
    void shouldHaveCallEdgeNestedClass() {
      Class<?>[] nestedClasses = CallGraphAnalysis.class.getDeclaredClasses();
      boolean hasCallEdge =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("CallEdge"));
      assertTrue(hasCallEdge, "CallGraphAnalysis should have CallEdge nested class");
    }

    @Test
    @DisplayName("CallEdge should be public")
    void callEdgeShouldBePublic() throws ClassNotFoundException {
      Class<?> edgeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallEdge");
      assertTrue(Modifier.isPublic(edgeClass.getModifiers()), "CallEdge should be public");
    }

    @Test
    @DisplayName("CallEdge should be static")
    void callEdgeShouldBeStatic() throws ClassNotFoundException {
      Class<?> edgeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallEdge");
      assertTrue(Modifier.isStatic(edgeClass.getModifiers()), "CallEdge should be static");
    }

    @Test
    @DisplayName("CallEdge should be final")
    void callEdgeShouldBeFinal() throws ClassNotFoundException {
      Class<?> edgeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallEdge");
      assertTrue(Modifier.isFinal(edgeClass.getModifiers()), "CallEdge should be final");
    }

    @Test
    @DisplayName("CallEdge should have getCaller method")
    void callEdgeShouldHaveGetCallerMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> edgeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallEdge");
      final Method method = edgeClass.getMethod("getCaller");
      assertNotNull(method, "getCaller method should exist");
      assertEquals(String.class, method.getReturnType(), "getCaller should return String");
    }

    @Test
    @DisplayName("CallEdge should have getCallee method")
    void callEdgeShouldHaveGetCalleeMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> edgeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallEdge");
      final Method method = edgeClass.getMethod("getCallee");
      assertNotNull(method, "getCallee method should exist");
      assertEquals(String.class, method.getReturnType(), "getCallee should return String");
    }

    @Test
    @DisplayName("CallEdge should have getCallCount method")
    void callEdgeShouldHaveGetCallCountMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> edgeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallEdge");
      final Method method = edgeClass.getMethod("getCallCount");
      assertNotNull(method, "getCallCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getCallCount should return long");
    }

    @Test
    @DisplayName("CallEdge should have getTotalTime method")
    void callEdgeShouldHaveGetTotalTimeMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> edgeClass = Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallEdge");
      final Method method = edgeClass.getMethod("getTotalTime");
      assertNotNull(method, "getTotalTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getTotalTime should return Duration");
    }
  }

  // ========================================================================
  // CallGraphMetrics Class Tests
  // ========================================================================

  @Nested
  @DisplayName("CallGraphMetrics Class Tests")
  class CallGraphMetricsClassTests {

    @Test
    @DisplayName("should have CallGraphMetrics nested class")
    void shouldHaveCallGraphMetricsNestedClass() {
      Class<?>[] nestedClasses = CallGraphAnalysis.class.getDeclaredClasses();
      boolean hasMetrics =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("CallGraphMetrics"));
      assertTrue(hasMetrics, "CallGraphAnalysis should have CallGraphMetrics nested class");
    }

    @Test
    @DisplayName("CallGraphMetrics should be public")
    void callGraphMetricsShouldBePublic() throws ClassNotFoundException {
      Class<?> metricsClass =
          Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallGraphMetrics");
      assertTrue(
          Modifier.isPublic(metricsClass.getModifiers()), "CallGraphMetrics should be public");
    }

    @Test
    @DisplayName("CallGraphMetrics should be static")
    void callGraphMetricsShouldBeStatic() throws ClassNotFoundException {
      Class<?> metricsClass =
          Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallGraphMetrics");
      assertTrue(
          Modifier.isStatic(metricsClass.getModifiers()), "CallGraphMetrics should be static");
    }

    @Test
    @DisplayName("CallGraphMetrics should be final")
    void callGraphMetricsShouldBeFinal() throws ClassNotFoundException {
      Class<?> metricsClass =
          Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallGraphMetrics");
      assertTrue(Modifier.isFinal(metricsClass.getModifiers()), "CallGraphMetrics should be final");
    }

    @Test
    @DisplayName("CallGraphMetrics should have getTotalFunctions method")
    void callGraphMetricsShouldHaveGetTotalFunctionsMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> metricsClass =
          Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallGraphMetrics");
      final Method method = metricsClass.getMethod("getTotalFunctions");
      assertNotNull(method, "getTotalFunctions method should exist");
      assertEquals(int.class, method.getReturnType(), "getTotalFunctions should return int");
    }

    @Test
    @DisplayName("CallGraphMetrics should have getTotalCallSites method")
    void callGraphMetricsShouldHaveGetTotalCallSitesMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> metricsClass =
          Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallGraphMetrics");
      final Method method = metricsClass.getMethod("getTotalCallSites");
      assertNotNull(method, "getTotalCallSites method should exist");
      assertEquals(int.class, method.getReturnType(), "getTotalCallSites should return int");
    }

    @Test
    @DisplayName("CallGraphMetrics should have getRecursiveFunctions method")
    void callGraphMetricsShouldHaveGetRecursiveFunctionsMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> metricsClass =
          Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallGraphMetrics");
      final Method method = metricsClass.getMethod("getRecursiveFunctions");
      assertNotNull(method, "getRecursiveFunctions method should exist");
      assertEquals(int.class, method.getReturnType(), "getRecursiveFunctions should return int");
    }

    @Test
    @DisplayName("CallGraphMetrics should have getConnectedComponents method")
    void callGraphMetricsShouldHaveGetConnectedComponentsMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> metricsClass =
          Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallGraphMetrics");
      final Method method = metricsClass.getMethod("getConnectedComponents");
      assertNotNull(method, "getConnectedComponents method should exist");
      assertEquals(int.class, method.getReturnType(), "getConnectedComponents should return int");
    }

    @Test
    @DisplayName("CallGraphMetrics should have getAverageFanOut method")
    void callGraphMetricsShouldHaveGetAverageFanOutMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> metricsClass =
          Class.forName("ai.tegmentum.wasmtime4j.CallGraphAnalysis$CallGraphMetrics");
      final Method method = metricsClass.getMethod("getAverageFanOut");
      assertNotNull(method, "getAverageFanOut method should exist");
      assertEquals(double.class, method.getReturnType(), "getAverageFanOut should return double");
    }
  }

  // ========================================================================
  // Nested Classes Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Count Tests")
  class NestedClassesCountTests {

    @Test
    @DisplayName("should have exactly 4 public nested types")
    void shouldHaveExactly4PublicNestedTypes() {
      Class<?>[] nestedClasses = CallGraphAnalysis.class.getDeclaredClasses();
      long publicNestedCount =
          Arrays.stream(nestedClasses).filter(c -> Modifier.isPublic(c.getModifiers())).count();
      assertEquals(4, publicNestedCount, "Should have exactly 4 public nested types");
    }

    @Test
    @DisplayName("should have Builder, FunctionNode, CallEdge, and CallGraphMetrics nested types")
    void shouldHaveCorrectNestedTypes() {
      Class<?>[] nestedClasses = CallGraphAnalysis.class.getDeclaredClasses();
      Set<String> nestedNames =
          Arrays.stream(nestedClasses)
              .filter(c -> Modifier.isPublic(c.getModifiers()))
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      assertTrue(nestedNames.contains("Builder"), "Should have Builder nested class");
      assertTrue(nestedNames.contains("FunctionNode"), "Should have FunctionNode nested class");
      assertTrue(nestedNames.contains("CallEdge"), "Should have CallEdge nested class");
      assertTrue(
          nestedNames.contains("CallGraphMetrics"), "Should have CallGraphMetrics nested class");
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getCallers should return Set<String>")
    void getCallersShouldReturnSetOfString() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getCallers", String.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Set.class, paramType.getRawType(), "Raw type should be Set");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(String.class, typeArgs[0], "Type argument should be String");
    }

    @Test
    @DisplayName("getCallees should return Set<String>")
    void getCalleesShouldReturnSetOfString() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getCallees", String.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Set.class, paramType.getRawType(), "Raw type should be Set");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(String.class, typeArgs[0], "Type argument should be String");
    }

    @Test
    @DisplayName("getEntryPoints should return Set<String>")
    void getEntryPointsShouldReturnSetOfString() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getEntryPoints");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Set.class, paramType.getRawType(), "Raw type should be Set");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(String.class, typeArgs[0], "Type argument should be String");
    }

    @Test
    @DisplayName("getLeafFunctions should return Set<String>")
    void getLeafFunctionsShouldReturnSetOfString() throws NoSuchMethodException {
      final Method method = CallGraphAnalysis.class.getMethod("getLeafFunctions");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Set.class, paramType.getRawType(), "Raw type should be Set");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(String.class, typeArgs[0], "Type argument should be String");
    }
  }
}
