(module
  (table $t 1 externref)

  (func (export "get-externref") (param $i i32) (result externref)
    (table.get $t (local.get $i))
  )

  (func (export "set-externref") (param $i i32) (param $r externref)
    (table.set $t (local.get $i) (local.get $r))
  )

  (func (export "is-null-externref") (param $i i32) (result i32)
    (ref.is_null (table.get $t (local.get $i)))
  )

  (func (export "set-externref-trap") (param $i i32)
    (table.set $t (local.get $i) (ref.null extern))
  )
)
