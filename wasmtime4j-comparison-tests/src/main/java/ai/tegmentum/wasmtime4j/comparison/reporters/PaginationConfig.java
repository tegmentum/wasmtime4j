package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Pagination configuration.
 *
 * @since 1.0.0
 */
public final class PaginationConfig {
  private final int pageNumber;
  private final int pageSize;

  /**
   * Creates a new pagination configuration.
   *
   * @param pageNumber the page number (zero-based)
   * @param pageSize the size of each page
   */
  public PaginationConfig(final int pageNumber, final int pageSize) {
    if (pageNumber < 0) {
      throw new IllegalArgumentException("Page number cannot be negative");
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("Page size must be positive");
    }

    this.pageNumber = pageNumber;
    this.pageSize = pageSize;
  }

  public int getPageNumber() {
    return pageNumber;
  }

  public int getPageSize() {
    return pageSize;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final PaginationConfig that = (PaginationConfig) obj;
    return pageNumber == that.pageNumber && pageSize == that.pageSize;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pageNumber, pageSize);
  }

  @Override
  public String toString() {
    return "PaginationConfig{" + "page=" + pageNumber + ", size=" + pageSize + '}';
  }
}
