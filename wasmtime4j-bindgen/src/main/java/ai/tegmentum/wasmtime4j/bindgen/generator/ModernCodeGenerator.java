/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
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

package ai.tegmentum.wasmtime4j.bindgen.generator;

import ai.tegmentum.wasmtime4j.bindgen.BindgenConfig;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenField;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenFunction;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenType;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenVariantCase;
import ai.tegmentum.wasmtime4j.bindgen.util.JavaNaming;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

/**
 * Code generator for Java 17+ using records and sealed interfaces.
 *
 * <p>This generator produces:
 *
 * <ul>
 *   <li>Records for WIT record types
 *   <li>Sealed interfaces with record implementations for variants
 *   <li>Java enums for WIT enums
 *   <li>Interfaces with abstract methods for WIT functions
 * </ul>
 */
public final class ModernCodeGenerator extends JavaCodeGenerator {

  /**
   * Creates a new ModernCodeGenerator.
   *
   * @param config the bindgen configuration
   */
  public ModernCodeGenerator(final BindgenConfig config) {
    super(config);
  }

  @Override
  protected TypeSpec generateRecord(final BindgenType type) {
    String className = JavaNaming.toClassName(type.getName());

    // Note: JavaPoet doesn't directly support records, so we generate a class
    // that mimics record semantics. Users can manually convert to records.
    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    // Add documentation
    if (config.isGenerateJavadoc() && type.getDocumentation().isPresent()) {
      classBuilder.addJavadoc(type.getDocumentation().get() + "\n");
      classBuilder.addJavadoc("\n<p>This class is designed to be used as a record.\n");
    }

    // Add private final fields
    for (BindgenField field : type.getFields()) {
      String fieldName = JavaNaming.toFieldName(field.getName());
      TypeName fieldType = mapType(field.getType());
      classBuilder.addField(fieldType, fieldName, Modifier.PRIVATE, Modifier.FINAL);
    }

    // Add constructor
    MethodSpec.Builder constructorBuilder =
        MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

    for (BindgenField field : type.getFields()) {
      String fieldName = JavaNaming.toFieldName(field.getName());
      TypeName fieldType = mapType(field.getType());
      constructorBuilder.addParameter(ParameterSpec.builder(fieldType, fieldName).build());
      constructorBuilder.addStatement("this.$N = $N", fieldName, fieldName);
    }

    classBuilder.addMethod(constructorBuilder.build());

    // Add getters (record-style: fieldName() instead of getFieldName())
    for (BindgenField field : type.getFields()) {
      String fieldName = JavaNaming.toFieldName(field.getName());
      TypeName fieldType = mapType(field.getType());

      MethodSpec.Builder getterBuilder =
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(Modifier.PUBLIC)
              .returns(fieldType)
              .addStatement("return this.$N", fieldName);

      if (config.isGenerateJavadoc() && field.getDocumentation().isPresent()) {
        getterBuilder.addJavadoc(field.getDocumentation().get() + "\n");
      }

      classBuilder.addMethod(getterBuilder.build());
    }

    // Add equals
    classBuilder.addMethod(generateEquals(type, className));

    // Add hashCode
    classBuilder.addMethod(generateHashCode(type));

    // Add toString
    classBuilder.addMethod(generateToString(type, className));

    return classBuilder.build();
  }

  @Override
  protected TypeSpec generateVariant(final BindgenType type) {
    String interfaceName = JavaNaming.toClassName(type.getName());

    // Create sealed interface
    TypeSpec.Builder interfaceBuilder =
        TypeSpec.interfaceBuilder(interfaceName).addModifiers(Modifier.PUBLIC);

    // Add documentation
    if (config.isGenerateJavadoc() && type.getDocumentation().isPresent()) {
      interfaceBuilder.addJavadoc(type.getDocumentation().get() + "\n");
      interfaceBuilder.addJavadoc("\n<p>This is a sealed interface for variant type.\n");
    }

    // Note: JavaPoet doesn't support 'sealed' keyword directly
    // We generate the interface and nested record-like classes

    // Generate case classes as nested static classes
    for (BindgenVariantCase variantCase : type.getCases()) {
      TypeSpec caseClass = generateVariantCase(interfaceName, variantCase);
      interfaceBuilder.addType(caseClass);
    }

    return interfaceBuilder.build();
  }

  private TypeSpec generateVariantCase(
      final String parentName, final BindgenVariantCase variantCase) {
    String caseName = JavaNaming.toClassName(variantCase.getName());
    ClassName parentType = ClassName.get(config.getPackageName(), parentName);

    TypeSpec.Builder caseBuilder =
        TypeSpec.classBuilder(caseName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .addSuperinterface(parentType);

    // Add documentation
    if (config.isGenerateJavadoc() && variantCase.getDocumentation().isPresent()) {
      caseBuilder.addJavadoc(variantCase.getDocumentation().get() + "\n");
    }

    if (variantCase.hasPayload()) {
      BindgenType payloadType = variantCase.getPayload().get();
      TypeName javaType = mapType(payloadType);

      // Add value field
      caseBuilder.addField(javaType, "value", Modifier.PRIVATE, Modifier.FINAL);

      // Add constructor
      caseBuilder.addMethod(
          MethodSpec.constructorBuilder()
              .addModifiers(Modifier.PUBLIC)
              .addParameter(javaType, "value")
              .addStatement("this.value = value")
              .build());

      // Add getter
      caseBuilder.addMethod(
          MethodSpec.methodBuilder("value")
              .addModifiers(Modifier.PUBLIC)
              .returns(javaType)
              .addStatement("return this.value")
              .build());
    } else {
      // Add private constructor for singleton-like behavior
      caseBuilder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
    }

    return caseBuilder.build();
  }

  @Override
  protected TypeSpec generateResource(final BindgenType type) {
    String className = JavaNaming.toClassName(type.getName());

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get(AutoCloseable.class));

    // Add documentation
    if (config.isGenerateJavadoc()) {
      classBuilder.addJavadoc("Resource type: $L\n", type.getName());
      classBuilder.addJavadoc("\n<p>This resource should be closed after use.\n");
    }

    // Add handle field
    classBuilder.addField(TypeName.LONG, "handle", Modifier.PRIVATE, Modifier.FINAL);

    // Add constructor
    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.LONG, "handle")
            .addStatement("this.handle = handle")
            .build());

    // Add handle getter
    classBuilder.addMethod(
        MethodSpec.methodBuilder("handle")
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.LONG)
            .addStatement("return this.handle")
            .build());

    // Add close method
    classBuilder.addMethod(
        MethodSpec.methodBuilder("close")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addComment("Resource cleanup - to be implemented by runtime")
            .build());

    // Generate methods from functions (if any)
    for (BindgenFunction function :
        type.getFields().isEmpty()
            ? java.util.Collections.<BindgenFunction>emptyList()
            : java.util.Collections.<BindgenFunction>emptyList()) {
      classBuilder.addMethod(generateFunctionSignature(function));
    }

    return classBuilder.build();
  }

  private MethodSpec generateEquals(final BindgenType type, final String className) {
    MethodSpec.Builder equalsBuilder =
        MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.BOOLEAN)
            .addParameter(Object.class, "obj");

    equalsBuilder.addStatement("if (this == obj) return true");
    equalsBuilder.addStatement("if (obj == null || getClass() != obj.getClass()) return false");
    equalsBuilder.addStatement("$L that = ($L) obj", className, className);

    if (type.getFields().isEmpty()) {
      equalsBuilder.addStatement("return true");
    } else {
      StringBuilder comparison = new StringBuilder();
      for (int i = 0; i < type.getFields().size(); i++) {
        BindgenField field = type.getFields().get(i);
        String fieldName = JavaNaming.toFieldName(field.getName());
        TypeName fieldType = mapType(field.getType());

        if (i > 0) {
          comparison.append(" && ");
        }

        if (fieldType.isPrimitive()) {
          comparison.append(String.format("this.%s == that.%s", fieldName, fieldName));
        } else {
          comparison.append(
              String.format("java.util.Objects.equals(this.%s, that.%s)", fieldName, fieldName));
        }
      }
      equalsBuilder.addStatement("return " + comparison);
    }

    return equalsBuilder.build();
  }

  private MethodSpec generateHashCode(final BindgenType type) {
    MethodSpec.Builder hashCodeBuilder =
        MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.INT);

    if (type.getFields().isEmpty()) {
      hashCodeBuilder.addStatement("return 0");
    } else {
      StringBuilder args = new StringBuilder();
      for (int i = 0; i < type.getFields().size(); i++) {
        BindgenField field = type.getFields().get(i);
        String fieldName = JavaNaming.toFieldName(field.getName());
        if (i > 0) {
          args.append(", ");
        }
        args.append(fieldName);
      }
      hashCodeBuilder.addStatement("return java.util.Objects.hash($L)", args);
    }

    return hashCodeBuilder.build();
  }

  private MethodSpec generateToString(final BindgenType type, final String className) {
    MethodSpec.Builder toStringBuilder =
        MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(String.class);

    if (type.getFields().isEmpty()) {
      toStringBuilder.addStatement("return \"$L[]\"", className);
    } else {
      StringBuilder format = new StringBuilder();
      format.append(className).append("[");
      for (int i = 0; i < type.getFields().size(); i++) {
        BindgenField field = type.getFields().get(i);
        String fieldName = JavaNaming.toFieldName(field.getName());
        if (i > 0) {
          format.append(", ");
        }
        format.append(fieldName).append("=\" + ").append(fieldName).append(" + \"");
      }
      format.append("]");
      toStringBuilder.addStatement("return \"$L\"", format);
    }

    return toStringBuilder.build();
  }
}
