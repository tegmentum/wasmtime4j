---
name: rust-build-integration
description: Integrate Rust/Cargo compilation into Maven build process with Wasmtime source compilation and cross-platform support
status: backlog
created: 2025-08-30T00:02:25Z
---

# PRD: Rust Build Integration

## Executive Summary

Integrate Rust compilation into the Maven build process to automatically download and compile Wasmtime 36.0.2 from source for the host platform, with cross-compilation handled by GitHub Actions CI/CD pipeline. This eliminates the current dependency on pre-built native libraries while maintaining fast local development builds and reliable cross-platform artifact distribution.

## Problem Statement

**Current State:**
- wasmtime4j-native contains comprehensive Maven-Cargo integration infrastructure
- Cargo.toml correctly depends on Wasmtime 36.0.2
- Build system expects native libraries but cannot create them from source
- Project cannot execute real WebAssembly modules due to stub implementations

**Problems:**
1. **Source Compilation Gap**: No mechanism to download/compile Wasmtime from source
2. **Developer Friction**: Requires manual native library management
3. **Cross-Platform Distribution**: No automated system for building and distributing platform-specific libraries
4. **Build Inconsistency**: Different developers may have different native library versions
5. **CI/CD Blockers**: Cannot build complete project from source in automated environments

**Why This Matters Now:**
The current codebase has excellent foundations but cannot execute WebAssembly modules, making it unusable for its core purpose. Completing this integration transforms the project from a sophisticated stub into a functional WebAssembly runtime.

## User Stories

### Primary Developer Persona: Java WebAssembly Developer
- **Name**: Alex Chen, Senior Java Developer
- **Context**: Building a microservice that executes user-provided WASM modules
- **Environment**: macOS M1, IntelliJ IDEA, Maven builds

**User Journey:**
1. **Project Setup**: `git clone && ./mvnw compile` should build everything including native libraries
2. **Development Cycle**: Changes to Rust code should trigger incremental native builds
3. **Testing**: `./mvnw test` should run with freshly compiled native libraries
4. **Cross-Platform**: Should automatically receive platform-specific libraries from CI/CD builds

**Pain Points Being Addressed:**
- Currently cannot compile project without pre-built libraries
- No way to modify/debug native Wasmtime integration
- Cannot ensure library versions match across environments

### Secondary Persona: CI/CD Pipeline
- **Context**: Automated builds, releases, and testing
- **Requirements**: Full source builds, reproducible artifacts, platform-specific outputs

### Tertiary Persona: Platform Maintainer
- **Context**: Managing Wasmtime version updates and platform support
- **Requirements**: Easy version bumps, new platform addition, build troubleshooting

## Requirements

### Functional Requirements

**FR1: Wasmtime Source Management**
- Download Wasmtime 36.0.2 source during Maven build
- Verify source integrity (checksums/signatures)
- Cache downloaded source to avoid repeated downloads
- Support configurable Wasmtime versions via Maven properties

**FR2: Native Compilation Integration**
- Compile Wasmtime C API from source using Cargo
- Generate platform-specific native libraries (libwasmtime4j.{dylib,so,dll})
- Integrate with existing Maven build phases
- Support both debug and release build modes

**FR3: CI/CD Cross-Platform Build System**
- GitHub Actions workflow to build all supported platforms: linux-x86_64, linux-aarch64, windows-x86_64, macos-x86_64, macos-aarch64
- Automated artifact collection and packaging into Maven-compatible JARs
- Platform-specific build validation and testing
- Automatic publishing of cross-platform native library artifacts

**FR4: Build Optimization**
- Incremental builds - only rebuild when native source changes
- Parallel compilation when building multiple targets
- Build artifact caching between Maven runs
- Skip native builds when pre-built libraries are current

**FR5: Developer Experience**
- Single command builds: `./mvnw compile` includes host-platform native compilation
- Automatic download of cross-platform libraries from CI/CD artifacts
- Development profile for fast iterative builds on host platform only
- Integration with existing Maven profiles and lifecycles

**FR6: Integration Points**
- JNI header generation and synchronization
- Panama FFI symbol export verification  
- Library loading path resolution
- Test execution with compiled native libraries

**FR7: GitHub Actions Integration**
- Automated workflow triggered on native code changes
- Matrix builds for all supported platforms (Linux x86_64/aarch64, Windows x86_64, macOS x86_64/aarch64)
- Artifact publishing with versioned naming scheme
- Integration with Maven artifact resolution system

**FR8: Native Library Runtime Selection and Loading**
- Platform detection and automatic library selection at runtime
- Graceful handling of missing platform-specific libraries
- Integration with existing NativeLibraryUtils for CI/CD artifacts
- Multi-tier fallback strategy (CI/CD artifacts → cached builds → embedded libraries)

### Non-Functional Requirements

**NFR1: Performance**
- Initial build time: < 10 minutes for host platform on modern hardware
- Incremental builds: < 30 seconds when only Rust code changes
- CI/CD cross-platform build: < 30 minutes for all platforms in parallel
- Memory usage: < 4GB during local compilation

**NFR2: Reliability**
- Build success rate > 99% on supported platforms
- Deterministic builds - same source produces identical artifacts
- Robust error handling and recovery
- Comprehensive validation of build outputs

**NFR3: Maintainability**
- Clear separation between Maven and Cargo concerns
- Configurable via Maven properties
- Comprehensive logging and diagnostics
- Self-documenting build process

**NFR4: Platform Support**
- Development support: macOS (Intel/ARM), Linux (x86_64), Windows 10/11
- Host-platform compilation for local development
- CI/CD-generated libraries for all target platforms
- Consistent behavior across different host environments

**NFR5: Security**
- Secure source downloads (HTTPS, integrity verification)
- No embedded secrets or credentials
- Safe handling of cross-compilation toolchains
- Build isolation and sandboxing where possible

**NFR6: Artifact Integrity and Security**
- Cryptographic signing of CI/CD-built native libraries
- Checksum verification of downloaded artifacts
- Secure artifact storage and distribution
- Build provenance and audit trail for all native libraries

## Success Criteria

**Primary Success Metrics:**
1. **Build Success Rate**: 100% successful builds on clean checkout for all supported development platforms
2. **WebAssembly Execution**: Compiled native libraries successfully execute real WASM modules
3. **Cross-Platform Coverage**: All 5 target platforms build successfully in GitHub Actions CI/CD
4. **Developer Adoption**: Build time acceptable for daily development workflow
5. **CI/CD Reliability**: > 95% successful CI/CD builds over 30-day period
6. **Artifact Integrity**: 100% of downloaded artifacts pass verification checks

**Key Performance Indicators:**
- Time to first successful build: < 10 minutes
- Incremental build time: < 30 seconds
- CI/CD cross-platform build time: < 30 minutes
- CI/CD build success rate: > 95%
- Artifact download and verification time: < 2 minutes
- Test execution success with native libraries: 100%

**Validation Criteria:**
- `./mvnw clean compile` succeeds on fresh checkout
- Generated native libraries pass basic smoke tests
- JNI and Panama implementations both work with compiled libraries
- CI/CD-built libraries work on their target platforms
- Build artifacts are deterministic and reproducible

## Constraints & Assumptions

**Technical Constraints:**
- Must use Wasmtime 36.0.2 (latest stable release)
- Must maintain existing Maven project structure
- Must support existing development workflows
- Cannot break existing CI/CD pipelines
- Must work with standard Rust toolchain (rustc, cargo, rustup)

**Resource Constraints:**
- Build system complexity should not significantly impact maintainability
- Native compilation memory usage must be reasonable for developer machines
- Build time should not become prohibitive for daily development

**Platform Constraints:**
- GitHub Actions runners must support all target platform builds
- Must handle platform-specific dependencies in CI/CD environment
- Local development builds only for host platform to reduce complexity

**Assumptions:**
- Developers have or can install Rust toolchain (rustc, cargo, rustup)
- Internet access available for source downloads and CI/CD artifacts
- GitHub Actions provides reliable cross-platform build environment
- Maven build environment has sufficient memory and disk space
- Wasmtime source compilation is deterministic and reproducible

## Out of Scope

**Explicitly NOT Building:**
- Custom Wasmtime patches or modifications
- Alternative WebAssembly runtimes (Wasmer, etc.)
- Dynamic version switching between multiple Wasmtime versions
- Native library optimization beyond standard Wasmtime optimizations
- Local cross-compilation toolchain setup (delegated to CI/CD)
- IDE-specific integration beyond standard Maven support
- Docker/container-based compilation environments (GitHub Actions handles this)
- Native library signing or notarization for distribution
- Performance regression detection (exists in benchmarks module)
- Wasmtime runtime configuration or tuning options
- Self-hosted runners or custom CI/CD infrastructure

## Dependencies

**External Dependencies:**
- Rust toolchain (rustc, cargo, rustup) installed on development machines
- GitHub Actions infrastructure for cross-platform builds
- GitHub Actions runner availability and quota limits
- GitHub artifact storage limits and retention policies
- Internet access for Wasmtime source downloads and CI/CD artifacts
- System development tools (C compiler, linker) for host-platform compilation
- Maven Central or alternative repository for artifact distribution (future consideration)

**Internal Dependencies:**
- wasmtime4j-jni module JNI header generation must be synchronized
- wasmtime4j-panama module FFI bindings must match compiled exports
- wasmtime4j-native existing Maven configuration and profiles
- Build system must integrate with existing test execution framework

**Process Dependencies:**
- CLAUDE.md guidelines for no partial implementations
- Google Java Style Guide compliance for any added Java code
- Conventional commits format for implementation commits
- Existing static analysis tools (Checkstyle, SpotBugs, etc.) must pass

**Timeline Dependencies:**
- Must coordinate with any ongoing development to avoid merge conflicts
- Should align with next project milestone or release cycle
- GitHub Actions workflow setup may require iterative testing and refinement

## Implementation Notes

**Build Phases Integration:**
- Source download: `initialize` phase
- Host-platform toolchain setup: `initialize` phase  
- Host-platform native compilation: `compile` phase
- CI/CD artifact download: `process-resources` phase
- Library packaging: `process-classes` phase
- Validation: `test-compile` phase

**Maven Profiles Strategy:**
- Extend existing native-dev, native-release profiles for host-platform builds
- Add ci-artifacts profile for downloading cross-platform libraries
- Maintain existing incremental-build profile for local development
- Replace all-platforms profile with ci-integration profile

**Risk Mitigation:**
- Fallback to pre-built libraries if local compilation fails
- Fallback to cached CI/CD artifacts if latest builds unavailable
- Comprehensive validation of both local and CI/CD libraries
- Clear error messages and troubleshooting guidance
- Gradual rollout - initial opt-in, then default behavior

**Quality Gates:**
- All existing tests must pass with locally compiled native libraries
- CI/CD libraries must be validated on their respective target platforms
- Local build performance must meet specified time constraints
- CI/CD build performance must complete within acceptable timeframes
- No regressions in existing Maven build behavior

## Implementation Strategy

**Phased Rollout Approach:**

**Phase 1: Opt-In Local Compilation (Weeks 1-2)**
- Implement Wasmtime source download and host-platform compilation
- Add Maven profile `-Pnative-compile` for developers to opt-in
- Maintain existing pre-built library fallback as default behavior
- Validate basic functionality with local compilation

**Phase 2: CI/CD Infrastructure (Weeks 3-4)** 
- Implement GitHub Actions matrix build workflow
- Add artifact signing and verification
- Test CI/CD builds but keep as secondary option
- Add Maven profile `-Pci-artifacts` to use CI/CD libraries

**Phase 3: Integration & Testing (Weeks 5-6)**
- Integrate native library runtime selection logic
- Implement multi-tier fallback strategy
- Comprehensive testing across all platforms and scenarios
- Performance benchmarking and optimization

**Phase 4: Default Transition (Weeks 7-8)**
- Make local compilation the default for development builds
- Enable CI/CD artifacts for release builds
- Maintain pre-built library fallback for emergency scenarios
- Monitor adoption and collect feedback

**Phase 5: Full Deployment (Weeks 9-10)**
- Remove pre-built library dependencies
- CI/CD becomes the standard for cross-platform distribution
- Documentation and developer training
- Post-deployment monitoring and optimization

**Rollback Strategy:**
- Each phase can revert to previous behavior via Maven profiles
- Pre-built libraries remain available as ultimate fallback
- Clear rollback procedures documented for each phase