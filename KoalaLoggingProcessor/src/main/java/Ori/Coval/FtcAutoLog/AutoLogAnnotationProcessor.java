package Ori.Coval.FtcAutoLog;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotation processor that generates an AutoLogged subclass which
 * overrides fields and methods to log via WpiLog.
 */
@SupportedAnnotationTypes({"Ori.Coval.Logging.AutoLog", "Ori.Coval.Logging.AutoLogOutput", "Ori.Coval.Logging.AutoLogPose2d"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoLogAnnotationProcessor extends AbstractProcessor {
    private boolean staticRegistryWritten = false;
    // Adjust this to your WpiLog package
    private static final ClassName KOALA_LOG = ClassName.get("Ori.Coval.Logging.Logger", "KoalaLog");
    private static final ClassName LOGGED = ClassName.get("Ori.Coval.Logging", "Logged");
    private static final ClassName AUTO_LOG_MANAGER = ClassName.get("Ori.Coval.Logging", "AutoLogManager");
    private static final ClassName SUPPLIER_LOG = ClassName.get("Ori.Coval.Logging", "SupplierLog");

    List<Element> autoLogOutputElements = new ArrayList<>();
    List<Element> autoLogPose2DElements = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element e : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (isAutoLog(e)) {
                    generateAutoLog((TypeElement) e, getAnnotationValue(e, "Ori.Coval.Logging.AutoLog", "postToFtcDashboard"));
                }

                if (isPose2d(e)) {
                    autoLogPose2DElements.add(e);
                } else if (isAutoLogOutput(e)) {
                    autoLogOutputElements.add(e);
                }
            }
        }
        if (!staticRegistryWritten && roundEnv.processingOver()) {
            generateStaticRegistry();
            staticRegistryWritten = true;
        }
        return true;
    }

    private boolean isAutoLog(Element e) {
        return e.getKind() == ElementKind.CLASS && e.getAnnotationMirrors().stream().anyMatch(m -> m.getAnnotationType().toString()
                .equals("Ori.Coval.Logging.AutoLog"));
    }

    private boolean isPose2d(Element e) {
        if ((e.getKind() == ElementKind.METHOD || e.getKind() == ElementKind.FIELD)
                && e.getAnnotationMirrors().stream()
                .anyMatch(m -> m.getAnnotationType().toString().equals("Ori.Coval.Logging.AutoLogPose2d"))) {

            // determine the relevant TypeMirror
            TypeMirror tm = (e.getKind() == ElementKind.FIELD)
                    ? e.asType()
                    : ((ExecutableElement) e).getReturnType();

            if (tm.getKind() == TypeKind.ARRAY) {
                ArrayType at = (ArrayType) tm;
                TypeMirror comp = at.getComponentType();
                TypeKind kind = comp.getKind();

                // primitive types
                if (kind == TypeKind.DOUBLE || kind == TypeKind.FLOAT || kind == TypeKind.INT) {
                    return true;
                }

                // boxed types
                if (kind == TypeKind.DECLARED) {
                    DeclaredType dt = (DeclaredType) comp;
                    Element elem = dt.asElement();
                    if (elem instanceof TypeElement) {
                        String qName = ((TypeElement) elem).getQualifiedName().toString();
                        return qName.equals("java.lang.Double")
                                || qName.equals("java.lang.Float")
                                || qName.equals("java.lang.Integer");
                    }
                }
            }
        }

        return false;
    }



    private boolean isAutoLogOutput(Element e) {
        return (e.getKind() == ElementKind.METHOD || e.getKind() == ElementKind.FIELD) && e.getAnnotationMirrors().stream()
                .anyMatch(m -> m.getAnnotationType().toString().equals("Ori.Coval.Logging.AutoLogOutput"));
    }

    private boolean getAnnotationValue(Element element, String annotationName, String key) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(annotationName)) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                        mirror.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals(key)) {
                        return Boolean.parseBoolean(entry.getValue().getValue().toString());
                    }
                }
            }
        }
        return true;
    }

    private void generateStaticRegistry() {
        String registryPkg = "Ori.Coval.AutoLog";
        String registryName = "AutoLogStaticRegistry";

        // holder for each entry’s key, owner type, member name, whether it's a method, and post flag
        class Entry {
            final Element elem;
            final String key;
            final ClassName owner;
            final String member;
            final boolean isMethod;
            final boolean post;

            Entry(Element elem, String key, ClassName owner, String member,
                  boolean isMethod, boolean post) {
                this.elem     = elem;
                this.key      = key;
                this.owner    = owner;
                this.member   = member;
                this.isMethod = isMethod;
                this.post     = post;
            }
        }

        List<Entry> autoLogOutputEntries = new ArrayList<>();
        List<Entry> autoLogPose2DEntries = new ArrayList<>();

        // 'elements' contains all @AutoLogOutput‐annotated static fields/methods
        for (Element elem : autoLogOutputElements) {

            boolean isField = elem.getKind() == ElementKind.FIELD;
            boolean isMethod = elem.getKind() == ElementKind.METHOD
                    && ((ExecutableElement) elem).getParameters().isEmpty();
            if (!(isField || isMethod)) continue;
            if (!elem.getModifiers().contains(Modifier.STATIC)) continue;

            // skip if the enclosing class is itself @AutoLog
            TypeElement enclosing = (TypeElement) elem.getEnclosingElement();


            // build owner ClassName
            String pkgName = processingEnv.getElementUtils()
                    .getPackageOf(enclosing).getQualifiedName().toString();
            ClassName owner = ClassName.get(pkgName, enclosing.getSimpleName().toString());
            String memberName = elem.getSimpleName().toString();
            String key = enclosing.getSimpleName() + "/" + memberName;

            boolean postToFtc = getAnnotationValue(
                    elem,
                    "Ori.Coval.Logging.AutoLogOutput",
                    "postToFtcDashboard"
            );

            autoLogOutputEntries.add(new Entry(elem, key, owner, memberName, isMethod, postToFtc));
        }

        for (Element elem : autoLogPose2DElements) {

            boolean isField = elem.getKind() == ElementKind.FIELD;
            boolean isMethod = elem.getKind() == ElementKind.METHOD
                    && ((ExecutableElement) elem).getParameters().isEmpty();
            if (!(isField || isMethod)) continue;
            if (!elem.getModifiers().contains(Modifier.STATIC)) continue;

            // skip if the enclosing class is itself @AutoLog
            TypeElement enclosing = (TypeElement) elem.getEnclosingElement();

            // build owner ClassName
            String pkgName = processingEnv.getElementUtils()
                    .getPackageOf(enclosing).getQualifiedName().toString();
            ClassName owner = ClassName.get(pkgName, enclosing.getSimpleName().toString());
            String memberName = elem.getSimpleName().toString();
            String key = enclosing.getSimpleName() + "/" + memberName;

            boolean postToFtc = getAnnotationValue(
                    elem,
                    "Ori.Coval.Logging.AutoLogPose2d",
                    "postToFtcDashboard"
            );

            autoLogPose2DEntries.add(new Entry(elem, key, owner, memberName, isMethod, postToFtc));
        }

        // build the toLog() method
        MethodSpec.Builder toLog = MethodSpec.methodBuilder("toLog")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class);

        for (Entry e : autoLogOutputEntries) {

            // detect suppliers by return type of static method or field type
            if (!e.isMethod) {
                if (e.elem.getKind() != ElementKind.FIELD) {
                    // shouldn’t happen, but skip to be safe
                    continue;
                }
                // For static fields, check if it's a Supplier
                VariableElement field = (VariableElement)e.elem;
                String fieldType = field.asType().toString();
                if (fieldType.endsWith("Supplier")) {
                    // call proper getAsX()
                    String invoke = "";
                    if (fieldType.endsWith("DoubleSupplier")) invoke = ".getAsDouble()";
                    if (fieldType.endsWith("IntSupplier")) invoke = ".getAsInt()";
                    if (fieldType.endsWith("LongSupplier")) invoke = ".getAsLong()";
                    if (fieldType.endsWith("BooleanSupplier")) invoke = ".getAsBoolean()";
                    toLog.addStatement(
                            "$T.log($S, $T.$L$L, $L)",
                            KOALA_LOG, e.key, e.owner, e.member, invoke, e.post
                    );
                    continue;
                }
                // non‐supplier static field
                toLog.addStatement(
                        "$T.log($S, $T.$L, $L)",
                        KOALA_LOG, e.key, e.owner, e.member, e.post
                );
            } else {
                if (e.elem.getKind() != ElementKind.METHOD) {
                    continue;
                }
                // otherwise regular static no‐arg method
                toLog.addStatement(
                        "$T.log($S, $T.$L(), $L)",
                        KOALA_LOG, e.key, e.owner, e.member, e.post
                );
            }
        }

        for (Entry e : autoLogPose2DEntries) {

            // for fields:
            boolean isMethod = e.isMethod;
            String member = e.member;

            if (!isMethod) {
                // field is an array of ([x,y,theta])
                toLog.addStatement(
                        "$T.logPose2d($S, $T.$L[0], $T.$L[1], $T.$L[2], $L)",
                        KOALA_LOG,          // $T -> your logger
                        e.key,              // $S -> the key literal
                        e.owner, member,    // $T.$L[0]
                        e.owner, member,    // $T.$L[1]
                        e.owner, member,    // $T.$L[2]
                        e.post              // $L -> post flag
                );
            } else {
                // no-arg static method that returns a double[]
                toLog.addStatement(
                        "$T.logPose2d($S, $T.$L()[0], $T.$L()[1], $T.$L()[2], $L)",
                        KOALA_LOG,
                        e.key,
                        e.owner, member,     // $T.$L() returns the double[]
                        e.owner, member,
                        e.owner, member,
                        e.post
                );
            }
        }


        // build and write the registry class
        TypeSpec registry = TypeSpec.classBuilder(registryName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get("Ori.Coval.Logging", "Logged"))
                .addStaticBlock(CodeBlock.of(
                        "$T.register(new $L());",
                        AUTO_LOG_MANAGER, registryName
                ))
                .addMethod(toLog.build())
                .build();

        try {
            JavaFile.builder(registryPkg, registry)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (FilerException ignored) {
            // already written
        } catch (IOException ex) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Could not write AutoLogStaticRegistry: " + ex.getMessage()
            );
        }
    }


    private void generateAutoLog(TypeElement classElem, boolean postToFtcDashBoard) {
        String pkg = getPackageName(classElem);
        String orig = classElem.getSimpleName().toString();
        String autoName = orig + "AutoLogged";
        PackageElement currentPkg = processingEnv.getElementUtils().getPackageOf(classElem);

        // Builder for the new class
        TypeSpec.Builder clsBuilder = TypeSpec.classBuilder(autoName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(LOGGED)
                .superclass(TypeName.get(classElem.asType()));

        // Build toLog method for fields
        MethodSpec.Builder toLog = MethodSpec.methodBuilder("toLog")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Auto-generated telemetry logging\n");

        List<Element> allElements = new ArrayList<>();

        if(getAnnotationValue(classElem, "Ori.Coval.Logging.AutoLog", "logSuperClasses")) {


            List<TypeElement> hierarchy = new ArrayList<>();
            TypeElement current = classElem;
            while (current != null
                    && !current.getQualifiedName().toString().equals("java.lang.Object")) {
                hierarchy.add(current);
                TypeMirror sup = current.getSuperclass();
                if (sup.getKind() != TypeKind.DECLARED) break;
                current = (TypeElement) processingEnv.getTypeUtils().asElement(sup);
            }


            for (TypeElement te : hierarchy) {
                allElements.addAll(te.getEnclosedElements());
            }
        }
        else {
            allElements.addAll(classElem.getEnclosedElements());
        }

        // collect supplier fields so we can make one constructor
        List<String> supplierFields = new ArrayList<>();
        List<String> supplierKeys = new ArrayList<>();

        // Fields
        for (Element fe : allElements) {
            if(fe.getAnnotationMirrors().stream().anyMatch(m -> m.getAnnotationType().toString().equals("Ori.Coval.Logging.DoNotLog")))
                continue;

            if(fe.getModifiers().contains(Modifier.FINAL)) continue;

            if (!isPose2d(fe)) {
                if (fe.getKind() != ElementKind.FIELD) continue;
                VariableElement field = (VariableElement) fe;
                Set<Modifier> mods = field.getModifiers();
                if (mods.contains(Modifier.PRIVATE)) continue;

                PackageElement fieldPkg = processingEnv.getElementUtils().getPackageOf(field);
                if (!mods.contains(Modifier.PUBLIC) && !fieldPkg.equals(currentPkg)) continue;

                String fname = field.getSimpleName().toString();
                TypeMirror t = field.asType();
                TypeKind k = t.getKind();

                boolean isSupplier = (k == TypeKind.DECLARED && t.toString().equals("java.util.function.LongSupplier"))
                        || (k == TypeKind.DECLARED && t.toString().equals("java.util.function.DoubleSupplier"))
                        || (k == TypeKind.DECLARED && t.toString().equals("java.util.function.IntSupplier"))
                        || (k == TypeKind.DECLARED && t.toString().equals("java.util.function.BooleanSupplier"));

                if (!(isLoggableType(t) || isSupplier))
                    continue;

                String key = orig + "/" + fname;
                if (isSupplier) {
                    // pick the right method name for the supplier
                    String invokeSuffix;
                    switch (t.toString()) {
                        case "java.util.function.DoubleSupplier":
                            invokeSuffix = ".getAsDouble()";
                            break;
                        case "java.util.function.IntSupplier":
                            invokeSuffix = ".getAsInt()";
                            break;
                        case "java.util.function.LongSupplier":
                            invokeSuffix = ".getAsLong()";
                            break;
                        case "java.util.function.BooleanSupplier":
                            invokeSuffix = ".getAsBoolean()";
                            break;
                        default:
                            invokeSuffix = ""; // shouldn't happen
                    }

                    boolean isAutoLogOutput = false;
                    for (AnnotationMirror mirror : fe.getAnnotationMirrors()) {
                        if (mirror.getAnnotationType().toString().equals("Ori.Coval.Logging.AutoLogOutput")) {
                            isAutoLogOutput = true;

                            if(!fe.getModifiers().contains(Modifier.STATIC)) {
                                boolean postToFtc = getAnnotationValue(fe,
                                        "Ori.Coval.Logging.AutoLogOutput",
                                        "postToFtcDashboard"
                                );
                                // NOTE: we use two $L slots for fname and invokeSuffix
                                toLog.addStatement(
                                        "$T.log($S, this.$L$L, $L)",
                                        KOALA_LOG,
                                        key,
                                        fname,
                                        invokeSuffix,
                                        postToFtc
                                );
                            }
                        }
                    }

                    if(!isAutoLogOutput) {
                        supplierFields.add(fname);
                        supplierKeys.add(key);
                    }

                } else {
                    toLog.addStatement("$T.log($S, this.$L, $L)", KOALA_LOG, key, fname, postToFtcDashBoard);
                }
            } else {

                String keyBase = orig + "/" + fe.getSimpleName();
                boolean post = getAnnotationValue(
                        fe,
                        "Ori.Coval.Logging.AutoLogPose2d",
                        "postToFtcDashboard"
                );

                if(fe.getModifiers().contains(Modifier.STATIC)) continue;

                if (fe.getKind() == ElementKind.FIELD) {
                    // static or instance @AutoLogPose2d field
                    VariableElement field = (VariableElement) fe;
                    String name = field.getSimpleName().toString();
                    toLog.addStatement(
                            "$T.logPose2d($S, $L[0], $L[1], $L[2], $L)",
                            KOALA_LOG,
                            keyBase,
                            name, name, name,
                            post
                    );

                }
                else if (fe.getKind() == ElementKind.METHOD) {
                    // no-arg @AutoLogPose2d method
                    ExecutableElement method = (ExecutableElement) fe;
                    String name = method.getSimpleName().toString();
                    String accessor = "this." + name + "()";
                    toLog.addStatement(
                            "$T.logPose2d($S, $L[0], $L[1], $L[2], $L)",
                            KOALA_LOG,
                            keyBase,
                            accessor, accessor, accessor,
                            post
                    );
                }
            }
        }

        //constructor
        for (Element enclosed : classElem.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) continue;
            ExecutableElement constructorElem = (ExecutableElement) enclosed;

            List<ParameterSpec> paramList = new ArrayList<>();
            List<String> paramNames  = new ArrayList<>();
            for (VariableElement param : constructorElem.getParameters()) {
                String name = param.getSimpleName().toString();
                paramList.add(ParameterSpec.builder(TypeName.get(param.asType()), name).build());
                paramNames.add(name);
            }

            MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(paramList)
                    .addStatement("super($L)", String.join(", ", paramNames));

            // Inject supplier wrapping logic (as before)…
            if (!supplierFields.isEmpty()) {
                for (int i = 0; i < supplierFields.size(); i++) {
                    ctor.addStatement(
                            "super.$L = $T.wrap($S, super.$L, $L)",
                            supplierFields.get(i),
                            SUPPLIER_LOG,
                            supplierKeys.get(i),
                            supplierFields.get(i),
                            postToFtcDashBoard
                    );
                }
            }

            // Register with AutoLogManager
            ctor.addStatement("$T.register(this)", AUTO_LOG_MANAGER);

            clsBuilder.addMethod(ctor.build());
        }


        // Methods
        for (Element me : allElements) {
            if(me.getAnnotationMirrors().stream().anyMatch(m -> m.getAnnotationType().toString().equals("Ori.Coval.Logging.DoNotLog")))
                continue;

            if(me.getModifiers().contains(Modifier.FINAL)) continue;

            if (me.getKind() != ElementKind.METHOD) continue;
            ExecutableElement method = (ExecutableElement) me;
            Set<Modifier> mmods = method.getModifiers();

            PackageElement fieldPkg = processingEnv.getElementUtils().getPackageOf(method);
            if (!mmods.contains(Modifier.PUBLIC) && !fieldPkg.equals(currentPkg)) continue;

            if (mmods.contains(Modifier.STATIC)) continue;
            TypeMirror rt = method.getReturnType();
            if (!isLoggableType(rt))
                continue;
            String mname = method.getSimpleName().toString();
            TypeName rtn = TypeName.get(rt);
            String key = orig + "/" + mname;
            StringBuilder params = new StringBuilder();
            List<ParameterSpec> paramList = new ArrayList<>();
            for (VariableElement parameter : method.getParameters()) {
                TypeName type = TypeName.get(parameter.asType());
                paramList.add(ParameterSpec.builder(type, parameter.getSimpleName().toString()).build());
                params.append(",").append(parameter.getSimpleName().toString());
            }
            if (params.toString().startsWith(",")) {
                params = new StringBuilder(params.substring(1));
            }

            // override method
            MethodSpec.Builder overrideBuilder = MethodSpec.methodBuilder(mname)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(rtn)
                    .addStatement("$T result = super.$L($L)", rtn, mname, params.toString())
                    .addParameters(paramList)
                    .addStatement("return $T.log($S, result, $L)", KOALA_LOG, key, postToFtcDashBoard);

            MethodSpec override = overrideBuilder.build();


            clsBuilder.addMethod(override);
        }


        clsBuilder.addMethod(toLog.build());

        // Write file
        try {
            assert pkg != null;
            JavaFile.builder(pkg, clsBuilder.build()).build().writeTo(processingEnv.getFiler());
        } catch (IOException ex) {
            processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.ERROR,
                    "Failed to write AutoLogged: " + ex.getMessage());
        }
    }

    private String getPackageName(TypeElement t) {
        Element e = t;
        while (e != null && !(e instanceof PackageElement)) {
            e = e.getEnclosingElement();
        }
        return e == null ? null : ((PackageElement) e).getQualifiedName().toString();
    }

    private boolean isLoggableType(TypeMirror tm) {
        TypeKind k = tm.getKind();

        // 1) primitives
        if (k.isPrimitive()) return true;

        // 2) boxed types and String
        if (k == TypeKind.DECLARED) {
            String name = tm.toString();
            switch (name) {
                case "java.lang.Boolean":
                case "java.lang.Byte":
                case "java.lang.Character":
                case "java.lang.Short":
                case "java.lang.Integer":
                case "java.lang.Long":
                case "java.lang.Float":
                case "java.lang.Double":
                case "java.lang.String":
                    return true;
            }
        }

        // 3) arrays of any of the above (including wrapper arrays, primitive arrays, String[])
        if (k == TypeKind.ARRAY) {
            ArrayType at = (ArrayType) tm;
            return isLoggableType(at.getComponentType());
        }

        return false;
    }
}
