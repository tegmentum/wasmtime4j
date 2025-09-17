package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Result of a diff comparison operation. */
final class DiffResult {
  private final List<DiffLine> diffLines;
  private final boolean hasDifferences;

  public DiffResult(final List<DiffLine> diffLines, final boolean hasDifferences) {
    this.diffLines = List.copyOf(diffLines);
    this.hasDifferences = hasDifferences;
  }

  public List<DiffLine> getDiffLines() {
    return diffLines;
  }

  public boolean hasDifferences() {
    return hasDifferences;
  }

  public Map<String, Object> toMap() {
    final Map<String, Object> map = new HashMap<>();
    map.put("lines", diffLines.stream().map(DiffLine::toMap).toList());
    map.put("hasDifferences", hasDifferences);
    map.put(
        "addedLines",
        diffLines.stream().mapToLong(line -> line.getType() == DiffType.ADDED ? 1 : 0).sum());
    map.put(
        "removedLines",
        diffLines.stream().mapToLong(line -> line.getType() == DiffType.REMOVED ? 1 : 0).sum());
    map.put(
        "modifiedLines",
        diffLines.stream().mapToLong(line -> line.getType() == DiffType.MODIFIED ? 1 : 0).sum());
    map.put(
        "unchangedLines",
        diffLines.stream().mapToLong(line -> line.getType() == DiffType.UNCHANGED ? 1 : 0).sum());
    return map;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final DiffResult that = (DiffResult) obj;
    return hasDifferences == that.hasDifferences && Objects.equals(diffLines, that.diffLines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(diffLines, hasDifferences);
  }

  @Override
  public String toString() {
    return "DiffResult{" + "lines=" + diffLines.size() + ", hasDifferences=" + hasDifferences + '}';
  }
}
