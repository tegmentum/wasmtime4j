---
name: rust-build-integration
status: completed
created: 2025-08-30T00:35:27Z
completed: 2025-08-30T23:23:04Zprogress: 100%
prd: .claude/prds/rust-build-integration.md
github: https://github.com/tegmentum/wasmtime4j/issues/15
---

# Epic: Rust Build Integration

## Overview

Transform wasmtime4j from stub implementation to functional WebAssembly runtime by integrating Wasmtime 36.0.2 source compilation into the Maven build process. Leverage existing Maven-Cargo infrastructure in wasmtime4j-native, add host-platform compilation for local development, and implement GitHub Actions CI/CD for cross-platform library distribution.

## Architecture Decisions

**1. Hybrid Build Strategy**
- **Local Development**: Host-platform only compilation for fast iteration
- **Cross-Platform**: GitHub Actions matrix builds for production artifacts
- **Rationale**: Balances developer productivity with comprehensive platform coverage

**2. Leverage Existing Infrastructure**
- **Build on Current Foundation**: Extensive Maven-Cargo integration already exists
- **Extend Rather Than Replace**: Enhance existing profiles and build phases
- **Preserve Backward Compatibility**: Maintain fallback to pre-built libraries

**3. Multi-Tier Fallback Architecture**
- **Primary**: Locally compiled native libraries
- **Secondary**: CI/CD-built artifacts from GitHub Actions
- **Tertiary**: Embedded pre-built libraries (emergency fallback)
- **Runtime Selection**: Automatic platform detection and library loading

**4. Cargo Integration Strategy**
- **Source Management**: Maven downloads Wasmtime source, Cargo handles compilation
- **Build Lifecycle**: Integrate Cargo builds into Maven compile phase
- **Artifact Handling**: Maven packages and distributes compiled libraries

## Technical Approach

### Maven Build System Integration

**Wasmtime Source Management**
- Add Maven exec plugin execution to download Wasmtime 36.0.2 source
- Implement checksum verification for source integrity
- Cache downloaded source using Maven dependency mechanisms
- Use existing `wasmtime.version` property for version configuration

**Native Compilation Pipeline**
- Extend existing Cargo build executions in wasmtime4j-native/pom.xml
- Replace stub Rust implementations with real Wasmtime API calls
- Integrate with existing platform detection and library naming conventions
- Leverage existing incremental build profiles

**Library Distribution**
- Enhance existing JAR packaging with platform-specific classifiers
- Extend NativeLibraryUtils to handle CI/CD artifacts
- Implement library selection logic based on runtime platform detection
- Maintain existing resource loading mechanisms

### GitHub Actions CI/CD System

**Matrix Build Workflow**
- Create `.github/workflows/native-build.yml` with platform matrix
- Use existing Rust targets: linux-x86_64, linux-aarch64, windows-x86_64, macos-x86_64, macos-aarch64
- Implement artifact signing and verification
- Publish versioned artifacts to GitHub Packages or Maven Central

**Build Triggers**
- Trigger on changes to `wasmtime4j-native/src/**` and `wasmtime4j-native/Cargo.toml`
- Manual trigger for version updates and releases
- Integration with existing CI/CD workflows

### Native Library Runtime System

**Platform Detection & Loading**
- Extend existing platform detection in core wasmtime4j module
- Enhance NativeLibraryUtils with multi-source artifact resolution
- Implement graceful degradation when libraries unavailable
- Maintain existing library loading error handling

**Integration Points**
- Synchronize JNI header generation with native compilation
- Validate Panama FFI exports match compiled library symbols  
- Ensure test execution works with both local and CI/CD libraries
- Integrate with existing resource management and cleanup

## Implementation Strategy

**Phased 10-Week Rollout**
- **Phase 1 (Weeks 1-2)**: Local compilation with opt-in Maven profile
- **Phase 2 (Weeks 3-4)**: GitHub Actions infrastructure and artifact publishing
- **Phase 3 (Weeks 5-6)**: Integration testing and multi-tier fallback system
- **Phase 4 (Weeks 7-8)**: Default transition with monitoring and feedback
- **Phase 5 (Weeks 9-10)**: Full deployment and documentation

**Risk Mitigation**
- Each phase maintains rollback capability via Maven profiles
- Comprehensive testing at each phase before progression
- Fallback mechanisms ensure no disruption to existing workflows

## Task Breakdown Preview

High-level task categories that will be created:

- [ ] **Maven Source Integration**: Download and verify Wasmtime 36.0.2 source during build
- [ ] **Native Compilation Pipeline**: Replace stubs with real Wasmtime integration in Rust code
- [ ] **GitHub Actions Workflow**: Implement cross-platform matrix builds and artifact publishing
- [ ] **Library Runtime Selection**: Enhance NativeLibraryUtils for multi-source artifact resolution
- [ ] **Build System Integration**: Extend existing Maven profiles for local and CI/CD builds
- [ ] **Testing & Validation**: Ensure all implementations work with compiled native libraries
- [ ] **Documentation & Migration**: Update build documentation and provide migration guide

## Dependencies

**External Dependencies**
- Rust toolchain (1.75.0+) on developer machines
- GitHub Actions infrastructure and artifact storage
- Internet connectivity for source downloads and CI/CD artifacts

**Internal Dependencies**
- Existing wasmtime4j-native Maven-Cargo integration
- JNI header generation in wasmtime4j-jni module
- Panama FFI bindings in wasmtime4j-panama module
- Test framework integration across all modules

**Critical Path**
- Wasmtime source integration must complete before native compilation
- GitHub Actions workflow must be operational before CI/CD artifact integration
- Runtime selection logic depends on both local and CI/CD build systems

## Success Criteria (Technical)

**Build System Performance**
- Host-platform build time: < 10 minutes on modern hardware
- Incremental build time: < 30 seconds for Rust changes only
- CI/CD cross-platform build: < 30 minutes for all platforms

**Functional Validation**
- 100% test suite pass rate with locally compiled libraries
- Cross-platform libraries validated on target platforms
- Real WebAssembly module execution (replacing current stubs)
- Seamless integration with existing JNI and Panama implementations

**Quality Gates**
- No regressions in existing Maven build behavior
- Maintain Google Java Style Guide compliance
- Pass all existing static analysis tools (Checkstyle, SpotBugs, etc.)
- Build success rate > 99% on supported development platforms

## Estimated Effort

**Overall Timeline**: 10 weeks (phased implementation)

**Resource Requirements**
- 1 developer for Maven/Java integration (40% effort)
- 1 developer for Rust/native implementation (60% effort)  
- DevOps support for GitHub Actions setup (20% effort)
- Testing and validation across all phases

**Critical Path Items**
- Week 2: Local compilation functional
- Week 4: GitHub Actions producing valid artifacts
- Week 6: Integration and fallback systems working
- Week 8: Default transition complete
- Week 10: Full deployment and monitoring

**Key Simplifications**
- Leverage 90% of existing Maven-Cargo infrastructure
- Build on proven platform detection and loading mechanisms
- Use GitHub Actions instead of complex local cross-compilation
- Extend existing patterns rather than creating new architectures

## Tasks Created

- [ ] #16 - Maven Source Integration (parallel: true)
- [ ] #17 - Native Compilation Pipeline (parallel: false)
- [ ] #18 - Build System Integration (parallel: false)
- [ ] #23 - GitHub Actions Workflow (parallel: true)
- [ ] #24 - Library Runtime Selection (parallel: false)
- [ ] #25 - Testing & Validation (parallel: false)
- [ ] #26 - Documentation & Migration Guide (parallel: false)

**Task Summary:**
- Total tasks: 7
- Parallel tasks: 2 (16, 23)
- Sequential tasks: 5
- Estimated total effort: 184-242 hours (5-6 weeks with 1 developer, 3-4 weeks with 2 developers)

**Critical Path:** #16 → #17 → #18 → #24 → #25 → #26 (with #23 parallel to #18)
