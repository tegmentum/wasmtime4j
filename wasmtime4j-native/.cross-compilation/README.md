# Cross-Compilation Setup for Wasmtime4j

This directory contains the cross-compilation configuration for building Wasmtime4j native libraries across all supported platforms.

## Supported Platforms

| Target Triple | Platform Name | Architecture | Notes |
|---------------|---------------|--------------|-------|
| `x86_64-unknown-linux-gnu` | linux-x86_64 | unknown-linux | |
| `aarch64-unknown-linux-gnu` | linux-aarch64 | unknown-linux | |
| `x86_64-pc-windows-msvc` | windows-x86_64 | pc-windows | |
| `x86_64-apple-darwin` | macos-x86_64 | apple | |
| `aarch64-apple-darwin` | macos-aarch64 | apple | |

## Environment Setup

The `environment.sh` script configures the build environment for cross-compilation:

```bash
# Load general cross-compilation environment
source .cross-compilation/environment.sh

# Load target-specific environment
source .cross-compilation/environment.sh x86_64-unknown-linux-gnu
```

## Build Reproducibility

To ensure reproducible builds across different environments:

1. **Rust Version**: Use Rust 1.75.0 or compatible version
2. **Environment Variables**: Consistent RUSTFLAGS and build settings
3. **Source Date Epoch**: Fixed timestamp for deterministic builds
4. **Cargo Configuration**: Network and HTTP settings for reliability

## Platform-Specific Dependencies

Some targets may require additional system dependencies:

### linux-x86_64 (`x86_64-unknown-linux-gnu`)

Required dependencies: `gcc-multilib libc6-dev`

### linux-aarch64 (`aarch64-unknown-linux-gnu`)

Required dependencies: `gcc-aarch64-linux-gnu`

### windows-x86_64 (`x86_64-pc-windows-msvc`)

Required dependencies: `msvc-tools`

### macos-x86_64 (`x86_64-apple-darwin`)

Required dependencies: `osxcross`

### macos-aarch64 (`aarch64-apple-darwin`)

Required dependencies: `osxcross`

## Usage

### Setup (one-time)
```bash
./scripts/setup-cross-compilation.sh
```

### Build for specific platform
```bash
./scripts/build-native.sh --target x86_64-unknown-linux-gnu
```

### Build all platforms
```bash
./scripts/build-native.sh --all-platforms
```

## Troubleshooting

### Common Issues

1. **Missing cross-compiler**: Install platform-specific development tools
2. **Linker errors**: Ensure target-specific linkers are available
3. **Library not found**: Check that system libraries for target are installed

### Debug Mode

Enable debug output:
```bash
DEBUG=true ./scripts/setup-cross-compilation.sh
```

## Generated Files

- `environment.sh`: Cross-compilation environment configuration
- `targets.list`: List of installed targets
- `build-cache/`: Cached build artifacts (can be safely deleted)

## Build Cache

Build artifacts are cached in `build-cache/` to speed up incremental builds. The cache is organized by:

- Target triple
- Build mode (debug/release)
- Source hash

Cache can be cleaned with: `./scripts/build-native.sh --clean-cache`

## Last Updated

Generated on: 2025-09-16 11:00:17 UTC
Rust Version: rustc 1.89.0 (29483883e 2025-08-04)
Host Platform: macos-aarch64
