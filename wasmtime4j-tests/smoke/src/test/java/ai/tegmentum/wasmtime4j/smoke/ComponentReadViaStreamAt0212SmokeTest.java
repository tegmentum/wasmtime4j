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
 * Proves the host services {@code wasi:io/streams} read-via-stream at the CURRENT WASI version
 * ({@code 0.2.12}), not just at the older {@code 0.2.6} covered by
 * {@link ComponentFileInputStreamReadSmokeTest}. This is the host-side rebuttal to a hypothesis the
 * Svalinn fsread witness briefly held — that the shipped dylib fails to service read-via-stream at
 * 0.2.12.
 *
 * <p>The {@code file-stream-reader-0212} component depends on the {@code wasi} crate, which unifies
 * its WASI imports up to {@code wasi:io/streams@0.2.12}, and reads the granted file EXPLICITLY through
 * the {@code wasi} crate's {@code read_via_stream} + {@code blocking_read} (a file-backed input
 * stream). It returns the bytes here — so the host is fine at 0.2.12.
 *
 * <p>Context (why this matters): Svalinn observed a real trap on the file-backed input-stream path,
 * but only when a guest reads via {@code std::fs} in a component that links BOTH {@code std} (its own,
 * older wasip2 bindings) AND the {@code wasi} crate (0.2.12). That is a guest-side ABI mix inside the
 * component ({@code std::fs}'s compiled read-via-stream skews against the 0.2.12-unified imports), not
 * a host defect: the identical read-via-stream byte path driven through the {@code wasi} crate — as
 * this test does — succeeds. See {@code witness/fsread/FINDINGS.md}.
 */
@DisplayName("Component read-via-stream at wasi 0.2.12 (host-side) Smoke Test")
public final class ComponentReadViaStreamAt0212SmokeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentReadViaStreamAt0212SmokeTest.class.getName());

  private static final String COMPONENT_RESOURCE =
      "/components/file-stream-reader-0212.component.wasm";

  private static byte[] componentBytes;
  private static boolean jniAvailable;

  @BeforeAll
  static void loadComponentAndRuntime() throws IOException {
    try (InputStream is =
        ComponentReadViaStreamAt0212SmokeTest.class.getResourceAsStream(COMPONENT_RESOURCE)) {
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
  @DisplayName("host returns bytes for an explicit wasi:io/streams@0.2.12 read-via-stream")
  void readViaStreamAt0212ReturnsBytes(@TempDir final Path tmp) throws Exception {
    assumeTrue(componentBytes != null, "file-stream-reader-0212 component wasm not on test classpath");
    assumeTrue(jniAvailable, "JNI runtime not available");

    final String contents = "svalinn-read-via-stream-0212\nline2\n";
    Files.write(tmp.resolve("app.conf"), contents.getBytes(StandardCharsets.UTF_8));

    final String result = readViaStream(tmp, "app.conf");

    LOGGER.info("read-stream(0.2.12) result=" + result);
    assertTrue(
        result.startsWith("OK:"),
        "the host must service read-via-stream at 0.2.12, but guest returned: " + result);
    assertTrue(
        result.contains("svalinn-read-via-stream-0212"),
        "read must return the actual file contents, but guest returned: " + result);
  }
}
