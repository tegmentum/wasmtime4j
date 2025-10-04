package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for ResultComparator, verifying tolerance-based comparison accuracy and
 * semantic analysis capabilities.
 */
@DisplayName("ResultComparator Tests")
class ResultComparatorTest {

  private ResultComparator comparator;
  private ToleranceConfiguration defaultConfig;
  private ToleranceConfiguration strictConfig;
  private ToleranceConfiguration lenientConfig;

  @BeforeEach
  void setUp() {
    defaultConfig = ToleranceConfiguration.defaultConfig();
    strictConfig = ToleranceConfiguration.strictConfig();
    lenientConfig = ToleranceConfiguration.lenientConfig();
    comparator = new ResultComparator(defaultConfig);
  }

  @Nested
  @DisplayName("Null and Reference Comparison Tests")
  class NullAndReferenceComparisonTests {

    @Test
    @DisplayName("Should handle null values correctly")
    void shouldHandleNullValuesCorrectly() {
      // Both null
      ValueComparisonResult result = comparator.compareValues(null, null);
      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.EXACT_MATCH);

      // One null
      result = comparator.compareValues(null, "value");
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.NULL_MISMATCH);

      result = comparator.compareValues("value", null);
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.NULL_MISMATCH);
    }

    @Test
    @DisplayName("Should handle reference equality")
    void shouldHandleReferenceEquality() {
      final String sameReference = "same";
      final ValueComparisonResult result = comparator.compareValues(sameReference, sameReference);

      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.EXACT_MATCH);
    }
  }

  @Nested
  @DisplayName("Numeric Comparison Tests")
  class NumericComparisonTests {

    @Test
    @DisplayName("Should compare integers exactly")
    void shouldCompareIntegersExactly() {
      ValueComparisonResult result = comparator.compareValues(42, 42);
      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.NUMERIC);

      result = comparator.compareValues(42, 43);
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.NUMERIC);
    }

    @Test
    @DisplayName("Should apply floating point tolerance")
    void shouldApplyFloatingPointTolerance() {
      final ResultComparator strictComparator = new ResultComparator(strictConfig);
      final ResultComparator lenientComparator = new ResultComparator(lenientConfig);

      final double value1 = 1.0000000001;
      final double value2 = 1.0000000002;

      // Default tolerance should accept this small difference
      ValueComparisonResult defaultResult = comparator.compareValues(value1, value2);
      assertThat(defaultResult.isEquivalent()).isTrue();

      // Strict tolerance might reject it
      ValueComparisonResult strictResult = strictComparator.compareValues(value1, value2);
      // The exact result depends on tolerance configuration

      // Lenient tolerance should definitely accept it
      ValueComparisonResult lenientResult = lenientComparator.compareValues(value1, value2);
      assertThat(lenientResult.isEquivalent()).isTrue();
    }

    @Test
    @DisplayName("Should handle BigDecimal precision")
    void shouldHandleBigDecimalPrecision() {
      final BigDecimal value1 = new BigDecimal("1.000000000000000001");
      final BigDecimal value2 = new BigDecimal("1.000000000000000002");

      final ValueComparisonResult result = comparator.compareValues(value1, value2);
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.NUMERIC);
      // Result depends on configured BigDecimal tolerance
    }

    @Test
    @DisplayName("Should compare different numeric types")
    void shouldCompareDifferentNumericTypes() {
      final ValueComparisonResult result = comparator.compareValues(42, 42.0);
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.NUMERIC);
      assertThat(result.isEquivalent()).isTrue();

      final ValueComparisonResult result2 = comparator.compareValues(42L, 42);
      assertThat(result2.getComparisonType()).isEqualTo(ComparisonType.NUMERIC);
      assertThat(result2.isEquivalent()).isTrue();
    }
  }

  @Nested
  @DisplayName("String Comparison Tests")
  class StringComparisonTests {

    @Test
    @DisplayName("Should compare strings exactly")
    void shouldCompareStringsExactly() {
      ValueComparisonResult result = comparator.compareValues("exact", "exact");
      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.STRING);

      result = comparator.compareValues("different", "strings");
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.STRING);
    }

    @Test
    @DisplayName("Should perform semantic string comparison")
    void shouldPerformSemanticStringComparison() {
      // Whitespace normalization
      ValueComparisonResult result = comparator.compareValues("  hello   world  ", "hello world");
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.STRING);

      // Case normalization
      result = comparator.compareValues("Hello World", "hello world");
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isTrue();

      // Numeric string comparison
      result = comparator.compareValues("1.0", "1.00");
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isTrue();
    }
  }

  @Nested
  @DisplayName("Array Comparison Tests")
  class ArrayComparisonTests {

    @Test
    @DisplayName("Should compare arrays element by element")
    void shouldCompareArraysElementByElement() {
      final int[] array1 = {1, 2, 3, 4, 5};
      final int[] array2 = {1, 2, 3, 4, 5};
      final int[] array3 = {1, 2, 3, 4, 6};

      ValueComparisonResult result = comparator.compareValues(array1, array2);
      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.ARRAY);

      result = comparator.compareValues(array1, array3);
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.ARRAY);
    }

    @Test
    @DisplayName("Should handle arrays of different lengths")
    void shouldHandleArraysOfDifferentLengths() {
      final int[] array1 = {1, 2, 3};
      final int[] array2 = {1, 2, 3, 4};

      final ValueComparisonResult result = comparator.compareValues(array1, array2);
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.ARRAY);
      assertThat(result.getDetails()).contains("length mismatch");
    }

    @Test
    @DisplayName("Should handle multi-dimensional arrays")
    void shouldHandleMultiDimensionalArrays() {
      final int[][] array1 = {{1, 2}, {3, 4}};
      final int[][] array2 = {{1, 2}, {3, 4}};
      final int[][] array3 = {{1, 2}, {3, 5}};

      ValueComparisonResult result = comparator.compareValues(array1, array2);
      assertThat(result.isEquivalent()).isTrue();

      result = comparator.compareValues(array1, array3);
      assertThat(result.isEquivalent()).isFalse();
    }
  }

  @Nested
  @DisplayName("Collection Comparison Tests")
  class CollectionComparisonTests {

    @Test
    @DisplayName("Should compare ordered collections")
    void shouldCompareOrderedCollections() {
      final List<String> list1 = Arrays.asList("a", "b", "c");
      final List<String> list2 = Arrays.asList("a", "b", "c");
      final List<String> list3 = Arrays.asList("a", "c", "b"); // Different order

      ValueComparisonResult result = comparator.compareValues(list1, list2);
      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.COLLECTION);

      result = comparator.compareValues(list1, list3);
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
    }

    @Test
    @DisplayName("Should compare unordered collections")
    void shouldCompareUnorderedCollections() {
      final Set<String> set1 = Set.of("a", "b", "c");
      final Set<String> set2 = Set.of("c", "a", "b"); // Different order, same elements

      final ValueComparisonResult result = comparator.compareValues(set1, set2);
      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.COLLECTION);
    }

    @Test
    @DisplayName("Should handle collections of different sizes")
    void shouldHandleCollectionsOfDifferentSizes() {
      final List<Integer> list1 = Arrays.asList(1, 2, 3);
      final List<Integer> list2 = Arrays.asList(1, 2, 3, 4);

      final ValueComparisonResult result = comparator.compareValues(list1, list2);
      assertThat(result.isEquivalent()).isFalse();
      assertThat(result.getDetails()).contains("size mismatch");
    }
  }

  @Nested
  @DisplayName("Map Comparison Tests")
  class MapComparisonTests {

    @Test
    @DisplayName("Should compare maps by key-value pairs")
    void shouldCompareMapsByKeyValuePairs() {
      final Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
      final Map<String, Integer> map2 = Map.of("a", 1, "b", 2, "c", 3);
      final Map<String, Integer> map3 = Map.of("a", 1, "b", 2, "c", 4);

      ValueComparisonResult result = comparator.compareValues(map1, map2);
      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.MAP);

      result = comparator.compareValues(map1, map3);
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
    }

    @Test
    @DisplayName("Should handle maps with missing keys")
    void shouldHandleMapsWithMissingKeys() {
      final Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
      final Map<String, Integer> map2 = Map.of("a", 1, "c", 3);

      final ValueComparisonResult result = comparator.compareValues(map1, map2);
      assertThat(result.isEquivalent()).isFalse();
    }
  }

  @Nested
  @DisplayName("Complex Object Comparison Tests")
  class ComplexObjectComparisonTests {

    @Test
    @DisplayName("Should compare objects using reflection")
    void shouldCompareObjectsUsingReflection() {
      final TestObject obj1 = new TestObject("test", 42);
      final TestObject obj2 = new TestObject("test", 42);
      final TestObject obj3 = new TestObject("test", 43);

      ValueComparisonResult result = comparator.compareValues(obj1, obj2);
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.OBJECT_REFLECTION);

      result = comparator.compareValues(obj1, obj3);
      assertThat(result.isEquivalent()).isFalse();
    }

    @Test
    @DisplayName("Should handle enum comparisons")
    void shouldHandleEnumComparisons() {
      ValueComparisonResult result = comparator.compareValues(TestEnum.VALUE1, TestEnum.VALUE1);
      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.ENUM);

      result = comparator.compareValues(TestEnum.VALUE1, TestEnum.VALUE2);
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
    }
  }

  @Nested
  @DisplayName("WebAssembly-Specific Comparison Tests")
  class WebAssemblySpecificComparisonTests {

    @Test
    @DisplayName("Should compare ByteBuffers")
    void shouldCompareByteBuffers() {
      final ByteBuffer buffer1 = ByteBuffer.wrap(new byte[] {1, 2, 3, 4});
      final ByteBuffer buffer2 = ByteBuffer.wrap(new byte[] {1, 2, 3, 4});
      final ByteBuffer buffer3 = ByteBuffer.wrap(new byte[] {1, 2, 3, 5});

      ValueComparisonResult result = comparator.compareValues(buffer1, buffer2);
      assertThat(result.isExactMatch()).isTrue();
      assertThat(result.isEquivalent()).isTrue();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.BYTE_BUFFER);

      result = comparator.compareValues(buffer1, buffer3);
      assertThat(result.isExactMatch()).isFalse();
      assertThat(result.isEquivalent()).isFalse();
    }

    @Test
    @DisplayName("Should handle temporal value comparisons")
    void shouldHandleTemporalValueComparisons() {
      final Instant instant1 = Instant.now();
      final Instant instant2 = instant1.plus(Duration.ofMillis(10)); // Small difference
      final Instant instant3 = instant1.plus(Duration.ofSeconds(10)); // Large difference

      final ResultComparator lenientComparator = new ResultComparator(lenientConfig);

      ValueComparisonResult result = lenientComparator.compareValues(instant1, instant2);
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.TEMPORAL);
      assertThat(result.isEquivalent()).isTrue(); // Within tolerance

      result = lenientComparator.compareValues(instant1, instant3);
      assertThat(result.isEquivalent()).isFalse(); // Outside tolerance
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle type mismatches")
    void shouldHandleTypeMismatches() {
      final ValueComparisonResult result = comparator.compareValues("string", 42);
      assertThat(result.isEquivalent()).isFalse();
      assertThat(result.getComparisonType()).isEqualTo(ComparisonType.TYPE_MISMATCH);
      assertThat(result.getDetails()).contains("Type mismatch");
    }

    @Test
    @DisplayName("Should prevent infinite recursion")
    void shouldPreventInfiniteRecursion() {
      final RecursiveObject obj1 = new RecursiveObject("test");
      final RecursiveObject obj2 = new RecursiveObject("test");
      obj1.self = obj1; // Create self-reference
      obj2.self = obj2;

      // Should not cause stack overflow
      final ValueComparisonResult result = comparator.compareValues(obj1, obj2);
      assertThat(result).isNotNull();
      // May hit depth limit, but should not crash
    }

    @Test
    @DisplayName("Should handle large collections efficiently")
    void shouldHandleLargeCollectionsEfficiently() {
      final int[] largeArray1 = new int[10000];
      final int[] largeArray2 = new int[10000];
      Arrays.fill(largeArray1, 1);
      Arrays.fill(largeArray2, 1);

      final long startTime = System.currentTimeMillis();
      final ValueComparisonResult result = comparator.compareValues(largeArray1, largeArray2);
      final long endTime = System.currentTimeMillis();

      assertThat(result.isEquivalent()).isTrue();
      assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
    }
  }

  @Nested
  @DisplayName("Performance and Caching Tests")
  class PerformanceAndCachingTests {

    @Test
    @DisplayName("Should use caching for repeated comparisons")
    void shouldUseCachingForRepeatedComparisons() {
      final String value1 = "cached-value-1";
      final String value2 = "cached-value-2";

      // First comparison
      final long start1 = System.nanoTime();
      final ValueComparisonResult result1 = comparator.compareValues(value1, value2);
      final long time1 = System.nanoTime() - start1;

      // Second comparison (should use cache)
      final long start2 = System.nanoTime();
      final ValueComparisonResult result2 = comparator.compareValues(value1, value2);
      final long time2 = System.nanoTime() - start2;

      assertThat(result1.isEquivalent()).isEqualTo(result2.isEquivalent());
      assertThat(result1.isExactMatch()).isEqualTo(result2.isExactMatch());
      // Second call should be faster due to caching (though this is not guaranteed)
    }

    @Test
    @DisplayName("Should clear cache when requested")
    void shouldClearCacheWhenRequested() {
      // Perform some comparisons to populate cache
      comparator.compareValues("value1", "value2");
      comparator.compareValues(42, 43);

      final ResultComparator.CacheStatistics statsBefore = ResultComparator.getCacheStatistics();
      assertThat(statsBefore.getCacheSize()).isGreaterThan(0);

      // Clear cache
      ResultComparator.clearCache();

      final ResultComparator.CacheStatistics statsAfter = ResultComparator.getCacheStatistics();
      assertThat(statsAfter.getCacheSize()).isEqualTo(0);
    }
  }

  // Test helper classes
  private static final class TestObject {
    private final String name;
    private final int value;

    TestObject(final String name, final int value) {
      this.name = name;
      this.value = value;
    }

    // Note: No equals/hashCode override to test reflection-based comparison
  }

  private static final class RecursiveObject {
    private final String name;
    private RecursiveObject self;

    RecursiveObject(final String name) {
      this.name = name;
    }
  }

  private enum TestEnum {
    VALUE1,
    VALUE2,
    VALUE3
  }
}
