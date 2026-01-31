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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link CallGraphAnalysis} call graph analysis. */
@DisplayName("CallGraphAnalysis")
final class CallGraphAnalysisTest {

  private CallGraphAnalysis.FunctionNode createNode(
      final String name, final long callCount, final int fanIn, final int fanOut) {
    return new CallGraphAnalysis.FunctionNode(
        name, callCount, Duration.ofMillis(100), Duration.ofMillis(50), fanIn, fanOut);
  }

  private CallGraphAnalysis.CallEdge createEdge(
      final String caller, final String callee, final long callCount) {
    return new CallGraphAnalysis.CallEdge(caller, callee, callCount, Duration.ofMillis(10));
  }

  @Nested
  @DisplayName("builder and basic accessors")
  final class BuilderAndAccessorTests {

    @Test
    @DisplayName("should build empty call graph")
    void shouldBuildEmptyCallGraph() {
      final CallGraphAnalysis analysis = CallGraphAnalysis.builder().build();
      assertNotNull(analysis, "Built analysis should not be null");
      assertTrue(analysis.getFunctions().isEmpty(), "Empty graph should have no functions");
      assertTrue(analysis.getCallEdges().isEmpty(), "Empty graph should have no edges");
    }

    @Test
    @DisplayName("should build call graph with functions")
    void shouldBuildWithFunctions() {
      final CallGraphAnalysis.FunctionNode node = createNode("main", 100, 0, 3);
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder()
              .functions(Map.of("main", node))
              .build();
      assertEquals(1, analysis.getFunctions().size(), "Should have 1 function");
      assertNotNull(analysis.getFunctions().get("main"), "Should find 'main' function");
    }

    @Test
    @DisplayName("should build call graph with edges")
    void shouldBuildWithEdges() {
      final CallGraphAnalysis.CallEdge edge = createEdge("main", "helper", 50);
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder()
              .callEdges(List.of(edge))
              .build();
      assertEquals(1, analysis.getCallEdges().size(), "Should have 1 call edge");
    }

    @Test
    @DisplayName("should set entry points and leaf functions")
    void shouldSetEntryPointsAndLeafFunctions() {
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder()
              .entryPoints(Set.of("main", "_start"))
              .leafFunctions(Set.of("log", "abort"))
              .build();
      assertEquals(2, analysis.getEntryPoints().size(), "Should have 2 entry points");
      assertEquals(2, analysis.getLeafFunctions().size(), "Should have 2 leaf functions");
    }

    @Test
    @DisplayName("should set max call depth")
    void shouldSetMaxCallDepth() {
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder().maxCallDepth(15).build();
      assertEquals(15, analysis.getMaxCallDepth(), "Max call depth should be 15");
    }

    @Test
    @DisplayName("should set analysis time")
    void shouldSetAnalysisTime() {
      final Duration time = Duration.ofSeconds(2);
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder().analysisTime(time).build();
      assertEquals(time, analysis.getAnalysisTime(), "Analysis time should match");
    }
  }

  @Nested
  @DisplayName("getCallers and getCallees")
  final class CallerCalleeTests {

    @Test
    @DisplayName("should find callers of a function")
    void shouldFindCallers() {
      final List<CallGraphAnalysis.CallEdge> edges =
          List.of(createEdge("main", "helper", 10), createEdge("init", "helper", 5));
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder().callEdges(edges).build();
      final Set<String> callers = analysis.getCallers("helper");
      assertEquals(2, callers.size(), "helper should have 2 callers");
      assertTrue(callers.contains("main"), "main should be a caller of helper");
      assertTrue(callers.contains("init"), "init should be a caller of helper");
    }

    @Test
    @DisplayName("should find callees of a function")
    void shouldFindCallees() {
      final List<CallGraphAnalysis.CallEdge> edges =
          List.of(createEdge("main", "foo", 10), createEdge("main", "bar", 20));
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder().callEdges(edges).build();
      final Set<String> callees = analysis.getCallees("main");
      assertEquals(2, callees.size(), "main should call 2 functions");
      assertTrue(callees.contains("foo"), "main should call foo");
      assertTrue(callees.contains("bar"), "main should call bar");
    }

    @Test
    @DisplayName("should return empty set for unknown function callers")
    void shouldReturnEmptyForUnknownCallers() {
      final CallGraphAnalysis analysis = CallGraphAnalysis.builder().build();
      final Set<String> callers = analysis.getCallers("nonexistent");
      assertTrue(callers.isEmpty(), "Unknown function should have no callers");
    }
  }

  @Nested
  @DisplayName("hasCallPath and findCallPath")
  final class CallPathTests {

    @Test
    @DisplayName("should find direct call path")
    void shouldFindDirectCallPath() {
      final List<CallGraphAnalysis.CallEdge> edges = List.of(createEdge("A", "B", 1));
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder().callEdges(edges).build();
      assertTrue(analysis.hasCallPath("A", "B"), "Should find direct path from A to B");
    }

    @Test
    @DisplayName("should find transitive call path")
    void shouldFindTransitiveCallPath() {
      final List<CallGraphAnalysis.CallEdge> edges =
          List.of(createEdge("A", "B", 1), createEdge("B", "C", 1));
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder().callEdges(edges).build();
      assertTrue(analysis.hasCallPath("A", "C"), "Should find transitive path from A to C");
    }

    @Test
    @DisplayName("should return false for no call path")
    void shouldReturnFalseForNoPath() {
      final List<CallGraphAnalysis.CallEdge> edges = List.of(createEdge("A", "B", 1));
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder().callEdges(edges).build();
      assertFalse(analysis.hasCallPath("B", "A"), "Should not find reverse path from B to A");
    }

    @Test
    @DisplayName("should find self path")
    void shouldFindSelfPath() {
      final CallGraphAnalysis analysis = CallGraphAnalysis.builder().build();
      assertTrue(analysis.hasCallPath("X", "X"), "Should find path from X to X (self)");
    }

    @Test
    @DisplayName("should return call path list")
    void shouldReturnCallPathList() {
      final List<CallGraphAnalysis.CallEdge> edges =
          List.of(createEdge("A", "B", 1), createEdge("B", "C", 1));
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder().callEdges(edges).build();
      final Optional<List<String>> path = analysis.findCallPath("A", "C");
      assertTrue(path.isPresent(), "Path from A to C should exist");
      assertEquals(3, path.get().size(), "Path should have 3 nodes: A, B, C");
      assertEquals("A", path.get().get(0), "Path should start with A");
      assertEquals("C", path.get().get(2), "Path should end with C");
    }

    @Test
    @DisplayName("should handle cycles in call graph without stack overflow")
    void shouldHandleCyclesWithoutStackOverflow() {
      final List<CallGraphAnalysis.CallEdge> edges =
          List.of(createEdge("A", "B", 1), createEdge("B", "A", 1));
      final CallGraphAnalysis analysis =
          CallGraphAnalysis.builder().callEdges(edges).build();
      assertFalse(
          analysis.hasCallPath("A", "C"),
          "Should not find path to C in cyclic A<->B graph");
    }
  }

  @Nested
  @DisplayName("FunctionNode")
  final class FunctionNodeTests {

    @Test
    @DisplayName("should store all fields correctly")
    void shouldStoreAllFields() {
      final CallGraphAnalysis.FunctionNode node =
          new CallGraphAnalysis.FunctionNode(
              "myFunc", 200, Duration.ofMillis(500), Duration.ofMillis(100), 3, 5);
      assertEquals("myFunc", node.getName(), "Name should be myFunc");
      assertEquals(200, node.getCallCount(), "Call count should be 200");
      assertEquals(Duration.ofMillis(500), node.getTotalTime(), "Total time should match");
      assertEquals(Duration.ofMillis(100), node.getExclusiveTime(), "Exclusive time should match");
      assertEquals(3, node.getFanIn(), "Fan-in should be 3");
      assertEquals(5, node.getFanOut(), "Fan-out should be 5");
    }

    @Test
    @DisplayName("should produce readable toString")
    void shouldProduceReadableToString() {
      final CallGraphAnalysis.FunctionNode node = createNode("test", 42, 2, 3);
      final String str = node.toString();
      assertTrue(str.contains("test"), "toString should contain function name");
      assertTrue(str.contains("42"), "toString should contain call count");
    }
  }

  @Nested
  @DisplayName("CallEdge")
  final class CallEdgeTests {

    @Test
    @DisplayName("should store caller and callee")
    void shouldStoreCallerAndCallee() {
      final CallGraphAnalysis.CallEdge edge = createEdge("sender", "receiver", 75);
      assertEquals("sender", edge.getCaller(), "Caller should be sender");
      assertEquals("receiver", edge.getCallee(), "Callee should be receiver");
      assertEquals(75, edge.getCallCount(), "Call count should be 75");
    }
  }

  @Nested
  @DisplayName("CallGraphMetrics")
  final class CallGraphMetricsTests {

    @Test
    @DisplayName("should store all metrics fields")
    void shouldStoreAllMetricsFields() {
      final CallGraphAnalysis.CallGraphMetrics metrics =
          new CallGraphAnalysis.CallGraphMetrics(50, 120, 3, 2, 2.4);
      assertEquals(50, metrics.getTotalFunctions(), "Total functions should be 50");
      assertEquals(120, metrics.getTotalCallSites(), "Total call sites should be 120");
      assertEquals(3, metrics.getRecursiveFunctions(), "Recursive functions should be 3");
      assertEquals(2, metrics.getConnectedComponents(), "Connected components should be 2");
      assertEquals(2.4, metrics.getAverageFanOut(), 0.001, "Average fan-out should be 2.4");
    }
  }

  @Nested
  @DisplayName("toString")
  final class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString")
    void shouldProduceNonNullToString() {
      final CallGraphAnalysis analysis = CallGraphAnalysis.builder().build();
      assertNotNull(analysis.toString(), "toString should not return null");
      assertTrue(
          analysis.toString().contains("CallGraphAnalysis"),
          "toString should contain class name");
    }
  }
}
