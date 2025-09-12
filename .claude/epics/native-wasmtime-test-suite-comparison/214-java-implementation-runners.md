# Task 004: Java Implementation Runners

## Task Overview
Implement dedicated runners for wasmtime4j-jni and wasmtime4j-panama implementations that execute identical test scenarios as the native Wasmtime runner, enabling systematic behavioral comparison and compatibility validation across all runtime targets.

## Work Streams Analysis

### Stream A: JNI Implementation Runner (20 hours)
**Scope**: wasmtime4j-jni execution wrapper and integration
**Files**: `JniImplementationRunner.java`, `JniResultMapper.java`, `JniErrorHandler.java`
**Work**:
- Implement JniImplementationRunner using wasmtime4j-jni APIs
- Create result mapping from JNI-specific objects to TestExecutionResult format
- Build error handling and exception mapping for JNI-specific failures
- Implement proper resource management and cleanup for JNI resources
- Add JNI-specific performance metrics collection (native call overhead, memory usage)

**Dependencies**:
- ✅ Task 002 (Core comparison engine interfaces)
- ✅ Functional wasmtime4j-jni implementation
- ⏸ Requires unified wasmtime4j API stability

### Stream B: Panama Implementation Runner (20 hours)
**Scope**: wasmtime4j-panama execution wrapper and integration
**Files**: `PanamaImplementationRunner.java`, `PanamaResultMapper.java`, `PanamaErrorHandler.java`
**Work**:
- Implement PanamaImplementationRunner using wasmtime4j-panama APIs
- Create result mapping from Panama-specific objects to TestExecutionResult format
- Build error handling for Panama FFI exceptions and native call failures
- Implement proper memory segment cleanup and resource management
- Add Panama-specific performance metrics collection (FFI overhead, memory mapping)

**Dependencies**:
- ✅ Task 002 (Core comparison engine interfaces)
- ✅ Functional wasmtime4j-panama implementation
- ⏸ Requires Java 23+ runtime environment

### Stream C: Unified Runner Framework (16 hours)
**Scope**: Common abstractions and shared functionality
**Files**: `AbstractTestRunner.java`, `RunnerFactory.java`, `RuntimeDetector.java`
**Work**:
- Design AbstractTestRunner base class with common functionality
- Implement RunnerFactory for automatic runtime selection and instantiation
- Create RuntimeDetector for Java version and capability detection
- Build shared error mapping and exception translation logic
- Implement common performance metrics collection framework

**Dependencies**:
- ✅ Task 002 (Core comparison engine interfaces)
- ⏸ Depends on Streams A and B for concrete implementation patterns

## Implementation Approach

### Runner Architecture Design
- Implement AbstractTestRunner interface from core comparison engine
- Use Strategy pattern for runtime-specific execution logic
- Apply Template Method pattern for common execution workflow
- Implement Builder pattern for flexible test configuration

### Resource Management Strategy
- Proper cleanup of native resources (JNI handles, Panama memory segments)
- Implement try-with-resources pattern for automatic resource management
- Use weak references for large objects to enable garbage collection
- Add resource leak detection for debugging and testing

### Error Translation Framework
```java
// Common error mapping strategy
public abstract class AbstractTestRunner implements TestRunner {
    protected TestExecutionResult executeTest(TestCase testCase) {
        try {
            Object result = executeImplementationSpecific(testCase);
            return mapToTestResult(result);
        } catch (Exception e) {
            return mapExceptionToResult(e, testCase);
        }
    }
    
    protected abstract Object executeImplementationSpecific(TestCase testCase);
    protected abstract TestExecutionResult mapToTestResult(Object result);
}
```

### Performance Metrics Collection
- Execution time measurement with nanosecond precision
- Memory usage tracking before/after test execution
- Native call count and overhead measurement
- Resource allocation and cleanup timing

## Acceptance Criteria

### Functional Requirements
- [ ] JniImplementationRunner successfully executes all test scenarios using wasmtime4j-jni
- [ ] PanamaImplementationRunner successfully executes all test scenarios using wasmtime4j-panama
- [ ] Both runners produce TestExecutionResult objects identical in structure to native runner
- [ ] Runtime selection automatically chooses appropriate implementation based on Java version
- [ ] All error conditions are properly mapped to comparable exception types

### Performance Requirements
- [ ] JNI runner execution overhead under 10ms per test compared to direct JNI usage
- [ ] Panama runner execution overhead under 5ms per test compared to direct Panama usage
- [ ] Memory usage remains under 200MB per runner during concurrent execution
- [ ] Resource cleanup completes within 100ms after test execution

### Quality Requirements
- [ ] No resource leaks (memory, file handles, native resources) under any execution scenario
- [ ] Consistent behavior across multiple test runs with identical inputs
- [ ] Proper exception handling and error reporting for all failure modes
- [ ] Thread safety for concurrent test execution within same runner instance

### Integration Requirements
- [ ] Both runners implement AbstractTestRunner interface completely
- [ ] Results are compatible with ResultCollector aggregation
- [ ] Integration with ComparisonOrchestrator for parallel execution
- [ ] Support for all configuration options from ComparisonConfiguration

## Dependencies
- **Prerequisite**: Task 002 (Core comparison engine) completion
- **Prerequisite**: Functional wasmtime4j-jni implementation
- **Prerequisite**: Functional wasmtime4j-panama implementation
- **Parallel**: Task 003 (Native Wasmtime Runner) - independent development
- **Enables**: Task 005 (Result Analysis Framework) - provides comparison targets

## Readiness Status
- **Status**: READY (after Task 002 completion)
- **Blocking**: Task 002 must complete first
- **Launch Condition**: Core comparison engine interfaces available and wasmtime4j implementations functional

## Effort Estimation
- **Total Duration**: 56 hours (7 days)
- **Work Stream A**: 20 hours (JNI implementation runner)
- **Work Stream B**: 20 hours (Panama implementation runner)
- **Work Stream C**: 16 hours (Unified runner framework)
- **Parallel Work**: Streams A and B can run in parallel, Stream C integrates both
- **Risk Buffer**: 25% (14 additional hours for implementation integration complexity)

## Agent Requirements
- **Agent Type**: general-purpose with Java expertise
- **Key Skills**: Java 23+, JNI, Panama Foreign Function API, design patterns, resource management
- **Specialized Knowledge**: wasmtime4j API usage, WebAssembly runtime behavior
- **Platform Requirements**: Java 23+ environment, access to both JNI and Panama implementations
- **Tools**: Java 23+, JUnit 5, profiling tools, memory analysis tools

## Risk Mitigation
- **Implementation Dependencies**: Verify wasmtime4j implementations are stable before starting
- **Resource Management**: Implement comprehensive leak detection and testing
- **Performance Impact**: Profile and optimize runner overhead to minimize measurement skew
- **Error Mapping**: Create comprehensive test suite for error condition handling