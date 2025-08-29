package ai.tegmentum.wasmtime4j.webassembly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Manages WebAssembly test data including loading, caching, and validation.
 * Provides thread-safe access to test modules with intelligent caching.
 */
public final class WasmTestDataManager {
    private static final Logger LOGGER = Logger.getLogger(WasmTestDataManager.class.getName());
    
    // Cache configuration
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final int MAX_CACHE_SIZE = 1000;
    
    // Singleton instance
    private static volatile WasmTestDataManager instance;
    private static final Object LOCK = new Object();
    
    // Cache storage
    private final Map<String, CachedTestCase> testCaseCache = new ConcurrentHashMap<>();
    private final Map<WasmTestSuiteLoader.TestSuiteType, List<WasmTestCase>> suiteCache = new ConcurrentHashMap<>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // Statistics
    private volatile int cacheHits = 0;
    private volatile int cacheMisses = 0;
    private volatile Instant lastCleanup = Instant.now();
    
    private WasmTestDataManager() {
        // Private constructor for singleton
    }
    
    /**
     * Gets the singleton instance of the test data manager.
     *
     * @return the test data manager instance
     */
    public static WasmTestDataManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new WasmTestDataManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Loads a test case by name with caching support.
     *
     * @param testName the test case name
     * @return the test case if found
     * @throws IOException if loading fails
     */
    public WasmTestCase loadTestCase(final String testName) throws IOException {
        // Check cache first
        final CachedTestCase cached = testCaseCache.get(testName);
        if (cached != null && !cached.isExpired()) {
            cacheHits++;
            LOGGER.fine("Cache hit for test case: " + testName);
            return cached.testCase;
        }
        
        // Load from disk
        cacheMisses++;
        LOGGER.fine("Cache miss for test case: " + testName);
        
        final WasmTestCase testCase = WasmTestSuiteLoader.loadTestCase(testName)
            .orElseThrow(() -> new IOException("Test case not found: " + testName));
        
        // Cache the result
        cacheLock.writeLock().lock();
        try {
            testCaseCache.put(testName, new CachedTestCase(testCase, Instant.now()));
            cleanupCacheIfNeeded();
        } finally {
            cacheLock.writeLock().unlock();
        }
        
        return testCase;
    }
    
    /**
     * Loads all test cases for a specific suite type with caching.
     *
     * @param suiteType the test suite type
     * @return list of test cases
     * @throws IOException if loading fails
     */
    public List<WasmTestCase> loadTestSuite(final WasmTestSuiteLoader.TestSuiteType suiteType) throws IOException {
        // Check cache first
        final List<WasmTestCase> cached = suiteCache.get(suiteType);
        if (cached != null) {
            cacheHits++;
            LOGGER.fine("Cache hit for test suite: " + suiteType);
            return cached;
        }
        
        // Load from disk
        cacheMisses++;
        LOGGER.fine("Cache miss for test suite: " + suiteType);
        
        final List<WasmTestCase> testCases = WasmTestSuiteLoader.loadTestSuite(suiteType);
        
        // Cache the result
        suiteCache.put(suiteType, testCases);
        
        return testCases;
    }
    
    /**
     * Validates a WebAssembly module and provides detailed information.
     *
     * @param moduleBytes the WebAssembly module bytes
     * @return validation result with details
     */
    public WasmModuleValidationResult validateModule(final byte[] moduleBytes) {
        final WasmModuleValidationResult.Builder resultBuilder = new WasmModuleValidationResult.Builder();
        
        // Basic validation
        if (moduleBytes == null || moduleBytes.length == 0) {
            return resultBuilder
                .valid(false)
                .addError("Module is empty or null")
                .build();
        }
        
        // Check minimum size
        if (moduleBytes.length < 8) {
            return resultBuilder
                .valid(false)
                .addError("Module too small (minimum 8 bytes required)")
                .build();
        }
        
        // Check magic number
        if (moduleBytes[0] != 0x00 || moduleBytes[1] != 0x61 || 
            moduleBytes[2] != 0x73 || moduleBytes[3] != 0x6d) {
            return resultBuilder
                .valid(false)
                .addError("Invalid WebAssembly magic number")
                .build();
        }
        
        // Check version
        if (moduleBytes[4] != 0x01 || moduleBytes[5] != 0x00 || 
            moduleBytes[6] != 0x00 || moduleBytes[7] != 0x00) {
            return resultBuilder
                .valid(false)
                .addError("Unsupported WebAssembly version")
                .build();
        }
        
        // Basic structure analysis
        resultBuilder.valid(true)
                    .moduleSize(moduleBytes.length)
                    .addInfo("Valid WebAssembly magic number and version");
        
        // Analyze sections
        analyzeSections(moduleBytes, resultBuilder);
        
        return resultBuilder.build();
    }
    
    /**
     * Creates a backup of test data for safety.
     *
     * @param backupDirectory the directory where to create the backup
     * @throws IOException if backup creation fails
     */
    public void createTestDataBackup(final Path backupDirectory) throws IOException {
        Files.createDirectories(backupDirectory);
        
        final Path wasmTestsRoot = WasmTestSuiteLoader.getTestSuiteDirectory(
            WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS).getParent();
        
        // Copy all test directories
        for (final WasmTestSuiteLoader.TestSuiteType suiteType : WasmTestSuiteLoader.TestSuiteType.values()) {
            final Path sourceDir = WasmTestSuiteLoader.getTestSuiteDirectory(suiteType);
            final Path targetDir = backupDirectory.resolve(suiteType.getDirectoryName());
            
            if (Files.exists(sourceDir)) {
                copyDirectory(sourceDir, targetDir);
                LOGGER.info("Backed up " + suiteType.name() + " to " + targetDir);
            }
        }
        
        LOGGER.info("Test data backup completed to: " + backupDirectory);
    }
    
    /**
     * Initializes test data by creating standard test modules and ensuring directories exist.
     *
     * @throws IOException if initialization fails
     */
    public void initializeTestData() throws IOException {
        // Ensure test suite directories exist
        WasmTestSuiteLoader.ensureTestSuitesAvailable();
        
        // Create standard test modules
        final Path customTestsDir = WasmTestSuiteLoader.getTestSuiteDirectory(
            WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS);
        
        WatToWasmConverter.createStandardTestModules(customTestsDir);
        
        // Generate WASM files from WAT files if wabt is available
        if (WatToWasmConverter.isWabtAvailable()) {
            generateWasmFilesFromWat(customTestsDir);
        } else {
            LOGGER.warning("wabt is not available. WAT files will not be converted to WASM automatically.");
            createHardcodedWasmFiles(customTestsDir);
        }
        
        LOGGER.info("Test data initialization completed");
    }
    
    /**
     * Gets cache statistics.
     *
     * @return cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        cacheLock.readLock().lock();
        try {
            final int totalRequests = cacheHits + cacheMisses;
            final double hitRate = totalRequests > 0 ? (double) cacheHits / totalRequests : 0.0;
            
            return new CacheStatistics(
                cacheHits,
                cacheMisses,
                hitRate,
                testCaseCache.size(),
                suiteCache.size(),
                lastCleanup
            );
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Clears all caches.
     */
    public void clearCache() {
        cacheLock.writeLock().lock();
        try {
            testCaseCache.clear();
            suiteCache.clear();
            cacheHits = 0;
            cacheMisses = 0;
            LOGGER.info("Cache cleared");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Analyzes WebAssembly module sections.
     *
     * @param moduleBytes the module bytes
     * @param resultBuilder the result builder to populate
     */
    private void analyzeSections(final byte[] moduleBytes, final WasmModuleValidationResult.Builder resultBuilder) {
        // This is a simplified analysis - a full implementation would parse all sections
        int pos = 8; // Skip magic and version
        int sectionCount = 0;
        
        while (pos < moduleBytes.length) {
            if (pos + 1 >= moduleBytes.length) {
                break;
            }
            
            final int sectionType = moduleBytes[pos] & 0xFF;
            pos++;
            
            // Read section size (simplified - assumes single-byte size)
            if (pos >= moduleBytes.length) {
                break;
            }
            
            final int sectionSize = moduleBytes[pos] & 0xFF;
            pos += 1 + sectionSize;
            
            sectionCount++;
            
            // Add section information
            final String sectionName = getSectionName(sectionType);
            resultBuilder.addInfo("Found " + sectionName + " section (size: " + sectionSize + " bytes)");
            
            if (pos >= moduleBytes.length) {
                break;
            }
        }
        
        resultBuilder.addInfo("Total sections: " + sectionCount);
    }
    
    /**
     * Gets the name of a WebAssembly section by type ID.
     *
     * @param sectionType the section type ID
     * @return the section name
     */
    private String getSectionName(final int sectionType) {
        return switch (sectionType) {
            case 1 -> "Type";
            case 2 -> "Import";
            case 3 -> "Function";
            case 4 -> "Table";
            case 5 -> "Memory";
            case 6 -> "Global";
            case 7 -> "Export";
            case 8 -> "Start";
            case 9 -> "Element";
            case 10 -> "Code";
            case 11 -> "Data";
            default -> "Custom/Unknown";
        };
    }
    
    /**
     * Cleans up expired cache entries if needed.
     */
    private void cleanupCacheIfNeeded() {
        final Instant now = Instant.now();
        if (Duration.between(lastCleanup, now).toMinutes() < 5 && testCaseCache.size() < MAX_CACHE_SIZE) {
            return; // No cleanup needed
        }
        
        int removedCount = 0;
        final var iterator = testCaseCache.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }
        
        lastCleanup = now;
        if (removedCount > 0) {
            LOGGER.fine("Cleaned up " + removedCount + " expired cache entries");
        }
    }
    
    /**
     * Generates WASM files from WAT files in a directory.
     *
     * @param directory the directory containing WAT files
     * @throws IOException if generation fails
     */
    private void generateWasmFilesFromWat(final Path directory) throws IOException {
        try (final var paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".wat"))
                 .forEach(watFile -> {
                     try {
                         final Path wasmFile = watFile.resolveSibling(
                             watFile.getFileName().toString().replaceFirst("\\.wat$", ".wasm"));
                         WatToWasmConverter.convertWatToWasm(watFile, wasmFile);
                     } catch (final IOException e) {
                         LOGGER.warning("Failed to convert " + watFile + " to WASM: " + e.getMessage());
                     }
                 });
        }
    }
    
    /**
     * Creates hardcoded WASM files when wabt is not available.
     *
     * @param directory the directory where to create WASM files
     * @throws IOException if file creation fails
     */
    private void createHardcodedWasmFiles(final Path directory) throws IOException {
        // Create simple add module
        final byte[] addModule = {
            0x00, 0x61, 0x73, 0x6d, // magic
            0x01, 0x00, 0x00, 0x00, // version
            0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // type section
            0x03, 0x02, 0x01, 0x00, // function section
            0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export section
            0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b // code section
        };
        Files.write(directory.resolve("add.wasm"), addModule);
        
        LOGGER.info("Created hardcoded WASM files in " + directory);
    }
    
    /**
     * Copies a directory recursively.
     *
     * @param source the source directory
     * @param target the target directory
     * @throws IOException if copying fails
     */
    private void copyDirectory(final Path source, final Path target) throws IOException {
        Files.createDirectories(target);
        try (final var paths = Files.walk(source)) {
            paths.forEach(sourcePath -> {
                try {
                    final Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (final IOException e) {
                    LOGGER.warning("Failed to copy " + sourcePath + ": " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * Cached test case with expiration.
     */
    private static final class CachedTestCase {
        final WasmTestCase testCase;
        final Instant cachedAt;
        
        CachedTestCase(final WasmTestCase testCase, final Instant cachedAt) {
            this.testCase = testCase;
            this.cachedAt = cachedAt;
        }
        
        boolean isExpired() {
            return Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) > 0;
        }
    }
    
    /**
     * Cache statistics.
     */
    public static final class CacheStatistics {
        private final int hits;
        private final int misses;
        private final double hitRate;
        private final int testCaseCacheSize;
        private final int suiteCacheSize;
        private final Instant lastCleanup;
        
        CacheStatistics(final int hits, final int misses, final double hitRate,
                       final int testCaseCacheSize, final int suiteCacheSize,
                       final Instant lastCleanup) {
            this.hits = hits;
            this.misses = misses;
            this.hitRate = hitRate;
            this.testCaseCacheSize = testCaseCacheSize;
            this.suiteCacheSize = suiteCacheSize;
            this.lastCleanup = lastCleanup;
        }
        
        public int getHits() { return hits; }
        public int getMisses() { return misses; }
        public double getHitRate() { return hitRate; }
        public int getTestCaseCacheSize() { return testCaseCacheSize; }
        public int getSuiteCacheSize() { return suiteCacheSize; }
        public Instant getLastCleanup() { return lastCleanup; }
        
        @Override
        public String toString() {
            return String.format("CacheStatistics{hits=%d, misses=%d, hitRate=%.2f%%, " +
                               "testCacheSize=%d, suiteCacheSize=%d, lastCleanup=%s}",
                               hits, misses, hitRate * 100, testCaseCacheSize, suiteCacheSize, lastCleanup);
        }
    }
}