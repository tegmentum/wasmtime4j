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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.wit.WitString;
import ai.tegmentum.wasmtime4j.wit.WitU64;
import ai.tegmentum.wasmtime4j.wit.WitValue;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.MarshalledValue;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone decomposition micro-benchmark for a component export
 * render(u64, string, string, string) -> string.
 *
 * Isolates: Java-side arg marshalling+return unmarshalling, the native crossing (full invoke),
 * and a JNI+lock+lookup floor (nativeComponentInstanceHasFunc).
 */
public final class MarshallDecompBench {

  static final String LEVEL = "INFO";
  static final String LOGGER = "com.example.service.OrderProcessor";
  static final String MESSAGE =
      "Processing order 12345 for customer Alice with total $99.95 across 3 line items";
  static final String FUNC = "render";

  static long engineHandle;
  static long instanceId;
  static long sink; // blackhole

  public static void main(String[] args) throws Exception {
    ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();

    // A component exporting render(u64,string,string,string)->string. Override with
    // -Dw4j.bench.component=/path/to.wasm; defaults to the Svalinn log4j witness core.
    String wasmPath = System.getProperty("w4j.bench.component",
        System.getProperty("user.home")
            + "/git/svalinn/witness/log4j/component/svalinn-logging-core.component.wasm");
    byte[] wasm = Files.readAllBytes(Paths.get(wasmPath));

    JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine();
    JniComponent.JniComponentHandle comp = engine.loadComponentFromBytes(wasm);
    JniComponent.JniComponentInstanceHandle inst = engine.instantiateComponent(comp);
    engineHandle = engine.getNativeHandle();
    instanceId = inst.getNativeHandle();

    // sanity: one full call
    String out = total(0);
    System.out.println("sanity render() -> \"" + out + "\"");

    // Pre-marshalled args for NATIVE_ONLY
    int[] disc = new int[4];
    byte[][] data = new byte[4][];
    buildArgs(0, disc, data);
    // canned result for JAVA_MARSHAL unmarshal
    Object[] r = JniComponent.nativeComponentInvokeFunction(engineHandle, instanceId, FUNC, disc, data);
    final int cannedType = (Integer) r[0];
    final byte[] cannedData = (byte[]) r[1];

    final int WARMUP = 200_000;
    final int ITERS = 1_000_000;

    // Warmup all paths
    for (int i = 0; i < WARMUP; i++) {
      sink += total(i).length();
      sink += nativeOnly(disc, data);
      sink += javaMarshal(i, cannedType, cannedData);
      sink += probe();
    }

    System.out.println("\n--- results (ns/op, min of 5 trials, " + ITERS + " iters) ---");
    report("TOTAL (java marshal + native + java unmarshal)", () -> {
      long s = 0;
      for (int i = 0; i < ITERS; i++) s += total(i).length();
      return s;
    }, ITERS);
    report("NATIVE_ONLY (invoke, pre-marshalled args)", () -> {
      long s = 0;
      for (int i = 0; i < ITERS; i++) s += nativeOnly(disc, data);
      return s;
    }, ITERS);
    report("JAVA_MARSHAL (arg marshal + return unmarshal, no native)", () -> {
      long s = 0;
      for (int i = 0; i < ITERS; i++) s += javaMarshal(i, cannedType, cannedData);
      return s;
    }, ITERS);
    report("PROBE (nativeComponentInstanceHasFunc: JNI+lock+lookup)", () -> {
      long s = 0;
      for (int i = 0; i < ITERS; i++) s += probe();
      return s;
    }, ITERS);

    System.out.println("sink=" + sink);
    inst.close();
    comp.close();
    engine.close();
  }

  interface Loop { long run(); }

  static void report(String name, Loop loop, int iters) {
    long best = Long.MAX_VALUE;
    for (int t = 0; t < 5; t++) {
      long t0 = System.nanoTime();
      sink += loop.run();
      long el = System.nanoTime() - t0;
      best = Math.min(best, el);
    }
    System.out.printf("%-58s %8.1f ns/op%n", name, (double) best / iters);
  }

  // Build a fresh set of marshalled args each call (realistic per-event)
  static void buildArgs(long ts, int[] disc, byte[][] data) throws Exception {
    List<WitValue> vals = new ArrayList<>(4);
    vals.add(WitU64.of(ts));
    vals.add(WitString.of(LEVEL));
    vals.add(WitString.of(LOGGER));
    vals.add(WitString.of(MESSAGE));
    List<MarshalledValue> m = WitValueMarshaller.marshalAll(vals);
    for (int i = 0; i < 4; i++) {
      disc[i] = m.get(i).getTypeDiscriminator();
      data[i] = m.get(i).getData();
    }
  }

  static String total(long ts) {
    try {
      int[] disc = new int[4];
      byte[][] data = new byte[4][];
      buildArgs(ts, disc, data);
      Object[] r = JniComponent.nativeComponentInvokeFunction(engineHandle, instanceId, FUNC, disc, data);
      int type = (Integer) r[0];
      byte[] rd = (byte[]) r[1];
      WitValue rv = WitValueMarshaller.unmarshal(type, rd);
      return ((WitString) rv).toJava();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static long nativeOnly(int[] disc, byte[][] data) {
    Object[] r = JniComponent.nativeComponentInvokeFunction(engineHandle, instanceId, FUNC, disc, data);
    return ((byte[]) r[1]).length;
  }

  static long javaMarshal(long ts, int cannedType, byte[] cannedData) {
    try {
      int[] disc = new int[4];
      byte[][] data = new byte[4][];
      buildArgs(ts, disc, data);
      long s = disc[0] + data[3].length;
      WitValue rv = WitValueMarshaller.unmarshal(cannedType, cannedData);
      return s + ((WitString) rv).toJava().length();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static long probe() {
    return JniComponent.nativeComponentInstanceHasFunc(engineHandle, instanceId, FUNC);
  }
}
