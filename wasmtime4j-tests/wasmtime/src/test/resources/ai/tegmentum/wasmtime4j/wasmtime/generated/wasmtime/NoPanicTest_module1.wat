(module
  (func $test (param i32) (result externref)
        i32.const 0
        if
        else
        end
        local.get 0
        table.get 0
  )
  (table 4 externref)
  (export "test" (func $test))
)
