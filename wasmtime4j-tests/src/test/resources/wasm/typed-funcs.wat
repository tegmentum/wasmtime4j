(module
  ;; () -> void
  (func $noop
    nop)
  (export "noop" (func $noop))

  ;; (i32) -> i32
  (func $increment_i32 (param $x i32) (result i32)
    local.get $x
    i32.const 1
    i32.add)
  (export "increment_i32" (func $increment_i32))

  ;; (i32, i32) -> i32
  (func $add_i32 (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.add)
  (export "add_i32" (func $add_i32))

  ;; (i64) -> i64
  (func $increment_i64 (param $x i64) (result i64)
    local.get $x
    i64.const 1
    i64.add)
  (export "increment_i64" (func $increment_i64))

  ;; (i64, i64) -> i64
  (func $add_i64 (param $a i64) (param $b i64) (result i64)
    local.get $a
    local.get $b
    i64.add)
  (export "add_i64" (func $add_i64))

  ;; (f32) -> f32
  (func $negate_f32 (param $x f32) (result f32)
    local.get $x
    f32.neg)
  (export "negate_f32" (func $negate_f32))

  ;; (f64) -> f64
  (func $negate_f64 (param $x f64) (result f64)
    local.get $x
    f64.neg)
  (export "negate_f64" (func $negate_f64))
)
