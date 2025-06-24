package Ori.Coval.FtcAutoLog;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.util.Pair;

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
@SupportedAnnotationTypes({"Ori.Coval.Logging.AutoLog", "Ori.Coval.Logging.AutoLogOutput"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoLogAnnotationProcessor extends AbstractProcessor {
    private boolean staticRegistryWritten = false;
    // Adjust this to your WpiLog package
    private static final ClassName KOALA_LOG = ClassName.get("Ori.Coval.Logging.Logger", "KoalaLog");
    private static final ClassName LOGGED = ClassName.get("Ori.Coval.Logging", "Logged");
    private static final ClassName AUTO_LOG_MANAGER = ClassName.get("Ori.Coval.Logging", "AutoLogManager");
    private static final ClassName SUPPLIER_LOG = ClassName.get("Ori.Coval.Logging", "SupplierLog");

    List<Element> elements = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element e : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (e.getKind() == ElementKind.CLASS) {
                    boolean postToFtc = getAnnotationValue(e,"Ori.Coval.Logging.AutoLog", "postToFtcDashboard", true);
                    generateAutoLog((TypeElement) e, postToFtc);
                }

                if(e.getKind() == ElementKind.METHOD || e.getKind() == ElementKind.FIELD) {
                    elements.add(e);
                }
            }
        }
        if (!staticRegistryWritten && roundEnv.processingOver()) {
            generateStaticRegistry();
            staticRegistryWritten = true;
        }
        return true;
    }

    private boolean getAnnotationValue(Element element, String annotationName, String key, boolean defaultValue) {
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
        return defaultValue;
    }
    private void generateStaticRegistry() {
        String registryPkg  = "Ori.Coval.AutoLog";
        String registryName = "AutoLogStaticRegistry";

        // holder for each entry’s key, owner type, member name, whether it's a method, and post flag
        class Entry {
            final String key;
            final ClassName owner;
            final String member;
            final boolean isMethod;
            final boolean post;
            Entry(String key, ClassName owner, String member, boolean isMethod, boolean post) {
                this.key      = key;
                this.owner    = owner;
                this.member   = member;
                this.isMethod = isMethod;
                this.post     = post;
            }
        }
        List<Entry> entries = new ArrayList<>();

        // 'elements' should be your collected @AutoLogOutput elements across rounds
        for (Element elem : elements) {
            // only static fields or zero-arg static methods
            boolean isField  = elem.getKind() == ElementKind.FIELD;
            boolean isMethod = elem.getKind() == ElementKind.METHOD
                    && ((ExecutableElement)elem).getParameters().isEmpty();
            if (!(isField || isMethod)) continue;
            if (!elem.getModifiers().contains(Modifier.STATIC)) continue;

            TypeElement enclosing = (TypeElement) elem.getEnclosingElement();
            boolean classHasAutoLog = enclosing.getAnnotationMirrors().stream()
                    .anyMatch(m -> m.getAnnotationType().toString()
                            .equals("Ori.Coval.Logging.AutoLog"));
            if (classHasAutoLog) continue;

            // fully qualified owner class
            String pkgName  = processingEnv.getElementUtils()
                    .getPackageOf(enclosing).getQualifiedName().toString();
            String clsName  = enclosing.getSimpleName().toString();
            ClassName owner = ClassName.get(pkgName, clsName);

            String memberName = elem.getSimpleName().toString();
            String key        = clsName + "/" + memberName;

            boolean postToFtc = getAnnotationValue(
                    elem,
                    "Ori.Coval.Logging.AutoLogOutput",
                    "postToFtcDashboard",
                    true
            );

            entries.add(new Entry(key, owner, memberName, isMethod, postToFtc));
        }

        // build the toLog() method
        MethodSpec.Builder toLog = MethodSpec.methodBuilder("toLog")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class);

        ClassName koalaLog = ClassName.get("Ori.Coval.Logging.Logger", "KoalaLog");
        for (Entry e : entries) {
            String access = e.isMethod
                    ? String.format("%s.%s()", "owner", "member")  // placeholder, see below
                    : String.format("%s.%s",   "owner", "member");
            // but with JavaPoet we do:
            if (e.isMethod) {
                toLog.addStatement(
                        "$T.log($S, $T.$L(), $L)",
                        koalaLog,
                        e.key,
                        e.owner,
                        e.member,
                        e.post
                );
            } else {
                toLog.addStatement(
                        "$T.log($S, $T.$L, $L)",
                        koalaLog,
                        e.key,
                        e.owner,
                        e.member,
                        e.post
                );
            }
        }

        // build the registry class
        TypeSpec registry = TypeSpec.classBuilder(registryName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get("Ori.Coval.Logging", "Logged"))
                .addStaticBlock(CodeBlock.of(
                        "$T.register(new $L());",
                        ClassName.get("Ori.Coval.Logging", "AutoLogManager"),
                        registryName
                ))
                .addMethod(toLog.build())
                .build();

        // emit it once (FilerException swallowed)
        try {
            JavaFile.builder(registryPkg, registry)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (FilerException ignored) {
            // already written, ignore
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

        // Builder for the new class
        TypeSpec.Builder clsBuilder = TypeSpec.classBuilder(autoName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(LOGGED)
                .superclass(TypeName.get(classElem.asType()));

        // Build toLog method for fields
        MethodSpec.Builder toLog = MethodSpec.methodBuilder("toLog")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Auto-generated telemetry logging\n");


        // collect supplier fields so we can make one constructor
        List<String> supplierFields = new ArrayList<>();
        List<String> supplierKeys = new ArrayList<>();

        // Fields
        for (Element fe : classElem.getEnclosedElements()) {
            if (fe.getKind() != ElementKind.FIELD) continue;
            VariableElement field = (VariableElement) fe;
            Set<Modifier> mods = field.getModifiers();
            if (mods.contains(Modifier.PRIVATE)) continue;
            String fname = field.getSimpleName().toString();
            TypeMirror t = field.asType();
            TypeKind k = t.getKind();

            boolean isSupplier = (k == TypeKind.DECLARED && t.toString().equals("java.util.function.LongSupplier"))
                    || (k == TypeKind.DECLARED && t.toString().equals("java.util.function.DoubleSupplier"))
                    || (k == TypeKind.DECLARED && t.toString().equals("java.util.function.IntSupplier"))
                    || (k == TypeKind.DECLARED && t.toString().equals("java.util.function.BooleanSupplier"));

            if (!(k.isPrimitive()
                    || (k == TypeKind.DECLARED && t.toString().equals("java.lang.String"))
                    || k == TypeKind.ARRAY
                    || isSupplier))
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

                for (AnnotationMirror mirror : fe.getAnnotationMirrors()) {
                    if (mirror.getAnnotationType().toString().equals("Ori.Coval.Logging.AutoLogOutput")) {
                        boolean postToFtc = getAnnotationValue(fe,
                                "Ori.Coval.Logging.AutoLogOutput",
                                "postToFtcDashboard",
                                true
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
                supplierFields.add(fname);
                supplierKeys.add(key);
            } else {
                toLog.addStatement("$T.log($S, this.$L, $L)", KOALA_LOG, key, fname, postToFtcDashBoard);
            }
        }

        //constructor
        for (Element enclosed : classElem.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) continue;

            ExecutableElement constructorElem = (ExecutableElement) enclosed;

            List<ParameterSpec> paramList = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();

            for (VariableElement param : constructorElem.getParameters()) {
                String name = param.getSimpleName().toString();
                TypeName type = TypeName.get(param.asType());
                paramList.add(ParameterSpec.builder(type, name).build());
                paramNames.add(name);
            }

            MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(paramList)
                    .addStatement("super($L)", String.join(", ", paramNames));

            // Inject supplier wrapping logic
            if (!supplierFields.isEmpty()) {
                for (int i = 0; i < supplierFields.size(); i++) {
                    String fname = supplierFields.get(i);
                    String key = supplierKeys.get(i);
                    ctor.addStatement(
                            "super.$L = $T.wrap($S, super.$L, $L)",
                            fname, SUPPLIER_LOG, key, fname, postToFtcDashBoard
                    );
                }
            }

            // Register with AutoLogManager
            ctor.addStatement("$T.register(this)", AUTO_LOG_MANAGER);

            // Add the constructor to the class
            clsBuilder.addMethod(ctor.build());
        }


        // Methods
        for (Element me : classElem.getEnclosedElements()) {
            if (me.getKind() != ElementKind.METHOD) continue;
            ExecutableElement method = (ExecutableElement) me;
            Set<Modifier> mmods = method.getModifiers();
            if (!mmods.contains(Modifier.PUBLIC)) continue;
//            if (!method.getParameters().isEmpty()) continue;
            TypeMirror rt = method.getReturnType();
            TypeKind rtk = rt.getKind();
            if (!(rtk.isPrimitive() || (rtk == TypeKind.DECLARED && rt.toString().equals("java.lang.String"))))
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

            MethodSpec override  = overrideBuilder.build();


            clsBuilder.addMethod(override);

            for (AnnotationMirror mirror : me.getAnnotationMirrors()) {
                if (mirror.getAnnotationType().toString().equals("Ori.Coval.Logging.AutoLogOutput") && paramList.isEmpty()) {
                    boolean postToFtc = getAnnotationValue(me,"Ori.Coval.Logging.AutoLogOutput", "postToFtcDashboard", true);
                    toLog.addStatement("$T.log($S, this.$L(), $L)", KOALA_LOG, key, mname, postToFtc);
                }
            }
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
}
