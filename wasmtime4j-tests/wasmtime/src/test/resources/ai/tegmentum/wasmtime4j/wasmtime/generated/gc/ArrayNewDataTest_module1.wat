(module
  (type $arr (array (mut i8)))

  (data $d "abcd")

  (func (export "array-new-data") (param i32 i32) (result (ref $arr))
    (array.new_data $arr $d (local.get 0) (local.get 1))
  )
)
