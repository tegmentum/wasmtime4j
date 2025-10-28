package ai.tegmentum.wasmtime4j.comparison.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: wide-arithmetic.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class WideArithmeticTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = WideArithmeticTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("wide arithmetic")
  public void testWideArithmetic(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/WideArithmeticTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/WideArithmeticTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "i64.add128" ( i64.const 0) ( i64.const 0) ( i64.const 0) (
      // i64.const 0)) ( i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 0) ( i64.const 1) ( i64.const 1) (
      // i64.const 0)) ( i64.const 1) ( i64.const 1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(1L)},
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 1) ( i64.const 0) ( i64.const -1) (
      // i64.const 0)) ( i64.const 0) ( i64.const 1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(1L)},
          WasmValue.i64(1L),
          WasmValue.i64(0L),
          WasmValue.i64(-1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 1) ( i64.const 1) ( i64.const -1) (
      // i64.const -1)) ( i64.const 0) ( i64.const 1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(1L)},
          WasmValue.i64(1L),
          WasmValue.i64(1L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const 0) ( i64.const 0) (
      // i64.const 0)) ( i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const 0) ( i64.const 1) (
      // i64.const 0)) ( i64.const -1) ( i64.const -1))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(-1L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const 1) ( i64.const 1) (
      // i64.const 1)) ( i64.const -1) ( i64.const -1))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(-1L)},
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const 0) ( i64.const 1) (
      // i64.const 1)) ( i64.const -1) ( i64.const -2))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(-2L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 0) ( i64.const 0)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 0) ( i64.const 0)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 1) ( i64.const 1)) ( i64.const 1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 1) ( i64.const 1)) ( i64.const 1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -1) ( i64.const -1)) ( i64.const 1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(0L)},
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -1) ( i64.const 1)) ( i64.const -1) (
      // i64.const -1))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(-1L)},
          WasmValue.i64(-1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const -1) ( i64.const 1)) ( i64.const -1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(0L)},
          WasmValue.i64(-1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const -2418420703207364752) ( i64.const -1) (
      // i64.const -1) ( i64.const -1)) ( i64.const -2418420703207364753) ( i64.const -1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(-2418420703207364753L), WasmValue.i64(-1L)},
          WasmValue.i64(-2418420703207364752L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 0) ( i64.const 0) ( i64.const
      // -4579433644172935106) ( i64.const -1)) ( i64.const -4579433644172935106) ( i64.const -1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(-4579433644172935106L), WasmValue.i64(-1L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(-4579433644172935106L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 0) ( i64.const 0) ( i64.const 1) (
      // i64.const -1)) ( i64.const 1) ( i64.const -1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(-1L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 1) ( i64.const 0) ( i64.const 1) (
      // i64.const 0)) ( i64.const 2) ( i64.const 0))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(2L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.add128" ( i64.const -1) ( i64.const -1) ( i64.const -1) (
      // i64.const -1)) ( i64.const -2) ( i64.const -1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(-2L), WasmValue.i64(-1L)},
          WasmValue.i64(-1L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 0) ( i64.const -1) ( i64.const 1) (
      // i64.const 0)) ( i64.const 1) ( i64.const -1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(-1L)},
          WasmValue.i64(0L),
          WasmValue.i64(-1L),
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 0) ( i64.const 0) ( i64.const 0) (
      // i64.const -1)) ( i64.const 0) ( i64.const -1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(-1L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(0L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 1) ( i64.const 0) ( i64.const -1) (
      // i64.const -1)) ( i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(0L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 0) ( i64.const 6184727276166606191) (
      // i64.const 0) ( i64.const 1)) ( i64.const 0) ( i64.const 6184727276166606192))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(6184727276166606192L)},
          WasmValue.i64(0L),
          WasmValue.i64(6184727276166606191L),
          WasmValue.i64(0L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const -8434911321912688222) ( i64.const -1) (
      // i64.const 1) ( i64.const -1)) ( i64.const -8434911321912688221) ( i64.const -2))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(-8434911321912688221L), WasmValue.i64(-2L)},
          WasmValue.i64(-8434911321912688222L),
          WasmValue.i64(-1L),
          WasmValue.i64(1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 1) ( i64.const -1) ( i64.const 0) (
      // i64.const -1)) ( i64.const 1) ( i64.const -2))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(-2L)},
          WasmValue.i64(1L),
          WasmValue.i64(-1L),
          WasmValue.i64(0L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 1) ( i64.const -5148941131328838092) (
      // i64.const 0) ( i64.const 0)) ( i64.const 1) ( i64.const -5148941131328838092))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(-5148941131328838092L)},
          WasmValue.i64(1L),
          WasmValue.i64(-5148941131328838092L),
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 1) ( i64.const 1) ( i64.const 1) (
      // i64.const 0)) ( i64.const 2) ( i64.const 1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(2L), WasmValue.i64(1L)},
          WasmValue.i64(1L),
          WasmValue.i64(1L),
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.add128" ( i64.const -1) ( i64.const -1) ( i64.const
      // -3636740005180858631) ( i64.const -1)) ( i64.const -3636740005180858632) ( i64.const -1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(-3636740005180858632L), WasmValue.i64(-1L)},
          WasmValue.i64(-1L),
          WasmValue.i64(-1L),
          WasmValue.i64(-3636740005180858631L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const -5529682780229988275) ( i64.const -1) (
      // i64.const 0) ( i64.const 0)) ( i64.const -5529682780229988275) ( i64.const -1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(-5529682780229988275L), WasmValue.i64(-1L)},
          WasmValue.i64(-5529682780229988275L),
          WasmValue.i64(-1L),
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 1) ( i64.const -5381447440966559717) (
      // i64.const 1020031372481336745) ( i64.const 1)) ( i64.const 1020031372481336746) ( i64.const
      // -5381447440966559716))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {
            WasmValue.i64(1020031372481336746L), WasmValue.i64(-5381447440966559716L)
          },
          WasmValue.i64(1L),
          WasmValue.i64(-5381447440966559717L),
          WasmValue.i64(1020031372481336745L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 1) ( i64.const 1) ( i64.const 0) (
      // i64.const 0)) ( i64.const 1) ( i64.const 1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(1L)},
          WasmValue.i64(1L),
          WasmValue.i64(1L),
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.add128" ( i64.const -9133888546939907356) ( i64.const -1) (
      // i64.const 1) ( i64.const 1)) ( i64.const -9133888546939907355) ( i64.const 0))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(-9133888546939907355L), WasmValue.i64(0L)},
          WasmValue.i64(-9133888546939907356L),
          WasmValue.i64(-1L),
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const -4612047512704241719) ( i64.const -1) (
      // i64.const 0) ( i64.const -1)) ( i64.const -4612047512704241719) ( i64.const -2))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(-4612047512704241719L), WasmValue.i64(-2L)},
          WasmValue.i64(-4612047512704241719L),
          WasmValue.i64(-1L),
          WasmValue.i64(0L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.add128" ( i64.const 414720966820876428) ( i64.const -1) (
      // i64.const 1) ( i64.const 0)) ( i64.const 414720966820876429) ( i64.const -1))
      runner.assertReturn(
          "i64.add128",
          new WasmValue[] {WasmValue.i64(414720966820876429L), WasmValue.i64(-1L)},
          WasmValue.i64(414720966820876428L),
          WasmValue.i64(-1L),
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const -2459085471354756766) (
      // i64.const -9151153060221070927) ( i64.const -1)) ( i64.const 9151153060221070927) (
      // i64.const -2459085471354756766))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {
            WasmValue.i64(9151153060221070927L), WasmValue.i64(-2459085471354756766L)
          },
          WasmValue.i64(0L),
          WasmValue.i64(-2459085471354756766L),
          WasmValue.i64(-9151153060221070927L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 4566502638724063423) ( i64.const
      // -4282658540409485563) ( i64.const -6884077310018979971) ( i64.const -1)) ( i64.const
      // -6996164124966508222) ( i64.const -4282658540409485563))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {
            WasmValue.i64(-6996164124966508222L), WasmValue.i64(-4282658540409485563L)
          },
          WasmValue.i64(4566502638724063423L),
          WasmValue.i64(-4282658540409485563L),
          WasmValue.i64(-6884077310018979971L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 1) ( i64.const 3118380319444903041) (
      // i64.const 0) ( i64.const 3283115686417695443)) ( i64.const 1) ( i64.const
      // -164735366972792402))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(-164735366972792402L)},
          WasmValue.i64(1L),
          WasmValue.i64(3118380319444903041L),
          WasmValue.i64(0L),
          WasmValue.i64(3283115686417695443L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const -7208415241680161810) ( i64.const -1) (
      // i64.const 1) ( i64.const 0)) ( i64.const -7208415241680161811) ( i64.const -1))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-7208415241680161811L), WasmValue.i64(-1L)},
          WasmValue.i64(-7208415241680161810L),
          WasmValue.i64(-1L),
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const 3944850126731328706) (
      // i64.const 1) ( i64.const 1)) ( i64.const -1) ( i64.const 3944850126731328704))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(3944850126731328704L)},
          WasmValue.i64(0L),
          WasmValue.i64(3944850126731328706L),
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 1) ( i64.const -1) ( i64.const -1) (
      // i64.const -1)) ( i64.const 2) ( i64.const -1))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(2L), WasmValue.i64(-1L)},
          WasmValue.i64(1L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const -1) ( i64.const -1) ( i64.const
      // 4855833073346115923) ( i64.const -6826437637438999645)) ( i64.const -4855833073346115924) (
      // i64.const 6826437637438999644))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {
            WasmValue.i64(-4855833073346115924L), WasmValue.i64(6826437637438999644L)
          },
          WasmValue.i64(-1L),
          WasmValue.i64(-1L),
          WasmValue.i64(4855833073346115923L),
          WasmValue.i64(-6826437637438999645L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 1) ( i64.const 0) ( i64.const -1) (
      // i64.const -1)) ( i64.const 2) ( i64.const 0))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(2L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(0L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 1) ( i64.const 0) ( i64.const 1) (
      // i64.const 0)) ( i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const -1) ( i64.const -1) ( i64.const 0) (
      // i64.const 0)) ( i64.const -1) ( i64.const -1))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(-1L)},
          WasmValue.i64(-1L),
          WasmValue.i64(-1L),
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 1) ( i64.const -1) ( i64.const
      // -6365475388498096428) ( i64.const -1)) ( i64.const 6365475388498096429) ( i64.const -1))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(6365475388498096429L), WasmValue.i64(-1L)},
          WasmValue.i64(1L),
          WasmValue.i64(-1L),
          WasmValue.i64(-6365475388498096428L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 6804238617560992346) ( i64.const -1) (
      // i64.const 0) ( i64.const -1)) ( i64.const 6804238617560992346) ( i64.const 0))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(6804238617560992346L), WasmValue.i64(0L)},
          WasmValue.i64(6804238617560992346L),
          WasmValue.i64(-1L),
          WasmValue.i64(0L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const 1) ( i64.const 1) (
      // i64.const -7756145513466453619)) ( i64.const -1) ( i64.const 7756145513466453619))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(7756145513466453619L)},
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(1L),
          WasmValue.i64(-7756145513466453619L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 1) ( i64.const -1) ( i64.const 1) (
      // i64.const 1)) ( i64.const 0) ( i64.const -2))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(-2L)},
          WasmValue.i64(1L),
          WasmValue.i64(-1L),
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const 1) ( i64.const 1) (
      // i64.const 0)) ( i64.const -1) ( i64.const 0))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 1) ( i64.const 5602881641763648953) (
      // i64.const -2110589244314239080) ( i64.const -1)) ( i64.const 2110589244314239081) (
      // i64.const 5602881641763648953))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {
            WasmValue.i64(2110589244314239081L), WasmValue.i64(5602881641763648953L)
          },
          WasmValue.i64(1L),
          WasmValue.i64(5602881641763648953L),
          WasmValue.i64(-2110589244314239080L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const 1) ( i64.const -1) (
      // i64.const -1)) ( i64.const 1) ( i64.const 1))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(1L)},
          WasmValue.i64(0L),
          WasmValue.i64(1L),
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const -1) ( i64.const
      // 3553816990259121806) ( i64.const -2105235417856431622)) ( i64.const -3553816990259121806) (
      // i64.const 2105235417856431620))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {
            WasmValue.i64(-3553816990259121806L), WasmValue.i64(2105235417856431620L)
          },
          WasmValue.i64(0L),
          WasmValue.i64(-1L),
          WasmValue.i64(3553816990259121806L),
          WasmValue.i64(-2105235417856431622L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 1861102705894987245) ( i64.const 1) (
      // i64.const 3713781778534059871) ( i64.const 1)) ( i64.const -1852679072639072626) (
      // i64.const -1))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-1852679072639072626L), WasmValue.i64(-1L)},
          WasmValue.i64(1861102705894987245L),
          WasmValue.i64(1L),
          WasmValue.i64(3713781778534059871L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.sub128" ( i64.const 0) ( i64.const -1) ( i64.const 1) (
      // i64.const 1832524486821761762)) ( i64.const -1) ( i64.const -1832524486821761764))
      runner.assertReturn(
          "i64.sub128",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(-1832524486821761764L)},
          WasmValue.i64(0L),
          WasmValue.i64(-1L),
          WasmValue.i64(1L),
          WasmValue.i64(1832524486821761762L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 1) ( i64.const 1)) ( i64.const 1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 0) ( i64.const 6287758211025156705))
      // ( i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(6287758211025156705L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -6643537319803451357) ( i64.const 1))
      // ( i64.const -6643537319803451357) ( i64.const -1))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(-6643537319803451357L), WasmValue.i64(-1L)},
          WasmValue.i64(-6643537319803451357L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -2483565146858803428) ( i64.const 0))
      // ( i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(-2483565146858803428L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 1) ( i64.const 1)) ( i64.const 1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -3838951433439430085) ( i64.const
      // 3471602925362676030)) ( i64.const 5186941893001237834) ( i64.const -722475195264825124))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {
            WasmValue.i64(5186941893001237834L), WasmValue.i64(-722475195264825124L)
          },
          WasmValue.i64(-3838951433439430085L),
          WasmValue.i64(3471602925362676030L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -8262495286814853129) ( i64.const
      // 7883241869666573970)) ( i64.const -8557189786755031842) ( i64.const -3530988912334554469))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {
            WasmValue.i64(-8557189786755031842L), WasmValue.i64(-3530988912334554469L)
          },
          WasmValue.i64(-8262495286814853129L),
          WasmValue.i64(7883241869666573970L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 4278371902407959701) ( i64.const 1))
      // ( i64.const 4278371902407959701) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(4278371902407959701L), WasmValue.i64(0L)},
          WasmValue.i64(4278371902407959701L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -8852706149487089182) ( i64.const
      // -1)) ( i64.const 8852706149487089182) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(8852706149487089182L), WasmValue.i64(0L)},
          WasmValue.i64(-8852706149487089182L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 1) ( i64.const -1)) ( i64.const -1) (
      // i64.const -1))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(-1L)},
          WasmValue.i64(1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -1) ( i64.const
      // -4329244561838653387)) ( i64.const 4329244561838653387) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(4329244561838653387L), WasmValue.i64(0L)},
          WasmValue.i64(-1L),
          WasmValue.i64(-4329244561838653387L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -1) ( i64.const -1)) ( i64.const 1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(0L)},
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 697896157315764057) ( i64.const 1)) (
      // i64.const 697896157315764057) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(697896157315764057L), WasmValue.i64(0L)},
          WasmValue.i64(697896157315764057L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 1) ( i64.const 1)) ( i64.const 1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const -1) ( i64.const 0)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(-1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 0) ( i64.const -3769664482072947073))
      // ( i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(-3769664482072947073L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 1) ( i64.const 8414291037346403854))
      // ( i64.const 8414291037346403854) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(8414291037346403854L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(8414291037346403854L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 1) ( i64.const -1)) ( i64.const -1) (
      // i64.const -1))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(-1L)},
          WasmValue.i64(1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 5014655679779318485) ( i64.const
      // -5080037812563681985)) ( i64.const 2842857627777395563) ( i64.const -1380983027057486843))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {
            WasmValue.i64(2842857627777395563L), WasmValue.i64(-1380983027057486843L)
          },
          WasmValue.i64(5014655679779318485L),
          WasmValue.i64(-5080037812563681985L));

      // ( assert_return ( invoke "i64.mul_wide_s" ( i64.const 0) ( i64.const 1)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_s",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const -4734436040338162711) ( i64.const 0))
      // ( i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(-4734436040338162711L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 1) ( i64.const 0)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 3270597527173764279) ( i64.const
      // 6636648075495406358)) ( i64.const -5430303818902260550) ( i64.const 1176674035141685826))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {
            WasmValue.i64(-5430303818902260550L), WasmValue.i64(1176674035141685826L)
          },
          WasmValue.i64(3270597527173764279L),
          WasmValue.i64(6636648075495406358L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const -7771814344630108151) ( i64.const 1))
      // ( i64.const -7771814344630108151) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(-7771814344630108151L), WasmValue.i64(0L)},
          WasmValue.i64(-7771814344630108151L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 1) ( i64.const 0)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 1) ( i64.const -7864138787704962081))
      // ( i64.const -7864138787704962081) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(-7864138787704962081L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(-7864138787704962081L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 1) ( i64.const 518555141550256010)) (
      // i64.const 518555141550256010) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(518555141550256010L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(518555141550256010L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 1) ( i64.const -1)) ( i64.const -1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 1118900477321231571) ( i64.const -1))
      // ( i64.const -1118900477321231571) ( i64.const 1118900477321231570))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {
            WasmValue.i64(-1118900477321231571L), WasmValue.i64(1118900477321231570L)
          },
          WasmValue.i64(1118900477321231571L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const -1) ( i64.const 0)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(-1L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const -5586890671027490027) ( i64.const 1))
      // ( i64.const -5586890671027490027) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(-5586890671027490027L), WasmValue.i64(0L)},
          WasmValue.i64(-5586890671027490027L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 0) ( i64.const 3603850799751152505))
      // ( i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(3603850799751152505L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const -1) ( i64.const -1)) ( i64.const 1) (
      // i64.const 18446744073709551614))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(-2L)},
          WasmValue.i64(-1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 0) ( i64.const 1)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const -7344082851774441644) ( i64.const
      // 3896439839137544024)) ( i64.const 5738542512914895072) ( i64.const 2345175459296971666))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {
            WasmValue.i64(5738542512914895072L), WasmValue.i64(2345175459296971666L)
          },
          WasmValue.i64(-7344082851774441644L),
          WasmValue.i64(3896439839137544024L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 0) ( i64.const 0)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 616395976148874061) ( i64.const 0)) (
      // i64.const 0) ( i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(616395976148874061L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 2810729703362889816) ( i64.const -1))
      // ( i64.const -2810729703362889816) ( i64.const 2810729703362889815))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {
            WasmValue.i64(-2810729703362889816L), WasmValue.i64(2810729703362889815L)
          },
          WasmValue.i64(2810729703362889816L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 1) ( i64.const -1)) ( i64.const -1) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(-1L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "i64.mul_wide_u" ( i64.const 1) ( i64.const 0)) ( i64.const 0) (
      // i64.const 0))
      runner.assertReturn(
          "i64.mul_wide_u",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(1L),
          WasmValue.i64(0L));

      // Compile and instantiate module 2
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/WideArithmeticTest_module2.wat
      final String moduleWat2 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/WideArithmeticTest_module2.wat");
      runner.compileAndInstantiate(moduleWat2);

      // ( assert_return ( invoke "u64::overflowing_add" ( i64.const 0) ( i64.const 0)) ( i64.const
      // 0) ( i64.const 0))
      runner.assertReturn(
          "u64::overflowing_add",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(0L));

      // ( assert_return ( invoke "u64::overflowing_add" ( i64.const 0) ( i64.const 1)) ( i64.const
      // 1) ( i64.const 0))
      runner.assertReturn(
          "u64::overflowing_add",
          new WasmValue[] {WasmValue.i64(1L), WasmValue.i64(0L)},
          WasmValue.i64(0L),
          WasmValue.i64(1L));

      // ( assert_return ( invoke "u64::overflowing_add" ( i64.const 1) ( i64.const -1)) ( i64.const
      // 0) ( i64.const 1))
      runner.assertReturn(
          "u64::overflowing_add",
          new WasmValue[] {WasmValue.i64(0L), WasmValue.i64(1L)},
          WasmValue.i64(1L),
          WasmValue.i64(-1L));

      // ( assert_return ( invoke "u64::overflowing_add" ( i64.const -2) ( i64.const -1)) (
      // i64.const -3) ( i64.const 1))
      runner.assertReturn(
          "u64::overflowing_add",
          new WasmValue[] {WasmValue.i64(-3L), WasmValue.i64(1L)},
          WasmValue.i64(-2L),
          WasmValue.i64(-1L));
    }
  }
}
