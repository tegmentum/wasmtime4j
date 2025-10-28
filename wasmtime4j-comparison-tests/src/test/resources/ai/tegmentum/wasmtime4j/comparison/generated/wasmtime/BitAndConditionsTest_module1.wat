( module ( func ( export "if_b20") ( param i32) ( result i32) ( i32.and ( local.get 
              0) ( i32.shl ( i32.const 1) ( i32.const 20))) if ( result i32) i32.const 100 else 
                      i32.const 200 end) ( func ( export "select_b20") ( param i32 i32 i32) ( result i32) 
                              local.get 1 local.get 2 ( i32.and ( local.get 0) ( i32.shl ( i32.const 1) ( i32.const 
                                        20))) select) ( func ( export "eqz_b20") ( param i32) ( result i32) ( i32.and ( local.get 
                                                    0) ( i32.shl ( i32.const 1) ( i32.const 20))) i32.eqz) ( func ( export "if_b40") 
                                                              ( param i64) ( result i64) ( i64.and ( local.get 0) ( i64.shl ( i64.const 1) ( i64.const 
                                                                            40))) i64.const 0 i64.ne if ( result i64) i64.const 100 else i64.const 200 end) ( 
                                                                                func ( export "select_b40") ( param i64 i64 i64) ( result i64) local.get 1 local.get 
                                                                                      2 ( i64.and ( local.get 0) ( i64.shl ( i64.const 1) ( i64.const 40))) i64.const 0 
                                                                                                i64.ne select) ( func ( export "eqz_b40") ( param i64) ( result i32) ( i64.and ( 
                                                                                                            local.get 0) ( i64.shl ( i64.const 1) ( i64.const 40))) i64.eqz) ( func ( export 
                                                                                                                      "if_bit32") ( param i32 i32) ( result i32) ( i32.and ( local.get 0) ( i32.shl ( i32.const 
                                                                                                                                  1) ( local.get 1))) if ( result i32) i32.const 100 else i32.const 200 end) ( func 
                                                                                                                                        ( export "if_bit64") ( param i64 i64) ( result i64) ( i64.and ( local.get 0) ( i64.shl 
                                                                                                                                                    ( i64.const 1) ( local.get 1))) i64.const 0 i64.ne if ( result i64) i64.const 100 
                                                                                                                                                          else i64.const 200 end))