---
name: remove-concurrent-access-coordinator
status: backlog
created: 2025-09-21T13:14:30Z
progress: 0%
prd: .claude/prds/remove-concurrent-access-coordinator.md
github: [Will be updated when synced to GitHub]
---

# Epic: Remove Concurrent Access Coordinator

## Overview

Remove the over-engineered `ConcurrentAccessCoordinator` (454 lines) from the wasmtime4j-panama module and replace with simple, direct thread-safe mechanisms for FFI operations. This enterprise concurrency framework contains optimistic locking, bulk operation coordination, async batching, and complex statistics collection that adds unnecessary complexity to basic WebAssembly runtime bindings.

**Impact**: Eliminate 454+ lines of scope creep, reduce FFI call overhead by 10-50%, and simplify thread safety to basic synchronized blocks.

## Architecture Decisions

### Concurrency Strategy Shift
- **From**: Enterprise multi-level locking with optimistic concurrency control
- **To**: Simple synchronized blocks or basic ReentrantLock for FFI calls
- **Rationale**: Panama FFI operations need basic thread safety, not distributed systems coordination

### Synchronization Approach
- **Remove**: StampedLock optimistic reads, bulk operation batching, async coordination
- **Use**: Standard `synchronized` keyword or `ReentrantLock` for simplicity
- **Rationale**: Direct, understandable thread safety patterns for FFI operations

### Resource Management
- **Remove**: Complex arena coordination, resource type locks, deadlock prevention
- **Keep**: Basic thread-safe access to Panama MemorySegment and Arena operations
- **Rationale**: Panama FFI has built-in resource management; additional layers cause complexity

### Performance Philosophy
- **Remove**: Statistics collection, performance monitoring, operation batching
- **Focus**: Minimal overhead synchronization for correctness only
- **Rationale**: Performance optimization belongs in application layer, not runtime bindings

## Technical Approach

### Current Complex Infrastructure (Remove)
- **ConcurrentAccessCoordinator.java**: 454-line enterprise framework
- **Multi-level locking**: StampedLock, ReadWriteLock, resource-type locks
- **Bulk operations**: Queue-based batching with coordination
- **Async processing**: CompletableFuture-based operation pipelining
- **Statistics**: Performance monitoring and metrics collection

### Simple Replacement Strategy
- **Basic Synchronization**: Use `synchronized` blocks around FFI calls
- **Thread-Safe Utilities**: Simple utility methods for common patterns
- **Defensive Programming**: Maintain null checks and parameter validation
- **Clean Patterns**: Standard Java concurrency practices

### Implementation Pattern
```java
// Replace complex coordinator usage:
// coordinator.executeWithArenaCoordination(arena, operation)

// With simple synchronization:
private final Object ffiLock = new Object();

public Result callNative() {
    synchronized(ffiLock) {
        return nativeFFICall();
    }
}
```

### Affected Components
- **Panama Engine**: Direct FFI calls with simple synchronization
- **Panama Store**: Basic thread-safe state management
- **Panama Memory**: Simple synchronized memory operations
- **Panama Functions**: Thread-safe function invocation
- **Resource Management**: Basic Arena lifecycle protection

## Implementation Strategy

### Phase 1: Audit Current Usage (0.5 days)
- Verify ConcurrentAccessCoordinator has no current usage in codebase
- Identify any Panama FFI operations requiring thread safety
- Document current synchronization patterns in existing code

### Phase 2: Implement Simple Thread Safety (1 day)
- Add basic synchronized blocks to FFI operations requiring thread safety
- Implement simple utility patterns for common synchronization needs
- Ensure defensive programming principles are maintained

### Phase 3: Remove Coordinator Infrastructure (0.5 days)
- Delete ConcurrentAccessCoordinator.java and related classes
- Clean up any imports or references (currently none found)
- Remove compiled .class files from build directories

### Phase 4: Testing and Validation (1 day)
- Run comprehensive tests to ensure no functional regression
- Validate thread safety with concurrent access tests
- Measure FFI call overhead improvements
- Confirm build and runtime performance improvements

## Task Breakdown Preview

High-level task categories that will be created:
- [ ] **Usage Audit**: Verify no current usage and identify FFI operations needing thread safety
- [ ] **Simple Synchronization**: Implement basic thread-safe patterns for FFI operations
- [ ] **Coordinator Removal**: Delete ConcurrentAccessCoordinator and cleanup references
- [ ] **Thread Safety Testing**: Validate concurrent access works correctly with simple patterns
- [ ] **Performance Validation**: Confirm FFI overhead reduction and no functional regression

## Dependencies

### External Dependencies
- **None**: This is a removal/simplification effort with no external blocking dependencies

### Internal Dependencies
- **Code Review**: Maintainer approval for simplified thread safety approach
- **Testing Verification**: Ensure simplified patterns maintain thread safety guarantees
- **Performance Baseline**: Measure FFI call overhead before and after changes

### Risk Mitigation
- **Low Risk**: Coordinator currently has no usage, making removal safe
- **Conservative Approach**: Add simple synchronization only where actually needed
- **Rollback Plan**: Simple revert capability if any issues discovered during testing

## Success Criteria (Technical)

### Performance Benchmarks
- **FFI Call Overhead**: 10-50% reduction in direct call latency
- **Memory Usage**: Elimination of coordination infrastructure overhead
- **Build Performance**: Faster compilation without complex concurrency code

### Quality Gates
- **Thread Safety**: All FFI operations remain thread-safe with simple patterns
- **Functional Coverage**: Zero regression in WebAssembly functionality
- **Code Simplicity**: Thread safety understandable by any Java developer

### Acceptance Criteria
- ConcurrentAccessCoordinator completely removed from codebase
- Simple synchronized blocks implemented for FFI operations requiring thread safety
- All existing tests pass without modification
- FFI call overhead measurably reduced
- Code complexity significantly reduced (454+ lines removed)

## Estimated Effort

### Overall Timeline
- **Total Duration**: 3 days
- **Critical Path**: Simple synchronization implementation
- **Resource Requirements**: 1 developer familiar with Panama FFI patterns

### Task Distribution
- **Usage Audit**: 0.5 days (15%)
- **Simple Synchronization**: 1 day (35%)
- **Coordinator Removal**: 0.5 days (15%)
- **Testing and Validation**: 1 day (35%)

### Risk Factors
- **Very Low Risk**: Removing unused scope creep with simple replacement
- **Main Consideration**: Ensuring adequate thread safety without over-engineering
- **Performance Gain**: Direct FFI calls without coordination overhead

This epic eliminates enterprise middleware complexity and returns to appropriate, simple thread safety for WebAssembly runtime FFI operations.