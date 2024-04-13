package indi.shui4.thinking.spring.annotations;

import java.lang.annotation.*;

/**
 * @author shui4
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyConfiguration {
	String name();
}
