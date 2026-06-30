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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.component.ComponentFunc;
import ai.tegmentum.wasmtime4j.component.ComponentFunction;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import ai.tegmentum.wasmtime4j.component.ComponentResult;
import ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor;
import ai.tegmentum.wasmtime4j.component.ComponentTypedFunc;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.component.ComponentValFactory;
import ai.tegmentum.wasmtime4j.component.ComponentVariant;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wit.WitBool;
import ai.tegmentum.wasmtime4j.wit.WitBorrow;
import ai.tegmentum.wasmtime4j.wit.WitChar;
import ai.tegmentum.wasmtime4j.wit.WitEnum;
import ai.tegmentum.wasmtime4j.wit.WitFlags;
import ai.tegmentum.wasmtime4j.wit.WitFloat32;
import ai.tegmentum.wasmtime4j.wit.WitFloat64;
import ai.tegmentum.wasmtime4j.wit.WitList;
import ai.tegmentum.wasmtime4j.wit.WitOption;
import ai.tegmentum.wasmtime4j.wit.WitOwn;
import ai.tegmentum.wasmtime4j.wit.WitRecord;
import ai.tegmentum.wasmtime4j.wit.WitResult;
import ai.tegmentum.wasmtime4j.wit.WitS16;
import ai.tegmentum.wasmtime4j.wit.WitS32;
import ai.tegmentum.wasmtime4j.wit.WitS64;
import ai.tegmentum.wasmtime4j.wit.WitS8;
import ai.tegmentum.wasmtime4j.wit.WitString;
import ai.tegmentum.wasmtime4j.wit.WitTuple;
import ai.tegmentum.wasmtime4j.wit.WitType;
import ai.tegmentum.wasmtime4j.wit.WitU16;
import ai.tegmentum.wasmtime4j.wit.WitU32;
import ai.tegmentum.wasmtime4j.wit.WitU64;
import ai.tegmentum.wasmtime4j.wit.WitU8;
import ai.tegmentum.wasmtime4j.wit.WitValue;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller;
import ai.tegmentum.wasmtime4j.wit.WitVariant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JNI implementation of ComponentFunc for WebAssembly Component Model functions.
 *
 * <p>This class provides typed access to component functions with support for creating
 * ComponentTypedFunc wrappers for zero-cost typed invocations.
 *
 * @since 1.0.0
 */
public final class JniComponentFunc
    implements ComponentFunc, ComponentFunction, ComponentTypedFunc.TypedComponentFunctionSupport {

  private static final Logger LOGGER = Logger.getLogger(JniComponentFunc.class.getName());

  private final String functionName;
  private final JniComponentInstanceImpl instance;
  private final JniComponentImpl component;
  private volatile boolean valid = true;

  /**
   * Creates a new JNI component function.
   *
   * @param functionName the name of the component function
   * @param instance the component instance this function belongs to
   * @param component the component that created the instance
   */
  public JniComponentFunc(
      final String functionName,
      final JniComponentInstanceImpl instance,
      final JniComponentImpl component) {
    Validation.requireNonEmpty(functionName, "functionName");
    Validation.requireNonNull(instance, "instance");
    Validation.requireNonNull(component, "component");
    this.functionName = functionName;
    this.instance = instance;
    this.component = component;
    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine("Created JniComponentFunc: " + functionName);
    }
  }

  @Override
  public String getName() {
    return functionName;
  }

  @Override
  public List<ComponentTypeDescriptor> getParameterTypes() {
    // Return empty list - parameter types would require native introspection
    // This is acceptable for basic functionality; can be enhanced later
    return Collections.emptyList();
  }

  @Override
  public List<ComponentTypeDescriptor> getResultTypes() {
    // Return empty list - result types would require native introspection
    // This is acceptable for basic functionality; can be enhanced later
    return Collections.emptyList();
  }

  @Override
  public List<ComponentVal> call(final ComponentVal... args) throws WasmException {
    Validation.requireNonNull(args, "args");
    return call(Arrays.asList(args));
  }

  @Override
  public List<ComponentVal> call(final List<ComponentVal> args) throws WasmException {
    Validation.requireNonNull(args, "args");
    if (!isValid()) {
      throw new WasmException("Component function is not valid");
    }

    try {
      // Convert ComponentVal to WitValue for marshalling
      final List<WitValue> witValues = new ArrayList<>(args.size());
      for (final ComponentVal arg : args) {
        witValues.add(componentValToWitValue(arg));
      }

      // Marshal all parameters
      final List<WitValueMarshaller.MarshalledValue> marshalled =
          WitValueMarshaller.marshalAll(witValues);

      // Prepare arrays for JNI call
      final int[] typeDiscriminators = new int[marshalled.size()];
      final byte[][] data = new byte[marshalled.size()][];
      for (int i = 0; i < marshalled.size(); i++) {
        typeDiscriminators[i] = marshalled.get(i).getTypeDiscriminator();
        data[i] = marshalled.get(i).getData();
      }

      // Call native function
      final Object[] result =
          JniComponent.nativeComponentInvokeFunction(
              component.getEngine().getNativeHandle(),
              instance.getNativeInstance().getNativeHandle(),
              functionName,
              typeDiscriminators,
              data);

      // Handle void return
      if (result == null || result.length == 0) {
        return Collections.emptyList();
      }

      // Unmarshal result
      final int resultType = (Integer) result[0];
      final byte[] resultData = (byte[]) result[1];
      final WitValue resultWitValue = WitValueMarshaller.unmarshal(resultType, resultData);

      // Convert back to ComponentVal
      return Collections.singletonList(witValueToComponentVal(resultWitValue));

    } catch (final ValidationException e) {
      throw new WasmException("WIT value marshalling failed: " + e.getMessage(), e);
    } catch (final Exception e) {
      throw new WasmException("Function invocation failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Object call(final Object... args) throws WasmException {
    // Delegate to typed call after converting
    if (args == null || args.length == 0) {
      final List<ComponentVal> results = call(Collections.<ComponentVal>emptyList());
      return results.isEmpty() ? null : results.get(0);
    }

    // Convert Object[] to ComponentVal[]
    final ComponentVal[] componentArgs = new ComponentVal[args.length];
    for (int i = 0; i < args.length; i++) {
      componentArgs[i] = objectToComponentVal(args[i]);
    }

    final List<ComponentVal> results = call(componentArgs);
    return results.isEmpty() ? null : results.get(0);
  }

  @Override
  public boolean isValid() {
    return valid && instance.isValid();
  }

  @Override
  public ComponentInstance getInstance() {
    return instance;
  }

  @Override
  public ComponentTypedFunc asTyped(final String signature) {
    Validation.requireNonEmpty(signature, "signature");
    return new JniComponentTypedFunc(this, signature);
  }

  /** Marks this function as invalid. */
  void invalidate() {
    valid = false;
  }

  /**
   * Converts a ComponentVal to WitValue for marshalling.
   *
   * @param val the ComponentVal to convert
   * @return the corresponding WitValue
   * @throws ValidationException if conversion fails
   */
  private WitValue componentValToWitValue(final ComponentVal val) throws ValidationException {
    try {
      switch (val.getType()) {
        case BOOL:
          return WitBool.of(val.asBool());
        case S8:
          return WitS8.of(val.asS8());
        case S16:
          return WitS16.of(val.asS16());
        case S32:
          return WitS32.of(val.asS32());
        case S64:
          return WitS64.of(val.asS64());
        case U8:
          // ComponentVal.asU8() returns short (0-255), WitU8.of() expects byte
          return WitU8.of((byte) val.asU8());
        case U16:
          // ComponentVal.asU16() returns int (0-65535), WitU16.of() expects short
          return WitU16.of((short) val.asU16());
        case U32:
          // ComponentVal.asU32() returns long (0-4294967295), WitU32.of() expects int
          return WitU32.of((int) val.asU32());
        case U64:
          return WitU64.of(val.asU64());
        case F32:
          return WitFloat32.of(val.asF32());
        case F64:
          return WitFloat64.of(val.asF64());
        case CHAR:
          return WitChar.of(val.asChar());
        case STRING:
          return WitString.of(val.asString());
        case OWN:
          {
            // Coerce to an owned handle: a value tagged OWN may still wrap a handle whose
            // own/borrow flag was set differently (e.g. a handle returned by a prior call).
            final ComponentResourceHandle h = val.asResource();
            return WitOwn.fromHandle(
                h.isOwned() ? h : ComponentResourceHandle.own(h.getResourceType(), h.getIndex()));
          }
        case BORROW:
          {
            // Coerce to a borrowed handle. Resources returned as `own` (e.g. a vm-handle from
            // create-vm) are commonly passed back as `borrow<T>` to later calls (define-class,
            // invoke-static); present them as a borrow of the same resource index.
            final ComponentResourceHandle h = val.asResource();
            return WitBorrow.fromHandle(
                h.isBorrowed()
                    ? h
                    : ComponentResourceHandle.borrow(h.getResourceType(), h.getIndex()));
          }
        case LIST:
          {
            final List<ComponentVal> src = val.asList();
            if (src.isEmpty()) {
              // Empty list: element type can't be inferred from values. The canonical ABI
              // lowering of a zero-length list doesn't use the element type, so a placeholder
              // is adequate.
              return WitList.empty(WitType.createString());
            }
            final List<WitValue> elements = new ArrayList<>();
            for (final ComponentVal e : src) {
              elements.add(componentValToWitValue(e));
            }
            return WitList.of(elements);
          }
        case TUPLE:
          {
            final List<WitValue> elements = new ArrayList<>();
            for (final ComponentVal e : val.asTuple()) {
              elements.add(componentValToWitValue(e));
            }
            return WitTuple.of(elements);
          }
        case RECORD:
          {
            final Map<String, WitValue> fields = new LinkedHashMap<>();
            for (final Map.Entry<String, ComponentVal> e : val.asRecord().entrySet()) {
              fields.put(e.getKey(), componentValToWitValue(e.getValue()));
            }
            return WitRecord.of(fields);
          }
        case OPTION:
          {
            final Optional<ComponentVal> some = val.asSome();
            if (some.isPresent()) {
              final WitValue inner = componentValToWitValue(some.get());
              return WitOption.some(WitType.option(witTypeOf(some.get())), inner);
            }
            // None: the inner type is not recoverable from the value; the canonical lowering
            // uses the function's declared parameter type, so a string placeholder is adequate.
            return WitOption.none(WitType.option(WitType.createString()));
          }
        case VARIANT:
          {
            final ComponentVariant variant = val.asVariant();
            final Optional<ComponentVal> payload = variant.getPayload();
            final Map<String, Optional<WitType>> cases = new LinkedHashMap<>();
            cases.put(variant.getCaseName(), payload.map(JniComponentFunc.this::witTypeOf));
            final WitType type = WitType.variant("variant", cases);
            return payload.isPresent()
                ? WitVariant.of(type, variant.getCaseName(), componentValToWitValue(payload.get()))
                : WitVariant.of(type, variant.getCaseName());
          }
        case ENUM:
          return WitEnum.of(WitType.enumType("enum", List.of(val.asEnum())), val.asEnum());
        case FLAGS:
          {
            final List<String> names = new ArrayList<>(val.asFlags());
            return WitFlags.of(WitType.flags("flags", names), val.asFlags());
          }
        case RESULT:
          {
            final ComponentResult result = val.asResult();
            final Optional<ComponentVal> ok = result.getOk();
            final Optional<ComponentVal> err = result.getErr();
            final WitType type =
                WitType.result(
                    ok.map(JniComponentFunc.this::witTypeOf),
                    err.map(JniComponentFunc.this::witTypeOf));
            if (result.isOk()) {
              return ok.isPresent()
                  ? WitResult.ok(type, componentValToWitValue(ok.get()))
                  : WitResult.ok(type);
            }
            return err.isPresent()
                ? WitResult.err(type, componentValToWitValue(err.get()))
                : WitResult.err(type);
          }
        default:
          throw new IllegalArgumentException("Unsupported ComponentVal type: " + val.getType());
      }
    } catch (final Exception e) {
      if (e instanceof ValidationException) {
        throw (ValidationException) e;
      }
      throw new ValidationException(
          "Failed to convert ComponentVal to WitValue: " + e.getMessage(), e);
    }
  }

  /**
   * Best-effort derivation of a {@link WitType} from a value's structure. Used where the composite
   * WitValue factories require a type (option/variant/result/enum/flags). Inner types of empty
   * collections / {@code none} are not recoverable from the value alone; the canonical ABI lowering
   * uses the function's declared parameter type, so a placeholder suffices there.
   */
  private WitType witTypeOf(final ComponentVal val) {
    switch (val.getType()) {
      case BOOL:
        return WitType.createBool();
      case S8:
        return WitType.createS8();
      case S16:
        return WitType.createS16();
      case S32:
        return WitType.createS32();
      case S64:
        return WitType.createS64();
      case U8:
        return WitType.createU8();
      case U16:
        return WitType.createU16();
      case U32:
        return WitType.createU32();
      case U64:
        return WitType.createU64();
      case F32:
        return WitType.createFloat32();
      case F64:
        return WitType.createFloat64();
      case CHAR:
        return WitType.createChar();
      case STRING:
        return WitType.createString();
      case LIST:
        {
          final List<ComponentVal> es = val.asList();
          return WitType.list(es.isEmpty() ? WitType.createString() : witTypeOf(es.get(0)));
        }
      case TUPLE:
        {
          final List<WitType> types = new ArrayList<>();
          for (final ComponentVal e : val.asTuple()) {
            types.add(witTypeOf(e));
          }
          return WitType.tuple(types);
        }
      case RECORD:
        {
          final Map<String, WitType> fields = new LinkedHashMap<>();
          for (final Map.Entry<String, ComponentVal> e : val.asRecord().entrySet()) {
            fields.put(e.getKey(), witTypeOf(e.getValue()));
          }
          return WitType.record("record", fields);
        }
      case OPTION:
        return WitType.option(val.asSome().map(this::witTypeOf).orElse(WitType.createString()));
      case ENUM:
        return WitType.enumType("enum", List.of(val.asEnum()));
      case FLAGS:
        return WitType.flags("flags", new ArrayList<>(val.asFlags()));
      case VARIANT:
        {
          final ComponentVariant v = val.asVariant();
          final Map<String, Optional<WitType>> cases = new LinkedHashMap<>();
          cases.put(v.getCaseName(), v.getPayload().map(this::witTypeOf));
          return WitType.variant("variant", cases);
        }
      case RESULT:
        {
          final ComponentResult r = val.asResult();
          return WitType.result(r.getOk().map(this::witTypeOf), r.getErr().map(this::witTypeOf));
        }
      default:
        return WitType.createString();
    }
  }

  /**
   * Converts a WitValue to ComponentVal after unmarshalling.
   *
   * @param val the WitValue to convert
   * @return the corresponding ComponentVal
   */
  private ComponentVal witValueToComponentVal(final WitValue val) {
    if (val instanceof WitBool) {
      return ComponentVal.bool(((WitBool) val).toJava());
    } else if (val instanceof WitS8) {
      return ComponentVal.s8(((WitS8) val).toJava());
    } else if (val instanceof WitS16) {
      return ComponentVal.s16(((WitS16) val).toJava());
    } else if (val instanceof WitS32) {
      return ComponentVal.s32(((WitS32) val).toJava());
    } else if (val instanceof WitS64) {
      return ComponentVal.s64(((WitS64) val).toJava());
    } else if (val instanceof WitU8) {
      // u8 stores as byte but ComponentVal.u8() expects short for 0-255 range
      return ComponentVal.u8((short) ((WitU8) val).toUnsignedInt());
    } else if (val instanceof WitU16) {
      // u16 stores as short but ComponentVal.u16() expects int for 0-65535 range
      return ComponentVal.u16(((WitU16) val).toUnsignedInt());
    } else if (val instanceof WitU32) {
      // u32 stores as int but ComponentVal.u32() expects long for 0-4294967295 range
      return ComponentVal.u32(((WitU32) val).toUnsignedLong());
    } else if (val instanceof WitU64) {
      // u64 toJava() returns Long
      return ComponentVal.u64(((WitU64) val).toJava());
    } else if (val instanceof WitFloat32) {
      return ComponentVal.f32(((WitFloat32) val).toJava());
    } else if (val instanceof WitFloat64) {
      return ComponentVal.f64(((WitFloat64) val).toJava());
    } else if (val instanceof WitChar) {
      // WitChar toJava() returns Character
      return ComponentVal.char_(((WitChar) val).toJava());
    } else if (val instanceof WitString) {
      return ComponentVal.string((String) ((WitString) val).toJava());
    } else if (val instanceof WitOwn) {
      return ComponentVal.own(((WitOwn) val).getHandle());
    } else if (val instanceof WitBorrow) {
      return ComponentVal.borrow(((WitBorrow) val).getHandle());
    } else if (val instanceof WitList) {
      final List<ComponentVal> elements = new ArrayList<>();
      for (final WitValue e : ((WitList) val).getElements()) {
        elements.add(witValueToComponentVal(e));
      }
      return ComponentValFactory.INSTANCE.createList(elements);
    } else if (val instanceof WitTuple) {
      final List<ComponentVal> elements = new ArrayList<>();
      for (final WitValue e : ((WitTuple) val).getElements()) {
        elements.add(witValueToComponentVal(e));
      }
      return ComponentValFactory.INSTANCE.createTuple(elements);
    } else if (val instanceof WitRecord) {
      final Map<String, ComponentVal> fields = new LinkedHashMap<>();
      for (final Map.Entry<String, WitValue> e : ((WitRecord) val).getFields().entrySet()) {
        fields.put(e.getKey(), witValueToComponentVal(e.getValue()));
      }
      return ComponentValFactory.INSTANCE.createRecord(fields);
    } else if (val instanceof WitOption) {
      final WitOption opt = (WitOption) val;
      return opt.isSome() && opt.getValue().isPresent()
          ? ComponentValFactory.INSTANCE.createSome(witValueToComponentVal(opt.getValue().get()))
          : ComponentValFactory.INSTANCE.createNone();
    } else if (val instanceof WitResult) {
      final WitResult res = (WitResult) val;
      final ComponentVal placeholder = ComponentValFactory.INSTANCE.createBool(false);
      if (res.isOk()) {
        return ComponentValFactory.INSTANCE.createOk(
            res.getOk().map(this::witValueToComponentVal).orElse(placeholder));
      }
      return ComponentValFactory.INSTANCE.createErr(
          res.getErr().map(this::witValueToComponentVal).orElse(placeholder));
    } else if (val instanceof WitVariant) {
      final WitVariant var = (WitVariant) val;
      return ComponentValFactory.INSTANCE.createVariant(
          var.getCaseName(), var.getPayload().map(this::witValueToComponentVal).orElse(null));
    } else if (val instanceof WitEnum) {
      return ComponentValFactory.INSTANCE.createEnum(((WitEnum) val).getDiscriminant());
    } else if (val instanceof WitFlags) {
      return ComponentValFactory.INSTANCE.createFlags(((WitFlags) val).toJava());
    } else {
      throw new IllegalArgumentException(
          "Unsupported WitValue type: " + val.getClass().getSimpleName());
    }
  }

  /**
   * Converts a Java Object to ComponentVal.
   *
   * @param obj the object to convert
   * @return the corresponding ComponentVal
   */
  private ComponentVal objectToComponentVal(final Object obj) {
    if (obj instanceof ComponentVal) {
      return (ComponentVal) obj;
    } else if (obj instanceof WitValue) {
      return witValueToComponentVal((WitValue) obj);
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
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + obj.getClass().getName() + " to ComponentVal");
    }
  }
}
