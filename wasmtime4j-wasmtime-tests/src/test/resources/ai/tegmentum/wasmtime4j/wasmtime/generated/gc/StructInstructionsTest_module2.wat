(module
  (type $t (struct (field (mut i32) (mut i16))))

  (func (export "struct.get-null")
    (drop (struct.get $t 0 (ref.null $t)))
  )

  (func (export "struct.get_s-null")
    (drop (struct.get_s $t 1 (ref.null $t)))
  )

  (func (export "struct.get_u-null")
    (drop (struct.get_u $t 1 (ref.null $t)))
  )

  (func (export "struct.set-null")
    (struct.set $t 0 (ref.null $t) (i32.const 0))
  )
)
