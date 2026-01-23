(module
    (func (export "i16x8.extend_low_i8x16_s") (param v128) (result v128)
        local.get 0
        i16x8.extend_low_i8x16_s
        f32x4.convert_i32x4_u)
    (func (export "i16x8.extend_low_i8x16_u") (param v128) (result v128)
        local.get 0
        i16x8.extend_low_i8x16_u
        f32x4.convert_i32x4_u)
    (func (export "i32x4.extend_low_i16x8_s") (param v128) (result v128)
        local.get 0
        i32x4.extend_low_i16x8_s
        f32x4.convert_i32x4_u)
    (func (export "i32x4.extend_low_i16x8_u") (param v128) (result v128)
        local.get 0
        i32x4.extend_low_i16x8_u
        f32x4.convert_i32x4_u)
    (func (export "i64x2.extend_low_i32x4_s") (param v128) (result v128)
        local.get 0
        i64x2.extend_low_i32x4_s
        f32x4.convert_i32x4_u)
    (func (export "i64x2.extend_low_i32x4_u") (param v128) (result v128)
        local.get 0
        i64x2.extend_low_i32x4_u
        f32x4.convert_i32x4_u)
)
