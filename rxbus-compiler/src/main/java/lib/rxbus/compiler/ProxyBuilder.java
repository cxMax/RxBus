package lib.rxbus.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import lib.rxbus.ThreadType;

import static lib.rxbus.ThreadType.Computation;
import static lib.rxbus.ThreadType.IO;
import static lib.rxbus.ThreadType.Immediate;
import static lib.rxbus.ThreadType.MainThread;
import static lib.rxbus.ThreadType.NewThread;
import static lib.rxbus.ThreadType.Trampoline;

/**
 * use javaPoet to build processor file
 * Created by cxmax on 2017/2/23.
 */
public class ProxyBuilder {
    private static final String CLASS_UNIFORM_MARK = "$$Proxy";

    private static final ClassName BUSPROXY = ClassName.get("lib.rxbus", "BusProxy");
    private static final ClassName FILTER_FUNC = ClassName.get("rx.functions", "Func1");
    private static final ClassName PROXY_ACTION = ClassName.get("", "ProxyAction");
    private static final ClassName SCHEDULER_MAIN = ClassName.get("rx.android.schedulers", "AndroidSchedulers", "mainThread");
    private static final ClassName SCHEDULER_IO = ClassName.get("rx.schedulers", "Schedulers", "io");
    private static final ClassName SCHEDULER_COMPUTATION = ClassName.get("rx.schedulers", "Schedulers", "computation");
    private static final ClassName SCHEDULER_NEWTHREAD = ClassName.get("rx.schedulers", "Schedulers", "newThread");
    private static final ClassName SCHEDULER_IMMEDIATE = ClassName.get("rx.schedulers", "Schedulers", "immediate");
    private static final ClassName SCHEDULER_TRAMPOLINE = ClassName.get("rx.schedulers", "Schedulers", "trampoline");

    private String mPackagePath;
    private ClassName mTargetClassName;
    private Set<MethodBinder> mMethods;

    public ProxyBuilder(ClassName targetClassName) {
        this.mTargetClassName = targetClassName;
        this.mPackagePath = targetClassName.packageName();
        mMethods = new LinkedHashSet<>();
    }

    public void addMethod(MethodBinder methodBinder) {
        mMethods.add(methodBinder);
    }

    public void build(Filer filer){
        JavaFile javaFile = JavaFile.builder(mPackagePath , createTargetClass()).build();
        try {
            javaFile.writeTo(filer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void build(File file) {
        JavaFile javaFile = JavaFile.builder(mPackagePath, createTargetClass()).build();
        try {
            javaFile.writeTo(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TypeSpec createTargetClass() {
        return TypeSpec.classBuilder(mTargetClassName.simpleName() + CLASS_UNIFORM_MARK)
                .addModifiers(Modifier.PUBLIC,Modifier.FINAL)
                .superclass(ParameterizedTypeName.get(BUSPROXY, TypeVariableName.get(mTargetClassName.simpleName())))
                .addMethods(createMethods())
                .build();
    }

    private ArrayList<MethodSpec> createMethods() {
        ArrayList<MethodSpec> methods = new ArrayList<>();
        methods.add(createConstructor());
        return methods;
    }

    private MethodSpec createConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        for (MethodBinder binder : mMethods) {
            builder.addCode(createMethodCode(binder));
        }
        return builder.build();
    }

    private CodeBlock createMethodCode(MethodBinder binder) {
        CodeBlock.Builder builder = CodeBlock.builder();
        Set<String> tags = binder.getTags();
        for (String tag : tags) {
            ClassName thread = getRxThread(binder.getThreadType());

            CodeBlock.Builder b = CodeBlock.builder();
            b.addStatement("createMethod($S\n,$T()\n,$L)", tag, thread, createProxyAction(binder));
            builder.add(b.build());
        }
        return builder.build();
    }

    private TypeSpec createFunc1(List<TypeMirror> paramTypes) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("call")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Object.class, "o")
                .returns(Boolean.class);

        methodBuilder.addStatement("return true");

        TypeSpec func1 = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(ParameterizedTypeName.get(FILTER_FUNC, TypeName.get(Object.class), TypeName.get(Boolean.class)))
                .addMethod(methodBuilder.build())
                .build();
        return func1;
    }

    private TypeSpec createProxyAction(MethodBinder binder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toDo")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(mTargetClassName, "target");
        TypeMirror paramType = binder.getParamTypes();
        TypeName typeName;
        if (paramType == null) {
            typeName = ClassName.get("java.lang", "Object");
            methodBuilder.addStatement("target." + binder.getMethodName() + "()");
        } else {
            if (paramType.getKind().isPrimitive()) {
                typeName = TypeName.get(paramType);
                if (!typeName.isBoxedPrimitive())
                    typeName = typeName.box();
            } else
                typeName = ClassName.get(paramType);
            methodBuilder.addStatement("target." + binder.getMethodName() + "(o)");
        }
        methodBuilder.addParameter(typeName, "o");

        TypeSpec proxyAction = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(ParameterizedTypeName.get(PROXY_ACTION, mTargetClassName, typeName))
                .addMethod(methodBuilder.build())
                .build();
        return proxyAction;
    }

    private ClassName getRxThread(ThreadType threadType) {
        ClassName className = SCHEDULER_IMMEDIATE;
        switch (threadType) {
            case MainThread:
                className = SCHEDULER_MAIN;
                break;
            case IO:
                className = SCHEDULER_IO;
                break;
            case Computation:
                className = SCHEDULER_COMPUTATION;
                break;
            case Immediate:
                className = SCHEDULER_IMMEDIATE;
                break;
            case NewThread:
                className = SCHEDULER_NEWTHREAD;
                break;
            case Trampoline:
                className = SCHEDULER_TRAMPOLINE;
                break;
        }
        return className;
    }

    @Override
    public String toString() {
        return "ProxyBuilder{" +
                "mPackagePath='" + mPackagePath + '\'' +
                ", mTargetClassName=" + mTargetClassName +
                ", mMethods=" + mMethods +
                '}';
    }

    String getClassName() {
        return mTargetClassName.toString();
    }
}
