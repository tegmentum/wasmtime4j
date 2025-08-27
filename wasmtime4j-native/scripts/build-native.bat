@echo off
REM Wasmtime4j Native Library Build Script for Windows
REM This script handles cross-compilation of the native Rust library

setlocal enabledelayedexpansion

set PROJECT_ROOT=%~dp0\..
set SCRIPT_DIR=%PROJECT_ROOT%\scripts
set NATIVE_DIR=%PROJECT_ROOT%\src\main\resources\natives

REM Configuration
set WASMTIME_VERSION=36.0.2
set RUST_VERSION=1.75.0

echo [INFO] Wasmtime4j Native Library Build Script
echo [INFO] Project Root: %PROJECT_ROOT%

REM Check if native compilation should be skipped
if "%WASMTIME4J_SKIP_NATIVE%"=="true" (
    echo [WARN] Native compilation skipped (WASMTIME4J_SKIP_NATIVE=true)
    exit /b 0
)

REM Check prerequisites
echo [INFO] Checking prerequisites...

where rustc >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Rust compiler not found. Please install Rust %RUST_VERSION% or later.
    exit /b 1
)

where cargo >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Cargo not found. Please install Rust toolchain.
    exit /b 1
)

REM Get Rust version
for /f "tokens=2" %%i in ('rustc --version') do set CURRENT_RUST_VERSION=%%i
echo [INFO] Found Rust version: %CURRENT_RUST_VERSION%

echo [SUCCESS] Prerequisites check passed

REM For now, this is a placeholder that creates empty placeholder files
echo [WARN] Native compilation is currently a placeholder
echo [INFO] Creating placeholder native libraries...

REM Create placeholder libraries for testing
set platforms[0]=linux-x64
set platforms[1]=linux-aarch64
set platforms[2]=windows-x64
set platforms[3]=macos-x64
set platforms[4]=macos-aarch64

set extensions[0]=so
set extensions[1]=so
set extensions[2]=dll
set extensions[3]=dylib
set extensions[4]=dylib

set prefixes[0]=lib
set prefixes[1]=lib
set prefixes[2]=
set prefixes[3]=lib
set prefixes[4]=lib

for /l %%i in (0,1,4) do (
    set platform=!platforms[%%i]!
    set extension=!extensions[%%i]!
    set prefix=!prefixes[%%i]!
    set output_dir=%NATIVE_DIR%\!platform!
    set lib_file=!output_dir!\!prefix!wasmtime4j.!extension!
    
    if not exist "!output_dir!" mkdir "!output_dir!"
    echo # Placeholder native library for !platform! > "!lib_file!"
    echo [INFO] Created placeholder: !lib_file!
)

echo [SUCCESS] Native library build process completed
exit /b 0