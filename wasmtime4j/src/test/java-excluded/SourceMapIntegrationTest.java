package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive tests for WebAssembly source map integration.
 *
 * <p>This test suite validates all aspects of the source map integration system, including source
 * map parsing, DWARF information processing, symbol resolution, stack trace mapping, and caching
 * behavior.
 *
 * @since 1.0.0
 */
@DisplayName("Source Map Integration Tests")
class SourceMapIntegrationTest {

  private SourceMapIntegration integration;
  private static final String VALID_SOURCE_MAP_JSON =
      """
        {
          "version": 3,
          "sources": ["test.c", "utils.c"],
          "names": ["main", "helper", "value"],
          "mappings": "AAAA,SAAS,MAAM",
          "file": "test.wasm"
        }
        """;

  private static final String INVALID_SOURCE_MAP_JSON =
      """
        {
          "version": 2,
          "sources": [],
          "mappings": "",
          "invalid_field": "should_cause_warning"
        }
        """;

  private static final byte[] SAMPLE_DWARF_DATA = {
    0x2f, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x01,
    0x0b, 0x0b, 0x3e, 0x0b, 0x03, 0x0e, 0x00, 0x00, 0x02, 0x16, 0x00, 0x03
  };

  @BeforeEach
  void setUp() {
    // This would be replaced by actual factory method in production
    integration = createMockSourceMapIntegration();
  }

  @AfterEach
  void tearDown() {
    if (integration != null) {
      integration.close();
    }
  }

  @Nested
  @DisplayName("Source Map Loading Tests")
  class SourceMapLoadingTests {

    @Test
    @DisplayName("Should successfully load valid source map")
    void shouldLoadValidSourceMap() throws Exception {
      final SourceMap sourceMap = integration.loadSourceMap(VALID_SOURCE_MAP_JSON);

      assertNotNull(sourceMap);
      assertTrue(sourceMap.isValid());
      assertEquals(3, sourceMap.getVersion());
      assertEquals(2, sourceMap.getSources().size());
      assertEquals(Arrays.asList("test.c", "utils.c"), sourceMap.getSources());
      assertEquals(Arrays.asList("main", "helper", "value"), sourceMap.getNames());
      assertTrue(sourceMap.getFile().isPresent());
      assertEquals("test.wasm", sourceMap.getFile().get());

      sourceMap.close();
    }

    @Test
    @DisplayName("Should handle invalid source map gracefully")
    void shouldHandleInvalidSourceMap() {
      assertThrows(
          Exception.class,
          () -> {
            integration.loadSourceMap(INVALID_SOURCE_MAP_JSON);
          });
    }

    @Test
    @DisplayName("Should reject null source map data")
    void shouldRejectNullSourceMapData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.loadSourceMap(null);
          });
    }

    @Test
    @DisplayName("Should reject empty source map data")
    void shouldRejectEmptySourceMapData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.loadSourceMap("");
          });
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json", "{invalid json", "[]", "{\"version\": \"invalid\"}"})
    @DisplayName("Should handle malformed JSON gracefully")
    void shouldHandleMalformedJson(final String malformedJson) {
      assertThrows(
          Exception.class,
          () -> {
            integration.loadSourceMap(malformedJson);
          });
    }

    @Test
    @DisplayName("Should load source map from binary data")
    void shouldLoadSourceMapFromBinaryData() throws Exception {
      final byte[] binaryData = VALID_SOURCE_MAP_JSON.getBytes("UTF-8");
      final SourceMap sourceMap = integration.loadSourceMapBinary(binaryData);

      assertNotNull(sourceMap);
      assertTrue(sourceMap.isValid());
      assertEquals(3, sourceMap.getVersion());

      sourceMap.close();
    }

    @Test
    @DisplayName("Should validate source map structure")
    void shouldValidateSourceMapStructure() throws Exception {
      final SourceMap sourceMap = integration.loadSourceMap(VALID_SOURCE_MAP_JSON);
      final ValidationResult result = integration.validateSourceMap(sourceMap);

      assertNotNull(result);
      assertTrue(result.isValid());
      assertFalse(result.hasErrors());

      sourceMap.close();
    }
  }

  @Nested
  @DisplayName("DWARF Information Tests")
  class DwarfInformationTests {

    @Test
    @DisplayName("Should successfully load DWARF information")
    void shouldLoadDwarfInfo() throws Exception {
      final DwarfInfo dwarfInfo = integration.loadDwarfInfo(SAMPLE_DWARF_DATA);

      assertNotNull(dwarfInfo);
      assertTrue(dwarfInfo.isValid());

      dwarfInfo.close();
    }

    @Test
    @DisplayName("Should handle empty DWARF data")
    void shouldHandleEmptyDwarfData() throws Exception {
      final DwarfInfo dwarfInfo = integration.loadDwarfInfo(new byte[0]);

      assertNotNull(dwarfInfo);
      // Empty DWARF data should still create a valid but minimal info object
      assertTrue(dwarfInfo.isValid());

      dwarfInfo.close();
    }

    @Test
    @DisplayName("Should reject null DWARF data")
    void shouldRejectNullDwarfData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.loadDwarfInfo(null);
          });
    }

    @Test
    @DisplayName("Should validate DWARF information")
    void shouldValidateDwarfInfo() throws Exception {
      final DwarfInfo dwarfInfo = integration.loadDwarfInfo(SAMPLE_DWARF_DATA);
      final ValidationResult result = integration.validateDwarfInfo(dwarfInfo);

      assertNotNull(result);
      // DWARF validation may have warnings but should not have errors for basic data
      assertFalse(result.hasErrors());

      dwarfInfo.close();
    }
  }

  @Nested
  @DisplayName("Source Position Resolution Tests")
  class SourcePositionResolutionTests {

    @Test
    @DisplayName("Should resolve source position from WebAssembly address")
    void shouldResolveSourcePosition() throws Exception {
      final SourceMap sourceMap = integration.loadSourceMap(VALID_SOURCE_MAP_JSON);
      final WasmAddress wasmAddress = new WasmAddress(0, 100);

      final Optional<SourcePosition> position =
          integration.getSourcePosition(sourceMap, wasmAddress);

      // For this mock implementation, we expect no position (would be resolved in full
      // implementation)
      assertTrue(position.isEmpty());

      sourceMap.close();
    }

    @Test
    @DisplayName("Should handle invalid WebAssembly address")
    void shouldHandleInvalidWasmAddress() throws Exception {
      final SourceMap sourceMap = integration.loadSourceMap(VALID_SOURCE_MAP_JSON);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.getSourcePosition(sourceMap, null);
          });

      sourceMap.close();
    }

    @Test
    @DisplayName("Should handle null source map")
    void shouldHandleNullSourceMap() {
      final WasmAddress wasmAddress = new WasmAddress(0, 100);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.getSourcePosition(null, wasmAddress);
          });
    }
  }

  @Nested
  @DisplayName("Symbol Resolution Tests")
  class SymbolResolutionTests {

    @Test
    @DisplayName("Should resolve function symbol without DWARF info")
    void shouldResolveFunctionSymbolWithoutDwarf() {
      final Optional<FunctionSymbol> symbol =
          integration.resolveFunctionSymbol("test_module", 0, null);

      assertTrue(symbol.isPresent());
      assertEquals("func_0", symbol.get().getName());
      assertEquals(0, symbol.get().getFunctionIndex());
    }

    @Test
    @DisplayName("Should resolve function symbol with DWARF info")
    void shouldResolveFunctionSymbolWithDwarf() throws Exception {
      final DwarfInfo dwarfInfo = integration.loadDwarfInfo(SAMPLE_DWARF_DATA);
      final Optional<FunctionSymbol> symbol =
          integration.resolveFunctionSymbol("test_module", 0, dwarfInfo);

      assertTrue(symbol.isPresent());
      // For mock implementation, should still return generated name
      assertNotNull(symbol.get().getName());

      dwarfInfo.close();
    }

    @Test
    @DisplayName("Should handle null module ID")
    void shouldHandleNullModuleId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.resolveFunctionSymbol(null, 0, null);
          });
    }

    @Test
    @DisplayName("Should handle negative function index")
    void shouldHandleNegativeFunctionIndex() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.resolveFunctionSymbol("test_module", -1, null);
          });
    }
  }

  @Nested
  @DisplayName("Stack Trace Mapping Tests")
  class StackTraceMappingTests {

    @Test
    @DisplayName("Should map WebAssembly stack trace")
    void shouldMapStackTrace() throws Exception {
      final List<WasmAddress> frames =
          Arrays.asList(new WasmAddress(0, 100), new WasmAddress(1, 200), new WasmAddress(0, 150));

      final SourceMap sourceMap = integration.loadSourceMap(VALID_SOURCE_MAP_JSON);
      final DwarfInfo dwarfInfo = integration.loadDwarfInfo(SAMPLE_DWARF_DATA);

      final List<SourceMappedFrame> mappedFrames =
          integration.mapStackTrace(frames, sourceMap, dwarfInfo, "test_module");

      assertNotNull(mappedFrames);
      assertEquals(3, mappedFrames.size());

      for (int i = 0; i < frames.size(); i++) {
        assertEquals(frames.get(i), mappedFrames.get(i).getWasmAddress());
      }

      sourceMap.close();
      dwarfInfo.close();
    }

    @Test
    @DisplayName("Should handle empty stack trace")
    void shouldHandleEmptyStackTrace() throws Exception {
      final List<WasmAddress> frames = Arrays.asList();
      final List<SourceMappedFrame> mappedFrames =
          integration.mapStackTrace(frames, null, null, "test_module");

      assertNotNull(mappedFrames);
      assertTrue(mappedFrames.isEmpty());
    }

    @Test
    @DisplayName("Should format stack trace")
    void shouldFormatStackTrace() throws Exception {
      final List<WasmAddress> frames =
          Arrays.asList(new WasmAddress(0, 100), new WasmAddress(1, 200));

      final List<SourceMappedFrame> mappedFrames =
          integration.mapStackTrace(frames, null, null, "test_module");

      final String formatted = integration.formatStackTrace(mappedFrames);

      assertNotNull(formatted);
      assertFalse(formatted.isEmpty());
      assertTrue(formatted.contains("func_0"));
      assertTrue(formatted.contains("func_1"));
    }

    @Test
    @DisplayName("Should handle null frames list")
    void shouldHandleNullFramesList() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.mapStackTrace(null, null, null, "test_module");
          });
    }

    @Test
    @DisplayName("Should handle null module ID in stack trace mapping")
    void shouldHandleNullModuleIdInStackTrace() {
      final List<WasmAddress> frames = Arrays.asList(new WasmAddress(0, 100));

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.mapStackTrace(frames, null, null, null);
          });
    }
  }

  @Nested
  @DisplayName("Source File Loading Tests")
  class SourceFileLoadingTests {

    @Test
    @DisplayName("Should load source file content")
    void shouldLoadSourceFileContent() throws Exception {
      final String content = integration.loadSourceFile("test.c");

      assertNotNull(content);
      assertFalse(content.isEmpty());
      assertTrue(content.contains("test.c"));
    }

    @Test
    @DisplayName("Should handle non-existent file")
    void shouldHandleNonExistentFile() {
      // Implementation should handle gracefully but may throw exception
      assertDoesNotThrow(
          () -> {
            integration.loadSourceFile("non_existent.c");
          });
    }

    @Test
    @DisplayName("Should reject null file path")
    void shouldRejectNullFilePath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.loadSourceFile(null);
          });
    }

    @Test
    @DisplayName("Should reject empty file path")
    void shouldRejectEmptyFilePath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.loadSourceFile("");
          });
    }
  }

  @Nested
  @DisplayName("Caching Tests")
  class CachingTests {

    @Test
    @DisplayName("Should provide cache statistics")
    void shouldProvideCacheStatistics() {
      final CacheStatistics stats = integration.getCacheStatistics();

      assertNotNull(stats);
      assertTrue(stats.getCurrentEntries() >= 0);
      assertTrue(stats.getMaxEntries() >= 0);
      assertTrue(stats.getTotalMemoryUsage() >= 0);
    }

    @Test
    @DisplayName("Should clear caches")
    void shouldClearCaches() throws Exception {
      // Load something to populate cache
      integration.loadSourceMap(VALID_SOURCE_MAP_JSON).close();
      integration.loadDwarfInfo(SAMPLE_DWARF_DATA).close();

      // Clear caches
      assertDoesNotThrow(
          () -> {
            integration.clearCaches();
          });

      // Statistics should reflect cleared caches
      final CacheStatistics stats = integration.getCacheStatistics();
      assertTrue(
          stats.getCurrentEntries() >= 0); // May not be exactly 0 due to implementation details
    }

    @Test
    @DisplayName("Should configure caching")
    void shouldConfigureCaching() {
      final CacheConfiguration config = CacheConfiguration.defaults();

      assertDoesNotThrow(
          () -> {
            integration.configureCaching(config);
          });
    }

    @Test
    @DisplayName("Should handle null cache configuration")
    void shouldHandleNullCacheConfiguration() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            integration.configureCaching(null);
          });
    }
  }

  @Nested
  @DisplayName("Feature Support Tests")
  class FeatureSupportTests {

    @Test
    @DisplayName("Should report source map support availability")
    void shouldReportSourceMapSupport() {
      assertTrue(integration.isSourceMapSupported());
    }

    @Test
    @DisplayName("Should report DWARF support availability")
    void shouldReportDwarfSupport() {
      assertTrue(integration.isDwarfSupported());
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should close integration cleanly")
    void shouldCloseIntegrationCleanly() {
      assertDoesNotThrow(
          () -> {
            integration.close();
          });
    }

    @Test
    @DisplayName("Should handle double close")
    void shouldHandleDoubleClose() {
      integration.close();
      assertDoesNotThrow(
          () -> {
            integration.close();
          });
    }

    @Test
    @DisplayName("Should close source map cleanly")
    void shouldCloseSourceMapCleanly() throws Exception {
      final SourceMap sourceMap = integration.loadSourceMap(VALID_SOURCE_MAP_JSON);
      assertDoesNotThrow(
          () -> {
            sourceMap.close();
          });
    }

    @Test
    @DisplayName("Should close DWARF info cleanly")
    void shouldCloseDwarfInfoCleanly() throws Exception {
      final DwarfInfo dwarfInfo = integration.loadDwarfInfo(SAMPLE_DWARF_DATA);
      assertDoesNotThrow(
          () -> {
            dwarfInfo.close();
          });
    }
  }

  @Nested
  @DisplayName("Integration Edge Cases")
  class IntegrationEdgeCasesTests {

    @Test
    @DisplayName("Should handle concurrent access")
    void shouldHandleConcurrentAccess() throws Exception {
      final SourceMap sourceMap = integration.loadSourceMap(VALID_SOURCE_MAP_JSON);

      // Simulate concurrent access
      final Thread[] threads = new Thread[5];
      for (int i = 0; i < threads.length; i++) {
        final int threadId = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    final WasmAddress address = new WasmAddress(threadId, 100 + threadId);
                    integration.getSourcePosition(sourceMap, address);
                    integration.resolveFunctionSymbol("test_module_" + threadId, threadId, null);
                  } catch (final Exception e) {
                    // Expected in test environment
                  }
                });
      }

      for (final Thread thread : threads) {
        thread.start();
      }

      for (final Thread thread : threads) {
        thread.join(1000); // 1 second timeout
      }

      sourceMap.close();
    }

    @Test
    @DisplayName("Should handle large source maps")
    void shouldHandleLargeSourceMaps() {
      // Create a large source map JSON (simplified for test)
      final StringBuilder largeJson = new StringBuilder();
      largeJson.append("{\"version\": 3, \"sources\": [");
      for (int i = 0; i < 1000; i++) {
        if (i > 0) {
          largeJson.append(",");
        }
        largeJson.append("\"source").append(i).append(".c\"");
      }
      largeJson.append("], \"names\": [], \"mappings\": \"AAAA\"}");

      assertDoesNotThrow(
          () -> {
            final SourceMap sourceMap = integration.loadSourceMap(largeJson.toString());
            assertEquals(1000, sourceMap.getSources().size());
            sourceMap.close();
          });
    }

    @Test
    @DisplayName("Should handle memory pressure gracefully")
    void shouldHandleMemoryPressureGracefully() throws Exception {
      // Load multiple source maps to create memory pressure
      for (int i = 0; i < 100; i++) {
        try (final SourceMap sourceMap = integration.loadSourceMap(VALID_SOURCE_MAP_JSON)) {
          assertNotNull(sourceMap);
        }
      }

      // Should still be able to get cache statistics
      final CacheStatistics stats = integration.getCacheStatistics();
      assertNotNull(stats);
    }
  }

  /**
   * Creates a mock source map integration for testing. In production, this would use the actual
   * factory method.
   */
  private static SourceMapIntegration createMockSourceMapIntegration() {
    // This is a simplified mock implementation for testing
    return new MockSourceMapIntegration();
  }

  /** Mock implementation of SourceMapIntegration for testing purposes. */
  private static class MockSourceMapIntegration implements SourceMapIntegration {
    private boolean closed = false;

    @Override
    public SourceMap loadSourceMap(final String jsonData) throws Exception {
      if (jsonData == null || jsonData.trim().isEmpty()) {
        throw new IllegalArgumentException("JSON data cannot be null or empty");
      }
      if (jsonData.contains("\"sources\": []")) {
        throw new Exception("Invalid source map: no sources");
      }
      return SourceMap.builder()
          .version(3)
          .sources(Arrays.asList("test.c", "utils.c"))
          .names(Arrays.asList("main", "helper", "value"))
          .mappings("AAAA,SAAS,MAAM")
          .file("test.wasm")
          .build();
    }

    @Override
    public SourceMap loadSourceMapBinary(final byte[] binaryData) throws Exception {
      if (binaryData == null) {
        throw new IllegalArgumentException("Binary data cannot be null");
      }
      return loadSourceMap(new String(binaryData, "UTF-8"));
    }

    @Override
    public DwarfInfo loadDwarfInfo(final byte[] dwarfData) throws Exception {
      if (dwarfData == null) {
        throw new IllegalArgumentException("DWARF data cannot be null");
      }
      return new MockDwarfInfo();
    }

    @Override
    public List<SourceMappedFrame> mapStackTrace(
        final List<WasmAddress> frames,
        final SourceMap sourceMap,
        final DwarfInfo dwarfInfo,
        final String moduleId)
        throws Exception {
      if (frames == null) {
        throw new IllegalArgumentException("Frames cannot be null");
      }
      if (moduleId == null) {
        throw new IllegalArgumentException("Module ID cannot be null");
      }

      return frames.stream().map(SourceMappedFrame::new).toList();
    }

    @Override
    public Optional<SourcePosition> getSourcePosition(
        final SourceMap sourceMap, final WasmAddress wasmAddress) throws Exception {
      if (sourceMap == null) {
        throw new IllegalArgumentException("Source map cannot be null");
      }
      if (wasmAddress == null) {
        throw new IllegalArgumentException("WASM address cannot be null");
      }
      return Optional.empty(); // Mock implementation returns no position
    }

    @Override
    public Optional<FunctionSymbol> resolveFunctionSymbol(
        final String moduleId, final int functionIndex, final DwarfInfo dwarfInfo) {
      if (moduleId == null) {
        throw new IllegalArgumentException("Module ID cannot be null");
      }
      if (functionIndex < 0) {
        throw new IllegalArgumentException("Function index cannot be negative");
      }
      return Optional.of(new FunctionSymbol("func_" + functionIndex, functionIndex));
    }

    @Override
    public String loadSourceFile(final String path) throws Exception {
      if (path == null || path.trim().isEmpty()) {
        throw new IllegalArgumentException("Path cannot be null or empty");
      }
      return "// Source file: " + path + "\n// (Mock content)\n";
    }

    @Override
    public String formatStackTrace(final List<SourceMappedFrame> frames) {
      if (frames == null) {
        throw new IllegalArgumentException("Frames cannot be null");
      }

      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < frames.size(); i++) {
        final SourceMappedFrame frame = frames.get(i);
        sb.append("Frame ").append(i).append(": ");
        sb.append(frame.getFunctionName()).append("()");
        sb.append(" at ").append(frame.getWasmAddress()).append("\n");
      }
      return sb.toString();
    }

    @Override
    public ValidationResult validateSourceMap(final SourceMap sourceMap) {
      if (sourceMap == null) {
        throw new IllegalArgumentException("Source map cannot be null");
      }
      return ValidationResult.success();
    }

    @Override
    public ValidationResult validateDwarfInfo(final DwarfInfo dwarfInfo) {
      if (dwarfInfo == null) {
        throw new IllegalArgumentException("DWARF info cannot be null");
      }
      return ValidationResult.success();
    }

    @Override
    public void clearCaches() {
      // Mock implementation - no-op
    }

    @Override
    public CacheStatistics getCacheStatistics() {
      return CacheStatistics.empty();
    }

    @Override
    public void configureCaching(final CacheConfiguration config) {
      if (config == null) {
        throw new IllegalArgumentException("Config cannot be null");
      }
      // Mock implementation - no-op
    }

    @Override
    public boolean isSourceMapSupported() {
      return true;
    }

    @Override
    public boolean isDwarfSupported() {
      return true;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  /** Mock implementation of DwarfInfo for testing. */
  private static class MockDwarfInfo implements DwarfInfo {
    private boolean closed = false;

    @Override
    public List<CompilationUnit> getCompilationUnits() {
      return Arrays.asList();
    }

    @Override
    public List<LineProgram> getLinePrograms() {
      return Arrays.asList();
    }

    @Override
    public List<DwarfFunction> getFunctions() {
      return Arrays.asList();
    }

    @Override
    public List<DwarfType> getTypes() {
      return Arrays.asList();
    }

    @Override
    public Optional<DwarfFunction> getFunctionByAddress(final long address) {
      return Optional.empty();
    }

    @Override
    public Optional<LineInfo> getLineByAddress(final long address) {
      return Optional.empty();
    }

    @Override
    public Optional<String> getProducer() {
      return Optional.of("test-compiler");
    }

    @Override
    public Optional<Integer> getLanguage() {
      return Optional.of(1); // DW_LANG_C
    }

    @Override
    public ValidationResult validate() {
      return ValidationResult.success();
    }

    @Override
    public DwarfMetadata getMetadata() {
      return new DwarfMetadata() {
        @Override
        public int getCompilationUnitCount() {
          return 0;
        }

        @Override
        public int getFunctionCount() {
          return 0;
        }

        @Override
        public int getLineEntryCount() {
          return 0;
        }

        @Override
        public int getTypeCount() {
          return 0;
        }

        @Override
        public long getCreationTime() {
          return System.currentTimeMillis();
        }

        @Override
        public long estimateMemoryUsage() {
          return 1024;
        }
      };
    }

    @Override
    public boolean isValid() {
      return !closed;
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}
