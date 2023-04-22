package indi.shui4.thinking.spring.depencey.injection.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 自定义依赖注入注解
 * @author shui4
 */
@Target({METHOD, CONSTRUCTOR, FIELD})
@Retention(RUNTIME)
@Documented
public @interface InjectedUser {
}
