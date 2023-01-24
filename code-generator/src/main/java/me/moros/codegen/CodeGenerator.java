package me.moros.codegen;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

record CodeGenerator(Logger logger, Path output, String version) {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  void generate(InputStream resourceFile, String packageName, String typeName) {
    generate(resourceFile, packageName, typeName, typeName + "Impl", typeName + "s");
  }

  void generate(InputStream resourceFile, String packageName, String typeName, String implName, String holder) {
    if (resourceFile == null) {
      logger.log(Level.ERROR, "Failed to find resource file for " + typeName);
      return;
    }
    ClassName typeClass = ClassName.get(packageName, typeName);
    ClassName loaderClass = ClassName.get(packageName, implName);

    JsonObject json = GSON.fromJson(new InputStreamReader(resourceFile), JsonObject.class);
    ClassName materialsCN = ClassName.get(packageName, holder);
    // BlockConstants class
    TypeSpec.Builder builder = TypeSpec.interfaceBuilder(materialsCN)
      // Add @SuppressWarnings("unused")
      .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unused").build())
      .addJavadoc("Code autogenerated, do not edit! " + version);

    // Use data
    json.keySet().forEach(namespace -> {
      final String resourceKey = namespace.replace("minecraft:", "");
      final String constantName = resourceKey
        .replace(".", "_")
        .replace("/", "_")
        .toUpperCase(Locale.ROOT);
      builder.addField(
        FieldSpec.builder(typeClass, constantName)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer(
            "$T.get($S)",
            loaderClass,
            resourceKey
          )
          .build()
      );
    });
    writeFiles(List.of(JavaFile.builder(packageName, builder.build()).indent("  ").skipJavaLangImports(true).build()));
  }

  private void writeFiles(List<JavaFile> fileList) {
    for (JavaFile javaFile : fileList) {
      try {
        javaFile.writeTo(output);
      } catch (IOException e) {
        logger.log(Level.ERROR, "An error occurred while writing source code to the file system.", e);
      }
    }
  }
}