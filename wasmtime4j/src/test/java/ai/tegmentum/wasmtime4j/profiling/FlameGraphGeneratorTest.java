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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.profiling.FlameGraphGenerator.FlameFrame;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for FlameGraphGenerator. */
@DisplayName("FlameGraphGenerator Tests")
class FlameGraphGeneratorTest {

  private static final Logger LOGGER = Logger.getLogger(FlameGraphGeneratorTest.class.getName());

  private FlameGraphGenerator generator;

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up FlameGraphGenerator test");
    generator = new FlameGraphGenerator();
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Tearing down FlameGraphGenerator test");
    if (generator != null) {
      generator.clear();
    }
  }

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("Should create generator with default max samples")
    void shouldCreateGeneratorWithDefaultMaxSamples() {
      LOGGER.info("Testing default generator creation");

      FlameGraphGenerator defaultGenerator = new FlameGraphGenerator();
      assertThat(defaultGenerator.getSampleCount()).isZero();
    }

    @Test
    @DisplayName("Should create generator with custom max samples")
    void shouldCreateGeneratorWithCustomMaxSamples() {
      LOGGER.info("Testing custom max samples generator creation");

      FlameGraphGenerator customGenerator = new FlameGraphGenerator(500);
      assertThat(customGenerator.getSampleCount()).isZero();
    }

    @Test
    @DisplayName("Should use default when max samples is zero or negative")
    void shouldUseDefaultWhenMaxSamplesIsZeroOrNegative() {
      LOGGER.info("Testing invalid max samples handling");

      FlameGraphGenerator zeroGenerator = new FlameGraphGenerator(0);
      FlameGraphGenerator negativeGenerator = new FlameGraphGenerator(-100);

      // Both should work without error
      assertThat(zeroGenerator.getSampleCount()).isZero();
      assertThat(negativeGenerator.getSampleCount()).isZero();
    }
  }

  @Nested
  @DisplayName("Sample Recording Tests")
  class SampleRecordingTests {

    @Test
    @DisplayName("Should record sample with all parameters")
    void shouldRecordSampleWithAllParameters() {
      LOGGER.info("Testing sample recording with all parameters");

      List<String> stackTrace = List.of("main", "processData", "computeHash");
      Map<String, String> metadata = Map.of("module", "data-processor", "version", "1.0");

      long sampleId =
          generator.recordSample(Duration.ofMillis(10), stackTrace, "main-thread", metadata);

      assertThat(sampleId).isPositive();
      assertThat(generator.getSampleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record sample with null thread name")
    void shouldRecordSampleWithNullThreadName() {
      LOGGER.info("Testing sample recording with null thread name");

      long sampleId = generator.recordSample(Duration.ofMillis(5), List.of("func1"), null, null);

      assertThat(sampleId).isPositive();
      assertThat(generator.getSampleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject null duration")
    void shouldRejectNullDuration() {
      LOGGER.info("Testing rejection of null duration");

      assertThatThrownBy(() -> generator.recordSample(null, List.of("func"), "thread", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Duration cannot be null");
    }

    @Test
    @DisplayName("Should reject null stack trace")
    void shouldRejectNullStackTrace() {
      LOGGER.info("Testing rejection of null stack trace");

      assertThatThrownBy(() -> generator.recordSample(Duration.ofMillis(10), null, "thread", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Stack trace cannot be null");
    }

    @Test
    @DisplayName("Should record multiple samples")
    void shouldRecordMultipleSamples() {
      LOGGER.info("Testing multiple sample recording");

      for (int i = 0; i < 100; i++) {
        generator.recordSample(
            Duration.ofMillis(i + 1), List.of("main", "func" + i), "thread-" + i, null);
      }

      assertThat(generator.getSampleCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should enforce max samples limit")
    void shouldEnforceMaxSamplesLimit() {
      LOGGER.info("Testing max samples limit enforcement");

      FlameGraphGenerator smallGenerator = new FlameGraphGenerator(10);

      for (int i = 0; i < 20; i++) {
        smallGenerator.recordSample(Duration.ofMillis(1), List.of("func" + i), "thread", null);
      }

      assertThat(smallGenerator.getSampleCount()).isLessThanOrEqualTo(10);
    }
  }

  @Nested
  @DisplayName("Flame Graph Generation Tests")
  class FlameGraphGenerationTests {

    @Test
    @DisplayName("Should generate empty flame graph with no samples")
    void shouldGenerateEmptyFlameGraphWithNoSamples() {
      LOGGER.info("Testing empty flame graph generation");

      FlameFrame root = generator.generateFlameGraph();

      assertThat(root).isNotNull();
      assertThat(root.getName()).isEqualTo("all");
      assertThat(root.getTotalTime()).isEqualTo(Duration.ZERO);
      assertThat(root.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("Should generate flame graph with single sample")
    void shouldGenerateFlameGraphWithSingleSample() {
      LOGGER.info("Testing single sample flame graph");

      generator.recordSample(Duration.ofMillis(10), List.of("main"), "thread", null);

      FlameFrame root = generator.generateFlameGraph();

      assertThat(root.getName()).isEqualTo("all");
      assertThat(root.getTotalTime()).isEqualTo(Duration.ofMillis(10));
      assertThat(root.getChildren()).hasSize(1);

      FlameFrame mainFrame = root.getChildren().get(0);
      assertThat(mainFrame.getName()).isEqualTo("main");
      assertThat(mainFrame.getTotalTime()).isEqualTo(Duration.ofMillis(10));
    }

    @Test
    @DisplayName("Should generate flame graph with nested stack trace")
    void shouldGenerateFlameGraphWithNestedStackTrace() {
      LOGGER.info("Testing nested stack trace flame graph");

      generator.recordSample(
          Duration.ofMillis(100), List.of("main", "processData", "parseJson"), "thread", null);

      FlameFrame root = generator.generateFlameGraph();

      assertThat(root.getChildren()).hasSize(1);

      FlameFrame main = root.getChildren().get(0);
      assertThat(main.getName()).isEqualTo("main");
      assertThat(main.getChildren()).hasSize(1);

      FlameFrame processData = main.getChildren().get(0);
      assertThat(processData.getName()).isEqualTo("processData");
      assertThat(processData.getChildren()).hasSize(1);

      FlameFrame parseJson = processData.getChildren().get(0);
      assertThat(parseJson.getName()).isEqualTo("parseJson");
      assertThat(parseJson.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("Should aggregate time for same function calls")
    void shouldAggregateTimeForSameFunctionCalls() {
      LOGGER.info("Testing time aggregation for same function");

      generator.recordSample(Duration.ofMillis(10), List.of("main", "helper"), "t1", null);
      generator.recordSample(Duration.ofMillis(20), List.of("main", "helper"), "t2", null);
      generator.recordSample(Duration.ofMillis(30), List.of("main", "other"), "t3", null);

      FlameFrame root = generator.generateFlameGraph();

      assertThat(root.getTotalTime()).isEqualTo(Duration.ofMillis(60));

      FlameFrame main = root.getChildren().get(0);
      assertThat(main.getName()).isEqualTo("main");
      assertThat(main.getTotalTime()).isEqualTo(Duration.ofMillis(60));
      assertThat(main.getChildren()).hasSize(2);

      // Find helper frame
      FlameFrame helper =
          main.getChildren().stream()
              .filter(f -> f.getName().equals("helper"))
              .findFirst()
              .orElseThrow();
      assertThat(helper.getTotalTime()).isEqualTo(Duration.ofMillis(30));
    }
  }

  @Nested
  @DisplayName("SVG Generation Tests")
  class SvgGenerationTests {

    @Test
    @DisplayName("Should generate SVG from flame graph")
    void shouldGenerateSvgFromFlameGraph() {
      LOGGER.info("Testing SVG generation");

      generator.recordSample(Duration.ofMillis(50), List.of("main", "compute"), "thread", null);
      generator.recordSample(Duration.ofMillis(30), List.of("main", "io"), "thread", null);

      FlameFrame root = generator.generateFlameGraph();
      String svg = generator.generateSvg(root);

      assertThat(svg).isNotNull();
      assertThat(svg).startsWith("<?xml version=\"1.0\"");
      assertThat(svg).contains("<svg");
      assertThat(svg).contains("</svg>");
      assertThat(svg).contains("main");
    }

    @Test
    @DisplayName("Should reject null root frame for SVG generation")
    void shouldRejectNullRootFrameForSvgGeneration() {
      LOGGER.info("Testing SVG generation with null root");

      assertThatThrownBy(() -> generator.generateSvg(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Root frame cannot be null");
    }

    @Test
    @DisplayName("Should generate SVG with proper XML escaping in text elements")
    void shouldGenerateSvgWithProperXmlEscaping() {
      LOGGER.info("Testing XML escaping in SVG");

      generator.recordSample(
          Duration.ofMillis(100), List.of("func<with>special&chars"), "thread", null);

      FlameFrame root = generator.generateFlameGraph();
      String svg = generator.generateSvg(root);

      // Text elements should contain escaped characters
      assertThat(svg).contains("&lt;");
      assertThat(svg).contains("&gt;");
      assertThat(svg).contains("&amp;");
      // Verify SVG structure is valid
      assertThat(svg).contains("<text");
      assertThat(svg).contains("</text>");
    }
  }

  @Nested
  @DisplayName("FlameFrame Tests")
  class FlameFrameTests {

    @Test
    @DisplayName("Should create flame frame with valid parameters")
    void shouldCreateFlameFrameWithValidParameters() {
      LOGGER.info("Testing FlameFrame creation");

      List<FlameFrame> children = new ArrayList<>();
      FlameFrame frame = new FlameFrame("testFunc", Duration.ofMillis(100), children);

      assertThat(frame.getName()).isEqualTo("testFunc");
      assertThat(frame.getTotalTime()).isEqualTo(Duration.ofMillis(100));
      assertThat(frame.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("Should reject null name")
    void shouldRejectNullName() {
      LOGGER.info("Testing FlameFrame null name rejection");

      assertThatThrownBy(() -> new FlameFrame(null, Duration.ofMillis(10), new ArrayList<>()))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Name cannot be null");
    }

    @Test
    @DisplayName("Should reject null total time")
    void shouldRejectNullTotalTime() {
      LOGGER.info("Testing FlameFrame null time rejection");

      assertThatThrownBy(() -> new FlameFrame("func", null, new ArrayList<>()))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Total time cannot be null");
    }

    @Test
    @DisplayName("Should handle null children list")
    void shouldHandleNullChildrenList() {
      LOGGER.info("Testing FlameFrame with null children");

      FlameFrame frame = new FlameFrame("func", Duration.ofMillis(10), null);

      assertThat(frame.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("Should return immutable children list")
    void shouldReturnImmutableChildrenList() {
      LOGGER.info("Testing FlameFrame immutable children");

      FlameFrame frame = new FlameFrame("func", Duration.ofMillis(10), new ArrayList<>());
      List<FlameFrame> children = frame.getChildren();

      assertThatThrownBy(() -> children.add(new FlameFrame("child", Duration.ZERO, null)))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
      LOGGER.info("Testing FlameFrame toString");

      FlameFrame frame = new FlameFrame("myFunc", Duration.ofMillis(50), new ArrayList<>());
      String str = frame.toString();

      assertThat(str).contains("myFunc");
      assertThat(str).contains("FlameFrame");
    }
  }

  @Nested
  @DisplayName("Clear Tests")
  class ClearTests {

    @Test
    @DisplayName("Should clear all samples")
    void shouldClearAllSamples() {
      LOGGER.info("Testing sample clearing");

      for (int i = 0; i < 10; i++) {
        generator.recordSample(Duration.ofMillis(1), List.of("func" + i), "thread", null);
      }

      assertThat(generator.getSampleCount()).isEqualTo(10);

      generator.clear();

      assertThat(generator.getSampleCount()).isZero();
    }

    @Test
    @DisplayName("Should generate empty flame graph after clear")
    void shouldGenerateEmptyFlameGraphAfterClear() {
      LOGGER.info("Testing flame graph after clear");

      generator.recordSample(Duration.ofMillis(100), List.of("main"), "thread", null);
      generator.clear();

      FlameFrame root = generator.generateFlameGraph();

      assertThat(root.getTotalTime()).isEqualTo(Duration.ZERO);
      assertThat(root.getChildren()).isEmpty();
    }
  }
}
