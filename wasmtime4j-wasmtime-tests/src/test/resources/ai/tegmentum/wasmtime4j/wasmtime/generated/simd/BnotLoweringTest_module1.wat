(module
  (func $v128_not (export "v128_not") (result v128)
    v128.const f32x4 0 0 0 0
    f32x4.abs
    v128.not)
)
