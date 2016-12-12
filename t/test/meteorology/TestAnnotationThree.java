package test.meteorology;

import net.flaviusb.types.variant_encoding.Variant;
import net.flaviusb.types.variant_encoding.VariantConstructor;

@Variant(
  baseName = "service.meteorology.Weather",
  facadeName = "service.meteorology.Rainy"
)
public class TestAnnotationThree {
  public String description;
  @VariantConstructor
  public TestAnnotationThree(int rain_level, boolean umbrella_warning) {
    if(umbrella_warning) {
      description = "Bring umbrella";
    } else {
      if(rain_level > 10) {
        description = "Heavy rain";
      } else {
        description = "Rainy.";
      }
    }
  }
}
