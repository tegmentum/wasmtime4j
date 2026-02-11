package examples;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.wasi.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;

/**
 * WASI file processing example demonstrating:
 * - WASI configuration and setup
 * - File system access from WebAssembly
 * - Environment variable handling
 * - Command line argument passing
 * - Standard I/O redirection
 */
public class WasiFileProcessor {

    private final WasmRuntime runtime;
    private final Engine engine;

    public WasiFileProcessor() throws WasmException {
        this.runtime = WasmRuntime.builder()
            .enableMetrics(true)
            .build();

        EngineConfig config = EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.SPEED)
            .enableWasi(true)
            .build();

        this.engine = runtime.createEngine(config);

        System.out.println("WASI File Processor initialized");
        System.out.println("Runtime: " + runtime.getRuntimeType());
    }

    /**
     * Demonstrates basic file processing with WASI
     */
    public void processTextFile(String inputFile, String outputFile) throws Exception {
        System.out.println("\n=== Processing Text File ===");
        System.out.println("Input: " + inputFile);
        System.out.println("Output: " + outputFile);

        // Create sample input file
        createSampleTextFile(inputFile);

        try (Store store = engine.createStore()) {
            // Configure WASI with file system access
            WasiConfig wasiConfig = WasiConfig.builder()
                .args("text-processor", "--input", inputFile, "--output", outputFile)
                .env("PROCESSOR_MODE", "uppercase")
                .env("LOG_LEVEL", "INFO")
                .preOpenDir(Paths.get("."), "/workspace")
                .inheritStdout()
                .inheritStderr()
                .build();

            WasiContext wasi = WasiContext.create(store, wasiConfig);

            // Load WASI-enabled WebAssembly module
            byte[] wasmBytes = loadTextProcessorModule();
            Module module = Module.fromBytes(engine, wasmBytes);

            // Create linker for WASI imports
            Linker linker = Linker.create(engine);
            wasi.addToLinker(linker);

            // Instantiate module with WASI
            Instance instance = linker.instantiate(store, module);

            // Call the main function
            Function mainFunc = instance.getFunction("_start");
            if (mainFunc != null) {
                mainFunc.call();
                System.out.println("Text processing completed successfully");
            } else {
                System.out.println("No _start function found, calling process_file directly");
                callProcessFileFunction(instance, inputFile, outputFile);
            }

            // Verify output
            verifyProcessedFile(outputFile);
        }
    }

    /**
     * Demonstrates directory traversal and bulk file processing
     */
    public void processDirectory(String inputDir, String outputDir) throws Exception {
        System.out.println("\n=== Processing Directory ===");
        System.out.println("Input directory: " + inputDir);
        System.out.println("Output directory: " + outputDir);

        // Create sample directory structure
        createSampleDirectory(inputDir);
        Files.createDirectories(Paths.get(outputDir));

        try (Store store = engine.createStore()) {
            WasiConfig wasiConfig = WasiConfig.builder()
                .args("batch-processor", "--input-dir", inputDir, "--output-dir", outputDir)
                .env("BATCH_SIZE", "10")
                .env("PARALLEL_PROCESSING", "true")
                .preOpenDir(Paths.get(inputDir), "/input")
                .preOpenDir(Paths.get(outputDir), "/output")
                .inheritStdio()
                .build();

            WasiContext wasi = WasiContext.create(store, wasiConfig);

            Module module = Module.fromBytes(engine, loadBatchProcessorModule());

            Linker linker = Linker.create(engine);
            wasi.addToLinker(linker);

            Instance instance = linker.instantiate(store, module);

            // Process directory
            Function processDir = instance.getFunction("process_directory");
            if (processDir != null) {
                processDir.call();
            } else {
                System.out.println("Using _start function for directory processing");
                Function start = instance.getFunction("_start");
                start.call();
            }

            System.out.println("Directory processing completed");
        }
    }

    /**
     * Demonstrates secure file processing with restricted permissions
     */
    public void secureFileProcessing(String inputFile) throws Exception {
        System.out.println("\n=== Secure File Processing ===");

        createSampleTextFile(inputFile);

        try (Store store = engine.createStore()) {
            // Restricted WASI configuration
            WasiConfig wasiConfig = WasiConfig.builder()
                .args("secure-processor", inputFile)
                // No environment variables
                .preOpenDir(Paths.get(".").resolve("allowed"), "/allowed") // Only allowed directory
                // No stdout/stderr inheritance for security
                .build();

            WasiContext wasi = WasiContext.create(store, wasiConfig);

            // Set resource limits
            store.setFuelLimit(1_000_000); // Limit computation
            store.setMemoryLimit(10 * 1024 * 1024); // 10MB memory limit

            Module module = Module.fromBytes(engine, loadSecureProcessorModule());

            Linker linker = Linker.create(engine);
            wasi.addToLinker(linker);

            Instance instance = linker.instantiate(store, module);

            try {
                Function secureProcess = instance.getFunction("secure_process");
                secureProcess.call();
                System.out.println("Secure processing completed within limits");
            } catch (WasmTrapException e) {
                System.err.println("Security violation or resource limit exceeded: " + e.getMessage());
            }
        }
    }

    /**
     * Demonstrates I/O redirection for logging and monitoring
     */
    public void processWithLogging(String inputFile) throws Exception {
        System.out.println("\n=== Processing with Custom I/O ===");

        createSampleTextFile(inputFile);

        // Create custom I/O streams
        Path logFile = Paths.get("processor.log");
        Path errorFile = Paths.get("processor.err");

        try (Store store = engine.createStore()) {
            WasiConfig wasiConfig = WasiConfig.builder()
                .args("logging-processor", inputFile)
                .env("LOG_FILE", logFile.toString())
                .preOpenDir(Paths.get("."), "/workspace")
                .stdout(logFile)
                .stderr(errorFile)
                .build();

            WasiContext wasi = WasiContext.create(store, wasiConfig);

            Module module = Module.fromBytes(engine, loadLoggingProcessorModule());

            Linker linker = Linker.create(engine);
            wasi.addToLinker(linker);

            Instance instance = linker.instantiate(store, module);

            Function process = instance.getFunction("_start");
            process.call();

            // Display captured logs
            if (Files.exists(logFile)) {
                System.out.println("=== Captured Logs ===");
                Files.readAllLines(logFile).forEach(System.out::println);
            }

            if (Files.exists(errorFile) && Files.size(errorFile) > 0) {
                System.out.println("=== Captured Errors ===");
                Files.readAllLines(errorFile).forEach(System.err::println);
            }
        }
    }

    private void createSampleTextFile(String filename) throws IOException {
        List<String> lines = Arrays.asList(
            "Hello, WebAssembly!",
            "This is a sample text file.",
            "It contains multiple lines of text.",
            "Each line will be processed by the WASI module.",
            "The processor can transform, analyze, or modify this content.",
            "WASI provides secure file system access.",
            "WebAssembly ensures safe execution.",
            "End of sample file."
        );

        Files.write(Paths.get(filename), lines);
        System.out.println("Created sample file: " + filename);
    }

    private void createSampleDirectory(String dirName) throws IOException {
        Path dir = Paths.get(dirName);
        Files.createDirectories(dir);

        // Create multiple files for batch processing
        for (int i = 1; i <= 5; i++) {
            String filename = "file" + i + ".txt";
            List<String> content = Arrays.asList(
                "This is file number " + i,
                "Content line 1 of file " + i,
                "Content line 2 of file " + i,
                "End of file " + i
            );
            Files.write(dir.resolve(filename), content);
        }

        System.out.println("Created sample directory with 5 files: " + dirName);
    }

    private void callProcessFileFunction(Instance instance, String inputFile, String outputFile) throws WasmException {
        // Direct function call if _start is not available
        Function processFunc = instance.getFunction("process_file");
        if (processFunc == null) {
            throw new WasmException("No process_file function found");
        }

        Memory memory = instance.getMemory("memory");
        if (memory == null) {
            throw new WasmException("No memory export found");
        }

        // Write filenames to memory and call function
        byte[] inputBytes = inputFile.getBytes();
        byte[] outputBytes = outputFile.getBytes();

        memory.write(1000, inputBytes);
        memory.write(2000, outputBytes);

        Value[] params = {
            Value.i32(1000), Value.i32(inputBytes.length),
            Value.i32(2000), Value.i32(outputBytes.length)
        };

        processFunc.call(params);
    }

    private void verifyProcessedFile(String filename) throws IOException {
        if (Files.exists(Paths.get(filename))) {
            System.out.println("=== Processed File Content ===");
            List<String> lines = Files.readAllLines(Paths.get(filename));
            lines.forEach(line -> System.out.println("  " + line));
        } else {
            System.out.println("Warning: Processed file not found: " + filename);
        }
    }

    // Placeholder methods for loading different WASI modules
    private byte[] loadTextProcessorModule() {
        // In practice, load from resources or files
        return createMinimalWasiModule("text_processor");
    }

    private byte[] loadBatchProcessorModule() {
        return createMinimalWasiModule("batch_processor");
    }

    private byte[] loadSecureProcessorModule() {
        return createMinimalWasiModule("secure_processor");
    }

    private byte[] loadLoggingProcessorModule() {
        return createMinimalWasiModule("logging_processor");
    }

    private byte[] createMinimalWasiModule(String name) {
        // This is a placeholder - in a real implementation you would:
        // 1. Load actual WASI-compiled modules (from Rust, C, etc.)
        // 2. Use pre-built modules from resources
        // 3. Compile from source using appropriate toolchains

        System.out.println("Loading WASI module: " + name + " (placeholder)");

        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // WASM magic number
            0x01, 0x00, 0x00, 0x00  // WASM version
            // ... (rest would be actual WASI-enabled bytecode)
        };
    }

    public void cleanup() throws Exception {
        if (engine != null) {
            engine.close();
        }
        if (runtime != null) {
            runtime.close();
        }
    }

    public static void main(String[] args) {
        WasiFileProcessor processor = null;

        try {
            processor = new WasiFileProcessor();

            // Demonstrate different WASI capabilities
            processor.processTextFile("input.txt", "output.txt");
            processor.processDirectory("input_dir", "output_dir");
            processor.secureFileProcessing("secure_input.txt");
            processor.processWithLogging("logged_input.txt");

            System.out.println("\n=== All WASI examples completed successfully ===");

        } catch (Exception e) {
            System.err.println("WASI processing failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (processor != null) {
                try {
                    processor.cleanup();
                } catch (Exception e) {
                    System.err.println("Cleanup failed: " + e.getMessage());
                }
            }
        }
    }
}