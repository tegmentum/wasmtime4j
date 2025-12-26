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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ReferenceGraph} interface.
 *
 * <p>ReferenceGraph represents the reference relationships between objects in the WebAssembly GC
 * heap, useful for debugging memory leaks and understanding object lifecycle dependencies.
 */
@DisplayName("ReferenceGraph Tests")
class ReferenceGraphTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ReferenceGraph.class.isInterface(), "ReferenceGraph should be an interface");
    }
  }

  @Nested
  @DisplayName("Object Query Method Tests")
  class ObjectQueryMethodTests {

    @Test
    @DisplayName("should have getAllObjects method")
    void shouldHaveGetAllObjectsMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("getAllObjects");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getOutgoingReferences method")
    void shouldHaveGetOutgoingReferencesMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("getOutgoingReferences", long.class);
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getIncomingReferences method")
    void shouldHaveGetIncomingReferencesMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("getIncomingReferences", long.class);
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }
  }

  @Nested
  @DisplayName("Path Analysis Method Tests")
  class PathAnalysisMethodTests {

    @Test
    @DisplayName("should have getPathFromRoot method")
    void shouldHaveGetPathFromRootMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("getPathFromRoot", long.class);
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getReachableObjects method")
    void shouldHaveGetReachableObjectsMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("getReachableObjects", long.class);
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getObjectsReaching method")
    void shouldHaveGetObjectsReachingMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("getObjectsReaching", long.class);
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }
  }

  @Nested
  @DisplayName("Graph Analysis Method Tests")
  class GraphAnalysisMethodTests {

    @Test
    @DisplayName("should have findStronglyConnectedComponents method")
    void shouldHaveFindStronglyConnectedComponentsMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("findStronglyConnectedComponents");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have findPotentialLeaks method")
    void shouldHaveFindPotentialLeaksMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("findPotentialLeaks");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getReferenceEdge method")
    void shouldHaveGetReferenceEdgeMethod() throws NoSuchMethodException {
      final Method method =
          ReferenceGraph.class.getMethod("getReferenceEdge", long.class, long.class);
      assertEquals(
          ReferenceGraph.ReferenceEdgeInfo.class,
          method.getReturnType(),
          "Should return ReferenceEdgeInfo");
    }
  }

  @Nested
  @DisplayName("ReferenceEdgeInfo Interface Tests")
  class ReferenceEdgeInfoTests {

    @Test
    @DisplayName("should be a nested interface")
    void shouldBeNestedInterface() {
      assertTrue(
          ReferenceGraph.ReferenceEdgeInfo.class.isInterface(),
          "ReferenceEdgeInfo should be an interface");
    }

    @Test
    @DisplayName("should have getFromObjectId method")
    void shouldHaveGetFromObjectIdMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.ReferenceEdgeInfo.class.getMethod("getFromObjectId");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getToObjectId method")
    void shouldHaveGetToObjectIdMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.ReferenceEdgeInfo.class.getMethod("getToObjectId");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getReferenceType method")
    void shouldHaveGetReferenceTypeMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.ReferenceEdgeInfo.class.getMethod("getReferenceType");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getIndex method")
    void shouldHaveGetIndexMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.ReferenceEdgeInfo.class.getMethod("getIndex");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws NoSuchMethodException {
      final Method method = ReferenceGraph.ReferenceEdgeInfo.class.getMethod("getDescription");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getOutgoingReferences should take long parameter")
    void getOutgoingReferencesShouldTakeLongParameter() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("getOutgoingReferences", long.class);
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");
    }

    @Test
    @DisplayName("getIncomingReferences should take long parameter")
    void getIncomingReferencesShouldTakeLongParameter() throws NoSuchMethodException {
      final Method method = ReferenceGraph.class.getMethod("getIncomingReferences", long.class);
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");
    }

    @Test
    @DisplayName("getReferenceEdge should take two long parameters")
    void getReferenceEdgeShouldTakeTwoLongParameters() throws NoSuchMethodException {
      final Method method =
          ReferenceGraph.class.getMethod("getReferenceEdge", long.class, long.class);
      assertEquals(2, method.getParameterCount(), "Should have 2 parameters");
      assertEquals(long.class, method.getParameterTypes()[0], "First parameter should be long");
      assertEquals(long.class, method.getParameterTypes()[1], "Second parameter should be long");
    }
  }

  @Nested
  @DisplayName("WASM GC Specification Compliance Tests")
  class WasmGcSpecificationComplianceTests {

    @Test
    @DisplayName("should have all required reference graph methods")
    void shouldHaveAllRequiredReferenceGraphMethods() {
      final String[] expectedMethods = {
        "getAllObjects",
        "getOutgoingReferences",
        "getIncomingReferences",
        "getPathFromRoot",
        "getReachableObjects",
        "getObjectsReaching",
        "findStronglyConnectedComponents",
        "findPotentialLeaks",
        "getReferenceEdge"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ReferenceGraph.class, methodName), "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support memory leak detection pattern")
    void shouldSupportMemoryLeakDetectionPattern() {
      // Documents usage:
      // Set<Long> leaks = graph.findPotentialLeaks();
      // for (Long objectId : leaks) {
      //   List<Long> path = graph.getPathFromRoot(objectId);
      // }
      assertTrue(
          hasMethod(ReferenceGraph.class, "findPotentialLeaks"), "Need findPotentialLeaks method");
      assertTrue(hasMethod(ReferenceGraph.class, "getPathFromRoot"), "Need getPathFromRoot method");
    }

    @Test
    @DisplayName("should support reference traversal pattern")
    void shouldSupportReferenceTraversalPattern() {
      // Documents usage:
      // Set<Long> outgoing = graph.getOutgoingReferences(objectId);
      // Set<Long> incoming = graph.getIncomingReferences(objectId);
      assertTrue(
          hasMethod(ReferenceGraph.class, "getOutgoingReferences"),
          "Need getOutgoingReferences method");
      assertTrue(
          hasMethod(ReferenceGraph.class, "getIncomingReferences"),
          "Need getIncomingReferences method");
    }

    @Test
    @DisplayName("should support cycle detection pattern")
    void shouldSupportCycleDetectionPattern() {
      // Documents usage:
      // List<Set<Long>> sccs = graph.findStronglyConnectedComponents();
      assertTrue(
          hasMethod(ReferenceGraph.class, "findStronglyConnectedComponents"),
          "Need findStronglyConnectedComponents method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
