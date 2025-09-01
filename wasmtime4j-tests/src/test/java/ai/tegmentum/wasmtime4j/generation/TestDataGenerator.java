package ai.tegmentum.wasmtime4j.generation;

import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Automated test data generation utilities for comprehensive WebAssembly testing. Provides
 * capabilities to generate various types of test data including WebAssembly modules, function
 * parameters, memory layouts, and edge case scenarios.
 */
public final class TestDataGenerator {
  private static final Logger LOGGER = Logger.getLogger(TestDataGenerator.class.getName());

  // Random number generator for test data
  private static final Random RANDOM = new SecureRandom();

  // WebAssembly constants
  private static final byte[] WASM_MAGIC = {0x00, 0x61, 0x73, 0x6d};
  private static final byte[] WASM_VERSION = {0x01, 0x00, 0x00, 0x00};

  // Common WebAssembly opcodes
  private static final byte I32_CONST = 0x41;
  private static final byte I64_CONST = 0x42;
  private static final byte F32_CONST = 0x43;
  private static final byte F64_CONST = 0x44;
  private static final byte I32_ADD = 0x6a;
  private static final byte I32_SUB = 0x6b;
  private static final byte I32_MUL = 0x6c;
  private static final byte I32_EQZ = 0x45;
  private static final byte END = 0x0b;
  private static final byte RETURN = 0x0f;

  private TestDataGenerator() {
    // Utility class - prevent instantiation
  }

  /** Configuration for test data generation. */
  public static final class GenerationConfig {
    private final int seed;
    private final boolean generateValidData;
    private final boolean generateInvalidData;
    private final boolean generateEdgeCases;
    private final int maxDataSize;
    private final int minDataSize;
    private final Map<String, Object> customParameters;

    private GenerationConfig(final Builder builder) {
      this.seed = builder.seed;
      this.generateValidData = builder.generateValidData;
      this.generateInvalidData = builder.generateInvalidData;
      this.generateEdgeCases = builder.generateEdgeCases;
      this.maxDataSize = builder.maxDataSize;
      this.minDataSize = builder.minDataSize;
      this.customParameters = new HashMap<>(builder.customParameters);
    }

    // Getters
    public int getSeed() {
      return seed;
    }

    public boolean shouldGenerateValidData() {
      return generateValidData;
    }

    public boolean shouldGenerateInvalidData() {
      return generateInvalidData;
    }

    public boolean shouldGenerateEdgeCases() {
      return generateEdgeCases;
    }

    public int getMaxDataSize() {
      return maxDataSize;
    }

    public int getMinDataSize() {
      return minDataSize;
    }

    public Map<String, Object> getCustomParameters() {
      return new HashMap<>(customParameters);
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Builder for configuring TestDataGenerator instances. */
    public static final class Builder {
      private int seed = RANDOM.nextInt();
      private boolean generateValidData = true;
      private boolean generateInvalidData = false;
      private boolean generateEdgeCases = true;
      private int maxDataSize = 1024 * 1024; // 1MB
      private int minDataSize = 1;
      private final Map<String, Object> customParameters = new HashMap<>();

      public Builder seed(final int seed) {
        this.seed = seed;
        return this;
      }

      public Builder generateValidData(final boolean generate) {
        this.generateValidData = generate;
        return this;
      }

      public Builder generateInvalidData(final boolean generate) {
        this.generateInvalidData = generate;
        return this;
      }

      public Builder generateEdgeCases(final boolean generate) {
        this.generateEdgeCases = generate;
        return this;
      }

      public Builder maxDataSize(final int maxSize) {
        this.maxDataSize = maxSize;
        return this;
      }

      public Builder minDataSize(final int minSize) {
        this.minDataSize = minSize;
        return this;
      }

      public Builder customParameter(final String key, final Object value) {
        this.customParameters.put(key, value);
        return this;
      }

      public GenerationConfig build() {
        return new GenerationConfig(this);
      }
    }
  }

  /** Generated test data container. */
  public static final class GeneratedTestData {
    private final String name;
    private final String category;
    private final Object data;
    private final boolean isValid;
    private final String description;
    private final Map<String, Object> metadata;

    /**
     * Creates a new generated test data instance.
     *
     * @param name the name of the test data
     * @param category the category of the test data
     * @param data the actual test data object
     * @param isValid whether the data is valid
     * @param description description of the test data
     * @param metadata additional metadata for the test data
     */
    public GeneratedTestData(
        final String name,
        final String category,
        final Object data,
        final boolean isValid,
        final String description,
        final Map<String, Object> metadata) {
      this.name = name;
      this.category = category;
      this.data = data;
      this.isValid = isValid;
      this.description = description;
      this.metadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
    }

    // Getters
    public String getName() {
      return name;
    }

    public String getCategory() {
      return category;
    }

    public Object getData() {
      return data;
    }

    public boolean isValid() {
      return isValid;
    }

    public String getDescription() {
      return description;
    }

    /**
     * Gets a copy of the metadata map.
     *
     * @return copy of the metadata map
     */
    public Map<String, Object> getMetadata() {
      return new HashMap<>(metadata);
    }

    /**
     * Gets the test data cast to the specified type.
     *
     * @param <T> the type to cast to
     * @param type the class type to cast to
     * @return the test data cast to the specified type
     * @throws ClassCastException if the data cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(final Class<T> type) {
      if (type.isInstance(data)) {
        return (T) data;
      }
      throw new ClassCastException(
          "Cannot cast " + data.getClass().getSimpleName() + " to " + type.getSimpleName());
    }
  }

  /**
   * Generates random WebAssembly modules for testing.
   *
   * @param config generation configuration
   * @param count number of modules to generate
   * @return list of generated modules
   */
  public static List<GeneratedTestData> generateWasmModules(
      final GenerationConfig config, final int count) {
    LOGGER.info("Generating " + count + " WebAssembly modules");

    final List<GeneratedTestData> modules = new ArrayList<>();
    final Random random = new Random(config.getSeed());

    for (int i = 0; i < count; i++) {
      final String moduleName = "generated_module_" + i;

      if (config.shouldGenerateValidData()) {
        // Generate valid modules
        final byte[] validModule = generateValidModule(random);
        modules.add(
            new GeneratedTestData(
                moduleName + "_valid",
                "wasm_module",
                validModule,
                true,
                "Generated valid WebAssembly module with random functionality",
                Map.of("generator", "valid", "size", validModule.length)));
      }

      if (config.shouldGenerateInvalidData()) {
        // Generate invalid modules
        final byte[] invalidModule = generateInvalidModule(random);
        modules.add(
            new GeneratedTestData(
                moduleName + "_invalid",
                "wasm_module",
                invalidModule,
                false,
                "Generated invalid WebAssembly module for error testing",
                Map.of("generator", "invalid", "size", invalidModule.length)));
      }

      if (config.shouldGenerateEdgeCases()) {
        // Generate edge case modules
        final byte[] edgeCaseModule = generateEdgeCaseModule(random);
        modules.add(
            new GeneratedTestData(
                moduleName + "_edge",
                "wasm_module",
                edgeCaseModule,
                true,
                "Generated edge case WebAssembly module",
                Map.of("generator", "edge_case", "size", edgeCaseModule.length)));
      }
    }

    return modules;
  }

  /**
   * Generates test parameters for function calls.
   *
   * @param parameterTypes array of parameter types ('i', 'f', 'd', 'l' for int, float, double,
   *     long)
   * @param config generation configuration
   * @param count number of parameter sets to generate
   * @return list of generated parameter sets
   */
  public static List<GeneratedTestData> generateFunctionParameters(
      final char[] parameterTypes, final GenerationConfig config, final int count) {
    LOGGER.info(
        "Generating "
            + count
            + " function parameter sets for types: "
            + Arrays.toString(parameterTypes));

    final List<GeneratedTestData> parameterSets = new ArrayList<>();
    final Random random = new Random(config.getSeed());

    for (int i = 0; i < count; i++) {
      final Object[] parameters = new Object[parameterTypes.length];
      final StringBuilder description = new StringBuilder("Parameters: ");

      for (int j = 0; j < parameterTypes.length; j++) {
        final char type = parameterTypes[j];

        switch (type) {
          case 'i': // int32
            if (config.shouldGenerateEdgeCases() && random.nextDouble() < 0.1) {
              parameters[j] = generateEdgeCaseInt32(random);
            } else {
              parameters[j] = random.nextInt();
            }
            break;

          case 'l': // int64
            if (config.shouldGenerateEdgeCases() && random.nextDouble() < 0.1) {
              parameters[j] = generateEdgeCaseInt64(random);
            } else {
              parameters[j] = random.nextLong();
            }
            break;

          case 'f': // float32
            if (config.shouldGenerateEdgeCases() && random.nextDouble() < 0.1) {
              parameters[j] = generateEdgeCaseFloat(random);
            } else {
              parameters[j] = random.nextFloat() * 1000.0f;
            }
            break;

          case 'd': // float64
            if (config.shouldGenerateEdgeCases() && random.nextDouble() < 0.1) {
              parameters[j] = generateEdgeCaseDouble(random);
            } else {
              parameters[j] = random.nextDouble() * 1000.0;
            }
            break;

          default:
            parameters[j] = random.nextInt();
        }

        if (j > 0) {
          description.append(", ");
        }
        description.append(parameters[j]);
      }

      parameterSets.add(
          new GeneratedTestData(
              "params_" + i,
              "function_parameters",
              parameters,
              true,
              description.toString(),
              Map.of("types", new String(parameterTypes), "count", parameterTypes.length)));
    }

    return parameterSets;
  }

  /**
   * Generates memory data patterns for testing.
   *
   * @param config generation configuration
   * @param count number of data patterns to generate
   * @return list of generated memory patterns
   */
  public static List<GeneratedTestData> generateMemoryData(
      final GenerationConfig config, final int count) {
    LOGGER.info("Generating " + count + " memory data patterns");

    final List<GeneratedTestData> memoryPatterns = new ArrayList<>();
    final Random random = new Random(config.getSeed());

    for (int i = 0; i < count; i++) {
      final int size =
          config.getMinDataSize()
              + random.nextInt(Math.max(1, config.getMaxDataSize() - config.getMinDataSize()));

      final byte[] data = generateMemoryPattern(random, size, config.shouldGenerateEdgeCases());

      memoryPatterns.add(
          new GeneratedTestData(
              "memory_" + i,
              "memory_data",
              data,
              true,
              "Generated memory pattern of size " + size + " bytes",
              Map.of("size", size, "pattern_type", detectPatternType(data))));
    }

    return memoryPatterns;
  }

  /**
   * Generates test data for WASI operations.
   *
   * @param config generation configuration
   * @param count number of test cases to generate
   * @return list of generated WASI test data
   */
  public static List<GeneratedTestData> generateWasiTestData(
      final GenerationConfig config, final int count) {
    LOGGER.info("Generating " + count + " WASI test data cases");

    final List<GeneratedTestData> wasiData = new ArrayList<>();
    final Random random = new Random(config.getSeed());

    for (int i = 0; i < count; i++) {
      // Generate different types of WASI test data
      final int dataType = random.nextInt(4);

      switch (dataType) {
        case 0: // File path
          final String filePath = generateFilePath(random, config.shouldGenerateInvalidData());
          wasiData.add(
              new GeneratedTestData(
                  "wasi_filepath_" + i,
                  "wasi_filepath",
                  filePath,
                  isValidFilePath(filePath),
                  "Generated file path: " + filePath,
                  Map.of("type", "filepath", "length", filePath.length())));
          break;

        case 1: // Environment variable
          final Map<String, String> envVar =
              generateEnvironmentVariable(random, config.shouldGenerateInvalidData());
          wasiData.add(
              new GeneratedTestData(
                  "wasi_env_" + i,
                  "wasi_environment",
                  envVar,
                  true,
                  "Generated environment variable",
                  Map.of("type", "environment", "count", envVar.size())));
          break;

        case 2: // Command line arguments
          final String[] args = generateCommandLineArgs(random, config.shouldGenerateInvalidData());
          wasiData.add(
              new GeneratedTestData(
                  "wasi_args_" + i,
                  "wasi_arguments",
                  args,
                  true,
                  "Generated command line arguments",
                  Map.of("type", "arguments", "count", args.length)));
          break;

        case 3: // File content
          final byte[] fileContent =
              generateFileContent(random, config.getMinDataSize(), config.getMaxDataSize());
          wasiData.add(
              new GeneratedTestData(
                  "wasi_content_" + i,
                  "wasi_file_content",
                  fileContent,
                  true,
                  "Generated file content of " + fileContent.length + " bytes",
                  Map.of("type", "file_content", "size", fileContent.length)));
          break;

        default:
          // Default case for unknown data types
          wasiData.add(
              new GeneratedTestData(
                  "wasi_default_" + i,
                  "wasi_default",
                  "default_data",
                  true,
                  "Generated default WASI data",
                  Map.of("type", "default")));
          break;
      }
    }

    return wasiData;
  }

  /**
   * Generates a collection of edge case test data.
   *
   * @param config generation configuration
   * @return list of edge case test data
   */
  public static List<GeneratedTestData> generateEdgeCases(final GenerationConfig config) {
    LOGGER.info("Generating edge case test data");

    final List<GeneratedTestData> edgeCases = new ArrayList<>();

    // Numeric edge cases
    edgeCases.add(
        new GeneratedTestData(
            "edge_int32_max",
            "edge_case",
            Integer.MAX_VALUE,
            true,
            "Maximum 32-bit integer value",
            Map.of("type", "numeric", "subtype", "int32_max")));

    edgeCases.add(
        new GeneratedTestData(
            "edge_int32_min",
            "edge_case",
            Integer.MIN_VALUE,
            true,
            "Minimum 32-bit integer value",
            Map.of("type", "numeric", "subtype", "int32_min")));

    edgeCases.add(
        new GeneratedTestData(
            "edge_float_nan",
            "edge_case",
            Float.NaN,
            true,
            "Float NaN value",
            Map.of("type", "numeric", "subtype", "float_nan")));

    edgeCases.add(
        new GeneratedTestData(
            "edge_float_infinity",
            "edge_case",
            Float.POSITIVE_INFINITY,
            true,
            "Float positive infinity",
            Map.of("type", "numeric", "subtype", "float_pos_inf")));

    // Memory edge cases
    edgeCases.add(
        new GeneratedTestData(
            "edge_empty_memory",
            "edge_case",
            new byte[0],
            true,
            "Empty memory block",
            Map.of("type", "memory", "size", 0)));

    edgeCases.add(
        new GeneratedTestData(
            "edge_large_memory",
            "edge_case",
            new byte[65536],
            true,
            "Large memory block (64KB)",
            Map.of("type", "memory", "size", 65536)));

    // String edge cases
    edgeCases.add(
        new GeneratedTestData(
            "edge_empty_string",
            "edge_case",
            "",
            true,
            "Empty string",
            Map.of("type", "string", "length", 0)));

    edgeCases.add(
        new GeneratedTestData(
            "edge_unicode_string",
            "edge_case",
            "🌍🚀✨🎯🔥💎⚡🌟",
            true,
            "Unicode emoji string",
            Map.of("type", "string", "encoding", "unicode")));

    // Array edge cases
    edgeCases.add(
        new GeneratedTestData(
            "edge_empty_array",
            "edge_case",
            new Object[0],
            true,
            "Empty array",
            Map.of("type", "array", "length", 0)));

    return edgeCases;
  }

  /**
   * Generates property-based test data using generators.
   *
   * @param generators map of property name to generator function
   * @param config generation configuration
   * @param count number of test cases to generate
   * @return list of generated property test data
   */
  public static List<GeneratedTestData> generatePropertyTestData(
      final Map<String, Supplier<Object>> generators,
      final GenerationConfig config,
      final int count) {

    LOGGER.info("Generating " + count + " property-based test cases");

    final List<GeneratedTestData> testData = new ArrayList<>();
    final Random random = new Random(config.getSeed());

    for (int i = 0; i < count; i++) {
      final Map<String, Object> properties = new HashMap<>();
      final StringBuilder description = new StringBuilder("Generated properties: ");

      for (final Map.Entry<String, Supplier<Object>> entry : generators.entrySet()) {
        final String propertyName = entry.getKey();
        final Object propertyValue = entry.getValue().get();

        properties.put(propertyName, propertyValue);

        if (properties.size() > 1) {
          description.append(", ");
        }
        description.append(propertyName).append("=").append(propertyValue);
      }

      testData.add(
          new GeneratedTestData(
              "property_test_" + i,
              "property_based",
              properties,
              true,
              description.toString(),
              Map.of("property_count", properties.size())));
    }

    return testData;
  }

  /** Generates a valid WebAssembly module with random functionality. */
  private static byte[] generateValidModule(final Random random) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();

      // Magic number and version
      baos.write(WASM_MAGIC);
      baos.write(WASM_VERSION);

      // Type section (function signatures)
      baos.write(0x01); // Type section ID
      final byte[] typeSection = generateTypeSection(random);
      writeVarUInt32(baos, typeSection.length);
      baos.write(typeSection);

      // Function section
      baos.write(0x03); // Function section ID
      final byte[] functionSection = generateFunctionSection(random, 1);
      writeVarUInt32(baos, functionSection.length);
      baos.write(functionSection);

      // Export section
      baos.write(0x07); // Export section ID
      final byte[] exportSection = generateExportSection(random);
      writeVarUInt32(baos, exportSection.length);
      baos.write(exportSection);

      // Code section
      baos.write(0x0a); // Code section ID
      final byte[] codeSection = generateCodeSection(random);
      writeVarUInt32(baos, codeSection.length);
      baos.write(codeSection);

      return baos.toByteArray();
    } catch (final IOException e) {
      // Should not happen with ByteArrayOutputStream
      throw new RuntimeException("Failed to generate WASM module", e);
    }
  }

  /** Generates an invalid WebAssembly module for error testing. */
  private static byte[] generateInvalidModule(final Random random) {
    final int errorType = random.nextInt(4);

    switch (errorType) {
      case 0: // Invalid magic number
        return new byte[] {0x00, 0x61, 0x73, 0x6e, 0x01, 0x00, 0x00, 0x00};

      case 1: // Invalid version
        return new byte[] {0x00, 0x61, 0x73, 0x6d, 0x02, 0x00, 0x00, 0x00};

      case 2: // Truncated module
        return new byte[] {0x00, 0x61, 0x73, 0x6d};

      default: // Invalid section
        final byte[] validStart = new byte[8];
        System.arraycopy(WASM_MAGIC, 0, validStart, 0, 4);
        System.arraycopy(WASM_VERSION, 0, validStart, 4, 4);

        final byte[] result = new byte[10];
        System.arraycopy(validStart, 0, result, 0, 8);
        result[8] = 0x0f; // Invalid section ID
        result[9] = 0x00; // Section size

        return result;
    }
  }

  /** Generates an edge case WebAssembly module. */
  private static byte[] generateEdgeCaseModule(final Random random) {
    // For simplicity, return a minimal but unusual valid module
    return WasmTestModules.getModule("basic_empty");
  }

  // Helper methods for WebAssembly module generation

  private static byte[] generateTypeSection(final Random random) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();

      // Number of types
      writeVarUInt32(baos, 1);

      // Function type
      baos.write(0x60); // Function type

      // Parameters
      final int paramCount = random.nextInt(3); // 0-2 parameters
      writeVarUInt32(baos, paramCount);
      for (int i = 0; i < paramCount; i++) {
        baos.write(0x7f); // i32 type
      }

      // Results
      writeVarUInt32(baos, 1); // One result
      baos.write(0x7f); // i32 type

      return baos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException("Failed to generate type section", e);
    }
  }

  private static byte[] generateFunctionSection(final Random random, final int functionCount) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();

      writeVarUInt32(baos, functionCount);
      for (int i = 0; i < functionCount; i++) {
        writeVarUInt32(baos, 0); // Type index 0
      }

      return baos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException("Failed to generate function section", e);
    }
  }

  private static byte[] generateExportSection(final Random random) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();

      writeVarUInt32(baos, 1); // One export

      // Export name
      final String name = "test";
      writeVarUInt32(baos, name.length());
      baos.write(name.getBytes(StandardCharsets.UTF_8));

      // Export kind (function)
      baos.write(0x00);

      // Export index
      writeVarUInt32(baos, 0);

      return baos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException("Failed to generate export section", e);
    }
  }

  private static byte[] generateCodeSection(final Random random) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();

      writeVarUInt32(baos, 1); // One function body

      // Function body
      final ByteArrayOutputStream bodyBaos = new ByteArrayOutputStream();

      // Locals
      writeVarUInt32(bodyBaos, 0); // No locals

      // Body
      bodyBaos.write(I32_CONST);
      writeVarInt32(bodyBaos, random.nextInt(1000));
      bodyBaos.write(END);

      final byte[] body = bodyBaos.toByteArray();
      writeVarUInt32(baos, body.length);
      baos.write(body);

      return baos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException("Failed to generate code section", e);
    }
  }

  private static void writeVarUInt32(final ByteArrayOutputStream baos, final int value)
      throws IOException {
    int remaining = value;
    while (remaining >= 0x80) {
      baos.write((remaining & 0x7f) | 0x80);
      remaining >>>= 7;
    }
    baos.write(remaining);
  }

  private static void writeVarInt32(final ByteArrayOutputStream baos, final int value)
      throws IOException {
    int remaining = value;
    boolean hasMore = true;

    while (hasMore) {
      byte toWrite = (byte) (remaining & 0x7f);
      remaining >>= 7;

      if ((remaining == 0 && (toWrite & 0x40) == 0) || (remaining == -1 && (toWrite & 0x40) != 0)) {
        hasMore = false;
      } else {
        toWrite |= (byte) 0x80;
      }

      baos.write(toWrite);
    }
  }

  // Edge case value generators

  private static int generateEdgeCaseInt32(final Random random) {
    final int[] edgeCases = {
      0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1
    };
    return edgeCases[random.nextInt(edgeCases.length)];
  }

  private static long generateEdgeCaseInt64(final Random random) {
    final long[] edgeCases = {
      0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE - 1, Long.MIN_VALUE + 1
    };
    return edgeCases[random.nextInt(edgeCases.length)];
  }

  private static float generateEdgeCaseFloat(final Random random) {
    final float[] edgeCases = {
      0.0f,
      -0.0f,
      1.0f,
      -1.0f,
      Float.MAX_VALUE,
      Float.MIN_VALUE,
      Float.POSITIVE_INFINITY,
      Float.NEGATIVE_INFINITY,
      Float.NaN,
      Float.MIN_NORMAL
    };
    return edgeCases[random.nextInt(edgeCases.length)];
  }

  private static double generateEdgeCaseDouble(final Random random) {
    final double[] edgeCases = {
      0.0,
      -0.0,
      1.0,
      -1.0,
      Double.MAX_VALUE,
      Double.MIN_VALUE,
      Double.POSITIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.NaN,
      Double.MIN_NORMAL
    };
    return edgeCases[random.nextInt(edgeCases.length)];
  }

  // Memory and data generators

  private static byte[] generateMemoryPattern(
      final Random random, final int size, final boolean includeEdgeCases) {
    final byte[] data = new byte[size];

    if (includeEdgeCases && random.nextDouble() < 0.3) {
      // Generate special patterns
      final int patternType = random.nextInt(4);

      switch (patternType) {
        case 0: // All zeros
          // data is already initialized to zeros
          break;

        case 1: // All ones
          Arrays.fill(data, (byte) 0xff);
          break;

        case 2: // Alternating pattern
          for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 2 == 0 ? 0xaa : 0x55);
          }
          break;

        case 3: // Incremental pattern
          for (int i = 0; i < size; i++) {
            data[i] = (byte) (i & 0xff);
          }
          break;

        default:
          // Default case - fill with random data
          random.nextBytes(data);
          break;
      }
    } else {
      // Random data
      random.nextBytes(data);
    }

    return data;
  }

  private static String detectPatternType(final byte[] data) {
    if (data.length == 0) {
      return "empty";
    }

    boolean allZeros = true;
    boolean allOnes = true;
    boolean incremental = true;

    for (int i = 0; i < data.length; i++) {
      if (data[i] != 0) {
        allZeros = false;
      }
      if (data[i] != (byte) 0xff) {
        allOnes = false;
      }
      if (data[i] != (byte) (i & 0xff)) {
        incremental = false;
      }
    }

    if (allZeros) {
      return "zeros";
    }
    if (allOnes) {
      return "ones";
    }
    if (incremental) {
      return "incremental";
    }

    return "random";
  }

  // WASI data generators

  private static String generateFilePath(final Random random, final boolean includeInvalid) {
    if (includeInvalid && random.nextDouble() < 0.2) {
      // Generate invalid paths
      final String[] invalidPaths = {
        "", "\\0", "con:", "prn:", "aux:", "nul:", "/dev/null\0", "very_long_" + "x".repeat(300)
      };
      return invalidPaths[random.nextInt(invalidPaths.length)];
    }

    final String[] directories = {"tmp", "home", "usr", "var", "opt", "data"};
    final String[] filenames = {"test.txt", "data.bin", "config.json", "log.txt", "temp.dat"};

    return "/"
        + directories[random.nextInt(directories.length)]
        + "/"
        + filenames[random.nextInt(filenames.length)];
  }

  private static boolean isValidFilePath(final String path) {
    return path != null && !path.isEmpty() && !path.contains("\0") && path.length() < 256;
  }

  private static Map<String, String> generateEnvironmentVariable(
      final Random random, final boolean includeInvalid) {
    final Map<String, String> env = new HashMap<>();
    final String[] keys = {"PATH", "HOME", "USER", "TEMP", "WASMTIME_DEBUG"};
    final String[] values = {"/usr/bin", "/home/user", "testuser", "/tmp", "1"};

    final String key = keys[random.nextInt(keys.length)];
    final String value = values[random.nextInt(values.length)];

    env.put(key, value);
    return env;
  }

  private static String[] generateCommandLineArgs(
      final Random random, final boolean includeInvalid) {
    final int argCount = 1 + random.nextInt(5);
    final String[] args = new String[argCount];

    args[0] = "program"; // Program name

    for (int i = 1; i < argCount; i++) {
      if (includeInvalid && random.nextDouble() < 0.1) {
        args[i] = "\0invalid"; // Invalid argument with null byte
      } else {
        args[i] = "--option" + i;
      }
    }

    return args;
  }

  private static byte[] generateFileContent(
      final Random random, final int minSize, final int maxSize) {
    final int size = minSize + random.nextInt(Math.max(1, maxSize - minSize));
    final byte[] content = new byte[size];

    // Generate text-like content
    for (int i = 0; i < size; i++) {
      if (random.nextDouble() < 0.1) {
        content[i] = '\n'; // Newline
      } else {
        content[i] = (byte) (32 + random.nextInt(95)); // Printable ASCII
      }
    }

    return content;
  }

  /**
   * Gets default generation configuration.
   *
   * @return default configuration
   */
  public static GenerationConfig getDefaultConfig() {
    return GenerationConfig.builder().build();
  }

  /**
   * Gets comprehensive generation configuration for thorough testing.
   *
   * @return comprehensive configuration
   */
  public static GenerationConfig getComprehensiveConfig() {
    return GenerationConfig.builder()
        .generateValidData(true)
        .generateInvalidData(true)
        .generateEdgeCases(true)
        .maxDataSize(10 * 1024 * 1024) // 10MB
        .build();
  }

  /**
   * Gets fast generation configuration for quick testing.
   *
   * @return fast configuration with smaller data sizes
   */
  public static GenerationConfig getFastConfig() {
    return GenerationConfig.builder()
        .generateValidData(true)
        .generateInvalidData(false)
        .generateEdgeCases(false)
        .maxDataSize(1024) // 1KB
        .build();
  }
}
