/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.component.ComponentFunc;
import ai.tegmentum.wasmtime4j.component.ComponentFunction;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor;
import ai.tegmentum.wasmtime4j.component.ComponentTypedFunc;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Panama implementation of a WebAssembly Component Model function.
 *
 * <p>This class wraps a function name and its parent component instance, providing a first-class
 * function object that can be invoked multiple times without name lookup overhead.
 *
 * @since 1.0.0
 */
final class PanamaComponentFunction
    implements ComponentFunc, ComponentFunction, ComponentTypedFunc.TypedComponentFunctionSupport {

  private final String functionName;
  private final PanamaComponentInstance instance;

  /**
   * Creates a new Panama component function.
   *
   * @param functionName the function name
   * @param instance the parent component instance
   */
  PanamaComponentFunction(final String functionName, final PanamaComponentInstance instance) {
    this.functionName = Objects.requireNonNull(functionName, "functionName cannot be null");
    this.instance = Objects.requireNonNull(instance, "instance cannot be null");
  }

  @Override
  public String getName() {
    return functionName;
  }

  @Override
  public List<ComponentTypeDescriptor> getParameterTypes() {
    // Return empty list - parameter types would require native introspection
    return Collections.emptyList();
  }

  @Override
  public List<ComponentTypeDescriptor> getResultTypes() {
    // Return empty list - result types would require native introspection
    return Collections.emptyList();
  }

  @Override
  public List<ComponentVal> call(final ComponentVal... args) throws WasmException {
    Objects.requireNonNull(args, "args cannot be null");
    return call(Arrays.asList(args));
  }

  @Override
  public List<ComponentVal> call(final List<ComponentVal> args) throws WasmException {
    Objects.requireNonNull(args, "args cannot be null");
    if (!isValid()) {
      throw new WasmException("Cannot call function: component instance is closed");
    }

    try {
      // Convert ComponentVal to Object for the existing invoke path
      final Object[] objArgs = new Object[args.size()];
      for (int i = 0; i < args.size(); i++) {
        objArgs[i] = componentValToJavaObject(args.get(i));
      }

      // Invoke through the existing PanamaComponentInstance path
      final Object result = instance.invoke(functionName, objArgs);

      // Handle void return
      if (result == null) {
        return Collections.emptyList();
      }

      // Convert result back to ComponentVal
      return Collections.singletonList(objectToComponentVal(result));

    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Function invocation failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Object call(final Object... args) throws WasmException {
    if (!isValid()) {
      throw new WasmException("Cannot call function: component instance is closed");
    }
    return instance.invoke(functionName, args);
  }

  @Override
  public boolean isValid() {
    return instance.isValid();
  }

  @Override
  public ComponentInstance getInstance() {
    return instance;
  }

  @Override
  public ComponentTypedFunc asTyped(final String signature) {
    Objects.requireNonNull(signature, "signature cannot be null");
    if (signature.isEmpty()) {
      throw new IllegalArgumentException("signature cannot be empty");
    }
    return new PanamaComponentTypedFunc(this, signature);
  }

  @Override
  public String toString() {
    return "PanamaComponentFunction{name='" + functionName + "', valid=" + isValid() + "}";
  }

  /**
   * Converts a ComponentVal to a Java object suitable for the invoke path.
   *
   * @param val the ComponentVal to convert
   * @return the corresponding Java object
   * @throws ValidationException if conversion fails
   */
  private Object componentValToJavaObject(final ComponentVal val) throws ValidationException {
    try {
      switch (val.getType()) {
        case BOOL:
          return val.asBool();
        case S8:
          return val.asS8();
        case S16:
          return val.asS16();
        case S32:
          return val.asS32();
        case S64:
          return val.asS64();
        case U8:
          return (int) val.asU8();
        case U16:
          return val.asU16();
        case U32:
          return val.asU32();
        case U64:
          return val.asU64();
        case F32:
          return val.asF32();
        case F64:
          return val.asF64();
        case CHAR:
          return val.asChar();
        case STRING:
          return val.asString();
        case OWN:
        case BORROW:
          return val.asResource();
        default:
          throw new IllegalArgumentException("Unsupported ComponentVal type: " + val.getType());
      }
    } catch (final Exception e) {
      if (e instanceof ValidationException) {
        throw (ValidationException) e;
      }
      throw new ValidationException(
          "Failed to convert ComponentVal to Java object: " + e.getMessage(), e);
    }
  }

  /**
   * Converts a Java object returned by invoke to a ComponentVal.
   *
   * @param obj the object to convert
   * @return the corresponding ComponentVal
   */
  private ComponentVal objectToComponentVal(final Object obj) {
    if (obj instanceof ComponentVal) {
      return (ComponentVal) obj;
    } else if (obj instanceof Boolean) {
      return ComponentVal.bool((Boolean) obj);
    } else if (obj instanceof Byte) {
      return ComponentVal.s8((Byte) obj);
    } else if (obj instanceof Short) {
      return ComponentVal.s16((Short) obj);
    } else if (obj instanceof Integer) {
      return ComponentVal.s32((Integer) obj);
    } else if (obj instanceof Long) {
      return ComponentVal.s64((Long) obj);
    } else if (obj instanceof Float) {
      return ComponentVal.f32((Float) obj);
    } else if (obj instanceof Double) {
      return ComponentVal.f64((Double) obj);
    } else if (obj instanceof Character) {
      return ComponentVal.char_((Character) obj);
    } else if (obj instanceof String) {
      return ComponentVal.string((String) obj);
    } else if (obj instanceof ComponentResourceHandle) {
      final ComponentResourceHandle handle = (ComponentResourceHandle) obj;
      return handle.isOwned() ? ComponentVal.own(handle) : ComponentVal.borrow(handle);
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + obj.getClass().getName() + " to ComponentVal");
    }
  }
}
