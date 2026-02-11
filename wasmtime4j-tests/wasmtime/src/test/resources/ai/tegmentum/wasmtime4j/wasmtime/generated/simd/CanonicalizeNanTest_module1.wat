(module
  (func (export "f32x4.floor") (param v128) (result v128)
    local.get 0
    f32x4.floor)
  (func (export "f32x4.nearest") (param v128) (result v128)
    local.get 0
    f32x4.nearest)
  (func (export "f32x4.sqrt") (param v128) (result v128)
    local.get 0
    f32x4.sqrt)
  (func (export "f32x4.trunc") (param v128) (result v128)
    local.get 0
    f32x4.trunc)
  (func (export "f32x4.ceil") (param v128) (result v128)
    local.get 0
    f32x4.ceil)

  (func (export "f64x2.floor") (param v128) (result v128)
    local.get 0
    f64x2.floor)
  (func (export "f64x2.nearest") (param v128) (result v128)
    local.get 0
    f64x2.nearest)
  (func (export "f64x2.sqrt") (param v128) (result v128)
    local.get 0
    f64x2.sqrt)
  (func (export "f64x2.trunc") (param v128) (result v128)
    local.get 0
    f64x2.trunc)
  (func (export "f64x2.ceil") (param v128) (result v128)
    local.get 0
    f64x2.ceil)

  (func (export "reinterpret-and-demote") (param i64) (result i32)
    local.get 0
    f64.reinterpret_i64
    f32.demote_f64
    i32.reinterpret_f32)

  (func (export "reinterpret-and-promote") (param i32) (result i64)
    local.get 0
    f32.reinterpret_i32
    f64.promote_f32
    i64.reinterpret_f64)

  (func (export "copysign-and-demote") (param f64) (result f32)
    local.get 0
    f64.const -0x1
    f64.copysign
    f32.demote_f64)

  (func (export "copysign-and-promote") (param f32) (result f64)
    local.get 0
    f32.const -0x1
    f32.copysign
    f64.promote_f32)

  (func (export "f32x4.demote_f64x2_zero") (param v128) (result v128)
    local.get 0
    f32x4.demote_f64x2_zero)

  (func (export "f64x2.promote_low_f32x4") (param v128) (result v128)
    local.get 0
    f64x2.promote_low_f32x4)
)
