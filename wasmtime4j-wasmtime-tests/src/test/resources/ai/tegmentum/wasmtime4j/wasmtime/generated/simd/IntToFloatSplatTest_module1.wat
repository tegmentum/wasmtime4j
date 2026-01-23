(module
  (func (export "f") (param i32) (result v128)
    ;; Splat the i32 parameter across all 4 lanes of i32x4
    (i32x4.splat (local.get 0))
    ;; Convert the low 2 i32 lanes to unsigned f64
    (f64x2.convert_low_i32x4_u)
  )
)
