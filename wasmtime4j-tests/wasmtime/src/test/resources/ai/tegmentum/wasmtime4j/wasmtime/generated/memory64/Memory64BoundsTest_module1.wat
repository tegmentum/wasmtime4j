(module
  (memory i64 1)

  (func (export "copy") (param i64 i64 i64)
      local.get 0
      local.get 1
      local.get 2
      memory.copy)

  (func (export "fill") (param i64 i32 i64)
      local.get 0
      local.get 1
      local.get 2
      memory.fill)

  (func (export "init") (param i64 i32 i32)
      local.get 0
      local.get 1
      local.get 2
      memory.init 0)

  (data "1234")
)
