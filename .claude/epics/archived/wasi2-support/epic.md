---
name: wasi2-support
status: completed
created: 2025-08-31T13:49:28Z
completed: 2025-09-01T14:15:00Z
progress: 96%
prd: .claude/prds/wasi2-support.md
github: https://github.com/tegmentum/wasmtime4j/issues/93
---

# Epic: wasi2-support

## Overview

Implement WASI Preview 2 (Component Model) support by leveraging the existing robust WASI1 foundation and adding a unified public API layer. This approach maximizes code reuse from the comprehensive JNI/Panama implementations while adding component model capabilities on top. The implementation will focus on extending the current wasmtime4j-native library with WASI2 bindings and creating a clean public API abstraction that works across both runtimes.

## Architecture Decisions

### Leverage Existing WASI1 Foundation
- **Reuse Security Framework**: Extend existing WasiPermissionManager and WasiSecurityValidator for component model
- **Extend File System**: Build streaming I/O on top of robust WasiFileSystem implementations
- **Preserve Backend Architecture**: Keep JNI/Panama separation while adding unified API layer

### Component Model Integration
- **Native-First Approach**: Implement component model bindings in wasmtime4j-native using Wasmtime's component API
- **WIT Interface Binding**: Use Wasmtime's built-in WIT support rather than external tooling to minimize dependencies
- **Resource Management**: Extend existing resource tracking patterns for component instances and handles

### Public API Design
- **Factory Pattern**: Single entry point `WasiFactory.create()` with runtime auto-detection
- **Builder Abstraction**: Extend existing WasiContextBuilder pattern for WASI2 configuration
- **Unified Exception Hierarchy**: Map component model errors to existing WASI exception types

## Technical Approach

### Native Library Extensions (wasmtime4j-native)
**Component Model Bindings**
- Add Wasmtime component instantiation and linking APIs
- Implement WIT interface resolution and binding
- Add component resource management with automatic cleanup
- Extend existing JNI and Panama FFI exports for component operations

**Streaming Extensions**
- Build async I/O on existing file operation foundations
- Add backpressure and flow control mechanisms  
- Implement streaming interfaces compatible with Java reactive patterns
- Extend resource limits to cover streaming operations

### Public API Layer (wasmtime4j)
**Unified WASI Interface**
- Create `ai.tegmentum.wasmtime4j.wasi` package with public interfaces
- Implement `WasiFactory` with automatic JNI/Panama selection
- Add `WasiComponent` interface for component model operations
- Extend existing error handling patterns for WASI2 errors

**Component Composition Framework**
- Add component linking and resource sharing capabilities
- Implement pipeline composition for data processing use cases
- Provide validation for component interface compatibility
- Add component lifecycle management (load, instantiate, execute, cleanup)

### Backend Implementation Updates
**JNI Backend Extensions**
- Extend existing JNI wasi package structure for component model
- Add component instantiation and execution methods
- Implement streaming I/O integration with Java NIO
- Maintain existing security and permission validation patterns

**Panama Backend Extensions**  
- Add component model FFI bindings matching JNI interface
- Implement streaming operations using Panama memory segments
- Ensure feature parity with JNI implementation
- Leverage existing Panama resource management patterns

## Implementation Strategy

### Phase 1: Foundation (Month 1-2)
**Native Library Updates**
- Upgrade wasmtime4j-native to Wasmtime 36.0.2+ with WASI2 support
- Implement basic component instantiation and cleanup
- Add core WIT interface binding capabilities

**Public API Framework**
- Create unified WASI public API package structure
- Implement WasiFactory with runtime detection
- Add basic WasiComponent interface definition

### Phase 2: Core Features (Month 2-4)
**Component Model Support**
- Complete component loading, linking, and execution
- Add resource sharing between components
- Implement component composition pipelines

**Streaming I/O**
- Add async file operations building on existing file system
- Implement backpressure and flow control
- Integrate with Java CompletableFuture and reactive streams

### Phase 3: Advanced Features (Month 4-6)
**Network Capabilities**
- Add HTTP client interface through WASI2 component model
- Implement socket operations (TCP/UDP) with security validation
- Add TLS support leveraging existing security frameworks

**Key-Value Storage**
- Implement WASI2 KV interfaces with multiple backend options
- Add transaction support and consistency guarantees
- Integrate with existing resource quota management

## Task Breakdown Preview

High-level task categories that will be created:
- [ ] **Native WASI2 Integration**: Upgrade wasmtime4j-native with component model bindings
- [ ] **Public API Foundation**: Create unified WASI API in wasmtime4j module
- [ ] **Component Model Core**: Implement component loading, linking, and execution
- [ ] **Streaming I/O Framework**: Add async operations and backpressure handling
- [ ] **Backend Extensions**: Update JNI/Panama implementations for WASI2 support
- [ ] **Network Capabilities**: HTTP client and socket operations
- [ ] **Key-Value Storage**: Persistent storage interfaces with transaction support
- [ ] **Integration Testing**: Comprehensive test suite for all WASI2 features
- [ ] **Performance Optimization**: JMH benchmarks and performance tuning

## Dependencies

### External Dependencies
**Wasmtime 36.0.2+ with WASI Preview 2**
- Critical path: All WASI2 functionality depends on stable component model APIs
- Mitigation: Pin to specific Wasmtime version with controlled upgrade path
- Timeline: Immediate requirement for Phase 1

**Existing wasmtime4j Infrastructure**
- Leverage: Current WASI1 security, file system, and resource management
- Risk: Low - well-established foundation with comprehensive test coverage
- Timeline: Available immediately

### Internal Dependencies
**Native Development Capacity**  
- Requirement: Rust development for wasmtime4j-native component model bindings
- Timeline: 2 months critical path for core functionality
- Mitigation: Focus on essential features first, defer advanced capabilities

**Testing Infrastructure**
- Requirement: WASI2-specific test cases and component model validation
- Timeline: Parallel development throughout implementation
- Mitigation: Reuse existing test patterns and infrastructure

## Success Criteria (Technical)

### Performance Benchmarks
- **Component Instantiation**: < 10ms for typical WASI2 components
- **Streaming Throughput**: 95% of native performance for large data operations  
- **Memory Overhead**: < 1MB additional memory per component instance
- **Concurrent Scaling**: Support 1000+ simultaneous component instances

### Quality Gates
- **Test Coverage**: 95% code coverage including integration tests
- **Zero Crash Requirement**: No JVM crashes from any WASI2 operation
- **API Completeness**: 100% coverage of WASI Preview 2 specification
- **Cross-Platform Validation**: Full functionality on Linux/macOS/Windows x86_64/ARM64

### Acceptance Criteria
- **Single Import Access**: `import ai.tegmentum.wasmtime4j.wasi.*` provides complete API
- **Runtime Transparency**: Same code works on JNI (Java 8+) and Panama (Java 23+)
- **Backward Compatibility**: All existing WASI1 functionality preserved
- **Component Composition**: Multi-component pipelines with shared resources

## Estimated Effort

### Overall Timeline Estimate
- **MVP (Basic Component Model)**: 3-4 months
- **Full WASI2 Implementation**: 6 months  
- **Production Ready**: 8 months including comprehensive testing and documentation

### Resource Requirements
- **Primary Development**: Single developer with Rust and Java expertise
- **Testing Effort**: ~25% of development time for comprehensive test coverage
- **Documentation**: ~15% of development time for API docs and examples

### Critical Path Items
1. **Wasmtime Native Integration** (2 months) - Blocks all other work
2. **Public API Design** (1 month) - Required for backend implementation  
3. **Component Model Core** (2 months) - Foundation for advanced features
4. **Integration Testing** (1 month) - Validates cross-platform functionality

### Risk Mitigation
- **Parallel Development**: Work on API design while native integration progresses
- **Incremental Delivery**: Each phase delivers working functionality
- **Fallback Strategy**: Manual WIT interface implementation if tooling fails
- **Performance Validation**: Continuous benchmarking throughout development

## Tasks Created
- [ ] #100 - Implement Network Capabilities (parallel: true)
- [ ] #101 - Implement Key-Value Storage (parallel: true)
- [ ] #102 - Create Comprehensive Test Suite and Performance Benchmarks (parallel: false)
- [ ] #94 - Upgrade Native Library to WASI2 (parallel: false)
- [ ] #95 - Create Public WASI API Foundation (parallel: false)
- [ ] #96 - Implement Component Model Core (parallel: false)
- [ ] #97 - Implement Streaming I/O Framework (parallel: true)
- [ ] #98 - Update JNI Backend for WASI2 (parallel: true)
- [ ] #99 - Update Panama Backend for WASI2 (parallel: true)

Total tasks:        9
Parallel tasks:        5
Sequential tasks: 4
Estimated total effort: 640 hours
