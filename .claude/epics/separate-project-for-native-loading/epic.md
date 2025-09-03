---
name: separate-project-for-native-loading
status: backlog
created: 2025-09-01T22:56:11Z
progress: 0%
prd: .claude/prds/separate-project-for-native-loading.md
github: https://github.com//issues/125
---

# Epic: Separate Project for Native Loading

## Overview

Extract wasmtime4j's mature native loading infrastructure into a standalone Maven library called `native-loader`. The implementation leverages the existing, battle-tested code (PlatformDetector + NativeLibraryUtils) with minimal changes - only parameterizing 3 hardcoded strings and adding a builder pattern API. This approach maximizes code reuse while minimizing risk.

## Architecture Decisions

### Core Design Philosophy
- **Minimal Extraction**: Reuse 95% of existing wasmtime4j code with only essential parameterization
- **Hybrid API**: Static utilities for simplicity + Builder pattern for advanced configuration
- **Zero Dependencies**: Maintain java.util.logging and JDK-only dependencies
- **Backward Compatibility**: Java 8+ support for maximum ecosystem adoption

### Technology Stack
- **Build System**: Maven 3.x with standard lifecycle
- **Java Version**: Java 8+ (source/target compatibility)
- **Testing**: JUnit 5 + JMH for performance benchmarks
- **Distribution**: Maven Central with automated release pipeline
- **Documentation**: Javadoc + README with code examples

### Key Technical Decisions
- **Package Structure**: `io.github.native4j` (assumes community org)
- **API Design**: `NativeLoader.loadLibrary()` (simple) + `NativeLoader.builder()` (advanced)
- **Security Model**: Configurable validation levels (strict/moderate/permissive)
- **Resource Conventions**: Built-in support for Maven Native, JNA, and custom patterns
- **Performance**: Maintain < 5% overhead vs direct System.loadLibrary()

## Technical Approach

### Core Components

#### 1. Platform Detection (Zero Changes)
- **Source**: Copy `PlatformDetector.java` from wasmtime4j as-is
- **Functionality**: OS/architecture detection, resource path generation
- **Testing**: Existing unit tests cover all supported platforms

#### 2. Native Loading Engine (Minimal Changes)
- **Source**: Parameterize `NativeLibraryUtils.java` (3 hardcoded strings)
- **Changes Required**:
  ```java
  // Before: private static final String LIBRARY_NAME = "wasmtime4j";
  // After:  Constructor parameter: String libraryName
  
  // Before: private static final String TEMP_FILE_PREFIX = "wasmtime4j-native-";
  // After:  Configuration parameter with default
  
  // Before: private static final String TEMP_DIR_SUFFIX = "-wasmtime4j";
  // After:  Configuration parameter with default
  ```
- **Functionality**: All existing security, caching, cleanup logic preserved

#### 3. Builder Pattern API (New)
```java
public class NativeLoaderBuilder {
    private String libraryName;
    private String tempPrefix = "native-";
    private SecurityLevel security = SecurityLevel.MODERATE;
    private ResourcePathConvention convention = ResourcePathConvention.MAVEN_NATIVE;
    
    public NativeLoaderBuilder libraryName(String name) { ... }
    public NativeLoaderBuilder tempPrefix(String prefix) { ... }
    public NativeLoaderBuilder security(SecurityLevel level) { ... }
    public NativeLoaderBuilder resourceConvention(ResourcePathConvention conv) { ... }
    public LibraryLoadInfo load() { ... }
}
```

#### 4. Static Convenience API (Minimal Wrapper)
```java
public class NativeLoader {
    public static LibraryLoadInfo loadLibrary(String libraryName) {
        return builder().libraryName(libraryName).load();
    }
    
    public static NativeLoaderBuilder builder() {
        return new NativeLoaderBuilder();
    }
}
```

### Configuration System

#### Resource Path Conventions
- **Maven Native**: `/natives/{platform}/{lib}{name}{ext}`
- **JNA Style**: `/natives/{platform}/{name}{ext}`
- **Custom**: User-defined pattern with placeholders

#### Security Levels
- **STRICT**: Full path validation, no custom temp dirs
- **MODERATE**: Standard validation, configurable temp locations  
- **PERMISSIVE**: Minimal validation for maximum compatibility

### Integration Strategy

#### wasmtime4j Migration
1. Add dependency: `io.github.native4j:native-loader:1.0.0`
2. Replace existing NativeLibraryLoader implementations:
   ```java
   // Before: NativeLibraryUtils.loadNativeLibrary("wasmtime4j", ...)
   // After:  NativeLoader.loadLibrary("wasmtime4j")
   ```
3. Remove duplicate PlatformDetector and NativeLibraryUtils classes
4. Validate zero regression through existing test suite

## Implementation Strategy

### Development Phases

#### Phase 1: Core Extraction (Week 1)
- Create Maven project structure with proper POM configuration
- Copy and parameterize core classes (PlatformDetector + NativeLibraryUtils)
- Implement basic static API (`NativeLoader.loadLibrary()`)
- Set up comprehensive test suite with all platform combinations

#### Phase 2: Enhanced Features (Week 2)
- Implement builder pattern API with configuration options
- Add support for multiple resource path conventions
- Implement configurable security levels
- Create performance benchmark suite (JMH)

#### Phase 3: Production Readiness (Week 3)
- Maven Central publishing setup (POM, GPG signing, staging)
- Comprehensive documentation (README, Javadoc, usage examples)
- CI/CD pipeline with multi-platform testing
- Community contribution guidelines

#### Phase 4: wasmtime4j Migration (Week 4)
- Publish 1.0.0 release to Maven Central
- Update wasmtime4j POM to include new dependency
- Replace internal implementations with library calls
- Remove duplicate code and validate zero regression

### Risk Mitigation

#### High Priority: Functional Regression
- **Strategy**: Extensive integration testing between library and wasmtime4j
- **Validation**: All existing wasmtime4j tests must pass without changes
- **Fallback**: Keep original code until migration fully validated

#### Medium Priority: Performance Impact  
- **Strategy**: Continuous JMH benchmarking throughout development
- **Threshold**: Reject any changes causing >5% performance degradation
- **Monitoring**: Compare library performance to direct System.loadLibrary()

#### Low Priority: Maven Central Delays
- **Strategy**: Start publication process early in Phase 3
- **Fallback**: Use GitHub Packages as temporary distribution method
- **Timeline**: Allow 1-2 weeks buffer for Maven Central approval process

## Tasks Created

- [ ] #130 - Project Setup and Maven Configuration (parallel: false)
- [ ] #131 - Extract Core Platform Detection (parallel: false)
- [ ] #132 - Parameterize NativeLibraryUtils (parallel: true)
- [ ] #137 - Implement Builder Pattern API (parallel: true)
- [ ] #138 - Add Multi-Convention Resource Path Support (parallel: true)
- [ ] #139 - Comprehensive Test Suite (parallel: true)
- [ ] #133 - Performance Benchmarks with JMH (parallel: true)
- [ ] #134 - Documentation and Maven Central Publishing (parallel: true)
- [ ] #135 - wasmtime4j Integration and Migration (parallel: false)
- [ ] #136 - Integration Validation and Release (parallel: false)

Total tasks: 10
Parallel tasks: 6
Sequential tasks: 4
Estimated total effort: 28 hours (3.5 days)
## Dependencies

### External Dependencies
- **Maven Central Account**: For publishing (assume existing or can be created)
- **GitHub Actions**: For CI/CD pipeline (existing infrastructure)
- **GPG Key**: For artifact signing (can be generated)

### Internal Dependencies
- **wasmtime4j Code Access**: Full access to existing NativeLibraryUtils and PlatformDetector
- **Test Infrastructure**: Reuse existing wasmtime4j test patterns and WebAssembly modules
- **Documentation Standards**: Follow existing wasmtime4j documentation patterns

### Coordination Requirements
- **No Breaking Changes**: wasmtime4j public API must remain unchanged during migration
- **Synchronized Release**: wasmtime4j update should follow library publication quickly
- **Test Coordination**: Both projects tested together during transition period

## Success Criteria (Technical)

### Performance Benchmarks
- **Loading Latency**: ≤ 5% overhead compared to direct System.loadLibrary()
- **Memory Footprint**: ≤ 1MB additional heap usage for typical scenarios
- **Cache Efficiency**: > 95% cache hit rate for repeated library loading
- **Resource Cleanup**: Zero memory leaks in 24-hour continuous operation

### Quality Gates
- **Test Coverage**: > 95% line coverage across all modules
- **Platform Coverage**: All 6 supported platforms (Linux/Windows/macOS × x86_64/ARM64) tested
- **Error Handling**: 100% of error conditions have defined recovery behavior
- **API Documentation**: Complete Javadoc for all public classes and methods

### wasmtime4j Migration Success
- **Zero Regression**: All existing wasmtime4j tests pass with zero changes
- **Code Reduction**: Remove > 500 lines of duplicate platform detection code
- **Build Impact**: No increase in wasmtime4j build time or artifact size
- **Runtime Equivalence**: Identical behavior for all existing wasmtime4j use cases

## Estimated Effort

### Overall Timeline: 4 weeks
- **Week 1**: Core extraction and basic functionality (40% complete)
- **Week 2**: Advanced features and configuration (70% complete)  
- **Week 3**: Production readiness and publishing (90% complete)
- **Week 4**: wasmtime4j migration and validation (100% complete)

### Resource Requirements
- **Primary Developer**: 1 full-time equivalent for 4 weeks
- **Code Review**: Minimal - mostly extracting proven code
- **Testing Resources**: GitHub Actions for multi-platform CI (existing)
- **Documentation**: ~2-3 days for comprehensive documentation

### Critical Path Items
1. **Maven Central Setup**: Longest lead time - start early in Phase 3
2. **Multi-platform Testing**: Ensure all 6 platform combinations work correctly
3. **wasmtime4j Integration**: Must validate zero behavioral changes
4. **Performance Validation**: Cannot ship with >5% performance regression

### Low Risk Profile
This epic has exceptionally low technical risk because:
- 95% of code already exists and is battle-tested in wasmtime4j
- Only 3 hardcoded strings need parameterization
- All complex logic (security, caching, cleanup) remains unchanged
- Extensive test coverage already exists for core functionality
