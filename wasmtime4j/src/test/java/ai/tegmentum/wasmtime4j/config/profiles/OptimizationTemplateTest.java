package ai.tegmentum.wasmtime4j.config.profiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;
import ai.tegmentum.wasmtime4j.WasmFeature;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Comprehensive tests for OptimizationTemplate configuration templates. */
class OptimizationTemplateTest {

  @Test
  @DisplayName("Builder should create valid optimization templates")
  void testBuilderCreatesValidTemplates() {
    OptimizationTemplate template =
        OptimizationTemplate.builder(
                "Test Template", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Test description")
            .parameter("test_param", "test_value")
            .requiredFeature(WasmFeature.SIMD)
            .optionalFeature(WasmFeature.THREADS)
            .build();

    assertNotNull(template);
    assertEquals("Test Template", template.getName());
    assertEquals("Test description", template.getDescription());
    assertEquals(OptimizationTemplate.TemplateType.CPU_INTENSIVE, template.getType());
    assertTrue(template.getParameters().containsKey("test_param"));
    assertEquals("test_value", template.getParameters().get("test_param"));
    assertTrue(template.getRequiredFeatures().contains(WasmFeature.SIMD));
    assertTrue(template.getOptionalFeatures().contains(WasmFeature.THREADS));
  }

  @Test
  @DisplayName("Builder should reject null name and type")
  void testBuilderValidatesRequiredParameters() {
    assertThrows(
        NullPointerException.class,
        () -> {
          OptimizationTemplate.builder(null, OptimizationTemplate.TemplateType.CPU_INTENSIVE);
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          OptimizationTemplate.builder("Test", null);
        });
  }

  @Test
  @DisplayName("Builder should reject null description")
  void testBuilderValidatesDescription() {
    assertThrows(
        NullPointerException.class,
        () -> {
          OptimizationTemplate.builder("Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
              .description(null);
        });
  }

  @Test
  @DisplayName("Builder should reject null parameter keys and values")
  void testBuilderValidatesParameters() {
    OptimizationTemplate.Builder builder =
        OptimizationTemplate.builder("Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE);

    assertThrows(
        NullPointerException.class,
        () -> {
          builder.parameter(null, "value");
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          builder.parameter("key", null);
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          builder.parameters(null);
        });
  }

  @Test
  @DisplayName("Builder should reject null features")
  void testBuilderValidatesFeatures() {
    OptimizationTemplate.Builder builder =
        OptimizationTemplate.builder("Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE);

    assertThrows(
        NullPointerException.class,
        () -> {
          builder.requiredFeature(null);
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          builder.optionalFeature(null);
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          builder.requiredFeatures(null);
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          builder.optionalFeatures(null);
        });
  }

  @ParameterizedTest
  @EnumSource(OptimizationTemplate.TemplateType.class)
  @DisplayName("All template types should create valid configurations")
  void testAllTemplateTypesCreateValidConfigurations(
      OptimizationTemplate.TemplateType templateType) {
    OptimizationTemplate template =
        OptimizationTemplate.builder("Test", templateType)
            .description("Test template for " + templateType)
            .build();

    assertDoesNotThrow(
        () -> {
          EngineConfig config = template.createConfig();
          assertNotNull(config);
          assertNotNull(config.getOptimizationLevel());
        });
  }

  @Test
  @DisplayName("CPU intensive template should optimize for maximum performance")
  void testCpuIntensiveTemplate() {
    OptimizationTemplate template =
        OptimizationTemplate.builder("CPU Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("CPU intensive workload")
            .build();

    EngineConfig config = template.createConfig();

    assertEquals(OptimizationLevel.SPEED, config.getOptimizationLevel());
    assertTrue(config.isParallelCompilation());
    assertFalse(config.isCraneliftDebugVerifier());
    assertFalse(config.isGenerateDebugInfo());

    var settings = config.getCraneliftSettings();
    assertEquals("speed", settings.get("opt_level"));
    assertEquals("false", settings.get("enable_verifier"));
    assertEquals("true", settings.get("enable_jump_threading"));
  }

  @Test
  @DisplayName("I/O intensive template should enable cooperative scheduling")
  void testIoIntensiveTemplate() {
    OptimizationTemplate template =
        OptimizationTemplate.builder("IO Test", OptimizationTemplate.TemplateType.IO_INTENSIVE)
            .description("I/O intensive workload")
            .build();

    EngineConfig config = template.createConfig();

    assertEquals(OptimizationLevel.SPEED, config.getOptimizationLevel());
    assertTrue(config.isParallelCompilation());
    assertTrue(config.isConsumeFuel()); // Enable cooperative scheduling
    assertTrue(config.isEpochInterruption());

    var settings = config.getCraneliftSettings();
    assertEquals("speed", settings.get("opt_level"));
  }

  @Test
  @DisplayName("Memory constrained template should minimize memory usage")
  void testMemoryConstrainedTemplate() {
    OptimizationTemplate template =
        OptimizationTemplate.builder(
                "Memory Test", OptimizationTemplate.TemplateType.MEMORY_CONSTRAINED)
            .description("Memory constrained workload")
            .build();

    EngineConfig config = template.createConfig();

    assertEquals(OptimizationLevel.SIZE, config.getOptimizationLevel());
    assertFalse(config.isParallelCompilation());
    assertEquals(256 * 1024, config.getMaxWasmStack()); // 256KB stack limit
    assertEquals(128 * 1024, config.getAsyncStackSize()); // 128KB async stack

    var settings = config.getCraneliftSettings();
    assertEquals("size", settings.get("opt_level"));
    assertEquals("true", settings.get("enable_probestack"));
  }

  @Test
  @DisplayName("Real-time template should optimize for consistent latency")
  void testRealTimeTemplate() {
    OptimizationTemplate template =
        OptimizationTemplate.builder("Real-time Test", OptimizationTemplate.TemplateType.REAL_TIME)
            .description("Real-time workload")
            .build();

    EngineConfig config = template.createConfig();

    assertEquals(OptimizationLevel.SPEED, config.getOptimizationLevel());
    assertTrue(config.isParallelCompilation());
    assertEquals(2 * 1024 * 1024, config.getMaxWasmStack()); // 2MB stack for reduced allocations
    assertFalse(config.isConsumeFuel()); // Disable for predictable timing
    assertFalse(config.isEpochInterruption());

    var settings = config.getCraneliftSettings();
    assertEquals("speed", settings.get("opt_level"));
    assertEquals("false", settings.get("enable_safepoints"));
  }


  @Test
  @DisplayName("Custom parameters should override template defaults")
  void testCustomParametersOverride() {
    OptimizationTemplate template =
        OptimizationTemplate.builder("Custom Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Custom parameters test")
            .parameter("optimization_level", OptimizationLevel.SIZE)
            .parameter("parallel_compilation", false)
            .parameter("debug_info", true)
            .parameter("max_stack_size", 512L * 1024) // 512KB
            .build();

    EngineConfig config = template.createConfig();

    // Custom parameters should override template defaults
    assertEquals(OptimizationLevel.SIZE, config.getOptimizationLevel());
    assertFalse(config.isParallelCompilation());
    assertTrue(config.isGenerateDebugInfo());
    assertEquals(512 * 1024, config.getMaxWasmStack());
  }

  @Test
  @DisplayName("Custom Cranelift settings should be applied")
  void testCustomCraneliftSettings() {
    Map<String, String> customSettings = new HashMap<>();
    customSettings.put("enable_verifier", "true");
    customSettings.put("enable_nan_canonicalization", "false");
    customSettings.put("regalloc", "graph_coloring");

    OptimizationTemplate template =
        OptimizationTemplate.builder(
                "Cranelift Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Custom Cranelift settings test")
            .parameter("cranelift_settings", customSettings)
            .build();

    EngineConfig config = template.createConfig();
    var settings = config.getCraneliftSettings();

    assertTrue(settings.containsKey("enable_verifier"));
    assertEquals("true", settings.get("enable_verifier"));
    assertTrue(settings.containsKey("enable_nan_canonicalization"));
    assertEquals("false", settings.get("enable_nan_canonicalization"));
    assertTrue(settings.containsKey("regalloc"));
    assertEquals("graph_coloring", settings.get("regalloc"));
  }

  @Test
  @DisplayName("Required features should be added to configuration")
  void testRequiredFeaturesApplication() {
    OptimizationTemplate template =
        OptimizationTemplate.builder(
                "Features Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Required features test")
            .requiredFeature(WasmFeature.SIMD)
            .requiredFeature(WasmFeature.THREADS)
            .build();

    EngineConfig config = template.createConfig();
    Set<WasmFeature> features = config.getWasmFeatures();

    assertTrue(features.contains(WasmFeature.SIMD));
    assertTrue(features.contains(WasmFeature.THREADS));
  }

  @Test
  @DisplayName("Template application should not modify original config")
  void testTemplateApplicationImmutability() {
    EngineConfig originalConfig =
        new EngineConfig().optimizationLevel(OptimizationLevel.NONE).parallelCompilation(false);

    OptimizationTemplate template =
        OptimizationTemplate.builder(
                "Immutability Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Test immutability")
            .build();

    EngineConfig modifiedConfig = template.applyTo(originalConfig);

    // Original config should be unchanged
    assertEquals(OptimizationLevel.NONE, originalConfig.getOptimizationLevel());
    assertFalse(originalConfig.isParallelCompilation());

    // Modified config should have template settings
    assertEquals(OptimizationLevel.SPEED, modifiedConfig.getOptimizationLevel());
    assertTrue(modifiedConfig.isParallelCompilation());

    // Configs should be different objects
    assertNotSame(originalConfig, modifiedConfig);
  }

  @Test
  @DisplayName("Template should reject null engine config")
  void testNullEngineConfigRejection() {
    OptimizationTemplate template =
        OptimizationTemplate.builder("Null Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Null config test")
            .build();

    assertThrows(
        NullPointerException.class,
        () -> {
          template.applyTo(null);
        });
  }

  @Test
  @DisplayName("Predefined templates should be available and valid")
  void testPredefinedTemplates() {
    OptimizationTemplate[] templates = OptimizationTemplate.getPredefinedTemplates();

    assertNotNull(templates);
    assertTrue(templates.length > 0);

    for (OptimizationTemplate template : templates) {
      assertNotNull(template);
      assertNotNull(template.getName());
      assertNotNull(template.getDescription());
      assertNotNull(template.getType());
      assertFalse(template.getName().trim().isEmpty());
      assertFalse(template.getDescription().trim().isEmpty());

      // Should create valid configuration
      assertDoesNotThrow(
          () -> {
            EngineConfig config = template.createConfig();
            assertNotNull(config);
          });
    }
  }

  @Test
  @DisplayName("Template finder should work correctly")
  void testTemplateFinder() {
    // Should find existing templates
    assertNotNull(OptimizationTemplate.findTemplate("CPU Intensive"));
    assertNotNull(OptimizationTemplate.findTemplate("Memory Constrained"));
    assertNotNull(OptimizationTemplate.findTemplate("Real-time"));
    assertNotNull(OptimizationTemplate.findTemplate("I/O Intensive"));

    // Should return null for non-existent templates
    assertNull(OptimizationTemplate.findTemplate("Non-existent Template"));
    assertNull(OptimizationTemplate.findTemplate(""));
  }

  @Test
  @DisplayName("Template equals and hashCode should work correctly")
  void testEqualsAndHashCode() {
    OptimizationTemplate template1 =
        OptimizationTemplate.builder("Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Test template")
            .parameter("param1", "value1")
            .requiredFeature(WasmFeature.SIMD)
            .build();

    OptimizationTemplate template2 =
        OptimizationTemplate.builder("Test", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Test template")
            .parameter("param1", "value1")
            .requiredFeature(WasmFeature.SIMD)
            .build();

    OptimizationTemplate template3 =
        OptimizationTemplate.builder("Different", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Different template")
            .build();

    // Equal templates
    assertEquals(template1, template2);
    assertEquals(template1.hashCode(), template2.hashCode());

    // Different templates
    assertNotEquals(template1, template3);

    // Self equality
    assertEquals(template1, template1);

    // Null comparison
    assertNotEquals(template1, null);

    // Different class comparison
    assertNotEquals(template1, "string");
  }

  @Test
  @DisplayName("Template toString should be informative")
  void testToString() {
    OptimizationTemplate template =
        OptimizationTemplate.builder(
                "Test Template", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Test description")
            .build();

    String toString = template.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("Test Template"));
    assertTrue(toString.contains("CPU_INTENSIVE"));
    assertTrue(toString.contains("Test description"));
  }

  @Test
  @DisplayName("Builder should handle multiple parameters correctly")
  void testBuilderMultipleParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("param1", "value1");
    params.put("param2", 42);
    params.put("param3", true);

    OptimizationTemplate template =
        OptimizationTemplate.builder("Multi-param", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Multiple parameters test")
            .parameters(params)
            .parameter("param4", "value4") // Additional individual parameter
            .build();

    Map<String, Object> templateParams = template.getParameters();
    assertEquals("value1", templateParams.get("param1"));
    assertEquals(42, templateParams.get("param2"));
    assertEquals(true, templateParams.get("param3"));
    assertEquals("value4", templateParams.get("param4"));
  }

  @Test
  @DisplayName("Builder should handle multiple features correctly")
  void testBuilderMultipleFeatures() {
    Set<WasmFeature> requiredFeatures = EnumSet.of(WasmFeature.SIMD, WasmFeature.THREADS);
    Set<WasmFeature> optionalFeatures = EnumSet.of(WasmFeature.MEMORY64, WasmFeature.TAIL_CALL);

    OptimizationTemplate template =
        OptimizationTemplate.builder(
                "Multi-feature", OptimizationTemplate.TemplateType.CPU_INTENSIVE)
            .description("Multiple features test")
            .requiredFeatures(requiredFeatures)
            .optionalFeatures(optionalFeatures)
            .requiredFeature(WasmFeature.BULK_MEMORY) // Additional individual feature
            .optionalFeature(WasmFeature.MULTI_VALUE) // Additional individual feature
            .build();

    Set<WasmFeature> templateRequired = template.getRequiredFeatures();

    assertTrue(templateRequired.contains(WasmFeature.SIMD));
    assertTrue(templateRequired.contains(WasmFeature.THREADS));
    assertTrue(templateRequired.contains(WasmFeature.BULK_MEMORY));

    Set<WasmFeature> templateOptional = template.getOptionalFeatures();
    assertTrue(templateOptional.contains(WasmFeature.MEMORY64));
    assertTrue(templateOptional.contains(WasmFeature.TAIL_CALL));
    assertTrue(templateOptional.contains(WasmFeature.MULTI_VALUE));
  }
}
