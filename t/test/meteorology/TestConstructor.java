package test.meteorology;

import net.flaviusb.types.variant_encoding.Variant;
import net.flaviusb.types.variant_encoding.VariantConstructor;

import service.meteorology.Rainy;

public class TestConstructor {
  public String doIt() {
    Rainy weather = new Rainy(7, true);
    return weather.description;
  }
}
