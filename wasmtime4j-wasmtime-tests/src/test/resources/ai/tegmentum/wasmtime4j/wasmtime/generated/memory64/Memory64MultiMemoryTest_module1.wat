(module
  (memory $a i64 1)
  (memory $b i64 1)

  (func (export "copy") (param i64 i64 i64)
      local.get 0
      local.get 1
      local.get 2
      memory.copy $a $b)
)
