package ai.tegmentum.wasmtime4j;

import java.util.Objects;
import java.util.Optional;

/**
 * Debug symbol information for a WebAssembly stack frame.
 *
 * <p>FrameSymbol provides source-level debugging information extracted from DWARF debug symbols,
 * including source file locations and function names.
 *
 * @since 1.0.0
 */
public final class FrameSymbol {

  private final String name;
  private final String file;
  private final Integer line;
  private final Integer column;

  /**
   * Creates a new FrameSymbol with the specified debug information.
   *
   * @param name the symbol name (function or variable name)
   * @param file the source file path
   * @param line the source line number (1-based)
   * @param column the source column number (1-based)
   */
  public FrameSymbol(
      final String name, final String file, final Integer line, final Integer column) {
    this.name = name;
    this.file = file;
    this.line = line;
    this.column = column;
  }

  /**
   * Gets the symbol name.
   *
   * <p>This is typically a function name or variable name from the source code.
   *
   * @return the symbol name, or empty if not available
   */
  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  /**
   * Gets the source file path.
   *
   * @return the source file path, or empty if not available
   */
  public Optional<String> getFile() {
    return Optional.ofNullable(file);
  }

  /**
   * Gets the source line number.
   *
   * @return the line number (1-based), or empty if not available
   */
  public Optional<Integer> getLine() {
    return Optional.ofNullable(line);
  }

  /**
   * Gets the source column number.
   *
   * @return the column number (1-based), or empty if not available
   */
  public Optional<Integer> getColumn() {
    return Optional.ofNullable(column);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final FrameSymbol that = (FrameSymbol) obj;
    return Objects.equals(name, that.name)
        && Objects.equals(file, that.file)
        && Objects.equals(line, that.line)
        && Objects.equals(column, that.column);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, file, line, column);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (name != null) {
      sb.append(name);
    }
    if (file != null || line != null) {
      sb.append(" at ");
      if (file != null) {
        sb.append(file);
      }
      if (line != null) {
        sb.append(":").append(line);
        if (column != null) {
          sb.append(":").append(column);
        }
      }
    }
    return sb.length() > 0 ? sb.toString() : "<unknown>";
  }
}
