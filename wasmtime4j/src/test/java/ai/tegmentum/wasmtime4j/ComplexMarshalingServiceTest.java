package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive test suite for complex parameter marshaling functionality.
 *
 * <p>This test suite validates all aspects of the complex marshaling system including
 * multi-dimensional arrays, collections, custom POJOs, memory-based marshaling, error handling, and
 * performance characteristics.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComplexMarshalingServiceTest {

  private ComplexMarshalingService marshalingService;
  private MarshalingConfiguration defaultConfig;
  private MarshalingConfiguration performanceConfig;
  private MarshalingConfiguration safetyConfig;

  @BeforeEach
  void setUp() {
    defaultConfig = MarshalingConfiguration.defaultConfiguration();
    performanceConfig = MarshalingConfiguration.performanceOptimized();
    safetyConfig = MarshalingConfiguration.safetyOptimized();
    marshalingService = new ComplexMarshalingService(defaultConfig);
  }

  @Nested
  @DisplayName("Multi-dimensional Array Marshaling Tests")
  class MultiDimensionalArrayTests {

    @Test
    @DisplayName("Should marshal and unmarshal 2D integer array")
    void shouldMarshalAndUnmarshal2DIntegerArray() throws WasmException {
      // Given
      final int[][] originalArray = {
        {1, 2, 3, 4},
        {5, 6, 7, 8},
        {9, 10, 11, 12}
      };

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(originalArray);
      final int[][] reconstructedArray = marshalingService.unmarshal(marshaledData, int[][].class);

      // Then
      assertNotNull(reconstructedArray, "Reconstructed array should not be null");
      assertEquals(
          originalArray.length, reconstructedArray.length, "Array dimensions should match");
      assertArrayEquals(originalArray, reconstructedArray, "Array contents should match");
    }

    @Test
    @DisplayName("Should marshal and unmarshal 3D double array")
    void shouldMarshalAndUnmarshal3DDoubleArray() throws WasmException {
      // Given
      final double[][][] originalArray = {
        {{1.1, 2.2}, {3.3, 4.4}},
        {{5.5, 6.6}, {7.7, 8.8}}
      };

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(originalArray);
      final double[][][] reconstructedArray =
          marshalingService.unmarshal(marshaledData, double[][][].class);

      // Then
      assertNotNull(reconstructedArray, "Reconstructed array should not be null");
      assertEquals(originalArray.length, reconstructedArray.length, "First dimension should match");
      assertEquals(
          originalArray[0].length, reconstructedArray[0].length, "Second dimension should match");
      assertEquals(
          originalArray[0][0].length,
          reconstructedArray[0][0].length,
          "Third dimension should match");

      for (int i = 0; i < originalArray.length; i++) {
        for (int j = 0; j < originalArray[i].length; j++) {
          assertArrayEquals(
              originalArray[i][j],
              reconstructedArray[i][j],
              0.001,
              "Array contents should match at [" + i + "][" + j + "]");
        }
      }
    }

    @Test
    @DisplayName("Should handle empty arrays")
    void shouldHandleEmptyArrays() throws WasmException {
      // Given
      final int[][] emptyArray = new int[0][0];

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(emptyArray);
      final int[][] reconstructedArray = marshalingService.unmarshal(marshaledData, int[][].class);

      // Then
      assertNotNull(reconstructedArray, "Reconstructed array should not be null");
      assertEquals(0, reconstructedArray.length, "Array should be empty");
    }

    @Test
    @DisplayName("Should handle arrays with null elements")
    void shouldHandleArraysWithNullElements() throws WasmException {
      // Given
      final String[][] arrayWithNulls = {
        {"hello", null, "world"},
        {null, "test", null}
      };

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(arrayWithNulls);
      final String[][] reconstructedArray =
          marshalingService.unmarshal(marshaledData, String[][].class);

      // Then
      assertNotNull(reconstructedArray, "Reconstructed array should not be null");
      assertEquals(
          arrayWithNulls.length, reconstructedArray.length, "Array dimensions should match");
      assertArrayEquals(
          arrayWithNulls, reconstructedArray, "Array contents including nulls should match");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 50, 100})
    @DisplayName("Should handle various array sizes efficiently")
    void shouldHandleVariousArraySizesEfficiently(final int size) throws WasmException {
      // Given
      final int[][] array = new int[size][size];
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
          array[i][j] = i * size + j;
        }
      }

      // When
      final long startTime = System.nanoTime();
      final ComplexMarshalingService.MarshaledData marshaledData = marshalingService.marshal(array);
      final int[][] reconstructedArray = marshalingService.unmarshal(marshaledData, int[][].class);
      final long endTime = System.nanoTime();

      // Then
      assertNotNull(reconstructedArray, "Reconstructed array should not be null");
      assertArrayEquals(array, reconstructedArray, "Array contents should match for size " + size);

      final long durationMs = (endTime - startTime) / 1_000_000;
      assertTrue(
          durationMs < 5000,
          "Marshaling should complete within 5 seconds for size "
              + size
              + " (actual: "
              + durationMs
              + "ms)");
    }
  }

  @Nested
  @DisplayName("Collection Marshaling Tests")
  class CollectionMarshalingTests {

    @Test
    @DisplayName("Should marshal and unmarshal List with mixed types")
    void shouldMarshalAndUnmarshalListWithMixedTypes() throws WasmException {
      // Given
      final List<Object> originalList = new ArrayList<>();
      originalList.add(42);
      originalList.add("hello");
      originalList.add(3.14);
      originalList.add(true);
      originalList.add(null);

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(originalList);
      final List<?> reconstructedList = marshalingService.unmarshal(marshaledData, List.class);

      // Then
      assertNotNull(reconstructedList, "Reconstructed list should not be null");
      assertEquals(originalList.size(), reconstructedList.size(), "List sizes should match");
      assertEquals(originalList, reconstructedList, "List contents should match");
    }

    @Test
    @DisplayName("Should marshal and unmarshal Map with complex keys and values")
    void shouldMarshalAndUnmarshalMapWithComplexKeysAndValues() throws WasmException {
      // Given
      final Map<String, Object> originalMap = new HashMap<>();
      originalMap.put("integer", 42);
      originalMap.put("string", "hello world");
      originalMap.put("list", Arrays.asList(1, 2, 3));
      originalMap.put("nested_map", Map.of("key", "value"));
      originalMap.put("null_value", null);

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(originalMap);
      final Map<?, ?> reconstructedMap = marshalingService.unmarshal(marshaledData, Map.class);

      // Then
      assertNotNull(reconstructedMap, "Reconstructed map should not be null");
      assertEquals(originalMap.size(), reconstructedMap.size(), "Map sizes should match");
      assertEquals(originalMap, reconstructedMap, "Map contents should match");
    }

    @Test
    @DisplayName("Should handle nested collections")
    void shouldHandleNestedCollections() throws WasmException {
      // Given
      final List<List<String>> nestedList = new ArrayList<>();
      nestedList.add(Arrays.asList("a", "b", "c"));
      nestedList.add(Arrays.asList("d", "e", "f"));
      nestedList.add(new ArrayList<>()); // Empty nested list

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(nestedList);
      final List<?> reconstructedList = marshalingService.unmarshal(marshaledData, List.class);

      // Then
      assertNotNull(reconstructedList, "Reconstructed list should not be null");
      assertEquals(nestedList.size(), reconstructedList.size(), "Nested list sizes should match");
      assertEquals(nestedList, reconstructedList, "Nested list contents should match");
    }

    @Test
    @DisplayName("Should handle large collections efficiently")
    void shouldHandleLargeCollectionsEfficiently() throws WasmException {
      // Given
      final List<Integer> largeList = new ArrayList<>();
      for (int i = 0; i < 10000; i++) {
        largeList.add(i);
      }

      // When
      final long startTime = System.nanoTime();
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(largeList);
      final List<?> reconstructedList = marshalingService.unmarshal(marshaledData, List.class);
      final long endTime = System.nanoTime();

      // Then
      assertNotNull(reconstructedList, "Reconstructed list should not be null");
      assertEquals(largeList.size(), reconstructedList.size(), "Large list sizes should match");
      assertEquals(largeList, reconstructedList, "Large list contents should match");

      final long durationMs = (endTime - startTime) / 1_000_000;
      assertTrue(
          durationMs < 10000,
          "Large collection marshaling should complete within 10 seconds"
              + " (actual: "
              + durationMs
              + "ms)");
    }
  }

  @Nested
  @DisplayName("Custom Object Marshaling Tests")
  class CustomObjectMarshalingTests {

    @Test
    @DisplayName("Should marshal and unmarshal simple POJO")
    void shouldMarshalAndUnmarshalSimplePojo() throws WasmException {
      // Given
      final TestPojo originalPojo = new TestPojo("John Doe", 30, 75000.0);

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(originalPojo);
      final TestPojo reconstructedPojo = marshalingService.unmarshal(marshaledData, TestPojo.class);

      // Then
      assertNotNull(reconstructedPojo, "Reconstructed POJO should not be null");
      assertEquals(originalPojo.getName(), reconstructedPojo.getName(), "POJO name should match");
      assertEquals(originalPojo.getAge(), reconstructedPojo.getAge(), "POJO age should match");
      assertEquals(
          originalPojo.getSalary(),
          reconstructedPojo.getSalary(),
          0.01,
          "POJO salary should match");
    }

    @Test
    @DisplayName("Should marshal and unmarshal nested POJOs")
    void shouldMarshalAndUnmarshalNestedPojos() throws WasmException {
      // Given
      final TestAddress address = new TestAddress("123 Main St", "Anytown", "12345");
      final TestPersonWithAddress person = new TestPersonWithAddress("Jane Smith", 25, address);

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(person);
      final TestPersonWithAddress reconstructedPerson =
          marshalingService.unmarshal(marshaledData, TestPersonWithAddress.class);

      // Then
      assertNotNull(reconstructedPerson, "Reconstructed person should not be null");
      assertEquals(person.getName(), reconstructedPerson.getName(), "Person name should match");
      assertEquals(person.getAge(), reconstructedPerson.getAge(), "Person age should match");

      assertNotNull(reconstructedPerson.getAddress(), "Address should not be null");
      assertEquals(
          person.getAddress().getStreet(),
          reconstructedPerson.getAddress().getStreet(),
          "Address street should match");
      assertEquals(
          person.getAddress().getCity(),
          reconstructedPerson.getAddress().getCity(),
          "Address city should match");
      assertEquals(
          person.getAddress().getZipCode(),
          reconstructedPerson.getAddress().getZipCode(),
          "Address zip code should match");
    }

    @Test
    @DisplayName("Should handle POJOs with collections")
    void shouldHandlePojosWithCollections() throws WasmException {
      // Given
      final TestCompany company = new TestCompany();
      company.setName("ACME Corp");
      company.setEmployees(Arrays.asList("Alice", "Bob", "Charlie"));
      company.setDepartments(
          Map.of(
              "Engineering", 10,
              "Sales", 5,
              "HR", 2));

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(company);
      final TestCompany reconstructedCompany =
          marshalingService.unmarshal(marshaledData, TestCompany.class);

      // Then
      assertNotNull(reconstructedCompany, "Reconstructed company should not be null");
      assertEquals(company.getName(), reconstructedCompany.getName(), "Company name should match");
      assertEquals(
          company.getEmployees(), reconstructedCompany.getEmployees(), "Employees should match");
      assertEquals(
          company.getDepartments(),
          reconstructedCompany.getDepartments(),
          "Departments should match");
    }
  }

  @Nested
  @DisplayName("Memory-based Marshaling Tests")
  class MemoryBasedMarshalingTests {

    @Test
    @DisplayName("Should use memory-based marshaling for large objects")
    void shouldUseMemoryBasedMarshalingForLargeObjects() throws WasmException {
      // Given - create a large byte array
      final byte[] largeArray = new byte[10 * 1024 * 1024]; // 10MB
      for (int i = 0; i < largeArray.length; i++) {
        largeArray[i] = (byte) (i % 256);
      }

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(largeArray);

      // Then
      assertEquals(
          ComplexMarshalingService.MarshalingStrategy.MEMORY_BASED,
          marshaledData.getStrategy(),
          "Should use memory-based marshaling for large objects");
      assertNotNull(marshaledData.getMemoryHandle(), "Memory handle should be present");
      assertNull(
          marshaledData.getValueData(),
          "Value data should not be present for memory-based marshaling");

      // When - unmarshal
      final byte[] reconstructedArray = marshalingService.unmarshal(marshaledData, byte[].class);

      // Then
      assertNotNull(reconstructedArray, "Reconstructed array should not be null");
      assertEquals(largeArray.length, reconstructedArray.length, "Array lengths should match");
      assertArrayEquals(largeArray, reconstructedArray, "Array contents should match");
    }

    @Test
    @DisplayName("Should handle memory allocation failures gracefully")
    void shouldHandleMemoryAllocationFailuresGracefully() {
      // Given - create an extremely large object that would exceed memory limits
      final int[] extremelyLargeArray =
          new int[Integer.MAX_VALUE / 8]; // This should trigger memory issues

      // When & Then
      assertThrows(
          WasmException.class,
          () -> {
            marshalingService.marshal(extremelyLargeArray);
          },
          "Should throw WasmException for memory allocation failures");
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw MarshalingException for null objects")
    void shouldThrowMarshalingExceptionForNullObjects() {
      // When & Then
      final NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> {
                marshalingService.marshal(null);
              },
              "Should throw NullPointerException for null objects");

      assertTrue(
          exception.getMessage().contains("cannot be null"),
          "Exception message should indicate null object");
    }

    @Test
    @DisplayName("Should throw MarshalingException for incompatible types during unmarshaling")
    void shouldThrowMarshalingExceptionForIncompatibleTypes() throws WasmException {
      // Given
      final String originalString = "Hello, World!";
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(originalString);

      // When & Then
      assertThrows(
          WasmException.class,
          () -> {
            marshalingService.unmarshal(marshaledData, Integer.class);
          },
          "Should throw WasmException for incompatible types");
    }

    @Test
    @DisplayName("Should provide detailed error information in exceptions")
    void shouldProvideDetailedErrorInformationInExceptions() {
      // Given - an object that will cause marshaling issues
      final Object problematicObject =
          new Object() {
            @Override
            public String toString() {
              throw new RuntimeException("toString() failure");
            }
          };

      // When & Then
      final WasmException exception =
          assertThrows(
              WasmException.class,
              () -> {
                marshalingService.marshal(problematicObject);
              },
              "Should throw WasmException for problematic objects");

      assertNotNull(exception.getMessage(), "Exception should have a message");
      assertTrue(
          exception.getMessage().contains("Failed to marshal"),
          "Exception message should indicate marshaling failure");
    }

    @Test
    @DisplayName("Should handle circular references when detection is enabled")
    void shouldHandleCircularReferencesWhenDetectionIsEnabled() {
      // Given - configuration with circular reference detection enabled
      final MarshalingConfiguration config =
          MarshalingConfiguration.builder().withCircularReferenceDetection(true).build();
      final ComplexMarshalingService serviceWithDetection = new ComplexMarshalingService(config);

      // Create circular reference
      final TestNodeWithCircularRef nodeA = new TestNodeWithCircularRef("A");
      final TestNodeWithCircularRef nodeB = new TestNodeWithCircularRef("B");
      nodeA.setNext(nodeB);
      nodeB.setNext(nodeA); // Circular reference

      // When & Then
      assertThrows(
          WasmException.class,
          () -> {
            serviceWithDetection.marshal(nodeA);
          },
          "Should detect and reject circular references when detection is enabled");
    }
  }

  @Nested
  @DisplayName("Performance Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Should estimate object sizes accurately")
    void shouldEstimateObjectSizesAccurately() {
      // Given
      final String smallString = "hello";
      final String largeString = "x".repeat(10000);
      final int[] smallArray = {1, 2, 3, 4, 5};
      final int[] largeArray = new int[10000];

      // When
      final long smallStringSize = marshalingService.estimateSerializedSize(smallString);
      final long largeStringSize = marshalingService.estimateSerializedSize(largeString);
      final long smallArraySize = marshalingService.estimateSerializedSize(smallArray);
      final long largeArraySize = marshalingService.estimateSerializedSize(largeArray);

      // Then
      assertTrue(smallStringSize > 0, "Small string should have positive estimated size");
      assertTrue(
          largeStringSize > smallStringSize, "Large string should have larger estimated size");
      assertTrue(smallArraySize > 0, "Small array should have positive estimated size");
      assertTrue(largeArraySize > smallArraySize, "Large array should have larger estimated size");

      // Verify estimates are reasonable (not orders of magnitude off)
      assertTrue(largeStringSize < 50000, "Large string estimate should be reasonable");
      assertTrue(largeArraySize < 100000, "Large array estimate should be reasonable");
    }

    @Test
    @DisplayName("Should select appropriate marshaling strategies")
    void shouldSelectAppropriateMarshalingStrategies() throws WasmException {
      // Given
      final String smallObject = "small";
      final byte[] mediumObject = new byte[1024];
      final byte[] largeObject = new byte[10 * 1024 * 1024];

      // When
      final ComplexMarshalingService.MarshaledData smallData =
          marshalingService.marshal(smallObject);
      final ComplexMarshalingService.MarshaledData mediumData =
          marshalingService.marshal(mediumObject);
      final ComplexMarshalingService.MarshaledData largeData =
          marshalingService.marshal(largeObject);

      // Then
      assertEquals(
          ComplexMarshalingService.MarshalingStrategy.VALUE_BASED,
          smallData.getStrategy(),
          "Small objects should use value-based marshaling");

      // Medium and large objects should use more sophisticated strategies
      assertTrue(
          mediumData.getStrategy() == ComplexMarshalingService.MarshalingStrategy.VALUE_BASED
              || mediumData.getStrategy() == ComplexMarshalingService.MarshalingStrategy.HYBRID,
          "Medium objects should use value-based or hybrid marshaling");

      assertEquals(
          ComplexMarshalingService.MarshalingStrategy.MEMORY_BASED,
          largeData.getStrategy(),
          "Large objects should use memory-based marshaling");
    }

    @Test
    @DisplayName("Should perform marshaling within acceptable time limits")
    void shouldPerformMarshalingWithinAcceptableTimeLimits() throws WasmException {
      // Given
      final List<Integer> moderateList = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        moderateList.add(i);
      }

      // When
      final long startTime = System.nanoTime();
      final ComplexMarshalingService.MarshaledData marshaledData =
          marshalingService.marshal(moderateList);
      final List<?> reconstructedList = marshalingService.unmarshal(marshaledData, List.class);
      final long endTime = System.nanoTime();

      // Then
      final long durationMs = (endTime - startTime) / 1_000_000;
      assertTrue(
          durationMs < 1000,
          "Marshaling 1000 elements should complete within 1 second"
              + " (actual: "
              + durationMs
              + "ms)");
      assertEquals(moderateList, reconstructedList, "Marshaling should preserve data integrity");
    }
  }

  @Nested
  @DisplayName("Configuration Tests")
  class ConfigurationTests {

    @Test
    @DisplayName("Should respect marshaling threshold configurations")
    void shouldRespectMarshalingThresholdConfigurations() throws WasmException {
      // Given
      final MarshalingConfiguration lowThresholdConfig =
          MarshalingConfiguration.builder()
              .withValueMarshalingThreshold(100)
              .withHybridMarshalingThreshold(200)
              .build();

      final ComplexMarshalingService lowThresholdService =
          new ComplexMarshalingService(lowThresholdConfig);
      final byte[] mediumArray = new byte[150]; // Between thresholds

      // When
      final ComplexMarshalingService.MarshaledData marshaledData =
          lowThresholdService.marshal(mediumArray);

      // Then
      assertEquals(
          ComplexMarshalingService.MarshalingStrategy.HYBRID,
          marshaledData.getStrategy(),
          "Should use hybrid strategy for medium objects with low thresholds");
    }

    @Test
    @DisplayName("Should handle different configuration presets correctly")
    void shouldHandleDifferentConfigurationPresetsCorrectly() {
      // When
      final MarshalingConfiguration defaultConfig = MarshalingConfiguration.defaultConfiguration();
      final MarshalingConfiguration performanceConfig =
          MarshalingConfiguration.performanceOptimized();
      final MarshalingConfiguration safetyConfig = MarshalingConfiguration.safetyOptimized();

      // Then
      assertNotNull(defaultConfig, "Default configuration should not be null");
      assertNotNull(performanceConfig, "Performance configuration should not be null");
      assertNotNull(safetyConfig, "Safety configuration should not be null");

      // Performance config should have higher thresholds
      assertTrue(
          performanceConfig.getValueMarshalingThreshold()
              >= defaultConfig.getValueMarshalingThreshold(),
          "Performance config should have higher or equal value threshold");
      assertTrue(
          performanceConfig.getHybridMarshalingThreshold()
              >= defaultConfig.getHybridMarshalingThreshold(),
          "Performance config should have higher or equal hybrid threshold");

      // Safety config should have lower thresholds and more checks
      assertTrue(
          safetyConfig.getValueMarshalingThreshold() <= defaultConfig.getValueMarshalingThreshold(),
          "Safety config should have lower or equal value threshold");
      assertTrue(
          safetyConfig.isCircularReferenceDetectionEnabled(),
          "Safety config should enable circular reference detection");
      assertTrue(
          safetyConfig.isTypeValidationEnabled(), "Safety config should enable type validation");
    }
  }

  // Test helper classes
  public static class TestPojo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int age;
    private double salary;

    public TestPojo() {}

    public TestPojo(final String name, final int age, final double salary) {
      this.name = name;
      this.age = age;
      this.salary = salary;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(final int age) {
      this.age = age;
    }

    public double getSalary() {
      return salary;
    }

    public void setSalary(final double salary) {
      this.salary = salary;
    }
  }

  public static class TestAddress implements Serializable {
    private static final long serialVersionUID = 1L;

    private String street;
    private String city;
    private String zipCode;

    public TestAddress() {}

    public TestAddress(final String street, final String city, final String zipCode) {
      this.street = street;
      this.city = city;
      this.zipCode = zipCode;
    }

    public String getStreet() {
      return street;
    }

    public void setStreet(final String street) {
      this.street = street;
    }

    public String getCity() {
      return city;
    }

    public void setCity(final String city) {
      this.city = city;
    }

    public String getZipCode() {
      return zipCode;
    }

    public void setZipCode(final String zipCode) {
      this.zipCode = zipCode;
    }
  }

  public static class TestPersonWithAddress implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int age;
    private TestAddress address;

    public TestPersonWithAddress() {}

    public TestPersonWithAddress(final String name, final int age, final TestAddress address) {
      this.name = name;
      this.age = age;
      this.address = address;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(final int age) {
      this.age = age;
    }

    public TestAddress getAddress() {
      return address;
    }

    public void setAddress(final TestAddress address) {
      this.address = address;
    }
  }

  public static class TestCompany implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private List<String> employees;
    private Map<String, Integer> departments;

    public TestCompany() {}

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public List<String> getEmployees() {
      return employees;
    }

    public void setEmployees(final List<String> employees) {
      this.employees = employees;
    }

    public Map<String, Integer> getDepartments() {
      return departments;
    }

    public void setDepartments(final Map<String, Integer> departments) {
      this.departments = departments;
    }
  }

  public static class TestNodeWithCircularRef implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private TestNodeWithCircularRef next;

    public TestNodeWithCircularRef(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public TestNodeWithCircularRef getNext() {
      return next;
    }

    public void setNext(final TestNodeWithCircularRef next) {
      this.next = next;
    }
  }
}
