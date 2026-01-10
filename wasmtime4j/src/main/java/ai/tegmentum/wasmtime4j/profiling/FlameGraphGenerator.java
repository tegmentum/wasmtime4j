/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.profiling;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generator for flame graph visualizations of profiling data.
 *
 * <p>This class collects profiling samples and generates flame graph data structures that can be
 * rendered as SVG visualizations or processed for performance analysis.
 *
 * <p>Flame graphs are useful for identifying:
 *
 * <ul>
 *   <li>Hot spots in WebAssembly code execution
 *   <li>CPU-intensive functions
 *   <li>Call stack patterns
 *   <li>Performance bottlenecks
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FlameGraphGenerator generator = new FlameGraphGenerator();
 *
 * // Record samples during execution
 * generator.recordSample(
 *     Duration.ofMillis(10),
 *     List.of("main", "processData", "computeHash"),
 *     "main-thread",
 *     Map.of("module", "data-processor"));
 *
 * // Generate flame graph
 * FlameFrame root = generator.generateFlameGraph();
 * String svg = generator.generateSvg(root);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class FlameGraphGenerator {

  /** Default maximum number of samples to retain. */
  private static final int DEFAULT_MAX_SAMPLES = 100_000;

  private final AtomicLong sampleIdGenerator;
  private final Map<Long, SampleRecord> samples;
  private final Map<String, FlameFrame> frameCache;
  private final int maxSamples;

  /** Creates a new flame graph generator with default max samples. */
  public FlameGraphGenerator() {
    this(DEFAULT_MAX_SAMPLES);
  }

  /**
   * Creates a new flame graph generator with specified max samples.
   *
   * @param maxSamples maximum number of samples to retain
   */
  public FlameGraphGenerator(final int maxSamples) {
    this.sampleIdGenerator = new AtomicLong(0);
    this.samples = new ConcurrentHashMap<>();
    this.frameCache = new ConcurrentHashMap<>();
    this.maxSamples = maxSamples > 0 ? maxSamples : DEFAULT_MAX_SAMPLES;
  }

  /**
   * Records a profiling sample.
   *
   * @param duration the duration of the sample
   * @param stackTrace the call stack (from top to bottom)
   * @param threadName the thread name
   * @param metadata additional metadata
   * @return a unique sample ID
   */
  public long recordSample(
      final Duration duration,
      final List<String> stackTrace,
      final String threadName,
      final Map<String, String> metadata) {
    Objects.requireNonNull(duration, "Duration cannot be null");
    Objects.requireNonNull(stackTrace, "Stack trace cannot be null");

    // Enforce maxSamples limit by removing oldest entries
    while (samples.size() >= maxSamples) {
      final Long oldestKey = samples.keySet().stream().min(Long::compareTo).orElse(null);
      if (oldestKey != null) {
        samples.remove(oldestKey);
      } else {
        break;
      }
    }

    final long id = sampleIdGenerator.incrementAndGet();
    final SampleRecord record =
        new SampleRecord(
            id,
            duration,
            new ArrayList<>(stackTrace),
            threadName != null ? threadName : "unknown",
            metadata != null ? new HashMap<>(metadata) : new HashMap<>());
    samples.put(id, record);
    return id;
  }

  /**
   * Generates a flame graph from the collected samples.
   *
   * @return the root frame of the flame graph
   */
  public FlameFrame generateFlameGraph() {
    final FlameFrame root = new FlameFrame("all", Duration.ZERO, new ArrayList<>());

    for (final SampleRecord sample : samples.values()) {
      addSampleToFrame(root, sample.getStackTrace(), sample.getDuration(), 0);
    }

    return root;
  }

  private void addSampleToFrame(
      final FlameFrame frame, final List<String> stack, final Duration duration, final int index) {
    frame.addTime(duration);

    if (index < stack.size()) {
      final String functionName = stack.get(index);
      FlameFrame child = null;

      for (final FlameFrame existingChild : frame.getChildren()) {
        if (existingChild.getName().equals(functionName)) {
          child = existingChild;
          break;
        }
      }

      if (child == null) {
        child = new FlameFrame(functionName, Duration.ZERO, new ArrayList<>());
        frame.addChild(child);
      }

      addSampleToFrame(child, stack, duration, index + 1);
    }
  }

  /**
   * Generates an SVG representation of the flame graph.
   *
   * @param root the root frame of the flame graph
   * @return the SVG as a string
   */
  public String generateSvg(final FlameFrame root) {
    Objects.requireNonNull(root, "Root frame cannot be null");

    final StringBuilder svg = new StringBuilder();
    final int width = 1200;
    final int height = calculateHeight(root) * 20 + 40;

    svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append('\n');
    svg.append(
            String.format(
                "<svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\">",
                width, height))
        .append('\n');
    svg.append("<style>text { font-family: monospace; font-size: 12px; }</style>").append('\n');
    svg.append(String.format("<rect width=\"%d\" height=\"%d\" fill=\"#f8f8f8\"/>", width, height))
        .append('\n');

    renderFrame(svg, root, 0, 0, width, root.getTotalTime().toNanos());

    svg.append("</svg>");
    return svg.toString();
  }

  private int calculateHeight(final FlameFrame frame) {
    int maxChildHeight = 0;
    for (final FlameFrame child : frame.getChildren()) {
      maxChildHeight = Math.max(maxChildHeight, calculateHeight(child));
    }
    return 1 + maxChildHeight;
  }

  private void renderFrame(
      final StringBuilder svg,
      final FlameFrame frame,
      final int depth,
      final double x,
      final double width,
      final long totalNanos) {
    if (totalNanos == 0) {
      return;
    }

    final double frameWidth = width * ((double) frame.getTotalTime().toNanos() / totalNanos);
    if (frameWidth < 1) {
      return;
    }

    final int y = depth * 20 + 20;
    final String color = getColorForFrame(frame.getName());

    svg.append(
            String.format(
                "<rect x=\"%.1f\" y=\"%d\" width=\"%.1f\" height=\"18\" fill=\"%s\" stroke=\"#333\""
                    + " stroke-width=\"0.5\">",
                x, y, frameWidth, color))
        .append('\n');
    svg.append(
            String.format(
                "<title>%s (%.2f ms)</title>",
                frame.getName(), frame.getTotalTime().toNanos() / 1_000_000.0))
        .append('\n');
    svg.append("</rect>").append('\n');

    if (frameWidth > 50) {
      final String displayName =
          frame.getName().length() > (int) (frameWidth / 7)
              ? frame.getName().substring(0, Math.max(0, (int) (frameWidth / 7) - 3)) + "..."
              : frame.getName();
      svg.append(
              String.format(
                  "<text x=\"%.1f\" y=\"%d\" fill=\"#333\">%s</text>",
                  x + 2, y + 13, escapeXml(displayName)))
          .append('\n');
    }

    double childX = x;
    for (final FlameFrame child : frame.getChildren()) {
      final double childWidth =
          frameWidth * ((double) child.getTotalTime().toNanos() / frame.getTotalTime().toNanos());
      renderFrame(svg, child, depth + 1, childX, childWidth, frame.getTotalTime().toNanos());
      childX += childWidth;
    }
  }

  private String getColorForFrame(final String name) {
    // Generate a consistent color based on the frame name
    final int hash = name.hashCode();
    final int r = 200 + (hash & 0x3F);
    final int g = 100 + ((hash >> 6) & 0x7F);
    final int b = 50 + ((hash >> 13) & 0x3F);
    return String.format("#%02x%02x%02x", Math.min(255, r), Math.min(255, g), Math.min(255, b));
  }

  private String escapeXml(final String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /**
   * Gets the number of samples collected.
   *
   * @return the sample count
   */
  public long getSampleCount() {
    return samples.size();
  }

  /** Clears all collected samples. */
  public void clear() {
    samples.clear();
    frameCache.clear();
  }

  /** A frame in a flame graph representing a function or scope in the call stack. */
  public static final class FlameFrame {

    private final String name;
    private Duration totalTime;
    private final List<FlameFrame> children;

    /**
     * Creates a new flame frame.
     *
     * @param name the function or scope name
     * @param totalTime the total time spent in this frame
     * @param children the child frames
     */
    public FlameFrame(
        final String name, final Duration totalTime, final List<FlameFrame> children) {
      this.name = Objects.requireNonNull(name, "Name cannot be null");
      this.totalTime = Objects.requireNonNull(totalTime, "Total time cannot be null");
      this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
    }

    /**
     * Gets the frame name.
     *
     * @return the frame name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the total time spent in this frame.
     *
     * @return the total time spent in this frame
     */
    public Duration getTotalTime() {
      return totalTime;
    }

    /**
     * Gets the child frames (immutable view).
     *
     * @return the child frames (immutable view)
     */
    public List<FlameFrame> getChildren() {
      return Collections.unmodifiableList(children);
    }

    void addTime(final Duration duration) {
      this.totalTime = this.totalTime.plus(duration);
    }

    void addChild(final FlameFrame child) {
      this.children.add(child);
    }

    @Override
    public String toString() {
      return String.format(
          "FlameFrame{name='%s', totalTime=%s, children=%d}", name, totalTime, children.size());
    }
  }

  /** Internal record for profiling samples. */
  private static final class SampleRecord {

    private final long id;
    private final Duration duration;
    private final List<String> stackTrace;
    private final String threadName;
    private final Map<String, String> metadata;

    SampleRecord(
        final long id,
        final Duration duration,
        final List<String> stackTrace,
        final String threadName,
        final Map<String, String> metadata) {
      this.id = id;
      this.duration = duration;
      this.stackTrace = stackTrace;
      this.threadName = threadName;
      this.metadata = metadata;
    }

    long getId() {
      return id;
    }

    Duration getDuration() {
      return duration;
    }

    List<String> getStackTrace() {
      return stackTrace;
    }

    String getThreadName() {
      return threadName;
    }

    Map<String, String> getMetadata() {
      return metadata;
    }
  }
}
