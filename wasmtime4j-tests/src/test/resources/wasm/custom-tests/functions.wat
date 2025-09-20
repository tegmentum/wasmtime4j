(module
  (func $add (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.add)

  (func $multiply (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.mul)

  (func $factorial (param $n i32) (result i32)
    (local $result i32)
    i32.const 1
    local.set $result

    (loop $loop
      local.get $n
      i32.const 1
      i32.gt_s
      if
        local.get $result
        local.get $n
        i32.mul
        local.set $result

        local.get $n
        i32.const 1
        i32.sub
        local.set $n
        br $loop
      end
    )

    local.get $result)

  (export "add" (func $add))
  (export "multiply" (func $multiply))
  (export "factorial" (func $factorial))
)