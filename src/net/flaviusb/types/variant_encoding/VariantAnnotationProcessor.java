package net.flaviusb.types.variant_encoding;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.Filer;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaFileObject;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import java.io.Writer;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
   "net.flaviusb.types.variant_encoding.Variant",
   "net.flaviusb.types.variant_encoding.VariantConstructor"
 })
public class VariantAnnotationProcessor extends AbstractProcessor {
  static class VariantInstance {
    VariantInstance(String facade, String implementation, Writer handle, List<String> passthrough_code) {
      facadeName = facade;
      implementationName = implementation;
      source_handle = handle;
      if(facadeName.contains(".")) {
        String[] parts = facadeName.split("[.](?=[^.]+$)");
        facadeSimpleName = parts[1];
        facadePackageName = Optional.of(parts[0]);
      } else {
        facadeSimpleName = facadeName;
        facadePackageName = Optional.empty();
      }
      pregenerated_passthrough = passthrough_code;
    }
    String facadeName;
    String implementationName;
    String facadeSimpleName;
    Optional<String> facadePackageName;
    Writer source_handle;
    List<String> pregenerated_passthrough;
  }
  Filer filer;
  @Override public synchronized void init(ProcessingEnvironment env) {
    super.init(env);
    filer = processingEnv.getFiler();
  }
  public String preamble_builder(String fully_qualified_name, String type_type) {
    StringBuilder sb = new StringBuilder();
    if(fully_qualified_name.contains(".")) {
      String[] parts = fully_qualified_name.split("[.](?=[^.]+$)");
      sb.append("package ");
      sb.append(parts[0]);
      sb.append(";\nimport java.util.Optional;\n\npublic ");
      sb.append(type_type);
      sb.append(" ");
      sb.append(parts[1]);
    } else {
      sb.append("import java.util.Optional;\n\n");
      sb.append("public ");
      sb.append(type_type);
      sb.append(" ");
      sb.append(fully_qualified_name);
    }
    return sb.toString();
  }
  @Override
  public boolean process(Set<? extends TypeElement> annotations_for_this_round, RoundEnvironment env) {
    try {
      // Transform the unsorted annotations into VariantInstances grouped by baseName
      Set<? extends Element> variant_classes = env.getElementsAnnotatedWith(net.flaviusb.types.variant_encoding.Variant.class);
      Map<String, List<VariantInstance>> variant_groups = new HashMap<String, List<VariantInstance>>();
      for(Element el : variant_classes) {
        TypeElement element = (TypeElement) el;
        Variant v = element.getAnnotation(net.flaviusb.types.variant_encoding.Variant.class);
        if(v == null) {
          System.out.println("No class found :-(");
          continue;
        }
        List<VariantInstance> group = variant_groups.getOrDefault(
            v.baseName(),
            new ArrayList<VariantInstance>());
        // Delegate through all constructors annotated with @VariantConstructor
        List<String> pregenerated_passthrough = new ArrayList();
        Writer variant_out = filer.createSourceFile(v.facadeName()).openWriter();
        String implementation_class = element.getQualifiedName().toString();
        VariantInstance the_vi = new VariantInstance(v.facadeName(), implementation_class, variant_out, pregenerated_passthrough);
        for (Element elem : element.getEnclosedElements()) {
          if (!elem.getKind().equals(javax.lang.model.element.ElementKind.CONSTRUCTOR)) {
            continue;
          }
          ExecutableElement e = (ExecutableElement) elem;
          if(e.getAnnotation(net.flaviusb.types.variant_encoding.VariantConstructor.class) != null) {
            StringBuilder total = new StringBuilder();
            total.append("public ");
            total.append(the_vi.facadeSimpleName);
            total.append("(");
            StringBuilder bare_args = new StringBuilder();
            boolean first = true;
            for(VariableElement parameter : e.getParameters()) {
              if(first) {
                first = false;
              } else {
                total.append(", ");
                bare_args.append(", ");
              }
              total.append(parameter.asType().toString());
              total.append(" ");
              total.append(parameter.getSimpleName());
              bare_args.append(parameter.getSimpleName());
            }
            total.append(") {\nsuper(");
            total.append(bare_args);
            total.append(");\n}\n");
            the_vi.pregenerated_passthrough.add(total.toString());
          }
        }
        group.add(the_vi);
        // While we are here, write out the preamble for the facade class
        variant_out.write(preamble_builder(v.facadeName(), "class"));
        variant_out.write(" extends ");
        variant_out.write(implementation_class);
        variant_out.write(" implements ");
        variant_out.write(v.baseName());
        variant_out.write(" {\n");
        variant_groups.put(v.baseName(), group);
      }
      for(String variant_base_name : variant_groups.keySet()) {
        // Generate base interface for each variant
        List<VariantInstance> variants = variant_groups.get(variant_base_name);
        Writer variant_base_class_output = filer.createSourceFile(variant_base_name).openWriter();
        StringBuilder sb = new StringBuilder();
        sb.append(preamble_builder(variant_base_name, "interface"));
        sb.append(" {\n");
        for(VariantInstance vi : variants) {
          sb.append("public Optional<");
          sb.append(vi.facadeName);
          sb.append("> get");
          sb.append(vi.facadeSimpleName);
          sb.append("();\n");
          for (VariantInstance vii : variants) {
            // Write out the get method implementation for each variant
            vii.source_handle.write("public Optional<");
            vii.source_handle.write(vi.facadeName);
            vii.source_handle.write("> get");
            vii.source_handle.write(vi.facadeSimpleName);
            vii.source_handle.write("() {\n");
            if(vi.facadeName.equals(vii.facadeName)) {
              vii.source_handle.write("return Optional.of(this);\n");
            } else {
              vii.source_handle.write("return Optional.empty();\n");
            }
            vii.source_handle.write("}\n");
          }
        }
        for(VariantInstance vi : variants) {
          for(String passthrough : vi.pregenerated_passthrough) {
            vi.source_handle.write(passthrough);
          }
          vi.source_handle.write("}\n");
          vi.source_handle.close();
        }
        sb.append("}\n");
        variant_base_class_output.write(sb.toString());
        variant_base_class_output.close();
      }
      return true;
    } catch (java.io.IOException e) {
      throw new Error(e);
    }
  }
}
