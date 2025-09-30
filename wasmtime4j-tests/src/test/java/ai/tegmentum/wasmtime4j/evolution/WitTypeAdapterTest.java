package ai.tegmentum.wasmtime4j.evolution;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WitTypeAdapter;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive tests for WIT type adapter functionality.
 *
 * <p>These tests validate type adaptation scenarios including:
 *
 * <ul>
 *   <li>Primitive type conversions
 *   <li>Structural type adaptations
 *   <li>Collection type conversions
 *   <li>Variant and enum adaptations
 *   <li>Performance and validation
 * </ul>
 *
 * @since 1.0.0
 */
@Execution(ExecutionMode.CONCURRENT)
class WitTypeAdapterTest {

  private WasmRuntime runtime;

  @BeforeEach
  void setUp() {
    runtime = WasmRuntimeFactory.createRuntime();
  }

  @AfterEach
  void tearDown() {
    if (runtime != null) {
      runtime.close();
    }
  }

  @Nested
  @DisplayName("Primitive Type Adapters")
  class PrimitiveTypeAdapterTests {

    @Test
    @DisplayName("Should convert between integer types")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void shouldConvertBetweenIntegerTypes() {
      // Arrange
      final WitTypeAdapter adapter = createPrimitiveAdapter("i32", "i64");

      // Act
      final WasmValue sourceValue = WasmValue.i32(42);
      final WasmValue convertedValue = adapter.convertForward(sourceValue);

      // Assert
      assertNotNull(convertedValue, "Converted value should not be null");
      assertTrue(convertedValue.isI64(), "Converted value should be i64");
      assertEquals(
          42L, convertedValue.asI64(), "Converted value should preserve the original value");

      // Test reverse conversion
      final WasmValue reversedValue = adapter.convertReverse(convertedValue);
      assertNotNull(reversedValue, "Reversed value should not be null");
      assertTrue(reversedValue.isI32(), "Reversed value should be i32");
      assertEquals(42, reversedValue.asI32(), "Reversed value should match original");

      assertTrue(adapter.isLossless(), "I32 to I64 conversion should be lossless");
    }

    @Test
    @DisplayName("Should handle lossy integer conversions")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void shouldHandleLossyIntegerConversions() {
      // Arrange
      final WitTypeAdapter adapter = createPrimitiveAdapter("i64", "i32");

      // Act - Test value within i32 range
      final WasmValue smallValue = WasmValue.i64(100L);
      final WasmValue convertedSmall = adapter.convertForward(smallValue);

      // Assert
      assertTrue(convertedSmall.isI32(), "Should convert to i32");
      assertEquals(100, convertedSmall.asI32(), "Small value should convert correctly");

      // Test value outside i32 range
      final WasmValue largeValue = WasmValue.i64(Long.MAX_VALUE);
      final WasmValue convertedLarge = adapter.convertForward(largeValue);

      // The exact behavior depends on implementation, but it should handle overflow gracefully
      assertTrue(convertedLarge.isI32(), "Should still convert to i32");
      assertFalse(adapter.isLossless(), "I64 to I32 conversion should not be lossless");
    }

    @Test
    @DisplayName("Should convert between floating point types")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void shouldConvertBetweenFloatingPointTypes() {
      // Arrange
      final WitTypeAdapter adapter = createPrimitiveAdapter("f32", "f64");

      // Act
      final WasmValue sourceValue = WasmValue.f32(3.14159f);
      final WasmValue convertedValue = adapter.convertForward(sourceValue);

      // Assert
      assertTrue(convertedValue.isF64(), "Converted value should be f64");
      assertEquals(
          3.14159f, convertedValue.asF64(), 0.0001, "Converted value should preserve precision");

      assertTrue(adapter.isLossless(), "F32 to F64 conversion should be lossless");
    }

    @Test
    @DisplayName("Should validate primitive type conversions")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void shouldValidatePrimitiveTypeConversions() {
      // Arrange
      final WitTypeAdapter adapter = createPrimitiveAdapter("u32", "u64");

      // Act - Valid conversion
      final WasmValue validValue = WasmValue.u32(42);
      final WitTypeAdapter.AdapterValidationResult validResult =
          adapter.validateForwardConversion(validValue);

      // Assert
      assertTrue(validResult.isValid(), "Valid value should pass validation");
      assertTrue(validResult.getErrors().isEmpty(), "Valid value should have no errors");

      // Test invalid type
      final WasmValue invalidValue = WasmValue.f32(3.14f);
      final WitTypeAdapter.AdapterValidationResult invalidResult =
          adapter.validateForwardConversion(invalidValue);

      assertFalse(invalidResult.isValid(), "Invalid type should fail validation");
      assertFalse(invalidResult.getErrors().isEmpty(), "Invalid type should have errors");
    }
  }

  @Nested
  @DisplayName("Structural Type Adapters")
  class StructuralTypeAdapterTests {

    @Test
    @DisplayName("Should adapt record types with field additions")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldAdaptRecordTypesWithFieldAdditions() {
      // Arrange
      final WitTypeAdapter adapter =
          createStructuralAdapter(
              "source-record", Map.of("name", "string", "age", "u32"),
              "target-record", Map.of("name", "string", "age", "u32", "email", "string"));

      // Act
      final WasmValue sourceRecord =
          WasmValue.record(
              Map.of(
                  "name", WasmValue.string("John"),
                  "age", WasmValue.u32(30)));

      final WasmValue convertedRecord = adapter.convertForward(sourceRecord);

      // Assert
      assertTrue(convertedRecord.isRecord(), "Converted value should be a record");
      final Map<String, WasmValue> fields = convertedRecord.asRecord();

      assertEquals(WasmValue.string("John"), fields.get("name"), "Name field should be preserved");
      assertEquals(WasmValue.u32(30), fields.get("age"), "Age field should be preserved");
      assertTrue(fields.containsKey("email"), "Email field should be added");

      // Default value handling depends on implementation
      final WasmValue emailField = fields.get("email");
      assertNotNull(emailField, "Email field should have a value");
      assertTrue(emailField.isString(), "Email field should be a string");

      assertEquals(WitTypeAdapter.AdapterType.STRUCTURAL_ADAPTATION, adapter.getAdapterType());
    }

    @Test
    @DisplayName("Should adapt record types with field type changes")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldAdaptRecordTypesWithFieldTypeChanges() {
      // Arrange
      final WitTypeAdapter adapter =
          createStructuralAdapter(
              "coordinates", Map.of("x", "i32", "y", "i32"),
              "coordinates", Map.of("x", "f64", "y", "f64"));

      // Act
      final WasmValue sourceCoords =
          WasmValue.record(
              Map.of(
                  "x", WasmValue.i32(10),
                  "y", WasmValue.i32(20)));

      final WasmValue convertedCoords = adapter.convertForward(sourceCoords);

      // Assert
      assertTrue(convertedCoords.isRecord(), "Converted value should be a record");
      final Map<String, WasmValue> fields = convertedCoords.asRecord();

      assertTrue(fields.get("x").isF64(), "X field should be converted to f64");
      assertTrue(fields.get("y").isF64(), "Y field should be converted to f64");
      assertEquals(10.0, fields.get("x").asF64(), 0.001, "X value should be converted correctly");
      assertEquals(20.0, fields.get("y").asF64(), 0.001, "Y value should be converted correctly");
    }

    @Test
    @DisplayName("Should handle field removal with warnings")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void shouldHandleFieldRemovalWithWarnings() {
      // Arrange
      final WitTypeAdapter adapter =
          createStructuralAdapter(
              "full-record", Map.of("name", "string", "age", "u32", "deprecated", "string"),
              "minimal-record", Map.of("name", "string", "age", "u32"));

      // Act
      final WasmValue sourceRecord =
          WasmValue.record(
              Map.of(
                  "name", WasmValue.string("Alice"),
                  "age", WasmValue.u32(25),
                  "deprecated", WasmValue.string("old-value")));

      final WitTypeAdapter.AdapterValidationResult validation =
          adapter.validateForwardConversion(sourceRecord);

      // Assert
      assertTrue(validation.isValid(), "Conversion should be valid");
      assertTrue(validation.hasWarnings(), "Should have warnings about removed field");
      assertFalse(adapter.isLossless(), "Field removal makes conversion lossy");

      final WasmValue convertedRecord = adapter.convertForward(sourceRecord);
      final Map<String, WasmValue> fields = convertedRecord.asRecord();

      assertEquals(2, fields.size(), "Should have only 2 fields after conversion");
      assertFalse(fields.containsKey("deprecated"), "Deprecated field should be removed");
    }
  }

  @Nested
  @DisplayName("Collection Type Adapters")
  class CollectionTypeAdapterTests {

    @Test
    @DisplayName("Should adapt list element types")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void shouldAdaptListElementTypes() {
      // Arrange
      final WitTypeAdapter adapter = createCollectionAdapter("list<i32>", "list<f64>");

      // Act
      final List<WasmValue> sourceList =
          List.of(WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3));
      final WasmValue sourceValue = WasmValue.list(sourceList);
      final WasmValue convertedValue = adapter.convertForward(sourceValue);

      // Assert
      assertTrue(convertedValue.isList(), "Converted value should be a list");
      final List<WasmValue> convertedList = convertedValue.asList();

      assertEquals(3, convertedList.size(), "List size should be preserved");
      for (int i = 0; i < convertedList.size(); i++) {
        assertTrue(convertedList.get(i).isF64(), "List elements should be converted to f64");
        assertEquals(
            (double) (i + 1),
            convertedList.get(i).asF64(),
            0.001,
            "List element values should be converted correctly");
      }

      assertEquals(WitTypeAdapter.AdapterType.COLLECTION_ADAPTATION, adapter.getAdapterType());
    }

    @Test
    @DisplayName("Should adapt option types")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void shouldAdaptOptionTypes() {
      // Arrange
      final WitTypeAdapter adapter = createCollectionAdapter("option<i32>", "option<string>");

      // Act - Test with some value
      final WasmValue someValue = WasmValue.option(WasmValue.i32(42));
      final WasmValue convertedSome = adapter.convertForward(someValue);

      // Assert
      assertTrue(convertedSome.isOption(), "Converted value should be option");
      assertTrue(convertedSome.asOption().isPresent(), "Converted option should have value");
      assertTrue(convertedSome.asOption().get().isString(), "Converted value should be string");
      assertEquals(
          "42", convertedSome.asOption().get().asString(), "Value should be string representation");

      // Test with none value
      final WasmValue noneValue = WasmValue.option(null);
      final WasmValue convertedNone = adapter.convertForward(noneValue);

      assertTrue(convertedNone.isOption(), "Converted none should be option");
      assertTrue(convertedNone.asOption().isEmpty(), "Converted none should be empty");
    }
  }

  @Nested
  @DisplayName("Variant and Enum Adapters")
  class VariantAndEnumAdapterTests {

    @Test
    @DisplayName("Should adapt variant types")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void shouldAdaptVariantTypes() {
      // Arrange
      final WitTypeAdapter adapter =
          createVariantAdapter(
              "shape-v1", List.of("circle", "rectangle"),
              "shape-v2", List.of("circle", "rectangle", "triangle"));

      // Act
      final WasmValue sourceVariant = WasmValue.variant("circle", WasmValue.f32(5.0f));
      final WasmValue convertedVariant = adapter.convertForward(sourceVariant);

      // Assert
      assertTrue(convertedVariant.isVariant(), "Converted value should be variant");
      assertEquals("circle", convertedVariant.getVariantCase(), "Variant case should be preserved");
      assertNotNull(convertedVariant.getVariantValue(), "Variant value should be preserved");

      assertEquals(WitTypeAdapter.AdapterType.VARIANT_ADAPTATION, adapter.getAdapterType());
    }

    @Test
    @DisplayName("Should adapt enum types with value mapping")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void shouldAdaptEnumTypesWithValueMapping() {
      // Arrange
      final WitTypeAdapter adapter =
          createEnumAdapter(
              "status-v1", List.of("pending", "completed", "failed"),
              "status-v2", List.of("queued", "processing", "completed", "failed", "cancelled"));

      // Act
      final WasmValue sourceEnum = WasmValue.enumValue("completed");
      final WasmValue convertedEnum = adapter.convertForward(sourceEnum);

      // Assert
      assertTrue(convertedEnum.isEnum(), "Converted value should be enum");
      assertEquals("completed", convertedEnum.asEnum(), "Enum value should be preserved");
    }

    @Test
    @DisplayName("Should handle enum value removal gracefully")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void shouldHandleEnumValueRemovalGracefully() {
      // Arrange
      final WitTypeAdapter adapter =
          createEnumAdapter(
              "priority", List.of("low", "medium", "high", "critical"),
              "priority", List.of("low", "medium", "high") // "critical" removed
              );

      // Act - Test mapping of removed value
      final WasmValue removedValue = WasmValue.enumValue("critical");

      // This should either throw an exception or map to a default value
      // depending on implementation strategy
      assertThrows(
          WasmRuntimeException.class,
          () -> {
            adapter.convertForward(removedValue);
          },
          "Should throw exception for removed enum value");
    }
  }

  @Nested
  @DisplayName("Adapter Performance and Statistics")
  class AdapterPerformanceTests {

    @Test
    @DisplayName("Should collect conversion statistics")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void shouldCollectConversionStatistics() {
      // Arrange
      final WitTypeAdapter adapter = createPrimitiveAdapter("u32", "u64");
      adapter.resetStatistics();

      // Act - Perform multiple conversions
      for (int i = 0; i < 10; i++) {
        adapter.convertForward(WasmValue.u32(i));
      }
      for (int i = 0; i < 5; i++) {
        adapter.convertReverse(WasmValue.u64(i));
      }

      // Assert
      final WitTypeAdapter.AdapterStatistics stats = adapter.getStatistics();
      assertNotNull(stats, "Statistics should not be null");

      assertEquals(10, stats.getForwardConversions(), "Should record forward conversions");
      assertEquals(5, stats.getReverseConversions(), "Should record reverse conversions");
      assertEquals(15, stats.getSuccessfulConversions(), "Should record successful conversions");
      assertEquals(0, stats.getFailedConversions(), "Should have no failed conversions");
      assertEquals(1.0, stats.getSuccessRate(), 0.001, "Success rate should be 100%");

      assertTrue(stats.getTotalConversionTime() > 0, "Total conversion time should be positive");
      assertTrue(
          stats.getAverageConversionTime() > 0, "Average conversion time should be positive");
      assertTrue(
          stats.getLastConversionTime().isPresent(), "Last conversion time should be present");
    }

    @Test
    @DisplayName("Should handle conversion failures in statistics")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void shouldHandleConversionFailuresInStatistics() {
      // Arrange
      final WitTypeAdapter adapter = createPrimitiveAdapter("string", "u32");
      adapter.resetStatistics();

      // Act - Perform valid and invalid conversions
      adapter.convertForward(WasmValue.string("42")); // Valid
      adapter.convertForward(WasmValue.string("123")); // Valid

      // Try invalid conversion
      try {
        adapter.convertForward(WasmValue.string("not-a-number")); // Invalid
      } catch (final WasmRuntimeException e) {
        // Expected
      }

      // Assert
      final WitTypeAdapter.AdapterStatistics stats = adapter.getStatistics();

      assertEquals(3, stats.getForwardConversions(), "Should count all conversion attempts");
      assertEquals(2, stats.getSuccessfulConversions(), "Should count successful conversions");
      assertEquals(1, stats.getFailedConversions(), "Should count failed conversions");
      assertTrue(stats.getSuccessRate() < 1.0, "Success rate should be less than 100%");
      assertEquals(2.0 / 3.0, stats.getSuccessRate(), 0.001, "Success rate should be correct");
    }

    @Test
    @DisplayName("Should provide conversion metadata")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void shouldProvideConversionMetadata() {
      // Arrange
      final WitTypeAdapter adapter = createPrimitiveAdapter("f32", "f64");

      // Act
      final WitTypeAdapter.ConversionMetadata forwardMetadata =
          adapter.getForwardConversionMetadata();
      final WitTypeAdapter.ConversionMetadata reverseMetadata =
          adapter.getReverseConversionMetadata();

      // Assert
      assertNotNull(forwardMetadata, "Forward metadata should not be null");
      assertNotNull(reverseMetadata, "Reverse metadata should not be null");

      assertFalse(forwardMetadata.isLossy(), "F32 to F64 should not be lossy");
      assertTrue(reverseMetadata.isLossy(), "F64 to F32 should be lossy");

      assertEquals(
          WitTypeAdapter.ConversionCost.LOW,
          forwardMetadata.getCost(),
          "F32 to F64 conversion should be low cost");

      assertNotNull(forwardMetadata.getPreconditions(), "Preconditions should not be null");
      assertNotNull(forwardMetadata.getPostconditions(), "Postconditions should not be null");
      assertNotNull(forwardMetadata.getProperties(), "Properties should not be null");
    }
  }

  // Helper methods for creating test adapters

  private WitTypeAdapter createPrimitiveAdapter(final String sourceType, final String targetType) {
    // This would normally create a real adapter - simplified for testing
    return new TestWitTypeAdapter(
        sourceType, targetType, WitTypeAdapter.AdapterType.DIRECT_CONVERSION, true);
  }

  private WitTypeAdapter createStructuralAdapter(
      final String sourceTypeName,
      final Map<String, String> sourceFields,
      final String targetTypeName,
      final Map<String, String> targetFields) {
    return new TestWitTypeAdapter(
        sourceTypeName, targetTypeName, WitTypeAdapter.AdapterType.STRUCTURAL_ADAPTATION, false);
  }

  private WitTypeAdapter createCollectionAdapter(final String sourceType, final String targetType) {
    return new TestWitTypeAdapter(
        sourceType, targetType, WitTypeAdapter.AdapterType.COLLECTION_ADAPTATION, true);
  }

  private WitTypeAdapter createVariantAdapter(
      final String sourceType,
      final List<String> sourceCases,
      final String targetType,
      final List<String> targetCases) {
    return new TestWitTypeAdapter(
        sourceType, targetType, WitTypeAdapter.AdapterType.VARIANT_ADAPTATION, false);
  }

  private WitTypeAdapter createEnumAdapter(
      final String sourceType,
      final List<String> sourceValues,
      final String targetType,
      final List<String> targetValues) {
    return new TestWitTypeAdapter(
        sourceType, targetType, WitTypeAdapter.AdapterType.VARIANT_ADAPTATION, false);
  }

  /** Test implementation of WitTypeAdapter for testing purposes. */
  private static class TestWitTypeAdapter implements WitTypeAdapter {
    private final String sourceTypeName;
    private final String targetTypeName;
    private final AdapterType adapterType;
    private final boolean lossless;
    private final TestAdapterStatistics statistics = new TestAdapterStatistics();

    TestWitTypeAdapter(
        final String sourceTypeName,
        final String targetTypeName,
        final AdapterType adapterType,
        final boolean lossless) {
      this.sourceTypeName = sourceTypeName;
      this.targetTypeName = targetTypeName;
      this.adapterType = adapterType;
      this.lossless = lossless;
    }

    @Override
    public String getSourceTypeName() {
      return sourceTypeName;
    }

    @Override
    public String getTargetTypeName() {
      return targetTypeName;
    }

    @Override
    public AdapterType getAdapterType() {
      return adapterType;
    }

    @Override
    public boolean supportsForwardConversion() {
      return true;
    }

    @Override
    public boolean supportsReverseConversion() {
      return true;
    }

    @Override
    public WasmValue convertForward(final WasmValue sourceValue) {
      final long startTime = System.nanoTime();
      try {
        statistics.recordConversionAttempt(true);

        // Simplified conversion logic for testing
        final WasmValue result = performConversion(sourceValue, sourceTypeName, targetTypeName);

        statistics.recordConversionSuccess(true, System.nanoTime() - startTime);
        return result;
      } catch (final Exception e) {
        statistics.recordConversionFailure(true);
        throw e;
      }
    }

    @Override
    public WasmValue convertReverse(final WasmValue targetValue) {
      final long startTime = System.nanoTime();
      try {
        statistics.recordConversionAttempt(false);

        final WasmValue result = performConversion(targetValue, targetTypeName, sourceTypeName);

        statistics.recordConversionSuccess(false, System.nanoTime() - startTime);
        return result;
      } catch (final Exception e) {
        statistics.recordConversionFailure(false);
        throw e;
      }
    }

    private WasmValue performConversion(
        final WasmValue value, final String fromType, final String toType) {
      // Simplified conversion logic for testing
      if (fromType.equals("i32") && toType.equals("i64")) {
        return WasmValue.i64(value.asI32());
      } else if (fromType.equals("i64") && toType.equals("i32")) {
        return WasmValue.i32((int) value.asI64());
      } else if (fromType.equals("f32") && toType.equals("f64")) {
        return WasmValue.f64(value.asF32());
      } else if (fromType.equals("f64") && toType.equals("f32")) {
        return WasmValue.f32((float) value.asF64());
      } else if (fromType.equals("u32") && toType.equals("u64")) {
        return WasmValue.u64(Integer.toUnsignedLong(value.asU32()));
      } else if (fromType.equals("string") && toType.equals("u32")) {
        try {
          return WasmValue.u32(Integer.parseUnsignedInt(value.asString()));
        } catch (final NumberFormatException e) {
          throw new WasmRuntimeException("Cannot convert string to u32: " + value.asString());
        }
      }

      // Default: return the value as-is for unsupported conversions
      return value;
    }

    @Override
    public AdapterValidationResult validateForwardConversion(final WasmValue sourceValue) {
      try {
        convertForward(sourceValue);
        return AdapterValidationResult.success();
      } catch (final Exception e) {
        return AdapterValidationResult.failure(List.of(e.getMessage()));
      }
    }

    @Override
    public AdapterValidationResult validateReverseConversion(final WasmValue targetValue) {
      try {
        convertReverse(targetValue);
        return AdapterValidationResult.success();
      } catch (final Exception e) {
        return AdapterValidationResult.failure(List.of(e.getMessage()));
      }
    }

    @Override
    public ConversionMetadata getForwardConversionMetadata() {
      return new TestConversionMetadata(lossless);
    }

    @Override
    public ConversionMetadata getReverseConversionMetadata() {
      return new TestConversionMetadata(!lossless); // Reverse is lossy if forward isn't
    }

    @Override
    public AdapterStatistics getStatistics() {
      return statistics;
    }

    @Override
    public boolean isLossless() {
      return lossless;
    }

    @Override
    public List<String> getLimitations() {
      return lossless ? List.of() : List.of("Conversion may lose precision");
    }

    @Override
    public java.util.Optional<TypeMappingInfo> getTypeMappingInfo() {
      return java.util.Optional.empty();
    }

    @Override
    public void resetStatistics() {
      statistics.reset();
    }
  }

  private static class TestAdapterStatistics implements WitTypeAdapter.AdapterStatistics {
    private long forwardConversions = 0;
    private long reverseConversions = 0;
    private long successfulConversions = 0;
    private long failedConversions = 0;
    private long totalConversionTime = 0;
    private java.time.Instant lastConversionTime;

    void recordConversionAttempt(final boolean forward) {
      if (forward) {
        forwardConversions++;
      } else {
        reverseConversions++;
      }
    }

    void recordConversionSuccess(final boolean forward, final long conversionTime) {
      successfulConversions++;
      totalConversionTime += conversionTime;
      lastConversionTime = java.time.Instant.now();
    }

    void recordConversionFailure(final boolean forward) {
      failedConversions++;
      lastConversionTime = java.time.Instant.now();
    }

    void reset() {
      forwardConversions = 0;
      reverseConversions = 0;
      successfulConversions = 0;
      failedConversions = 0;
      totalConversionTime = 0;
      lastConversionTime = null;
    }

    @Override
    public long getForwardConversions() {
      return forwardConversions;
    }

    @Override
    public long getReverseConversions() {
      return reverseConversions;
    }

    @Override
    public long getSuccessfulConversions() {
      return successfulConversions;
    }

    @Override
    public long getFailedConversions() {
      return failedConversions;
    }

    @Override
    public double getAverageConversionTime() {
      final long totalConversions = forwardConversions + reverseConversions;
      return totalConversions > 0 ? (double) totalConversionTime / totalConversions : 0.0;
    }

    @Override
    public long getTotalConversionTime() {
      return totalConversionTime;
    }

    @Override
    public double getSuccessRate() {
      final long totalConversions = forwardConversions + reverseConversions;
      return totalConversions > 0 ? (double) successfulConversions / totalConversions : 0.0;
    }

    @Override
    public java.util.Optional<java.time.Instant> getLastConversionTime() {
      return java.util.Optional.ofNullable(lastConversionTime);
    }
  }

  private static class TestConversionMetadata implements WitTypeAdapter.ConversionMetadata {
    private final boolean lossy;

    TestConversionMetadata(final boolean lossy) {
      this.lossy = lossy;
    }

    @Override
    public boolean isLossy() {
      return lossy;
    }

    @Override
    public ConversionCost getCost() {
      return ConversionCost.LOW;
    }

    @Override
    public List<String> getPreconditions() {
      return List.of();
    }

    @Override
    public List<String> getPostconditions() {
      return List.of();
    }

    @Override
    public Map<String, Object> getProperties() {
      return Map.of();
    }
  }
}
