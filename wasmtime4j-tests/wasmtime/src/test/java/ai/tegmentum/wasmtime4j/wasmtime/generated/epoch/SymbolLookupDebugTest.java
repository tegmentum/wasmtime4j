/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.wasmtime.generated.epoch;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.foreign.SymbolLookup;
import org.junit.jupiter.api.Test;

/** Debug test for symbol lookup. */
public class SymbolLookupDebugTest {

  @Test
  void testSymbolsExist() throws Exception {
    NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertTrue(loader.isLoaded(), "Library should be loaded");

    // Get library path via reflection
    var pathField = loader.getClass().getDeclaredField("loadInfo");
    pathField.setAccessible(true);
    var loadInfo = pathField.get(loader);
    var getPathMethod = loadInfo.getClass().getMethod("getExtractedPath");
    var extractedPath = getPathMethod.invoke(loadInfo);
    System.out.println("Extracted library path: " + extractedPath);

    SymbolLookup lookup = loader.getSymbolLookup();
    assertNotNull(lookup, "SymbolLookup should not be null");

    // Check epoch symbols
    String[] symbols = {
      "wasmtime4j_panama_store_epoch_deadline_trap",
      "wasmtime4j_panama_store_set_epoch_deadline_callback",
      "wasmtime4j_panama_store_clear_epoch_deadline_callback",
      "wasmtime4j_panama_store_set_epoch_deadline",
      "wasmtime4j_engine_create",
      "wasmtime4j_panama_store_set_fuel"
    };

    for (String sym : symbols) {
      var found = lookup.find(sym);
      System.out.println(sym + " -> " + (found.isPresent() ? "FOUND" : "NOT FOUND"));
    }

    // Verify the library has the symbols by checking with nm
    if (extractedPath != null) {
      java.nio.file.Path libPath = java.nio.file.Path.of(extractedPath.toString());
      System.out.println("Library file size: " + java.nio.file.Files.size(libPath) + " bytes");
      System.out.println(
          "Library file modified: " + java.nio.file.Files.getLastModifiedTime(libPath));

      ProcessBuilder pb = new ProcessBuilder("nm", "-gU", extractedPath.toString());
      pb.redirectErrorStream(true);
      Process p = pb.start();
      String output = new String(p.getInputStream().readAllBytes());
      System.out.println("\nnm output (filtered for epoch):");
      for (String line : output.split("\n")) {
        if (line.contains("epoch")) {
          System.out.println(line);
        }
      }

      // Also check the JAR library
      System.out.println("\nJAR library epoch symbols:");
      String version = "42.0.1-1.0.0";
      String jarPath =
          System.getProperty("user.home")
              + "/.m2/repository/ai/tegmentum/wasmtime4j-native/"
              + version
              + "/wasmtime4j-native-"
              + version
              + ".jar";
      System.out.println("JAR path: " + jarPath);
      java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath);
      var entry = jar.getEntry("natives/darwin-aarch64/libwasmtime4j.dylib");
      System.out.println("JAR entry size: " + entry.getSize());
      jar.close();
    }
  }
}
