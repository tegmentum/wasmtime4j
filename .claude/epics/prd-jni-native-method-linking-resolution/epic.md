---
name: prd-jni-native-method-linking-resolution
status: backlog
created: 2025-09-10T00:13:50Z
progress: 0%
prd: .claude/prds/prd-jni-native-method-linking-resolution.md
github: https://github.com/tegmentum/wasmtime4j/issues/202
---

# Epic: JNI Native Method Linking Resolution

## Overview

Fix critical JNI UnsatisfiedLinkError issues preventing 7 JniTable native methods from linking properly between Java and Rust. The core problem is that despite implementing native methods in Rust with correct signatures, the JVM cannot discover them at runtime, blocking all WebAssembly table operations. This epic focuses on systematic JNI symbol resolution, library loading verification, and build system synchronization to achieve 100% test pass rate.

## Architecture Decisions

**JNI Symbol Resolution Strategy**
- Use exact JNI naming convention compliance (Java_package_Class_method)
- Implement automated symbol verification during build process
- Leverage native debugging tools (nm, objdump) for symbol inspection
- Maintain strict signature matching between Java declarations and Rust exports

**Build System Integration**
- Keep existing Maven-Rust integration but add verification steps
- Use cargo build hooks to validate JNI symbol generation
- Implement incremental build detection for Rust-Java synchronization
- Add build-time JNI compatibility checks to prevent regressions

**Library Loading Approach**
- Preserve existing NativeLibraryLoader pattern for consistency
- Enhance error reporting and diagnostic logging for loading failures
- Implement library version compatibility verification
- Use platform-specific loading strategies without breaking abstraction

**Error Handling Pattern**
- Leverage existing WasmtimeError -> Java exception mapping
- Enhance JNI error propagation with detailed diagnostic information
- Maintain defensive programming principles with comprehensive validation
- Use existing logging infrastructure for troubleshooting

## Technical Approach

### Native Library Integration
**JNI Symbol Generation**
- Verify exact method name mangling follows JNI specification
- Ensure package name encoding matches: `ai_tegmentum_wasmtime4j_jni_JniTable`
- Validate parameter type encoding in method signatures
- Use existing Rust `#[no_mangle]` and `extern "system"` patterns

**Library Loading Mechanism**
- Enhance existing NativeLibraryLoader with symbol verification
- Add comprehensive logging to library search and loading process
- Implement version compatibility checks between Java and native components
- Use platform-specific diagnostic tools for troubleshooting

**Build System Coordination**
- Leverage existing Maven-Cargo integration in wasmtime4j-native module
- Add post-compilation symbol verification steps
- Implement incremental build detection for Rust changes
- Maintain existing cross-platform compilation support

### Method Implementation Completion
**Wasmtime API Integration**
- Replace placeholder implementations with actual Wasmtime table operations
- Use existing wasmtime-rs crate patterns for Table, Store integration
- Implement proper memory management and resource lifecycle handling
- Maintain existing error handling and exception mapping patterns

**Parameter Validation**
- Leverage existing JniValidation utility patterns
- Implement comprehensive bounds checking for table operations
- Use existing defensive programming approach with null pointer validation
- Maintain consistent error message formatting and exception types

### Testing & Verification
**Automated Testing**
- Utilize existing JUnit 5 test framework and structure
- Leverage existing test patterns from working JNI methods
- Implement systematic testing of each fixed native method
- Use existing test categories (ConstructorTests, ResourceManagementTests, etc.)

**Build Verification**
- Add JNI symbol verification to existing Maven build process
- Implement automated regression detection for JNI integration
- Use existing cross-platform testing infrastructure
- Maintain existing code quality standards (Checkstyle, Spotless)

## Implementation Strategy

**Incremental Fix Approach**
- Fix one native method at a time to isolate issues
- Use existing working native methods as reference implementation
- Leverage successful JniValidation patterns from constructor fixes
- Maintain existing code organization and patterns

**Risk Mitigation**
- Use existing JNI patterns that already work (constructor validation)
- Implement comprehensive logging before making changes
- Create backup points before each modification
- Test incrementally to prevent breaking working functionality

**Quality Assurance**
- Leverage existing test suite structure and patterns
- Use established error handling and exception mapping
- Maintain existing code style and organization standards
- Follow existing defensive programming principles

## Task Breakdown Preview

High-level task categories that will be created:
- [ ] **JNI Symbol Diagnostic Analysis**: Investigate current symbol generation and linking
- [ ] **Library Loading Verification**: Enhance loading diagnostics and error handling  
- [ ] **Symbol Resolution Fix**: Correct JNI naming and signature issues
- [ ] **Method Implementation Completion**: Replace placeholders with Wasmtime API calls
- [ ] **Build System Enhancement**: Add verification and synchronization checks
- [ ] **Comprehensive Testing**: Validate all fixes and prevent regressions

## Dependencies

**Internal Dependencies**
- Existing Rust toolchain and wasmtime-rs integration
- Current Maven build system and multi-module structure
- Working JniValidation and error handling patterns
- Existing NativeLibraryLoader and loading infrastructure

**External Dependencies**
- Stable Wasmtime API for table operations (already integrated)
- JNI specification compliance (standard requirement)
- Platform-specific native debugging tools (nm, objdump, etc.)
- Existing CI/CD pipeline and test execution environment

**Prerequisite Work**
- Previous constructor validation fixes provide working reference patterns
- Existing native method implementations provide structural foundation
- Current test infrastructure provides verification framework
- Established error handling patterns provide integration approach

## Success Criteria (Technical)

**Primary Technical Metrics**
- 100% JniTable test pass rate (21/21 tests passing, currently 14/21)
- Zero UnsatisfiedLinkError exceptions in test execution
- All 7 native methods successfully linked and functional
- Build system passes JNI symbol verification checks

**Quality Gates**
- All existing tests remain passing (no regressions)
- Code maintains existing style and organization standards
- Error messages provide actionable diagnostic information
- Performance meets existing baseline (sub-10ms method calls)

**Acceptance Criteria**
- Native methods return meaningful results (not placeholders)
- Exception handling follows existing patterns and provides clear errors
- Build system automatically detects and prevents JNI integration issues
- Cross-platform compatibility maintained on all supported architectures

## Tasks Created

- [ ] 001.md - JNI Symbol Diagnostic Analysis (parallel: true)
- [ ] 002.md - Library Loading Verification and Enhancement (parallel: true)
- [ ] 003.md - JNI Symbol Resolution and Method Signature Fixes (parallel: false)
- [ ] 004.md - Wasmtime API Method Implementation (parallel: false)
- [ ] 005.md - Build System Enhancement and Verification (parallel: true)
- [ ] 006.md - Comprehensive Testing and Validation (parallel: false)

**Total tasks**: 6
**Parallel tasks**: 3 (001, 002, 005)
**Sequential tasks**: 3 (003, 004, 006)
**Estimated total effort**: 92 hours (11.5 days)

## Estimated Effort

**Overall Timeline**: 5-7 days (simplified from original 15-day estimate)

**Critical Path Items**:
1. JNI symbol diagnostic analysis (1 day)
2. Symbol resolution and method signature fixes (2-3 days)  
3. Method implementation completion with Wasmtime API (2 days)
4. Comprehensive testing and verification (1 day)

**Resource Requirements**:
- Single developer with JNI and Rust experience
- Access to native debugging tools and build environment
- Existing codebase and test infrastructure
- Platform-specific compilation and testing capabilities

**Risk Factors**:
- Platform-specific JNI symbol generation differences
- Wasmtime API integration complexity for table operations
- Build system synchronization edge cases
- Potential for introducing regressions in working functionality