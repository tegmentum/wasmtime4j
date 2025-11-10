(module
  ;; () -> void
  (func $noop
    nop)
  (export "noop" (func $noop))

  ;; (i32) -> void - side effect operation
  (func $consume_i32 (param $x i32)
    ;; Just consume the parameter, no return
    nop)
  (export "consume_i32" (func $consume_i32))

  ;; (i32, i32) -> void - side effect operation
  (func $consume_i32i32 (param $a i32) (param $b i32)
    ;; Just consume the parameters, no return
    nop)
  (export "consume_i32i32" (func $consume_i32i32))

  ;; (i64) -> void - side effect operation
  (func $consume_i64 (param $x i64)
    ;; Just consume the parameter, no return
    nop)
  (export "consume_i64" (func $consume_i64))

  ;; (i64, i64) -> void - side effect operation
  (func $consume_i64i64 (param $a i64) (param $b i64)
    ;; Just consume the parameters, no return
    nop)
  (export "consume_i64i64" (func $consume_i64i64))

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

  ;; (f32, f32) -> f32
  (func $add_f32 (param $a f32) (param $b f32) (result f32)
    local.get $a
    local.get $b
    f32.add)
  (export "add_f32" (func $add_f32))

  ;; (f64, f64) -> f64
  (func $add_f64 (param $a f64) (param $b f64) (result f64)
    local.get $a
    local.get $b
    f64.add)
  (export "add_f64" (func $add_f64))

  ;; (i32, i32, i32) -> i32
  (func $add_three_i32 (param $a i32) (param $b i32) (param $c i32) (result i32)
    local.get $a
    local.get $b
    i32.add
    local.get $c
    i32.add)
  (export "add_three_i32" (func $add_three_i32))

  ;; (i64, i64, i64) -> i64
  (func $add_three_i64 (param $a i64) (param $b i64) (param $c i64) (result i64)
    local.get $a
    local.get $b
    i64.add
    local.get $c
    i64.add)
  (export "add_three_i64" (func $add_three_i64))

  ;; (f32, f32, f32) -> f32 - fused multiply-add: a * b + c
  (func $fma_f32 (param $a f32) (param $b f32) (param $c f32) (result f32)
    local.get $a
    local.get $b
    f32.mul
    local.get $c
    f32.add)
  (export "fma_f32" (func $fma_f32))

  ;; (f64, f64, f64) -> f64 - fused multiply-add: a * b + c
  (func $fma_f64 (param $a f64) (param $b f64) (param $c f64) (result f64)
    local.get $a
    local.get $b
    f64.mul
    local.get $c
    f64.add)
  (export "fma_f64" (func $fma_f64))

  ;; (i32, i32) -> i64 - combine two i32s into i64 (high << 32 | low)
  (func $combine_i32 (param $high i32) (param $low i32) (result i64)
    ;; Convert high to i64, shift left 32 bits
    local.get $high
    i64.extend_i32_u
    i64.const 32
    i64.shl
    ;; Convert low to i64
    local.get $low
    i64.extend_i32_u
    ;; OR them together
    i64.or)
  (export "combine_i32" (func $combine_i32))

  ;; (i64) -> i32 - extract low 32 bits
  (func $truncate_i64 (param $x i64) (result i32)
    local.get $x
    i32.wrap_i64)
  (export "truncate_i64" (func $truncate_i64))

  ;; (i32, f32) -> f32 - multiply i32 by f32 (convert i32 to f32 first)
  (func $array_op_if (param $index i32) (param $value f32) (result f32)
    local.get $index
    f32.convert_i32_s
    local.get $value
    f32.mul)
  (export "array_op_if" (func $array_op_if))

  ;; (f32, i32) -> f32 - raise f32 to i32 power (simplified: multiply n times)
  (func $power_fi (param $base f32) (param $exp i32) (result f32)
    (local $result f32)
    (local $i i32)

    ;; Initialize result to 1.0
    f32.const 1.0
    local.set $result

    ;; If exponent is 0, return 1.0
    local.get $exp
    i32.eqz
    if
      f32.const 1.0
      return
    end

    ;; Handle negative exponents by using absolute value
    local.get $exp
    i32.const 0
    i32.lt_s
    if
      ;; For negative exponents, we'll just multiply the positive times
      ;; (simplified implementation)
      local.get $exp
      i32.const -1
      i32.mul
      local.set $exp
    end

    ;; Multiply base exp times
    i32.const 0
    local.set $i

    (block $break
      (loop $continue
        local.get $i
        local.get $exp
        i32.ge_s
        br_if $break

        local.get $result
        local.get $base
        f32.mul
        local.set $result

        local.get $i
        i32.const 1
        i32.add
        local.set $i

        br $continue
      )
    )

    local.get $result)
  (export "power_fi" (func $power_fi))
)
