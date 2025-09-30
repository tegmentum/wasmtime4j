package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Analysis of function call relationships and execution patterns.
 *
 * <p>Provides insights into how functions call each other, execution paths, and potential
 * optimization opportunities based on call graph structure.
 *
 * @since 1.0.0
 */
public final class CallGraphAnalysis {

  private final Map<String, FunctionNode> functions;
  private final List<CallEdge> callEdges;
  private final Set<String> entryPoints;
  private final Set<String> leafFunctions;
  private final int maxCallDepth;
  private final Duration analysisTime;
  private final CallGraphMetrics metrics;

  private CallGraphAnalysis(
      final Map<String, FunctionNode> functions,
      final List<CallEdge> callEdges,
      final Set<String> entryPoints,
      final Set<String> leafFunctions,
      final int maxCallDepth,
      final Duration analysisTime,
      final CallGraphMetrics metrics) {
    this.functions = Map.copyOf(functions);
    this.callEdges = List.copyOf(callEdges);
    this.entryPoints = Set.copyOf(entryPoints);
    this.leafFunctions = Set.copyOf(leafFunctions);
    this.maxCallDepth = maxCallDepth;
    this.analysisTime = Objects.requireNonNull(analysisTime);
    this.metrics = Objects.requireNonNull(metrics);
  }

  /**
   * Creates a new builder for call graph analysis.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets all functions in the call graph.
   *
   * @return map of function name to function node
   */
  public Map<String, FunctionNode> getFunctions() {
    return functions;
  }

  /**
   * Gets all call edges in the graph.
   *
   * @return list of call edges
   */
  public List<CallEdge> getCallEdges() {
    return callEdges;
  }

  /**
   * Gets entry point functions (called externally).
   *
   * @return set of entry point function names
   */
  public Set<String> getEntryPoints() {
    return entryPoints;
  }

  /**
   * Gets leaf functions (don't call other functions).
   *
   * @return set of leaf function names
   */
  public Set<String> getLeafFunctions() {
    return leafFunctions;
  }

  /**
   * Gets the maximum call depth observed.
   *
   * @return maximum call depth
   */
  public int getMaxCallDepth() {
    return maxCallDepth;
  }

  /**
   * Gets the time taken for analysis.
   *
   * @return analysis time
   */
  public Duration getAnalysisTime() {
    return analysisTime;
  }

  /**
   * Gets call graph metrics.
   *
   * @return call graph metrics
   */
  public CallGraphMetrics getMetrics() {
    return metrics;
  }

  /**
   * Gets direct callers of a function.
   *
   * @param functionName the function name
   * @return set of caller function names
   */
  public Set<String> getCallers(final String functionName) {
    return callEdges.stream()
        .filter(edge -> edge.getCallee().equals(functionName))
        .map(CallEdge::getCaller)
        .collect(java.util.stream.Collectors.toSet());
  }

  /**
   * Gets direct callees of a function.
   *
   * @param functionName the function name
   * @return set of callee function names
   */
  public Set<String> getCallees(final String functionName) {
    return callEdges.stream()
        .filter(edge -> edge.getCaller().equals(functionName))
        .map(CallEdge::getCallee)
        .collect(java.util.stream.Collectors.toSet());
  }

  /**
   * Checks if there's a call path from one function to another.
   *
   * @param from source function
   * @param to target function
   * @return true if path exists
   */
  public boolean hasCallPath(final String from, final String to) {
    return findCallPath(from, to, Set.of()).isPresent();
  }

  /**
   * Finds a call path between two functions.
   *
   * @param from source function
   * @param to target function
   * @return call path if it exists
   */
  public java.util.Optional<List<String>> findCallPath(final String from, final String to) {
    return findCallPath(from, to, Set.of());
  }

  private java.util.Optional<List<String>> findCallPath(
      final String from, final String to, final Set<String> visited) {
    if (from.equals(to)) {
      return java.util.Optional.of(List.of(from));
    }

    if (visited.contains(from)) {
      return java.util.Optional.empty(); // Cycle detection
    }

    final Set<String> newVisited = new java.util.HashSet<>(visited);
    newVisited.add(from);

    for (final String callee : getCallees(from)) {
      final java.util.Optional<List<String>> path = findCallPath(callee, to, newVisited);
      if (path.isPresent()) {
        final List<String> fullPath = new java.util.ArrayList<>();
        fullPath.add(from);
        fullPath.addAll(path.get());
        return java.util.Optional.of(fullPath);
      }
    }

    return java.util.Optional.empty();
  }

  /** Builder for call graph analysis. */
  public static final class Builder {
    private Map<String, FunctionNode> functions = Map.of();
    private List<CallEdge> callEdges = List.of();
    private Set<String> entryPoints = Set.of();
    private Set<String> leafFunctions = Set.of();
    private int maxCallDepth = 0;
    private Duration analysisTime = Duration.ZERO;
    private CallGraphMetrics metrics = new CallGraphMetrics(0, 0, 0, 0, 0);

    public Builder functions(final Map<String, FunctionNode> functions) {
      this.functions = Map.copyOf(Objects.requireNonNull(functions));
      return this;
    }

    public Builder callEdges(final List<CallEdge> callEdges) {
      this.callEdges = List.copyOf(Objects.requireNonNull(callEdges));
      return this;
    }

    public Builder entryPoints(final Set<String> entryPoints) {
      this.entryPoints = Set.copyOf(Objects.requireNonNull(entryPoints));
      return this;
    }

    public Builder leafFunctions(final Set<String> leafFunctions) {
      this.leafFunctions = Set.copyOf(Objects.requireNonNull(leafFunctions));
      return this;
    }

    public Builder maxCallDepth(final int maxCallDepth) {
      this.maxCallDepth = maxCallDepth;
      return this;
    }

    public Builder analysisTime(final Duration analysisTime) {
      this.analysisTime = Objects.requireNonNull(analysisTime);
      return this;
    }

    public Builder metrics(final CallGraphMetrics metrics) {
      this.metrics = Objects.requireNonNull(metrics);
      return this;
    }

    public CallGraphAnalysis build() {
      return new CallGraphAnalysis(
          functions, callEdges, entryPoints, leafFunctions, maxCallDepth, analysisTime, metrics);
    }
  }

  /** Represents a function in the call graph. */
  public static final class FunctionNode {
    private final String name;
    private final long callCount;
    private final Duration totalTime;
    private final Duration exclusiveTime;
    private final int fanIn;
    private final int fanOut;

    /**
     * Creates a new function node in the call graph.
     *
     * @param name function name
     * @param callCount number of times this function was called
     * @param totalTime total time spent in this function (including callees)
     * @param exclusiveTime time spent exclusively in this function
     * @param fanIn number of functions calling this function
     * @param fanOut number of functions this function calls
     */
    public FunctionNode(
        final String name,
        final long callCount,
        final Duration totalTime,
        final Duration exclusiveTime,
        final int fanIn,
        final int fanOut) {
      this.name = Objects.requireNonNull(name);
      this.callCount = callCount;
      this.totalTime = Objects.requireNonNull(totalTime);
      this.exclusiveTime = Objects.requireNonNull(exclusiveTime);
      this.fanIn = fanIn;
      this.fanOut = fanOut;
    }

    public String getName() {
      return name;
    }

    public long getCallCount() {
      return callCount;
    }

    public Duration getTotalTime() {
      return totalTime;
    }

    public Duration getExclusiveTime() {
      return exclusiveTime;
    }

    public int getFanIn() {
      return fanIn;
    }

    public int getFanOut() {
      return fanOut;
    }

    @Override
    public String toString() {
      return String.format(
          "FunctionNode{name='%s', calls=%d, fanIn=%d, fanOut=%d}", name, callCount, fanIn, fanOut);
    }
  }

  /** Represents a call edge in the graph. */
  public static final class CallEdge {
    private final String caller;
    private final String callee;
    private final long callCount;
    private final Duration totalTime;

    /**
     * Creates a new call edge in the call graph.
     *
     * @param caller name of the calling function
     * @param callee name of the called function
     * @param callCount number of calls from caller to callee
     * @param totalTime total time spent in these calls
     */
    public CallEdge(
        final String caller, final String callee, final long callCount, final Duration totalTime) {
      this.caller = Objects.requireNonNull(caller);
      this.callee = Objects.requireNonNull(callee);
      this.callCount = callCount;
      this.totalTime = Objects.requireNonNull(totalTime);
    }

    public String getCaller() {
      return caller;
    }

    public String getCallee() {
      return callee;
    }

    public long getCallCount() {
      return callCount;
    }

    public Duration getTotalTime() {
      return totalTime;
    }

    @Override
    public String toString() {
      return String.format("CallEdge{%s -> %s, calls=%d}", caller, callee, callCount);
    }
  }

  /** Metrics for the call graph. */
  public static final class CallGraphMetrics {
    private final int totalFunctions;
    private final int totalCallSites;
    private final int recursiveFunctions;
    private final int connectedComponents;
    private final double averageFanOut;

    /**
     * Creates new call graph metrics.
     *
     * @param totalFunctions total number of functions in the graph
     * @param totalCallSites total number of call sites
     * @param recursiveFunctions number of recursive functions
     * @param connectedComponents number of connected components
     * @param averageFanOut average fan-out across all functions
     */
    public CallGraphMetrics(
        final int totalFunctions,
        final int totalCallSites,
        final int recursiveFunctions,
        final int connectedComponents,
        final double averageFanOut) {
      this.totalFunctions = totalFunctions;
      this.totalCallSites = totalCallSites;
      this.recursiveFunctions = recursiveFunctions;
      this.connectedComponents = connectedComponents;
      this.averageFanOut = averageFanOut;
    }

    public int getTotalFunctions() {
      return totalFunctions;
    }

    public int getTotalCallSites() {
      return totalCallSites;
    }

    public int getRecursiveFunctions() {
      return recursiveFunctions;
    }

    public int getConnectedComponents() {
      return connectedComponents;
    }

    public double getAverageFanOut() {
      return averageFanOut;
    }

    @Override
    public String toString() {
      return String.format(
          "CallGraphMetrics{functions=%d, callSites=%d, recursive=%d, components=%d}",
          totalFunctions, totalCallSites, recursiveFunctions, connectedComponents);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "CallGraphAnalysis{functions=%d, edges=%d, entryPoints=%d, maxDepth=%d}",
        functions.size(), callEdges.size(), entryPoints.size(), maxCallDepth);
  }
}
