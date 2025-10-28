( module ( func ( export "func_is_null") ( param funcref) ( result i32) ( ref.is_null 
            ( local.get 0))) ( func ( export "func_is_null_with_non_null_funcref") ( result i32) 
                    ( call 0 ( ref.func 0))) ( func ( export "extern_is_null") ( param externref) ( result 
                                i32) ( ref.is_null ( local.get 0))))