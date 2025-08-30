package ai.tegmentum.wasmtime4j.webassembly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for converting WebAssembly Text (WAT) format to WebAssembly Binary (WASM) format.
 * Uses the WebAssembly Binary Toolkit (wabt) if available.
 */
public final class WatToWasmConverter {
    private static final Logger LOGGER = Logger.getLogger(WatToWasmConverter.class.getName());
    
    private static final String WAT2WASM_COMMAND = "wat2wasm";
    private static final int TIMEOUT_SECONDS = 30;
    
    private WatToWasmConverter() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Converts a WAT file to WASM format.
     *
     * @param watFile the input WAT file
     * @param wasmFile the output WASM file
     * @return true if conversion was successful
     * @throws IOException if file operations fail
     */
    public static boolean convertWatToWasm(final Path watFile, final Path wasmFile) throws IOException {
        if (!Files.exists(watFile)) {
            throw new IOException("WAT file does not exist: " + watFile);
        }
        
        if (!isWabtAvailable()) {
            LOGGER.warning("wabt (WebAssembly Binary Toolkit) is not available. " +
                          "Cannot convert WAT to WASM. Install wabt: " +
                          "https://github.com/WebAssembly/wabt");
            return false;
        }
        
        try {
            final List<String> command = new ArrayList<>();
            command.add(WAT2WASM_COMMAND);
            command.add(watFile.toString());
            command.add("-o");
            command.add(wasmFile.toString());
            
            final ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            LOGGER.info("Converting " + watFile + " to " + wasmFile);
            
            final Process process = processBuilder.start();
            final boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("wat2wasm conversion timed out after " + TIMEOUT_SECONDS + " seconds");
            }
            
            final int exitCode = process.exitValue();
            if (exitCode != 0) {
                final String output = readProcessOutput(process);
                throw new IOException("wat2wasm conversion failed with exit code " + exitCode + 
                                    ". Output: " + output);
            }
            
            if (!Files.exists(wasmFile)) {
                throw new IOException("wat2wasm did not create output file: " + wasmFile);
            }
            
            LOGGER.info("Successfully converted " + watFile + " to " + wasmFile);
            return true;
            
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("wat2wasm conversion was interrupted", e);
        }
    }
    
    /**
     * Converts WAT content directly to WASM bytes.
     *
     * @param watContent the WAT content as a string
     * @return the WASM bytes, or null if conversion failed
     * @throws IOException if the conversion fails
     */
    public static byte[] convertWatToWasmBytes(final String watContent) throws IOException {
        if (!isWabtAvailable()) {
            LOGGER.warning("wabt is not available for WAT to WASM conversion");
            return null;
        }
        
        // Create temporary files
        final Path tempDir = Files.createTempDirectory("wasmtime4j-test");
        final Path tempWatFile = tempDir.resolve("temp.wat");
        final Path tempWasmFile = tempDir.resolve("temp.wasm");
        
        try {
            // Write WAT content to temporary file
            Files.writeString(tempWatFile, watContent, StandardOpenOption.CREATE);
            
            // Convert to WASM
            if (convertWatToWasm(tempWatFile, tempWasmFile)) {
                return Files.readAllBytes(tempWasmFile);
            } else {
                return null;
            }
        } finally {
            // Cleanup temporary files
            try {
                Files.deleteIfExists(tempWatFile);
                Files.deleteIfExists(tempWasmFile);
                Files.deleteIfExists(tempDir);
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "Failed to cleanup temporary files", e);
            }
        }
    }
    
    /**
     * Checks if the WebAssembly Binary Toolkit (wabt) is available on the system.
     *
     * @return true if wat2wasm command is available
     */
    public static boolean isWabtAvailable() {
        try {
            final Process process = new ProcessBuilder(WAT2WASM_COMMAND, "--version")
                .redirectErrorStream(true)
                .start();
            
            final boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (final IOException | InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Gets the version of wabt if available.
     *
     * @return the wabt version string, or empty string if not available
     */
    public static String getWabtVersion() {
        if (!isWabtAvailable()) {
            return "";
        }
        
        try {
            final Process process = new ProcessBuilder(WAT2WASM_COMMAND, "--version")
                .redirectErrorStream(true)
                .start();
            
            final boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "";
            }
            
            if (process.exitValue() == 0) {
                return readProcessOutput(process).trim();
            }
            
        } catch (final IOException | InterruptedException e) {
            LOGGER.log(Level.FINE, "Failed to get wabt version", e);
        }
        
        return "";
    }
    
    /**
     * Creates standard WebAssembly test modules in WAT format.
     *
     * @param outputDirectory the directory where to create test files
     * @throws IOException if file creation fails
     */
    public static void createStandardTestModules(final Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        
        // Create simple addition module
        final String addWat = """
            (module
              (func $add (param $lhs i32) (param $rhs i32) (result i32)
                local.get $lhs
                local.get $rhs
                i32.add)
              (export "add" (func $add))
            )
            """;
        writeWatFile(outputDirectory.resolve("add.wat"), addWat);
        
        // Create memory operations module
        final String memoryWat = """
            (module
              (memory 1)
              (export "memory" (memory 0))
              (func $load (param $offset i32) (result i32)
                local.get $offset
                i32.load)
              (func $store (param $offset i32) (param $value i32)
                local.get $offset
                local.get $value
                i32.store)
              (export "load" (func $load))
              (export "store" (func $store))
            )
            """;
        writeWatFile(outputDirectory.resolve("memory.wat"), memoryWat);
        
        // Create function import module
        final String importWat = """
            (module
              (import "env" "imported_func" (func $imported (param i32) (result i32)))
              (func $call_imported (param $x i32) (result i32)
                local.get $x
                call $imported)
              (export "call_imported" (func $call_imported))
            )
            """;
        writeWatFile(outputDirectory.resolve("import.wat"), importWat);
        
        // Create table operations module
        final String tableWat = """
            (module
              (table 10 funcref)
              (export "table" (table 0))
              (func $dummy (result i32)
                i32.const 42)
              (elem (i32.const 0) $dummy)
              (func $call_indirect (param $index i32) (result i32)
                local.get $index
                call_indirect (type 0))
              (type (func (result i32)))
              (export "call_indirect" (func $call_indirect))
            )
            """;
        writeWatFile(outputDirectory.resolve("table.wat"), tableWat);
        
        LOGGER.info("Created standard test modules in " + outputDirectory);
    }
    
    /**
     * Reads the output from a process.
     *
     * @param process the process to read from
     * @return the process output as a string
     * @throws IOException if reading fails
     */
    private static String readProcessOutput(final Process process) throws IOException {
        final StringBuilder output = new StringBuilder();
        
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        
        return output.toString();
    }
    
    /**
     * Writes WAT content to a file.
     *
     * @param watFile the WAT file path
     * @param content the WAT content
     * @throws IOException if writing fails
     */
    private static void writeWatFile(final Path watFile, final String content) throws IOException {
        Files.writeString(watFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.fine("Created WAT file: " + watFile);
    }
}