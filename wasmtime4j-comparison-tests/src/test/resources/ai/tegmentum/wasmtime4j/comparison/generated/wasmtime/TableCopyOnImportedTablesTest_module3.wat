( module $o ( table $t ( import "m" "t") 6 funcref) ( table $u ( import "n" "u") 
          6 funcref) ( func ( export "copy_into_t_from_u_2") ( param i32 i32 i32) local.get 
                0 local.get 1 local.get 2 table.copy $t $u) ( func ( export "copy_into_u_from_t_2") 
                    ( param i32 i32 i32) local.get 0 local.get 1 local.get 2 table.copy $u $t) ( func 
                        ( export "call_t_2") ( param i32 i32 i32 i32 i32 i32 i32) ( result i32) local.get 
                              0 local.get 1 local.get 2 local.get 3 local.get 4 local.get 5 local.get 6 call_indirect 
                              $t ( param i32 i32 i32 i32 i32 i32) ( result i32)) ( func ( export "call_u_2") ( 
                                        param i32 i32 i32 i32 i32 i32 i32) ( result i32) local.get 0 local.get 1 local.get 
                                          2 local.get 3 local.get 4 local.get 5 local.get 6 call_indirect $u ( param i32 i32 
                                            i32 i32 i32 i32) ( result i32)))