(module
  (memory $a i32 1)
  (memory $b i64 1)

  (func (export "copy") (param i32 i64 i32)
      local.get 0
      local.get 1
      local.get 2
      memory.copy $a $b)
)
