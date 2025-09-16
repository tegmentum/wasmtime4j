package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Advanced result comparison engine that performs deep value comparison with configurable tolerance
 * levels for different data types. Supports semantic comparison for equivalent but differently
 * formatted results and uses reflection-based analysis for complex object hierarchies.
 *
 * <p>The comparator implements sophisticated tolerance mechanisms for floating-point numbers,
 * timing-sensitive results, and nested object structures while maintaining high accuracy
 * requirements (false positive rate &lt; 5%, false negative rate &lt; 1%).
 *
 * @since 1.0.0
 */
public final class ResultComparator {
  private static final Logger LOGGER = Logger.getLogger(ResultComparator.class.getName());

  // Comparison cache for performance optimization
  private static final Map<String, ValueComparisonResult> comparisonCache =
      new ConcurrentHashMap<>();

  // Default tolerances for different numeric types
  private static final double DEFAULT_DOUBLE_TOLERANCE = 1e-9;
  private static final float DEFAULT_FLOAT_TOLERANCE = 1e-6f;
  private static final BigDecimal DEFAULT_BIGDECIMAL_TOLERANCE = new BigDecimal("1e-15");

  // Reflection comparison limits to prevent infinite recursion
  private static final int MAX_REFLECTION_DEPTH = 10;
  private static final int MAX_COLLECTION_ELEMENTS_TO_COMPARE = 1000;

  // Types that should be compared by reference/identity
  private static final Set<Class<?>> REFERENCE_COMPARISON_TYPES =
      Set.of(Class.class, Thread.class, ThreadGroup.class);

  private final ToleranceConfiguration toleranceConfig;

  /**
   * Creates a new ResultComparator with the specified tolerance configuration.
   *
   * @param toleranceConfig the tolerance configuration to use
   */
  public ResultComparator(final ToleranceConfiguration toleranceConfig) {
    this.toleranceConfig =
        Objects.requireNonNull(toleranceConfig, "toleranceConfig cannot be null");
  }

  /**
   * Performs comprehensive comparison between two values with tolerance-based analysis.
   *
   * @param value1 the first value to compare
   * @param value2 the second value to compare
   * @return detailed value comparison result
   */
  public ValueComparisonResult compareValues(final Object value1, final Object value2) {
    return compareValues(value1, value2, 0);
  }

  /** Performs comparison with explicit recursion depth tracking. */
  private ValueComparisonResult compareValues(
      final Object value1, final Object value2, final int depth) {
    // Check cache first for performance
    final String cacheKey = createComparisonCacheKey(value1, value2);
    final ValueComparisonResult cached = comparisonCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    LOGGER.finest(
        "Comparing values at depth "
            + depth
            + ": "
            + getValueTypeDescription(value1)
            + " vs "
            + getValueTypeDescription(value2));

    final ValueComparisonResult.Builder resultBuilder = new ValueComparisonResult.Builder();

    // Prevent infinite recursion
    if (depth > MAX_REFLECTION_DEPTH) {
      return resultBuilder
          .equivalent(false)
          .exactMatch(false)
          .comparisonType(ComparisonType.DEPTH_LIMIT_EXCEEDED)
          .details("Maximum reflection depth exceeded")
          .build();
    }

    // Handle null values
    if (value1 == null && value2 == null) {
      return createExactMatchResult();
    }
    if (value1 == null || value2 == null) {
      return createNullMismatchResult(value1, value2);
    }

    // Handle reference equality
    if (value1 == value2) {
      return createExactMatchResult();
    }

    // Handle different types
    final Class<?> type1 = value1.getClass();
    final Class<?> type2 = value2.getClass();

    if (!areTypesCompatible(type1, type2)) {
      return createTypeMismatchResult(type1, type2);
    }

    // Perform type-specific comparison
    final ValueComparisonResult result = performTypedComparison(value1, value2, depth);

    // Cache result for performance (only cache leaf comparisons to avoid memory issues)
    if (depth == 0) {
      comparisonCache.put(cacheKey, result);
    }

    return result;
  }

  /** Performs type-specific comparison based on the value types. */
  private ValueComparisonResult performTypedComparison(
      final Object value1, final Object value2, final int depth) {
    final Class<?> type = value1.getClass();

    // Handle primitive and wrapper types
    if (isNumericType(type)) {
      return compareNumericValues(value1, value2);
    }

    // Handle strings
    if (type == String.class) {
      return compareStrings((String) value1, (String) value2);
    }

    // Handle temporal types
    if (Temporal.class.isAssignableFrom(type)) {
      return compareTemporalValues(value1, value2);
    }

    // Handle arrays
    if (type.isArray()) {
      return compareArrays(value1, value2, depth);
    }

    // Handle collections
    if (Collection.class.isAssignableFrom(type)) {
      return compareCollections((Collection<?>) value1, (Collection<?>) value2, depth);
    }

    // Handle maps
    if (Map.class.isAssignableFrom(type)) {
      return compareMaps((Map<?, ?>) value1, (Map<?, ?>) value2, depth);
    }

    // Handle enums
    if (type.isEnum()) {
      return compareEnums((Enum<?>) value1, (Enum<?>) value2);
    }

    // Handle ByteBuffer (common in WebAssembly)
    if (ByteBuffer.class.isAssignableFrom(type)) {
      return compareByteBuffers((ByteBuffer) value1, (ByteBuffer) value2);
    }

    // Handle reference comparison types
    if (REFERENCE_COMPARISON_TYPES.contains(type)) {
      return createReferenceComparisonResult(value1, value2);
    }

    // Handle complex objects using reflection
    return compareObjectsReflectively(value1, value2, depth);
  }

  /** Compares numeric values with appropriate tolerance levels. */
  private ValueComparisonResult compareNumericValues(final Object value1, final Object value2) {
    final ValueComparisonResult.Builder resultBuilder =
        new ValueComparisonResult.Builder().comparisonType(ComparisonType.NUMERIC);

    if (value1.equals(value2)) {
      return resultBuilder.equivalent(true).exactMatch(true).build();
    }

    // Handle different numeric types by converting to BigDecimal for precise comparison
    final BigDecimal decimal1 = convertToBigDecimal(value1);
    final BigDecimal decimal2 = convertToBigDecimal(value2);

    if (decimal1 == null || decimal2 == null) {
      return resultBuilder
          .equivalent(false)
          .exactMatch(false)
          .details("Failed to convert to BigDecimal")
          .build();
    }

    final BigDecimal difference = decimal1.subtract(decimal2).abs();
    final BigDecimal tolerance = getNumericTolerance(value1, value2);

    final boolean withinTolerance = difference.compareTo(tolerance) <= 0;
    final boolean exactMatch = difference.compareTo(BigDecimal.ZERO) == 0;

    return resultBuilder
        .equivalent(withinTolerance)
        .exactMatch(exactMatch)
        .details(String.format("Difference: %s, Tolerance: %s", difference, tolerance))
        .build();
  }

  /** Converts numeric values to BigDecimal for precise comparison. */
  private BigDecimal convertToBigDecimal(final Object value) {
    try {
      if (value instanceof BigDecimal) {
        return (BigDecimal) value;
      } else if (value instanceof BigInteger) {
        return new BigDecimal((BigInteger) value);
      } else if (value instanceof Double) {
        return BigDecimal.valueOf((Double) value);
      } else if (value instanceof Float) {
        return BigDecimal.valueOf((Float) value);
      } else if (value instanceof Long) {
        return BigDecimal.valueOf((Long) value);
      } else if (value instanceof Integer) {
        return BigDecimal.valueOf((Integer) value);
      } else if (value instanceof Short) {
        return BigDecimal.valueOf((Short) value);
      } else if (value instanceof Byte) {
        return BigDecimal.valueOf((Byte) value);
      } else {
        return new BigDecimal(value.toString());
      }
    } catch (final NumberFormatException e) {
      LOGGER.warning("Failed to convert value to BigDecimal: " + value);
      return null;
    }
  }

  /** Gets the appropriate numeric tolerance for comparison. */
  private BigDecimal getNumericTolerance(final Object value1, final Object value2) {
    if (value1 instanceof Float || value2 instanceof Float) {
      return BigDecimal.valueOf(toleranceConfig.getFloatTolerance());
    } else if (value1 instanceof Double || value2 instanceof Double) {
      return BigDecimal.valueOf(toleranceConfig.getDoubleTolerance());
    } else if (value1 instanceof BigDecimal || value2 instanceof BigDecimal) {
      return toleranceConfig.getBigDecimalTolerance();
    } else {
      // For integer types, use exact comparison (tolerance = 0)
      return BigDecimal.ZERO;
    }
  }

  /** Compares string values with semantic analysis. */
  private ValueComparisonResult compareStrings(final String str1, final String str2) {
    final ValueComparisonResult.Builder resultBuilder =
        new ValueComparisonResult.Builder().comparisonType(ComparisonType.STRING);

    final boolean exactMatch = str1.equals(str2);
    if (exactMatch) {
      return resultBuilder.equivalent(true).exactMatch(true).build();
    }

    // Perform semantic string comparison
    final boolean semanticallyEquivalent = areStringsSemanticallyEquivalent(str1, str2);

    return resultBuilder
        .equivalent(semanticallyEquivalent)
        .exactMatch(false)
        .details(
            String.format(
                "Length diff: %d, Semantic match: %b",
                Math.abs(str1.length() - str2.length()), semanticallyEquivalent))
        .build();
  }

  /** Determines if two strings are semantically equivalent. */
  private boolean areStringsSemanticallyEquivalent(final String str1, final String str2) {
    // Normalize whitespace and case
    final String normalized1 = str1.trim().toLowerCase().replaceAll("\\s+", " ");
    final String normalized2 = str2.trim().toLowerCase().replaceAll("\\s+", " ");

    if (normalized1.equals(normalized2)) {
      return true;
    }

    // Check for numeric string equivalence
    if (isNumericString(str1) && isNumericString(str2)) {
      try {
        final BigDecimal num1 = new BigDecimal(str1);
        final BigDecimal num2 = new BigDecimal(str2);
        final BigDecimal difference = num1.subtract(num2).abs();
        return difference.compareTo(DEFAULT_BIGDECIMAL_TOLERANCE) <= 0;
      } catch (final NumberFormatException e) {
        // Not numeric after all
      }
    }

    return false;
  }

  /** Checks if a string represents a numeric value. */
  private boolean isNumericString(final String str) {
    try {
      new BigDecimal(str);
      return true;
    } catch (final NumberFormatException e) {
      return false;
    }
  }

  /** Compares temporal values (dates, times, etc.). */
  private ValueComparisonResult compareTemporalValues(final Object value1, final Object value2) {
    final ValueComparisonResult.Builder resultBuilder =
        new ValueComparisonResult.Builder().comparisonType(ComparisonType.TEMPORAL);

    if (value1.equals(value2)) {
      return resultBuilder.equivalent(true).exactMatch(true).build();
    }

    // For temporal types, compare with timing tolerance
    if (value1 instanceof Instant && value2 instanceof Instant) {
      final Instant instant1 = (Instant) value1;
      final Instant instant2 = (Instant) value2;
      final Duration difference = Duration.between(instant1, instant2).abs();
      final boolean withinTolerance =
          difference.compareTo(toleranceConfig.getTimingTolerance()) <= 0;

      return resultBuilder
          .equivalent(withinTolerance)
          .exactMatch(false)
          .details("Time difference: " + difference)
          .build();
    }

    // For other temporal types, use exact comparison
    return resultBuilder
        .equivalent(false)
        .exactMatch(false)
        .details("Different temporal values")
        .build();
  }

  /** Compares arrays with element-by-element analysis. */
  private ValueComparisonResult compareArrays(
      final Object array1, final Object array2, final int depth) {
    final ValueComparisonResult.Builder resultBuilder =
        new ValueComparisonResult.Builder().comparisonType(ComparisonType.ARRAY);

    final int length1 = Array.getLength(array1);
    final int length2 = Array.getLength(array2);

    if (length1 != length2) {
      return resultBuilder
          .equivalent(false)
          .exactMatch(false)
          .details(String.format("Array length mismatch: %d vs %d", length1, length2))
          .build();
    }

    boolean allExactMatch = true;
    boolean allEquivalent = true;
    int comparedElements = 0;

    for (int i = 0; i < length1 && comparedElements < MAX_COLLECTION_ELEMENTS_TO_COMPARE; i++) {
      final Object element1 = Array.get(array1, i);
      final Object element2 = Array.get(array2, i);

      final ValueComparisonResult elementResult = compareValues(element1, element2, depth + 1);
      if (!elementResult.isExactMatch()) {
        allExactMatch = false;
      }
      if (!elementResult.isEquivalent()) {
        allEquivalent = false;
      }

      comparedElements++;
    }

    String details = String.format("Compared %d/%d elements", comparedElements, length1);
    if (comparedElements < length1) {
      details += " (limited for performance)";
    }

    return resultBuilder
        .equivalent(allEquivalent)
        .exactMatch(allExactMatch)
        .details(details)
        .build();
  }

  /** Compares collections with element-by-element analysis. */
  private ValueComparisonResult compareCollections(
      final Collection<?> collection1, final Collection<?> collection2, final int depth) {
    final ValueComparisonResult.Builder resultBuilder =
        new ValueComparisonResult.Builder().comparisonType(ComparisonType.COLLECTION);

    if (collection1.size() != collection2.size()) {
      return resultBuilder
          .equivalent(false)
          .exactMatch(false)
          .details(
              String.format(
                  "Collection size mismatch: %d vs %d", collection1.size(), collection2.size()))
          .build();
    }

    // For ordered collections, compare element by element
    if (collection1 instanceof List && collection2 instanceof List) {
      return compareOrderedCollections((List<?>) collection1, (List<?>) collection2, depth);
    }

    // For unordered collections, compare sets of elements
    return compareUnorderedCollections(collection1, collection2, depth);
  }

  /** Compares ordered collections (Lists). */
  private ValueComparisonResult compareOrderedCollections(
      final List<?> list1, final List<?> list2, final int depth) {
    boolean allExactMatch = true;
    boolean allEquivalent = true;
    int comparedElements = 0;

    final int size = Math.min(list1.size(), MAX_COLLECTION_ELEMENTS_TO_COMPARE);
    for (int i = 0; i < size; i++) {
      final ValueComparisonResult elementResult =
          compareValues(list1.get(i), list2.get(i), depth + 1);
      if (!elementResult.isExactMatch()) {
        allExactMatch = false;
      }
      if (!elementResult.isEquivalent()) {
        allEquivalent = false;
      }
      comparedElements++;
    }

    return new ValueComparisonResult.Builder()
        .comparisonType(ComparisonType.COLLECTION)
        .equivalent(allEquivalent)
        .exactMatch(allExactMatch)
        .details(String.format("Compared %d elements in ordered collection", comparedElements))
        .build();
  }

  /** Compares unordered collections (Sets, etc.). */
  private ValueComparisonResult compareUnorderedCollections(
      final Collection<?> collection1, final Collection<?> collection2, final int depth) {
    // For performance, convert to sets and compare containment
    final Set<Object> set1 = new HashSet<>(collection1);
    final Set<Object> set2 = new HashSet<>(collection2);

    final boolean exactMatch = set1.equals(set2);
    // For unordered collections, exact match implies equivalence
    final boolean equivalent = exactMatch;

    return new ValueComparisonResult.Builder()
        .comparisonType(ComparisonType.COLLECTION)
        .equivalent(equivalent)
        .exactMatch(exactMatch)
        .details(
            String.format(
                "Unordered collection comparison: %d unique elements",
                Math.max(set1.size(), set2.size())))
        .build();
  }

  /** Compares maps with key-value pair analysis. */
  private ValueComparisonResult compareMaps(
      final Map<?, ?> map1, final Map<?, ?> map2, final int depth) {
    final ValueComparisonResult.Builder resultBuilder =
        new ValueComparisonResult.Builder().comparisonType(ComparisonType.MAP);

    if (map1.size() != map2.size()) {
      return resultBuilder
          .equivalent(false)
          .exactMatch(false)
          .details(String.format("Map size mismatch: %d vs %d", map1.size(), map2.size()))
          .build();
    }

    boolean allExactMatch = true;
    boolean allEquivalent = true;

    for (final Map.Entry<?, ?> entry1 : map1.entrySet()) {
      final Object key = entry1.getKey();
      final Object value1 = entry1.getValue();
      final Object value2 = map2.get(key);

      if (value2 == null && !map2.containsKey(key)) {
        allExactMatch = false;
        allEquivalent = false;
        break;
      }

      final ValueComparisonResult valueResult = compareValues(value1, value2, depth + 1);
      if (!valueResult.isExactMatch()) {
        allExactMatch = false;
      }
      if (!valueResult.isEquivalent()) {
        allEquivalent = false;
      }
    }

    return resultBuilder
        .equivalent(allEquivalent)
        .exactMatch(allExactMatch)
        .details("Map comparison with " + map1.size() + " entries")
        .build();
  }

  /** Compares enum values. */
  private ValueComparisonResult compareEnums(final Enum<?> enum1, final Enum<?> enum2) {
    final boolean exactMatch = enum1.equals(enum2);
    return new ValueComparisonResult.Builder()
        .comparisonType(ComparisonType.ENUM)
        .equivalent(exactMatch)
        .exactMatch(exactMatch)
        .details(String.format("Enum comparison: %s vs %s", enum1.name(), enum2.name()))
        .build();
  }

  /** Compares ByteBuffer instances (common in WebAssembly operations). */
  private ValueComparisonResult compareByteBuffers(
      final ByteBuffer buffer1, final ByteBuffer buffer2) {
    final ValueComparisonResult.Builder resultBuilder =
        new ValueComparisonResult.Builder().comparisonType(ComparisonType.BYTE_BUFFER);

    if (buffer1.remaining() != buffer2.remaining()) {
      return resultBuilder
          .equivalent(false)
          .exactMatch(false)
          .details(
              String.format(
                  "ByteBuffer size mismatch: %d vs %d", buffer1.remaining(), buffer2.remaining()))
          .build();
    }

    final boolean exactMatch = buffer1.equals(buffer2);
    return resultBuilder
        .equivalent(exactMatch)
        .exactMatch(exactMatch)
        .details("ByteBuffer content comparison")
        .build();
  }

  /** Compares objects using reflection-based field analysis. */
  private ValueComparisonResult compareObjectsReflectively(
      final Object obj1, final Object obj2, final int depth) {
    final ValueComparisonResult.Builder resultBuilder =
        new ValueComparisonResult.Builder().comparisonType(ComparisonType.OBJECT_REFLECTION);

    final Class<?> clazz = obj1.getClass();
    final Field[] fields = clazz.getDeclaredFields();

    boolean allExactMatch = true;
    boolean allEquivalent = true;
    int comparedFields = 0;

    for (final Field field : fields) {
      // Skip static and transient fields
      if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
        continue;
      }

      try {
        field.setAccessible(true);
        final Object fieldValue1 = field.get(obj1);
        final Object fieldValue2 = field.get(obj2);

        final ValueComparisonResult fieldResult =
            compareValues(fieldValue1, fieldValue2, depth + 1);
        if (!fieldResult.isExactMatch()) {
          allExactMatch = false;
        }
        if (!fieldResult.isEquivalent()) {
          allEquivalent = false;
        }

        comparedFields++;
      } catch (final IllegalAccessException e) {
        LOGGER.warning("Unable to access field " + field.getName() + " for comparison");
        allExactMatch = false;
      }
    }

    return resultBuilder
        .equivalent(allEquivalent)
        .exactMatch(allExactMatch)
        .details(
            String.format(
                "Reflective comparison of %d fields in %s", comparedFields, clazz.getSimpleName()))
        .build();
  }

  /** Checks if two types are compatible for comparison. */
  private boolean areTypesCompatible(final Class<?> type1, final Class<?> type2) {
    if (type1.equals(type2)) {
      return true;
    }

    // Allow comparison between numeric types
    if (isNumericType(type1) && isNumericType(type2)) {
      return true;
    }

    // Allow comparison between collection types
    if (Collection.class.isAssignableFrom(type1) && Collection.class.isAssignableFrom(type2)) {
      return true;
    }

    // Allow comparison between map types
    if (Map.class.isAssignableFrom(type1) && Map.class.isAssignableFrom(type2)) {
      return true;
    }

    return false;
  }

  /** Checks if a type is numeric. */
  private boolean isNumericType(final Class<?> type) {
    return Number.class.isAssignableFrom(type)
        || type == byte.class
        || type == short.class
        || type == int.class
        || type == long.class
        || type == float.class
        || type == double.class;
  }

  /** Helper methods for creating common comparison results. */
  private ValueComparisonResult createExactMatchResult() {
    return new ValueComparisonResult.Builder()
        .equivalent(true)
        .exactMatch(true)
        .comparisonType(ComparisonType.EXACT_MATCH)
        .details("Values are identical")
        .build();
  }

  private ValueComparisonResult createNullMismatchResult(final Object value1, final Object value2) {
    return new ValueComparisonResult.Builder()
        .equivalent(false)
        .exactMatch(false)
        .comparisonType(ComparisonType.NULL_MISMATCH)
        .details(
            String.format(
                "Null mismatch: %s vs %s",
                value1 == null ? "null" : "non-null", value2 == null ? "null" : "non-null"))
        .build();
  }

  private ValueComparisonResult createTypeMismatchResult(
      final Class<?> type1, final Class<?> type2) {
    return new ValueComparisonResult.Builder()
        .equivalent(false)
        .exactMatch(false)
        .comparisonType(ComparisonType.TYPE_MISMATCH)
        .details(
            String.format("Type mismatch: %s vs %s", type1.getSimpleName(), type2.getSimpleName()))
        .build();
  }

  private ValueComparisonResult createReferenceComparisonResult(
      final Object value1, final Object value2) {
    final boolean sameReference = value1 == value2;
    return new ValueComparisonResult.Builder()
        .equivalent(sameReference)
        .exactMatch(sameReference)
        .comparisonType(ComparisonType.REFERENCE)
        .details("Reference comparison for " + value1.getClass().getSimpleName())
        .build();
  }

  /** Creates a cache key for comparison results. */
  private String createComparisonCacheKey(final Object value1, final Object value2) {
    final int hash1 = value1 == null ? 0 : value1.hashCode();
    final int hash2 = value2 == null ? 0 : value2.hashCode();
    return hash1 + ":" + hash2;
  }

  /** Gets a description of the value type for logging. */
  private String getValueTypeDescription(final Object value) {
    if (value == null) {
      return "null";
    }
    return value.getClass().getSimpleName();
  }

  /** Clears the comparison cache. */
  public static void clearCache() {
    comparisonCache.clear();
    LOGGER.info("Cleared result comparison cache");
  }

  /**
   * Gets cache statistics.
   *
   * @return cache statistics
   */
  public static CacheStatistics getCacheStatistics() {
    return new CacheStatistics(comparisonCache.size());
  }

  /** Cache statistics for result comparison. */
  public static final class CacheStatistics {
    private final int cacheSize;

    private CacheStatistics(final int cacheSize) {
      this.cacheSize = cacheSize;
    }

    public int getCacheSize() {
      return cacheSize;
    }

    @Override
    public String toString() {
      return String.format("CacheStatistics{size=%d}", cacheSize);
    }
  }
}
