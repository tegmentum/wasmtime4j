(module
  (type (func (param i32) (result i32)))
  (func (type 0) (param i32) (result i32)
    local.get 0
    i32x4.splat
    f64x2.abs
    v128.not
    i64x2.bitmask)
  (export "1" (func 0)))
