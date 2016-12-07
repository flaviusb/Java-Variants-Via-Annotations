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
    VariantInstance(String facade, String implementation) {
      facadeName = facade;
      implementationName = implementation;
    }
    String facadeName;
    String implementationName;
  }
  Filer filer;
  @Override public synchronized void init(ProcessingEnvironment env) {
    super.init(env);
    filer = processingEnv.getFiler();
  }
  @Override
  public boolean process(Set<? extends TypeElement> variant_classes, RoundEnvironment env) {
    // Transform the unsorted annotations into VariantInstances grouped by baseName
    Map<String, List<VariantInstance>> variant_groups = new HashMap<String, List<VariantInstance>>();
    for(TypeElement element : variant_classes) {
      Variant v = element.getAnnotation(Variant.class);
      List<VariantInstance> group = variant_groups.getOrDefault(v.baseName(), new ArrayList<VariantInstance>());
      group.add(new VariantInstance(v.facadeName(), element.getQualifiedName().toString()));
      variant_groups.put(v.baseName(), group);
    }
    // Generate base interface for each variant
    for(String variant_base_name : variant_groups.keySet()) {
       try {
         Writer variant_base_class_output = filer.createSourceFile(variant_base_name).openWriter();
       } catch (java.io.IOException e) {
         throw new Error(e);
       }
    }
    return false;
  }
}
