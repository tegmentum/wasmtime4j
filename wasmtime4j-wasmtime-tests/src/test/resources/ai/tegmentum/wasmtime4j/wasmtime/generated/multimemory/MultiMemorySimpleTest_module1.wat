(module
  (memory $m1 1)
  (memory $m2 1)

  (func (export "store1") (param i32 i64)
      local.get 0
      local.get 1
      i64.store $m1)

  (func (export "store2") (param i32 i64)
      local.get 0
      local.get 1
      i64.store $m2)

  (func (export "load1") (param i32) (result i64)
      local.get 0
      i64.load $m1)

  (func (export "load2") (param i32) (result i64)
      local.get 0
      i64.load $m2)
)
