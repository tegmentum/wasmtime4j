package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/** Result of an export operation. */
final class ExportResult {
  private final boolean successful;
  private final String message;
  private final long itemsProcessed;
  private final long bytesWritten;

  public ExportResult(
      final boolean successful,
      final String message,
      final long itemsProcessed,
      final long bytesWritten) {
    this.successful = successful;
    this.message = Objects.requireNonNull(message, "message cannot be null");
    this.itemsProcessed = itemsProcessed;
    this.bytesWritten = bytesWritten;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public String getMessage() {
    return message;
  }

  public long getItemsProcessed() {
    return itemsProcessed;
  }

  public long getBytesWritten() {
    return bytesWritten;
  }

  @Override
  public String toString() {
    return String.format(
        "ExportResult{successful=%s, items=%d, bytes=%d, message='%s'}",
        successful, itemsProcessed, bytesWritten, message);
  }
}
