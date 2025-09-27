package ai.tegmentum.wasmtime4j.toolchain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Request for WebAssembly linking operations.
 *
 * <p>Defines parameters for linking multiple WebAssembly object files
 * or modules into a single executable module.
 *
 * @since 1.0.0
 */
public final class LinkingRequest {

  private final List<Path> objectFiles;
  private final Path outputPath;
  private final List<Path> libraryPaths;
  private final Map<String, String> linkingFlags;
  private final Optional<Duration> timeout;
  private final boolean stripDebugInfo;
  private final boolean optimizeForSize;
  private final String targetTriple;
  private final LinkingMode linkingMode;

  private LinkingRequest(final Builder builder) {
    this.objectFiles = List.copyOf(builder.objectFiles);
    this.outputPath = Objects.requireNonNull(builder.outputPath);
    this.libraryPaths = List.copyOf(builder.libraryPaths);
    this.linkingFlags = Map.copyOf(builder.linkingFlags);
    this.timeout = Optional.ofNullable(builder.timeout);
    this.stripDebugInfo = builder.stripDebugInfo;
    this.optimizeForSize = builder.optimizeForSize;
    this.targetTriple = Objects.requireNonNull(builder.targetTriple);
    this.linkingMode = Objects.requireNonNull(builder.linkingMode);
  }

  /**
   * Creates a new linking request builder.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the object files to link.
   *
   * @return list of object file paths
   */
  public List<Path> getObjectFiles() {
    return objectFiles;
  }

  /**
   * Gets the output path for the linked module.
   *
   * @return output path
   */
  public Path getOutputPath() {
    return outputPath;
  }

  /**
   * Gets the library search paths.
   *
   * @return list of library paths
   */
  public List<Path> getLibraryPaths() {
    return libraryPaths;
  }

  /**
   * Gets the linking flags.
   *
   * @return map of linking flags
   */
  public Map<String, String> getLinkingFlags() {
    return linkingFlags;
  }

  /**
   * Gets the linking timeout.
   *
   * @return timeout duration, or empty if no timeout
   */
  public Optional<Duration> getTimeout() {
    return timeout;
  }

  /**
   * Checks if debug information should be stripped.
   *
   * @return true if debug info should be stripped
   */
  public boolean isStripDebugInfo() {
    return stripDebugInfo;
  }

  /**
   * Checks if linking should optimize for size.
   *
   * @return true if optimizing for size
   */
  public boolean isOptimizeForSize() {
    return optimizeForSize;
  }

  /**
   * Gets the target triple.
   *
   * @return target triple
   */
  public String getTargetTriple() {
    return targetTriple;
  }

  /**
   * Gets the linking mode.
   *
   * @return linking mode
   */
  public LinkingMode getLinkingMode() {
    return linkingMode;
  }

  /**
   * Builder for linking requests.
   */
  public static final class Builder {
    private List<Path> objectFiles = List.of();
    private Path outputPath;
    private List<Path> libraryPaths = List.of();
    private Map<String, String> linkingFlags = Map.of();
    private Duration timeout;
    private boolean stripDebugInfo = false;
    private boolean optimizeForSize = false;
    private String targetTriple = "wasm32-unknown-unknown";
    private LinkingMode linkingMode = LinkingMode.STATIC;

    public Builder objectFiles(final List<Path> objectFiles) {
      this.objectFiles = Objects.requireNonNull(objectFiles);
      return this;
    }

    public Builder addObjectFile(final Path objectFile) {
      this.objectFiles = List.copyOf(
          java.util.stream.Stream.concat(
              objectFiles.stream(),
              java.util.stream.Stream.of(Objects.requireNonNull(objectFile))
          ).toList()
      );
      return this;
    }

    public Builder outputPath(final Path outputPath) {
      this.outputPath = Objects.requireNonNull(outputPath);
      return this;
    }

    public Builder libraryPaths(final List<Path> libraryPaths) {
      this.libraryPaths = Objects.requireNonNull(libraryPaths);
      return this;
    }

    public Builder addLibraryPath(final Path libraryPath) {
      this.libraryPaths = List.copyOf(
          java.util.stream.Stream.concat(
              libraryPaths.stream(),
              java.util.stream.Stream.of(Objects.requireNonNull(libraryPath))
          ).toList()
      );
      return this;
    }

    public Builder linkingFlags(final Map<String, String> linkingFlags) {
      this.linkingFlags = Map.copyOf(Objects.requireNonNull(linkingFlags));
      return this;
    }

    public Builder addLinkingFlag(final String name, final String value) {
      final var newFlags = new java.util.HashMap<>(linkingFlags);
      newFlags.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
      this.linkingFlags = Map.copyOf(newFlags);
      return this;
    }

    public Builder timeout(final Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder stripDebugInfo(final boolean stripDebugInfo) {
      this.stripDebugInfo = stripDebugInfo;
      return this;
    }

    public Builder optimizeForSize(final boolean optimizeForSize) {
      this.optimizeForSize = optimizeForSize;
      return this;
    }

    public Builder targetTriple(final String targetTriple) {
      this.targetTriple = Objects.requireNonNull(targetTriple);
      return this;
    }

    public Builder linkingMode(final LinkingMode linkingMode) {
      this.linkingMode = Objects.requireNonNull(linkingMode);
      return this;
    }

    public LinkingRequest build() {
      if (objectFiles.isEmpty()) {
        throw new IllegalStateException("At least one object file must be specified");
      }
      if (outputPath == null) {
        throw new IllegalStateException("Output path must be specified");
      }
      return new LinkingRequest(this);
    }
  }

  /**
   * Linking modes for WebAssembly modules.
   */
  public enum LinkingMode {
    /** Static linking - all dependencies bundled */
    STATIC,

    /** Dynamic linking - dependencies loaded at runtime */
    DYNAMIC,

    /** Shared library linking */
    SHARED
  }

  @Override
  public String toString() {
    return String.format("LinkingRequest{objects=%d, output=%s, mode=%s, target=%s}",
        objectFiles.size(), outputPath, linkingMode, targetTriple);
  }
}