# Wasmtime4j Developer Tools and Utilities

This guide covers the developer tools, test utilities, and debugging support available in Wasmtime4j to help you build, test, and optimize WebAssembly applications.

## Table of Contents

1. [Test Utilities](#test-utilities)
2. [Benchmarking Tools](#benchmarking-tools)
3. [Debugging Support](#debugging-support)
4. [Development Utilities](#development-utilities)
5. [IDE Integration](#ide-integration)
6. [Performance Analysis](#performance-analysis)
7. [Module Validation](#module-validation)

## Test Utilities

### WasmTestUtils

The `WasmTestUtils` class provides utilities for creating test environments and generating test modules.

```java
import ai.tegmentum.wasmtime4j.test.WasmTestUtils;

public class TestExample {
    @Test
    public void testModuleCompilation() {
        // Create test engine optimized for testing
        Engine testEngine = WasmTestUtils.createTestEngine();

        // Generate simple test module
        byte[] testModule = WasmTestUtils.generateTestModule("add", WasmType.I32, WasmType.I32);

        // Compile and test
        Module module = testEngine.compileModule(testModule);
        assertNotNull(module);
        assertTrue(module.hasExport("add"));
    }

    @Test
    public void testImplementationParity() {
        // Test that JNI and Panama implementations produce identical results
        WasmTestUtils.assertImplementationParity(
            () -> {
                // JNI implementation call
                try (WasmRuntime jniRuntime = WasmRuntimeFactory.createJni()) {
                    return executeTest(jniRuntime);
                }
            },
            () -> {
                // Panama implementation call (if available)
                try (WasmRuntime panamaRuntime = WasmRuntimeFactory.createPanama()) {
                    return executeTest(panamaRuntime);
                }
            }
        );
    }

    private Object[] executeTest(WasmRuntime runtime) throws Exception {
        Engine engine = runtime.createEngine();
        Module module = runtime.compileModule(engine, testWasm);
        Store store = runtime.createStore(engine);
        Instance instance = runtime.instantiate(module);

        WasmFunction func = instance.getFunction("test_function").get();
        return func.call(WasmValue.i32(42));
    }
}
```

### Test Fixtures

Create reusable test fixtures for complex scenarios:

```java
import ai.tegmentum.wasmtime4j.test.TestFixture;
import ai.tegmentum.wasmtime4j.test.TestContext;

public class FixtureExample {
    @Test
    public void testWithFixture() throws Exception {
        TestFixture fixture = TestFixture.builder()
            .withModule("calculator.wasm")
            .withImports(createCalculatorImports())
            .withExpectedExports("add", "subtract", "multiply", "divide")
            .withTimeout(Duration.ofSeconds(30))
            .build();

        fixture.runTest((context) -> {
            Instance instance = context.getInstance();

            // Test addition
            WasmFunction add = instance.getFunction("add").get();
            WasmValue[] result = add.call(WasmValue.i32(10), WasmValue.i32(5));
            assertEquals(15, result[0].asInt());

            // Test division by zero handling
            WasmFunction divide = instance.getFunction("divide").get();
            assertThrows(RuntimeException.class, () -> {
                divide.call(WasmValue.i32(10), WasmValue.i32(0));
            });
        });
    }

    private ImportMap createCalculatorImports() {
        return ImportMap.empty()
            .addFunction("env", "log", (args) -> {
                System.out.println("Calculator log: " + args[0].asInt());
                return new WasmValue[0];
            });
    }
}
```

### Memory Testing Utilities

Test memory operations and detect leaks:

```java
import ai.tegmentum.wasmtime4j.test.MemoryTestUtils;

public class MemoryTestExample {
    @Test
    public void testMemoryOperations() throws Exception {
        try (Engine engine = WasmTestUtils.createTestEngine()) {
            Module module = engine.compileModule(memoryTestWasm);
            Store store = engine.createStore();
            Instance instance = module.instantiate(store);

            WasmMemory memory = instance.getMemory("memory").get();

            // Test memory operations
            MemoryTestUtils.testMemoryOperations(memory, instance);

            // Check for memory leaks
            MemoryTestUtils.assertNoMemoryLeaks(() -> {
                for (int i = 0; i < 1000; i++) {
                    WasmFunction allocate = instance.getFunction("allocate").get();
                    allocate.call(WasmValue.i32(1024));
                }
            });
        }
    }

    @Test
    public void testLargeMemoryAllocation() throws Exception {
        // Test behavior with large memory allocations
        MemoryTestUtils.withMemoryPressure(16L * 1024 * 1024 * 1024, () -> {
            // Test code that should handle memory pressure gracefully
            Engine engine = WasmTestUtils.createTestEngine();
            // ... test operations
        });
    }
}
```

## Benchmarking Tools

### Built-in Benchmarks

Run comprehensive benchmarks to compare performance:

```java
import ai.tegmentum.wasmtime4j.benchmark.BenchmarkSuite;
import ai.tegmentum.wasmtime4j.benchmark.BenchmarkResult;

public class BenchmarkExample {
    public static void main(String[] args) throws Exception {
        BenchmarkSuite suite = BenchmarkSuite.create();

        // Run built-in benchmarks
        BenchmarkResult engineCreation = suite.runBenchmark("engine_creation");
        BenchmarkResult moduleCompilation = suite.runBenchmark("module_compilation");
        BenchmarkResult functionCalls = suite.runBenchmark("function_calls");

        System.out.println("Engine creation: " + engineCreation.getAverageTime());
        System.out.println("Module compilation: " + moduleCompilation.getAverageTime());
        System.out.println("Function calls: " + functionCalls.getAverageTime());

        // Compare JNI vs Panama implementations
        BenchmarkResult comparison = suite.runComparison(
            "JNI vs Panama Function Calls",
            () -> benchmarkJniCall(),
            () -> benchmarkPanamaCall()
        );

        System.out.println("JNI time: " + comparison.getTime1());
        System.out.println("Panama time: " + comparison.getTime2());
        System.out.println("Speedup: " + comparison.getSpeedup());
    }

    private static void benchmarkJniCall() throws Exception {
        try (WasmRuntime runtime = WasmRuntimeFactory.createJni()) {
            // Benchmark JNI implementation
            performFunctionCalls(runtime);
        }
    }

    private static void benchmarkPanamaCall() throws Exception {
        try (WasmRuntime runtime = WasmRuntimeFactory.createPanama()) {
            // Benchmark Panama implementation
            performFunctionCalls(runtime);
        }
    }

    private static void performFunctionCalls(WasmRuntime runtime) throws Exception {
        Engine engine = runtime.createEngine();
        Module module = runtime.compileModule(engine, benchmarkWasm);
        Store store = runtime.createStore(engine);
        Instance instance = runtime.instantiate(module);

        WasmFunction func = instance.getFunction("compute").get();
        for (int i = 0; i < 10000; i++) {
            func.call(WasmValue.i32(i));
        }
    }
}
```

### Custom Benchmarks

Create custom benchmarks for your specific use cases:

```java
import ai.tegmentum.wasmtime4j.benchmark.CustomBenchmark;
import ai.tegmentum.wasmtime4j.benchmark.BenchmarkConfig;

public class CustomBenchmarkExample {
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
    public static class MyBenchmark {

        @Param({"small.wasm", "medium.wasm", "large.wasm"})
        private String moduleFile;

        @Param({"JNI", "Panama"})
        private String runtime;

        private WasmRuntime wasmRuntime;
        private Engine engine;
        private Module module;

        @Setup(Level.Trial)
        public void setupTrial() throws Exception {
            wasmRuntime = "JNI".equals(runtime)
                ? WasmRuntimeFactory.createJni()
                : WasmRuntimeFactory.createPanama();

            engine = wasmRuntime.createEngine(EngineConfig.forSpeed());
            byte[] wasmBytes = loadModuleFile(moduleFile);
            module = wasmRuntime.compileModule(engine, wasmBytes);
        }

        @TearDown(Level.Trial)
        public void teardownTrial() throws Exception {
            if (wasmRuntime != null) {
                wasmRuntime.close();
            }
        }

        @Benchmark
        public WasmValue[] benchmarkFunctionCall() throws Exception {
            Store store = wasmRuntime.createStore(engine);
            Instance instance = wasmRuntime.instantiate(module);

            WasmFunction func = instance.getFunction("benchmark_function").get();
            return func.call(WasmValue.i32(1000));
        }

        private byte[] loadModuleFile(String filename) throws IOException {
            return Files.readAllBytes(Paths.get("benchmarks", filename));
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(MyBenchmark.class.getSimpleName())
            .forks(1)
            .build();

        new Runner(opt).run();
    }
}
```

## Debugging Support

### Debug Information

Enable debug information for better error messages and stack traces:

```java
public class DebugExample {
    public static void main(String[] args) throws Exception {
        // Create engine with debug information enabled
        EngineConfig debugConfig = new EngineConfig()
            .debugInfo(true)
            .detailedDiagnostics(true)
            .stackTraces(true);

        try (Engine engine = Engine.create(debugConfig)) {
            try {
                Module module = engine.compileModule(invalidWasm);
            } catch (CompilationException e) {
                // Get detailed compilation diagnostics
                ErrorDiagnostics diagnostics = e.getDiagnostics();

                System.err.println("Compilation failed:");
                for (ValidationIssue issue : diagnostics.getValidationIssues()) {
                    System.err.printf("  %s at %s: %s%n",
                        issue.getSeverity(),
                        issue.getLocation(),
                        issue.getDescription()
                    );

                    // Get suggested fixes
                    for (SuggestedFix fix : issue.getSuggestedFixes()) {
                        System.err.println("    Suggestion: " + fix.getDescription());
                    }
                }
            }

            try {
                // Runtime error with stack trace
                WasmFunction func = instance.getFunction("error_function").get();
                func.call();
            } catch (RuntimeException e) {
                // Get WebAssembly stack trace
                WasmStackTrace stackTrace = e.getWasmStackTrace();
                System.err.println("WebAssembly stack trace:");

                for (StackFrame frame : stackTrace.getFrames()) {
                    System.err.printf("  at %s (%s:%d)%n",
                        frame.getFunctionName(),
                        frame.getSourceLocation().getFilename(),
                        frame.getSourceLocation().getLineNumber()
                    );
                }
            }
        }
    }
}
```

### Interactive Debugger

Use the interactive debugger for step-by-step execution:

```java
import ai.tegmentum.wasmtime4j.debug.WasmDebugger;
import ai.tegmentum.wasmtime4j.debug.Breakpoint;
import ai.tegmentum.wasmtime4j.debug.DebugSession;

public class DebuggerExample {
    public static void main(String[] args) throws Exception {
        // Create debugger-enabled engine
        EngineConfig debugConfig = new EngineConfig()
            .debugInfo(true)
            .enableDebugger(true);

        try (Engine engine = Engine.create(debugConfig)) {
            Module module = engine.compileModule(debugWasm);
            Store store = engine.createStore();

            // Create debug session
            WasmDebugger debugger = WasmDebugger.create();
            DebugSession session = debugger.createSession(store, module);

            // Set breakpoints
            session.setBreakpoint("main", 5);
            session.setBreakpoint("compute", 1);

            // Set up debug event handlers
            session.onBreakpoint((breakpoint, context) -> {
                System.out.println("Hit breakpoint at " + breakpoint.getLocation());
                System.out.println("Local variables:");

                for (VariableInfo var : context.getLocalVariables()) {
                    System.out.println("  " + var.getName() + " = " + var.getValue());
                }

                // Interactive commands
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    System.out.print("(debug) ");
                    String command = scanner.nextLine().trim();

                    switch (command) {
                        case "continue":
                        case "c":
                            return; // Continue execution

                        case "step":
                        case "s":
                            context.stepOver();
                            break;

                        case "next":
                        case "n":
                            context.stepInto();
                            break;

                        case "print locals":
                        case "locals":
                            for (VariableInfo var : context.getLocalVariables()) {
                                System.out.println("  " + var.getName() + " = " + var.getValue());
                            }
                            break;

                        case "stack":
                            WasmStackTrace stack = context.getStackTrace();
                            for (StackFrame frame : stack.getFrames()) {
                                System.out.println("  " + frame.getFunctionName() + " at " + frame.getSourceLocation());
                            }
                            break;

                        default:
                            System.out.println("Unknown command: " + command);
                            System.out.println("Available commands: continue, step, next, locals, stack");
                    }
                }
            });

            // Start debugging
            Instance instance = session.instantiate();
            WasmFunction main = instance.getFunction("main").get();
            main.call();
        }
    }
}
```

## Development Utilities

### Module Analysis

Analyze WebAssembly modules to understand their structure:

```java
import ai.tegmentum.wasmtime4j.tools.ModuleAnalyzer;
import ai.tegmentum.wasmtime4j.tools.AnalysisReport;

public class ModuleAnalysisExample {
    public static void main(String[] args) throws Exception {
        byte[] wasmBytes = Files.readAllBytes(Paths.get("complex-module.wasm"));

        try (Engine engine = Engine.create()) {
            Module module = engine.compileModule(wasmBytes);

            // Analyze module structure
            ModuleAnalyzer analyzer = ModuleAnalyzer.create();
            AnalysisReport report = analyzer.analyze(module);

            // Print analysis results
            System.out.println("Module Analysis Report");
            System.out.println("=====================");
            System.out.println("Size: " + report.getModuleSize() + " bytes");
            System.out.println("Functions: " + report.getFunctionCount());
            System.out.println("Imports: " + report.getImportCount());
            System.out.println("Exports: " + report.getExportCount());
            System.out.println("Memory pages: " + report.getMemoryPages());

            // Function analysis
            System.out.println("\nFunctions:");
            for (FunctionInfo func : report.getFunctions()) {
                System.out.printf("  %s: %d parameters, %d results%n",
                    func.getName(),
                    func.getParameterCount(),
                    func.getResultCount()
                );

                if (func.isComplexFunction()) {
                    System.out.println("    (complex function - consider optimization)");
                }
            }

            // Import analysis
            System.out.println("\nImports:");
            for (ImportInfo imp : report.getImports()) {
                System.out.printf("  %s.%s (%s)%n",
                    imp.getModule(),
                    imp.getName(),
                    imp.getType()
                );
            }

            // Security analysis
            SecurityAnalysisReport security = analyzer.analyzeSecurityImplications(module);
            if (security.hasSecurityConcerns()) {
                System.out.println("\nSecurity Concerns:");
                for (SecurityConcern concern : security.getConcerns()) {
                    System.out.printf("  %s: %s%n",
                        concern.getSeverity(),
                        concern.getDescription()
                    );
                }
            }

            // Performance suggestions
            PerformanceAnalysisReport performance = analyzer.analyzePerformance(module);
            if (performance.hasOptimizationSuggestions()) {
                System.out.println("\nOptimization Suggestions:");
                for (OptimizationSuggestion suggestion : performance.getSuggestions()) {
                    System.out.printf("  %s: %s%n",
                        suggestion.getCategory(),
                        suggestion.getDescription()
                    );
                }
            }
        }
    }
}
```

### Code Generation

Generate boilerplate code for common patterns:

```java
import ai.tegmentum.wasmtime4j.tools.CodeGenerator;
import ai.tegmentum.wasmtime4j.tools.BindingGenerator;

public class CodeGenerationExample {
    public static void main(String[] args) throws Exception {
        byte[] wasmBytes = Files.readAllBytes(Paths.get("api-module.wasm"));

        try (Engine engine = Engine.create()) {
            Module module = engine.compileModule(wasmBytes);

            // Generate Java bindings for exported functions
            BindingGenerator generator = BindingGenerator.create();
            String javaCode = generator.generateJavaBindings(module, "com.example.wasm");

            // Write generated code to file
            Files.write(Paths.get("generated", "WasmApi.java"), javaCode.getBytes());

            System.out.println("Generated Java bindings:");
            System.out.println(javaCode);

            // Generate test code
            CodeGenerator testGenerator = CodeGenerator.forTesting();
            String testCode = testGenerator.generateTestClass(module, "com.example.wasm.WasmApiTest");

            Files.write(Paths.get("generated", "WasmApiTest.java"), testCode.getBytes());

            // Generate documentation
            CodeGenerator docGenerator = CodeGenerator.forDocumentation();
            String documentation = docGenerator.generateMarkdownDocs(module);

            Files.write(Paths.get("generated", "API.md"), documentation.getBytes());
        }
    }
}
```

### Configuration Wizard

Interactive configuration helper:

```java
import ai.tegmentum.wasmtime4j.tools.ConfigurationWizard;

public class ConfigurationWizardExample {
    public static void main(String[] args) {
        ConfigurationWizard wizard = ConfigurationWizard.create();

        // Interactive configuration
        EngineConfig config = wizard.interactiveConfiguration();

        // Or programmatic configuration based on use case
        EngineConfig webConfig = wizard.configureForWebService();
        EngineConfig dataConfig = wizard.configureForDataProcessing();
        EngineConfig embeddedConfig = wizard.configureForEmbeddedSystem();

        System.out.println("Recommended web service configuration:");
        System.out.println("  Optimization level: " + webConfig.getOptimizationLevel());
        System.out.println("  Parallel compilation: " + webConfig.isParallelCompilation());
        System.out.println("  Memory limit: " + webConfig.getMaxMemorySize());

        // Save configuration for reuse
        wizard.saveConfiguration(config, "my-app-config.json");

        // Load saved configuration
        EngineConfig savedConfig = wizard.loadConfiguration("my-app-config.json");
    }
}
```

## IDE Integration

### IntelliJ IDEA Plugin

Features provided by the IntelliJ IDEA plugin:

- **Syntax highlighting** for WebAssembly Text Format (WAT)
- **Code completion** for Wasmtime4j APIs
- **Debugging support** with breakpoints in WebAssembly code
- **Performance profiling** integration
- **Module validation** on-the-fly

Installation:
1. Open IntelliJ IDEA
2. Go to Settings → Plugins
3. Search for "Wasmtime4j"
4. Install the plugin
5. Restart IDE

Usage:
```java
// The plugin provides:
// - Auto-completion for Wasmtime4j classes and methods
// - Quick fixes for common issues
// - Code templates for common patterns
// - Integration with the debugger
// - Performance metrics display
```

### VS Code Extension

Features provided by the VS Code extension:

- **Language server** for WebAssembly and Wasmtime4j
- **Integrated terminal** for running WebAssembly modules
- **Debug adapter** for step-by-step debugging
- **Code snippets** for common operations

Installation:
```bash
code --install-extension wasmtime4j.vscode-wasmtime4j
```

Configuration (`.vscode/settings.json`):
```json
{
    "wasmtime4j.runtime.preferredImplementation": "auto",
    "wasmtime4j.debug.enableSourceMaps": true,
    "wasmtime4j.performance.enableProfiling": true,
    "wasmtime4j.validation.enableLinting": true
}
```

## Performance Analysis

### Profiling Tools

Built-in profiling for performance optimization:

```java
import ai.tegmentum.wasmtime4j.profiler.WasmProfiler;
import ai.tegmentum.wasmtime4j.profiler.ProfilingReport;

public class ProfilingExample {
    public static void main(String[] args) throws Exception {
        try (Engine engine = Engine.create()) {
            Module module = engine.compileModule(wasmBytes);
            Store store = engine.createStore();
            Instance instance = module.instantiate(store);

            // Create profiler
            WasmProfiler profiler = WasmProfiler.create();

            // Configure profiling
            ProfilingOptions options = ProfilingOptions.builder()
                .enableFunctionProfiling(true)
                .enableMemoryProfiling(true)
                .enableInstructionCounting(true)
                .samplingInterval(Duration.ofMicroseconds(100))
                .build();

            // Start profiling
            profiler.startProfiling(instance, options);

            // Run the code to be profiled
            WasmFunction computeFunction = instance.getFunction("compute").get();
            for (int i = 0; i < 10000; i++) {
                computeFunction.call(WasmValue.i32(i));
            }

            // Stop profiling and get report
            ProfilingReport report = profiler.stopProfiling();

            // Analyze results
            System.out.println("Profiling Report");
            System.out.println("================");

            // Function-level analysis
            for (FunctionProfile funcProfile : report.getFunctionProfiles()) {
                System.out.printf("Function: %s%n", funcProfile.getFunctionName());
                System.out.printf("  Calls: %d%n", funcProfile.getCallCount());
                System.out.printf("  Total time: %s%n", funcProfile.getTotalTime());
                System.out.printf("  Average time: %s%n", funcProfile.getAverageTime());
                System.out.printf("  Instructions: %d%n", funcProfile.getInstructionCount());

                if (funcProfile.isHotSpot()) {
                    System.out.println("  ** HOT SPOT - Consider optimization **");
                }
            }

            // Memory analysis
            MemoryProfile memProfile = report.getMemoryProfile();
            System.out.printf("Memory usage:%n");
            System.out.printf("  Peak: %d bytes%n", memProfile.getPeakUsage());
            System.out.printf("  Average: %d bytes%n", memProfile.getAverageUsage());
            System.out.printf("  Allocations: %d%n", memProfile.getAllocationCount());

            // Generate optimization recommendations
            List<OptimizationRecommendation> recommendations = report.getOptimizationRecommendations();
            if (!recommendations.isEmpty()) {
                System.out.println("\nOptimization Recommendations:");
                for (OptimizationRecommendation rec : recommendations) {
                    System.out.printf("  %s: %s%n", rec.getPriority(), rec.getDescription());
                }
            }

            // Export detailed report
            profiler.exportReport(report, "profile-report.json");
            profiler.exportFlameGraph(report, "flamegraph.svg");
        }
    }
}
```

### Memory Analysis

Detailed memory usage analysis:

```java
import ai.tegmentum.wasmtime4j.memory.MemoryAnalyzer;
import ai.tegmentum.wasmtime4j.memory.MemoryReport;

public class MemoryAnalysisExample {
    public static void main(String[] args) throws Exception {
        try (Engine engine = Engine.create()) {
            Module module = engine.compileModule(wasmBytes);
            Store store = engine.createStore();
            Instance instance = module.instantiate(store);

            WasmMemory memory = instance.getMemory("memory").get();

            // Create memory analyzer
            MemoryAnalyzer analyzer = MemoryAnalyzer.create();

            // Analyze memory layout
            MemoryReport report = analyzer.analyzeMemory(memory);

            System.out.println("Memory Analysis Report");
            System.out.println("======================");
            System.out.printf("Total size: %d bytes (%d pages)%n",
                report.getTotalSize(), report.getPageCount());
            System.out.printf("Used memory: %d bytes (%.1f%%)%n",
                report.getUsedMemory(),
                report.getUsagePercentage() * 100);

            // Memory regions
            for (MemoryRegion region : report.getMemoryRegions()) {
                System.out.printf("Region %s: %d-%d (%d bytes) - %s%n",
                    region.getName(),
                    region.getStartAddress(),
                    region.getEndAddress(),
                    region.getSize(),
                    region.getType()
                );
            }

            // Memory fragmentation analysis
            FragmentationReport fragReport = analyzer.analyzeFragmentation(memory);
            System.out.printf("Fragmentation: %.1f%%\n",
                fragReport.getFragmentationPercentage() * 100);

            if (fragReport.isHighlyFragmented()) {
                System.out.println("Warning: High memory fragmentation detected");
                for (String suggestion : fragReport.getDefragmentationSuggestions()) {
                    System.out.println("  Suggestion: " + suggestion);
                }
            }

            // Memory leak detection
            LeakDetectionReport leakReport = analyzer.detectLeaks(memory, Duration.ofMinutes(1));
            if (leakReport.hasLeaks()) {
                System.out.println("Memory leaks detected:");
                for (MemoryLeak leak : leakReport.getLeaks()) {
                    System.out.printf("  Leak at %d: %d bytes (allocated %s ago)%n",
                        leak.getAddress(),
                        leak.getSize(),
                        leak.getAge()
                    );
                }
            }
        }
    }
}
```

## Module Validation

### Comprehensive Validation

Validate WebAssembly modules before deployment:

```java
import ai.tegmentum.wasmtime4j.validation.ModuleValidator;
import ai.tegmentum.wasmtime4j.validation.ValidationReport;

public class ModuleValidationExample {
    public static void main(String[] args) throws Exception {
        byte[] wasmBytes = Files.readAllBytes(Paths.get("module-to-validate.wasm"));

        // Create validator with strict rules
        ModuleValidator validator = ModuleValidator.builder()
            .strictMode(true)
            .checkSecurityImplications(true)
            .validatePerformance(true)
            .checkCompatibility(true)
            .build();

        // Validate module
        ValidationReport report = validator.validate(wasmBytes);

        System.out.println("Module Validation Report");
        System.out.println("========================");
        System.out.println("Valid: " + report.isValid());

        if (!report.isValid()) {
            System.out.println("Validation Errors:");
            for (ValidationError error : report.getErrors()) {
                System.out.printf("  %s at %s: %s%n",
                    error.getSeverity(),
                    error.getLocation(),
                    error.getMessage()
                );
            }
        }

        // Security analysis
        SecurityValidationReport securityReport = report.getSecurityReport();
        if (securityReport.hasSecurityIssues()) {
            System.out.println("Security Issues:");
            for (SecurityIssue issue : securityReport.getIssues()) {
                System.out.printf("  %s: %s%n",
                    issue.getSeverity(),
                    issue.getDescription()
                );
            }
        }

        // Performance analysis
        PerformanceValidationReport perfReport = report.getPerformanceReport();
        if (perfReport.hasPerformanceIssues()) {
            System.out.println("Performance Issues:");
            for (PerformanceIssue issue : perfReport.getIssues()) {
                System.out.printf("  %s: %s%n",
                    issue.getImpact(),
                    issue.getDescription()
                );
            }
        }

        // Compatibility check
        CompatibilityReport compatReport = report.getCompatibilityReport();
        System.out.println("Compatibility:");
        System.out.println("  JNI runtime: " + compatReport.isJniCompatible());
        System.out.println("  Panama runtime: " + compatReport.isPanamaCompatible());
        System.out.println("  WASI support: " + compatReport.isWasiCompatible());

        // Generate detailed report
        validator.generateDetailedReport(report, "validation-report.html");
    }
}
```

These developer tools provide comprehensive support for building, testing, debugging, and optimizing WebAssembly applications with Wasmtime4j. Use them throughout your development workflow to ensure high-quality, performant applications.