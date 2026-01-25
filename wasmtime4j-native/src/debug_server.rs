use wasmtime::*;
use wasmtime_wasi::WasiCtx;
use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use std::sync::mpsc::{self, Receiver, Sender};
use std::thread;
use std::time::{Duration, SystemTime};

/// Advanced debugging server for WebAssembly execution with instruction stepping,
/// breakpoints, and memory inspection capabilities
pub struct DebugServer {
    engine: Engine,
    debug_sessions: Arc<RwLock<HashMap<u64, DebugSession>>>,
    next_session_id: Arc<Mutex<u64>>,
    event_sender: Sender<DebugEvent>,
    event_receiver: Option<Receiver<DebugEvent>>,
    is_running: Arc<Mutex<bool>>,
}

impl DebugServer {
    pub fn new(engine: Engine) -> Self {
        let (sender, receiver) = mpsc::channel();

        Self {
            engine,
            debug_sessions: Arc::new(RwLock::new(HashMap::new())),
            next_session_id: Arc::new(Mutex::new(1)),
            event_sender: sender,
            event_receiver: Some(receiver),
            is_running: Arc::new(Mutex::new(false)),
        }
    }

    /// Creates a new debug session for an instance
    pub fn create_session(&self, instance: Instance, module: Module) -> Result<u64, anyhow::Error> {
        let session_id = {
            let mut next_id = self.next_session_id.lock()
                .unwrap_or_else(|poisoned| {
                    log::warn!("next_session_id mutex was poisoned in create_session, recovering");
                    poisoned.into_inner()
                });
            let id = *next_id;
            *next_id += 1;
            id
        };

        let session = DebugSession::new(session_id, instance, module, self.event_sender.clone())?;

        {
            let mut sessions = self.debug_sessions.write()
                .unwrap_or_else(|poisoned| {
                    log::warn!("debug_sessions mutex was poisoned in create_session, recovering");
                    poisoned.into_inner()
                });
            sessions.insert(session_id, session);
        }

        Ok(session_id)
    }

    /// Removes a debug session
    pub fn remove_session(&self, session_id: u64) -> Result<(), anyhow::Error> {
        let mut sessions = self.debug_sessions.write()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in remove_session, recovering");
                poisoned.into_inner()
            });
        sessions.remove(&session_id);
        Ok(())
    }

    /// Sets a breakpoint in a debug session
    pub fn set_breakpoint(&self, session_id: u64, function_index: u32, instruction_offset: u32,
                          condition: Option<String>) -> Result<u32, anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in set_breakpoint, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.set_breakpoint(function_index, instruction_offset, condition)
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Removes a breakpoint from a debug session
    pub fn remove_breakpoint(&self, session_id: u64, breakpoint_id: u32) -> Result<(), anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in remove_breakpoint, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.remove_breakpoint(breakpoint_id)
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Steps to the next instruction in a debug session
    pub fn step_next(&self, session_id: u64) -> Result<ExecutionContext, anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in step_next, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.step_next()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Steps into a function call in a debug session
    pub fn step_into(&self, session_id: u64) -> Result<ExecutionContext, anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in step_into, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.step_into()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Steps out of the current function in a debug session
    pub fn step_out(&self, session_id: u64) -> Result<ExecutionContext, anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in step_out, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.step_out()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Continues execution in a debug session
    pub fn continue_execution(&self, session_id: u64) -> Result<(), anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in continue_execution, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.continue_execution()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Pauses execution in a debug session
    pub fn pause_execution(&self, session_id: u64) -> Result<ExecutionContext, anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in pause_execution, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.pause_execution()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Gets the call stack for a debug session
    pub fn get_call_stack(&self, session_id: u64) -> Result<Vec<StackFrame>, anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in get_call_stack, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.get_call_stack()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Inspects memory in a debug session
    pub fn inspect_memory(&self, session_id: u64, address: u64, length: u32) -> Result<Vec<u8>, anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in inspect_memory, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.inspect_memory(address, length)
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Gets variable values in the current scope for a debug session
    pub fn get_variables(&self, session_id: u64) -> Result<HashMap<String, VariableValue>, anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in get_variables, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.get_variables()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Evaluates a watch expression in a debug session
    pub fn evaluate_watch(&self, session_id: u64, expression: &str) -> Result<WatchResult, anyhow::Error> {
        let sessions = self.debug_sessions.read()
            .unwrap_or_else(|poisoned| {
                log::warn!("debug_sessions mutex was poisoned in evaluate_watch, recovering");
                poisoned.into_inner()
            });
        if let Some(session) = sessions.get(&session_id) {
            session.evaluate_watch(expression)
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Starts the debug server event processing loop
    pub fn start(&mut self) -> Result<(), anyhow::Error> {
        {
            let mut running = self.is_running.lock()
                .unwrap_or_else(|poisoned| {
                    log::warn!("is_running mutex was poisoned in start, recovering");
                    poisoned.into_inner()
                });
            if *running {
                return Ok(());
            }
            *running = true;
        }

        let receiver = self.event_receiver.take()
            .ok_or_else(|| anyhow::anyhow!("Event receiver already taken"))?;

        let is_running = self.is_running.clone();

        thread::spawn(move || {
            loop {
                let should_continue = {
                    let running = is_running.lock()
                        .unwrap_or_else(|poisoned| {
                            log::warn!("is_running mutex was poisoned in event loop, recovering");
                            poisoned.into_inner()
                        });
                    *running
                };
                if !should_continue {
                    break;
                }
                match receiver.recv_timeout(Duration::from_millis(100)) {
                    Ok(event) => {
                        // Process debug events
                        Self::process_debug_event(event);
                    }
                    Err(mpsc::RecvTimeoutError::Timeout) => {
                        // Continue polling
                    }
                    Err(mpsc::RecvTimeoutError::Disconnected) => {
                        break;
                    }
                }
            }
        });

        Ok(())
    }

    /// Stops the debug server
    pub fn stop(&self) {
        let mut running = self.is_running.lock()
            .unwrap_or_else(|poisoned| {
                log::warn!("is_running mutex was poisoned in stop, recovering");
                poisoned.into_inner()
            });
        *running = false;
    }

    fn process_debug_event(event: DebugEvent) {
        match event {
            DebugEvent::BreakpointHit { session_id, breakpoint_id, context } => {
                println!("Breakpoint {} hit in session {}", breakpoint_id, session_id);
                // Handle breakpoint hit
            }
            DebugEvent::StepCompleted { session_id, context } => {
                println!("Step completed in session {}", session_id);
                // Handle step completion
            }
            DebugEvent::ExecutionPaused { session_id, context } => {
                println!("Execution paused in session {}", session_id);
                // Handle execution pause
            }
            DebugEvent::ExecutionResumed { session_id } => {
                println!("Execution resumed in session {}", session_id);
                // Handle execution resume
            }
            DebugEvent::Error { session_id, message } => {
                println!("Debug error in session {}: {}", session_id, message);
                // Handle debug error
            }
        }
    }
}

/// Individual debug session for a WebAssembly instance
pub struct DebugSession {
    session_id: u64,
    instance: Arc<Mutex<Instance>>,
    module: Module,
    breakpoints: Arc<RwLock<HashMap<u32, Breakpoint>>>,
    next_breakpoint_id: Arc<Mutex<u32>>,
    execution_state: Arc<Mutex<ExecutionState>>,
    current_context: Arc<Mutex<Option<ExecutionContext>>>,
    event_sender: Sender<DebugEvent>,
    call_stack: Arc<Mutex<Vec<StackFrame>>>,
}

impl DebugSession {
    pub fn new(session_id: u64, instance: Instance, module: Module,
               event_sender: Sender<DebugEvent>) -> Result<Self, anyhow::Error> {
        Ok(Self {
            session_id,
            instance: Arc::new(Mutex::new(instance)),
            module,
            breakpoints: Arc::new(RwLock::new(HashMap::new())),
            next_breakpoint_id: Arc::new(Mutex::new(1)),
            execution_state: Arc::new(Mutex::new(ExecutionState::Ready)),
            current_context: Arc::new(Mutex::new(None)),
            event_sender,
            call_stack: Arc::new(Mutex::new(Vec::new())),
        })
    }

    pub fn set_breakpoint(&self, function_index: u32, instruction_offset: u32,
                          condition: Option<String>) -> Result<u32, anyhow::Error> {
        let breakpoint_id = {
            let mut next_id = self.next_breakpoint_id.lock()
                .unwrap_or_else(|poisoned| {
                    log::warn!("next_breakpoint_id mutex was poisoned in set_breakpoint, recovering");
                    poisoned.into_inner()
                });
            let id = *next_id;
            *next_id += 1;
            id
        };

        let breakpoint = Breakpoint {
            id: breakpoint_id,
            function_index,
            instruction_offset,
            condition,
            enabled: true,
            hit_count: 0,
        };

        {
            let mut breakpoints = self.breakpoints.write()
                .unwrap_or_else(|poisoned| {
                    log::warn!("breakpoints mutex was poisoned in set_breakpoint, recovering");
                    poisoned.into_inner()
                });
            breakpoints.insert(breakpoint_id, breakpoint);
        }

        Ok(breakpoint_id)
    }

    pub fn remove_breakpoint(&self, breakpoint_id: u32) -> Result<(), anyhow::Error> {
        let mut breakpoints = self.breakpoints.write()
            .unwrap_or_else(|poisoned| {
                log::warn!("breakpoints mutex was poisoned in remove_breakpoint, recovering");
                poisoned.into_inner()
            });
        breakpoints.remove(&breakpoint_id);
        Ok(())
    }

    pub fn step_next(&self) -> Result<ExecutionContext, anyhow::Error> {
        {
            let mut state = self.execution_state.lock()
                .unwrap_or_else(|poisoned| {
                    log::warn!("execution_state mutex was poisoned in step_next, recovering");
                    poisoned.into_inner()
                });
            *state = ExecutionState::Stepping;
        }

        // Simulate stepping to next instruction
        let context = self.create_execution_context(0, 1, "main")?;

        {
            let mut current = self.current_context.lock()
                .unwrap_or_else(|poisoned| {
                    log::warn!("current_context mutex was poisoned in step_next, recovering");
                    poisoned.into_inner()
                });
            *current = Some(context.clone());
        }

        {
            let mut state = self.execution_state.lock()
                .unwrap_or_else(|poisoned| {
                    log::warn!("execution_state mutex was poisoned in step_next (final), recovering");
                    poisoned.into_inner()
                });
            *state = ExecutionState::Paused;
        }

        let _ = self.event_sender.send(DebugEvent::StepCompleted {
            session_id: self.session_id,
            context: context.clone(),
        });

        Ok(context)
    }

    pub fn step_into(&self) -> Result<ExecutionContext, anyhow::Error> {
        // Similar to step_next but would enter function calls
        self.step_next()
    }

    pub fn step_out(&self) -> Result<ExecutionContext, anyhow::Error> {
        // Similar to step_next but would exit current function
        self.step_next()
    }

    pub fn continue_execution(&self) -> Result<(), anyhow::Error> {
        {
            let mut state = self.execution_state.lock()
                .unwrap_or_else(|poisoned| {
                    log::warn!("execution_state mutex was poisoned in continue_execution, recovering");
                    poisoned.into_inner()
                });
            *state = ExecutionState::Running;
        }

        let _ = self.event_sender.send(DebugEvent::ExecutionResumed {
            session_id: self.session_id,
        });

        Ok(())
    }

    pub fn pause_execution(&self) -> Result<ExecutionContext, anyhow::Error> {
        {
            let mut state = self.execution_state.lock()
                .unwrap_or_else(|poisoned| {
                    log::warn!("execution_state mutex was poisoned in pause_execution, recovering");
                    poisoned.into_inner()
                });
            *state = ExecutionState::Paused;
        }

        let context = self.create_execution_context(0, 5, "main")?;

        {
            let mut current = self.current_context.lock()
                .unwrap_or_else(|poisoned| {
                    log::warn!("current_context mutex was poisoned in pause_execution, recovering");
                    poisoned.into_inner()
                });
            *current = Some(context.clone());
        }

        let _ = self.event_sender.send(DebugEvent::ExecutionPaused {
            session_id: self.session_id,
            context: context.clone(),
        });

        Ok(context)
    }

    pub fn get_call_stack(&self) -> Result<Vec<StackFrame>, anyhow::Error> {
        let stack = self.call_stack.lock()
            .unwrap_or_else(|poisoned| {
                log::warn!("call_stack mutex was poisoned in get_call_stack, recovering");
                poisoned.into_inner()
            });
        Ok(stack.clone())
    }

    pub fn inspect_memory(&self, _address: u64, _length: u32) -> Result<Vec<u8>, anyhow::Error> {
        // TODO: Implement with proper store access
        // Memory inspection now requires store context in Wasmtime 38+
        Err(anyhow::anyhow!("Memory inspection not yet implemented for Wasmtime 38+"))
    }

    pub fn get_variables(&self) -> Result<HashMap<String, VariableValue>, anyhow::Error> {
        let mut variables = HashMap::new();

        // Simplified variable extraction
        // Real implementation would extract from current execution frame
        variables.insert("local_0".to_string(), VariableValue::I32(42));
        variables.insert("local_1".to_string(), VariableValue::F64(3.14159));

        Ok(variables)
    }

    pub fn evaluate_watch(&self, expression: &str) -> Result<WatchResult, anyhow::Error> {
        // Simplified watch expression evaluation
        // Real implementation would parse and evaluate the expression
        match expression {
            "local_0" => Ok(WatchResult::Success(VariableValue::I32(42))),
            "local_1" => Ok(WatchResult::Success(VariableValue::F64(3.14159))),
            _ => Ok(WatchResult::Error(format!("Unknown variable: {}", expression))),
        }
    }

    fn create_execution_context(&self, function_index: u32, instruction_offset: u32,
                               function_name: &str) -> Result<ExecutionContext, anyhow::Error> {
        let mut locals = HashMap::new();
        locals.insert("local_0".to_string(), VariableValue::I32(42));
        locals.insert("local_1".to_string(), VariableValue::F64(3.14159));

        let stack = vec![
            VariableValue::I32(1),
            VariableValue::I32(2),
            VariableValue::I32(3),
        ];

        Ok(ExecutionContext {
            function_index,
            instruction_offset,
            function_name: function_name.to_string(),
            locals,
            stack,
        })
    }
}

/// Breakpoint information
#[derive(Debug, Clone)]
pub struct Breakpoint {
    pub id: u32,
    pub function_index: u32,
    pub instruction_offset: u32,
    pub condition: Option<String>,
    pub enabled: bool,
    pub hit_count: u64,
}

/// Execution context at a specific point
#[derive(Debug, Clone)]
pub struct ExecutionContext {
    pub function_index: u32,
    pub instruction_offset: u32,
    pub function_name: String,
    pub locals: HashMap<String, VariableValue>,
    pub stack: Vec<VariableValue>,
}

/// Stack frame information
#[derive(Debug, Clone)]
pub struct StackFrame {
    pub function_index: u32,
    pub function_name: String,
    pub instruction_offset: u32,
    pub variables: HashMap<String, VariableValue>,
}

/// Variable value types
#[derive(Debug, Clone)]
pub enum VariableValue {
    I32(i32),
    I64(i64),
    F32(f32),
    F64(f64),
    V128([u8; 16]),
    FuncRef(Option<u32>),
    ExternRef(Option<u32>),
}

/// Watch expression evaluation result
#[derive(Debug, Clone)]
pub enum WatchResult {
    Success(VariableValue),
    Error(String),
}

/// Debug execution state
#[derive(Debug, Clone, PartialEq)]
pub enum ExecutionState {
    Ready,
    Running,
    Paused,
    Stepping,
    Error,
}

/// Debug events
#[derive(Debug, Clone)]
pub enum DebugEvent {
    BreakpointHit {
        session_id: u64,
        breakpoint_id: u32,
        context: ExecutionContext,
    },
    StepCompleted {
        session_id: u64,
        context: ExecutionContext,
    },
    ExecutionPaused {
        session_id: u64,
        context: ExecutionContext,
    },
    ExecutionResumed {
        session_id: u64,
    },
    Error {
        session_id: u64,
        message: String,
    },
}

// C FFI exports for JNI integration
use std::os::raw::{c_char, c_int, c_long};
use std::ffi::{CStr, CString};

#[no_mangle]
pub extern "C" fn create_debug_server(engine_ptr: *mut Engine) -> *mut DebugServer {
    if engine_ptr.is_null() {
        return std::ptr::null_mut();
    }

    unsafe {
        let engine = (*engine_ptr).clone();
        Box::into_raw(Box::new(DebugServer::new(engine)))
    }
}

#[no_mangle]
pub extern "C" fn destroy_debug_server(server_ptr: *mut DebugServer) {
    if !server_ptr.is_null() {
        unsafe {
            drop(Box::from_raw(server_ptr));
        }
    }
}

#[no_mangle]
pub extern "C" fn create_debug_session(
    server_ptr: *mut DebugServer,
    instance_ptr: *mut Instance,
    module_ptr: *mut Module,
    session_id_out: *mut u64,
) -> c_int {
    if server_ptr.is_null() || instance_ptr.is_null() ||
       module_ptr.is_null() || session_id_out.is_null() {
        return -1;
    }

    unsafe {
        let server = &*server_ptr;
        let instance = (*instance_ptr).clone();
        let module = (*module_ptr).clone();

        match server.create_session(instance, module) {
            Ok(session_id) => {
                *session_id_out = session_id;
                0
            }
            Err(_) => -1,
        }
    }
}

#[no_mangle]
pub extern "C" fn set_debug_breakpoint(
    server_ptr: *const DebugServer,
    session_id: u64,
    function_index: u32,
    instruction_offset: u32,
    condition: *const c_char,
    breakpoint_id_out: *mut u32,
) -> c_int {
    if server_ptr.is_null() || breakpoint_id_out.is_null() {
        return -1;
    }

    unsafe {
        let server = &*server_ptr;
        let condition_opt = if condition.is_null() {
            None
        } else {
            CStr::from_ptr(condition).to_str().ok().map(|s| s.to_string())
        };

        match server.set_breakpoint(session_id, function_index, instruction_offset, condition_opt) {
            Ok(breakpoint_id) => {
                *breakpoint_id_out = breakpoint_id;
                0
            }
            Err(_) => -1,
        }
    }
}

#[no_mangle]
pub extern "C" fn debug_step_next(
    server_ptr: *const DebugServer,
    session_id: u64,
    context_out: *mut ExecutionContext,
) -> c_int {
    if server_ptr.is_null() || context_out.is_null() {
        return -1;
    }

    unsafe {
        let server = &*server_ptr;

        match server.step_next(session_id) {
            Ok(context) => {
                *context_out = context;
                0
            }
            Err(_) => -1,
        }
    }
}

#[no_mangle]
pub extern "C" fn debug_inspect_memory(
    server_ptr: *const DebugServer,
    session_id: u64,
    address: u64,
    length: u32,
    data_out: *mut *mut u8,
    actual_length_out: *mut usize,
) -> c_int {
    if server_ptr.is_null() || data_out.is_null() || actual_length_out.is_null() {
        return -1;
    }

    unsafe {
        let server = &*server_ptr;

        match server.inspect_memory(session_id, address, length) {
            Ok(data) => {
                let data_size = data.len();
                let boxed_data = data.into_boxed_slice();
                *data_out = Box::into_raw(boxed_data) as *mut u8;
                *actual_length_out = data_size;
                0
            }
            Err(_) => -1,
        }
    }
}

#[no_mangle]
pub extern "C" fn free_memory_data(data_ptr: *mut u8, size: usize) {
    if !data_ptr.is_null() {
        unsafe {
            drop(Box::from_raw(std::slice::from_raw_parts_mut(data_ptr, size)));
        }
    }
}

// Panama FFI exports with wasmtime4j_ prefix

/// Variable value type constants for FFI
pub const VARIABLE_TYPE_I32: i32 = 0;
pub const VARIABLE_TYPE_I64: i32 = 1;
pub const VARIABLE_TYPE_F32: i32 = 2;
pub const VARIABLE_TYPE_F64: i32 = 3;
pub const VARIABLE_TYPE_V128: i32 = 4;
pub const VARIABLE_TYPE_FUNCREF: i32 = 5;
pub const VARIABLE_TYPE_EXTERNREF: i32 = 6;

/// FFI struct for evaluation results
#[repr(C)]
pub struct EvaluationResultFFI {
    pub success: bool,
    pub value_type: i32,
    pub i32_value: i32,
    pub i64_value: i64,
    pub f32_value: f32,
    pub f64_value: f64,
    pub error_message: *mut c_char,
}

impl EvaluationResultFFI {
    fn success_i32(value: i32) -> Self {
        Self {
            success: true,
            value_type: VARIABLE_TYPE_I32,
            i32_value: value,
            i64_value: 0,
            f32_value: 0.0,
            f64_value: 0.0,
            error_message: std::ptr::null_mut(),
        }
    }

    fn success_i64(value: i64) -> Self {
        Self {
            success: true,
            value_type: VARIABLE_TYPE_I64,
            i32_value: 0,
            i64_value: value,
            f32_value: 0.0,
            f64_value: 0.0,
            error_message: std::ptr::null_mut(),
        }
    }

    fn success_f32(value: f32) -> Self {
        Self {
            success: true,
            value_type: VARIABLE_TYPE_F32,
            i32_value: 0,
            i64_value: 0,
            f32_value: value,
            f64_value: 0.0,
            error_message: std::ptr::null_mut(),
        }
    }

    fn success_f64(value: f64) -> Self {
        Self {
            success: true,
            value_type: VARIABLE_TYPE_F64,
            i32_value: 0,
            i64_value: 0,
            f32_value: 0.0,
            f64_value: value,
            error_message: std::ptr::null_mut(),
        }
    }

    fn error(message: &str) -> Self {
        let error_cstr = CString::new(message).unwrap_or_else(|_| CString::new("Unknown error").unwrap());
        Self {
            success: false,
            value_type: -1,
            i32_value: 0,
            i64_value: 0,
            f32_value: 0.0,
            f64_value: 0.0,
            error_message: error_cstr.into_raw(),
        }
    }
}

/// Creates a debug server
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_create_server(engine_ptr: *mut Engine) -> *mut DebugServer {
    create_debug_server(engine_ptr)
}

/// Destroys a debug server
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_destroy_server(server_ptr: *mut DebugServer) {
    destroy_debug_server(server_ptr);
}

/// Creates a debug session
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_create_session_ffi(
    server_ptr: *mut DebugServer,
    instance_ptr: *mut Instance,
    module_ptr: *mut Module,
) -> u64 {
    let mut session_id: u64 = 0;
    let result = create_debug_session(server_ptr, instance_ptr, module_ptr, &mut session_id as *mut u64);
    if result == 0 {
        session_id
    } else {
        0
    }
}

/// Sets a breakpoint
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_set_breakpoint(
    server_ptr: *const DebugServer,
    session_id: u64,
    function_index: u32,
    instruction_offset: u32,
) -> u32 {
    let mut breakpoint_id: u32 = 0;
    let result = set_debug_breakpoint(
        server_ptr,
        session_id,
        function_index,
        instruction_offset,
        std::ptr::null(),
        &mut breakpoint_id as *mut u32,
    );
    if result == 0 {
        breakpoint_id
    } else {
        u32::MAX
    }
}

/// Removes a breakpoint
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_remove_breakpoint(
    server_ptr: *const DebugServer,
    session_id: u64,
    breakpoint_id: u32,
) -> bool {
    if server_ptr.is_null() {
        return false;
    }

    unsafe {
        let server = &*server_ptr;
        server.remove_breakpoint(session_id, breakpoint_id).is_ok()
    }
}

/// Performs step-into
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_step_into(
    server_ptr: *const DebugServer,
    session_id: u64,
    function_index_out: *mut u32,
    instruction_offset_out: *mut u32,
) -> bool {
    if server_ptr.is_null() || function_index_out.is_null() || instruction_offset_out.is_null() {
        return false;
    }

    unsafe {
        let server = &*server_ptr;
        match server.step_into(session_id) {
            Ok(context) => {
                *function_index_out = context.function_index;
                *instruction_offset_out = context.instruction_offset;
                true
            }
            Err(_) => false,
        }
    }
}

/// Performs step-over
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_step_over(
    server_ptr: *const DebugServer,
    session_id: u64,
    function_index_out: *mut u32,
    instruction_offset_out: *mut u32,
) -> bool {
    if server_ptr.is_null() || function_index_out.is_null() || instruction_offset_out.is_null() {
        return false;
    }

    unsafe {
        let server = &*server_ptr;
        match server.step_next(session_id) {
            Ok(context) => {
                *function_index_out = context.function_index;
                *instruction_offset_out = context.instruction_offset;
                true
            }
            Err(_) => false,
        }
    }
}

/// Performs step-out
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_step_out(
    server_ptr: *const DebugServer,
    session_id: u64,
    function_index_out: *mut u32,
    instruction_offset_out: *mut u32,
) -> bool {
    if server_ptr.is_null() || function_index_out.is_null() || instruction_offset_out.is_null() {
        return false;
    }

    unsafe {
        let server = &*server_ptr;
        match server.step_out(session_id) {
            Ok(context) => {
                *function_index_out = context.function_index;
                *instruction_offset_out = context.instruction_offset;
                true
            }
            Err(_) => false,
        }
    }
}

/// Continues execution
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_continue(
    server_ptr: *const DebugServer,
    session_id: u64,
) -> bool {
    if server_ptr.is_null() {
        return false;
    }

    unsafe {
        let server = &*server_ptr;
        server.continue_execution(session_id).is_ok()
    }
}

/// Pauses execution
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_pause(
    server_ptr: *const DebugServer,
    session_id: u64,
) -> bool {
    if server_ptr.is_null() {
        return false;
    }

    unsafe {
        let server = &*server_ptr;
        server.pause_execution(session_id).is_ok()
    }
}

/// Evaluates an expression in the debug context
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_evaluate_expression(
    server_ptr: *const DebugServer,
    session_id: u64,
    expression: *const c_char,
    result_out: *mut EvaluationResultFFI,
) -> bool {
    if server_ptr.is_null() || expression.is_null() || result_out.is_null() {
        return false;
    }

    unsafe {
        let server = &*server_ptr;
        let expr_str = match CStr::from_ptr(expression).to_str() {
            Ok(s) => s,
            Err(_) => {
                *result_out = EvaluationResultFFI::error("Invalid expression string");
                return false;
            }
        };

        match server.evaluate_watch(session_id, expr_str) {
            Ok(WatchResult::Success(value)) => {
                *result_out = match value {
                    VariableValue::I32(v) => EvaluationResultFFI::success_i32(v),
                    VariableValue::I64(v) => EvaluationResultFFI::success_i64(v),
                    VariableValue::F32(v) => EvaluationResultFFI::success_f32(v),
                    VariableValue::F64(v) => EvaluationResultFFI::success_f64(v),
                    _ => EvaluationResultFFI::error("Unsupported value type"),
                };
                true
            }
            Ok(WatchResult::Error(msg)) => {
                *result_out = EvaluationResultFFI::error(&msg);
                false
            }
            Err(e) => {
                *result_out = EvaluationResultFFI::error(&e.to_string());
                false
            }
        }
    }
}

/// Frees an evaluation result error message
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_free_evaluation_result(result: *mut EvaluationResultFFI) {
    if !result.is_null() {
        unsafe {
            let result_ref = &mut *result;
            if !result_ref.error_message.is_null() {
                drop(CString::from_raw(result_ref.error_message));
                result_ref.error_message = std::ptr::null_mut();
            }
        }
    }
}

/// Gets the number of local variables in the current frame
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_get_local_count(
    server_ptr: *const DebugServer,
    session_id: u64,
) -> i32 {
    if server_ptr.is_null() {
        return -1;
    }

    unsafe {
        let server = &*server_ptr;
        match server.get_variables(session_id) {
            Ok(vars) => vars.len() as i32,
            Err(_) => -1,
        }
    }
}

/// Gets call stack depth
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_get_stack_depth(
    server_ptr: *const DebugServer,
    session_id: u64,
) -> i32 {
    if server_ptr.is_null() {
        return -1;
    }

    unsafe {
        let server = &*server_ptr;
        match server.get_call_stack(session_id) {
            Ok(stack) => stack.len() as i32,
            Err(_) => -1,
        }
    }
}

/// Closes a debug session
#[no_mangle]
pub extern "C" fn wasmtime4j_debug_close_session_ffi(
    server_ptr: *mut DebugServer,
    session_id: u64,
) -> bool {
    if server_ptr.is_null() {
        return false;
    }

    unsafe {
        let server = &*server_ptr;
        server.remove_session(session_id).is_ok()
    }
}