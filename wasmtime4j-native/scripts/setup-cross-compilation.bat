@echo off
REM Wasmtime4j Cross-Compilation Setup Script for Windows
REM This script sets up the complete cross-compilation environment for all supported platforms

setlocal enabledelayedexpansion

set PROJECT_ROOT=%~dp0\..
set SCRIPT_DIR=%PROJECT_ROOT%\scripts

REM Configuration
set WASMTIME_VERSION=36.0.2
set REQUIRED_RUST_VERSION=1.75.0

echo [INFO] Wasmtime4j Cross-Compilation Setup
echo [INFO] ===================================
echo [INFO] Project Root: %PROJECT_ROOT%

REM Cross-compilation targets
set targets[0]=x86_64-unknown-linux-gnu
set targets[1]=aarch64-unknown-linux-gnu
set targets[2]=x86_64-pc-windows-gnu
set targets[3]=x86_64-apple-darwin
set targets[4]=aarch64-apple-darwin

set platforms[0]=linux-x64
set platforms[1]=linux-aarch64
set platforms[2]=windows-x64
set platforms[3]=macos-x64
set platforms[4]=macos-aarch64

REM Parse command line arguments
set skip_deps=false
set verify_only=false
set clean_setup=false

:parse_args
if "%1"=="" goto check_prerequisites
if "%1"=="--skip-deps" (
    set skip_deps=true
    shift
    goto parse_args
)
if "%1"=="--verify-only" (
    set verify_only=true
    shift
    goto parse_args
)
if "%1"=="--clean" (
    set clean_setup=true
    shift
    goto parse_args
)
if "%1"=="--help" goto show_help
if "%1"=="-h" goto show_help
echo [ERROR] Unknown option: %1
goto show_help

:show_help
echo Usage: %0 [OPTIONS]
echo.
echo Options:
echo   --skip-deps     Skip platform dependency checks
echo   --verify-only   Only verify existing setup
echo   --clean         Clean existing setup before creating new one
echo   --help, -h      Show this help message
echo.
echo Environment Variables:
echo   DEBUG=true      Enable debug output
exit /b 0

:check_prerequisites
echo [STEP] Checking prerequisites...

REM Clean setup if requested
if "%clean_setup%"=="true" (
    echo [INFO] Cleaning existing setup...
    if exist "%PROJECT_ROOT%\.cross-compilation" (
        rmdir /s /q "%PROJECT_ROOT%\.cross-compilation"
    )
)

REM Verify only mode
if "%verify_only%"=="true" goto verify_setup

REM Check Rust installation
where rustc >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Rust compiler not found. Please install Rust %REQUIRED_RUST_VERSION% or later.
    echo [INFO] Visit https://rustup.rs/ to install Rust
    exit /b 1
)

where cargo >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Cargo not found. Please install Rust toolchain.
    exit /b 1
)

where rustup >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] rustup not found. Please install rustup.
    exit /b 1
)

REM Get Rust version
for /f "tokens=2" %%i in ('rustc --version') do set CURRENT_RUST_VERSION=%%i
echo [INFO] Found Rust version: !CURRENT_RUST_VERSION!

REM Simple version comparison (major.minor only)
for /f "tokens=1,2 delims=." %%a in ("!REQUIRED_RUST_VERSION!") do (
    set required_major=%%a
    set required_minor=%%b
)
for /f "tokens=1,2 delims=." %%a in ("!CURRENT_RUST_VERSION!") do (
    set current_major=%%a
    set current_minor=%%b
)

if !current_major! lss !required_major! (
    echo [WARN] Rust version !CURRENT_RUST_VERSION! is older than required !REQUIRED_RUST_VERSION!
    echo [INFO] Consider updating: rustup update stable
) else if !current_major! equ !required_major! (
    if !current_minor! lss !required_minor! (
        echo [WARN] Rust version !CURRENT_RUST_VERSION! is older than required !REQUIRED_RUST_VERSION!
        echo [INFO] Consider updating: rustup update stable
    )
)

echo [SUCCESS] Prerequisites check passed

REM Install cross-compilation targets
if "%skip_deps%"=="false" goto install_targets
goto setup_environment

:install_targets
echo [STEP] Installing cross-compilation targets...

REM Get installed targets
rustup target list --installed > %TEMP%\installed_targets.txt

set installed_count=0
set skipped_count=0

for /l %%i in (0,1,4) do (
    set target=!targets[%%i]!
    set platform=!platforms[%%i]!
    echo [INFO] Processing target: !target! (platform: !platform!)
    
    REM Check if target is already installed
    findstr /c:"!target!" %TEMP%\installed_targets.txt >nul
    if !ERRORLEVEL! equ 0 (
        echo [DEBUG] Target !target! already installed
        set /a skipped_count+=1
    ) else (
        echo [INFO] Installing target: !target!
        rustup target add !target!
        if !ERRORLEVEL! equ 0 (
            echo [SUCCESS] Installed target: !target!
            set /a installed_count+=1
        ) else (
            echo [ERROR] Failed to install target: !target!
            exit /b 1
        )
    )
)

del %TEMP%\installed_targets.txt

echo [SUCCESS] Cross-compilation targets processed - Installed: !installed_count!, Skipped: !skipped_count!

:setup_environment
echo [STEP] Setting up platform-specific build environment...

REM Create environment configuration directory
set env_dir=%PROJECT_ROOT%\.cross-compilation
if not exist "!env_dir!" mkdir "!env_dir!"

REM Generate environment setup script for Windows
(
echo @echo off
echo REM Cross-compilation environment setup for Windows
echo.
echo REM Export environment variables for consistent builds
echo set RUST_BACKTRACE=1
echo set CARGO_NET_RETRY=10
echo set CARGO_HTTP_TIMEOUT=300
echo set CARGO_HTTP_MULTIPLEXING=false
echo.
echo REM Build reproducibility settings
echo if "%%SOURCE_DATE_EPOCH%%"=="" for /f %%%%i in ('powershell -command "[int][double]::Parse((Get-Date -UFormat %%%%s))"'^) do set SOURCE_DATE_EPOCH=%%%%i
echo set RUSTFLAGS=%%RUSTFLAGS%% -C embed-bitcode=no -C debuginfo=2
echo.
echo REM Target-specific environment variables
echo if "%%1"=="x86_64-unknown-linux-gnu" (
echo     set CC=gcc
echo     set CXX=g++
echo     set AR=ar
echo     set STRIP=strip
echo ^)
echo if "%%1"=="aarch64-unknown-linux-gnu" (
echo     set CC=aarch64-linux-gnu-gcc
echo     set CXX=aarch64-linux-gnu-g++
echo     set AR=aarch64-linux-gnu-ar
echo     set STRIP=aarch64-linux-gnu-strip
echo     set CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER=aarch64-linux-gnu-gcc
echo ^)
echo if "%%1"=="x86_64-pc-windows-gnu" (
echo     set CC=x86_64-w64-mingw32-gcc
echo     set CXX=x86_64-w64-mingw32-g++
echo     set AR=x86_64-w64-mingw32-ar
echo     set STRIP=x86_64-w64-mingw32-strip
echo     set CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER=x86_64-w64-mingw32-gcc
echo ^)
echo if "%%1"=="x86_64-apple-darwin" (
echo     if not "%%OSXCROSS_PATH%%"=="" (
echo         set CC=o64-clang
echo         set CXX=o64-clang++
echo         set AR=x86_64-apple-darwin15-ar
echo         set STRIP=x86_64-apple-darwin15-strip
echo         set CARGO_TARGET_X86_64_APPLE_DARWIN_LINKER=o64-clang
echo     ^)
echo ^)
echo if "%%1"=="aarch64-apple-darwin" (
echo     if not "%%OSXCROSS_PATH%%"=="" (
echo         set CC=oa64-clang
echo         set CXX=oa64-clang++
echo         set AR=aarch64-apple-darwin20-ar
echo         set STRIP=aarch64-apple-darwin20-strip
echo         set CARGO_TARGET_AARCH64_APPLE_DARWIN_LINKER=oa64-clang
echo     ^)
echo ^)
) > "%env_dir%\environment.bat"

echo [SUCCESS] Build environment configuration created at %env_dir%\environment.bat

:setup_cache
echo [STEP] Setting up build cache management...

set cache_dir=%env_dir%\build-cache
if not exist "!cache_dir!" mkdir "!cache_dir!"

REM Create cache management script
(
echo @echo off
echo REM Build cache management for Wasmtime4j cross-compilation
echo.
echo setlocal enabledelayedexpansion
echo.
echo set CACHE_DIR=%%~dp0build-cache
echo.
echo set command=%%1
echo if "%%command%%"=="" set command=info
echo.
echo if "%%command%%"=="info" goto show_info
echo if "%%command%%"=="clean" goto clean_cache
echo if "%%command%%"=="clean-target" goto clean_target
echo if "%%command%%"=="help" goto show_help
echo echo [ERROR] Unknown command: %%command%%
echo goto show_help
echo.
echo :show_info
echo if not exist "%%CACHE_DIR%%" (
echo     echo [INFO] No build cache found
echo     exit /b 0
echo ^)
echo.
echo for /f %%%%i in ('dir "%%CACHE_DIR%%" /s /-c ^| find "File(s)"'^) do set total_size=%%%%i
echo for /f %%%%i in ('dir "%%CACHE_DIR%%" /s /b ^| find /c /v ""'^) do set file_count=%%%%i
echo.
echo echo Build Cache Information:
echo echo   Location: %%CACHE_DIR%%
echo echo   Files: %%file_count%%
echo echo.
echo if %%file_count%% gtr 0 (
echo     echo Cache Contents:
echo     for /d %%%%d in ("%%CACHE_DIR%%\*"^) do (
echo         echo   %%%%~nd: [directory]
echo     ^)
echo ^)
echo exit /b 0
echo.
echo :clean_cache
echo if not exist "%%CACHE_DIR%%" (
echo     echo [INFO] No build cache to clean
echo     exit /b 0
echo ^)
echo.
echo echo [INFO] Cleaning build cache...
echo rmdir /s /q "%%CACHE_DIR%%"
echo mkdir "%%CACHE_DIR%%"
echo echo [SUCCESS] Build cache cleaned
echo exit /b 0
echo.
echo :clean_target
echo set target=%%2
echo if "%%target%%"=="" (
echo     echo [ERROR] Target not specified
echo     exit /b 1
echo ^)
echo.
echo set target_cache=%%CACHE_DIR%%\%%target%%
echo if exist "%%target_cache%%" (
echo     echo [INFO] Cleaning cache for target: %%target%%
echo     rmdir /s /q "%%target_cache%%"
echo     echo [SUCCESS] Cache cleaned for target: %%target%%
echo ^) else (
echo     echo [INFO] No cache found for target: %%target%%
echo ^)
echo exit /b 0
echo.
echo :show_help
echo echo Usage: %%0 [COMMAND] [OPTIONS]
echo echo.
echo echo Commands:
echo echo   info              Show cache information
echo echo   clean             Clean all cache
echo echo   clean-target      Clean cache for specific target
echo echo   help              Show this help
echo echo.
echo echo Options for clean-target:
echo echo   TARGET            Specify target to clean
echo exit /b 0
) > "%env_dir%\manage-cache.bat"

echo [SUCCESS] Build cache management setup completed

:save_targets
echo [STEP] Saving target configuration...

set targets_file=%env_dir%\targets.list

(
echo # Wasmtime4j Cross-Compilation Targets
echo # Generated on: %date% %time%
echo # Host: windows-x64
echo.
for /l %%i in (0,1,4) do (
    echo !targets[%%i]!:!platforms[%%i]!
)
) > "%targets_file%"

echo [SUCCESS] Target configuration saved to %targets_file%

:create_docs
echo [STEP] Creating cross-compilation documentation...

set doc_file=%env_dir%\README.md

(
echo # Cross-Compilation Setup for Wasmtime4j
echo.
echo This directory contains the cross-compilation configuration for building Wasmtime4j native libraries across all supported platforms.
echo.
echo ## Supported Platforms
echo.
echo ^| Target Triple ^| Platform Name ^| Architecture ^| Notes ^|
echo ^|---------------^|---------------^|--------------^|-------^|
for /l %%i in (0,1,4) do (
    set target=!targets[%%i]!
    set platform=!platforms[%%i]!
    echo ^| `!target!` ^| !platform! ^| x86_64/aarch64 ^| ^|
)
echo.
echo ## Environment Setup
echo.
echo The `environment.bat` script configures the build environment for cross-compilation:
echo.
echo ```batch
echo REM Load general cross-compilation environment
echo call .cross-compilation\environment.bat
echo.
echo REM Load target-specific environment
echo call .cross-compilation\environment.bat x86_64-unknown-linux-gnu
echo ```
echo.
echo ## Build Reproducibility
echo.
echo To ensure reproducible builds across different environments:
echo.
echo 1. **Rust Version**: Use Rust %REQUIRED_RUST_VERSION% or compatible version
echo 2. **Environment Variables**: Consistent RUSTFLAGS and build settings
echo 3. **Source Date Epoch**: Fixed timestamp for deterministic builds
echo 4. **Cargo Configuration**: Network and HTTP settings for reliability
echo.
echo ## Usage
echo.
echo ### Setup (one-time^)
echo ```batch
echo scripts\setup-cross-compilation.bat
echo ```
echo.
echo ### Build for specific platform
echo ```batch
echo scripts\build-native.bat compile --target x86_64-unknown-linux-gnu
echo ```
echo.
echo ### Build all platforms
echo ```batch
echo scripts\build-native.bat compile --all-platforms
echo ```
echo.
echo ## Generated Files
echo.
echo - `environment.bat`: Cross-compilation environment configuration
echo - `targets.list`: List of installed targets
echo - `build-cache\`: Cached build artifacts (can be safely deleted^)
echo.
echo ## Last Updated
echo.
echo Generated on: %date% %time%
echo Rust Version: %CURRENT_RUST_VERSION%
echo Host Platform: windows-x64
) > "%doc_file%"

echo [SUCCESS] Documentation created at %doc_file%

:verify_setup
echo [STEP] Verifying cross-compilation setup...

set verification_failed=false

REM Check installed targets
rustup target list --installed > %TEMP%\installed_targets_verify.txt

for /l %%i in (0,1,4) do (
    set target=!targets[%%i]!
    findstr /c:"!target!" %TEMP%\installed_targets_verify.txt >nul
    if !ERRORLEVEL! neq 0 (
        echo [ERROR] Target !target! is not installed
        set verification_failed=true
    ) else (
        echo [DEBUG] Verified target: !target!
    )
)

del %TEMP%\installed_targets_verify.txt

REM Test basic compilation (simplified for Windows)
set test_dir=%PROJECT_ROOT%\.cross-compilation\test
if exist "!test_dir!" rmdir /s /q "!test_dir!"
mkdir "!test_dir!"

(
echo #[no_mangle]
echo pub extern "C" fn test_function(^) -^> i32 {
echo     42
echo }
) > "%test_dir%\lib.rs"

(
echo [package]
echo name = "cross-compile-test"
echo version = "0.1.0"
echo edition = "2021"
echo.
echo [lib]
echo crate-type = ["cdylib"]
) > "%test_dir%\Cargo.toml"

pushd "%test_dir%"

for /l %%i in (0,1,4) do (
    set target=!targets[%%i]!
    set platform=!platforms[%%i]!
    echo [INFO] Testing compilation for !target! (platform: !platform!)
    
    REM Try to compile for this target
    cargo build --target !target! --release --quiet >nul 2>nul
    if !ERRORLEVEL! equ 0 (
        echo [SUCCESS] Compilation test passed for !target!
    ) else (
        echo [WARN] Compilation test failed for !target! (may need additional dependencies)
    )
)

popd

REM Clean up test directory
rmdir /s /q "%test_dir%"

if "%verification_failed%"=="true" (
    echo [ERROR] Setup verification failed - some targets are missing
    exit /b 1
)

echo [SUCCESS] Cross-compilation setup verification completed

:success
echo.
echo [SUCCESS] Cross-compilation setup completed successfully!
echo [INFO] 
echo [INFO] Next steps:
echo [INFO]   1. Review documentation: .cross-compilation\README.md
echo [INFO]   2. Test compilation: scripts\build-native.bat verify
echo [INFO]   3. Build all platforms: scripts\build-native.bat compile --all-platforms

exit /b 0