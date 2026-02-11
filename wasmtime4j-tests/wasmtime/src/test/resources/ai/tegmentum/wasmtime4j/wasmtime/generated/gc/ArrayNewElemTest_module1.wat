(module
  (type $arr (array i31ref))

  (elem $e i31ref
    (ref.i31 (i32.const 0xaa))
    (ref.i31 (i32.const 0xbb))
    (ref.i31 (i32.const 0xcc))
    (ref.i31 (i32.const 0xdd)))

  (func (export "array-new-elem") (param i32 i32) (result (ref $arr))
    (array.new_elem $arr $e (local.get 0) (local.get 1))
  )
)
