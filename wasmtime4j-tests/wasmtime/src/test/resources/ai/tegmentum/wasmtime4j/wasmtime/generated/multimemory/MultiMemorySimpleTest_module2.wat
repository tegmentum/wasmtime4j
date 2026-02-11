(module
  (memory $m1 1)
  (memory $m2 2)

  (func (export "grow1") (param i32) (result i32)
      local.get 0
      memory.grow $m1)

  (func (export "grow2") (param i32) (result i32)
      local.get 0
      memory.grow $m2)

  (func (export "size1") (result i32) memory.size $m1)
  (func (export "size2") (result i32) memory.size $m2)
)
