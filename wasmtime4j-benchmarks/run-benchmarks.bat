@echo off
setlocal enabledelayedexpansion

REM Wasmtime4j Benchmarks Execution Script for Windows
REM Usage: run-benchmarks.bat [category] [profile] [additional-args...]

REM Default values
set "DEFAULT_CATEGORY=all"
set "DEFAULT_PROFILE=standard"
set "OUTPUT_DIR=benchmark-results"

REM Get timestamp
for /f "tokens=2-4 delims=/ " %%i in ('date /t') do set "DATE_PART=%%k%%i%%j"
for /f "tokens=1-2 delims=: " %%i in ('time /t') do set "TIME_PART=%%i%%j"
set "TIME_PART=%TIME_PART: =0%"
set "TIMESTAMP=%DATE_PART%_%TIME_PART%"

REM Function to show usage
if "%1"=="--help" goto :show_usage
if "%1"=="-h" goto :show_usage
if "%1"=="/?" goto :show_usage

echo Wasmtime4j Benchmarks Runner
echo ================================

REM Check prerequisites
echo [INFO] Checking prerequisites...

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed or not in PATH
    exit /b 1
)

REM Get Java version
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr "version"') do set "JAVA_VERSION_STRING=%%i"
set "JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%"

echo [INFO] Java version: %JAVA_VERSION_STRING%

REM Check if Maven wrapper is available
if exist "..\mvnw.cmd" (
    echo [INFO] Maven wrapper is available for building
    set "MVN_CMD=..\mvnw.cmd"
) else if exist "mvn.exe" (
    echo [INFO] Maven is available for building
    set "MVN_CMD=mvn"
) else (
    echo [WARNING] Neither Maven nor Maven wrapper found - assuming JAR is already built
    set "MVN_CMD="
)

REM Detect available runtimes
echo [INFO] Detecting available runtimes...
REM Extract major version number (simplified for Windows batch)
echo Available runtimes: JNI
for /f "tokens=1 delims=." %%i in ("%JAVA_VERSION_STRING%") do (
    if %%i GEQ 23 (
        echo [INFO] Java 23+ detected - Panama Foreign Function API available
        echo Available runtimes: JNI, Panama
    ) else (
        echo [INFO] Java %%i detected - Only JNI runtime available
        echo Available runtimes: JNI
    )
)

REM Create output directory if it doesn't exist
if not exist "%OUTPUT_DIR%" (
    mkdir "%OUTPUT_DIR%"
    echo [INFO] Created output directory: %OUTPUT_DIR%
)

REM Build benchmarks if needed
set "BENCHMARK_JAR=target\wasmtime4j-benchmarks.jar"

if not exist "%BENCHMARK_JAR%" (
    echo [INFO] Benchmark JAR not found, attempting to build...
    
    if defined MVN_CMD (
        echo [INFO] Building benchmarks...
        cd ..
        %MVN_CMD% clean package -pl wasmtime4j-benchmarks -am -DskipTests
        cd wasmtime4j-benchmarks
        
        if not exist "%BENCHMARK_JAR%" (
            echo [ERROR] Build failed - benchmark JAR not created
            exit /b 1
        )
        
        echo [SUCCESS] Benchmarks built successfully
    ) else (
        echo [ERROR] Cannot build benchmarks - no Maven found and JAR doesn't exist
        exit /b 1
    )
) else (
    echo [INFO] Using existing benchmark JAR: %BENCHMARK_JAR%
)

REM Generate system information
set "INFO_FILE=%OUTPUT_DIR%\system_info_%TIMESTAMP%.txt"

echo [INFO] Generating system information...

(
    echo System Information - Generated %DATE% %TIME%
    echo ================================================
    echo.
    echo OS Information:
    systeminfo | findstr /C:"OS Name" /C:"OS Version" /C:"System Type"
    echo.
    echo Java Information:
    java -version
    echo.
    echo Environment Variables:
    set | findstr /I "JAVA"
    echo.
) > "%INFO_FILE%"

echo [INFO] System information saved to: %INFO_FILE%

REM Parse arguments
set "CATEGORY=%1"
set "PROFILE=%2"

if "%CATEGORY%"=="" set "CATEGORY=%DEFAULT_CATEGORY%"
if "%PROFILE%"=="" set "PROFILE=%DEFAULT_PROFILE%"

REM Shift arguments to get additional parameters
set "ADDITIONAL_ARGS="
set "ARG_COUNT=0"
for %%i in (%*) do (
    set /a ARG_COUNT+=1
    if !ARG_COUNT! GTR 2 (
        set "ADDITIONAL_ARGS=!ADDITIONAL_ARGS! %%i"
    )
)

REM Set up output files
set "OUTPUT_FILE=%OUTPUT_DIR%\benchmark_%CATEGORY%_%PROFILE%_%TIMESTAMP%.json"
set "LOG_FILE=%OUTPUT_DIR%\benchmark_%CATEGORY%_%PROFILE%_%TIMESTAMP%.log"

echo.
echo [INFO] Starting benchmarks...
echo [INFO] Category: %CATEGORY%
echo [INFO] Profile: %PROFILE%
echo [INFO] Output file: %OUTPUT_FILE%
echo [INFO] Log file: %LOG_FILE%

REM Prepare and run the benchmark command
set "JAVA_CMD=java -cp %BENCHMARK_JAR% ai.tegmentum.wasmtime4j.benchmarks.BenchmarkRunner"
set "BENCHMARK_ARGS=%CATEGORY% --profile %PROFILE% --output %OUTPUT_FILE% %ADDITIONAL_ARGS%"

echo [INFO] Executing: %JAVA_CMD% %BENCHMARK_ARGS%
echo.

REM Run the benchmarks
%JAVA_CMD% %BENCHMARK_ARGS% 2>&1 | tee "%LOG_FILE%"

if errorlevel 1 (
    echo [ERROR] Benchmarks failed! Check %LOG_FILE% for details.
    exit /b 1
) else (
    echo [SUCCESS] Benchmarks completed successfully!
    echo [INFO] Results saved to: %OUTPUT_FILE%
    echo [INFO] Log saved to: %LOG_FILE%
    
    REM Show summary if available
    set "SUMMARY_FILE=%OUTPUT_FILE:.json=_summary.txt%"
    if exist "%SUMMARY_FILE%" (
        echo.
        echo [INFO] Summary:
        type "%SUMMARY_FILE%"
    )
)

echo.
echo [SUCCESS] All operations completed successfully!
goto :eof

:show_usage
echo Wasmtime4j Benchmarks Runner
echo.
echo Usage: %~nx0 [category] [profile] [additional-args...]
echo.
echo Categories:
echo   all         - Run all benchmarks (default)
echo   runtime     - Runtime initialization benchmarks
echo   module      - Module operation benchmarks
echo   function    - Function execution benchmarks
echo   memory      - Memory operation benchmarks
echo   comparison  - JNI vs Panama comparison benchmarks
echo.
echo Profiles:
echo   quick       - Fast benchmarks for development (1 iteration, 1 warmup, 1 fork)
echo   standard    - Standard benchmarks (5 iterations, 3 warmup, 2 forks) [default]
echo   production  - Production benchmarks (10 iterations, 5 warmup, 3 forks)
echo   comprehensive - Comprehensive benchmarks (15 iterations, 8 warmup, 5 forks)
echo.
echo Additional arguments will be passed directly to the benchmark runner.
echo.
echo Examples:
echo   %~nx0                                    # Run all benchmarks with standard profile
echo   %~nx0 runtime quick                     # Run runtime benchmarks with quick profile
echo   %~nx0 comparison production --output results.json
echo   %~nx0 all comprehensive --iterations 20
goto :eof