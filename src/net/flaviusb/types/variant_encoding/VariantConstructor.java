package net.flaviusb.types.variant_encoding;

import javax.annotation.processing.*;
import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.CONSTRUCTOR)
public @interface VariantConstructor {
}
