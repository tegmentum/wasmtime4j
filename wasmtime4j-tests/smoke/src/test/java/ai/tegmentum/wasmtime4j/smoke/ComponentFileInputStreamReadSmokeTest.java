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
package ai.tegmentum.wasmtime4j.smoke;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiPreview2Config;
import ai.tegmentum.wasmtime4j.wit.WitString;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Smoke test proving that a wasip2 Component can read a file through a FILE-BACKED INPUT STREAM
 * (Rust {@code std::fs::read} -&gt; {@code wasi:filesystem} {@code read-via-stream} -&gt;
 * {@code wasi:io/streams} {@code blocking-read}) on the component instantiation path.
 *
 * <p>Regression guard tied to a symptom the Svalinn project reported twice (see
 * {@code witness/fsread/FINDINGS.md} and {@code svalinn-secure-log/FINDINGS.md}): {@code File::open}
 * of a granted file succeeds, but the subsequent stream read was observed to TRAP with no guest
 * panic, while the low-level positioned {@code descriptor.read}, file OUTPUT streams, and SOCKET
 * input streams worked. Investigation showed the wasi:io/streams file host on this component path is
 * stock {@code wasmtime_wasi::p2::add_to_linker_sync} — a consistently synchronous path — and that a
 * file-backed input-stream read returns the bytes on the shipped 46.0.1-1.2.0 dylib (as this test
 * asserts). This test therefore locks in the working behavior so any future regression that drops or
 * mis-wires the file-backed input-stream read fails loudly rather than silently degrading a
 * capability Svalinn depends on.
 *
 * <p>The {@code file-stream-reader.component.wasm} component exports {@code read-stream(path) ->
 * string}; it calls {@code std::fs::read} on the given guest path and returns {@code "OK:\n<bytes>"}
 * on success or {@code "ERR: <detail>"} on a clean Rust error. A host trap would produce no return at
 * all (the invoke would throw), which is what a stream-wiring regression would look like.
 *
 * <p><b>What actually caused the Svalinn trap (resolved).</b> This component is pure {@code std}
 * (no {@code wasi} crate) and imports {@code wasi 0.2.6}; its {@code std::fs} read returns the bytes.
 * The Svalinn fsread witness trap was later isolated to a GUEST-SIDE ABI mix, not a host defect: its
 * component links BOTH {@code std} (its own older wasip2 bindings) AND the {@code wasi} crate, which
 * unifies the component's imports up to {@code 0.2.12}; {@code std::fs}'s compiled read-via-stream
 * then skews against the unified imports and traps, while the identical read-via-stream byte path
 * driven through the {@code wasi} crate succeeds. The host services read-via-stream at both 0.2.6
 * (this test) and 0.2.12 (see {@link ComponentReadViaStreamAt0212SmokeTest}). See
 * {@code witness/fsread/FINDINGS.md} for the full three-export isolation.
 */
@DisplayName("Component File-Backed Input Stream Read Smoke Test")
public final class ComponentFileInputStreamReadSmokeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentFileInputStreamReadSmokeTest.class.getName());

  private static final String COMPONENT_RESOURCE = "/components/file-stream-reader.component.wasm";

  private static byte[] componentBytes;
  private static boolean jniAvailable;

  @BeforeAll
  static void loadComponentAndRuntime() throws IOException {
    try (InputStream is =
        ComponentFileInputStreamReadSmokeTest.class.getResourceAsStream(COMPONENT_RESOURCE)) {
      if (is != null) {
        componentBytes = is.readAllBytes();
      }
    }
    try {
      jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    } catch (final Throwable t) {
      jniAvailable = false;
    }
  }

  /**
   * Instantiate the reader component with {@code hostDir} preopened read-write at {@code /data} and
   * invoke {@code read-stream("/data/<fileName>")}. Returns the guest's result string, or throws if
   * the stream read traps (a stream-wiring regression would surface here as a thrown trap).
   */
  private static String readViaStream(final Path hostDir, final String fileName) throws Exception {
    try (WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI);
        Engine engine = runtime.createEngine();
        ComponentEngine componentEngine = runtime.createComponentEngine()) {
      final Component component = componentEngine.compileComponent(componentBytes);

      final WasiPreview2Config wasi =
          WasiPreview2Config.builder().preopenDir(hostDir, "/data", false).build();

      final ComponentLinker<Object> linker = runtime.createComponentLinker(engine);
      linker.enableWasiPreview2(wasi);

      final Store store = engine.createStore();
      final ComponentInstance instance = linker.instantiate(store, component);

      final Object result = instance.invoke("read-stream", WitString.of("/data/" + fileName));
      linker.close();
      return String.valueOf(result);
    }
  }

  @Test
  @DisplayName("component reads a preopened file via wasi:io/streams and gets the bytes back")
  void fileBackedInputStreamReadReturnsBytes(@TempDir final Path tmp) throws Exception {
    assumeTrue(componentBytes != null, "file-stream-reader component wasm not on test classpath");
    assumeTrue(jniAvailable, "JNI runtime not available");

    final String contents = "svalinn-file-stream-read-witness\nline2\n";
    Files.write(tmp.resolve("app.conf"), contents.getBytes(StandardCharsets.UTF_8));

    // A stream-wiring regression would make this invoke THROW (a host trap with no guest panic).
    // On the shipped dylib it returns the bytes — assert that directly rather than skipping, so a
    // regression fails loudly instead of silently degrading.
    final String result = readViaStream(tmp, "app.conf");

    LOGGER.info("read-stream result=" + result);
    assertTrue(
        result.startsWith("OK:"),
        "file-backed input-stream read must return the file bytes, but guest returned: " + result);
    assertTrue(
        result.contains("svalinn-file-stream-read-witness"),
        "read must return the actual file contents, but guest returned: " + result);
  }
}
