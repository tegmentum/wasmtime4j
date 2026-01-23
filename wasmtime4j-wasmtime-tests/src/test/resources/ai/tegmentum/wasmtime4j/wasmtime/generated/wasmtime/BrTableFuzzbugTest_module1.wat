(module
  ;; Simple br_table test - returns the index clamped to [0, 3]
  ;; Default case (index >= 4) returns 3
  (func $main (export "main") (param i32 i32 i32) (result i32)
    (local $result i32)
    ;; Set default result
    i32.const 3
    local.set $result

    block $done
      block $case3
        block $case2
          block $case1
            block $case0
              local.get 0
              br_table $case0 $case1 $case2 $case3 $done
            end ;; $case0
            i32.const 0
            local.set $result
            br $done
          end ;; $case1
          i32.const 1
          local.set $result
          br $done
        end ;; $case2
        i32.const 2
        local.set $result
        br $done
      end ;; $case3
      i32.const 3
      local.set $result
    end ;; $done
    local.get $result
  )
)
