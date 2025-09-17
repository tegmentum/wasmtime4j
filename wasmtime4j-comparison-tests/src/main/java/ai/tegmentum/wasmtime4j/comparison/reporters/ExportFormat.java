package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Supported export formats.
 *
 * @since 1.0.0
 */
public enum ExportFormat {
  JSON("application/json", ".json"),
  CSV("text/csv", ".csv"),
  XML("application/xml", ".xml"),
  HTML("text/html", ".html"),
  BUNDLE("application/zip", ".zip"),
  PDF("application/pdf", ".pdf");

  private final String mimeType;
  private final String fileExtension;

  ExportFormat(final String mimeType, final String fileExtension) {
    this.mimeType = mimeType;
    this.fileExtension = fileExtension;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getFileExtension() {
    return fileExtension;
  }
}
