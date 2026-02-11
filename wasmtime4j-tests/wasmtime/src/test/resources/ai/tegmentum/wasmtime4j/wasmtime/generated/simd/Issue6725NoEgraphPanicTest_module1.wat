(module
  (func (param v128) (result v128)
    (i8x16.eq (local.get 0) (local.get 0))
    (i8x16.ne (local.get 0) (local.get 0))
    v128.or
  )
)
