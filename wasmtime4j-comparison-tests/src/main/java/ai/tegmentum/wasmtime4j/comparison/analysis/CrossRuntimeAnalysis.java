package ai.tegmentum.wasmtime4j.comparison.analysis;

import ai.tegmentum.wasmtime4j.testsuite.TestRuntime;
import java.util.List;
import java.util.Map;

/** Analysis results comparing test execution across different runtimes. */
public final class CrossRuntimeAnalysis {

  private final Map<TestRuntime, Integer> runtimeSuccessRates;
  private final List<String> discrepancies;

  public CrossRuntimeAnalysis(
      final Map<TestRuntime, Integer> runtimeSuccessRates, final List<String> discrepancies) {
    this.runtimeSuccessRates = Map.copyOf(runtimeSuccessRates);
    this.discrepancies = List.copyOf(discrepancies);
  }

  public Map<TestRuntime, Integer> getRuntimeSuccessRates() {
    return runtimeSuccessRates;
  }

  public List<String> getDiscrepancies() {
    return discrepancies;
  }

  /**
   * Checks if there are runtime discrepancies.
   *
   * @return true if discrepancies exist
   */
  public boolean hasDiscrepancies() {
    return !discrepancies.isEmpty();
  }
}
