(module
  (type $arr (array (mut i8)))

  (data $d "abcd")

  (func (export "array-init-data") (param $arr_len i32)
                                   (param $dst i32)
                                   (param $src i32)
                                   (param $data_len i32) (result (ref $arr))
    (local $a (ref $arr))
    (local.set $a (array.new_default $arr (local.get $arr_len)))
    (array.init_data $arr $d (local.get $a) (local.get $dst) (local.get $src) (local.get $data_len))
    (local.get $a)
  )
)
