( module ( global $g ( mut externref) ( ref.null extern)) ( func $has_a_stack_map 
          ( local externref) global.get $g local.tee 0 global.set $g local.get 0 global.set 
            $g ref.null extern global.set $g) ( func ( export "run") ( result i32) call $gc ref.null 
                  extern global.set $g i32.const 0) ( func ( export "init") ( param externref) local.get 
                        0 global.set $g) ( func $gc ( local $i i32) i32.const 10000 local.set $i ( loop $continue 
                              ( global.set $g ( global.get $g)) ( local.tee $i ( i32.sub ( local.get $i) ( i32.const 
                                          1))) br_if $continue)))