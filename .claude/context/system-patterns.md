---
created: 2025-08-27T00:32:32Z
last_updated: 2025-08-27T00:32:32Z
version: 1.0
author: Claude Code PM System
---

# System Patterns

## Architectural Patterns

### Multi-Runtime Architecture
**Pattern**: Adapter pattern with factory selection
**Implementation**: 
- Common interface definitions in public module
- Runtime-specific implementations in private modules
- Factory-based loading with automatic detection
- Graceful fallback mechanisms

**Benefits**:
- Clean separation between public API and implementations
- Version-specific optimization (JNI vs Panama)
- Runtime selection flexibility

### Shared Native Library Pattern
**Pattern**: Single native library with multiple language bindings
**Implementation**:
- Consolidated Rust library in `wasmtime4j-native`
- Both JNI and Panama FFI exports
- Eliminates code duplication
- Ensures consistency across implementations

**Benefits**:
- Single source of truth for native functionality
- Reduced maintenance overhead
- Consistent behavior across runtimes

## Design Patterns in Use

### Factory Pattern
**Usage**: Runtime selection and instantiation
```java
// Automatic detection
WasmRuntime runtime = WasmRuntimeFactory.create();

// Manual override
System.setProperty("wasmtime4j.runtime", "jni");
WasmRuntime runtime = WasmRuntimeFactory.create();
```

### Adapter Pattern  
**Usage**: Unified interface over different native implementations
- JNI adapter implements common interface
- Panama adapter implements same interface
- Client code remains unchanged

### Bridge Pattern
**Usage**: Separating abstraction from implementation
- Public API defines abstractions
- Private modules provide concrete implementations
- Allows independent evolution of both sides

## Error Handling Patterns

### Two-Tier Error Strategy
**Pattern**: Comprehensive internal mapping with simplified external API

**Internal Tier**:
- Comprehensive native error mapping
- Detailed error codes and context
- Full stack traces and diagnostics

**External Tier**:
- Broader exception categories
- `CompilationException`
- `RuntimeException` 
- `ValidationException`

**Benefits**:
- Internal debugging capabilities
- Clean public API
- Consistent error handling across implementations

### Defensive Programming Pattern
**Pattern**: Fail-safe with extensive validation
**Implementation**:
- Validate all native calls before execution
- Null checks and boundary validation
- Defensive copying for mutable parameters
- Never assume native code correctness

## Code Organization Patterns

### Package Structure Pattern
**Pattern**: Hierarchical package organization
```
ai.tegmentum.wasmtime4j          # Public API
├── .jni                         # JNI implementation (private)
├── .panama                      # Panama implementation (private)
└── .native                      # Native library interfaces
```

### Module Dependency Pattern
**Pattern**: Unidirectional dependency flow
```
Public API → Factory → Implementation Modules → Native Library
```
- No reverse dependencies
- Clean separation of concerns
- Testable in isolation

## Testing Patterns

### Multi-Implementation Testing
**Pattern**: Same test suite across all implementations
- Tests written against public API
- Run against both JNI and Panama
- Implementation-agnostic validation
- Behavior consistency verification

### Test Category Pattern
**Organization**:
- Unit tests: Individual component testing
- Integration tests: Cross-module compatibility
- WebAssembly tests: Official test suite compliance
- Performance tests: JMH benchmarks

## Build Patterns

### Maven Multi-Module Pattern
**Structure**:
- Parent POM with common configuration
- Module-specific POMs with unique concerns
- Dependency management at parent level
- Profile-based platform targeting

### Cross-Compilation Pattern
**Implementation**:
- Platform-specific build profiles
- Native library compilation during Maven build
- Artifact packaging with platform libraries
- Runtime library extraction and loading

## Resource Management Patterns

### Native Resource Lifecycle
**Pattern**: Explicit resource management with cleanup
- Native resource wrappers
- Automatic cleanup on garbage collection
- Explicit disposal methods
- Resource leak prevention

### Memory Management Pattern
**Strategy**: Minimize GC pressure while maintaining safety
- Object pooling for frequently used objects
- Direct memory allocation where appropriate
- Careful lifetime management of native references

## Logging Patterns

### Structured Logging Pattern
**Implementation**: Consistent logging format across modules
- Standard log levels (SEVERE, WARNING, INFO, FINE)
- Contextual information in log messages
- Module-specific loggers
- Performance-aware logging (lazy evaluation)

## Integration Patterns

### GitHub PM Integration
**Pattern**: Local-first with explicit synchronization
- Local file-based workflow
- Explicit sync to GitHub issues
- Bidirectional state management
- Conflict resolution strategies

### Claude Code PM Workflow
**Pattern**: Spec-driven development with AI assistance
- PRD → Epic → Task decomposition
- Context preservation across sessions
- Parallel execution with specialized agents
- Audit trail maintenance