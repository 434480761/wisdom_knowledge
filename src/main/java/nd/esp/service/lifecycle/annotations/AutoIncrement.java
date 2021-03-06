package nd.esp.service.lifecycle.annotations;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoIncrement {
    String name() default "";
}
