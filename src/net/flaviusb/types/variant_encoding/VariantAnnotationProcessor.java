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
import javax.lang.model.type.TypeKind;
import javax.tools.JavaFileObject;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import java.io.Writer;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
   "net.flaviusb.types.variant_encoding.Variant"
 })
public class VariantAnnotationProcessor extends AbstractProcessor {
  static class VariantInstance {
    VariantInstance(String facade, String implementation, Writer handle) {
      facadeName = facade;
      implementationName = implementation;
      source_handle = handle;
    }
    String facadeName;
    String implementationName;
    Writer source_handle;
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
      sb.append(";\n\npublic ");
      sb.append(type_type);
      sb.append(" ");
      sb.append(parts[1]);
    } else {
      sb.append("public ");
      sb.append(type_type);
      sb.append(" ");
      sb.append(fully_qualified_name);
    }
    return sb.toString();
  }
  @Override
  public boolean process(Set<? extends TypeElement> variant_classes, RoundEnvironment env) {
    try {
      // Transform the unsorted annotations into VariantInstances grouped by baseName
      Map<String, List<VariantInstance>> variant_groups = new HashMap<String, List<VariantInstance>>();
      for(TypeElement element : variant_classes) {
        Variant v = element.getAnnotation(Variant.class);
        List<VariantInstance> group = variant_groups.getOrDefault(v.baseName(), new ArrayList<VariantInstance>());
        Writer variant_out = filer.createSourceFile(v.facadeName()).openWriter();
        String implementation_class = element.getQualifiedName().toString();
        group.add(new VariantInstance(v.facadeName(), implementation_class, variant_out));
        // While we are here, write out the preamble for the facade class
        variant_out.write(preamble_builder(v.facadeName(), "class"));
        variant_out.write(" extends ");
        variant_out.write(implementation_class);
        variant_out.write(", implements ");
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
          sb.append(vi.facadeName);
          sb.append("();\n");
          for (VariantInstance vii : variants) {
            // Write out the get method implementation for each variant
            vii.source_handle.write("public Optional<");
            vii.source_handle.write(vi.facadeName);
            vii.source_handle.write("> get");
            vii.source_handle.write(vi.facadeName);
            vii.source_handle.write("() {\n");
            if(vi.facadeName.equals(vii.facadeName)) {
              vii.source_handle.write("return Optional.of(this);\n");
            } else {
              vii.source_handle.write("return Optional.empty();\n");
            }
            vii.source_handle.write("}\n");
          }
        }
        sb.append("}\n");
      }
      return false;
    } catch (java.io.IOException e) {
      throw new Error(e);
    }
  }
}
