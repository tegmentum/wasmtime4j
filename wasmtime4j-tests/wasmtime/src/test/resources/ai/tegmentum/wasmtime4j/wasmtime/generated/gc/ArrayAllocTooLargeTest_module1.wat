(module
  (type $arr_i8 (array i8))
  (type $arr_i64 (array i64))

  ;; Overflow on `elems_size = len * sizeof(elem_type)`
  ;; Returns anyref to avoid typed reference issues
  (func (export "overflow-elems-size")
    (array.new_default $arr_i64 (i32.const -1))
    drop
  )

  ;; Overflow on `base_size + elems_size`
  ;; Returns anyref to avoid typed reference issues
  (func (export "overflow-add-base-size")
    (array.new_default $arr_i8 (i32.const -1))
    drop
  )
)
