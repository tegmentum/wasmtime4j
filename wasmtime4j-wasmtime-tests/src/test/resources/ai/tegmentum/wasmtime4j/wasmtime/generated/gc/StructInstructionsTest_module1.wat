(module
  (type $ty (struct (field (mut f32))
                    (field (mut i8))
                    (field (mut anyref))))

  (global $g (mut (ref null $ty)) (ref.null $ty))

  ;; Constructors.

  (func $new-default (result (ref $ty))
    (struct.new_default $ty)
  )
  (func (export "new-default")
    (global.set $g (call $new-default))
  )

  ;; Getters.

  (func $get-f32 (param (ref null $ty)) (result f32)
    (struct.get $ty 0 (local.get 0))
  )
  (func (export "get-f32") (result f32)
    (call $get-f32 (global.get $g))
  )

  (func $get-s-i8 (param (ref null $ty)) (result i32)
    (struct.get_s $ty 1 (local.get 0))
  )
  (func (export "get-s-i8") (result i32)
    (call $get-s-i8 (global.get $g))
  )

  (func $get-u-i8 (param (ref null $ty)) (result i32)
    (struct.get_u $ty 1 (local.get 0))
  )
  (func (export "get-u-i8") (result i32)
    (call $get-u-i8 (global.get $g))
  )

  ;; Setters.

  (func $set-f32 (param (ref null $ty) f32)
    (struct.set $ty 0 (local.get 0) (local.get 1))
  )
  (func (export "set-f32") (param f32)
    (call $set-f32 (global.get $g) (local.get 0))
  )

  (func $set-i8 (param (ref null $ty) i32)
    (struct.set $ty 1 (local.get 0) (local.get 1))
  )
  (func (export "set-i8") (param i32)
    (call $set-i8 (global.get $g) (local.get 0))
  )
)
