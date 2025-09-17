package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Represents a single line in a diff comparison. */
final class DiffLine {
  private final int lineNumber;
  private final String leftText;
  private final String rightText;
  private final DiffType type;

  public DiffLine(
      final int lineNumber, final String leftText, final String rightText, final DiffType type) {
    this.lineNumber = lineNumber;
    this.leftText = leftText;
    this.rightText = rightText;
    this.type = Objects.requireNonNull(type, "type cannot be null");
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getLeftText() {
    return leftText;
  }

  public String getRightText() {
    return rightText;
  }

  public DiffType getType() {
    return type;
  }

  public Map<String, Object> toMap() {
    final Map<String, Object> map = new HashMap<>();
    map.put("lineNumber", lineNumber);
    map.put("leftText", leftText);
    map.put("rightText", rightText);
    map.put("type", type.toString());
    map.put("cssClass", type.getCssClass());
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

    final DiffLine diffLine = (DiffLine) obj;
    return lineNumber == diffLine.lineNumber
        && Objects.equals(leftText, diffLine.leftText)
        && Objects.equals(rightText, diffLine.rightText)
        && type == diffLine.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lineNumber, leftText, rightText, type);
  }

  @Override
  public String toString() {
    return "DiffLine{" + "lineNumber=" + lineNumber + ", type=" + type + '}';
  }
}
