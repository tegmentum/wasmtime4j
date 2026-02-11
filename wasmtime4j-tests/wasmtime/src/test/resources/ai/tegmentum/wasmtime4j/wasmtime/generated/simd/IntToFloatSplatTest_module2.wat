(module
  (func (export "g") (result v128)
    ;; Splat constant 0 across all 4 lanes of i32x4
    (i32x4.splat (i32.const 0))
    ;; Convert the low 2 i32 lanes to unsigned f64
    (f64x2.convert_low_i32x4_u)
  )
)
