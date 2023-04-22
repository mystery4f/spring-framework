package indi.shui4.thinking.spring.depencey.injection.annotation;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author shui4
 */
@Target({METHOD, CONSTRUCTOR, FIELD})
@Retention(RUNTIME)
@Documented
@Autowired
public @interface MyAutowired {
	/**
	 * Declares whether the annotated dependency is required.
	 * <p>Defaults to {@code true}.
	 */
	boolean required() default true;
}
