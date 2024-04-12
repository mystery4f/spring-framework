package indi.shui4.thinking.spring.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * {@link MyComponent} "派生"注解
 *
 * @author shui4
 */
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyComponent {
}
