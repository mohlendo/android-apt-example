package de.manuelohlendorf.androidaptexample.compiler;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import de.manuelohlendorf.androidaptexample.annotations.Component;

/**
 * Simple annotation processor that generates a class that holds a map of
 * all classes that implement that annotation
 */
@AutoService(Processor.class)
public class AndroidAptExampleProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        elementUtils = env.getElementUtils();
        filer = env.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Component.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // collect all the annotated classes
        Set<? extends javax.lang.model.element.Element> elements = roundEnv.getElementsAnnotatedWith(Component.class);
        Map<String, String> componentClasses = new HashMap<>();
        for (javax.lang.model.element.Element element : elements) {
            if (!SuperficialValidation.validateElement(element)) {
                continue;
            }
            // now get the annotation value an the class name
            String name = elementUtils.getPackageOf(element).getQualifiedName().toString() + "." + element.getSimpleName().toString();
            final String[] values = element.getAnnotation(Component.class).value();
            for (String value : values) {
                componentClasses.put(value, name);
            }
        }

        // ignore empty writes
        if (componentClasses.isEmpty()) {
            return false;
        }


        // Create the registry class
        TypeSpec.Builder result = TypeSpec.classBuilder("ComponentRegistry");
        result.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Map<String, Class>
        ClassName map = ClassName.get("java.util", "Map");
        ClassName hashMap = ClassName.get("java.util", "HashMap");
        final ParameterizedTypeName mapOfClasses = ParameterizedTypeName.get(map, TypeName.get(String.class), TypeName.get(Class.class));

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getRegisteredClasses")
                .returns(mapOfClasses)
                .addStatement("$T result = new $T<>()", mapOfClasses, hashMap)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);

        // add all the statements for the annotated classes
        for (String name : componentClasses.keySet()) {
            methodBuilder = methodBuilder.addStatement("result.put(\"$N\", $N.class)", name, componentClasses.get(name));
        }
        methodBuilder = methodBuilder.addStatement("return result");
        result.addMethod(methodBuilder.build());

        // write the new file
        try {
            JavaFile.builder("de.manuelohlendorf.androidaptexample", result.build())
                    .addFileComment("Generated code from annotation compiler. Do not modify!")
                    .build().writeTo(filer);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Unable to write register %s",
                    e.getMessage()), null);
        }
        return true;
    }
}
