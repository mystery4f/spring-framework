package indi.shui4.thinking.spring.annotations;

import java.lang.annotation.*;

/**
 * {@link MyComponent2} "派生"注解
 *
 * @author shui4
 */
@MyComponent
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyComponent2 {


	String value1();
}
