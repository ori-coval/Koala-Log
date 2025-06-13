package Ori.Coval.FtcAutoLog;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotation processor that generates an AutoLogged subclass which
 * overrides fields and methods to log via WpiLog.
 */
@SupportedAnnotationTypes("Ori.Coval.Logging.AutoLog")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoLogAnnotationProcessor extends AbstractProcessor {
    // Adjust this to your WpiLog package
    private static final ClassName WPILOG = ClassName.get("Ori.Coval.Logging", "WpiLog");
    private static final ClassName LOGGED = ClassName.get("Ori.Coval.Logging", "Logged");
    private static final ClassName AUTO_LOG_MANAGER = ClassName.get("Ori.Coval.Logging", "AutoLogManager");
    private static final ClassName SUPPLIER_LOG = ClassName.get("Ori.Coval.Logging", "SupplierLog");

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element e : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (e.getKind() == ElementKind.CLASS) {
                    boolean postToFtc = getAnnotationValue(e, "postToFtcDashboard", true);
                    generate((TypeElement) e, postToFtc);
                }
            }
        }
        return true;
    }

    private boolean getAnnotationValue(Element element, String key, boolean defaultValue) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals("Ori.Coval.Logging.AutoLog")) {
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


    private void generate(TypeElement classElem, boolean postToFtcDashBoard) {
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

            if (!(k.isPrimitive() || (k == TypeKind.DECLARED && t.toString().equals("java.lang.String")) || k == TypeKind.ARRAY || isSupplier))
                continue;

            String key = orig + "/" + fname;
            if (isSupplier) {
                supplierFields.add(fname);
                supplierKeys.add(key);
            } else {
                toLog.addStatement("$T.log($S, this.$L, $L)", WPILOG, key, fname, postToFtcDashBoard);
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
                    .addStatement("return $T.log($S, result, $L)", WPILOG, key, postToFtcDashBoard);

            MethodSpec override  = overrideBuilder.build();


            clsBuilder.addMethod(override);
            // also log in toLog
//            toLog.addStatement("$T.getInstance().log($S, this.$L())", WPILOG, key, mname);
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
