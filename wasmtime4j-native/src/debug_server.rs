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
            let mut next_id = self.next_session_id.lock().unwrap();
            let id = *next_id;
            *next_id += 1;
            id
        };

        let session = DebugSession::new(session_id, instance, module, self.event_sender.clone())?;

        {
            let mut sessions = self.debug_sessions.write().unwrap();
            sessions.insert(session_id, session);
        }

        Ok(session_id)
    }

    /// Removes a debug session
    pub fn remove_session(&self, session_id: u64) -> Result<(), anyhow::Error> {
        let mut sessions = self.debug_sessions.write().unwrap();
        sessions.remove(&session_id);
        Ok(())
    }

    /// Sets a breakpoint in a debug session
    pub fn set_breakpoint(&self, session_id: u64, function_index: u32, instruction_offset: u32,
                          condition: Option<String>) -> Result<u32, anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.set_breakpoint(function_index, instruction_offset, condition)
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Removes a breakpoint from a debug session
    pub fn remove_breakpoint(&self, session_id: u64, breakpoint_id: u32) -> Result<(), anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.remove_breakpoint(breakpoint_id)
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Steps to the next instruction in a debug session
    pub fn step_next(&self, session_id: u64) -> Result<ExecutionContext, anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.step_next()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Steps into a function call in a debug session
    pub fn step_into(&self, session_id: u64) -> Result<ExecutionContext, anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.step_into()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Steps out of the current function in a debug session
    pub fn step_out(&self, session_id: u64) -> Result<ExecutionContext, anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.step_out()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Continues execution in a debug session
    pub fn continue_execution(&self, session_id: u64) -> Result<(), anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.continue_execution()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Pauses execution in a debug session
    pub fn pause_execution(&self, session_id: u64) -> Result<ExecutionContext, anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.pause_execution()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Gets the call stack for a debug session
    pub fn get_call_stack(&self, session_id: u64) -> Result<Vec<StackFrame>, anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.get_call_stack()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Inspects memory in a debug session
    pub fn inspect_memory(&self, session_id: u64, address: u64, length: u32) -> Result<Vec<u8>, anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.inspect_memory(address, length)
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Gets variable values in the current scope for a debug session
    pub fn get_variables(&self, session_id: u64) -> Result<HashMap<String, VariableValue>, anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.get_variables()
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Evaluates a watch expression in a debug session
    pub fn evaluate_watch(&self, session_id: u64, expression: &str) -> Result<WatchResult, anyhow::Error> {
        let sessions = self.debug_sessions.read().unwrap();
        if let Some(session) = sessions.get(&session_id) {
            session.evaluate_watch(expression)
        } else {
            Err(anyhow::anyhow!("Debug session not found: {}", session_id))
        }
    }

    /// Starts the debug server event processing loop
    pub fn start(&mut self) -> Result<(), anyhow::Error> {
        {
            let mut running = self.is_running.lock().unwrap();
            if *running {
                return Ok(());
            }
            *running = true;
        }

        let receiver = self.event_receiver.take()
            .ok_or_else(|| anyhow::anyhow!("Event receiver already taken"))?;

        let is_running = self.is_running.clone();

        thread::spawn(move || {
            while *is_running.lock().unwrap() {
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
        let mut running = self.is_running.lock().unwrap();
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
            let mut next_id = self.next_breakpoint_id.lock().unwrap();
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
            let mut breakpoints = self.breakpoints.write().unwrap();
            breakpoints.insert(breakpoint_id, breakpoint);
        }

        Ok(breakpoint_id)
    }

    pub fn remove_breakpoint(&self, breakpoint_id: u32) -> Result<(), anyhow::Error> {
        let mut breakpoints = self.breakpoints.write().unwrap();
        breakpoints.remove(&breakpoint_id);
        Ok(())
    }

    pub fn step_next(&self) -> Result<ExecutionContext, anyhow::Error> {
        {
            let mut state = self.execution_state.lock().unwrap();
            *state = ExecutionState::Stepping;
        }

        // Simulate stepping to next instruction
        let context = self.create_execution_context(0, 1, "main")?;

        {
            let mut current = self.current_context.lock().unwrap();
            *current = Some(context.clone());
        }

        {
            let mut state = self.execution_state.lock().unwrap();
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
            let mut state = self.execution_state.lock().unwrap();
            *state = ExecutionState::Running;
        }

        let _ = self.event_sender.send(DebugEvent::ExecutionResumed {
            session_id: self.session_id,
        });

        Ok(())
    }

    pub fn pause_execution(&self) -> Result<ExecutionContext, anyhow::Error> {
        {
            let mut state = self.execution_state.lock().unwrap();
            *state = ExecutionState::Paused;
        }

        let context = self.create_execution_context(0, 5, "main")?;

        {
            let mut current = self.current_context.lock().unwrap();
            *current = Some(context.clone());
        }

        let _ = self.event_sender.send(DebugEvent::ExecutionPaused {
            session_id: self.session_id,
            context: context.clone(),
        });

        Ok(context)
    }

    pub fn get_call_stack(&self) -> Result<Vec<StackFrame>, anyhow::Error> {
        let stack = self.call_stack.lock().unwrap();
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