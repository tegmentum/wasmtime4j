package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Results of hot function analysis for streaming compilation optimization.
 *
 * <p>HotFunctionAnalysis provides information about which functions in a WebAssembly module are
 * likely to be frequently called and should be prioritized during streaming compilation.
 *
 * @since 1.0.0
 */
public final class HotFunctionAnalysis {

  private final List<HotFunctionInfo> hotFunctions;
  private final List<ColdFunctionInfo> coldFunctions;
  private final Map<String, Double> functionHotness;
  private final CallGraphAnalysis callGraph;
  private final long totalFunctions;
  private final double analysisConfidence;
  private final AnalysisMethod analysisMethod;

  private HotFunctionAnalysis(final Builder builder) {
    this.hotFunctions = List.copyOf(builder.hotFunctions);
    this.coldFunctions = List.copyOf(builder.coldFunctions);
    this.functionHotness = Map.copyOf(builder.functionHotness);
    this.callGraph = builder.callGraph;
    this.totalFunctions = builder.totalFunctions;
    this.analysisConfidence = builder.analysisConfidence;
    this.analysisMethod = builder.analysisMethod;
  }

  /**
   * Gets the list of functions identified as hot.
   *
   * <p>Hot functions are likely to be called frequently and should be prioritized for compilation.
   *
   * @return list of hot function information
   */
  public List<HotFunctionInfo> getHotFunctions() {
    return hotFunctions;
  }

  /**
   * Gets the list of functions identified as cold.
   *
   * <p>Cold functions are rarely called and can be deferred during compilation.
   *
   * @return list of cold function information
   */
  public List<ColdFunctionInfo> getColdFunctions() {
    return coldFunctions;
  }

  /**
   * Gets the hotness score for all analyzed functions.
   *
   * <p>Hotness scores range from 0.0 (coldest) to 1.0 (hottest) and indicate the relative
   * importance of each function for prioritization.
   *
   * @return map of function names to hotness scores
   */
  public Map<String, Double> getFunctionHotness() {
    return functionHotness;
  }

  /**
   * Gets the call graph analysis results.
   *
   * <p>Call graph analysis provides information about function relationships and calling patterns.
   *
   * @return call graph analysis results
   */
  public CallGraphAnalysis getCallGraph() {
    return callGraph;
  }

  /**
   * Gets the total number of functions analyzed.
   *
   * @return total number of functions
   */
  public long getTotalFunctions() {
    return totalFunctions;
  }

  /**
   * Gets the confidence level of the analysis results.
   *
   * <p>Confidence ranges from 0.0 (low confidence) to 1.0 (high confidence) and indicates how
   * reliable the hotness predictions are expected to be.
   *
   * @return analysis confidence level
   */
  public double getAnalysisConfidence() {
    return analysisConfidence;
  }

  /**
   * Gets the method used for hotness analysis.
   *
   * @return analysis method
   */
  public AnalysisMethod getAnalysisMethod() {
    return analysisMethod;
  }

  /**
   * Gets the number of functions identified as hot.
   *
   * @return number of hot functions
   */
  public int getHotFunctionCount() {
    return hotFunctions.size();
  }

  /**
   * Gets the number of functions identified as cold.
   *
   * @return number of cold functions
   */
  public int getColdFunctionCount() {
    return coldFunctions.size();
  }

  /**
   * Gets the hotness score for a specific function.
   *
   * @param functionName the name of the function
   * @return hotness score, or 0.0 if function not found
   * @throws IllegalArgumentException if functionName is null
   */
  public double getFunctionHotnessScore(final String functionName) {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    return functionHotness.getOrDefault(functionName, 0.0);
  }

  /**
   * Checks if a function is identified as hot.
   *
   * @param functionName the name of the function to check
   * @return true if the function is hot
   * @throws IllegalArgumentException if functionName is null
   */
  public boolean isHotFunction(final String functionName) {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    return hotFunctions.stream().anyMatch(info -> functionName.equals(info.getFunctionName()));
  }

  /**
   * Checks if a function is identified as cold.
   *
   * @param functionName the name of the function to check
   * @return true if the function is cold
   * @throws IllegalArgumentException if functionName is null
   */
  public boolean isColdFunction(final String functionName) {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    return coldFunctions.stream().anyMatch(info -> functionName.equals(info.getFunctionName()));
  }

  /**
   * Creates a new builder for HotFunctionAnalysis.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final HotFunctionAnalysis that = (HotFunctionAnalysis) obj;
    return totalFunctions == that.totalFunctions
        && Double.compare(that.analysisConfidence, analysisConfidence) == 0
        && Objects.equals(hotFunctions, that.hotFunctions)
        && Objects.equals(coldFunctions, that.coldFunctions)
        && Objects.equals(functionHotness, that.functionHotness)
        && Objects.equals(callGraph, that.callGraph)
        && analysisMethod == that.analysisMethod;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        hotFunctions,
        coldFunctions,
        functionHotness,
        callGraph,
        totalFunctions,
        analysisConfidence,
        analysisMethod);
  }

  @Override
  public String toString() {
    return "HotFunctionAnalysis{"
        + "hotFunctions="
        + hotFunctions.size()
        + ", coldFunctions="
        + coldFunctions.size()
        + ", totalFunctions="
        + totalFunctions
        + ", analysisConfidence="
        + analysisConfidence
        + ", analysisMethod="
        + analysisMethod
        + '}';
  }

  /** Builder for HotFunctionAnalysis. */
  public static final class Builder {
    private List<HotFunctionInfo> hotFunctions = List.of();
    private List<ColdFunctionInfo> coldFunctions = List.of();
    private Map<String, Double> functionHotness = Map.of();
    private CallGraphAnalysis callGraph;
    private long totalFunctions = 0;
    private double analysisConfidence = 0.0;
    private AnalysisMethod analysisMethod = AnalysisMethod.STATIC_ANALYSIS;

    private Builder() {}

    public Builder hotFunctions(final List<HotFunctionInfo> hotFunctions) {
      this.hotFunctions = Objects.requireNonNull(hotFunctions, "Hot functions list cannot be null");
      return this;
    }

    public Builder coldFunctions(final List<ColdFunctionInfo> coldFunctions) {
      this.coldFunctions =
          Objects.requireNonNull(coldFunctions, "Cold functions list cannot be null");
      return this;
    }

    public Builder functionHotness(final Map<String, Double> functionHotness) {
      this.functionHotness =
          Objects.requireNonNull(functionHotness, "Function hotness map cannot be null");
      return this;
    }

    public Builder callGraph(final CallGraphAnalysis callGraph) {
      this.callGraph = Objects.requireNonNull(callGraph, "Call graph analysis cannot be null");
      return this;
    }

    public Builder totalFunctions(final long totalFunctions) {
      this.totalFunctions = totalFunctions;
      return this;
    }

    public Builder analysisConfidence(final double analysisConfidence) {
      this.analysisConfidence = analysisConfidence;
      return this;
    }

    public Builder analysisMethod(final AnalysisMethod analysisMethod) {
      this.analysisMethod =
          Objects.requireNonNull(analysisMethod, "Analysis method cannot be null");
      return this;
    }

    public HotFunctionAnalysis build() {
      return new HotFunctionAnalysis(this);
    }
  }
}