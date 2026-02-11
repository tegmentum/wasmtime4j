package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.metadata.CustomSection;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionParser;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionSecurity;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionType;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionValidationResult;
import ai.tegmentum.wasmtime4j.metadata.DefaultCustomSectionMetadata;
import ai.tegmentum.wasmtime4j.metadata.DefaultCustomSectionParser;
import ai.tegmentum.wasmtime4j.metadata.NameSection;
import ai.tegmentum.wasmtime4j.metadata.ProducersSection;
import ai.tegmentum.wasmtime4j.metadata.TargetFeaturesSection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark for WebAssembly custom section operations.
 *
 * <p>This benchmark measures the performance of custom section parsing, validation, and metadata
 * extraction operations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class CustomSectionBenchmark {

  private List<CustomSection> smallSections;
  private List<CustomSection> largeSections;
  private List<CustomSection> manySections;
  private CustomSectionParser parser;
  private DefaultCustomSectionMetadata smallMetadata;
  private DefaultCustomSectionMetadata largeMetadata;
  private NameSection complexNameSection;
  private ProducersSection complexProducersSection;
  private TargetFeaturesSection complexTargetFeaturesSection;

  @Setup
  public void setup() {
    parser = new DefaultCustomSectionParser();
    setupSmallSections();
    setupLargeSections();
    setupManySections();
    setupComplexSections();
    setupMetadataObjects();
  }

  @Benchmark
  public void benchmarkSmallSectionValidation(final Blackhole bh) {
    for (final CustomSection section : smallSections) {
      final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(section);
      bh.consume(result);
    }
  }

  @Benchmark
  public void benchmarkLargeSectionValidation(final Blackhole bh) {
    for (final CustomSection section : largeSections) {
      final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(section);
      bh.consume(result);
    }
  }

  @Benchmark
  public void benchmarkManySectionsValidation(final Blackhole bh) {
    final CustomSectionValidationResult result =
        CustomSectionSecurity.validateSecurity(manySections);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkNameSectionParsing(final Blackhole bh) {
    final byte[] nameData = createNameSectionData();
    final java.util.Optional<NameSection> result = parser.parseNameSection(nameData);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkProducersSectionParsing(final Blackhole bh) {
    final byte[] producersData = createProducersSectionData();
    final java.util.Optional<ProducersSection> result = parser.parseProducersSection(producersData);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkTargetFeaturesSectionParsing(final Blackhole bh) {
    final byte[] targetFeaturesData = createTargetFeaturesSectionData();
    final java.util.Optional<TargetFeaturesSection> result =
        parser.parseTargetFeaturesSection(targetFeaturesData);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkNameSectionSerialization(final Blackhole bh) {
    final java.util.Optional<byte[]> result = parser.serializeNameSection(complexNameSection);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkProducersSectionSerialization(final Blackhole bh) {
    final java.util.Optional<byte[]> result =
        parser.serializeProducersSection(complexProducersSection);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkTargetFeaturesSectionSerialization(final Blackhole bh) {
    final java.util.Optional<byte[]> result =
        parser.serializeTargetFeaturesSection(complexTargetFeaturesSection);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkMetadataAccess(final Blackhole bh) {
    bh.consume(smallMetadata.getAllCustomSections());
    bh.consume(smallMetadata.getCustomSectionNames());
    bh.consume(smallMetadata.getCustomSectionTypes());
    bh.consume(smallMetadata.getCustomSectionCount());
    bh.consume(smallMetadata.getCustomSectionsTotalSize());
  }

  @Benchmark
  public void benchmarkMetadataSearch(final Blackhole bh) {
    bh.consume(largeMetadata.getCustomSectionsByName("name"));
    bh.consume(largeMetadata.getCustomSectionsByType(CustomSectionType.PRODUCERS));
    bh.consume(largeMetadata.getFirstCustomSection("target_features"));
    bh.consume(largeMetadata.hasCustomSection("name"));
    bh.consume(largeMetadata.hasCustomSection(CustomSectionType.DWARF));
  }

  @Benchmark
  public void benchmarkComplexMetadataOperations(final Blackhole bh) {
    bh.consume(largeMetadata.getNameSection());
    bh.consume(largeMetadata.getProducersSection());
    bh.consume(largeMetadata.getTargetFeaturesSection());
    bh.consume(largeMetadata.getDebuggingSections());
    bh.consume(largeMetadata.hasDebuggingInfo());
    bh.consume(largeMetadata.getCustomSectionsSummary());
  }

  @Benchmark
  public void benchmarkFullValidation(final Blackhole bh) {
    final CustomSectionValidationResult result = smallMetadata.validateCustomSections();
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkSuspiciousPatternDetection(final Blackhole bh) {
    for (final CustomSection section : largeSections) {
      final boolean suspicious =
          CustomSectionSecurity.containsSuspiciousPatterns(section.getData());
      bh.consume(suspicious);
    }
  }

  @Benchmark
  public void benchmarkSectionNameClassification(final Blackhole bh) {
    final String[] testNames = {
      "name",
      "producers",
      "target_features",
      ".debug_info",
      ".debug_line",
      "custom_section",
      "my_data",
      "application_metadata",
      "unknown"
    };

    for (final String name : testNames) {
      final CustomSectionType type = CustomSectionType.fromName(name);
      bh.consume(type);
    }
  }

  private void setupSmallSections() {
    smallSections = new ArrayList<>();
    smallSections.add(new CustomSection("name", createNameSectionData(), CustomSectionType.NAME));
    smallSections.add(
        new CustomSection("producers", createProducersSectionData(), CustomSectionType.PRODUCERS));
    smallSections.add(
        new CustomSection(
            "target_features",
            createTargetFeaturesSectionData(),
            CustomSectionType.TARGET_FEATURES));
  }

  private void setupLargeSections() {
    largeSections = new ArrayList<>();

    // Create larger sections with more data
    final byte[] largeNameData = createLargeNameSectionData();
    final byte[] largeProducersData = createLargeProducersSectionData();
    final byte[] customData = createLargeCustomData();

    largeSections.add(new CustomSection("name", largeNameData, CustomSectionType.NAME));
    largeSections.add(
        new CustomSection("producers", largeProducersData, CustomSectionType.PRODUCERS));
    largeSections.add(new CustomSection("custom_large", customData, CustomSectionType.UNKNOWN));
  }

  private void setupManySections() {
    manySections = new ArrayList<>();

    // Create many small sections
    for (int i = 0; i < 100; i++) {
      final String name = "section_" + i;
      final byte[] data = ("data for section " + i).getBytes(StandardCharsets.UTF_8);
      manySections.add(new CustomSection(name, data, CustomSectionType.UNKNOWN));
    }

    // Add some standard sections
    manySections.add(new CustomSection("name", createNameSectionData(), CustomSectionType.NAME));
    manySections.add(
        new CustomSection("producers", createProducersSectionData(), CustomSectionType.PRODUCERS));
  }

  private void setupComplexSections() {
    // Create complex name section
    final Map<Integer, String> functionNames = new HashMap<>();
    final Map<Integer, Map<Integer, String>> localNames = new HashMap<>();
    final Map<Integer, String> typeNames = new HashMap<>();

    for (int i = 0; i < 50; i++) {
      functionNames.put(i, "function_" + i);
      typeNames.put(i, "type_" + i);

      final Map<Integer, String> locals = new HashMap<>();
      for (int j = 0; j < 10; j++) {
        locals.put(j, "local_" + j);
      }
      localNames.put(i, locals);
    }

    complexNameSection =
        NameSection.builder()
            .setModuleName("benchmark_module")
            .setFunctionNames(functionNames)
            .setLocalNames(localNames)
            .setTypeNames(typeNames)
            .build();

    // Create complex producers section
    final List<ProducersSection.ProducerEntry> languages = new ArrayList<>();
    final List<ProducersSection.ProducerEntry> tools = new ArrayList<>();

    languages.add(new ProducersSection.ProducerEntry("Rust", "1.70.0"));
    languages.add(new ProducersSection.ProducerEntry("C", "11"));
    languages.add(new ProducersSection.ProducerEntry("JavaScript", "ES2020"));

    tools.add(new ProducersSection.ProducerEntry("rustc", "1.70.0"));
    tools.add(new ProducersSection.ProducerEntry("wasm-pack", "0.12.0"));
    tools.add(new ProducersSection.ProducerEntry("wasmtime4j", "1.0.0"));

    complexProducersSection =
        ProducersSection.builder().setLanguages(languages).setProcessedBy(tools).build();

    // Create complex target features section
    complexTargetFeaturesSection =
        TargetFeaturesSection.builder()
            .addRequiredFeature("simd128")
            .addRequiredFeature("bulk-memory")
            .addUsedFeature("threads")
            .addUsedFeature("exception-handling")
            .addDisabledFeature("tail-call")
            .build();
  }

  private void setupMetadataObjects() {
    smallMetadata = new DefaultCustomSectionMetadata(smallSections, parser);
    largeMetadata = new DefaultCustomSectionMetadata(manySections, parser);
  }

  private byte[] createNameSectionData() {
    // Create a simple name section with module name and a few function names
    final List<Byte> data = new ArrayList<>();

    // Module name subsection (type 0)
    data.add((byte) 0); // subsection type
    final byte[] moduleNameData = encodeString("test_module");
    addULEB128(data, moduleNameData.length);
    addBytes(data, moduleNameData);

    // Function names subsection (type 1)
    data.add((byte) 1); // subsection type
    final List<Byte> functionNamesData = new ArrayList<>();
    addULEB128(functionNamesData, 3); // 3 functions
    addULEB128(functionNamesData, 0);
    addBytes(functionNamesData, encodeString("main"));
    addULEB128(functionNamesData, 1);
    addBytes(functionNamesData, encodeString("helper"));
    addULEB128(functionNamesData, 2);
    addBytes(functionNamesData, encodeString("cleanup"));

    final byte[] functionNamesBytes = toByteArray(functionNamesData);
    addULEB128(data, functionNamesBytes.length);
    addBytes(data, functionNamesBytes);

    return toByteArray(data);
  }

  private byte[] createProducersSectionData() {
    final List<Byte> data = new ArrayList<>();

    // Number of fields
    addULEB128(data, 2); // language and processed-by

    // Language field
    addBytes(data, encodeString("language"));
    addULEB128(data, 1); // 1 language entry
    addBytes(data, encodeString("Rust"));
    addBytes(data, encodeString("1.70.0"));

    // Processed-by field
    addBytes(data, encodeString("processed-by"));
    addULEB128(data, 1); // 1 tool entry
    addBytes(data, encodeString("rustc"));
    addBytes(data, encodeString("1.70.0"));

    return toByteArray(data);
  }

  private byte[] createTargetFeaturesSectionData() {
    final List<Byte> data = new ArrayList<>();

    // Number of features
    addULEB128(data, 3);

    // Required feature
    data.add((byte) 0x2B); // '+'
    addBytes(data, encodeString("simd128"));

    // Used feature
    data.add((byte) 0x3D); // '='
    addBytes(data, encodeString("bulk-memory"));

    // Disabled feature
    data.add((byte) 0x2D); // '-'
    addBytes(data, encodeString("tail-call"));

    return toByteArray(data);
  }

  private byte[] createLargeNameSectionData() {
    // Create a name section with many functions and locals
    final List<Byte> data = new ArrayList<>();

    // Function names subsection (type 1)
    data.add((byte) 1);
    final List<Byte> functionNamesData = new ArrayList<>();
    addULEB128(functionNamesData, 100); // 100 functions

    for (int i = 0; i < 100; i++) {
      addULEB128(functionNamesData, i);
      addBytes(functionNamesData, encodeString("function_" + i));
    }

    final byte[] functionNamesBytes = toByteArray(functionNamesData);
    addULEB128(data, functionNamesBytes.length);
    addBytes(data, functionNamesBytes);

    return toByteArray(data);
  }

  private byte[] createLargeProducersSectionData() {
    final List<Byte> data = new ArrayList<>();

    // Number of fields
    addULEB128(data, 3); // language, processed-by, and sdk

    // Language field with multiple entries
    addBytes(data, encodeString("language"));
    addULEB128(data, 5);
    addBytes(data, encodeString("Rust"));
    addBytes(data, encodeString("1.70.0"));
    addBytes(data, encodeString("C"));
    addBytes(data, encodeString("11"));
    addBytes(data, encodeString("JavaScript"));
    addBytes(data, encodeString("ES2020"));
    addBytes(data, encodeString("TypeScript"));
    addBytes(data, encodeString("4.9.0"));
    addBytes(data, encodeString("AssemblyScript"));
    addBytes(data, encodeString("0.20.0"));

    // Processed-by field with multiple tools
    addBytes(data, encodeString("processed-by"));
    addULEB128(data, 3);
    addBytes(data, encodeString("rustc"));
    addBytes(data, encodeString("1.70.0"));
    addBytes(data, encodeString("wasm-pack"));
    addBytes(data, encodeString("0.12.0"));
    addBytes(data, encodeString("wasmtime4j"));
    addBytes(data, encodeString("1.0.0"));

    // SDK field
    addBytes(data, encodeString("sdk"));
    addULEB128(data, 1);
    addBytes(data, encodeString("wasi-sdk"));
    addBytes(data, encodeString("16.0"));

    return toByteArray(data);
  }

  private byte[] createLargeCustomData() {
    // Create 64KB of custom data
    final byte[] data = new byte[64 * 1024];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 256);
    }
    return data;
  }

  private byte[] encodeString(final String str) {
    final byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
    final List<Byte> result = new ArrayList<>();
    addULEB128(result, strBytes.length);
    addBytes(result, strBytes);
    return toByteArray(result);
  }

  private void addULEB128(final List<Byte> data, int value) {
    do {
      byte b = (byte) (value & 0x7F);
      value >>>= 7;
      if (value != 0) {
        b |= 0x80;
      }
      data.add(b);
    } while (value != 0);
  }

  private void addBytes(final List<Byte> data, final byte[] bytes) {
    for (final byte b : bytes) {
      data.add(b);
    }
  }

  private byte[] toByteArray(final List<Byte> data) {
    final byte[] result = new byte[data.size()];
    for (int i = 0; i < data.size(); i++) {
      result[i] = data.get(i);
    }
    return result;
  }
}
