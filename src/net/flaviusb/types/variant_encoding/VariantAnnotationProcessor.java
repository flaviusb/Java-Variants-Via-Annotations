package net.flaviusb.types.variant_encoding;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
   "net.flaviusb.types.variant_encoding.Variant"
 })
public class VariantAnnotationProcessor extends AbstractProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> base, RoundEnvironment env) {
    return false;
  }
}
